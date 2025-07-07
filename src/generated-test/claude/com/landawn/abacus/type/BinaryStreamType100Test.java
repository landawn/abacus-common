package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BinaryStreamType100Test extends TestBase {

    private Type<InputStream> binaryStreamType;

    @BeforeEach
    public void setUp() {
        binaryStreamType = createType("BinaryStream");
    }

    @Test
    public void testTypeName() {
        assertEquals("BinaryStream", binaryStreamType.name());
    }

    // Note: BinaryStreamType inherits all functionality from InputStreamType,
    // so the actual behavior is tested through InputStreamType tests.
    // BinaryStreamType only provides a different type name constant.
}
