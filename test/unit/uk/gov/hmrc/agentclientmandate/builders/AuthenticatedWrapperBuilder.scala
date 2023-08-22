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

package unit.uk.gov.hmrc.agentclientmandate.builders

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, ~}

import scala.concurrent.Future

object AuthenticatedWrapperBuilder {
  def mockAuthorisedClient(mockAuthConnector: AuthConnector): Unit = {
    when(mockAuthConnector.authorise[Option[Credentials]](ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(Some(Credentials("ggCredId", "GovernmentGateway")))
    }
  }

  type RetrievalConstruction = Enrolments ~ Option[String] ~ Some[String] ~ AgentInformation ~ Some[Credentials]

  def mockAuthorisedAgent(mockAuthConnector: AuthConnector): Unit = {
    val fakeRefNo = "ABX123456"
    val fakeRefNumberEnrolment = Enrolments(Set(Enrolment("HMRC-AGENT-AGENT", Seq(EnrolmentIdentifier("AgentRefNumber", fakeRefNo)), "Activated")))
    val agentCode = Some("agentCode")
    val agentInformation = AgentInformation(Some("agentID"), agentCode, Some("agentFriendlyName"))
    val optCredentials = Some(Credentials("providerID", "providerType"))
    val internalId = Some("internalID")

    val retrievalConstruction: RetrievalConstruction =
      new ~(
        new ~(
          new ~(
            new ~(
              fakeRefNumberEnrolment,
              internalId
            ),
            agentCode),
          agentInformation),
        optCredentials)

    when(mockAuthConnector.authorise[RetrievalConstruction](ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
        Future.successful(retrievalConstruction)
    }
  }

  def mockUnAuthenticated(mockAuthConnector: AuthConnector): Unit = {
    when(mockAuthConnector.authorise[Any](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn {
      Future.failed(MissingBearerToken("Missing bearer token"))
    }
  }
}
