package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class StringBufferTypeTest extends TestBase {

    private final StringBufferType stringBufferType = new StringBufferType();

    @Test
    public void testClazz() {
        assertEquals(StringBuffer.class, stringBufferType.javaType());
    }

    @Test
    public void test_clazz() {
        assertNotNull(stringBufferType.javaType());
    }

    @Test
    public void test_stringOf() {
        // Test with null
        assertNull(stringBufferType.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = stringBufferType.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void testIsImmutable() {
        assertFalse(stringBufferType.isImmutable());
    }

    @Test
    public void test_name() {
        assertNotNull(stringBufferType.name());
        assertFalse(stringBufferType.name().isEmpty());
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        stringBufferType.appendTo(sw, null);
        assertNotNull(sw.toString());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> stringBufferType.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> stringBufferType.get(rs, "col"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> stringBufferType.set(stmt, 1, null));
    }

}
