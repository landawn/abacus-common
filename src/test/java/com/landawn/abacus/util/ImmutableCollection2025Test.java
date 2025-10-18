package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableCollection2025Test extends TestBase {

    @Test
    public void test_wrap_withNormalCollection() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(collection);

        Assertions.assertNotNull(wrapped);
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("a"));
        Assertions.assertTrue(wrapped.contains("b"));
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void test_wrap_withNull() {
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(null);

        Assertions.assertNotNull(wrapped);
        Assertions.assertTrue(wrapped.isEmpty());
        Assertions.assertEquals(0, wrapped.size());
        Assertions.assertTrue(wrapped instanceof ImmutableList);
    }

    @Test
    public void test_wrap_withAlreadyImmutableCollection() {
        ImmutableList<String> immutableList = ImmutableList.of("x", "y", "z");
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(immutableList);

        Assertions.assertSame(immutableList, wrapped);
    }

    @Test
    public void test_wrap_withImmutableSet() {
        ImmutableSet<Integer> immutableSet = ImmutableSet.of(1, 2, 3);
        ImmutableCollection<Integer> wrapped = ImmutableCollection.wrap(immutableSet);

        Assertions.assertSame(immutableSet, wrapped);
    }

    @Test
    public void test_wrap_reflectsUnderlyingChanges() {
        List<String> mutableList = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(mutableList);

        Assertions.assertEquals(2, wrapped.size());
        Assertions.assertTrue(wrapped.contains("a"));

        mutableList.add("c");

        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void test_wrap_withEmptyCollection() {
        Collection<String> empty = Collections.emptyList();
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(empty);

        Assertions.assertNotNull(wrapped);
        Assertions.assertTrue(wrapped.isEmpty());
        Assertions.assertEquals(0, wrapped.size());
    }

    @Test
    public void test_wrap_withSet() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        ImmutableCollection<Integer> wrapped = ImmutableCollection.wrap(set);

        Assertions.assertEquals(5, wrapped.size());
        Assertions.assertTrue(wrapped.contains(3));
        Assertions.assertFalse(wrapped.contains(10));
    }

    @Test
    public void test_add_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.add("c"));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_addAll_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> collection.addAll(Arrays.asList("c", "d")));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_addAll_withEmptyCollection_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.addAll(Collections.emptyList()));
    }

    @Test
    public void test_remove_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.remove("a"));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_remove_withNonExistentElement_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.remove("z"));
    }

    @Test
    public void test_removeIf_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.removeIf(s -> s.equals("a")));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_removeIf_withAlwaysFalsePredicate_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.removeIf(s -> false));
    }

    @Test
    public void test_removeAll_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> collection.removeAll(Arrays.asList("a", "b")));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_removeAll_withEmptyCollection_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.removeAll(Collections.emptyList()));
    }

    @Test
    public void test_retainAll_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class,
                () -> collection.retainAll(Arrays.asList("a", "b")));

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_retainAll_withAllElements_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.retainAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void test_clear_throwsUnsupportedOperationException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        UnsupportedOperationException exception = Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.clear());

        Assertions.assertNotNull(exception);
    }

    @Test
    public void test_clear_onEmptyCollection_stillThrowsException() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.clear());
    }

    @Test
    public void test_contains_withExistingElement() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertTrue(collection.contains("a"));
        Assertions.assertTrue(collection.contains("b"));
        Assertions.assertTrue(collection.contains("c"));
    }

    @Test
    public void test_contains_withNonExistentElement() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertFalse(collection.contains("z"));
        Assertions.assertFalse(collection.contains(""));
    }

    @Test
    public void test_contains_withNull() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", null, "b"));

        Assertions.assertTrue(collection.contains(null));
    }

    @Test
    public void test_contains_withNullNotPresent() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertFalse(collection.contains(null));
    }

    @Test
    public void test_iterator_hasCorrectElements() {
        List<String> original = Arrays.asList("a", "b", "c");
        ImmutableCollection<String> collection = ImmutableCollection.wrap(original);

        ObjIterator<String> iterator = collection.iterator();
        Assertions.assertNotNull(iterator);

        List<String> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }

        Assertions.assertEquals(original, iterated);
    }

    @Test
    public void test_iterator_onEmptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        ObjIterator<String> iterator = collection.iterator();
        Assertions.assertNotNull(iterator);
        Assertions.assertFalse(iterator.hasNext());
    }

    @Test
    public void test_iterator_multipleIterations() {
        ImmutableCollection<Integer> collection = ImmutableCollection.wrap(Arrays.asList(1, 2, 3));

        ObjIterator<Integer> iter1 = collection.iterator();
        int count1 = 0;
        while (iter1.hasNext()) {
            iter1.next();
            count1++;
        }

        ObjIterator<Integer> iter2 = collection.iterator();
        int count2 = 0;
        while (iter2.hasNext()) {
            iter2.next();
            count2++;
        }

        Assertions.assertEquals(3, count1);
        Assertions.assertEquals(3, count2);
    }

    @Test
    public void test_size_normalCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c", "d", "e"));

        Assertions.assertEquals(5, collection.size());
    }

    @Test
    public void test_size_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        Assertions.assertEquals(0, collection.size());
    }

    @Test
    public void test_size_singleElement() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("only"));

        Assertions.assertEquals(1, collection.size());
    }

    @Test
    public void test_size_withDuplicates() {
        ImmutableCollection<Integer> collection = ImmutableCollection.wrap(Arrays.asList(1, 2, 2, 3, 3, 3));

        Assertions.assertEquals(6, collection.size());
    }

    @Test
    public void test_toArray_noArgs() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Object[] array = collection.toArray();

        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length);
        Assertions.assertArrayEquals(new Object[] { "a", "b", "c" }, array);
    }

    @Test
    public void test_toArray_noArgs_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        Object[] array = collection.toArray();

        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);
    }

    @Test
    public void test_toArray_noArgs_modification() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Object[] array = collection.toArray();
        array[0] = "modified";

        Assertions.assertTrue(collection.contains("a"));
        Assertions.assertFalse(collection.contains("modified"));
    }

    @Test
    public void test_toArray_withTypedArray_exactSize() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        String[] array = new String[3];
        String[] result = collection.toArray(array);

        Assertions.assertSame(array, result);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void test_toArray_withTypedArray_smallerSize() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        String[] array = new String[1];
        String[] result = collection.toArray(array);

        Assertions.assertNotSame(array, result);
        Assertions.assertEquals(3, result.length);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void test_toArray_withTypedArray_largerSize() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        String[] array = new String[5];
        String[] result = collection.toArray(array);

        Assertions.assertSame(array, result);
        Assertions.assertEquals("a", result[0]);
        Assertions.assertEquals("b", result[1]);
        Assertions.assertEquals("c", result[2]);
        Assertions.assertNull(result[3]);
        Assertions.assertNull(result[4]);
    }

    @Test
    public void test_toArray_withTypedArray_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        String[] array = new String[2];
        String[] result = collection.toArray(array);

        Assertions.assertSame(array, result);
        Assertions.assertNull(result[0]);
    }

    @Test
    public void test_toArray_withTypedArray_zeroLength() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("x", "y"));

        String[] result = collection.toArray(new String[0]);

        Assertions.assertEquals(2, result.length);
        Assertions.assertArrayEquals(new String[] { "x", "y" }, result);
    }

    @Test
    public void test_equals_sameInstance() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertTrue(collection.equals(collection));
    }

    @Test
    public void test_equals_equalCollections() {
        ImmutableCollection<String> collection1 = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Collection<String> collection2 = Arrays.asList("a", "b", "c");

        Assertions.assertFalse(collection1.equals(collection2));
    }

    @Test
    public void test_equals_differentCollections() {
        ImmutableCollection<String> collection1 = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Collection<String> collection2 = Arrays.asList("a", "b", "d");

        Assertions.assertFalse(collection1.equals(collection2));
    }

    @Test
    public void test_equals_differentSizes() {
        ImmutableCollection<String> collection1 = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Collection<String> collection2 = Arrays.asList("a", "b");

        Assertions.assertFalse(collection1.equals(collection2));
    }

    @Test
    public void test_equals_withNull() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertFalse(collection.equals(null));
    }

    @Test
    public void test_equals_withNonCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertFalse(collection.equals("not a collection"));
        Assertions.assertFalse(collection.equals(123));
    }

    @Test
    public void test_equals_emptyCollections() {
        ImmutableCollection<String> collection1 = ImmutableCollection.wrap(Collections.emptyList());
        Collection<String> collection2 = Collections.emptyList();

        Assertions.assertFalse(collection1.equals(collection2));
    }

    @Test
    public void test_equals_withDifferentCollectionTypes() {
        ImmutableCollection<Integer> collection1 = ImmutableCollection.wrap(Arrays.asList(1, 2, 3));
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));

        Assertions.assertNotNull(collection1);
        Assertions.assertNotNull(set);
    }

    @Test
    public void test_hashCode_consistency() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        int hash1 = collection.hashCode();
        int hash2 = collection.hashCode();

        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashCode_equalCollections() {
        ImmutableCollection<String> collection1 = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        ImmutableCollection<String> collection2 = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertEquals(collection1.hashCode(), collection2.hashCode());
    }

    @Test
    public void test_hashCode_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        int hashCode = collection.hashCode();
        Assertions.assertNotNull(hashCode);
    }

    @Test
    public void test_toString_normalCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        String str = collection.toString();

        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
        Assertions.assertTrue(str.contains("c"));
    }

    @Test
    public void test_toString_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        String str = collection.toString();

        Assertions.assertNotNull(str);
    }

    @Test
    public void test_toString_withNull() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", null, "b"));

        String str = collection.toString();

        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("null"));
    }

    @Test
    public void test_toString_singleElement() {
        ImmutableCollection<Integer> collection = ImmutableCollection.wrap(Arrays.asList(42));

        String str = collection.toString();

        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("42"));
    }

    @Test
    public void test_immutability_addNotAllowed() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        int sizeBefore = collection.size();

        try {
            collection.add("c");
            Assertions.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        Assertions.assertEquals(sizeBefore, collection.size());
    }

    @Test
    public void test_immutability_removeNotAllowed() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        int sizeBefore = collection.size();

        try {
            collection.remove("b");
            Assertions.fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        Assertions.assertEquals(sizeBefore, collection.size());
        Assertions.assertTrue(collection.contains("b"));
    }

    @Test
    public void test_typeCompatibility_integers() {
        Collection<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        ImmutableCollection<Integer> immutable = ImmutableCollection.wrap(numbers);

        Assertions.assertEquals(5, immutable.size());
        Assertions.assertTrue(immutable.contains(3));
    }

    @Test
    public void test_typeCompatibility_customObjects() {
        Collection<Object> objects = Arrays.asList(new Object(), new Object());
        ImmutableCollection<Object> immutable = ImmutableCollection.wrap(objects);

        Assertions.assertEquals(2, immutable.size());
    }

    @Test
    public void test_containsAll_allPresent() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c", "d"));

        Assertions.assertTrue(collection.containsAll(Arrays.asList("a", "b")));
        Assertions.assertTrue(collection.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void test_containsAll_someNotPresent() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertFalse(collection.containsAll(Arrays.asList("a", "z")));
    }

    @Test
    public void test_isEmpty_emptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Collections.emptyList());

        Assertions.assertTrue(collection.isEmpty());
    }

    @Test
    public void test_isEmpty_nonEmptyCollection() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a"));

        Assertions.assertFalse(collection.isEmpty());
    }
}
