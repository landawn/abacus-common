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

public class KryoSerConfigTest extends TestBase {

    private KryoSerConfig config;

    @BeforeEach
    void setUp() {
        config = new KryoSerConfig();
    }

    @Test
    public void test_skipTransientField() {
        KryoSerConfig config = new KryoSerConfig();
        config.setSkipTransientField(true);
        assertTrue(config.isSkipTransientField());

        config.setSkipTransientField(false);
        assertFalse(config.isSkipTransientField());
    }

    @Test
    public void testEqualityWithDifferentConfigurations() {
        KryoSerConfig config1 = new KryoSerConfig();
        KryoSerConfig config2 = new KryoSerConfig();

        config1.setWriteClass(true).setSkipTransientField(true);
        config2.setWriteClass(true).setSkipTransientField(false);
        assertNotEquals(config1, config2);

        config2.setSkipTransientField(true);
        assertEquals(config1, config2);

        Map<Class<?>, Set<String>> ignoredProps1 = new HashMap<>();
        ignoredProps1.put(String.class, new HashSet<>(Arrays.asList("prop1")));
        config1.setIgnoredPropNames(ignoredProps1);

        assertNotEquals(config1, config2);

        config2.setIgnoredPropNames(ignoredProps1);
        assertEquals(config1, config2);
    }

    // constructor
    @Test
    public void test_constructor() {
        KryoSerConfig config = new KryoSerConfig();
        assertNotNull(config);
        assertFalse(config.isWriteClass());
    }

    @Test
    public void test_setExclusion() {
        KryoSerConfig config = new KryoSerConfig();
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());

        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
    }

    @Test
    public void test_setIgnoredPropNames() {
        KryoSerConfig config = new KryoSerConfig();
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");
        ignoredPropNames.put(String.class, props);

        KryoSerConfig result = config.setIgnoredPropNames(ignoredPropNames);
        assertSame(config, result);
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void testDefaultValues() {
        KryoSerConfig newConfig = new KryoSerConfig();

        assertFalse(newConfig.isWriteClass());
        assertNull(newConfig.getExclusion());
        assertTrue(newConfig.isSkipTransientField());
        assertNull(newConfig.getIgnoredPropNames(String.class));
    }

    @Test
    public void testWriteClass() {
        assertFalse(config.isWriteClass());

        config.setWriteClass(true);
        assertTrue(config.isWriteClass());

        config.setWriteClass(false);
        assertFalse(config.isWriteClass());
    }

    @Test
    public void testIsWriteClass() {
        assertFalse(config.isWriteClass());
        config.setWriteClass(true);
        assertTrue(config.isWriteClass());
    }

    // isWriteClass
    @Test
    public void test_writeClass() {
        KryoSerConfig config = new KryoSerConfig();
        assertFalse(config.isWriteClass());

        KryoSerConfig result = config.setWriteClass(true);
        assertSame(config, result);
        assertTrue(config.isWriteClass());

        config.setWriteClass(false);
        assertFalse(config.isWriteClass());
    }

    // setWriteClass
    @Test
    public void testSetWriteClass() {
        KryoSerConfig result = config.setWriteClass(true);
        assertSame(config, result);
        assertTrue(config.isWriteClass());
    }

    // copy
    @Test
    public void testCopy() {
        config.setWriteClass(true);
        config.setExclusion(Exclusion.NULL);
        config.setSkipTransientField(false);

        KryoSerConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertTrue(copy.isWriteClass());
        assertEquals(Exclusion.NULL, copy.getExclusion());
        assertFalse(copy.isSkipTransientField());
    }

    // hashCode
    @Test
    public void test_hashCode() {
        KryoSerConfig config1 = new KryoSerConfig();
        KryoSerConfig config2 = new KryoSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.setWriteClass(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        KryoSerConfig config1 = new KryoSerConfig();
        KryoSerConfig config2 = new KryoSerConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config1.setWriteClass(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setWriteClass(true);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // equals
    @Test
    public void test_equals() {
        KryoSerConfig config1 = new KryoSerConfig();
        KryoSerConfig config2 = new KryoSerConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.setWriteClass(true);
        assertFalse(config1.equals(config2));

        config2.setWriteClass(false);
        assertTrue(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void testEquals() {
        KryoSerConfig config1 = new KryoSerConfig();
        KryoSerConfig config2 = new KryoSerConfig();

        assertEquals(config1, config2);
        assertEquals(config1, config1);
        assertNotEquals(config1, null);
        assertNotEquals(config1, "string");

        config1.setWriteClass(true);
        assertNotEquals(config1, config2);

        config2.setWriteClass(true);
        assertEquals(config1, config2);

        config1.setExclusion(Exclusion.NULL);
        assertNotEquals(config1, config2);

        config2.setExclusion(Exclusion.NULL);
        assertEquals(config1, config2);
    }

    // toString
    @Test
    public void test_toString() {
        KryoSerConfig config = new KryoSerConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("writeClass"));
        assertTrue(str.contains("exclusion"));
        assertTrue(str.contains("skipTransientField"));
    }

    @Test
    public void testToString() {
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("writeClass=false"));

        config.setWriteClass(true);
        str = config.toString();
        assertTrue(str.contains("writeClass=true"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOf() {
        KryoSerConfig config1 = KryoSerConfig.create().setWriteClass(true);
        assertTrue(config1.isWriteClass());

        KryoSerConfig config2 = KryoSerConfig.create().setWriteClass(false);
        assertFalse(config2.isWriteClass());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOfWithAllParams() {
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredProps.put(String.class, props);

        KryoSerConfig config = KryoSerConfig.create().setWriteClass(true).setExclusion(Exclusion.DEFAULT).setIgnoredPropNames(ignoredProps);

        assertTrue(config.isWriteClass());
        assertEquals(Exclusion.DEFAULT, config.getExclusion());
        assertEquals(props, config.getIgnoredPropNames(String.class));
    }

    // create
    @Test
    public void test_KSC_create() {
        KryoSerConfig config = KryoSerConfig.create();
        assertNotNull(config);
    }

    @Test
    public void testKSCCreate() {
        KryoSerConfig newConfig = KryoSerConfig.create();

        assertNotNull(newConfig);
        assertNotSame(config, newConfig);
        assertFalse(newConfig.isWriteClass());
    }

    // combined config tests
    @Test
    public void test_KSC_of_writeClass() {
        KryoSerConfig config = KryoSerConfig.create().setWriteClass(true);
        assertNotNull(config);
        assertTrue(config.isWriteClass());

        config = KryoSerConfig.create().setWriteClass(false);
        assertFalse(config.isWriteClass());
    }

    @Test
    public void test_KSC_of_exclusion_ignoredPropNames() {
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredPropNames.put(String.class, props);

        KryoSerConfig config = KryoSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredPropNames);
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

        KryoSerConfig config = KryoSerConfig.create().setWriteClass(true).setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredPropNames);
        assertNotNull(config);
        assertTrue(config.isWriteClass());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testKSCOfWithExclusion() {
        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        ignoredProps.put(String.class, props);

        KryoSerConfig config = KryoSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames(ignoredProps);

        assertEquals(Exclusion.NULL, config.getExclusion());
        assertEquals(props, config.getIgnoredPropNames(String.class));
    }

    @Test
    public void testMethodChaining() {
        KryoSerConfig result = config.setWriteClass(true).setExclusion(Exclusion.NULL).setSkipTransientField(true);

        assertSame(config, result);
        assertTrue(config.isWriteClass());
        assertEquals(Exclusion.NULL, config.getExclusion());
        assertTrue(config.isSkipTransientField());
    }

    // getExclusion
    @Test
    public void testGetExclusion() {
        assertNull(config.getExclusion());
        config.setExclusion(Exclusion.NULL);
        assertEquals(Exclusion.NULL, config.getExclusion());
    }

    // isSkipTransientField
    @Test
    public void testIsSkipTransientField() {
        assertTrue(config.isSkipTransientField());
        config.setSkipTransientField(false);
        assertFalse(config.isSkipTransientField());
    }

    @Test
    public void testInheritedMethods() {
        config.setExclusion(Exclusion.DEFAULT);
        assertEquals(Exclusion.DEFAULT, config.getExclusion());

        config.setSkipTransientField(true);
        assertTrue(config.isSkipTransientField());

        Set<String> ignoredProps = new HashSet<>();
        ignoredProps.add("password");
        config.setIgnoredPropNames(String.class, ignoredProps);
        assertEquals(ignoredProps, config.getIgnoredPropNames(String.class));
    }

}
