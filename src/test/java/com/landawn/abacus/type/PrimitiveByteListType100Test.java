package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveByteListType100Test extends TestBase {

    private PrimitiveByteListType type;
    private Type<ByteList> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(ByteList.class);
        type = (PrimitiveByteListType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(ByteList.class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyList() {
        ByteList list = ByteList.of(new byte[0]);
        assertEquals("[]", type.stringOf(list));
    }

    @Test
    public void testStringOfNonEmptyList() {
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", type.stringOf(list));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
    }

    @Test
    public void testValueOfEmptyString() {
        assertNull(type.valueOf(""));
    }

    @Test
    public void testValueOfEmptyArray() {
        ByteList result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testValueOfNonEmptyArray() {
        ByteList result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals((byte) 1, result.get(0));
        assertEquals((byte) 2, result.get(1));
        assertEquals((byte) 3, result.get(2));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] bytes = { 1, 2, 3 };
        when(rs.getBytes(1)).thenReturn(bytes);

        ByteList result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetFromResultSetByIndexNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBytes(1)).thenReturn(null);

        ByteList result = type.get(rs, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] bytes = { 1, 2, 3 };
        when(rs.getBytes("column")).thenReturn(bytes);

        ByteList result = type.get(rs, "column");
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testGetFromResultSetByLabelNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBytes("column")).thenReturn(null);

        ByteList result = type.get(rs, "column");
        assertNull(result);
    }

    @Test
    public void testSetPreparedStatementByIndex() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });

        type.set(stmt, 1, list);
        verify(stmt).setBytes(1, new byte[] { 1, 2, 3 });
    }

    @Test
    public void testSetPreparedStatementByIndexNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);
        verify(stmt).setBytes(1, null);
    }

    @Test
    public void testSetCallableStatementByName() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });

        type.set(stmt, "param", list);
        verify(stmt).setBytes("param", new byte[] { 1, 2, 3 });
    }

    @Test
    public void testSetCallableStatementByNameNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param", null);
        verify(stmt).setBytes("param", null);
    }

    @Test
    public void testSetPreparedStatementWithSqlType() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });

        type.set(stmt, 1, list, java.sql.Types.BINARY);
        verify(stmt).setBytes(1, new byte[] { 1, 2, 3 });
    }

    @Test
    public void testSetPreparedStatementWithSqlTypeNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null, java.sql.Types.BINARY);
        verify(stmt).setBytes(1, null);
    }

    @Test
    public void testSetCallableStatementWithSqlType() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });

        type.set(stmt, "param", list, java.sql.Types.BINARY);
        verify(stmt).setBytes("param", new byte[] { 1, 2, 3 });
    }

    @Test
    public void testSetCallableStatementWithSqlTypeNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "param", null, java.sql.Types.BINARY);
        verify(stmt).setBytes("param", null);
    }

    @Test
    public void testAppendToNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ByteList list = ByteList.of(new byte[0]);
        type.appendTo(sb, list);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyList() throws IOException {
        StringBuilder sb = new StringBuilder();
        ByteList list = ByteList.of(new byte[] { 1, 2, 3 });
        type.appendTo(sb, list);
        assertEquals("[1, 2, 3]", sb.toString());
    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ByteList list = ByteList.of(new byte[0]);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyList() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ByteList list = ByteList.of(new byte[] { 1, 2 });
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(writer, list, config);
        verify(writer).write('[');
        verify(writer).write((byte) 1);
        verify(writer).write(", ");
        verify(writer).write((byte) 2);
        verify(writer).write(']');
    }
}
