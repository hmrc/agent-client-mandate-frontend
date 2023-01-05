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
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class DataCacheServiceSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("test")))

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData: FormData = FormData("some-data")

  val formDataJson: JsValue = Json.toJson(formData)

  val cacheMap: CacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  val mockSessionCache: SessionCache = mock[SessionCache]

  override def beforeEach: Unit = {
    reset(mockSessionCache)
  }

  case class FormData(name: String)

  object FormData {
    implicit val formats: OFormat[FormData] = Json.format[FormData]
  }

  val mockDefaultHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockAppConfig: AppConfig = mock[AppConfig]

  class Setup {
    val testDataCacheService = new DataCacheService(
      mockDefaultHttpClient,
      mockAppConfig
    )
  }

  "DataCacheService" must {

    "return None" when {
      "formId of the cached form does not exist for defined data type" in new Setup {

        when(mockSessionCache.fetchAndGetEntry[FormData](key = ArgumentMatchers.eq(formIdNotExist))
          (any(), any(), any())) thenReturn {
          Future.successful(None)
        }

        when(mockDefaultHttpClient.GET[CacheMap](any(), any(), any())
          (any(), any(), any()))
          .thenReturn(Future.successful(CacheMap("test", Map())))

        await(testDataCacheService.fetchAndGetFormData[FormData](formIdNotExist)) must be(None)
      }
    }

    "return Some" when {
      "formId of the cached form does exist for defined data type" in new Setup {

        when(mockSessionCache.fetchAndGetEntry[FormData](key = ArgumentMatchers.eq(formIdNotExist))
          (any(), any(), any())) thenReturn {
          Future.successful(Some(formData))
        }

        when(mockDefaultHttpClient.GET[CacheMap](any(), any(), any())
          (any(), any(), any()))
          .thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

        await(testDataCacheService.fetchAndGetFormData[FormData](formIdNotExist)) must be(Some(formData))
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in new Setup {
        when(mockSessionCache.cache[FormData](ArgumentMatchers.eq(formId), ArgumentMatchers.eq(formData))
          (any(), any(), any())) thenReturn {
          Future.successful(cacheMap)
        }

        when(mockDefaultHttpClient.PUT[FormData, CacheMap](any(), any(), any())
          (any(), any(), any(), any()))
          .thenReturn(Future.successful(CacheMap("test", Map(formIdNotExist -> Json.toJson(formData)))))

        await(testDataCacheService.cacheFormData[FormData](formId, formData)) must be(formData)
      }
    }

    "clear cache" when {
      "asked to do so" in new Setup {
        when(mockSessionCache.remove()(any(), any())).thenReturn(Future.successful(HttpResponse(OK, "")))

        when(mockDefaultHttpClient.DELETE[HttpResponse]
        (any(), any())
        (any(), any(), any())
        ).thenReturn(Future.successful(HttpResponse(OK, "")))

        await(testDataCacheService.clearCache()).status must be(OK)
      }
    }

  }

}
