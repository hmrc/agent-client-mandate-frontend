/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayName
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayNameForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDisplayNameController @Inject()(
                                             dataCacheService: DataCacheService,
                                             mcc: MessagesControllerComponents,
                                             val authConnector: AuthConnector,
                                             implicit val ec: ExecutionContext,
                                             implicit val appConfig: AppConfig,
                                             templateClientDisplayName:views.html.agent.clientDisplayName
                                           ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      redirectUrl match {
        case Some(x) if !AgentClientMandateUtils.isRelativeOrDev(x) => Future.successful(BadRequest("The return url is not correctly formatted"))
        case _ =>
          dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) map {
            case Some(clientDisplayname) => Ok(templateClientDisplayName(
              clientDisplayNameForm.fill(clientDisplayname), service, redirectUrl, getBackLink(service, redirectUrl)))
            case None => Ok(templateClientDisplayName(
              clientDisplayNameForm, service, redirectUrl, getBackLink(service, redirectUrl)))
          }
      }
    }
  }


  def editFromSummary(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      for {
        clientDisplayName <- dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId)
        callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
      } yield {
        redirectUrl match {
          case Some(x) if !AgentClientMandateUtils.isRelativeOrDev(x) => BadRequest("The return url is not correctly formatted")
          case _ =>
            clientDisplayName match {
              case Some(clientDisplayname) => Ok(templateClientDisplayName(
                clientDisplayNameForm.fill(clientDisplayname), service, redirectUrl, getBackLink(service,
                Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url))))
              case None => Ok(templateClientDisplayName(clientDisplayNameForm, service, redirectUrl, getBackLink(service, redirectUrl)))
            }
        }
      }
    }
  }


  def submit(service: String, redirectUrl: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      redirectUrl match {
        case Some(x) if !AgentClientMandateUtils.isRelativeOrDev(x) => Future.successful(BadRequest("The return url is not correctly formatted"))
        case _ =>
          clientDisplayNameForm.bindFromRequest.fold(
            formWithError => Future.successful(BadRequest(templateClientDisplayName(
              formWithError, service, redirectUrl, getBackLink(service, redirectUrl)))),
            data =>
              dataCacheService.cacheFormData[ClientDisplayName](clientDisplayNameFormId, data) map { _ =>
                redirectUrl match {
                  case Some(redirect) => Redirect(redirect)
                  case None => Redirect(routes.OverseasClientQuestionController.view())
                }
              }
          )
      }
    }
  }

  def getClientDisplayName(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId).map { displayName =>
        Ok(Json.toJson(displayName))
      }
    }
  }

  private def getBackLink(service: String, redirectUrl: Option[String]): Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.CollectAgentEmailController.view().url)
    }
  }
}
