package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class JdkCompositionContractTest extends TestBase {
    static Stream<Arguments> nullOperands() throws Exception {
        List<Arguments> cases = new ArrayList<>();
        add(cases, (Function<Object, Object>) value -> value, java.util.function.Function.class, "compose", "andThen");
        add(cases, (BiFunction<Object, Object, Object>) (a, b) -> a, java.util.function.BiFunction.class, java.util.function.Function.class, "andThen");
        add(cases, (Consumer<Object>) value -> {
        }, java.util.function.Consumer.class, "andThen");
        add(cases, (BiConsumer<Object, Object>) (a, b) -> {
        }, java.util.function.BiConsumer.class, "andThen");
        add(cases, (Predicate<Object>) value -> true, java.util.function.Predicate.class, "and", "or");
        add(cases, (BiPredicate<Object, Object>) (a, b) -> true, java.util.function.BiPredicate.class, "and", "or");
        add(cases, (IntConsumer) value -> {
        }, java.util.function.IntConsumer.class, "andThen");
        add(cases, (LongConsumer) value -> {
        }, java.util.function.LongConsumer.class, "andThen");
        add(cases, (DoubleConsumer) value -> {
        }, java.util.function.DoubleConsumer.class, "andThen");
        add(cases, (IntPredicate) value -> true, java.util.function.IntPredicate.class, "and", "or");
        add(cases, (LongPredicate) value -> true, java.util.function.LongPredicate.class, "and", "or");
        add(cases, (DoublePredicate) value -> true, java.util.function.DoublePredicate.class, "and", "or");
        add(cases, (IntUnaryOperator) value -> value, java.util.function.IntUnaryOperator.class, "compose", "andThen");
        add(cases, (LongUnaryOperator) value -> value, java.util.function.LongUnaryOperator.class, "compose", "andThen");
        add(cases, (DoubleUnaryOperator) value -> value, java.util.function.DoubleUnaryOperator.class, "compose", "andThen");
        add(cases, (UnaryOperator<Object>) value -> value, UnaryOperator.class, java.util.function.UnaryOperator.class, "compose", "andThen");
        return cases.stream();
    }

    private static void add(List<Arguments> cases, Object receiver, Class<?> contract, String... names) throws Exception {
        add(cases, receiver, contract, contract, names);
    }

    private static void add(List<Arguments> cases, Object receiver, Class<?> contract, Class<?> operand, String... names) throws Exception {
        for (String name : names) {
            cases.add(Arguments.of(contract.getSimpleName() + "." + name, receiver, contract.getMethod(name, operand)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullOperands")
    void rejectsNullThroughTheDeclaredContract(String name, Object receiver, Method method) {
        InvocationTargetException failure = assertThrows(InvocationTargetException.class, () -> method.invoke(receiver, new Object[] { null }));
        assertInstanceOf(NullPointerException.class, failure.getCause(), name);
    }

    @Test
    void objectCompositionForwardsNullEmptyAndUnicodePayloadsInOrder() {
        for (String input : new String[] { null, "", "\uD83D\uDE03\uD800\u00E9" }) {
            List<String> seen = new ArrayList<>();
            Function<String, String> first = value -> {
                seen.add(value);
                return value;
            };
            Function<String, String> second = value -> {
                seen.add(value);
                return value;
            };
            assertSame(input, first.andThen(second).apply(input));
            assertEquals(java.util.Arrays.asList(input, input), seen);
            seen.clear();
            assertSame(input, second.compose(first).apply(input));
            assertEquals(java.util.Arrays.asList(input, input), seen);
        }
    }

    @Test
    void primitiveCompositionPreservesBoundaryValuesAndFloatingBits() {
        IntUnaryOperator ints = value -> value;
        LongUnaryOperator longs = value -> value;
        DoubleUnaryOperator doubles = value -> value;
        for (int value : new int[] { Integer.MIN_VALUE, 0, Integer.MAX_VALUE }) {
            assertEquals(value, ints.compose(ints).andThen(ints).applyAsInt(value));
        }
        for (long value : new long[] { Long.MIN_VALUE, 0L, Long.MAX_VALUE }) {
            assertEquals(value, longs.compose(longs).andThen(longs).applyAsLong(value));
        }
        for (double value : new double[] { -0.0, 0.0, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY }) {
            assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(doubles.compose(doubles).andThen(doubles).applyAsDouble(value)));
        }
    }

    @Test
    void shortCircuitingAndCallbackFailureIdentityRemainIntact() {
        AssertionError failure = new AssertionError("callback");
        Predicate<String> fail = value -> {
            throw failure;
        };
        assertFalse(((Predicate<String>) value -> false).and(fail).test(null));
        assertTrue(((Predicate<String>) value -> true).or(fail).test(""));
        assertSame(failure, assertThrows(AssertionError.class, () -> fail.and(value -> true).test("\uD83D\uDE03")));
        Function<String, String> first = value -> {
            throw failure;
        };
        Function<String, String> after = value -> {
            fail("after must not be evaluated");
            return value;
        };
        assertSame(failure, assertThrows(AssertionError.class, () -> first.andThen(after).apply(null)));
        Consumer<String> consumer = value -> {
            throw failure;
        };
        assertSame(failure, assertThrows(AssertionError.class, () -> consumer.andThen(value -> fail("after must not be evaluated")).accept("")));
    }
}
