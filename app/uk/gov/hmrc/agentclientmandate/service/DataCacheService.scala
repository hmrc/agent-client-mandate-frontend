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

package uk.gov.hmrc.agentclientmandate.service

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Format
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DataCacheService @Inject()(val http: DefaultHttpClient,
                                 val config: AppConfig) extends SessionCache {

  val baseUri: String = config.baseDataCacheUri
  val defaultSource: String = config.dataCacheDefaultSource
  val domain: String = config.dataCacheDomain

  def fetchAndGetFormData[T](formId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    fetchAndGetEntry[T](key = formId)
  }

  def cacheFormData[T](formId: String, formData: T)(implicit hc: HeaderCarrier, formats: Format[T]): Future[T] = {
    cache[T](formId, formData).map(_ => formData)
  }

  def clearCache()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    remove()
  }
}
