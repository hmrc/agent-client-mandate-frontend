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

package uk.gov.hmrc.agentclientmandate.utils

import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.models.{Link, PrincipalTaxIdentifiers, StartDelegationContext}
import uk.gov.hmrc.domain.AtedUtr

object DelegationUtils {

  def createDelegationContext(service: String,
                              serviceId: String,
                              clientName: String,
                              attorney: Option[String],
                              internal: String)(implicit messages: Messages, appConfig: AppConfig): StartDelegationContext = {
    StartDelegationContext(
      principalName = clientName,
      attorneyName = attorney.getOrElse("Agent"),
      link = Link(
        url = getReturnUrl,
        text = messages("mandate.agent.delegation.url.text")
      ),
      principalTaxIdentifiers = getPrincipalTaxIdentifiers(service, serviceId),
      internalId = internal
    )
  }

  def getPrincipalTaxIdentifiers(service: String, serviceId: String): PrincipalTaxIdentifiers = {
    service.toLowerCase match {
      case "ated" => PrincipalTaxIdentifiers(ated = Some(AtedUtr(serviceId)))
      case _ => PrincipalTaxIdentifiers()
    }
  }

  def getReturnUrl(implicit appConfig: AppConfig): String = s"""${appConfig.servicesConfig.getString("microservice.return-part-url")}"""

  def getDelegatedServiceRedirectUrl(service: String)(implicit appConfig: AppConfig): String = {
    appConfig.servicesConfig.getString(s"microservice.delegated-service-redirect-url.${service.toLowerCase}")
  }

  def getDelegatedServiceHomeUrl(service: String)(implicit appConfig: AppConfig): String = {
    appConfig.servicesConfig.getString(s"microservice.delegated-service-home-url.${service.toLowerCase}")
  }
}
