package com.landawn.abacus.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PrefixSearchTableTest extends TestBase {

    // ===== get(List) =====

    @Test
    public void testGet() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder()
                .add(Arrays.asList(1, 2), "bar")
                .add(Arrays.asList(1, 2, 3), "foo")
                .build();

        // Longest prefix match
        assertEquals("foo", table.get(Arrays.asList(1, 2, 3)).get());
        assertEquals("bar", table.get(Arrays.asList(1, 2)).get());

        // No match
        assertTrue(table.get(Arrays.asList(9)).isEmpty());
    }

    @Test
    public void testGet_emptyKeyThrows() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        assertThrows(IllegalArgumentException.class, () -> table.get(Arrays.asList()));
    }

    @Test
    public void testGet_nullKeyElementThrows() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();
        assertThrows(NullPointerException.class, () -> table.get(Arrays.asList(1, null)));
    }

    // ===== getAll(List) =====

    @Test
    public void testGetAll() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder()
                .add(Arrays.asList(1, 2, 3), "foo")
                .add(Arrays.asList(1, 2), "bar")
                .add(Arrays.asList(1, 2, 4), "baz")
                .add(Arrays.asList(2, 1, 3), "zoo")
                .build();

        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3)).toMap();
        assertEquals(2, map.size());
        assertTrue(map.equals(N.asMap(asList(1, 2), "bar", asList(1, 2, 3), "foo")));
    }

    @Test
    public void testGetAll_emptyTable() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        assertTrue(table.getAll(Arrays.asList(1)).toMap().isEmpty());
    }

    @Test
    public void testGetAll_singleKeyMatched() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, N.newImmutableEntry(Arrays.asList(1), "foo")));
    }

    @Test
    public void testGetAll_singleKeyNotMatched() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();
        assertTrue(table.getAll(Arrays.asList(2)).toMap().isEmpty());
    }

    @Test
    public void testGetAll_singleKeyMatchesPrefix() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, N.newImmutableEntry(Arrays.asList(1), "foo")));
    }

    @Test
    public void testGetAll_multipleKeysExactMatch() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2, 3), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, N.newImmutableEntry(Arrays.asList(1, 2, 3), "foo")));
    }

    @Test
    public void testGetAll_multipleKeysPrefixMatched() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2, 3), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3, 4, 5)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, N.newImmutableEntry(Arrays.asList(1, 2, 3), "foo")));
    }

    @Test
    public void testGetAll_multipleKeysLongerThanSearchKeySize() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2, 3), "foo").build();
        assertTrue(table.getAll(Arrays.asList(1, 2)).toMap().isEmpty());
    }

    @Test
    public void testGetAll_emptyKeyThrows() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        assertThrows(IllegalArgumentException.class, () -> table.getAll(Arrays.asList()));
    }

    // ===== toBuilder() =====

    @Test
    public void testToBuilder() {
        PrefixSearchTable<Integer, String> original = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2), "bar").build();

        PrefixSearchTable<Integer, String> rebuilt = original.toBuilder().build();
        assertEquals("bar", rebuilt.get(Arrays.asList(1, 2)).get());
    }

    @Test
    public void testToBuilder_empty() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build().toBuilder().build();
        assertTrue(table.getAll(Arrays.asList(1)).toMap().isEmpty());
        assertTrue(table.get(Arrays.asList(1)).isEmpty());
    }

    // ===== toString() =====

    @Test
    public void testToString() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();

        String str = table.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testToString_emptyTable() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        String str = table.toString();
        assertNotNull(str);
        assertEquals("{}", str);
    }

    // ===== Builder.add(List, V) =====

    @Test
    public void testAdd() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        builder.add(Arrays.asList(1, 2, 3), "foo");
        PrefixSearchTable<Integer, String> table = builder.build();
        assertEquals("foo", table.get(Arrays.asList(1, 2, 3)).get());
    }

    @Test
    public void testAdd_redundantMappingAllowed() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder()
                .add(Arrays.asList(1, 2, 3), "foo")
                .add(Arrays.asList(1), "bar")
                .add(Arrays.asList(1, 2, 3), "foo")
                .build();

        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3)).toMap();
        assertEquals(2, map.size());
        assertTrue(map.equals(N.asMap(Arrays.asList(1), "bar", Arrays.asList(1, 2, 3), "foo")));
    }

    // ===== Builder.addAll(Map) =====

    @Test
    public void testAddAll() {
        Map<List<Integer>, String> mappings = new HashMap<>();
        mappings.put(Arrays.asList(1, 2), "bar");
        mappings.put(Arrays.asList(3, 4), "baz");

        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().addAll(mappings).build();

        assertEquals("bar", table.get(Arrays.asList(1, 2)).get());
        assertEquals("baz", table.get(Arrays.asList(3, 4)).get());
    }

    // ===== builder() =====

    @Test
    public void testBuilder() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        assertNotNull(builder);
    }

    @Test
    public void testAddAll_emptyMap() {
        Map<List<Integer>, String> mappings = new HashMap<>();

        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().addAll(mappings).build();

        assertTrue(table.get(Arrays.asList(1)).isEmpty());
    }

    // ===== Builder.build() =====

    @Test
    public void testBuild() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();

        assertNotNull(table);
        assertEquals("foo", table.get(Arrays.asList(1)).get());
    }

    @Test
    public void testBuild_emptyBuilder() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        assertNotNull(table);
        assertTrue(table.get(Arrays.asList(1)).isEmpty());
    }

    @Test
    public void testAdd_emptyKeyThrows() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.add(Arrays.asList(), "foo"));
    }

    @Test
    public void testAdd_nullKeyElementThrows() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        assertThrows(NullPointerException.class, () -> builder.add(Arrays.asList(1, null), "foo"));
    }

    @Test
    public void testAdd_conflictingMappingDisallowed() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        builder.add(Arrays.asList(1, 2, 3), "foo");
        assertThrows(IllegalArgumentException.class, () -> builder.add(Arrays.asList(1, 2, 3), "bar"));
    }

    /**
     * Adding the same compound key with the same value twice must be a no-op (not a conflict).
     */
    @Test
    public void testAdd_sameValueRedundantAllowed() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder()
                .add(Arrays.asList(1, 2, 3), "foo")
                .add(Arrays.asList(1, 2, 3), "foo")
                .build();
        assertEquals("foo", table.get(Arrays.asList(1, 2, 3)).get());
    }

    /**
     * Verifies the longest-prefix-match contract when intermediate prefixes are NOT mapped.
     * Only the longest registered prefix should be the value of get(); getAll() should yield
     * only mapped prefixes, in ascending length order.
     */
    @Test
    public void testGet_longestPrefixWhenIntermediateUnmapped() {
        PrefixSearchTable<String, Integer> table = PrefixSearchTable.<String, Integer> builder()
                .add(Arrays.asList("a"), 1)
                .add(Arrays.asList("a", "b", "c"), 3)
                .build();
        // [a, b] is unmapped, but [a] and [a,b,c] are. Longest prefix of [a,b,c,d] is [a,b,c].
        assertEquals(3, table.get(Arrays.asList("a", "b", "c", "d")).get());

        Map<List<String>, Integer> all = table.getAll(Arrays.asList("a", "b", "c", "d")).toMap();
        assertEquals(2, all.size());
        assertEquals(1, all.get(Arrays.asList("a")));
        assertEquals(3, all.get(Arrays.asList("a", "b", "c")));
    }

    /**
     * Trie traversal must be case-sensitive when keys are case-sensitive Strings.
     * Demonstrates that key equality is delegated to the K type's equals/hashCode.
     */
    @Test
    public void testGet_caseSensitiveKeys() {
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder()
                .add(Arrays.asList("Foo", "Bar"), "match")
                .build();
        assertEquals("match", table.get(Arrays.asList("Foo", "Bar")).get());
        // Different case must not match - equals on String is case-sensitive
        assertTrue(table.get(Arrays.asList("foo", "bar")).isEmpty());
        assertTrue(table.get(Arrays.asList("FOO", "BAR")).isEmpty());
    }

    /** Iterator returned by getAll throws NoSuchElementException after exhaustion. */
    @Test
    public void testGetAll_iteratorExhaustionThrows() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1), "foo").build();
        java.util.Iterator<Map.Entry<List<Integer>, String>> it = table.getAll(Arrays.asList(1)).iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(java.util.NoSuchElementException.class, it::next);
    }
}
