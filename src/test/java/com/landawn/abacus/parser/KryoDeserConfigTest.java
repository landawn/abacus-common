package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class KryoDeserConfigTest extends TestBase {

    private KryoDeserConfig config;

    @BeforeEach
    void setUp() {
        config = KryoDeserConfig.create();
    }

    @Test
    public void testDefaultValues() {
        KryoDeserConfig newConfig = new KryoDeserConfig();

        assertTrue(newConfig.isIgnoreUnmatchedProperty());
    }

    // hashCode
    @Test
    public void testHashCode() {
        KryoDeserConfig config1 = KryoDeserConfig.create();
        KryoDeserConfig config2 = KryoDeserConfig.create();

        assertEquals(config1.hashCode(), config2.hashCode());

        config1.setIgnoreUnmatchedProperty(false);
        assertNotEquals(config1.hashCode(), config2.hashCode());

        config2.setIgnoreUnmatchedProperty(false);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    // equals
    @Test
    public void testEquals() {
        KryoDeserConfig config1 = KryoDeserConfig.create();
        KryoDeserConfig config2 = KryoDeserConfig.create();

        assertEquals(config1, config1);
        assertEquals(config1, config2);
        assertNotEquals(config1, null);
        assertNotEquals(config1, "string");

        config1.setIgnoreUnmatchedProperty(false);
        assertNotEquals(config1, config2);

        config2.setIgnoreUnmatchedProperty(false);
        assertEquals(config1, config2);
    }

    // toString
    @Test
    public void testToString() {
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("ignoredPropNames="));
        assertTrue(str.contains("ignoreUnmatchedProperty="));
    }

    // create
    @Test
    public void test_KDC_create() {
        KryoDeserConfig config = KryoDeserConfig.create();
        assertNotNull(config);
    }

    @Test
    public void testKDCCreate() {
        KryoDeserConfig newConfig = KryoDeserConfig.create();

        assertNotNull(newConfig);
        assertNotSame(config, newConfig);
    }

    // inherited method tests
    @Test
    public void test_setElementType() {
        KryoDeserConfig config = KryoDeserConfig.create();
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
    }

    @Test
    public void test_setMapKeyType() {
        KryoDeserConfig config = KryoDeserConfig.create();
        config.setMapKeyType(String.class);
        assertNotNull(config.getMapKeyType());
    }

    @Test
    public void test_setMapValueType() {
        KryoDeserConfig config = KryoDeserConfig.create();
        config.setMapValueType(Integer.class);
        assertNotNull(config.getMapValueType());
    }

    @Test
    public void test_ignoreUnmatchedProperty() {
        KryoDeserConfig config = KryoDeserConfig.create();
        config.setIgnoreUnmatchedProperty(true);
        assertNotNull(config);
    }

    @Test
    public void testCreateMultipleInstances() {
        KryoDeserConfig config1 = KryoDeserConfig.create();
        KryoDeserConfig config2 = KryoDeserConfig.create();

        assertNotSame(config1, config2);

        config1.setIgnoreUnmatchedProperty(true);
        assertTrue(config2.isIgnoreUnmatchedProperty());
    }

    // copy
    @Test
    public void testCopy() {
        config.setIgnoreUnmatchedProperty(false);
        config.setElementType(String.class);

        KryoDeserConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertEquals(false, copy.isIgnoreUnmatchedProperty());
        assertNotNull(copy.getElementType());
    }

    @Test
    public void testInheritedMethods() {
        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());

    }

    @Test
    public void testMethodChaining() {
        KryoDeserConfig result = config.setIgnoreUnmatchedProperty(true);

        assertSame(config, result);
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

}
