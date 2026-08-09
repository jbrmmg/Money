# JBR-697 - Improve the way that report generation works

## Objective

Currently monthly reports are generated at a time when its expected to be complete - the generation happens after 2/3 months and by detecting the absence of the report file.  I would like to make the report generation based more on changes to the underlying data rather than the presence of the file.

## Method

Firstly only look at generating the reports upto just over 18 months old - unless asked to via the API (this is existing behavior).

The process will need to store some information about the report, this information will be as follows:

+ ID of the report (month, year and type - type is monthly or annual, for annual month is null)
+ Total number of transactions in the report.
+ Total number of categories in the report.
+ Sum of all credits in the report.
+ Sum of all debits in the report.
+ MD5 checksum for report data (see MD5 Definition section).
+ Date of last check for this report (informational only).
+ Flag - generated successfully.

This information should be stored in the database - it will not be necessary to access this data outside of the app - there is no requirement for an API to access or modify it.

### Scheduled Report Generation

The scheduler should look at monthly reports starting 18 months ago up to the current month if the day is greater than 15, otherwise the previous month.

The locked status of statements is ignored - reports will be generated as soon as data exists and regenerated if it changes. Early reports may be incomplete but will be updated as data is entered.

For each month in the window, generate the transaction data for the report and then calculate the total number of transactions, total number of categories, sum of all credits, sum of all debits, and the MD5.

If the record for this report is in the database, the data has not changed, and it is flagged as successful then skip this report. Otherwise upsert the record into the table and generate the report. If the report is successfully saved then flag as successful.

If the report was regenerated (data changed or was not previously successful), send the report email.

### Annual Report Generation

An annual report is generated for year Y when all 12 monthly reports for year Y are flagged as successful in the database. This check is independent of the 18-month scheduler window — a monthly report that was generated before the window started still counts toward the annual trigger.

The annual report follows the same upsert and email rules as monthly reports: skip if data unchanged and flagged successful, otherwise regenerate and email.

### API-Triggered Generation

When a report is requested via the API the 18-month window restriction is bypassed, but the change-detection check still applies:

- Compute the current data (transactions, counts, sums, MD5) for the requested report.
- If the data has changed since the last saved record (or no record exists), upsert the record, regenerate the report, and return HTTP 200.
- If the data is unchanged and the report is already flagged successful, return HTTP 200 without regenerating.

## MD5 Definition

The MD5 is computed from the transactions in the report period, serialised as a UTF-8 string and then hashed.

**Sort order:** transactions are sorted ascending by `(date, amount, accountId, categoryId, description)`.

**Row format:** each transaction is serialised as pipe-delimited fields:

```
date|amount|accountId|categoryId|description
```

**Field rules:**
- `date` — formatted as `yyyy-MM-dd`
- `amount` — formatted as a decimal with exactly 2 decimal places, e.g. `-125.00`
- `accountId` — the account ID string
- `categoryId` — the category ID string, or empty string if null
- `description` — the description string, or empty string if null

**Row separator:** rows are joined with `\n`.

The resulting string is hashed with MD5 using UTF-8 encoding.
