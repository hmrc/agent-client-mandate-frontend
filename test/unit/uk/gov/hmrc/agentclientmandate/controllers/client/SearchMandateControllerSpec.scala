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

package unit.uk.gov.hmrc.agentclientmandate.controllers.client

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
import uk.gov.hmrc.agentclientmandate.controllers.client.SearchMandateController
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class SearchMandateControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "SearchMandateController" must {

    "not return NOT_FOUND at route " when {

      "GET /mandate/client/search" in {
        val result = route(FakeRequest(GET, "/mandate/client/search")).get
        status(result) mustNot be(NOT_FOUND)
      }

    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for search mandate view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "redirect to unauthorised page for UNAUTHORISED client" when {

      "client requests for search mandate view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return search mandate view for AUTHORISED client" when {

      "client requests(GET) for search mandate view" in {
        viewWithAuthorisedClient() { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your unique authorisation number? - GOV.UK")
          document.getElementById("header").text() must include("What is your unique authorisation number?")
          document.getElementById("mandateRef").`val`() must be("")
          document.getElementById("submit").text() must be("Continue")
        }
      }

      "client requests(GET) for search mandate view pre-populated and the data has been cached" in {
        val cached = ClientCache(mandate = Some(mandate1))
        viewWithAuthorisedClient(Some(cached)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("What is your unique authorisation number? - GOV.UK")
          document.getElementById("header").text() must include("What is your unique authorisation number?")
          document.getElementById("mandateRef").`val`() must be("ABC123")
          document.getElementById("submit").text() must be("Continue")
        }
      }

    }

    "redirect to 'Review Mandate view' view for Authorised Client" when {

      "valid form is submitted, mandate is found from backend, cache object exists and update of cache with mandate is successful" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }

      "valid form is submitted but with mandate having spaces, mandate is found from backend, cache object exists and update of cache with mandate is successful" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"   $mandateId   ")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = Some(ClientEmail("bb@bb.com")))
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }

      "throw an exception when cached email not found from cache" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val clientParty = Some(Party("client-id", "client name",
          `type` = PartyType.Organisation, contactDetails = ContactDetails("bb@bb.com", None)))
        val cachedData = ClientCache(email = None)
        val mandate1 = mandate.copy(clientParty = clientParty)
        val returnCache = cachedData.copy(mandate = Some(mandate1))
        submitWithAuthorisedClient(request = fakeRequest, cachedData = Some(cachedData), mandate = Some(mandate1), returnCache = returnCache) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("email not cached")
        }
      }
    }

    "redirect to 'collect email' view for authorised client" when {
      "valid form is submitted, mandate is found from backend, but cache object doesn't exist" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> s"$mandateId")
        val returnCache = ClientCache(mandate = Some(mandate))
        submitWithAuthorisedClient(request = fakeRequest, cachedData = None, mandate = Some(mandate), returnCache = returnCache) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/email"))
        }
      }
    }


    "returns BAD_REQUEST" when {
      "empty form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("You must answer unique authorisation number question")
          verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "mandateRef field has more than expected length" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "a" * 11)
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("A unique authorisation number cannot be more than 8 characters")
          verify(mockMandateService, times(0)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "invalid agent reference is passed" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("The unique authorisation number you entered cannot be found. Check the number, or enter a different number.")
          verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
        }
      }

      "agent reference is already used" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("mandateRef" -> "A1B2C3D4")
        val returnCache = ClientCache(mandate = Some(mandate1))
        submitWithAuthorisedClient(request = fakeRequest, cachedData = None, mandate = Some(mandate1), returnCache = returnCache) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-list").text() must include("There is a problem with the unique authorisation number question")
          document.getElementsByClass("error-notification").text() must include("The unique authorisation number you entered has already been used. Check the number, or enter a different number.")
          verify(mockMandateService, times(1)).fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).fetchAndGetFormData[ClientCache](Matchers.any())(Matchers.any(), Matchers.any())
          verify(mockDataCacheService, times(0)).cacheFormData[ClientCache](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
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
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val mandate1 = Mandate(id = mandateId, createdBy = User("cerdId", "Joe Bloggs"),
    agentParty = Party("ated-ref-no", "name", `type` = PartyType.Organisation,
      contactDetails = ContactDetails("aa@aa.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(status = Status.Approved, DateTime.now(), updatedBy = ""),
    statusHistory = Nil, subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "")),
    clientDisplayName = "client display name")
  val service = "ATED"

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestSearchMandateController extends SearchMandateController {
    override val authConnector = mockAuthConnector
    override val dataCacheService = mockDataCacheService
    override val mandateService = mockMandateService
  }

  override def beforeEach(): Unit = {
    reset(mockMandateService)
    reset(mockDataCacheService)
    reset(mockAuthConnector)
  }

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestSearchMandateController.view(service).apply(SessionBuilder.buildRequestWithSessionNoUser)
    test(result)
  }

  def viewWithAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestSearchMandateController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 cachedData: Option[ClientCache] = None,
                                 mandate: Option[Mandate] = None,
                                 returnCache: ClientCache = ClientCache())(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    when(mockMandateService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandate))
    when(mockDataCacheService.cacheFormData[ClientCache](Matchers.eq(TestSearchMandateController.clientFormId), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnCache))
    val result = TestSearchMandateController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
