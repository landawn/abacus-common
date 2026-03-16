package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AtomicBooleanTypeTest extends TestBase {

    private final AtomicBooleanType type = new AtomicBooleanType();

    @Test
    public void test_clazz() {
        assertEquals(AtomicBoolean.class, type.javaType());
    }

    @Test
    public void test_name() {
        assertEquals("AtomicBoolean", type.name());
    }

    @Test
    public void test_stringOf() {
        assertEquals("true", type.stringOf(new AtomicBoolean(true)));
        assertEquals("false", type.stringOf(new AtomicBoolean(false)));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        assertEquals(true, type.valueOf("true").get());
        assertEquals(false, type.valueOf("false").get());
        assertEquals(false, type.valueOf("anything").get());
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with true
        when(rs.getBoolean(1)).thenReturn(true);
        assertEquals(true, type.get(rs, 1).get());

        // Test with false
        when(rs.getBoolean(2)).thenReturn(false);
        assertEquals(false, type.get(rs, 2).get());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);

        // Test with true
        when(rs.getBoolean("boolCol")).thenReturn(true);
        assertEquals(true, type.get(rs, "boolCol").get());

        // Test with false
        when(rs.getBoolean("falseCol")).thenReturn(false);
        assertEquals(false, type.get(rs, "falseCol").get());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        // Test with true
        type.set(stmt, 1, new AtomicBoolean(true));
        verify(stmt).setBoolean(1, true);

        // Test with false
        type.set(stmt, 2, new AtomicBoolean(false));
        verify(stmt).setBoolean(2, false);

        // Test with null (sets SQL NULL)
        type.set(stmt, 3, null);
        verify(stmt).setNull(3, java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        // Test with true
        type.set(stmt, "param1", new AtomicBoolean(true));
        verify(stmt).setBoolean("param1", true);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.BOOLEAN);
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();

        // Test true
        type.appendTo(sw, new AtomicBoolean(true));
        assertEquals("true", sw.toString());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_defaultValue() {
        assertNull(type.defaultValue());
    }

}
