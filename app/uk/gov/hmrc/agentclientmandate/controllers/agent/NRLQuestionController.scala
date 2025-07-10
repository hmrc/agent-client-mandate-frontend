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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{ControllerPageIdConstants, MandateConstants, MandateFeatureSwitches}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestion
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NRLQuestionController @Inject()(
                                       dataCacheService: DataCacheService,
                                       mcc: MessagesControllerComponents,
                                       val authConnector: AuthConnector,
                                       implicit val ec: ExecutionContext,
                                       implicit val appConfig: AppConfig,
                                       implicit val servicesConfig: ServicesConfig,
                                       templateNrlQuestion: views.html.agent.nrl_question
                                     ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  val controllerId: String = ControllerPageIdConstants.nrlQuestionControllerId

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[NRLQuestion](nrlFormId) map {
          case Some(data) => Ok(templateNrlQuestion(nrlQuestionForm.fill(data), service, getBackLink(service)))
          case _ => Ok(templateNrlQuestion(nrlQuestionForm, service, getBackLink(service)))
        }
      }
  }


  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        nrlQuestionForm.bindFromRequest().fold(
          formWithErrors => {
            val result = BadRequest(templateNrlQuestion(formWithErrors, service, getBackLink(service)))
            Future.successful(result)
          },
          data => {
            dataCacheService.cacheFormData[NRLQuestion](nrlFormId, data)
            val result = if (data.nrl.getOrElse(false)) {
              Redirect(routes.PaySAQuestionController.view())
            } else {
              if (MandateFeatureSwitches.registeringClientContentUpdate.enabled) {
                Redirect(routes.BeforeRegisteringClientController.view(controllerId))
              } else {
                Redirect(routes.ClientPermissionController.view(controllerId))
              }
            }
            Future.successful(result)
          }
        )
      }
  }

  private def getBackLink(service: String): Some[String] = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.OverseasClientQuestionController.view().url)
  }
}
