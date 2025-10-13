package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;

@Tag("new-test")
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
        assertNull(datasetType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(datasetType.valueOf(null));
        assertNull(datasetType.valueOf(""));

    }
}
