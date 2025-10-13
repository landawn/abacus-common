package com.landawn.abacus.util.function;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

@Tag("new-test")
public class FunctionAPI100Test extends TestBase {

    @Nested
    public class BiConsumerTest {

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            BiConsumer<String, Integer> consumer1 = (s, i) -> results.add(s + ":" + i);
            BiConsumer<String, Integer> consumer2 = (s, i) -> results.add(s.toUpperCase() + ":" + (i * 2));

            BiConsumer<String, Integer> chainedConsumer = consumer1.andThen(consumer2);
            chainedConsumer.accept("test", 5);

            assertEquals(2, results.size());
            assertEquals("test:5", results.get(0));
            assertEquals("TEST:10", results.get(1));
        }

        @Test
        public void testToThrowable() {
            BiConsumer<String, Integer> consumer = (s, i) -> {
            };
            Throwables.BiConsumer<String, Integer, ?> throwableConsumer = consumer.toThrowable();
            assertNotNull(throwableConsumer);
            BiConsumer<String, Integer> original = (BiConsumer<String, Integer>) throwableConsumer;
            assertNotNull(original);
        }
    }

    @Nested
    public class BiFunctionTest {

        @Test
        public void testApply() {
            BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
            String result = biFunction.apply("value", 10);
            assertEquals("value:10", result);
        }

        @Test
        public void testAndThen() {
            BiFunction<String, Integer, String> biFunction = (s, i) -> s + ":" + i;
            java.util.function.Function<String, Integer> afterFunction = String::length;

            BiFunction<String, Integer, Integer> chainedFunction = biFunction.andThen(afterFunction);
            Integer finalResult = chainedFunction.apply("value", 10);

            assertEquals(8, finalResult);
        }

        @Test
        public void testToThrowable() {
            BiFunction<String, Integer, String> biFunction = (s, i) -> s + i;
            Throwables.BiFunction<String, Integer, String, ?> throwableBiFunction = biFunction.toThrowable();
            assertNotNull(throwableBiFunction);
            BiFunction<String, Integer, String> original = (BiFunction<String, Integer, String>) throwableBiFunction;
            assertNotNull(original);
        }
    }

    @Nested
    public class BiIntObjConsumerTest {

        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            BiIntObjConsumer<String> consumer = (i, j, s) -> result.set(s + (i + j));
            consumer.accept(10, 20, "Sum: ");
            assertEquals("Sum: 30", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            BiIntObjConsumer<String> consumer1 = (i, j, s) -> results.add(s + (i + j));
            BiIntObjConsumer<String> consumer2 = (i, j, s) -> results.add(s.toUpperCase() + (i * j));

            BiIntObjConsumer<String> chainedConsumer = consumer1.andThen(consumer2);
            chainedConsumer.accept(3, 4, "Result: ");

            assertEquals(2, results.size());
            assertEquals("Result: 7", results.get(0));
            assertEquals("RESULT: 12", results.get(1));
        }
    }

    @Nested
    public class BiIntObjFunctionTest {

        @Test
        public void testApply() {
            BiIntObjFunction<String, String> function = (i, j, s) -> s + (i * j);
            String result = function.apply(5, 6, "Product: ");
            assertEquals("Product: 30", result);
        }

        @Test
        public void testAndThen() {
            BiIntObjFunction<String, String> function = (i, j, s) -> s + (i + j);
            java.util.function.Function<String, Integer> afterFunction = String::length;

            BiIntObjFunction<String, Integer> chainedFunction = function.andThen(afterFunction);
            Integer result = chainedFunction.apply(5, 10, "Sum=");
            assertEquals(6, result);
        }
    }

    @Nested
    public class BiIntObjPredicateTest {
        final BiIntObjPredicate<String> isSumEven = (i, j, s) -> (i + j) % 2 == 0;
        final BiIntObjPredicate<String> isStringLong = (i, j, s) -> s.length() > 5;

        @Test
        public void testTest() {
            assertTrue(isSumEven.test(2, 4, "any"));
            assertFalse(isSumEven.test(2, 3, "any"));
        }

        @Test
        public void testNegate() {
            BiIntObjPredicate<String> isSumOdd = isSumEven.negate();
            assertFalse(isSumOdd.test(2, 4, "any"));
            assertTrue(isSumOdd.test(2, 3, "any"));
        }

        @Test
        public void testAnd() {
            BiIntObjPredicate<String> combined = isSumEven.and(isStringLong);
            assertTrue(combined.test(2, 2, "long string"));
            assertFalse(combined.test(2, 3, "long string"));
            assertFalse(combined.test(2, 2, "short"));
            assertFalse(combined.test(2, 3, "short"));
        }

        @Test
        public void testOr() {
            BiIntObjPredicate<String> combined = isSumEven.or(isStringLong);
            assertTrue(combined.test(2, 2, "long string"));
            assertTrue(combined.test(2, 3, "long string"));
            assertTrue(combined.test(2, 2, "short"));
            assertFalse(combined.test(2, 3, "short"));
        }
    }

    @Nested
    public class BinaryOperatorTest {
        @Test
        public void testApply() {
            BinaryOperator<Integer> adder = (a, b) -> a + b;
            assertEquals(5, adder.apply(2, 3));
        }

        @Test
        public void testToThrowable() {
            BinaryOperator<String> concat = (s1, s2) -> s1 + s2;
            Throwables.BinaryOperator<String, ?> throwableOp = concat.toThrowable();
            assertNotNull(throwableOp);
            BinaryOperator<String> original = (BinaryOperator<String>) throwableOp;
            assertNotNull(original);
        }
    }

    @Nested
    public class BiObjIntConsumerTest {

        @Test
        public void testAccept() {
            AtomicInteger result = new AtomicInteger();
            BiObjIntConsumer<String, String> consumer = (s1, s2, i) -> result.set(s1.length() + s2.length() + i);
            consumer.accept("abc", "de", 10);
            assertEquals(15, result.get());
        }

        @Test
        public void testAndThen() {
            List<Integer> results = new ArrayList<>();
            BiObjIntConsumer<String, Integer> consumer1 = (s, i1, i2) -> results.add(s.length() + i1 + i2);
            BiObjIntConsumer<String, Integer> consumer2 = (s, i1, i2) -> results.add(s.length() * i1 * i2);

            BiObjIntConsumer<String, Integer> chainedConsumer = consumer1.andThen(consumer2);
            chainedConsumer.accept("test", 5, 10);

            assertEquals(2, results.size());
            assertEquals(19, results.get(0));
            assertEquals(200, results.get(1));
        }
    }

    @Nested
    public class BiObjIntFunctionTest {

        @Test
        public void testApply() {
            BiObjIntFunction<String, Integer, String> function = (s, i1, i2) -> s + (i1 * i2);
            String result = function.apply("Result: ", 10, 5);
            assertEquals("Result: 50", result);
        }

        @Test
        public void testAndThen() {
            BiObjIntFunction<String, String, String> function = (s1, s2, i) -> s1 + s2 + i;
            java.util.function.Function<String, Integer> afterFunction = String::length;

            BiObjIntFunction<String, String, Integer> chainedFunction = function.andThen(afterFunction);
            Integer result = chainedFunction.apply("a", "b", 100);

            assertEquals(5, result);
        }
    }

    @Nested
    public class BiObjIntPredicateTest {
        final BiObjIntPredicate<String, Integer> isSumOfLengthsEqualToInt = (s, i1, i2) -> s.length() + i1.toString().length() == i2;
        final BiObjIntPredicate<String, String> isConcatLengthEven = (s1, s2, i) -> (s1.length() + s2.length() + i) % 2 == 0;

        @Test
        public void testTest() {
            assertTrue(isSumOfLengthsEqualToInt.test("test", 123, 7));
            assertFalse(isSumOfLengthsEqualToInt.test("test", 123, 8));
        }

        @Test
        public void testNegate() {
            BiObjIntPredicate<String, String> isConcatLengthOdd = isConcatLengthEven.negate();
            assertFalse(isConcatLengthOdd.test("a", "b", 2));
            assertTrue(isConcatLengthOdd.test("a", "b", 1));
        }

        @Test
        public void testAnd() {
            BiObjIntPredicate<String, Integer> first = (s, i1, i2) -> s.length() > i2;
            BiObjIntPredicate<String, Integer> second = (s, i1, i2) -> i1 > i2;
            BiObjIntPredicate<String, Integer> combined = first.and(second);

            assertTrue(combined.test("hello", 20, 4));
            assertFalse(combined.test("hello", 3, 4));
            assertFalse(combined.test("hi", 20, 4));
        }

        @Test
        public void testOr() {
            BiObjIntPredicate<String, Integer> first = (s, i1, i2) -> s.length() > i2;
            BiObjIntPredicate<String, Integer> second = (s, i1, i2) -> i1 > i2;
            BiObjIntPredicate<String, Integer> combined = first.or(second);

            assertTrue(combined.test("hello", 20, 4));
            assertTrue(combined.test("hello", 3, 4));
            assertTrue(combined.test("hi", 20, 4));
            assertFalse(combined.test("hi", 3, 4));
        }
    }

    @Nested
    public class BiPredicateTest {
        final BiPredicate<String, String> isLengthEqual = (s1, s2) -> s1.length() == s2.length();
        final BiPredicate<String, String> startsWithSameChar = (s1, s2) -> !s1.isEmpty() && !s2.isEmpty() && s1.charAt(0) == s2.charAt(0);

        @Test
        public void testTest() {
            assertTrue(isLengthEqual.test("abc", "def"));
            assertFalse(isLengthEqual.test("a", "ab"));
        }

        @Test
        public void testNegate() {
            BiPredicate<String, String> isLengthNotEqual = isLengthEqual.negate();
            assertFalse(isLengthNotEqual.test("abc", "def"));
            assertTrue(isLengthNotEqual.test("a", "ab"));
        }

        @Test
        public void testAnd() {
            BiPredicate<String, String> combined = isLengthEqual.and(startsWithSameChar);
            assertTrue(combined.test("apple", "apply"));
            assertFalse(combined.test("apple", "orange"));
            assertFalse(combined.test("apple", "apricot"));
            assertFalse(combined.test("banana", "orange"));
        }

        @Test
        public void testOr() {
            BiPredicate<String, String> combined = isLengthEqual.or(startsWithSameChar);
            assertTrue(combined.test("apple", "apply"));
            assertTrue(combined.test("apple", "apricot"));
            assertTrue(combined.test("banana", "orange"));
            assertFalse(combined.test("red", "blue"));
        }

        @Test
        public void testToThrowable() {
            BiPredicate<Integer, Integer> isEven = (i1, i2) -> (i1 + i2) % 2 == 0;
            Throwables.BiPredicate<Integer, Integer, ?> throwablePredicate = isEven.toThrowable();
            assertNotNull(throwablePredicate);
            BiPredicate<Integer, Integer> original = (BiPredicate<Integer, Integer>) throwablePredicate;
            assertNotNull(original);
        }
    }

    @Nested
    public class BooleanBiConsumerTest {
        @Test
        public void testAccept() {
            List<String> results = new ArrayList<>();
            BooleanBiConsumer consumer = (t, u) -> results.add(t + ":" + u);
            consumer.accept(true, false);
            assertEquals(1, results.size());
            assertEquals("true:false", results.get(0));
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            BooleanBiConsumer first = (t, u) -> order.add("first:" + t + "," + u);
            BooleanBiConsumer second = (t, u) -> order.add("second:" + t + "," + u);

            first.andThen(second).accept(true, true);

            assertEquals(2, order.size());
            assertEquals("first:true,true", order.get(0));
            assertEquals("second:true,true", order.get(1));
        }
    }

    @Nested
    public class BooleanBiFunctionTest {
        @Test
        public void testApply() {
            BooleanBiFunction<String> function = (t, u) -> "Received: " + t + " and " + u;
            assertEquals("Received: true and false", function.apply(true, false));
        }

        @Test
        public void testAndThen() {
            BooleanBiFunction<String> initial = (t, u) -> t + "," + u;
            java.util.function.Function<String, Integer> after = String::length;

            BooleanBiFunction<Integer> chained = initial.andThen(after);
            assertEquals(10, chained.apply(true, false));
        }
    }

    @Nested
    public class BooleanBinaryOperatorTest {
        @Test
        public void testApplyAsBoolean() {
            BooleanBinaryOperator logicalAnd = (left, right) -> left && right;
            assertTrue(logicalAnd.applyAsBoolean(true, true));
            assertFalse(logicalAnd.applyAsBoolean(true, false));
            assertFalse(logicalAnd.applyAsBoolean(false, true));
            assertFalse(logicalAnd.applyAsBoolean(false, false));
        }
    }

    @Nested
    public class BooleanBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(BooleanBiPredicate.ALWAYS_TRUE.test(true, false));
            assertFalse(BooleanBiPredicate.ALWAYS_FALSE.test(true, false));

            assertTrue(BooleanBiPredicate.BOTH_TRUE.test(true, true));
            assertFalse(BooleanBiPredicate.BOTH_TRUE.test(true, false));

            assertTrue(BooleanBiPredicate.BOTH_FALSE.test(false, false));
            assertFalse(BooleanBiPredicate.BOTH_FALSE.test(true, false));

            assertTrue(BooleanBiPredicate.EQUAL.test(true, true));
            assertFalse(BooleanBiPredicate.EQUAL.test(true, false));

            assertTrue(BooleanBiPredicate.NOT_EQUAL.test(true, false));
            assertFalse(BooleanBiPredicate.NOT_EQUAL.test(true, true));
        }

        @Test
        public void testTest() {
            BooleanBiPredicate isFirstTrue = (t, u) -> t;
            assertTrue(isFirstTrue.test(true, false));
            assertFalse(isFirstTrue.test(false, true));
        }

        @Test
        public void testNegate() {
            BooleanBiPredicate isFirstTrue = (t, u) -> t;
            BooleanBiPredicate isFirstFalse = isFirstTrue.negate();
            assertFalse(isFirstFalse.test(true, false));
            assertTrue(isFirstFalse.test(false, true));
        }

        @Test
        public void testAnd() {
            BooleanBiPredicate isFirstTrue = (t, u) -> t;
            BooleanBiPredicate isSecondTrue = (t, u) -> u;
            BooleanBiPredicate bothTrue = isFirstTrue.and(isSecondTrue);

            assertTrue(bothTrue.test(true, true));
            assertFalse(bothTrue.test(true, false));
            assertFalse(bothTrue.test(false, true));
            assertFalse(bothTrue.test(false, false));
        }

        @Test
        public void testOr() {
            BooleanBiPredicate isFirstTrue = (t, u) -> t;
            BooleanBiPredicate isSecondTrue = (t, u) -> u;
            BooleanBiPredicate eitherTrue = isFirstTrue.or(isSecondTrue);

            assertTrue(eitherTrue.test(true, true));
            assertTrue(eitherTrue.test(true, false));
            assertTrue(eitherTrue.test(false, true));
            assertFalse(eitherTrue.test(false, false));
        }
    }

    @Nested
    public class BooleanConsumerTest {
        @Test
        public void testAccept() {
            AtomicBoolean result = new AtomicBoolean(false);
            BooleanConsumer consumer = result::set;
            consumer.accept(true);
            assertTrue(result.get());
        }

        @Test
        public void testAndThen() {
            List<Boolean> results = new ArrayList<>();
            BooleanConsumer first = results::add;
            BooleanConsumer second = b -> results.add(!b);
            BooleanConsumer chained = first.andThen(second);
            chained.accept(true);

            assertEquals(2, results.size());
            assertEquals(true, results.get(0));
            assertEquals(false, results.get(1));
        }
    }

    @Nested
    public class BooleanFunctionTest {
        @Test
        public void testStaticBox() {
            assertEquals(Boolean.TRUE, BooleanFunction.BOX.apply(true));
            assertEquals(Boolean.FALSE, BooleanFunction.BOX.apply(false));
        }

        @Test
        public void testStaticIdentity() {
            BooleanFunction<Boolean> identity = BooleanFunction.identity();
            assertTrue(identity.apply(true));
            assertFalse(identity.apply(false));
        }

        @Test
        public void testApply() {
            BooleanFunction<String> function = val -> val ? "Yes" : "No";
            assertEquals("Yes", function.apply(true));
            assertEquals("No", function.apply(false));
        }

        @Test
        public void testAndThen() {
            BooleanFunction<String> initial = val -> val ? "true" : "false";
            java.util.function.Function<String, Integer> after = String::length;
            BooleanFunction<Integer> chained = initial.andThen(after);

            assertEquals(4, chained.apply(true));
            assertEquals(5, chained.apply(false));
        }
    }

    @Nested
    public class BooleanNConsumerTest {
        @Test
        public void testAccept() {
            AtomicInteger trueCount = new AtomicInteger(0);
            BooleanNConsumer consumer = args -> {
                for (boolean b : args)
                    if (b)
                        trueCount.incrementAndGet();
            };
            consumer.accept(true, false, true, true);
            assertEquals(3, trueCount.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            BooleanNConsumer first = args -> order.add("first:" + Arrays.toString(args));
            BooleanNConsumer second = args -> order.add("second:" + Arrays.toString(args));
            BooleanNConsumer chained = first.andThen(second);
            chained.accept(true, false);

            assertEquals(2, order.size());
            assertEquals("first:[true, false]", order.get(0));
            assertEquals("second:[true, false]", order.get(1));
        }
    }

    @Nested
    public class BooleanNFunctionTest {
        @Test
        public void testApply() {
            BooleanNFunction<Integer> countTrues = args -> {
                int count = 0;
                for (boolean b : args)
                    if (b)
                        count++;
                return count;
            };
            assertEquals(2, countTrues.apply(true, false, true));
            assertEquals(0, countTrues.apply(false, false));
        }

        @Test
        public void testAndThen() {
            BooleanNFunction<Integer> countTrues = args -> {
                int count = 0;
                for (boolean b : args)
                    if (b)
                        count++;
                return count;
            };
            java.util.function.Function<Integer, String> after = count -> "Count is " + count;
            BooleanNFunction<String> chained = countTrues.andThen(after);

            assertEquals("Count is 3", chained.apply(true, true, false, true));
        }
    }

    @Nested
    public class BooleanPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(BooleanPredicate.ALWAYS_TRUE.test(false));
            assertFalse(BooleanPredicate.ALWAYS_FALSE.test(true));
            assertTrue(BooleanPredicate.IS_TRUE.test(true));
            assertFalse(BooleanPredicate.IS_TRUE.test(false));
            assertTrue(BooleanPredicate.IS_FALSE.test(false));
            assertFalse(BooleanPredicate.IS_FALSE.test(true));
        }

        @Test
        public void testStaticOf() {
            BooleanPredicate pred = val -> val;
            assertSame(pred, BooleanPredicate.of(pred));
        }

        @Test
        public void testNegate() {
            BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
            BooleanPredicate isFalse = isTrue.negate();
            assertTrue(isFalse.test(false));
            assertFalse(isFalse.test(true));
        }

        @Test
        public void testAnd() {
            BooleanPredicate alwaysTrue = BooleanPredicate.ALWAYS_TRUE;
            BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
            BooleanPredicate combined = alwaysTrue.and(isTrue);

            assertTrue(combined.test(true));
            assertFalse(combined.test(false));
        }

        @Test
        public void testOr() {
            BooleanPredicate alwaysFalse = BooleanPredicate.ALWAYS_FALSE;
            BooleanPredicate isTrue = BooleanPredicate.IS_TRUE;
            BooleanPredicate combined = alwaysFalse.or(isTrue);

            assertTrue(combined.test(true));
            assertFalse(combined.test(false));
        }
    }

    @Nested
    public class BooleanSupplierTest {
        @Test
        public void testStaticFields() {
            assertTrue(BooleanSupplier.TRUE.getAsBoolean());
            assertFalse(BooleanSupplier.FALSE.getAsBoolean());

            BooleanSupplier randomSupplier = BooleanSupplier.RANDOM;
            boolean result = randomSupplier.getAsBoolean();
            assertTrue(result || !result);
        }

        @Test
        public void testGetAsBoolean() {
            final boolean value = true;
            BooleanSupplier supplier = () -> value;
            assertTrue(supplier.getAsBoolean());
        }
    }

    @Nested
    public class BooleanTernaryOperatorTest {
        @Test
        public void testApplyAsBoolean() {
            BooleanTernaryOperator majority = (a, b, c) -> (a && b) || (b && c) || (a && c);
            assertTrue(majority.applyAsBoolean(true, true, false));
            assertTrue(majority.applyAsBoolean(true, false, true));
            assertTrue(majority.applyAsBoolean(false, true, true));
            assertTrue(majority.applyAsBoolean(true, true, true));
            assertFalse(majority.applyAsBoolean(false, false, true));
            assertFalse(majority.applyAsBoolean(false, true, false));
            assertFalse(majority.applyAsBoolean(true, false, false));
            assertFalse(majority.applyAsBoolean(false, false, false));
        }
    }

    @Nested
    public class BooleanToByteFunctionTest {
        @Test
        public void testDefault() {
            assertEquals((byte) 1, BooleanToByteFunction.DEFAULT.applyAsByte(true));
            assertEquals((byte) 0, BooleanToByteFunction.DEFAULT.applyAsByte(false));
        }

        @Test
        public void testApplyAsByte() {
            BooleanToByteFunction customFunc = value -> value ? (byte) -1 : (byte) -2;
            assertEquals((byte) -1, customFunc.applyAsByte(true));
            assertEquals((byte) -2, customFunc.applyAsByte(false));
        }
    }

    @Nested
    public class BooleanToCharFunctionTest {
        @Test
        public void testDefault() {
            assertEquals('Y', BooleanToCharFunction.DEFAULT.applyAsChar(true));
            assertEquals('N', BooleanToCharFunction.DEFAULT.applyAsChar(false));
        }

        @Test
        public void testApplyAsChar() {
            BooleanToCharFunction customFunc = value -> value ? 'T' : 'F';
            assertEquals('T', customFunc.applyAsChar(true));
            assertEquals('F', customFunc.applyAsChar(false));
        }
    }

    @Nested
    public class BooleanToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(1, BooleanToIntFunction.DEFAULT.applyAsInt(true));
            assertEquals(0, BooleanToIntFunction.DEFAULT.applyAsInt(false));
        }

        @Test
        public void testApplyAsInt() {
            BooleanToIntFunction customFunc = value -> value ? 100 : -100;
            assertEquals(100, customFunc.applyAsInt(true));
            assertEquals(-100, customFunc.applyAsInt(false));
        }
    }

    @Nested
    public class BooleanTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            BooleanTriConsumer consumer = (a, b, c) -> result.set(a + "," + b + "," + c);
            consumer.accept(true, false, true);
            assertEquals("true,false,true", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            BooleanTriConsumer first = (a, b, c) -> results.add("first");
            BooleanTriConsumer second = (a, b, c) -> results.add("second");

            BooleanTriConsumer chained = first.andThen(second);
            chained.accept(true, true, false);

            assertEquals(2, results.size());
            assertEquals("first", results.get(0));
            assertEquals("second", results.get(1));
        }
    }

    @Nested
    public class BooleanTriFunctionTest {
        @Test
        public void testApply() {
            BooleanTriFunction<String> function = (a, b, c) -> a + ":" + b + ":" + c;
            String result = function.apply(false, true, false);
            assertEquals("false:true:false", result);
        }

        @Test
        public void testAndThen() {
            BooleanTriFunction<Integer> countTrues = (a, b, c) -> (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
            java.util.function.Function<Integer, String> after = count -> "Count: " + count;

            BooleanTriFunction<String> chained = countTrues.andThen(after);
            String result = chained.apply(true, false, true);

            assertEquals("Count: 2", result);
        }
    }

    @Nested
    public class BooleanTriPredicateTest {
        final BooleanTriPredicate atLeastTwoTrue = (a, b, c) -> (a && b) || (b && c) || (a && c);
        final BooleanTriPredicate allSame = (a, b, c) -> (a == b) && (b == c);

        @Test
        public void testStaticFields() {
            assertTrue(BooleanTriPredicate.ALWAYS_TRUE.test(false, true, false));
            assertFalse(BooleanTriPredicate.ALWAYS_FALSE.test(true, true, true));
        }

        @Test
        public void testTest() {
            assertTrue(atLeastTwoTrue.test(true, true, false));
            assertFalse(atLeastTwoTrue.test(true, false, false));
        }

        @Test
        public void testNegate() {
            BooleanTriPredicate lessThanTwoTrue = atLeastTwoTrue.negate();
            assertFalse(lessThanTwoTrue.test(true, true, false));
            assertTrue(lessThanTwoTrue.test(true, false, false));
        }

        @Test
        public void testAnd() {
            BooleanTriPredicate combined = atLeastTwoTrue.and(allSame);
            assertTrue(combined.test(true, true, true));
            assertFalse(combined.test(true, true, false));
            assertFalse(combined.test(false, false, false));
        }

        @Test
        public void testOr() {
            BooleanTriPredicate combined = atLeastTwoTrue.or(allSame);
            assertTrue(combined.test(true, true, true));
            assertTrue(combined.test(true, true, false));
            assertTrue(combined.test(false, false, false));
            assertFalse(combined.test(true, false, false));
        }
    }

    @Nested
    public class BooleanUnaryOperatorTest {
        final BooleanUnaryOperator not = operand -> !operand;

        @Test
        public void testStaticIdentity() {
            BooleanUnaryOperator identity = BooleanUnaryOperator.identity();
            assertTrue(identity.applyAsBoolean(true));
            assertFalse(identity.applyAsBoolean(false));
        }

        @Test
        public void testApplyAsBoolean() {
            assertTrue(not.applyAsBoolean(false));
            assertFalse(not.applyAsBoolean(true));
        }

        @Test
        public void testCompose() {
            BooleanUnaryOperator identity = BooleanUnaryOperator.identity();
            BooleanUnaryOperator composedNot = identity.compose(not);
            BooleanUnaryOperator composedIdentity = not.compose(not);

            assertTrue(composedNot.applyAsBoolean(false));
            assertFalse(composedNot.applyAsBoolean(true));

            assertTrue(composedIdentity.applyAsBoolean(true));
            assertFalse(composedIdentity.applyAsBoolean(false));
        }

        @Test
        public void testAndThen() {
            BooleanUnaryOperator identity = BooleanUnaryOperator.identity();
            BooleanUnaryOperator chainedNot = not.andThen(identity);
            BooleanUnaryOperator chainedIdentity = not.andThen(not);

            assertTrue(chainedNot.applyAsBoolean(false));
            assertFalse(chainedNot.applyAsBoolean(true));

            assertTrue(chainedIdentity.applyAsBoolean(true));
            assertFalse(chainedIdentity.applyAsBoolean(false));
        }
    }

    @Nested
    public class ByteBiConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Integer> result = new AtomicReference<>();
            ByteBiConsumer consumer = (t, u) -> result.set(t + u);
            consumer.accept((byte) 10, (byte) 20);
            assertEquals(30, result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ByteBiConsumer first = (t, u) -> order.add("first:" + t + "," + u);
            ByteBiConsumer second = (t, u) -> order.add("second:" + t + "," + u);

            first.andThen(second).accept((byte) 5, (byte) 7);

            assertEquals(2, order.size());
            assertEquals("first:5,7", order.get(0));
            assertEquals("second:5,7", order.get(1));
        }
    }

    @Nested
    public class ByteBiFunctionTest {
        @Test
        public void testApply() {
            ByteBiFunction<Integer> sumFunction = (t, u) -> t + u;
            assertEquals(15, sumFunction.apply((byte) 8, (byte) 7));
        }

        @Test
        public void testAndThen() {
            ByteBiFunction<Integer> sumFunction = (t, u) -> t + u;
            java.util.function.Function<Integer, String> after = val -> "Sum is " + val;

            ByteBiFunction<String> chained = sumFunction.andThen(after);
            assertEquals("Sum is 20", chained.apply((byte) 12, (byte) 8));
        }
    }

    @Nested
    public class ByteBinaryOperatorTest {
        @Test
        public void testApplyAsByte() {
            ByteBinaryOperator adder = (left, right) -> (byte) (left + right);
            assertEquals((byte) 5, adder.applyAsByte((byte) 2, (byte) 3));
            assertEquals((byte) 127, adder.applyAsByte((byte) 120, (byte) 7));
        }
    }

    @Nested
    public class ByteBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(ByteBiPredicate.ALWAYS_TRUE.test((byte) 1, (byte) 2));
            assertFalse(ByteBiPredicate.ALWAYS_FALSE.test((byte) 1, (byte) 2));
            assertTrue(ByteBiPredicate.EQUAL.test((byte) 5, (byte) 5));
            assertFalse(ByteBiPredicate.EQUAL.test((byte) 5, (byte) 6));
            assertTrue(ByteBiPredicate.NOT_EQUAL.test((byte) 5, (byte) 6));
            assertFalse(ByteBiPredicate.NOT_EQUAL.test((byte) 5, (byte) 5));
            assertTrue(ByteBiPredicate.GREATER_THAN.test((byte) 6, (byte) 5));
            assertFalse(ByteBiPredicate.GREATER_THAN.test((byte) 5, (byte) 5));
            assertTrue(ByteBiPredicate.GREATER_EQUAL.test((byte) 5, (byte) 5));
            assertFalse(ByteBiPredicate.GREATER_EQUAL.test((byte) 4, (byte) 5));
            assertTrue(ByteBiPredicate.LESS_THAN.test((byte) 4, (byte) 5));
            assertFalse(ByteBiPredicate.LESS_THAN.test((byte) 5, (byte) 5));
            assertTrue(ByteBiPredicate.LESS_EQUAL.test((byte) 5, (byte) 5));
            assertFalse(ByteBiPredicate.LESS_EQUAL.test((byte) 6, (byte) 5));
        }

        @Test
        public void testNegate() {
            ByteBiPredicate notEqual = ByteBiPredicate.EQUAL.negate();
            assertTrue(notEqual.test((byte) 1, (byte) 2));
            assertFalse(notEqual.test((byte) 1, (byte) 1));
        }

        @Test
        public void testAnd() {
            ByteBiPredicate combined = ByteBiPredicate.GREATER_THAN.and((t, u) -> (t + u) > 20);
            assertTrue(combined.test((byte) 15, (byte) 10));
            assertFalse(combined.test((byte) 15, (byte) 4));
            assertFalse(combined.test((byte) 10, (byte) 15));
        }

        @Test
        public void testOr() {
            ByteBiPredicate combined = ByteBiPredicate.LESS_THAN.or(ByteBiPredicate.EQUAL);
            assertTrue(combined.test((byte) 4, (byte) 5));
            assertTrue(combined.test((byte) 5, (byte) 5));
            assertFalse(combined.test((byte) 6, (byte) 5));
        }
    }

    @Nested
    public class ByteConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Byte> result = new AtomicReference<>();
            ByteConsumer consumer = result::set;
            consumer.accept((byte) 123);
            assertEquals((byte) 123, result.get());
        }

        @Test
        public void testAndThen() {
            List<Byte> results = new ArrayList<>();
            ByteConsumer first = results::add;
            ByteConsumer second = b -> results.add((byte) (b * 2));
            ByteConsumer chained = first.andThen(second);
            chained.accept((byte) 10);

            assertEquals(2, results.size());
            assertEquals((byte) 10, results.get(0));
            assertEquals((byte) 20, results.get(1));
        }
    }

    @Nested
    public class ByteFunctionTest {
        @Test
        public void testStaticBox() {
            assertEquals(Byte.valueOf((byte) 99), ByteFunction.BOX.apply((byte) 99));
        }

        @Test
        public void testStaticIdentity() {
            ByteFunction<Byte> identity = ByteFunction.identity();
            assertEquals((byte) 42, identity.apply((byte) 42));
        }

        @Test
        public void testAndThen() {
            ByteFunction<String> initial = val -> "Byte:" + val;
            java.util.function.Function<String, Integer> after = String::length;
            ByteFunction<Integer> chained = initial.andThen(after);

            assertEquals(7, chained.apply((byte) 10));
            assertEquals(8, chained.apply((byte) 100));
        }
    }

    @Nested
    public class ByteNConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Integer> sum = new AtomicReference<>(0);
            ByteNConsumer consumer = args -> {
                int currentSum = 0;
                for (byte b : args)
                    currentSum += b;
                sum.set(currentSum);
            };
            consumer.accept((byte) 1, (byte) 2, (byte) 3);
            assertEquals(6, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ByteNConsumer first = args -> order.add("first:" + Arrays.toString(args));
            ByteNConsumer second = args -> order.add("second:" + Arrays.toString(args));
            ByteNConsumer chained = first.andThen(second);
            chained.accept((byte) 8, (byte) 9);

            assertEquals(2, order.size());
            assertEquals("first:[8, 9]", order.get(0));
            assertEquals("second:[8, 9]", order.get(1));
        }
    }

    @Nested
    public class ByteNFunctionTest {
        @Test
        public void testApply() {
            ByteNFunction<Integer> sumFunction = args -> {
                int sum = 0;
                for (byte b : args)
                    sum += b;
                return sum;
            };
            assertEquals(10, sumFunction.apply((byte) 1, (byte) 2, (byte) 3, (byte) 4));
        }

        @Test
        public void testAndThen() {
            ByteNFunction<Integer> sumFunction = args -> {
                int sum = 0;
                for (byte b : args)
                    sum += b;
                return sum;
            };
            java.util.function.Function<Integer, String> after = val -> "Total=" + val;
            ByteNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Total=6", chained.apply((byte) 1, (byte) 2, (byte) 3));
        }
    }

    @Nested
    public class BytePredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(BytePredicate.ALWAYS_TRUE.test((byte) 0));
            assertFalse(BytePredicate.ALWAYS_FALSE.test((byte) 0));
            assertTrue(BytePredicate.IS_ZERO.test((byte) 0));
            assertFalse(BytePredicate.IS_ZERO.test((byte) 1));
            assertTrue(BytePredicate.NOT_ZERO.test((byte) 1));
            assertFalse(BytePredicate.NOT_ZERO.test((byte) 0));
            assertTrue(BytePredicate.IS_POSITIVE.test((byte) 1));
            assertFalse(BytePredicate.IS_POSITIVE.test((byte) 0));
            assertFalse(BytePredicate.IS_POSITIVE.test((byte) -1));
            assertTrue(BytePredicate.NOT_POSITIVE.test((byte) 0));
            assertTrue(BytePredicate.NOT_POSITIVE.test((byte) -1));
            assertFalse(BytePredicate.NOT_POSITIVE.test((byte) 1));
            assertTrue(BytePredicate.IS_NEGATIVE.test((byte) -1));
            assertFalse(BytePredicate.IS_NEGATIVE.test((byte) 0));
            assertTrue(BytePredicate.NOT_NEGATIVE.test((byte) 0));
            assertFalse(BytePredicate.NOT_NEGATIVE.test((byte) -1));
        }

        @Test
        public void testStaticOf() {
            BytePredicate pred = b -> b > 0;
            assertSame(pred, BytePredicate.of(pred));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(BytePredicate.equal((byte) 10).test((byte) 10));
            assertFalse(BytePredicate.equal((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.notEqual((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.greaterThan((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.greaterEqual((byte) 10).test((byte) 10));
            assertTrue(BytePredicate.lessThan((byte) 10).test((byte) 9));
            assertTrue(BytePredicate.lessEqual((byte) 10).test((byte) 10));
            assertTrue(BytePredicate.between((byte) 10, (byte) 20).test((byte) 15));
            assertFalse(BytePredicate.between((byte) 10, (byte) 20).test((byte) 10));
            assertFalse(BytePredicate.between((byte) 10, (byte) 20).test((byte) 20));
        }

        @Test
        public void testNegateAndOr() {
            BytePredicate isPositive = BytePredicate.IS_POSITIVE;
            BytePredicate isZero = BytePredicate.IS_ZERO;
            BytePredicate isNegative = isPositive.negate().and(isZero.negate());

            assertTrue(isNegative.test((byte) -5));
            assertFalse(isNegative.test((byte) 0));
            assertFalse(isNegative.test((byte) 5));

            BytePredicate isZeroOrPositive = isZero.or(isPositive);
            assertTrue(isZeroOrPositive.test((byte) 0));
            assertTrue(isZeroOrPositive.test((byte) 5));
            assertFalse(isZeroOrPositive.test((byte) -5));
        }
    }

    @Nested
    public class ByteSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals((byte) 0, ByteSupplier.ZERO.getAsByte());
            byte randomByte = ByteSupplier.RANDOM.getAsByte();
            assertTrue(randomByte >= Byte.MIN_VALUE && randomByte <= Byte.MAX_VALUE);
        }

        @Test
        public void testGetAsByte() {
            ByteSupplier supplier = () -> (byte) 127;
            assertEquals((byte) 127, supplier.getAsByte());
        }
    }

    @Nested
    public class ByteTernaryOperatorTest {
        @Test
        public void testApplyAsByte() {
            ByteTernaryOperator conditional = (a, b, c) -> a > 0 ? b : c;
            assertEquals((byte) 20, conditional.applyAsByte((byte) 10, (byte) 20, (byte) 30));
            assertEquals((byte) 30, conditional.applyAsByte((byte) -10, (byte) 20, (byte) 30));
        }
    }

    @Nested
    public class ByteToBooleanFunctionTest {
        @Test
        public void testDefault() {
            assertTrue(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 1));
            assertTrue(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 127));
            assertFalse(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 0));
            assertFalse(ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) -1));
        }

        @Test
        public void testApplyAsBoolean() {
            ByteToBooleanFunction isEven = value -> value % 2 == 0;
            assertTrue(isEven.applyAsBoolean((byte) 10));
            assertFalse(isEven.applyAsBoolean((byte) 11));
            assertTrue(isEven.applyAsBoolean((byte) 0));
        }
    }

    @Nested
    public class ByteToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(5, ByteToIntFunction.DEFAULT.applyAsInt((byte) 5));
            assertEquals(-5, ByteToIntFunction.DEFAULT.applyAsInt((byte) -5));
        }

        @Test
        public void testApplyAsInt() {
            ByteToIntFunction square = value -> value * value;
            assertEquals(100, square.applyAsInt((byte) 10));
            assertEquals(25, square.applyAsInt((byte) -5));
        }
    }

    @Nested
    public class ByteTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Integer> sum = new AtomicReference<>();
            ByteTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept((byte) 1, (byte) 2, (byte) 3);
            assertEquals(6, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ByteTriConsumer first = (a, b, c) -> order.add("first");
            ByteTriConsumer second = (a, b, c) -> order.add("second");
            ByteTriConsumer chained = first.andThen(second);

            chained.accept((byte) 1, (byte) 2, (byte) 3);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class ByteTriFunctionTest {
        @Test
        public void testApply() {
            ByteTriFunction<Long> sumFunction = (a, b, c) -> (long) a + b + c;
            assertEquals(6L, sumFunction.apply((byte) 1, (byte) 2, (byte) 3));
        }

        @Test
        public void testAndThen() {
            ByteTriFunction<Integer> sumFunction = (a, b, c) -> a + b + c;
            java.util.function.Function<Integer, String> after = val -> "Sum: " + val;
            ByteTriFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum: 30", chained.apply((byte) 10, (byte) 5, (byte) 15));
        }
    }

    @Nested
    public class ByteTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(ByteTriPredicate.ALWAYS_TRUE.test((byte) 1, (byte) 2, (byte) 3));
            assertFalse(ByteTriPredicate.ALWAYS_FALSE.test((byte) 1, (byte) 2, (byte) 3));
        }

        @Test
        public void testTest() {
            ByteTriPredicate sumEquals = (a, b, c) -> (a + b) == c;
            assertTrue(sumEquals.test((byte) 5, (byte) 10, (byte) 15));
            assertFalse(sumEquals.test((byte) 5, (byte) 10, (byte) 16));
        }

        @Test
        public void testNegate() {
            ByteTriPredicate sumEquals = (a, b, c) -> (a + b) == c;
            ByteTriPredicate sumNotEquals = sumEquals.negate();
            assertFalse(sumNotEquals.test((byte) 5, (byte) 10, (byte) 15));
            assertTrue(sumNotEquals.test((byte) 5, (byte) 10, (byte) 16));
        }

        @Test
        public void testAnd() {
            ByteTriPredicate sumIsPositive = (a, b, c) -> (a + b + c) > 0;
            ByteTriPredicate allArePositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
            ByteTriPredicate combined = sumIsPositive.and(allArePositive);

            assertTrue(combined.test((byte) 1, (byte) 2, (byte) 3));
            assertFalse(combined.test((byte) 10, (byte) 20, (byte) -5));
            assertFalse(combined.test((byte) -10, (byte) -20, (byte) 5));
        }

        @Test
        public void testOr() {
            ByteTriPredicate anyIsZero = (a, b, c) -> a == 0 || b == 0 || c == 0;
            ByteTriPredicate allAreSame = (a, b, c) -> a == b && b == c;
            ByteTriPredicate combined = anyIsZero.or(allAreSame);

            assertTrue(combined.test((byte) 1, (byte) 0, (byte) 3));
            assertTrue(combined.test((byte) 5, (byte) 5, (byte) 5));
            assertFalse(combined.test((byte) 1, (byte) 2, (byte) 3));
        }
    }

    @Nested
    public class ByteUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            ByteUnaryOperator identity = ByteUnaryOperator.identity();
            assertEquals((byte) 42, identity.applyAsByte((byte) 42));
        }

        @Test
        public void testApplyAsByte() {
            ByteUnaryOperator increment = op -> (byte) (op + 1);
            assertEquals((byte) 11, increment.applyAsByte((byte) 10));
        }

        @Test
        public void testCompose() {
            ByteUnaryOperator increment = op -> (byte) (op + 1);
            ByteUnaryOperator doubleIt = op -> (byte) (op * 2);
            ByteUnaryOperator composed = doubleIt.compose(increment);

            assertEquals((byte) 22, composed.applyAsByte((byte) 10));
        }

        @Test
        public void testAndThen() {
            ByteUnaryOperator increment = op -> (byte) (op + 1);
            ByteUnaryOperator doubleIt = op -> (byte) (op * 2);
            ByteUnaryOperator chained = increment.andThen(doubleIt);

            assertEquals((byte) 22, chained.applyAsByte((byte) 10));
        }
    }

    @Nested
    public class CallableTest {
        @Test
        public void testCall() {
            Callable<String> callable = () -> "Success";
            assertEquals("Success", callable.call());
        }

        @Test
        public void testCallWithException() {
            Callable<String> callable = () -> {
                throw new IllegalStateException("error");
            };
            assertThrows(IllegalStateException.class, callable::call);
        }

        @Test
        public void testToRunnable() {
            AtomicBoolean called = new AtomicBoolean(false);
            Callable<String> callable = () -> {
                called.set(true);
                return "done";
            };
            Runnable runnable = callable.toRunnable();
            runnable.run();
            assertTrue(called.get());
        }

        @Test
        public void testToThrowable() throws Throwable {
            Callable<Integer> callable = () -> 1;
            Throwables.Callable<Integer, ?> throwableCallable = callable.toThrowable();
            assertNotNull(throwableCallable);
            assertEquals(1, throwableCallable.call());
        }
    }

    @Nested
    public class CharBiConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            CharBiConsumer consumer = (t, u) -> result.set("" + t + u);
            consumer.accept('a', 'b');
            assertEquals("ab", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            CharBiConsumer first = (t, u) -> results.add("first:" + t + u);
            CharBiConsumer second = (t, u) -> results.add("second:" + t + u);
            CharBiConsumer chained = first.andThen(second);

            chained.accept('x', 'y');
            assertEquals(2, results.size());
            assertEquals("first:xy", results.get(0));
            assertEquals("second:xy", results.get(1));
        }
    }

    @Nested
    public class CharBiFunctionTest {
        @Test
        public void testApply() {
            CharBiFunction<String> function = (t, u) -> String.valueOf(t) + u;
            assertEquals("ab", function.apply('a', 'b'));
        }

        @Test
        public void testAndThen() {
            CharBiFunction<String> initial = (t, u) -> "" + t + u;
            java.util.function.Function<String, Integer> after = String::length;
            CharBiFunction<Integer> chained = initial.andThen(after);

            assertEquals(2, chained.apply('1', '2'));
        }
    }

    @Nested
    public class CharBinaryOperatorTest {
        @Test
        public void testApplyAsChar() {
            CharBinaryOperator maxChar = (left, right) -> left > right ? left : right;
            assertEquals('z', maxChar.applyAsChar('a', 'z'));
            assertEquals('c', maxChar.applyAsChar('c', 'a'));
        }
    }

    @Nested
    public class CharBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(CharBiPredicate.ALWAYS_TRUE.test('a', 'b'));
            assertFalse(CharBiPredicate.ALWAYS_FALSE.test('a', 'b'));
            assertTrue(CharBiPredicate.EQUAL.test('x', 'x'));
            assertFalse(CharBiPredicate.EQUAL.test('x', 'y'));
            assertTrue(CharBiPredicate.NOT_EQUAL.test('x', 'y'));
            assertTrue(CharBiPredicate.GREATER_THAN.test('b', 'a'));
            assertTrue(CharBiPredicate.GREATER_EQUAL.test('b', 'b'));
            assertTrue(CharBiPredicate.LESS_THAN.test('a', 'b'));
            assertTrue(CharBiPredicate.LESS_EQUAL.test('a', 'a'));
        }

        @Test
        public void testNegateAndOr() {
            CharBiPredicate isEqual = (t, u) -> t == u;
            CharBiPredicate isCaseInsensitiveEqual = (t, u) -> Character.toLowerCase(t) == Character.toLowerCase(u);

            CharBiPredicate combined = isEqual.or(isCaseInsensitiveEqual);
            assertTrue(combined.test('a', 'a'));
            assertTrue(combined.test('a', 'A'));
            assertFalse(combined.test('a', 'b'));

            CharBiPredicate bothConditionsFalse = combined.negate();
            assertTrue(bothConditionsFalse.test('a', 'b'));

            CharBiPredicate bothMustBeTrue = isEqual.and(isCaseInsensitiveEqual);
            assertTrue(bothMustBeTrue.test('a', 'a'));
            assertFalse(bothMustBeTrue.test('a', 'A'));
        }
    }

    @Nested
    public class CharConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Character> ref = new AtomicReference<>();
            CharConsumer consumer = ref::set;
            consumer.accept('z');
            assertEquals('z', ref.get());
        }

        @Test
        public void testAndThen() {
            StringBuilder sb = new StringBuilder();
            CharConsumer first = sb::append;
            CharConsumer second = c -> sb.append(Character.toUpperCase(c));
            CharConsumer chained = first.andThen(second);
            chained.accept('h');
            assertEquals("hH", sb.toString());
        }
    }

    @Nested
    public class CharFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Character.valueOf('c'), CharFunction.BOX.apply('c'));
            assertEquals('k', CharFunction.identity().apply('k'));
        }

        @Test
        public void testAndThen() {
            CharFunction<String> toString = String::valueOf;
            java.util.function.Function<String, String> repeat = s -> s + s;
            CharFunction<String> chained = toString.andThen(repeat);
            assertEquals("aa", chained.apply('a'));
        }
    }

    @Nested
    public class CharNConsumerTest {
        @Test
        public void testAccept() {
            StringBuilder sb = new StringBuilder();
            CharNConsumer consumer = sb::append;
            consumer.accept('h', 'e', 'l', 'l', 'o');
            assertEquals("hello", sb.toString());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            CharNConsumer first = args -> results.add("first:" + new String(args));
            CharNConsumer second = args -> results.add("second:" + new String(args));
            CharNConsumer chained = first.andThen(second);
            chained.accept('a', 'b');
            assertEquals(2, results.size());
            assertEquals("first:ab", results.get(0));
            assertEquals("second:ab", results.get(1));
        }
    }

    @Nested
    public class CharNFunctionTest {
        @Test
        public void testApply() {
            CharNFunction<String> asString = String::new;
            assertEquals("world", asString.apply('w', 'o', 'r', 'l', 'd'));
        }

        @Test
        public void testAndThen() {
            CharNFunction<String> asString = String::new;
            java.util.function.Function<String, Integer> length = String::length;
            CharNFunction<Integer> chained = asString.andThen(length);
            assertEquals(4, chained.apply('j', 'a', 'v', 'a'));
        }
    }

    @Nested
    public class CharPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(CharPredicate.ALWAYS_TRUE.test(' '));
            assertFalse(CharPredicate.ALWAYS_FALSE.test(' '));
            assertTrue(CharPredicate.IS_ZERO.test('\0'));
            assertFalse(CharPredicate.IS_ZERO.test('a'));
            assertTrue(CharPredicate.NOT_ZERO.test('a'));
            assertFalse(CharPredicate.NOT_ZERO.test('\0'));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(CharPredicate.equal('a').test('a'));
            assertTrue(CharPredicate.notEqual('a').test('b'));
            assertTrue(CharPredicate.greaterThan('b').test('c'));
            assertTrue(CharPredicate.greaterEqual('b').test('b'));
            assertTrue(CharPredicate.lessThan('b').test('a'));
            assertTrue(CharPredicate.lessEqual('b').test('b'));
            assertTrue(CharPredicate.between('a', 'e').test('c'));
            assertFalse(CharPredicate.between('a', 'e').test('a'));
            assertFalse(CharPredicate.between('a', 'e').test('e'));
        }

        @Test
        public void testStaticOf() {
            CharPredicate isDigit = Character::isDigit;
            assertSame(isDigit, CharPredicate.of(isDigit));
        }

        @Test
        public void testNegateAndOr() {
            CharPredicate isDigit = Character::isDigit;
            CharPredicate isLetter = Character::isLetter;
            CharPredicate isLetterOrDigit = isLetter.or(isDigit);
            CharPredicate isNotLetterOrDigit = isLetterOrDigit.negate();

            assertTrue(isLetterOrDigit.test('a'));
            assertTrue(isLetterOrDigit.test('1'));
            assertFalse(isLetterOrDigit.test('.'));

            assertTrue(isNotLetterOrDigit.test('.'));
            assertFalse(isNotLetterOrDigit.test('a'));
        }
    }

    @Nested
    public class CharSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals('\0', CharSupplier.ZERO.getAsChar());
            char randomChar = CharSupplier.RANDOM.getAsChar();
            assertTrue(Character.isDefined(randomChar));
        }

        @Test
        public void testGetAsChar() {
            CharSupplier supplier = () -> 'x';
            assertEquals('x', supplier.getAsChar());
        }
    }

    @Nested
    public class CharTernaryOperatorTest {
        @Test
        public void testApplyAsChar() {
            CharTernaryOperator conditional = (a, b, c) -> Character.isDigit(a) ? b : c;
            assertEquals('Y', conditional.applyAsChar('1', 'Y', 'N'));
            assertEquals('N', conditional.applyAsChar('a', 'Y', 'N'));
        }
    }

    @Nested
    public class CharToBooleanFunctionTest {
        @Test
        public void testDefault() {
            assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('Y'));
            assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('y'));
            assertTrue(CharToBooleanFunction.DEFAULT.applyAsBoolean('1'));
            assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('N'));
            assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('0'));
            assertFalse(CharToBooleanFunction.DEFAULT.applyAsBoolean('x'));
        }

        @Test
        public void testApplyAsBoolean() {
            CharToBooleanFunction isWhitespace = Character::isWhitespace;
            assertTrue(isWhitespace.applyAsBoolean(' '));
            assertTrue(isWhitespace.applyAsBoolean('\t'));
            assertFalse(isWhitespace.applyAsBoolean('a'));
        }
    }

    @Nested
    public class CharToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(65, CharToIntFunction.DEFAULT.applyAsInt('A'));
            assertEquals(97, CharToIntFunction.DEFAULT.applyAsInt('a'));
        }

        @Test
        public void testApplyAsInt() {
            CharToIntFunction numericValue = Character::getNumericValue;
            assertEquals(5, numericValue.applyAsInt('5'));
            assertEquals(9, numericValue.applyAsInt('9'));
            assertEquals(10, numericValue.applyAsInt('a'));
        }
    }

    @Nested
    public class CharTriConsumerTest {
        @Test
        public void testAccept() {
            StringBuilder sb = new StringBuilder();
            CharTriConsumer consumer = (a, b, c) -> sb.append(a).append(b).append(c);
            consumer.accept('a', 'b', 'c');
            assertEquals("abc", sb.toString());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            CharTriConsumer first = (a, b, c) -> results.add("first:" + a + b + c);
            CharTriConsumer second = (a, b, c) -> results.add("second:" + a + b + c);
            CharTriConsumer chained = first.andThen(second);

            chained.accept('x', 'y', 'z');
            assertEquals(2, results.size());
            assertEquals("first:xyz", results.get(0));
            assertEquals("second:xyz", results.get(1));
        }
    }

    @Nested
    public class CharTriFunctionTest {
        @Test
        public void testApply() {
            CharTriFunction<String> asString = (a, b, c) -> "" + a + b + c;
            assertEquals("123", asString.apply('1', '2', '3'));
        }

        @Test
        public void testAndThen() {
            CharTriFunction<String> asString = (a, b, c) -> "" + a + b + c;
            java.util.function.Function<String, Integer> length = String::length;
            CharTriFunction<Integer> chained = asString.andThen(length);

            assertEquals(3, chained.apply('a', 'b', 'c'));
        }
    }

    @Nested
    public class CharTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(CharTriPredicate.ALWAYS_TRUE.test('a', 'b', 'c'));
            assertFalse(CharTriPredicate.ALWAYS_FALSE.test('a', 'b', 'c'));
        }

        @Test
        public void testTest() {
            CharTriPredicate alphabetical = (a, b, c) -> a <= b && b <= c;
            assertTrue(alphabetical.test('a', 'b', 'c'));
            assertTrue(alphabetical.test('a', 'a', 'c'));
            assertFalse(alphabetical.test('c', 'b', 'a'));
        }

        @Test
        public void testNegate() {
            CharTriPredicate alphabetical = (a, b, c) -> a <= b && b <= c;
            CharTriPredicate notAlphabetical = alphabetical.negate();
            assertFalse(notAlphabetical.test('a', 'b', 'c'));
            assertTrue(notAlphabetical.test('c', 'b', 'a'));
        }

        @Test
        public void testAnd() {
            CharTriPredicate alphabetical = (a, b, c) -> a <= b && b <= c;
            CharTriPredicate allDifferent = (a, b, c) -> a != b && b != c && a != c;
            CharTriPredicate combined = alphabetical.and(allDifferent);

            assertTrue(combined.test('a', 'b', 'c'));
            assertFalse(combined.test('a', 'a', 'c'));
            assertFalse(combined.test('c', 'b', 'a'));
        }

        @Test
        public void testOr() {
            CharTriPredicate alphabetical = (a, b, c) -> a <= b && b <= c;
            CharTriPredicate allSame = (a, b, c) -> a == b && b == c;
            CharTriPredicate combined = alphabetical.or(allSame);

            assertTrue(combined.test('a', 'b', 'c'));
            assertTrue(combined.test('x', 'x', 'x'));
            assertFalse(combined.test('c', 'b', 'a'));
        }
    }

    @Nested
    public class CharUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            CharUnaryOperator identity = CharUnaryOperator.identity();
            assertEquals('A', identity.applyAsChar('A'));
        }

        @Test
        public void testApplyAsChar() {
            CharUnaryOperator toUpper = Character::toUpperCase;
            assertEquals('A', toUpper.applyAsChar('a'));
            assertEquals('Z', toUpper.applyAsChar('Z'));
        }

        @Test
        public void testCompose() {
            CharUnaryOperator nextChar = op -> (char) (op + 1);
            CharUnaryOperator toUpper = Character::toUpperCase;
            CharUnaryOperator composed = toUpper.compose(nextChar);

            assertEquals('B', composed.applyAsChar('a'));
        }

        @Test
        public void testAndThen() {
            CharUnaryOperator toUpper = Character::toUpperCase;
            CharUnaryOperator nextChar = op -> (char) (op + 1);
            CharUnaryOperator chained = toUpper.andThen(nextChar);

            assertEquals('B', chained.applyAsChar('a'));
        }
    }

    @Nested
    public class ConsumerTest {
        @Test
        public void testAccept() {
            List<String> list = new ArrayList<>();
            Consumer<String> consumer = list::add;
            consumer.accept("hello");
            assertEquals(1, list.size());
            assertEquals("hello", list.get(0));
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            Consumer<String> consumer1 = s -> results.add("C1:" + s);
            Consumer<String> consumer2 = s -> results.add("C2:" + s);

            consumer1.andThen(consumer2).accept("test");

            assertEquals(2, results.size());
            assertEquals("C1:test", results.get(0));
            assertEquals("C2:test", results.get(1));
        }

        @Test
        public void testToThrowable() {
            Consumer<String> consumer = s -> {
            };
            Throwables.Consumer<String, ?> throwableConsumer = consumer.toThrowable();
            assertNotNull(throwableConsumer);
            Consumer<String> original = (Consumer<String>) throwableConsumer;
            assertNotNull(original);
        }
    }

    private static final double DELTA = 1e-9;

    @Nested
    public class DoubleBiConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Double> result = new AtomicReference<>();
            DoubleBiConsumer consumer = (t, u) -> result.set(t + u);
            consumer.accept(1.5, 2.5);
            assertEquals(4.0, result.get(), DELTA);
        }

        @Test
        public void testAndThen() {
            List<Double> results = new ArrayList<>();
            DoubleBiConsumer first = (t, u) -> results.add(t + u);
            DoubleBiConsumer second = (t, u) -> results.add(t * u);
            DoubleBiConsumer chained = first.andThen(second);

            chained.accept(2.0, 3.0);
            assertEquals(2, results.size());
            assertEquals(5.0, results.get(0), DELTA);
            assertEquals(6.0, results.get(1), DELTA);
        }
    }

    @Nested
    public class DoubleBiFunctionTest {
        @Test
        public void testApply() {
            DoubleBiFunction<String> function = (t, u) -> t + " " + u;
            assertEquals("3.14 2.71", function.apply(3.14, 2.71));
        }

        @Test
        public void testAndThen() {
            DoubleBiFunction<Double> sumFunction = Double::sum;
            java.util.function.Function<Double, String> after = val -> "Result: " + val;
            DoubleBiFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Result: 7.7", chained.apply(5.5, 2.2));
        }
    }

    @Nested
    public class DoubleBinaryOperatorTest {
        @Test
        public void testApplyAsDouble() {
            DoubleBinaryOperator multiplier = (left, right) -> left * right;
            assertEquals(10.0, multiplier.applyAsDouble(2.5, 4.0), DELTA);
        }
    }

    @Nested
    public class DoubleBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(DoubleBiPredicate.ALWAYS_TRUE.test(1.0, 2.0));
            assertFalse(DoubleBiPredicate.ALWAYS_FALSE.test(1.0, 2.0));

            assertTrue(DoubleBiPredicate.EQUAL.test(1.0, 1.0));
            assertTrue(DoubleBiPredicate.EQUAL.test(Double.NaN, Double.NaN));
            assertFalse(DoubleBiPredicate.EQUAL.test(1.0, 2.0));
            assertFalse(DoubleBiPredicate.EQUAL.test(0.0, -0.0));

            assertTrue(DoubleBiPredicate.NOT_EQUAL.test(1.0, 2.0));
            assertFalse(DoubleBiPredicate.NOT_EQUAL.test(Double.NaN, Double.NaN));

            assertTrue(DoubleBiPredicate.GREATER_THAN.test(2.0, 1.0));
            assertFalse(DoubleBiPredicate.GREATER_THAN.test(1.0, 1.0));

            assertTrue(DoubleBiPredicate.GREATER_EQUAL.test(1.0, 1.0));
            assertFalse(DoubleBiPredicate.GREATER_EQUAL.test(0.0, 1.0));

            assertTrue(DoubleBiPredicate.LESS_THAN.test(1.0, 2.0));
            assertTrue(DoubleBiPredicate.LESS_EQUAL.test(1.0, 1.0));
        }

        @Test
        public void testNegateAndOr() {
            DoubleBiPredicate isClose = (t, u) -> Math.abs(t - u) < 0.1;
            DoubleBiPredicate isFar = isClose.negate();
            DoubleBiPredicate bothPositive = (t, u) -> t > 0 && u > 0;

            assertTrue(isClose.test(1.0, 1.05));
            assertFalse(isFar.test(1.0, 1.05));

            DoubleBiPredicate combined = isClose.or(bothPositive);
            assertTrue(combined.test(1.0, 1.05));
            assertTrue(combined.test(5.0, 10.0));
            assertFalse(combined.test(-1.0, -2.0));
        }
    }

    @Nested
    public class DoubleConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<Double> firstRef = new AtomicReference<>();
            AtomicReference<Double> secondRef = new AtomicReference<>();
            DoubleConsumer first = firstRef::set;
            java.util.function.DoubleConsumer second = secondRef::set;

            first.andThen(second).accept(Math.PI);

            assertEquals(Math.PI, firstRef.get(), DELTA);
            assertEquals(Math.PI, secondRef.get(), DELTA);
        }
    }

    @Nested
    public class DoubleFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Double.valueOf(1.23), DoubleFunction.BOX.apply(1.23));
            assertEquals(42.0, DoubleFunction.identity().apply(42.0), DELTA);
        }

        @Test
        public void testAndThen() {
            DoubleFunction<String> toString = String::valueOf;
            java.util.function.Function<String, Integer> length = String::length;
            DoubleFunction<Integer> chained = toString.andThen(length);
            assertEquals(4, chained.apply(3.14));
        }
    }

    @Nested
    public class DoubleMapMultiConsumerTest {
        @Test
        public void testAccept() {
            List<Double> results = new ArrayList<>();
            DoubleMapMultiConsumer multiConsumer = (value, downstream) -> {
                downstream.accept(value);
                downstream.accept(-value);
            };

            multiConsumer.accept(5.5, results::add);

            assertEquals(2, results.size());
            assertEquals(5.5, results.get(0), DELTA);
            assertEquals(-5.5, results.get(1), DELTA);
        }
    }

    @Nested
    public class DoubleNConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Double> sum = new AtomicReference<>(0.0);
            DoubleNConsumer consumer = args -> {
                double currentSum = 0;
                for (double d : args)
                    currentSum += d;
                sum.set(currentSum);
            };
            consumer.accept(1.1, 2.2, 3.3);
            assertEquals(6.6, sum.get(), DELTA);
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            DoubleNConsumer first = args -> order.add("first:" + Arrays.toString(args));
            DoubleNConsumer second = args -> order.add("second:" + Arrays.toString(args));
            DoubleNConsumer chained = first.andThen(second);
            chained.accept(1.0, 2.0);

            assertEquals(2, order.size());
            assertEquals("first:[1.0, 2.0]", order.get(0));
            assertEquals("second:[1.0, 2.0]", order.get(1));
        }
    }

    @Nested
    public class DoubleNFunctionTest {
        @Test
        public void testApply() {
            DoubleNFunction<Double> productFunction = args -> {
                double product = 1.0;
                for (double d : args)
                    product *= d;
                return product;
            };
            assertEquals(24.0, productFunction.apply(1.0, 2.0, 3.0, 4.0), DELTA);
        }

        @Test
        public void testAndThen() {
            DoubleNFunction<Double> sumFunction = args -> {
                double sum = 0;
                for (double d : args)
                    sum += d;
                return sum;
            };
            java.util.function.Function<Double, String> after = val -> "Sum=" + val;
            DoubleNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum=6.6", chained.apply(1.1, 2.2, 3.3));
        }
    }

    @Nested
    public class DoubleObjConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            DoubleObjConsumer<String> consumer = (d, s) -> result.set(s + ":" + d);
            consumer.accept(3.14, "PI");
            assertEquals("PI:3.14", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> results = new ArrayList<>();
            DoubleObjConsumer<String> first = (d, s) -> results.add("first:" + s + d);
            DoubleObjConsumer<String> second = (d, s) -> results.add("second:" + s + d);
            DoubleObjConsumer<String> chained = first.andThen(second);

            chained.accept(1.1, "val");
            assertEquals(2, results.size());
            assertEquals("first:val1.1", results.get(0));
            assertEquals("second:val1.1", results.get(1));
        }
    }

    @Nested
    public class DoubleObjFunctionTest {
        @Test
        public void testApply() {
            DoubleObjFunction<String, String> function = (d, s) -> s.repeat((int) d);
            assertEquals("aaa", function.apply(3.0, "a"));
        }

        @Test
        public void testAndThen() {
            DoubleObjFunction<Integer, String> initial = (d, i) -> d + ":" + i;
            java.util.function.Function<String, Integer> after = String::length;
            DoubleObjFunction<Integer, Integer> chained = initial.andThen(after);

            assertEquals(7, chained.apply(1.234, 5));
        }
    }

    @Nested
    public class DoubleObjPredicateTest {
        @Test
        public void testTest() {
            DoubleObjPredicate<String> predicate = (d, s) -> s.length() > d;
            assertTrue(predicate.test(4.0, "hello"));
            assertFalse(predicate.test(5.0, "hello"));
        }

        @Test
        public void testNegateAndOr() {
            DoubleObjPredicate<String> lengthIsGreaterThan = (d, s) -> s.length() > d;
            DoubleObjPredicate<String> lengthIsLessThanOrEqual = lengthIsGreaterThan.negate();
            DoubleObjPredicate<String> containsE = (d, s) -> s.contains("e");

            assertTrue(lengthIsLessThanOrEqual.test(5.0, "hello"));
            assertFalse(lengthIsLessThanOrEqual.test(4.9, "hello"));

            DoubleObjPredicate<String> orCombined = lengthIsGreaterThan.or(containsE);
            assertTrue(orCombined.test(10.0, "hello"));
            assertTrue(orCombined.test(4.0, "world"));

            DoubleObjPredicate<String> andCombined = lengthIsGreaterThan.and(containsE);
            assertTrue(andCombined.test(4.0, "hello"));
            assertFalse(andCombined.test(10.0, "hello"));
            assertFalse(andCombined.test(4.0, "world"));
        }
    }

    @Nested
    public class DoublePredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(DoublePredicate.ALWAYS_TRUE.test(0.0));
            assertFalse(DoublePredicate.ALWAYS_FALSE.test(0.0));
            assertTrue(DoublePredicate.IS_ZERO.test(0.0));
            assertFalse(DoublePredicate.IS_ZERO.test(1.0));
            assertTrue(DoublePredicate.NOT_ZERO.test(1.0));
            assertFalse(DoublePredicate.NOT_ZERO.test(0.0));
            assertTrue(DoublePredicate.IS_POSITIVE.test(1.0));
            assertFalse(DoublePredicate.IS_POSITIVE.test(0.0));
            assertTrue(DoublePredicate.NOT_POSITIVE.test(0.0));
            assertTrue(DoublePredicate.IS_NEGATIVE.test(-1.0));
            assertTrue(DoublePredicate.NOT_NEGATIVE.test(0.0));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(DoublePredicate.equal(10.5).test(10.5));
            assertTrue(DoublePredicate.notEqual(10.5).test(10.6));
            assertTrue(DoublePredicate.greaterThan(10.5).test(10.6));
            assertTrue(DoublePredicate.greaterEqual(10.5).test(10.5));
            assertTrue(DoublePredicate.lessThan(10.5).test(10.4));
            assertTrue(DoublePredicate.lessEqual(10.5).test(10.5));
            assertTrue(DoublePredicate.between(10.0, 20.0).test(15.0));
            assertFalse(DoublePredicate.between(10.0, 20.0).test(10.0));
        }

        @Test
        public void testStaticOf() {
            DoublePredicate isFinite = Double::isFinite;
            assertSame(isFinite, DoublePredicate.of(isFinite));
        }

        @Test
        public void testNegateAndOr() {
            DoublePredicate isFinite = Double::isFinite;
            DoublePredicate isInfinite = isFinite.negate();
            assertTrue(isInfinite.test(Double.POSITIVE_INFINITY));
            assertFalse(isInfinite.test(1.0));
        }
    }

    @Nested
    public class DoubleSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals(0.0, DoubleSupplier.ZERO.getAsDouble(), DELTA);
            double randomDouble = DoubleSupplier.RANDOM.getAsDouble();
            assertTrue(randomDouble >= 0.0 && randomDouble < 1.0);
        }

        @Test
        public void testGetAsDouble() {
            DoubleSupplier supplier = () -> Math.PI;
            assertEquals(Math.PI, supplier.getAsDouble(), DELTA);
        }
    }

    @Nested
    public class DoubleTernaryOperatorTest {
        @Test
        public void testApplyAsDouble() {
            DoubleTernaryOperator operator = (a, b, c) -> a > 0 ? b : (a < 0 ? c : 0);
            assertEquals(10.0, operator.applyAsDouble(1.0, 10.0, 20.0), DELTA);
            assertEquals(20.0, operator.applyAsDouble(-1.0, 10.0, 20.0), DELTA);
            assertEquals(0.0, operator.applyAsDouble(0.0, 10.0, 20.0), DELTA);
        }
    }

    @Nested
    public class DoubleToFloatFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.45f, DoubleToFloatFunction.DEFAULT.applyAsFloat(123.45d), (float) DELTA);
        }

        @Test
        public void testApplyAsFloat() {
            DoubleToFloatFunction custom = val -> (float) (val * 2.0);
            assertEquals(5.0f, custom.applyAsFloat(2.5d), (float) DELTA);
        }
    }

    @Nested
    public class DoubleToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123, DoubleToIntFunction.DEFAULT.applyAsInt(123.45d));
            assertEquals(-123, DoubleToIntFunction.DEFAULT.applyAsInt(-123.45d));
        }

        @Test
        public void testApplyAsInt() {
            DoubleToIntFunction rounding = val -> (int) Math.round(val);
            assertEquals(123, rounding.applyAsInt(123.45d));
            assertEquals(124, rounding.applyAsInt(123.55d));
        }
    }

    @Nested
    public class DoubleToLongFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123L, DoubleToLongFunction.DEFAULT.applyAsLong(123.45d));
            assertEquals(-123L, DoubleToLongFunction.DEFAULT.applyAsLong(-123.45d));
        }

        @Test
        public void testApplyAsLong() {
            DoubleToLongFunction floor = val -> (long) Math.floor(val);
            assertEquals(123L, floor.applyAsLong(123.99d));
            assertEquals(-124L, floor.applyAsLong(-123.01d));
        }
    }

    private static final double DOUBLE_DELTA = 1e-9;
    private static final float FLOAT_DELTA = 1e-6f;

    @Nested
    public class DoubleTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Double> sum = new AtomicReference<>();
            DoubleTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept(1.1, 2.2, 3.3);
            assertEquals(6.6, sum.get(), DOUBLE_DELTA);
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            DoubleTriConsumer first = (a, b, c) -> order.add("first");
            DoubleTriConsumer second = (a, b, c) -> order.add("second");
            DoubleTriConsumer chained = first.andThen(second);

            chained.accept(1.0, 2.0, 3.0);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class DoubleTriFunctionTest {
        @Test
        public void testApply() {
            DoubleTriFunction<String> function = (a, b, c) -> a + "," + b + "," + c;
            assertEquals("1.1,2.2,3.3", function.apply(1.1, 2.2, 3.3));
        }

        @Test
        public void testAndThen() {
            DoubleTriFunction<Double> sum = (a, b, c) -> a + b + c;
            java.util.function.Function<Double, String> toString = Object::toString;
            DoubleTriFunction<String> chained = sum.andThen(toString);
            assertEquals("6.6", chained.apply(1.1, 2.2, 3.3));
        }
    }

    @Nested
    public class DoubleTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(DoubleTriPredicate.ALWAYS_TRUE.test(1, 2, 3));
            assertFalse(DoubleTriPredicate.ALWAYS_FALSE.test(1, 2, 3));
        }

        @Test
        public void testNegateAndOr() {
            DoubleTriPredicate isPythagorean = (a, b, c) -> (a * a + b * b) == (c * c);
            DoubleTriPredicate isNotPythagorean = isPythagorean.negate();
            DoubleTriPredicate anyAreZero = (a, b, c) -> a == 0 || b == 0 || c == 0;

            assertTrue(isPythagorean.test(3, 4, 5));
            assertFalse(isNotPythagorean.test(3, 4, 5));
            assertTrue(isNotPythagorean.test(3, 4, 6));

            DoubleTriPredicate combined = isPythagorean.or(anyAreZero);
            assertTrue(combined.test(3, 4, 5));
            assertTrue(combined.test(0, 4, 6));
            assertFalse(combined.test(1, 2, 3));
        }
    }

    @Nested
    public class DoubleUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            assertEquals(42.0, DoubleUnaryOperator.identity().applyAsDouble(42.0), DOUBLE_DELTA);
        }

        @Test
        public void testComposeAndThen() {
            DoubleUnaryOperator addOne = d -> d + 1;
            java.util.function.DoubleUnaryOperator timesTwo = d -> d * 2;

            DoubleUnaryOperator composed = addOne.compose(timesTwo);
            assertEquals(21.0, composed.applyAsDouble(10.0), DOUBLE_DELTA);

            DoubleUnaryOperator chained = addOne.andThen(timesTwo);
            assertEquals(22.0, chained.applyAsDouble(10.0), DOUBLE_DELTA);
        }
    }

    @Nested
    public class FloatBiConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Float> result = new AtomicReference<>();
            FloatBiConsumer consumer = (t, u) -> result.set(t + u);
            consumer.accept(1.5f, 2.5f);
            assertEquals(4.0f, result.get(), FLOAT_DELTA);
        }

        @Test
        public void testAndThen() {
            List<Float> results = new ArrayList<>();
            FloatBiConsumer first = (t, u) -> results.add(t + u);
            FloatBiConsumer second = (t, u) -> results.add(t * u);
            FloatBiConsumer chained = first.andThen(second);

            chained.accept(2.0f, 3.0f);
            assertEquals(2, results.size());
            assertEquals(5.0f, results.get(0), FLOAT_DELTA);
            assertEquals(6.0f, results.get(1), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatBiFunctionTest {
        @Test
        public void testApply() {
            FloatBiFunction<String> function = (t, u) -> t + " " + u;
            assertEquals("3.14 2.71", function.apply(3.14f, 2.71f));
        }

        @Test
        public void testAndThen() {
            FloatBiFunction<Float> sumFunction = Float::sum;
            java.util.function.Function<Float, String> after = val -> "Result: " + val;
            FloatBiFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Result: 7.7", chained.apply(5.5f, 2.2f));
        }
    }

    @Nested
    public class FloatBinaryOperatorTest {
        @Test
        public void testApplyAsFloat() {
            FloatBinaryOperator multiplier = (left, right) -> left * right;
            assertEquals(10.0f, multiplier.applyAsFloat(2.5f, 4.0f), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(FloatBiPredicate.ALWAYS_TRUE.test(1.0f, 2.0f));
            assertFalse(FloatBiPredicate.ALWAYS_FALSE.test(1.0f, 2.0f));

            assertTrue(FloatBiPredicate.EQUAL.test(1.0f, 1.0f));
            assertTrue(FloatBiPredicate.EQUAL.test(Float.NaN, Float.NaN));
            assertFalse(FloatBiPredicate.EQUAL.test(0.0f, -0.0f));

            assertTrue(FloatBiPredicate.NOT_EQUAL.test(1.0f, 2.0f));
            assertFalse(FloatBiPredicate.NOT_EQUAL.test(Float.NaN, Float.NaN));

            assertTrue(FloatBiPredicate.GREATER_THAN.test(2.0f, 1.0f));
            assertTrue(FloatBiPredicate.GREATER_EQUAL.test(1.0f, 1.0f));
            assertTrue(FloatBiPredicate.LESS_THAN.test(1.0f, 2.0f));
            assertTrue(FloatBiPredicate.LESS_EQUAL.test(1.0f, 1.0f));
        }

        @Test
        public void testNegateAndOr() {
            FloatBiPredicate isClose = (t, u) -> Math.abs(t - u) < 0.1f;
            FloatBiPredicate isFar = isClose.negate();
            assertTrue(isFar.test(1.0f, 2.0f));
            assertFalse(isClose.and(isFar).test(1.0f, 2.0f));
            assertTrue(isClose.or(isFar).test(1.0f, 2.0f));
        }
    }

    @Nested
    public class FloatConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<Float> firstRef = new AtomicReference<>();
            AtomicReference<Float> secondRef = new AtomicReference<>();
            FloatConsumer first = firstRef::set;
            FloatConsumer second = secondRef::set;

            first.andThen(second).accept(3.14f);

            assertEquals(3.14f, firstRef.get(), FLOAT_DELTA);
            assertEquals(3.14f, secondRef.get(), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Float.valueOf(1.23f), FloatFunction.BOX.apply(1.23f));
            assertEquals(42.0f, FloatFunction.identity().apply(42.0f), FLOAT_DELTA);
        }

        @Test
        public void testAndThen() {
            FloatFunction<String> toString = String::valueOf;
            java.util.function.Function<String, Integer> length = String::length;
            FloatFunction<Integer> chained = toString.andThen(length);
            assertEquals(4, chained.apply(3.14f));
        }
    }

    @Nested
    public class FloatNConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Float> sum = new AtomicReference<>(0.0f);
            FloatNConsumer consumer = args -> {
                float currentSum = 0;
                for (float f : args)
                    currentSum += f;
                sum.set(currentSum);
            };
            consumer.accept(1.1f, 2.2f, 3.3f);
            assertEquals(6.6f, sum.get(), FLOAT_DELTA);
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            FloatNConsumer first = args -> order.add("first:" + Arrays.toString(args));
            FloatNConsumer second = args -> order.add("second:" + Arrays.toString(args));
            FloatNConsumer chained = first.andThen(second);
            chained.accept(1.0f, 2.0f);

            assertEquals(2, order.size());
            assertEquals("first:[1.0, 2.0]", order.get(0));
            assertEquals("second:[1.0, 2.0]", order.get(1));
        }
    }

    @Nested
    public class FloatNFunctionTest {
        @Test
        public void testApply() {
            FloatNFunction<Float> productFunction = args -> {
                float product = 1.0f;
                for (float f : args)
                    product *= f;
                return product;
            };
            assertEquals(24.0f, productFunction.apply(1.0f, 2.0f, 3.0f, 4.0f), FLOAT_DELTA);
        }

        @Test
        public void testAndThen() {
            FloatNFunction<Float> sumFunction = args -> {
                float sum = 0;
                for (float f : args)
                    sum += f;
                return sum;
            };
            java.util.function.Function<Float, String> after = val -> "Sum=" + val;
            FloatNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum=6.6000004", chained.apply(1.1f, 2.2f, 3.3f));
        }
    }

    @Nested
    public class FloatPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(FloatPredicate.ALWAYS_TRUE.test(0f));
            assertFalse(FloatPredicate.ALWAYS_FALSE.test(0f));
            assertTrue(FloatPredicate.IS_ZERO.test(0f));
            assertTrue(FloatPredicate.NOT_ZERO.test(1f));
            assertTrue(FloatPredicate.IS_POSITIVE.test(1f));
            assertTrue(FloatPredicate.NOT_POSITIVE.test(0f));
            assertTrue(FloatPredicate.IS_NEGATIVE.test(-1f));
            assertTrue(FloatPredicate.NOT_NEGATIVE.test(0f));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(FloatPredicate.equal(10.5f).test(10.5f));
            assertTrue(FloatPredicate.notEqual(10.5f).test(10.6f));
            assertTrue(FloatPredicate.greaterThan(10.5f).test(10.6f));
            assertTrue(FloatPredicate.greaterEqual(10.5f).test(10.5f));
            assertTrue(FloatPredicate.lessThan(10.5f).test(10.4f));
            assertTrue(FloatPredicate.lessEqual(10.5f).test(10.5f));
            assertTrue(FloatPredicate.between(10f, 20f).test(15f));
            assertFalse(FloatPredicate.between(10f, 20f).test(10f));
        }

        @Test
        public void testStaticOf() {
            FloatPredicate isFinite = Float::isFinite;
            assertSame(isFinite, FloatPredicate.of(isFinite));
        }
    }

    @Nested
    public class FloatSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals(0.0f, FloatSupplier.ZERO.getAsFloat(), FLOAT_DELTA);
            float randomFloat = FloatSupplier.RANDOM.getAsFloat();
            assertTrue(randomFloat >= 0.0f && randomFloat < 1.0f);
        }

        @Test
        public void testGetAsFloat() {
            FloatSupplier supplier = () -> 3.14f;
            assertEquals(3.14f, supplier.getAsFloat(), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatTernaryOperatorTest {
        @Test
        public void testApplyAsFloat() {
            FloatTernaryOperator operator = (a, b, c) -> a > 0 ? b + c : b - c;
            assertEquals(30.0f, operator.applyAsFloat(1.0f, 10.0f, 20.0f), FLOAT_DELTA);
            assertEquals(-10.0f, operator.applyAsFloat(-1.0f, 10.0f, 20.0f), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatToDoubleFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.44999694824219d, FloatToDoubleFunction.DEFAULT.applyAsDouble(123.45f));
        }

        @Test
        public void testApplyAsDouble() {
            FloatToDoubleFunction custom = val -> (double) val * 2.0;
            assertEquals(5.0d, custom.applyAsDouble(2.5f), DOUBLE_DELTA);
        }
    }

    @Nested
    public class FloatToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123, FloatToIntFunction.DEFAULT.applyAsInt(123.45f));
        }

        @Test
        public void testApplyAsInt() {
            FloatToIntFunction rounding = val -> Math.round(val);
            assertEquals(123, rounding.applyAsInt(123.45f));
            assertEquals(124, rounding.applyAsInt(123.55f));
        }
    }

    @Nested
    public class FloatToLongFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123L, FloatToLongFunction.DEFAULT.applyAsLong(123.45f));
        }

        @Test
        public void testApplyAsLong() {
            FloatToLongFunction rounding = val -> Math.round(val);
            assertEquals(123L, rounding.applyAsLong(123.45f));
            assertEquals(124L, rounding.applyAsLong(123.55f));
        }
    }

    @Nested
    public class FloatTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<Float> sum = new AtomicReference<>();
            FloatTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept(1.1f, 2.2f, 3.3f);
            assertEquals(6.6f, sum.get(), FLOAT_DELTA);
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            FloatTriConsumer first = (a, b, c) -> order.add("first");
            FloatTriConsumer second = (a, b, c) -> order.add("second");
            FloatTriConsumer chained = first.andThen(second);

            chained.accept(1f, 2f, 3f);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class FloatTriFunctionTest {
        @Test
        public void testApply() {
            FloatTriFunction<String> function = (a, b, c) -> a + "," + b + "," + c;
            assertEquals("1.1,2.2,3.3", function.apply(1.1f, 2.2f, 3.3f));
        }

        @Test
        public void testAndThen() {
            FloatTriFunction<Float> sum = (a, b, c) -> a + b + c;
            java.util.function.Function<Float, String> toString = Object::toString;
            FloatTriFunction<String> chained = sum.andThen(toString);
            assertEquals("6.6000004", chained.apply(1.1f, 2.2f, 3.3f));
        }
    }

    @Nested
    public class FloatTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(FloatTriPredicate.ALWAYS_TRUE.test(1f, 2f, 3f));
            assertFalse(FloatTriPredicate.ALWAYS_FALSE.test(1f, 2f, 3f));
        }

        @Test
        public void testTest() {
            FloatTriPredicate predicate = (a, b, c) -> a + b > c;
            assertTrue(predicate.test(3.0f, 4.0f, 5.0f));
            assertFalse(predicate.test(1.0f, 2.0f, 4.0f));
        }

        @Test
        public void testNegateAndOr() {
            FloatTriPredicate isPythagorean = (a, b, c) -> (a * a + b * b) == (c * c);
            FloatTriPredicate isNotPythagorean = isPythagorean.negate();
            FloatTriPredicate anyIsZero = (a, b, c) -> a == 0 || b == 0 || c == 0;

            assertTrue(isPythagorean.test(3f, 4f, 5f));
            assertFalse(isNotPythagorean.test(3f, 4f, 5f));
            assertTrue(isNotPythagorean.test(3f, 4f, 6f));

            FloatTriPredicate combined = isPythagorean.or(anyIsZero);
            assertTrue(combined.test(3f, 4f, 5f));
            assertTrue(combined.test(0f, 4f, 6f));
            assertFalse(combined.test(1f, 2f, 3f));
        }
    }

    @Nested
    public class FloatUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            assertEquals(42.0f, FloatUnaryOperator.identity().applyAsFloat(42.0f), FLOAT_DELTA);
        }

        @Test
        public void testComposeAndThen() {
            FloatUnaryOperator addOne = d -> d + 1;
            FloatUnaryOperator timesTwo = d -> d * 2;

            FloatUnaryOperator composed = addOne.compose(timesTwo);
            assertEquals(21.0f, composed.applyAsFloat(10.0f), FLOAT_DELTA);

            FloatUnaryOperator chained = addOne.andThen(timesTwo);
            assertEquals(22.0f, chained.applyAsFloat(10.0f), FLOAT_DELTA);
        }
    }

    @Nested
    public class FunctionTest {
        @Test
        public void testStaticIdentity() {
            Function<String, String> identity = Function.identity();
            assertEquals("hello", identity.apply("hello"));
            assertNull(identity.apply(null));
        }

        @Test
        public void testCompose() {
            Function<String, Integer> toLength = String::length;
            Function<Integer, String> toString = Object::toString;
            Function<String, String> composed = toString.compose(toLength);
            assertEquals("5", composed.apply("hello"));
        }

        @Test
        public void testAndThen() {
            Function<String, Integer> toLength = String::length;
            Function<Integer, String> toString = Object::toString;
            Function<String, String> chained = toLength.andThen(toString);
            assertEquals("5", chained.apply("hello"));
        }

        @Test
        public void testToThrowable() {
            Function<Integer, Integer> function = i -> i;
            Throwables.Function<Integer, Integer, ?> throwableFunction = function.toThrowable();
            assertNotNull(throwableFunction);
        }
    }

    @Nested
    public class IntBiConsumerTest {
        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            IntBiConsumer first = (t, u) -> order.add("first:" + (t + u));
            IntBiConsumer second = (t, u) -> order.add("second:" + (t * u));

            first.andThen(second).accept(10, 5);

            assertEquals(2, order.size());
            assertEquals("first:15", order.get(0));
            assertEquals("second:50", order.get(1));
        }
    }

    @Nested
    public class IntBiFunctionTest {
        @Test
        public void testAndThen() {
            IntBiFunction<Integer> sum = Integer::sum;
            java.util.function.Function<Integer, String> toString = Object::toString;
            IntBiFunction<String> chained = sum.andThen(toString);
            assertEquals("15", chained.apply(10, 5));
        }
    }

    @Nested
    public class IntBinaryOperatorTest {
        @Test
        public void testApplyAsInt() {
            IntBinaryOperator subtract = (left, right) -> left - right;
            assertEquals(5, subtract.applyAsInt(10, 5));
        }
    }

    @Nested
    public class IntBiObjConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<String> result1 = new AtomicReference<>();
            AtomicReference<String> result2 = new AtomicReference<>();
            IntBiObjConsumer<String, String> first = (i, t, u) -> result1.set(i + t + u);
            IntBiObjConsumer<String, String> second = (i, t, u) -> result2.set(u + t + i);

            first.andThen(second).accept(1, "a", "b");
            assertEquals("1ab", result1.get());
            assertEquals("ba1", result2.get());
        }
    }

    @Nested
    public class IntBiObjFunctionTest {
        @Test
        public void testAndThen() {
            IntBiObjFunction<String, Integer, String> func = (i, t, u) -> t + i + u;
            java.util.function.Function<String, Integer> after = String::length;
            IntBiObjFunction<String, Integer, Integer> chained = func.andThen(after);

            assertEquals(4, chained.apply(10, "a", 5));
        }
    }

    @Nested
    public class IntBiObjPredicateTest {
        @Test
        public void testNegateAndOr() {
            IntBiObjPredicate<String, String> pred1 = (i, t, u) -> i > t.length();
            IntBiObjPredicate<String, String> pred2 = (i, t, u) -> i > u.length();

            IntBiObjPredicate<String, String> orPred = pred1.or(pred2);
            assertTrue(orPred.test(5, "abc", "abcdef"));
            assertTrue(orPred.test(5, "abcdef", "abc"));
            assertFalse(orPred.test(2, "abc", "abc"));

            IntBiObjPredicate<String, String> andPred = pred1.and(pred2);
            assertTrue(andPred.test(10, "abc", "abcdef"));
            assertFalse(andPred.test(5, "abc", "abcdef"));

            IntBiObjPredicate<String, String> negated = andPred.negate();
            assertTrue(negated.test(5, "abc", "abcdef"));
        }
    }

    @Nested
    public class IntBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(IntBiPredicate.ALWAYS_TRUE.test(1, 2));
            assertFalse(IntBiPredicate.ALWAYS_FALSE.test(1, 2));
            assertTrue(IntBiPredicate.EQUAL.test(2, 2));
            assertTrue(IntBiPredicate.NOT_EQUAL.test(1, 2));
            assertTrue(IntBiPredicate.GREATER_THAN.test(2, 1));
            assertTrue(IntBiPredicate.GREATER_EQUAL.test(2, 2));
            assertTrue(IntBiPredicate.LESS_THAN.test(1, 2));
            assertTrue(IntBiPredicate.LESS_EQUAL.test(2, 2));
        }

        @Test
        public void testNegateAndOr() {
            IntBiPredicate isDivisible = (t, u) -> t % u == 0;
            IntBiPredicate isNotDivisible = isDivisible.negate();
            IntBiPredicate bothEven = (t, u) -> t % 2 == 0 && u % 2 == 0;

            assertTrue(isDivisible.test(10, 5));
            assertTrue(isNotDivisible.test(10, 3));

            IntBiPredicate combined = isDivisible.and(bothEven);
            assertTrue(combined.test(10, 2));
            assertFalse(combined.test(10, 5));
        }
    }

    @Nested
    public class IntConsumerTest {
        @Test
        public void testAndThen() {
            AtomicInteger firstResult = new AtomicInteger();
            AtomicInteger secondResult = new AtomicInteger();
            IntConsumer first = val -> firstResult.set(val + 1);
            java.util.function.IntConsumer second = val -> secondResult.set(val * 2);

            first.andThen(second).accept(10);

            assertEquals(11, firstResult.get());
            assertEquals(20, secondResult.get());
        }
    }

    @Nested
    public class IntFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Integer.valueOf(99), IntFunction.BOX.apply(99));
            assertEquals(42, IntFunction.identity().apply(42));
        }

        @Test
        public void testAndThen() {
            IntFunction<String> toString = String::valueOf;
            java.util.function.Function<String, Integer> length = String::length;
            IntFunction<Integer> chained = toString.andThen(length);
            assertEquals(3, chained.apply(123));
        }
    }

    @Nested
    public class IntMapMultiConsumerTest {
        @Test
        public void testAccept() {
            List<Integer> results = new ArrayList<>();
            IntMapMultiConsumer multiConsumer = (value, downstream) -> {
                downstream.accept(value);
                downstream.accept(value * 2);
                downstream.accept(value * 3);
            };

            multiConsumer.accept(5, results::add);

            assertEquals(3, results.size());
            assertEquals(5, results.get(0));
            assertEquals(10, results.get(1));
            assertEquals(15, results.get(2));
        }
    }

    @Nested
    public class IntNConsumerTest {
        @Test
        public void testAccept() {
            AtomicInteger sum = new AtomicInteger(0);
            IntNConsumer consumer = args -> {
                int currentSum = 0;
                for (int i : args)
                    currentSum += i;
                sum.set(currentSum);
            };
            consumer.accept(1, 2, 3, 4);
            assertEquals(10, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            IntNConsumer first = args -> order.add("first:" + Arrays.stream(args).sum());
            IntNConsumer second = args -> order.add("second:" + Arrays.stream(args).sum());
            IntNConsumer chained = first.andThen(second);
            chained.accept(10, 20);

            assertEquals(2, order.size());
            assertEquals("first:30", order.get(0));
            assertEquals("second:30", order.get(1));
        }
    }

    @Nested
    public class IntNFunctionTest {
        @Test
        public void testApply() {
            IntNFunction<Integer> productFunction = args -> {
                int product = 1;
                for (int i : args)
                    product *= i;
                return product;
            };
            assertEquals(24, productFunction.apply(1, 2, 3, 4));
        }

        @Test
        public void testAndThen() {
            IntNFunction<Integer> sumFunction = args -> Arrays.stream(args).sum();
            java.util.function.Function<Integer, String> after = val -> "Sum=" + val;
            IntNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum=6", chained.apply(1, 2, 3));
        }
    }

    @Nested
    public class IntObjConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<String> result1 = new AtomicReference<>();
            AtomicReference<String> result2 = new AtomicReference<>();
            IntObjConsumer<String> first = (i, t) -> result1.set(i + t);
            IntObjConsumer<String> second = (i, t) -> result2.set(t + i);

            first.andThen(second).accept(1, "a");
            assertEquals("1a", result1.get());
            assertEquals("a1", result2.get());
        }
    }

    @Nested
    public class IntObjFunctionTest {
        @Test
        public void testAndThen() {
            IntObjFunction<String, String> func = (i, t) -> t.repeat(i);
            java.util.function.Function<String, Integer> after = String::length;
            IntObjFunction<String, Integer> chained = func.andThen(after);

            assertEquals(6, chained.apply(3, "ab"));
        }
    }

    @Nested
    public class IntObjOperatorTest {
        @Test
        public void testApplyAsInt() {
            IntObjOperator<String> operator = (operand, obj) -> operand + obj.length();
            assertEquals(15, operator.applyAsInt(10, "hello"));
        }
    }

    @Nested
    public class IntObjPredicateTest {
        @Test
        public void testNegateAndOr() {
            IntObjPredicate<String> lengthEquals = (i, t) -> i == t.length();
            IntObjPredicate<String> lengthNotEquals = lengthEquals.negate();
            IntObjPredicate<String> startsWithA = (i, t) -> t.startsWith("a");

            assertTrue(lengthEquals.test(3, "abc"));
            assertFalse(lengthNotEquals.test(3, "abc"));
            assertTrue(lengthNotEquals.test(4, "abc"));

            IntObjPredicate<String> combined = lengthEquals.or(startsWithA);
            assertTrue(combined.test(5, "apple"));
            assertTrue(combined.test(3, "xyz"));
            assertFalse(combined.test(4, "xyz"));
        }
    }

    @Nested
    public class IntPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(IntPredicate.ALWAYS_TRUE.test(0));
            assertFalse(IntPredicate.ALWAYS_FALSE.test(0));
            assertTrue(IntPredicate.IS_ZERO.test(0));
            assertTrue(IntPredicate.NOT_ZERO.test(1));
            assertTrue(IntPredicate.IS_POSITIVE.test(1));
            assertTrue(IntPredicate.NOT_POSITIVE.test(0));
            assertTrue(IntPredicate.IS_NEGATIVE.test(-1));
            assertTrue(IntPredicate.NOT_NEGATIVE.test(0));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(IntPredicate.equal(10).test(10));
            assertTrue(IntPredicate.notEqual(10).test(11));
            assertTrue(IntPredicate.greaterThan(10).test(11));
            assertTrue(IntPredicate.greaterEqual(10).test(10));
            assertTrue(IntPredicate.lessThan(10).test(9));
            assertTrue(IntPredicate.lessEqual(10).test(10));
            assertTrue(IntPredicate.between(10, 20).test(15));
            assertFalse(IntPredicate.between(10, 20).test(10));
        }

        @Test
        public void testStaticOf() {
            IntPredicate isEven = i -> i % 2 == 0;
            assertSame(isEven, IntPredicate.of(isEven));
        }
    }

    @Nested
    public class IntSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals(0, IntSupplier.ZERO.getAsInt());
            int randomInt = IntSupplier.RANDOM.getAsInt();
            assertNotNull(randomInt);
        }

        @Test
        public void testGetAsInt() {
            IntSupplier supplier = () -> 42;
            assertEquals(42, supplier.getAsInt());
        }
    }

    @Nested
    public class IntTernaryOperatorTest {
        @Test
        public void testApplyAsInt() {
            IntTernaryOperator operator = (a, b, c) -> a > 0 ? b : (a < 0 ? c : 0);
            assertEquals(10, operator.applyAsInt(1, 10, 20));
            assertEquals(20, operator.applyAsInt(-1, 10, 20));
            assertEquals(0, operator.applyAsInt(0, 10, 20));
        }
    }

    @Nested
    public class IntToBooleanFunctionTest {
        @Test
        public void testDefault() {
            assertTrue(IntToBooleanFunction.DEFAULT.applyAsBoolean(1));
            assertTrue(IntToBooleanFunction.DEFAULT.applyAsBoolean(Integer.MAX_VALUE));
            assertFalse(IntToBooleanFunction.DEFAULT.applyAsBoolean(0));
            assertFalse(IntToBooleanFunction.DEFAULT.applyAsBoolean(-1));
        }

        @Test
        public void testApplyAsBoolean() {
            IntToBooleanFunction isEven = value -> value % 2 == 0;
            assertTrue(isEven.applyAsBoolean(10));
            assertFalse(isEven.applyAsBoolean(11));
            assertTrue(isEven.applyAsBoolean(0));
        }
    }

    @Nested
    public class IntToByteFunctionTest {
        @Test
        public void testDefault() {
            assertEquals((byte) 123, IntToByteFunction.DEFAULT.applyAsByte(123));
            assertEquals((byte) 1, IntToByteFunction.DEFAULT.applyAsByte(257));
        }

        @Test
        public void testApplyAsByte() {
            IntToByteFunction custom = val -> (byte) (val / 2);
            assertEquals((byte) 5, custom.applyAsByte(10));
        }
    }

    @Nested
    public class IntToCharFunctionTest {
        @Test
        public void testDefault() {
            assertEquals('A', IntToCharFunction.DEFAULT.applyAsChar(65));
            assertEquals((char) 65601, IntToCharFunction.DEFAULT.applyAsChar(65601));
        }

        @Test
        public void testApplyAsChar() {
            IntToCharFunction custom = val -> (char) (val + 1);
            assertEquals('B', custom.applyAsChar(65));
        }
    }

    @Nested
    public class IntToDoubleFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.0d, IntToDoubleFunction.DEFAULT.applyAsDouble(123), DOUBLE_DELTA);
        }

        @Test
        public void testApplyAsDouble() {
            IntToDoubleFunction custom = val -> val / 2.0;
            assertEquals(5.5d, custom.applyAsDouble(11), DOUBLE_DELTA);
        }
    }

    @Nested
    public class IntToFloatFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.0f, IntToFloatFunction.DEFAULT.applyAsFloat(123), FLOAT_DELTA);
        }

        @Test
        public void testApplyAsFloat() {
            IntToFloatFunction custom = val -> val / 2.0f;
            assertEquals(5.5f, custom.applyAsFloat(11), FLOAT_DELTA);
        }
    }

    @Nested
    public class IntToLongFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123L, IntToLongFunction.DEFAULT.applyAsLong(123));
            assertEquals(Integer.MAX_VALUE, IntToLongFunction.DEFAULT.applyAsLong(Integer.MAX_VALUE));
        }

        @Test
        public void testApplyAsLong() {
            IntToLongFunction custom = val -> (long) val * 2;
            assertEquals(200L, custom.applyAsLong(100));
        }
    }

    @Nested
    public class IntToShortFunctionTest {
        @Test
        public void testDefault() {
            assertEquals((short) 123, IntToShortFunction.DEFAULT.applyAsShort(123));
            assertEquals((short) -32768, IntToShortFunction.DEFAULT.applyAsShort(32768));
        }

        @Test
        public void testApplyAsShort() {
            IntToShortFunction custom = val -> (short) (val / 2);
            assertEquals((short) 5, custom.applyAsShort(10));
        }
    }

    @Nested
    public class IntTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicInteger sum = new AtomicInteger();
            IntTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept(1, 2, 3);
            assertEquals(6, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            IntTriConsumer first = (a, b, c) -> order.add("first");
            IntTriConsumer second = (a, b, c) -> order.add("second");
            IntTriConsumer chained = first.andThen(second);

            chained.accept(1, 2, 3);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class IntTriFunctionTest {
        @Test
        public void testApply() {
            IntTriFunction<String> function = (a, b, c) -> a + "," + b + "," + c;
            assertEquals("1,2,3", function.apply(1, 2, 3));
        }

        @Test
        public void testAndThen() {
            IntTriFunction<Integer> sum = (a, b, c) -> a + b + c;
            java.util.function.Function<Integer, String> toString = Object::toString;
            IntTriFunction<String> chained = sum.andThen(toString);
            assertEquals("6", chained.apply(1, 2, 3));
        }
    }

    @Nested
    public class IntTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(IntTriPredicate.ALWAYS_TRUE.test(1, 2, 3));
            assertFalse(IntTriPredicate.ALWAYS_FALSE.test(1, 2, 3));
        }

        @Test
        public void testTest() {
            IntTriPredicate predicate = (a, b, c) -> a + b == c;
            assertTrue(predicate.test(3, 4, 7));
            assertFalse(predicate.test(1, 2, 4));
        }

        @Test
        public void testNegateAndOr() {
            IntTriPredicate isPythagorean = (a, b, c) -> (a * a + b * b) == (c * c);
            IntTriPredicate isNotPythagorean = isPythagorean.negate();
            IntTriPredicate anyAreZero = (a, b, c) -> a == 0 || b == 0 || c == 0;

            assertTrue(isPythagorean.test(3, 4, 5));
            assertFalse(isNotPythagorean.test(3, 4, 5));

            IntTriPredicate combined = isPythagorean.or(anyAreZero);
            assertTrue(combined.test(3, 4, 5));
            assertTrue(combined.test(0, 4, 6));
            assertFalse(combined.test(1, 2, 3));
        }
    }

    @Nested
    public class IntUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            assertEquals(42, IntUnaryOperator.identity().applyAsInt(42));
        }

        @Test
        public void testComposeAndThen() {
            IntUnaryOperator addOne = i -> i + 1;
            java.util.function.IntUnaryOperator timesTwo = i -> i * 2;

            IntUnaryOperator composed = addOne.compose(timesTwo);
            assertEquals(21, composed.applyAsInt(10));

            IntUnaryOperator chained = addOne.andThen(timesTwo);
            assertEquals(22, chained.applyAsInt(10));
        }
    }

    @Nested
    public class LongBiConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<Long> sum = new AtomicReference<>();
            AtomicReference<Long> product = new AtomicReference<>();
            LongBiConsumer first = (t, u) -> sum.set(t + u);
            LongBiConsumer second = (t, u) -> product.set(t * u);

            first.andThen(second).accept(10L, 5L);

            assertEquals(15L, sum.get());
            assertEquals(50L, product.get());
        }
    }

    @Nested
    public class LongBiFunctionTest {
        @Test
        public void testAndThen() {
            LongBiFunction<Long> sum = Long::sum;
            java.util.function.Function<Long, String> toString = Object::toString;
            LongBiFunction<String> chained = sum.andThen(toString);
            assertEquals("15", chained.apply(10L, 5L));
        }
    }

    @Nested
    public class LongBinaryOperatorTest {
        @Test
        public void testApplyAsLong() {
            LongBinaryOperator multiply = (left, right) -> left * right;
            assertEquals(50L, multiply.applyAsLong(10L, 5L));
        }
    }

    @Nested
    public class LongBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(LongBiPredicate.ALWAYS_TRUE.test(1L, 2L));
            assertFalse(LongBiPredicate.ALWAYS_FALSE.test(1L, 2L));
            assertTrue(LongBiPredicate.EQUAL.test(2L, 2L));
            assertTrue(LongBiPredicate.NOT_EQUAL.test(1L, 2L));
            assertTrue(LongBiPredicate.GREATER_THAN.test(2L, 1L));
            assertTrue(LongBiPredicate.GREATER_EQUAL.test(2L, 2L));
            assertTrue(LongBiPredicate.LESS_THAN.test(1L, 2L));
            assertTrue(LongBiPredicate.LESS_EQUAL.test(2L, 2L));
        }

        @Test
        public void testNegateAndOr() {
            LongBiPredicate isDivisible = (t, u) -> t % u == 0;
            LongBiPredicate isNotDivisible = isDivisible.negate();
            LongBiPredicate bothPositive = (t, u) -> t > 0 && u > 0;

            assertTrue(isDivisible.test(10L, 5L));
            assertTrue(isNotDivisible.test(10L, 3L));

            LongBiPredicate combined = isDivisible.and(bothPositive);
            assertTrue(combined.test(10L, 5L));
            assertFalse(combined.test(-10L, 5L));
        }
    }

    @Nested
    public class LongConsumerTest {
        @Test
        public void testAndThen() {
            AtomicLong firstResult = new AtomicLong();
            AtomicLong secondResult = new AtomicLong();
            LongConsumer first = val -> firstResult.set(val + 1L);
            java.util.function.LongConsumer second = val -> secondResult.set(val * 2L);

            first.andThen(second).accept(100L);

            assertEquals(101L, firstResult.get());
            assertEquals(200L, secondResult.get());
        }
    }

    @Nested
    public class LongFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Long.valueOf(99L), LongFunction.BOX.apply(99L));
            assertEquals(42L, LongFunction.identity().apply(42L));
        }

        @Test
        public void testAndThen() {
            LongFunction<String> toString = String::valueOf;
            java.util.function.Function<String, Integer> length = String::length;
            LongFunction<Integer> chained = toString.andThen(length);
            assertEquals(3, chained.apply(123L));
        }
    }

    @Nested
    public class LongMapMultiConsumerTest {
        @Test
        public void testAccept() {
            List<Long> results = new ArrayList<>();
            LongMapMultiConsumer multiConsumer = (value, downstream) -> {
                downstream.accept(value);
                downstream.accept(value + 1);
            };

            multiConsumer.accept(100L, results::add);

            assertEquals(2, results.size());
            assertEquals(100L, results.get(0));
            assertEquals(101L, results.get(1));
        }
    }

    @Nested
    public class LongNConsumerTest {
        @Test
        public void testAccept() {
            AtomicLong sum = new AtomicLong(0);
            LongNConsumer consumer = args -> {
                long currentSum = 0;
                for (long l : args)
                    currentSum += l;
                sum.set(currentSum);
            };
            consumer.accept(1L, 2L, 3L, 4L);
            assertEquals(10L, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            LongNConsumer first = args -> order.add("first:" + Arrays.stream(args).sum());
            LongNConsumer second = args -> order.add("second:" + Arrays.stream(args).sum());
            LongNConsumer chained = first.andThen(second);
            chained.accept(10L, 20L);

            assertEquals(2, order.size());
            assertEquals("first:30", order.get(0));
            assertEquals("second:30", order.get(1));
        }
    }

    @Nested
    public class LongNFunctionTest {
        @Test
        public void testApply() {
            LongNFunction<Long> productFunction = args -> {
                long product = 1L;
                for (long l : args)
                    product *= l;
                return product;
            };
            assertEquals(24L, productFunction.apply(1L, 2L, 3L, 4L));
        }

        @Test
        public void testAndThen() {
            LongNFunction<Long> sumFunction = args -> Arrays.stream(args).sum();
            java.util.function.Function<Long, String> after = val -> "Sum=" + val;
            LongNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum=6", chained.apply(1L, 2L, 3L));
        }
    }

    @Nested
    public class LongObjConsumerTest {
        @Test
        public void testAndThen() {
            AtomicReference<String> result1 = new AtomicReference<>();
            AtomicReference<String> result2 = new AtomicReference<>();
            LongObjConsumer<String> first = (l, t) -> result1.set(l + t);
            LongObjConsumer<String> second = (l, t) -> result2.set(t + l);

            first.andThen(second).accept(1L, "a");
            assertEquals("1a", result1.get());
            assertEquals("a1", result2.get());
        }
    }

    @Nested
    public class LongObjFunctionTest {
        @Test
        public void testAndThen() {
            LongObjFunction<String, String> func = (l, t) -> t.repeat((int) l);
            java.util.function.Function<String, Integer> after = String::length;
            LongObjFunction<String, Integer> chained = func.andThen(after);

            assertEquals(6, chained.apply(3L, "ab"));
        }
    }

    @Nested
    public class LongObjPredicateTest {
        @Test
        public void testNegateAndOr() {
            LongObjPredicate<String> lengthEquals = (l, t) -> l == t.length();
            LongObjPredicate<String> lengthNotEquals = lengthEquals.negate();
            LongObjPredicate<String> startsWithA = (l, t) -> t.startsWith("a");

            assertTrue(lengthEquals.test(3L, "abc"));
            assertFalse(lengthNotEquals.test(3L, "abc"));
            assertTrue(lengthNotEquals.test(4L, "abc"));

            LongObjPredicate<String> combined = lengthEquals.or(startsWithA);
            assertTrue(combined.test(5L, "apple"));
            assertTrue(combined.test(3L, "xyz"));
            assertFalse(combined.test(4L, "xyz"));
        }
    }

    @Nested
    public class LongPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(LongPredicate.ALWAYS_TRUE.test(0L));
            assertFalse(LongPredicate.ALWAYS_FALSE.test(0L));
            assertTrue(LongPredicate.IS_ZERO.test(0L));
            assertTrue(LongPredicate.NOT_ZERO.test(1L));
            assertTrue(LongPredicate.IS_POSITIVE.test(1L));
            assertTrue(LongPredicate.NOT_POSITIVE.test(0L));
            assertTrue(LongPredicate.IS_NEGATIVE.test(-1L));
            assertTrue(LongPredicate.NOT_NEGATIVE.test(0L));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(LongPredicate.equal(10L).test(10L));
            assertTrue(LongPredicate.notEqual(10L).test(11L));
            assertTrue(LongPredicate.greaterThan(10L).test(11L));
            assertTrue(LongPredicate.greaterEqual(10L).test(10L));
            assertTrue(LongPredicate.lessThan(10L).test(9L));
            assertTrue(LongPredicate.lessEqual(10L).test(10L));
            assertTrue(LongPredicate.between(10L, 20L).test(15L));
            assertFalse(LongPredicate.between(10L, 20L).test(10L));
        }

        @Test
        public void testStaticOf() {
            LongPredicate isEven = l -> l % 2 == 0;
            assertSame(isEven, LongPredicate.of(isEven));
        }
    }

    @Nested
    public class LongSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals(0L, LongSupplier.ZERO.getAsLong());
            long randomLong = LongSupplier.RANDOM.getAsLong();
            assertNotNull(randomLong);
        }

        @Test
        public void testGetAsLong() {
            LongSupplier supplier = () -> 42L;
            assertEquals(42L, supplier.getAsLong());
        }
    }

    @Nested
    public class LongTernaryOperatorTest {
        @Test
        public void testApplyAsLong() {
            LongTernaryOperator operator = (a, b, c) -> a > 0 ? b : (a < 0 ? c : 0);
            assertEquals(10L, operator.applyAsLong(1L, 10L, 20L));
            assertEquals(20L, operator.applyAsLong(-1L, 10L, 20L));
            assertEquals(0L, operator.applyAsLong(0L, 10L, 20L));
        }
    }

    @Nested
    public class LongToDoubleFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.0d, LongToDoubleFunction.DEFAULT.applyAsDouble(123L), DOUBLE_DELTA);
        }

        @Test
        public void testApplyAsDouble() {
            LongToDoubleFunction custom = val -> val / 2.0;
            assertEquals(5.5d, custom.applyAsDouble(11L), DOUBLE_DELTA);
        }
    }

    @Nested
    public class LongToFloatFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123.0f, LongToFloatFunction.DEFAULT.applyAsFloat(123L), FLOAT_DELTA);
        }

        @Test
        public void testApplyAsFloat() {
            LongToFloatFunction custom = val -> (float) val / 2.0f;
            assertEquals(5.5f, custom.applyAsFloat(11L), FLOAT_DELTA);
        }
    }

    @Nested
    public class LongToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123, LongToIntFunction.DEFAULT.applyAsInt(123L));
            assertEquals(-1, LongToIntFunction.DEFAULT.applyAsInt(Long.MAX_VALUE));
        }

        @Test
        public void testApplyAsInt() {
            LongToIntFunction custom = val -> (int) (val * 2);
            assertEquals(200, custom.applyAsInt(100L));
        }
    }

    @Nested
    public class LongTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicLong sum = new AtomicLong();
            LongTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept(1L, 2L, 3L);
            assertEquals(6L, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            LongTriConsumer first = (a, b, c) -> order.add("first");
            LongTriConsumer second = (a, b, c) -> order.add("second");
            LongTriConsumer chained = first.andThen(second);

            chained.accept(1L, 2L, 3L);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class LongTriFunctionTest {
        @Test
        public void testApply() {
            LongTriFunction<String> function = (a, b, c) -> a + "," + b + "," + c;
            assertEquals("1,2,3", function.apply(1L, 2L, 3L));
        }

        @Test
        public void testAndThen() {
            LongTriFunction<Long> sum = (a, b, c) -> a + b + c;
            java.util.function.Function<Long, String> toString = Object::toString;
            LongTriFunction<String> chained = sum.andThen(toString);
            assertEquals("6", chained.apply(1L, 2L, 3L));
        }
    }

    @Nested
    public class LongTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(LongTriPredicate.ALWAYS_TRUE.test(1L, 2L, 3L));
            assertFalse(LongTriPredicate.ALWAYS_FALSE.test(1L, 2L, 3L));
        }

        @Test
        public void testNegateAndOr() {
            LongTriPredicate isPythagorean = (a, b, c) -> (a * a + b * b) == (c * c);
            LongTriPredicate isNotPythagorean = isPythagorean.negate();
            LongTriPredicate anyIsZero = (a, b, c) -> a == 0 || b == 0 || c == 0;

            assertTrue(isPythagorean.test(3L, 4L, 5L));
            assertFalse(isNotPythagorean.test(3L, 4L, 5L));

            LongTriPredicate combined = isPythagorean.or(anyIsZero);
            assertTrue(combined.test(3L, 4L, 5L));
            assertTrue(combined.test(0L, 4L, 6L));
            assertFalse(combined.test(1L, 2L, 3L));
        }
    }

    @Nested
    public class LongUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            assertEquals(42L, LongUnaryOperator.identity().applyAsLong(42L));
        }

        @Test
        public void testComposeAndThen() {
            LongUnaryOperator addOne = l -> l + 1;
            java.util.function.LongUnaryOperator timesTwo = l -> l * 2;

            LongUnaryOperator composed = addOne.compose(timesTwo);
            assertEquals(21L, composed.applyAsLong(10L));

            LongUnaryOperator chained = addOne.andThen(timesTwo);
            assertEquals(22L, chained.applyAsLong(10L));
        }
    }

    @Nested
    public class NConsumerTest {
        @Test
        public void testAccept() {
            List<String> list = new ArrayList<>();
            NConsumer<String> consumer = args -> {
                for (String s : args) {
                    list.add(s);
                }
            };
            consumer.accept("a", "b", "c");
            assertEquals(List.of("a", "b", "c"), list);
        }

        @Test
        public void testAndThen() {
            AtomicReference<String> firstResult = new AtomicReference<>();
            AtomicReference<String> secondResult = new AtomicReference<>();
            NConsumer<String> first = args -> firstResult.set(String.join("", args));
            NConsumer<String> second = args -> secondResult.set(String.join("-", args));

            first.andThen(second).accept("x", "y", "z");
            assertEquals("xyz", firstResult.get());
            assertEquals("x-y-z", secondResult.get());
        }
    }

    @Nested
    public class NFunctionTest {
        @Test
        public void testApply() {
            NFunction<Integer, Integer> sum = args -> Arrays.stream(args).mapToInt(i -> i).sum();
            assertEquals(10, sum.apply(1, 2, 3, 4));
        }

        @Test
        public void testAndThen() {
            NFunction<String, Integer> count = args -> args.length;
            java.util.function.Function<Integer, String> after = val -> "Count is " + val;
            NFunction<String, String> chained = count.andThen(after);
            assertEquals("Count is 3", chained.apply("a", "b", "c"));
        }
    }

    @Nested
    public class NPredicateTest {
        @Test
        public void testTest() {
            NPredicate<Integer> allEven = args -> Arrays.stream(args).allMatch(i -> i % 2 == 0);
            assertTrue(allEven.test(2, 4, 6));
            assertFalse(allEven.test(2, 3, 6));
        }

        @Test
        public void testNegateAndOr() {
            NPredicate<String> hasDuplicates = args -> Arrays.stream(args).distinct().count() < args.length;
            NPredicate<String> noDuplicates = hasDuplicates.negate();
            NPredicate<String> hasEmptyString = args -> Arrays.stream(args).anyMatch(String::isEmpty);

            assertTrue(hasDuplicates.test("a", "b", "a"));
            assertFalse(noDuplicates.test("a", "b", "a"));
            assertTrue(noDuplicates.test("a", "b", "c"));

            NPredicate<String> combinedOr = hasDuplicates.or(hasEmptyString);
            assertTrue(combinedOr.test("a", "b", "a"));
            assertTrue(combinedOr.test("a", "b", ""));
            assertFalse(combinedOr.test("a", "b", "c"));

            NPredicate<String> combinedAnd = hasDuplicates.and(hasEmptyString);
            assertTrue(combinedAnd.test("a", "", "a"));
            assertFalse(combinedAnd.test("a", "b", "a"));
        }
    }

    @Nested
    public class ObjBiIntConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjBiIntConsumer<String> consumer = (t, i, j) -> result.set(t + ":" + i + ":" + j);
            consumer.accept("Test", 1, 2);
            assertEquals("Test:1:2", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ObjBiIntConsumer<String> first = (t, i, j) -> order.add("first");
            ObjBiIntConsumer<String> second = (t, i, j) -> order.add("second");
            ObjBiIntConsumer<String> chained = first.andThen(second);

            chained.accept("Test", 1, 2);
            assertEquals(2, order.size());
            assertEquals("first", order.get(0));
            assertEquals("second", order.get(1));
        }
    }

    @Nested
    public class ObjBiIntFunctionTest {
        @Test
        public void testApply() {
            ObjBiIntFunction<String, String> function = (t, i, j) -> t + (i + j);
            assertEquals("Sum=15", function.apply("Sum=", 10, 5));
        }

        @Test
        public void testAndThen() {
            ObjBiIntFunction<String, String> initial = (t, i, j) -> t + (i + j);
            java.util.function.Function<String, Integer> after = String::length;
            ObjBiIntFunction<String, Integer> chained = initial.andThen(after);

            assertEquals(6, chained.apply("Sum=", 10, 5));
        }
    }

    @Nested
    public class ObjBiIntPredicateTest {
        @Test
        public void testTest() {
            ObjBiIntPredicate<String> predicate = (t, i, j) -> t.length() == (i + j);
            assertTrue(predicate.test("hello", 2, 3));
            assertFalse(predicate.test("hello", 2, 2));
        }

        @Test
        public void testNegateAndOr() {
            ObjBiIntPredicate<String> lengthEqualsSum = (t, i, j) -> t.length() == (i + j);
            ObjBiIntPredicate<String> lengthNotEqualsSum = lengthEqualsSum.negate();
            ObjBiIntPredicate<String> iIsEven = (t, i, j) -> i % 2 == 0;

            assertTrue(lengthNotEqualsSum.test("hello", 2, 2));
            assertTrue(lengthEqualsSum.and(iIsEven).test("hello", 2, 3));
            assertTrue(lengthEqualsSum.or(iIsEven).test("hello", 2, 4));
        }
    }

    @Nested
    public class ObjBooleanConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjBooleanConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", true);
            assertEquals("Result:true", result.get());
        }
    }

    @Nested
    public class ObjByteConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjByteConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", (byte) 127);
            assertEquals("Result:127", result.get());
        }
    }

    @Nested
    public class ObjCharConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjCharConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", 'A');
            assertEquals("Result:A", result.get());
        }
    }

    @Nested
    public class ObjDoubleConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjDoubleConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("PI", 3.14);
            assertEquals("PI:3.14", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ObjDoubleConsumer<String> first = (t, u) -> order.add("first");
            ObjDoubleConsumer<String> second = (t, u) -> order.add("second");
            ObjDoubleConsumer<String> chained = first.andThen(second);
            chained.accept("Test", 1.0);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class ObjDoubleFunctionTest {
        @Test
        public void testApply() {
            ObjDoubleFunction<String, String> function = (t, u) -> t + u;
            assertEquals("Value=99.9", function.apply("Value=", 99.9));
        }

        @Test
        public void testAndThen() {
            ObjDoubleFunction<String, String> initial = (t, u) -> t + u;
            java.util.function.Function<String, Integer> after = String::length;
            ObjDoubleFunction<String, Integer> chained = initial.andThen(after);

            assertEquals(10, chained.apply("Value=", 99.9));
        }
    }

    @Nested
    public class ObjDoublePredicateTest {
        @Test
        public void testTest() {
            ObjDoublePredicate<String> predicate = (t, u) -> u > t.length();
            assertTrue(predicate.test("four", 4.1));
            assertFalse(predicate.test("four", 4.0));
        }

        @Test
        public void testNegateAndOr() {
            ObjDoublePredicate<String> isGreaterThanLength = (t, u) -> u > t.length();
            ObjDoublePredicate<String> isLessThanOrEqualLength = isGreaterThanLength.negate();
            ObjDoublePredicate<String> isInteger = (t, u) -> u == Math.floor(u);

            assertTrue(isLessThanOrEqualLength.test("four", 4.0));
            assertTrue(isGreaterThanLength.or(isInteger).test("four", 4.0));
            assertFalse(isGreaterThanLength.and(isInteger).test("four", 4.1));
        }
    }

    @Nested
    public class ObjFloatConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjFloatConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", 123.45f);
            assertEquals("Result:123.45", result.get());
        }
    }

    @Nested
    public class ObjIntConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjIntConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", 123);
            assertEquals("Result:123", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ObjIntConsumer<String> first = (t, u) -> order.add("first");
            ObjIntConsumer<String> second = (t, u) -> order.add("second");
            ObjIntConsumer<String> chained = first.andThen(second);
            chained.accept("Test", 1);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class ObjIntFunctionTest {
        @Test
        public void testApply() {
            ObjIntFunction<String, String> function = (t, u) -> t + u;
            assertEquals("Value=99", function.apply("Value=", 99));
        }

        @Test
        public void testAndThen() {
            ObjIntFunction<String, String> initial = (t, u) -> t + u;
            java.util.function.Function<String, Integer> after = String::length;
            ObjIntFunction<String, Integer> chained = initial.andThen(after);
            assertEquals(8, chained.apply("Value=", 99));
        }
    }

    @Nested
    public class ObjIntPredicateTest {
        @Test
        public void testTest() {
            ObjIntPredicate<String> predicate = (t, u) -> t.length() == u;
            assertTrue(predicate.test("hello", 5));
            assertFalse(predicate.test("hello", 4));
        }

        @Test
        public void testNegateAndOr() {
            ObjIntPredicate<String> lengthEquals = (t, u) -> t.length() == u;
            ObjIntPredicate<String> lengthNotEquals = lengthEquals.negate();
            ObjIntPredicate<String> isEven = (t, u) -> u % 2 == 0;

            assertTrue(lengthNotEquals.test("hello", 4));
            assertTrue(lengthEquals.or(isEven).test("hello", 4));
            assertFalse(lengthEquals.and(isEven).test("hello", 5));
        }
    }

    @Nested
    public class ObjLongConsumerTest {
        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ObjLongConsumer<String> first = (t, u) -> order.add("first");
            ObjLongConsumer<String> second = (t, u) -> order.add("second");
            ObjLongConsumer<String> chained = first.andThen(second);
            chained.accept("Test", 1L);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class ObjLongFunctionTest {
        @Test
        public void testAndThen() {
            ObjLongFunction<String, String> initial = (t, u) -> t + u;
            java.util.function.Function<String, Integer> after = String::length;
            ObjLongFunction<String, Integer> chained = initial.andThen(after);
            assertEquals(25, chained.apply("Value=", Long.MAX_VALUE));
        }
    }

    @Nested
    public class ObjLongPredicateTest {
        @Test
        public void testNegateAndOr() {
            ObjLongPredicate<String> lengthEquals = (t, u) -> t.length() == u;
            ObjLongPredicate<String> lengthNotEquals = lengthEquals.negate();
            ObjLongPredicate<String> isPositive = (t, u) -> u > 0;

            assertTrue(lengthNotEquals.test("hello", 4L));
            assertTrue(lengthEquals.or(isPositive).test("hello", 4L));
            assertFalse(lengthEquals.and(isPositive).test("hello", -5L));
        }
    }

    @Nested
    public class ObjShortConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            ObjShortConsumer<String> consumer = (t, value) -> result.set(t + ":" + value);
            consumer.accept("Result", (short) 123);
            assertEquals("Result:123", result.get());
        }
    }

    @Nested
    public class PredicateTest {
        @Test
        public void testNegate() {
            Predicate<String> isEmpty = String::isEmpty;
            Predicate<String> isNotEmpty = isEmpty.negate();
            assertTrue(isNotEmpty.test("hello"));
            assertFalse(isNotEmpty.test(""));
        }

        @Test
        public void testAnd() {
            Predicate<String> isLongerThan3 = s -> s.length() > 3;
            Predicate<String> startsWithH = s -> s.startsWith("h");
            Predicate<String> combined = isLongerThan3.and(startsWithH);
            assertTrue(combined.test("hello"));
            assertFalse(combined.test("hi"));
            assertFalse(combined.test("world"));
        }

        @Test
        public void testOr() {
            Predicate<String> isLength3 = s -> s.length() == 3;
            Predicate<String> isLength5 = s -> s.length() == 5;
            Predicate<String> combined = isLength3.or(isLength5);
            assertTrue(combined.test("abc"));
            assertTrue(combined.test("hello"));
            assertFalse(combined.test("four"));
        }

        @Test
        public void testToThrowable() throws Throwable {
            Predicate<String> predicate = s -> true;
            Throwables.Predicate<String, ?> throwablePredicate = predicate.toThrowable();
            assertNotNull(throwablePredicate);
            assertTrue(throwablePredicate.test("test"));
        }
    }

    @Nested
    public class QuadConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            QuadConsumer<String, Integer, Long, Boolean> consumer = (a, b, c, d) -> result.set(a + b + c + d);
            consumer.accept("A", 1, 2L, true);
            assertEquals("A12true", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            QuadConsumer<String, Integer, Long, Boolean> first = (a, b, c, d) -> order.add("first");
            QuadConsumer<String, Integer, Long, Boolean> second = (a, b, c, d) -> order.add("second");
            QuadConsumer<String, Integer, Long, Boolean> chained = first.andThen(second);
            chained.accept("A", 1, 2L, true);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class QuadFunctionTest {
        @Test
        public void testApply() {
            QuadFunction<String, Integer, Long, Boolean, String> function = (a, b, c, d) -> a + ":" + b + ":" + c + ":" + d;
            assertEquals("A:1:2:true", function.apply("A", 1, 2L, true));
        }

        @Test
        public void testAndThen() {
            QuadFunction<String, Integer, Long, Boolean, String> initial = (a, b, c, d) -> a + ":" + b + ":" + c + ":" + d;
            java.util.function.Function<String, Integer> after = String::length;
            QuadFunction<String, Integer, Long, Boolean, Integer> chained = initial.andThen(after);
            assertEquals(10, chained.apply("A", 1, 2L, true));
        }
    }

    @Nested
    public class QuadPredicateTest {
        @Test
        public void testTest() {
            QuadPredicate<String, String, Integer, Integer> predicate = (a, b, c, d) -> a.length() + b.length() == c + d;
            assertTrue(predicate.test("a", "b", 1, 1));
            assertFalse(predicate.test("a", "b", 1, 2));
        }

        @Test
        public void testNegateAndOr() {
            QuadPredicate<String, String, Integer, Integer> pred1 = (a, b, c, d) -> a.equals(b);
            QuadPredicate<String, String, Integer, Integer> pred2 = (a, b, c, d) -> c.equals(d);

            assertTrue(pred1.or(pred2).test("x", "y", 5, 5));
            assertTrue(pred1.and(pred2).test("x", "x", 5, 5));
            assertFalse(pred1.and(pred2).test("x", "y", 5, 5));
            assertTrue(pred1.negate().test("x", "y", 5, 5));
        }
    }

    @Nested
    public class RunnableTest {
        @Test
        public void testRun() {
            AtomicBoolean hasRun = new AtomicBoolean(false);
            Runnable runnable = () -> hasRun.set(true);
            runnable.run();
            assertTrue(hasRun.get());
        }

        @Test
        public void testToCallable() {
            AtomicBoolean hasRun = new AtomicBoolean(false);
            com.landawn.abacus.util.function.Runnable runnable = () -> hasRun.set(true);
            Callable<Void> callable = runnable.toCallable();
            try {
                assertNull(callable.call());
            } catch (Exception e) {
                fail("Should not have thrown an exception");
            }
            assertTrue(hasRun.get());
        }

        @Test
        public void testToThrowable() {
            Runnable runnable = () -> {
            };
            Throwables.Runnable<?> throwableRunnable = runnable.toThrowable();
            assertNotNull(throwableRunnable);
            assertDoesNotThrow(throwableRunnable::run);
        }
    }

    @Nested
    public class ShortBiConsumerTest {
        @Test
        public void testAndThen() {
            List<Integer> results = new ArrayList<>();
            ShortBiConsumer first = (t, u) -> results.add(t + u);
            ShortBiConsumer second = (t, u) -> results.add(t * u);
            ShortBiConsumer chained = first.andThen(second);

            chained.accept((short) 10, (short) 5);
            assertEquals(2, results.size());
            assertEquals(15, results.get(0));
            assertEquals(50, results.get(1));
        }
    }

    @Nested
    public class ShortBiFunctionTest {
        @Test
        public void testAndThen() {
            ShortBiFunction<Integer> sum = (t, u) -> t + u;
            java.util.function.Function<Integer, String> toString = Object::toString;
            ShortBiFunction<String> chained = sum.andThen(toString);
            assertEquals("15", chained.apply((short) 10, (short) 5));
        }
    }

    @Nested
    public class ShortBinaryOperatorTest {
        @Test
        public void testApplyAsShort() {
            ShortBinaryOperator adder = (left, right) -> (short) (left + right);
            assertEquals((short) 15, adder.applyAsShort((short) 10, (short) 5));
        }
    }

    @Nested
    public class ShortBiPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(ShortBiPredicate.ALWAYS_TRUE.test((short) 1, (short) 2));
            assertFalse(ShortBiPredicate.ALWAYS_FALSE.test((short) 1, (short) 2));
            assertTrue(ShortBiPredicate.EQUAL.test((short) 2, (short) 2));
            assertTrue(ShortBiPredicate.NOT_EQUAL.test((short) 1, (short) 2));
            assertTrue(ShortBiPredicate.GREATER_THAN.test((short) 2, (short) 1));
            assertTrue(ShortBiPredicate.GREATER_EQUAL.test((short) 2, (short) 2));
            assertTrue(ShortBiPredicate.LESS_THAN.test((short) 1, (short) 2));
            assertTrue(ShortBiPredicate.LESS_EQUAL.test((short) 2, (short) 2));
        }

        @Test
        public void testNegate() {
            ShortBiPredicate isEqual = (t, u) -> t == u;
            ShortBiPredicate isNotEqual = isEqual.negate();
            assertTrue(isNotEqual.test((short) 1, (short) 2));
            assertFalse(isNotEqual.test((short) 2, (short) 2));
        }
    }

    @Nested
    public class ShortConsumerTest {
        @Test
        public void testAndThen() {
            AtomicInteger result1 = new AtomicInteger();
            AtomicInteger result2 = new AtomicInteger();
            ShortConsumer first = v -> result1.set(v);
            ShortConsumer second = v -> result2.set(v * 2);

            first.andThen(second).accept((short) 100);
            assertEquals(100, result1.get());
            assertEquals(200, result2.get());
        }
    }

    @Nested
    public class ShortFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Short.valueOf((short) 99), ShortFunction.BOX.apply((short) 99));
            assertEquals((short) 42, ShortFunction.identity().apply((short) 42));
        }

        @Test
        public void testAndThen() {
            ShortFunction<String> toString = String::valueOf;
            java.util.function.Function<String, Integer> length = String::length;
            ShortFunction<Integer> chained = toString.andThen(length);
            assertEquals(3, chained.apply((short) 123));
        }
    }

    @Nested
    public class ShortNConsumerTest {
        @Test
        public void testAccept() {
            AtomicInteger sum = new AtomicInteger(0);
            ShortNConsumer consumer = args -> {
                int currentSum = 0;
                for (short s : args)
                    currentSum += s;
                sum.set(currentSum);
            };
            consumer.accept((short) 1, (short) 2, (short) 3, (short) 4);
            assertEquals(10, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ShortNConsumer first = args -> order.add("first");
            ShortNConsumer second = args -> order.add("second");
            ShortNConsumer chained = first.andThen(second);
            chained.accept((short) 1, (short) 2);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class ShortNFunctionTest {
        @Test
        public void testAndThen() {
            ShortNFunction<Integer> sumFunction = args -> {
                int sum = 0;
                for (short s : args)
                    sum += s;
                return sum;
            };
            java.util.function.Function<Integer, String> after = val -> "Sum=" + val;
            ShortNFunction<String> chained = sumFunction.andThen(after);

            assertEquals("Sum=6", chained.apply((short) 1, (short) 2, (short) 3));
        }
    }

    @Nested
    public class ShortPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(ShortPredicate.ALWAYS_TRUE.test((short) 0));
            assertFalse(ShortPredicate.ALWAYS_FALSE.test((short) 0));
            assertTrue(ShortPredicate.IS_ZERO.test((short) 0));
            assertTrue(ShortPredicate.NOT_ZERO.test((short) 1));
            assertTrue(ShortPredicate.IS_POSITIVE.test((short) 1));
            assertTrue(ShortPredicate.NOT_POSITIVE.test((short) 0));
            assertTrue(ShortPredicate.IS_NEGATIVE.test((short) -1));
            assertTrue(ShortPredicate.NOT_NEGATIVE.test((short) 0));
        }

        @Test
        public void testStaticFactories() {
            assertTrue(ShortPredicate.equal((short) 10).test((short) 10));
            assertTrue(ShortPredicate.notEqual((short) 10).test((short) 11));
            assertTrue(ShortPredicate.greaterThan((short) 10).test((short) 11));
            assertTrue(ShortPredicate.greaterEqual((short) 10).test((short) 10));
            assertTrue(ShortPredicate.lessThan((short) 10).test((short) 9));
            assertTrue(ShortPredicate.lessEqual((short) 10).test((short) 10));
            assertTrue(ShortPredicate.between((short) 10, (short) 20).test((short) 15));
            assertFalse(ShortPredicate.between((short) 10, (short) 20).test((short) 10));
        }

        @Test
        public void testStaticOf() {
            ShortPredicate isEven = s -> s % 2 == 0;
            assertSame(isEven, ShortPredicate.of(isEven));
        }
    }

    @Nested
    public class ShortSupplierTest {
        @Test
        public void testStaticFields() {
            assertEquals((short) 0, ShortSupplier.ZERO.getAsShort());
            short randomShort = ShortSupplier.RANDOM.getAsShort();
            assertNotNull(randomShort);
        }

        @Test
        public void testGetAsShort() {
            ShortSupplier supplier = () -> (short) 42;
            assertEquals((short) 42, supplier.getAsShort());
        }
    }

    @Nested
    public class ShortTernaryOperatorTest {
        @Test
        public void testApplyAsShort() {
            ShortTernaryOperator operator = (a, b, c) -> (short) (a > 0 ? b + c : b - c);
            assertEquals((short) 30, operator.applyAsShort((short) 1, (short) 10, (short) 20));
            assertEquals((short) -10, operator.applyAsShort((short) -1, (short) 10, (short) 20));
        }
    }

    @Nested
    public class ShortToIntFunctionTest {
        @Test
        public void testDefault() {
            assertEquals(123, ShortToIntFunction.DEFAULT.applyAsInt((short) 123));
        }

        @Test
        public void testApplyAsInt() {
            ShortToIntFunction custom = val -> val * 2;
            assertEquals(200, custom.applyAsInt((short) 100));
        }
    }

    @Nested
    public class ShortTriConsumerTest {
        @Test
        public void testAccept() {
            AtomicInteger sum = new AtomicInteger();
            ShortTriConsumer consumer = (a, b, c) -> sum.set(a + b + c);
            consumer.accept((short) 1, (short) 2, (short) 3);
            assertEquals(6, sum.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            ShortTriConsumer first = (a, b, c) -> order.add("first");
            ShortTriConsumer second = (a, b, c) -> order.add("second");
            ShortTriConsumer chained = first.andThen(second);

            chained.accept((short) 1, (short) 2, (short) 3);
            assertEquals(List.of("first", "second"), order);
        }
    }

    @Nested
    public class ShortTriFunctionTest {
        @Test
        public void testApply() {
            ShortTriFunction<String> function = (a, b, c) -> a + "," + b + "," + c;
            assertEquals("1,2,3", function.apply((short) 1, (short) 2, (short) 3));
        }

        @Test
        public void testAndThen() {
            ShortTriFunction<Integer> sum = (a, b, c) -> a + b + c;
            java.util.function.Function<Integer, String> toString = Object::toString;
            ShortTriFunction<String> chained = sum.andThen(toString);
            assertEquals("6", chained.apply((short) 1, (short) 2, (short) 3));
        }
    }

    @Nested
    public class ShortTriPredicateTest {
        @Test
        public void testStaticFields() {
            assertTrue(ShortTriPredicate.ALWAYS_TRUE.test((short) 1, (short) 2, (short) 3));
            assertFalse(ShortTriPredicate.ALWAYS_FALSE.test((short) 1, (short) 2, (short) 3));
        }

        @Test
        public void testNegateAndOr() {
            ShortTriPredicate sumEquals = (a, b, c) -> (a + b) == c;
            ShortTriPredicate sumNotEquals = sumEquals.negate();
            ShortTriPredicate allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;

            assertTrue(sumEquals.test((short) 3, (short) 4, (short) 7));
            assertFalse(sumNotEquals.test((short) 3, (short) 4, (short) 7));
            assertTrue(sumEquals.and(allPositive).test((short) 3, (short) 4, (short) 7));
            assertFalse(sumEquals.and(allPositive).test((short) -3, (short) 10, (short) 7));
        }
    }

    @Nested
    public class ShortUnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            assertEquals((short) 42, ShortUnaryOperator.identity().applyAsShort((short) 42));
        }

        @Test
        public void testComposeAndThen() {
            ShortUnaryOperator addOne = s -> (short) (s + 1);
            ShortUnaryOperator timesTwo = s -> (short) (s * 2);

            ShortUnaryOperator composed = addOne.compose(timesTwo);
            assertEquals((short) 21, composed.applyAsShort((short) 10));

            ShortUnaryOperator chained = addOne.andThen(timesTwo);
            assertEquals((short) 22, chained.applyAsShort((short) 10));
        }
    }

    @Nested
    public class SupplierTest {
        @Test
        public void testGet() {
            Supplier<String> supplier = () -> "hello";
            assertEquals("hello", supplier.get());
        }

        @Test
        public void testToThrowable() throws Throwable {
            Supplier<Integer> supplier = () -> 1;
            Throwables.Supplier<Integer, ?> throwableSupplier = supplier.toThrowable();
            assertNotNull(throwableSupplier);
            assertEquals(1, throwableSupplier.get());
        }
    }

    @Nested
    public class ToBooleanBiFunctionTest {
        @Test
        public void testApplyAsBoolean() {
            ToBooleanBiFunction<String, Integer> function = (s, i) -> s.length() == i;
            assertTrue(function.applyAsBoolean("hello", 5));
            assertFalse(function.applyAsBoolean("hello", 4));
        }
    }

    @Nested
    public class ToBooleanFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertTrue(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.TRUE));
            assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.FALSE));
            assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(null));
        }

        @Test
        public void testApplyAsBoolean() {
            ToBooleanFunction<String> isBlank = String::isBlank;
            assertTrue(isBlank.applyAsBoolean(" "));
            assertFalse(isBlank.applyAsBoolean(" a "));
        }
    }

    @Nested
    public class ToByteBiFunctionTest {
        @Test
        public void testApplyAsByte() {
            ToByteBiFunction<String, Integer> function = (s, i) -> (byte) (s.length() + i);
            assertEquals((byte) 8, function.applyAsByte("hello", 3));
        }
    }

    @Nested
    public class ToByteFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals((byte) 10, ToByteFunction.UNBOX.applyAsByte(Byte.valueOf((byte) 10)));
            assertEquals((byte) 0, ToByteFunction.UNBOX.applyAsByte(null));
        }

        @Test
        public void testStaticFromNum() {
            assertEquals((byte) 123, ToByteFunction.FROM_NUM.applyAsByte(123));
            assertEquals((byte) 45, ToByteFunction.FROM_NUM.applyAsByte(45.67d));
            assertEquals((byte) 0, ToByteFunction.FROM_NUM.applyAsByte(null));
        }

        @Test
        public void testApplyAsByte() {
            ToByteFunction<String> lengthAsByte = s -> (byte) s.length();
            assertEquals((byte) 5, lengthAsByte.applyAsByte("hello"));
        }
    }

    @Nested
    public class ToCharBiFunctionTest {
        @Test
        public void testApplyAsChar() {
            ToCharBiFunction<String, Integer> function = String::charAt;
            assertEquals('e', function.applyAsChar("hello", 1));
        }
    }

    @Nested
    public class ToCharFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals('a', ToCharFunction.UNBOX.applyAsChar(Character.valueOf('a')));
            assertEquals('\0', ToCharFunction.UNBOX.applyAsChar(null));
        }

        @Test
        public void testApplyAsChar() {
            ToCharFunction<String> firstChar = s -> s.isEmpty() ? '\0' : s.charAt(0);
            assertEquals('h', firstChar.applyAsChar("hello"));
        }
    }

    @Nested
    public class ToDoubleBiFunctionTest {
        @Test
        public void testApplyAsDouble() {
            ToDoubleBiFunction<String, Integer> function = (s, i) -> Double.parseDouble(s) + i;
            assertEquals(113.45, function.applyAsDouble("10.45", 103), DOUBLE_DELTA);
        }
    }

    @Nested
    public class ToDoubleFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals(3.14, ToDoubleFunction.UNBOX.applyAsDouble(Double.valueOf(3.14)), DOUBLE_DELTA);
            assertEquals(0.0, ToDoubleFunction.UNBOX.applyAsDouble(null), DOUBLE_DELTA);
        }

        @Test
        public void testStaticFromNum() {
            assertEquals(123.0, ToDoubleFunction.FROM_NUM.applyAsDouble(123), DOUBLE_DELTA);
            assertEquals(45.67, ToDoubleFunction.FROM_NUM.applyAsDouble(45.67f), DOUBLE_DELTA);
            assertEquals(0.0, ToDoubleFunction.FROM_NUM.applyAsDouble(null), DOUBLE_DELTA);
        }

        @Test
        public void testApplyAsDouble() {
            ToDoubleFunction<String> parseDouble = Double::parseDouble;
            assertEquals(123.45, parseDouble.applyAsDouble("123.45"), DOUBLE_DELTA);
        }
    }

    @Nested
    public class ToDoubleTriFunctionTest {
        @Test
        public void testApplyAsDouble() {
            ToDoubleTriFunction<String, Integer, Long> function = (s, i, l) -> s.length() + i + l;
            assertEquals(18.0, function.applyAsDouble("hello", 3, 10L), DOUBLE_DELTA);
        }
    }

    @Nested
    public class ToFloatBiFunctionTest {
        @Test
        public void testApplyAsFloat() {
            ToFloatBiFunction<String, Integer> function = (s, i) -> s.length() + i;
            assertEquals(8.0f, function.applyAsFloat("hello", 3), FLOAT_DELTA);
        }
    }

    @Nested
    public class ToFloatFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals(3.14f, ToFloatFunction.UNBOX.applyAsFloat(3.14f), FLOAT_DELTA);
            assertEquals(0f, ToFloatFunction.UNBOX.applyAsFloat(null), FLOAT_DELTA);
        }

        @Test
        public void testStaticFromNum() {
            assertEquals(123f, ToFloatFunction.FROM_NUM.applyAsFloat(123), FLOAT_DELTA);
            assertEquals(45.67f, ToFloatFunction.FROM_NUM.applyAsFloat(45.67d), FLOAT_DELTA);
            assertEquals(0f, ToFloatFunction.FROM_NUM.applyAsFloat(null), FLOAT_DELTA);
        }

        @Test
        public void testApplyAsFloat() {
            ToFloatFunction<String> parseFloat = Float::parseFloat;
            assertEquals(123.45f, parseFloat.applyAsFloat("123.45"), FLOAT_DELTA);
        }
    }

    @Nested
    public class ToIntBiFunctionTest {
        @Test
        public void testApplyAsInt() {
            ToIntBiFunction<String, String> function = (s1, s2) -> s1.length() + s2.length();
            assertEquals(8, function.applyAsInt("hello", "you"));
        }
    }

    @Nested
    public class ToIntFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals(123, ToIntFunction.UNBOX.applyAsInt(123));
            assertEquals(0, ToIntFunction.UNBOX.applyAsInt(null));
        }

        @Test
        public void testStaticFromNum() {
            assertEquals(123, ToIntFunction.FROM_NUM.applyAsInt(123L));
            assertEquals(45, ToIntFunction.FROM_NUM.applyAsInt(45.67d));
            assertEquals(0, ToIntFunction.FROM_NUM.applyAsInt(null));
        }

        @Test
        public void testApplyAsInt() {
            ToIntFunction<String> length = String::length;
            assertEquals(5, length.applyAsInt("hello"));
        }
    }

    @Nested
    public class ToIntTriFunctionTest {
        @Test
        public void testApplyAsInt() {
            ToIntTriFunction<String, Long, Double> function = (s, l, d) -> s.length() + l.intValue() + d.intValue();
            assertEquals(18, function.applyAsInt("hello", 10L, 3.14));
        }
    }

    @Nested
    public class ToLongBiFunctionTest {
        @Test
        public void testApplyAsLong() {
            ToLongBiFunction<String, Integer> function = (s, i) -> (long) s.length() + i;
            assertEquals(8L, function.applyAsLong("hello", 3));
        }
    }

    @Nested
    public class ToLongFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals(123L, ToLongFunction.UNBOX.applyAsLong(123L));
            assertEquals(0L, ToLongFunction.UNBOX.applyAsLong(null));
        }

        @Test
        public void testStaticFromNum() {
            assertEquals(123L, ToLongFunction.FROM_NUM.applyAsLong(123));
            assertEquals(45L, ToLongFunction.FROM_NUM.applyAsLong(45.67d));
            assertEquals(0L, ToLongFunction.FROM_NUM.applyAsLong(null));
        }

        @Test
        public void testApplyAsLong() {
            ToLongFunction<String> parseLong = Long::parseLong;
            assertEquals(1234567890L, parseLong.applyAsLong("1234567890"));
        }
    }

    @Nested
    public class ToLongTriFunctionTest {
        @Test
        public void testApplyAsLong() {
            ToLongTriFunction<String, Integer, Double> function = (s, i, d) -> s.length() + i + d.longValue();
            assertEquals(18L, function.applyAsLong("hello", 10, 3.14));
        }
    }

    @Nested
    public class ToShortBiFunctionTest {
        @Test
        public void testApplyAsShort() {
            ToShortBiFunction<String, Integer> function = (s, i) -> (short) (s.length() + i);
            assertEquals((short) 8, function.applyAsShort("hello", 3));
        }
    }

    @Nested
    public class ToShortFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertEquals((short) 123, ToShortFunction.UNBOX.applyAsShort((short) 123));
            assertEquals((short) 0, ToShortFunction.UNBOX.applyAsShort(null));
        }

        @Test
        public void testStaticFromNum() {
            assertEquals((short) 123, ToShortFunction.FROM_NUM.applyAsShort(123L));
            assertEquals((short) 45, ToShortFunction.FROM_NUM.applyAsShort(45.67d));
            assertEquals((short) 0, ToShortFunction.FROM_NUM.applyAsShort(null));
        }

        @Test
        public void testApplyAsShort() {
            ToShortFunction<String> lengthAsShort = s -> (short) s.length();
            assertEquals((short) 5, lengthAsShort.applyAsShort("hello"));
        }
    }

    @Nested
    public class TriConsumerTest {
        @Test
        public void testAccept() {
            AtomicReference<String> result = new AtomicReference<>();
            TriConsumer<String, Integer, Boolean> consumer = (a, b, c) -> result.set(a + b + c);
            consumer.accept("Test", 1, true);
            assertEquals("Test1true", result.get());
        }

        @Test
        public void testAndThen() {
            List<String> order = new ArrayList<>();
            TriConsumer<String, Integer, Boolean> first = (a, b, c) -> order.add("first");
            TriConsumer<String, Integer, Boolean> second = (a, b, c) -> order.add("second");
            TriConsumer<String, Integer, Boolean> chained = first.andThen(second);

            chained.accept("Test", 1, true);
            assertEquals(List.of("first", "second"), order);
        }

        @Test
        public void testToThrowable() {
            TriConsumer<String, Integer, Boolean> consumer = (a, b, c) -> {
            };
            Throwables.TriConsumer<String, Integer, Boolean, ?> throwableConsumer = consumer.toThrowable();
            assertNotNull(throwableConsumer);
            assertDoesNotThrow(() -> throwableConsumer.accept("a", 1, true));
        }
    }

    @Nested
    public class TriFunctionTest {
        @Test
        public void testApply() {
            TriFunction<String, Integer, Long, String> function = (a, b, c) -> a + ":" + b + ":" + c;
            assertEquals("A:1:2", function.apply("A", 1, 2L));
        }

        @Test
        public void testAndThen() {
            TriFunction<String, Integer, Long, String> initial = (a, b, c) -> a + ":" + b + ":" + c;
            Function<String, Integer> after = String::length;
            TriFunction<String, Integer, Long, Integer> chained = initial.andThen(after);
            assertEquals(5, chained.apply("A", 1, 2L));
        }

        @Test
        public void testToThrowable() throws Throwable {
            TriFunction<String, Integer, Long, String> function = (a, b, c) -> "result";
            Throwables.TriFunction<String, Integer, Long, String, ?> throwableFunction = function.toThrowable();
            assertNotNull(throwableFunction);
            assertEquals("result", throwableFunction.apply("a", 1, 1L));
        }
    }

    @Nested
    public class TriPredicateTest {
        @Test
        public void testTest() {
            TriPredicate<String, String, Integer> predicate = (a, b, c) -> a.length() + b.length() == c;
            assertTrue(predicate.test("a", "b", 2));
            assertFalse(predicate.test("a", "b", 3));
        }

        @Test
        public void testNegateAndOr() {
            TriPredicate<String, String, Integer> pred1 = (a, b, c) -> a.equals(b);
            TriPredicate<String, String, Integer> pred2 = (a, b, c) -> a.length() == c;

            assertTrue(pred1.or(pred2).test("x", "y", 1));
            assertTrue(pred1.and(pred2).test("x", "x", 1));
            assertFalse(pred1.and(pred2).test("x", "y", 1));
            assertTrue(pred1.negate().test("x", "y", 1));
        }

        @Test
        public void testToThrowable() throws Throwable {
            TriPredicate<String, Integer, Long> predicate = (a, b, c) -> true;
            Throwables.TriPredicate<String, Integer, Long, ?> throwablePredicate = predicate.toThrowable();
            assertNotNull(throwablePredicate);
            assertTrue(throwablePredicate.test("a", 1, 1L));
        }
    }

    @Nested
    public class UnaryOperatorTest {
        @Test
        public void testStaticIdentity() {
            UnaryOperator<String> identity = UnaryOperator.identity();
            assertEquals("hello", identity.apply("hello"));
            assertNull(identity.apply(null));
        }

        @Test
        public void testCompose() {
            UnaryOperator<String> toUpper = String::toUpperCase;
            java.util.function.UnaryOperator<String> addWorld = s -> s + " world";
            UnaryOperator<String> composed = toUpper.compose(addWorld);
            assertEquals("HELLO WORLD", composed.apply("hello"));
        }

        @Test
        public void testAndThen() {
            UnaryOperator<String> toUpper = String::toUpperCase;
            java.util.function.UnaryOperator<String> addWorld = s -> s + " world";
            UnaryOperator<String> chained = toUpper.andThen(addWorld);
            assertEquals("HELLO world", chained.apply("hello"));
        }

        @Test
        public void testToThrowable() throws Throwable {
            UnaryOperator<String> operator = s -> s;
            Throwables.UnaryOperator<String, ?> throwableOperator = operator.toThrowable();
            assertNotNull(throwableOperator);
            assertEquals("test", throwableOperator.apply("test"));
        }
    }

    @Nested
    public class UtilClassTest {
        @Test
        public void testRandomInstancesNotNull() {
            Object randomBoolean = com.landawn.abacus.util.function.BooleanSupplier.RANDOM;
            assertNotNull(randomBoolean);
        }
    }
}
