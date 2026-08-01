# JBR-681 – Chart/Graph Dialog

## Objective

Add a "Chart" button to the toolbar that opens a modal dialog showing preset charts based on the transaction data currently visible in the list. Two charts are provided initially:

1. **Spending by Category** (pie chart) — net-negative categories only, as absolute values
2. **Account Balance** (line graph) — per-account running balance and overall running balance over time

The dialog opens with the pie chart displayed by default; the user can switch to the line graph.

## Charting Library

Install **ng2-charts** (Angular wrapper around Chart.js):

```bash
npm install ng2-charts chart.js
```

Chart.js components must be registered once (e.g., in `app.module.ts` or the chart component itself) using `Chart.register(...)` from `chart.js/auto` or by registering individual controllers/scales.

## Data Scope

Charts are derived from the `ITransactionReport[]` data currently held by `GridTransaction` (i.e., the currently loaded page). A future enhancement could load all pages or use a dedicated API endpoint; for this ticket client-side computation from the current page is sufficient.

## New Files

| Path | Purpose |
|---|---|
| `src/app/money/chart/chart-dialog.component.ts` | Standalone chart dialog component |
| `src/app/money/chart/chart-dialog.component.html` | Dialog template |
| `src/app/money/chart/chart-dialog.component.css` | Dialog styles |

## Toolbar Button

### `money-toolbar.component.ts`

Add a new `@Output`:

```typescript
@Output() chartClick = new EventEmitter<void>();
```

And handler:

```typescript
onChart() { this.chartClick.emit(); }
```

### `money-toolbar.component.html`

Add a button after the Export button, before Rec. File, in the first toolbar group:

```html
<button class="btn btn-secondary toolbar-btn" title="View charts" (click)="onChart()">
    <em class="fa fa-bar-chart"></em>
    <span class="btn-label">Chart</span>
</button>
```

## Wiring: `money.component`

### `money.component.html`

Add `(chartClick)="onChart()"` to the `<jbr-money-toolbar>` element.

### `money.component.ts`

```typescript
onChart(): void {
    this.grid.openChartModal();
}
```

`this.grid` is already available via the existing `@ViewChild(GridTransaction)`.

## Chart Modal in `GridTransaction`

### `grid-transaction.ts`

- Add `@ViewChild('templateChart') private templateChart: TemplateRef<any>`.
- Add `chartInputs: Record<string, unknown>` to pass transaction data to the dialog.
- Add a public method:

```typescript
openChartModal(): void {
    this.chartInputs = { data: this.data ?? [] };
    this.modalRef = this._modalService.show(this.templateChart, { class: 'modal-xl' });
}
```

- Add `ChartDialogComponent` to the component's `imports` array.

### `grid-transaction.html`

Add a modal template near the existing `templateRecFile` template:

```html
<ng-template #templateChart>
    <jbr-chart-dialog [data]="chartInputs?.data" (close)="modalRef?.hide()"></jbr-chart-dialog>
</ng-template>
```

## `ChartDialogComponent`

### Inputs / Outputs

```typescript
@Input() data: ITransactionReport[] = [];
@Output() close = new EventEmitter<void>();
```

### Internal State

```typescript
chartType: 'pie' | 'line' = 'pie';
```

### Pie Chart — Data Computation

Filter `data` to rows where `type === TransactionReport.TRANSACTION`.

Group by `category?.id` (use `'__none__'` as key when `category` is null):

```
netByCategory: Map<string, { label: string, net: number }>
```

For each transaction, interpret the amount:
- `amount.type === 'DB'` → subtract `amount.value` from the category net (debit/spending)
- `amount.type === 'CR'` → add `amount.value` to the category net (income)

Keep only entries where `net < 0` (net spending).  Segment values are `Math.abs(net)`.  Labels are the category description, or `'Uncategorised'` when category is null.

Chart.js dataset: type `'pie'`, one dataset, N data points. Assign distinct colours per segment.

### Line Graph — Data Computation

Filter `data` to rows where `type === TransactionReport.TRANSACTION`, sorted by the existing sort order (already sorted in the result).

**Overall balance line**: for each transaction row in order, use `balance.value` as the Y value and `date` as the X label.  This reflects the combined running balance across all accounts, computed by the backend.

**Per-account balance lines**: for each distinct `account.id` present in the data:
1. Walk through all TRANSACTION rows in order.
2. Maintain a running total starting at `0`.
3. When a row belongs to this account, add its amount (positive for CR, negative for DB) to the running total and record a point at that date.
4. Between dates where this account has no transaction, carry forward the last known running total.

Note: per-account lines show **net change relative to the start of the visible data**, not absolute account balances.  Absolute per-account balances would require a new backend endpoint and are deferred to a future ticket.

Collect all unique dates (X-axis labels) from the data in order.  Each dataset has one point per date.

### Dialog Layout

```
+---------------------------------------------------+
| [Charts]                               [×  Close] |
+---------------------------------------------------+
| [Spending by Category]  [Account Balance]         |
|                                                   |
|                   <chart canvas>                  |
|                                                   |
+---------------------------------------------------+
|                              [Close]              |
+---------------------------------------------------+
```

- Title: "Charts"
- Toggle buttons (Bootstrap `btn-group`): "Spending by Category" | "Account Balance"
- Active toggle uses `btn-primary`; inactive uses `btn-outline-secondary`
- Chart canvas sized to fill available dialog width
- `modal-xl` class so the chart has enough horizontal space
- Footer "Close" button emits the `(close)` output

### Template Skeleton

```html
<div class="modal-header">
    <h5 class="modal-title">Charts</h5>
    <button type="button" class="btn-close" (click)="close.emit()"></button>
</div>
<div class="modal-body">
    <div class="btn-group mb-3">
        <button [class]="chartType==='pie' ? 'btn btn-primary' : 'btn btn-outline-secondary'"
                (click)="chartType='pie'">Spending by Category</button>
        <button [class]="chartType==='line' ? 'btn btn-primary' : 'btn btn-outline-secondary'"
                (click)="chartType='line'">Account Balance</button>
    </div>
    <canvas baseChart
            *ngIf="chartType==='pie'"
            [data]="pieChartData"
            [type]="'pie'"
            [options]="pieChartOptions">
    </canvas>
    <canvas baseChart
            *ngIf="chartType==='line'"
            [data]="lineChartData"
            [type]="'line'"
            [options]="lineChartOptions">
    </canvas>
</div>
<div class="modal-footer">
    <button class="btn btn-secondary" (click)="close.emit()">Close</button>
</div>
```

### Computed Properties

Chart data properties (`pieChartData`, `lineChartData`) should be computed when the `data` input changes.  Use `ngOnChanges` or a setter on `data` to recompute when new data arrives.

## Colour Palette

Use a fixed palette of distinct colours (e.g. Bootstrap's palette or Chart.js defaults) cycling as needed.  For the line graph, the overall balance line should be visually distinct (e.g. bold black or dark grey).

## Testing

Manual verification:
- Chart button appears in toolbar between Export and Rec. File
- Clicking "Chart" opens the modal
- Pie chart displays by default; only net-negative categories are shown as segments
- Switching to "Account Balance" shows a line graph with one line per account plus an overall balance line
- Closing the dialog (header × or footer Close) hides the modal
- Opening the dialog again re-renders with current data
- Works when the transaction list is empty (no crash; show empty chart or "No data" message)
