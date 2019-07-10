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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentclientmandate.config.ConcreteAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.ContactDetails
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object ReviewMandateController extends ReviewMandateController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait ReviewMandateController extends FrontendController with AuthorisedWrappers with MandateConstants {

  def dataCacheService: DataCacheService

  def view(service: String): Action[AnyContent] = Action.async { implicit request =>
    withOrgCredId(Some(service)) { _ =>
      dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
        case Some(cache) =>
          cache.mandate match {
            case Some(x) =>
              val clientContactDetailsUpdated = x.clientParty.map(_.contactDetails).map(_.copy(email = cache.email.map(_.email).getOrElse("")))

              // $COVERAGE-OFF$
              val updatedClientParty = x.clientParty.map(_.copy(contactDetails = clientContactDetailsUpdated.getOrElse(ContactDetails(cache.email.map(_.email).getOrElse(throw new RuntimeException("email not cached"))))))
              // $COVERAGE-ON$

              val updatedMandate = x.copy(clientParty = updatedClientParty)
              dataCacheService.cacheFormData[ClientCache](clientFormId, cache.copy(mandate = Some(updatedMandate))) flatMap { _ =>
                Future.successful(Ok(views.html.client.reviewMandate(service, updatedMandate, getBackLink(service))))
              }
            case None => Future.successful(Redirect(routes.SearchMandateController.view()))
          }
        case None => Future.successful(Redirect(routes.CollectEmailController.view()))
      }
    }
  }

  def submit(service: String): Action[AnyContent] = Action.async { implicit request =>
    withOrgCredId(Some(service)) { _ =>
      Future.successful(Redirect(routes.MandateDeclarationController.view()))
    }
  }

  private def getBackLink(service: String) = {
    Some(routes.SearchMandateController.view().url)
  }
}
