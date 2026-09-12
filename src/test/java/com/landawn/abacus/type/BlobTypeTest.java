package com.landawn.abacus.type;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BlobTypeTest extends TestBase {

    private final BlobType type = new BlobType();

    @Test
    public void testClazz() {
        Class<Blob> result = type.javaType();
        assertEquals(Blob.class, result);
    }

    @Test
    public void testConcreteBlobSubclassMetadata() {
        final BlobType subtype = new BlobType(SerialBlob.class);

        assertEquals(SerialBlob.class, subtype.javaType());
        assertEquals("SerialBlob", subtype.name());
    }

    @Test
    public void testIsNotSerializable() {
        assertFalse(type.isSerializable());
        assertEquals(Type.SerializationType.UNKNOWN, type.serializationType());
    }

    @Test
    public void test_clazz() {
        assertNotNull(type.javaType());
    }

    @Test
    public void test_stringOf() {
        // Test with null
        assertThrows(UnsupportedOperationException.class, () -> type.stringOf(null));
    }

    @Test
    public void testStringOf() {
        Blob blob = mock(Blob.class);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.stringOf(blob));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf((String) null));
    }

    @Test
    public void testValueOf() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf("test"));
    }

    @Test
    public void testValueOf_Null() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf(null));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob expectedBlob = mock(Blob.class);
        when(rs.getBlob(1)).thenReturn(expectedBlob);

        Blob result = type.get(rs, 1);

        assertEquals(expectedBlob, result);
        verify(rs).getBlob(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob(1)).thenReturn(null);

        Blob result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getBlob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Blob expectedBlob = mock(Blob.class);
        when(rs.getBlob("columnName")).thenReturn(expectedBlob);

        Blob result = type.get(rs, "columnName");

        assertEquals(expectedBlob, result);
        verify(rs).getBlob("columnName");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBlob("columnName")).thenReturn(null);

        Blob result = type.get(rs, "columnName");

        Assertions.assertNull(result);
        verify(rs).getBlob("columnName");
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Blob blob = mock(Blob.class);

        type.set(stmt, 1, blob);

        verify(stmt).setBlob(1, blob);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setBlob(1, (Blob) null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Blob blob = mock(Blob.class);

        type.set(stmt, "paramName", blob);

        verify(stmt).setBlob("paramName", blob);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setBlob("paramName", (Blob) null);
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertNotNull(sw.toString());
    }

    // Finding 19 (2026-09-08): ClobType and NClobType were given a valueOf(Object) identity/null override,
    // BlobType was not, so it fell through to AbstractType.valueOf(Object), which routed both null and a real
    // Blob into the unconditionally throwing stringOf/valueOf(String) pair.
    @Test
    public void reviewFixes20260908_valueOfObjectBlobReturnsSameInstanceWithoutTouchingIt() throws SQLException {
        final Blob blob = mock(Blob.class);

        assertSame(blob, type.valueOf((Object) blob));

        verify(blob, never()).free();
        verify(blob, never()).length();
        verify(blob, never()).getBytes(anyLong(), anyInt());
        verify(blob, never()).getBinaryStream();
    }

    @Test
    public void reviewFixes20260908_valueOfObjectSerialBlobRemainsUsableAfterwards() throws SQLException {
        final SerialBlob sb = new SerialBlob(new byte[] { 1, 2, 3 });

        assertSame(sb, type.valueOf((Object) sb));
        assertEquals(3L, sb.length());
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, sb.getBytes(1, 3));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectNullReturnsNull() {
        Assertions.assertNull(type.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectNonBlobThrowsBeforeAnyRead() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf((Object) "x"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf((Object) 42));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf((Object) new byte[] { 1 }));
        // the String overload is unchanged
        Assertions.assertThrows(UnsupportedOperationException.class, () -> type.valueOf("x"));
    }

    @Test
    public void reviewFixes20260908_valueOfObjectSubtypeHandlerAcceptsOnlyItsOwnClass() throws SQLException {
        final BlobType serialType = new BlobType(SerialBlob.class);
        final SerialBlob sb = new SerialBlob(new byte[] { 7, 8 });
        final Blob foreign = mock(Blob.class);

        assertSame(sb, serialType.valueOf((Object) sb));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> serialType.valueOf((Object) foreign));

        verify(foreign, never()).free();
        verify(foreign, never()).length();
        assertEquals(2L, sb.length());
    }

}
