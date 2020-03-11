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

package views.agent

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.play.config.{AssetsConfig, GTMConfig, OptimizelyConfig}
import uk.gov.hmrc.play.views.html.layouts.{Footer, GTMSnippet, Head, OptimizelySnippet}

trait ViewTestHelper {
  self: MockitoSugar =>

  private val mcc: MessagesControllerComponents = stubMessagesControllerComponents()
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang.defaultLang))

  val mockOptConfig: OptimizelyConfig = mock[OptimizelyConfig]
  val mockGtmConfig: GTMConfig = mock[GTMConfig]
  val mockAssetsConfig: AssetsConfig = mock[AssetsConfig]
  val mockConfig: Configuration = mock[Configuration]

  implicit val mockAppConfig: AppConfig = mock[AppConfig]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  when(mockAppConfig.servicesConfig)
    .thenReturn(mockServicesConfig)

  when(mockAppConfig.analyticsHost)
    .thenReturn("")
  when(mockAppConfig.analyticsToken)
    .thenReturn("")

  when(mockOptConfig.url)
    .thenReturn(None)
  when(mockGtmConfig.url)
    .thenReturn(None)
  when(mockAssetsConfig.assetsPrefix)
    .thenReturn("test")

  when(mockAppConfig.configuration)
    .thenReturn(mockConfig)

  val optimizelySnippet: OptimizelySnippet = new OptimizelySnippet(mockOptConfig)
  val gtmSnippet: GTMSnippet = new GTMSnippet(mockGtmConfig)

  when(mockServicesConfig.getString(ArgumentMatchers.eq("microservice.delegated-service-sign-out-url.ated")))
    .thenReturn("http://localhost:9916/ated/logout")

  when(mockAppConfig.customHeadTemplate)
    .thenReturn(new Head(optimizelySnippet, mockAssetsConfig, gtmSnippet))
  when(mockAppConfig.customFooterTemplate)
    .thenReturn(new Footer(mockAssetsConfig))

}
