/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.connectors

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

object EmailServiceConnector extends EmailServiceConnector {
  val serviceUrl = baseUrl("email")
  val http = WSHttp
}

trait EmailServiceConnector extends ServicesConfig with RawResponseReads {

  def serviceUrl: String

  def http: CorePost

  def validate(email: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/validate-email-address"
    http.POST[EmailToValidate, HttpResponse](getUrl, EmailToValidate(email))
  }
}

case class EmailToValidate(email: String)

object EmailToValidate {
  implicit def formats: Format[EmailToValidate] = Json.format[EmailToValidate]
}
