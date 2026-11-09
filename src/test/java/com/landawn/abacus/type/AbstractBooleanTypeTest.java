package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.MutableChar;
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

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
        // Text that is none of the accepted texts is rejected (code review 2026-10-05).
        assertThrows(IllegalArgumentException.class, () -> type.valueOf((Object) "abc"));
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

        assertEquals(Boolean.FALSE, type.valueOf("N".toCharArray(), 0, 1));
        assertEquals(Boolean.FALSE, type.valueOf("-1".toCharArray(), 0, 2));
        // Text that is none of the accepted texts is rejected (code review 2026-10-05).
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("xyz".toCharArray(), 0, 3));
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
        assertEquals(Boolean.TRUE, type.valueOf((short) 1));
        assertEquals(Boolean.TRUE, type.valueOf((byte) 1));
        // Only an Integer, Short or Byte 1, 0 or -1 is a boolean value: any other value, and any other Number class
        // (1L, 1.0), is rejected.
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(100));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(1L));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(1.0));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(0.1));
    }

    @Test
    public void testValueOf_ZeroNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(0));
        assertEquals(Boolean.FALSE, type.valueOf((short) 0));
        assertEquals(Boolean.FALSE, type.valueOf((byte) 0));
        // A Long or Double zero is a Number of another class, rejected whatever its value.
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(0L));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(0.0));
    }

    @Test
    public void testValueOf_NegativeNumber() {
        assertEquals(Boolean.FALSE, type.valueOf(-1));
        assertEquals(Boolean.FALSE, type.valueOf((short) -1));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(-100));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(-1L));
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
        assertEquals(Boolean.TRUE, type.valueOf(new StringBuilder(" true ")));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "TRUE "));
        assertEquals(Boolean.TRUE, type.valueOf((Object) "\tY\n"));
        assertEquals(Boolean.TRUE, type.valueOf((Object) " 1 "));
        assertEquals(Boolean.TRUE, type.valueOf('Y'));
        assertEquals(Boolean.TRUE, type.valueOf('y'));
        assertEquals(Boolean.TRUE, type.valueOf('1'));
        assertEquals(Boolean.FALSE, type.valueOf('N'));
        assertEquals(Boolean.FALSE, type.valueOf(' '));
        assertEquals(Boolean.FALSE, type.valueOf((Object) " false "));
        // interior padding is not removed, and the result is not boolean text (code review 2026-10-05)
        assertThrows(IllegalArgumentException.class, () -> type.valueOf((Object) "t rue"));
        // G05-113: a full-width space (U+3000) is padding here too, as it is to the blank test
        assertEquals(Boolean.TRUE, type.valueOf((Object) "　Y"));
        // the documented "empty CharSequence -> FALSE, not the default" special case is kept and covers blank text
        assertEquals(Boolean.FALSE, type.valueOf((Object) ""));
        assertEquals(Boolean.FALSE, type.valueOf((Object) "   "));
        assertEquals(Boolean.FALSE, type.valueOf(new StringBuilder()));
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
        // interior padding is not removed, and the result is not boolean text (code review 2026-10-05)
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("t rue".toCharArray(), 0, 5));
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
        // a lone 'N', 'n' or '0' is FALSE; any other lone non-whitespace character is rejected
        assertEquals(Boolean.FALSE, type.valueOf("N".toCharArray(), 0, 1));
        assertEquals(Boolean.FALSE, type.valueOf("0".toCharArray(), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("x".toCharArray(), 0, 1));
        // a non-breaking space (U+00A0) is neither <= ' ' nor Java whitespace, so it is not padding: both paths reject
        // it as text that is not boolean text
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(new char[] { (char) 0x00a0 }, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(String.valueOf((char) 0x00a0)));
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
        // character - a Unicode space is blank text (the default value), a C0 control is stripped to "", which is not
        // boolean text and is rejected (code review 2026-10-05). Deferring to valueOf(String) keeps both paths on the
        // same answer.
        assertNull(type.valueOf(String.valueOf(ideographicSpace)));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(String.valueOf(startOfHeading)));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(new char[] { startOfHeading }, 0, 1));

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
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("tr" + ideographicSpace + "ue"));
        // a non-breaking space is neither <= ' ' nor Character.isWhitespace: not padding, so the text is rejected
        assertThrows(IllegalArgumentException.class, () -> type.valueOf((char) 0x00a0 + "true"));

        // valueOf(Object) reads a CharSequence by the same rule (blank stays the documented FALSE)
        assertEquals(Boolean.TRUE, type.valueOf((Object) (ideographicSpace + "true")));
        assertEquals(Boolean.TRUE, type.valueOf(new StringBuilder(ideographicSpace + "Y")));
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

        // A digit 2-9 is a number other than 1, 0 and -1 (code review 2026-10-05): both overloads must reject it alike.
        assertEquals(outcome(() -> type.valueOf(str)), outcome(() -> type.valueOf(region, 1, cbuf.length)),
                () -> "char[] and String disagree on " + toEscapedString(str));
    }

    private static Object outcome(final Supplier<Boolean> call) {
        try {
            return call.get();
        } catch (final IllegalArgumentException e) {
            return e.getClass().getName() + ": " + e.getMessage();
        }
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

    private static String serialize(final java.util.function.Consumer<BufferedJsonWriter> action) {
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
        try {
            action.accept(writer);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testPrimitiveBooleanGetFromResultSet() throws Exception {
        final Type<Boolean> type = Type.of(boolean.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean(1)).thenReturn(true);
        when(rs.getBoolean("c")).thenReturn(false);
        assertEquals(Boolean.TRUE, type.get(rs, 1));
        assertEquals(Boolean.FALSE, type.get(rs, "c"));
    }

    @Test
    public void testBooleanIntType() throws Exception {
        final BooleanIntType type = new BooleanIntType();
        assertFalse(type.isCsvQuoteRequired());
        assertEquals(Boolean.FALSE, type.valueOf((Object) null));

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(0);
        when(rs.getInt("c")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        assertNull(type.get(rs, 1));
        assertNull(type.get(rs, "c"));
    }

    @Test
    public void testPrimitiveCharGetEmptyUsesDefault() throws Exception {
        final Type<Character> type = Type.of(char.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("");
        when(rs.getString("c")).thenReturn(null);
        assertEquals(type.defaultValue(), type.get(rs, 1));
        assertEquals(type.defaultValue(), type.get(rs, "c"));
    }

    @Test
    public void testOptionalCharType() throws Exception {
        final Type<OptionalChar> type = Type.of(OptionalChar.class);
        assertFalse(type.defaultValue().isPresent());

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString("c")).thenReturn("");
        assertFalse(type.get(rs, "c").isPresent());

        assertEquals("'x'", serialize(w -> {
            try {
                type.serializeTo(w, OptionalChar.of('x'), JsonSerConfig.create().setCharQuotation('\''));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
        assertEquals("\"'\"", serialize(w -> {
            try {
                type.serializeTo(w, OptionalChar.of('\''), JsonSerConfig.create().setCharQuotation('"'));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
    }

    @Test
    public void testOptionalBooleanType() throws Exception {
        final Type<OptionalBoolean> type = Type.of(OptionalBoolean.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("c")).thenReturn(1);
        assertTrue(type.get(rs, "c").get());

        assertEquals("false", serialize(w -> {
            try {
                type.serializeTo(w, OptionalBoolean.empty(), JsonSerConfig.create().setWriteNullBooleanAsFalse(true));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
    }

    @Test
    public void testOptionalFloatAndDoubleGet() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("n")).thenReturn(null);
        when(rs.getObject("d")).thenReturn(2.5d);
        when(rs.getObject("s")).thenReturn("1.5");
        when(rs.getObject("f")).thenReturn(1.5f);

        final Type<OptionalFloat> ft = Type.of(OptionalFloat.class);
        assertFalse(ft.get(rs, "n").isPresent());
        assertEquals(2.5f, ft.get(rs, "d").get());
        assertEquals(1.5f, ft.get(rs, "s").get());

        final Type<OptionalDouble> dt = Type.of(OptionalDouble.class);
        assertFalse(dt.isCsvQuoteRequired());
        assertEquals(1.5d, dt.get(rs, "f").get());
        assertEquals(2.5d, dt.get(rs, "d").get());
    }

    @Test
    public void testMutableTypesReadSqlNull() throws Exception {
        final ResultSet rs = mock(ResultSet.class);
        when(rs.wasNull()).thenReturn(true);
        assertNull(Type.of(MutableShort.class).get(rs, 1));
        assertNull(Type.of(MutableShort.class).get(rs, "c"));
        assertNull(Type.of(MutableLong.class).get(rs, 1));
        assertNull(Type.of(MutableLong.class).get(rs, "c"));
        assertNull(Type.of(MutableInt.class).get(rs, 1));
        assertNull(Type.of(MutableInt.class).get(rs, "c"));
        assertNull(Type.of(MutableFloat.class).get(rs, 1));
        assertNull(Type.of(MutableFloat.class).get(rs, "c"));
        assertNull(Type.of(MutableDouble.class).get(rs, 1));
        assertNull(Type.of(MutableDouble.class).get(rs, "c"));
        assertNull(Type.of(MutableByte.class).get(rs, 1));
        assertNull(Type.of(MutableByte.class).get(rs, "c"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testMutableTypesSerializeNullAsZero() {
        final JsonSerConfig zero = JsonSerConfig.create().setWriteNullNumberAsZero(true);
        for (final Class<?> cls : new Class<?>[] { MutableShort.class, MutableFloat.class, MutableDouble.class, MutableByte.class }) {
            final Type type = Type.of(cls);
            final String out = serialize(w -> {
                try {
                    type.serializeTo(w, null, zero);
                } catch (final java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
            assertTrue(out.startsWith("0"), cls + " -> " + out);
        }

        final Type<MutableLong> lt = Type.of(MutableLong.class);
        assertEquals("\"5\"", serialize(w -> {
            try {
                lt.serializeTo(w, MutableLong.of(5), JsonSerConfig.create().setWriteLongAsString(true));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
    }

    @Test
    public void testMutableCharType() {
        final Type<MutableChar> type = Type.of(MutableChar.class);
        assertTrue(type.isComparable());
        assertEquals("a", serialize(w -> {
            try {
                type.serializeTo(w, MutableChar.of('a'), null);
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
        assertEquals("'\\''", serialize(w -> {
            try {
                type.serializeTo(w, MutableChar.of('\''), JsonSerConfig.create().setCharQuotation('\''));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
        assertEquals("\"'\"", serialize(w -> {
            try {
                type.serializeTo(w, MutableChar.of('\''), JsonSerConfig.create().setCharQuotation('"'));
            } catch (final java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }));
    }

    @Test
    public void testHolderType() throws Exception {
        final Type<Holder<String>> type = TypeFactory.getType("Holder<String>");
        assertTrue(type.isParameterizedType());
        assertNull(type.stringOf(new Holder<>()));
        assertNull(type.valueOf((String) null).value());

        final ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("v");
        when(rs.getString("c")).thenReturn("w");
        when(rs.wasNull()).thenReturn(true);
        assertNull(type.get(rs, 1).value());
        assertNull(type.get(rs, "c").value());
    }
}
