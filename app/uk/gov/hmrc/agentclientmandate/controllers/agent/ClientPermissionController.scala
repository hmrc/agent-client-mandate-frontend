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

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{ControllerPageIdConstants, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermission
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientPermissionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientPermissionController @Inject()(
                                            businessCustomerConnector: BusinessCustomerFrontendConnector,
                                            atedSubscriptionConnector: AtedSubscriptionFrontendConnector,
                                            dataCacheService: DataCacheService,
                                            mcc: MessagesControllerComponents,
                                            val authConnector: AuthConnector,
                                            implicit val ec: ExecutionContext,
                                            implicit val appConfig: AppConfig
                                          ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        for {
          clientPermission <- dataCacheService.fetchAndGetFormData[ClientPermission](clientPermissionFormId)
          _ <- businessCustomerConnector.clearCache(service)
          _ <- {
            if (service.toUpperCase == "ATED") atedSubscriptionConnector.clearCache(service)
            else Future.successful(HttpResponse(OK))
          }
        } yield Ok(views.html.agent.clientPermission(clientPermissionForm.fill(clientPermission.getOrElse(ClientPermission())), service, callingPage, getBackLink(service, callingPage)))
      }
  }


  def submit(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        clientPermissionForm.bindFromRequest.fold(
          formWithErrors => {
            val result = BadRequest(views.html.agent.clientPermission(formWithErrors, service, callingPage, getBackLink(service, callingPage)))
            Future.successful(result)
          },
          data => {
            dataCacheService.cacheFormData[ClientPermission](clientPermissionFormId, data)
            val result = if (data.hasPermission.getOrElse(false)) {
              Redirect(routes.HasClientRegisteredBeforeController.view(callingPage))
            } else {
              Redirect(routes.AgentSummaryController.view(Some(service)))
            }

            Future.successful(result)
          }
        )
      }
  }

  private def getBackLink(service: String, callingPage: String) = {
    val pageId: String = ControllerPageIdConstants.paySAQuestionControllerId

    callingPage match {
      case `pageId` => Some(routes.PaySAQuestionController.view().url)
      case _        => Some(routes.NRLQuestionController.view().url)
    }
  }
}
