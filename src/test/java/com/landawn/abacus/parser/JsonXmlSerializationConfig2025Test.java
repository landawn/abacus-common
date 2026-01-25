package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

@Tag("2025")
public class JsonXmlSerializationConfig2025Test extends TestBase {

    static class TestConfig extends JsonXmlSerializationConfig<TestConfig> {
    }

    @Test
    public void test_getCharQuotation() {
        TestConfig config = new TestConfig();
        assertEquals(WD._QUOTATION_D, config.getCharQuotation());
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
    public void test_setCharQuotation_invalidChar() {
        TestConfig config = new TestConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setCharQuotation('x'));
    }

    @Test
    public void test_getStringQuotation() {
        TestConfig config = new TestConfig();
        assertEquals(WD._QUOTATION_D, config.getStringQuotation());
    }

    @Test
    public void test_setStringQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setStringQuotation('\'');
        assertSame(config, result);
        assertEquals('\'', config.getStringQuotation());
    }

    @Test
    public void test_setStringQuotation_invalidChar() {
        TestConfig config = new TestConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setStringQuotation('x'));
    }

    @Test
    public void test_noCharQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.noCharQuotation();
        assertSame(config, result);
        assertEquals(0, config.getCharQuotation());
    }

    @Test
    public void test_noStringQuotation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.noStringQuotation();
        assertSame(config, result);
        assertEquals(0, config.getStringQuotation());
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
    public void test_setDateTimeFormat() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertSame(config, result);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_prettyFormat() {
        TestConfig config = new TestConfig();
        assertFalse(config.prettyFormat());

        TestConfig result = config.prettyFormat(true);
        assertSame(config, result);
        assertTrue(config.prettyFormat());
    }

    @Test
    public void test_getIndentation() {
        TestConfig config = new TestConfig();
        assertNotNull(config.getIndentation());
    }

    @Test
    public void test_setIndentation() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setIndentation("\t");
        assertSame(config, result);
        assertEquals("\t", config.getIndentation());
    }

    @Test
    public void test_getPropNamingPolicy() {
        TestConfig config = new TestConfig();
        assertEquals(null, config.getPropNamingPolicy());
    }

    @Test
    public void test_setPropNamingPolicy() {
        TestConfig config = new TestConfig();
        TestConfig result = config.setPropNamingPolicy(NamingPolicy.SNAKE_CASE);
        assertSame(config, result);
        assertEquals(NamingPolicy.SNAKE_CASE, config.getPropNamingPolicy());
    }

    @Test
    public void test_writeLongAsString() {
        TestConfig config = new TestConfig();
        assertFalse(config.writeLongAsString());

        TestConfig result = config.writeLongAsString(true);
        assertSame(config, result);
        assertTrue(config.writeLongAsString());
    }

    @Test
    public void test_writeNullStringAsEmpty() {
        TestConfig config = new TestConfig();
        assertFalse(config.writeNullStringAsEmpty());

        TestConfig result = config.writeNullStringAsEmpty(true);
        assertSame(config, result);
        assertTrue(config.writeNullStringAsEmpty());
    }

    @Test
    public void test_writeNullNumberAsZero() {
        TestConfig config = new TestConfig();
        assertFalse(config.writeNullNumberAsZero());

        TestConfig result = config.writeNullNumberAsZero(true);
        assertSame(config, result);
        assertTrue(config.writeNullNumberAsZero());
    }

    @Test
    public void test_writeNullBooleanAsFalse() {
        TestConfig config = new TestConfig();
        assertFalse(config.writeNullBooleanAsFalse());

        TestConfig result = config.writeNullBooleanAsFalse(true);
        assertSame(config, result);
        assertTrue(config.writeNullBooleanAsFalse());
    }

    @Test
    public void test_writeBigDecimalAsPlain() {
        TestConfig config = new TestConfig();
        assertFalse(config.writeBigDecimalAsPlain());

        TestConfig result = config.writeBigDecimalAsPlain(true);
        assertSame(config, result);
        assertTrue(config.writeBigDecimalAsPlain());
    }

    @Test
    public void test_failOnEmptyBean() {
        TestConfig config = new TestConfig();
        assertTrue(config.failOnEmptyBean());

        TestConfig result = config.failOnEmptyBean(false);
        assertSame(config, result);
        assertFalse(config.failOnEmptyBean());
    }

    @Test
    public void test_supportCircularReference() {
        TestConfig config = new TestConfig();
        assertFalse(config.supportCircularReference());

        TestConfig result = config.supportCircularReference(true);
        assertSame(config, result);
        assertTrue(config.supportCircularReference());
    }
}
