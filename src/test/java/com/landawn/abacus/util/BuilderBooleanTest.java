package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Builder.BooleanListBuilder;

public class BuilderBooleanTest extends BuilderTestSupport {
    @Test
    public void testBooleanListBuilder_set_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, false));
    }

    @Test
    public void testBooleanListBuilder_add_returnsBuilder() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.add(true));
    }

    @Test
    public void testBooleanListBuilder_addAtIndex_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, false));
    }

    @Test
    public void testBooleanListBuilder_addAll_returnsBuilder() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(BooleanList.of(true)));
    }

    @Test
    public void testBooleanListBuilder_addAllAtIndex_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, BooleanList.of(false)));
    }

    @Test
    public void testBooleanListBuilder_remove_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.remove(true));
    }

    @Test
    public void testBooleanListBuilder_removeAll_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(BooleanList.of(true)));
    }

    @Test
    public void testBooleanListBuilder_set() {
        BooleanList list = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(list);
        builder.set(0, false);
        assertFalse(builder.val().get(0));
    }

    @Test
    public void testBooleanListBuilder_add() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder builder = Builder.of(list);
        builder.add(true).add(false);
        assertEquals(2, builder.val().size());
        assertTrue(builder.val().get(0));
    }

    @Test
    public void testBooleanListBuilder_addAtIndex() {
        BooleanList list = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(list);
        builder.add(1, true);
        assertEquals(3, builder.val().size());
        assertTrue(builder.val().get(1));
    }

    @Test
    public void testBooleanListBuilder_addAll() {
        BooleanList list = BooleanList.of(true);
        BooleanList toAdd = BooleanList.of(false, true);
        BooleanListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder_addAllAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        BooleanList toAdd = BooleanList.of(false);
        BooleanListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(3, builder.val().size());
        assertFalse(builder.val().get(1));
    }

    @Test
    public void testBooleanListBuilder_remove() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanListBuilder builder = Builder.of(list);
        builder.remove(false);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder_removeAll() {
        BooleanList list = BooleanList.of(true, false, true, false);
        BooleanList toRemove = BooleanList.of(false);
        BooleanListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder() {
        BooleanList bl = BooleanList.of(true);
        BooleanListBuilder builder = Builder.of(bl);
        builder.set(0, false).add(true).add(0, true).addAll(BooleanList.of(false, false));
        assertEquals(BooleanList.of(true, false, true, false, false), bl);
        builder.addAll(1, BooleanList.of(true, true));
        assertEquals(BooleanList.of(true, true, true, false, true, false, false), bl);
        builder.remove(true);
        assertEquals(BooleanList.of(true, true, false, true, false, false), bl);
        builder.removeAll(BooleanList.of(false));
        assertEquals(BooleanList.of(true, true, true), bl);
    }

    @Test
    public void testBooleanListBuilderSet() {
        BooleanList list = new BooleanList();
        list.add(true);
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.set(0, false);
        Assertions.assertFalse(list.get(0));
    }

    @Test
    public void testBooleanListBuilderAdd() {
        BooleanList list = new BooleanList();
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.add(true);
        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(list.get(0));
    }

    @Test
    public void testBooleanListBuilderAddAtIndex() {
        BooleanList list = new BooleanList();
        list.add(true);
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.add(0, false);
        Assertions.assertEquals(2, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
    }

    @Test
    public void testBooleanListBuilderAddAll() {
        BooleanList list = new BooleanList();
        BooleanList toAdd = new BooleanList();
        toAdd.add(true);
        toAdd.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);

        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.get(0));
        Assertions.assertFalse(list.get(1));
    }

    @Test
    public void testBooleanListBuilderAddAllAtIndex() {
        BooleanList list = new BooleanList();
        list.add(true);
        BooleanList toAdd = new BooleanList();
        toAdd.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.addAll(0, toAdd);

        Assertions.assertEquals(2, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
    }

    @Test
    public void testBooleanListBuilderRemove() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.remove(true);

        Assertions.assertEquals(1, list.size());
        Assertions.assertFalse(list.get(0));
    }

    @Test
    public void testBooleanListBuilderRemoveAll() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList toRemove = new BooleanList();
        toRemove.add(true);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);

        Assertions.assertEquals(1, list.size());
        Assertions.assertFalse(list.get(0));
    }

    @Test
    public void testBooleanListBuilder_of() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }
}
