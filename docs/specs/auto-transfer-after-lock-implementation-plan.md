# Implementation Plan: Automatic Transfer on Statement Lock

## Context

The three account fields used by this feature (`transfer_account`, `transfer_day`, `weekend_adj`) already exist in the database (added in changelog 017) and on the `Account` entity. They are not yet exposed in `AccountDTO` or the API. No new database migration is required.

---

## Step 1 — AccountDTO

**File:** `src/main/java/com/jbr/middletier/money/dto/AccountDTO.java`

Add three fields. ModelMapper will wire them automatically via the existing `AccountMapper` type maps, provided the names match the entity getters:

```java
private String transferAccountId;     // matches Account.getTransferAccountId()
private Integer transferDay;          // matches Account.getTransferDay()
private AdjustmentType weekendAdj;    // matches Account.getWeekendAdj()
```

No controller or mapper changes are needed — the existing CRUD endpoints and `AccountMapper` will pick these up automatically.

Note: `AccountManager` extends `AbstractManager` and holds an in-memory cache. Changes via the API go through the manager, so the cache stays consistent. No additional cache handling is required.

---

## Step 2 — Extract Weekend Adjustment Utility

**File:** `src/main/java/com/jbr/middletier/money/util/DateAdjustmentUtil.java` (new)

The `adjustDate(LocalDate, AdjustmentType)` method in `RegularCtrl` (lines 36–52) implements the weekend adjustment logic. Extract it into a shared static utility to avoid duplication:

```java
public class DateAdjustmentUtil {
    public static LocalDate adjustForWeekend(LocalDate date, AdjustmentType adjustment) { ... }
}
```

Update `RegularCtrl.adjustDate()` to delegate to this utility.

---

## Step 3 — AccountTransactionManager — Latest Transaction Date

**File:** `src/main/java/com/jbr/middletier/money/manager/AccountTransactionManager.java`

Add a method to retrieve the latest transaction date for a statement. This sits alongside the existing `getFinalBalanceForStatement()` method which already queries transactions for a statement:

```java
public Optional<LocalDate> getLatestTransactionDateForStatement(Statement statement)
```

Returns `Optional.empty()` if the statement has no transactions (which triggers the skip condition in StatementManager).

---

## Step 4 — StatementManager

**File:** `src/main/java/com/jbr/middletier/money/manager/StatementManager.java`

### 4a — Inject ReconciliationManager

Add `ReconciliationManager` to the constructor. `AccountTransactionManager` is already passed as a parameter to `statementLock()` to avoid a circular dependency — keep that pattern.

```java
private final ReconciliationManager reconciliationManager;

@Autowired
public StatementManager(..., ReconciliationManager reconciliationManager) {
    ...
    this.reconciliationManager = reconciliationManager;
}
```

### 4b — Payment date calculation (private helper)

```java
private LocalDate derivePaymentDate(LocalDate latestTransactionDate, Account account)
```

Logic:
1. Move to the next month using `YearMonth` arithmetic, using `account.getTransferDay()` as the target day:
   ```java
   YearMonth nextMonth = YearMonth.from(latestTransactionDate).plusMonths(1);
   int day = Math.min(account.getTransferDay(), nextMonth.lengthOfMonth());
   LocalDate paymentDate = nextMonth.atDay(day);
   ```
   The `Math.min` clamp handles invalid dates (e.g. 31 June → 30 June) without rolling into the next month.
2. If `account.getWeekendAdj()` is not null, call `DateAdjustmentUtil.adjustForWeekend(paymentDate, account.getWeekendAdj())`.
3. Return the resulting date.

### 4c — Transfer creation (private helper)

```java
private void createAutoTransfer(Statement lockedStatement,
                                 FinancialAmount balance,
                                 AccountTransactionManager accountTransactionManager)
```

**Skip if any of the following are true** (return early):
- `lockedStatement`'s account has no `transferAccountId` (null)
- `lockedStatement`'s account has no `transferDay` (null)
- `accountTransactionManager.getLatestTransactionDateForStatement(lockedStatement)` returns empty

**If not skipped:**
1. Look up the Transfer Account from `AccountManager` using the `transferAccountId`.
2. Call `derivePaymentDate()` with the latest transaction date and the locked account.
3. Build two `TransactionDTO` objects:
   - **from**: account = Transfer Account, amount = `balance`, category = `"TRF"`, date = derived date, description = `"Automatic transfer " + LocalDateTime.now()`
   - **to**: account = locked statement's account, amount = `balance`, category = `"TRF"`, date = derived date, description = same
4. Call `accountTransactionManager.createTransaction(List.of(fromDto, toDto))`.

(`createTransaction` automatically negates the "to" amount and links the pair via `oppositeTransactionId`.)

### 4d — Update `statementLock()`

After saving the locked and new statements and publishing `StatementLockEvent` (current lines 108–110), add:

```java
createAutoTransfer(statement.get(), balance, accountTransactionManager);
reconciliationManager.clearRepositoryData();
```

The reconciliation clear runs unconditionally per the spec.

---

## Step 5 — Integration Test

**File:** `src/test/java/com/jbr/middletier/money/StatementTest.java`

Add a new test method (or extend `testLockStatement()`):

1. Set up a credit card account with `transferAccountId` pointing to a bank account, and `transferDay` set to a specific day.
2. Add transactions to the credit card statement.
3. Call the lock statement API.
4. Assert:
   - A transfer transaction pair exists with the correct amount, date (next month at transfer day), description (`"Automatic transfer ..."`), and linked `oppositeTransactionId` fields.
   - The reconciliation repository is empty.

Add a second scenario for each skip condition (no transfer account, no transfer day, no transactions) and assert no transfer transaction is created in each case. The reconciliation data should still be cleared in all cases.

Seed data for test accounts may need to be added to `src/main/resources/db/changelog/dbg-it/`.

---

## Summary of Files Changed

| File | Change |
|------|--------|
| `dto/AccountDTO.java` | Add 3 fields (`transferAccountId`, `transferDay`, `weekendAdj`) |
| `util/DateAdjustmentUtil.java` | New shared utility — extract from `RegularCtrl` |
| `schedule/RegularCtrl.java` | Delegate `adjustDate()` to `DateAdjustmentUtil` |
| `manager/AccountTransactionManager.java` | Add `getLatestTransactionDateForStatement()` |
| `manager/StatementManager.java` | Inject `ReconciliationManager`; add date calc + transfer creation; update `statementLock()` |
| `StatementTest.java` | New integration test scenarios |
| `db/changelog/dbg-it/` | Test seed data (if needed) |
