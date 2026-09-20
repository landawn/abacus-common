package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Splitter.MapSplitter;

/**
 * Regression tests for the 2026-08-31 {@link Splitter} review. One nested section per finding.
 */
public class SplitterRegressionATest extends TestBase {

    /** J4/D1 (doc half): configuration mutates in place and returns {@code this}, never a copy. */
    @Nested
    public class ConfigurationMutatesInPlace extends TestBase {

        @Test
        public void everyConfigurationMethodReturnsTheSameInstance() {
            final Splitter s = Splitter.with(',');

            assertSame(s, s.omitEmptyStrings());
            assertSame(s, s.trimResults());
            assertSame(s, s.stripResults());
            assertSame(s, s.limit(3));
        }

        @Test
        public void aDerivedVariantIsTheSameSplitter() {
            final Splitter base = Splitter.with(',');
            final Splitter trimmed = base.trimResults();

            assertSame(base, trimmed);
            assertEquals(Arrays.asList("a", "b"), base.split(" a , b "));
        }
    }

    /** J3: a {@code null} source yields nothing; an empty source yields one empty token. */
    @Nested
    public class NullSourceVersusEmptySource extends TestBase {

        @Test
        public void nullSourceYieldsNoElements() {
            assertEquals(0, Splitter.with(",").split((CharSequence) null).size());
            assertEquals(0, Splitter.with(",").splitToArray((CharSequence) null).length);
            assertEquals(0, Splitter.with(",").splitToStream((CharSequence) null).count());
        }

        @Test
        public void emptySourceYieldsOneEmptyToken() {
            final List<String> parts = Splitter.with(",").split("");

            assertEquals(1, parts.size());
            assertEquals("", parts.get(0));
            assertEquals(1, Splitter.with(",").splitToArray("").length);
            assertEquals(1, Splitter.with(",").splitToStream("").count());
        }

        @Test
        public void omitEmptyStringsDropsTheEmptyToken() {
            assertEquals(0, Splitter.with(",").omitEmptyStrings().split("").size());
        }

        @Test
        public void sameForCharStringAndPatternDelimiters() {
            assertEquals(1, Splitter.with(',').split("").size());
            assertEquals(1, Splitter.with("::").split("").size());
            assertEquals(1, Splitter.with(Pattern.compile(",")).split("").size());
        }

        @Test
        public void emptyTokenConvertsToZeroForPrimitiveComponentsAndNullForBoxed() {
            assertArrayEquals(new int[] { 1, 0, 3 }, Splitter.with(",").splitToArray("1,,3", int[].class));
            assertEquals(Arrays.asList(1, null, 3), Splitter.with(",").split("1,,3", Integer.class));
        }

        @Test
        public void emptySourceConvertsToASingleZeroForAPrimitiveArray() {
            assertArrayEquals(new int[] { 0 }, Splitter.with(",").splitToArray("", int[].class));
        }
    }

    /** Cleanup: the multi-character delimiter's char[] is extracted once, not per split call. */
    @Nested
    public class MultiCharDelimiterSplittingIsUnchanged extends TestBase {

        @Test
        public void repeatedSplitsOnACachedSplitterAgree() {
            final Splitter s = Splitter.with("::");

            for (int i = 0; i < 3; i++) {
                assertEquals(Arrays.asList("a", "b", "c"), s.split("a::b::c"));
            }
        }

        @Test
        public void partialDelimiterMatchesAreNotSplitPoints() {
            assertEquals(Arrays.asList("a:b"), Splitter.with("::").split("a:b"));
            assertEquals(Arrays.asList("a", ":b"), Splitter.with("::").split("a:::b"));
        }

        @Test
        public void sourceShorterThanTheDelimiter() {
            assertEquals(Arrays.asList("a"), Splitter.with("::::").split("a"));
            assertEquals(Arrays.asList(""), Splitter.with("::").split(""));
        }

        @Test
        public void trailingAndLeadingDelimiters() {
            assertEquals(Arrays.asList("", "a", ""), Splitter.with("::").split("::a::"));
        }

        @Test
        public void interactsCorrectlyWithLimitTrimAndOmitEmpty() {
            assertEquals(Arrays.asList("a", "b::c"), Splitter.with("::").limit(2).split("a::b::c"));
            assertEquals(Arrays.asList("a", "b"), Splitter.with("::").trimResults().split(" a :: b "));
            assertEquals(Arrays.asList("a", "b"), Splitter.with("::").omitEmptyStrings().split("a::::b"));
        }
    }

    /** Cleanup: the pattern splitter's duplicated "no more matches" branch was collapsed into one. */
    @Nested
    public class PatternSplittingIsUnchanged extends TestBase {

        @Test
        public void basicPatternSplit() {
            assertEquals(Arrays.asList("one", "two", "three"), Splitter.pattern("\\s+").split("one  two   three"));
            assertEquals(Arrays.asList("a", "b", "c"), Splitter.pattern("[,;]").split("a,b;c"));
        }

        @Test
        public void noMatchReturnsTheWholeInput() {
            assertEquals(Arrays.asList("abc"), Splitter.pattern(",").split("abc"));
        }

        @Test
        public void trailingAndLeadingMatches() {
            assertEquals(Arrays.asList("", "a", ""), Splitter.pattern(",").split(",a,"));
        }

        @Test
        public void zeroWidthLookaheadStillTerminatesAndSplits() {
            assertEquals(Arrays.asList("a", ",b"), Splitter.with(Pattern.compile("(?=,)")).split("a,b"));
        }

        @Test
        public void zeroWidthLookbehindStillTerminatesAndSplits() {
            assertEquals(Arrays.asList("a,", "b"), Splitter.with(Pattern.compile("(?<=,)")).split("a,b"));
        }

        @Test
        public void patternWithLimitTrimAndOmitEmpty() {
            assertEquals(Arrays.asList("a", "b,c"), Splitter.pattern(",").limit(2).split("a,b,c"));
            assertEquals(Arrays.asList("a", "b"), Splitter.pattern(",").trimResults().split(" a , b "));
            assertEquals(Arrays.asList("a", "b"), Splitter.pattern(",").omitEmptyStrings().split("a,,b"));
        }

        @Test
        public void limitOneTakesTheWholeInputThroughTheFastPath() {
            assertEquals(Arrays.asList("a,b,c"), Splitter.pattern(",").limit(1).split("a,b,c"));
            assertEquals(Arrays.asList("a::b"), Splitter.with("::").limit(1).split("a::b"));
            assertEquals(Arrays.asList("a,b"), Splitter.with(',').limit(1).split("a,b"));
        }

        @Test
        public void limitLargerThanTheTokenCountReturnsEverything() {
            assertEquals(Arrays.asList("a", "b", "c"), Splitter.pattern(",").limit(99).split("a,b,c"));
        }

        @Test
        public void aMatchAtPositionZeroYieldsALeadingEmptyToken() {
            assertEquals(Arrays.asList("", "a"), Splitter.pattern(",").split(",a"));
            assertEquals(Arrays.asList("a"), Splitter.pattern(",").omitEmptyStrings().split(",a"));
        }

        @Test
        public void patternMatchingTheEmptyStringIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile("x*")));
        }

        @Test
        public void forLinesHandlesAllLineSeparators() {
            assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), Splitter.forLines().split("line1\nline2\r\nline3\rline4"));
        }
    }

    /**
     * The multi-character delimiter's {@code char[]} is now shared by every iterator the Splitter creates, so a
     * configured instance must still be safe for concurrent read-only splitting (as the class javadoc promises).
     */
    @Test
    public void aSharedMultiCharSplitterIsSafeForConcurrentReadOnlyUse() throws Exception {
        final Splitter shared = Splitter.with("::").trimResults();
        final int threads = 8;
        final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);

        try {
            final java.util.List<java.util.concurrent.Future<Boolean>> futures = new java.util.ArrayList<>();

            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < 2000; i++) {
                        if (!Arrays.asList("a", "b", "c").equals(shared.split(" a :: b :: c "))) {
                            return false;
                        }
                    }
                    return true;
                }));
            }

            for (final java.util.concurrent.Future<Boolean> f : futures) {
                assertTrue(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** D10: {@code WHITE_SPACE_PATTERN} is a constant to hand to {@code with(Pattern)}, not a built-in mode. */
    @Test
    public void whiteSpacePatternIsUsableAsADelimiter() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(Splitter.WHITE_SPACE_PATTERN).split("a  b\tc"));
    }

    /** D4 + cleanup: the mapper overloads are variance-correct and no longer reuse a raw list. */
    @Nested
    public class MapperOverloads extends TestBase {

        @Test
        public void splitWithMapperProducesTheMappedList() {
            final Function<String, Integer> toInteger = Integer::parseInt;

            assertEquals(Arrays.asList(1, 2, 3), Splitter.with(",").split("1,2,3", toInteger));
        }

        @Test
        public void splitWithMapperAcceptsAWiderReturnType() {
            final Function<String, Object> f = x -> x;

            assertEquals(Arrays.asList("a", "b"), Splitter.with(",").split("a,b", f));
        }

        @Test
        public void splitWithMapperTolerateNullResults() {
            final Function<String, String> toNull = x -> null;

            assertEquals(Arrays.asList((String) null, null), Splitter.with(",").split("a,b", toNull));
        }

        @Test
        public void splitWithMapperOnANullSourceReturnsAnEmptyList() {
            final Function<String, Integer> toInteger = Integer::parseInt;

            assertTrue(Splitter.with(",").split((CharSequence) null, toInteger).isEmpty());
        }

        @Test
        public void splitToArrayAcceptsAnExtendsStringMapper() {
            final Function<String, String> upper = String::toUpperCase;

            assertArrayEquals(new String[] { "A", "B" }, Splitter.with(",").splitToArray("a,b", upper));
        }

        @Test
        public void splitToArrayWithMapperRejectsNullMapper() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitToArray("a,b", (Function<String, String>) null));
        }

        @Test
        public void splitWithMapperRejectsNullMapper() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").split("a,b", (Function<String, String>) null));
        }
    }

    /**
     * Cleanup: the unreachable third-token check is gone (the key-value splitter is limited to two parts), the
     * malformed-entry check remains, and the message now says what is wrong.
     */
    @Nested
    public class MapSplitterEntryValidation extends TestBase {

        @Test
        public void anEntryWithoutTheDelimiterIsRejectedWithADescriptiveMessage() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1,b"));

            assertTrue(ex.getMessage().contains("\"b\""), ex.getMessage());
            assertTrue(ex.getMessage().contains("key-value delimiter"), ex.getMessage());
        }

        @Test
        public void extraDelimitersStayInTheValue() {
            assertEquals(map("a", "1", "b", "2=3"), MapSplitter.with(",", "=").split("a=1,b=2=3"));
        }

        @Test
        public void extraDelimitersStayInTheValueForTheTypedOverload() {
            assertEquals(map("a", "1", "b", "2=3"), MapSplitter.with(",", "=").split("a=1,b=2=3", String.class, String.class));
        }

        @Test
        public void extraDelimitersStayInTheValueForTheStreamOverload() {
            final Map<String, String> m = MapSplitter.with(",", "=").splitToStream("a=1,b=2=3").toMap(Map.Entry::getKey, Map.Entry::getValue);

            assertEquals("1", m.get("a"));
            assertEquals("2=3", m.get("b"));
        }

        @Test
        public void malformedEntryIsRejectedByTheTypedOverloadToo() {
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1,b", String.class, String.class));
        }

        @Test
        public void malformedEntryIsRejectedByTheStreamOverloadToo() {
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").splitToStream("a=1,b").toList());
        }

        @Test
        public void anEmptyEntryIsRejectedOnceEmptyEntriesAreKept() {
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").omitEmptyStrings(false).split("a=1,,b=2"));
        }

        @Test
        public void emptyEntriesAreOmittedByDefault() {
            assertEquals(map("a", "1", "b", "2"), MapSplitter.with(",", "=").split("a=1,,b=2"));
        }

        @Test
        public void anEmptySourceYieldsAnEmptyMap() {
            assertTrue(MapSplitter.with(",", "=").split("").isEmpty());
        }

        @Test
        public void aNullSourceYieldsAnEmptyMap() {
            assertTrue(MapSplitter.with(",", "=").split((CharSequence) null).isEmpty());
        }

        @Test
        public void insertionOrderIsPreserved() {
            // assertEquals on two Maps is order-insensitive, so compare the key sequence explicitly.
            assertEquals(Arrays.asList("z", "a", "m"), new ArrayList<>(MapSplitter.with(",", "=").split("z=1,a=2,m=3").keySet()));
        }

        @Test
        public void emptyKeysAndValuesArePreserved() {
            assertEquals(map("", "1"), MapSplitter.with(",", "=").split("=1"));
            assertEquals(map("a", ""), MapSplitter.with(",", "=").split("a="));
        }

        @Test
        public void streamOverloadIsIdempotentAcrossRepeatedHasNext() {
            final ObjIterator<Map.Entry<String, String>> iter = MapSplitter.with(",", "=").splitToStream("a=1,b=2").iterator();

            assertTrue(iter.hasNext());
            assertTrue(iter.hasNext());
            assertEquals("a", iter.next().getKey());
            assertTrue(iter.hasNext());
            assertEquals("b", iter.next().getKey());
            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
        }

        @Test
        public void splitToEntryStreamUsesTheSameRewrittenIterator() {
            assertEquals(map("a", "1", "c", "3"), MapSplitter.with(",", "=").splitToEntryStream("a=1,b=2,c=3").filter(e -> !"b".equals(e.getKey())).toMap());
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").splitToEntryStream("a=1,b").toList());
        }

        @Test
        public void limitAbsorbsTheRemainderIntoTheLastEntry() {
            assertEquals(map("a", "1", "b", "2,c=3"), MapSplitter.with(",", "=").limit(2).split("a=1,b=2,c=3"));
        }

        /** Doc pin: the N-th entry always extends to the end of the input, absorbing whatever follows it. */
        @Test
        public void limitAbsorbsWhateverFollowsTheNthEntry() {
            assertEquals(map("a", "1", "b", "2,"), MapSplitter.with(",", "=").limit(2).split("a=1,b=2,"));
            assertEquals(map("a", "1", "b", "2,,,"), MapSplitter.with(",", "=").limit(2).split("a=1,b=2,,,"));
            assertEquals(map("a", "1,"), MapSplitter.with(",", "=").limit(1).split("a=1,"));

            // Nothing is left after the N-th entry here, so its value is untouched.
            assertEquals(map("a", "1", "b", "2"), MapSplitter.with(",", "=").limit(3).split("a=1,b=2,"));
            assertEquals(map("a", "1", "b", "2"), MapSplitter.with(",", "=").limit(2).split("a=1,b=2"));

            // A leading or internal empty entry is dropped before the limit is counted, so it is not absorbed.
            assertEquals(map("a", "1", "b", "2"), MapSplitter.with(",", "=").limit(2).split("a=1,,b=2"));
            assertEquals(map("a", "1", "b", "2"), MapSplitter.with(",", "=").limit(2).split(",,a=1,b=2"));

            // ... but only while empty entries are omitted, which is merely the default: kept, the internal empty
            // entry is counted and absorbed, and it lands in the N-th entry's KEY.
            assertEquals(map("a", "1", ",b", "2"), MapSplitter.with(",", "=").omitEmptyStrings(false).limit(2).split("a=1,,b=2"));
        }

        /** Doc pin: the residue absorbed by the N-th entry lands in its KEY when its key-value delimiter follows the cut. */
        @Test
        public void limitResidueLandsInTheKeyWhenTheEntryDelimiterFollowsIt() {
            // The N-th entry is "b,c=2" -> split on its FIRST "=", so the residue ",c" ends up inside the key.
            assertEquals(map("a", "1", "b,c", "2"), MapSplitter.with(",", "=").limit(2).split("a=1,b,c=2"));
            assertEquals(map("a", "1", "b,c", "2,d=3"), MapSplitter.with(",", "=").limit(2).split("a=1,b,c=2,d=3"));

            // Control: with the delimiter before the cut the residue lands in the value, as the bullet's example says.
            assertEquals(map("a", "1", "b", "2,c=3"), MapSplitter.with(",", "=").limit(2).split("a=1,b=2,c=3"));
        }

        /** Doc pin: the limit caps the entries PARSED, not the size of the returned map. */
        @Test
        public void limitCapsEntriesParsedNotTheSizeOfTheMap() {
            assertEquals(map("a", "2,b=3"), MapSplitter.with(",", "=").limit(2).split("a=1,a=2,b=3"));
            assertEquals(1, MapSplitter.with(",", "=").limit(2).split("a=1,a=2,b=3").size());
            assertEquals(map("a", "3"), MapSplitter.with(",", "=").limit(3).split("a=1,a=2,a=3"));

            // Distinct keys, so this one really does yield N entries - the javadoc example is accurate.
            assertEquals(2, MapSplitter.with(",", "=").limit(2).split("a=1,b=2,c=3").size());
        }

        @Test
        public void theDocumentedLimitTrimOmitEmptyExample() {
            // Updated with the omitEmptyStrings()/limit() ordering fix: the empty entry is dropped before the
            // limit is counted, so the final entry starts at "b" instead of absorbing the omitted entry.
            assertEquals(map("a", "1", "b", "2 , c = 3"),
                    MapSplitter.with(",", "=").limit(2).trimResults().omitEmptyStrings().split(" a = 1 , , b = 2 , c = 3 "));
        }
    }

    /** Guards behaviour the rest of the suite depends on: split output is unchanged for the common shapes. */
    @Test
    public void commonSplitShapesAreUnchanged() {
        assertEquals(Arrays.asList("apple", "banana", "cherry"), Splitter.with(",").split("apple,banana,cherry"));
        assertEquals(Arrays.asList("apple", "banana", "cherry"), Splitter.with(",").trimResults().omitEmptyStrings().split("  apple,  , banana , cherry  "));
        assertEquals(Arrays.asList("one", "two", "three four five"), Splitter.pattern("\\s+").limit(3).split("one two three four five"));
        assertNull(Splitter.with(",").split("1,,3", Integer.class).get(1));
    }

    /** Doc pin for the class javadoc's "Output Methods" list: the Collection-supplier overloads are splitToCollection. */
    @Test
    public void collectionSupplierOverloadsAreNamedSplitToCollection() throws Exception {
        // getMethod throws NoSuchMethodException when the method is absent and never returns null, so the call
        // itself - propagating out of this throws-Exception method - is the assertion.
        Splitter.class.getMethod("splitToCollection", CharSequence.class, Supplier.class);
        assertThrows(NoSuchMethodException.class, () -> Splitter.class.getMethod("split", CharSequence.class, Supplier.class));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(",").splitToCollection("a,b", ArrayList::new));
    }

    private static Map<String, String> map(final String... kv) {
        final Map<String, String> m = new LinkedHashMap<>();

        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }

        return m;
    }
}
