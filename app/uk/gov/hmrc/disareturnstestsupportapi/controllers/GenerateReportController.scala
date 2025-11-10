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

package uk.gov.hmrc.disareturnstestsupportapi.controllers

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, ControllerComponents}
import uk.gov.hmrc.disareturnstestsupportapi.connectors.{DisaReturnsCallbackConnector, GenerateReportConnector}
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.common._
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.ErrorResponse.ValidationFailureResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import uk.gov.hmrc.disareturnstestsupportapi.utils.WithJsonBody
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportController @Inject() (
  cc:                      ControllerComponents,
  generateReportConnector: GenerateReportConnector,
  callbackConnector:       DisaReturnsCallbackConnector
)(implicit ec:             ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def generateReport(zRef: String, year: String, month: String): Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      WithJsonBody[GenerateReportRequest](
        req =>
          generateReportConnector
            .generateReport(req, zRef, year, month)
            .flatMap {
              case GenerateReportResult.Success =>
                logger.info(s"[GenerateReportController] Generate Report successful for zRef=$zRef, year=$year, month=$month.")
                callbackConnector
                  .callback(zRef, year, month, req.totalRecords)
                  .map {
                    case CallbackResponse.Success =>
                      logger.info(s"[GenerateReportController] Callback successful for zRef=$zRef, year=$year, month=$month")
                      NoContent
                    case _ =>
                      logger.error(s"[GenerateReportController] Callback failed for zRef=$zRef, year=$year, month=$month")
                      InternalServerError(Json.toJson(InternalServerErr()))
                  }

              case GenerateReportResult.Failure =>
                logger.error(s"[GenerateReportController] Generate Report failed for zRef=$zRef, year=$year, month=$month")
                Future.successful(InternalServerError(Json.toJson(InternalServerErr())))
            }
            .recover { case _ =>
              logger.error(s"[GenerateReportController] Unexpected error during Generate Report for zRef=$zRef, year=$year, month=$month")
              InternalServerError(Json.toJson(InternalServerErr()))
            },
        extraValidation = { _ =>
          val errors: Seq[ErrorResponse] = List(
            Option.unless(IsaRefValidator.isValid(zRef))(InvalidZref),
            Option.unless(TaxYearValidator.isValid(year))(InvalidTaxYear),
            Option.unless(MonthValidator.isValid(month))(InvalidMonth)
          ).flatten

          errors match {
            case Nil              => None
            case Seq(singleError) => Some(singleError)
            case errors           => Some(ValidationFailureResponse.createFromErrorResponses(errors))
          }
        }
      )
    }

}
