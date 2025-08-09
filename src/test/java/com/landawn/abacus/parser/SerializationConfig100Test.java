// SerializationConfig100Test.java
package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class SerializationConfig100Test extends TestBase {
    
    private TestSerializationConfig config;
    
    // Test implementation of abstract class
    private static class TestSerializationConfig extends SerializationConfig<TestSerializationConfig> {
    }
    
    @BeforeEach
    public void setUp() {
        config = new TestSerializationConfig();
    }
    
    @Test
    public void testGetExclusion() {
        assertNull(config.getExclusion());
    }
    
    @Test
    public void testSetExclusion() {
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
        
        config.setExclusion(Exclusion.NONE);
        assertEquals(Exclusion.NONE, config.getExclusion());
        
        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
    }
    
    @Test
    public void testSkipTransientField() {
        assertTrue(config.skipTransientField());
    }
    
    @Test
    public void testSkipTransientFieldSetter() {
        config.skipTransientField(false);
        assertFalse(config.skipTransientField());
        
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());
    }
    
    @Test
    public void testHashCode() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        TestSerializationConfig config2 = new TestSerializationConfig();
        
        assertEquals(config1.hashCode(), config2.hashCode());
        
        config1.setExclusion(Exclusion.NULL);
        assertNotEquals(config1.hashCode(), config2.hashCode());
        
        config2.setExclusion(Exclusion.NULL);
        assertEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    public void testEquals() {
        TestSerializationConfig config1 = new TestSerializationConfig();
        TestSerializationConfig config2 = new TestSerializationConfig();
        
        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertFalse(config1.equals(null));
        assertFalse(config1.equals("string"));
        
        config1.setExclusion(Exclusion.NULL);
        assertFalse(config1.equals(config2));
        
        config2.setExclusion(Exclusion.NULL);
        assertTrue(config1.equals(config2));
        
        config1.skipTransientField(false);
        assertFalse(config1.equals(config2));
        
        config2.skipTransientField(false);
        assertTrue(config1.equals(config2));
        
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("testProp");
        config1.setIgnoredPropNames(ignoredProps);
        assertFalse(config1.equals(config2));
    }
    
    @Test
    public void testToString() {
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("ignoredPropNames"));
        assertTrue(str.contains("exclusion"));
        assertTrue(str.contains("skipTransientField"));
    }
}

// ParserUtilTest.java
