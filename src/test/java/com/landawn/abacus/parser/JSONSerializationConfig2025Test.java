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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("2025")
public class JSONSerializationConfig2025Test extends TestBase {

    @Test
    public void test_constructor() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertNotNull(config);
        assertTrue(config.quotePropName());
        assertTrue(config.quoteMapKey());
        assertTrue(config.bracketRootValue());
        assertFalse(config.wrapRootValue());
        assertFalse(config.writeNullToEmpty());
        assertFalse(config.writeDatasetByRow());
    }

    @Test
    public void test_JSC_create() {
        JSONSerializationConfig config = JSONSerializationConfig.JSC.create();
        assertNotNull(config);
    }

    @Test
    public void test_writeNullToEmpty() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.writeNullToEmpty());

        JSONSerializationConfig result = config.writeNullToEmpty(true);
        assertSame(config, result);
        assertTrue(config.writeNullToEmpty());

        config.writeNullToEmpty(false);
        assertFalse(config.writeNullToEmpty());
    }

    @Test
    public void test_writeDatasetByRow() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.writeDatasetByRow());

        JSONSerializationConfig result = config.writeDatasetByRow(true);
        assertSame(config, result);
        assertTrue(config.writeDatasetByRow());

        config.writeDatasetByRow(false);
        assertFalse(config.writeDatasetByRow());
    }

    @Test
    public void test_writeRowColumnKeyType() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.writeRowColumnKeyType());

        JSONSerializationConfig result = config.writeRowColumnKeyType(true);
        assertSame(config, result);
        assertTrue(config.writeRowColumnKeyType());
    }

    @Test
    public void test_writeColumnType() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.writeColumnType());

        JSONSerializationConfig result = config.writeColumnType(true);
        assertSame(config, result);
        assertTrue(config.writeColumnType());
    }

    @Test
    public void test_quotePropName() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertTrue(config.quotePropName());

        JSONSerializationConfig result = config.quotePropName(false);
        assertSame(config, result);
        assertFalse(config.quotePropName());
    }

    @Test
    public void test_quoteMapKey() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertTrue(config.quoteMapKey());

        JSONSerializationConfig result = config.quoteMapKey(false);
        assertSame(config, result);
        assertFalse(config.quoteMapKey());
    }

    @Test
    public void test_bracketRootValue() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertTrue(config.bracketRootValue());

        JSONSerializationConfig result = config.bracketRootValue(false);
        assertSame(config, result);
        assertFalse(config.bracketRootValue());
    }

    @Test
    public void test_wrapRootValue() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.wrapRootValue());

        JSONSerializationConfig result = config.wrapRootValue(true);
        assertSame(config, result);
        assertTrue(config.wrapRootValue());
    }

    @Test
    public void test_JSC_of_quotePropName_quoteMapKey() {
        JSONSerializationConfig config = JSONSerializationConfig.JSC.of(true, false);
        assertNotNull(config);
        assertTrue(config.quotePropName());
        assertFalse(config.quoteMapKey());
    }

    @Test
    public void test_JSC_of_dateTimeFormat() {
        JSONSerializationConfig config = JSONSerializationConfig.JSC.of(DateTimeFormat.ISO_8601_TIMESTAMP);
        assertNotNull(config);
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config.getDateTimeFormat());
    }

    @Test
    public void test_JSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        JSONSerializationConfig config = JSONSerializationConfig.JSC.of(Exclusion.NULL, ignoredPropNames);
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

        JSONSerializationConfig config = JSONSerializationConfig.JSC.of(true, false, DateTimeFormat.ISO_8601_TIMESTAMP, Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.quotePropName());
        assertFalse(config.quoteMapKey());
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, config.getDateTimeFormat());
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    @Test
    public void test_equals() {
        JSONSerializationConfig config1 = new JSONSerializationConfig();
        JSONSerializationConfig config2 = new JSONSerializationConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));

        config2.quotePropName(false);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void test_hashCode() {
        JSONSerializationConfig config1 = new JSONSerializationConfig();
        JSONSerializationConfig config2 = new JSONSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("quotePropName"));
        assertTrue(str.contains("quoteMapKey"));
    }

    @Test
    public void test_prettyFormat() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        assertFalse(config.prettyFormat());

        config.prettyFormat(true);
        assertTrue(config.prettyFormat());
    }

    @Test
    public void test_setDateTimeFormat() {
        JSONSerializationConfig config = new JSONSerializationConfig();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_DATE_TIME);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, config.getDateTimeFormat());
    }
}
