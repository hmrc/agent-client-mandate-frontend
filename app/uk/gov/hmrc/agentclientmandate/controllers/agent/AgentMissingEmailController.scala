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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Logger
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, EmailService}
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentMissingEmailForm
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentMissingEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AgentMissingEmailController extends FrontendController with Actions {

  def agentClientMandateService: AgentClientMandateService
  def emailService: EmailService

  def view(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      Future.successful(Ok(views.html.agent.agentMissingEmail(agentMissingEmailForm, service)))
  }

  def submit(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      AgentMissingEmailForm.validateAgentMissingEmail(agentMissingEmailForm.bindFromRequest).fold(
        formWithError => Future.successful(BadRequest(views.html.agent.agentMissingEmail(formWithError, service))),
        data => {
          emailService.validate(data.email.get) map { isValidEmail =>
            if (isValidEmail) {
              agentClientMandateService.updateAgentMissingEmail(data.email.get, AuthUtils.getArn, service)
              Redirect(routes.AgentSummaryController.view(Some(service)))
            } else {
              val errorMsg = Messages("agent.enter-email.error.email.invalid-by-email-service")
              val errorForm = agentMissingEmailForm.withError(key = "agent-enter-email-form", message = errorMsg).fill(data)
              BadRequest(views.html.agent.agentMissingEmail(errorForm, service))
            }
          }
        }
      )
  }

}

object AgentMissingEmailController extends AgentMissingEmailController {
  // $COVERAGE-OFF$
  val agentClientMandateService = AgentClientMandateService
  val authConnector: AuthConnector = FrontendAuthConnector
  val emailService: EmailService = EmailService
  // $COVERAGE-ON$
}
