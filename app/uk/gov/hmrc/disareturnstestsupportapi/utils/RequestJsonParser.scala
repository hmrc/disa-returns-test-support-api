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

package uk.gov.hmrc.disareturnstestsupportapi.utils

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

import play.api.libs.json._
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.{EmptyPayload, ValidationFailureResponse}

import javax.inject.{Inject, Singleton}

@Singleton
class RequestJsonParser @Inject()(cc: ControllerComponents) {

  def parseJson[T: Reads](request: Request[AnyContent]): Either[Result, T] = {
    request.body.asJson match {
      case None =>
        Left(BadRequest(Json.toJson(EmptyPayload())))
      case Some(js) =>
        js.validate[T].asEither.left.map { errors =>
          val jsErrors = ValidationFailureResponse.createFromJsError(JsError(errors))
          BadRequest(Json.toJson(jsErrors))
        }
    }
  }
}
