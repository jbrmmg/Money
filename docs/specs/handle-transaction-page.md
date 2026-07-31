# JBR-684 handle paging in the transaction list API

## Objective

Currently, the server limits the number of transactions returned in the transaction list API to the value specified in `maxPageSize`.  This should be changed to be more of a page model - the client will provide the page size and page number and the server needs to indicate the total number of transactions that meet the filter.

## Requirement

A new endpoint will be added at `/api/v1/transaction/list/page`, extending the `/api/v1/transaction/list` endpoint.  `pageNumber` and `maxPageSize` are already in the body of the existing API and so will be in the new API.

The new endpoint will return a new DTO which contains the following properties:

+ totalCount - the total number of transactions.
+ pageNumber - the page number being returned.
+ maxPageSize - the page size.
+ transactions - this is the same as the list returned by the existing endpoint.

The change should as far as possible re-use the implementation currently in the getTransactions method in TransactionReportManager.

At the moment the system reads the first n transactions that meet the criteria where n is the page size provided by the client.  The system then calculates the start balance, today's balance and the future balance (if there are transactions in the future).

If the page number provided is less than 1, then assume page 1 (the first page). If the page number provided is greater than the maximum number of pages calculated from page size and number of transactions, then assume the last page.  This is the page number that should be returned in the response.

The system then puts a running balance on each transaction.

Two things are needed:

+ Use the page number provided to get the sub-range of transactions.
+ Return the additional information: totalCount, pageNumber and maxPageSize.

## Testing

Add tests to check the functionality is correct. Tests should be added to MoneyReportIT.  Testing should cover:

+ specifying a page number less than 1 and assert that page 1 is returned.
+ specifying a page number greater than the calculated maximum, assert that the last page is returned.
+ requesting page 1 and asserting only page 1 is returned, assert that the balances are correct.
+ requesting page 2 and asserting only page 2 is returned, assert that the balances are correct.

## Suggestion

A suggested solution is to remove the current break from the transaction loop and generate the data for the whole transaction set - then determine the transactions that are in the requested page.  Ensure that the opening and closing balances (and future balance if required) match the page that is being returned.  The implementation for the current endpoint is to return page 1.


