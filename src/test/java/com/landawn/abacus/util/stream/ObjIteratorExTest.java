package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjIteratorExTest extends TestBase {

    @Test
    public void testEmptySingleton() {
        ObjIteratorEx<String> empty1 = ObjIteratorEx.empty();
        ObjIteratorEx<Integer> empty2 = ObjIteratorEx.empty();
        Assertions.assertSame(empty1, empty2);
    }

    @Test
    public void testEmpty() {
        ObjIteratorEx<String> iter = ObjIteratorEx.empty();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
        Assertions.assertEquals(0, iter.count());
        iter.close();
    }

    @Test
    public void testEmptyAdvanceAndCount() {
        ObjIteratorEx<String> empty = ObjIteratorEx.empty();
        empty.advance(5); // should not throw
        Assertions.assertEquals(0, empty.count());
        Assertions.assertFalse(empty.hasNext());
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
    public void testToList() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        List<String> result = iter.toList();
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollectionCount() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIteratorEx<String> iter = ObjIteratorEx.of(list);
        // Collection-backed iterator should support count via iteration
        Assertions.assertEquals("a", iter.next());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithSubRangeAdvanceAndCount() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 5);

        iter.advance(2);
        Assertions.assertEquals(3, iter.count()); // remaining after advance
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayToArraySmall() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 3);

        String[] result = iter.toArray(new String[0]);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayToList() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        List<String> result = iter.toList();
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayNull() {
        String[] nullArray = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullArray);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfArrayWithIndicesEmpty() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 1, 1);
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfCollectionNull() {
        Collection<String> nullCollection = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullCollection);
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
    public void testOfIterableNull() {
        Iterable<String> nullIterable = null;
        ObjIteratorEx<String> iter = ObjIteratorEx.of(nullIterable);
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
    public void testOfEmptyArray() {
        String[] emptyArray = {};
        ObjIteratorEx<String> iter = ObjIteratorEx.of(emptyArray);
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertEquals(0, iter.count());
    }

    @Test
    public void testAdvanceNegative() {
        String[] array = { "a", "b", "c" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array);

        // Advancing by negative or zero should have no effect
        iter.advance(-1);
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testOfArrayToArrayLarge() {
        String[] array = { "x", "y" };
        ObjIteratorEx<String> iter = ObjIteratorEx.of(array, 0, 2);

        String[] output = new String[5];
        String[] result = iter.toArray(output);
        Assertions.assertSame(output, result);
        Assertions.assertEquals("x", result[0]);
        Assertions.assertEquals("y", result[1]);
        Assertions.assertNull(result[2]); // null sentinel
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
    public void testOfArrayWithIndicesInvalid() {
        String[] array = { "a", "b", "c" };
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> ObjIteratorEx.of(array, 0, 4));
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
    public void testDeferAdvance_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx), so defer falls back to super.advance
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(1);
        Assertions.assertEquals("b", iter.next());
    }

    @Test
    public void testDeferCount_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx)
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertEquals(3, iter.count());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDeferLazyInitialization() {
        boolean[] initialized = { false };
        Supplier<Iterator<String>> supplier = () -> {
            initialized[0] = true;
            return Arrays.asList("a", "b").iterator();
        };
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        Assertions.assertFalse(initialized[0]); // not yet initialized
        iter.hasNext(); // triggers init
        Assertions.assertTrue(initialized[0]);
    }

    @Test
    public void testDeferAdvanceZero() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(0); // should not advance
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testDeferAdvanceNegative() {
        String[] array = { "a", "b", "c" };
        Supplier<Iterator<String>> supplier = () -> ObjIteratorEx.of(array);
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        iter.advance(-5); // should not advance
        Assertions.assertEquals("a", iter.next());
    }

    @Test
    public void testDeferNullSupplier() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ObjIteratorEx.defer(null));
    }

    @Test
    public void testDeferClose_NonObjIteratorEx() {
        List<String> list = Arrays.asList("a", "b", "c");
        // Supplier returns a plain Iterator (not ObjIteratorEx), close should not throw
        Supplier<Iterator<String>> supplier = () -> list.iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);

        // Force initialization by calling hasNext
        Assertions.assertTrue(iter.hasNext());
        iter.close();
        Assertions.assertNotNull(iter);
    }

    @Test
    public void testDeferCloseBeforeInit() {
        Supplier<Iterator<String>> supplier = () -> Arrays.asList("a").iterator();
        ObjIteratorEx<String> iter = ObjIteratorEx.defer(supplier);
        // Close without using - should init and close without exception
        iter.close();
    }

    @Test
    public void testClose() {
        ObjIteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c");
        iter.close();
        assertNotNull(iter);
    }

}
