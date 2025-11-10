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

package models.errors

import play.api.libs.json.Json
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.ErrorResponse.ValidationFailureResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import utils.BaseUnitSpec

class ErrorResponseSpec extends BaseUnitSpec {

  "ErrorResponse" should {
    "serialize InternalServerErr with default values" in {
      val err  = InternalServerErr()
      val json = Json.toJson(err)

      (json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "deserialize InternalServerErr from JSON" in {
      val json = Json.obj(
        "code"    -> "INTERNAL_SERVER_ERROR",
        "message" -> "There has been an issue processing your request"
      )

      val result = json.as[InternalServerErr]
      result.code    shouldBe "INTERNAL_SERVER_ERROR"
      result.message shouldBe "There has been an issue processing your request"
    }

    "serialize ValidationFailureResponse correctly" in {
      val response = ValidationFailureResponse(
        issues = Seq(
          Map("INVALID_Z_REFERENCE" -> "Invalid parameter for zReference"),
          Map("INVALID_Z_MONTH"     -> "Invalid parameter for month")
        )
      )

      val json = Json.toJson(response)
      (json \ "code").as[String]                          shouldBe "BAD_REQUEST"
      (json \ "message").as[String]                       shouldBe "Issue(s) with your request"
      (json \ "issues").as[Seq[Map[String, String]]].size shouldBe 2
    }

    "deserialize ValidationFailureResponse from JSON" in {
      val json = Json.obj(
        "code"    -> "BAD_REQUEST",
        "message" -> "Issue(s) with your request",
        "issues" -> Json.arr(
          Json.obj("INVALID_Z_REFERENCE" -> "Invalid parameter for zReference"),
          Json.obj("INVALID_MONTH"       -> "Invalid parameter for month")
        )
      )

      val result = json.as[ValidationFailureResponse]
      result.code    shouldBe "BAD_REQUEST"
      result.message shouldBe "Issue(s) with your request"
      result.issues    should contain allOf (
        Map("INVALID_Z_REFERENCE" -> "Invalid parameter for zReference"),
        Map("INVALID_MONTH"       -> "Invalid parameter for month")
      )
    }
  }
}
