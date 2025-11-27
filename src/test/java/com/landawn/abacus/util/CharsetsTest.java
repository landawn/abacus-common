package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class CharsetsTest extends AbstractTest {

    @Test
    public void test_toCharset() {
        assertEquals(Charsets.DEFAULT, Charsets.get(Charsets.DEFAULT.name()));
        assertEquals(Charsets.UTF_16, Charsets.get(Charsets.UTF_16.toString()));
    }
}
