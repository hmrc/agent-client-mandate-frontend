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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.utils.{ControllerPageIdConstants, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasClientQuestion
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasClientQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasClientQuestionController @Inject()(
                                                  dataCacheService: DataCacheService,
                                                  mcc: MessagesControllerComponents,
                                                  val authConnector: AuthConnector,
                                                  implicit val ec: ExecutionContext,
                                                  implicit val appConfig: AppConfig,
                                                  templateClientQuestion: views.html.agent.overseasClientQuestion
                                                ) extends FrontendController(mcc) with AuthorisedWrappers with MandateConstants {

  val controllerId: String = ControllerPageIdConstants.overseasClientQuestionControllerId

  def view(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        dataCacheService.fetchAndGetFormData[OverseasClientQuestion](overseasTaxRefFormId) map {
          case Some(data) => Ok(templateClientQuestion(overseasClientQuestionForm.fill(data), service, getBackLink(service)))
          case _ => Ok(templateClientQuestion(overseasClientQuestionForm, service, getBackLink(service)))
        }
      }
  }

  def submit(service: String): Action[AnyContent] = Action.async {
    implicit request =>
      withAgentRefNumber(Some(service)) { _ =>
        overseasClientQuestionForm.bindFromRequest().fold(
          formWithError => {
            val result = BadRequest(templateClientQuestion(formWithError, service, getBackLink(service)))
            Future.successful(result)
          },
          data => {
            dataCacheService.cacheFormData[OverseasClientQuestion](overseasTaxRefFormId, data)
            val isOverSeas = data.isOverseas.getOrElse(false)
            val result = if (isOverSeas) {
              Redirect(routes.NRLQuestionController.view())
            } else {
              Redirect(routes.MandateDetailsController.view(controllerId))
            }
            Future.successful(result)
          }
        )
      }
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientDisplayNameController.view().url)
  }
}
