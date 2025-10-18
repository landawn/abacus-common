package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PasswordType100Test extends TestBase {

    private PasswordType passwordType;
    private PasswordType customPasswordType;

    @BeforeEach
    public void setUp() {
        passwordType = (PasswordType) createType("Password");
        customPasswordType = (PasswordType) createType("Password(MD5)");
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull(passwordType);
        assertEquals("Password", passwordType.name());
    }

    @Test
    public void testConstructorWithAlgorithm() {
        assertNotNull(customPasswordType);
        assertEquals("Password", customPasswordType.name());
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String encryptedPassword = "encrypted_password_123";
        when(rs.getString(1)).thenReturn(encryptedPassword);

        String result = passwordType.get(rs, 1);
        assertEquals(encryptedPassword, result);
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        String result = passwordType.get(rs, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        String encryptedPassword = "encrypted_password_456";
        when(rs.getString("password")).thenReturn(encryptedPassword);

        String result = passwordType.get(rs, "password");
        assertEquals(encryptedPassword, result);
    }

    @Test
    public void testGetFromResultSetByLabelWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("password")).thenReturn(null);

        String result = passwordType.get(rs, "password");
        assertNull(result);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        String plainPassword = "mySecretPassword123";

        passwordType.set(stmt, 1, plainPassword);

        verify(stmt).setString(eq(1), argThat(encrypted -> encrypted != null && !encrypted.equals(plainPassword)));
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        passwordType.set(stmt, 1, null);

        verify(stmt).setString(eq(1), any());
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        String plainPassword = "anotherPassword456";

        passwordType.set(stmt, "pwd_param", plainPassword);

        verify(stmt).setString(eq("pwd_param"), argThat(encrypted -> encrypted != null && !encrypted.equals(plainPassword)));
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        passwordType.set(stmt, "pwd_param", null);

        verify(stmt).setString(eq("pwd_param"), any());
    }

    @Test
    public void testPasswordEncryption() throws SQLException {
        PreparedStatement stmt1 = mock(PreparedStatement.class);
        PreparedStatement stmt2 = mock(PreparedStatement.class);
        String password = "testPassword";

        passwordType.set(stmt1, 1, password);
        passwordType.set(stmt2, 1, password);

        verify(stmt1).setString(eq(1), any(String.class));
        verify(stmt2).setString(eq(1), any(String.class));
    }

    @Test
    public void testDifferentPasswordsEncryptDifferently() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        String password1 = "password1";
        String password2 = "password2";

        passwordType.set(stmt, 1, password1);
        passwordType.set(stmt, 2, password2);

        verify(stmt).setString(eq(1), argThat(s -> s != null));
        verify(stmt).setString(eq(2), argThat(s -> s != null));
    }

    @Test
    public void testIsString() {
        assertTrue(passwordType.isString());
    }

    @Test
    public void testClazz() {
        assertEquals(String.class, passwordType.clazz());
    }

    @Test
    public void testStringOf() {
        assertEquals("test", passwordType.stringOf("test"));
        assertNull(passwordType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertEquals("test", passwordType.valueOf("test"));
        assertNull(passwordType.valueOf(null));
    }
}
