package com.greenleaf.lor.ocr.pipeline.model

import java.util.Date

case class LORWriter (
                      name: String,
                      rank: String,
                      isProgramDirector: Boolean
                     )

case class LORMetaData (
                        fileName: String,
                        writerMetaData: LORWriter
                       )

case class UserMetaData (
                          participantNumber: Int,
                          medicalSchool: String,
                          gender: String,
                          race: String,
                          aoa: Boolean,
                          step1: Option[Int],
                          step2: Option[Int],
                          birthDate: Option[Date],
                          interviewDate: Option[Date],
                          lorMetaData: Seq[LORMetaData]
                        )
