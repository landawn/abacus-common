package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
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

@Tag("2025")
public class u2025Test extends TestBase {

    @Test
    public void testOptionalBoolean_empty() {
        OptionalBoolean empty = OptionalBoolean.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
        assertSame(empty, OptionalBoolean.empty());
    }

    @Test
    public void testOptionalBoolean_of() {
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
    public void testOptionalBoolean_ofNullable() {
        OptionalBoolean ofTrue = OptionalBoolean.ofNullable(Boolean.TRUE);
        assertTrue(ofTrue.isPresent());
        assertTrue(ofTrue.get());

        OptionalBoolean ofFalse = OptionalBoolean.ofNullable(Boolean.FALSE);
        assertTrue(ofFalse.isPresent());
        assertFalse(ofFalse.get());

        OptionalBoolean ofNull = OptionalBoolean.ofNullable(null);
        assertFalse(ofNull.isPresent());
    }

    @Test
    public void testOptionalBoolean_get() {
        assertTrue(OptionalBoolean.of(true).get());
        assertFalse(OptionalBoolean.of(false).get());

        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().get());
    }

    @Test
    public void testOptionalBoolean_isPresent() {
        assertTrue(OptionalBoolean.of(true).isPresent());
        assertTrue(OptionalBoolean.of(false).isPresent());
        assertFalse(OptionalBoolean.empty().isPresent());
    }

    @Test
    public void testOptionalBoolean_isEmpty() {
        assertFalse(OptionalBoolean.of(true).isEmpty());
        assertFalse(OptionalBoolean.of(false).isEmpty());
        assertTrue(OptionalBoolean.empty().isEmpty());
    }

    @Test
    public void testOptionalBoolean_ifPresent() {
        AtomicBoolean called = new AtomicBoolean(false);

        OptionalBoolean.of(true).ifPresent(v -> called.set(v));
        assertTrue(called.get());

        called.set(false);
        OptionalBoolean.empty().ifPresent(v -> called.set(v));
        assertFalse(called.get());
    }

    @Test
    public void testOptionalBoolean_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalBoolean.of(true).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());

        actionCalled.set(false);
        OptionalBoolean.empty().ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertFalse(actionCalled.get());
        assertTrue(emptyActionCalled.get());
    }

    @Test
    public void testOptionalBoolean_filter() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        assertTrue(trueOpt.filter(v -> v).isPresent());
        assertFalse(trueOpt.filter(v -> !v).isPresent());

        OptionalBoolean falseOpt = OptionalBoolean.of(false);
        assertTrue(falseOpt.filter(v -> !v).isPresent());
        assertFalse(falseOpt.filter(v -> v).isPresent());

        assertFalse(OptionalBoolean.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalBoolean_map() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalBoolean mapped = trueOpt.map(v -> !v);
        assertTrue(mapped.isPresent());
        assertFalse(mapped.get());

        assertFalse(OptionalBoolean.empty().map(v -> !v).isPresent());
    }

    @Test
    public void testOptionalBoolean_mapToChar() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalChar mapped = trueOpt.mapToChar(v -> v ? 'T' : 'F');
        assertTrue(mapped.isPresent());
        assertEquals('T', mapped.get());

        assertFalse(OptionalBoolean.empty().mapToChar(v -> 'X').isPresent());
    }

    @Test
    public void testOptionalBoolean_mapToInt() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalInt mapped = trueOpt.mapToInt(v -> v ? 1 : 0);
        assertTrue(mapped.isPresent());
        assertEquals(1, mapped.get());

        assertFalse(OptionalBoolean.empty().mapToInt(v -> 1).isPresent());
    }

    @Test
    public void testOptionalBoolean_mapToLong() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalLong mapped = trueOpt.mapToLong(v -> v ? 1L : 0L);
        assertTrue(mapped.isPresent());
        assertEquals(1L, mapped.get());

        assertFalse(OptionalBoolean.empty().mapToLong(v -> 1L).isPresent());
    }

    @Test
    public void testOptionalBoolean_mapToDouble() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalDouble mapped = trueOpt.mapToDouble(v -> v ? 1.0 : 0.0);
        assertTrue(mapped.isPresent());
        assertEquals(1.0, mapped.get());

        assertFalse(OptionalBoolean.empty().mapToDouble(v -> 1.0).isPresent());
    }

    @Test
    public void testOptionalBoolean_mapToObj() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        Optional<String> mapped = trueOpt.mapToObj(v -> v ? "TRUE" : "FALSE");
        assertTrue(mapped.isPresent());
        assertEquals("TRUE", mapped.get());

        assertFalse(OptionalBoolean.empty().mapToObj(v -> "X").isPresent());
    }

    @Test
    public void testOptionalBoolean_flatMap() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        OptionalBoolean mapped = trueOpt.flatMap(v -> OptionalBoolean.of(!v));
        assertTrue(mapped.isPresent());
        assertFalse(mapped.get());

        assertFalse(OptionalBoolean.empty().flatMap(v -> OptionalBoolean.of(v)).isPresent());
    }

    @Test
    public void testOptionalBoolean_or() {
        OptionalBoolean trueOpt = OptionalBoolean.of(true);
        assertSame(trueOpt, trueOpt.or(() -> OptionalBoolean.of(false)));

        OptionalBoolean empty = OptionalBoolean.empty();
        OptionalBoolean result = empty.or(() -> OptionalBoolean.of(true));
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    public void testOptionalBoolean_orElse() {
        assertTrue(OptionalBoolean.of(true).orElse(false));
        assertFalse(OptionalBoolean.of(false).orElse(true));
        assertTrue(OptionalBoolean.empty().orElse(true));
        assertFalse(OptionalBoolean.empty().orElse(false));
    }

    @Test
    public void testOptionalBoolean_orElseGet() {
        assertTrue(OptionalBoolean.of(true).orElseGet(() -> false));
        assertFalse(OptionalBoolean.of(false).orElseGet(() -> true));
        assertTrue(OptionalBoolean.empty().orElseGet(() -> true));
    }

    @Test
    public void testOptionalBoolean_orElseThrow() {
        assertTrue(OptionalBoolean.of(true).orElseThrow());
        assertFalse(OptionalBoolean.of(false).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow());
    }

    @Test
    public void testOptionalBoolean_orElseThrow_withSupplier() {
        assertTrue(OptionalBoolean.of(true).orElseThrow(IllegalStateException::new));

        assertThrows(IllegalStateException.class, () -> OptionalBoolean.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalBoolean_stream() {
        assertEquals(1, OptionalBoolean.of(true).stream().count());
        assertEquals(1, OptionalBoolean.of(false).stream().count());
        assertEquals(0, OptionalBoolean.empty().stream().count());

        assertTrue(OptionalBoolean.of(true).stream().findFirst().isPresent());
    }

    @Test
    public void testOptionalBoolean_equals() {
        assertEquals(OptionalBoolean.of(true), OptionalBoolean.of(true));
        assertEquals(OptionalBoolean.of(false), OptionalBoolean.of(false));
        assertEquals(OptionalBoolean.empty(), OptionalBoolean.empty());

        assertNotEquals(OptionalBoolean.of(true), OptionalBoolean.of(false));
        assertNotEquals(OptionalBoolean.of(true), OptionalBoolean.empty());
        assertNotEquals(OptionalBoolean.empty(), OptionalBoolean.of(false));
    }

    @Test
    public void testOptionalBoolean_hashCode() {
        assertEquals(OptionalBoolean.of(true).hashCode(), OptionalBoolean.of(true).hashCode());
        assertEquals(OptionalBoolean.of(false).hashCode(), OptionalBoolean.of(false).hashCode());
        assertEquals(OptionalBoolean.empty().hashCode(), OptionalBoolean.empty().hashCode());
    }

    @Test
    public void testOptionalBoolean_toString() {
        assertTrue(OptionalBoolean.of(true).toString().contains("true"));
        assertTrue(OptionalBoolean.of(false).toString().contains("false"));
        assertTrue(OptionalBoolean.empty().toString().contains("empty") || OptionalBoolean.empty().toString().contains("Optional"));
    }

    @Test
    public void testOptionalBoolean_compareTo() {
        assertEquals(0, OptionalBoolean.of(true).compareTo(OptionalBoolean.of(true)));
        assertEquals(0, OptionalBoolean.of(false).compareTo(OptionalBoolean.of(false)));
        assertEquals(0, OptionalBoolean.empty().compareTo(OptionalBoolean.empty()));

        assertTrue(OptionalBoolean.of(true).compareTo(OptionalBoolean.of(false)) > 0);
        assertTrue(OptionalBoolean.of(false).compareTo(OptionalBoolean.of(true)) < 0);
        assertTrue(OptionalBoolean.of(true).compareTo(OptionalBoolean.empty()) > 0);
        assertTrue(OptionalBoolean.empty().compareTo(OptionalBoolean.of(true)) < 0);
    }

    @Test
    public void testOptionalChar_empty() {
        OptionalChar empty = OptionalChar.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalChar_of() {
        OptionalChar opt = OptionalChar.of('A');
        assertTrue(opt.isPresent());
        assertEquals('A', opt.get());
    }

    @Test
    public void testOptionalChar_ofNullable() {
        OptionalChar opt = OptionalChar.ofNullable('B');
        assertTrue(opt.isPresent());
        assertEquals('B', opt.get());

        OptionalChar nullOpt = OptionalChar.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalChar_get() {
        assertEquals('C', OptionalChar.of('C').get());
        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().get());
    }

    @Test
    public void testOptionalChar_isPresent() {
        assertTrue(OptionalChar.of('A').isPresent());
        assertFalse(OptionalChar.empty().isPresent());
    }

    @Test
    public void testOptionalChar_isEmpty() {
        assertFalse(OptionalChar.of('A').isEmpty());
        assertTrue(OptionalChar.empty().isEmpty());
    }

    @Test
    public void testOptionalChar_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalChar.of('X').ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());

        OptionalChar.empty().ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());
    }

    @Test
    public void testOptionalChar_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalChar.of('A').ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());

        actionCalled.set(false);
        OptionalChar.empty().ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertFalse(actionCalled.get());
        assertTrue(emptyActionCalled.get());
    }

    @Test
    public void testOptionalChar_filter() {
        OptionalChar opt = OptionalChar.of('A');
        assertTrue(opt.filter(v -> v == 'A').isPresent());
        assertFalse(opt.filter(v -> v == 'B').isPresent());
        assertFalse(OptionalChar.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalChar_map() {
        OptionalChar opt = OptionalChar.of('a');
        OptionalChar mapped = opt.map(v -> Character.toUpperCase(v));
        assertTrue(mapped.isPresent());
        assertEquals('A', mapped.get());

        assertFalse(OptionalChar.empty().map(v -> v).isPresent());
    }

    @Test
    public void testOptionalChar_mapToInt() {
        OptionalChar opt = OptionalChar.of('A');
        OptionalInt mapped = opt.mapToInt(v -> (int) v);
        assertTrue(mapped.isPresent());
        assertEquals(65, mapped.get());

        assertFalse(OptionalChar.empty().mapToInt(v -> (int) v).isPresent());
    }

    @Test
    public void testOptionalChar_mapToObj() {
        OptionalChar opt = OptionalChar.of('A');
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("A", mapped.get());

        assertFalse(OptionalChar.empty().mapToObj(v -> String.valueOf(v)).isPresent());
    }

    @Test
    public void testOptionalChar_flatMap() {
        OptionalChar opt = OptionalChar.of('A');
        OptionalChar mapped = opt.flatMap(v -> OptionalChar.of((char) (v + 1)));
        assertTrue(mapped.isPresent());
        assertEquals('B', mapped.get());

        assertFalse(OptionalChar.empty().flatMap(v -> OptionalChar.of(v)).isPresent());
    }

    @Test
    public void testOptionalChar_or() {
        OptionalChar opt = OptionalChar.of('A');
        assertSame(opt, opt.or(() -> OptionalChar.of('B')));

        OptionalChar result = OptionalChar.empty().or(() -> OptionalChar.of('B'));
        assertTrue(result.isPresent());
        assertEquals('B', result.get());
    }

    @Test
    public void testOptionalChar_orElse() {
        assertEquals('A', OptionalChar.of('A').orElse('B'));
        assertEquals('B', OptionalChar.empty().orElse('B'));
    }

    @Test
    public void testOptionalChar_orElseGet() {
        assertEquals('A', OptionalChar.of('A').orElseGet(() -> 'B'));
        assertEquals('B', OptionalChar.empty().orElseGet(() -> 'B'));
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
    public void testOptionalChar_equals() {
        assertEquals(OptionalChar.of('A'), OptionalChar.of('A'));
        assertNotEquals(OptionalChar.of('A'), OptionalChar.of('B'));
        assertNotEquals(OptionalChar.of('A'), OptionalChar.empty());
    }

    @Test
    public void testOptionalChar_hashCode() {
        assertEquals(OptionalChar.of('A').hashCode(), OptionalChar.of('A').hashCode());
    }

    @Test
    public void testOptionalChar_toString() {
        assertTrue(OptionalChar.of('A').toString().contains("A"));
    }

    @Test
    public void testOptionalChar_compareTo() {
        assertEquals(0, OptionalChar.of('A').compareTo(OptionalChar.of('A')));
        assertTrue(OptionalChar.of('B').compareTo(OptionalChar.of('A')) > 0);
        assertTrue(OptionalChar.of('A').compareTo(OptionalChar.of('B')) < 0);
    }

    @Test
    public void testOptionalByte_empty() {
        OptionalByte empty = OptionalByte.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalByte_of() {
        OptionalByte opt = OptionalByte.of((byte) 42);
        assertTrue(opt.isPresent());
        assertEquals((byte) 42, opt.get());
    }

    @Test
    public void testOptionalByte_ofNullable() {
        OptionalByte opt = OptionalByte.ofNullable(Byte.valueOf((byte) 10));
        assertTrue(opt.isPresent());
        assertEquals((byte) 10, opt.get());

        OptionalByte nullOpt = OptionalByte.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalByte_get() {
        assertEquals((byte) 5, OptionalByte.of((byte) 5).get());
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().get());
    }

    @Test
    public void testOptionalByte_isPresent() {
        assertTrue(OptionalByte.of((byte) 1).isPresent());
        assertFalse(OptionalByte.empty().isPresent());
    }

    @Test
    public void testOptionalByte_isEmpty() {
        assertFalse(OptionalByte.of((byte) 1).isEmpty());
        assertTrue(OptionalByte.empty().isEmpty());
    }

    @Test
    public void testOptionalByte_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalByte.of((byte) 10).ifPresent(v -> called.set(v));
        assertEquals(10, called.get());

        called.set(0);
        OptionalByte.empty().ifPresent(v -> called.set(v));
        assertEquals(0, called.get());
    }

    @Test
    public void testOptionalByte_filter() {
        OptionalByte opt = OptionalByte.of((byte) 10);
        assertTrue(opt.filter(v -> v == 10).isPresent());
        assertFalse(opt.filter(v -> v == 20).isPresent());
        assertFalse(OptionalByte.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalByte_map() {
        OptionalByte opt = OptionalByte.of((byte) 10);
        OptionalByte mapped = opt.map(v -> (byte) (v * 2));
        assertTrue(mapped.isPresent());
        assertEquals((byte) 20, mapped.get());

        assertFalse(OptionalByte.empty().map(v -> v).isPresent());
    }

    @Test
    public void testOptionalByte_mapToInt() {
        OptionalByte opt = OptionalByte.of((byte) 10);
        OptionalInt mapped = opt.mapToInt(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(20, mapped.get());

        assertFalse(OptionalByte.empty().mapToInt(v -> v).isPresent());
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
    }

    @Test
    public void testOptionalByte_stream() {
        assertEquals(1, OptionalByte.of((byte) 10).stream().count());
        assertEquals(0, OptionalByte.empty().stream().count());
    }

    @Test
    public void testOptionalByte_equals() {
        assertEquals(OptionalByte.of((byte) 10), OptionalByte.of((byte) 10));
        assertNotEquals(OptionalByte.of((byte) 10), OptionalByte.of((byte) 20));
        assertNotEquals(OptionalByte.of((byte) 10), OptionalByte.empty());
    }

    @Test
    public void testOptionalByte_compareTo() {
        assertEquals(0, OptionalByte.of((byte) 10).compareTo(OptionalByte.of((byte) 10)));
        assertTrue(OptionalByte.of((byte) 20).compareTo(OptionalByte.of((byte) 10)) > 0);
    }

    @Test
    public void testOptionalShort_empty() {
        OptionalShort empty = OptionalShort.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalShort_of() {
        OptionalShort opt = OptionalShort.of((short) 100);
        assertTrue(opt.isPresent());
        assertEquals((short) 100, opt.get());
    }

    @Test
    public void testOptionalShort_ofNullable() {
        OptionalShort opt = OptionalShort.ofNullable(Short.valueOf((short) 100));
        assertTrue(opt.isPresent());
        assertEquals((short) 100, opt.get());

        OptionalShort nullOpt = OptionalShort.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalShort_get() {
        assertEquals((short) 100, OptionalShort.of((short) 100).get());
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().get());
    }

    @Test
    public void testOptionalShort_isPresent() {
        assertTrue(OptionalShort.of((short) 1).isPresent());
        assertFalse(OptionalShort.empty().isPresent());
    }

    @Test
    public void testOptionalShort_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalShort.of((short) 100).ifPresent(v -> called.set(v));
        assertEquals(100, called.get());
    }

    @Test
    public void testOptionalShort_filter() {
        OptionalShort opt = OptionalShort.of((short) 100);
        assertTrue(opt.filter(v -> v == 100).isPresent());
        assertFalse(opt.filter(v -> v == 200).isPresent());
    }

    @Test
    public void testOptionalShort_map() {
        OptionalShort opt = OptionalShort.of((short) 100);
        OptionalShort mapped = opt.map(v -> (short) (v * 2));
        assertTrue(mapped.isPresent());
        assertEquals((short) 200, mapped.get());
    }

    @Test
    public void testOptionalShort_mapToInt() {
        OptionalShort opt = OptionalShort.of((short) 100);
        OptionalInt mapped = opt.mapToInt(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(200, mapped.get());
    }

    @Test
    public void testOptionalShort_orElse() {
        assertEquals((short) 100, OptionalShort.of((short) 100).orElse((short) 200));
        assertEquals((short) 200, OptionalShort.empty().orElse((short) 200));
    }

    @Test
    public void testOptionalShort_orElseThrow() {
        assertEquals((short) 100, OptionalShort.of((short) 100).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow());
    }

    @Test
    public void testOptionalShort_stream() {
        assertEquals(1, OptionalShort.of((short) 100).stream().count());
        assertEquals(0, OptionalShort.empty().stream().count());
    }

    @Test
    public void testOptionalShort_equals() {
        assertEquals(OptionalShort.of((short) 100), OptionalShort.of((short) 100));
        assertNotEquals(OptionalShort.of((short) 100), OptionalShort.empty());
    }

    @Test
    public void testOptionalShort_compareTo() {
        assertEquals(0, OptionalShort.of((short) 100).compareTo(OptionalShort.of((short) 100)));
        assertTrue(OptionalShort.of((short) 200).compareTo(OptionalShort.of((short) 100)) > 0);
    }

    @Test
    public void testOptionalInt_empty() {
        OptionalInt empty = OptionalInt.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalInt_of() {
        OptionalInt opt = OptionalInt.of(42);
        assertTrue(opt.isPresent());
        assertEquals(42, opt.get());
    }

    @Test
    public void testOptionalInt_ofNullable() {
        OptionalInt opt = OptionalInt.ofNullable(Integer.valueOf(42));
        assertTrue(opt.isPresent());
        assertEquals(42, opt.get());

        OptionalInt nullOpt = OptionalInt.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalInt_get() {
        assertEquals(42, OptionalInt.of(42).get());
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().get());
    }

    @Test
    public void testOptionalInt_isPresent() {
        assertTrue(OptionalInt.of(42).isPresent());
        assertFalse(OptionalInt.empty().isPresent());
    }

    @Test
    public void testOptionalInt_isEmpty() {
        assertFalse(OptionalInt.of(42).isEmpty());
        assertTrue(OptionalInt.empty().isEmpty());
    }

    @Test
    public void testOptionalInt_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalInt.of(42).ifPresent(v -> called.set(v));
        assertEquals(42, called.get());

        called.set(0);
        OptionalInt.empty().ifPresent(v -> called.set(v));
        assertEquals(0, called.get());
    }

    @Test
    public void testOptionalInt_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalInt.of(42).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptionalInt_filter() {
        OptionalInt opt = OptionalInt.of(42);
        assertTrue(opt.filter(v -> v == 42).isPresent());
        assertFalse(opt.filter(v -> v == 100).isPresent());
        assertFalse(OptionalInt.empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptionalInt_map() {
        OptionalInt opt = OptionalInt.of(42);
        OptionalInt mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(84, mapped.get());

        assertFalse(OptionalInt.empty().map(v -> v * 2).isPresent());
    }

    @Test
    public void testOptionalInt_mapToLong() {
        OptionalInt opt = OptionalInt.of(42);
        OptionalLong mapped = opt.mapToLong(v -> (long) v);
        assertTrue(mapped.isPresent());
        assertEquals(42L, mapped.get());
    }

    @Test
    public void testOptionalInt_mapToDouble() {
        OptionalInt opt = OptionalInt.of(42);
        OptionalDouble mapped = opt.mapToDouble(v -> (double) v);
        assertTrue(mapped.isPresent());
        assertEquals(42.0, mapped.get());
    }

    @Test
    public void testOptionalInt_mapToObj() {
        OptionalInt opt = OptionalInt.of(42);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("42", mapped.get());
    }

    @Test
    public void testOptionalInt_flatMap() {
        OptionalInt opt = OptionalInt.of(42);
        OptionalInt mapped = opt.flatMap(v -> OptionalInt.of(v * 2));
        assertTrue(mapped.isPresent());
        assertEquals(84, mapped.get());
    }

    @Test
    public void testOptionalInt_or() {
        OptionalInt opt = OptionalInt.of(42);
        assertSame(opt, opt.or(() -> OptionalInt.of(100)));

        OptionalInt result = OptionalInt.empty().or(() -> OptionalInt.of(100));
        assertTrue(result.isPresent());
        assertEquals(100, result.get());
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
    public void testOptionalInt_orElseThrow() {
        assertEquals(42, OptionalInt.of(42).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow());
    }

    @Test
    public void testOptionalInt_orElseThrow_withSupplier() {
        assertEquals(42, OptionalInt.of(42).orElseThrow(IllegalStateException::new));
        assertThrows(IllegalStateException.class, () -> OptionalInt.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalInt_stream() {
        assertEquals(1, OptionalInt.of(42).stream().count());
        assertEquals(0, OptionalInt.empty().stream().count());
    }

    @Test
    public void testOptionalInt_equals() {
        assertEquals(OptionalInt.of(42), OptionalInt.of(42));
        assertNotEquals(OptionalInt.of(42), OptionalInt.of(100));
        assertNotEquals(OptionalInt.of(42), OptionalInt.empty());
    }

    @Test
    public void testOptionalInt_hashCode() {
        assertEquals(OptionalInt.of(42).hashCode(), OptionalInt.of(42).hashCode());
    }

    @Test
    public void testOptionalInt_toString() {
        assertTrue(OptionalInt.of(42).toString().contains("42"));
    }

    @Test
    public void testOptionalInt_compareTo() {
        assertEquals(0, OptionalInt.of(42).compareTo(OptionalInt.of(42)));
        assertTrue(OptionalInt.of(100).compareTo(OptionalInt.of(42)) > 0);
        assertTrue(OptionalInt.of(42).compareTo(OptionalInt.of(100)) < 0);
    }

    @Test
    public void testOptionalLong_empty() {
        OptionalLong empty = OptionalLong.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalLong_of() {
        OptionalLong opt = OptionalLong.of(123L);
        assertTrue(opt.isPresent());
        assertEquals(123L, opt.get());
    }

    @Test
    public void testOptionalLong_ofNullable() {
        OptionalLong opt = OptionalLong.ofNullable(Long.valueOf(123L));
        assertTrue(opt.isPresent());
        assertEquals(123L, opt.get());

        OptionalLong nullOpt = OptionalLong.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalLong_get() {
        assertEquals(123L, OptionalLong.of(123L).get());
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().get());
    }

    @Test
    public void testOptionalLong_isPresent() {
        assertTrue(OptionalLong.of(123L).isPresent());
        assertFalse(OptionalLong.empty().isPresent());
    }

    @Test
    public void testOptionalLong_isEmpty() {
        assertFalse(OptionalLong.of(123L).isEmpty());
        assertTrue(OptionalLong.empty().isEmpty());
    }

    @Test
    public void testOptionalLong_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalLong.of(123L).ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());
    }

    @Test
    public void testOptionalLong_filter() {
        OptionalLong opt = OptionalLong.of(123L);
        assertTrue(opt.filter(v -> v == 123L).isPresent());
        assertFalse(opt.filter(v -> v == 456L).isPresent());
    }

    @Test
    public void testOptionalLong_map() {
        OptionalLong opt = OptionalLong.of(123L);
        OptionalLong mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(246L, mapped.get());
    }

    @Test
    public void testOptionalLong_mapToInt() {
        OptionalLong opt = OptionalLong.of(123L);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(123, mapped.get());
    }

    @Test
    public void testOptionalLong_mapToDouble() {
        OptionalLong opt = OptionalLong.of(123L);
        OptionalDouble mapped = opt.mapToDouble(v -> (double) v);
        assertTrue(mapped.isPresent());
        assertEquals(123.0, mapped.get());
    }

    @Test
    public void testOptionalLong_mapToObj() {
        OptionalLong opt = OptionalLong.of(123L);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("123", mapped.get());
    }

    @Test
    public void testOptionalLong_flatMap() {
        OptionalLong opt = OptionalLong.of(123L);
        OptionalLong mapped = opt.flatMap(v -> OptionalLong.of(v * 2));
        assertTrue(mapped.isPresent());
        assertEquals(246L, mapped.get());
    }

    @Test
    public void testOptionalLong_or() {
        OptionalLong opt = OptionalLong.of(123L);
        assertSame(opt, opt.or(() -> OptionalLong.of(456L)));

        OptionalLong result = OptionalLong.empty().or(() -> OptionalLong.of(456L));
        assertTrue(result.isPresent());
        assertEquals(456L, result.get());
    }

    @Test
    public void testOptionalLong_orElse() {
        assertEquals(123L, OptionalLong.of(123L).orElse(456L));
        assertEquals(456L, OptionalLong.empty().orElse(456L));
    }

    @Test
    public void testOptionalLong_orElseGet() {
        assertEquals(123L, OptionalLong.of(123L).orElseGet(() -> 456L));
        assertEquals(456L, OptionalLong.empty().orElseGet(() -> 456L));
    }

    @Test
    public void testOptionalLong_orElseThrow() {
        assertEquals(123L, OptionalLong.of(123L).orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow());
    }

    @Test
    public void testOptionalLong_stream() {
        assertEquals(1, OptionalLong.of(123L).stream().count());
        assertEquals(0, OptionalLong.empty().stream().count());
    }

    @Test
    public void testOptionalLong_equals() {
        assertEquals(OptionalLong.of(123L), OptionalLong.of(123L));
        assertNotEquals(OptionalLong.of(123L), OptionalLong.empty());
    }

    @Test
    public void testOptionalLong_compareTo() {
        assertEquals(0, OptionalLong.of(123L).compareTo(OptionalLong.of(123L)));
        assertTrue(OptionalLong.of(456L).compareTo(OptionalLong.of(123L)) > 0);
    }

    @Test
    public void testOptionalFloat_empty() {
        OptionalFloat empty = OptionalFloat.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalFloat_of() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        assertTrue(opt.isPresent());
        assertEquals(3.14f, opt.get(), 0.001f);
    }

    @Test
    public void testOptionalFloat_of_zero() {
        OptionalFloat zero1 = OptionalFloat.of(0.0f);
        OptionalFloat zero2 = OptionalFloat.of(0.0f);
        assertSame(zero1, zero2);
    }

    @Test
    public void testOptionalFloat_ofNullable() {
        OptionalFloat opt = OptionalFloat.ofNullable(Float.valueOf(3.14f));
        assertTrue(opt.isPresent());
        assertEquals(3.14f, opt.get(), 0.001f);

        OptionalFloat nullOpt = OptionalFloat.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalFloat_get() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).get(), 0.001f);
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().get());
    }

    @Test
    public void testOptionalFloat_isPresent() {
        assertTrue(OptionalFloat.of(3.14f).isPresent());
        assertFalse(OptionalFloat.empty().isPresent());
    }

    @Test
    public void testOptionalFloat_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalFloat.of(3.14f).ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());
    }

    @Test
    public void testOptionalFloat_filter() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        assertTrue(opt.filter(v -> v > 3.0f).isPresent());
        assertFalse(opt.filter(v -> v > 4.0f).isPresent());
    }

    @Test
    public void testOptionalFloat_map() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        OptionalFloat mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(6.28f, mapped.get(), 0.001f);
    }

    @Test
    public void testOptionalFloat_mapToDouble() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        OptionalDouble mapped = opt.mapToDouble(v -> (double) v);
        assertTrue(mapped.isPresent());
        assertEquals(3.14, mapped.get(), 0.001);
    }

    @Test
    public void testOptionalFloat_mapToObj() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get().contains("3.14"));
    }

    @Test
    public void testOptionalFloat_flatMap() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        OptionalFloat mapped = opt.flatMap(v -> OptionalFloat.of(v * 2));
        assertTrue(mapped.isPresent());
        assertEquals(6.28f, mapped.get(), 0.001f);
    }

    @Test
    public void testOptionalFloat_or() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        assertSame(opt, opt.or(() -> OptionalFloat.of(2.71f)));

        OptionalFloat result = OptionalFloat.empty().or(() -> OptionalFloat.of(2.71f));
        assertTrue(result.isPresent());
        assertEquals(2.71f, result.get(), 0.001f);
    }

    @Test
    public void testOptionalFloat_orElse() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).orElse(2.71f), 0.001f);
        assertEquals(2.71f, OptionalFloat.empty().orElse(2.71f), 0.001f);
    }

    @Test
    public void testOptionalFloat_orElseGet() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).orElseGet(() -> 2.71f), 0.001f);
        assertEquals(2.71f, OptionalFloat.empty().orElseGet(() -> 2.71f), 0.001f);
    }

    @Test
    public void testOptionalFloat_orElseThrow() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).orElseThrow(), 0.001f);
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow());
    }

    @Test
    public void testOptionalFloat_stream() {
        assertEquals(1, OptionalFloat.of(3.14f).stream().count());
        assertEquals(0, OptionalFloat.empty().stream().count());
    }

    @Test
    public void testOptionalFloat_equals() {
        assertEquals(OptionalFloat.of(3.14f), OptionalFloat.of(3.14f));
        assertNotEquals(OptionalFloat.of(3.14f), OptionalFloat.empty());
    }

    @Test
    public void testOptionalFloat_compareTo() {
        assertEquals(0, OptionalFloat.of(3.14f).compareTo(OptionalFloat.of(3.14f)));
        assertTrue(OptionalFloat.of(4.0f).compareTo(OptionalFloat.of(3.0f)) > 0);
    }

    @Test
    public void testOptionalDouble_empty() {
        OptionalDouble empty = OptionalDouble.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptionalDouble_of() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        assertTrue(opt.isPresent());
        assertEquals(3.14159, opt.get(), 0.00001);
    }

    @Test
    public void testOptionalDouble_of_zero() {
        OptionalDouble zero1 = OptionalDouble.of(0.0);
        OptionalDouble zero2 = OptionalDouble.of(0.0);
        assertSame(zero1, zero2);
    }

    @Test
    public void testOptionalDouble_ofNullable() {
        OptionalDouble opt = OptionalDouble.ofNullable(Double.valueOf(3.14159));
        assertTrue(opt.isPresent());
        assertEquals(3.14159, opt.get(), 0.00001);

        OptionalDouble nullOpt = OptionalDouble.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptionalDouble_get() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).get(), 0.00001);
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().get());
    }

    @Test
    public void testOptionalDouble_isPresent() {
        assertTrue(OptionalDouble.of(3.14159).isPresent());
        assertFalse(OptionalDouble.empty().isPresent());
    }

    @Test
    public void testOptionalDouble_isEmpty() {
        assertFalse(OptionalDouble.of(3.14159).isEmpty());
        assertTrue(OptionalDouble.empty().isEmpty());
    }

    @Test
    public void testOptionalDouble_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        OptionalDouble.of(3.14159).ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());
    }

    @Test
    public void testOptionalDouble_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalDouble.of(3.14159).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptionalDouble_filter() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        assertTrue(opt.filter(v -> v > 3.0).isPresent());
        assertFalse(opt.filter(v -> v > 4.0).isPresent());
    }

    @Test
    public void testOptionalDouble_map() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        OptionalDouble mapped = opt.map(v -> v * 2);
        assertTrue(mapped.isPresent());
        assertEquals(6.28318, mapped.get(), 0.00001);
    }

    @Test
    public void testOptionalDouble_mapToInt() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(3, mapped.get());
    }

    @Test
    public void testOptionalDouble_mapToLong() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        OptionalLong mapped = opt.mapToLong(v -> v.longValue());
        assertTrue(mapped.isPresent());
        assertEquals(3L, mapped.get());
    }

    @Test
    public void testOptionalDouble_mapToObj() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get().contains("3.14"));
    }

    @Test
    public void testOptionalDouble_flatMap() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        OptionalDouble mapped = opt.flatMap(v -> OptionalDouble.of(v * 2));
        assertTrue(mapped.isPresent());
        assertEquals(6.28318, mapped.get(), 0.00001);
    }

    @Test
    public void testOptionalDouble_or() {
        OptionalDouble opt = OptionalDouble.of(3.14159);
        assertSame(opt, opt.or(() -> OptionalDouble.of(2.71828)));

        OptionalDouble result = OptionalDouble.empty().or(() -> OptionalDouble.of(2.71828));
        assertTrue(result.isPresent());
        assertEquals(2.71828, result.get(), 0.00001);
    }

    @Test
    public void testOptionalDouble_orElse() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).orElse(2.71828), 0.00001);
        assertEquals(2.71828, OptionalDouble.empty().orElse(2.71828), 0.00001);
    }

    @Test
    public void testOptionalDouble_orElseGet() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).orElseGet(() -> 2.71828), 0.00001);
        assertEquals(2.71828, OptionalDouble.empty().orElseGet(() -> 2.71828), 0.00001);
    }

    @Test
    public void testOptionalDouble_orElseThrow() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).orElseThrow(), 0.00001);
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow());
    }

    @Test
    public void testOptionalDouble_orElseThrow_withSupplier() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).orElseThrow(IllegalStateException::new), 0.00001);
        assertThrows(IllegalStateException.class, () -> OptionalDouble.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalDouble_stream() {
        assertEquals(1, OptionalDouble.of(3.14159).stream().count());
        assertEquals(0, OptionalDouble.empty().stream().count());
    }

    @Test
    public void testOptionalDouble_equals() {
        assertEquals(OptionalDouble.of(3.14159), OptionalDouble.of(3.14159));
        assertNotEquals(OptionalDouble.of(3.14159), OptionalDouble.empty());
    }

    @Test
    public void testOptionalDouble_hashCode() {
        assertEquals(OptionalDouble.of(3.14159).hashCode(), OptionalDouble.of(3.14159).hashCode());
    }

    @Test
    public void testOptionalDouble_toString() {
        assertTrue(OptionalDouble.of(3.14159).toString().contains("3.14"));
    }

    @Test
    public void testOptionalDouble_compareTo() {
        assertEquals(0, OptionalDouble.of(3.14159).compareTo(OptionalDouble.of(3.14159)));
        assertTrue(OptionalDouble.of(4.0).compareTo(OptionalDouble.of(3.0)) > 0);
        assertTrue(OptionalDouble.of(3.0).compareTo(OptionalDouble.of(4.0)) < 0);
    }

    @Test
    public void testOptional_empty() {
        Optional<String> empty = Optional.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testOptional_of() {
        Optional<String> opt = Optional.of("Hello");
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        assertThrows(NullPointerException.class, () -> Optional.of(null));
    }

    @Test
    public void testOptional_ofNullable() {
        Optional<String> opt = Optional.ofNullable("Hello");
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        Optional<String> nullOpt = Optional.ofNullable(null);
        assertFalse(nullOpt.isPresent());
    }

    @Test
    public void testOptional_get() {
        assertEquals("Hello", Optional.of("Hello").get());
        assertThrows(NoSuchElementException.class, () -> Optional.empty().get());
    }

    @Test
    public void testOptional_isPresent() {
        assertTrue(Optional.of("Hello").isPresent());
        assertFalse(Optional.empty().isPresent());
    }

    @Test
    public void testOptional_isEmpty() {
        assertFalse(Optional.of("Hello").isEmpty());
        assertTrue(Optional.empty().isEmpty());
    }

    @Test
    public void testOptional_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        Optional.of("Hello").ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());

        called.set(0);
        Optional.empty().ifPresent(v -> called.incrementAndGet());
        assertEquals(0, called.get());
    }

    @Test
    public void testOptional_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        Optional.of("Hello").ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptional_filter() {
        Optional<String> opt = Optional.of("Hello");
        assertTrue(opt.filter(v -> v.length() == 5).isPresent());
        assertFalse(opt.filter(v -> v.length() == 10).isPresent());
        assertFalse(Optional.<String> empty().filter(v -> true).isPresent());
    }

    @Test
    public void testOptional_map() {
        Optional<String> opt = Optional.of("Hello");
        Optional<Integer> mapped = opt.map(v -> v.length());
        assertTrue(mapped.isPresent());
        assertEquals(Integer.valueOf(5), mapped.get());

        assertFalse(Optional.<String> empty().map(v -> v.length()).isPresent());
    }

    @Test
    public void testOptional_flatMap() {
        Optional<String> opt = Optional.of("Hello");
        Optional<String> mapped = opt.flatMap(v -> Optional.of(v.toUpperCase()));
        assertTrue(mapped.isPresent());
        assertEquals("HELLO", mapped.get());

        assertFalse(Optional.<String> empty().flatMap(v -> Optional.of(v)).isPresent());
    }

    @Test
    public void testOptional_or() {
        Optional<String> opt = Optional.of("Hello");
        assertSame(opt, opt.or(() -> Optional.of("World")));

        Optional<String> result = Optional.<String> empty().or(() -> Optional.of("World"));
        assertTrue(result.isPresent());
        assertEquals("World", result.get());
    }

    @Test
    public void testOptional_orElse() {
        assertEquals("Hello", Optional.of("Hello").orElse("World"));
        assertEquals("World", Optional.<String> empty().orElse("World"));
    }

    @Test
    public void testOptional_orElseGet() {
        assertEquals("Hello", Optional.of("Hello").orElseGet(() -> "World"));
        assertEquals("World", Optional.<String> empty().orElseGet(() -> "World"));
    }

    @Test
    public void testOptional_orElseThrow() {
        assertEquals("Hello", Optional.of("Hello").orElseThrow());
        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow());
    }

    @Test
    public void testOptional_orElseThrow_withSupplier() {
        assertEquals("Hello", Optional.of("Hello").orElseThrow(IllegalStateException::new));
        assertThrows(IllegalStateException.class, () -> Optional.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptional_stream() {
        assertEquals(1, Optional.of("Hello").stream().count());
        assertEquals(0, Optional.empty().stream().count());

        Optional<String> opt = Optional.of("Hello");
        assertEquals("Hello", opt.stream().findFirst().get());
    }

    @Test
    public void testOptional_equals() {
        assertEquals(Optional.of("Hello"), Optional.of("Hello"));
        assertNotEquals(Optional.of("Hello"), Optional.of("World"));
        assertNotEquals(Optional.of("Hello"), Optional.empty());
        assertEquals(Optional.empty(), Optional.empty());
    }

    @Test
    public void testOptional_hashCode() {
        assertEquals(Optional.of("Hello").hashCode(), Optional.of("Hello").hashCode());
        assertEquals(Optional.empty().hashCode(), Optional.empty().hashCode());
    }

    @Test
    public void testOptional_toString() {
        assertTrue(Optional.of("Hello").toString().contains("Hello"));
        String emptyStr = Optional.empty().toString();
        assertTrue(emptyStr.contains("empty") || emptyStr.contains("Optional"));
    }

    @Test
    public void testNullable_empty() {
        Nullable<String> empty = Nullable.empty();
        assertFalse(empty.isPresent());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testNullable_of() {
        Nullable<String> opt = Nullable.of("Hello");
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        Nullable<String> nullOpt = Nullable.of(null);
        assertTrue(nullOpt.isPresent());
    }

    @Test
    public void testNullable_from() {
        Nullable<String> opt = Nullable.from(Optional.of("Hello"));
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        Nullable<String> empty = Nullable.from(Optional.empty());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testNullable_get() {
        assertEquals("Hello", Nullable.of("Hello").get());
        assertThrows(NoSuchElementException.class, () -> Nullable.empty().get());
    }

    @Test
    public void testNullable_isPresent() {
        assertTrue(Nullable.of("Hello").isPresent());
        assertFalse(Nullable.empty().isPresent());
        assertTrue(Nullable.of(null).isPresent());
    }

    @Test
    public void testNullable_isEmpty() {
        assertFalse(Nullable.of("Hello").isEmpty());
        assertTrue(Nullable.empty().isEmpty());
        assertFalse(Nullable.of(null).isEmpty());
    }

    @Test
    public void testNullable_isNull() {
        assertFalse(Nullable.of("Hello").isNull());
        assertTrue(Nullable.of(null).isNull());
        assertTrue(Nullable.empty().isNull());
    }

    @Test
    public void testNullable_isNotNull() {
        assertTrue(Nullable.of("Hello").isNotNull());
        assertFalse(Nullable.of(null).isNotNull());
        assertFalse(Nullable.empty().isNotNull());
    }

    @Test
    public void testNullable_ifPresent() {
        AtomicInteger called = new AtomicInteger(0);

        Nullable.of("Hello").ifPresent(v -> called.incrementAndGet());
        assertEquals(1, called.get());

        called.set(0);
        Nullable.empty().ifPresent(v -> called.incrementAndGet());
        assertEquals(0, called.get());
    }

    @Test
    public void testNullable_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        Nullable.of("Hello").ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testNullable_filter() {
        Nullable<String> opt = Nullable.of("Hello");
        assertTrue(opt.filter(v -> v.length() == 5).isPresent());
        assertFalse(opt.filter(v -> v.length() == 10).isPresent());
        assertFalse(Nullable.<String> empty().filter(v -> true).isPresent());
    }

    @Test
    public void testNullable_map() {
        Nullable<String> opt = Nullable.of("Hello");
        Nullable<Integer> mapped = opt.map(v -> v.length());
        assertTrue(mapped.isPresent());
        assertEquals(Integer.valueOf(5), mapped.get());

        assertFalse(Nullable.<String> empty().map(v -> v.length()).isPresent());
    }

    @Test
    public void testNullable_flatMap() {
        Nullable<String> opt = Nullable.of("Hello");
        Nullable<String> mapped = opt.flatMap(v -> Nullable.of(v.toUpperCase()));
        assertTrue(mapped.isPresent());
        assertEquals("HELLO", mapped.get());

        assertFalse(Nullable.<String> empty().flatMap(v -> Nullable.of(v)).isPresent());
    }

    @Test
    public void testNullable_or() {
        Nullable<String> opt = Nullable.of("Hello");
        assertSame(opt, opt.or(() -> Nullable.of("World")));

        Nullable<String> result = Nullable.<String> empty().or(() -> Nullable.of("World"));
        assertTrue(result.isPresent());
        assertEquals("World", result.get());
    }

    @Test
    public void testNullable_orElse() {
        assertEquals("Hello", Nullable.of("Hello").orElse("World"));
        assertEquals("World", Nullable.<String> empty().orElse("World"));
    }

    @Test
    public void testNullable_orElseGet() {
        assertEquals("Hello", Nullable.of("Hello").orElseGet(() -> "World"));
        assertEquals("World", Nullable.<String> empty().orElseGet(() -> "World"));
    }

    @Test
    public void testNullable_orElseNull() {
        assertEquals("Hello", Nullable.of("Hello").orElseNull());
        assertNull(Nullable.<String> empty().orElseNull());
    }

    @Test
    public void testNullable_orElseThrow() {
        assertEquals("Hello", Nullable.of("Hello").orElseThrow());
        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow());
    }

    @Test
    public void testNullable_orElseThrow_withSupplier() {
        assertEquals("Hello", Nullable.of("Hello").orElseThrow(IllegalStateException::new));
        assertThrows(IllegalStateException.class, () -> Nullable.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testNullable_stream() {
        assertEquals(1, Nullable.of("Hello").stream().count());
        assertEquals(0, Nullable.empty().stream().count());
    }

    @Test
    public void testNullable_toOptional() {
        Optional<String> opt = Nullable.of("Hello").toOptional();
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        Optional<String> empty = Nullable.<String> empty().toOptional();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testNullable_toJdkOptional() {
        java.util.Optional<String> opt = Nullable.of("Hello").toJdkOptional();
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        java.util.Optional<String> empty = Nullable.<String> empty().toJdkOptional();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testNullable_equals() {
        assertEquals(Nullable.of("Hello"), Nullable.of("Hello"));
        assertNotEquals(Nullable.of("Hello"), Nullable.of("World"));
        assertNotEquals(Nullable.of("Hello"), Nullable.empty());
        assertEquals(Nullable.empty(), Nullable.empty());
    }

    @Test
    public void testNullable_hashCode() {
        assertEquals(Nullable.of("Hello").hashCode(), Nullable.of("Hello").hashCode());
        assertEquals(Nullable.empty().hashCode(), Nullable.empty().hashCode());
    }

    @Test
    public void testNullable_toString() {
        assertTrue(Nullable.of("Hello").toString().contains("Hello"));
    }

    @Test
    public void testOptionalBoolean_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalBoolean_orElseThrow_withMessageAndParam() {
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Error: {0}", "param1"));
    }

    @Test
    public void testOptionalBoolean_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().orElseThrow("Error: {0}, {1}, {2}, {3}", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalBoolean_toList() {
        assertEquals(1, OptionalBoolean.of(true).toList().size());
        assertTrue(OptionalBoolean.of(true).toList().get(0));
        assertEquals(0, OptionalBoolean.empty().toList().size());
    }

    @Test
    public void testOptionalBoolean_toSet() {
        assertEquals(1, OptionalBoolean.of(true).toSet().size());
        assertTrue(OptionalBoolean.of(true).toSet().contains(Boolean.TRUE));
        assertEquals(0, OptionalBoolean.empty().toSet().size());
    }

    @Test
    public void testOptionalBoolean_toImmutableList() {
        assertEquals(1, OptionalBoolean.of(true).toImmutableList().size());
        assertEquals(0, OptionalBoolean.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalBoolean_toImmutableSet() {
        assertEquals(1, OptionalBoolean.of(true).toImmutableSet().size());
        assertEquals(0, OptionalBoolean.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalBoolean_boxed() {
        Optional<Boolean> boxed = OptionalBoolean.of(true).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Boolean.TRUE, boxed.get());

        assertFalse(OptionalBoolean.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalBoolean_getAsBoolean() {
        assertTrue(OptionalBoolean.of(true).getAsBoolean());
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().getAsBoolean());
    }

    @Test
    public void testOptionalChar_orElseZero() {
        assertEquals('A', OptionalChar.of('A').orElseZero());
        assertEquals('\0', OptionalChar.empty().orElseZero());
    }

    @Test
    public void testOptionalChar_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalChar_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalChar_toList() {
        assertEquals(1, OptionalChar.of('A').toList().size());
        assertEquals(Character.valueOf('A'), OptionalChar.of('A').toList().get(0));
        assertEquals(0, OptionalChar.empty().toList().size());
    }

    @Test
    public void testOptionalChar_toSet() {
        assertEquals(1, OptionalChar.of('A').toSet().size());
        assertEquals(0, OptionalChar.empty().toSet().size());
    }

    @Test
    public void testOptionalChar_toImmutableList() {
        assertEquals(1, OptionalChar.of('A').toImmutableList().size());
        assertEquals(0, OptionalChar.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalChar_toImmutableSet() {
        assertEquals(1, OptionalChar.of('A').toImmutableSet().size());
        assertEquals(0, OptionalChar.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalChar_boxed() {
        Optional<Character> boxed = OptionalChar.of('A').boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Character.valueOf('A'), boxed.get());

        assertFalse(OptionalChar.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalChar_getAsChar() {
        assertEquals('A', OptionalChar.of('A').getAsChar());
        assertThrows(NoSuchElementException.class, () -> OptionalChar.empty().getAsChar());
    }

    @Test
    public void testOptionalByte_orElseZero() {
        assertEquals((byte) 10, OptionalByte.of((byte) 10).orElseZero());
        assertEquals((byte) 0, OptionalByte.empty().orElseZero());
    }

    @Test
    public void testOptionalByte_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalByte_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalByte_toList() {
        assertEquals(1, OptionalByte.of((byte) 10).toList().size());
        assertEquals(Byte.valueOf((byte) 10), OptionalByte.of((byte) 10).toList().get(0));
        assertEquals(0, OptionalByte.empty().toList().size());
    }

    @Test
    public void testOptionalByte_toSet() {
        assertEquals(1, OptionalByte.of((byte) 10).toSet().size());
        assertEquals(0, OptionalByte.empty().toSet().size());
    }

    @Test
    public void testOptionalByte_toImmutableList() {
        assertEquals(1, OptionalByte.of((byte) 10).toImmutableList().size());
        assertEquals(0, OptionalByte.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalByte_toImmutableSet() {
        assertEquals(1, OptionalByte.of((byte) 10).toImmutableSet().size());
        assertEquals(0, OptionalByte.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalByte_boxed() {
        Optional<Byte> boxed = OptionalByte.of((byte) 10).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Byte.valueOf((byte) 10), boxed.get());

        assertFalse(OptionalByte.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalByte_getAsByte() {
        assertEquals((byte) 10, OptionalByte.of((byte) 10).getAsByte());
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().getAsByte());
    }

    @Test
    public void testOptionalShort_orElseZero() {
        assertEquals((short) 100, OptionalShort.of((short) 100).orElseZero());
        assertEquals((short) 0, OptionalShort.empty().orElseZero());
    }

    @Test
    public void testOptionalShort_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalShort_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalShort_toList() {
        assertEquals(1, OptionalShort.of((short) 100).toList().size());
        assertEquals(Short.valueOf((short) 100), OptionalShort.of((short) 100).toList().get(0));
        assertEquals(0, OptionalShort.empty().toList().size());
    }

    @Test
    public void testOptionalShort_toSet() {
        assertEquals(1, OptionalShort.of((short) 100).toSet().size());
        assertEquals(0, OptionalShort.empty().toSet().size());
    }

    @Test
    public void testOptionalShort_toImmutableList() {
        assertEquals(1, OptionalShort.of((short) 100).toImmutableList().size());
        assertEquals(0, OptionalShort.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalShort_toImmutableSet() {
        assertEquals(1, OptionalShort.of((short) 100).toImmutableSet().size());
        assertEquals(0, OptionalShort.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalShort_boxed() {
        Optional<Short> boxed = OptionalShort.of((short) 100).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Short.valueOf((short) 100), boxed.get());

        assertFalse(OptionalShort.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalShort_getAsShort() {
        assertEquals((short) 100, OptionalShort.of((short) 100).getAsShort());
        assertThrows(NoSuchElementException.class, () -> OptionalShort.empty().getAsShort());
    }

    @Test
    public void testOptionalShort_isEmpty() {
        assertFalse(OptionalShort.of((short) 100).isEmpty());
        assertTrue(OptionalShort.empty().isEmpty());
    }

    @Test
    public void testOptionalShort_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalShort.of((short) 100).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptionalShort_mapToObj() {
        OptionalShort opt = OptionalShort.of((short) 100);
        Optional<String> mapped = opt.mapToObj(v -> String.valueOf(v));
        assertTrue(mapped.isPresent());
        assertEquals("100", mapped.get());
    }

    @Test
    public void testOptionalShort_flatMap() {
        OptionalShort opt = OptionalShort.of((short) 100);
        OptionalShort mapped = opt.flatMap(v -> OptionalShort.of((short) (v * 2)));
        assertTrue(mapped.isPresent());
        assertEquals((short) 200, mapped.get());
    }

    @Test
    public void testOptionalShort_or() {
        OptionalShort opt = OptionalShort.of((short) 100);
        assertSame(opt, opt.or(() -> OptionalShort.of((short) 200)));

        OptionalShort result = OptionalShort.empty().or(() -> OptionalShort.of((short) 200));
        assertTrue(result.isPresent());
        assertEquals((short) 200, result.get());
    }

    @Test
    public void testOptionalShort_orElseGet() {
        assertEquals((short) 100, OptionalShort.of((short) 100).orElseGet(() -> (short) 200));
        assertEquals((short) 200, OptionalShort.empty().orElseGet(() -> (short) 200));
    }

    @Test
    public void testOptionalShort_orElseThrow_withSupplier() {
        assertEquals((short) 100, OptionalShort.of((short) 100).orElseThrow(IllegalStateException::new));
        assertThrows(IllegalStateException.class, () -> OptionalShort.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalInt_orElseZero() {
        assertEquals(42, OptionalInt.of(42).orElseZero());
        assertEquals(0, OptionalInt.empty().orElseZero());
    }

    @Test
    public void testOptionalInt_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalInt_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalInt_toList() {
        assertEquals(1, OptionalInt.of(42).toList().size());
        assertEquals(Integer.valueOf(42), OptionalInt.of(42).toList().get(0));
        assertEquals(0, OptionalInt.empty().toList().size());
    }

    @Test
    public void testOptionalInt_toSet() {
        assertEquals(1, OptionalInt.of(42).toSet().size());
        assertEquals(0, OptionalInt.empty().toSet().size());
    }

    @Test
    public void testOptionalInt_toImmutableList() {
        assertEquals(1, OptionalInt.of(42).toImmutableList().size());
        assertEquals(0, OptionalInt.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalInt_toImmutableSet() {
        assertEquals(1, OptionalInt.of(42).toImmutableSet().size());
        assertEquals(0, OptionalInt.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalInt_boxed() {
        Optional<Integer> boxed = OptionalInt.of(42).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Integer.valueOf(42), boxed.get());

        assertFalse(OptionalInt.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalInt_getAsInt() {
        assertEquals(42, OptionalInt.of(42).getAsInt());
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().getAsInt());
    }

    @Test
    public void testOptionalLong_orElseZero() {
        assertEquals(123L, OptionalLong.of(123L).orElseZero());
        assertEquals(0L, OptionalLong.empty().orElseZero());
    }

    @Test
    public void testOptionalLong_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalLong_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalLong_toList() {
        assertEquals(1, OptionalLong.of(123L).toList().size());
        assertEquals(Long.valueOf(123L), OptionalLong.of(123L).toList().get(0));
        assertEquals(0, OptionalLong.empty().toList().size());
    }

    @Test
    public void testOptionalLong_toSet() {
        assertEquals(1, OptionalLong.of(123L).toSet().size());
        assertEquals(0, OptionalLong.empty().toSet().size());
    }

    @Test
    public void testOptionalLong_toImmutableList() {
        assertEquals(1, OptionalLong.of(123L).toImmutableList().size());
        assertEquals(0, OptionalLong.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalLong_toImmutableSet() {
        assertEquals(1, OptionalLong.of(123L).toImmutableSet().size());
        assertEquals(0, OptionalLong.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalLong_boxed() {
        Optional<Long> boxed = OptionalLong.of(123L).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Long.valueOf(123L), boxed.get());

        assertFalse(OptionalLong.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalLong_getAsLong() {
        assertEquals(123L, OptionalLong.of(123L).getAsLong());
        assertThrows(NoSuchElementException.class, () -> OptionalLong.empty().getAsLong());
    }

    @Test
    public void testOptionalLong_ifPresentOrElse() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalLong.of(123L).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptionalLong_orElseThrow_withSupplier() {
        assertEquals(123L, OptionalLong.of(123L).orElseThrow(IllegalStateException::new));
        assertThrows(IllegalStateException.class, () -> OptionalLong.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalFloat_orElseZero() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).orElseZero(), 0.001f);
        assertEquals(0.0f, OptionalFloat.empty().orElseZero(), 0.001f);
    }

    @Test
    public void testOptionalFloat_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalFloat_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalFloat_toList() {
        assertEquals(1, OptionalFloat.of(3.14f).toList().size());
        assertEquals(Float.valueOf(3.14f), OptionalFloat.of(3.14f).toList().get(0));
        assertEquals(0, OptionalFloat.empty().toList().size());
    }

    @Test
    public void testOptionalFloat_toSet() {
        assertEquals(1, OptionalFloat.of(3.14f).toSet().size());
        assertEquals(0, OptionalFloat.empty().toSet().size());
    }

    @Test
    public void testOptionalFloat_toImmutableList() {
        assertEquals(1, OptionalFloat.of(3.14f).toImmutableList().size());
        assertEquals(0, OptionalFloat.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalFloat_toImmutableSet() {
        assertEquals(1, OptionalFloat.of(3.14f).toImmutableSet().size());
        assertEquals(0, OptionalFloat.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalFloat_boxed() {
        Optional<Float> boxed = OptionalFloat.of(3.14f).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Float.valueOf(3.14f), boxed.get());

        assertFalse(OptionalFloat.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalFloat_getAsFloat() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).getAsFloat(), 0.001f);
        assertThrows(NoSuchElementException.class, () -> OptionalFloat.empty().getAsFloat());
    }

    @Test
    public void testOptionalFloat_isEmpty() {
        assertFalse(OptionalFloat.of(3.14f).isEmpty());
        assertTrue(OptionalFloat.empty().isEmpty());
    }

    @Test
    public void testOptionalFloat_ifPresentOrElse_additional() {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        AtomicBoolean emptyActionCalled = new AtomicBoolean(false);

        OptionalFloat.of(3.14f).ifPresentOrElse(v -> actionCalled.set(true), () -> emptyActionCalled.set(true));
        assertTrue(actionCalled.get());
        assertFalse(emptyActionCalled.get());
    }

    @Test
    public void testOptionalFloat_mapToInt() {
        OptionalFloat opt = OptionalFloat.of(3.14f);
        OptionalInt mapped = opt.mapToInt(v -> v.intValue());
        assertTrue(mapped.isPresent());
        assertEquals(3, mapped.get());
    }

    @Test
    public void testOptionalFloat_orElseThrow_withSupplier() {
        assertEquals(3.14f, OptionalFloat.of(3.14f).orElseThrow(IllegalStateException::new), 0.001f);
        assertThrows(IllegalStateException.class, () -> OptionalFloat.empty().orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testOptionalDouble_orElseZero() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).orElseZero(), 0.00001);
        assertEquals(0.0, OptionalDouble.empty().orElseZero(), 0.00001);
    }

    @Test
    public void testOptionalDouble_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptionalDouble_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptionalDouble_toList() {
        assertEquals(1, OptionalDouble.of(3.14159).toList().size());
        assertEquals(Double.valueOf(3.14159), OptionalDouble.of(3.14159).toList().get(0));
        assertEquals(0, OptionalDouble.empty().toList().size());
    }

    @Test
    public void testOptionalDouble_toSet() {
        assertEquals(1, OptionalDouble.of(3.14159).toSet().size());
        assertEquals(0, OptionalDouble.empty().toSet().size());
    }

    @Test
    public void testOptionalDouble_toImmutableList() {
        assertEquals(1, OptionalDouble.of(3.14159).toImmutableList().size());
        assertEquals(0, OptionalDouble.empty().toImmutableList().size());
    }

    @Test
    public void testOptionalDouble_toImmutableSet() {
        assertEquals(1, OptionalDouble.of(3.14159).toImmutableSet().size());
        assertEquals(0, OptionalDouble.empty().toImmutableSet().size());
    }

    @Test
    public void testOptionalDouble_boxed() {
        Optional<Double> boxed = OptionalDouble.of(3.14159).boxed();
        assertTrue(boxed.isPresent());
        assertEquals(Double.valueOf(3.14159), boxed.get());

        assertFalse(OptionalDouble.empty().boxed().isPresent());
    }

    @Test
    public void testOptionalDouble_getAsDouble() {
        assertEquals(3.14159, OptionalDouble.of(3.14159).getAsDouble(), 0.00001);
        assertThrows(NoSuchElementException.class, () -> OptionalDouble.empty().getAsDouble());
    }

    @Test
    public void testOptional_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testOptional_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> Optional.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testOptional_toList() {
        assertEquals(1, Optional.of("Hello").toList().size());
        assertEquals("Hello", Optional.of("Hello").toList().get(0));
        assertEquals(0, Optional.empty().toList().size());
    }

    @Test
    public void testOptional_toSet() {
        assertEquals(1, Optional.of("Hello").toSet().size());
        assertEquals(0, Optional.empty().toSet().size());
    }

    @Test
    public void testOptional_toImmutableList() {
        assertEquals(1, Optional.of("Hello").toImmutableList().size());
        assertEquals(0, Optional.empty().toImmutableList().size());
    }

    @Test
    public void testOptional_toImmutableSet() {
        assertEquals(1, Optional.of("Hello").toImmutableSet().size());
        assertEquals(0, Optional.empty().toImmutableSet().size());
    }

    @Test
    public void testOptional_toJdkOptional() {
        java.util.Optional<String> jdkOpt = Optional.of("Hello").toJdkOptional();
        assertTrue(jdkOpt.isPresent());
        assertEquals("Hello", jdkOpt.get());

        java.util.Optional<String> empty = Optional.<String> empty().toJdkOptional();
        assertFalse(empty.isPresent());
    }

    @Test
    public void testOptional_from() {
        Optional<String> opt = Optional.from(java.util.Optional.of("Hello"));
        assertTrue(opt.isPresent());
        assertEquals("Hello", opt.get());

        Optional<String> empty = Optional.from(java.util.Optional.empty());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testNullable_orElseThrow_withMessage() {
        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Custom error message"));
    }

    @Test
    public void testNullable_orElseThrow_withMessageAndParams() {
        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Error: {0}", "param1"));

        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Error: {0}, {1}", "param1", "param2"));

        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Error: {0}, {1}, {2}", "p1", "p2", "p3"));

        assertThrows(NoSuchElementException.class, () -> Nullable.empty().orElseThrow("Error with params", "p1", "p2", "p3", "p4"));
    }

    @Test
    public void testNullable_toList() {
        assertEquals(1, Nullable.of("Hello").toList().size());
        assertEquals("Hello", Nullable.of("Hello").toList().get(0));
        assertEquals(0, Nullable.empty().toList().size());
    }

    @Test
    public void testNullable_toSet() {
        assertEquals(1, Nullable.of("Hello").toSet().size());
        assertEquals(0, Nullable.empty().toSet().size());
    }

    @Test
    public void testNullable_toImmutableList() {
        assertEquals(1, Nullable.of("Hello").toImmutableList().size());
        assertEquals(0, Nullable.empty().toImmutableList().size());
    }

    @Test
    public void testNullable_toImmutableSet() {
        assertEquals(1, Nullable.of("Hello").toImmutableSet().size());
        assertEquals(0, Nullable.empty().toImmutableSet().size());
    }
}
