package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.DataSet;

public class DataSetType100Test extends TestBase {

    private DataSetType dataSetType;

    @BeforeEach
    public void setUp() {
        dataSetType = (DataSetType) createType(DataSet.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(DataSet.class, dataSetType.clazz());
    }

    @Test
    public void testIsDataSet() {
        assertTrue(dataSetType.isDataSet());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(dataSetType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.DATA_SET, dataSetType.getSerializationType());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(dataSetType.stringOf(null));

        // Test with actual DataSet would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(dataSetType.valueOf(null));
        assertNull(dataSetType.valueOf(""));

        // Test with actual JSON would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }
}
