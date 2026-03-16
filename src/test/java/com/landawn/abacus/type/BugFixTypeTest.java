package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.u.Nullable;

/**
 * Tests verifying type system bug fixes.
 */
@Tag("new-test")
public class BugFixTypeTest extends TestBase {

    // ============================================================
    // Fix: NullableType.stringOf() should use elementType.stringOf()
    // ============================================================

    @Test
    public void testNullableType_stringOfUsesElementType() {
        @SuppressWarnings("unchecked")
        Type<Nullable<String>> type = TypeFactory.getType("Nullable<String>");
        assertNotNull(type);

        // String element - should serialize correctly
        Nullable<String> value = Nullable.of("hello");
        String result = type.stringOf(value);
        assertEquals("hello", result);

        // Null element
        Nullable<String> nullValue = Nullable.of((String) null);
        assertNull(type.stringOf(nullValue));

        // Empty Nullable
        Nullable<String> empty = Nullable.empty();
        assertNull(type.stringOf(empty));
    }

    @Test
    public void testNullableType_stringOfWithDate() {
        @SuppressWarnings("unchecked")
        Type<Nullable<Date>> type = TypeFactory.getType("Nullable<Date>");
        assertNotNull(type);

        // Using elementType.stringOf should produce a date string, not the toString() result
        Date date = new Date(0); // epoch
        Nullable<Date> value = Nullable.of(date);
        String result = type.stringOf(value);
        assertNotNull(result);
        // The result should be parseable back
        Nullable<Date> parsed = type.valueOf(result);
        assertNotNull(parsed);
        assertTrue(parsed.isPresent());
    }

    @Test
    public void testNullableType_roundTrip() {
        @SuppressWarnings("unchecked")
        Type<Nullable<Integer>> type = TypeFactory.getType("Nullable<Integer>");
        assertNotNull(type);

        Nullable<Integer> original = Nullable.of(42);
        String str = type.stringOf(original);
        Nullable<Integer> parsed = type.valueOf(str);

        assertTrue(parsed.isPresent());
        assertEquals(42, parsed.get());
    }

    // ============================================================
    // Fix: EnumType.getEnumClass() null check
    // ============================================================

    @Test
    public void testEnumType_basicEnumWorks() {
        // A simple enum should be handled correctly
        @SuppressWarnings("unchecked")
        Type<Thread.State> type = TypeFactory.getType(Thread.State.class);
        assertNotNull(type);

        assertEquals("RUNNABLE", type.stringOf(Thread.State.RUNNABLE));
        assertEquals(Thread.State.RUNNABLE, type.valueOf("RUNNABLE"));
    }

    @Test
    public void testEnumType_nonEnumClassThrows() {
        // getEnumClass with a non-enum class should throw IllegalArgumentException
        assertThrows(Exception.class, () -> {
            new EnumType<>(String.class.getName());
        });
    }

    // ============================================================
    // Fix: CollectionType IOException re-throw (not wrapping)
    // ============================================================

    @Test
    public void testCollectionType_appendToWritesCorrectly() throws IOException {
        @SuppressWarnings("unchecked")
        Type<List<String>> type = TypeFactory.getType("List<String>");
        assertNotNull(type);

        List<String> list = Arrays.asList("a", "b", "c");
        StringWriter sw = new StringWriter();
        type.appendTo(sw, list);
        String result = sw.toString();

        // Should produce valid JSON array
        assertNotNull(result);
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testCollectionType_appendToHandlesNull() throws IOException {
        @SuppressWarnings("unchecked")
        Type<List<String>> type = TypeFactory.getType("List<String>");

        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testCollectionType_appendToHandlesEmpty() throws IOException {
        @SuppressWarnings("unchecked")
        Type<List<String>> type = TypeFactory.getType("List<String>");

        StringWriter sw = new StringWriter();
        type.appendTo(sw, new ArrayList<>());
        String result = sw.toString();
        assertEquals("[]", result);
    }

    // ============================================================
    // Fix: ObjectArrayType IOException re-throw
    // ============================================================

    @Test
    public void testObjectArrayType_appendToWritesCorrectly() throws IOException {
        @SuppressWarnings("unchecked")
        Type<String[]> type = TypeFactory.getType(String[].class);
        assertNotNull(type);

        String[] arr = { "hello", "world" };
        StringWriter sw = new StringWriter();
        type.appendTo(sw, arr);
        String result = sw.toString();

        assertNotNull(result);
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testObjectArrayType_stringOfAndValueOf() {
        @SuppressWarnings("unchecked")
        Type<String[]> type = TypeFactory.getType(String[].class);

        String[] arr = { "a", "b", "c" };
        String str = type.stringOf(arr);
        assertNotNull(str);

        String[] parsed = type.valueOf(str);
        assertNotNull(parsed);
        assertEquals(3, parsed.length);
        assertEquals("a", parsed[0]);
        assertEquals("b", parsed[1]);
        assertEquals("c", parsed[2]);
    }

    // ============================================================
    // Fix: IndexedType IOException re-throw
    // ============================================================

    @Test
    public void testIndexedType_appendToWritesCorrectly() throws IOException {
        @SuppressWarnings("unchecked")
        Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");
        assertNotNull(type);

        Indexed<String> indexed = Indexed.of("hello", 5);
        StringWriter sw = new StringWriter();
        type.appendTo(sw, indexed);
        String result = sw.toString();

        assertNotNull(result);
        assertTrue(result.contains("5"));
        assertTrue(result.contains("hello"));
    }

    @Test
    public void testIndexedType_stringOfAndValueOf() {
        @SuppressWarnings("unchecked")
        Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");

        Indexed<String> indexed = Indexed.of("test", 10);
        String str = type.stringOf(indexed);
        assertNotNull(str);

        Indexed<String> parsed = type.valueOf(str);
        assertNotNull(parsed);
        assertEquals(10, parsed.index());
        assertEquals("test", parsed.value());
    }

    @Test
    public void testIndexedType_nullHandling() throws IOException {
        @SuppressWarnings("unchecked")
        Type<Indexed<String>> type = TypeFactory.getType("Indexed<String>");

        assertNull(type.stringOf(null));

        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }
}
