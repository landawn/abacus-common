package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BufferedWriter100Test extends TestBase {

    @Test
    public void testClassStructure() {
        Assertions.assertTrue(BufferedWriter.class.isSealed());
    }
}
