package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.MultimapBuilder;

public class BuilderMultimapTest extends BuilderTestSupport {
    @Test
    public void testMultimapBuilder_put_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.put("a", 1));
    }

    @Test
    public void testMultimapBuilder_put() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put("a", 1).put("a", 2);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put(map);
        assertEquals(2, builder.val().totalValueCount());
    }

    @Test
    public void testMultimapBuilder_putMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany("a", Arrays.asList(1, 2, 3));
        assertEquals(3, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(map);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMultimap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("a", 1);
        other.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(other);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeOne() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("a", 1);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeAll() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("a");
        assertFalse(builder.val().containsKey("a"));
    }

    @Test
    public void testMultimapBuilder_removeMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("a", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("a", Arrays.asList(1, 2));
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeManyMultimap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany(toRemove);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put("a", 1).put("a", 2).putMany("b", Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2), multimap.get("a"));
        assertEquals(Arrays.asList(3, 4), multimap.get("b"));
        builder.removeOne("a", 1);
        assertEquals(Arrays.asList(2), multimap.get("a"));
        builder.removeAll("b");
        assertFalse(multimap.containsKey("b"));
    }

    @Test
    public void testMultimapBuilderPut() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.put("key", 1).put("key", 2);
        Assertions.assertEquals(2, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put(map);

        Assertions.assertEquals(1, multimap.get("key1").size());
        Assertions.assertEquals(1, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderPutMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.putMany("key", Arrays.asList(1, 2, 3));
        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(1, 2));
        map.put("key2", Arrays.asList(3, 4));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(map);

        Assertions.assertEquals(2, multimap.get("key1").size());
        Assertions.assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderPutManyMultimap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> toAdd = CommonUtil.newListMultimap();
        toAdd.putValues("key", Arrays.asList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(toAdd);

        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderRemoveOne() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("key", 1);
        multimap.put("key", 2);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("key", 1);

        Assertions.assertEquals(1, multimap.get("key").size());
        Assertions.assertEquals(Integer.valueOf(2), multimap.get("key").get(0));
    }

    @Test
    public void testMultimapBuilderRemoveOneMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 1);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne(toRemove);

        Assertions.assertFalse(multimap.containsKey("key1"));
        Assertions.assertEquals(1, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderRemoveAll() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.putValues("key", CommonUtil.toList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("key");

        Assertions.assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testMultimapBuilderRemoveMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.putValues("key", Arrays.asList(1, 2, 3, 4));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("key", Arrays.asList(1, 3));

        Assertions.assertEquals(2, multimap.get("key").size());
        Assertions.assertTrue(multimap.get("key").contains(2));
        Assertions.assertTrue(multimap.get("key").contains(4));
    }

    @Test
    public void testMultimapBuilderPutManyFromMultimap() {
        Multimap<String, Integer, List<Integer>> multimap1 = CommonUtil.newListMultimap();
        Multimap<String, Integer, List<Integer>> multimap2 = CommonUtil.newListMultimap();

        multimap2.put("key1", 10);
        multimap2.put("key1", 20);
        multimap2.put("key2", 30);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap1);

        builder.putMany(multimap2);

        assertEquals(Arrays.asList(10, 20), multimap1.get("key1"));
        assertEquals(Arrays.asList(30), multimap1.get("key2"));
    }

    @Test
    public void testMultimapBuilder_removeMany_collection() {
        Multimap<String, Integer, List<Integer>> mm = new Multimap<>();
        mm.putValues("key", Arrays.asList(1, 2, 3, 4));
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(mm);
        builder.removeMany("key", Arrays.asList(2, 3));
        assertEquals(2, builder.val().get("key").size());
    }

    @Test
    public void testMultimapBuilder_removeMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = new Multimap<>();
        mm.putValues("key", Arrays.asList(1, 2, 3));
        Multimap<String, Integer, List<Integer>> toRemove = new Multimap<>();
        toRemove.put("key", 2);
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(mm);
        builder.removeMany(toRemove);
        assertEquals(2, builder.val().get("key").size());
    }

    @Test
    public void testMultimapBuilder_of() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertEquals(1, builder.val().totalValueCount());
    }

    @Test
    public void testMultimapBuilder_removeOneMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("b", 2);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne(toRemove);
        assertNull(builder.val().get("a"));
    }

    @Test
    public void testMultimapBuilder_removeManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 2));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany(toRemove);
        assertNull(builder.val().get("a"));
    }

    @Test
    public void testMultimapBuilderRemoveOperations() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key1", 3);
        multimap.put("key2", 4);
        multimap.put("key3", 5);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        Map<String, Integer> toRemoveOne = new HashMap<>();
        toRemoveOne.put("key1", 2);
        builder.removeOne(toRemoveOne);

        assertEquals(Arrays.asList(1, 3), multimap.get("key1"));

        builder.removeMany("key1", Arrays.asList(1, 3));
        assertNull(multimap.get("key1"));

        builder.removeAll("key2");
        assertNull(multimap.get("key2"));

        Map<String, Collection<Integer>> toRemoveMany = new HashMap<>();
        toRemoveMany.put("key3", Arrays.asList(5));
        builder.removeMany(toRemoveMany);
        assertNull(multimap.get("key3"));
    }

    @Test
    public void testMultimapBuilder_putMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.put(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_putMany_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany("a", Arrays.asList(1)));
    }

    @Test
    public void testMultimapBuilder_putManyMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_putManyMultimap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany(CommonUtil.newListMultimap()));
    }

    @Test
    public void testMultimapBuilder_removeOne_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeOne("a", 1));
    }

    @Test
    public void testMultimapBuilder_removeOneMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeOne(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_removeAll_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeAll("a"));
    }

    @Test
    public void testMultimapBuilder_removeMany_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany("a", Arrays.asList(1)));
    }

    @Test
    public void testMultimapBuilder_removeManyMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_removeManyMultimap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany(CommonUtil.newListMultimap()));
    }
}
