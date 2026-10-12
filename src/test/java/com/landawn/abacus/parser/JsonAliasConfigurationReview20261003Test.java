package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Direction;

public class JsonAliasConfigurationReview20261003Test extends TestBase {
    public static class Bean {
        @JsonXmlField(aliases = { "alias" })
        private Object value;
        @JsonXmlField(aliases = { "entries" })
        private List<Integer> items;

        public Object getValue() {
            return value;
        }

        public void setValue(final Object value) {
            this.value = value;
        }

        public List<Integer> getItems() {
            return items;
        }

        public void setItems(final List<Integer> items) {
            this.items = items;
        }
    }

    private static Bean read(final boolean reader, final String json, final JsonDeserConfig config) {
        final JsonParser parser = ParserFactory.createJsonParser();
        return reader ? parser.deserialize(new StringReader(json), config, Bean.class) : parser.deserialize(json, config, Bean.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void ignoredWireAliasesMatchQuotedAndUnquotedNames(final boolean reader) {
        final JsonDeserConfig config = JsonDeserConfig.create().setIgnoredPropNames(Set.of("alias"));
        for (final String name : List.of("\"alias\"", "'alias'", "alias", "\"ali\\u0061s\"")) {
            assertNull(read(reader, "{" + name + ":1}", config).getValue(), name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void valueTypeUsesTheWireAlias(final boolean reader) {
        final JsonDeserConfig config = JsonDeserConfig.create().setValueType("alias", Long.class);
        for (final String name : List.of("\"alias\"", "'alias'", "alias")) {
            assertEquals(Long.valueOf(1), read(reader, "{" + name + ":1}", config).getValue(), name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void collectionHandlerUsesTheWireAlias(final boolean reader) {
        final JsonDeserConfig config = JsonDeserConfig.create().setPropHandler("entries", (collection, element) -> collection.add((Integer) element + 10));
        for (final String name : List.of("\"entries\"", "'entries'", "entries")) {
            assertEquals(List.of(11, 12), read(reader, "{" + name + ":[1,2]}", config).getItems(), name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void canonicalNamesAndCanonicalIgnoresRemainSupported(final boolean reader) {
        assertEquals(Integer.valueOf(1), read(reader, "{\"value\":1}", JsonDeserConfig.create()).getValue());
        assertEquals(Integer.valueOf(1), read(reader, "{\"alias\":1}", JsonDeserConfig.create()).getValue());
        assertEquals(Long.valueOf(1), read(reader, "{\"value\":1}", JsonDeserConfig.create().setValueType("value", Long.class)).getValue());
        assertEquals(List.of(11), read(reader, "{\"items\":[1]}",
                JsonDeserConfig.create().setPropHandler("items", (collection, element) -> collection.add((Integer) element + 10))).getItems());
        final JsonDeserConfig ignored = JsonDeserConfig.create().setIgnoredPropNames(Set.of("value"));
        assertNull(read(reader, "{\"value\":1}", ignored).getValue());
        assertNull(read(reader, "{\"alias\":1}", ignored).getValue());
        assertNull(read(reader, "{alias:1}", ignored).getValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void computedHandlersAlsoReceiveTheWireName(final boolean reader) {
        final JsonDeserConfig config = new JsonDeserConfig() {
            @Override
            public BiConsumer<? super Collection<Object>, ?> getPropHandler(final String name) {
                return "entries".equals(name) ? (collection, element) -> collection.add((Integer) element + 10) : super.getPropHandler(name);
            }
        };
        assertEquals(List.of(11), read(reader, "{\"entries\":[1]}", config).getItems());
    }

    public static class WriteOnlyBean {
        @JsonXmlField(aliases = { "entries" }, direction = Direction.SERIALIZE_ONLY)
        private List<Integer> items;

        public List<Integer> getItems() {
            return items;
        }

        public void setItems(final List<Integer> items) {
            this.items = items;
        }
    }

    // ---- follow-up 2026-10-04: discarded known properties resolve handlers like matched ones ----

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void ignoredAliasedPropertyStillFindsItsJavaNameHandler(final boolean reader) {
        final List<Object> seen = new ArrayList<>();
        final JsonDeserConfig config = JsonDeserConfig.create()
                .setIgnoredPropNames(Set.of("items"))
                .setPropHandler("items", (collection, element) -> seen.add(element));
        for (final String name : List.of("\"entries\"", "'entries'", "entries")) {
            seen.clear();
            assertNull(read(reader, "{" + name + ":[1,2]}", config).getItems(), name);
            assertEquals(List.of(1, 2), seen, name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void serializeOnlyAliasedPropertyStillFindsItsJavaNameHandler(final boolean reader) {
        final List<Object> seen = new ArrayList<>();
        final JsonDeserConfig config = JsonDeserConfig.create().setPropHandler("items", (collection, element) -> seen.add(element));
        final JsonParser parser = ParserFactory.createJsonParser();
        final String json = "{\"entries\":[1,2]}";
        final WriteOnlyBean bean = reader ? parser.deserialize(new StringReader(json), config, WriteOnlyBean.class)
                : parser.deserialize(json, config, WriteOnlyBean.class);
        assertNull(bean.getItems());
        assertEquals(List.of(1, 2), seen);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void discardedPropertyPrefersTheWireNameHandler(final boolean reader) {
        final List<Object> javaName = new ArrayList<>();
        final List<Object> wireName = new ArrayList<>();
        final JsonDeserConfig config = JsonDeserConfig.create()
                .setIgnoredPropNames(Set.of("items"))
                .setPropHandler("items", (collection, element) -> javaName.add(element))
                .setPropHandler("entries", (collection, element) -> wireName.add(element));
        assertNull(read(reader, "{\"entries\":[1,2]}", config).getItems());
        assertEquals(List.of(1, 2), wireName);
        assertEquals(List.of(), javaName);
    }
}
