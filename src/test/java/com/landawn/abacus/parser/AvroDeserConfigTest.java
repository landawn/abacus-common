package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

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

    @Test
    public void test_ignoreUnmatchedProperty() {
        AvroDeserConfig config = AvroDeserConfig.create();
        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());
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

    // inherited method tests
    @Test
    public void test_setElementType() {
        AvroDeserConfig config = AvroDeserConfig.create();
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
    }

    // Regression: equals/hashCode/toString previously ignored inherited fields
    // (elementType, mapKeyType, mapValueType, ...) which are actually used by AvroParser
    // during deserialization. Two configs that differ only by elementType must NOT be equal.
    @Test
    public void test_equals_includesElementType() {
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();
        assertEquals(config1, config2);

        config1.setElementType(String.class);
        assertFalse(config1.equals(config2));
        assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setElementType(String.class);
        assertTrue(config1.equals(config2));
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_equals_includesMapKeyAndValueType() {
        AvroDeserConfig config1 = AvroDeserConfig.create();
        AvroDeserConfig config2 = AvroDeserConfig.create();

        config1.setMapKeyType(String.class);
        assertFalse(config1.equals(config2));

        config2.setMapKeyType(String.class);
        assertTrue(config1.equals(config2));

        config1.setMapValueType(Integer.class);
        assertFalse(config1.equals(config2));

        config2.setMapValueType(Integer.class);
        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_toString_includesInheritedFields() {
        AvroDeserConfig config = AvroDeserConfig.create();
        config.setElementType(String.class);
        String str = config.toString();
        assertTrue(str.contains("elementType="));
        assertTrue(str.contains("mapKeyType="));
        assertTrue(str.contains("mapValueType="));
        assertTrue(str.contains("schema="));
    }

    // reviewFixes20260906 (P6-13): equals/hashCode delegate to DeserializationConfig.
    @Test
    public void reviewFixes20260906_equalsAndHashCodeDelegateToParent() {
        final Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        final Schema sameSchema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        final Schema otherSchema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"Other\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");

        final AvroDeserConfig a = AvroDeserConfig.create()
                .setSchema(schema)
                .setIgnoreUnmatchedProperty(false)
                .setElementType(String.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class);

        final AvroDeserConfig copy = a.copy();
        assertNotSame(a, copy);
        assertEquals(a, copy);
        assertEquals(copy, a);
        assertEquals(a.hashCode(), copy.hashCode());

        final AvroDeserConfig b = AvroDeserConfig.create()
                .setSchema(sameSchema)
                .setIgnoreUnmatchedProperty(false)
                .setElementType(String.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setSchema(otherSchema);
        assertNotEquals(a, b);
        b.setSchema(null);
        assertNotEquals(a, b);
        assertNotEquals(b, a);
        b.setSchema(sameSchema);
        assertEquals(a, b);

        b.setIgnoreUnmatchedProperty(true);
        assertNotEquals(a, b);
        b.setIgnoreUnmatchedProperty(false);
        assertEquals(a, b);

        b.setElementType(Integer.class);
        assertNotEquals(a, b);
        b.setElementType(String.class);
        assertEquals(a, b);

        b.setMapKeyType(Integer.class);
        assertNotEquals(a, b);
        b.setMapKeyType(String.class);
        assertEquals(a, b);

        b.setMapValueType(String.class);
        assertNotEquals(a, b);
        b.setMapValueType(Integer.class);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertFalse(a.equals(null));
        assertFalse(a.equals("not a config"));
        assertFalse(a.equals(KryoDeserConfig.create()));

        // The parent requires the exact same class: an anonymous subclass is unequal in BOTH directions.
        final AvroDeserConfig anonymous = new AvroDeserConfig() {
        };
        anonymous.setSchema(schema).setIgnoreUnmatchedProperty(false).setElementType(String.class).setMapKeyType(String.class).setMapValueType(Integer.class);
        assertEquals(anonymous.equals(a), a.equals(anonymous));
        assertFalse(a.equals(anonymous));
    }

}
