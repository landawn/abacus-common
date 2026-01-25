package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractLongType100Test extends TestBase {

    private Type<Number> longType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        longType = createType("Long");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testStringOf() {
        assertNull(longType.stringOf(null));
        assertEquals("123", longType.stringOf(123L));
        assertEquals("-456", longType.stringOf(-456L));
        assertEquals("0", longType.stringOf(0L));
        assertEquals("9223372036854775807", longType.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", longType.stringOf(Long.MIN_VALUE));
    }

    @Test
    public void testValueOfObject() {
        assertEquals(null, longType.valueOf((Object) null));

        Date date = new Date(1234567890L);
        assertEquals(1234567890L, longType.valueOf(date));

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(9876543210L);
        assertEquals(9876543210L, longType.valueOf(calendar));

        Instant instant = Instant.ofEpochMilli(1111111111L);
        assertEquals(1111111111L, longType.valueOf(instant));

        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertEquals(zdt.toInstant().toEpochMilli(), longType.valueOf(zdt));

        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        assertEquals(Timestamp.valueOf(ldt).getTime(), longType.valueOf(ldt));

        assertEquals(123L, longType.valueOf("123"));
    }

    @Test
    public void testValueOfString() {
        assertEquals(null, longType.valueOf(""));
        assertEquals(null, longType.valueOf((String) null));
        assertEquals(123L, longType.valueOf("123"));
        assertEquals(-456L, longType.valueOf("-456"));
        assertEquals(789L, longType.valueOf("789L"));
        assertEquals(100L, longType.valueOf("100l"));
        assertEquals(200L, longType.valueOf("200F"));
        assertEquals(300L, longType.valueOf("300f"));
        assertEquals(400L, longType.valueOf("400D"));
        assertEquals(500L, longType.valueOf("500d"));

        assertThrows(NumberFormatException.class, () -> longType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> longType.valueOf("12.34"));
    }

    @Test
    public void testValueOfCharArray() {
        assertEquals(null, longType.valueOf(null, 0, 0));
        assertEquals(null, longType.valueOf(new char[0], 0, 0));

        char[] chars = "123456789012".toCharArray();
        assertEquals(123456789012L, longType.valueOf(chars, 0, 12));
        assertEquals(3456789L, longType.valueOf(chars, 2, 7));
        assertEquals(2L, longType.valueOf(chars, 11, 1));

        char[] negChars = "-987654321".toCharArray();
        assertEquals(-987654321L, longType.valueOf(negChars, 0, 10));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(123456789L);
        when(rs.getObject(2)).thenReturn(-987654321L);

        assertEquals(123456789L, longType.get(rs, 1));
        assertEquals(-987654321L, longType.get(rs, 2));

        verify(rs).getObject(1);
        verify(rs).getObject(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id")).thenReturn(789012345L);
        when(rs.getObject("timestamp")).thenReturn(-100200300L);

        assertEquals(789012345L, longType.get(rs, "id"));
        assertEquals(-100200300L, longType.get(rs, "timestamp"));

        verify(rs).getObject("id");
        verify(rs).getObject("timestamp");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        longType.set(stmt, 1, null);
        verify(stmt).setNull(1, Types.BIGINT);

        longType.set(stmt, 2, 123456789L);
        verify(stmt).setLong(2, 123456789L);

        longType.set(stmt, 3, -987654321L);
        verify(stmt).setLong(3, -987654321L);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        longType.set(stmt, "param1", null);
        verify(stmt).setNull("param1", Types.BIGINT);

        longType.set(stmt, "param2", 789012345L);
        verify(stmt).setLong("param2", 789012345L);

        longType.set(stmt, "param3", -100200300L);
        verify(stmt).setLong("param3", -100200300L);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        longType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        longType.appendTo(sb, 123456789L);
        assertEquals("123456789", sb.toString());

        sb.setLength(0);
        longType.appendTo(sb, -987654321L);
        assertEquals("-987654321", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        longType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        longType.writeCharacter(writer, 123456789L, null);
        verify(writer).write(123456789L);

        when(config.writeNullNumberAsZero()).thenReturn(true);
        longType.writeCharacter(writer, null, config);
        verify(writer).write(0L);

        when(config.writeLongAsString()).thenReturn(true);
        when(config.getStringQuotation()).thenReturn('"');
        longType.writeCharacter(writer, 987654321L, config);
        verify(writer).write(987654321L);
    }
}
