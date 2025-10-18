package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableArray100Test extends TestBase {

    @Test
    public void testOf_SingleElement() {
        ImmutableArray<String> array = ImmutableArray.of("single");
        Assertions.assertEquals(1, array.length());
        Assertions.assertEquals("single", array.get(0));
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2);
        Assertions.assertEquals(2, array.length());
        Assertions.assertEquals(1, array.get(0));
        Assertions.assertEquals(2, array.get(1));
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals("a", array.get(0));
        Assertions.assertEquals("b", array.get(1));
        Assertions.assertEquals("c", array.get(2));
    }

    @Test
    public void testOf_FourElements() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3, 4);
        Assertions.assertEquals(4, array.length());
        Assertions.assertEquals(4, array.get(3));
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableArray<String> array = ImmutableArray.of("1", "2", "3", "4", "5");
        Assertions.assertEquals(5, array.length());
        Assertions.assertEquals("5", array.get(4));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3, 4, 5, 6);
        Assertions.assertEquals(6, array.length());
        Assertions.assertEquals(6, array.get(5));
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "d", "e", "f", "g");
        Assertions.assertEquals(7, array.length());
        Assertions.assertEquals("g", array.get(6));
    }

    @Test
    public void testOf_EightElements() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3, 4, 5, 6, 7, 8);
        Assertions.assertEquals(8, array.length());
        Assertions.assertEquals(8, array.get(7));
    }

    @Test
    public void testOf_NineElements() {
        ImmutableArray<String> array = ImmutableArray.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        Assertions.assertEquals(9, array.length());
        Assertions.assertEquals("9", array.get(8));
    }

    @Test
    public void testOf_TenElements() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Assertions.assertEquals(10, array.length());
        Assertions.assertEquals(10, array.get(9));
    }

    @Test
    public void testCopyOf() {
        String[] original = { "a", "b", "c" };
        ImmutableArray<String> array = ImmutableArray.copyOf(original);

        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals("a", array.get(0));
        Assertions.assertEquals("b", array.get(1));
        Assertions.assertEquals("c", array.get(2));

        original[0] = "modified";
        Assertions.assertEquals("a", array.get(0));
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableArray<String> array = ImmutableArray.copyOf(null);
        Assertions.assertEquals(0, array.length());
        Assertions.assertTrue(array.isEmpty());
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableArray<String> array = ImmutableArray.copyOf(new String[0]);
        Assertions.assertEquals(0, array.length());
        Assertions.assertTrue(array.isEmpty());
    }

    @Test
    public void testWrap() {
        String[] original = { "x", "y", "z" };
        ImmutableArray<String> array = ImmutableArray.wrap(original);

        Assertions.assertEquals(3, array.length());

        original[1] = "modified";
        Assertions.assertEquals("modified", array.get(1));
    }

    @Test
    public void testWrap_Null() {
        ImmutableArray<String> array = ImmutableArray.wrap(null);
        Assertions.assertEquals(0, array.length());
        Assertions.assertTrue(array.isEmpty());
    }

    @Test
    public void testLength() {
        Assertions.assertEquals(0, ImmutableArray.copyOf(new String[0]).length());
        Assertions.assertEquals(1, ImmutableArray.of("a").length());
        Assertions.assertEquals(5, ImmutableArray.of(1, 2, 3, 4, 5).length());
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(ImmutableArray.copyOf(null).isEmpty());
        Assertions.assertTrue(ImmutableArray.copyOf(new String[0]).isEmpty());
        Assertions.assertFalse(ImmutableArray.of("a").isEmpty());
    }

    @Test
    public void testGet() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        Assertions.assertEquals("a", array.get(0));
        Assertions.assertEquals("b", array.get(1));
        Assertions.assertEquals("c", array.get(2));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> array.get(3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
    }

    @Test
    public void testIndexOf() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "b");
        Assertions.assertEquals(0, array.indexOf("a"));
        Assertions.assertEquals(1, array.indexOf("b"));
        Assertions.assertEquals(2, array.indexOf("c"));
        Assertions.assertEquals(-1, array.indexOf("d"));
        Assertions.assertEquals(-1, array.indexOf(null));
    }

    @Test
    public void testIndexOf_WithNull() {
        String[] original = { "a", null, "c" };
        ImmutableArray<String> array = ImmutableArray.wrap(original);
        Assertions.assertEquals(1, array.indexOf(null));
    }

    @Test
    public void testLastIndexOf() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "b");
        Assertions.assertEquals(0, array.lastIndexOf("a"));
        Assertions.assertEquals(3, array.lastIndexOf("b"));
        Assertions.assertEquals(2, array.lastIndexOf("c"));
        Assertions.assertEquals(-1, array.lastIndexOf("d"));
    }

    @Test
    public void testContains() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        Assertions.assertTrue(array.contains("a"));
        Assertions.assertTrue(array.contains("b"));
        Assertions.assertTrue(array.contains("c"));
        Assertions.assertFalse(array.contains("d"));
        Assertions.assertFalse(array.contains(null));
    }

    @Test
    public void testCopy() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "d", "e");

        ImmutableArray<String> copy1 = array.copy(1, 4);
        Assertions.assertEquals(3, copy1.length());
        Assertions.assertEquals("b", copy1.get(0));
        Assertions.assertEquals("c", copy1.get(1));
        Assertions.assertEquals("d", copy1.get(2));

        ImmutableArray<String> copy2 = array.copy(0, 5);
        Assertions.assertEquals(5, copy2.length());

        ImmutableArray<String> copy3 = array.copy(2, 2);
        Assertions.assertEquals(0, copy3.length());
    }

    @Test
    public void testCopy_InvalidRange() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> array.copy(-1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> array.copy(0, 4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> array.copy(2, 1));
    }

    @Test
    public void testAsList() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        ImmutableList<String> list = array.asList();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add("d"));
    }

    @Test
    public void testIterator() {
        ImmutableArray<String> array = ImmutableArray.of("x", "y", "z");
        ObjIterator<String> iter = array.iterator();

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("x", iter.next());
        Assertions.assertEquals("y", iter.next());
        Assertions.assertEquals("z", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3, 4, 5);
        List<Integer> collected = array.stream().toList();

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), collected);
    }

    @Test
    public void testForEach() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        List<String> collected = new ArrayList<>();

        array.forEach(collected::add);

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testForEach_NullConsumer() {
        ImmutableArray<String> array = ImmutableArray.of("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> array.forEach((Consumer<String>) null));
    }

    @Test
    public void testForeach() throws Exception {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        List<String> collected = new ArrayList<>();

        array.foreach(collected::add);

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testForeach_NullConsumer() {
        ImmutableArray<String> array = ImmutableArray.of("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            array.foreach((Throwables.Consumer<String, Exception>) null);
        });
    }

    @Test
    public void testForeachIndexed() throws Exception {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        Map<Integer, String> collected = new HashMap<>();

        array.foreachIndexed((index, value) -> collected.put(index, value));

        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals("a", collected.get(0));
        Assertions.assertEquals("b", collected.get(1));
        Assertions.assertEquals("c", collected.get(2));
    }

    @Test
    public void testForeachIndexed_NullConsumer() {
        ImmutableArray<String> array = ImmutableArray.of("a");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            array.foreachIndexed(null);
        });
    }

    @Test
    public void testHashCode() {
        ImmutableArray<String> array1 = ImmutableArray.of("a", "b", "c");
        ImmutableArray<String> array2 = ImmutableArray.of("a", "b", "c");
        ImmutableArray<String> array3 = ImmutableArray.of("a", "b", "d");

        Assertions.assertEquals(array1.hashCode(), array2.hashCode());
        Assertions.assertNotEquals(array1.hashCode(), array3.hashCode());
    }

    @Test
    public void testEquals() {
        ImmutableArray<String> array1 = ImmutableArray.of("a", "b", "c");
        ImmutableArray<String> array2 = ImmutableArray.of("a", "b", "c");
        ImmutableArray<String> array3 = ImmutableArray.of("a", "b", "d");
        ImmutableArray<String> array4 = ImmutableArray.of("a", "b");

        Assertions.assertEquals(array1, array2);
        Assertions.assertNotEquals(array1, array3);
        Assertions.assertNotEquals(array1, array4);
        Assertions.assertNotEquals(array1, null);
        Assertions.assertNotEquals(array1, "not an array");
    }

    @Test
    public void testToString() {
        ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
        String str = array.toString();

        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
        Assertions.assertTrue(str.contains("c"));
    }

    @Test
    public void testWithNullElements() {
        String[] original = { "a", null, "c" };
        ImmutableArray<String> array = ImmutableArray.wrap(original);

        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals("a", array.get(0));
        Assertions.assertNull(array.get(1));
        Assertions.assertEquals("c", array.get(2));
        Assertions.assertTrue(array.contains(null));
    }

    @Test
    public void testImplementsImmutable() {
        ImmutableArray<String> array = ImmutableArray.of("a");
        Assertions.assertTrue(array instanceof Immutable);
    }
}
