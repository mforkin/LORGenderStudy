package com.greenleaf.lor.ocr.pipeline.model.stats

import java.io.PrintWriter

import com.greenleaf.lor.ocr.pipeline.KeyParser
import com.greenleaf.lor.ocr.pipeline.model.UserMetaData

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class MostFrequentWords (val groupExtractor: (UserMetaData, String) => String, minWordLength: Int = 1)  extends GroupedStatisticalObject {
  val label = "MostFrequentWords"
  val mostFrequentWordsPerGroup = mutable.Map[String, (mutable.Map[String, Int], Int)]()

  override def updateInternalStatistic(userMetaData: UserMetaData, fileName: String, fileText: String): Unit = {
    val group = groupExtractor(userMetaData, KeyParser.fileNameToParticipantLetterId(fileName))
    val words = fileText.split(" ").filter(_.length >= minWordLength)
    val (groupData, numDocs) = mostFrequentWordsPerGroup.getOrElse(group, (mutable.Map[String, Int](), 0))
    words.foreach(word => {
      val cntForWord = groupData.getOrElse(word, 0) + 1
      groupData.update(word, cntForWord)
    })
    mostFrequentWordsPerGroup.update(group, (groupData, numDocs + 1))
  }

  override def toString: String = {
    mostFrequentWordsPerGroup.foldLeft(label + s" $minWordLength\n\n") {
      case (txt, (group, (groupData, docCount))) =>
        groupData.toList.sortBy(_._2)(Ordering.Int.reverse).take(50).foldLeft(txt + "\t" + group + "\n") {
          case (txt, (word, cnt)) =>
            txt + s"\t\t$word -> $cnt / $docCount -> ${cnt / docCount.toDouble}\n"
        }
    }
  }

  override def toCSV: Unit = {
    val keys = mostFrequentWordsPerGroup.keys.mkString("_")
    val topX = 50
    val fName = s"$label-top$topX-$minWordLength-$keys.csv"

    val headers = "group,word,averageCnt,totalCnt,totalDocs\n"
    val output = mostFrequentWordsPerGroup.foldLeft(headers) {
      case (o, (group, (groupData, docCount))) =>
        groupData.toList.sortBy(_._2)(Ordering.Int.reverse).take(topX).foldLeft(o) {
          case (txt, (word, cnt)) =>
            s"$txt$group,$word,${cnt / docCount.toDouble},$cnt,$docCount\n"
        }
    }

    val pw = new PrintWriter(s"/tmp/$fName")

    Try {
      pw.write(output)
      pw.flush()
    } match {
      case Success(value) => pw.close()
      case Failure(exception) =>
        pw.close()
        throw new Exception(s"Could write file $fName", exception)
    }

  }
}