package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AtomicBooleanType100Test extends TestBase {

    private Type<AtomicBoolean> atomicBooleanType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        atomicBooleanType = createType(AtomicBoolean.class);
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(AtomicBoolean.class, atomicBooleanType.clazz());
    }

    @Test
    public void testStringOf() {
        assertNull(atomicBooleanType.stringOf(null));
        assertEquals("true", atomicBooleanType.stringOf(new AtomicBoolean(true)));
        assertEquals("false", atomicBooleanType.stringOf(new AtomicBoolean(false)));
    }

    @Test
    public void testValueOf() {
        assertNull(atomicBooleanType.valueOf(""));
        assertNull(atomicBooleanType.valueOf((String) null));

        AtomicBoolean trueValue = atomicBooleanType.valueOf("true");
        assertNotNull(trueValue);
        assertTrue(trueValue.get());

        AtomicBoolean falseValue = atomicBooleanType.valueOf("false");
        assertNotNull(falseValue);
        assertFalse(falseValue.get());

        AtomicBoolean anyValue = atomicBooleanType.valueOf("anything");
        assertNotNull(anyValue);
        assertFalse(anyValue.get());
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean(1)).thenReturn(true);
        when(rs.getBoolean(2)).thenReturn(false);

        AtomicBoolean result1 = atomicBooleanType.get(rs, 1);
        assertNotNull(result1);
        assertTrue(result1.get());

        AtomicBoolean result2 = atomicBooleanType.get(rs, 2);
        assertNotNull(result2);
        assertFalse(result2.get());

        verify(rs).getBoolean(1);
        verify(rs).getBoolean(2);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("active")).thenReturn(true);
        when(rs.getBoolean("disabled")).thenReturn(false);

        AtomicBoolean result1 = atomicBooleanType.get(rs, "active");
        assertNotNull(result1);
        assertTrue(result1.get());

        AtomicBoolean result2 = atomicBooleanType.get(rs, "disabled");
        assertNotNull(result2);
        assertFalse(result2.get());

        verify(rs).getBoolean("active");
        verify(rs).getBoolean("disabled");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        atomicBooleanType.set(stmt, 1, new AtomicBoolean(true));
        verify(stmt).setBoolean(1, true);

        atomicBooleanType.set(stmt, 2, new AtomicBoolean(false));
        verify(stmt).setBoolean(2, false);

        atomicBooleanType.set(stmt, 3, null);
        verify(stmt).setBoolean(3, false);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        atomicBooleanType.set(stmt, "param1", new AtomicBoolean(true));
        verify(stmt).setBoolean("param1", true);

        atomicBooleanType.set(stmt, "param2", new AtomicBoolean(false));
        verify(stmt).setBoolean("param2", false);

        atomicBooleanType.set(stmt, "param3", null);
        verify(stmt).setBoolean("param3", false);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        atomicBooleanType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        atomicBooleanType.appendTo(sb, new AtomicBoolean(true));
        assertEquals("true", sb.toString());

        sb.setLength(0);
        atomicBooleanType.appendTo(sb, new AtomicBoolean(false));
        assertEquals("false", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        atomicBooleanType.writeCharacter(writer, null, null);
        verify(writer).append("null");

        atomicBooleanType.writeCharacter(writer, new AtomicBoolean(true), null);
        verify(writer).append("true");

        atomicBooleanType.writeCharacter(writer, new AtomicBoolean(false), config);
        verify(writer).append("false");
    }
}
