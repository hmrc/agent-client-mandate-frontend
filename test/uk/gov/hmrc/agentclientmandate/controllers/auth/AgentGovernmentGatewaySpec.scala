/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers.auth

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class AgentGovernmentGatewaySpec extends PlaySpec with OneServerPerSuite {

  "AgentGovernmentGateway" must {

    "extend Government gateway trait" when {

      "overriding loginURL" in {
        AgentGovernmentGateway.loginURL must be("http://localhost:9025/gg/sign-in")
      }

      "overriding continueURL" in {
        AgentGovernmentGateway.continueURL must be("http://localhost:9959/mandate/agent/select-service")
      }

    }
  }

}
