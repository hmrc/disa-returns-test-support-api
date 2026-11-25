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

package controllers.actions

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.{AffinityGroup, BearerTokenExpired, Enrolment, UnsupportedAuthProvider}
import uk.gov.hmrc.disareturnstestsupportapi.config.Constants.{AgentEnrolmentKey, EnrolmentKey}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.actions.{AuthenticatedAuthAction, EnrolmentVerificationAuthAction}
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import utils.BaseUnitSpec

import scala.concurrent.Future

class AuthActionSpec extends BaseUnitSpec {

  private def enrolmentAuthAction(zRef: String, agentAuthenticationEnabled: Boolean = true) = {
    when(mockAppConfig.agentAuthenticationEnabled).thenReturn(agentAuthenticationEnabled)
    new EnrolmentVerificationAuthAction(mockAuthConnector, stubControllerComponents(), mockAppConfig).apply(zRef)
  }

  private def authenticatedAuthAction(zRef: String) =
    new AuthenticatedAuthAction(mockAuthConnector, stubControllerComponents()).apply(zRef)

  import play.api.mvc.Results._
  def testBlock: Request[AnyContent] => Future[Result] =
    _ => Future.successful(Ok("Success"))

  private val agentClientRelationshipNotFound = Json.obj(
    "code"    -> "AGENT_CLIENT_RELATIONSHIP_NOT_FOUND",
    "message" -> s"We are unable to authenticate your access to $validZRef"
  )

  "EnrolmentVerificationAuthAction.invokeBlock" should {

    "allow the request when user is authorised" in {
      authorizationForZRef()

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Success"

      val predicateCaptor: ArgumentCaptor[Predicate] =
        ArgumentCaptor.forClass(classOf[Predicate])

      val retrievalCaptor: ArgumentCaptor[Retrieval[?]] =
        ArgumentCaptor.forClass(classOf[Retrieval[?]])

      verify(mockAuthConnector).authorise(
        predicateCaptor.capture(),
        retrievalCaptor.capture()
      )(any(), any())

      val actualPredicate = predicateCaptor.getValue
      val expectedPredicate =
        (AffinityGroup.Organisation and Enrolment(EnrolmentKey)) or (AffinityGroup.Agent and Enrolment(AgentEnrolmentKey))

      val actualRetrieval   = retrievalCaptor.getValue
      val expectedRetrieval = authorisedEnrolments and affinityGroup

      withClue(
        s"""
           |Auth predicate mismatch:
           |Expected: $expectedPredicate
           |Actual:   $actualPredicate
           |""".stripMargin
      ) {
        actualPredicate shouldBe expectedPredicate
      }

      withClue(
        s"""
           |Auth retrieval mismatch:
           |Expected: $expectedRetrieval
           |Actual:   $actualRetrieval
           |""".stripMargin
      ) {
        actualRetrieval shouldBe expectedRetrieval
      }
    }

    "allow an agent with a delegated enrolment for the Z-Ref" in {
      stubEnrolments(affinityGroup = Some(AffinityGroup.Agent))
      stubAgentDelegatedEnrolmentSuccess()

      val result = enrolmentAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Success"
      verify(mockAuthConnector).authorise(eqTo(agentDelegatedEnrolmentPredicate()), any())(any(), any())
    }

    "return FORBIDDEN with AGENT_CLIENT_RELATIONSHIP_NOT_FOUND when an agent does not have a delegated enrolment for the Z-Ref" in {
      stubEnrolments(affinityGroup = Some(AffinityGroup.Agent))
      stubAgentDelegatedEnrolmentFailure()

      val result = enrolmentAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

      status(result)        shouldBe FORBIDDEN
      contentAsJson(result) shouldBe agentClientRelationshipNotFound
    }

    "return UNAUTHORISED without checking for a delegated enrolment when agent authentication is disabled" in {
      stubEnrolments(affinityGroup = Some(AffinityGroup.Agent))

      val result = enrolmentAuthAction(validZRef, agentAuthenticationEnabled = false).invokeBlock(FakeRequest(), testBlock)

      status(result)        shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.toJson(UnauthorisedErr())
      verify(mockAuthConnector, never()).authorise(eqTo(agentDelegatedEnrolmentPredicate()), any())(any(), any())
    }

    "return UNAUTHORISED when the agent delegated enrolment check fails with another AuthorisationException" in {
      stubEnrolments(affinityGroup = Some(AffinityGroup.Agent))
      stubAgentDelegatedEnrolmentFailure(BearerTokenExpired("expired"))

      val result = enrolmentAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

      status(result)        shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe Json.toJson(UnauthorisedErr(message = "expired"))
    }

    "return InternalServerError when the agent delegated enrolment check fails unexpectedly" in {
      stubEnrolments(affinityGroup = Some(AffinityGroup.Agent))
      stubAgentDelegatedEnrolmentFailure(new RuntimeException("Unexpected error"))

      val result = enrolmentAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.toJson(InternalServerErr())
    }

    Seq(Some(AffinityGroup.Individual), None).foreach { maybeAffinityGroup =>
      s"return UNAUTHORISED when auth returns an affinity group of $maybeAffinityGroup" in {
        stubEnrolments(affinityGroup = maybeAffinityGroup)

        val result = enrolmentAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

        status(result)        shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe Json.toJson(UnauthorisedErr())
      }
    }

    "return UNAUTHORISED when zRef does not match that retrieved from enrolment" in {
      authorizationForZRef("Z1235")

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)                            shouldBe UNAUTHORIZED
      contentAsJson(result).as[UnauthorisedErr] shouldBe UnauthorisedErr(message = "Z-Ref does not match enrolment.")
    }

    "return UNAUTHORISED when enrolment key does not match" in {
      stubEnrolments(enrolmentKey = "HMRC-HELLO")

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)                            shouldBe UNAUTHORIZED
      contentAsJson(result).as[UnauthorisedErr] shouldBe UnauthorisedErr(message = "Z-Ref does not match enrolment.")
    }

    "return UNAUTHORISED when identifier is missing" in {
      stubEnrolments(identifierKey = None)

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)                            shouldBe UNAUTHORIZED
      contentAsJson(result).as[UnauthorisedErr] shouldBe UnauthorisedErr(message = "Z-Ref does not match enrolment.")
    }

    "return UNAUTHORISED when AuthorisationException is thrown by auth connector" in {
      unauthorized(UnsupportedAuthProvider("fubar"))

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)                            shouldBe UNAUTHORIZED
      contentAsJson(result).as[UnauthorisedErr] shouldBe UnauthorisedErr(message = "fubar")
    }

    "return InternalServerError for unexpected exceptions thrown by auth connector" in {
      unauthorized(new RuntimeException("Unexpected error"))

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = enrolmentAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)        shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe Json.toJson(InternalServerErr())
    }
  }

  "AuthenticatedAuthAction.invokeBlock" should {

    "allow an organisation user without checking enrolments" in {
      stubAffinityGroup(Some(AffinityGroup.Organisation))

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = authenticatedAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Success"

      val predicateCaptor: ArgumentCaptor[Predicate] =
        ArgumentCaptor.forClass(classOf[Predicate])

      val retrievalCaptor: ArgumentCaptor[Retrieval[?]] =
        ArgumentCaptor.forClass(classOf[Retrieval[?]])

      verify(mockAuthConnector).authorise(predicateCaptor.capture(), retrievalCaptor.capture())(any(), any())
      predicateCaptor.getValue shouldBe (AffinityGroup.Organisation or (AffinityGroup.Agent and Enrolment(AgentEnrolmentKey)))
      retrievalCaptor.getValue shouldBe affinityGroup
    }

    "allow an agent without checking for a delegated enrolment" in {
      stubAffinityGroup(Some(AffinityGroup.Agent))

      val result = authenticatedAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Success"
      verify(mockAuthConnector, times(1)).authorise(any(), any())(any(), any())
      verify(mockAuthConnector, never()).authorise(eqTo(agentDelegatedEnrolmentPredicate()), any())(any(), any())
    }

    Seq(Some(AffinityGroup.Individual), None).foreach { maybeAffinityGroup =>
      s"return UNAUTHORISED when auth returns an affinity group of $maybeAffinityGroup" in {
        stubAffinityGroup(maybeAffinityGroup)

        val result = authenticatedAuthAction(validZRef).invokeBlock(FakeRequest(), testBlock)

        status(result)        shouldBe UNAUTHORIZED
        contentAsJson(result) shouldBe Json.toJson(UnauthorisedErr())
      }
    }

    "return UNAUTHORISED for an unauthenticated request" in {
      unauthorized(UnsupportedAuthProvider("fubar"))

      val request = FakeRequest().withHeaders(AUTHORIZATION -> "Bearer abc123")

      val result = authenticatedAuthAction(validZRef).invokeBlock(request, testBlock)

      status(result)                            shouldBe UNAUTHORIZED
      contentAsJson(result).as[UnauthorisedErr] shouldBe UnauthorisedErr(message = "fubar")
    }
  }
}
