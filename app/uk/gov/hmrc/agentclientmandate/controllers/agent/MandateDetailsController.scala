/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.utils.{ControllerPageIdConstants, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.{AgentEmail, ClientDisplayName}
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class MandateDetailsController @Inject()(
                                          mcc: MessagesControllerComponents,
                                          dataCacheService: DataCacheService,
                                          mandateService: AgentClientMandateService,
                                          implicit val ec: ExecutionContext,
                                          implicit val appConfig: AppConfig,
                                          val authConnector: AuthConnector,
                                          templateMandateDetails: views.html.agent.mandateDetails
                                        ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String, callingPage: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      for {
        agentEmail <- dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId)
        clientDisplayName <- dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId)
        _ <- dataCacheService.cacheFormData[String](callingPageCacheId, callingPage)
      } yield {
        agentEmail match {
          case Some(email) =>
            clientDisplayName match {
              case Some(x) => Ok(templateMandateDetails(email.email, service, x.name, getBackLink(service, callingPage)))
              case _ => Redirect(routes.ClientDisplayNameController.view())
            }
          case _ => Redirect(routes.CollectAgentEmailController.addClient())
        }
      }
    }
  }

  def submit(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { authRetrievals =>
      mandateService.createMandate(service, authRetrievals) map (_ => Redirect(routes.UniqueAgentReferenceController.view()))
    }
  }

  private def getBackLink(service: String, callingPage: String): Some[String] = {
    val pageId: String = ControllerPageIdConstants.paySAQuestionControllerId

    callingPage match {
      case `pageId` => Some(routes.PaySAQuestionController.view().url)
      case _        => Some(routes.OverseasClientQuestionController.view().url)
    }
  }
}
