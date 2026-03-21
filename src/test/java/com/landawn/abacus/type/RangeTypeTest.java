package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Range;

public class RangeTypeTest extends TestBase {

    private final RangeType rangeType = new RangeType("Integer");

    @Test
    public void test_clazz() {
        assertEquals(Range.class, rangeType.javaType());
    }

    @Test
    public void test_stringOf() {
        Range<Integer> range = Range.openClosed(1, 10);
        assertNotNull(rangeType.stringOf(range));
        assertNull(rangeType.stringOf(null));
    }

    @Test
    public void test_stringOf_AllBoundTypes() {
        assertNotNull(rangeType.stringOf(Range.open(1, 10)));
        assertNotNull(rangeType.stringOf(Range.openClosed(1, 10)));
        assertNotNull(rangeType.stringOf(Range.closedOpen(1, 10)));
        assertNotNull(rangeType.stringOf(Range.closed(1, 10)));
    }

    @Test
    public void test_valueOf_String() {
        assertNull(rangeType.valueOf((String) null));
        assertNull(rangeType.valueOf(""));
    }

    @Test
    public void test_valueOf_AllBoundTypes() {
        // OPEN_OPEN
        Range<Integer> result = rangeType.valueOf("(1, 10)");
        assertNotNull(result);

        // OPEN_CLOSED
        result = rangeType.valueOf("(1, 10]");
        assertNotNull(result);

        // CLOSED_OPEN
        result = rangeType.valueOf("[1, 10)");
        assertNotNull(result);

        // CLOSED
        result = rangeType.valueOf("[1, 10]");
        assertNotNull(result);
    }

    @Test
    public void test_valueOf_rejectsMalformedEndpointCount() {
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[1]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[1,2,3]"));
    }

    @Test
    public void test_valueOf_rejectsInvalidBoundDelimiters() {
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("{1,2]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[1,2}"));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Range<Integer> range = Range.openClosed(1, 10);
        rangeType.appendTo(sw, range);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        rangeType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_appendTo_AllBoundTypes() throws IOException {
        // OPEN_OPEN
        StringWriter sw = new StringWriter();
        rangeType.appendTo(sw, Range.open(1, 10));
        assertTrue(sw.toString().startsWith("("));
        assertTrue(sw.toString().endsWith(")"));

        // OPEN_CLOSED
        sw = new StringWriter();
        rangeType.appendTo(sw, Range.openClosed(1, 10));
        assertTrue(sw.toString().startsWith("("));
        assertTrue(sw.toString().endsWith("]"));

        // CLOSED_OPEN
        sw = new StringWriter();
        rangeType.appendTo(sw, Range.closedOpen(1, 10));
        assertTrue(sw.toString().startsWith("["));
        assertTrue(sw.toString().endsWith(")"));

        // CLOSED
        sw = new StringWriter();
        rangeType.appendTo(sw, Range.closed(1, 10));
        assertTrue(sw.toString().startsWith("["));
        assertTrue(sw.toString().endsWith("]"));
    }

    @Test
    public void test_appendTo_WithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        rangeType.appendTo(sb, Range.closed(1, 10));
        assertNotNull(sb.toString());
        assertTrue(sb.toString().startsWith("["));
        assertTrue(sb.toString().endsWith("]"));
    }

    @Test
    public void test_writeCharacter_AllBoundTypes() throws IOException {
        var writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        assertDoesNotThrow(() -> rangeType.writeCharacter(writer, Range.open(1, 10), null));
        assertDoesNotThrow(() -> rangeType.writeCharacter(writer, Range.openClosed(1, 10), null));
        assertDoesNotThrow(() -> rangeType.writeCharacter(writer, Range.closedOpen(1, 10), null));
        assertDoesNotThrow(() -> rangeType.writeCharacter(writer, Range.closed(1, 10), null));
        assertDoesNotThrow(() -> rangeType.writeCharacter(writer, null, null));
    }

    @Test
    public void test_name() {
        assertEquals("Range<Integer>", rangeType.name());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        assertDoesNotThrow(() -> rangeType.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        assertDoesNotThrow(() -> rangeType.get(rs, "col"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        assertDoesNotThrow(() -> rangeType.set(stmt, 1, null));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        assertDoesNotThrow(() -> rangeType.set(stmt, "param", null));
    }

}
