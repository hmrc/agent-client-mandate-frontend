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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerFrontendConnector
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder._

import scala.concurrent.Future

class BusinessCustomerFrontendConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CoreGet
  val mockWSHttp: CoreGet = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  implicit val ac: AuthContext = createRegisteredAgentAuthContext("agent", "agentId")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest(GET, "")

  object TestBusinessCustomerFrontendConnector extends BusinessCustomerFrontendConnector {
    override val http = mockWSHttp
    override def crypto: (String) => String = new SessionCookieCryptoFilter(new ApplicationCrypto(Play.current.configuration.underlying)).encrypt _
  
  }

  "BusinessCustomerFrontendConnector" must {
    "clear cache" in {
      when(mockWSHttp.GET[HttpResponse]
        (Matchers.any())
        (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200)))

      val response = TestBusinessCustomerFrontendConnector.clearCache("")
      await(response).status must be(OK)
    }
  }
}
