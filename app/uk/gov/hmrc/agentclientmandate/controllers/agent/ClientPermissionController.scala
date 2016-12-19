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
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermissionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HttpResponse
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.Future

object ClientPermissionController extends ClientPermissionController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val businessCustomerConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
  val atedSubscriptionConnector: AtedSubscriptionFrontendConnector = AtedSubscriptionFrontendConnector
  // $COVERAGE-ON$
}

trait ClientPermissionController extends FrontendController with Actions {

  def businessCustomerConnector: BusinessCustomerFrontendConnector
  def atedSubscriptionConnector: AtedSubscriptionFrontendConnector

  def view(service: String) = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit user => implicit request =>
      for {
        clearBcResp <- businessCustomerConnector.clearCache(service)
        serviceResp <- {
          if (service.toUpperCase == "ATED") atedSubscriptionConnector.clearCache(service)
          else Future.successful(HttpResponse(OK))
        }
      } yield Ok(views.html.agent.clientPermission(clientPermissionForm, service))
  }


  def submit(service: String) = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit user => implicit request =>
      clientPermissionForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.agent.clientPermission(formWithErrors, service)),
        data => {
          if (data.hasPermission.getOrElse(false)) Redirect(nonUkUri(service))
          else Redirect(routes.AgentSummaryController.view(service))
        }
      )
  }

}
