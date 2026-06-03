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

package uk.gov.hmrc.agentclientmandate.service

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentclientmandate.repositories.SessionCacheRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DataCacheServiceSpec
    extends PlaySpec
    with MockitoSugar
    with ScalaFutures {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit val executionContext: ExecutionContext =
    ExecutionContext.Implicits.global

  case class FakeData(field: String)

  object FakeData {
    implicit val formats: Format[FakeData] = Json.format[FakeData]
  }

  val mockSessionCacheRepository: SessionCacheRepository =
    mock[SessionCacheRepository]
  val service = new DataCacheService(mockSessionCacheRepository)

  "DataCacheService" should {
    "fetch and get form data from the session cache repository" in {
      val formId = "agent-ref-id"
      val data = FakeData("test")

      when(
        mockSessionCacheRepository.getFromSession[FakeData](eqTo(formId))(
          any[Format[FakeData]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Some(data)))

      val result = service.fetchAndGetFormData[FakeData](formId).futureValue

      result mustBe Some(data)

      verify(mockSessionCacheRepository).getFromSession[FakeData](eqTo(formId))(
        any[Format[FakeData]],
        any[HeaderCarrier]
      )
    }

    "cache form data in the session cache repository" in {
      val formId = "agent-ref-id"
      val data = FakeData("test")

      when(
        mockSessionCacheRepository
          .putSession[FakeData](eqTo(formId), eqTo(data))(
            any[Format[FakeData]],
            any[HeaderCarrier],
            any[ExecutionContext]
          )
      ).thenReturn(Future.successful(data))

      val result = service.cacheFormData[FakeData](formId, data).futureValue

      result mustBe data

      verify(mockSessionCacheRepository)
        .putSession[FakeData](eqTo(formId), eqTo(data))(
          any[Format[FakeData]],
          any[HeaderCarrier],
          any[ExecutionContext]
        )
    }

    "clear the session cache repository" in {
      when(
        mockSessionCacheRepository.deleteFromSession(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      service.clearCache().futureValue mustBe ()

      verify(mockSessionCacheRepository).deleteFromSession(any[HeaderCarrier])
    }
  }
}
