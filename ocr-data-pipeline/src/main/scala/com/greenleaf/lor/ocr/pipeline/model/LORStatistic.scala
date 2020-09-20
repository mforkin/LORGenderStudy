package com.greenleaf.lor.ocr.pipeline.model

case class SimpleGenderId (numHe: Int, numHim: Int, numShe: Int, numHer: Int)
case class TopWord (word: String, cnt: Int)
case class LORStatistic (wordCount: Int, topTenWords: Seq[TopWord], genderId: SimpleGenderId)
case class ApplicantStatistic (id: Int, stats: Seq[LORStatistic])

case class SummaryStatistic (avgWordCount: Double, topTenWords: Seq[TopWord], topFrequentWord: Seq[TopWord])
