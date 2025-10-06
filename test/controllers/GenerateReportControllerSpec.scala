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

package controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsJson, status}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.GenerateReportController
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import utils.BaseUnitSpec

import scala.concurrent.Future

class GenerateReportControllerSpec extends BaseUnitSpec {

  private val controller = app.injector.instanceOf[GenerateReportController]

  val zRef  = "Z1234"
  val year  = "2025-26"
  val month = "MAY"

  val validRequest: GenerateReportRequest = GenerateReportRequest(oversubscribed = 5, traceAndMatch = 20, failedEligibility = 6)
  val validJson:    JsValue               = Json.toJson(validRequest)

  "GenerateReportController.generateReport" should {

    "return 204 NoContent when both generateReport and callback succeed" in {
      when(mockGenerateReportConnector.generateReport(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Success))
      when(mockCallbackConnector.callback(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(CallbackResponse.Success))

      val request = FakeRequest(POST, s"/generate/$zRef/$year/$month")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef, year, month)(request)

      status(result) shouldBe NO_CONTENT
    }

    "return 500 InternalServerError when generateReport fails" in {
      when(mockGenerateReportConnector.generateReport(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Failure))

      val request = FakeRequest(POST, s"/generate/$zRef/$year/$month")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef, year, month)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 InternalServerError when callback fails" in {
      when(mockGenerateReportConnector.generateReport(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Success))
      when(mockCallbackConnector.callback(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(CallbackResponse.Failure))

      val request = FakeRequest(POST, s"/generate/$zRef/$year/$month")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef, year, month)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 400 BadRequest when zRef, year, and month are invalid" in {
      val invalidzRef  = "z123333333"
      val invalidYear  = "20AB"
      val invalidMonth = "13"

      val request = FakeRequest(POST, s"/generate/$invalidzRef/$invalidYear/$invalidMonth")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(invalidzRef, invalidYear, invalidMonth)(request)

      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "code").as[String]    should include("BAD_REQUEST")
      (json \ "message").as[String] should include("Issue(s) with your request")

      val errors = (json \ "issues").as[Seq[JsObject]]
      errors.find(_.keys.contains("zRef")).flatMap(_.\("zRef").asOpt[String])       shouldBe Some("ZReference did not match expected format")
      errors.find(_.keys.contains("taxYear")).flatMap(_.\("taxYear").asOpt[String]) shouldBe Some("Invalid parameter for tax year")
      errors.find(_.keys.contains("month")).flatMap(_.\("month").asOpt[String])     shouldBe Some("Invalid parameter for month")

    }

    "return 500 InternalServerError when generateReport throws an exception (recover block)" in {
      when(mockGenerateReportConnector.generateReport(any(), any(), any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("fail")))

      val request = FakeRequest(POST, s"/generate/$zRef/$year/$month")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef, year, month)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }
  }

}
