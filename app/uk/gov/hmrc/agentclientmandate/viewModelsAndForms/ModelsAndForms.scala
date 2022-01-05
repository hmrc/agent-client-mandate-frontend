/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentclientmandate.viewModelsAndForms

import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms}
import play.api.libs.json.{Json, OFormat}

case class YesNoQuestion(yesNo: Boolean)

object YesNoQuestion {
  implicit val formats: OFormat[YesNoQuestion] = Json.format[YesNoQuestion]
}

class YesNoQuestionForm(errorMessage: String) {

  def yesNoQuestionForm =
    Form(
      mapping(
        "yesNo" -> Forms.of[Boolean](requiredBooleanFormatter)
      )(YesNoQuestion.apply)(YesNoQuestion.unapply)
    )

  def requiredBooleanFormatter: Formatter[Boolean] = new Formatter[Boolean] {
    def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      Right(data.getOrElse(key,"")).right.flatMap {
        case "true"   => Right(true)
        case "false"  => Right(false)
        case _        => Left(Seq(FormError(key, errorMessage)))
      }
    }

    def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }
}
