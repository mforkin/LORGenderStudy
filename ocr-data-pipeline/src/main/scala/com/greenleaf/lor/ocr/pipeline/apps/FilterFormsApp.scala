package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.FileHelper
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils

import scala.collection.parallel.CollectionConverters._

object FilterFormsApp extends App {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val textPath = config.getString("textPath")
  val processedTextPath = config.getString("processedTextPath")
  val processedFormPath = config.getString("processedFormPath")
  val formPage1Path = processedFormPath + File.separator + "page1"
  val formPage2Path = processedFormPath + File.separator + "page2"

  val textDir = new File(textPath)

  val textFiles = textDir.listFiles().filter(f => !f.isDirectory && !f.getName.startsWith(".")).par

  textFiles.foreach(f => {
    val txt = FileHelper.getTxtFromFile(f)
    if (isFormPageOne(txt)) {
      FileUtils.copyFile(f, new File(formPage1Path + File.separator + f.getName))
    } else if (isFormPageTwo(txt)) {
      FileUtils.copyFile(f, new File(formPage2Path + File.separator + f.getName))
    } else {
      FileUtils.copyFile(f, new File(processedTextPath + File.separator + f.getName))
    }
  })

  def isFormPageOne (txt: String): Boolean = {
    txt.toLowerCase.contains("nature and amount of contact with the applicant") ||
      txt.toLowerCase.contains("performance to other trainees in the program") ||
      txt.toLowerCase.contains("best practice for use of the solr is to printA") ||
      txt.length < 100
  }

  def isFormPageTwo (txt: String): Boolean = {
    txt.toLowerCase.contains("if you worked with this applicant in a clinical")
  }
}
