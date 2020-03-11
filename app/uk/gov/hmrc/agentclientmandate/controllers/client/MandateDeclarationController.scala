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
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientCache
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MandateDeclarationController @Inject()(
                                             val dataCacheService: DataCacheService,
                                             val mandateService: AgentClientMandateService,
                                             val authConnector: AuthConnector,
                                             mcc: MessagesControllerComponents,
                                             implicit val ec: ExecutionContext,
                                             implicit val appConfig: AppConfig
                                            ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) map {
          _.flatMap(_.mandate) match {
            case Some(x) => Ok(views.html.client.mandateDeclaration(x, getBackLink(service)))
            case None => Redirect(routes.ReviewMandateController.view())
          }
        }
      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withOrgCredId(Some(service)) { clientAuthRetrievals =>
        dataCacheService.fetchAndGetFormData[ClientCache](clientFormId) flatMap {
          _.flatMap(_.mandate) match {
            case Some(mandate) =>
              mandateService.approveMandate(mandate, clientAuthRetrievals) flatMap {
                case Some(_) => Future.successful(Redirect(routes.MandateConfirmationController.view()))
                case None    => Future.successful(Redirect(routes.ReviewMandateController.view()))
              }
            case None => Future.successful(Redirect(routes.ReviewMandateController.view()))
          }
        }
      }
  }

  private def getBackLink(service: String): Option[String] = {
    Some(routes.ReviewMandateController.view().url)
  }
}
