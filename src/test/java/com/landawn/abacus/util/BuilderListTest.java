package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.ListBuilder;

public class BuilderListTest extends BuilderTestSupport {
    @Test
    public void testListBuilder_add_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testListBuilder_set() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);

        assertSame(builder, builder.set(1, "updated"));
        assertEquals(Arrays.asList("a", "updated", "c"), list);
        assertThrows(IndexOutOfBoundsException.class, () -> builder.set(3, "invalid"));
    }

    @Test
    public void testListBuilder_add() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add("a").add("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_addAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add(1, "b");
        assertEquals("b", builder.val().get(1));
    }

    @Test
    public void testListBuilder_addAll() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(Arrays.asList("b", "c"));
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAllVarargs() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll("b", "c");
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAllAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(1, Arrays.asList("b"));
        assertEquals("b", builder.val().get(1));
    }

    @Test
    public void testListBuilder_remove() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.remove("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_removeAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.remove(1);
        assertEquals(2, builder.val().size());
        assertEquals("c", builder.val().get(1));
    }

    @Test
    public void testListBuilder_removeAll() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll(Arrays.asList("b"));
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_removeAllVarargs() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll("b", "c");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testListBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add("a").add(0, "b").addAll(Arrays.asList("c", "d")).addAll(0, Arrays.asList("e", "f"));
        assertEquals(Arrays.asList("e", "f", "b", "a", "c", "d"), list);
        builder.remove(0);
        assertEquals(Arrays.asList("f", "b", "a", "c", "d"), list);
    }

    @Test
    public void testListBuilderRemoveAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.remove(1);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("c", list.get(1));
    }

    @Test
    public void testListBuilder_of() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAllAtIndex_emptyCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(1, new ArrayList<>());
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilderOperations() {
        List<String> list = new ArrayList<>();
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.add("one").add(0, "zero").addAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("zero", list.get(0));

        builder.remove("zero");
        Assertions.assertEquals(3, list.size());

        builder.removeAll(Arrays.asList("one", "two"));
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("three", list.get(0));
    }

    @Test
    public void testListBuilderAddAllAtIndex() {
        List<String> list = new ArrayList<>();
        list.add("one");
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.addAll(0, Arrays.asList("zero"));
        Assertions.assertEquals("zero", list.get(0));
        Assertions.assertEquals("one", list.get(1));
    }

    @Test
    public void testListBuilder_removeAll_empty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll(Collections.emptyList());
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAll_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll(Arrays.asList("a")));
    }

    @Test
    public void testListBuilder_addAllVarargs_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll("a", "b"));
    }

    @Test
    public void testListBuilder_remove_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testListBuilder_removeAll_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testListBuilder_removeAllVarargs_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.removeAll("a", "b"));
    }

    @Test
    public void testListBuilder_addAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.add(0, "b"));
    }

    @Test
    public void testListBuilder_addAllAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll(0, Arrays.asList("b")));
    }

    @Test
    public void testListBuilder_removeAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.remove(0));
    }

    @Test
    public void testListBuilder_addAll_nullCollection() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll((Collection<String>) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testListBuilder_removeAll_nullCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.removeAll((Collection<?>) null);
        assertEquals(1, list.size());
    }

    @Test
    public void testListBuilder_addAllAtIndex_nullCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll(0, (Collection<String>) null);
        assertEquals(1, list.size());
    }

    @Test
    public void testListBuilder_addAllVarargs_null() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll((String[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testListBuilder_removeAllVarargs_null() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.removeAll((String[]) null);
        assertEquals(1, list.size());
    }
}
