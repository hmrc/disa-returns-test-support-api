/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import play.api.libs.json._
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import utils.BaseUnitSpec

class GenerateReportRequestSpec extends BaseUnitSpec {

  "GenerateReportRequest.totalRecords" should {
    "return the correct total of all fields" in {
      val request = GenerateReportRequest(oversubscribed = 10, traceAndMatch = 5, failedEligibility = 3)
      request.totalRecords shouldBe 18
    }

    "return zero when all fields are zero" in {
      val request = GenerateReportRequest(0, 0, 0)
      request.totalRecords shouldBe 0
    }
  }

  "GenerateReportRequest JSON validation" should {
    "successfully parse valid JSON" in {
      val json = Json.parse("""
          |{
          |  "oversubscribed": 2,
          |  "traceAndMatch": 3,
          |  "failedEligibility": 4
          |}
        """.stripMargin)

      val result = json.validate[GenerateReportRequest]
      result.isSuccess shouldBe true
      result.get       shouldBe GenerateReportRequest(2, 3, 4)
    }

    "fail to parse JSON with negative values" in {
      val json = Json.parse("""
          |{
          |  "oversubscribed": -1,
          |  "traceAndMatch": 3,
          |  "failedEligibility": 4
          |}
        """.stripMargin)

      val result = json.validate[GenerateReportRequest]
      result.isError shouldBe true
    }

    "fail to parse JSON with missing fields" in {
      val json = Json.parse("""
          |{
          |  "oversubscribed": 1,
          |  "traceAndMatch": 2
          |}
        """.stripMargin)

      val result = json.validate[GenerateReportRequest]
      result.isError shouldBe true
    }
  }
}
