package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;

public class DatasetType100Test extends TestBase {

    private DatasetType datasetType;

    @BeforeEach
    public void setUp() {
        datasetType = (DatasetType) createType(Dataset.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Dataset.class, datasetType.clazz());
    }

    @Test
    public void testIsDataset() {
        assertTrue(datasetType.isDataset());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(datasetType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.DATA_SET, datasetType.getSerializationType());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(datasetType.stringOf(null));

        // Test with actual Dataset would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(datasetType.valueOf(null));
        assertNull(datasetType.valueOf(""));

        // Test with actual JSON would require mocking Utils.jsonParser
        // Since the implementation depends on external parser
    }
}
