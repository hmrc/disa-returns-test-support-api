Use this API to trigger the generation of a test reconciliation report for the ISA Returns API.

It is designed to help you test how your software handles reconciliation results during the asynchronous submission process, including report retrieval and notification handling. The API simulates the behaviour of the live system and supports integration testing in the sandbox environment during the public Beta.

When you call the API, it generates a test reconciliation report with simulated errors. You can specify how many of each error type to include:

- oversubscriptions
- trace and match failures
- eligibility failures

The report is not returned directly. Instead:

- a monthly return summary and reconciliation report are made available to the ISA Returns API
- a push-pull notification (PPNS) is triggered by the ISA Returns API

This API may be withdrawn when the ISA Returns API goes live.

Access is limited to organisations authorised to use the ISA Returns API.

### Request access to the API ###
This is a restricted-access API. Its endpoints are visible only to authorised and subscribed software applications. Because it is a restricted-access API, you must request access before your software application can be subscribed to the API.

### Who can request access ###
To request access, you must:

- be an employee of an organisation listed on the [ISA manager register (GOV.UK)](https://www.gov.uk/government/publications/list-of-individual-savings-account-isa-managers-approved-by-hmrc/registered-individual-savings-account-isa-managers) or
- be part of a third-party organisation with an existing relationship with a listed ISA manager
- have an HMRC Developer Hub account with a registered software application

If you are an ISA manager and your organisation is not listed on the register, please check [how to apply for ISA manager status on GOV.UK](https://www.gov.uk/guidance/apply-to-be-an-isa-manager).

If you are a third-party organisation, HMRC may ask you to provide evidence of your organisation's relationship with the ISA manager to confirm eligibility.

If you do not have a Developer Hub account, you can [register for an account on GOV.UK](https://developer.service.hmrc.gov.uk/developer/registration). The account must use a work email address.

### How to request access ###
Once your Developer Hub account and software application are set up:

1. [Sign in to Developer Hub](https://developer.service.hmrc.gov.uk/developer/login).
2. Return to this API landing page.
3. Go to the Endpoints section and select 'Request access'.
4. Fill in the request form with:
    - your organisation name
    - the API name: ISA Returns Test Support
    - the application ID linked to your Developer Hub software application

HMRC may contact you to discuss your request and confirm eligibility.

If your access is approved, you will receive a confirmation email, and your software application will be subscribed to the API.

If you are not signed in, or access has not yet been granted, the Endpoints section will not display a link. You may see 'Not applicable' or 'Sign in to request access' instead.