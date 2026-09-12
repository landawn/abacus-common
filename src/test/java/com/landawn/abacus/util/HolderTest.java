package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

public class HolderTest extends TestBase {

    @Test
    public void testOfAndValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("test", holder.value());
        assertEquals("test", holder.getValue());
        assertFalse(holder.isNull());
        assertTrue(holder.isNotNull());

        holder = Holder.of(null);
        assertNull(holder.value());
        assertTrue(holder.isNull());
        assertFalse(holder.isNotNull());

        Holder<String> empty = new Holder<>();
        assertTrue(empty.isNull());
        empty.setValue("value");
        assertEquals("value", empty.value());
        empty.setValue(null);
        assertTrue(empty.isNull());
    }

    @Test
    public void testGetAndSet() {
        Holder<String> holder = Holder.of("old");
        assertEquals("old", holder.getAndSet("new"));
        assertEquals("new", holder.value());
        assertEquals("next", holder.setAndGet("next"));
        assertEquals("next", holder.value());
        assertEquals("next", holder.getAndSet(null));
        assertNull(holder.value());
    }

    @Test
    public void testUpdate() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        assertEquals(10, holder.getAndUpdate(n -> n + 1));
        assertEquals(11, holder.value());
        assertEquals(22, holder.updateAndGet(n -> n * 2));
        assertEquals(22, holder.value());
        assertNull(Holder.of((Integer) null).getAndUpdate(n -> n == null ? 0 : n));
        assertThrows(IllegalArgumentException.class, () -> Holder.of(1).getAndUpdate(null));
        assertThrows(IllegalArgumentException.class, () -> Holder.of(1).updateAndGet(null));
    }

    @Test
    public void testSetIf() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        assertTrue(holder.setIf(n -> n != null && n > 5, 20));
        assertEquals(20, holder.value());
        assertFalse(holder.setIf(n -> n != null && n < 5, 1));
        assertEquals(20, holder.value());
        Holder<Integer> empty = Holder.of(null);
        assertTrue(empty.setIf(n -> n == null, 5));
        assertEquals(5, empty.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of(1).setIf(null, 2));
    }

    @Test
    public void testIfNotNull() throws Exception {
        Holder<Integer> seen = new Holder<>();
        Holder.of(10).ifNotNull(v -> seen.setValue(v * 2));
        assertEquals(20, seen.value());
        Holder.of((Integer) null).ifNotNull(v -> seen.setValue(-1));
        assertEquals(20, seen.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").ifNotNull(null));

        Holder<String> branch = new Holder<>();
        Holder.of(10).ifNotNullOrElse(v -> branch.setValue("value: " + v), () -> branch.setValue("empty"));
        assertEquals("value: 10", branch.value());
        Holder.of((Integer) null).ifNotNullOrElse(v -> branch.setValue("value"), () -> branch.setValue("empty"));
        assertEquals("empty", branch.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").ifNotNullOrElse(null, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").ifNotNullOrElse(v -> {
        }, null));
    }

    @Test
    public void testAcceptAndMap() throws Exception {
        Holder<String> captured = new Holder<>();
        Holder.of("test").accept(captured::setValue);
        assertEquals("test", captured.value());
        Holder.of((String) null).accept(captured::setValue);
        assertNull(captured.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").accept(null));

        Holder.of(10).acceptIfNotNull(v -> captured.setValue(String.valueOf(v * 2)));
        assertEquals("20", captured.value());
        Holder.of((Integer) null).acceptIfNotNull(v -> captured.setValue("no"));
        assertEquals("20", captured.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").acceptIfNotNull(null));

        assertEquals(Integer.valueOf(4), Holder.of("test").map(String::length));
        assertEquals(Integer.valueOf(0), Holder.of((String) null).map(s -> s == null ? 0 : s.length()));
        assertNull(Holder.of("test").map(s -> null));

        Nullable<Integer> present = Holder.of("test").mapIfNotNull(String::length);
        assertEquals(4, present.get());
        assertFalse(Holder.of((String) null).mapIfNotNull(String::length).isPresent());
        assertTrue(Holder.of("test").mapIfNotNull(v -> null).isPresent());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").mapIfNotNull(null));

        assertEquals(4, Holder.of("test").mapToNonNullIfNotNull(String::length).get());
        assertFalse(Holder.of((String) null).mapToNonNullIfNotNull(String::length).isPresent());
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").map(null));
        assertThrows(IllegalArgumentException.class, () -> Holder.of("x").mapToNonNullIfNotNull(null));
    }

    @Test
    public void testFilter() throws Exception {
        assertEquals(10, Holder.of(10).filter(v -> v > 5).get());
        assertFalse(Holder.of(3).filter(v -> v > 5).isPresent());
        assertTrue(Holder.of((Integer) null).filter(v -> v == null).isPresent());
        assertFalse(Holder.of((String) null).filter(v -> v != null).isPresent());

        Holder<Integer> holder = Holder.of(10);
        Nullable<Integer> snapshot = holder.filter(v -> {
            holder.setValue(null);
            return true;
        });
        assertEquals(10, snapshot.get());
        assertNull(holder.value());

        assertEquals(10, Holder.of(10).filterIfNotNull(n -> n > 5).get());
        assertFalse(Holder.of(3).filterIfNotNull(n -> n > 5).isPresent());
        assertFalse(Holder.of((Integer) null).filterIfNotNull(n -> true).isPresent());
        assertThrows(IllegalArgumentException.class, () -> Holder.of(10).filterIfNotNull(null));

        Holder<Integer> mutated = Holder.of(10);
        Optional<Integer> kept = mutated.filterIfNotNull(v -> {
            mutated.setValue(null);
            return true;
        });
        assertEquals(10, kept.get());
        assertNull(mutated.value());
        assertThrows(IllegalArgumentException.class, () -> Holder.of(10).filter(null));
    }

    @Test
    public void testOrElseIfNull() {
        assertEquals("test", Holder.of("test").orElseIfNull("default"));
        assertEquals("default", Holder.of((String) null).orElseIfNull("default"));
        assertNull(Holder.of((String) null).orElseIfNull(null));
        assertEquals("test", Holder.of("test").orElseGetIfNull(() -> "default"));
        assertEquals("default", Holder.of((String) null).orElseGetIfNull(() -> "default"));
        assertNull(Holder.of((String) null).orElseGetIfNull(() -> null));
        assertThrows(IllegalArgumentException.class, () -> Holder.of((String) null).orElseGetIfNull(null));
    }

    @Test
    public void testOrElseThrowIfNull() throws Exception {
        assertEquals("test", Holder.of("test").orElseThrowIfNull());
        assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull());
        assertEquals("Custom error", assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull("Custom error")).getMessage());
        assertTrue(assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull("Error: %s", "param1")).getMessage()
                .contains("param1"));
        assertTrue(assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull("Error: %s %s", "p1", "p2")).getMessage()
                .contains("p2"));
        assertTrue(
                assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3")).getMessage()
                        .contains("p3"));
        assertTrue(assertThrows(NoSuchElementException.class, () -> Holder.of((String) null).orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"))
                .getMessage()
                .contains("p4"));
        assertEquals("test", Holder.of("test").orElseThrowIfNull(() -> new IllegalStateException("Error")));
        assertThrows(IllegalStateException.class, () -> Holder.of((String) null).orElseThrowIfNull(() -> new IllegalStateException("Error")));
        assertThrows(IllegalArgumentException.class, () -> Holder.of((String) null).orElseThrowIfNull((Supplier<Exception>) null));
    }

    @Test
    public void testEqualsHashCodeToString() {
        Holder<String> a = Holder.of("test");
        Holder<String> b = Holder.of("test");
        Holder<String> c = Holder.of("other");
        Holder<String> n1 = Holder.of(null);
        Holder<String> n2 = Holder.of(null);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(n1, n2);
        assertNotEquals(a, n1);
        assertNotEquals(a, "test");
        assertNotEquals(a, null);
        assertEquals("test".hashCode(), a.hashCode());
        assertEquals(0, n1.hashCode());
        assertEquals("Holder[test]", a.toString());
        assertEquals("Holder[null]", n1.toString());
        assertTrue(Holder.of(new Object() {
            @Override
            public String toString() {
                return "CustomObject";
            }
        }).toString().contains("CustomObject"));
    }

    @Test
    public void testComplexChaining() {
        Optional<Integer> result = Holder.of("test").mapIfNotNull(String::toUpperCase).mapIfNotNull(s -> s + "123").mapToNonNullIfNotNull(String::length);
        assertEquals(7, result.get());
        assertFalse(Holder.of((String) null).mapIfNotNull(String::toUpperCase).mapToNonNullIfNotNull(String::length).isPresent());
    }
}
