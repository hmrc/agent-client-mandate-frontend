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

package helpers.application

import helpers.wiremock.WireMockConfig
import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentclientmandate.repositories.SessionCacheRepository

trait IntegrationApplication
    extends GuiceOneServerPerSuite
    with WireMockConfig {
  self: TestSuite =>

  lazy val appConfig: Map[String, Any] = Map(
    "microservice.services.auth.host" -> wireMockHost,
    "microservice.services.auth.port" -> wireMockPort
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig)
    .build()

  val currentAppBaseUrl: String = "agent-client-mandate-frontend"
  val testAppUrl: String = s"http://localhost:$port/mandate"

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
}
