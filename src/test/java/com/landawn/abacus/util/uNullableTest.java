package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

public class uNullableTest extends uTestSupport {
    @Test
    public void testNullable_orElseThrowIfNull_NonNull_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("hello");
        assertEquals("hello", nonNull.orElseThrowIfNull("error"));
    }

    @Test
    public void testNullableVsOptionalDifferences() {
        Nullable<String> nullableNull = Nullable.of((String) null);
        assertTrue(nullableNull.isPresent());
        assertNull(nullableNull.get());
        assertTrue(nullableNull.isNull());

        assertThrows(NullPointerException.class, () -> Optional.of((String) null));

        Nullable<String> nullableEmpty = Nullable.empty();
        Optional<String> optionalEmpty = Optional.empty();
        assertFalse(nullableEmpty.isPresent());
        assertFalse(optionalEmpty.isPresent());

        assertTrue(nullableEmpty.isNull());
        assertTrue(nullableNull.isNull());
    }

    // ===================== Nullable Additional Tests =====================

    @Test
    public void testNullable_ofString() {
        Nullable<String> opt = Nullable.of("test");
        assertTrue(opt.isPresent());
        assertEquals("test", opt.get());

        Nullable<String> empty = Nullable.of("");
        assertTrue(empty.isPresent());
        assertEquals("", empty.get());
    }

    @Test
    public void testNullable_or() {
        Nullable<String> present = Nullable.of("first");
        Nullable<String> result = present.or(() -> Nullable.of("second"));
        assertTrue(result.isPresent());
        assertEquals("first", result.get());

        Nullable<String> empty = Nullable.empty();
        Nullable<String> result2 = empty.or(() -> Nullable.of("second"));
        assertTrue(result2.isPresent());
        assertEquals("second", result2.get());

        // Note: Nullable.of(null) is present, so or() returns it
        Nullable<String> nullValue = Nullable.of((String) null);
        Nullable<String> result3 = nullValue.or(() -> Nullable.of("fallback"));
        assertTrue(result3.isPresent());
        assertTrue(result3.isNull());
    }

    @Test
    public void testNullable_contains() {
        assertTrue(Nullable.of("A").contains("A"));
        assertFalse(Nullable.of("A").contains("B"));
        assertFalse(Nullable.empty().contains("A"));

        // null value contains null
        assertTrue(Nullable.of((String) null).contains(null));
        assertFalse(Nullable.of("A").contains(null));
    }

    @Test
    public void testNullable_flatMap() throws Exception {
        Nullable<String> opt = Nullable.of("test");
        Nullable<Integer> mapped = opt.flatMap(s -> Nullable.of(s.length()));
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        Nullable<String> empty = Nullable.empty();
        Nullable<Integer> emptyMapped = empty.flatMap(s -> Nullable.of(10));
        assertFalse(emptyMapped.isPresent());

        // present with null value - flatMap still applies
        Nullable<String> nullVal = Nullable.of((String) null);
        Nullable<Integer> nullMapped = nullVal.flatMap(s -> Nullable.of(99));
        assertTrue(nullMapped.isPresent());
        assertEquals(99, nullMapped.get());
    }

    @Test
    public void testNullable_stream() {
        assertEquals(1, Nullable.of("test").stream().count());
        assertEquals(1, Nullable.of((String) null).stream().count());
        assertEquals(0, Nullable.empty().stream().count());
    }

    @Test
    public void testNullable_toList() {
        List<String> list = Nullable.of("test").toList();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        List<String> nullList = Nullable.of((String) null).toList();
        assertEquals(1, nullList.size());
        assertNull(nullList.get(0));

        List<String> emptyList = Nullable.<String> empty().toList();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testNullable_toSet() {
        Set<String> set = Nullable.of("test").toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        Set<String> emptySet = Nullable.<String> empty().toSet();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullable_toImmutableList() {
        ImmutableList<String> list = Nullable.of("test").toImmutableList();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        ImmutableList<String> emptyList = Nullable.<String> empty().toImmutableList();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testNullable_toImmutableSet() {
        ImmutableSet<String> set = Nullable.of("test").toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        ImmutableSet<String> emptySet = Nullable.<String> empty().toImmutableSet();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullable_toJdkOptional() {
        java.util.Optional<String> jdkOpt = Nullable.of("test").toJdkOptional();
        assertTrue(jdkOpt.isPresent());
        assertEquals("test", jdkOpt.get());

        java.util.Optional<String> nullJdkOpt = Nullable.of((String) null).toJdkOptional();
        assertFalse(nullJdkOpt.isPresent());

        java.util.Optional<String> emptyJdkOpt = Nullable.<String> empty().toJdkOptional();
        assertFalse(emptyJdkOpt.isPresent());
    }

    @Test
    public void testNullable_ifPresent() throws Exception {
        StringBuilder sb = new StringBuilder();
        Nullable.of("test").ifPresent(s -> sb.append(s));
        assertEquals("test", sb.toString());

        sb.setLength(0);
        Nullable.of((String) null).ifPresent(s -> sb.append("called"));
        assertEquals("called", sb.toString());

        sb.setLength(0);
        Nullable.empty().ifPresent(s -> sb.append("should_not"));
        assertEquals("", sb.toString());
    }

    @Test
    public void testNullable_ifPresentOrElse() throws Exception {
        StringBuilder sb = new StringBuilder();
        Nullable.of("test").ifPresentOrElse(s -> sb.append(s), () -> sb.append("empty"));
        assertEquals("test", sb.toString());

        sb.setLength(0);
        Nullable.<String> empty().ifPresentOrElse(s -> sb.append(s), () -> sb.append("empty"));
        assertEquals("empty", sb.toString());
    }

    @Test
    public void testNullable_filter() throws Exception {
        Nullable<Integer> opt = Nullable.of(10);
        assertTrue(opt.filter(v -> v > 5).isPresent());
        assertFalse(opt.filter(v -> v > 20).isPresent());
        assertFalse(Nullable.<Integer> empty().filter(v -> true).isPresent());
    }

    @Test
    public void testNullable_mapToNonNull() throws Exception {
        Nullable<String> opt = Nullable.of("test");
        Optional<Integer> mapped = opt.mapToNonNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        assertFalse(Nullable.<String> empty().mapToNonNull(String::length).isPresent());
    }

    @Test
    public void testNullable_mapToBoolean() throws Exception {
        Nullable<String> opt = Nullable.of("test");
        OptionalBoolean mapped = opt.mapToBoolean(s -> s.length() > 3);
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        assertFalse(Nullable.<String> empty().mapToBoolean(s -> true).isPresent());
    }

    @Test
    public void testNullable_orElse() {
        assertEquals("test", Nullable.of("test").orElse("default"));
        assertNull(Nullable.of((String) null).orElse("default"));
        assertEquals("default", Nullable.<String> empty().orElse("default"));
    }

    @Test
    public void testNullable_orElseGet() {
        assertEquals("test", Nullable.of("test").orElseGet(() -> "default"));
        assertNull(Nullable.of((String) null).orElseGet(() -> "default"));
        assertEquals("default", Nullable.<String> empty().orElseGet(() -> "default"));
    }

    @Test
    public void testNullable_orElseThrow() {
        assertEquals("test", Nullable.of("test").orElseThrow());
        assertNull(Nullable.of((String) null).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow());
    }

    @Test
    public void testNullable_mapToChar() {
        com.landawn.abacus.util.u.OptionalChar result = Nullable.of("hello").mapToChar(s -> s.charAt(0));
        assertTrue(result.isPresent());
        assertEquals('h', result.get());
        assertFalse(Nullable.<String> empty().mapToChar(s -> s.charAt(0)).isPresent());
    }

    @Test
    public void testNullable_mapToByte() {
        OptionalByte result = Nullable.of("5").mapToByte(s -> Byte.parseByte(s));
        assertTrue(result.isPresent());
        assertEquals((byte) 5, result.get());
        assertFalse(Nullable.<String> empty().mapToByte(s -> (byte) 0).isPresent());
    }

    @Test
    public void testNullable_mapToShort() {
        com.landawn.abacus.util.u.OptionalShort result = Nullable.of("10").mapToShort(s -> Short.parseShort(s));
        assertTrue(result.isPresent());
        assertEquals((short) 10, result.get());
        assertFalse(Nullable.<String> empty().mapToShort(s -> (short) 0).isPresent());
    }

    @Test
    public void testNullable_mapToInt() {
        OptionalInt result = Nullable.of("42").mapToInt(Integer::parseInt);
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
        assertFalse(Nullable.<String> empty().mapToInt(Integer::parseInt).isPresent());
    }

    @Test
    public void testNullable_mapToLong() {
        OptionalLong result = Nullable.of("100").mapToLong(Long::parseLong);
        assertTrue(result.isPresent());
        assertEquals(100L, result.get());
        assertFalse(Nullable.<String> empty().mapToLong(Long::parseLong).isPresent());
    }

    @Test
    public void testNullable_mapToFloat() {
        OptionalFloat result = Nullable.of("1.5").mapToFloat(Float::parseFloat);
        assertTrue(result.isPresent());
        assertEquals(1.5f, result.get(), 0.001f);
        assertFalse(Nullable.<String> empty().mapToFloat(Float::parseFloat).isPresent());
    }

    @Test
    public void testNullable_mapToDouble() {
        OptionalDouble result = Nullable.of("3.14").mapToDouble(Double::parseDouble);
        assertTrue(result.isPresent());
        assertEquals(3.14, result.get(), 0.001);
        assertFalse(Nullable.<String> empty().mapToDouble(Double::parseDouble).isPresent());
    }

    @Test
    public void testNullable_orElseThrow_WithMessage() {
        assertEquals("hello", Nullable.of("hello").orElseThrow("No value present"));
        assertThrows(NoSuchElementException.class, () -> Nullable.<String> empty().orElseThrow("Custom error"));
    }

    @Test
    public void testNullable_orElseThrow_WithMessageAndArg() {
        assertEquals("hello", Nullable.of("hello").orElseThrow("Error: %s", "arg1"));
        assertThrows(NoSuchElementException.class, () -> Nullable.<String> empty().orElseThrow("Error: %s", "arg1"));
    }

    @Test
    public void testNullable_orElseThrow_WithMessageAndTwoArgs() {
        assertEquals("hello", Nullable.of("hello").orElseThrow("Error: %s %s", "a", "b"));
        assertThrows(NoSuchElementException.class, () -> Nullable.<String> empty().orElseThrow("Error: %s %s", "a", "b"));
    }

    @Test
    public void testNullable_orElseThrow_WithMessageAndThreeArgs() {
        assertEquals("hello", Nullable.of("hello").orElseThrow("Error: %s %s %s", "a", "b", "c"));
        assertThrows(NoSuchElementException.class, () -> Nullable.<String> empty().orElseThrow("Error: %s %s %s", "a", "b", "c"));
    }

    @Test
    public void testNullable_orElseThrow_WithVarargs() {
        assertEquals("hello", Nullable.of("hello").orElseThrow("Error: %s", new Object[] { "arg" }));
        assertThrows(NoSuchElementException.class, () -> Nullable.<String> empty().orElseThrow("Error", new Object[0]));
    }

    @Test
    public void testNullable_orElseThrowIfNull_NonNull_WithParam_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("world");
        assertEquals("world", nonNull.orElseThrowIfNull("Error: %s", "param"));
    }

    @Test
    public void testNullable_orElseThrowIfNull_NonNull_WithTwoParams_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("value");
        assertEquals("value", nonNull.orElseThrowIfNull("Error: %s %s", "p1", "p2"));
    }

    @Test
    public void testNullable_orElseThrowIfNull_NonNull_WithThreeParams_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("result");
        assertEquals("result", nonNull.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
    }

    @Test
    public void testNullable_orElseThrowIfNull_NonNull_WithVarargs_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("data");
        assertEquals("data", nonNull.orElseThrowIfNull("Error", new Object[] { "arg1", "arg2" }));
    }

    @Test
    public void testNullable_equals_SameInstance() {
        u.Nullable<String> n = u.Nullable.of("test");
        assertTrue(n.equals(n));
    }

    @Test
    public void testNullable_equals_Null() {
        u.Nullable<String> n = u.Nullable.of("test");
        assertFalse(n.equals(null));
    }

    @Test
    public void testNullable_equals_DifferentType() {
        u.Nullable<String> n = u.Nullable.of("test");
        assertFalse(n.equals("test"));
    }

    @Test
    public void testNullable_from_Optional_Null() {
        // B3 (2026-09-01): every from(..) treats a null argument as absence.
        final u.Optional<String> nullOptional = null;
        final u.Nullable<String> result = u.Nullable.from(nullOptional);
        assertFalse(result.isPresent());
    }

    @Test
    public void testNullable_from_Optional_NonNull() {
        u.Optional<String> opt = u.Optional.of("value");
        u.Nullable<String> result = u.Nullable.from(opt);
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    @DisplayName("Empty Nullable distinct from Nullable.of(null)")
    public void testNullable_EmptyVsPresentNull() {
        Nullable<String> empty = Nullable.empty();
        Nullable<String> presentNull = Nullable.of((String) null);
        assertFalse(empty.isPresent());
        assertTrue(presentNull.isPresent());
        assertNotEquals(empty, presentNull);
    }
}
