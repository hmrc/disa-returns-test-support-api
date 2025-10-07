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

package models.common

import uk.gov.hmrc.disareturnstestsupportapi.models.common.TaxYearValidator
import utils.BaseUnitSpec

class TaxYearValidatorSpec extends BaseUnitSpec {

  "TaxYearValidator.isValid" should {

    "return true for correctly formatted and incremented years" in {
      TaxYearValidator.isValid("2022-23") shouldBe true
    }

    "return false if end year does not equal start + 1" in {
      TaxYearValidator.isValid("2022-24") shouldBe false
    }

    "return false if the format is invalid" in {
      TaxYearValidator.isValid("202324")  shouldBe false
      TaxYearValidator.isValid("2023/24") shouldBe false
      TaxYearValidator.isValid("abcd-ef") shouldBe false
      TaxYearValidator.isValid("1999-00") shouldBe false
    }
  }
}
