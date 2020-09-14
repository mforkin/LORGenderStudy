package com.greenleaf.lor.ocr.pipeline

import java.io.File

import com.typesafe.scalalogging.StrictLogging
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object PDFUtils extends StrictLogging {
  def splitDoc (file: File, outDir: String): Unit = {
    Try(PDDocument.load(file)) match {
      case Success(pdf) =>
        val pages: Seq[PDDocument] = Try {
          val splitter = new Splitter()
          splitter.split(pdf).asScala.toSeq
        } match {
          case Success(docs) => docs
          case Failure(ex) =>
            pdf.close()
            throw new Exception(s"Failed to split pdf doc: ${file.getName}", ex)
        }

        Try {
          for ((page, idx) <- pages.zipWithIndex) {
            page.save(outDir + File.separator + FileUtils.formatOutputName(file.getName, idx.toString))
          }
        } match {
          case Success(_) =>
            pdf.close()
          case Failure(ex) =>
            pdf.close()
            throw new Exception(s"Failed to write split pdf: ${file.getName}", ex)
        }
      case Failure(ex) =>
        logger.error(s"Failed to load pdf: ${file.getName}")
    }


  }

  def splitDoc (filePath: String, outDir: String): Unit = {
    val file = new File(filePath)
    splitDoc(file, outDir)
  }
}
