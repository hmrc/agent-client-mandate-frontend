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
import uk.gov.hmrc.agentclientmandate.models.{ContactDetails, Party, PartyType}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.MandateReferenceForm.mandateRefForm
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, MandateReference}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchMandateController @Inject()(
                                         mcc: MessagesControllerComponents,
                                         val authConnector: AuthConnector,
                                         dataCacheService: DataCacheService,
                                         mandateService: AgentClientMandateService,
                                         implicit val ec: ExecutionContext,
                                         implicit val appConfig: AppConfig
                                       ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map { a =>
          a.flatMap(_.mandate) match {
            case Some(x) => Ok(views.html.client.searchMandate(service, mandateRefForm.fill(MandateReference(x.id)), getBackLink(service)))
            case None => Ok(views.html.client.searchMandate(service, mandateRefForm, getBackLink(service)))
          }
        }
      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { authRetrievals =>
        mandateRefForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.client.searchMandate(service, formWithErrors, getBackLink(service)))),
          data => mandateService.fetchClientMandate(data.mandateRef.toUpperCase, authRetrievals) flatMap {
            case Some(x) =>
              if (x.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.New) {
                val errorMsg = "client.search-mandate.error.mandateRef.already-used-by-mandate-service"
                val errorForm = mandateRefForm.withError(key = "mandateRef", message = errorMsg).fill(data)
                Future.successful(BadRequest(views.html.client.searchMandate(service, errorForm, getBackLink(service))))
              } else {
                dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
                  case Some(y) =>
                    //id and name as well as type will be updated/populated by mandate backend
                    //same applies to mandate current status as well as history
                    val clientParty = Party(
                      id = "",
                      name = "",
                      `type` = PartyType.Organisation,
                      contactDetails = ContactDetails(y.email.map(_.email).getOrElse(throw new RuntimeException("email not cached")))
                    )
                    val updatedMandate = x.copy(clientParty = Some(clientParty))
                    dataCacheService.cacheFormData[ClientCache](
                      clientFormId,
                      y.copy(mandate = Some(updatedMandate))
                    ) flatMap { _ =>
                      Future.successful(Redirect(routes.ReviewMandateController.view()))
                    }
                  case None => Future.successful(Redirect(routes.CollectEmailController.view()))
                }
              }
            case None =>
              val errorMsg = "client.search-mandate.error.mandateRef.not-found-by-mandate-service"
              val errorForm = mandateRefForm.withError(key = "mandateRef", message = errorMsg).fill(data)
              Future.successful(BadRequest(views.html.client.searchMandate(service, errorForm, getBackLink(service))))
          }
        )
      }
  }

  private def getBackLink(service: String): Some[String] = {
    Some(routes.CollectEmailController.back().url)
  }
}
