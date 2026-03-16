package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BufferedJsonWriterTest extends TestBase {

    @Test
    public void testStaticFields() {
        Assertions.assertTrue(BufferedJsonWriter.LENGTH_OF_REPLACEMENT_CHARS > 0);
    }
}
