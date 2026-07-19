#!/usr/bin/env python3
"""
Flask web app for viewing Money application transactions.

Usage:
  python transactions_web.py [--host URL] [--port PORT]

Then open http://localhost:5001 in your browser.
"""
import argparse
from datetime import date
from decimal import Decimal

import requests
from flask import Flask, jsonify, render_template, request
from werkzeug.middleware.proxy_fix import ProxyFix

DEFAULT_HOST = "http://triggersbroom.jbrmmg.me.uk/money"
PAGE_SIZE = 1_000_000

app = Flask(__name__)
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_prefix=1)

import os
_money_base: str = os.environ.get("MONEY_API_HOST", DEFAULT_HOST).rstrip("/")


def signed(financial_amount: dict) -> Decimal:
    return Decimal(str(financial_amount["value"]))


def fetch_accounts(session: requests.Session) -> list[dict]:
    resp = session.get(f"{_money_base}/accounts")
    resp.raise_for_status()
    return resp.json()


def fetch_statements(session: requests.Session) -> list[dict]:
    resp = session.get(f"{_money_base}/statement")
    resp.raise_for_status()
    return resp.json()


def fetch_transactions(session: requests.Session) -> list[dict]:
    resp = session.post(f"{_money_base}/transaction/list", json={"maxPageSize": PAGE_SIZE})
    resp.raise_for_status()
    return [
        row for row in resp.json()
        if row.get("type") == "TRANSACTION" and not row.get("predicted")
    ]


def earliest_statements(statements: list[dict]) -> dict[str, dict]:
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


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/transactions")
def api_transactions():
    from_date = request.args.get("from", "")

    try:
        with requests.Session() as session:
            accounts = fetch_accounts(session)
            accounts.sort(key=lambda a: a["name"])
            account_ids = [a["id"] for a in accounts]

            name_counts: dict[str, int] = {}
            for a in accounts:
                name_counts[a["name"]] = name_counts.get(a["name"], 0) + 1
            account_names = {
                a["id"]: (
                    f"{a['name']} ({a['id']})"
                    if name_counts[a["name"]] > 1
                    else a["name"]
                )
                for a in accounts
            }

            statements = fetch_statements(session)
            earliest = earliest_statements(statements)
            transactions = fetch_transactions(session)

    except requests.RequestException as exc:
        return jsonify({"error": str(exc)}), 502

    transactions.sort(key=lambda t: t["date"])

    ob_amounts: dict[str, Decimal] = {}
    ob_date_val: date | None = None
    for acc_id in account_ids:
        stmt = earliest.get(acc_id)
        if stmt is None:
            ob_amounts[acc_id] = Decimal("0")
        else:
            ob_amounts[acc_id] = signed(stmt["openBalance"])
            stmt_date = date(stmt["year"], stmt["month"], 1)
            if ob_date_val is None or stmt_date < ob_date_val:
                ob_date_val = stmt_date

    running_total = sum(ob_amounts.values(), Decimal("0"))
    chart_balances: dict[str, Decimal] = dict(ob_amounts)
    rows: list[dict] = []

    if not from_date:
        rows.append({
            "date": "",
            "description": "Opening Balance",
            "amounts": {k: float(v) for k, v in ob_amounts.items()},
            "total": float(running_total),
            "transfer": False,
            "chart_balances": {k: float(v) for k, v in chart_balances.items()},
            "is_ob": True,
        })

    for tx in transactions:
        acc_id = tx["account"]["id"]
        amount = signed(tx["amount"])
        is_transfer = tx.get("oppositeId") is not None
        if not is_transfer:
            running_total += amount
        chart_balances[acc_id] = chart_balances.get(acc_id, Decimal("0")) + amount
        if from_date and tx["date"] < from_date:
            continue
        rows.append({
            "date": tx["date"],
            "description": tx["description"],
            "amounts": {acc_id: float(amount)},
            "total": float(running_total),
            "transfer": is_transfer,
            "chart_balances": {k: float(v) for k, v in chart_balances.items()},
            "is_ob": False,
        })

    return jsonify({
        "accounts": [{"id": aid, "name": account_names[aid]} for aid in account_ids],
        "rows": rows,
    })


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Money transactions web viewer")
    parser.add_argument(
        "--host", default=DEFAULT_HOST,
        help="Base URL of the Money API (default: %(default)s)",
    )
    parser.add_argument(
        "--port", type=int, default=8081,
        help="Port to listen on (default: %(default)s)",
    )
    args = parser.parse_args()
    _money_base = args.host.rstrip("/")
    app.run(debug=True, host="0.0.0.0", port=args.port)
