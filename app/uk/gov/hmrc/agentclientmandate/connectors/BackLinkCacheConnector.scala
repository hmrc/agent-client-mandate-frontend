/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models.BackLinkModel
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BackLinkCacheConnector @Inject()(val http: DefaultHttpClient,
                                       config: AppConfig) extends SessionCache {
  val baseUri: String = config.baseDataCacheUri
  val defaultSource: String = config.dataCacheDefaultSource
  val domain: String = config.dataCacheDomain

  val sourceId: String = "BC_Back_Link"

  private def getKey(pageId: String) = s"$sourceId:$pageId"

  def fetchAndGetBackLink(pageId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    fetchAndGetEntry[BackLinkModel](getKey(pageId)).map(_.flatMap(_.backLink))
  }

  def saveBackLink(pageId: String, returnUrl: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cache[BackLinkModel](getKey(pageId), BackLinkModel(returnUrl)).map(_ => returnUrl)
  }

}
