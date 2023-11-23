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

import play.api.i18n.Messages

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{DelegationUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayName
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.ClientDisplayNameForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
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

  def view(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        for {
          clientDisplayname <- dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId)
        } yield {
          redirectUrl match {
            case Some(providedUrl) =>
              DelegationUtils.getSafeLink(providedUrl, appConfig.environment) match {
                case Some(safeLink) =>
                  processViewRequest(service, clientDisplayname, redirectUrl, Some(safeLink))
                case None => BadRequest("The return url is not correctly formatted")
              }
            case _ => processViewRequest(service, clientDisplayname)
          }
        }
      }
  }

  private def processViewRequest(service: String, clientDisplayname: Option[ClientDisplayName],
                                 redirectUrl: Option[RedirectUrl] = None, safeLink: Option[String] = None)
                                (implicit request: Request[_], messages: Messages) = {
    clientDisplayname match {
      case Some(clientName) => Ok(templateClientDisplayName(
        clientDisplayNameForm.fill(clientName), service, redirectUrl, getBackLink(safeLink)))
      case None => Ok(templateClientDisplayName(
        clientDisplayNameForm, service, redirectUrl, getBackLink(safeLink)))
    }
  }


  def editFromSummary(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        for {
          clientDisplayName <- dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId)
          callingPage <- dataCacheService.fetchAndGetFormData[String](callingPageCacheId)
        } yield {
          redirectUrl match {
            case Some(providedUrl) =>
              DelegationUtils.getSafeLink(providedUrl, appConfig.environment) match {
                case Some(safeLink) =>
                  processEditSummaryRequest(service, clientDisplayName, callingPage, redirectUrl, Some(safeLink))
                case None => BadRequest("The return url is not correctly formatted")
              }
            case _ => processEditSummaryRequest(service, clientDisplayName, callingPage)
          }
        }
      }
  }

  private def processEditSummaryRequest(service: String, clientDisplayname: Option[ClientDisplayName], callingPage: Option[String],
                                 redirectUrl: Option[RedirectUrl] = None, safeLink: Option[String] = None)
                                (implicit request: Request[_], messages: Messages) = {
    clientDisplayname match {
      case Some(clientName) =>
        Ok(templateClientDisplayName(
          clientDisplayNameForm.fill(clientName), service, redirectUrl, getBackLink(
            Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.MandateDetailsController.view(callingPage.getOrElse("")).url))))
      case None => Ok(templateClientDisplayName(
        clientDisplayNameForm, service, redirectUrl, getBackLink(safeLink)))
    }
  }


  def submit(service: String, redirectUrl: Option[RedirectUrl]): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        redirectUrl match {
          case Some(providedUrl) =>
            DelegationUtils.getSafeLink(providedUrl, appConfig.environment) match {
              case Some(safeLink) =>
                processSubmitRequest(service, redirectUrl, Some(safeLink))
              case None => Future.successful(BadRequest("The return url is not correctly formatted"))
            }
          case None => processSubmitRequest(service)
        }
      }
  }

  private def processSubmitRequest(service: String, redirectUrl: Option[RedirectUrl] = None, safeLink: Option[String] = None)
                                  (implicit request: Request[_], messages: Messages) = {
    clientDisplayNameForm.bindFromRequest().fold(
      formWithError => {
        Future.successful(BadRequest(templateClientDisplayName(
          formWithError, service, redirectUrl, getBackLink(safeLink))))},
      data => {
        dataCacheService.cacheFormData[ClientDisplayName](clientDisplayNameFormId, data) flatMap { _ =>
          redirectUrl match {
            case Some(_) => Future.successful(Redirect(safeLink.getOrElse("")))
            case None => Future.successful(Redirect(routes.OverseasClientQuestionController.view()))
          }
        }
      })
  }


  def getClientDisplayName(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId).map { displayName =>
        Ok(Json.toJson(displayName))
      }
    }
  }

  private def getBackLink(redirectUrl: Option[String]): Option[String] = {
    redirectUrl match {
      case Some(x) => Some(x)
      case None => Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.CollectAgentEmailController.view().url)
    }
  }
}
