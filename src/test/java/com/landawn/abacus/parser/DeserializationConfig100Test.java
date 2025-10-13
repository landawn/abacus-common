package com.landawn.abacus.parser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class DeserializationConfig100Test extends TestBase {

    private DeserializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        config = new JSONDeserializationConfig();
    }

    @Test
    public void testIgnoreUnmatchedProperty() {
        Assertions.assertTrue(config.ignoreUnmatchedProperty());

        config.ignoreUnmatchedProperty(false);
        Assertions.assertFalse(config.ignoreUnmatchedProperty());

        config.ignoreUnmatchedProperty(true);
        Assertions.assertTrue(config.ignoreUnmatchedProperty());
    }

    @Test
    public void testGetElementType() {
        Assertions.assertNull(config.getElementType());
    }

    @Test
    public void testSetElementTypeWithClass() {
        config.setElementType(String.class);
        Type<?> eleType = config.getElementType();
        Assertions.assertNotNull(eleType);
        Assertions.assertEquals(String.class, eleType.clazz());
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
        Assertions.assertEquals(String.class, eleType.clazz());
    }

    @Test
    public void testGetMapKeyType() {
        Assertions.assertNull(config.getMapKeyType());
    }

    @Test
    public void testSetMapKeyTypeWithClass() {
        config.setMapKeyType(Long.class);
        Type<?> keyType = config.getMapKeyType();
        Assertions.assertNotNull(keyType);
        Assertions.assertEquals(Long.class, keyType.clazz());
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
        Assertions.assertEquals(Integer.class, keyType.clazz());
    }

    @Test
    public void testGetMapValueType() {
        Assertions.assertNull(config.getMapValueType());
    }

    @Test
    public void testSetMapValueTypeWithClass() {
        config.setMapValueType(Double.class);
        Type<?> valueType = config.getMapValueType();
        Assertions.assertNotNull(valueType);
        Assertions.assertEquals(Double.class, valueType.clazz());
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
        Assertions.assertEquals(Boolean.class, valueType.clazz());
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
        Assertions.assertEquals(String.class, type.clazz());
    }

    @Test
    public void testGetValueTypeWithDefault() {
        Type<Integer> intType = N.typeOf(Integer.class);
        Type<?> result = config.getValueType("nonExistent", intType);
        Assertions.assertEquals(intType, result);

        config.setValueType("prop1", String.class);
        Type<?> type = config.getValueType("prop1", intType);
        Assertions.assertNotEquals(intType, type);
        Assertions.assertEquals(String.class, type.clazz());
    }

    @Test
    public void testGetValueTypeClass() {
        Assertions.assertNull(config.getValueTypeClass("nonExistent"));

        config.setValueType("prop1", String.class);
        Class<?> clazz = config.getValueTypeClass("prop1");
        Assertions.assertEquals(String.class, clazz);
    }

    @Test
    public void testGetValueTypeClassWithDefault() {
        Class<?> result = config.getValueTypeClass("nonExistent", Integer.class);
        Assertions.assertEquals(Integer.class, result);

        config.setValueType("prop1", String.class);
        Class<?> clazz = config.getValueTypeClass("prop1", Integer.class);
        Assertions.assertEquals(String.class, clazz);
    }

    @Test
    public void testSetValueTypeWithClass() {
        config.setValueType("prop1", Double.class);
        Type<?> type = config.getValueType("prop1");
        Assertions.assertNotNull(type);
        Assertions.assertEquals(Double.class, type.clazz());
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
        Assertions.assertEquals(Float.class, type.clazz());
    }

    @Test
    public void testSetValueTypes() {
        Map<String, Type<?>> valueTypes = new HashMap<>();
        valueTypes.put("prop1", N.typeOf(String.class));
        valueTypes.put("prop2", N.typeOf(Integer.class));

        config.setValueTypes(valueTypes);

        Assertions.assertEquals(String.class, config.getValueType("prop1").clazz());
        Assertions.assertEquals(Integer.class, config.getValueType("prop2").clazz());
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
        DeserializationConfig<?> config1 = new JSONDeserializationConfig();
        DeserializationConfig<?> config2 = new JSONDeserializationConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.setElementType(String.class);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        DeserializationConfig<?> config1 = new JSONDeserializationConfig();
        DeserializationConfig<?> config2 = new JSONDeserializationConfig();

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

    public static class TestBean {
        private String name;
        private int age;

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
    }
}
