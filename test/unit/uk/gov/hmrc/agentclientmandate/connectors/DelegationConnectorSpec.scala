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
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.models.{Link, PrincipalTaxIdentifiers, StartDelegationContext}
import uk.gov.hmrc.domain.AtedUtr
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DelegationConnectorSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  val mockDefaultHttpClient = mock[DefaultHttpClient]
  val mockServicesConfig = mock[ServicesConfig]

  trait Setup {
    val connector: DelegationConnector = new DelegationConnector(
      mockDefaultHttpClient,
      mockServicesConfig
    )
  }

  override def beforeEach(): Unit = {
    reset(mockDefaultHttpClient)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val startDelegationContext = StartDelegationContext(
    "principalName",
    "attorneyName",
    Link("url", "text"),
    PrincipalTaxIdentifiers(
      ated = Some(AtedUtr("XN1200000100001"))
    ),
    "internalId"
  )

  "startDelegation" should {
    "create a new delegation" when {
      "supplied with a delegation context" in new Setup {
        when(mockDefaultHttpClient.PUT[StartDelegationContext, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(CREATED, "")))

        await(connector.startDelegation("oid", startDelegationContext)) mustBe true
      }
    }

    "fail to create a new delegation" when {
      "a 400 response is returned" in new Setup {
        when(mockDefaultHttpClient.PUT[StartDelegationContext, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("failed", BAD_REQUEST, BAD_REQUEST)))

        intercept[RuntimeException](await(connector.startDelegation("oid", startDelegationContext)))
      }

      "a 200 response is returned" in new Setup {
        when(mockDefaultHttpClient.PUT[StartDelegationContext, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        intercept[RuntimeException](await(connector.startDelegation("oid", startDelegationContext)))
      }

      "a 500 response is returned" in new Setup {
        when(mockDefaultHttpClient.PUT[StartDelegationContext, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("failed", INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR)))

        intercept[RuntimeException](await(connector.startDelegation("oid", startDelegationContext)))
      }
    }
  }

}
