package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AvroDeserializationConfig2025Test extends TestBase {

    private static final String TEST_SCHEMA_JSON = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";

    @Test
    public void test_ADC_create() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        assertNotNull(config);
        assertNull(config.getSchema());
    }

    @Test
    public void test_getSchema() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        assertNull(config.getSchema());
    }

    @Test
    public void test_setSchema() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);

        AvroDeserializationConfig result = config.setSchema(schema);
        assertSame(config, result);
        assertEquals(schema, config.getSchema());
    }

    @Test
    public void test_equals() {
        AvroDeserializationConfig config1 = AvroDeserializationConfig.ADC.create();
        AvroDeserializationConfig config2 = AvroDeserializationConfig.ADC.create();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));

        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        config2.setSchema(schema);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void test_hashCode() {
        AvroDeserializationConfig config1 = AvroDeserializationConfig.ADC.create();
        AvroDeserializationConfig config2 = AvroDeserializationConfig.ADC.create();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("schema"));
    }

    @Test
    public void test_setElementType() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
    }

    @Test
    public void test_ignoreUnmatchedProperty() {
        AvroDeserializationConfig config = AvroDeserializationConfig.ADC.create();
        config.ignoreUnmatchedProperty(true);
        assertTrue(config.ignoreUnmatchedProperty());
    }
}
