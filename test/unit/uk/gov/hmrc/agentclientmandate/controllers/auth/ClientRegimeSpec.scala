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

package unit.uk.gov.hmrc.agentclientmandate.controllers.auth

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.agentclientmandate.controllers.auth.{ClientGovernmentGateway, ClientRegime}
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class ClientRegimeSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  val serviceName: String = "ATED"

  "ClientRegime" must {

    "extend TaxRegime" when {

      "overriding isAuthorised - when user has org account - return true" in {
        when(accounts.agent).thenReturn(None)
        when(accounts.org.isDefined).thenReturn(true)
        ClientRegime(Some(serviceName)).isAuthorised(accounts) must be(true)
      }

      "overriding isAuthorised - when user doesn't have org account - return false" in {
        when(accounts.agent).thenReturn(None)
        when(accounts.org.isDefined).thenReturn(false)
        ClientRegime(Some(serviceName)).isAuthorised(accounts) must be(false)
      }

      "overriding authenticationType" in {
        ClientRegime(Some(serviceName)).authenticationType must be(ClientGovernmentGateway(serviceName))
      }

      "overriding unauthorised page" in {
        ClientRegime(Some(serviceName)).unauthorisedLandingPage must be(None)
      }

    }

  }

  val accounts = mock[Accounts](RETURNS_DEEP_STUBS)

}
