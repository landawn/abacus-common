package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class KryoDeserializationConfig2025Test extends TestBase {

    @Test
    public void test_KDC_create() {
        KryoDeserializationConfig config = KryoDeserializationConfig.KDC.create();
        assertNotNull(config);
    }

    @Test
    public void test_setElementType() {
        KryoDeserializationConfig config = KryoDeserializationConfig.KDC.create();
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
    }

    @Test
    public void test_setMapKeyType() {
        KryoDeserializationConfig config = KryoDeserializationConfig.KDC.create();
        config.setMapKeyType(String.class);
        assertNotNull(config.getMapKeyType());
    }

    @Test
    public void test_setMapValueType() {
        KryoDeserializationConfig config = KryoDeserializationConfig.KDC.create();
        config.setMapValueType(Integer.class);
        assertNotNull(config.getMapValueType());
    }

    @Test
    public void test_ignoreUnmatchedProperty() {
        KryoDeserializationConfig config = KryoDeserializationConfig.KDC.create();
        config.ignoreUnmatchedProperty(true);
        assertNotNull(config);
    }
}
