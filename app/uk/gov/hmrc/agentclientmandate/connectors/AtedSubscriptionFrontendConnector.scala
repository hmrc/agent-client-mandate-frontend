/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.Request
import uk.gov.hmrc.agentclientmandate.config.WSHttp
import uk.gov.hmrc.http.{CoreGet, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import scala.concurrent.Future

trait AtedSubscriptionFrontendConnector extends ServicesConfig with RawResponseReads with HeaderCarrierForPartialsConverter {

  def serviceUrl: String = baseUrl("ated-subscription-frontend")
  def http: CoreGet
  val clearCacheUri = "clear-cache"

  def clearCache(service: String)(implicit request: Request[_], ac: AuthContext): Future[HttpResponse] = {
    val getUrl = s"$serviceUrl/$clearCacheUri/$service"
    http.GET[HttpResponse](getUrl)
  }

}

object AtedSubscriptionFrontendConnector extends AtedSubscriptionFrontendConnector {
  // $COVERAGE-OFF$
  val http = WSHttp
  override def crypto: (String) => String = SessionCookieCryptoFilter.encrypt _
  // $COVERAGE-ON$
}
