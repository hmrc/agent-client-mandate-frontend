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
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils.isNonUkClient
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait RemoveClientController extends FrontendController with AuthorisedWrappers {

  def acmService: AgentClientMandateService

  def view(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
          mandate => Ok(views.html.agent.removeClient(new YesNoQuestionForm("agent.remove-client.error").yesNoQuestionForm,
            service, mandate.clientDisplayName, mandateId, getBackLink(service)))
        )
      }
  }

  def confirm(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        val form = new YesNoQuestionForm("agent.remove-client.error")
        form.yesNoQuestionForm.bindFromRequest.fold(
          formWithError =>
            acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
              mandate => BadRequest(views.html.agent.removeClient(formWithError, service, mandate.clientDisplayName, mandateId, getBackLink(service)))
            ),
          data => {
            val removeClient = data.yesNo.getOrElse(false)
            if (removeClient) {
              acmService.removeClient(mandateId, authRetrievals).map { removedClient =>
                if (removedClient) {
                  Redirect(routes.RemoveClientController.showConfirmation(mandateId))
                }
                else {
                  throw new RuntimeException(s"Client removal Failed with id $mandateId for service $service")
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

  def showConfirmation(service: String, mandateId: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
          mandate => Ok(views.html.agent.removeClientConfirmation(service, mandate.id, mandate.clientDisplayName, isNonUkClient(mandate)))
        )
      }
    }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
  }
}


object RemoveClientController extends RemoveClientController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val acmService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}
