package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

public class JsonXmlSerConfigTest extends TestBase {

    public static class TestConfig extends JsonXmlSerConfig<TestConfig> {
    }

    private TestConfig config;

    @BeforeEach
    public void setUp() {
        config = new TestConfig();
    }

    @Test
    public void test_getCharQuotation() {
        TestConfig config = new TestConfig();
        assertEquals(SK._DOUBLE_QUOTE, config.getCharQuotation());
    }

    @Test
    public void testGetCharQuotation() {
        assertEquals(SK._DOUBLE_QUOTE, config.getCharQuotation());
    }

    @Test
    public void testDefaultValues() {
        TestConfig newConfig = new TestConfig();

        assertEquals(SK._DOUBLE_QUOTE, newConfig.getCharQuotation());
        assertEquals(SK._DOUBLE_QUOTE, newConfig.getStringQuotation());
        assertEquals(DateTimeFormat.LONG, newConfig.getDateTimeFormat());
        assertFalse(newConfig.isPrettyFormat());
        assertEquals("    ", newConfig.getIndentation());
        assertNull(newConfig.getPropNamingPolicy());
        assertFalse(newConfig.isWriteLongAsString());
        assertFalse(newConfig.isWriteNullStringAsEmpty());
        assertFalse(newConfig.isWriteNullNumberAsZero());
        assertFalse(newConfig.isWriteNullBooleanAsFalse());
        assertFalse(newConfig.isWriteBigDecimalAsPlain());
        assertTrue(newConfig.isFailOnEmptyBean());
        assertFalse(newConfig.isCircularReferenceSupported());
    }

    @Test
    public void testSetCharQuotation() {
        config.setCharQuotation('\'');
        assertEquals('\'', config.getCharQuotation());

        config.setCharQuotation('"');
        assertEquals('"', config.getCharQuotation());

        config.setCharQuotation((char) 0);
        assertEquals((char) 0, config.getCharQuotation());
    }

    @Test
    public void test_setCharQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setCharQuotation('\'');
        assertSame(config, result);
        assertEquals('\'', config.getCharQuotation());

        config.setCharQuotation('"');
        assertEquals('"', config.getCharQuotation());

        config.setCharQuotation((char) 0);
        assertEquals(0, config.getCharQuotation());
    }

    @Test
    public void testSetCharQuotationZero() {
        config.setCharQuotation((char) 0);
        assertEquals((char) 0, config.getCharQuotation());
    }

    @Test
    public void test_setCharQuotation_invalidChar() {
        TestConfig config = new TestConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('x'));
    }

    @Test
    public void testSetCharQuotationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('a'));
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('@'));
    }

    @Test
    public void test_getStringQuotation() {
        TestConfig config = new TestConfig();
        assertEquals(SK._DOUBLE_QUOTE, config.getStringQuotation());
    }

    @Test
    public void testGetStringQuotation() {
        assertEquals(SK._DOUBLE_QUOTE, config.getStringQuotation());
    }

    @Test
    public void testSetStringQuotation() {
        config.setStringQuotation('\'');
        assertEquals('\'', config.getStringQuotation());

        config.setStringQuotation('"');
        assertEquals('"', config.getStringQuotation());

        config.setStringQuotation((char) 0);
        assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void test_setStringQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setStringQuotation('\'');
        assertSame(config, result);
        assertEquals('\'', config.getStringQuotation());
    }

    @Test
    public void testSetStringQuotationZero() {
        config.setStringQuotation((char) 0);
        assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void test_setStringQuotation_invalidChar() {
        TestConfig config = new TestConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setStringQuotation('x'));
    }

    @Test
    public void testSetStringQuotationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> config.setStringQuotation('x'));
        assertThrows(IllegalArgumentException.class, () -> config.setStringQuotation('#'));
    }

    @Test
    public void testNoCharQuotation() {
        config.noCharQuotation();
        assertEquals((char) 0, config.getCharQuotation());
    }

    @Test
    public void test_noCharQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.noCharQuotation();
        assertSame(config, result);
        assertEquals(0, config.getCharQuotation());
    }

    @Test
    public void testNoStringQuotation() {
        config.noStringQuotation();
        assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void test_noStringQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.noStringQuotation();
        assertSame(config, result);
        assertEquals(0, config.getStringQuotation());
    }

    @Test
    public void testNoQuotation() {
        config.noQuotation();
        assertEquals((char) 0, config.getCharQuotation());
        assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void test_noQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.noQuotation();
        assertSame(config, result);
        assertEquals(0, config.getCharQuotation());
        assertEquals(0, config.getStringQuotation());
    }

    @Test
    public void test_getDateTimeFormat() {
        TestConfig config = new TestConfig();
        assertEquals(DateTimeFormat.LONG, config.getDateTimeFormat());
    }

    @Test
    public void testGetDateTimeFormat() {
        assertEquals(DateTimeFormat.LONG, config.getDateTimeFormat());
    }

    @Test
    public void testSetDateTimeFormat() {
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());

        config.setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config.getDateTimeFormat());
    }

    @Test
    public void testSetDateTimeFormatAllValues() {
        for (DateTimeFormat format : DateTimeFormat.values()) {
            config.setDateTimeFormat(format);
            assertEquals(format, config.getDateTimeFormat());
        }
    }

    @Test
    public void test_setDateTimeFormat() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertSame(config, result);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void testPrettyFormat() {
        assertFalse(config.isPrettyFormat());

        config.setPrettyFormat(true);
        assertTrue(config.isPrettyFormat());

        config.setPrettyFormat(false);
        assertFalse(config.isPrettyFormat());
    }

    @Test
    public void test_prettyFormat() {
        TestConfig config = new TestConfig();
        assertFalse(config.isPrettyFormat());

        TestConfig result = config.setPrettyFormat(true);
        assertSame(config, result);
        assertTrue(config.isPrettyFormat());
    }

    @Test
    public void testGetIndentation() {
        assertEquals("    ", config.getIndentation());
    }

    @Test
    public void test_getIndentation() {
        TestConfig config = new TestConfig();
        assertNotNull(config.getIndentation());
    }

    @Test
    public void testSetIndentation() {
        config.setIndentation("\t");
        assertEquals("\t", config.getIndentation());

        config.setIndentation("  ");
        assertEquals("  ", config.getIndentation());
    }

    @Test
    public void test_setIndentation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setIndentation("\t");
        assertSame(config, result);
        assertEquals("\t", config.getIndentation());
    }

    @Test
    public void testSetIndentationEmpty() {
        config.setIndentation("");
        assertEquals("", config.getIndentation());
    }

    @Test
    public void test_getPropNamingPolicy() {
        TestConfig config = new TestConfig();
        assertEquals(null, config.getPropNamingPolicy());
    }

    @Test
    public void testGetPropNamingPolicy() {
        assertNull(config.getPropNamingPolicy());
    }

    @Test
    public void test_setPropNamingPolicy() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
        assertSame(config, result);
        assertEquals(NamingPolicy.SNAKE_CASE, config.getPropNamingPolicy());
    }

    @Test
    public void testSetPropNamingPolicy() {
        config.setPropNamingPolicy(NamingPolicy.CAMEL_CASE);
        assertEquals(NamingPolicy.CAMEL_CASE, config.getPropNamingPolicy());

        config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
        assertEquals(NamingPolicy.SNAKE_CASE, config.getPropNamingPolicy());
    }

    @Test
    public void testSetPropNamingPolicyNull() {
        config.setPropNamingPolicy(NamingPolicy.CAMEL_CASE);
        assertNotNull(config.getPropNamingPolicy());

        config.setPropNamingPolicy(null);
        assertNull(config.getPropNamingPolicy());
    }

    @Test
    public void testWriteLongAsStringToggle() {
        assertFalse(config.isWriteLongAsString());
        config.setWriteLongAsString(true);
        assertTrue(config.isWriteLongAsString());
        config.setWriteLongAsString(false);
        assertFalse(config.isWriteLongAsString());
    }

    @Test
    public void test_writeLongAsString() {
        TestConfig config = new TestConfig();
        assertFalse(config.isWriteLongAsString());

        TestConfig result = config.setWriteLongAsString(true);
        assertSame(config, result);
        assertTrue(config.isWriteLongAsString());
    }

    @Test
    public void test_writeNullStringAsEmpty() {
        TestConfig config = new TestConfig();
        assertFalse(config.isWriteNullStringAsEmpty());

        TestConfig result = config.setWriteNullStringAsEmpty(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullStringAsEmpty());
    }

    @Test
    public void testWriteNullStringAsEmptyToggle() {
        assertFalse(config.isWriteNullStringAsEmpty());
        config.setWriteNullStringAsEmpty(true);
        assertTrue(config.isWriteNullStringAsEmpty());
        config.setWriteNullStringAsEmpty(false);
        assertFalse(config.isWriteNullStringAsEmpty());
    }

    @Test
    public void test_writeNullNumberAsZero() {
        TestConfig config = new TestConfig();
        assertFalse(config.isWriteNullNumberAsZero());

        TestConfig result = config.setWriteNullNumberAsZero(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullNumberAsZero());
    }

    @Test
    public void testWriteNullNumberAsZeroToggle() {
        assertFalse(config.isWriteNullNumberAsZero());
        config.setWriteNullNumberAsZero(true);
        assertTrue(config.isWriteNullNumberAsZero());
        config.setWriteNullNumberAsZero(false);
        assertFalse(config.isWriteNullNumberAsZero());
    }

    @Test
    public void test_writeNullBooleanAsFalse() {
        TestConfig config = new TestConfig();
        assertFalse(config.isWriteNullBooleanAsFalse());

        TestConfig result = config.setWriteNullBooleanAsFalse(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullBooleanAsFalse());
    }

    @Test
    public void testWriteNullBooleanAsFalseToggle() {
        assertFalse(config.isWriteNullBooleanAsFalse());
        config.setWriteNullBooleanAsFalse(true);
        assertTrue(config.isWriteNullBooleanAsFalse());
        config.setWriteNullBooleanAsFalse(false);
        assertFalse(config.isWriteNullBooleanAsFalse());
    }

    @Test
    public void testWriteBigDecimalAsPlainToggle() {
        assertFalse(config.isWriteBigDecimalAsPlain());
        config.setWriteBigDecimalAsPlain(true);
        assertTrue(config.isWriteBigDecimalAsPlain());
        config.setWriteBigDecimalAsPlain(false);
        assertFalse(config.isWriteBigDecimalAsPlain());
    }

    @Test
    public void test_writeBigDecimalAsPlain() {
        TestConfig config = new TestConfig();
        assertFalse(config.isWriteBigDecimalAsPlain());

        TestConfig result = config.setWriteBigDecimalAsPlain(true);
        assertSame(config, result);
        assertTrue(config.isWriteBigDecimalAsPlain());
    }

    @Test
    public void test_failOnEmptyBean() {
        TestConfig config = new TestConfig();
        assertTrue(config.isFailOnEmptyBean());

        TestConfig result = config.setFailOnEmptyBean(false);
        assertSame(config, result);
        assertFalse(config.isFailOnEmptyBean());
    }

    @Test
    public void testFailOnEmptyBeanToggle() {
        assertTrue(config.isFailOnEmptyBean());
        config.setFailOnEmptyBean(false);
        assertFalse(config.isFailOnEmptyBean());
        config.setFailOnEmptyBean(true);
        assertTrue(config.isFailOnEmptyBean());
    }

    @Test
    public void testCircularReferenceSupportedToggle() {
        assertFalse(config.isCircularReferenceSupported());
        config.setCircularReferenceSupported(true);
        assertTrue(config.isCircularReferenceSupported());
        config.setCircularReferenceSupported(false);
        assertFalse(config.isCircularReferenceSupported());
    }

    @Test
    public void test_circularReferenceSupported() {
        TestConfig config = new TestConfig();
        assertFalse(config.isCircularReferenceSupported());

        TestConfig result = config.setCircularReferenceSupported(true);
        assertSame(config, result);
        assertTrue(config.isCircularReferenceSupported());
    }

    @Test
    public void testMethodChaining() {
        TestConfig result = config.setCharQuotation('\'')
                .setStringQuotation('\'')
                .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
                .setPrettyFormat(true)
                .setIndentation("\t")
                .setPropNamingPolicy(NamingPolicy.SCREAMING_SNAKE_CASE)
                .setWriteLongAsString(true)
                .setWriteNullStringAsEmpty(true)
                .setWriteNullNumberAsZero(true)
                .setWriteNullBooleanAsFalse(true)
                .setWriteBigDecimalAsPlain(true)
                .setFailOnEmptyBean(false)
                .setCircularReferenceSupported(true);

        assertSame(config, result);
        assertEquals('\'', config.getCharQuotation());
        assertEquals('\'', config.getStringQuotation());
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
        assertTrue(config.isPrettyFormat());
        assertEquals("\t", config.getIndentation());
        assertEquals(NamingPolicy.SCREAMING_SNAKE_CASE, config.getPropNamingPolicy());
        assertTrue(config.isWriteLongAsString());
        assertTrue(config.isWriteNullStringAsEmpty());
        assertTrue(config.isWriteNullNumberAsZero());
        assertTrue(config.isWriteNullBooleanAsFalse());
        assertTrue(config.isWriteBigDecimalAsPlain());
        assertFalse(config.isFailOnEmptyBean());
        assertTrue(config.isCircularReferenceSupported());
    }

    @Test
    public void testDefaultStaticConstants() {
        assertEquals(DateTimeFormat.LONG, JsonXmlSerConfig.defaultDateTimeFormat);
        assertFalse(JsonXmlSerConfig.defaultPrettyFormat);
        assertFalse(JsonXmlSerConfig.defaultWriteBigDecimalAsPlain);
        assertEquals("    ", JsonXmlSerConfig.defaultIndentation);
    }

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P8-01 setIndentation validation; P8-04/P8-07/P8-15 javadoc pins)
    // ---------------------------------------------------------------------------------------------

    public static class NullHolder {
        private String s;
        private List<String> l;
        private Map<String, Integer> m;
        private Integer i;
        private Boolean b;
        private NullHolder nested;
        private List<String> tags = N.asList("a", null);
        private List<Integer> nums = N.asList(1, null);
        private List<Boolean> flags = N.asList(true, null);

        public String getS() {
            return s;
        }

        public void setS(final String s) {
            this.s = s;
        }

        public List<String> getL() {
            return l;
        }

        public void setL(final List<String> l) {
            this.l = l;
        }

        public Map<String, Integer> getM() {
            return m;
        }

        public void setM(final Map<String, Integer> m) {
            this.m = m;
        }

        public Integer getI() {
            return i;
        }

        public void setI(final Integer i) {
            this.i = i;
        }

        public Boolean getB() {
            return b;
        }

        public void setB(final Boolean b) {
            this.b = b;
        }

        public NullHolder getNested() {
            return nested;
        }

        public void setNested(final NullHolder nested) {
            this.nested = nested;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }

        public List<Integer> getNums() {
            return nums;
        }

        public void setNums(final List<Integer> nums) {
            this.nums = nums;
        }

        public List<Boolean> getFlags() {
            return flags;
        }

        public void setFlags(final List<Boolean> flags) {
            this.flags = flags;
        }
    }

    @JsonXmlConfig(namingPolicy = NamingPolicy.SNAKE_CASE)
    public static class SnakeBean {
        private String firstName = "a";

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }

    public static class DateHolder {
        private Date d = new Date(0);

        public Date getD() {
            return d;
        }

        public void setD(final Date d) {
            this.d = d;
        }
    }

    @Test
    public void reviewFixes20260906_setIndentation_nullRejected() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> config.setIndentation(null));
        assertTrue(e.getMessage().contains("indentation"), e.getMessage());
        // the rejected value must not have been stored
        assertEquals(JsonXmlSerConfig.defaultIndentation, config.getIndentation());

        assertThrows(IllegalArgumentException.class, () -> new JsonSerConfig().setIndentation(null));
        assertThrows(IllegalArgumentException.class, () -> new XmlSerConfig().setIndentation(null));
    }

    @Test
    public void reviewFixes20260906_setIndentation_markupAndTextRejected() {
        for (final String bad : new String[] { "<evil/>", "x", " x ", "\"z\": 0, ", "\t-", "/*", "  \n#" }) {
            config.setIndentation("  ");
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> config.setIndentation(bad), bad);
            assertTrue(e.getMessage().contains("space, tab, CR or LF"), e.getMessage());
            assertEquals("  ", config.getIndentation(), "value must be unchanged after rejecting: " + bad);
        }
    }

    @Test
    public void reviewFixes20260906_setIndentation_otherUnicodeWhitespaceRejected() {
        // These pass Character.isWhitespace / Strings.isBlank but are rejected by the XML reader (U+000B, U+000C, U+001C)
        // or the JSON reader (U+2028), or are not whitespace to either (U+00A0, U+FEFF).
        for (final String bad : new String[] { "\u2028", "\f", "\u000B", "\u001C", "\u00A0", "\uFEFF", " \u2029" }) {
            config.setIndentation("\t");
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> config.setIndentation(bad));
            assertTrue(e.getMessage().contains("\\u"), e.getMessage());
            assertEquals("\t", config.getIndentation());
        }
    }

    @Test
    public void reviewFixes20260906_setIndentation_legalValuesAccepted() {
        for (final String ok : new String[] { "", " ", "  ", "\t", "\r\n", "\n", "\r", "  \t", "    ", "\t\t" }) {
            assertSame(config, config.setIndentation(ok));
            assertEquals(ok, config.getIndentation());
        }
    }

    @Test
    public void reviewFixes20260906_setIndentation_prettyPrintRoundTripWithLegalIndentation() {
        final JsonParser jp = ParserFactory.createJsonParser();
        final XmlParser xp = ParserFactory.createXmlParser();

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", N.asList(1, 2));

        for (final String ind : new String[] { "", "\t", "  ", "\r\n" }) {
            final String json = jp.serialize(map, new JsonSerConfig().setPrettyFormat(true).setIndentation(ind));
            assertFalse(json.contains("null"), json);
            assertTrue(json.contains("\n" + ind + "\"a\": 1"), json);
            assertEquals("{a=1, b=[1, 2]}", jp.deserialize(json, Map.class).toString());

            final String xml = xp.serialize(map, new XmlSerConfig().setPrettyFormat(true).setIndentation(ind));
            assertFalse(xml.contains("null"), xml);
            assertTrue(xml.contains("\n" + ind + "<a>1</a>"), xml);
            assertEquals("{a=1, b=[1, 2]}", xp.deserialize(xml, Map.class).toString());
        }

        // the injection shape from the review is no longer reachable
        assertThrows(IllegalArgumentException.class, () -> new XmlSerConfig().setPrettyFormat(true).setIndentation("<evil/>"));
    }

    @Test
    public void reviewFixes20260906_dateTimeFormat_nullIsDistinctFromLongDefault() {
        assertEquals(DateTimeFormat.LONG, config.getDateTimeFormat());
        assertSame(config, config.setDateTimeFormat(null));
        assertNull(config.getDateTimeFormat());

        final JsonParser jp = ParserFactory.createJsonParser();
        final DateHolder h = new DateHolder();
        assertEquals("{\"d\": 0}", jp.serialize(h, new JsonSerConfig()));
        assertEquals("{\"d\": 0}", jp.serialize(h, new JsonSerConfig().setDateTimeFormat(DateTimeFormat.LONG)));
        assertEquals("{\"d\": \"1970-01-01T00:00:00Z\"}", jp.serialize(h, new JsonSerConfig().setDateTimeFormat(null)));
        assertEquals("{\"d\": \"1970-01-01T00:00:00Z\"}", jp.serialize(h, new JsonSerConfig().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)));
    }

    @Test
    public void reviewFixes20260906_writeNullFlags_scope_beanPropsUnderExclusionNoneAndTypedElements() {
        final JsonParser jp = ParserFactory.createJsonParser();
        final JsonSerConfig all3 = new JsonSerConfig().setExclusion(Exclusion.NONE)
                .setWriteNullStringAsEmpty(true)
                .setWriteNullNumberAsZero(true)
                .setWriteNullBooleanAsFalse(true);

        // Exclusion.NONE: String/Integer/Boolean properties -> "", 0, false; List/Map/nested bean stay null;
        // typed elements -> "", 0, false
        assertEquals(
                "{\"s\": \"\", \"l\": null, \"m\": null, \"i\": 0, \"b\": false, \"nested\": null, \"tags\": [\"a\", \"\"], \"nums\": [1, 0], \"flags\": [true, false]}",
                jp.serialize(new NullHolder(), all3));

        // each flag affects only its own slot kind
        assertEquals(
                "{\"s\": \"\", \"l\": null, \"m\": null, \"i\": null, \"b\": null, \"nested\": null, \"tags\": [\"a\", \"\"], \"nums\": [1, null], \"flags\": [true, null]}",
                jp.serialize(new NullHolder(), new JsonSerConfig().setExclusion(Exclusion.NONE).setWriteNullStringAsEmpty(true)));
        assertEquals(
                "{\"s\": null, \"l\": null, \"m\": null, \"i\": 0, \"b\": null, \"nested\": null, \"tags\": [\"a\", null], \"nums\": [1, 0], \"flags\": [true, null]}",
                jp.serialize(new NullHolder(), new JsonSerConfig().setExclusion(Exclusion.NONE).setWriteNullNumberAsZero(true)));
        assertEquals(
                "{\"s\": null, \"l\": null, \"m\": null, \"i\": null, \"b\": false, \"nested\": null, \"tags\": [\"a\", null], \"nums\": [1, null], \"flags\": [true, false]}",
                jp.serialize(new NullHolder(), new JsonSerConfig().setExclusion(Exclusion.NONE).setWriteNullBooleanAsFalse(true)));

        // default exclusion (NULL) drops the null properties before the flags are consulted; elements still honour them
        assertEquals("{\"tags\": [\"a\", \"\"], \"nums\": [1, 0], \"flags\": [true, false]}", jp.serialize(new NullHolder(),
                new JsonSerConfig().setWriteNullStringAsEmpty(true).setWriteNullNumberAsZero(true).setWriteNullBooleanAsFalse(true)));

        // writeNullToEmpty takes precedence only for the types that have an empty form (String/Collection/array/Map);
        // a null Integer/Boolean property still honours writeNullNumberAsZero/writeNullBooleanAsFalse
        assertEquals(
                "{\"s\": \"\", \"l\": [], \"m\": {}, \"i\": 0, \"b\": false, \"nested\": null, \"tags\": [\"a\", \"\"], \"nums\": [1, 0], \"flags\": [true, false]}",
                jp.serialize(new NullHolder(), all3.copy().setWriteNullToEmpty(true)));

        // map values and untyped root collection elements: always null
        final Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", null);
        assertEquals("{\"x\": null}", jp.serialize(m, all3));
        assertEquals("[\"a\", null]", jp.serialize(N.asList("a", null), all3));

        // without the flags every null stays null
        assertEquals(
                "{\"s\": null, \"l\": null, \"m\": null, \"i\": null, \"b\": null, \"nested\": null, \"tags\": [\"a\", null], \"nums\": [1, null], \"flags\": [true, null]}",
                jp.serialize(new NullHolder(), new JsonSerConfig().setExclusion(Exclusion.NONE)));

        // XML serializers do not apply the flags: null properties are empty elements with isNull="true"
        final String xml = ParserFactory.createXmlParser()
                .serialize(new NullHolder(),
                        new XmlSerConfig().setExclusion(Exclusion.NONE)
                                .setWriteNullStringAsEmpty(true)
                                .setWriteNullNumberAsZero(true)
                                .setWriteNullBooleanAsFalse(true));
        assertTrue(xml.contains("<s isNull=\"true\" />"), xml);
        assertTrue(xml.contains("<i isNull=\"true\" />"), xml);
        assertTrue(xml.contains("<b isNull=\"true\" />"), xml);
    }

    @Test
    public void reviewFixes20260906_propNamingPolicy_nullDefersToAnnotation_explicitOverridesIt() {
        final JsonParser jp = ParserFactory.createJsonParser();
        final XmlParser xp = ParserFactory.createXmlParser();

        assertNull(new JsonSerConfig().getPropNamingPolicy());
        // null -> the bean's @JsonXmlConfig(SNAKE_CASE) applies
        assertEquals("{\"first_name\": \"a\"}", jp.serialize(new SnakeBean(), new JsonSerConfig()));
        assertEquals("<snake_bean><first_name>a</first_name></snake_bean>", xp.serialize(new SnakeBean(), new XmlSerConfig()));
        // explicit CAMEL_CASE overrides the annotation (it is not a no-op)
        assertEquals("{\"firstName\": \"a\"}", jp.serialize(new SnakeBean(), new JsonSerConfig().setPropNamingPolicy(NamingPolicy.CAMEL_CASE)));
        final String camelXml = xp.serialize(new SnakeBean(), new XmlSerConfig().setPropNamingPolicy(NamingPolicy.CAMEL_CASE));
        assertTrue(camelXml.contains("<firstName>a</firstName>"), camelXml);
        assertFalse(camelXml.contains("first_name"), camelXml);
        // plain Map keys are never renamed
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("firstName", 1);
        assertEquals("{\"firstName\": 1}", jp.serialize(map, new JsonSerConfig().setPropNamingPolicy(NamingPolicy.SNAKE_CASE)));
        assertEquals("<map><firstName>1</firstName></map>", xp.serialize(map, new XmlSerConfig().setPropNamingPolicy(NamingPolicy.SNAKE_CASE)));
    }

    public static class NullArrayHolder {
        private String s;
        private String[] as = { "a", null };
        private Integer[] ai = { 1, null };
        private Boolean[] ab = { true, null };

        public String getS() {
            return s;
        }

        public void setS(final String s) {
            this.s = s;
        }

        public String[] getAs() {
            return as;
        }

        public void setAs(final String[] as) {
            this.as = as;
        }

        public Integer[] getAi() {
            return ai;
        }

        public void setAi(final Integer[] ai) {
            this.ai = ai;
        }

        public Boolean[] getAb() {
            return ab;
        }

        public void setAb(final Boolean[] ab) {
            this.ab = ab;
        }
    }

    // R6 self-review: the three writeNull* flags never change a null bean PROPERTY in XML (it stays an
    // isNull="true" element), but they do reach a null ELEMENT of a typed array property, because the array
    // is written through its Type. The javadoc of all three setters says exactly this now.
    @Test
    public void reviewFixes20260906_writeNullFlags_xml_propertyUnaffected_typedArrayElementAffected() {
        final XmlSerConfig cfg = new XmlSerConfig().setExclusion(Exclusion.NONE)
                .setWriteNullStringAsEmpty(true)
                .setWriteNullNumberAsZero(true)
                .setWriteNullBooleanAsFalse(true);

        for (final XmlParser xp : new XmlParser[] { ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser() }) {
            final String xml = xp.serialize(new NullArrayHolder(), cfg);

            assertTrue(xml.contains("<s isNull=\"true\" />"), xml);
            assertTrue(xml.contains("<as>[&quot;a&quot;, &quot;&quot;]</as>"), xml);
            assertTrue(xml.contains("<ai>[1, 0]</ai>"), xml);
            assertTrue(xml.contains("<ab>[true, false]</ab>"), xml);

            // without the flags the same elements stay null
            final String plain = xp.serialize(new NullArrayHolder(), new XmlSerConfig().setExclusion(Exclusion.NONE));
            assertTrue(plain.contains("<s isNull=\"true\" />"), plain);
            assertTrue(plain.contains("<as>[&quot;a&quot;, null]</as>"), plain);
            assertTrue(plain.contains("<ai>[1, null]</ai>"), plain);
            assertTrue(plain.contains("<ab>[true, null]</ab>"), plain);
        }
    }

}
