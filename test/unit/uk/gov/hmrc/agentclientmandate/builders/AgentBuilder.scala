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

package unit.uk.gov.hmrc.agentclientmandate.builders

import uk.gov.hmrc.agentclientmandate.models._

object AgentBuilder {

  def buildAgentDetails: AgentDetails = {
    val registeredAddressDetails = RegisteredAddressDetails("address1", "address2", None, None, None, "FR")
    val contactDetails = EtmpContactDetails()
    AgentDetails("safeId", isAnIndividual = false, None,
      Some(Organisation("Org Name", Some(true))),
      registeredAddressDetails, contactDetails, Some(Identification("IdNumber", "issuingCountry", "FR")))
  }

  def buildUkAgentDetails: AgentDetails = {
    val registeredAddressDetails = RegisteredAddressDetails("address1", "address2", None, None, None, "GB")
    val contactDetails = EtmpContactDetails()
    AgentDetails("safeId", isAnIndividual = false, None,
      Some(Organisation("Org Name", Some(true))),
      registeredAddressDetails, contactDetails, None)
  }

}
