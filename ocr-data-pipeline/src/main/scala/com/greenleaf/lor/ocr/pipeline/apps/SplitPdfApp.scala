package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.{KeyParser, PDFUtils}
import com.typesafe.config.ConfigFactory

import scala.collection.parallel.CollectionConverters._

object SplitPdfApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val keyPath = config.getString("keyPath")
  val pdfSplitPath = config.getString("pdfSplitPath")
  val dataDir = config.getString("dataPath")

  val applicants = KeyParser.parseKey(keyPath).par

  applicants.foreach(applicant => {
    applicant.lorMetaData.foreach(lor => {
        PDFUtils.splitDoc(dataDir + File.pathSeparator + lor.fileName + ".pdf", pdfSplitPath)
    })
  })
}
