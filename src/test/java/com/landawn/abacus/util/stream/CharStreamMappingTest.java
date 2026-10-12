package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.u.OptionalChar;

/** CharStream mapping contracts. */
public class CharStreamMappingTest extends CharStreamTestSupport {

    @Test
    public void testAsIntStream() {
        CharStream stream = createCharStream('a', 'b', 'c');
        IntStream result = stream.asIntStream();
        assertArrayEquals(new int[] { 97, 98, 99 }, result.toArray());
    }

    @Test
    public void testBoxed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> result = stream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testFlatMap() {
        CharStream stream = createCharStream('a', 'b');
        CharStream result = stream.flatMap(c -> CharStream.of(c, Character.toUpperCase(c)));
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, result.toArray());
    }

    @Test
    public void testFlatmapCollection() {
        // multi-element
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, createCharStream('a', 'b').flatmap(c -> Arrays.asList(c, Character.toUpperCase(c))).toArray());

        // single element
        assertArrayEquals(new char[] { 'x', 'y' }, createCharStream('x').flatmap(c -> Arrays.asList(c, 'y')).toArray());

        // empty stream
        assertArrayEquals(new char[] {}, createCharStream(new char[] {}).flatmap(c -> Arrays.asList((Character) c)).toArray());

        // empty collection
        assertArrayEquals(new char[] {}, createCharStream('a', 'b').flatmap(c -> java.util.Collections.<Character> emptyList()).toArray());

        // null collection
        assertArrayEquals(new char[] {}, createCharStream('a').flatmap(c -> (java.util.Collection<Character>) null).toArray());

        // null elements -> (char) 0
        char[] withNulls = createCharStream('a').flatmap(c -> Arrays.asList((Character) null, 'z')).toArray();
        assertEquals(2, withNulls.length);
        assertEquals((char) 0, withNulls[0]);
        assertEquals('z', withNulls[1]);

        // mixed nulls and values
        assertArrayEquals(new char[] { (char) 0, 'm', (char) 0 },
                createCharStream('a').flatmap(c -> Arrays.asList((Character) null, 'm', (Character) null)).toArray());
    }

    @Test
    public void testFlatmap_WithCharArray() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<char[]> mapper = c -> new char[] { c, Character.toUpperCase(c) };
        char[] result = stream.flatMapArray(mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, result);
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        CharStream stream = createCharStream('a', 'b');
        List<String> result = stream.flatMapArrayToObj(c -> new String[] { "x" + c, "y" + c }).toList();
        assertEquals(4, result.size());
        assertEquals("xa", result.get(0));
        assertEquals("ya", result.get(1));
    }

    @Test
    public void testFlatMapToInt() {
        CharStream stream = createCharStream('a', 'b');
        IntStream result = stream.flatMapToInt(c -> IntStream.of(c, Character.toUpperCase(c)));
        assertArrayEquals(new int[] { 97, 65, 98, 66 }, result.toArray());
    }

    @Test
    public void testFlatMapToObj() {
        CharStream stream = createCharStream('a', 'b');
        Stream<String> result = stream.flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(Character.toUpperCase(c))));
        assertEquals(Arrays.asList("a", "A", "b", "B"), result.toList());
    }

    @Test
    public void testFlatmapToObj_WithCollection() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<List<String>> mapper = c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c)));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testMap() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.map(c -> Character.toUpperCase(c)).toArray();
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, result);
    }

    @Test
    public void testMapPartial() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharFunction<OptionalChar> mapper = c -> c == 'b' ? OptionalChar.empty() : OptionalChar.of(Character.toUpperCase(c));
        char[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new char[] { 'A', 'C' }, result);
    }

    @Test
    public void testMapPartial_AllEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharFunction<OptionalChar> mapper = c -> OptionalChar.empty();
        char[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testMapToInt() {
        CharStream stream = createCharStream('a', 'b', 'c');
        int[] result = stream.mapToInt(c -> (int) c).toArray();
        assertArrayEquals(new int[] { 97, 98, 99 }, result);
    }

    @Test
    public void testMapToObj() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> result = stream.mapToObj(c -> String.valueOf(c)).toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testTransform() {
        CharStream stream = createCharStream('a', 'b', 'c');
        IntStream result = stream.transform(s -> s.mapToInt(c -> (int) c));
        assertNotNull(result);
        int[] arr = result.toArray();
        assertEquals(3, arr.length);
        assertEquals(97, arr[0]);
    }

}
