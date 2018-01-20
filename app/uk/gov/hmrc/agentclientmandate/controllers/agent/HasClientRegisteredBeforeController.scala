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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import play.api.Logger
import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.{AtedSubscriptionFrontendConnector, BusinessCustomerFrontendConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegisteredForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.controllers.agent.routes
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.PrevRegistered

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpResponse

object HasClientRegisteredBeforeController extends HasClientRegisteredBeforeController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val businessCustomerConnector: BusinessCustomerFrontendConnector = BusinessCustomerFrontendConnector
  val atedSubscriptionConnector: AtedSubscriptionFrontendConnector = AtedSubscriptionFrontendConnector
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait HasClientRegisteredBeforeController extends FrontendController with Actions with MandateConstants {

  def businessCustomerConnector: BusinessCustomerFrontendConnector
  def atedSubscriptionConnector: AtedSubscriptionFrontendConnector
  def dataCacheService: DataCacheService

  def view(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      for {
        prevRegistered <- dataCacheService.fetchAndGetFormData[PrevRegistered](prevRegisteredFormId)
        clearBcResp <- businessCustomerConnector.clearCache(service)
        serviceResp <- {
          if (service.toUpperCase == "ATED") atedSubscriptionConnector.clearCache(service)
          else Future.successful(HttpResponse(OK))
        }
      } yield Ok(views.html.agent.hasClientRegisteredBefore(prevRegisteredForm.fill(prevRegistered.getOrElse(PrevRegistered())), callingPage, service, getBackLink(service, callingPage)))
  }


  def submit(service: String, callingPage: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence) {
    implicit user => implicit request =>
      prevRegisteredForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.agent.hasClientRegisteredBefore(formWithErrors, callingPage, service, getBackLink(service, callingPage))),
        data => {
          dataCacheService.cacheFormData[PrevRegistered](prevRegisteredFormId, data)
          if (data.prevRegistered.getOrElse(false)) {
            Redirect(routes.PreviousMandateRefController.view( callingPage))
          } else
            Redirect(nonUkUri(service, routes.HasClientRegisteredBeforeController.view( callingPage).url))
        }
      )
  }

  private def getBackLink(service: String, callingPage: String) = {
    Some(routes.ClientPermissionController.view(callingPage).url)
  }
}
