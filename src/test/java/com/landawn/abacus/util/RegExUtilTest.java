package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
public class RegExUtilTest extends AbstractTest {

    @Test
    public void test_whitespace_pattern() {
        String text = "This is a string  with spaces,\ttabs,\nand newlines.";

        String ret = RegExUtil.removeAll(text, RegExUtil.WHITESPACE_FINDER);
        N.println(ret);

        assertEquals("Thisisastringwithspaces,tabs,andnewlines.", ret);

        ret = RegExUtil.replaceAll(text, RegExUtil.WHITESPACE_FINDER, " ");
        N.println(ret);

        assertEquals("This is a string with spaces, tabs, and newlines.", ret);

        assertFalse(RegExUtil.matches(text, RegExUtil.WHITESPACE_FINDER));
        assertTrue(RegExUtil.find(text, RegExUtil.WHITESPACE_FINDER));

        assertFalse(RegExUtil.matches(text, RegExUtil.WHITESPACE_MATCHER));
        assertFalse(RegExUtil.find(text, RegExUtil.WHITESPACE_MATCHER));

        N.println("a b c".split(" "));
        assertTrue(N.equals(new String[] { "a", "b", "c" }, "a b c".split(" ")));
        assertTrue(N.equals(new String[] { "a", "b", "c" }, "a b c".split(" ", 0)));
        assertTrue(N.equals(new String[] { "a", "b", "c" }, "a b c".split(" ", -1)));
        assertTrue(N.equals(new String[] { "a b c" }, "a b c".split(" ", 1)));
        assertTrue(N.equals(new String[] { "a", "b c" }, "a b c".split(" ", 2)));
        assertTrue(N.equals(new String[] { "a", "b", "c" }, "a b c".split(RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(N.equals(new String[] { "a b c" }, "a b c".split(RegExUtil.WHITESPACE_MATCHER.pattern())));
        assertTrue(N.equals(new String[] { "" }, "".split(" ")));
        assertTrue(N.equals(new String[] { "" }, "".split(RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(N.equals(new String[] { "" }, "".split(RegExUtil.WHITESPACE_MATCHER.pattern())));

        assertTrue(N.equals(new String[] { "" }, RegExUtil.split("", " ")));
        assertTrue(N.equals(new String[] { "" }, RegExUtil.split("", RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(N.equals(new String[] { "" }, RegExUtil.split("", RegExUtil.WHITESPACE_MATCHER.pattern())));
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, RegExUtil.split(null, " ")));
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, RegExUtil.split(null, RegExUtil.WHITESPACE_FINDER.pattern())));
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, RegExUtil.split(null, RegExUtil.WHITESPACE_MATCHER.pattern())));

        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Splitter.with(" ").splitToArray(null)));
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Splitter.with(RegExUtil.WHITESPACE_FINDER).splitToArray(null)));
        assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Splitter.with(RegExUtil.WHITESPACE_MATCHER).splitToArray(null)));

        assertTrue(N.equals(new String[] { "" }, Splitter.with(" ").splitToArray("")));
        assertTrue(N.equals(new String[] { "" }, Splitter.with(RegExUtil.WHITESPACE_FINDER).splitToArray("")));
        assertTrue(N.equals(new String[] { "" }, Splitter.with(RegExUtil.WHITESPACE_MATCHER).splitToArray("")));
    }

}
