/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.utils.{FeatureSwitch, MandateConstants}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.agentclientmandate.views
@Singleton
class CannotRegisterClientKickoutController @Inject()(
                                   mcc: MessagesControllerComponents,
                                   val authConnector: AuthConnector,
                                   implicit val ec: ExecutionContext,
                                   implicit val appConfig: AppConfig,
                                   implicit val servicesConfig: ServicesConfig,
                                   cannotRegisterClientKickoutView: views.html.agent.cannotRegisterClientKickout
                                 ) extends FrontendController(mcc)
  with AuthorisedWrappers
  with MandateConstants {
  def show(callingPage: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(None) { _ =>
      if(FeatureSwitch.isEnabled("registering_client_content_update")) {
        Future.successful(
          Ok(
            cannotRegisterClientKickoutView(
              agentSummaryUrl = routes.AgentSummaryController.view().url,
              backLink  = Some(routes.ClientPermissionController.view(callingPage).url)
            )
          )
        )
      } else {
        Future.successful(NotFound)
      }
    }
  }
}