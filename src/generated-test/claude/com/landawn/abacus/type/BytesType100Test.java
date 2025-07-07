package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

public class BytesType100Test extends TestBase {

    private BytesType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (BytesType) createType("Bytes");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<byte[]> result = type.clazz();
        assertEquals(byte[].class, result);
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_EmptyArray() {
        byte[] bytes = new byte[0];
        String result = type.stringOf(bytes);
        Assertions.assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testStringOf_SimpleArray() {
        byte[] bytes = new byte[] { 1, 2, 3 };
        String result = type.stringOf(bytes);
        Assertions.assertNotNull(result);
        // Base64 encoding of [1, 2, 3]
        assertEquals("AQID", result);
    }

    @Test
    public void testStringOf_AllByteValues() {
        byte[] bytes = new byte[] { (byte) 0, (byte) 127, (byte) -128, (byte) -1 };
        String result = type.stringOf(bytes);
        Assertions.assertNotNull(result);
        // Verify it's a valid Base64 string
        Assertions.assertTrue(result.matches("^[A-Za-z0-9+/]*={0,2}$"));
    }

    @Test
    public void testValueOf_Null() {
        byte[] result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        byte[] result = type.valueOf("");
        Assertions.assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_ValidBase64() {
        String base64 = "AQID"; // Base64 encoding of [1, 2, 3]
        byte[] result = type.valueOf(base64);

        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 2, result[1]);
        assertEquals((byte) 3, result[2]);
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] expectedBytes = new byte[] { 10, 20, 30 };
        when(rs.getBytes(1)).thenReturn(expectedBytes);

        byte[] result = type.get(rs, 1);

        Assertions.assertArrayEquals(expectedBytes, result);
        verify(rs).getBytes(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBytes(1)).thenReturn(null);

        byte[] result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getBytes(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] expectedBytes = new byte[] { -1, 0, 1 };
        when(rs.getBytes("columnName")).thenReturn(expectedBytes);

        byte[] result = type.get(rs, "columnName");

        Assertions.assertArrayEquals(expectedBytes, result);
        verify(rs).getBytes("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };

        type.set(stmt, 1, bytes);

        verify(stmt).setBytes(1, bytes);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setBytes(1, null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        byte[] bytes = new byte[] { 100, -100 };

        type.set(stmt, "paramName", bytes);

        verify(stmt).setBytes("paramName", bytes);
    }

    @Test
    public void testSet_CallableStatement_String_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        type.set(stmt, "paramName", null);

        verify(stmt).setBytes("paramName", null);
    }

    @Test
    public void testRoundTrip() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }

        String base64 = type.stringOf(original);
        byte[] restored = type.valueOf(base64);

        Assertions.assertArrayEquals(original, restored);
    }

    @Test
    public void testLargeArray() {
        byte[] largeArray = new byte[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (byte) (i % 256);
        }

        String base64 = type.stringOf(largeArray);
        byte[] restored = type.valueOf(base64);

        Assertions.assertArrayEquals(largeArray, restored);
    }

    @Test
    public void testSpecialCases() {
        // Test with bytes that might cause issues in Base64
        byte[] special = new byte[] { 0, 1, 2, 3, 61, 62, 63, 64, 65 };

        String base64 = type.stringOf(special);
        byte[] restored = type.valueOf(base64);

        Assertions.assertArrayEquals(special, restored);
    }
}
