package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class NumberFactoryParameterReview20261003Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void numberParameterDoesNotHideStringConstructor(final boolean directHandler) {
        final Type<NumberFactoryWithConstructor> type = directHandler ? new NumberType<>(NumberFactoryWithConstructor.class)
                : Type.of(NumberFactoryWithConstructor.class);

        assertEquals(42, type.valueOf("42").intValue());
        assertEquals(-27, type.valueOf("-27").intValue());
        assertThrows(NumberFormatException.class, () -> type.valueOf("not-a-number"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    void stringConstructorWinsOverAdditionalNumericFactories() {
        // The constructor defines text semantics; the differently represented long factory adds only 10.
        assertEquals(1042, Type.of(NumberAndLongFactories.class).valueOf("42").intValue());
    }

    @Test
    void unsupportedNumberOverloadDoesNotMakeLastResortFactoryAmbiguous() {
        assertEquals(52, Type.of(NumberAndLongFactoriesWithoutConstructor.class).valueOf("42").intValue());
    }

    @Test
    void namedStringFactoryRetainsPriority() {
        assertEquals(42, Type.of(StringFactoryPreferred.class).valueOf("42").intValue());
    }

    @Test
    void fieldCompatibleFactoryRetainsPriority() {
        assertEquals(42, Type.of(FieldFactoryPreferred.class).valueOf("42").intValue());
    }

    @Test
    void concreteNumberParameterUsesItsOwnParser() {
        assertEquals(42, Type.of(DecimalFactory.class).valueOf("42.0").intValue());
        assertThrows(ArithmeticException.class, () -> Type.of(DecimalFactory.class).valueOf("42.5"));
    }

    @Test
    void abstractNumberParameterWithStringCreatorRemainsSupported() {
        assertEquals(42, Type.of(AbstractParameterFactory.class).valueOf("42").intValue());
    }

    @Test
    void numberOnlyFactoryStillReportsUnsupportedCreationForNonemptyInput() {
        final Type<NumberOnlyFactory> type = Type.of(NumberOnlyFactory.class);
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf("42"));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
        assertThrows(UnsupportedOperationException.class, () -> Type.of(Number.class).valueOf("42"));
    }

    private abstract static class TestNumber extends Number {
        @Override
        public int intValue() {
            return (int) longValue();
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

    private static final class NumberFactoryWithConstructor extends TestNumber {
        private final int value;

        public NumberFactoryWithConstructor(final String value) {
            this.value = Integer.parseInt(value);
        }

        public static NumberFactoryWithConstructor of(final Number value) {
            throw new AssertionError("Number factory must not be selected");
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class NumberAndLongFactories extends TestNumber {
        private final int value;

        private NumberAndLongFactories(final int value) {
            this.value = value;
        }

        public NumberAndLongFactories(final String value) {
            this.value = Integer.parseInt(value) + 1000;
        }

        public static NumberAndLongFactories of(final Number value) {
            throw new AssertionError("Number factory must not be selected");
        }

        public static NumberAndLongFactories of(final long value) {
            return new NumberAndLongFactories(Math.toIntExact(value) + 10);
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class StringFactoryPreferred extends TestNumber {
        private final int value;

        private StringFactoryPreferred(final int value) {
            this.value = value;
        }

        public StringFactoryPreferred(final String value) {
            this.value = Integer.parseInt(value) + 1000;
        }

        public static StringFactoryPreferred valueOf(final String value) {
            return new StringFactoryPreferred(Integer.parseInt(value));
        }

        public static StringFactoryPreferred of(final Number value) {
            throw new AssertionError("String factory must retain priority");
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class NumberAndLongFactoriesWithoutConstructor extends TestNumber {
        private final int value;

        private NumberAndLongFactoriesWithoutConstructor(final int value) {
            this.value = value;
        }

        public static NumberAndLongFactoriesWithoutConstructor of(final Number value) {
            throw new AssertionError("Number cannot parse text");
        }

        public static NumberAndLongFactoriesWithoutConstructor of(final long value) {
            return new NumberAndLongFactoriesWithoutConstructor(Math.toIntExact(value) + 10);
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class FieldFactoryPreferred extends TestNumber {
        private final int value;

        private FieldFactoryPreferred(final int value) {
            this.value = value;
        }

        public static FieldFactoryPreferred of(final Integer value) {
            return new FieldFactoryPreferred(value);
        }

        public static FieldFactoryPreferred of(final Number value) {
            throw new AssertionError("Field-compatible factory must retain priority");
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class DecimalFactory extends TestNumber {
        private final int value;

        private DecimalFactory(final int value) {
            this.value = value;
        }

        public static DecimalFactory of(final BigDecimal value) {
            return new DecimalFactory(value.intValueExact());
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private abstract static class ParseableNumber extends TestNumber {
        public static ParseableNumber valueOf(final String value) {
            return new ParsedNumber(Integer.parseInt(value));
        }
    }

    private static final class ParsedNumber extends ParseableNumber {
        private final int value;

        private ParsedNumber(final int value) {
            this.value = value;
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class AbstractParameterFactory extends TestNumber {
        private final int value;

        private AbstractParameterFactory(final int value) {
            this.value = value;
        }

        public static AbstractParameterFactory of(final ParseableNumber value) {
            return new AbstractParameterFactory(value.intValue());
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class NumberOnlyFactory extends TestNumber {
        private final int value;

        private NumberOnlyFactory(final int value) {
            this.value = value;
        }

        public static NumberOnlyFactory of(final Number value) {
            return new NumberOnlyFactory(value.intValue());
        }

        @Override
        public long longValue() {
            return value;
        }
    }
}
