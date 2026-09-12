package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BooleanListContainsTest extends BooleanListTestSupport {

    @Test
    public void testContains() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.contains(true));
        assertTrue(list.containsAny(new boolean[] { false, false }));
        assertFalse(list.containsAny(new boolean[] {}));
        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true, true, true)));
    }

    @Test
    public void testContainsAny() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = BooleanList.of(false, false);
            assertTrue(list1.containsAny(list2));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true);
            boolean[] arr = { false };
            assertFalse(list.containsAny(arr));
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, true, true);
            assertFalse(a.containsAny(new boolean[] { false }));
        }
        {
            list = new BooleanList();
            list.add(true);
            assertFalse(list.containsAny(new boolean[] {}));
        }
    }

    @Test
    public void testContainsAny_LargeData() {
        {
            list = new BooleanList();
            // Trigger needToSet path: min(lenA, lenB) > 3 && max(lenA, lenB) > 9
            BooleanList large = new BooleanList();
            for (int i = 0; i < 15; i++) {
                large.add(i % 2 == 0);
            }
            BooleanList query = BooleanList.of(true, true, true, true, true);
            assertTrue(large.containsAny(query));
        }
        {
            list = new BooleanList();
            // All elements are true; query has false
            BooleanList large = new BooleanList();
            for (int i = 0; i < 15; i++) {
                large.add(true);
            }
            boolean[] query = { false };
            assertFalse(large.containsAny(query));
        }
    }

    @Test
    public void testContainsAny_Empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void testContainsAll() {
        {
            list = new BooleanList();
            // Small lists (no set path)
            BooleanList a = BooleanList.of(true, false);
            BooleanList b = BooleanList.of(true);
            assertTrue(a.containsAll(b));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            boolean[] arr = { true };
            assertTrue(list.containsAll(arr));
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, true);
            assertFalse(a.containsAll(new boolean[] { false }));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            assertTrue(list.containsAll(BooleanList.of(true, false)));
            assertTrue(list.containsAll(BooleanList.of(true, true, true)));
        }
    }

    @Test
    public void testContainsAll_LargeData() {
        {
            list = new BooleanList();
            // needToSet triggers when min(lenA,lenB) > 3 and max > 9
            // Build a large list and a query that uses the set path
            BooleanList large = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
            BooleanList query = BooleanList.of(true, false, true, false);
            assertTrue(large.containsAll(query));

            BooleanList query2 = BooleanList.of(true, false, true, false);
            BooleanList small = BooleanList.of(true, false, true);
            // small list won't use set path; make sure both branches work
            assertTrue(small.containsAll(BooleanList.of(true)));
        }
        {
            list = new BooleanList();
            BooleanList large = new BooleanList();
            for (int i = 0; i < 15; i++) {
                large.add(i % 2 == 0);
            }
            boolean[] query = { true, false };
            assertTrue(large.containsAll(query));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true);
            BooleanList list2 = new BooleanList();
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false);
            assertTrue(a.containsAll(new boolean[0]));
        }
    }

    @Test
    public void testContainsDuplicates() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            assertTrue(list.containsDuplicates());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            assertFalse(list.containsDuplicates());
        }
    }

    @Test
    public void testContainsDuplicates_Empty() {
        BooleanList list = new BooleanList();
        assertFalse(list.containsDuplicates());
    }
}
