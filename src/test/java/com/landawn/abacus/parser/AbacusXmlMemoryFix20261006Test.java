package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;

@Tag("unit")
class AbacusXmlMemoryFix20261006Test extends TestBase {
    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void compactCustomCollectionsPreserveConstructionIdentityAndBulkCopyHooks(final XmlParserType mode) {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(mode);
        final Type<CountingList> type = Type.of(CountingList.class);
        CountingList.constructors = 0;
        CountingList.bulkCopies = 0;
        final CountingList result = parser.deserialize("<list>[\"a\",\"b\",\"c\"]</list>", type);
        assertEquals(List.of("a", "b", "c"), result);
        assertEquals(mode == XmlParserType.StAX ? 0 : 1, CountingList.bulkCopies);
        assertEquals(2, CountingList.constructors);
        assertEquals(mode == XmlParserType.StAX ? 2 : 1, result.constructionId);
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void compactCustomMapsPreserveConstructionIdentityAndBulkCopyHooks(final XmlParserType mode) {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(mode);
        final Type<CountingMap> type = Type.of(CountingMap.class);
        CountingMap.constructors = 0;
        CountingMap.bulkCopies = 0;
        final CountingMap result = parser.deserialize("<map>{\"a\":\"b\",\"c\":\"d\"}</map>", type);
        assertEquals(Map.of("a", "b", "c", "d"), result);
        assertEquals(mode == XmlParserType.SAX ? 1 : 0, CountingMap.bulkCopies);
        assertEquals(2, CountingMap.constructors);
        assertEquals(mode == XmlParserType.SAX ? 1 : 2, result.constructionId);
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void nestedCompactResultsReplaceTheCorrectParentSlotAndRetainImmutableTargets(final XmlParserType mode) {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(mode);
        final Object lists = parser.deserialize("<list><e><list>[1,2]</list></e><e><list>[3]</list></e></list>", Type.of("List<ImmutableList<Integer>>"));
        final List<?> result = assertInstanceOf(List.class, lists);
        assertEquals(List.of(List.of(1, 2), List.of(3)), result);
        assertInstanceOf(ImmutableList.class, result.get(0));

        final Object maps = parser.deserialize("<list><e><map>{\"a\":1}</map></e><e><map>{\"b\":2}</map></e></list>",
                Type.of("List<ImmutableMap<String,Integer>>"));
        final List<?> mapResult = assertInstanceOf(List.class, maps);
        assertEquals(List.of(Map.of("a", 1), Map.of("b", 2)), mapResult);
        assertInstanceOf(ImmutableMap.class, mapResult.get(0));

        assertTrue(parser.deserialize("<list> \t </list>", List.class).isEmpty());
        assertTrue(parser.deserialize("<map>{}</map>", Map.class).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(XmlParserType.class)
    void copyOnWriteCollectionsAreCompletedAtRootAndNestedBoundaries(final XmlParserType mode) {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(mode);
        for (final String body : List.of("<e>1</e><e>2</e>", "[1,2]")) {
            final Type<?> target = Type.of("java.util.concurrent.CopyOnWriteArrayList<Integer>");
            assertEquals(CopyOnWriteArrayList.class, target.javaType());
            final Object root = parser.deserialize("<list>" + body + "</list>", target);
            assertInstanceOf(CopyOnWriteArrayList.class, root);
            assertEquals(List.of(1, 2), root);
            final Object nested = parser.deserialize("<list><e><list>" + body + "</list></e></list>",
                    Type.of("List<java.util.concurrent.CopyOnWriteArrayList<Integer>>"));
            assertInstanceOf(CopyOnWriteArrayList.class, ((List<?>) nested).get(0));
            assertEquals(List.of(List.of(1, 2)), nested);
        }
    }

    @Test
    void staxDoesNotReadDiscardedInterEntryWhitespaceIntoStringsOrCharacterBuffers() throws Exception {
        final String xml = "<map>" + (" ".repeat(4096) + "<entry><key>k</key><value>v</value></entry>").repeat(32) + " </map>";
        final TextCountingReader reader = new TextCountingReader(XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(xml)));
        try {
            while (reader.getEventType() != XMLStreamConstants.START_ELEMENT) {
                reader.next();
            }
            final Object result = new AbacusXmlParserImpl(XmlParserType.StAX).readByStreamParser(reader, new XmlDeserConfig(), Type.of(Map.class));
            assertEquals(Map.of("k", "v"), result);
            assertEquals(0, reader.whitespaceReads);
        } finally {
            reader.close();
        }
    }

    @Test
    void staxCompactTextStillJoinsFragmentsAndPreservesIgnoredEntryFallback() {
        final AbacusXmlParserImpl parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        final Type<Map<String, String>> type = Type.of("Map<String,String>");
        assertEquals(Map.of("a", " b "), parser.deserialize("<map> \n{<![CDATA[\"a\":\" b \"]]><![CDATA[}]]> </map>", type));
        final XmlDeserConfig config = XmlDeserConfig.create().setIgnoredPropNames(Set.of("skip"));
        assertEquals(Map.of("a", "b"), parser.deserialize("<map>{\"a\":\"b\"}<entry><key>skip</key><value>discarded</value></entry> </map>", config, type));
        assertEquals(Map.of("k", "v"), parser.deserialize("<map>discarded<entry><key>k</key><value>v</value></entry>also discarded</map>", type));
        assertThrows(ParsingException.class, () -> parser.deserialize("<map>{broken}</map>", type));
        assertEquals(Map.of("a", "b"), parser.deserialize("<map>{\"a\":\"b\"}</map>", type));
    }

    @Test
    void staxDoesNotAddEmptinessQueriesToCustomMapHooks() {
        final Type<ObservedEmptyMap> type = Type.of(ObservedEmptyMap.class);
        ObservedEmptyMap.emptyQueries = 0;
        final ObservedEmptyMap result = new AbacusXmlParserImpl(XmlParserType.StAX)
                .deserialize("<map>before<entry><key>k</key><value>v</value></entry>after</map>", type);
        assertEquals(Map.of("k", "v"), result);
        assertEquals(1, ObservedEmptyMap.emptyQueries);
    }

    private static final class TextCountingReader extends StreamReaderDelegate {
        private int whitespaceReads;

        TextCountingReader(final XMLStreamReader parent) {
            super(parent);
        }

        @Override
        public String getText() {
            if (isWhiteSpace()) {
                whitespaceReads++;
            }
            return super.getText();
        }

        @Override
        public char[] getTextCharacters() {
            if (isWhiteSpace()) {
                whitespaceReads++;
            }
            return super.getTextCharacters();
        }
    }

    public static class CountingList extends ArrayList<String> {
        static int constructors;
        static int bulkCopies;
        final int constructionId;

        public CountingList() {
            constructionId = ++constructors;
        }

        @Override
        public boolean addAll(final Collection<? extends String> values) {
            bulkCopies++;
            return super.addAll(values);
        }
    }

    public static class CountingMap extends LinkedHashMap<String, String> {
        static int constructors;
        static int bulkCopies;
        final int constructionId;

        public CountingMap() {
            constructionId = ++constructors;
        }

        @Override
        public void putAll(final Map<? extends String, ? extends String> values) {
            bulkCopies++;
            super.putAll(values);
        }
    }

    public static class ObservedEmptyMap extends LinkedHashMap<String, String> {
        static int emptyQueries;

        @Override
        public boolean isEmpty() {
            emptyQueries++;
            return super.isEmpty();
        }
    }
}
