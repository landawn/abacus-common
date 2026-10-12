package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class BeansFieldHeadReview20261003Test extends TestBase {
    @Test
    void fieldOnlyHeadWinsOverOuterClassQualifiedAlias() {
        final Node value = new Node();
        assertNull(Beans.getPropGetter(Node.class, "node.name"));
        assertNull(Beans.getPropSetter(Node.class, "node.name"));
        assertNull(Beans.getPropField(Node.class, "node.name"));
        assertEquals("inner", Beans.getPropValue(value, "node.name"));
        assertEquals("inner", Beans.getPropValueIfPresent(value, "node.name").get());
    }

    @Test
    void nestedWritesAndUnreachableValuesUseTheFieldHead() {
        final Node value = Beans.mapToBean(Map.of("node.name", "updated"), false, Node.class);
        assertEquals("outer", value.getName());
        assertEquals("updated", value.node.name);
        value.node = null;
        assertNull(Beans.getPropValue(value, "node.name"));
        assertFalse(Beans.getPropValueIfPresent(value, "node.name").isPresent());
    }

    @Test
    void nonBeanQualifierStillResolvesTheOuterAlias() {
        final Scalar scalar = new Scalar();
        assertNotNull(Beans.getPropGetter(Scalar.class, "scalar.name"));
        assertEquals("outer", Beans.getPropValue(scalar, "scalar.name"));
    }

    public static class Inner {
        public String name = "inner";
    }

    public static class Node {
        public Inner node = new Inner();
        private String name = "outer";

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }
    }

    public static class Scalar {
        public String scalar = "plain";
        private String name = "outer";

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }
    }
}
