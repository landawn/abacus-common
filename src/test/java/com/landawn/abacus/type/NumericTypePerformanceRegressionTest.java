package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Numbers;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class NumericTypePerformanceRegressionTest extends TestBase {

    private static final Class<?>[] COMPONENTS = { byte.class, short.class, int.class, long.class, float.class, double.class, Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class };

    @Test
    void integralSuffixesRetainRadixPrecedenceAndOrdinaryLongSuffix() {
        for (final Class<?> component : List.of(byte.class, short.class, int.class, long.class, Byte.class, Short.class, Integer.class, Long.class)) {
            final Type<?> type = Type.of(component);

            for (final String suffix : List.of("F", "f", "D", "d", "L", "l")) {
                assertEquals(12L, ((Number) type.valueOf("+0012" + suffix)).longValue(), component + suffix);
                assertEquals(-12L, ((Number) type.valueOf("-12" + suffix)).longValue(), component + suffix);
            }

            assertEquals(31L, ((Number) type.valueOf("0x1F")).longValue());
            assertEquals(29L, ((Number) type.valueOf("#1D")).longValue());
            assertEquals(-31L, ((Number) type.valueOf("-0x1F")).longValue());

            for (final String invalid : List.of("1FF", "1fD", "1 L", " 1F", "1F ", "F", "+F", "1.0F", "1e2D")) {
                assertThrows(NumberFormatException.class, () -> type.valueOf(invalid), component + ": " + invalid);
            }
        }
    }

    @Test
    void integralSuffixOverflowKeepsCheckedFailureAndMessage() {
        final Class<?>[] components = { Byte.class, Short.class, Integer.class, Long.class };
        final String[] overflow = { "128", "32768", "2147483648", "9223372036854775808" };

        for (int i = 0; i < components.length; i++) {
            final Type<?> type = Type.of(components[i]);
            final String value = overflow[i];
            final ArithmeticException original = assertThrows(ArithmeticException.class, () -> type.valueOf(value));

            for (final String suffix : List.of("F", "f", "D", "d")) {
                assertEquals(original.getMessage(), assertThrows(ArithmeticException.class, () -> type.valueOf(value + suffix)).getMessage());
            }
        }
    }

    @Test
    void floatingSuffixesKeepRoundingSignedZeroAndExtendedForms() {
        for (final Class<?> component : List.of(float.class, double.class, Float.class, Double.class)) {
            final Type<?> type = Type.of(component);

            for (final String value : List.of("-0", "-0.0", "+.5", "1.", "1e-400", "1e400", "1.000000059604644775390625",
                    "1.00000000000000011102230246251565404236316680908203125", "0x1.8p1", "NaN", "Infinity")) {
                final Object expected = type.valueOf(value);
                assertSameNumericValue(expected, type.valueOf(value + "L"));
                assertSameNumericValue(expected, type.valueOf(" \t" + value + "l\n"));
            }

            for (final String invalid : List.of("1 L", "1F L", "1LL", "1fD", "1eL", "1e+L", "L", "0x1fL")) {
                assertThrows(NumberFormatException.class, () -> type.valueOf(invalid), component + ": " + invalid);
            }
        }
    }

    @Test
    void allNumericArrayFamiliesMatchScalarBoundaries() {
        for (final Class<?> component : COMPONENTS) {
            final Type<?> element = Type.of(component);
            final String[] values = boundaryValues(component);
            final Object result = arrayType(component).valueOf("[" + String.join(", ", values) + "]");
            assertEquals(Array.newInstance(component, 0).getClass(), result.getClass());
            assertEquals(values.length, Array.getLength(result));

            for (int i = 0; i < values.length; i++) {
                assertSameNumericValue(element.valueOf(values[i]), Array.get(result, i));
            }
        }
    }

    @Test
    void floatingArraysKeepMidpointRoundingAndSignedUnderflow() {
        final String[] values = { "-0", "-0.0", "-1e-999", "+.5", "1.", "1e+999", "1.000000059604644775390625",
                "1.00000000000000011102230246251565404236316680908203125" };

        for (final Class<?> component : List.of(float.class, double.class, Float.class, Double.class)) {
            final Object result = arrayType(component).valueOf("[" + String.join(", ", values) + "]");

            for (int i = 0; i < values.length; i++) {
                assertSameNumericValue(Type.of(component).valueOf(values[i]), Array.get(result, i));
            }
        }
    }

    @Test
    void arrayFallbackRetainsNullDefaultsAndStructuralWhitespace() {
        for (final Class<?> component : COMPONENTS) {
            final Type<?> type = arrayType(component);
            assertNull(type.valueOf((String) null));
            assertNull(type.valueOf("  "));
            assertEquals(0, Array.getLength(type.valueOf("[]")));
            assertEquals(0, Array.getLength(type.valueOf("[ ]")));
            final Object nullValues = type.valueOf("[null, 1]");
            assertEquals(Type.of(component).defaultValue(), Array.get(nullValues, 0));

            for (final String text : List.of("1,2", "[1,\t2]", "\u2003[1,\u20032]\u2003", "[1F,2D]")) {
                final Object result = type.valueOf(text);
                assertEquals(2, Array.getLength(result));
                assertEquals(1.0, ((Number) Array.get(result, 0)).doubleValue());
                assertEquals(2.0, ((Number) Array.get(result, 1)).doubleValue());
            }

            assertThrows(NumberFormatException.class, () -> type.valueOf("[\u0661]"));
        }
    }

    @Test
    void arrayFallbackRetainsPrimitiveAndBoxedQuoteDifferences() {
        for (final Class<?> component : COMPONENTS) {
            final Type<?> type = arrayType(component);

            if (component.isPrimitive()) {
                final Object result = type.valueOf("[\"1\",'2',\"\\u0033\"]");
                assertEquals(3, Array.getLength(result));
                assertEquals(3.0, ((Number) Array.get(result, 2)).doubleValue());
                assertThrows(NumberFormatException.class, () -> type.valueOf("[\"null\"]"));
            } else if (component == Long.class) {
                assertArrayEquals(new Long[] { 1L, 2L }, (Long[]) type.valueOf("[\"1\",'2']"));
                assertThrows(NumberFormatException.class, () -> type.valueOf("[\"\\u0033\"]"));
            } else {
                assertThrows(NumberFormatException.class, () -> type.valueOf("[\"1\"]"));
            }

            assertThrows(NumberFormatException.class, () -> type.valueOf("[\"\"]"));
        }
    }

    @Test
    void missingArraySlotWinsBeforeAnyNumericConversion() {
        for (final Class<?> component : COMPONENTS) {
            final Type<?> type = arrayType(component);

            for (final String text : List.of("[bad,]", "[2147483648,]", "[9223372036854775808, ]", "[1e+999,,2]")) {
                final IllegalArgumentException error = assertThrowsExactly(IllegalArgumentException.class, () -> type.valueOf(text));
                assertEquals("Missing array element at index 1", error.getMessage());
            }
        }
    }

    @Test
    void integralArrayOverflowRetainsTypeSpecificFailures() {
        final Class<?>[] components = { byte.class, short.class, int.class, long.class, Byte.class, Short.class, Integer.class, Long.class };
        final String[] values = { "128", "32768", "2147483648", "9223372036854775808" };

        for (int i = 0; i < components.length; i++) {
            final Class<?> component = components[i];
            final String overflow = values[i % 4];
            final ArithmeticException scalar = assertThrows(ArithmeticException.class, () -> Type.of(component).valueOf(overflow));
            final ArithmeticException array = assertThrows(ArithmeticException.class, () -> arrayType(component).valueOf("[0," + overflow + "]"));
            assertEquals(scalar.getMessage(), array.getMessage());
        }
    }

    @Test
    void customCapturedHandlerIsNotInvokedByTheFastPath() {
        final List<String> calls = new ArrayList<>();
        final AbstractType<Integer> custom = new AbstractType<>("NumericArrayCounter20261008") {
            @Override
            public Class<Integer> javaType() {
                return Integer.class;
            }

            @Override
            public String stringOf(final Integer value) {
                return value.toString();
            }

            @Override
            public Integer valueOf(final String text) {
                calls.add(text);

                if ("2".equals(text)) {
                    throw new IllegalStateException("second custom value");
                }

                return Integer.valueOf(text) + 100;
            }
        };

        // Matching javaType/name categories must not be mistaken for the exact built-in descriptor.
        for (final String text : List.of("[1,2,3]", "[1,3]", "[1,]", "[bad,]", "[1,2147483648]", "[null,1]")) {
            assertNull(AbstractArrayType.parseNumericArray(text, custom, Integer.class));
            assertTrue(calls.isEmpty(), "Fallback must receive the original input without any prior application conversion");
        }
    }

    @Test
    void capturedHandlerMustAlsoMatchTheArrayComponent() {
        assertNull(AbstractArrayType.parseNumericArray("[1,2]", Type.of(long.class), int.class));
        assertNull(AbstractArrayType.parseNumericArray("[1,2]", Type.of(Integer.class), int.class));
        assertNull(AbstractArrayType.parseNumericArray("[1,2]", Type.of(int.class), Integer.class));
    }

    @Test
    void primitiveListParsingInheritsArrayOptimizationAndFallbacks() {
        final Type<IntList> type = Type.of(IntList.class);
        assertArrayEquals(new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE }, type.valueOf("[-2147483648,0,2147483647]").toArray());
        assertArrayEquals(new int[] { 1, 0, 31 }, type.valueOf("[\"1\",null,0x1F]").toArray());
    }

    @Test
    void textWithoutASuffixRecoveryKeepsTheUnderlyingParserOutcome() {
        // valueOf hands only text ending in l/L/f/F/d/D to its suffix recovery. Everything else, including a lone
        // suffix letter, must produce exactly the value or the exception of the underlying parser.
        final List<String> inputs = List.of("0", "-7", "+42", "007", "127", "128", "-129", "32768", "2147483648", "9223372036854775808", "1.5", "1e5", "abc",
                "", " 1", "1 ", "+", "-", "0x", "\u0661", "L", "l", "F", "f", "D", "d", " ", ".", "1e", "NaN", "-Infinity", " 2.5\t");

        for (final String text : inputs) {
            if (!text.isEmpty()) {
                assertEquals(outcome(() -> Numbers.toByte(text)), outcome(() -> Type.of(byte.class).valueOf(text)), "byte " + text);
                assertEquals(outcome(() -> Numbers.toShort(text)), outcome(() -> Type.of(Short.class).valueOf(text)), "Short " + text);
                assertEquals(outcome(() -> Numbers.toInt(text)), outcome(() -> Type.of(int.class).valueOf(text)), "int " + text);
                assertEquals(outcome(() -> Numbers.toLong(text)), outcome(() -> Type.of(Long.class).valueOf(text)), "Long " + text);
                assertEquals(outcome(() -> Float.valueOf(text.trim())), outcome(() -> Type.of(float.class).valueOf(text)), "float " + text);
                assertEquals(outcome(() -> Double.valueOf(text.trim())), outcome(() -> Type.of(Double.class).valueOf(text)), "Double " + text);
            }
        }
    }

    @Test
    void trailingNonDecimalElementsFallBackWithTheirOriginalResults() {
        final Type<int[]> ints = Type.of(int[].class);
        final Type<Integer[]> boxed = Type.of(Integer[].class);

        for (final String text : List.of("[1, 2, null]", "[1, 2, \"3\"]", "[1, 2, 3L]", "[1, 2, 3 4]", "[1,2,]", "[1, 2,  ]", "[1, 2, 3x]", "[null]",
                "[1, 2, [3]]", "[1, 2, 3e1]")) {
            assertNull(AbstractArrayType.parseNumericArray(text, Type.of(int.class), int.class), text);
        }

        assertArrayEquals(new int[] { 1, 2, 3 }, (int[]) AbstractArrayType.parseNumericArray("[1, 2 , 3 ]", Type.of(int.class), int.class));
        assertArrayEquals(new int[] { 7 }, (int[]) AbstractArrayType.parseNumericArray("[ 7 ]", Type.of(int.class), int.class));
        assertArrayEquals(new double[] { 1, 2, 30 }, (double[]) AbstractArrayType.parseNumericArray("[1, 2, 3e1]", Type.of(double.class), double.class));

        assertArrayEquals(new int[] { 1, 2, 0 }, ints.valueOf("[1, 2, null]"));
        assertArrayEquals(new Integer[] { 1, 2, null }, boxed.valueOf("[1, 2, null]"));
        assertArrayEquals(new int[] { 1, 2, 3 }, ints.valueOf("[1, 2, \"3\"]"));
        assertArrayEquals(new int[] { 1, 2, 3 }, ints.valueOf("[1, 2, 3L]"));
        assertEquals("Missing array element at index 2", assertThrows(IllegalArgumentException.class, () -> ints.valueOf("[1,2,]")).getMessage());
        assertThrows(NumberFormatException.class, () -> ints.valueOf("[1, 2, 3 4]"));
        assertThrows(NumberFormatException.class, () -> boxed.valueOf("[1, 2, \"3\"]"));
    }

    @Test
    void integralFallbackKeepsHexDigitsAndMinimumValueSpellings() {
        // A final F or D of hexadecimal text is a digit: an overflowing value must not be read as its suffix-free prefix.
        final String[][] overflowingHex = { { "0x1AD", "0x1AF" }, { "0x1234AD", "0x1234AF" }, { "0x1234567AD", "0x1234567AF" },
                { "0x1234567890ABCDEAD", "0x1234567890ABCDEAF" } };
        final Class<?>[][] components = { { byte.class, Byte.class }, { short.class, Short.class }, { int.class, Integer.class }, { long.class, Long.class } };

        for (int i = 0; i < components.length; i++) {
            for (final Class<?> component : components[i]) {
                for (final String text : overflowingHex[i]) {
                    assertThrows(ArithmeticException.class, () -> Type.of(component).valueOf(text), component + " " + text);
                }
            }
        }

        // MIN_VALUE also marks text the allocation-free parse rejects; its own spellings must still parse.
        for (final String suffix : List.of("", "L", "l", "F", "f", "D", "d")) {
            assertEquals(Integer.MIN_VALUE, Type.of(int.class).valueOf("-2147483648" + suffix), suffix);
            assertEquals(Long.MIN_VALUE, Type.of(Long.class).valueOf("-9223372036854775808" + suffix), suffix);
            assertEquals(Short.MIN_VALUE, Type.of(short.class).valueOf("-32768" + suffix), suffix);
            assertEquals(Byte.MIN_VALUE, Type.of(Byte.class).valueOf("-128" + suffix), suffix);
        }

        assertEquals(Integer.MIN_VALUE, Type.of(Integer.class).valueOf("-0x80000000"));
        assertEquals(Long.MIN_VALUE, Type.of(long.class).valueOf("-0x8000000000000000L"));
    }

    @Test
    void decimalTypeSuffixesParseWithoutConstructingExceptions() {
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final List<Type<?>> types = List.of(Type.of(int.class), Type.of(Long.class), Type.of(short.class), Type.of(Byte.class));
        final String[] texts = { "12F", "-7d", "+100D", "45f" };
        long checksum = 0;

        for (int i = 0; i < 20_000; i++) {
            checksum += ((Number) types.get(i & 3).valueOf(texts[(i >> 2) & 3])).longValue();
        }

        final int calls = 40_000;
        final long before = threads.getCurrentThreadAllocatedBytes();

        for (int i = 0; i < calls; i++) {
            checksum += ((Number) types.get(i & 3).valueOf(texts[(i >> 2) & 3])).longValue();
        }

        final long perCall = (threads.getCurrentThreadAllocatedBytes() - before) / calls;

        // The suffix-free copy and a boxed result remain; an exception with its stack trace costs well over a kilobyte.
        assertTrue(perCall < 256, "suffixed integral text allocated " + perCall + " bytes per call");
        assertTrue(checksum != 0);
    }

    private static String outcome(final Callable<?> call) {
        try {
            final Object value = call.call();

            if (value instanceof Float f) {
                return "Float " + Integer.toHexString(Float.floatToRawIntBits(f));
            } else if (value instanceof Double d) {
                return "Double " + Long.toHexString(Double.doubleToRawLongBits(d));
            }

            return value.getClass().getSimpleName() + " " + value;
        } catch (final Exception e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static Type<?> arrayType(final Class<?> component) {
        return Type.of(Array.newInstance(component, 0).getClass());
    }

    private static String[] boundaryValues(final Class<?> component) {
        if (component == byte.class || component == Byte.class) {
            return new String[] { "-128", "-0", "+0", "127" };
        } else if (component == short.class || component == Short.class) {
            return new String[] { "-32768", "-0", "+0", "32767" };
        } else if (component == int.class || component == Integer.class) {
            return new String[] { "-2147483648", "-0", "+0", "2147483647" };
        } else if (component == long.class || component == Long.class) {
            return new String[] { "-9223372036854775808", "-0", "+0", "9223372036854775807" };
        } else if (component == float.class || component == Float.class) {
            return new String[] { "-3.4028235e38", "-0.0", "1.4e-45", "3.4028235e38" };
        } else {
            return new String[] { "-1.7976931348623157e308", "-0.0", "4.9e-324", "1.7976931348623157e308" };
        }
    }

    private static void assertSameNumericValue(final Object expected, final Object actual) {
        if (expected instanceof Float value) {
            assertEquals(Float.floatToRawIntBits(value), Float.floatToRawIntBits((Float) actual));
        } else if (expected instanceof Double value) {
            assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits((Double) actual));
        } else {
            assertEquals(expected, actual);
        }
    }
}
