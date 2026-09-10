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

package config

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.actions.{AuthAction, AuthenticatedAuthAction, EnrolmentVerificationAuthAction}
import uk.gov.hmrc.disareturnstestsupportapi.models.validators.{LooseZReferenceValidator, StrictZReferenceValidator, ZReferenceValidator}
import utils.BaseUnitSpec

class ModuleSpec extends BaseUnitSpec {

  "Module" should {
    "bind the enrolment verification auth action when enrolment verification is enabled" in {
      app.injector.instanceOf[AuthAction] shouldBe a[EnrolmentVerificationAuthAction]
    }

    "bind the authenticated auth action when enrolment verification is disabled" in {
      val application = GuiceApplicationBuilder()
        .configure("features.enrolment-verification-enabled" -> false)
        .build()

      try application.injector.instanceOf[AuthAction] shouldBe a[AuthenticatedAuthAction]
      finally await(application.stop())
    }

    "bind the strict Z-reference validator by default" in {
      app.injector.instanceOf[ZReferenceValidator] shouldBe a[StrictZReferenceValidator]
    }

    "bind the loose Z-reference validator when strict validation is disabled" in {
      val application = GuiceApplicationBuilder()
        .configure("features.strict-z-reference-validation-enabled" -> false)
        .build()

      try application.injector.instanceOf[ZReferenceValidator] shouldBe a[LooseZReferenceValidator]
      finally await(application.stop())
    }
  }
}
