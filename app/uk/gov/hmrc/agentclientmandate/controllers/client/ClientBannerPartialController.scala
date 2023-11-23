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

package uk.gov.hmrc.agentclientmandate.controllers.client

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.Status.{Active, Approved, Cancelled, Rejected}
import uk.gov.hmrc.agentclientmandate.service.AgentClientMandateService
import uk.gov.hmrc.agentclientmandate.utils.RelativeOrAbsoluteWithHostnameFromAllowlist
import uk.gov.hmrc.agentclientmandate.views.html.partials.client_banner
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientBannerPartialController @Inject()(mcc: MessagesControllerComponents,
                                              val authConnector: AuthConnector,
                                              mandateService: AgentClientMandateService,
                                              implicit val ec: ExecutionContext,
                                              implicit val appConfig: AppConfig) extends FrontendController(mcc) with AuthorisedWrappers {
  def getBanner(clientId: String, service: String, returnUrl: RedirectUrl): Action[AnyContent] = Action.async {
    implicit request => {
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        val mandateHost = appConfig.mandateFrontendHost

        getSafeLink(returnUrl) match {
          case Some(_) =>
            mandateService.fetchClientMandateByClient(clientId, service).map {
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
                case _ =>
                  logger.warn(s"[ClientBannerPartialController][getBanner] - status not valid")
                  NotFound
              }
              case None => NotFound
            }
          case None => Future.successful(BadRequest("The return url is not correctly formatted"))
        }

      }
    }
  }

  private def getSafeLink(theUrl: RedirectUrl) = {
    try {
      val policy = new RelativeOrAbsoluteWithHostnameFromAllowlist(appConfig.environment)
      Some(policy.url(theUrl))
    } catch {
      case _: Exception => None
    }
  }
}
