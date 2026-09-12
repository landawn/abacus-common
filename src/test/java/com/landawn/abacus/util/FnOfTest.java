package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn.Disposables;
import com.landawn.abacus.util.Fn.Entries;
import com.landawn.abacus.util.Fn.FB;
import com.landawn.abacus.util.Fn.FC;
import com.landawn.abacus.util.Fn.FD;
import com.landawn.abacus.util.Fn.FF;
import com.landawn.abacus.util.Fn.FI;
import com.landawn.abacus.util.Fn.FL;
import com.landawn.abacus.util.Fn.FS;
import com.landawn.abacus.util.Fn.Pairs;
import com.landawn.abacus.util.Fn.Triples;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.CharPredicate;

public class FnOfTest extends FnTestSupport {

    @Test
    public void testFC() {
        assertTrue(FC.isZero().test((char) 0));
        assertFalse(FC.isZero().test('a'));
        assertTrue(FC.isWhitespace().test(' '));
        assertTrue(FC.isWhitespace().test('\t'));
        assertFalse(FC.isWhitespace().test('a'));
        assertTrue(FC.equal().test('a', 'a'));
        assertTrue(FC.notEqual().test('a', 'b'));
        assertTrue(FC.greaterThan().test('b', 'a'));
        assertTrue(FC.greaterThanOrEqual().test('a', 'a'));
        assertTrue(FC.lessThan().test('a', 'b'));
        assertTrue(FC.lessThanOrEqual().test('a', 'a'));
        assertEquals('a', FC.unbox().applyAsChar('a'));
        assertEquals(3, FC.len().apply(new char[] { 'a', 'b', 'c' }));
        assertEquals(0, FC.len().apply(null));
        final CharPredicate letters = FC.p(Character::isLetter);
        assertTrue(letters.test('a'));
        assertSame(letters, FC.p(letters));
        assertEquals("a", FC.f(c -> String.valueOf(c)).apply('a'));
        final AtomicInteger n = new AtomicInteger();
        FC.c(c -> n.incrementAndGet()).accept('x');
        assertEquals(1, n.get());
        assertEquals(MergeResult.TAKE_FIRST, FC.alternate().apply('a', 'b'));
        assertEquals('a', FC.CharBinaryOperators.MIN.applyAsChar('a', 'b'));
        assertEquals('b', FC.CharBinaryOperators.MAX.applyAsChar('a', 'b'));
        assertThrows(IllegalArgumentException.class, () -> FC.p(null));
    }

    @Test
    public void testFB() {
        assertTrue(FB.positive().test((byte) 5));
        assertFalse(FB.positive().test((byte) 0));
        assertTrue(FB.notNegative().test((byte) 0));
        assertFalse(FB.notNegative().test((byte) -1));
        assertTrue(FB.equal().test((byte) 1, (byte) 1));
        assertTrue(FB.notEqual().test((byte) 1, (byte) 2));
        assertTrue(FB.greaterThan().test((byte) 2, (byte) 1));
        assertTrue(FB.lessThan().test((byte) 1, (byte) 2));
        assertEquals(1, FB.unbox().applyAsByte((byte) 1));
        assertEquals(2, FB.len().apply(new byte[] { 1, 2 }));
        assertEquals((byte) 1, FB.ByteBinaryOperators.MIN.applyAsByte((byte) 1, (byte) 2));
        assertEquals((byte) 2, FB.ByteBinaryOperators.MAX.applyAsByte((byte) 1, (byte) 2));
        assertEquals(3, FB.sum().apply(new byte[] { 1, 2 }));
        assertEquals(1.5, FB.average().apply(new byte[] { 1, 2 }));
    }

    @Test
    public void testFS() {
        assertTrue(FS.positive().test((short) 1));
        assertTrue(FS.notNegative().test((short) 0));
        assertTrue(FS.equal().test((short) 1, (short) 1));
        assertTrue(FS.greaterThan().test((short) 2, (short) 1));
        assertEquals(2, FS.len().apply(new short[] { 1, 2 }));
        assertEquals((short) 1, FS.ShortBinaryOperators.MIN.applyAsShort((short) 1, (short) 2));
        assertEquals((short) 2, FS.ShortBinaryOperators.MAX.applyAsShort((short) 1, (short) 2));
    }

    @Test
    public void testFI() {
        assertTrue(FI.positive().test(1));
        assertTrue(FI.notNegative().test(0));
        assertTrue(FI.equal().test(1, 1));
        assertTrue(FI.greaterThan().test(2, 1));
        assertEquals(2, FI.len().apply(new int[] { 1, 2 }));
        assertEquals(1, FI.IntBinaryOperators.MIN.applyAsInt(1, 2));
        assertEquals(2, FI.IntBinaryOperators.MAX.applyAsInt(1, 2));
        assertEquals(3, FI.sum().apply(new int[] { 1, 2 }));
        assertEquals(1.5, FI.average().apply(new int[] { 1, 2 }));
    }

    @Test
    public void testFL() {
        assertTrue(FL.positive().test(1L));
        assertTrue(FL.notNegative().test(0L));
        assertTrue(FL.equal().test(1L, 1L));
        assertEquals(2, FL.len().apply(new long[] { 1L, 2L }));
        assertEquals(1L, FL.LongBinaryOperators.MIN.applyAsLong(1L, 2L));
        assertEquals(2L, FL.LongBinaryOperators.MAX.applyAsLong(1L, 2L));
    }

    @Test
    public void testFF() {
        assertTrue(FF.positive().test(1f));
        assertTrue(FF.notNegative().test(0f));
        assertTrue(FF.equal().test(1f, 1f));
        assertEquals(2, FF.len().apply(new float[] { 1f, 2f }));
        assertEquals(1f, FF.FloatBinaryOperators.MIN.applyAsFloat(1f, 2f));
        assertEquals(2f, FF.FloatBinaryOperators.MAX.applyAsFloat(1f, 2f));
    }

    @Test
    public void testFD() {
        assertTrue(FD.positive().test(1d));
        assertTrue(FD.notNegative().test(0d));
        assertTrue(FD.equal().test(1d, 1d));
        assertEquals(2, FD.len().apply(new double[] { 1d, 2d }));
        assertEquals(1d, FD.DoubleBinaryOperators.MIN.applyAsDouble(1d, 2d));
        assertEquals(2d, FD.DoubleBinaryOperators.MAX.applyAsDouble(1d, 2d));
        assertEquals(3d, FD.sum().apply(new double[] { 1d, 2d }));
        assertEquals(1.5, FD.average().apply(new double[] { 1d, 2d }));
    }

    @Test
    public void testEntries() {
        assertEquals("a1", Entries.f((String k, Integer v) -> k + v).apply(new AbstractMap.SimpleEntry<>("a", 1)));
        assertTrue(Entries.p((String k, Integer v) -> v > 0).test(new AbstractMap.SimpleEntry<>("a", 1)));
        final AtomicInteger n = new AtomicInteger();
        Entries.c((String k, Integer v) -> n.set(v)).accept(new AbstractMap.SimpleEntry<>("a", 7));
        assertEquals(7, n.get());
        assertThrows(IllegalArgumentException.class, () -> Entries.f(null));
        assertThrows(IllegalArgumentException.class, () -> Entries.p(null));
        assertThrows(IllegalArgumentException.class, () -> Entries.c(null));
    }

    @Test
    public void testPairsAndTriples() {
        assertEquals(List.of(1, 2), Pairs.<Integer> toList().apply(Pair.of(1, 2)));
        assertEquals(Set.of(1, 2), Pairs.<Integer> toSet().apply(Pair.of(1, 2)));
        assertEquals(List.of(1, 2, 3), Triples.<Integer> toList().apply(Triple.of(1, 2, 3)));
        assertEquals(Set.of(1, 2, 3), Triples.<Integer> toSet().apply(Triple.of(1, 2, 3)));
    }

    @Test
    public void testDisposables() {
        final DisposableArray<String> arr = DisposableArray.wrap(new String[] { "a", "b" });
        assertArrayEquals(new String[] { "a", "b" }, Disposables.<String, DisposableArray<String>> cloneArray().apply(arr));
        assertEquals("[a, b]", Disposables.toStr().apply(arr));
        assertEquals("a,b", Disposables.join(",").apply(arr));
    }
}
