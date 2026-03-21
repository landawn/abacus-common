package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AvroSerConfigTest extends TestBase {

    private AvroSerConfig config;

    @BeforeEach
    public void setUp() {
        config = new AvroSerConfig();
    }

    private static final String TEST_SCHEMA_JSON = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";

    // constructor
    @Test
    public void test_constructor() {
        AvroSerConfig config = new AvroSerConfig();
        assertNotNull(config);
        assertNull(config.getSchema());
    }

    // getSchema
    @Test
    public void test_getSchema() {
        AvroSerConfig config = new AvroSerConfig();
        assertNull(config.getSchema());
    }

    @Test
    public void testGetSchema() {
        Assertions.assertNull(config.getSchema());
    }

    // setSchema
    @Test
    public void test_setSchema() {
        AvroSerConfig config = new AvroSerConfig();
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);

        AvroSerConfig result = config.setSchema(schema);
        assertSame(config, result);
        assertEquals(schema, config.getSchema());
    }

    @Test
    public void testSetSchema() {
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        AvroSerConfig result = config.setSchema(schema);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(schema, config.getSchema());

        config.setSchema(null);
        Assertions.assertNull(config.getSchema());
    }

    // copy
    @Test
    public void testCopy() {
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        config.setSchema(schema);
        config.setExclusion(Exclusion.NULL);

        AvroSerConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertEquals(schema, copy.getSchema());
        assertEquals(Exclusion.NULL, copy.getExclusion());
    }

    // hashCode
    @Test
    public void test_hashCode() {
        AvroSerConfig config1 = new AvroSerConfig();
        AvroSerConfig config2 = new AvroSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        AvroSerConfig config1 = new AvroSerConfig();
        AvroSerConfig config2 = new AvroSerConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        config1.setSchema(schema);

        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setSchema(schema);
        Assertions.assertEquals(config1.hashCode(), config2.hashCode());
    }

    // equals
    @Test
    public void test_equals() {
        AvroSerConfig config1 = new AvroSerConfig();
        AvroSerConfig config2 = new AvroSerConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));

        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        config2.setSchema(schema);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void testEquals() {
        AvroSerConfig config1 = new AvroSerConfig();
        AvroSerConfig config2 = new AvroSerConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        config1.setSchema(schema);

        Assertions.assertNotEquals(config1, config2);

        config2.setSchema(schema);
        Assertions.assertEquals(config1, config2);
    }

    // toString
    @Test
    public void test_toString() {
        AvroSerConfig config = new AvroSerConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("schema"));
    }

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("schema="));
        Assertions.assertTrue(str.contains("ignoredPropNames="));
        Assertions.assertTrue(str.contains("exclusion="));
        Assertions.assertTrue(str.contains("skipTransientField="));
    }

    // create
    @Test
    public void test_ASC_create() {
        AvroSerConfig config = AvroSerConfig.create();
        assertNotNull(config);
    }

    @Test
    public void testASCCreate() {
        AvroSerConfig config = AvroSerConfig.create();
        Assertions.assertNotNull(config);
        Assertions.assertNull(config.getSchema());
    }

    // combined config tests
    @Test
    public void test_ASC_of_schema() {
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        AvroSerConfig config = AvroSerConfig.create().setSchema(schema);
        assertNotNull(config);
        assertEquals(schema, config.getSchema());
    }

    @Test
    public void test_ASC_of_schema_exclusion_ignoredPropNames() {
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        AvroSerConfig config = AvroSerConfig.create().setSchema(schema).setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredPropNames);
        assertNotNull(config);
        assertEquals(schema, config.getSchema());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void testASCOf() {
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        AvroSerConfig config1 = AvroSerConfig.create().setSchema(schema);
        Assertions.assertEquals(schema, config1.getSchema());

        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        AvroSerConfig config2 = AvroSerConfig.create().setSchema(schema).setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredProps);
        Assertions.assertEquals(schema, config2.getSchema());
        Assertions.assertEquals(Exclusion.NULL, config2.getExclusion());
    }

}
