package com.greenleaf.lor.ocr.pipeline

import com.greenleaf.lor.ocr.pipeline.model.{LORStatistic, SimpleGenderId, TopWord}

object StatsUtils {
  def calcLORStatistic (text: String) = {
    val tokenizedText = text.split("[ ]")
    val (stat, cnt) = tokenizedText.foldLeft((SimpleGenderId(0,0,0,0), Map[String, Int]())) {
      case ((statistic, wordCnt), word) =>
        (
          if (word.equalsIgnoreCase("he")) {
            statistic.copy(numHe = statistic.numHe + 1)
          } else if (word.equalsIgnoreCase("him")) {
            statistic.copy(numHim = statistic.numHim + 1)
          } else if (word.equalsIgnoreCase("she")) {
            statistic.copy(numShe = statistic.numShe + 1)
          } else if (word.equalsIgnoreCase("her")) {
            statistic.copy(numHer = statistic.numHer + 1)
          } else {
            statistic
          },
          wordCnt.get(word) match {
            case Some(cnt) => wordCnt.updated(word, cnt + 1)
            case None => wordCnt.updated(word, cnt)
          }
        )
    }
    LORStatistic(
      tokenizedText.length,
      cnt.toSeq.sortBy {
        case (_, count) => count
      }.take(10).map {
        case (word, count) => TopWord(word, count)
      },
      stat
    )
  }
}
