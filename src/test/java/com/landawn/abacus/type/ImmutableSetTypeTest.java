package com.landawn.abacus.type;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ImmutableSet;

public class ImmutableSetTypeTest extends TestBase {

    private final ImmutableSetType type = new ImmutableSetType("String");

    @Test
    public void testStringOf() {
        ImmutableSet<String> values = ImmutableSet.of("a", "b");
        String str = type.stringOf(values);

        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
    }

    @Test
    public void testValueOf() {
        ImmutableSet<String> values = type.valueOf("[\"a\", \"b\"]");

        assertNotNull(values);
        assertEquals(ImmutableSet.of("a", "b"), values);
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, ImmutableSet.of("x"));
        // appendTo emits the plain, toString()-style form: the String element is NOT quoted
        org.junit.jupiter.api.Assertions.assertEquals("[x]", sb.toString());

        sb.setLength(0);
        type.appendTo(sb, null);
        org.junit.jupiter.api.Assertions.assertEquals("null", sb.toString());
    }

    @Test
    public void testSerializeTo() throws IOException {
        ImmutableSet<String> set = ImmutableSet.of("x");
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        type.serializeTo(writer, set, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        // serializeTo emits JSON: the String element IS quoted; equals stringOf and differs from appendTo
        org.junit.jupiter.api.Assertions.assertEquals("[\"x\"]", json);
        org.junit.jupiter.api.Assertions.assertEquals(type.stringOf(set), json);

        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, set);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }

    @Test
    public void testGetTypeName() {
        String typeName = ImmutableSetType.getTypeName(ImmutableSet.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableSet"));
        assertTrue(typeName.contains("String"));

        typeName = ImmutableSetType.getTypeName(ImmutableSet.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableSet"));
        assertTrue(typeName.contains("String"));
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
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

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testValueOfSortedAndNavigableSubtypes() {
        // regression: sorted/navigable subclasses routed to this handler, but valueOf always
        // produced a plain ImmutableSet -> ClassCastException (and lost sort order)
        final Type<com.landawn.abacus.util.ImmutableSortedSet<Integer>> sortedType = TypeFactory.getType("com.landawn.abacus.util.ImmutableSortedSet<Integer>");
        final com.landawn.abacus.util.ImmutableSortedSet<Integer> ss = sortedType.valueOf("[3, 1, 2]");

        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList(1, 2, 3), new java.util.ArrayList<>(ss));

        final Type<com.landawn.abacus.util.ImmutableNavigableSet<Integer>> navType = TypeFactory
                .getType("com.landawn.abacus.util.ImmutableNavigableSet<Integer>");
        final com.landawn.abacus.util.ImmutableNavigableSet<Integer> ns = navType.valueOf("[3, 1, 2]");

        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), ns.first());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(3), ns.last());
    }

}
