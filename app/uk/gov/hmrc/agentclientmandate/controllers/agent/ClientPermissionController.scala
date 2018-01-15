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
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermission

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

object ClientPermissionController extends ClientPermissionController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val businessCustomerConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
  val atedSubscriptionConnector: AtedSubscriptionFrontendConnector = AtedSubscriptionFrontendConnector
  val dataCacheService: DataCacheService = DataCacheService

  // $COVERAGE-ON$
}

trait ClientPermissionController extends FrontendController with Actions with MandateConstants {

  def businessCustomerConnector: BusinessCustomerFrontendConnector
  def atedSubscriptionConnector: AtedSubscriptionFrontendConnector
  def dataCacheService: DataCacheService

  def view(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      for {
        clientPermission <- dataCacheService.fetchAndGetFormData[ClientPermission](clientPermissionFormId)
        clearBcResp <- businessCustomerConnector.clearCache(service)
        serviceResp <- {
          if (service.toUpperCase == "ATED") atedSubscriptionConnector.clearCache(service)
          else Future.successful(HttpResponse(OK))
        }
      } yield Ok(views.html.agent.clientPermission(clientPermissionForm.fill(clientPermission.getOrElse(ClientPermission())), service, callingPage, getBackLink(service, callingPage)))
  }


  def submit(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence) {
    implicit user => implicit request =>
      clientPermissionForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.agent.clientPermission(formWithErrors, service, callingPage, getBackLink(service, callingPage))),
        data => {
          dataCacheService.cacheFormData[ClientPermission](clientPermissionFormId, data)
          if (data.hasPermission.getOrElse(false))
            Redirect(routes.HasClientRegisteredBeforeController.view(callingPage))
          else
            Redirect(routes.AgentSummaryController.view(Some(service)))
        }
      )
  }

  private def getBackLink(service: String, callingPage: String) = {
    callingPage match {
      case PaySAQuestionController.controllerId => Some(routes.PaySAQuestionController.view().url)
      case _ => Some(routes.NRLQuestionController.view().url)
    }
  }
}
