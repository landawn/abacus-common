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

import org.junit.jupiter.api.Test;

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

public class uTest extends uTestSupport {

    @Test
    public void testOptionalOfAndGet() {
        Optional<String> empty = Optional.empty();
        Optional<String> present = Optional.of("test");
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertTrue(present.isPresent());
        assertEquals("test", present.get());
        assertThrows(NullPointerException.class, () -> Optional.of(null));
        assertTrue(Optional.ofNullable("test").isPresent());
        assertFalse(Optional.ofNullable(null).isPresent());
        assertEquals("jdk", Optional.from(java.util.Optional.of("jdk")).get());
        assertFalse(Optional.from(java.util.Optional.empty()).isPresent());
        assertSame(Optional.empty(), Optional.empty());
        assertSame(Optional.TRUE, Optional.of(Boolean.TRUE));
        assertSame(Optional.FALSE, Optional.ofNullable(Boolean.FALSE));
        assertSame(Optional.of(""), Optional.ofNullable(""));
        assertTrue(Optional.TRUE.get());
        assertFalse(Optional.FALSE.get());

        assertEquals("test", present.orElse("other"));
        assertEquals("other", empty.orElse("other"));
        assertEquals("test", present.orElseGet(() -> "other"));
        assertEquals("other", empty.orElseGet(() -> "other"));
        assertEquals("test", present.orElseNull());
        assertNull(empty.orElseNull());
        assertEquals("test", present.orElseThrow());
        assertThrows(NoSuchElementException.class, empty::get);
        assertThrows(NoSuchElementException.class, empty::orElseThrow);
        assertTrue(present.contains("test"));
        assertFalse(present.contains("other"));
        assertFalse(empty.contains("test"));
    }

    @Test
    public void testOptionalMapFilterAndConvert() throws Exception {
        Optional<String> present = Optional.of("value");
        Optional<String> empty = Optional.empty();
        assertTrue(present.filter(s -> s.equals("value")).isPresent());
        assertFalse(present.filter(s -> s.equals("wrong")).isPresent());
        assertFalse(empty.filter(s -> true).isPresent());
        assertEquals(5, present.map(String::length).get());
        assertFalse(empty.map(String::length).isPresent());
        assertEquals(5, present.flatMap(s -> Optional.of(s.length())).get());
        assertFalse(empty.flatMap(s -> Optional.of(s.length())).isPresent());
        assertThrows(IllegalArgumentException.class, () -> present.flatMap(s -> null));

        AtomicBoolean called = new AtomicBoolean();
        present.ifPresent(v -> called.set(true));
        assertTrue(called.get());
        called.set(false);
        empty.ifPresent(v -> called.set(true));
        assertFalse(called.get());
        AtomicBoolean elseCalled = new AtomicBoolean();
        present.ifPresentOrElse(v -> called.set(true), () -> elseCalled.set(true));
        assertTrue(called.get());
        assertFalse(elseCalled.get());
        called.set(false);
        empty.ifPresentOrElse(v -> called.set(true), () -> elseCalled.set(true));
        assertFalse(called.get());
        assertTrue(elseCalled.get());
        assertThrows(IllegalArgumentException.class, () -> present.ifPresent(null));
        assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> present.ifPresentOrElse(s -> {
        }, null));

        AtomicBoolean mapped = new AtomicBoolean();
        empty.map(s -> {
            mapped.set(true);
            return s;
        });
        empty.filter(s -> {
            mapped.set(true);
            return true;
        });
        empty.flatMap(s -> {
            mapped.set(true);
            return Optional.of(s);
        });
        assertFalse(mapped.get());

        assertEquals(1, present.stream().count());
        assertEquals(0, empty.stream().count());
        assertEquals(List.of("value"), present.toList());
        assertTrue(empty.toList().isEmpty());
        assertEquals(Set.of("value"), present.toSet());
        assertTrue(present.toJdkOptional().isPresent());
        assertFalse(empty.toJdkOptional().isPresent());
        assertEquals(1, present.toImmutableList().size());
        assertTrue(empty.toImmutableSet().isEmpty());
        assertEquals("value", present.or(() -> Optional.of("second")).get());
        assertEquals("second", empty.or(() -> Optional.of("second")).get());
        assertEquals(5.0f, present.mapToFloat(s -> (float) s.length()).get());
        assertFalse(empty.mapToFloat(s -> 1f).isPresent());
    }

    @Test
    public void testNullable() throws Exception {
        Nullable<String> empty = Nullable.empty();
        Nullable<String> present = Nullable.of("test");
        Nullable<String> presentNull = Nullable.of(null);
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertTrue(empty.isNotPresent());
        assertTrue(empty.isNull());
        assertFalse(empty.isNotNull());
        assertTrue(present.isPresent());
        assertTrue(present.isNotNull());
        assertTrue(presentNull.isPresent());
        assertTrue(presentNull.isNull());
        assertFalse(presentNull.isNotNull());
        assertSame(Nullable.empty(), Nullable.empty());
        assertSame(Nullable.TRUE, Nullable.of(Boolean.TRUE));
        assertSame(Nullable.of(""), Nullable.of(""));

        AtomicBoolean called = new AtomicBoolean();
        present.ifNotNull(v -> called.set(true));
        assertTrue(called.get());
        called.set(false);
        presentNull.ifNotNull(v -> called.set(true));
        assertFalse(called.get());
        empty.ifNotNull(v -> called.set(true));
        assertFalse(called.get());
        present.ifNotNullOrElse(s -> called.set(true), () -> called.set(false));
        assertTrue(called.get());
        called.set(true);
        presentNull.ifNotNullOrElse(s -> called.set(true), () -> called.set(false));
        assertFalse(called.get());
        assertThrows(IllegalArgumentException.class, () -> present.ifNotNullOrElse(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> present.ifNotNullOrElse(s -> {
        }, null));

        assertEquals(4, present.flatMapIfNotNull(s -> Nullable.of(s.length())).get());
        assertFalse(presentNull.flatMapIfNotNull(s -> Nullable.of(10)).isPresent());
        assertFalse(empty.flatMapIfNotNull(s -> Nullable.of(10)).isPresent());
        assertThrows(IllegalArgumentException.class, () -> present.flatMapIfNotNull(null));
        assertThrows(IllegalArgumentException.class, () -> present.flatMap(s -> null));

        assertEquals(List.of("test"), present.toListIfNotNull());
        assertTrue(presentNull.toListIfNotNull().isEmpty());
        assertTrue(empty.toSetIfNotNull().isEmpty());
        assertEquals(1, present.toImmutableListIfNotNull().size());
        assertTrue(presentNull.toImmutableSetIfNotNull().isEmpty());
        assertEquals(1, present.streamIfNotNull().count());
        assertEquals(0, presentNull.streamIfNotNull().count());
        assertEquals("test", present.toOptional().get());
        assertFalse(presentNull.toOptional().isPresent());
        assertEquals("test", Nullable.from(Optional.of("test")).get());
        assertFalse(Nullable.from(Optional.empty()).isPresent());
        assertFalse(Nullable.from((java.util.Optional<String>) null).isPresent());
        assertTrue(present.filterIfNotNull(s -> s.length() > 1).isPresent());
        assertFalse(presentNull.filterIfNotNull(s -> true).isPresent());
    }

    @Test
    public void testPrimitiveFactoriesAndCache() {
        assertSame(OptionalBoolean.empty(), OptionalBoolean.empty());
        assertSame(OptionalChar.empty(), OptionalChar.empty());
        assertSame(OptionalByte.empty(), OptionalByte.empty());
        assertSame(OptionalShort.empty(), OptionalShort.empty());
        assertSame(OptionalInt.empty(), OptionalInt.empty());
        assertSame(OptionalLong.empty(), OptionalLong.empty());
        assertSame(OptionalFloat.empty(), OptionalFloat.empty());
        assertSame(OptionalDouble.empty(), OptionalDouble.empty());

        assertFalse(OptionalBoolean.ofNullable(null).isPresent());
        assertFalse(OptionalInt.ofNullable(null).isPresent());
        assertFalse(OptionalLong.ofNullable(null).isPresent());
        assertFalse(OptionalDouble.ofNullable(null).isPresent());
        assertTrue(OptionalInt.from(java.util.OptionalInt.of(42)).isPresent());
        assertTrue(OptionalInt.from(null).isEmpty());
        assertTrue(OptionalLong.from(null).isEmpty());
        assertTrue(OptionalDouble.from(null).isEmpty());
        assertEquals(42, OptionalInt.of(42).getAsInt());
        assertEquals(42L, OptionalLong.of(42L).getAsLong());
        assertEquals(42.5, OptionalDouble.of(42.5).getAsDouble());
        assertEquals(42, OptionalInt.of(42).toJdkOptional().getAsInt());
        assertFalse(OptionalInt.empty().toJdkOptional().isPresent());

        assertSame(OptionalInt.of(0), OptionalInt.of(0));
        assertSame(OptionalInt.of(-256), OptionalInt.of(-256));
        assertSame(OptionalInt.of(1024), OptionalInt.of(1024));
        assertNotSame(OptionalInt.of(99999), OptionalInt.of(99999));
        assertEquals(OptionalInt.of(99999), OptionalInt.of(99999));
        assertSame(OptionalChar.of((char) 0), OptionalChar.of((char) 0));
        assertSame(OptionalChar.of((char) 128), OptionalChar.of((char) 128));
        assertNotSame(OptionalChar.of((char) 129), OptionalChar.of((char) 129));
        assertSame(OptionalShort.of((short) -128), OptionalShort.of((short) -128));
        assertSame(OptionalShort.of((short) 256), OptionalShort.of((short) 256));
        assertNotSame(OptionalShort.of((short) 257), OptionalShort.of((short) 257));
        assertSame(OptionalLong.of(-256L), OptionalLong.of(-256L));
        assertSame(OptionalLong.of(1024L), OptionalLong.of(1024L));
        assertNotSame(OptionalLong.of(1025L), OptionalLong.of(1025L));
        assertSame(OptionalByte.of((byte) -128), OptionalByte.of((byte) -128));
        assertSame(OptionalByte.of((byte) 127), OptionalByte.of((byte) 127));
        assertSame(OptionalFloat.of(0.0f), OptionalFloat.of(0.0f));
        assertSame(OptionalDouble.of(0.0d), OptionalDouble.of(0.0d));
    }

    @Test
    public void testEqualsHashCodeToStringAndCompare() {
        assertEquals(Optional.of("A"), Optional.of("A"));
        assertNotEquals(Optional.of("A"), Optional.of("B"));
        assertEquals(Optional.empty(), Optional.empty());
        assertNotEquals(Optional.of("A"), Optional.empty());
        assertEquals("Optional[A]", Optional.of("A").toString());
        assertEquals("Optional.empty", Optional.empty().toString());
        assertEquals(0, Optional.empty().hashCode());

        assertEquals(Nullable.of("A"), Nullable.of("A"));
        assertEquals(Nullable.of(null), Nullable.of(null));
        assertEquals(Nullable.empty(), Nullable.empty());
        assertNotEquals(Nullable.of("A"), Nullable.of(null));
        assertNotEquals(Nullable.of("A"), Nullable.empty());
        assertNotEquals(Nullable.of(null), Nullable.empty());
        assertEquals("Nullable[val]", Nullable.of("val").toString());
        assertEquals("Nullable[null]", Nullable.of(null).toString());
        assertEquals("Nullable.empty", Nullable.empty().toString());
        assertEquals(0, Nullable.empty().hashCode());

        assertEquals(OptionalInt.of(1), OptionalInt.of(1));
        assertNotEquals(OptionalInt.of(1), OptionalInt.of(2));
        assertNotEquals(OptionalInt.of(1), OptionalLong.of(1L));
        assertNotEquals(OptionalInt.empty(), OptionalLong.empty());
        assertNotEquals(Optional.of("x"), Nullable.of("x"));
        assertNotEquals(Optional.of("x"), java.util.Optional.of("x"));
        assertEquals("OptionalInt[42]", OptionalInt.of(42).toString());
        assertEquals("OptionalBoolean[true]", OptionalBoolean.of(true).toString());
        assertEquals(0, OptionalBoolean.empty().hashCode());
        assertEquals(0, OptionalInt.empty().hashCode());
        assertEquals(0, OptionalDouble.empty().hashCode());

        assertEquals(Optional.of(true), OptionalBoolean.of(true).boxed());
        assertEquals(Optional.of(1), OptionalInt.of(1).boxed());
        assertEquals(Optional.empty(), OptionalBoolean.empty().boxed());
        assertEquals(Optional.of('a'), OptionalChar.of('a').boxed());
        assertEquals(Optional.empty(), OptionalChar.empty().boxed());
        assertEquals(java.util.Optional.of('a'), OptionalChar.of('a').boxed().toJdkOptional());
        assertEquals(java.util.Optional.empty(), OptionalChar.empty().boxed().toJdkOptional());

        assertThrows(NullPointerException.class, () -> OptionalInt.of(1).compareTo(null));
        assertThrows(NullPointerException.class, () -> OptionalInt.empty().compareTo(null));
        assertOptionalOrdering(OptionalBoolean.empty(), OptionalBoolean.of(false), OptionalBoolean.of(true));
        assertOptionalOrdering(OptionalInt.empty(), OptionalInt.of(Integer.MIN_VALUE), OptionalInt.of(Integer.MAX_VALUE));
        assertOptionalOrdering(OptionalFloat.empty(), OptionalFloat.of(-0f), OptionalFloat.of(0f));
        assertOptionalOrdering(OptionalFloat.empty(), OptionalFloat.of(Float.POSITIVE_INFINITY), OptionalFloat.of(Float.NaN));
        assertOptionalOrdering(OptionalDouble.empty(), OptionalDouble.of(-0d), OptionalDouble.of(0d));
        assertEquals(0, OptionalFloat.of(Float.NaN).compareTo(OptionalFloat.of(Float.intBitsToFloat(0x7fc00001))));
        assertEquals(0, OptionalDouble.of(Double.NaN).compareTo(OptionalDouble.of(Double.longBitsToDouble(0x7ff8000000000001L))));

        assertThrows(NoSuchElementException.class, () -> Optional.empty().get());
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().getAsBoolean());
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().getAsInt());
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().getAsLong());
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().getAsDouble());
    }

    @Test
    public void reviewFixes20260906_primitiveMapToXxxAcceptsASupertypeMapper() {
        final Throwables.ToIntFunction<Number, RuntimeException> toInt = Number::intValue;
        final Throwables.ToLongFunction<Number, RuntimeException> toLong = Number::longValue;
        final Throwables.ToDoubleFunction<Number, RuntimeException> toDouble = Number::doubleValue;

        assertEquals(OptionalInt.of(42), Optional.of(42L).mapToInt(toInt));
        assertEquals(OptionalInt.of(42), Nullable.of(42L).mapToInt(toInt));
        assertEquals(OptionalInt.of(42), OptionalLong.of(42L).mapToInt(toInt));
        assertEquals(OptionalInt.of(1), OptionalByte.of((byte) 1).mapToInt(toInt));
        assertEquals(OptionalInt.of(1), OptionalShort.of((short) 1).mapToInt(toInt));
        assertEquals(OptionalInt.of(2), OptionalFloat.of(2.7f).mapToInt(toInt));
        assertEquals(OptionalInt.of(3), OptionalDouble.of(3.9d).mapToInt(toInt));
        assertEquals(OptionalLong.of(42L), OptionalInt.of(42).mapToLong(toLong));
        assertEquals(OptionalDouble.of(1.5d), OptionalFloat.of(1.5f).mapToDouble(toDouble));

        assertEquals(OptionalInt.of(8), OptionalLong.of(7L).mapToInt(v -> (int) (v + 1)));
        assertEquals(OptionalInt.of(1), OptionalBoolean.of(true).mapToInt(v -> v ? 1 : 0));
        assertEquals(OptionalInt.empty(), OptionalLong.empty().mapToInt(toInt));
        assertThrows(IllegalArgumentException.class, () -> OptionalLong.of(1L).mapToInt(null));
    }

    @Test
    public void reviewFixes20260906_mapIfNotNullKeepsANullMapperResultPresent() {
        final Nullable<String> nullResult = Nullable.of("x").mapIfNotNull(v -> null);
        assertTrue(nullResult.isPresent());
        assertTrue(nullResult.isNull());
        assertNull(nullResult.orElse("d"));
        assertEquals("Nullable[null]", nullResult.toString());

        assertTrue(Nullable.of("x").map(v -> null).isPresent());
        assertFalse(Nullable.of((String) null).mapIfNotNull(v -> v).isPresent());
        assertFalse(Nullable.<String> empty().mapIfNotNull(v -> v).isPresent());
        assertEquals("X", Nullable.of("x").mapIfNotNull(String::toUpperCase).get());
    }

    @Test
    public void reviewFixes20260906_containsComparesArraysByIdentity() {
        final int[] arr = { 1, 2 };
        assertFalse(Optional.of(arr).contains(new int[] { 1, 2 }));
        assertTrue(Optional.of(arr).contains(arr));
        assertFalse(Nullable.of(arr).contains(new int[] { 1, 2 }));
        assertTrue(Nullable.of(arr).contains(arr));

        assertTrue(Optional.of("test").contains("test"));
        assertFalse(Optional.of("test").contains("hello"));
        assertFalse(Optional.<String> empty().contains("test"));
        assertTrue(Nullable.of((String) null).contains(null));
        assertFalse(Nullable.<String> empty().contains(null));
    }

    @Test
    public void reviewFixes20260906_nullablePresentNullIsReturnedByEveryOrElseThrowOverload() {
        final Nullable<String> pn = Nullable.of((String) null);
        assertNull(pn.orElseThrow());
        assertNull(pn.orElseThrow("boom"));
        assertNull(pn.orElseThrow("boom {}", "a"));
        assertNull(pn.orElseThrow("boom {} {}", "a", "b"));
        assertNull(pn.orElseThrow("boom {} {} {}", "a", "b", "c"));
        assertNull(pn.orElseThrow("boom {} {} {} {}", "a", "b", "c", "d"));
        assertNull(pn.orElseThrow(IllegalStateException::new));
        assertThrows(NoSuchElementException.class, pn::orElseThrowIfNull);
        assertThrows(IllegalStateException.class, () -> pn.orElseThrowIfNull(IllegalStateException::new));
        assertEquals("fallback", pn.orElseGetIfNull(() -> "fallback"));

        final Nullable<String> absent = Nullable.empty();
        assertThrows(NoSuchElementException.class, absent::orElseThrow);
        assertThrows(NoSuchElementException.class, () -> absent.orElseThrow("boom"));
        assertThrows(NoSuchElementException.class, () -> absent.orElseThrow("boom {}", "a"));
        assertThrows(NoSuchElementException.class, () -> absent.orElseThrow("boom {} {}", "a", "b"));
        assertThrows(NoSuchElementException.class, () -> absent.orElseThrow("boom {} {} {}", "a", "b", "c"));
        assertThrows(NoSuchElementException.class, () -> absent.orElseThrow("boom {} {} {} {}", "a", "b", "c", "d"));
        assertThrows(IllegalStateException.class, () -> absent.orElseThrow(IllegalStateException::new));
    }

    @Test
    public void reviewFixes20260906_mapToXxxJavadocExamplesRenderAsDocumented() {
        assertEquals("OptionalLong[1]", Optional.of("test").mapToLong(val -> 1L).toString());
        assertEquals("OptionalFloat[1.0]", Optional.of("test").mapToFloat(val -> 1.0f).toString());
        assertEquals("OptionalDouble[1.0]", Optional.of("test").mapToDouble(val -> 1).toString());
        assertEquals("OptionalLong[1]", Nullable.of("test").mapToLong(val -> 1L).toString());
        assertEquals("OptionalFloat[1.0]", Nullable.of("test").mapToFloat(val -> 1.0f).toString());
        assertEquals("OptionalDouble[1.0]", Nullable.of("test").mapToDouble(val -> 1).toString());
        assertEquals("OptionalChar[A]", Nullable.of("test").mapToCharIfNotNull(val -> 'A').toString());
        assertEquals("OptionalByte[1]", Optional.of("test").mapToByte(val -> 1).toString());
        assertEquals("OptionalShort[1]", Optional.of("test").mapToShort(val -> 1).toString());
        assertEquals("OptionalInt[1]", Optional.of("test").mapToInt(val -> 1).toString());
        assertEquals("OptionalChar[A]", Optional.of("test").mapToChar(val -> 'A').toString());
    }

    @Test
    public void reviewFixes20260906_stringOverloadWinsForEveryStringArgument() {
        final Optional<Object> a = Optional.of((Object) "abc");
        final Optional<CharSequence> b = Optional.ofNullable((CharSequence) "abc");
        final Nullable<Object> c = Nullable.of((Object) "abc");
        assertEquals("abc", a.get());
        assertEquals("abc", b.get());
        assertEquals("abc", c.get());
        final Optional<String> plain = Optional.of("abc");
        assertEquals("abc", plain.get());
        assertSame(Nullable.of((String) null), Nullable.of((String) null));
    }
}
