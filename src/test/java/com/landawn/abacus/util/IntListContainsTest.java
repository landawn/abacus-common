package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntListContainsTest extends IntListTestSupport {

    @Test
    public void testContains() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list.contains(3));
        assertFalse(list.contains(42));
    }

    @Test
    public void testContains_Empty() {
        IntList list = new IntList();
        assertFalse(list.contains(1));
    }

    @Test
    public void testContainsAny() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = IntList.of(4, 5, 6);
            assertFalse(list1.containsAny(list2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = { 4, 5, 6 };
            assertFalse(list.containsAny(arr));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            assertFalse(a.containsAny(new int[] { 99 }));
        }
    }

    @Test
    public void testContainsAny_LargeData() {
        IntList large = new IntList();
        for (int i = 0; i < 15; i++) {
            large.add(i);
        }
        int[] query = { 100, 101, 102, 103, 104 };
        assertFalse(large.containsAny(query));
    }

    @Test
    public void testContainsAny_Empty() {
        IntList other = new IntList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3, 4, 5);
            IntList list2 = IntList.of(2, 4);
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            int[] arr = { 2, 4 };
            assertTrue(list.containsAll(arr));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2);
            assertFalse(a.containsAll(new int[] { 1, 99 }));
        }
    }

    @Test
    public void testContainsAll_LargeData() {
        {
            list = new IntList();
            IntList large = new IntList();
            for (int i = 0; i < 15; i++) {
                large.add(i);
            }
            int[] query = { 0, 1, 2, 3, 4 };
            assertTrue(large.containsAll(query));
        }
        {
            list = new IntList();
            IntList large = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
            IntList query = IntList.of(1, 2, 3, 4);
            assertTrue(large.containsAll(query));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertTrue(list.containsAll(new IntList()));
        }
    }

    @Test
    public void testContainsDuplicates() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertFalse(list.containsDuplicates());
    }

    // ---- perf review 2026-09-26 G047 begin ----
    // G047-01: containsAll/disjoint/containsAny when this list is larger than the argument (hash side switched to the argument)
    @Test
    public void testContainsAll_largerReceiverEdgeCases() {
        final IntList big = IntList.range(0, 20);
        // all present, with duplicates in the argument
        assertTrue(big.containsAll(IntList.of(3, 3, 19, 0, 0)));
        // last required value found only at the very end of the receiver
        assertTrue(big.containsAll(IntList.of(19, 18, 17, 16)));
        // one value missing
        assertFalse(big.containsAll(IntList.of(1, 2, 3, 20)));
        assertFalse(big.containsAll(new int[] { -1, 2, 3, 4 }));
        // negative values and extremes
        final IntList extremes = IntList.of(Integer.MIN_VALUE, -5, 0, 5, Integer.MAX_VALUE, 7, 7, 8, 9, 10, 11);
        assertTrue(extremes.containsAll(IntList.of(Integer.MAX_VALUE, Integer.MIN_VALUE, 7, 7)));
        assertFalse(extremes.containsAll(IntList.of(Integer.MAX_VALUE, Integer.MIN_VALUE, 7, 6)));
        // a window over a larger buffer must not see values beyond its size
        final int[] buffer = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 99, 98, 97, 96 };
        final IntList window = IntList.of(buffer, 12);
        assertFalse(window.containsAll(IntList.of(1, 2, 3, 99)));
        assertTrue(window.disjoint(IntList.of(99, 98, 97, 96)));
        assertFalse(window.containsAny(IntList.of(99, 98, 97, 96)));
        assertFalse(window.disjoint(IntList.of(99, 98, 97, 12)));
        assertTrue(window.containsAny(new int[] { 99, 98, 97, 12 }));
        // the argument itself and an equal-size argument
        assertTrue(big.containsAll(big));
        assertFalse(big.disjoint(big));
        // the receiver is left unchanged
        assertTrue(big.equals(IntList.range(0, 20)));
    }

    // G047-01: randomized equivalence with a naive model over both size orderings and the needToSet boundary
    @Test
    public void testContainsAll_disjoint_matchesNaiveModel() {
        final java.util.Random random = new java.util.Random(20260926L);

        for (int trial = 0; trial < 3000; trial++) {
            final int sizeA = random.nextInt(30);
            final int sizeB = random.nextInt(30);
            final int range = 1 + random.nextInt(40);
            final int[] a = new int[sizeA];
            final int[] b = new int[sizeB];

            for (int i = 0; i < sizeA; i++) {
                a[i] = random.nextInt(range) - range / 2;
            }

            for (int i = 0; i < sizeB; i++) {
                b[i] = random.nextInt(range) - range / 2;
            }

            boolean expectedContainsAll = true;
            boolean expectedDisjoint = true;

            for (final int x : b) {
                boolean found = false;

                for (final int y : a) {
                    if (x == y) {
                        found = true;
                        break;
                    }
                }

                expectedContainsAll &= found;
                expectedDisjoint &= !found;
            }

            final IntList listA = IntList.of(a.clone());
            final IntList listB = IntList.of(b.clone());
            final String msg = java.util.Arrays.toString(a) + " / " + java.util.Arrays.toString(b);

            org.junit.jupiter.api.Assertions.assertEquals(expectedContainsAll, listA.containsAll(listB), msg);
            org.junit.jupiter.api.Assertions.assertEquals(expectedContainsAll, listA.containsAll(b.clone()), msg);
            org.junit.jupiter.api.Assertions.assertEquals(expectedDisjoint, listA.disjoint(listB), msg);
            org.junit.jupiter.api.Assertions.assertEquals(expectedDisjoint, listA.disjoint(b.clone()), msg);
            org.junit.jupiter.api.Assertions.assertEquals(sizeA > 0 && sizeB > 0 && !expectedDisjoint, listA.containsAny(listB), msg);
            org.junit.jupiter.api.Assertions.assertEquals(sizeA > 0 && sizeB > 0 && !expectedDisjoint, listA.containsAny(b.clone()), msg);
            org.junit.jupiter.api.Assertions.assertArrayEquals(a, listA.toArray(), msg);
            org.junit.jupiter.api.Assertions.assertArrayEquals(b, listB.toArray(), msg);
        }
    }

    // G047-02: removeDuplicates unsorted path (membership set changed from LinkedHashSet to HashSet) keeps first occurrences in order
    @Test
    @SuppressWarnings("deprecation")
    public void testRemoveDuplicates_unsortedMatchesFirstOccurrenceModel() {
        final java.util.Random random = new java.util.Random(926L);

        for (int trial = 0; trial < 2000; trial++) {
            final int size = random.nextInt(40);
            final int range = 1 + random.nextInt(50);
            final int[] a = new int[size];

            for (int i = 0; i < size; i++) {
                a[i] = random.nextInt(range) - range / 2 + (random.nextInt(10) == 0 ? Integer.MIN_VALUE : 0);
            }

            final java.util.LinkedHashSet<Integer> model = new java.util.LinkedHashSet<>();

            for (final int x : a) {
                model.add(x);
            }

            final int[] expected = new int[model.size()];
            int k = 0;

            for (final Integer x : model) {
                expected[k++] = x;
            }

            final IntList list = IntList.of(a.clone());
            final String msg = java.util.Arrays.toString(a);

            org.junit.jupiter.api.Assertions.assertEquals(expected.length < size, list.removeDuplicates(), msg);
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray(), msg);

            final int[] backing = list.internalArray();

            for (int i = list.size(); i < size; i++) {
                org.junit.jupiter.api.Assertions.assertEquals(0, backing[i], msg);
            }
        }
    }
    // ---- perf review 2026-09-26 G047 end ----
}
