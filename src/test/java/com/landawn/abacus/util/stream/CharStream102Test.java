package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalChar;

@Tag("new-test")
public class CharStream102Test extends TestBase {

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toList());
        assertEquals(N.asList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toList());
        assertEquals(N.asList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toList());
        assertEquals(N.asList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 },
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toArray());
        assertEquals(N.asList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toList());
        assertEquals(N.asList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.asList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toList());
        assertEquals(N.asList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toArray());
        assertEquals(N.asList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toList());
        assertEquals(N.asList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.asList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).count());
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).count());
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
        rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(10, CharStream.of((char) 1, (char) 2).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).toList());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toList());
        assertEquals(10, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, CharStream.of((char) 1, (char) 2).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).toList());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toList());
        assertEquals(N.asList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).count());
        Object[] indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toArray();
        assertEquals(5, indexed.length);
        indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).toArray();
        assertEquals(4, indexed.length);
        List<IndexedChar> indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().skip(1).toList();
        assertEquals(4, indexedList.size());
        assertEquals(1, indexedList.get(0).index());
        assertEquals((char) 2, indexedList.get(0).value());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Character> skipped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.asList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toList());
        assertEquals(N.asList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toList());
        assertEquals(N.asList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toList());
        assertEquals(N.asList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Character> collected = new ArrayList<>();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).toList());
        collected.clear();
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onEach(collected::add).skip(1).toList());
        collected.clear();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).toList());
        collected.clear();
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onEach(collected::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPeek() {
        List<Character> collected = new ArrayList<>();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toList());
        collected.clear();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendOptional() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toList());

        try {
            CharStream.empty().throwIfEmpty().count();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toList());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toList());

        try {
            CharStream.empty().throwIfEmpty(() -> new IllegalStateException("Empty")).count();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Empty", e.getMessage());
        }
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(0, CharStream.empty().ifEmpty(() -> actionExecuted.set(true)).count());
        assertTrue(actionExecuted.get());
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

        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
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

        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Character> collection = N.asList((char) 2, (char) 3, (char) 4, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toArray());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toList());
        assertEquals(N.asList((char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toArray());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toList());
        assertEquals(N.asList((char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Character> collection = N.asList((char) 2, (char) 3, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toList());
        assertEquals(N.asList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toList());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toList());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.asList(10, 20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.asList(20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.asList(10, 20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.asList(20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.asList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.asList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapArray() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.asList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.asList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.asList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.asList(2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.asList(2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(6, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.asList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.asList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).count());
        List<CharList> collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseTriWithMerge() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.asList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.asList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependArray() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendArray() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyArray() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.asList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(1, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.asList((char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.asList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toList());
        assertEquals(N.asList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.asList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThree() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.asList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.asList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.asList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.asList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThreeDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.asList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.asList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterAsIntStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().boxed().toList());
        assertEquals(N.asList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).toArray());
        assertEquals(N.asList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().boxed().toList());
        assertEquals(N.asList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.asList((Character) (char) 1, (Character) (char) 2, (Character) (char) 3, (Character) (char) 4, (Character) (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toList());
        assertEquals(N.asList((Character) (char) 2, (Character) (char) 3, (Character) (char) 4, (Character) (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.asList((Character) (char) 1, (Character) (char) 2, (Character) (char) 3, (Character) (char) 4, (Character) (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toList());
        assertEquals(N.asList((Character) (char) 2, (Character) (char) 3, (Character) (char) 4, (Character) (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedByEmpty() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.asList(), CharStream.empty().toList());
        assertEquals(N.asList(), CharStream.empty().skip(1).toList());
        assertEquals(0, CharStream.empty().map(e -> e).count());
        assertEquals(0, CharStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.asList(), CharStream.empty().map(e -> e).toList());
        assertEquals(N.asList(), CharStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByDefer() {
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfNullable() {
        assertEquals(1, CharStream.ofNullable((Character) 'a').count());
        assertEquals(0, CharStream.ofNullable((Character) 'a').skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable((Character) 'a').toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) 'a').skip(1).toArray());
        assertEquals(N.asList('a'), CharStream.ofNullable((Character) 'a').toList());
        assertEquals(N.asList(), CharStream.ofNullable((Character) 'a').skip(1).toList());
        assertEquals(1, CharStream.ofNullable((Character) 'a').map(e -> e).count());
        assertEquals(0, CharStream.ofNullable((Character) 'a').map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable((Character) 'a').map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) 'a').map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a'), CharStream.ofNullable((Character) 'a').map(e -> e).toList());
        assertEquals(N.asList(), CharStream.ofNullable((Character) 'a').map(e -> e).skip(1).toList());

        assertEquals(0, CharStream.ofNullable((Character) null).count());
        assertEquals(0, CharStream.ofNullable((Character) null).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).skip(1).toArray());
        assertEquals(N.asList(), CharStream.ofNullable((Character) null).toList());
        assertEquals(N.asList(), CharStream.ofNullable((Character) null).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArray() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArrayWithRange() {
        char[] array = new char[] { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.asList((char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequence() {
        assertEquals(5, CharStream.of("abcde").count());
        assertEquals(4, CharStream.of("abcde").skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of("abcde").skip(1).toList());
        assertEquals(5, CharStream.of("abcde").map(e -> e).count());
        assertEquals(4, CharStream.of("abcde").map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequenceWithRange() {
        assertEquals(3, CharStream.of("abcdefg", 2, 5).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).skip(1).toArray());
        assertEquals(N.asList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).toList());
        assertEquals(N.asList('d', 'e'), CharStream.of("abcdefg", 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of("abcdefg", 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).toList());
        assertEquals(N.asList('d', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArray() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(5, CharStream.of(array).count());
        assertEquals(4, CharStream.of(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of(array).skip(1).toList());
        assertEquals(5, CharStream.of(array).map(e -> e).count());
        assertEquals(4, CharStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArrayWithRange() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.asList('c', 'd', 'e'), CharStream.of(array, 2, 5).toList());
        assertEquals(N.asList('d', 'e'), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('c', 'd', 'e'), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.asList('d', 'e'), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCollection() {
        List<Character> collection = N.asList('a', 'b', 'c', 'd', 'e');
        assertEquals(5, CharStream.of(collection).count());
        assertEquals(4, CharStream.of(collection).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of(collection).skip(1).toList());
        assertEquals(5, CharStream.of(collection).map(e -> e).count());
        assertEquals(4, CharStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfIterator() {
        CharIterator iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(5, CharStream.of(iterator).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(4, CharStream.of(iterator).skip(1).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(iterator).toArray());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(iterator).skip(1).toArray());
    }

    @Test
    public void testStreamCreatedByOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(5, CharStream.of(buffer).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(4, CharStream.of(buffer).skip(1).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(buffer).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(buffer).skip(1).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.of(buffer).toList());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.of(buffer).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlatten2D() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenVertically() {
        char[][] array = new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        assertEquals(6, CharStream.flatten(array, true).count());
        assertEquals(5, CharStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.asList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).toList());
        assertEquals(N.asList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.asList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenWithAlignment() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(6, CharStream.flatten(array, '*', false).count());
        assertEquals(5, CharStream.flatten(array, '*', false).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).skip(1).toArray());
        assertEquals(N.asList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).toList());
        assertEquals(N.asList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, '*', false).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, '*', false).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).toList());
        assertEquals(N.asList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlatten3D() {
        char[][][] array = new char[][][] { { { 'a', 'b' }, { 'c' } }, { { 'd', 'e' } } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRange() {
        assertEquals(5, CharStream.range((char) 1, (char) 6).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).skip(1).toList());
        assertEquals(5, CharStream.range((char) 1, (char) 6).map(e -> e).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeWithStep() {
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).toList());
        assertEquals(N.asList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.asList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeClosed() {
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeClosedWithStep() {
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).toList());
        assertEquals(N.asList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.asList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRepeat() {
        assertEquals(5, CharStream.repeat('a', 5).count());
        assertEquals(4, CharStream.repeat('a', 5).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).skip(1).toArray());
        assertEquals(N.asList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).toList());
        assertEquals(N.asList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).skip(1).toList());
        assertEquals(5, CharStream.repeat('a', 5).map(e -> e).count());
        assertEquals(4, CharStream.repeat('a', 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).toList());
        assertEquals(N.asList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toList());
        count[0] = 0;
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toList());
        count[0] = 0;
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithInitAndPredicate() {
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateInfinite() {
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByGenerate() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).toList());
        count[0] = 0;
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.asList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toList());
        count[0] = 0;
        assertEquals(N.asList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByConcatArrays() {
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toList());
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toArray());
        assertEquals(N.asList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toList());
        assertEquals(N.asList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObjCollection() {
        assertEquals(6, CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.asList("A" + e, "B" + e)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" }, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.asList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toList());
        assertEquals(N.asList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toList());
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.asList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toList());
        assertEquals(N.asList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObjArray() {
        assertEquals(6, CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.asList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
    }

}
