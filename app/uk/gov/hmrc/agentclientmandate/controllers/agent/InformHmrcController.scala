/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InformHmrcController @Inject()(
                                      mcc: MessagesControllerComponents,
                                      implicit val ec: ExecutionContext,
                                      implicit val appConfig: AppConfig,
                                      val authConnector: AuthConnector,
                                      templateInformHMRC: views.html.agent.informHmrc
                                    ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants  {

  def view(service: String, callingPage: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      Future.successful(Ok(templateInformHMRC(callingPage, service, getBackLink(callingPage))))
    }
  }

  def continue(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        Future.successful(Redirect(appConfig.nonUkUri(service, routes.InformHmrcController.view(callingPage).url)))
      }
  }

  private def getBackLink(callingPage: String): Option[String] = {
    Some(routes.PreviousUniqueAuthorisationNumberController.view(callingPage).url)
  }
}
