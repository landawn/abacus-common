package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class FloatType100Test extends TestBase {

    private FloatType floatType;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        floatType = (FloatType) createType(Float.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Float.class, floatType.clazz());
    }

    @Test
    public void testIsPrimitiveWrapper() {
        assertTrue(floatType.isPrimitiveWrapper());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        Float expectedFloat = 3.14f;
        when(resultSet.getObject(1)).thenReturn(expectedFloat);

        Float result = floatType.get(resultSet, 1);
        assertEquals(expectedFloat, result);
        verify(resultSet).getObject(1);

        when(resultSet.getObject(2)).thenReturn(null);
        assertNull(floatType.get(resultSet, 2));

        when(resultSet.getObject(3)).thenReturn(42);
        result = floatType.get(resultSet, 3);
        assertEquals(42.0f, result);
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        Float expectedFloat = 3.14f;
        when(resultSet.getObject("floatColumn")).thenReturn(expectedFloat);

        Float result = floatType.get(resultSet, "floatColumn");
        assertEquals(expectedFloat, result);
        verify(resultSet).getObject("floatColumn");

        when(resultSet.getObject("nullColumn")).thenReturn(null);
        assertNull(floatType.get(resultSet, "nullColumn"));

        when(resultSet.getObject("intColumn")).thenReturn(42);
        result = floatType.get(resultSet, "intColumn");
        assertEquals(42.0f, result);
    }
}
