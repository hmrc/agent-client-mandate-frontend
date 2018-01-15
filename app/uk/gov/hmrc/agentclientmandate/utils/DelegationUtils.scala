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

package uk.gov.hmrc.agentclientmandate.utils

import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.domain.AtedUtr
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.{AuthContext, DelegationContext, Link, TaxIdentifiers}

object DelegationUtils extends ServicesConfig {

  def createDelegationContext(service: String, serviceId: String, clientName: String)(implicit ac: AuthContext): DelegationContext = {
    DelegationContext(
      principalName = clientName,
      attorneyName = ac.principal.name.getOrElse("Agent"),
      link = Link(
        url = getReturnUrl,
        text = Messages("mandate.agent.delegation.url.text")
    ),
      principalTaxIdentifiers = getPrincipalTaxIdentifiers(service, serviceId)
    )
  }

  def getPrincipalTaxIdentifiers(service: String, serviceId: String): TaxIdentifiers = {
    service.toLowerCase match {
      case "ated" => TaxIdentifiers(ated = Some(AtedUtr(serviceId)))
      case any => TaxIdentifiers()
    }
  }

  def getReturnUrl: String = s"""${getString("microservice.return-part-url")}"""

  def getDelegatedServiceRedirectUrl(service: String): String = {
    getString(s"microservice.delegated-service-redirect-url.${service.toLowerCase}")
  }

  def getDelegatedServiceHomeUrl(service: String): String = {
    getString(s"microservice.delegated-service-home-url.${service.toLowerCase}")
  }
}
