package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IfTest extends TestBase {

    // ==================== is(boolean) ====================

    @Test
    public void testIs() {
        assertTrue(If.is(true).b);
        assertFalse(If.is(false).b);

        assertTrue(If.is(true) == If.is(true));
        assertTrue(If.is(false) == If.is(false));
    }

    @Test
    public void testIs_CachedInstances() {
        If trueIf1 = If.is(true);
        If trueIf2 = If.is(true);
        assertSame(trueIf1, trueIf2);

        If falseIf1 = If.is(false);
        If falseIf2 = If.is(false);
        assertSame(falseIf1, falseIf2);
    }

    @Test
    public void testIs_TrueNotSameAsFalse() {
        assertTrue(If.is(true) != If.is(false));
    }

    // ==================== not(boolean) ====================

    @Test
    public void testNot() {
        assertFalse(If.not(true).b);
        assertTrue(If.not(false).b);

        assertTrue(If.not(true) == If.is(false));
        assertTrue(If.not(false) == If.is(true));
    }

    @Test
    public void testNot_CachedInstances() {
        assertSame(If.not(true), If.is(false));
        assertSame(If.not(false), If.is(true));
    }

    // ==================== exists(int) ====================

    @Test
    public void testExists() {
        assertTrue(If.exists(0).b);
        assertTrue(If.exists(1).b);
        assertTrue(If.exists(100).b);
        assertFalse(If.exists(-1).b);
        assertFalse(If.exists(-100).b);
    }

    @Test
    public void testExists_BoundaryValues() {
        assertTrue(If.exists(0).b);
        assertTrue(If.exists(Integer.MAX_VALUE).b);
        assertFalse(If.exists(-1).b);
        assertFalse(If.exists(Integer.MIN_VALUE).b);
    }

    @Test
    public void testExists_WithIndexOf() {
        List<String> list = Arrays.asList("a", "b", "c");

        AtomicBoolean executed = new AtomicBoolean(false);
        If.exists(list.indexOf("b")).then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.exists(list.indexOf("d")).then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    // ==================== isNull(Object) ====================

    @Test
    public void testIsNull() {
        assertTrue(If.isNull(null).b);
        assertFalse(If.isNull("").b);
        assertFalse(If.isNull("abc").b);
        assertFalse(If.isNull(new Object()).b);
        assertFalse(If.isNull(0).b);
    }

    @Test
    public void testIsNull_VariousTypes() {
        assertTrue(If.isNull((String) null).b);
        assertTrue(If.isNull((Integer) null).b);
        assertTrue(If.isNull((List<?>) null).b);
        assertFalse(If.isNull(Boolean.FALSE).b);
        assertFalse(If.isNull(0L).b);
        assertFalse(If.isNull(new ArrayList<>()).b);
    }

    // ==================== isEmpty(CharSequence) ====================

    @Test
    public void testIsEmptyCharSequence() {
        assertTrue(If.isEmpty((CharSequence) null).b);
        assertTrue(If.isEmpty("").b);
        assertFalse(If.isEmpty(" ").b);
        assertFalse(If.isEmpty("abc").b);
    }

    @Test
    public void testIsEmptyCharSequence_StringBuilder() {
        assertTrue(If.isEmpty((CharSequence) new StringBuilder()).b);
        assertFalse(If.isEmpty((CharSequence) new StringBuilder("text")).b);
    }

    // ==================== isEmpty(boolean[]) ====================

    @Test
    public void testIsEmptyBooleanArray() {
        assertTrue(If.isEmpty((boolean[]) null).b);
        assertTrue(If.isEmpty(new boolean[0]).b);
        assertFalse(If.isEmpty(new boolean[] { true }).b);
    }

    @Test
    public void testIsEmptyBooleanArray_MultipleElements() {
        assertFalse(If.isEmpty(new boolean[] { true, false, true }).b);
    }

    // ==================== isEmpty(char[]) ====================

    @Test
    public void testIsEmptyCharArray() {
        assertTrue(If.isEmpty((char[]) null).b);
        assertTrue(If.isEmpty(new char[0]).b);
        assertFalse(If.isEmpty(new char[] { 'a' }).b);
    }

    // ==================== isEmpty(byte[]) ====================

    @Test
    public void testIsEmptyByteArray() {
        assertTrue(If.isEmpty((byte[]) null).b);
        assertTrue(If.isEmpty(new byte[0]).b);
        assertFalse(If.isEmpty(new byte[] { 1 }).b);
    }

    // ==================== isEmpty(short[]) ====================

    @Test
    public void testIsEmptyShortArray() {
        assertTrue(If.isEmpty((short[]) null).b);
        assertTrue(If.isEmpty(new short[0]).b);
        assertFalse(If.isEmpty(new short[] { 1 }).b);
    }

    // ==================== isEmpty(int[]) ====================

    @Test
    public void testIsEmptyIntArray() {
        assertTrue(If.isEmpty((int[]) null).b);
        assertTrue(If.isEmpty(new int[0]).b);
        assertFalse(If.isEmpty(new int[] { 1 }).b);
    }

    // ==================== isEmpty(long[]) ====================

    @Test
    public void testIsEmptyLongArray() {
        assertTrue(If.isEmpty((long[]) null).b);
        assertTrue(If.isEmpty(new long[0]).b);
        assertFalse(If.isEmpty(new long[] { 1 }).b);
    }

    // ==================== isEmpty(float[]) ====================

    @Test
    public void testIsEmptyFloatArray() {
        assertTrue(If.isEmpty((float[]) null).b);
        assertTrue(If.isEmpty(new float[0]).b);
        assertFalse(If.isEmpty(new float[] { 1.0f }).b);
    }

    // ==================== isEmpty(double[]) ====================

    @Test
    public void testIsEmptyDoubleArray() {
        assertTrue(If.isEmpty((double[]) null).b);
        assertTrue(If.isEmpty(new double[0]).b);
        assertFalse(If.isEmpty(new double[] { 1.0 }).b);
    }

    // ==================== isEmpty(Object[]) ====================

    @Test
    public void testIsEmptyObjectArray() {
        assertTrue(If.isEmpty((Object[]) null).b);
        assertTrue(If.isEmpty(new Object[0]).b);
        assertFalse(If.isEmpty(new Object[] { "a" }).b);
    }

    @Test
    public void testIsEmptyObjectArray_WithNullElement() {
        assertFalse(If.isEmpty(new Object[] { null }).b);
    }

    // ==================== isEmpty(Collection) ====================

    @Test
    public void testIsEmptyCollection() {
        assertTrue(If.isEmpty((Collection<?>) null).b);
        assertTrue(If.isEmpty(new ArrayList<>()).b);
        assertFalse(If.isEmpty(Arrays.asList("a")).b);
    }

    // ==================== isEmpty(Map) ====================

    @Test
    public void testIsEmptyMap() {
        assertTrue(If.isEmpty((Map<?, ?>) null).b);
        assertTrue(If.isEmpty(new HashMap<>()).b);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(If.isEmpty(map).b);
    }

    // ==================== isEmpty(PrimitiveList) ====================

    @Test
    public void testIsEmptyPrimitiveList() {
        assertTrue(If.isEmpty((IntList) null).b);
        assertTrue(If.isEmpty(IntList.of()).b);
        assertFalse(If.isEmpty(IntList.of(1, 2, 3)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_BooleanList() {
        assertTrue(If.isEmpty((BooleanList) null).b);
        assertTrue(If.isEmpty(BooleanList.of()).b);
        assertFalse(If.isEmpty(BooleanList.of(true)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_CharList() {
        assertTrue(If.isEmpty((CharList) null).b);
        assertTrue(If.isEmpty(CharList.of()).b);
        assertFalse(If.isEmpty(CharList.of('a')).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_ByteList() {
        assertTrue(If.isEmpty((ByteList) null).b);
        assertTrue(If.isEmpty(ByteList.of()).b);
        assertFalse(If.isEmpty(ByteList.of((byte) 1)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_ShortList() {
        assertTrue(If.isEmpty((ShortList) null).b);
        assertTrue(If.isEmpty(ShortList.of()).b);
        assertFalse(If.isEmpty(ShortList.of((short) 1)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_LongList() {
        assertTrue(If.isEmpty((LongList) null).b);
        assertTrue(If.isEmpty(LongList.of()).b);
        assertFalse(If.isEmpty(LongList.of(1L)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_FloatList() {
        assertTrue(If.isEmpty((FloatList) null).b);
        assertTrue(If.isEmpty(FloatList.of()).b);
        assertFalse(If.isEmpty(FloatList.of(1.0f)).b);
    }

    @Test
    public void testIsEmptyPrimitiveList_DoubleList() {
        assertTrue(If.isEmpty((DoubleList) null).b);
        assertTrue(If.isEmpty(DoubleList.of()).b);
        assertFalse(If.isEmpty(DoubleList.of(1.0)).b);
    }

    // ==================== isEmpty(Multiset) ====================

    @Test
    public void testIsEmptyMultiset() {
        assertTrue(If.isEmpty((Multiset<?>) null).b);
        assertTrue(If.isEmpty(Multiset.of()).b);
        assertFalse(If.isEmpty(Multiset.of("a", "b")).b);
    }

    // ==================== isEmpty(Multimap) ====================

    @Test
    public void testIsEmptyMultimap() {
        assertTrue(If.isEmpty((ListMultimap<?, ?>) null).b);
        assertTrue(If.isEmpty(new ListMultimap<>()).b);

        ListMultimap<String, String> mm = new ListMultimap<>();
        mm.put("key", "value");
        assertFalse(If.isEmpty(mm).b);
    }

    @Test
    public void testIsEmptyMultimap_SetMultimap() {
        assertTrue(If.isEmpty((SetMultimap<?, ?>) null).b);
        assertTrue(If.isEmpty(new SetMultimap<>()).b);

        SetMultimap<String, String> sm = new SetMultimap<>();
        sm.put("key", "value");
        assertFalse(If.isEmpty(sm).b);
    }

    // ==================== isBlank(CharSequence) ====================

    @Test
    public void testIsBlank() {
        assertTrue(If.isBlank(null).b);
        assertTrue(If.isBlank("").b);
        assertTrue(If.isBlank(" ").b);
        assertTrue(If.isBlank("  ").b);
        assertTrue(If.isBlank("\t").b);
        assertTrue(If.isBlank("\n").b);
        assertFalse(If.isBlank("a").b);
        assertFalse(If.isBlank(" a ").b);
    }

    @Test
    public void testIsBlank_MixedWhitespace() {
        assertTrue(If.isBlank(" \t\n\r ").b);
        assertFalse(If.isBlank("\t a \n").b);
    }

    // ==================== notNull(Object) ====================

    @Test
    public void testNotNull() {
        assertFalse(If.notNull(null).b);
        assertTrue(If.notNull("").b);
        assertTrue(If.notNull("abc").b);
        assertTrue(If.notNull(new Object()).b);
        assertTrue(If.notNull(0).b);
    }

    @Test
    public void testNotNull_VariousTypes() {
        assertTrue(If.notNull(new int[0]).b);
        assertTrue(If.notNull(new ArrayList<>()).b);
        assertTrue(If.notNull(new HashMap<>()).b);
        assertTrue(If.notNull(Boolean.TRUE).b);
    }

    // ==================== notEmpty(CharSequence) ====================

    @Test
    public void testNotEmptyCharSequence() {
        assertFalse(If.notEmpty((CharSequence) null).b);
        assertFalse(If.notEmpty("").b);
        assertTrue(If.notEmpty(" ").b);
        assertTrue(If.notEmpty("abc").b);
    }

    // ==================== notEmpty(boolean[]) ====================

    @Test
    public void testNotEmptyBooleanArray() {
        assertFalse(If.notEmpty((boolean[]) null).b);
        assertFalse(If.notEmpty(new boolean[0]).b);
        assertTrue(If.notEmpty(new boolean[] { true }).b);
    }

    // ==================== notEmpty(char[]) ====================

    @Test
    public void testNotEmptyCharArray() {
        assertFalse(If.notEmpty((char[]) null).b);
        assertFalse(If.notEmpty(new char[0]).b);
        assertTrue(If.notEmpty(new char[] { 'a' }).b);
    }

    // ==================== notEmpty(byte[]) ====================

    @Test
    public void testNotEmptyByteArray() {
        assertFalse(If.notEmpty((byte[]) null).b);
        assertFalse(If.notEmpty(new byte[0]).b);
        assertTrue(If.notEmpty(new byte[] { 1 }).b);
    }

    // ==================== notEmpty(short[]) ====================

    @Test
    public void testNotEmptyShortArray() {
        assertFalse(If.notEmpty((short[]) null).b);
        assertFalse(If.notEmpty(new short[0]).b);
        assertTrue(If.notEmpty(new short[] { 1 }).b);
    }

    // ==================== notEmpty(int[]) ====================

    @Test
    public void testNotEmptyIntArray() {
        assertFalse(If.notEmpty((int[]) null).b);
        assertFalse(If.notEmpty(new int[0]).b);
        assertTrue(If.notEmpty(new int[] { 1 }).b);
    }

    // ==================== notEmpty(long[]) ====================

    @Test
    public void testNotEmptyLongArray() {
        assertFalse(If.notEmpty((long[]) null).b);
        assertFalse(If.notEmpty(new long[0]).b);
        assertTrue(If.notEmpty(new long[] { 1 }).b);
    }

    // ==================== notEmpty(float[]) ====================

    @Test
    public void testNotEmptyFloatArray() {
        assertFalse(If.notEmpty((float[]) null).b);
        assertFalse(If.notEmpty(new float[0]).b);
        assertTrue(If.notEmpty(new float[] { 1.0f }).b);
    }

    // ==================== notEmpty(double[]) ====================

    @Test
    public void testNotEmptyDoubleArray() {
        assertFalse(If.notEmpty((double[]) null).b);
        assertFalse(If.notEmpty(new double[0]).b);
        assertTrue(If.notEmpty(new double[] { 1.0 }).b);
    }

    // ==================== notEmpty(Object[]) ====================

    @Test
    public void testNotEmptyObjectArray() {
        assertFalse(If.notEmpty((Object[]) null).b);
        assertFalse(If.notEmpty(new Object[0]).b);
        assertTrue(If.notEmpty(new Object[] { "a" }).b);
    }

    // ==================== notEmpty(Collection) ====================

    @Test
    public void testNotEmptyCollection() {
        assertFalse(If.notEmpty((Collection<?>) null).b);
        assertFalse(If.notEmpty(new ArrayList<>()).b);
        assertTrue(If.notEmpty(Arrays.asList("a")).b);
    }

    // ==================== notEmpty(Map) ====================

    @Test
    public void testNotEmptyMap() {
        assertFalse(If.notEmpty((Map<?, ?>) null).b);
        assertFalse(If.notEmpty(new HashMap<>()).b);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(If.notEmpty(map).b);
    }

    // ==================== notEmpty(PrimitiveList) ====================

    @Test
    public void testNotEmptyPrimitiveList() {
        assertFalse(If.notEmpty((IntList) null).b);
        assertFalse(If.notEmpty(IntList.of()).b);
        assertTrue(If.notEmpty(IntList.of(1, 2, 3)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_BooleanList() {
        assertFalse(If.notEmpty((BooleanList) null).b);
        assertFalse(If.notEmpty(BooleanList.of()).b);
        assertTrue(If.notEmpty(BooleanList.of(true)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_CharList() {
        assertFalse(If.notEmpty((CharList) null).b);
        assertFalse(If.notEmpty(CharList.of()).b);
        assertTrue(If.notEmpty(CharList.of('a')).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_ByteList() {
        assertFalse(If.notEmpty((ByteList) null).b);
        assertFalse(If.notEmpty(ByteList.of()).b);
        assertTrue(If.notEmpty(ByteList.of((byte) 1)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_ShortList() {
        assertFalse(If.notEmpty((ShortList) null).b);
        assertFalse(If.notEmpty(ShortList.of()).b);
        assertTrue(If.notEmpty(ShortList.of((short) 1)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_LongList() {
        assertFalse(If.notEmpty((LongList) null).b);
        assertFalse(If.notEmpty(LongList.of()).b);
        assertTrue(If.notEmpty(LongList.of(1L)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_FloatList() {
        assertFalse(If.notEmpty((FloatList) null).b);
        assertFalse(If.notEmpty(FloatList.of()).b);
        assertTrue(If.notEmpty(FloatList.of(1.0f)).b);
    }

    @Test
    public void testNotEmptyPrimitiveList_DoubleList() {
        assertFalse(If.notEmpty((DoubleList) null).b);
        assertFalse(If.notEmpty(DoubleList.of()).b);
        assertTrue(If.notEmpty(DoubleList.of(1.0)).b);
    }

    // ==================== notEmpty(Multiset) ====================

    @Test
    public void testNotEmptyMultiset() {
        assertFalse(If.notEmpty((Multiset<?>) null).b);
        assertFalse(If.notEmpty(Multiset.of()).b);
        assertTrue(If.notEmpty(Multiset.of("a", "b")).b);
    }

    // ==================== notEmpty(Multimap) ====================

    @Test
    public void testNotEmptyMultimap() {
        assertFalse(If.notEmpty((ListMultimap<?, ?>) null).b);
        assertFalse(If.notEmpty(new ListMultimap<>()).b);

        ListMultimap<String, String> mm = new ListMultimap<>();
        mm.put("key", "value");
        assertTrue(If.notEmpty(mm).b);
    }

    @Test
    public void testNotEmptyMultimap_SetMultimap() {
        assertFalse(If.notEmpty((SetMultimap<?, ?>) null).b);
        assertFalse(If.notEmpty(new SetMultimap<>()).b);

        SetMultimap<String, String> sm = new SetMultimap<>();
        sm.put("key", "value");
        assertTrue(If.notEmpty(sm).b);
    }

    // ==================== notBlank(CharSequence) ====================

    @Test
    public void testNotBlank() {
        assertFalse(If.notBlank(null).b);
        assertFalse(If.notBlank("").b);
        assertFalse(If.notBlank(" ").b);
        assertFalse(If.notBlank("  ").b);
        assertFalse(If.notBlank("\t").b);
        assertFalse(If.notBlank("\n").b);
        assertTrue(If.notBlank("a").b);
        assertTrue(If.notBlank(" a ").b);
    }

    @Test
    public void testNotBlank_MixedWhitespace() {
        assertFalse(If.notBlank(" \t\n\r ").b);
        assertTrue(If.notBlank("\t a \n").b);
    }

    // ==================== thenDoNothing() ====================

    @Test
    public void testThenDoNothing() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.is(true).thenDoNothing().orElse(() -> executed.set(true));
        assertFalse(executed.get());

        If.is(false).thenDoNothing().orElse(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void testThenDoNothing_ReturnType() {
        If.OrElse result = If.is(true).thenDoNothing();
        assertNotNull(result);
        assertSame(If.OrElse.TRUE, result);

        If.OrElse result2 = If.is(false).thenDoNothing();
        assertSame(If.OrElse.FALSE, result2);
    }

    @Test
    public void testThenDoNothing_WithOrElseThrow() {
        assertDoesNotThrow(() -> If.is(true).thenDoNothing().orElseThrow(() -> new RuntimeException("should not throw")));

        assertThrows(RuntimeException.class, () -> If.is(false).thenDoNothing().orElseThrow(() -> new RuntimeException("should throw")));
    }

    // ==================== then(Runnable) ====================

    @Test
    public void testThenRunnable() {
        AtomicInteger counter = new AtomicInteger(0);

        If.is(true).then(() -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        If.is(false).then(() -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThenRunnable_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then((Throwables.Runnable<Exception>) null));
    }

    @Test
    public void testThenRunnable_ReturnType() {
        If.OrElse result = If.is(true).then(() -> {
        });
        assertSame(If.OrElse.TRUE, result);

        If.OrElse result2 = If.is(false).then(() -> {
        });
        assertSame(If.OrElse.FALSE, result2);
    }

    @Test
    public void testThenRunnable_ExceptionPropagation() {
        assertThrows(RuntimeException.class, () -> If.is(true).then(() -> {
            throw new RuntimeException("test error");
        }));

        // false condition: exception not thrown since action not executed
        assertDoesNotThrow(() -> If.is(false).then(() -> {
            throw new RuntimeException("test error");
        }));
    }

    // ==================== then(T, Consumer) ====================

    @Test
    public void testThenWithInput() {
        AtomicInteger result = new AtomicInteger(0);

        If.is(true).then(5, result::set);
        assertEquals(5, result.get());

        If.is(false).then(10, result::set);
        assertEquals(5, result.get());
    }

    @Test
    public void testThenWithInput_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then("value", null));
    }

    @Test
    public void testThenWithInput_NullValue() {
        AtomicReference<String> result = new AtomicReference<>("initial");
        If.is(true).then(null, result::set);
        assertTrue(result.get() == null);
    }

    @Test
    public void testThenWithInput_ReturnType() {
        If.OrElse orElse = If.is(true).then(42, v -> {
        });
        assertSame(If.OrElse.TRUE, orElse);

        If.OrElse orElse2 = If.is(false).then(42, v -> {
        });
        assertSame(If.OrElse.FALSE, orElse2);
    }

    @Test
    public void testThenWithInput_ComplexType() {
        List<String> items = new ArrayList<>();
        If.is(true).then(Arrays.asList("a", "b", "c"), items::addAll);
        assertEquals(3, items.size());
    }

    // ==================== thenThrow(Supplier) ====================

    @Test
    public void testThenThrow() {
        assertThrows(RuntimeException.class, () -> If.is(true).thenThrow(() -> new RuntimeException("Error")));

        assertDoesNotThrow(() -> If.is(false).thenThrow(() -> new RuntimeException("Error")));
    }

    @Test
    public void testThenThrow_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).thenThrow(null));
    }

    @Test
    public void testThenThrow_ExceptionMessage() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            If.is(true).thenThrow(() -> new RuntimeException("specific message"));
        });
        assertEquals("specific message", thrown.getMessage());
    }

    @Test
    public void testThenThrow_ReturnType() {
        // When condition is false, thenThrow does not throw and returns OrElse.FALSE
        If.OrElse result = If.is(false).thenThrow(() -> new RuntimeException("err"));
        assertSame(If.OrElse.FALSE, result);
    }

    @Test
    public void testThenThrow_CheckedException() {
        assertThrows(Exception.class, () -> If.is(true).thenThrow(() -> new Exception("checked")));
    }

    @Test
    public void testThenThrow_WithOrElse() {
        AtomicBoolean executed = new AtomicBoolean(false);
        If.is(false).thenThrow(() -> new RuntimeException("not thrown")).orElse(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void testThenThrow_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).thenThrow(() -> new IllegalArgumentException("bad arg")));
    }

    // ==================== OrElse.orElse(Runnable) ====================

    @Test
    public void testOrElseRunnable() {
        AtomicInteger counter = new AtomicInteger(0);

        If.is(true).then(() -> counter.set(1)).orElse(() -> counter.set(2));
        assertEquals(1, counter.get());

        If.is(false).then(() -> counter.set(1)).orElse(() -> counter.set(2));
        assertEquals(2, counter.get());
    }

    @Test
    public void testOrElseRunnable_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse((Throwables.Runnable<Exception>) null));
    }

    @Test
    public void testOrElseRunnable_NotExecutedWhenTrue() {
        AtomicBoolean orElseExecuted = new AtomicBoolean(false);
        If.is(true).then(() -> {
        }).orElse(() -> orElseExecuted.set(true));
        assertFalse(orElseExecuted.get());
    }

    @Test
    public void testOrElseRunnable_ExecutedWhenFalse() {
        AtomicBoolean orElseExecuted = new AtomicBoolean(false);
        If.is(false).then(() -> {
        }).orElse(() -> orElseExecuted.set(true));
        assertTrue(orElseExecuted.get());
    }

    @Test
    public void testOrElseRunnable_ExceptionPropagation() {
        assertThrows(RuntimeException.class, () -> If.is(false).then(() -> {
        }).orElse(() -> {
            throw new RuntimeException("orElse error");
        }));
    }

    // ==================== OrElse.orElse(T, Consumer) ====================

    @Test
    public void testOrElseWithInput() {
        AtomicInteger result = new AtomicInteger(0);

        If.is(true).then(() -> result.set(1)).orElse(5, result::set);
        assertEquals(1, result.get());

        If.is(false).then(() -> result.set(1)).orElse(5, result::set);
        assertEquals(5, result.get());
    }

    @Test
    public void testOrElseWithInput_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse("value", null));
    }

    @Test
    public void testOrElseWithInput_NullValue() {
        AtomicReference<String> result = new AtomicReference<>("initial");
        If.is(false).then(() -> {
        }).orElse(null, result::set);
        assertTrue(result.get() == null);
    }

    @Test
    public void testOrElseWithInput_NotExecutedWhenTrue() {
        AtomicInteger result = new AtomicInteger(0);
        If.is(true).then(() -> result.set(10)).orElse(20, result::set);
        assertEquals(10, result.get());
    }

    // ==================== OrElse.orElseThrow(Supplier) ====================

    @Test
    public void testOrElseThrow() {
        assertDoesNotThrow(() -> If.is(true).then(() -> {
        }).orElseThrow(() -> new RuntimeException("Error")));

        assertThrows(RuntimeException.class, () -> If.is(false).then(() -> {
        }).orElseThrow(() -> new RuntimeException("Error")));
    }

    @Test
    public void testOrElseThrow_Null() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElseThrow(null));
    }

    @Test
    public void testOrElseThrow_ExceptionMessage() {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            If.is(false).then(() -> {
            }).orElseThrow(() -> new RuntimeException("or else message"));
        });
        assertEquals("or else message", thrown.getMessage());
    }

    @Test
    public void testOrElseThrow_NotThrownWhenTrue() {
        assertDoesNotThrow(() -> If.is(true).then(() -> {
        }).orElseThrow(() -> new RuntimeException("should not throw")));
    }

    @Test
    public void testOrElseThrow_CheckedException() {
        assertThrows(Exception.class, () -> If.is(false).then(() -> {
        }).orElseThrow(() -> new Exception("checked orElse")));
    }

    // ==================== Integration / combined tests ====================

    @Test
    public void testCompleteIfElseChain() {
        AtomicReference<String> result = new AtomicReference<>();
        If.is(true).then(() -> result.set("success")).orElse(() -> result.set("failure"));
        assertEquals("success", result.get());

        If.is(false).then(() -> result.set("success2")).orElse(() -> result.set("failure2"));
        assertEquals("failure2", result.get());
    }

    @Test
    public void testNestedConditions() {
        List<String> list = Arrays.asList("a", "b", "c");
        String searchItem = "b";
        AtomicBoolean found = new AtomicBoolean(false);

        If.notEmpty(list).then(() -> {
            If.exists(list.indexOf(searchItem)).then(() -> found.set(true));
        });

        assertTrue(found.get());
    }

    @Test
    public void testNullCheckWithAction() {
        String value = "test";
        AtomicReference<String> processed = new AtomicReference<>();

        If.notNull(value).then(value, v -> processed.set(v.toUpperCase())).orElse(() -> processed.set("NULL"));
        assertEquals("TEST", processed.get());

        processed.set(null);
        If.notNull((String) null).then(value, v -> processed.set(v.toUpperCase())).orElse(() -> processed.set("NULL"));
        assertEquals("NULL", processed.get());
    }

    @Test
    public void testCollectionProcessing() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger sum = new AtomicInteger(0);

        If.notEmpty(numbers).then(() -> numbers.forEach(sum::addAndGet)).orElseThrow(() -> new IllegalStateException("List is empty"));

        assertEquals(15, sum.get());
    }

    @Test
    public void testValidationScenario() {
        String input = "";

        assertThrows(IllegalArgumentException.class, () -> If.isBlank(input).thenThrow(() -> new IllegalArgumentException("Input cannot be blank")));

        String validInput = "valid";
        assertDoesNotThrow(() -> If.isBlank(validInput).thenThrow(() -> new IllegalArgumentException("Input cannot be blank")));
    }

    @Test
    public void testMultipleConditionTypes() {
        AtomicInteger step = new AtomicInteger(0);

        If.is(true).then(() -> step.set(1));
        assertEquals(1, step.get());

        If.not(false).then(() -> step.set(2));
        assertEquals(2, step.get());

        If.notNull("value").then(() -> step.set(3));
        assertEquals(3, step.get());

        If.notEmpty("value").then(() -> step.set(4));
        assertEquals(4, step.get());

        If.notBlank("value").then(() -> step.set(5));
        assertEquals(5, step.get());

        If.exists(0).then(() -> step.set(6));
        assertEquals(6, step.get());
    }

    @Test
    public void testIsEmptyPrimitiveArrays() {
        assertTrue(If.isEmpty((boolean[]) null).b);
        assertTrue(If.isEmpty(new boolean[0]).b);
        assertFalse(If.isEmpty(new boolean[] { true }).b);

        assertTrue(If.isEmpty((char[]) null).b);
        assertTrue(If.isEmpty(new char[0]).b);
        assertFalse(If.isEmpty(new char[] { 'a' }).b);

        assertTrue(If.isEmpty((byte[]) null).b);
        assertTrue(If.isEmpty(new byte[0]).b);
        assertFalse(If.isEmpty(new byte[] { 1 }).b);

        assertTrue(If.isEmpty((short[]) null).b);
        assertTrue(If.isEmpty(new short[0]).b);
        assertFalse(If.isEmpty(new short[] { 1 }).b);

        assertTrue(If.isEmpty((int[]) null).b);
        assertTrue(If.isEmpty(new int[0]).b);
        assertFalse(If.isEmpty(new int[] { 1 }).b);

        assertTrue(If.isEmpty((long[]) null).b);
        assertTrue(If.isEmpty(new long[0]).b);
        assertFalse(If.isEmpty(new long[] { 1L }).b);

        assertTrue(If.isEmpty((float[]) null).b);
        assertTrue(If.isEmpty(new float[0]).b);
        assertFalse(If.isEmpty(new float[] { 1.0f }).b);

        assertTrue(If.isEmpty((double[]) null).b);
        assertTrue(If.isEmpty(new double[0]).b);
        assertFalse(If.isEmpty(new double[] { 1.0 }).b);
    }

    @Test
    public void testNotEmptyPrimitiveArrays() {
        assertTrue(If.notEmpty(new boolean[] { true }).b);
        assertFalse(If.notEmpty(new boolean[0]).b);

        assertTrue(If.notEmpty(new char[] { 'a' }).b);
        assertFalse(If.notEmpty(new char[0]).b);

        assertTrue(If.notEmpty(new byte[] { 1 }).b);
        assertFalse(If.notEmpty(new byte[0]).b);

        assertTrue(If.notEmpty(new short[] { 1 }).b);
        assertFalse(If.notEmpty(new short[0]).b);

        assertTrue(If.notEmpty(new int[] { 1 }).b);
        assertFalse(If.notEmpty(new int[0]).b);

        assertTrue(If.notEmpty(new long[] { 1L }).b);
        assertFalse(If.notEmpty(new long[0]).b);

        assertTrue(If.notEmpty(new float[] { 1.0f }).b);
        assertFalse(If.notEmpty(new float[0]).b);

        assertTrue(If.notEmpty(new double[] { 1.0 }).b);
        assertFalse(If.notEmpty(new double[0]).b);
    }

    @Test
    public void testComplexChaining() {
        List<String> results = new ArrayList<>();

        If.notEmpty(Arrays.asList("a", "b", "c")).then(() -> results.add("list not empty")).orElse(() -> results.add("list empty"));

        If.isBlank("  ").then(() -> results.add("string is blank")).orElse(() -> results.add("string not blank"));

        If.exists(Arrays.asList("x", "y", "z").indexOf("y")).then(() -> results.add("element found")).orElse(() -> results.add("element not found"));

        assertEquals(Arrays.asList("list not empty", "string is blank", "element found"), results);
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
    }

    @Test
    public void testNullArguments_FalseCondition() {
        // Null checks should be enforced even when condition is false
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then((Throwables.Runnable<?>) null));

        assertThrows(IllegalArgumentException.class, () -> If.is(false).then("value", null));

        assertThrows(IllegalArgumentException.class, () -> If.is(false).thenThrow(null));
    }

    @Test
    public void testOrElse_NullArguments_FalseCondition() {
        // orElse null checks should be enforced even when the orElse branch is not taken
        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElse((Throwables.Runnable<?>) null));

        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElse("value", null));

        assertThrows(IllegalArgumentException.class, () -> If.is(false).then(() -> {
        }).orElseThrow(null));
    }

    @Test
    public void testOrElse_CachedInstances() {
        assertSame(If.OrElse.TRUE, If.OrElse.TRUE);
        assertSame(If.OrElse.FALSE, If.OrElse.FALSE);
        assertTrue(If.OrElse.TRUE != If.OrElse.FALSE);
    }

    @Test
    public void testThenDoNothing_WithOrElseConsumer() {
        AtomicReference<String> ref = new AtomicReference<>("untouched");
        If.is(false).thenDoNothing().orElse("touched", ref::set);
        assertEquals("touched", ref.get());

        ref.set("untouched");
        If.is(true).thenDoNothing().orElse("touched", ref::set);
        assertEquals("untouched", ref.get());
    }

    @Test
    public void testIs_SideEffectOnlyInThenBranch() {
        List<String> log = new ArrayList<>();
        If.is(true).then(() -> log.add("then")).orElse(() -> log.add("else"));
        assertEquals(Arrays.asList("then"), log);

        log.clear();
        If.is(false).then(() -> log.add("then")).orElse(() -> log.add("else"));
        assertEquals(Arrays.asList("else"), log);
    }
}
