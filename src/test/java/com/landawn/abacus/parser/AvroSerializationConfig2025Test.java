package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AvroSerializationConfig2025Test extends TestBase {

    private static final String TEST_SCHEMA_JSON = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}";

    @Test
    public void test_constructor() {
        AvroSerializationConfig config = new AvroSerializationConfig();
        assertNotNull(config);
        assertNull(config.getSchema());
    }

    @Test
    public void test_ASC_create() {
        AvroSerializationConfig config = AvroSerializationConfig.ASC.create();
        assertNotNull(config);
    }

    @Test
    public void test_getSchema() {
        AvroSerializationConfig config = new AvroSerializationConfig();
        assertNull(config.getSchema());
    }

    @Test
    public void test_setSchema() {
        AvroSerializationConfig config = new AvroSerializationConfig();
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);

        AvroSerializationConfig result = config.setSchema(schema);
        assertSame(config, result);
        assertEquals(schema, config.getSchema());
    }

    @Test
    public void test_ASC_of_schema() {
        Schema schema = new Schema.Parser().parse(TEST_SCHEMA_JSON);
        AvroSerializationConfig config = AvroSerializationConfig.ASC.of(schema);
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

        AvroSerializationConfig config = AvroSerializationConfig.ASC.of(schema, Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertEquals(schema, config.getSchema());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_equals() {
        AvroSerializationConfig config1 = new AvroSerializationConfig();
        AvroSerializationConfig config2 = new AvroSerializationConfig();

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
        AvroSerializationConfig config1 = new AvroSerializationConfig();
        AvroSerializationConfig config2 = new AvroSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        AvroSerializationConfig config = new AvroSerializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("schema"));
    }
}
