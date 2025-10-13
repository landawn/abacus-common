package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BufferedJSONWriter100Test extends TestBase {

    @Test
    public void testStaticFields() {
        Assertions.assertTrue(BufferedJSONWriter.LENGTH_OF_REPLACEMENT_CHARS > 0);
    }
}
