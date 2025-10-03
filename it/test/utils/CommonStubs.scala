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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.JsObject

trait CommonStubs {

  def stubAuth(): Unit =
    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody("{}")
        }
    }

  def stubAuthFail(): Unit =
    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse()
            .withStatus(Status.UNAUTHORIZED)
            .withBody("{}")
        }
    }

  def stubGenerateReport(status: Int, body: JsObject, zRef: String, year: String, month: String): Unit =
    stubFor(
      post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body.toString)
        )
    )

  def stubCallback(status: Int, zRef: String, year: String, month: String): Unit =
    stubFor(
      post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
        .willReturn(
          aResponse()
            .withStatus(status)
        )
    )

}
