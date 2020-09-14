package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.OCRUtils
import com.typesafe.config.ConfigFactory

import scala.collection.parallel.CollectionConverters._

object ConvertApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val imagePath = config.getString("imagePath")
  val textPath = config.getString("textPath")
  val tessDataPath = config.getString("tessDataPath")

  val imagesDir = new File(imagePath)

  val images = imagesDir.listFiles().filter(f => !f.isDirectory && !f.getName.startsWith(".")).par

  images.foreach(lorImage => OCRUtils.convertDoc(lorImage, textPath, tessDataPath))
}