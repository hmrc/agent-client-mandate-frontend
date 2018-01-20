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
import uk.gov.hmrc.agentclientmandate.controllers.agent.EditMandateDetailsController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, EmailService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthBuilder, SessionBuilder}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class EditMandateDetailsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  "EditMandateControllerSpec" must {

    "not return NOT_FOUND at route " when {
      "GET /agent/edit-client/:service/:mandateId " in {
        val result = route(FakeRequest(GET, s"/mandate/agent/edit-client/$mandateId")).get
        status(result) mustNot be(NOT_FOUND)
      }
    }

    "return 'edit mandate' view" when {
      "clientParty exist for the mandate fetched" in {
        viewWithAuthorisedAgent(Some(mandate)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("Edit test client4 - GOV.UK")
          document.getElementById("header").text() must include("Edit test client4")
          document.getElementById("pre-header").text() must include("Manage your ATED service")
          document.getElementById("sub-heading").text() must be("Unique authorisation number AS123456")
          document.getElementById("displayName_field").text() must include("Display name")
          document.getElementById("displayName_hint").text() must include("This does not change the official company name.")
          document.getElementById("submit").text() must be("Save changes")
        }
      }
    }

    "throw No Mandate returned exception" when {
      "clientParty does exist for the mandate fetched" in {
        viewWithAuthorisedAgent() { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "", "email" -> "")
        submitEditMandateDetails(fakeRequest, false, getMandate = Some(mandate)) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("error-notification").text() must include("You must answer the client display name question You must answer the email address question")
        }
      }
    }

    "return a BAD_REQUEST" when {
      "valid form is submitted with invalid email" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@aa.com")
        submitEditMandateDetails(fakeRequest, false, getMandate = Some(mandate), editMandate = Some(mandate)) { result =>
          status(result) must be(BAD_REQUEST)
        }
      }
    }

    "throw No Mandate Found! exception" when {
      "invalid form is submitted and no valid mandate is fetched for the mandate id" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "")
        submitEditMandateDetails(fakeRequest, true, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned with id AS123456 for service ATED")
        }
      }
    }

    "valid form is submitted throw No Mandate Found! exception" when {
      "valid form is submitted but no valid mandate is fetched for the mandate id" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, true, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate Found with id AS123456 for service ATED")
        }
      }
    }

    "redirect to summary page" when {
      "valid form is submitted with valid email and mandate is edited" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, true, getMandate = Some(mandate), editMandate = Some(mandate)) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/summary"))
        }
      }
    }

    "return back to edit-client page" when {
      "valid form is submitted with valid email but mandate is NOT edited" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, true, getMandate = Some(mandate)) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/edit-client/AS123456"))
        }
      }
    }
    "return back to edit-client page with exception" when {
      "valid form is submitted with valid email but mandate is NOT edited" in {
        val fakeRequest = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, false, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned with id AS123456 for service ATED")
        }
      }
    }

  }

  val mockAuthConnector = mock[AuthConnector]
  val mockEmailService = mock[EmailService]
  val mockAcmService = mock[AgentClientMandateService]
  val service = "ATED"
  val mandateId = "AS123456"
  val clientDisplayName = "ACME Limited"


  val mandate: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation,
    ContactDetails("agent@agent.com", None)), clientParty = Some(Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))),
    currentStatus = MandateStatus(Status.Approved, DateTime.now(), "credId"),
    statusHistory = Seq(MandateStatus(Status.New, DateTime.now(), "credId")),
    Subscription(None, Service("ated", "ATED")),
    clientDisplayName = s"$clientDisplayName")

  val mandate1: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None, agentParty = Party("JARN123457", "agency name", PartyType.Organisation,
    ContactDetails("agent@agent.com", None)), clientParty = None,
    currentStatus = MandateStatus(Status.Approved, DateTime.now(), "credId"),
    statusHistory = Seq(MandateStatus(Status.New, DateTime.now(), "credId")),
    Subscription(None, Service("ated", "ATED")),
    clientDisplayName = s"$clientDisplayName")

  object TestEditMandateController extends EditMandateDetailsController {
    override val authConnector = mockAuthConnector
    override val emailService = mockEmailService
    override val acmService = mockAcmService
  }

  def viewWithAuthorisedAgent(mandate: Option[Mandate] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createOrgAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockAcmService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(mandate))
    val result = TestEditMandateController.view(service, mandateId).apply(SessionBuilder.buildRequestWithSession(userId))
    test(result)
  }

  def submitEditMandateDetails(request: FakeRequest[AnyContentAsFormUrlEncoded],
                               emailValid: Boolean,
                               getMandate: Option[Mandate] = None,
                               editMandate: Option[Mandate] = None)(test: Future[Result] => Any) {
    val userId = s"user-${UUID.randomUUID}"
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val user = AuthBuilder.createRegisteredAgentAuthContext(userId, "name")
    AuthBuilder.mockAuthorisedAgent(userId, mockAuthConnector)
    when(mockEmailService.validate(Matchers.any())(Matchers.any())).thenReturn(Future.successful(emailValid))
    when(mockAcmService.fetchClientMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(getMandate))
    when(mockAcmService.editMandate(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(editMandate))
    val result = TestEditMandateController.submit(service, mandateId).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
    test(result)
  }

}
