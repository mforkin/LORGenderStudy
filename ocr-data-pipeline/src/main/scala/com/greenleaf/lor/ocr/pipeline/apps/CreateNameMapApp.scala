package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.PDFUtils
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object CreateNameMapApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val basePath = config.getString("rawDataBasePath")
  val dirs = config.getStringList("rawDataDirs")

  val nameMap = mutable.Map[Int, (String, String, String)]()

  def parseName (f: File, pageIndex: Int) = {
    val index1Text = PDFUtils.getPageText(f, pageIndex).toUpperCase
    val nameRow = index1Text.split("\n").filter(line => line.toUpperCase.startsWith("NAME:")).head
    val name = nameRow.replaceAll("NAME:", "").trim()
    val participantNumber = f.getName.split(" ").last.split("[.]").head.toInt
    val namePieces = name.split(",").map(p => p.trim())
    val lastName = namePieces.head.trim
    val restPieces = namePieces(1).split(" ")
    val firstName = restPieces.head.trim
    val rest = restPieces.tail.mkString(" ")

    nameMap.put(
      participantNumber,
      (
        lastName.replaceAll("[^a-zA-Z]", ""),
        firstName.replaceAll("[^a-zA-Z]", ""),
        rest.replaceAll("[^a-zA-Z ]", "").trim
      )
    )
  }

  dirs.forEach(dir => {
    val d = new File(basePath + File.separator + dir)

    val files = d.listFiles().filter(f => {
      !f.isDirectory && !f.getName.startsWith(".")
    })

    files.foreach(f => {
      Try {
        parseName(f, 1)
      } match {
        case Success(_) =>
        case Failure(ex) =>
          Try {
            parseName(f, 0)
          } match {
            case Success(_) =>
            case Failure(ex) =>
              Try {
                parseName(f, 2)
              } match {
                case Success(_) =>
                case Failure(ex) =>
                  throw new Exception(s"Failed to extract name for file: ${f.getName}", ex)
              }
          }
      }
    })
  })

  val pw = new PrintWriter(basePath + File.separator + "idNameMap.tsv")
  Try {
    nameMap.foreach {
      case (pId, (last, first, rest)) =>
        pw.write(s"$pId\t$last\t$first\t$rest\n")
    }
  } match {
    case Success(_) => pw.close()
    case Failure(ex) =>
      pw.close()
      throw new Exception("unable to write map", ex)
  }
}
