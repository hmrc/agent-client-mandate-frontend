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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.models.StartDelegationContext
import uk.gov.hmrc.domain.AtedUtr
import uk.gov.hmrc.http.{CorePut, HeaderCarrier, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.frontend.auth.{Link, TaxIdentifiers}
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationServiceException

import scala.concurrent.Future

class DelegationConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  trait MockedVerbs extends CorePut
  val mockWSHttp: MockedVerbs = mock[MockedVerbs]

  trait Setup {
    val connector: DelegationConnector = new DelegationConnector {
      override val http: CorePut = mockWSHttp

      override protected def serviceUrl: String = "serviceUrl"
    }
  }

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val startDelegationContext = StartDelegationContext(
    "principalName",
    "attorneyName",
    Link("url", "text"),
    TaxIdentifiers(
      ated = Some(AtedUtr("XN1200000100001"))
    ),
    "internalId"
  )

  "startDelegation" should {
    "create a new delegation" when {
      "supplied with a delegation context" in new Setup {
        when(mockWSHttp.PUT[StartDelegationContext, HttpResponse](Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(HttpResponse(CREATED)))

        await(connector.startDelegation("oid", startDelegationContext)) mustBe true
      }
    }

    "fail to create a new delegation" when {
      "a 400 response is returned" in new Setup {
        when(mockWSHttp.PUT[StartDelegationContext, HttpResponse](Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(Upstream4xxResponse("failed", BAD_REQUEST, BAD_REQUEST)))

        intercept[DelegationServiceException](await(connector.startDelegation("oid", startDelegationContext)))
      }

      "a 200 response is returned" in new Setup {
        when(mockWSHttp.PUT[StartDelegationContext, HttpResponse](Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        intercept[DelegationServiceException](await(connector.startDelegation("oid", startDelegationContext)))
      }

      "a 500 response is returned" in new Setup {
        when(mockWSHttp.PUT[StartDelegationContext, HttpResponse](Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.failed(Upstream5xxResponse("failed", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

        intercept[DelegationServiceException](await(connector.startDelegation("oid", startDelegationContext)))
      }
    }
  }

}
