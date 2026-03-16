package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("2025")
public class DeserializationConfigTest extends TestBase {

    private TestDeserializationConfig config;

    private static class TestDeserializationConfig extends Deserialization<TestDeserializationConfig> {
        // Concrete implementation for testing
    }

    private static class TestBean {
        private String name;
        private int age;
        private TestAddress address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public TestAddress getAddress() {
            return address;
        }

        public void setAddress(TestAddress address) {
            this.address = address;
        }
    }

    private static class TestAddress {
        private String street;
        private String city;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    @BeforeEach
    public void setUp() {
        config = new TestDeserializationConfig();
    }

    @Test
    public void test_ignoreUnmatchedProperty_defaultValue() {
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_setTrue() {
        config.setIgnoreUnmatchedProperty(true);
        assertTrue(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_setFalse() {
        config.setIgnoreUnmatchedProperty(false);
        assertFalse(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_methodChaining() {
        TestDeserializationConfig result = config.setIgnoreUnmatchedProperty(false);
        assertEquals(config, result);
        assertFalse(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void test_getElementType_defaultValue() {
        assertNull(config.getElementType());
    }

    @Test
    public void test_setElementType_withClass() {
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().javaType());
    }

    @Test
    public void test_setElementType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setElementType(type);
        assertSame(type, config.getElementType());
    }

    @Test
    public void test_setElementType_withString() {
        config.setElementType("String");
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().javaType());
    }

    @Test
    public void test_setElementType_methodChaining() {
        TestDeserializationConfig result = config.setElementType(String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_getMapKeyType_defaultValue() {
        assertNull(config.getMapKeyType());
    }

    @Test
    public void test_setMapKeyType_withClass() {
        config.setMapKeyType(String.class);
        assertNotNull(config.getMapKeyType());
        assertEquals(String.class, config.getMapKeyType().javaType());
    }

    @Test
    public void test_setMapKeyType_withType() {
        Type<String> type = N.typeOf(String.class);
        config.setMapKeyType(type);
        assertSame(type, config.getMapKeyType());
    }

    @Test
    public void test_setMapKeyType_withString() {
        config.setMapKeyType("String");
        assertNotNull(config.getMapKeyType());
        assertEquals(String.class, config.getMapKeyType().javaType());
    }

    @Test
    public void test_setMapKeyType_methodChaining() {
        TestDeserializationConfig result = config.setMapKeyType(String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_getMapValueType_defaultValue() {
        assertNull(config.getMapValueType());
    }

    @Test
    public void test_setMapValueType_withClass() {
        config.setMapValueType(Integer.class);
        assertNotNull(config.getMapValueType());
        assertEquals(Integer.class, config.getMapValueType().javaType());
    }

    @Test
    public void test_setMapValueType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setMapValueType(type);
        assertSame(type, config.getMapValueType());
    }

    @Test
    public void test_setMapValueType_withString() {
        config.setMapValueType("Integer");
        assertNotNull(config.getMapValueType());
        assertEquals(Integer.class, config.getMapValueType().javaType());
    }

    @Test
    public void test_setMapValueType_methodChaining() {
        TestDeserializationConfig result = config.setMapValueType(Integer.class);
        assertEquals(config, result);
    }

    @Test
    public void test_hasValueTypes_defaultFalse() {
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void test_hasValueTypes_withValueTypeMap() {
        config.setValueType("key", String.class);
        assertTrue(config.hasValueTypes());
    }

    @Test
    public void test_hasValueTypes_withBeanClass() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertTrue(config.hasValueTypes());
    }

    @Test
    public void test_getValueType_withoutDefault() {
        assertNull(config.getValueType("unknown"));
    }

    @Test
    public void test_getValueType_withDefault() {
        Type<String> defaultType = N.typeOf(String.class);
        Type<String> result = config.getValueType("unknown", defaultType);
        assertSame(defaultType, result);
    }

    @Test
    public void test_getValueType_configured() {
        config.setValueType("name", String.class);
        Type<?> result = config.getValueType("name");
        assertNotNull(result);
        assertEquals(String.class, result.javaType());
    }

    @Test
    public void test_setValueType_withType() {
        Type<String> type = N.typeOf(String.class);
        config.setValueType("name", type);
        assertSame(type, config.getValueType("name"));
    }

    @Test
    public void test_setValueType_methodChaining() {
        TestDeserializationConfig result = config.setValueType("name", String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_setValueTypes_methodChaining() {
        Map<String, Type<?>> types = new HashMap<>();
        TestDeserializationConfig result = config.setValueTypes(types);
        assertEquals(config, result);
    }

    @Test
    public void test_setValueTypesByBeanClass() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertNotNull(config.getValueType("name"));
        assertNotNull(config.getValueType("age"));
        assertNotNull(config.getValueType("address"));
    }

    @Test
    public void test_setValueTypesByBeanClass_withNull() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertTrue(config.hasValueTypes());

        config.setValueTypesByBeanClass(null);
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void test_setValueTypesByBeanClass_methodChaining() {
        TestDeserializationConfig result = config.setValueTypesByBeanClass(TestBean.class);
        assertEquals(config, result);
    }

    @Test
    public void test_hashCode_defaultConfig() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        TestDeserializationConfig config2 = new TestDeserializationConfig();
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withSameSettings() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setElementType(String.class);
        config1.setIgnoreUnmatchedProperty(false);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setElementType(String.class);
        config2.setIgnoreUnmatchedProperty(false);

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_equals_sameInstance() {
        assertTrue(config.equals(config));
    }

    @Test
    public void test_equals_defaultConfigs() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        TestDeserializationConfig config2 = new TestDeserializationConfig();
        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentIgnoreUnmatchedProperty() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setIgnoreUnmatchedProperty(true);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setIgnoreUnmatchedProperty(false);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentElementType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setElementType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setElementType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentMapKeyType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setMapKeyType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setMapKeyType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentMapValueType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setMapValueType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setMapValueType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withNull() {
        assertFalse(config.equals(null));
    }

    @Test
    public void test_equals_withDifferentType() {
        assertFalse(config.equals("not a config"));
    }

    @Test
    public void test_toString_defaultConfig() {
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("ignoredPropNames"));
        assertTrue(result.contains("ignoreUnmatchedProperty"));
        assertTrue(result.contains("elementType"));
        assertTrue(result.contains("mapKeyType"));
        assertTrue(result.contains("mapValueType"));
    }

    @Test
    public void test_toString_withElementType() {
        config.setElementType(String.class);
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("String"));
    }

    @Test
    public void test_copy_createsNewInstance() {
        TestDeserializationConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
    }

    @Test
    public void test_copy_copiesSettings() {
        config.setElementType(String.class);
        config.setMapKeyType(String.class);
        config.setMapValueType(Integer.class);
        config.setIgnoreUnmatchedProperty(false);

        TestDeserializationConfig copy = config.copy();

        assertNotNull(copy.getElementType());
        assertNotNull(copy.getMapKeyType());
        assertNotNull(copy.getMapValueType());
        assertFalse(copy.isIgnoreUnmatchedProperty());
    }

    @Test
    public void test_methodChaining_complex() {
        TestDeserializationConfig result = config.setIgnoreUnmatchedProperty(false)
                .setElementType(String.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class)
                .setValueType("name", String.class);

        assertEquals(config, result);
        assertFalse(config.isIgnoreUnmatchedProperty());
        assertNotNull(config.getElementType());
        assertNotNull(config.getMapKeyType());
        assertNotNull(config.getMapValueType());
        assertNotNull(config.getValueType("name"));
    }

    @Test
    public void testIgnoreUnmatchedProperty() {
        Assertions.assertTrue(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(false);
        Assertions.assertFalse(config.isIgnoreUnmatchedProperty());

        config.setIgnoreUnmatchedProperty(true);
        Assertions.assertTrue(config.isIgnoreUnmatchedProperty());
    }

    @Test
    public void testSetElementTypeWithClass() {
        config.setElementType(String.class);
        Type<?> eleType = config.getElementType();
        Assertions.assertNotNull(eleType);
        Assertions.assertEquals(String.class, eleType.javaType());
    }

    @Test
    public void testSetElementTypeWithType() {
        Type<String> stringType = N.typeOf(String.class);
        config.setElementType(stringType);
        Type<?> eleType = config.getElementType();
        Assertions.assertEquals(stringType, eleType);
    }

    @Test
    public void testSetElementTypeWithString() {
        config.setElementType("String");
        Type<?> eleType = config.getElementType();
        Assertions.assertNotNull(eleType);
        Assertions.assertEquals(String.class, eleType.javaType());
    }

    @Test
    public void testSetMapKeyTypeWithClass() {
        config.setMapKeyType(Long.class);
        Type<?> keyType = config.getMapKeyType();
        Assertions.assertNotNull(keyType);
        Assertions.assertEquals(Long.class, keyType.javaType());
    }

    @Test
    public void testSetMapKeyTypeWithType() {
        Type<Integer> intType = N.typeOf(Integer.class);
        config.setMapKeyType(intType);
        Type<?> keyType = config.getMapKeyType();
        Assertions.assertEquals(intType, keyType);
    }

    @Test
    public void testSetMapKeyTypeWithString() {
        config.setMapKeyType("Integer");
        Type<?> keyType = config.getMapKeyType();
        Assertions.assertNotNull(keyType);
        Assertions.assertEquals(Integer.class, keyType.javaType());
    }

    @Test
    public void testSetMapValueTypeWithClass() {
        config.setMapValueType(Double.class);
        Type<?> valueType = config.getMapValueType();
        Assertions.assertNotNull(valueType);
        Assertions.assertEquals(Double.class, valueType.javaType());
    }

    @Test
    public void testSetMapValueTypeWithType() {
        Type<Boolean> boolType = N.typeOf(Boolean.class);
        config.setMapValueType(boolType);
        Type<?> valueType = config.getMapValueType();
        Assertions.assertEquals(boolType, valueType);
    }

    @Test
    public void testSetMapValueTypeWithString() {
        config.setMapValueType("Boolean");
        Type<?> valueType = config.getMapValueType();
        Assertions.assertNotNull(valueType);
        Assertions.assertEquals(Boolean.class, valueType.javaType());
    }

    @Test
    public void testHasValueTypes() {
        Assertions.assertFalse(config.hasValueTypes());

        config.setValueType("prop1", String.class);
        Assertions.assertTrue(config.hasValueTypes());
    }

    @Test
    public void testGetValueType() {
        Assertions.assertNull(config.getValueType("nonExistent"));

        config.setValueType("prop1", String.class);
        Type<?> type = config.getValueType("prop1");
        Assertions.assertNotNull(type);
        Assertions.assertEquals(String.class, type.javaType());
    }

    @Test
    public void testGetValueTypeWithDefault() {
        Type<Integer> intType = N.typeOf(Integer.class);
        Type<?> result = config.getValueType("nonExistent", intType);
        Assertions.assertEquals(intType, result);

        config.setValueType("prop1", String.class);
        Type<?> type = config.getValueType("prop1", intType);
        Assertions.assertNotEquals(intType, type);
        Assertions.assertEquals(String.class, type.javaType());
    }

    @Test
    public void testSetValueTypeWithClass() {
        config.setValueType("prop1", Double.class);
        Type<?> type = config.getValueType("prop1");
        Assertions.assertNotNull(type);
        Assertions.assertEquals(Double.class, type.javaType());
    }

    @Test
    public void testSetValueTypeWithType() {
        Type<Long> longType = N.typeOf(Long.class);
        config.setValueType("prop1", longType);
        Type<?> type = config.getValueType("prop1");
        Assertions.assertEquals(longType, type);
    }

    @Test
    public void testSetValueTypeWithString() {
        config.setValueType("prop1", "Float");
        Type<?> type = config.getValueType("prop1");
        Assertions.assertNotNull(type);
        Assertions.assertEquals(Float.class, type.javaType());
    }

    @Test
    public void testSetValueTypes() {
        Map<String, Type<?>> valueTypes = new HashMap<>();
        valueTypes.put("prop1", N.typeOf(String.class));
        valueTypes.put("prop2", N.typeOf(Integer.class));

        config.setValueTypes(valueTypes);

        Assertions.assertEquals(String.class, config.getValueType("prop1").javaType());
        Assertions.assertEquals(Integer.class, config.getValueType("prop2").javaType());
    }

    @Test
    public void testSetValueTypesByBeanClass() {
        config.setValueTypesByBeanClass(TestBean.class);
        Assertions.assertTrue(config.hasValueTypes());

        config.setValueTypesByBeanClass(null);
        Assertions.assertFalse(config.hasValueTypes());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            config.setValueTypesByBeanClass(String.class);
        });
    }

    @Test
    public void testHashCode() {
        Deserialization<?> config1 = new JsonDeserConfig();
        Deserialization<?> config2 = new JsonDeserConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.setElementType(String.class);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        Deserialization<?> config1 = new JsonDeserConfig();
        Deserialization<?> config2 = new JsonDeserConfig();

        Assertions.assertEquals(config1, config1);
        Assertions.assertEquals(config1, config2);
        Assertions.assertNotEquals(config1, null);
        Assertions.assertNotEquals(config1, "string");

        config1.setElementType(String.class);
        Assertions.assertNotEquals(config1, config2);

        config2.setElementType(String.class);
        Assertions.assertEquals(config1, config2);
    }

    @Test
    public void testToString() {
        String str = config.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("ignoreUnmatchedProperty="));
        Assertions.assertTrue(str.contains("elementType="));
        Assertions.assertTrue(str.contains("mapKeyType="));
        Assertions.assertTrue(str.contains("mapValueType="));
    }

}
