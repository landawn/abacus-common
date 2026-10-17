package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractArrayTypeTest extends TestBase {
    private Type<Object[]> type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = createType(Object[].class);
        writer = createCharacterWriter();
    }

    @Test
    public void testIsArray() {
        assertTrue(type.isArray());
    }

    @Test
    public void testGetSerializationType_Serializable() {
        assertEquals(SerializationType.ARRAY, type.serializationType());
    }

    @Test
    public void testArray2Collection_NullArray() {
        Collection<Object> result = type.arrayToCollection(null, ArrayList.class);
        assertNull(result);
    }

    @Test
    public void testArray2Collection_ArrayList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result instanceof ArrayList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_HashSet() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, HashSet.class);
        assertNotNull(result);
        assertTrue(result instanceof HashSet);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_LinkedList() {
        Object[] array = new Object[] { "a", "b", "c" };
        Collection<Object> result = type.arrayToCollection(array, LinkedList.class);
        assertNotNull(result);
        assertTrue(result instanceof LinkedList);
        assertEquals(3, result.size());
    }

    @Test
    public void testArray2Collection_EmptyArray() {
        Object[] array = new Object[0];
        Collection<Object> result = type.arrayToCollection(array, ArrayList.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // 2026-10-05: the shared compact-JSON element normalization behind the array codecs' valueOf(String).
    @Test
    public void jsonElementContent20261005() {
        assertNull(AbstractArrayType.jsonElementContent("null"));
        assertEquals("NULL", AbstractArrayType.jsonElementContent("NULL"));
        assertEquals("null", AbstractArrayType.jsonElementContent("\"null\""));
        assertEquals("1", AbstractArrayType.jsonElementContent("1"));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"1\""));
        assertEquals("-2", AbstractArrayType.jsonElementContent("'-2'"));
        assertEquals("abc", AbstractArrayType.jsonElementContent("\"abc\""));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"\\u0031\""));
        assertEquals("\t1", AbstractArrayType.jsonElementContent("'\\t1'"));
        // decoded exactly once: a double-escaped sequence is the escaped text
        assertEquals("\\u0031", AbstractArrayType.jsonElementContent("\"\\\\u0031\""));
        // a malformed escape is rejected
        assertThrows(IllegalArgumentException.class, () -> AbstractArrayType.jsonElementContent("\"\\u00\""));
        // mismatched or lone quotes are not a quoted element
        assertEquals("\"1'", AbstractArrayType.jsonElementContent("\"1'"));
        assertEquals("\"1", AbstractArrayType.jsonElementContent("\"1"));
        assertEquals("\"", AbstractArrayType.jsonElementContent("\""));
        // an empty quoted element stays quoted for the numeric codecs and is its empty content for the boolean ones
        assertEquals("\"\"", AbstractArrayType.jsonElementContent("\"\""));
        assertEquals("''", AbstractArrayType.jsonElementContent("''"));
        assertEquals("\"\"", AbstractArrayType.jsonElementContent("\"\"", false));
        assertEquals("", AbstractArrayType.jsonElementContent("\"\"", true));
        assertEquals("", AbstractArrayType.jsonElementContent("''", true));
        assertEquals("Y", AbstractArrayType.jsonElementContent("'Y'", true));
        assertEquals("1", AbstractArrayType.jsonElementContent("\"\\u0031\"", true));
        assertNull(AbstractArrayType.jsonElementContent("null", true));
        assertEquals("\"", AbstractArrayType.jsonElementContent("\"", true));
    }

    // ---- deep review 2026-10-16 G162 begin ----
    // parseNumericArray (the decimal fast path of the 12 numeric array types) reads and converts each element in one
    // pass, also reads the bare null token, and converts short decimals to float/double directly (Clinger fast path).
    // Compare with split plus the scalar codecs directly: surrounding whitespace is now accepted by the fast path,
    // so changing the array's formatting no longer provides an independent reference parser.

    private static final Class<?>[] G162_COMPONENTS = { byte.class, short.class, int.class, long.class, float.class, double.class, Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class };

    private static String g162Outcome(final Supplier<Object> parse) {
        try {
            final Object value = parse.get();
            final StringBuilder sb = new StringBuilder(value.getClass().getSimpleName()).append(' ');

            for (int i = 0, len = Array.getLength(value); i < len; i++) {
                final Object element = Array.get(value, i);

                // Raw bits keep -0.0 and 0.0 apart.
                if (element instanceof Float f) {
                    sb.append('f').append(Integer.toHexString(Float.floatToRawIntBits(f)));
                } else if (element instanceof Double d) {
                    sb.append('d').append(Long.toHexString(Double.doubleToRawLongBits(d)));
                } else {
                    sb.append(element);
                }

                sb.append(',');
            }

            return sb.toString();
        } catch (final RuntimeException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static void g162AssertSameAsOriginalParser(final Class<?> component, final String text) {
        final Type<?> arrayType = Type.of(Array.newInstance(component, 0).getClass());

        assertEquals(g162Outcome(() -> g162ParseOriginal(component, text)), g162Outcome(() -> arrayType.valueOf(text)), () -> component + " " + text);
    }

    private static Object g162ParseOriginal(final Class<?> component, final String text) {
        final String[] elements = AbstractArrayType.split(text);
        final Type<?> elementType = Type.of(component);
        final Object result = Array.newInstance(component, elements.length);

        for (int i = 0; i < elements.length; i++) {
            Array.set(result, i, elementType.valueOf(AbstractArrayType.jsonElementContent(elements[i])));
        }

        return result;
    }

    @Test
    public void testParseNumericArray_nullTokenIsReadOnTheFastPathLikeTheOriginalParser() {
        for (final Class<?> component : G162_COMPONENTS) {
            final Type<?> elementType = Type.of(component);
            final Object nullDefault = component.isPrimitive() ? elementType.defaultValue() : null;

            for (final String text : List.of("[null]", "[null, 1, null]", "[ null ,1 , null ]", "[null,1,null]")) {
                final Object array = AbstractArrayType.parseNumericArray(text, elementType, component);

                assertNotNull(array, component + " " + text);
                assertEquals(nullDefault, Array.get(array, 0), component + " " + text);
                g162AssertSameAsOriginalParser(component, text);
            }

            final Object array = AbstractArrayType.parseNumericArray("[null, 1, null]", elementType, component);
            assertEquals(1, ((Number) Array.get(array, 1)).intValue());
            assertEquals(nullDefault, Array.get(array, 2));

            // Anything else spelled like null is still left to the original parser.
            for (final String text : List.of("[nul]", "[nullx]", "[null1]", "[-null]", "[null null]", "[\"null\"]", "[NULL]", "[null,]", "[1, null, ]")) {
                assertNull(AbstractArrayType.parseNumericArray(text, elementType, component), component + " " + text);
                g162AssertSameAsOriginalParser(component, text);
            }
        }
    }

    @Test
    public void testParseNumericArray_integralElementsMatchOriginalParserAtEveryLength() {
        final List<String> values = List.of("0", "-0", "+0", "+7", "-7", "127", "128", "-128", "-129", "32767", "32768", "-32769", "2147483647", "2147483648",
                "-2147483648", "-2147483649", "999999999999999999", "-999999999999999999", "1000000000000000000", "9223372036854775807",
                "9223372036854775808", "-9223372036854775808", "-9223372036854775809", "00000000000000000000000000042", "-0000000000000000000127",
                "99999999999999999999999");

        for (final Class<?> component : G162_COMPONENTS) {
            for (final String value : values) {
                g162AssertSameAsOriginalParser(component, "[" + value + "]");
                g162AssertSameAsOriginalParser(component, "[1, " + value + ", 2]");
            }
        }

        assertEquals(Long.MIN_VALUE, ((long[]) AbstractArrayType.parseNumericArray("[-9223372036854775808]", Type.of(long.class), long.class))[0]);
        assertEquals(42, ((int[]) AbstractArrayType.parseNumericArray("[00000000000000000000000000042]", Type.of(int.class), int.class))[0]);
        assertNull(AbstractArrayType.parseNumericArray("[1, 2147483648]", Type.of(int.class), int.class));
    }

    @Test
    public void testParseNumericArray_structuralWhitespaceAndQuotedDecimalsStayOnFastPath() {
        final List<String> inputs = List.of(" \t[1, 2]\r\n", "\u2003[\u20021,\t2\n]\u205f", "[\"1\",2]", "[1,'2']", "['1',\"2\"]",
                "\u001c[\r\"-12\"\n,\u3000null, '+7', -0\t]\u001f");

        for (final Class<?> component : G162_COMPONENTS) {
            for (final String input : inputs) {
                assertNotNull(AbstractArrayType.parseNumericArray(input, Type.of(component), component), component + " " + input);
                g162AssertSameAsOriginalParser(component, input);
            }

            for (final String input : List.of(" [] ", "[\u2003]", "[\"1,2\",3]", "[\"\\u0031\",2]", "['\\t1',2]", "[\"1\"x,2]", "[1,\"2']",
                    "[\"1' ,2]", "[\"\",2]", "['',2]", "[\"null\",2]", "[\" 1 \",2]", "[\"1\\\"\",2]", "[1\u00a0,2]", "\u00a0[1,2]",
                    "[\"1\",,2]", "[\"9223372036854775808\",\t]", "[bad,\u2003]", "[1,\"2,\"]", "[1,2]x")) {
                assertNull(AbstractArrayType.parseNumericArray(input, Type.of(component), component), component + " " + input);
                g162AssertSameAsOriginalParser(component, input);
            }
        }
    }

    @Test
    public void testParseNumericArray_fullRangeLongAccumulation() {
        for (final Class<?> component : List.of(long.class, Long.class)) {
            for (final String value : List.of("9223372036854775807", "-9223372036854775808", "+9223372036854775807", "999999999999999999",
                    "1000000000000000000", "-1000000000000000000", "0", "-0")) {
                final int signLength = value.charAt(0) == '-' || value.charAt(0) == '+' ? 1 : 0;
                final String padded = value.substring(0, signLength) + "0".repeat(100) + value.substring(signLength);
                final String input = " \t[\"" + value + "\", " + padded + ", null]\n";
                assertNotNull(AbstractArrayType.parseNumericArray(input, Type.of(component), component), input);
                g162AssertSameAsOriginalParser(component, input);
            }

            for (final String value : List.of("9223372036854775808", "-9223372036854775809", "9999999999999999999", "-9999999999999999999",
                    "18446744073709551616", "99999999999999999999")) {
                for (final String input : List.of("[" + value + "]", "['" + value + "',1]", "[" + value + ",\u2003]", "[000" + value + ",,1]")) {
                    assertNull(AbstractArrayType.parseNumericArray(input, Type.of(component), component), input);
                    g162AssertSameAsOriginalParser(component, input);
                }
            }
        }

        final Random random = new Random(20261018);

        for (int i = 0; i < 2000; i++) {
            final long value = random.nextLong();
            g162AssertSameAsOriginalParser(long.class, "[" + value + ",\"" + random.nextLong() + "\"]");
            g162AssertSameAsOriginalParser(Long.class, "[null,'" + value + "'," + random.nextLong() + "]");
        }
    }

    @Test
    public void testParseNumericArray_trailingZeroNormalizationKeepsExactRounding() {
        final List<String> significands = List.of("0", "-0", "1", "-125", "16777216", "16777217", "9007199254740992", "9007199254740993",
                "18446744073709551616", "1000000059604644775390625", "100000000000000011102230246251565404236316680908203125");

        for (final Class<?> component : List.of(float.class, Float.class, double.class, Double.class)) {
            for (final String significand : significands) {
                for (final int zeroCount : new int[] { 1, 18, 40, 128 }) {
                    for (final int scale : new int[] { -324, -46, -23, -22, -11, -10, -1, 0, 10, 11, 22, 23, 309 }) {
                        final String value = significand + "0".repeat(zeroCount) + "e" + (scale - zeroCount);
                        final String fraction = significand + "." + "0".repeat(zeroCount) + "e" + scale;
                        g162AssertSameAsOriginalParser(component, "[" + value + ", '" + fraction + "']");
                    }
                }
            }
        }

        final Random random = new Random(20261019);

        for (int i = 0; i < 1000; i++) {
            final long significand = random.nextBoolean() ? (1L << 53) - 20 + random.nextInt(40) : random.nextLong() >>> 11;
            final int zeroCount = 1 + random.nextInt(60);
            final String value = (random.nextBoolean() ? "-" : "") + significand + "0".repeat(zeroCount) + "e" + (random.nextInt(49) - 24 - zeroCount);

            for (final Class<?> component : List.of(float.class, Float.class, double.class, Double.class)) {
                g162AssertSameAsOriginalParser(component, "[" + value + "]");
            }
        }
    }

    @Test
    public void testParseNumericArray_cappedExponentCannotCancelALongFraction() {
        // The old exponent cap changed 1e1000000 to 1e100000. With 100000 fractional digits this silently became 1.
        final String fraction = "0." + "0".repeat(99_999) + "1";

        for (final Class<?> component : List.of(float.class, Float.class, double.class, Double.class)) {
            for (final String sign : List.of("", "-")) {
                for (final String exponent : List.of("100000", "1000000", "+1000001", "10000000", "-1000000", "2147483648", "-2147483648")) {
                    g162AssertSameAsOriginalParser(component, "[" + sign + fraction + "e" + exponent + "]");
                }

                g162AssertSameAsOriginalParser(component, "[" + sign + fraction + "0000e1000000]");
                g162AssertSameAsOriginalParser(component, "[" + sign + "0." + "0".repeat(100_000) + "e1000000]");
            }
        }
    }

    @Test
    public void testParseNumericArray_emptyElementIsRejectedBeforeTheArrayIsAllocated() {
        for (final String text : List.of("[,1]", "[1,,2]", "[1,2,,3]", "[1" + ",".repeat(1000) + "1]")) {
            assertNull(AbstractArrayType.parseNumericArray(text, Type.of(long.class), long.class), text);
            g162AssertSameAsOriginalParser(long.class, text);
        }

        // A run of commas must not size an array by its length (8 MB here) before the input is rejected.
        final String commas = "[1" + ",".repeat(1_000_000) + "1]";
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        final long before = threads.getCurrentThreadAllocatedBytes();

        assertNull(AbstractArrayType.parseNumericArray(commas, Type.of(long.class), long.class));
        assertTrue(threads.getCurrentThreadAllocatedBytes() - before < 1_000_000, "allocated " + (threads.getCurrentThreadAllocatedBytes() - before));
    }

    @Test
    public void testParseNumericArray_floatingDirectConversionIsCorrectlyRounded() {
        // Boundaries of the exact range (2^53 and 1e22 for double, 2^24 and 1e10 for float), values just outside it,
        // signed zeros, leading/trailing zeros, round-half-even midpoints and exponents beyond the int range.
        final List<String> values = List.of("0", "-0", "-0.0", "+.5", "5.", "0.1", "0.3", "-2.5e-3", "1e22", "1e23", "1e-22", "1e-23", "9007199254740992",
                "9007199254740993", "9007199254740995", "900719925474099.3", "16777216", "16777217", "16777219", "1677721.7", "1e10", "1e11", "1e-10",
                "1e-11", "3.4028235e38", "3.4028236e38", "1.4e-45", "4.9e-324", "123456789012345678", "1234567890123456789",
                "0.000000000000000000000000000001", "00000000000000000000000001.25", "1.2500000000000000000000000", "1.000000059604644775390625",
                "1.00000000000000011102230246251565404236316680908203125", "1e100000000", "0e999999999", "-1e-100000000", "2.2250738585072011e-308",
                "8.98846567431158e307", "12.34E+5", "12.34e-5", "1E0");

        for (final Class<?> component : List.of(float.class, double.class, Float.class, Double.class)) {
            for (final String value : values) {
                final String text = "[" + value + ", " + value + "]";

                assertNotNull(AbstractArrayType.parseNumericArray(text, Type.of(component), component), component + " " + text);
                g162AssertSameAsOriginalParser(component, text);
            }
        }

        assertEquals(Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(((double[]) AbstractArrayType.parseNumericArray("[-0]", Type.of(double.class), double.class))[0]));
        assertEquals(Float.floatToRawIntBits(-0.0f),
                Float.floatToRawIntBits(((Float[]) AbstractArrayType.parseNumericArray("[-0.0]", Type.of(Float.class), Float.class))[0]));

        // Short decimals the direct conversion accepts, and neighbours just outside its exact range.
        final Random random = new Random(20261017);

        for (int i = 0; i < 4000; i++) {
            final long floatSignificand = (1L << 24) - 50 + random.nextInt(100);
            final long doubleSignificand = (1L << 53) - 50 + random.nextInt(100);
            final int scale = random.nextInt(29) - 14;
            final String floatText = "[" + random.nextInt(1 << 24) + "e" + scale + ", " + floatSignificand + "E" + (random.nextInt(25) - 12) + "]";
            final String doubleText = "[" + (random.nextLong() >>> 11) + "e" + (random.nextInt(49) - 24) + ", " + doubleSignificand + "e" + scale + ", -"
                    + random.nextInt(1_000_000) + "." + random.nextInt(1000) + "]";

            g162AssertSameAsOriginalParser(float.class, floatText);
            g162AssertSameAsOriginalParser(Float.class, doubleText);
            g162AssertSameAsOriginalParser(double.class, doubleText);
            g162AssertSameAsOriginalParser(Double.class, floatText);
        }
    }

    @Test
    public void testParseNumericArray_randomInputMatchesOriginalParser() {
        final Random random = new Random(20261016);
        final String[] separators = { ",", ", ", " , ", ",  " };

        for (int round = 0; round < 2500; round++) {
            final int count = 1 + random.nextInt(5);
            final StringBuilder sb = new StringBuilder("[");

            if (random.nextInt(4) == 0) {
                sb.append(' ');
            }

            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append(separators[random.nextInt(separators.length)]);
                }

                switch (random.nextInt(16)) {
                    case 0 -> sb.append("null");
                    case 1 -> sb.append(random.nextInt(256) - 128);
                    case 2 -> sb.append(random.nextInt(70000) - 35000);
                    case 3 -> sb.append(random.nextInt());
                    case 4 -> sb.append(random.nextLong());
                    case 5 -> sb.append(random.nextBoolean() ? "+" : "-").append("000").append(random.nextInt(300));
                    case 6 -> sb.append(random.nextInt(1_000_000) / 100.0);
                    case 7 -> sb.append(random.nextDouble() * 1000);
                    case 8 -> sb.append(Double.longBitsToDouble(random.nextLong()));
                    case 9 -> sb.append(Float.intBitsToFloat(random.nextInt()));
                    case 10 -> sb.append(random.nextFloat() * 100);
                    case 11 -> sb.append(random.nextInt(100000)).append('e').append(random.nextInt(60) - 30);
                    case 12 -> sb.append('"').append(random.nextInt(100)).append('"');
                    case 13 -> sb.append(random.nextInt(100)).append(random.nextBoolean() ? "L" : "f");
                    case 14 -> sb.append(random.nextBoolean() ? "" : "\t").append(random.nextInt(100));
                    default -> sb.append(random.nextBoolean() ? "9223372036854775808" : "2147483648");
                }
            }

            final String text = sb.append(random.nextInt(4) == 0 ? " ]" : "]").toString();

            for (final Class<?> component : G162_COMPONENTS) {
                g162AssertSameAsOriginalParser(component, text);
            }
        }
    }
    // ---- deep review 2026-10-16 G162 end ----
}
