/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.repositories.SessionCacheRepository
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

class DataCacheServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test")))
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData: FormData = FormData("some-data")

  val mockSessionCacheRepository: SessionCacheRepository = mock[SessionCacheRepository]

  override def beforeEach(): Unit = {
    reset(mockSessionCacheRepository)
  }

  case class FormData(name: String)

  object FormData {
    implicit val formats: OFormat[FormData] = Json.format[FormData]
  }

  val testDataCacheService = new DataCacheService(mockSessionCacheRepository)

  "DataCacheService" must {

    "return None" when {
      "formId of the cached form does not exist for defined data type" in {
        when(mockSessionCacheRepository.getFromSession[FormData](ArgumentMatchers.eq(formIdNotExist))(any(), any()))
          .thenReturn(Future.successful(None))

        await(testDataCacheService.fetchAndGetFormData[FormData](formIdNotExist)) must be(None)
      }
    }

    "return Some" when {
      "formId of the cached form does exist for defined data type" in {
        when(mockSessionCacheRepository.getFromSession[FormData](ArgumentMatchers.eq(formId))(any(), any()))
          .thenReturn(Future.successful(Some(formData)))

        await(testDataCacheService.fetchAndGetFormData[FormData](formId)) must be(Some(formData))
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in {
        when(mockSessionCacheRepository.putSession[FormData](ArgumentMatchers.eq(formId), ArgumentMatchers.eq(formData))(any(), any(), any()))
          .thenReturn(Future.successful(formData))

        await(testDataCacheService.cacheFormData[FormData](formId, formData)) must be(formData)
      }
    }

    "clear cache" when {
      "asked to do so" in {
        when(mockSessionCacheRepository.deleteFromSession(any())).thenReturn(Future.successful(()))

        await(testDataCacheService.clearCache()) must be(())
      }
    }
  }

}
