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

import uk.gov.hmrc.agentclientmandate.config.FrontendAppConfig._
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasClientQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.OverseasClientQuestion
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants

object OverseasClientQuestionController extends OverseasClientQuestionController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val controllerId: String = "overseas"
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait OverseasClientQuestionController extends FrontendController with Actions with MandateConstants{
  def dataCacheService: DataCacheService
  val controllerId: String

  def view(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      dataCacheService.fetchAndGetFormData[OverseasClientQuestion](overseasTaxRefFormId) map {
        case Some(data) => Ok(views.html.agent.overseasClientQuestion(overseasClientQuestionForm.fill(data), service, getBackLink(service)))
        case _ => Ok(views.html.agent.overseasClientQuestion(overseasClientQuestionForm, service, getBackLink(service)))
      }
  }

  def submit(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence) {
    implicit authContext => implicit request =>
      overseasClientQuestionForm.bindFromRequest.fold(
        formWithError => BadRequest(views.html.agent.overseasClientQuestion(formWithError, service, getBackLink(service))),
        data => {
          dataCacheService.cacheFormData[OverseasClientQuestion](overseasTaxRefFormId, data)
          val isOverSeas = data.isOverseas.getOrElse(false)
          if (isOverSeas) Redirect(routes.NRLQuestionController.view())
          else
            Redirect(routes.MandateDetailsController.view(controllerId))
        }
      )
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.ClientDisplayNameController.view().url)
  }
}
