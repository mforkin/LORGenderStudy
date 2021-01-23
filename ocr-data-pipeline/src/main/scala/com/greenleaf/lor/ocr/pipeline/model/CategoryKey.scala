package com.greenleaf.lor.ocr.pipeline.model

import java.io.FileReader

import com.typesafe.config.ConfigFactory
import org.apache.commons.csv.{CSVFormat, CSVParser}

import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters._

case class CategoryKey (groupings: Map[String, Set[String]])

object CategoryKey {
  val config = ConfigFactory.load().getConfig("com.greenleaf.lor")
  val categoryKeyPath = config.getString("categoryKeyPath")
  def apply(): CategoryKey = parseKeyFile(categoryKeyPath)

  def parseKeyFile (keyPath: String) = {
    val source = new FileReader(keyPath)
    val parser = new CSVParser(source, CSVFormat.DEFAULT.withHeader())
    Try {
      val categories = parser.getHeaderNames.asScala.toList
      parser.asScala.foldLeft(Map[String, Set[String]]()) {
        case (key, row) =>
          categories.foldLeft(key) {
            (key, category) =>
              Option(row.get(category)) match {
                case Some(word) =>
                  key.get(category) match {
                    case Some(wordSet) => key.updated(category, wordSet + word)
                    case None => key.updated(category, Set(word))
                  }
                case None => key
              }
          }
      }
    } match {
      case Success(value) =>
        parser.close()
        source.close()
        CategoryKey(value)
      case Failure(ex) =>
        parser.close()
        source.close()
        throw new Exception("Couldn't parse category key file: " + keyPath, ex)
    }


  }
}


