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

package uk.gov.hmrc.agentclientmandate.controllers.client

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.Status.{Active, Approved, Cancelled, Rejected}
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views.html.partials.client_banner
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class ClientBannerPartialController @Inject()(mcc: MessagesControllerComponents,
                                              val authConnector: AuthConnector,
                                              mandateService: AgentClientMandateService,
                                              implicit val ec: ExecutionContext,
                                              implicit val appConfig: AppConfig) extends FrontendController(mcc) with AuthorisedWrappers {
  def getBanner(clientId: String, service: String, returnUrl: String): Action[AnyContent] = Action.async {
    implicit request => {
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        val mandateHost = appConfig.mandateFrontendHost

        mandateService.fetchClientMandateByClient(clientId, service, clientAuthRetrievals).map {
          case Some(mandate) => mandate.currentStatus.status match {
            case Active => Ok(client_banner(service, mandate.agentParty.name, mandateHost
              + uk.gov.hmrc.agentclientmandate.controllers.client.routes.RemoveAgentController.view(
              mandate.id, returnUrl).url, "attorneyBanner--client-request-accepted", "active", "approved_active"))
            case Approved => Ok(client_banner(service, mandate.agentParty.name, mandateHost
              + uk.gov.hmrc.agentclientmandate.controllers.client.routes.RemoveAgentController.view(
              mandate.id, returnUrl).url, "attorneyBanner--client-request-requested", "approved", "approved_active"))
            case Rejected => Ok(client_banner(service, mandate.agentParty.name, mandateHost
              + uk.gov.hmrc.agentclientmandate.controllers.client.routes.CollectEmailController.view().url,
              "attorneyBanner--client-request-rejected", "rejected", "cancelled_rejected"))
            case Cancelled => Ok(client_banner(service, mandate.agentParty.name, mandateHost
              + uk.gov.hmrc.agentclientmandate.controllers.client.routes.CollectEmailController.view().url,
              "attorneyBanner--client-request-rejected", "cancelled", "cancelled_rejected"))
          }
          case None => NotFound
        }
      }
    }
  }
}