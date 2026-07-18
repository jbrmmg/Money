#!/usr/bin/env python3
"""
Extract transactions from the Money application REST API and write a CSV.

Output format:
  Date | Description | <account columns...> | Total

  - One column per account (including closed accounts)
  - Amount placed in the matching account column; other columns blank
  - Transfers (transactions with an oppositeId) are excluded
  - First row is the opening balance for each account (from earliest statement)
  - Total column is the cumulative running sum across all accounts
  - Rows are in ascending date order

Usage:
  python extract_transactions.py [options]

  --host    Base URL of the Money API, without /api/v1 (default: http://triggersbroom.jbrmmg.me.uk/money)
  --from    Earliest transaction date to include, yyyy-MM-dd (optional)
  --to      Latest  transaction date to include, yyyy-MM-dd (optional)
  --output  Output CSV filename (default: transactions.csv)
"""

import argparse
import csv
import sys
from datetime import date
from decimal import Decimal

import requests


DEFAULT_HOST = "http://triggersbroom.jbrmmg.me.uk/money"
PAGE_SIZE = 1_000_000  # pagination is not implemented server-side; fetch everything in one request


def signed(financial_amount: dict) -> Decimal:
    """Convert a FinancialAmount JSON object to a signed Decimal.

    The serializer writes the internal signed BigDecimal directly:
      positive value → CR (money in)
      negative value → DB (money out)
    The type field is redundant for our purposes; the value is already signed.
    """
    return Decimal(str(financial_amount["value"]))


def get_accounts(session: requests.Session, base: str) -> list[dict]:
    resp = session.get(f"{base}/accounts")
    resp.raise_for_status()
    return resp.json()


def get_all_statements(session: requests.Session, base: str) -> list[dict]:
    resp = session.get(f"{base}/statement")
    resp.raise_for_status()
    return resp.json()


def get_transactions(
    session: requests.Session,
    base: str,
) -> list[dict]:
    """Fetch every TRANSACTION-type row, excluding transfers and predicted transactions."""
    body: dict = {"maxPageSize": PAGE_SIZE}

    print("  Sending request...", end=" ", flush=True)
    resp = session.post(f"{base}/transaction/list", json=body)
    resp.raise_for_status()
    page_data: list[dict] = resp.json()
    print(f"got {len(page_data)} rows")

    transactions = []
    for row in page_data:
        if row.get("type") != "TRANSACTION":
            continue
        if row.get("predicted"):
            continue
        transactions.append(row)

    return transactions


def earliest_statements(statements: list[dict]) -> dict[str, dict]:
    """Return the earliest statement per accountId, keyed by accountId."""
    result: dict[str, dict] = {}
    for stmt in statements:
        acc_id = stmt["accountId"]
        if acc_id not in result:
            result[acc_id] = stmt
        else:
            existing = result[acc_id]
            if (stmt["year"], stmt["month"]) < (existing["year"], existing["month"]):
                result[acc_id] = stmt
    return result


def build_opening_row(
    account_ids: list[str],
    account_names: dict[str, str],
    earliest: dict[str, dict],
) -> tuple[dict[str, Decimal], str, Decimal]:
    """
    Build the opening balance amounts per account.

    Returns:
        amounts  – {account_id: signed Decimal}
        row_date – ISO date string for the row (first day of earliest statement across all accounts)
        total    – sum of all opening balances
    """
    amounts: dict[str, Decimal] = {}
    earliest_date: date | None = None

    print("\nOpening balances:")
    for acc_id in account_ids:
        stmt = earliest.get(acc_id)
        if stmt is None:
            amounts[acc_id] = Decimal("0")
            print(f"  {account_names[acc_id]:<30}  no statements found, using 0.00")
            continue

        amounts[acc_id] = signed(stmt["openBalance"])
        stmt_date = date(stmt["year"], stmt["month"], 1)
        print(f"  {account_names[acc_id]:<30}  earliest statement: {stmt_date.strftime('%b %Y')}  balance: {amounts[acc_id]:>12.2f}")

        if earliest_date is None or stmt_date < earliest_date:
            earliest_date = stmt_date

    total = sum(amounts.values(), Decimal("0"))
    print(f"  {'Total':<30}  {total:>12.2f}")
    print()
    row_date = earliest_date.isoformat() if earliest_date else ""
    return amounts, row_date, total


def format_amount(value: Decimal | None) -> str:
    if value is None:
        return ""
    return f"{value:.2f}"


def write_html(
    rows: list[dict],
    account_ids: list[str],
    account_names: dict[str, str],
    path: str,
) -> None:
    col_names = [account_names[aid] for aid in account_ids]

    def cell(value: Decimal | None, is_total: bool = False) -> str:
        if value is None or value == Decimal("0"):
            return "<td></td>"
        cls = "pos" if value > 0 else "neg"
        if is_total:
            cls += " total-cell"
        return f'<td class="{cls}">{value:,.2f}</td>'

    header_cells = "".join(f"<th>{c}</th>" for c in col_names)
    html_rows = []
    for row in rows:
        is_ob = row["description"] == "Opening Balance"
        tr_class = ' class="ob"' if is_ob else ""
        desc_class = ' class="transfer"' if row.get("transfer") else ""
        cells = f"<td>{row['date']}</td><td{desc_class}>{row['description']}</td>"
        for aid in account_ids:
            amount = row["amounts"].get(aid)
            cells += cell(amount if amount != Decimal("0") else None)
        cells += cell(row["total"], is_total=True)
        html_rows.append(f"  <tr{tr_class}>{cells}</tr>")

    table_rows = "\n".join(html_rows)

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Transactions</title>
<style>
  body {{ font-family: sans-serif; font-size: 0.85em; margin: 0; padding: 1em; }}
  table {{ border-collapse: collapse; width: 100%; }}
  th, td {{ border: 1px solid #ddd; padding: 4px 8px; white-space: nowrap; }}
  th {{ background: #333; color: #fff; position: sticky; top: 0; }}
  td {{ text-align: left; }}
  td.pos, td.neg, td.total-cell {{ text-align: right; }}
  td.neg {{ color: #c00; }}
  td.transfer {{ color: #080; }}
  td.total-cell {{ font-weight: bold; }}
  tr.ob td {{ background: #f0f4ff; font-style: italic; }}
  tr:nth-child(even):not(.ob) {{ background: #fafafa; }}
  tr:hover {{ background: #fffbcc; }}
</style>
</head>
<body>
<table>
<thead>
  <tr><th>Date</th><th>Description</th>{header_cells}<th>Total</th></tr>
</thead>
<tbody>
{table_rows}
</tbody>
</table>
</body>
</html>"""

    with open(path, "w", encoding="utf-8") as fh:
        fh.write(html)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Export Money application transactions to CSV"
    )
    parser.add_argument(
        "--host", default=DEFAULT_HOST, help="Base URL of the Money API"
    )
    parser.add_argument(
        "--from", dest="from_date", metavar="YYYY-MM-DD",
        help="Only output transactions from this date onwards (opening balance row is omitted)"
    )
    parser.add_argument(
        "--html", dest="html_output", metavar="FILE", help="Also write an HTML version to this file"
    )
    parser.add_argument(
        "--output", default="transactions.csv", help="Output CSV filename"
    )
    args = parser.parse_args()

    base = args.host.rstrip("/")

    with requests.Session() as session:
        print("Fetching accounts...", end=" ", flush=True)
        accounts = get_accounts(session, base)
        accounts.sort(key=lambda a: a["name"])
        account_ids = [a["id"] for a in accounts]

        # If two accounts share the same name, append the ID to disambiguate column headers
        name_counts: dict[str, int] = {}
        for a in accounts:
            name_counts[a["name"]] = name_counts.get(a["name"], 0) + 1
        account_names = {
            a["id"]: (f"{a['name']} ({a['id']})" if name_counts[a["name"]] > 1 else a["name"])
            for a in accounts
        }
        print(f"{len(accounts)} accounts found")

        print("Fetching statements...", end=" ", flush=True)
        statements = get_all_statements(session, base)
        earliest = earliest_statements(statements)
        print(f"{len(statements)} statements found across {len(earliest)} accounts")

        print("Fetching transactions...")
        transactions = get_transactions(session, base)
        print(f"  {len(transactions)} transactions total")

    # Sort transactions by date (API may already return them sorted, but be explicit)
    transactions.sort(key=lambda t: t["date"])

    # Build the opening balance row
    ob_amounts, ob_date, running_total = build_opening_row(account_ids, account_names, earliest)

    rows = []

    if not args.from_date:
        rows.append({
            "date": "",
            "description": "Opening Balance",
            "amounts": ob_amounts,
            "total": running_total,
        })

    for tx in transactions:
        acc_id = tx["account"]["id"]
        amount = signed(tx["amount"])
        is_transfer = tx.get("oppositeId") is not None
        if not is_transfer:
            running_total += amount
        if args.from_date and tx["date"] < args.from_date:
            continue
        rows.append(
            {
                "date": tx["date"],
                "description": tx["description"],
                "amounts": {acc_id: amount},
                "total": running_total,
                "transfer": is_transfer,
            }
        )

    # Write CSV
    col_names = [account_names[aid] for aid in account_ids]
    fieldnames = ["Date", "Description"] + col_names + ["Total"]

    with open(args.output, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()

        for row in rows:
            csv_row: dict[str, str] = {
                "Date": row["date"],
                "Description": row["description"],
                "Total": format_amount(row["total"]),
            }
            for acc_id in account_ids:
                name = account_names[acc_id]
                csv_row[name] = format_amount(row["amounts"].get(acc_id))
            writer.writerow(csv_row)

    print(f"Written {len(rows)} rows (including opening balance) to {args.output}")

    if args.html_output:
        write_html(rows, account_ids, account_names, args.html_output)
        print(f"Written {len(rows)} rows to {args.html_output}")


if __name__ == "__main__":
    main()
