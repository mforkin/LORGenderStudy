package com.greenleaf.lor.ocr.pipeline

import java.io.{File, PrintWriter}

import net.sourceforge.tess4j.ITessAPI.TessPageSegMode
import net.sourceforge.tess4j.Tesseract

import scala.util.{Failure, Success, Try}

object OCRUtils {
  def convertDoc (imgFile: File, outPath: String, tessDataPath: String): Unit = {
    val tesseract = new Tesseract ()
    tesseract.setPageSegMode(TessPageSegMode.PSM_AUTO_OSD)
    val outNamePieces = imgFile.getName.split("[.]")
    val outName = outNamePieces.slice(0, outNamePieces.length - 1).mkString(".") + ".txt"
    val pw = new PrintWriter(outPath + File.separator + outName, "UTF-8")
    Try {
      tesseract.setDatapath(tessDataPath)
      val text = tesseract.doOCR(imgFile)
      pw.write(text)
    } match {
      case Success(_) => pw.close()
      case Failure(ex) =>
        pw.close()
        throw new Exception(s"Failed to generate text from file: ${imgFile.getName}", ex)
    }
  }

  def convertDoc (filePath: String, outPath: String, tessDataPath: String): Unit =
    convertDoc(new File(filePath), outPath, tessDataPath)
}
