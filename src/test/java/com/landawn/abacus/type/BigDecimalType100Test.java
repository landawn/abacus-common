package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class BigDecimalType100Test extends TestBase {

    private Type<BigDecimal> bigDecimalType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        bigDecimalType = createType(BigDecimal.class);
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(BigDecimal.class, bigDecimalType.clazz());
    }

    @Test
    public void testStringOf() {
        assertNull(bigDecimalType.stringOf(null));
        assertEquals("123.456", bigDecimalType.stringOf(new BigDecimal("123.456")));
        assertEquals("-987.654", bigDecimalType.stringOf(new BigDecimal("-987.654")));
        assertEquals("0", bigDecimalType.stringOf(BigDecimal.ZERO));
        assertEquals("1E+10", bigDecimalType.stringOf(new BigDecimal("1E+10")));
    }

    @Test
    public void testValueOfString() {
        assertNull(bigDecimalType.valueOf(""));
        assertNull(bigDecimalType.valueOf((String) null));

        assertEquals(new BigDecimal("123.456"), bigDecimalType.valueOf("123.456"));
        assertEquals(new BigDecimal("-987.654"), bigDecimalType.valueOf("-987.654"));
        assertEquals(BigDecimal.ZERO, bigDecimalType.valueOf("0"));
        assertEquals(new BigDecimal("1E+10"), bigDecimalType.valueOf("1E+10"));

        assertThrows(NumberFormatException.class, () -> bigDecimalType.valueOf("abc"));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(bigDecimalType.valueOf(new char[0], 0, 0));

        char[] chars = "123.456".toCharArray();
        assertEquals(new BigDecimal("123.456"), bigDecimalType.valueOf(chars, 0, 7));

        char[] negChars = "-987.654".toCharArray();
        assertEquals(new BigDecimal("-987.654"), bigDecimalType.valueOf(negChars, 0, 8));

        char[] sciChars = "1.23E+5".toCharArray();
        assertEquals(new BigDecimal("1.23E+5"), bigDecimalType.valueOf(sciChars, 0, 7));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        BigDecimal value1 = new BigDecimal("123.456");
        BigDecimal value2 = new BigDecimal("-987.654");

        when(rs.getBigDecimal(1)).thenReturn(value1);
        when(rs.getBigDecimal(2)).thenReturn(value2);
        when(rs.getBigDecimal(3)).thenReturn(null);

        assertEquals(value1, bigDecimalType.get(rs, 1));
        assertEquals(value2, bigDecimalType.get(rs, 2));
        assertNull(bigDecimalType.get(rs, 3));

        verify(rs).getBigDecimal(1);
        verify(rs).getBigDecimal(2);
        verify(rs).getBigDecimal(3);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        BigDecimal value1 = new BigDecimal("999.999");
        BigDecimal value2 = new BigDecimal("-111.111");

        when(rs.getBigDecimal("price")).thenReturn(value1);
        when(rs.getBigDecimal("balance")).thenReturn(value2);
        when(rs.getBigDecimal("empty")).thenReturn(null);

        assertEquals(value1, bigDecimalType.get(rs, "price"));
        assertEquals(value2, bigDecimalType.get(rs, "balance"));
        assertNull(bigDecimalType.get(rs, "empty"));

        verify(rs).getBigDecimal("price");
        verify(rs).getBigDecimal("balance");
        verify(rs).getBigDecimal("empty");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        BigDecimal value = new BigDecimal("123.456");

        bigDecimalType.set(stmt, 1, value);
        verify(stmt).setBigDecimal(1, value);

        bigDecimalType.set(stmt, 2, null);
        verify(stmt).setBigDecimal(2, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        BigDecimal value = new BigDecimal("789.012");

        bigDecimalType.set(stmt, "param1", value);
        verify(stmt).setBigDecimal("param1", value);

        bigDecimalType.set(stmt, "param2", null);
        verify(stmt).setBigDecimal("param2", null);
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        bigDecimalType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterNormal() throws IOException {
        BigDecimal value = new BigDecimal("123.456");
        bigDecimalType.writeCharacter(writer, value, null);
        verify(writer).writeCharacter("123.456");
    }

    @Test
    public void testWriteCharacterWithPlainFormat() throws IOException {
        BigDecimal value = new BigDecimal("1E+10");
        when(config.writeBigDecimalAsPlain()).thenReturn(true);

        bigDecimalType.writeCharacter(writer, value, config);
        verify(writer).writeCharacter("10000000000");
    }

    @Test
    public void testWriteCharacterScientificNotation() throws IOException {
        BigDecimal value = new BigDecimal("1E+10");
        when(config.writeBigDecimalAsPlain()).thenReturn(false);

        bigDecimalType.writeCharacter(writer, value, config);
        verify(writer).writeCharacter("1E+10");
    }
}
