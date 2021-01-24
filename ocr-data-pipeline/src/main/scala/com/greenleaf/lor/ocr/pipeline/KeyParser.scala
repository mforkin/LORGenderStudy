package com.greenleaf.lor.ocr.pipeline

import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date

import com.greenleaf.lor.ocr.pipeline.model.{LORMetaData, LORWriter, UserMetaData}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}

import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

object KeyParser extends StrictLogging {
  val dateFormatter = new SimpleDateFormat("DD/MM/YYYY")

  def parseKey (fileName: String): Seq[UserMetaData] = {
    val source = new FileReader(fileName)
    val parser = new CSVParser(source, CSVFormat.DEFAULT.withHeader())

    Try (
      parser.asScala.foldLeft(Map[Int, UserMetaData]())((metaData, record) => {
        val id = record.get(0).toInt
        val existingMetaData = metaData.get(id)
        val userData = existingMetaData match {
          case Some(d) => d
          case None => extractUserData(record)
        }
        val LORData = extractLORData(record)
        metaData.updated(
          id,
          userData.copy(
            lorMetaData = (userData.lorMetaData :+ LORData)
          )
        )
      })
    ) match {
      case Success(userMetaData) =>
        parser.close()
        source.close()
        userMetaData.values.toSeq
      case Failure(ex) =>
        parser.close()
        source.close()
        throw new Exception(s"Failed to parse key file: ${fileName}", ex)
    }
  }

  private def extractUserData (record: CSVRecord) =
    UserMetaData (
      extractByHeader(record, "Participant Number").toInt,
      extractByHeader(record, "Medical School"),
      extractByHeader(record, "Gender"),
      extractByHeader(record, "Race (Self Identification)"),
      extractByHeader(record, "Alpha Omega Alpha (AOA) Status").equalsIgnoreCase("YES"),
      extractStepScore(extractByHeader(record, "USMLE Score (Step 1)")),
      extractStepScore(extractByHeader(record, "USMLE Score (Step 2)")),
      extractDate(extractByHeader(record, "Birth Date")),
      extractDate(extractByHeader(record, "Interview Date")),
      Seq()
    )

  private def extractLORData (record: CSVRecord) =
    LORMetaData(
      extractByHeader(record, "Letter of Recommendation File"),
      LORWriter(
        extractByHeader(record, "Letter Writer's Name"),
        extractByHeader(record, "Letter Writer's Academic Rank"),
        extractByHeader(record, "Anesthesiology Residency Program Director").equalsIgnoreCase("YES")
      )
    )

  private def extractByHeader (record: CSVRecord, header: String): String =
    record.get(header).toUpperCase().trim()

  private def extractStepScore (score: String): Option[Int] = {
    Try(score.toInt).toOption
  }

  private def extractDate (date: String): Option[Date] = {
    Try(dateFormatter.parse(date)).toOption
  }

  def fileNameToParticipantLetterId(fileName: String): String = fileName.split("[.]").head
  def fileNameToParticipantId(fileName: String): Int = fileNameToParticipantLetterId(fileName).replaceAll("[a-zA-Z]", "").toInt
}
