package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;

@Tag("2025")
public class DurationTypeTest extends TestBase {

    private final DurationType type = new DurationType();

    @Test
    public void test_clazz() {
        assertEquals(Duration.class, type.javaType());
    }

    @Test
    public void test_name() {
        assertEquals("Duration", type.name());
    }

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }

    @Test
    public void test_stringOf() {
        Duration duration = Duration.ofMillis(5000);
        assertNotNull(type.stringOf(duration));
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(5000L);

        Duration result = type.get(rs, 1);
        assertNotNull(result);
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("duration")).thenReturn(10000L);

        Duration result = type.get(rs, "duration");
        assertNotNull(result);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        Duration duration = Duration.ofMillis(5000);
        type.set(stmt, "param", duration);
        verify(stmt).setLong(eq("param"), anyLong());

        type.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.BIGINT);
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        Duration duration = Duration.ofMillis(5000);
        type.set(stmt, 1, duration);
        verify(stmt).setLong(1, 5000L);

        type.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.BIGINT);
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Duration duration = Duration.ofMillis(5000);
        type.appendTo(sw, duration);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

}
