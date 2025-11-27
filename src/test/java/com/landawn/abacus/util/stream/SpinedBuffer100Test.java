package com.landawn.abacus.util.stream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class SpinedBuffer100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        Assertions.assertEquals(0, buffer.size());
        Assertions.assertTrue(buffer.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>(20);
        Assertions.assertEquals(0, buffer.size());
        Assertions.assertTrue(buffer.isEmpty());
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>(0);
        Assertions.assertEquals(0, buffer.size());
        buffer.add("test");
        Assertions.assertEquals(1, buffer.size());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpinedBuffer<>(-1));
    }

    @Test
    public void testAccept() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        buffer.accept("test1");
        buffer.accept("test2");
        Assertions.assertEquals(2, buffer.size());

        Iterator<String> iter = buffer.iterator();
        Assertions.assertEquals("test1", iter.next());
        Assertions.assertEquals("test2", iter.next());
    }

    @Test
    public void testAdd() {
        SpinedBuffer<Integer> buffer = new SpinedBuffer<>();

        for (int i = 0; i < 5; i++) {
            Assertions.assertTrue(buffer.add(i));
        }

        Assertions.assertEquals(5, buffer.size());

        int count = 0;
        for (Integer value : buffer) {
            Assertions.assertEquals(count++, value);
        }
    }

    @Test
    public void testAddWithSpineGrowth() {
        SpinedBuffer<Integer> buffer = new SpinedBuffer<>(2);

        for (int i = 0; i < 100; i++) {
            buffer.add(i);
        }

        Assertions.assertEquals(100, buffer.size());

        int count = 0;
        for (Integer value : buffer) {
            Assertions.assertEquals(count++, value);
        }
    }

    @Test
    public void testIteratorEmptyBuffer() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        Iterator<String> iter = buffer.iterator();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testIteratorSingleChunk() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        buffer.add("a");
        buffer.add("b");
        buffer.add("c");

        Iterator<String> iter = buffer.iterator();
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testIteratorMultipleChunks() {
        SpinedBuffer<Integer> buffer = new SpinedBuffer<>(3);

        for (int i = 0; i < 30; i++) {
            buffer.add(i);
        }

        Iterator<Integer> iter = buffer.iterator();
        for (int i = 0; i < 30; i++) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals(i, iter.next());
        }
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testSize() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        Assertions.assertEquals(0, buffer.size());

        buffer.add("test");
        Assertions.assertEquals(1, buffer.size());

        for (int i = 0; i < 99; i++) {
            buffer.add("item" + i);
        }
        Assertions.assertEquals(100, buffer.size());
    }

    @Test
    public void testRemoveUnsupported() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        buffer.add("test");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> buffer.remove("test"));
    }

    @Test
    public void testRemoveAllUnsupported() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();
        buffer.add("test");
        List<String> toRemove = Arrays.asList("test");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> buffer.removeAll(toRemove));
    }

    @Test
    public void testOfIntDefaultConstructor() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfIntConstructorWithCapacity() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt(20);
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfIntConstructorWithNegativeCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpinedBuffer.OfInt(-1));
    }

    @Test
    public void testOfIntAccept() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
        buffer.accept(10);
        buffer.accept(20);
        Assertions.assertEquals(2, buffer.size());

        IntIterator iter = buffer.iterator();
        Assertions.assertEquals(10, iter.nextInt());
        Assertions.assertEquals(20, iter.nextInt());
    }

    @Test
    public void testOfIntAdd() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();

        for (int i = 0; i < 50; i++) {
            Assertions.assertTrue(buffer.add(i));
        }

        Assertions.assertEquals(50, buffer.size());

        IntIterator iter = buffer.iterator();
        for (int i = 0; i < 50; i++) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals(i, iter.nextInt());
        }
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfIntIteratorEmpty() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();
        IntIterator iter = buffer.iterator();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testOfIntSpineGrowth() {
        SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt(2);

        for (int i = 0; i < 100; i++) {
            buffer.add(i);
        }

        Assertions.assertEquals(100, buffer.size());

        IntIterator iter = buffer.iterator();
        for (int i = 0; i < 100; i++) {
            Assertions.assertEquals(i, iter.nextInt());
        }
    }

    @Test
    public void testOfLongDefaultConstructor() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfLongConstructorWithCapacity() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong(20);
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfLongConstructorWithNegativeCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpinedBuffer.OfLong(-1));
    }

    @Test
    public void testOfLongAccept() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
        buffer.accept(10L);
        buffer.accept(20L);
        Assertions.assertEquals(2, buffer.size());

        LongIterator iter = buffer.iterator();
        Assertions.assertEquals(10L, iter.nextLong());
        Assertions.assertEquals(20L, iter.nextLong());
    }

    @Test
    public void testOfLongAdd() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();

        for (long i = 0; i < 50; i++) {
            Assertions.assertTrue(buffer.add(i));
        }

        Assertions.assertEquals(50, buffer.size());

        LongIterator iter = buffer.iterator();
        for (long i = 0; i < 50; i++) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals(i, iter.nextLong());
        }
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfLongIteratorEmpty() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();
        LongIterator iter = buffer.iterator();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testOfLongSpineGrowth() {
        SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong(2);

        for (long i = 0; i < 100; i++) {
            buffer.add(i);
        }

        Assertions.assertEquals(100, buffer.size());

        LongIterator iter = buffer.iterator();
        for (long i = 0; i < 100; i++) {
            Assertions.assertEquals(i, iter.nextLong());
        }
    }

    @Test
    public void testOfDoubleDefaultConstructor() {
        SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfDoubleConstructorWithCapacity() {
        SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble(20);
        Assertions.assertEquals(0, buffer.size());
    }

    @Test
    public void testOfDoubleConstructorWithNegativeCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpinedBuffer.OfDouble(-1));
    }

    @Test
    public void testOfDoubleAccept() {
        SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
        buffer.accept(10.5);
        buffer.accept(20.5);
        Assertions.assertEquals(2, buffer.size());

        DoubleIterator iter = buffer.iterator();
        Assertions.assertEquals(10.5, iter.nextDouble());
        Assertions.assertEquals(20.5, iter.nextDouble());
    }

    @Test
    public void testOfDoubleAdd() {
        SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();

        for (int i = 0; i < 50; i++) {
            Assertions.assertTrue(buffer.add(i * 1.1));
        }

        Assertions.assertEquals(50, buffer.size());

        DoubleIterator iter = buffer.iterator();
        for (int i = 0; i < 50; i++) {
            Assertions.assertTrue(iter.hasNext());
            Assertions.assertEquals(i * 1.1, iter.nextDouble(), 0.0001);
        }
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testOfDoubleIteratorEmpty() {
        SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();
        DoubleIterator iter = buffer.iterator();
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testOfDoubleSpineGrowth() {
        {
            SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble(2);

            for (int i = 0; i < 100; i++) {
                buffer.add(i * 0.5);
            }

            Assertions.assertEquals(100, buffer.size());

            DoubleList list = buffer.iterator().toList();
            N.println(list);
        }

        {
            SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble(2);

            for (int i = 0; i < 100; i++) {
                buffer.add(i * 0.5);
            }

            Assertions.assertEquals(100, buffer.size());

            DoubleIterator iter = buffer.iterator();
            for (int i = 0; i < 100; i++) {
                Assertions.assertEquals(i * 0.5, iter.nextDouble(), 0.0001);
            }
        }
    }

    @Test
    public void testCollectionInterface() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>();

        Assertions.assertTrue(buffer.isEmpty());

        buffer.add("test");
        Assertions.assertFalse(buffer.isEmpty());

        Assertions.assertTrue(buffer.contains("test"));
        Assertions.assertFalse(buffer.contains("notfound"));

        buffer.add("test2");
        Object[] array = buffer.toArray();
        Assertions.assertEquals(2, array.length);
        Assertions.assertEquals("test", array[0]);
        Assertions.assertEquals("test2", array[1]);
    }

    @Test
    public void testLargeDataset() {
        SpinedBuffer<Integer> buffer = new SpinedBuffer<>(1);

        int total = 10000;
        for (int i = 0; i < total; i++) {
            buffer.add(i);
        }

        Assertions.assertEquals(total, buffer.size());

        int count = 0;
        for (Integer value : buffer) {
            Assertions.assertEquals(count++, value);
        }
        Assertions.assertEquals(total, count);
    }

    @Test
    public void testZeroCapacityWithGrowth() {
        SpinedBuffer<String> buffer = new SpinedBuffer<>(0);

        for (int i = 0; i < 20; i++) {
            buffer.add("item" + i);
        }

        Assertions.assertEquals(20, buffer.size());

        Iterator<String> iter = buffer.iterator();
        for (int i = 0; i < 20; i++) {
            Assertions.assertEquals("item" + i, iter.next());
        }
    }
}
