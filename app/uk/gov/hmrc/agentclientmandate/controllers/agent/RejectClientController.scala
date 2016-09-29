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
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.RejectClientQuestionForm._

import scala.concurrent.Future

trait RejectClientController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def view(mandateId: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>

      acmService.fetchClientMandate(mandateId).map { response =>
         response match {
          case Some(mandate) => Ok(views.html.agent.rejectClient(rejectClientQuestionForm, mandate.clientParty.get.name, mandateId))
          case _ => throw new RuntimeException("No Mandate returned")
        }
      }
  }

  def confirm(mandateId: String, clientName: String) = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit authContext => implicit request =>
        rejectClientQuestionForm.bindFromRequest.fold(
            formWithError => BadRequest(views.html.agent.rejectClient(formWithError, clientName, mandateId)),
            data => {
              val rejectClient = data.rejectClient.getOrElse(false)
              if (rejectClient) {
                //TODO rejection of client
                Redirect(routes.RejectClientController.showConfirmation(clientName))
              }
              else {
                Redirect(routes.AgentClientSummaryController.view)
              }
            }
        )
  }

  def showConfirmation(clientName: String) = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Ok(views.html.agent.rejectClientConfirmation(clientName))
  }
}

object RejectClientController extends RejectClientController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  // $COVERAGE-ON$
}