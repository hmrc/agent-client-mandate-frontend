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

package uk.gov.hmrc.agentclientmandate.controllers.client

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.Status.Expired
import uk.gov.hmrc.agentclientmandate.models.ClientDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientEmailForm._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EditEmailController @Inject()(
                                     dataCacheService: DataCacheService,
                                     mandateService: AgentClientMandateService,
                                     mcc: MessagesControllerComponents,
                                     val authConnector: AuthConnector,
                                     implicit val ec: ExecutionContext,
                                     implicit val appConfig: AppConfig,
                                     templateEditEmail: views.html.client.editEmail
                                   ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def getClientMandateDetails(clientId: String, service: String, returnUrl: RedirectUrl): Action[AnyContent] = Action.async {
    implicit request => {
      withOrgCredId(Some(service)) { _ =>
        AgentClientMandateUtils.getSafeLink(returnUrl, appConfig) match {
          case Some(safeLink) =>
            mandateService.fetchClientMandateByClient(clientId, service).map {
              case Some(mandate) =>
                val clientDetails = ClientDetails(
                  mandate.agentParty.name,
                  appConfig.mandateFrontendHost + routes.RemoveAgentController.view(mandate.id, returnUrl).url,
                  mandate.clientParty.get.contactDetails.email,
                  appConfig.mandateFrontendHost + routes.EditEmailController.view(mandate.id, RedirectUrl(safeLink)).url,
                  mandate.currentStatus.status.toString)
                Ok(Json.toJson(clientDetails))

              case _ => NotFound
            }
          case None => Future.successful(BadRequest("The return url is not correctly formatted"))
        }
      }
    }
  }


  def view(mandateId: String, service: String, returnUrl: RedirectUrl): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        AgentClientMandateUtils.getSafeLink(returnUrl, appConfig) match {
          case Some(safeLink) =>
            saveBackLink(safeLink).flatMap { _ =>
              for {
                _ <- dataCacheService.cacheFormData("MANDATE_ID", mandateId)
                mandate <- mandateService.fetchClientMandate(mandateId, authRetrievals)
              } yield {
                val clientForm = ClientEmail(mandate.get.clientParty.get.contactDetails.email)
                Ok(templateEditEmail(service, clientEmailForm.fill(clientForm), Some(safeLink)))
              }
            }
          case None => Future.successful(BadRequest("The return url is not correctly formatted"))
        }


      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        clientEmailForm.bindFromRequest().fold(
          formWithError =>
            getBackLink.map(backLink => BadRequest(templateEditEmail(service, formWithError, backLink))),
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

  private def getBackLink(implicit hc: HeaderCarrier) :Future[Option[String]]= {
    dataCacheService.fetchAndGetFormData[String](backLinkId)
      .map {_.filter(_.trim.nonEmpty)}
  }

}
