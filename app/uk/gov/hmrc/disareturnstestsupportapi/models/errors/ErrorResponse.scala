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

package uk.gov.hmrc.disareturnstestsupportapi.models.errors

import play.api.libs.json._

trait ErrorResponse {
  def code: String

  def message: String
}

case object InvalidZref extends ErrorResponse {
  val code = "zRef"
  val message = "ZReference did not match expected format"
}

case object InvalidTaxYear extends ErrorResponse {
  val code = "taxYear"
  val message = "Invalid parameter for tax year"
}

case object InvalidMonth extends ErrorResponse {
  val code = "month"
  val message = "Invalid parameter for month"
}

case class InternalServerErr(
                              code: String = "INTERNAL_SERVER_ERROR",
                              message: String = "There has been an issue processing your request"
                            )

object InternalServerErr {
  implicit val format: OFormat[InternalServerErr] = Json.format[InternalServerErr]
}

case class UnauthorisedErr(code: String = "UNAUTHORIZED",
                           message: String = "Unauthorized")

object UnauthorisedErr {
  implicit val format: OFormat[UnauthorisedErr] = Json.format[UnauthorisedErr]
}

case class ValidationFailureResponse(
                                      code: String = "BAD_REQUEST",
                                      message: String = "Issue(s) with your request",
                                      issues: Seq[Map[String, String]]
                                    )

object ValidationFailureResponse {
  implicit val responseFormat: OFormat[ValidationFailureResponse] = Json.format[ValidationFailureResponse]

  private def formatFieldPath(jsPath: JsPath): String =
    jsPath.path
      .map {
        case KeyPathNode(key) => key
        case IdxPathNode(idx) => idx.toString
      }
      .mkString(".")

  private def mapJsErrorMessage(message: String): String = message match {
    case "error.min" => "This field must be greater than or equal to 0"
    case _ => "This field is required"
  }

  def createFromJsError(jsError: JsError): ValidationFailureResponse = {
    val issues: Seq[Map[String, String]] = jsError.errors.toSeq.flatMap { case (path, errors) =>
      errors.map { validationError =>
        Map(formatFieldPath(path) -> mapJsErrorMessage(validationError.message))
      }
    }

    ValidationFailureResponse(issues = issues)
  }

  def createFromErrorResponses(errors: Seq[ErrorResponse]): ValidationFailureResponse = {
    val issues: Seq[Map[String, String]] = errors.map { err =>
      Map(err.code -> err.message)
    }

    ValidationFailureResponse(issues = issues)
  }

}
