package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.model.ApplicantStatistic
import com.greenleaf.lor.ocr.pipeline.{FileHelper, KeyParser, StatsUtils}
import com.typesafe.config.ConfigFactory

import scala.collection.parallel.CollectionConverters._

object BasicAnalysisApp {
  val config = ConfigFactory.load().getConfig("com.greeleaf.lor")
  val keyPath = config.getString("keyPath")
  val processedTextPath = config.getString("processedTextPath")

  val key = KeyParser.parseKey(keyPath)
  val keyMap = key.map(u => (u.participantNumber, u)).toMap

  val textDir = new File(processedTextPath)

  val textFiles = textDir.listFiles().filter(f => !f.isDirectory && !f.getName.startsWith(".")).par

  val applicantStats = textFiles.foldLeft(Map[Int, Seq[ApplicantStatistic]]()) {
    case (applicantMap, f) =>
      val applicantId = FileHelper.getApplicantIdFromFile(f.getName)
      val stat = StatsUtils.calcLORStatistic(
        FileHelper.getTxtFromFile(f)
      )
      applicantMap.get(applicantId) match {
        case Some(stats) =>
          applicantMap.updated(applicantId, stats :+ stat)
        case None =>
          applicantMap.updated(applicantId, Seq(stat))
      }
  }.values.flatten.toSeq

  // --------------------------
  // @TODO filter these out, but for now fastest to just hard code since we only have two
  def getSummaryRace (stats: Seq[ApplicantStatistic])  = {
    stats.groupBy {
      case stat => keyMap.get(stat.id)
        .map(applicantData => {
          val race = applicantData.race
          if (race.equalsIgnoreCase("WHITE")) {
            race
          } else {
            "NON-WHITE"
          }
        })
        .getOrElse("unknown")
    }

  }

  def getSummaryGender (stats: Seq[ApplicantStatistic]) = {

  }

  // --------------------------

  def computeSummary ()
}
