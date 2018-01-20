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

package uk.gov.hmrc.agentclientmandate.service

import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.agentclientmandate.connectors.{AgentClientMandateConnector, BusinessCustomerConnector}
import uk.gov.hmrc.agentclientmandate.models._
import uk.gov.hmrc.agentclientmandate.utils.{AgentClientMandateUtils, MandateConstants}
import uk.gov.hmrc.agentclientmandate.viewModelsAndForms._
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

case class Mandates(activeMandates: Seq[Mandate], pendingMandates: Seq[Mandate])

trait AgentClientMandateService extends MandateConstants {

  def dataCacheService: DataCacheService

  def agentClientMandateConnector: AgentClientMandateConnector

  def businessCustomerConnector: BusinessCustomerConnector

  def createMandate(service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[String] = {
    dataCacheService.fetchAndGetFormData[AgentEmail](agentEmailFormId) flatMap {
      case Some(cachedEmail) =>
        dataCacheService.fetchAndGetFormData[ClientDisplayName](clientDisplayNameFormId) flatMap {
          case Some(displayName) =>
            val mandateDto = CreateMandateDto(cachedEmail.email, service, displayName.name)
            agentClientMandateConnector.createMandate(mandateDto) flatMap {
              response => response.status match {
                case CREATED =>
                  val mandateId = (response.json \ "mandateId").as[String]
                  dataCacheService.clearCache() flatMap { clearCacheResponse =>
                    val clientDetails = ClientMandateDisplayDetails(displayName.name, mandateId, cachedEmail.email)
                    dataCacheService.cacheFormData[ClientMandateDisplayDetails](agentRefCacheId, clientDetails) flatMap { cachingResponse =>
                      Future.successful(mandateId)
                    }
                  }
                case status => throw new RuntimeException(s"Mandate not created for $service")
              }
            }
          case None => throw new RuntimeException(s"Client Display Name not found in cache for $service")
        }
      case None => throw new RuntimeException(s"Email not found in cache for $service")
    }
  }

  def fetchClientMandateClientName(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Mandate] = {
    fetchClientMandate(mandateId).map {
      case Some(mandate) => mandate
      case _ => throw new RuntimeException(s"[AgentClientMandateService][fetchClientMandateClientName] No Mandate returned for id $mandateId")
    }
  }

  def fetchClientMandateAgentName(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[String] = {
    fetchClientMandate(mandateId).map {
      case Some(mandate) => mandate.agentParty.name
      case _ => throw new RuntimeException(s"[AgentClientMandateService][fetchClientMandateAgentName] No Mandate Agent Name returned with id $mandateId")
    }
  }


  def fetchClientMandate(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.fetchMandate(mandateId) map {
      response => response.status match {
        case OK => response.json.asOpt[Mandate]
        case status => None
      }
    }
  }

  def fetchClientMandateByClient(clientId: String, serviceName: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.fetchMandateByClient(clientId, serviceName) map {
      response => response.status match {
        case OK => response.json.asOpt[Mandate]
        case status => None
      }
    }
  }

  def approveMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.approveMandate(mandate) flatMap { response =>
      response.status match {
        case OK =>
          val mandate = response.json.as[Mandate]
          dataCacheService.clearCache() flatMap { clearCacheRep =>
            dataCacheService.cacheFormData[Mandate](clientApprovedMandateId, mandate) flatMap { cacheResp =>
              Future.successful(Some(mandate))
            }
          }
        case status => Future.successful(None)
      }
    }
  }

  def fetchAllClientMandates(arn: String,
                             serviceName: String,
                             allClients: Boolean = true,
                             displayName: Option[String] = None,
                             update: Boolean = false)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandates]] = {
    agentClientMandateConnector.fetchAllMandates(arn, serviceName, allClients, displayName) map {
      response =>
        response.status match {
          case OK =>
            response.json.asOpt[Seq[Mandate]] match {
            case Some (x) =>
              val pendingMandates = x.filter (a => AgentClientMandateUtils.isPendingStatus (a.currentStatus.status) )
              val activeMandates = x.filter (a => a.currentStatus.status == Status.Active)
              Some (Mandates (activeMandates, pendingMandates) )
            case None => None
          }
          case NOT_FOUND => None
        }
    }
  }

  def rejectClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = {
    agentClientMandateConnector.rejectClient(mandateId).map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def acceptClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = {
    agentClientMandateConnector.activateMandate(mandateId).map { response =>
      response.status match {
        case OK => true
        case _ => {
          Logger.warn("Status for activation not OK: " + response.status)
          false
        }
      }
    }
  }

  def fetchClientsCancelled(arn: String, serviceName: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Seq[String]]] = {
    agentClientMandateConnector.fetchClientsCancelled(arn, serviceName).map { response =>
      response.status match {
        case OK => Some(response.json.as[Seq[String]])
        case _ => None
      }
    }
  }

  def fetchAgentDetails()(implicit hc: HeaderCarrier, ac: AuthContext): Future[AgentDetails] = {
    agentClientMandateConnector.fetchAgentDetails()
  }

  def removeAgent(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = {
    agentClientMandateConnector.remove(mandateId).map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def removeClient(mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = {
    agentClientMandateConnector.remove(mandateId).map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def editMandate(mandate: Mandate)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[Mandate]] = {
    agentClientMandateConnector.editMandate(mandate).map { response =>
      response.status match {
        case OK => response.json.asOpt[Mandate]
        case status => None
      }
    }
  }

  def doesAgentHaveMissingEmail(service: String, arn: String)(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = {
    for{
      response <- agentClientMandateConnector.doesAgentHaveMissingEmail(service, arn)
      _ <- agentClientMandateConnector.updateAgentCredId(ac.user.userId)
    } yield {
      response.status match {
        case OK => true
        case _ => false
      }
    }
  }

  def updateAgentMissingEmail(emailAddress: String, arn: String, service: String)(implicit hc: HeaderCarrier, ac: AuthContext): Unit = {
    agentClientMandateConnector.updateAgentMissingEmail(emailAddress, arn, service)
  }

  def updateClientEmail(emailAddress: String, mandateId: String)(implicit hc: HeaderCarrier, ac: AuthContext): Unit = {
    agentClientMandateConnector.updateClientEmail(emailAddress, mandateId)
  }

  def updateRegisteredDetails(editAgentDetails: Option[EditAgentAddressDetails] = None,
                              editNonUKIdDetails: Option[Identification] = None)
                             (implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[UpdateRegistrationDetailsRequest]] = {
    val cachedRespData = dataCacheService.fetchAndGetFormData[AgentDetails](agentDetailsFormId)
    for {
      cachedData <- cachedRespData
      updatedDataResponse <- {
        cachedData match {
          case Some(oldData) => updateDetails(oldData, editAgentDetails, editNonUKIdDetails)
          case None => Future.successful(None)
        }
      }
      _ <- updatedDataResponse match {
        case Some(x) => dataCacheService.clearCache().flatMap(r => Future.successful(r))
        case None => Future.successful(None)
      }
    } yield {
      updatedDataResponse
    }

  }

  private def updateDetails(cachedData: AgentDetails,
                                    editAgentDetails: Option[EditAgentAddressDetails],
                                    nonUkiChangeDetails: Option[Identification])(implicit hc: HeaderCarrier, ac: AuthContext) = {
    val updateData = UpdateRegistrationDetailsRequest(isAnIndividual = false,
      individual = None,
      organisation = Some(Organisation(organisationName = editAgentDetails.map(_.agentName).getOrElse(cachedData.organisation.map(_.organisationName).getOrElse("")),
        isAGroup = cachedData.organisation.flatMap(_.isAGroup),
        organisationType = cachedData.organisation.flatMap(_.organisationType))),
      address = editAgentDetails.map(_.address).getOrElse(cachedData.addressDetails),
      contactDetails = cachedData.contactDetails,
      isAnAgent = true,
      isAGroup = cachedData.organisation.flatMap(_.isAGroup).getOrElse(false),
      identification = nonUkiChangeDetails)

    businessCustomerConnector.updateRegistrationDetails(cachedData.safeId, updateData).map {
      response =>
        response.status match {
          case OK => Some(updateData)
          case status =>
            Logger.warn(s"[AgentClientMandateService] [updateBusinessDetails] [status] = ${status} && [response.body] = ${response.body}")
            None
        }
    }
  }

}

object AgentClientMandateService extends AgentClientMandateService {
  val dataCacheService = DataCacheService
  val agentClientMandateConnector = AgentClientMandateConnector
  val businessCustomerConnector = BusinessCustomerConnector
}
