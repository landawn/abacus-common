package com.landawn.abacus.parser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class DeserializationConfig100Test extends TestBase {

    private Deserialization<?> config;

    @BeforeEach
    public void setUp() {
        config = new JsonDeserConfig();
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
    public void testGetElementType() {
        Assertions.assertNull(config.getElementType());
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
    public void testGetMapKeyType() {
        Assertions.assertNull(config.getMapKeyType());
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
    public void testGetMapValueType() {
        Assertions.assertNull(config.getMapValueType());
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
