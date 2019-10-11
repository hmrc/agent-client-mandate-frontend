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
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object RejectClientController extends RejectClientController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val acmService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}

trait RejectClientController extends FrontendController with AuthorisedWrappers {

  def acmService: AgentClientMandateService

  def view(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
        mandate => Ok(views.html.agent.rejectClient(service,
          new YesNoQuestionForm("agent.reject-client.error").yesNoQuestionForm,
          mandate.clientDisplayName, mandateId, getBackLink(service)))
      )
    }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      val form = new YesNoQuestionForm("agent.reject-client.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
            mandate => BadRequest(views.html.agent.rejectClient(service, formWithError, mandate.clientDisplayName, mandateId, getBackLink(service)))
          ),
        data => {
          val rejectClient = data.yesNo.getOrElse(false)
          if (rejectClient) {
            acmService.rejectClient(mandateId, authRetrievals).map { rejectedClient =>
              if (rejectedClient) {
                Redirect(routes.RejectClientController.confirmation(mandateId))
              }
              else {
                throw new RuntimeException(s"Client Rejection Failed with id $mandateId for service $service")
              }
            }
          }
          else {
            Future.successful(Redirect(routes.AgentSummaryController.view()))
          }
        }
      )
    }
  }

  def confirmation(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
        mandate =>
          Ok(views.html.agent.rejectClientConfirmation(service, mandate.clientDisplayName))
      )
    }
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
  }
}
