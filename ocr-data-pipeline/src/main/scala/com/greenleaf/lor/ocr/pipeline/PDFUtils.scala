package com.greenleaf.lor.ocr.pipeline

import java.io.File

import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object PDFUtils {
  def splitDoc (filePath: String, outDir: String): Unit = {
    val file = new File(filePath)
    val pdf = PDDocument.load(file)

    val pages: Seq[PDDocument] = Try {
      val splitter = new Splitter()
      splitter.split(pdf).asScala.toSeq
    } match {
      case Success(docs) => docs
      case Failure(ex) =>
        pdf.close()
        throw new Exception(s"Failed to split pdf doc: ${filePath}", ex)
    }

    Try {
      for ((page, idx) <- pages.zipWithIndex) {
        page.save(outDir + File.separator + formatOutputName(file.getName, idx))
      }
    } match {
      case Success(_) =>
        pdf.close()
      case Failure(ex) =>
        pdf.close()
        throw new Exception(s"Failed to write split pdf: ${filePath}", ex)
    }
  }

  private def formatOutputName (fileName: String, pageNumber: Int): String = {
    val fileNameParts = fileName.split("[.]")
    fileNameParts.slice(0, fileNameParts.length - 1).mkString(".") + s"-$pageNumber" + fileNameParts.last
  }
}
