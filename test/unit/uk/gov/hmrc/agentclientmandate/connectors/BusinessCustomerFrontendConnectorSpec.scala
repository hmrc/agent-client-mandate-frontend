/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerFrontendConnector
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class BusinessCustomerFrontendConnectorSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockDefaultHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockSessionCookieCrypto: SessionCookieCrypto = mock[SessionCookieCrypto]
  val mockEncWithDec: Encrypter with Decrypter = mock[Encrypter with Decrypter]

  override def beforeEach(): Unit = {
    when(mockSessionCookieCrypto.crypto)
      .thenReturn(mockEncWithDec)
    when(mockEncWithDec.encrypt(ArgumentMatchers.any()))
      .thenReturn(Crypted("test"))

    reset(mockDefaultHttpClient)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest(GET, "")

  class Setup {
    val connector = new BusinessCustomerFrontendConnector(
      mockServicesConfig,
      mockDefaultHttpClient,
      mockSessionCookieCrypto
    )
  }

  "BusinessCustomerFrontendConnector" must {
    "clear cache" in new Setup {
      when(mockDefaultHttpClient.GET[HttpResponse]
        (ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

      val response = connector.clearCache("")
      await(response).status must be(OK)
    }

    "crypto" in new Setup {
      connector.crypto("test").length mustBe 4
    }
  }
}
