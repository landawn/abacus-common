package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalShort;

/** ShortStream mapping contracts. */
public class ShortStreamMappingTest extends ShortStreamTestSupport {

    @Test
    public void testAsIntStream() {
        IntStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).asIntStream();
        assertArrayEquals(new int[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testAsIntStream_Empty() {
        int[] result = ShortStream.empty().asIntStream().toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testBoxed() {
        Stream<Short> stream = ShortStream.of((short) 1, (short) 2, (short) 3).boxed();
        assertArrayEquals(new Short[] { 1, 2, 3 }, stream.toArray());
    }

    @Test
    public void testBoxed_Empty() {
        List<Short> result = ShortStream.empty().boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmap_Empty() {
        short[] result = ShortStream.empty().flatMapArray(s -> new short[] { s, (short) (s * 2) }).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).flatMapArray(n -> new short[] { n, (short) (n + 10) });
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2).flatMap(n -> ShortStream.of(n, (short) (n + 10)));
        assertArrayEquals(new short[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatMap_Empty() {
        short[] result = ShortStream.empty().flatMap(s -> ShortStream.of(s, (short) (s * 2))).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatmapCollection() {
        // multi
        assertArrayEquals(new short[] { 1, 10, 2, 20 },
                ShortStream.of((short) 1, (short) 2).flatmap(n -> Arrays.asList((Short) n, (short) (n * 10))).toArray());

        // single
        assertArrayEquals(new short[] { 7, 9 }, ShortStream.of((short) 7).flatmap(n -> Arrays.asList((Short) n, (short) 9)).toArray());

        // empty stream
        assertArrayEquals(new short[] {}, ShortStream.empty().flatmap(n -> Arrays.asList((Short) n)).toArray());

        // empty collection
        assertArrayEquals(new short[] {}, ShortStream.of((short) 1, (short) 2).flatmap(n -> java.util.Collections.<Short> emptyList()).toArray());

        // null collection
        assertArrayEquals(new short[] {}, ShortStream.of((short) 1).flatmap(n -> (java.util.Collection<Short>) null).toArray());

        // null elements -> (short) 0
        assertArrayEquals(new short[] { 0, 5, 0 }, ShortStream.of((short) 1).flatmap(n -> Arrays.asList((Short) null, (short) 5, (Short) null)).toArray());

        assertEquals(4, ShortStream.of((short) 1, (short) 2).flatmap(n -> Arrays.asList((Short) null, n)).count());
    }

    @Test
    @DisplayName("Test flatMapArray() with array")
    public void testFlatmapArray() {
        ShortStream flattened = stream.flatMapArray(n -> new short[] { n, (short) (n * 10) });
        assertArrayEquals(new short[] { 1, 10, 2, 20, 3, 30, 4, 40, 5, 50 }, flattened.toArray());
    }

    // flatMapArrayToObj

    @Test
    public void testFlatMapArrayToObj_Empty() {
        List<String> result = ShortStream.empty().flatMapArrayToObj(s -> new String[] { "A" + s }).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        ShortStream stream = createShortStream((short) 1, (short) 2);
        List<String> result = stream.flatMapArrayToObj(s -> new String[] { "x" + s, "y" + s }).toList();
        assertEquals(4, result.size());
        assertEquals("x1", result.get(0));
        assertEquals("y1", result.get(1));
    }

    @Test
    public void testFlatMapArrayToObjMethod() {
        List<String> result = createShortStream((short) 1, (short) 2).flatMapArrayToObj(s -> new String[] { "A" + s, "B" + s }).toList();
        assertEquals(4, result.size());
        assertEquals("A1", result.get(0));
        assertEquals("B1", result.get(1));
    }

    @Test
    public void testFlatMapToInt() {
        IntStream stream = ShortStream.of((short) 1, (short) 2).flatMapToInt(n -> IntStream.of(n, n + 10));
        assertArrayEquals(new int[] { 1, 11, 2, 12 }, stream.toArray());
    }

    @Test
    public void testFlatMapToInt_Empty() {
        int[] result = ShortStream.empty().flatMapToInt(s -> IntStream.of((int) s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testFlatMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatMapToObj(n -> Stream.of("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    public void testFlatMapToObj_Empty() {
        List<String> result = ShortStream.empty().flatMapToObj(s -> Stream.of(String.valueOf(s))).toList();
        assertTrue(result.isEmpty());
    }

    // flatmapToObj

    @Test
    public void testFlatmapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2).flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2" }, stream.toArray());
    }

    @Test
    @DisplayName("Test flatmapToObj() with collection")
    public void testFlatmapToObjCollection() {
        Stream<String> flattened = stream.flatmapToObj(n -> Arrays.asList("A" + n, "B" + n));
        List<String> result = flattened.toList();
        assertEquals(Arrays.asList("A1", "B1", "A2", "B2", "A3", "B3", "A4", "B4", "A5", "B5"), result);
    }

    @Test
    public void testFlatmapToObjMethod() {
        List<String> result = createShortStream((short) 1, (short) 2).flatmapToObj(s -> Arrays.asList("X" + s, "Y" + s)).toList();
        assertEquals(4, result.size());
        assertEquals("X1", result.get(0));
        assertEquals("Y1", result.get(1));
    }

    @Test
    public void testMap() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).map(n -> (short) (n * 2));
        assertArrayEquals(new short[] { 2, 4, 6 }, stream.toArray());
    }

    @Test
    public void testMap_Empty() {
        short[] result = ShortStream.empty().map(s -> (short) (s * 2)).toArray();
        assertEquals(0, result.length);
    }

    // mapPartial

    @Test
    public void testMapPartial() {
        ShortStream stream = ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)
                .mapPartial(n -> n % 2 == 0 ? OptionalShort.of((short) (n * 10)) : OptionalShort.empty());
        assertArrayEquals(new short[] { 20, 40 }, stream.toArray());
    }

    @Test
    public void testMapPartial_AllEmpty() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).mapPartial(s -> OptionalShort.empty()).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapPartial_AllPresent() {
        short[] result = createShortStream((short) 1, (short) 2, (short) 3).mapPartial(s -> OptionalShort.of((short) (s * 10))).toArray();
        assertArrayEquals(new short[] { 10, 20, 30 }, result);
    }

    @Test
    public void testMapPartial_Empty() {
        short[] result = ShortStream.empty().mapPartial(s -> OptionalShort.of(s)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToInt() {
        IntStream stream = ShortStream.of((short) 1, (short) 2, (short) 3).mapToInt(n -> n * 10);
        assertArrayEquals(new int[] { 10, 20, 30 }, stream.toArray());
    }

    @Test
    public void testMapToInt_Empty() {
        int[] result = ShortStream.empty().mapToInt(s -> (int) s).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testMapToObj() {
        Stream<String> stream = ShortStream.of((short) 1, (short) 2, (short) 3).mapToObj(n -> "S" + n);
        assertArrayEquals(new String[] { "S1", "S2", "S3" }, stream.toArray());
    }

    @Test
    public void testMapToObj_Empty() {
        List<String> result = ShortStream.empty().mapToObj(String::valueOf).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransform() {
        IntStream result = createShortStream((short) 1, (short) 2, (short) 3).transform(s -> s.mapToInt(n -> n * 10));

        assertArrayEquals(new int[] { 10, 20, 30 }, result.toArray());
    }

    @Test
    public void testTransformMethod() {
        long count = createShortStream((short) 1, (short) 2, (short) 3).transform(s -> s.filter(v -> v > 1)).count();
        assertEquals(2, count);
    }

}
