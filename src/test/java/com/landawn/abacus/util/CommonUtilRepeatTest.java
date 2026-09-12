package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class CommonUtilRepeatTest extends CommonUtilTestSupport {

    @Test
    public void testRepeat() {
        assertEquals(Arrays.asList("a", "a", "a"), CommonUtil.repeat("a", 3));
        assertEquals(Arrays.asList('a', 'a', 'a'), CommonUtil.repeat('a', 3));
        assertEquals(5, CommonUtil.repeat("x", 5).size());
        assertTrue(CommonUtil.repeat("x", 0).isEmpty());
        assertEquals(Arrays.asList(null, null, null), CommonUtil.repeat(null, 3));
    }

    @Test
    public void testCycle() {
        assertEquals(Arrays.asList("a", "b", "a", "b", "a", "b"), CommonUtil.cycle(Arrays.asList("a", "b"), 3));
        assertEquals(Arrays.asList("a", "b", "a", "b", "a"), CommonUtil.cycleToSize(Arrays.asList("a", "b"), 5));
        assertEquals(Arrays.asList("a", "b", "c", "a", "b", "c"), CommonUtil.cycleToSize(Arrays.asList("a", "b", "c"), 6));
        assertTrue(CommonUtil.cycle(Arrays.asList("a", "b", "c"), 0).isEmpty());
        assertTrue(CommonUtil.cycle(new ArrayList<>(), 3).isEmpty());
        assertTrue(CommonUtil.cycleToSize(Arrays.asList("a", "b", "c"), 0).isEmpty());
    }

    @Test
    public void testRepeatElements() {
        assertEquals(Arrays.asList("a", "a", "a", "b", "b", "b"), CommonUtil.repeatElements(Arrays.asList("a", "b"), 3));
        assertTrue(CommonUtil.repeatElements(Arrays.asList("a", "b", "c"), 0).isEmpty());
        assertTrue(CommonUtil.repeatElements(new ArrayList<>(), 3).isEmpty());

        assertEquals(Arrays.asList("a", "a", "a", "b", "b"), CommonUtil.repeatElementsToSize(Arrays.asList("a", "b"), 5));
        assertEquals(Arrays.asList("a", "a", "b", "b", "c", "c"), CommonUtil.repeatElementsToSize(Arrays.asList("a", "b", "c"), 6));
        assertEquals(Arrays.asList(1, 1, 2, 2, 3), CommonUtil.repeatElementsToSize(Arrays.asList(1, 2, 3), 5));
        assertEquals(Arrays.asList(1, 1, 1, 2, 2, 2), CommonUtil.repeatElementsToSize(Arrays.asList(1, 2), 6));
        assertTrue(CommonUtil.repeatElementsToSize(Arrays.asList("a", "b", "c"), 0).isEmpty());
        assertEquals(0, CommonUtil.repeatElementsToSize(Arrays.asList(1, 2, 3), 0).size());
    }
}
