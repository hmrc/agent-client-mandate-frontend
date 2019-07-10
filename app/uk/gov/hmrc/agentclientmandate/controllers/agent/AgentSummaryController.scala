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

import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, AnyContent, Request, Results}
import uk.gov.hmrc.agentclientmandate.config.ConcreteAuthConnector
import uk.gov.hmrc.agentclientmandate.connectors.DelegationConnector
import uk.gov.hmrc.agentclientmandate.controllers.auth.AuthorisedWrappers
import uk.gov.hmrc.agentclientmandate.models.AgentDetails
import uk.gov.hmrc.agentclientmandate.service.{AgentClientMandateService, DataCacheService, Mandates}
import uk.gov.hmrc.agentclientmandate.utils.DelegationUtils._
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClients
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms.FilterClientsForm._
import uk.gov.hmrc.agentclientmandate.views
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

object AgentSummaryController extends AgentSummaryController {
  // $COVERAGE-OFF$
  val authConnector: AuthConnector = ConcreteAuthConnector
  val agentClientMandateService: AgentClientMandateService = AgentClientMandateService
  val delegationConnector: DelegationConnector = DelegationConnector
  val dataCacheService: DataCacheService = DataCacheService
  // $COVERAGE-ON$
}

trait AgentSummaryController extends FrontendController with AuthorisedWrappers {

  def agentClientMandateService: AgentClientMandateService
  def dataCacheService: DataCacheService
  val delegationConnector: DelegationConnector

  val screenReaderTextId = "screenReaderTextId"

  def view(service: String, tabName: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) {agentAuthRetrievals =>
      for {
        screenReaderText <- dataCacheService.fetchAndGetFormData[String](screenReaderTextId)
        mandates <- agentClientMandateService.fetchAllClientMandates(agentAuthRetrievals, service)
        agentDetails <- agentClientMandateService.fetchAgentDetails(agentAuthRetrievals)
        clientsCancelled <- agentClientMandateService.fetchClientsCancelled(agentAuthRetrievals, service)
        _ <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
      } yield {
        showView(service, mandates, agentDetails, clientsCancelled, screenReaderText.getOrElse(""), tabName)
      }
    }
  }

  def activate(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { agentAuthRetrievals =>
      agentClientMandateService.acceptClient(mandateId, agentAuthRetrievals).flatMap { clientAccepted =>
        if (clientAccepted) {
          agentClientMandateService.fetchClientMandate(mandateId, agentAuthRetrievals).flatMap {
            case Some(x) =>
              dataCacheService.cacheFormData[String](screenReaderTextId, Messages("client.summary.hidden.client_activated", x.clientDisplayName)) map {_ =>
                Redirect(routes.AgentSummaryController.view(Some(service)))
              }
            case _ => throw new RuntimeException("Failed to fetch client")
          }
        }
        else throw new RuntimeException("Failed to accept client")
      }
    }
  }


  def doDelegation(service: String, mandateId: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { agentAuthRetrievals =>
      agentClientMandateService.fetchClientMandate(mandateId, agentAuthRetrievals).flatMap { mandate =>
        mandate.flatMap(_.subscription.referenceNumber) match {
          case Some(serviceId) =>
            val clientName = mandate.map(_.clientDisplayName).getOrElse("")
            val delegationContext = createDelegationContext(
              service,
              serviceId,
              clientName,
              agentAuthRetrievals.agentFriendlyName,
              agentAuthRetrievals.internalId
            )
            val redirectUrl = getDelegatedServiceRedirectUrl(service)

            delegationConnector.startDelegation(agentAuthRetrievals.internalId, delegationContext).map { _ =>
              Results.SeeOther(redirectUrl)
            }
          case None =>
            throw new RuntimeException(s"[AgentSummaryController][doDelegation] Failed to doDelegation to for mandateId $mandateId for service $service")
        }
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
      case Some(x) if x.pendingMandates.nonEmpty && tabName.contains("pending-clients") =>
        Ok(views.html.agent.agentSummary.pending(service, x, agentDetails, clientsCancelled, screenReaderText))
      case Some(x) if x.activeMandates.nonEmpty =>
        Ok(views.html.agent.agentSummary.clients(service, x, agentDetails, clientsCancelled, screenReaderText, filterClientsForm.fill(FilterClients(None, "allClients"))))
      case Some(x) if x.pendingMandates.nonEmpty =>
        Ok(views.html.agent.agentSummary.pending(service, x, agentDetails, clientsCancelled, screenReaderText))
      case _ =>
        Ok(views.html.agent.agentSummary.noClientsNoPending(service, agentDetails, clientsCancelled))
    }
  }

  def update(service: String): Action[AnyContent] = Action.async { implicit request =>
    withAgentRefNumber(Some(service)) { agentAuthRetrievals =>
      filterClientsForm.bindFromRequest.fold(
        _ => {
          for {
            agentDetails      <- agentClientMandateService.fetchAgentDetails(agentAuthRetrievals)
            clientsCancelled  <- agentClientMandateService.fetchClientsCancelled(agentAuthRetrievals, service)
            _                 <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
          } yield {
            BadRequest(views.html.agent.agentSummary.noClientsNoPending(service, agentDetails, clientsCancelled))
          }
        },
        data => {
          for {
            mandates <- agentClientMandateService
              .fetchAllClientMandates(agentAuthRetrievals, service, data.showAllClients == "allClients", data.displayName)
            agentDetails <- agentClientMandateService.fetchAgentDetails(agentAuthRetrievals)
            clientsCancelled <- agentClientMandateService.fetchClientsCancelled(agentAuthRetrievals, service)
            _ <- dataCacheService.cacheFormData[String](screenReaderTextId, "")
          } yield {
            Ok(views.html.agent.agentSummary.clients(service, mandates.getOrElse(Mandates(Seq(), Seq())), agentDetails, clientsCancelled, "", filterClientsForm.fill(data), true))
          }
        }
      )
    }
  }
}
