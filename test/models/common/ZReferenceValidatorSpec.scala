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

import uk.gov.hmrc.disareturnstestsupportapi.models.validators.{LooseZReferenceValidator, StrictZReferenceValidator}
import utils.BaseUnitSpec
import utils.TestConstants.validZReference

class ZReferenceValidatorSpec extends BaseUnitSpec {

  private val strictValidator = new StrictZReferenceValidator
  private val looseValidator  = new LooseZReferenceValidator

  "ZReferenceValidator.isValid" should {

    "return true for valid ZRef format" in {
      strictValidator.isValid(validZReference) shouldBe true
      strictValidator.isValid("Z0000")         shouldBe true
      strictValidator.isValid("Z9999")         shouldBe true
      strictValidator.isValid("z1234")         shouldBe true
    }

    "return false for missing Z prefix" in {
      strictValidator.isValid("1234")  shouldBe false
      strictValidator.isValid("A1234") shouldBe false
      strictValidator.isValid("|1234") shouldBe false
    }

    "return false for incorrect length" in {
      strictValidator.isValid("Z123")   shouldBe false
      strictValidator.isValid("Z12345") shouldBe false
    }

    "return false for non-numeric suffix" in {
      strictValidator.isValid("Z12A4") shouldBe false
      strictValidator.isValid("Zabcd") shouldBe false
    }

    "return false for empty" in {
      strictValidator.isValid("") shouldBe false
    }

    "accept four to eight digits when strict validation is disabled" in {
      looseValidator.isValid("Z1234")      shouldBe true
      looseValidator.isValid("Z12345")     shouldBe true
      looseValidator.isValid("Z12345678")  shouldBe true
      looseValidator.isValid("Z123")       shouldBe false
      looseValidator.isValid("Z123456789") shouldBe false
    }
  }
}
