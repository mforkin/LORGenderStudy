package com.greenleaf.lor.ocr.pipeline.model.stats
import com.greenleaf.lor.ocr.pipeline.model.CategoryKey
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.math3.stat.inference.TTest

import scala.collection.mutable

class SignificanceStat (
                        specificWordCount: SpecificWordCount,
                        categoryKey: CategoryKey
                       ) extends StrictLogging {

  val ttest = new TTest()

  def calculateAll (): Unit = {
    val categoriesPerDoc: Seq[(String, Map[String, mutable.Map[String, Int]])] = getCategoriesPerDocument().toList
    categoriesPerDoc.sliding(2).foreach(window => {
      val (group1Name, _) = window.head
      val (group2Name, _) = window(1)
      calculate(group1Name, group2Name)
    })
  }
  def calculate (group1: String, group2: String): Unit = {
    val categoriesPerDoc = getCategoriesPerDocument()
    val group1Samples = categoriesPerDoc(group1)
    val group2Samples = categoriesPerDoc(group2)

    val res = categoryKey.groupings.map {
      case (category, _) =>
        val sample1 = group1Samples(category).values.map(_.toDouble)
        val sample2 = group2Samples(category).values.map(_.toDouble)
        category -> ttest.tTest(sample1.toArray, sample2.toArray)
    }

    logger.info(
      res.foldLeft(s"\npValues $group1 <-> $group2:\n\n") {
        case (category, pVal) => s"\t$category -> $pVal\n"
      }
    )
  }

  // returns group -> category -> docId -> cnt
  def getCategoriesPerDocument (): mutable.Map[String, Map[String, mutable.Map[String, Int]]] ={
    specificWordCount.groupDocToWordCnt.map {
      case (group, docToWordCnt) =>
        group -> categoryKey.groupings.map {
          case (category, words) =>
            category -> docToWordCnt.map {
              case (docId, wordCnt) =>
                docId -> words.foldLeft(0) {
                  case (total, word) =>
                    total + wordCnt.getOrElse(word, 0)
                }
            }
        }
    }
  }
}
