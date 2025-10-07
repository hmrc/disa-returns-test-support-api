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

package controllers

import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsString, contentType, route, status, writeableOf_AnyContentAsEmpty}
import utils.BaseIntegrationSpec

class DocumentationControllerISpec extends BaseIntegrationSpec {

  "GET /api/definition" should {
    "return the API definition.json file" in {
      val request = FakeRequest(GET, "/api/definition")
      val result  = route(app, request).get

      status(result)        shouldBe OK
      contentType(result)   shouldBe Some("application/json")
      contentAsString(result) should include("api")
    }
  }

  "GET /api/conf/1.0/application.yaml" should {
    "return the OpenAPI spec file" in {
      val request = FakeRequest(GET, "/api/conf/1.0/application.yaml")
      val result  = route(app, request).get

      status(result)        shouldBe OK
      contentType(result)   shouldBe Some("application/octet-stream")
      contentAsString(result) should include("openapi")
    }
  }
}
