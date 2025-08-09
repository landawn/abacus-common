package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.KryoParser;
import com.landawn.abacus.parser.ParserFactory;

public class KryoTest {
    static final KryoParser kryoParser = ParserFactory.createKryoParser();

    @Test
    public void test_01() {
        N.println(kryoParser.clone(Pair.of("abc", 123)));
    }

}
