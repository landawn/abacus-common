package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Group N8 (N.java 36665-41898): no behaviour changes were made; these assertions pin the
 * conditions now spelled out in the reworded {@code @throws IndexOutOfBoundsException} tags of
 * {@code forEach(T[], int, int, Consumer)} / {@code forEach(Collection, int, int, Consumer)} and the
 * unchanged validation order of the parser-facade overloads. Expected GREEN on both baseline and patched.
 */
public class ExcReviewN8Test extends com.landawn.abacus.TestBase {

    @Test
    public void forEachArrayRange_documentedIoobeConditions() {
        final String[] a = { "a", "b", "c" };
        final List<String> out = new ArrayList<>();

        // forward mode
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, -1, 2, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, 0, 4, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, -1, -1, out::add));
        // reverse mode
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, 2, -2, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, 4, 0, out::add));
        // reverse mode: fromIndex == length and toIndex == -1 are both legal
        out.clear();
        assertDoesNotThrow(() -> N.forEach(a, 3, -1, out::add));
        assertEquals(Arrays.asList("c", "b", "a"), out);
        // null array is length 0
        assertDoesNotThrow(() -> N.forEach((String[]) null, 0, 0, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach((String[]) null, 0, 1, out::add));
        // range validated before action
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(a, 0, 4, null));
        assertThrows(IllegalArgumentException.class, () -> N.forEach(a, 0, 3, null));
    }

    @Test
    public void forEachCollectionRange_documentedIoobeConditions() {
        final List<String> c = Arrays.asList("a", "b", "c");
        final List<String> out = new ArrayList<>();

        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(c, -1, 2, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(c, 0, 4, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(c, 2, -2, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(c, 4, 0, out::add));
        out.clear();
        assertDoesNotThrow(() -> N.forEach(c, 3, -1, out::add));
        assertEquals(Arrays.asList("c", "b", "a"), out);
        assertDoesNotThrow(() -> N.forEach((List<String>) null, 0, 0, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach((List<String>) null, 0, 1, out::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(c, 0, 4, null));
        assertThrows(IllegalArgumentException.class, () -> N.forEach(c, 0, 3, null));
    }

    @Test
    public void parserFacades_validationUnchanged() {
        // null targetType -> IAE (String, substring and format overloads); null source is accepted
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{}", (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{}", 0, 2, (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> N.formatJson("{}", (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> N.fromXml("<map></map>", (Class<Object>) null));
        assertEquals("", N.toJson(null));
        assertEquals("", N.formatJson(null));
        // substring overload: range checked against N.len(json), a null json is length 0
        assertThrows(IndexOutOfBoundsException.class, () -> N.fromJson((String) null, 0, 1, Object.class));
        assertThrows(IndexOutOfBoundsException.class, () -> N.fromJson("{}", 1, 3, Object.class));
        // streamJson: elementType validated first, non-array root -> UnsupportedOperationException
        assertThrows(IllegalArgumentException.class, () -> N.streamJson("[1]", (com.landawn.abacus.type.Type<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> N.streamJson("[1]", N.typeOf(Integer.class))); // unsupported element type
        assertThrows(UnsupportedOperationException.class, () -> N.streamJson("{\"a\":1}", N.typeOf(java.util.Map.class)));
    }
}
