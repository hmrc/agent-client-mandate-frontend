/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{DataCacheService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayNameForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future

object ClientDisplayNameController extends ClientDisplayNameController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val emailService: EmailService = EmailService
  // $COVERAGE-ON$
}

trait ClientDisplayNameController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def authConnector: AuthConnector

  def view(service: String, redirectUrl: Option[String]) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit user => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) map {
        case Some(clientDisplayname) => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm.fill(clientDisplayname), service, redirectUrl))
        case None => Ok(views.html.agent.clientDisplayName(clientDisplayNameForm, service, redirectUrl))
      }
  }


  def submit(service: String, redirectUrl: Option[String]) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      clientDisplayNameForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.agent.clientDisplayName(formWithError, service, redirectUrl))),
        data =>
          dataCacheService.cacheFormData[ClientDisplayName](clientDisplayNameFormId, data) map { cachedData =>
            redirectUrl match {
              case Some(redirect) => Redirect(redirect)
              case None => Redirect(routes.OverseasClientQuestionController.view(service))
            }
          }
      )
  }

  def getClientDisplayName(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId).map { displayName =>
          Ok(Json.toJson(displayName))
      }
  }
}
