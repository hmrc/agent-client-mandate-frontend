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

import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import play.api.i18n.Messages
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.models.{ContactDetails, Party, PartyType}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{ClientCache, MandateReference}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.MandateReferenceForm.mandateRefForm
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


object SearchMandateController extends SearchMandateController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  val mandateService: AgentClientMandateService = AgentClientMandateService
}

trait SearchMandateController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def mandateService: AgentClientMandateService

  def view(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>

      dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map { a =>
        a.flatMap(_.mandate) match {
          case Some(x) => Ok(views.html.client.searchMandate(service, mandateRefForm.fill(MandateReference(x.id)), getBackLink(service)))
          case None => Ok(views.html.client.searchMandate(service, mandateRefForm, getBackLink(service)))
        }
      }
  }

  def submit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      mandateRefForm.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.client.searchMandate(service, formWithErrors, getBackLink(service)))),
        data => mandateService.fetchClientMandate(data.mandateRef.toUpperCase) flatMap {
          case Some(x) => {
            if (x.currentStatus.status != uk.gov.hmrc.agentclientmandate.models.Status.New) {
              val errorMsg = Messages("client.search-mandate.error.mandateRef.already-used-by-mandate-service")
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
                  ) flatMap { cachedData =>
                    Future.successful(Redirect(routes.ReviewMandateController.view()))
                  }
                case None => Future.successful(Redirect(routes.CollectEmailController.view()))
              }
            }
          }
          case None =>
            val errorMsg = Messages("client.search-mandate.error.mandateRef.not-found-by-mandate-service")
            val errorForm = mandateRefForm.withError(key = "mandateRef", message = errorMsg).fill(data)
            Future.successful(BadRequest(views.html.client.searchMandate(service, errorForm, getBackLink(service))))
        }
      )
  }

  private def getBackLink(service: String) = {
    Some(routes.CollectEmailController.back().url)
  }
}
