package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractPrimitiveArrayType100Test extends TestBase {

    private Type<int[]> intArrayType;

    @BeforeEach
    public void setUp() {
        intArrayType = createType("int[]");
    }

    @Test
    public void testIsPrimitiveArray() {
        assertTrue(intArrayType.isPrimitiveArray());
    }

    @Test
    public void testDeepHashCode() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        int[] array3 = { 1, 2, 4 };

        assertEquals(intArrayType.hashCode(array1), intArrayType.deepHashCode(array1));
        assertEquals(intArrayType.hashCode(array2), intArrayType.deepHashCode(array2));
        assertNotEquals(intArrayType.deepHashCode(array1), intArrayType.deepHashCode(array3));
    }

    @Test
    public void testDeepEquals() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        int[] array3 = { 1, 2, 4 };

        assertTrue(intArrayType.deepEquals(array1, array2));
        assertFalse(intArrayType.deepEquals(array1, array3));
        assertTrue(intArrayType.deepEquals(null, null));
        assertFalse(intArrayType.deepEquals(array1, null));
    }

    @Test
    public void testToString() {
        int[] array = { 1, 2, 3 };
        String result = intArrayType.toString(array);
        assertNotNull(result);
        assertNotEquals("null", result);

        assertEquals("null", intArrayType.toString(null));
    }

    @Test
    public void testDeepToString() {
        int[] array = { 1, 2, 3 };
        String result = intArrayType.deepToString(array);
        assertEquals(intArrayType.toString(array), result);
    }
}
