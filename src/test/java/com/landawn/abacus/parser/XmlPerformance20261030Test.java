package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MapEntity;

@Tag("unit")
public class XmlPerformance20261030Test {

    @Test
    public void deepSaxResultsStillParseButTheirHandlersAreNotPooled() throws Exception {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(XmlParserType.SAX);
        final AbacusXmlParserImpl.XmlSAXHandler<?> handler = borrowHandler(parser);
        final int depth = 2_048;
        try {
            parseSax(handler, "<chain>" + "<child><chain>".repeat(depth) + "</chain></child>".repeat(depth) + "</chain>");
            Chain value = (Chain) ((Holder<?>) field(handler, "resultHolder")).value();
            int actualDepth = 0;
            while (value.getChild() != null) {
                actualDepth++;
                value = value.getChild();
            }
            assertEquals(depth, actualDepth);
        } finally {
            recycleHandler(handler);
        }
        assertFalse(handlerPool().contains(handler), "An outlier's empty traversal arrays must not remain strongly pooled");
        assertReset(handler);
        assertNull(parser.deserialize("<chain/>", Chain.class).getChild());
    }

    @Test
    public void failedDeepSaxParseAlsoDiscardsAndResetsItsHandler() throws Exception {
        final AbacusXmlParserImpl.XmlSAXHandler<?> handler = borrowHandler(new AbacusXmlParserImpl(XmlParserType.SAX));
        try {
            assertThrows(SAXException.class, () -> parseSax(handler, "<chain>" + "<child><chain>".repeat(512)));
        } finally {
            recycleHandler(handler);
        }
        assertFalse(handlerPool().contains(handler));
        assertReset(handler);
    }

    @Test
    public void ordinarySaxHandlersRemainReusableAfterSuccessAndFailure() throws Exception {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(XmlParserType.SAX);
        for (final boolean malformed : new boolean[] { false, true }) {
            final AbacusXmlParserImpl.XmlSAXHandler<?> handler = borrowHandler(parser);
            try {
                if (malformed) {
                    assertThrows(SAXException.class, () -> parseSax(handler, "<chain><child>"));
                } else {
                    parseSax(handler, "<chain><child><chain/></child></chain>");
                }
            } finally {
                recycleHandler(handler);
            }
            assertTrue(handlerPool().contains(handler), "Normal-sized handlers should still be pooled");
            assertReset(handler);
        }
        assertNull(parser.deserialize("<chain/>", Chain.class).getChild());
    }

    @Test
    public void fragmentedPropertiesUseCharacterRangesAndPreserveTheirTextAndPosition() throws Exception {
        for (final AbstractXmlParser parser : streamParsers()) {
            final String xml = "<payload><text> a<![CDATA[ b ]]>&amp;<![CDATA[\uD83D\uDE00]]> </text><after>end</after></payload>";
            final CountingReader reader = reader(xml);
            try {
                final Payload value = read(parser, reader, Type.of(Payload.class));
                assertEquals(" a b &\uD83D\uDE00 ", value.getText());
                assertEquals("end", value.getAfter());
                assertEquals(2, reader.textCalls, "Only the first event of each property should need a String");
                assertTrue(reader.characterCalls >= 3);
                assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
                assertEquals("payload", reader.getLocalName());
            } finally {
                reader.close();
            }
        }
    }

    @Test
    public void singleEventPropertyKeepsTheStringFastPath() throws Exception {
        for (final AbstractXmlParser parser : streamParsers()) {
            final CountingReader reader = reader("<payload><text> unchanged </text></payload>");
            try {
                assertEquals(" unchanged ", read(parser, reader, Type.of(Payload.class)).getText());
                assertEquals(1, reader.textCalls);
                assertEquals(0, reader.characterCalls);
            } finally {
                reader.close();
            }
        }
    }

    @Test
    public void fragmentedContainerAndElementPathsPreserveValues() throws Exception {
        for (final AbstractXmlParser parser : streamParsers()) {
            assertArrayEquals(new String[] { "a", "b" }, readFragmented(parser, "<array>[\"a\",<![CDATA[\"b\"]]>]</array>", Type.of(String[].class), 1));
            assertEquals(List.of("a", "b"), readFragmented(parser, "<list>[\"a\",<![CDATA[\"b\"]]>]</list>", Type.of("List<String>"), 1));
            assertEquals(List.of(" a b "), readFragmented(parser, "<list><e> a<![CDATA[ b ]]></e></list>", Type.of("List<String>"), 1));
            final String xml = parser instanceof XmlParserImpl ? "<map><key>a<![CDATA[b]]></key></map>"
                    : "<map><entry><key>ke<![CDATA[y]]></key><value>a<![CDATA[b]]></value></entry></map>";
            assertEquals(Map.of("key", "ab"), readFragmented(parser, xml, Type.of("Map<String,String>"), parser instanceof XmlParserImpl ? 1 : 2));
        }
        final MapEntity entity = readFragmented(new XmlParserImpl(XmlParserType.StAX), "<entity><key>a<![CDATA[b]]></key></entity>", Type.of(MapEntity.class),
                1);
        assertEquals("ab", entity.get("key"));
    }

    @Test
    public void fragmentedWhitespaceAndMixedContentKeepExistingRouting() throws Exception {
        for (final AbstractXmlParser parser : streamParsers()) {
            final String xml = "<payload><child> <![CDATA[ \n ]]><payload><text>x</text></payload></child><after>end</after></payload>";
            final CountingReader reader = reader(xml);
            try {
                final Payload value = read(parser, reader, Type.of(Payload.class));
                assertEquals("x", value.getChild().getText());
                assertEquals("end", value.getAfter());
            } finally {
                reader.close();
            }
            final CountingReader mixed = reader("<payload><text>a<![CDATA[b]]><nested/></text></payload>");
            try {
                final ParsingException exception = assertThrows(ParsingException.class, () -> read(parser, mixed, Type.of(Payload.class)));
                assertEquals("Mixed scalar and nested XML values are not supported", exception.getMessage());
            } finally {
                mixed.close();
            }
        }
    }

    private static AbstractXmlParser[] streamParsers() {
        return new AbstractXmlParser[] { new XmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.StAX) };
    }

    private static <T> T readFragmented(final AbstractXmlParser parser, final String xml, final Type<T> type, final int expectedTextCalls) throws Exception {
        final CountingReader reader = reader(xml);
        try {
            final T result = read(parser, reader, type);
            assertEquals(expectedTextCalls, reader.textCalls);
            assertTrue(reader.characterCalls > 0);
            assertEquals(XMLStreamConstants.END_ELEMENT, reader.getEventType());
            return result;
        } finally {
            reader.close();
        }
    }

    private static <T> T read(final AbstractXmlParser parser, final XMLStreamReader reader, final Type<T> type) throws XMLStreamException {
        return parser instanceof XmlParserImpl standard ? standard.readByStreamParser(reader, new XmlDeserConfig(), type)
                : ((AbacusXmlParserImpl) parser).readByStreamParser(reader, new XmlDeserConfig(), type);
    }

    private static CountingReader reader(final String xml) throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_COALESCING, false);
        final CountingReader reader = new CountingReader(factory.createXMLStreamReader(new StringReader(xml)));
        AbstractXmlParser.moveToRootElement(reader);
        return reader;
    }

    // A provider may expose a nonzero slice of a shared buffer and overwrite it on next().
    private static final class CountingReader extends StreamReaderDelegate {
        private static final int OFFSET = 7;
        private char[] characters = new char[32];
        private int textCalls;
        private int characterCalls;

        private CountingReader(final XMLStreamReader reader) {
            super(reader);
        }

        @Override
        public String getText() {
            textCalls++;
            return super.getText();
        }

        @Override
        public char[] getTextCharacters() {
            characterCalls++;
            final int length = super.getTextLength();
            if (characters.length < length + OFFSET + 1) {
                characters = new char[length + OFFSET + 1];
            }
            Arrays.fill(characters, '#');
            System.arraycopy(super.getTextCharacters(), super.getTextStart(), characters, OFFSET, length);
            return characters;
        }

        @Override
        public int getTextStart() {
            return OFFSET;
        }

        @Override
        public int next() throws XMLStreamException {
            Arrays.fill(characters, '?');
            return super.next();
        }
    }

    private static AbacusXmlParserImpl.XmlSAXHandler<?> borrowHandler(final AbacusXmlParserImpl parser) throws Exception {
        final Method method = AbacusXmlParserImpl.class.getDeclaredMethod("getXmlSAXHandler", XmlDeserConfig.class, Map.class, Type.class);
        method.setAccessible(true);
        return (AbacusXmlParserImpl.XmlSAXHandler<?>) method.invoke(parser, new XmlDeserConfig(), null, Type.of(Chain.class));
    }

    private static void recycleHandler(final AbacusXmlParserImpl.XmlSAXHandler<?> handler) throws Exception {
        final Method method = AbacusXmlParserImpl.class.getDeclaredMethod("recycle", AbacusXmlParserImpl.XmlSAXHandler.class);
        method.setAccessible(true);
        method.invoke(null, handler);
    }

    private static Queue<?> handlerPool() throws Exception {
        final Field field = AbacusXmlParserImpl.class.getDeclaredField("xmlSAXHandlerPool");
        field.setAccessible(true);
        return (Queue<?>) field.get(null);
    }

    private static void parseSax(final AbacusXmlParserImpl.XmlSAXHandler<?> handler, final String xml) throws Exception {
        // Exercise handler retention beyond 256 frames independently of the shared parser's security limits.
        // Keep the larger limit local to this test parser; the deepest fixture has 4,097 elements.
        final SAXParserFactory factory = SAXParserFactory.newDefaultInstance();
        factory.setNamespaceAware(true);
        final SAXParser parser = factory.newSAXParser();
        parser.setProperty("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", "8192");
        parser.parse(new InputSource(new StringReader(xml)), handler);
    }

    private static Object field(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void assertReset(final AbacusXmlParserImpl.XmlSAXHandler<?> handler) throws Exception {
        for (final String name : new String[] { "xmlParser", "inputType", "nodeTypes", "config", "bean", "beanInfo", "sb" }) {
            assertNull(field(handler, name), name);
        }
        assertNull(((Holder<?>) field(handler, "resultHolder")).value());
        for (final String name : new String[] { "nodeTypeQueue", "nodeValueQueue", "optionalWrapperQueue", "keyQueue" }) {
            assertTrue(((List<?>) field(handler, name)).isEmpty(), name);
        }
        assertTrue(((Map<?, ?>) field(handler, "beanInfoQueue")).isEmpty());
    }

    public static class Chain {
        private Chain child;

        public Chain getChild() {
            return child;
        }

        public void setChild(final Chain child) {
            this.child = child;
        }
    }

    public static class Payload {
        private String text;
        private String after;
        private Payload child;

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(final String after) {
            this.after = after;
        }

        public Payload getChild() {
            return child;
        }

        public void setChild(final Payload child) {
            this.child = child;
        }
    }
}
