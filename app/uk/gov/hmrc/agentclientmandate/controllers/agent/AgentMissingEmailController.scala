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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentclientmandate.config.ConcreteAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentMissingEmailForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AgentMissingEmailController extends FrontendController with AuthorisedWrappers {

  def agentClientMandateService: AgentClientMandateService

  def view(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      Future.successful(Ok(views.html.agent.agentMissingEmail(agentMissingEmailForm, service)))
    }
  }

  def submit(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      agentMissingEmailForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.agent.agentMissingEmail(formWithError, service))),
        data => {
          agentClientMandateService.updateAgentMissingEmail(data.email.get, authRetrievals, service)
          Future.successful(Redirect(routes.AgentSummaryController.view(Some(service))))
        }
      )
    }
  }
}

object AgentMissingEmailController extends AgentMissingEmailController {

  // $COVERAGE-OFF$
  val agentClientMandateService: AgentClientMandateService.type = AgentClientMandateService
  val authConnector: AuthConnector = ConcreteAuthConnector
  // $COVERAGE-ON$

}
