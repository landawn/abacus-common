package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.KryoSerializationConfig.KSC;


public class KryoSerializationConfig100Test extends TestBase {
    
    private KryoSerializationConfig config;
    
    @BeforeEach
    public void setUp() {
        config = new KryoSerializationConfig();
    }
    
    @Test
    public void testWriteClass() {
        assertFalse(config.writeClass());
        
        config.writeClass(true);
        assertTrue(config.writeClass());
        
        config.writeClass(false);
        assertFalse(config.writeClass());
    }
    
    @Test
    public void testHashCode() {
        KryoSerializationConfig config1 = new KryoSerializationConfig();
        KryoSerializationConfig config2 = new KryoSerializationConfig();
        
        assertEquals(config1.hashCode(), config2.hashCode());
        
        config1.writeClass(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());
        
        config2.writeClass(true);
        assertEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    public void testEquals() {
        KryoSerializationConfig config1 = new KryoSerializationConfig();
        KryoSerializationConfig config2 = new KryoSerializationConfig();
        
        assertEquals(config1, config2);
        assertEquals(config1, config1);
        assertNotEquals(config1, null);
        assertNotEquals(config1, "string");
        
        config1.writeClass(true);
        assertNotEquals(config1, config2);
        
        config2.writeClass(true);
        assertEquals(config1, config2);
        
        config1.setExclusion(Exclusion.NULL);
        assertNotEquals(config1, config2);
        
        config2.setExclusion(Exclusion.NULL);
        assertEquals(config1, config2);
    }
    
    @Test
    public void testToString() {
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("writeClass=false"));
        
        config.writeClass(true);
        str = config.toString();
        assertTrue(str.contains("writeClass=true"));
    }
    
    @Test
    public void testKSCCreate() {
        KryoSerializationConfig newConfig = KSC.create();
        
        assertNotNull(newConfig);
        assertNotSame(config, newConfig);
        assertFalse(newConfig.writeClass());
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOf() {
        KryoSerializationConfig config1 = KSC.of(true);
        assertTrue(config1.writeClass());
        
        KryoSerializationConfig config2 = KSC.of(false);
        assertFalse(config2.writeClass());
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOfWithExclusion() {
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredProps.put(String.class, props);
        
        KryoSerializationConfig config = KSC.of(Exclusion.NULL, ignoredProps);
        
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertEquals(props, config.getIgnoredPropNames(String.class));
    }
    
    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOfWithAllParams() {
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredProps.put(String.class, props);
        
        KryoSerializationConfig config = KSC.of(true, Exclusion.DEFAULT, ignoredProps);
        
        assertTrue(config.writeClass());
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
        assertEquals(props, config.getIgnoredPropNames(String.class));
    }
    
    @Test
    public void testMethodChaining() {
        KryoSerializationConfig result = config
            .writeClass(true)
            .setExclusion(Exclusion.NULL)
            .skipTransientField(true);
        
        assertSame(config, result);
        assertTrue(config.writeClass());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertTrue(config.skipTransientField());
    }
    
    @Test
    public void testInheritedMethods() {
        // Test methods inherited from SerializationConfig
        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
        
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());
        
        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("password");
        config.setIgnoredPropNames(String.class, ignoredProps);
        assertEquals(ignoredProps, config.getIgnoredPropNames(String.class));
    }
    
    @Test
    public void testDefaultValues() {
        KryoSerializationConfig newConfig = new KryoSerializationConfig();
        
        assertFalse(newConfig.writeClass());
        assertNull(newConfig.getExclusion());
        assertTrue(newConfig.skipTransientField());
        assertNull(newConfig.getIgnoredPropNames(String.class));
    }
    
    @Test
    public void testEqualityWithDifferentConfigurations() {
        KryoSerializationConfig config1 = new KryoSerializationConfig();
        KryoSerializationConfig config2 = new KryoSerializationConfig();
        
        // Test different combinations
        config1.writeClass(true).skipTransientField(true);
        config2.writeClass(true).skipTransientField(false);
        assertNotEquals(config1, config2);
        
        config2.skipTransientField(true);
        assertEquals(config1, config2);
        
        // Test with ignored properties
        Map<Class<?>, Set<String>> ignoredProps1 = new HashMap<>();
        ignoredProps1.put(String.class, new HashSet<>(Arrays.asList("prop1")));
        config1.setIgnoredPropNames(ignoredProps1);
        
        assertNotEquals(config1, config2);
        
        config2.setIgnoredPropNames(ignoredProps1);
        assertEquals(config1, config2);
    }
}
