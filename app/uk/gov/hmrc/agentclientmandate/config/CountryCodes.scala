/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentclientmandate.config

import java.util.PropertyResourceBundle
import play.api.Environment
import play.api.libs.json.{JsValue, Json}

import java.io.{InputStream, InputStreamReader}
import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.io.Source
import scala.util.{Success, Try}

trait CountryCodes {
  val environment: Environment
  val countryString: InputStream = environment.resourceAsStream("location-autocomplete-canonical-list.json")
    .getOrElse(throw new Exception("no countries file found"))
  val countryJs: JsValue = Json.parse(Source.fromInputStream(countryString).mkString)

  val countryMap: Map[String, String] = countryJs.as[Map[String, String]]


  lazy val resourceStream: PropertyResourceBundle =
    (environment.resourceAsStream("country-code.properties") flatMap { stream =>
      val inputStreamReader: InputStreamReader = new InputStreamReader(stream, "UTF-8")

      val optBundle: Option[PropertyResourceBundle] = Try(new PropertyResourceBundle(inputStreamReader)) match {
        case Success(bundle) => Some(bundle)
        case _               => None
      }
      stream.close()
      optBundle
    }).getOrElse(throw new RuntimeException("[CountryCodes] Could not retrieve property bundle"))

  def getIsoCodeTupleList: List[(String, String)] = {
    resourceStream.getKeys.asScala.toList.map(key => (key, resourceStream.getString(key))).sortBy{case (_,v) => v}
  }
}
