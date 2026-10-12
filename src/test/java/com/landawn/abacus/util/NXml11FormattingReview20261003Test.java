package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.XmlSerConfig;

@Tag("unit")
class NXml11FormattingReview20261003Test extends TestBase {
    private static final String XML_11 = "<?xml version=\"1.1\"?>";

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void restrictedTextRemainsReadable(final boolean configured) throws Exception {
        final String input = XML_11 + "<r>before" + restrictedReferences() + "<child/>after</r>";
        final String output = format(input, configured);
        assertEquals(parse(input).getDocumentElement().getTextContent(), parse(output).getDocumentElement().getTextContent());
        assertTrue(output.startsWith(XML_11));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void restrictedAttributesRemainReadable(final boolean configured) throws Exception {
        final String input = XML_11 + "<r a=\"" + restrictedReferences() + "\"><child/></r>";
        final String output = format(input, configured);
        assertEquals(parse(input).getDocumentElement().getAttribute("a"), parse(output).getDocumentElement().getAttribute("a"));
        assertTrue(output.startsWith(XML_11));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void referencedXml11LineEndsAvoidSecondNormalization(final boolean configured) throws Exception {
        final String input = XML_11 + "<r a=\"&#133;&#8232;&#13;&#10;&#9;\">&#133;<c/>&#8232;&#13;&#10;&#9;</r>";
        final String output = format(input, configured);
        final Document before = parse(input);
        final Document after = parse(output);
        assertEquals(before.getDocumentElement().getTextContent(), after.getDocumentElement().getTextContent());
        assertEquals(before.getDocumentElement().getAttribute("a"), after.getDocumentElement().getAttribute("a"));
        assertEquals("1.1", after.getXmlVersion());
        assertFalse(output.contains("\u0085"));
        assertFalse(output.contains("\u2028"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void xml11OtherNodeKindsKeepParsedContent(final boolean configured) throws Exception {
        final String input = XML_11 + "<?p before?><r xml:space=\"preserve\"><!--a\u0085b--><![CDATA[c\u2028d]]><?q e\u0085f?></r>";
        final String output = format(input, configured);
        assertTrue(output.startsWith(XML_11));
        assertTrue(output.contains("<!--a\nb-->"));
        assertTrue(output.contains("<![CDATA[c\nd]]>"));
        assertTrue(output.contains("<?q e\nf?>"));
        assertTrue(parse(input).getDocumentElement().isEqualNode(parse(output).getDocumentElement()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void xml10EscapingAndDeclarationOmissionStayUnchanged(final boolean configured) throws Exception {
        final String input = "<?xml version=\"1.0\"?><r a=\"&quot;&lt;&amp;&#13;&#10;&#9;\">&lt;&amp;&gt;&#13;&#10;&#9;</r>";
        final String output = format(input, configured);
        assertEquals("<r a=\"&quot;&lt;&amp;&#13;&#10;&#9;\">&lt;&amp;&gt;&#13;\n\t</r>", output);
        assertTrue(parse(input).getDocumentElement().isEqualNode(parse(output).getDocumentElement()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void xml10C1AndUnicodeLineSeparatorsStayLiteral(final boolean configured) throws Exception {
        final String value = "\u007f\u0084\u0085\u0086\u009f\u2028";
        final String input = "<r a=\"" + value + "\">" + value + "<c/></r>";
        final String output = format(input, configured);
        assertEquals(input, output);
        assertTrue(parse(input).getDocumentElement().isEqualNode(parse(output).getDocumentElement()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void doctypeRemainsRejected(final boolean configured) {
        assertThrows(ParsingException.class, () -> format(XML_11 + "<!DOCTYPE r [<!ENTITY e 'x'>]><r>&e;</r>", configured));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void blankInputRemainsEmpty(final boolean configured) {
        assertEquals("", format(null, configured));
        assertEquals("", format(" \n\t", configured));
    }

    private static String format(final String xml, final boolean configured) {
        return configured ? N.formatXml(xml, new XmlSerConfig().setIndentation("  ")) : N.formatXml(xml);
    }

    private static Document parse(final String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String restrictedReferences() {
        final StringBuilder result = new StringBuilder();
        // XML 1.1 RestrictedChar production: every member, not just the range endpoints.
        for (final int[] range : new int[][] { { 1, 8 }, { 11, 12 }, { 14, 31 }, { 127, 132 }, { 134, 159 } }) {
            for (int value = range[0]; value <= range[1]; value++) {
                result.append("&#").append(value).append(';');
            }
        }
        return result.toString();
    }
}
