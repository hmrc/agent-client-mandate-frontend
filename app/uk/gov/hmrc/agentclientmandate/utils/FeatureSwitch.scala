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

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.libs.json.{Json, OFormat}

import scala.util.Try

case class FeatureSwitch(name: String, enabled: Boolean)

object FeatureSwitch {

  def forName(name: String)(implicit config: ServicesConfig): FeatureSwitch = {
    FeatureSwitch(name, isEnabled(name))
  }

  def isEnabled(name: String)(implicit config: ServicesConfig): Boolean = {
    val sysPropValue = sys.props.get(systemPropertyName(name))
    sysPropValue match {
      case Some(x)  => x.toBoolean
      case None     => Try(config.getBoolean(confPropertyName(name))).getOrElse(false)
    }
  }

  def disable(switch: FeatureSwitch)(implicit config: ServicesConfig): FeatureSwitch = setProp(switch.name, value = false)

  def setProp(name: String, value: Boolean)(implicit config: ServicesConfig): FeatureSwitch = {
    val systemProps = sys.props.+=((systemPropertyName(name), value.toString))
    forName(name)
  }

  def enable(switch: FeatureSwitch)(implicit config: ServicesConfig): FeatureSwitch = {
    setProp(switch.name, value = true)
  }

  def confPropertyName(name: String) = s"features.$name"

  def systemPropertyName(name: String) = s"features.$name"

  implicit val format: OFormat[FeatureSwitch] = Json.format[FeatureSwitch]
}

object MandateFeatureSwitches {

  def  singleService(implicit config: ServicesConfig): FeatureSwitch = FeatureSwitch.forName("single_service")

  def byName(name: String)(implicit config: ServicesConfig): Option[FeatureSwitch] = name match {
    case "single_service" => Some(singleService)
    case _ => None
  }

}
