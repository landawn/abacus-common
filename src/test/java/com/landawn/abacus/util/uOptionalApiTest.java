package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class uOptionalApiTest extends uTestSupport {

    @Test
    public void testOptionalBoolean() throws Exception {
        assertTrue(OptionalBoolean.of(true).getAsBoolean());
        assertFalse(OptionalBoolean.of(false).getAsBoolean());
        assertThrows(NoSuchElementException.class, () -> OptionalBoolean.empty().getAsBoolean());
        assertFalse(OptionalBoolean.of(true).map(v -> !v).get());
        assertFalse(OptionalBoolean.empty().map(v -> !v).isPresent());
        assertTrue(OptionalBoolean.of(true).orElseFalse());
        assertFalse(OptionalBoolean.of(false).orElseFalse());
        assertFalse(OptionalBoolean.empty().orElseFalse());
        assertTrue(OptionalBoolean.of(true).orElseTrue());
        assertFalse(OptionalBoolean.of(false).orElseTrue());
        assertTrue(OptionalBoolean.empty().orElseTrue());
        assertEquals("OptionalBoolean[true]", OptionalBoolean.of(true).toString());
        assertEquals("OptionalBoolean[false]", OptionalBoolean.of(false).toString());
        assertEquals("OptionalBoolean.empty", OptionalBoolean.empty().toString());
    }

    @Test
    public void testOptionalChar() throws Exception {
        OptionalChar present = OptionalChar.of('A');
        assertTrue(present.isPresent());
        assertFalse(present.isEmpty());
        assertEquals('A', present.get());
        assertEquals('A', present.getAsChar());
        assertFalse(OptionalChar.ofNullable(null).isPresent());
        assertEquals('X', OptionalChar.ofNullable('X').get());
        char[] seen = { 0 };
        present.ifPresent(v -> seen[0] = v);
        assertEquals('A', seen[0]);
        boolean[] emptyCalled = { false };
        OptionalChar.empty().ifPresentOrElse(v -> seen[0] = 'X', () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
        assertTrue(present.filter(v -> v == 'A').isPresent());
        assertFalse(present.filter(v -> v == 'Z').isPresent());
        assertEquals('B', present.map(v -> (char) (v + 1)).get());
        assertEquals(65, present.mapToInt(v -> (int) v).get());
        assertFalse(present.flatMap(v -> OptionalChar.empty()).isPresent());
        assertEquals('A', present.or(() -> OptionalChar.of('Z')).get());
        assertEquals('Z', OptionalChar.empty().or(() -> OptionalChar.of('Z')).get());
        assertEquals('A', present.orElse('Z'));
        assertEquals('Z', OptionalChar.empty().orElseGet(() -> 'Z'));
        assertEquals('A', present.orElseThrow());
        assertThrows(IllegalStateException.class, () -> OptionalChar.empty().orElseThrow(IllegalStateException::new));
        assertEquals(1, present.stream().count());
        assertEquals('A', present.toList().get(0));
        assertTrue(present.toSet().contains('A'));
        assertEquals('A', present.toImmutableList().get(0));
        assertTrue(present.toImmutableSet().contains('A'));
        assertEquals(Optional.of('A'), present.boxed());
        assertTrue(present.compareTo(OptionalChar.empty()) > 0);
        assertEquals(present, OptionalChar.of('A'));
        assertEquals("OptionalChar[A]", present.toString());
        assertEquals("OptionalChar.empty", OptionalChar.empty().toString());
    }

    @Test
    public void testOptionalByte() throws Exception {
        OptionalByte present = OptionalByte.of((byte) 10);
        assertEquals((byte) 10, present.get());
        assertFalse(OptionalByte.ofNullable(null).isPresent());
        byte[] seen = { 0 };
        present.ifPresent(v -> seen[0] = v);
        assertEquals((byte) 10, seen[0]);
        boolean[] emptyCalled = { false };
        OptionalByte.empty().ifPresentOrElse(v -> seen[0] = 99, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
        assertTrue(present.filter(v -> v > 5).isPresent());
        assertEquals((byte) 20, present.map(v -> (byte) (v * 2)).get());
        assertEquals(100, present.mapToInt(v -> v * 10).get());
        assertEquals("10", present.mapToObj(String::valueOf).get());
        assertEquals((byte) 11, present.flatMap(v -> OptionalByte.of((byte) (v + 1))).get());
        assertEquals((byte) 10, present.or(() -> OptionalByte.of((byte) 20)).get());
        assertEquals((byte) 20, OptionalByte.empty().or(() -> OptionalByte.of((byte) 20)).get());
        assertEquals((byte) 10, present.orElseZero());
        assertEquals((byte) 0, OptionalByte.empty().orElseZero());
        assertEquals((byte) 20, OptionalByte.empty().orElse((byte) 20));
        assertEquals((byte) 10, present.orElseThrow());
        assertThrows(NoSuchElementException.class, () -> OptionalByte.empty().orElseThrow());
        assertThrows(IllegalStateException.class, () -> OptionalByte.empty().orElseThrow(IllegalStateException::new));
        assertEquals(1, present.stream().count());
        assertEquals((byte) 10, present.toList().get(0));
        assertTrue(present.toSet().contains((byte) 10));
        assertEquals((byte) 10, present.toImmutableList().get(0));
        assertEquals(Optional.of((byte) 10), present.boxed());
        assertTrue(present.compareTo(OptionalByte.of((byte) 20)) < 0);
        assertEquals(present, OptionalByte.of((byte) 10));
        assertEquals("OptionalByte[10]", present.toString());
    }

    @Test
    public void testOptionalShort() throws Exception {
        OptionalShort present = OptionalShort.of((short) 100);
        assertEquals((short) 100, present.get());
        assertFalse(OptionalShort.ofNullable(null).isPresent());
        short[] seen = { 0 };
        present.ifPresent(v -> seen[0] = v);
        assertEquals((short) 100, seen[0]);
        boolean[] emptyCalled = { false };
        OptionalShort.empty().ifPresentOrElse(v -> seen[0] = 1, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
        assertTrue(present.filter(v -> v > 50).isPresent());
        assertEquals((short) 200, present.map(v -> (short) (v * 2)).get());
        assertEquals(100, present.mapToInt(v -> v.intValue()).get());
        assertEquals("100", present.mapToObj(String::valueOf).get());
        assertEquals((short) 101, present.flatMap(v -> OptionalShort.of((short) (v + 1))).get());
        assertEquals((short) 100, present.orElseZero());
        assertEquals((short) 9, OptionalShort.empty().orElse((short) 9));
        assertEquals((short) 100, present.orElseThrow());
        assertEquals(1, present.stream().count());
        assertTrue(present.toSet().contains((short) 100));
        assertEquals(Optional.of((short) 100), present.boxed());
        assertEquals(present, OptionalShort.of((short) 100));
        assertEquals("OptionalShort[100]", present.toString());
        assertEquals("OptionalShort.empty", OptionalShort.empty().toString());
    }

    @Test
    public void testOptionalInt() throws Exception {
        OptionalInt present = OptionalInt.of(42);
        assertEquals(42, present.get());
        assertFalse(OptionalInt.ofNullable(null).isPresent());
        int[] seen = { 0 };
        present.ifPresent(v -> seen[0] = v);
        assertEquals(42, seen[0]);
        boolean[] emptyCalled = { false };
        OptionalInt.empty().ifPresentOrElse(v -> seen[0] = 1, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
        assertTrue(present.mapToBoolean(v -> v > 5).get());
        assertEquals('A', OptionalInt.of(65).mapToChar(v -> (char) v.intValue()).get());
        assertEquals(21.0, present.mapToDouble(v -> v / 2.0).get());
        assertEquals(84, present.flatMap(v -> OptionalInt.of(v * 2)).get());
        assertEquals(42, present.or(() -> OptionalInt.of(100)).get());
        assertEquals(100, OptionalInt.empty().or(() -> OptionalInt.of(100)).get());
        assertEquals(42, present.orElseZero());
        assertEquals(100, OptionalInt.empty().orElse(100));
        assertEquals(100, OptionalInt.empty().orElseGet(() -> 100));
        assertEquals(42, present.orElseThrow());
        assertEquals(42, present.orElseThrow("No value"));
        assertEquals(42, present.orElseThrow("Error: %s", "arg1"));
        assertEquals(42, present.orElseThrow("Error: %s %s", "a", "b"));
        assertEquals(42, present.orElseThrow("Error: %s %s %s", "a", "b", "c"));
        assertEquals(42, present.orElseThrow("Error: %s", new Object[] { "arg" }));
        assertThrows(NoSuchElementException.class, () -> OptionalInt.empty().orElseThrow("Custom error"));
        assertThrows(IllegalStateException.class, () -> OptionalInt.empty().orElseThrow(() -> new IllegalStateException("not found")));
        assertEquals(1, present.stream().count());
        assertEquals(42, present.toList().get(0));
        assertTrue(present.toSet().contains(42));
        assertEquals(42, present.toImmutableList().get(0));
        assertTrue(present.toImmutableSet().contains(42));
        assertEquals(Optional.of(42), present.boxed());
        assertEquals(42, present.toJdkOptional().getAsInt());
        assertTrue(present.compareTo(OptionalInt.of(20)) > 0);
        assertEquals(present, OptionalInt.of(42));
        assertEquals("OptionalInt[42]", present.toString());
    }

    @Test
    public void testOptionalLong() throws Exception {
        OptionalLong present = OptionalLong.of(42L);
        assertEquals(42L, present.get());
        assertFalse(OptionalLong.ofNullable(null).isPresent());
        assertEquals(42L, OptionalLong.from(java.util.OptionalLong.of(42L)).get());
        assertFalse(OptionalLong.from(java.util.OptionalLong.empty()).isPresent());
        long[] seen = { 0 };
        present.ifPresent(v -> seen[0] = v);
        assertEquals(42L, seen[0]);
        boolean[] emptyCalled = { false };
        OptionalLong.empty().ifPresentOrElse(v -> seen[0] = 1, () -> emptyCalled[0] = true);
        assertTrue(emptyCalled[0]);
        assertTrue(present.filter(v -> v > 5).isPresent());
        assertEquals(84L, present.map(v -> v * 2).get());
        assertEquals(42, present.mapToInt(v -> v.intValue()).get());
        assertEquals(42.0, present.mapToDouble(v -> v.doubleValue()).get());
        assertEquals("42", present.mapToObj(String::valueOf).get());
        assertEquals(43L, present.flatMap(v -> OptionalLong.of(v + 1)).get());
        assertEquals(42L, present.or(() -> OptionalLong.of(1L)).get());
        assertEquals(42L, present.orElseZero());
        assertEquals(100L, OptionalLong.empty().orElseGet(() -> 100L));
        assertEquals(42L, present.orElseThrow());
        assertEquals(1, present.stream().count());
        assertEquals(42L, present.toList().get(0));
        assertTrue(present.toImmutableSet().contains(42L));
        assertEquals(Optional.of(42L), present.boxed());
        assertEquals(42L, present.toJdkOptional().getAsLong());
        assertEquals(present, OptionalLong.of(42L));
        assertEquals("OptionalLong[42]", present.toString());
    }

    @Test
    public void testOptionalFloat() throws Exception {
        OptionalFloat present = OptionalFloat.of(42.5f);
        assertEquals(42.5f, present.getAsFloat());
        assertFalse(OptionalFloat.ofNullable(null).isPresent());
        assertTrue(present.filter(v -> v > 1).isPresent());
        assertEquals(85.0f, present.map(v -> v * 2).get());
        assertEquals(42, present.mapToInt(v -> v.intValue()).get());
        assertEquals(42.5, present.mapToDouble(v -> v.doubleValue()).get(), 0.001);
        assertEquals("42.5", present.mapToObj(String::valueOf).get());
        assertFalse(present.flatMap(v -> OptionalFloat.empty()).isPresent());
        assertEquals(42.5f, present.or(() -> OptionalFloat.of(1f)).get());
        assertEquals(42.5f, present.orElseThrow());
        assertEquals(1, present.stream().count());
        assertEquals(Optional.of(42.5f), present.boxed());
        assertEquals(42.5f, present.toImmutableList().get(0));
        assertTrue(present.toImmutableSet().contains(42.5f));
        assertEquals(present, OptionalFloat.of(42.5f));
        assertEquals("OptionalFloat[42.5]", present.toString());

        assertSame(OptionalFloat.of(0.0f), OptionalFloat.of(0.0f));
        assertNotSame(OptionalFloat.of(-0.0f), OptionalFloat.of(-0.0f));
        assertNotEquals(OptionalFloat.of(-0.0f), OptionalFloat.of(0.0f));
        assertNotEquals(OptionalFloat.of(-0.0f).hashCode(), OptionalFloat.of(0.0f).hashCode());
        assertTrue(OptionalFloat.of(-0.0f).compareTo(OptionalFloat.of(0.0f)) < 0);
        assertEquals(OptionalFloat.of(Float.NaN), OptionalFloat.of(Float.NaN));
        assertEquals(0, OptionalFloat.of(Float.NaN).compareTo(OptionalFloat.of(Float.NaN)));
    }

    @Test
    public void testOptionalDouble() throws Exception {
        OptionalDouble present = OptionalDouble.of(42.5);
        assertEquals(42.5, present.get());
        assertFalse(OptionalDouble.ofNullable(null).isPresent());
        assertEquals(42.5, OptionalDouble.from(java.util.OptionalDouble.of(42.5)).get());
        assertTrue(present.filter(v -> v > 1).isPresent());
        assertEquals(85.0, present.map(v -> v * 2).get());
        assertEquals(42, present.mapToInt(v -> v.intValue()).get());
        assertEquals(42L, present.mapToLong(v -> v.longValue()).get());
        assertEquals("42.5", present.mapToObj(String::valueOf).get());
        assertFalse(present.flatMap(v -> OptionalDouble.empty()).isPresent());
        assertEquals(42.5, present.or(() -> OptionalDouble.of(1d)).get());
        assertEquals(0.0, OptionalDouble.empty().orElseZero());
        assertEquals(42.5, present.orElseThrow());
        assertEquals(1, present.stream().count());
        assertEquals(Optional.of(42.5), present.boxed());
        assertEquals(42.5, present.toJdkOptional().getAsDouble());
        assertEquals(42.5, present.toImmutableList().get(0));
        assertTrue(present.toImmutableSet().contains(42.5));
        assertEquals(present, OptionalDouble.of(42.5));
        assertEquals("OptionalDouble[42.5]", present.toString());

        assertSame(OptionalDouble.of(0.0d), OptionalDouble.of(0.0d));
        assertNotSame(OptionalDouble.of(-0.0d), OptionalDouble.of(-0.0d));
        assertNotEquals(OptionalDouble.of(-0.0d), OptionalDouble.of(0.0d));
        assertTrue(OptionalDouble.of(-0.0d).compareTo(OptionalDouble.of(0.0d)) < 0);
        assertEquals(OptionalDouble.of(Double.NaN), OptionalDouble.of(Double.NaN));
        assertEquals(0, OptionalDouble.of(Double.NaN).compareTo(OptionalDouble.of(Double.NaN)));
    }

    /**
     * Pins the "Checked exceptions" row of the JDK-comparison tables in the {@code OptionalInt},
     * {@code OptionalLong} and {@code OptionalDouble} class javadocs, and the matching bullet in
     * {@code OptionalFloat}: only the {@code Throwables.*} positions let a lambda body throw a checked
     * exception, while {@code or} / {@code orElseGet} / {@code orElseThrow(Supplier)} take plain
     * suppliers - though {@code orElseThrow(Supplier)} still throws the checked exception its supplier
     * RETURNS.
     *
     * <p>Section (1) below samples only {@code OptionalInt}; every enumerated position on all four types is
     * covered by {@link #testCheckedExceptionDocContract_everyEnumeratedPositionOnAllFourTypes()}.
     */
    @Test
    public void testCheckedExceptionDocContract() throws Exception {
        // (1) The Throwables.* positions really do let the lambda body throw a checked exception.
        assertThrows(IOException.class, () -> OptionalInt.of(1).map(v -> {
            throw new IOException("map");
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).filter(v -> {
            throw new IOException("filter");
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).ifPresent(v -> {
            throw new IOException("ifPresent");
        }));

        // (2) or / orElseGet / orElseThrow(Supplier) take the standard java.util.function suppliers,
        //     whose bodies cannot throw a checked exception (a compile-time fact, pinned here as the
        //     declared parameter types the javadoc names). getDeclaredMethod(name, X.class) resolves BY X,
        //     so asserting its parameter type is X would be a tautology: enumerate the declared overloads
        //     instead, which also proves there is no SECOND one-arg overload taking something else.
        assertEquals(List.of(Supplier.class), oneArgParamTypes(OptionalInt.class, "or"));
        assertEquals(List.of(Supplier.class), oneArgParamTypes(OptionalLong.class, "or"));
        assertEquals(List.of(Supplier.class), oneArgParamTypes(OptionalFloat.class, "or"));
        assertEquals(List.of(Supplier.class), oneArgParamTypes(OptionalDouble.class, "or"));
        assertEquals(List.of(IntSupplier.class), oneArgParamTypes(OptionalInt.class, "orElseGet"));
        assertEquals(List.of(LongSupplier.class), oneArgParamTypes(OptionalLong.class, "orElseGet"));
        assertEquals(List.of(DoubleSupplier.class), oneArgParamTypes(OptionalDouble.class, "orElseGet"));
        // orElseThrow(String) is the other one-arg overload, so it is part of the expected set
        for (final Class<?> cls : new Class<?>[] { OptionalInt.class, OptionalLong.class, OptionalFloat.class, OptionalDouble.class }) {
            assertEquals(List.of(String.class, Supplier.class), oneArgParamTypes(cls, "orElseThrow"), cls.getName());
        }

        // (3) OptionalFloat.orElseGet is the exception the table text must not flatten: it takes abacus's
        //     own FloatSupplier, which is a Throwables.FloatSupplier pinned to RuntimeException.
        //     isAssignableFrom is erasure-only and would still pass for <Exception>, so read the actual
        //     type argument off the generic interface - that is where the RuntimeException pin lives.
        assertEquals(List.of(com.landawn.abacus.util.function.FloatSupplier.class), oneArgParamTypes(OptionalFloat.class, "orElseGet"));
        final java.lang.reflect.Type[] floatSupplierInterfaces = com.landawn.abacus.util.function.FloatSupplier.class.getGenericInterfaces();
        assertEquals(1, floatSupplierInterfaces.length);
        final java.lang.reflect.ParameterizedType floatSupplierBound = (java.lang.reflect.ParameterizedType) floatSupplierInterfaces[0];
        assertSame(Throwables.FloatSupplier.class, floatSupplierBound.getRawType());
        assertArrayEquals(new java.lang.reflect.Type[] { RuntimeException.class }, floatSupplierBound.getActualTypeArguments());

        // (4) orElseThrow(Supplier) DOES throw a checked exception - the one its supplier returns.
        assertThrows(IOException.class, () -> OptionalInt.empty().orElseThrow(() -> new IOException("boom")));
        assertThrows(IOException.class, () -> OptionalFloat.empty().orElseThrow(() -> new IOException("boom")));
        assertThrows(IOException.class, () -> OptionalLong.empty().orElseThrow(() -> new IOException("boom")));
        assertThrows(IOException.class, () -> OptionalDouble.empty().orElseThrow(() -> new IOException("boom")));
    }

    /** The one-argument declared overloads of {@code name} on {@code cls}, by parameter type, name-sorted. */
    private static List<Class<?>> oneArgParamTypes(final Class<?> cls, final String name) {
        return java.util.Arrays.stream(cls.getDeclaredMethods())
                .filter(m -> m.getName().equals(name) && m.getParameterCount() == 1)
                .map(m -> m.getParameterTypes()[0])
                .sorted(java.util.Comparator.comparing(Class::getName))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Completes the coverage of the "Checked exceptions" doc row: {@link #testCheckedExceptionDocContract()}
     * executes only three of the enumerated positions, and only on {@code OptionalInt}. This runs EVERY
     * position the four class javadocs enumerate - {@code filter}, {@code map}, each {@code mapToXxx},
     * {@code mapToObj}, {@code flatMap}, {@code ifPresent} and both arms of {@code ifPresentOrElse} - on all
     * four types, so retyping any one of them to a {@code java.util.function} interface breaks this test
     * instead of silently falsifying the javadoc.
     */
    @Test
    public void testCheckedExceptionDocContract_everyEnumeratedPositionOnAllFourTypes() throws Exception {
        final IOException boom = new IOException("boom");

        // --- OptionalInt: filter, map, mapToBoolean, mapToChar, mapToLong, mapToFloat, mapToDouble, mapToObj, flatMap, ifPresent, both arms of ifPresentOrElse
        assertThrows(IOException.class, () -> OptionalInt.of(1).filter(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).map(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToBoolean(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToChar(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToLong(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToFloat(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToDouble(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).mapToObj(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).flatMap(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).ifPresent(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalInt.of(1).ifPresentOrElse(v -> {
            throw boom;
        }, () -> {
            // the empty arm is not reached when the value is present
        }));
        assertThrows(IOException.class, () -> OptionalInt.empty().ifPresentOrElse(v -> {
            // the present arm is not reached when the optional is empty
        }, () -> {
            throw boom;
        }));

        // --- OptionalLong: filter, map, mapToInt, mapToDouble, mapToObj, flatMap, ifPresent, both arms of ifPresentOrElse
        assertThrows(IOException.class, () -> OptionalLong.of(1L).filter(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).map(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).mapToInt(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).mapToDouble(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).mapToObj(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).flatMap(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).ifPresent(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalLong.of(1L).ifPresentOrElse(v -> {
            throw boom;
        }, () -> {
            // the empty arm is not reached when the value is present
        }));
        assertThrows(IOException.class, () -> OptionalLong.empty().ifPresentOrElse(v -> {
            // the present arm is not reached when the optional is empty
        }, () -> {
            throw boom;
        }));

        // --- OptionalFloat: filter, map, mapToInt, mapToDouble, mapToObj, flatMap, ifPresent, both arms of ifPresentOrElse
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).filter(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).map(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).mapToInt(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).mapToDouble(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).mapToObj(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).flatMap(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).ifPresent(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalFloat.of(1f).ifPresentOrElse(v -> {
            throw boom;
        }, () -> {
            // the empty arm is not reached when the value is present
        }));
        assertThrows(IOException.class, () -> OptionalFloat.empty().ifPresentOrElse(v -> {
            // the present arm is not reached when the optional is empty
        }, () -> {
            throw boom;
        }));

        // --- OptionalDouble: filter, map, mapToInt, mapToLong, mapToObj, flatMap, ifPresent, both arms of ifPresentOrElse
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).filter(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).map(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).mapToInt(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).mapToLong(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).mapToObj(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).flatMap(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).ifPresent(v -> {
            throw boom;
        }));
        assertThrows(IOException.class, () -> OptionalDouble.of(1d).ifPresentOrElse(v -> {
            throw boom;
        }, () -> {
            // the empty arm is not reached when the value is present
        }));
        assertThrows(IOException.class, () -> OptionalDouble.empty().ifPresentOrElse(v -> {
            // the present arm is not reached when the optional is empty
        }, () -> {
            throw boom;
        }));
    }
}
