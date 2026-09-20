package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Splitter.MapSplitter;

/**
 * Regression tests for the 2026-09-01 {@link Splitter} review. One nested section per finding.
 */
public class SplitterRegressionBTest extends TestBase {

    /**
     * D1: the output overloads no longer declare a type variable in argument position, so an inline lambda or
     * method reference is no longer ambiguous. <b>This whole section failing to compile is the regression.</b>
     */
    @Nested
    public class InlineLambdasAndMethodReferencesCompile extends TestBase {

        @Test
        public void mapperOverloadTakesAMethodReference() {
            assertEquals(Arrays.asList(1, 2, 3), Splitter.with(",").split("1,2,3", Integer::parseInt));
        }

        @Test
        public void mapperOverloadTakesAnInlineLambda() {
            assertEquals(Arrays.asList(1, 2), Splitter.with(",").split("1,2", s -> Integer.parseInt(s)));
            assertEquals(Arrays.asList("A", "B"), Splitter.with(",").split("a,b", s -> s.toUpperCase()));
        }

        @Test
        public void splitIntoStillAcceptsEveryCollectionShape() {
            final List<String> list = new ArrayList<>();
            Splitter.with(",").splitInto("a,b", list);
            assertEquals(Arrays.asList("a", "b"), list);

            final Collection<String> set = new LinkedHashSet<>();
            Splitter.with(",").splitInto("a,b,a", set);
            assertEquals(2, set.size());

            final List<Integer> ints = new ArrayList<>();
            Splitter.with(",").splitInto("1,2", Integer.class, ints);
            assertEquals(Arrays.asList(1, 2), ints);

            final List<Integer> viaType = new ArrayList<>();
            Splitter.with(",").splitInto("3,4", CommonUtil.typeOf(Integer.class), viaType);
            assertEquals(Arrays.asList(3, 4), viaType);
        }

        @Test
        public void mapSplitterSplitIntoStillAcceptsEveryMapShape() {
            final Map<String, String> map = new LinkedHashMap<>();
            MapSplitter.with(",", "=").splitInto("a=1,b=2", map);
            assertEquals("{a=1, b=2}", map.toString());

            final Map<String, String> sorted = new TreeMap<>();
            MapSplitter.with(",", "=").splitInto("z=1,a=2", sorted);
            assertEquals("{a=2, z=1}", sorted.toString());

            final Map<Integer, String> typed = new LinkedHashMap<>();
            MapSplitter.with(",", "=").splitInto("1=a,2=b", Integer.class, String.class, typed);
            assertEquals("{1=a, 2=b}", typed.toString());

            final Map<Integer, String> viaType = new LinkedHashMap<>();
            MapSplitter.with(",", "=").splitInto("3=c", CommonUtil.typeOf(Integer.class), CommonUtil.typeOf(String.class), viaType);
            assertEquals("{3=c}", viaType.toString());
        }

        @Test
        public void explicitTypeArgumentsOnTheSupplierOverloadStillWork() {
            final java.util.function.Supplier<LinkedHashSet<String>> factory = LinkedHashSet::new;

            assertEquals(2, Splitter.with(",").splitToCollection("a,b,a", factory).size());
        }
    }

    /** B4: a Supplier returning {@code null} used to produce the bare message {@code "supplier result"}. */
    @Nested
    public class SupplierReturningNullMessage extends TestBase {

        @Test
        public void splitterSupplierMessageIsASentence() {
            final java.util.function.Supplier<List<String>> nullSupplier = () -> null;

            for (final Executable call : new Executable[] { //
                    () -> Splitter.with(",").splitToCollection("a", nullSupplier), //
                    () -> Splitter.with(",").splitToCollection("a", Integer.class, () -> null), //
                    () -> Splitter.with(",").splitToCollection("a", CommonUtil.typeOf(Integer.class), () -> null) }) {
                final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call::run);

                assertNotNull(ex.getMessage());
                assertTrue(ex.getMessage().contains("must not return null"), ex.getMessage());
            }
        }

        @Test
        public void mapSplitterSupplierMessageIsASentence() {
            final java.util.function.Supplier<Map<String, String>> nullSupplier = () -> null;

            for (final Executable call : new Executable[] { //
                    () -> MapSplitter.with(",", "=").splitToMap("a=1", nullSupplier), //
                    () -> MapSplitter.with(",", "=").splitToMap("a=1", String.class, String.class, () -> null), //
                    () -> MapSplitter.with(",", "=").splitToMap("a=1", CommonUtil.typeOf(String.class), CommonUtil.typeOf(String.class), () -> null) }) {
                final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, call::run);

                assertNotNull(ex.getMessage());
                assertTrue(ex.getMessage().contains("must not return null"), ex.getMessage());
            }
        }

        /** Local functional interface so the loop above stays readable. */
        private interface Executable {
            void run();
        }
    }

    /** D6: an empty output array is now a no-op instead of an error; {@code null} still throws. */
    @Nested
    public class EmptyOutputArrayIsANoOp extends TestBase {

        @Test
        public void emptyArrayIsAccepted() {
            final String[] output = new String[0];

            Splitter.with(",").splitInto("a,b,c", output);

            assertEquals(0, output.length);
        }

        @Test
        public void nullArrayStillThrows() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitInto("a", (String[]) null));
        }

        @Test
        public void partialFillIsUnchanged() {
            final String[] tooSmall = new String[3];
            Splitter.with(",").splitInto("a,b,c,d", tooSmall);
            assertArrayEquals(new String[] { "a", "b", "c" }, tooSmall);

            final String[] tooLarge = { "x", "y", "z" };
            Splitter.with(",").splitInto("a", tooLarge);
            assertArrayEquals(new String[] { "a", "y", "z" }, tooLarge);
        }
    }

    /** D7: trim and strip are mutually exclusive and the last one called wins, matching {@link Joiner}. */
    @Nested
    public class TrimAndStripAreLastCallWins extends TestBase {

        private static final String SOURCE = "a\t, b ";

        @Test
        public void stripAfterTrimSelectsStrip() {
            assertEquals(Arrays.asList("a", "b"), Splitter.with(',').trimResults().stripResults().split(SOURCE));
        }

        @Test
        public void trimAfterStripSelectsTrim() {
            // Pre-fix this returned ["a", "b"] because strip won regardless of order.
            assertEquals(Arrays.asList("a\t", "b"), Splitter.with(',').stripResults().trimResults().split(SOURCE));
        }

        @Test
        public void deprecatedBooleanFormsFollowTheSameRule() {
            assertEquals(Arrays.asList("a", "b"), Splitter.with(',').trim(true).strip(true).split(SOURCE));
            assertEquals(Arrays.asList("a\t", "b"), Splitter.with(',').strip(true).trim(true).split(SOURCE));
        }

        @Test
        public void passingFalseOnlyClearsItsOwnMode() {
            assertEquals(Arrays.asList("a", "b"), Splitter.with(',').stripResults().trim(false).split(SOURCE));
            assertEquals(Arrays.asList("a\t", "b"), Splitter.with(',').trimResults().strip(false).split(SOURCE));
        }

        @Test
        public void matchesJoinersPrecedence() {
            assertEquals("a", Joiner.with(",").trimBeforeAppend().stripBeforeAppend().append(" a ").toString());
            assertEquals(" a ", Joiner.with(",").stripBeforeAppend().trimBeforeAppend().append(" a ").toString());
            assertEquals(Arrays.asList(" a "), Splitter.with(',').stripResults().trimResults().split(" a "));
        }

        @Test
        public void mapSplitterInheritsTheRule() {
            assertEquals("{a=1}", MapSplitter.with(",", "=").trimResults().stripResults().split("\ta\t=\t1\t").toString());
            assertEquals("{\ta\t=\t1\t}", MapSplitter.with(",", "=").stripResults().trimResults().split("\ta\t=\t1\t").toString());
        }
    }

    /** B2 (documentation half): {@code trimResults()} removes only {@code U+0020}. */
    @Nested
    public class TrimResultsIsSpaceOnly extends TestBase {

        @Test
        public void tabsAndNewlinesSurviveTrimButNotStrip() {
            assertEquals(Arrays.asList("a\t", "b"), Splitter.with(',').trimResults().split("a\t, b"));
            assertEquals(Arrays.asList("a", "b"), Splitter.with(',').stripResults().split("a\t, b"));
            assertEquals(Arrays.asList("\nx\n"), Splitter.with(',').trimResults().split("\nx\n"));
            assertEquals(Arrays.asList("x"), Splitter.with(',').stripResults().split("\nx\n"));
        }

        @Test
        public void stripDoesNotRemoveTheNonBreakingSpace() {
            // Character.isWhitespace(' ') is false - documented on stripResults().
            assertEquals(Arrays.asList(" a "), Splitter.with(',').stripResults().split(" a "));
        }

        @Test
        public void streamAndSeqLineSplittingNowMatchTheirOwnContract() {
            // Downstream half of the same finding: a tab-only line must be trimmed away, not kept.
            final String source = "  line1  \n\t\n  line2  ";

            assertEquals(Arrays.asList("line1", "line2"), com.landawn.abacus.util.stream.Stream.splitToLines(source, true, true).toList());
            assertEquals(Arrays.asList("  line1  ", "\t", "  line2  "), com.landawn.abacus.util.stream.Stream.splitToLines(source, false, true).toList());
            assertEquals(Arrays.asList("line1", "", "line2"), com.landawn.abacus.util.stream.Stream.splitToLines(source, true, false).toList());
        }
    }

    /** J2: the empty-token conversion rule, previously documented only on {@code splitToArray}. */
    @Nested
    public class EmptyTokenConversion extends TestBase {

        @Test
        public void boxedTypesConvertAnEmptyTokenToNull() {
            assertEquals(Arrays.asList(1, null, 3), Splitter.with(",").split("1,,3", Integer.class));
            assertEquals(Arrays.asList((Integer) null), Splitter.with(",").split("", Integer.class));
            assertEquals(1, Splitter.with(",").split("", Integer.class).size());
        }

        @Test
        public void primitiveArraysConvertAnEmptyTokenToZero() {
            assertArrayEquals(new int[] { 1, 0, 3 }, (int[]) Splitter.with(",").splitToArray("1,,3", int[].class));
            assertArrayEquals(new Integer[] { 1, null, 3 }, (Integer[]) Splitter.with(",").splitToArray("1,,3", Integer[].class));
        }

        @Test
        public void omitEmptyStringsDropsThemInstead() {
            assertEquals(Arrays.asList(1, 3), Splitter.with(",").omitEmptyStrings().split("1,,3", Integer.class));
            assertTrue(Splitter.with(",").omitEmptyStrings().split("", Integer.class).isEmpty());
        }

        @Test
        public void typeOverloadBehavesIdentically() {
            assertEquals(Arrays.asList(1, null, 3), Splitter.with(",").split("1,,3", CommonUtil.typeOf(Integer.class)));
        }

        @Test
        public void nullSourceYieldsNothingWhileEmptySourceYieldsOneToken() {
            assertTrue(Splitter.with(",").split((CharSequence) null, Integer.class).isEmpty());
            assertEquals(1, Splitter.with(",").split("", Integer.class).size());
        }
    }

    /** J3: the limit counts returned substrings, so omitted empty tokens do not consume it. */
    @Nested
    public class LimitInteractionWithOmitEmptyStrings extends TestBase {

        @Test
        public void omittedEmptyTokensDoNotConsumeTheLimit() {
            assertEquals(Arrays.asList("a", "b,c"), Splitter.with(",").omitEmptyStrings().limit(2).split(",,a,b,c"));
            assertArrayEquals(new String[] { "", ",a,b,c" }, ",,a,b,c".split(",", 2));
        }

        @Test
        public void withoutOmitEmptyStringsTheLimitMatchesStringSplit() {
            assertEquals(Arrays.asList("", ",a,b,c"), Splitter.with(",").limit(2).split(",,a,b,c"));
        }

        @Test
        public void trimmingIsAppliedBeforeTheLimitIsCounted() {
            assertEquals(Arrays.asList("a", "b , c"), Splitter.with(",").trimResults().limit(2).split(" a , b , c "));
        }

        @Test
        public void limitMustBePositive() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").limit(0));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").limit(-1));
        }
    }

    /** J4: the key rules a {@code MapSplitter} applies when it builds a {@code Map}. */
    @Nested
    public class MapSplitterKeyRules extends TestBase {

        @Test
        public void duplicateKeysAreLastOneWins() {
            assertEquals("{a=2}", MapSplitter.with(",", "=").split("a=1,a=2").toString());
            assertEquals("{a=2}", MapSplitter.with(",", "=").splitToImmutableMap("a=1,a=2").toString());
        }

        @Test
        public void convertedKeysCanCollideEvenWhenTheTextDiffers() {
            assertEquals("{1=b}", MapSplitter.with(",", "=").split("1=a,01=b", Integer.class, String.class).toString());
        }

        @Test
        public void streamsPreserveEveryEntry() {
            assertEquals(2, MapSplitter.with(",", "=").splitToStream("a=1,a=2").count());
            assertEquals("[a=1, a=2]", MapSplitter.with(",", "=").splitToStream("a=1,a=2").toList().toString());
        }

        @Test
        public void anEmptyKeyIsAccepted() {
            assertEquals("{=1}", MapSplitter.with(",", "=").split("=1").toString());
        }

        @Test
        public void anEntryWithNoDelimiterThrows() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("nodelim"));

            assertTrue(ex.getMessage().contains("nodelim"), ex.getMessage());
        }

        @Test
        public void suppliedSortedMapReordersTheResult() {
            final java.util.function.Supplier<TreeMap<String, String>> factory = TreeMap::new;

            assertEquals("{a=1, m=2, z=3}", MapSplitter.with(",", "=").splitToMap("z=3,a=1,m=2", factory).toString());
        }
    }

    /** J7: {@code MapSplitter.omitEmptyStrings(boolean)} is the only way to keep empty entries, so it is kept. */
    @Nested
    public class MapSplitterKeepsEmptyEntriesOnRequest extends TestBase {

        @Test
        public void emptyEntriesAreOmittedByDefault() {
            assertEquals("{a=1, b=2}", MapSplitter.with(",", "=").split("a=1,,b=2").toString());
        }

        @Test
        public void omitEmptyStringsFalseSurfacesTheEmptyEntryAsAnError() {
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").omitEmptyStrings(false).split("a=1,,b=2"));
            assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").omitEmptyStrings(false).split(""));
        }

        @Test
        public void omitEmptyStringsRestoresTheDefault() {
            assertEquals("{a=1, b=2}", MapSplitter.with(",", "=").omitEmptyStrings(false).omitEmptyStrings().split("a=1,,b=2").toString());
        }
    }

    /** O3: the dead-condition removal in the three iterators must not move any boundary. */
    @Nested
    public class IteratorBoundariesUnchanged extends TestBase {

        @Test
        public void allThreeEnginesAgreeOnEveryEdgeShape() {
            final String[] sources = { "", ",", ",,", "a", "a,", ",a", "a,b", "a,,b", "a,b," };

            for (final String source : sources) {
                final List<String> viaChar = Splitter.with(',').split(source);
                final List<String> viaPattern = Splitter.with(java.util.regex.Pattern.compile(",")).split(source);

                assertEquals(viaChar, viaPattern, "source=" + source);
                assertEquals(Arrays.asList(source.split(",", -1)), viaChar, "source=" + source);
            }
        }

        @Test
        public void multiCharacterDelimiterEdges() {
            assertEquals(Arrays.asList("", ""), Splitter.with("ab").split("ab"));
            assertEquals(Arrays.asList("xa"), Splitter.with("ab").split("xa"));
            assertEquals(Arrays.asList("a", ""), Splitter.with("ab").split("aab"));
            assertEquals(Arrays.asList(""), Splitter.with("ab").split(""));
            assertTrue(Splitter.with("ab").split((CharSequence) null).isEmpty());
        }

        @Test
        public void limitOneReturnsTheWholeSource() {
            assertEquals(Arrays.asList("a,b,c"), Splitter.with(',').limit(1).split("a,b,c"));
            assertEquals(Arrays.asList(""), Splitter.with(',').limit(1).split(""));
            assertTrue(Splitter.with(',').omitEmptyStrings().limit(1).split("").isEmpty());
        }

        @Test
        public void iteratorIsExhaustedExactlyOnce() {
            final ObjIterator<String> iter = Splitter.with(',').iterate("a,b");

            assertEquals("a", iter.next());
            assertEquals("b", iter.next());
            assertThrows(java.util.NoSuchElementException.class, iter::next);
        }
    }

    /** D5: the MapSplitter constructor reconfigures the splitters it is handed; the factories pass fresh ones. */
    @Nested
    public class MapSplitterFactoriesDoNotShareSplitters extends TestBase {

        @Test
        public void eachFactoryCallProducesIndependentSplitters() {
            final MapSplitter first = MapSplitter.with(",", "=");
            final MapSplitter second = MapSplitter.with(",", "=");

            first.trimResults().limit(1);

            assertEquals("{a=1, b=2}", second.split("a=1,b=2").toString());
            assertEquals("{a=1,b=2}", first.split("a=1,b=2").toString());
        }

        @Test
        public void aSplitterUsedElsewhereIsUnaffectedByCreatingAMapSplitter() {
            final Splitter shared = Splitter.with(',');
            final List<String> before = shared.split("a,,b");

            MapSplitter.with(",", "=");

            assertEquals(before, shared.split("a,,b"));
            assertEquals(3, shared.split("a,,b").size());
        }
    }

    /** O1/J6 have no runtime effect; this only pins the null-argument contracts that surround them. */
    @Nested
    public class ArgumentValidationUnchanged extends TestBase {

        @Test
        public void nullArgumentsStillThrowIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () -> Splitter.with((CharSequence) null));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(""));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with((java.util.regex.Pattern) null));
            assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(java.util.regex.Pattern.compile("a*")));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").split("a", (Class<Object>) null));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitInto("a", (List<String>) null));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitToArray("a", (Class<Object>) null));
            assertThrows(IllegalArgumentException.class, () -> Splitter.with(",").splitToArray("a", String.class));
        }

        @Test
        public void arrayTypeConversionIsUnchanged() {
            assertArrayEquals(new String[] { "a", "b" }, (String[]) Splitter.with(",").splitToArray("a,b", String[].class));
            assertNull(((Integer[]) Splitter.with(",").splitToArray("", Integer[].class))[0]);
        }
    }
}
