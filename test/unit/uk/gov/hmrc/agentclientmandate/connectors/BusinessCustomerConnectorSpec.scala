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

package unit.uk.gov.hmrc.agentclientmandate.connectors

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.connectors.BusinessCustomerConnector
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessCustomerConnectorSpec extends PlaySpec  with MockitoSugar {

  val testAgentAuthRetrievals = AgentAuthRetrievals(
    "agentRef",
    "agentCode",
    Some("agentFriendlyName"),
    "providerId",
    "internalId"
  )

  val mockServicesConfig = mock[ServicesConfig]

  val acknowledgementReference = "acknowledgementRef"

  class Setup extends ConnectorMocks {
    when(mockServicesConfig.baseUrl("business-customer")).thenReturn("http://localhost:9020/")
    val connector = new BusinessCustomerConnector(mockHttpClient, mockServicesConfig)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "BusinessCustomerConnector" must {

    "return status OK" when {
      "business customer service responds with a HttpResponse OK" in new Setup {
        val updateRegDetails = UpdateRegistrationDetailsRequest(acknowledgementReference, isAnIndividual = false,None,Some(Organisation("Org Name",Some(true))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(OK, responseJson, Map.empty[String, Seq[String]])))
        val result = await(connector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(OK)
      }
    }

    "return response" when {
      "business customer service responds with a HttpResponse INTERNAL_SERVER_ERROR" in new Setup {
        val updateRegDetails = UpdateRegistrationDetailsRequest(acknowledgementReference, isAnIndividual = false,None,Some(Organisation("Org Name",Some(true))),
          RegisteredAddressDetails("address1","address2",None,None,None,"FR"),
          EtmpContactDetails(None,None,None,None),isAnAgent = true,isAGroup = true,Some(Identification("idnumber","FR","issuingInstitution")))
        when(execute[HttpResponse]).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson, Map.empty[String, Seq[String]])))
        val result = await(connector.updateRegistrationDetails("safeId", updateRegDetails, testAgentAuthRetrievals))
        result.status must be(INTERNAL_SERVER_ERROR)
      }
    }

  }

  val responseJson: JsValue = Json.parse("""{"valid": true}""")
}
