package com.greenleaf.lor.ocr.pipeline.model.stats

import com.greenleaf.lor.ocr.pipeline.model.UserMetaData

trait StatisticalObject {
  def label: String
  def groupExtractor: (UserMetaData, String) => String
}

abstract class GroupedStatisticalObject extends StatisticalObject {
  def updateInternalStatistic (userMetaData: UserMetaData, fileName: String, fileText: String): Unit
}