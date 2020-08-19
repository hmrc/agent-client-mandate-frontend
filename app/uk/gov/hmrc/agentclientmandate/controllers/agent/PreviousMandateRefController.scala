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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.OldMandateReference
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.MandateReferenceForm.clientAuthNumForm
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, ClientEmail, MandateReference}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreviousMandateRefController @Inject()(
                                            val mcc: MessagesControllerComponents,
                                            val authConnector: AuthConnector,
                                            dataCacheService: DataCacheService,
                                            mandateService: AgentClientMandateService,
                                            implicit val ec: ExecutionContext,
                                            implicit val appConfig: AppConfig
                                            ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map { a =>
          a.flatMap(_.mandate) match {
            case Some(x) => Ok(views.html.agent.searchPreviousMandate(service, clientAuthNumForm.fill(MandateReference(x.id)),
              callingPage, getBackLink(service, callingPage)))
            case None => Ok(views.html.agent.searchPreviousMandate(service, clientAuthNumForm, callingPage, getBackLink(service, callingPage)))
          }
        }
      }
  }

  def submit(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { authRetrievals =>
        clientAuthNumForm.bindFromRequest.fold(
          formWithErrors => {
            val result = BadRequest(views.html.agent.searchPreviousMandate(service, formWithErrors, callingPage, getBackLink(service, callingPage)))
            Future.successful(result)
          },
          data => {
            mandateService.fetchClientMandate(data.mandateRef.toUpperCase, authRetrievals) flatMap {
              case Some(x) =>
                dataCacheService.cacheFormData[OldMandateReference](oldNonUkMandate, OldMandateReference(x.id,
                  x.subscription.referenceNumber.getOrElse(throw new RuntimeException("No Client Ref no. found!"))))
                dataCacheService.cacheFormData[ClientCache](clientFormId, ClientCache(Some(ClientEmail(x.clientParty
                  .map(_.contactDetails.email).getOrElse(""))), Some(x))) flatMap { cacheResp =>
                    Future.successful(Redirect(appConfig.addNonUkClientCorrespondenceUri(routes.PreviousMandateRefController.view(callingPage).url)))
                }
              case None =>
                val errorMsg = "client.search-mandate.error.clientAuthNum"
                val errorForm = clientAuthNumForm.withError(key = "mandateRef", message = errorMsg).fill(data)
                Future.successful(BadRequest(views.html.agent.searchPreviousMandate(service, errorForm, callingPage, getBackLink(service, callingPage))))
            }
          }
        )
      }
  }

  def getOldMandateFromSession(service: String): Action[AnyContent] = Action.async {
      implicit request =>
        dataCacheService.fetchAndGetFormData[OldMandateReference](oldNonUkMandate).map { mandateRef =>
          Ok(Json.toJson(mandateRef))
        }
  }

  private def getBackLink(service: String, callingPage: String): Some[String] = {
    Some(routes.PreviousUniqueAuthorisationNumberController.view(callingPage).url)
  }
}
