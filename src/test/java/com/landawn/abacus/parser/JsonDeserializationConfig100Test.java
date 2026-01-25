package com.landawn.abacus.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;

@Tag("new-test")
public class JsonDeserializationConfig100Test extends TestBase {

    private JsonDeserializationConfig config;

    @BeforeEach
    public void setUp() {
        config = new JsonDeserializationConfig();
    }

    @Test
    public void testIgnoreNullOrEmpty() {
        Assertions.assertFalse(config.ignoreNullOrEmpty());

        JsonDeserializationConfig result = config.ignoreNullOrEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.ignoreNullOrEmpty());

        config.ignoreNullOrEmpty(false);
        Assertions.assertFalse(config.ignoreNullOrEmpty());
    }

    @Test
    public void testReadNullToEmpty() {
        Assertions.assertFalse(config.readNullToEmpty());

        JsonDeserializationConfig result = config.readNullToEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.readNullToEmpty());

        config.readNullToEmpty(false);
        Assertions.assertFalse(config.readNullToEmpty());
    }

    @Test
    public void testGetMapInstanceType() {
        Assertions.assertEquals(HashMap.class, config.getMapInstanceType());
    }

    @Test
    public void testSetMapInstanceType() {
        JsonDeserializationConfig result = config.setMapInstanceType(java.util.LinkedHashMap.class);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(java.util.LinkedHashMap.class, config.getMapInstanceType());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setMapInstanceType(null);
        });
    }

    @Test
    public void testSetPropHandler() {
        BiConsumer<Collection<Object>, Object> handler = (collection, element) -> {
            collection.add(element);
        };

        JsonDeserializationConfig result = config.setPropHandler("testProp", handler);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(handler, config.getPropHandler("testProp"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setPropHandler(null, handler);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setPropHandler("", handler);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setPropHandler("prop", null);
        });
    }

    @Test
    public void testGetPropHandler() {
        Assertions.assertNull(config.getPropHandler("nonExistent"));

        BiConsumer<Collection<?>, Object> handler = (collection, element) -> {
        };
        config.setPropHandler("test", handler);
        Assertions.assertEquals(handler, config.getPropHandler("test"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.getPropHandler("");
        });
    }

    @Test
    public void testHashCode() {
        JsonDeserializationConfig config1 = new JsonDeserializationConfig();
        JsonDeserializationConfig config2 = new JsonDeserializationConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.ignoreNullOrEmpty(true);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        JsonDeserializationConfig config1 = new JsonDeserializationConfig();
        JsonDeserializationConfig config2 = new JsonDeserializationConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        config1.ignoreNullOrEmpty(true);
        Assertions.assertNotEquals(config1, config2);

        config2.ignoreNullOrEmpty(true);
        Assertions.assertEquals(config1, config2);
    }

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("ignoreNullOrEmpty="));
        Assertions.assertTrue(str.contains("readNullToEmpty="));
        Assertions.assertTrue(str.contains("mapInstanceType="));
    }

    @Test
    public void testJDCCreate() {
        JsonDeserializationConfig config = JDC.create();
        Assertions.assertNotNull(config);
        Assertions.assertFalse(config.ignoreNullOrEmpty());
        Assertions.assertFalse(config.readNullToEmpty());
    }

    @Test
    public void testJDCOf() {
        JsonDeserializationConfig config1 = JDC.of(String.class);
        Assertions.assertEquals(String.class, config1.getElementType().clazz());

        JsonDeserializationConfig config2 = JDC.of(String.class, Integer.class);
        Assertions.assertEquals(String.class, config2.getMapKeyType().clazz());
        Assertions.assertEquals(Integer.class, config2.getMapValueType().clazz());

        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        JsonDeserializationConfig config3 = JDC.of(true, ignoredProps);
        Assertions.assertTrue(config3.ignoreUnmatchedProperty());

        JsonDeserializationConfig config4 = JDC.of(String.class, true, ignoredProps);
        Assertions.assertEquals(String.class, config4.getElementType().clazz());
        Assertions.assertTrue(config4.ignoreUnmatchedProperty());

        JsonDeserializationConfig config5 = JDC.of(String.class, Integer.class, true, ignoredProps);
        Assertions.assertEquals(String.class, config5.getMapKeyType().clazz());
        Assertions.assertEquals(Integer.class, config5.getMapValueType().clazz());
        Assertions.assertTrue(config5.ignoreUnmatchedProperty());

        JsonDeserializationConfig config6 = JDC.of(ArrayList.class, String.class, Integer.class, true, ignoredProps);
        Assertions.assertEquals(ArrayList.class, config6.getElementType().clazz());
        Assertions.assertEquals(String.class, config6.getMapKeyType().clazz());
        Assertions.assertEquals(Integer.class, config6.getMapValueType().clazz());
        Assertions.assertTrue(config6.ignoreUnmatchedProperty());
    }
}
