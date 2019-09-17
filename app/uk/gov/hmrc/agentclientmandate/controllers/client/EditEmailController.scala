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

package uk.gov.hmrc.agentclientmandate.controllers.client

import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.{ConcreteAuthConnector, FrontendAppConfig}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.ClientDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object EditEmailController extends EditEmailController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val mandateService: AgentClientMandateService = AgentClientMandateService
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait EditEmailController extends FrontendController with AuthorisedWrappers with MandateConstants {

  def dataCacheService: DataCacheService
  def mandateService: AgentClientMandateService

  def getClientMandateDetails(clientId: String, service: String, returnUrl: ContinueUrl): Action[AnyContent] = Action.async {
    implicit request => {
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        if (!returnUrl.isRelativeOrDev(FrontendAppConfig.env)) {
          Future.successful(BadRequest("The return url is not correctly formatted"))
        }
        else {
          mandateService.fetchClientMandateByClient(clientId, service, clientAuthRetrievals).map {
            case Some(mandate) => mandate.currentStatus.status match {
              case uk.gov.hmrc.agentclientmandate.models.Status.Active =>
                val clientDetails = ClientDetails(mandate.agentParty.name, mandateFrontendHost + routes.RemoveAgentController.view(mandate.id, returnUrl).url, mandate.clientParty.get.contactDetails.email, mandateFrontendHost + routes.EditEmailController.view(mandate.id, returnUrl).url)
                Ok(Json.toJson(clientDetails))
              case _ => NotFound
            }
            case _ => NotFound
          }
        }
      }
    }
  }


  def view(mandateId: String, service: String, returnUrl: ContinueUrl): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        if (!returnUrl.isRelativeOrDev(FrontendAppConfig.env)) {
          Future.successful(BadRequest("The return url is not correctly formatted"))
        }
        else {
          saveBackLink(returnUrl.url).flatMap { _ =>
            for {
              _       <- dataCacheService.cacheFormData("MANDATE_ID", mandateId)
              mandate <- mandateService.fetchClientMandate(mandateId, authRetrievals)
            } yield {
              val clientForm = ClientEmail(mandate.get.clientParty.get.contactDetails.email)
              Ok(views.html.client.editEmail(service, clientEmailForm.fill(clientForm), Some(returnUrl.url)))
            }
          }
        }
      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        clientEmailForm.bindFromRequest.fold(
          formWithError =>
            getBackLink.map(backLink => BadRequest(views.html.client.editEmail(service, formWithError, backLink))),
          data => {
            for {
              cachedMandateId <- dataCacheService.fetchAndGetFormData[String]("MANDATE_ID")
            } yield {
              mandateService.updateClientEmail(data.email, cachedMandateId.get, clientAuthRetrievals)
            }
            getBackLink.map(backLink => Redirect(backLink.get))
          }
        )
      }
  }

  val backLinkId = "EditEmailController:BackLink"
  private def saveBackLink(redirectUrl: String)(implicit hc: HeaderCarrier): Future[String] = {
    dataCacheService.cacheFormData[String](backLinkId, redirectUrl)
  }

  private def getBackLink(implicit hc: HeaderCarrier, request: Request[AnyContent]) :Future[Option[String]]= {
    dataCacheService.fetchAndGetFormData[String](backLinkId)
      .map {_.filter(_.trim.nonEmpty)}
  }

}
