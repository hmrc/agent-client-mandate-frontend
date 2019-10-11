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
import uk.gov.hmrc.agentclientmandate.utils.MandateFeatureSwitches._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentSelectServiceForm.selectServiceForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object SelectServiceController extends SelectServiceController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val agentClientMandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}

trait SelectServiceController extends FrontendController with AuthorisedWrappers {

  def agentClientMandateService: AgentClientMandateService

  def view: Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(None) { authRetrievals =>
      if (singleService.enabled) {
        agentClientMandateService.doesAgentHaveMissingEmail("ated", authRetrievals).map { agentHasMissingEmail =>
          if (agentHasMissingEmail) {
            Redirect(routes.AgentMissingEmailController.view())
          }
          else {
            Redirect(routes.AgentSummaryController.view())
          }
        }
      }
      else Future.successful(Ok(views.html.agent.selectService(selectServiceForm)))
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(None) { authRetrievals =>
      selectServiceForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.agent.selectService(formWithError))),
        selectedService => {
          val service = selectedService.service.get
          agentClientMandateService.doesAgentHaveMissingEmail(service, authRetrievals).map { agentHasMissingEmail =>
            if (agentHasMissingEmail) {
              Redirect(routes.AgentMissingEmailController.view())
            }
            else {
              Redirect(routes.AgentSummaryController.view())
            }
          }
        }
      )
    }
  }

}
