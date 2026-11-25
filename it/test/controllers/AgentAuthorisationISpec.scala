/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, noContent}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.{FORBIDDEN, NO_CONTENT, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.await
import utils.BaseIntegrationSpec
import utils.TestConstants.{bearerToken, reportingWindowEnd, reportingWindowStart, validZReference}

trait AgentAuthorisationBehaviours { self: BaseIntegrationSpec =>

  protected val agentClientRelationshipNotFound: JsValue = Json.obj(
    "code"    -> "AGENT_CLIENT_RELATIONSHIP_NOT_FOUND",
    "message" -> s"We are unable to authenticate your access to $validZReference"
  )

  protected val unauthorised: JsValue = Json.obj("code" -> "UNAUTHORIZED", "message" -> "Unauthorized")

  protected def generateReportRequest(): WSResponse = {
    stubGenerateReport(noContent, validZReference)
    stubCallback(noContent, validZReference)
    await(
      ws.url(s"http://localhost:$port/monthly/$validZReference/reconciliation")
        .withHttpHeaders(AUTHORIZATION -> bearerToken)
        .post(Json.obj("oversubscribed" -> 1, "traceAndMatch" -> 1, "failedEligibility" -> 1))
    )
  }

  protected def reportingWindowOverrideRequest(): WSResponse = {
    stubReportingWindowOverride(aResponse().withStatus(NO_CONTENT), validZReference)
    await(
      ws.url(s"http://localhost:$port/monthly/$validZReference/reporting-window-override")
        .withHttpHeaders(AUTHORIZATION -> bearerToken)
        .put(Json.obj("startDate" -> reportingWindowStart.toString, "endDate" -> reportingWindowEnd.toString))
    )
  }

  protected val endpoints: Seq[(String, () => WSResponse)] = Seq(
    ("generate test reconciliation report", () => generateReportRequest()),
    ("reporting window override", () => reportingWindowOverrideRequest())
  )
}

class AgentAuthorisationISpec extends BaseIntegrationSpec with AgentAuthorisationBehaviours {

  override def config: Map[String, Any] =
    super.config ++ Map(
      "features.enrolment-verification-enabled" -> true,
      "features.agent-authentication-enabled"   -> true
    )

  endpoints.foreach { case (name, request) =>
    s"the $name endpoint" should {

      "allow an agent with a delegated enrolment matching the requested Z-reference through" in {
        stubAgentAuth(hasDelegatedEnrolment = true)

        request().status shouldBe NO_CONTENT
        verifyAgentDelegatedAuthRequested(times = 1)
      }

      "return 403 AGENT_CLIENT_RELATIONSHIP_NOT_FOUND when the agent does not have a delegated enrolment matching the requested Z-reference" in {
        stubAgentAuth(hasDelegatedEnrolment = false)

        val result = request()

        result.status shouldBe FORBIDDEN
        result.json   shouldBe agentClientRelationshipNotFound
        verifyAgentDelegatedAuthRequested(times = 1)
      }
    }
  }
}

class AgentAuthorisationEnrolmentCheckDisabledISpec extends BaseIntegrationSpec with AgentAuthorisationBehaviours {

  override def config: Map[String, Any] =
    super.config ++ Map(
      "features.enrolment-verification-enabled" -> false,
      "features.agent-authentication-enabled"   -> true
    )

  endpoints.foreach { case (name, request) =>
    s"the $name endpoint" should {

      "allow an agent without a delegated enrolment matching the requested Z-reference through when the enrolment check is disabled" in {
        stubAgentAuth(hasDelegatedEnrolment = false, enrolmentCheckEnabled = false)

        request().status shouldBe NO_CONTENT
        verifyAgentDelegatedAuthRequested(times = 0)
      }
    }
  }
}

class AgentAuthorisationDisabledISpec extends BaseIntegrationSpec with AgentAuthorisationBehaviours {

  override def config: Map[String, Any] =
    super.config ++ Map(
      "features.enrolment-verification-enabled" -> true,
      "features.agent-authentication-enabled"   -> false
    )

  endpoints.foreach { case (name, request) =>
    s"the $name endpoint" should {

      "return 401 for an agent with a delegated enrolment when agent authentication is disabled" in {
        stubAgentAuth(hasDelegatedEnrolment = true)

        val result = request()

        result.status shouldBe UNAUTHORIZED
        result.json   shouldBe unauthorised
        verifyAgentDelegatedAuthRequested(times = 0)
      }
    }
  }
}

class AgentAuthorisationDisabledWithEnrolmentCheckDisabledISpec extends BaseIntegrationSpec with AgentAuthorisationBehaviours {

  override def config: Map[String, Any] =
    super.config ++ Map(
      "features.enrolment-verification-enabled" -> false,
      "features.agent-authentication-enabled"   -> false
    )

  endpoints.foreach { case (name, request) =>
    s"the $name endpoint" should {

      "allow an agent without a delegated enrolment matching the requested Z-reference through when the enrolment check is disabled, even though agent authentication is disabled" in {
        stubAgentAuth(hasDelegatedEnrolment = false, enrolmentCheckEnabled = false)

        request().status shouldBe NO_CONTENT
        verifyAgentDelegatedAuthRequested(times = 0)
      }
    }
  }
}
