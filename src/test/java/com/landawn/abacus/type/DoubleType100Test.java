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
public class DoubleType100Test extends TestBase {

    private DoubleType doubleType;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        doubleType = (DoubleType) createType(Double.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Double.class, doubleType.clazz());
    }

    @Test
    public void testIsPrimitiveWrapper() {
        assertTrue(doubleType.isPrimitiveWrapper());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        Double expectedDouble = 3.14;
        when(resultSet.getObject(1)).thenReturn(expectedDouble);

        Double result = doubleType.get(resultSet, 1);
        assertEquals(expectedDouble, result);
        verify(resultSet).getObject(1);

        when(resultSet.getObject(2)).thenReturn(null);
        assertNull(doubleType.get(resultSet, 2));

        when(resultSet.getObject(3)).thenReturn(42);
        result = doubleType.get(resultSet, 3);
        assertEquals(42.0, result);
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        Double expectedDouble = 3.14;
        when(resultSet.getObject("doubleColumn")).thenReturn(expectedDouble);

        Double result = doubleType.get(resultSet, "doubleColumn");
        assertEquals(expectedDouble, result);
        verify(resultSet).getObject("doubleColumn");

        when(resultSet.getObject("nullColumn")).thenReturn(null);
        assertNull(doubleType.get(resultSet, "nullColumn"));

        when(resultSet.getObject("intColumn")).thenReturn(42);
        result = doubleType.get(resultSet, "intColumn");
        assertEquals(42.0, result);
    }
}
