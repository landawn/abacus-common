package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class NumberTypeTest extends TestBase {

    public static final class BoxedFactoryNumber extends Number {
        private final int value;

        private BoxedFactoryNumber(final int value) {
            this.value = value;
        }

        public static BoxedFactoryNumber of(final Integer value) {
            return new BoxedFactoryNumber(value);
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    @Test
    public void testFactoryAcceptsBoxedPrimitiveValue() {
        final NumberType<BoxedFactoryNumber> type = new NumberType<>(BoxedFactoryNumber.class);
        assertEquals(42, type.valueOf("42").intValue());
        assertEquals(Integer.MIN_VALUE, type.valueOf(Integer.toString(Integer.MIN_VALUE)).intValue());
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void testCopyConstructorDoesNotRecursivelyResolveItsOwnType() {
        final Type<CopyConstructorNumber> type = Type.of(CopyConstructorNumber.class);
        assertEquals(CopyConstructorNumber.class, type.javaType());
        assertNull(type.valueOf((String) null));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("12"));
    }

    @Test
    public void testCopyFactoryDoesNotRecursivelyResolveItsOwnType() {
        final Type<CopyFactoryNumber> type = Type.of(CopyFactoryNumber.class);
        assertEquals(CopyFactoryNumber.class, type.javaType());
        assertNull(type.valueOf((String) null));
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("12"));
    }

    @Test
    public void testNumericConstructorRemainsAvailableAlongsideCopyConstructor() {
        final Type<CopyAndLongConstructorNumber> type = Type.of(CopyAndLongConstructorNumber.class);
        assertEquals(123L, type.valueOf("123").longValue());
        assertEquals(Long.MIN_VALUE, type.valueOf(Long.toString(Long.MIN_VALUE)).longValue());
    }

    @Test
    public void testNumericFactoryRemainsAvailableAlongsideCopyFactory() {
        final Type<CopyAndLongFactoryNumber> type = Type.of(CopyAndLongFactoryNumber.class);
        assertEquals(123L, type.valueOf("123").longValue());
        assertEquals(Long.MIN_VALUE, type.valueOf(Long.toString(Long.MIN_VALUE)).longValue());
        assertEquals(Long.MAX_VALUE, type.valueOf(Long.toString(Long.MAX_VALUE)).longValue());
        assertNull(type.valueOf((String) null));
    }

    public abstract static class ReviewNumber extends Number {
        private final long value;

        protected ReviewNumber(final long value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    public static final class CopyConstructorNumber extends ReviewNumber {
        public CopyConstructorNumber(final CopyConstructorNumber value) {
            super(value.longValue());
        }
    }

    public static final class CopyFactoryNumber extends ReviewNumber {
        private CopyFactoryNumber(final long value) {
            super(value);
        }

        public static CopyFactoryNumber copy(final CopyFactoryNumber value) {
            return new CopyFactoryNumber(value.longValue());
        }
    }

    public static final class CopyAndLongConstructorNumber extends ReviewNumber {
        public CopyAndLongConstructorNumber(final CopyAndLongConstructorNumber value) {
            super(value.longValue());
        }

        public CopyAndLongConstructorNumber(final long value) {
            super(value);
        }
    }

    public static final class CopyAndLongFactoryNumber extends ReviewNumber {
        private CopyAndLongFactoryNumber(final long value) {
            super(value);
        }

        public static CopyAndLongFactoryNumber of(final CopyAndLongFactoryNumber value) {
            return new CopyAndLongFactoryNumber(value.longValue());
        }

        public static CopyAndLongFactoryNumber of(final long value) {
            return new CopyAndLongFactoryNumber(value);
        }
    }

    private NumberType<Integer> intNumberType;
    private NumberType<Double> doubleNumberType;
    private NumberType<Long> longNumberType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        intNumberType = (NumberType<Integer>) createType(Integer.class);
        doubleNumberType = (NumberType<Double>) createType(Double.class);
        longNumberType = (NumberType<Long>) createType(Long.class);
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testNumberTypeWithTypeName() {
        NumberType<Number> numberType = (NumberType<Number>) createType("Number");
        assertNotNull(numberType);
        assertEquals(Number.class, numberType.javaType());
    }

    @Test
    public void testNumberTypeConstructorWithClass() {
        // Test creating NumberType via Class directly (uses reflection constructor)
        NumberType<Integer> intType = (NumberType<Integer>) createType(Integer.class);
        assertNotNull(intType);
        assertEquals(Integer.class, intType.javaType());
        assertEquals(Integer.valueOf(42), intType.valueOf("42"));
        assertEquals("42", intType.stringOf(42));
    }

    @Test
    public void testNumberTypeWithBigInteger() {
        NumberType<java.math.BigInteger> bigIntType = (NumberType<java.math.BigInteger>) createType(java.math.BigInteger.class);
        assertNotNull(bigIntType);
        java.math.BigInteger result = bigIntType.valueOf("12345678901234567890");
        assertEquals(new java.math.BigInteger("12345678901234567890"), result);
        assertEquals("12345678901234567890", bigIntType.stringOf(new java.math.BigInteger("12345678901234567890")));
    }

    @Test
    public void testIsNumber() {
        assertTrue(intNumberType.isNumber());
        assertTrue(doubleNumberType.isNumber());
        assertTrue(longNumberType.isNumber());
    }

    @Test
    public void testTypeCapabilitiesFollowRepresentedNumberClass() {
        NumberType<Number> numberType = (NumberType<Number>) createType("Number");

        assertFalse(numberType.isImmutable());
        assertFalse(numberType.isComparable());
        assertThrows(UnsupportedOperationException.class, () -> numberType.compare(1, 2));
        assertTrue(intNumberType.isImmutable());
        assertTrue(intNumberType.isComparable());
        assertTrue(new NumberType<>(java.math.BigInteger.class).isImmutable());
        assertFalse(new NumberType<>(java.util.concurrent.atomic.LongAdder.class).isImmutable());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(intNumberType.isCsvQuoteRequired());
        assertFalse(doubleNumberType.isCsvQuoteRequired());
        assertFalse(longNumberType.isCsvQuoteRequired());
    }

    @Test
    public void testClazz() {
        assertEquals(Integer.class, intNumberType.javaType());
        assertEquals(Double.class, doubleNumberType.javaType());
        assertEquals(Long.class, longNumberType.javaType());
    }

    @Test
    public void testValueOfWithValidString() {
        assertEquals(Integer.valueOf(123), intNumberType.valueOf("123"));
        assertEquals(Double.valueOf(123.45), doubleNumberType.valueOf("123.45"));
        assertEquals(Long.valueOf(9876543210L), longNumberType.valueOf("9876543210"));
    }

    @Test
    public void testSpecialNumberTypes() {
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
    public void testValueOfWithInvalidString() {
        assertThrows(NumberFormatException.class, () -> intNumberType.valueOf("abc"));
        assertThrows(NumberFormatException.class, () -> doubleNumberType.valueOf("xyz"));
        assertThrows(NumberFormatException.class, () -> longNumberType.valueOf("not-a-number"));
    }

    @Test
    public void testStringOfWithValue() {
        assertEquals("123", intNumberType.stringOf(123));
        assertEquals("123.45", doubleNumberType.stringOf(123.45));
        assertEquals("9876543210", longNumberType.stringOf(9876543210L));
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(intNumberType.stringOf(null));
        assertNull(doubleNumberType.stringOf(null));
        assertNull(longNumberType.stringOf(null));
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
    public void testSerializeToWithNull() throws IOException {
        intNumberType.serializeTo(writer, null, config);
        verify(writer).write(new char[] { 'n', 'u', 'l', 'l' });
    }

    @Test
    public void testSerializeToWithNullAsZero() throws IOException {
        NumberType<java.math.BigInteger> genericNumberType = new NumberType<>(java.math.BigInteger.class);
        when(config.isWriteNullNumberAsZero()).thenReturn(true);

        genericNumberType.serializeTo(writer, null, config);

        verify(writer).write('0');
    }

    @Test
    public void testSerializeToWithValue() throws IOException {
        intNumberType.serializeTo(writer, 123, config);
        verify(writer).writeInt(123);

        doubleNumberType.serializeTo(writer, 123.45, config);
        verify(writer).write(123.45);
    }

}
