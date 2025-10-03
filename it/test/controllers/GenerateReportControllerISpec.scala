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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.await
import utils.BaseIntegrationSpec

class GenerateReportControllerISpec extends BaseIntegrationSpec {

  val zRef         = "Z1234"
  val invalidZRef  = "1234"
  val year         = "2025-26"
  val invalidYear  = "202526"
  val month        = "FEB"
  val invalidMonth = "XYZ"

  val validJsonBody: String =
    """
      |{
      |  "oversubscribed": 10,
      |  "traceAndMatch": 5,
      |  "failedEligibility": 3
      |}
    """.stripMargin

  val invalidJsonBody: String =
    """
      |{
      |  "oversubscribeddddd": 10,
      |  "traceAndMatch": 5,
      |  "failedEligibility": 3
      |}
    """.stripMargin

  val validParsedJson:   JsValue = Json.parse(validJsonBody)
  val invalidParsedJson: JsValue = Json.parse(invalidJsonBody)

  "POST /monthly/:zRef/:year/:month/reconciliation" should {

    "return 204 NoContent when generate and callback both succeed" in {

      stubFor(
        post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
          .willReturn(noContent)
      )
      stubFor(
        post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
          .willReturn(noContent)
      )

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status shouldBe NO_CONTENT
    }

    "return 500 InternalServerError when callback fails" in {
      stubFor(
        post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
          .willReturn(noContent)
      )
      stubFor(
        post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
          .willReturn(serverError)
      )

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 InternalServerError when generateReport fails" in {
      stubFor(
        post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
          .willReturn(serverError)
      )
      stubFor(
        post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
          .willReturn(noContent())
      )
      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe INTERNAL_SERVER_ERROR
      (result.json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (result.json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 InternalServerError when generateReport throws an exception" in {
      stubFor(
        post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
      )
      stubFor(
        post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
          .willReturn(noContent())
      )
      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe INTERNAL_SERVER_ERROR
      (result.json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (result.json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 400 BadRequest when body JSON is invalid for oversubscribed" in {

      val result = generateRequest(zRef = zRef, year = year, month = month, body = invalidParsedJson)

      result.status shouldBe BAD_REQUEST

      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Issue(s) with your request"

      val errors = (result.json \ "issues").as[Seq[JsValue]]
      errors.map(e => (e \ "oversubscribed").as[String]).head shouldBe "This field is required"

    }
    "return 400 BadRequest when body JSON is invalid for oversubscribed field value is less than zero" in {

      val invalidJsonBody: String =
        """
          |{
          |  "oversubscribed": -10,
          |  "traceAndMatch": 5,
          |  "failedEligibility": 3
          |}
    """.stripMargin

      val result = generateRequest(zRef = zRef, year = year, month = month, body = Json.parse(invalidJsonBody))

      result.status shouldBe BAD_REQUEST

      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Issue(s) with your request"

      val errors = (result.json \ "issues").as[Seq[JsValue]]
      errors.map(e => (e \ "oversubscribed").as[String]).head shouldBe "This field must be greater than or equal to 0"

    }

    "return 400 BadRequest when validation fails zRef" in {
      val result = generateRequest(zRef = invalidZRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Issue(s) with your request"

      val errors = (result.json \ "issues").as[Seq[JsValue]]
      errors.map(e => (e \ "zRef").as[String]).head shouldBe "ZReference did not match expected format"
    }
    "return 400 BadRequest when validation fails taxYear" in {
      val result = generateRequest(zRef = zRef, year = invalidYear, month = month, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Issue(s) with your request"

      val errors = (result.json \ "issues").as[Seq[JsValue]]
      errors.map(e => (e \ "taxYear").as[String]).head shouldBe "TaxYear did not match expected format"
    }
    "return 400 BadRequest when validation fails month" in {
      val result = generateRequest(zRef = zRef, year = year, month = invalidMonth, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Issue(s) with your request"

      val errors = (result.json \ "issues").as[Seq[JsValue]]
      errors.map(e => (e \ "month").as[String]).head shouldBe "Month did not match expected format"
    }
  }

  val testHeaders: Seq[(String, String)] = Seq("Content-Type" -> "application/json")

  def generateRequest(
    zRef:    String,
    year:    String,
    month:   String,
    body:    JsValue,
    headers: Seq[(String, String)] = testHeaders
  ): WSResponse = {
    stubAuth()
    await(
      ws.url(
        s"http://localhost:$port/$zRef/$year/$month/reconciliation"
      ).withFollowRedirects(follow = false)
        .withHttpHeaders(headers: _*)
        .post(body)
    )
  }
}
