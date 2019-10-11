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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector => Auditing}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.frontend.auth.connectors.{AuthConnector, DelegationConnector}
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

object FrontendAuditConnector extends Auditing with AppName {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")

  override protected def appNameConfiguration: Configuration = Play.current.configuration


}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with AppName {


  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some{Play.current.configuration.underlying}

  override protected def appNameConfiguration: Configuration = Play.current.configuration

}

object WSHttp extends WSHttp {
  override val hooks: Seq[AnyRef with HttpHook] = NoneRequired

}

object FrontendAuthConnector extends AuthConnector with ServicesConfig {
  val serviceUrl: String = baseUrl("auth")
  lazy val http: WSHttp.type = WSHttp

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode
}


object ConcreteAuthConnector extends PlayAuthConnector with ServicesConfig {
  override val serviceUrl: String = baseUrl("auth")

  override def http: CorePost = WSHttp

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode
}

object AgentClientMandateSessionCache extends SessionCache with AppName with ServicesConfig {
  override lazy val http: WSHttp.type = WSHttp
  override lazy val defaultSource: String = appName
  override lazy val baseUri: String = baseUrl("session-cache")
  override lazy val domain: String = getConfString("session-cache.domain", throw new Exception(s"Could not find config 'session-cache.domain'"))

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

object FrontendDelegationConnector extends DelegationConnector with ServicesConfig {
  val serviceUrl: String = baseUrl("delegation")
  lazy val http: WSHttp.type = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}
