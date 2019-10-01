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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.collection.JavaConverters._

@Singleton
class AgentClientMandateFrontendErrorHandler @Inject()(
                                                        val messagesApi: MessagesApi,
                                                        val configuration: Configuration,
                                                        implicit val appConfig: AppConfig
                                                      ) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): HtmlFormat.Appendable = {
    val bitsFromPath: Array[String] = rh.path.split("/")
    val servicesUsed: List[String] = configuration.getStringList("microservice.servicesUsed")
      .map (_.asScala.toList) getOrElse (throw new Exception(s"Missing configuration for services used"))

    val service: Array[String] = bitsFromPath.filter(servicesUsed.contains(_))

    uk.gov.hmrc.agentclientmandate.views.html.error_template(pageTitle, heading, message, service.headOption)
  }

}