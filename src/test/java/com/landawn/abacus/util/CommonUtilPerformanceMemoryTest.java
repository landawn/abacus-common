package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for CommonUtil. */
@Tag("slow-test")
public class CommonUtilPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G009 end ----
    // ---- perf review 2026-10-08 G011 begin ----
    @Nested
    class G011 {

        private int referenceHash(final char[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final byte[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final short[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        private int referenceHash(final int[] a, final int fromIndex, final int toIndex) {
            int result = 1;

            for (int i = fromIndex; i < toIndex; i++) {
                result = 31 * result + a[i];
            }

            return result;
        }

        // G011-01: the range hashCode(char[]/byte[]/short[]/int[], from, to) overloads take Arrays.hashCode for the whole-array
        // range; pin both the whole-array and every partial range against the 31 * h + e reference fold.
        @Test
        public void testHashCodeRange_wholeAndPartialRangesMatchReference() {
            final Random random = new Random(20261008L);
            final int[] lengths = { 0, 1, 2, 3, 7, 8, 9, 15, 16, 17, 31, 32, 33, 63, 64, 65, 100, 1000, 4099 };

            for (final int len : lengths) {
                final char[] ca = new char[len];
                final byte[] ba = new byte[len];
                final short[] sa = new short[len];
                final int[] ia = new int[len];

                for (int i = 0; i < len; i++) {
                    final int r = random.nextInt();
                    // include the extreme values of each type (char is widened unsigned, byte/short signed)
                    ca[i] = i % 11 == 0 ? Character.MAX_VALUE : (char) r;
                    ba[i] = i % 13 == 0 ? Byte.MIN_VALUE : (byte) r;
                    sa[i] = i % 17 == 0 ? Short.MIN_VALUE : (short) r;
                    ia[i] = i % 19 == 0 ? Integer.MIN_VALUE : r;
                }

                // whole array: the new branch
                assertEquals(referenceHash(ca, 0, len), CommonUtil.hashCode(ca, 0, len));
                assertEquals(referenceHash(ba, 0, len), CommonUtil.hashCode(ba, 0, len));
                assertEquals(referenceHash(sa, 0, len), CommonUtil.hashCode(sa, 0, len));
                assertEquals(referenceHash(ia, 0, len), CommonUtil.hashCode(ia, 0, len));
                assertEquals(CommonUtil.hashCode(ia), CommonUtil.hashCode(ia, 0, len));

                // partial ranges: the loop
                final int[][] ranges = { { 0, len - 1 }, { 1, len }, { len / 3, len - len / 3 }, { len / 2, len / 2 }, { len, len } };

                for (final int[] range : ranges) {
                    final int from = range[0];
                    final int to = range[1];

                    if (from < 0 || to < from) {
                        continue;
                    }

                    assertEquals(referenceHash(ca, from, to), CommonUtil.hashCode(ca, from, to));
                    assertEquals(referenceHash(ba, from, to), CommonUtil.hashCode(ba, from, to));
                    assertEquals(referenceHash(sa, from, to), CommonUtil.hashCode(sa, from, to));
                    assertEquals(referenceHash(ia, from, to), CommonUtil.hashCode(ia, from, to));
                }
            }
        }

        // G011-01: null/empty arrays and the range validation (type, message and precedence over the null check) are unchanged.
        @Test
        public void testHashCodeRange_nullEmptyAndValidationUnchanged() {
            assertEquals(0, CommonUtil.hashCode((char[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((byte[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((short[]) null, 0, 0));
            assertEquals(0, CommonUtil.hashCode((int[]) null, 0, 0));
            assertEquals(1, CommonUtil.hashCode(new char[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new byte[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new short[0], 0, 0));
            assertEquals(1, CommonUtil.hashCode(new int[0], 0, 0));
            assertEquals(31 + 'x', CommonUtil.hashCode(new char[] { 'x' }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new byte[] { -7 }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new short[] { -7 }, 0, 1));
            assertEquals(31 - 7, CommonUtil.hashCode(new int[] { -7 }, 0, 1));

            final int[] ia = { 1, 2, 3, 4, 5 };
            assertEquals("Index range [0, 1) is out-of-bounds for length 0",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode((int[]) null, 0, 1)).getMessage());
            assertEquals("Index range [0, 6) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, 0, 6)).getMessage());
            assertEquals("Index range [-1, 5) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, -1, 5)).getMessage());
            assertEquals("Index range [3, 2) is out-of-bounds for length 5",
                    assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(ia, 3, 2)).getMessage());
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new char[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new byte[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(new short[2], 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode((char[]) null, 0, 1));
        }

        // G011-01: PrimitiveList.hashCode() passes (0, size): a full list (size == capacity) now takes the whole-array branch,
        // a list with spare capacity the loop; equal lists keep equal hash codes either way.
        @Test
        public void testPrimitiveListHashCode_fullAndSpareCapacityListsAgree() {
            final Random random = new Random(8L);

            for (final int len : new int[] { 0, 1, 5, 16, 33, 1000 }) {
                final int[] ia = new int[len];
                final char[] ca = new char[len];
                final byte[] ba = new byte[len];
                final short[] sa = new short[len];

                for (int i = 0; i < len; i++) {
                    ia[i] = random.nextInt();
                    ca[i] = (char) random.nextInt();
                    ba[i] = (byte) random.nextInt();
                    sa[i] = (short) random.nextInt();
                }

                final IntList fullInts = IntList.of(ia.clone());
                final IntList spareInts = new IntList(Arrays.copyOf(ia, len + 3), len);
                assertEquals(referenceHash(ia, 0, len), fullInts.hashCode());
                assertEquals(referenceHash(ia, 0, len), spareInts.hashCode());
                assertEquals(fullInts, spareInts);

                final CharList fullChars = CharList.of(ca.clone());
                final CharList spareChars = new CharList(Arrays.copyOf(ca, len + 3), len);
                assertEquals(referenceHash(ca, 0, len), fullChars.hashCode());
                assertEquals(referenceHash(ca, 0, len), spareChars.hashCode());
                assertEquals(fullChars, spareChars);

                final ByteList fullBytes = ByteList.of(ba.clone());
                final ByteList spareBytes = new ByteList(Arrays.copyOf(ba, len + 3), len);
                assertEquals(referenceHash(ba, 0, len), fullBytes.hashCode());
                assertEquals(referenceHash(ba, 0, len), spareBytes.hashCode());
                assertEquals(fullBytes, spareBytes);

                final ShortList fullShorts = ShortList.of(sa.clone());
                final ShortList spareShorts = new ShortList(Arrays.copyOf(sa, len + 3), len);
                assertEquals(referenceHash(sa, 0, len), fullShorts.hashCode());
                assertEquals(referenceHash(sa, 0, len), spareShorts.hashCode());
                assertEquals(fullShorts, spareShorts);

                // a mutation through the list is seen by the next hashCode() on both paths
                if (len > 0) {
                    fullInts.set(len - 1, ia[len - 1] + 1);
                    spareInts.set(len - 1, ia[len - 1] + 1);
                    assertEquals(fullInts.hashCode(), spareInts.hashCode());
                    assertEquals(referenceHash(fullInts.toArray(), 0, len), fullInts.hashCode());
                }
            }
        }
    }

    // ---- perf review 2026-10-08 G011 end ----
    // ---- perf review 2026-10-08 G012 begin ----
    @Nested
    class G012 {

        static class StaticBean {
            int value = 7;

            class InnerOfStatic {
                StaticBean outer() {
                    return StaticBean.this;
                }
            }
        }

        class InnerBean {
            int value = 11;

            G012 outer() {
                return G012.this;
            }

            class Deeper {
                InnerBean outer() {
                    return InnerBean.this;
                }
            }
        }

        class InnerWithoutUsableConstructor {
            InnerWithoutUsableConstructor(final int ignored) {
            }
        }

        private final Object anonymousBean = new Object() {
            @Override
            public String toString() {
                return "anonymous bean";
            }
        };

        @SuppressWarnings("serial")
        private final List<String> anonymousList = new ArrayList<>() {
        };

        private <T> void assertFreshInstances(final Class<T> cls, final Class<?> expectedResultClass) {
            final Object first = CommonUtil.newInstance(cls);
            final Object second = CommonUtil.newInstance(cls);

            assertEquals(expectedResultClass, first.getClass());
            assertEquals(expectedResultClass, second.getClass());
            assertNotSame(first, second);
        }

        // G012-01: newInstance caches the anonymous-or-member and enclosing-class lookups; results for every class shape, on
        // first and repeated (cached) calls, interleaved, must be what the uncached lookups produced.
        @Test
        public void testNewInstance_topLevelStaticNestedAndInnerClassesRepeatedly() {
            for (int round = 0; round < 3; round++) {
                assertFreshInstances(Object.class, Object.class);
                assertFreshInstances(StringBuilder.class, StringBuilder.class);
                assertFreshInstances(ArrayList.class, ArrayList.class);
                assertFreshInstances(HashMap.class, HashMap.class);
                assertFreshInstances(LinkedHashSet.class, LinkedHashSet.class);
                assertFreshInstances(List.class, ArrayList.class);
                assertFreshInstances(StaticBean.class, StaticBean.class);
                assertFreshInstances(StaticBean.InnerOfStatic.class, StaticBean.InnerOfStatic.class);
                assertFreshInstances(InnerBean.class, InnerBean.class);
                assertFreshInstances(InnerBean.Deeper.class, InnerBean.Deeper.class);
                assertFreshInstances(anonymousBean.getClass(), anonymousBean.getClass());

                assertTrue(((Collection<?>) CommonUtil.newInstance(ArrayList.class)).isEmpty());
                assertEquals(7, CommonUtil.newInstance(StaticBean.class).value);
                assertEquals(11, CommonUtil.newInstance(InnerBean.class).value);

                // a new enclosing chain is built every time: the caller's enclosing object is never reused
                final InnerBean inner = CommonUtil.newInstance(InnerBean.class);
                assertNotSame(this, inner.outer());
                assertNotSame(inner.outer(), CommonUtil.newInstance(InnerBean.class).outer());

                final InnerBean.Deeper deeper = CommonUtil.newInstance(InnerBean.Deeper.class);
                assertNotNull(deeper.outer());
                assertNotSame(this, deeper.outer().outer());
                assertNotNull(CommonUtil.newInstance(StaticBean.InnerOfStatic.class).outer());
                assertEquals("anonymous bean", CommonUtil.newInstance(anonymousBean.getClass()).toString());
            }
        }

        // G012-01: the rejections that follow the cached lookups keep their exception type and message on every call.
        @Test
        public void testNewInstance_rejectedShapesThrowTheSameExceptionOnEveryCall() {
            class LocalBean {
            }

            for (int round = 0; round < 3; round++) {
                final IllegalArgumentException anonymousContainer = assertThrows(IllegalArgumentException.class,
                        () -> CommonUtil.newInstance(anonymousList.getClass()));
                assertEquals("Cannot reflectively instantiate anonymous collection/map class: " + anonymousList.getClass().getName()
                        + ". Use a named container class.", anonymousContainer.getMessage());

                final IllegalArgumentException local = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(LocalBean.class));
                assertTrue(local.getMessage().startsWith("No default constructor found in class: "), local.getMessage());

                final IllegalArgumentException member = assertThrows(IllegalArgumentException.class,
                        () -> CommonUtil.newInstance(InnerWithoutUsableConstructor.class));
                assertTrue(member.getMessage().startsWith("No constructor accepting an enclosing instance found in class: "), member.getMessage());

                final IllegalArgumentException noDefault = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Integer.class));
                assertEquals("No default constructor found in class: java.lang.Integer", noDefault.getMessage());

                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Runnable.class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(int.class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(String[].class));
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance((Class<?>) null));
            }
        }

        // G012-02: an Object[] source is read directly instead of through reflect.Array.get - same elements, same order,
        // same element identity, same destinations.
        @Test
        public void testConvert_objectArraySourceIntoCollections() {
            final com.landawn.abacus.type.Type<List<String>> listOfString = new TypeReference<List<String>>() {
            }.type();
            final String b = new String("b");
            final Object[] mixed = { b, null, "a", 3, "a" };

            final List<String> strings = CommonUtil.convert(mixed, listOfString);
            assertEquals(Arrays.asList("b", null, "a", "3", "a"), strings);
            assertEquals(ArrayList.class, strings.getClass());
            assertSame(b, strings.get(0));

            final String[] typed = { "x", null, "y" };
            final List<String> fromTyped = CommonUtil.convert(typed, listOfString);
            assertEquals(Arrays.asList("x", null, "y"), fromTyped);
            assertSame(typed[0], fromTyped.get(0));
            fromTyped.add("mutable");

            final Set<String> set = CommonUtil.convert(mixed, new TypeReference<Set<String>>() {
            }.type());
            assertEquals(Arrays.asList("b", null, "a", "3"), new ArrayList<>(set));
            assertEquals(LinkedHashSet.class, set.getClass());

            final ImmutableList<Integer> immutable = CommonUtil.convert(new Object[] { "1", 2, null }, new TypeReference<ImmutableList<Integer>>() {
            }.type());
            assertEquals(Arrays.asList(1, 2, null), immutable);

            final Collection<Long> longs = CommonUtil.convert(new Object[] { 1, "2", null }, new TypeReference<Collection<Long>>() {
            }.type());
            assertEquals(Arrays.asList(1L, 2L, null), new ArrayList<>(longs));

            final List<List<Integer>> nested = CommonUtil.convert(new int[][] { { 1, 2 }, null, {} }, new TypeReference<List<List<Integer>>>() {
            }.type());
            assertEquals(Arrays.asList(Arrays.asList(1, 2), null, Collections.emptyList()), nested);

            final List<String> empty = CommonUtil.convert(new Object[0], listOfString);
            assertTrue(empty.isEmpty());
            empty.add("mutable");

            assertSame(mixed, CommonUtil.convert(mixed, Object[].class));
        }

        // G012-02: an array of references is written directly when the converted element is an instance of the component
        // type; element identity, nulls, length and the exact array class are unchanged.
        @Test
        public void testConvert_intoReferenceArrays() {
            final Integer[] integers = CommonUtil.convert(Arrays.asList(1, "2", null, 4L), Integer[].class);
            assertArrayEquals(new Integer[] { 1, 2, null, 4 }, integers);
            assertEquals(Integer[].class, integers.getClass());

            final String first = new String("first");
            final String[] fromObjects = CommonUtil.convert(new Object[] { first, null, 5 }, String[].class);
            assertArrayEquals(new String[] { "first", null, "5" }, fromObjects);
            assertEquals(String[].class, fromObjects.getClass());
            assertSame(first, fromObjects[0]);

            final Number[] numbers = CommonUtil.convert(Arrays.asList(1, 2L, 3.5d), Number[].class);
            assertEquals(Number[].class, numbers.getClass());
            assertEquals(Integer.class, numbers[0].getClass());
            assertEquals(Long.class, numbers[1].getClass());
            assertEquals(Double.class, numbers[2].getClass());

            final Object element = new Object();
            final Object[] objects = CommonUtil.convert(new LinkedList<>(Arrays.asList(element, null)), Object[].class);
            assertEquals(Object[].class, objects.getClass());
            assertSame(element, objects[0]);
            assertNull(objects[1]);

            final String[][] grid = CommonUtil.convert(new Object[] { new Object[] { 1, "a" }, null, Arrays.asList("b") }, String[][].class);
            assertEquals(String[][].class, grid.getClass());
            assertArrayEquals(new String[] { "1", "a" }, grid[0]);
            assertNull(grid[1]);
            assertArrayEquals(new String[] { "b" }, grid[2]);

            final List<String>[] lists = CommonUtil.convert(new Object[] { new Object[] { 1, 2 }, Arrays.asList(3) }, new TypeReference<List<String>[]>() {
            }.type());
            assertEquals(Arrays.asList("1", "2"), lists[0]);
            assertEquals(Arrays.asList("3"), lists[1]);

            assertEquals(0, CommonUtil.convert(new ArrayList<>(), String[].class).length);
            assertEquals(String[].class, CommonUtil.convert(new Object[0], String[].class).getClass());
        }

        // G012-02: primitive arrays keep going through reflect.Array (boxing on read, unboxing and widening on write).
        @Test
        public void testConvert_primitiveArraysUnchanged() {
            assertEquals(Arrays.asList(1, 2, 3), CommonUtil.convert(new int[] { 1, 2, 3 }, new TypeReference<List<Integer>>() {
            }.type()));
            assertArrayEquals(new int[] { 1, 0, 3 }, CommonUtil.convert(Arrays.asList(1L, null, "3"), int[].class));
            assertArrayEquals(new Long[] { 1L, 2L }, CommonUtil.convert(new long[] { 1L, 2L }, Long[].class));
            assertArrayEquals(new double[] { 1.0, 2.5 }, CommonUtil.convert(new Object[] { 1, "2.5" }, double[].class), 0.0);
            assertArrayEquals(new long[] { 4L, 5L }, CommonUtil.convert(new Integer[] { 4, 5 }, long[].class));

            final int[] same = { 9 };
            assertSame(same, CommonUtil.convert(same, int[].class));
        }

        // G012-02: element failures and cycles surface exactly as before, whatever the source or destination kind.
        @Test
        public void testConvert_arrayElementFailuresAndCycles() {
            final com.landawn.abacus.type.Type<List<Integer>> listOfInteger = new TypeReference<List<Integer>>() {
            }.type();

            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new Object[] { "1", "bad" }, listOfInteger));
            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new Object[] { "1", "bad" }, Integer[].class));
            assertThrows(NumberFormatException.class, () -> CommonUtil.convert(Arrays.asList("1", "bad"), Integer[].class));

            final Object[] cyclic = new Object[2];
            cyclic[0] = "a";
            cyclic[1] = cyclic;
            final IllegalArgumentException cycle = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cyclic, String[].class));
            assertEquals("Cyclic container conversion is not supported", cycle.getMessage());
            assertEquals("Cyclic container conversion is not supported",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cyclic, listOfInteger)).getMessage());

            final IllegalArgumentException notBoolean = assertThrows(IllegalArgumentException.class,
                    () -> CommonUtil.convert(new Object[] { 1, 2 }, Boolean[].class));
            assertTrue(notBoolean.getMessage().startsWith("Cannot convert"), notBoolean.getMessage());
        }
    }

    // ---- perf review 2026-10-08 G012 end ----
    // ---- perf review 2026-10-08 G014 begin ----
    @Nested
    class G014 {

        private static final int[] COUNTS_AROUND_OLD_CAP = { 1, 2, 1023, 1024, 1025, 1536, 2000, 2999, 3000, 3001, 5000 };

        private List<Integer> distinctValues(final int size) {
            final List<Integer> values = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                values.add((int) ((i * 7919L) % 3001)); // distinct for size <= 3001, not in ascending order
            }

            return values;
        }

        private Collection<Integer> newCollection(final String kind, final List<Integer> values) {
            switch (kind) {
                case "HashSet":
                    return new HashSet<>(values);
                case "LinkedHashSet":
                    return new LinkedHashSet<>(values);
                case "TreeSet":
                    return new TreeSet<>(values);
                case "ArrayDeque":
                    return new java.util.ArrayDeque<>(values);
                case "PriorityQueue":
                    return new java.util.PriorityQueue<>(values);
                case "ConcurrentLinkedQueue":
                    return new java.util.concurrent.ConcurrentLinkedQueue<>(values);
                case "ArrayBlockingQueue":
                    return new java.util.concurrent.ArrayBlockingQueue<>(Math.max(1, values.size()), false, values);
                default:
                    throw new IllegalArgumentException(kind);
            }
        }

        private <T> List<T> iterationOrder(final Iterable<T> c) {
            final List<T> order = new ArrayList<>();

            for (final T e : c) {
                order.add(e);
            }

            return order;
        }

        /**
         * A Collection that counts the hasNext()/next() calls of its iterators and has no descending iterator.
         */
        private final class CountingCollection<T> extends java.util.AbstractCollection<T> {
            private final List<T> backing;
            final AtomicInteger hasNextCalls = new AtomicInteger();
            final AtomicInteger nextCalls = new AtomicInteger();

            CountingCollection(final List<T> backing) {
                this.backing = backing;
            }

            @Override
            public Iterator<T> iterator() {
                final Iterator<T> iterator = backing.iterator();

                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        hasNextCalls.incrementAndGet();
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        nextCalls.incrementAndGet();
                        return iterator.next();
                    }
                };
            }

            @Override
            public int size() {
                return backing.size();
            }

            void resetCounts() {
                hasNextCalls.set(0);
                nextCalls.set(0);
            }
        }

        // G014-01: firstElements(Iterable, n) on a non-List Collection larger than n sizes its result for exactly n elements;
        // pins the elements, their encounter order and the fresh mutable ArrayList result on both sides of the old 1024 cap.
        @Test
        void testFirstElements_nonListCollectionsAroundOldCapacityCap() {
            final List<Integer> values = distinctValues(3000);

            for (final String kind : new String[] { "HashSet", "LinkedHashSet", "TreeSet", "ArrayDeque", "PriorityQueue", "ConcurrentLinkedQueue",
                    "ArrayBlockingQueue" }) {
                final Collection<Integer> c = newCollection(kind, values);
                final List<Integer> order = iterationOrder(c);
                assertEquals(3000, order.size());

                for (final int n : COUNTS_AROUND_OLD_CAP) {
                    final List<Integer> result = CommonUtil.firstElements(c, n);

                    assertEquals(order.subList(0, Math.min(n, order.size())), result, kind + " n=" + n);
                    assertEquals(ArrayList.class, result.getClass(), kind + " n=" + n);
                    result.add(-1); // a fresh, modifiable list
                    assertEquals(3000, c.size(), kind + " n=" + n);
                }

                assertTrue(CommonUtil.firstElements(c, 0).isEmpty());
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.firstElements(c, -1));
            }

            // a non-Collection Iterable (size unknown) keeps its capped initial capacity and the same result
            final Iterable<Integer> iterable = () -> values.iterator();

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(values.subList(0, Math.min(n, values.size())), CommonUtil.firstElements(iterable, n), "Iterable n=" + n);
            }

            // a Collection that holds nulls (no descending iterator, not a List)
            final List<Integer> withNulls = new ArrayList<>(values);

            for (int i = 0; i < withNulls.size(); i += 5) {
                withNulls.set(i, null);
            }

            final CountingCollection<Integer> counting = new CountingCollection<>(withNulls);

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(withNulls.subList(0, Math.min(n, withNulls.size())), CommonUtil.firstElements(counting, n), "nulls n=" + n);
            }
        }

        // G014-02: lastElements(Iterable, n) on a Collection without a descending iterator (the ArrayDeque window path) sizes the
        // window for exactly n elements; pins elements, order, null elements and the fresh mutable ArrayList result.
        @Test
        void testLastElements_windowPathAroundOldCapacityCap() {
            final List<Integer> values = distinctValues(3000);

            final List<Integer> hashSetValues = new ArrayList<>(values);
            hashSetValues.set(1234, null); // HashSet allows one null element

            final List<Collection<Integer>> sources = new ArrayList<>();
            sources.add(new HashSet<>(hashSetValues));
            sources.add(newCollection("PriorityQueue", values));
            sources.add(newCollection("ConcurrentLinkedQueue", values));
            sources.add(newCollection("ArrayBlockingQueue", values));

            final List<Integer> withNulls = new ArrayList<>(values);

            for (int i = 0; i < withNulls.size(); i += 7) {
                withNulls.set(i, null);
            }

            sources.add(new CountingCollection<>(withNulls));

            for (final Collection<Integer> c : sources) {
                final String kind = c.getClass().getSimpleName();
                final List<Integer> order = iterationOrder(c);
                assertEquals(3000, order.size());

                for (final int n : COUNTS_AROUND_OLD_CAP) {
                    final List<Integer> result = CommonUtil.lastElements(c, n);

                    assertEquals(order.subList(Math.max(0, order.size() - n), order.size()), result, kind + " n=" + n);
                    assertEquals(ArrayList.class, result.getClass(), kind + " n=" + n);
                    result.add(-1); // a fresh, modifiable list
                    assertEquals(3000, c.size(), kind + " n=" + n);
                }

                assertTrue(CommonUtil.lastElements(c, 0).isEmpty());
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.lastElements(c, -1));
            }

            // a non-Collection Iterable (size unknown) keeps its capped initial capacity and the same result
            final Iterable<Integer> iterable = () -> withNulls.iterator();

            for (final int n : COUNTS_AROUND_OLD_CAP) {
                assertEquals(withNulls.subList(Math.max(0, withNulls.size() - n), withNulls.size()), CommonUtil.lastElements(iterable, n), "Iterable n=" + n);
            }
        }

        // G014-01/02: the source iterator is driven exactly as before: firstElements stops right after the n-th next() (no
        // extra hasNext()), lastElements walks the whole collection once; the backing collection is not modified.
        @Test
        void testFirstLastElements_iteratorCallSequenceOnNonListCollection() {
            final List<Integer> values = distinctValues(2500);
            final CountingCollection<Integer> counting = new CountingCollection<>(values);

            for (final int n : new int[] { 1, 1024, 1025, 2000, 2499 }) {
                counting.resetCounts();
                assertEquals(values.subList(0, n), CommonUtil.firstElements(counting, n), "first n=" + n);
                assertEquals(n, counting.hasNextCalls.get(), "first hasNext n=" + n);
                assertEquals(n, counting.nextCalls.get(), "first next n=" + n);

                counting.resetCounts();
                assertEquals(values.subList(values.size() - n, values.size()), CommonUtil.lastElements(counting, n), "last n=" + n);
                assertEquals(values.size() + 1, counting.hasNextCalls.get(), "last hasNext n=" + n);
                assertEquals(values.size(), counting.nextCalls.get(), "last next n=" + n);
            }

            assertEquals(distinctValues(2500), values);
        }
    }

    // ---- perf review 2026-10-08 G014 end ----
    // ---- perf review 2026-10-08 G015 begin ----
    @Nested
    class G015 {

        private final Class<?>[] componentTypes = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
                String.class };

        private Object valueAt(final Class<?> componentType, final int i) {
            if (componentType == boolean.class) {
                return (i * 37 % 5) < 2;
            } else if (componentType == char.class) {
                return (char) (i * 7 + 1);
            } else if (componentType == byte.class) {
                return (byte) (i * 13 + 1);
            } else if (componentType == short.class) {
                return (short) (i * 13 + 1);
            } else if (componentType == int.class) {
                return i * 13 + 1;
            } else if (componentType == long.class) {
                return i * 13L + (1L << 40);
            } else if (componentType == float.class) {
                return i % 7 == 0 ? Float.NaN : (i % 7 == 1 ? -0.0f : i * 0.5f);
            } else if (componentType == double.class) {
                return i % 7 == 0 ? Double.NaN : (i % 7 == 1 ? -0.0d : i * 0.25d);
            } else {
                return i % 4 == 0 ? null : "s" + i;
            }
        }

        private Object newArray(final Class<?> componentType, final int length) {
            final Object a = java.lang.reflect.Array.newInstance(componentType, length);

            for (int i = 0; i < length; i++) {
                java.lang.reflect.Array.set(a, i, valueAt(componentType, i));
            }

            return a;
        }

        private Object copyOf(final Object a) {
            final int length = java.lang.reflect.Array.getLength(a);
            final Object copy = java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), length);
            System.arraycopy(a, 0, copy, 0, length);
            return copy;
        }

        // reference: element i of [fromIndex, toIndex) moves to fromIndex + (i + floorMod(distance, length)) % length
        private Object expectedRotation(final Object original, final int fromIndex, final int toIndex, final int distance) {
            final Object expected = copyOf(original);
            final int length = toIndex - fromIndex;

            if (length > 0) {
                final int shift = Math.floorMod(distance, length);

                for (int i = 0; i < length; i++) {
                    java.lang.reflect.Array.set(expected, fromIndex + (i + shift) % length, java.lang.reflect.Array.get(original, fromIndex + i));
                }
            }

            return expected;
        }

        private void rotate(final Object a, final int fromIndex, final int toIndex, final int distance) {
            if (a instanceof boolean[]) {
                CommonUtil.rotate((boolean[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof char[]) {
                CommonUtil.rotate((char[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof byte[]) {
                CommonUtil.rotate((byte[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof short[]) {
                CommonUtil.rotate((short[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof int[]) {
                CommonUtil.rotate((int[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof long[]) {
                CommonUtil.rotate((long[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof float[]) {
                CommonUtil.rotate((float[]) a, fromIndex, toIndex, distance);
            } else if (a instanceof double[]) {
                CommonUtil.rotate((double[]) a, fromIndex, toIndex, distance);
            } else {
                CommonUtil.rotate((Object[]) a, fromIndex, toIndex, distance);
            }
        }

        private void rotateWhole(final Object a, final int distance) {
            if (a instanceof boolean[]) {
                CommonUtil.rotate((boolean[]) a, distance);
            } else if (a instanceof char[]) {
                CommonUtil.rotate((char[]) a, distance);
            } else if (a instanceof byte[]) {
                CommonUtil.rotate((byte[]) a, distance);
            } else if (a instanceof short[]) {
                CommonUtil.rotate((short[]) a, distance);
            } else if (a instanceof int[]) {
                CommonUtil.rotate((int[]) a, distance);
            } else if (a instanceof long[]) {
                CommonUtil.rotate((long[]) a, distance);
            } else if (a instanceof float[]) {
                CommonUtil.rotate((float[]) a, distance);
            } else if (a instanceof double[]) {
                CommonUtil.rotate((double[]) a, distance);
            } else {
                CommonUtil.rotate((Object[]) a, distance);
            }
        }

        private void assertSameElements(final Object expected, final Object actual, final String message) {
            final int length = java.lang.reflect.Array.getLength(expected);
            assertEquals(length, java.lang.reflect.Array.getLength(actual), message);

            for (int i = 0; i < length; i++) {
                // Float/Double.equals: NaN equals NaN, -0.0 differs from 0.0
                assertEquals(java.lang.reflect.Array.get(expected, i), java.lang.reflect.Array.get(actual, i), message + " at index " + i);
            }
        }

        private void assertRotation(final Object original, final int fromIndex, final int toIndex, final int distance) {
            final Object actual = copyOf(original);
            rotate(actual, fromIndex, toIndex, distance);
            assertSameElements(expectedRotation(original, fromIndex, toIndex, distance), actual, original.getClass().getSimpleName() + " range [" + fromIndex
                    + ", " + toIndex + ") of " + java.lang.reflect.Array.getLength(original) + ", distance " + distance);
        }

        private int[] distancesFor(final int length) {
            final List<Integer> result = new ArrayList<>();

            if (length <= 21) {
                for (int d = -2 * length - 1; d <= 2 * length + 1; d++) {
                    result.add(d);
                }
            } else {
                for (final int d : new int[] { 0, 1, -1, 2, -2, 7, -7, length - 1, length, length + 1, -(length - 1), -length, -(length + 1), length / 2,
                        length / 3 + 1, ((int) (length * 0.618)) | 1, -(((int) (length * 0.618)) | 1), 3 * length + 5 }) {
                    result.add(d);
                }
            }

            result.addAll(Arrays.asList(Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MAX_VALUE, Integer.MAX_VALUE - 1));

            return result.stream().mapToInt(Integer::intValue).toArray();
        }

        // G015-01: rotate(X[], from, to, distance) uses a shift for distance +/-1 and three reversals otherwise (primitive overloads);
        // results must equal the plain cyclic-shift definition for every length (both sides of the bulk-copy threshold), offset and distance.
        @Test
        void testRotate_allArrayTypesMatchReference() {
            final List<Integer> lengths = new ArrayList<>();

            for (int length = 0; length <= 21; length++) {
                lengths.add(length);
            }

            lengths.addAll(Arrays.asList(31, 64, 65, 257));

            for (final Class<?> componentType : componentTypes) {
                for (final int length : lengths) {
                    for (final int prefix : new int[] { 0, 2 }) {
                        for (final int suffix : new int[] { 0, 3 }) {
                            final Object original = newArray(componentType, prefix + length + suffix);

                            for (final int distance : distancesFor(length)) {
                                assertRotation(original, prefix, prefix + length, distance);
                            }
                        }
                    }
                }
            }
        }

        // G015-01: large ranges (where the former cycle-following loop was cache-unfriendly) give the same result.
        @Test
        void testRotate_largeRangesMatchReference() {
            for (final Class<?> componentType : componentTypes) {
                final int length = componentType == int.class ? 10_007 : 4_096;
                final Object original = newArray(componentType, length);

                for (final int distance : new int[] { 1, -1, 2, -3, ((int) (length * 0.618)) | 1, length / 2, length / 3 + 1, length - 1, Integer.MIN_VALUE,
                        Integer.MAX_VALUE }) {
                    assertRotation(original, 0, length, distance);
                    assertRotation(original, 3, length - 2, distance);
                }
            }
        }

        // G015-01: the whole-array overloads, null/empty/singleton arrays and the unchanged range validation.
        @Test
        void testRotate_wholeArrayNullAndInvalidRanges() {
            for (final Class<?> componentType : componentTypes) {
                for (final int length : new int[] { 0, 1, 2, 9, 10, 11, 40 }) {
                    for (final int distance : new int[] { -11, -1, 0, 1, 3, 10, 41 }) {
                        final Object original = newArray(componentType, length);
                        final Object actual = copyOf(original);
                        rotateWhole(actual, distance);
                        assertSameElements(expectedRotation(original, 0, length, distance), actual,
                                componentType + " whole length " + length + ", distance " + distance);
                    }
                }

                final Object a = newArray(componentType, 5);
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, -1, 2, 1));
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, 3, 2, 1));
                assertThrows(IndexOutOfBoundsException.class, () -> rotate(a, 0, 6, 1));
                assertSameElements(newArray(componentType, 5), a, "unchanged after failed validation");
            }

            CommonUtil.rotate((boolean[]) null, 1);
            CommonUtil.rotate((char[]) null, 1);
            CommonUtil.rotate((byte[]) null, 1);
            CommonUtil.rotate((short[]) null, 1);
            CommonUtil.rotate((int[]) null, 1);
            CommonUtil.rotate((long[]) null, 1);
            CommonUtil.rotate((float[]) null, 1);
            CommonUtil.rotate((double[]) null, 1);
            CommonUtil.rotate((int[]) null, 0, 0, 7);
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate((int[]) null, 0, 1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.rotate((byte[]) null, 0, 1, 1));
        }

        // G015-01: the primitive lists rotate their used range [0, size) of a larger backing array through N.rotate.
        @Test
        void testRotate_primitiveListsWithSpareCapacity() {
            for (final int size : new int[] { 2, 9, 10, 37, 1000 }) {
                for (final int distance : new int[] { 1, -1, 4, -(size / 3 + 1), size + 2 }) {
                    final IntList intList = new IntList(size * 2 + 5);
                    final ByteList byteList = new ByteList(size * 2 + 5);
                    final int[] expected = new int[size];

                    for (int i = 0; i < size; i++) {
                        intList.add(i * 3 + 1);
                        byteList.add((byte) (i * 3 + 1));
                        expected[Math.floorMod(i + distance, size)] = i * 3 + 1;
                    }

                    intList.rotate(distance);
                    byteList.rotate(distance);

                    assertArrayEquals(expected, intList.toArray(), "IntList size " + size + ", distance " + distance);

                    final byte[] expectedBytes = new byte[size];

                    for (int i = 0; i < size; i++) {
                        expectedBytes[i] = (byte) expected[i];
                    }

                    assertArrayEquals(expectedBytes, byteList.toArray(), "ByteList size " + size + ", distance " + distance);
                }
            }
        }
    }

    // ---- perf review 2026-10-08 G076 end ----
    // ---- perf review 2026-10-08 G118 begin ----
    @Nested
    class G118 {
        private static final String G118_SAME_COLUMNS_MESSAGE = "These two Datasets do not have the same column names: ";

        private Dataset g118Table(final List<String> columnNames, final Object[]... rows) {
            return CommonUtil.newDataset(columnNames, Arrays.asList(rows));
        }

        private String g118MergeFailure(final Dataset... datasets) {
            return assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(datasets), true)).getMessage();
        }

        /**
         * Asserts that {@code actual} is what {@code java.lang.reflect.Array.get} gives: the same wrapper class and the same value,
         * with floating-point values compared by their raw bits (NaN payloads, -0.0).
         */
        private void g118AssertSameValue(final Object expected, final Object actual) {
            if (expected == null) {
                assertNull(actual);
                return;
            }

            assertNotNull(actual);
            assertEquals(expected.getClass(), actual.getClass());

            if (expected instanceof Double) {
                assertEquals(Double.doubleToRawLongBits((Double) expected), Double.doubleToRawLongBits((Double) actual));
            } else if (expected instanceof Float) {
                assertEquals(Float.floatToRawIntBits((Float) expected), Float.floatToRawIntBits((Float) actual));
            } else {
                assertEquals(expected, actual);
            }
        }

        /**
         * One row of length 7 per primitive array class, with the boundary values of the box caches and of the type.
         */
        private Object[] g118EdgeRows() {
            return new Object[] { new int[] { Integer.MIN_VALUE, -129, -128, 0, 127, 128, Integer.MAX_VALUE },
                    new long[] { Long.MIN_VALUE, -129, -128, 0, 127, 128, Long.MAX_VALUE },
                    new double[] { Double.NaN, Double.longBitsToDouble(0x7ff8000000000123L), -0.0, 0.0, Double.MIN_VALUE, Double.NEGATIVE_INFINITY,
                            Double.MAX_VALUE },
                    new boolean[] { true, false, true, true, false, false, true },
                    new float[] { Float.NaN, Float.intBitsToFloat(0x7fc00123), -0.0f, 0.0f, Float.MIN_VALUE, Float.POSITIVE_INFINITY, Float.MAX_VALUE },
                    new char[] { 0, 'a', 127, 128, (char) 0xD800, (char) 0xDFFF, (char) 0xFFFF }, new byte[] { Byte.MIN_VALUE, -1, 0, 1, 2, 3, Byte.MAX_VALUE },
                    new short[] { Short.MIN_VALUE, -129, -128, 0, 127, 128, Short.MAX_VALUE } };
        }

        private Object g118RandomPrimitiveArray(final Random random, final int kind, final int length) {
            final Object array;

            switch (kind) {
                case 0:
                    array = new int[length];
                    break;
                case 1:
                    array = new long[length];
                    break;
                case 2:
                    array = new double[length];
                    break;
                case 3:
                    array = new boolean[length];
                    break;
                case 4:
                    array = new float[length];
                    break;
                case 5:
                    array = new char[length];
                    break;
                case 6:
                    array = new byte[length];
                    break;
                default:
                    array = new short[length];
                    break;
            }

            for (int i = 0; i < length; i++) {
                final long bits = random.nextInt(4) == 0 ? random.nextInt(300) - 150 : random.nextLong();

                if (array instanceof final int[] a) {
                    a[i] = (int) bits;
                } else if (array instanceof final long[] a) {
                    a[i] = bits;
                } else if (array instanceof final double[] a) {
                    a[i] = random.nextInt(5) == 0 ? Double.longBitsToDouble(bits) : bits / 7.0;
                } else if (array instanceof final boolean[] a) {
                    a[i] = (bits & 1) == 0;
                } else if (array instanceof final float[] a) {
                    a[i] = random.nextInt(5) == 0 ? Float.intBitsToFloat((int) bits) : bits / 7.0f;
                } else if (array instanceof final char[] a) {
                    a[i] = (char) bits;
                } else if (array instanceof final byte[] a) {
                    a[i] = (byte) bits;
                } else {
                    ((short[]) array)[i] = (short) bits;
                }
            }

            return array;
        }

        // G118-01: merge(Collection, true) compares the column-name sets through the datasets' column index maps
        @Test
        void testMerge_requiresSameColumnsAcceptsReorderedSetsAndRejectsOthers() {
            final Dataset ab = g118Table(Arrays.asList("a", "b"), new Object[] { 1, 2 }, new Object[] { 3, 4 });
            final Dataset ba = g118Table(Arrays.asList("b", "a"), new Object[] { 20, 10 });
            final Dataset ab2 = g118Table(Arrays.asList("a", "b"), new Object[] { 5, 6 });

            final Dataset two = CommonUtil.merge(Arrays.asList(ab, ba), true);
            assertEquals(Arrays.asList("a", "b"), two.columnNames());
            assertEquals(Arrays.asList(1, 3, 10), two.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, 20), two.getColumn("b"));

            final Dataset three = CommonUtil.merge(Arrays.asList(ab, ba, ab2), true);
            assertEquals(Arrays.asList("a", "b"), three.columnNames());
            assertEquals(Arrays.asList(1, 3, 10, 5), three.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, 20, 6), three.getColumn("b"));

            final Dataset ac = g118Table(Arrays.asList("a", "c"), new Object[] { 7, 8 });
            final Dataset a = g118Table(Arrays.asList("a"), new Object[] { 9 });
            final Dataset abc = g118Table(Arrays.asList("a", "b", "c"), new Object[] { 1, 2, 3 });

            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a, c]", g118MergeFailure(ab, ac));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a]", g118MergeFailure(ab, ba, a));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b], [a, b, c]", g118MergeFailure(ab, abc, ba));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a], [a, b]", g118MergeFailure(a, ab));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, c], [a, b]", g118MergeFailure(abc, ab, ab2));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[b, a], [a, c]", g118MergeFailure(ba, ab, ac));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, c], [a, b]", g118MergeFailure(ac, ac, ab));
            // only the first name of each list differs
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[c, a], [b, a]", g118MergeFailure(g118Table(Arrays.asList("c", "a"), new Object[] { 1, 2 }), ba));

            // zero-column datasets
            assertEquals(0, CommonUtil.merge(Arrays.asList(CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset()), true).columnCount());
            assertEquals(0, CommonUtil.merge(Arrays.asList(CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset(), CommonUtil.newEmptyDataset()), true)
                    .columnCount());
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[], [a]", g118MergeFailure(CommonUtil.newEmptyDataset(), a));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a], []", g118MergeFailure(a, CommonUtil.newEmptyDataset(), a));

            // requiresSameColumns == false is not checked; a null element is still rejected first
            final Dataset union = CommonUtil.merge(Arrays.asList(ab, ac, a), false);
            assertEquals(Arrays.asList("a", "b", "c"), union.columnNames());
            assertEquals(Arrays.asList(1, 3, 7, 9), union.getColumn("a"));
            assertEquals(Arrays.asList(2, 4, null, null), union.getColumn("b"));
            assertEquals(Arrays.asList(null, null, 8, null), union.getColumn("c"));
            assertEquals("null element is found in datasets",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ab, null), true)).getMessage());
            assertEquals("null element is found in datasets",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ab, ac, null), true)).getMessage());

            // the inputs are not changed
            assertEquals(Arrays.asList("a", "b"), ab.columnNames());
            assertEquals(2, ab.size());
            assertEquals(Arrays.asList("b", "a"), ba.columnNames());
            assertEquals(1, ba.size());
        }

        // G118-01: the check follows every column rename/add/swap/move/remove, and wide schemas in any order
        @Test
        void testMerge_requiresSameColumnsFollowsColumnChangesAndWideSchemas() {
            final Dataset ds = g118Table(Arrays.asList("a", "b", "c"), new Object[] { 1, 2, 3 });
            final Dataset other = g118Table(Arrays.asList("c", "b", "a"), new Object[] { 30, 20, 10 });
            assertEquals(2, CommonUtil.merge(Arrays.asList(ds, other), true).size()); // both column index maps exist from here on

            ds.renameColumn("c", "x");
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, x], [c, b, a]", g118MergeFailure(ds, other));
            other.renameColumn("c", "x");
            Dataset merged = CommonUtil.merge(Arrays.asList(ds, other, ds), true);
            assertEquals(Arrays.asList("a", "b", "x"), merged.columnNames());
            assertEquals(Arrays.asList(1, 10, 1), merged.getColumn("a"));
            assertEquals(Arrays.asList(3, 30, 3), merged.getColumn("x"));

            ds.addColumn("d", Arrays.asList(4));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[a, b, x, d], [x, b, a]", g118MergeFailure(ds, other));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[x, b, a], [a, b, x, d]", g118MergeFailure(other, ds));
            other.addColumn(0, "d", Arrays.asList(40));
            merged = CommonUtil.merge(Arrays.asList(ds, other), true);
            assertEquals(Arrays.asList(4, 40), merged.getColumn("d"));

            ds.swapColumns("a", "d");
            ds.moveColumn("b", 0);
            assertEquals(Arrays.asList("b", "d", "x", "a"), ds.columnNames());
            merged = CommonUtil.merge(Arrays.asList(other, ds, other), true);
            assertEquals(Arrays.asList("d", "x", "b", "a"), merged.columnNames());
            assertEquals(Arrays.asList(40, 4, 40), merged.getColumn("d"));
            assertEquals(Arrays.asList(10, 1, 10), merged.getColumn("a"));

            ds.removeColumn("x");
            assertEquals(G118_SAME_COLUMNS_MESSAGE + "[b, d, a], [d, x, b, a]", g118MergeFailure(ds, other));
            other.removeColumn("x");
            assertEquals(Arrays.asList(20, 2), CommonUtil.merge(Arrays.asList(other, ds), true).getColumn("b"));

            // 300 columns, the other dataset in reverse order
            final List<String> names = new ArrayList<>();
            final Object[] row = new Object[300];

            for (int i = 0; i < 300; i++) {
                names.add("col" + i);
                row[i] = i;
            }

            final List<String> reversedNames = new ArrayList<>(names);
            Collections.reverse(reversedNames);
            final Object[] reversedRow = new Object[300];

            for (int i = 0; i < 300; i++) {
                reversedRow[i] = 1000 + 299 - i;
            }

            final Dataset wide = g118Table(names, row);
            final Dataset wideReversed = g118Table(reversedNames, reversedRow);
            merged = CommonUtil.merge(Arrays.asList(wide, wideReversed, wide), true);
            assertEquals(names, merged.columnNames());

            for (int i = 0; i < 300; i++) {
                assertEquals(Arrays.asList(i, 1000 + i, i), merged.getColumn("col" + i));
            }

            final List<String> renamed = new ArrayList<>(reversedNames);
            renamed.set(150, "other");
            final Dataset wideRenamed = g118Table(renamed, reversedRow);
            assertEquals(G118_SAME_COLUMNS_MESSAGE + names + ", " + renamed, g118MergeFailure(wide, wideReversed, wideRenamed));
            assertEquals(G118_SAME_COLUMNS_MESSAGE + renamed + ", " + names, g118MergeFailure(wideRenamed, wide));

            final List<String> longer = new ArrayList<>(names);
            longer.add("col300");
            final Object[] longerRow = Arrays.copyOf(row, 301);
            assertEquals(G118_SAME_COLUMNS_MESSAGE + names + ", " + longer, g118MergeFailure(wide, g118Table(longer, longerRow)));
        }

        // G118-01: a slice invalidated by its parent still fails with ConcurrentModificationException in any position
        @Test
        void testMerge_requiresSameColumnsInvalidatedSliceStillFailsFast() {
            final Dataset parent = g118Table(Arrays.asList("a", "b"), new Object[] { 1, 2 }, new Object[] { 3, 4 });
            final Dataset slice = parent.slice(0, 1);
            final Dataset other = g118Table(Arrays.asList("b", "a"), new Object[] { 20, 10 });
            assertEquals(3, CommonUtil.merge(Arrays.asList(other, slice, other), true).size());

            parent.addRow(new Object[] { 5, 6 });

            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(other, slice), true));
            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(slice, other), true));
            assertThrows(java.util.ConcurrentModificationException.class, () -> CommonUtil.merge(Arrays.asList(other, other, slice), true));
            assertEquals(1, other.size());
        }

        // G118-02: newDataset reads primitive-array rows with typed loads: same cell values and wrapper classes as Array.get
        @Test
        void testNewDataset_primitiveArrayRowsMatchReflectiveRead() {
            final List<String> columnNames = Arrays.asList("c0", "c1", "c2", "c3", "c4", "c5", "c6");

            for (final Object row : g118EdgeRows()) {
                final Object copy = java.lang.reflect.Array.newInstance(row.getClass().getComponentType(), 7);
                System.arraycopy(row, 0, copy, 0, 7);
                final Dataset ds = CommonUtil.newDataset(columnNames, Arrays.asList(row, null, copy));
                assertEquals(columnNames, ds.columnNames());
                assertEquals(3, ds.size());

                for (int c = 0; c < 7; c++) {
                    final Object expected = java.lang.reflect.Array.get(row, c);
                    g118AssertSameValue(expected, ds.get(0, c));
                    assertNull((Object) ds.get(1, c));
                    g118AssertSameValue(expected, ds.get(2, c));
                }

                // the overload with properties takes the same path
                final Map<String, Object> properties = new HashMap<>();
                properties.put("p", 1);
                final Dataset withProperties = CommonUtil.newDataset(columnNames, Arrays.asList(row), properties);
                assertEquals(properties, withProperties.getProperties());

                for (int c = 0; c < 7; c++) {
                    g118AssertSameValue(java.lang.reflect.Array.get(row, c), withProperties.get(0, c));
                }
            }

            // a single column unpacks a one-element primitive array
            final Dataset single = CommonUtil.newDataset(Arrays.asList("a"),
                    Arrays.asList(new int[] { 5 }, new int[] { 300 }, new char[] { 'z' }, new double[] { -0.0 }));
            g118AssertSameValue(5, single.get(0, 0));
            g118AssertSameValue(300, single.get(1, 0));
            g118AssertSameValue('z', single.get(2, 0));
            g118AssertSameValue(-0.0, single.get(3, 0));

            // random rows of every primitive class, alternating classes from row to row
            final Random random = new Random(118);
            final List<String> six = Arrays.asList("a", "b", "c", "d", "e", "f");
            final List<Object> rows = new ArrayList<>();

            for (int i = 0; i < 400; i++) {
                rows.add(g118RandomPrimitiveArray(random, random.nextInt(3) == 0 ? random.nextInt(8) : i % 8, 6));
            }

            final Dataset randomDataset = CommonUtil.newDataset(six, rows);
            assertEquals(400, randomDataset.size());

            for (int r = 0; r < 400; r++) {
                for (int c = 0; c < 6; c++) {
                    g118AssertSameValue(java.lang.reflect.Array.get(rows.get(r), c), randomDataset.get(r, c));
                }
            }
        }

        // G118-02: mixed row kinds keep their cells; a wrong-length primitive row still throws the same IAE before any Dataset is built
        @Test
        void testNewDataset_mixedRowKindsAndLengthMismatchUnchanged() {
            final List<String> columnNames = Arrays.asList("a", "b");
            final Map<String, Object> mapRow = new LinkedHashMap<>();
            mapRow.put("b", "mb");
            mapRow.put("a", "ma");
            final List<Object> rows = Arrays.asList(new int[] { 1, 1000 }, new Object[] { "x", null }, new long[] { 2L, -2L }, null, Arrays.asList("l1", 7),
                    mapRow, new boolean[] { true, false }, new Integer[] { 3, 4 }, new double[] { 0.5, Double.NaN }, new short[] { 9, -9 });
            final Dataset ds = CommonUtil.newDataset(columnNames, rows);
            assertEquals(10, ds.size());
            assertEquals(Arrays.asList(1, "x", 2L, null, "l1", "ma", true, 3, 0.5, (short) 9), ds.getColumn("a"));
            assertEquals(Arrays.asList(1000, null, -2L, null, 7, "mb", false, 4, Double.NaN, (short) -9), ds.getColumn("b"));
            g118AssertSameValue(1000, ds.get(0, 1));
            g118AssertSameValue(-2L, ds.get(2, 1));
            g118AssertSameValue((short) -9, ds.get(9, 1));

            assertEquals("The length of row array (3) does not match the length of column name list (2)", assertThrows(IllegalArgumentException.class,
                    () -> CommonUtil.newDataset(columnNames, Arrays.asList(new int[] { 1, 2 }, new int[] { 1, 2, 3 }))).getMessage());
            assertEquals("The length of row array (1) does not match the length of column name list (2)", assertThrows(IllegalArgumentException.class,
                    () -> CommonUtil.newDataset(columnNames, Arrays.asList(new long[] { 1, 2 }, new double[] { 1 }))).getMessage());
            assertEquals("The length of row array (2) does not match the length of column name list (1)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a"), Arrays.asList(new long[] { 1, 2 })))
                            .getMessage());
            assertEquals("The length of row array (0) does not match the length of column name list (1)",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a"), Arrays.asList(new boolean[0]))).getMessage());
            assertEquals("The length of row array (3) does not match the length of column name list (2)", assertThrows(IllegalArgumentException.class,
                    () -> CommonUtil.newDataset(columnNames, Arrays.asList(new Object[] { 1, 2 }, new char[] { 'a', 'b', 'c' }))).getMessage());
        }

        // G118-03: convertStructure reads primitive-array sources with typed loads: same element values and classes as Array.get
        @Test
        void testConvert_primitiveArraySourcesMatchReflectiveRead() {
            final com.landawn.abacus.type.Type<List<Object>> listOfObject = com.landawn.abacus.type.Type.of("List<Object>");
            final Random random = new Random(1118);
            final List<Object> sources = new ArrayList<>(Arrays.asList(g118EdgeRows()));

            for (int i = 0; i < 200; i++) {
                sources.add(g118RandomPrimitiveArray(random, i % 8, random.nextInt(6)));
            }

            for (final Object source : sources) {
                final int length = java.lang.reflect.Array.getLength(source);
                final List<Object> list = CommonUtil.convert(source, listOfObject);
                final Object[] objects = CommonUtil.convert(source, Object[].class);
                final List<?> rawList = CommonUtil.convert(source, List.class);
                assertEquals(ArrayList.class, list.getClass());
                assertEquals(length, list.size());
                assertEquals(length, objects.length);
                assertEquals(length, rawList.size());

                for (int i = 0; i < length; i++) {
                    final Object expected = java.lang.reflect.Array.get(source, i);
                    g118AssertSameValue(expected, list.get(i));
                    g118AssertSameValue(expected, objects[i]);
                    g118AssertSameValue(expected, rawList.get(i));
                }
            }

            assertEquals(Arrays.asList("1", "-2"), CommonUtil.convert(new int[] { 1, -2 }, com.landawn.abacus.type.Type.of("List<String>")));
            assertEquals(Arrays.asList(true, false), CommonUtil.convert(new int[] { 1, 0 }, com.landawn.abacus.type.Type.of("List<Boolean>")));
            assertEquals(Arrays.asList(1L, 300L), CommonUtil.convert(new int[] { 1, 300 }, com.landawn.abacus.type.Type.of("List<Long>")));
            assertEquals(Arrays.asList(97), CommonUtil.convert(new char[] { 'a' }, com.landawn.abacus.type.Type.of("List<Integer>")));
            assertEquals(new ArrayList<>(Arrays.asList(3, 1)),
                    new ArrayList<>(CommonUtil.convert(new int[] { 3, 1, 3 }, com.landawn.abacus.type.Type.of("Set<Integer>"))));
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2, 3)),
                    CommonUtil.convert(new int[][] { { 1 }, { 2, 3 } }, com.landawn.abacus.type.Type.of("List<List<Integer>>")));
            assertArrayEquals(new Object[] { (short) 1, (short) -1 }, CommonUtil.convert(new short[] { 1, -1 }, Object[].class));
            assertArrayEquals(new Number[] { 1.5f }, CommonUtil.convert(new float[] { 1.5f }, Number[].class));
            assertArrayEquals(new String[] { "a", "b" }, CommonUtil.convert(new char[] { 'a', 'b' }, String[].class));
            final Double[] widened = CommonUtil.convert(new float[] { Float.intBitsToFloat(0x7fc00123) }, Double[].class);
            assertEquals(0x7ff8000000000000L, Double.doubleToRawLongBits(widened[0]));
        }

        // G118-03: primitive-array targets store exact-wrapper results with typed stores: same arrays, same exceptions at the same element
        @Test
        void testConvert_primitiveArrayTargetsAndFailuresUnchanged() {
            assertArrayEquals(new int[] { 1, 0, 3 }, CommonUtil.convert(Arrays.asList(1, null, 3), int[].class));
            assertArrayEquals(new int[] { 1, 2, 3, 99 }, CommonUtil.convert(Arrays.asList((short) 1, 2L, "3", 'c'), int[].class));
            assertArrayEquals(new int[] { 7 }, CommonUtil.convert(new long[] { 7 }, int[].class));
            assertArrayEquals(new int[] { 1 }, CommonUtil.convert(new double[] { 1.5 }, int[].class));
            assertArrayEquals(new char[] { 'A' }, CommonUtil.convert(new int[] { 65 }, char[].class));
            assertArrayEquals(new int[] { 97 }, CommonUtil.convert(new char[] { 'a' }, int[].class));
            assertArrayEquals(new boolean[] { true, false }, CommonUtil.convert(new int[] { 1, 0 }, boolean[].class));
            assertArrayEquals(new long[] { 1, 2 }, CommonUtil.convert(Arrays.asList(1, 2), long[].class));
            assertArrayEquals(new double[] { 1.5 }, CommonUtil.convert(Arrays.asList(1.5f), double[].class));
            assertArrayEquals(new byte[] { -1, 5 }, CommonUtil.convert(new Object[] { (byte) -1, "5" }, byte[].class));
            assertArrayEquals(new short[] { -300, 7 }, CommonUtil.convert(new Integer[] { -300, 7 }, short[].class));
            assertArrayEquals(new float[] { 1.25f, Float.POSITIVE_INFINITY }, CommonUtil.convert(new double[] { 1.25, 1e300 }, float[].class));

            final double[] doubles = CommonUtil.convert(Arrays.asList(Double.longBitsToDouble(0x7ff8000000000123L), -0.0), double[].class);
            assertEquals(0x7ff8000000000123L, Double.doubleToRawLongBits(doubles[0]));
            assertEquals(0x8000000000000000L, Double.doubleToRawLongBits(doubles[1]));
            final float[] floats = CommonUtil.convert(Arrays.asList(Float.intBitsToFloat(0x7fc00123), -0.0f), float[].class);
            assertEquals(0x7fc00123, Float.floatToRawIntBits(floats[0]));
            assertEquals(0x80000000, Float.floatToRawIntBits(floats[1]));

            final int[][] nested = CommonUtil.convert(Arrays.asList(new Integer[] { 1, 2 }, null), int[][].class);
            assertArrayEquals(new int[] { 1, 2 }, nested[0]);
            assertNull(nested[1]);

            final int[] same = { 1 };
            assertSame(same, CommonUtil.convert(same, int[].class));

            // random Integer/Long/Double lists into primitive targets: the same values a per-element conversion gives
            final Random random = new Random(2118);

            for (int i = 0; i < 200; i++) {
                final List<Integer> values = new ArrayList<>();

                for (int j = 0, n = random.nextInt(8); j < n; j++) {
                    values.add(random.nextInt(4) == 0 ? null : random.nextInt());
                }

                final int[] ints = CommonUtil.convert(values, int[].class);
                final long[] longs = CommonUtil.convert(values, long[].class);
                assertEquals(values.size(), ints.length);

                for (int j = 0; j < ints.length; j++) {
                    assertEquals(values.get(j) == null ? 0 : values.get(j), ints[j]);
                    assertEquals(values.get(j) == null ? 0L : (long) values.get(j), longs[j]);
                }
            }

            final NumberFormatException listFailure = assertThrows(NumberFormatException.class,
                    () -> CommonUtil.convert(Arrays.asList("1", "abc"), int[].class));
            assertEquals("abc is not a valid Integer.", listFailure.getMessage());
            assertEquals("x is not a valid Integer.",
                    assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new String[] { "1", "x" }, int[].class)).getMessage());
            assertEquals("true is not a valid Integer.",
                    assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new boolean[] { true }, int[].class)).getMessage());
            assertEquals("int overflow: 9223372036854775807",
                    assertThrows(ArithmeticException.class, () -> CommonUtil.convert(new long[] { 1, Long.MAX_VALUE }, int[].class)).getMessage());
            assertEquals("Cannot convert Integer value 2 to boolean: only 1 (true), 0 (false) and -1 (false) are accepted",
                    assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new int[] { 1, 2 }, boolean[].class)).getMessage());
            assertEquals("No public static method or constructor found in java.lang.Number to create a value from String",
                    assertThrows(UnsupportedOperationException.class, () -> CommonUtil.convert(new Object[] { "a" }, Number[].class)).getMessage());
        }
    }
}
