package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.type.Type;

@Tag("old-test")
public class JAXBXmlParserTest extends AbstractXmlParserTest {
    @Override
    protected Parser<?, ?> getParser() {
        return jaxbXmlParser;
    }

    @Test
    public void test_0() {
        try {
            Node node = null;
            jaxbXmlParser.deserialize(node, null, Account.class);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Type<?>> nodeClasses = null;
            Node node = null;
            jaxbXmlParser.deserialize(node, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Type<?>> nodeClasses = null;
            InputStream is = null;
            jaxbXmlParser.deserialize(is, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }

        try {
            Map<String, Type<?>> nodeClasses = null;
            Reader reader = null;
            jaxbXmlParser.deserialize(reader, null, nodeClasses);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }
}
