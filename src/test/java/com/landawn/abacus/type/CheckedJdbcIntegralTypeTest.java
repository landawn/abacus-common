package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class CheckedJdbcIntegralTypeTest extends TestBase {
    static Stream<Arguments> integralTypes() {
        return Stream.of(Arguments.of("Byte", -128L, 127L), Arguments.of("Integer", (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE),
                Arguments.of("Long", Long.MIN_VALUE, Long.MAX_VALUE), Arguments.of("OptionalByte", -128L, 127L), Arguments.of("OptionalShort", -32768L, 32767L),
                Arguments.of("OptionalInt", (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE), Arguments.of("OptionalLong", Long.MIN_VALUE, Long.MAX_VALUE),
                Arguments.of("java.util.OptionalInt", (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE),
                Arguments.of("java.util.OptionalLong", Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @ParameterizedTest
    @MethodSource("integralTypes")
    void preservesNullAndFiniteCoercion(String name, long min, long max) throws Exception {
        Type<?> type = TypeFactory.getType(name);
        ResultSet rs = mock(ResultSet.class);
        assertRead(type, rs, null, type.valueOf((String) null));
        assertRead(type, rs, "", type.valueOf("0"));
        assertRead(type, rs, -0.0d, type.valueOf("0"));
        assertRead(type, rs, new BigDecimal("12.99"), type.valueOf("12"));
        assertRead(type, rs, new BigDecimal("-12.99"), type.valueOf("-12"));
        assertRead(type, rs, "12", type.valueOf("12"));
    }

    @ParameterizedTest
    @MethodSource("integralTypes")
    void preservesExactBoundaries(String name, long min, long max) throws Exception {
        Type<?> type = TypeFactory.getType(name);
        ResultSet rs = mock(ResultSet.class);
        for (long boundary : new long[] { min, max }) {
            String text = Long.toString(boundary);
            Object expected = type.valueOf(text);
            assertRead(type, rs, new BigInteger(text), expected);
            assertRead(type, rs, new BigDecimal(text), expected);
            assertRead(type, rs, text, expected);
            assertRead(type, rs, new BigDecimal(text).add(new BigDecimal(boundary < 0 ? "-0.9" : "0.9")), expected);
        }
    }

    @ParameterizedTest
    @MethodSource("integralTypes")
    void rejectsOverflowAndNonfiniteValues(String name, long min, long max) throws Exception {
        Type<?> type = TypeFactory.getType(name);
        ResultSet rs = mock(ResultSet.class);
        for (Object invalid : new Object[] { BigInteger.valueOf(min).subtract(BigInteger.ONE), BigInteger.valueOf(max).add(BigInteger.ONE),
                new BigDecimal("1E100"), Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN }) {
            when(rs.getObject(1)).thenReturn(invalid);
            when(rs.getObject("value")).thenReturn(invalid);
            assertThrows(ArithmeticException.class, () -> type.get(rs, 1), name + ": " + invalid);
            assertThrows(ArithmeticException.class, () -> type.get(rs, "value"), name + ": " + invalid);
        }
        for (String invalid : new String[] { "12.5", "not a number", "\uD83D\uDE00" }) {
            when(rs.getObject(1)).thenReturn(invalid);
            when(rs.getObject("value")).thenReturn(invalid);
            assertThrows(NumberFormatException.class, () -> type.get(rs, 1));
            assertThrows(NumberFormatException.class, () -> type.get(rs, "value"));
        }
    }

    private static void assertRead(Type<?> type, ResultSet rs, Object input, Object expected) throws Exception {
        when(rs.getObject(1)).thenReturn(input);
        when(rs.getObject("value")).thenReturn(input);
        assertEquals(expected, type.get(rs, 1));
        assertEquals(expected, type.get(rs, "value"));
    }
}
