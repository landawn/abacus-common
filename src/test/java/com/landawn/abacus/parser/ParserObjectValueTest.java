package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

public class ParserObjectValueTest extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();

    public static class ValueBean {
        private String a;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }
    }

    @Test
    public void missingObjectValuesAreRejectedAcrossTargetsAndSources() {
        for (String json : List.of("{\"a\":}", "{\"a\":   }", "{\"a\":,\"b\":2}", "{\"a\":", "a:", "{\"a\":null,\"b\":}")) {
            for (Class<?> target : List.of(Map.class, ValueBean.class)) {
                assertThrows(ParsingException.class, () -> parser.deserialize(json, target), json + "/" + target);
                assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), target), json + "/" + target);
            }
        }
    }

    @Test
    public void ignoredAndUnknownPropertiesStillRequireValues() {
        for (JsonDeserConfig config : List.of(JsonDeserConfig.create().setIgnoreUnmatchedProperty(true),
                JsonDeserConfig.create().setIgnoredPropNames(Set.of("a", "unknown")))) {
            for (String json : List.of("{\"a\":}", "{\"unknown\":}", "{\"unknown\":,\"a\":\"ok\"}")) {
                for (Class<?> target : List.of(Map.class, ValueBean.class)) {
                    assertThrows(ParsingException.class, () -> parser.deserialize(json, config, target), json);
                    assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), config, target), json);
                }
            }
        }
    }

    @Test
    public void nestedMissingValuesAreRejected() {
        for (String json : List.of("{\"nested\":{\"a\":}}", "[{\"a\":}]", "{\"nested\":[{\"a\":,\"b\":2}]}")) {
            Class<?> target = json.startsWith("[") ? List.class : Map.class;
            assertThrows(ParsingException.class, () -> parser.deserialize(json, target), json);
            assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), target), json);
        }
    }

    @Test
    public void explicitNullEmptyAndCompletedValuesRemainValid() {
        for (String json : List.of("{\"a\":null}", "{\"a\":\"\"}", "{\"a\":\"\u540d\ud83d\ude00\"}")) {
            Map<?, ?> expected = parser.deserialize(json, Map.class);
            assertEquals(expected, parser.deserialize(new StringReader(json), Map.class));
            assertEquals(expected.get("a"), parser.deserialize(json, ValueBean.class).getA());
        }
        assertNull(parser.deserialize("{\"a\":null}", ValueBean.class).getA());
        assertEquals("", parser.deserialize("{\"a\":\"\"}", ValueBean.class).getA());
        assertEquals(Map.of(), parser.deserialize("{}", Map.class));
        assertEquals(Map.of("a", List.of(), "b", Map.of()), parser.deserialize("{\"a\":[],\"b\":{}}", Map.class));
    }
}
