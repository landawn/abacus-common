package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class InstantTypeTest extends TestBase {

    private InstantType instantType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        instantType = (InstantType) createType("Instant");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(Instant.class, instantType.javaType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(instantType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidInstant() {
        Instant instant = Instant.parse("2023-12-25T10:30:45.123456789Z");
        String result = instantType.stringOf(instant);
        assertEquals("2023-12-25T10:30:45.123456789Z", result);
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(instantType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        Instant result = instantType.valueOf(millis);
        assertNotNull(result);
        assertEquals(millis, result.toEpochMilli());
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25T10:30:45.123Z";
        Instant result = instantType.valueOf((Object) str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_Object_Instant_preservesNanos() {
        Instant nanos = Instant.parse("2023-12-25T10:30:45.123456789Z");
        Instant result = instantType.valueOf(nanos);
        assertEquals(nanos, result);
        assertEquals(123456789, result.getNano());
    }

    @Test
    public void testValueOf_Object_Date_and_Calendar() {
        long millis = 1703502645123L;
        Date date = new Date(millis);
        assertEquals(millis, instantType.valueOf(date).toEpochMilli());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        assertEquals(millis, instantType.valueOf(cal).toEpochMilli());
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(instantType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(instantType.valueOf(""));
    }

    @Test
    public void testValueOf_String_NullString() {
        assertNull(instantType.valueOf("null"));
        assertNull(instantType.valueOf("NULL"));
    }

    @Test
    public void testValueOf_String_SysTime() {
        Instant before = Instant.now();
        Instant result = instantType.valueOf("sysTime");
        Instant after = Instant.now();

        assertNotNull(result);
        assertTrue(result.toEpochMilli() >= before.toEpochMilli());
        assertTrue(result.toEpochMilli() <= after.toEpochMilli());
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        Instant result = instantType.valueOf(String.valueOf(millis));
        assertNotNull(result);
        assertEquals(millis, result.toEpochMilli());
    }

    @Test
    public void testValueOf_String_ISO8601DateTime() {
        String str = "2023-12-25T10:30:45Z";
        Instant result = instantType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601Timestamp() {
        String str = "2023-12-25T10:30:45.123Z";
        Instant result = instantType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_StandardFormat() {
        String str = "2023-12-25T10:30:45.123456789Z";
        Instant result = instantType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_ParsesInstantToString() {
        // Every form produced by Instant.toString() must round-trip through valueOf.
        Instant[] values = { //
                Instant.ofEpochSecond(1703502645L), // second precision, "...:45Z"
                Instant.ofEpochSecond(1703502645L, 123000000), // millis, "...45.123Z"
                Instant.ofEpochSecond(1703502645L, 123456000), // micros
                Instant.ofEpochSecond(1703502645L, 123456789), // nanos
                Instant.ofEpochSecond(1703502600L), // whole-minute instant
                Instant.EPOCH, //
                Instant.now() };

        for (Instant value : values) {
            String text = value.toString();
            Instant result = instantType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value, result, () -> "Mismatch for: " + text);
        }
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(instantType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(instantType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_NumericString() {
        long millis = 1703502645123L;
        char[] cbuf = String.valueOf(millis).toCharArray();
        Instant result = instantType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(millis, result.toEpochMilli());
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25T10:30:45.123Z";
        char[] cbuf = str.toCharArray();
        Instant result = instantType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        Instant result = instantType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.toInstant(), result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(instantType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp("instant_column")).thenReturn(timestamp);

        Instant result = instantType.get(rs, "instant_column");
        assertNotNull(result);
        assertEquals(timestamp.toInstant(), result);
    }

    @Test
    public void testGet_ResultSet_ByName_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("instant_column")).thenReturn(null);

        assertNull(instantType.get(rs, "instant_column"));
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");

        instantType.set(stmt, 1, instant);
        verify(stmt).setTimestamp(1, Timestamp.from(instant));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        instantType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");

        instantType.set(stmt, "param_name", instant);
        verify(stmt).setTimestamp("param_name", Timestamp.from(instant));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        instantType.set(stmt, "param_name", null);
        verify(stmt).setTimestamp("param_name", null);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");

        instantType.appendTo(sb, instant);
        assertTrue(sb.toString().contains("2023-12-25"));
        assertTrue(sb.toString().contains("10:30:45"));
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        instantType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        instantType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_NoConfig() throws IOException {
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");

        instantType.serializeTo(characterWriter, instant, null);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testSerializeTo_LongFormat() throws IOException {
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write(instant.toEpochMilli());
    }

    @Test
    public void testSerializeTo_ISO8601DateTime() throws IOException {
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write("2023-12-25T10:30:45Z");
    }

    @Test
    public void testSerializeTo_ISO8601Timestamp() throws IOException {
        Instant instant = Instant.parse("2023-12-25T10:30:45Z");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write("2023-12-25T10:30:45.000Z");
    }

    @Test
    public void testSerializeTo_WithQuotes() throws IOException {
        Instant instant = Instant.parse("2023-12-25T10:30:45.123Z");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter, times(2)).write('"');
        verify(characterWriter).write(anyString());
    }

    // --- review fixes 2026-09-06 (T10-01, T10-02, T10-03) ---

    @Test
    public void reviewFixes20260906_T1001_fastPathRejectsImpossibleCalendarValues() {
        // the 20/24-char 'Z' fast path resolved with SMART and silently moved Feb 30 -> Feb 28 etc.
        for (final String s : new String[] { "2023-02-30T10:30:45Z", "2023-02-30T10:30:45.123Z", "2023-04-31T10:30:45Z", "2023-02-29T00:00:00Z",
                "2023-02-29T00:00:00.000Z" }) {
            assertThrows(DateTimeParseException.class, () -> instantType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> instantType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // same exception type for the 25-char form that always went to the general parser
        assertThrows(DateTimeParseException.class, () -> instantType.valueOf("2023-02-30T10:30:45+00:00"));

        // JDK design, pinned: the STRICT fast path rejects 24:00 but Instant.parse (ISO_INSTANT) accepts it as
        // next-day midnight, so an Instant (unlike OffsetDateTime/ZonedDateTime) still yields a value here
        assertEquals(Instant.parse("2023-10-16T00:00:00Z"), instantType.valueOf("2023-10-15T24:00:00Z"));
        assertEquals(Instant.parse("2023-10-16T00:00:00Z"), instantType.valueOf("2023-10-15T24:00:00.000Z"));

        // valid forms unchanged and equal to the JDK parser
        for (final String s : new String[] { "2024-02-29T00:00:00Z", "2024-02-29T00:00:00.000Z", "0000-02-29T00:00:00Z", "0001-01-01T00:00:00.000Z",
                "9999-12-31T23:59:59.999Z", "2023-10-15T10:30:45.12Z", "2023-10-15T10:30:45.123456789Z", "2023-10-15t10:30:45z" }) {
            assertEquals(Instant.parse(s), instantType.valueOf(s), s);
            assertEquals(Instant.parse(s), instantType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        final Instant x = Instant.parse("2024-02-29T12:34:56.789Z");
        assertEquals(x, instantType.valueOf(instantType.stringOf(x)));
    }

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        // overflow used to escape as ArithmeticException; hex / type suffix were accepted (Numbers.toLong grammar)
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L",
                "1700000000000d", "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> instantType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> instantType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final Instant expected = Instant.ofEpochMilli(Long.parseLong(s));
            assertEquals(expected, instantType.valueOf(s), s);
            assertEquals(expected, instantType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // T10-03: numeric text is epoch millis only when longer than four characters
        assertEquals(Instant.ofEpochMilli(12345L), instantType.valueOf("12345"));
        assertThrows(DateTimeParseException.class, () -> instantType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> instantType.valueOf("0"));
    }
}
