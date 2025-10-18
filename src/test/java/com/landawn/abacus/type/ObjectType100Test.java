package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ObjectType100Test extends TestBase {

    private ObjectType<Object> objectType;
    private Type<String> stringObjectType;
    private Type<Integer> integerObjectType;

    @BeforeEach
    public void setUp() {
        objectType = (ObjectType<Object>) createType(Object.class);
        stringObjectType = createType(String.class);
        integerObjectType = createType(Integer.class);
    }

    @Test
    public void testDefaultConstructor() {
        ObjectType<Object> defaultType = (ObjectType<Object>) createType("Object");
        assertNotNull(defaultType);
        assertEquals(Object.class, defaultType.clazz());
    }

    @Test
    public void testConstructorWithClass() {
        assertEquals(String.class, stringObjectType.clazz());
        assertEquals(Integer.class, integerObjectType.clazz());
    }

    @Test
    public void testConstructorWithTypeNameAndClass() {
        ObjectType<String> customType = (ObjectType<String>) createType("CustomString");
        assertNotNull(customType);
        assertEquals("CustomString", customType.name());
    }

    @Test
    public void testClazz() {
        assertEquals(Object.class, objectType.clazz());
        assertEquals(String.class, stringObjectType.clazz());
        assertEquals(Integer.class, integerObjectType.clazz());
    }

    @Test
    public void testName() {
        assertNotNull(objectType.name());
        assertNotNull(stringObjectType.name());
        assertNotNull(integerObjectType.name());
    }

    @Test
    public void testIsGenericType() {
        assertFalse(objectType.isGenericType());
        assertFalse(stringObjectType.isGenericType());
        assertFalse(integerObjectType.isGenericType());
    }

    @Test
    public void testIsPrimitiveType() {
        assertFalse(objectType.isPrimitiveType());
        assertFalse(stringObjectType.isPrimitiveType());
        assertFalse(integerObjectType.isPrimitiveType());
    }

    @Test
    public void testIsObjectType() {
        assertTrue(objectType.isObjectType());
        assertFalse(stringObjectType.isObjectType());
        assertFalse(integerObjectType.isObjectType());
    }

    @Test
    public void testStringOf() {
        assertEquals("test", stringObjectType.stringOf("test"));
        assertEquals("123", integerObjectType.stringOf(123));
        assertNull(objectType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertEquals("test", stringObjectType.valueOf("test"));
        assertEquals(123, integerObjectType.valueOf("123"));
        assertNull(objectType.valueOf(null));
    }

    @Test
    public void testIsSerializable() {
        assertTrue(stringObjectType.isSerializable());
        assertTrue(integerObjectType.isSerializable());
    }

    @Test
    public void testEquals() {
        String str1 = "test";
        String str2 = "test";
        String str3 = "different";

        assertTrue(stringObjectType.equals(str1, str2));
        assertFalse(stringObjectType.equals(str1, str3));
        assertTrue(stringObjectType.equals(null, null));
        assertFalse(stringObjectType.equals(str1, null));
        assertFalse(stringObjectType.equals(null, str1));
    }

    @Test
    public void testHashCode() {
        String str = "test";
        Integer num = 123;

        assertEquals(str.hashCode(), stringObjectType.hashCode(str));
        assertEquals(num.hashCode(), integerObjectType.hashCode(num));
        assertEquals(0, objectType.hashCode(null));
    }

    @Test
    public void testToString() {
        assertEquals("test", stringObjectType.toString("test"));
        assertEquals("123", integerObjectType.toString(123));
        assertEquals("null", objectType.toString(null));
    }
}
