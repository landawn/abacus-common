package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class NumberFactoryFallbackReview20261003Test extends TestBase {

    @Test
    void uniqueWiderFactoryDoesNotDependOnBackingFieldType() {
        assertEquals(42, Type.of(WiderFactory.class).valueOf("42").intValue());
        assertEquals(Integer.MIN_VALUE, Type.of(WiderFactory.class).valueOf(Integer.toString(Integer.MIN_VALUE)).intValue());
    }

    @Test
    void uniqueNarrowerFactoryUsesItsOwnParserAndRejectsOverflow() {
        assertEquals(Integer.MAX_VALUE, Type.of(NarrowerFactory.class).valueOf(Integer.toString(Integer.MAX_VALUE)).longValue());
        assertThrows(ArithmeticException.class, () -> Type.of(NarrowerFactory.class).valueOf("2147483648"));
    }

    @Test
    void uniqueBoxedFactoryUsesBoxedParameterType() {
        assertEquals(42, Type.of(BoxedFactory.class).valueOf("42").longValue());
    }

    @Test
    void existingStringFactoryKeepsPriority() {
        assertEquals(142, Type.of(StringPreferred.class).valueOf("42").longValue());
    }

    @Test
    void existingFieldCompatibleFactoryKeepsPriority() {
        assertEquals(142, Type.of(FieldPreferred.class).valueOf("42").longValue());
    }

    @Test
    void stringConstructorPrecedesNewNumericFallback() {
        assertEquals(142, Type.of(FactoryBeforeConstructor.class).valueOf("42").longValue());
    }

    @Test
    void moneyTextKeepsItsUnitsAndFractionalPart() {
        assertEquals(new BigDecimal("12.34"), Type.of(Money.class).valueOf("12.34").value);
        assertEquals(new BigDecimal("1234"), Type.of(Money.class).valueOf("1234").value);
        assertEquals(new BigDecimal("12.34"), Money.ofCents(1234).value);
    }

    @Test
    void numericConstructorAlsoPrecedesNewNumericFallback() {
        assertEquals(new BigDecimal("1234"), Type.of(WholeMoney.class).valueOf("1234").value);
    }

    public static final class Money extends TestNumber {
        private final BigDecimal value;

        public Money(final String value) {
            this.value = new BigDecimal(value);
        }

        public static Money ofCents(final long value) {
            return new Money(BigDecimal.valueOf(value, 2).toPlainString());
        }

        @Override
        public long longValue() {
            return value.longValue();
        }
    }

    public static final class WholeMoney extends TestNumber {
        private final BigDecimal value;

        public WholeMoney(final long value) {
            this.value = BigDecimal.valueOf(value);
        }

        private WholeMoney(final BigDecimal value) {
            this.value = value;
        }

        public static WholeMoney ofCents(final long value) {
            return new WholeMoney(BigDecimal.valueOf(value, 2));
        }

        @Override
        public long longValue() {
            return value.longValue();
        }
    }

    @Test
    void ambiguousNumericFactoriesDoNotDependOnReflectionOrder() {
        assertThrows(UnsupportedOperationException.class, () -> Type.of(AmbiguousFactories.class).valueOf("42"));
    }

    @Test
    void ambiguousNumericFactoriesRetainConstructorFallback() {
        assertEquals(42, Type.of(AmbiguousWithConstructor.class).valueOf("42").longValue());
    }

    @Test
    void copyFactoryIsExcludedFromFallbackCandidates() {
        assertEquals(42, Type.of(CopyAndNumericFactory.class).valueOf("42").longValue());
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

    private static final class WiderFactory extends TestNumber {
        private final int value;

        private WiderFactory(final int value) {
            this.value = value;
        }

        public static WiderFactory of(final long value) {
            return new WiderFactory(Math.toIntExact(value));
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class NarrowerFactory extends TestNumber {
        private final long value;

        private NarrowerFactory(final long value) {
            this.value = value;
        }

        public static NarrowerFactory of(final int value) {
            return new NarrowerFactory(value);
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class BoxedFactory extends TestNumber {
        private final Integer value;

        private BoxedFactory(final Integer value) {
            this.value = value;
        }

        public static BoxedFactory of(final Long value) {
            return new BoxedFactory(Math.toIntExact(value));
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class StringPreferred extends TestNumber {
        private final int value;

        private StringPreferred(final int value) {
            this.value = value;
        }

        public static StringPreferred valueOf(final String value) {
            return new StringPreferred(Integer.parseInt(value) + 100);
        }

        public static StringPreferred of(final long value) {
            return new StringPreferred(Math.toIntExact(value) + 200);
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class FieldPreferred extends TestNumber {
        private final int value;

        private FieldPreferred(final int value) {
            this.value = value;
        }

        public static FieldPreferred of(final Integer value) {
            return new FieldPreferred(value + 100);
        }

        public static FieldPreferred of(final long value) {
            return new FieldPreferred(Math.toIntExact(value) + 200);
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class FactoryBeforeConstructor extends TestNumber {
        private final int value;

        private FactoryBeforeConstructor(final int value) {
            this.value = value;
        }

        public FactoryBeforeConstructor(final String value) {
            this.value = Integer.parseInt(value) + 100;
        }

        public static FactoryBeforeConstructor of(final long value) {
            return new FactoryBeforeConstructor(Math.toIntExact(value));
        }

        @Override
        public long longValue() {
            return value;
        }
    }

    private static final class AmbiguousFactories extends TestNumber {
        private final double value;

        private AmbiguousFactories(final double value) {
            this.value = value;
        }

        public static AmbiguousFactories of(final int value) {
            return new AmbiguousFactories(value);
        }

        public static AmbiguousFactories of(final long value) {
            return new AmbiguousFactories(value + 100);
        }

        @Override
        public long longValue() {
            return (long) value;
        }
    }

    private static final class AmbiguousWithConstructor extends TestNumber {
        private final double value;

        public AmbiguousWithConstructor(final String value) {
            this.value = Double.parseDouble(value);
        }

        public static AmbiguousWithConstructor of(final int value) {
            return new AmbiguousWithConstructor("100");
        }

        public static AmbiguousWithConstructor of(final long value) {
            return new AmbiguousWithConstructor("200");
        }

        @Override
        public long longValue() {
            return (long) value;
        }
    }

    private static final class CopyAndNumericFactory extends TestNumber {
        private final int value;

        private CopyAndNumericFactory(final int value) {
            this.value = value;
        }

        public static CopyAndNumericFactory of(final CopyAndNumericFactory value) {
            return value;
        }

        public static CopyAndNumericFactory of(final long value) {
            return new CopyAndNumericFactory(Math.toIntExact(value));
        }

        @Override
        public long longValue() {
            return value;
        }
    }
}
