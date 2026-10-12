package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.N;

/** CharStream lifecycle contracts. */
public class CharStreamLifecycleTest extends CharStreamTestSupport {

    @Test
    public void testAutoClose() {
        AtomicInteger closeCount = new AtomicInteger(0);
        try (CharStream stream = createCharStream('a', 'b', 'c').onClose(closeCount::incrementAndGet)) {
            stream.count();
        }
        assertThat(closeCount.get()).isEqualTo(1);
    }

    // close

    @Test
    public void testClose() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.close();
        assertNotNull(stream);
    }

    @Test
    public void testOfReaderWithCloseFlag() {
        Reader reader = new StringReader("abc");
        CharStream stream = CharStream.of(reader, true);
        assertEquals(3, stream.count());
        stream.close();
    }

    @Test
    public void testOfReaderWithCloseFlagFalse() {
        Reader reader = new StringReader("xyz");
        CharStream stream = CharStream.of(reader, false);
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
        stream.close();
    }

    // onClose

    @Test
    public void testOnClose() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> closeActions = new ArrayList<>();
        stream.onClose(() -> closeActions.add("closed"));
        stream.close();
        assertEquals(Arrays.asList("closed"), closeActions);
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        CharStream stream = createCharStream('a', 'b');
        List<String> actions = new ArrayList<>();
        stream.onClose(() -> actions.add("first")).onClose(() -> actions.add("second"));
        stream.close();
        assertEquals(2, actions.size());
        assertEquals("first", actions.get(0));
        assertEquals("second", actions.get(1));
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testParallel_WithExecutor() {
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithMaxThreadNumAndExecutor() {
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(2, executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    // rangeClosed

    @Test
    public void testRangeClosed_SingleElement() {
        CharStream stream = CharStream.rangeClosed('c', 'c');
        assertArrayEquals(new char[] { 'c' }, stream.toArray());
    }

    @Test
    public void testRangeClosed_ThreeParams() {
        CharStream stream = CharStream.rangeClosed('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e', 'g' }, stream.toArray());
    }

    @Test
    public void testRangeClosed_TwoParams() {
        CharStream stream = CharStream.rangeClosed('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new char[] { 'a' }, CharStream.rangeClosed('a', 'a').toArray());
    }

    @Test
    public void testRangeClosedWithNegativeStepForCoverage() {
        CharStream stream = CharStream.rangeClosed('j', 'a', -2);
        assertArrayEquals(new char[] { 'j', 'h', 'f', 'd', 'b' }, stream.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        List<Character> result = CharStream.rangeClosed('a', 'e', 2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = CharStream.rangeClosed('e', 'a', -2).boxed().toList();
        assertEquals(Arrays.asList('e', 'c', 'a'), result);

        result = CharStream.rangeClosed('a', 'b', 5).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testStreamCreatedByRangeClosed() {
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeClosedWithStep() {
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).toList());
        assertEquals(N.toList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

}
