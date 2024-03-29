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

package views.agent.agentSummary

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

trait ViewTestHelper {
  self: MockitoSugar =>

  private val mcc: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang.defaultLang))

  val mockConfig: Configuration = mock[Configuration]

  implicit val mockAppConfig: AppConfig = mock[AppConfig]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  when(mockAppConfig.servicesConfig)
    .thenReturn(mockServicesConfig)

  when(mockAppConfig.configuration)
    .thenReturn(mockConfig)

  when(mockAppConfig.serviceSignOutUrl(ArgumentMatchers.any()))
    .thenReturn("http://localhost:9916/ated/logout")

}
