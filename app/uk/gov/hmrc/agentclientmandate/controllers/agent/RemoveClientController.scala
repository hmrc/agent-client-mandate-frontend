/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.mvc.Action
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentclientmandate.views
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

trait RemoveClientController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def view(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>

      acmService.fetchClientMandateClientName(mandateId).map(
        clientName => Ok(views.html.agent.removeClient(new YesNoQuestionForm("agent.remove-client.error").yesNoQuestionForm,
          service, clientName, mandateId))
      )
  }

  def confirm(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      val form = new YesNoQuestionForm("agent.remove-client.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateClientName(mandateId).map(
            clientName =>  BadRequest(views.html.agent.removeClient(formWithError, service, clientName, mandateId))
          ),
        data => {
          val removeClient = data.yesNo.getOrElse(false)
          if (removeClient) {
            acmService.removeClient(mandateId).map { removedClient =>
              if (removedClient) {
                Redirect(routes.RemoveClientController.showConfirmation(service, mandateId))
              }
              else {
                throw new RuntimeException(s"Client removal Failed with id $mandateId for service $service")
              }
            }
          }
          else {
            Future.successful(Redirect(routes.AgentSummaryController.view(service)))
          }
        }
      )
  }

  def showConfirmation(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandateClientName(mandateId).map(
        clientName =>  Ok(views.html.agent.removeClientConfirmation(service, clientName))
      )
    }
}


object RemoveClientController extends RemoveClientController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  // $COVERAGE-ON$
}
