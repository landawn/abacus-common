package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("2025")
public class XmlDeserConfigTest extends TestBase {

    private XmlDeserConfig config;

    @BeforeEach
    public void setUp() {
        config = new XmlDeserConfig();
    }

    // =====================================================================
    // create
    // =====================================================================

    @Test
    public void testCreate() {
        XmlDeserConfig config = XmlDeserConfig.create();
        assertNotNull(config);
        assertNotSame(config, XmlDeserConfig.create());
    }

    @Test
    public void testCreate_defaultValues() {
        XmlDeserConfig config = XmlDeserConfig.create();
        assertTrue(config.isIgnoreUnmatchedProperty());
        assertNull(config.getElementType());
        assertNull(config.getMapKeyType());
        assertNull(config.getMapValueType());
        assertFalse(config.hasValueTypes());
        assertNull(config.getIgnoredPropNames());
    }

    // =====================================================================
    // isIgnoreUnmatchedProperty / setIgnoreUnmatchedProperty
    // =====================================================================

    @Test
    public void test_ignoreUnmatchedProperty() {
        XmlDeserConfig config = new XmlDeserConfig();
        XmlDeserConfig result = config.setIgnoreUnmatchedProperty(true);
        assertSame(config, result);
        assertEquals(true, config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(false);
        assertEquals(false, config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void testIgnoreUnmatchedProperty_defaultTrue() {
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void testIgnoreUnmatchedProperty_setFalse() {
        config.setIgnoreUnmatchedProperty(false);
        assertFalse(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void testIgnoreUnmatchedProperty_toggle() {
        config.setIgnoreUnmatchedProperty(false);
        assertFalse(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

    // =====================================================================
    // setIgnoredPropNames
    // =====================================================================

    @Test
    public void test_setIgnoredPropNames() {
        XmlDeserConfig config = new XmlDeserConfig();
        Map<Class<?>, Set<String>> ignoredPropNames = new HashMap<>();
        Set<String> props = new HashSet<>();
        props.add("prop1");
        props.add("prop2");
        ignoredPropNames.put(String.class, props);

        XmlDeserConfig result = config.setIgnoredPropNames(ignoredPropNames);
        assertSame(config, result);
        assertNotNull(config.getIgnoredPropNames());
    }

    @Test
    public void testSetIgnoredPropNames_global() {
        Set<String> props = new HashSet<>();
        props.add("field1");
        props.add("field2");

        config.setIgnoredPropNames(props);

        assertNotNull(config.getIgnoredPropNames());
        assertTrue(config.getIgnoredPropNames().containsKey(Object.class));
    }

    @Test
    public void testSetIgnoredPropNames_forSpecificClass() {
        Set<String> props = new HashSet<>();
        props.add("password");

        config.setIgnoredPropNames(String.class, props);

        Collection<String> result = config.getIgnoredPropNames(String.class);
        assertNotNull(result);
        assertTrue(result.contains("password"));
    }

    @Test
    public void testGetIgnoredPropNames_fallbackToGlobal() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("globalField");

        config.setIgnoredPropNames(globalProps);

        Collection<String> result = config.getIgnoredPropNames(Integer.class);
        assertNotNull(result);
        assertTrue(result.contains("globalField"));
    }

    @Test
    public void testGetIgnoredPropNames_classSpecificOverridesGlobal() {
        Set<String> globalProps = new HashSet<>();
        globalProps.add("globalField");
        config.setIgnoredPropNames(globalProps);

        Set<String> classProps = new HashSet<>();
        classProps.add("classField");
        config.setIgnoredPropNames(String.class, classProps);

        Collection<String> result = config.getIgnoredPropNames(String.class);
        assertEquals(classProps, result);
    }

    // =====================================================================
    // getElementType / setElementType
    // =====================================================================

    @Test
    public void testGetElementType_defaultNull() {
        assertNull(config.getElementType());
    }

    @Test
    public void testSetElementType_withClass() {
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().javaType());
    }

    @Test
    public void testSetElementType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setElementType(type);
        assertSame(type, config.getElementType());
    }

    @Test
    public void testSetElementType_withString() {
        config.setElementType("String");
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().javaType());
    }

    @Test
    public void testSetElementType_methodChaining() {
        XmlDeserConfig result = config.setElementType(String.class);
        assertSame(config, result);
    }

    // =====================================================================
    // getMapKeyType / setMapKeyType
    // =====================================================================

    @Test
    public void testGetMapKeyType_defaultNull() {
        assertNull(config.getMapKeyType());
    }

    @Test
    public void testSetMapKeyType_withClass() {
        config.setMapKeyType(String.class);
        assertNotNull(config.getMapKeyType());
        assertEquals(String.class, config.getMapKeyType().javaType());
    }

    @Test
    public void testSetMapKeyType_withType() {
        Type<Long> type = N.typeOf(Long.class);
        config.setMapKeyType(type);
        assertSame(type, config.getMapKeyType());
    }

    @Test
    public void testSetMapKeyType_withString() {
        config.setMapKeyType("Long");
        assertNotNull(config.getMapKeyType());
        assertEquals(Long.class, config.getMapKeyType().javaType());
    }

    // =====================================================================
    // getMapValueType / setMapValueType
    // =====================================================================

    @Test
    public void testGetMapValueType_defaultNull() {
        assertNull(config.getMapValueType());
    }

    @Test
    public void testSetMapValueType_withClass() {
        config.setMapValueType(Integer.class);
        assertNotNull(config.getMapValueType());
        assertEquals(Integer.class, config.getMapValueType().javaType());
    }

    @Test
    public void testSetMapValueType_withType() {
        Type<Double> type = N.typeOf(Double.class);
        config.setMapValueType(type);
        assertSame(type, config.getMapValueType());
    }

    @Test
    public void testSetMapValueType_withString() {
        config.setMapValueType("Double");
        assertNotNull(config.getMapValueType());
        assertEquals(Double.class, config.getMapValueType().javaType());
    }

    // =====================================================================
    // hasValueTypes / getValueType / setValueType
    // =====================================================================

    @Test
    public void testHasValueTypes_defaultFalse() {
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void testHasValueTypes_afterSetValueType() {
        config.setValueType("key", String.class);
        assertTrue(config.hasValueTypes());
    }

    @Test
    public void testGetValueType_notConfigured() {
        assertNull(config.getValueType("unknown"));
    }

    @Test
    public void testGetValueType_withDefault() {
        Type<String> defaultType = N.typeOf(String.class);
        Type<String> result = config.getValueType("unknown", defaultType);
        assertSame(defaultType, result);
    }

    @Test
    public void testSetValueType_withClass() {
        config.setValueType("name", String.class);
        Type<?> result = config.getValueType("name");
        assertNotNull(result);
        assertEquals(String.class, result.javaType());
    }

    @Test
    public void testSetValueType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setValueType("age", type);
        assertSame(type, config.getValueType("age"));
    }

    @Test
    public void testSetValueType_withString() {
        config.setValueType("count", "Integer");
        Type<?> result = config.getValueType("count");
        assertNotNull(result);
        assertEquals(Integer.class, result.javaType());
    }

    @Test
    public void testSetValueTypes_map() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("name", N.typeOf(String.class));
        types.put("age", N.typeOf(Integer.class));

        config.setValueTypes(types);

        assertEquals(String.class, config.getValueType("name").javaType());
        assertEquals(Integer.class, config.getValueType("age").javaType());
    }

    // =====================================================================
    // copy
    // =====================================================================

    @Test
    public void testCopy() {
        config.setIgnoreUnmatchedProperty(false);
        config.setElementType(String.class);
        config.setMapKeyType(String.class);
        config.setMapValueType(Integer.class);

        XmlDeserConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
        assertFalse(copy.isIgnoreUnmatchedProperty());
        assertNotNull(copy.getElementType());
        assertNotNull(copy.getMapKeyType());
        assertNotNull(copy.getMapValueType());
    }

    @Test
    public void testCopy_independentModification() {
        XmlDeserConfig copy = config.copy();
        copy.setIgnoreUnmatchedProperty(false);
        assertTrue(config.isIgnoreUnmatchedProperty());
        assertFalse(copy.isIgnoreUnmatchedProperty());
    }

    // =====================================================================
    // hashCode
    // =====================================================================

    @Test
    public void testHashCode_defaultConfigs() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        XmlDeserConfig config2 = new XmlDeserConfig();
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode_withSameSettings() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setElementType(String.class);
        config1.setIgnoreUnmatchedProperty(false);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setElementType(String.class);
        config2.setIgnoreUnmatchedProperty(false);

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testHashCode_withDifferentSettings() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setElementType(String.class);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setElementType(Integer.class);

        assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    // =====================================================================
    // equals
    // =====================================================================

    @Test
    public void testEquals_sameInstance() {
        assertTrue(config.equals(config));
    }

    @Test
    public void testEquals_defaultConfigs() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        XmlDeserConfig config2 = new XmlDeserConfig();
        assertTrue(config1.equals(config2));
    }

    @Test
    public void testEquals_withNull() {
        assertFalse(config.equals(null));
    }

    @Test
    public void testEquals_withDifferentType() {
        assertFalse(config.equals("not a config"));
    }

    @Test
    public void testEquals_withDifferentIgnoreUnmatchedProperty() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setIgnoreUnmatchedProperty(true);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setIgnoreUnmatchedProperty(false);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEquals_withDifferentElementType() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setElementType(String.class);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setElementType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEquals_withSameElementType() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setElementType(String.class);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setElementType(String.class);

        assertTrue(config1.equals(config2));
    }

    @Test
    public void testEquals_withDifferentMapKeyType() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setMapKeyType(String.class);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setMapKeyType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void testEquals_withDifferentMapValueType() {
        XmlDeserConfig config1 = new XmlDeserConfig();
        config1.setMapValueType(String.class);

        XmlDeserConfig config2 = new XmlDeserConfig();
        config2.setMapValueType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    // =====================================================================
    // toString
    // =====================================================================

    @Test
    public void testToString_defaultConfig() {
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("ignoredPropNames"));
        assertTrue(result.contains("ignoreUnmatchedProperty"));
        assertTrue(result.contains("elementType"));
        assertTrue(result.contains("mapKeyType"));
        assertTrue(result.contains("mapValueType"));
    }

    @Test
    public void testToString_withElementType() {
        config.setElementType(String.class);
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("String"));
    }

    // =====================================================================
    // method chaining
    // =====================================================================

    @Test
    public void testMethodChaining() {
        XmlDeserConfig result = config.setIgnoreUnmatchedProperty(false)
                .setElementType(String.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class)
                .setValueType("name", String.class);

        assertSame(config, result);
        assertFalse(config.isIgnoreUnmatchedProperty());
        assertNotNull(config.getElementType());
        assertNotNull(config.getMapKeyType());
        assertNotNull(config.getMapValueType());
        assertNotNull(config.getValueType("name"));
    }
}
