package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.u.Optional;

public class HBaseColumnTypeTest extends TestBase {

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
        assertEquals(HBaseColumn.class, hbaseColumnType.javaType());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = hbaseColumnType.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = hbaseColumnType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(hbaseColumnType.isParameterizedType());
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
    public void testRoundTripNullAndReservedStringValues() {
        assertRoundTrip(null, 1L);
        assertRoundTrip("null", 2L);
        assertRoundTrip("\\null", 3L);
        assertRoundTrip("\\value", 4L);
        assertRoundTrip("\\~abacus-hbase-column:v1~N", 5L);
    }

    @Test
    public void testLegacyNullAndBackslashPayloadsRemainUnchanged() {
        assertEquals("null", hbaseColumnType.valueOf("6:null").value());
        assertEquals("\\value", hbaseColumnType.valueOf("7:\\value").value());
    }

    @Test
    public void testRoundTripNonNullValueWithNullStringRepresentation() {
        final HBaseColumnType<Optional<String>> type = (HBaseColumnType<Optional<String>>) createType("HBaseColumn<Optional<String>>");
        final HBaseColumn<Optional<String>> column = new HBaseColumn<>(Optional.empty(), 8L);

        final HBaseColumn<Optional<String>> roundTripped = type.valueOf(type.stringOf(column));

        assertNotNull(roundTripped.value());
        assertTrue(roundTripped.value().isEmpty());
    }

    private void assertRoundTrip(final String value, final long version) {
        final HBaseColumn<String> roundTripped = hbaseColumnType.valueOf(hbaseColumnType.stringOf(new HBaseColumn<>(value, version)));

        assertEquals(version, roundTripped.version());
        assertEquals(value, roundTripped.value());
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
