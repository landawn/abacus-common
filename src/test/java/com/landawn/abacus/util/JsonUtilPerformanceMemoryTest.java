package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for JsonUtil. */
@Tag("slow-test")
public class JsonUtilPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G038 end ----
    // ---- perf review 2026-10-08 G041 begin ----
    @Nested
    class G041 {

        /**
         * Compares a JsonUtil.wrap(array) result with org.json's reflective JSONArray(Object) constructor, which is what
         * every JsonUtil.wrap(array) overload delegated to before: same length, element classes, element text and JSON.
         */
        private void assertSameAsReflectiveConstructor(final Object array, final org.json.JSONArray actual) {
            final org.json.JSONArray expected = new org.json.JSONArray(array);

            assertEquals(expected.length(), actual.length());

            for (int i = 0; i < expected.length(); i++) {
                final Object expectedElement = expected.get(i);
                final Object actualElement = actual.get(i);

                assertEquals(expectedElement.getClass(), actualElement.getClass(), "element class at " + i);
                assertEquals(String.valueOf(expectedElement), String.valueOf(actualElement), "element at " + i);

                if (!(expectedElement instanceof org.json.JSONObject) && !(expectedElement instanceof org.json.JSONArray)) {
                    assertEquals(expectedElement, actualElement, "element at " + i);
                }
            }

            assertTrue(expected.similar(actual));
            assertEquals(expected.toString(), actual.toString());
        }

        /** Runs both conversions and requires the same result or the same exception class and message. */
        private void assertSameOutcomeAsReflectiveConstructor(final Object array, final java.util.function.Supplier<org.json.JSONArray> wrap) {
            org.json.JSONArray expected = null;
            RuntimeException expectedFailure = null;

            try {
                expected = new org.json.JSONArray(array);
            } catch (final RuntimeException e) {
                expectedFailure = e;
            }

            if (expectedFailure == null) {
                final org.json.JSONArray actual = wrap.get();
                assertEquals(expected.toString(), actual.toString());
                assertSameAsReflectiveConstructor(array, actual);
            } else {
                final RuntimeException actualFailure = assertThrows(RuntimeException.class, wrap::get);
                assertEquals(expectedFailure.getClass(), actualFailure.getClass());
                assertEquals(expectedFailure.getMessage(), actualFailure.getMessage());
            }
        }

        // G041-01: wrap(primitive[]) appends through typed put(..) instead of the reflective JSONArray(Object) constructor.
        @Test
        public void testWrapPrimitiveArrays_sameElementsAsReflectiveConstructor() {
            final boolean[] booleans = { true, false, false, true };
            final char[] chars = { 'a', (char) 0, (char) 0x7F, (char) 0x80, (char) 0xE9, (char) 0xFFFF, 'Z' };
            final byte[] bytes = { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };
            final short[] shorts = { Short.MIN_VALUE, -129, -128, 0, 127, 128, Short.MAX_VALUE };
            final int[] ints = { Integer.MIN_VALUE, -129, -128, -1, 0, 1, 127, 128, Integer.MAX_VALUE };
            final long[] longs = { Long.MIN_VALUE, -129L, -1L, 0L, 127L, 128L, Long.MAX_VALUE };
            final float[] floats = { -Float.MAX_VALUE, -1.5f, -0.0f, 0.0f, Float.MIN_VALUE, 0.1f, 3.4028235E38f };
            final double[] doubles = { -Double.MAX_VALUE, -1.5, -0.0, 0.0, Double.MIN_VALUE, 0.1, Double.MAX_VALUE };

            assertSameAsReflectiveConstructor(booleans, JsonUtil.wrap(booleans));
            assertSameAsReflectiveConstructor(chars, JsonUtil.wrap(chars));
            assertSameAsReflectiveConstructor(bytes, JsonUtil.wrap(bytes));
            assertSameAsReflectiveConstructor(shorts, JsonUtil.wrap(shorts));
            assertSameAsReflectiveConstructor(ints, JsonUtil.wrap(ints));
            assertSameAsReflectiveConstructor(longs, JsonUtil.wrap(longs));
            assertSameAsReflectiveConstructor(floats, JsonUtil.wrap(floats));
            assertSameAsReflectiveConstructor(doubles, JsonUtil.wrap(doubles));

            // char/byte/short stay Character/Byte/Short (there is no put(char/byte/short); put(int) would give Integer).
            assertEquals(Character.class, JsonUtil.wrap(chars).get(0).getClass());
            assertEquals(Byte.class, JsonUtil.wrap(bytes).get(0).getClass());
            assertEquals(Short.class, JsonUtil.wrap(shorts).get(0).getClass());
            assertEquals(Boolean.class, JsonUtil.wrap(booleans).get(0).getClass());

            // Signed zero survives.
            assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits((Float) JsonUtil.wrap(floats).get(2)));
            assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits((Double) JsonUtil.wrap(doubles).get(2)));

            // Empty and singleton arrays.
            assertEquals(0, JsonUtil.wrap(new boolean[0]).length());
            assertEquals(0, JsonUtil.wrap(new char[0]).length());
            assertEquals(0, JsonUtil.wrap(new byte[0]).length());
            assertEquals(0, JsonUtil.wrap(new short[0]).length());
            assertEquals(0, JsonUtil.wrap(new int[0]).length());
            assertEquals(0, JsonUtil.wrap(new long[0]).length());
            assertEquals(0, JsonUtil.wrap(new float[0]).length());
            assertEquals(0, JsonUtil.wrap(new double[0]).length());
            assertEquals("[]", JsonUtil.wrap(new int[0]).toString());
            assertSameAsReflectiveConstructor(new int[] { 42 }, JsonUtil.wrap(new int[] { 42 }));
            assertSameAsReflectiveConstructor(new char[] { 'x' }, JsonUtil.wrap(new char[] { 'x' }));

            // The result is a fresh, independent, mutable JSONArray.
            final org.json.JSONArray first = JsonUtil.wrap(ints);
            assertNotSame(first, JsonUtil.wrap(ints));
            first.put(5);
            assertEquals(ints.length + 1, first.length());
            assertEquals(ints.length, JsonUtil.wrap(ints).length());
        }

        // G041-01: larger random inputs, every primitive overload.
        @Test
        public void testWrapPrimitiveArrays_randomLargeInputs() {
            final Random random = new Random(41);
            final int size = 3000;
            final boolean[] booleans = new boolean[size];
            final char[] chars = new char[size];
            final byte[] bytes = new byte[size];
            final short[] shorts = new short[size];
            final int[] ints = new int[size];
            final long[] longs = new long[size];
            final float[] floats = new float[size];
            final double[] doubles = new double[size];

            for (int i = 0; i < size; i++) {
                booleans[i] = random.nextBoolean();
                chars[i] = (char) random.nextInt(0x10000);
                bytes[i] = (byte) random.nextInt();
                shorts[i] = (short) random.nextInt();
                ints[i] = i % 3 == 0 ? random.nextInt(256) - 128 : random.nextInt();
                longs[i] = i % 3 == 0 ? random.nextInt(256) - 128 : random.nextLong();
                floats[i] = (random.nextFloat() - 0.5f) * (float) Math.pow(10, random.nextInt(30) - 15);
                doubles[i] = (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(600) - 300);
            }

            assertSameAsReflectiveConstructor(booleans, JsonUtil.wrap(booleans));
            assertSameAsReflectiveConstructor(chars, JsonUtil.wrap(chars));
            assertSameAsReflectiveConstructor(bytes, JsonUtil.wrap(bytes));
            assertSameAsReflectiveConstructor(shorts, JsonUtil.wrap(shorts));
            assertSameAsReflectiveConstructor(ints, JsonUtil.wrap(ints));
            assertSameAsReflectiveConstructor(longs, JsonUtil.wrap(longs));
            assertSameAsReflectiveConstructor(floats, JsonUtil.wrap(floats));
            assertSameAsReflectiveConstructor(doubles, JsonUtil.wrap(doubles));
        }

        // G041-01: non-finite values fail with the same JSONException; null arrays are still rejected first.
        @Test
        public void testWrapPrimitiveArrays_failuresUnchanged() {
            for (final float bad : new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY }) {
                final float[] array = { 1.5f, bad, 2.5f };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
                assertEquals("JSON does not allow non-finite numbers.", assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(array)).getMessage());
            }

            for (final double bad : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
                final double[] array = { bad };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
                assertEquals("JSON does not allow non-finite numbers.", assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(array)).getMessage());
            }

            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((boolean[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((char[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((byte[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((short[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((int[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((long[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((float[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((double[]) null));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.wrap((Object[]) null));
        }

        // G041-02: wrap(Object[]) goes through JSONArray(Collection) instead of the reflective array constructor.
        @Test
        public void testWrapObjectArray_sameElementsAsReflectiveConstructor() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", 1);
            map.put("n", null);
            map.put("list", Arrays.asList(1, null, "x"));

            final org.json.JSONObject jsonObject = new org.json.JSONObject().put("a", 1);
            final org.json.JSONArray jsonArray = new org.json.JSONArray().put(1).put("b");
            final Object[] array = { null, "text", 7, 7L, 1.25d, 2.5f, (short) 3, (byte) 4, 'c', true, new java.math.BigDecimal("1.10"),
                    new java.math.BigInteger("123456789012345678901234567890"), map, Arrays.asList(1, null, "x"), new LinkedHashSet<>(Arrays.asList(3, 1, 2)),
                    new int[] { 1, 2 }, new char[] { 'h', 'i' }, new Object[] { "nested", null, new double[] { 0.5 } }, jsonObject, jsonArray,
                    java.util.concurrent.TimeUnit.SECONDS, new StringBuilder("sb"), org.json.JSONObject.NULL };

            final org.json.JSONArray wrapped = JsonUtil.wrap(array);
            assertSameAsReflectiveConstructor(array, wrapped);
            assertSame(org.json.JSONObject.NULL, wrapped.get(0));
            assertSame(jsonObject, wrapped.get(18));
            assertSame(jsonArray, wrapped.get(19));
            assertSame(org.json.JSONObject.NULL, wrapped.get(22));

            assertSameAsReflectiveConstructor(new Object[0], JsonUtil.wrap(new Object[0]));
            assertSameAsReflectiveConstructor(new Object[] { null }, JsonUtil.wrap(new Object[] { null }));

            // A covariant String[] argument.
            final String[] strings = { "a", null, "c" };
            assertSameAsReflectiveConstructor(strings, JsonUtil.wrap(strings));

            final Random random = new Random(4102);
            final Object[] large = new Object[2000];

            for (int i = 0; i < large.length; i++) {
                final int kind = random.nextInt(5);
                large[i] = kind == 0 ? null
                        : kind == 1 ? "s" + random.nextInt() : kind == 2 ? random.nextInt() : kind == 3 ? random.nextDouble() : Arrays.asList(i, "e");
            }

            assertSameAsReflectiveConstructor(large, JsonUtil.wrap(large));
        }

        // G041-02: the failures (non-finite numbers, nesting-depth limit) are reported exactly as before, at the same depth.
        @Test
        public void testWrapObjectArray_failuresAndNestingLimitUnchanged() {
            final Object[] withNaN = { 1, Double.NaN };
            assertSameOutcomeAsReflectiveConstructor(withNaN, () -> JsonUtil.wrap(withNaN));
            assertThrows(org.json.JSONException.class, () -> JsonUtil.wrap(withNaN));

            final Object[] withNestedInfinity = { Arrays.asList(1.0f, Float.POSITIVE_INFINITY) };
            assertSameOutcomeAsReflectiveConstructor(withNestedInfinity, () -> JsonUtil.wrap(withNestedInfinity));

            // Nested lists around org.json's nesting limit: both routes must start the elements at the same depth.
            for (final int depth : new int[] { 510, 511, 512, 513, 514 }) {
                final List<Object> deep = new ArrayList<>();
                List<Object> current = deep;

                for (int i = 1; i < depth; i++) {
                    final List<Object> next = new ArrayList<>();
                    current.add(next);
                    current = next;
                }

                current.add("leaf");

                final Object[] array = { "head", deep };
                assertSameOutcomeAsReflectiveConstructor(array, () -> JsonUtil.wrap(array));
            }

            // A self-containing map element hits the same recursion guard.
            final Map<String, Object> self = new HashMap<>();
            self.put("self", self);
            final Object[] withCycle = { self };
            assertSameOutcomeAsReflectiveConstructor(withCycle, () -> JsonUtil.wrap(withCycle));
        }

        // G041-03: unwrap(JSONArray, primitive[]) stores exact-wrapper elements directly instead of through Array.set.
        @Test
        public void testUnwrapToPrimitiveArrays_valuesDefaultsAndConversions() {
            final org.json.JSONArray ints = new org.json.JSONArray().put(1)
                    .put(org.json.JSONObject.NULL)
                    .put("12")
                    .put(Integer.MIN_VALUE)
                    .put(Integer.MAX_VALUE)
                    .put(-128)
                    .put(128);
            assertArrayEquals(new int[] { 1, 0, 12, Integer.MIN_VALUE, Integer.MAX_VALUE, -128, 128 }, JsonUtil.unwrap(ints, int[].class));
            assertArrayEquals(new int[] { 1, 0, 12, Integer.MIN_VALUE, Integer.MAX_VALUE, -128, 128 },
                    (int[]) JsonUtil.unwrap(ints, CommonUtil.typeOf(int[].class)));

            final org.json.JSONArray longs = new org.json.JSONArray().put(Long.MIN_VALUE).put(org.json.JSONObject.NULL).put(Long.MAX_VALUE).put(7).put("-9");
            assertArrayEquals(new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE, 7L, -9L }, JsonUtil.unwrap(longs, long[].class));

            final org.json.JSONArray doubles = new org.json.JSONArray().put(1.5).put(org.json.JSONObject.NULL).put(-0.0).put(Double.MAX_VALUE).put("2.25");
            final double[] doubleResult = JsonUtil.unwrap(doubles, double[].class);
            assertEquals(5, doubleResult.length);
            assertEquals(1.5, doubleResult[0], 0.0);
            assertEquals(0.0, doubleResult[1], 0.0);
            assertEquals(Double.MAX_VALUE, doubleResult[3], 0.0);
            assertEquals(2.25, doubleResult[4], 0.0);

            final org.json.JSONArray floats = new org.json.JSONArray().put(1.5f).put(org.json.JSONObject.NULL).put("2.5").put(Float.MAX_VALUE);
            final float[] floatResult = JsonUtil.unwrap(floats, float[].class);
            assertEquals(4, floatResult.length);
            assertEquals(1.5f, floatResult[0], 0.0f);
            assertEquals(0.0f, floatResult[1], 0.0f);
            assertEquals(2.5f, floatResult[2], 0.0f);
            assertEquals(Float.MAX_VALUE, floatResult[3], 0.0f);

            final org.json.JSONArray booleans = new org.json.JSONArray().put(true).put(org.json.JSONObject.NULL).put(false).put("true");
            assertArrayEquals(new boolean[] { true, false, false, true }, JsonUtil.unwrap(booleans, boolean[].class));

            final org.json.JSONArray chars = new org.json.JSONArray().put("A").put(org.json.JSONObject.NULL).put("z");
            assertArrayEquals(new char[] { 'A', (char) 0, 'z' }, JsonUtil.unwrap(chars, char[].class));

            final org.json.JSONArray bytes = new org.json.JSONArray().put(Byte.MIN_VALUE).put(org.json.JSONObject.NULL).put(Byte.MAX_VALUE).put("5");
            assertArrayEquals(new byte[] { Byte.MIN_VALUE, 0, Byte.MAX_VALUE, 5 }, JsonUtil.unwrap(bytes, byte[].class));

            final org.json.JSONArray shorts = new org.json.JSONArray().put(Short.MIN_VALUE).put(org.json.JSONObject.NULL).put(Short.MAX_VALUE).put("-6");
            assertArrayEquals(new short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE, -6 }, JsonUtil.unwrap(shorts, short[].class));

            // Empty input, a fresh result per call, and the round trip through wrap.
            assertEquals(0, JsonUtil.unwrap(new org.json.JSONArray(), int[].class).length);
            assertNotSame(JsonUtil.unwrap(ints, int[].class), JsonUtil.unwrap(ints, int[].class));

            final Random random = new Random(1041);
            final int[] source = new int[4000];
            final double[] doubleSource = new double[4000];

            for (int i = 0; i < source.length; i++) {
                source[i] = random.nextInt();
                doubleSource[i] = random.nextDouble() * 1e6 - 5e5;
            }

            assertArrayEquals(source, JsonUtil.unwrap(JsonUtil.wrap(source), int[].class));
            assertArrayEquals(doubleSource, JsonUtil.unwrap(JsonUtil.wrap(doubleSource), double[].class), 0.0);
        }

        // G041-03: a conversion failure is still thrown by the element conversion, before any store, with the same type.
        @Test
        public void testUnwrapToPrimitiveArrays_conversionFailuresUnchanged() {
            assertThrows(NumberFormatException.class, () -> JsonUtil.unwrap(new org.json.JSONArray().put(1).put("abc"), int[].class));
            assertThrows(RuntimeException.class, () -> JsonUtil.unwrap(new org.json.JSONArray().put(1).put(new org.json.JSONObject()), int[].class));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.unwrap((org.json.JSONArray) null, int[].class));
        }
    }
}
