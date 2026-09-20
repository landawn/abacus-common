package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

public class NMergeTest extends NTestSupport {

    private static final BiFunction<Integer, Integer, MergeResult> BY_VALUE = (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

    @Test
    public void testMerge_arraysAndIterables() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), N.merge(new Integer[] { 1, 3, 5, 7 }, new Integer[] { 2, 4, 6, 8 }, BY_VALUE));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), N.merge(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6), BY_VALUE));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), N.merge(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, (x, y) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(4, 5, 6, 1, 2, 3), N.merge(new Integer[] { 1, 2, 3 }, new Integer[] { 4, 5, 6 }, (x, y) -> MergeResult.TAKE_SECOND));
        assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "elderberry", "fig"), N.merge(new String[] { "apple", "cherry", "elderberry" },
                new String[] { "banana", "date", "fig" }, (a, b) -> a.compareTo(b) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

        assertEquals(Arrays.asList(1, 2, 3), N.merge((Integer[]) null, new Integer[] { 1, 2, 3 }, BY_VALUE));
        assertEquals(Arrays.asList(1, 2, 3), N.merge(new Integer[] { 1, 2, 3 }, null, BY_VALUE));
        assertTrue(N.merge((Integer[]) null, (Integer[]) null, BY_VALUE).isEmpty());
        assertTrue(N.merge(new Integer[0], new Integer[0], BY_VALUE).isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), N.merge(new Integer[0], new Integer[] { 1, 2, 3 }, (a, b) -> MergeResult.TAKE_FIRST));
        assertEquals(Arrays.asList(1, 2, 3), N.merge((List<Integer>) null, Arrays.asList(1, 2, 3), BY_VALUE));
    }

    @Test
    public void testMerge_collectionOfIterables() {
        List<List<Integer>> lists = Arrays.asList(Arrays.asList(1, 4, 7), Arrays.asList(2, 5, 8), Arrays.asList(3, 6, 9));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), N.merge(lists, BY_VALUE));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)),
                N.merge(Arrays.asList(Arrays.asList(1, 4), Arrays.asList(2, 5), Arrays.asList(3, 6)), BY_VALUE, HashSet::new));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                N.merge(Arrays.asList(Arrays.asList(1, 5, 9), Arrays.asList(2, 6, 10), Arrays.asList(3, 7, 11), Arrays.asList(4, 8, 12)),
                        (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));

        assertTrue(N.merge((List<List<Integer>>) null, BY_VALUE).isEmpty());
        assertTrue(N.merge(new ArrayList<List<Integer>>(), BY_VALUE).isEmpty());
        assertEquals(0, N.merge(Collections.emptyList(), (x, y) -> MergeResult.TAKE_FIRST, IntFunctions.ofList()).size());
        assertEquals(Arrays.asList(3, 1, 2), N.merge(Collections.singletonList(Arrays.asList(3, 1, 2)), BY_VALUE, ArrayList::new));

        List<List<Integer>> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add(Arrays.asList(1, 2, 3));
        withNull.add(null);
        withNull.add(Arrays.asList(4, 5, 6));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), N.merge(withNull, (a, b) -> MergeResult.TAKE_FIRST));

        List<List<Integer>> bothNull = new ArrayList<>();
        bothNull.add(null);
        bothNull.add(null);
        assertEquals(0, N.merge(bothNull, BY_VALUE, ArrayList::new).size());

        List<Iterable<Integer>> two = Arrays.asList(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), N.merge(two, BY_VALUE, IntFunctions.ofList()));

        Iterable<Integer> odd = () -> Arrays.asList(1, 3, 5).iterator();
        Iterable<Integer> even = () -> Arrays.asList(2, 4, 6).iterator();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), N.merge(Arrays.asList(odd, even), BY_VALUE, IntFunctions.ofList()));
    }

    @Test
    public void testMerge_nullSelectorAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> N.merge(new Integer[0], new Integer[0], null));
        assertThrows(IllegalArgumentException.class, () -> N.merge(Collections.<Integer> emptyList(), Collections.<Integer> emptyList(), null));
        assertThrows(IllegalArgumentException.class, () -> N.merge(Collections.<Iterable<Integer>> emptyList(), null));
        assertThrows(IllegalArgumentException.class, () -> N.merge(Collections.<Iterable<Integer>> emptyList(), null, IntFunctions.ofList()));

        Collection<Integer> hugeEmpty = new java.util.AbstractCollection<>() {
            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
        assertThrows(ArithmeticException.class,
                () -> N.merge(Arrays.<Iterable<Integer>> asList(hugeEmpty, hugeEmpty), (a, b) -> MergeResult.TAKE_FIRST, IntFunctions.ofList()));

        Collection<Integer> hugeSingleton = new java.util.AbstractCollection<>() {
            @Override
            public Iterator<Integer> iterator() {
                return Collections.singleton(1).iterator();
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
        assertThrows(ArithmeticException.class, () -> N.merge(Arrays.<Iterable<Integer>> asList(hugeSingleton, hugeSingleton, Collections.emptyList()),
                (a, b) -> MergeResult.TAKE_FIRST, IntFunctions.ofList()));
    }

    /**
     * A single {@code null} element used to produce three different outcomes depending on the collection's
     * size: size 1 gave a raw {@link NullPointerException} from {@code Dataset.copy()}; size 2 gave
     * {@link IllegalArgumentException} naming {@code 'a'}/{@code 'b'}, which are parameters of the
     * <i>two-argument</i> {@code merge} rather than of this method; and size 3 or more gave a raw
     * {@code NullPointerException} from {@code Dataset.columnNames()}. Validation now completes before any
     * main logic, so a {@code null} element is always an {@code IllegalArgumentException} naming this
     * method's own parameter.
     */
    @Test
    public void testMergeDatasetCollectionRejectsANullElementRegardlessOfSize() {
        final Dataset ds = N.newDataset(Arrays.asList("id"), Arrays.asList(Arrays.asList((Object) 1)));

        final List<Collection<Dataset>> inputs = Arrays.asList(Arrays.asList((Dataset) null), Arrays.asList(null, ds), Arrays.asList(ds, null),
                Arrays.asList(ds, null, ds), Arrays.asList(ds, ds, ds, null));

        for (final Collection<Dataset> input : inputs) {
            for (final boolean requiresSameColumns : new boolean[] { false, true }) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> N.merge(input, requiresSameColumns),
                        () -> "size " + input.size() + ", requiresSameColumns=" + requiresSameColumns);

                assertTrue(e.getMessage().contains("dss"), e.getMessage());
            }

            assertThrows(IllegalArgumentException.class, () -> N.merge(input), () -> "size " + input.size());
        }

        // A null or empty collection remains the documented no-op that returns an empty Dataset.
        assertTrue(N.merge((Collection<Dataset>) null).isEmpty());
        assertTrue(N.merge(Collections.<Dataset> emptyList()).isEmpty());
    }
}
