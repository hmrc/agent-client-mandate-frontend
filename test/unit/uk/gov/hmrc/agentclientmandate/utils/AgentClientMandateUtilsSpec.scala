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

package unit.uk.gov.hmrc.agentclientmandate.utils

import org.joda.time.DateTime
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils
import unit.uk.gov.hmrc.agentclientmandate.builders.AgentBuilder

class AgentClientMandateUtilsSpec extends PlaySpec with MockitoSugar {

  val appConfig: AppConfig = mock[AppConfig]

  "AgentClientMandateUtils" must {
    "validateUTR" must {
      "given valid UTR return true" in {
        AgentClientMandateUtils.validateUTR(Some("1111111111")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("1111111112")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("8111111113")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("6111111114")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("4111111115")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("2111111116")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("2111111117")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("9111111118")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("7111111119")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("5111111123")) must be(true)
        AgentClientMandateUtils.validateUTR(Some("3111111124")) must be(true)
      }
      "given invalid UTR return false" in {
        AgentClientMandateUtils.validateUTR(Some("2111111111")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111111")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111 111 ")) must be(false)
        AgentClientMandateUtils.validateUTR(Some("211111ab111 ")) must be(false)
      }
      "None as UTR return false" in {
        AgentClientMandateUtils.validateUTR(None) must be(false)
      }
    }

    "checkStatus" when {
      "a valid status is passed" in {
        AgentClientMandateUtils.checkStatus(Status.PendingActivation) must be("Pending")
      }
    }
  }

  "isUkAgent" must {
    "return false, when agent country is not UK" in {
      AgentClientMandateUtils.isUkAgent(AgentBuilder.buildAgentDetails) must be(false)
    }

    "return true, when agent country is UK" in {
      AgentClientMandateUtils.isUkAgent(AgentBuilder.buildUkAgentDetails) must be(true)
    }
  }


  "isNonUkClient" must {
    "return false, when mandate history has status = ACTIVE/NEW" in {
      AgentClientMandateUtils.isNonUkClient(mandate) must be(false)
    }

    "return true, when mandate history does not have status = ACTIVE/NEW" in {
      AgentClientMandateUtils.isNonUkClient(mandate1) must be(true)
    }
  }

  val mandate: Mandate = Mandate(id = "12345678", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("X0101000000101", "client name", PartyType.Organisation, ContactDetails("agent@agent.com", None))),
    currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Seq(MandateStatus(Status.New, DateTime.now(), "updatedby"),
    MandateStatus(Status.Active, DateTime.now(), "updatedby")), Subscription(None, Service("ated", "ATED")), clientDisplayName = "client display name")

  val mandate1: Mandate = Mandate(id = "12345678", createdBy = User("credId", "agentName", Some("agentCode")), None, None,
    agentParty = Party("JARN123456", "agency name", PartyType.Organisation, ContactDetails("agent@agent.com", None)),
    clientParty = Some(Party("X0101000000101", "client name", PartyType.Organisation, ContactDetails("agent@agent.com", None))),
    currentStatus = MandateStatus(Status.New, DateTime.now(), "credId"), statusHistory = Nil, Subscription(None, Service("ated", "ATED")),
    clientDisplayName = "client display name")
}
