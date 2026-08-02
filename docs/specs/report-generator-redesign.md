# JBR-686 – Report Generator Redesign

## Objective

Replace the current report generation pipeline with one that produces well-styled, self-contained HTML
reports browsable as a static archive. The existing pipeline (JDOM2 HTML + iText 5 XMLWorker) is fragile
and produces poor output. The redesign uses Thymeleaf templates with inline SVG charts, and copies the
resulting HTML into a year-partitioned directory structure with generated index pages.

PDF generation is intentionally removed. The browser's built-in Print → Save as PDF covers that use-case.

---

## Current Pipeline (to be replaced)

```
Java (JDOM2) → Report.html ─┐
Java (JDOM2+Batik) → *.svg → *.png ─┤  → iText 5 XMLWorker → Report.pdf → share/
```

Problems:
- XMLWorker has very limited CSS support — layout is highly constrained
- HTML is built in Java using JDOM2 DOM elements — hard to read and change
- SVG charts are transcoded to PNG via Batik before being embedded as file paths in the HTML
- Overall: slow pipeline, poor output, difficult to maintain

---

## New Pipeline

```
Java data → Thymeleaf template → HTML string
Java (JDOM2 SVG) → inline SVG strings (embedded via th:utext)
HTML string → write to share/<year>/<MonthName>.html
            → regenerate share/<year>/index.html
            → regenerate share/index.html
```

Key changes:
- Thymeleaf templates replace JDOM2 HTML construction — templates are real HTML files
  readable/previewable in a browser
- SVG charts are generated in Java and injected as **inline SVG** directly into the template —
  no PNG conversion, no file paths, no Batik transcoding
- HTML replaces PDF as the output format. No openhtmltopdf dependency.
- A two-level static index (root + per-year) makes historic reports navigable without a server

---

## Archive Directory Structure

```
share/
  index.html                  ← root index; links to each year sub-directory
  2024/
    index.html                ← year index; links to each month + annual (if present)
    January.html
    February.html
    ...
    December.html
    annual.html               ← annual summary (Stage 4)
  2025/
    index.html
    January.html
    ...
```

### Root `index.html`

- Title: "Financial Reports"
- One link per year that has at least one report, newest year first
- Each link points to `<year>/index.html` and displays the year

### Year `index.html`

- Title: `"<year> Reports"`
- If `annual.html` exists in the directory, show it first as "Annual Report – <year>"
- Then one link per month that has a report, chronologically ordered (January → December)
- Each link points to `<MonthName>.html`

### Regeneration rule

Both index files are regenerated from scratch (by scanning the directory) every time a new
report is written. They are not append-only; a full scan ensures correctness after any manual
file deletions.

---

## Report Content

### Monthly Report

#### Page 1 — Summary

**Header**
- Full-width header bar containing the report title: `"[Month] [Year]"` (e.g. "April 2024")
- Subtitle: `"Monthly Financial Report"`

**Summary Strip**
Four equally-sized KPI boxes in a row:

| Box | Content | Colour |
|---|---|---|
| Total Income | Sum of all credit transactions | Green text |
| Total Spending | Sum of all debit transactions (absolute value) | Red text |
| Net | Income − Spending | Green if positive, red if negative |
| vs Last Month | Spending change as % with (+)/(-) indicator | Red if up, green if down |

**Charts — side by side**

Left (60% width): **Spending Donut Chart**
- Category segments using each category's colour
- Total spending amount printed in the centre of the donut hole
- Category name labels on or near each segment (suppress for segments < 5%)

Right (40% width): **Category Comparison Bar Chart** (horizontal)
- One row per expense category, sorted by absolute spending (largest first)
- Two bars per row:
  - Current period bar: solid category colour
  - Previous period bar: same colour at 40% opacity
- Category name on the left of each row
- No axis labels needed — relative proportions are the point

#### Page 2 — Transaction List

Single-column table with one transaction per row, sorted chronologically.

Columns:
| Date | Account | Category | Description | Amount |
|---|---|---|---|---|
| dd-MMM-yyyy | Account name | Category name (coloured dot) | Description text | Right-aligned, red if debit |

- Totals row at the bottom: Total Credits | Total Debits
- Transfers (transactions with an `oppositeId`) shown with a ⇄ indicator and grey text

---

### Annual Report

#### Page 1 — Annual Summary

**Header**: `"[Year] Annual Report"`

**Summary Strip** — same 4 KPI boxes but comparing current year to previous year instead of month-to-month.

**Charts — stacked vertically**

1. **12-Month Income vs Spending Bar Chart** (vertical grouped bars)
   - X axis: Jan–Dec
   - Two bars per month: Income (green) and Spending (red)
   - This is the primary visual for the annual report — shows seasonal patterns and bad months at a glance

2. **Annual Spending Donut Chart**
   - Same layout as monthly donut but for the full year's transactions
   - Total spending for the year in the centre

3. **Annual Category Comparison Bar Chart**
   - Same layout as monthly version but comparing this year vs previous year

#### Pages 2–13 — Per-Month Sections

One page per month (January through December), each containing:
- Month title: `"[Month] [Year]"`
- Summary strip (4 KPI boxes for that month vs previous month)
- Spending donut chart for the month
- Category comparison bar chart (month vs prior month)

No transaction list in the annual report — individual transactions are in the monthly reports.

---

## SVG Charts (Server-Side Java)

All charts are generated as SVG strings in Java and injected inline via `th:utext`. No PNG files are
written; no Batik transcoding is needed for chart generation.

### 1. `DonutChartSvg` — replaces `PieChartSvg`

Modify the existing `PieChartSvg` to:
- Add a white centre circle (converting from full pie to donut)
- Print the total spending amount as text in the centre
- Suppress segment labels for segments below 5%
- Keep existing segment and label generation logic

### 2. `ComparisonBarChartSvg` — new

Horizontal bar chart.

Inputs: `Map<Category, BigDecimal>` current and previous spending maps.

Layout (10000 × N×600 SVG units, where N = number of categories):
- Left column (2500 units): category name, right-aligned
- Bar area (6500 units): two horizontal bars per row
  - Current bar: full category colour, y+100, height=160
  - Previous bar: same colour at 40% opacity, y+340, height=130
- Right column: current amount text

### 3. `MonthlyBarChartSvg` — new (annual report only)

Vertical grouped bar chart.

Inputs: `List<MonthSummary>` for current year and previous year (12 entries each)

Layout (12000 × 6000 SVG units):
- X axis: Jan–Dec
- Y axis: £ values, auto-scaled to max(income, spending) across all months
- Two bars per month: Income (green `#3cb44b`) and Spending (red `#e6194b`)
- Gridlines at 25%, 50%, 75%, 100% of max

Each `MonthSummary` carries: `month`, `totalIncome`, `totalSpending`.

---

## Thymeleaf Templates

Location: `src/main/resources/templates/report/`

```
templates/report/
  monthly.html          — full monthly report
  annual.html           — full annual report (Stage 4)
```

CSS is embedded in the `<head>` as a `<style>` block. Includes `@page { size: A4; margin: 15mm; }`
so the file prints to A4 correctly from the browser.

Fonts: use web-safe fonts only (`Arial, Helvetica, sans-serif`).

---

## Data Model

Classes in `com.jbr.middletier.money.reporting`:

```java
public class ReportPeriodData {
    String title;              // "April 2024" or "2024"
    String subtitle;           // "Monthly Financial Report" or "Annual Report"
    BigDecimal totalIncome;
    BigDecimal totalSpending;
    BigDecimal net;
    double spendingChangePct;  // positive = spending went up
    String donutSvg;           // inline SVG string
    String comparisonBarSvg;   // inline SVG string
    String monthlyBarSvg;      // inline SVG string (annual only, null for monthly)
    List<TransactionRow> transactions;
    List<ReportPeriodData> monthSections; // annual only: 12 month sub-sections
}

public class TransactionRow {
    String date;               // "22-Apr-2024"
    String accountName;
    String categoryColour;     // hex without #, e.g. "FF5733"
    String categoryName;
    String description;
    BigDecimal amount;         // signed (negative = debit)
    boolean isTransfer;        // true if oppositeId != null
}
```

---

## Dependencies

### Add to `pom.xml`

```xml
<!-- Thymeleaf (already added in Stage 1) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### Remove from `pom.xml`

```xml
<!-- PDF generation — no longer needed -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-core</artifactId>
</dependency>
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
</dependency>
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-svg-support</artifactId>
</dependency>

<!-- Old pipeline -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
</dependency>
<dependency>
    <groupId>com.itextpdf.tool</groupId>
    <artifactId>xmlworker</artifactId>
</dependency>
```

`jdom2` stays — still used for SVG generation (`ScalableVectorGraphics` hierarchy).

`batik-transcoder` and `batik-codec` are removed in Stage 4 once the old annual report
pipeline is replaced and `createPngFromSvg` is deleted.

### Classes to delete (Stage 4, after all stages complete)

| Class | Reason |
|---|---|
| `xml/html/ReportHtml.java` | Replaced by `templates/report/monthly.html` and `annual.html` |
| `xml/svg/PieChartSvg.java` | Replaced by `DonutChartSvg` |
| `xml/svg/CategorySvg.java` | PNG embedding no longer used |
| `reporting/PdfRenderer.java` | PDF generation removed |

Do **not** delete:
- `xml/html/HyperTextMarkupLanguage.java` — still used by `EmailHtml` / `EmailGenerator`
- `xml/html/EmailHtml.java` — still used by `EmailGenerator`
- `xml/svg/ScalableVectorGraphics.java` — base class for all SVG generation
- `xml/svg/LogoSvg.java` — used by `LogoManager`

---

## Application Properties

```yaml
money:
  report-debug-html: false   # when true, also writes Report.html to reportWorking for browser inspection
```

The `reportShare` property is the root of the static archive (`share/` above).

---

## Development Stages

---

### Stage 1 — Thymeleaf HTML for Monthly Report ✅ COMPLETE

**Goal**: produce a browser-previewable HTML file for the monthly report. No PDF at this stage.

**Deliverable**: `Report.html` viewable in a browser showing the monthly report layout with real data but no charts.

---

### Stage 2 — SVG Charts in Monthly Report ✅ COMPLETE

**Goal**: complete the monthly report HTML with all charts.

**Deliverable**: `Report.html` viewable in a browser showing the complete monthly report — summary strip, donut chart, comparison bars, transaction list.

---

### Stage 3 — Static HTML Archive ✅ COMPLETE

**Goal**: replace PDF output with a static HTML archive. Monthly reports are written as individual HTML
files into a year-partitioned directory, with two levels of generated index pages.

Tasks:
- Remove `openhtmltopdf-*` dependencies from `pom.xml`
- Delete `PdfRenderer.java`
- Modify `ReportGenerator.generateReport(year, month)`:
  - Build `ReportPeriodData` and render HTML via Thymeleaf (unchanged from Stage 2)
  - Write HTML to `share/<year>/<MonthName>.html` (e.g. `share/2024/January.html`)
  - Regenerate `share/<year>/index.html` (scan year directory for present month files)
  - Regenerate `share/index.html` (scan share directory for present year subdirectories)
- Change `getMonthFilename(fullPath, year, month)` to return `.html` filenames
- Update `regularReport()` existence-check to look for `.html` files
- Update `reportsGeneratedForYear()` to check for `.html` files
- Add `generateYearIndex(year)` — writes `share/<year>/index.html`
- Add `generateRootIndex()` — writes `share/index.html`
- Optionally: add `money.report-debug-html` flag — when true, also write to `reportWorking/Report.html`

Index file format:
- Plain HTML, `@page { size: A4; }` not needed (these are navigation pages, not printable reports)
- Root index: table/list of years (newest first), each linking to `<year>/index.html`
- Year index: link to `annual.html` if present, then January → December links for months present

**Deliverable**: Monthly HTML reports written to the archive, both index files kept up-to-date.
Old PDF path removed.

---

### Stage 4 — Annual HTML Report ✅ COMPLETE

**Goal**: annual report using the same Thymeleaf pipeline, completing the removal of the old JDOM2 pipeline.

Tasks:
- Implement `MonthlyBarChartSvg`
- Create `templates/report/annual.html`
- Add `monthSections` list to `ReportPeriodData`
- Modify `ReportGenerator.generateAnnualReport()` to use Thymeleaf pipeline and write to
  `share/<year>/annual.html`; call `generateYearIndex(year)` afterwards
- Delete `ReportHtml.java`, `PieChartSvg.java`, `CategorySvg.java`, all `createPng*` methods
- Remove `batik-transcoder` and `batik-codec` explicit declarations

**Deliverable**: Annual HTML report generated end-to-end. Old JDOM2/Batik pipeline fully removed.

---

### Stage 5 — Date-Based Transactions and Revised Scheduling

**Goal**: fix the report content and scheduling logic so that reports are based on transaction date
rather than statement membership, and the scheduled job generates reports for exactly the months
that have stable, complete data.

#### Problem with statement-based grouping

Currently `generateReport(year, month)` fetches transactions via
`findByStatementIdYearAndStatementIdMonth(year, month)` — i.e. transactions that have been filed
into that particular statement. This means a transaction dated 15 January but reconciled and placed
in a February statement will appear in the February report rather than the January report. The reports
are therefore misleading and not aligned with calendar months.

#### Transaction query change

Replace the two statement-based repository queries used in report generation with date-range queries:

- Add to `TransactionRepository`:
  ```java
  List<Transaction> findByDateBetween(LocalDate start, LocalDate end);
  ```
  Spring Data JPA derives this automatically from the method name.

- In `generateReport(year, month)`:
  ```java
  LocalDate start = LocalDate.of(year, month, 1);
  LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
  List<Transaction> transactions = transactionRepository.findByDateBetween(start, end);

  LocalDate prevStart = start.minusMonths(1);
  LocalDate prevEnd   = prevStart.withDayOfMonth(prevStart.lengthOfMonth());
  List<Transaction> previousTransactions = transactionRepository.findByDateBetween(prevStart, prevEnd);
  ```

- In `generateAnnualReport(year)`:
  ```java
  List<Transaction> transactions = transactionRepository.findByDateBetween(
      LocalDate.of(year,     1, 1), LocalDate.of(year,     12, 31));
  List<Transaction> previousTransactions = transactionRepository.findByDateBetween(
      LocalDate.of(year - 1, 1, 1), LocalDate.of(year - 1, 12, 31));
  ```

The per-month grouping already done by `Collectors.groupingBy(t -> t.getDate().getMonthValue())`
inside `generateAnnualReport` remains unchanged — it was already date-based.

#### Scheduling algorithm change

Replace the current `regularReport()` logic (which required every active account to have a locked
statement for a month) with the following algorithm:

1. Collect all locked statements from active (non-closed) accounts.
2. If none exist, return — nothing to do.
3. Sort them by `(year, month)`.
4. **`startMonthYear`** = the earliest `(year, month)` in that list.
5. **`endMonthYear`** = the latest `(year, month)` in that list.
6. **`evaluationMonthYear`** = `endMonthYear` − 3 months (with year rollover).
   - If `endMonth <= 3`, subtract from year: e.g. end = (2026, 2) → evaluation = (2025, 11).
7. Iterate every calendar month from `startMonthYear` to `evaluationMonthYear` inclusive:
   - If `share/<year>/<MonthName>.html` does **not** exist → call `generateReport(year, month)`.
   - If the month is December and `share/<year>/annual.html` does **not** exist →
     call `generateAnnualReport(year)`.

The 3-month buffer prevents generating reports for months that are still close to the latest
activity and may receive late-dated transactions from ongoing reconciliation.

Remove `getMonthStatusMap()` and the `MonthStatus` helper class once they are no longer referenced
(check whether any test still calls `getMonthStatusMap()` directly and update it accordingly).

#### Tasks

- Add `findByDateBetween(LocalDate, LocalDate)` to `TransactionRepository`
- Update `generateReport()` to use date-based queries (see above)
- Update `generateAnnualReport()` to use date-based queries (see above)
- Replace `regularReport()` body with the new algorithm
- Remove `getMonthStatusMap()` and `MonthStatus` if unused
- Update unit tests to exercise date-based transaction selection
- `ScheduleReportTest` and `AnnualReportTest` should continue to pass with transactions that
  carry explicit dates matching the month under test

**Deliverable**: Reports contain transactions by date; the scheduled job generates exactly the
months with stable data, not just the months where every account has a locked statement.

---

## Testing

Unit tests: none required for the Thymeleaf templates themselves — visual output is verified by browser inspection.

Integration tests: add to `MoneyReportIT` or a new `ReportGeneratorIT`:
- Call `generateReport(year, month)` and assert:
  - HTML file exists at `share/<year>/<MonthName>.html`
  - `share/<year>/index.html` exists and contains a link to the month file
  - `share/index.html` exists and contains a link to the year
- Call `generateAnnualReport(year)` and assert `share/<year>/annual.html` exists and year index links to it
- `reportsGeneratedForYear()` returns true when all 12 month HTML files + annual HTML are present
- Existing tests for `regularReport()` scheduling logic should continue to pass unchanged
