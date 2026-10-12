package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Beans;

@Tag("unit")
class GenericPropertyPathReview20261003Test extends TestBase {

    @Test
    void parameterizedRootRetainsResolvedTypeWhenReadingAndWritingPaths() throws Exception {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Holder.class.getField("box").getGenericType());
        final Box<Child> box = new Box<>();

        assertEquals(Child.class, info.getPropInfo("value").clazz);
        assertEquals(2, info.getPropInfoChain("value.name").size());
        info.setPropValue(box, "value.name", "created");
        assertEquals("created", box.value.name);
        assertEquals("created", info.getPropValue(box, "value.name"));
        info.setPropValue(box, "value.name", "updated");
        assertEquals("updated", box.value.name);
    }

    @Test
    void nestedGenericBeanPathPopulatesPublicMapToBeanConversion() {
        final Holder holder = Beans.mapToBean(Map.of("box.value.name", "kept"), Holder.class);

        assertEquals("kept", holder.box.value.name);
        assertEquals("kept", Beans.getPropValue(holder, "box.value.name"));
        final Holder strict = Beans.mapToBean(Map.of("box.value.name", "strict"), false, Holder.class);
        assertEquals("strict", strict.box.value.name);
    }

    @Test
    void collectionElementRetainsItsGenericArgumentsAcrossThePath() {
        final Holder holder = Beans.mapToBean(Map.of("boxes.value.name", "first"), false, Holder.class);

        assertEquals(1, holder.boxes.size());
        assertEquals("first", holder.boxes.get(0).value.name);
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Holder.class);
        assertEquals("first", info.getPropValue(holder, "boxes.value.name"));
        info.setPropValue(holder, "boxes.value.name", "updated");
        assertEquals("updated", holder.boxes.get(0).value.name);
    }

    @Test
    void pathCachesRemainSeparateForDifferentArgumentsOfTheSameRawBean() throws Exception {
        final ParserUtil.BeanInfo childInfo = ParserUtil.getBeanInfo(Holder.class.getField("box").getGenericType());
        final ParserUtil.BeanInfo otherInfo = ParserUtil.getBeanInfo(Holder.class.getField("other").getGenericType());

        assertEquals(2, childInfo.getPropInfoChain("value.name").size());
        assertTrue(otherInfo.getPropInfoChain("value.name").isEmpty());
        assertEquals(2, otherInfo.getPropInfoChain("value.count").size());
        assertTrue(childInfo.getPropInfoChain("value.count").isEmpty());
        assertEquals(2, childInfo.getPropInfoChain("value.name").size());
    }

    @Test
    void ordinaryAndUnmatchedPathsKeepExistingBehavior() {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Holder.class);
        final Holder holder = new Holder();

        info.setPropValue(holder, "child.name", "ordinary");
        assertEquals("ordinary", info.getPropValue(holder, "child.name"));
        assertTrue(info.getPropInfoChain("child.missing").isEmpty());
        assertTrue(info.getPropInfoChain("missing").isEmpty());
        assertFalse(info.setPropValue(holder, "box.value.missing", "ignored", true));
        assertNull(holder.box);
        assertThrows(IllegalArgumentException.class, () -> info.setPropValue(holder, "child.missing", "rejected"));
    }

    public static class Child {
        public String name;
    }

    public static class OtherChild {
        public int count;
    }

    public static class Box<T> {
        public T value;
    }

    public static class Holder {
        public Box<Child> box;
        public Box<OtherChild> other;
        public List<Box<Child>> boxes;
        public Child child;
    }
}
