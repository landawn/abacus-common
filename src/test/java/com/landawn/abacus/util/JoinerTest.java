package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class JoinerTest extends JoinerTestSupport {

    @Test
    public void testWith() {
        assertEquals("a, b", Joiner.withDefault().append("a").append("b").toString());
        assertEquals("k=v", Joiner.withDefault().appendEntry("k", "v").toString());
        assertEquals("a, b, c", Joiner.withDefault().append("a").append("b").append("c").toString());
        assertEquals("a, b, c", Joiner.with(", ").append("a").append("b").append("c").toString());
        assertEquals("a#b#c", Joiner.with("#").append("a").append("b").append("c").toString());
        assertEquals("x|y|z", Joiner.with("|").append("x").append("y").append("z").toString());
        assertEquals("a --- b --- c", Joiner.with(" --- ").append("a").append("b").append("c").toString());
        assertEquals("k:v,a:1", Joiner.with(",", ":").appendEntry("k", "v").appendEntry("a", 1).toString());
        assertEquals("key1->val1;key2->val2", Joiner.with(";", "->").appendEntry("key1", "val1").appendEntry("key2", "val2").toString());
        assertEquals("[a, b, c]", Joiner.with(", ", "[", "]").append("a").append("b").append("c").toString());
        assertEquals("{a,b,c}", Joiner.with(",", "{", "}").append("a").append("b").append("c").toString());
        assertEquals("START-a|b|c-END", Joiner.with("|", "START-", "-END").append("a").append("b").append("c").toString());
        assertEquals("{a=1, b=2}", Joiner.with(", ", "=", "{", "}").appendEntry("a", 1).appendEntry("b", 2).toString());
        assertEquals("<name:Alice;age:30>", Joiner.with(";", ":", "<", ">").appendEntry("name", "Alice").appendEntry("age", "30").toString());
        assertEquals("a, b, c", Joiner.with(", ").appendAll(Arrays.asList("a", "b", "c")).toString());
        assertEquals("abc", Joiner.with("").append("a").append("b").append("c").toString());
        assertEquals("ab", Joiner.with("", "", "", "").append("a").append("b").toString());
        assertEquals("Hello,世界,🌍", Joiner.with(",").append("Hello").append("世界").append("🌍").toString());
        assertEquals("a\nb\tc\rd\te\tf", Joiner.with("\t").append("a\nb").append("c\rd").append("e\tf").toString());
        assertEquals("false, 0, 3, 5.0, NULL, NULL",
                Joiner.withDefault().useForNull("NULL").append(false).append(0).append(3L).append(5d).append((Boolean) null).append((Double) null).toString());
    }

    @Test
    public void testWith_EdgeCase() {
        assertEquals("<>", Joiner.with(",", "<", ">").toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with((CharSequence) null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(null, "="));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(null, "=", "{", "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", null, "{", "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", "=", null, "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", "=", "{", null));
    }

    @Test
    public void testSetEmptyValue() {
        assertEquals("NONE", Joiner.with(", ").setEmptyValue("NONE").toString());
        assertEquals("EMPTY", Joiner.with(", ").setEmptyValue("EMPTY").toString());
        assertEquals("[]", Joiner.with(", ", "[", "]").toString());

        final Joiner joiner = Joiner.with(",");
        assertSame(joiner, joiner.setEmptyValue("EMPTY"));
        assertEquals("EMPTY", joiner.toString());
        joiner.append("test");
        assertEquals("test", joiner.toString());
    }

    @Test
    public void testSetEmptyValue_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").setEmptyValue(null));
    }

    @Test
    public void testTrimBeforeAppend() {
        assertEquals("a,b,c", Joiner.with(",").trimBeforeAppend().append("  a  ").append("  b  ").append("  c  ").toString());
        assertEquals("text", Joiner.with(",").trimBeforeAppend().append(new StringBuilder("  text  ")).toString());
        assertEquals("padded", Joiner.with(",").trimBeforeAppend().append((Object) "  padded  ").toString());
        assertEquals("hello world", Joiner.with(",").trimBeforeAppend().append("  hello world  ", 0, 15).toString());
    }

    @Test
    public void testStripBeforeAppend() {
        final String emSpace = " ";
        assertEquals("a,b,c",
                Joiner.with(",").stripBeforeAppend().append(emSpace + "a" + emSpace).append(" b ").append((Object) (emSpace + "c" + emSpace)).toString());
        assertEquals(emSpace + "a" + emSpace, Joiner.with(",").trimBeforeAppend().append(emSpace + "a" + emSpace).toString());
        assertEquals("key=value",
                Joiner.with(",").stripBeforeAppend().appendEntry(emSpace + "key" + emSpace, new StringBuilder(emSpace + "value" + emSpace)).toString());
        assertEquals(emSpace + "a" + emSpace, Joiner.with(",").stripBeforeAppend().trimBeforeAppend().append(emSpace + "a" + emSpace).toString());
        assertEquals("a", Joiner.with(",").trimBeforeAppend().stripBeforeAppend().append(emSpace + "a" + emSpace).toString());
    }

    @Test
    public void testSkipNulls() {
        assertEquals("a,b,c", Joiner.with(",").skipNulls().append("a").append((String) null).append("b").append((Object) null).append("c").toString());
        assertEquals("a, b", Joiner.with(", ").skipNulls().append("a").append((Object) null).append("b").toString());
        assertEquals("", Joiner.with(",").skipNulls().append((String) null).toString());
        assertEquals("null", Joiner.with(",").append((String) null).toString());
        assertEquals("<<key1->value1|key2->null|key3->value3>>",
                Joiner.with("|", "->", "<<", ">>")
                        .trimBeforeAppend()
                        .skipNulls()
                        .appendEntry("  key1  ", "  value1  ")
                        .appendEntry("key2", (String) null)
                        .appendEntry("key3", "value3")
                        .toString());
    }

    @Test
    public void testUseForNull() {
        assertEquals("a,NULL,b", Joiner.with(",").useForNull("NULL").append("a").append((String) null).append("b").toString());
        assertEquals("a,,b", Joiner.with(",").useForNull("").append("a").append((String) null).append("b").toString());
        assertEquals("null", Joiner.with(",").useForNull(null).append((String) null).toString());
        assertEquals("a, N/A, b", Joiner.with(", ").useForNull("N/A").append("a").append((java.util.List<?>) null).append("b").toString());

        final Joiner joiner = Joiner.with(",");
        assertSame(joiner, joiner.useForNull("N/A"));
        joiner.append((String) null);
        assertEquals("N/A", joiner.toString());
    }

    @Test
    public void testReuseBuffer() {
        final Joiner joiner = Joiner.with(",").reuseBuffer();
        joiner.append("a").append("b");
        assertEquals("a,b", joiner.toString());
        assertEquals("a,b,c", joiner.append("c").toString());

        try (Joiner pooled = Joiner.with(",").reuseBuffer()) {
            pooled.append("a").append("b").append("c");
            assertEquals("a,b,c", pooled.toString());
        }
    }

    @Test
    public void testReuseBuffer_EdgeCase() {
        final Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IllegalStateException.class, joiner::reuseBuffer);
        assertThrows(IllegalStateException.class, () -> Joiner.with(", ").append("a").reuseBuffer());
    }

    @Test
    public void testRepeat() {
        assertEquals("x,x,x", Joiner.with(",").repeat("x", 3).toString());
        assertEquals("a,a,a,a,a,a,a,a,a,a", Joiner.with(",").repeat("a", 10).toString());
        assertEquals("42,42,42", Joiner.with(",").repeat(42, 3).toString());
        assertEquals("x-x-x", Joiner.with("-").repeat("x", 3).toString());
        assertEquals("5-5-5", Joiner.with("-").repeat(5, 3).toString());
        assertEquals("a, a, a", Joiner.with(", ").repeat("a", 3).toString());
        assertEquals("ha", Joiner.with("").repeat("ha", 1).toString());

        final String large = Joiner.with(",").repeat("x", 15).toString();
        assertEquals(15, large.split(",").length);
        assertEquals("x,x,x,x,x,x,x,x,x,x,x,x,x,x,x", large);
    }

    @Test
    public void testRepeat_EdgeCase() {
        assertEquals("start,end", Joiner.with(",").append("start").repeat("x", 0).append("end").toString());
        assertEquals("start,end", Joiner.with(",").append("start").repeat(42, 0).append("end").toString());
        assertEquals("", Joiner.with(", ").repeat("a", 0).toString());
        assertEquals("null,null,null", Joiner.with(",").repeat((String) null, 3).toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").repeat("x", -1));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").repeat(new Object(), -1));

        final Object unevaluated = new Object() {
            @Override
            public String toString() {
                throw new AssertionError("zero repetitions must not evaluate the value");
            }
        };
        assertEquals("", Joiner.with(",").repeat(unevaluated, 0).toString());

        final String slow = Joiner.with(" ").trimBeforeAppend().repeat(" ", 9).toString();
        final String fast = Joiner.with(" ").trimBeforeAppend().repeat(" ", 10).toString();
        assertEquals(8, slow.length());
        assertEquals(9, fast.length());

        final String nullText = "  NIL  ";
        assertEquals("  NIL  |  NIL  ", Joiner.with("|").trimBeforeAppend().useForNull(nullText).repeat((String) null, 2).toString());
        assertEquals(Strings.repeat(nullText, 10, "|"), Joiner.with("|").trimBeforeAppend().useForNull(nullText).repeat((String) null, 10).toString());
    }

    @Test
    public void testMerge() {
        final Joiner joiner1 = Joiner.with(",", "[", "]").append("a").append("b");
        joiner1.merge(Joiner.with("-").append("c").append("d"));
        assertEquals("[a,b,c-d]", joiner1.toString());

        final Joiner sameConfig = Joiner.with(",", "[", "]").append("a").append("b");
        sameConfig.merge(Joiner.with(",", "[", "]").append("c").append("d"));
        assertEquals("[a,b,c,d]", sameConfig.toString());

        assertEquals("c|d", Joiner.with(", ").merge(Joiner.with("|").append("c").append("d")).toString());
    }

    @Test
    public void testMerge_EdgeCase() {
        assertEquals("a", Joiner.with(",").append("a").merge(Joiner.with("-")).toString());
        assertEquals("", Joiner.with(",").merge(Joiner.with("-")).toString());
        assertEquals("[a,b,c-EMPTY-d]",
                Joiner.with(",", "[", "]")
                        .skipNulls()
                        .append("a")
                        .append((String) null)
                        .append("b")
                        .merge(Joiner.with("-", "{", "}").useForNull("EMPTY").append("c").append((String) null).append("d"))
                        .toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").merge(null));

        final Joiner other = Joiner.with(",").reuseBuffer().append("x").append("y");
        assertEquals("x,y", other.toString());
        assertEquals("a;x,y", Joiner.with(";").append("a").merge(other).toString());
    }

    @Test
    public void testLength() {
        final Joiner joiner = Joiner.with(",");
        assertEquals(0, joiner.length());
        joiner.append("hello");
        assertEquals(5, joiner.length());
        joiner.append("world");
        assertEquals(11, joiner.length());

        final Joiner wrapped = Joiner.with(",", "[", "]");
        assertEquals(2, wrapped.length());
        wrapped.append("a");
        assertEquals(3, wrapped.length());
        wrapped.append("b");
        assertEquals(5, wrapped.length());

        final Joiner afterToString = Joiner.with(",").append("test");
        afterToString.toString();
        assertEquals(4, afterToString.length());
    }

    @Test
    public void testToString() {
        assertEquals("", Joiner.with(", ").toString());
        assertEquals("a", Joiner.with(", ").append("a").toString());
        assertEquals("a,b,c", Joiner.with(",").append("a").append("b").append("c").toString());
        assertEquals("[a, b, c]", Joiner.with(", ", "[", "]").append("a").append("b").append("c").toString());
        assertEquals("[]", Joiner.with(",", "[", "]").toString());
        assertEquals("EMPTY", Joiner.with(",").setEmptyValue("EMPTY").toString());
        assertEquals("", Joiner.with(", ").appendAll(new int[0]).toString());
        assertEquals("", Joiner.with(", ").appendAll((int[]) null).toString());
        assertEquals("", Joiner.with(", ").appendAll(Collections.emptyList()).toString());

        final Joiner joiner = Joiner.with(",").append("a").append("b");
        assertEquals("a,b", joiner.toString());
        assertEquals("a,b,c", joiner.append("c").toString());

        final Joiner reused = Joiner.with(", ", "=", "{", "}").reuseBuffer();
        assertEquals("{a=1}", reused.appendEntry("a", 1).toString());
        assertEquals("{a=1, b=2}", reused.appendEntry("b", 2).toString());
    }

    @Test
    public void testMap() {
        assertEquals(Integer.valueOf(5), Joiner.with(",").append("a").append("b").append("c").map(String::length));
        assertEquals("HELLO,WORLD", Joiner.with(",").append("hello").append("world").map(String::toUpperCase));
        assertEquals(Integer.valueOf(0), Joiner.with(",").map(String::length));
    }

    @Test
    public void testMapIfNotEmpty() {
        assertFalse(Joiner.with(",").mapIfNotEmpty(String::length).isPresent());
        assertTrue(Joiner.with(",").mapIfNotEmpty(s -> s.length()).isEmpty());
        assertFalse(Joiner.with(",", "[", "]").mapIfNotEmpty(String::length).isPresent());

        final Joiner joiner = Joiner.with(",").append("a").append("b");
        assertTrue(joiner.mapIfNotEmpty(String::length).isPresent());
        assertEquals(Integer.valueOf(3), joiner.mapIfNotEmpty(String::length).get());

        final AtomicBoolean emptyCalled = new AtomicBoolean(false);
        assertFalse(Joiner.with(",").mapIfNotEmpty(s -> {
            emptyCalled.set(true);
            return s;
        }).isPresent());
        assertFalse(emptyCalled.get());

        final AtomicBoolean called = new AtomicBoolean(false);
        final u.Optional<String> mapped = Joiner.with(",").append("a").mapIfNotEmpty(s -> {
            called.set(true);
            return "mapped:" + s;
        });
        assertTrue(called.get());
        assertEquals("mapped:a", mapped.get());

        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").append("a").mapIfNotEmpty(null));
    }

    @Test
    public void testClose() {
        final Joiner skipped = Joiner.with(",").skipNulls();
        skipped.close();
        assertSame(skipped, skipped.appendAll(new String[0]));
        assertSame(skipped, skipped.append((String) null));
        assertThrows(IllegalStateException.class, () -> skipped.append((CharSequence) null));
        assertThrows(IllegalStateException.class, () -> skipped.merge(Joiner.with(",")));

        final Joiner closed = Joiner.with(",").reuseBuffer().append("a").append("b");
        closed.close();
        assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed.append("c")).getMessage());

        final Joiner joiner = Joiner.with(",");
        joiner.close();
        joiner.close();
        assertEquals("", joiner.toString());

        final Joiner plain = Joiner.with(",").append("a").append("b");
        plain.close();
        assertEquals("a,b", plain.toString());

        final Joiner reused = Joiner.with(",").reuseBuffer().append("a");
        assertEquals("a", reused.toString());
        reused.close();
        assertEquals("a", reused.toString());
    }

    @Test
    public void testMethodChaining() {
        final Person person = new Person("John", null, "NYC", null);
        assertEquals("name=John, age=null", Joiner.with(", ").appendBean(person, Arrays.asList("name", "age")).toString());
        assertEquals("name=John", Joiner.with(", ").appendBean(person, true, Collections.singleton("city")).toString());

        assertEquals("a,b", Joiner.with(",").skipNulls().trimBeforeAppend().append("  a  ").append((String) null).append("  b  ").toString());
    }

    @Test
    public void testReuseBufferObserversAfterToString() {
        final Joiner j = Joiner.with(",").reuseBuffer();
        j.append("a").append("b");

        assertEquals("a,b", j.toString());
        assertEquals("a,b", j.toString());
        assertEquals(3, j.length());
        assertTrue(j.mapIfNotEmpty(String::length).isPresent());

        final StringBuilder sb = new StringBuilder();
        try {
            j.appendTo(sb);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("a,b", sb.toString());

        j.append("c");
        assertEquals("a,b,c", j.toString());
    }

    @Test
    public void testLargeDataset() {
        final String payload = "payload".repeat(1000);
        assertEquals(payload + "|" + payload, Joiner.with("|").appendAll(Arrays.asList(payload, payload)).toString());

        final Joiner joiner = Joiner.with(",");
        for (int i = 0; i < 1000; i++) {
            joiner.append(i);
        }
        final String result = joiner.toString();
        assertTrue(result.startsWith("0,1,2"));
        assertTrue(result.endsWith("997,998,999"));

        final StringBuilder largeBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBuilder.append("x");
        }
        final String large = Joiner.with(",").append(largeBuilder).append("end").toString();
        assertTrue(large.startsWith("xxxx"));
        assertTrue(large.endsWith("x,end"));
    }

    @Test
    public void reviewFixes20260906_appendToRejectsNullInEveryState() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendTo(null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").setEmptyValue("").appendTo(null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").append("a").appendTo(null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ", "[", "]").appendTo(null));

        final StringBuilder sb = new StringBuilder("Result: ");
        Joiner.with(", ").append("a").append("b").appendTo(sb);
        assertEquals("Result: a, b", sb.toString());

        final StringBuilder empty = new StringBuilder("x");
        assertSame(empty, Joiner.with(",").appendTo(empty));
        assertEquals("x", empty.toString());

        final StringBuilder withEmptyValue = new StringBuilder();
        Joiner.with(",").setEmptyValue("<none>").appendTo(withEmptyValue);
        assertEquals("<none>", withEmptyValue.toString());
    }

    /**
     * A {@code toString()} racing a {@code close()} used to hand the SAME pooled {@code StringBuilder} back to
     * {@link Objectory} twice. The pool is process-wide, so the next two {@code Objectory.createStringBuilder()}
     * callers anywhere in the JVM -- in code that never touched a {@code Joiner} -- were handed one builder to
     * share and silently interleaved their content. The same window also let {@code toString()} read a buffer
     * that {@code close()} had already reset, returning {@code ""} and breaking {@link Joiner#close()}'s promise
     * that closing never turns accumulated content into the empty value.
     *
     * <p>{@code Joiner} is still documented <b>not thread-safe</b>, and this pins only the double-release window
     * between {@code toString()} and {@code close()}. An {@code append()} racing a {@code close()} is a second,
     * deliberately open window - {@code append} is not synchronized - and can still hand the shared pool a buffer
     * the appending thread is writing into; that stays unsupported. The check is necessarily statistical: a single
     * race wins the window only now and then - on the unfixed build 19 to 53 of the 4000 trials below did, across
     * eight measured runs - so it is the trial count that makes a false green vanishingly unlikely. With
     * {@code close()}, {@code toString()} and the buffer release all holding the same monitor, both counters are
     * structurally zero.</p>
     */
    @Test
    public void concurrentToStringAndCloseNeverDoubleRecyclesThePooledBuffer() throws Exception {
        final int trials = 4000;

        final Field poolField = Objectory.class.getDeclaredField("stringBuilderPool");
        poolField.setAccessible(true);

        @SuppressWarnings("unchecked")
        final Queue<StringBuilder> pool = (Queue<StringBuilder>) poolField.get(null);

        // Borrow the whole shared pool for the duration and hand it back in the finally. Draining it once keeps a
        // pool left full by an earlier test from making Objectory.recycle() a silent no-op, which would hide the
        // very double release this looks for; emptying it per trial instead would force 4000 fresh builders and so
        // fire Objectory's every-1000th-object WARN -- stack trace included -- into the shared suite log four
        // times. Borrowed, each trial just re-polls the builder the previous trial recycled.
        final List<StringBuilder> borrowed = new ArrayList<>();

        for (StringBuilder pooled = pool.poll(); pooled != null; pooled = pool.poll()) {
            borrowed.add(pooled);
        }

        try {
            assertTimeoutPreemptively(Duration.ofMinutes(3), () -> {
                int doubleRecycled = 0;
                int wrongValue = 0;

                for (int i = 0; i < trials; i++) {
                    final Joiner joiner = Joiner.with(",").reuseBuffer().append("a").append("b");
                    final CyclicBarrier start = new CyclicBarrier(2);
                    final AtomicReference<String> rendered = new AtomicReference<>();

                    final Thread reader = new Thread(() -> {
                        awaitQuietly(start);
                        rendered.set(joiner.toString());
                    });

                    final Thread closer = new Thread(() -> {
                        awaitQuietly(start);
                        joiner.close();
                    });

                    reader.start();
                    closer.start();
                    reader.join();
                    closer.join();

                    // An identity duplicate in the pool is one StringBuilder waiting in two slots: two later,
                    // unrelated borrowers would be handed the same instance.
                    final IdentityHashMap<StringBuilder, Boolean> seen = new IdentityHashMap<>();
                    boolean duplicated = false;

                    for (final StringBuilder pooled : pool) {
                        if (seen.put(pooled, Boolean.TRUE) != null) {
                            duplicated = true;
                            break;
                        }
                    }

                    if (duplicated) {
                        doubleRecycled++;

                        // Drop the duplicate so one contaminated trial is not re-counted by every later one.
                        pool.clear();
                    }

                    if (!"a,b".equals(rendered.get())) {
                        wrongValue++;
                    }
                }

                // Both counters go into one message: the first failing assertion aborts the method, so the second
                // symptom would otherwise never be reported on a red build.
                final String summary = doubleRecycled + " of " + trials
                        + " toString()/close() races returned the same pooled StringBuilder to Objectory twice, and "
                        + wrongValue + " of " + trials + " racing toString() calls did not return \"a,b\"";

                assertEquals(0, doubleRecycled, summary);
                assertEquals(0, wrongValue, summary);
            });
        } finally {
            pool.clear();

            for (final StringBuilder pooled : borrowed) {
                pool.offer(pooled);
            }
        }
    }

    /**
     * Pins the scope of the two monitors the class javadoc documents: {@link Joiner#toString()} and
     * {@link Joiner#close()} are {@code synchronized}, so the pooled buffer cannot be released to {@link Objectory}
     * twice, while the {@code append} family deliberately is not - which is why appending concurrently stays
     * unsupported and why the race test above pins only the {@code toString()}/{@code close()} window.
     */
    @Test
    public void onlyToStringAndCloseHoldTheMonitorTheAppendFamilyDoesNot() throws Exception {
        assertTrue(Modifier.isSynchronized(Joiner.class.getDeclaredMethod("toString").getModifiers()));
        assertTrue(Modifier.isSynchronized(Joiner.class.getDeclaredMethod("close").getModifiers()));

        assertFalse(Modifier.isSynchronized(Joiner.class.getMethod("append", String.class).getModifiers()));
        assertFalse(Modifier.isSynchronized(Joiner.class.getMethod("append", Object.class).getModifiers()));
        assertFalse(Modifier.isSynchronized(Joiner.class.getMethod("appendEntry", String.class, String.class).getModifiers()));
    }

    private static void awaitQuietly(final CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
