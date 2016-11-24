/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.controllers.testOnly

import play.api.Logger
import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.agentclientmandate.views

object GGBreakingRelationshipController extends FrontendController with Actions {

  def agentClientMandateConnector: AgentClientMandateConnector = AgentClientMandateConnector
  override val authConnector = FrontendAuthConnector

  def view() = AuthorisedFor(AgentRegime, GGConfidence) {
    implicit authContext => implicit request =>
      Ok(views.html.testOnly.checkBreakingRelationships())
  }

  def submit() = AuthorisedFor(AgentRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      agentClientMandateConnector.remove(request.body.asFormUrlEncoded.get.apply("mandateId").head).map { x =>
        Logger.info("********" + x.body + "*************")
        Redirect(uk.gov.hmrc.agentclientmandate.controllers.agent.routes.AgentSummaryController.view("ATED"))
      }

  }

}
