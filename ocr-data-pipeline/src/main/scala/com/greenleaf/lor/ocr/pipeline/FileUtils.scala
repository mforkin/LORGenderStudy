package com.greenleaf.lor.ocr.pipeline

object FileUtils {
  def formatOutputName (fileName: String, toAppend: String): String = {
    val fileNameParts = fileName.split("[.]")
    fileNameParts.slice(0, fileNameParts.length - 1).mkString(".") + s"-$toAppend." + fileNameParts.last
  }
}
