package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.SK;

public class JsonXmlSerConfigTest extends TestBase {

    static class TestConfig extends JsonXmlSerConfig<TestConfig> {
    }

    private TestConfig config;

    @BeforeEach
    void setUp() {
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
        assertFalse(newConfig.isSupportCircularReference());
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
    public void testWriteLongAsString() {
        assertFalse(config.isWriteLongAsString());

        config.setWriteLongAsString(true);
        assertTrue(config.isWriteLongAsString());

        config.setWriteLongAsString(false);
        assertFalse(config.isWriteLongAsString());
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
    public void testWriteNullStringAsEmpty() {
        assertFalse(config.isWriteNullStringAsEmpty());

        config.setWriteNullStringAsEmpty(true);
        assertTrue(config.isWriteNullStringAsEmpty());

        config.setWriteNullStringAsEmpty(false);
        assertFalse(config.isWriteNullStringAsEmpty());
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
    public void testWriteNullNumberAsZero() {
        assertFalse(config.isWriteNullNumberAsZero());

        config.setWriteNullNumberAsZero(true);
        assertTrue(config.isWriteNullNumberAsZero());

        config.setWriteNullNumberAsZero(false);
        assertFalse(config.isWriteNullNumberAsZero());
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
    public void testWriteNullBooleanAsFalse() {
        assertFalse(config.isWriteNullBooleanAsFalse());

        config.setWriteNullBooleanAsFalse(true);
        assertTrue(config.isWriteNullBooleanAsFalse());

        config.setWriteNullBooleanAsFalse(false);
        assertFalse(config.isWriteNullBooleanAsFalse());
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
    public void testWriteBigDecimalAsPlain() {
        assertFalse(config.isWriteBigDecimalAsPlain());

        config.setWriteBigDecimalAsPlain(true);
        assertTrue(config.isWriteBigDecimalAsPlain());

        config.setWriteBigDecimalAsPlain(false);
        assertFalse(config.isWriteBigDecimalAsPlain());
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
    public void testFailOnEmptyBean() {
        assertTrue(config.isFailOnEmptyBean());

        config.setFailOnEmptyBean(false);
        assertFalse(config.isFailOnEmptyBean());

        config.setFailOnEmptyBean(true);
        assertTrue(config.isFailOnEmptyBean());
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
    public void testSupportCircularReference() {
        assertFalse(config.isSupportCircularReference());

        config.setSupportCircularReference(true);
        assertTrue(config.isSupportCircularReference());

        config.setSupportCircularReference(false);
        assertFalse(config.isSupportCircularReference());
    }

    @Test
    public void testSupportCircularReferenceToggle() {
        assertFalse(config.isSupportCircularReference());
        config.setSupportCircularReference(true);
        assertTrue(config.isSupportCircularReference());
        config.setSupportCircularReference(false);
        assertFalse(config.isSupportCircularReference());
    }

    @Test
    public void test_supportCircularReference() {
        TestConfig config = new TestConfig();
        assertFalse(config.isSupportCircularReference());

        TestConfig result = config.setSupportCircularReference(true);
        assertSame(config, result);
        assertTrue(config.isSupportCircularReference());
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
                .setSupportCircularReference(true);

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
        assertTrue(config.isSupportCircularReference());
    }

    @Test
    public void testDefaultStaticConstants() {
        assertEquals(DateTimeFormat.LONG, JsonXmlSerConfig.defaultDateTimeFormat);
        assertFalse(JsonXmlSerConfig.defaultPrettyFormat);
        assertFalse(JsonXmlSerConfig.defaultWriteBigDecimalAsPlain);
        assertEquals("    ", JsonXmlSerConfig.defaultIndentation);
    }

}
