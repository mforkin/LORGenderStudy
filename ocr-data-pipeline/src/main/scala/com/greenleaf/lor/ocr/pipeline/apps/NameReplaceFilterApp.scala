package com.greenleaf.lor.ocr.pipeline.apps

import java.io.{File, PrintWriter}

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory

import scala.collection.parallel.CollectionConverters.ArrayIsParallelizable
import scala.util.{Failure, Success, Try}
import collection.JavaConverters

object NameReplaceFilterApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val cleanSignOffPath = config.getString("cleanSignOffTextPath")
  val sanitizedNamePath = config.getString("sanitizedNameTextPath")
  val idNameMapPath = config.getString("rawDataBasePath") + File.separator + "idNameMap.tsv"

  val idNameMap = FileHelper.getTxtLinesFromFile(new File(idNameMapPath)).map(line => {
    val pieces = line.split("\t")
    pieces.head.toInt -> (
      pieces(1).toLowerCase,
      pieces(2).toLowerCase,
      Try(pieces(3)).toOption.map(_.toLowerCase)
    )
  }).toMap

  val textDir = new File(cleanSignOffPath)

  val textFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  }).par

  textFiles.foreach(f => {
    val participantNumber = f.getName.split("-").head.replaceAll("[a-zA-Z]", "").toInt

    val (last, first, middle) = idNameMap(participantNumber)

    val txt = middle.foldLeft(
      FileHelper.getTxtFromFile(f, "\n").toLowerCase
        .replaceAll(last, "*-*-*-*-*-")
        .replaceAll(first, "_+_+_+_+_+")
    ) {
      case (tot, m) => tot.replaceAll(m, "-=-=-=-=-=")
    }

    val pw = new PrintWriter(sanitizedNamePath + File.separator + f.getName)
    Try {
      pw.write(txt)
    } match {
      case Success(_) => pw.close()
      case Failure(ex) =>
        pw.close()
        throw new Exception(s"Could write name sanitized file ${f.getName}", ex)
    }
  })


}
