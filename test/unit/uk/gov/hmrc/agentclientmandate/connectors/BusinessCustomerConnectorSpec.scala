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
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec  with MockitoSugar with BeforeAndAfterEach {

  val testAgentAuthRetrievals = AgentAuthRetrievals(
    "agentRef",
    "agentCode",
    Some("agentFriendlyName"),
    "providerId",
    "internalId"
  )

  val mockDefaultHttpClient = mock[DefaultHttpClient]
  val mockServicesConfig = mock[ServicesConfig]

  class Setup {
    val connector = new BusinessCustomerConnector(mockDefaultHttpClient, mockServicesConfig)
  }


  "BusinessCustomerConnector" must {

    "return status OK" when {
      "business customer service responds with a HttpResponse OK" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val updateRegDetails = UpdateRegistrationDetailsRequest(isAnIndividual = false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockDefaultHttpClient.POST[UpdateRegistrationDetailsRequest, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(responseJson))))
        val result = await(connector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(OK)
      }
    }


    "return response" when {
      "business customer service responds with a HttpResponse INTERNAL_SERVER_ERROR" in new Setup {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val updateRegDetails = UpdateRegistrationDetailsRequest(isAnIndividual = false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockDefaultHttpClient.POST[UpdateRegistrationDetailsRequest, HttpResponse]
          (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(responseJson))))
        val result = await(connector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(INTERNAL_SERVER_ERROR)
      }
    }

  }

  override def beforeEach(): Unit = {
    reset(mockDefaultHttpClient)
  }

  val responseJson: JsValue = Json.parse("""{"valid": true}""")
}
