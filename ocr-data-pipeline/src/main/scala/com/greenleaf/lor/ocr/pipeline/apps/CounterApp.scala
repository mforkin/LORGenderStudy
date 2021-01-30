package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File

import com.greenleaf.lor.ocr.pipeline.{FileHelper, KeyParser}
import com.greenleaf.lor.ocr.pipeline.model.stats.{AverageWordCount, MostFrequentWords, SignificanceStat, SpecificWordCount}
import com.greenleaf.lor.ocr.pipeline.model.{CategoryKey, UserMetaData}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

object CounterApp extends App with StrictLogging {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val dataPath = config.getString("singleDocPath")
  val keyPath = config.getString("keyPath")

  val key: Seq[UserMetaData] = KeyParser.parseKey(keyPath)
  val textDir = new File(dataPath)

  val wordCategoryKey = CategoryKey.apply()

  val stats = List(
    new AverageWordCount(
      StatsAppHelper.extractApplicantIsWhite,
      "AverageWordCount_isWhite_minLength1",
      1
    ),
    new AverageWordCount(
      StatsAppHelper.extractApplicantIsWhite,
      "AverageWordCount_isWhite_minLength5",
      5
    ),
    new AverageWordCount(
      StatsAppHelper.extractApplicantGender,
      "AverageWordCount_gender_minLength1",
      1
    ),
    new AverageWordCount(
      StatsAppHelper.extractApplicantGender,
      "AverageWordCount_gender_minLength5",
      5
    ),
    new MostFrequentWords(
      StatsAppHelper.extractApplicantIsWhite,
      "MostFrequentWords_isWhite_minLength5",
      5
    ),
    new MostFrequentWords(
      StatsAppHelper.extractApplicantGender,
      "MostFrequentWords_gender_minLength5",
      5
    )
  )

  val specificWordCountStats = List(
    new SpecificWordCount(
      StatsAppHelper.extractApplicantIsWhite,
      wordCategoryKey,
      "specificWordCount_isWhite_minDist0"
    ),
    new SpecificWordCount(
      StatsAppHelper.extractApplicantGender,
      wordCategoryKey,
      "specificWordCount_gender_minDist0"
    ),
    new SpecificWordCount(
      StatsAppHelper.extractApplicantIsWhite,
      wordCategoryKey,
      "specificWordCount_isWhite_minDist1",
      1
    ),
    new SpecificWordCount(
      StatsAppHelper.extractApplicantGender,
      wordCategoryKey,
      "specificWordCount_gender_minDist1",
      1
    )
  )

  val txtFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  }).flatMap(f => {
    val participantId = KeyParser.fileNameToParticipantId(f.getName)
    key.find(_.participantNumber == participantId) match {
      case Some(keyEntry) =>
        Some((f.getName, keyEntry, FileHelper.getTxtFromFile(f, " ")))
      case None =>
        logger.info(s"Couldn't find file in key: ${f.getName} -> $participantId")
        None
    }
  })

  txtFiles.foreach {
    case (fileName, keyEntry, fileTxt) =>
      stats.par.foreach(_.updateInternalStatistic(keyEntry, fileName, fileTxt))
      specificWordCountStats.par.foreach(_.updateInternalStatistic(keyEntry, fileName, fileTxt))
  }

  stats.foreach(_.toCSV)

  logger.info("\n\n ---- Significance Stats ---- \n\n")

  val significancesStats = specificWordCountStats.map {
    case stat =>
      val ss = new SignificanceStat(stat, wordCategoryKey, stat.label + "sigStats")
      ss.calculateAll()
      ss
  }

  significancesStats.foreach(_.toCSV)
}
