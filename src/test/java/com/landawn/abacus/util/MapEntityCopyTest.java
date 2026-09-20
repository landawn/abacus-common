package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class MapEntityCopyTest extends TestBase {
    @Test
    void copyPreservesLiteralKeysOrderAndShallowValues() {
        final MapEntity original = new MapEntity("User");
        original.props().put("User.id", 1);
        original.props().put("id", 2);
        original.props().put(null, null);
        original.props().put("", "");
        final StringBuilder mutable = new StringBuilder("\uD83D\uDE00");
        original.props().put("User.\u03B1", mutable);
        final MapEntity copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertEquals(new ArrayList<>(original.props().keySet()), new ArrayList<>(copy.props().keySet()));
        assertSame(mutable, copy.props().get("User.\u03B1"));
        copy.props().remove("id");
        assertEquals(2, original.props().get("id"));
        original.props().put("later", 3);
        assertFalse(copy.props().containsKey("later"));
    }

    @Test
    void emptyAndNormallyNormalizedEntitiesCopyIndependently() {
        final MapEntity empty = new MapEntity("User");
        assertEquals(empty, empty.copy());
        final MapEntity normal = empty.set("User.id", 1);
        assertEquals(normal, normal.copy());
        assertNotSame(normal.props(), normal.copy().props());
    }
}
