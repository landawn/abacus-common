package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ---- bug review 2026-09-30 G119 begin ----
    private static final class PrivateCtorNumber extends Number {
        private final int value;

        public PrivateCtorNumber(final String value) {
            this.value = Integer.parseInt(value);
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

    private static final class PrivateFactoryNumber extends Number {
        private final long value;

        private PrivateFactoryNumber(final long value) {
            this.value = value;
        }

        public static PrivateFactoryNumber valueOf(final String value) {
            return new PrivateFactoryNumber(Long.parseLong(value));
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

    // G119-01: the auto-detected public constructor/factory of a non-public Number subclass must be callable
    @Test
    public void testValueOf_nonPublicNumberSubclass() {
        final Type<PrivateCtorNumber> ctorType = Type.of(PrivateCtorNumber.class);
        assertEquals(4, ctorType.valueOf("4").intValue());

        final Type<PrivateFactoryNumber> factoryType = Type.of(PrivateFactoryNumber.class);
        assertEquals(5L, factoryType.valueOf("5").longValue());
    }
    // ---- bug review 2026-09-30 G119 end ----

    // ---- deep review 2026-10-16 G018 begin ----
    public static final class G018NoNumericFieldNumber extends Number {
        private final String text;

        public G018NoNumericFieldNumber(final String text) {
            this.text = String.valueOf(Long.parseLong(text));
        }

        public static G018NoNumericFieldNumber of(final Number value) {
            throw new AssertionError("Number factory must not be selected");
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public long longValue() {
            return Long.parseLong(text);
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }
    }

    public static final class G018NumberAndLongConstructorNumber extends Number {
        private final int value;

        public G018NumberAndLongConstructorNumber(final Number value) {
            throw new AssertionError("Number constructor must not be selected");
        }

        public G018NumberAndLongConstructorNumber(final long value) {
            this.value = Math.toIntExact(value);
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

    // G018-01: without a concrete numeric field, a static of(Number) factory hid the String constructor -> UOE
    @Test
    public void testValueOf_numberFactoryDoesNotHideStringConstructorWithoutNumericField() {
        final Type<G018NoNumericFieldNumber> type = Type.of(G018NoNumericFieldNumber.class);
        assertEquals(4200L, type.valueOf("4200").longValue());
        assertNull(type.valueOf(""));
    }

    // G018-01: a public (Number) constructor hid the usable (long) constructor -> UOE
    @Test
    public void testValueOf_numberConstructorDoesNotHideLongConstructor() {
        final Type<G018NumberAndLongConstructorNumber> type = Type.of(G018NumberAndLongConstructorNumber.class);
        assertEquals(4200, type.valueOf("4200").intValue());
        assertEquals(-7, type.valueOf("-7").intValue());
    }
    // ---- deep review 2026-10-16 G018 end ----

    // ---- deep review 2026-10-16 verify G118 begin ----
    public static final class G118NumberFieldNumber extends Number {
        private final Number value;

        public G118NumberFieldNumber(final String text) {
            this.value = Long.parseLong(text);
        }

        public static G118NumberFieldNumber of(final Number value) {
            throw new AssertionError("Number factory must not be selected");
        }

        @Override
        public int intValue() {
            return value.intValue();
        }

        @Override
        public long longValue() {
            return value.longValue();
        }

        @Override
        public float floatValue() {
            return value.floatValue();
        }

        @Override
        public double doubleValue() {
            return value.doubleValue();
        }
    }

    public static final class G118OnlyNumberCreatorsNumber extends ReviewNumber {
        public G118OnlyNumberCreatorsNumber(final Number value) {
            super(value.longValue());
        }

        public static G118OnlyNumberCreatorsNumber from(final Number value) {
            return new G118OnlyNumberCreatorsNumber(value);
        }
    }

    // G018-01: a backing field declared as Number (valueType Number, as with no numeric field) must not let of(Number) hide the String constructor
    @Test
    public void testValueOf_numberFactoryDoesNotHideStringConstructorWithNumberField() {
        final Type<G118NumberFieldNumber> type = Type.of(G118NumberFieldNumber.class);
        assertEquals(12L, type.valueOf("12").longValue());
        assertNull(type.valueOf(null));
    }

    // G018-01 neighbour: a class whose only creators take Number still cannot be built from text (UnsupportedOperationException), empty text is null
    @Test
    public void testValueOf_onlyNumberCreatorsStillUnsupported() {
        final Type<G118OnlyNumberCreatorsNumber> type = Type.of(G118OnlyNumberCreatorsNumber.class);
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("1"));
        assertNull(type.valueOf(""));
    }
    // ---- deep review 2026-10-16 verify G118 end ----

    // ---- deep review 2026-10-16 G144 begin ----

    /** No numeric field: a String constructor plus a factory in different units (cents). */
    public static final class G144TextUnitsNumber extends Number {
        private final String text;

        public G144TextUnitsNumber(final String text) {
            this.text = text;
        }

        public static G144TextUnitsNumber ofCents(final long cents) {
            return new G144TextUnitsNumber(String.valueOf(cents / 100));
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public long longValue() {
            return Long.parseLong(text);
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }
    }

    /** No numeric field and no public constructor: one numeric factory only. */
    public static final class G144SingleFactoryNumber extends Number {
        private final String text;

        private G144SingleFactoryNumber(final String text) {
            this.text = text;
        }

        public static G144SingleFactoryNumber ofCents(final long cents) {
            return new G144SingleFactoryNumber(String.valueOf(cents / 100));
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public long longValue() {
            return Long.parseLong(text);
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }
    }

    /** No numeric field and no public constructor: two numeric factories (ambiguous). */
    public static final class G144TwoFactoriesNumber extends Number {
        private final String text;

        private G144TwoFactoriesNumber(final String text) {
            this.text = text;
        }

        public static G144TwoFactoriesNumber ofCents(final long cents) {
            return new G144TwoFactoriesNumber(String.valueOf(cents / 100));
        }

        public static G144TwoFactoriesNumber ofUnits(final int units) {
            return new G144TwoFactoriesNumber(String.valueOf(units));
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public long longValue() {
            return Long.parseLong(text);
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }
    }

    /** A concrete numeric field: a factory of the field's type is preferred over the String constructor (unchanged). */
    public static final class G144LongFieldNumber extends Number {
        private final long cents;

        public G144LongFieldNumber(final String text) {
            this.cents = Long.parseLong(text) * 100;
        }

        private G144LongFieldNumber(final long cents) {
            this.cents = cents;
        }

        public static G144LongFieldNumber ofCents(final long cents) {
            return new G144LongFieldNumber(cents);
        }

        @Override
        public int intValue() {
            return (int) longValue();
        }

        @Override
        public long longValue() {
            return cents / 100;
        }

        @Override
        public float floatValue() {
            return longValue();
        }

        @Override
        public double doubleValue() {
            return longValue();
        }
    }

    // G144 (G018-02): without a numeric field, a numeric factory in different units no longer wins over the String constructor
    // ("4200" was read as ofCents(4200) = 42).
    @Test
    public void testValueOf_noNumericFieldPrefersStringConstructorOverUnitsFactory() {
        assertEquals(4200L, Type.of(G144TextUnitsNumber.class).valueOf("4200").longValue());
    }

    // G144: without a numeric field and with several numeric factories (and no constructor) the choice is ambiguous, so no
    // creator is selected, instead of one picked in reflection order.
    @Test
    public void testValueOf_noNumericFieldSeveralFactoriesIsUnsupported() {
        final Type<G144TwoFactoriesNumber> type = Type.of(G144TwoFactoriesNumber.class);
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("4200"));
        assertNull(type.valueOf(""));
    }

    // G144 neighbours (unchanged): a single numeric factory is still the last resort without a constructor, and with a
    // concrete numeric field a factory of the field's type is still preferred over the String constructor.
    @Test
    public void testValueOf_singleFactoryAndFieldTypedFactoryUnchanged() {
        assertEquals(42L, Type.of(G144SingleFactoryNumber.class).valueOf("4200").longValue());
        assertEquals(42L, Type.of(G144LongFieldNumber.class).valueOf("4200").longValue());
    }

    // ---- deep review 2026-10-16 G144 end ----

    // ---- deep review 2026-10-16 G154 begin ----

    /** Its numeric field is inherited (ReviewNumber.value); the class itself declares only a static numeric field. */
    public static final class G154InheritedFieldNumber extends ReviewNumber {
        private static final long serialVersionUID = 1L;

        public G154InheritedFieldNumber(final String text) {
            super(Long.parseLong(text));
        }

        public static G154InheritedFieldNumber ofCents(final long cents) {
            return new G154InheritedFieldNumber(String.valueOf(cents / 100));
        }
    }

    /** An abstract Number subclass without fields whose only creator is one static numeric factory. */
    public abstract static class G154AbstractFactoryNumber extends ReviewNumber {
        protected G154AbstractFactoryNumber(final long value) {
            super(value);
        }

        public static G154AbstractFactoryNumber of(final long value) {
            return new G154AbstractFactoryNumber(value) {
            };
        }
    }

    // G154 (verifies G144): a numeric field inherited from a superclass, or a static numeric field, does not enable step 2,
    // so the String constructor is used instead of the units factory (r89: ofCents(4200) = 42).
    @Test
    public void testValueOf_inheritedOrStaticNumericFieldPrefersStringConstructor() {
        final Type<G154InheritedFieldNumber> type = Type.of(G154InheritedFieldNumber.class);
        assertEquals(4200L, type.valueOf("4200").longValue());
        assertEquals(-7L, type.valueOf("-7").longValue());
        assertNull(type.valueOf(""));
    }

    // G154 neighbour (unchanged): an abstract Number subclass with a single static numeric factory still uses it.
    @Test
    public void testValueOf_abstractNumberWithSingleFactoryUnchanged() {
        final Type<G154AbstractFactoryNumber> type = Type.of(G154AbstractFactoryNumber.class);
        assertEquals(5L, type.valueOf("5").longValue());
        assertNull(type.valueOf(null));
    }

    // ---- deep review 2026-10-16 G154 end ----
}
