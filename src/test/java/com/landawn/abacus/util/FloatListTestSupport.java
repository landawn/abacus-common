package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class FloatListTestSupport extends TestBase {

    protected FloatList list;
    protected static final float DELTA = 0.0001f;

    @BeforeEach
    public void setUp() {
        list = new FloatList();
    }
}
