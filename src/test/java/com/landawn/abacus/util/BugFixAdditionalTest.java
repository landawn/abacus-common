package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests verifying additional bug fixes: RowDataset.merge(), Difference.MapDifference,
 * and Observer negative sleep.
 */
@Tag("2025")
public class BugFixAdditionalTest extends TestBase {

    // ============================================================
    // Fix: RowDataset.merge() reusing same list for multiple columns
    // ============================================================

    @Test
    public void testRowDataset_mergeDoesNotShareColumnLists() {
        // Create first dataset with columns "a" and "b"
        List<String> cols1 = new ArrayList<>(Arrays.asList("a", "b"));
        List<List<Object>> data1 = new ArrayList<>();
        data1.add(new ArrayList<>(Arrays.asList(1, 2))); // column "a"
        data1.add(new ArrayList<>(Arrays.asList("x", "y"))); // column "b"
        RowDataset ds1 = new RowDataset(cols1, data1);

        // Create second dataset with columns "a", "c", "d"
        List<String> cols2 = new ArrayList<>(Arrays.asList("a", "c", "d"));
        List<List<Object>> data2 = new ArrayList<>();
        data2.add(new ArrayList<>(Arrays.asList(3))); // column "a"
        data2.add(new ArrayList<>(Arrays.asList("newC"))); // column "c"
        data2.add(new ArrayList<>(Arrays.asList("newD"))); // column "d"
        RowDataset ds2 = new RowDataset(cols2, data2);

        // Merge columns "c" and "d" from ds2 into ds1
        ds1.merge(ds2, Arrays.asList("c", "d"));

        // After merge, "c" and "d" should be separate list objects, not the same reference
        List<Object> colC = ds1.getColumn("c");
        List<Object> colD = ds1.getColumn("d");
        assertNotSame(colC, colD, "Columns 'c' and 'd' should not be the same list object");

        // Verify data integrity: both should have null padding for existing rows + merged data
        assertEquals(3, colC.size()); // 2 existing rows (null padded) + 1 merged row
        assertEquals(3, colD.size());

        // First 2 rows should be null (padding for ds1's original rows)
        assertNull(colC.get(0));
        assertNull(colC.get(1));
        assertNull(colD.get(0));
        assertNull(colD.get(1));

        // Third row should have the merged values
        assertEquals("newC", colC.get(2));
        assertEquals("newD", colD.get(2));
    }

    @Test
    public void testRowDataset_mergePreservesExistingData() {
        List<String> cols1 = new ArrayList<>(Arrays.asList("id", "name"));
        List<List<Object>> data1 = new ArrayList<>();
        data1.add(new ArrayList<>(Arrays.asList(1, 2)));
        data1.add(new ArrayList<>(Arrays.asList("Alice", "Bob")));
        RowDataset ds1 = new RowDataset(cols1, data1);

        List<String> cols2 = new ArrayList<>(Arrays.asList("id", "name"));
        List<List<Object>> data2 = new ArrayList<>();
        data2.add(new ArrayList<>(Arrays.asList(3)));
        data2.add(new ArrayList<>(Arrays.asList("Charlie")));
        RowDataset ds2 = new RowDataset(cols2, data2);

        ds1.merge(ds2, Arrays.asList("id", "name"));

        assertEquals(3, ds1.size());
        assertEquals(1, ds1.getColumn("id").get(0));
        assertEquals(2, ds1.getColumn("id").get(1));
        assertEquals(3, ds1.getColumn("id").get(2));
    }

    // ============================================================
    // Fix: MapDifference.of() valueEquivalence bypass when val2 is null
    // ============================================================

    @Test
    public void testMapDifference_nullValueInMap2() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", null); // key exists but value is null

        var diff = Difference.MapDifference.of(map1, map2);

        // "a" should be common (both have value 1)
        assertEquals(1, diff.common().get("a"));

        // "b" should be in differentValues (map1 has 2, map2 has null)
        assertTrue(diff.onlyOnLeft().isEmpty(), "'b' exists in both maps, should not be onlyOnLeft");
        assertTrue(diff.differentValues().containsKey("b"), "'b' should be in differentValues");
        assertEquals(2, diff.differentValues().get("b").left());
        assertNull(diff.differentValues().get("b").right());
    }

    @Test
    public void testMapDifference_nullValueInMap1() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", null); // key exists but value is null
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        var diff = Difference.MapDifference.of(map1, map2);

        // "b" should be common
        assertEquals(2, diff.common().get("b"));

        // "a" should be in differentValues
        assertTrue(diff.differentValues().containsKey("a"));
        assertNull(diff.differentValues().get("a").left());
        assertEquals(1, diff.differentValues().get("a").right());
    }

    @Test
    public void testMapDifference_bothValuesNull() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", null);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", null);

        var diff = Difference.MapDifference.of(map1, map2);

        // Both have null for same key - should be common
        assertTrue(diff.common().containsKey("a"));
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
    }

    @Test
    public void testMapDifference_keyOnlyInMap1() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);

        var diff = Difference.MapDifference.of(map1, map2);

        // "a" should be only on left
        assertTrue(diff.onlyOnLeft().containsKey("a"));
        assertEquals(1, diff.onlyOnLeft().get("a"));
        // "b" should be common
        assertEquals(2, diff.common().get("b"));
    }

    @Test
    public void testMapDifference_keyOnlyInMap2() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("c", 3);

        var diff = Difference.MapDifference.of(map1, map2);

        // "c" should be only on right
        assertTrue(diff.onlyOnRight().containsKey("c"));
        assertEquals(3, diff.onlyOnRight().get("c"));
    }

    // ============================================================
    // Fix: Observer negative sleep (Math.max(0, ...))
    // ============================================================

    @Test
    public void testObserver_delayWithZero() {
        // delay(0) should not cause issues
        List<Integer> results = new ArrayList<>();

        Observer.of(Arrays.asList(1, 2, 3)).delay(0, java.util.concurrent.TimeUnit.MILLISECONDS).observe(results::add, e -> {
        }, () -> {
        });

        // Give some time for async processing
        N.sleepUninterruptibly(500);

        assertEquals(3, results.size());
    }

    @Test
    public void testObserver_basicObserve() {
        List<String> results = new ArrayList<>();

        Observer.of(Arrays.asList("a", "b", "c")).observe(results::add, e -> {
        }, () -> {
        });

        N.sleepUninterruptibly(500);

        assertEquals(Arrays.asList("a", "b", "c"), results);
    }
}
