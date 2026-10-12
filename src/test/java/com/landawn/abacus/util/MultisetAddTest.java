package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MultisetAddTest extends MultisetTestSupport {

    // add

    @Test
    public void testAdd() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
        assertEquals(2, multiset.size());
    }

    @Test
    public void testAdd_IncrementExisting() {
        multiset.add("apple", 2);
        int oldCount = multiset.add("apple", 3);
        assertEquals(2, oldCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_MultipleOccurrences() {
        int oldCount = multiset.add("apple", 3);
        assertEquals(0, oldCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_NegativeOccurrences() {
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", -1));
    }

    @Test
    public void testAdd_Null() {
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
    }

    @Test
    public void testAdd_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("apple", 1));
    }

    @Test
    public void testAdd_SingleOccurrence() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));
    }

    @Test
    public void testAdd_ZeroOccurrences() {
        int oldCount = multiset.add("apple", 0);
        assertEquals(0, oldCount);
        assertEquals(0, multiset.getCount("apple"));
    }

    @Test
    public void testAddElementWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(0, multiset.add("a", 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.size());

        assertEquals(3, multiset.add("a", 2));
        assertEquals(5, multiset.getCount("a"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("b", 0));
        assertEquals(0, multiset.getCount("b"));
        assertEquals(5, multiset.size());

        assertEquals(0, multiset.add("c", 1));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(6, multiset.size());
    }

    @Test
    public void testAddElementWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", -1));
    }

    @Test
    public void testAddElementWithOccurrences_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a", 2));

        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));

        Multiset<String> multiset2 = new Multiset<>();

        multiset2.add("b", 10);
        multiset2.add("b", Integer.MAX_VALUE - 10);
        assertEquals(Integer.MAX_VALUE, multiset2.getCount("b"));

    }

    @Test
    @DisplayName("Test add() single element")
    public void testAddSingle() {
        assertTrue(multiset.add("apple"));
        assertEquals(1, multiset.getCount("apple"));

        assertTrue(multiset.add("apple"));
        assertEquals(2, multiset.getCount("apple"));
    }

    @Test
    public void testAddSingleElement() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add("a"));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.size());

        assertTrue(multiset.add("a"));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.size());

        assertTrue(multiset.add("b"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(3, multiset.size());
    }

    @Test
    public void testAddSingleElement_maxOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("a"));
    }

    @Test
    public void testAddSingleElement_nullNotPermittedByDefaultMap() {
        Multiset<String> multiset = new Multiset<>();
        assertTrue(multiset.add(null));
        assertEquals(1, multiset.getCount(null));
        assertTrue(multiset.add(null));
        assertEquals(2, multiset.getCount(null));
    }

    @Test
    public void testAddWithOccurrences() {
        assertEquals(0, multiset.add("apple", 3));
        assertEquals(3, multiset.getCount("apple"));

        assertEquals(3, multiset.add("apple", 2));
        assertEquals(5, multiset.getCount("apple"));

        assertEquals(0, multiset.add("banana", 0));
        assertEquals(0, multiset.getCount("banana"));

        assertThrows(IllegalArgumentException.class, () -> multiset.add("test", -1));

        multiset.add("overflow", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.add("overflow", 1));
    }

    // addAll

    @Test
    public void testAddAll() {
        List<String> list = Arrays.asList("a", "b", "a");
        assertTrue(multiset.addAll(list));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testAddAll_Empty() {
        assertFalse(multiset.addAll(new ArrayList<>()));
    }

    @Test
    public void testAddAll_Null() {
        assertFalse(multiset.addAll(null));
    }

    @Test
    public void testAddAll_WithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));
    }

    @Test
    public void testAddAll_WithOccurrences_Negative() {
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }

    @Test
    public void testAddAll_WithOccurrences_Zero() {
        List<String> list = Arrays.asList("a", "b");
        assertFalse(multiset.addAll(list, 0));
        assertEquals(0, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        Collection<String> toAdd = Arrays.asList("a", "b", "c", "b");

        assertTrue(multiset.addAll(toAdd));
        assertEquals(2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
        assertEquals(1 + 4, multiset.size());
    }

    @Test
    public void testAddAllCollection_empty() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollection_null() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 1);
        Collection<String> toAdd = Arrays.asList("a", "b");

        assertTrue(multiset.addAll(toAdd, 2));
        assertEquals(1 + 2, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1 + 2 + 2, multiset.size());
    }

    @Test
    public void testAddAllCollectionWithOccurrences_emptyCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(Collections.emptyList(), 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_negative() {
        Multiset<String> multiset = new Multiset<>();
        Collection<String> toAdd = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(toAdd, -1));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_nullCollection() {
        Multiset<String> multiset = Multiset.of("a");
        assertFalse(multiset.addAll(null, 2));
        assertEquals(1, multiset.getCount("a"));
    }

    @Test
    public void testAddAllCollectionWithOccurrences_zero() {
        Multiset<String> multiset = Multiset.of("a", "b");
        Collection<String> toAdd = Arrays.asList("a", "c");
        assertFalse(multiset.addAll(toAdd, 0));
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(0, multiset.getCount("c"));
    }

    @Test
    @DisplayName("Test addAll() empty collection")
    public void testAddAllEmptyCollection() {
        assertFalse(multiset.addAll(Collections.emptyList()));
        assertTrue(multiset.isEmpty());
    }

    // U22-05: addAll(Multiset) iterates another source directly and merges each count with one lookup
    @Test
    public void testU2205_addAllFromAnotherMultisetMergesCountsAndLeavesTheSourceUntouched() {
        final Multiset<String> dst = new Multiset<>();
        dst.add("a", 2);
        dst.add("c", 5);
        final Multiset<String> src = new Multiset<>();
        src.add("a", 3);
        src.add("b", 4);

        assertTrue(dst.addAll(src));
        assertEquals(5, dst.getCount("a")); // existing element: in-place count update
        assertEquals(4, dst.getCount("b")); // new element: inserted
        assertEquals(5, dst.getCount("c"));
        assertEquals(3, src.getCount("a"));
        assertEquals(4, src.getCount("b"));
        assertEquals(2, src.entrySet().size());

        assertTrue(dst.addAll(src, 2));
        assertEquals(11, dst.getCount("a"));
        assertEquals(12, dst.getCount("b"));
        assertEquals(5, dst.getCount("c"));

        assertFalse(dst.addAll(new Multiset<>()));
        assertFalse(dst.addAll(src, 0));
        assertEquals(11, dst.getCount("a"));
    }

    @Test
    public void testC345_addAllFromFinerBackingMergesCounts() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> src = new Multiset<>(supplier);
        src.add(new String("k"), 1);
        src.add(new String("k"), 2);

        final Multiset<String> dst = new Multiset<>();
        assertTrue(dst.addAll(src));
        assertEquals(3, dst.getCount("k"));
        assertEquals(1, dst.countOfDistinctElements());
    }

    @Test
    public void testC345_addAllMultisetAddsWholeCountsQuickly() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", 500_000_000);
        src.add("b", 500_000_000);

        final Multiset<String> dst = new Multiset<>();
        assertTimeout(Duration.ofMillis(1000), () -> assertTrue(dst.addAll(src)));
        assertEquals(500_000_000, dst.getCount("a"));
        assertEquals(500_000_000, dst.getCount("b"));
        assertEquals(1_000_000_000L, dst.sumOfOccurrences());
    }

    @Test
    public void testC345_addAllMultisetOverflowLeavesFailingElementUnchanged() {
        final Multiset<String> dst = new Multiset<>();
        dst.add("a", Integer.MAX_VALUE - 1);

        final Multiset<String> src = new Multiset<>();
        src.add("a", 5);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> dst.addAll(src));
        assertEquals(Integer.MAX_VALUE - 1, dst.getCount("a"));
        assertFalse(e.getMessage().contains("occurrencesToAdd=1"), e.getMessage());
    }

    @Test
    public void testC345_addAllMultisetWithMultiplier() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", 2);
        src.add(null, 1);
        src.add("ß", 3);

        final Multiset<String> dst = Multiset.of("a");
        assertTrue(dst.addAll(src, 3));
        assertEquals(7, dst.getCount("a"));
        assertEquals(3, dst.getCount(null));
        assertEquals(9, dst.getCount("ß"));

        assertFalse(dst.addAll(src, 0));
        assertFalse(dst.addAll(new Multiset<>()));
        assertFalse(dst.addAll((Multiset<String>) null));
        assertEquals(19, dst.size());
    }

    @Test
    public void testC345_addAllMultisetWithMultiplierOverflowingLong() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", Integer.MAX_VALUE);

        final Multiset<String> dst = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> dst.addAll(src, Integer.MAX_VALUE));
        assertEquals(0, dst.getCount("a"));
        assertTrue(dst.isEmpty());
    }

    @Test
    public void testU2205_addAllOverflowLeavesTheFailingElementUnchanged() {
        final Multiset<String> big = new Multiset<>();
        big.add("x", Integer.MAX_VALUE - 1);

        final Multiset<String> src = new Multiset<>(LinkedHashMap.class);
        src.add("x", 2);
        src.add("y", 1);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> big.addAll(src));
        assertTrue(e.getMessage().contains("out of the bound of int"), e.getMessage());
        assertTrue(e.getMessage().contains("current=" + (Integer.MAX_VALUE - 1)), e.getMessage());
        assertEquals(Integer.MAX_VALUE - 1, big.getCount("x"));
        assertEquals(0, big.getCount("y")); // "x" failed first: nothing after it was added

        // an exact fit is accepted
        final Multiset<String> one = new Multiset<>();
        one.add("x", 1);
        assertTrue(big.addAll(one));
        assertEquals(Integer.MAX_VALUE, big.getCount("x"));

        // self-add at MAX_VALUE: refused, unchanged
        assertThrows(IllegalArgumentException.class, () -> big.addAll(big));
        assertEquals(Integer.MAX_VALUE, big.getCount("x"));

        // count * occurrencesToAdd beyond int, for a new element
        final Multiset<String> huge = new Multiset<>();
        huge.add("x", Integer.MAX_VALUE);
        final Multiset<String> empty = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> empty.addAll(huge, 2));
        assertEquals(0, empty.getCount("x"));
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testC345_addAllSelfDoubles() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 2);
        ms.add("b", 3);

        assertTrue(ms.addAll(ms));
        assertEquals(4, ms.getCount("a"));
        assertEquals(6, ms.getCount("b"));

        assertTrue(ms.addAll(ms, 2));
        assertEquals(12, ms.getCount("a"));
        assertEquals(18, ms.getCount("b"));
    }

    @Test
    public void testU2205_addAllSelfStillDoubles() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 2);
        ms.add("b", 3);

        assertTrue(ms.addAll(ms));
        assertEquals(4, ms.getCount("a"));
        assertEquals(6, ms.getCount("b"));

        assertTrue(ms.addAll(ms, 3));
        assertEquals(16, ms.getCount("a"));
        assertEquals(24, ms.getCount("b"));
        assertEquals(2, ms.entrySet().size());
    }

    @Test
    public void testAddAllWithOccurrences() {
        List<String> list = Arrays.asList("a", "b");
        assertTrue(multiset.addAll(list, 3));
        assertEquals(3, multiset.getCount("a"));
        assertEquals(3, multiset.getCount("b"));

        assertFalse(multiset.addAll(list, 0));
        assertFalse(multiset.addAll(Collections.emptyList(), 5));
        assertFalse(multiset.addAll(null, 5));

        assertThrows(IllegalArgumentException.class, () -> multiset.addAll(list, -1));
    }

    @Test
    @DisplayName("addAll rejects a count overflow, and the elements accepted before it stay added")
    public void reviewFixes20260906_addAllRejectsCountOverflow() {
        final Multiset<String> one = new Multiset<>();
        one.add("a", Integer.MAX_VALUE);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> one.addAll(Arrays.asList("a")));
        assertTrue(e1.getMessage().contains("out of the bound of int"), e1.getMessage());
        assertEquals(Integer.MAX_VALUE, one.getCount("a"));

        final Multiset<String> two = new Multiset<>();
        two.add("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> two.addAll(Arrays.asList("a"), 1));
        assertEquals(Integer.MAX_VALUE, two.getCount("a"));

        // add(e, MAX_VALUE) twice throws the same way
        final Multiset<String> twice = new Multiset<>();
        twice.add("a", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> twice.add("a", Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, twice.getCount("a"));

        // addAll is not atomic: "y" is already in when "z" overflows - the javadoc now says so
        final Multiset<String> partial = new Multiset<>();
        partial.add("z", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> partial.addAll(Arrays.asList("y", "z")));
        assertEquals(1, partial.getCount("y"));
        assertEquals(Integer.MAX_VALUE, partial.getCount("z"));

        // Control: an addAll that lands exactly on MAX_VALUE is fine
        final Multiset<String> edge = new Multiset<>();
        edge.add("a", Integer.MAX_VALUE - 1);
        assertTrue(edge.addAll(Arrays.asList("a")));
        assertEquals(Integer.MAX_VALUE, edge.getCount("a"));
    }

    // addAndGetCount

    @Test
    public void testAddAndGetCount() {
        Multiset<String> multiset = new Multiset<>();
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(3, multiset.getCount("a"));

        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));

        assertEquals(0, multiset.addAndGetCount("b", 0));
        assertEquals(0, multiset.getCount("b"));

        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_ExistingElement() {
        multiset.add("apple", 2);
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(5, newCount);
        assertEquals(5, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount_MultipleAdds() {
        assertEquals(3, multiset.addAndGetCount("a", 3));
        assertEquals(5, multiset.addAndGetCount("a", 2));
        assertEquals(5, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_Negative() {
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", -1));
    }

    @Test
    public void testAddAndGetCount_negative() {
        Multiset<String> multiset = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", -1));
    }

    @Test
    public void testAddAndGetCount_NewElement() {
        int newCount = multiset.addAndGetCount("apple", 3);
        assertEquals(3, newCount);
        assertEquals(3, multiset.getCount("apple"));
    }

    @Test
    public void testAddAndGetCount_Overflow() {
        multiset.add("apple", Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("apple", 1));
    }

    @Test
    public void testAddAndGetCount_overflow() {
        Multiset<String> multiset = new Multiset<>();
        multiset.setCount("a", Integer.MAX_VALUE - 1);
        assertThrows(IllegalArgumentException.class, () -> multiset.addAndGetCount("a", 2));
        assertEquals(Integer.MAX_VALUE - 1, multiset.getCount("a"));
    }

    @Test
    public void testAddAndGetCount_Zero() {
        multiset.add("apple", 5);
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(5, newCount);
    }

    @Test
    public void testAddAndGetCount_ZeroOnNewElement() {
        int newCount = multiset.addAndGetCount("apple", 0);
        assertEquals(0, newCount);
        assertEquals(0, multiset.getCount("apple"));
        assertFalse(multiset.contains("apple"));
    }
}
