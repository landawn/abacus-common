package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class NContainsTest extends NTestSupport {

    @Test
    public void testContainsDuplicatesCollectionRules() {
        final List<Integer> separatedDuplicates = List.of(1, 2, 1);
        assertTrue(N.containsDuplicates(separatedDuplicates));
        assertFalse(N.containsDuplicates(separatedDuplicates, true));
        assertTrue(N.containsDuplicates(new Integer[] { 1, 2, 1 }, true));

        final int[] first = { 1 };
        final int[] second = { 1 };
        final Set<int[]> arrays = new HashSet<>(List.of(first, second));
        assertFalse(N.containsDuplicates(arrays));
        assertFalse(N.containsDuplicates(arrays, true));
        assertTrue(N.containsDuplicates(List.of(first, second)));
        assertTrue(N.containsDuplicates(List.of(first, second), true));
    }

    @Test
    public void testContainsAnyAndNoneRetainReceiverSetMembership() {
        final Set<String> insensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        insensitive.add("alpha");

        assertTrue(N.containsAny(insensitive, "ALPHA"));
        assertFalse(N.containsNone(insensitive, "ALPHA"));
        assertTrue(N.containsAny(insensitive, Set.of("ALPHA")));
        assertFalse(N.containsNone(insensitive, Set.of("ALPHA")));
        assertFalse(N.containsAny(Set.of("ALPHA"), insensitive));
        assertTrue(N.containsNone(Set.of("ALPHA"), insensitive));

        assertTrue(N.containsAny(List.of("ALPHA"), insensitive));
        assertFalse(N.containsNone(List.of("ALPHA"), insensitive));
        assertFalse(N.containsAny(List.of("ALPHA"), insensitive.toArray()));
        assertTrue(N.containsNone(List.of("ALPHA"), insensitive.toArray()));
    }

    @Test
    public void testContainsAllUsesTreeSetReceiverMembership() {
        final Collection<String> receiver = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        receiver.addAll(List.of("alpha", "beta"));
        final Collection<String> values = List.of("ALPHA", "BETA");

        assertTrue(N.containsAll(receiver, values));
        assertTrue(N.containsAll((Iterable<String>) receiver, values));
        assertFalse(N.containsAll(receiver, List.of("ALPHA", "missing")));
        assertFalse(N.containsAll((Iterable<String>) receiver, List.of("ALPHA", "missing")));
    }

    @Test
    public void testContains_arrays() {
        assertTrue(N.contains(new boolean[] { true, false, true }, true));
        assertFalse(N.contains(new boolean[] { false, false }, true));
        assertFalse(N.contains((boolean[]) null, true));
        assertFalse(N.contains(EMPTY_BOOLEAN_ARRAY_CONST, true));

        assertTrue(N.contains(new char[] { 'a', 'b', 'c' }, 'c'));
        assertFalse(N.contains(new char[] { 'a', 'b' }, 'z'));
        assertFalse(N.contains((char[]) null, 'a'));

        assertTrue(N.contains(new byte[] { 1, 2, 3 }, (byte) 3));
        assertFalse(N.contains(new byte[] { 1, 2 }, (byte) 10));
        assertFalse(N.contains((byte[]) null, (byte) 1));

        assertTrue(N.contains(new short[] { 10, 20, 30 }, (short) 30));
        assertFalse(N.contains((short[]) null, (short) 1));

        assertTrue(N.contains(new int[] { 1, 2, 3, 4, 5 }, 3));
        assertFalse(N.contains(new int[] { 1, 2 }, 7));
        assertFalse(N.contains((int[]) null, 1));
        assertFalse(N.contains(EMPTY_INT_ARRAY_CONST, 1));

        assertTrue(N.contains(new long[] { 1000L, 2000L }, 1000L));
        assertFalse(N.contains((long[]) null, 1L));

        assertTrue(N.contains(new float[] { 1.5f, 2.3f }, 1.5f));
        assertTrue(N.contains(new float[] { 1.0f, Float.NaN, 2.0f }, Float.NaN));
        assertFalse(N.contains((float[]) null, 1.0f));

        assertTrue(N.contains(new double[] { 1.5, 2.3 }, 1.5));
        assertTrue(N.contains(new double[] { 1.0, Double.NaN, 2.0 }, Double.NaN));
        assertFalse(N.contains((double[]) null, 1.0));

        assertTrue(N.contains(new Object[] { "hello", "world", null }, "hello"));
        assertTrue(N.contains(new Object[] { "hello", null }, null));
        assertFalse(N.contains(new Object[] { "a" }, null));
        assertFalse(N.contains((Object[]) null, "a"));
    }

    @Test
    public void testContains_collectionIterableIterator() {
        List<String> words = Arrays.asList("apple", "banana", null, "cherry");
        assertTrue(N.contains(words, "apple"));
        assertTrue(N.contains(words, null));
        assertFalse(N.contains(words, "grape"));
        assertFalse(N.contains((Collection<String>) null, "test"));
        assertFalse(N.contains(Collections.emptyList(), "test"));

        Set<Integer> numbers = new HashSet<>(Arrays.asList(1, 2, 3));
        assertTrue(N.contains(numbers, 2));
        assertTrue(N.contains((Iterable<Integer>) numbers, 2));
        assertFalse(N.contains((Iterable<String>) null, "test"));

        assertTrue(N.contains(Arrays.asList("a", "b", "c").iterator(), "b"));
        assertFalse(N.contains(Arrays.asList("a", "b", "c").iterator(), "z"));
        assertFalse(N.contains((Iterator<String>) null, "test"));
        assertFalse(N.contains(Collections.emptyIterator(), "test"));
    }

    @Test
    public void testContains_arrayElementsUseIdentity() {
        final List<int[]> list = Arrays.asList(new int[] { 1, 2 });
        assertFalse(N.contains(list, new int[] { 1, 2 }));
        assertFalse(N.contains((Iterable<?>) list, new int[] { 1, 2 }));
        assertFalse(N.contains(new Object[] { new int[] { 1, 2 } }, new int[] { 1, 2 }));
        final Iterable<int[]> notACollection = () -> Arrays.asList(new int[] { 1, 2 }).iterator();
        assertFalse(N.contains(notACollection, new int[] { 1, 2 }));
        assertTrue(N.contains(list, list.get(0)));
    }

    @Test
    public void testContainsAll() {
        Collection<String> main = Arrays.asList("a", "b", "c", "d");
        assertTrue(N.containsAll(main, Arrays.asList("a", "c")));
        assertTrue(N.containsAll(main, "a", "c"));
        assertTrue(N.containsAll(main, new String[] {}));
        assertTrue(N.containsAll(main, (Collection<?>) null));
        assertTrue(N.containsAll(main, (Object[]) null));
        assertFalse(N.containsAll(main, Arrays.asList("a", "e")));
        assertFalse(N.containsAll(main, "a", "e"));
        assertFalse(N.containsAll(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.containsAll(null, "a"));
        assertTrue(N.containsAll(Arrays.asList("a", "a", "b"), Arrays.asList("a", "a")));
        assertTrue(N.containsAll(Arrays.asList("a", "b"), "a", "a"));

        assertTrue(N.containsAll((Iterable<?>) main, Arrays.asList("a", "c")));
        assertFalse(N.containsAll((Iterable<String>) null, Arrays.asList("a")));
        assertFalse(N.containsAll((Iterable<String>) Collections::emptyIterator, Arrays.asList("a")));

        assertTrue(N.containsAll(main.iterator(), Arrays.asList("a", "c")));
        assertFalse(N.containsAll(main.iterator(), Arrays.asList("a", "e")));
        assertFalse(N.containsAll((Iterator<String>) null, Arrays.asList("a")));
        assertTrue(N.containsAll(Collections.emptyIterator(), Collections.emptySet()));

        assertTrue(N.containsAll(main, new HashSet<>(Arrays.asList("a", "c"))));
        assertFalse(N.containsAll(main, new HashSet<>(Arrays.asList("a", "e"))));
        assertTrue(N.containsAll(main, (Set<?>) null));
        assertFalse(N.containsAll((Collection<?>) null, new HashSet<>(Arrays.asList("a"))));
    }

    @Test
    public void testContainsAny() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsAny(main, Arrays.asList("c", "d")));
        assertTrue(N.containsAny(main, "c", "d"));
        assertFalse(N.containsAny(main, Arrays.asList("d", "e")));
        assertFalse(N.containsAny(main, "d", "e"));
        assertFalse(N.containsAny(main, Collections.emptyList()));
        assertFalse(N.containsAny(main));
        assertFalse(N.containsAny(main, (Collection<?>) null));
        assertFalse(N.containsAny(main, (Object[]) null));
        assertFalse(N.containsAny(null, Arrays.asList("a")));

        assertTrue(N.containsAny((Iterable<?>) main, new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny((Iterable<String>) null, new HashSet<>(Arrays.asList("a"))));
        assertFalse(N.containsAny((Iterable<String>) Collections::emptyIterator, new HashSet<>(Arrays.asList("a"))));

        assertTrue(N.containsAny(main.iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertFalse(N.containsAny(main.iterator(), new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsAny((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));

        Set<String> setMatch = new HashSet<>(Arrays.asList("c", "d"));
        Set<String> setNoMatch = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(N.containsAny(main, setMatch));
        assertFalse(N.containsAny(main, setNoMatch));
        assertFalse(N.containsAny(main, new HashSet<>()));
        assertFalse(N.containsAny((Collection<?>) null, setMatch));
    }

    @Test
    public void testContainsNone() {
        Collection<String> main = Arrays.asList("a", "b", "c");
        assertTrue(N.containsNone(main, Arrays.asList("d", "e")));
        assertTrue(N.containsNone(main, "d", "e"));
        assertFalse(N.containsNone(main, Arrays.asList("c", "d")));
        assertFalse(N.containsNone(main, "c", "d"));
        assertTrue(N.containsNone(main, Collections.emptyList()));
        assertTrue(N.containsNone(main));
        assertTrue(N.containsNone(main, (Collection<?>) null));
        assertTrue(N.containsNone(main, (Object[]) null));
        assertTrue(N.containsNone(null, Arrays.asList("a")));
        assertTrue(N.containsNone(Collections.emptyList(), Arrays.asList("a")));

        assertTrue(N.containsNone((Iterable<?>) main, new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsNone((Iterable<?>) main, new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone((Iterable<String>) null, new HashSet<>(Arrays.asList("a"))));

        assertTrue(N.containsNone(main.iterator(), new HashSet<>(Arrays.asList("d", "e"))));
        assertFalse(N.containsNone(main.iterator(), new HashSet<>(Arrays.asList("c", "d"))));
        assertTrue(N.containsNone((Iterator<String>) null, new HashSet<>(Arrays.asList("a"))));

        Set<String> somePresent = new HashSet<>(Arrays.asList("c", "d"));
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));
        assertFalse(N.containsNone(main, somePresent));
        assertTrue(N.containsNone(main, nonePresent));
        assertTrue(N.containsNone(main, new HashSet<>()));
        assertTrue(N.containsNone((Collection<?>) null, somePresent));
    }

    @Test
    public void testContainsAnyContainsNone_listAndSet_notAmbiguous() {
        List<String> list = Arrays.asList("a", "b", "c");
        Set<String> somePresent = new HashSet<>(Arrays.asList("b", "x"));
        Set<String> nonePresent = new HashSet<>(Arrays.asList("x", "y"));

        assertTrue(N.containsAny(list, somePresent));
        assertFalse(N.containsAny(list, nonePresent));
        assertFalse(N.containsNone(list, somePresent));
        assertTrue(N.containsNone(list, nonePresent));
        assertTrue(N.containsAll(list, new HashSet<>(Arrays.asList("a", "b"))));
        assertFalse(N.containsAll(list, new HashSet<>(Arrays.asList("a", "x"))));
        assertFalse(N.containsAny(list, new HashSet<>()));
        assertTrue(N.containsNone(list, new HashSet<>()));
    }

    @Test
    public void testContainsDuplicates() {
        assertFalse(N.containsDuplicates(new boolean[] { true, false }));
        assertTrue(N.containsDuplicates(new boolean[] { true, false, true }));
        assertFalse(N.containsDuplicates(new char[] { 'a', 'b', 'c' }));
        assertTrue(N.containsDuplicates(new char[] { 'a', 'b', 'a' }));
        assertFalse(N.containsDuplicates(new int[] { 1, 2, 3 }));
        assertTrue(N.containsDuplicates(new int[] { 1, 2, 1 }));
        assertTrue(N.containsDuplicates(new int[] { 1, 1, 2, 3 }, true));
        assertFalse(N.containsDuplicates(new int[] { 1, 2, 3 }, true));
        assertFalse(N.containsDuplicates(new String[] { "a", "b", "c" }));
        assertTrue(N.containsDuplicates(Arrays.asList("x", "y", "x")));
        assertFalse(N.containsDuplicates(new int[0]));
        assertFalse(N.containsDuplicates((int[]) null));
        assertFalse(N.containsDuplicates((Collection<?>) null));

        final char[] unsorted = { 'x', 'a', 'b', 'a', 'z' };
        assertTrue(N.containsDuplicates(unsorted, 1, 4, false));
        assertFalse(N.containsDuplicates(unsorted, 0, 3, false));

        final char[] sorted = { 'a', 'b', 'b', 'c', 'd' };
        assertTrue(N.containsDuplicates(sorted, 0, 4, true));
        assertFalse(N.containsDuplicates(sorted, 3, 5, true));

        char[] three = { 'x', 'a', 'a', 'b' };
        assertTrue(N.containsDuplicates(three, 1, 4, false));
        assertTrue(N.containsDuplicates(three, 1, 4, true));
        assertTrue(N.containsDuplicates(three, 1, 3, false));
        assertFalse(N.containsDuplicates(three, 0, 2, false));
    }
}
