package com.landawn.abacus.parser;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.parser.AvroDeserializationConfig.ADC;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AvroDeserializationConfig100Test extends TestBase {

    private AvroDeserializationConfig config;

    @BeforeEach
    public void setUp() {
        config = ADC.create();
    }

    @Test
    public void testGetSchema() {
        Assertions.assertNull(config.getSchema());
    }

    @Test
    public void testSetSchema() {
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);

        AvroDeserializationConfig result = config.setSchema(schema);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(schema, config.getSchema());

        config.setSchema(null);
        Assertions.assertNull(config.getSchema());
    }

    @Test
    public void testHashCode() {
        AvroDeserializationConfig config1 = ADC.create();
        AvroDeserializationConfig config2 = ADC.create();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        config1.setSchema(schema);

        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setSchema(schema);
        Assertions.assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        AvroDeserializationConfig config1 = ADC.create();
        AvroDeserializationConfig config2 = ADC.create();

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

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("schema="));
        Assertions.assertTrue(str.contains("ignoredPropNames="));
        Assertions.assertTrue(str.contains("ignoreUnmatchedProperty="));
    }

    @Test
    public void testADCCreate() {
        AvroDeserializationConfig config = ADC.create();
        Assertions.assertNotNull(config);
        Assertions.assertNull(config.getSchema());
        Assertions.assertTrue(config.ignoreUnmatchedProperty());
    }
}
