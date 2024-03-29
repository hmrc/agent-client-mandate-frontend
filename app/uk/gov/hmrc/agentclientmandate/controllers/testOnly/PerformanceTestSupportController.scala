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

package uk.gov.hmrc.agentclientmandate.controllers.testOnly

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentclientmandate.connectors.AgentClientMandateConnector
import uk.gov.hmrc.agentclientmandate.models.Mandate
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class PerformanceTestSupportController @Inject()(
                                                  mcc: MessagesControllerComponents,
                                                  agentClientMandateConnector: AgentClientMandateConnector,
                                                  implicit val ec: ExecutionContext
                                                ) extends FrontendController(mcc) with Logging {


  def createMandate(): Action[AnyContent] = Action.async { implicit request =>
    logger.debug("inserting test mandate")
     agentClientMandateConnector.testOnlyCreateMandate(request.body.asJson.get.as[Mandate]).map { x =>
       logger.debug("inserted test mandate")
       Ok
     }
  }

  def deleteMandate(mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    logger.debug(s"deleting mandate: $mandateId")
    agentClientMandateConnector.testOnlyDeleteMandate(mandateId).map { x =>
      logger.debug("deleted mandate")
      Ok
    }
  }

}

