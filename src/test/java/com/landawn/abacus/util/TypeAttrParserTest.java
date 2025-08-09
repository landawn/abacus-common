/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.TypeAttrParser;

public class TypeAttrParserTest extends AbstractTest {

    @Test
    public void testParser() {
        TypeAttrParser result = TypeAttrParser.parse("List<String>(,)");
        N.println(result.getClassName());
        N.println(result.getTypeParameters());
        N.println(result.getParameters());
        assertTrue(N.equals(new String[] { "," }, result.getParameters()));

        result = TypeAttrParser.parse("List(a,b)");
        N.println(result.getClassName());
        N.println(result.getTypeParameters());
        N.println(result.getParameters());

        assertTrue(N.equals(new String[] { "a", "b" }, result.getParameters()));
    }
}
