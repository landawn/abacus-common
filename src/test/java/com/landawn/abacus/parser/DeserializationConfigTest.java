package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

public class DeserializationConfigTest extends TestBase {

    private TestDeserializationConfig config;

    private static class TestDeserializationConfig extends DeserializationConfig<TestDeserializationConfig> {
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
        DeserializationConfig<?> config1 = new JsonDeserConfig();
        DeserializationConfig<?> config2 = new JsonDeserConfig();

        Assertions.assertEquals(config1.hashCode(), config2.hashCode());

        config1.setElementType(String.class);
        Assertions.assertNotEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void testEquals() {
        DeserializationConfig<?> config1 = new JsonDeserConfig();
        DeserializationConfig<?> config2 = new JsonDeserConfig();

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

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P8-08 copy() clones the value-type map; P8-10 IAE for non-Class/
    // non-ParameterizedType; P8-12 null key rejected; P8-13 javadoc pin)
    // ---------------------------------------------------------------------------------------------

    /** Generic bean so that a {@code Box<String>} field yields a ParameterizedType with a bean raw type. */
    public static class Box<T> {
        private T value;

        public T getValue() {
            return value;
        }

        public void setValue(final T value) {
            this.value = value;
        }
    }

    @SuppressWarnings("unused")
    private static class ReflectTypes {
        List<?> wildcard;
        List<String>[] genericArray;
        Box<String> parameterizedBean;

        <T> void typeVariable(final T t) {
        }
    }

    private static java.lang.reflect.Type reflectType(final String fieldName) throws Exception {
        return ReflectTypes.class.getDeclaredField(fieldName).getGenericType();
    }

    @Test
    public void reviewFixes20260906_setValueType_nullKeyRejected_allOverloads() {
        final Type<String> stringType = Type.of(String.class);

        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType(null, stringType));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType(null, String.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType(null, "String"));

        // nothing was stored: no map created, no null key, no leak into equals/toString
        assertFalse(config.hasValueTypes());
        assertNull(config.getValueType(null));
        assertEquals(new TestDeserializationConfig(), config);
        assertTrue(config.toString().contains("valueTypeMap=null"), config.toString());

        // an existing map is left untouched by a rejected call
        config.setValueType("x", stringType);
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType(null, stringType));
        assertEquals(1, config.valueTypeMap.size());
        assertSame(stringType, config.getValueType("x"));
    }

    @Test
    public void reviewFixes20260906_setValueType_emptyKeyAccepted_andConsultedForEmptyMapKey() {
        final Type<Integer> intType = Type.of(Integer.class);
        assertSame(config, config.setValueType("", intType));
        assertTrue(config.hasValueTypes());
        assertSame(intType, config.getValueType(""));

        // the "" entry is what the JSON parser consults for the empty map key
        final JsonParser jp = ParserFactory.createJsonParser();
        final Map<String, Object> result = jp.deserialize("{\"\":\"1\",\"x\":\"2\"}", new JsonDeserConfig().setValueType("", Integer.class), Map.class);
        assertEquals(Integer.valueOf(1), result.get(""));
        assertEquals("2", result.get("x"));
    }

    @Test
    public void reviewFixes20260906_setValueTypesByBeanClass_wildcardTypeThrowsIAE() throws Exception {
        final java.lang.reflect.Type wildcard = ((ParameterizedType) reflectType("wildcard")).getActualTypeArguments()[0];
        assertTrue(wildcard instanceof WildcardType);

        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypesByBeanClass(wildcard));
        assertTrue(e.getMessage().contains("Not a bean type"), e.getMessage());
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void reviewFixes20260906_setValueTypesByBeanClass_genericArrayTypeThrowsIAE() throws Exception {
        final java.lang.reflect.Type genericArray = reflectType("genericArray");
        assertTrue(genericArray instanceof GenericArrayType);

        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypesByBeanClass(genericArray));
        assertTrue(e.getMessage().contains("Not a bean type"), e.getMessage());
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void reviewFixes20260906_setValueTypesByBeanClass_typeVariableThrowsIAE() throws Exception {
        final java.lang.reflect.Type typeVar = ReflectTypes.class.getDeclaredMethod("typeVariable", Object.class).getGenericParameterTypes()[0];
        assertTrue(typeVar instanceof TypeVariable);

        // a previously configured bean must survive the rejected call
        config.setValueTypesByBeanClass(TestBean.class);
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypesByBeanClass(typeVar));
        assertTrue(e.getMessage().contains("Not a bean type"), e.getMessage());
        assertTrue(config.hasValueTypes());
        assertEquals(String.class, config.getValueType("name").javaType());
    }

    @Test
    public void reviewFixes20260906_setValueTypesByBeanClass_classAndParameterizedBeanStillAccepted_nonBeanStillIAE() throws Exception {
        // ParameterizedType with a bean raw type is still accepted and resolves property types
        final java.lang.reflect.Type parameterizedBean = reflectType("parameterizedBean");
        assertTrue(parameterizedBean instanceof ParameterizedType);
        assertSame(config, config.setValueTypesByBeanClass(parameterizedBean));
        assertTrue(config.hasValueTypes());
        assertNotNull(config.getValueType("value"));

        // non-bean Class / non-bean ParameterizedType keep the original IAE (not a CCE)
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypesByBeanClass(String.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypesByBeanClass(reflectType("wildcard")));
        // null still clears
        config.setValueTypesByBeanClass(null);
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void reviewFixes20260906_copy_valueTypeMapIsNotSharedWithTheOriginal() {
        final Type<String> stringType = Type.of(String.class);
        final Type<Integer> intType = Type.of(Integer.class);
        config.setValueType("x", stringType);

        final TestDeserializationConfig copy = config.copy();
        assertNotSame(config, copy);
        assertEquals(config, copy);
        assertEquals(config.hashCode(), copy.hashCode());
        assertNotSame(config.valueTypeMap, copy.valueTypeMap);
        assertSame(stringType, copy.getValueType("x"));

        // mutating the copy must not leak into the original (this was the state-dependent leak)
        copy.setValueType("y", intType);
        assertNull(config.getValueType("y"));
        assertEquals(1, config.valueTypeMap.size());
        assertSame(intType, copy.getValueType("y"));
        Assertions.assertNotEquals(config, copy);

        // ... and mutating the original must not leak into the copy
        config.setValueType("z", intType);
        assertNull(copy.getValueType("z"));
        assertEquals(2, copy.valueTypeMap.size());
    }

    @Test
    public void reviewFixes20260906_copy_withoutValueTypes_lazyMapOnCopyDoesNotLeak() {
        final TestDeserializationConfig copy = config.copy();
        assertNull(copy.valueTypeMap);

        copy.setValueType("y", Integer.class);
        assertFalse(config.hasValueTypes());
        assertNull(config.valueTypeMap);
        assertTrue(copy.hasValueTypes());

        // setValueTypes(Map) replaces the copy's map by reference, as documented, without touching the original
        final Map<String, Type<?>> shared = new HashMap<>();
        shared.put("q", Type.of(Long.class));
        config.setValueTypes(shared);
        final TestDeserializationConfig copy2 = config.copy();
        assertNotSame(shared, copy2.valueTypeMap);
        shared.put("r", Type.of(Long.class));
        assertNotNull(config.getValueType("r"));
        assertNull(copy2.getValueType("r"));
    }

    public static class Named {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // G09 fixes 2026-09-08: null keys and null types are rejected at every value-type door
    // ---------------------------------------------------------------------------------------------

    @Test
    public void g09_setValueType_nullTypeIsRejectedByEveryOverload() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType("x", (Type<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType("x", (Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType("x", (String) null));

        // nothing was installed, so a configured key still means "there is a type for it"
        assertFalse(config.hasValueTypes());
        assertNull(config.getValueType("x"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueType(null, Type.of(String.class)));
    }

    @Test
    public void g09_setValueTypes_rejectsNullKeysAndNullTypes() {
        final Map<String, Type<?>> nullKey = new HashMap<>();
        nullKey.put(null, Type.of(String.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypes(nullKey));

        final Map<String, Type<?>> nullType = new HashMap<>();
        nullType.put("x", null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setValueTypes(nullType));

        // a rejected map is not installed
        assertFalse(config.hasValueTypes());
        assertNull(config.valueTypeMap);

        // a valid map still is, and null still clears
        final Map<String, Type<?>> valid = new HashMap<>();
        valid.put("x", Type.of(String.class));
        assertSame(config, config.setValueTypes(valid));
        assertSame(valid, config.valueTypeMap);
        assertEquals(String.class, config.getValueType("x").javaType());

        config.setValueTypes(null);
        assertFalse(config.hasValueTypes());
        assertNull(config.valueTypeMap);
    }

    @Test
    public void reviewFixes20260906_ignoreUnmatchedProperty_false_throwsParsingException() {
        final JsonParser jp = ParserFactory.createJsonParser();
        final ParsingException e = Assertions.assertThrows(ParsingException.class,
                () -> jp.deserialize("{\"name\":\"n\",\"zzz\":1}", new JsonDeserConfig().setIgnoreUnmatchedProperty(false), Named.class));
        assertTrue(e.getMessage().contains("zzz"), e.getMessage());

        final Named bean = jp.deserialize("{\"name\":\"n\",\"zzz\":1}", new JsonDeserConfig().setIgnoreUnmatchedProperty(true), Named.class);
        assertEquals("n", bean.getName());
    }

}
