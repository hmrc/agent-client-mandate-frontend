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
import uk.gov.hmrc.agentclientmandate.controllers.agent.EditMandateDetailsController
import uk.gov.hmrc.agentclientmandate.models.{MandateStatus, Service, Status, Subscription, _}
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.views.html.agent.editClient
import uk.gov.hmrc.auth.core.AuthConnector
import unit.uk.gov.hmrc.agentclientmandate.builders.{AuthenticatedWrapperBuilder, MockControllerSetup, SessionBuilder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EditMandateDetailsControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfterEach with MockControllerSetup with GuiceOneServerPerSuite {

  "EditMandateControllerSpec" must {

    "return 'edit mandate' view" when {
      "clientParty exist for the mandate fetched" in new Setup {
        viewWithAuthorisedAgent(Some(mandate)) { result =>
          status(result) must be(OK)
          val document = Jsoup.parse(contentAsString(result))
          document.title() must be("agent.edit-mandate-details.title - GOV.UK")
          document.getElementsByTag("header").text() must include("agent.edit-mandate-details.header")
          document.getElementsByTag("header").text() must include("ated.screen-reader.section agent.edit-mandate-details.pre-header")
          document.getElementById("sub-heading").text() must be("agent.edit-mandate-details.sub-heading")
          document.getElementsByClass("govuk-label").text() must include("agent.edit-mandate-details.displayName")
          document.getElementById("displayName-hint").text() must include("agent.edit-mandate-details.hint")
          assert(document.getElementById("displayName") != null)
          assert(document.getElementsByAttributeValue("for", "email").text() contains "agent.edit-mandate-details.email")
          assert(document.getElementById("email") != null)
          document.getElementById("submit").text() must be("agent.edit-mandate-details.submit")
          assert(document.select("#remove-client-link").text() === "client.summary.client-remove")
        }
      }
    }

    "throw No Mandate returned exception" when {
      "clientParty does exist for the mandate fetched" in new Setup {
        viewWithAuthorisedAgent() { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned")
        }
      }
    }

    "returns BAD_REQUEST" when {
      "invalid form is submitted" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "", "email" -> "")
        submitEditMandateDetails(fakeRequest, emailValid = false, getMandate = Some(mandate)) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("agent.edit-client.error.dispName agent.edit-client.error.email")
          document.getElementsByClass("govuk-error-message").text() must
            include("govukErrorMessage.visuallyHiddenText: agent.edit-client.error.dispName govukErrorMessage.visuallyHiddenText: agent.edit-client.error.email")
        }
      }

      "valid form is submitted with invalid email" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aaa@aaaaaaam")
        submitEditMandateDetails(fakeRequest, emailValid = false, getMandate = Some(mandate), editMandate = Some(mandate)) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.email.error.email.invalid")
          document.getElementsByClass("govuk-error-message").text() must include("client.email.error.email.invalid")
        }
      }

      "valid form is submitted but email provided is too long" in new Setup {
        val tooLongEmail: String = "aaa@" + "a"*237 + ".com"
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> tooLongEmail)
        submitEditMandateDetails(fakeRequest, emailValid = false, getMandate = Some(mandate), editMandate = Some(mandate)) { result =>
          status(result) must be(BAD_REQUEST)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementsByClass("govuk-list").text() must include("client.email.error.email.too.long")
          document.getElementsByClass("govuk-error-message").text() must
            include("client.email.error.email.too.long")
        }
      }
    }

    "throw No Mandate Found! exception" when {
      "invalid form is submitted and no valid mandate is fetched for the mandate id" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "")
        submitEditMandateDetails(fakeRequest, emailValid = true, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned with id AS123456 for service ATED")
        }
      }
    }

    "valid form is submitted throw No Mandate Found! exception" when {
      "valid form is submitted but no valid mandate is fetched for the mandate id" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, emailValid = true, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate Found with id AS123456 for service ATED")
        }
      }
    }

    "redirect to summary page" when {
      "valid form is submitted with valid email and mandate is edited" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, emailValid = true, getMandate = Some(mandate), editMandate = Some(mandate)) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/summary"))
        }
      }
    }

    "return back to edit-client page" when {
      "valid form is submitted with valid email but mandate is NOT edited" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest().withMethod("POST").withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aa@mail.com")
        submitEditMandateDetails(fakeRequest, emailValid = true, getMandate = Some(mandate)) { result =>
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(s"/mandate/agent/edit-client/AS123456"))
        }
      }
    }
    "return back to edit-client page with exception" when {
      "valid form is submitted with valid email but mandate is NOT edited" in new Setup {
        val fakeRequest: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody("displayName" -> "disp-name", "email" -> "aamail.com")
        submitEditMandateDetails(fakeRequest, emailValid = false, None) { result =>
          val thrown = the[RuntimeException] thrownBy await(result)
          thrown.getMessage must include("No Mandate returned with id AS123456 for service ATED")
        }
      }
    }

  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockAcmService: AgentClientMandateService = mock[AgentClientMandateService]
  val service: String = "ATED"
  val mandateId: String = "AS123456"
  val clientDisplayName: String = "ACME Limited"
  val injectedViewInstanceEditClient: editClient = app.injector.instanceOf[views.html.agent.editClient]

  val mandate: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("12345671", "test client4", PartyType.Individual, ContactDetails("aa.aa@a.com", None))),
    currentStatus = MandateStatus(Status.Approved, DateTime.now(), "credId"),
    statusHistory = Seq(MandateStatus(Status.New, DateTime.now(), "credId")),
    Subscription(None, Service("ated", "ATED")),
    clientDisplayName = s"$clientDisplayName")

  val mandate1: Mandate = Mandate(id = mandateId, createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123457", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = None,
    currentStatus = MandateStatus(Status.Approved, DateTime.now(), "credId"),
    statusHistory = Seq(MandateStatus(Status.New, DateTime.now(), "credId")),
    Subscription(None, Service("ated", "ATED")),
    clientDisplayName = s"$clientDisplayName")

  class Setup {
    val controller = new EditMandateDetailsController(
      stubbedMessagesControllerComponents,
      mockAcmService,
      implicitly,
      mockAppConfig,
      mockAuthConnector,
      injectedViewInstanceEditClient
    )

    def viewWithAuthorisedAgent(mandate: Option[Mandate] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockAcmService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any())).thenReturn(Future.successful(mandate))
      val result = controller.view(service, mandateId).apply(SessionBuilder.buildRequestWithSession(userId))
      test(result)
    }

    def submitEditMandateDetails(request: FakeRequest[AnyContentAsFormUrlEncoded],
                                 emailValid: Boolean,
                                 getMandate: Option[Mandate] = None,
                                 editMandate: Option[Mandate] = None)(test: Future[Result] => Any): Unit = {
      val userId = s"user-${UUID.randomUUID}"

      AuthenticatedWrapperBuilder.mockAuthorisedAgent(mockAuthConnector)
      when(mockAcmService.fetchClientMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any())).thenReturn(Future.successful(getMandate))
      when(mockAcmService.editMandate(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any())).thenReturn(Future.successful(editMandate))
      val result = controller.submit(service, mandateId).apply(SessionBuilder.updateRequestFormWithSession(request, userId))
      test(result)
    }
  }
}
