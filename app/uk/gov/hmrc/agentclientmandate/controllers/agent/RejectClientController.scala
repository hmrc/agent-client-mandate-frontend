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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.utils.AgentClientMandateUtils._

import scala.concurrent.Future

object RejectClientController extends RejectClientController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  // $COVERAGE-ON$
}

trait RejectClientController extends FrontendController with Actions {

  def acmService: AgentClientMandateService

  def view(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandateClientName(mandateId).map(
        mandate => Ok(views.html.agent.rejectClient(service,
          new YesNoQuestionForm("agent.reject-client.error").yesNoQuestionForm,
          mandate.clientDisplayName, mandateId, getBackLink(service)))
      )
  }

  def submit(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      val form = new YesNoQuestionForm("agent.reject-client.error")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateClientName(mandateId).map(
            mandate => BadRequest(views.html.agent.rejectClient(service, formWithError, mandate.clientDisplayName, mandateId, getBackLink(service)))
          ),
        data => {
          val rejectClient = data.yesNo.getOrElse(false)
          if (rejectClient) {
            acmService.rejectClient(mandateId).map { rejectedClient =>
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

  def confirmation(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      acmService.fetchClientMandateClientName(mandateId).map(
        mandate =>
          Ok(views.html.agent.rejectClientConfirmation(service, mandate.clientDisplayName))
      )

  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view().url)
  }
}
