package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.BooleanListBuilder;
import com.landawn.abacus.util.Builder.ByteListBuilder;
import com.landawn.abacus.util.Builder.CharListBuilder;
import com.landawn.abacus.util.Builder.CollectionBuilder;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.Builder.DoubleListBuilder;
import com.landawn.abacus.util.Builder.FloatListBuilder;
import com.landawn.abacus.util.Builder.IntListBuilder;
import com.landawn.abacus.util.Builder.ListBuilder;
import com.landawn.abacus.util.Builder.LongListBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultimapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.Builder.ShortListBuilder;

public class BuilderOfTest extends BuilderTestSupport {
    @Test
    public void testOfBooleanList() {
        BooleanList bl = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(bl);
        assertNotNull(builder);
        assertSame(bl, builder.val());
    }

    @Test
    public void testOfCharList() {
        CharList cl = CharList.of('a', 'b');
        CharListBuilder builder = Builder.of(cl);
        assertNotNull(builder);
        assertSame(cl, builder.val());
    }

    @Test
    public void testOfByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2);
        ByteListBuilder builder = Builder.of(bl);
        assertNotNull(builder);
        assertSame(bl, builder.val());
    }

    @Test
    public void testOfShortList() {
        ShortList sl = ShortList.of((short) 1, (short) 2);
        ShortListBuilder builder = Builder.of(sl);
        assertNotNull(builder);
        assertSame(sl, builder.val());
    }

    @Test
    public void testOfIntList() {
        IntList il = IntList.of(1, 2);
        IntListBuilder builder = Builder.of(il);
        assertNotNull(builder);
        assertSame(il, builder.val());
    }

    @Test
    public void testOfLongList() {
        LongList ll = LongList.of(1L, 2L);
        LongListBuilder builder = Builder.of(ll);
        assertNotNull(builder);
        assertSame(ll, builder.val());
    }

    @Test
    public void testOfFloatList() {
        FloatList fl = FloatList.of(1.0f, 2.0f);
        FloatListBuilder builder = Builder.of(fl);
        assertNotNull(builder);
        assertSame(fl, builder.val());
    }

    @Test
    public void testOfDoubleList() {
        DoubleList dl = DoubleList.of(1.0, 2.0);
        DoubleListBuilder builder = Builder.of(dl);
        assertNotNull(builder);
        assertSame(dl, builder.val());
    }

    @Test
    public void testOfList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        assertNotNull(builder);
        assertSame(list, builder.val());
    }

    @Test
    public void testOfCollection() {
        Collection<String> coll = new HashSet<>(Arrays.asList("a", "b"));
        CollectionBuilder<String, Collection<String>> builder = Builder.of(coll);
        assertNotNull(builder);
        assertSame(coll, builder.val());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        assertNotNull(builder);
        assertSame(map, builder.val());
    }

    @Test
    public void testOfMultiset() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        assertNotNull(builder);
        assertSame(multiset, builder.val());
    }

    @Test
    public void testOfMultimap() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertSame(multimap, builder.val());
    }

    @Test
    public void testOfDataset() {
        Dataset dataset = new RowDataset(Arrays.asList("col1"), CommonUtil.toList(new ArrayList<>()));
        DatasetBuilder builder = Builder.of(dataset);
        assertNotNull(builder);
        assertSame(dataset, builder.val());
    }

    @Test
    public void testOfGeneric() {
        String str = "test";
        Builder<String> builder = Builder.of(str);
        Assertions.assertNotNull(builder);
        Assertions.assertEquals(str, builder.val());

        Integer num = 42;
        Builder<Integer> intBuilder = Builder.of(num);
        Assertions.assertEquals(num, intBuilder.val());

        List<String> list = new ArrayList<>();
        Builder builder2 = Builder.of(list);
        Assertions.assertTrue(builder2 instanceof Builder.ListBuilder);
    }

    @Test
    public void testOf_Object_PlainObject() {
        String val = "hello";
        Builder<String> builder = Builder.of(val);
        assertNotNull(builder);
        assertEquals(val, builder.val());
    }

    @Test
    public void testOf_Object_List_DispatchesToListBuilder() {
        // A custom list not in the creatorMap but instanceof List
        java.util.Vector<String> vector = new java.util.Vector<>();
        Builder<?> builder = Builder.of(vector);
        assertNotNull(builder);
        assertTrue(builder instanceof Builder.ListBuilder);
    }

    @Test
    public void testOf_Object_Collection_DispatchesToCollectionBuilder() {
        // A Set (not List, not Multiset)
        java.util.TreeSet<String> treeSet = new java.util.TreeSet<>();
        Builder<?> builder = Builder.of(treeSet);
        assertNotNull(builder);
        assertTrue(builder instanceof Builder.CollectionBuilder);
    }

    @Test
    public void testOf_Object_Map_DispatchesToMapBuilder() {
        // A custom map not in the creatorMap but instanceof Map
        java.util.WeakHashMap<String, Integer> weakMap = new java.util.WeakHashMap<>();
        Builder<?> builder = Builder.of(weakMap);
        assertNotNull(builder);
        assertTrue(builder instanceof Builder.MapBuilder);
    }

    @Test
    public void testOf_Object_KnownListType_UsesCreatorMap() {
        ArrayList<String> list = new ArrayList<>();
        Builder<?> builder = Builder.of(list);
        assertNotNull(builder);
        assertTrue(builder instanceof Builder.ListBuilder);
    }

    @Test
    public void testOf_Object_KnownMapType_UsesCreatorMap() {
        HashMap<String, Integer> map = new HashMap<>();
        Builder<?> builder = Builder.of(map);
        assertNotNull(builder);
        assertTrue(builder instanceof Builder.MapBuilder);
    }

    @Test
    public void testOfNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((List<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((Map<?, ?>) null));
    }

    @Test
    public void testOf_BooleanList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((BooleanList) null));
    }

    @Test
    public void testOf_CharList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((CharList) null));
    }

    @Test
    public void testOf_ByteList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((ByteList) null));
    }

    @Test
    public void testOf_ShortList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((ShortList) null));
    }

    @Test
    public void testOf_IntList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((IntList) null));
    }

    @Test
    public void testOf_LongList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((LongList) null));
    }

    @Test
    public void testOf_FloatList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((FloatList) null));
    }

    @Test
    public void testOf_DoubleList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((DoubleList) null));
    }

    @Test
    public void testOf_Collection_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Collection<String>) null));
    }

    @Test
    public void testOf_Map_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Map<String, Integer>) null));
    }

    @Test
    public void testOf_Multiset_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Multiset<String>) null));
    }

    @Test
    public void testOf_Dataset_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Dataset) null));
    }

    @Test
    public void testOf_Object_Null_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Object) null));
    }

    @Test
    public void testOf_Multimap_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Multimap<String, Integer, List<Integer>>) null));
    }

    // ---- G28-005: of(Object) must return the specialized builder for all eight primitive lists.
    // PrimitiveList is not a java.util.List, so the instanceof chain in of(Object) has no branch for
    // these - only the creatorMap entries make this work.

    @Test
    public void testOf_Object_primitiveLists_returnSpecializedBuilders() {
        assertEquals(BooleanListBuilder.class, Builder.of((Object) BooleanList.of(true)).getClass());
        assertEquals(CharListBuilder.class, Builder.of((Object) CharList.of('a')).getClass());
        assertEquals(ByteListBuilder.class, Builder.of((Object) ByteList.of((byte) 1)).getClass());
        assertEquals(ShortListBuilder.class, Builder.of((Object) ShortList.of((short) 1)).getClass());
        assertEquals(IntListBuilder.class, Builder.of((Object) IntList.of(1)).getClass());
        assertEquals(LongListBuilder.class, Builder.of((Object) LongList.of(1L)).getClass());
        assertEquals(FloatListBuilder.class, Builder.of((Object) FloatList.of(1f)).getClass());
        assertEquals(DoubleListBuilder.class, Builder.of((Object) DoubleList.of(1d)).getClass());

        // and the builder really operates on the wrapped list
        final IntList il = IntList.of(1, 2, 3);
        final Builder<?> b = Builder.of((Object) il);
        ((IntListBuilder) b).add(4);
        assertEquals(IntList.of(1, 2, 3, 4), il);
    }

    // ---- G28-007: Multiset is a Collection, but of(Multiset) wins over of(Collection) ----

    @Test
    public void testOf_Multiset_returnsMultisetBuilder() {
        final Multiset<String> ms = Multiset.of("a", "a", "b");
        assertTrue(ms instanceof Collection);
        assertEquals(MultisetBuilder.class, Builder.of(ms).getClass());
        assertEquals(MultisetBuilder.class, Builder.of((Object) ms).getClass());

        // the MultisetBuilder-only API is reachable and mutates the wrapped multiset
        Builder.of(ms).setCount("b", 5);
        assertEquals(5, ms.getCount("b"));
    }
}
