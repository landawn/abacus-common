package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-01 {@link Joiner} review. One nested section per finding.
 */
public class JoinerRegressionBTest extends TestBase {

    /** A value whose {@code toString()} always throws, standing in for a lazy proxy or a half-built DTO. */
    private static final class Boom {
        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    /** A {@link CharSequence} whose {@code toString()} and {@code charAt()} both throw. */
    private static final class BoomCharSequence implements CharSequence {
        @Override
        public int length() {
            return 5;
        }

        @Override
        public char charAt(final int index) {
            throw new IllegalStateException("boom-charAt");
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return this;
        }

        @Override
        public String toString() {
            throw new IllegalStateException("boom-toString");
        }
    }

    public static class ReviewBean {
        private String name = "John";
        private Integer age = null;
        private String city = "NYC";

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class BoomBean {
        private Object bad = new Boom();
        private String ok = "v";

        public Object getBad() {
            return bad;
        }

        public void setBad(final Object bad) {
            this.bad = bad;
        }

        public String getOk() {
            return ok;
        }

        public void setOk(final String ok) {
            this.ok = ok;
        }
    }

    /**
     * B1: an element whose rendering throws must not leave a committed separator (or a half-written
     * {@code "key="}) behind. Every one of these fails on the pre-fix code.
     */
    @Nested
    public class ThrowingElementLeavesNoDanglingSeparator {

        /** Runs {@code op} on a Joiner that already holds "a" and asserts the buffer is untouched. */
        private void assertBufferUnchanged(final Consumer<Joiner> op) {
            final Joiner joiner = Joiner.with(", ").append("a");

            assertThrows(IllegalStateException.class, () -> op.accept(joiner));

            assertEquals("a", joiner.toString());
            // The decisive part: appending again must not produce a phantom empty element ("a, , b").
            assertEquals("a, b", joiner.append("b").toString());
        }

        @Test
        public void appendObject() {
            assertBufferUnchanged(j -> j.append(new Boom()));
        }

        @Test
        public void appendCharSequenceWithThrowingToString() {
            assertBufferUnchanged(j -> j.append(new BoomCharSequence()));
        }

        @Test
        public void appendCharSequenceRangeWithThrowingCharAt() {
            // Range validation alone did not cover this: StringBuilder pulls the characters through charAt().
            assertBufferUnchanged(j -> j.append(new BoomCharSequence(), 0, 3));
        }

        @Test
        public void appendCharSequenceRangeWithThrowingToStringUnderTrim() {
            assertBufferUnchanged(j -> j.trimBeforeAppend().append(new BoomCharSequence(), 0, 3));
        }

        @Test
        public void appendIfNotNull() {
            assertBufferUnchanged(j -> j.appendIfNotNull(new Boom()));
        }

        @Test
        public void appendEntryObjectValue() {
            assertBufferUnchanged(j -> j.appendEntry("k", new Boom()));
        }

        @Test
        public void appendEntryCharSequenceValue() {
            assertBufferUnchanged(j -> j.appendEntry("k", new BoomCharSequence()));
        }

        @Test
        public void appendEntryMapEntryWithThrowingKey() {
            assertBufferUnchanged(j -> j.appendEntry(new AbstractMap.SimpleEntry<>(new Boom(), "v")));
        }

        @Test
        public void appendEntryMapEntryWithThrowingValue() {
            assertBufferUnchanged(j -> j.appendEntry(new AbstractMap.SimpleEntry<>("k", new Boom())));
        }

        @Test
        public void appendAllObjectArray() {
            assertBufferUnchanged(j -> j.appendAll(new Object[] { new Boom() }));
        }

        @Test
        public void appendAllObjectArrayRange() {
            assertBufferUnchanged(j -> j.appendAll(new Object[] { new Boom() }, 0, 1));
        }

        @Test
        public void appendAllCollection() {
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList((Object) new Boom())));
        }

        @Test
        public void appendAllCollectionRange() {
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList((Object) new Boom()), 0, 1));
        }

        @Test
        public void appendAllIterable() {
            assertBufferUnchanged(j -> j.appendAll((Iterable<Object>) Arrays.asList((Object) new Boom())));
        }

        @Test
        public void appendAllIterableWithFilter() {
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList((Object) new Boom()), e -> true));
        }

        @Test
        public void appendAllIterator() {
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList((Object) new Boom()).iterator()));
        }

        @Test
        public void appendAllIteratorWithFilter() {
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList((Object) new Boom()).iterator(), e -> true));
        }

        @Test
        public void appendEntriesMap() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", new Boom())));
        }

        @Test
        public void appendEntriesMapRange() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", new Boom()), 0, 1));
        }

        @Test
        public void appendEntriesMapWithPredicate() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", new Boom()), e -> true));
        }

        @Test
        public void appendEntriesMapWithBiPredicate() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", new Boom()), (k, v) -> true));
        }

        @Test
        public void appendEntriesMapWithExtractors() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", "v"), k -> {
                throw new IllegalStateException("boom");
            }, v -> v));
        }

        @Test
        public void appendEntriesMapWithThrowingKey() {
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap(new Boom(), "v")));
        }

        @Test
        public void appendBeanAll() {
            assertBufferUnchanged(j -> j.appendBean(new BoomBean()));
        }

        @Test
        public void appendBeanSelected() {
            assertBufferUnchanged(j -> j.appendBean(new BoomBean(), Arrays.asList("bad")));
        }

        @Test
        public void appendBeanIgnoreNull() {
            assertBufferUnchanged(j -> j.appendBean(new BoomBean(), false, null));
        }

        @Test
        public void appendBeanWithFilter() {
            assertBufferUnchanged(j -> j.appendBean(new BoomBean(), (p, v) -> true));
        }

        @Test
        public void repeatObject() {
            assertBufferUnchanged(j -> j.repeat(new Boom(), 2));
        }

        /**
         * The throwing element in position 2, where the separator is committed by {@code sb.append(separator)}
         * rather than by {@code prepareBuilder()}. The good element must survive; nothing of the bad one may.
         */
        @Nested
        public class ThrowingElementAfterTheFirst {

            private void assertKeepsFirstOnly(final Consumer<Joiner> op, final String expected) {
                final Joiner joiner = Joiner.with(", ").append("a");

                assertThrows(IllegalStateException.class, () -> op.accept(joiner));

                assertEquals(expected, joiner.toString());
            }

            @Test
            public void bulkElementPaths() {
                final List<Object> values = Arrays.asList("ok", new Boom(), "never");
                final Object[] array = values.toArray();

                assertKeepsFirstOnly(j -> j.appendAll(array), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(array, 0, 3), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(values), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(values, 0, 3), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(new LinkedList<>(values), 0, 3), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(new LinkedHashSet<>(values), 0, 3), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll((Iterable<Object>) values), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(values, e -> true), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(values.iterator()), "a, ok");
                assertKeepsFirstOnly(j -> j.appendAll(values.iterator(), e -> true), "a, ok");
            }

            @Test
            public void bulkEntryPaths() {
                final Map<String, Object> entries = new LinkedHashMap<>();
                entries.put("ok", "v");
                entries.put("bad", new Boom());

                assertKeepsFirstOnly(j -> j.appendEntries(entries), "a, ok=v");
                assertKeepsFirstOnly(j -> j.appendEntries(entries, 0, 2), "a, ok=v");
                assertKeepsFirstOnly(j -> j.appendEntries(entries, e -> true), "a, ok=v");
                assertKeepsFirstOnly(j -> j.appendEntries(entries, (k, v) -> true), "a, ok=v");
                assertKeepsFirstOnly(j -> j.appendEntries(entries, k -> k, v -> v), "a, ok=v");
            }
        }

        /** Paths that were already safe before the fix - they must stay safe. */
        @Test
        public void alreadySafePathsRemainSafe() {
            assertBufferUnchanged(j -> j.appendIf(true, () -> {
                throw new IllegalStateException("boom");
            }));
            assertBufferUnchanged(j -> j.appendAll(Arrays.asList("x"), e -> {
                throw new IllegalStateException("boom");
            }));
            assertBufferUnchanged(j -> j.appendEntries(Collections.singletonMap("k", "v"), (k, v) -> {
                throw new IllegalStateException("boom");
            }));

            final Joiner joiner = Joiner.with(", ").append("a");
            assertThrows(IndexOutOfBoundsException.class, () -> joiner.append("abc", 0, 9));
            assertEquals("a", joiner.toString());
        }
    }

    /** B3: {@code merge(null)} used to throw {@code IllegalArgumentException} with a {@code null} message. */
    @Nested
    public class MergeNullMessage {

        @Test
        public void namesTheArgument() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ").merge(null));

            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("other"), ex.getMessage());
        }
    }

    /** D3: selected bean properties render {@code null}; {@code appendBean(bean)} skips them. */
    @Nested
    public class AppendBeanNullPropertyContract {

        @Test
        public void appendBeanSkipsNullPropertiesButExplicitSelectionRendersThem() {
            final ReviewBean bean = new ReviewBean();

            assertEquals("name=John, city=NYC", Joiner.with(", ").appendBean(bean).toString());
            assertEquals("name=John, age=null, city=NYC", Joiner.with(", ").appendBean(bean, Arrays.asList("name", "age", "city")).toString());
        }

        @Test
        public void selectedNullPropertyHonorsUseForNull() {
            assertEquals("age=N/A", Joiner.with(", ").useForNull("N/A").appendBean(new ReviewBean(), Arrays.asList("age")).toString());
        }

        @Test
        public void ignoreNullPropertyIsTheSelectAndSkipAlternative() {
            assertEquals("name=John, city=NYC", Joiner.with(", ").appendBean(new ReviewBean(), true, new HashSet<>(Arrays.asList("nothing"))).toString());
        }

        @Test
        public void unknownPropertyNameThrows() {
            final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> Joiner.with(", ").appendBean(new ReviewBean(), Arrays.asList("nope")));

            assertTrue(ex.getMessage().contains("nope"), ex.getMessage());
        }
    }

    /** J1: {@code repeat(Object, int)} renders via {@code N.toString}, not a bare {@code Object.toString()}. */
    @Nested
    public class RepeatObjectRendering {

        @Test
        public void arraysAreRenderedElementWise() {
            assertEquals("[1, 2]-[1, 2]", Joiner.with("-").repeat(new int[] { 1, 2 }, 2).toString());
        }

        @Test
        public void collectionsAreRenderedElementWise() {
            assertEquals("[a, b];[a, b]", Joiner.with(";").repeat((Object) Arrays.asList("a", "b"), 2).toString());
        }

        @Test
        public void useForNullApplies() {
            assertEquals("N/A-N/A", Joiner.with("-").useForNull("N/A").repeat((Object) null, 2).toString());
        }

        @Test
        public void trimBeforeAppendApplies() {
            assertEquals("x-x", Joiner.with("-").trimBeforeAppend().repeat((Object) "  x  ", 2).toString());
        }

        @Test
        public void skipNullsIsNotHonored() {
            assertEquals("null-null", Joiner.with("-").skipNulls().repeat((Object) null, 2).toString());
        }

        @Test
        public void zeroRepeatsAppendNothingAndNegativeThrows() {
            assertEquals("", Joiner.with("-").repeat((Object) "x", 0).toString());
            assertThrows(IllegalArgumentException.class, () -> Joiner.with("-").repeat((Object) "x", -1));
        }
    }

    /** O4: the first entry must honor {@code isEmptyKeyValueDelimiter} exactly like every later entry. */
    @Nested
    public class EmptyKeyValueDelimiterIsUniformAcrossEntries {

        private final Map<String, Integer> map = new LinkedHashMap<>();

        private Map<String, Integer> map() {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
            return map;
        }

        @Test
        public void everyAppendEntriesOverloadAgrees() {
            assertEquals("a1,b2", Joiner.with(",", "").appendEntries(map()).toString());
            assertEquals("a1,b2", Joiner.with(",", "").appendEntries(map(), 0, 2).toString());
            assertEquals("a1,b2", Joiner.with(",", "").appendEntries(map(), e -> true).toString());
            assertEquals("a1,b2", Joiner.with(",", "").appendEntries(map(), (k, v) -> true).toString());
            assertEquals("a1,b2", Joiner.with(",", "").appendEntries(map(), k -> k, v -> v).toString());
            assertEquals("a1,b2", Joiner.with(",", "").appendEntry("a", 1).appendEntry("b", 2).toString());
        }

        @Test
        public void appendBeanAgreesToo() {
            assertEquals("nameJohn,cityNYC", Joiner.with(",", "").appendBean(new ReviewBean()).toString());
            assertEquals("nameJohn", Joiner.with(",", "").appendBean(new ReviewBean(), Arrays.asList("name")).toString());
            assertEquals("nameJohn,cityNYC", Joiner.with(",", "").appendBean(new ReviewBean(), (k, v) -> v != null).toString());
        }

        @Test
        public void bothDelimitersEmpty() {
            assertEquals("a1b2", Joiner.with("", "").appendEntries(map()).toString());
        }
    }

    /** O5: the {@code Collection} range overload must give the same answer for every collection shape. */
    @Nested
    public class CollectionRangeIsShapeIndependent {

        @Test
        public void randomAccessListLinkedListAndSetAgree() {
            final List<String> values = Arrays.asList("a", "b", "c", "d", "e");

            for (int from = 0; from <= values.size(); from++) {
                for (int to = from; to <= values.size(); to++) {
                    final int f = from;
                    final int t = to;
                    final String viaArrayList = Joiner.with("-").appendAll(new ArrayList<>(values), f, t).toString();

                    assertEquals(viaArrayList, Joiner.with("-").appendAll(new LinkedList<>(values), f, t).toString(), "LinkedList " + f + ".." + t);
                    assertEquals(viaArrayList, Joiner.with("-").appendAll(new LinkedHashSet<>(values), f, t).toString(), "LinkedHashSet " + f + ".." + t);
                    assertEquals(viaArrayList, Joiner.with("-").appendAll(values, f, t).toString(), "Arrays.asList " + f + ".." + t);
                }
            }
        }

        @Test
        public void skipNullsStillAppliesWithinTheRange() {
            final List<String> values = Arrays.asList("a", null, "c");

            assertEquals("a-c", Joiner.with("-").skipNulls().appendAll(values, 0, 3).toString());
            assertEquals("a-c", Joiner.with("-").skipNulls().appendAll(new LinkedList<>(values), 0, 3).toString());
            assertEquals("c", Joiner.with("-").skipNulls().appendAll(values, 1, 3).toString());
            assertEquals("c", Joiner.with("-").skipNulls().appendAll(new LinkedList<>(values), 1, 3).toString());
        }

        @Test
        public void outOfRangeStillThrows() {
            final List<String> values = Arrays.asList("a", "b");

            assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with("-").appendAll(values, 0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with("-").appendAll(new LinkedList<>(values), 2, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with("-").appendAll((java.util.Collection<String>) null, 0, 1));
        }

        /**
         * A weakly consistent collection may report a {@code size()} its iterator does not deliver. The range
         * overload must stop, exactly as the previous for-each shape did, not throw {@code NoSuchElementException}.
         */
        @Test
        public void aShortIteratorEndsTheLoopInsteadOfThrowing() {
            final java.util.AbstractCollection<String> overstatesSize = new java.util.AbstractCollection<>() {
                @Override
                public java.util.Iterator<String> iterator() {
                    return Arrays.asList("a", "b").iterator();
                }

                @Override
                public int size() {
                    return 5; // lies: the iterator only yields 2
                }
            };

            assertEquals("a-b", Joiner.with("-").appendAll(overstatesSize, 0, 5).toString());
            assertEquals("b", Joiner.with("-").appendAll(overstatesSize, 1, 5).toString());
            assertEquals("", Joiner.with("-").appendAll(overstatesSize, 3, 5).toString());
        }
    }

    /** B5 (documentation half): the deliberate divergence from {@code Appendable} for a {@code null} element. */
    @Nested
    public class NullElementRangeSemantics {

        @Test
        public void nullElementAppendsTheWholeNullTextAndIgnoresTheRange() {
            assertEquals("null", Joiner.with(", ").append((CharSequence) null, 0, 2).toString());
            assertEquals("null", Joiner.with(", ").append((CharSequence) null, 0, 99).toString());
            assertEquals("N/A", Joiner.with(", ").useForNull("N/A").append((CharSequence) null, 3, 1).toString());
        }

        @Test
        public void nullElementIsSkippedWhenSkipNullsIsOn() {
            assertEquals("", Joiner.with(", ").skipNulls().append((CharSequence) null, 0, 2).toString());
        }

        @Test
        public void nonNullElementStillRangeChecks() {
            assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(", ").append("abc", 0, 9));
            assertEquals("he", Joiner.with(", ").append("hello", 0, 2).toString());
            assertEquals("", Joiner.with(", ").append("hello", 2, 2).toString());
        }

        @Test
        public void subSequenceIsCopiedNotStreamed() {
            // The copy is what makes a throwing charAt() safe; the visible result must be unchanged.
            final StringBuilder source = new StringBuilder("hello world");

            assertEquals("ello", Joiner.with(", ").append(source, 1, 5).toString());
            assertEquals("ello", Joiner.with(", ").trimBeforeAppend().append(source, 1, 5).toString());
            assertEquals("world", Joiner.with(", ").stripBeforeAppend().append(source, 5, 11).toString());
        }
    }

    /** Guards the private render-then-commit helpers against collateral damage on ordinary input. */
    @Nested
    public class BehaviourPreservedForOrdinaryInput {

        @Test
        public void separatorPlacementIsUnchanged() {
            assertEquals("[a, b, c]", Joiner.with(", ", "[", "]").appendAll(Arrays.asList("a", "b", "c")).toString());
            assertEquals("abc", Joiner.with("").appendAll(Arrays.asList("a", "b", "c")).toString());
            assertEquals("x, a, b", Joiner.with(", ").append("x").appendAll(new Object[] { "a", "b" }).toString());
            assertEquals("a, null, b", Joiner.with(", ").appendAll(new Object[] { "a", null, "b" }).toString());
            assertEquals("a, b", Joiner.with(", ").skipNulls().appendAll(new Object[] { "a", null, "b" }).toString());
        }

        @Test
        public void emptyBulkAppendsCommitNothing() {
            assertEquals("", Joiner.with(", ").appendAll(new Object[0]).toString());
            assertEquals("", Joiner.with(", ").appendAll(Collections.emptyList()).toString());
            assertEquals("", Joiner.with(", ").skipNulls().appendAll(new Object[] { null, null }).toString());
            assertEquals("a", Joiner.with(", ").append("a").skipNulls().appendAll(new Object[] { null }).toString());
            assertFalse(Joiner.with(", ").appendAll(new Object[0]).mapIfNotEmpty(String::length).isPresent());
        }

        @Test
        public void mergeAndReuseStillWork() {
            final Joiner left = Joiner.with(", ").append("a").append("b");
            final Joiner right = Joiner.with(", ").append("c").append("d");

            assertEquals("a, b, c, d", left.merge(right).toString());

            try (Joiner pooled = Joiner.with(",").reuseBuffer()) {
                pooled.appendAll(new Object[] { 0, 1, 2 });
                assertEquals("0,1,2", pooled.toString());
                assertEquals("0,1,2,3", pooled.append(3).toString());
            }
        }
    }
}
