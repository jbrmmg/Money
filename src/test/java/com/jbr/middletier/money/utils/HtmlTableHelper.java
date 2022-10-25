package com.jbr.middletier.money.utils;

import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HtmlTableHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlTableHelper.class);

    private static class CompareResult {
        private boolean result;

        public CompareResult() {
            result = true;
        }

        public void fail() {
            result = false;
        }

        public boolean getResult() {
            return result;
        }
    }

    @SuppressWarnings("SameParameterValue")
    public static abstract class HtmlTableAssertHelperData {
        protected void assertText(CompareResult result, String expected, String actual, String log) {
            if(!expected.equals(actual)) {
                result.fail();
                LOG.warn("Unexpected text difference {} {} {}", expected, actual, log);
            }
        }
        protected void assertInteger(CompareResult result, int expected, int actual, String log) {
            if(expected != actual) {
                result.fail();
                LOG.warn("Unexpected number difference {} {} {}", expected, actual, log);
            }
        }
        protected void assertNull(CompareResult result, Object shouldBeNull, String log) {
            if(shouldBeNull != null) {
                result.fail();
                LOG.warn("Expected value not null {}", log);
            }
        }
        protected void assertNotNull(CompareResult result, Object shouldNoBeNull, String log) {
            if(shouldNoBeNull == null) {
                result.fail();
                LOG.warn("Expected value null {}", log);
            }
        }

        public abstract void checkElement(CompareResult result, Element element);
    }

    private static class HtmlTableAssertHelperDataText extends HtmlTableAssertHelperData {
        private final String text;
        private final String className;

        public HtmlTableAssertHelperDataText(String text, String className) {
            this.text = text;
            this.className = className;
        }

        @Override
        public void checkElement(CompareResult result, Element element) {
            assertText(result,this.text, element.getText(),"Text Check");
            Attribute classAttribute = element.getAttribute("class");
            if(this.className.length() > 0) {
                assertNotNull(result,classAttribute,"class attribute");
                if(classAttribute != null) {
                    assertText(result, this.className, classAttribute.getValue(), "Class Check");
                }
            } else {
                assertNull(result,classAttribute,"class attribute (2)");
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
        public void checkElement(CompareResult result, Element element) {
            List<Element> images = element.getChildren("img");
            assertInteger(result, 1, images.size(), "Image Count");
            assertText(result, height + "px", images.get(0).getAttribute("height").getValue(), "height");
            assertText(result, width + "px", images.get(0).getAttribute("width").getValue(), "width");
            assertText(result, this.path, images.get(0).getAttribute("src").getValue(), "source path");
        }
    }

    private static void internalExpectTableBuilder(List<List<HtmlTableAssertHelperData>> expected, int line, HtmlTableAssertHelperData data) {
        if(line == expected.size()) {
            expected.add(new ArrayList<>());
        }

        List<HtmlTableAssertHelperData> valueList = expected.get(line);
        valueList.add(data);
    }

    public static void expectTableBuliderText(List<List<HtmlTableAssertHelperData>> expected, int line, String text, String className) {
        internalExpectTableBuilder(expected,line,new HtmlTableAssertHelperDataText(text,className));
    }

    public static void expectTableBuliderImage(List<List<HtmlTableAssertHelperData>> expected, int line, int height, int width, String imagePath) {
        internalExpectTableBuilder(expected,line,new HtmlTableAssertHelperDataImage(height,width,imagePath));
    }

    private static void checkTableRow(CompareResult result, Element row, List<HtmlTableAssertHelperData> expected) {
        List<Element> columns = row.getChildren("td");
        if(expected.size() != columns.size()) {
            LOG.warn("Table row, column difference");
            result.fail();
            return;
        }

        for(int i = 0; i < columns.size(); i++) {
            expected.get(i).checkElement(result, columns.get(i));
        }
    }

    private static void checkTableHeaderRow(CompareResult result, Element row, List<HtmlTableAssertHelperData> expected) {
        List<Element> columns = row.getChildren("th");
        if(expected.size() != columns.size()) {
            LOG.warn("Header row, column difference");
            result.fail();
            return;
        }

        for(int i = 0; i < columns.size(); i++) {
            expected.get(i).checkElement(result, columns.get(i));
        }
    }

    public static boolean checkTable(Element table, List<List<HtmlTableAssertHelperData>> expected) {
        CompareResult result = new CompareResult();

        List<Element> rows = table.getChildren("tr");
        if(expected.size() != rows.size()) {
            LOG.warn("Row Size difference");
            return false;
        }

        for(int i = 0; i < rows.size(); i++) {
            if(i == 0) {
                checkTableHeaderRow(result, rows.get(i), expected.get(i));
            } else {
                checkTableRow(result, rows.get(i), expected.get(i));
            }
        }

        return result.getResult();
    }
}
