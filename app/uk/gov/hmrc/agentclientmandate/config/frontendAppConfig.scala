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

package uk.gov.hmrc.agentclientmandate.config

import play.api.{Configuration, Play}
import play.api.Mode.Mode
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.collection.JavaConverters._

trait AppConfig {
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val defaultBetaFeedbackUrl: String
  def betaFeedbackUrl(service: Option[String], returnUri: String): String
  val betaFeedbackUnauthenticatedUrl: String
  val logoutUrl: String
  val mandateFrontendHost: String
  val defaultTimeoutSeconds: Int
  val timeoutCountdown: Int
  val servicesUsed: List[String]
  val urBannerToggle: Boolean

  def serviceSignOutUrl(service: Option[String]): String
  def nonUkUri(service: String, backLinkUrl: String): String
  def addNonUkClientCorrespondenceUri(service: String, backLinkUrl: String): String

  protected def mode: Mode = Play.current.mode

  protected def runModeConfiguration: Configuration = Play.current.configuration

}

object FrontendAppConfig extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private val contactHost = configuration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "agent-client-mandate-frontend"


  override lazy val defaultBetaFeedbackUrl = s"$contactHost/contact/beta-feedback"

  override def betaFeedbackUrl(service: Option[String], returnUri: String) = {
    val feedbackUrl = service match {
      case Some(delegatedService) if (!delegatedService.isEmpty()) =>
        configuration.getString(s"microservice.delegated-service.${delegatedService.toLowerCase}.beta-feedback-url").getOrElse(defaultBetaFeedbackUrl)
      case _ => defaultBetaFeedbackUrl
    }
    feedbackUrl + "?return=" + returnUri
  }

  override lazy val urBannerToggle: Boolean = loadConfig("urBanner.toggle").toBoolean
  lazy val urBannerLink: String = loadConfig("urBanner.link")
  override lazy val analyticsToken = loadConfig("google-analytics.token")
  override lazy val analyticsHost = loadConfig("google-analytics.host")
  override lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/contact/beta-feedback-unauthenticated"
  override lazy val logoutUrl = s"""${configuration.getString("microservice.logout.url").getOrElse("/gg/sign-out")}"""
  override lazy val timeoutCountdown: Int = loadConfig("timeoutCountdown").toInt
  override lazy val defaultTimeoutSeconds: Int = loadConfig("defaultTimeoutSeconds").toInt

  override def nonUkUri(service: String, backLinkUrl: String): String = {
    val forwardUrl =
      s"""${
        configuration.getString("microservice.services.business-customer-frontend.nonUK-uri").
          getOrElse("")
      }/${service.toLowerCase}"""
    val returnUrl =
      s"""${
        configuration.getString("microservice.services.business-customer-frontend.nonUK-return-uri").
          getOrElse("")
      }/${service.toLowerCase}"""

    forwardUrl + "?backLinkUrl=" + mandateFrontendHost + backLinkUrl
  }

  override def addNonUkClientCorrespondenceUri(service: String, backLinkUrl: String): String = {
    val forwardUrl =
      s"""${
        configuration.getString(s"microservice.services.ated-subscription-frontend.subscriptionUrl").
          getOrElse("")
      }"""
    forwardUrl + "?backLinkUrl=" + mandateFrontendHost + backLinkUrl
  }


  override def serviceSignOutUrl(service: Option[String]): String = {
    service match {
      case Some(delegatedService) if !delegatedService.isEmpty =>
        configuration.getString(s"microservice.delegated-service-sign-out-url.${delegatedService.toLowerCase}").getOrElse(logoutUrl)
      case _ => logoutUrl
    }
  }

  override lazy val mandateFrontendHost = configuration.getString(s"microservice.services.agent-client-mandate-frontend.host").getOrElse("")

  override lazy val servicesUsed: List[String] = {
    configuration.getStringList("microservice.servicesUsed").map(_.asScala.toList) getOrElse (throw new Exception(s"Missing configuration for services used"))
  }
}


