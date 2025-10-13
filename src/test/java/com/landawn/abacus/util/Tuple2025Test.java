package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.Tuple.Tuple8;
import com.landawn.abacus.util.Tuple.Tuple9;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class Tuple2025Test extends TestBase {

    @Test
    public void testOf_Tuple1() {
        Tuple1<String> t1 = Tuple.of("hello");
        Assertions.assertEquals("hello", t1._1);

        Tuple1<String> t1Null = Tuple.of((String) null);
        Assertions.assertNull(t1Null._1);
    }

    @Test
    public void testOf_Tuple2() {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        Assertions.assertEquals("hello", t2._1);
        Assertions.assertEquals(42, t2._2);

        Tuple2<String, Integer> t2WithNull = Tuple.of(null, 42);
        Assertions.assertNull(t2WithNull._1);
        Assertions.assertEquals(42, t2WithNull._2);

        Tuple2<String, Integer> t2AllNull = Tuple.of(null, null);
        Assertions.assertNull(t2AllNull._1);
        Assertions.assertNull(t2AllNull._2);
    }

    @Test
    public void testOf_Tuple3() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("hello", 42, true);
        Assertions.assertEquals("hello", t3._1);
        Assertions.assertEquals(42, t3._2);
        Assertions.assertEquals(true, t3._3);

        Tuple3<String, Integer, Boolean> t3WithNull = Tuple.of(null, 42, null);
        Assertions.assertNull(t3WithNull._1);
        Assertions.assertEquals(42, t3WithNull._2);
        Assertions.assertNull(t3WithNull._3);
    }

    @Test
    public void testOf_Tuple4() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("hello", 42, true, 3.14);
        Assertions.assertEquals("hello", t4._1);
        Assertions.assertEquals(42, t4._2);
        Assertions.assertEquals(true, t4._3);
        Assertions.assertEquals(3.14, t4._4);

        Tuple4<String, Integer, Boolean, Double> t4WithNull = Tuple.of("hello", null, true, null);
        Assertions.assertEquals("hello", t4WithNull._1);
        Assertions.assertNull(t4WithNull._2);
        Assertions.assertEquals(true, t4WithNull._3);
        Assertions.assertNull(t4WithNull._4);
    }

    @Test
    public void testOf_Tuple5() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("hello", 42, true, 3.14, 100L);
        Assertions.assertEquals("hello", t5._1);
        Assertions.assertEquals(42, t5._2);
        Assertions.assertEquals(true, t5._3);
        Assertions.assertEquals(3.14, t5._4);
        Assertions.assertEquals(100L, t5._5);
    }

    @Test
    public void testOf_Tuple6() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("hello", 42, true, 3.14, 100L, 'x');
        Assertions.assertEquals("hello", t6._1);
        Assertions.assertEquals(42, t6._2);
        Assertions.assertEquals(true, t6._3);
        Assertions.assertEquals(3.14, t6._4);
        Assertions.assertEquals(100L, t6._5);
        Assertions.assertEquals('x', t6._6);
    }

    @Test
    public void testOf_Tuple7() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("hello", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertEquals("hello", t7._1);
        Assertions.assertEquals(42, t7._2);
        Assertions.assertEquals(true, t7._3);
        Assertions.assertEquals(3.14, t7._4);
        Assertions.assertEquals(100L, t7._5);
        Assertions.assertEquals('x', t7._6);
        Assertions.assertEquals(2.5f, t7._7);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOf_Tuple8() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("hello", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertEquals("hello", t8._1);
        Assertions.assertEquals(42, t8._2);
        Assertions.assertEquals(true, t8._3);
        Assertions.assertEquals(3.14, t8._4);
        Assertions.assertEquals(100L, t8._5);
        Assertions.assertEquals('x', t8._6);
        Assertions.assertEquals(2.5f, t8._7);
        Assertions.assertEquals((byte) 10, t8._8);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testOf_Tuple9() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("hello", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertEquals("hello", t9._1);
        Assertions.assertEquals(42, t9._2);
        Assertions.assertEquals(true, t9._3);
        Assertions.assertEquals(3.14, t9._4);
        Assertions.assertEquals(100L, t9._5);
        Assertions.assertEquals('x', t9._6);
        Assertions.assertEquals(2.5f, t9._7);
        Assertions.assertEquals((byte) 10, t9._8);
        Assertions.assertEquals((short) 20, t9._9);
    }

    @Test
    public void testCreate_FromMapEntry() {
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("key", 100);
        Tuple2<String, Integer> tuple = Tuple.create(entry);

        Assertions.assertEquals("key", tuple._1);
        Assertions.assertEquals(100, tuple._2);

        Map.Entry<String, Integer> nullEntry = new HashMap.SimpleEntry<>(null, null);
        Tuple2<String, Integer> tupleWithNull = Tuple.create(nullEntry);
        Assertions.assertNull(tupleWithNull._1);
        Assertions.assertNull(tupleWithNull._2);
    }

    @Test
    public void testCreate_FromArray() {
        Tuple1<String> t1 = Tuple.create(new Object[] { "a" });
        Assertions.assertEquals("a", t1._1);

        Tuple2<String, Integer> t2 = Tuple.create(new Object[] { "a", 1 });
        Assertions.assertEquals("a", t2._1);
        Assertions.assertEquals(1, t2._2);

        Tuple3<String, Integer, Boolean> t3 = Tuple.create(new Object[] { "a", 1, true });
        Assertions.assertEquals("a", t3._1);
        Assertions.assertEquals(1, t3._2);
        Assertions.assertEquals(true, t3._3);

        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.create(new Object[] { "a", 1, true, 2.0 });
        Assertions.assertEquals("a", t4._1);
        Assertions.assertEquals(1, t4._2);
        Assertions.assertEquals(true, t4._3);
        Assertions.assertEquals(2.0, t4._4);

        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.create(new Object[] { "a", 1, true, 2.0, 3L });
        Assertions.assertEquals("a", t5._1);
        Assertions.assertEquals(1, t5._2);
        Assertions.assertEquals(true, t5._3);
        Assertions.assertEquals(2.0, t5._4);
        Assertions.assertEquals(3L, t5._5);

        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.create(new Object[] { "a", 1, true, 2.0, 3L, 'x' });
        Assertions.assertEquals("a", t6._1);
        Assertions.assertEquals(1, t6._2);
        Assertions.assertEquals(true, t6._3);
        Assertions.assertEquals(2.0, t6._4);
        Assertions.assertEquals(3L, t6._5);
        Assertions.assertEquals('x', t6._6);

        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.create(new Object[] { "a", 1, true, 2.0, 3L, 'x', 4.0f });
        Assertions.assertEquals("a", t7._1);
        Assertions.assertEquals(1, t7._2);
        Assertions.assertEquals(true, t7._3);
        Assertions.assertEquals(2.0, t7._4);
        Assertions.assertEquals(3L, t7._5);
        Assertions.assertEquals('x', t7._6);
        Assertions.assertEquals(4.0f, t7._7);

        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.create(new Object[] { "a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5 });
        Assertions.assertEquals("a", t8._1);
        Assertions.assertEquals(1, t8._2);
        Assertions.assertEquals(true, t8._3);
        Assertions.assertEquals(2.0, t8._4);
        Assertions.assertEquals(3L, t8._5);
        Assertions.assertEquals('x', t8._6);
        Assertions.assertEquals(4.0f, t8._7);
        Assertions.assertEquals((byte) 5, t8._8);

        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple
                .create(new Object[] { "a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5, (short) 6 });
        Assertions.assertEquals("a", t9._1);
        Assertions.assertEquals(1, t9._2);
        Assertions.assertEquals(true, t9._3);
        Assertions.assertEquals(2.0, t9._4);
        Assertions.assertEquals(3L, t9._5);
        Assertions.assertEquals('x', t9._6);
        Assertions.assertEquals(4.0f, t9._7);
        Assertions.assertEquals((byte) 5, t9._8);
        Assertions.assertEquals((short) 6, t9._9);
    }

    @Test
    public void testCreate_FromArray_InvalidSize() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tuple.create(new Object[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });
        });
    }

    @Test
    public void testCreate_FromCollection() {
        Tuple1<String> t1 = Tuple.create(Arrays.asList("a"));
        Assertions.assertEquals("a", t1._1);

        Tuple2<String, Integer> t2 = Tuple.create(Arrays.asList("a", 1));
        Assertions.assertEquals("a", t2._1);
        Assertions.assertEquals(1, t2._2);

        Tuple3<String, Integer, Boolean> t3 = Tuple.create(Arrays.asList("a", 1, true));
        Assertions.assertEquals("a", t3._1);
        Assertions.assertEquals(1, t3._2);
        Assertions.assertEquals(true, t3._3);

        Collection<?> emptyList = new ArrayList<>();
        assertEquals(0, Tuple.create(emptyList).arity());

        List<Integer> largeList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Tuple.create(largeList);
        });
    }

    @Test
    public void testToList_Tuple1() {
        Tuple1<String> t1 = Tuple.of("a");
        List<String> list = Tuple.toList(t1);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testToList_Tuple2() {
        Tuple2<String, String> t2 = Tuple.of("a", "b");
        List<String> list = Tuple.toList(t2);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
    }

    @Test
    public void testToList_Tuple3() {
        Tuple3<String, String, String> t3 = Tuple.of("a", "b", "c");
        List<String> list = Tuple.toList(t3);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));
    }

    @Test
    public void testToList_Tuple4() {
        Tuple4<Integer, Integer, Integer, Integer> t4 = Tuple.of(1, 2, 3, 4);
        List<Integer> list = Tuple.toList(t4);
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
        Assertions.assertEquals(4, list.get(3));
    }

    @Test
    public void testToList_Tuple5() {
        Tuple5<Integer, Integer, Integer, Integer, Integer> t5 = Tuple.of(1, 2, 3, 4, 5);
        List<Integer> list = Tuple.toList(t5);
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(5, list.get(4));
    }

    @Test
    public void testToList_Tuple6() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t6 = Tuple.of(1, 2, 3, 4, 5, 6);
        List<Integer> list = Tuple.toList(t6);
        Assertions.assertEquals(6, list.size());
        Assertions.assertEquals(6, list.get(5));
    }

    @Test
    public void testToList_Tuple7() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> t7 = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> list = Tuple.toList(t7);
        Assertions.assertEquals(7, list.size());
        Assertions.assertEquals(7, list.get(6));
    }

    @Test
    public void testToList_Tuple8() {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t8 = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<Integer> list = Tuple.toList(t8);
        Assertions.assertEquals(8, list.size());
        Assertions.assertEquals(8, list.get(7));
    }

    @Test
    public void testToList_Tuple9() {
        Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t9 = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> list = Tuple.toList(t9);
        Assertions.assertEquals(9, list.size());
        Assertions.assertEquals(9, list.get(8));
    }

    @Test
    public void testFlatten_Tuple2ToTuple3() {
        Tuple2<String, Integer> inner = Tuple.of("a", 1);
        Tuple2<Tuple2<String, Integer>, Boolean> nested = Tuple.of(inner, true);
        Tuple3<String, Integer, Boolean> flattened = Tuple.flatten(nested);

        Assertions.assertEquals("a", flattened._1);
        Assertions.assertEquals(1, flattened._2);
        Assertions.assertEquals(true, flattened._3);
    }

    @Test
    public void testFlatten_Tuple3ToTuple5() {
        Tuple3<String, Integer, Boolean> inner = Tuple.of("a", 1, true);
        Tuple3<Tuple3<String, Integer, Boolean>, Double, Long> nested = Tuple.of(inner, 2.5, 100L);
        Tuple5<String, Integer, Boolean, Double, Long> flattened = Tuple.flatten(nested);

        Assertions.assertEquals("a", flattened._1);
        Assertions.assertEquals(1, flattened._2);
        Assertions.assertEquals(true, flattened._3);
        Assertions.assertEquals(2.5, flattened._4);
        Assertions.assertEquals(100L, flattened._5);
    }

    @Test
    public void testAccept() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        StringBuilder sb = new StringBuilder();
        t2.accept(tuple -> sb.append(tuple._1).append(":").append(tuple._2));
        Assertions.assertEquals("hello:42", sb.toString());
    }

    @Test
    public void testMap() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        String result = t2.map(tuple -> tuple._1 + " " + tuple._2);
        Assertions.assertEquals("hello 42", result);
    }

    @Test
    public void testFilter_True() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        Optional<Tuple2<String, Integer>> result = t2.filter(tuple -> tuple._2 > 40);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(t2, result.get());
    }

    @Test
    public void testFilter_False() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        Optional<Tuple2<String, Integer>> result = t2.filter(tuple -> tuple._2 < 40);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testTuple1_Arity() {
        Tuple1<String> t1 = Tuple.of("test");
        Assertions.assertEquals(1, t1.arity());
    }

    @Test
    public void testTuple1_AnyNull() {
        Tuple1<String> t1 = Tuple.of("test");
        Assertions.assertFalse(t1.anyNull());

        Tuple1<String> t1Null = Tuple.of((String) null);
        Assertions.assertTrue(t1Null.anyNull());
    }

    @Test
    public void testTuple1_AllNull() {
        Tuple1<String> t1 = Tuple.of("test");
        Assertions.assertFalse(t1.allNull());

        Tuple1<String> t1Null = Tuple.of((String) null);
        Assertions.assertTrue(t1Null.allNull());
    }

    @Test
    public void testTuple1_Contains() {
        Tuple1<String> t1 = Tuple.of("test");
        Assertions.assertTrue(t1.contains("test"));
        Assertions.assertFalse(t1.contains("other"));
        Assertions.assertFalse(t1.contains(null));

        Tuple1<String> t1Null = Tuple.of((String) null);
        Assertions.assertTrue(t1Null.contains(null));
    }

    @Test
    public void testTuple1_ToArray() {
        Tuple1<String> t1 = Tuple.of("test");
        Object[] array = t1.toArray();
        Assertions.assertEquals(1, array.length);
        Assertions.assertEquals("test", array[0]);
    }

    @Test
    public void testTuple1_ToArrayTyped() {
        Tuple1<String> t1 = Tuple.of("test");

        String[] smallArray = new String[0];
        String[] result1 = t1.toArray(smallArray);
        Assertions.assertEquals(1, result1.length);
        Assertions.assertEquals("test", result1[0]);

        String[] exactArray = new String[1];
        String[] result2 = t1.toArray(exactArray);
        Assertions.assertSame(exactArray, result2);
        Assertions.assertEquals("test", result2[0]);

        String[] largeArray = new String[3];
        String[] result3 = t1.toArray(largeArray);
        Assertions.assertSame(largeArray, result3);
        Assertions.assertEquals("test", result3[0]);
    }

    @Test
    public void testTuple1_ForEach() throws Exception {
        Tuple1<String> t1 = Tuple.of("test");
        List<Object> collected = new ArrayList<>();
        t1.forEach(collected::add);
        Assertions.assertEquals(1, collected.size());
        Assertions.assertEquals("test", collected.get(0));
    }

    @Test
    public void testTuple1_HashCode() {
        Tuple1<String> t1 = Tuple.of("test");
        Tuple1<String> t1Same = Tuple.of("test");
        Assertions.assertEquals(t1.hashCode(), t1Same.hashCode());

        Tuple1<String> t1Null = Tuple.of((String) null);
        int hashWithNull = t1Null.hashCode();
        Assertions.assertNotNull(hashWithNull);
    }

    @Test
    public void testTuple1_Equals() {
        Tuple1<String> t1 = Tuple.of("test");
        Tuple1<String> t1Same = Tuple.of("test");
        Tuple1<String> t1Different = Tuple.of("other");

        Assertions.assertTrue(t1.equals(t1));
        Assertions.assertTrue(t1.equals(t1Same));
        Assertions.assertFalse(t1.equals(t1Different));
        Assertions.assertFalse(t1.equals(null));
        Assertions.assertFalse(t1.equals("string"));
    }

    @Test
    public void testTuple1_ToString() {
        Tuple1<String> t1 = Tuple.of("test");
        Assertions.assertEquals("(test)", t1.toString());

        Tuple1<String> t1Null = Tuple.of((String) null);
        Assertions.assertEquals("(null)", t1Null.toString());
    }

    @Test
    public void testTuple2_Arity() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Assertions.assertEquals(2, t2.arity());
    }

    @Test
    public void testTuple2_AnyNull() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Assertions.assertFalse(t2.anyNull());

        Tuple2<String, Integer> t2OneNull = Tuple.of(null, 42);
        Assertions.assertTrue(t2OneNull.anyNull());

        Tuple2<String, Integer> t2AllNull = Tuple.of(null, null);
        Assertions.assertTrue(t2AllNull.anyNull());
    }

    @Test
    public void testTuple2_AllNull() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Assertions.assertFalse(t2.allNull());

        Tuple2<String, Integer> t2OneNull = Tuple.of(null, 42);
        Assertions.assertFalse(t2OneNull.allNull());

        Tuple2<String, Integer> t2AllNull = Tuple.of(null, null);
        Assertions.assertTrue(t2AllNull.allNull());
    }

    @Test
    public void testTuple2_Contains() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Assertions.assertTrue(t2.contains("test"));
        Assertions.assertTrue(t2.contains(42));
        Assertions.assertFalse(t2.contains("other"));
        Assertions.assertFalse(t2.contains(100));
    }

    @Test
    public void testTuple2_ToArray() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Object[] array = t2.toArray();
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals("test", array[0]);
        Assertions.assertEquals(42, array[1]);
    }

    @Test
    public void testTuple2_ToArrayTyped() {
        Tuple2<String, String> t2 = Tuple.of("a", "b");

        String[] smallArray = new String[1];
        String[] result1 = t2.toArray(smallArray);
        Assertions.assertEquals(2, result1.length);
        Assertions.assertEquals("a", result1[0]);
        Assertions.assertEquals("b", result1[1]);

        String[] exactArray = new String[2];
        String[] result2 = t2.toArray(exactArray);
        Assertions.assertSame(exactArray, result2);

        String[] largeArray = new String[5];
        String[] result3 = t2.toArray(largeArray);
        Assertions.assertSame(largeArray, result3);
    }

    @Test
    public void testTuple2_ToPair() {
        Tuple2<String, Integer> t2 = Tuple.of("key", 100);
        Pair<String, Integer> pair = t2.toPair();
        Assertions.assertEquals("key", pair.left());
        Assertions.assertEquals(100, pair.right());
    }

    @Test
    public void testTuple2_ToEntry() {
        Tuple2<String, Integer> t2 = Tuple.of("key", 100);
        ImmutableEntry<String, Integer> entry = t2.toEntry();
        Assertions.assertEquals("key", entry.getKey());
        Assertions.assertEquals(100, entry.getValue());
    }

    @Test
    public void testTuple2_Reverse() {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        Tuple2<Integer, String> reversed = t2.reverse();
        Assertions.assertEquals(42, reversed._1);
        Assertions.assertEquals("hello", reversed._2);
    }

    @Test
    public void testTuple2_ForEach() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("a", 1);
        List<Object> collected = new ArrayList<>();
        t2.forEach(collected::add);
        Assertions.assertEquals(2, collected.size());
        Assertions.assertEquals("a", collected.get(0));
        Assertions.assertEquals(1, collected.get(1));
    }

    @Test
    public void testTuple2_AcceptBiConsumer() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        StringBuilder sb = new StringBuilder();
        t2.accept((s, i) -> sb.append(s).append(":").append(i));
        Assertions.assertEquals("hello:42", sb.toString());
    }

    @Test
    public void testTuple2_MapBiFunction() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        String result = t2.map((s, i) -> s + " " + i);
        Assertions.assertEquals("hello 42", result);
    }

    @Test
    public void testTuple2_FilterBiPredicate() throws Exception {
        Tuple2<String, Integer> t2 = Tuple.of("hello", 42);
        Optional<Tuple2<String, Integer>> result1 = t2.filter((s, i) -> i > 40);
        Assertions.assertTrue(result1.isPresent());

        Optional<Tuple2<String, Integer>> result2 = t2.filter((s, i) -> i < 40);
        Assertions.assertFalse(result2.isPresent());
    }

    @Test
    public void testTuple2_HashCode() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Tuple2<String, Integer> t2Same = Tuple.of("test", 42);
        Assertions.assertEquals(t2.hashCode(), t2Same.hashCode());

        Tuple2<String, Integer> t2WithNull = Tuple.of(null, null);
        int hashWithNull = t2WithNull.hashCode();
        Assertions.assertNotNull(hashWithNull);
    }

    @Test
    public void testTuple2_Equals() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Tuple2<String, Integer> t2Same = Tuple.of("test", 42);
        Tuple2<String, Integer> t2Different = Tuple.of("test", 100);

        Assertions.assertTrue(t2.equals(t2));
        Assertions.assertTrue(t2.equals(t2Same));
        Assertions.assertFalse(t2.equals(t2Different));
        Assertions.assertFalse(t2.equals(null));
        Assertions.assertFalse(t2.equals("string"));
    }

    @Test
    public void testTuple2_ToString() {
        Tuple2<String, Integer> t2 = Tuple.of("test", 42);
        Assertions.assertEquals("(test, 42)", t2.toString());

        Tuple2<String, Integer> t2WithNull = Tuple.of(null, null);
        Assertions.assertEquals("(null, null)", t2WithNull.toString());
    }

    @Test
    public void testTuple3_Arity() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Assertions.assertEquals(3, t3.arity());
    }

    @Test
    public void testTuple3_AnyNull() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Assertions.assertFalse(t3.anyNull());

        Tuple3<String, Integer, Boolean> t3OneNull = Tuple.of(null, 42, true);
        Assertions.assertTrue(t3OneNull.anyNull());

        Tuple3<String, Integer, Boolean> t3AllNull = Tuple.of(null, null, null);
        Assertions.assertTrue(t3AllNull.anyNull());
    }

    @Test
    public void testTuple3_AllNull() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Assertions.assertFalse(t3.allNull());

        Tuple3<String, Integer, Boolean> t3SomeNull = Tuple.of(null, 42, null);
        Assertions.assertFalse(t3SomeNull.allNull());

        Tuple3<String, Integer, Boolean> t3AllNull = Tuple.of(null, null, null);
        Assertions.assertTrue(t3AllNull.allNull());
    }

    @Test
    public void testTuple3_Contains() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Assertions.assertTrue(t3.contains("test"));
        Assertions.assertTrue(t3.contains(42));
        Assertions.assertTrue(t3.contains(true));
        Assertions.assertFalse(t3.contains("other"));
    }

    @Test
    public void testTuple3_ToArray() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Object[] array = t3.toArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("test", array[0]);
        Assertions.assertEquals(42, array[1]);
        Assertions.assertEquals(true, array[2]);
    }

    @Test
    public void testTuple3_ToArrayTyped() {
        Tuple3<String, String, String> t3 = Tuple.of("a", "b", "c");

        String[] smallArray = new String[2];
        String[] result = t3.toArray(smallArray);
        Assertions.assertEquals(3, result.length);
        Assertions.assertEquals("a", result[0]);
        Assertions.assertEquals("b", result[1]);
        Assertions.assertEquals("c", result[2]);
    }

    @Test
    public void testTuple3_ToTriple() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Triple<String, Integer, Boolean> triple = t3.toTriple();
        Assertions.assertEquals("test", triple.left());
        Assertions.assertEquals(42, triple.middle());
        Assertions.assertEquals(true, triple.right());
    }

    @Test
    public void testTuple3_Reverse() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        Tuple3<Boolean, Integer, String> reversed = t3.reverse();
        Assertions.assertEquals(true, reversed._1);
        Assertions.assertEquals(1, reversed._2);
        Assertions.assertEquals("a", reversed._3);
    }

    @Test
    public void testTuple3_ForEach() throws Exception {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        List<Object> collected = new ArrayList<>();
        t3.forEach(collected::add);
        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals("a", collected.get(0));
        Assertions.assertEquals(1, collected.get(1));
        Assertions.assertEquals(true, collected.get(2));
    }

    @Test
    public void testTuple3_AcceptTriConsumer() throws Exception {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("hello", 42, true);
        StringBuilder sb = new StringBuilder();
        t3.accept((s, i, b) -> sb.append(s).append(":").append(i).append(":").append(b));
        Assertions.assertEquals("hello:42:true", sb.toString());
    }

    @Test
    public void testTuple3_MapTriFunction() throws Exception {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("hello", 42, true);
        String result = t3.map((s, i, b) -> s + " " + i + " " + b);
        Assertions.assertEquals("hello 42 true", result);
    }

    @Test
    public void testTuple3_FilterTriPredicate() throws Exception {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("hello", 42, true);
        Optional<Tuple3<String, Integer, Boolean>> result1 = t3.filter((s, i, b) -> i > 40 && b);
        Assertions.assertTrue(result1.isPresent());

        Optional<Tuple3<String, Integer, Boolean>> result2 = t3.filter((s, i, b) -> i < 40);
        Assertions.assertFalse(result2.isPresent());
    }

    @Test
    public void testTuple3_HashCode() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Tuple3<String, Integer, Boolean> t3Same = Tuple.of("test", 42, true);
        Assertions.assertEquals(t3.hashCode(), t3Same.hashCode());
    }

    @Test
    public void testTuple3_Equals() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Tuple3<String, Integer, Boolean> t3Same = Tuple.of("test", 42, true);
        Tuple3<String, Integer, Boolean> t3Different = Tuple.of("test", 42, false);

        Assertions.assertTrue(t3.equals(t3));
        Assertions.assertTrue(t3.equals(t3Same));
        Assertions.assertFalse(t3.equals(t3Different));
        Assertions.assertFalse(t3.equals(null));
    }

    @Test
    public void testTuple3_ToString() {
        Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 42, true);
        Assertions.assertEquals("(test, 42, true)", t3.toString());
    }

    @Test
    public void testTuple4_Arity() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Assertions.assertEquals(4, t4.arity());
    }

    @Test
    public void testTuple4_AnyNull() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Assertions.assertFalse(t4.anyNull());

        Tuple4<String, Integer, Boolean, Double> t4WithNull = Tuple.of("test", null, true, 3.14);
        Assertions.assertTrue(t4WithNull.anyNull());
    }

    @Test
    public void testTuple4_AllNull() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Assertions.assertFalse(t4.allNull());

        Tuple4<String, Integer, Boolean, Double> t4AllNull = Tuple.of(null, null, null, null);
        Assertions.assertTrue(t4AllNull.allNull());
    }

    @Test
    public void testTuple4_Contains() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Assertions.assertTrue(t4.contains("test"));
        Assertions.assertTrue(t4.contains(42));
        Assertions.assertTrue(t4.contains(true));
        Assertions.assertTrue(t4.contains(3.14));
        Assertions.assertFalse(t4.contains("other"));
    }

    @Test
    public void testTuple4_ToArray() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Object[] array = t4.toArray();
        Assertions.assertEquals(4, array.length);
        Assertions.assertEquals("test", array[0]);
        Assertions.assertEquals(3.14, array[3]);
    }

    @Test
    public void testTuple4_ToArrayTyped() {
        Tuple4<String, String, String, String> t4 = Tuple.of("a", "b", "c", "d");
        String[] result = t4.toArray(new String[0]);
        Assertions.assertEquals(4, result.length);
    }

    @Test
    public void testTuple4_Reverse() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("a", 1, true, 2.0);
        Tuple4<Double, Boolean, Integer, String> reversed = t4.reverse();
        Assertions.assertEquals(2.0, reversed._1);
        Assertions.assertEquals(true, reversed._2);
        Assertions.assertEquals(1, reversed._3);
        Assertions.assertEquals("a", reversed._4);
    }

    @Test
    public void testTuple4_ForEach() throws Exception {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("a", 1, true, 2.0);
        List<Object> collected = new ArrayList<>();
        t4.forEach(collected::add);
        Assertions.assertEquals(4, collected.size());
    }

    @Test
    public void testTuple4_HashCode() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Tuple4<String, Integer, Boolean, Double> t4Same = Tuple.of("test", 42, true, 3.14);
        Assertions.assertEquals(t4.hashCode(), t4Same.hashCode());
    }

    @Test
    public void testTuple4_Equals() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Tuple4<String, Integer, Boolean, Double> t4Same = Tuple.of("test", 42, true, 3.14);
        Tuple4<String, Integer, Boolean, Double> t4Different = Tuple.of("test", 42, true, 2.0);

        Assertions.assertTrue(t4.equals(t4));
        Assertions.assertTrue(t4.equals(t4Same));
        Assertions.assertFalse(t4.equals(t4Different));
    }

    @Test
    public void testTuple4_ToString() {
        Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 42, true, 3.14);
        Assertions.assertEquals("(test, 42, true, 3.14)", t4.toString());
    }

    @Test
    public void testTuple5_Arity() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertEquals(5, t5.arity());
    }

    @Test
    public void testTuple5_AnyNull() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertFalse(t5.anyNull());

        Tuple5<String, Integer, Boolean, Double, Long> t5WithNull = Tuple.of("test", null, true, 3.14, null);
        Assertions.assertTrue(t5WithNull.anyNull());
    }

    @Test
    public void testTuple5_AllNull() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertFalse(t5.allNull());

        Tuple5<String, Integer, Boolean, Double, Long> t5AllNull = Tuple.of(null, null, null, null, null);
        Assertions.assertTrue(t5AllNull.allNull());
    }

    @Test
    public void testTuple5_Contains() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertTrue(t5.contains("test"));
        Assertions.assertTrue(t5.contains(100L));
        Assertions.assertFalse(t5.contains("other"));
    }

    @Test
    public void testTuple5_ToArray() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Object[] array = t5.toArray();
        Assertions.assertEquals(5, array.length);
        Assertions.assertEquals(100L, array[4]);
    }

    @Test
    public void testTuple5_ToArrayTyped() {
        Tuple5<Integer, Integer, Integer, Integer, Integer> t5 = Tuple.of(1, 2, 3, 4, 5);
        Integer[] result = t5.toArray(new Integer[0]);
        Assertions.assertEquals(5, result.length);
    }

    @Test
    public void testTuple5_Reverse() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("a", 1, true, 2.0, 3L);
        Tuple5<Long, Double, Boolean, Integer, String> reversed = t5.reverse();
        Assertions.assertEquals(3L, reversed._1);
        Assertions.assertEquals(2.0, reversed._2);
        Assertions.assertEquals(true, reversed._3);
        Assertions.assertEquals(1, reversed._4);
        Assertions.assertEquals("a", reversed._5);
    }

    @Test
    public void testTuple5_ForEach() throws Exception {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("a", 1, true, 2.0, 3L);
        List<Object> collected = new ArrayList<>();
        t5.forEach(collected::add);
        Assertions.assertEquals(5, collected.size());
    }

    @Test
    public void testTuple5_HashCode() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Tuple5<String, Integer, Boolean, Double, Long> t5Same = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertEquals(t5.hashCode(), t5Same.hashCode());
    }

    @Test
    public void testTuple5_Equals() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Tuple5<String, Integer, Boolean, Double, Long> t5Same = Tuple.of("test", 42, true, 3.14, 100L);
        Tuple5<String, Integer, Boolean, Double, Long> t5Different = Tuple.of("test", 42, true, 3.14, 200L);

        Assertions.assertTrue(t5.equals(t5));
        Assertions.assertTrue(t5.equals(t5Same));
        Assertions.assertFalse(t5.equals(t5Different));
    }

    @Test
    public void testTuple5_ToString() {
        Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 42, true, 3.14, 100L);
        Assertions.assertEquals("(test, 42, true, 3.14, 100)", t5.toString());
    }

    @Test
    public void testTuple6_Arity() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertEquals(6, t6.arity());
    }

    @Test
    public void testTuple6_AnyNull() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertFalse(t6.anyNull());

        Tuple6<String, Integer, Boolean, Double, Long, Character> t6WithNull = Tuple.of("test", null, true, 3.14, null, 'x');
        Assertions.assertTrue(t6WithNull.anyNull());
    }

    @Test
    public void testTuple6_AllNull() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertFalse(t6.allNull());

        Tuple6<String, Integer, Boolean, Double, Long, Character> t6AllNull = Tuple.of(null, null, null, null, null, null);
        Assertions.assertTrue(t6AllNull.allNull());
    }

    @Test
    public void testTuple6_Contains() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertTrue(t6.contains("test"));
        Assertions.assertTrue(t6.contains('x'));
        Assertions.assertFalse(t6.contains("other"));
    }

    @Test
    public void testTuple6_ToArray() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Object[] array = t6.toArray();
        Assertions.assertEquals(6, array.length);
        Assertions.assertEquals('x', array[5]);
    }

    @Test
    public void testTuple6_ToArrayTyped() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> t6 = Tuple.of(1, 2, 3, 4, 5, 6);
        Integer[] result = t6.toArray(new Integer[0]);
        Assertions.assertEquals(6, result.length);
    }

    @Test
    public void testTuple6_Reverse() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("a", 1, true, 2.0, 3L, 'x');
        Tuple6<Character, Long, Double, Boolean, Integer, String> reversed = t6.reverse();
        Assertions.assertEquals('x', reversed._1);
        Assertions.assertEquals(3L, reversed._2);
        Assertions.assertEquals(2.0, reversed._3);
        Assertions.assertEquals(true, reversed._4);
        Assertions.assertEquals(1, reversed._5);
        Assertions.assertEquals("a", reversed._6);
    }

    @Test
    public void testTuple6_ForEach() throws Exception {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("a", 1, true, 2.0, 3L, 'x');
        List<Object> collected = new ArrayList<>();
        t6.forEach(collected::add);
        Assertions.assertEquals(6, collected.size());
    }

    @Test
    public void testTuple6_HashCode() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6Same = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertEquals(t6.hashCode(), t6Same.hashCode());
    }

    @Test
    public void testTuple6_Equals() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6Same = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6Different = Tuple.of("test", 42, true, 3.14, 100L, 'y');

        Assertions.assertTrue(t6.equals(t6));
        Assertions.assertTrue(t6.equals(t6Same));
        Assertions.assertFalse(t6.equals(t6Different));
    }

    @Test
    public void testTuple6_ToString() {
        Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 42, true, 3.14, 100L, 'x');
        Assertions.assertEquals("(test, 42, true, 3.14, 100, x)", t6.toString());
    }

    @Test
    public void testTuple7_Arity() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertEquals(7, t7.arity());
    }

    @Test
    public void testTuple7_AnyNull() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertFalse(t7.anyNull());

        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7WithNull = Tuple.of(null, 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertTrue(t7WithNull.anyNull());
    }

    @Test
    public void testTuple7_AllNull() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertFalse(t7.allNull());

        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7AllNull = Tuple.of(null, null, null, null, null, null, null);
        Assertions.assertTrue(t7AllNull.allNull());
    }

    @Test
    public void testTuple7_Contains() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertTrue(t7.contains("test"));
        Assertions.assertTrue(t7.contains(2.5f));
        Assertions.assertFalse(t7.contains("other"));
    }

    @Test
    public void testTuple7_ToArray() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Object[] array = t7.toArray();
        Assertions.assertEquals(7, array.length);
        Assertions.assertEquals(2.5f, array[6]);
    }

    @Test
    public void testTuple7_ToArrayTyped() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> t7 = Tuple.of(1, 2, 3, 4, 5, 6, 7);
        Integer[] result = t7.toArray(new Integer[0]);
        Assertions.assertEquals(7, result.length);
    }

    @Test
    public void testTuple7_Reverse() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f);
        Tuple7<Float, Character, Long, Double, Boolean, Integer, String> reversed = t7.reverse();
        Assertions.assertEquals(4.0f, reversed._1);
        Assertions.assertEquals('x', reversed._2);
        Assertions.assertEquals(3L, reversed._3);
        Assertions.assertEquals(2.0, reversed._4);
        Assertions.assertEquals(true, reversed._5);
        Assertions.assertEquals(1, reversed._6);
        Assertions.assertEquals("a", reversed._7);
    }

    @Test
    public void testTuple7_ForEach() throws Exception {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f);
        List<Object> collected = new ArrayList<>();
        t7.forEach(collected::add);
        Assertions.assertEquals(7, collected.size());
    }

    @Test
    public void testTuple7_HashCode() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertEquals(t7.hashCode(), t7Same.hashCode());
    }

    @Test
    public void testTuple7_Equals() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Different = Tuple.of("test", 42, true, 3.14, 100L, 'x', 3.0f);

        Assertions.assertTrue(t7.equals(t7));
        Assertions.assertTrue(t7.equals(t7Same));
        Assertions.assertFalse(t7.equals(t7Different));
    }

    @Test
    public void testTuple7_ToString() {
        Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f);
        Assertions.assertEquals("(test, 42, true, 3.14, 100, x, 2.5)", t7.toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_Arity() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertEquals(8, t8.arity());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_AnyNull() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertFalse(t8.anyNull());

        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8WithNull = Tuple.of(null, 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertTrue(t8WithNull.anyNull());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_AllNull() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertFalse(t8.allNull());

        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8AllNull = Tuple.of(null, null, null, null, null, null, null, null);
        Assertions.assertTrue(t8AllNull.allNull());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_Contains() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertTrue(t8.contains("test"));
        Assertions.assertTrue(t8.contains((byte) 10));
        Assertions.assertFalse(t8.contains("other"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_ToArray() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Object[] array = t8.toArray();
        Assertions.assertEquals(8, array.length);
        Assertions.assertEquals((byte) 10, array[7]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_ToArrayTyped() {
        Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t8 = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8);
        Integer[] result = t8.toArray(new Integer[0]);
        Assertions.assertEquals(8, result.length);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_Reverse() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5);
        Tuple8<Byte, Float, Character, Long, Double, Boolean, Integer, String> reversed = t8.reverse();
        Assertions.assertEquals((byte) 5, reversed._1);
        Assertions.assertEquals(4.0f, reversed._2);
        Assertions.assertEquals('x', reversed._3);
        Assertions.assertEquals(3L, reversed._4);
        Assertions.assertEquals(2.0, reversed._5);
        Assertions.assertEquals(true, reversed._6);
        Assertions.assertEquals(1, reversed._7);
        Assertions.assertEquals("a", reversed._8);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_ForEach() throws Exception {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5);
        List<Object> collected = new ArrayList<>();
        t8.forEach(collected::add);
        Assertions.assertEquals(8, collected.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_HashCode() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertEquals(t8.hashCode(), t8Same.hashCode());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_Equals() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Different = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 20);

        Assertions.assertTrue(t8.equals(t8));
        Assertions.assertTrue(t8.equals(t8Same));
        Assertions.assertFalse(t8.equals(t8Different));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple8_ToString() {
        Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10);
        Assertions.assertEquals("(test, 42, true, 3.14, 100, x, 2.5, 10)", t8.toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_Arity() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertEquals(9, t9.arity());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_AnyNull() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertFalse(t9.anyNull());

        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9WithNull = Tuple.of(null, 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertTrue(t9WithNull.anyNull());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_AllNull() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertFalse(t9.allNull());

        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9AllNull = Tuple.of(null, null, null, null, null, null, null, null,
                null);
        Assertions.assertTrue(t9AllNull.allNull());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_Contains() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertTrue(t9.contains("test"));
        Assertions.assertTrue(t9.contains((short) 20));
        Assertions.assertFalse(t9.contains("other"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_ToArray() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Object[] array = t9.toArray();
        Assertions.assertEquals(9, array.length);
        Assertions.assertEquals((short) 20, array[8]);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_ToArrayTyped() {
        Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> t9 = Tuple.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Integer[] result = t9.toArray(new Integer[0]);
        Assertions.assertEquals(9, result.length);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_Reverse() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5, (short) 6);
        Tuple9<Short, Byte, Float, Character, Long, Double, Boolean, Integer, String> reversed = t9.reverse();
        Assertions.assertEquals((short) 6, reversed._1);
        Assertions.assertEquals((byte) 5, reversed._2);
        Assertions.assertEquals(4.0f, reversed._3);
        Assertions.assertEquals('x', reversed._4);
        Assertions.assertEquals(3L, reversed._5);
        Assertions.assertEquals(2.0, reversed._6);
        Assertions.assertEquals(true, reversed._7);
        Assertions.assertEquals(1, reversed._8);
        Assertions.assertEquals("a", reversed._9);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_ForEach() throws Exception {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5, (short) 6);
        List<Object> collected = new ArrayList<>();
        t9.forEach(collected::add);
        Assertions.assertEquals(9, collected.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_HashCode() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertEquals(t9.hashCode(), t9Same.hashCode());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_Equals() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Same = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Different = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 30);

        Assertions.assertTrue(t9.equals(t9));
        Assertions.assertTrue(t9.equals(t9Same));
        Assertions.assertFalse(t9.equals(t9Different));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTuple9_ToString() {
        Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 42, true, 3.14, 100L, 'x', 2.5f, (byte) 10,
                (short) 20);
        Assertions.assertEquals("(test, 42, true, 3.14, 100, x, 2.5, 10, 20)", t9.toString());
    }
}
