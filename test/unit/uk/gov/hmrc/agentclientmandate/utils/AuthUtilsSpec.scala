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

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder._

class AuthUtilsSpec extends PlaySpec with OneServerPerSuite {

  "AuthUtils" must {

    "return true" when {
      "isRegisteredAgent is called on registered agent user" in {
        implicit val ac = createRegisteredAgentAuthContext("userId", "userName")
        AuthUtils.isRegisteredAgent must be(true)
      }
      "isOrgUser is called on an org user" in {
        implicit val ac = createOrgAuthContext("userId", "userName")
        AuthUtils.isOrgUser must be(true)
      }
    }

    "return false" when {
      "isRegisteredAgent is called on NON-registered agent user" in {
        implicit val ac = createNonRegisteredAgentAuthContext("userId", "userName")
        AuthUtils.isRegisteredAgent must be(false)
      }
      "isOrgUser is called on a NON-org user" in {
        implicit val ac = createInvalidAuthContext("userId", "userName")
        AuthUtils.isOrgUser must be(false)
      }
    }

    "return authLink" when {
      "getAgentLink is called on an agent user" in {
        implicit val ac = createNonRegisteredAgentAuthContext("userId", "userName")
        AuthUtils.getAgentLink must be(ac.principal.accounts.agent.get.link)
      }
      "getOrgLink is called on an Org user" in {
        implicit val ac = createOrgAuthContext("userId", "userName")
        AuthUtils.getOrgLink must be(ac.principal.accounts.org.get.link)
      }
      "getAuthLink is called on registered Agent user" in {
        implicit val ac = createRegisteredAgentAuthContext("userId", "userName")
        AuthUtils.getAuthLink must be(ac.principal.accounts.agent.get.link)
      }
      "getAuthLink is called on an Org user" in {
        implicit val ac = createOrgAuthContext("userId", "userName")
        AuthUtils.getAuthLink must be(ac.principal.accounts.org.get.link)
      }
    }

    "return Arn" when {
      "getArn is called on registered agent user" in {
        implicit val ac = createRegisteredAgentAuthContext("userId", "userName")
        val arn = ac.principal.accounts.agent.flatMap(_.agentBusinessUtr).
          map(_.utr).getOrElse(throw new RuntimeException("invalid authority"))
        AuthUtils.getArn must be(arn)
      }
    }

    "throws runtime exception" when {
      "getAgentLink is called on non-agent user" in {
        implicit val ac = createOrgAuthContext("userId", "userName")
        val thrown = the[RuntimeException] thrownBy AuthUtils.getAgentLink
        thrown.getMessage must be("Not an agent")
      }
      "getOrgLink is called on non-Org user" in {
        implicit val ac = createNonRegisteredAgentAuthContext("userId", "userName")
        val thrown = the[RuntimeException] thrownBy AuthUtils.getOrgLink
        thrown.getMessage must be("Not an Org user")
      }
      "getAuthLink is called on non-registered agent" in {
        implicit val ac = createNonRegisteredAgentAuthContext("userId", "userName")
        val thrown = the[RuntimeException] thrownBy AuthUtils.getAuthLink
        thrown.getMessage must be("invalid user type")
      }
      "getArn is called on non-agent user" in {
        implicit val ac = createOrgAuthContext("userId", "userName")
        val thrown = the[RuntimeException] thrownBy AuthUtils.getArn
        thrown.getMessage must be("invalid authority")
      }
    }

  }

}
