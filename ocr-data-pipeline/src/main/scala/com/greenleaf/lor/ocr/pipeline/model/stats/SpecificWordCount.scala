package com.greenleaf.lor.ocr.pipeline.model.stats

import java.util.regex.Pattern

import com.greenleaf.lor.ocr.pipeline.KeyParser
import com.greenleaf.lor.ocr.pipeline.model.{CategoryKey, UserMetaData}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.similarity.LevenshteinDistance

import scala.collection.mutable

class SpecificWordCount (
                          val groupExtractor: (UserMetaData, String) => String,
                          categoryKey: CategoryKey,
                          val label: String,
                          minDistance: Int = 0,
                        ) extends GroupedStatisticalObject {
  // Group -> Word -> totalCount, docCount
  val specificWordCountsPerGroup = mutable.Map[String, mutable.Map[String, (Int, Int)]]()

  // Group -> docId -> word -> count
  val groupDocToWordCnt = mutable.Map[String, mutable.Map[String, mutable.Map[String, Int]]]()

  val docsPerGroup = mutable.Map[String, Int]()

  override def toString: String = {
    docsPerGroup.foldLeft("\nTotal Docs:\n\n") {
      case (str, (group, cnt)) => s"$str\t$group -> $cnt\n"
    } +
      specificWordCountsPerGroup.foldLeft("\n\n\nCnt per Group") {
        case (str, (group, wordMap)) => wordMap.toList.sortBy(_._2._1)(Ordering.Int.reverse).take(20).foldLeft(s"$str\t$group\n") {
          case (str, (word, (total, docs))) => s"$str\t\t$word -> ($total, $docs) (${total/(docsPerGroup(group).toDouble)}, ${docs/(docsPerGroup(group).toDouble)})\n"
        }
      }
  }

  override def toCSV: Unit = {

  }

  def updateInternalStatistic(userMetaData: UserMetaData, fileName: String, fileText: String): Unit = {
    val group = groupExtractor(userMetaData, KeyParser.fileNameToParticipantLetterId(fileName))
    val groupWordCounts = specificWordCountsPerGroup.getOrElse(group, mutable.Map[String, (Int, Int)]())
    val groupDocCounts = groupDocToWordCnt.getOrElse(group, mutable.Map[String, mutable.Map[String, Int]]())
    val letterId = KeyParser.fileNameToParticipantLetterId(fileName)
    val docCounts = groupDocCounts.getOrElse(letterId, mutable.Map[String, Int]())
    val words: Set[String] = categoryKey.groupings.flatMap(_._2).toSet
    words.foreach {
      case word =>
        val cnt = WordCntHelper.getWordCount(word, fileText, minDistance)
        val (totalCnt, totalDocs) = groupWordCounts.getOrElse(word, (0, 0))
        val docCnt = docCounts.getOrElse(word, 0)
        groupWordCounts.put(word, (totalCnt + cnt, totalDocs + (if (cnt > 0) 1 else 0)))
        docCounts.put(word, docCnt + cnt)
    }
    specificWordCountsPerGroup.put(group, groupWordCounts)
    groupDocCounts.put(letterId, docCounts)
    groupDocToWordCnt.put(group, groupDocCounts)
    docsPerGroup.put(group, docsPerGroup.getOrElse(group, 0) + 1)
  }
}

object WordCntHelper extends StrictLogging {
  val distanceCalculator = new LevenshteinDistance()
  def getWordCount (word: String, text: String, editDistance: Int): Int = {
    if (word.contains("*")) {
      if (editDistance == 0) {
        getWordCountRegExStrict(word, text)
      } else {
        getWordCountRegExFuzzy(word, text, editDistance)
      }
    } else {
      if (editDistance == 0) {
        getWordCountStrict(word, text)
      } else {
        getWordCountFuzzy(word, text, editDistance)
      }
    }
  }

  def getWordCountFuzzy (word: String, text: String, editDistance: Int): Int = {
    if (word.contains(" ")) {
      text.split(" ").sliding(word.split(" ").length - 1).foldLeft(0) {
        case (cnt, window) =>
          cnt + (if (distanceCalculator.apply(word, window.mkString(" ")) < editDistance) 1 else 0)
      }
    } else {
      text.split(" ").foldLeft(0) {
        case (cnt, wordInText) =>
          cnt + (if (distanceCalculator.apply(wordInText, word) < editDistance) 1 else 0)
      }
    }
  }

  def getWordCountStrict (word: String, text: String): Int = {
    if (word.contains(" ")) {
      getWordCountRegExStrict(word, text)
    } else {
      text.split(" ").foldLeft(0) {
        case (cnt, wordInText) =>
          cnt + (if (wordInText.equalsIgnoreCase(word)) 1 else 0)
      }
    }
  }

  def getWordCountRegExFuzzy (word: String, text: String, editDistance: Int): Int = {
    if (word.contains(" ")) {
      logger.trace("Can't fuzzy match, fallback to strict regex")
      getWordCountRegExStrict(word, text)
    } else {
      val startsWithStar = word.startsWith("*")
      val endsWithStar = word.endsWith("*")

      val testWord = word.replaceAll("[*]", "")

      text.split(" ").foldLeft(0) {
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
              logger.trace("Can't fuzzy match, fallback to strict regex")
              getWordCountRegExStrict(word, text)
            }
          }

      }
    }
  }

  def getWordCountRegExStrict (word: String, text: String): Int = {
    if (word.contains(" ")) {
      val regExString = word.replaceAll("[*]", "[a-zA-Z]*")
      val regEx = Pattern.compile(regExString, Pattern.CASE_INSENSITIVE)
      regEx.matcher(text).results().count().toInt
    } else {
      val regExString = "^" + word.replaceAll("[*]", "[a-zA-Z]*") + "$"
      val regEx = Pattern.compile(regExString, Pattern.CASE_INSENSITIVE)
      text.split(" ").foldLeft(0) {
        case (cnt, wordInText) =>
          cnt + (if (regEx.matcher(wordInText).find()) 1 else 0)
      }
    }
  }
}
