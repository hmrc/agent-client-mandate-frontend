/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.utils

trait MandateConstants {

  val agentFormId: String = "agent-form-id"
  val agentEmailFormId: String = "agent-email"
  val agentRefCacheId: String = "agent-ref-id"
  val clientFormId: String = "client-form-id"
  val clientEditEmailId: String = "client-edit-email-form-id"
  val clientApprovedMandateId = "client-approved"
  val clientDisplayNameFormId = "client-display-name-form-id"
  val agentDetailsFormId = "agent-details"
  val overseasTaxRefFormId = "overseas-tax-ref"
  val nrlFormId = "nrl-form-id"
  val paySaFormId = "paysa-form-id"
  val clientPermissionFormId = "client-permission-form-id"
  val prevUniqueAuthNumFormId = "prev-unique-auth-num-form-id"
  val prevRegisteredFormId = "previously-registered-form-id"
  val oldNonUkMandate = "old-nonuk-mandate-form-id"
  val callingPageCacheId = "calling-page-id"
}
