package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;

public class JsonCanonicalConfigurationTest extends TestBase {
    public static class Bean {
        @JsonXmlField(aliases = "given")
        public Object firstName;
        @JsonXmlField(aliases = "entries")
        public List<Integer> firstItems;
    }

    private static Bean read(final boolean reader, final String json, final JsonDeserConfig config) {
        final JsonParser parser = ParserFactory.createJsonParser();
        return reader ? parser.deserialize(new StringReader(json), config, Bean.class) : parser.deserialize(json, config, Bean.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void canonicalValueTypesApplyToEveryAcceptedSpelling(final boolean reader) {
        for (final String name : List.of("firstName", "first_name", "FIRSTNAME", "given")) {
            for (final String quote : List.of("\"", "'", "")) {
                assertEquals(1L, read(reader, "{" + quote + name + quote + ":1}", JsonDeserConfig.create().setValueType("firstName", Long.class)).firstName,
                        name + quote);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void canonicalHandlersApplyToEveryAcceptedSpelling(final boolean reader) {
        for (final String name : List.of("firstItems", "first_items", "FIRSTITEMS", "entries")) {
            for (final String quote : List.of("\"", "'", "")) {
                assertEquals(List.of(11, 12),
                        read(reader, "{" + quote + name + quote + ":[1,2]}",
                                JsonDeserConfig.create().setPropHandler("firstItems", (values, element) -> values.add((Integer) element + 10))).firstItems,
                        name + quote);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void wireNameConfigurationWinsWhenBothNamesAreConfigured(final boolean reader) {
        final JsonDeserConfig config = JsonDeserConfig.create()
                .setValueType("firstName", Long.class)
                .setValueType("given", Double.class)
                .setPropHandler("firstItems", (values, element) -> values.add((Integer) element + 10))
                .setPropHandler("entries", (values, element) -> values.add((Integer) element + 100));
        for (final String quote : List.of("\"", "'", "")) {
            final Bean bean = read(reader, "{" + quote + "given" + quote + ":1," + quote + "entries" + quote + ":[1]}", config);
            assertEquals(1.0, bean.firstName);
            assertEquals(List.of(101), bean.firstItems);
        }
    }
}
