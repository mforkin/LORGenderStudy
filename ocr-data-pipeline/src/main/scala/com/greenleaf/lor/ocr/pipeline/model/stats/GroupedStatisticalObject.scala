package com.greenleaf.lor.ocr.pipeline.model.stats

import com.greenleaf.lor.ocr.pipeline.model.UserMetaData

trait StatisticalObject {
  def label: String
  def groupExtractor: (UserMetaData, String) => String
}

trait Outputable {
  def toCSV: Unit
}

abstract class GroupedStatisticalObject extends StatisticalObject with Outputable {
  def updateInternalStatistic (userMetaData: UserMetaData, fileName: String, fileText: String): Unit
}