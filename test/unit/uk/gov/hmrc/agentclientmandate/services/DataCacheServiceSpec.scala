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

package unit.uk.gov.hmrc.agentclientmandate.services

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AgentClientMandateSessionCache
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class DataCacheServiceSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  case class FormData(name: String)

  object FormData {
    implicit val formats = Json.format[FormData]
  }

  "DataCacheService" must {

    "use correct session cache" in {
      DataCacheService.sessionCache must be(AgentClientMandateSessionCache)
    }

    "return None" when {
      "formId of the cached form does not exist for defined data type" in {

        when(mockSessionCache.fetchAndGetEntry[FormData](key = Matchers.eq(formIdNotExist))(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(None)
        }
        await(TestDataCacheService.fetchAndGetFormData[FormData](formIdNotExist)) must be(None)
      }
    }

    "return Some" when {
      "formId of the cached form does exist for defined data type" in {

        when(mockSessionCache.fetchAndGetEntry[FormData](key = Matchers.eq(formIdNotExist))(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(Some(formData))
        }
        await(TestDataCacheService.fetchAndGetFormData[FormData](formIdNotExist)) must be(Some(formData))
      }
    }

    "save form data" when {
      "valid form data with a valid form id is passed" in {
        when(mockSessionCache.cache[FormData](Matchers.eq(formId), Matchers.eq(formData))(Matchers.any(), Matchers.any(), Matchers.any())) thenReturn {
          Future.successful(cacheMap)
        }
        await(TestDataCacheService.cacheFormData[FormData](formId, formData)) must be(formData)
      }
    }

    "clear cache" when {
      "asked to do so" in {
        when(mockSessionCache.remove()(Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))
        await(TestDataCacheService.clearCache()).status must be(OK)
      }
    }

  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val formId = "form-id"
  val formIdNotExist = "no-form-id"

  val formData = FormData("some-data")

  val formDataJson = Json.toJson(formData)

  val cacheMap = CacheMap(id = formId, Map("date" -> formDataJson))

  val mockSessionCache = mock[SessionCache]

  override def beforeEach: Unit = {
    reset(mockSessionCache)
  }

  object TestDataCacheService extends DataCacheService {
    override val sessionCache: SessionCache = mockSessionCache
  }

}
