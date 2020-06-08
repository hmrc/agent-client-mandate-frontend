/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models.PrincipalTaxIdentifiers
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils
import uk.gov.hmrc.domain.{AtedUtr, Generator}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class DelegationUtilsSpec extends PlaySpec  with MockitoSugar {

  val atedUtr: AtedUtr = new Generator().nextAtedUtr

  implicit val appConfig: AppConfig = mock[AppConfig]
  implicit val messages: Messages = mock[Messages]


  implicit val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  when(appConfig.servicesConfig)
    .thenReturn(mockServicesConfig)

  when(mockServicesConfig.getString(ArgumentMatchers.eq("microservice.delegated-service-redirect-url.ated")))
    .thenReturn("http://localhost:9916/ated/account-summary")
  when(mockServicesConfig.getString(ArgumentMatchers.eq("microservice.delegated-service-home-url.ated")))
    .thenReturn("https://www.gov.uk/guidance/register-for-the-annual-tax-on-enveloped-dwellings-online-service")
  when(mockServicesConfig.getString(ArgumentMatchers.eq("microservice.return-part-url")))
    .thenReturn("http://localhost:9959/mandate/agent/summary")

  "DelegationUtils" must {

    "createDelegationContext" must {
      "returns delegation context" in {
        val result = DelegationUtils.createDelegationContext("ated", atedUtr.utr, "Client-Name", Some("user-name"), "internalID")
        result.attorneyName must be("user-name")
        result.principalName must be("Client-Name")
      }
    }

    "getPrincipalTaxIdentifiers" must {
      "returns TaxIdentifiers with ated filled, if service=ated" in {
        DelegationUtils.getPrincipalTaxIdentifiers("ated", atedUtr.utr) must be(PrincipalTaxIdentifiers(ated = Some(atedUtr)))
      }
      "returns empty TaxIdentifiers" in {
        DelegationUtils.getPrincipalTaxIdentifiers("xyz", "xyz") must be(PrincipalTaxIdentifiers())
      }
    }

    "getReturnUrl" must {
      "returns return url into mandate for service specific summary page" in {
        DelegationUtils.getReturnUrl must be("http://localhost:9959/mandate/agent/summary")
      }
    }

    "getDelegatedServiceRedirectUrl" must {
      "returns delegated service redirect url for specific service" in {
        DelegationUtils.getDelegatedServiceRedirectUrl("ated") must be("http://localhost:9916/ated/account-summary")
      }
    }

    "getDelegatedServiceHomeUrl" must {
      "returns delegated service home url for specific service" in {
        DelegationUtils.getDelegatedServiceHomeUrl("ated") must
          be("https://www.gov.uk/guidance/register-for-the-annual-tax-on-enveloped-dwellings-online-service")
        DelegationUtils.getDelegatedServiceHomeUrl("ATED") must
          be("https://www.gov.uk/guidance/register-for-the-annual-tax-on-enveloped-dwellings-online-service")
      }
    }
  }

}
