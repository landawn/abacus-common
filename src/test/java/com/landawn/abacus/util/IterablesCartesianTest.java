package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.RandomAccess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class IterablesCartesianTest extends IterablesTestSupport {
    @Test
    public void testCartesianProductVarargs() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        assertEquals(8, product.size());

        assertEquals(Arrays.asList(1, 3, 5), product.get(0));
        assertEquals(Arrays.asList(2, 4, 6), product.get(7));
    }

    @Test
    public void testCartesianProductCollection() {
        Collection<Collection<Integer>> collections = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));
        List<List<Integer>> product = Iterables.cartesianProduct(collections);

        assertEquals(8, product.size());

        assertEquals(Arrays.asList(1, 3, 5), product.get(0));
        assertEquals(Arrays.asList(2, 4, 6), product.get(7));
    }

    @Test
    public void testCartesianProductThreeSets() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");
        List<String> list3 = Arrays.asList("x", "y");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2, list3);

        assertEquals(8, product.size());
        assertEquals(Arrays.asList("a", "1", "x"), product.get(0));
        assertEquals(Arrays.asList("b", "2", "y"), product.get(7));
    }

    @Test
    public void testCartesianProductContains() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertTrue(product.contains(Arrays.asList("a", "1")));
        assertTrue(product.contains(Arrays.asList("b", "2")));
        assertFalse(product.contains(Arrays.asList("c", "1")));
        assertFalse(product.contains(Arrays.asList("a", "1", "extra")));
        assertFalse(product.contains("not a list"));
    }

    @Test
    public void testCartesianProductRandomAccess() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("1", "2");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertTrue(product instanceof RandomAccess);

        assertEquals(Arrays.asList("b", "1"), product.get(2));
        assertEquals(Arrays.asList("a", "2"), product.get(1));
    }

    // ===================== cartesianProduct =====================

    @Test
    @SuppressWarnings("unchecked")
    public void testCartesianProduct_Dedicated() {
        List<List<Integer>> result = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertEquals(4, result.size());
        assertTrue(result.contains(Arrays.asList(1, 3)));
        assertTrue(result.contains(Arrays.asList(1, 4)));
        assertTrue(result.contains(Arrays.asList(2, 3)));
        assertTrue(result.contains(Arrays.asList(2, 4)));
    }

    @Test
    public void testCartesianProductCollection_Dedicated() {
        List<Collection<Integer>> collections = new ArrayList<>();
        collections.add(Arrays.asList(1, 2));
        collections.add(Arrays.asList(3, 4));
        List<List<Integer>> result = Iterables.cartesianProduct(collections);
        assertEquals(4, result.size());
    }

    // ===================== cartesianProduct edge cases =====================

    @Test
    public void testCartesianProduct_ContainsCheck() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(3, 4));
        assertTrue(product.contains(list(1, 3)));
        assertTrue(product.contains(list(2, 4)));
        assertFalse(product.contains(list(1, 5)));
        assertFalse(product.contains("not a list"));
    }

    @Test
    public void testCartesianProductWithEmptyList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList(), Arrays.asList(5, 6));

        assertEquals(0, product.size());
    }

    @Test
    public void testCartesianProductWithEmptyAxisAndOverflowingSuffix() {
        final List<Integer> largeAxis = Collections.nCopies(46_341, 1);

        for (int emptyAxis = 0; emptyAxis < 3; emptyAxis++) {
            for (final Collection<Integer> empty : Arrays.asList(Collections.<Integer> emptyList(), null)) {
                final List<Collection<Integer>> axes = new ArrayList<>(Collections.nCopies(3, largeAxis));
                axes.set(emptyAxis, empty);

                final List<List<Integer>> product = Iterables.cartesianProduct(axes);
                assertTrue(product.isEmpty());
                assertFalse(product.contains(Arrays.asList(1, 1, 1)));
                assertThrows(IndexOutOfBoundsException.class, () -> product.get(0));
            }
        }

        assertThrows(IllegalArgumentException.class, () -> Iterables.cartesianProduct(largeAxis, largeAxis));
    }

    @Test
    public void testCartesianProductNoLists() {
        List<List<Integer>> product = Iterables.cartesianProduct(new ArrayList<Collection<Integer>>());

        assertEquals(1, product.size());
        assertTrue(product.get(0).isEmpty());
    }

    @Test
    public void testCartesianProductSingleList() {
        List<List<Integer>> product = Iterables.cartesianProduct(Arrays.asList(Arrays.asList(1, 2, 3)));

        assertEquals(3, product.size());
        assertEquals(Arrays.asList(1), product.get(0));
        assertEquals(Arrays.asList(2), product.get(1));
        assertEquals(Arrays.asList(3), product.get(2));
    }

    @Test
    public void testCartesianProductCollectionOfCollections() {
        List<Collection<?>> listOfColls = new ArrayList<>();
        List<List<Object>> cpEmptyOuter = Iterables.cartesianProduct(listOfColls);
        assertEquals(1, cpEmptyOuter.size());
        assertTrue(cpEmptyOuter.get(0).isEmpty());

        listOfColls.add(Arrays.asList(1, 2));
        List<List<Object>> cpOneList = Iterables.cartesianProduct(listOfColls);
        assertEquals(2, cpOneList.size());
        assertEquals(Collections.singletonList(1), cpOneList.get(0));
        assertEquals(Collections.singletonList(2), cpOneList.get(1));

        listOfColls.add(Arrays.asList("A", "B"));
        List<List<Object>> cpTwoLists = Iterables.cartesianProduct(listOfColls);
        assertEquals(4, cpTwoLists.size());
        assertEquals(Arrays.asList(1, "A"), cpTwoLists.get(0));
        assertEquals(Arrays.asList(1, "B"), cpTwoLists.get(1));
        assertEquals(Arrays.asList(2, "A"), cpTwoLists.get(2));
        assertEquals(Arrays.asList(2, "B"), cpTwoLists.get(3));

        List<List<Object>> cpNullInput = Iterables.cartesianProduct((Collection<? extends Collection<?>>) null);
        assertEquals(1, cpNullInput.size());
        assertTrue(cpNullInput.get(0).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCartesianProductVarArgs() {
        List<List<Integer>> cp = Iterables.cartesianProduct(list(1, 2), list(3, 4));
        assertEquals(4, cp.size());
        assertTrue(cp.contains(list(1, 3)));
        assertTrue(cp.contains(list(1, 4)));
        assertTrue(cp.contains(list(2, 3)));
        assertTrue(cp.contains(list(2, 4)));
        assertEquals(list(1, 3), cp.get(0));
        assertEquals(list(1, 4), cp.get(1));
        assertEquals(list(2, 3), cp.get(2));
        assertEquals(list(2, 4), cp.get(3));

        List<List<Object>> cpMixed = Iterables.cartesianProduct(list(1, 2), list("a"), list(true, false));
        assertEquals(4, cpMixed.size());
        assertTrue(cpMixed.contains(list(1, "a", true)));
        assertTrue(cpMixed.contains(list(2, "a", false)));
        assertEquals(list(1, "a", true), cpMixed.get(0));

        List<List<Integer>> cpEmptyList = Iterables.cartesianProduct(list(1, 2), list());
        assertTrue(cpEmptyList.isEmpty());

        List<List<Integer>> cpNoLists = Iterables.cartesianProduct();
        assertEquals(1, cpNoLists.size());
        assertTrue(cpNoLists.get(0).isEmpty());
    }

    @Test
    public void testCartesianProductWithEmptySet() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> emptyList = Collections.emptyList();

        List<List<String>> product = Iterables.cartesianProduct(list1, emptyList);

        assertTrue(product.isEmpty());
    }

    @Test
    public void testCartesianProductEmpty() {
        List<Collection<String>> emptyCollections = Collections.emptyList();
        List<List<String>> product = Iterables.cartesianProduct(emptyCollections);

        assertEquals(1, product.size());
        assertEquals(Collections.emptyList(), product.get(0));
    }

    @Test
    public void testCartesianProductSingleElement() {
        List<String> singleList = Arrays.asList("a");
        List<List<String>> product = Iterables.cartesianProduct(singleList);

        assertEquals(1, product.size());
        assertEquals(Arrays.asList("a"), product.get(0));
    }

    @Test
    public void testCartesianProductIndexOutOfBounds() {
        List<String> list1 = Arrays.asList("a");
        List<String> list2 = Arrays.asList("1");

        List<List<String>> product = Iterables.cartesianProduct(list1, list2);

        assertThrows(IndexOutOfBoundsException.class, () -> product.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> product.get(-1));

        List<List<String>> product2 = Lists.cartesianProduct(list1, list2);

        assertThrows(IndexOutOfBoundsException.class, () -> product2.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> product2.get(-1));
    }

    @Test
    public void testCartesianProduct_EmptyInput() {
        // Counter-intuitive but mathematically consistent: zero axes -> one empty tuple.
        List<List<Object>> r = Iterables.cartesianProduct();
        assertEquals(1, r.size());
        assertTrue(r.get(0).isEmpty());
    }

    @Test
    public void testCartesianProduct_OneEmptyAxis() {
        List<List<Integer>> r = Iterables.cartesianProduct(Arrays.asList(1, 2), Collections.<Integer> emptyList());
        assertEquals(0, r.size());
    }

    @Test
    public void testCartesianProduct_Sizes() {
        List<List<Integer>> r = Iterables.cartesianProduct(Arrays.asList(1, 2, 3), Arrays.asList(10, 20));
        assertEquals(6, r.size());
        assertEquals(Arrays.asList(1, 10), r.get(0));
        assertEquals(Arrays.asList(3, 20), r.get(5));
        assertFalse(r.contains(new LinkedHashSet<>(Arrays.asList(1, 10))));
    }

    @Test
    public void testCartesianProductNullAxisIsEmpty() {
        final List<Collection<? extends Integer>> axes = Arrays.asList(Arrays.asList(1), null);

        assertTrue(Iterables.cartesianProduct(axes).isEmpty());
    }

    @Test
    public void testCartesianProduct_tupleIsAFreshModifiableListPerGet() {
        final List<List<Object>> product = Iterables.<Object> cartesianProduct(Arrays.asList(1, 2), Arrays.asList("A"));

        final List<Object> first = product.get(0);
        assertEquals(Arrays.asList(1, "A"), first);

        first.add("MUTATED");
        assertEquals(Arrays.asList(1, "A"), product.get(0), "mutating a tuple must not affect the product");
        Assertions.assertNotSame(first, product.get(0), "each get() builds a fresh tuple");
    }

    @Test
    public void testCartesianProduct_toStringDoesNotMaterialiseAHugeProduct() {
        final List<Integer> axis = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            axis.add(i);
        }

        // 1000^3 == 1e9 tuples: AbstractCollection.toString() would have tried to build all of them
        final List<List<Integer>> product = Iterables.cartesianProduct(axis, axis, axis);
        assertEquals(1_000_000_000, product.size());
        assertTrue(product.toString().startsWith("cartesianProduct([[0, 1, 2,"));
    }
}
