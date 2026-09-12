package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

public class BuilderTest extends BuilderTestSupport {

    @Test
    public void testOfAndVal() {
        String value = "test";
        Builder<String> builder = Builder.of(value);
        assertEquals(value, builder.val());
        assertSame(value, builder.val());
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Object) null));

        assertTrue(Builder.of(new ArrayList<String>()) instanceof ListBuilder);
        assertTrue(Builder.of(new LinkedList<String>()) instanceof ListBuilder);
        assertTrue(Builder.of(new HashSet<String>()) instanceof CollectionBuilder);
        assertTrue(Builder.of(new LinkedHashSet<String>()) instanceof CollectionBuilder);
        assertTrue(Builder.of(new HashMap<String, Integer>()) instanceof MapBuilder);
        assertTrue(Builder.of(new TreeMap<String, Integer>()) instanceof MapBuilder);
        assertTrue(Builder.of(new LinkedHashMap<String, Integer>()) instanceof MapBuilder);
        assertTrue(Builder.of(Multiset.of()) instanceof MultisetBuilder);
        assertTrue(Builder.of(CommonUtil.newListMultimap()) instanceof MultimapBuilder);
        assertTrue(Builder.of(CommonUtil.newSetMultimap()) instanceof MultimapBuilder);
        assertTrue(Builder.of(CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)))) instanceof DatasetBuilder);
        assertTrue(Builder.of(Dataset.rows(Arrays.asList("a"), Arrays.asList(Arrays.asList(1)))) instanceof DatasetBuilder);
        assertTrue(Builder.of(BooleanList.of(true)) instanceof BooleanListBuilder);
        assertTrue(Builder.of(CharList.of('a')) instanceof CharListBuilder);
        assertTrue(Builder.of(ByteList.of((byte) 1)) instanceof ByteListBuilder);
        assertTrue(Builder.of(ShortList.of((short) 1)) instanceof ShortListBuilder);
        assertTrue(Builder.of(IntList.of(1)) instanceof IntListBuilder);
        assertTrue(Builder.of(LongList.of(1L)) instanceof LongListBuilder);
        assertTrue(Builder.of(FloatList.of(1f)) instanceof FloatListBuilder);
        assertTrue(Builder.of(DoubleList.of(1d)) instanceof DoubleListBuilder);

        StringBuilder sb = new StringBuilder("test");
        Builder<StringBuilder> generic = Builder.of(sb);
        assertSame(sb, generic.val());
        assertFalse(generic instanceof ListBuilder);
        assertFalse(generic instanceof MapBuilder);
        assertFalse(generic instanceof CollectionBuilder);
    }

    @Test
    public void testAcceptApplyStream() {
        List<String> list = new ArrayList<>();
        Builder<List<String>> builder = Builder.of(list);
        assertSame(builder, builder.accept(l -> l.add("test")));
        assertEquals(List.of("test"), builder.val());
        Builder.of(list).accept(l -> l.add("a")).accept(l -> l.add("b"));
        assertTrue(list.contains("a"));
        assertEquals(4, Builder.of("test").apply(String::length));
        assertNull(Builder.of("test").apply(s -> null));
        assertEquals(1, Builder.of("test").stream().count());
        assertEquals("hello", Builder.of("hello").stream().findFirst().orElse(null));
    }

    @Test
    public void testListAndMapBuilders() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> listBuilder = Builder.of(list);
        listBuilder.add("first").addAll(Arrays.asList("second", "third")).add(1, "inserted").remove("third").accept(l -> l.add("fourth"));
        Collections.reverse(list);
        assertEquals(Arrays.asList("fourth", "second", "inserted", "first"), list);
        assertSame(listBuilder, listBuilder.addAll((Collection<String>) null).removeAll((Collection<?>) null));
        assertEquals(Arrays.asList("fourth", "second", "inserted", "first"), list);

        CollectionBuilder<String, LinkedHashSet<String>> collectionBuilder = Builder.of(new LinkedHashSet<>(List.of("apple", "banana")));
        assertSame(collectionBuilder, collectionBuilder.addAll((Collection<String>) null).removeAll((Collection<?>) null));
        assertEquals(new LinkedHashSet<>(List.of("apple", "banana")), collectionBuilder.val());

        Map<String, Integer> map = Builder.of(new HashMap<String, Integer>()).put("ab", 1).put("abc", 1).val();
        assertEquals(1, map.get("ab"));
        assertEquals(List.of(), Builder.of(new ArrayList<Long>()).val());
        assertEquals(2, Builder.of(new Multiset<String>()).add("abc").add("123").val().size());
        assertEquals(1, Builder.of(CommonUtil.newListMultimap()).put("abc", 123).val().size());
    }

    @Test
    public void testPrimitiveListBuilders() {
        CharList chars = CharList.of('a');
        Builder.of(chars)
                .set(0, 'b')
                .add('c')
                .add(0, 'd')
                .addAll(CharList.of('e', 'f'))
                .addAll(1, CharList.of('x', 'y'))
                .remove('d')
                .removeAll(CharList.of('x', 'y'));
        assertEquals(CharList.of('b', 'c', 'e', 'f'), chars);

        ByteList bytes = ByteList.of((byte) 1);
        Builder.of(bytes)
                .set(0, (byte) 2)
                .add((byte) 3)
                .add(0, (byte) 4)
                .addAll(ByteList.of((byte) 5, (byte) 6))
                .remove((byte) 2)
                .removeAll(ByteList.of((byte) 5, (byte) 6));
        assertEquals(ByteList.of((byte) 4, (byte) 3), bytes);

        ShortList shorts = ShortList.of((short) 1);
        Builder.of(shorts)
                .set(0, (short) 2)
                .add((short) 3)
                .add(0, (short) 4)
                .addAll(ShortList.of((short) 5, (short) 6))
                .remove((short) 2)
                .removeAll(ShortList.of((short) 5, (short) 6));
        assertEquals(ShortList.of((short) 4, (short) 3), shorts);

        IntList ints = IntList.of(1);
        IntListBuilder intBuilder = Builder.of(ints);
        assertSame(intBuilder, intBuilder.set(0, 2));
        intBuilder.add(3).add(0, 4).addAll(IntList.of(5, 6)).remove(2).removeAll(IntList.of(5, 6));
        assertEquals(IntList.of(4, 3), ints);
        assertEquals(IntList.of(3, 5), Builder.of(IntList.of(1, 2, 3)).add(1).remove(2).add(5).removeAll(IntList.of(1)).val());

        LongList longs = LongList.of(1L);
        Builder.of(longs).set(0, 2L).add(3L).add(0, 4L).addAll(LongList.of(5L, 6L)).remove(2L).removeAll(LongList.of(5L, 6L));
        assertEquals(LongList.of(4L, 3L), longs);

        FloatList floats = FloatList.of(1.0f);
        Builder.of(floats).set(0, 2.0f).add(3.0f).add(0, 4.0f).addAll(FloatList.of(5.0f, 6.0f)).remove(2.0f).removeAll(FloatList.of(5.0f, 6.0f));
        assertEquals(FloatList.of(4.0f, 3.0f), floats);

        DoubleList doubles = DoubleList.of(1.0);
        Builder.of(doubles).set(0, 2.0).add(3.0).add(0, 4.0).addAll(DoubleList.of(5.0, 6.0)).remove(2.0).removeAll(DoubleList.of(5.0, 6.0));
        assertEquals(DoubleList.of(4.0, 3.0), doubles);
    }

    @Test
    public void reviewFixes20260906_datasetBuilderPropagatesDelegateArgumentValidation() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).renameColumn("nope", "x"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).renameColumn("name", "age"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).renameColumns(CommonUtil.asMap("nope", "x")));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).renameColumns(CommonUtil.asMap("name", "age")));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).addColumn("name", Arrays.asList(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).addColumn("x", Arrays.asList(1)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).addColumn(0, "name", Arrays.asList(1, 2)));
        assertThrows(IndexOutOfBoundsException.class, () -> Builder.of(reviewFixes20260906Dataset()).addColumn(9, "x", Arrays.asList(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).removeColumn("nope"));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).removeColumns(Arrays.asList("nope")));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).convertColumn("nope", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).convertColumns(CommonUtil.asMap("nope", Integer.class)));

        final Dataset otherColumns = Dataset.rows(Arrays.asList("x", "y"), new Object[][] { { 1, 2 } });
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).prepend(otherColumns));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).append(otherColumns));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).prepend(null));
        assertThrows(IllegalArgumentException.class, () -> Builder.of(reviewFixes20260906Dataset()).append(null));

        final Dataset frozen = reviewFixes20260906Dataset();
        frozen.freeze();
        assertThrows(IllegalStateException.class, () -> Builder.of(frozen).renameColumn("name", "n"));

        assertEquals(Arrays.asList("n", "age"), Builder.of(reviewFixes20260906Dataset()).renameColumn("name", "n").val().columnNames());
        assertEquals(Arrays.asList("name", "age", "x"), Builder.of(reviewFixes20260906Dataset()).addColumn("x", Arrays.asList(1, 2)).val().columnNames());
        assertEquals(Arrays.asList("name"), Builder.of(reviewFixes20260906Dataset()).removeColumn("age").val().columnNames());
    }
}
