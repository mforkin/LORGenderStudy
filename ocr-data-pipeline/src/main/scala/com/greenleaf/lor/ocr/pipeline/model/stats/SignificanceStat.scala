package com.greenleaf.lor.ocr.pipeline.model.stats
import java.io.PrintWriter

import com.greenleaf.lor.ocr.pipeline.model.CategoryKey
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.math3.stat.inference.TTest

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

class SignificanceStat (
                        specificWordCount: SpecificWordCount,
                        categoryKey: CategoryKey
                       ) extends StrictLogging with Outputable {

  val ttest = new TTest()

  val significanceValues = mutable.Map[String, Double]()
  val groupCategoryCnts = mutable.Map[String, (String, Double, Int)]()

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

    categoryKey.groupings.map {
      case (category, _) =>
        val sample1 = group1Samples(category).values.map(_.toDouble)
        val sample2 = group2Samples(category).values.map(_.toDouble)
        groupCategoryCnts.put(category, (group1, sample1.sum, sample1.size))
        groupCategoryCnts.put(category, (group2, sample2.sum, sample2.size))
        significanceValues.put(s"${category}_-_${group1}_-_$group2", ttest.tTest(sample1.toArray, sample2.toArray))
    }
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

  def toCSV: Unit = {
    val significanceFName = "/tmp/SignificanceStats.csv"
    val headers = "category,group1,group2,significance\n"
    val output = significanceValues.foldLeft(headers) {
      case (o, (label, sig)) =>
        val Array(category, group1, group2) = label.split("[_][-][_]")
        s"$o$category,$group1,$group2,$sig\n"
    }

    val pw = new PrintWriter(significanceFName)
    Try {
      pw.write(output)
    } match {
      case Success(_) => pw.close()
      case Failure(exception) =>
        pw.close()
        throw new Exception("Couldn't write significances", exception)
    }

    val groupCntsFName = "/tmp/CategoryCounts.csv"
    val groupCntsHeaders = "category,group,averageCnt,totalcnt,totalDocs\n"
    val groupCntsoutput = groupCategoryCnts.foldLeft(groupCntsHeaders) {
      case (o, (category, (group, totalCnt, docCount))) =>
        s"$o$category,$group,${totalCnt/docCount.toDouble},$totalCnt,$docCount\n"
    }

    val grpPW = new PrintWriter(groupCntsFName)
    Try {
      grpPW.write(groupCntsoutput)
    } match {
      case Success(_) => pw.close()
      case Failure(exception) =>
        pw.close()
        throw new Exception("Couldn't write grpCategoryCnt", exception)
    }
  }
}
