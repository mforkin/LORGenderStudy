package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}

object SignOffFilterApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val cleanGreetingTextPath = config.getString("cleanGreetingTextPath")
  val cleanSignOffTextPath = config.getString("cleanSignOffTextPath")
  val noSignOffTextPath = config.getString("noSignOffTextPath")

  val signOffs = Seq(
    "sincerely yours",
    "yours sincerely",
    "sincerely,",
    "kindly,",
    "sincercly,",
    "respectfully,",
    "regards,"
  )

  val textDir = new File(cleanGreetingTextPath)

  val txtFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  })

  val lastFiles = txtFiles.groupBy(f => f.getName.split("-").head).map {
    case (_, files) =>
      files.maxBy(f => f.getName).getName
  }.toSet

  txtFiles.foreach(f => {
    if (expectSignOff(f.getName)) {
      val lines = FileHelper.getTxtLinesFromFile(f)
      val index = findSignOffLine(lines)
      if (index < 0) {
        FileUtils.copyFile(f, new File(noSignOffTextPath + File.separator + f.getName))
      } else {
        val text = lines.take(index + 1).mkString("\n")
        val pw = new PrintWriter(cleanSignOffTextPath + File.separator + f.getName, "UTF-8")
        Try {
          pw.write(text)
        } match {
          case Success(_) => pw.close()
          case Failure(ex) =>
            pw.close()
            throw new Exception(s"Failed to clean signOff text from file: ${f.getName}", ex)
        }
      }

    } else {
      FileUtils.copyFile(f, new File(cleanSignOffTextPath + File.separator + f.getName))
    }
  })

  // only expect it on the last file
  def expectSignOff (fileName: String): Boolean = {
    lastFiles.contains(fileName)
  }

  def findSignOffLine(lines: Seq[String]): Int = {
    lines.zipWithIndex.reverse.foldLeft(-1)((index, row) => {
      if (index < 0) {
        row match {
          case (line, i) => {
            val standardizeLine = line.toLowerCase
            if (signOffs.exists(signOff => standardizeLine.contains(signOff))) {
              i
            } else {
              index
            }
          }
        }
      } else {
        index
      }
    })
  }
}
