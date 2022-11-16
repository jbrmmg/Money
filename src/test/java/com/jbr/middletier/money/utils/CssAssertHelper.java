package com.jbr.middletier.money.utils;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import org.junit.Assert;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CssAssertHelper {
    public static class CssAssertHelperData {
        private final String expectedExpression;
        private final boolean mandatory;
        private boolean found;

        public CssAssertHelperData(String expectedExpression, boolean mandatory) {
            this.expectedExpression = expectedExpression;
            this.mandatory = mandatory;
            this.found = false;
        }

        public void hasBeenFound() {
            this.found = true;
        }

        public boolean wasNotMandatoryOrFound() {
            return !mandatory || found;
        }

        public String getExpectedExpression() {
            return this.expectedExpression;
        }
    }

    public static void expectCssBuilder(Map<String,Map<String,CssAssertHelperData>> expected, String selector, String property, String expression, boolean mandatory) {
        Assert.assertNotNull(expected);

        // Check the selector is present.
        Map<String,CssAssertHelperData> expectedSelector;
        if(expected.containsKey(selector)) {
            expectedSelector = expected.get(selector);
        } else {
            expectedSelector = new HashMap<>();
            expected.put(selector,expectedSelector);
        }

        // Add the property in.
        Assert.assertFalse(expectedSelector.containsKey(property));
        expectedSelector.put(property,new CssAssertHelperData(expression,mandatory));
    }

    private static void checkCssDeclarations(CSSStyleRule rule, Map<String,CssAssertHelperData> expected) {
        Assert.assertEquals(expected.keySet().size(), rule.getDeclarationCount());

        for(int i = 0; i < rule.getDeclarationCount(); i++) {
            CSSDeclaration declaration = rule.getDeclarationAtIndex(i);
            Assert.assertNotNull(declaration);

            Assert.assertTrue(expected.containsKey(declaration.getProperty()));

            CssAssertHelperData expectedData = expected.get(declaration.getProperty());
            Assert.assertEquals(expectedData.getExpectedExpression(),declaration.getExpressionAsCSSString());

            expectedData.hasBeenFound();
        }
    }

    private static String getSelectorName(CSSSelector selector) {
        if(selector.getMemberCount() == 1) {
            CSSSelectorSimpleMember member = (CSSSelectorSimpleMember) selector.getMemberAtIndex(0);
            Assert.assertNotNull(member);
            return member.getValue();
        }

        if(selector.getMemberCount() == 2) {
            CSSSelectorSimpleMember member1 = (CSSSelectorSimpleMember) selector.getMemberAtIndex(0);
            Assert.assertNotNull(member1);
            CSSSelectorSimpleMember member2 = (CSSSelectorSimpleMember) selector.getMemberAtIndex(1);
            Assert.assertNotNull(member2);
            return member1.getValue() + member2.getValue();
        }

        Assert.fail();
        return "";
    }

    private static void checkSelector(CSSStyleRule nextRule, CSSSelector selector, Map<String,Map<String,CssAssertHelperData>> expected) {
        String selectorName = getSelectorName(selector);

        // Is this selector expected?
        Assert.assertTrue(expected.containsKey(selectorName));
        checkCssDeclarations(nextRule,expected.get(selectorName));
    }

    public static void checkCss(CascadingStyleSheet css, Map<String,Map<String,CssAssertHelperData>> expected) {
        for (CSSStyleRule nextRule : Objects.requireNonNull(css).getAllStyleRules()) {
            CSSSelector selector = nextRule.getSelectorAtIndex(0);

            Assert.assertNotNull(selector);
            checkSelector(nextRule,selector,expected);
        }

        // Verify all not mandatory found
        for(Map.Entry<String,Map<String,CssAssertHelperData>> next : expected.entrySet()) {
            for(Map.Entry<String,CssAssertHelperData> nextItem : next.getValue().entrySet()) {
                Assert.assertTrue(nextItem.getValue().wasNotMandatoryOrFound());
            }
        }
    }

    public static void checkReportCSS(String cssValue) {
        Map<String,Map<String,CssAssertHelper.CssAssertHelperData>> expected = new HashMap<>();
        CssAssertHelper.expectCssBuilder(expected,"body", "font-family", "Arial,Helvetica,sans-serif", true);
        CssAssertHelper.expectCssBuilder(expected,"body", "font-size","12px", true);
        CssAssertHelper.expectCssBuilder(expected,"h1", "font-weight","bolder", true);
        CssAssertHelper.expectCssBuilder(expected,"h1", "font-size","24px", true);
        CssAssertHelper.expectCssBuilder(expected,"h2", "font-weight","bold", true);
        CssAssertHelper.expectCssBuilder(expected,"h2", "font-size","14px", true);
        CssAssertHelper.expectCssBuilder(expected,"table", "border-spacing","0", true);
        CssAssertHelper.expectCssBuilder(expected,"td", "font-size","10px", true);
        CssAssertHelper.expectCssBuilder(expected,"td", "white-space","nowrap", true);
        CssAssertHelper.expectCssBuilder(expected,"td.date", "text-align","right", true);
        CssAssertHelper.expectCssBuilder(expected,"td.description", "white-space","nowrap", true);
        CssAssertHelper.expectCssBuilder(expected,"td.description", "font-size","8px", true);
        CssAssertHelper.expectCssBuilder(expected,"td.amount", "color","#000000", true);
        CssAssertHelper.expectCssBuilder(expected,"td.amount", "text-align","right", true);
        CssAssertHelper.expectCssBuilder(expected,"td.amount-debit", "color","#FF0000", true);
        CssAssertHelper.expectCssBuilder(expected,"td.center-column", "border-right","2px solid darkblue", true);
        CssAssertHelper.expectCssBuilder(expected,"td.center-column", "width","10px", true);
        CssAssertHelper.expectCssBuilder(expected,"th.total-column", "padding-left","30px", true);
        CssAssertHelper.expectCssBuilder(expected,"td.total-row", "border-top","2px solid black", true);
        CssAssertHelper.expectCssBuilder(expected,"td.total-row", "padding-top","4px", true);
        CssAssertHelper.expectCssBuilder(expected,"td.total-row", "font-size","14px", true);
        CssAssertHelper.expectCssBuilder(expected,"td.total-row", "font-weight","bold", true);
        CssAssertHelper.expectCssBuilder(expected,"img.pie", "display","block", true);
        CssAssertHelper.expectCssBuilder(expected,"img.pie", "margin-left","auto", true);
        CssAssertHelper.expectCssBuilder(expected,"img.pie", "margin-right","auto", true);

        CascadingStyleSheet css = CSSReader.readFromString(cssValue, StandardCharsets.UTF_8, ECSSVersion.CSS30);

        CssAssertHelper.checkCss(css,expected);
    }
}
