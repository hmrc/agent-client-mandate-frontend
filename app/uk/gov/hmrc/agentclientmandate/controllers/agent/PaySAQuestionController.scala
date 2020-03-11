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
import uk.gov.hmrc.agentclientmandate.utils.ControllerPageIdConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PaySAQuestion._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaySAQuestionController @Inject()(
                                         val authConnector: AuthConnector,
                                         implicit val ec: ExecutionContext,
                                         implicit val appConfig: AppConfig,
                                         val mcc: MessagesControllerComponents
                                       ) extends FrontendController(mcc) with AuthorisedWrappers {

  val controllerId: String = ControllerPageIdConstants.paySAQuestionControllerId

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        val result = Ok(views.html.agent.paySAQuestion(paySAQuestionForm, service, getBackLink(service)))
        Future.successful(result)
      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        paySAQuestionForm.bindFromRequest.fold(
          formWithErrors => {
            val result = BadRequest(views.html.agent.paySAQuestion(formWithErrors, service, getBackLink(service)))
            Future.successful(result)
          },
          data => {
            val result = if (data.paySA.getOrElse(false)) {
              Redirect(routes.MandateDetailsController.view(controllerId))
            } else {
              Redirect(routes.ClientPermissionController.view(controllerId))
            }
            Future.successful(result)
          }
        )
      }
  }


  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.NRLQuestionController.view().url)
  }
}
