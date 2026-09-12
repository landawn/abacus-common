package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.util.Range;

public class RangeTypeTest extends TestBase {

    private final RangeType<Integer> rangeType = new RangeType("Integer");
    private final RangeType<String> stringRangeType = new RangeType("String");

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
    public void test_stringOf_StringEndpointsAreJsonSafe() {
        final Range<String> range = Range.openClosed("a,b", "x\"y]");

        assertEquals("(\"a,b\", \"x\\\"y]\"]", stringRangeType.stringOf(range));
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
    public void test_valueOf_roundTripsQuotedStringEndpoints() {
        final Range<String> range = Range.closedOpen("a,b", "x\"y]");
        final String serialized = stringRangeType.stringOf(range);
        final Range<String> result = stringRangeType.valueOf(serialized);

        assertEquals(Range.BoundType.CLOSED_OPEN, result.boundType());
        assertEquals("a,b", result.lowerEndpoint());
        assertEquals("x\"y]", result.upperEndpoint());
    }

    @Test
    public void test_valueOf_RawRangeUsesComparableEndpoints() {
        final Type<Range> rawRangeType = TypeFactory.getType(Range.class);
        final Range<?> result = rawRangeType.valueOf("[1, 10]");

        assertEquals(1, result.lowerEndpoint());
        assertEquals(10, result.upperEndpoint());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_valueOf_PrimitiveEndpointTypeUsesWrapperArray() {
        final Type<Range<Integer>> primitiveRangeType = TypeFactory.getType("Range<int>");
        final Range<Integer> result = primitiveRangeType.valueOf("[1, 10]");

        assertEquals(1, result.lowerEndpoint());
        assertEquals(10, result.upperEndpoint());
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
    public void test_appendTo_StringEndpointsMatchesStringOf() throws IOException {
        final Range<String> range = Range.openClosed("a,b", "x\"y]");
        final StringWriter sw = new StringWriter();

        stringRangeType.appendTo(sw, range);

        assertEquals(stringRangeType.stringOf(range), sw.toString());
    }

    @Test
    public void test_serializeTo_AllBoundTypes() throws IOException {
        var writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        assertDoesNotThrow(() -> rangeType.serializeTo(writer, Range.open(1, 10), null));
        assertDoesNotThrow(() -> rangeType.serializeTo(writer, Range.openClosed(1, 10), null));
        assertDoesNotThrow(() -> rangeType.serializeTo(writer, Range.closedOpen(1, 10), null));
        assertDoesNotThrow(() -> rangeType.serializeTo(writer, Range.closed(1, 10), null));
        assertDoesNotThrow(() -> rangeType.serializeTo(writer, null, null));
    }

    @Test
    public void test_serializeTo_StringEndpointsMatchesStringOf() throws IOException {
        final var writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        final Range<String> range = Range.closed("a,b", "x\"y]");

        stringRangeType.serializeTo(writer, range, null);

        assertTrue(writer.toString().contains("\\\"a,b\\\""));
        assertTrue(writer.toString().contains("\\\"x\\\\\\\"y]\\\""));
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

    // --- review fixes 2026-09-06 (T10-05, T10-09) ---

    @Test
    public void reviewFixes20260906_T1005_instantEndpointsRoundTripToMillisOnly() {
        // documented: instant-bearing endpoints are written as epoch millis with the default config
        final Type<Range<Instant>> type = TypeFactory.getType("Range<Instant>");
        final Instant lo = Instant.ofEpochSecond(1700000000L, 123456789);
        final Instant hi = Instant.ofEpochSecond(1700000001L, 987654321);

        assertEquals("[1700000000123, 1700000001987]", type.stringOf(Range.closed(lo, hi)));

        final Range<Instant> back = type.valueOf(type.stringOf(Range.closed(lo, hi)));
        assertEquals(Instant.ofEpochMilli(1700000000123L), back.lowerEndpoint());
        assertEquals(Instant.ofEpochMilli(1700000001987L), back.upperEndpoint());
        assertEquals(Range.BoundType.CLOSED_CLOSED, back.boundType());
        assertNotEquals(Range.closed(lo, hi), back); // nanos lost, as documented

        final Range<Instant> millisOnly = Range.openClosed(Instant.ofEpochMilli(1700000000123L), Instant.ofEpochMilli(1700000001987L));
        assertEquals(millisOnly, type.valueOf(type.stringOf(millisOnly)));

        // a local endpoint is written as ISO text and round-trips exactly
        final Type<Range<LocalDateTime>> localType = TypeFactory.getType("Range<LocalDateTime>");
        final Range<LocalDateTime> local = Range.closedOpen(LocalDateTime.of(2023, 1, 1, 10, 30, 45, 123456789), LocalDateTime.of(2023, 1, 2, 10, 30));
        assertEquals(local, localType.valueOf(localType.stringOf(local)));

        assertNull(type.stringOf(null));
    }

    @Test
    public void reviewFixes20260906_T1009_endpointParseFailuresSurfaceElementHandlerExceptions() {
        // Unquoted decimals use numeric coercion; quoted fractions reach the strict text handler.
        assertEquals(Range.closed(1, 2), rangeType.valueOf("[1.5, 2]"));
        assertEquals(Range.closed(-1, 2), rangeType.valueOf("[-1.9, 2.9]"));
        assertEquals(Range.closed(Integer.MIN_VALUE, Integer.MAX_VALUE), rangeType.valueOf("[-2147483648, 2147483647]"));
        assertThrows(NumberFormatException.class, () -> rangeType.valueOf("[\"1.5\", 2]"));
        assertThrows(NumberFormatException.class, () -> rangeType.valueOf("[\"bad\", 2]"));
        assertThrows(ArithmeticException.class, () -> rangeType.valueOf("[2147483648, 2147483649]"));
        assertThrows(ParsingException.class, () -> rangeType.valueOf("[1 5]"));

        // Range's own validation stays IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[5, 1]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[null, 5]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[,]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[1]"));
        assertThrows(IllegalArgumentException.class, () -> rangeType.valueOf("[1, 2, 3]"));

        // a nested endpoint list is unwrapped, not rejected (documented)
        assertEquals(Range.closed(1, 5), rangeType.valueOf("[[1, 5]]"));
        assertEquals(Range.closed("", "\u00e9\ud83d\ude42"), stringRangeType.valueOf("[\"\", \"\u00e9\ud83d\ude42\"]"));
        assertNull(rangeType.valueOf((String) null));
        assertNull(rangeType.valueOf(""));
        assertNull(rangeType.valueOf(" "));
    }

}
