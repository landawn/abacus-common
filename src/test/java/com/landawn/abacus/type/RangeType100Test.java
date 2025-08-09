package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Range;

public class RangeType100Test extends TestBase {

    private RangeType<Integer> rangeType;

    @BeforeEach
    public void setUp() {
        rangeType = (RangeType<Integer>) createType("Range<Integer>");
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(rangeType.declaringName());
        assertTrue(rangeType.declaringName().contains("Range"));
    }

    @Test
    public void testClazz() {
        assertEquals(Range.class, rangeType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = rangeType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testGetElementType() {
        assertNotNull(rangeType.getElementType());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(rangeType.isGenericType());
    }

    @Test
    public void testStringOf() {
        // Test open-open range
        Range<Integer> openOpen = Range.open(1, 5);
        String result = rangeType.stringOf(openOpen);
        assertNotNull(result);
        assertTrue(result.startsWith("(") && result.endsWith(")"));

        // Test closed-closed range
        Range<Integer> closedClosed = Range.closed(1, 5);
        result = rangeType.stringOf(closedClosed);
        assertNotNull(result);
        assertTrue(result.startsWith("[") && result.endsWith("]"));

        // Test open-closed range
        Range<Integer> openClosed = Range.openClosed(1, 5);
        result = rangeType.stringOf(openClosed);
        assertNotNull(result);
        assertTrue(result.startsWith("(") && result.endsWith("]"));

        // Test closed-open range
        Range<Integer> closedOpen = Range.closedOpen(1, 5);
        result = rangeType.stringOf(closedOpen);
        assertNotNull(result);
        assertTrue(result.startsWith("[") && result.endsWith(")"));

        // Test null
        assertNull(rangeType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        // Test parsing open-open range
        Range<Integer> range = rangeType.valueOf("(1, 5)");
        assertNotNull(range);

        // Test parsing closed-closed range
        range = rangeType.valueOf("[1, 5]");
        assertNotNull(range);

        // Test parsing open-closed range
        range = rangeType.valueOf("(1, 5]");
        assertNotNull(range);

        // Test parsing closed-open range
        range = rangeType.valueOf("[1, 5)");
        assertNotNull(range);

        // Test null/empty string
        assertNull(rangeType.valueOf(null));
        assertNull(rangeType.valueOf(""));
        assertNull(rangeType.valueOf(" "));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        // Test with range
        Range<Integer> range = Range.closed(1, 5);
        rangeType.appendTo(writer, range);
        assertTrue(writer.toString().length() > 0);

        // Test with null
        writer = new StringWriter();
        rangeType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        // Test with range
        Range<Integer> range = Range.closed(1, 5);
        rangeType.writeCharacter(writer, range, config);

        // Test with null
        rangeType.writeCharacter(writer, null, config);
    }
}
