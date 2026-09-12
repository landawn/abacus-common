package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IfTest extends TestBase {

    @Test
    public void testIsAndNot() {
        assertTrue(If.is(true).b);
        assertFalse(If.is(false).b);
        assertSame(If.is(true), If.is(true));
        assertSame(If.is(false), If.is(false));
        assertTrue(If.is(true) != If.is(false));
        assertFalse(If.not(true).b);
        assertTrue(If.not(false).b);
        assertSame(If.not(true), If.is(false));
        assertSame(If.not(false), If.is(true));
        assertTrue(If.exists(0).b);
        assertTrue(If.exists(Integer.MAX_VALUE).b);
        assertFalse(If.exists(-1).b);
        assertFalse(If.exists(Integer.MIN_VALUE).b);
        assertTrue(If.exists(Arrays.asList("a", "b").indexOf("b")).b);
        assertFalse(If.exists(Arrays.asList("a", "b").indexOf("z")).b);
    }

    @Test
    public void testNullEmptyBlank() {
        assertTrue(If.isNull(null).b);
        assertFalse(If.isNull("").b);
        assertFalse(If.isNull(0).b);
        assertFalse(If.notNull(null).b);
        assertTrue(If.notNull("x").b);

        assertTrue(If.isEmpty((CharSequence) null).b);
        assertTrue(If.isEmpty("").b);
        assertFalse(If.isEmpty(" ").b);
        assertTrue(If.isEmpty(new StringBuilder()).b);
        assertFalse(If.isEmpty(new StringBuilder("x")).b);
        assertTrue(If.notEmpty("a").b);
        assertFalse(If.notEmpty("").b);

        assertTrue(If.isEmpty((boolean[]) null).b);
        assertTrue(If.isEmpty(new boolean[0]).b);
        assertFalse(If.isEmpty(new boolean[] { true }).b);
        assertTrue(If.isEmpty((char[]) null).b);
        assertFalse(If.isEmpty(new char[] { 'a' }).b);
        assertTrue(If.isEmpty((byte[]) null).b);
        assertFalse(If.isEmpty(new byte[] { 1 }).b);
        assertTrue(If.isEmpty((short[]) null).b);
        assertFalse(If.isEmpty(new short[] { 1 }).b);
        assertTrue(If.isEmpty((int[]) null).b);
        assertFalse(If.isEmpty(new int[] { 1 }).b);
        assertTrue(If.isEmpty((long[]) null).b);
        assertFalse(If.isEmpty(new long[] { 1 }).b);
        assertTrue(If.isEmpty((float[]) null).b);
        assertFalse(If.isEmpty(new float[] { 1 }).b);
        assertTrue(If.isEmpty((double[]) null).b);
        assertFalse(If.isEmpty(new double[] { 1 }).b);
        assertTrue(If.isEmpty((Object[]) null).b);
        assertFalse(If.isEmpty(new Object[] { null }).b);
        assertTrue(If.notEmpty(new int[] { 1 }).b);
        assertFalse(If.notEmpty(new int[0]).b);

        assertTrue(If.isEmpty((Collection<?>) null).b);
        assertTrue(If.isEmpty(new ArrayList<>()).b);
        assertFalse(If.isEmpty(Arrays.asList("a")).b);
        assertTrue(If.isEmpty((Map<?, ?>) null).b);
        assertFalse(If.isEmpty(Map.of("k", "v")).b);
        assertTrue(If.isEmpty((IntList) null).b);
        assertTrue(If.isEmpty(IntList.of()).b);
        assertFalse(If.isEmpty(IntList.of(1)).b);
        assertTrue(If.isEmpty(BooleanList.of()).b);
        assertFalse(If.isEmpty(BooleanList.of(true)).b);
        assertTrue(If.isEmpty(CharList.of()).b);
        assertFalse(If.isEmpty(CharList.of('a')).b);
        assertTrue(If.isEmpty(ByteList.of()).b);
        assertFalse(If.isEmpty(ByteList.of((byte) 1)).b);
        assertTrue(If.isEmpty(ShortList.of()).b);
        assertFalse(If.isEmpty(ShortList.of((short) 1)).b);
        assertTrue(If.isEmpty(LongList.of()).b);
        assertFalse(If.isEmpty(LongList.of(1L)).b);
        assertTrue(If.isEmpty(FloatList.of()).b);
        assertFalse(If.isEmpty(FloatList.of(1f)).b);
        assertTrue(If.isEmpty(DoubleList.of()).b);
        assertFalse(If.isEmpty(DoubleList.of(1d)).b);
        assertTrue(If.isEmpty((Multiset<?>) null).b);
        assertFalse(If.isEmpty(Multiset.of("a")).b);
        assertTrue(If.isEmpty((ListMultimap<?, ?>) null).b);
        assertFalse(If.isEmpty(CommonUtil.newListMultimap(Map.of("k", "v"))).b);
        assertTrue(If.isEmpty((SetMultimap<?, ?>) null).b);
        assertFalse(If.isEmpty(CommonUtil.newSetMultimap(Map.of("k", "v"))).b);
        assertTrue(If.notEmpty(Arrays.asList("a")).b);
        assertTrue(If.notEmpty(IntList.of(1)).b);
        assertTrue(If.notEmpty(Multiset.of("a")).b);
        assertTrue(If.notEmpty(CommonUtil.newListMultimap(Map.of("k", "v"))).b);

        assertTrue(If.isBlank(null).b);
        assertTrue(If.isBlank("").b);
        assertTrue(If.isBlank(" \t\n\r ").b);
        assertFalse(If.isBlank("a").b);
        assertFalse(If.notBlank(" ").b);
        assertTrue(If.notBlank(" a ").b);
        assertTrue(If.notBlank("\t a \n").b);
    }

    @Test
    public void testThenAndOrElse() throws Exception {
        AtomicInteger n = new AtomicInteger();
        If.is(true).then(() -> n.set(1)).orElse(() -> n.set(2));
        assertEquals(1, n.get());
        If.is(false).then(() -> n.set(1)).orElse(() -> n.set(2));
        assertEquals(2, n.get());
        If.is(true).then(5, n::set).orElse(9, n::set);
        assertEquals(5, n.get());
        If.is(false).then(5, n::set).orElse(9, n::set);
        assertEquals(9, n.get());

        AtomicReference<String> ref = new AtomicReference<>("init");
        If.is(true).then(null, ref::set);
        assertNull(ref.get());
        If.is(false).then(() -> {
        }).orElse(null, ref::set);
        assertNull(ref.get());

        assertSame(If.OrElse.TRUE, If.is(true).thenDoNothing());
        assertSame(If.OrElse.FALSE, If.is(false).thenDoNothing());
        AtomicBoolean elseRan = new AtomicBoolean();
        If.is(true).thenDoNothing().orElse(() -> elseRan.set(true));
        assertFalse(elseRan.get());
        If.is(false).thenDoNothing().orElse(() -> elseRan.set(true));
        assertTrue(elseRan.get());
        If.is(false).thenDoNothing().orElse("touched", ref::set);
        assertEquals("touched", ref.get());

        assertSame(If.OrElse.TRUE, If.is(true).then(() -> {
        }));
        assertSame(If.OrElse.FALSE, If.is(false).then(() -> {
        }));
        assertThrows(RuntimeException.class, () -> If.is(true).then(() -> {
            throw new RuntimeException("then");
        }));
        assertDoesNotThrow(() -> If.is(false).then(() -> {
            throw new RuntimeException("then");
        }));
        assertThrows(RuntimeException.class, () -> If.is(false).then(() -> {
        }).orElse(() -> {
            throw new RuntimeException("else");
        }));

        assertThrows(RuntimeException.class, () -> If.is(true).thenThrow(() -> new RuntimeException("Error")));
        assertDoesNotThrow(() -> If.is(false).thenThrow(() -> new RuntimeException("Error")));
        assertEquals("specific message",
                assertThrows(RuntimeException.class, () -> If.is(true).thenThrow(() -> new RuntimeException("specific message"))).getMessage());
        assertSame(If.OrElse.FALSE, If.is(false).thenThrow(() -> new RuntimeException("err")));
        assertThrows(Exception.class, () -> If.is(true).thenThrow(() -> new Exception("checked")));
        If.is(false).thenThrow(() -> new RuntimeException("not thrown")).orElse(() -> n.set(42));
        assertEquals(42, n.get());

        assertDoesNotThrow(() -> If.is(true).then(() -> {
        }).orElseThrow(() -> new RuntimeException("Error")));
        assertEquals("or else message", assertThrows(RuntimeException.class, () -> If.is(false).then(() -> {
        }).orElseThrow(() -> new RuntimeException("or else message"))).getMessage());
        assertThrows(Exception.class, () -> If.is(false).then(() -> {
        }).orElseThrow(() -> new Exception("checked")));
    }

    @Test
    public void testNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then((Throwables.Runnable<?>) null));
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then("value", null));
        assertThrows(IllegalArgumentException.class, () -> If.is(true).thenThrow(null));
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse((Throwables.Runnable<?>) null));
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse("value", null));
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElseThrow(null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then((Throwables.Runnable<?>) null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then("value", null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).thenThrow(null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElse((Throwables.Runnable<?>) null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElse("value", null));
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElseThrow(null));
    }

    @Test
    public void testChaining() {
        List<String> results = new ArrayList<>();
        If.notEmpty(Arrays.asList("a", "b", "c")).then(() -> results.add("list not empty")).orElse(() -> results.add("list empty"));
        If.isBlank("  ").then(() -> results.add("string is blank")).orElse(() -> results.add("string not blank"));
        If.exists(Arrays.asList("x", "y", "z").indexOf("y")).then(() -> results.add("element found")).orElse(() -> results.add("element not found"));
        assertEquals(Arrays.asList("list not empty", "string is blank", "element found"), results);
        assertSame(If.OrElse.TRUE, If.OrElse.TRUE);
        assertTrue(If.OrElse.TRUE != If.OrElse.FALSE);
    }
}
