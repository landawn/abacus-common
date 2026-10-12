package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;

public class XmlNameReview20261003Test extends TestBase {

    public static class ModernNameBean {
        @JsonXmlField(name = "\u0370")
        public String value;
    }

    private static ModernNameBean modernNameBean() {
        final ModernNameBean bean = new ModernNameBean();
        bean.value = "x";
        return bean;
    }

    private static XmlParser[] standardParsers() {
        return new XmlParser[] { new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM) };
    }

    private static XmlParser[] allParsers() {
        return new XmlParser[] { new XmlParserImpl(XmlParserType.StAX), new XmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.StAX),
                new AbacusXmlParserImpl(XmlParserType.DOM), new AbacusXmlParserImpl(XmlParserType.SAX) };
    }

    @Test
    public void modernUnicodeLettersAreRejectedBeforeWritingMapTags() {
        for (final XmlParser parser : standardParsers()) {
            for (final String name : new String[] { "\u0370", "\u0237", "\u1c90", "a\u0370", "a\u0237", "a\u1c90" }) {
                assertThrows(ParsingException.class, () -> parser.serialize(Map.of(name, "x")), name);
            }
        }
    }

    @Test
    public void modernUnicodePropertyNameIsRejectedBeforeWritingBeanTags() {
        for (final XmlParser parser : allParsers()) {
            assertThrows(ParsingException.class, () -> parser.serialize(modernNameBean()));
        }
    }

    @Test
    public void establishedNamesStillRoundTrip() {
        for (final XmlParser parser : standardParsers()) {
            for (final String name : new String[] { "_a-b.c_0", "caf\u00e9", "\u4e2d\u6587", "a\u0301", "a\u0661", "a\u00b7b" }) {
                final Map<String, String> value = Map.of(name, "x");
                assertEquals(value, parser.deserialize(parser.serialize(value), Map.class), name);
            }
        }
    }

    @Test
    public void fourthEditionIdeographicsAndExtendersRoundTrip() {
        for (final XmlParser parser : standardParsers()) {
            // These are explicitly allowed by Appendix B, although current Unicode categories alone miss them.
            for (final String name : new String[] { "\u3007", "\u3021", "\u2180", "a\u0387", "a\u3031" }) {
                final Map<String, String> value = Map.of(name, "x");
                assertEquals(value, parser.deserialize(parser.serialize(value), Map.class), name);
            }
        }
    }

    @Test
    public void unsupportedNameShapesRemainRejected() {
        for (final String name : new String[] { "", "1a", "a b", "a:b", "\ud800", "\udc00", "\ud800\udc00" }) {
            assertThrows(ParsingException.class, () -> AbstractXmlParser.checkXmlElementName(name, "name"));
            assertThrows(ParsingException.class, () -> AbstractXmlParser.checkXmlElementName(name.toCharArray(), "name"));
        }
    }

    @Test
    public void modernUnicodeNamesStillWorkAsAttributeValues() {
        for (final XmlParser parser : allParsers()) {
            final String xml = parser.serialize(modernNameBean(), XmlSerConfig.create().setTagByPropertyName(false));
            assertEquals("x", parser.deserialize(xml, ModernNameBean.class).value);
        }
    }
}
