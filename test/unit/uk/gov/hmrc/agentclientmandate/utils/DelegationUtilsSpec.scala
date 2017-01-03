/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.frontend.auth.{AuthContext, TaxIdentifiers}
import unit.uk.gov.hmrc.agentclientmandate.builders.AuthBuilder

class DelegationUtilsSpec extends PlaySpec with OneServerPerSuite {

  val atedUtr = new Generator().nextAtedUtr

  "DelegationUtils" must {

    "createDelegationContext" must {
      "returns delegation context" in {
        implicit val ac: AuthContext = AuthBuilder.createRegisteredAgentAuthContext("user-id", "user-name")
        val result = DelegationUtils.createDelegationContext("ated", atedUtr.utr, "Client-Name")
        result.attorneyName must be("user-name")
        result.principalName must be("Client-Name")
      }
    }

    "getPrincipalTaxIdentifiers" must {
      "returns TaxIdentifiers with ated filled, if service=ated" in {
        DelegationUtils.getPrincipalTaxIdentifiers("ated", atedUtr.utr) must be(TaxIdentifiers(ated = Some(atedUtr)))
      }
      "returns empty TaxIdentifiers" in {
        DelegationUtils.getPrincipalTaxIdentifiers("xyz", "xyz") must be(TaxIdentifiers())
      }
    }

    "getReturnUrl" must {
      "returns return url into mandate for service specific summary page" in {
        DelegationUtils.getReturnUrl("ated") must be("http://localhost:9959/mandate/agent/summary/ated")
      }
    }

    "getDelegatedServiceRedirectUrl" must {
      "returns delegated service redirect url for specific service" in {
        DelegationUtils.getDelegatedServiceRedirectUrl("ated") must be("http://localhost:9916/ated/account-summary")
      }
    }

    "getDelegatedServiceHomeUrl" must {
      "returns delegated service home url for specific service" in {
        DelegationUtils.getDelegatedServiceHomeUrl("ated") must be("http://localhost:9916/ated/welcome")
        DelegationUtils.getDelegatedServiceHomeUrl("ATED") must be("http://localhost:9916/ated/welcome")
      }
    }
  }

}
