package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.PDFUtils
import com.typesafe.config.ConfigFactory

import scala.collection.parallel.CollectionConverters._

object SplitPdfApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val pdfSplitPath = config.getString("pdfSplitPath")
  val dataDirPath = config.getString("dataPath")

  val dataDir = new File(dataDirPath)
  val lorPDFs = dataDir.listFiles().filter(f => !f.isDirectory && !f.getName.startsWith(".")).par

  lorPDFs.foreach(lorPDF => PDFUtils.splitDoc(lorPDF, pdfSplitPath))
}
