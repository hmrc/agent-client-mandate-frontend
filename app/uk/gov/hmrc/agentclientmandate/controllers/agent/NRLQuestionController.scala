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

package uk.gov.hmrc.agentclientmandate.controllers.agent

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.agentclientmandate.service.DataCacheService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.agentclientmandate.utils.MandateConstants
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.NRLQuestion

object NRLQuestionController extends NRLQuestionController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = FrontendAuthConnector
  val controllerId: String = "nrl"
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait NRLQuestionController extends FrontendController with Actions with MandateConstants {
  def dataCacheService: DataCacheService

  val controllerId: String

  def view(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit user => implicit request =>
      dataCacheService.fetchAndGetFormData[NRLQuestion](nrlFormId) map {
        case Some(data) => Ok(views.html.agent.nrl_question(nrlQuestionForm.fill(data), service, getBackLink(service)))
        case _=> Ok(views.html.agent.nrl_question(nrlQuestionForm, service, getBackLink(service)))
      }
  }


  def submit(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence) {
    implicit user => implicit request =>
      nrlQuestionForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.agent.nrl_question(formWithErrors, service, getBackLink(service))),
        data => {
          dataCacheService.cacheFormData[NRLQuestion](nrlFormId, data)
          if (data.nrl.getOrElse(false))
            Redirect(routes.PaySAQuestionController.view())
          else
            Redirect(routes.ClientPermissionController.view( controllerId))
        }
      )
  }

  private def getBackLink(service: String) = {
    Some(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.OverseasClientQuestionController.view().url)
  }
}
