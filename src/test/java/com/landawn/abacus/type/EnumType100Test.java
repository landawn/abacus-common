package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class EnumType100Test extends TestBase {

    private enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    private EnumType<TestEnum> enumTypeByName;
    private EnumType<TestEnum> enumTypeByOrdinal;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        enumTypeByName = (EnumType<TestEnum>) createType(TestEnum.class.getName());
        enumTypeByOrdinal = (EnumType<TestEnum>) createType(TestEnum.class.getName() + "(true)");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testEnumerated() {
        assertEquals(EnumBy.NAME, enumTypeByName.enumerated());
        assertEquals(EnumBy.ORDINAL, enumTypeByOrdinal.enumerated());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(enumTypeByName.isSerializable());
        assertTrue(enumTypeByOrdinal.isSerializable());
    }

    @Test
    public void testIsImmutable() {
        assertTrue(enumTypeByName.isImmutable());
        assertTrue(enumTypeByOrdinal.isImmutable());
    }

    @Test
    public void testStringOf() {
        assertEquals("VALUE1", enumTypeByName.stringOf(TestEnum.VALUE1));
        assertEquals("VALUE2", enumTypeByName.stringOf(TestEnum.VALUE2));
        assertNull(enumTypeByName.stringOf(null));
    }

    @Test
    public void testValueOfString() {
        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf("VALUE1"));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf("VALUE2"));

        assertNull(enumTypeByName.valueOf((String) null));
        assertNull(enumTypeByName.valueOf(""));

        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf("0"));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf("1"));

        assertThrows(IllegalArgumentException.class, () -> enumTypeByName.valueOf("INVALID"));
    }

    @Test
    public void testValueOfInt() {
        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf(0));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf(1));
        assertEquals(TestEnum.VALUE3, enumTypeByName.valueOf(2));

        assertThrows(IllegalArgumentException.class, () -> enumTypeByName.valueOf(99));

    }

    @Test
    public void testGetByColumnIndexWithName() throws SQLException {
        when(resultSet.getString(1)).thenReturn("VALUE1");
        assertEquals(TestEnum.VALUE1, enumTypeByName.get(resultSet, 1));
        verify(resultSet).getString(1);
    }

    @Test
    public void testGetByColumnIndexWithOrdinal() throws SQLException {
        when(resultSet.getInt(1)).thenReturn(1);
        assertEquals(TestEnum.VALUE2, enumTypeByOrdinal.get(resultSet, 1));
        verify(resultSet).getInt(1);
    }

    @Test
    public void testGetByColumnLabelWithName() throws SQLException {
        when(resultSet.getString("enumColumn")).thenReturn("VALUE2");
        assertEquals(TestEnum.VALUE2, enumTypeByName.get(resultSet, "enumColumn"));
        verify(resultSet).getString("enumColumn");
    }

    @Test
    public void testGetByColumnLabelWithOrdinal() throws SQLException {
        when(resultSet.getInt("enumColumn")).thenReturn(2);
        assertEquals(TestEnum.VALUE3, enumTypeByOrdinal.get(resultSet, "enumColumn"));
        verify(resultSet).getInt("enumColumn");
    }

    @Test
    public void testSetPreparedStatementWithName() throws SQLException {
        enumTypeByName.set(preparedStatement, 1, TestEnum.VALUE1);
        verify(preparedStatement).setString(1, "VALUE1");

        enumTypeByName.set(preparedStatement, 2, null);
        verify(preparedStatement).setString(2, null);
    }

    @Test
    public void testSetPreparedStatementWithOrdinal() throws SQLException {
        enumTypeByOrdinal.set(preparedStatement, 1, TestEnum.VALUE2);
        verify(preparedStatement).setInt(1, 1);

        enumTypeByOrdinal.set(preparedStatement, 2, null);
        verify(preparedStatement).setInt(2, 0);
    }

    @Test
    public void testSetCallableStatementWithName() throws SQLException {
        enumTypeByName.set(callableStatement, "enumParam", TestEnum.VALUE3);
        verify(callableStatement).setString("enumParam", "VALUE3");

        enumTypeByName.set(callableStatement, "nullParam", null);
        verify(callableStatement).setString("nullParam", null);
    }

    @Test
    public void testSetCallableStatementWithOrdinal() throws SQLException {
        enumTypeByOrdinal.set(callableStatement, "enumParam", TestEnum.VALUE1);
        verify(callableStatement).setInt("enumParam", 0);

        enumTypeByOrdinal.set(callableStatement, "nullParam", null);
        verify(callableStatement).setInt("nullParam", 0);
    }

    @Test
    public void testWriteCharacter() throws IOException {
        enumTypeByName.writeCharacter(characterWriter, null, config);

        enumTypeByName.writeCharacter(characterWriter, TestEnum.VALUE1, config);

        enumTypeByOrdinal.writeCharacter(characterWriter, TestEnum.VALUE2, config);

        when(config.getStringQuotation()).thenReturn('"');
        enumTypeByName.writeCharacter(characterWriter, TestEnum.VALUE3, config);
    }
}
