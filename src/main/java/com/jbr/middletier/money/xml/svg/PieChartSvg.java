package com.jbr.middletier.money.xml.svg;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.util.CategoryPercentageHelper;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.text.DecimalFormat;
import java.util.List;

public class PieChartSvg extends ScalableVectorGraphics {
    private static final String ELEMENT_CIRCLE = "circle";
    private static final String ELEMENT_TEXT = "text";
    private static final String ATTRIBUTE_ID = "id";
    private static final String ATTRIBUTE_CENTER_X = "cx";
    private static final String ATTRIBUTE_CENTER_Y = "cy";
    private static final String ATTRIBUTE_X = "x";
    private static final String ATTRIBUTE_Y = "y";
    private static final String ATTRIBUTE_RADIUS = "r";
    private static final String ATTRIBUTE_FILL = "fill";
    private static final String ATTRIBUTE_FONT_SIZE = "font-size";
    private static final String ATTRIBUTE_TEXT_ANCHOR = "text-anchor";
    private static final String ATTRIBUTE_STROKE = "stroke";
    private static final String ATTRIBUTE_STROKE_WIDTH = "stroke-width";
    private static final String ATTRIBUTE_STROKE_DASHARRAY = "stroke-dasharray";
    private static final String ATTRIBUTE_TRANSFORM = "transform";
    private static final DecimalFormat noDP = new DecimalFormat("#");
    private static final DecimalFormat sixDP = new DecimalFormat("#.######");

    private Element getPieSegment(Namespace svgNamespace, String id, double radius, String colour, double percent) {
        String strokeDashArray1 = sixDP.format(2 * radius * Math.PI * percent / 100.0);
        String strokeDashArray2 = sixDP.format(2 * radius * Math.PI);

        return new Element(ELEMENT_CIRCLE, svgNamespace)
                .setAttribute(ATTRIBUTE_ID,id)
                .setAttribute(ATTRIBUTE_RADIUS, noDP.format(radius))
                .setAttribute(ATTRIBUTE_CENTER_X,"5000")
                .setAttribute(ATTRIBUTE_CENTER_Y,"5000")
                .setAttribute(ATTRIBUTE_FILL,"none")
                .setAttribute(ATTRIBUTE_STROKE,"#" + colour)
                .setAttribute(ATTRIBUTE_STROKE_WIDTH, noDP.format(2 * radius))
                .setAttribute(ATTRIBUTE_STROKE_DASHARRAY,strokeDashArray1 + " " + strokeDashArray2)
                .setAttribute(ATTRIBUTE_TRANSFORM,"rotate(-90) translate(" + noDP.format(-4 * radius) + ")");
    }

    private double getBrightness(String colour) {
        int red = Integer.parseInt(colour.substring(0,2),16);
        int green = Integer.parseInt(colour.substring(2,4),16);
        int blue = Integer.parseInt(colour.substring(4,6),16);

        return Math.sqrt(red * red * .241 + green * green * .691 + blue * blue * .068);
    }

    private String getTextColour(String colour) {
        return getBrightness(colour) > 130 ? "000000" : "FFFFFF";
    }

    private Element getPieSegmentText(Namespace svgNamespace, String id, double angle, String colour, double percent, String name) {
        double x = 5000 + Math.sin(Math.toRadians((angle + 180) * -1)) * 4800;
        double y = 5000 + Math.cos(Math.toRadians((angle + 180) * -1)) * 4800;

        double rotateAngle = angle + 90;

        boolean textAnchorEnd = false;

        // If the rotate angle is in the range -90 to -180, then add 180 and anchor text to the end.
        if( (rotateAngle >= -270) && (rotateAngle < -90) ){
            textAnchorEnd = true;
            rotateAngle += 180;
        }

        int textSize = 120;

        if(percent > 50) {
            textSize = 1200;
        } else if (percent > 20) {
            textSize = 600;
        } else if (percent > 5) {
            textSize = 300;
        }

        String textColour = getTextColour(colour);

        return new Element(ELEMENT_TEXT, svgNamespace)
                .setAttribute(ATTRIBUTE_ID,id + "-txt")
                .setAttribute(ATTRIBUTE_FILL, "#" + textColour)
                .setAttribute(ATTRIBUTE_FONT_SIZE, textSize + "px")
                .setAttribute(ATTRIBUTE_TEXT_ANCHOR, textAnchorEnd ? "end" : "start")
                .setAttribute(ATTRIBUTE_X, sixDP.format(x))
                .setAttribute(ATTRIBUTE_Y, sixDP.format(y))
                .setAttribute(ATTRIBUTE_TRANSFORM, "rotate(" + sixDP.format(rotateAngle) + " " + sixDP.format(x) + "," + sixDP.format(y) + ")")
                .setText(name);
    }

    public PieChartSvg(List<Transaction> transactions) {
        Namespace svgNamespace = Namespace.getNamespace(NAMESPACE);

        Element background = new Element(ELEMENT_CIRCLE, svgNamespace)
                .setAttribute(ATTRIBUTE_ID, "BCKG")
                .setAttribute(ATTRIBUTE_RADIUS, "5000")
                .setAttribute(ATTRIBUTE_CENTER_X, "5000")
                .setAttribute(ATTRIBUTE_CENTER_Y, "5000")
                .setAttribute(ATTRIBUTE_FILL, "white");

        Element root = new Element("svg", svgNamespace)
                .setAttribute(ATTRIBUTE_VIEW_BOX,"0 0 10000 10000")
                .addContent(background);

        CategoryPercentageHelper categoryPercentageHelper = new CategoryPercentageHelper(transactions);

        double percent = 100;
        double radius = 2500;
        for(Category next : categoryPercentageHelper.getCategories()) {
            root.addContent(getPieSegment(svgNamespace, next.getId(), radius, next.getColour(), percent));

            percent = percent - categoryPercentageHelper.getPercentage(next);

            if(percent < 0.0) {
                percent = 0.0;
            }
        }

        percent = 100;
        for(Category next : categoryPercentageHelper.getCategories()) {
            double halfWay = (100 - (percent - categoryPercentageHelper.getPercentage(next) / 2)) * 3.6;

            root.addContent(getPieSegmentText(svgNamespace, next.getId(), halfWay * -1, next.getColour(), categoryPercentageHelper.getPercentage(next), next.getName()));

            percent -= categoryPercentageHelper.getPercentage(next);
        }

        Element outline = new Element(ELEMENT_CIRCLE, svgNamespace)
                .setAttribute(ATTRIBUTE_ID, "OUTL")
                .setAttribute(ATTRIBUTE_RADIUS, "5000")
                .setAttribute(ATTRIBUTE_CENTER_X, "5000")
                .setAttribute(ATTRIBUTE_CENTER_Y, "5000")
                .setAttribute(ATTRIBUTE_FILL, "none")
                .setAttribute(ATTRIBUTE_STROKE,"black")
                .setAttribute(ATTRIBUTE_STROKE_WIDTH,"20");

        root.addContent(outline);

        this.svg.addContent(root);
    }
}
