package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PrimitiveShortTypeTest extends TestBase {

    private final PrimitiveShortType primitiveShortType = new PrimitiveShortType();

    @Test
    public void testDefaultValue() {
        assertEquals(Short.valueOf((short) 0), primitiveShortType.defaultValue());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        primitiveShortType.set(stmt, 1, (short) 12345);
        verify(stmt).setShort(1, (short) 12345);

        primitiveShortType.set(stmt, 2, null);
        verify(stmt).setNull(2, java.sql.Types.SMALLINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        primitiveShortType.set(stmt, "param1", (short) 9999);
        verify(stmt).setShort("param1", (short) 9999);

        primitiveShortType.set(stmt, "param2", null);
        verify(stmt).setNull("param2", java.sql.Types.SMALLINT);
    }

    @Test
    public void test_get_ResultSet_usesPrimitiveDefaultForSqlNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getShort(1)).thenReturn((short) 0);
        when(rs.getShort("col")).thenReturn((short) 0);

        assertEquals((short) 0, primitiveShortType.get(rs, 1));
        assertEquals((short) 0, primitiveShortType.get(rs, "col"));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        primitiveShortType.appendTo(sw, (short) 12345);
        assertEquals("12345", sw.toString());

        sw = new StringWriter();
        primitiveShortType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(primitiveShortType.isCsvQuoteRequired());
    }

}
