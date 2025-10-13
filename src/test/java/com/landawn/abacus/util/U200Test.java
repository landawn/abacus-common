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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class U200Test extends TestBase {

    @Nested
    public class OptionalTest {

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
    }

    @Nested
    public class OptionalIntTest {

        @Test
        public void testCreation() {
            u.OptionalInt empty = u.OptionalInt.empty();
            assertFalse(empty.isPresent());

            u.OptionalInt present = u.OptionalInt.of(10);
            assertTrue(present.isPresent());
            assertEquals(10, present.get());

            u.OptionalInt fromNullable = u.OptionalInt.ofNullable(20);
            assertTrue(fromNullable.isPresent());
            assertFalse(u.OptionalInt.ofNullable(null).isPresent());
        }

        @Test
        public void testGetAndOrElse() {
            u.OptionalInt present = u.OptionalInt.of(10);
            u.OptionalInt empty = u.OptionalInt.empty();

            assertEquals(10, present.get());
            assertEquals(10, present.getAsInt());
            assertThrows(NoSuchElementException.class, empty::get);

            assertEquals(10, present.orElse(5));
            assertEquals(5, empty.orElse(5));

            assertEquals(10, present.orElseGet(() -> 5));
            assertEquals(5, empty.orElseGet(() -> 5));

            assertEquals(10, present.orElseZero());
            assertEquals(0, empty.orElseZero());
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
    }

    @Nested
    public class NullableTest {

        @Test
        public void testCreation() {
            u.Nullable<String> empty = u.Nullable.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isNotPresent());

            u.Nullable<String> present = u.Nullable.of("test");
            assertTrue(present.isPresent());
            assertEquals("test", present.get());

            u.Nullable<String> presentNull = u.Nullable.of(null);
            assertTrue(presentNull.isPresent());
            assertNull(presentNull.get());
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
        public void testGetAndOrElse() {
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> present = u.Nullable.of("val");
            u.Nullable<String> presentNull = u.Nullable.of(null);

            assertEquals("val", present.orElse("other"));
            assertEquals("other", empty.orElse("other"));
            assertEquals(null, presentNull.orElse("other"));

            assertEquals("val", present.orElseIfNull("other"));
            assertEquals("other", empty.orElseIfNull("other"));
            assertEquals("other", presentNull.orElseIfNull("other"));

            AtomicInteger supplierCalls = new AtomicInteger(0);
            assertEquals("val", present.orElseGetIfNull(() -> {
                supplierCalls.incrementAndGet();
                return "other";
            }));
            assertEquals(0, supplierCalls.get());

            assertEquals("other", presentNull.orElseGetIfNull(() -> {
                supplierCalls.incrementAndGet();
                return "other";
            }));
            assertEquals(1, supplierCalls.get());
        }

        @Test
        public void testOrElseThrow() {
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> presentNull = u.Nullable.of(null);

            assertThrows(NoSuchElementException.class, empty::orElseThrow);
            assertNull(presentNull.orElseThrow());

            assertThrows(NoSuchElementException.class, empty::orElseThrowIfNull);
            assertThrows(NoSuchElementException.class, presentNull::orElseThrowIfNull);
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
        public void testConversion() {
            u.Nullable<String> present = u.Nullable.of("val");
            u.Nullable<String> presentNull = u.Nullable.of(null);
            u.Nullable<String> empty = u.Nullable.empty();

            assertTrue(present.toOptional().isPresent());
            assertFalse(presentNull.toOptional().isPresent());
            assertFalse(empty.toOptional().isPresent());

            assertEquals(1, present.streamIfNotNull().count());
            assertEquals(0, presentNull.streamIfNotNull().count());
            assertEquals(0, empty.streamIfNotNull().count());
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
    }

}
