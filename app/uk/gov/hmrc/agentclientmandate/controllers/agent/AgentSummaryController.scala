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
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.agentclientmandate.config.{FrontendAuthConnector, FrontendDelegationConnector}
import uk.gov.hmrc.agentclientmandate.controllers.auth.AgentRegime
import uk.gov.hmrc.agentclientmandate.models.AgentDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.utils.AuthUtils
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClients
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClientsForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.play.frontend.auth.connectors.DelegationConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, Delegator}
import uk.gov.hmrc.play.frontend.controller.FrontendController

object AgentSummaryController extends AgentSummaryController {
  val authConnector = FrontendAuthConnector
  val agentClientMandateService = AgentClientMandateService
  val delegationConnector: DelegationConnector = FrontendDelegationConnector
  val dataCacheService: DataCacheService = DataCacheService
}

trait AgentSummaryController extends FrontendController with Actions with Delegator {

  def agentClientMandateService: AgentClientMandateService
  def dataCacheService: DataCacheService

  val screenReaderTextId = "screenReaderTextId"

  def view(service: String, tabName: Option[String] = None) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      for {
        screenReaderText <- dataCacheService.fetchAndGetFormData[String](screenReaderTextId)
        mandates <- agentClientMandateService.fetchAllClientMandates(AuthUtils.getArn, service)
        agentDetails <- agentClientMandateService.fetchAgentDetails()
        clientsCancelled <- agentClientMandateService.fetchClientsCancelled(AuthUtils.getArn, service)
        _ <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
      } yield {
        showView(service, mandates, agentDetails, clientsCancelled, screenReaderText.getOrElse(""), tabName)
      }
  }

  def activate(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      agentClientMandateService.acceptClient(mandateId).flatMap { clientAccepted =>
        if (clientAccepted) {
          agentClientMandateService.fetchClientMandate(mandateId).flatMap {
            case Some(x) =>
              val arn = AuthUtils.getArn
              for {
                mandates <- agentClientMandateService.fetchAllClientMandates(arn, service)
                agentDetails <- agentClientMandateService.fetchAgentDetails()
                _ <- dataCacheService.cacheFormData[String](screenReaderTextId, Messages("client.summary.hidden.client_activated", x.clientDisplayName))
              } yield {
                Redirect(routes.AgentSummaryController.view(Some(service)))
              }
            case _ => throw new RuntimeException("Failed to fetch client")
          }
        }
        else throw new RuntimeException("Failed to accept client")
      }
  }


  def doDelegation(service: String, mandateId: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async {
    implicit authContext => implicit request =>
      agentClientMandateService.fetchClientMandate(mandateId).flatMap{
        mandate =>
          mandate.flatMap(_.subscription.referenceNumber) match {
            case Some(serviceId) =>
              val clientName = mandate.map(_.clientDisplayName).getOrElse("")
              startDelegationAndRedirect(createDelegationContext(service, serviceId, clientName), getDelegatedServiceRedirectUrl(service))
            case None =>
              throw new RuntimeException(s"[AgentSummaryController][doDelegation] Failed to doDelegation to for mandateId $mandateId for service $service")
          }
      }
  }

  private def showView(service: String,
                       mandates: Option[Mandates],
                       agentDetails: AgentDetails,
                       clientsCancelled: Option[Seq[String]],
                       screenReaderText: String,
                       tabName: Option[String] = None)(implicit request: Request[AnyContent]) = {

    mandates match {
      case Some(x) if (x.pendingMandates.size > 0 && tabName.equals(Some("pending-clients"))) =>
        Ok(views.html.agent.agentSummary.pending(service, x, agentDetails, clientsCancelled, screenReaderText))
      case Some(x) if (x.activeMandates.size > 0) =>
        Ok(views.html.agent.agentSummary.clients(service, x, agentDetails, clientsCancelled, screenReaderText,filterClientsForm.fill(FilterClients(None, "allClients"))))
      case Some(x) if (x.pendingMandates.size > 0) =>
        Ok(views.html.agent.agentSummary.pending(service, x, agentDetails, clientsCancelled, screenReaderText))
      case _ =>
        Ok(views.html.agent.agentSummary.noClientsNoPending(service, agentDetails, clientsCancelled))
    }
  }

  def update(service: String) = AuthorisedFor(AgentRegime(Some(service)), GGConfidence).async{
     implicit authContext => implicit request =>

     filterClientsForm.bindFromRequest.fold(
       formWithError => {
         for {
           screenReaderText <- dataCacheService.fetchAndGetFormData[String](screenReaderTextId)
           mandates <- agentClientMandateService.fetchAllClientMandates(AuthUtils.getArn, service)
           agentDetails <- agentClientMandateService.fetchAgentDetails()
           clientsCancelled <- agentClientMandateService.fetchClientsCancelled(AuthUtils.getArn, service)
           _ <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
         } yield {
          BadRequest(views.html.agent.agentSummary.noClientsNoPending(service, agentDetails, clientsCancelled))
         }
      },
      data => {
        for {
          screenReaderText <- dataCacheService.fetchAndGetFormData[String](screenReaderTextId)
          mandates <- agentClientMandateService.fetchAllClientMandates(AuthUtils.getArn, service, data.showAllClients == "allClients", data.displayName, true)
          agentDetails <- agentClientMandateService.fetchAgentDetails()
          clientsCancelled <- agentClientMandateService.fetchClientsCancelled(AuthUtils.getArn, service)
          _ <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
        } yield {
          Ok(views.html.agent.agentSummary.clients(service, mandates.getOrElse(Mandates(Seq(), Seq())), agentDetails, clientsCancelled, "", filterClientsForm.fill(data), true))
        }
      }
    )
  }
}
