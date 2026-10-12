package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.concat;
import static com.landawn.abacus.util.Strings.concatNullToEmpty;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.joinEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/** Strings join contracts, including regression cases. */
public class StringsJoinTest extends StringsTestSupport {

    private static Map<String, Integer> toIntMap() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        return map;
    }




    /** A Collection whose {@code size()} disagrees with its iterator: the size is only a capacity hint. */
    private static final class LyingSizeCollection extends AbstractCollection<String> {
        private final List<String> elements;
        private final int reportedSize;

        LyingSizeCollection(final List<String> elements, final int reportedSize) {
            this.elements = elements;
            this.reportedSize = reportedSize;
        }

        @Override
        public Iterator<String> iterator() {
            return elements.iterator();
        }

        @Override
        public int size() {
            return reportedSize;
        }
    }



    /** A plain {@code Iterable} that is not a {@code Collection}, so no size is available. */
    private static Iterable<String> iterableOf(final List<String> elements) {
        return elements::iterator;
    }

    protected static int capacityReference(final int elementCount, final long estimatedElementLength, final long delimiterLength, final int prefixLength,
            final int suffixLength) {
        final long maximumCapacity = 1_048_576;
        final int actualElementCount = Math.max(0, elementCount);
        final int delimiterCount = Math.max(0, actualElementCount - 1);
        long result = (long) Math.max(0, prefixLength) + Math.max(0, suffixLength);

        if (result >= maximumCapacity) {
            return (int) maximumCapacity;
        }

        final long actualElementLength = Math.max(0, estimatedElementLength);

        if (actualElementCount > 0 && actualElementLength > (maximumCapacity - result) / actualElementCount) {
            return (int) maximumCapacity;
        }

        result += actualElementCount * actualElementLength;
        final long actualDelimiterLength = Math.max(0, delimiterLength);

        if (delimiterCount > 0 && actualDelimiterLength > (maximumCapacity - result) / delimiterCount) {
            return (int) maximumCapacity;
        }

        return (int) (result + delimiterCount * actualDelimiterLength);
    }

    // concat

    @Test
    public void testConcat() {
        assertEquals("abcdef", Strings.concat("abc", "def"));
        assertEquals("HelloWorld", concat("Hello", "World"));
        assertEquals("Hello", concat("Hello", ""));
        assertEquals("World", concat("", "World"));
        assertEquals("", concat("", ""));
        assertEquals("ab", concat("a", "b"));

        assertEquals("abc", Strings.concat("a", "b", "c"));
        assertEquals("HelloWorldTest", concat("Hello", "World", "Test"));
        assertEquals("HelloWorld", concat("Hello", "World", ""));
        assertEquals("HelloTest", concat("Hello", "", "Test"));

        assertEquals("Hello World!!", Strings.concat("Hello", " ", "World", "!", "!"));
        assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        assertEquals("AC", Strings.concat("A", "", "", "", "C"));
        assertEquals("abcde", concat("a", "b", "c", "d", "e"));

        assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        assertEquals("123456", Strings.concat("1", "2", "3", "4", "5", "6"));
        assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));

        assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        assertEquals("abcdefghi", concat("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", "9", "!", "!"));
    }

    @Test
    public void testConcat_Objects() {
        assertEquals("Hello123", Strings.concat("Hello", 123));
        assertEquals("42 is the answer", Strings.concat(42, " is the answer"));
        assertEquals("3.14true", Strings.concat(3.14, true));
        assertEquals("12", concat(1, 2));
        assertEquals("true42", concat(true, 42));
        assertEquals("1null3", concat(1, null, 3));
        assertEquals("truenull", Strings.concat(true, null));

        assertEquals("Hello World", Strings.concat("Hello", " ", "World"));
        assertEquals("123", Strings.concat(1, 2, 3));
        assertEquals("Value: 42!", Strings.concat("Value: ", 42, "!"));

        assertEquals("ABCD", Strings.concat("A", "B", "C", "D"));
        assertEquals("1+2=3", Strings.concat(1, "+", 2, "=3"));
        assertEquals("Result: 10 out of 20", Strings.concat("Result: ", 10, " out of ", 20));

        assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        assertEquals("1 2 3", Strings.concat(1, " ", 2, " ", 3));
        assertEquals("Sum of 1+2=3", Strings.concat("Sum of ", 1, "+", 2, "=3"));

        assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        assertEquals("123456", Strings.concat(1, 2, 3, 4, 5, 6));
        assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        assertEquals("1234567", Strings.concat(1, 2, 3, 4, 5, 6, 7));
        assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        assertEquals("12345678", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8));
        assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        assertEquals("123456789", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    public void testC566_concatContract() {
        assertEquals("nullnull", Strings.concat((Object) null, (Object) null));
        assertEquals("[1, 2]x", Strings.concat(new int[] { 1, 2 }, "x"));
        assertEquals("b", Strings.concatNullToEmpty(null, "b"));
        assertEquals("", Strings.concatNullToEmpty(null, null));
        assertEquals("ab", Strings.concatNullToEmpty("a", "b"));
    }

    @Test
    public void testConcatAndCapacityOptimizations_AllDispatchAndOverflowBoundaries() {
        final String[] alphabet = { null, "", "a", "\uD83D\uDE00", "xyz" };

        for (int length = 0; length <= 32; length++) {
            final String[] values = new String[length];
            for (int i = 0; i < length; i++) {
                values[i] = alphabet[(length * 3 + i) % alphabet.length];
            }
            assertEquals(concatNullToEmptyReference(values), Strings.concatNullToEmpty(values), "length=" + length);
        }

        final int[] counts = { Integer.MIN_VALUE, -1, 0, 1, 2, 17, Integer.MAX_VALUE };
        final long[] lengths = { Long.MIN_VALUE, -1, 0, 1, 31, 1_048_576, Integer.MAX_VALUE, Long.MAX_VALUE };
        final int[] affixLengths = { Integer.MIN_VALUE, -1, 0, 1, 31, 1_048_576, Integer.MAX_VALUE };

        for (final int count : counts) {
            for (final long elementLength : lengths) {
                for (final long delimiterLength : lengths) {
                    for (final int prefixLength : affixLengths) {
                        for (final int suffixLength : new int[] { -1, 0, 7, Integer.MAX_VALUE }) {
                            assertEquals(capacityReference(count, elementLength, delimiterLength, prefixLength, suffixLength),
                                    Strings.calculateBufferSize(count, elementLength, delimiterLength, prefixLength, suffixLength));
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testConcatNullToEmpty() {
        assertEquals("", Strings.concatNullToEmpty());
        assertEquals("A", Strings.concatNullToEmpty("A"));
        assertEquals("abc", Strings.concatNullToEmpty("abc", null));
        assertEquals("def", Strings.concatNullToEmpty(null, "def"));
        assertEquals("", Strings.concatNullToEmpty((String[]) null));
        assertEquals("a", Strings.concatNullToEmpty("a", null, ""));
        assertEquals("", Strings.concatNullToEmpty(null, null));
        assertEquals("Hello", concatNullToEmpty("Hello", null));
        assertEquals("World", concatNullToEmpty(null, "World"));
        assertEquals("Hello", concatNullToEmpty("Hello", null, null));
        assertEquals("", concatNullToEmpty(null, null, null));
        assertEquals("123", Strings.concatNullToEmpty("1", null, "2", null, "3"));
        assertEquals("", Strings.concatNullToEmpty(null, null, null, null, null));
        assertEquals("", Strings.concatNullToEmpty(null, null, null, null, null, null));
        assertEquals("abc", concatNullToEmpty("a", null, "b", null, "c"));
        assertEquals("", concatNullToEmpty(new String[] {}));
        assertEquals("", concatNullToEmpty((String[]) null));
        assertEquals("b", concatNullToEmpty(null, "b"));

        assertEquals("Hello World", Strings.concatNullToEmpty(new String[] { "Hello", " ", "World" }));
        assertEquals("ABCD", Strings.concatNullToEmpty(new String[] { "A", "B", "C", "D" }));
        assertEquals("HelloWorld", Strings.concatNullToEmpty(new String[] { "Hello", null, "World" }));
        assertEquals("", Strings.concatNullToEmpty(new String[] { null, null }));
        assertEquals("", Strings.concatNullToEmpty(new String[0]));
        assertEquals("A", Strings.concatNullToEmpty(new String[] { "A" }));
        assertEquals("AB", Strings.concatNullToEmpty(new String[] { "A", "B" }));
        assertEquals("ABC", Strings.concatNullToEmpty(new String[] { "A", "B", "C" }));

        final String[] largeArray = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        assertEquals("ABCDEFGHIJ", Strings.concatNullToEmpty(largeArray));
        final String[] mixedArray = { "A", null, "B", null, "C", "D", "E", "F", "G", "H" };
        assertEquals("ABCDEFGH", Strings.concatNullToEmpty(mixedArray));

        for (int i = 2; i <= 7; i++) {
            final String[] arr = new String[i];
            Arrays.fill(arr, "X");
            assertEquals("X".repeat(i), Strings.concatNullToEmpty(arr));
        }
    }

    // join

    @Test
    public void testC543_intAndLongJoinNullAndEmptyRules() {
        assertEquals("", Strings.join((int[]) null, ", "));
        assertEquals("", Strings.join(new int[] {}, ", "));
        assertEquals("12", Strings.join(new int[] { 1, 2 }, null));
        assertEquals("12", Strings.join(new int[] { 1, 2 }, ""));
        assertEquals("", Strings.join((int[]) null, 0, 0, ", "));
        assertEquals("", Strings.join(new int[] { 1, 2 }, 1, 1, ", "));
        assertEquals("[]", Strings.join((int[]) null, 0, 0, ", ", "[", "]"));

        assertEquals("", Strings.join((long[]) null, ", "));
        assertEquals("", Strings.join(new long[] {}, ", "));
        assertEquals("12", Strings.join(new long[] { 1L, 2L }, null));
        assertEquals("", Strings.join((long[]) null, 0, 0, ", "));
        assertEquals("", Strings.join(new long[] { 1L, 2L }, 1, 1, ", "));
        assertEquals("[]", Strings.join((long[]) null, 0, 0, ", ", "[", "]"));
        assertEquals("[2]", Strings.join(new long[] { 1L, 2L }, 1, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_BufferCapacityHintsAreBoundedAndDelimiterAware() {
        assertEquals(16, Strings.calculateBufferSize(1, 16, Integer.MAX_VALUE, 0, 0));
        assertEquals(32, Strings.calculateBufferSize(2, 10, 7, 2, 3));
        assertEquals(1024 * 1024, Strings.calculateBufferSize(Integer.MAX_VALUE, 16, 0, 4, 0));
        assertEquals(1024 * 1024, Strings.calculateBufferSize(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));

        final String unusedLargeDelimiter = "x".repeat(1_000_000);
        assertEquals("value", Strings.join(List.of("value").iterator(), unusedLargeDelimiter));
        assertEquals("value", Strings.join(List.of("value"), 0, 1, unusedLargeDelimiter));
    }

    @Test
    public void testJoin_Collection() {
        final List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("[a, b, c]", join(list, 0, 3, ", ", "[", "]", false));
        assertEquals("START:b:c:END", join(list, 1, 3, ":", "START:", ":END", false));
        assertEquals("b, c", Strings.join(list, 1, 3, ", "));
        assertEquals("b,c", join(list, 1, 3, ","));
        assertEquals("", join(list, 2, 2, ","));
        assertEquals("[b,c,d]", Strings.join(Arrays.asList("a", "b", "c", "d", "e"), 1, 4, ",", "[", "]"));
        assertEquals("[a,b,c]", Strings.join(Arrays.asList(" a ", " b ", " c "), 0, 3, ",", "[", "]", true));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), 0, 3, ", ", true));
        assertThrows(IndexOutOfBoundsException.class, () -> join(list, 0, 5, ","));

        final LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(" a ", " b ", " c "));
        assertEquals("[ a ,  b ,  c ]", Strings.join(set, 0, 3, ", ", "[", "]", false));
        assertEquals("[a, b, c]", Strings.join(set, 0, 3, ", ", "[", "]", true));
        assertEquals("[a]", Strings.join(set, 0, 1, ", ", "[", "]", true));
        assertEquals("[]", Strings.join(set, 0, 0, ", ", "[", "]", false));
        assertEquals("[]", Strings.join((java.util.Collection<?>) null, 0, 0, ", ", "[", "]", false));
    }

    @Test
    public void testJoin_Iterable() {
        assertEquals("1, 2, 3", Strings.join(list(1, 2, 3)));
        assertEquals("1-2-3", Strings.join(list(1, 2, 3), "-"));
        assertEquals("a,b,c", Strings.join(Arrays.asList("a", "b", "c"), ","));
        assertEquals("", Strings.join(new ArrayList<>(), ","));
        assertEquals("", Strings.join((Iterable<String>) null, ","));
        assertEquals("a|b|c", Strings.join(Arrays.asList("a", "b", "c"), "|"));
        assertEquals("abc", Strings.join(Arrays.asList("a", "b", "c"), null));
        assertEquals("", join((Iterable<?>) null));
        assertEquals("[a, b]", join(Arrays.asList("a", "b"), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3), "-", "{", "}"));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b "), ", ", "[", "]", true));
        assertEquals("[x,y,z]", Strings.join(Arrays.asList("x", "y", "z"), ",", "[", "]"));
        assertEquals("[x,y]", Strings.join(Arrays.asList(" x ", " y "), ",", "[", "]", true));
    }

    @Test
    public void testJoin_Iterator() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c").iterator()));
        assertEquals("a:b:c", join(Arrays.asList("a", "b", "c").iterator(), ":"));
        assertEquals("abc", join(Arrays.asList("a", "b", "c").iterator(), null));
        assertEquals("", join(Collections.emptyList().iterator()));
        assertEquals("", join((Iterator<?>) null));
        assertEquals("[a, b]", join(Arrays.asList("a", "b").iterator(), ", ", "[", "]"));
        assertEquals("[a, b, c]", Strings.join(Arrays.asList("a", "b", "c").iterator(), ", ", "[", "]"));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c ").iterator(), ", ", "", "", true));
        assertEquals("[a,b,c]", Strings.join(Arrays.asList(" a ", " b ", " c ").iterator(), ",", "[", "]", true));
        assertEquals("[a, b, c]", Strings.join(Arrays.asList("a", "b", "c").iterator(), ", ", "[", "]", false));
        assertEquals("[]", Strings.join((Iterator<?>) null, ", ", "[", "]", false));
        assertEquals("]", Strings.join((Iterator<?>) null, ", ", "", "]", false));
        assertEquals("[", Strings.join((Iterator<?>) null, ", ", "[", "", false));
        assertEquals("", Strings.join((Iterator<?>) null, ", ", "", "", false));
    }

    @Test
    public void testJoin_IteratorConvertsElementsAsConsumed() {
        final List<String> events = new ArrayList<>();
        final Iterator<Object> iter = new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < 2;
            }

            @Override
            public Object next() {
                final int current = index++;
                events.add("next" + current);
                return new Object() {
                    @Override
                    public String toString() {
                        events.add("string" + current);
                        return Integer.toString(current);
                    }
                };
            }
        };

        assertEquals("[0,1]", Strings.join(iter, ",", "[", "]", false));
        assertEquals(List.of("next0", "string0", "next1", "string1"), events);
    }

    @Test
    public void testJoin_ListRangeStartsAtRequestedIndex() {
        final int[] getCount = { 0 };
        final List<Integer> list = new java.util.AbstractList<>() {
            @Override
            public Integer get(final int index) {
                getCount[0]++;
                return index;
            }

            @Override
            public int size() {
                return 1000;
            }
        };

        assertEquals("[998,999]", Strings.join(list, 998, 1000, ",", "[", "]", false));
        assertEquals(2, getCount[0]);
    }

    @Test
    public void testJoin_ObjectArray() {
        assertEquals("a, b, c", Strings.join(new Object[] { "a", "b", "c" }));
        assertEquals("a|b|c", Strings.join(new Object[] { "a", "b", "c" }, "|"));
        assertEquals("[a|b|c]", Strings.join(new Object[] { "a", "b", "c" }, "|", "[", "]"));
        assertEquals("a,b,c", Strings.join(new String[] { "a", "b", "c" }, ","));
        assertEquals("abc", Strings.join(new String[] { "a", "b", "c" }, ""));
        assertEquals("", Strings.join((String[]) null, ","));
        assertEquals("1, null, 3", join(new Object[] { 1, null, 3 }));
        assertEquals("a, null, b", Strings.join(new Object[] { "a", null, "b" }, ", "));
        assertEquals("", join(new Object[] {}));
        assertEquals("", join((Object[]) null));
        assertEquals("", Strings.join((Object[]) null, ", "));
        assertEquals("[]", Strings.join(new Object[0], ", ", "[", "]"));

        assertEquals("b,c", join(new Object[] { "a", "b", "c" }, 1, 3, ","));
        assertEquals("b, c", Strings.join(new Object[] { "a", "b", "c", "d" }, 1, 3, ", "));
        assertEquals("a, b, c", join(new Object[] { " a ", " b ", " c " }, ", ", "", "", true));
        assertEquals("b:c", join(new Object[] { " a ", " b ", " c " }, 1, 3, ":", true));
        assertEquals("[a, b]", join(new Object[] { " a ", " b " }, 0, 2, ", ", "[", "]", true));
        assertEquals("[anull3]", join(new Object[] { " a ", null, 3 }, 0, 3, null, "[", "]", true));
        assertEquals("[ a ,  b ,  c ]", Strings.join(new Object[] { " a ", " b ", " c " }, 0, 3, ", ", "[", "]", false));
        assertEquals("[a, b, c]", Strings.join(new Object[] { " a ", " b ", " c " }, 0, 3, ", ", "[", "]", true));
        assertEquals("[apple,banana,cherry]", Strings.join(new Object[] { " apple ", " banana ", " cherry " }, 0, 3, ",", "[", "]", true));
        assertEquals("{1-null-3}", join(new Object[] { 1, null, 3 }, 0, 3, "-", "{", "}"));
        assertEquals("[]", Strings.join((Object[]) null, 0, 0, ", ", "[", "]", false));
    }

    @Test
    public void testJoin_PrimitiveArrays() {
        assertEquals("true, false, true", join(new boolean[] { true, false, true }));
        assertEquals("true:false:true", join(new boolean[] { true, false, true }, ":"));
        assertEquals("truefalsetrue", join(new boolean[] { true, false, true }, null));
        assertEquals("true,false,true", Strings.join(new boolean[] { true, false, true }, ","));
        assertEquals("", join(new boolean[] {}));
        assertEquals("", join((boolean[]) null));
        assertEquals("true", join(new boolean[] { true }));

        assertEquals("a, b, c", join(new char[] { 'a', 'b', 'c' }));
        assertEquals("a:b:c", join(new char[] { 'a', 'b', 'c' }, ":"));
        assertEquals("abc", join(new char[] { 'a', 'b', 'c' }, ""));
        assertEquals("a,b,c", Strings.join(new char[] { 'a', 'b', 'c' }, ","));
        assertEquals("", join((char[]) null));
        assertEquals("x", join(new char[] { 'x' }));

        assertEquals("1, 2, 3", join(new byte[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new byte[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new byte[] { 1, 2, 3 }, null));
        assertEquals("1,2,3", Strings.join(new byte[] { 1, 2, 3 }, ","));
        assertEquals("", join((byte[]) null));

        assertEquals("1, 2, 3", join(new short[] { 1, 2, 3 }));
        assertEquals("10,20,30", Strings.join(new short[] { 10, 20, 30 }, ","));
        assertEquals("", join((short[]) null));

        assertEquals("1, 2, 3", join(new int[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new int[] { 1, 2, 3 }, ":"));
        assertEquals("1|2|3", Strings.join(new int[] { 1, 2, 3 }, "|"));
        assertEquals("", Strings.join((int[]) null));

        assertEquals("1, 2, 3", join(new long[] { 1L, 2L, 3L }));
        assertEquals("100,200,300", Strings.join(new long[] { 100L, 200L, 300L }, ","));
        assertEquals("", join((long[]) null));

        assertEquals("1.0, 2.0, 3.0", join(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals("1.5,2.5,3.5", Strings.join(new float[] { 1.5f, 2.5f, 3.5f }, ","));
        assertEquals("", join((float[]) null));

        assertEquals("1.0, 2.0, 3.0", join(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals("1.1,2.2,3.3", Strings.join(new double[] { 1.1, 2.2, 3.3 }, ","));
        assertEquals("", join((double[]) null));
    }

    @Test
    public void testJoin_PrimitiveArrays_RangeAndPrefixSuffix() {
        assertEquals("false,true", join(new boolean[] { true, false, true }, 1, 3, ","));
        assertEquals("[true, false]", join(new boolean[] { true, false }, 0, 2, ", ", "[", "]"));
        assertEquals("[true, false]", join(new boolean[] { true, false }, ", ", "[", "]"));
        assertEquals("[true,false]", Strings.join(new boolean[] { true, false, true, false }, 0, 2, ",", "[", "]"));
        assertEquals("[]", join(new boolean[] {}, ", ", "[", "]"));
        assertEquals("[]", join((boolean[]) null, ", ", "[", "]"));
        assertEquals("prefix", join(new boolean[] {}, 0, 0, ", ", "prefix", null));
        assertEquals("suffix", join(new boolean[] {}, 0, 0, ", ", null, "suffix"));
        assertEquals("[truefalsetrue]", Strings.join(new boolean[] { true, false, true }, 0, 3, "", "[", "]"));
        assertEquals("[", Strings.join(new boolean[] { true, false, true }, 1, 1, ", ", "[", ""));
        assertEquals("]", Strings.join(new boolean[] { true, false, true }, 1, 1, ", ", "", "]"));

        assertEquals("b,c", join(new char[] { 'a', 'b', 'c' }, 1, 3, ","));
        assertEquals("[a, b]", join(new char[] { 'a', 'b' }, 0, 2, ", ", "[", "]"));
        assertEquals("[a, b, c]", join(new char[] { 'a', 'b', 'c' }, ", ", "[", "]"));
        assertEquals("[a,b,c]", Strings.join(new char[] { 'a', 'b', 'c', 'd' }, 0, 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.join(new char[] { 'a', 'b', 'c' }, 0, 3, "", "[", "]"));
        assertEquals("[]", join((char[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new byte[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new byte[] { 1, 2, 3 }, ", ", "[", "]"));
        assertEquals("[2,3,4]", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, ",", "[", "]"));
        assertEquals("Bytes: 2 | 3 | 4", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, " | ", "Bytes: ", ""));
        assertEquals("<12>", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 0, 2, "", "<", ">"));
        assertEquals("[]", join((byte[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new short[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new short[] { 1, 2, 3 }, ", ", "[", "]"));
        assertEquals("[10,20]", Strings.join(new short[] { 10, 20, 30, 40 }, 0, 2, ",", "[", "]"));
        assertEquals("2-3-4", Strings.join(new short[] { 1, 2, 3, 4, 5 }, 1, 4, "-", "", ""));
        assertEquals("[]", join((short[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new int[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2]", join(new int[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("START:100:200:END", join(new int[] { 100, 200 }, ":", "START:", ":END"));
        assertEquals("{2+3+4}", Strings.join(new int[] { 1, 2, 3, 4, 5 }, 1, 4, "+", "{", "}"));
        assertEquals("]", Strings.join(new int[] { 1, 2, 3 }, 1, 1, ", ", "", "]"));
        assertEquals("", Strings.join((int[]) null, 0, 0, ", ", "", ""));

        assertEquals("2,3", join(new long[] { 1L, 2L, 3L }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new long[] { 1L, 2L, 3L }, ", ", "[", "]"));
        assertEquals("<100-200>", Strings.join(new long[] { 100L, 200L, 300L, 400L }, 0, 2, "-", "<", ">"));
        assertEquals("200-300-400", Strings.join(new long[] { 100L, 200L, 300L, 400L, 500L }, 1, 4, "-", "", ""));
        assertEquals("[]", join((long[]) null, ", ", "[", "]"));

        assertEquals("2.0,3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, ","));
        assertEquals("[1.0, 2.0, 3.0]", join(new float[] { 1.0f, 2.0f, 3.0f }, ", ", "[", "]"));
        assertEquals("[2.5,3.5]", Strings.join(new float[] { 1.5f, 2.5f, 3.5f }, 1, 3, ",", "[", "]"));
        assertEquals("]", Strings.join(new float[] { 1.1f, 2.2f, 3.3f }, 0, 0, ", ", "", "]"));
        assertEquals("[]", join((float[]) null, ", ", "[", "]"));

        assertEquals("2.0,3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, ","));
        assertEquals("[1.0, 2.0, 3.0]", join(new double[] { 1.0, 2.0, 3.0 }, ", ", "[", "]"));
        assertEquals("[1.1,2.2]", Strings.join(new double[] { 1.1, 2.2, 3.3, 4.4 }, 0, 2, ",", "[", "]"));
        assertEquals("[", Strings.join((double[]) null, 0, 0, ", ", "[", ""));
        assertEquals("[]", join((double[]) null, ", ", "[", "]"));

        assertThrows(IndexOutOfBoundsException.class, () -> join(new boolean[] { true, false }, 0, 3, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new byte[] { 1, 2, 3, 4, 5 }, -1, 3, ", ", "", ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 0, 10, ", ", "", ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new short[] { 1, 2, 3, 4, 5 }, -1, 3, ", ", "", ""));
    }

    @Test
    public void testJoinArrayEqualsExplicitRangeWithElementSeparator() {
        final String sep = Strings.ELEMENT_SEPARATOR;
        final Random rnd = new Random(1830L);

        for (int it = 0; it < 200; it++) {
            final int len = rnd.nextInt(6);
            final boolean[] booleans = new boolean[len];
            final char[] chars = new char[len];
            final byte[] bytes = new byte[len];
            final short[] shorts = new short[len];
            final int[] ints = new int[len];
            final long[] longs = new long[len];
            final float[] floats = new float[len];
            final double[] doubles = new double[len];
            final Object[] objects = new Object[len];

            for (int i = 0; i < len; i++) {
                booleans[i] = rnd.nextBoolean();
                chars[i] = (char) ('a' + rnd.nextInt(26));
                bytes[i] = (byte) rnd.nextInt(256);
                shorts[i] = (short) rnd.nextInt(65536);
                ints[i] = rnd.nextInt();
                longs[i] = rnd.nextLong();
                floats[i] = rnd.nextFloat();
                doubles[i] = rnd.nextDouble();
                objects[i] = rnd.nextInt(3) == 0 ? null : "v" + i;
            }

            assertEquals(Strings.join(booleans, 0, len, sep), Strings.join(booleans));
            assertEquals(Strings.join(chars, 0, len, sep), Strings.join(chars));
            assertEquals(Strings.join(bytes, 0, len, sep), Strings.join(bytes));
            assertEquals(Strings.join(shorts, 0, len, sep), Strings.join(shorts));
            assertEquals(Strings.join(ints, 0, len, sep), Strings.join(ints));
            assertEquals(Strings.join(longs, 0, len, sep), Strings.join(longs));
            assertEquals(Strings.join(floats, 0, len, sep), Strings.join(floats));
            assertEquals(Strings.join(doubles, 0, len, sep), Strings.join(doubles));
            assertEquals(Strings.join(objects, 0, len, sep), Strings.join(objects));
        }

        // A null array returns "" instead of throwing, unlike the range overload's checkFromToIndex contract.
        assertEquals("", Strings.join((boolean[]) null));
        assertEquals("", Strings.join((char[]) null));
        assertEquals("", Strings.join((byte[]) null));
        assertEquals("", Strings.join((short[]) null));
        assertEquals("", Strings.join((int[]) null));
        assertEquals("", Strings.join((long[]) null));
        assertEquals("", Strings.join((float[]) null));
        assertEquals("", Strings.join((double[]) null));
        assertEquals("", Strings.join((Object[]) null));
    }

    @Test
    public void testC567_joinCollectionRangeOnNonListCollections() {
        final LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertEquals("b,c", Strings.join(set, 1, 3, ","));
        assertEquals("d", Strings.join(set, 3, 4, ","));
        assertEquals("", Strings.join(set, 2, 2, ","));
        assertEquals("[b, c, d]", Strings.join(set, 1, 4, ", ", "[", "]"));
        assertEquals("b|c", Strings.join(new LinkedHashSet<>(Arrays.asList(" a ", " b ", " c ")), 1, 3, "|", true));

        final ArrayDeque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals("3-4-5", Strings.join(deque, 2, 5, "-"));
        assertEquals("1", Strings.join(deque, 0, 1, "-"));

        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, 3, 5, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, -1, 2, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(set, 3, 2, ","));
    }

    @Test
    public void testJoinerAndStringsJoinAgree() {
        final String[] values = { "a", "b", "c" };
        assertEquals(Joiner.with(", ").appendAll(values).toString(), Strings.join(values, ", "));
    }

    @Test
    public void testJoinIterable_agreesWithArrayIteratorAndCollectionRange() {
        final Random rnd = new Random(830L);
        final String[] delimiters = { ", ", "-", "", null };
        final String[] affixes = { "", "[", null };

        for (int it = 0; it < 800; it++) {
            final int len = rnd.nextInt(6);
            final List<Object> list = new ArrayList<>();

            for (int i = 0; i < len; i++) {
                list.add(rnd.nextInt(4) == 0 ? null : (rnd.nextBoolean() ? " s" + i + " " : (Object) i));
            }

            final Object[] array = list.toArray();
            final String delimiter = delimiters[rnd.nextInt(delimiters.length)];
            final String prefix = affixes[rnd.nextInt(affixes.length)];
            final String suffix = affixes[rnd.nextInt(affixes.length)];
            final boolean trim = rnd.nextBoolean();

            final List<String> asStrings = new ArrayList<>();

            for (final Object o : list) {
                asStrings.add(o == null ? null : String.valueOf(o));
            }

            final String fromArray = Strings.join(array, 0, len, delimiter, prefix, suffix, trim);

            assertEquals(fromArray, Strings.join(list, delimiter, prefix, suffix, trim), "Iterable(List)");
            assertEquals(fromArray, Strings.join(list.iterator(), delimiter, prefix, suffix, trim), "Iterator");
            assertEquals(fromArray, Strings.join(list, 0, len, delimiter, prefix, suffix, trim), "Collection range");
            assertEquals(fromArray, Strings.join(iterableOf(asStrings), delimiter, prefix, suffix, trim), "non-Collection Iterable");
            assertEquals(fromArray, Strings.join(new LyingSizeCollection(asStrings, 1), delimiter, prefix, suffix, trim), "Collection with a lying size()");
        }
    }

    @Test
    public void testJoinIterable_largeCollectionMatchesArray() {
        final int n = 5000;
        final String[] array = new String[n];

        for (int i = 0; i < n; i++) {
            array[i] = "element" + i;
        }

        final List<String> list = Arrays.asList(array);
        final LinkedHashSet<String> set = new LinkedHashSet<>(list);

        assertEquals(Strings.join(array, ", "), Strings.join(list, ", "));
        assertEquals(Strings.join(array, ", "), Strings.join(set, ", "));
        assertEquals(Strings.join(array, ", "), Strings.join(list.iterator(), ", "));
        assertEquals(Strings.join(array, ", ", "[", "]"), Strings.join(list, ", ", "[", "]"));
    }

    @Test
    public void testJoinIterable_nonCollectionIterableStillWorks() {
        final List<String> elements = Arrays.asList(" a ", " b ");

        assertEquals(" a ,  b ", Strings.join(iterableOf(elements), ", "));
        assertEquals("[ a | b ]", Strings.join(iterableOf(elements), "|", "[", "]"));
        assertEquals("[a|b]", Strings.join(iterableOf(elements), "|", "[", "]", true));
    }

    @Test
    public void testJoinIterable_nullAndEmptyUnchanged() {
        assertEquals("", Strings.join((Iterable<?>) null, ", "));
        assertEquals("", Strings.join(new ArrayList<>(), ", "));
        assertEquals("Start:  :End", Strings.join((Iterable<?>) null, ", ", "Start: ", " :End"));
        assertEquals("[]", Strings.join(new ArrayList<>(), ", ", "[", "]"));
        assertEquals("]", Strings.join((Iterable<?>) null, ", ", "", "]"));
        assertEquals("[", Strings.join((Iterable<?>) null, ", ", "[", ""));
        assertEquals("", Strings.join(iterableOf(new ArrayList<>()), ", "));
    }

    @Test
    public void testJoinIterable_singleElementAndNullElement() {
        assertEquals("only", Strings.join(Arrays.asList("only"), ", "));
        assertEquals("null", Strings.join(java.util.Collections.singletonList(null), ", "));
        assertEquals("[only]", Strings.join(Arrays.asList("only"), ", ", "[", "]"));
        assertEquals("a, null, b", Strings.join(Arrays.asList("a", null, "b"), ", "));
    }

    @Test
    public void testJoinIterable_sizeIsOnlyACapacityHint_overReportedAndNegativeSize() {
        final List<String> elements = Arrays.asList("a", "b");

        assertEquals("a, b", Strings.join(new LyingSizeCollection(elements, 1_000_000), ", "));
        // A negative size must not reach new StringBuilder(int); calculateBufferSize clamps it.
        assertEquals("a, b", Strings.join(new LyingSizeCollection(elements, -1), ", "));
        assertEquals("<a-b>", Strings.join(new LyingSizeCollection(elements, -5), "-", "<", ">"));
    }

    @Test
    public void testJoinIterable_sizeIsOnlyACapacityHint_underReportedSize() {
        final List<String> elements = Arrays.asList("a", "b", "c", "d", "e");

        assertEquals("a, b, c, d, e", Strings.join(new LyingSizeCollection(elements, 1), ", "));
        assertEquals("[a|b|c|d|e]", Strings.join(new LyingSizeCollection(elements, 0), "|", "[", "]"));
    }

    @Test
    public void testC527_selfContainingCollectionOverflowsTheStack() {
        final List<Object> self = new ArrayList<>();
        self.add("x");
        self.add(self);

        assertThrows(StackOverflowError.class, () -> Strings.join(self));
        assertThrows(StackOverflowError.class, () -> Strings.join(new Object[] { self }, ", "));
        assertThrows(StackOverflowError.class, () -> Strings.join(self.iterator(), ", "));

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", self);
        assertThrows(StackOverflowError.class, () -> Strings.joinEntries(map));
        assertThrows(StackOverflowError.class, () -> Strings.concat(self, "x"));

        // lenientFormat goes through N.deepToString and survives
        assertEquals("[x, (this Collection)]", Strings.lenientFormat("%s", self));
        assertEquals("[x, (this Collection)]", self.toString());
    }



    // G074-01: pins join(char[], ..., empty/null delimiter, ...) after the bulk-append change
    @Test
    public void testJoinCharArray_emptyOrNullDelimiterRanges() {
        final char[] chars = { 'a', 'b', '\u00e9', '\u4e2d', 'c', '\ud83d', '\ude00', 'd' };
        assertEquals("ab\u00e9\u4e2dc\ud83d\ude00d", join(chars, ""));
        assertEquals("ab\u00e9\u4e2dc\ud83d\ude00d", join(chars, (String) null));
        assertEquals("b\u00e9\u4e2d", join(chars, 1, 4, ""));
        assertEquals("b\u00e9\u4e2d", join(chars, 1, 4, null));
        assertEquals("\ud83d\ude00d", join(chars, 5, 8, ""));
        assertEquals("d", join(chars, 7, 8, ""));
        assertEquals("", join(chars, 3, 3, ""));
        assertEquals("<c\ud83d>", join(chars, 4, 6, "", "<", ">"));
        assertEquals("<c\ud83d", join(chars, 4, 6, null, "<", null));
        assertEquals("c\ud83d>", join(chars, 4, 6, "", "", ">"));
        assertEquals("[ab]", join(new char[] { 'a', 'b' }, "", "[", "]"));
        assertEquals("[a]", join(new char[] { 'a' }, 0, 1, "", "[", "]"));
        assertEquals("a", join(new char[] { 'a' }, 0, 1, ""));
        assertThrows(IndexOutOfBoundsException.class, () -> join(chars, 2, 9, ""));
        assertThrows(IndexOutOfBoundsException.class, () -> join(chars, -1, 2, ""));

        final char[] big = new char[5000];
        final StringBuilder expected = new StringBuilder();

        for (int i = 0; i < big.length; i++) {
            big[i] = (char) (i % 3 == 0 ? 'x' : 0x100 + i);

            if (i >= 7 && i < 4993) {
                expected.append(big[i]);
            }
        }

        assertEquals(expected.toString(), join(big, 7, 4993, ""));
        assertEquals("(" + expected + ")", join(big, 7, 4993, "", "(", ")"));
    }

    // joinEntries

    @Test
    public void testJoinEntries() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", Strings.joinEntries(map));
        assertEquals("a:1;b:2", Strings.joinEntries(map, ";", ":"));
        assertEquals("{a:1;b:2}", Strings.joinEntries(map, ";", ":", "{", "}"));
        assertEquals("a=1;b=2", joinEntries(map, ";"));
        assertEquals("", joinEntries(Collections.emptyMap()));
        assertEquals("", joinEntries((Map<?, ?>) null));
        assertEquals("a->1, b->2", joinEntries(map, ", ", "->"));
        assertEquals("{a=1, b=2}", joinEntries(map, ", ", "=", "{", "}"));

        final Map<String, String> ranged = new LinkedHashMap<>();
        ranged.put("a", "1");
        ranged.put("b", "2");
        ranged.put("c", "3");
        assertEquals("a=1", Strings.joinEntries(ranged, 0, 1, ", ", "="));
        assertEquals("b=2", joinEntries(toIntMap(), 1, 2, ","));
        assertEquals("b:2", joinEntries(toIntMap(), 1, 2, ",", ":"));
        assertEquals("b=2, c=3", joinEntries(toIntMap(), 1, 3, ", "));
        assertEquals("[b:2|c:3]", joinEntries(toIntMap(), 1, 3, "|", ":", "[", "]", false));
        assertEquals("", joinEntries(toIntMap(), 1, 1, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> joinEntries(toIntMap(), 0, 4, ","));

        final Map<String, String> spaced = new LinkedHashMap<>();
        spaced.put(" a ", " 1 ");
        spaced.put(" b ", " 2 ");
        assertEquals("a=1, b=2", joinEntries(spaced, ", ", "=", "", "", true));
        assertEquals("{a:1|b:2}", joinEntries(spaced, "|", ":", "{", "}", true));

        final LinkedHashMap<String, Integer> fromTo = new LinkedHashMap<>();
        fromTo.put("a", 1);
        fromTo.put("b", 2);
        fromTo.put("c", 3);
        fromTo.put("d", 4);
        final String sliced = Strings.joinEntries(fromTo, 1, 3, ",", "=", "[", "]", false);
        assertTrue(sliced.contains("b=2"));
        assertTrue(sliced.contains("c=3"));
        assertFalse(sliced.contains("a=1"));

        final LinkedHashMap<String, String> trimMap = new LinkedHashMap<>();
        trimMap.put(" id ", " 1 ");
        trimMap.put(" name ", " Alice ");
        trimMap.put(" role ", " Admin ");
        assertEquals("{id:1, name:Alice}", Strings.joinEntries(trimMap, 0, 2, ", ", ":", "{", "}", true));
        assertEquals("{ id : 1 ,  name : Alice }", Strings.joinEntries(trimMap, 0, 2, ", ", ":", "{", "}", false));
        assertEquals("{}", Strings.joinEntries(trimMap, 1, 1, ", ", ":", "{", "}", false));
        assertEquals("{}", Strings.joinEntries((Map<?, ?>) null, 0, 0, ", ", ":", "{", "}", false));
        assertEquals("{a=1, b=2}", Strings.joinEntries(ranged, 0, 2, ", ", "=", "{", "}"));

        final LinkedHashMap<Object, Object> nullEntry = new LinkedHashMap<>();
        nullEntry.put(null, null);
        assertEquals("null=null", Strings.joinEntries(nullEntry));
    }

    @Test
    public void testJoinEntries_Extractors() {
        final List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>(" name ", 1), new AbstractMap.SimpleEntry<>(" age ", 25));
        assertEquals("[ name =1,  age =25]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[name=1, age=25]", Strings.joinEntries(entries, ", ", "=", "[", "]", true, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[]",
                Strings.joinEntries(new ArrayList<Map.Entry<String, Integer>>(), ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        final List<Map.Entry<String, String>> withNulls = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", null),
                new AbstractMap.SimpleEntry<>(null, "value2"), new AbstractMap.SimpleEntry<>("key3", "value3"));
        assertEquals("key1=null, null=value2, key3=value3", Strings.joinEntries(withNulls, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        final List<Map.Entry<String, String>> single = Arrays.asList(new AbstractMap.SimpleEntry<>("key", "value"));
        assertEquals("key=value", Strings.joinEntries(single, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[key=value]", Strings.joinEntries(single, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);
        final Function<Map.Entry<String, Integer>, String> keyExtractor = e -> e.getKey().toUpperCase();
        final Function<Map.Entry<String, Integer>, String> valueExtractor = e -> String.valueOf(e.getValue() * 2);
        assertEquals("KEY1=200, KEY2=400", joinEntries(map.entrySet(), ", ", "=", "", "", false, keyExtractor, valueExtractor));

        final List<String[]> pairs = Arrays.asList(new String[] { "key1", "val1" }, new String[] { "key2", "val2" });
        assertEquals("[key1=val1, key2=val2]", Strings.<String[]> joinEntries(pairs, ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));
        assertEquals("[k1=v1]",
                Strings.<String[]> joinEntries(Collections.singletonList(new String[] { " k1 ", " v1 " }), ", ", "=", "[", "]", true, e -> e[0], e -> e[1]));
        assertEquals("[]", Strings.<String[]> joinEntries((List<String[]>) null, ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));
        assertEquals("[null=null]",
                Strings.<String[]> joinEntries(Collections.singletonList(new String[] { null, null }), ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));

        final Iterable<String[]> nullIterable = null;
        final Function<String[], Object> extractor = entry -> entry[0];
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", null, extractor));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", extractor, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", "[", "]", false, null, extractor));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", "[", "]", false, extractor, null));
    }

    @Test
    public void testJoinEntries_nullDelimitersTreatedAsEmpty() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        assertEquals("a=1b=2", Strings.joinEntries(m, (String) null));
        assertEquals("a1, b2", Strings.joinEntries(m, ", ", (String) null));
        assertEquals("a=1, b=2", Strings.joinEntries(m, ", "));
    }

    @Test
    public void testC564_joinEntriesForwarderStillValidatesExtractorsCollaboratorFirst() {
        final List<Map.Entry<String, Integer>> entries = Arrays.asList(Map.entry("a", 1), Map.entry("b", 2));
        final List<Map.Entry<String, Integer>> nullEntries = null;
        final List<Map.Entry<String, Integer>> noEntries = Collections.emptyList();
        final Function<Map.Entry<String, Integer>, String> key = Map.Entry::getKey;
        final Function<Map.Entry<String, Integer>, Integer> value = Map.Entry::getValue;

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(nullEntries, ",", "=", null, value)).getMessage()
                .contains("keyExtractor"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(noEntries, ",", "=", key, null)).getMessage()
                .contains("valueExtractor"));
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> Strings.joinEntries(entries, ",", "=", null, null)).getMessage().contains("keyExtractor"));
        assertEquals("a=1, b=2", Strings.joinEntries(entries, ", ", "=", key, value));
        assertEquals("", Strings.joinEntries(nullEntries, ", ", "=", key, value));
        assertEquals("", Strings.joinEntries(noEntries, ", ", "=", key, value));
        assertEquals("a1b2", Strings.joinEntries(entries, null, "", key, value));
    }



    // C-030: joinEntries sizing change keeps the output
    @Test
    public void testC030_joinEntriesIterable() {
        final List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        entries.add(new java.util.AbstractMap.SimpleEntry<>("a", 1));
        entries.add(new java.util.AbstractMap.SimpleEntry<>("b", 2));
        final Iterable<Map.Entry<String, Integer>> iterable = entries::iterator;

        assertEquals("[a=1, b=2]", Strings.joinEntries(iterable, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[a=1, b=2]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[]",
                Strings.joinEntries(Collections.<Map.Entry<String, Integer>> emptyList(), ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void testC528_joinEntriesRangeUnchanged() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.put("d", 4);

        assertEquals("b=2, c=3", Strings.joinEntries(m, 1, 3, ", ", "=", "", "", false));
        assertEquals("<b=2, c=3>", Strings.joinEntries(m, 1, 3, ", ", "=", "<", ">", false));
        assertEquals("a=1", Strings.joinEntries(m, 0, 1, ", ", "=", "", "", false));
        assertEquals("d=4", Strings.joinEntries(m, 3, 4, ", ", "=", "", "", false));
        assertEquals("", Strings.joinEntries(m, 2, 2, ", ", "=", "", "", false));
        assertEquals("a=1, b=2, c=3, d=4", Strings.joinEntries(m, 0, 4, ", ", "=", "", "", false));
        assertEquals("b=2, c=3", Strings.joinEntries(m, 1, 3, ", "));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.joinEntries(m, 1, 5, ", ", "=", "", "", false));
    }

}
