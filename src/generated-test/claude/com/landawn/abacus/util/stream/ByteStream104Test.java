package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteSupplier;

public class ByteStream104Test extends TestBase {
    // Test 1: filter(P predicate)
    @Test
    public void testFilter() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    // Test 2: filter(P predicate, C actionOnDroppedItem)
    @Test
    public void testFilterWithAction() {
        List<Byte> dropped = new ArrayList<>();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).toList());

        dropped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).filter(i -> i > 2, dropped::add).skip(1).toList());

        dropped.clear();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());

        dropped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    // Test 3: takeWhile(P predicate)
    @Test
    public void testTakeWhile() {
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new byte[] { 2 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((byte) 2), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new byte[] { 2 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.asList((byte) 2), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    // Test 4: dropWhile(P predicate)
    @Test
    public void testDropWhile() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    // Test 5: dropWhile(P predicate, C actionOnDroppedItem)
    @Test
    public void testDropWhileWithAction() {
        List<Byte> dropped = new ArrayList<>();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).toList());

        dropped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());

        dropped.clear();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());

        dropped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());

        dropped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());

        dropped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());

        dropped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    // Test 6: skipUntil(P predicate)
    @Test
    public void testSkipUntil() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    // Test 7: distinct()
    @Test
    public void testDistinct() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().toArray());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().toList());
        assertEquals(N.asList((byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).distinct().skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().toList());
        assertEquals(N.asList((byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 3).map(e -> e).distinct().skip(1).toList());
    }

    // Test 8: intersection(Collection<?> c)
    @Test
    public void testIntersection() {
        List<Byte> collection = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).skip(1).count());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).toArray());
        assertArrayEquals(new byte[] { 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).toList());
        assertEquals(N.asList((byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).intersection(collection).skip(1).toList());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).skip(1).count());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).toArray());
        assertArrayEquals(new byte[] { 3 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).toList());
        assertEquals(N.asList((byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).intersection(collection).skip(1).toList());
    }

    // Test 9: difference(Collection<?> c)
    @Test
    public void testDifference() {
        List<Byte> collection = Arrays.asList((byte) 2, (byte) 3, (byte) 6);
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).difference(collection).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).toArray());
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).toList());
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).difference(collection).skip(1).toList());
    }

    // Test 10: symmetricDifference(Collection<T> c)
    @Test
    public void testSymmetricDifference() {
        List<Byte> collection = Arrays.asList((byte) 3, (byte) 4, (byte) 6);
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).count());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 5, 6 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).toArray());
        assertArrayEquals(new byte[] { 2, 5, 6 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).toList());
        assertEquals(N.asList((byte) 2, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).symmetricDifference(collection).skip(1).toList());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).count());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 5, 6 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).toArray());
        assertArrayEquals(new byte[] { 2, 5, 6 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).toList());
        assertEquals(N.asList((byte) 2, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).symmetricDifference(collection).skip(1).toList());
    }

    // Test 11: reversed()
    @Test
    public void testReversed() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().skip(1).count());
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().toArray());
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().toList());
        assertEquals(N.asList((byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).reversed().skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().toList());
        assertEquals(N.asList((byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).reversed().skip(1).toList());
    }

    // Test 12: rotated(int distance)
    @Test
    public void testRotated() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).skip(1).count());
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).toArray());
        assertArrayEquals(new byte[] { 5, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).skip(1).toArray());
        assertEquals(N.asList((byte) 4, (byte) 5, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).toList());
        assertEquals(N.asList((byte) 5, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).rotated(2).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new byte[] { 5, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.asList((byte) 4, (byte) 5, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).toList());
        assertEquals(N.asList((byte) 5, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).rotated(2).skip(1).toList());
    }

    // Test 13: shuffled()
    @Test
    public void testShuffled() {
        // Since shuffled is random, we can only check the count and that all elements are present
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().skip(1).count());
        byte[] arr = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().toArray();
        assertEquals(5, arr.length);
        byte[] arr2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().skip(1).toArray();
        assertEquals(4, arr2.length);
        List<Byte> list = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().toList();
        assertEquals(5, list.size());
        List<Byte> list2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled().skip(1).toList();
        assertEquals(4, list2.size());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().skip(1).count());
        byte[] arr3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().toArray();
        assertEquals(5, arr3.length);
        byte[] arr4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().skip(1).toArray();
        assertEquals(4, arr4.length);
        List<Byte> list3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().toList();
        assertEquals(5, list3.size());
        List<Byte> list4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled().skip(1).toList();
        assertEquals(4, list4.size());
    }

    // Test 14: shuffled(Random rnd)
    @Test
    public void testShuffledWithRandom() {
        Random rnd = new Random(42); // Using a seed for reproducibility
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).skip(1).count());
        byte[] arr = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).toArray();
        assertEquals(5, arr.length);
        byte[] arr2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).skip(1).toArray();
        assertEquals(4, arr2.length);
        List<Byte> list = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).toList();
        assertEquals(5, list.size());
        List<Byte> list2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).shuffled(new Random(42)).skip(1).toList();
        assertEquals(4, list2.size());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).skip(1).count());
        byte[] arr3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).toArray();
        assertEquals(5, arr3.length);
        byte[] arr4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).skip(1).toArray();
        assertEquals(4, arr4.length);
        List<Byte> list3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).toList();
        assertEquals(5, list3.size());
        List<Byte> list4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).shuffled(new Random(42)).skip(1).toList();
        assertEquals(4, list4.size());
    }

    // Test 15: sorted()
    @Test
    public void testSorted() {
        assertEquals(5, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().count());
        assertEquals(4, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).sorted().skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().count());
        assertEquals(4, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).sorted().skip(1).toList());
    }

    // Test 16: reverseSorted()
    @Test
    public void testReverseSorted() {
        assertEquals(5, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().count());
        assertEquals(4, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().skip(1).count());
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().toArray());
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().toList());
        assertEquals(N.asList((byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).reverseSorted().skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().count());
        assertEquals(4, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new byte[] { 4, 3, 2, 1 },
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.asList((byte) 4, (byte) 3, (byte) 2, (byte) 1),
                ByteStream.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    // Test 17: cycled()
    @Test
    public void testCycled() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).toArray());
        assertArrayEquals(new byte[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled().limit(10).skip(1).toList());
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new byte[] { 2, 3, 1, 2, 3, 1, 2, 3, 1 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled().limit(10).skip(1).toList());
    }

    // Test 18: cycled(long rounds)
    @Test
    public void testCycledWithRounds() {
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).count());
        assertEquals(8, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).toArray());
        assertArrayEquals(new byte[] { 2, 3, 1, 2, 3, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).cycled(3).skip(1).toList());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).count());
        assertEquals(8, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 1, 2, 3, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).toArray());
        assertArrayEquals(new byte[] { 2, 3, 1, 2, 3, 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3, (byte) 1, (byte) 2, (byte) 3),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).cycled(3).skip(1).toList());
    }

    // Test 19: indexed() - returns Stream<IndexedByte>
    @Test
    public void testIndexed() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().skip(1).count());

        Object[] arr = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().toArray();
        assertEquals(5, arr.length);

        Object[] arr2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().skip(1).toArray();
        assertEquals(4, arr2.length);

        List<?> list = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().toList();
        assertEquals(5, list.size());

        List<?> list2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).indexed().skip(1).toList();
        assertEquals(4, list2.size());

        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().skip(1).count());

        Object[] arr3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().toArray();
        assertEquals(5, arr3.length);

        Object[] arr4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().skip(1).toArray();
        assertEquals(4, arr4.length);

        List<?> list3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().toList();
        assertEquals(5, list3.size());

        List<?> list4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).indexed().skip(1).toList();
        assertEquals(4, list4.size());
    }

    // Test 20: skip(long n) - already inherited from BaseStream, tested implicitly in other tests

    // Test 21: skip(long n, C actionOnSkippedItem)
    @Test
    public void testSkipWithAction() {
        List<Byte> skipped = new ArrayList<>();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).skip(1).count());

        skipped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).skip(1).toArray());

        skipped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(2, skipped::add).skip(1).toList());

        skipped.clear();
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).skip(1).count());

        skipped.clear();
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertArrayEquals(new byte[] { 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());

        skipped.clear();
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2), skipped);

        skipped.clear();
        assertEquals(N.asList((byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    // Test 22: limit(long maxSize)
    @Test
    public void testLimit() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).toArray());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).toList());
        assertEquals(N.asList((byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).limit(3).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new byte[] { 2, 3 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).toList());
        assertEquals(N.asList((byte) 2, (byte) 3), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).limit(3).skip(1).toList());
    }

    // Test 23: step(long step)
    @Test
    public void testStep() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).toArray());
        assertArrayEquals(new byte[] { 3, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).toList());
        assertEquals(N.asList((byte) 3, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).step(2).skip(1).toList());
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).count());
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).toArray());
        assertArrayEquals(new byte[] { 3, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).toList());
        assertEquals(N.asList((byte) 3, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).step(2).skip(1).toList());
    }

    // Test 24-28: rateLimited and delay - Skip these as they involve timing and are not suitable for unit tests

    // Test 29: onEach(C action)
    @Test
    public void testOnEach() {
        List<Byte> processed = new ArrayList<>();
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).skip(1).count());

        processed.clear();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).skip(1).toArray());

        processed.clear();
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onEach(processed::add).skip(1).toList());

        processed.clear();
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).skip(1).count());

        processed.clear();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).skip(1).toArray());

        processed.clear();
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onEach(processed::add).skip(1).toList());
    }

    // Test 30: peek(C action) - same as onEach
    @Test
    public void testPeek() {
        List<Byte> processed = new ArrayList<>();
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).skip(1).count());

        processed.clear();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).skip(1).toArray());

        processed.clear();
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).peek(processed::add).skip(1).toList());

        processed.clear();
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).count());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).skip(1).count());

        processed.clear();
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).toArray());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).skip(1).toArray());

        processed.clear();
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).toList());
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), processed);

        processed.clear();
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).peek(processed::add).skip(1).toList());
    }

    // Test 31-34: prepend/append with streams - These need ByteStream instances, testing basic prepend/append with arrays

    // Test 35-37: throwIfEmpty methods
    @Test
    public void testThrowIfEmpty() {
        // Non-empty stream - should not throw
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).throwIfEmpty().skip(1).toList());

        // Empty stream - should throw
        try {
            ByteStream.empty().throwIfEmpty().count();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }

    // Test 38: onClose(Runnable closeHandler)
    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());

        closed.set(false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());

        closed.set(false);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());

        closed.set(false);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());

        closed.set(false);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());

        closed.set(false);
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    // Test 39: map(ByteUnaryOperator mapper)
    @Test
    public void testMap() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).skip(1).count());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).toArray());
        assertArrayEquals(new byte[] { 4, 6, 8, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).skip(1).toArray());
        assertEquals(N.asList((byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).toList());
        assertEquals(N.asList((byte) 4, (byte) 6, (byte) 8, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> (byte) (e * 2)).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).skip(1).count());
        assertArrayEquals(new byte[] { 2, 4, 6, 8, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).toArray());
        assertArrayEquals(new byte[] { 4, 6, 8, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).skip(1).toArray());
        assertEquals(N.asList((byte) 2, (byte) 4, (byte) 6, (byte) 8, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).toList());
        assertEquals(N.asList((byte) 4, (byte) 6, (byte) 8, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).map(e -> (byte) (e * 2)).skip(1).toList());
    }

    // Test 40: mapToInt(ByteToIntFunction mapper) - returns IntStream
    @Test
    public void testMapToInt() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.asList(10, 20, 30, 40, 50), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).toList());
        assertEquals(N.asList(20, 30, 40, 50), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToInt(e -> e * 10).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.asList(10, 20, 30, 40, 50), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).toList());
        assertEquals(N.asList(20, 30, 40, 50),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).toList());
    }

    // Test 41: mapToObj(ByteFunction<? extends T> mapper) - returns Stream<T>
    @Test
    public void testMapToObj() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).skip(1).count());
        assertArrayEquals(new String[] { "Byte:1", "Byte:2", "Byte:3", "Byte:4", "Byte:5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).toArray());
        assertArrayEquals(new String[] { "Byte:2", "Byte:3", "Byte:4", "Byte:5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).skip(1).toArray());
        assertEquals(N.asList("Byte:1", "Byte:2", "Byte:3", "Byte:4", "Byte:5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).toList());
        assertEquals(N.asList("Byte:2", "Byte:3", "Byte:4", "Byte:5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).mapToObj(e -> "Byte:" + e).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).skip(1).count());
        assertArrayEquals(new String[] { "Byte:1", "Byte:2", "Byte:3", "Byte:4", "Byte:5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).toArray());
        assertArrayEquals(new String[] { "Byte:2", "Byte:3", "Byte:4", "Byte:5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).skip(1).toArray());
        assertEquals(N.asList("Byte:1", "Byte:2", "Byte:3", "Byte:4", "Byte:5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).toList());
        assertEquals(N.asList("Byte:2", "Byte:3", "Byte:4", "Byte:5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).mapToObj(e -> "Byte:" + e).skip(1).toList());
    }

    // Test 42: flatMap(ByteFunction<? extends ByteStream> mapper)
    @Test
    public void testFlatMap() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).toList());
        assertEquals(N.asList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).toList());
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).toList());
        assertEquals(N.asList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMap(e -> ByteStream.of(e, (byte) (e * 2))).skip(1).toList());
    }

    // Test 43: flatmap(ByteFunction<byte[]> mapper)
    @Test
    public void testFlatmapArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.asList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toList());

        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.asList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmap(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toList());
    }

    // Test 44: flatMapToInt(ByteFunction<? extends IntStream> mapper) - returns IntStream
    @Test
    public void testFlatMapToInt() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).toList());
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 10, 2, 20, 3, 30, 4, 40, 5, 50 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).toArray());
        assertEquals(N.asList(1, 10, 2, 20, 3, 30, 4, 40, 5, 50),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).toList());
        assertEquals(N.asList(10, 2, 20, 3, 30, 4, 40, 5, 50),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(1).toList());
    }

    // Test 45: flatMapToObj(ByteFunction<? extends Stream<? extends T>> mapper) - returns Stream<T>
    @Test
    public void testFlatMapToObj() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).toList());
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(1).toList());
    }

    // Test 46: flatmapToObj(ByteFunction<? extends Collection<? extends T>> mapper) - returns Stream<T>
    @Test
    public void testFlatmapToObjCollection() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).skip(1).toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).skip(1).toList());
        assertEquals(10,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .flatmapToObj(e -> Arrays.asList("A" + e, "B" + e))
                        .map(e -> e)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatmapToObj(e -> Arrays.asList("A" + e, "B" + e)).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .flatmapToObj(e -> Arrays.asList("A" + e, "B" + e))
                        .map(e -> e)
                        .skip(1)
                        .toList());
    }

    // Test 47: flattmapToObj(ByteFunction<T[]> mapper) - returns Stream<T>
    @Test
    public void testFlattmapToObjArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).skip(1).toList());
        assertEquals(10,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flattmapToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toArray());
        assertArrayEquals(new String[] { "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flattmapToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flattmapToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.asList("B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flattmapToObj(e -> new String[] { "A" + e, "B" + e })
                        .skip(1)
                        .toList());
    }

    // Test 48: mapPartial(ByteFunction<OptionalByte> mapper)
    @Test
    public void testMapPartial() {
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .count());
        assertEquals(1,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 4, 8 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .toArray());
        assertArrayEquals(new byte[] { 8 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 4, (byte) 8),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .toList());
        assertEquals(N.asList((byte) 8),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .count());
        assertEquals(1,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 4, 8 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .toArray());
        assertArrayEquals(new byte[] { 8 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 4, (byte) 8),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .toList());
        assertEquals(N.asList((byte) 8),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalByte.of((byte) (e * 2)) : OptionalByte.empty())
                        .skip(1)
                        .toList());
    }

    // Test 49: rangeMap(ByteBiPredicate sameRange, ByteBinaryOperator mapper)
    @Test
    public void testRangeMap() {
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11).rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 3, 6, 21 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11).rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 6, 21 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 21),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11).rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 6, (byte) 21),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 3, 6, 21 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .toArray());
        assertArrayEquals(new byte[] { 6, 21 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 21),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .toList());
        assertEquals(N.asList((byte) 6, (byte) 21),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMap((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toList());
    }

    // Test 50: rangeMapToObj(ByteBiPredicate sameRange, ByteBiFunction<? extends T> mapper) - returns Stream<T>
    @Test
    public void testRangeMapToObj() {
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "Range[1,2]", "Range[3,3]", "Range[10,11]" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .toArray());
        assertArrayEquals(new String[] { "Range[3,3]", "Range[10,11]" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("Range[1,2]", "Range[3,3]", "Range[10,11]"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .toList());
        assertEquals(N.asList("Range[3,3]", "Range[10,11]"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "Range[1,2]", "Range[3,3]", "Range[10,11]" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .toArray());
        assertArrayEquals(new String[] { "Range[3,3]", "Range[10,11]" },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .toArray());
        assertEquals(N.asList("Range[1,2]", "Range[3,3]", "Range[10,11]"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .toList());
        assertEquals(N.asList("Range[3,3]", "Range[10,11]"),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 10, (byte) 11)
                        .map(e -> e)
                        .rangeMapToObj((a, b) -> Math.abs(b - a) <= 1, (a, b) -> "Range[" + a + "," + b + "]")
                        .skip(1)
                        .toList());
    }

    // Test 51: collapse(ByteBiPredicate collapsible) - returns Stream<ByteList>
    @Test
    public void testCollapse() {
        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).count());

        Object[] arr = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).toArray();
        assertEquals(2, arr.length);

        Object[] arr2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toArray();
        assertEquals(1, arr2.length);

        List<ByteList> list = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).toList();
        assertEquals(2, list.size());

        List<ByteList> list2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toList();
        assertEquals(1, list2.size());

        assertEquals(2, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).count());
        assertEquals(1, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).count());

        Object[] arr3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).toArray();
        assertEquals(2, arr3.length);

        Object[] arr4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).skip(1).toArray();
        assertEquals(1, arr4.length);

        List<ByteList> list3 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).map(e -> e).collapse((a, b) -> Math.abs(b - a) <= 1).toList();
        assertEquals(2, list3.size());

        List<ByteList> list4 = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                .map(e -> e)
                .collapse((a, b) -> Math.abs(b - a) <= 1)
                .skip(1)
                .toList();
        assertEquals(1, list4.size());
    }

    // Test 52-53: collapse with merge functions
    @Test
    public void testCollapseWithMerge() {
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).count());
        assertEquals(1,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 6, 11 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 11 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 6, (byte) 11),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6).collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 11),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .count());
        assertEquals(1,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 6, 11 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .toArray());
        assertArrayEquals(new byte[] { 11 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 6, (byte) 11),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .toList());
        assertEquals(N.asList((byte) 11),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 5, (byte) 6)
                        .map(e -> e)
                        .collapse((a, b) -> Math.abs(b - a) <= 1, (a, b) -> (byte) (a + b))
                        .skip(1)
                        .toList());
    }

    // Test 54-56: scan methods
    @Test
    public void testScan() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 10, 15 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 3, 6, 10, 15 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 6, (byte) 10, (byte) 15),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 10, (byte) 15),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((a, b) -> (byte) (a + b)).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 10, 15 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 3, 6, 10, 15 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 6, (byte) 10, (byte) 15),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 10, (byte) 15),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((a, b) -> (byte) (a + b)).skip(1).toList());
    }

    @Test
    public void testScanWithInit() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, (a, b) -> (byte) (a + b)).skip(1).toList());
    }

    @Test
    public void testScanWithInitIncluded() {
        assertEquals(6, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).count());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 10, 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 10, (byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).toList());
        assertEquals(6, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).count());
        assertEquals(5,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).count());
        assertArrayEquals(new byte[] { 10, 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).toArray());
        assertArrayEquals(new byte[] { 11, 13, 16, 20, 25 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).toArray());
        assertEquals(N.asList((byte) 10, (byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).toList());
        assertEquals(N.asList((byte) 11, (byte) 13, (byte) 16, (byte) 20, (byte) 25),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).scan((byte) 10, true, (a, b) -> (byte) (a + b)).skip(1).toList());
    }

    // Test 60: mergeWith
    @Test
    public void testMergeWith() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        ByteStream b = ByteStream.of((byte) 2, (byte) 4, (byte) 6);

        assertEquals(6,
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(5,
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 },
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(6,
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(5,
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 },
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 1, (byte) 3, (byte) 5)
                        .map(e -> e)
                        .mergeWith(ByteStream.of((byte) 2, (byte) 4, (byte) 6), (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    // Test 61-64: zipWith methods
    @Test
    public void testZipWith() {
        assertEquals(3, ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).skip(1).count());
        assertArrayEquals(new byte[] { 5, 7, 9 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).toArray());
        assertArrayEquals(new byte[] { 7, 9 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 9),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).toList());
        assertEquals(N.asList((byte) 7, (byte) 9),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).skip(1).toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).map(e -> e).zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y)).count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 5, 7, 9 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y))
                        .toArray());
        assertArrayEquals(new byte[] { 7, 9 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 9),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y))
                        .toList());
        assertEquals(N.asList((byte) 7, (byte) 9),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipWithThreeStreams() {
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 18 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 18 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 12, (byte) 15, (byte) 18),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.asList((byte) 15, (byte) 18),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 18 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 18 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 12, (byte) 15, (byte) 18),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.asList((byte) 15, (byte) 18),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5, (byte) 6), ByteStream.of((byte) 7, (byte) 8, (byte) 9), (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipWithDefault() {
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y)).count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 5, 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toArray());
        assertArrayEquals(new byte[] { 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3).zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y)).toList());
        assertEquals(N.asList((byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toList());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .count());
        assertEquals(2,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 5, 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toArray());
        assertArrayEquals(new byte[] { 7, 13 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .toList());
        assertEquals(N.asList((byte) 7, (byte) 13),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), (byte) 0, (byte) 10, (x, y) -> (byte) (x + y))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testZipWithThreeStreamsDefault() {
        assertEquals(4,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 12, (byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.asList((byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
        assertEquals(4,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .count());
        assertEquals(3,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 12, 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toArray());
        assertArrayEquals(new byte[] { 15, 22, 20 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toArray());
        assertEquals(N.asList((byte) 12, (byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .toList());
        assertEquals(N.asList((byte) 15, (byte) 22, (byte) 20),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3)
                        .map(e -> e)
                        .zipWith(ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9, (byte) 10), (byte) 0, (byte) 10, (byte) 20,
                                (x, y, z) -> (byte) (x + y + z))
                        .skip(1)
                        .toList());
    }

    // Test 67: empty()
    @Test
    public void testEmpty() {
        assertEquals(0, ByteStream.empty().count());
        assertEquals(0, ByteStream.empty().skip(1).count());
        assertArrayEquals(new byte[0], ByteStream.empty().toArray());
        assertArrayEquals(new byte[0], ByteStream.empty().skip(1).toArray());
        assertEquals(Collections.emptyList(), ByteStream.empty().toList());
        assertEquals(Collections.emptyList(), ByteStream.empty().skip(1).toList());
        assertEquals(0, ByteStream.empty().map(e -> e).count());
        assertEquals(0, ByteStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new byte[0], ByteStream.empty().map(e -> e).toArray());
        assertArrayEquals(new byte[0], ByteStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(Collections.emptyList(), ByteStream.empty().map(e -> e).toList());
        assertEquals(Collections.emptyList(), ByteStream.empty().map(e -> e).skip(1).toList());
    }

    // Test 68: defer(Supplier<ByteStream> supplier)
    @Test
    public void testDefer() {
        assertEquals(5, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).count());
        assertEquals(4, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).skip(1).toList());
        assertEquals(5, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).count());
        assertEquals(4, ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 },
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 },
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.defer(() -> ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)).map(e -> e).skip(1).toList());
    }

    // Test 69: ofNullable(Byte e)
    @Test
    public void testOfNullable() {
        assertEquals(1, ByteStream.ofNullable((byte) 5).count());
        assertEquals(0, ByteStream.ofNullable((byte) 5).skip(1).count());
        assertArrayEquals(new byte[] { 5 }, ByteStream.ofNullable((byte) 5).toArray());
        assertArrayEquals(new byte[0], ByteStream.ofNullable((byte) 5).skip(1).toArray());
        assertEquals(N.asList((byte) 5), ByteStream.ofNullable((byte) 5).toList());
        assertEquals(Collections.emptyList(), ByteStream.ofNullable((byte) 5).skip(1).toList());
        assertEquals(1, ByteStream.ofNullable((byte) 5).map(e -> e).count());
        assertEquals(0, ByteStream.ofNullable((byte) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 5 }, ByteStream.ofNullable((byte) 5).map(e -> e).toArray());
        assertArrayEquals(new byte[0], ByteStream.ofNullable((byte) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 5), ByteStream.ofNullable((byte) 5).map(e -> e).toList());
        assertEquals(Collections.emptyList(), ByteStream.ofNullable((byte) 5).map(e -> e).skip(1).toList());

        // Test with null
        assertEquals(0, ByteStream.ofNullable((Byte) null).count());
        assertEquals(0, ByteStream.ofNullable((Byte) null).skip(1).count());
        assertArrayEquals(new byte[0], ByteStream.ofNullable((Byte) null).toArray());
        assertArrayEquals(new byte[0], ByteStream.ofNullable((Byte) null).skip(1).toArray());
        assertEquals(Collections.emptyList(), ByteStream.ofNullable((Byte) null).toList());
        assertEquals(Collections.emptyList(), ByteStream.ofNullable((Byte) null).skip(1).toList());
    }

    // Test 70: of(byte... a)
    @Test
    public void testOfArray() {
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).skip(1).toList());
        assertEquals(5, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).count());
        assertEquals(4, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).skip(1).toList());
    }

    // Test 71: of(byte[] a, int startIndex, int endIndex)
    @Test
    public void testOfArrayWithIndices() {
        byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    // Test 72: of(Byte[] a)
    @Test
    public void testOfByteObjectArray() {
        Byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(5, ByteStream.of(array).count());
        assertEquals(4, ByteStream.of(array).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).skip(1).toList());
        assertEquals(5, ByteStream.of(array).map(e -> e).count());
        assertEquals(4, ByteStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(array).map(e -> e).skip(1).toList());
    }

    // Test 73: of(Byte[] a, int startIndex, int endIndex)
    @Test
    public void testOfByteObjectArrayWithIndices() {
        Byte[] array = { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, ByteStream.of(array, 2, 5).count());
        assertEquals(2, ByteStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, ByteStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, ByteStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 3, 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 5 }, ByteStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 3, (byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.asList((byte) 4, (byte) 5), ByteStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    // Test 74: of(Collection<Byte> c)
    @Test
    public void testOfCollection() {
        List<Byte> collection = Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(5, ByteStream.of(collection).count());
        assertEquals(4, ByteStream.of(collection).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(collection).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(collection).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(collection).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(collection).skip(1).toList());
        assertEquals(5, ByteStream.of(collection).map(e -> e).count());
        assertEquals(4, ByteStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(collection).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(collection).map(e -> e).skip(1).toList());
    }

    // Test 75: of(ByteIterator iterator)
    @Test
    public void testOfIterator() {
        ByteIterator iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).skip(1).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(iter).map(e -> e).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(iter).map(e -> e).skip(1).count());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(iter).map(e -> e).skip(1).toArray());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).toList());

        iter = ByteIterator.of(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(iter).map(e -> e).skip(1).toList());
    }

    // Test 76: of(ByteBuffer buf)
    @Test
    public void testOfByteBuffer() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(buf).count());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(buf).skip(1).count());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(buf).toArray());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(buf).skip(1).toArray());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(buf).toList());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(buf).skip(1).toList());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(5, ByteStream.of(buf).map(e -> e).count());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(4, ByteStream.of(buf).map(e -> e).skip(1).count());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.of(buf).map(e -> e).toArray());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.of(buf).map(e -> e).skip(1).toArray());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(buf).map(e -> e).toList());

        buf = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.of(buf).map(e -> e).skip(1).toList());
    }

    // Test 77-79: of(File file), of(InputStream is) - Skip these as they involve IO operations

    // Test 80: flatten(byte[][] a)
    @Test
    public void testFlatten2D() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(6, ByteStream.flatten(array).count());
        assertEquals(5, ByteStream.flatten(array).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, ByteStream.flatten(array).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, ByteStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).skip(1).toList());
        assertEquals(6, ByteStream.flatten(array).map(e -> e).count());
        assertEquals(5, ByteStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, ByteStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, ByteStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).map(e -> e).skip(1).toList());
    }

    // Test 81: flatten(byte[][] a, boolean vertically)
    @Test
    public void testFlattenVertically() {
        byte[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals(6, ByteStream.flatten(array, true).count());
        assertEquals(5, ByteStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).toList());
        assertEquals(N.asList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).skip(1).toList());
        assertEquals(6, ByteStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, ByteStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 2, 5, 3, 6 }, ByteStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.asList((byte) 4, (byte) 2, (byte) 5, (byte) 3, (byte) 6), ByteStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    // Test 82: flatten(byte[][] a, byte valueForAlignment, boolean vertically)
    @Test
    public void testFlattenWithAlignment() {
        byte[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).toList());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).skip(1).toList());
        assertEquals(9, ByteStream.flatten(array, (byte) 0, true).map(e -> e).count());
        assertEquals(8, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 3, 6, 2, 4, 0, 0, 5, 0 }, ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).toList());
        assertEquals(N.asList((byte) 3, (byte) 6, (byte) 2, (byte) 4, (byte) 0, (byte) 0, (byte) 5, (byte) 0),
                ByteStream.flatten(array, (byte) 0, true).map(e -> e).skip(1).toList());
    }

    // Test 83: flatten(byte[][][] a)
    @Test
    public void testFlatten3D() {
        byte[][][] array = { { { 1, 2 }, { 3 } }, { { 4, 5, 6 } } };
        assertEquals(6, ByteStream.flatten(array).count());
        assertEquals(5, ByteStream.flatten(array).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, ByteStream.flatten(array).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, ByteStream.flatten(array).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).skip(1).toList());
        assertEquals(6, ByteStream.flatten(array).map(e -> e).count());
        assertEquals(5, ByteStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 }, ByteStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 }, ByteStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6), ByteStream.flatten(array).map(e -> e).skip(1).toList());
    }

    // Test 84: range(byte startInclusive, byte endExclusive)
    @Test
    public void testRange() {
        assertEquals(5, ByteStream.range((byte) 1, (byte) 6).count());
        assertEquals(4, ByteStream.range((byte) 1, (byte) 6).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.range((byte) 1, (byte) 6).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.range((byte) 1, (byte) 6).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.range((byte) 1, (byte) 6).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.range((byte) 1, (byte) 6).skip(1).toList());
        assertEquals(5, ByteStream.range((byte) 1, (byte) 6).map(e -> e).count());
        assertEquals(4, ByteStream.range((byte) 1, (byte) 6).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.range((byte) 1, (byte) 6).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.range((byte) 1, (byte) 6).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.range((byte) 1, (byte) 6).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.range((byte) 1, (byte) 6).map(e -> e).skip(1).toList());
    }

    // Test 85: range(byte startInclusive, byte endExclusive, byte by)
    @Test
    public void testRangeWithStep() {
        assertEquals(3, ByteStream.range((byte) 1, (byte) 10, (byte) 3).count());
        assertEquals(2, ByteStream.range((byte) 1, (byte) 10, (byte) 3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 7 }, ByteStream.range((byte) 1, (byte) 10, (byte) 3).toArray());
        assertArrayEquals(new byte[] { 4, 7 }, ByteStream.range((byte) 1, (byte) 10, (byte) 3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 7), ByteStream.range((byte) 1, (byte) 10, (byte) 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 7), ByteStream.range((byte) 1, (byte) 10, (byte) 3).skip(1).toList());
        assertEquals(3, ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).count());
        assertEquals(2, ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 7 }, ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 7 }, ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 7), ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).toList());
        assertEquals(N.asList((byte) 4, (byte) 7), ByteStream.range((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).toList());
    }

    // Test 86: rangeClosed(byte startInclusive, byte endInclusive)
    @Test
    public void testRangeClosed() {
        assertEquals(5, ByteStream.rangeClosed((byte) 1, (byte) 5).count());
        assertEquals(4, ByteStream.rangeClosed((byte) 1, (byte) 5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.rangeClosed((byte) 1, (byte) 5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.rangeClosed((byte) 1, (byte) 5).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.rangeClosed((byte) 1, (byte) 5).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.rangeClosed((byte) 1, (byte) 5).skip(1).toList());
        assertEquals(5, ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).count());
        assertEquals(4, ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.rangeClosed((byte) 1, (byte) 5).map(e -> e).skip(1).toList());
    }

    // Test 87: rangeClosed(byte startInclusive, byte endInclusive, byte by)
    @Test
    public void testRangeClosedWithStep() {
        assertEquals(4, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).count());
        assertEquals(3, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 7, 10 }, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).toArray());
        assertArrayEquals(new byte[] { 4, 7, 10 }, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 7, (byte) 10), ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).toList());
        assertEquals(N.asList((byte) 4, (byte) 7, (byte) 10), ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).skip(1).toList());
        assertEquals(4, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).count());
        assertEquals(3, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 4, 7, 10 }, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 4, 7, 10 }, ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 4, (byte) 7, (byte) 10), ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).toList());
        assertEquals(N.asList((byte) 4, (byte) 7, (byte) 10), ByteStream.rangeClosed((byte) 1, (byte) 10, (byte) 3).map(e -> e).skip(1).toList());
    }

    // Test 88: repeat(byte element, long n)
    @Test
    public void testRepeat() {
        assertEquals(5, ByteStream.repeat((byte) 7, 5).count());
        assertEquals(4, ByteStream.repeat((byte) 7, 5).skip(1).count());
        assertArrayEquals(new byte[] { 7, 7, 7, 7, 7 }, ByteStream.repeat((byte) 7, 5).toArray());
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, ByteStream.repeat((byte) 7, 5).skip(1).toArray());
        assertEquals(N.asList((byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7), ByteStream.repeat((byte) 7, 5).toList());
        assertEquals(N.asList((byte) 7, (byte) 7, (byte) 7, (byte) 7), ByteStream.repeat((byte) 7, 5).skip(1).toList());
        assertEquals(5, ByteStream.repeat((byte) 7, 5).map(e -> e).count());
        assertEquals(4, ByteStream.repeat((byte) 7, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 7, 7, 7, 7, 7 }, ByteStream.repeat((byte) 7, 5).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, ByteStream.repeat((byte) 7, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 7, (byte) 7, (byte) 7, (byte) 7, (byte) 7), ByteStream.repeat((byte) 7, 5).map(e -> e).toList());
        assertEquals(N.asList((byte) 7, (byte) 7, (byte) 7, (byte) 7), ByteStream.repeat((byte) 7, 5).map(e -> e).skip(1).toList());
    }

    // Test 89: random()
    @Test
    public void testRandom() {
        assertEquals(10, ByteStream.random().limit(10).count());
        assertEquals(9, ByteStream.random().limit(10).skip(1).count());
        byte[] arr = ByteStream.random().limit(10).toArray();
        assertEquals(10, arr.length);
        byte[] arr2 = ByteStream.random().limit(10).skip(1).toArray();
        assertEquals(9, arr2.length);
        List<Byte> list = ByteStream.random().limit(10).toList();
        assertEquals(10, list.size());
        List<Byte> list2 = ByteStream.random().limit(10).skip(1).toList();
        assertEquals(9, list2.size());
        assertEquals(10, ByteStream.random().limit(10).map(e -> e).count());
        assertEquals(9, ByteStream.random().limit(10).map(e -> e).skip(1).count());
        byte[] arr3 = ByteStream.random().limit(10).map(e -> e).toArray();
        assertEquals(10, arr3.length);
        byte[] arr4 = ByteStream.random().limit(10).map(e -> e).skip(1).toArray();
        assertEquals(9, arr4.length);
        List<Byte> list3 = ByteStream.random().limit(10).map(e -> e).toList();
        assertEquals(10, list3.size());
        List<Byte> list4 = ByteStream.random().limit(10).map(e -> e).skip(1).toList();
        assertEquals(9, list4.size());
    }

    // Test 90-94: iterate and generate methods
    @Test
    public void testIterate() {
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).limit(5).skip(1).toList());
        assertEquals(5, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).count());
        assertEquals(4, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5),
                ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.iterate((byte) 1, n -> (byte) (n + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testGenerate() {
        ByteSupplier supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };

        assertEquals(5, ByteStream.generate(supplier).limit(5).count());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(4, ByteStream.generate(supplier).limit(5).skip(1).count());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.generate(supplier).limit(5).toArray());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.generate(supplier).limit(5).skip(1).toArray());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.generate(supplier).limit(5).toList());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.generate(supplier).limit(5).skip(1).toList());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(5, ByteStream.generate(supplier).map(e -> e).limit(5).count());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(4, ByteStream.generate(supplier).map(e -> e).limit(5).skip(1).count());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.generate(supplier).map(e -> e).limit(5).toArray());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.generate(supplier).map(e -> e).limit(5).skip(1).toArray());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.generate(supplier).map(e -> e).limit(5).toList());

        supplier = new ByteSupplier() {
            private byte value = 0;

            @Override
            public byte getAsByte() {
                return ++value;
            }
        };
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.generate(supplier).map(e -> e).limit(5).skip(1).toList());
    }

    // Test 95-100: concat methods
    @Test
    public void testConcatArrays() {
        byte[] a1 = { 1, 2 };
        byte[] a2 = { 3, 4 };
        byte[] a3 = { 5 };

        assertEquals(5, ByteStream.concat(a1, a2, a3).count());
        assertEquals(4, ByteStream.concat(a1, a2, a3).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(a1, a2, a3).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.concat(a1, a2, a3).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(a1, a2, a3).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(a1, a2, a3).skip(1).toList());
        assertEquals(5, ByteStream.concat(a1, a2, a3).map(e -> e).count());
        assertEquals(4, ByteStream.concat(a1, a2, a3).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(a1, a2, a3).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.concat(a1, a2, a3).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(a1, a2, a3).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(a1, a2, a3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testConcatStreams() {
        ByteStream s1 = ByteStream.of((byte) 1, (byte) 2);
        ByteStream s2 = ByteStream.of((byte) 3, (byte) 4);
        ByteStream s3 = ByteStream.of((byte) 5);

        assertEquals(5, ByteStream.concat(s1, s2, s3).count());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(4, ByteStream.concat(s1, s2, s3).skip(1).count());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(s1, s2, s3).toArray());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.concat(s1, s2, s3).skip(1).toArray());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(s1, s2, s3).toList());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(s1, s2, s3).skip(1).toList());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(5, ByteStream.concat(s1, s2, s3).map(e -> e).count());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(4, ByteStream.concat(s1, s2, s3).map(e -> e).skip(1).count());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, ByteStream.concat(s1, s2, s3).map(e -> e).toArray());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertArrayEquals(new byte[] { 2, 3, 4, 5 }, ByteStream.concat(s1, s2, s3).map(e -> e).skip(1).toArray());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(s1, s2, s3).map(e -> e).toList());

        s1 = ByteStream.of((byte) 1, (byte) 2);
        s2 = ByteStream.of((byte) 3, (byte) 4);
        s3 = ByteStream.of((byte) 5);
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5), ByteStream.concat(s1, s2, s3).map(e -> e).skip(1).toList());
    }

    // Test 101-114: zip methods
    @Test
    public void testZipArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };

        assertEquals(3, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).count());
        assertEquals(2, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).skip(1).count());
        assertArrayEquals(new byte[] { 5, 7, 9 }, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toArray());
        assertArrayEquals(new byte[] { 7, 9 }, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 9), ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).toList());
        assertEquals(N.asList((byte) 7, (byte) 9), ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).skip(1).toList());
        assertEquals(3, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).count());
        assertEquals(2, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 5, 7, 9 }, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 7, 9 }, ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 5, (byte) 7, (byte) 9), ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).toList());
        assertEquals(N.asList((byte) 7, (byte) 9), ByteStream.zip(a, b, (x, y) -> (byte) (x + y)).map(e -> e).skip(1).toList());
    }

    // Test 115-121: merge methods
    @Test
    public void testMergeArrays() {
        byte[] a = { 1, 3, 5 };
        byte[] b = { 2, 4, 6 };

        assertEquals(6, ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).count());
        assertEquals(5, ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 },
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).skip(1).toList());
        assertEquals(6, ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).count());
        assertEquals(5, ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6 },
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toArray());
        assertArrayEquals(new byte[] { 2, 3, 4, 5, 6 },
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toArray());
        assertEquals(N.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).toList());
        assertEquals(N.asList((byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6),
                ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).map(e -> e).skip(1).toList());
    }
}
