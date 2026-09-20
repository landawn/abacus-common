package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;

/**
 * Regression tests for the {@link Joiner} review of 2026-09-01 (second pass).
 *
 * <p>The theme is one coherent closed-state policy: <b>closing ends writing, not reading.</b> Before the fix a
 * {@code reuseBuffer()} Joiner that was closed before being materialized silently reported the empty value
 * (losing its content), {@code merge} silently contributed nothing for such a source, and a closed receiver
 * accepted {@code merge} whenever the argument happened to be empty.</p>
 */
public class JoinerClosedStateFixTest extends TestBase {

    /** B2: close() must preserve the accumulated content instead of discarding it with the pooled builder. */
    @Nested
    public class ClosingPreservesContent extends TestBase {

        @Test
        public void aClosedPooledJoinerStillReportsItsContent() {
            final Joiner j = Joiner.with(", ", "[", "]").reuseBuffer().append("a").append("b");
            j.close();

            // Used to return "[]" with length 2 - the content was gone with the recycled builder.
            assertEquals("[a, b]", j.toString());
            assertEquals("[a, b]".length(), j.length());
        }

        @Test
        public void closingIsIdempotentAndStillReportsContent() {
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a");
            j.close();
            j.close();

            assertEquals("a", j.toString());
        }

        @Test
        public void aClosedPooledJoinerMatchesAClosedPlainOneExactly() {
            final Joiner pooled = Joiner.with(", ", "[", "]").reuseBuffer().append("a").append("b");
            final Joiner plain = Joiner.with(", ", "[", "]").append("a").append("b");
            pooled.close();
            plain.close();

            assertEquals(plain.toString(), pooled.toString());
            assertEquals(plain.length(), pooled.length());
        }

        @Test
        public void closingAnEmptyJoinerStillYieldsTheEmptyValue() {
            final Joiner j = Joiner.with(", ", "[", "]").reuseBuffer();
            j.close();
            assertEquals("[]", j.toString());

            final Joiner withEmptyValue = Joiner.with(", ").setEmptyValue("NONE").reuseBuffer();
            withEmptyValue.close();
            assertEquals("NONE", withEmptyValue.toString());
        }

        @Test
        public void contentAddedAfterReEnablingBufferReuseSurvivesClose() {
            // A stale latestToStringValue from the first toString() must not suppress materialization: the
            // buffer holds the newer content and has to win.
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a");
            assertEquals("a", j.toString());

            j.reuseBuffer();
            j.append("b");
            j.close();

            assertEquals("a, b", j.toString());
            assertEquals(4, j.length());
        }

        @Test
        public void materializingBeforeCloseIsUnaffected() {
            // The ordinary try-with-resources path: toString() already released the builder, so close() has
            // nothing left to materialize.
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a").append("b");
            assertEquals("a, b", j.toString());
            j.close();
            assertEquals("a, b", j.toString());
        }

        @Test
        public void aClosedJoinerHoldingOnlyAnEmptyElementMatchesAnOpenOne() {
            // An appended "" IS an element (StringJoiner semantics), so such a Joiner is not empty. Preserving
            // the content on close() makes the closed pooled case agree with the open case, where it used to
            // fall back to the empty value and report Optional.empty().
            final Joiner closedPooled = Joiner.with(",").reuseBuffer().append("");
            closedPooled.close();
            final Joiner openPlain = Joiner.with(",").append("");

            assertEquals(openPlain.mapIfNotEmpty(String::length), closedPooled.mapIfNotEmpty(String::length));
            assertEquals(openPlain.length(), closedPooled.length());
            assertEquals(openPlain.toString(), closedPooled.toString());
        }

        @Test
        public void appendingAfterCloseStillThrows() {
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a");
            j.close();

            assertThrows(IllegalStateException.class, () -> j.append("b"));
            assertThrows(IllegalStateException.class, () -> j.appendAll(new int[] { 1 }));
            assertThrows(IllegalStateException.class, () -> j.appendEntry("k", "v"));
        }

        @Test
        public void aClosedJoinerCanStillBeMappedAndAppendedTo() throws Exception {
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a").append("b");
            j.close();

            assertEquals(4, j.map(String::length));
            assertEquals(4, j.mapIfNotEmpty(String::length).orElseThrow());

            final StringBuilder sb = new StringBuilder("X:");
            j.appendTo(sb);
            assertEquals("X:a, b", sb.toString());
        }
    }

    /** B3 / B3b: merge reads a closed source, and rejects a closed receiver unconditionally. */
    @Nested
    public class MergeHonoursTheClosedStatePolicy extends TestBase {

        @Test
        public void aClosedPooledSourceStillContributesItsContent() {
            final Joiner other = Joiner.with("; ").reuseBuffer().append("p");
            other.close();

            // Used to contribute nothing at all, silently producing "x, y".
            assertEquals("x, y, p", Joiner.with(", ").append("x").append("y").merge(other).toString());
        }

        @Test
        public void aClosedPooledSourceWithAPrefixIsMergedWithoutIt() {
            // merge() skips other.prefix.length() characters of the preserved value, so a prefix longer than
            // one character would expose any off-by-one in that offset.
            final Joiner one = Joiner.with("; ", "<", ">").reuseBuffer().append("p").append("q");
            one.close();
            assertEquals("x, p; q", Joiner.with(", ").append("x").merge(one).toString());

            final Joiner three = Joiner.with("; ", "<<<", ">").reuseBuffer().append("p");
            three.close();
            assertEquals("x, p", Joiner.with(", ").append("x").merge(three).toString());
        }

        @Test
        public void aClosedPlainSourceStillContributesItsContent() {
            final Joiner other = Joiner.with("; ").append("p");
            other.close();

            assertEquals("x, p", Joiner.with(", ").append("x").merge(other).toString());
        }

        @Test
        public void aClosedReceiverRejectsMergeEvenWhenTheSourceIsEmpty() {
            final Joiner closed = Joiner.with(", ").append("x");
            closed.close();

            // Used to succeed silently, because the empty-source path never reached prepareBuilder().
            assertThrows(IllegalStateException.class, () -> closed.merge(Joiner.with("; ")));
            assertThrows(IllegalStateException.class, () -> closed.merge(Joiner.with("; ").append("p")));
        }

        @Test
        public void aNullSourceIsStillRejectedOnAnOpenJoiner() {
            assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ").merge(null));
        }

        @Test
        public void mergeSemanticsForAnEmptyElementAreUnchanged() {
            // StringJoiner parity: an appended "" IS an element, so the merge contributes a separator.
            assertEquals("a", Joiner.with(", ").append("a").merge(Joiner.with(", ")).toString());
            assertEquals("a, ", Joiner.with(", ").append("a").merge(Joiner.with(", ").append("")).toString());
        }
    }

    /** D4b: enabling buffer reuse on a closed Joiner can never take effect, so it must be rejected. */
    @Nested
    public class ReuseBufferRejectsAClosedJoiner extends TestBase {

        @Test
        public void reuseBufferOnAClosedPooledJoinerThrows() {
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a");
            j.close();

            // Used to be accepted, handing back a Joiner that threw on the very next append.
            assertThrows(IllegalStateException.class, j::reuseBuffer);
        }

        @Test
        public void reuseBufferOnAClosedPlainJoinerThrows() {
            final Joiner j = Joiner.with(", ").append("a");
            j.close();

            assertThrows(IllegalStateException.class, j::reuseBuffer);
        }

        @Test
        public void reuseBufferIsStillRejectedOnceTheBufferExists() {
            final Joiner j = Joiner.with(", ").append("a");

            assertThrows(IllegalStateException.class, j::reuseBuffer);
        }

        @Test
        public void theDocumentedReEnableRecipeStillWorks() {
            // toString() releases the pooled builder and clears the flag; re-enabling BEFORE the next append
            // pools the next builder too.
            final Joiner j = Joiner.with(", ").reuseBuffer().append("a");
            assertEquals("a", j.toString());

            assertSame(j, j.reuseBuffer());
            j.append("b");
            assertEquals("a, b", j.toString());
        }
    }

    /** B5: toString() no longer appends the suffix into the buffer and rewinds it. */
    @Nested
    public class ToStringDoesNotMutateTheBuffer extends TestBase {

        @Test
        public void repeatedToStringIsStable() {
            final Joiner j = Joiner.with(", ", "[", "]").append("a").append("b");

            assertEquals("[a, b]", j.toString());
            assertEquals("[a, b]", j.toString());
            assertEquals("[a, b]", j.toString());
        }

        @Test
        public void appendingAfterToStringContinuesFromTheContentNotTheSuffix() {
            final Joiner j = Joiner.with(", ", "[", "]").append("a");
            assertEquals("[a]", j.toString());

            j.append("b");
            assertEquals("[a, b]", j.toString());
            assertEquals("[a, b]".length(), j.length());
        }

        @Test
        public void theSameHoldsForAPooledJoiner() {
            final Joiner j = Joiner.with(", ", "[", "]").reuseBuffer().append("a");
            assertEquals("[a]", j.toString());

            j.append("b");
            assertEquals("[a, b]", j.toString());
        }

        @Test
        public void anEmptySuffixIsUnaffected() {
            final Joiner j = Joiner.with(", ").append("a").append("b");

            assertEquals("a, b", j.toString());
            assertEquals("a, b", j.toString());
        }
    }

    /**
     * O3: the primitive and {@code StringBuilder} entry overloads now share {@code appendEntryPrefix} while
     * keeping their typed {@code append(value)} call, so their output must be byte-identical to before.
     */
    @Nested
    public class EntryRenderingIsUnchanged extends TestBase {

        @Test
        public void everyPrimitiveEntryOverloadRendersAsBefore() {
            assertEquals("enabled=true", Joiner.with(", ").appendEntry("enabled", true).toString());
            assertEquals("grade=A", Joiner.with(", ").appendEntry("grade", 'A').toString());
            assertEquals("count=42", Joiner.with(", ").appendEntry("count", 42).toString());
            assertEquals("timestamp=1234567890", Joiner.with(", ").appendEntry("timestamp", 1234567890L).toString());
            assertEquals("price=19.99", Joiner.with(", ").appendEntry("price", 19.99f).toString());
            assertEquals("temperature=98.6", Joiner.with(", ").appendEntry("temperature", 98.6).toString());
        }

        @Test
        public void anEmptyKeyValueDelimiterStillOmitsIt() {
            assertEquals("k1", Joiner.with(", ", "", "", "").appendEntry("k", 1).toString());
            assertEquals("ktrue", Joiner.with(", ", "", "", "").appendEntry("k", true).toString());
            assertEquals("kv", Joiner.with(", ", "", "", "").appendEntry("k", "v").toString());
            assertEquals("<kvk2v2>", Joiner.with("", "", "<", ">").appendEntry("k", "v").appendEntry("k2", "v2").toString());
        }

        @Test
        public void multipleEntriesStillGetTheElementSeparator() {
            assertEquals("a=1, b=2, c=3", Joiner.with(", ").appendEntry("a", 1).appendEntry("b", 2).appendEntry("c", 3).toString());
        }

        @Test
        public void stringAndStringBuilderValuesRenderAsBefore() {
            assertEquals("name=John", Joiner.with(", ").appendEntry("name", "John").toString());
            assertEquals("data=xyz", Joiner.with(", ").appendEntry("data", new StringBuilder("xyz")).toString());
            assertEquals("data=null", Joiner.with(", ").appendEntry("data", (StringBuilder) null).toString());
            assertEquals("data=N/A", Joiner.with(", ").useForNull("N/A").appendEntry("data", (String) null).toString());
        }

        @Test
        public void trimmingStillAppliesToKeyAndValueOfEveryOverload() {
            assertEquals("k=v", Joiner.with(", ").trimBeforeAppend().appendEntry("  k  ", "  v  ").toString());
            assertEquals("k=v", Joiner.with(", ").trimBeforeAppend().appendEntry("  k  ", new StringBuilder("  v  ")).toString());
            assertEquals("k=v", Joiner.with(", ").stripBeforeAppend().appendEntry("\tk\t", new StringBuilder("\tv\t")).toString());
            // Primitives are never trimmed, but the key still is.
            assertEquals("k=1", Joiner.with(", ").trimBeforeAppend().appendEntry("  k  ", 1).toString());
        }

        @Test
        public void objectAndMapEntryOverloadsRenderAsBefore() {
            assertEquals("created=x", Joiner.with(", ").appendEntry("created", (Object) "x").toString());
            assertEquals("score=100", Joiner.with(", ").appendEntry(new AbstractMap.SimpleEntry<>("score", 100)).toString());
            assertEquals("null", Joiner.with(", ").appendEntry((Map.Entry<?, ?>) null).toString());
            assertEquals("k=[1, 2]", Joiner.with(", ").appendEntry("k", new int[] { 1, 2 }).toString());
        }

        @Test
        public void bulkEntryPathsAreUnchanged() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("a", 1);
            m.put("b", 2);
            m.put("c", 3);

            assertEquals("a=1, b=2, c=3", Joiner.with(", ").appendEntries(m).toString());
            assertEquals("b=2, c=3", Joiner.with(", ").appendEntries(m, 1, 3).toString());
            assertEquals("b=2, c=3", Joiner.with(", ").appendEntries(m, e -> e.getValue() > 1).toString());
            assertEquals("b=2, c=3", Joiner.with(", ").appendEntries(m, (k, v) -> v > 1).toString());
            assertEquals("A=$1, B=$2, C=$3", Joiner.with(", ").appendEntries(m, String::toUpperCase, v -> "$" + v).toString());
        }
    }

    /** J7: repeat(String, int) documents trim/strip and null handling; the behaviour is asserted here. */
    @Nested
    public class RepeatStringSemantics extends TestBase {

        @Test
        public void repeatAppliesTrimOnceAndIgnoresSkipNulls() {
            assertEquals("Hello, Hello, Hello", Joiner.with(", ").repeat("Hello", 3).toString());
            assertEquals("a-a-a", Joiner.with("-").trimBeforeAppend().repeat("  a  ", 3).toString());
            assertEquals("N/A-N/A", Joiner.with("-").useForNull("N/A").skipNulls().repeat((String) null, 2).toString());
            assertEquals("", Joiner.with("-").repeat("x", 0).toString());
            assertThrows(IllegalArgumentException.class, () -> Joiner.with("-").repeat("x", -1));
        }
    }

    /**
     * A closed Joiner must be rejected <i>before</i> the element is rendered. {@code assertNotClosed()} is reachable
     * only from {@code prepareBuilder()}, so hoisting the rendering above that call let a closed Joiner run an
     * arbitrary {@code toString()} - or build a whole {@code Strings.repeat()} run - before refusing the write.
     */
    @Nested
    public class ClosedStateIsCheckedBeforeRendering extends TestBase {

        private Joiner closed() {
            final Joiner j = Joiner.with(", ").append("a");
            j.close();

            return j;
        }

        @Test
        public void aClosedJoinerNeverRendersTheElement() {
            final int[] renders = { 0 };
            final Object counted = new Object() {
                @Override
                public String toString() {
                    renders[0]++;

                    return "x";
                }
            };
            final Joiner j = closed();

            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.append(counted)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.repeat(counted, 3)).getMessage());
            assertEquals(0, renders[0], "a closed Joiner must not render the element it is going to refuse");
        }

        @Test
        public void aThrowingToStringDoesNotMaskTheClosedState() {
            final CharSequence boom = new CharSequence() {
                @Override
                public int length() {
                    return 5;
                }

                @Override
                public char charAt(final int index) {
                    throw new UnsupportedOperationException("boom-charAt");
                }

                @Override
                public CharSequence subSequence(final int start, final int end) {
                    return this;
                }

                @Override
                public String toString() {
                    throw new UnsupportedOperationException("boom-toString");
                }
            };
            final Joiner j = closed();

            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.append(boom)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.append(boom, 0, 2)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.append(boom, 0, 99)).getMessage());
        }

        @Test
        public void aClosedJoinerDoesNotBuildTheRepetitionFirst() {
            // Strings.repeat() reports a run this long as OutOfMemoryError, which used to surface instead of the
            // closed state because the run was built before prepareBuilder() was ever reached.
            final Joiner j = closed();

            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> j.repeat("xyz", Integer.MAX_VALUE)).getMessage());
        }

        @Test
        public void repeatStillRendersANullAsTheUntrimmedNullText() {
            // repeat(String, int) folds its dead `str == null ? nullText : format(str)` ternary into format(str),
            // which yields the null text verbatim - trimBeforeAppend must not reach it.
            assertEquals("  N/A  -  N/A  ", Joiner.with("-").useForNull("  N/A  ").trimBeforeAppend().repeat((String) null, 2).toString());
            assertEquals("N/A-N/A", Joiner.with("-").useForNull("N/A").repeat((String) null, 2).toString());
        }
    }

    /**
     * R10-5: after the single-element family was taught to refuse a closed {@code Joiner} before rendering, the
     * <i>bulk</i> family still ran one round of user code - a foreign {@code toString()}, a filter predicate, a
     * key/value extractor or a bean getter - before {@code prepareBuilder()} refused the write. Every one of those
     * entry points must now refuse first, including ahead of argument validation.
     */
    @Nested
    public class TheBulkFamilyAlsoRefusesBeforeRunningUserCode extends TestBase {

        private final int[] calls = { 0 };

        private Joiner closed() {
            final Joiner j = Joiner.with(", ").append("a");
            j.close();

            return j;
        }

        /** An element whose rendering is observable. */
        private Object counted() {
            return new Object() {
                @Override
                public String toString() {
                    calls[0]++;

                    return "x";
                }
            };
        }

        /** A CharSequence whose rendering is observable. */
        private CharSequence countedCharSequence() {
            return new CharSequence() {
                @Override
                public int length() {
                    return 1;
                }

                @Override
                public char charAt(final int index) {
                    return 'x';
                }

                @Override
                public CharSequence subSequence(final int start, final int end) {
                    return this;
                }

                @Override
                public String toString() {
                    calls[0]++;

                    return "x";
                }
            };
        }

        private void refuses(final Executable call) {
            calls[0] = 0;

            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, call).getMessage());
            assertEquals(0, calls[0], "a closed Joiner must not run user code before refusing");
        }

        @Test
        public void everyArrayCollectionAndIteratorEntryPointRefusesBeforeRendering() {
            final Object e = counted();
            final List<Object> list = Arrays.asList(e);

            refuses(() -> closed().appendAll(new Object[] { e }));
            refuses(() -> closed().appendAll(new Object[] { e }, 0, 1));
            refuses(() -> closed().appendAll(list));
            refuses(() -> closed().appendAll(list, 0, 1));
            refuses(() -> closed().appendAll((Iterable<Object>) list));
            refuses(() -> closed().appendAll(list.iterator()));
            // The filter is user code too, so it must not run either.
            refuses(() -> closed().appendAll((Iterable<Object>) list, x -> {
                calls[0]++;
                return true;
            }));
            refuses(() -> closed().appendAll(list.iterator(), x -> {
                calls[0]++;
                return true;
            }));
        }

        @Test
        public void everyEntryEntryPointRefusesBeforeRendering() {
            final Object e = counted();
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", e);

            refuses(() -> closed().appendEntry("k", e));
            refuses(() -> closed().appendEntry("k", countedCharSequence()));
            refuses(() -> closed().appendEntry("k", new StringBuilder("v")));
            refuses(() -> closed().appendEntry(new AbstractMap.SimpleEntry<>("k", e)));
            refuses(() -> closed().appendEntries(map));
            refuses(() -> closed().appendEntries(map, 0, 1));
            refuses(() -> closed().appendEntries(map, entry -> {
                calls[0]++;
                return true;
            }));
            refuses(() -> closed().appendEntries(map, (k, v) -> {
                calls[0]++;
                return true;
            }));
            refuses(() -> closed().appendEntries(map, k -> {
                calls[0]++;
                return k;
            }, v -> v));
        }

        @Test
        public void everyBeanEntryPointRefusesBeforeCallingAGetter() {
            final CountingBean bean = new CountingBean();

            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed().appendBean(bean)).getMessage());
            assertThrows(IllegalStateException.class, () -> closed().appendBean(bean, Arrays.asList("name")));
            assertThrows(IllegalStateException.class, () -> closed().appendBean(bean, true, null));
            assertThrows(IllegalStateException.class, () -> closed().appendBean(bean, (k, v) -> true));

            assertEquals(0, bean.reads, "a closed Joiner must not call a property getter");
        }

        @Test
        public void closedStateIsCheckedBeforeArgumentValidation() {
            final Object e = counted();
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", e);

            // Closed state takes precedence over invalid ranges in every append overload.
            assertThrows(IllegalStateException.class, () -> closed().appendAll(new Object[] { e }, 0, 9));
            assertThrows(IllegalStateException.class, () -> closed().appendAll(Arrays.asList(e), 0, 9));
            assertThrows(IllegalStateException.class, () -> closed().appendEntries(map, 0, 9));

            assertEquals("Joiner has been closed",
                    assertThrows(IllegalStateException.class, () -> closed().appendAll(Arrays.asList(e), (Predicate<Object>) null)).getMessage());
            assertEquals("Joiner has been closed",
                    assertThrows(IllegalStateException.class, () -> closed().appendEntries(map, (Predicate<Map.Entry<String, Object>>) null)).getMessage());
            assertEquals("Joiner has been closed",
                    assertThrows(IllegalStateException.class, () -> closed().appendBean(new CountingBean(), (BiPredicate<String, Object>) null)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed().repeat("x", -1)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed().merge(null)).getMessage());
        }

        @Test
        public void emptyBulkRequestsRequireAnOpenJoiner() {
            assertThrows(IllegalStateException.class, () -> closed().appendAll(new Object[0]));
            assertThrows(IllegalStateException.class, () -> closed().appendAll(new Object[] { "z" }, 1, 1));
            assertThrows(IllegalStateException.class, () -> closed().appendAll(Collections.emptyList()));
            assertThrows(IllegalStateException.class, () -> closed().appendAll((Iterable<Object>) Collections.<Object> emptyList()));
            assertThrows(IllegalStateException.class, () -> closed().appendAll(Collections.emptyList().iterator()));
            assertThrows(IllegalStateException.class, () -> closed().appendEntries(new LinkedHashMap<>()));

            // Overloads that check closed first refuse even an empty or null source.
            assertEquals("Joiner has been closed",
                    assertThrows(IllegalStateException.class, () -> closed().appendAll((Iterable<Object>) Collections.<Object> emptyList(), x -> true))
                            .getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed().appendBean(null)).getMessage());
            assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed().repeat("x", 0)).getMessage());
        }

        @Test
        public void openJoinersAreUnaffected() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("k", "v");
            map.put("j", "w");

            assertEquals("x, y", Joiner.with(", ").appendAll(Arrays.asList("x", "y")).toString());
            assertEquals("y", Joiner.with(", ").appendAll(Arrays.asList("x", "y"), s -> "y".equals(s)).toString());
            assertEquals("y", Joiner.with(", ").appendAll(Arrays.asList("x", "y").iterator(), s -> "y".equals(s)).toString());
            assertEquals("k=v, j=w", Joiner.with(", ").appendEntries(map).toString());
            assertEquals("k=v", Joiner.with(", ").appendEntries(map, entry -> "k".equals(entry.getKey())).toString());
            assertEquals("k=v", Joiner.with(", ").appendEntries(map, (k, v) -> "k".equals(k)).toString());
            assertEquals("K=v, J=w", Joiner.with(", ").appendEntries(map, k -> Strings.toUpperCase(k), v -> v).toString());
            assertEquals("k=v", Joiner.with(", ").appendEntry(new AbstractMap.SimpleEntry<>("k", "v")).toString());

            final CountingBean bean = new CountingBean();
            assertEquals("name=n", Joiner.with(", ").appendBean(bean).toString());
            assertEquals("name=n", Joiner.with(", ").appendBean(bean, Arrays.asList("name")).toString());
            assertEquals("name=n", Joiner.with(", ").appendBean(bean, (k, v) -> true).toString());
        }
    }

    /** A bean whose getter calls are observable. */
    public static class CountingBean {
        int reads = 0;
        @AccessFieldByMethod
        private String name = "n";

        public String getName() {
            reads++;

            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void selectedBeanNamesAreValidatedBeforeGettersOrAppends() {
        final CountingBean bean = new CountingBean();
        final Joiner joiner = Joiner.with(", ").append("saved");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(bean, Arrays.asList("name", "missing")));
        assertEquals(0, bean.reads);
        assertEquals("saved", joiner.toString());
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(bean, Arrays.asList("name", null)));
        assertEquals(0, bean.reads);
        assertEquals("saved", joiner.toString());

        joiner.appendBean(bean, Arrays.asList("name", "name"));
        assertEquals(2, bean.reads);
        assertEquals("saved, name=n, name=n", joiner.toString());
        bean.setName(null);
        assertEquals("name=missing", Joiner.with(", ").useForNull("missing").skipNulls().appendBean(bean, List.of("name")).toString());
        assertEquals(3, bean.reads);
    }

    @Test
    public void selectedNestedBeanPathsRetainTheirLookupAndNullSemantics() {
        final NestedCountingBean bean = new NestedCountingBean();
        final Joiner joiner = Joiner.with(", ");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(bean, List.of("child.name", "child.missing")));
        assertEquals(0, bean.reads);
        assertEquals(0, bean.child.reads);
        assertEquals("", joiner.toString());
        assertEquals("child.name=n", joiner.appendBean(bean, List.of("child.name")).toString());
        assertEquals(1, bean.reads);
        assertEquals(1, bean.child.reads);
        bean.setChild(null);
        assertEquals("child.name=null", Joiner.with(", ").appendBean(bean, List.of("child.name")).toString());
        assertEquals(2, bean.reads);
    }

    public static class NestedCountingBean {
        int reads;
        @AccessFieldByMethod
        private CountingBean child = new CountingBean();

        public CountingBean getChild() {
            reads++;
            return child;
        }

        public void setChild(final CountingBean child) {
            this.child = child;
        }
    }

    /** Guards the shapes the rest of the suite depends on: ordinary joining is unchanged. */
    @Test
    public void ordinaryJoiningIsUnchanged() {
        assertEquals("a, b, c", Joiner.with(", ").appendAll(Arrays.asList("a", "b", "c")).toString());
        assertEquals("[a, b, c]", Joiner.with(", ", "[", "]").appendAll(new String[] { "a", "b", "c" }).toString());
        assertEquals("1-2-3", Joiner.with("-").appendAll(new int[] { 1, 2, 3 }).toString());
        assertEquals("a, b", Joiner.with(", ").skipNulls().appendAll(new Object[] { "a", null, "b" }).toString());
        assertEquals("a, null, b", Joiner.with(", ").appendAll(new Object[] { "a", null, "b" }).toString());
        assertEquals("[]", Joiner.with(", ", "[", "]").toString());
        assertEquals("he, orl", Joiner.with(", ").append("hello", 0, 2).append("world", 1, 4).toString());
        assertNotNull(Joiner.withDefault().appendAll(Arrays.asList("a")).toString());
    }

    private static Joiner closedJoiner() {
        final Joiner j = Joiner.with(", ").append("a");
        j.close();

        return j;
    }

    private static void refusesWithIse(final Executable call) {
        assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, call).getMessage());
    }

    /**
     * Verifies the closed-state contract for value appends and the read-only {@code appendTo} operation.
     * The next test covers skipped elements, empty inputs, and invalid arguments.
     */
    @Test
    public void everyValueShapedAppendOverloadRefusesAClosedJoiner() throws Exception {
        refusesWithIse(() -> closedJoiner().append(true));
        refusesWithIse(() -> closedJoiner().append('x'));
        refusesWithIse(() -> closedJoiner().append(1));
        refusesWithIse(() -> closedJoiner().append(1L));
        refusesWithIse(() -> closedJoiner().append(1f));
        refusesWithIse(() -> closedJoiner().append(1d));
        refusesWithIse(() -> closedJoiner().append("s"));
        refusesWithIse(() -> closedJoiner().append(new StringBuilder("s")));

        refusesWithIse(() -> closedJoiner().appendEntry("k", true));
        refusesWithIse(() -> closedJoiner().appendEntry("k", 'x'));
        refusesWithIse(() -> closedJoiner().appendEntry("k", 1));
        refusesWithIse(() -> closedJoiner().appendEntry("k", 1L));
        refusesWithIse(() -> closedJoiner().appendEntry("k", 1f));
        refusesWithIse(() -> closedJoiner().appendEntry("k", 1d));
        refusesWithIse(() -> closedJoiner().appendEntry("k", "v"));

        // Primitive arrays and the primitive list family refuse a non-empty source too.
        refusesWithIse(() -> closedJoiner().appendAll(new int[] { 1 }));
        refusesWithIse(() -> closedJoiner().appendAll(new boolean[] { true }));
        refusesWithIse(() -> closedJoiner().appendAll(new char[] { 'a' }));
        refusesWithIse(() -> closedJoiner().appendAll(new double[] { 1d }));
        refusesWithIse(() -> closedJoiner().appendAll(IntList.of(1)));
        refusesWithIse(() -> closedJoiner().appendAll(CharList.of('a')));
        refusesWithIse(() -> closedJoiner().appendAll(new int[] { 1, 2 }, 0, 2));
        refusesWithIse(() -> closedJoiner().appendAll(IntList.of(1, 2), 0, 2));

        // ... and the one read path that deliberately does not.
        assertEquals("a", closedJoiner().appendTo(new StringBuilder()).toString());
    }

    /**
     * Verifies that append operations check the closed state before argument validation or no-op returns.
     */
    @Test
    public void closedStatePrecedesNoOpsAndArgumentValidation() {
        // Skipped elements still require an open receiver.
        final Joiner skipping = Joiner.with(", ").skipNulls().append("a");
        skipping.close();
        refusesWithIse(() -> skipping.append((String) null));

        final Joiner skipping2 = Joiner.with(", ").skipNulls().append("a");
        skipping2.close();
        refusesWithIse(() -> skipping2.append((StringBuilder) null));

        // Appending the configured null text also requires an open receiver.
        refusesWithIse(() -> closedJoiner().append((String) null));
        refusesWithIse(() -> closedJoiner().append((StringBuilder) null));

        // Null sources and empty ranges still require an open receiver.
        refusesWithIse(() -> closedJoiner().appendAll((int[]) null));
        refusesWithIse(() -> closedJoiner().appendAll(new int[0]));
        refusesWithIse(() -> closedJoiner().appendAll((IntList) null));
        refusesWithIse(() -> closedJoiner().appendAll(new int[] { 1, 2 }, 1, 1));

        refusesWithIse(() -> closedJoiner().appendEntries((Map<?, ?>) null));
        refusesWithIse(() -> closedJoiner().appendEntries(new LinkedHashMap<>()));
        refusesWithIse(() -> closedJoiner().appendEntries(Collections.singletonMap("k", "v")));

        // The condition and invalid supplier cannot bypass the closed check.
        refusesWithIse(() -> closedJoiner().appendIf(false, () -> "z"));
        refusesWithIse(() -> closedJoiner().appendIf(true, null));
        refusesWithIse(() -> closedJoiner().appendIf(false, null));
        refusesWithIse(() -> closedJoiner().appendIf(true, () -> "z"));
    }
}
