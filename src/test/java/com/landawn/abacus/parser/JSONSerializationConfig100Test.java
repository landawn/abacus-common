package com.landawn.abacus.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("new-test")
public class JSONSerializationConfig100Test extends TestBase {

    private JSONSerializationConfig config;

    @BeforeEach
    public void setUp() {
        config = new JSONSerializationConfig();
    }

    @Test
    public void testConstructor() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        Assertions.assertNotNull(config);
        Assertions.assertTrue(config.quotePropName());
        Assertions.assertTrue(config.quoteMapKey());
        Assertions.assertTrue(config.bracketRootValue());
        Assertions.assertFalse(config.wrapRootValue());
        Assertions.assertFalse(config.writeNullToEmpty());
        Assertions.assertFalse(config.writeDatasetByRow());
    }

    @Test
    public void testWriteNullToEmpty() {
        Assertions.assertFalse(config.writeNullToEmpty());

        JSONSerializationConfig result = config.writeNullToEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.writeNullToEmpty());

        config.writeNullToEmpty(false);
        Assertions.assertFalse(config.writeNullToEmpty());
    }

    @Test
    public void testWriteDatasetByRow() {
        Assertions.assertFalse(config.writeDatasetByRow());

        JSONSerializationConfig result = config.writeDatasetByRow(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.writeDatasetByRow());

        config.writeDatasetByRow(false);
        Assertions.assertFalse(config.writeDatasetByRow());
    }

    @Test
    public void testWriteRowColumnKeyType() {
        Assertions.assertFalse(config.writeRowColumnKeyType());

        JSONSerializationConfig result = config.writeRowColumnKeyType(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.writeRowColumnKeyType());

        config.writeRowColumnKeyType(false);
        Assertions.assertFalse(config.writeRowColumnKeyType());
    }

    @Test
    public void testWriteColumnType() {
        Assertions.assertFalse(config.writeColumnType());

        JSONSerializationConfig result = config.writeColumnType(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.writeColumnType());

        config.writeColumnType(false);
        Assertions.assertFalse(config.writeColumnType());
    }

    @Test
    public void testSetCharQuotation() {
        JSONSerializationConfig result = config.setCharQuotation('\'');
        Assertions.assertSame(config, result);
        Assertions.assertEquals('\'', config.getCharQuotation());
    }

    @Test
    public void testSetStringQuotation() {
        JSONSerializationConfig result = config.setStringQuotation('\'');
        Assertions.assertSame(config, result);
        Assertions.assertEquals('\'', config.getStringQuotation());
    }

    @Test
    public void testNoCharQuotation() {
        JSONSerializationConfig result = config.noCharQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getCharQuotation());
    }

    @Test
    public void testNoStringQuotation() {
        JSONSerializationConfig result = config.noStringQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void testNoQuotation() {
        JSONSerializationConfig result = config.noQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getCharQuotation());
        Assertions.assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void testQuotePropName() {
        Assertions.assertTrue(config.quotePropName());

        JSONSerializationConfig result = config.quotePropName(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.quotePropName());

        config.quotePropName(true);
        Assertions.assertTrue(config.quotePropName());
    }

    @Test
    public void testQuoteMapKey() {
        Assertions.assertTrue(config.quoteMapKey());

        JSONSerializationConfig result = config.quoteMapKey(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.quoteMapKey());

        config.quoteMapKey(true);
        Assertions.assertTrue(config.quoteMapKey());
    }

    @Test
    public void testBracketRootValue() {
        Assertions.assertTrue(config.bracketRootValue());

        JSONSerializationConfig result = config.bracketRootValue(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.bracketRootValue());

        config.bracketRootValue(true);
        Assertions.assertTrue(config.bracketRootValue());
    }

    @Test
    public void testWrapRootValue() {
        Assertions.assertFalse(config.wrapRootValue());

        JSONSerializationConfig result = config.wrapRootValue(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.wrapRootValue());

        config.wrapRootValue(false);
        Assertions.assertFalse(config.wrapRootValue());
    }

    @Test
    public void testHashCode() {
        JSONSerializationConfig config1 = new JSONSerializationConfig();
        JSONSerializationConfig config2 = new JSONSerializationConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.quotePropName(false);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        JSONSerializationConfig config1 = new JSONSerializationConfig();
        JSONSerializationConfig config2 = new JSONSerializationConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        config1.quotePropName(false);
        Assertions.assertNotEquals(config1, config2);

        config2.quotePropName(false);
        Assertions.assertEquals(config1, config2);
    }

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("quotePropName="));
        Assertions.assertTrue(str.contains("quoteMapKey="));
        Assertions.assertTrue(str.contains("bracketRootValue="));
        Assertions.assertTrue(str.contains("wrapRootValue="));
    }

    @Test
    public void testJSCCreate() {
        JSONSerializationConfig config = JSC.create();
        Assertions.assertNotNull(config);
        Assertions.assertTrue(config.quotePropName());
        Assertions.assertTrue(config.quoteMapKey());
    }

    @Test
    public void testJSCOf() {
        JSONSerializationConfig config1 = JSC.of(false, true);
        Assertions.assertFalse(config1.quotePropName());
        Assertions.assertTrue(config1.quoteMapKey());

        JSONSerializationConfig config2 = JSC.of(DateTimeFormat.ISO_8601_DATE_TIME);
        Assertions.assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config2.getDateTimeFormat());

        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(String.class, new HashSet<>());
        JSONSerializationConfig config3 = JSC.of(Exclusion.NULL, ignoredProps);
        Assertions.assertEquals(Exclusion.NULL, config3.getExclusion());

        JSONSerializationConfig config4 = JSC.of(true, false, DateTimeFormat.ISO_8601_TIMESTAMP, Exclusion.NULL, ignoredProps);
        Assertions.assertTrue(config4.quotePropName());
        Assertions.assertFalse(config4.quoteMapKey());
        Assertions.assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config4.getDateTimeFormat());
        Assertions.assertEquals(Exclusion.NULL, config4.getExclusion());
    }
}
