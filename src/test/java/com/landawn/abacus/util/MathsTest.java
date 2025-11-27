package com.landawn.abacus.util;

import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class MathsTest {

    @Test
    public void test_log() {
        N.println(Numbers.log2(10));
        N.println(Numbers.log10(10));

        N.println(Math.log(10));
        N.println(Math.log10(10));

        N.println(Math.log(2));
        N.println(Math.log10(2));
    }

    @Test
    public void test_saturatedAdd() {
        N.println(Numbers.saturatedAdd(Long.MAX_VALUE, Long.MAX_VALUE));
        N.println(Long.MAX_VALUE);
        N.println(Numbers.saturatedAdd(Long.MIN_VALUE, Long.MIN_VALUE));
        N.println(Long.MIN_VALUE);

        N.println(Numbers.saturatedAdd(Long.MIN_VALUE, Long.MAX_VALUE));
        N.println(Long.MIN_VALUE + Long.MAX_VALUE);

        N.println(Math.pow(2, 2));
        N.println(Numbers.pow(2, 2));

        N.println(Math.sqrt(2));
        N.println(Numbers.sqrt(9, RoundingMode.UNNECESSARY));
        N.println(Numbers.sqrt(2, RoundingMode.DOWN));
    }
}
