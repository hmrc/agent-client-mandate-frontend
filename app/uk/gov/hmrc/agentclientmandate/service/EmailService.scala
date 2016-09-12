/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.service

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.connectors.EmailServiceConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import play.mvc.Http.Status.OK

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object EmailService extends EmailService {
  val emailServiceConnector: EmailServiceConnector = EmailServiceConnector
}

case class EmailResponse(valid: Boolean)

object EmailResponse {
  implicit val formats = Json.format[EmailResponse]
}

trait EmailService {

  def emailServiceConnector: EmailServiceConnector

  def validate(email: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    emailServiceConnector.validate(email) map { response =>
      response.status match {
        case OK => response.json.as[EmailResponse].valid
        case status =>
          Logger.warn(s"[EmailService][validate] - status = $status, responseBody = ${response.body}")
          throw new InternalServerException("[EmailService][validate] - Error validating email")
      }
    }
  }

}
