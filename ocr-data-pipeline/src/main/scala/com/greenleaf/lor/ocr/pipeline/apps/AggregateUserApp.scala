package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success, Try}

object AggregateUserApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val sanitizedDataPath = config.getString("sanitizedNameTextPath")
  val singleDocPath = config.getString("singleDocPath")

  val textDir = new File(sanitizedDataPath)

  val textFileGroups = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  }).groupBy(_.getName.split("-").head)

  textFileGroups.foreach {
    case (group, files) =>
      val singleFileText = files.map(f => FileHelper.getTxtFromFile(f, "\n")).mkString("\n")
      val pw = new PrintWriter(singleDocPath + File.separator + group + ".txt")
      Try {
        pw.write(singleFileText)
      } match {
        case Success(_) => pw.close()
        case Failure(ex) =>
          pw.close()
          throw new Exception("Could not combine files: " + group, ex)
      }
  }
}
