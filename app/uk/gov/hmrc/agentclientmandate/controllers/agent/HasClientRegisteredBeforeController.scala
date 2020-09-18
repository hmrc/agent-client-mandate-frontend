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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegistered
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegisteredForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HasClientRegisteredBeforeController @Inject()(
                                                     mcc: MessagesControllerComponents,
                                                     dataCacheService: DataCacheService,
                                                     businessCustomerConnector: BusinessCustomerFrontendConnector,
                                                     atedSubscriptionConnector: AtedSubscriptionFrontendConnector,
                                                     implicit val ec: ExecutionContext,
                                                     implicit val appConfig: AppConfig,
                                                     val authConnector: AuthConnector,
                                                     templateHasClientRegisteredBefore: views.html.agent.hasClientRegisteredBefore
                                                   ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  def view(service: String, callingPage: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { _ =>
      for {
        prevRegistered <- dataCacheService.fetchAndGetFormData[PrevRegistered](prevRegisteredFormId)
        _ <- businessCustomerConnector.clearCache(service)
        _ <- {
          if (service.toUpperCase == "ATED") atedSubscriptionConnector.clearCache(service)
          else Future.successful(HttpResponse(OK, ""))
        }
      } yield Ok(templateHasClientRegisteredBefore(prevRegisteredForm.fill(prevRegistered.getOrElse(PrevRegistered())),
        callingPage, service, getBackLink(service, callingPage)))
    }
  }


  def submit(service: String, callingPage: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        prevRegisteredForm.bindFromRequest.fold(
          formWithErrors => {
            val result = BadRequest(templateHasClientRegisteredBefore(
              formWithErrors, callingPage, service, getBackLink(service, callingPage))
            )
            Future.successful(result)
          },
          data => {
            dataCacheService.cacheFormData[PrevRegistered](prevRegisteredFormId, data)
            val result = if (data.prevRegistered.getOrElse(false)) {
              Redirect(routes.PreviousUniqueAuthorisationNumberController.view(callingPage))
            } else {
              Redirect(appConfig.nonUkUri(service, routes.HasClientRegisteredBeforeController.view(callingPage).url))
            }

            Future.successful(result)
          }
        )
      }
  }

  private def getBackLink(service: String, callingPage: String) = {
    Some(routes.ClientPermissionController.view(callingPage).url)
  }
}
