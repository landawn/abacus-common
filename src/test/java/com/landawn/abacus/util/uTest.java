package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class uTest extends TestBase {

    @Nested
    public class OptionalTest {
    }

    @Nested
    public class OptionalIntTest {
    }

    @Nested
    public class NullableTest {
    }

    // --- Additional coverage tests for u.Nullable ---

    @Test
    public void testNullable_orElseThrowIfNull_NonNull_ReturnsValue() {
        u.Nullable<String> nonNull = u.Nullable.of("hello");
        assertEquals("hello", nonNull.orElseThrowIfNull("error"));
    }

    @Test
    public void testCreation() {
        u.Optional<String> empty = u.Optional.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());

        u.Optional<String> present = u.Optional.of("test");
        assertTrue(present.isPresent());
        assertEquals("test", present.get());
        assertThrows(NullPointerException.class, () -> u.Optional.of(null));

        u.Optional<String> fromNullable = u.Optional.ofNullable("test");
        assertTrue(fromNullable.isPresent());
        u.Optional<String> fromNull = u.Optional.ofNullable(null);
        assertFalse(fromNull.isPresent());

        u.Optional<String> fromJdk = u.Optional.from(java.util.Optional.of("jdk"));
        assertTrue(fromJdk.isPresent());
        assertEquals("jdk", fromJdk.get());
        u.Optional<Object> fromEmptyJdk = u.Optional.from(java.util.Optional.empty());
        assertFalse(fromEmptyJdk.isPresent());
    }

    @Test
    public void testGetAndOrElse() {
        u.Optional<String> present = u.Optional.of("val");
        u.Optional<String> empty = u.Optional.empty();

        assertEquals("val", present.get());
        assertThrows(NoSuchElementException.class, empty::get);

        assertEquals("val", present.orElse("other"));
        assertEquals("other", empty.orElse("other"));

        assertEquals("val", present.orElseGet(() -> "other"));
        assertEquals("other", empty.orElseGet(() -> "other"));

        assertEquals("val", present.orElseNull());
        assertNull(empty.orElseNull());
    }

    @Test
    public void testOrElseThrow() {
        u.Optional<String> present = u.Optional.of("val");
        u.Optional<String> empty = u.Optional.empty();

        assertEquals("val", present.orElseThrow());
        assertThrows(NoSuchElementException.class, empty::orElseThrow);

        assertEquals("val", present.orElseThrow(RuntimeException::new));
        assertThrows(RuntimeException.class, () -> empty.orElseThrow(RuntimeException::new));

        Exception e = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("error msg %s", "param"));
        assertEquals("error msg param", e.getMessage());
    }

    @Test
    public void testFilterMapAndFlatMap() throws Exception {
        u.Optional<String> present = u.Optional.of("value");
        u.Optional<String> empty = u.Optional.empty();

        assertTrue(present.filter(s -> s.equals("value")).isPresent());
        assertFalse(present.filter(s -> s.equals("wrong")).isPresent());
        assertFalse(empty.filter(s -> true).isPresent());

        u.Optional<Integer> mapped = present.map(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(5, mapped.get());
        assertFalse(empty.map(String::length).isPresent());

        u.Optional<Integer> flatMapped = present.flatMap(s -> u.Optional.of(s.length()));
        assertTrue(flatMapped.isPresent());
        assertEquals(5, flatMapped.get());
        assertFalse(empty.flatMap(s -> u.Optional.of(s.length())).isPresent());
    }

    @Test
    public void testIfPresent() {
        AtomicBoolean presentCalled = new AtomicBoolean(false);
        u.Optional.of("val").ifPresent(v -> presentCalled.set(true));
        assertTrue(presentCalled.get());

        presentCalled.set(false);
        u.Optional.empty().ifPresent(v -> presentCalled.set(true));
        assertFalse(presentCalled.get());
    }

    @Test
    public void testIfPresentOrElse() {
        AtomicBoolean presentCalled = new AtomicBoolean(false);
        AtomicBoolean elseCalled = new AtomicBoolean(false);

        u.Optional.of("val").ifPresentOrElse(v -> presentCalled.set(true), () -> elseCalled.set(true));
        assertTrue(presentCalled.get());
        assertFalse(elseCalled.get());

        presentCalled.set(false);
        elseCalled.set(false);
        u.Optional.empty().ifPresentOrElse(v -> presentCalled.set(true), () -> elseCalled.set(true));
        assertFalse(presentCalled.get());
        assertTrue(elseCalled.get());
    }

    @Test
    public void testContains() {
        assertTrue(u.Optional.of("A").contains("A"));
        assertFalse(u.Optional.of("A").contains("B"));
        assertFalse(u.Optional.empty().contains("A"));
    }

    @Test
    public void testConversion() {
        u.Optional<String> present = u.Optional.of("v");
        u.Optional<String> empty = u.Optional.empty();

        assertEquals(1, present.stream().count());
        assertEquals(0, empty.stream().count());

        assertEquals(List.of("v"), present.toList());
        assertTrue(empty.toList().isEmpty());

        assertEquals(Set.of("v"), present.toSet());
        assertTrue(empty.toSet().isEmpty());

        assertTrue(present.toJdkOptional().isPresent());
        assertFalse(empty.toJdkOptional().isPresent());

    }

    @Test
    public void testEqualsAndHashCodeAndToString() {
        u.Optional<String> p1 = u.Optional.of("A");
        u.Optional<String> p2 = u.Optional.of("A");
        u.Optional<String> p3 = u.Optional.of("B");
        u.Optional<String> e1 = u.Optional.empty();
        u.Optional<String> e2 = u.Optional.empty();

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(e1, e2);
        assertNotEquals(p1, e1);

        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(e1.hashCode(), e2.hashCode());

        assertEquals("Optional[A]", p1.toString());
        assertEquals("Optional.empty", e1.toString());
    }

    @Test
    public void testMap() throws Exception {
        u.OptionalInt present = u.OptionalInt.of(10);

        assertEquals(20, present.map(v -> v * 2).get());
        assertEquals(20L, present.mapToLong(v -> (long) v * 2).get());
        assertEquals("10", present.mapToObj(String::valueOf).get());
        assertFalse(u.OptionalInt.empty().map(v -> v * 2).isPresent());
    }

    @Test
    public void testFilter() throws Exception {
        u.OptionalInt present = u.OptionalInt.of(10);
        assertTrue(present.filter(v -> v > 5).isPresent());
        assertFalse(present.filter(v -> v < 5).isPresent());
        assertFalse(u.OptionalInt.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testEquals() {
        assertEquals(u.OptionalInt.of(1), u.OptionalInt.of(1));
        assertNotEquals(u.OptionalInt.of(1), u.OptionalInt.of(2));
        assertEquals(u.OptionalInt.empty(), u.OptionalInt.empty());
        assertNotEquals(u.OptionalInt.of(1), u.OptionalInt.empty());
    }

    @Test
    public void testStateChecks() {
        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<String> present = u.Nullable.of("test");
        u.Nullable<String> presentNull = u.Nullable.of(null);

        assertFalse(empty.isPresent());
        assertTrue(present.isPresent());
        assertTrue(presentNull.isPresent());

        assertTrue(empty.isNull());
        assertFalse(present.isNull());
        assertTrue(presentNull.isNull());

        assertFalse(empty.isNotNull());
        assertTrue(present.isNotNull());
        assertFalse(presentNull.isNotNull());
    }

    @Test
    public void testMapAndFilter() throws Exception {
        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<String> present = u.Nullable.of("val");
        u.Nullable<String> presentNull = u.Nullable.of(null);

        assertEquals("VAL", present.map(String::toUpperCase).get());
        assertEquals("not called", presentNull.map(v -> "not called").get());
        assertFalse(empty.map(v -> "not called").isPresent());

        assertEquals("VAL", present.mapIfNotNull(String::toUpperCase).get());
        assertFalse(presentNull.mapIfNotNull(v -> "not called").isPresent());
        assertFalse(empty.mapIfNotNull(v -> "not called").isPresent());
    }

    @Test
    public void testIfNotNull() {
        AtomicBoolean notNullCalled = new AtomicBoolean(false);
        u.Nullable.of("val").ifNotNull(v -> notNullCalled.set(true));
        assertTrue(notNullCalled.get());

        notNullCalled.set(false);
        u.Nullable.of(null).ifNotNull(v -> notNullCalled.set(true));
        assertFalse(notNullCalled.get());

        notNullCalled.set(false);
        u.Nullable.empty().ifNotNull(v -> notNullCalled.set(true));
        assertFalse(notNullCalled.get());
    }

    @Test
    public void testEqualsAndHashCode() {
        u.Nullable<String> p1 = u.Nullable.of("A");
        u.Nullable<String> p2 = u.Nullable.of("A");
        u.Nullable<String> n1 = u.Nullable.of(null);
        u.Nullable<String> n2 = u.Nullable.of(null);
        u.Nullable<String> e1 = u.Nullable.empty();
        u.Nullable<String> e2 = u.Nullable.empty();

        assertEquals(p1, p2);
        assertEquals(n1, n2);
        assertEquals(e1, e2);

        assertNotEquals(p1, n1);
        assertNotEquals(p1, e1);
        assertNotEquals(n1, e1);

        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals(n1.hashCode(), n2.hashCode());
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("Nullable[val]", u.Nullable.of("val").toString());
        assertEquals("Nullable[null]", u.Nullable.of(null).toString());
        assertEquals("Nullable.empty", u.Nullable.empty().toString());
    }

    @Test
    public void testEmpty() {
        u.OptionalBoolean optional = u.OptionalBoolean.empty();
        assertFalse(optional.isPresent());
        assertTrue(optional.isEmpty());
    }

    @Test
    public void testOf() {
        u.OptionalBoolean optionalTrue = u.OptionalBoolean.of(true);
        assertTrue(optionalTrue.isPresent());
        assertTrue(optionalTrue.get());

        u.OptionalBoolean optionalFalse = u.OptionalBoolean.of(false);
        assertTrue(optionalFalse.isPresent());
        assertFalse(optionalFalse.get());
    }

    @Test
    public void testOfNullable() {
        u.OptionalBoolean optional = u.OptionalBoolean.ofNullable(true);
        assertTrue(optional.isPresent());
        assertTrue(optional.get());

        optional = u.OptionalBoolean.ofNullable(false);
        assertTrue(optional.isPresent());
        assertFalse(optional.get());

        optional = u.OptionalBoolean.ofNullable(null);
        assertFalse(optional.isPresent());
    }

    @Test
    public void testGet() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.get());
    }

    @Test
    public void testIsPresent() {
        assertTrue(u.OptionalBoolean.of(true).isPresent());
        assertTrue(u.OptionalBoolean.of(false).isPresent());
        assertFalse(u.OptionalBoolean.empty().isPresent());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(u.OptionalBoolean.of(true).isEmpty());
        assertFalse(u.OptionalBoolean.of(false).isEmpty());
        assertTrue(u.OptionalBoolean.empty().isEmpty());
    }

    @Test
    public void testMapToChar() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalChar mapped = optional.mapToChar(value -> value ? 'T' : 'F');
        assertTrue(mapped.isPresent());
        assertEquals('T', mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToChar(value -> 'X');
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToInt() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalInt mapped = optional.mapToInt(value -> value ? 1 : 0);
        assertTrue(mapped.isPresent());
        assertEquals(1, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToInt(value -> 99);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToLong() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalLong mapped = optional.mapToLong(value -> value ? 1L : 0L);
        assertTrue(mapped.isPresent());
        assertEquals(1L, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToLong(value -> 99L);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToDouble() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalDouble mapped = optional.mapToDouble(value -> value ? 1.0 : 0.0);
        assertTrue(mapped.isPresent());
        assertEquals(1.0, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToDouble(value -> 99.0);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToObj() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        Optional<String> mapped = optional.mapToObj(value -> value ? "TRUE" : "FALSE");
        assertTrue(mapped.isPresent());
        assertEquals("TRUE", mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToObj(value -> "X");
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testFlatMap() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalBoolean mapped = optional.flatMap(value -> u.OptionalBoolean.of(!value));
        assertTrue(mapped.isPresent());
        assertFalse(mapped.get());

        mapped = optional.flatMap(value -> u.OptionalBoolean.empty());
        assertFalse(mapped.isPresent());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.flatMap(value -> u.OptionalBoolean.of(true));
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testOr() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        u.OptionalBoolean result = optional.or(() -> u.OptionalBoolean.of(false));
        assertTrue(result.isPresent());
        assertTrue(result.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        result = empty.or(() -> u.OptionalBoolean.of(false));
        assertTrue(result.isPresent());
        assertFalse(result.get());
    }

    @Test
    public void testOrElse() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElse(false));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertFalse(empty.orElse(false));
        assertTrue(empty.orElse(true));
    }

    @Test
    public void testOrElseGet() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseGet(() -> false));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertFalse(empty.orElseGet(() -> false));
        assertTrue(empty.orElseGet(() -> true));
    }

    @Test
    public void testOrElseThrowWithMessage() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
        assertEquals("Custom error", ex.getMessage());
    }

    @Test
    public void testOrElseThrowWithMessageAndParam() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s", "param"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
    }

    @Test
    public void testOrElseThrowWithMessageAndTwoParams() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s", "p1", "p2"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
    }

    @Test
    public void testOrElseThrowWithMessageAndThreeParams() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
    }

    @Test
    public void testOrElseThrowWithMessageAndParamsArray() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
    }

    @Test
    public void testOrElseThrowWithSupplier() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow(() -> new IllegalStateException()));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
    }

    @Test
    public void testStream() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        List<Boolean> list = optional.stream().toList();
        assertEquals(1, list.size());
        assertTrue(list.get(0));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        list = empty.stream().toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testToList() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        List<Boolean> list = optional.toList();
        assertEquals(1, list.size());
        assertTrue(list.get(0));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        list = empty.toList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testToSet() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        Set<Boolean> set = optional.toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(true));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        set = empty.toSet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testToImmutableList() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        ImmutableList<Boolean> list = optional.toImmutableList();
        assertEquals(1, list.size());
        assertTrue(list.get(0));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        list = empty.toImmutableList();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        ImmutableSet<Boolean> set = optional.toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(true));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        set = empty.toImmutableSet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testBoxed() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        Optional<Boolean> boxed = optional.boxed();
        assertTrue(boxed.isPresent());
        assertTrue(boxed.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        boxed = empty.boxed();
        assertFalse(boxed.isPresent());
    }

    @Test
    public void testCompareTo() {
        u.OptionalBoolean true1 = u.OptionalBoolean.of(true);
        u.OptionalBoolean true2 = u.OptionalBoolean.of(true);
        u.OptionalBoolean false1 = u.OptionalBoolean.of(false);
        u.OptionalBoolean empty = u.OptionalBoolean.empty();

        assertEquals(0, true1.compareTo(true2));
        assertTrue(true1.compareTo(false1) > 0);
        assertTrue(false1.compareTo(true1) < 0);
        assertTrue(true1.compareTo(empty) > 0);
        assertTrue(empty.compareTo(true1) < 0);
        assertEquals(0, empty.compareTo(u.OptionalBoolean.empty()));
        assertTrue(true1.compareTo(null) > 0);
    }

    @Test
    public void testHashCode() {
        u.OptionalBoolean true1 = u.OptionalBoolean.of(true);
        u.OptionalBoolean true2 = u.OptionalBoolean.of(true);
        u.OptionalBoolean false1 = u.OptionalBoolean.of(false);
        u.OptionalBoolean empty = u.OptionalBoolean.empty();

        assertEquals(true1.hashCode(), true2.hashCode());
        assertNotEquals(true1.hashCode(), false1.hashCode());
        assertNotEquals(true1.hashCode(), empty.hashCode());
    }

    @Test
    public void testConstants() {
        assertSame(u.OptionalBoolean.TRUE, u.OptionalBoolean.of(true));
        assertSame(u.OptionalBoolean.FALSE, u.OptionalBoolean.of(false));
        assertTrue(u.OptionalBoolean.TRUE.isPresent());
        assertTrue(u.OptionalBoolean.TRUE.get());
        assertTrue(u.OptionalBoolean.FALSE.isPresent());
        assertFalse(u.OptionalBoolean.FALSE.get());
    }

    @Test
    public void testMapToBoolean() throws Exception {
        u.OptionalChar optional = u.OptionalChar.of('A');

        u.OptionalBoolean mapped = optional.mapToBoolean(value -> value == 'A');
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        u.OptionalChar empty = u.OptionalChar.empty();
        mapped = empty.mapToBoolean(value -> true);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToNonNull() throws Exception {
        u.OptionalChar optional = u.OptionalChar.of('A');

        Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
        assertTrue(mapped.isPresent());
        assertEquals("A", mapped.get());

        u.OptionalChar empty = u.OptionalChar.empty();
        mapped = empty.mapToObj(value -> "X");
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testOrElseZero() {
        u.OptionalChar optional = u.OptionalChar.of('A');
        assertEquals('A', optional.orElseZero());

        u.OptionalChar empty = u.OptionalChar.empty();
        assertEquals((char) 0, empty.orElseZero());
    }

    @Test
    public void testOrElseThrowWithMessageAndParams() {
        u.OptionalChar optional = u.OptionalChar.of('A');
        assertEquals('A', optional.orElseThrow("Error %s", "param"));
        assertEquals('A', optional.orElseThrow("Error %s %s", "p1", "p2"));
        assertEquals('A', optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
        assertEquals('A', optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

        u.OptionalChar empty = u.OptionalChar.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
    }

    @Test
    public void testFrom() {
        java.util.OptionalInt jdkOptional = java.util.OptionalInt.of(42);
        u.OptionalInt optional = u.OptionalInt.from(jdkOptional);
        assertTrue(optional.isPresent());
        assertEquals(42, optional.get());

        jdkOptional = java.util.OptionalInt.empty();
        optional = u.OptionalInt.from(jdkOptional);
        assertFalse(optional.isPresent());
    }

    @Test
    public void testGetAsInt() {
        u.OptionalInt optional = u.OptionalInt.of(42);
        assertEquals(42, optional.getAsInt());

        u.OptionalInt empty = u.OptionalInt.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsInt());
    }

    @Test
    public void testMapToFloat() throws Exception {
        u.OptionalInt optional = u.OptionalInt.of(42);

        u.OptionalFloat mapped = optional.mapToFloat(value -> value / 2.0f);
        assertTrue(mapped.isPresent());
        assertEquals(21.0f, mapped.get());

        u.OptionalInt empty = u.OptionalInt.empty();
        mapped = empty.mapToFloat(value -> 99.0f);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testToJdkOptional() {
        u.OptionalInt optional = u.OptionalInt.of(42);
        java.util.OptionalInt jdkOptional = optional.toJdkOptional();
        assertTrue(jdkOptional.isPresent());
        assertEquals(42, jdkOptional.getAsInt());

        u.OptionalInt empty = u.OptionalInt.empty();
        jdkOptional = empty.toJdkOptional();
        assertFalse(jdkOptional.isPresent());
    }

    //    @Test
    //    public void testDeprecatedDunderscoreMethod() {
    //        u.OptionalInt optional = u.OptionalInt.of(42);
    //        java.util.OptionalInt jdkOptional = optional.__();
    //        assertTrue(jdkOptional.isPresent());
    //        assertEquals(42, jdkOptional.getAsInt());
    //
    //        u.OptionalInt empty = u.OptionalInt.empty();
    //        jdkOptional = empty.__();
    //        assertFalse(jdkOptional.isPresent());
    //    }

    @Test
    public void testGetAsLong() {
        u.OptionalLong optional = u.OptionalLong.of(42L);
        assertEquals(42L, optional.getAsLong());

        u.OptionalLong empty = u.OptionalLong.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsLong());
    }

    @Test
    public void testGetAsDouble() {
        u.OptionalDouble optional = u.OptionalDouble.of(42.5);
        assertEquals(42.5, optional.getAsDouble());

        u.OptionalDouble empty = u.OptionalDouble.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsDouble());
    }

    @Test
    @DisplayName("Test static constants")
    public void testStaticConstants() {
        assertTrue(u.Optional.TRUE.isPresent());
        assertEquals(Boolean.TRUE, u.Optional.TRUE.get());

        assertTrue(u.Optional.FALSE.isPresent());
        assertEquals(Boolean.FALSE, u.Optional.FALSE.get());
    }

    @Test
    @DisplayName("Test of(String)")
    public void testOfString() {
        u.Optional<String> opt = u.Optional.of("test");
        assertTrue(opt.isPresent());
        assertEquals("test", opt.get());

        u.Optional<String> emptyStr = u.Optional.of("");
        assertTrue(emptyStr.isPresent());
        assertEquals("", emptyStr.get());

        assertThrows(NullPointerException.class, () -> u.Optional.of((String) null));
    }

    @Test
    @DisplayName("Test of(T)")
    public void testOfGeneric() {
        u.Optional<Integer> intOpt = u.Optional.of(42);
        assertTrue(intOpt.isPresent());
        assertEquals(42, intOpt.get());

        List<String> list = Arrays.asList("a", "b");
        u.Optional<List<String>> listOpt = u.Optional.of(list);
        assertTrue(listOpt.isPresent());
        assertEquals(list, listOpt.get());

        assertThrows(NullPointerException.class, () -> u.Optional.of(null));
    }

    @Test
    @DisplayName("Test ofNullable(String)")
    public void testOfNullableString() {
        u.Optional<String> opt = u.Optional.ofNullable("test");
        assertTrue(opt.isPresent());
        assertEquals("test", opt.get());

        u.Optional<String> emptyStr = u.Optional.ofNullable("");
        assertTrue(emptyStr.isPresent());
        assertEquals("", emptyStr.get());

        u.Optional<String> nullOpt = u.Optional.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    @DisplayName("Test ofNullable(T)")
    public void testOfNullableGeneric() {
        u.Optional<Integer> opt = u.Optional.ofNullable(42);
        assertTrue(opt.isPresent());
        assertEquals(42, opt.get());

        u.Optional<Integer> nullOpt = u.Optional.ofNullable((Integer) null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    @DisplayName("Test get() and orElseThrow()")
    public void testGetAndOrElseThrow() {
        u.Optional<String> present = u.Optional.of("test");
        assertEquals("test", present.get());
        assertEquals("test", present.orElseThrow());

        u.Optional<String> empty = u.Optional.empty();
        assertThrows(NoSuchElementException.class, () -> empty.get());
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
    }

    @Test
    @DisplayName("Test isPresent() and isEmpty()")
    public void testIsPresentAndIsEmpty() {
        u.Optional<String> present = u.Optional.of("test");
        assertTrue(present.isPresent());
        assertFalse(present.isEmpty());

        u.Optional<String> empty = u.Optional.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("Test mapToByte()")
    public void testMapToByte() throws Exception {
        u.Optional<Integer> opt = u.Optional.of(100);
        OptionalByte mapped = opt.mapToByte(Integer::byteValue);
        assertTrue(mapped.isPresent());
        assertEquals((byte) 100, mapped.get());

        u.Optional<Integer> empty = u.Optional.empty();
        OptionalByte mappedEmpty = empty.mapToByte(i -> (byte) 0);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToByte(null));
    }

    @Test
    @DisplayName("Test mapToShort()")
    public void testMapToShort() throws Exception {
        u.Optional<Integer> opt = u.Optional.of(1000);
        OptionalShort mapped = opt.mapToShort(Integer::shortValue);
        assertTrue(mapped.isPresent());
        assertEquals((short) 1000, mapped.get());

        u.Optional<Integer> empty = u.Optional.empty();
        OptionalShort mappedEmpty = empty.mapToShort(i -> (short) 0);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToShort(null));
    }

    @Test
    @DisplayName("Test flatMapIfNotNull()")
    public void testFlatMapIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        u.Nullable<Integer> mapped = opt.flatMapIfNotNull(s -> u.Nullable.of(s.length()));
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        u.Nullable<Integer> mappedNull = nullOpt.flatMapIfNotNull(s -> u.Nullable.of(10));
        assertFalse(mappedNull.isPresent());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<Integer> emptyMapped = empty.flatMapIfNotNull(s -> u.Nullable.of(10));
        assertFalse(emptyMapped.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.flatMapIfNotNull(null));
    }

    @Test
    @DisplayName("Test orIfNull()")
    public void testOrIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("first");
        u.Nullable<String> result = nonNull.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result.isPresent());
        assertEquals("first", result.get());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        u.Nullable<String> result2 = nullValue.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result2.isPresent());
        assertEquals("second", result2.get());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<String> result3 = empty.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result3.isPresent());
        assertEquals("second", result3.get());

        assertThrows(IllegalArgumentException.class, () -> nullValue.orIfNull(null));
    }

    @Test
    @DisplayName("Test orElseNull()")
    public void testOrElseNull() {
        u.Nullable<String> present = u.Nullable.of("test");
        assertEquals("test", present.orElseNull());

        u.Nullable<String> nullPresent = u.Nullable.of((String) null);
        assertNull(nullPresent.orElseNull());

        u.Nullable<String> empty = u.Nullable.empty();
        assertNull(empty.orElseNull());
    }

    @Test
    @DisplayName("Test orElseIfNull()")
    public void testOrDefaultIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseIfNull("default"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertEquals("default", nullValue.orElseIfNull("default"));

        u.Nullable<String> empty = u.Nullable.empty();
        assertEquals("default", empty.orElseIfNull("default"));
    }

    @Test
    @DisplayName("Test orElseGetIfNull()")
    public void testOrElseGetIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseGetIfNull(() -> "default"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertEquals("default", nullValue.orElseGetIfNull(() -> "default"));

        u.Nullable<String> empty = u.Nullable.empty();
        assertEquals("default", empty.orElseGetIfNull(() -> "default"));

        assertThrows(IllegalArgumentException.class, () -> nullValue.orElseGetIfNull(null));
    }

    @Test
    @DisplayName("Test orElseThrow(Supplier)")
    public void testOrElseThrowSupplier() {
        u.Nullable<String> present = u.Nullable.of("test");
        assertEquals("test", present.orElseThrow(() -> new RuntimeException()));

        u.Nullable<String> empty = u.Nullable.empty();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> empty.orElseThrow(() -> new RuntimeException("Custom")));
        assertEquals("Custom", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> empty.orElseThrow((Supplier<RuntimeException>) null));
    }

    @Test
    @DisplayName("Test orElseThrowIfNull()")
    public void testOrElseThrowIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseThrowIfNull());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull());

        u.Nullable<String> empty = u.Nullable.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull());
    }

    @Test
    @DisplayName("Test orElseThrowIfNull() with message variations")
    public void testOrElseThrowIfNullWithMessage() {
        u.Nullable<String> nullValue = u.Nullable.of((String) null);

        NoSuchElementException ex1 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Custom error"));
        assertEquals("Custom error", ex1.getMessage());

        NoSuchElementException ex2 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s", "param1"));
        assertTrue(ex2.getMessage().contains("param1"));

        NoSuchElementException ex3 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s", "param1", "param2"));
        assertTrue(ex3.getMessage().contains("param1"));
        assertTrue(ex3.getMessage().contains("param2"));

        NoSuchElementException ex4 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
        assertTrue(ex4.getMessage().contains("p1"));
        assertTrue(ex4.getMessage().contains("p2"));
        assertTrue(ex4.getMessage().contains("p3"));

        NoSuchElementException ex5 = assertThrows(NoSuchElementException.class,
                () -> nullValue.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
        assertTrue(ex5.getMessage().contains("p1"));
        assertTrue(ex5.getMessage().contains("p4"));
    }

    @Test
    @DisplayName("Test orElseThrowIfNull(Supplier)")
    public void testOrElseThrowIfNullSupplier() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseThrowIfNull(() -> new RuntimeException()));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> nullValue.orElseThrowIfNull(() -> new RuntimeException("Custom")));
        assertEquals("Custom", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> nullValue.orElseThrowIfNull((Supplier<RuntimeException>) null));
    }

    @Test
    @DisplayName("Test streamIfNotNull()")
    public void testStreamIfNotNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        List<String> list = nonNull.streamIfNotNull().toList();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        List<String> nullList = nullValue.streamIfNotNull().toList();
        assertTrue(nullList.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        List<String> emptyList = empty.streamIfNotNull().toList();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("Test toListIfNotNull()")
    public void testToListIfNotNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        List<String> list = nonNull.toListIfNotNull();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        List<String> nullList = nullValue.toListIfNotNull();
        assertTrue(nullList.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        List<String> emptyList = empty.toListIfNotNull();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("Test toSetIfNotNull()")
    public void testToSetIfNotNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        Set<String> set = nonNull.toSetIfNotNull();
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        Set<String> nullSet = nullValue.toSetIfNotNull();
        assertTrue(nullSet.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        Set<String> emptySet = empty.toSetIfNotNull();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    @DisplayName("Test toImmutableListIfNotNull()")
    public void testToImmutableListIfNotNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        ImmutableList<String> list = nonNull.toImmutableListIfNotNull();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        ImmutableList<String> nullList = nullValue.toImmutableListIfNotNull();
        assertTrue(nullList.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        ImmutableList<String> emptyList = empty.toImmutableListIfNotNull();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("Test toImmutableSetIfNotNull()")
    public void testToImmutableSetIfNotNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        ImmutableSet<String> set = nonNull.toImmutableSetIfNotNull();
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        ImmutableSet<String> nullSet = nullValue.toImmutableSetIfNotNull();
        assertTrue(nullSet.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        ImmutableSet<String> emptySet = empty.toImmutableSetIfNotNull();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    @DisplayName("Test toOptional()")
    public void testToOptional() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        u.Optional<String> opt = nonNull.toOptional();
        assertTrue(opt.isPresent());
        assertEquals("test", opt.get());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        u.Optional<String> nullOpt = nullValue.toOptional();
        assertFalse(nullOpt.isPresent());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Optional<String> emptyOpt = empty.toOptional();
        assertFalse(emptyOpt.isPresent());
    }

    //    @Test
    //    @DisplayName("Test __()")
    //    public void testDoubleUnderscore() {
    //        u.Optional<String> present = u.Optional.of("test");
    //        java.util.Optional<String> jdkOpt = present.__();
    //        assertTrue(jdkOpt.isPresent());
    //        assertEquals("test", jdkOpt.get());
    //
    //        u.Optional<String> empty = u.Optional.empty();
    //        java.util.Optional<String> emptyJdkOpt = empty.__();
    //        assertFalse(emptyJdkOpt.isPresent());
    //    }

    @Test
    @DisplayName("Test from(Optional)")
    public void testFromOptional() {
        u.Optional<String> opt = u.Optional.of("test");
        u.Nullable<String> nullable = u.Nullable.from(opt);
        assertTrue(nullable.isPresent());
        assertEquals("test", nullable.get());

        u.Optional<String> empty = u.Optional.empty();
        u.Nullable<String> emptyNullable = u.Nullable.from(empty);
        assertFalse(emptyNullable.isPresent());
    }

    @Test
    @DisplayName("Test from(java.util.Optional)")
    public void testFromJdkOptional() {
        java.util.Optional<String> jdkOpt = java.util.Optional.of("test");
        u.Nullable<String> nullable = u.Nullable.from(jdkOpt);
        assertTrue(nullable.isPresent());
        assertEquals("test", nullable.get());

        java.util.Optional<String> emptyJdkOpt = java.util.Optional.empty();
        u.Nullable<String> emptyNullable = u.Nullable.from(emptyJdkOpt);
        assertFalse(emptyNullable.isPresent());
    }

    @Test
    @DisplayName("Test isPresent(), isNotPresent(), isEmpty()")
    public void testPresenceChecks() {
        u.Nullable<String> present = u.Nullable.of("test");
        assertTrue(present.isPresent());
        assertFalse(present.isNotPresent());
        assertFalse(present.isEmpty());

        u.Nullable<String> nullPresent = u.Nullable.of((String) null);
        assertTrue(nullPresent.isPresent());
        assertFalse(nullPresent.isNotPresent());
        assertFalse(nullPresent.isEmpty());

        u.Nullable<String> empty = u.Nullable.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isNotPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("Test isNull() and isNotNull()")
    public void testNullChecks() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertFalse(nonNull.isNull());
        assertTrue(nonNull.isNotNull());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertTrue(nullValue.isNull());
        assertFalse(nullValue.isNotNull());

        u.Nullable<String> empty = u.Nullable.empty();
        assertTrue(empty.isNull());
        assertFalse(empty.isNotNull());
    }

    @Test
    @DisplayName("Test ifNotNullOrElse()")
    public void testIfNotNullOrElse() throws Exception {
        StringBuilder sb = new StringBuilder();
        u.Nullable<String> nonNull = u.Nullable.of("test");
        u.Nullable<String> result = nonNull.ifNotNullOrElse(s -> sb.append("notNull:" + s), () -> sb.append("null"));
        assertEquals("notNull:test", sb.toString());
        assertSame(nonNull, result);

        StringBuilder sb2 = new StringBuilder();
        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        u.Nullable<String> result2 = nullValue.ifNotNullOrElse(s -> sb2.append("notNull:" + s), () -> sb2.append("null"));
        assertEquals("null", sb2.toString());
        assertSame(nullValue, result2);

        assertThrows(IllegalArgumentException.class, () -> nonNull.ifNotNullOrElse(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> nonNull.ifNotNullOrElse(s -> {
        }, null));
    }

    @Test
    @DisplayName("Test filterIfNotNull()")
    public void testFilterIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(10);
        u.Optional<Integer> filtered = opt.filterIfNotNull(i -> i > 5);
        assertTrue(filtered.isPresent());
        assertEquals(10, filtered.get());

        u.Optional<Integer> filtered2 = opt.filterIfNotNull(i -> i > 20);
        assertFalse(filtered2.isPresent());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        u.Optional<Integer> filtered3 = nullOpt.filterIfNotNull(i -> true);
        assertFalse(filtered3.isPresent());

        u.Nullable<Integer> empty = u.Nullable.empty();
        u.Optional<Integer> filtered4 = empty.filterIfNotNull(i -> true);
        assertFalse(filtered4.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.filterIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapIfNotNull()")
    public void testMapIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        u.Nullable<Integer> mapped = opt.mapIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        u.Nullable<Integer> mappedNull = nullOpt.mapIfNotNull(String::length);
        assertFalse(mappedNull.isPresent());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<Integer> mappedEmpty = empty.mapIfNotNull(String::length);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToNonNullIfNotNull()")
    public void testMapToNonNullIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        u.Optional<Integer> mapped = opt.mapToNonNullIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        u.Optional<Integer> mappedNull = nullOpt.mapToNonNullIfNotNull(String::length);
        assertFalse(mappedNull.isPresent());

        assertThrows(NullPointerException.class, () -> opt.mapToNonNullIfNotNull(s -> null));

        assertThrows(IllegalArgumentException.class, () -> opt.mapToNonNullIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToBooleanIfNotNull()")
    public void testMapToBooleanIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalBoolean mapped = opt.mapToBooleanIfNotNull(s -> s.length() > 3);
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalBoolean mappedNull = nullOpt.mapToBooleanIfNotNull(s -> true);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToBooleanIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToCharIfNotNull()")
    public void testMapToCharIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalChar mapped = opt.mapToCharIfNotNull(s -> s.charAt(0));
        assertTrue(mapped.isPresent());
        assertEquals('t', mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalChar mappedNull = nullOpt.mapToCharIfNotNull(s -> 'a');
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToCharIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToByteIfNotNull()")
    public void testMapToByteIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(100);
        OptionalByte mapped = opt.mapToByteIfNotNull(Integer::byteValue);
        assertTrue(mapped.isPresent());
        assertEquals((byte) 100, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalByte mappedNull = nullOpt.mapToByteIfNotNull(i -> (byte) 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToByteIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToShortIfNotNull()")
    public void testMapToShortIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(1000);
        OptionalShort mapped = opt.mapToShortIfNotNull(Integer::shortValue);
        assertTrue(mapped.isPresent());
        assertEquals((short) 1000, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalShort mappedNull = nullOpt.mapToShortIfNotNull(i -> (short) 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToShortIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToIntIfNotNull()")
    public void testMapToIntIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalInt mapped = opt.mapToIntIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.getAsInt());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalInt mappedNull = nullOpt.mapToIntIfNotNull(s -> 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToIntIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToLongIfNotNull()")
    public void testMapToLongIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalLong mapped = opt.mapToLongIfNotNull(s -> (long) s.length());
        assertTrue(mapped.isPresent());
        assertEquals(4L, mapped.getAsLong());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalLong mappedNull = nullOpt.mapToLongIfNotNull(s -> 0L);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToLongIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToFloatIfNotNull()")
    public void testMapToFloatIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(10);
        OptionalFloat mapped = opt.mapToFloatIfNotNull(i -> i / 2.0f);
        assertTrue(mapped.isPresent());
        assertEquals(5.0f, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalFloat mappedNull = nullOpt.mapToFloatIfNotNull(i -> 0.0f);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToFloatIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToDoubleIfNotNull()")
    public void testMapToDoubleIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(10);
        OptionalDouble mapped = opt.mapToDoubleIfNotNull(i -> i / 2.0);
        assertTrue(mapped.isPresent());
        assertEquals(5.0, mapped.getAsDouble());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalDouble mappedNull = nullOpt.mapToDoubleIfNotNull(i -> 0.0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToDoubleIfNotNull(null));
    }

    @Test
    public void testOptionalConstants() {
        assertTrue(Optional.TRUE.isPresent());
        assertTrue(Optional.TRUE.get());

        assertTrue(Optional.FALSE.isPresent());
        assertFalse(Optional.FALSE.get());
    }

    @Test
    public void testFromJavaOptional() {
        Optional<String> fromEmpty = Optional.from(java.util.Optional.empty());
        assertFalse(fromEmpty.isPresent());

        Optional<String> fromPresent = Optional.from(java.util.Optional.of("test"));
        assertTrue(fromPresent.isPresent());
        assertEquals("test", fromPresent.get());

        Optional<String> fromNull = Optional.from(null);
        assertFalse(fromNull.isPresent());
    }

    @Test
    public void testMapToPrimitives() throws Exception {
        Optional<String> opt = Optional.of("test");
        Optional<String> empty = Optional.empty();

        u.OptionalBoolean boolOpt = opt.mapToBoolean(s -> s.length() > 3);
        assertTrue(boolOpt.isPresent());
        assertTrue(boolOpt.get());
        assertFalse(empty.mapToBoolean(s -> true).isPresent());

        u.OptionalChar charOpt = opt.mapToChar(s -> s.charAt(0));
        assertTrue(charOpt.isPresent());
        assertEquals('t', charOpt.get());
        assertFalse(empty.mapToChar(s -> 'a').isPresent());

        u.OptionalByte byteOpt = opt.mapToByte(s -> (byte) s.length());
        assertTrue(byteOpt.isPresent());
        assertEquals((byte) 4, byteOpt.get());
        assertFalse(empty.mapToByte(s -> (byte) 1).isPresent());

        u.OptionalShort shortOpt = opt.mapToShort(s -> (short) s.length());
        assertTrue(shortOpt.isPresent());
        assertEquals((short) 4, shortOpt.get());
        assertFalse(empty.mapToShort(s -> (short) 1).isPresent());

        u.OptionalInt intOpt = opt.mapToInt(String::length);
        assertTrue(intOpt.isPresent());
        assertEquals(4, intOpt.get());
        assertFalse(empty.mapToInt(String::length).isPresent());

        u.OptionalLong longOpt = opt.mapToLong(s -> (long) s.length());
        assertTrue(longOpt.isPresent());
        assertEquals(4L, longOpt.get());
        assertFalse(empty.mapToLong(s -> 1L).isPresent());

        u.OptionalFloat floatOpt = opt.mapToFloat(s -> (float) s.length());
        assertTrue(floatOpt.isPresent());
        assertEquals(4.0f, floatOpt.get());
        assertFalse(empty.mapToFloat(s -> 1.0f).isPresent());

        u.OptionalDouble doubleOpt = opt.mapToDouble(s -> (double) s.length());
        assertTrue(doubleOpt.isPresent());
        assertEquals(4.0, doubleOpt.get());
        assertFalse(empty.mapToDouble(s -> 1.0).isPresent());
    }

    @Test
    public void testImmutability() {
        Optional<String> original = Optional.of("test");
        Optional<String> filtered = original.filter(s -> false);

        assertTrue(original.isPresent());
        assertFalse(filtered.isPresent());
        assertNotSame(original, filtered);
    }

    @Test
    public void testChaining() throws Exception {
        Optional<String> result = Optional.of("test").filter(s -> s.length() > 3).map(String::toUpperCase).flatMap(s -> Optional.of(s + "!"));

        assertTrue(result.isPresent());
        assertEquals("TEST!", result.get());

        Optional<String> empty = Optional.of("ab").filter(s -> s.length() > 3).map(String::toUpperCase);

        assertFalse(empty.isPresent());
    }

    @Test
    public void testWithNullValues() {
        assertThrows(NullPointerException.class, () -> Optional.of(null));

        Optional<String> nullableEmpty = Optional.ofNullable(null);
        assertFalse(nullableEmpty.isPresent());
        assertTrue(nullableEmpty.isEmpty());
    }

    @Test
    public void testSpecialCharacters() {
        OptionalChar newline = OptionalChar.of('\n');
        assertTrue(newline.isPresent());
        assertEquals('\n', newline.get());

        OptionalChar tab = OptionalChar.of('\t');
        assertTrue(tab.isPresent());
        assertEquals('\t', tab.get());

        OptionalChar nullChar = OptionalChar.of('\0');
        assertTrue(nullChar.isPresent());
        assertEquals('\0', nullChar.get());

        OptionalChar unicode = OptionalChar.of('\u03A9');
        assertTrue(unicode.isPresent());
        assertEquals('\u03A9', unicode.get());
    }

    @Test
    public void testEdgeCases() {
        OptionalInt min = OptionalInt.of(Integer.MIN_VALUE);
        assertTrue(min.isPresent());
        assertEquals(Integer.MIN_VALUE, min.get());

        OptionalInt max = OptionalInt.of(Integer.MAX_VALUE);
        assertTrue(max.isPresent());
        assertEquals(Integer.MAX_VALUE, max.get());

        OptionalInt overflow = OptionalInt.of(Integer.MAX_VALUE).map(v -> v + 1);
        assertTrue(overflow.isPresent());
        assertEquals(Integer.MIN_VALUE, overflow.get());
    }

    @Test
    public void testIsNotPresent() {
        assertFalse(Nullable.of("test").isNotPresent());
        assertFalse(Nullable.of((String) null).isNotPresent());
        assertTrue(Nullable.empty().isNotPresent());
    }

    @Test
    public void testIsNull() {
        assertFalse(Nullable.of("test").isNull());
        assertTrue(Nullable.of((String) null).isNull());
        assertTrue(Nullable.empty().isNull());
    }

    @Test
    public void testIsNotNull() {
        assertTrue(Nullable.of("test").isNotNull());
        assertFalse(Nullable.of((String) null).isNotNull());
        assertFalse(Nullable.empty().isNotNull());
    }

    @Test
    public void testMapToPrimitivesIfNotNull() throws Exception {
        Nullable<String> nullable = Nullable.of("test");
        Nullable<String> nullValue = Nullable.of((String) null);

        assertTrue(nullable.mapToBooleanIfNotNull(s -> s.length() > 3).isPresent());
        assertTrue(nullable.mapToCharIfNotNull(s -> s.charAt(0)).isPresent());
        assertTrue(nullable.mapToByteIfNotNull(s -> (byte) s.length()).isPresent());
        assertTrue(nullable.mapToShortIfNotNull(s -> (short) s.length()).isPresent());
        assertTrue(nullable.mapToIntIfNotNull(String::length).isPresent());
        assertTrue(nullable.mapToLongIfNotNull(s -> (long) s.length()).isPresent());
        assertTrue(nullable.mapToFloatIfNotNull(s -> (float) s.length()).isPresent());
        assertTrue(nullable.mapToDoubleIfNotNull(s -> (double) s.length()).isPresent());

        assertFalse(nullValue.mapToBooleanIfNotNull(s -> {
            fail("Should not be called");
            return true;
        }).isPresent());
        assertFalse(nullValue.mapToCharIfNotNull(s -> {
            fail("Should not be called");
            return 'a';
        }).isPresent());
        assertFalse(nullValue.mapToByteIfNotNull(s -> {
            fail("Should not be called");
            return (byte) 1;
        }).isPresent());
        assertFalse(nullValue.mapToShortIfNotNull(s -> {
            fail("Should not be called");
            return (short) 1;
        }).isPresent());
        assertFalse(nullValue.mapToIntIfNotNull(s -> {
            fail("Should not be called");
            return 1;
        }).isPresent());
        assertFalse(nullValue.mapToLongIfNotNull(s -> {
            fail("Should not be called");
            return 1L;
        }).isPresent());
        assertFalse(nullValue.mapToFloatIfNotNull(s -> {
            fail("Should not be called");
            return 1.0f;
        }).isPresent());
        assertFalse(nullValue.mapToDoubleIfNotNull(s -> {
            fail("Should not be called");
            return 1.0;
        }).isPresent());
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

    @Test
    public void testSpecialValues() {
        OptionalDouble min = OptionalDouble.of(Double.MIN_VALUE);
        assertTrue(min.isPresent());
        assertEquals(Double.MIN_VALUE, min.get());

        OptionalDouble max = OptionalDouble.of(Double.MAX_VALUE);
        assertTrue(max.isPresent());
        assertEquals(Double.MAX_VALUE, max.get());

        OptionalDouble posInf = OptionalDouble.of(Double.POSITIVE_INFINITY);
        assertTrue(posInf.isPresent());
        assertTrue(Double.isInfinite(posInf.get()));
        assertTrue(posInf.get() > 0);

        OptionalDouble negInf = OptionalDouble.of(Double.NEGATIVE_INFINITY);
        assertTrue(negInf.isPresent());
        assertTrue(Double.isInfinite(negInf.get()));
        assertTrue(negInf.get() < 0);

        OptionalDouble result = posInf.map(v -> v + 1);
        assertTrue(Double.isInfinite(result.get()));

        result = OptionalDouble.of(1.0).map(v -> v / 0.0);
        assertTrue(Double.isInfinite(result.get()));

        result = OptionalDouble.of(0.0).map(v -> v / 0.0);
        assertTrue(Double.isNaN(result.get()));
    }

    @Test
    public void testGettersAndBasicMethods() {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        assertEquals(42.5f, opt.get());
        assertTrue(opt.isPresent());
        assertFalse(opt.isEmpty());

        OptionalFloat empty = OptionalFloat.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertThrows(NoSuchElementException.class, () -> empty.get());
    }

    @Test
    public void testIfPresentMethods() throws Exception {
        float[] result = new float[1];
        OptionalFloat.of(42.5f).ifPresent(v -> result[0] = v);
        assertEquals(42.5f, result[0]);

        boolean[] emptyCalled = new boolean[1];
        OptionalFloat.of(42.5f).ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals(42.5f, result[0]);
        assertFalse(emptyCalled[0]);

        OptionalFloat.empty().ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOrElseMethods() {
        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseZero());
        assertEquals(0.0f, OptionalFloat.empty().orElseZero());

        assertEquals(42.5f, OptionalFloat.of(42.5f).orElse(100.0f));
        assertEquals(100.0f, OptionalFloat.empty().orElse(100.0f));

        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseGet(() -> 100.0f));
        assertEquals(100.0f, OptionalFloat.empty().orElseGet(() -> 100.0f));
    }

    @Test
    public void testCollectionMethods() {
        List<Float> list = OptionalFloat.of(42.5f).toList();
        assertEquals(1, list.size());
        assertEquals(42.5f, list.get(0));

        assertTrue(OptionalFloat.empty().toList().isEmpty());

        Set<Float> set = OptionalFloat.of(42.5f).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42.5f));

        ImmutableList<Float> immList = OptionalFloat.of(42.5f).toImmutableList();
        assertEquals(1, immList.size());
        assertEquals(42.5f, immList.get(0));

        ImmutableSet<Float> immSet = OptionalFloat.of(42.5f).toImmutableSet();
        assertEquals(1, immSet.size());
        assertTrue(immSet.contains(42.5f));
    }

    // ===================== OptionalByte Tests =====================

    @Test
    public void testOptionalByte_empty() {
        OptionalByte empty = OptionalByte.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertThrows(NoSuchElementException.class, () -> empty.get());
    }

    @Test
    public void testOptionalByte_of() {
        OptionalByte opt = OptionalByte.of((byte) 42);
        assertTrue(opt.isPresent());
        assertFalse(opt.isEmpty());
        assertEquals((byte) 42, opt.get());
    }

    @Test
    public void testOptionalByte_ofNullable() {
        OptionalByte present = OptionalByte.ofNullable((byte) 10);
        assertTrue(present.isPresent());
        assertEquals((byte) 10, present.get());

        OptionalByte empty = OptionalByte.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalByte_ifPresent() throws Exception {
        byte[] result = new byte[1];
        OptionalByte.of((byte) 5).ifPresent(v -> result[0] = v);
        assertEquals((byte) 5, result[0]);

        result[0] = 0;
        OptionalByte.empty().ifPresent(v -> result[0] = v);
        assertEquals((byte) 0, result[0]);
    }

    @Test
    public void testOptionalByte_ifPresentOrElse() throws Exception {
        byte[] result = new byte[1];
        boolean[] emptyCalled = new boolean[1];

        OptionalByte.of((byte) 5).ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals((byte) 5, result[0]);
        assertFalse(emptyCalled[0]);

        OptionalByte.empty().ifPresentOrElse(v -> result[0] = (byte) 99, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOptionalByte_filter() throws Exception {
        OptionalByte opt = OptionalByte.of((byte) 10);
        assertTrue(opt.filter(v -> v > 5).isPresent());
        assertFalse(opt.filter(v -> v > 20).isPresent());
        assertFalse(OptionalByte.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalByte_map() throws Exception {
        OptionalByte opt = OptionalByte.of((byte) 10);
        OptionalByte mapped = opt.map(v -> (byte) (v * 2));
        assertTrue(mapped.isPresent());
        assertEquals((byte) 20, mapped.get());
        assertFalse(OptionalByte.empty().map(v -> (byte) 1).isPresent());
    }

    @Test
    public void testOptionalByte_mapToInt() throws Exception {
        OptionalByte opt = OptionalByte.of((byte) 10);
        OptionalInt mapped = opt.mapToInt(v -> v * 10);
        assertTrue(mapped.isPresent());
        assertEquals(100, mapped.get());
        assertFalse(OptionalByte.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalByte_mapToObj() throws Exception {
        OptionalByte opt = OptionalByte.of((byte) 65);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("65", mapped.get());
        assertFalse(OptionalByte.empty().mapToObj(v -> "x").isPresent());
    }

    @Test
    public void testOptionalByte_flatMap() throws Exception {
        OptionalByte opt = OptionalByte.of((byte) 10);
        OptionalByte mapped = opt.flatMap(v -> OptionalByte.of((byte) (v + 1)));
        assertTrue(mapped.isPresent());
        assertEquals((byte) 11, mapped.get());

        assertFalse(opt.flatMap(v -> OptionalByte.empty()).isPresent());
        assertFalse(OptionalByte.empty().flatMap(v -> OptionalByte.of((byte) 1)).isPresent());
    }

    @Test
    public void testOptionalByte_or() {
        OptionalByte opt = OptionalByte.of((byte) 10);
        assertEquals((byte) 10, opt.or(() -> OptionalByte.of((byte) 20)).get());

        OptionalByte empty = OptionalByte.empty();
        assertEquals((byte) 20, empty.or(() -> OptionalByte.of((byte) 20)).get());
    }

    @Test
    public void testOptionalByte_orElseZero() {
        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElseZero());
        assertEquals((byte) 0, OptionalByte.empty().orElseZero());
    }

    @Test
    public void testOptionalByte_orElse() {
        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElse((byte) 20));
        assertEquals((byte) 20, OptionalByte.empty().orElse((byte) 20));
    }

    @Test
    public void testOptionalByte_orElseThrow() {
        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow());

        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElseThrow("Error"));
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Error"));

        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElseThrow(() -> new IllegalStateException()));
        assertThrows(IllegalStateException.class, () -> OptionalByte.empty().orElseThrow(() -> new IllegalStateException()));
    }

    @Test
    public void testOptionalByte_stream() {
        assertEquals(1, OptionalByte.of((byte) 10).stream().count());
        assertEquals(0, OptionalByte.empty().stream().count());
    }

    @Test
    public void testOptionalByte_toList() {
        List<Byte> list = OptionalByte.of((byte) 10).toList();
        assertEquals(1, list.size());
        assertEquals((byte) 10, (byte) list.get(0));
        assertTrue(OptionalByte.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalByte_toSet() {
        Set<Byte> set = OptionalByte.of((byte) 10).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains((byte) 10));
        assertTrue(OptionalByte.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalByte_toImmutableList() {
        ImmutableList<Byte> list = OptionalByte.of((byte) 10).toImmutableList();
        assertEquals(1, list.size());
        assertEquals((byte) 10, (byte) list.get(0));
        assertTrue(OptionalByte.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalByte_toImmutableSet() {
        ImmutableSet<Byte> set = OptionalByte.of((byte) 10).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains((byte) 10));
        assertTrue(OptionalByte.empty().toImmutableSet().isEmpty());
    }

    @Test
    public void testOptionalByte_boxed() {
        Optional<Byte> boxed = OptionalByte.of((byte) 10).boxed();
        assertTrue(boxed.isPresent());
        assertEquals((byte) 10, (byte) boxed.get());
        assertFalse(OptionalByte.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalByte_compareTo() {
        OptionalByte a = OptionalByte.of((byte) 10);
        OptionalByte b = OptionalByte.of((byte) 20);
        OptionalByte empty = OptionalByte.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalByte.of((byte) 10)));
        assertTrue(a.compareTo(empty) > 0);
        assertTrue(empty.compareTo(a) < 0);
    }

    @Test
    public void testOptionalByte_equalsAndHashCode() {
        OptionalByte a = OptionalByte.of((byte) 10);
        OptionalByte b = OptionalByte.of((byte) 10);
        OptionalByte c = OptionalByte.of((byte) 20);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testOptionalByte_toString() {
        assertEquals("OptionalByte[10]", OptionalByte.of((byte) 10).toString());
        assertEquals("OptionalByte.empty", OptionalByte.empty().toString());
    }

    // ===================== OptionalShort Tests =====================

    @Test
    public void testOptionalShort_empty() {
        OptionalShort empty = OptionalShort.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertThrows(NoSuchElementException.class, () -> empty.get());
    }

    @Test
    public void testOptionalShort_of() {
        OptionalShort opt = OptionalShort.of((short) 100);
        assertTrue(opt.isPresent());
        assertFalse(opt.isEmpty());
        assertEquals((short) 100, opt.get());
    }

    @Test
    public void testOptionalShort_ofNullable() {
        OptionalShort present = OptionalShort.ofNullable((short) 100);
        assertTrue(present.isPresent());
        assertEquals((short) 100, present.get());

        OptionalShort empty = OptionalShort.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalShort_ifPresent() throws Exception {
        short[] result = new short[1];
        OptionalShort.of((short) 5).ifPresent(v -> result[0] = v);
        assertEquals((short) 5, result[0]);

        result[0] = 0;
        OptionalShort.empty().ifPresent(v -> result[0] = v);
        assertEquals((short) 0, result[0]);
    }

    @Test
    public void testOptionalShort_ifPresentOrElse() throws Exception {
        short[] result = new short[1];
        boolean[] emptyCalled = new boolean[1];

        OptionalShort.of((short) 5).ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals((short) 5, result[0]);
        assertFalse(emptyCalled[0]);

        OptionalShort.empty().ifPresentOrElse(v -> result[0] = (short) 99, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOptionalShort_filter() throws Exception {
        OptionalShort opt = OptionalShort.of((short) 100);
        assertTrue(opt.filter(v -> v > 50).isPresent());
        assertFalse(opt.filter(v -> v > 200).isPresent());
        assertFalse(OptionalShort.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalShort_map() throws Exception {
        OptionalShort opt = OptionalShort.of((short) 10);
        OptionalShort mapped = opt.map(v -> (short) (v * 2));
        assertTrue(mapped.isPresent());
        assertEquals((short) 20, mapped.get());
        assertFalse(OptionalShort.empty().map(v -> (short) 1).isPresent());
    }

    @Test
    public void testOptionalShort_mapToInt() throws Exception {
        OptionalShort opt = OptionalShort.of((short) 100);
        OptionalInt mapped = opt.mapToInt(v -> v * 10);
        assertTrue(mapped.isPresent());
        assertEquals(1000, mapped.get());
        assertFalse(OptionalShort.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalShort_mapToObj() throws Exception {
        OptionalShort opt = OptionalShort.of((short) 100);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("100", mapped.get());
        assertFalse(OptionalShort.empty().mapToObj(v -> "x").isPresent());
    }

    @Test
    public void testOptionalShort_flatMap() throws Exception {
        OptionalShort opt = OptionalShort.of((short) 10);
        OptionalShort mapped = opt.flatMap(v -> OptionalShort.of((short) (v + 1)));
        assertTrue(mapped.isPresent());
        assertEquals((short) 11, mapped.get());

        assertFalse(opt.flatMap(v -> OptionalShort.empty()).isPresent());
        assertFalse(OptionalShort.empty().flatMap(v -> OptionalShort.of((short) 1)).isPresent());
    }

    @Test
    public void testOptionalShort_or() {
        OptionalShort opt = OptionalShort.of((short) 10);
        assertEquals((short) 10, opt.or(() -> OptionalShort.of((short) 20)).get());

        OptionalShort empty = OptionalShort.empty();
        assertEquals((short) 20, empty.or(() -> OptionalShort.of((short) 20)).get());
    }

    @Test
    public void testOptionalShort_orElseZero() {
        assertEquals((short) 10, OptionalShort.of((short) 10).orElseZero());
        assertEquals((short) 0, OptionalShort.empty().orElseZero());
    }

    @Test
    public void testOptionalShort_orElse() {
        assertEquals((short) 10, OptionalShort.of((short) 10).orElse((short) 20));
        assertEquals((short) 20, OptionalShort.empty().orElse((short) 20));
    }

    @Test
    public void testOptionalShort_orElseThrow() {
        assertEquals((short) 10, OptionalShort.of((short) 10).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow());

        assertEquals((short) 10, OptionalShort.of((short) 10).orElseThrow("Error"));
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Error"));

        assertEquals((short) 10, OptionalShort.of((short) 10).orElseThrow(() -> new IllegalStateException()));
        assertThrows(IllegalStateException.class, () -> OptionalShort.empty().orElseThrow(() -> new IllegalStateException()));
    }

    @Test
    public void testOptionalShort_stream() {
        assertEquals(1, OptionalShort.of((short) 10).stream().count());
        assertEquals(0, OptionalShort.empty().stream().count());
    }

    @Test
    public void testOptionalShort_toList() {
        List<Short> list = OptionalShort.of((short) 10).toList();
        assertEquals(1, list.size());
        assertEquals((short) 10, (short) list.get(0));
        assertTrue(OptionalShort.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalShort_toSet() {
        Set<Short> set = OptionalShort.of((short) 10).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains((short) 10));
        assertTrue(OptionalShort.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalShort_toImmutableList() {
        ImmutableList<Short> list = OptionalShort.of((short) 10).toImmutableList();
        assertEquals(1, list.size());
        assertEquals((short) 10, (short) list.get(0));
        assertTrue(OptionalShort.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalShort_toImmutableSet() {
        ImmutableSet<Short> set = OptionalShort.of((short) 10).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains((short) 10));
        assertTrue(OptionalShort.empty().toImmutableSet().isEmpty());
    }

    @Test
    public void testOptionalShort_boxed() {
        Optional<Short> boxed = OptionalShort.of((short) 10).boxed();
        assertTrue(boxed.isPresent());
        assertEquals((short) 10, (short) boxed.get());
        assertFalse(OptionalShort.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalShort_compareTo() {
        OptionalShort a = OptionalShort.of((short) 10);
        OptionalShort b = OptionalShort.of((short) 20);
        OptionalShort empty = OptionalShort.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalShort.of((short) 10)));
        assertTrue(a.compareTo(empty) > 0);
        assertTrue(empty.compareTo(a) < 0);
    }

    @Test
    public void testOptionalShort_equalsAndHashCode() {
        OptionalShort a = OptionalShort.of((short) 10);
        OptionalShort b = OptionalShort.of((short) 10);
        OptionalShort c = OptionalShort.of((short) 20);

        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testOptionalShort_toString() {
        assertEquals("OptionalShort[10]", OptionalShort.of((short) 10).toString());
        assertEquals("OptionalShort.empty", OptionalShort.empty().toString());
    }

    // ===================== OptionalLong Additional Tests =====================

    @Test
    public void testOptionalLong_ofNullable() {
        OptionalLong present = OptionalLong.ofNullable(42L);
        assertTrue(present.isPresent());
        assertEquals(42L, present.get());

        OptionalLong empty = OptionalLong.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalLong_from() {
        java.util.OptionalLong jdkOpt = java.util.OptionalLong.of(42L);
        OptionalLong opt = OptionalLong.from(jdkOpt);
        assertTrue(opt.isPresent());
        assertEquals(42L, opt.get());

        OptionalLong fromEmpty = OptionalLong.from(java.util.OptionalLong.empty());
        assertFalse(fromEmpty.isPresent());
    }

    @Test
    public void testOptionalLong_filter() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        assertTrue(opt.filter(v -> v > 10).isPresent());
        assertFalse(opt.filter(v -> v > 100).isPresent());
        assertFalse(OptionalLong.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalLong_map() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        OptionalLong mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(84L, mapped.get());
        assertFalse(OptionalLong.empty().map(v -> 1L).isPresent());
    }

    @Test
    public void testOptionalLong_mapToInt() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(42, mapped.get());
        assertFalse(OptionalLong.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalLong_mapToDouble() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        OptionalDouble mapped = opt.mapToDouble(v -> v / 2.0);
        assertTrue(mapped.isPresent());
        assertEquals(21.0, mapped.get());
        assertFalse(OptionalLong.empty().mapToDouble(v -> 1.0).isPresent());
    }

    @Test
    public void testOptionalLong_mapToObj() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        Optional<String> mapped = opt.mapToObj(String::valueOf);
        assertTrue(mapped.isPresent());
        assertEquals("42", mapped.get());
        assertFalse(OptionalLong.empty().mapToObj(v -> "x").isPresent());
    }

    @Test
    public void testOptionalLong_flatMap() throws Exception {
        OptionalLong opt = OptionalLong.of(42L);
        OptionalLong mapped = opt.flatMap(v -> OptionalLong.of(v + 1));
        assertTrue(mapped.isPresent());
        assertEquals(43L, mapped.get());
        assertFalse(opt.flatMap(v -> OptionalLong.empty()).isPresent());
        assertFalse(OptionalLong.empty().flatMap(v -> OptionalLong.of(1L)).isPresent());
    }

    @Test
    public void testOptionalLong_or() {
        assertEquals(42L, OptionalLong.of(42L).or(() -> OptionalLong.of(100L)).get());
        assertEquals(100L, OptionalLong.empty().or(() -> OptionalLong.of(100L)).get());
    }

    @Test
    public void testOptionalLong_orElse() {
        assertEquals(42L, OptionalLong.of(42L).orElse(100L));
        assertEquals(100L, OptionalLong.empty().orElse(100L));
    }

    @Test
    public void testOptionalLong_orElseZero() {
        assertEquals(42L, OptionalLong.of(42L).orElseZero());
        assertEquals(0L, OptionalLong.empty().orElseZero());
    }

    @Test
    public void testOptionalLong_orElseThrow() {
        assertEquals(42L, OptionalLong.of(42L).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow());

        assertEquals(42L, OptionalLong.of(42L).orElseThrow("Error"));
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Error"));

        assertEquals(42L, OptionalLong.of(42L).orElseThrow(() -> new IllegalStateException()));
        assertThrows(IllegalStateException.class, () -> OptionalLong.empty().orElseThrow(() -> new IllegalStateException()));
    }

    @Test
    public void testOptionalLong_stream() {
        assertEquals(1, OptionalLong.of(42L).stream().count());
        assertEquals(0, OptionalLong.empty().stream().count());
    }

    @Test
    public void testOptionalLong_toList() {
        List<Long> list = OptionalLong.of(42L).toList();
        assertEquals(1, list.size());
        assertEquals(42L, (long) list.get(0));
        assertTrue(OptionalLong.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalLong_toSet() {
        Set<Long> set = OptionalLong.of(42L).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42L));
        assertTrue(OptionalLong.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalLong_boxed() {
        Optional<Long> boxed = OptionalLong.of(42L).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(42L, (long) boxed.get());
        assertFalse(OptionalLong.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalLong_toJdkOptional() {
        java.util.OptionalLong jdk = OptionalLong.of(42L).toJdkOptional();
        assertTrue(jdk.isPresent());
        assertEquals(42L, jdk.getAsLong());
        assertFalse(OptionalLong.empty().toJdkOptional().isPresent());
    }

    @Test
    public void testOptionalLong_compareTo() {
        OptionalLong a = OptionalLong.of(10L);
        OptionalLong b = OptionalLong.of(20L);
        OptionalLong empty = OptionalLong.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalLong.of(10L)));
        assertTrue(a.compareTo(empty) > 0);
    }

    @Test
    public void testOptionalLong_equalsAndHashCode() {
        assertEquals(OptionalLong.of(42L), OptionalLong.of(42L));
        assertNotEquals(OptionalLong.of(42L), OptionalLong.of(43L));
        assertEquals(OptionalLong.of(42L).hashCode(), OptionalLong.of(42L).hashCode());
    }

    @Test
    public void testOptionalLong_toString() {
        assertEquals("OptionalLong[42]", OptionalLong.of(42L).toString());
        assertEquals("OptionalLong.empty", OptionalLong.empty().toString());
    }

    // ===================== OptionalDouble Additional Tests =====================

    @Test
    public void testOptionalDouble_ofNullable() {
        OptionalDouble present = OptionalDouble.ofNullable(42.5);
        assertTrue(present.isPresent());
        assertEquals(42.5, present.get());

        OptionalDouble empty = OptionalDouble.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalDouble_from() {
        java.util.OptionalDouble jdkOpt = java.util.OptionalDouble.of(42.5);
        OptionalDouble opt = OptionalDouble.from(jdkOpt);
        assertTrue(opt.isPresent());
        assertEquals(42.5, opt.get());

        OptionalDouble fromEmpty = OptionalDouble.from(java.util.OptionalDouble.empty());
        assertFalse(fromEmpty.isPresent());
    }

    @Test
    public void testOptionalDouble_filter() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        assertTrue(opt.filter(v -> v > 10.0).isPresent());
        assertFalse(opt.filter(v -> v > 100.0).isPresent());
        assertFalse(OptionalDouble.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalDouble_map() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        OptionalDouble mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(85.0, mapped.get());
        assertFalse(OptionalDouble.empty().map(v -> 1.0).isPresent());
    }

    @Test
    public void testOptionalDouble_mapToInt() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(42, mapped.get());
        assertFalse(OptionalDouble.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalDouble_mapToObj() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        Optional<String> mapped = opt.mapToObj(String::valueOf);
        assertTrue(mapped.isPresent());
        assertEquals("42.5", mapped.get());
        assertFalse(OptionalDouble.empty().mapToObj(v -> "x").isPresent());
    }

    @Test
    public void testOptionalDouble_flatMap() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        OptionalDouble mapped = opt.flatMap(v -> OptionalDouble.of(v + 1));
        assertTrue(mapped.isPresent());
        assertEquals(43.5, mapped.get());
        assertFalse(opt.flatMap(v -> OptionalDouble.empty()).isPresent());
        assertFalse(OptionalDouble.empty().flatMap(v -> OptionalDouble.of(1.0)).isPresent());
    }

    @Test
    public void testOptionalDouble_or() {
        assertEquals(42.5, OptionalDouble.of(42.5).or(() -> OptionalDouble.of(100.0)).get());
        assertEquals(100.0, OptionalDouble.empty().or(() -> OptionalDouble.of(100.0)).get());
    }

    @Test
    public void testOptionalDouble_orElse() {
        assertEquals(42.5, OptionalDouble.of(42.5).orElse(100.0));
        assertEquals(100.0, OptionalDouble.empty().orElse(100.0));
    }

    @Test
    public void testOptionalDouble_orElseZero() {
        assertEquals(42.5, OptionalDouble.of(42.5).orElseZero());
        assertEquals(0.0, OptionalDouble.empty().orElseZero());
    }

    @Test
    public void testOptionalDouble_orElseThrow() {
        assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow());

        assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow("Error"));
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Error"));

        assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow(() -> new IllegalStateException()));
        assertThrows(IllegalStateException.class, () -> OptionalDouble.empty().orElseThrow(() -> new IllegalStateException()));
    }

    @Test
    public void testOptionalDouble_stream() {
        assertEquals(1, OptionalDouble.of(42.5).stream().count());
        assertEquals(0, OptionalDouble.empty().stream().count());
    }

    @Test
    public void testOptionalDouble_toList() {
        List<Double> list = OptionalDouble.of(42.5).toList();
        assertEquals(1, list.size());
        assertEquals(42.5, list.get(0));
        assertTrue(OptionalDouble.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalDouble_toSet() {
        Set<Double> set = OptionalDouble.of(42.5).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42.5));
        assertTrue(OptionalDouble.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalDouble_boxed() {
        Optional<Double> boxed = OptionalDouble.of(42.5).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(42.5, boxed.get());
        assertFalse(OptionalDouble.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalDouble_toJdkOptional() {
        java.util.OptionalDouble jdk = OptionalDouble.of(42.5).toJdkOptional();
        assertTrue(jdk.isPresent());
        assertEquals(42.5, jdk.getAsDouble());
        assertFalse(OptionalDouble.empty().toJdkOptional().isPresent());
    }

    @Test
    public void testOptionalDouble_compareTo() {
        OptionalDouble a = OptionalDouble.of(10.0);
        OptionalDouble b = OptionalDouble.of(20.0);
        OptionalDouble empty = OptionalDouble.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalDouble.of(10.0)));
        assertTrue(a.compareTo(empty) > 0);
    }

    @Test
    public void testOptionalDouble_equalsAndHashCode() {
        assertEquals(OptionalDouble.of(42.5), OptionalDouble.of(42.5));
        assertNotEquals(OptionalDouble.of(42.5), OptionalDouble.of(43.5));
        assertEquals(OptionalDouble.of(42.5).hashCode(), OptionalDouble.of(42.5).hashCode());
    }

    @Test
    public void testOptionalDouble_toString() {
        assertEquals("OptionalDouble[42.5]", OptionalDouble.of(42.5).toString());
        assertEquals("OptionalDouble.empty", OptionalDouble.empty().toString());
    }

    // ===================== OptionalFloat Additional Tests =====================

    @Test
    public void testOptionalFloat_ofNullable() {
        OptionalFloat present = OptionalFloat.ofNullable(42.5f);
        assertTrue(present.isPresent());
        assertEquals(42.5f, present.get());

        OptionalFloat empty = OptionalFloat.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalFloat_filter() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        assertTrue(opt.filter(v -> v > 10.0f).isPresent());
        assertFalse(opt.filter(v -> v > 100.0f).isPresent());
        assertFalse(OptionalFloat.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalFloat_map() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        OptionalFloat mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(85.0f, mapped.get());
        assertFalse(OptionalFloat.empty().map(v -> 1.0f).isPresent());
    }

    @Test
    public void testOptionalFloat_flatMap() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        OptionalFloat mapped = opt.flatMap(v -> OptionalFloat.of(v + 1));
        assertTrue(mapped.isPresent());
        assertEquals(43.5f, mapped.get());
        assertFalse(opt.flatMap(v -> OptionalFloat.empty()).isPresent());
        assertFalse(OptionalFloat.empty().flatMap(v -> OptionalFloat.of(1.0f)).isPresent());
    }

    @Test
    public void testOptionalFloat_or() {
        assertEquals(42.5f, OptionalFloat.of(42.5f).or(() -> OptionalFloat.of(100.0f)).get());
        assertEquals(100.0f, OptionalFloat.empty().or(() -> OptionalFloat.of(100.0f)).get());
    }

    @Test
    public void testOptionalFloat_orElseThrow() {
        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow());

        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseThrow("Error"));
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Error"));
    }

    @Test
    public void testOptionalFloat_stream() {
        assertEquals(1, OptionalFloat.of(42.5f).stream().count());
        assertEquals(0, OptionalFloat.empty().stream().count());
    }

    @Test
    public void testOptionalFloat_boxed() {
        Optional<Float> boxed = OptionalFloat.of(42.5f).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(42.5f, boxed.get());
        assertFalse(OptionalFloat.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalFloat_compareTo() {
        OptionalFloat a = OptionalFloat.of(10.0f);
        OptionalFloat b = OptionalFloat.of(20.0f);
        OptionalFloat empty = OptionalFloat.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalFloat.of(10.0f)));
        assertTrue(a.compareTo(empty) > 0);
    }

    @Test
    public void testOptionalFloat_equalsAndHashCode() {
        assertEquals(OptionalFloat.of(42.5f), OptionalFloat.of(42.5f));
        assertNotEquals(OptionalFloat.of(42.5f), OptionalFloat.of(43.5f));
        assertEquals(OptionalFloat.of(42.5f).hashCode(), OptionalFloat.of(42.5f).hashCode());
    }

    @Test
    public void testOptionalFloat_toString() {
        assertEquals("OptionalFloat[42.5]", OptionalFloat.of(42.5f).toString());
        assertEquals("OptionalFloat.empty", OptionalFloat.empty().toString());
    }

    // ===================== OptionalChar Additional Tests =====================

    @Test
    public void testOptionalChar_filter() throws Exception {
        OptionalChar opt = OptionalChar.of('A');
        assertTrue(opt.filter(v -> v == 'A').isPresent());
        assertFalse(opt.filter(v -> v == 'B').isPresent());
        assertFalse(OptionalChar.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalChar_map() throws Exception {
        OptionalChar opt = OptionalChar.of('A');
        OptionalChar mapped = opt.map(v -> (char) (v + 1));
        assertTrue(mapped.isPresent());
        assertEquals('B', mapped.get());
        assertFalse(OptionalChar.empty().map(v -> 'X').isPresent());
    }

    @Test
    public void testOptionalChar_mapToInt() throws Exception {
        OptionalChar opt = OptionalChar.of('A');
        OptionalInt mapped = opt.mapToInt(v -> (int) v);
        assertTrue(mapped.isPresent());
        assertEquals(65, mapped.get());
        assertFalse(OptionalChar.empty().mapToInt(v -> 0).isPresent());
    }

    @Test
    public void testOptionalChar_flatMap() throws Exception {
        OptionalChar opt = OptionalChar.of('A');
        OptionalChar mapped = opt.flatMap(v -> OptionalChar.of((char) (v + 1)));
        assertTrue(mapped.isPresent());
        assertEquals('B', mapped.get());
        assertFalse(opt.flatMap(v -> OptionalChar.empty()).isPresent());
        assertFalse(OptionalChar.empty().flatMap(v -> OptionalChar.of('X')).isPresent());
    }

    @Test
    public void testOptionalChar_or() {
        assertEquals('A', OptionalChar.of('A').or(() -> OptionalChar.of('Z')).get());
        assertEquals('Z', OptionalChar.empty().or(() -> OptionalChar.of('Z')).get());
    }

    @Test
    public void testOptionalChar_orElseThrow() {
        assertEquals('A', OptionalChar.of('A').orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow());
    }

    @Test
    public void testOptionalChar_stream() {
        assertEquals(1, OptionalChar.of('A').stream().count());
        assertEquals(0, OptionalChar.empty().stream().count());
    }

    @Test
    public void testOptionalChar_toList() {
        List<Character> list = OptionalChar.of('A').toList();
        assertEquals(1, list.size());
        assertEquals('A', (char) list.get(0));
        assertTrue(OptionalChar.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalChar_toSet() {
        Set<Character> set = OptionalChar.of('A').toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains('A'));
        assertTrue(OptionalChar.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalChar_boxed() {
        Optional<Character> boxed = OptionalChar.of('A').boxed();
        assertTrue(boxed.isPresent());
        assertEquals('A', (char) boxed.get());
        assertFalse(OptionalChar.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalChar_compareTo() {
        OptionalChar a = OptionalChar.of('A');
        OptionalChar b = OptionalChar.of('B');
        OptionalChar empty = OptionalChar.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalChar.of('A')));
        assertTrue(a.compareTo(empty) > 0);
    }

    @Test
    public void testOptionalChar_equalsAndHashCode() {
        assertEquals(OptionalChar.of('A'), OptionalChar.of('A'));
        assertNotEquals(OptionalChar.of('A'), OptionalChar.of('B'));
        assertEquals(OptionalChar.of('A').hashCode(), OptionalChar.of('A').hashCode());
    }

    @Test
    public void testOptionalChar_toString() {
        assertEquals("OptionalChar[A]", OptionalChar.of('A').toString());
        assertEquals("OptionalChar.empty", OptionalChar.empty().toString());
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

    // ===================== Additional Missing Tests =====================

    // --- OptionalBoolean: getAsBoolean() ---

    @Test
    public void testOptionalBoolean_getAsBoolean() {
        OptionalBoolean opt = OptionalBoolean.of(true);
        assertTrue(opt.getAsBoolean());

        OptionalBoolean optFalse = OptionalBoolean.of(false);
        assertFalse(optFalse.getAsBoolean());

        OptionalBoolean empty = OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsBoolean());
    }

    @Test
    public void testOptionalBoolean_map() throws Exception {
        OptionalBoolean opt = OptionalBoolean.of(true);
        OptionalBoolean mapped = opt.map(v -> !v);
        assertTrue(mapped.isPresent());
        assertFalse(mapped.get());

        OptionalBoolean empty = OptionalBoolean.empty();
        assertFalse(empty.map(v -> !v).isPresent());
    }

    @Test
    public void testOptionalBoolean_toString() {
        assertEquals("OptionalBoolean[true]", OptionalBoolean.of(true).toString());
        assertEquals("OptionalBoolean[false]", OptionalBoolean.of(false).toString());
        assertEquals("OptionalBoolean.empty", OptionalBoolean.empty().toString());
    }

    // --- OptionalChar: additional missing ---

    @Test
    public void testOptionalChar_ofNullable() {
        OptionalChar present = OptionalChar.ofNullable('X');
        assertTrue(present.isPresent());
        assertEquals('X', present.get());

        OptionalChar empty = OptionalChar.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalChar_getAsChar() {
        OptionalChar opt = OptionalChar.of('Z');
        assertEquals('Z', opt.getAsChar());

        OptionalChar empty = OptionalChar.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsChar());
    }

    @Test
    public void testOptionalChar_ifPresent() throws Exception {
        char[] result = new char[1];
        OptionalChar.of('A').ifPresent(v -> result[0] = v);
        assertEquals('A', result[0]);

        result[0] = 0;
        OptionalChar.empty().ifPresent(v -> result[0] = v);
        assertEquals((char) 0, result[0]);
    }

    @Test
    public void testOptionalChar_ifPresentOrElse() throws Exception {
        char[] result = new char[1];
        boolean[] emptyCalled = new boolean[1];

        OptionalChar.of('B').ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals('B', result[0]);
        assertFalse(emptyCalled[0]);

        OptionalChar.empty().ifPresentOrElse(v -> result[0] = 'X', () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOptionalChar_isEmpty() {
        assertFalse(OptionalChar.of('A').isEmpty());
        assertTrue(OptionalChar.empty().isEmpty());
    }

    @Test
    public void testOptionalChar_orElse() {
        assertEquals('A', OptionalChar.of('A').orElse('Z'));
        assertEquals('Z', OptionalChar.empty().orElse('Z'));
    }

    @Test
    public void testOptionalChar_orElseGet() {
        assertEquals('A', OptionalChar.of('A').orElseGet(() -> 'Z'));
        assertEquals('Z', OptionalChar.empty().orElseGet(() -> 'Z'));
    }

    @Test
    public void testOptionalChar_orElseThrowWithSupplier() {
        assertEquals('A', OptionalChar.of('A').orElseThrow(() -> new IllegalStateException()));
        assertThrows(IllegalStateException.class, () -> OptionalChar.empty().orElseThrow(() -> new IllegalStateException()));
    }

    @Test
    public void testOptionalChar_toImmutableList() {
        ImmutableList<Character> list = OptionalChar.of('A').toImmutableList();
        assertEquals(1, list.size());
        assertEquals('A', (char) list.get(0));
        assertTrue(OptionalChar.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalChar_toImmutableSet() {
        ImmutableSet<Character> set = OptionalChar.of('A').toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains('A'));
        assertTrue(OptionalChar.empty().toImmutableSet().isEmpty());
    }

    // --- OptionalInt: additional missing ---

    @Test
    public void testOptionalInt_ofNullable() {
        OptionalInt present = OptionalInt.ofNullable(42);
        assertTrue(present.isPresent());
        assertEquals(42, present.get());

        OptionalInt empty = OptionalInt.ofNullable(null);
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptionalInt_mapToBoolean() throws Exception {
        OptionalInt opt = OptionalInt.of(10);
        OptionalBoolean mapped = opt.mapToBoolean(v -> v > 5);
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        assertFalse(OptionalInt.empty().mapToBoolean(v -> true).isPresent());
    }

    @Test
    public void testOptionalInt_mapToChar() throws Exception {
        OptionalInt opt = OptionalInt.of(65);
        OptionalChar mapped = opt.mapToChar(v -> (char) v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals('A', mapped.get());

        assertFalse(OptionalInt.empty().mapToChar(v -> 'X').isPresent());
    }

    @Test
    public void testOptionalInt_mapToDouble() throws Exception {
        OptionalInt opt = OptionalInt.of(10);
        OptionalDouble mapped = opt.mapToDouble(v -> v / 2.0);
        assertTrue(mapped.isPresent());
        assertEquals(5.0, mapped.get());

        assertFalse(OptionalInt.empty().mapToDouble(v -> 1.0).isPresent());
    }

    @Test
    public void testOptionalInt_ifPresent() throws Exception {
        int[] result = new int[1];
        OptionalInt.of(42).ifPresent(v -> result[0] = v);
        assertEquals(42, result[0]);

        result[0] = 0;
        OptionalInt.empty().ifPresent(v -> result[0] = v);
        assertEquals(0, result[0]);
    }

    @Test
    public void testOptionalInt_ifPresentOrElse() throws Exception {
        int[] result = new int[1];
        boolean[] emptyCalled = new boolean[1];

        OptionalInt.of(42).ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals(42, result[0]);
        assertFalse(emptyCalled[0]);

        OptionalInt.empty().ifPresentOrElse(v -> result[0] = 99, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOptionalInt_or() {
        assertEquals(42, OptionalInt.of(42).or(() -> OptionalInt.of(100)).get());
        assertEquals(100, OptionalInt.empty().or(() -> OptionalInt.of(100)).get());
    }

    @Test
    public void testOptionalInt_orElseZero() {
        assertEquals(42, OptionalInt.of(42).orElseZero());
        assertEquals(0, OptionalInt.empty().orElseZero());
    }

    @Test
    public void testOptionalInt_orElse() {
        assertEquals(42, OptionalInt.of(42).orElse(100));
        assertEquals(100, OptionalInt.empty().orElse(100));
    }

    @Test
    public void testOptionalInt_orElseGet() {
        assertEquals(42, OptionalInt.of(42).orElseGet(() -> 100));
        assertEquals(100, OptionalInt.empty().orElseGet(() -> 100));
    }

    @Test
    public void testOptionalInt_toList() {
        List<Integer> list = OptionalInt.of(42).toList();
        assertEquals(1, list.size());
        assertEquals(42, (int) list.get(0));
        assertTrue(OptionalInt.empty().toList().isEmpty());
    }

    @Test
    public void testOptionalInt_toSet() {
        Set<Integer> set = OptionalInt.of(42).toSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
        assertTrue(OptionalInt.empty().toSet().isEmpty());
    }

    @Test
    public void testOptionalInt_toImmutableList() {
        ImmutableList<Integer> list = OptionalInt.of(42).toImmutableList();
        assertEquals(1, list.size());
        assertEquals(42, (int) list.get(0));
        assertTrue(OptionalInt.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalInt_toImmutableSet() {
        ImmutableSet<Integer> set = OptionalInt.of(42).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
        assertTrue(OptionalInt.empty().toImmutableSet().isEmpty());
    }

    @Test
    public void testOptionalInt_boxed() {
        Optional<Integer> boxed = OptionalInt.of(42).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(42, (int) boxed.get());
        assertFalse(OptionalInt.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalInt_compareTo() {
        OptionalInt a = OptionalInt.of(10);
        OptionalInt b = OptionalInt.of(20);
        OptionalInt empty = OptionalInt.empty();

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(OptionalInt.of(10)));
        assertTrue(a.compareTo(empty) > 0);
        assertTrue(empty.compareTo(a) < 0);
    }

    @Test
    public void testOptionalInt_equalsAndHashCode() {
        assertEquals(OptionalInt.of(42), OptionalInt.of(42));
        assertNotEquals(OptionalInt.of(42), OptionalInt.of(43));
        assertEquals(OptionalInt.of(42).hashCode(), OptionalInt.of(42).hashCode());
    }

    @Test
    public void testOptionalInt_toString() {
        assertEquals("OptionalInt[42]", OptionalInt.of(42).toString());
        assertEquals("OptionalInt.empty", OptionalInt.empty().toString());
    }

    @Test
    public void testOptionalInt_toJdkOptional() {
        java.util.OptionalInt jdk = OptionalInt.of(42).toJdkOptional();
        assertTrue(jdk.isPresent());
        assertEquals(42, jdk.getAsInt());
        assertFalse(OptionalInt.empty().toJdkOptional().isPresent());
    }

    @Test
    public void testOptionalInt_flatMap() throws Exception {
        OptionalInt result = OptionalInt.of(5).flatMap(v -> OptionalInt.of(v * 2));
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
        assertFalse(OptionalInt.of(5).flatMap(v -> OptionalInt.empty()).isPresent());
        assertFalse(OptionalInt.empty().flatMap(v -> OptionalInt.of(99)).isPresent());
    }

    @Test
    public void testOptionalInt_stream() {
        long count = OptionalInt.of(42).stream().count();
        assertEquals(1, count);
        assertEquals(0, OptionalInt.empty().stream().count());
    }

    @Test
    public void testOptionalInt_orElseThrow_String() {
        assertEquals(42, OptionalInt.of(42).orElseThrow("No value"));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Custom error"));
    }

    @Test
    public void testOptionalInt_orElseThrow_String_Arg1() {
        assertEquals(42, OptionalInt.of(42).orElseThrow("Error: %s", "arg1"));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: %s", "arg1"));
    }

    @Test
    public void testOptionalInt_orElseThrow_String_Arg2() {
        assertEquals(42, OptionalInt.of(42).orElseThrow("Error: %s %s", "a", "b"));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: %s %s", "a", "b"));
    }

    @Test
    public void testOptionalInt_orElseThrow_String_Arg3() {
        assertEquals(42, OptionalInt.of(42).orElseThrow("Error: %s %s %s", "a", "b", "c"));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: %s %s %s", "a", "b", "c"));
    }

    @Test
    public void testOptionalInt_orElseThrow_Varargs() {
        assertEquals(42, OptionalInt.of(42).orElseThrow("Error: %s", new Object[] { "arg" }));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error", new Object[0]));
    }

    @Test
    public void testOptionalInt_orElseThrow_Supplier() throws Exception {
        assertEquals(42, OptionalInt.of(42).orElseThrow(() -> new IllegalStateException("not found")));
        assertThrows(IllegalStateException.class, () -> OptionalInt.empty().orElseThrow(() -> new IllegalStateException("not found")));
    }

    // --- OptionalFloat: additional missing ---

    @Test
    public void testOptionalFloat_getAsFloat() {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        assertEquals(42.5f, opt.getAsFloat());

        OptionalFloat empty = OptionalFloat.empty();
        assertThrows(NoSuchElementException.class, () -> empty.getAsFloat());
    }

    @Test
    public void testOptionalFloat_mapToObj() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        Optional<String> mapped = opt.mapToObj(String::valueOf);
        assertTrue(mapped.isPresent());
        assertEquals("42.5", mapped.get());

        assertFalse(OptionalFloat.empty().mapToObj(v -> "x").isPresent());
    }

    @Test
    public void testOptionalFloat_mapToInt() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(42, mapped.get());

        assertFalse(OptionalFloat.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalFloat_mapToDouble() throws Exception {
        OptionalFloat opt = OptionalFloat.of(42.5f);
        OptionalDouble mapped = opt.mapToDouble(v -> v.doubleValue());
        assertTrue(mapped.isPresent());
        assertEquals(42.5, mapped.get(), 0.001);

        assertFalse(OptionalFloat.empty().mapToDouble(v -> 1.0).isPresent());
    }

    @Test
    public void testOptionalFloat_toImmutableList() {
        ImmutableList<Float> list = OptionalFloat.of(42.5f).toImmutableList();
        assertEquals(1, list.size());
        assertEquals(42.5f, list.get(0));
        assertTrue(OptionalFloat.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalFloat_toImmutableSet() {
        ImmutableSet<Float> set = OptionalFloat.of(42.5f).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42.5f));
        assertTrue(OptionalFloat.empty().toImmutableSet().isEmpty());
    }

    // --- OptionalLong: additional missing ---

    @Test
    public void testOptionalLong_ifPresent() throws Exception {
        long[] result = new long[1];
        OptionalLong.of(42L).ifPresent(v -> result[0] = v);
        assertEquals(42L, result[0]);

        result[0] = 0;
        OptionalLong.empty().ifPresent(v -> result[0] = v);
        assertEquals(0L, result[0]);
    }

    @Test
    public void testOptionalLong_ifPresentOrElse() throws Exception {
        long[] result = new long[1];
        boolean[] emptyCalled = new boolean[1];

        OptionalLong.of(42L).ifPresentOrElse(v -> result[0] = v, () -> emptyCalled[0] = true);
        assertEquals(42L, result[0]);
        assertFalse(emptyCalled[0]);

        OptionalLong.empty().ifPresentOrElse(v -> result[0] = 99L, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
    }

    @Test
    public void testOptionalLong_orElseGet() {
        assertEquals(42L, OptionalLong.of(42L).orElseGet(() -> 100L));
        assertEquals(100L, OptionalLong.empty().orElseGet(() -> 100L));
    }

    @Test
    public void testOptionalLong_toImmutableList() {
        ImmutableList<Long> list = OptionalLong.of(42L).toImmutableList();
        assertEquals(1, list.size());
        assertEquals(42L, (long) list.get(0));
        assertTrue(OptionalLong.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalLong_toImmutableSet() {
        ImmutableSet<Long> set = OptionalLong.of(42L).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42L));
        assertTrue(OptionalLong.empty().toImmutableSet().isEmpty());
    }

    // --- OptionalDouble: additional missing ---

    @Test
    public void testOptionalDouble_mapToLong() throws Exception {
        OptionalDouble opt = OptionalDouble.of(42.5);
        OptionalLong mapped = opt.mapToLong(v -> v.longValue());
        assertTrue(mapped.isPresent());
        assertEquals(42L, mapped.get());

        assertFalse(OptionalDouble.empty().mapToLong(v -> 1L).isPresent());
    }

    @Test
    public void testOptionalDouble_toImmutableList() {
        ImmutableList<Double> list = OptionalDouble.of(42.5).toImmutableList();
        assertEquals(1, list.size());
        assertEquals(42.5, list.get(0));
        assertTrue(OptionalDouble.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptionalDouble_toImmutableSet() {
        ImmutableSet<Double> set = OptionalDouble.of(42.5).toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains(42.5));
        assertTrue(OptionalDouble.empty().toImmutableSet().isEmpty());
    }

    // --- Optional<T>: additional missing ---

    @Test
    public void testOptional_or() {
        Optional<String> present = Optional.of("first");
        Optional<String> result = present.or(() -> Optional.of("second"));
        assertTrue(result.isPresent());
        assertEquals("first", result.get());

        Optional<String> empty = Optional.empty();
        Optional<String> result2 = empty.or(() -> Optional.of("second"));
        assertTrue(result2.isPresent());
        assertEquals("second", result2.get());
    }

    @Test
    public void testOptional_toImmutableList() {
        ImmutableList<String> list = Optional.of("test").toImmutableList();
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
        assertTrue(Optional.empty().toImmutableList().isEmpty());
    }

    @Test
    public void testOptional_toImmutableSet() {
        ImmutableSet<String> set = Optional.of("test").toImmutableSet();
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
        assertTrue(Optional.empty().toImmutableSet().isEmpty());
    }

    @Test
    public void testOptional_mapToFloat() throws Exception {
        Optional<String> opt = Optional.of("test");
        OptionalFloat mapped = opt.mapToFloat(s -> (float) s.length());
        assertTrue(mapped.isPresent());
        assertEquals(4.0f, mapped.get());

        assertFalse(Optional.<String> empty().mapToFloat(s -> 1.0f).isPresent());
    }

    // --- Nullable<T>: additional missing ---

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
        u.Optional<String> nullOptional = null;
        u.Nullable<String> result = u.Nullable.from(nullOptional);
        assertFalse(result.isPresent());
    }

    @Test
    public void testNullable_from_Optional_NonNull() {
        u.Optional<String> opt = u.Optional.of("value");
        u.Nullable<String> result = u.Nullable.from(opt);
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

}
