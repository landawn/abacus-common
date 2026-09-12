package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.MultisetBuilder;

public class BuilderMultisetTest extends BuilderTestSupport {
    @Test
    public void testMultisetBuilder_setCount_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.setCount("a", 5));
    }

    @Test
    public void testMultisetBuilder_setCount() {
        Multiset<String> multiset = Multiset.of("a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.setCount("a", 5);
        assertEquals(5, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_add() {
        Multiset<String> multiset = Multiset.of();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a").add("a");
        assertEquals(2, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_addWithOccurrences() {
        Multiset<String> multiset = Multiset.of();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a", 3);
        assertEquals(3, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_remove() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.remove("a");
        assertEquals(1, builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_removeWithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.remove("a", 2);
        assertEquals(1, builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAllOccurrencesOf("a");
        assertEquals(0, builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_removeAll() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAll(Arrays.asList("a"));
        assertEquals(0, builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrencesCollection() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAllOccurrencesOfAll(Arrays.asList("a", "b"));
        assertEquals(0, builder.val().getCount("a"));
        assertEquals(0, builder.val().getCount("b"));
    }

    @Test
    public void testMultisetBuilder() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a").add("a", 2).setCount("b", 3);
        assertEquals(3, multiset.count("a"));
        assertEquals(3, multiset.count("b"));
        builder.remove("a");
        assertEquals(2, multiset.count("a"));
        builder.remove("a", 2);
        assertEquals(0, multiset.count("a"));
        builder.removeAllOccurrencesOfAll(Arrays.asList("b"));
        assertEquals(0, multiset.count("b"));
    }

    @Test
    public void testMultisetBuilderSetCount() {
        Multiset<String> multiset = new Multiset<>();
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.setCount("test", 3);
        Assertions.assertEquals(3, multiset.getCount("test"));

        builder.setCount("test", 0);
        Assertions.assertEquals(0, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderAdd() {
        Multiset<String> multiset = new Multiset<>();
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.add("test");
        Assertions.assertEquals(1, multiset.getCount("test"));

        builder.add("test", 2);
        Assertions.assertEquals(3, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderRemove() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("test", 3);
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.remove("test");
        Assertions.assertEquals(2, multiset.getCount("test"));

        builder.remove("test", 2);
        Assertions.assertEquals(0, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderRemoveAllOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("test", 5);
        multiset.add("other", 3);
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.removeAllOccurrencesOf("test");
        Assertions.assertEquals(0, multiset.getCount("test"));
        Assertions.assertEquals(3, multiset.getCount("other"));

        builder.removeAllOccurrencesOfAll(Arrays.asList("other"));
        Assertions.assertEquals(0, multiset.getCount("other"));
    }

    @Test
    public void testMultisetBuilder_of() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        assertNotNull(builder);
        assertEquals(2, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilderEdgeCases() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);

        builder.add("item", 3).remove("item", 5);

        assertEquals(0, multiset.count("item"));

        builder.setCount("item2", 10).setCount("item2", 5);

        assertEquals(5, multiset.count("item2"));
    }

    @Test
    public void testMultisetBuilder_add_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testMultisetBuilder_addWithOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.add("a", 3));
    }

    @Test
    public void testMultisetBuilder_remove_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testMultisetBuilder_removeWithOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.remove("a", 2));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAllOccurrencesOf("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrencesCollection_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAllOccurrencesOfAll(Arrays.asList("a")));
    }

    @Test
    public void testMultisetBuilder_removeAll_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }
}
