package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.entity.PersonType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.XmlUtil;

import untrusted.abacus.UntrustedXmlType;

public abstract class AbstractXmlParserTest extends AbstractParserTest {
    private static final String UNTRUSTED_INITIALIZED_PROPERTY = "com.landawn.abacus.test.untrustedXmlTypeInitialized";

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

    @Test
    public void testTypeAttributesUseExactSafeNamesByDefault() throws Exception {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);
        final String previousInitializedValue = System.getProperty(UNTRUSTED_INITIALIZED_PROPERTY);

        try {
            System.clearProperty(property);
            System.clearProperty(UNTRUSTED_INITIALIZED_PROPERTY);

            final DocumentBuilder parser = XmlUtil.createDOMParser(false, false);
            final Node safeNode = parser.parse(new InputSource(new StringReader("<value type=\"ArrayList\"/>"))).getDocumentElement();
            assertSame(ArrayList.class, AbstractXmlParser.getAttributeTypeClass(safeNode));
            assertSame(HashSet.class, AbstractXmlParser.resolveTypeAttribute("HashSet<Object>").javaType());

            Type.of(PersonType.class);
            assertNull(AbstractXmlParser.resolveTypeAttribute(PersonType.class.getSimpleName()));
            assertNull(AbstractXmlParser.resolveTypeAttribute("List<" + PersonType.class.getSimpleName() + ">"));
            assertSame(PersonType.class, AbstractXmlParser.resolveTypeAttribute(PersonType.class.getCanonicalName()).javaType());
            assertSame(List.class, AbstractXmlParser.resolveTypeAttribute("List<" + PersonType.class.getCanonicalName() + ">").javaType());
            assertSame(ImmutableMap.class, AbstractXmlParser.resolveTypeAttribute("ImmutableMap<Object, Object>").javaType());
            assertNull(AbstractXmlParser.resolveTypeAttribute(" \t "));

            final Node blankNode = parser.parse(new InputSource(new StringReader("<value type=\"   \"/>"))).getDocumentElement();
            assertNull(AbstractXmlParser.getAttributeTypeClass(blankNode));

            final String untrustedName = "untrusted.abacus.UntrustedXmlType";
            final Node untrustedNode = parser.parse(new InputSource(new StringReader("<value type=\"" + untrustedName + "\"/>"))).getDocumentElement();
            assertThrows(ParsingException.class, () -> AbstractXmlParser.getAttributeTypeClass(untrustedNode));

            final AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "type", "type", "CDATA", untrustedName);
            assertThrows(ParsingException.class, () -> AbstractXmlParser.getAttributeTypeClass(attrs));

            final XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("<value type=\"" + untrustedName + "\"/>"));
            xmlReader.nextTag();
            assertThrows(ParsingException.class, () -> AbstractXmlParser.getAttributeTypeClass(xmlReader));
            xmlReader.close();

            assertNull(AbstractXmlParser.resolveTypeAttribute("List<" + untrustedName + ">"));
            assertNull(System.getProperty(UNTRUSTED_INITIALIZED_PROPERTY), "A rejected type attribute must not initialize its class");

            final String unregisteredAlias = "untrusted.abacus.UnregisteredXmlTypeAliasForTest";
            assertNull(AbstractXmlParser.resolveTypeAttribute(unregisteredAlias));

            final String registeredAlias = "untrusted.abacus.RegisteredXmlTypeAliasForTest";

            if (TypeFactory.getTypeIfPresent(registeredAlias) == null) {
                TypeFactory.registerType(registeredAlias, UntrustedXmlType.class, value -> value.toString(), value -> null);
            }

            assertNull(AbstractXmlParser.resolveTypeAttribute(registeredAlias));
            assertNull(AbstractXmlParser.resolveTypeAttribute("List<" + registeredAlias + ">"));
            assertNull(System.getProperty(UNTRUSTED_INITIALIZED_PROPERTY), "Rejecting a registered alias must not initialize its class");
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }

            if (previousInitializedValue == null) {
                System.clearProperty(UNTRUSTED_INITIALIZED_PROPERTY);
            } else {
                System.setProperty(UNTRUSTED_INITIALIZED_PROPERTY, previousInitializedValue);
            }
        }
    }

    @Test
    public void testLegacyTypeAttributeClassForNameCanBeEnabled() throws Exception {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);

        try {
            System.setProperty(property, "true");

            final String legacyTypeName = "untrusted.abacus.LegacyXmlType";
            final DocumentBuilder parser = XmlUtil.createDOMParser(false, false);
            final Node legacyNode = parser.parse(new InputSource(new StringReader("<value type=\"" + legacyTypeName + "\"/>"))).getDocumentElement();
            assertEquals(legacyTypeName, AbstractXmlParser.getAttributeTypeClass(legacyNode).getName());

            final AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "type", "type", "CDATA", legacyTypeName);
            assertEquals(legacyTypeName, AbstractXmlParser.getAttributeTypeClass(attrs).getName());

            final XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("<value type=\"" + legacyTypeName + "\"/>"));
            xmlReader.nextTag();
            assertEquals(legacyTypeName, AbstractXmlParser.getAttributeTypeClass(xmlReader).getName());
            xmlReader.close();
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    protected final void assertRejectedTypeAttributeDoesNotFallBackToNodeName(final XmlParser xmlParser) {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);

        try {
            System.clearProperty(property);

            final String xml = "<personType type=\"untrusted.abacus.DoesNotExist\"><id>1</id></personType>";
            final ParsingException exception = assertThrows(ParsingException.class, () -> xmlParser.deserialize(xml, Object.class));
            assertEquals("XML type attribute is not allowed: untrusted.abacus.DoesNotExist", exception.getMessage());
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }
}
