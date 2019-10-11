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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.agentclientmandate.controllers.auth.ExternalUrls

class ExternalUrlsSpec extends PlaySpec with GuiceOneServerPerSuite {

  "ExternalUrls" must {
    "contain companyAuthHost" in {
      ExternalUrls.companyAuthHost must be("http://localhost:9025")
    }
    "contain loginCallback for Agent" in {
      ExternalUrls.loginCallbackAgent must be("http://localhost:9959/mandate/agent/summary")
    }

    "contain loginCallback for Client" in {
      ExternalUrls.loginCallbackClient must be("http://localhost:9959/mandate/client/email")
    }
    "contain loginPath" in {
      ExternalUrls.loginPath must be("gg/sign-in")
    }
  }

}
