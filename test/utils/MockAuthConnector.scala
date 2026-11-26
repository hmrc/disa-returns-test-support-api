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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.disareturnstestsupportapi.config.Constants.{DelegatedAuthRule, EnrolmentKey, IdentifierKey}

import scala.concurrent.Future

trait MockAuthConnector extends TestData {

  type EnrolmentRetrievals = Enrolments ~ Option[AffinityGroup]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def stubEnrolments(
    enrolmentKey:  String = EnrolmentKey,
    identifierKey: Option[String] = Some(IdentifierKey),
    zRef:          String = validZRef,
    state:         String = "Activated",
    affinityGroup: Option[AffinityGroup] = Some(AffinityGroup.Organisation)
  ): Unit = {
    val enrolment = Enrolments(
      Set(
        Enrolment(
          key = enrolmentKey,
          identifiers = identifierKey.fold(Seq.empty[EnrolmentIdentifier])(key => Seq(EnrolmentIdentifier(key, zRef))),
          state = state
        )
      )
    )

    when(mockAuthConnector.authorise[EnrolmentRetrievals](any(), any())(any(), any()))
      .thenReturn(Future.successful(new ~(enrolment, affinityGroup)))
  }

  def stubAffinityGroup(affinityGroup: Option[AffinityGroup]): Unit =
    when(mockAuthConnector.authorise[Option[AffinityGroup]](any(), any())(any(), any()))
      .thenReturn(Future.successful(affinityGroup))

  def agentDelegatedEnrolmentPredicate(zRef: String = validZRef): Enrolment =
    Enrolment(EnrolmentKey).withIdentifier(IdentifierKey, zRef).withDelegatedAuthRule(DelegatedAuthRule)

  def stubAgentDelegatedEnrolmentSuccess(zRef: String = validZRef): Unit =
    when(mockAuthConnector.authorise[Unit](eqTo(agentDelegatedEnrolmentPredicate(zRef)), any())(any(), any()))
      .thenReturn(Future.unit)

  def stubAgentDelegatedEnrolmentFailure(authException: Exception = InsufficientEnrolments(), zRef: String = validZRef): Unit =
    when(mockAuthConnector.authorise[Unit](eqTo(agentDelegatedEnrolmentPredicate(zRef)), any())(any(), any()))
      .thenReturn(Future.failed(authException))

  def authorizationForZRef(zRef: String = validZRef): Unit =
    stubEnrolments(zRef = zRef)

  def unauthorized(authException: Exception = InsufficientEnrolments("")): Unit =
    when(mockAuthConnector.authorise[Unit](any(), any())(any(), any()))
      .thenReturn(Future.failed(authException))
}
