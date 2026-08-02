package com.jbr.middletier.money.xml.svg;

import org.jdom2.Element;
import org.jdom2.Namespace;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MonthlyBarChartSvg extends ScalableVectorGraphics {

    public record MonthData(BigDecimal income, BigDecimal spending) {}

    private static final int TOTAL_WIDTH = 13400;
    private static final int TOTAL_HEIGHT = 7000;
    private static final int LEFT_MARGIN = 1200;
    private static final int TOP_MARGIN = 400;
    private static final int BOTTOM_MARGIN = 600;
    // chart area x: LEFT_MARGIN .. LEFT_MARGIN+CHART_WIDTH   y: TOP_MARGIN .. TOP_MARGIN+CHART_HEIGHT
    private static final int CHART_WIDTH = 12000;  // TOTAL_WIDTH - LEFT_MARGIN - 200
    private static final int CHART_HEIGHT = 6000;  // TOTAL_HEIGHT - TOP_MARGIN - BOTTOM_MARGIN
    private static final int GROUP_WIDTH = 1000;   // CHART_WIDTH / 12
    private static final int BAR_WIDTH = 360;
    private static final int INCOME_BAR_OFFSET = 100;
    private static final int SPENDING_BAR_OFFSET = 540; // 100 + 360 + 80
    private static final String INCOME_COLOUR = "3cb44b";
    private static final String SPENDING_COLOUR = "e6194b";
    private static final String[] MONTH_ABBR = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    public MonthlyBarChartSvg(List<MonthData> months) {
        Namespace ns = Namespace.getNamespace(NAMESPACE);

        BigDecimal maxValue = BigDecimal.ONE; // guard against zero
        for (MonthData m : months) {
            if (m.income().compareTo(maxValue) > 0) maxValue = m.income();
            if (m.spending().compareTo(maxValue) > 0) maxValue = m.spending();
        }

        Element root = new Element("svg", ns)
                .setAttribute(ATTRIBUTE_VIEW_BOX, "0 0 " + TOTAL_WIDTH + " " + TOTAL_HEIGHT)
                .setAttribute(ATTRIBUTE_WIDTH, String.valueOf(TOTAL_WIDTH / 20))
                .setAttribute(ATTRIBUTE_HEIGHT, String.valueOf(TOTAL_HEIGHT / 20));

        int floorY = TOP_MARGIN + CHART_HEIGHT;
        root.addContent(hline(ns, LEFT_MARGIN, floorY, LEFT_MARGIN + CHART_WIDTH, "#cccccc", 20));

        // Gridlines and Y-axis labels at 25 / 50 / 75 / 100 %
        for (int pct = 25; pct <= 100; pct += 25) {
            int y = TOP_MARGIN + CHART_HEIGHT * (100 - pct) / 100;
            root.addContent(hline(ns, LEFT_MARGIN, y, LEFT_MARGIN + CHART_WIDTH, "#e0e0e0", 15));
            BigDecimal labelValue = maxValue.multiply(BigDecimal.valueOf(pct))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            root.addContent(new Element("text", ns)
                    .setAttribute("x", String.valueOf(LEFT_MARGIN - 60))
                    .setAttribute("y", String.valueOf(y))
                    .setAttribute("text-anchor", "end")
                    .setAttribute("dominant-baseline", "middle")
                    .setAttribute("font-size", "190px")
                    .setAttribute("fill", "#666666")
                    .setText(String.format("£%,.0f", labelValue)));
        }

        // Bars and month labels
        for (int i = 0; i < Math.min(months.size(), 12); i++) {
            MonthData m = months.get(i);
            int groupX = LEFT_MARGIN + i * GROUP_WIDTH;

            if (m.income().compareTo(BigDecimal.ZERO) > 0) {
                int h = m.income().multiply(BigDecimal.valueOf(CHART_HEIGHT))
                        .divide(maxValue, 0, RoundingMode.HALF_UP).intValue();
                root.addContent(rect(ns, groupX + INCOME_BAR_OFFSET, floorY - h, BAR_WIDTH, h, INCOME_COLOUR));
            }

            if (m.spending().compareTo(BigDecimal.ZERO) > 0) {
                int h = m.spending().multiply(BigDecimal.valueOf(CHART_HEIGHT))
                        .divide(maxValue, 0, RoundingMode.HALF_UP).intValue();
                root.addContent(rect(ns, groupX + SPENDING_BAR_OFFSET, floorY - h, BAR_WIDTH, h, SPENDING_COLOUR));
            }

            root.addContent(new Element("text", ns)
                    .setAttribute("x", String.valueOf(groupX + GROUP_WIDTH / 2))
                    .setAttribute("y", String.valueOf(floorY + 370))
                    .setAttribute("text-anchor", "middle")
                    .setAttribute("font-size", "200px")
                    .setAttribute("fill", "#333333")
                    .setText(MONTH_ABBR[i]));
        }

        // Legend in top-right corner
        int legendX = LEFT_MARGIN + CHART_WIDTH - 2500;
        int legendY = TOP_MARGIN + 80;
        root.addContent(rect(ns, legendX,        legendY, 200, 200, INCOME_COLOUR));
        root.addContent(legendLabel(ns, legendX + 260,  legendY + 160, "Income"));
        root.addContent(rect(ns, legendX + 1300, legendY, 200, 200, SPENDING_COLOUR));
        root.addContent(legendLabel(ns, legendX + 1560, legendY + 160, "Spending"));

        this.svg.addContent(root);
    }

    private Element hline(Namespace ns, int x1, int y, int x2, String stroke, int width) {
        return new Element("line", ns)
                .setAttribute("x1", String.valueOf(x1))
                .setAttribute("y1", String.valueOf(y))
                .setAttribute("x2", String.valueOf(x2))
                .setAttribute("y2", String.valueOf(y))
                .setAttribute("stroke", stroke)
                .setAttribute("stroke-width", String.valueOf(width));
    }

    private Element rect(Namespace ns, int x, int y, int w, int h, String colour) {
        return new Element("rect", ns)
                .setAttribute("x", String.valueOf(x))
                .setAttribute("y", String.valueOf(y))
                .setAttribute("width", String.valueOf(w))
                .setAttribute("height", String.valueOf(h))
                .setAttribute("fill", "#" + colour);
    }

    private Element legendLabel(Namespace ns, int x, int y, String text) {
        return new Element("text", ns)
                .setAttribute("x", String.valueOf(x))
                .setAttribute("y", String.valueOf(y))
                .setAttribute("font-size", "200px")
                .setAttribute("fill", "#333333")
                .setText(text);
    }
}
