package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalByte;

/** ByteStream mapping contracts. */
public class ByteStreamMappingTest extends ByteStreamTestSupport {

    @Test
    public void testAsIntStream() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.asIntStream().toArray());

        ByteStream stream2 = ByteStream.of((byte) -1, (byte) 0, (byte) 127);
        assertArrayEquals(new int[] { -1, 0, 127 }, stream2.asIntStream().toArray());
    }

    @Test
    public void testAsIntStream_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        int[] result = stream.asIntStream().toArray();
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testBoxed() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> boxed = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), boxed);
    }

    @Test
    public void testBoxed_Verify() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = stream.boxed().toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), result);
    }

    @Test
    public void testFlatmap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, stream.flatMapArray(b -> new byte[] { b, (byte) (b * 10) }).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertEquals(2, stream2.flatMapArray(b -> new byte[] { b }).count());
    }

    @Test
    public void testFlatMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, stream.flatMap(b -> ByteStream.of(b, (byte) (b * 10))).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, stream2.flatMap(b -> ByteStream.of(b)).toArray());
    }

    @Test
    public void testFlatmapCollection() {
        // multi-element happy path
        assertArrayEquals(new byte[] { 1, 10, 2, 20 }, createByteStream((byte) 1, (byte) 2).flatmap(b -> Arrays.asList((Byte) b, (byte) (b * 10))).toArray());

        // single element
        assertArrayEquals(new byte[] { 7, 8 }, createByteStream((byte) 7).flatmap(b -> Arrays.asList((Byte) b, (byte) 8)).toArray());

        // empty stream
        assertArrayEquals(new byte[] {}, createByteStream(new byte[] {}).flatmap(b -> Arrays.asList((Byte) b)).toArray());

        // empty collection from mapper - element contributes nothing
        assertArrayEquals(new byte[] {}, createByteStream((byte) 1, (byte) 2).flatmap(b -> java.util.Collections.<Byte> emptyList()).toArray());

        // null collection from mapper - treated as empty (matches Stream.flatmap)
        assertArrayEquals(new byte[] {}, createByteStream((byte) 1, (byte) 2).flatmap(b -> (Collection<Byte>) null).toArray());

        // collection with all null elements -> all become (byte) 0
        assertArrayEquals(new byte[] { 0, 0 }, createByteStream((byte) 1).flatmap(b -> Arrays.asList((Byte) null, (Byte) null)).toArray());

        // collection mixing nulls and real values
        assertArrayEquals(new byte[] { 0, 5, 0 }, createByteStream((byte) 1).flatmap(b -> Arrays.asList((Byte) null, (byte) 5, (Byte) null)).toArray());

        // count with nulls included
        assertEquals(4, createByteStream((byte) 1, (byte) 2).flatmap(b -> Arrays.asList((Byte) null, b)).count());
    }

    @Test
    public void testFlatmapNullByteElementEmittedAsZero() {
        final byte[] result = ByteStream.of((byte) 1, (byte) 2).flatmap(b -> Arrays.asList((Byte) null, b)).toArray();

        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 0, 1, 0, 2 }, result);
    }

    @Test
    public void testFlatmapArray() {
        assertEquals(10, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9, ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).skip(1).count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.toList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).skip(1).toList());

        assertEquals(10,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).count());
        assertEquals(9,
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArray(e -> new byte[] { e, (byte) (e * 2) })
                        .skip(1)
                        .count());
        assertArrayEquals(new byte[] { 1, 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).toArray());
        assertArrayEquals(new byte[] { 2, 2, 4, 3, 6, 4, 8, 5, 10 },
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArray(e -> new byte[] { e, (byte) (e * 2) })
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((byte) 1, (byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).map(e -> e).flatMapArray(e -> new byte[] { e, (byte) (e * 2) }).toList());
        assertEquals(N.toList((byte) 2, (byte) 2, (byte) 4, (byte) 3, (byte) 6, (byte) 4, (byte) 8, (byte) 5, (byte) 10),
                ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5)
                        .map(e -> e)
                        .flatMapArray(e -> new byte[] { e, (byte) (e * 2) })
                        .skip(1)
                        .toList());
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        ByteStream stream = createByteStream((byte) 1, (byte) 2);
        List<String> result = stream.flatMapArrayToObj(b -> new String[] { "x" + b, "y" + b }).toList();
        assertEquals(4, result.size());
        assertEquals("x1", result.get(0));
        assertEquals("y1", result.get(1));
        assertEquals("x2", result.get(2));
        assertEquals("y2", result.get(3));
    }

    @Test
    public void testFlatmapByteArray() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<Byte> result = byteStream.flatMapArray(b -> new byte[] { b, (byte) (b * 10) }).toList();
        assertEquals(Arrays.asList((byte) 1, (byte) 10, (byte) 2, (byte) 20), result);
    }

    @Test
    public void testFlatMapToInt() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        assertArrayEquals(new int[] { 1, 10, 2, 20 }, stream.flatMapToInt(b -> IntStream.of(b, b * 10)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 3, (byte) 4);
        assertEquals(2, stream2.flatMapToInt(b -> IntStream.of(b)).count());
    }

    @Test
    public void testFlatMapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<String> result = stream.flatMapToObj(b -> Stream.of("byte:" + b, "doubled:" + (b * 2))).toList();
        assertEquals(Arrays.asList("byte:1", "doubled:2", "byte:2", "doubled:4"), result);
    }

    @Test
    public void testFlatmapToObj() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2);
        List<Integer> result = stream.flatmapToObj(b -> Arrays.asList((int) b, b * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20), result);
    }

    @Test
    public void testFlatmapToObjCollection() {
        byteStream = createByteStream(new byte[] { 1, 2 });
        List<String> result = byteStream.flatmapToObj(b -> Arrays.asList("C" + b, "D" + b)).toList();
        assertEquals(Arrays.asList("C1", "D1", "C2", "D2"), result);
    }

    @Test
    public void testMap() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 2, 4, 6 }, stream.map(b -> (byte) (b * 2)).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream2.map(b -> b).toArray());

        ByteStream emptyStream = ByteStream.empty();
        assertEquals(0, emptyStream.map(b -> (byte) (b * 2)).count());
    }

    @Test
    public void testMapPartial() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertArrayEquals(new byte[] { 2, 4 }, stream.mapPartial(b -> b % 2 == 0 ? OptionalByte.of(b) : OptionalByte.empty()).toArray());

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 3, (byte) 5);
        assertEquals(0, stream2.mapPartial(b -> OptionalByte.empty()).count());
    }

    @Test
    public void testMapToInt() {
        ByteStream stream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        assertArrayEquals(new int[] { 10, 20, 30 }, stream.mapToInt(b -> b * 10).toArray());

        ByteStream stream2 = ByteStream.of((byte) 5, (byte) 10, (byte) 15);
        assertArrayEquals(new int[] { 5, 10, 15 }, stream2.mapToInt(b -> b).toArray());
    }

    @Test
    public void testMapToObj() {
        ByteStream stream = ByteStream.of((byte) 65, (byte) 66, (byte) 67);
        List<String> result = stream.mapToObj(b -> String.valueOf((char) b)).toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);

        ByteStream stream2 = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        List<Integer> intList = stream2.mapToObj(b -> (int) b).toList();
        assertEquals(Arrays.asList(1, 2, 3), intList);
    }

    @Test
    public void testTransform() {
        byteStream = createByteStream(new byte[] { 1, 2, 3 });
        List<String> result = byteStream.transform(s -> s.mapToObj(b -> "Item" + b)).toList();
        assertEquals(Arrays.asList("Item1", "Item2", "Item3"), result);
    }

}
