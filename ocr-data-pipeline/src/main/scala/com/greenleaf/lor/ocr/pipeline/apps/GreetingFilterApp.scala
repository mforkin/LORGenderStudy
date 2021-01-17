package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.text.similarity.LevenshteinDistance

import scala.collection.parallel.CollectionConverters.ArrayIsParallelizable
import scala.util.{Failure, Success, Try}

object GreetingFilterApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val cleanStandardTextPath = config.getString("cleanStandardTextPath")
  val cleanGreetingTextPath = config.getString("cleanGreetingTextPath")
  val noHeaderFoundPath = config.getString("noGreetingTextPath")
  val distanceCalculator = new LevenshteinDistance()
  val introMaxEdits = 1

  val introductions = Seq(
    "whom it may concern",
    "whom tt may concern",
    "whom is may concern",
    "whom it may concem",
    "whom this may concern",
    "to program director",
    "to the program director",
    "to the anesthesia program director",
    "to residency program director",
    "to selection committee",
    "to sir/madam",
    "to the attention of the program director",
    "to residency directors",
    "dear ",
    "subject:",
    "recognition of limits, conscientiousness, etc",
    "residency selection committee:",
    "residency program director,",
    "program director,",
    "anesthesiology residency program:",
    "letter of recommendation for ",
    "eras letter id:",
    "aamc id:",
    "written comments:",
    "reservations about qualification for further training",
    "re:",
    "rie:",
    "department of surgery letter of recommendation",
    "svannah, GA 31406"
  )

  val textDir = new File(cleanStandardTextPath)

  val textFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  }).par

  val firstFiles = textFiles.groupBy(f => f.getName.split("-").head).map {
    case (_, files) =>
      files.minBy(f => f.getName).getName
  }.toSet

  textFiles.foreach(f => {
    if (expectGreeting(f.getName)) {
      val lines = FileHelper.getTxtLinesFromFile(f)
      val maxExpectedIdx = lines.length.toDouble
      val index = findIntroLine(lines)
      if (index > maxExpectedIdx) {
        FileUtils.copyFile(f, new File(noHeaderFoundPath + File.separator + s"tooBig-$index" + f.getName))
      } else if (index < 0) {
        FileUtils.copyFile(f, new File(noHeaderFoundPath + File.separator + f.getName))
      } else {
        val text = lines.drop(index).mkString("\n")
        val pw = new PrintWriter(cleanGreetingTextPath + File.separator + f.getName, "UTF-8")
        Try {
          pw.write(text)
        } match {
          case Success(_) => pw.close()
          case Failure(ex) =>
            pw.close()
            throw new Exception(s"Failed to clean greeting text from file: ${f.getName}", ex)
        }
      }
    } else {
      FileUtils.copyFile(f, new File(cleanGreetingTextPath + File.separator + f.getName))
    }
  })

  def expectGreeting(fileName: String): Boolean = {
    firstFiles.contains(fileName)
  }

  def findIntroLine (lines: Seq[String]): Int = {
    lines.zipWithIndex.foldLeft(-1)((index, row) => {
      // only use the first occurrence, should break out but whatever
      if (index < 0) {
        row match {
          case (line, i) =>
            val standardizeLine = line.toLowerCase
            if (introductions.exists(intro => standardizeLine.contains(intro))) {
              i
            } else {
              index
            }
        }
      } else {
        index
      }
    })
  }
}
