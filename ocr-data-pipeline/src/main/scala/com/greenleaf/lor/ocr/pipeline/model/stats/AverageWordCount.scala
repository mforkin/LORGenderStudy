package com.greenleaf.lor.ocr.pipeline.model.stats

import java.io.PrintWriter

import com.greenleaf.lor.ocr.pipeline.KeyParser
import com.greenleaf.lor.ocr.pipeline.model.UserMetaData

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class AverageWordCount (val groupExtractor: (UserMetaData, String) => String, minWordLength: Int = 1)  extends GroupedStatisticalObject {
  val label = "AverageWordCount"
  //group -> (totalWords, totalDocs)
  val wordCountsPerGroup = mutable.Map[String, (Int, Int)]()
  def updateInternalStatistic(userMetaData: UserMetaData, fileName: String, fileText: String): Unit = {
    val group = groupExtractor(userMetaData, KeyParser.fileNameToParticipantLetterId(fileName))
    val numberOfWords = fileText.split(" ").count(_.length >= minWordLength)
    val (totalWords, totalDocs) = wordCountsPerGroup.getOrElse(group, (0, 0))
    wordCountsPerGroup.update(group, (totalWords + numberOfWords, totalDocs + 1))
  }

  override def toString: String = {
    wordCountsPerGroup.foldLeft(label + s" $minWordLength\n\n") {
      case (txt, (group, (totalWords, totalDocs))) =>
        s"$txt \t$group: $totalWords / $totalDocs -> ${totalWords / totalDocs.toDouble} \n"
    }
  }

  def toCSV: Unit = {
    val keys = wordCountsPerGroup.keys.mkString("_")
    val fName = s"$label-min_$minWordLength-$keys.csv"

    val headers = "group,average,totalWords,totalDocs\n"
    val output = wordCountsPerGroup.foldLeft(headers) {
      case (o, (group, (totalCnt, totalDoc))) =>
        s"$o$group,${totalCnt/totalDoc.toDouble},$totalCnt,$totalDoc\n"
    }

    val pw = new PrintWriter(s"/tmp/$fName")

    Try {
      pw.write(output)
      pw.flush()
    } match {
      case Success(_) => pw.close()
      case Failure(exception) =>
        pw.close()
        throw new Exception(s"Could write $fName", exception)
    }
  }
}
