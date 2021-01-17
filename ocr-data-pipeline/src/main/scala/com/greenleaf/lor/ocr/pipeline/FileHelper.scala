package com.greenleaf.lor.ocr.pipeline

import java.io.File

import scala.io.Source

object FileHelper {
  def formatOutputName (fileName: String, toAppend: String): String = {
    val fileNameParts = fileName.split("[.]")
    fileNameParts.slice(0, fileNameParts.length - 1).mkString(".") + s"-$toAppend." + fileNameParts.last
  }

  def getTxtLinesFromFile (f: File): Seq[String] = {
    val source = Source.fromFile(f)
    try {
        source.getLines().toList
    } catch {
      case ex: Throwable => throw new Exception (s"Unable to load file content: ${f.getName}", ex)
    } finally {
      source.close()
    }
  }


  def getTxtFromFile (f: File, joiner: String = " "): String = {
    val source = Source.fromFile(f)
    try {
      source.getLines().mkString(joiner)
    } catch {
      case ex: Throwable => throw new Exception (s"Unable to load file content: ${f.getName}", ex)
    } finally {
      source.close()
    }
  }
}
