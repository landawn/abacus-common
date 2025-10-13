package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.HBaseColumn;

@Tag("new-test")
public class HBaseColumnType100Test extends TestBase {

    private HBaseColumnType<String> hbaseColumnType;

    @BeforeEach
    public void setUp() {
        hbaseColumnType = (HBaseColumnType<String>) createType("HBaseColumn<String>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = hbaseColumnType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("HBaseColumn"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(HBaseColumn.class, hbaseColumnType.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = hbaseColumnType.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = hbaseColumnType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(hbaseColumnType.isGenericType());
    }

    @Test
    public void testStringOf() {
        assertNull(hbaseColumnType.stringOf(null));

        HBaseColumn<String> column = new HBaseColumn<>("value", 12345L);
        String result = hbaseColumnType.stringOf(column);
        assertNotNull(result);
        assertTrue(result.contains("12345"));
        assertTrue(result.contains(":"));
    }

    @Test
    public void testValueOf() {
        assertNull(hbaseColumnType.valueOf(null));
        assertNull(hbaseColumnType.valueOf(""));

        String input = "12345:testValue";
        HBaseColumn<String> result = hbaseColumnType.valueOf(input);
        assertNotNull(result);
        assertEquals(12345L, result.version());
    }

    @Test
    public void testGetTypeName() {
        String typeName = HBaseColumnType.getTypeName(HBaseColumn.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("HBaseColumn"));
        assertTrue(typeName.contains("String"));

        typeName = HBaseColumnType.getTypeName(HBaseColumn.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("HBaseColumn"));
        assertTrue(typeName.contains("String"));
    }
}
