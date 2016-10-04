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

package uk.gov.hmrc.agentclientmandate.controllers.client

import uk.gov.hmrc.agentclientmandate.config.FrontendAuthConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.{AgentRegime, ClientRegime}
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.YesNoQuestionForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

object RemoveAgentController extends RemoveAgentController {
  // $COVERAGE-OFF$
  val authConnector = FrontendAuthConnector
  val acmService = AgentClientMandateService
  val dataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait RemoveAgentController extends FrontendController with Actions {

  def acmService: AgentClientMandateService
  def dataCacheService: DataCacheService

  def view(mandateId: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      request.getQueryString("returnUrl") match {
        case Some(returnUrl) => {
          dataCacheService.cacheFormData[String]("RETURN_URL", returnUrl).flatMap {
            case cache => {
              acmService.fetchClientMandate(mandateId).map {
                  case Some(mandate) => Ok(views.html.client.removeAgent(yesNoQuestionForm, mandate.agentParty.name, mandateId))
                  case _ => throw new RuntimeException("No Mandate returned")
                }
            }
          }
        }
        case _ => throw new RuntimeException("No returnUrl specified")
      }
  }

  def confirm(mandateId: String, agentName: String) = AuthorisedFor(ClientRegime, GGConfidence).async {
    implicit authContext => implicit request =>
      yesNoQuestionForm.bindFromRequest.fold(
        formWithError => Future.successful(BadRequest(views.html.client.removeAgent(formWithError, agentName, mandateId))),
        data => {
          val removeAgent = data.yesNo.getOrElse(false)
          if (removeAgent) {
            acmService.removeAgent(mandateId).map { removedAgent =>
              if (removedAgent) Redirect(routes.ChangeAgentController.view)
              else throw new RuntimeException("Agent Removal Failed")
            }
          }
          else {
            dataCacheService.fetchAndGetFormData[String]("RETURN_URL").map { returnUrl =>
              returnUrl match {
                case Some(x) => Redirect(x)
                case _ => throw new RuntimeException("Cache Retrieval Failed")
              }
            }
          }
        }
      )
  }

//  def showConfirmation(agentName: String) = AuthorisedFor(ClientRegime, GGConfidence) {
//    implicit authContext => implicit request =>
//      ??? //Ok(views.html.agent.removeAgentConfirmation(agentName))
//  }
}