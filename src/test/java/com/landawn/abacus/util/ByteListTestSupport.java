package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class ByteListTestSupport extends TestBase {

    protected ByteList list;

    @BeforeEach
    public void setUp() {
        list = new ByteList();
    }

    // ---- newly added tests for uncovered methods ----

    // --- Tests for inherited PrimitiveList methods ---

    // --- Missing dedicated tests ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
