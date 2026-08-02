package com.jbr.middletier.money.xml.svg;

import com.jbr.middletier.money.data.primary.Category;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComparisonBarChartSvg extends ScalableVectorGraphics {
    private static final int ROW_HEIGHT = 600;
    private static final int NAME_RIGHT_X = 2400;
    private static final int BAR_START_X = 2500;
    private static final int BAR_AREA_WIDTH = 6500;   // 2500 to 9000
    private static final int AMOUNT_LEFT_X = 9050;
    private static final int TOTAL_WIDTH = 10000;

    public ComparisonBarChartSvg(Map<Category, BigDecimal> current, Map<Category, BigDecimal> previous) {
        Namespace ns = Namespace.getNamespace(NAMESPACE);

        List<Category> categories = new ArrayList<>(current.keySet());
        categories.sort((a, b) -> current.get(b).compareTo(current.get(a)));

        if (categories.isEmpty()) {
            this.svg.addContent(new Element("svg", ns)
                    .setAttribute(ATTRIBUTE_VIEW_BOX, "0 0 " + TOTAL_WIDTH + " " + ROW_HEIGHT)
                    .setAttribute(ATTRIBUTE_WIDTH, "500")
                    .setAttribute(ATTRIBUTE_HEIGHT, "25"));
            return;
        }

        BigDecimal maxAmount = BigDecimal.ZERO;
        for (Category c : categories) {
            BigDecimal cur = current.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal prev = previous.getOrDefault(c, BigDecimal.ZERO);
            if (cur.compareTo(maxAmount) > 0) maxAmount = cur;
            if (prev.compareTo(maxAmount) > 0) maxAmount = prev;
        }

        int totalHeight = categories.size() * ROW_HEIGHT;
        int displayHeight = 500 * totalHeight / TOTAL_WIDTH;
        Element root = new Element("svg", ns)
                .setAttribute(ATTRIBUTE_VIEW_BOX, "0 0 " + TOTAL_WIDTH + " " + totalHeight)
                .setAttribute(ATTRIBUTE_WIDTH, "500")
                .setAttribute(ATTRIBUTE_HEIGHT, String.valueOf(displayHeight));

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            String colour = cat.getColour() != null ? cat.getColour() : "999999";
            int rowY = i * ROW_HEIGHT;
            BigDecimal curAmt = current.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal prevAmt = previous.getOrDefault(cat, BigDecimal.ZERO);

            root.addContent(new Element("text", ns)
                    .setAttribute("x", String.valueOf(NAME_RIGHT_X))
                    .setAttribute("y", String.valueOf(rowY + ROW_HEIGHT / 2))
                    .setAttribute("text-anchor", "end")
                    .setAttribute("dominant-baseline", "central")
                    .setAttribute("font-size", "200px")
                    .setAttribute("fill", "#333333")
                    .setText(cat.getName()));

            if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
                int curBarWidth = curAmt
                        .multiply(BigDecimal.valueOf(BAR_AREA_WIDTH))
                        .divide(maxAmount, 0, RoundingMode.HALF_UP)
                        .intValue();
                root.addContent(new Element("rect", ns)
                        .setAttribute("x", String.valueOf(BAR_START_X))
                        .setAttribute("y", String.valueOf(rowY + 100))
                        .setAttribute("width", String.valueOf(curBarWidth))
                        .setAttribute("height", "160")
                        .setAttribute("fill", "#" + colour));

                int prevBarWidth = prevAmt
                        .multiply(BigDecimal.valueOf(BAR_AREA_WIDTH))
                        .divide(maxAmount, 0, RoundingMode.HALF_UP)
                        .intValue();
                root.addContent(new Element("rect", ns)
                        .setAttribute("x", String.valueOf(BAR_START_X))
                        .setAttribute("y", String.valueOf(rowY + 340))
                        .setAttribute("width", String.valueOf(prevBarWidth))
                        .setAttribute("height", "130")
                        .setAttribute("fill", "#" + colour)
                        .setAttribute("opacity", "0.4"));
            }

            root.addContent(new Element("text", ns)
                    .setAttribute("x", String.valueOf(AMOUNT_LEFT_X))
                    .setAttribute("y", String.valueOf(rowY + ROW_HEIGHT / 2))
                    .setAttribute("dominant-baseline", "central")
                    .setAttribute("font-size", "190px")
                    .setAttribute("fill", "#333333")
                    .setText(String.format("£%,.0f", curAmt)));
        }

        this.svg.addContent(root);
    }
}
