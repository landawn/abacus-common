package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class IndexTest {

    @Test
    public void test_indexOfSubArray() {
        final int[] a = { 1, 2, 3, 1, 2, 2, 3, 1, 2, 2, 3 };
        int[] b = { 1, 2, 2, 3 };
        N.println(Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(3, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));

        N.println(Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(7, Index.lastOfSubArray(a, 100, b, 0, b.length).orElse(-1));

        b = new int[] { 1, 2, 2, 2, 3 };
        N.println(Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(-1, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubArray(a, 1, b, 2, 3).orElse(-1));
        assertEquals(-1, Index.ofSubList(CommonUtil.toList(a), 0, CommonUtil.toList(b), 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubList(CommonUtil.toList(a), 1, CommonUtil.toList(b), 2, 3).orElse(-1));

        N.println(Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(-1, Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 1, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 2, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 3, b, 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubArray(a, a.length - 4, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubList(CommonUtil.toList(a), a.length - 1, CommonUtil.toList(b), 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubList(CommonUtil.toList(a), a.length - 4, CommonUtil.toList(b), 2, 3).orElse(-1));

        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 1).orElse(-1));
        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 2).orElse(-1));
        assertEquals(4, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 4).orElse(-1));
    }

}
