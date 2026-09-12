package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class LongListTestSupport extends TestBase {

    protected LongList list;

    @BeforeEach
    public void setUp() {
        list = new LongList();
    }
}
