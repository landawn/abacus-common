package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.XmlUtil;

import testfixtures.types.UntrustedXmlType;

public abstract class AbstractXmlParserTest extends AbstractParserTest {
    // Explicit policy remains independent of global registrations; ordinary factories retain round-trip compatibility.
    private final AbstractXmlParser resolver = new XmlParserImpl(XmlParserType.StAX, null, null, java.util.Set.of());
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
    public void testExplicitTypePolicyUsesExactSafeNames() throws Exception {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);
        final String previousInitializedValue = System.getProperty(UNTRUSTED_INITIALIZED_PROPERTY);

        try {
            System.clearProperty(property);
            System.clearProperty(UNTRUSTED_INITIALIZED_PROPERTY);

            final DocumentBuilder parser = XmlUtil.createDOMParser(false, false);
            final Node safeNode = parser.parse(new InputSource(new StringReader("<value type=\"ArrayList\"/>"))).getDocumentElement();
            assertSame(ArrayList.class, resolver.getAttributeTypeClass(safeNode));
            assertSame(HashSet.class, resolver.resolveTypeAttribute("HashSet<Object>").javaType());

            Type.of(PersonType.class);
            assertNull(resolver.resolveTypeAttribute(PersonType.class.getSimpleName()));
            assertNull(resolver.resolveTypeAttribute("List<" + PersonType.class.getSimpleName() + ">"));
            assertNull(resolver.resolveTypeAttribute(PersonType.class.getCanonicalName()));
            assertNull(resolver.resolveTypeAttribute("List<" + PersonType.class.getCanonicalName() + ">"));
            assertSame(ImmutableMap.class, resolver.resolveTypeAttribute("ImmutableMap<Object, Object>").javaType());
            assertNull(resolver.resolveTypeAttribute(" \t "));

            final Node blankNode = parser.parse(new InputSource(new StringReader("<value type=\"   \"/>"))).getDocumentElement();
            assertNull(resolver.getAttributeTypeClass(blankNode));

            final String untrustedName = "untrusted.abacus.UntrustedXmlType";
            final Node untrustedNode = parser.parse(new InputSource(new StringReader("<value type=\"" + untrustedName + "\"/>"))).getDocumentElement();
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(untrustedNode));

            final AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "type", "type", "CDATA", untrustedName);
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(attrs));

            final XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("<value type=\"" + untrustedName + "\"/>"));
            xmlReader.nextTag();
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(xmlReader));
            xmlReader.close();

            assertNull(resolver.resolveTypeAttribute("List<" + untrustedName + ">"));
            assertNull(System.getProperty(UNTRUSTED_INITIALIZED_PROPERTY), "A rejected type attribute must not initialize its class");

            final String unregisteredAlias = "untrusted.abacus.UnregisteredXmlTypeAliasForTest";
            assertNull(resolver.resolveTypeAttribute(unregisteredAlias));

            final String registeredAlias = "untrusted.abacus.RegisteredXmlTypeAliasForTest";

            if (TypeFactory.getTypeIfPresent(registeredAlias) == null) {
                TypeFactory.registerType(registeredAlias, UntrustedXmlType.class, value -> value.toString(), value -> null);
            }

            assertNull(resolver.resolveTypeAttribute(registeredAlias));
            assertNull(resolver.resolveTypeAttribute("List<" + registeredAlias + ">"));
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
    public void testLegacySystemPropertyCannotGrantTypePermission() throws Exception {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);

        try {
            System.setProperty(property, "true");

            final String legacyTypeName = "testfixtures.types.LegacyXmlType";
            final DocumentBuilder parser = XmlUtil.createDOMParser(false, false);
            final Node legacyNode = parser.parse(new InputSource(new StringReader("<value type=\"" + legacyTypeName + "\"/>"))).getDocumentElement();
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(legacyNode));

            final AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "type", "type", "CDATA", legacyTypeName);
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(attrs));

            final XMLStreamReader xmlReader = XMLInputFactory.newFactory().createXMLStreamReader(new StringReader("<value type=\"" + legacyTypeName + "\"/>"));
            xmlReader.nextTag();
            assertThrows(ParsingException.class, () -> resolver.getAttributeTypeClass(xmlReader));
            xmlReader.close();
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    /**
     * G11-9 (2026-09-08): the writers emit {@link com.landawn.abacus.type.Type#xmlName()}, which is
     * {@code Type.name()}, and many built-ins register under the simple name of their class. Accepting only canonical
     * class names therefore rejected this parser's own output; the rule now also accepts a registered type's own
     * simple name, while a registration alias - which is also its type's {@code name()} - stays rejected.
     */
    @Test
    public void testWriterEmittedIntrinsicTypeNamesAreAccepted() {
        final String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        final String previousValue = System.getProperty(property);

        try {
            System.clearProperty(property);

            assertEquals("MapEntity", Type.of(MapEntity.class).xmlName());
            assertSame(MapEntity.class, resolver.resolveTypeAttribute(Type.of(MapEntity.class).xmlName()).javaType());
            assertSame(MapEntity.class, resolver.resolveTypeAttribute(MapEntity.class.getCanonicalName()).javaType());
            assertSame(MutableInt.class, resolver.resolveTypeAttribute(Type.of(MutableInt.class).xmlName()).javaType());
            assertSame(Dataset.class, resolver.resolveTypeAttribute("Dataset").javaType());
            assertSame(List.class, resolver.resolveTypeAttribute("List<MutableInt>").javaType());

            final Node mapEntityNode = XmlUtil.createDOMParser(false, false)
                    .parse(new InputSource(new StringReader("<value type=\"MapEntity\"/>")))
                    .getDocumentElement();
            assertSame(MapEntity.class, resolver.getAttributeTypeClass(mapEntityNode));

            // A registration alias is its OWN Type.name(), so the name() test alone would have readmitted it. Only a
            // name the class itself supplies - its canonical or simple name - is accepted.
            final String alias = "untrusted.abacus.IntrinsicNameAliasForTest";

            if (TypeFactory.getTypeIfPresent(alias) == null) {
                TypeFactory.registerType(alias, UntrustedXmlType.class, String::valueOf, value -> null);
            }

            assertEquals(alias, TypeFactory.getTypeIfPresent(alias).name());
            assertNull(resolver.resolveTypeAttribute(alias));
            assertNull(resolver.resolveTypeAttribute("List<" + alias + ">"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    /**
     * G11-51 (2026-09-08): a processing instruction is not character data, so the text-coalescing loop every StAX
     * backend runs stopped at it and dropped everything before it. The filter that hides it lives on the base stream
     * reader factories, so both StAX backends - and all three source shapes - coalesce {@code a<?pi x?>b} to "ab".
     */
    @Test
    public void testStreamReaderCoalescesTextAcrossAProcessingInstruction() {
        final String xml = "<piTextBean><raw>a<?pi x?>b</raw></piTextBean>";

        for (final XmlParserType parserType : XmlParserType.values()) {
            assertEquals("ab", new AbacusXmlParserImpl(parserType).deserialize(xml, null, PiTextBean.class).getRaw(), "AbacusXmlParserImpl " + parserType);
        }

        final AbacusXmlParserImpl staxParser = new AbacusXmlParserImpl(XmlParserType.StAX);
        assertEquals("ab", staxParser.deserialize(new StringReader(xml), null, PiTextBean.class).getRaw());
        assertEquals("ab", staxParser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), null, PiTextBean.class).getRaw());

        // A comment in the same position was already coalesced, and the two StAX backends must not disagree.
        assertEquals("ab", staxParser.deserialize("<piTextBean><raw>a<!--c-->b</raw></piTextBean>", null, PiTextBean.class).getRaw());
        assertEquals("ab", new XmlParserImpl(XmlParserType.StAX).deserialize(xml, null, PiTextBean.class).getRaw());
        assertEquals("ab", new XmlParserImpl(XmlParserType.DOM).deserialize(xml, null, PiTextBean.class).getRaw());
    }

    public static class PiTextBean {
        private String raw;

        public String getRaw() {
            return raw;
        }

        public void setRaw(final String raw) {
            this.raw = raw;
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
