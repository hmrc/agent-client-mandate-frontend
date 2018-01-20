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

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.ClientRegime
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.models.ContactDetails

import scala.concurrent.Future

object ReviewMandateController extends ReviewMandateController {
  val authConnector: AuthConnector = FrontendAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
}

trait ReviewMandateController extends FrontendController with Actions with MandateConstants {

  def dataCacheService: DataCacheService

  def view(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence).async {
    implicit authContext =>
      implicit request =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
          case Some(cache) =>
            cache.mandate match {
              case Some(x) =>
                val clientContactDetailsUpdated = x.clientParty.map(_.contactDetails).map(_.copy(email = cache.email.map(_.email).getOrElse("")))
                // $COVERAGE-OFF$
                val updatedClientParty = x.clientParty.map(_.copy(contactDetails = clientContactDetailsUpdated.getOrElse(ContactDetails(cache.email.map(_.email).getOrElse(throw new RuntimeException("email not cached"))))))
                // $COVERAGE-ON$
                val updatedMandate = x.copy(clientParty = updatedClientParty)
                dataCacheService.cacheFormData[ClientCache](clientFormId, cache.copy(mandate = Some(updatedMandate))) flatMap { cachedData =>
                  Future.successful(Ok(views.html.client.reviewMandate(service, updatedMandate, getBackLink(service))))
                }
              case None => Future.successful(Redirect(routes.SearchMandateController.view()))
            }
          case None => Future.successful(Redirect(routes.CollectEmailController.view()))
        }
  }

  def submit(service: String) = AuthorisedFor(ClientRegime(Some(service)), GGConfidence) {
    implicit authContext =>
      implicit request =>
        Redirect(routes.MandateDeclarationController.view())
  }

  private def getBackLink(service: String) = {
    Some(routes.SearchMandateController.view().url)
  }
}
