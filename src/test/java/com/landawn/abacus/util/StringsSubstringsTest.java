package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringBetween;
import static com.landawn.abacus.util.Strings.substringIndicesBetween;
import static com.landawn.abacus.util.Strings.substringsBetween;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.DelimiterMatchMode;

/** Strings substrings contracts, including regression cases. */
public class StringsSubstringsTest extends StringsTestSupport {

    /**
     * Verbatim copy of the r9620 nested engine of
     * {@link Strings#substringIndicesBetween(String, int, int, String, String, DelimiterMatchMode, int)} (before C-431),
     * used as the differential reference.
     */
    private static List<IndexRange> previousEngine(final String str, final int fromIndex, final int toIndex, final String begin, final String end,
            final DelimiterMatchMode delimiterMatchMode, final int maxCount) {
        if (str == null || Strings.isEmpty(begin) || Strings.isEmpty(end) || maxCount == 0) {
            return new ArrayList<>();
        }

        int idx = str.indexOf(begin, fromIndex);

        if (idx < 0 || idx > toIndex - begin.length()) {
            return new ArrayList<>();
        }

        final List<IndexRange> res = new ArrayList<>();
        final int bl = begin.length();
        final int el = end.length();

        idx += bl;

        if (delimiterMatchMode == DelimiterMatchMode.SEQUENTIAL) {
            int endIndex = -1;

            do {
                endIndex = str.indexOf(end, idx);

                if (endIndex < 0 || endIndex > toIndex - el) {
                    break;
                }

                res.add(new IndexRange(idx, endIndex));

                if (res.size() >= maxCount) {
                    break;
                }

                idx = str.indexOf(begin, endIndex + el);

                if (idx < 0) {
                    break;
                }

                idx += bl;
            } while (idx < toIndex);
        } else {
            final Deque<Integer> queue = new ArrayDeque<>();

            queue.add(idx);
            int next = -1;

            for (int i = idx; i < toIndex;) {
                if (queue.isEmpty()) {
                    idx = next >= i ? next : str.indexOf(begin, i);

                    if (idx < 0) {
                        break;
                    } else {
                        idx += bl;
                        queue.add(idx);
                        i = idx;
                    }
                }

                idx = str.indexOf(end, i);

                if (idx < 0 || idx > toIndex - el) {
                    break;
                } else {
                    final int endIndex = idx;
                    idx = res.size() > 0 ? Math.max(res.get(res.size() - 1).end() + el, queue.peekLast()) : queue.peekLast();

                    while ((idx = str.indexOf(begin, idx)) >= 0 && idx + bl <= endIndex) {
                        idx += bl;
                        queue.push(idx);
                    }

                    if (idx >= 0) {
                        next = idx;
                    }

                    final int startIndex = queue.pop();

                    if (delimiterMatchMode == DelimiterMatchMode.OUTERMOST_ONLY && res.size() > 0 && startIndex < res.get(res.size() - 1).start()) {
                        while (res.size() > 0 && startIndex < res.get(res.size() - 1).start()) {
                            res.remove(res.size() - 1);
                        }
                    }

                    res.add(new IndexRange(startIndex, endIndex));

                    if (res.size() >= maxCount && (delimiterMatchMode != DelimiterMatchMode.OUTERMOST_ONLY || queue.isEmpty())) {
                        break;
                    }

                    i = endIndex + el;
                }
            }

            if (delimiterMatchMode == DelimiterMatchMode.OUTERMOST_ONLY) {
                while (res.size() > maxCount) {
                    res.remove(res.size() - 1);
                }
            }
        }

        return res;
    }

    protected List<String> substringsBetween_StackBased_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, DelimiterMatchMode.ALL_LEVELS);
    }

    protected List<String> substringsBetween_IgnoreNested_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, DelimiterMatchMode.OUTERMOST_ONLY);
    }

    @Test
    public void testAllModes_documentedExamplesUnchanged() {
        final String s = "3[a2[c]]2[a]";
        assertEquals(List.of("a2[c", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.SEQUENTIAL));
        assertEquals(List.of("c", "a2[c]", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween(s, '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));

        final String t = "[[b[a]]c]";
        assertEquals(List.of("[b[a"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.SEQUENTIAL));
        assertEquals(List.of("a", "b[a]", "[b[a]]c"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween(t, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testOutermostOnly_balancedInputUnchanged() {
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween("[[b[a]]c]", "[", "]", DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("a[b]c"), Strings.substringsBetween("[a[b]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of("[a]"), Strings.substringsBetween("[[a]]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testOutermostOnly_maxCountTruncatesTheSameSequence() {
        for (final String s : new String[] { "a[b[c]", "3[a2[c]]2[a]", "[[b[a]]c]", "[a][b][c]" }) {
            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> full = Strings.substringIndicesBetween(s, 0, s.length(), "[", "]", mode, Integer.MAX_VALUE);

                for (int maxCount = 0; maxCount <= full.size() + 1; maxCount++) {
                    final List<IndexRange> limited = Strings.substringIndicesBetween(s, 0, s.length(), "[", "]", mode, maxCount);
                    assertEquals(full.subList(0, Math.min(maxCount, full.size())), limited, "s=" + s + " mode=" + mode + " maxCount=" + maxCount);
                }
            }
        }
    }

    // J-1 : DelimiterMatchMode.OUTERMOST_ONLY on unbalanced input
    @Test
    public void testOutermostOnly_unclosedBeginDelimiterSuppressesNothing() {
        // The '[' at index 1 is never closed, so "c" is the outermost *match* even though it is not at
        // nesting depth zero. This is the behaviour the enum javadoc now states.
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of(""), Strings.substringsBetween("[x[]x", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(List.of(""), Strings.substringsBetween("[[[][x[", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));

        // ALL_LEVELS agrees on these inputs (there is only one complete pair each)
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(""), Strings.substringsBetween("[x[]x", '[', ']', DelimiterMatchMode.ALL_LEVELS));

        // an unmatched *end* delimiter likewise changes nothing
        assertEquals(List.of("a"), Strings.substringsBetween("[a]b]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testOutermostOnly_indexRangesMatchTheSubstrings() {
        // substringIndicesBetween must stay the exact index counterpart of substringsBetween
        for (final String s : new String[] { "a[b[c]", "[x[]x", "3[a2[c]]2[a]", "[[b[a]]c]", "[a]b]", "[[[][x[" }) {
            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> ranges = Strings.substringIndicesBetween(s, '[', ']', mode);
                final List<String> expected = new ArrayList<>(ranges.size());

                for (final IndexRange r : ranges) {
                    expected.add(s.substring(r.start(), r.end()));
                }

                assertEquals(expected, Strings.substringsBetween(s, '[', ']', mode), "s=" + s + " mode=" + mode);
            }
        }
    }

    // substringIndicesBetween

    @Test
    public void testC431_deepNestBeforeLongTailIsLinear() {
        final int depth = 3000;
        final String str = "{".repeat(depth) + "}".repeat(depth) + "x".repeat(2_000_000) + "{z}";

        final List<IndexRange> all = Strings.substringIndicesBetween(str, 0, str.length(), "{", "}", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE);
        assertEquals(depth + 1, all.size());
        assertEquals(IndexRange.of(depth, depth), all.get(0));

        final List<IndexRange> outer = Strings.substringIndicesBetween(str, 0, str.length(), "{", "}", DelimiterMatchMode.OUTERMOST_ONLY, Integer.MAX_VALUE);
        assertEquals(2, outer.size());
        assertEquals(IndexRange.of(1, 2 * depth - 1), outer.get(0));
        assertEquals("z", str.substring(outer.get(1).start(), outer.get(1).end()));
    }

    @Test
    public void testC542_maxCountIsAlwaysAPrefixOfTheUnlimitedResult() {
        final Random random = new Random(20260925L);
        final String[][] delimiterPairs = { { "[", "]" }, { "ab", "bc" }, { "aa", "a" }, { "a", "ab" }, { "x", "xx" }, { "[]", "[" } };
        final char[] alphabet = { 'a', 'b', 'c', 'x', '[', ']' };
        int checks = 0;

        for (int round = 0; round < 20000; round++) {
            final int len = random.nextInt(24);
            final StringBuilder sb = new StringBuilder(len);

            for (int i = 0; i < len; i++) {
                sb.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String str = sb.toString();
            final String[] pair = delimiterPairs[random.nextInt(delimiterPairs.length)];
            final int from = random.nextInt(len + 1);
            final int to = from + random.nextInt(len - from + 1);

            for (final DelimiterMatchMode mode : DelimiterMatchMode.values()) {
                final List<IndexRange> unlimited = Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, Integer.MAX_VALUE);

                for (int maxCount = 0; maxCount <= 4; maxCount++) {
                    final List<IndexRange> limited = Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, maxCount);
                    assertEquals(unlimited.subList(0, Math.min(maxCount, unlimited.size())), limited,
                            () -> "str=" + str + " from=" + from + " to=" + to + " pair=" + Arrays.toString(pair) + " mode=" + mode);
                    checks++;
                }
            }
        }

        assertTrue(checks > 0);
    }

    // C-431: the nested-mode engine no longer rescans the tail after every end delimiter; results are unchanged
    @Test
    public void testC431_nestedSubstringIndicesBetweenMatchesPreviousEngine() {
        final Random rnd = new Random(431);
        final String[][] delimiterPairs = { { "[", "]" }, { "ab", "bc" }, { "[[", "]]" }, { "a", "a" }, { "(", ")" }, { "aa", "a" } };
        final String alphabet = "[]()abcx";
        final DelimiterMatchMode[] modes = { DelimiterMatchMode.ALL_LEVELS, DelimiterMatchMode.OUTERMOST_ONLY, DelimiterMatchMode.SEQUENTIAL };

        for (int round = 0; round < 60_000; round++) {
            final StringBuilder sb = new StringBuilder();
            final int n = rnd.nextInt(24);
            for (int k = 0; k < n; k++) {
                sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            }
            final String str = sb.toString();
            final String[] pair = delimiterPairs[rnd.nextInt(delimiterPairs.length)];
            final int from = str.isEmpty() ? 0 : rnd.nextInt(str.length() + 1);
            final int to = from + rnd.nextInt(str.length() - from + 1);
            final DelimiterMatchMode mode = modes[rnd.nextInt(modes.length)];
            final int maxCount = rnd.nextBoolean() ? Integer.MAX_VALUE : rnd.nextInt(4);

            assertEquals(previousEngine(str, from, to, pair[0], pair[1], mode, maxCount),
                    Strings.substringIndicesBetween(str, from, to, pair[0], pair[1], mode, maxCount),
                    () -> str + " [" + from + "," + to + ") " + Arrays.toString(pair) + " " + mode + " " + maxCount);
        }
    }

    @Test
    public void testSubstringIndicesBetween() {
        List<IndexRange> result = Strings.substringIndicesBetween("a:b:c", ':', ':');
        assertEquals(1, result.size());
        assertEquals(new IndexRange(2, 3), result.get(0));
    }

    @Test
    public void test_substringIndicesBetween() {

        {
            assertEquals(List.of(new IndexRange(2, 6), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 7), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(1, 5)), Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(4, 5), new IndexRange(2, 6), new IndexRange(1, 8)),
                    Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(1, 8)), Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(1, 5), new IndexRange(7, 8)),
                    Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(4, 5), new IndexRange(7, 8), new IndexRange(2, 10)),
                    Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 10)), Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

    }

    @Test
    public void test_substringIndicesBetween_maxCount() {
        final String str = "3[a2[c]]2[a]";

        // SEQUENTIAL: maxCount simply limits sequential matches.
        assertEquals(List.of(new IndexRange(2, 6)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 1));
        assertEquals(List.of(new IndexRange(2, 6), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 2));

        // ALL_LEVELS: matches are reported inner-first, so maxCount limits in that discovery order.
        assertEquals(List.of(new IndexRange(5, 6)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 1));
        assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 7)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 2));

        // OUTERMOST_ONLY: nested matches must never be returned, even when maxCount stops the scan early.
        // Before the fix, maxCount=1 returned [[5, 6]] ("c"), a nested match that the strategy promises to ignore.
        assertEquals(List.of(new IndexRange(2, 7)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 10));

        // Value-returning variant goes through the same code path.
        assertEquals(CommonUtil.toList("a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // Regression: an enclosing match discovered after maxCount sibling matches must still replace them,
        // and the result must be trimmed to the first maxCount outermost matches.
        assertEquals(List.of(new IndexRange(1, 7)), Strings.substringIndicesBetween("[[a][b]]", 0, 8, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(new IndexRange(1, 2), new IndexRange(4, 10)),
                Strings.substringIndicesBetween("[x][[a][b]]", 0, 11, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(CommonUtil.toList("[a][b]"), Strings.substringsBetween("[[a][b]]", 0, 8, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // Unclosed outer delimiter: pending matches stay, but the result must still honor maxCount.
        final String unclosed = "[a[b]c[d]";
        assertEquals(List.of(new IndexRange(3, 4), new IndexRange(7, 8)),
                Strings.substringIndicesBetween(unclosed, 0, unclosed.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 10));
        assertEquals(List.of(new IndexRange(3, 4)),
                Strings.substringIndicesBetween(unclosed, 0, unclosed.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // maxCount == 0 returns an empty list.
        assertTrue(Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 0).isEmpty());
    }

    @Test
    public void test_substringIndicesBetween_overlappingDelimiters() {
        // Regression: with multi-character delimiters whose occurrences overlap (begin "ab" and end "bc"
        // share the 'b' at index 3), the nested modes must not treat the overlapping begin as nested.
        // Before the fix, ALL_LEVELS/OUTERMOST_ONLY pushed a content start past the end delimiter and
        // threw IllegalArgumentException from IndexRange(4, 3).
        final List<IndexRange> expected = List.of(new IndexRange(2, 3));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.SEQUENTIAL));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(CommonUtil.toList("a"), Strings.substringsBetween("ababc", "ab", "bc", DelimiterMatchMode.ALL_LEVELS));

        // Same input through the range-bounded overload; the end delimiter must be wholly inside the range.
        assertEquals(expected, Strings.substringIndicesBetween("ababc", 0, 5, "ab", "bc", DelimiterMatchMode.ALL_LEVELS, 10));
        assertTrue(Strings.substringIndicesBetween("ababc", 0, 4, "ab", "bc", DelimiterMatchMode.ALL_LEVELS, 10).isEmpty());

        // Equal begin/end delimiters remain pairable.
        assertEquals(List.of(new IndexRange(3, 4)), Strings.substringIndicesBetween("xabxabx", "ab", "ab", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(new IndexRange(3, 4)), Strings.substringIndicesBetween("xabxabx", "ab", "ab", DelimiterMatchMode.OUTERMOST_ONLY));

        // Non-overlapping multi-character delimiters still nest normally.
        assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 9)),
                Strings.substringIndicesBetween("<<a<<b>>c>>", "<<", ">>", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(new IndexRange(2, 9)), Strings.substringIndicesBetween("<<a<<b>>c>>", "<<", ">>", DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testSubstringIndicesBetween_Chars() {
        List<IndexRange> indices = substringIndicesBetween("3[a2[c]]2[a]", '[', ']');
        assertEquals(2, indices.size());
        assertEquals(new IndexRange(2, 6), indices.get(0));
        assertEquals(new IndexRange(10, 11), indices.get(1));

        assertTrue(substringIndicesBetween("abc", '[', ']').isEmpty());
        assertTrue(substringIndicesBetween(null, '[', ']').isEmpty());
        assertTrue(substringIndicesBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_CharsWithRange() {
        List<IndexRange> indices = substringIndicesBetween("3[a2[c]]2[a]", 0, 8, '[', ']');

        assertEquals(1, indices.size());
        assertEquals(new IndexRange(2, 6), indices.get(0));
    }

    @Test
    public void testSubstringIndicesBetween_Strings() {
        List<IndexRange> indices = substringIndicesBetween("<tag>text1</tag>", "<tag>", "</tag>");

        assertEquals(1, indices.size());
        assertEquals(new IndexRange(5, 10), indices.get(0));

        assertTrue(substringIndicesBetween("abc", "<", ">").isEmpty());
        assertTrue(substringIndicesBetween(null, "<", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "<", "").isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_StringsWithRange() {
        List<IndexRange> indices = substringIndicesBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");

        assertEquals("text1", substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>").get(0));
        assertEquals(1, indices.size());
        assertEquals(new IndexRange(5, 10), indices.get(0));

    }

    // substringsBetween

    @Test
    public void testC562_nullDelimiterMatchModeRejectedForNullAndEmptyInput() {
        for (final String str : new String[] { null, "" }) {
            assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(str, '[', ']', null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(str, "[", "]", null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(str, '[', ']', null));
            assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(str, "[", "]", null));
        }

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(null, "[", "]", null)).getMessage()
                .contains("delimiterMatchMode"));
    }

    @Test
    public void testC542_outermostOnlyWithSmallMaxCountResultUnchanged() {
        final String str = "[" + "[a]".repeat(2000);
        assertEquals(List.of(new IndexRange(2, 3)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a", "a"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of(new IndexRange(2, 3), new IndexRange(5, 6)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));

        // the pinned shape from StringsSubstringTest: an enclosing match still replaces a stored nested one
        assertEquals(List.of("a2[c]"), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a2[c]", "a"), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of("[b[a]]c"), Strings.substringsBetween("[[b[a]]c]", 0, 9, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("c"), Strings.substringsBetween("a[b[c]", 0, 6, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(), Strings.substringsBetween("3[a2[c]]2[a]", 0, 12, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 0));
    }

    @Test
    public void testSubstringsBetween() {
        List<String> result = Strings.substringsBetween("a:b:c:d", ':', ':');
        assertEquals(Arrays.asList("b"), result);
    }

    @Test
    public void test_substringsBetween() {

        {
            assertEquals("[\"a2[c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"c\", \"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"[b[a\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a\", \"b[a]\", \"[b[a]]c\"]",
                    CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

        {
            assertEquals("[\"[b[a\", \"c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.SEQUENTIAL)));
            assertEquals("[\"a\", \"c\", \"b[a][c]d\"]",
                    CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.ALL_LEVELS)));
            assertEquals("[\"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY)));
        }

    }

    @Test
    public void testSubstringsBetween_EndDelimiterMustFitWithinRange() {
        final String str = "a<x>>z";

        for (final DelimiterMatchMode strategy : DelimiterMatchMode.values()) {
            assertTrue(Strings.substringsBetween(str, 0, 4, "<", ">>", strategy, Integer.MAX_VALUE).isEmpty());
            assertEquals(CommonUtil.asList("x"), Strings.substringsBetween(str, 0, 5, "<", ">>", strategy, Integer.MAX_VALUE));
        }
    }

    @Test
    public void testSubstringsBetween_NestedBracketsDefaultMode() {
        assertEquals(CommonUtil.toList("a2[c", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']'));
    }

    @Test
    public void testSubstringsBetween_StringStringString() {

        String[] results = StringUtils.substringsBetween("[one], [two], [three]", "[", "]");
        assertEquals(3, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);
        assertEquals("three", results[2]);

        results = StringUtils.substringsBetween("[one], [two], [three]", "", "");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], three", "[", "]");
        assertEquals(2, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);

        results = StringUtils.substringsBetween("[one], [two], three]", "[", "]");
        assertEquals(2, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);

        results = StringUtils.substringsBetween("[one], two], three]", "[", "]");
        assertEquals(1, results.length);
        assertEquals("one", results[0]);

        results = StringUtils.substringsBetween("one], two], [three]", "[", "]");
        assertEquals(1, results.length);
        assertEquals("three", results[0]);

        results = StringUtils.substringsBetween("aabhellobabnonba", "ab", "ba");
        assertEquals(1, results.length);
        assertEquals("hello", results[0]);

        results = StringUtils.substringsBetween("one, two, three", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one, two, three", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("one, two, three]", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", "[", null);
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", null, "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", "", "");
        assertNull(results);

        results = StringUtils.substringsBetween(null, "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("", "[", "]");
        assertEquals(0, results.length);

        results = StringUtils.substringsBetween("", "", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("", "[", "");
        assertNull(results);

        results = StringUtils.substringsBetween("", "", "");
        assertNull(results);
    }

    @Test
    public void testSubstringsBetween_StringStringString_01() {

        List<String> results = Strings.substringsBetween("[one], [two], [three]", "[", "]");
        assertEquals(3, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));
        assertEquals("three", results.get(2));

        results = Strings.substringsBetween("[one], [two], [three]", "", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], three", "[", "]");
        assertEquals(2, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));

        results = Strings.substringsBetween("[one], [two], three]", "[", "]");
        assertEquals(2, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));

        results = Strings.substringsBetween("[one], two], three]", "[", "]");
        assertEquals(1, results.size());
        assertEquals("one", results.get(0));

        results = Strings.substringsBetween("one], two], [three]", "[", "]");
        assertEquals(1, results.size());
        assertEquals("three", results.get(0));

        results = Strings.substringsBetween("aabhellobabnonba", "ab", "ba");
        assertEquals(1, results.size());
        assertEquals("hello", results.get(0));

        results = Strings.substringsBetween("one, two, three", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one, two, three", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("one, two, three]", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", "[", null);
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", null, "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", "", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween(null, "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "[", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "", "");
        assertEquals(0, results.size());
    }

    @Test
    public void testSubstringsBetweenDeclaredExceptionsAreActuallyThrown() {
        // Range overloads: a null input has length zero, so only (0, 0) is a valid range.
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween(null, 0, 1, '[', ']'));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween(null, 0, 1, "[", "]"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween("abc", 0, 4, "[", "]"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringsBetween("abc", 0, 4, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringIndicesBetween(null, 0, 1, '[', ']'));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringIndicesBetween(null, 0, 1, "[", "]"));

        // Mode-taking overloads reject a null mode collaborator-first.
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", '[', ']', null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", "[", "]", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween("a[b]c", '[', ']', null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween("a[b]c", "[", "]", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween(null, 0, 0, "[", "]", null, 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringsBetween("a[b]c", 0, 5, "[", "]", Strings.DelimiterMatchMode.SEQUENTIAL, -1));

        // ... and the no-mode overloads still cannot throw, because they supply a non-null constant.
        assertEquals(0, Strings.substringsBetween(null, '[', ']').size());
        assertEquals(0, Strings.substringsBetween(null, "[", "]").size());
        assertEquals(1, Strings.substringsBetween("a[b]c", '[', ']').size());
    }

    @Test
    public void testSubstringsBetweenDefaultStrategy() {
        assertEquals(list("a", "b"), Strings.substringsBetween("[a][b]", '[', ']'));
        assertEquals(list("a[b"), Strings.substringsBetween("[a[b]c]", '[', ']'));
    }

    @Test
    public void testC545_substringsBetweenMaxCountCountsPerMode() {
        final String str = "3[a2[c]]2[a]";
        assertEquals(List.of("a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of("a2[c"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 1));
        assertEquals(List.of("c"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 1));
        assertEquals(List.of("c", "a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 2));

        // substrings == mapped indices for the same arguments
        final List<IndexRange> ranges = Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1);
        assertEquals(1, ranges.size());
        assertEquals("a2[c]", str.substring(ranges.get(0).start(), ranges.get(0).end()));
    }

    @Test
    public void testSubstringsBetweenStackBased_honorsToIndex() {
        // regression: the ALL_LEVELS branch ignored toIndex, returning matches partially or entirely
        // outside the requested range
        assertTrue(Strings.substringsBetween("a[b]c", 0, 3, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE).isEmpty());
        assertTrue(Strings.substringsBetween("a[bcdefg]h", 0, 3, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE).isEmpty());
        assertEquals(CommonUtil.asList("a"), Strings.substringsBetween("[a]x[b]", 0, 4, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE));

        // full-range behavior unchanged
        assertEquals(CommonUtil.asList("b"), Strings.substringsBetween("a[b]c", 0, 5, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE));
    }

    @Test
    public void testSubstringsBetween_Chars() {
        List<String> result = substringsBetween("3[a2[c]]2[a]", '[', ']');

        assertEquals(2, result.size());
        assertEquals("a2[c", result.get(0));
        assertEquals("a", result.get(1));

        assertTrue(substringsBetween("abc", '[', ']').isEmpty());
        assertTrue(substringsBetween(null, '[', ']').isEmpty());
        assertTrue(substringsBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_CharsWithRange() {
        List<String> result = substringsBetween("3[a2[c]]2[a]", 0, 8, '[', ']');

        assertEquals("a2[c", substringBetween("3[a2[c]]2[a]", '[', ']'));

        assertEquals(1, result.size());
        assertEquals("a2[c", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_Strings() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", "<tag>", "</tag>");

        assertEquals(2, result.size());
        assertEquals("text1", result.get(0));
        assertEquals("text2", result.get(1));

        assertTrue(substringsBetween("abc", "<", ">").isEmpty());
        assertTrue(substringsBetween(null, "<", ">").isEmpty());
        assertTrue(substringsBetween("test", null, ">").isEmpty());
        assertTrue(substringsBetween("test", "<", null).isEmpty());
    }

    @Test
    public void testSubstringsBetween_StringsWithRange() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");

        assertEquals(1, result.size());
        assertEquals("text1", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, "<", ">").isEmpty());
    }

    @Test
    public void testSubstringsBetweenIgnoreNested() {
        assertEquals(list("a2[c]", "a"), substringsBetween_IgnoreNested_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("[b[a]]c"), substringsBetween_IgnoreNested_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void testSubstringsBetweenStackBased() {
        assertEquals(list("c", "a2[c]", "a"), substringsBetween_StackBased_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("a", "b[a]", "[b[a]]c"), substringsBetween_StackBased_("[[b[a]]c]", '[', ']'));
    }

}
