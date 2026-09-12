package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 {@link Joiner} review. One nested section per finding.
 */
public class JoinerRegressionATest extends TestBase {

    /**
     * B4: {@code prepareBuilder()} commits the element separator, so a range check that ran after it left a
     * dangling separator behind when it threw. The range is now validated first.
     */
    @Nested
    public class AppendSubSequenceValidatesBeforeCommittingSeparator {

        @Test
        public void outOfRangeIndices_throwAndLeaveJoinerUntouched() {
            final Joiner beyond = Joiner.with(", ").append("a");
            assertThrows(IndexOutOfBoundsException.class, () -> beyond.append("bcd", 0, 99));
            assertEquals("a", beyond.toString());

            final Joiner negative = Joiner.with(", ").append("a");
            assertThrows(IndexOutOfBoundsException.class, () -> negative.append("bcd", -1, 2));
            assertEquals("a", negative.toString());

            final Joiner reversed = Joiner.with(", ").append("a");
            assertThrows(IndexOutOfBoundsException.class, () -> reversed.append("bcd", 2, 1));
            assertEquals("a", reversed.toString());
        }

        @Test
        public void alsoValidatedOnTheTrimmingPath() {
            final Joiner j = Joiner.with(", ").trimBeforeAppend().append("a");

            assertThrows(IndexOutOfBoundsException.class, () -> j.append("bcd", 0, 99));

            assertEquals("a", j.toString());
        }

        @Test
        public void alsoValidatedOnTheStrippingPath() {
            final Joiner j = Joiner.with(", ").stripBeforeAppend().append("a");

            assertThrows(IndexOutOfBoundsException.class, () -> j.append("bcd", 0, 99));

            assertEquals("a", j.toString());
        }

        @Test
        public void aFailedFirstAppendLeavesTheJoinerGenuinelyEmpty() {
            final Joiner j = Joiner.with(", ", "[", "]");

            assertThrows(IndexOutOfBoundsException.class, () -> j.append("bcd", 0, 99));

            assertEquals("[]", j.toString());
            // toString() alone cannot tell the two apart (prefix+suffix either way); mapIfNotEmpty can, because
            // it reports whether a buffer was ever created - i.e. whether the failed append left a mark.
            assertTrue(j.mapIfNotEmpty(String::length).isEmpty());
            assertTrue(Joiner.with(", ", "[", "]").mapIfNotEmpty(String::length).isEmpty());
        }

        @Test
        public void nullElementIsStillRenderedAndIndicesAreIgnored() {
            // A null element never reads the range, so out-of-range indices must not turn into an exception.
            assertEquals("a, null", Joiner.with(", ").append("a").append((CharSequence) null, 0, 99).toString());
        }

        @Test
        public void nullElementIsStillSkippedWhenSkipNullsIsOn() {
            assertEquals("a", Joiner.with(", ").skipNulls().append("a").append((CharSequence) null, 0, 99).toString());
        }

        @Test
        public void validRangesStillWork() {
            assertEquals("he, orl", Joiner.with(", ").append("hello", 0, 2).append("world", 1, 4).toString());
            assertEquals("", Joiner.with(", ").append("hello", 2, 2).toString()); // empty but legal range
            assertEquals("hello", Joiner.with(", ").append("hello", 0, 5).toString()); // whole sequence
        }

        @Test
        public void validRangeOnTheTrimmingPathStillWorks() {
            assertEquals("b", Joiner.with(", ").trimBeforeAppend().append("a b c", 1, 4).toString());
        }

        @Test
        public void boundaryRangesAreAccepted() {
            assertEquals("", Joiner.with(", ").append("hello", 5, 5).toString()); // start == end == length
            assertEquals("o", Joiner.with(", ").append("hello", 4, 5).toString()); // end == length
            assertEquals("", Joiner.with(", ").append("", 0, 0).toString()); // empty CharSequence
        }

        @Test
        public void worksForNonStringCharSequences() {
            final StringBuilder sb = new StringBuilder("hello");

            assertEquals("he", Joiner.with(", ").append(sb, 0, 2).toString());
            assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(", ").append(sb, 0, 99));
        }

        @Test
        public void closedStateIsCheckedBeforeArgumentValidation() {
            final Joiner j = Joiner.with(", ").reuseBuffer();
            j.close();

            assertThrows(IllegalStateException.class, () -> j.append("bcd", 0, 99));
            assertThrows(IllegalStateException.class, () -> j.append("bcd", 0, 2));
        }
    }

    /**
     * B1/J6: an appended empty string is a real element, so merging a Joiner that holds one contributes a
     * separator. Only a Joiner that was never appended to is "empty". This locks parity with
     * {@link StringJoiner#merge(StringJoiner)}.
     */
    @Nested
    public class MergeEmptyElementSemantics {

        @Test
        public void mergingAJoinerHoldingOneEmptyElementAppendsThatElement() {
            final Joiner j1 = Joiner.with(", ").append("a");
            final Joiner j2 = Joiner.with(", ", "[", "]").append("");

            assertEquals("a, ", j1.merge(j2).toString());
        }

        @Test
        public void matchesJdkStringJoiner() {
            final StringJoiner s1 = new StringJoiner(", ");
            s1.add("a");
            final StringJoiner s2 = new StringJoiner(", ", "[", "]");
            s2.add("");

            final Joiner j1 = Joiner.with(", ").append("a");
            final Joiner j2 = Joiner.with(", ", "[", "]").append("");

            assertEquals(s1.merge(s2).toString(), j1.merge(j2).toString());
        }

        @Test
        public void mergingANeverAppendedJoinerHasNoEffect() {
            final Joiner j1 = Joiner.with(", ").append("a");

            assertEquals("a", j1.merge(Joiner.with(", ")).toString());
        }

        @Test
        public void mergingANeverAppendedJoinerWithPrefixSuffixHasNoEffect() {
            final Joiner j1 = Joiner.with(", ").append("a");

            assertEquals("a", j1.merge(Joiner.with(", ", "[", "]")).toString());
        }

        @Test
        public void mergingTwoEmptyElementsKeepsBoth() {
            final Joiner j1 = Joiner.with(",").append("a");
            final Joiner j2 = Joiner.with(",").append("").append("");

            assertEquals("a,,", j1.merge(j2).toString());
        }

        @Test
        public void mergeIntoAnEmptyJoinerStillHonoursTheEmptyElement() {
            assertEquals("", Joiner.with(",").merge(Joiner.with(",").append("")).toString());
        }
    }

    /**
     * J2: {@code append(Object)} / {@code appendEntry(String, Object)} render through {@link N#toString(Object)},
     * which is not {@code Object.toString()} for arrays and collections.
     */
    @Nested
    public class ObjectRenderingUsesNToString {

        @Test
        public void arraysAreRenderedElementWise() {
            assertEquals("[1, 2]", Joiner.with(", ").append(new int[] { 1, 2 }).toString());
            assertEquals("[a, b]", Joiner.with(", ").append(new String[] { "a", "b" }).toString());
        }

        @Test
        public void collectionsAreRenderedElementWise() {
            assertEquals("[1, 2]", Joiner.with(", ").append((Object) Arrays.asList(1, 2)).toString());
        }

        @Test
        public void entryValuesUseTheSameRendering() {
            assertEquals("k=[1, 2]", Joiner.with(", ").appendEntry("k", new int[] { 1, 2 }).toString());
        }
    }

    /**
     * D9: the value type of the bean filter is {@code Object} rather than an unchecked wildcard, so a filter
     * that would receive values it cannot handle is rejected at compile time instead of throwing
     * {@link ClassCastException} at run time.
     */
    @Nested
    public class AppendBeanFilterVariance {

        @Test
        public void lambdaFiltersStillWork() {
            final Account a = new Account();
            a.setFirstName("John");
            a.setLastName(null);

            assertEquals("firstName=John", Joiner.with(", ").appendBean(a, (prop, val) -> val != null).toString());
        }

        @Test
        public void aStoredObjectValuedPredicateCanBePassed() {
            final BiPredicate<String, Object> filter = (prop, val) -> "firstName".equals(prop);
            final Account a = new Account();
            a.setFirstName("John");

            assertEquals("firstName=John", Joiner.with(", ").appendBean(a, filter).toString());
        }

        @Test
        public void aSuperTypedPredicateCanBePassed() {
            final BiPredicate<Object, Object> filter = (prop, val) -> "firstName".equals(prop);
            final Account a = new Account();
            a.setFirstName("John");

            assertEquals("firstName=John", Joiner.with(", ").appendBean(a, filter).toString());
        }

        @Test
        public void nullFilterIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ").appendBean(new Account(), (BiPredicate<String, Object>) null));
        }
    }

    /**
     * Cleanup: the empty-separator / empty-key-value-delimiter flags are derived from the snapshotted fields, so
     * they cannot disagree with the strings the Joiner actually uses even if the caller mutates its argument.
     */
    @Nested
    public class EmptinessFlagsFollowTheSnapshottedDelimiters {

        @Test
        public void mutatingTheSeparatorArgumentAfterConstructionChangesNothing() {
            final StringBuilder sep = new StringBuilder();
            final Joiner j = Joiner.with(sep);
            sep.append("###"); // must not affect the already-built Joiner

            assertEquals("ab", j.append("a").append("b").toString());
        }

        @Test
        public void mutatingTheKeyValueDelimiterArgumentAfterConstructionChangesNothing() {
            final StringBuilder kv = new StringBuilder();
            final Joiner j = Joiner.with(",", kv);
            kv.append("###");

            assertEquals("kv", j.appendEntry("k", "v").toString());
        }

        @Test
        public void emptySeparatorAndDelimiterStillConcatenate() {
            assertEquals("k1v1k2v2", Joiner.with("", "").appendEntry("k1", "v1").appendEntry("k2", "v2").toString());
        }
    }

    /**
     * J1: the two published examples that used a bare {@code null} literal did not compile. These are the
     * corrected forms; that they compile at all is the assertion.
     */
    @Nested
    public class PublishedNullExamplesCompile {

        @Test
        public void classJavadocExample() {
            final String missing = null;
            final String clean = Joiner.with(" | ").skipNulls().trimBeforeAppend().append("  hello  ").append(missing).append("  world  ").toString();

            assertEquals("hello | world", clean);
        }

        @Test
        public void appendStringJavadocExample() {
            final String missing = null;

            assertEquals("hello, world", Joiner.with(", ").append("hello").append("world").toString());
            assertEquals("a, b", Joiner.with(", ").skipNulls().append("a").append(missing).append("b").toString());
        }

        @Test
        public void anExplicitlyCastNullWorksToo() {
            assertEquals("a, b", Joiner.with(", ").skipNulls().append("a").append((String) null).append("b").toString());
        }
    }

    /** Minimal bean used by the {@code appendBean} tests. */
    public static class Account {
        private String firstName;
        private String lastName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(final String lastName) {
            this.lastName = lastName;
        }
    }

    /** Guards the assumption the other sections rely on: configuration is captured, not aliased. */
    @Test
    public void joinerWithReturnsDistinctInstances() {
        final Joiner a = Joiner.with(",");
        final Joiner b = Joiner.with(",");

        assertTrue(a != b);
        assertSame(a, a.skipNulls());
    }
}
