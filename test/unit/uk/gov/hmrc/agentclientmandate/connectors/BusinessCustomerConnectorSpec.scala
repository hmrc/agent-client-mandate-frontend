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
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder.createRegisteredAgentAuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.{CoreGet, CorePost, HeaderCarrier, HttpResponse}

class BusinessCustomerConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "BusinessCustomerConnector" must {

    "know the service url to connect to" when {

      "trying to connect to Business Customer service" in {
        TestBusinessCustomerConnector.serviceUrl must be("http://localhost:9924")
      }
    }

    "return status OK" when {
      "business customer service responds with a HttpResponse OK" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val ac: AuthContext = createRegisteredAgentAuthContext("agent", "agentId")
        val updateRegDetails = UpdateRegistrationDetailsRequest(false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),RegisteredAddressDetails("address1","address2",None,None,None,"FR"),EtmpContactDetails(None,None,None,None),true,true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockWSHttp.POST[UpdateRegistrationDetailsRequest, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, responseJson = Some(responseJson))))
        val result = await(TestBusinessCustomerConnector.updateRegistrationDetails("safeId", updateRegDetails))
        result.status must be(OK)
      }
    }


    "return response" when {
      "business customer service responds with a HttpResponse INTERNAL_SERVER_ERROR" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        implicit val ac: AuthContext = createRegisteredAgentAuthContext("agent", "agentId")
        val updateRegDetails = UpdateRegistrationDetailsRequest(false,None,Some(Organisation("Org Name",Some(true),Some("org_type"))),RegisteredAddressDetails("address1","address2",None,None,None,"FR"),EtmpContactDetails(None,None,None,None),true,true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(mockWSHttp.POST[UpdateRegistrationDetailsRequest, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(responseJson))))
        val result = await(TestBusinessCustomerConnector.updateRegistrationDetails("safeId", updateRegDetails))
        result.status must be(INTERNAL_SERVER_ERROR)
      }
    }

  }

  trait MockedVerbs extends CoreGet with CorePost
  val mockWSHttp: CoreGet with CorePost = mock[MockedVerbs]

  override def beforeEach(): Unit = {
    reset(mockWSHttp)
  }

  val responseJson = Json.parse("""{"valid": true}""")

  object TestBusinessCustomerConnector extends BusinessCustomerConnector {
    override val serviceUrl = baseUrl("business-customer")
    override val http = mockWSHttp
    override val baseUri = "business-customer"
    override val updateRegistrationDetailsURI = "update"
  }

}
