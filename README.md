
# disa-returns-test-support-api

This service provides a **testing scaffold** for developers and ISA Managers integrating with the [DISA Returns API](https://github.com/hmrc/disa-returns).

It enables simulation of return submission and reconciliation scenarios that ISA Managers will encounter in production, allowing validation of integrations and error-handling logic before go-live.

It is available in development environments and external test. External test will be the sandbox integration environment for API consumers building applications.

## Available endpoints

### Generate reconciliation report and callback

#### Endpoint summary

This endpoint enables simulation of the scenario where a user has successfully submitted and declared their return, and **NPS** makes a callback to the **DISA Returns API** to indicate the reconciliation report is ready for retrieval.

Note that in the case there are no reconciliation issues, NPS will not generate a report.

Once invoked, this API will make calls to both the main API's NPS callback route to provide a return summary, and also to the stub which will hold the generated reconciliation report.

The link provided in the return summary via the callback can then be called to retrieve the report on the stub, as it would be from NPS.

| Path | Method | Auth          | Purpose                                                                |
|---|---|---------------|------------------------------------------------------------------------|
| `/:zRef/:year/:month/reconciliation` | **POST** | *X-Client-ID* | Simulate NPS → DISA reconciliation report for a given ZREF and period. |


#### Path parameters

| Name | Type | Example | Description | Constraints                              |
|---|---|---:|---|------------------------------------------|
| `zRef` | `string` | `Z1234567` | ISA Manager reference for the return. |           |
| `year` | `string` | `2027` | Tax year of the return being reconciled. | `YYYY-YY` |
| `month` | `string` | `04` | Month of the return being reconciled. | `MM`; `01`–`12` |

#### Request body

The body should be a JSON representation of the following case class:

```scala
case class GenerateReportRequest(
                                  oversubscribed:    Int,
                                  traceAndMatch:     Int,
                                  failedEligibility: Int
                                )
```

## Running the app

```bash
# Run the app locally with service manager
sm2 --start DISA_RETURNS_ALL
```

## Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

## Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source files are correctly formatted
sbt scalafmtCheckAll

# checks all sbt files are correctly formatted
sbt scalafmtSbtCheck

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```

## Viewing the API specifications

*For internal HMRC developers.*

This repository contains API definitions for the DSA Returns API, deployed to the API platform.

To view and test this documentation locally, follow the instructions below.

```zsh
# Run the API platform devhub preview locally with service manager
sm2 -start DEVHUB_PREVIEW_OPENAPI

# Run disa returns locally
sbt run

# Open the API platform devhub preview in your browser
open http://localhost:9680/api-documentation/docs/openapi/preview/
```

From this page, you can enter the fully qualified url of the documentation you wish to view, for example:

```
http://localhost:1200/api/conf/1.0/application.yaml
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").