/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.config

import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util.Base64
import javax.inject.{Inject, Named}
import scala.util.Try

class AppConfig @Inject()(
                           val servicesConfig: ServicesConfig,
                           val environment: Environment,
                           val configuration: Configuration,
                           @Named("appName") val appName: String
                         ) extends ExternalUrls with CountryCodes {

  private def loadConfig(key: String) = servicesConfig.getString(key)

  private val contactHost = servicesConfig.getString("contact-frontend.host")
  private val contactFormServiceIdentifier = "agent-client-mandate-frontend"

  lazy val defaultBetaFeedbackUrl = s"$contactHost/contact/beta-feedback"

  def betaFeedbackUrl(service: Option[String], returnUri: String): String = {
    val feedbackUrl = service match {
      case Some(delegatedService) if delegatedService.nonEmpty =>
        Try(servicesConfig.getString(s"microservice.delegated-service.${delegatedService.toLowerCase}.beta-feedback-url")).getOrElse(defaultBetaFeedbackUrl)
      case _ => defaultBetaFeedbackUrl
    }
    feedbackUrl + "?return=" + returnUri
  }

  lazy val urBannerToggle: Boolean = loadConfig("urBanner.toggle").toBoolean
  lazy val urBannerLink: String = loadConfig("urBanner.link")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  lazy val logoutUrl = s"""${Try(servicesConfig.getString("microservice.logout.url")).getOrElse("/bas-gateway/sign-out-without-state")}"""
  lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt

  lazy val baseDataCacheUri: String = servicesConfig.baseUrl("cachable.session-cache")
  lazy val dataCacheDefaultSource: String = appName
  lazy val dataCacheDomain: String = servicesConfig.getConfString(
    "cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'")
  )

  def nonUkUri(service: String, backLinkUrl: String): String = {
    val forwardUrl =
      s"""${
        servicesConfig.getString("microservice.services.business-customer-frontend.nonUK-uri")
      }/${service.toLowerCase}"""

    forwardUrl + "?backLinkUrl=" + mandateFrontendHost + backLinkUrl
  }

  def addNonUkClientCorrespondenceUri(backLinkUrl: String): String = {
    val forwardUrl =
      s"""${
        servicesConfig.getString(s"microservice.services.ated-subscription-frontend.subscriptionUrl")
      }"""
    forwardUrl + "?backLinkUrl=" + mandateFrontendHost + backLinkUrl
  }


  def serviceSignOutUrl(service: Option[String]): String = {
    service match {
      case Some(delegatedService) if delegatedService.nonEmpty =>
        Try(servicesConfig.getString(s"microservice.delegated-service-sign-out-url.${delegatedService.toLowerCase}")).getOrElse(logoutUrl)
      case _ => logoutUrl
    }
  }

  lazy val mandateFrontendHost: String = servicesConfig.getString(s"microservice.services.agent-client-mandate-frontend.host")

  lazy val servicesUsed: List[String] = {
    val base64String = servicesConfig.getString("microservice.servicesUsed")

    new String(Base64.getDecoder.decode(base64String), "UTF-8").split(",").toList
  }


  lazy val cookies: String = servicesConfig.getString("urls.footer.cookies")
  lazy val accessibilityStatement: String = servicesConfig.getString("urls.footer.accessibility_statement")
  lazy val privacy: String = servicesConfig.getString("urls.footer.privacy_policy")
  lazy val termsConditions: String = servicesConfig.getString("urls.footer.terms_and_conditions")
  lazy val govukHelp: String = servicesConfig.getString("urls.footer.help_page")
}


