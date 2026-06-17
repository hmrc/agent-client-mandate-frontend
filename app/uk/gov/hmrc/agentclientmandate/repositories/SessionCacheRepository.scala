/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.repositories

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.cache.{CacheIdType, DataKey, MongoCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

object SessionCacheId extends CacheIdType[HeaderCarrier] {
  override def run: HeaderCarrier => String =
    _.sessionId.map(_.value).getOrElse("")
}

trait SessionCacheRepository {
  def putSession[T: Writes](key: String, data: T)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[T]

  def getFromSession[T: Reads](key: String)(implicit
      hc: HeaderCarrier
  ): Future[Option[T]]

  def deleteFromSession(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class DefaultSessionCacheRepository @Inject() (
    mongoComponent: MongoComponent,
    timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends SessionCacheRepository {

  private val cacheRepo = new MongoCacheRepository[HeaderCarrier](
    mongoComponent = mongoComponent,
    collectionName = "sessions",
    ttl = 900.seconds,
    timestampSupport = timestampSupport,
    cacheIdType = SessionCacheId
  )

  def putSession[T: Writes](key: String, data: T)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[T] =
    cacheRepo.put[T](hc)(DataKey[T](key), data).map(_ => data)

  def getFromSession[T: Reads](key: String)(implicit
      hc: HeaderCarrier
  ): Future[Option[T]] =
    cacheRepo.get[T](hc)(DataKey[T](key))

  def deleteFromSession(implicit hc: HeaderCarrier): Future[Unit] =
    cacheRepo.deleteEntity(hc)
}
