package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

@Tag("new-test")
public class JSONXMLSerializationConfig100Test extends TestBase {

    private TestConfig config;

    public static class TestConfig extends JSONXMLSerializationConfig<TestConfig> {
    }

    @BeforeEach
    public void setUp() {
        config = new TestConfig();
    }

    @Test
    public void testGetCharQuotation() {
        assertEquals(WD._QUOTATION_D, config.getCharQuotation());
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
    public void testSetCharQuotationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('a'));
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('@'));
    }

    @Test
    public void testGetStringQuotation() {
        assertEquals(WD._QUOTATION_D, config.getStringQuotation());
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
    public void testNoStringQuotation() {
        config.noStringQuotation();
        assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void testNoQuotation() {
        config.noQuotation();
        assertEquals((char) 0, config.getCharQuotation());
        assertEquals((char) 0, config.getStringQuotation());
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
    public void testPrettyFormat() {
        assertFalse(config.prettyFormat());

        config.prettyFormat(true);
        assertTrue(config.prettyFormat());

        config.prettyFormat(false);
        assertFalse(config.prettyFormat());
    }

    @Test
    public void testGetIndentation() {
        assertEquals("    ", config.getIndentation());
    }

    @Test
    public void testSetIndentation() {
        config.setIndentation("\t");
        assertEquals("\t", config.getIndentation());

        config.setIndentation("  ");
        assertEquals("  ", config.getIndentation());
    }

    @Test
    public void testGetPropNamingPolicy() {
        assertNull(config.getPropNamingPolicy());
    }

    @Test
    public void testSetPropNamingPolicy() {
        config.setPropNamingPolicy(NamingPolicy.LOWER_CAMEL_CASE);
        assertEquals(NamingPolicy.LOWER_CAMEL_CASE, config.getPropNamingPolicy());

        config.setPropNamingPolicy(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
        assertEquals(NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, config.getPropNamingPolicy());
    }

    @Test
    public void testWriteLongAsString() {
        assertFalse(config.writeLongAsString());

        config.writeLongAsString(true);
        assertTrue(config.writeLongAsString());

        config.writeLongAsString(false);
        assertFalse(config.writeLongAsString());
    }

    @Test
    public void testWriteNullStringAsEmpty() {
        assertFalse(config.writeNullStringAsEmpty());

        config.writeNullStringAsEmpty(true);
        assertTrue(config.writeNullStringAsEmpty());

        config.writeNullStringAsEmpty(false);
        assertFalse(config.writeNullStringAsEmpty());
    }

    @Test
    public void testWriteNullNumberAsZero() {
        assertFalse(config.writeNullNumberAsZero());

        config.writeNullNumberAsZero(true);
        assertTrue(config.writeNullNumberAsZero());

        config.writeNullNumberAsZero(false);
        assertFalse(config.writeNullNumberAsZero());
    }

    @Test
    public void testWriteNullBooleanAsFalse() {
        assertFalse(config.writeNullBooleanAsFalse());

        config.writeNullBooleanAsFalse(true);
        assertTrue(config.writeNullBooleanAsFalse());

        config.writeNullBooleanAsFalse(false);
        assertFalse(config.writeNullBooleanAsFalse());
    }

    @Test
    public void testWriteBigDecimalAsPlain() {
        assertFalse(config.writeBigDecimalAsPlain());

        config.writeBigDecimalAsPlain(true);
        assertTrue(config.writeBigDecimalAsPlain());

        config.writeBigDecimalAsPlain(false);
        assertFalse(config.writeBigDecimalAsPlain());
    }

    @Test
    public void testFailOnEmptyBean() {
        assertTrue(config.failOnEmptyBean());

        config.failOnEmptyBean(false);
        assertFalse(config.failOnEmptyBean());

        config.failOnEmptyBean(true);
        assertTrue(config.failOnEmptyBean());
    }

    @Test
    public void testSupportCircularReference() {
        assertFalse(config.supportCircularReference());

        config.supportCircularReference(true);
        assertTrue(config.supportCircularReference());

        config.supportCircularReference(false);
        assertFalse(config.supportCircularReference());
    }

    @Test
    public void testMethodChaining() {
        TestConfig result = config.setCharQuotation('\'')
                .setStringQuotation('\'')
                .setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME)
                .prettyFormat(true)
                .setIndentation("\t")
                .setPropNamingPolicy(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE)
                .writeLongAsString(true)
                .writeNullStringAsEmpty(true)
                .writeNullNumberAsZero(true)
                .writeNullBooleanAsFalse(true)
                .writeBigDecimalAsPlain(true)
                .failOnEmptyBean(false)
                .supportCircularReference(true);

        assertSame(config, result);
        assertEquals('\'', config.getCharQuotation());
        assertEquals('\'', config.getStringQuotation());
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
        assertTrue(config.prettyFormat());
        assertEquals("\t", config.getIndentation());
        assertEquals(NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, config.getPropNamingPolicy());
        assertTrue(config.writeLongAsString());
        assertTrue(config.writeNullStringAsEmpty());
        assertTrue(config.writeNullNumberAsZero());
        assertTrue(config.writeNullBooleanAsFalse());
        assertTrue(config.writeBigDecimalAsPlain());
        assertFalse(config.failOnEmptyBean());
        assertTrue(config.supportCircularReference());
    }

    @Test
    public void testDefaultValues() {
        TestConfig newConfig = new TestConfig();

        assertEquals(WD._QUOTATION_D, newConfig.getCharQuotation());
        assertEquals(WD._QUOTATION_D, newConfig.getStringQuotation());
        assertEquals(DateTimeFormat.LONG, newConfig.getDateTimeFormat());
        assertFalse(newConfig.prettyFormat());
        assertEquals("    ", newConfig.getIndentation());
        assertNull(newConfig.getPropNamingPolicy());
        assertFalse(newConfig.writeLongAsString());
        assertFalse(newConfig.writeNullStringAsEmpty());
        assertFalse(newConfig.writeNullNumberAsZero());
        assertFalse(newConfig.writeNullBooleanAsFalse());
        assertFalse(newConfig.writeBigDecimalAsPlain());
        assertTrue(newConfig.failOnEmptyBean());
        assertFalse(newConfig.supportCircularReference());
    }
}
