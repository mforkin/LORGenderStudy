package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.model.UserMetaData
import com.greenleaf.lor.ocr.pipeline.{FileHelper, KeyParser}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}

object SignOffFilterApp extends App with StrictLogging {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val cleanGreetingTextPath = config.getString("cleanGreetingTextPath")
  val cleanSignOffTextPath = config.getString("cleanSignOffTextPath")
  val noSignOffTextPath = config.getString("noSignOffTextPath")
  val skipSignOffFilter = config.getStringList("skipSignOffFilter")
  val keyPath = config.getString("keyPath")
  val pagesToTrim = config.getStringList("signOffPagesToTrim")

  val key: Seq[UserMetaData] = KeyParser.parseKey(keyPath)

  val signOffs = Seq(
    "sincerely yours",
    "yours sincerely",
    "sincerely and respectfully",
    "sincerely,",
    "sincerely.",
    "kindly,",
    "sincercly,",
    "sincergly",
    "cerely,",
    "respectfully,",
    "respectfully yours",
    "respectfully submitt",
    "yours truly",
    "sincerely arid ri",
    "yours faithfully,",
    "regards,",
    "5. please feel free to e-mail me with any questions at",
    "thank you, and do not hesitate",
    "thank you for your time",
    "thank you so much for your consideration",
    "nk you for your consideration",
    "thanking you,",
    "thanking you ,",
    "feel free to call me any time",
    "do not hesitate",
    "do.not hesitate",
    "don't hesitate",
    "feel free to contact me",
    "se_wb",
    "cpt (puy a",
    "ncerely yours,",
    "j erfolio md, mba, facs, fccp",
    "dedicated and competent surgeon. j recommend her highly for a position in",
    "or if you have any questions",
    "if | can answer any questions",
    "sincere ours",
    "singerely",
    "thank you very much",
    "john f. butteyworth",
    "steven j zeichn",
    "steven zeichnef, md",
    "not hesitate to contact me at",
    "please call if you have any questions",
    "please contact me if you have any questions",
    "to contact me with any questions",
    "sineerely",
    "charles e. sc",
    "thank you for giving very strong consideration to her application",
    "ryan j. gunselman",
    "(cof soliman; md.",
    "highly recommend her/him for further gme",
    "we will do all we can to recruit her to unc",
    "we will do all we can to recruit",
    "michael as | am certain he will make an outstanding resident",
    "david a. a, m.d.",
    "sinceteis,",
    "know if there is any further information | can provide",
    "thomas jantes, m.d.",
    "cpeyan e, res d,",
    "i look forward to watching his career blossoms been my honor to have worked with him",
    "| long branch, nj 07740",
    "michael j. fowler md"
  )

  val textDir = new File(cleanGreetingTextPath)

  val txtFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  })

  val lastFiles = txtFiles
    .filter(f => !pagesToTrim.contains(f.getName().split("[.]").head))
    .groupBy(f => f.getName.split("-").head).map {
    case (_, files) =>
      files.maxBy(f => f.getName).getName
  }.toSet

  txtFiles.foreach(f => {
    if (expectSignOff(f.getName)) {
      val lines = FileHelper.getTxtLinesFromFile(f)
      val index = findSignOffLine(lines)
      if (index < 0) {
        val pieces = f.getName.split("-")
        val letterId = pieces.head
        val participantNumber = pieces.head.replaceAll("[a-zA-Z]", "").toInt
        val letterWriterName = key.find(a => a.participantNumber == participantNumber) match {
          case Some(participantData) =>
            participantData.lorMetaData.find(a => a.fileName.equalsIgnoreCase(letterId))  match {
              case Some(letterMetaData) =>
                letterMetaData.writerMetaData.name
              case None =>
                logger.info(s"bad letter id $letterId, not in key")
                //throw new Exception(s"bad letter id $letterId, not in key")
                "nope"
            }
          case None =>
            throw new Exception(s"Bad fileName: ${f.getName} parsed into ${participantNumber}")
        }
        val letterWriterIndex = findLetterWriterLine(lines, letterWriterName, f.getName.toLowerCase().contains("127c"))
        if (letterWriterIndex > 0 && letterWriterIndex > lines.count(_.trim.nonEmpty) / 2) {
          val text = lines.take(letterWriterIndex + 1).mkString("\n")
          val pw = new PrintWriter(cleanSignOffTextPath + File.separator + f.getName, "UTF-8")
          Try {
            pw.write(text)
          } match {
            case Success(_) => pw.close()
            case Failure(ex) =>
              pw.close()
              throw new Exception(s"Failed to clean signOff text from file: ${f.getName}", ex)
          }
        } else {
          FileUtils.copyFile(f, new File(noSignOffTextPath + File.separator + f.getName))
        }
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
    val letterId = fileName.split("-").head
    lastFiles.contains(fileName) && !skipSignOffFilter.contains(letterId)
  }

  def findLetterWriterLine(lines: Seq[String], letterWriterName: String, l: Boolean = false): Int = {
    lines.zipWithIndex.reverse.foldLeft(-1)((index, row) => {
      if (index < 0) {
        row match {
          case (line, i) =>
            val standardizeLine = line.toLowerCase
            if (standardizeLine.contains(letterWriterName.replaceAll("[.]", "").toLowerCase)) {
              i
            } else if (standardizeLine.contains(letterWriterName.replaceAll("[.]", "").split(" ").last.toLowerCase)) {
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

  def findSignOffLine(lines: Seq[String]): Int = {
    lines.zipWithIndex.reverse.foldLeft(-1)((index, row) => {
      if (index < 0) {
        row match {
          case (line, i) => {
            val standardizeLine = line.toLowerCase
            if (
              standardizeLine.equalsIgnoreCase("sincerely") ||
              standardizeLine.equalsIgnoreCase("thank you") ||
              standardizeLine.equalsIgnoreCase("thank you,") ||
              standardizeLine.equalsIgnoreCase("sincere") ||
              standardizeLine.equalsIgnoreCase("you.") ||
              standardizeLine.equalsIgnoreCase("my a") ||
              standardizeLine.equalsIgnoreCase("lamar barb") ||
              standardizeLine.equalsIgnoreCase("best regards") ||
              standardizeLine.equalsIgnoreCase("\"tarde") ||
              standardizeLine.equalsIgnoreCase("wn u") ||
              standardizeLine.equalsIgnoreCase("yours, l") ||
              standardizeLine.equalsIgnoreCase("pd g no") ||
              standardizeLine.equalsIgnoreCase("sincer") ||
              standardizeLine.equalsIgnoreCase("~ c ct") ||
              standardizeLine.equalsIgnoreCase("sin unl") ||
              standardizeLine.equalsIgnoreCase("without reservation, | wholehearted]") ||
              standardizeLine.equalsIgnoreCase("sincerel") ||
                signOffs.exists(signOff => standardizeLine.contains(signOff))
            ) {
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
