# ISA Returns Test Support API Changelog

All notable changes to this API will be documented in this file. Learn about API features, fixes, deprecations and documentation changes.

## 24 November 2026

### Updates

- Added agent support. Agents can now generate test reconciliation reports and override the reporting window for an ISA manager they are authorised to act for.
- Added new error response `AGENT_CLIENT_RELATIONSHIP_NOT_FOUND` (403). It is returned when an agent is not authorised to act for the ISA manager with the requested Z-reference.

### What impact does this have?

- Agents must have an authorised relationship with the ISA manager for the requested Z-reference before calling the generate test reconciliation report or reporting window override endpoints.
- Consumers should handle the new `AGENT_CLIENT_RELATIONSHIP_NOT_FOUND` error code in their error handling logic.

## 1 July 2026

### Updates

- The generate reconciliation report endpoint now validates request bodies using a strict JSON parser. Empty or whitespace-only payloads return the existing `EMPTY_PAYLOAD` error response.
- Added new error response `MALFORMED_JSON` (400) returned when the request body contains invalid JSON.
- Updated request body validation failures to return `VALIDATION_FAILURE` (400) with field-specific errors. Each field error includes a `code`, `message` and JSON pointer `path`.
- Added new field-level validation error codes `MISSING_FIELD` and `VALIDATION_ERROR`.
- Updated the OpenAPI schema to document field-specific validation errors for the generate reconciliation report endpoint.

### What impact does this have?

- Consumers must send a valid JSON request body to the generate reconciliation report endpoint.
- Consumers should handle the new `MALFORMED_JSON`, `VALIDATION_FAILURE`, `MISSING_FIELD` and `VALIDATION_ERROR` error codes.
- Consumers currently parsing validation errors from the previous `issues` response field must update their error handling to use the new `errors` array.
