package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class NumberType100Test extends TestBase {

    private NumberType<Integer> intNumberType;
    private NumberType<Double> doubleNumberType;
    private NumberType<Long> longNumberType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        intNumberType = (NumberType<Integer>) createType(Integer.class);
        doubleNumberType = (NumberType<Double>) createType(Double.class);
        longNumberType = (NumberType<Long>) createType(Long.class);
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testIsNumber() {
        assertTrue(intNumberType.isNumber());
        assertTrue(doubleNumberType.isNumber());
        assertTrue(longNumberType.isNumber());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(intNumberType.isNonQuotableCsvType());
        assertTrue(doubleNumberType.isNonQuotableCsvType());
        assertTrue(longNumberType.isNonQuotableCsvType());
    }

    @Test
    public void testClazz() {
        assertEquals(Integer.class, intNumberType.clazz());
        assertEquals(Double.class, doubleNumberType.clazz());
        assertEquals(Long.class, longNumberType.clazz());
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(intNumberType.valueOf(null));
        assertNull(doubleNumberType.valueOf(null));
        assertNull(longNumberType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(intNumberType.valueOf(""));
        assertNull(doubleNumberType.valueOf(""));
        assertNull(longNumberType.valueOf(""));
    }

    @Test
    public void testValueOfWithValidString() {
        assertEquals(Integer.valueOf(123), intNumberType.valueOf("123"));
        assertEquals(Double.valueOf(123.45), doubleNumberType.valueOf("123.45"));
        assertEquals(Long.valueOf(9876543210L), longNumberType.valueOf("9876543210"));
    }

    @Test
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> intNumberType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> doubleNumberType.valueOf("xyz"));
        assertThrows(NumberFormatException.class, () -> longNumberType.valueOf("not-a-number"));
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(intNumberType.stringOf(null));
        assertNull(doubleNumberType.stringOf(null));
        assertNull(longNumberType.stringOf(null));
    }

    @Test
    public void testStringOfWithValue() {
        assertEquals("123", intNumberType.stringOf(123));
        assertEquals("123.45", doubleNumberType.stringOf(123.45));
        assertEquals("9876543210", longNumberType.stringOf(9876543210L));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        intNumberType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        intNumberType.appendTo(sb, 123);
        assertEquals("123", sb.toString());

        sb = new StringBuilder();
        doubleNumberType.appendTo(sb, 123.45);
        assertEquals("123.45", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        intNumberType.writeCharacter(writer, null, config);
        verify(writer).write(new char[] { 'n', 'u', 'l', 'l' });
    }

    @Test
    public void testWriteCharacterWithValue() throws IOException {
        intNumberType.writeCharacter(writer, 123, config);
        verify(writer).writeInt(123);
        ;

        doubleNumberType.writeCharacter(writer, 123.45, config);
        verify(writer).write(123.45);
    }

    @Test
    public void testNumberTypeWithTypeName() {
        NumberType<Number> numberType = (NumberType<Number>) createType("Number");
        assertNotNull(numberType);
        assertEquals(Number.class, numberType.clazz());
    }

    @Test
    public void testSpecialNumberTypes() {
        // Test with different Number subclasses
        NumberType<Float> floatType = (NumberType<Float>) createType(Float.class);
        assertEquals(Float.valueOf(3.14f), floatType.valueOf("3.14"));
        assertEquals("3.14", floatType.stringOf(3.14f));

        NumberType<Short> shortType = (NumberType<Short>) createType(Short.class);
        assertEquals(Short.valueOf((short) 100), shortType.valueOf("100"));
        assertEquals("100", shortType.stringOf((short) 100));

        NumberType<Byte> byteType = (NumberType<Byte>) createType(Byte.class);
        assertEquals(Byte.valueOf((byte) 50), byteType.valueOf("50"));
        assertEquals("50", byteType.stringOf((byte) 50));
    }
}
