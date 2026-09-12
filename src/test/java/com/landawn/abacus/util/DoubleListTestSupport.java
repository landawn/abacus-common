package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class DoubleListTestSupport extends TestBase {

    protected DoubleList list;
    protected static final double DELTA = 0.000001;

    @BeforeEach
    public void setUp() {
        list = new DoubleList();
    }

    // --- Gap coverage tests ---

    // --- Missing dedicated tests ---

    // --- regression tests for 2026-06-10 deep-review fixes ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
