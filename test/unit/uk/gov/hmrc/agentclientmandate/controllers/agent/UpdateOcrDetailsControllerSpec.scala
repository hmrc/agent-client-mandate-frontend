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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.agent.UpdateOcrDetailsController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasCompany
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.RunMode
import unit.uk.gov.hmrc.agentclientmandate.builders.{AgentBuilder, AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UpdateOcrDetailsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfterEach with MockControllerSetup {

  "UpdateOcrDetailsController" should {

    "redirect to unathorised page" when {
      "the user is UNAUTHORISED" in new Setup {
        getWithUnAuthorisedUser("abc") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "return status OK" when {
      "user is AUTHORISED" in new Setup {
        getWithAuthorisedUser(cachedData, "abc") { result =>
          status(result) must be(OK)
        }
      }
    }

    "throw exception" when {
      "no cached data is returned" in new Setup {
        getWithAuthorisedUser(None, "abc") { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Registration Details found")
        }
      }
    }


    "fail to submit the input ocr details" when {
      "UNAUTHORISED user tries to submit" in new Setup {
        saveWithUnAuthorisedUser("abc") { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }
    }

    "submit the input ocr details" when {
      "AUTHORISED user tries to submit" in new Setup {
        val x = OverseasCompany(Some(true), Some("IdNumber"), Some("issuingCountry"), Some("FR"))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(updateRegDetails, "abc")(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/mandate/agent/edit")
        }
      }
    }

    "fail to submit the input ocr details" when {
      "AUTHORISED user tries to submit but fails due to form eror" in new Setup {
        val x = OverseasCompany(Some(true), Some("IdNumber"), Some("issuingCountry"), Some(""))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(None, "abc")(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }

      "AUTHORISED user tries to submit but ETMP update fails" in new Setup {
        val x = OverseasCompany(Some(true), Some("IdNumber"), Some("issuingCountry"), Some("FR"))
        val inputJson = Json.toJson(x)
        val fakeRequest = FakeRequest().withJsonBody(inputJson)
        saveWithAuthorisedUser(None, "abc")(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }
  }
  val cachedData: Some[AgentDetails] = Some(AgentBuilder.buildAgentDetails)
  val agentDetails: AgentDetails = AgentBuilder.buildAgentDetails
  val updateRegDetails: Some[UpdateRegistrationDetailsRequest] = Some(UpdateRegistrationDetailsRequest(isAnIndividual = false, None,
    Some(Organisation("Org name", Some(true), Some("org_type"))),
    RegisteredAddressDetails("address1", "address2", None, None, None, "FR"), EtmpContactDetails(None, None, None, None),
    isAnAgent = true, isAGroup = true,
    identification = Some(Identification("IdNumber", "issuingCountry", "FR"))))

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAgentClientMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]



  class Setup {
    val controller = new UpdateOcrDetailsController(
      mockAgentClientMandateService,
      mockDataCacheService,
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      implicitly,
      mockAppConfig
    )

    def getWithUnAuthorisedUser(service: String)(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def getWithAuthorisedUser(cachedData: Option[AgentDetails] = None, service: String)(test: Future[Result] => Any): Any = {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[AgentDetails](ArgumentMatchers.eq(controller.agentDetailsFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(cachedData))
      when(mockAgentClientMandateService.fetchAgentDetails(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn (Future.successful(agentDetails))
      val result = controller.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithUnAuthorisedUser(service: String)(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.submit(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def saveWithAuthorisedUser(updatedRegDetails: Option[UpdateRegistrationDetailsRequest], service: String)
                              (fakeRequest: FakeRequest[AnyContentAsJson])(test: Future[Result] => Any) {
      val userId = s"user-${UUID.randomUUID}"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockAgentClientMandateService.updateRegisteredDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn (Future.successful(updatedRegDetails))
      val result = controller.submit(service).apply(SessionBuilder.updateRequestWithSession(fakeRequest, userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockDataCacheService)
    reset(mockAgentClientMandateService)
  }
}
