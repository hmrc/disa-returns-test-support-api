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

package uk.gov.hmrc.disareturnstestsupportapi.connectors

import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.disareturnstestsupportapi.config.Service
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportConnector @Inject() (config: Configuration, httpClient: HttpClientV2)(implicit ec: ExecutionContext) {

  private val baseUrl: Service = config.get[Service]("microservice.services.disa-returns-stubs")
  private val token:   String  = config.get[String]("microservice.services.disa-returns-stubs.authorization-token")

  def generate(
    body:        GenerateReportRequest,
    zref:        String,
    year:        String,
    month:       String
  )(implicit hc: HeaderCarrier): Future[GenerateReportResult] = {

    val url = url"$baseUrl/test-only/$zref/$year/$month/reconciliation"
    httpClient
      .post(url)
      .withBody(Json.toJson(body))
      .setHeader("Authorization" -> s"Bearer $token")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 204 => GenerateReportResult.Success
          case _ => GenerateReportResult.Failure
        }
      }
  }

}
