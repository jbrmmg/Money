# JBR-678 – Opening balance when a date filter is applied

## Objective

When a date range filter is active, `calculateOpeningBalance` in `TransactionReportManager` currently returns `null`, so no opening balance is shown in the transaction list.  It is possible to calculate a meaningful opening balance for a date-filtered result set, and this fix implements that.

## Problem

`calculateOpeningBalance` (line 183) returns `null` early whenever `filter.getDateRange() != null`.  The following early-return conditions must stay in place as the resulting transaction set is not contiguous by date and an opening balance would be misleading: value range, categories, description, predicted=true, fromReconciled.  An account filter does **not** block the opening balance — filtering by account simply restricts which accounts contribute to the sum, and the algorithm handles this naturally since it works from the accounts present in the result set.

## Algorithm

For each account that appears in the filtered result set, find the **Account Earliest Transaction (AET)** — the transaction for that account with the earliest date in the result set.  If multiple transactions share that date, choose the first one returned by the existing sort order (`statementSort`, `date`, `amount`, `accountId`).

For each AET, one of three scenarios applies:

### Scenario 1 – AET is in a locked statement

The AET has a `statementYear`/`statementMonth` and `locked == true`.

1. Retrieve the `Statement` for that account/year/month and read its `openBalance`.
2. Query all transactions in the same statement (same account, year, month) whose date is **strictly before** the AET date, and sum their amounts.
3. The opening balance contribution for this account = `statement.openBalance + sum`.

### Scenario 2 – AET is in an unlocked (current) statement

The AET has a `statementYear`/`statementMonth` and `locked == false`.

Same steps as Scenario 1, using the unlocked statement's `openBalance`.

### Scenario 3 – AET has no statement

The AET has null `statementYear`/`statementMonth`.  Treat it as Scenario 2 using the latest unlocked statement for the account:

1. Retrieve the latest unlocked `Statement` for the account (via `StatementManager.getLatestStatementInternal`).  If none exists, the opening balance contribution for this account is zero.
2. Sum all transactions that belong to that unlocked statement with date **strictly before** the AET date.
3. Also sum all transactions with **no statement** for this account with date **strictly before** the AET date.
4. Opening balance contribution = `latestUnlockedStatement.openBalance + sum`.

### Combining accounts

The overall opening balance is the sum of the per-account contributions across all accounts present in the result set.

## Implementation

### Changes to `calculateOpeningBalance`

Remove the early return for `dateRange`:

```java
// Remove these lines:
if(filter.getDateRange() != null) {
    LOG.debug("No opening balance - date filter");
    return null;
}
```

Both `openingBalanceFromAllAccounts` and `openingBalanceFromAccounts` will be removed and replaced by a single unified method that implements the algorithm above.  The locked-filter special case previously handled by `openingBalanceFromAllAccounts` is absorbed into the per-account algorithm (scenarios 1/2/3 cover locked and unlocked statements uniformly).

### New method – `openingBalanceForAccount`

Add a private method that, given the AET `TransactionReportDTO` and the matching `Account` entity, returns the `BigDecimal` opening balance for that account using the three-scenario logic described above.

This method will:
- Use `statementManager.getStatement(account, month, year)` for scenarios 1 and 2.
- Use `statementManager.getLatestStatementInternal(account)` for scenario 3.
- Query `transactionReportRepository` to obtain the prior-transactions sum in all cases.

### New repository query

Add a method to `TransactionReportRepository` (the internal datasource repository) to retrieve transactions by account, statement (year/month), and a date strictly before a given date.  A separate variant (accepting null year/month) will be needed for the no-statement case (scenario 3).  Prefer named query methods or `@Query` over introducing a new `Specification` to keep the call site simple.

### Unified `calculateOpeningBalance` method

Replace both `openingBalanceFromAccounts` and `openingBalanceFromAllAccounts` with a single new private method that:
1. Iterates the sorted result list to identify the AET per account (first occurrence per account ID — the sort order guarantees this is the earliest transaction for that account).
2. Calls `openingBalanceForAccount` for each AET.
3. Returns the sum across all accounts.

### Affected methods / call sites

- `calculateOpeningBalance` – remove dateRange early-return, replace `openingBalanceFrom*` calls.
- `buildResult` – no change required; it calls `calculateOpeningBalance` unchanged.
- Both `getTransactions` and `getTransactionsPage` benefit automatically via `buildResult`.

## Testing

Add tests to `MoneyReportIT` (integration tests against a real MySQL container) covering:

- With a `from`-date filter: assert the opening balance equals the correct value as at the start date (sum of statement opening balance and prior transactions in that statement).
- With a `to`-date filter only: assert opening balance is still calculated correctly.
- AET in a locked statement (scenario 1): assert opening balance is derived from the locked statement.
- AET in the current (unlocked) statement (scenario 2): assert opening balance is derived from the unlocked statement.
- AET with no statement (scenario 3): assert the latest unlocked statement is used.
- Date filter combined with an account filter: assert opening balance is calculated correctly (only the filtered accounts contribute).
- No date filter: assert existing behaviour is unchanged (regression).
- Date filter combined with a category filter: assert opening balance is still `null` (other early-returns remain in place).
- Date filter combined with a value range filter: assert opening balance is still `null`.
