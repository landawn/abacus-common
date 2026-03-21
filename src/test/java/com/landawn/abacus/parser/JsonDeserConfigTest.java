package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JsonDeserConfigTest extends TestBase {

    private JsonDeserConfig config;

    @BeforeEach
    public void setUp() {
        config = new JsonDeserConfig();
    }

    // isIgnoreNullOrEmpty
    @Test
    public void testIsIgnoreNullOrEmpty() {
        Assertions.assertFalse(config.isIgnoreNullOrEmpty());
    }

    // setIgnoreNullOrEmpty
    @Test
    public void testIgnoreNullOrEmpty() {
        Assertions.assertFalse(config.isIgnoreNullOrEmpty());

        JsonDeserConfig result = config.setIgnoreNullOrEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isIgnoreNullOrEmpty());

        config.setIgnoreNullOrEmpty(false);
        Assertions.assertFalse(config.isIgnoreNullOrEmpty());
    }

    // copy
    @Test
    public void testCopy() {
        config.setIgnoreNullOrEmpty(true);
        config.setReadNullToEmpty(true);
        config.setMapInstanceType(java.util.LinkedHashMap.class);

        JsonDeserConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertTrue(copy.isIgnoreNullOrEmpty());
        assertTrue(copy.isReadNullToEmpty());
        assertEquals(java.util.LinkedHashMap.class, copy.getMapInstanceType());
    }

    // isReadNullToEmpty
    @Test
    public void testIsReadNullToEmpty() {
        Assertions.assertFalse(config.isReadNullToEmpty());
    }

    // setReadNullToEmpty
    @Test
    public void testReadNullToEmpty() {
        Assertions.assertFalse(config.isReadNullToEmpty());

        JsonDeserConfig result = config.setReadNullToEmpty(true);
        Assertions.assertSame(config, result);
        Assertions.assertTrue(config.isReadNullToEmpty());

        config.setReadNullToEmpty(false);
        Assertions.assertFalse(config.isReadNullToEmpty());
    }

    // getMapInstanceType
    @Test
    public void testGetMapInstanceType() {
        Assertions.assertEquals(HashMap.class, config.getMapInstanceType());
    }

    // setMapInstanceType
    @Test
    public void testSetMapInstanceType() {
        JsonDeserConfig result = config.setMapInstanceType(java.util.LinkedHashMap.class);
        Assertions.assertSame(config, result);
        Assertions.assertEquals(java.util.LinkedHashMap.class, config.getMapInstanceType());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setMapInstanceType(null);
        });
    }

    // setPropHandler
    @Test
    public void testSetPropHandler() {
        BiConsumer<Collection<Object>, Object> handler = (collection, element) -> {
            collection.add(element);
        };

        JsonDeserConfig result = config.setPropHandler("testProp", handler);
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

    // getPropHandler
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

    // hashCode
    @Test
    public void test_hashCode() {
        JsonDeserConfig config1 = new JsonDeserConfig();
        JsonDeserConfig config2 = new JsonDeserConfig();

        assertEquals(config1.hashCode(), config2.hashCode());

        config2.setIgnoreNullOrEmpty(true);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode() {
        JsonDeserConfig config1 = new JsonDeserConfig();
        JsonDeserConfig config2 = new JsonDeserConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.setIgnoreNullOrEmpty(true);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    // equals
    @Test
    public void test_equals() {
        JsonDeserConfig config1 = new JsonDeserConfig();
        JsonDeserConfig config2 = new JsonDeserConfig();

        assertTrue(config1.equals(config1));
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));

        config2.setIgnoreNullOrEmpty(true);
        assertFalse(config1.equals(config2));

        config2.setIgnoreNullOrEmpty(false);
        assertTrue(config1.equals(config2));

        config2.setReadNullToEmpty(true);
        assertFalse(config1.equals(config2));

        assertFalse(config1.equals(null));
        assertFalse(config1.equals("not a config"));
    }

    @Test
    public void testEquals() {
        JsonDeserConfig config1 = new JsonDeserConfig();
        JsonDeserConfig config2 = new JsonDeserConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        config1.setIgnoreNullOrEmpty(true);
        Assertions.assertNotEquals(config1, config2);

        config2.setIgnoreNullOrEmpty(true);
        Assertions.assertEquals(config1, config2);
    }

    // toString
    @Test
    public void test_toString() {
        JsonDeserConfig config = new JsonDeserConfig();
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("ignoreNullOrEmpty"));
        assertTrue(str.contains("readNullToEmpty"));
        assertTrue(str.contains("mapInstanceType"));
    }

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("ignoreNullOrEmpty="));
        Assertions.assertTrue(str.contains("readNullToEmpty="));
        Assertions.assertTrue(str.contains("mapInstanceType="));
    }

    // combined config tests
    @Test
    public void testJDCOf() {
        JsonDeserConfig config1 = JsonDeserConfig.create().setElementType(String.class);
        Assertions.assertEquals(String.class, config1.getElementType().javaType());

        JsonDeserConfig config2 = JsonDeserConfig.create().setMapKeyType(String.class).setMapValueType(Integer.class);
        Assertions.assertEquals(String.class, config2.getMapKeyType().javaType());
        Assertions.assertEquals(Integer.class, config2.getMapValueType().javaType());

        Map<Class<?>, Set<String>> ignoredProps = new HashMap<>();
        JsonDeserConfig config3 = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true).setIgnoredPropNames(ignoredProps);
        Assertions.assertTrue(config3.isIgnoreUnmatchedProperty());

        JsonDeserConfig config4 = JsonDeserConfig.create().setElementType(String.class).setIgnoreUnmatchedProperty(true).setIgnoredPropNames(ignoredProps);
        Assertions.assertEquals(String.class, config4.getElementType().javaType());
        Assertions.assertTrue(config4.isIgnoreUnmatchedProperty());

        JsonDeserConfig config5 = JsonDeserConfig.create()
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class)
                .setIgnoreUnmatchedProperty(true)
                .setIgnoredPropNames(ignoredProps);
        Assertions.assertEquals(String.class, config5.getMapKeyType().javaType());
        Assertions.assertEquals(Integer.class, config5.getMapValueType().javaType());
        Assertions.assertTrue(config5.isIgnoreUnmatchedProperty());

        JsonDeserConfig config6 = JsonDeserConfig.create()
                .setElementType(ArrayList.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class)
                .setIgnoreUnmatchedProperty(true)
                .setIgnoredPropNames(ignoredProps);
        Assertions.assertEquals(ArrayList.class, config6.getElementType().javaType());
        Assertions.assertEquals(String.class, config6.getMapKeyType().javaType());
        Assertions.assertEquals(Integer.class, config6.getMapValueType().javaType());
        Assertions.assertTrue(config6.isIgnoreUnmatchedProperty());
    }

    // create
    @Test
    public void testJDCCreate() {
        JsonDeserConfig config = JsonDeserConfig.create();
        Assertions.assertNotNull(config);
        Assertions.assertFalse(config.isIgnoreNullOrEmpty());
        Assertions.assertFalse(config.isReadNullToEmpty());
    }

    // setIgnoreUnmatchedProperty / isIgnoreUnmatchedProperty
    @Test
    public void testIgnoreUnmatchedProperty() {
        assertTrue(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(false);
        assertFalse(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

    // setElementType / getElementType
    @Test
    public void testElementType() {
        Assertions.assertNull(config.getElementType());

        config.setElementType(String.class);
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().javaType());
    }

    // setMapKeyType / getMapKeyType and setMapValueType / getMapValueType
    @Test
    public void testMapKeyValueType() {
        Assertions.assertNull(config.getMapKeyType());
        Assertions.assertNull(config.getMapValueType());

        config.setMapKeyType(String.class);
        config.setMapValueType(Integer.class);
        assertEquals(String.class, config.getMapKeyType().javaType());
        assertEquals(Integer.class, config.getMapValueType().javaType());
    }

}
