package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.KryoDeserializationConfig.KDC;

@Tag("new-test")
public class KryoDeserializationConfig100Test extends TestBase {

    private KryoDeserializationConfig config;

    @BeforeEach
    public void setUp() {
        config = new KryoDeserializationConfig();
    }

    @Test
    public void testKDCCreate() {
        KryoDeserializationConfig newConfig = KDC.create();

        assertNotNull(newConfig);
        assertNotSame(config, newConfig);
    }

    @Test
    public void testInheritedMethods() {
        config.ignoreUnmatchedProperty(true);
        assertTrue(config.ignoreUnmatchedProperty());

        config.ignoreUnmatchedProperty(true);
        assertTrue(config.ignoreUnmatchedProperty());

    }

    @Test
    public void testMethodChaining() {
        KryoDeserializationConfig result = config.ignoreUnmatchedProperty(true);

        assertSame(config, result);
        assertTrue(config.ignoreUnmatchedProperty());
    }

    @Test
    public void testDefaultValues() {
        KryoDeserializationConfig newConfig = new KryoDeserializationConfig();

        assertTrue(newConfig.ignoreUnmatchedProperty());
    }

    @Test
    public void testCreateMultipleInstances() {
        KryoDeserializationConfig config1 = KDC.create();
        KryoDeserializationConfig config2 = KDC.create();

        assertNotSame(config1, config2);

        config1.ignoreUnmatchedProperty(true);
        assertTrue(config2.ignoreUnmatchedProperty());
    }
}
