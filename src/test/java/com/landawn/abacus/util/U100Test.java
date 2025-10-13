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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class U100Test extends TestBase {

    @Nested
    public class uOptionalTest {

        @Test
        public void testOptionalConstants() {
            assertTrue(Optional.TRUE.isPresent());
            assertTrue(Optional.TRUE.get());

            assertTrue(Optional.FALSE.isPresent());
            assertFalse(Optional.FALSE.get());
        }

        @Test
        public void testEmpty() {
            Optional<String> empty = Optional.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testOfString() {
            Optional<String> opt = Optional.of("test");
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            Optional<String> emptyString = Optional.of("");
            assertTrue(emptyString.isPresent());
            assertEquals("", emptyString.get());

            assertThrows(NullPointerException.class, () -> Optional.of((String) null));
        }

        @Test
        public void testOf() {
            Optional<Integer> opt = Optional.of(42);
            assertTrue(opt.isPresent());
            assertEquals(42, opt.get());

            assertThrows(NullPointerException.class, () -> Optional.of(null));
        }

        @Test
        public void testOfNullableString() {
            Optional<String> nullOpt = Optional.ofNullable((String) null);
            assertFalse(nullOpt.isPresent());

            Optional<String> emptyString = Optional.ofNullable("");
            assertTrue(emptyString.isPresent());
            assertEquals("", emptyString.get());

            Optional<String> opt = Optional.ofNullable("test");
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());
        }

        @Test
        public void testOfNullable() {
            Optional<Integer> nullOpt = Optional.ofNullable((Integer) null);
            assertFalse(nullOpt.isPresent());

            Optional<Integer> opt = Optional.ofNullable(42);
            assertTrue(opt.isPresent());
            assertEquals(42, opt.get());
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
        public void testGet() {
            Optional<String> present = Optional.of("test");
            assertEquals("test", present.get());

            Optional<String> empty = Optional.empty();
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(Optional.of("test").isPresent());
            assertFalse(Optional.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(Optional.of("test").isEmpty());
            assertTrue(Optional.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);

            Optional.of("test").ifPresent(value -> {
                assertEquals("test", value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            Optional.empty().ifPresent(value -> called.set(true));
            assertFalse(called.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

            Optional.of("test").ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(emptyActionCalled.get());

            actionCalled.set(false);
            Optional.empty().ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertFalse(actionCalled.get());
            assertTrue(emptyActionCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            Optional<String> opt = Optional.of("test");

            Optional<String> filtered = opt.filter(s -> s.length() > 3);
            assertTrue(filtered.isPresent());
            assertEquals("test", filtered.get());

            filtered = opt.filter(s -> s.length() > 10);
            assertFalse(filtered.isPresent());

            Optional<String> empty = Optional.empty();
            assertFalse(empty.filter(s -> true).isPresent());
        }

        @Test
        public void testMap() throws Exception {
            Optional<String> opt = Optional.of("test");

            Optional<Integer> length = opt.map(String::length);
            assertTrue(length.isPresent());
            assertEquals(4, length.get());

            assertThrows(NullPointerException.class, () -> opt.map(s -> null));

            Optional<Integer> emptyMapped = Optional.<String> empty().map(String::length);
            assertFalse(emptyMapped.isPresent());
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
        public void testFlatMap() throws Exception {
            Optional<String> opt = Optional.of("test");

            Optional<Integer> result = opt.flatMap(s -> Optional.of(s.length()));
            assertTrue(result.isPresent());
            assertEquals(4, result.get());

            result = opt.flatMap(s -> Optional.empty());
            assertFalse(result.isPresent());

            Optional<Integer> emptyResult = Optional.<String> empty().flatMap(s -> Optional.of(s.length()));
            assertFalse(emptyResult.isPresent());

            assertThrows(NullPointerException.class, () -> opt.flatMap(s -> null));
        }

        @Test
        public void testContains() {
            Optional<String> opt = Optional.of("test");
            assertTrue(opt.contains("test"));
            assertFalse(opt.contains("other"));
            assertFalse(opt.contains(null));

            Optional<String> empty = Optional.empty();
            assertFalse(empty.contains("test"));
            assertFalse(empty.contains(null));
        }

        @Test
        public void testOr() {
            Optional<String> present = Optional.of("test");
            Optional<String> result = present.or(() -> Optional.of("other"));
            assertEquals("test", result.get());

            Optional<String> empty = Optional.empty();
            result = empty.or(() -> Optional.of("other"));
            assertEquals("other", result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElseNull() {
            assertEquals("test", Optional.of("test").orElseNull());
            assertNull(Optional.empty().orElseNull());
        }

        @Test
        public void testOrElse() {
            assertEquals("test", Optional.of("test").orElse("other"));
            assertEquals("other", Optional.empty().orElse("other"));
            assertNull(Optional.empty().orElse(null));
        }

        @Test
        public void testOrElseGet() {
            assertEquals("test", Optional.of("test").orElseGet(() -> "other"));
            assertEquals("other", Optional.empty().orElseGet(() -> "other"));
            assertNull(Optional.empty().orElseGet(() -> null));
        }

        @Test
        public void testOrElseThrow() {
            assertEquals("test", Optional.of("test").orElseThrow());
            assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow());

            assertEquals("test", Optional.of("test").orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals("test", Optional.of("test").orElseThrow("Value: %s", "param"));
            ex = assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Value: %s", "param"));
            assertTrue(ex.getMessage().contains("param"));

            assertEquals("test", Optional.of("test").orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> Optional.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            List<String> presentList = Optional.of("test").stream().toList();
            assertEquals(1, presentList.size());
            assertEquals("test", presentList.get(0));

            List<String> emptyList = Optional.<String> empty().stream().toList();
            assertEquals(0, emptyList.size());
        }

        @Test
        public void testToList() {
            List<String> presentList = Optional.of("test").toList();
            assertEquals(1, presentList.size());
            assertEquals("test", presentList.get(0));

            List<String> emptyList = Optional.<String> empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<String> presentSet = Optional.of("test").toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains("test"));

            Set<String> emptySet = Optional.<String> empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<String> presentList = Optional.of("test").toImmutableList();
            assertEquals(1, presentList.size());
            assertEquals("test", presentList.get(0));

            ImmutableList<String> emptyList = Optional.<String> empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<String> presentSet = Optional.of("test").toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains("test"));

            ImmutableSet<String> emptySet = Optional.<String> empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToJdkOptional() {
            java.util.Optional<String> present = Optional.of("test").toJdkOptional();
            assertTrue(present.isPresent());
            assertEquals("test", present.get());

            java.util.Optional<String> empty = Optional.<String> empty().toJdkOptional();
            assertFalse(empty.isPresent());

            java.util.Optional<String> presentDeprecated = Optional.of("test").__();
            assertTrue(presentDeprecated.isPresent());
            assertEquals("test", presentDeprecated.get());
        }

        @Test
        public void testEquals() {
            Optional<String> opt1 = Optional.of("test");
            Optional<String> opt2 = Optional.of("test");
            Optional<String> opt3 = Optional.of("other");
            Optional<String> empty1 = Optional.empty();
            Optional<String> empty2 = Optional.empty();

            assertEquals(opt1, opt1);

            assertEquals(opt1, opt2);
            assertEquals(opt2, opt1);

            assertEquals(empty1, empty2);

            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);

            assertNotEquals(opt1, null);
            assertNotEquals(opt1, "test");
        }

        @Test
        public void testHashCode() {
            Optional<String> opt1 = Optional.of("test");
            Optional<String> opt2 = Optional.of("test");
            Optional<String> opt3 = Optional.of("other");
            Optional<String> empty1 = Optional.empty();
            Optional<String> empty2 = Optional.empty();

            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(opt1.hashCode(), opt3.hashCode());
            assertNotEquals(opt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("Optional[test]", Optional.of("test").toString());
            assertEquals("Optional.empty", Optional.empty().toString());
            assertThrows(NullPointerException.class, () -> Optional.of((String) null).toString());
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
    }

    @Nested
    public class uOptionalBooleanTest {

        @Test
        public void testConstants() {
            assertTrue(OptionalBoolean.TRUE.isPresent());
            assertTrue(OptionalBoolean.TRUE.get());

            assertTrue(OptionalBoolean.FALSE.isPresent());
            assertFalse(OptionalBoolean.FALSE.get());
        }

        @Test
        public void testEmpty() {
            OptionalBoolean empty = OptionalBoolean.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testOf() {
            OptionalBoolean trueOpt = OptionalBoolean.of(true);
            assertTrue(trueOpt.isPresent());
            assertTrue(trueOpt.get());
            assertSame(OptionalBoolean.TRUE, trueOpt);

            OptionalBoolean falseOpt = OptionalBoolean.of(false);
            assertTrue(falseOpt.isPresent());
            assertFalse(falseOpt.get());
            assertSame(OptionalBoolean.FALSE, falseOpt);
        }

        @Test
        public void testOfNullable() {
            OptionalBoolean nullOpt = OptionalBoolean.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalBoolean trueOpt = OptionalBoolean.ofNullable(Boolean.TRUE);
            assertTrue(trueOpt.isPresent());
            assertTrue(trueOpt.get());

            OptionalBoolean falseOpt = OptionalBoolean.ofNullable(Boolean.FALSE);
            assertTrue(falseOpt.isPresent());
            assertFalse(falseOpt.get());
        }

        @Test
        public void testGet() {
            assertTrue(OptionalBoolean.of(true).get());
            assertFalse(OptionalBoolean.of(false).get());
            assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(OptionalBoolean.of(true).isPresent());
            assertTrue(OptionalBoolean.of(false).isPresent());
            assertFalse(OptionalBoolean.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(OptionalBoolean.of(true).isEmpty());
            assertFalse(OptionalBoolean.of(false).isEmpty());
            assertTrue(OptionalBoolean.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);

            OptionalBoolean.of(true).ifPresent(value -> {
                assertTrue(value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            OptionalBoolean.of(false).ifPresent(value -> {
                assertFalse(value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            OptionalBoolean.empty().ifPresent(value -> called.set(true));
            assertFalse(called.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

            OptionalBoolean.of(true).ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(emptyActionCalled.get());

            actionCalled.set(false);
            OptionalBoolean.empty().ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertFalse(actionCalled.get());
            assertTrue(emptyActionCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            OptionalBoolean trueOpt = OptionalBoolean.of(true);
            OptionalBoolean filtered = trueOpt.filter(value -> value);
            assertTrue(filtered.isPresent());
            assertTrue(filtered.get());

            filtered = trueOpt.filter(value -> !value);
            assertFalse(filtered.isPresent());

            OptionalBoolean falseOpt = OptionalBoolean.of(false);
            filtered = falseOpt.filter(value -> !value);
            assertTrue(filtered.isPresent());
            assertFalse(filtered.get());

            assertFalse(OptionalBoolean.empty().filter(value -> true).isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalBoolean mapped = OptionalBoolean.of(true).map(value -> !value);
            assertTrue(mapped.isPresent());
            assertFalse(mapped.get());

            mapped = OptionalBoolean.of(false).map(value -> !value);
            assertTrue(mapped.isPresent());
            assertTrue(mapped.get());

            assertFalse(OptionalBoolean.empty().map(value -> !value).isPresent());
        }

        @Test
        public void testMapToChar() throws Exception {
            OptionalChar charOpt = OptionalBoolean.of(true).mapToChar(b -> b ? 'T' : 'F');
            assertTrue(charOpt.isPresent());
            assertEquals('T', charOpt.get());

            charOpt = OptionalBoolean.of(false).mapToChar(b -> b ? 'T' : 'F');
            assertTrue(charOpt.isPresent());
            assertEquals('F', charOpt.get());

            assertFalse(OptionalBoolean.empty().mapToChar(b -> 'X').isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            OptionalInt intOpt = OptionalBoolean.of(true).mapToInt(b -> b ? 1 : 0);
            assertTrue(intOpt.isPresent());
            assertEquals(1, intOpt.get());

            intOpt = OptionalBoolean.of(false).mapToInt(b -> b ? 1 : 0);
            assertTrue(intOpt.isPresent());
            assertEquals(0, intOpt.get());

            assertFalse(OptionalBoolean.empty().mapToInt(b -> 0).isPresent());
        }

        @Test
        public void testMapToLong() throws Exception {
            OptionalLong longOpt = OptionalBoolean.of(true).mapToLong(b -> b ? 1L : 0L);
            assertTrue(longOpt.isPresent());
            assertEquals(1L, longOpt.get());

            assertFalse(OptionalBoolean.empty().mapToLong(b -> 0L).isPresent());
        }

        @Test
        public void testMapToDouble() throws Exception {
            OptionalDouble doubleOpt = OptionalBoolean.of(true).mapToDouble(b -> b ? 1.0 : 0.0);
            assertTrue(doubleOpt.isPresent());
            assertEquals(1.0, doubleOpt.get());

            assertFalse(OptionalBoolean.empty().mapToDouble(b -> 0.0).isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalBoolean.of(true).mapToObj(b -> b ? "yes" : "no");
            assertTrue(strOpt.isPresent());
            assertEquals("yes", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalBoolean.of(true).mapToObj(b -> null));

            assertFalse(OptionalBoolean.empty().mapToObj(b -> "test").isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalBoolean result = OptionalBoolean.of(true).flatMap(b -> OptionalBoolean.of(!b));
            assertTrue(result.isPresent());
            assertFalse(result.get());

            result = OptionalBoolean.of(true).flatMap(b -> OptionalBoolean.empty());
            assertFalse(result.isPresent());

            result = OptionalBoolean.empty().flatMap(b -> OptionalBoolean.of(true));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalBoolean.of(true).flatMap(b -> null));
        }

        @Test
        public void testOr() {
            OptionalBoolean present = OptionalBoolean.of(true);
            OptionalBoolean result = present.or(() -> OptionalBoolean.of(false));
            assertTrue(result.get());

            OptionalBoolean empty = OptionalBoolean.empty();
            result = empty.or(() -> OptionalBoolean.of(false));
            assertFalse(result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElse() {
            assertTrue(OptionalBoolean.of(true).orElse(false));
            assertFalse(OptionalBoolean.of(false).orElse(true));
            assertTrue(OptionalBoolean.empty().orElse(true));
            assertFalse(OptionalBoolean.empty().orElse(false));
        }

        @Test
        public void testOrElseGet() {
            assertTrue(OptionalBoolean.of(true).orElseGet(() -> false));
            assertFalse(OptionalBoolean.of(false).orElseGet(() -> true));
            assertTrue(OptionalBoolean.empty().orElseGet(() -> true));
            assertFalse(OptionalBoolean.empty().orElseGet(() -> false));
        }

        @Test
        public void testOrElseThrow() {
            assertTrue(OptionalBoolean.of(true).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow());

            assertTrue(OptionalBoolean.of(true).orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertTrue(OptionalBoolean.of(true).orElseThrow("Value: %s", "param"));
            ex = assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Value: %s", "param"));
            assertTrue(ex.getMessage().contains("param"));

            assertTrue(OptionalBoolean.of(true).orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalBoolean.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            List<Boolean> presentList = OptionalBoolean.of(true).stream().toList();
            assertEquals(1, presentList.size());
            assertTrue(presentList.get(0));

            List<Boolean> emptyList = OptionalBoolean.empty().stream().toList();
            assertEquals(0, emptyList.size());
        }

        @Test
        public void testToList() {
            List<Boolean> trueList = OptionalBoolean.of(true).toList();
            assertEquals(1, trueList.size());
            assertTrue(trueList.get(0));

            List<Boolean> falseList = OptionalBoolean.of(false).toList();
            assertEquals(1, falseList.size());
            assertFalse(falseList.get(0));

            List<Boolean> emptyList = OptionalBoolean.empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<Boolean> trueSet = OptionalBoolean.of(true).toSet();
            assertEquals(1, trueSet.size());
            assertTrue(trueSet.contains(true));

            Set<Boolean> emptySet = OptionalBoolean.empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<Boolean> presentList = OptionalBoolean.of(true).toImmutableList();
            assertEquals(1, presentList.size());
            assertTrue(presentList.get(0));

            ImmutableList<Boolean> emptyList = OptionalBoolean.empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<Boolean> presentSet = OptionalBoolean.of(true).toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(true));

            ImmutableSet<Boolean> emptySet = OptionalBoolean.empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testBoxed() {
            Optional<Boolean> boxedTrue = OptionalBoolean.of(true).boxed();
            assertTrue(boxedTrue.isPresent());
            assertTrue(boxedTrue.get());

            Optional<Boolean> boxedFalse = OptionalBoolean.of(false).boxed();
            assertTrue(boxedFalse.isPresent());
            assertFalse(boxedFalse.get());

            Optional<Boolean> boxedEmpty = OptionalBoolean.empty().boxed();
            assertFalse(boxedEmpty.isPresent());
        }

        @Test
        public void testCompareTo() {
            OptionalBoolean trueOpt = OptionalBoolean.of(true);
            OptionalBoolean falseOpt = OptionalBoolean.of(false);
            OptionalBoolean empty = OptionalBoolean.empty();

            assertEquals(0, trueOpt.compareTo(OptionalBoolean.of(true)));
            assertEquals(0, falseOpt.compareTo(OptionalBoolean.of(false)));
            assertEquals(0, empty.compareTo(OptionalBoolean.empty()));

            assertTrue(trueOpt.compareTo(falseOpt) > 0);
            assertTrue(falseOpt.compareTo(trueOpt) < 0);

            assertTrue(trueOpt.compareTo(empty) > 0);
            assertTrue(falseOpt.compareTo(empty) > 0);
            assertTrue(empty.compareTo(trueOpt) < 0);

            assertTrue(trueOpt.compareTo(null) > 0);
            assertEquals(0, empty.compareTo(null));
        }

        @Test
        public void testEquals() {
            OptionalBoolean trueOpt1 = OptionalBoolean.of(true);
            OptionalBoolean trueOpt2 = OptionalBoolean.of(true);
            OptionalBoolean falseOpt = OptionalBoolean.of(false);
            OptionalBoolean empty1 = OptionalBoolean.empty();
            OptionalBoolean empty2 = OptionalBoolean.empty();

            assertEquals(trueOpt1, trueOpt1);

            assertEquals(trueOpt1, trueOpt2);
            assertEquals(trueOpt2, trueOpt1);

            assertEquals(empty1, empty2);

            assertNotEquals(trueOpt1, falseOpt);
            assertNotEquals(trueOpt1, empty1);

            assertNotEquals(trueOpt1, null);
            assertNotEquals(trueOpt1, "true");
        }

        @Test
        public void testHashCode() {
            OptionalBoolean trueOpt1 = OptionalBoolean.of(true);
            OptionalBoolean trueOpt2 = OptionalBoolean.of(true);
            OptionalBoolean falseOpt = OptionalBoolean.of(false);
            OptionalBoolean empty1 = OptionalBoolean.empty();
            OptionalBoolean empty2 = OptionalBoolean.empty();

            assertEquals(trueOpt1.hashCode(), trueOpt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(trueOpt1.hashCode(), falseOpt.hashCode());
            assertNotEquals(trueOpt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("OptionalBoolean[true]", OptionalBoolean.of(true).toString());
            assertEquals("OptionalBoolean[false]", OptionalBoolean.of(false).toString());
            assertEquals("OptionalBoolean.empty", OptionalBoolean.empty().toString());
        }

        @Test
        public void testImmutability() {
            OptionalBoolean original = OptionalBoolean.of(true);
            OptionalBoolean mapped = original.map(b -> !b);

            assertTrue(original.get());
            assertFalse(mapped.get());
            assertNotSame(original, mapped);
        }
    }

    @Nested
    public class uOptionalCharTest {

        @Test
        public void testEmpty() {
            OptionalChar empty = OptionalChar.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testOf() {
            OptionalChar cached1 = OptionalChar.of('A');
            OptionalChar cached2 = OptionalChar.of('A');
            assertSame(cached1, cached2);

            OptionalChar cached3 = OptionalChar.of((char) 0);
            OptionalChar cached4 = OptionalChar.of((char) 0);
            assertSame(cached3, cached4);

            OptionalChar cached5 = OptionalChar.of((char) 128);
            OptionalChar cached6 = OptionalChar.of((char) 128);
            assertSame(cached5, cached6);

            OptionalChar nonCached1 = OptionalChar.of((char) 200);
            OptionalChar nonCached2 = OptionalChar.of((char) 200);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);

            OptionalChar opt = OptionalChar.of('Z');
            assertTrue(opt.isPresent());
            assertEquals('Z', opt.get());
        }

        @Test
        public void testOfNullable() {
            OptionalChar nullOpt = OptionalChar.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalChar opt = OptionalChar.ofNullable('X');
            assertTrue(opt.isPresent());
            assertEquals('X', opt.get());
        }

        @Test
        public void testGet() {
            assertEquals('A', OptionalChar.of('A').get());
            assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(OptionalChar.of('A').isPresent());
            assertFalse(OptionalChar.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(OptionalChar.of('A').isEmpty());
            assertTrue(OptionalChar.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);

            OptionalChar.of('X').ifPresent(value -> {
                assertEquals('X', value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            OptionalChar.empty().ifPresent(value -> called.set(true));
            assertFalse(called.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

            OptionalChar.of('A').ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(emptyActionCalled.get());

            actionCalled.set(false);
            OptionalChar.empty().ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertFalse(actionCalled.get());
            assertTrue(emptyActionCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            OptionalChar opt = OptionalChar.of('A');

            OptionalChar filtered = opt.filter(c -> Character.isUpperCase(c));
            assertTrue(filtered.isPresent());
            assertEquals('A', filtered.get());

            filtered = opt.filter(c -> Character.isLowerCase(c));
            assertFalse(filtered.isPresent());

            assertFalse(OptionalChar.empty().filter(c -> true).isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalChar mapped = OptionalChar.of('a').map(Character::toUpperCase);
            assertTrue(mapped.isPresent());
            assertEquals('A', mapped.get());

            assertFalse(OptionalChar.empty().map(Character::toUpperCase).isPresent());
        }

        @Test
        public void testMapToBoolean() throws Exception {
            OptionalBoolean boolOpt = OptionalChar.of('A').mapToBoolean(Character::isUpperCase);
            assertTrue(boolOpt.isPresent());
            assertTrue(boolOpt.get());

            boolOpt = OptionalChar.of('a').mapToBoolean(Character::isUpperCase);
            assertTrue(boolOpt.isPresent());
            assertFalse(boolOpt.get());

            assertFalse(OptionalChar.empty().mapToBoolean(Character::isUpperCase).isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            OptionalInt intOpt = OptionalChar.of('A').mapToInt(c -> (int) c);
            assertTrue(intOpt.isPresent());
            assertEquals(65, intOpt.get());

            assertFalse(OptionalChar.empty().mapToInt(c -> 0).isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalChar.of('X').mapToObj(c -> "Char: " + c);
            assertTrue(strOpt.isPresent());
            assertEquals("Char: X", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalChar.of('X').mapToObj(c -> null));

            assertFalse(OptionalChar.empty().mapToObj(c -> "test").isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalChar result = OptionalChar.of('a').flatMap(c -> OptionalChar.of(Character.toUpperCase(c)));
            assertTrue(result.isPresent());
            assertEquals('A', result.get());

            result = OptionalChar.of('X').flatMap(c -> OptionalChar.empty());
            assertFalse(result.isPresent());

            result = OptionalChar.empty().flatMap(c -> OptionalChar.of('Z'));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalChar.of('X').flatMap(c -> null));
        }

        @Test
        public void testOr() {
            OptionalChar present = OptionalChar.of('A');
            OptionalChar result = present.or(() -> OptionalChar.of('B'));
            assertEquals('A', result.get());

            OptionalChar empty = OptionalChar.empty();
            result = empty.or(() -> OptionalChar.of('B'));
            assertEquals('B', result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElseZero() {
            assertEquals('X', OptionalChar.of('X').orElseZero());
            assertEquals('\0', OptionalChar.empty().orElseZero());
        }

        @Test
        public void testOrElse() {
            assertEquals('A', OptionalChar.of('A').orElse('B'));
            assertEquals('B', OptionalChar.empty().orElse('B'));
        }

        @Test
        public void testOrElseGet() {
            assertEquals('A', OptionalChar.of('A').orElseGet(() -> 'B'));
            assertEquals('B', OptionalChar.empty().orElseGet(() -> 'B'));
        }

        @Test
        public void testOrElseThrow() {
            assertEquals('A', OptionalChar.of('A').orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow());

            assertEquals('A', OptionalChar.of('A').orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals('A', OptionalChar.of('A').orElseThrow("Char: %c", 'X'));
            ex = assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Char: %c", 'X'));
            assertTrue(ex.getMessage().contains("X"));

            assertEquals('A', OptionalChar.of('A').orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalChar.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            char[] presentArray = OptionalChar.of('X').stream().toArray();
            assertEquals(1, presentArray.length);
            assertEquals('X', presentArray[0]);

            char[] emptyArray = OptionalChar.empty().stream().toArray();
            assertEquals(0, emptyArray.length);
        }

        @Test
        public void testToList() {
            List<Character> presentList = OptionalChar.of('X').toList();
            assertEquals(1, presentList.size());
            assertEquals('X', presentList.get(0));

            List<Character> emptyList = OptionalChar.empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<Character> presentSet = OptionalChar.of('X').toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains('X'));

            Set<Character> emptySet = OptionalChar.empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<Character> presentList = OptionalChar.of('X').toImmutableList();
            assertEquals(1, presentList.size());
            assertEquals('X', presentList.get(0));

            ImmutableList<Character> emptyList = OptionalChar.empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<Character> presentSet = OptionalChar.of('X').toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains('X'));

            ImmutableSet<Character> emptySet = OptionalChar.empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testBoxed() {
            Optional<Character> boxed = OptionalChar.of('X').boxed();
            assertTrue(boxed.isPresent());
            assertEquals('X', boxed.get());

            Optional<Character> boxedEmpty = OptionalChar.empty().boxed();
            assertFalse(boxedEmpty.isPresent());
        }

        @Test
        public void testCompareTo() {
            OptionalChar optA = OptionalChar.of('A');
            OptionalChar optZ = OptionalChar.of('Z');
            OptionalChar empty = OptionalChar.empty();

            assertEquals(0, optA.compareTo(OptionalChar.of('A')));
            assertEquals(0, empty.compareTo(OptionalChar.empty()));

            assertTrue(optA.compareTo(optZ) < 0);
            assertTrue(optZ.compareTo(optA) > 0);

            assertTrue(optA.compareTo(empty) > 0);
            assertTrue(empty.compareTo(optA) < 0);

            assertTrue(optA.compareTo(null) > 0);
            assertEquals(0, empty.compareTo(null));
        }

        @Test
        public void testEquals() {
            OptionalChar opt1 = OptionalChar.of('X');
            OptionalChar opt2 = OptionalChar.of('X');
            OptionalChar opt3 = OptionalChar.of('Y');
            OptionalChar empty1 = OptionalChar.empty();
            OptionalChar empty2 = OptionalChar.empty();

            assertEquals(opt1, opt1);

            assertEquals(opt1, opt2);
            assertEquals(opt2, opt1);

            assertEquals(empty1, empty2);

            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);

            assertNotEquals(opt1, null);
            assertNotEquals(opt1, 'X');
        }

        @Test
        public void testHashCode() {
            OptionalChar opt1 = OptionalChar.of('X');
            OptionalChar opt2 = OptionalChar.of('X');
            OptionalChar opt3 = OptionalChar.of('Y');
            OptionalChar empty1 = OptionalChar.empty();
            OptionalChar empty2 = OptionalChar.empty();

            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(opt1.hashCode(), opt3.hashCode());
            assertNotEquals(opt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("OptionalChar[A]", OptionalChar.of('A').toString());
            assertEquals("OptionalChar[\0]", OptionalChar.of('\0').toString());
            assertEquals("OptionalChar.empty", OptionalChar.empty().toString());
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
    }

    @Nested
    public class uOptionalIntTest {

        @Test
        public void testEmpty() {
            OptionalInt empty = OptionalInt.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
            assertThrows(NoSuchElementException.class, () -> empty.getAsInt());
        }

        @Test
        public void testOf() {
            OptionalInt cached1 = OptionalInt.of(0);
            OptionalInt cached2 = OptionalInt.of(0);
            assertSame(cached1, cached2);

            OptionalInt cached3 = OptionalInt.of(-256);
            OptionalInt cached4 = OptionalInt.of(-256);
            assertSame(cached3, cached4);

            OptionalInt cached5 = OptionalInt.of(1024);
            OptionalInt cached6 = OptionalInt.of(1024);
            assertSame(cached5, cached6);

            OptionalInt nonCached1 = OptionalInt.of(2000);
            OptionalInt nonCached2 = OptionalInt.of(2000);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);

            OptionalInt opt = OptionalInt.of(42);
            assertTrue(opt.isPresent());
            assertEquals(42, opt.get());
        }

        @Test
        public void testOfNullable() {
            OptionalInt nullOpt = OptionalInt.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalInt opt = OptionalInt.ofNullable(42);
            assertTrue(opt.isPresent());
            assertEquals(42, opt.get());
        }

        @Test
        public void testFromJavaOptional() {
            OptionalInt fromPresent = OptionalInt.from(java.util.OptionalInt.of(42));
            assertTrue(fromPresent.isPresent());
            assertEquals(42, fromPresent.get());

            OptionalInt fromEmpty = OptionalInt.from(java.util.OptionalInt.empty());
            assertFalse(fromEmpty.isPresent());
        }

        @Test
        public void testGet() {
            assertEquals(42, OptionalInt.of(42).get());
            assertEquals(42, OptionalInt.of(42).getAsInt());
            assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().get());
            assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().getAsInt());
        }

        @Test
        public void testIsPresent() {
            assertTrue(OptionalInt.of(42).isPresent());
            assertFalse(OptionalInt.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(OptionalInt.of(42).isEmpty());
            assertTrue(OptionalInt.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicInteger value = new AtomicInteger(0);

            OptionalInt.of(42).ifPresent(v -> value.set(v));
            assertEquals(42, value.get());

            value.set(0);
            OptionalInt.empty().ifPresent(v -> value.set(v));
            assertEquals(0, value.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicInteger value = new AtomicInteger(0);
            AtomicBoolean emptyCalled = new AtomicBoolean(false);

            OptionalInt.of(42).ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(42, value.get());
            assertFalse(emptyCalled.get());

            value.set(0);
            OptionalInt.empty().ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(0, value.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            OptionalInt opt = OptionalInt.of(42);

            OptionalInt filtered = opt.filter(v -> v > 40);
            assertTrue(filtered.isPresent());
            assertEquals(42, filtered.get());

            filtered = opt.filter(v -> v > 50);
            assertFalse(filtered.isPresent());

            assertFalse(OptionalInt.empty().filter(v -> true).isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalInt mapped = OptionalInt.of(42).map(v -> v * 2);
            assertTrue(mapped.isPresent());
            assertEquals(84, mapped.get());

            assertFalse(OptionalInt.empty().map(v -> v * 2).isPresent());
        }

        @Test
        public void testMapToBoolean() throws Exception {
            OptionalBoolean boolOpt = OptionalInt.of(42).mapToBoolean(v -> v > 40);
            assertTrue(boolOpt.isPresent());
            assertTrue(boolOpt.get());

            assertFalse(OptionalInt.empty().mapToBoolean(v -> true).isPresent());
        }

        @Test
        public void testMapToChar() throws Exception {
            OptionalChar charOpt = OptionalInt.of(65).mapToChar(v -> (char) v.intValue());
            assertTrue(charOpt.isPresent());
            assertEquals('A', charOpt.get());

            assertFalse(OptionalInt.empty().mapToChar(v -> 'A').isPresent());
        }

        @Test
        public void testMapToLong() throws Exception {
            OptionalLong longOpt = OptionalInt.of(42).mapToLong(v -> v * 1000L);
            assertTrue(longOpt.isPresent());
            assertEquals(42000L, longOpt.get());

            assertFalse(OptionalInt.empty().mapToLong(v -> 0L).isPresent());
        }

        @Test
        public void testMapToFloat() throws Exception {
            OptionalFloat floatOpt = OptionalInt.of(42).mapToFloat(v -> v / 2.0f);
            assertTrue(floatOpt.isPresent());
            assertEquals(21.0f, floatOpt.get());

            assertFalse(OptionalInt.empty().mapToFloat(v -> 0.0f).isPresent());
        }

        @Test
        public void testMapToDouble() throws Exception {
            OptionalDouble doubleOpt = OptionalInt.of(42).mapToDouble(v -> v / 2.0);
            assertTrue(doubleOpt.isPresent());
            assertEquals(21.0, doubleOpt.get());

            assertFalse(OptionalInt.empty().mapToDouble(v -> 0.0).isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalInt.of(42).mapToObj(v -> "Number: " + v);
            assertTrue(strOpt.isPresent());
            assertEquals("Number: 42", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalInt.of(42).mapToObj(v -> null));

            assertFalse(OptionalInt.empty().mapToObj(v -> "test").isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalInt result = OptionalInt.of(42).flatMap(v -> OptionalInt.of(v * 2));
            assertTrue(result.isPresent());
            assertEquals(84, result.get());

            result = OptionalInt.of(42).flatMap(v -> OptionalInt.empty());
            assertFalse(result.isPresent());

            result = OptionalInt.empty().flatMap(v -> OptionalInt.of(100));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalInt.of(42).flatMap(v -> null));
        }

        @Test
        public void testOr() {
            OptionalInt present = OptionalInt.of(42);
            OptionalInt result = present.or(() -> OptionalInt.of(100));
            assertEquals(42, result.get());

            OptionalInt empty = OptionalInt.empty();
            result = empty.or(() -> OptionalInt.of(100));
            assertEquals(100, result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElseZero() {
            assertEquals(42, OptionalInt.of(42).orElseZero());
            assertEquals(0, OptionalInt.empty().orElseZero());
        }

        @Test
        public void testOrElse() {
            assertEquals(42, OptionalInt.of(42).orElse(100));
            assertEquals(100, OptionalInt.empty().orElse(100));
            assertEquals(-1, OptionalInt.empty().orElse(-1));
        }

        @Test
        public void testOrElseGet() {
            assertEquals(42, OptionalInt.of(42).orElseGet(() -> 100));
            assertEquals(100, OptionalInt.empty().orElseGet(() -> 100));

            AtomicInteger counter = new AtomicInteger(0);
            OptionalInt.of(42).orElseGet(() -> counter.incrementAndGet());
            assertEquals(0, counter.get());

            OptionalInt.empty().orElseGet(() -> counter.incrementAndGet());
            assertEquals(1, counter.get());
        }

        @Test
        public void testOrElseThrow() {
            assertEquals(42, OptionalInt.of(42).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow());

            assertEquals(42, OptionalInt.of(42).orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals(42, OptionalInt.of(42).orElseThrow("Value: %d", 999));
            ex = assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Value: %d", 999));
            assertTrue(ex.getMessage().contains("999"));

            assertEquals(42, OptionalInt.of(42).orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalInt.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            int[] presentArray = OptionalInt.of(42).stream().toArray();
            assertEquals(1, presentArray.length);
            assertEquals(42, presentArray[0]);

            int[] emptyArray = OptionalInt.empty().stream().toArray();
            assertEquals(0, emptyArray.length);
        }

        @Test
        public void testToList() {
            List<Integer> presentList = OptionalInt.of(42).toList();
            assertEquals(1, presentList.size());
            assertEquals(42, presentList.get(0));

            List<Integer> emptyList = OptionalInt.empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<Integer> presentSet = OptionalInt.of(42).toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42));

            Set<Integer> emptySet = OptionalInt.empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<Integer> presentList = OptionalInt.of(42).toImmutableList();
            assertEquals(1, presentList.size());
            assertEquals(42, presentList.get(0));

            ImmutableList<Integer> emptyList = OptionalInt.empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<Integer> presentSet = OptionalInt.of(42).toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42));

            ImmutableSet<Integer> emptySet = OptionalInt.empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testBoxed() {
            Optional<Integer> boxed = OptionalInt.of(42).boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42, boxed.get());

            Optional<Integer> boxedEmpty = OptionalInt.empty().boxed();
            assertFalse(boxedEmpty.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            java.util.OptionalInt jdkOpt = OptionalInt.of(42).toJdkOptional();
            assertTrue(jdkOpt.isPresent());
            assertEquals(42, jdkOpt.getAsInt());

            java.util.OptionalInt jdkEmpty = OptionalInt.empty().toJdkOptional();
            assertFalse(jdkEmpty.isPresent());

            java.util.OptionalInt jdkOptDeprecated = OptionalInt.of(42).__();
            assertTrue(jdkOptDeprecated.isPresent());
            assertEquals(42, jdkOptDeprecated.getAsInt());
        }

        @Test
        public void testCompareTo() {
            OptionalInt opt42 = OptionalInt.of(42);
            OptionalInt opt100 = OptionalInt.of(100);
            OptionalInt optNeg = OptionalInt.of(-10);
            OptionalInt empty = OptionalInt.empty();

            assertEquals(0, opt42.compareTo(OptionalInt.of(42)));
            assertEquals(0, empty.compareTo(OptionalInt.empty()));

            assertTrue(opt42.compareTo(opt100) < 0);
            assertTrue(opt100.compareTo(opt42) > 0);
            assertTrue(optNeg.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(empty) > 0);
            assertTrue(empty.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(null) > 0);
            assertEquals(0, empty.compareTo(null));
        }

        @Test
        public void testEquals() {
            OptionalInt opt1 = OptionalInt.of(42);
            OptionalInt opt2 = OptionalInt.of(42);
            OptionalInt opt3 = OptionalInt.of(100);
            OptionalInt empty1 = OptionalInt.empty();
            OptionalInt empty2 = OptionalInt.empty();

            assertEquals(opt1, opt1);

            assertEquals(opt1, opt2);
            assertEquals(opt2, opt1);

            assertEquals(empty1, empty2);

            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);

            assertNotEquals(opt1, null);
            assertNotEquals(opt1, 42);
        }

        @Test
        public void testHashCode() {
            OptionalInt opt1 = OptionalInt.of(42);
            OptionalInt opt2 = OptionalInt.of(42);
            OptionalInt opt3 = OptionalInt.of(100);
            OptionalInt empty1 = OptionalInt.empty();
            OptionalInt empty2 = OptionalInt.empty();

            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(opt1.hashCode(), opt3.hashCode());
            assertNotEquals(opt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("OptionalInt[42]", OptionalInt.of(42).toString());
            assertEquals("OptionalInt[-100]", OptionalInt.of(-100).toString());
            assertEquals("OptionalInt.empty", OptionalInt.empty().toString());
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
    }

    @Nested
    public class uNullableTest {

        @Test
        public void testConstants() {
            assertTrue(Nullable.TRUE.isPresent());
            assertTrue(Nullable.TRUE.get());

            assertTrue(Nullable.FALSE.isPresent());
            assertFalse(Nullable.FALSE.get());
        }

        @Test
        public void testEmpty() {
            Nullable<String> empty = Nullable.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isNotPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testOfString() {
            Nullable<String> nonEmpty = Nullable.of("test");
            assertTrue(nonEmpty.isPresent());
            assertEquals("test", nonEmpty.get());

            Nullable<String> nullString = Nullable.of((String) null);
            assertTrue(nullString.isPresent());
            assertNull(nullString.get());
            assertTrue(nullString.isNull());

            Nullable<String> emptyString = Nullable.of("");
            assertTrue(emptyString.isPresent());
            assertEquals("", emptyString.get());
            assertFalse(emptyString.isNull());
        }

        @Test
        public void testOf() {
            Nullable<Integer> nonNull = Nullable.of(42);
            assertTrue(nonNull.isPresent());
            assertEquals(42, nonNull.get());
            assertFalse(nonNull.isNull());
            assertTrue(nonNull.isNotNull());

            Nullable<Integer> nullValue = Nullable.of((Integer) null);
            assertTrue(nullValue.isPresent());
            assertNull(nullValue.get());
            assertTrue(nullValue.isNull());
            assertFalse(nullValue.isNotNull());
        }

        @Test
        public void testFromOptional() {
            Optional<String> present = Optional.of("test");
            Nullable<String> fromPresent = Nullable.from(present);
            assertTrue(fromPresent.isPresent());
            assertEquals("test", fromPresent.get());

            Optional<String> empty = Optional.empty();
            Nullable<String> fromEmpty = Nullable.from(empty);
            assertFalse(fromEmpty.isPresent());
        }

        @Test
        public void testFromJavaOptional() {
            java.util.Optional<String> present = java.util.Optional.of("test");
            Nullable<String> fromPresent = Nullable.from(present);
            assertTrue(fromPresent.isPresent());
            assertEquals("test", fromPresent.get());

            java.util.Optional<String> empty = java.util.Optional.empty();
            Nullable<String> fromEmpty = Nullable.from(empty);
            assertFalse(fromEmpty.isPresent());
        }

        @Test
        public void testGet() {
            assertEquals("test", Nullable.of("test").get());
            assertNull(Nullable.of((String) null).get());
            assertThrows(NoSuchElementException.class, () -> Nullable.empty().get());
        }

        @Test
        public void testIsPresent() {
            assertTrue(Nullable.of("test").isPresent());
            assertTrue(Nullable.of((String) null).isPresent());
            assertFalse(Nullable.empty().isPresent());
        }

        @Test
        public void testIsNotPresent() {
            assertFalse(Nullable.of("test").isNotPresent());
            assertFalse(Nullable.of((String) null).isNotPresent());
            assertTrue(Nullable.empty().isNotPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(Nullable.of("test").isEmpty());
            assertFalse(Nullable.of((String) null).isEmpty());
            assertTrue(Nullable.empty().isEmpty());
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
        public void testIfPresent() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);

            Nullable.of("test").ifPresent(value -> {
                assertEquals("test", value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            Nullable.of((String) null).ifPresent(value -> {
                assertNull(value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            Nullable.empty().ifPresent(value -> called.set(true));
            assertFalse(called.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

            Nullable.of("test").ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(emptyActionCalled.get());

            actionCalled.set(false);
            emptyActionCalled.set(false);
            Nullable.of((String) null).ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(emptyActionCalled.get());

            actionCalled.set(false);
            Nullable.empty().ifPresentOrElse(value -> actionCalled.set(true), () -> emptyActionCalled.set(true));
            assertFalse(actionCalled.get());
            assertTrue(emptyActionCalled.get());
        }

        @Test
        public void testIfNotNull() throws Exception {
            AtomicBoolean called = new AtomicBoolean(false);

            Nullable.of("test").ifNotNull(value -> {
                assertEquals("test", value);
                called.set(true);
            });
            assertTrue(called.get());

            called.set(false);
            Nullable.of((String) null).ifNotNull(value -> called.set(true));
            assertFalse(called.get());

            called.set(false);
            Nullable.empty().ifNotNull(value -> called.set(true));
            assertFalse(called.get());
        }

        @Test
        public void testIfNotNullOrElse() throws Exception {
            AtomicBoolean actionCalled = new AtomicBoolean(false);
            AtomicBoolean elseActionCalled = new AtomicBoolean(false);

            Nullable.of("test").ifNotNullOrElse(value -> actionCalled.set(true), () -> elseActionCalled.set(true));
            assertTrue(actionCalled.get());
            assertFalse(elseActionCalled.get());

            actionCalled.set(false);
            Nullable.of((String) null).ifNotNullOrElse(value -> actionCalled.set(true), () -> elseActionCalled.set(true));
            assertFalse(actionCalled.get());
            assertTrue(elseActionCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            Nullable<String> filtered = Nullable.of("test").filter(s -> s.length() > 3);
            assertTrue(filtered.isPresent());
            assertEquals("test", filtered.get());

            filtered = Nullable.of("ab").filter(s -> s.length() > 3);
            assertFalse(filtered.isPresent());

            filtered = Nullable.of((String) null).filter(s -> s != null);
            assertFalse(filtered.isPresent());

            assertFalse(Nullable.<String> empty().filter(s -> true).isPresent());
        }

        @Test
        public void testFilterIfNotNull() throws Exception {
            Optional<String> filtered = Nullable.of("test").filterIfNotNull(s -> s.length() > 3);
            assertTrue(filtered.isPresent());
            assertEquals("test", filtered.get());

            filtered = Nullable.of("ab").filterIfNotNull(s -> s.length() > 3);
            assertFalse(filtered.isPresent());

            filtered = Nullable.of((String) null).filterIfNotNull(s -> {
                fail("Predicate should not be called for null value");
                return true;
            });
            assertFalse(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            Nullable<Integer> length = Nullable.of("test").map(String::length);
            assertTrue(length.isPresent());
            assertEquals(4, length.get());

            Nullable<String> nullResult = Nullable.of("test").map(s -> null);
            assertTrue(nullResult.isPresent());
            assertNull(nullResult.get());

            Nullable<String> fromNull = Nullable.of((String) null).map(s -> s == null ? "was null" : s);
            assertTrue(fromNull.isPresent());
            assertEquals("was null", fromNull.get());

            assertFalse(Nullable.<String> empty().map(String::length).isPresent());
        }

        @Test
        public void testMapToNonNull() throws Exception {
            Optional<Integer> length = Nullable.of("test").mapToNonNull(String::length);
            assertTrue(length.isPresent());
            assertEquals(4, length.get());

            assertThrows(NullPointerException.class, () -> Nullable.of("test").mapToNonNull(s -> null));

            assertFalse(Nullable.<String> empty().mapToNonNull(String::length).isPresent());
        }

        @Test
        public void testMapIfNotNull() throws Exception {
            Nullable<Integer> length = Nullable.of("test").mapIfNotNull(String::length);
            assertTrue(length.isPresent());
            assertEquals(4, length.get());

            Nullable<Integer> fromNull = Nullable.of((String) null).mapIfNotNull(s -> {
                fail("Mapper should not be called for null value");
                return s.length();
            });
            assertFalse(fromNull.isPresent());

            assertFalse(Nullable.<String> empty().mapIfNotNull(String::length).isPresent());
        }

        @Test
        public void testMapToNonNullIfNotNull() throws Exception {
            Optional<Integer> length = Nullable.of("test").mapToNonNullIfNotNull(String::length);
            assertTrue(length.isPresent());
            assertEquals(4, length.get());

            Optional<Integer> fromNull = Nullable.of((String) null).mapToNonNullIfNotNull(s -> {
                fail("Mapper should not be called for null value");
                return s.length();
            });
            assertFalse(fromNull.isPresent());

            assertThrows(NullPointerException.class, () -> Nullable.of("test").mapToNonNullIfNotNull(s -> null));
        }

        @Test
        public void testMapToPrimitives() throws Exception {
            Nullable<String> nullable = Nullable.of("test");
            Nullable<String> nullValue = Nullable.of((String) null);
            Nullable<String> empty = Nullable.empty();

            assertTrue(nullable.mapToBoolean(s -> s.length() > 3).isPresent());
            assertTrue(nullable.mapToChar(s -> s.charAt(0)).isPresent());
            assertTrue(nullable.mapToByte(s -> (byte) s.length()).isPresent());
            assertTrue(nullable.mapToShort(s -> (short) s.length()).isPresent());
            assertTrue(nullable.mapToInt(String::length).isPresent());
            assertTrue(nullable.mapToLong(s -> (long) s.length()).isPresent());
            assertTrue(nullable.mapToFloat(s -> (float) s.length()).isPresent());
            assertTrue(nullable.mapToDouble(s -> (double) s.length()).isPresent());

            assertFalse(empty.mapToBoolean(s -> true).isPresent());
            assertFalse(empty.mapToChar(s -> 'a').isPresent());
            assertFalse(empty.mapToByte(s -> (byte) 1).isPresent());
            assertFalse(empty.mapToShort(s -> (short) 1).isPresent());
            assertFalse(empty.mapToInt(s -> 1).isPresent());
            assertFalse(empty.mapToLong(s -> 1L).isPresent());
            assertFalse(empty.mapToFloat(s -> 1.0f).isPresent());
            assertFalse(empty.mapToDouble(s -> 1.0).isPresent());
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
        public void testFlatMap() throws Exception {
            Nullable<Integer> result = Nullable.of("test").flatMap(s -> Nullable.of(s.length()));
            assertTrue(result.isPresent());
            assertEquals(4, result.get());

            result = Nullable.of("test").flatMap(s -> Nullable.empty());
            assertFalse(result.isPresent());

            result = Nullable.<String> empty().flatMap(s -> Nullable.of(100));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> Nullable.of("test").flatMap(s -> null));
        }

        @Test
        public void testFlatMapIfNotNull() throws Exception {
            Nullable<Integer> result = Nullable.of("test").flatMapIfNotNull(s -> Nullable.of(s.length()));
            assertTrue(result.isPresent());
            assertEquals(4, result.get());

            result = Nullable.of((String) null).flatMapIfNotNull(s -> {
                fail("Mapper should not be called for null value");
                return Nullable.of(100);
            });
            assertFalse(result.isPresent());
        }

        @Test
        public void testContains() {
            assertTrue(Nullable.of("test").contains("test"));
            assertFalse(Nullable.of("test").contains("other"));
            assertFalse(Nullable.of("test").contains(null));

            assertTrue(Nullable.of((String) null).contains(null));
            assertFalse(Nullable.of((String) null).contains("test"));

            assertFalse(Nullable.empty().contains("test"));
            assertFalse(Nullable.empty().contains(null));
        }

        @Test
        public void testOr() {
            Nullable<String> present = Nullable.of("test");
            Nullable<String> result = present.or(() -> Nullable.of("other"));
            assertEquals("test", result.get());

            Nullable<String> empty = Nullable.empty();
            result = empty.or(() -> Nullable.of("other"));
            assertEquals("other", result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrIfNull() {
            Nullable<String> nonNull = Nullable.of("test");
            Nullable<String> result = nonNull.orIfNull(() -> Nullable.of("other"));
            assertEquals("test", result.get());

            Nullable<String> nullValue = Nullable.of((String) null);
            result = nullValue.orIfNull(() -> Nullable.of("other"));
            assertEquals("other", result.get());

            Nullable<String> empty = Nullable.empty();
            result = empty.orIfNull(() -> Nullable.of("other"));
            assertTrue(result.isPresent());
        }

        @Test
        public void testOrElseNull() {
            assertEquals("test", Nullable.of("test").orElseNull());
            assertNull(Nullable.of((String) null).orElseNull());
            assertNull(Nullable.empty().orElseNull());
        }

        @Test
        public void testOrElse() {
            assertEquals("test", Nullable.of("test").orElse("other"));
            assertNull(Nullable.of((String) null).orElse("other"));
            assertEquals("other", Nullable.empty().orElse("other"));
        }

        @Test
        public void testOrElseIfNull() {
            assertEquals("test", Nullable.of("test").orElseIfNull("other"));
            assertEquals("other", Nullable.of((String) null).orElseIfNull("other"));
            assertEquals("other", Nullable.empty().orElseIfNull("other"));
        }

        @Test
        public void testOrElseGet() {
            assertEquals("test", Nullable.of("test").orElseGet(() -> "other"));
            assertNull(Nullable.of((String) null).orElseGet(() -> "other"));
            assertEquals("other", Nullable.empty().orElseGet(() -> "other"));
        }

        @Test
        public void testOrElseGetIfNull() {
            assertEquals("test", Nullable.of("test").orElseGetIfNull(() -> "other"));
            assertEquals("other", Nullable.of((String) null).orElseGetIfNull(() -> "other"));
            assertEquals("other", Nullable.empty().orElseGetIfNull(() -> "other"));
        }

        @Test
        public void testOrElseThrow() {
            assertEquals("test", Nullable.of("test").orElseThrow());
            assertNull(Nullable.of((String) null).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow());

            assertEquals("test", Nullable.of("test").orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals("test", Nullable.of("test").orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> Nullable.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testOrElseThrowIfNull() {
            assertEquals("test", Nullable.of("test").orElseThrowIfNull());

            assertThrows(NoSuchElementException.class, () -> Nullable.of((String) null).orElseThrowIfNull());

            assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrowIfNull());

            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> Nullable.of((String) null).orElseThrowIfNull("Value was null"));
            assertEquals("Value was null", ex.getMessage());
        }

        @Test
        public void testStream() {
            List<String> list = Nullable.of("test").stream().toList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            List<String> nullList = Nullable.of((String) null).stream().toList();
            assertEquals(1, nullList.size());
            assertNull(nullList.get(0));

            List<String> emptyList = Nullable.<String> empty().stream().toList();
            assertEquals(0, emptyList.size());
        }

        @Test
        public void testStreamIfNotNull() {
            List<String> list = Nullable.of("test").streamIfNotNull().toList();
            assertEquals(1, list.size());
            assertEquals("test", list.get(0));

            List<String> nullList = Nullable.of((String) null).streamIfNotNull().toList();
            assertEquals(0, nullList.size());

            List<String> emptyList = Nullable.<String> empty().streamIfNotNull().toList();
            assertEquals(0, emptyList.size());
        }

        @Test
        public void testToList() {
            List<String> presentList = Nullable.of("test").toList();
            assertEquals(1, presentList.size());
            assertEquals("test", presentList.get(0));

            List<String> nullList = Nullable.of((String) null).toList();
            assertEquals(1, nullList.size());
            assertNull(nullList.get(0));

            List<String> emptyList = Nullable.<String> empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToListIfNotNull() {
            List<String> presentList = Nullable.of("test").toListIfNotNull();
            assertEquals(1, presentList.size());
            assertEquals("test", presentList.get(0));

            List<String> nullList = Nullable.of((String) null).toListIfNotNull();
            assertTrue(nullList.isEmpty());

            List<String> emptyList = Nullable.<String> empty().toListIfNotNull();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<String> presentSet = Nullable.of("test").toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains("test"));

            Set<String> nullSet = Nullable.of((String) null).toSet();
            assertEquals(1, nullSet.size());
            assertTrue(nullSet.contains(null));

            Set<String> emptySet = Nullable.<String> empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToSetIfNotNull() {
            Set<String> presentSet = Nullable.of("test").toSetIfNotNull();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains("test"));

            Set<String> nullSet = Nullable.of((String) null).toSetIfNotNull();
            assertTrue(nullSet.isEmpty());

            Set<String> emptySet = Nullable.<String> empty().toSetIfNotNull();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToOptional() {
            Optional<String> opt = Nullable.of("test").toOptional();
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            opt = Nullable.of((String) null).toOptional();
            assertFalse(opt.isPresent());

            opt = Nullable.<String> empty().toOptional();
            assertFalse(opt.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            java.util.Optional<String> opt = Nullable.of("test").toJdkOptional();
            assertTrue(opt.isPresent());
            assertEquals("test", opt.get());

            opt = Nullable.of((String) null).toJdkOptional();
            assertFalse(opt.isPresent());

            opt = Nullable.<String> empty().toJdkOptional();
            assertFalse(opt.isPresent());
        }

        @Test
        public void testEquals() {
            Nullable<String> nonNull1 = Nullable.of("test");
            Nullable<String> nonNull2 = Nullable.of("test");
            Nullable<String> nonNull3 = Nullable.of("other");
            Nullable<String> null1 = Nullable.of((String) null);
            Nullable<String> null2 = Nullable.of((String) null);
            Nullable<String> empty1 = Nullable.empty();
            Nullable<String> empty2 = Nullable.empty();

            assertEquals(nonNull1, nonNull1);
            assertEquals(null1, null1);
            assertEquals(empty1, empty1);

            assertEquals(nonNull1, nonNull2);
            assertEquals(nonNull2, nonNull1);
            assertEquals(null1, null2);
            assertEquals(empty1, empty2);

            assertNotEquals(nonNull1, nonNull3);
            assertNotEquals(nonNull1, null1);
            assertNotEquals(nonNull1, empty1);
            assertNotEquals(null1, empty1);

            assertNotEquals(nonNull1, null);
            assertNotEquals(nonNull1, "test");
        }

        @Test
        public void testHashCode() {
            Nullable<String> nonNull1 = Nullable.of("test");
            Nullable<String> nonNull2 = Nullable.of("test");
            Nullable<String> null1 = Nullable.of((String) null);
            Nullable<String> null2 = Nullable.of((String) null);
            Nullable<String> empty1 = Nullable.empty();
            Nullable<String> empty2 = Nullable.empty();

            assertEquals(nonNull1.hashCode(), nonNull2.hashCode());
            assertEquals(null1.hashCode(), null2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());

            assertNotEquals(nonNull1.hashCode(), null1.hashCode());
            assertNotEquals(nonNull1.hashCode(), empty1.hashCode());
            assertNotEquals(null1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("Nullable[test]", Nullable.of("test").toString());
            assertEquals("Nullable[null]", Nullable.of((String) null).toString());
            assertEquals("Nullable.empty", Nullable.empty().toString());
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
    }

    @Nested
    public class uOptionalLongTest {

        @Test
        public void testEmpty() {
            OptionalLong empty = OptionalLong.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
            assertThrows(NoSuchElementException.class, () -> empty.getAsLong());
        }

        @Test
        public void testOf() {
            OptionalLong cached1 = OptionalLong.of(0L);
            OptionalLong cached2 = OptionalLong.of(0L);
            assertSame(cached1, cached2);

            OptionalLong cached3 = OptionalLong.of(-256L);
            OptionalLong cached4 = OptionalLong.of(-256L);
            assertSame(cached3, cached4);

            OptionalLong cached5 = OptionalLong.of(1024L);
            OptionalLong cached6 = OptionalLong.of(1024L);
            assertSame(cached5, cached6);

            OptionalLong nonCached1 = OptionalLong.of(2000L);
            OptionalLong nonCached2 = OptionalLong.of(2000L);
            assertNotSame(nonCached1, nonCached2);
            assertEquals(nonCached1, nonCached2);

            OptionalLong opt = OptionalLong.of(42L);
            assertTrue(opt.isPresent());
            assertEquals(42L, opt.get());
        }

        @Test
        public void testOfNullable() {
            OptionalLong nullOpt = OptionalLong.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalLong opt = OptionalLong.ofNullable(42L);
            assertTrue(opt.isPresent());
            assertEquals(42L, opt.get());
        }

        @Test
        public void testFromJavaOptional() {
            OptionalLong fromPresent = OptionalLong.from(java.util.OptionalLong.of(42L));
            assertTrue(fromPresent.isPresent());
            assertEquals(42L, fromPresent.get());

            OptionalLong fromEmpty = OptionalLong.from(java.util.OptionalLong.empty());
            assertFalse(fromEmpty.isPresent());
        }

        @Test
        public void testGet() {
            assertEquals(42L, OptionalLong.of(42L).get());
            assertEquals(42L, OptionalLong.of(42L).getAsLong());
            assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().get());
            assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().getAsLong());
        }

        @Test
        public void testIsPresent() {
            assertTrue(OptionalLong.of(42L).isPresent());
            assertFalse(OptionalLong.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(OptionalLong.of(42L).isEmpty());
            assertTrue(OptionalLong.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicLong value = new AtomicLong(0);

            OptionalLong.of(42L).ifPresent(v -> value.set(v));
            assertEquals(42L, value.get());

            value.set(0);
            OptionalLong.empty().ifPresent(v -> value.set(v));
            assertEquals(0L, value.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicLong value = new AtomicLong(0);
            AtomicBoolean emptyCalled = new AtomicBoolean(false);

            OptionalLong.of(42L).ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(42L, value.get());
            assertFalse(emptyCalled.get());

            value.set(0);
            OptionalLong.empty().ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(0L, value.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            OptionalLong opt = OptionalLong.of(42L);

            OptionalLong filtered = opt.filter(v -> v > 40);
            assertTrue(filtered.isPresent());
            assertEquals(42L, filtered.get());

            filtered = opt.filter(v -> v > 50);
            assertFalse(filtered.isPresent());

            assertFalse(OptionalLong.empty().filter(v -> true).isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalLong mapped = OptionalLong.of(42L).map(v -> v * 2);
            assertTrue(mapped.isPresent());
            assertEquals(84L, mapped.get());

            assertFalse(OptionalLong.empty().map(v -> v * 2).isPresent());
        }

        @Test
        public void testMapToInt() throws Exception {
            OptionalInt intOpt = OptionalLong.of(42L).mapToInt(v -> v.intValue());
            assertTrue(intOpt.isPresent());
            assertEquals(42, intOpt.get());

            intOpt = OptionalLong.of(Long.MAX_VALUE).mapToInt(v -> v.intValue());
            assertTrue(intOpt.isPresent());
            assertEquals(-1, intOpt.get());

            assertFalse(OptionalLong.empty().mapToInt(v -> 0).isPresent());
        }

        @Test
        public void testMapToDouble() throws Exception {
            OptionalDouble doubleOpt = OptionalLong.of(42L).mapToDouble(v -> v / 2.0);
            assertTrue(doubleOpt.isPresent());
            assertEquals(21.0, doubleOpt.get());

            assertFalse(OptionalLong.empty().mapToDouble(v -> 0.0).isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalLong.of(42L).mapToObj(v -> "Number: " + v);
            assertTrue(strOpt.isPresent());
            assertEquals("Number: 42", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalLong.of(42L).mapToObj(v -> null));

            assertFalse(OptionalLong.empty().mapToObj(v -> "test").isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalLong result = OptionalLong.of(42L).flatMap(v -> OptionalLong.of(v * 2));
            assertTrue(result.isPresent());
            assertEquals(84L, result.get());

            result = OptionalLong.of(42L).flatMap(v -> OptionalLong.empty());
            assertFalse(result.isPresent());

            result = OptionalLong.empty().flatMap(v -> OptionalLong.of(100L));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalLong.of(42L).flatMap(v -> null));
        }

        @Test
        public void testOr() {
            OptionalLong present = OptionalLong.of(42L);
            OptionalLong result = present.or(() -> OptionalLong.of(100L));
            assertEquals(42L, result.get());

            OptionalLong empty = OptionalLong.empty();
            result = empty.or(() -> OptionalLong.of(100L));
            assertEquals(100L, result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElseZero() {
            assertEquals(42L, OptionalLong.of(42L).orElseZero());
            assertEquals(0L, OptionalLong.empty().orElseZero());
        }

        @Test
        public void testOrElse() {
            assertEquals(42L, OptionalLong.of(42L).orElse(100L));
            assertEquals(100L, OptionalLong.empty().orElse(100L));
            assertEquals(-1L, OptionalLong.empty().orElse(-1L));
        }

        @Test
        public void testOrElseGet() {
            assertEquals(42L, OptionalLong.of(42L).orElseGet(() -> 100L));
            assertEquals(100L, OptionalLong.empty().orElseGet(() -> 100L));

            AtomicLong counter = new AtomicLong(0);
            OptionalLong.of(42L).orElseGet(() -> counter.incrementAndGet());
            assertEquals(0L, counter.get());

            OptionalLong.empty().orElseGet(() -> counter.incrementAndGet());
            assertEquals(1L, counter.get());
        }

        @Test
        public void testOrElseThrow() {
            assertEquals(42L, OptionalLong.of(42L).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow());

            assertEquals(42L, OptionalLong.of(42L).orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals(42L, OptionalLong.of(42L).orElseThrow("Value: %d", 999L));
            ex = assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Value: %d", 999L));
            assertTrue(ex.getMessage().contains("999"));

            assertEquals(42L, OptionalLong.of(42L).orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalLong.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            long[] presentArray = OptionalLong.of(42L).stream().toArray();
            assertEquals(1, presentArray.length);
            assertEquals(42L, presentArray[0]);

            long[] emptyArray = OptionalLong.empty().stream().toArray();
            assertEquals(0, emptyArray.length);
        }

        @Test
        public void testToList() {
            List<Long> presentList = OptionalLong.of(42L).toList();
            assertEquals(1, presentList.size());
            assertEquals(42L, presentList.get(0));

            List<Long> emptyList = OptionalLong.empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<Long> presentSet = OptionalLong.of(42L).toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42L));

            Set<Long> emptySet = OptionalLong.empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<Long> presentList = OptionalLong.of(42L).toImmutableList();
            assertEquals(1, presentList.size());
            assertEquals(42L, presentList.get(0));

            ImmutableList<Long> emptyList = OptionalLong.empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<Long> presentSet = OptionalLong.of(42L).toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42L));

            ImmutableSet<Long> emptySet = OptionalLong.empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testBoxed() {
            Optional<Long> boxed = OptionalLong.of(42L).boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42L, boxed.get());

            Optional<Long> boxedEmpty = OptionalLong.empty().boxed();
            assertFalse(boxedEmpty.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            java.util.OptionalLong jdkOpt = OptionalLong.of(42L).toJdkOptional();
            assertTrue(jdkOpt.isPresent());
            assertEquals(42L, jdkOpt.getAsLong());

            java.util.OptionalLong jdkEmpty = OptionalLong.empty().toJdkOptional();
            assertFalse(jdkEmpty.isPresent());

            java.util.OptionalLong jdkOptDeprecated = OptionalLong.of(42L).__();
            assertTrue(jdkOptDeprecated.isPresent());
            assertEquals(42L, jdkOptDeprecated.getAsLong());
        }

        @Test
        public void testCompareTo() {
            OptionalLong opt42 = OptionalLong.of(42L);
            OptionalLong opt100 = OptionalLong.of(100L);
            OptionalLong optNeg = OptionalLong.of(-10L);
            OptionalLong empty = OptionalLong.empty();

            assertEquals(0, opt42.compareTo(OptionalLong.of(42L)));
            assertEquals(0, empty.compareTo(OptionalLong.empty()));

            assertTrue(opt42.compareTo(opt100) < 0);
            assertTrue(opt100.compareTo(opt42) > 0);
            assertTrue(optNeg.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(empty) > 0);
            assertTrue(empty.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(null) > 0);
            assertEquals(0, empty.compareTo(null));
        }

        @Test
        public void testEquals() {
            OptionalLong opt1 = OptionalLong.of(42L);
            OptionalLong opt2 = OptionalLong.of(42L);
            OptionalLong opt3 = OptionalLong.of(100L);
            OptionalLong empty1 = OptionalLong.empty();
            OptionalLong empty2 = OptionalLong.empty();

            assertEquals(opt1, opt1);

            assertEquals(opt1, opt2);
            assertEquals(opt2, opt1);

            assertEquals(empty1, empty2);

            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);

            assertNotEquals(opt1, null);
            assertNotEquals(opt1, 42L);
        }

        @Test
        public void testHashCode() {
            OptionalLong opt1 = OptionalLong.of(42L);
            OptionalLong opt2 = OptionalLong.of(42L);
            OptionalLong opt3 = OptionalLong.of(100L);
            OptionalLong empty1 = OptionalLong.empty();
            OptionalLong empty2 = OptionalLong.empty();

            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(opt1.hashCode(), opt3.hashCode());
            assertNotEquals(opt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("OptionalLong[42]", OptionalLong.of(42L).toString());
            assertEquals("OptionalLong[-100]", OptionalLong.of(-100L).toString());
            assertEquals("OptionalLong.empty", OptionalLong.empty().toString());
        }

        @Test
        public void testEdgeCases() {
            OptionalLong min = OptionalLong.of(Long.MIN_VALUE);
            assertTrue(min.isPresent());
            assertEquals(Long.MIN_VALUE, min.get());

            OptionalLong max = OptionalLong.of(Long.MAX_VALUE);
            assertTrue(max.isPresent());
            assertEquals(Long.MAX_VALUE, max.get());

            OptionalLong overflow = OptionalLong.of(Long.MAX_VALUE).map(v -> v + 1);
            assertTrue(overflow.isPresent());
            assertEquals(Long.MIN_VALUE, overflow.get());
        }
    }

    @Nested
    public class uOptionalDoubleTest {

        @Test
        public void testEmpty() {
            OptionalDouble empty = OptionalDouble.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
            assertThrows(NoSuchElementException.class, () -> empty.getAsDouble());
        }

        @Test
        public void testOf() {
            OptionalDouble zero1 = OptionalDouble.of(0.0);
            OptionalDouble zero2 = OptionalDouble.of(0.0);
            assertSame(zero1, zero2);

            OptionalDouble opt = OptionalDouble.of(3.14);
            assertTrue(opt.isPresent());
            assertEquals(3.14, opt.get());

            OptionalDouble nan = OptionalDouble.of(Double.NaN);
            assertTrue(nan.isPresent());
            assertTrue(Double.isNaN(nan.get()));

            OptionalDouble posInf = OptionalDouble.of(Double.POSITIVE_INFINITY);
            assertTrue(posInf.isPresent());
            assertEquals(Double.POSITIVE_INFINITY, posInf.get());

            OptionalDouble negInf = OptionalDouble.of(Double.NEGATIVE_INFINITY);
            assertTrue(negInf.isPresent());
            assertEquals(Double.NEGATIVE_INFINITY, negInf.get());
        }

        @Test
        public void testOfNullable() {
            OptionalDouble nullOpt = OptionalDouble.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalDouble opt = OptionalDouble.ofNullable(42.5);
            assertTrue(opt.isPresent());
            assertEquals(42.5, opt.get());
        }

        @Test
        public void testFromJavaOptional() {
            OptionalDouble fromPresent = OptionalDouble.from(java.util.OptionalDouble.of(42.5));
            assertTrue(fromPresent.isPresent());
            assertEquals(42.5, fromPresent.get());

            OptionalDouble fromEmpty = OptionalDouble.from(java.util.OptionalDouble.empty());
            assertFalse(fromEmpty.isPresent());
        }

        @Test
        public void testGet() {
            assertEquals(42.5, OptionalDouble.of(42.5).get());
            assertEquals(42.5, OptionalDouble.of(42.5).getAsDouble());
            assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().get());
            assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().getAsDouble());
        }

        @Test
        public void testIsPresent() {
            assertTrue(OptionalDouble.of(42.5).isPresent());
            assertFalse(OptionalDouble.empty().isPresent());
        }

        @Test
        public void testIsEmpty() {
            assertFalse(OptionalDouble.of(42.5).isEmpty());
            assertTrue(OptionalDouble.empty().isEmpty());
        }

        @Test
        public void testIfPresent() throws Exception {
            AtomicReference<Double> value = new AtomicReference<>(0.0);

            OptionalDouble.of(42.5).ifPresent(v -> value.set(v));
            assertEquals(42.5, value.get());

            value.set(0.0);
            OptionalDouble.empty().ifPresent(v -> value.set(v));
            assertEquals(0.0, value.get());
        }

        @Test
        public void testIfPresentOrElse() throws Exception {
            AtomicReference<Double> value = new AtomicReference<>(0.0);
            AtomicBoolean emptyCalled = new AtomicBoolean(false);

            OptionalDouble.of(42.5).ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(42.5, value.get());
            assertFalse(emptyCalled.get());

            value.set(0.0);
            OptionalDouble.empty().ifPresentOrElse(v -> value.set(v), () -> emptyCalled.set(true));
            assertEquals(0.0, value.get());
            assertTrue(emptyCalled.get());
        }

        @Test
        public void testFilter() throws Exception {
            OptionalDouble opt = OptionalDouble.of(42.5);

            OptionalDouble filtered = opt.filter(v -> v > 40.0);
            assertTrue(filtered.isPresent());
            assertEquals(42.5, filtered.get());

            filtered = opt.filter(v -> v > 50.0);
            assertFalse(filtered.isPresent());

            assertFalse(OptionalDouble.empty().filter(v -> true).isPresent());

            OptionalDouble nanOpt = OptionalDouble.of(Double.NaN);
            filtered = nanOpt.filter(Double::isNaN);
            assertTrue(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalDouble mapped = OptionalDouble.of(42.5).map(v -> v * 2);
            assertTrue(mapped.isPresent());
            assertEquals(85.0, mapped.get());

            assertFalse(OptionalDouble.empty().map(v -> v * 2).isPresent());

            mapped = OptionalDouble.of(-16.0).map(Math::sqrt);
            assertTrue(mapped.isPresent());
            assertTrue(Double.isNaN(mapped.get()));
        }

        @Test
        public void testMapToInt() throws Exception {
            OptionalInt intOpt = OptionalDouble.of(42.7).mapToInt(v -> v.intValue());
            assertTrue(intOpt.isPresent());
            assertEquals(42, intOpt.get());

            intOpt = OptionalDouble.of(42.5).mapToInt(v -> (int) Math.round(v));
            assertTrue(intOpt.isPresent());
            assertEquals(43, intOpt.get());

            assertFalse(OptionalDouble.empty().mapToInt(v -> 0).isPresent());
        }

        @Test
        public void testMapToLong() throws Exception {
            OptionalLong longOpt = OptionalDouble.of(42.7).mapToLong(v -> v.longValue());
            assertTrue(longOpt.isPresent());
            assertEquals(42L, longOpt.get());

            longOpt = OptionalDouble.of(1e15).mapToLong(v -> v.longValue());
            assertTrue(longOpt.isPresent());
            assertEquals(1000000000000000L, longOpt.get());

            assertFalse(OptionalDouble.empty().mapToLong(v -> 0L).isPresent());
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalDouble.of(42.5).mapToObj(v -> String.format("%.2f", v));
            assertTrue(strOpt.isPresent());
            assertEquals("42.50", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalDouble.of(42.5).mapToObj(v -> null));

            assertFalse(OptionalDouble.empty().mapToObj(v -> "test").isPresent());
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalDouble result = OptionalDouble.of(42.5).flatMap(v -> OptionalDouble.of(v * 2));
            assertTrue(result.isPresent());
            assertEquals(85.0, result.get());

            result = OptionalDouble.of(42.5).flatMap(v -> OptionalDouble.empty());
            assertFalse(result.isPresent());

            result = OptionalDouble.empty().flatMap(v -> OptionalDouble.of(100.0));
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalDouble.of(42.5).flatMap(v -> null));
        }

        @Test
        public void testOr() {
            OptionalDouble present = OptionalDouble.of(42.5);
            OptionalDouble result = present.or(() -> OptionalDouble.of(100.0));
            assertEquals(42.5, result.get());

            OptionalDouble empty = OptionalDouble.empty();
            result = empty.or(() -> OptionalDouble.of(100.0));
            assertEquals(100.0, result.get());

            assertThrows(NullPointerException.class, () -> empty.or(() -> null));
        }

        @Test
        public void testOrElseZero() {
            assertEquals(42.5, OptionalDouble.of(42.5).orElseZero());
            assertEquals(0.0, OptionalDouble.empty().orElseZero());
        }

        @Test
        public void testOrElse() {
            assertEquals(42.5, OptionalDouble.of(42.5).orElse(100.0));
            assertEquals(100.0, OptionalDouble.empty().orElse(100.0));
            assertEquals(Double.NaN, OptionalDouble.empty().orElse(Double.NaN));
        }

        @Test
        public void testOrElseGet() {
            assertEquals(42.5, OptionalDouble.of(42.5).orElseGet(() -> 100.0));
            assertEquals(100.0, OptionalDouble.empty().orElseGet(() -> 100.0));

            AtomicBoolean supplierCalled = new AtomicBoolean(false);
            OptionalDouble.of(42.5).orElseGet(() -> {
                supplierCalled.set(true);
                return 100.0;
            });
            assertFalse(supplierCalled.get());

            OptionalDouble.empty().orElseGet(() -> {
                supplierCalled.set(true);
                return 100.0;
            });
            assertTrue(supplierCalled.get());
        }

        @Test
        public void testOrElseThrow() {
            assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow());

            assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow("Custom message"));
            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow("Value: %.2f", 99.9));
            ex = assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Value: %.2f", 99.9));
            assertTrue(ex.getMessage().contains("99.9"));

            assertEquals(42.5, OptionalDouble.of(42.5).orElseThrow(() -> new RuntimeException("Custom")));
            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalDouble.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
        }

        @Test
        public void testStream() {
            double[] presentArray = OptionalDouble.of(42.5).stream().toArray();
            assertEquals(1, presentArray.length);
            assertEquals(42.5, presentArray[0]);

            double[] emptyArray = OptionalDouble.empty().stream().toArray();
            assertEquals(0, emptyArray.length);
        }

        @Test
        public void testToList() {
            List<Double> presentList = OptionalDouble.of(42.5).toList();
            assertEquals(1, presentList.size());
            assertEquals(42.5, presentList.get(0));

            List<Double> emptyList = OptionalDouble.empty().toList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToSet() {
            Set<Double> presentSet = OptionalDouble.of(42.5).toSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42.5));

            Set<Double> emptySet = OptionalDouble.empty().toSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testToImmutableList() {
            ImmutableList<Double> presentList = OptionalDouble.of(42.5).toImmutableList();
            assertEquals(1, presentList.size());
            assertEquals(42.5, presentList.get(0));

            ImmutableList<Double> emptyList = OptionalDouble.empty().toImmutableList();
            assertTrue(emptyList.isEmpty());
        }

        @Test
        public void testToImmutableSet() {
            ImmutableSet<Double> presentSet = OptionalDouble.of(42.5).toImmutableSet();
            assertEquals(1, presentSet.size());
            assertTrue(presentSet.contains(42.5));

            ImmutableSet<Double> emptySet = OptionalDouble.empty().toImmutableSet();
            assertTrue(emptySet.isEmpty());
        }

        @Test
        public void testBoxed() {
            Optional<Double> boxed = OptionalDouble.of(42.5).boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42.5, boxed.get());

            Optional<Double> boxedEmpty = OptionalDouble.empty().boxed();
            assertFalse(boxedEmpty.isPresent());
        }

        @Test
        public void testToJdkOptional() {
            java.util.OptionalDouble jdkOpt = OptionalDouble.of(42.5).toJdkOptional();
            assertTrue(jdkOpt.isPresent());
            assertEquals(42.5, jdkOpt.getAsDouble());

            java.util.OptionalDouble jdkEmpty = OptionalDouble.empty().toJdkOptional();
            assertFalse(jdkEmpty.isPresent());

            java.util.OptionalDouble jdkOptDeprecated = OptionalDouble.of(42.5).__();
            assertTrue(jdkOptDeprecated.isPresent());
            assertEquals(42.5, jdkOptDeprecated.getAsDouble());
        }

        @Test
        public void testCompareTo() {
            OptionalDouble opt42 = OptionalDouble.of(42.5);
            OptionalDouble opt100 = OptionalDouble.of(100.0);
            OptionalDouble optNeg = OptionalDouble.of(-10.5);
            OptionalDouble empty = OptionalDouble.empty();

            assertEquals(0, opt42.compareTo(OptionalDouble.of(42.5)));
            assertEquals(0, empty.compareTo(OptionalDouble.empty()));

            assertTrue(opt42.compareTo(opt100) < 0);
            assertTrue(opt100.compareTo(opt42) > 0);
            assertTrue(optNeg.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(empty) > 0);
            assertTrue(empty.compareTo(opt42) < 0);

            assertTrue(opt42.compareTo(null) > 0);
            assertEquals(0, empty.compareTo(null));

            OptionalDouble nan1 = OptionalDouble.of(Double.NaN);
            OptionalDouble nan2 = OptionalDouble.of(Double.NaN);
            assertEquals(0, nan1.compareTo(nan2));
        }

        @Test
        public void testEquals() {
            OptionalDouble opt1 = OptionalDouble.of(42.5);
            OptionalDouble opt2 = OptionalDouble.of(42.5);
            OptionalDouble opt3 = OptionalDouble.of(100.0);
            OptionalDouble empty1 = OptionalDouble.empty();
            OptionalDouble empty2 = OptionalDouble.empty();

            assertEquals(opt1, opt1);

            assertEquals(opt1, opt2);
            assertEquals(opt2, opt1);

            assertEquals(empty1, empty2);

            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);

            assertNotEquals(opt1, null);
            assertNotEquals(opt1, 42.5);

            OptionalDouble nan1 = OptionalDouble.of(Double.NaN);
            OptionalDouble nan2 = OptionalDouble.of(Double.NaN);
            assertEquals(nan1, nan2);
        }

        @Test
        public void testHashCode() {
            OptionalDouble opt1 = OptionalDouble.of(42.5);
            OptionalDouble opt2 = OptionalDouble.of(42.5);
            OptionalDouble opt3 = OptionalDouble.of(100.0);
            OptionalDouble empty1 = OptionalDouble.empty();
            OptionalDouble empty2 = OptionalDouble.empty();

            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertEquals(empty1.hashCode(), empty2.hashCode());
            assertNotEquals(opt1.hashCode(), opt3.hashCode());
            assertNotEquals(opt1.hashCode(), empty1.hashCode());
        }

        @Test
        public void testToString() {
            assertEquals("OptionalDouble[42.5]", OptionalDouble.of(42.5).toString());
            assertEquals("OptionalDouble[-100.0]", OptionalDouble.of(-100.0).toString());
            assertEquals("OptionalDouble[NaN]", OptionalDouble.of(Double.NaN).toString());
            assertEquals("OptionalDouble[Infinity]", OptionalDouble.of(Double.POSITIVE_INFINITY).toString());
            assertEquals("OptionalDouble[-Infinity]", OptionalDouble.of(Double.NEGATIVE_INFINITY).toString());
            assertEquals("OptionalDouble.empty", OptionalDouble.empty().toString());
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
    }

    @Nested
    public class OptionalFloatTest {

        @Test
        public void testEmpty() {
            OptionalFloat empty = OptionalFloat.empty();
            assertFalse(empty.isPresent());
            assertTrue(empty.isEmpty());
            assertThrows(NoSuchElementException.class, () -> empty.get());
        }

        @Test
        public void testOf() {
            OptionalFloat zero1 = OptionalFloat.of(0.0f);
            OptionalFloat zero2 = OptionalFloat.of(0.0f);
            assertSame(zero1, zero2);

            OptionalFloat opt = OptionalFloat.of(3.14f);
            assertTrue(opt.isPresent());
            assertEquals(3.14f, opt.get());

            OptionalFloat nan = OptionalFloat.of(Float.NaN);
            assertTrue(nan.isPresent());
            assertTrue(Float.isNaN(nan.get()));

            OptionalFloat posInf = OptionalFloat.of(Float.POSITIVE_INFINITY);
            assertTrue(posInf.isPresent());
            assertEquals(Float.POSITIVE_INFINITY, posInf.get());

            OptionalFloat negInf = OptionalFloat.of(Float.NEGATIVE_INFINITY);
            assertTrue(negInf.isPresent());
            assertEquals(Float.NEGATIVE_INFINITY, negInf.get());
        }

        @Test
        public void testOfNullable() {
            OptionalFloat nullOpt = OptionalFloat.ofNullable(null);
            assertFalse(nullOpt.isPresent());

            OptionalFloat opt = OptionalFloat.ofNullable(42.5f);
            assertTrue(opt.isPresent());
            assertEquals(42.5f, opt.get());
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
        public void testFilter() throws Exception {
            OptionalFloat opt = OptionalFloat.of(42.5f);

            OptionalFloat filtered = opt.filter(v -> v > 40.0f);
            assertTrue(filtered.isPresent());
            assertEquals(42.5f, filtered.get());

            filtered = opt.filter(v -> v > 50.0f);
            assertFalse(filtered.isPresent());

            OptionalFloat nanOpt = OptionalFloat.of(Float.NaN);
            filtered = nanOpt.filter(Float::isNaN);
            assertTrue(filtered.isPresent());
        }

        @Test
        public void testMap() throws Exception {
            OptionalFloat mapped = OptionalFloat.of(21.25f).map(v -> v * 2);
            assertTrue(mapped.isPresent());
            assertEquals(42.5f, mapped.get());

            assertFalse(OptionalFloat.empty().map(v -> v * 2).isPresent());

            mapped = OptionalFloat.of(-16.0f).map(v -> (float) Math.sqrt(v));
            assertTrue(mapped.isPresent());
            assertTrue(Float.isNaN(mapped.get()));
        }

        @Test
        public void testMapToInt() throws Exception {
            OptionalInt intOpt = OptionalFloat.of(42.7f).mapToInt(v -> v.intValue());
            assertTrue(intOpt.isPresent());
            assertEquals(42, intOpt.get());

            intOpt = OptionalFloat.of(42.5f).mapToInt(v -> Math.round(v));
            assertTrue(intOpt.isPresent());
            assertEquals(43, intOpt.get());
        }

        @Test
        public void testMapToDouble() throws Exception {
            OptionalDouble doubleOpt = OptionalFloat.of(42.5f).mapToDouble(v -> v * 2.0);
            assertTrue(doubleOpt.isPresent());
            assertEquals(85.0, doubleOpt.get(), 0.001);
        }

        @Test
        public void testMapToObj() throws Exception {
            Optional<String> strOpt = OptionalFloat.of(42.5f).mapToObj(v -> String.format("%.2f", v));
            assertTrue(strOpt.isPresent());
            assertEquals("42.50", strOpt.get());

            assertThrows(NullPointerException.class, () -> OptionalFloat.of(42.5f).mapToObj(v -> null));
        }

        @Test
        public void testFlatMap() throws Exception {
            OptionalFloat result = OptionalFloat.of(21.25f).flatMap(v -> OptionalFloat.of(v * 2));
            assertTrue(result.isPresent());
            assertEquals(42.5f, result.get());

            result = OptionalFloat.of(42.5f).flatMap(v -> OptionalFloat.empty());
            assertFalse(result.isPresent());

            assertThrows(NullPointerException.class, () -> OptionalFloat.of(42.5f).flatMap(v -> null));
        }

        @Test
        public void testOr() {
            OptionalFloat present = OptionalFloat.of(42.5f);
            OptionalFloat result = present.or(() -> OptionalFloat.of(100.0f));
            assertEquals(42.5f, result.get());

            OptionalFloat empty = OptionalFloat.empty();
            result = empty.or(() -> OptionalFloat.of(100.0f));
            assertEquals(100.0f, result.get());
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
        public void testOrElseThrow() {
            assertEquals(42.5f, OptionalFloat.of(42.5f).orElseThrow());
            assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow());

            NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Custom message"));
            assertEquals("Custom message", ex.getMessage());

            RuntimeException customEx = assertThrows(RuntimeException.class, () -> OptionalFloat.empty().orElseThrow(() -> new RuntimeException("Custom")));
            assertEquals("Custom", customEx.getMessage());
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

        @Test
        public void testBoxed() {
            Optional<Float> boxed = OptionalFloat.of(42.5f).boxed();
            assertTrue(boxed.isPresent());
            assertEquals(42.5f, boxed.get());

            assertFalse(OptionalFloat.empty().boxed().isPresent());
        }

        @Test
        public void testCompareTo() {
            OptionalFloat opt42 = OptionalFloat.of(42.5f);
            OptionalFloat opt100 = OptionalFloat.of(100.0f);
            OptionalFloat optNeg = OptionalFloat.of(-10.5f);
            OptionalFloat empty = OptionalFloat.empty();

            assertEquals(0, opt42.compareTo(OptionalFloat.of(42.5f)));
            assertTrue(opt42.compareTo(opt100) < 0);
            assertTrue(opt100.compareTo(opt42) > 0);
            assertTrue(optNeg.compareTo(opt42) < 0);
            assertTrue(opt42.compareTo(empty) > 0);
            assertTrue(empty.compareTo(opt42) < 0);
            assertTrue(opt42.compareTo(null) > 0);

            OptionalFloat nan1 = OptionalFloat.of(Float.NaN);
            OptionalFloat nan2 = OptionalFloat.of(Float.NaN);
            assertEquals(0, nan1.compareTo(nan2));
        }

        @Test
        public void testEqualsAndHashCode() {
            OptionalFloat opt1 = OptionalFloat.of(42.5f);
            OptionalFloat opt2 = OptionalFloat.of(42.5f);
            OptionalFloat opt3 = OptionalFloat.of(100.0f);
            OptionalFloat empty1 = OptionalFloat.empty();
            OptionalFloat empty2 = OptionalFloat.empty();

            assertEquals(opt1, opt2);
            assertEquals(opt1.hashCode(), opt2.hashCode());
            assertNotEquals(opt1, opt3);
            assertNotEquals(opt1, empty1);
            assertEquals(empty1, empty2);
            assertNotEquals(opt1, null);
            assertNotEquals(opt1, 42.5f);

            OptionalFloat nan1 = OptionalFloat.of(Float.NaN);
            OptionalFloat nan2 = OptionalFloat.of(Float.NaN);
            assertEquals(nan1, nan2);
        }

        @Test
        public void testToString() {
            assertEquals("OptionalFloat[42.5]", OptionalFloat.of(42.5f).toString());
            assertEquals("OptionalFloat[-100.0]", OptionalFloat.of(-100.0f).toString());
            assertEquals("OptionalFloat[NaN]", OptionalFloat.of(Float.NaN).toString());
            assertEquals("OptionalFloat[Infinity]", OptionalFloat.of(Float.POSITIVE_INFINITY).toString());
            assertEquals("OptionalFloat[-Infinity]", OptionalFloat.of(Float.NEGATIVE_INFINITY).toString());
            assertEquals("OptionalFloat.empty", OptionalFloat.empty().toString());
        }

        @Test
        public void testSpecialValues() {
            OptionalFloat min = OptionalFloat.of(Float.MIN_VALUE);
            assertTrue(min.isPresent());
            assertEquals(Float.MIN_VALUE, min.get());

            OptionalFloat max = OptionalFloat.of(Float.MAX_VALUE);
            assertTrue(max.isPresent());
            assertEquals(Float.MAX_VALUE, max.get());

            OptionalFloat posInf = OptionalFloat.of(Float.POSITIVE_INFINITY);
            assertTrue(posInf.isPresent());
            assertTrue(Float.isInfinite(posInf.get()));
            assertTrue(posInf.get() > 0);

            OptionalFloat negInf = OptionalFloat.of(Float.NEGATIVE_INFINITY);
            assertTrue(negInf.isPresent());
            assertTrue(Float.isInfinite(negInf.get()));
            assertTrue(negInf.get() < 0);

            OptionalFloat result = posInf.map(v -> v + 1);
            assertTrue(Float.isInfinite(result.get()));

            result = OptionalFloat.of(1.0f).map(v -> v / 0.0f);
            assertTrue(Float.isInfinite(result.get()));

            result = OptionalFloat.of(0.0f).map(v -> v / 0.0f);
            assertTrue(Float.isNaN(result.get()));
        }
    }
}
