package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntList;

public class AbstractPrimitiveListType100Test extends TestBase {

    private Type<IntList> intListType;

    @BeforeEach
    public void setUp() {
        intListType = createType("IntList");
    }

    @Test
    public void testIsPrimitiveList() {
        assertTrue(intListType.isPrimitiveList());
    }
}
