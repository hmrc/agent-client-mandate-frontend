/*
 * Copyright 2023 HM Revenue & Customs
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

import java.util

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import scala.jdk.CollectionConverters._

@Singleton
class AgentClientMandateFrontendErrorHandler @Inject()(
                                                        val messagesApi: MessagesApi,
                                                        val configuration: Configuration,
                                                        val templateError: uk.gov.hmrc.agentclientmandate.views.html.error_template,
                                                        implicit val appConfig: AppConfig
                                                      ) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)
                                    (implicit rh: Request[_]): HtmlFormat.Appendable = {
    val bitsFromPath: Array[String] = rh.path.split("/")
    val config: util.List[String] = configuration.underlying.getStringList("microservice.servicesUsed")
    val service: Array[String] = bitsFromPath.filter(config.asScala.contains(_))

    templateError(pageTitle, heading, message, None, service.headOption, appConfig)
  }

  override def internalServerErrorTemplate(implicit request: Request[_]): Html = {
    templateError(
      Messages("agent.client.mandate.generic.error.title"),
      Messages("agent.client.mandate.generic.error.header"),
      Messages("agent.client.mandate.generic.error.message"),
      Some(Messages("agent.client.mandate.generic.error.message2")),
      None,
      appConfig)
  }
}
