package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CommonUtilCheckElementTest extends CommonUtilTestSupport {

    @Test
    public void testCheckElementIndex() {
        assertEquals(0, CommonUtil.checkElementIndex(0, 5));
        assertEquals(4, CommonUtil.checkElementIndex(4, 5));
        assertEquals(0, CommonUtil.checkElementIndex(0, 1));
        assertEquals(5, CommonUtil.checkElementIndex(5, 10));
        assertEquals(9, CommonUtil.checkElementIndex(9, 10));
        assertEquals(0, CommonUtil.checkElementIndex(0, 5, "index"));
        assertEquals(5, CommonUtil.checkElementIndex(5, 10, "myIndex"));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(5, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 5, "myIndex"));

        IndexOutOfBoundsException negative = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(-1, 10, "testIndex"));
        assertTrue(negative.getMessage().contains("testIndex (-1) must not be negative"));
        IndexOutOfBoundsException tooBig = assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkElementIndex(10, 10, "testIndex"));
        assertTrue(tooBig.getMessage().contains("testIndex (10) must be less than size (10)"));
        IllegalArgumentException negSize = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementIndex(0, -1, "testIndex"));
        assertTrue(negSize.getMessage().contains("negative size: -1"));
    }

    @Test
    public void testCheckElementNotNull() {
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new Object[] { "a", "b", "c" }));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new Object[0]));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Object[]) null));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new String[] { "a", "b" }, "myArray"));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new Object[0], "arr"));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Object[]) null, "arr"));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new Object[] { "a", null, "c" }));
        IllegalArgumentException arrMsg = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(new String[] { "a", null, "b" }));
        assertEquals("null element is found in array", arrMsg.getMessage());
        IllegalArgumentException named = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(new String[] { "a", null }, "myArray"));
        assertEquals("null element is found in myArray", named.getMessage());
        IllegalArgumentException custom = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(new String[] { null }, "Custom error for null element in array"));
        assertEquals("Custom error for null element in array", custom.getMessage());

        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Arrays.asList("a", "b", "c")));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new ArrayList<>()));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Collection<?>) null));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(new HashSet<>(Arrays.asList(1, 2, 3))));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Arrays.asList("a", "b"), "myColl"));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull(Collections.emptyList(), "myList"));
        assertDoesNotThrow(() -> CommonUtil.checkElementNotNull((Collection<?>) null, "myList"));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(Arrays.asList("a", null, "c")));
        List<String> withNull = new ArrayList<>();
        withNull.add("a");
        withNull.add(null);
        IllegalArgumentException collMsg = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(withNull));
        assertEquals("null element is found in collection", collMsg.getMessage());
        IllegalArgumentException collNamed = assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkElementNotNull(withNull, "myColl"));
        assertEquals("null element is found in myColl", collNamed.getMessage());
        IllegalArgumentException collCustom = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkElementNotNull(withNull, "Custom error for null element in collection"));
        assertEquals("Custom error for null element in collection", collCustom.getMessage());
    }
}
