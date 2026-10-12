package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Suppliers;

/** CharStream collection contracts. */
public class CharStreamCollectionTest extends CharStreamTestSupport {

    // collect

    @Test
    public void testCollect() {
        StringBuilder sb = createCharStream('a', 'b', 'c').collect(StringBuilder::new, (builder, c) -> builder.append(c), (b1, b2) -> b1.append(b2));
        assertThat(sb.toString()).isEqualTo("abc");
    }

    @Test
    public void testCollect_ThreeArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        StringBuilder result = stream.collect(StringBuilder::new, (sb, c) -> sb.append(c), (sb1, sb2) -> sb1.append(sb2));
        assertEquals("abc", result.toString());
    }

    @Test
    public void testCollect_TwoArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        StringBuilder result = stream.collect(StringBuilder::new, (sb, c) -> sb.append(c));
        assertEquals("abc", result.toString());
    }

    @Test
    public void testCollectSupplierAccumulator() {
        CharList list = createCharStream(new char[] { 'd', 'e', 'f' }).collect(CharList::new, CharList::add);
        assertEquals(CharList.of('d', 'e', 'f'), list);

        list = createCharStream(new char[] {}).collect(CharList::new, CharList::add);
        assertEquals(0, list.size());
    }

    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        StringBuilder sb = createCharStream(new char[] { 'a', 'b', 'c' }).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("abc", sb.toString());

        sb = createCharStream(new char[] {}).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("", sb.toString());
    }

    // groupTo

    @Test
    public void testGroupTo() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b');
        Map<Character, Long> result = stream.groupTo(c -> c, Collectors.counting());
        assertEquals(3, result.size());
        assertEquals(2L, result.get('a'));
        assertEquals(2L, result.get('b'));
        assertEquals(1L, result.get('c'));
    }

    @Test
    public void testGroupTo_WithMapFactory() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b');
        Map<Character, Long> result = stream.groupTo(c -> c, Collectors.counting(), Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testGroupToWithMapFactory() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0, Collectors.toList(),
                Suppliers.ofTreeMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testToArray() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testToCharList() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharList result = stream.toCharList();
        assertEquals(CharList.of('a', 'b', 'c'), result);
    }

    @Test
    public void testToCollection() {
        CharStream stream = createCharStream('a', 'b', 'c');
        java.util.LinkedList<Character> result = stream.toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
    }

    @Test
    public void testToImmutableList() {
        CharStream stream = createCharStream('a', 'b', 'c');
        com.landawn.abacus.util.ImmutableList<Character> result = stream.toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
        assertEquals(Character.valueOf('b'), result.get(1));
        assertEquals(Character.valueOf('c'), result.get(2));
    }

    @Test
    public void testToImmutableList_EmptyStream() {
        com.landawn.abacus.util.ImmutableList<Character> result = CharStream.empty().toImmutableList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToImmutableSet() {
        CharStream stream = createCharStream('a', 'b', 'c', 'a');
        com.landawn.abacus.util.ImmutableSet<Character> result = stream.toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    public void testToImmutableSet_EmptyStream() {
        com.landawn.abacus.util.ImmutableSet<Character> result = CharStream.empty().toImmutableSet();
        assertEquals(0, result.size());
    }

    @Test
    public void testToList() {
        List<Character> list = createCharStream('a', 'b', 'c').toList();
        assertThat(list).containsExactly('a', 'b', 'c');
    }

    // toMap

    @Test
    public void testToMap() {
        Map<Character, Integer> map = createCharStream('a', 'b', 'c').toMap(c -> c, c -> c - 'a');
        assertThat(map).containsExactly(entry('a', 0), entry('b', 1), entry('c', 2));
    }

    @Test
    public void testToMap_KeyAndValueMappers() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c));
        assertEquals(3, result.size());
        assertEquals("a", result.get('a'));
        assertEquals("b", result.get('b'));
        assertEquals("c", result.get('c'));
    }

    @Test
    public void testToMap_WithMapFactory() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testToMap_WithMapFactoryAndMergeFunction() {
        CharStream stream = createCharStream('a', 'b', 'a');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2, result.size());
        assertEquals("aa", result.get('a'));
    }

    @Test
    public void testToMap_WithMergeFunction() {
        CharStream stream = createCharStream('a', 'b', 'a');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals("aa", result.get('a'));
        assertEquals("b", result.get('b'));
    }

    @Test
    public void testToMapKeyAndValueMapperAndMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testToMapKeyAndValueMapperDuplicateKeys() {
        assertThrows(IllegalStateException.class, () -> createCharStream(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c));
    }

    @Test
    public void testToMultiset() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> result = stream.toMultiset();
        assertEquals(3, result.getCount('a'));
        assertEquals(2, result.getCount('b'));
        assertEquals(1, result.getCount('c'));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        CharStream stream = createCharStream('a', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> result = stream.toMultiset(com.landawn.abacus.util.Multiset::new);
        assertEquals(2, result.getCount('a'));
        assertEquals(1, result.getCount('b'));
    }

    @Test
    public void testToSet() {
        Set<Character> set = createCharStream('a', 'b', 'a', 'c').toSet();
        assertThat(set).containsExactlyInAnyOrder('a', 'b', 'c');
    }

}
