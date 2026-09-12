package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ClazzTypeTest extends TestBase {

    private final ClazzType type = new ClazzType(String.class.getName());

    @Test
    public void testClazz() {
        Class<Class> result = type.javaType();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testIsImmutable() {
        boolean result = type.isImmutable();
        Assertions.assertTrue(result);
    }

    @Test
    public void testParameterizedTypeMetadata() {
        Assertions.assertTrue(type.isParameterizedType());
        assertEquals(1, type.parameterTypes().size());
        assertEquals(String.class, type.parameterTypes().get(0).javaType());
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

    @Test
    public void testStringOf_NestedClass() {
        String result = type.stringOf(java.util.Map.Entry.class);
        assertEquals("java.util.Map.Entry", result);
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
    public void testValueOf_NestedClassName() {
        Class result = type.valueOf("java.util.Map$Entry");
        assertEquals(java.util.Map.Entry.class, result);
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    // ---- review fixes 2026-09-06, T2-06: a Class handed to valueOf(Object) is returned as is ----

    @Test
    public void reviewFixes20260906_valueOfObjectAcceptsAClassInstance() {
        // Before the fix: IllegalArgumentException "No class found by name: class java.lang.Integer".
        assertEquals(Integer.class, type.valueOf((Object) Integer.class));
        assertEquals(int[].class, type.valueOf((Object) int[].class));
        assertEquals(java.util.Map.Entry.class, type.valueOf((Object) java.util.Map.Entry.class));
        // Non-Class arguments still go through the string form.
        assertEquals(int[].class, type.valueOf((Object) "int[]"));
        Assertions.assertNull(type.valueOf((Object) null));
        // The pool-registered handler behaves the same.
        assertEquals(Integer.class, ((Type<Class>) createType("Clazz<Object>")).valueOf((Object) Integer.class));
    }

    // ---- T2-13: documented limits of the class-name round trip ----

    @Test
    public void reviewFixes20260906_voidAndHiddenClassNamesDoNotRoundTrip() {
        assertEquals("void", type.stringOf(void.class));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("void"));

        final Runnable lambda = () -> {
        };
        final String hiddenName = type.stringOf(lambda.getClass());
        Assertions.assertNotNull(hiddenName);
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(hiddenName));
    }

    // ---- F117 review fix 2026-09-08: an unresolvable type argument must not degrade to Object ----

    @Test
    public void reviewFixes20260908_unresolvableTypeArgumentIsRejectedAtConstruction() {
        // TypeFactory answers an unknown class token with an ObjectType over Object.class, so this used to
        // construct silently and parameterClass() returned Object.class - which the method's javadoc
        // ("the parameter class of this Clazz<T> type") does not admit.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("Clazz<com.nosuch.Missing>"));
        Assertions.assertTrue(e.getMessage().contains("com.nosuch.Missing"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> new ClazzType("com.nosuch.Missing"));
    }

    @Test
    public void reviewFixes20260908_typeArgumentsThatDoResolveToObjectAreStillAccepted() {
        // The unbounded wildcards are mapped to Object.class by TypeFactory on purpose, and "Object" itself
        // resolves normally; neither may be caught by the unresolvable-name guard.
        assertEquals(Object.class, ((ClazzType) createType("Clazz<?>")).parameterClass());
        assertEquals(Object.class, ((ClazzType) createType("Clazz<? super java.lang.Integer>")).parameterClass());
        assertEquals(Object.class, ((ClazzType) createType("Clazz<Object>")).parameterClass());
        assertEquals(Object.class, ((ClazzType) createType("Clazz<java.lang.Object>")).parameterClass());

        // ... and the resolvable arguments keep reporting the class they resolved to.
        assertEquals(Integer.class, ((ClazzType) createType("Clazz<java.lang.Integer>")).parameterClass());
        assertEquals(Integer.class, ((ClazzType) createType("Clazz<? extends java.lang.Integer>")).parameterClass());
        assertEquals(int.class, ((ClazzType) createType("Clazz<int>")).parameterClass());
        assertEquals(java.util.List.class, ((ClazzType) createType("Clazz<List<String>>")).parameterClass());
        assertEquals(String.class, type.parameterClass());
    }
}
