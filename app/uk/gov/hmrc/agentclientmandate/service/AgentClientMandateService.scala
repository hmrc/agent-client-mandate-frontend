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

package uk.gov.hmrc.agentclientmandate.service

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, BusinessCustomerConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class Mandates(activeMandates: Seq[Mandate], pendingMandates: Seq[Mandate])

@Singleton
class AgentClientMandateService @Inject()(val dataCacheService: DataCacheService,
                                          val agentClientMandateConnector: AgentClientMandateConnector,
                                          val businessCustomerConnector: BusinessCustomerConnector) extends MandateConstants with Logging {

  def acknowledgementReference: String = AgentClientMandateUtils.getUniqueAckNo

  def createMandate(service: String, authRetrievals: AgentAuthRetrievals)
                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId) flatMap {
      case Some(cachedEmail) =>
        dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) flatMap {
          case Some(displayName) =>
            val mandateDto = CreateMandateDto(cachedEmail.email, service, displayName.name)
            agentClientMandateConnector.createMandate(mandateDto, authRetrievals) flatMap {
              response => response.status match {
                case CREATED =>
                  val mandateId = (response.json \ "mandateId").as[String]
                  dataCacheService.clearCache() flatMap { _ =>
                    val clientDetails = ClientMandateDisplayDetails(displayName.name, mandateId, cachedEmail.email)
                    dataCacheService.cacheFormData[ClientMandateDisplayDetails](agentRefCacheId, clientDetails) flatMap { _ =>
                      Future.successful(mandateId)
                    }
                  }
                case _ => throw new RuntimeException(s"Mandate not created for $service")
              }
            }
          case None => throw new RuntimeException(s"Client Display Name not found in cache for $service")
        }
      case None => throw new RuntimeException(s"Email not found in cache for $service")
    }
  }

  def fetchClientMandateClientName(mandateId: String, authRetrievals: MandateAuthRetrievals)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Mandate] = {
    fetchClientMandate(mandateId, authRetrievals).map {
      case Some(mandate) => mandate
      case _ => throw new RuntimeException(s"[AgentClientMandateService][fetchClientMandateClientName] No Mandate returned for id $mandateId")
    }
  }

  def fetchClientMandateAgentName(mandateId: String, authRetrievals: MandateAuthRetrievals)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    fetchClientMandate(mandateId, authRetrievals).map {
      case Some(mandate) => mandate.agentParty.name
      case _ => throw new RuntimeException(s"[AgentClientMandateService][fetchClientMandateAgentName] No Mandate Agent Name returned with id $mandateId")
    }
  }


  def fetchClientMandate(mandateId: String, authRetrievals: MandateAuthRetrievals)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.fetchMandate(mandateId) map {
      response => response.status match {
        case OK => response.json.asOpt[Mandate]
        case _ => None
      }
    }
  }

  def fetchClientMandateByClient(clientId: String, serviceName: String)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.fetchMandateByClient(clientId, serviceName) map {
      response => response.status match {
        case OK => response.json.asOpt[Mandate]
        case _ => None
      }
    }
  }

  def approveMandate(mandate: Mandate, clientAuthRetrievals: ClientAuthRetrievals)
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.approveMandate(mandate, clientAuthRetrievals) flatMap { response =>
      response.status match {
        case OK =>
          val mandate = response.json.as[Mandate]
          dataCacheService.clearCache() flatMap { _ =>
            dataCacheService.cacheFormData[Mandate](clientApprovedMandateId, mandate) flatMap { _ =>
              Future.successful(Some(mandate))
            }
          }
        case _ => Future.successful(None)
      }
    }
  }

  def fetchAllClientMandates(agentAuthRetrievals: AgentAuthRetrievals,
                             serviceName: String,
                             allClients: Boolean = true,
                             displayName: Option[String] = None)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Mandates]] = {
    agentClientMandateConnector.fetchAllMandates(agentAuthRetrievals, serviceName, allClients, displayName) map {
      response =>
        response.status match {
          case OK =>
            response.json.asOpt[Seq[Mandate]] match {
            case Some (x) =>
              val pendingMandates = x.filter (a => AgentClientMandateUtils.isPendingStatus (a.currentStatus.status) )
              val activeMandates = x.filter (a => a.currentStatus.status == Status.Active)
              Some (Mandates (activeMandates, pendingMandates) )
            case _ => None
          }
          case _ => None
        }
    }
  }

  def rejectClient(mandateId: String, authRetrievals: AgentAuthRetrievals)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    agentClientMandateConnector.rejectClient(mandateId, authRetrievals.agentCode).map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def acceptClient(mandateId: String, agentAuthRetrievals: AgentAuthRetrievals)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    agentClientMandateConnector.activateMandate(mandateId, agentAuthRetrievals.agentCode).map { response =>
      response.status match {
        case OK => true
        case _  =>
          logger.warn("Status for activation not OK: " + response.status)
          false
      }
    }
  }

  def fetchClientsCancelled(agentAuthRetrievals: AgentAuthRetrievals, serviceName: String)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[String]]] = {
    agentClientMandateConnector.fetchClientsCancelled(agentAuthRetrievals, serviceName).map { response =>
      response.status match {
        case OK => Some(response.json.as[Seq[String]])
        case _ => None
      }
    }
  }

  def fetchAgentDetails(agentAuthRetrievals: AgentAuthRetrievals)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AgentDetails] = {
    agentClientMandateConnector.fetchAgentDetails()
  }

  def removeAgent(mandateId: String, authRetrievals: ClientAuthRetrievals)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    removeAgentClient(mandateId, authRetrievals)
  }

  def removeClient(mandateId: String, authRetrievals: AgentAuthRetrievals)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    removeAgentClient(mandateId, authRetrievals)
  }

  private def removeAgentClient(mandateId: String, authRetrievals: MandateAuthRetrievals)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    agentClientMandateConnector.remove(mandateId).map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def editMandate(mandate: Mandate, agentAuthRetrievals: AgentAuthRetrievals)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.editMandate(mandate, agentAuthRetrievals).map { response =>
      response.status match {
        case OK =>
          val mandate = response.json.asOpt[Mandate]
          logger.info(s"[AgentClientMandateService][editMandate] - Mandate ${mandate.map(_.id)} successfully updated")
          mandate
        case _ => None
      }
    }
  }

  def doesAgentHaveMissingEmail(service: String, agentAuthRetrievals: AgentAuthRetrievals)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    for{
      response <- agentClientMandateConnector.doesAgentHaveMissingEmail(service, agentAuthRetrievals)
      _        <- agentClientMandateConnector.updateAgentCredId(agentAuthRetrievals)
    } yield {
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def updateAgentMissingEmail(emailAddress: Option[String], agentAuthRetrievals: AgentAuthRetrievals, service: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    agentClientMandateConnector.updateAgentMissingEmail(emailAddress, agentAuthRetrievals, service)
  }

  def updateClientEmail(emailAddress: String, mandateId: String, clientAuthRetrievals: ClientAuthRetrievals)
                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Unit = {
    agentClientMandateConnector.updateClientEmail(emailAddress, mandateId, clientAuthRetrievals)
  }

  def updateRegisteredDetails(editAgentDetails: Option[EditAgentAddressDetails] = None,
                              editNonUKIdDetails: Option[Identification] = None,
                              agentAuthRetrievals: AgentAuthRetrievals)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRegistrationDetailsRequest]] = {
    val cachedRespData = dataCacheService.fetchAndGetFormData[AgentDetails](agentDetailsFormId)
    for {
      cachedData <- cachedRespData
      updatedDataResponse <- {
        cachedData match {
          case Some(oldData) => updateDetails(oldData, editAgentDetails, editNonUKIdDetails, agentAuthRetrievals)
          case None => Future.successful(None)
        }
      }
      _ <- updatedDataResponse match {
        case Some(_) => dataCacheService.clearCache().flatMap(r => Future.successful(r))
        case None => Future.successful(None)
      }
    } yield {
      updatedDataResponse
    }

  }

  private def updateDetails(cachedData: AgentDetails,
                            editAgentDetails: Option[EditAgentAddressDetails],
                            nonUkiChangeDetails: Option[Identification],
                            agentAuthRetrievals: AgentAuthRetrievals)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext) = {
    val updateData = UpdateRegistrationDetailsRequest(
      acknowledgementReference = acknowledgementReference,
      isAnIndividual = false,
      individual = None,
      organisation = Some(Organisation(
      organisationName = editAgentDetails.map(_.agentName).getOrElse(cachedData.organisation.map(_.organisationName).getOrElse("")))),
      address = editAgentDetails.map(_.address).getOrElse(cachedData.addressDetails),
      contactDetails = cachedData.contactDetails,
      isAnAgent = true,
      isAGroup = cachedData.organisation.flatMap(_.isAGroup).getOrElse(false),
      identification = nonUkiChangeDetails)

    businessCustomerConnector.updateRegistrationDetails(cachedData.safeId, updateData, agentAuthRetrievals).map {
      response =>
        response.status match {
          case OK => Some(updateData)
          case status =>
            logger.warn(s"[AgentClientMandateService] [updateBusinessDetails] [status] = $status && [response.body] = ${response.body}")
            None
        }
    }
  }

}
