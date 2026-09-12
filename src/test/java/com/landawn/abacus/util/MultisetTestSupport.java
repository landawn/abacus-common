package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

public abstract class MultisetTestSupport extends AbstractTest {

    protected Multiset<String> multiset;
    protected Multiset<Integer> intMultiset;

    @BeforeEach
    public void setUp() {
        multiset = new Multiset<>();
        intMultiset = new Multiset<>();
    }

    // Additional tests for missing coverage

    // -------- 2026-09-06 review fixes --------
}
