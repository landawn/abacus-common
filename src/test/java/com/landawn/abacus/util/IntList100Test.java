package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IntList100Test extends TestBase {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = new IntList();
    }

}
