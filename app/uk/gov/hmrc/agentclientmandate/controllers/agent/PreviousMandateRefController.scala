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

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.models.{Mandate, OldMandateReference}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.MandateReferenceForm.mandateRefForm
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail, MandateReference}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object PreviousMandateRefController extends PreviousMandateRefController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val mandateService: AgentClientMandateService = AgentClientMandateService
}

trait PreviousMandateRefController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def mandateService: AgentClientMandateService

  def view(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map { a =>
          a.flatMap(_.mandate) match {
            case Some(x) => Ok(views.html.agent.searchPreviousMandate(service, mandateRefForm.fill(MandateReference(x.id)), callingPage, getBackLink(service, callingPage)))
            case None => Ok(views.html.agent.searchPreviousMandate(service, mandateRefForm, callingPage, getBackLink(service, callingPage)))
          }
        }
  }

  def submit(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>
        mandateRefForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.agent.searchPreviousMandate(service, formWithErrors, callingPage, getBackLink(service, callingPage)))),
          data => {
            mandateService.fetchClientMandate(data.mandateRef.toUpperCase) flatMap {
              case Some(x) =>
                dataCacheService.cacheFormData[OldMandateReference](oldNonUkMandate, OldMandateReference(x.id,
                  x.subscription.referenceNumber.getOrElse(throw new RuntimeException("No Client Ref no. found!"))))
                dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(Some(ClientEmail(x.clientParty.map(_.contactDetails.email).getOrElse(""))), Some(x))) flatMap { cacheResp =>
                  Future.successful(Redirect(addNonUkClientCorrespondenceUri(service, routes.PreviousMandateRefController.view(callingPage).url)))
                }
              case None =>
                val errorMsg = Messages("client.search-mandate.error.mandateRef.not-found-by-mandate-service")
                val errorForm = mandateRefForm.withError(key = "mandateRef", message = errorMsg).fill(data)
                Future.successful(BadRequest(views.html.agent.searchPreviousMandate(service, errorForm, callingPage, getBackLink(service, callingPage))))
            }
          }
        )
  }

  def getOldMandateFromSession(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>
        dataCacheService.fetchAndGetFormData[OldMandateReference](oldNonUkMandate).map { mandateRef =>
          Ok(Json.toJson(mandateRef))
        }
  }

  private def getBackLink(service: String, callingPage: String) = {
    Some(routes.HasClientRegisteredBeforeController.view(callingPage).url)
  }
}
