package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class StringType100Test extends TestBase {

    private StringType stringType;

    @BeforeEach
    public void setUp() {
        stringType = (StringType) createType("String");
    }

    @Test
    public void testType() {
        // StringType extends AbstractStringType which has all the implementations
        // Just verify it can be created
        assertNotNull(stringType);
    }
}
