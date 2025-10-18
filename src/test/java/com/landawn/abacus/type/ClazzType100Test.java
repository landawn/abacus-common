package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ClazzType100Test extends TestBase {

    private ClazzType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ClazzType) createType("Clazz<String>");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<Class> result = type.clazz();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testIsImmutable() {
        boolean result = type.isImmutable();
        Assertions.assertTrue(result);
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_SimpleClass() {
        String result = type.stringOf(String.class);
        assertEquals("java.lang.String", result);
    }

    @Test
    public void testStringOf_PrimitiveClass() {
        String result = type.stringOf(int.class);
        assertEquals("int", result);
    }

    @Test
    public void testStringOf_ArrayClass() {
        String result = type.stringOf(String[].class);
        assertEquals("java.lang.String[]", result);
    }

    @Test
    public void testStringOf_PrimitiveArrayClass() {
        String result = type.stringOf(int[].class);
        assertEquals("int[]", result);
    }

    @Test
    public void testStringOf_NestedClass() {
        String result = type.stringOf(java.util.Map.Entry.class);
        assertEquals("java.util.Map.Entry", result);
    }

    @Test
    public void testValueOf_Null() {
        Class result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        Class result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_SimpleClassName() {
        Class result = type.valueOf("java.lang.String");
        assertEquals(String.class, result);
    }

    @Test
    public void testValueOf_PrimitiveName() {
        Class result = type.valueOf("int");
        assertEquals(int.class, result);
    }

    @Test
    public void testValueOf_BooleanPrimitive() {
        Class result = type.valueOf("boolean");
        assertEquals(boolean.class, result);
    }

    @Test
    public void testValueOf_ArrayClassName() {
        Class result = type.valueOf("java.lang.String[]");
        assertEquals(String[].class, result);
    }

    @Test
    public void testValueOf_PrimitiveArrayName() {
        Class result = type.valueOf("int[]");
        assertEquals(int[].class, result);
    }

    @Test
    public void testValueOf_MultidimensionalArray() {
        Class result = type.valueOf("int[][]");
        assertEquals(int[][].class, result);
    }

    @Test
    public void testValueOf_NestedClassName() {
        Class result = type.valueOf("java.util.Map$Entry");
        assertEquals(java.util.Map.Entry.class, result);
    }

    @Test
    public void testRoundTrip_SimpleClass() {
        Class original = Integer.class;
        String str = type.stringOf(original);
        Class restored = type.valueOf(str);
        assertEquals(original, restored);
    }

    @Test
    public void testRoundTrip_ArrayClass() {
        Class original = Object[].class;
        String str = type.stringOf(original);
        Class restored = type.valueOf(str);
        assertEquals(original, restored);
    }

    @Test
    public void testRoundTrip_PrimitiveClass() {
        Class original = double.class;
        String str = type.stringOf(original);
        Class restored = type.valueOf(str);
        assertEquals(original, restored);
    }

    @Test
    public void testAllPrimitiveTypes() {
        Class[] primitives = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class };

        for (Class primitive : primitives) {
            String str = type.stringOf(primitive);
            Class restored = type.valueOf(str);
            assertEquals(primitive, restored, "Failed for primitive: " + primitive);
        }
    }

    @Test
    public void testWrapperClasses() {
        Class[] wrappers = { Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class };

        for (Class wrapper : wrappers) {
            String str = type.stringOf(wrapper);
            Class restored = type.valueOf(str);
            assertEquals(wrapper, restored, "Failed for wrapper: " + wrapper);
        }
    }
}
