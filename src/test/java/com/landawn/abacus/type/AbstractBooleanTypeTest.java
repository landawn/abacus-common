package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.u.OptionalBoolean;

public class AbstractBooleanTypeTest extends TestBase {
    private Type<Boolean> type;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Boolean.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_True() {
        assertEquals("true", type.stringOf(Boolean.TRUE));
    }

    @Test
    public void testStringOf_False() {
        assertEquals("false", type.stringOf(Boolean.FALSE));
    }

    @Test
    public void testStringOf_Null() {
        assertEquals(null, type.stringOf(null));
    }

    @Test
    public void testValueOf_BooleanObject() {
        assertEquals(Boolean.TRUE, type.valueOf(Boolean.TRUE));
        assertEquals(Boolean.FALSE, type.valueOf(Boolean.FALSE));
    }

    @Test
    public void testValueOf_CharSequence_Y() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "Y"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "y"));
    }

    @Test
    public void testValueOf_CharSequence_1() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "1"));
    }

    @Test
    public void testValueOf_CharSequence_True() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) "true"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "TRUE"));
    }

    @Test
    public void testValueOf_CharSequence_False() {
        assertEquals(Boolean.FALSE, type.valueOf((Object) "false"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "FALSE"));
    }

    @Test
    public void testValueOf_CharSequence_Other() {
        assertEquals(Boolean.FALSE, type.valueOf((Object) "N"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "0"));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "abc"));
    }

    @Test
    public void testValueOf_String_Boolean() {
        assertEquals(Boolean.TRUE, type.valueOf("true"));
        assertEquals(Boolean.TRUE, type.valueOf("TRUE"));
        assertEquals(Boolean.FALSE, type.valueOf("false"));
        assertEquals(Boolean.FALSE, type.valueOf("FALSE"));
    }

    @Test
    public void testValueOf_CharArray_True() {
        char[] cbuf = "true".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));

        cbuf = "TRUE".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));

        cbuf = "TrUe".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 0, 4));
    }

    @Test
    public void testValueOf_CharArray_False() {
        char[] cbuf = "false".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(cbuf, 0, 5));

        cbuf = "xyz".toCharArray();
        assertEquals(Boolean.FALSE, type.valueOf(cbuf, 0, 3));
    }

    @Test
    public void testValueOf_CharArray_Offset() {
        char[] cbuf = "xxtrueyy".toCharArray();
        assertEquals(Boolean.TRUE, type.valueOf(cbuf, 2, 4));
    }

    @Test
    public void testValueOf_NullObject() {
        Boolean result = type.valueOf((Object) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_PositiveNumber() {
        assertEquals(Boolean.TRUE, type.valueOf(1));
        assertEquals(Boolean.TRUE, type.valueOf(100L));
        assertEquals(Boolean.FALSE, type.valueOf(0.1));
    }

    @Test
    public void testValueOf_ZeroNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(0));
        assertEquals(Boolean.FALSE, type.valueOf(0L));
        assertEquals(Boolean.FALSE, type.valueOf(0.0));
    }

    @Test
    public void testValueOf_NegativeNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(-1));
        assertEquals(Boolean.FALSE, type.valueOf(-100L));
    }

    @Test
    public void testValueOf_String_Null() {
        Boolean result = type.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Boolean result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOf_String_SingleChar() {
        assertEquals(Boolean.TRUE, type.valueOf("Y"));
        assertEquals(Boolean.TRUE, type.valueOf("y"));
        assertEquals(Boolean.TRUE, type.valueOf("1"));
        assertEquals(Boolean.FALSE, type.valueOf("N"));
        assertEquals(Boolean.FALSE, type.valueOf("0"));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Boolean result = type.valueOf(null, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        Boolean result = type.valueOf(cbuf, 0, 0);
        assertNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(true);
        assertEquals(Boolean.TRUE, type.get(resultSet, 1));
        verify(resultSet).getObject(1);
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        when(resultSet.getObject("active")).thenReturn(false);
        assertEquals(Boolean.FALSE, type.get(resultSet, "active"));
        verify(resultSet).getObject("active");
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setNull(1, java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSet_PreparedStatement_True() throws SQLException {
        type.set(preparedStatement, 1, Boolean.TRUE);
        verify(preparedStatement).setBoolean(1, true);
    }

    @Test
    public void testSet_PreparedStatement_False() throws SQLException {
        type.set(preparedStatement, 1, Boolean.FALSE);
        verify(preparedStatement).setBoolean(1, false);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setNull("param", java.sql.Types.BOOLEAN);
    }

    @Test
    public void testSet_CallableStatement_True() throws SQLException {
        type.set(callableStatement, "param", Boolean.TRUE);
        verify(callableStatement).setBoolean("param", true);
    }

    @Test
    public void testSet_CallableStatement_False() throws SQLException {
        type.set(callableStatement, "param", Boolean.FALSE);
        verify(callableStatement).setBoolean("param", false);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_True() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Boolean.TRUE);
        assertEquals("true", sb.toString());
    }

    @Test
    public void testAppendTo_False() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, Boolean.FALSE);
        assertEquals("false", sb.toString());
    }

    @Test
    public void testSerializeTo_Null_NoConfig() throws IOException {
        assertDoesNotThrow(() -> {
            type.serializeTo(characterWriter, null, null);
        });
    }

    @Test
    public void testSerializeTo_True_NoConfig() throws IOException {
        assertDoesNotThrow(() -> {
            type.serializeTo(characterWriter, Boolean.TRUE, null);
        });
    }

    @Test
    public void testSerializeTo_False_NoConfig() throws IOException {
        assertDoesNotThrow(() -> {
            type.serializeTo(characterWriter, Boolean.FALSE, null);
        });
    }

    @Test
    public void testSerializeTo_Null_WithWriteNullBooleanAsFalse() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.isWriteNullBooleanAsFalse()).thenReturn(true);
            type.serializeTo(characterWriter, null, config);
        });
    }

    @Test
    public void testSerializeTo_Null_WithoutWriteNullBooleanAsFalse() throws IOException {
        assertDoesNotThrow(() -> {
            when(config.isWriteNullBooleanAsFalse()).thenReturn(false);
            type.serializeTo(characterWriter, null, config);
        });
    }

    // Bug: valueOf used isEmpty (no trim), so " Y" / blank diverged from AtomicBooleanType.
    @Test
    public void testValueOf_trimsAndTreatsBlankAsDefault() {
        assertEquals(type.defaultValue(), type.valueOf("   "));
        assertEquals(Boolean.TRUE, type.valueOf(" Y"));
        assertEquals(Boolean.TRUE, type.valueOf(" 1 "));
        assertEquals(Boolean.TRUE, type.valueOf(" true "));
        assertEquals(Boolean.FALSE, type.valueOf(" false "));
    }

    // T4-04: the trim fix above reached valueOf(String) only; valueOf(Object) and valueOf(char[]) (the JSON path)
    // still used the untrimmed rule, and a Character fell through to Boolean.valueOf ('Y' -> false).
    @Test
    public void reviewFixes20260906_objectPath_trimsAndReadsCharacterAsOneCharString() {
        assertEquals(Boolean.TRUE, type.valueOf((Object) " Y"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) new StringBuilder(" true ")));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "TRUE "));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "\tY\n"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) " 1 "));
        assertEquals(Boolean.TRUE, type.valueOf((Object) 'Y'));
        assertEquals(Boolean.TRUE, type.valueOf((Object) 'y'));
        assertEquals(Boolean.TRUE, type.valueOf((Object) '1'));
        assertEquals(Boolean.FALSE, type.valueOf((Object) 'N'));
        assertEquals(Boolean.FALSE, type.valueOf((Object) ' '));
        assertEquals(Boolean.FALSE, type.valueOf((Object) " false "));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "t rue"));
        // G05-113: a full-width space (U+3000) is padding here too, as it is to the blank test
        assertEquals(Boolean.TRUE, type.valueOf((Object) "　Y"));
        // the documented "empty CharSequence -> FALSE, not the default" special case is kept and covers blank text
        assertEquals(Boolean.FALSE, type.valueOf((Object) ""));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "   "));
        assertEquals(Boolean.FALSE, type.valueOf((Object) new StringBuilder()));
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260906_charArrayPath_trimsWhenFastChecksMiss() {
        assertEquals(Boolean.TRUE, type.valueOf(" Y".toCharArray(), 0, 2));
        assertEquals(Boolean.TRUE, type.valueOf(" true ".toCharArray(), 0, 6));
        assertEquals(Boolean.TRUE, type.valueOf("TRUE ".toCharArray(), 0, 5));
        assertEquals(Boolean.TRUE, type.valueOf("\tY\n".toCharArray(), 0, 3));
        assertEquals(Boolean.TRUE, type.valueOf(" 1".toCharArray(), 0, 2));
        // only the region counts, not its neighbours
        assertEquals(Boolean.TRUE, type.valueOf("x Y x".toCharArray(), 1, 3));
        assertEquals(Boolean.TRUE, type.valueOf("xxtrueyy".toCharArray(), 2, 4));
        assertEquals(Boolean.TRUE, type.valueOf("Y".toCharArray(), 0, 1));
        assertEquals(Boolean.FALSE, type.valueOf(" false ".toCharArray(), 0, 7));
        assertEquals(Boolean.FALSE, type.valueOf("t rue".toCharArray(), 0, 5));
        // G05-113: U+3000 is padding on this path too, and the String overload answers the same
        assertEquals(Boolean.TRUE, type.valueOf("　Y".toCharArray(), 0, 2));
        // R11: a whitespace-only region is blank at EVERY length -> the default (null here), as valueOf(String)
        // and the XML reader already gave; the one-character region used to answer FALSE and kept JSON and XML apart
        assertNull(type.valueOf(" ".toCharArray(), 0, 1));
        assertEquals(type.valueOf(" "), type.valueOf(" ".toCharArray(), 0, 1));
        assertNull(type.valueOf(new char[] { (char) 9 }, 0, 1)); // a tab is blank too
        assertNull(type.valueOf("x y".toCharArray(), 1, 1));
        assertNull(type.valueOf("   ".toCharArray(), 0, 3));
        assertEquals(type.defaultValue(), type.valueOf("   ".toCharArray(), 0, 3));
        // a lone non-whitespace character that is not Y/y/1 is still FALSE
        assertEquals(Boolean.FALSE, type.valueOf("N".toCharArray(), 0, 1));
        assertEquals(Boolean.FALSE, type.valueOf("0".toCharArray(), 0, 1));
        // a non-breaking space (U+00A0) is neither <= ' ' nor Java whitespace, so it stays FALSE on both paths
        assertEquals(Boolean.FALSE, type.valueOf(new char[] { (char) 0x00a0 }, 0, 1));
        assertEquals(Boolean.FALSE, type.valueOf(String.valueOf((char) 0x00a0)));
        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(new char[0], 0, 0));
    }

    // R12: the char[] fast paths defined padding as "<= ' '" while valueOf(String) rejects Character.isWhitespace
    // text as blank, so the JSON (char[]) and XML (String) readers still disagreed on Unicode whitespace above
    // U+0020: valueOf(new char[] { IDEOGRAPHIC_SPACE }, 0, 1) answered FALSE where valueOf(String) answers the default.
    @Test
    public void reviewFixes20260908_charArrayAndStringAgreeOnUnicodePadding() {
        final Type<Boolean> primitiveType = createType(boolean.class);
        final char ideographicSpace = 0x3000; // Character.isWhitespace, but greater than ' '
        final char lineSeparator = 0x2028; // Character.isWhitespace, but greater than ' '
        final char startOfHeading = 0x0001; // <= ' ' (so String.trim() removes it), but not Character.isWhitespace

        // the reported divergence: a lone U+3000 is blank text on both paths now
        assertNull(type.valueOf(new char[] { ideographicSpace }, 0, 1));
        assertEquals(Boolean.FALSE, primitiveType.valueOf(new char[] { ideographicSpace }, 0, 1));
        assertNull(type.valueOf(new char[] { ideographicSpace, lineSeparator, ' ' }, 0, 3));
        assertEquals(Boolean.FALSE, primitiveType.valueOf(new char[] { ideographicSpace, lineSeparator, ' ' }, 0, 3));

        // both paths strip U+3000 padding (see reviewFixes20260908_unicodePaddingIsStrippedNotOnlyDetected)
        assertEquals(type.valueOf(String.valueOf(ideographicSpace) + 'Y'), type.valueOf(new char[] { ideographicSpace, 'Y' }, 0, 2));
        assertEquals(type.valueOf("Y" + ideographicSpace), type.valueOf(new char[] { 'Y', ideographicSpace }, 0, 2));

        // C0 controls are still stripped by valueOf(String), so the char[] path must keep deferring for them
        assertEquals(Boolean.TRUE, type.valueOf(new char[] { startOfHeading, 'Y' }, 0, 2));
        assertEquals(type.valueOf(String.valueOf(startOfHeading) + 'Y'), type.valueOf(new char[] { startOfHeading, 'Y' }, 0, 2));

        // R04 review 2026-09-08: the two halves of isPadding do NOT land on the same answer for a lone padding
        // character - a Unicode space is blank text (the default value), a C0 control is stripped to "" and read
        // by Boolean.valueOf (FALSE). Deferring to valueOf(String) is what keeps both paths on the same one.
        assertNull(type.valueOf(String.valueOf(ideographicSpace)));
        assertEquals(Boolean.FALSE, type.valueOf(String.valueOf(startOfHeading)));
        assertEquals(Boolean.FALSE, type.valueOf(new char[] { startOfHeading }, 0, 1));

        // the invariant in general: for every char, the char[] overload must answer exactly what the String
        // overload answers for the same text, at every region length the fast paths care about
        for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
            final char ch = (char) i;

            assertCharArrayMatchesString(type, new char[] { ch });
            assertCharArrayMatchesString(type, new char[] { ch, 'Y' });
            assertCharArrayMatchesString(type, new char[] { 'Y', ch });
            assertCharArrayMatchesString(type, new char[] { ch, 'r', 'u', 'e' });
            assertCharArrayMatchesString(type, new char[] { 't', 'r', 'u', ch });
            assertCharArrayMatchesString(type, new char[] { ch, 't', 'r', 'u', 'e' });
            assertCharArrayMatchesString(type, new char[] { 't', 'r', 'u', 'e', ch });
        }
    }

    // G05-113: valueOf(String) tested for blankness with Character.isWhitespace (Strings.isBlank) but removed
    // padding with String.trim(), which only removes characters <= ' ': a lone U+3000 was blank, yet U+3000
    // followed by "true" or by " Y" kept its padding and parsed as FALSE.
    @Test
    public void reviewFixes20260908_unicodePaddingIsStrippedNotOnlyDetected() {
        final String ideographicSpace = String.valueOf((char) 0x3000); // Character.isWhitespace, but greater than ' '
        final String startOfHeading = String.valueOf((char) 0x0001); // <= ' ', but not Character.isWhitespace

        assertEquals(Boolean.TRUE, type.valueOf(ideographicSpace + "true"));
        assertEquals(Boolean.TRUE, type.valueOf(ideographicSpace + "TRUE" + ideographicSpace));
        assertEquals(Boolean.TRUE, type.valueOf(ideographicSpace + "Y"));
        assertEquals(Boolean.TRUE, type.valueOf("1" + ideographicSpace));
        assertEquals(Boolean.FALSE, type.valueOf(ideographicSpace + "N"));

        // both padding definitions are honoured, so the control characters String.trim() removed still go
        assertEquals(Boolean.TRUE, type.valueOf(startOfHeading + "true" + startOfHeading));
        assertEquals(Boolean.TRUE, type.valueOf(startOfHeading + ideographicSpace + " Y "));

        // blank text (of either kind) still yields the default value, and interior padding is still not removed
        assertNull(type.valueOf(ideographicSpace));
        assertNull(type.valueOf(ideographicSpace + " " + ideographicSpace));
        assertEquals(Boolean.FALSE, type.valueOf("tr" + ideographicSpace + "ue"));
        // a non-breaking space is neither <= ' ' nor Character.isWhitespace: not padding
        assertEquals(Boolean.FALSE, type.valueOf((char) 0x00a0 + "true"));

        // valueOf(Object) reads a CharSequence by the same rule (blank stays the documented FALSE)
        assertEquals(Boolean.TRUE, type.valueOf((Object) (ideographicSpace + "true")));
        assertEquals(Boolean.TRUE, type.valueOf((Object) new StringBuilder(ideographicSpace + "Y")));
        assertEquals(Boolean.FALSE, type.valueOf((Object) ideographicSpace));

        // the strip lives in the shared AbstractType.parseBoolean, so the other boolean handlers agree
        assertEquals(true, createType(AtomicBoolean.class).valueOf(ideographicSpace + "true").get());
        assertNull(createType(AtomicBoolean.class).valueOf(ideographicSpace));
        assertEquals(true, createType(MutableBoolean.class).valueOf(ideographicSpace + "Y").value());
        assertNull(createType(MutableBoolean.class).valueOf(ideographicSpace));
        assertEquals(OptionalBoolean.of(true), createType(OptionalBoolean.class).valueOf(ideographicSpace + "1"));
        assertEquals(OptionalBoolean.empty(), createType(OptionalBoolean.class).valueOf(ideographicSpace));
    }

    private static void assertCharArrayMatchesString(final Type<Boolean> type, final char[] cbuf) {
        final String str = new String(cbuf);
        // read the region out of a larger buffer so the offset/len handling is exercised as well
        final char[] region = ("[" + str + "]").toCharArray();

        assertEquals(type.valueOf(str), type.valueOf(region, 1, cbuf.length), () -> "char[] and String disagree on " + toEscapedString(str));
    }

    private static String toEscapedString(final String str) {
        final StringBuilder sb = new StringBuilder(str.length() * 7);

        for (int i = 0; i < str.length(); i++) {
            sb.append(String.format("U+%04X ", (int) str.charAt(i)));
        }

        return sb.toString().trim();
    }

    public static class BoolBean {
        private boolean b;
        private Boolean w;

        public boolean isB() {
            return b;
        }

        public void setB(final boolean b) {
            this.b = b;
        }

        public Boolean getW() {
            return w;
        }

        public void setW(final Boolean w) {
            this.w = w;
        }
    }

    @Test
    public void reviewFixes20260906_jsonAndXmlAgree() {
        BoolBean fromJson = ParserFactory.createJsonParser().deserialize("{\"b\": \" true \", \"w\": \" Y\"}", BoolBean.class);
        BoolBean fromXml = ParserFactory.createXmlParser().deserialize("<boolBean><b> true </b><w> Y</w></boolBean>", BoolBean.class);
        assertEquals(true, fromJson.isB());
        assertEquals(Boolean.TRUE, fromJson.getW());
        assertEquals(true, fromXml.isB());
        assertEquals(Boolean.TRUE, fromXml.getW());

        assertNull(ParserFactory.createJsonParser().deserialize("{\"w\": \"  \"}", BoolBean.class).getW());
        assertNull(ParserFactory.createXmlParser().deserialize("<boolBean><w>  </w></boolBean>", BoolBean.class).getW());
        // R11: a single blank character must agree too (JSON used to read it as FALSE, XML as null)
        assertNull(ParserFactory.createJsonParser().deserialize("{\"w\": \" \"}", BoolBean.class).getW());
        assertNull(ParserFactory.createXmlParser().deserialize("<boolBean><w> </w></boolBean>", BoolBean.class).getW());
        assertEquals(Boolean.FALSE, ParserFactory.createJsonParser().deserialize("{\"w\": \"N\"}", BoolBean.class).getW());
    }
}
