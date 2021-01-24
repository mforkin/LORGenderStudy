package com.greenleaf.lor.ocr.pipeline.model.stats

import com.greenleaf.lor.ocr.pipeline.KeyParser
import com.greenleaf.lor.ocr.pipeline.model.UserMetaData

import scala.collection.mutable

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
}
