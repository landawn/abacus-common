package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

class TryTest {

    @Test
    void test_01() {
        Try.run(() -> N.println("aa"), Exception::printStackTrace);
    }

}
