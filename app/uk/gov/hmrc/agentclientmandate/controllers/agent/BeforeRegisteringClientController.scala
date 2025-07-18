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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.utils.{ACMFeatureSwitches, ControllerPageIdConstants, MandateConstants}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}

@Singleton
class BeforeRegisteringClientController @Inject()(
                                       mcc: MessagesControllerComponents,
                                       val authConnector: AuthConnector,
                                       implicit val appConfig: AppConfig,
                                       templateBeforeRegisteringClient: views.html.agent.beforeRegisteringClient,
                                        ACMFeatureSwitches: ACMFeatureSwitches
                                       ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  val controllerId: String = ControllerPageIdConstants.beforeRegisteringClientControllerId

  def view(service: String, callingPage: String): Action[AnyContent] = Action { implicit request =>
    if (ACMFeatureSwitches.registeringClientContentUpdate.enabled) {
      Ok(templateBeforeRegisteringClient(callingPage, service, getBackLink(callingPage)))
    } else {
      Redirect(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientPermissionController.view(callingPage))
    }
  }

  def submit(): Action[AnyContent] = Action {
    Redirect(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientPermissionController.view(controllerId))
  }

  private def getBackLink(callingPage: String): String
  = {
    val paySAPageId: String = ControllerPageIdConstants.paySAQuestionControllerId

    callingPage match {
      case `paySAPageId` => routes.PaySAQuestionController.view().url
      case _ => routes.NRLQuestionController.view().url
    }
  }


  }

