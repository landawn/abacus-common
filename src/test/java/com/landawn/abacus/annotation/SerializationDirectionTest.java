package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlDeserConfig;

public class SerializationDirectionTest extends TestBase {
    public static class InputOnly {
        @JsonXmlField(direction = JsonXmlField.Direction.DESERIALIZE_ONLY)
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Test
    public void documentedDirectionKeepsJsonInputAndOmitsOutput() {
        final var parser = ParserFactory.createJsonParser();
        for (final String token : new String[] { "null", "\"\"", "\"\u03bb\ud83d\ude00\"" }) {
            final InputOnly bean = parser.deserialize("{\"value\":" + token + "}", InputOnly.class);
            final String expected = token.equals("null") ? null : token.substring(1, token.length() - 1);
            assertEquals(expected, bean.getValue());
            assertEquals("{}", parser.serialize(bean));
        }
    }

    @Test
    public void documentedDirectionKeepsXmlInputAndOmitsOutput() {
        final var parser = ParserFactory.createXmlParser();
        final InputOnly bean = parser.deserialize("<InputOnly><value>\u03bb\ud83d\ude00</value></InputOnly>", InputOnly.class);
        assertEquals("\u03bb\ud83d\ude00", bean.getValue());
        assertFalse(parser.serialize(bean).contains("<value"));
    }

    // a13 F-1: @JsonXmlField(ignore = true) and @JsonXmlConfig(ignoredFields) are documented as output-only.
    // These tests pin that contract: the property is omitted from serialized output but still populated from input.
    @JsonXmlConfig(ignoredFields = { "cfgIgnored", "pattern.*" })
    public static class OutputIgnored {
        private String kept;
        @JsonXmlField(ignore = true)
        private String password;
        private String cfgIgnored;
        private String patternIgnored;

        public String getKept() {
            return kept;
        }

        public void setKept(final String kept) {
            this.kept = kept;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public String getCfgIgnored() {
            return cfgIgnored;
        }

        public void setCfgIgnored(final String cfgIgnored) {
            this.cfgIgnored = cfgIgnored;
        }

        public String getPatternIgnored() {
            return patternIgnored;
        }

        public void setPatternIgnored(final String patternIgnored) {
            this.patternIgnored = patternIgnored;
        }
    }

    private static OutputIgnored populated() {
        final OutputIgnored bean = new OutputIgnored();
        bean.setKept("k");
        bean.setPassword("secret");
        bean.setCfgIgnored("cfg");
        bean.setPatternIgnored("pat");
        return bean;
    }

    @Test
    public void ignoredFieldsStillAcceptJsonInputAndAreOmittedFromJsonOutput() {
        final var parser = ParserFactory.createJsonParser();
        for (final String token : new String[] { "null", "\"\"", "\"secret\"", "\"\u03bb\ud83d\ude00\"" }) {
            final String json = "{\"kept\":\"k\",\"password\":" + token + ",\"cfgIgnored\":" + token + ",\"patternIgnored\":" + token + "}";
            final String expected = token.equals("null") ? null : token.substring(1, token.length() - 1);
            for (final OutputIgnored bean : new OutputIgnored[] { parser.deserialize(json, OutputIgnored.class),
                    parser.deserialize(json, JsonDeserConfig.create().setIgnoreUnmatchedProperty(false), OutputIgnored.class) }) {
                assertEquals("k", bean.getKept());
                assertEquals(expected, bean.getPassword(), "ignore = true must not block input");
                assertEquals(expected, bean.getCfgIgnored(), "ignoredFields (exact) must not block input");
                assertEquals(expected, bean.getPatternIgnored(), "ignoredFields (regex) must not block input");
            }
        }
        // Regression guard: the same three properties never reach the output.
        assertEquals("{\"kept\": \"k\"}", parser.serialize(populated()));
    }

    @Test
    public void ignoredFieldsStillAcceptXmlInputAndAreOmittedFromXmlOutput() {
        final var parser = ParserFactory.createXmlParser();
        final String xml = "<OutputIgnored><kept>k</kept><password>\u03bb\ud83d\ude00</password><cfgIgnored>cfg</cfgIgnored>"
                + "<patternIgnored>pat</patternIgnored></OutputIgnored>";
        for (final OutputIgnored bean : new OutputIgnored[] { parser.deserialize(xml, OutputIgnored.class),
                parser.deserialize(xml, XmlDeserConfig.create().setIgnoreUnmatchedProperty(false), OutputIgnored.class) }) {
            assertEquals("k", bean.getKept());
            assertEquals("\u03bb\ud83d\ude00", bean.getPassword());
            assertEquals("cfg", bean.getCfgIgnored());
            assertEquals("pat", bean.getPatternIgnored());
        }
        final String out = parser.serialize(populated());
        assertTrue(out.contains("<kept>k</kept>"), out);
        assertFalse(out.contains("password"), out);
        assertFalse(out.contains("cfgIgnored"), out);
        assertFalse(out.contains("patternIgnored"), out);
    }

    @Test
    public void ignoredFieldWithNonDefaultDirectionIsRejectedAtIntrospection() {
        final var parser = ParserFactory.createJsonParser();
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(new IgnoredButSerializeOnly()));
    }

    public static class IgnoredButSerializeOnly {
        @JsonXmlField(ignore = true, direction = JsonXmlField.Direction.SERIALIZE_ONLY)
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
