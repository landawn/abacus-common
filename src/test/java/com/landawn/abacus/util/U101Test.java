package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class U101Test extends TestBase {

    @Nested
    @DisplayName("Optional Tests")
    public class OptionalTest {

        @Test
        @DisplayName("Test static constants")
        public void testStaticConstants() {
            // Test TRUE constant
            assertTrue(u.Optional.TRUE.isPresent());
            assertEquals(Boolean.TRUE, u.Optional.TRUE.get());

            // Test FALSE constant
            assertTrue(u.Optional.FALSE.isPresent());
            assertEquals(Boolean.FALSE, u.Optional.FALSE.get());
        }

        @Test
        @DisplayName("Test empty()")
        public void testEmpty() {
            u.Optional<String> empty = u.Optional.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        @DisplayName("Test of(String)")
        public void testOfString() {
            // Test with non-empty string
            u.Optional<String> opt = u.Optional.of("test");
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            // Test with empty string
            u.Optional<String> emptyStr = u.Optional.of("");
            assertTrue(emptyStr.isPresent());
            assertEquals("", emptyStr.get());

            // Test null throws exception
            assertThrows(NullPointerException.class, () -> u.Optional.of((String) null));
        }

        @Test
        @DisplayName("Test of(T)")
        public void testOfGeneric() {
            // Test with Integer
            u.Optional<Integer> intOpt = u.Optional.of(42);
            assertTrue(intOpt.isPresent());
            assertEquals(42, intOpt.get());

            // Test with custom object
            List<String> list = Arrays.asList("a", "b");
            u.Optional<List<String>> listOpt = u.Optional.of(list);
            assertTrue(listOpt.isPresent());
            assertEquals(list, listOpt.get());

            // Test null throws exception
            assertThrows(NullPointerException.class, () -> u.Optional.of(null));
        }

        @Test
        @DisplayName("Test ofNullable(String)")
        public void testOfNullableString() {
            // Test with non-null string
            u.Optional<String> opt = u.Optional.ofNullable("test");
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            // Test with empty string
            u.Optional<String> emptyStr = u.Optional.ofNullable("");
            assertTrue(emptyStr.isPresent());
            assertEquals("", emptyStr.get());

            // Test with null
            u.Optional<String> nullOpt = u.Optional.ofNullable(null);
            assertFalse(nullOpt.isPresent());
        }

        @Test
        @DisplayName("Test ofNullable(T)")
        public void testOfNullableGeneric() {
            // Test with non-null value
            u.Optional<Integer> opt = u.Optional.ofNullable(42);
            assertTrue(opt.isPresent());
            assertEquals(42, opt.get());

            // Test with null
            u.Optional<Integer> nullOpt = u.Optional.ofNullable((Integer) null);
            assertFalse(nullOpt.isPresent());
        }

        @Test
        @DisplayName("Test from(java.util.Optional)")
        public void testFrom() {
            // Test with present java.util.Optional
            java.util.Optional<String> jdkOpt = java.util.Optional.of("test");
            u.Optional<String> opt = u.Optional.from(jdkOpt);
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            // Test with empty java.util.Optional
            java.util.Optional<String> emptyJdkOpt = java.util.Optional.empty();
            u.Optional<String> emptyOpt = u.Optional.from(emptyJdkOpt);
            assertFalse(emptyOpt.isPresent());

            // Test with null
            u.Optional<String> nullOpt = u.Optional.from(null);
            assertFalse(nullOpt.isPresent());
        }

        @Test
        @DisplayName("Test get() and orElseThrow()")
        public void testGetAndOrElseThrow() {
            // Test with present value
            u.Optional<String> present = u.Optional.of("test");
            assertEquals("test", present.get());
            assertEquals("test", present.orElseThrow());

            // Test with empty
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
        @DisplayName("Test ifPresent()")
        public void testIfPresent() throws Exception {
            // Test with present value
            StringBuilder sb = new StringBuilder();
            u.Optional<String> present = u.Optional.of("test");
            u.Optional<String> result = present.ifPresent(s -> sb.append(s));
            assertEquals("test", sb.toString());
            assertSame(present, result); // Should return itself

            // Test with empty
            StringBuilder sb2 = new StringBuilder();
            u.Optional<String> empty = u.Optional.empty();
            u.Optional<String> emptyResult = empty.ifPresent(s -> sb2.append(s));
            assertEquals("", sb2.toString());
            assertSame(empty, emptyResult);

            // Test null action throws exception
            assertThrows(IllegalArgumentException.class, () -> present.ifPresent(null));
        }

        @Test
        @DisplayName("Test ifPresentOrElse()")
        public void testIfPresentOrElse() throws Exception {
            // Test with present value
            StringBuilder sb = new StringBuilder();
            u.Optional<String> present = u.Optional.of("test");
            u.Optional<String> result = present.ifPresentOrElse(s -> sb.append("present:" + s), () -> sb.append("empty"));
            assertEquals("present:test", sb.toString());
            assertSame(present, result);

            // Test with empty
            StringBuilder sb2 = new StringBuilder();
            u.Optional<String> empty = u.Optional.empty();
            u.Optional<String> emptyResult = empty.ifPresentOrElse(s -> sb2.append("present:" + s), () -> sb2.append("empty"));
            assertEquals("empty", sb2.toString());
            assertSame(empty, emptyResult);

            // Test null arguments throw exception
            assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(null, () -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(s -> {
            }, null));
        }

        @Test
        @DisplayName("Test filter()")
        public void testFilter() throws Exception {
            // Test with present value that passes filter
            u.Optional<Integer> opt = u.Optional.of(10);
            u.Optional<Integer> filtered = opt.filter(i -> i > 5);
            assertTrue(filtered.isPresent());
            assertEquals(10, filtered.get());

            // Test with present value that fails filter
            u.Optional<Integer> filtered2 = opt.filter(i -> i > 20);
            assertFalse(filtered2.isPresent());

            // Test with empty
            u.Optional<Integer> empty = u.Optional.empty();
            u.Optional<Integer> filtered3 = empty.filter(i -> i > 5);
            assertFalse(filtered3.isPresent());

            // Test null predicate throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.filter(null));
        }

        @Test
        @DisplayName("Test mapToNonNull()")
        public void testMap() throws Exception {
            // Test with present value
            u.Optional<String> opt = u.Optional.of("test");
            u.Optional<Integer> mapped = opt.map(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test mapping to null throws exception
            assertThrows(NullPointerException.class, () -> opt.map(s -> null));

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            u.Optional<Integer> mappedEmpty = empty.map(String::length);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.map(null));
        }

        @Test
        @DisplayName("Test mapToBoolean()")
        public void testMapToBoolean() throws Exception {
            // Test with present value
            u.Optional<String> opt = u.Optional.of("test");
            OptionalBoolean mapped = opt.mapToBoolean(s -> s.length() > 3);
            assertTrue(mapped.isPresent());
            assertTrue(mapped.get());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            OptionalBoolean mappedEmpty = empty.mapToBoolean(s -> true);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToBoolean(null));
        }

        @Test
        @DisplayName("Test mapToChar()")
        public void testMapToChar() throws Exception {
            // Test with present value
            u.Optional<String> opt = u.Optional.of("test");
            OptionalChar mapped = opt.mapToChar(s -> s.charAt(0));
            assertTrue(mapped.isPresent());
            assertEquals('t', mapped.get());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            OptionalChar mappedEmpty = empty.mapToChar(s -> 'a');
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToChar(null));
        }

        @Test
        @DisplayName("Test mapToByte()")
        public void testMapToByte() throws Exception {
            // Test with present value
            u.Optional<Integer> opt = u.Optional.of(100);
            OptionalByte mapped = opt.mapToByte(Integer::byteValue);
            assertTrue(mapped.isPresent());
            assertEquals((byte) 100, mapped.get());

            // Test with empty
            u.Optional<Integer> empty = u.Optional.empty();
            OptionalByte mappedEmpty = empty.mapToByte(i -> (byte) 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToByte(null));
        }

        @Test
        @DisplayName("Test mapToShort()")
        public void testMapToShort() throws Exception {
            // Test with present value
            u.Optional<Integer> opt = u.Optional.of(1000);
            OptionalShort mapped = opt.mapToShort(Integer::shortValue);
            assertTrue(mapped.isPresent());
            assertEquals((short) 1000, mapped.get());

            // Test with empty
            u.Optional<Integer> empty = u.Optional.empty();
            OptionalShort mappedEmpty = empty.mapToShort(i -> (short) 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToShort(null));
        }

        @Test
        @DisplayName("Test mapToInt()")
        public void testMapToInt() throws Exception {
            // Test with present value
            u.Optional<String> opt = u.Optional.of("test");
            OptionalInt mapped = opt.mapToInt(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.getAsInt());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            OptionalInt mappedEmpty = empty.mapToInt(s -> 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToInt(null));
        }

        @Test
        @DisplayName("Test mapToLong()")
        public void testMapToLong() throws Exception {
            // Test with present value
            u.Optional<String> opt = u.Optional.of("test");
            OptionalLong mapped = opt.mapToLong(s -> (long) s.length());
            assertTrue(mapped.isPresent());
            assertEquals(4L, mapped.getAsLong());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            OptionalLong mappedEmpty = empty.mapToLong(s -> 0L);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToLong(null));
        }

        @Test
        @DisplayName("Test mapToFloat()")
        public void testMapToFloat() throws Exception {
            // Test with present value
            u.Optional<Integer> opt = u.Optional.of(10);
            OptionalFloat mapped = opt.mapToFloat(i -> i / 2.0f);
            assertTrue(mapped.isPresent());
            assertEquals(5.0f, mapped.get());

            // Test with empty
            u.Optional<Integer> empty = u.Optional.empty();
            OptionalFloat mappedEmpty = empty.mapToFloat(i -> 0.0f);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToFloat(null));
        }

        @Test
        @DisplayName("Test mapToDouble()")
        public void testMapToDouble() throws Exception {
            // Test with present value
            u.Optional<Integer> opt = u.Optional.of(10);
            OptionalDouble mapped = opt.mapToDouble(i -> i / 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(5.0, mapped.getAsDouble());

            // Test with empty
            u.Optional<Integer> empty = u.Optional.empty();
            OptionalDouble mappedEmpty = empty.mapToDouble(i -> 0.0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToDouble(null));
        }

        @Test
        @DisplayName("Test flatMap()")
        public void testFlatMap() throws Exception {
            // Test with present value mapping to present
            u.Optional<String> opt = u.Optional.of("test");
            u.Optional<Integer> mapped = opt.flatMap(s -> u.Optional.of(s.length()));
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test with present value mapping to empty
            u.Optional<Integer> mappedEmpty = opt.flatMap(s -> u.Optional.empty());
            assertFalse(mappedEmpty.isPresent());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            u.Optional<Integer> emptyMapped = empty.flatMap(s -> u.Optional.of(10));
            assertFalse(emptyMapped.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.flatMap(null));

            // Test mapper returning null throws exception
            assertThrows(NullPointerException.class, () -> opt.flatMap(s -> null));
        }

        @Test
        @DisplayName("Test flatMapIfNotNull()")
        public void testFlatMapIfNotNull() throws Exception {
            // Test with non-null value mapping to present
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Nullable<Integer> mapped = opt.flatMapIfNotNull(s -> u.Nullable.of(s.length()));
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            u.Nullable<Integer> mappedNull = nullOpt.flatMapIfNotNull(s -> u.Nullable.of(10));
            assertFalse(mappedNull.isPresent());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<Integer> emptyMapped = empty.flatMapIfNotNull(s -> u.Nullable.of(10));
            assertFalse(emptyMapped.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.flatMapIfNotNull(null));
        }

        @Test
        @DisplayName("Test contains()")
        public void testContains() {
            // Test with present non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            assertTrue(opt.contains("test"));
            assertFalse(opt.contains("other"));
            assertFalse(opt.contains(null));

            // Test with present null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            assertTrue(nullOpt.contains(null));
            assertFalse(nullOpt.contains("test"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertFalse(empty.contains("test"));
            assertFalse(empty.contains(null));
        }

        @Test
        @DisplayName("Test or()")
        public void testOr() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("first");
            u.Nullable<String> result = present.or(() -> u.Nullable.of("second"));
            assertTrue(result.isPresent());
            assertEquals("first", result.get());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> result2 = empty.or(() -> u.Nullable.of("second"));
            assertTrue(result2.isPresent());
            assertEquals("second", result2.get());

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> empty.or(null));

            // Test supplier returning null throws exception
            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        @DisplayName("Test orIfNull()")
        public void testOrIfNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("first");
            u.Nullable<String> result = nonNull.orIfNull(() -> u.Nullable.of("second"));
            assertTrue(result.isPresent());
            assertEquals("first", result.get());

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            u.Nullable<String> result2 = nullValue.orIfNull(() -> u.Nullable.of("second"));
            assertTrue(result2.isPresent());
            assertEquals("second", result2.get());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> result3 = empty.orIfNull(() -> u.Nullable.of("second"));
            assertTrue(result3.isPresent());
            assertEquals("second", result3.get());

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> nullValue.orIfNull(null));
        }

        @Test
        @DisplayName("Test orElseNull()")
        public void testOrElseNull() {
            // Test with present non-null value
            u.Nullable<String> present = u.Nullable.of("test");
            assertEquals("test", present.orElseNull());

            // Test with present null value
            u.Nullable<String> nullPresent = u.Nullable.of((String) null);
            assertNull(nullPresent.orElseNull());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertNull(empty.orElseNull());
        }

        @Test
        @DisplayName("Test orElse()")
        public void testOrElse() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            assertEquals("test", present.orElse("default"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertEquals("default", empty.orElse("default"));

            // Test with null default
            assertNull(empty.orElse(null));
        }

        @Test
        @DisplayName("Test orElseIfNull()")
        public void testOrElseIfNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertEquals("test", nonNull.orElseIfNull("default"));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            assertEquals("default", nullValue.orElseIfNull("default"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertEquals("default", empty.orElseIfNull("default"));
        }

        @Test
        @DisplayName("Test orElseGet()")
        public void testOrElseGet() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            assertEquals("test", present.orElseGet(() -> "default"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertEquals("default", empty.orElseGet(() -> "default"));

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> empty.orElseGet(null));
        }

        @Test
        @DisplayName("Test orElseGetIfNull()")
        public void testOrElseGetIfNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertEquals("test", nonNull.orElseGetIfNull(() -> "default"));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            assertEquals("default", nullValue.orElseGetIfNull(() -> "default"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertEquals("default", empty.orElseGetIfNull(() -> "default"));

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> nullValue.orElseGetIfNull(null));
        }

        @Test
        @DisplayName("Test orElseThrow() with message variations")
        public void testOrElseThrowWithMessage() {
            u.Nullable<String> empty = u.Nullable.empty();

            // Test with custom message
            NoSuchElementException ex1 = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
            assertEquals("Custom error", ex1.getMessage());

            // Test with message and one param
            NoSuchElementException ex2 = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error: %s", "param1"));
            assertTrue(ex2.getMessage().contains("param1"));

            // Test with message and two params
            NoSuchElementException ex3 = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error: %s %s", "param1", "param2"));
            assertTrue(ex3.getMessage().contains("param1"));
            assertTrue(ex3.getMessage().contains("param2"));

            // Test with message and three params
            NoSuchElementException ex4 = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error: %s %s %s", "p1", "p2", "p3"));
            assertTrue(ex4.getMessage().contains("p1"));
            assertTrue(ex4.getMessage().contains("p2"));
            assertTrue(ex4.getMessage().contains("p3"));

            // Test with message and multiple params
            NoSuchElementException ex5 = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
            assertTrue(ex5.getMessage().contains("p1"));
            assertTrue(ex5.getMessage().contains("p4"));
        }

        @Test
        @DisplayName("Test orElseThrow(Supplier)")
        public void testOrElseThrowSupplier() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            assertEquals("test", present.orElseThrow(() -> new RuntimeException()));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> empty.orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", ex.getMessage());

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> empty.orElseThrow((Supplier<RuntimeException>) null));
        }

        @Test
        @DisplayName("Test orElseThrowIfNull()")
        public void testOrElseThrowIfNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertEquals("test", nonNull.orElseThrowIfNull());

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull());
        }

        @Test
        @DisplayName("Test orElseThrowIfNull() with message variations")
        public void testOrElseThrowIfNullWithMessage() {
            u.Nullable<String> nullValue = u.Nullable.of((String) null);

            // Test with custom message
            NoSuchElementException ex1 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Custom error"));
            assertEquals("Custom error", ex1.getMessage());

            // Test with message and one param
            NoSuchElementException ex2 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s", "param1"));
            assertTrue(ex2.getMessage().contains("param1"));

            // Test with message and two params
            NoSuchElementException ex3 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s", "param1", "param2"));
            assertTrue(ex3.getMessage().contains("param1"));
            assertTrue(ex3.getMessage().contains("param2"));

            // Test with message and three params
            NoSuchElementException ex4 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
            assertTrue(ex4.getMessage().contains("p1"));
            assertTrue(ex4.getMessage().contains("p2"));
            assertTrue(ex4.getMessage().contains("p3"));

            // Test with message and multiple params
            NoSuchElementException ex5 = assertThrows(NoSuchElementException.class,
                    () -> nullValue.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
            assertTrue(ex5.getMessage().contains("p1"));
            assertTrue(ex5.getMessage().contains("p4"));
        }

        @Test
        @DisplayName("Test orElseThrowIfNull(Supplier)")
        public void testOrElseThrowIfNullSupplier() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertEquals("test", nonNull.orElseThrowIfNull(() -> new RuntimeException()));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> nullValue.orElseThrowIfNull(() -> new RuntimeException("Custom")));
            assertEquals("Custom", ex.getMessage());

            // Test null supplier throws exception
            assertThrows(IllegalArgumentException.class, () -> nullValue.orElseThrowIfNull((Supplier<RuntimeException>) null));
        }

        @Test
        @DisplayName("Test stream()")
        public void testStream() {
            // Test with present non-null value
            u.Nullable<String> present = u.Nullable.of("test");
            List<String> list = present.stream().toList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with present null value
            u.Nullable<String> nullPresent = u.Nullable.of((String) null);
            List<String> nullList = nullPresent.stream().toList();
            assertEquals(1, nullList.size());
            assertNull(nullList.get(0));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            List<String> emptyList = empty.stream().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test streamIfNotNull()")
        public void testStreamIfNotNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            List<String> list = nonNull.streamIfNotNull().toList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            List<String> nullList = nullValue.streamIfNotNull().toList();
            assertTrue(nullList.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            List<String> emptyList = empty.streamIfNotNull().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test toList()")
        public void testToList() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            List<String> list = present.toList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            List<String> emptyList = empty.toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test toListIfNotNull()")
        public void testToListIfNotNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            List<String> list = nonNull.toListIfNotNull();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            List<String> nullList = nullValue.toListIfNotNull();
            assertTrue(nullList.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            List<String> emptyList = empty.toListIfNotNull();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test toSet()")
        public void testToSet() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            Set<String> set = present.toSet();
            assertEquals(1, set.size());
            assertTrue(set.contains("test"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            Set<String> emptySet = empty.toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        @DisplayName("Test toSetIfNotNull()")
        public void testToSetIfNotNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            Set<String> set = nonNull.toSetIfNotNull();
            assertEquals(1, set.size());
            assertTrue(set.contains("test"));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            Set<String> nullSet = nullValue.toSetIfNotNull();
            assertTrue(nullSet.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            Set<String> emptySet = empty.toSetIfNotNull();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        @DisplayName("Test toImmutableList()")
        public void testToImmutableList() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            ImmutableList<String> list = present.toImmutableList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            ImmutableList<String> emptyList = empty.toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test toImmutableListIfNotNull()")
        public void testToImmutableListIfNotNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            ImmutableList<String> list = nonNull.toImmutableListIfNotNull();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            ImmutableList<String> nullList = nullValue.toImmutableListIfNotNull();
            assertTrue(nullList.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            ImmutableList<String> emptyList = empty.toImmutableListIfNotNull();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        @DisplayName("Test toImmutableSet()")
        public void testToImmutableSet() {
            // Test with present value
            u.Nullable<String> present = u.Nullable.of("test");
            ImmutableSet<String> set = present.toImmutableSet();
            assertEquals(1, set.size());
            assertTrue(set.contains("test"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            ImmutableSet<String> emptySet = empty.toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        @DisplayName("Test toImmutableSetIfNotNull()")
        public void testToImmutableSetIfNotNull() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            ImmutableSet<String> set = nonNull.toImmutableSetIfNotNull();
            assertEquals(1, set.size());
            assertTrue(set.contains("test"));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            ImmutableSet<String> nullSet = nullValue.toImmutableSetIfNotNull();
            assertTrue(nullSet.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            ImmutableSet<String> emptySet = empty.toImmutableSetIfNotNull();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        @DisplayName("Test toOptional()")
        public void testToOptional() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            u.Optional<String> opt = nonNull.toOptional();
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            u.Optional<String> nullOpt = nullValue.toOptional();
            assertFalse(nullOpt.isPresent());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Optional<String> emptyOpt = empty.toOptional();
            assertFalse(emptyOpt.isPresent());
        }

        @Test
        @DisplayName("Test toJdkOptional()")
        public void testToJdkOptional() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            java.util.Optional<String> jdkOpt = nonNull.toJdkOptional();
            assertTrue(jdkOpt.isPresent());
            assertEquals("test", jdkOpt.get());

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            java.util.Optional<String> nullJdkOpt = nullValue.toJdkOptional();
            assertFalse(nullJdkOpt.isPresent());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            java.util.Optional<String> emptyJdkOpt = empty.toJdkOptional();
            assertFalse(emptyJdkOpt.isPresent());
        }

        @Test
        @DisplayName("Test equals()")
        public void testEquals() {
            u.Nullable<String> nonNull1 = u.Nullable.of("test");
            u.Nullable<String> nonNull2 = u.Nullable.of("test");
            u.Nullable<String> nonNull3 = u.Nullable.of("other");
            u.Nullable<String> null1 = u.Nullable.of((String) null);
            u.Nullable<String> null2 = u.Nullable.of((String) null);
            u.Nullable<String> empty1 = u.Nullable.empty();
            u.Nullable<String> empty2 = u.Nullable.empty();

            // Test equality
            assertEquals(nonNull1, nonNull1); // Same instance
            assertEquals(nonNull1, nonNull2); // Equal non-null values
            assertNotEquals(nonNull1, nonNull3); // Different values
            assertEquals(null1, null2); // Both null
            assertEquals(empty1, empty2); // Both empty
            assertNotEquals(nonNull1, null1); // Non-null vs null
            assertNotEquals(nonNull1, empty1); // Present vs empty
            assertNotEquals(null1, empty1); // Null vs empty

            // Test with null and other types
            assertNotEquals(nonNull1, null);
            assertNotEquals(nonNull1, "test");
        }

        @Test
        @DisplayName("Test hashCode()")
        public void testHashCode() {
            u.Nullable<String> nonNull1 = u.Nullable.of("test");
            u.Nullable<String> nonNull2 = u.Nullable.of("test");
            u.Nullable<String> null1 = u.Nullable.of((String) null);
            u.Nullable<String> null2 = u.Nullable.of((String) null);
            u.Nullable<String> empty1 = u.Nullable.empty();
            u.Nullable<String> empty2 = u.Nullable.empty();

            // Equal objects should have equal hash codes
            assertEquals(nonNull1.hashCode(), nonNull2.hashCode());
            assertEquals(null1.hashCode(), null2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
        }

        @Test
        @DisplayName("Test toString()")
        public void testToString() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertTrue(nonNull.toString().contains("test"));
            assertTrue(nonNull.toString().contains("Nullable"));

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            assertTrue(nullValue.toString().contains("null"));
            assertTrue(nullValue.toString().contains("Nullable"));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertEquals("Nullable.empty", empty.toString());
        }

        @Test
        @DisplayName("Test __()")
        public void testDoubleUnderscore() {
            // Test with present value
            u.Optional<String> present = u.Optional.of("test");
            java.util.Optional<String> jdkOpt = present.__();
            assertTrue(jdkOpt.isPresent());
            assertEquals("test", jdkOpt.get());

            // Test with empty
            u.Optional<String> empty = u.Optional.empty();
            java.util.Optional<String> emptyJdkOpt = empty.__();
            assertFalse(emptyJdkOpt.isPresent());
        }
    }

    @Nested
    @DisplayName("Nullable Tests")
    public class NullableTest {

        @Test
        @DisplayName("Test static constants")
        public void testStaticConstants() {
            // Test TRUE constant
            assertTrue(u.Nullable.TRUE.isPresent());
            assertEquals(Boolean.TRUE, u.Nullable.TRUE.get());

            // Test FALSE constant
            assertTrue(u.Nullable.FALSE.isPresent());
            assertEquals(Boolean.FALSE, u.Nullable.FALSE.get());
        }

        @Test
        @DisplayName("Test empty()")
        public void testEmpty() {
            u.Nullable<String> empty = u.Nullable.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isNotPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        @DisplayName("Test of(String)")
        public void testOfString() {
            // Test with non-null string
            u.Nullable<String> opt = u.Nullable.of("test");
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            // Test with empty string
            u.Nullable<String> emptyStr = u.Nullable.of("");
            assertTrue(emptyStr.isPresent());
            assertEquals("", emptyStr.get());

            // Test with null
            u.Nullable<String> nullStr = u.Nullable.of((String) null);
            assertTrue(nullStr.isPresent());
            assertNull(nullStr.get());
            assertTrue(nullStr.isNull());
        }

        @Test
        @DisplayName("Test of(T)")
        public void testOfGeneric() {
            // Test with non-null value
            u.Nullable<Integer> intOpt = u.Nullable.of(42);
            assertTrue(intOpt.isPresent());
            assertEquals(42, intOpt.get());

            // Test with null
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            assertTrue(nullOpt.isPresent());
            assertNull(nullOpt.get());
            assertTrue(nullOpt.isNull());
        }

        @Test
        @DisplayName("Test from(Optional)")
        public void testFromOptional() {
            // Test with present Optional
            u.Optional<String> opt = u.Optional.of("test");
            u.Nullable<String> nullable = u.Nullable.from(opt);
            assertTrue(nullable.isPresent());
            assertEquals("test", nullable.get());

            // Test with empty Optional
            u.Optional<String> empty = u.Optional.empty();
            u.Nullable<String> emptyNullable = u.Nullable.from(empty);
            assertFalse(emptyNullable.isPresent());
        }

        @Test
        @DisplayName("Test from(java.util.Optional)")
        public void testFromJdkOptional() {
            // Test with present java.util.Optional
            java.util.Optional<String> jdkOpt = java.util.Optional.of("test");
            u.Nullable<String> nullable = u.Nullable.from(jdkOpt);
            assertTrue(nullable.isPresent());
            assertEquals("test", nullable.get());

            // Test with empty java.util.Optional
            java.util.Optional<String> emptyJdkOpt = java.util.Optional.empty();
            u.Nullable<String> emptyNullable = u.Nullable.from(emptyJdkOpt);
            assertFalse(emptyNullable.isPresent());
        }

        @Test
        @DisplayName("Test get() and orElseThrow()")
        public void testGetAndOrElseThrow() {
            // Test with present non-null value
            u.Nullable<String> present = u.Nullable.of("test");
            assertEquals("test", present.get());
            assertEquals("test", present.orElseThrow());

            // Test with present null value
            u.Nullable<String> nullPresent = u.Nullable.of((String) null);
            assertNull(nullPresent.get());
            assertNull(nullPresent.orElseThrow());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
            assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());
        }

        @Test
        @DisplayName("Test isPresent(), isNotPresent(), isEmpty()")
        public void testPresenceChecks() {
            // Test with non-null value
            u.Nullable<String> present = u.Nullable.of("test");
            assertTrue(present.isPresent());
            assertFalse(present.isNotPresent());
            assertFalse(present.isEmpty());

            // Test with null value
            u.Nullable<String> nullPresent = u.Nullable.of((String) null);
            assertTrue(nullPresent.isPresent());
            assertFalse(nullPresent.isNotPresent());
            assertFalse(nullPresent.isEmpty());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isNotPresent());
            assertTrue(empty.isEmpty());
        }

        @Test
        @DisplayName("Test isNull() and isNotNull()")
        public void testNullChecks() {
            // Test with non-null value
            u.Nullable<String> nonNull = u.Nullable.of("test");
            assertFalse(nonNull.isNull());
            assertTrue(nonNull.isNotNull());

            // Test with null value
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            assertTrue(nullValue.isNull());
            assertFalse(nullValue.isNotNull());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            assertTrue(empty.isNull());
            assertFalse(empty.isNotNull());
        }

        @Test
        @DisplayName("Test ifPresent()")
        public void testIfPresent() throws Exception {
            // Test with present non-null value
            StringBuilder sb = new StringBuilder();
            u.Nullable<String> present = u.Nullable.of("test");
            u.Nullable<String> result = present.ifPresent(s -> sb.append(s));
            assertEquals("test", sb.toString());
            assertSame(present, result);

            // Test with present null value
            StringBuilder sb2 = new StringBuilder();
            u.Nullable<String> nullPresent = u.Nullable.of((String) null);
            u.Nullable<String> result2 = nullPresent.ifPresent(s -> sb2.append("null:" + s));
            assertEquals("null:null", sb2.toString());
            assertSame(nullPresent, result2);

            // Test with empty
            StringBuilder sb3 = new StringBuilder();
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> result3 = empty.ifPresent(s -> sb3.append(s));
            assertEquals("", sb3.toString());
            assertSame(empty, result3);

            // Test null action throws exception
            assertThrows(IllegalArgumentException.class, () -> present.ifPresent(null));
        }

        @Test
        @DisplayName("Test ifPresentOrElse()")
        public void testIfPresentOrElse() throws Exception {
            // Test with present value
            StringBuilder sb = new StringBuilder();
            u.Nullable<String> present = u.Nullable.of("test");
            u.Nullable<String> result = present.ifPresentOrElse(s -> sb.append("present:" + s), () -> sb.append("empty"));
            assertEquals("present:test", sb.toString());
            assertSame(present, result);

            // Test with empty
            StringBuilder sb2 = new StringBuilder();
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> result2 = empty.ifPresentOrElse(s -> sb2.append("present:" + s), () -> sb2.append("empty"));
            assertEquals("empty", sb2.toString());
            assertSame(empty, result2);

            // Test null arguments throw exception
            assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(null, () -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(s -> {
            }, null));
        }

        @Test
        @DisplayName("Test ifNotNull()")
        public void testIfNotNull() throws Exception {
            // Test with non-null value
            StringBuilder sb = new StringBuilder();
            u.Nullable<String> nonNull = u.Nullable.of("test");
            u.Nullable<String> result = nonNull.ifNotNull(s -> sb.append(s));
            assertEquals("test", sb.toString());
            assertSame(nonNull, result);

            // Test with null value
            StringBuilder sb2 = new StringBuilder();
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            u.Nullable<String> result2 = nullValue.ifNotNull(s -> sb2.append(s));
            assertEquals("", sb2.toString());
            assertSame(nullValue, result2);

            // Test with empty
            StringBuilder sb3 = new StringBuilder();
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<String> result3 = empty.ifNotNull(s -> sb3.append(s));
            assertEquals("", sb3.toString());
            assertSame(empty, result3);

            // Test null action throws exception
            assertThrows(IllegalArgumentException.class, () -> nonNull.ifNotNull(null));
        }

        @Test
        @DisplayName("Test ifNotNullOrElse()")
        public void testIfNotNullOrElse() throws Exception {
            // Test with non-null value
            StringBuilder sb = new StringBuilder();
            u.Nullable<String> nonNull = u.Nullable.of("test");
            u.Nullable<String> result = nonNull.ifNotNullOrElse(s -> sb.append("notNull:" + s), () -> sb.append("null"));
            assertEquals("notNull:test", sb.toString());
            assertSame(nonNull, result);

            // Test with null value
            StringBuilder sb2 = new StringBuilder();
            u.Nullable<String> nullValue = u.Nullable.of((String) null);
            u.Nullable<String> result2 = nullValue.ifNotNullOrElse(s -> sb2.append("notNull:" + s), () -> sb2.append("null"));
            assertEquals("null", sb2.toString());
            assertSame(nullValue, result2);

            // Test null arguments throw exception
            assertThrows(IllegalArgumentException.class, () -> nonNull.ifNotNullOrElse(null, () -> {
            }));
            assertThrows(IllegalArgumentException.class, () -> nonNull.ifNotNullOrElse(s -> {
            }, null));
        }

        @Test
        @DisplayName("Test filter()")
        public void testFilter() throws Exception {
            // Test with present value that passes filter
            u.Nullable<Integer> opt = u.Nullable.of(10);
            u.Nullable<Integer> filtered = opt.filter(i -> i > 5);
            assertTrue(filtered.isPresent());
            assertEquals(10, filtered.get());

            // Test with present value that fails filter
            u.Nullable<Integer> filtered2 = opt.filter(i -> i > 20);
            assertFalse(filtered2.isPresent());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            u.Nullable<Integer> filtered3 = nullOpt.filter(i -> i != null);
            assertFalse(filtered3.isPresent());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            u.Nullable<Integer> filtered4 = empty.filter(i -> true);
            assertFalse(filtered4.isPresent());

            // Test null predicate throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.filter(null));
        }

        @Test
        @DisplayName("Test filterIfNotNull()")
        public void testFilterIfNotNull() throws Exception {
            // Test with non-null value that passes filter
            u.Nullable<Integer> opt = u.Nullable.of(10);
            u.Optional<Integer> filtered = opt.filterIfNotNull(i -> i > 5);
            assertTrue(filtered.isPresent());
            assertEquals(10, filtered.get());

            // Test with non-null value that fails filter
            u.Optional<Integer> filtered2 = opt.filterIfNotNull(i -> i > 20);
            assertFalse(filtered2.isPresent());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            u.Optional<Integer> filtered3 = nullOpt.filterIfNotNull(i -> true);
            assertFalse(filtered3.isPresent());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            u.Optional<Integer> filtered4 = empty.filterIfNotNull(i -> true);
            assertFalse(filtered4.isPresent());

            // Test null predicate throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.filterIfNotNull(null));
        }

        @Test
        @DisplayName("Test map()")
        public void testMap() throws Exception {
            // Test with present non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Nullable<Integer> mapped = opt.map(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test mapping to null
            u.Nullable<String> mappedToNull = opt.map(s -> null);
            assertTrue(mappedToNull.isPresent());
            assertNull(mappedToNull.get());

            // Test with present null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            u.Nullable<Integer> mappedNull = nullOpt.map(s -> s == null ? 0 : s.length());
            assertTrue(mappedNull.isPresent());
            assertEquals(0, mappedNull.get());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<Integer> mappedEmpty = empty.map(String::length);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.map(null));
        }

        @Test
        @DisplayName("Test mapToNonNull()")
        public void testMapToNonNull() throws Exception {
            // Test with present value
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Optional<Integer> mapped = opt.mapToNonNull(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test mapping to null throws exception
            assertThrows(NullPointerException.class, () -> opt.mapToNonNull(s -> null));

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Optional<Integer> mappedEmpty = empty.mapToNonNull(String::length);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToNonNull(null));
        }

        @Test
        @DisplayName("Test mapToBoolean()")
        public void testMapToBoolean() throws Exception {
            // Test with present value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalBoolean mapped = opt.mapToBoolean(s -> s.length() > 3);
            assertTrue(mapped.isPresent());
            assertTrue(mapped.get());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            OptionalBoolean mappedEmpty = empty.mapToBoolean(s -> true);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToBoolean(null));
        }

        @Test
        @DisplayName("Test mapToChar()")
        public void testMapToChar() throws Exception {
            // Test with present value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalChar mapped = opt.mapToChar(s -> s.charAt(0));
            assertTrue(mapped.isPresent());
            assertEquals('t', mapped.get());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            OptionalChar mappedEmpty = empty.mapToChar(s -> 'a');
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToChar(null));
        }

        @Test
        @DisplayName("Test mapToByte()")
        public void testMapToByte() throws Exception {
            // Test with present value
            u.Nullable<Integer> opt = u.Nullable.of(100);
            OptionalByte mapped = opt.mapToByte(Integer::byteValue);
            assertTrue(mapped.isPresent());
            assertEquals((byte) 100, mapped.get());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            OptionalByte mappedEmpty = empty.mapToByte(i -> (byte) 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToByte(null));
        }

        @Test
        @DisplayName("Test mapToShort()")
        public void testMapToShort() throws Exception {
            // Test with present value
            u.Nullable<Integer> opt = u.Nullable.of(1000);
            OptionalShort mapped = opt.mapToShort(Integer::shortValue);
            assertTrue(mapped.isPresent());
            assertEquals((short) 1000, mapped.get());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            OptionalShort mappedEmpty = empty.mapToShort(i -> (short) 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToShort(null));
        }

        @Test
        @DisplayName("Test mapToInt()")
        public void testMapToInt() throws Exception {
            // Test with present value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalInt mapped = opt.mapToInt(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.getAsInt());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            OptionalInt mappedEmpty = empty.mapToInt(s -> 0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToInt(null));
        }

        @Test
        @DisplayName("Test mapToLong()")
        public void testMapToLong() throws Exception {
            // Test with present value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalLong mapped = opt.mapToLong(s -> (long) s.length());
            assertTrue(mapped.isPresent());
            assertEquals(4L, mapped.getAsLong());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            OptionalLong mappedEmpty = empty.mapToLong(s -> 0L);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToLong(null));
        }

        @Test
        @DisplayName("Test mapToFloat()")
        public void testMapToFloat() throws Exception {
            // Test with present value
            u.Nullable<Integer> opt = u.Nullable.of(10);
            OptionalFloat mapped = opt.mapToFloat(i -> i / 2.0f);
            assertTrue(mapped.isPresent());
            assertEquals(5.0f, mapped.get());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            OptionalFloat mappedEmpty = empty.mapToFloat(i -> 0.0f);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToFloat(null));
        }

        @Test
        @DisplayName("Test mapToDouble()")
        public void testMapToDouble() throws Exception {
            // Test with present value
            u.Nullable<Integer> opt = u.Nullable.of(10);
            OptionalDouble mapped = opt.mapToDouble(i -> i / 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(5.0, mapped.getAsDouble());

            // Test with empty
            u.Nullable<Integer> empty = u.Nullable.empty();
            OptionalDouble mappedEmpty = empty.mapToDouble(i -> 0.0);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToDouble(null));
        }

        @Test
        @DisplayName("Test mapIfNotNull()")
        public void testMapIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Nullable<Integer> mapped = opt.mapIfNotNull(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            u.Nullable<Integer> mappedNull = nullOpt.mapIfNotNull(String::length);
            assertFalse(mappedNull.isPresent());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<Integer> mappedEmpty = empty.mapIfNotNull(String::length);
            assertFalse(mappedEmpty.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToNonNullIfNotNull()")
        public void testMapToNonNullIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Optional<Integer> mapped = opt.mapToNonNullIfNotNull(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            u.Optional<Integer> mappedNull = nullOpt.mapToNonNullIfNotNull(String::length);
            assertFalse(mappedNull.isPresent());

            // Test mapping to null throws exception
            assertThrows(NullPointerException.class, () -> opt.mapToNonNullIfNotNull(s -> null));

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToNonNullIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToBooleanIfNotNull()")
        public void testMapToBooleanIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalBoolean mapped = opt.mapToBooleanIfNotNull(s -> s.length() > 3);
            assertTrue(mapped.isPresent());
            assertTrue(mapped.get());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            OptionalBoolean mappedNull = nullOpt.mapToBooleanIfNotNull(s -> true);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToBooleanIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToCharIfNotNull()")
        public void testMapToCharIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalChar mapped = opt.mapToCharIfNotNull(s -> s.charAt(0));
            assertTrue(mapped.isPresent());
            assertEquals('t', mapped.get());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            OptionalChar mappedNull = nullOpt.mapToCharIfNotNull(s -> 'a');
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToCharIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToByteIfNotNull()")
        public void testMapToByteIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<Integer> opt = u.Nullable.of(100);
            OptionalByte mapped = opt.mapToByteIfNotNull(Integer::byteValue);
            assertTrue(mapped.isPresent());
            assertEquals((byte) 100, mapped.get());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            OptionalByte mappedNull = nullOpt.mapToByteIfNotNull(i -> (byte) 0);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToByteIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToShortIfNotNull()")
        public void testMapToShortIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<Integer> opt = u.Nullable.of(1000);
            OptionalShort mapped = opt.mapToShortIfNotNull(Integer::shortValue);
            assertTrue(mapped.isPresent());
            assertEquals((short) 1000, mapped.get());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            OptionalShort mappedNull = nullOpt.mapToShortIfNotNull(i -> (short) 0);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToShortIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToIntIfNotNull()")
        public void testMapToIntIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalInt mapped = opt.mapToIntIfNotNull(String::length);
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.getAsInt());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            OptionalInt mappedNull = nullOpt.mapToIntIfNotNull(s -> 0);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToIntIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToLongIfNotNull()")
        public void testMapToLongIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<String> opt = u.Nullable.of("test");
            OptionalLong mapped = opt.mapToLongIfNotNull(s -> (long) s.length());
            assertTrue(mapped.isPresent());
            assertEquals(4L, mapped.getAsLong());

            // Test with null value
            u.Nullable<String> nullOpt = u.Nullable.of((String) null);
            OptionalLong mappedNull = nullOpt.mapToLongIfNotNull(s -> 0L);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToLongIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToFloatIfNotNull()")
        public void testMapToFloatIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<Integer> opt = u.Nullable.of(10);
            OptionalFloat mapped = opt.mapToFloatIfNotNull(i -> i / 2.0f);
            assertTrue(mapped.isPresent());
            assertEquals(5.0f, mapped.get());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            OptionalFloat mappedNull = nullOpt.mapToFloatIfNotNull(i -> 0.0f);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToFloatIfNotNull(null));
        }

        @Test
        @DisplayName("Test mapToDoubleIfNotNull()")
        public void testMapToDoubleIfNotNull() throws Exception {
            // Test with non-null value
            u.Nullable<Integer> opt = u.Nullable.of(10);
            OptionalDouble mapped = opt.mapToDoubleIfNotNull(i -> i / 2.0);
            assertTrue(mapped.isPresent());
            assertEquals(5.0, mapped.getAsDouble());

            // Test with null value
            u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
            OptionalDouble mappedNull = nullOpt.mapToDoubleIfNotNull(i -> 0.0);
            assertFalse(mappedNull.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.mapToDoubleIfNotNull(null));
        }

        @Test
        @DisplayName("Test flatMap()")
        public void testFlatMap() throws Exception {
            // Test with present value mapping to present
            u.Nullable<String> opt = u.Nullable.of("test");
            u.Nullable<Integer> mapped = opt.flatMap(s -> u.Nullable.of(s.length()));
            assertTrue(mapped.isPresent());
            assertEquals(4, mapped.get());

            // Test with present value mapping to empty
            u.Nullable<Integer> mappedEmpty = opt.flatMap(s -> u.Nullable.empty());
            assertFalse(mappedEmpty.isPresent());

            // Test with empty
            u.Nullable<String> empty = u.Nullable.empty();
            u.Nullable<Integer> emptyMapped = empty.flatMap(s -> u.Nullable.of(10));
            assertFalse(emptyMapped.isPresent());

            // Test null mapper throws exception
            assertThrows(IllegalArgumentException.class, () -> opt.flatMap(null));
        }
    }
}
