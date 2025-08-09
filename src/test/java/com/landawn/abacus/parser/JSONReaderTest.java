/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

public class JSONReaderTest extends AbstractTest {

    @Test
    public void test_parser() throws Exception {
        String str = "{aa: 12, bb: \\\"\\u123dkeialskdfj\\n\\r\\f\\\"}";
        char[] cbuf = new char[1024];
        JSONReader reader = JSONStringReader.parse(str, cbuf);
        int token = reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();

        str = "{aa: 12, bb: \\\"\\u123dkeialskdfj\\n\\r\\f\\\"}";
        char[] rbuf = new char[32];
        cbuf = new char[1024];
        reader = JSONStreamReader.parse(IOUtil.string2Reader(str), rbuf, cbuf);
        token = reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
        N.println(token);
        N.println(reader.hasText() ? reader.getText() : "null");
        reader.nextToken();
    }
}
