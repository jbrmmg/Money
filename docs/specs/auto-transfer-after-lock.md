# Automatic Transfer on Statement Lock

# Background

Currently, when a credit card statement is locked, the user will then manually create a transfer transaction that represents the credit card being paid off in full.  This typically happens after reconciling the transactions.

# Requirements

## Database

+ Account
  + New items (the default values should be null)
    + Account type - enum stored as a coded string - CreditCard ('CR'), Bank ('BK') - indicates if an account represents a credit card or a bank account.
    + Default source - boolean - a flag that indicates if this account is the default source for transfers
    + Default payment day - int - the day the payments are normally made
    + Weekend adjust - string - this is the same as weekend adjust on the regular payment - says how the payment date should adjust if it's on a weekend.

DTO and associated APIs will need to be updated.

## Functionality

Currently there is a lock statement API which locks the statement and creates the next statement - all that functionality stays the same.  In addition, once the statement is locked, the process should do the following:

+ Create a new transfer transaction, with details as follows:
  + Using the default source account as the 'from', 
  + The credit card account as the 'to', 
  + The amount is the closing balance of the statement being locked (it's the same as the opening balance on the new statement), 
  + The category is Transfer, 
  + The description should be 'Automatic transfer <datetime>', 
  + The date should be derived as follows:
    + Start at the date of the latest transaction that is on the statement being locked.
    + Move the date to the next month with day as default payment day, for example if the latest transaction date is 25 May 2025 and payment day is 13, the date of payment will be 13 Jun 2025.
    + It's possible that the date is not valid (e.g. 31 Jun 2025) - in that scenario move date to the end of the month (30 Jun 2025), i.e. do not roll into the next month.
    + If weekend adjustment is specified AND the date calculated is on a weekend then it will need to be adjusted (see regular payment functionality)

+ Call the reconciliationManager.clearRepositoryData() method - this will reset the reconciliation data.  This should happen regardless of the transfer being created or not.

The transfer transaction should be skipped if any of the following are true:

+ If there is no account marked as default source
+ Account type is not CreditCard (CR) or the payment day is not set for the account being locked. Weekend Adjust is optional, so can be null.
+ If there are no transactions on the statement being locked - this means there is nothing to transfer.

There is a delete statement operation that could potentially delete the statement - there is no requirement to undo the transfer transaction; it will have to be removed manually in that scenario. 

## Testing

There is a test class (StatementTest) which currently contains tests for lock statement. Either update an existing test or create a new test that asserts the transfer transaction and clearing reconciliation data.