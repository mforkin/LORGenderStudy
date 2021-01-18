package com.greenleaf.lor.ocr.pipeline

import java.io.File

import com.typesafe.scalalogging.StrictLogging
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper

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
            page.save(outDir + File.separator + FileHelper.formatOutputName(file.getName, idx.toString))
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

  def getPageText (file: File, pageIndex: Int) = {
    Try(PDDocument.load(file)) match {
      case Success(pdf) =>
        val page = Try {
          val splitter = new Splitter()
          val pageIter = splitter.split(pdf).asScala
          pageIter.take(pageIndex + 1).last
        } match {
          case Success(page) => page
          case Failure(ex) =>
            pdf.close()
            throw new Exception(s"Failed to get page at index ${pageIndex}", ex)
        }
        val stripper = new PDFTextStripper()
        stripper.getText(page)
      case Failure(ex) =>
        throw new Exception(s"Failed to load pdf: ${file.getName}", ex)
    }
  }
}
