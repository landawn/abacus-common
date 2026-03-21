package com.landawn.abacus.util;

import com.landawn.abacus.TestBase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;

public class KryoTest extends TestBase {
    static final KryoParser kryoParser = ParserFactory.createKryoParser();

    @Test
    public void test_01() {
        assertDoesNotThrow(() -> {
            N.println(kryoParser.deepCopy(Pair.of("abc", 123)));
        });
    }

}
