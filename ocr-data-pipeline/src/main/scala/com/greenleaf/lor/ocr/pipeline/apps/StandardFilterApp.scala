package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.text.similarity.LevenshteinDistance

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object StandardFilterApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val processFilesPath = config.getString("processedTextPath")
  val cleanStandardTextPath = config.getString("cleanStandardTextPath")
  val cleanStatsPath = config.getString("cleanStandardStatsPath")
  val cleanedFiles: mutable.Set[String] = mutable.Set()
  val cleanPW = new PrintWriter(cleanStatsPath + File.separator + "cleanStandardStats.txt")
  val distanceCalculator = new LevenshteinDistance()
  val numEdits = 5

  Try {
    val textDir = new File(processFilesPath)

    val txtFiles = textDir.listFiles().filter(f => {
      !f.isDirectory && !f.getName.startsWith(".")
    })

    val groupedFiles: Map[String, Array[File]] = txtFiles.groupBy(f => f.getName.split("-").head)
    groupedFiles.foreach {
      case (_, files) =>
        if (files.length == 1) {
          val f = files.head
          FileUtils.copyFile(f, new File(cleanStandardTextPath + File.separator + f.getName))
        } else {
          val fileLines = files.map(f => f.getName -> FileHelper.getTxtLinesFromFile(f))
          val commonLines = fileLines.tail.foldLeft(fileLines.head._2) {
            case (common, (_, newLines)) =>
              common.filter(line => line.length > 0 && newLines.exists(l => distanceCalculator.apply(l.toLowerCase, line.toLowerCase) < numEdits))
          }

          if (commonLines.isEmpty) {
            files.foreach(f => FileUtils.copyFile(f, new File(cleanStandardTextPath + File.separator + f.getName)))
          } else {
            fileLines.foreach {
              case (name, lines) => {
                val newLines = lines.filter(line => line.length == 0 || (line.length > 0 && !commonLines.exists(l => distanceCalculator.apply(l.toLowerCase, line.toLowerCase) < numEdits))).mkString("\n")
                val pw = new PrintWriter(cleanStandardTextPath + File.separator + name)
                Try {
                  pw.write(newLines)
                  cleanedFiles.add(name)
                } match {
                  case Success(_) => pw.close()
                  case Failure(ex) =>
                    pw.close()
                    throw new Exception(s"Failed to remove detected standard text from file: ${name}", ex)
                }
              }
            }
          }
        }
    }
    cleanedFiles.mkString("\n")
  } match {
    case Success(stats) =>
      Try {
        cleanPW.write(stats)
      } match {
        case Success(_) => cleanPW.close()
        case Failure(ex) =>
          cleanPW.close()
          throw new Exception("Failed to write standardized stats file", ex)
      }
    case Failure(ex) =>
      cleanPW.close()
      throw new Exception(s"Failed to write standardized stats file", ex)
  }
}
