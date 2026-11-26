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

package uk.gov.hmrc.disareturnstestsupportapi.controllers.actions

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.{Forbidden, InternalServerError, Unauthorized}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.config.Constants.{AgentEnrolmentKey, DelegatedAuthRule, EnrolmentKey, IdentifierKey}
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.{AgentClientRelationshipNotFound, InternalServerErr, UnauthorisedErr}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AuthAction {
  def apply(zRef: String): ActionBuilder[Request, AnyContent]
}

abstract class BaseAuthAction(ac: AuthConnector, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AuthAction with Logging {

  protected val auth: AuthorisedFunctions = new AuthorisedFunctions {
    override def authConnector: AuthConnector = ac
  }

  protected def authorise[A](zRef: String, request: Request[A], block: Request[A] => Future[Result])(implicit
    hc:                            HeaderCarrier
  ): Future[Result]

  protected def rejectUnsupportedAffinityGroup(): Future[Result] = {
    logger.warn("[AuthAction][authorise] User does not have a supported affinity group")
    rejectUnauthorised()
  }

  protected def rejectUnauthorised(): Future[Result] =
    Future.successful(Unauthorized(Json.toJson(UnauthorisedErr())))

  final override def apply(zRef: String): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser:                     BodyParser[AnyContent] = cc.parsers.defaultBodyParser
      override protected def executionContext: ExecutionContext       = cc.executionContext

      override def invokeBlock[A](
        request: Request[A],
        block:   Request[A] => Future[Result]
      ): Future[Result] = {
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

        authorise(zRef, request, block).recover {
          case ex: AuthorisationException =>
            logger.warn(s"[AuthAction][invokeBlock] Authorization failed. Error: ${ex.reason}")
            Unauthorized(Json.toJson(UnauthorisedErr(message = s"${ex.reason}")))

          case ex =>
            logger.warn(s"[AuthAction][invokeBlock] Auth request failed with unexpected exception: $ex")
            InternalServerError(Json.toJson(InternalServerErr()))
        }
      }
    }
}

@Singleton
class EnrolmentVerificationAuthAction @Inject() (ac: AuthConnector, cc: ControllerComponents, appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends BaseAuthAction(ac, cc) {

  override protected def authorise[A](zRef: String, request: Request[A], block: Request[A] => Future[Result])(implicit
    hc:                                     HeaderCarrier
  ): Future[Result] =
    auth
      .authorised((Organisation and Enrolment(EnrolmentKey)) or (Agent and Enrolment(AgentEnrolmentKey)))
      .retrieve(authorisedEnrolments and affinityGroup) {
        case _ ~ Some(Agent) if !appConfig.agentAuthenticationEnabled =>
          rejectDisabledAgentAuthentication()
        case _ ~ Some(Agent) =>
          authenticateAgent(zRef, request)(block)
        case enrolments ~ Some(Organisation) =>
          authenticateOrganisationUser(zRef, request, enrolments)(block)
        case _ =>
          rejectUnsupportedAffinityGroup()
      }

  private def rejectDisabledAgentAuthentication(): Future[Result] = {
    logger.warn("[EnrolmentVerificationAuthAction][authorise] Agent authentication is disabled")
    rejectUnauthorised()
  }

  private def authenticateAgent[A](zRef: String, request: Request[A])(block: Request[A] => Future[Result])(implicit
    hc:                                  HeaderCarrier
  ): Future[Result] =
    auth
      .authorised(
        Enrolment(EnrolmentKey)
          .withIdentifier(IdentifierKey, zRef)
          .withDelegatedAuthRule(DelegatedAuthRule)
      )(block(request))
      .recover { case _: InsufficientEnrolments =>
        logger.warn(s"[EnrolmentVerificationAuthAction][authenticateAgent] Agent does not have a delegated enrolment for Z-Ref [$zRef]")
        Forbidden(Json.toJson(AgentClientRelationshipNotFound(zRef)))
      }

  private def authenticateOrganisationUser[A](zRef: String, request: Request[A], enrolments: Enrolments)(
    block:                                          Request[A] => Future[Result]
  ): Future[Result] = {
    val zRefMatchesEnrolment = enrolments
      .getEnrolment(EnrolmentKey)
      .fold(false)(_.getIdentifier(IdentifierKey).exists(_.value == zRef))

    if (zRefMatchesEnrolment) block(request)
    else throw InternalError("Z-Ref does not match enrolment.")
  }
}

@Singleton
class AuthenticatedAuthAction @Inject() (ac: AuthConnector, cc: ControllerComponents)(implicit ec: ExecutionContext) extends BaseAuthAction(ac, cc) {

  override protected def authorise[A](zRef: String, request: Request[A], block: Request[A] => Future[Result])(implicit
    hc:                                     HeaderCarrier
  ): Future[Result] =
    auth
      .authorised(Organisation or (Agent and Enrolment(AgentEnrolmentKey)))
      .retrieve(affinityGroup) {
        case Some(Agent) | Some(Organisation) =>
          block(request)
        case _ =>
          rejectUnsupportedAffinityGroup()
      }
}
