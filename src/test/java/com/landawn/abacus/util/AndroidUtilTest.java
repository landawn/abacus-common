package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

public class AndroidUtilTest {

    @Test
    public void test() {
        AndroidUtil.getThreadPoolExecutor().execute(() -> System.out.print("Hello"));

    }
}
