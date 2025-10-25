package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ObjIteratorEx100Test extends TestBase {

    @Test
    public void testEmpty() {
        ObjIteratorEx<String> iter = ObjIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
        Assertions.assertEquals(0, iter.count());
        iter.close();
    }

    @Test
    public void testOfArray() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testOfArrayNull() {
        String[] nullArray = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullArray);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndices() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 4);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("d", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesInvalid() {
        String[] array = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 0, 4));
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollectionNull() {
        Collection<String> nullCollection = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullCollection);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list.iterator());

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIteratorNull() {
        Iterator<String> nullIterator = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullIterator);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIteratorAlreadyObjIteratorEx() {
        ObjIteratorEx<String> original = ObjIteratorEx.of("a", "b", "c");
        ObjIteratorEx<String> wrapped = ObjIteratorEx.of(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testOfIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of((Iterable<String>) list);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIterableNull() {
        Iterable<String> nullIterable = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullIterable);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer() {
        List<String> list = Arrays.asList("a", "b", "c");
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferWithObjIteratorEx() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(1);
        Assertions.assertEquals("b", iter.next());

        Assertions.assertEquals(1, iter.count());

        iter.close();
    }

    @Test
    public void testDeferNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIteratorEx.defer(null));
    }

    @Test
    public void testAdvance() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        iter.advance(2);
        Assertions.assertEquals("c", iter.next());

        iter.advance(1);
        Assertions.assertEquals("e", iter.next());

        iter.advance(10);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testAdvanceZero() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        iter.advance(0);
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testCount() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 4);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        String[] result = iter.toArray(new String[0]);
        Assertions.assertArrayEquals(array, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testToArrayWithSufficientSize() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        String[] output = new String[5];
        String[] result = iter.toArray(output);
        Assertions.assertSame(output, result);
        Assertions.assertEquals("a", result[0]);
        Assertions.assertEquals("b", result[1]);
        Assertions.assertEquals("c", result[2]);
    }

    @Test
    public void testToList() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        List<String> result = iter.toList();
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testClose() {
        ObjIteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c");
        iter.close();
    }
}
