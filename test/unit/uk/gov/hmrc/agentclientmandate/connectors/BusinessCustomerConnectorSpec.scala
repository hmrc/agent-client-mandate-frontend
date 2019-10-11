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
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  val testAgentAuthRetrievals = AgentAuthRetrievals(
    "agentRef",
    "agentCode",
    Some("agentFriendlyName"),
    "providerId",
    "internalId"
  )

  "BusinessCustomerConnector" must {

    "know the service url to connect to" when {

      "trying to connect to Business Customer service" in {
        TestBusinessCustomerConnector.serviceUrl must be("http://localhost:9924")
      }
    }

    "return status OK" when {
      "business customer service responds with a HttpResponse OK" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val updateRegDetails = UpdateRegistrationDetailsRequest(isAnIndividual = false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockWSHttp.POST[UpdateRegistrationDetailsRequest, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(responseJson))))
        val result = await(TestBusinessCustomerConnector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(OK)
      }
    }


    "return response" when {
      "business customer service responds with a HttpResponse INTERNAL_SERVER_ERROR" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val updateRegDetails = UpdateRegistrationDetailsRequest(isAnIndividual = false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockWSHttp.POST[UpdateRegistrationDetailsRequest, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(responseJson))))
        val result = await(TestBusinessCustomerConnector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(INTERNAL_SERVER_ERROR)
      }
    }

  }

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp: CoreGet with CorePost = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val responseJson: JsValue = Json.parse("""{"valid": true}""")

  object TestBusinessCustomerConnector extends BusinessCustomerConnector {
    override val serviceUrl: String = baseUrl("business-customer")
    override val http: CoreGet with CorePost = mockWSHttp
    override val baseUri: String = "business-customer"
    override val updateRegistrationDetailsURI: String = "update"
  }

}
