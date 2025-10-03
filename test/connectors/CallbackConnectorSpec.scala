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

package connectors

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import uk.gov.hmrc.disareturnstestsupportapi.connectors.CallbackConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import utils.BaseUnitSpec

import scala.concurrent.Future

class CallbackConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    val connector = new CallbackConnector(mockAppConfig, mockHttpClient)

    val zref         = "Z1234"
    val year         = "2025-26"
    val month        = "FEB"
    val totalRecords = 42
    val testUrl      = "http://localhost:1200"

    when(mockAppConfig.disaReturnsBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.post(url"$testUrl/callback/monthly/$zref/$year/$month")).thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "CallbackConnector.sendMonthlyCallback" should {

    "return Success when the response status is 204" in new TestSetup {
      val httpResponse: HttpResponse = HttpResponse(204, "")
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(httpResponse))

      val result: CallbackResponse = connector.sendMonthlyCallback(zref, year, month, totalRecords).futureValue
      result shouldBe CallbackResponse.Success
    }

    "return Failure when the response status is not 204" in new TestSetup {
      val httpResponse: HttpResponse = HttpResponse(500, "")
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(httpResponse))

      val result: CallbackResponse = connector.sendMonthlyCallback(zref, year, month, totalRecords).futureValue
      result shouldBe CallbackResponse.Failure
    }

    "return Failure when the call throws an exception" in new TestSetup {
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.failed(new RuntimeException("Timeout")))

      val result: CallbackResponse = connector
        .sendMonthlyCallback(zref, year, month, totalRecords)
        .recover { case _ =>
          CallbackResponse.Failure
        }
        .futureValue

      result shouldBe CallbackResponse.Failure
    }
  }
}
