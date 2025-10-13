package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BigIntegerType100Test extends TestBase {

    private Type<BigInteger> bigIntegerType;

    @BeforeEach
    public void setUp() {
        bigIntegerType = createType(BigInteger.class);
    }

    @Test
    public void testClazz() {
        assertEquals(BigInteger.class, bigIntegerType.clazz());
    }

    @Test
    public void testStringOf() {
        assertNull(bigIntegerType.stringOf(null));
        assertEquals("123456789", bigIntegerType.stringOf(new BigInteger("123456789")));
        assertEquals("-987654321", bigIntegerType.stringOf(new BigInteger("-987654321")));
        assertEquals("0", bigIntegerType.stringOf(BigInteger.ZERO));
        assertEquals("1", bigIntegerType.stringOf(BigInteger.ONE));
        assertEquals("10", bigIntegerType.stringOf(BigInteger.TEN));

        String largeNumber = "12345678901234567890123456789012345678901234567890";
        assertEquals(largeNumber, bigIntegerType.stringOf(new BigInteger(largeNumber)));
    }

    @Test
    public void testValueOf() {
        assertNull(bigIntegerType.valueOf(""));
        assertNull(bigIntegerType.valueOf((String) null));

        assertEquals(new BigInteger("123456789"), bigIntegerType.valueOf("123456789"));
        assertEquals(new BigInteger("-987654321"), bigIntegerType.valueOf("-987654321"));
        assertEquals(BigInteger.ZERO, bigIntegerType.valueOf("0"));
        assertEquals(BigInteger.ONE, bigIntegerType.valueOf("1"));
        assertEquals(BigInteger.TEN, bigIntegerType.valueOf("10"));

        String largeNumber = "98765432109876543210987654321098765432109876543210";
        assertEquals(new BigInteger(largeNumber), bigIntegerType.valueOf(largeNumber));

        assertThrows(NumberFormatException.class, () -> bigIntegerType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> bigIntegerType.valueOf("12.34"));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("123456789");
        when(rs.getString(2)).thenReturn("-987654321");
        when(rs.getString(3)).thenReturn(null);
        when(rs.getString(4)).thenReturn("");

        assertEquals(new BigInteger("123456789"), bigIntegerType.get(rs, 1));
        assertEquals(new BigInteger("-987654321"), bigIntegerType.get(rs, 2));
        assertNull(bigIntegerType.get(rs, 3));
        assertNull(bigIntegerType.get(rs, 4));

        verify(rs).getString(1);
        verify(rs).getString(2);
        verify(rs).getString(3);
        verify(rs).getString(4);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("bignum")).thenReturn("999999999999999999999");
        when(rs.getString("negative")).thenReturn("-111111111111111111111");
        when(rs.getString("empty")).thenReturn(null);

        assertEquals(new BigInteger("999999999999999999999"), bigIntegerType.get(rs, "bignum"));
        assertEquals(new BigInteger("-111111111111111111111"), bigIntegerType.get(rs, "negative"));
        assertNull(bigIntegerType.get(rs, "empty"));

        verify(rs).getString("bignum");
        verify(rs).getString("negative");
        verify(rs).getString("empty");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        BigInteger value = new BigInteger("123456789012345678901234567890");

        bigIntegerType.set(stmt, 1, value);
        verify(stmt).setString(1, "123456789012345678901234567890");

        bigIntegerType.set(stmt, 2, null);
        verify(stmt).setString(2, null);

        bigIntegerType.set(stmt, 3, BigInteger.ZERO);
        verify(stmt).setString(3, "0");
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        BigInteger value = new BigInteger("-987654321098765432109876543210");

        bigIntegerType.set(stmt, "param1", value);
        verify(stmt).setString("param1", "-987654321098765432109876543210");

        bigIntegerType.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);

        bigIntegerType.set(stmt, "param3", BigInteger.ONE);
        verify(stmt).setString("param3", "1");
    }
}
