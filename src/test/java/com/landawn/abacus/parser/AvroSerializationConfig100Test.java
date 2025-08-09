package com.landawn.abacus.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.AvroSerializationConfig.ASC;

import com.landawn.abacus.TestBase;


public class AvroSerializationConfig100Test extends TestBase {
    
    private AvroSerializationConfig config;
    
    @BeforeEach
    public void setUp() {
        config = new AvroSerializationConfig();
    }
    
    @Test
    public void testConstructor() {
        AvroSerializationConfig config = new AvroSerializationConfig();
        Assertions.assertNotNull(config);
        Assertions.assertNull(config.getSchema());
    }
    
    @Test
    public void testGetSchema() {
        Assertions.assertNull(config.getSchema());
    }
    
    @Test
    public void testSetSchema() {
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        
        AvroSerializationConfig result = config.setSchema(schema);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(schema, config.getSchema());
        
        // Test setting null
        config.setSchema(null);
        Assertions.assertNull(config.getSchema());
    }
    
    @Test
    public void testHashCode() {
        AvroSerializationConfig config1 = new AvroSerializationConfig();
        AvroSerializationConfig config2 = new AvroSerializationConfig();
        
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
        AvroSerializationConfig config1 = new AvroSerializationConfig();
        AvroSerializationConfig config2 = new AvroSerializationConfig();
        
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
        Assertions.assertTrue(str.contains("exclusion="));
        Assertions.assertTrue(str.contains("skipTransientField="));
    }
    
    @Test
    public void testASCCreate() {
        AvroSerializationConfig config = ASC.create();
        Assertions.assertNotNull(config);
        Assertions.assertNull(config.getSchema());
    }
    
    @Test
    public void testASCOf() {
        // Test deprecated methods
        String schemaJson = "{\"type\":\"record\",\"name\":\"Test\",\"fields\":[{\"name\":\"field1\",\"type\":\"string\"}]}";
        Schema schema = new Schema.Parser().parse(schemaJson);
        
        AvroSerializationConfig config1 = ASC.of(schema);
        Assertions.assertEquals(schema, config1.getSchema());
        
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        AvroSerializationConfig config2 = ASC.of(schema, Exclusion.NULL, ignoredProps);
        Assertions.assertEquals(schema, config2.getSchema());
        Assertions.assertEquals(Exclusion.NULL, config2.getExclusion());
    }
}
