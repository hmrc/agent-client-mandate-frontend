/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RejectClientController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        acmService: AgentClientMandateService,
                                        implicit val ec: ExecutionContext,
                                        implicit val appConfig: AppConfig,
                                        val authConnector: AuthConnector
                                      ) extends FrontendController(mcc) with AuthorisedWrappers {

  def view(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
        mandate => Ok(views.html.agent.rejectClient(service,
          new YesNoQuestionForm("yes-no.error.mandatory.clientReject").yesNoQuestionForm,
          mandate.clientDisplayName, mandateId, getBackLink(service)))
      )
    }
  }

  def submit(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      val form = new YesNoQuestionForm("yes-no.error.mandatory.clientReject")
      form.yesNoQuestionForm.bindFromRequest.fold(
        formWithError =>
          acmService.fetchClientMandateClientName(mandateId, authRetrievals).map(
            mandate => BadRequest(views.html.agent.rejectClient(service, formWithError, mandate.clientDisplayName, mandateId, getBackLink(service)))
          ),
        data => {
          if (data.yesNo) {
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
