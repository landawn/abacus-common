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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
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
    public void testGetAll_emptyKeyThrows() {
        PrefixSearchTable<Integer, String> table = PrefixSearchTable.<Integer, String> builder().build();
        assertThrows(IllegalArgumentException.class, () -> table.getAll(Arrays.asList()));
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

    // ===== builder() =====

    @Test
    public void testBuilder() {
        PrefixSearchTable.Builder<Integer, String> builder = PrefixSearchTable.builder();
        assertNotNull(builder);
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
}
