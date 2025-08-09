/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import com.landawn.abacus.entity.extendDirty.basic.Account;

public class JAXBXMLParserTest extends AbstractXMLParserTest {
    @Override
    protected Parser<?, ?> getParser() {
        return jaxbXMLParser;
    }

    @Test
    public void test_0() {
        try {
            Node node = null;
            jaxbXMLParser.deserialize(node, null, Account.class);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Class<?>> nodeClasses = null;
            Node node = null;
            jaxbXMLParser.deserialize(node, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Class<?>> nodeClasses = null;
            InputStream is = null;
            jaxbXMLParser.deserialize(is, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Class<?>> nodeClasses = null;
            Reader reader = null;
            jaxbXMLParser.deserialize(reader, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }
}
