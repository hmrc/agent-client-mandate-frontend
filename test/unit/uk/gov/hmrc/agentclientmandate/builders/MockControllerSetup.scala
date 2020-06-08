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

package unit.uk.gov.hmrc.agentclientmandate.builders

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.MessagesControllerComponents
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentclientmandate.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.play.config.{AssetsConfig, GTMConfig, OptimizelyConfig}
import uk.gov.hmrc.play.views.html.layouts.{Footer, GTMSnippet, Head, OptimizelySnippet}

trait MockControllerSetup {
  self: MockitoSugar =>

  val mockAppConfig: AppConfig = mock[AppConfig]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockConfig: Configuration = mock[Configuration]
  val mockEnvironment: Environment = mock[Environment]

  val stubbedMessagesControllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()

  when(mockAppConfig.companyAuthHost)
    .thenReturn("")
  when(mockAppConfig.loginPath)
    .thenReturn("gg/sign-in")
  when(mockAppConfig.analyticsHost)
    .thenReturn("")
  when(mockAppConfig.analyticsToken)
    .thenReturn("")
  when(mockAppConfig.urBannerToggle)
    .thenReturn(true)
  when(mockAppConfig.getIsoCodeTupleList)
    .thenReturn(List())
  when(mockAppConfig.addNonUkClientCorrespondenceUri(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(
      "http://localhost:9933/ated-subscription/registered-business-address" +
        "?backLinkUrl=" +
        "" +
        "http://localhost:9959/mandate/agent/search-previous/callingPage")
  when(mockAppConfig.nonUkUri(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(
      "http://localhost:9923/business-customer/agent/register/non-uk-client/ated" +
        "?backLinkUrl=" +
        "" +
        "http://localhost:9959/mandate/agent/client-registered-previously/callPage")
  when(mockAppConfig.servicesConfig)
    .thenReturn(mockServicesConfig)
  when(mockServicesConfig.getString(ArgumentMatchers.eq("microservice.delegated-service-redirect-url.ated")))
    .thenReturn("http://localhost:9916/ated/account-summary")
  when(mockAppConfig.environment)
    .thenReturn(mockEnvironment)
  when(mockAppConfig.loginCallbackAgent).thenReturn("/mandate/agent/summary")
  when(mockAppConfig.loginCallbackClient).thenReturn("/mandate/client/email")

  val mockOptConfig: OptimizelyConfig = mock[OptimizelyConfig]
  val mockGtmConfig: GTMConfig = mock[GTMConfig]
  val mockAssetsConfig: AssetsConfig = mock[AssetsConfig]

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

  when(mockAppConfig.customHeadTemplate)
    .thenReturn(new Head(optimizelySnippet, mockAssetsConfig, gtmSnippet))
  when(mockAppConfig.customFooterTemplate)
    .thenReturn(new Footer(mockAssetsConfig))
}
