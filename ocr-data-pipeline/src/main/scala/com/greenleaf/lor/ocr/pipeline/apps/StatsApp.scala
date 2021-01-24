package com.greenleaf.lor.ocr.pipeline.apps

import java.io.File
import java.util.regex.Pattern

import com.greenleaf.lor.ocr.pipeline.{FileHelper, KeyParser}
import com.greenleaf.lor.ocr.pipeline.model.{CategoryKey, UserMetaData}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.similarity.LevenshteinDistance

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

trait StatGroup {
  def label: String;
  def groupExtractor: (UserMetaData, String) => String;
}

class GroupWordStatResult(label: String, groupStat: Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)]) {
  override def toString: String = {
      groupStat.foldLeft(label + "\n\n") {
        case (output, (group, stats)) =>
            stats._1.toList.sortBy {
              case (_, (_, docCount)) => docCount
            }.foldLeft(output + "\t" + group + "\n") {
              case (output, (word, (totalCount ,docCount))) =>
                output + "\t\t" + word + "->" + (totalCount / groupStat(group)._3.toDouble) + "," + (docCount / groupStat(group)._3.toDouble) + "\n"
            }
      }
  }

  def getCategoryInfo: String = {
    groupStat.foldLeft(label + "\n\n") {
      case (output, (group, stats)) =>
        stats._2.toList.sortBy {
          case (_, docCount) => docCount
        }.foldLeft(output + "\t" + group + "\n") {
          case (output, (category, (docCount))) =>
            output + "\t\t" + category + "->" + (docCount / groupStat(group)._3.toDouble) + "\n"
        }
    }
  }
}

class WordCountStatGroup (
                           val groupExtractor: (UserMetaData, String) => String,
                           val label: String,
                           wordCategoryKey: CategoryKey
                     ) extends StatGroup {
  val distanceCalculator = new LevenshteinDistance()


  // groupStat: group, (word -> (totalCount, docCount))
  def updateStats (userMetaData: UserMetaData, fileName: String, fileText: String, groupStat: Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)], editDistance: Int): Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)] = {
    val group = groupExtractor(userMetaData, fileName)
    val tokenizedText = tokenizeText(fileText)
    val res = wordCategoryKey.groupings.foldLeft(groupStat) {
      case (groupStat, (category, words)) =>
        val (updatedStats, didUpdate) = words.foldLeft((groupStat, false)) {
          case (groupStat, word) =>
            if (word.contains(" ")) {
              val (newStat, didUpdate) = computeNewStat(group, word, tokenizedText, groupStat._1, editDistance);
              val (updatedStat, didUpdateTwo) = computeNewStat(group, word.replaceAll(" ", "-"), tokenizedText, newStat, editDistance)
              (updatedStat, didUpdate || didUpdateTwo || groupStat._2)
            } else {
              val (newStat, didUpdate) = computeNewStat(group, word, tokenizedText, groupStat._1, editDistance)
              (newStat, didUpdate || groupStat._2)
            }
        }
        val groupStats = updatedStats(group)
        val categoryStats = groupStats._2
        val updatedCategoryStats = categoryStats.updated(
          category,
          categoryStats.getOrElse(category, 0) + (if (didUpdate) 1 else 0)
        )
        updatedStats.updated(
          group,
          (groupStats._1, updatedCategoryStats, groupStats._3)
        )
    }
    val resGroup = res(group)
    res.updated(
      group,
      (resGroup._1, resGroup._2, resGroup._3 + 1)
    )
  }

  private def tokenizeText (text: String): Array[String] = {
    text.split(" ")
  }

  private def computeNewStat (
                       group: String,
                       word: String,
                       text: Array[String],
                       groupStat: Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)],
                       editDistance: Int): (Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)], Boolean) = {
    // get the existing group stats, or create a blank groupStat for the group
    val existingGroupStats: (Map[String, (Int, Int)], Map[String, Int], Int) = groupStat.getOrElse(group, (Map[String, (Int, Int)](), Map[String, Int](), 0))
    val existingGroup: Map[String, (Int, Int)] = existingGroupStats._1
    val existingCategoryInfo: Map[String, Int] = existingGroupStats._2
    val existingCategoryCnt: Int = existingGroupStats._3
    val cnt: Int = getWordCount(word, text, editDistance)
    val (existingTotalCount, existingDocCount) = existingGroup.getOrElse(word, (0, 0))
    val newWordStat: (Int, Int) = (existingTotalCount + cnt, existingDocCount + (if (cnt > 0) 1 else 0))
    val updatedData: Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)] = groupStat.updated(
      group,
      (existingGroup.updated(word, newWordStat), existingCategoryInfo, existingCategoryCnt)
    )
    (
      updatedData,
      cnt > 0
    )
  }

  private def getWordCount(word: String, text: Array[String], editDistance: Int = 0): Int = {
    if (word.contains("*")) {
      if (editDistance == 0) {
        getWordCountStrictRegEx(word, text)
      } else {
        getWordCountFuzzyRegEx(word, text, editDistance)
      }
    } else {
      if (editDistance == 0) {
        getWordCountStrict(word, text)
      } else {
        getWordCountFuzzy(word, text, editDistance)
      }
    }
  }

  private def getWordCountFuzzy (word: String, text: Array[String], editDistance: Int): Int = {
    text.foldLeft(0) {
      case (totalCnt, wordInText) =>
        totalCnt + (if(distanceCalculator.apply(wordInText, word) < editDistance) 1 else 0)
    }
  }

  // only handles * at the beginning or end of a word
  private def getWordCountFuzzyRegEx (word: String, text: Array[String], editDistance: Int): Int = {
    val startsWithStar = word.startsWith("*")
    val endsWithStar = word.endsWith("*")

    val testWord = word.replaceAll("[*]", "")

    text.foldLeft(0) {
      case (totalCnt, wordInText) =>
        val lengthDiff = wordInText.length - testWord.length
        if (lengthDiff < 0) {
          totalCnt
        } else {
          if (startsWithStar && endsWithStar) {
            totalCnt + (if (distanceCalculator.apply(wordInText, testWord) < editDistance + lengthDiff) 1 else 0)
          } else if (startsWithStar) {
            totalCnt + (if (distanceCalculator.apply(wordInText.drop(lengthDiff), testWord) < editDistance) 1 else 0)
          } else if (endsWithStar) {
            totalCnt + (if (distanceCalculator.apply(wordInText.dropRight(lengthDiff), testWord) < editDistance) 1 else 0)
          } else {
            throw new Exception("Unsupported Regex")
          }
        }

    }
  }

  private def getWordCountStrict (word: String, text: Array[String]): Int = {
    text.foldLeft(0) {
      case (totalCnt, wordInText) =>
        totalCnt + (if(wordInText.equalsIgnoreCase(word)) 1 else 0)
    }
  }

  private def getWordCountStrictRegEx (word: String, text: Array[String]): Int = {
    val regExString = "^" + word.replaceAll("[*]", ".*") + "$"
    val regEx = Pattern.compile(regExString, Pattern.CASE_INSENSITIVE)
    text.foldLeft(0) {
      case (totalCnt, wordInText) =>
        totalCnt + (if(regEx.matcher(wordInText).find()) 1 else 0)
    }
  }

}



object StatsApp extends App with StrictLogging {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val dataPath = config.getString("singleDocPath")
  val keyPath = config.getString("keyPath")

  val key: Seq[UserMetaData] = KeyParser.parseKey(keyPath)
  val textDir = new File(dataPath)

  val wordCategoryKey = CategoryKey.apply()

  val statGroups = List(
    /*new WordCountStatGroup(
      StatsAppHelper.extractRankFromMetaData,
      "Rank Group",
      wordCategoryKey
    ),
    new WordCountStatGroup(
      StatsAppHelper.extractIsProgramDirector,
      "Program Director Group",
      wordCategoryKey
    ),
     */
    new WordCountStatGroup(
      StatsAppHelper.extractApplicantIsWhite,
      "Is White Group",
      wordCategoryKey
    ),
    new WordCountStatGroup(
      StatsAppHelper.extractApplicantGender,
      "Gender Group",
      wordCategoryKey
    )
  )

  val txtFiles = textDir.listFiles().filter(f => {
    !f.isDirectory && !f.getName.startsWith(".")
  }).flatMap(f => {
    val pieces = f.getName.split("[.]")
    val letterId = pieces.head
    val participantNumber = pieces.head.replaceAll("[a-zA-Z]", "").toInt
    key.find(a => a.participantNumber == participantNumber) match {
      case Some(keyEntry) =>
        Some((f.getName, keyEntry, FileHelper.getTxtFromFile(f, " ")))
      case None =>
        logger.info("Couldn't find file in key: " + f.getName)
        None
    }
  })

  val results = statGroups.par.map {
    case (group) =>
      val stats = txtFiles.foldLeft(Map[String, (Map[String, (Int, Int)], Map[String, Int], Int)]()) {
        case (totalStats, (fileName, keyEntry, fileTxt)) =>
          group.updateStats(keyEntry, fileName, fileTxt, totalStats, 0)
      }
      new GroupWordStatResult(group.label, stats)
  }

  results.map(r => {
    //logger.info(r.toString)
    logger.info(r.getCategoryInfo)
  })
}

object StatsAppHelper extends StrictLogging {
  def extractApplicantIsWhite (userMetaData: UserMetaData, fileName: String): String = {
    userMetaData.race.equalsIgnoreCase("white").toString
  }

  def extractApplicantGender (userMetaData: UserMetaData, fileName: String): String = {
    userMetaData.gender
  }

  def extractRankFromMetaData (userMetaData: UserMetaData, fileName: String): String = {
    val fileLookup = fileName.split("[.]").head
    userMetaData.lorMetaData.find(_.fileName.equalsIgnoreCase(fileLookup)) match {
      case Some(data) => data.writerMetaData.rank
      case None =>
        logger.info("No key found for file " + fileName)
        "unknown"
    }
  }

  def extractIsProgramDirector (userMetaData: UserMetaData, fileName: String): String = {
    val fileLookup = fileName.split("[.]").head
    userMetaData.lorMetaData.find(_.fileName.equalsIgnoreCase(fileLookup)) match {
      case Some(data) => data.writerMetaData.isProgramDirector.toString
      case None =>
        logger.info("No key found for file " + fileName)
        "unknown"
    }
  }
}
