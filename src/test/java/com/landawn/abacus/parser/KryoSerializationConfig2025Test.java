package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

@Tag("2025")
public class KryoSerializationConfig2025Test extends TestBase {

    @Test
    public void test_constructor() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        assertNotNull(config);
        assertFalse(config.writeClass());
    }

    @Test
    public void test_KSC_create() {
        KryoSerializationConfig config = KryoSerializationConfig.KSC.create();
        assertNotNull(config);
    }

    @Test
    public void test_writeClass() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        assertFalse(config.writeClass());

        KryoSerializationConfig result = config.writeClass(true);
        assertSame(config, result);
        assertTrue(config.writeClass());

        config.writeClass(false);
        assertFalse(config.writeClass());
    }

    @Test
    public void test_KSC_of_writeClass() {
        KryoSerializationConfig config = KryoSerializationConfig.KSC.of(true);
        assertNotNull(config);
        assertTrue(config.writeClass());

        config = KryoSerializationConfig.KSC.of(false);
        assertFalse(config.writeClass());
    }

    @Test
    public void test_KSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        KryoSerializationConfig config = KryoSerializationConfig.KSC.of(Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_KSC_of_writeClass_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        KryoSerializationConfig config = KryoSerializationConfig.KSC.of(true, Exclusion.NULL, ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.writeClass());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void test_equals() {
        KryoSerializationConfig config1 = new KryoSerializationConfig();
        KryoSerializationConfig config2 = new KryoSerializationConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.writeClass(true);
        assertFalse(config1.equals(config2));

        config2.writeClass(false);
        assertTrue(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void test_hashCode() {
        KryoSerializationConfig config1 = new KryoSerializationConfig();
        KryoSerializationConfig config2 = new KryoSerializationConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.writeClass(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_toString() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("writeClass"));
        assertTrue(str.contains("exclusion"));
        assertTrue(str.contains("skipTransientField"));
    }

    @Test
    public void test_setExclusion() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());

        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
    }

    @Test
    public void test_skipTransientField() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        config.skipTransientField(true);
        assertTrue(config.skipTransientField());

        config.skipTransientField(false);
        assertFalse(config.skipTransientField());
    }

    @Test
    public void test_setIgnoredPropNames() {
        KryoSerializationConfig config = new KryoSerializationConfig();
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");
        ignoredPropNames.put(String.class, props);

        KryoSerializationConfig result = config.setIgnoredPropNames(ignoredPropNames);
        assertSame(config, result);
        assertNotNull(config.getIgnoredPropNames());
    }
}
