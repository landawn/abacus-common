package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AtomicLongType100Test extends TestBase {

    private Type<AtomicLong> atomicLongType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        atomicLongType = createType(AtomicLong.class);
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(AtomicLong.class, atomicLongType.clazz());
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(123456789L);
        when(rs.getLong(2)).thenReturn(-987654321L);

        AtomicLong result1 = atomicLongType.get(rs, 1);
        assertNotNull(result1);
        assertEquals(123456789L, result1.get());

        AtomicLong result2 = atomicLongType.get(rs, 2);
        assertNotNull(result2);
        assertEquals(-987654321L, result2.get());

        verify(rs).getLong(1);
        verify(rs).getLong(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("timestamp")).thenReturn(1234567890123L);
        when(rs.getLong("value")).thenReturn(-9876543210L);

        AtomicLong result1 = atomicLongType.get(rs, "timestamp");
        assertNotNull(result1);
        assertEquals(1234567890123L, result1.get());

        AtomicLong result2 = atomicLongType.get(rs, "value");
        assertNotNull(result2);
        assertEquals(-9876543210L, result2.get());

        verify(rs).getLong("timestamp");
        verify(rs).getLong("value");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        atomicLongType.set(stmt, 1, new AtomicLong(123456789L));
        verify(stmt).setLong(1, 123456789L);

        atomicLongType.set(stmt, 2, new AtomicLong(-987654321L));
        verify(stmt).setLong(2, -987654321L);

        atomicLongType.set(stmt, 3, null);
        verify(stmt).setLong(3, 0L);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        atomicLongType.set(stmt, "param1", new AtomicLong(1234567890123L));
        verify(stmt).setLong("param1", 1234567890123L);

        atomicLongType.set(stmt, "param2", new AtomicLong(-9876543210L));
        verify(stmt).setLong("param2", -9876543210L);

        atomicLongType.set(stmt, "param3", null);
        verify(stmt).setLong("param3", 0L);
    }

    @Test
    public void testStringOf() {
        assertNull(atomicLongType.stringOf(null));
        assertEquals("123456789", atomicLongType.stringOf(new AtomicLong(123456789L)));
        assertEquals("-987654321", atomicLongType.stringOf(new AtomicLong(-987654321L)));
        assertEquals("0", atomicLongType.stringOf(new AtomicLong(0L)));
    }

    @Test
    public void testValueOf() {
        assertNull(atomicLongType.valueOf(""));
        assertNull(atomicLongType.valueOf((String) null));

        AtomicLong value1 = atomicLongType.valueOf("123456789");
        assertNotNull(value1);
        assertEquals(123456789L, value1.get());

        AtomicLong value2 = atomicLongType.valueOf("-987654321");
        assertNotNull(value2);
        assertEquals(-987654321L, value2.get());

        assertThrows(NumberFormatException.class, () -> atomicLongType.valueOf("abc"));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        atomicLongType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        atomicLongType.appendTo(sb, new AtomicLong(123456789L));
        assertEquals("123456789", sb.toString());

        sb.setLength(0);
        atomicLongType.appendTo(sb, new AtomicLong(-987654321L));
        assertEquals("-987654321", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        atomicLongType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        atomicLongType.writeCharacter(writer, new AtomicLong(123456789L), null);
        verify(writer).write(123456789L);

        atomicLongType.writeCharacter(writer, new AtomicLong(-987654321L), config);
        verify(writer).write(-987654321L);
    }
}
