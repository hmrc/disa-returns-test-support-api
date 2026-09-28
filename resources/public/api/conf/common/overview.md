`The ISA Returns Test Support API simulates the behaviour of the live service to account for the generation of the reconciliation report.

It provides support to:

- initiate the test reconciliation report in the sandbox environment
- configure the start and end date of the reporting window in the sandbox environment to test the reporting window behaviour during the Monthly return submission 

This allows you to validate your integration and application behaviour before deploying against the production environment.

## Reconciliation report handling

The reconciliation report handling process is divided into:

- test report generation
- post-report generation procedures

### Test report generation

Based on the request being submitted to the ISA Returns Test Support API, it generates a test reconciliation report 
containing configurable simulated errors. Each sample reconciliation report has an upper threshold of 2000 simulated 
errors per report. The API request allows you to specify the number of occurrences for each of the following supported 
error types:

- oversubscription error – indicates a situation where an investor has over-subscribed beyond the HMRC acceptable ISA 
- investment limit through various banks / financial institutions
- trace and match failures – indicates a situation where the details of an investor submitted through various banks / 
- financial institutions is different compared to the details already available on the backend database
- eligibility failures – indicates a situation where an investor is non-eligible for a specific ISA product due to 
- strict eligibility criteria such as age restrictions or nationality etc

The generated reconciliation report can be retrieved through a separate ISA Returns API call and is not included in the 
API response.

### Post-report generation

Once the reconciliation report is generated:

- though the reconciliation report is available – it will not be returned automatically
- a notification is triggered via the Push Pull Notifications Service (PPNS) to notify your application that the 
- reconciliation report results are available

Your application can then use the relevant endpoints to retrieve and process the generated results.

This allows you to test the complete asynchronous submission and notification flow before integrating with the live 
service.

## Getting started

The ISA Returns Test Support API is a controlled access API and its endpoints are available only to authorised software 
applications that are subscribed to the API. You may start using the ISA Returns Test Support API by:

- [registering your application on the HMRC Developer Hub](#register-your-application)
- [requesting access to the API](#request-access-to-the-api)
- [subscribing your application to the API](#subscribe-your-application-to-api)

Once the above steps are completed, you can configure the authentication and use the correct headers to trigger test 
reconciliation reports and to test the end-to-end integration of your application with the ISA Returns Test Support API.

### Register your application

Register a sandbox application on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/). 
This generates a client ID and secret for use in the sandbox environment.

You will use this application to:

- subscribe to the sandbox version of the API
- test integration and validate your request and response handling
- simulate submission flows using test users

Use the sandbox base URL in your application: 
[https://test-api.service.hmrc.gov.uk](https://test-api.service.hmrc.gov.uk).

You must also create one or more test Government Gateway user IDs to authenticate against user-restricted endpoints. 
These can be created on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/).

For more information, see [Testing in the sandbox](https://developer.service.hmrc.gov.uk/api-documentation/docs/testing).

### Request access to the API

You can request access if you have a HMRC Developer Hub account with a registered software application and satisfy 
either of the following:

- you are an employee of an organisation listed on the 
[ISA manager register (GOV.UK)]](https://www.gov.uk/government/publications/list-of-individual-savings-account-isa-managers-approved-by-hmrc/registered-individual-savings-account-isa-managers)
- you are part of a third-party organisation with an existing relationship with a listed ISA manager

If you are an ISA manager and your organisation is not listed on the register, check the relevant 
[registration guidance](https://www.gov.uk/guidance/apply-to-be-an-isa-manager).

If you are a third-party organisation, HMRC may request evidence of your organisation’s relationship with the ISA manager 
to verify and confirm your eligibility.

If you do not have a HMRC Developer Hub account, you must 
[register for an account](https://developer.service.hmrc.gov.uk/developer/registration) prior to requesting access to 
the ISA Returns Test Support API. Your account must be registered using an official organisation email address.

#### How to request access

Prior to requesting access to the ISA Returns Test Support API, make sure that:

- you have a [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/) account
- your software application is registered
- you have the application ID for the software application

To request access:

1. [Sign in](https://developer.service.hmrc.gov.uk/developer/login) to your HMRC Developer Hub account.
2. Return to the **ISA Returns Test Support API** landing page.
3. In the **Endpoints** section, select **Request access**.
4. Complete the access request form with:
    - your organisation name
    - **API name**: ISA Returns Test Support
    - the application ID associated with your Developer Hub software application
5. Submit the request.

HMRC may contact you for additional information to verify your organisation and eligibility.

### Subscribe your application to API

If your request is approved, you will receive a confirmation email, and your software application will be subscribed to 
the ISA Returns Test Support API.

If you are not signed in, or access has not yet been granted, the **Endpoints** section will not display a link. You may 
see **Not applicable** or **Sign in to request access** instead.

If you are not familiar with subscriptions or API visibility, see the 
[Reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide#api-access).

Configure authentication

The ISA Returns Test Support API uses OAuth 2.0 with user-restricted scopes. In the sandbox environment, you must:

- create a test Government Gateway user ID on the Developer Hub
- use the OAuth 2.0 grant flow to obtain a test access token
- include the token in the Authorization header of each request to the API

Tokens expire after 4 hours and must then be refreshed.

Test failure scenarios such as a user not granting authority during the OAuth flow. This ensures your application 
handles all outcomes correctly.

For full guidance, see the [Authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation) 
guide.

### Use the correct headers

All API requests must include:

- an Accept header to specify the API version (for example, application/vnd.hmrc.1.0+json)
- a valid Authorization header with your bearer token

Each API call must include the Z-Reference assigned to the specific ISA manager.

### Testing related endpoint visibility

The **Request access** option is displayed only when you are eligible to request access.

If you are not signed in, or your software application has not yet been granted access, the **Endpoints** section may 
display:

- **Sign in to request access**, if you need to authenticate
- **Not applicable**, if access cannot currently be requested

Once access has been approved and your application has been subscribed, the API endpoints will become available to your 
application.

**Note**

The sandbox data and responses are intended exclusively for testing purposes.
