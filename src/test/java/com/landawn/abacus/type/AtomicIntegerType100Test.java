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
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AtomicIntegerType100Test extends TestBase {

    private Type<AtomicInteger> atomicIntegerType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        atomicIntegerType = createType(AtomicInteger.class);
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(AtomicInteger.class, atomicIntegerType.clazz());
    }

    @Test
    public void testStringOf() {
        assertNull(atomicIntegerType.stringOf(null));
        assertEquals("123", atomicIntegerType.stringOf(new AtomicInteger(123)));
        assertEquals("-456", atomicIntegerType.stringOf(new AtomicInteger(-456)));
        assertEquals("0", atomicIntegerType.stringOf(new AtomicInteger(0)));
    }

    @Test
    public void testValueOf() {
        assertNull(atomicIntegerType.valueOf(""));
        assertNull(atomicIntegerType.valueOf((String) null));

        AtomicInteger value1 = atomicIntegerType.valueOf("123");
        assertNotNull(value1);
        assertEquals(123, value1.get());

        AtomicInteger value2 = atomicIntegerType.valueOf("-456");
        assertNotNull(value2);
        assertEquals(-456, value2.get());

        assertThrows(NumberFormatException.class, () -> atomicIntegerType.valueOf("abc"));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(123);
        when(rs.getInt(2)).thenReturn(-456);

        AtomicInteger result1 = atomicIntegerType.get(rs, 1);
        assertNotNull(result1);
        assertEquals(123, result1.get());

        AtomicInteger result2 = atomicIntegerType.get(rs, 2);
        assertNotNull(result2);
        assertEquals(-456, result2.get());

        verify(rs).getInt(1);
        verify(rs).getInt(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("count")).thenReturn(789);
        when(rs.getInt("value")).thenReturn(-100);

        AtomicInteger result1 = atomicIntegerType.get(rs, "count");
        assertNotNull(result1);
        assertEquals(789, result1.get());

        AtomicInteger result2 = atomicIntegerType.get(rs, "value");
        assertNotNull(result2);
        assertEquals(-100, result2.get());

        verify(rs).getInt("count");
        verify(rs).getInt("value");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        atomicIntegerType.set(stmt, 1, new AtomicInteger(123));
        verify(stmt).setInt(1, 123);

        atomicIntegerType.set(stmt, 2, new AtomicInteger(-456));
        verify(stmt).setInt(2, -456);

        atomicIntegerType.set(stmt, 3, null);
        verify(stmt).setInt(3, 0);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        atomicIntegerType.set(stmt, "param1", new AtomicInteger(789));
        verify(stmt).setInt("param1", 789);

        atomicIntegerType.set(stmt, "param2", new AtomicInteger(-100));
        verify(stmt).setInt("param2", -100);

        atomicIntegerType.set(stmt, "param3", null);
        verify(stmt).setInt("param3", 0);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        atomicIntegerType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        atomicIntegerType.appendTo(sb, new AtomicInteger(123));
        assertEquals("123", sb.toString());

        sb.setLength(0);
        atomicIntegerType.appendTo(sb, new AtomicInteger(-456));
        assertEquals("-456", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        atomicIntegerType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        atomicIntegerType.writeCharacter(writer, new AtomicInteger(123), null);
        verify(writer).writeInt(123);

        atomicIntegerType.writeCharacter(writer, new AtomicInteger(-456), config);
        verify(writer).writeInt(-456);
    }
}
