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

package unit.uk.gov.hmrc.agentclientmandate.controllers.auth

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Headers, Result, Results}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.AgentAuthRetrievals
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, EmptyRetrieval, ~}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisedWrappersSpec extends UnitSpec with MockitoSugar {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAppConfig: AppConfig = mock[AppConfig]
  implicit val fr: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(Headers("Authorization" -> "value"))

  trait Setup {
    protected val authorisedWrappers: AuthorisedWrappers = new AuthorisedWrappers {
      override def authConnector: AuthConnector = mockAuthConnector
      def loginUrl: String = "loginUrl"
      def continueUrl(isAnAgent: Boolean): String => String = (_: String) => "continueUrl"
    }
  }

  "fallbackHeaderCarrier" should {
    "return a headercarrier" when {
      "there is authorization in the request" in new Setup {
        authorisedWrappers.fallbackHeaderCarrier(hc, fr).authorization shouldBe Some(Authorization("value"))
      }

      "there is a HeaderCarrier" in new Setup {
        authorisedWrappers.fallbackHeaderCarrier(hc.copy(authorization = Some(Authorization("test"))), FakeRequest()).authorization shouldBe
          Some(Authorization("test"))
      }
    }

    "fail to return anything" when {
      "neither authorisation in the request or the HeaderCarrier" in new Setup {
        intercept[MissingBearerToken](authorisedWrappers.fallbackHeaderCarrier(hc, FakeRequest()))
      }
    }
  }

  "agentAuthenticated" should {
    "authenticate an agent" when {
      "the agent has an enrolment, with no retrieval" in new Setup {
        val future: Future[Result] = Future.successful(Results.Ok("test"))
        val body: Unit => Future[Result] = { _ =>
          future
        }

        when(mockAuthConnector.authorise(ArgumentMatchers.any(), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        await(authorisedWrappers.agentAuthenticated(None, EmptyRetrieval)(body)) shouldBe await(future)
      }
    }

    "fail to authenticate an agent" when {
      "there is no active session" in new Setup {
        val future: Future[Result] = Future.successful(Results.Ok("test"))
        val body: Unit => Future[Result] = {_ =>
          future
        }

        when(mockAuthConnector.authorise(ArgumentMatchers.any(), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(MissingBearerToken("No bearer token")))

        val result: Result = await(authorisedWrappers.agentAuthenticated(None, EmptyRetrieval)(body))
        status(result) shouldBe 303
      }

      "there is an internal error exception" in new Setup {
        val future: Future[Result] = Future.successful(Results.Ok("test"))
        val body: Unit => Future[Result] = {_ =>
          future
        }

        when(mockAuthConnector.authorise(ArgumentMatchers.any(), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(InternalError("test")))

        val result: Result = await(authorisedWrappers.agentAuthenticated(None, EmptyRetrieval)(body))
        status(result) shouldBe 500
      }

      "there are insufficient enrolments" in new Setup {
        val future: Future[Result] = Future.successful(Results.Ok("test"))
        val body: Unit => Future[Result] = {_ =>
          future
        }

        when(mockAuthConnector.authorise(ArgumentMatchers.any(), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.failed(InsufficientEnrolments("test")))

        val result: Result = await(authorisedWrappers.agentAuthenticated(None, EmptyRetrieval)(body))
        status(result) shouldBe 303
      }
    }
  }

  "clientAuthenticated" should {
    "authenticate a client" when {
      "the client has an enrolment with no retrieval" in new Setup {
        val future: Future[Result] = Future.successful(Results.Ok("test"))
        val body: Unit => Future[Result] = { _ =>
          future
        }

        when(mockAuthConnector.authorise(ArgumentMatchers.any(), ArgumentMatchers.eq(EmptyRetrieval))(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(()))

        await(authorisedWrappers.clientAuthenticated(None, EmptyRetrieval)(body)) shouldBe await(future)
      }
    }
  }

  type RetrievalConstruction = Enrolments ~ Option[String] ~ Option[String] ~ AgentInformation ~ Option[Credentials]

  "withAgentRefNumber" should {
    "authenticate an agent with their ref number" when {
      "the agent has an enrolment with an agent ref number, with no retrieval" in new Setup {
        val fakeRefNo = "ABX123456"
        val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", fakeRefNo)), "Activated")))
        def future(refNumber: AgentAuthRetrievals): Future[Result] = Future.successful(Results.Ok(refNumber.agentRef))
        val body: AgentAuthRetrievals => Future[Result] = { agentAuthRetrievals =>
          future(agentAuthRetrievals)
        }
        val agentCode = Some("agentCode")
        val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
        val optCredentials = Some(Credentials("providerID", "providerType"))
        val internalId = Some("internalId")

        val retrievalConstruction: RetrievalConstruction =
          new ~(new ~(new ~(new ~(fakeRefNumberEnrolment, internalId), agentCode), agentInformation), optCredentials)

        when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(retrievalConstruction))

        val result: Result = await(authorisedWrappers.withAgentRefNumber(None)(body))
        status(result) shouldBe 200
      }
    }

    "fail to authenticate an agent" when {
      "there is no ref number" in new Setup {
        val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(), "Activated")))
        def future(refNumber: AgentAuthRetrievals): Future[Result] = Future.successful(Results.Ok(refNumber.agentRef))
        val body: AgentAuthRetrievals => Future[Result] = { agentAuthRetrievals =>
          future(agentAuthRetrievals)
        }
        val agentCode: Option[String] = Some("agentCode")
        val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
        val optCredentials = Some(Credentials("providerID", "providerType"))
        val internalId = Some("internalId")

        val retrievalConstruction: RetrievalConstruction =
          new ~(new ~(new ~(new ~(fakeRefNumberEnrolment, internalId), agentCode), agentInformation), optCredentials)

        when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(retrievalConstruction))

        val result: Result = await(authorisedWrappers.withAgentRefNumber(None)(body))
        status(result) shouldBe 500
      }

      "there is no agent code" in new Setup {
        val fakeRefNo = "ABX123456"
        val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", fakeRefNo)), "Activated")))

        def future(refNumber: AgentAuthRetrievals): Future[Result] = Future.successful(Results.Ok(refNumber.agentRef))

        val body: AgentAuthRetrievals => Future[Result] = { agentAuthRetrievals =>
          future(agentAuthRetrievals)
        }
        val agentCode: Option[String] = None
        val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
        val optCredentials = Some(Credentials("providerID", "providerType"))
        val internalId = Some("internalId")

        val retrievalConstruction: RetrievalConstruction =
          new ~(new ~(new ~(new ~(fakeRefNumberEnrolment, internalId), agentCode), agentInformation), optCredentials)

        when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(retrievalConstruction))

        val result: Result = await(authorisedWrappers.withAgentRefNumber(None)(body))
        status(result) shouldBe 500
      }

      "there is no credentials" in new Setup {
        val fakeRefNo = "ABX123456"
        val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", fakeRefNo)), "Activated")))
        def future(refNumber: AgentAuthRetrievals): Future[Result] = Future.successful(Results.Ok(refNumber.agentRef))
        val body: AgentAuthRetrievals => Future[Result] = { agentAuthRetrievals =>
          future(agentAuthRetrievals)
        }
        val agentCode: Option[String] = Some("agentCode")
        val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
        val optCredentials = None
        val internalId = Some("internalId")

        val retrievalConstruction: RetrievalConstruction =
          new ~(new ~(new ~(new ~(fakeRefNumberEnrolment, internalId), agentCode), agentInformation), optCredentials)

        when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(retrievalConstruction))

        val result: Result = await(authorisedWrappers.withAgentRefNumber(None)(body))
        status(result) shouldBe 500
      }

      "there is no internalId" in new Setup {
        val fakeRefNo = "ABX123456"
        val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", fakeRefNo)), "Activated")))
        def future(refNumber: AgentAuthRetrievals): Future[Result] = Future.successful(Results.Ok(refNumber.agentRef))
        val body: AgentAuthRetrievals => Future[Result] = { agentAuthRetrievals =>
          future(agentAuthRetrievals)
        }
        val agentCode: Option[String] = Some("agentCode")
        val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
        val optCredentials = Some(Credentials("providerID", "providerType"))
        val internalId = None

        val retrievalConstruction: RetrievalConstruction =
          new ~(new ~(new ~(new ~(fakeRefNumberEnrolment, internalId), agentCode), agentInformation), optCredentials)

        when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(retrievalConstruction))

        val result: Result = await(authorisedWrappers.withAgentRefNumber(None)(body))
        status(result) shouldBe 500
      }
    }
  }

}