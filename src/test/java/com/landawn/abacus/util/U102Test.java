package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class U102Test extends TestBase {

    @Nested
    @DisplayName("OptionalBooleanTest")
    public class OptionalBooleanTest {

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
        public void testIfPresent() throws Exception {
            AtomicBoolean consumed = new AtomicBoolean(false);
            u.OptionalBoolean optional = u.OptionalBoolean.of(true);

            optional.ifPresent(value -> consumed.set(value));
            assertTrue(consumed.get());

            consumed.set(false);
            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            empty.ifPresent(value -> consumed.set(true));
            assertFalse(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicBoolean consumed = new AtomicBoolean(false);
            AtomicBoolean emptyCalled = new AtomicBoolean(false);

            u.OptionalBoolean optional = u.OptionalBoolean.of(true);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertTrue(consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(false);
            emptyCalled.set(false);
            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            empty.ifPresentOrElse(value -> consumed.set(true), () -> emptyCalled.set(true));
            assertFalse(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalBoolean optional = u.OptionalBoolean.of(true);

            u.OptionalBoolean filtered = optional.filter(value -> value);
            assertTrue(filtered.isPresent());
            assertTrue(filtered.get());

            filtered = optional.filter(value -> !value);
            assertFalse(filtered.isPresent());

            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalBoolean optional = u.OptionalBoolean.of(true);

            u.OptionalBoolean mapped = optional.map(value -> !value);
            assertTrue(mapped.isPresent());
            assertFalse(mapped.get());

            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            mapped = empty.map(value -> !value);
            assertFalse(mapped.isPresent());
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
        public void testOrElseThrow() {
            u.OptionalBoolean optional = u.OptionalBoolean.of(true);
            assertTrue(optional.orElseThrow());

            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
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
        public void testEquals() {
            u.OptionalBoolean true1 = u.OptionalBoolean.of(true);
            u.OptionalBoolean true2 = u.OptionalBoolean.of(true);
            u.OptionalBoolean false1 = u.OptionalBoolean.of(false);
            u.OptionalBoolean empty1 = u.OptionalBoolean.empty();
            u.OptionalBoolean empty2 = u.OptionalBoolean.empty();

            assertEquals(true1, true1);
            assertEquals(true1, true2);
            assertNotEquals(true1, false1);
            assertNotEquals(true1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(true1, null);
            assertNotEquals(true1, "string");
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
        public void testToString() {
            u.OptionalBoolean optional = u.OptionalBoolean.of(true);
            assertEquals("OptionalBoolean[true]", optional.toString());

            optional = u.OptionalBoolean.of(false);
            assertEquals("OptionalBoolean[false]", optional.toString());

            u.OptionalBoolean empty = u.OptionalBoolean.empty();
            assertEquals("OptionalBoolean.empty", empty.toString());
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
    }

    @Nested
    @DisplayName("OptionalCharTest")
    public class OptionalCharTest {

        @Test
        public void testEmpty() {
            u.OptionalChar optional = u.OptionalChar.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertTrue(optional.isPresent());
            assertEquals('A', optional.get());

            u.OptionalChar cached1 = u.OptionalChar.of((char) 0);
            u.OptionalChar cached2 = u.OptionalChar.of((char) 0);
            assertSame(cached1, cached2);

            u.OptionalChar cached3 = u.OptionalChar.of((char) 128);
            u.OptionalChar cached4 = u.OptionalChar.of((char) 128);
            assertSame(cached3, cached4);

            u.OptionalChar nonCached1 = u.OptionalChar.of((char) 129);
            u.OptionalChar nonCached2 = u.OptionalChar.of((char) 129);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalChar optional = u.OptionalChar.ofNullable('A');
            assertTrue(optional.isPresent());
            assertEquals('A', optional.get());

            optional = u.OptionalChar.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.get());

            u.OptionalChar empty = u.OptionalChar.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalChar.of('A').isPresent());
            assertFalse(u.OptionalChar.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalChar.of('A').isEmpty());
            assertTrue(u.OptionalChar.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Character> consumed = new AtomicReference<>();
            u.OptionalChar optional = u.OptionalChar.of('A');

            optional.ifPresent(value -> consumed.set(value));
            assertEquals('A', consumed.get());

            consumed.set(null);
            u.OptionalChar empty = u.OptionalChar.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Character> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalChar optional = u.OptionalChar.of('A');
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals('A', consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalChar empty = u.OptionalChar.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalChar optional = u.OptionalChar.of('A');

            u.OptionalChar filtered = optional.filter(value -> value == 'A');
            assertTrue(filtered.isPresent());
            assertEquals('A', filtered.get());

            filtered = optional.filter(value -> value == 'B');
            assertFalse(filtered.isPresent());

            u.OptionalChar empty = u.OptionalChar.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalChar optional = u.OptionalChar.of('A');

            u.OptionalChar mapped = optional.map(value -> (char) (value + 1));
            assertTrue(mapped.isPresent());
            assertEquals('B', mapped.get());

            u.OptionalChar empty = u.OptionalChar.empty();
            mapped = empty.map(value -> 'X');
            assertFalse(mapped.isPresent());
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
        public void testMapToInt() throws Exception {
            u.OptionalChar optional = u.OptionalChar.of('A');

            u.OptionalInt mapped = optional.mapToInt(value -> (int) value);
            assertTrue(mapped.isPresent());
            assertEquals(65, mapped.get());

            u.OptionalChar empty = u.OptionalChar.empty();
            mapped = empty.mapToInt(value -> 99);
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
        public void testFlatMap() throws Exception {
            u.OptionalChar optional = u.OptionalChar.of('A');

            u.OptionalChar mapped = optional.flatMap(value -> u.OptionalChar.of((char) (value + 1)));
            assertTrue(mapped.isPresent());
            assertEquals('B', mapped.get());

            mapped = optional.flatMap(value -> u.OptionalChar.empty());
            assertFalse(mapped.isPresent());

            u.OptionalChar empty = u.OptionalChar.empty();
            mapped = empty.flatMap(value -> u.OptionalChar.of('X'));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            u.OptionalChar result = optional.or(() -> u.OptionalChar.of('B'));
            assertTrue(result.isPresent());
            assertEquals('A', result.get());

            u.OptionalChar empty = u.OptionalChar.empty();
            result = empty.or(() -> u.OptionalChar.of('B'));
            assertTrue(result.isPresent());
            assertEquals('B', result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElseZero());

            u.OptionalChar empty = u.OptionalChar.empty();
            assertEquals((char) 0, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElse('B'));

            u.OptionalChar empty = u.OptionalChar.empty();
            assertEquals('B', empty.orElse('B'));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElseGet(() -> 'B'));

            u.OptionalChar empty = u.OptionalChar.empty();
            assertEquals('B', empty.orElseGet(() -> 'B'));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElseThrow());

            u.OptionalChar empty = u.OptionalChar.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElseThrow("Error"));

            u.OptionalChar empty = u.OptionalChar.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
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
        public void testOrElseThrowWithSupplier() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals('A', optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalChar empty = u.OptionalChar.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalChar empty = u.OptionalChar.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            List<Character> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals('A', list.get(0));

            u.OptionalChar empty = u.OptionalChar.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            Set<Character> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains('A'));

            u.OptionalChar empty = u.OptionalChar.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            ImmutableList<Character> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals('A', list.get(0));

            u.OptionalChar empty = u.OptionalChar.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            ImmutableSet<Character> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains('A'));

            u.OptionalChar empty = u.OptionalChar.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            Optional<Character> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals('A', boxed.get());

            u.OptionalChar empty = u.OptionalChar.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalChar a1 = u.OptionalChar.of('A');
            u.OptionalChar a2 = u.OptionalChar.of('A');
            u.OptionalChar b = u.OptionalChar.of('B');
            u.OptionalChar empty = u.OptionalChar.empty();

            assertEquals(0, a1.compareTo(a2));
            assertTrue(a1.compareTo(b) < 0);
            assertTrue(b.compareTo(a1) > 0);
            assertTrue(a1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(a1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalChar.empty()));
            assertTrue(a1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalChar a1 = u.OptionalChar.of('A');
            u.OptionalChar a2 = u.OptionalChar.of('A');
            u.OptionalChar b = u.OptionalChar.of('B');
            u.OptionalChar empty1 = u.OptionalChar.empty();
            u.OptionalChar empty2 = u.OptionalChar.empty();

            assertEquals(a1, a1);
            assertEquals(a1, a2);
            assertNotEquals(a1, b);
            assertNotEquals(a1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(a1, null);
            assertNotEquals(a1, "string");
        }

        @Test
        public void testHashCode() {
            u.OptionalChar a1 = u.OptionalChar.of('A');
            u.OptionalChar a2 = u.OptionalChar.of('A');
            u.OptionalChar b = u.OptionalChar.of('B');
            u.OptionalChar empty = u.OptionalChar.empty();

            assertEquals(a1.hashCode(), a2.hashCode());
            assertNotEquals(a1.hashCode(), b.hashCode());
            assertNotEquals(a1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalChar optional = u.OptionalChar.of('A');
            assertEquals("OptionalChar[A]", optional.toString());

            u.OptionalChar empty = u.OptionalChar.empty();
            assertEquals("OptionalChar.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalByteTest")
    public class OptionalByteTest {

        @Test
        public void testEmpty() {
            u.OptionalByte optional = u.OptionalByte.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertTrue(optional.isPresent());
            assertEquals((byte) 42, optional.get());

            u.OptionalByte cached1 = u.OptionalByte.of(Byte.MIN_VALUE);
            u.OptionalByte cached2 = u.OptionalByte.of(Byte.MIN_VALUE);
            assertSame(cached1, cached2);

            u.OptionalByte cached3 = u.OptionalByte.of(Byte.MAX_VALUE);
            u.OptionalByte cached4 = u.OptionalByte.of(Byte.MAX_VALUE);
            assertSame(cached3, cached4);

            for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
                byte b = (byte) i;
                assertSame(u.OptionalByte.of(b), u.OptionalByte.of(b));
            }
        }

        @Test
        public void testOfNullable() {
            u.OptionalByte optional = u.OptionalByte.ofNullable((byte) 42);
            assertTrue(optional.isPresent());
            assertEquals((byte) 42, optional.get());

            optional = u.OptionalByte.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalByte.of((byte) 42).isPresent());
            assertFalse(u.OptionalByte.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalByte.of((byte) 42).isEmpty());
            assertTrue(u.OptionalByte.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Byte> consumed = new AtomicReference<>();
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals((byte) 42, consumed.get());

            consumed.set(null);
            u.OptionalByte empty = u.OptionalByte.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Byte> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals((byte) 42, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalByte empty = u.OptionalByte.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            u.OptionalByte filtered = optional.filter(value -> value == 42);
            assertTrue(filtered.isPresent());
            assertEquals((byte) 42, filtered.get());

            filtered = optional.filter(value -> value == 0);
            assertFalse(filtered.isPresent());

            u.OptionalByte empty = u.OptionalByte.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            u.OptionalByte mapped = optional.map(value -> (byte) (value + 1));
            assertTrue(mapped.isPresent());
            assertEquals((byte) 43, mapped.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            mapped = empty.map(value -> (byte) 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            u.OptionalInt mapped = optional.mapToInt(value -> value * 2);
            assertTrue(mapped.isPresent());
            assertEquals(84, mapped.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            mapped = empty.mapToInt(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42", mapped.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);

            u.OptionalByte mapped = optional.flatMap(value -> u.OptionalByte.of((byte) (value + 1)));
            assertTrue(mapped.isPresent());
            assertEquals((byte) 43, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalByte.empty());
            assertFalse(mapped.isPresent());

            u.OptionalByte empty = u.OptionalByte.empty();
            mapped = empty.flatMap(value -> u.OptionalByte.of((byte) 99));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            u.OptionalByte result = optional.or(() -> u.OptionalByte.of((byte) 99));
            assertTrue(result.isPresent());
            assertEquals((byte) 42, result.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            result = empty.or(() -> u.OptionalByte.of((byte) 99));
            assertTrue(result.isPresent());
            assertEquals((byte) 99, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseZero());

            u.OptionalByte empty = u.OptionalByte.empty();
            assertEquals((byte) 0, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElse((byte) 99));

            u.OptionalByte empty = u.OptionalByte.empty();
            assertEquals((byte) 99, empty.orElse((byte) 99));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseGet(() -> (byte) 99));

            u.OptionalByte empty = u.OptionalByte.empty();
            assertEquals((byte) 99, empty.orElseGet(() -> (byte) 99));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseThrow());

            u.OptionalByte empty = u.OptionalByte.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseThrow("Error"));

            u.OptionalByte empty = u.OptionalByte.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseThrow("Error %s", "param"));
            assertEquals((byte) 42, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals((byte) 42, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals((byte) 42, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalByte empty = u.OptionalByte.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals((byte) 42, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalByte empty = u.OptionalByte.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalByte empty = u.OptionalByte.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            List<Byte> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals((byte) 42, list.get(0));

            u.OptionalByte empty = u.OptionalByte.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            Set<Byte> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains((byte) 42));

            u.OptionalByte empty = u.OptionalByte.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            ImmutableList<Byte> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals((byte) 42, list.get(0));

            u.OptionalByte empty = u.OptionalByte.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            ImmutableSet<Byte> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains((byte) 42));

            u.OptionalByte empty = u.OptionalByte.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            Optional<Byte> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals((byte) 42, boxed.get());

            u.OptionalByte empty = u.OptionalByte.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalByte val1 = u.OptionalByte.of((byte) 10);
            u.OptionalByte val2 = u.OptionalByte.of((byte) 10);
            u.OptionalByte val3 = u.OptionalByte.of((byte) 20);
            u.OptionalByte empty = u.OptionalByte.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalByte.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalByte val1 = u.OptionalByte.of((byte) 42);
            u.OptionalByte val2 = u.OptionalByte.of((byte) 42);
            u.OptionalByte val3 = u.OptionalByte.of((byte) 99);
            u.OptionalByte empty1 = u.OptionalByte.empty();
            u.OptionalByte empty2 = u.OptionalByte.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");
        }

        @Test
        public void testHashCode() {
            u.OptionalByte val1 = u.OptionalByte.of((byte) 42);
            u.OptionalByte val2 = u.OptionalByte.of((byte) 42);
            u.OptionalByte val3 = u.OptionalByte.of((byte) 99);
            u.OptionalByte empty = u.OptionalByte.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalByte optional = u.OptionalByte.of((byte) 42);
            assertEquals("OptionalByte[42]", optional.toString());

            u.OptionalByte empty = u.OptionalByte.empty();
            assertEquals("OptionalByte.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalShortTest")
    public class OptionalShortTest {

        @Test
        public void testEmpty() {
            u.OptionalShort optional = u.OptionalShort.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertTrue(optional.isPresent());
            assertEquals((short) 42, optional.get());

            u.OptionalShort cached1 = u.OptionalShort.of((short) -128);
            u.OptionalShort cached2 = u.OptionalShort.of((short) -128);
            assertSame(cached1, cached2);

            u.OptionalShort cached3 = u.OptionalShort.of((short) 256);
            u.OptionalShort cached4 = u.OptionalShort.of((short) 256);
            assertSame(cached3, cached4);

            u.OptionalShort nonCached1 = u.OptionalShort.of((short) 257);
            u.OptionalShort nonCached2 = u.OptionalShort.of((short) 257);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalShort optional = u.OptionalShort.ofNullable((short) 42);
            assertTrue(optional.isPresent());
            assertEquals((short) 42, optional.get());

            optional = u.OptionalShort.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalShort.of((short) 42).isPresent());
            assertFalse(u.OptionalShort.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalShort.of((short) 42).isEmpty());
            assertTrue(u.OptionalShort.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Short> consumed = new AtomicReference<>();
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals((short) 42, consumed.get());

            consumed.set(null);
            u.OptionalShort empty = u.OptionalShort.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Short> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals((short) 42, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalShort empty = u.OptionalShort.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            u.OptionalShort filtered = optional.filter(value -> value == 42);
            assertTrue(filtered.isPresent());
            assertEquals((short) 42, filtered.get());

            filtered = optional.filter(value -> value == 0);
            assertFalse(filtered.isPresent());

            u.OptionalShort empty = u.OptionalShort.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            u.OptionalShort mapped = optional.map(value -> (short) (value + 1));
            assertTrue(mapped.isPresent());
            assertEquals((short) 43, mapped.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            mapped = empty.map(value -> (short) 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            u.OptionalInt mapped = optional.mapToInt(value -> value * 2);
            assertTrue(mapped.isPresent());
            assertEquals(84, mapped.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            mapped = empty.mapToInt(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42", mapped.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);

            u.OptionalShort mapped = optional.flatMap(value -> u.OptionalShort.of((short) (value + 1)));
            assertTrue(mapped.isPresent());
            assertEquals((short) 43, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalShort.empty());
            assertFalse(mapped.isPresent());

            u.OptionalShort empty = u.OptionalShort.empty();
            mapped = empty.flatMap(value -> u.OptionalShort.of((short) 99));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            u.OptionalShort result = optional.or(() -> u.OptionalShort.of((short) 99));
            assertTrue(result.isPresent());
            assertEquals((short) 42, result.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            result = empty.or(() -> u.OptionalShort.of((short) 99));
            assertTrue(result.isPresent());
            assertEquals((short) 99, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseZero());

            u.OptionalShort empty = u.OptionalShort.empty();
            assertEquals((short) 0, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElse((short) 99));

            u.OptionalShort empty = u.OptionalShort.empty();
            assertEquals((short) 99, empty.orElse((short) 99));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseGet(() -> (short) 99));

            u.OptionalShort empty = u.OptionalShort.empty();
            assertEquals((short) 99, empty.orElseGet(() -> (short) 99));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseThrow());

            u.OptionalShort empty = u.OptionalShort.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseThrow("Error"));

            u.OptionalShort empty = u.OptionalShort.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseThrow("Error %s", "param"));
            assertEquals((short) 42, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals((short) 42, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals((short) 42, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalShort empty = u.OptionalShort.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals((short) 42, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalShort empty = u.OptionalShort.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalShort empty = u.OptionalShort.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            List<Short> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals((short) 42, list.get(0));

            u.OptionalShort empty = u.OptionalShort.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            Set<Short> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains((short) 42));

            u.OptionalShort empty = u.OptionalShort.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            ImmutableList<Short> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals((short) 42, list.get(0));

            u.OptionalShort empty = u.OptionalShort.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            ImmutableSet<Short> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains((short) 42));

            u.OptionalShort empty = u.OptionalShort.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            Optional<Short> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals((short) 42, boxed.get());

            u.OptionalShort empty = u.OptionalShort.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalShort val1 = u.OptionalShort.of((short) 10);
            u.OptionalShort val2 = u.OptionalShort.of((short) 10);
            u.OptionalShort val3 = u.OptionalShort.of((short) 20);
            u.OptionalShort empty = u.OptionalShort.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalShort.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalShort val1 = u.OptionalShort.of((short) 42);
            u.OptionalShort val2 = u.OptionalShort.of((short) 42);
            u.OptionalShort val3 = u.OptionalShort.of((short) 99);
            u.OptionalShort empty1 = u.OptionalShort.empty();
            u.OptionalShort empty2 = u.OptionalShort.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");
        }

        @Test
        public void testHashCode() {
            u.OptionalShort val1 = u.OptionalShort.of((short) 42);
            u.OptionalShort val2 = u.OptionalShort.of((short) 42);
            u.OptionalShort val3 = u.OptionalShort.of((short) 99);
            u.OptionalShort empty = u.OptionalShort.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalShort optional = u.OptionalShort.of((short) 42);
            assertEquals("OptionalShort[42]", optional.toString());

            u.OptionalShort empty = u.OptionalShort.empty();
            assertEquals("OptionalShort.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalIntTest")
    public class OptionalIntTest {

        @Test
        public void testEmpty() {
            u.OptionalInt optional = u.OptionalInt.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertTrue(optional.isPresent());
            assertEquals(42, optional.get());

            u.OptionalInt cached1 = u.OptionalInt.of(-256);
            u.OptionalInt cached2 = u.OptionalInt.of(-256);
            assertSame(cached1, cached2);

            u.OptionalInt cached3 = u.OptionalInt.of(1024);
            u.OptionalInt cached4 = u.OptionalInt.of(1024);
            assertSame(cached3, cached4);

            u.OptionalInt nonCached1 = u.OptionalInt.of(1025);
            u.OptionalInt nonCached2 = u.OptionalInt.of(1025);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalInt optional = u.OptionalInt.ofNullable(42);
            assertTrue(optional.isPresent());
            assertEquals(42, optional.get());

            optional = u.OptionalInt.ofNullable(null);
            assertFalse(optional.isPresent());
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
        public void testGet() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testGetAsInt() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.getAsInt());

            u.OptionalInt empty = u.OptionalInt.empty();
            assertThrows(NoSuchElementException.class, () -> empty.getAsInt());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalInt.of(42).isPresent());
            assertFalse(u.OptionalInt.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalInt.of(42).isEmpty());
            assertTrue(u.OptionalInt.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Integer> consumed = new AtomicReference<>();
            u.OptionalInt optional = u.OptionalInt.of(42);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals(42, consumed.get());

            consumed.set(null);
            u.OptionalInt empty = u.OptionalInt.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Integer> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalInt optional = u.OptionalInt.of(42);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals(42, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalInt empty = u.OptionalInt.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalInt filtered = optional.filter(value -> value == 42);
            assertTrue(filtered.isPresent());
            assertEquals(42, filtered.get());

            filtered = optional.filter(value -> value == 0);
            assertFalse(filtered.isPresent());

            u.OptionalInt empty = u.OptionalInt.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalInt mapped = optional.map(value -> value + 1);
            assertTrue(mapped.isPresent());
            assertEquals(43, mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.map(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToBoolean() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalBoolean mapped = optional.mapToBoolean(value -> value > 0);
            assertTrue(mapped.isPresent());
            assertTrue(mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.mapToBoolean(value -> true);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToChar() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(65);

            u.OptionalChar mapped = optional.mapToChar(value -> (char) value.intValue());
            assertTrue(mapped.isPresent());
            assertEquals('A', mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.mapToChar(value -> 'X');
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToLong() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalLong mapped = optional.mapToLong(value -> value * 1000L);
            assertTrue(mapped.isPresent());
            assertEquals(42000L, mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.mapToLong(value -> 99L);
            assertFalse(mapped.isPresent());
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
        public void testMapToDouble() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalDouble mapped = optional.mapToDouble(value -> value / 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(21.0, mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.mapToDouble(value -> 99.0);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42", mapped.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalInt optional = u.OptionalInt.of(42);

            u.OptionalInt mapped = optional.flatMap(value -> u.OptionalInt.of(value + 1));
            assertTrue(mapped.isPresent());
            assertEquals(43, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalInt.empty());
            assertFalse(mapped.isPresent());

            u.OptionalInt empty = u.OptionalInt.empty();
            mapped = empty.flatMap(value -> u.OptionalInt.of(99));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            u.OptionalInt result = optional.or(() -> u.OptionalInt.of(99));
            assertTrue(result.isPresent());
            assertEquals(42, result.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            result = empty.or(() -> u.OptionalInt.of(99));
            assertTrue(result.isPresent());
            assertEquals(99, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseZero());

            u.OptionalInt empty = u.OptionalInt.empty();
            assertEquals(0, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElse(99));

            u.OptionalInt empty = u.OptionalInt.empty();
            assertEquals(99, empty.orElse(99));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseGet(() -> 99));

            u.OptionalInt empty = u.OptionalInt.empty();
            assertEquals(99, empty.orElseGet(() -> 99));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseThrow());

            u.OptionalInt empty = u.OptionalInt.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseThrow("Error"));

            u.OptionalInt empty = u.OptionalInt.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseThrow("Error %s", "param"));
            assertEquals(42, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals(42, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals(42, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalInt empty = u.OptionalInt.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals(42, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalInt empty = u.OptionalInt.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalInt empty = u.OptionalInt.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            List<Integer> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals(42, list.get(0));

            u.OptionalInt empty = u.OptionalInt.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            Set<Integer> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42));

            u.OptionalInt empty = u.OptionalInt.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            ImmutableList<Integer> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals(42, list.get(0));

            u.OptionalInt empty = u.OptionalInt.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            ImmutableSet<Integer> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42));

            u.OptionalInt empty = u.OptionalInt.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            Optional<Integer> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42, boxed.get());

            u.OptionalInt empty = u.OptionalInt.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
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

        @Test
        public void testDeprecatedDunderscoreMethod() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            java.util.OptionalInt jdkOptional = optional.__();
            assertTrue(jdkOptional.isPresent());
            assertEquals(42, jdkOptional.getAsInt());

            u.OptionalInt empty = u.OptionalInt.empty();
            jdkOptional = empty.__();
            assertFalse(jdkOptional.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalInt val1 = u.OptionalInt.of(10);
            u.OptionalInt val2 = u.OptionalInt.of(10);
            u.OptionalInt val3 = u.OptionalInt.of(20);
            u.OptionalInt empty = u.OptionalInt.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalInt.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalInt val1 = u.OptionalInt.of(42);
            u.OptionalInt val2 = u.OptionalInt.of(42);
            u.OptionalInt val3 = u.OptionalInt.of(99);
            u.OptionalInt empty1 = u.OptionalInt.empty();
            u.OptionalInt empty2 = u.OptionalInt.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");
        }

        @Test
        public void testHashCode() {
            u.OptionalInt val1 = u.OptionalInt.of(42);
            u.OptionalInt val2 = u.OptionalInt.of(42);
            u.OptionalInt val3 = u.OptionalInt.of(99);
            u.OptionalInt empty = u.OptionalInt.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalInt optional = u.OptionalInt.of(42);
            assertEquals("OptionalInt[42]", optional.toString());

            u.OptionalInt empty = u.OptionalInt.empty();
            assertEquals("OptionalInt.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalLongTest")
    public class OptionalLongTest {

        @Test
        public void testEmpty() {
            u.OptionalLong optional = u.OptionalLong.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertTrue(optional.isPresent());
            assertEquals(42L, optional.get());

            u.OptionalLong cached1 = u.OptionalLong.of(-256L);
            u.OptionalLong cached2 = u.OptionalLong.of(-256L);
            assertSame(cached1, cached2);

            u.OptionalLong cached3 = u.OptionalLong.of(1024L);
            u.OptionalLong cached4 = u.OptionalLong.of(1024L);
            assertSame(cached3, cached4);

            u.OptionalLong nonCached1 = u.OptionalLong.of(1025L);
            u.OptionalLong nonCached2 = u.OptionalLong.of(1025L);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalLong optional = u.OptionalLong.ofNullable(42L);
            assertTrue(optional.isPresent());
            assertEquals(42L, optional.get());

            optional = u.OptionalLong.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testFrom() {
            java.util.OptionalLong jdkOptional = java.util.OptionalLong.of(42L);
            u.OptionalLong optional = u.OptionalLong.from(jdkOptional);
            assertTrue(optional.isPresent());
            assertEquals(42L, optional.get());

            jdkOptional = java.util.OptionalLong.empty();
            optional = u.OptionalLong.from(jdkOptional);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testGetAsLong() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.getAsLong());

            u.OptionalLong empty = u.OptionalLong.empty();
            assertThrows(NoSuchElementException.class, () -> empty.getAsLong());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalLong.of(42L).isPresent());
            assertFalse(u.OptionalLong.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalLong.of(42L).isEmpty());
            assertTrue(u.OptionalLong.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Long> consumed = new AtomicReference<>();
            u.OptionalLong optional = u.OptionalLong.of(42L);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals(42L, consumed.get());

            consumed.set(null);
            u.OptionalLong empty = u.OptionalLong.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Long> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalLong optional = u.OptionalLong.of(42L);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals(42L, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalLong empty = u.OptionalLong.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            u.OptionalLong filtered = optional.filter(value -> value == 42L);
            assertTrue(filtered.isPresent());
            assertEquals(42L, filtered.get());

            filtered = optional.filter(value -> value == 0L);
            assertFalse(filtered.isPresent());

            u.OptionalLong empty = u.OptionalLong.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            u.OptionalLong mapped = optional.map(value -> value + 1L);
            assertTrue(mapped.isPresent());
            assertEquals(43L, mapped.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            mapped = empty.map(value -> 99L);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            u.OptionalInt mapped = optional.mapToInt(value -> value.intValue());
            assertTrue(mapped.isPresent());
            assertEquals(42, mapped.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            mapped = empty.mapToInt(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToDouble() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            u.OptionalDouble mapped = optional.mapToDouble(value -> value / 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(21.0, mapped.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            mapped = empty.mapToDouble(value -> 99.0);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42", mapped.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalLong optional = u.OptionalLong.of(42L);

            u.OptionalLong mapped = optional.flatMap(value -> u.OptionalLong.of(value + 1L));
            assertTrue(mapped.isPresent());
            assertEquals(43L, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalLong.empty());
            assertFalse(mapped.isPresent());

            u.OptionalLong empty = u.OptionalLong.empty();
            mapped = empty.flatMap(value -> u.OptionalLong.of(99L));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            u.OptionalLong result = optional.or(() -> u.OptionalLong.of(99L));
            assertTrue(result.isPresent());
            assertEquals(42L, result.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            result = empty.or(() -> u.OptionalLong.of(99L));
            assertTrue(result.isPresent());
            assertEquals(99L, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseZero());

            u.OptionalLong empty = u.OptionalLong.empty();
            assertEquals(0L, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElse(99L));

            u.OptionalLong empty = u.OptionalLong.empty();
            assertEquals(99L, empty.orElse(99L));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseGet(() -> 99L));

            u.OptionalLong empty = u.OptionalLong.empty();
            assertEquals(99L, empty.orElseGet(() -> 99L));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseThrow());

            u.OptionalLong empty = u.OptionalLong.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseThrow("Error"));

            u.OptionalLong empty = u.OptionalLong.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseThrow("Error %s", "param"));
            assertEquals(42L, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals(42L, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals(42L, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalLong empty = u.OptionalLong.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals(42L, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalLong empty = u.OptionalLong.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalLong empty = u.OptionalLong.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            List<Long> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals(42L, list.get(0));

            u.OptionalLong empty = u.OptionalLong.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            Set<Long> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42L));

            u.OptionalLong empty = u.OptionalLong.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            ImmutableList<Long> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals(42L, list.get(0));

            u.OptionalLong empty = u.OptionalLong.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            ImmutableSet<Long> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42L));

            u.OptionalLong empty = u.OptionalLong.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            Optional<Long> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42L, boxed.get());

            u.OptionalLong empty = u.OptionalLong.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            java.util.OptionalLong jdkOptional = optional.toJdkOptional();
            assertTrue(jdkOptional.isPresent());
            assertEquals(42L, jdkOptional.getAsLong());

            u.OptionalLong empty = u.OptionalLong.empty();
            jdkOptional = empty.toJdkOptional();
            assertFalse(jdkOptional.isPresent());
        }

        @Test
        public void testDeprecatedDunderscoreMethod() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            java.util.OptionalLong jdkOptional = optional.__();
            assertTrue(jdkOptional.isPresent());
            assertEquals(42L, jdkOptional.getAsLong());

            u.OptionalLong empty = u.OptionalLong.empty();
            jdkOptional = empty.__();
            assertFalse(jdkOptional.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalLong val1 = u.OptionalLong.of(10L);
            u.OptionalLong val2 = u.OptionalLong.of(10L);
            u.OptionalLong val3 = u.OptionalLong.of(20L);
            u.OptionalLong empty = u.OptionalLong.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalLong.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalLong val1 = u.OptionalLong.of(42L);
            u.OptionalLong val2 = u.OptionalLong.of(42L);
            u.OptionalLong val3 = u.OptionalLong.of(99L);
            u.OptionalLong empty1 = u.OptionalLong.empty();
            u.OptionalLong empty2 = u.OptionalLong.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");
        }

        @Test
        public void testHashCode() {
            u.OptionalLong val1 = u.OptionalLong.of(42L);
            u.OptionalLong val2 = u.OptionalLong.of(42L);
            u.OptionalLong val3 = u.OptionalLong.of(99L);
            u.OptionalLong empty = u.OptionalLong.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalLong optional = u.OptionalLong.of(42L);
            assertEquals("OptionalLong[42]", optional.toString());

            u.OptionalLong empty = u.OptionalLong.empty();
            assertEquals("OptionalLong.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalFloatTest")
    public class OptionalFloatTest {

        @Test
        public void testEmpty() {
            u.OptionalFloat optional = u.OptionalFloat.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertTrue(optional.isPresent());
            assertEquals(42.5f, optional.get());

            u.OptionalFloat zero1 = u.OptionalFloat.of(0f);
            u.OptionalFloat zero2 = u.OptionalFloat.of(0f);
            assertSame(zero1, zero2);

            u.OptionalFloat nonCached1 = u.OptionalFloat.of(42.5f);
            u.OptionalFloat nonCached2 = u.OptionalFloat.of(42.5f);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalFloat optional = u.OptionalFloat.ofNullable(42.5f);
            assertTrue(optional.isPresent());
            assertEquals(42.5f, optional.get());

            optional = u.OptionalFloat.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalFloat.of(42.5f).isPresent());
            assertFalse(u.OptionalFloat.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalFloat.of(42.5f).isEmpty());
            assertTrue(u.OptionalFloat.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Float> consumed = new AtomicReference<>();
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals(42.5f, consumed.get());

            consumed.set(null);
            u.OptionalFloat empty = u.OptionalFloat.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Float> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals(42.5f, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalFloat empty = u.OptionalFloat.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            u.OptionalFloat filtered = optional.filter(value -> value > 40f);
            assertTrue(filtered.isPresent());
            assertEquals(42.5f, filtered.get());

            filtered = optional.filter(value -> value < 40f);
            assertFalse(filtered.isPresent());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            u.OptionalFloat mapped = optional.map(value -> value * 2f);
            assertTrue(mapped.isPresent());
            assertEquals(85f, mapped.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            mapped = empty.map(value -> 99f);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            u.OptionalInt mapped = optional.mapToInt(value -> value.intValue());
            assertTrue(mapped.isPresent());
            assertEquals(42, mapped.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            mapped = empty.mapToInt(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToDouble() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            u.OptionalDouble mapped = optional.mapToDouble(value -> value * 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(85.0, mapped.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            mapped = empty.mapToDouble(value -> 99.0);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42.5", mapped.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);

            u.OptionalFloat mapped = optional.flatMap(value -> u.OptionalFloat.of(value * 2f));
            assertTrue(mapped.isPresent());
            assertEquals(85f, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalFloat.empty());
            assertFalse(mapped.isPresent());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            mapped = empty.flatMap(value -> u.OptionalFloat.of(99f));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            u.OptionalFloat result = optional.or(() -> u.OptionalFloat.of(99f));
            assertTrue(result.isPresent());
            assertEquals(42.5f, result.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            result = empty.or(() -> u.OptionalFloat.of(99f));
            assertTrue(result.isPresent());
            assertEquals(99f, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseZero());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertEquals(0f, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElse(99f));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertEquals(99f, empty.orElse(99f));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseGet(() -> 99f));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertEquals(99f, empty.orElseGet(() -> 99f));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseThrow());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseThrow("Error"));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseThrow("Error %s", "param"));
            assertEquals(42.5f, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals(42.5f, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals(42.5f, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals(42.5f, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalFloat empty = u.OptionalFloat.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            List<Float> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals(42.5f, list.get(0));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            Set<Float> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42.5f));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            ImmutableList<Float> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals(42.5f, list.get(0));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            ImmutableSet<Float> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42.5f));

            u.OptionalFloat empty = u.OptionalFloat.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            Optional<Float> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42.5f, boxed.get());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalFloat val1 = u.OptionalFloat.of(10.5f);
            u.OptionalFloat val2 = u.OptionalFloat.of(10.5f);
            u.OptionalFloat val3 = u.OptionalFloat.of(20.5f);
            u.OptionalFloat empty = u.OptionalFloat.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalFloat.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalFloat val1 = u.OptionalFloat.of(42.5f);
            u.OptionalFloat val2 = u.OptionalFloat.of(42.5f);
            u.OptionalFloat val3 = u.OptionalFloat.of(99.5f);
            u.OptionalFloat empty1 = u.OptionalFloat.empty();
            u.OptionalFloat empty2 = u.OptionalFloat.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");

            u.OptionalFloat nan1 = u.OptionalFloat.of(Float.NaN);
            u.OptionalFloat nan2 = u.OptionalFloat.of(Float.NaN);
            assertEquals(nan1, nan2);
        }

        @Test
        public void testHashCode() {
            u.OptionalFloat val1 = u.OptionalFloat.of(42.5f);
            u.OptionalFloat val2 = u.OptionalFloat.of(42.5f);
            u.OptionalFloat val3 = u.OptionalFloat.of(99.5f);
            u.OptionalFloat empty = u.OptionalFloat.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalFloat optional = u.OptionalFloat.of(42.5f);
            assertEquals("OptionalFloat[42.5]", optional.toString());

            u.OptionalFloat empty = u.OptionalFloat.empty();
            assertEquals("OptionalFloat.empty", empty.toString());
        }
    }

    @Nested
    @DisplayName("OptionalDoubleTest")
    public class OptionalDoubleTest {

        @Test
        public void testEmpty() {
            u.OptionalDouble optional = u.OptionalDouble.empty();
            assertFalse(optional.isPresent());
            assertTrue(optional.isEmpty());
        }

        @Test
        public void testOf() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertTrue(optional.isPresent());
            assertEquals(42.5, optional.get());

            u.OptionalDouble zero1 = u.OptionalDouble.of(0d);
            u.OptionalDouble zero2 = u.OptionalDouble.of(0d);
            assertSame(zero1, zero2);

            u.OptionalDouble nonCached1 = u.OptionalDouble.of(42.5);
            u.OptionalDouble nonCached2 = u.OptionalDouble.of(42.5);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);
        }

        @Test
        public void testOfNullable() {
            u.OptionalDouble optional = u.OptionalDouble.ofNullable(42.5);
            assertTrue(optional.isPresent());
            assertEquals(42.5, optional.get());

            optional = u.OptionalDouble.ofNullable(null);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testFrom() {
            java.util.OptionalDouble jdkOptional = java.util.OptionalDouble.of(42.5);
            u.OptionalDouble optional = u.OptionalDouble.from(jdkOptional);
            assertTrue(optional.isPresent());
            assertEquals(42.5, optional.get());

            jdkOptional = java.util.OptionalDouble.empty();
            optional = u.OptionalDouble.from(jdkOptional);
            assertFalse(optional.isPresent());
        }

        @Test
        public void testGet() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testGetAsDouble() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.getAsDouble());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertThrows(NoSuchElementException.class, () -> empty.getAsDouble());
        }

        @Test
        public void testIsPresent() {
            assertTrue(u.OptionalDouble.of(42.5).isPresent());
            assertFalse(u.OptionalDouble.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(u.OptionalDouble.of(42.5).isEmpty());
            assertTrue(u.OptionalDouble.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Double> consumed = new AtomicReference<>();
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            optional.ifPresent(value -> consumed.set(value));
            assertEquals(42.5, consumed.get());

            consumed.set(null);
            u.OptionalDouble empty = u.OptionalDouble.empty();
            empty.ifPresent(value -> consumed.set(value));
            assertNull(consumed.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Double> consumed = new AtomicReference<>();
            AtomicReference<Boolean> emptyCalled = new AtomicReference<>(false);

            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            optional.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertEquals(42.5, consumed.get());
            assertFalse(emptyCalled.get());

            consumed.set(null);
            emptyCalled.set(false);
            u.OptionalDouble empty = u.OptionalDouble.empty();
            empty.ifPresentOrElse(value -> consumed.set(value), () -> emptyCalled.set(true));
            assertNull(consumed.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            u.OptionalDouble filtered = optional.filter(value -> value > 40);
            assertTrue(filtered.isPresent());
            assertEquals(42.5, filtered.get());

            filtered = optional.filter(value -> value < 40);
            assertFalse(filtered.isPresent());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            filtered = empty.filter(value -> true);
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            u.OptionalDouble mapped = optional.map(value -> value * 2);
            assertTrue(mapped.isPresent());
            assertEquals(85.0, mapped.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            mapped = empty.map(value -> 99.0);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            u.OptionalInt mapped = optional.mapToInt(value -> value.intValue());
            assertTrue(mapped.isPresent());
            assertEquals(42, mapped.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            mapped = empty.mapToInt(value -> 99);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToLong() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            u.OptionalLong mapped = optional.mapToLong(value -> value.longValue());
            assertTrue(mapped.isPresent());
            assertEquals(42L, mapped.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            mapped = empty.mapToLong(value -> 99L);
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
            assertTrue(mapped.isPresent());
            assertEquals("42.5", mapped.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            mapped = empty.mapToObj(value -> "X");
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);

            u.OptionalDouble mapped = optional.flatMap(value -> u.OptionalDouble.of(value * 2));
            assertTrue(mapped.isPresent());
            assertEquals(85.0, mapped.get());

            mapped = optional.flatMap(value -> u.OptionalDouble.empty());
            assertFalse(mapped.isPresent());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            mapped = empty.flatMap(value -> u.OptionalDouble.of(99.0));
            assertFalse(mapped.isPresent());
        }

        @Test
        public void testOr() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            u.OptionalDouble result = optional.or(() -> u.OptionalDouble.of(99.0));
            assertTrue(result.isPresent());
            assertEquals(42.5, result.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            result = empty.or(() -> u.OptionalDouble.of(99.0));
            assertTrue(result.isPresent());
            assertEquals(99.0, result.get());
        }

        @Test
        public void testOrElseZero() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseZero());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertEquals(0.0, empty.orElseZero());
        }

        @Test
        public void testOrElse() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElse(99.0));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertEquals(99.0, empty.orElse(99.0));
        }

        @Test
        public void testOrElseGet() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseGet(() -> 99.0));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertEquals(99.0, empty.orElseGet(() -> 99.0));
        }

        @Test
        public void testOrElseThrow() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseThrow());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        public void testOrElseThrowWithMessage() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseThrow("Error"));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex.getMessage());
        }

        @Test
        public void testOrElseThrowWithMessageAndParams() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseThrow("Error %s", "param"));
            assertEquals(42.5, optional.orElseThrow("Error %s %s", "p1", "p2"));
            assertEquals(42.5, optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertEquals(42.5, optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
        }

        @Test
        public void testOrElseThrowWithSupplier() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals(42.5, optional.orElseThrow(() -> new IllegalStateException()));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
        }

        @Test
        public void testStream() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            long count = optional.stream().count();
            assertEquals(1, count);

            u.OptionalDouble empty = u.OptionalDouble.empty();
            count = empty.stream().count();
            assertEquals(0, count);
        }

        @Test
        public void testToList() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            List<Double> list = optional.toList();
            assertEquals(1, list.size());
            assertEquals(42.5, list.get(0));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            list = empty.toList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToSet() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            Set<Double> set = optional.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42.5));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            set = empty.toSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            ImmutableList<Double> list = optional.toImmutableList();
            assertEquals(1, list.size());
            assertEquals(42.5, list.get(0));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            list = empty.toImmutableList();
            assertTrue(list.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            ImmutableSet<Double> set = optional.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains(42.5));

            u.OptionalDouble empty = u.OptionalDouble.empty();
            set = empty.toImmutableSet();
            assertTrue(set.isEmpty());
        }

        @Test
        public void testBoxed() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            Optional<Double> boxed = optional.boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42.5, boxed.get());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            boxed = empty.boxed();
            assertFalse(boxed.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            java.util.OptionalDouble jdkOptional = optional.toJdkOptional();
            assertTrue(jdkOptional.isPresent());
            assertEquals(42.5, jdkOptional.getAsDouble());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            jdkOptional = empty.toJdkOptional();
            assertFalse(jdkOptional.isPresent());
        }

        @Test
        public void testDeprecatedDunderscoreMethod() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            java.util.OptionalDouble jdkOptional = optional.__();
            assertTrue(jdkOptional.isPresent());
            assertEquals(42.5, jdkOptional.getAsDouble());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            jdkOptional = empty.__();
            assertFalse(jdkOptional.isPresent());
        }

        @Test
        public void testCompareTo() {
            u.OptionalDouble val1 = u.OptionalDouble.of(10.5);
            u.OptionalDouble val2 = u.OptionalDouble.of(10.5);
            u.OptionalDouble val3 = u.OptionalDouble.of(20.5);
            u.OptionalDouble empty = u.OptionalDouble.empty();

            assertEquals(0, val1.compareTo(val2));
            assertTrue(val1.compareTo(val3) < 0);
            assertTrue(val3.compareTo(val1) > 0);
            assertTrue(val1.compareTo(empty) > 0);
            assertTrue(empty.compareTo(val1) < 0);
            assertEquals(0, empty.compareTo(u.OptionalDouble.empty()));
            assertTrue(val1.compareTo(null) > 0);
        }

        @Test
        public void testEquals() {
            u.OptionalDouble val1 = u.OptionalDouble.of(42.5);
            u.OptionalDouble val2 = u.OptionalDouble.of(42.5);
            u.OptionalDouble val3 = u.OptionalDouble.of(99.5);
            u.OptionalDouble empty1 = u.OptionalDouble.empty();
            u.OptionalDouble empty2 = u.OptionalDouble.empty();

            assertEquals(val1, val1);
            assertEquals(val1, val2);
            assertNotEquals(val1, val3);
            assertNotEquals(val1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(val1, null);
            assertNotEquals(val1, "string");

            u.OptionalDouble nan1 = u.OptionalDouble.of(Double.NaN);
            u.OptionalDouble nan2 = u.OptionalDouble.of(Double.NaN);
            assertEquals(nan1, nan2);
        }

        @Test
        public void testHashCode() {
            u.OptionalDouble val1 = u.OptionalDouble.of(42.5);
            u.OptionalDouble val2 = u.OptionalDouble.of(42.5);
            u.OptionalDouble val3 = u.OptionalDouble.of(99.5);
            u.OptionalDouble empty = u.OptionalDouble.empty();

            assertEquals(val1.hashCode(), val2.hashCode());
            assertNotEquals(val1.hashCode(), val3.hashCode());
            assertNotEquals(val1.hashCode(), empty.hashCode());
        }

        @Test
        public void testToString() {
            u.OptionalDouble optional = u.OptionalDouble.of(42.5);
            assertEquals("OptionalDouble[42.5]", optional.toString());

            u.OptionalDouble empty = u.OptionalDouble.empty();
            assertEquals("OptionalDouble.empty", empty.toString());
        }
    }

}
