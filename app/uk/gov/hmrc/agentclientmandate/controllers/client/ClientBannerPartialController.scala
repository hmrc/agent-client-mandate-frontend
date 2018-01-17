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

package uk.gov.hmrc.agentclientmandate.controllers.client

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.{FrontendAppConfig, FrontendAuthConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.views.html.partials.client_banner
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait ClientBannerPartialController extends FrontendController with Actions {

  def mandateService: AgentClientMandateService

  def getBanner(clientId: String, service: String, returnUrl: ContinueUrl) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request => {

      if (!returnUrl.isRelativeOrDev(FrontendAppConfig.env)) {
        Future.successful(BadRequest("The return url is not correctly formatted"))
      }
      else {
        mandateService.fetchClientMandateByClient(clientId, service).map {
            case Some(mandate) => mandate.currentStatus.status match {
              case uk.gov.hmrc.agentclientmandate.models.Status.Active => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(mandate.id, returnUrl).url, "attorneyBanner--client-request-accepted", "active", "approved_active"))
              case uk.gov.hmrc.agentclientmandate.models.Status.Approved => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(mandate.id, returnUrl).url, "attorneyBanner--client-request-requested", "approved", "approved_active"))
              case uk.gov.hmrc.agentclientmandate.models.Status.Rejected => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.CollectEmailController.view().url, "attorneyBanner--client-request-rejected", "rejected", "cancelled_rejected"))
              case uk.gov.hmrc.agentclientmandate.models.Status.Cancelled => Ok(client_banner(service, mandate.agentParty.name, mandateFrontendHost + routes.CollectEmailController.view().url, "attorneyBanner--client-request-rejected", "cancelled", "cancelled_rejected"))
            }
            case None => NotFound
          }
      }
    }
  }
}

object ClientBannerPartialController extends ClientBannerPartialController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val mandateService: AgentClientMandateService = AgentClientMandateService
  // $COVERAGE-ON$
}
