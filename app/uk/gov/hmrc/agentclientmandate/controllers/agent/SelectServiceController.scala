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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.AgentSelectServiceForm.selectServiceForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentclientmandate.utils.MandateFeatureSwitches._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils

import scala.concurrent.Future

object SelectServiceController extends SelectServiceController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val agentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}

trait SelectServiceController extends FrontendController with Actions {

  def agentClientMandateService: AgentClientMandateService

  def view = AuthorisedFor(AgentRegime(), GGConfidence).async {
    implicit authContext => implicit request =>
      if(singleService.enabled) {
        agentClientMandateService.doesAgentHaveMissingEmail("ated", AuthUtils.getArn).map { agentHasMissingEmail =>
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

  def submit = AuthorisedFor(AgentRegime(), GGConfidence).async {
    implicit authContext => implicit request => selectServiceForm.bindFromRequest.fold(
      formWithError => Future.successful(BadRequest(views.html.agent.selectService(formWithError))),
      selectedService => {
        val service = selectedService.service.get
        agentClientMandateService.doesAgentHaveMissingEmail(service, AuthUtils.getArn).map { agentHasMissingEmail =>
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
