package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Splitter.MapSplitter;

/**
 * Regression tests for the {@link Splitter} review of 2026-09-01 (second pass).
 *
 * <p>Headline finding (B1): {@code omitEmptyStrings()} was silently defeated by {@code limit(n)} &mdash; the
 * final element was taken from the raw scan position, so empty fields the caller asked to be dropped were
 * absorbed into it, yielding elements that began with (or consisted entirely of) the delimiter. Every engine
 * was affected, and so was every {@code MapSplitter.limit(n)} call, because a {@code MapSplitter} always has
 * {@code omitEmptyStrings()} on.</p>
 */
public class SplitterOmitEmptyLimitFixTest extends TestBase {

    /**
     * A comma splitter on the named engine. The multi-character "string" engine is not selectable here because
     * it only takes its own code path for a delimiter of 2+ characters; it is covered separately.
     */
    private static Splitter commaEngine(final String name) {
        return switch (name) {
            case "char" -> Splitter.with(',');
            case "pattern" -> Splitter.with(Pattern.compile(","));
            default -> throw new IllegalArgumentException(name);
        };
    }

    /** B1: an omitted empty token must neither consume the limit nor survive inside the final element. */
    @Nested
    public class OmittedEmptyTokensNeverReachTheFinalElement {

        @ParameterizedTest(name = "{0} engine")
        @ValueSource(strings = { "char", "pattern" })
        public void emptyFieldsBeforeTheLimitBoundaryAreDropped(final String engineName) {
            // Used to return ["a", ",,b,c"] - the second element began with two delimiters.
            assertEquals(Arrays.asList("a", "b,c"), commaEngine(engineName).omitEmptyStrings().limit(2).split("a,,,b,c"));
        }

        @ParameterizedTest(name = "{0} engine")
        @ValueSource(strings = { "char", "pattern" })
        public void leadingEmptyFieldsAreDroppedEvenAtLimitOne(final String engineName) {
            // Used to return [",,a,b"] - the whole raw input, delimiters included.
            assertEquals(Arrays.asList("a,b"), commaEngine(engineName).omitEmptyStrings().limit(1).split(",,a,b"));
        }

        @ParameterizedTest(name = "{0} engine")
        @ValueSource(strings = { "char", "pattern" })
        public void anAllDelimiterInputYieldsNothing(final String engineName) {
            // Used to return [","] - a single "field" that was nothing but the delimiter.
            assertTrue(commaEngine(engineName).omitEmptyStrings().limit(1).split(",").isEmpty());
        }

        @ParameterizedTest(name = "{0} engine")
        @ValueSource(strings = { "char", "pattern" })
        public void trailingEmptyFieldsDoNotFillTheLimit(final String engineName) {
            // Used to return ["a", "b", ",,"].
            assertEquals(Arrays.asList("a", "b"), commaEngine(engineName).omitEmptyStrings().limit(3).split("a,b,,,"));
        }

        @Test
        public void theMultiCharacterStringEngineBehavesIdentically() {
            assertEquals(Arrays.asList("a", "b::c"), Splitter.with("::").omitEmptyStrings().limit(2).split("a::::::b::c"));
            assertEquals(Arrays.asList("a::b"), Splitter.with("::").omitEmptyStrings().limit(1).split("::::a::b"));
            assertTrue(Splitter.with("::").omitEmptyStrings().limit(1).split("::").isEmpty());
        }

        @Test
        public void trimmingRunsBeforeTheEmptyCheckWhichRunsBeforeTheLimit() {
            // " " trims to "", is dropped, and therefore does not become part of the final element.
            assertEquals(Arrays.asList("a", "b , c"), Splitter.with(',').trimResults().omitEmptyStrings().limit(2).split(" a , , b , c "));
            assertEquals(Arrays.asList("a", "b , c"), Splitter.with(',').stripResults().omitEmptyStrings().limit(2).split(" a ,\t, b , c "));
        }

        @Test
        public void withoutOmitEmptyStringsTheRemainderIsStillVerbatim() {
            // The fix must not change the far more common no-omit case.
            assertEquals(Arrays.asList("", ",a,b,c"), Splitter.with(',').limit(2).split(",,a,b,c"));
            // The widened final element is trimmed on both ends, so the trailing space is removed too.
            assertEquals(Arrays.asList("a", "b , c"), Splitter.with(',').trimResults().limit(2).split(" a , b , c "));
        }

        @Test
        public void aLimitAboveTheTokenCountChangesNothing() {
            for (final int limit : new int[] { 2, 3, 10, Integer.MAX_VALUE }) {
                assertEquals(Arrays.asList("a", "b"), Splitter.with(',').omitEmptyStrings().limit(limit).split("a,,b"), "limit " + limit);
            }
        }

        @Test
        public void theLimitStillCapsTheResultSize() {
            for (int limit = 1; limit <= 6; limit++) {
                assertTrue(Splitter.with(',').omitEmptyStrings().limit(limit).split("a,,b,,c,,d,,e").size() <= limit, "limit " + limit + " was exceeded");
            }
        }
    }

    /** B1 lives in the shared iterator, so every output path must inherit the fix, not just {@code split}. */
    @Nested
    public class EveryOutputPathGetsTheFix {

        private Splitter fresh() {
            return Splitter.with(',').omitEmptyStrings().limit(2);
        }

        @Test
        public void allSevenOutputPathsAgree() {
            final List<String> expected = Arrays.asList("a", "b,c");
            final String src = "a,,,b,c";

            assertEquals(expected, fresh().split(src));
            assertEquals(expected, fresh().splitToStream(src).toList());
            assertArrayEquals(expected.toArray(new String[0]), fresh().splitToArray(src));
            assertEquals(expected, fresh().splitToImmutableList(src));
            assertEquals(expected, fresh().splitToCollection(src, ArrayList::new));
            assertEquals(expected, fresh().splitThenApply(src, l -> l));

            final List<String> viaForEach = new ArrayList<>();
            fresh().splitThenForEach(src, viaForEach::add);
            assertEquals(expected, viaForEach);

            final List<String> viaOutput = new ArrayList<>();
            fresh().splitInto(src, viaOutput);
            assertEquals(expected, viaOutput);
        }

        @Test
        public void theIteratorProtocolHoldsAfterExhaustion() {
            final ObjIterator<String> it = fresh().splitToStream("a,,b").iterator();
            final List<String> seen = new ArrayList<>();

            while (it.hasNext()) {
                seen.add(it.next());
            }

            assertEquals(Arrays.asList("a", "b"), seen);
            // hasNext() must stay false and stay cheap; next() must throw rather than return null.
            assertFalse(it.hasNext());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }

    /** B1 as it reached {@code MapSplitter}, whose entry splitter always omits empty entries. */
    @Nested
    public class MapSplitterLimitNoLongerAbsorbsEmptyEntries {

        @Test
        public void theSecondKeyIsNotPrefixedWithTheEntryDelimiter() {
            final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("a", "1");
            expected.put("b", "2,c=3");

            // Used to produce the key ",b".
            assertEquals(expected, MapSplitter.with(",", "=").limit(2).split("a=1,,b=2,c=3"));
        }

        @Test
        public void leadingEmptyEntriesAreDropped() {
            final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("a", "1");

            // Used to produce the key ",,a".
            assertEquals(expected, MapSplitter.with(",", "=").limit(1).split(",,a=1"));
        }

        @Test
        public void anAllDelimiterInputNoLongerThrows() {
            // Adding limit(n) used to turn this valid, empty input into an IllegalArgumentException
            // ("Invalid map entry String: \",,\""), while the same input without a limit returned {}.
            assertTrue(MapSplitter.with(",", "=").limit(1).split(",,").isEmpty());
            assertTrue(MapSplitter.with(",", "=").split(",,").isEmpty());
        }

        @Test
        public void theLimitStillCapsTheEntryCount() {
            assertEquals(2, MapSplitter.with(",", "=").limit(2).split("a=1,,b=2,,c=3,,d=4").size());
        }

        @Test
        public void patternDelimitedMapSplittersGetTheFixToo() {
            final Map<String, String> expected = new LinkedHashMap<>();
            expected.put("a", "1");
            expected.put("b", "2;c=3");

            assertEquals(expected, MapSplitter.pattern("[,;]", "[=:]").limit(2).split("a=1,,b:2;c=3"));
        }
    }

    /** The three engines must stay observationally identical for a literal delimiter. */
    @Nested
    public class TheThreeEnginesAgree {

        @ParameterizedTest(name = "[{index}] src=''{0}'' limit={1} omit={2} trim={3}")
        @CsvSource({ "',,a,b', 1, true, false", "',,a,b,c', 2, true, true", "'a,,,b,c', 2, true, false", "'a,b,,,', 3, true, true", "',', 1, true, false",
                "'', 1, true, false", "'a', 3, false, false", "',a,', 1, true, true", "'a,,b', 2, true, false", "' a , , b ', 2, true, true",
                "'a,b,c', 2, false, false", "',,a,b,c', 2, false, false" })
        public void charStringAndPatternEnginesProduceTheSameTokens(final String src, final int limit, final boolean omit, final boolean trim) {
            final List<String> byChar = configure(Splitter.with(','), omit, trim, limit).split(src);
            final List<String> byPattern = configure(Splitter.with(Pattern.compile(",")), omit, trim, limit).split(src);
            // The string engine only takes its own code path for a multi-character delimiter, so run the
            // structurally identical input with "," widened to "::".
            final List<String> byString = configure(Splitter.with("::"), omit, trim, limit).split(src.replace(",", "::"))
                    .stream()
                    .map(s -> s.replace("::", ",")) // an absorbed remainder carries the widened delimiter
                    .toList();

            assertEquals(byChar, byPattern, "char vs pattern");
            assertEquals(byChar, byString, "char vs string");
        }

        private Splitter configure(final Splitter s, final boolean omit, final boolean trim, final int limit) {
            if (omit) {
                s.omitEmptyStrings();
            }

            if (trim) {
                s.trimResults();
            }

            return s.limit(limit);
        }
    }

    /** D1: the supplier overloads were renamed so a constructor reference is no longer ambiguous. */
    @Nested
    public class SupplierOverloadsTakeAConstructorReference {

        /** Failing to compile is the regression this guards. */
        @Test
        public void splitToCollectionAcceptsAnInlineConstructorReference() {
            final LinkedHashSet<String> set = Splitter.with(",").splitToCollection("a,b,a,c", LinkedHashSet::new);
            assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));

            final LinkedList<Integer> ints = Splitter.with(",").splitToCollection("1,2,3", Integer.class, LinkedList::new);
            assertEquals(Arrays.asList(1, 2, 3), ints);

            final LinkedList<Integer> viaType = Splitter.with(",").splitToCollection("1,2,3", CommonUtil.typeOf(Integer.class), LinkedList::new);
            assertEquals(Arrays.asList(1, 2, 3), viaType);
        }

        @Test
        public void splitToMapAcceptsAnInlineConstructorReference() {
            final TreeMap<String, String> m = MapSplitter.with(",", "=").splitToMap("z=3,a=1,m=2", TreeMap::new);
            assertEquals("{a=1, m=2, z=3}", m.toString());

            final TreeMap<String, Integer> typed = MapSplitter.with(",", "=").splitToMap("z=3,a=1", String.class, Integer.class, TreeMap::new);
            assertEquals("{a=1, z=3}", typed.toString());

            final TreeMap<String, Integer> viaType = MapSplitter.with(",", "=")
                    .splitToMap("z=3,a=1", CommonUtil.typeOf(String.class), CommonUtil.typeOf(Integer.class), TreeMap::new);
            assertEquals("{a=1, z=3}", viaType.toString());
        }

        @Test
        public void theMapperOverloadStillTakesAnInlineMethodReference() {
            assertEquals(Arrays.asList(1, 2, 3), Splitter.with(",").split("1,2,3", Integer::parseInt));
        }

        @Test
        public void nullAndNullReturningSuppliersStillFail() {
            final Supplier<List<String>> nullSupplier = () -> null;

            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitToCollection("a", nullSupplier));
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").splitToMap("a=1", () -> null));
        }
    }

    /** D2: the boolean setters are the only way to turn a flag back off, so they are no longer deprecated. */
    @Nested
    public class BooleanSettersResetAndAreNotDeprecated {

        @Test
        public void eachBooleanSetterTurnsItsFlagBackOff() {
            assertEquals(Arrays.asList(" a ", " b "), Splitter.with(',').trimResults().trim(false).split(" a , b "));
            assertEquals(Arrays.asList(" a ", " b "), Splitter.with(',').stripResults().strip(false).split(" a , b "));
            assertEquals(Arrays.asList("a", "", "b"), Splitter.with(',').omitEmptyStrings().omitEmptyStrings(false).split("a,,b"));
        }

        @Test
        public void mapSplitterBooleanSettersResetToo() {
            final Map<String, String> untrimmed = MapSplitter.with(",", "=").trimResults().trim(false).split(" a = 1 ");
            assertEquals(Collections.singleton(" a "), untrimmed.keySet());
            assertEquals(" 1 ", untrimmed.get(" a "));

            final Map<String, String> unstripped = MapSplitter.with(",", "=").stripResults().strip(false).split(" a = 1 ");
            assertEquals(Collections.singleton(" a "), unstripped.keySet());
            assertEquals(" 1 ", unstripped.get(" a "));
        }

        @Test
        public void noneOfTheFiveBooleanSettersIsDeprecated() throws Exception {
            assertFalse(Splitter.class.getMethod("omitEmptyStrings", boolean.class).isAnnotationPresent(Deprecated.class));
            assertFalse(Splitter.class.getMethod("trim", boolean.class).isAnnotationPresent(Deprecated.class));
            assertFalse(Splitter.class.getMethod("strip", boolean.class).isAnnotationPresent(Deprecated.class));
            assertFalse(MapSplitter.class.getMethod("trim", boolean.class).isAnnotationPresent(Deprecated.class));
            assertFalse(MapSplitter.class.getMethod("strip", boolean.class).isAnnotationPresent(Deprecated.class));
        }
    }

    /** B4/J4/J5/O2: documented edge behaviour that had been mis-stated. */
    @Nested
    public class DocumentedEdgeBehaviour {

        @Test
        public void aZeroWidthPatternIsAcceptedAndSplitsAtItsMatchPositions() {
            // \b matches no characters but does not match the empty input, so the guard accepts it. Unlike
            // String.split, a zero-length match at index 0 is not suppressed here.
            assertEquals(Arrays.asList("", "ab", " ", "cd", ""), Splitter.with(Pattern.compile("\\b")).split("ab cd"));
            assertEquals(Arrays.asList("ab", " ", "cd", ""), Arrays.asList("ab cd".split("\\b", -1)));
        }

        @Test
        public void aPatternMatchingTheEmptyInputIsRejectedWithAMessageThatNamesTheRule() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Splitter.pattern("a*"));
            assertTrue(e.getMessage().contains("must not match the empty input string"), e.getMessage());
        }

        @Test
        public void forLinesSplitsOnEveryUnicodeLineTerminator() {
            // Built from code points rather than written literally, so the file stays free of stray control
            // characters and the intent is explicit.
            for (final int terminator : new int[] { '\n', '\r', 0x0B, '\f', 0x85, 0x2028, 0x2029 }) {
                final String src = "a" + new String(Character.toChars(terminator)) + "b";

                assertEquals(Arrays.asList("a", "b"), Splitter.forLines().split(src), "U+" + Integer.toHexString(terminator));
            }

            // \r\n stays a single separator rather than producing an empty element between them.
            assertEquals(Arrays.asList("a", "b"), Splitter.forLines().split("a\r\nb"));
        }

        @Test
        public void anImmutableOutputCollectionThrowsUnsupportedOperation() {
            assertThrows(UnsupportedOperationException.class, () -> Splitter.with(',').splitInto("a,b", ImmutableList.of("z")));
            assertThrows(UnsupportedOperationException.class, () -> MapSplitter.with(",", "=").splitInto("a=1", ImmutableMap.of("z", "0")));
        }

        @Test
        public void anImmutableOutputIsUntouchedWhenNoElementIsProduced() {
            // The tag says "and at least one element is produced": a null source produces none, so no throw.
            Splitter.with(',').splitInto(null, ImmutableList.of("z"));
            MapSplitter.with(",", "=").splitInto(null, ImmutableMap.of("z", "0"));
        }
    }

    /**
     * Cycle-1 findings: behaviour that was correct but undocumented, now pinned so the new javadoc cannot
     * drift from it.
     */
    @Nested
    public class DocumentedButPreviouslyUnpinnedBehaviour {

        /** C-001: a multi-dimensional arrayType converts each token to the nested array type. */
        @Test
        public void aMultiDimensionalArrayTypeConvertsEachTokenToTheNestedType() {
            final String[][] nested = Splitter.with(',').splitToArray("a,b", String[][].class);

            assertEquals(2, nested.length);
            assertArrayEquals(new String[] { "a" }, nested[0]);
            assertArrayEquals(new String[] { "b" }, nested[1]);

            // A nested primitive type propagates the conversion failure rather than silently zero-filling.
            assertThrows(NumberFormatException.class, () -> Splitter.with(',').splitToArray("a,b", int[][].class));

            // Empty and null sources stay well defined at the nested type.
            assertEquals(1, Splitter.with(',').splitToArray("", String[][].class).length);
            assertEquals(0, Splitter.with(',').splitToArray(null, String[][].class).length);
        }

        /** C-002: the char delimiter is one UTF-16 code unit, so a lone surrogate splits inside a pair. */
        @Test
        public void aCharDelimiterMatchesOneCodeUnitNotOneCodePoint() {
            final String emoji = new String(Character.toChars(0x1F600)); // supplementary, 2 chars
            final String src = "a" + emoji + "b";

            // The supplementary character works as a delimiter through with(CharSequence), which matches the pair.
            assertEquals(Arrays.asList("a", "b"), Splitter.with(emoji).split(src));

            // A lone high surrogate splits inside the pair, leaving unpaired halves.
            final List<String> broken = Splitter.with(emoji.charAt(0)).split(src);
            assertEquals(2, broken.size());
            assertEquals("a", broken.get(0));
            assertEquals(emoji.charAt(1) + "b", broken.get(1));

            // A supplementary character in the DATA is never damaged by an unrelated delimiter.
            assertEquals(Arrays.asList("a", emoji, "b"), Splitter.with(',').split("a," + emoji + ",b"));
            assertEquals(Arrays.asList("a", emoji), Splitter.with(',').stripResults().split("a, " + emoji + " "));
        }

        /** C-003: entry-level config is captured at stream creation, key/value-level config on consumption. */
        @Test
        public void mapSplitterStreamCapturesEntryConfigEagerly() {
            final MapSplitter ms = MapSplitter.with(",", "=");
            final List<Map.Entry<String, String>> consumedNow = ms.splitToStream(" a = 1 , b = 2 ").toList();

            assertEquals(2, consumedNow.size());
            assertEquals(" a ", consumedNow.get(0).getKey());
            assertEquals(" 1 ", consumedNow.get(0).getValue());

            // Same splitter, trimming enabled before the stream is built: both halves trim.
            final MapSplitter trimmed = MapSplitter.with(",", "=").trimResults();
            final List<Map.Entry<String, String>> all = trimmed.splitToStream(" a = 1 , b = 2 ").toList();
            assertEquals("a", all.get(0).getKey());
            assertEquals("1", all.get(0).getValue());

            // A null source still yields an empty stream rather than throwing at construction time.
            assertTrue(MapSplitter.with(",", "=").splitToStream(null).toList().isEmpty());
        }
    }

    /** Guards the shapes the rest of the suite depends on: ordinary splitting is unchanged. */
    @Test
    public void ordinarySplittingIsUnchanged() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').split("a,b,c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with("::").split("a::b::c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(Pattern.compile(",")).split("a,b,c"));
        assertEquals(Arrays.asList(""), Splitter.with(',').split(""));
        assertEquals(Collections.emptyList(), Splitter.with(',').split((CharSequence) null));
        assertEquals(Arrays.asList("a", ""), Splitter.with(',').split("a,"));
        assertEquals(Arrays.asList("", "a"), Splitter.with(',').split(",a"));
        assertEquals(Arrays.asList("one", "two", "three four five"), Splitter.pattern("\\s+").limit(3).split("one two three four five"));
        assertEquals(Arrays.asList("a", "b,c"), Splitter.with(',').limit(2).split("a,b,c"));

        final Function<String, Integer> toInt = Integer::parseInt;
        assertEquals(Arrays.asList(1, 2, 3), Splitter.with(',').split("1,2,3", toInt));
    }
}
