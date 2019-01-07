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

package unit.uk.gov.hmrc.agentclientmandate.builders

import uk.gov.hmrc.domain.{AgentBusinessUtr, Generator}

import scala.util.Random

class AgentBusinessUtrGenerator(random: Random = new Random) extends Generator {

  def this(seed: Int) = this(new scala.util.Random(seed))

  val OneMillion = 1000000

  def nextAgentBusinessUtr: AgentBusinessUtr = {
    val suffix = f"${random.nextInt(OneMillion)}%07d"
    val weighting = s"ARN$suffix"
    val checkCharacter = calculateCheckCharacter(weighting)
    AgentBusinessUtr(f"${checkCharacter}ARN$suffix")
  }

}
