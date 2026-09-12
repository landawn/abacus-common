package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class XmlDomDepthTest {
    @Test
    void allThreeHelpersHandleDeepFiniteDocuments() {
        final var document = XmlUtil.createDOMParser().newDocument();
        final var root = document.createElement("root");
        document.appendChild(root);
        var cursor = root;
        for (int i = 0; i < 10000; i++) {
            final var child = document.createElement("n");
            cursor.appendChild(child);
            cursor = child;
        }
        final var leaf = document.createElement("leaf");
        cursor.appendChild(leaf);
        leaf.setTextContent("\uD83D\uDE00");
        assertEquals(List.of(leaf), XmlUtil.getNodesByName(root, "leaf"));
        assertSame(leaf, XmlUtil.getNextNodeByName(root, "leaf"));
        assertEquals("\uD83D\uDE00", XmlUtil.readElement(root).get("root" + ".n".repeat(10000) + ".leaf"));
    }

    @Test
    void traversalOrderAndDuplicatePathPoliciesRemainDistinct() {
        final var document = XmlUtil.createDOMParser().newDocument();
        final var root = document.createElement("root");
        root.setAttribute("id", "root-id");
        document.appendChild(root);
        final var branch = document.createElement("branch");
        final var deep = document.createElement("goal");
        deep.setTextContent("deep");
        branch.appendChild(deep);
        root.appendChild(branch);
        final var direct = document.createElement("goal");
        direct.setAttribute("id", "child-id");
        direct.setTextContent("first");
        root.appendChild(direct);
        final var repeated = document.createElement("goal");
        repeated.setTextContent("last");
        root.appendChild(repeated);
        assertEquals(List.of(deep, direct, repeated), XmlUtil.getNodesByName(root, "goal"));
        assertSame(direct, XmlUtil.getNextNodeByName(root, "goal"));
        assertSame(root, XmlUtil.getNextNodeByName(root, "root"));
        assertNull(XmlUtil.getNextNodeByName(root, "missing"));
        final var values = XmlUtil.readElement(root);
        assertEquals("root-id", values.get("id"));
        assertEquals("child-id", values.get("root.goal.id"));
        assertEquals("last", values.get("root.goal"));
        assertEquals("deep", values.get("root.branch.goal"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.getNodesByName(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.getNextNodeByName(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.readElement(null));
    }
}
