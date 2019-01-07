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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentclientmandate.controllers.client.MandateDeclarationController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class MandateDeclarationControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  "MandateDeclarationController" must {

    "not return NOT_FOUND at route " when {
      "GET /mandate/client/declaration" in {
        val result = route(FakeRequest(GET, "/mandate/client/declaration")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "redirect to login page for UNAUTHENTICATED client" when {

      "client requests(GET) for review mandate view" in {
        viewUnAuthenticatedClient { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result).get must include("/gg/sign-in")
        }
      }

    }

    "return mandate declaration view for AUTHORISED client" when {

      "client requests(GET) for mandate declaration view" in {
        val cachedData = Some(ClientCache(email = Some(ClientEmail("bb@bb.com")), mandate = Some(mandate)))
        viewAuthorisedClient(cachedData) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Declaration and consent - GOV.UK")
          document.getElementById("header").text() must include("Declaration and consent")
          document.getElementById("pre-heading").text() must include("Appoint an agent")
          document.getElementById("declare-title").text() must be("I declare that:")
          document.getElementById("agent-name").text() must be("name has agreed to act on my behalf in respect of ATED")
          document.getElementById("dec-info").text() must be("the information I have provided is correct and complete")
          document.getElementById("submit").text() must be("Agree and submit")
        }
      }
    }

    "redirect to mandate review page for AUTHORISED client" when {

      "client requests(GET) for mandate declaration view but mandate not found in cache" in {
        viewAuthorisedClient(None) { result =>
          status(result) must be(SEE_OTHER)
        }
      }
    }

    "redirect to mandate confirmation page for AUTHORISED client" when {

      "valid form is submitted, mandate is found in cache and updated with status=accepted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        val cacheReturn = Some(ClientCache(mandate = Some(mandate)))
        val mandateReturned = Some(mandate)
        submitWithAuthorisedClient(fakeRequest, cacheReturn, mandateReturned) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/confirmation"))
        }
      }
    }

    "redirect to mandate confirmation page for AUTHORISED client" when {

      "valid form is submitted, mandate is found in cache but update in backend fails" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        val cacheReturn = Some(ClientCache(mandate = Some(mandate)))
        submitWithAuthorisedClient(fakeRequest, cacheReturn) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
        }
      }
    }

    "redirect to review Mandate view" when {
      "mandate is not found in cache" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody()
        submitWithAuthorisedClient(fakeRequest) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some("/mandate/client/review"))
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
    statusHistory = Nil,
    subscription = Subscription(referenceNumber = None,
      service = Service(id = "ated-ref-no", name = "ATED")),
    clientDisplayName = "client display name")

  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheService = mock[DataCacheService]
  val mockMandateService = mock[AgentClientMandateService]

  object TestMandateDeclarationController extends MandateDeclarationController {
    val authConnector = mockAuthConnector
    val dataCacheService = mockDataCacheService
    val mandateService = mockMandateService
  }

  val service = "ATED"

  def viewUnAuthenticatedClient(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    AuthBuilder.mockUnAuthenticatedClient(userId, mockAuthConnector)
    val result = TestMandateDeclarationController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def viewAuthorisedClient(cachedData: Option[ClientCache] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)
    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestMandateDeclarationController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(cachedData))
    val result = TestMandateDeclarationController.view(service).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitWithAuthorisedClient(
                                  request: FakeRequest[AnyContentAsFormUrlEncoded],
                                  clientCache: Option[ClientCache] = None,
                                  mandate: Option[Mandate] = None)(test: Future[Result] => Any
                                ) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedClient(userId, mockAuthConnector)

    when(mockDataCacheService.fetchAndGetFormData[ClientCache](Matchers.eq(TestMandateDeclarationController.clientFormId))(Matchers.any(), Matchers.any())).thenReturn(Future.successful(clientCache))

    when(mockMandateService.approveMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandate))

    val result = TestMandateDeclarationController.submit(service).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
