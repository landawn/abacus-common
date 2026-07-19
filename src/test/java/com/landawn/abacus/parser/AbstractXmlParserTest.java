package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.util.XmlUtil;

public abstract class AbstractXmlParserTest extends AbstractParserTest {
    // TODO: AbstractXmlParser's Node deserialize delegate methods are exercised by concrete XmlParser tests; isolated coverage would require
    // a full fake XmlParser implementation because the class is package-private and has many inherited abstract parser methods.
    // TODO: AbstractXmlParser's File/InputStream/Reader/Node nodeTypes overloads are default unsupported hooks; concrete support is covered in
    // XmlParserImplTest and AbacusXmlParserImplTest, while JaxbParser intentionally does not support these overloads.

    @Test
    public void testCheckOneNodeIgnoresTextAndComments() throws Exception {
        final DocumentBuilder parser = XmlUtil.createDOMParser(false, false);
        final Document document = parser
                .parse(new InputSource(new StringReader("<wrapper>text<!-- comment --><?test instruction?><![CDATA[more text]]><child/></wrapper>")));
        final Node child = AbstractXmlParser.checkOneNode(document.getDocumentElement());

        assertEquals("child", child.getNodeName());

        final Document twoChildren = parser.parse(new InputSource(new StringReader("<wrapper><a/><b/></wrapper>")));
        assertThrows(ParsingException.class, () -> AbstractXmlParser.checkOneNode(twoChildren.getDocumentElement()));
    }
}
