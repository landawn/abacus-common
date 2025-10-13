package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class CharacterType100Test extends TestBase {

    private CharacterType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (CharacterType) createType("Character");
        writer = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        Class<Character> result = type.clazz();
        assertEquals(Character.class, result);
    }

    @Test
    public void testIsPrimitiveWrapper() {
        boolean result = type.isPrimitiveWrapper();
        Assertions.assertTrue(result);
    }

    @Test
    public void testGet_ResultSet_Int_SingleChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("A");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('A'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_MultipleChars() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("ABC");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('A'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(null);

        Character result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_Int_SpecialChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("\n");

        Character result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('\n'), result);
        verify(rs).getString(1);
    }

    @Test
    public void testGet_ResultSet_String_SingleChar() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn("X");

        Character result = type.get(rs, "charColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('X'), result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_LongString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn("Hello World");

        Character result = type.get(rs, "charColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('H'), result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("charColumn")).thenReturn(null);

        Character result = type.get(rs, "charColumn");

        Assertions.assertNull(result);
        verify(rs).getString("charColumn");
    }

    @Test
    public void testGet_ResultSet_String_Digit() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("digitColumn")).thenReturn("7");

        Character result = type.get(rs, "digitColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('7'), result);
        verify(rs).getString("digitColumn");
    }

    @Test
    public void testGet_ResultSet_String_Unicode() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("unicodeColumn")).thenReturn("€");

        Character result = type.get(rs, "unicodeColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf('€'), result);
        verify(rs).getString("unicodeColumn");
    }

    @Test
    public void testGet_ResultSet_String_Space() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("spaceColumn")).thenReturn(" ");

        Character result = type.get(rs, "spaceColumn");

        Assertions.assertNotNull(result);
        assertEquals(Character.valueOf(' '), result);
        verify(rs).getString("spaceColumn");
    }
}
