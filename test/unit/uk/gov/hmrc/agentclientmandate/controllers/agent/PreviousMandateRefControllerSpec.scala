/*
 * Copyright 2018 HM Revenue & Customs
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
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.agent.PreviousMandateRefController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientDisplayName, ClientEmail}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class PreviousMandateRefControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "PreviousMandateRefController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/agent/search-previous/nrl/ATED" in {
        val result = route(FakeRequest(GET, "/mandate/agent/search-previous/nrl")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        viewUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED agent" when {

      "client requests for search mandate view" in {
        viewUnAuthenticatedAgent { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED agent" when {

      "agent requests(GET) for search previous mandate view" in {
        viewWithAuthorisedAgent() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById("mandateRef").`val`() must be("")
        }
      }

      "agent requests(GET) for search mandate view pre-populated and the data has been cached" in {
        val cached = ClientCache(mandate = Some(mandate1))
        viewWithAuthorisedAgent(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is the previous unique authorisation number for the client? - GOV.UK")
          document.getElementById("header").text() must include("What is the previous unique authorisation number for the client?")
          document.getElementById("mandateRef").`val`() must be("ABC123")
          document.getElementById("submit").text() must be("Continue")
        }
      }

    }

    "redirect to ated-subscription corresponding address page" when {

      "valid form is submitted, mandate is found from backend, cache object exists and update of cache with mandate is successful" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9933/ated-subscription/registered-business-address?backLinkUrl=http://localhost:9959/mandate/agent/search-previous/callingPage"))
        }
      }

      "valid form is submitted but with mandate having spaces, mandate is found from backend, cache object exists and update of cache with mandate is successful" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"   $mandateId   ")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("http://localhost:9933/ated-subscription/registered-business-address?backLinkUrl=http://localhost:9959/mandate/agent/search-previous/callingPage"))
        }
      }

    }

    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("You must answer unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("You must answer unique authorisation number question")
          verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "mandateRef field has more than expected length" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "a" * 11)
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("You must answer unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("A unique authorisation number cannot be more than 8 characters")
          verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid agent reference is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        submitWithAuthorisedAgent(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("You must answer unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("The unique authorisation number you entered cannot be found. Check the number, or enter a different number.")
          verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }
    }

    "throw exception" when {
      "when client ref not found" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        val returnCache = ClientCache(mandate = Some(mandate))
        submitWithAuthorisedAgent(request = fakeRequest, cachedData = None, mandate = Some(mandate1), returnCache = returnCache) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Client Ref no. found!")
        }
      }
    }

    "retrieve old mandate info stored in session" when {
      "return ok" in {
        retrieveOldMandateFromSessionAuthorisedAgent(Some(OldMandateReference(mandateId, "ated-ref-no"))) { result =>
          status(result) must be(OK)
        }
      }
    }
  }

  val mandateId = "ABC123"
  val mandate = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.New, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = Some("atedref"),
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val mandate1 = Mandate(id = mandateId, createdBy = User("credId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.Approved, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val service = "ATED"

  val oldMandateReference = OldMandateReference("mandateId", "atedRefNum")

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestPreviousMandateRefController extends PreviousMandateRefController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val mandateService = mockMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockMandateService)
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewUnAuthenticatedAgent(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    val result = TestPreviousMandateRefController.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedAgent(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestPreviousMandateRefController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestPreviousMandateRefController.view(service, "callingPage").apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedAgent(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                cachedData: Option[ClientCache] = None,
                                mandate: Option[Mandate] = None,
                                returnCache: ClientCache = ClientCache())(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestPreviousMandateRefController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandate))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestPreviousMandateRefController.clientFormId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnCache))
    when(mockDataCacheService.cacheFormData[OldMandateReference](Matchers.eq(TestPreviousMandateRefController.oldNonUkMandate), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(oldMandateReference))
    val result = TestPreviousMandateRefController.submit(service, "callingPage").apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }


  def retrieveOldMandateFromSessionAuthorisedAgent(cachedData:Option[OldMandateReference] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[OldMandateReference](Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestPreviousMandateRefController.getOldMandateFromSession(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

}
