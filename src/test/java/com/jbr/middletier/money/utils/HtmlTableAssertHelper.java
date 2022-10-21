package com.jbr.middletier.money.utils;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class HtmlTableAssertHelper {
    public static abstract class HtmlTableAssertHelperData {
        public abstract void checkElement(Element element);
    }

    private static class HtmlTableAssertHelperDataText extends HtmlTableAssertHelperData {
        private final String text;
        private final String className;

        public HtmlTableAssertHelperDataText(String text, String className) {
            this.text = text;
            this.className = className;
        }

        @Override
        public void checkElement(Element element) {
            Assert.assertEquals(this.text,element.getText());
            Attribute classAttribute = element.getAttribute("class");
            if(this.className.length() > 0) {
                Assert.assertNotNull(classAttribute);
                Assert.assertEquals(this.className,classAttribute.getValue());
            } else {
                Assert.assertNull(classAttribute);
            }
        }
    }

    private static class HtmlTableAssertHelperDataImage extends HtmlTableAssertHelperData {
        private final int height;
        private final int width;
        private final String path;

        public HtmlTableAssertHelperDataImage(int height, int width, String path) {
            this.height = height;
            this.width = width;
            this.path = path;
        }

        @Override
        public void checkElement(Element element) {
            List<Element> images = element.getChildren("img");
            Assert.assertEquals(1, images.size());
            Assert.assertEquals(height + "px", images.get(0).getAttribute("height").getValue());
            Assert.assertEquals(width + "px", images.get(0).getAttribute("width").getValue());
            Assert.assertEquals(this.path, images.get(0).getAttribute("src").getValue());
        }
    }

    private static void internalExpectTableBuilder(List<List<HtmlTableAssertHelperData>> expected, int line, HtmlTableAssertHelperData data) {
        if(line == expected.size()) {
            expected.add(new ArrayList<>());
        }

        Assert.assertTrue(line < expected.size());

        List<HtmlTableAssertHelperData> valueList = expected.get(line);
        valueList.add(data);
    }

    public static void expectTableBuliderText(List<List<HtmlTableAssertHelperData>> expected, int line, String text, String className) {
        internalExpectTableBuilder(expected,line,new HtmlTableAssertHelperDataText(text,className));
    }

    public static void expectTableBuliderImage(List<List<HtmlTableAssertHelperData>> expected, int line, int height, int width, String imagePath) {
        internalExpectTableBuilder(expected,line,new HtmlTableAssertHelperDataImage(height,width,imagePath));
    }

    private static void checkTableRow(Element row, List<HtmlTableAssertHelperData> expected) {
        List<Element> columns = row.getChildren("td");
        Assert.assertEquals(expected.size(),columns.size());

        for(int i = 0; i < columns.size(); i++) {
            expected.get(i).checkElement(columns.get(i));
        }
    }

    private static void checkTableHeaderRow(Element row, List<HtmlTableAssertHelperData> expected) {
        List<Element> columns = row.getChildren("th");
        Assert.assertEquals(expected.size(),columns.size());

        for(int i = 0; i < columns.size(); i++) {
            expected.get(i).checkElement(columns.get(i));
        }
    }

    public static void checkTable(Element table, List<List<HtmlTableAssertHelperData>> expected) {
        List<Element> rows = table.getChildren("tr");
        Assert.assertEquals(expected.size(),rows.size());

        for(int i = 0; i < rows.size(); i++) {
            if(i == 0) {
                checkTableHeaderRow(rows.get(i), expected.get(i));
            } else {
                checkTableRow(rows.get(i), expected.get(i));
            }
        }
    }
}
