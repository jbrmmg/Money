# JBR-686 – Report Generator Redesign

## Objective

Replace the current report generation pipeline with one that produces professional-looking PDF reports. The existing pipeline (JDOM2 HTML + iText 5 XMLWorker) is fragile and produces poor output due to XMLWorker's severely limited CSS support. The redesign uses Thymeleaf templates for HTML authoring and openhtmltopdf for PDF conversion, with SVG charts embedded inline in the HTML.

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
HTML string → openhtmltopdf → Report.pdf → share/
```

Key changes:
- Thymeleaf templates replace JDOM2 HTML construction — templates are real HTML files readable/previewable in a browser
- SVG charts are generated in Java and injected as **inline SVG** directly into the template — no PNG conversion, no file paths, no Batik transcoding
- openhtmltopdf (Flying Saucer fork) replaces iText 5 + XMLWorker — much better CSS/table/page-break support
- The HTML intermediary remains useful for development and debugging (controlled by a flag in production)

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
| vs Last Month | Spending change as % with ▲/▼ arrow | Red if up, green if down |

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
| dd-MMM-yyyy | Account name | Category name (coloured left border or dot) | Description text | Right-aligned, red if debit |

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

All charts are generated as SVG strings in Java and injected inline via `th:utext`. No PNG files are written; no Batik transcoding is needed for chart generation.

### 1. `DonutChartSvg` — replaces `PieChartSvg`

Modify the existing `PieChartSvg` to:
- Add a white centre circle (converting from full pie to donut)
- Print the total spending amount as text in the centre
- Suppress segment labels for segments below 5%
- Keep existing segment and label generation logic

### 2. `ComparisonBarChartSvg` — new

Horizontal bar chart.

Inputs: `Map<Category, CategoryComparison>`, period label ("Previous Month" / "Previous Year")

Layout (10000 × N×300 SVG units, where N = number of categories):
- Left column (2500 units): category name, right-aligned
- Bar area (6500 units): two horizontal bars per row
  - Current bar: full category colour, height 100 units
  - Previous bar: 40% opacity category colour, height 100 units, below the current bar
- Right column (1000 units): current amount text

### 3. `MonthlyBarChartSvg` — new (annual report only)

Vertical grouped bar chart.

Inputs: `List<MonthSummary>` for current year and previous year (12 entries each)

Layout (12000 × 6000 SVG units):
- X axis: 12 month labels
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
  annual.html           — full annual report
  fragments/
    head.html           — <head> with embedded CSS (th:fragment="head")
    summary-strip.html  — 4 KPI boxes (th:fragment="summaryStrip(data)")
    transaction-table.html — transaction list (th:fragment="transactionTable(rows)")
    month-section.html  — single month section used in annual (th:fragment="monthSection(data)")
```

CSS is embedded in the `<head>` as a `<style>` block (not an external file) so openhtmltopdf resolves it without a base URI.

Fonts: use web-safe fonts only (`Arial, Helvetica, sans-serif`) to avoid font embedding complexity. Revisit in a future ticket if custom fonts are needed.

---

## Data Model

New classes in `com.jbr.middletier.money.reporting`:

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
    List<TransactionRow> transactions; // null for annual sub-month sections
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

`ReportGenerator` builds a `ReportPeriodData`, passes it to `TemplateEngine.process("report/monthly", context)`, and then passes the resulting HTML string to the PDF renderer.

---

## Dependencies

### Add to `pom.xml`

```xml
<!-- Thymeleaf -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>

<!-- openhtmltopdf -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-core</artifactId>
    <version>1.0.10</version>
</dependency>
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
</dependency>
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-svg-support</artifactId>
    <version>1.0.10</version>
</dependency>
```

### Remove from `pom.xml`

```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>          <!-- replaced by openhtmltopdf-pdfbox -->
</dependency>
<dependency>
    <groupId>com.itextpdf.tool</groupId>
    <artifactId>xmlworker</artifactId>         <!-- replaced by openhtmltopdf-core -->
</dependency>
<dependency>
    <groupId>com.helger</groupId>
    <artifactId>ph-css</artifactId>            <!-- CSS now written directly in template -->
</dependency>
```

`jdom2` stays — it is still used for SVG generation (`ScalableVectorGraphics` hierarchy).

`batik-transcoder` and `batik-codec` explicit declarations can be removed — they become transitive dependencies via `openhtmltopdf-svg-support`.

### Classes to delete (Stage 4, after all stages complete)

| Class | Reason |
|---|---|
| `xml/html/ReportHtml.java` | Replaced by `templates/report/monthly.html` and `annual.html` |
| `xml/svg/PieChartSvg.java` | Replaced by `DonutChartSvg` |
| `xml/svg/CategorySvg.java` | PNG embedding no longer used |

Do **not** delete:
- `xml/html/HyperTextMarkupLanguage.java` — still used by `EmailHtml` / `EmailGenerator`
- `xml/html/EmailHtml.java` — still used by `EmailGenerator`
- `xml/svg/ScalableVectorGraphics.java` — base class for all SVG generation
- `xml/svg/LogoSvg.java` — used by `LogoManager`

---

## Application Properties

Add:
```yaml
money:
  report-debug-html: false   # when true, writes Report.html to reportWorking for browser inspection
```

`reportWorking` is only needed when `report-debug-html: true` or during Stage 1–2 development. It can be removed from required config in Stage 4 if debug is false.

---

## Development Stages

The HTML file is the natural intermediary for staged development. Stages 1 and 2 produce an HTML file that can be opened directly in a browser to iterate on the layout before any PDF work begins.

---

### Stage 1 — Thymeleaf HTML for Monthly Report

**Goal**: produce a browser-previewable HTML file for the monthly report. No PDF at this stage.

Tasks:
- Add `spring-boot-starter-thymeleaf` dependency
- Create `ReportPeriodData` and `TransactionRow` model classes
- Create `templates/report/monthly.html` with:
  - Embedded CSS in `<head>`
  - Header section
  - Summary strip (4 KPI boxes) using static placeholder values initially, then wired to model
  - Placeholder `<div>` where charts will go (Stage 2)
  - Transaction table fragment
- Modify `ReportGenerator.generateReport()` to:
  - Build a `ReportPeriodData` from the transaction lists
  - Process the Thymeleaf template
  - Write HTML string to `reportWorking/Report.html`
- PDF generation call remains unchanged (iText will likely fail on the new HTML — that is acceptable at this stage)

**Deliverable**: `Report.html` viewable in a browser showing the monthly report layout with real data but no charts.

---

### Stage 2 — SVG Charts in Monthly Report

**Goal**: complete the monthly report HTML with all charts.

Tasks:
- Implement `DonutChartSvg` (modify `PieChartSvg` — add white centre circle and centre text)
- Implement `ComparisonBarChartSvg`
- Add `donutSvg` and `comparisonBarSvg` strings to `ReportPeriodData`
- Inject inline SVG into `monthly.html` using `th:utext`
- Refine layout and CSS until the HTML looks correct in a browser

**Deliverable**: `Report.html` viewable in a browser showing the complete monthly report — summary strip, donut chart, comparison bars, transaction list.

---

### Stage 3 — openhtmltopdf PDF Generation (Monthly)

**Goal**: produce a PDF from the HTML generated in Stage 2.

Tasks:
- Add `openhtmltopdf-*` dependencies, remove `itextpdf` and `xmlworker`
- Implement `PdfRenderer` helper that accepts an HTML string and writes a PDF:
  ```java
  try (OutputStream out = Files.newOutputStream(Paths.get(pdfPath))) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withHtmlContent(html, null);
      builder.toStream(out);
      builder.run();
  }
  ```
- Replace `generatePDF()` in `ReportGenerator` with a call to `PdfRenderer`
- Adjust `monthly.html` CSS for PDF-specific concerns:
  - `@page { size: A4; margin: 15mm; }`
  - `page-break-after: always` between Page 1 and Page 2
  - Ensure table widths are absolute (not percentage-based) if needed for openhtmltopdf
- Add `money.report-debug-html` flag — when true, also write HTML to disk alongside PDF

**Deliverable**: Monthly PDF report generated end-to-end, replacing the old pipeline.

---

### Stage 4 — Annual Report

**Goal**: annual report using the same pipeline.

Tasks:
- Implement `MonthlyBarChartSvg`
- Create `templates/report/annual.html` (reusing fragments from monthly)
- Add `monthSections` list to `ReportPeriodData`
- Modify `ReportGenerator.generateAnnualReport()` to use new pipeline
- Delete `ReportHtml.java`, `PieChartSvg.java`, `CategorySvg.java`
- Remove `batik-transcoder` and `batik-codec` explicit declarations

**Deliverable**: Annual PDF report generated end-to-end. Old JDOM2/iText pipeline fully removed.

---

## Testing

Unit tests: none required for the Thymeleaf templates themselves — visual output is verified by browser inspection.

Integration tests: add to `MoneyReportIT` or a new `ReportGeneratorIT`:
- Call `generateReport(year, month)` and assert the PDF file is created at the expected path
- Call `generateAnnualReport(year)` and assert the annual PDF and all 12 monthly PDFs exist
- `reportsGeneratedForYear()` returns true when all files are present
- Existing tests for `regularReport()` scheduling logic should continue to pass unchanged
