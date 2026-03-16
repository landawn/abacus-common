package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AvroDeserConfigTest extends TestBase {

    private AvroDeserConfig config;

    @BeforeEach
    public void setUp() {
        config = AvroDeserConfig.create();
    }

    private static final String TEST_SCHEMA_JSON = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";

    // getSchema
    @Test
    public void test_getSchema() {
        AvroDeserConfig config = AvroDeserConfig.create();
        assertNull(config.getSchema());
    }

    @Test
    public void testGetSchema() {
        Assertions.assertNull(config.getSchema());
    }

    // setSchema
    @Test
    public void test_setSchema() {
        AvroDeserConfig config = AvroDeserConfig.create();
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);

        AvroDeserConfig result = config.setSchema(schema);
        assertSame(config, result);
        assertEquals(schema, config.getSchema());
    }

    @Test
    public void testSetSchema() {
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        AvroDeserConfig result = config.setSchema(schema);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(schema, config.getSchema());

        config.setSchema(null);
        Assertions.assertNull(config.getSchema());
    }

    // hashCode
    @Test
    public void test_hashCode() {
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();

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
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();

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
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();

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
        AvroDeserConfig config = AvroDeserConfig.create();
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
        Assertions.assertTrue(str.contains("ignoreUnmatchedProperty="));
    }

    // create
    @Test
    public void test_ADC_create() {
        AvroDeserConfig config = AvroDeserConfig.create();
        assertNotNull(config);
        assertNull(config.getSchema());
    }

    @Test
    public void testADCCreate() {
        AvroDeserConfig config = AvroDeserConfig.create();
        Assertions.assertNotNull(config);
        Assertions.assertNull(config.getSchema());
        Assertions.assertTrue(config.isIgnoreUnmatchedProperty());
    }

    // copy
    @Test
    public void testCopy() {
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        config.setSchema(schema);
        config.setIgnoreUnmatchedProperty(false);

        AvroDeserConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertEquals(schema, copy.getSchema());
        assertFalse(copy.isIgnoreUnmatchedProperty());
    }

    // inherited method tests
    @Test
    public void test_setElementType() {
        AvroDeserConfig config = AvroDeserConfig.create();
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
    }

    @Test
    public void test_ignoreUnmatchedProperty() {
        AvroDeserConfig config = AvroDeserConfig.create();
        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

}
