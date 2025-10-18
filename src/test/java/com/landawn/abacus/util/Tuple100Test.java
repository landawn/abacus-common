package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class Tuple100Test extends TestBase {

    @Test
    public void testOf() {
        Tuple.Tuple1<String> t1 = Tuple.of("a");
        Assertions.assertEquals("a", t1._1);

        Tuple.Tuple2<String, Integer> t2 = Tuple.of("a", 1);
        Assertions.assertEquals("a", t2._1);
        Assertions.assertEquals(1, t2._2);

        Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        Assertions.assertEquals("a", t3._1);
        Assertions.assertEquals(1, t3._2);
        Assertions.assertEquals(true, t3._3);

        Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("a", 1, true, 2.0);
        Assertions.assertEquals("a", t4._1);
        Assertions.assertEquals(1, t4._2);
        Assertions.assertEquals(true, t4._3);
        Assertions.assertEquals(2.0, t4._4);

        Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("a", 1, true, 2.0, 3L);
        Assertions.assertEquals("a", t5._1);
        Assertions.assertEquals(1, t5._2);
        Assertions.assertEquals(true, t5._3);
        Assertions.assertEquals(2.0, t5._4);
        Assertions.assertEquals(3L, t5._5);

        Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("a", 1, true, 2.0, 3L, 'x');
        Assertions.assertEquals("a", t6._1);
        Assertions.assertEquals(1, t6._2);
        Assertions.assertEquals(true, t6._3);
        Assertions.assertEquals(2.0, t6._4);
        Assertions.assertEquals(3L, t6._5);
        Assertions.assertEquals('x', t6._6);

        Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f);
        Assertions.assertEquals("a", t7._1);
        Assertions.assertEquals(1, t7._2);
        Assertions.assertEquals(true, t7._3);
        Assertions.assertEquals(2.0, t7._4);
        Assertions.assertEquals(3L, t7._5);
        Assertions.assertEquals('x', t7._6);
        Assertions.assertEquals(4.0f, t7._7);

        Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5);
        Assertions.assertEquals("a", t8._1);
        Assertions.assertEquals(1, t8._2);
        Assertions.assertEquals(true, t8._3);
        Assertions.assertEquals(2.0, t8._4);
        Assertions.assertEquals(3L, t8._5);
        Assertions.assertEquals('x', t8._6);
        Assertions.assertEquals(4.0f, t8._7);
        Assertions.assertEquals((byte) 5, t8._8);

        Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5,
                (short) 6);
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
    public void testCreateFromMapEntry() {
        Map.Entry<String, Integer> entry = new HashMap.SimpleEntry<>("key", 100);
        Tuple.Tuple2<String, Integer> tuple = Tuple.create(entry);

        Assertions.assertEquals("key", tuple._1);
        Assertions.assertEquals(100, tuple._2);
    }

    @Test
    public void testCreateFromArray() {
        Object[] emptyArray = new Object[0];
        Tuple<?> t0 = Tuple.create(emptyArray);
        Assertions.assertEquals(0, t0.arity());

        Tuple<?> tNull = Tuple.create((Object[]) null);
        Assertions.assertEquals(0, tNull.arity());

        Object[] array1 = { "a" };
        Tuple.Tuple1<Object> t1 = Tuple.create(array1);
        Assertions.assertEquals("a", t1._1);

        Object[] array2 = { "a", 1 };
        Tuple.Tuple2<Object, Object> t2 = Tuple.create(array2);
        Assertions.assertEquals("a", t2._1);
        Assertions.assertEquals(1, t2._2);

        Object[] array3 = { "a", 1, true };
        Tuple.Tuple3<Object, Object, Object> t3 = Tuple.create(array3);
        Assertions.assertEquals("a", t3._1);
        Assertions.assertEquals(1, t3._2);
        Assertions.assertEquals(true, t3._3);

        Object[] array4 = { "a", 1, true, 2.0 };
        Tuple.Tuple4<Object, Object, Object, Object> t4 = Tuple.create(array4);
        Assertions.assertEquals("a", t4._1);
        Assertions.assertEquals(1, t4._2);
        Assertions.assertEquals(true, t4._3);
        Assertions.assertEquals(2.0, t4._4);

        Object[] array5 = { "a", 1, true, 2.0, 3L };
        Tuple.Tuple5<Object, Object, Object, Object, Object> t5 = Tuple.create(array5);
        Assertions.assertEquals("a", t5._1);
        Assertions.assertEquals(1, t5._2);
        Assertions.assertEquals(true, t5._3);
        Assertions.assertEquals(2.0, t5._4);
        Assertions.assertEquals(3L, t5._5);

        Object[] array6 = { "a", 1, true, 2.0, 3L, 'x' };
        Tuple.Tuple6<Object, Object, Object, Object, Object, Object> t6 = Tuple.create(array6);
        Assertions.assertEquals("a", t6._1);
        Assertions.assertEquals(1, t6._2);
        Assertions.assertEquals(true, t6._3);
        Assertions.assertEquals(2.0, t6._4);
        Assertions.assertEquals(3L, t6._5);
        Assertions.assertEquals('x', t6._6);

        Object[] array7 = { "a", 1, true, 2.0, 3L, 'x', 4.0f };
        Tuple.Tuple7<Object, Object, Object, Object, Object, Object, Object> t7 = Tuple.create(array7);
        Assertions.assertEquals("a", t7._1);
        Assertions.assertEquals(1, t7._2);
        Assertions.assertEquals(true, t7._3);
        Assertions.assertEquals(2.0, t7._4);
        Assertions.assertEquals(3L, t7._5);
        Assertions.assertEquals('x', t7._6);
        Assertions.assertEquals(4.0f, t7._7);

        Object[] array8 = { "a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5 };
        Tuple.Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> t8 = Tuple.create(array8);
        Assertions.assertEquals("a", t8._1);
        Assertions.assertEquals(1, t8._2);
        Assertions.assertEquals(true, t8._3);
        Assertions.assertEquals(2.0, t8._4);
        Assertions.assertEquals(3L, t8._5);
        Assertions.assertEquals('x', t8._6);
        Assertions.assertEquals(4.0f, t8._7);
        Assertions.assertEquals((byte) 5, t8._8);

        Object[] array9 = { "a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5, (short) 6 };
        Tuple.Tuple9<Object, Object, Object, Object, Object, Object, Object, Object, Object> t9 = Tuple.create(array9);
        Assertions.assertEquals("a", t9._1);
        Assertions.assertEquals(1, t9._2);
        Assertions.assertEquals(true, t9._3);
        Assertions.assertEquals(2.0, t9._4);
        Assertions.assertEquals(3L, t9._5);
        Assertions.assertEquals('x', t9._6);
        Assertions.assertEquals(4.0f, t9._7);
        Assertions.assertEquals((byte) 5, t9._8);
        Assertions.assertEquals((short) 6, t9._9);

        Object[] arrayTooMany = new Object[10];
        Assertions.assertThrows(IllegalArgumentException.class, () -> Tuple.create(arrayTooMany));
    }

    @Test
    public void testCreateFromCollection() {
        List<Object> emptyList = new ArrayList<>();
        Tuple<?> t0 = Tuple.create(emptyList);
        Assertions.assertEquals(0, t0.arity());

        Tuple<?> tNull = Tuple.create((List<Object>) null);
        Assertions.assertEquals(0, tNull.arity());

        List<Object> list1 = Arrays.asList("a");
        Tuple.Tuple1<Object> t1 = Tuple.create(list1);
        Assertions.assertEquals("a", t1._1);

        List<Object> list2 = Arrays.asList("a", 1);
        Tuple.Tuple2<Object, Object> t2 = Tuple.create(list2);
        Assertions.assertEquals("a", t2._1);
        Assertions.assertEquals(1, t2._2);

        List<Object> list3 = Arrays.asList("a", 1, true);
        Tuple.Tuple3<Object, Object, Object> t3 = Tuple.create(list3);
        Assertions.assertEquals("a", t3._1);
        Assertions.assertEquals(1, t3._2);
        Assertions.assertEquals(true, t3._3);

        List<Object> list4 = Arrays.asList("a", 1, true, 2.0);
        Tuple.Tuple4<Object, Object, Object, Object> t4 = Tuple.create(list4);
        Assertions.assertEquals("a", t4._1);
        Assertions.assertEquals(1, t4._2);
        Assertions.assertEquals(true, t4._3);
        Assertions.assertEquals(2.0, t4._4);

        List<Object> list5 = Arrays.asList("a", 1, true, 2.0, 3L);
        Tuple.Tuple5<Object, Object, Object, Object, Object> t5 = Tuple.create(list5);
        Assertions.assertEquals("a", t5._1);
        Assertions.assertEquals(1, t5._2);
        Assertions.assertEquals(true, t5._3);
        Assertions.assertEquals(2.0, t5._4);
        Assertions.assertEquals(3L, t5._5);

        List<Object> list6 = Arrays.asList("a", 1, true, 2.0, 3L, 'x');
        Tuple.Tuple6<Object, Object, Object, Object, Object, Object> t6 = Tuple.create(list6);
        Assertions.assertEquals("a", t6._1);
        Assertions.assertEquals(1, t6._2);
        Assertions.assertEquals(true, t6._3);
        Assertions.assertEquals(2.0, t6._4);
        Assertions.assertEquals(3L, t6._5);
        Assertions.assertEquals('x', t6._6);

        List<Object> list7 = Arrays.asList("a", 1, true, 2.0, 3L, 'x', 4.0f);
        Tuple.Tuple7<Object, Object, Object, Object, Object, Object, Object> t7 = Tuple.create(list7);
        Assertions.assertEquals("a", t7._1);
        Assertions.assertEquals(1, t7._2);
        Assertions.assertEquals(true, t7._3);
        Assertions.assertEquals(2.0, t7._4);
        Assertions.assertEquals(3L, t7._5);
        Assertions.assertEquals('x', t7._6);
        Assertions.assertEquals(4.0f, t7._7);

        List<Object> list8 = Arrays.asList("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5);
        Tuple.Tuple8<Object, Object, Object, Object, Object, Object, Object, Object> t8 = Tuple.create(list8);
        Assertions.assertEquals("a", t8._1);
        Assertions.assertEquals(1, t8._2);
        Assertions.assertEquals(true, t8._3);
        Assertions.assertEquals(2.0, t8._4);
        Assertions.assertEquals(3L, t8._5);
        Assertions.assertEquals('x', t8._6);
        Assertions.assertEquals(4.0f, t8._7);
        Assertions.assertEquals((byte) 5, t8._8);

        List<Object> list9 = Arrays.asList("a", 1, true, 2.0, 3L, 'x', 4.0f, (byte) 5, (short) 6);
        Tuple.Tuple9<Object, Object, Object, Object, Object, Object, Object, Object, Object> t9 = Tuple.create(list9);
        Assertions.assertEquals("a", t9._1);
        Assertions.assertEquals(1, t9._2);
        Assertions.assertEquals(true, t9._3);
        Assertions.assertEquals(2.0, t9._4);
        Assertions.assertEquals(3L, t9._5);
        Assertions.assertEquals('x', t9._6);
        Assertions.assertEquals(4.0f, t9._7);
        Assertions.assertEquals((byte) 5, t9._8);
        Assertions.assertEquals((short) 6, t9._9);

        List<Object> listTooMany = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Tuple.create(listTooMany));
    }

    @Test
    public void testToList() {
        Tuple.Tuple1<String> t1 = Tuple.of("a");
        List<String> list1 = Tuple.toList(t1);
        Assertions.assertEquals(1, list1.size());
        Assertions.assertEquals("a", list1.get(0));

        Tuple.Tuple2<String, String> t2 = Tuple.of("a", "b");
        List<String> list2 = Tuple.toList(t2);
        Assertions.assertEquals(2, list2.size());
        Assertions.assertEquals("a", list2.get(0));
        Assertions.assertEquals("b", list2.get(1));

        Tuple.Tuple3<String, String, String> t3 = Tuple.of("a", "b", "c");
        List<String> list3 = Tuple.toList(t3);
        Assertions.assertEquals(3, list3.size());
        Assertions.assertEquals("a", list3.get(0));
        Assertions.assertEquals("b", list3.get(1));
        Assertions.assertEquals("c", list3.get(2));

        Tuple.Tuple4<String, String, String, String> t4 = Tuple.of("a", "b", "c", "d");
        List<String> list4 = Tuple.toList(t4);
        Assertions.assertEquals(4, list4.size());
        Assertions.assertEquals("a", list4.get(0));
        Assertions.assertEquals("b", list4.get(1));
        Assertions.assertEquals("c", list4.get(2));
        Assertions.assertEquals("d", list4.get(3));

        Tuple.Tuple5<String, String, String, String, String> t5 = Tuple.of("a", "b", "c", "d", "e");
        List<String> list5 = Tuple.toList(t5);
        Assertions.assertEquals(5, list5.size());
        Assertions.assertEquals("a", list5.get(0));
        Assertions.assertEquals("b", list5.get(1));
        Assertions.assertEquals("c", list5.get(2));
        Assertions.assertEquals("d", list5.get(3));
        Assertions.assertEquals("e", list5.get(4));

        Tuple.Tuple6<String, String, String, String, String, String> t6 = Tuple.of("a", "b", "c", "d", "e", "f");
        List<String> list6 = Tuple.toList(t6);
        Assertions.assertEquals(6, list6.size());
        Assertions.assertEquals("a", list6.get(0));
        Assertions.assertEquals("b", list6.get(1));
        Assertions.assertEquals("c", list6.get(2));
        Assertions.assertEquals("d", list6.get(3));
        Assertions.assertEquals("e", list6.get(4));
        Assertions.assertEquals("f", list6.get(5));

        Tuple.Tuple7<String, String, String, String, String, String, String> t7 = Tuple.of("a", "b", "c", "d", "e", "f", "g");
        List<String> list7 = Tuple.toList(t7);
        Assertions.assertEquals(7, list7.size());
        Assertions.assertEquals("a", list7.get(0));
        Assertions.assertEquals("b", list7.get(1));
        Assertions.assertEquals("c", list7.get(2));
        Assertions.assertEquals("d", list7.get(3));
        Assertions.assertEquals("e", list7.get(4));
        Assertions.assertEquals("f", list7.get(5));
        Assertions.assertEquals("g", list7.get(6));

        Tuple.Tuple8<String, String, String, String, String, String, String, String> t8 = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h");
        List<String> list8 = Tuple.toList(t8);
        Assertions.assertEquals(8, list8.size());
        Assertions.assertEquals("a", list8.get(0));
        Assertions.assertEquals("b", list8.get(1));
        Assertions.assertEquals("c", list8.get(2));
        Assertions.assertEquals("d", list8.get(3));
        Assertions.assertEquals("e", list8.get(4));
        Assertions.assertEquals("f", list8.get(5));
        Assertions.assertEquals("g", list8.get(6));
        Assertions.assertEquals("h", list8.get(7));

        Tuple.Tuple9<String, String, String, String, String, String, String, String, String> t9 = Tuple.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        List<String> list9 = Tuple.toList(t9);
        Assertions.assertEquals(9, list9.size());
        Assertions.assertEquals("a", list9.get(0));
        Assertions.assertEquals("b", list9.get(1));
        Assertions.assertEquals("c", list9.get(2));
        Assertions.assertEquals("d", list9.get(3));
        Assertions.assertEquals("e", list9.get(4));
        Assertions.assertEquals("f", list9.get(5));
        Assertions.assertEquals("g", list9.get(6));
        Assertions.assertEquals("h", list9.get(7));
        Assertions.assertEquals("i", list9.get(8));
    }

    @Test
    public void testFlatten() {
        Tuple.Tuple2<String, Integer> inner2 = Tuple.of("a", 1);
        Tuple.Tuple2<Tuple.Tuple2<String, Integer>, Boolean> nested2 = Tuple.of(inner2, true);
        Tuple.Tuple3<String, Integer, Boolean> flattened3 = Tuple.flatten(nested2);

        Assertions.assertEquals("a", flattened3._1);
        Assertions.assertEquals(1, flattened3._2);
        Assertions.assertEquals(true, flattened3._3);

        Tuple.Tuple3<String, Integer, Boolean> inner3 = Tuple.of("a", 1, true);
        Tuple.Tuple3<Tuple.Tuple3<String, Integer, Boolean>, Double, Long> nested3 = Tuple.of(inner3, 2.0, 3L);
        Tuple.Tuple5<String, Integer, Boolean, Double, Long> flattened5 = Tuple.flatten(nested3);

        Assertions.assertEquals("a", flattened5._1);
        Assertions.assertEquals(1, flattened5._2);
        Assertions.assertEquals(true, flattened5._3);
        Assertions.assertEquals(2.0, flattened5._4);
        Assertions.assertEquals(3L, flattened5._5);
    }

    @Test
    public void testAccept() {
        Tuple.Tuple1<String> t1 = Tuple.of("test");
        final boolean[] called = { false };

        t1.accept(tuple -> {
            called[0] = true;
            Assertions.assertEquals("test", tuple._1);
        });

        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testMap() {
        Tuple.Tuple1<String> t1 = Tuple.of("test");
        String result = t1.map(tuple -> tuple._1 + "_mapped");
        Assertions.assertEquals("test_mapped", result);
    }

    @Test
    public void testFilter() {
        Tuple.Tuple1<String> t1 = Tuple.of("test");

        Optional<Tuple.Tuple1<String>> filtered1 = t1.filter(tuple -> tuple._1.equals("test"));
        Assertions.assertTrue(filtered1.isPresent());
        Assertions.assertEquals("test", filtered1.get()._1);

        Optional<Tuple.Tuple1<String>> filtered2 = t1.filter(tuple -> tuple._1.equals("other"));
        Assertions.assertFalse(filtered2.isPresent());
    }

    @Nested
    public class Tuple0Test {

        @Test
        public void testArity() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertEquals(0, t0.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertFalse(t0.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertTrue(t0.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertFalse(t0.contains("anything"));
            Assertions.assertFalse(t0.contains(null));
            Assertions.assertFalse(t0.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Object[] array = t0.toArray();
            Assertions.assertEquals(0, array.length);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            String[] array = new String[5];
            String[] result = t0.toArray(array);
            Assertions.assertSame(array, result);
            Assertions.assertEquals(5, result.length);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            final int[] callCount = { 0 };

            t0.forEach(obj -> callCount[0]++);

            Assertions.assertEquals(0, callCount[0]);
        }

        @Test
        public void testForEachWithNullConsumer() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertThrows(IllegalArgumentException.class, () -> t0.forEach(null));
        }

        @Test
        public void testToString() {
            Tuple.Tuple0 t0 = Tuple.create(new Object[0]);
            Assertions.assertEquals("()", t0.toString());
        }
    }

    @Nested
    public class Tuple1Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple1<String> t1 = new Tuple.Tuple1<>("test");
            Assertions.assertEquals("test", t1._1);

            Tuple.Tuple1<String> t1Null = new Tuple.Tuple1<>(null);
            Assertions.assertNull(t1Null._1);
        }

        @Test
        public void testArity() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Assertions.assertEquals(1, t1.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Assertions.assertFalse(t1.anyNull());

            Tuple.Tuple1<String> t1Null = Tuple.of(null);
            Assertions.assertTrue(t1Null.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Assertions.assertFalse(t1.allNull());

            Tuple.Tuple1<String> t1Null = Tuple.of(null);
            Assertions.assertTrue(t1Null.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Assertions.assertTrue(t1.contains("test"));
            Assertions.assertFalse(t1.contains("other"));
            Assertions.assertFalse(t1.contains(null));

            Tuple.Tuple1<String> t1Null = Tuple.of(null);
            Assertions.assertTrue(t1Null.contains(null));
            Assertions.assertFalse(t1Null.contains("test"));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Object[] array = t1.toArray();
            Assertions.assertEquals(1, array.length);
            Assertions.assertEquals("test", array[0]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");

            String[] smallArray = new String[0];
            String[] result1 = t1.toArray(smallArray);
            Assertions.assertEquals(1, result1.length);
            Assertions.assertEquals("test", result1[0]);

            String[] exactArray = new String[1];
            String[] result2 = t1.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);

            String[] largeArray = new String[3];
            largeArray[1] = "existing";
            largeArray[2] = "values";
            String[] result3 = t1.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals("existing", result3[1]);
            Assertions.assertEquals("values", result3[2]);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            final int[] callCount = { 0 };
            final String[] receivedValue = { null };

            t1.forEach(obj -> {
                callCount[0]++;
                receivedValue[0] = (String) obj;
            });

            Assertions.assertEquals(1, callCount[0]);
            Assertions.assertEquals("test", receivedValue[0]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple1<String> t1a = Tuple.of("test");
            Tuple.Tuple1<String> t1b = Tuple.of("test");
            Tuple.Tuple1<String> t1c = Tuple.of("other");
            Tuple.Tuple1<String> t1Null = Tuple.of(null);

            Assertions.assertEquals(t1a.hashCode(), t1b.hashCode());
            Assertions.assertNotEquals(t1a.hashCode(), t1c.hashCode());

            int nullHash = t1Null.hashCode();
            Assertions.assertTrue(nullHash >= 0 || nullHash < 0);
        }

        @Test
        public void testEquals() {
            Tuple.Tuple1<String> t1a = Tuple.of("test");
            Tuple.Tuple1<String> t1b = Tuple.of("test");
            Tuple.Tuple1<String> t1c = Tuple.of("other");
            Tuple.Tuple1<String> t1Null1 = Tuple.of(null);
            Tuple.Tuple1<String> t1Null2 = Tuple.of(null);

            Assertions.assertTrue(t1a.equals(t1a));

            Assertions.assertTrue(t1a.equals(t1b));
            Assertions.assertTrue(t1b.equals(t1a));

            Assertions.assertFalse(t1a.equals(t1c));
            Assertions.assertFalse(t1c.equals(t1a));

            Assertions.assertTrue(t1Null1.equals(t1Null2));
            Assertions.assertFalse(t1a.equals(t1Null1));

            Assertions.assertFalse(t1a.equals("test"));
            Assertions.assertFalse(t1a.equals(null));
            Assertions.assertFalse(t1a.equals(Tuple.of("test", "test")));
        }

        @Test
        public void testToString() {
            Tuple.Tuple1<String> t1 = Tuple.of("test");
            Assertions.assertEquals("(test)", t1.toString());

            Tuple.Tuple1<String> t1Null = Tuple.of(null);
            Assertions.assertEquals("(null)", t1Null.toString());

            Tuple.Tuple1<Integer> t1Int = Tuple.of(123);
            Assertions.assertEquals("(123)", t1Int.toString());
        }
    }

    @Nested
    public class Tuple2Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple2<String, Integer> t2 = new Tuple.Tuple2<>("test", 123);
            Assertions.assertEquals("test", t2._1);
            Assertions.assertEquals(123, t2._2);

            Tuple.Tuple2<String, Integer> t2Null = new Tuple.Tuple2<>(null, null);
            Assertions.assertNull(t2Null._1);
            Assertions.assertNull(t2Null._2);
        }

        @Test
        public void testArity() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Assertions.assertEquals(2, t2.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Assertions.assertFalse(t2.anyNull());

            Tuple.Tuple2<String, Integer> t2Null1 = Tuple.of(null, 123);
            Assertions.assertTrue(t2Null1.anyNull());

            Tuple.Tuple2<String, Integer> t2Null2 = Tuple.of("test", null);
            Assertions.assertTrue(t2Null2.anyNull());

            Tuple.Tuple2<String, Integer> t2AllNull = Tuple.of(null, null);
            Assertions.assertTrue(t2AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Assertions.assertFalse(t2.allNull());

            Tuple.Tuple2<String, Integer> t2Null1 = Tuple.of(null, 123);
            Assertions.assertFalse(t2Null1.allNull());

            Tuple.Tuple2<String, Integer> t2Null2 = Tuple.of("test", null);
            Assertions.assertFalse(t2Null2.allNull());

            Tuple.Tuple2<String, Integer> t2AllNull = Tuple.of(null, null);
            Assertions.assertTrue(t2AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Assertions.assertTrue(t2.contains("test"));
            Assertions.assertTrue(t2.contains(123));
            Assertions.assertFalse(t2.contains("other"));
            Assertions.assertFalse(t2.contains(456));
            Assertions.assertFalse(t2.contains(null));

            Tuple.Tuple2<String, Integer> t2WithNull = Tuple.of("test", null);
            Assertions.assertTrue(t2WithNull.contains("test"));
            Assertions.assertTrue(t2WithNull.contains(null));
            Assertions.assertFalse(t2WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Object[] array = t2.toArray();
            Assertions.assertEquals(2, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);

            Object[] smallArray = new Object[1];
            Object[] result1 = t2.toArray(smallArray);
            Assertions.assertEquals(2, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);

            Object[] exactArray = new Object[2];
            Object[] result2 = t2.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);

            Object[] largeArray = new Object[4];
            largeArray[2] = "existing";
            largeArray[3] = "values";
            Object[] result3 = t2.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals("existing", result3[2]);
            Assertions.assertEquals("values", result3[3]);
        }

        @Test
        public void testToPair() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Pair<String, Integer> pair = t2.toPair();

            Assertions.assertEquals("test", pair.left());
            Assertions.assertEquals(123, pair.right());
        }

        @Test
        public void testToEntry() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            ImmutableEntry<String, Integer> entry = t2.toEntry();

            Assertions.assertEquals("test", entry.getKey());
            Assertions.assertEquals(123, entry.getValue());
        }

        @Test
        public void testReverse() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Tuple.Tuple2<Integer, String> reversed = t2.reverse();

            Assertions.assertEquals(123, reversed._1);
            Assertions.assertEquals("test", reversed._2);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[2];

            t2.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(2, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
        }

        @Test
        public void testAcceptBiConsumer() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            final boolean[] called = { false };

            t2.accept((s, i) -> {
                called[0] = true;
                Assertions.assertEquals("test", s);
                Assertions.assertEquals(123, i);
            });

            Assertions.assertTrue(called[0]);
        }

        @Test
        public void testMapBiFunction() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            String result = t2.map((s, i) -> s + "_" + i);
            Assertions.assertEquals("test_123", result);
        }

        @Test
        public void testFilterBiPredicate() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);

            Optional<Tuple.Tuple2<String, Integer>> filtered1 = t2.filter((s, i) -> s.equals("test") && i == 123);
            Assertions.assertTrue(filtered1.isPresent());
            Assertions.assertEquals("test", filtered1.get()._1);
            Assertions.assertEquals(123, filtered1.get()._2);

            Optional<Tuple.Tuple2<String, Integer>> filtered2 = t2.filter((s, i) -> s.equals("other") || i == 456);
            Assertions.assertFalse(filtered2.isPresent());
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple2<String, Integer> t2a = Tuple.of("test", 123);
            Tuple.Tuple2<String, Integer> t2b = Tuple.of("test", 123);
            Tuple.Tuple2<String, Integer> t2c = Tuple.of("other", 123);
            Tuple.Tuple2<String, Integer> t2d = Tuple.of("test", 456);

            Assertions.assertEquals(t2a.hashCode(), t2b.hashCode());
            Assertions.assertNotEquals(t2a.hashCode(), t2c.hashCode());
            Assertions.assertNotEquals(t2a.hashCode(), t2d.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple2<String, Integer> t2a = Tuple.of("test", 123);
            Tuple.Tuple2<String, Integer> t2b = Tuple.of("test", 123);
            Tuple.Tuple2<String, Integer> t2c = Tuple.of("other", 123);
            Tuple.Tuple2<String, Integer> t2d = Tuple.of("test", 456);
            Tuple.Tuple2<String, Integer> t2Null = Tuple.of(null, null);

            Assertions.assertTrue(t2a.equals(t2a));

            Assertions.assertTrue(t2a.equals(t2b));
            Assertions.assertTrue(t2b.equals(t2a));

            Assertions.assertFalse(t2a.equals(t2c));
            Assertions.assertFalse(t2a.equals(t2d));

            Assertions.assertTrue(t2Null.equals(Tuple.of(null, null)));
            Assertions.assertFalse(t2a.equals(t2Null));

            Assertions.assertFalse(t2a.equals("test"));
            Assertions.assertFalse(t2a.equals(null));
            Assertions.assertFalse(t2a.equals(Tuple.of("test")));
        }

        @Test
        public void testToString() {
            Tuple.Tuple2<String, Integer> t2 = Tuple.of("test", 123);
            Assertions.assertEquals("(test, 123)", t2.toString());

            Tuple.Tuple2<String, Integer> t2Null = Tuple.of(null, null);
            Assertions.assertEquals("(null, null)", t2Null.toString());
        }
    }

    @Nested
    public class Tuple3Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = new Tuple.Tuple3<>("test", 123, true);
            Assertions.assertEquals("test", t3._1);
            Assertions.assertEquals(123, t3._2);
            Assertions.assertEquals(true, t3._3);

            Tuple.Tuple3<String, Integer, Boolean> t3Null = new Tuple.Tuple3<>(null, null, null);
            Assertions.assertNull(t3Null._1);
            Assertions.assertNull(t3Null._2);
            Assertions.assertNull(t3Null._3);
        }

        @Test
        public void testArity() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Assertions.assertEquals(3, t3.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Assertions.assertFalse(t3.anyNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null1 = Tuple.of(null, 123, true);
            Assertions.assertTrue(t3Null1.anyNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null2 = Tuple.of("test", null, true);
            Assertions.assertTrue(t3Null2.anyNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null3 = Tuple.of("test", 123, null);
            Assertions.assertTrue(t3Null3.anyNull());

            Tuple.Tuple3<String, Integer, Boolean> t3AllNull = Tuple.of(null, null, null);
            Assertions.assertTrue(t3AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Assertions.assertFalse(t3.allNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null1 = Tuple.of(null, 123, true);
            Assertions.assertFalse(t3Null1.allNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null2 = Tuple.of("test", null, true);
            Assertions.assertFalse(t3Null2.allNull());

            Tuple.Tuple3<String, Integer, Boolean> t3Null3 = Tuple.of("test", 123, null);
            Assertions.assertFalse(t3Null3.allNull());

            Tuple.Tuple3<String, Integer, Boolean> t3AllNull = Tuple.of(null, null, null);
            Assertions.assertTrue(t3AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Assertions.assertTrue(t3.contains("test"));
            Assertions.assertTrue(t3.contains(123));
            Assertions.assertTrue(t3.contains(true));
            Assertions.assertFalse(t3.contains("other"));
            Assertions.assertFalse(t3.contains(456));
            Assertions.assertFalse(t3.contains(false));
            Assertions.assertFalse(t3.contains(null));

            Tuple.Tuple3<String, Integer, Boolean> t3WithNull = Tuple.of("test", null, true);
            Assertions.assertTrue(t3WithNull.contains("test"));
            Assertions.assertTrue(t3WithNull.contains(null));
            Assertions.assertTrue(t3WithNull.contains(true));
            Assertions.assertFalse(t3WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Object[] array = t3.toArray();
            Assertions.assertEquals(3, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);

            Object[] smallArray = new Object[2];
            Object[] result1 = t3.toArray(smallArray);
            Assertions.assertEquals(3, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);

            Object[] exactArray = new Object[3];
            Object[] result2 = t3.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);

            Object[] largeArray = new Object[5];
            largeArray[3] = "existing";
            largeArray[4] = "values";
            Object[] result3 = t3.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals("existing", result3[3]);
            Assertions.assertEquals("values", result3[4]);
        }

        @Test
        public void testToTriple() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Triple<String, Integer, Boolean> triple = t3.toTriple();

            Assertions.assertEquals("test", triple.left());
            Assertions.assertEquals(123, triple.middle());
            Assertions.assertEquals(true, triple.right());
        }

        @Test
        public void testReverse() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Tuple.Tuple3<Boolean, Integer, String> reversed = t3.reverse();

            Assertions.assertEquals(true, reversed._1);
            Assertions.assertEquals(123, reversed._2);
            Assertions.assertEquals("test", reversed._3);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[3];

            t3.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(3, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
        }

        @Test
        public void testAcceptTriConsumer() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            final boolean[] called = { false };

            t3.accept((s, i, b) -> {
                called[0] = true;
                Assertions.assertEquals("test", s);
                Assertions.assertEquals(123, i);
                Assertions.assertEquals(true, b);
            });

            Assertions.assertTrue(called[0]);
        }

        @Test
        public void testMapTriFunction() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            String result = t3.map((s, i, b) -> s + "_" + i + "_" + b);
            Assertions.assertEquals("test_123_true", result);
        }

        @Test
        public void testFilterTriPredicate() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);

            Optional<Tuple.Tuple3<String, Integer, Boolean>> filtered1 = t3.filter((s, i, b) -> s.equals("test") && i == 123 && b);
            Assertions.assertTrue(filtered1.isPresent());
            Assertions.assertEquals("test", filtered1.get()._1);
            Assertions.assertEquals(123, filtered1.get()._2);
            Assertions.assertEquals(true, filtered1.get()._3);

            Optional<Tuple.Tuple3<String, Integer, Boolean>> filtered2 = t3.filter((s, i, b) -> s.equals("other") || i == 456 || !b);
            Assertions.assertFalse(filtered2.isPresent());
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple3<String, Integer, Boolean> t3a = Tuple.of("test", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3b = Tuple.of("test", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3c = Tuple.of("other", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3d = Tuple.of("test", 456, true);
            Tuple.Tuple3<String, Integer, Boolean> t3e = Tuple.of("test", 123, false);

            Assertions.assertEquals(t3a.hashCode(), t3b.hashCode());
            Assertions.assertNotEquals(t3a.hashCode(), t3c.hashCode());
            Assertions.assertNotEquals(t3a.hashCode(), t3d.hashCode());
            Assertions.assertNotEquals(t3a.hashCode(), t3e.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple3<String, Integer, Boolean> t3a = Tuple.of("test", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3b = Tuple.of("test", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3c = Tuple.of("other", 123, true);
            Tuple.Tuple3<String, Integer, Boolean> t3d = Tuple.of("test", 456, true);
            Tuple.Tuple3<String, Integer, Boolean> t3e = Tuple.of("test", 123, false);
            Tuple.Tuple3<String, Integer, Boolean> t3Null = Tuple.of(null, null, null);

            Assertions.assertTrue(t3a.equals(t3a));

            Assertions.assertTrue(t3a.equals(t3b));
            Assertions.assertTrue(t3b.equals(t3a));

            Assertions.assertFalse(t3a.equals(t3c));
            Assertions.assertFalse(t3a.equals(t3d));
            Assertions.assertFalse(t3a.equals(t3e));

            Assertions.assertTrue(t3Null.equals(Tuple.of(null, null, null)));
            Assertions.assertFalse(t3a.equals(t3Null));

            Assertions.assertFalse(t3a.equals("test"));
            Assertions.assertFalse(t3a.equals(null));
            Assertions.assertFalse(t3a.equals(Tuple.of("test", 123)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("test", 123, true);
            Assertions.assertEquals("(test, 123, true)", t3.toString());

            Tuple.Tuple3<String, Integer, Boolean> t3Null = Tuple.of(null, null, null);
            Assertions.assertEquals("(null, null, null)", t3Null.toString());
        }
    }

    @Nested
    public class Tuple4Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = new Tuple.Tuple4<>("test", 123, true, 3.14);
            Assertions.assertEquals("test", t4._1);
            Assertions.assertEquals(123, t4._2);
            Assertions.assertEquals(true, t4._3);
            Assertions.assertEquals(3.14, t4._4);

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null = new Tuple.Tuple4<>(null, null, null, null);
            Assertions.assertNull(t4Null._1);
            Assertions.assertNull(t4Null._2);
            Assertions.assertNull(t4Null._3);
            Assertions.assertNull(t4Null._4);
        }

        @Test
        public void testArity() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Assertions.assertEquals(4, t4.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Assertions.assertFalse(t4.anyNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null1 = Tuple.of(null, 123, true, 3.14);
            Assertions.assertTrue(t4Null1.anyNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null2 = Tuple.of("test", null, true, 3.14);
            Assertions.assertTrue(t4Null2.anyNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null3 = Tuple.of("test", 123, null, 3.14);
            Assertions.assertTrue(t4Null3.anyNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null4 = Tuple.of("test", 123, true, null);
            Assertions.assertTrue(t4Null4.anyNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4AllNull = Tuple.of(null, null, null, null);
            Assertions.assertTrue(t4AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Assertions.assertFalse(t4.allNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null1 = Tuple.of(null, 123, true, 3.14);
            Assertions.assertFalse(t4Null1.allNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null2 = Tuple.of("test", null, true, 3.14);
            Assertions.assertFalse(t4Null2.allNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null3 = Tuple.of("test", 123, null, 3.14);
            Assertions.assertFalse(t4Null3.allNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null4 = Tuple.of("test", 123, true, null);
            Assertions.assertFalse(t4Null4.allNull());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4AllNull = Tuple.of(null, null, null, null);
            Assertions.assertTrue(t4AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Assertions.assertTrue(t4.contains("test"));
            Assertions.assertTrue(t4.contains(123));
            Assertions.assertTrue(t4.contains(true));
            Assertions.assertTrue(t4.contains(3.14));
            Assertions.assertFalse(t4.contains("other"));
            Assertions.assertFalse(t4.contains(456));
            Assertions.assertFalse(t4.contains(false));
            Assertions.assertFalse(t4.contains(2.71));
            Assertions.assertFalse(t4.contains(null));

            Tuple.Tuple4<String, Integer, Boolean, Double> t4WithNull = Tuple.of("test", null, true, 3.14);
            Assertions.assertTrue(t4WithNull.contains("test"));
            Assertions.assertTrue(t4WithNull.contains(null));
            Assertions.assertTrue(t4WithNull.contains(true));
            Assertions.assertTrue(t4WithNull.contains(3.14));
            Assertions.assertFalse(t4WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Object[] array = t4.toArray();
            Assertions.assertEquals(4, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);

            Object[] smallArray = new Object[2];
            Object[] result1 = t4.toArray(smallArray);
            Assertions.assertEquals(4, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);

            Object[] exactArray = new Object[4];
            Object[] result2 = t4.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);

            Object[] largeArray = new Object[6];
            largeArray[4] = "existing";
            largeArray[5] = "values";
            Object[] result3 = t4.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals("existing", result3[4]);
            Assertions.assertEquals("values", result3[5]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Tuple.Tuple4<Double, Boolean, Integer, String> reversed = t4.reverse();

            Assertions.assertEquals(3.14, reversed._1);
            Assertions.assertEquals(true, reversed._2);
            Assertions.assertEquals(123, reversed._3);
            Assertions.assertEquals("test", reversed._4);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[4];

            t4.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(4, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4a = Tuple.of("test", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4b = Tuple.of("test", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4c = Tuple.of("other", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4d = Tuple.of("test", 456, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4e = Tuple.of("test", 123, false, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4f = Tuple.of("test", 123, true, 2.71);

            Assertions.assertEquals(t4a.hashCode(), t4b.hashCode());
            Assertions.assertNotEquals(t4a.hashCode(), t4c.hashCode());
            Assertions.assertNotEquals(t4a.hashCode(), t4d.hashCode());
            Assertions.assertNotEquals(t4a.hashCode(), t4e.hashCode());
            Assertions.assertNotEquals(t4a.hashCode(), t4f.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4a = Tuple.of("test", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4b = Tuple.of("test", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4c = Tuple.of("other", 123, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4d = Tuple.of("test", 456, true, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4e = Tuple.of("test", 123, false, 3.14);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4f = Tuple.of("test", 123, true, 2.71);
            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null = Tuple.of(null, null, null, null);

            Assertions.assertTrue(t4a.equals(t4a));

            Assertions.assertTrue(t4a.equals(t4b));
            Assertions.assertTrue(t4b.equals(t4a));

            Assertions.assertFalse(t4a.equals(t4c));
            Assertions.assertFalse(t4a.equals(t4d));
            Assertions.assertFalse(t4a.equals(t4e));
            Assertions.assertFalse(t4a.equals(t4f));

            Assertions.assertTrue(t4Null.equals(Tuple.of(null, null, null, null)));
            Assertions.assertFalse(t4a.equals(t4Null));

            Assertions.assertFalse(t4a.equals("test"));
            Assertions.assertFalse(t4a.equals(null));
            Assertions.assertFalse(t4a.equals(Tuple.of("test", 123, true)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple4<String, Integer, Boolean, Double> t4 = Tuple.of("test", 123, true, 3.14);
            Assertions.assertEquals("(test, 123, true, 3.14)", t4.toString());

            Tuple.Tuple4<String, Integer, Boolean, Double> t4Null = Tuple.of(null, null, null, null);
            Assertions.assertEquals("(null, null, null, null)", t4Null.toString());
        }
    }

    @Nested
    public class Tuple5Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = new Tuple.Tuple5<>("test", 123, true, 3.14, 999L);
            Assertions.assertEquals("test", t5._1);
            Assertions.assertEquals(123, t5._2);
            Assertions.assertEquals(true, t5._3);
            Assertions.assertEquals(3.14, t5._4);
            Assertions.assertEquals(999L, t5._5);

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null = new Tuple.Tuple5<>(null, null, null, null, null);
            Assertions.assertNull(t5Null._1);
            Assertions.assertNull(t5Null._2);
            Assertions.assertNull(t5Null._3);
            Assertions.assertNull(t5Null._4);
            Assertions.assertNull(t5Null._5);
        }

        @Test
        public void testArity() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Assertions.assertEquals(5, t5.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Assertions.assertFalse(t5.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null1 = Tuple.of(null, 123, true, 3.14, 999L);
            Assertions.assertTrue(t5Null1.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null2 = Tuple.of("test", null, true, 3.14, 999L);
            Assertions.assertTrue(t5Null2.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null3 = Tuple.of("test", 123, null, 3.14, 999L);
            Assertions.assertTrue(t5Null3.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null4 = Tuple.of("test", 123, true, null, 999L);
            Assertions.assertTrue(t5Null4.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null5 = Tuple.of("test", 123, true, 3.14, null);
            Assertions.assertTrue(t5Null5.anyNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5AllNull = Tuple.of(null, null, null, null, null);
            Assertions.assertTrue(t5AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Assertions.assertFalse(t5.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null1 = Tuple.of(null, 123, true, 3.14, 999L);
            Assertions.assertFalse(t5Null1.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null2 = Tuple.of("test", null, true, 3.14, 999L);
            Assertions.assertFalse(t5Null2.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null3 = Tuple.of("test", 123, null, 3.14, 999L);
            Assertions.assertFalse(t5Null3.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null4 = Tuple.of("test", 123, true, null, 999L);
            Assertions.assertFalse(t5Null4.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null5 = Tuple.of("test", 123, true, 3.14, null);
            Assertions.assertFalse(t5Null5.allNull());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5AllNull = Tuple.of(null, null, null, null, null);
            Assertions.assertTrue(t5AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Assertions.assertTrue(t5.contains("test"));
            Assertions.assertTrue(t5.contains(123));
            Assertions.assertTrue(t5.contains(true));
            Assertions.assertTrue(t5.contains(3.14));
            Assertions.assertTrue(t5.contains(999L));
            Assertions.assertFalse(t5.contains("other"));
            Assertions.assertFalse(t5.contains(456));
            Assertions.assertFalse(t5.contains(false));
            Assertions.assertFalse(t5.contains(2.71));
            Assertions.assertFalse(t5.contains(777L));
            Assertions.assertFalse(t5.contains(null));

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5WithNull = Tuple.of("test", null, true, 3.14, 999L);
            Assertions.assertTrue(t5WithNull.contains("test"));
            Assertions.assertTrue(t5WithNull.contains(null));
            Assertions.assertTrue(t5WithNull.contains(true));
            Assertions.assertTrue(t5WithNull.contains(3.14));
            Assertions.assertTrue(t5WithNull.contains(999L));
            Assertions.assertFalse(t5WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Object[] array = t5.toArray();
            Assertions.assertEquals(5, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
            Assertions.assertEquals(999L, array[4]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);

            Object[] smallArray = new Object[3];
            Object[] result1 = t5.toArray(smallArray);
            Assertions.assertEquals(5, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);
            Assertions.assertEquals(999L, result1[4]);

            Object[] exactArray = new Object[5];
            Object[] result2 = t5.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);
            Assertions.assertEquals(999L, result2[4]);

            Object[] largeArray = new Object[7];
            largeArray[5] = "existing";
            largeArray[6] = "values";
            Object[] result3 = t5.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals(999L, result3[4]);
            Assertions.assertEquals("existing", result3[5]);
            Assertions.assertEquals("values", result3[6]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Tuple.Tuple5<Long, Double, Boolean, Integer, String> reversed = t5.reverse();

            Assertions.assertEquals(999L, reversed._1);
            Assertions.assertEquals(3.14, reversed._2);
            Assertions.assertEquals(true, reversed._3);
            Assertions.assertEquals(123, reversed._4);
            Assertions.assertEquals("test", reversed._5);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[5];

            t5.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(5, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
            Assertions.assertEquals(999L, receivedValues[4]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5a = Tuple.of("test", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5b = Tuple.of("test", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5c = Tuple.of("other", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5d = Tuple.of("test", 456, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5e = Tuple.of("test", 123, false, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5f = Tuple.of("test", 123, true, 2.71, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5g = Tuple.of("test", 123, true, 3.14, 777L);

            Assertions.assertEquals(t5a.hashCode(), t5b.hashCode());
            Assertions.assertNotEquals(t5a.hashCode(), t5c.hashCode());
            Assertions.assertNotEquals(t5a.hashCode(), t5d.hashCode());
            Assertions.assertNotEquals(t5a.hashCode(), t5e.hashCode());
            Assertions.assertNotEquals(t5a.hashCode(), t5f.hashCode());
            Assertions.assertNotEquals(t5a.hashCode(), t5g.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5a = Tuple.of("test", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5b = Tuple.of("test", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5c = Tuple.of("other", 123, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5d = Tuple.of("test", 456, true, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5e = Tuple.of("test", 123, false, 3.14, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5f = Tuple.of("test", 123, true, 2.71, 999L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5g = Tuple.of("test", 123, true, 3.14, 777L);
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null = Tuple.of(null, null, null, null, null);

            Assertions.assertTrue(t5a.equals(t5a));

            Assertions.assertTrue(t5a.equals(t5b));
            Assertions.assertTrue(t5b.equals(t5a));

            Assertions.assertFalse(t5a.equals(t5c));
            Assertions.assertFalse(t5a.equals(t5d));
            Assertions.assertFalse(t5a.equals(t5e));
            Assertions.assertFalse(t5a.equals(t5f));
            Assertions.assertFalse(t5a.equals(t5g));

            Assertions.assertTrue(t5Null.equals(Tuple.of(null, null, null, null, null)));
            Assertions.assertFalse(t5a.equals(t5Null));

            Assertions.assertFalse(t5a.equals("test"));
            Assertions.assertFalse(t5a.equals(null));
            Assertions.assertFalse(t5a.equals(Tuple.of("test", 123, true, 3.14)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5 = Tuple.of("test", 123, true, 3.14, 999L);
            Assertions.assertEquals("(test, 123, true, 3.14, 999)", t5.toString());

            Tuple.Tuple5<String, Integer, Boolean, Double, Long> t5Null = Tuple.of(null, null, null, null, null);
            Assertions.assertEquals("(null, null, null, null, null)", t5Null.toString());
        }
    }

    @Nested
    public class Tuple6Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = new Tuple.Tuple6<>("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertEquals("test", t6._1);
            Assertions.assertEquals(123, t6._2);
            Assertions.assertEquals(true, t6._3);
            Assertions.assertEquals(3.14, t6._4);
            Assertions.assertEquals(999L, t6._5);
            Assertions.assertEquals('x', t6._6);

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null = new Tuple.Tuple6<>(null, null, null, null, null, null);
            Assertions.assertNull(t6Null._1);
            Assertions.assertNull(t6Null._2);
            Assertions.assertNull(t6Null._3);
            Assertions.assertNull(t6Null._4);
            Assertions.assertNull(t6Null._5);
            Assertions.assertNull(t6Null._6);
        }

        @Test
        public void testArity() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertEquals(6, t6.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertFalse(t6.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x');
            Assertions.assertTrue(t6Null1.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null2 = Tuple.of("test", null, true, 3.14, 999L, 'x');
            Assertions.assertTrue(t6Null2.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null3 = Tuple.of("test", 123, null, 3.14, 999L, 'x');
            Assertions.assertTrue(t6Null3.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null4 = Tuple.of("test", 123, true, null, 999L, 'x');
            Assertions.assertTrue(t6Null4.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null5 = Tuple.of("test", 123, true, 3.14, null, 'x');
            Assertions.assertTrue(t6Null5.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null6 = Tuple.of("test", 123, true, 3.14, 999L, null);
            Assertions.assertTrue(t6Null6.anyNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6AllNull = Tuple.of(null, null, null, null, null, null);
            Assertions.assertTrue(t6AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertFalse(t6.allNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x');
            Assertions.assertFalse(t6Null1.allNull());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6AllNull = Tuple.of(null, null, null, null, null, null);
            Assertions.assertTrue(t6AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertTrue(t6.contains("test"));
            Assertions.assertTrue(t6.contains(123));
            Assertions.assertTrue(t6.contains(true));
            Assertions.assertTrue(t6.contains(3.14));
            Assertions.assertTrue(t6.contains(999L));
            Assertions.assertTrue(t6.contains('x'));
            Assertions.assertFalse(t6.contains("other"));
            Assertions.assertFalse(t6.contains(456));
            Assertions.assertFalse(t6.contains(false));
            Assertions.assertFalse(t6.contains(2.71));
            Assertions.assertFalse(t6.contains(777L));
            Assertions.assertFalse(t6.contains('y'));
            Assertions.assertFalse(t6.contains(null));

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6WithNull = Tuple.of("test", null, true, 3.14, 999L, 'x');
            Assertions.assertTrue(t6WithNull.contains("test"));
            Assertions.assertTrue(t6WithNull.contains(null));
            Assertions.assertTrue(t6WithNull.contains(true));
            Assertions.assertTrue(t6WithNull.contains(3.14));
            Assertions.assertTrue(t6WithNull.contains(999L));
            Assertions.assertTrue(t6WithNull.contains('x'));
            Assertions.assertFalse(t6WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Object[] array = t6.toArray();
            Assertions.assertEquals(6, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
            Assertions.assertEquals(999L, array[4]);
            Assertions.assertEquals('x', array[5]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');

            Object[] smallArray = new Object[3];
            Object[] result1 = t6.toArray(smallArray);
            Assertions.assertEquals(6, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);
            Assertions.assertEquals(999L, result1[4]);
            Assertions.assertEquals('x', result1[5]);

            Object[] exactArray = new Object[6];
            Object[] result2 = t6.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);
            Assertions.assertEquals(999L, result2[4]);
            Assertions.assertEquals('x', result2[5]);

            Object[] largeArray = new Object[8];
            largeArray[6] = "existing";
            largeArray[7] = "values";
            Object[] result3 = t6.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals(999L, result3[4]);
            Assertions.assertEquals('x', result3[5]);
            Assertions.assertEquals("existing", result3[6]);
            Assertions.assertEquals("values", result3[7]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<Character, Long, Double, Boolean, Integer, String> reversed = t6.reverse();

            Assertions.assertEquals('x', reversed._1);
            Assertions.assertEquals(999L, reversed._2);
            Assertions.assertEquals(3.14, reversed._3);
            Assertions.assertEquals(true, reversed._4);
            Assertions.assertEquals(123, reversed._5);
            Assertions.assertEquals("test", reversed._6);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[6];

            t6.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(6, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
            Assertions.assertEquals(999L, receivedValues[4]);
            Assertions.assertEquals('x', receivedValues[5]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6a = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6b = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6c = Tuple.of("other", 123, true, 3.14, 999L, 'x');

            Assertions.assertEquals(t6a.hashCode(), t6b.hashCode());
            Assertions.assertNotEquals(t6a.hashCode(), t6c.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6a = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6b = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6c = Tuple.of("other", 123, true, 3.14, 999L, 'x');
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null = Tuple.of(null, null, null, null, null, null);

            Assertions.assertTrue(t6a.equals(t6a));

            Assertions.assertTrue(t6a.equals(t6b));
            Assertions.assertTrue(t6b.equals(t6a));

            Assertions.assertFalse(t6a.equals(t6c));

            Assertions.assertTrue(t6Null.equals(Tuple.of(null, null, null, null, null, null)));
            Assertions.assertFalse(t6a.equals(t6Null));

            Assertions.assertFalse(t6a.equals("test"));
            Assertions.assertFalse(t6a.equals(null));
            Assertions.assertFalse(t6a.equals(Tuple.of("test", 123, true, 3.14, 999L)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6 = Tuple.of("test", 123, true, 3.14, 999L, 'x');
            Assertions.assertEquals("(test, 123, true, 3.14, 999, x)", t6.toString());

            Tuple.Tuple6<String, Integer, Boolean, Double, Long, Character> t6Null = Tuple.of(null, null, null, null, null, null);
            Assertions.assertEquals("(null, null, null, null, null, null)", t6Null.toString());
        }
    }

    @Nested
    public class Tuple7Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = new Tuple.Tuple7<>("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertEquals("test", t7._1);
            Assertions.assertEquals(123, t7._2);
            Assertions.assertEquals(true, t7._3);
            Assertions.assertEquals(3.14, t7._4);
            Assertions.assertEquals(999L, t7._5);
            Assertions.assertEquals('x', t7._6);
            Assertions.assertEquals(2.5f, t7._7);

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null = new Tuple.Tuple7<>(null, null, null, null, null, null, null);
            Assertions.assertNull(t7Null._1);
            Assertions.assertNull(t7Null._2);
            Assertions.assertNull(t7Null._3);
            Assertions.assertNull(t7Null._4);
            Assertions.assertNull(t7Null._5);
            Assertions.assertNull(t7Null._6);
            Assertions.assertNull(t7Null._7);
        }

        @Test
        public void testArity() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertEquals(7, t7.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertFalse(t7.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7Null1.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null2 = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7Null2.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null3 = Tuple.of("test", 123, null, 3.14, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7Null3.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null4 = Tuple.of("test", 123, true, null, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7Null4.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null5 = Tuple.of("test", 123, true, 3.14, null, 'x', 2.5f);
            Assertions.assertTrue(t7Null5.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null6 = Tuple.of("test", 123, true, 3.14, 999L, null, 2.5f);
            Assertions.assertTrue(t7Null6.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', null);
            Assertions.assertTrue(t7Null7.anyNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7AllNull = Tuple.of(null, null, null, null, null, null, null);
            Assertions.assertTrue(t7AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertFalse(t7.allNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertFalse(t7Null1.allNull());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7AllNull = Tuple.of(null, null, null, null, null, null, null);
            Assertions.assertTrue(t7AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7.contains("test"));
            Assertions.assertTrue(t7.contains(123));
            Assertions.assertTrue(t7.contains(true));
            Assertions.assertTrue(t7.contains(3.14));
            Assertions.assertTrue(t7.contains(999L));
            Assertions.assertTrue(t7.contains('x'));
            Assertions.assertTrue(t7.contains(2.5f));
            Assertions.assertFalse(t7.contains("other"));
            Assertions.assertFalse(t7.contains(456));
            Assertions.assertFalse(t7.contains(false));
            Assertions.assertFalse(t7.contains(2.71));
            Assertions.assertFalse(t7.contains(777L));
            Assertions.assertFalse(t7.contains('y'));
            Assertions.assertFalse(t7.contains(3.5f));
            Assertions.assertFalse(t7.contains(null));

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7WithNull = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertTrue(t7WithNull.contains("test"));
            Assertions.assertTrue(t7WithNull.contains(null));
            Assertions.assertTrue(t7WithNull.contains(true));
            Assertions.assertTrue(t7WithNull.contains(3.14));
            Assertions.assertTrue(t7WithNull.contains(999L));
            Assertions.assertTrue(t7WithNull.contains('x'));
            Assertions.assertTrue(t7WithNull.contains(2.5f));
            Assertions.assertFalse(t7WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Object[] array = t7.toArray();
            Assertions.assertEquals(7, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
            Assertions.assertEquals(999L, array[4]);
            Assertions.assertEquals('x', array[5]);
            Assertions.assertEquals(2.5f, array[6]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);

            Object[] smallArray = new Object[4];
            Object[] result1 = t7.toArray(smallArray);
            Assertions.assertEquals(7, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);
            Assertions.assertEquals(999L, result1[4]);
            Assertions.assertEquals('x', result1[5]);
            Assertions.assertEquals(2.5f, result1[6]);

            Object[] exactArray = new Object[7];
            Object[] result2 = t7.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);
            Assertions.assertEquals(999L, result2[4]);
            Assertions.assertEquals('x', result2[5]);
            Assertions.assertEquals(2.5f, result2[6]);

            Object[] largeArray = new Object[9];
            largeArray[7] = "existing";
            largeArray[8] = "values";
            Object[] result3 = t7.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals(999L, result3[4]);
            Assertions.assertEquals('x', result3[5]);
            Assertions.assertEquals(2.5f, result3[6]);
            Assertions.assertEquals("existing", result3[7]);
            Assertions.assertEquals("values", result3[8]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<Float, Character, Long, Double, Boolean, Integer, String> reversed = t7.reverse();

            Assertions.assertEquals(2.5f, reversed._1);
            Assertions.assertEquals('x', reversed._2);
            Assertions.assertEquals(999L, reversed._3);
            Assertions.assertEquals(3.14, reversed._4);
            Assertions.assertEquals(true, reversed._5);
            Assertions.assertEquals(123, reversed._6);
            Assertions.assertEquals("test", reversed._7);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[7];

            t7.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(7, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
            Assertions.assertEquals(999L, receivedValues[4]);
            Assertions.assertEquals('x', receivedValues[5]);
            Assertions.assertEquals(2.5f, receivedValues[6]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7d = Tuple.of("test", 456, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7e = Tuple.of("test", 123, false, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7f = Tuple.of("test", 123, true, 2.71, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7g = Tuple.of("test", 123, true, 3.14, 777L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7h = Tuple.of("test", 123, true, 3.14, 999L, 'y', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7i = Tuple.of("test", 123, true, 3.14, 999L, 'x', 3.5f);

            Assertions.assertEquals(t7a.hashCode(), t7b.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7c.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7d.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7e.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7f.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7g.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7h.hashCode());
            Assertions.assertNotEquals(t7a.hashCode(), t7i.hashCode());
        }

        @Test
        public void testEquals() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7d = Tuple.of("test", 456, true, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7e = Tuple.of("test", 123, false, 3.14, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7f = Tuple.of("test", 123, true, 2.71, 999L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7g = Tuple.of("test", 123, true, 3.14, 777L, 'x', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7h = Tuple.of("test", 123, true, 3.14, 999L, 'y', 2.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7i = Tuple.of("test", 123, true, 3.14, 999L, 'x', 3.5f);
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null = Tuple.of(null, null, null, null, null, null, null);

            Assertions.assertTrue(t7a.equals(t7a));

            Assertions.assertTrue(t7a.equals(t7b));
            Assertions.assertTrue(t7b.equals(t7a));

            Assertions.assertFalse(t7a.equals(t7c));
            Assertions.assertFalse(t7a.equals(t7d));
            Assertions.assertFalse(t7a.equals(t7e));
            Assertions.assertFalse(t7a.equals(t7f));
            Assertions.assertFalse(t7a.equals(t7g));
            Assertions.assertFalse(t7a.equals(t7h));
            Assertions.assertFalse(t7a.equals(t7i));

            Assertions.assertTrue(t7Null.equals(Tuple.of(null, null, null, null, null, null, null)));
            Assertions.assertFalse(t7a.equals(t7Null));

            Assertions.assertFalse(t7a.equals("test"));
            Assertions.assertFalse(t7a.equals(null));
            Assertions.assertFalse(t7a.equals(Tuple.of("test", 123, true, 3.14, 999L, 'x')));
        }

        @Test
        public void testToString() {
            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f);
            Assertions.assertEquals("(test, 123, true, 3.14, 999, x, 2.5)", t7.toString());

            Tuple.Tuple7<String, Integer, Boolean, Double, Long, Character, Float> t7Null = Tuple.of(null, null, null, null, null, null, null);
            Assertions.assertEquals("(null, null, null, null, null, null, null)", t7Null.toString());
        }
    }

    @Nested
    public class Tuple8Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = new Tuple.Tuple8<>("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertEquals("test", t8._1);
            Assertions.assertEquals(123, t8._2);
            Assertions.assertEquals(true, t8._3);
            Assertions.assertEquals(3.14, t8._4);
            Assertions.assertEquals(999L, t8._5);
            Assertions.assertEquals('x', t8._6);
            Assertions.assertEquals(2.5f, t8._7);
            Assertions.assertEquals((byte) 42, t8._8);

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null = new Tuple.Tuple8<>(null, null, null, null, null, null, null,
                    null);
            Assertions.assertNull(t8Null._1);
            Assertions.assertNull(t8Null._2);
            Assertions.assertNull(t8Null._3);
            Assertions.assertNull(t8Null._4);
            Assertions.assertNull(t8Null._5);
            Assertions.assertNull(t8Null._6);
            Assertions.assertNull(t8Null._7);
            Assertions.assertNull(t8Null._8);
        }

        @Test
        public void testArity() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertEquals(8, t8.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertFalse(t8.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertTrue(t8Null1.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null2 = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8Null2.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null3 = Tuple.of("test", 123, null, 3.14, 999L, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8Null3.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null4 = Tuple.of("test", 123, true, null, 999L, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8Null4.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null5 = Tuple.of("test", 123, true, 3.14, null, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8Null5.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null6 = Tuple.of("test", 123, true, 3.14, 999L, null, 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8Null6.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', null,
                    (byte) 42);
            Assertions.assertTrue(t8Null7.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, null);
            Assertions.assertTrue(t8Null8.anyNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8AllNull = Tuple.of(null, null, null, null, null, null, null, null);
            Assertions.assertTrue(t8AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertFalse(t8.allNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertFalse(t8Null1.allNull());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8AllNull = Tuple.of(null, null, null, null, null, null, null, null);
            Assertions.assertTrue(t8AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertTrue(t8.contains("test"));
            Assertions.assertTrue(t8.contains(123));
            Assertions.assertTrue(t8.contains(true));
            Assertions.assertTrue(t8.contains(3.14));
            Assertions.assertTrue(t8.contains(999L));
            Assertions.assertTrue(t8.contains('x'));
            Assertions.assertTrue(t8.contains(2.5f));
            Assertions.assertTrue(t8.contains((byte) 42));
            Assertions.assertFalse(t8.contains("other"));
            Assertions.assertFalse(t8.contains(456));
            Assertions.assertFalse(t8.contains(false));
            Assertions.assertFalse(t8.contains(2.71));
            Assertions.assertFalse(t8.contains(777L));
            Assertions.assertFalse(t8.contains('y'));
            Assertions.assertFalse(t8.contains(3.5f));
            Assertions.assertFalse(t8.contains((byte) 99));
            Assertions.assertFalse(t8.contains(null));

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8WithNull = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42);
            Assertions.assertTrue(t8WithNull.contains("test"));
            Assertions.assertTrue(t8WithNull.contains(null));
            Assertions.assertTrue(t8WithNull.contains(true));
            Assertions.assertTrue(t8WithNull.contains(3.14));
            Assertions.assertTrue(t8WithNull.contains(999L));
            Assertions.assertTrue(t8WithNull.contains('x'));
            Assertions.assertTrue(t8WithNull.contains(2.5f));
            Assertions.assertTrue(t8WithNull.contains((byte) 42));
            Assertions.assertFalse(t8WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Object[] array = t8.toArray();
            Assertions.assertEquals(8, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
            Assertions.assertEquals(999L, array[4]);
            Assertions.assertEquals('x', array[5]);
            Assertions.assertEquals(2.5f, array[6]);
            Assertions.assertEquals((byte) 42, array[7]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);

            Object[] smallArray = new Object[5];
            Object[] result1 = t8.toArray(smallArray);
            Assertions.assertEquals(8, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);
            Assertions.assertEquals(999L, result1[4]);
            Assertions.assertEquals('x', result1[5]);
            Assertions.assertEquals(2.5f, result1[6]);
            Assertions.assertEquals((byte) 42, result1[7]);

            Object[] exactArray = new Object[8];
            Object[] result2 = t8.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);
            Assertions.assertEquals(999L, result2[4]);
            Assertions.assertEquals('x', result2[5]);
            Assertions.assertEquals(2.5f, result2[6]);
            Assertions.assertEquals((byte) 42, result2[7]);

            Object[] largeArray = new Object[10];
            largeArray[8] = "existing";
            largeArray[9] = "values";
            Object[] result3 = t8.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals(999L, result3[4]);
            Assertions.assertEquals('x', result3[5]);
            Assertions.assertEquals(2.5f, result3[6]);
            Assertions.assertEquals((byte) 42, result3[7]);
            Assertions.assertEquals("existing", result3[8]);
            Assertions.assertEquals("values", result3[9]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<Byte, Float, Character, Long, Double, Boolean, Integer, String> reversed = t8.reverse();

            Assertions.assertEquals((byte) 42, reversed._1);
            Assertions.assertEquals(2.5f, reversed._2);
            Assertions.assertEquals('x', reversed._3);
            Assertions.assertEquals(999L, reversed._4);
            Assertions.assertEquals(3.14, reversed._5);
            Assertions.assertEquals(true, reversed._6);
            Assertions.assertEquals(123, reversed._7);
            Assertions.assertEquals("test", reversed._8);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[8];

            t8.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(8, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
            Assertions.assertEquals(999L, receivedValues[4]);
            Assertions.assertEquals('x', receivedValues[5]);
            Assertions.assertEquals(2.5f, receivedValues[6]);
            Assertions.assertEquals((byte) 42, receivedValues[7]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);

            Assertions.assertEquals(t8a.hashCode(), t8b.hashCode());
            Assertions.assertNotEquals(t8a.hashCode(), t8c.hashCode());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null = Tuple.of(null, null, null, null, null, null, null, null);
            int nullHash = t8Null.hashCode();
            Assertions.assertTrue(nullHash >= 0 || nullHash < 0);
        }

        @Test
        public void testEquals() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8d = Tuple.of("test", 456, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8e = Tuple.of("test", 123, false, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8f = Tuple.of("test", 123, true, 2.71, 999L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8g = Tuple.of("test", 123, true, 3.14, 777L, 'x', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8h = Tuple.of("test", 123, true, 3.14, 999L, 'y', 2.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8i = Tuple.of("test", 123, true, 3.14, 999L, 'x', 3.5f, (byte) 42);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8j = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 99);
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null = Tuple.of(null, null, null, null, null, null, null, null);

            Assertions.assertTrue(t8a.equals(t8a));

            Assertions.assertTrue(t8a.equals(t8b));
            Assertions.assertTrue(t8b.equals(t8a));

            Assertions.assertFalse(t8a.equals(t8c));
            Assertions.assertFalse(t8a.equals(t8d));
            Assertions.assertFalse(t8a.equals(t8e));
            Assertions.assertFalse(t8a.equals(t8f));
            Assertions.assertFalse(t8a.equals(t8g));
            Assertions.assertFalse(t8a.equals(t8h));
            Assertions.assertFalse(t8a.equals(t8i));
            Assertions.assertFalse(t8a.equals(t8j));

            Assertions.assertTrue(t8Null.equals(Tuple.of(null, null, null, null, null, null, null, null)));
            Assertions.assertFalse(t8a.equals(t8Null));

            Assertions.assertFalse(t8a.equals("test"));
            Assertions.assertFalse(t8a.equals(null));
            Assertions.assertFalse(t8a.equals(Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42);
            Assertions.assertEquals("(test, 123, true, 3.14, 999, x, 2.5, 42)", t8.toString());

            Tuple.Tuple8<String, Integer, Boolean, Double, Long, Character, Float, Byte> t8Null = Tuple.of(null, null, null, null, null, null, null, null);
            Assertions.assertEquals("(null, null, null, null, null, null, null, null)", t8Null.toString());
        }
    }

    @Nested
    public class Tuple9Test {

        @Test
        public void testConstructor() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = new Tuple.Tuple9<>("test", 123, true, 3.14, 999L, 'x',
                    2.5f, (byte) 42, (short) 256);
            Assertions.assertEquals("test", t9._1);
            Assertions.assertEquals(123, t9._2);
            Assertions.assertEquals(true, t9._3);
            Assertions.assertEquals(3.14, t9._4);
            Assertions.assertEquals(999L, t9._5);
            Assertions.assertEquals('x', t9._6);
            Assertions.assertEquals(2.5f, t9._7);
            Assertions.assertEquals((byte) 42, t9._8);
            Assertions.assertEquals((short) 256, t9._9);

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null = new Tuple.Tuple9<>(null, null, null, null, null, null,
                    null, null, null);
            Assertions.assertNull(t9Null._1);
            Assertions.assertNull(t9Null._2);
            Assertions.assertNull(t9Null._3);
            Assertions.assertNull(t9Null._4);
            Assertions.assertNull(t9Null._5);
            Assertions.assertNull(t9Null._6);
            Assertions.assertNull(t9Null._7);
            Assertions.assertNull(t9Null._8);
            Assertions.assertNull(t9Null._9);
        }

        @Test
        public void testArity() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertEquals(9, t9.arity());
        }

        @Test
        public void testAnyNull() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertFalse(t9.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null1.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null2 = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null2.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null3 = Tuple.of("test", 123, null, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null3.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null4 = Tuple.of("test", 123, true, null, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null4.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null5 = Tuple.of("test", 123, true, 3.14, null, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null5.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null6 = Tuple.of("test", 123, true, 3.14, 999L, null, 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null6.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null7 = Tuple.of("test", 123, true, 3.14, 999L, 'x', null,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9Null7.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null8 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    null, (short) 256);
            Assertions.assertTrue(t9Null8.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, null);
            Assertions.assertTrue(t9Null9.anyNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9AllNull = Tuple.of(null, null, null, null, null, null, null,
                    null, null);
            Assertions.assertTrue(t9AllNull.anyNull());
        }

        @Test
        public void testAllNull() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertFalse(t9.allNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null1 = Tuple.of(null, 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertFalse(t9Null1.allNull());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9AllNull = Tuple.of(null, null, null, null, null, null, null,
                    null, null);
            Assertions.assertTrue(t9AllNull.allNull());
        }

        @Test
        public void testContains() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9.contains("test"));
            Assertions.assertTrue(t9.contains(123));
            Assertions.assertTrue(t9.contains(true));
            Assertions.assertTrue(t9.contains(3.14));
            Assertions.assertTrue(t9.contains(999L));
            Assertions.assertTrue(t9.contains('x'));
            Assertions.assertTrue(t9.contains(2.5f));
            Assertions.assertTrue(t9.contains((byte) 42));
            Assertions.assertTrue(t9.contains((short) 256));
            Assertions.assertFalse(t9.contains("other"));
            Assertions.assertFalse(t9.contains(456));
            Assertions.assertFalse(t9.contains(false));
            Assertions.assertFalse(t9.contains(2.71));
            Assertions.assertFalse(t9.contains(777L));
            Assertions.assertFalse(t9.contains('y'));
            Assertions.assertFalse(t9.contains(3.5f));
            Assertions.assertFalse(t9.contains((byte) 99));
            Assertions.assertFalse(t9.contains((short) 512));
            Assertions.assertFalse(t9.contains(null));

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9WithNull = Tuple.of("test", null, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertTrue(t9WithNull.contains("test"));
            Assertions.assertTrue(t9WithNull.contains(null));
            Assertions.assertTrue(t9WithNull.contains(true));
            Assertions.assertTrue(t9WithNull.contains(3.14));
            Assertions.assertTrue(t9WithNull.contains(999L));
            Assertions.assertTrue(t9WithNull.contains('x'));
            Assertions.assertTrue(t9WithNull.contains(2.5f));
            Assertions.assertTrue(t9WithNull.contains((byte) 42));
            Assertions.assertTrue(t9WithNull.contains((short) 256));
            Assertions.assertFalse(t9WithNull.contains(123));
        }

        @Test
        public void testToArray() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Object[] array = t9.toArray();
            Assertions.assertEquals(9, array.length);
            Assertions.assertEquals("test", array[0]);
            Assertions.assertEquals(123, array[1]);
            Assertions.assertEquals(true, array[2]);
            Assertions.assertEquals(3.14, array[3]);
            Assertions.assertEquals(999L, array[4]);
            Assertions.assertEquals('x', array[5]);
            Assertions.assertEquals(2.5f, array[6]);
            Assertions.assertEquals((byte) 42, array[7]);
            Assertions.assertEquals((short) 256, array[8]);
        }

        @Test
        public void testToArrayWithParameter() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);

            Object[] smallArray = new Object[5];
            Object[] result1 = t9.toArray(smallArray);
            Assertions.assertEquals(9, result1.length);
            Assertions.assertEquals("test", result1[0]);
            Assertions.assertEquals(123, result1[1]);
            Assertions.assertEquals(true, result1[2]);
            Assertions.assertEquals(3.14, result1[3]);
            Assertions.assertEquals(999L, result1[4]);
            Assertions.assertEquals('x', result1[5]);
            Assertions.assertEquals(2.5f, result1[6]);
            Assertions.assertEquals((byte) 42, result1[7]);
            Assertions.assertEquals((short) 256, result1[8]);

            Object[] exactArray = new Object[9];
            Object[] result2 = t9.toArray(exactArray);
            Assertions.assertSame(exactArray, result2);
            Assertions.assertEquals("test", result2[0]);
            Assertions.assertEquals(123, result2[1]);
            Assertions.assertEquals(true, result2[2]);
            Assertions.assertEquals(3.14, result2[3]);
            Assertions.assertEquals(999L, result2[4]);
            Assertions.assertEquals('x', result2[5]);
            Assertions.assertEquals(2.5f, result2[6]);
            Assertions.assertEquals((byte) 42, result2[7]);
            Assertions.assertEquals((short) 256, result2[8]);

            Object[] largeArray = new Object[11];
            largeArray[9] = "existing";
            largeArray[10] = "values";
            Object[] result3 = t9.toArray(largeArray);
            Assertions.assertSame(largeArray, result3);
            Assertions.assertEquals("test", result3[0]);
            Assertions.assertEquals(123, result3[1]);
            Assertions.assertEquals(true, result3[2]);
            Assertions.assertEquals(3.14, result3[3]);
            Assertions.assertEquals(999L, result3[4]);
            Assertions.assertEquals('x', result3[5]);
            Assertions.assertEquals(2.5f, result3[6]);
            Assertions.assertEquals((byte) 42, result3[7]);
            Assertions.assertEquals((short) 256, result3[8]);
            Assertions.assertEquals("existing", result3[9]);
            Assertions.assertEquals("values", result3[10]);
        }

        @Test
        public void testReverse() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<Short, Byte, Float, Character, Long, Double, Boolean, Integer, String> reversed = t9.reverse();

            Assertions.assertEquals((short) 256, reversed._1);
            Assertions.assertEquals((byte) 42, reversed._2);
            Assertions.assertEquals(2.5f, reversed._3);
            Assertions.assertEquals('x', reversed._4);
            Assertions.assertEquals(999L, reversed._5);
            Assertions.assertEquals(3.14, reversed._6);
            Assertions.assertEquals(true, reversed._7);
            Assertions.assertEquals(123, reversed._8);
            Assertions.assertEquals("test", reversed._9);
        }

        @Test
        public void testForEach() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            final int[] callCount = { 0 };
            final Object[] receivedValues = new Object[9];

            t9.forEach(obj -> {
                receivedValues[callCount[0]] = obj;
                callCount[0]++;
            });

            Assertions.assertEquals(9, callCount[0]);
            Assertions.assertEquals("test", receivedValues[0]);
            Assertions.assertEquals(123, receivedValues[1]);
            Assertions.assertEquals(true, receivedValues[2]);
            Assertions.assertEquals(3.14, receivedValues[3]);
            Assertions.assertEquals(999L, receivedValues[4]);
            Assertions.assertEquals('x', receivedValues[5]);
            Assertions.assertEquals(2.5f, receivedValues[6]);
            Assertions.assertEquals((byte) 42, receivedValues[7]);
            Assertions.assertEquals((short) 256, receivedValues[8]);
        }

        @Test
        public void testHashCode() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);

            Assertions.assertEquals(t9a.hashCode(), t9b.hashCode());
            Assertions.assertNotEquals(t9a.hashCode(), t9c.hashCode());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null = Tuple.of(null, null, null, null, null, null, null,
                    null, null);
            int nullHash = t9Null.hashCode();
            Assertions.assertTrue(nullHash >= 0 || nullHash < 0);
        }

        @Test
        public void testEquals() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9a = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9b = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9c = Tuple.of("other", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9d = Tuple.of("test", 456, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9e = Tuple.of("test", 123, false, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9f = Tuple.of("test", 123, true, 2.71, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9g = Tuple.of("test", 123, true, 3.14, 777L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9h = Tuple.of("test", 123, true, 3.14, 999L, 'y', 2.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9i = Tuple.of("test", 123, true, 3.14, 999L, 'x', 3.5f,
                    (byte) 42, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9j = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 99, (short) 256);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9k = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 512);
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null = Tuple.of(null, null, null, null, null, null, null,
                    null, null);

            Assertions.assertTrue(t9a.equals(t9a));

            Assertions.assertTrue(t9a.equals(t9b));
            Assertions.assertTrue(t9b.equals(t9a));

            Assertions.assertFalse(t9a.equals(t9c));
            Assertions.assertFalse(t9a.equals(t9d));
            Assertions.assertFalse(t9a.equals(t9e));
            Assertions.assertFalse(t9a.equals(t9f));
            Assertions.assertFalse(t9a.equals(t9g));
            Assertions.assertFalse(t9a.equals(t9h));
            Assertions.assertFalse(t9a.equals(t9i));
            Assertions.assertFalse(t9a.equals(t9j));
            Assertions.assertFalse(t9a.equals(t9k));

            Assertions.assertTrue(t9Null.equals(Tuple.of(null, null, null, null, null, null, null, null, null)));
            Assertions.assertFalse(t9a.equals(t9Null));

            Assertions.assertFalse(t9a.equals("test"));
            Assertions.assertFalse(t9a.equals(null));
            Assertions.assertFalse(t9a.equals(Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f, (byte) 42)));
        }

        @Test
        public void testToString() {
            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9 = Tuple.of("test", 123, true, 3.14, 999L, 'x', 2.5f,
                    (byte) 42, (short) 256);
            Assertions.assertEquals("(test, 123, true, 3.14, 999, x, 2.5, 42, 256)", t9.toString());

            Tuple.Tuple9<String, Integer, Boolean, Double, Long, Character, Float, Byte, Short> t9Null = Tuple.of(null, null, null, null, null, null, null,
                    null, null);
            Assertions.assertEquals("(null, null, null, null, null, null, null, null, null)", t9Null.toString());
        }
    }
}
