package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.CollectionBuilder;

public class BuilderCollectionTest extends BuilderTestSupport {
    @Test
    public void testCollectionBuilder_add_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testCollectionBuilder_add() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.add("a").add("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_addAll() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.addAll(Arrays.asList("b", "c"));
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_addAllVarargs() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.addAll("b", "c");
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_remove() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.remove("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_removeAll() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll(Arrays.asList("b", "c"));
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll("b", "c");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testCollectionBuilderOperations() {
        Set<String> set = new HashSet<>();
        Builder.CollectionBuilder<String, Set<String>> builder = Builder.of(set);

        builder.add("one").addAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(3, set.size());

        builder.remove("one");
        Assertions.assertEquals(2, set.size());

        builder.removeAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void testCollectionBuilderAddAllVarargs() {
        Set<Integer> set = new LinkedHashSet<>();
        Builder.CollectionBuilder<Integer, Set<Integer>> builder = Builder.of(set);

        builder.addAll(1, 2, 3);
        Assertions.assertEquals(3, set.size());

        builder.removeAll(1, 2);
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains(3));
    }

    @Test
    public void testCollectionBuilderVarargs() {
        CollectionBuilder<String, Set<String>> builder = Builder.of(testSet);

        builder.addAll("p", "q").removeAll("x", "y");

        assertTrue(testSet.contains("p"));
        assertTrue(testSet.contains("q"));
        assertFalse(testSet.contains("x"));
        assertFalse(testSet.contains("y"));
    }

    @Test
    public void testCollectionBuilder_of() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder() {
        Collection<String> coll = new ArrayList<>();
        CollectionBuilder<String, Collection<String>> builder = Builder.of(coll);
        builder.add("a").addAll(Arrays.asList("b", "c")).addAll("d", "e");
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), coll);
        builder.remove("a");
        assertEquals(Arrays.asList("b", "c", "d", "e"), coll);
        builder.removeAll(Arrays.asList("b", "c"));
        assertEquals(Arrays.asList("d", "e"), coll);
        builder.removeAll("d", "e");
        assertTrue(coll.isEmpty());
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs_empty() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll();
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_addAll_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.addAll(Arrays.asList("a")));
    }

    @Test
    public void testCollectionBuilder_addAllVarargs_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.addAll("a", "b"));
    }

    @Test
    public void testCollectionBuilder_remove_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testCollectionBuilder_removeAll_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.removeAll("a", "b"));
    }

    @Test
    public void testCollectionBuilder_addAll_nullCollection() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.addAll((Collection<String>) null);
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionBuilder_removeAll_nullCollection() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.removeAll((Collection<?>) null);
        assertEquals(1, set.size());
    }

    @Test
    public void testCollectionBuilder_addAllVarargs_null() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.addAll((String[]) null);
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs_null() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.removeAll((String[]) null);
        assertEquals(1, set.size());
    }
}
