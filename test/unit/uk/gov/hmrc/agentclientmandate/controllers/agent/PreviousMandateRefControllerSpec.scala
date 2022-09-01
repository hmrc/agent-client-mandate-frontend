/*
 * Copyright 2022 HM Revenue & Customs
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
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.PreviousMandateRefController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.searchPreviousMandate
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PreviousMandateRefControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "PreviousMandateRefController" must {

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in new Setup {
        viewUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "client requests for search mandate view" in new Setup {
        viewUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED agent" when {

      "agent requests(GET) for search previous mandate view" in new Setup {
        viewWithAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("mandateRef").`val`() must be("")
        }
      }

      "agent requests(GET) for search mandate view pre-populated and the data has been cached" in new Setup {
        val cached: ClientCache = ClientCache(mandate = Some(mandate1))
        viewWithAuthorisedAgent(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.search-previous-mandate.title - GOV.UK")
          document.getElementsByTag("header").text() must include("agent.search-previous-mandate.header")
          document.getElementById("mandateRef").`val`() must be("ABC123")
          document.getElementById("submit").text() must be("continue-button")
        }
      }

    }

    "redirect to ated-subscription corresponding address page" when {

      "valid form is submitted, mandate is found from backend, cache object exists and update of cache with mandate is successful" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty: Option[Party] = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData: ClientCache = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1: Mandate = mandate.copy(clientParty = clientParty)
        val returnCache: ClientCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(
            "http://localhost:9933/ated-subscription/registered-business-address?backLinkUrl=http://localhost:9959/mandate/agent/search-previous/callingPage"))
        }
      }

      "valid form is submitted but with mandate having spaces, mandate is found from backend, " +
        "cache object exists and update of cache with mandate is successful" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> s"   $mandateId   ")
        val clientParty: Option[Party] = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData: ClientCache = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1: Mandate = mandate.copy(clientParty = clientParty)
        val returnCache: ClientCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(
            "http://localhost:9933/ated-subscription/registered-business-address?backLinkUrl=http://localhost:9959/mandate/agent/search-previous/callingPage"))
        }
      }

    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.search-mandate.error.clientAuthNum")
          document.getElementsByClass("govuk-error-message").text() must include("client.search-mandate.error.clientAuthNum")
          verify(mockMandateService, times(0)).fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "mandateRef field has more than expected length" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> "a" * 11)
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.search-mandate.error.clientAuthNum")
          document.getElementsByClass("govuk-error-message").text() must include("client.search-mandate.error.clientAuthNum")
          verify(mockMandateService, times(0)).fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }

      "invalid agent reference is passed" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.search-mandate.error.clientAuthNum")
          document.getElementsByClass("govuk-error-message").text() must
            include("client.search-mandate.error.clientAuthNum")
          verify(mockMandateService, times(1)).fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](ArgumentMatchers.any(),
            ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
        }
      }
    }

    "throw exception" when {
      "when client ref not found" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        val returnCache: ClientCache = ClientCache(mandate = Some(mandate))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = None, mandate = Some(mandate1), returnCache = returnCache) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Client Ref no. found!")
        }
      }
    }

    "retrieve old mandate info stored in session" when {
      "return ok" in new Setup {
        retrieveOldMandateFromSessionAuthorisedAgent(Some(OldMandateReference(mandateId, "ated-ref-no"))) { result =>
          status(result) must be(OK)
        }
      }
    }
  }

  val mandateId = "ABC123"
  val mandate: Mandate = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = Some("atedref"),
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val mandate1: Mandate = Mandate(id = mandateId, createdBy = User("credId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.Approved, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val service = "ATED"

  val oldMandateReference: OldMandateReference = OldMandateReference("mandateId", "atedRefNum")

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockDataCacheService: DataCacheService = mock[DataCacheService]
  val mockMandateService: AgentClientMandateService = mock[AgentClientMandateService]
  val injectedViewInstanceSearchPreviousMandate: searchPreviousMandate = app.injector.instanceOf[views.html.agent.searchPreviousMandate]

  class Setup {
    val controller = new PreviousMandateRefController(
      stubbedMessagesControllerComponents,
      mockAuthConnector,
      mockDataCacheService,
      mockMandateService,
      implicitly,
      mockAppConfig,
      injectedViewInstanceSearchPreviousMandate
    )

    def viewUnAuthenticatedAgent(test: Future[Result] => Any): Unit = {

      AuthenticatedWrapperBuilder.mockUnAuthenticated(mockAuthConnector)
      val result = controller.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSessionNoUser)
      test(result)
    }

    def viewWithAuthorisedAgent(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      val result = controller.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                  cachedData: Option[ClientCache] = None,
                                  mandate: Option[Mandate] = None,
                                  returnCache: ClientCache = ClientCache())(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[ClientCache](ArgumentMatchers.eq(controller.clientFormId))(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      when(mockMandateService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(
        ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(mandate))
      when(mockDataCacheService.cacheFormData[ClientCache]
        (ArgumentMatchers.eq(controller.clientFormId), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(returnCache))
      when(mockDataCacheService.cacheFormData[OldMandateReference]
        (ArgumentMatchers.eq(controller.oldNonUkMandate), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(oldMandateReference))
      val result = controller.submit(service, "callingPage").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }

    def retrieveOldMandateFromSessionAuthorisedAgent(cachedData:Option[OldMandateReference] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockDataCacheService.fetchAndGetFormData[OldMandateReference](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(cachedData))
      val result = controller.getOldMandateFromSession(service).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }
  }

  override def beforeEach(): Unit = {
    reset(mockMandateService)
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

}
