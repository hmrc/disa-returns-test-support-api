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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.disareturnstestsupportapi.config.Constants.{AgentEnrolmentKey, DelegatedAuthRule, EnrolmentKey, IdentifierKey}

trait CommonStubs {

  private val enrolmentVerificationAuthRequest = s"""{
    "authorise":[{"$$or":[
      [{"affinityGroup":"Organisation"},{"enrolment":"$EnrolmentKey","identifiers":[],"state":"Activated"}],
      [{"affinityGroup":"Agent"},{"enrolment":"$AgentEnrolmentKey","identifiers":[],"state":"Activated"}]
    ]}],
    "retrieve":["authorisedEnrolments","affinityGroup"]
  }"""

  private val authenticatedAuthRequest = s"""{
    "authorise":[{"$$or":[
      {"affinityGroup":"Organisation"},
      [{"affinityGroup":"Agent"},{"enrolment":"$AgentEnrolmentKey","identifiers":[],"state":"Activated"}]
    ]}],
    "retrieve":["affinityGroup"]
  }"""

  def agentDelegatedAuthRequest(zRef: String = TestConstants.validZReference): String = Json.stringify(
    Json.obj(
      "authorise" -> Json.arr(
        Json.obj(
          "enrolment"         -> EnrolmentKey,
          "identifiers"       -> Json.arr(Json.obj("key" -> IdentifierKey, "value" -> zRef)),
          "state"             -> "Activated",
          "delegatedAuthRule" -> DelegatedAuthRule
        )
      ),
      "retrieve" -> Json.arr()
    )
  )

  def stubAuth(zRef: String = TestConstants.validZReference): Unit = {
    val validEnrolment =
      s"""{
      "authorisedEnrolments": [{
        "key": "$EnrolmentKey",
        "identifiers": [{ "key": "$IdentifierKey", "value": "$zRef" }],
        "state": "Activated"
      }],
      "affinityGroup": "Organisation"
      }"""

    stubFor {
      post("/auth/authorise")
        .withRequestBody(equalToJson(enrolmentVerificationAuthRequest))
        .willReturn {
          aResponse.withStatus(OK).withBody(validEnrolment)
        }
    }
  }

  def stubAgentAuth(
    hasDelegatedEnrolment: Boolean,
    enrolmentCheckEnabled: Boolean = true,
    zRef:                  String = TestConstants.validZReference
  ): Unit = {
    val agentDetails = """{"authorisedEnrolments": [], "affinityGroup": "Agent"}"""

    stubFor {
      post("/auth/authorise")
        .withRequestBody(equalToJson(if (enrolmentCheckEnabled) enrolmentVerificationAuthRequest else authenticatedAuthRequest))
        .willReturn(aResponse.withStatus(OK).withBody(agentDetails))
    }

    val delegatedEnrolmentResponse =
      if (hasDelegatedEnrolment) aResponse.withStatus(OK).withBody("{}")
      else aResponse.withStatus(Status.UNAUTHORIZED).withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")

    stubFor {
      post("/auth/authorise")
        .withRequestBody(equalToJson(agentDelegatedAuthRequest(zRef)))
        .willReturn(delegatedEnrolmentResponse)
    }
  }

  def verifyAgentDelegatedAuthRequested(times: Int, zRef: String = TestConstants.validZReference): Unit =
    verify(times, postRequestedFor(urlEqualTo("/auth/authorise")).withRequestBody(equalToJson(agentDelegatedAuthRequest(zRef))))

  def stubAuthFail(): Unit =
    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse()
            .withStatus(Status.UNAUTHORIZED)
            .withBody("{}")
        }
    }

  def stubGenerateReport(
    status:       ResponseDefinitionBuilder,
    zRef:         String,
    responseBody: Option[String] = None
  ): Unit = {

    val responseWithBody = responseBody match {
      case Some(body) => status.withBody(body)
      case None       => status
    }

    stubFor(
      post(urlEqualTo(s"/monthly/$zRef/reconciliation"))
        .willReturn(responseWithBody)
    )
  }

  def stubCallback(status: ResponseDefinitionBuilder, zRef: String): Unit =
    stubFor(
      post(urlEqualTo(s"/callback/monthly/$zRef"))
        .willReturn(status)
    )

  def stubReportingWindowOverride(status: ResponseDefinitionBuilder, zRef: String): Unit =
    stubFor(
      put(urlEqualTo(s"/reporting-window-override/$zRef"))
        .willReturn(status)
    )

}
