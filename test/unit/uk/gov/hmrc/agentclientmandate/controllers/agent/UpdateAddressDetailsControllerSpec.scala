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

package unit.uk.gov.hmrc.agentclientmandate.controllers.agent

import java.util.UUID

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.controllers.agent.UpdateAddressDetailsController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthBuilder, SessionBuilder}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import play.api.mvc.{AnyContentAsFormUrlEncoded, AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.EditAgentAddressDetails

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class UpdateAddressDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "UpdateAddressDetailsController" should {

    "not respond with NOT_FOUND status" when {
      "GET /mandate/agent/details/edit/abc/businessDetails is invoked" in {
        val result = route(FakeRequest(GET, "/mandate/agent/details/edit/businessDetails")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to unathorised page" when {
      "the user is UNAUTHORISED" in {
        getWithUnAuthorisedUser("abc") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return status OK" when {
      "user is AUTHORISED" in {
        getWithAuthorisedUser(cachedData, "abc") { result =>
          status(result) must be(OK)
        }
      }
    }

    "throw exception" when {
      "no cached data is returned" in {
        getWithAuthorisedUser(None, "abc") { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Registration Details found")
        }
      }
    }

    "fail to submit the input business details" when {
      "UNAUTHORISED user tries to submit" in {
        saveWithUnAuthorisedUser("abc") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }
    "submit the input business details" when {
      "AUTHORISED user tries to submit" in {
        val x = EditAgentAddressDetails("Org name", address = RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(updateRegDetails, "abc")(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/edit")
        }
      }
    }

    "fail to submit the input business details" when {
      "AUTHORISED user tries to submit but fails due to form eror" in {
        val x = EditAgentAddressDetails("Org name", address = RegisteredAddressDetails("address1", "address2", countryCode = ""))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(None, "abc")(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "AUTHORISED user tries to submit but ETMP update fails" in {
        val x = EditAgentAddressDetails("Org name", address = RegisteredAddressDetails("address1", "address2", countryCode = "FR"))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(None, "abc")(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
  val cachedData = Some(AgentBuilder.buildAgentDetails)
  val agentDetails = AgentBuilder.buildAgentDetails
  val updateRegDetails = Some(UpdateRegistrationDetailsRequest(false, None, Some(Organisation("Org name", Some(true), Some("org_type"))), RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None), true, true, None))

  val mockAuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService = mock[AgentClientMandateService]
  val mockDataCacheService = mock[DataCacheService]

  object TestUpdateAddressDetailsController extends UpdateAddressDetailsController {
    override val authConnector = mockAuthConnector
    override val dataCacheService: DataCacheService = mockDataCacheService
    override val agentClientMandateService: AgentClientMandateService = mockAgentClientMandateService
  }

  override def beforeEach() = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockAgentClientMandateService)
  }

  def getWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestUpdateAddressDetailsController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def getWithAuthorisedUser(cachedData: Option[AgentDetails] = None, service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[AgentDetails](Matchers.eq(TestUpdateAddressDetailsController.agentDetailsFormId))(Matchers.any(), Matchers.any())) thenReturn Future.successful(cachedData)
    when(mockAgentClientMandateService.fetchAgentDetails()(Matchers.any(), Matchers.any())) thenReturn (Future.successful(agentDetails))
    val result = TestUpdateAddressDetailsController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthorisedAgent(userId, mockAuthConnector)
    val result = TestUpdateAddressDetailsController.submit(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def saveWithAuthorisedUser(updatedRegDetails: Option[UpdateRegistrationDetailsRequest], service: String)
                            (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockAgentClientMandateService.updateRegisteredDetails(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())) thenReturn (Future.successful(updatedRegDetails))
    val result = TestUpdateAddressDetailsController.submit(service).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
    test(result)
  }
}
