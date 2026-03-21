package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;

public class JsonSerConfigTest extends TestBase {

    private JsonSerConfig config;

    @BeforeEach
    void setUp() {
        config = JsonSerConfig.create();
    }

    @Test
    public void test_prettyFormat() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isPrettyFormat());

        config.setPrettyFormat(true);
        assertTrue(config.isPrettyFormat());
    }

    @Test
    public void test_setDateTimeFormat() {
        JsonSerConfig config = new JsonSerConfig();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }

    @Test
    public void test_constructor() {
        JsonSerConfig config = new JsonSerConfig();
        assertNotNull(config);
        assertTrue(config.isQuotePropName());
        assertTrue(config.isQuoteMapKey());
        assertTrue(config.isBracketRootValue());
        assertFalse(config.isWrapRootValue());
        assertFalse(config.isWriteNullToEmpty());
        assertFalse(config.isWriteDatasetByRow());
    }

    @Test
    public void test_writeNullToEmpty() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isWriteNullToEmpty());

        JsonSerConfig result = config.setWriteNullToEmpty(true);
        assertSame(config, result);
        assertTrue(config.isWriteNullToEmpty());

        config.setWriteNullToEmpty(false);
        assertFalse(config.isWriteNullToEmpty());
    }

    @Test
    public void test_writeDatasetByRow() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isWriteDatasetByRow());

        JsonSerConfig result = config.setWriteDatasetByRow(true);
        assertSame(config, result);
        assertTrue(config.isWriteDatasetByRow());

        config.setWriteDatasetByRow(false);
        assertFalse(config.isWriteDatasetByRow());
    }

    @Test
    public void test_writeRowColumnKeyType() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isWriteRowColumnKeyType());

        JsonSerConfig result = config.setWriteRowColumnKeyType(true);
        assertSame(config, result);
        assertTrue(config.isWriteRowColumnKeyType());
    }

    @Test
    public void test_writeColumnType() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isWriteColumnType());

        JsonSerConfig result = config.setWriteColumnType(true);
        assertSame(config, result);
        assertTrue(config.isWriteColumnType());
    }

    @Test
    public void test_quotePropName() {
        JsonSerConfig config = new JsonSerConfig();
        assertTrue(config.isQuotePropName());

        JsonSerConfig result = config.setQuotePropName(false);
        assertSame(config, result);
        assertFalse(config.isQuotePropName());
    }

    @Test
    public void test_quoteMapKey() {
        JsonSerConfig config = new JsonSerConfig();
        assertTrue(config.isQuoteMapKey());

        JsonSerConfig result = config.setQuoteMapKey(false);
        assertSame(config, result);
        assertFalse(config.isQuoteMapKey());
    }

    @Test
    public void test_bracketRootValue() {
        JsonSerConfig config = new JsonSerConfig();
        assertTrue(config.isBracketRootValue());

        JsonSerConfig result = config.setBracketRootValue(false);
        assertSame(config, result);
        assertFalse(config.isBracketRootValue());
    }

    @Test
    public void test_wrapRootValue() {
        JsonSerConfig config = new JsonSerConfig();
        assertFalse(config.isWrapRootValue());

        JsonSerConfig result = config.setWrapRootValue(true);
        assertSame(config, result);
        assertTrue(config.isWrapRootValue());
    }

    @Test
    public void testWriteNullToEmpty() {
        Assertions.assertFalse(config.isWriteNullToEmpty());

        JsonSerConfig result = config.setWriteNullToEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isWriteNullToEmpty());

        config.setWriteNullToEmpty(false);
        Assertions.assertFalse(config.isWriteNullToEmpty());
    }

    @Test
    public void testWriteDatasetByRow() {
        Assertions.assertFalse(config.isWriteDatasetByRow());

        JsonSerConfig result = config.setWriteDatasetByRow(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isWriteDatasetByRow());

        config.setWriteDatasetByRow(false);
        Assertions.assertFalse(config.isWriteDatasetByRow());
    }

    @Test
    public void testWriteRowColumnKeyType() {
        Assertions.assertFalse(config.isWriteRowColumnKeyType());

        JsonSerConfig result = config.setWriteRowColumnKeyType(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isWriteRowColumnKeyType());

        config.setWriteRowColumnKeyType(false);
        Assertions.assertFalse(config.isWriteRowColumnKeyType());
    }

    @Test
    public void testWriteColumnType() {
        Assertions.assertFalse(config.isWriteColumnType());

        JsonSerConfig result = config.setWriteColumnType(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isWriteColumnType());

        config.setWriteColumnType(false);
        Assertions.assertFalse(config.isWriteColumnType());
    }

    @Test
    public void testSetCharQuotation() {
        JsonSerConfig result = config.setCharQuotation('\'');
        Assertions.assertSame(config, result);
        Assertions.assertEquals('\'', config.getCharQuotation());
    }

    @Test
    public void testSetStringQuotation() {
        JsonSerConfig result = config.setStringQuotation('\'');
        Assertions.assertSame(config, result);
        Assertions.assertEquals('\'', config.getStringQuotation());
    }

    @Test
    public void testNoCharQuotation() {
        JsonSerConfig result = config.noCharQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getCharQuotation());
    }

    @Test
    public void testNoStringQuotation() {
        JsonSerConfig result = config.noStringQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void testNoQuotation() {
        JsonSerConfig result = config.noQuotation();
        Assertions.assertSame(config, result);
        Assertions.assertEquals((char) 0, config.getCharQuotation());
        Assertions.assertEquals((char) 0, config.getStringQuotation());
    }

    @Test
    public void testQuotePropName() {
        Assertions.assertTrue(config.isQuotePropName());

        JsonSerConfig result = config.setQuotePropName(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.isQuotePropName());

        config.setQuotePropName(true);
        Assertions.assertTrue(config.isQuotePropName());
    }

    @Test
    public void testQuoteMapKey() {
        Assertions.assertTrue(config.isQuoteMapKey());

        JsonSerConfig result = config.setQuoteMapKey(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.isQuoteMapKey());

        config.setQuoteMapKey(true);
        Assertions.assertTrue(config.isQuoteMapKey());
    }

    @Test
    public void testBracketRootValue() {
        Assertions.assertTrue(config.isBracketRootValue());

        JsonSerConfig result = config.setBracketRootValue(false);
        Assertions.assertSame(config, result);
        Assertions.assertFalse(config.isBracketRootValue());

        config.setBracketRootValue(true);
        Assertions.assertTrue(config.isBracketRootValue());
    }

    @Test
    public void testWrapRootValue() {
        Assertions.assertFalse(config.isWrapRootValue());

        JsonSerConfig result = config.setWrapRootValue(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isWrapRootValue());

        config.setWrapRootValue(false);
        Assertions.assertFalse(config.isWrapRootValue());
    }

    @Test
    public void test_hashCode() {
        JsonSerConfig config1 = new JsonSerConfig();
        JsonSerConfig config2 = new JsonSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        JsonSerConfig config1 = new JsonSerConfig();
        JsonSerConfig config2 = new JsonSerConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.setQuotePropName(false);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCodeVariesByField() {
        JsonSerConfig base = new JsonSerConfig();
        int baseHash = base.hashCode();

        JsonSerConfig modified = new JsonSerConfig();
        modified.setWriteNullToEmpty(true);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setWriteDatasetByRow(true);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setWriteRowColumnKeyType(true);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setWriteColumnType(true);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setQuoteMapKey(false);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setBracketRootValue(false);
        Assertions.assertNotEquals(baseHash, modified.hashCode());

        modified = new JsonSerConfig();
        modified.setWrapRootValue(true);
        Assertions.assertNotEquals(baseHash, modified.hashCode());
    }

    @Test
    public void testEqualsWithWriteDatasetByRow() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setWriteDatasetByRow(true);
        Assertions.assertNotEquals(c1, c2);

        c1.setWriteDatasetByRow(true);
        Assertions.assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWithWriteRowColumnKeyType() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setWriteRowColumnKeyType(true);
        Assertions.assertNotEquals(c1, c2);

        c1.setWriteRowColumnKeyType(true);
        Assertions.assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWithWriteColumnType() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setWriteColumnType(true);
        Assertions.assertNotEquals(c1, c2);

        c1.setWriteColumnType(true);
        Assertions.assertEquals(c1, c2);
    }

    @Test
    public void testEqualsWithQuoteMapKey() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setQuoteMapKey(false);
        Assertions.assertNotEquals(c1, c2);
    }

    @Test
    public void testEqualsWithBracketRootValue() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setBracketRootValue(false);
        Assertions.assertNotEquals(c1, c2);
    }

    @Test
    public void testEqualsWithWrapRootValue() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setWrapRootValue(true);
        Assertions.assertNotEquals(c1, c2);
    }

    @Test
    public void test_equals() {
        JsonSerConfig config1 = new JsonSerConfig();
        JsonSerConfig config2 = new JsonSerConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));

        config2.setQuotePropName(false);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void testEquals() {
        JsonSerConfig config1 = new JsonSerConfig();
        JsonSerConfig config2 = new JsonSerConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        config1.setQuotePropName(false);
        Assertions.assertNotEquals(config1, config2);

        config2.setQuotePropName(false);
        Assertions.assertEquals(config1, config2);
    }

    @Test
    public void testEqualsWithWriteNullToEmpty() {
        JsonSerConfig c1 = new JsonSerConfig();
        JsonSerConfig c2 = new JsonSerConfig();
        c2.setWriteNullToEmpty(true);
        Assertions.assertNotEquals(c1, c2);

        c1.setWriteNullToEmpty(true);
        Assertions.assertEquals(c1, c2);
    }

    @Test
    public void test_toString() {
        JsonSerConfig config = new JsonSerConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("quotePropName"));
        assertTrue(str.contains("quoteMapKey"));
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
    public void testToStringContainsAllFields() {
        JsonSerConfig c = new JsonSerConfig();
        String str = c.toString();
        Assertions.assertTrue(str.contains("writeNullToEmpty="));
        Assertions.assertTrue(str.contains("writeDatasetByRow="));
        Assertions.assertTrue(str.contains("writeRowColumnKeyType="));
        Assertions.assertTrue(str.contains("writeColumnType="));
        Assertions.assertTrue(str.contains("writeBigDecimalAsPlain="));
        Assertions.assertTrue(str.contains("failOnEmptyBean="));
        Assertions.assertTrue(str.contains("supportCircularReference="));
        Assertions.assertTrue(str.contains("indentation="));
        Assertions.assertTrue(str.contains("propNamingPolicy="));
    }

    @Test
    public void test_JSC_create() {
        JsonSerConfig config = JsonSerConfig.create();
        assertNotNull(config);
    }

    @Test
    public void test_JSC_of_quotePropName_quoteMapKey() {
        JsonSerConfig config = JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(false);
        assertNotNull(config);
        assertTrue(config.isQuotePropName());
        assertFalse(config.isQuoteMapKey());
    }

    @Test
    public void test_JSC_of_dateTimeFormat() {
        JsonSerConfig config = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);
        assertNotNull(config);
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config.getDateTimeFormat());
    }

    @Test
    public void test_JSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        JsonSerConfig config = JsonSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredPropNames);
        assertNotNull(config);
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_JSC_of_all_parameters() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        JsonSerConfig config = JsonSerConfig.create()
                .setQuotePropName(true)
                .setQuoteMapKey(false)
                .setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)
                .setExclusion(Exclusion.NULL)
                .setIgnoredPropNames(ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.isQuotePropName());
        assertFalse(config.isQuoteMapKey());
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config.getDateTimeFormat());
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void testJSCCreate() {
        JsonSerConfig config = JsonSerConfig.create();
        Assertions.assertNotNull(config);
        Assertions.assertTrue(config.isQuotePropName());
        Assertions.assertTrue(config.isQuoteMapKey());
    }

    @Test
    public void testJSCOf() {
        JsonSerConfig config1 = JsonSerConfig.create().setQuotePropName(false).setQuoteMapKey(true);
        Assertions.assertFalse(config1.isQuotePropName());
        Assertions.assertTrue(config1.isQuoteMapKey());

        JsonSerConfig config2 = JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        Assertions.assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config2.getDateTimeFormat());

        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        ignoredProps.put(String.class, new HashSet<>());
        JsonSerConfig config3 = JsonSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredProps);
        Assertions.assertEquals(Exclusion.NULL, config3.getExclusion());

        JsonSerConfig config4 = JsonSerConfig.create()
                .setQuotePropName(true)
                .setQuoteMapKey(false)
                .setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)
                .setExclusion(Exclusion.NULL)
                .setIgnoredPropNames(ignoredProps);
        Assertions.assertTrue(config4.isQuotePropName());
        Assertions.assertFalse(config4.isQuoteMapKey());
        Assertions.assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config4.getDateTimeFormat());
        Assertions.assertEquals(Exclusion.NULL, config4.getExclusion());
    }

    @Test
    public void testMethodChainingAllSetters() {
        JsonSerConfig c = JsonSerConfig.create()
                .setWriteNullToEmpty(true)
                .setWriteDatasetByRow(true)
                .setWriteRowColumnKeyType(true)
                .setWriteColumnType(true)
                .setQuotePropName(false)
                .setQuoteMapKey(false)
                .setBracketRootValue(false)
                .setWrapRootValue(true);

        Assertions.assertTrue(c.isWriteNullToEmpty());
        Assertions.assertTrue(c.isWriteDatasetByRow());
        Assertions.assertTrue(c.isWriteRowColumnKeyType());
        Assertions.assertTrue(c.isWriteColumnType());
        Assertions.assertFalse(c.isQuotePropName());
        Assertions.assertFalse(c.isQuoteMapKey());
        Assertions.assertFalse(c.isBracketRootValue());
        Assertions.assertTrue(c.isWrapRootValue());
    }

}
