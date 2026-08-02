package com.jbr.middletier.money.xml.svg;

import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.util.CategoryPercentageHelper;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

public class DonutChartSvg extends ScalableVectorGraphics {
    private static final DecimalFormat NO_DP = new DecimalFormat("#");
    private static final DecimalFormat SIX_DP = new DecimalFormat("#.######");
    private static final double CHART_RADIUS = 2500;

    private Element getPieSegment(Namespace ns, String id, String colour, double percent) {
        double circumference = 2 * CHART_RADIUS * Math.PI;
        return new Element("circle", ns)
                .setAttribute("id", id)
                .setAttribute("r", NO_DP.format(CHART_RADIUS))
                .setAttribute("cx", "5000")
                .setAttribute("cy", "5000")
                .setAttribute("fill", "none")
                .setAttribute("stroke", "#" + colour)
                .setAttribute("stroke-width", NO_DP.format(2 * CHART_RADIUS))
                .setAttribute("stroke-dasharray",
                        SIX_DP.format(circumference * percent / 100.0) + " " + SIX_DP.format(circumference))
                .setAttribute("transform", "rotate(-90) translate(-10000)");
    }

    private double getBrightness(String colour) {
        int r = Integer.parseInt(colour.substring(0, 2), 16);
        int g = Integer.parseInt(colour.substring(2, 4), 16);
        int b = Integer.parseInt(colour.substring(4, 6), 16);
        return Math.sqrt(r * r * .241 + g * g * .691 + b * b * .068);
    }

    private String getTextColour(String colour) {
        return getBrightness(colour) > 130 ? "000000" : "FFFFFF";
    }

    private Element getSegmentLabel(Namespace ns, String id, double angleDeg, String colour, double percent, String name) {
        double x = 5000 + Math.sin(Math.toRadians((angleDeg + 180) * -1)) * 3800;
        double y = 5000 + Math.cos(Math.toRadians((angleDeg + 180) * -1)) * 3800;
        double rotateAngle = angleDeg + 90;
        boolean anchorEnd = false;
        if (rotateAngle >= -270 && rotateAngle < -90) {
            anchorEnd = true;
            rotateAngle += 180;
        }
        int size = percent > 20 ? 300 : (percent > 10 ? 220 : 160);
        return new Element("text", ns)
                .setAttribute("id", id + "-txt")
                .setAttribute("fill", "#" + getTextColour(colour))
                .setAttribute("font-size", size + "px")
                .setAttribute("text-anchor", anchorEnd ? "end" : "start")
                .setAttribute("x", SIX_DP.format(x))
                .setAttribute("y", SIX_DP.format(y))
                .setAttribute("transform",
                        "rotate(" + SIX_DP.format(rotateAngle) + " " + SIX_DP.format(x) + "," + SIX_DP.format(y) + ")")
                .setText(name);
    }

    public DonutChartSvg(List<Transaction> transactions, BigDecimal totalSpending) {
        Namespace ns = Namespace.getNamespace(NAMESPACE);

        Element root = new Element("svg", ns)
                .setAttribute(ATTRIBUTE_VIEW_BOX, "0 0 10000 10000");

        root.addContent(new Element("circle", ns)
                .setAttribute("id", "BCKG")
                .setAttribute("r", "5000")
                .setAttribute("cx", "5000")
                .setAttribute("cy", "5000")
                .setAttribute("fill", "#f0f0f0"));

        CategoryPercentageHelper helper = new CategoryPercentageHelper(transactions);

        double percent = 100;
        for (Category cat : helper.getCategories()) {
            String colour = cat.getColour() != null ? cat.getColour() : "999999";
            root.addContent(getPieSegment(ns, cat.getId(), colour, percent));
            percent -= helper.getPercentage(cat);
            if (percent < 0) percent = 0;
        }

        percent = 100;
        for (Category cat : helper.getCategories()) {
            double segPct = helper.getPercentage(cat);
            if (segPct >= 5.0) {
                double midAngle = (100 - (percent - segPct / 2)) * 3.6;
                String colour = cat.getColour() != null ? cat.getColour() : "999999";
                root.addContent(getSegmentLabel(ns, cat.getId(), midAngle * -1, colour, segPct, cat.getName()));
            }
            percent -= segPct;
        }

        root.addContent(new Element("circle", ns)
                .setAttribute("id", "HOLE")
                .setAttribute("r", "2200")
                .setAttribute("cx", "5000")
                .setAttribute("cy", "5000")
                .setAttribute("fill", "white"));

        root.addContent(new Element("text", ns)
                .setAttribute("id", "TOTAL")
                .setAttribute("x", "5000")
                .setAttribute("y", "5000")
                .setAttribute("text-anchor", "middle")
                .setAttribute("dominant-baseline", "central")
                .setAttribute("font-size", "500px")
                .setAttribute("fill", "#333333")
                .setText(String.format("£%,.0f", totalSpending.abs())));

        this.svg.addContent(root);
    }
}
