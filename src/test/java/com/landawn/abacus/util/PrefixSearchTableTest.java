package com.landawn.abacus.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.EntryStream;

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
        assertTrue(map.equals(CommonUtil.asMap(asList(1, 2), "bar", asList(1, 2, 3), "foo")));
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
        assertTrue(Maps.containsEntry(map, CommonUtil.newImmutableEntry(Arrays.asList(1), "foo")));
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
        assertTrue(Maps.containsEntry(map, CommonUtil.newImmutableEntry(Arrays.asList(1), "foo")));
    }

    @Test
    public void testGetAll_multipleKeysExactMatch() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2, 3), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, CommonUtil.newImmutableEntry(Arrays.asList(1, 2, 3), "foo")));
    }

    @Test
    public void testGetAll_multipleKeysPrefixMatched() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().add(Arrays.asList(1, 2, 3), "foo").build();
        Map<List<Integer>, String> map = table.getAll(Arrays.asList(1, 2, 3, 4, 5)).toMap();
        assertEquals(1, map.size());
        assertTrue(Maps.containsEntry(map, CommonUtil.newImmutableEntry(Arrays.asList(1, 2, 3), "foo")));
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
        assertTrue(map.equals(CommonUtil.asMap(Arrays.asList(1), "bar", Arrays.asList(1, 2, 3), "foo")));
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
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder().add(Arrays.asList("Foo", "Bar"), "match").build();
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

    @Test
    public void testGetAllMatchedPrefixIsSnapshot() {
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder().add(Arrays.asList("a", "b"), "value").build();
        List<String> query = new ArrayList<>(Arrays.asList("a", "b", "c"));

        Map.Entry<List<String>, String> match = table.getAll(query).iterator().next();
        query.set(0, "changed");

        assertEquals(Arrays.asList("a", "b"), match.getKey());
        assertThrows(UnsupportedOperationException.class, () -> match.getKey().set(0, "changed"));
    }

    @Test
    public void testGetAllSnapshotsQueryBeforeLazyTraversal() {
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder()
                .add(Arrays.asList("a"), "short")
                .add(Arrays.asList("a", "b"), "long")
                .build();
        List<String> query = new ArrayList<>(Arrays.asList("a", "b"));

        EntryStream<List<String>, String> matches = table.getAll(query);
        query.set(0, "changed");

        assertEquals(CommonUtil.asMap(Arrays.asList("a"), "short", Arrays.asList("a", "b"), "long"), matches.toMap());
    }

    @Test
    public void testGetAllRejectsEveryNullElementEagerly() {
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder().add(Arrays.asList("a"), "value").build();

        assertThrows(NullPointerException.class, () -> table.getAll(Arrays.asList("missing", null)));
    }

    @Test
    public void reviewFixes20260908_toStringRendersADeepTableWithoutExhaustingTheStack() throws Exception {
        // the rendered form is unchanged for an ordinary table
        PrefixSearchTable<String, String> small = PrefixSearchTable.<String, String> builder().add(Arrays.asList("a", "b"), "foo").build();
        assertEquals("{a=Node[value=null, children={b=Node[value=foo, children={}]}]}", small.toString());
        assertEquals("{}", PrefixSearchTable.<String, String> builder().build().toString());

        PrefixSearchTable.Builder<String, String> builder = PrefixSearchTable.<String, String> builder();
        builder.add(Arrays.asList("x"), "shallow");
        List<String> deepKey = new ArrayList<>();
        for (int i = 0; i < 4000; i++) {
            deepKey.add("k" + i);
        }
        PrefixSearchTable<String, String> deep = builder.add(deepKey, "deep").build();

        // a table this class can build must also be printable: the recursive rendering (HashMap -> record Node ->
        // children -> ...) costs several frames per level and overflows a small stack long before this depth.
        final String[] rendered = new String[1];
        final Throwable[] failure = new Throwable[1];
        Thread renderer = new Thread(null, () -> {
            try {
                rendered[0] = deep.toString();
            } catch (Throwable t) { // NOSONAR - StackOverflowError is exactly what this pins
                failure[0] = t;
            }
        }, "prefix-search-table-toString", 128 * 1024);
        renderer.start();
        renderer.join(60_000);

        assertNull(failure[0], () -> "toString failed with " + failure[0]);
        assertNotNull(rendered[0]);
        assertTrue(rendered[0].startsWith("{"), rendered[0].substring(0, 40));
        assertTrue(rendered[0].endsWith("}"));
        assertTrue(rendered[0].contains("x=Node[value=shallow, children={}]"));
        assertTrue(rendered[0].contains("k0=Node[value=null, children={k1="), rendered[0].substring(0, 80));
        assertTrue(rendered[0].contains("k3999=Node[value=deep, children={}]"));
    }

    @Test
    public void reviewFixes20260908_getAllEmitsAnIndependentImmutablePrefixForEveryMatch() throws Exception {
        PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder()
                .add(Arrays.asList("a"), "one")
                .add(Arrays.asList("a", "b"), "two")
                .add(Arrays.asList("a", "b", "c"), "three")
                .build();
        List<String> query = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));

        List<Map.Entry<List<String>, String>> matches = table.getAll(query).toList();
        query.set(0, "changed");
        query.clear();

        assertEquals(3, matches.size());
        assertEquals(Arrays.asList("a"), matches.get(0).getKey());
        assertEquals(Arrays.asList("a", "b"), matches.get(1).getKey());
        assertEquals(Arrays.asList("a", "b", "c"), matches.get(2).getKey());
        assertEquals(Arrays.asList("one", "two", "three"), matches.stream().map(Map.Entry::getValue).toList());

        for (Map.Entry<List<String>, String> match : matches) {
            assertEquals(Arrays.asList("a", "b", "c").subList(0, match.getKey().size()).hashCode(), match.getKey().hashCode());
            assertThrows(UnsupportedOperationException.class, () -> match.getKey().set(0, "changed"));
            assertThrows(UnsupportedOperationException.class, () -> match.getKey().add("more"));

            // The entry itself is Serializable, so its key must be too: an immutable *sub-view* of the key
            // snapshot is immutable but not Serializable, and it would also pin the whole snapshot array.
            try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
                out.writeObject(match);
            }
        }
    }
}
