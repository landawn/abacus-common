package com.landawn.abacus.util.function;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

public class FunctionAPITest extends TestBase {

    @Nested
    public class BiConsumerTest {
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
    }

    @Nested
    public class BiIntObjConsumerTest {
    }

    @Nested
    public class BiIntObjFunctionTest {
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
    }

    @Nested
    public class BiObjIntConsumerTest {
    }

    @Nested
    public class BiObjIntFunctionTest {
    }

    @Nested
    public class BiObjIntPredicateTest {
        final BiObjIntPredicate<String, Integer> isSumOfLengthsEqualToInt = (s, i1, i2) -> s.length() + i1.toString().length() == i2;
        final BiObjIntPredicate<String, String> isConcatLengthEven = (s1, s2, i) -> (s1.length() + s2.length() + i) % 2 == 0;
    }

    @Nested
    public class BiPredicateTest {
        final BiPredicate<String, String> isLengthEqual = (s1, s2) -> s1.length() == s2.length();
        final BiPredicate<String, String> startsWithSameChar = (s1, s2) -> !s1.isEmpty() && !s2.isEmpty() && s1.charAt(0) == s2.charAt(0);
    }

    @Nested
    public class BooleanBiConsumerTest {
    }

    @Nested
    public class BooleanBiFunctionTest {
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
    }

    @Nested
    public class BooleanConsumerTest {
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
    }

    @Nested
    public class BooleanNConsumerTest {
    }

    @Nested
    public class BooleanNFunctionTest {
    }

    @Nested
    public class BooleanPredicateTest {

        @Test
        public void testStaticOf() {
            BooleanPredicate pred = val -> val;
            assertSame(pred, BooleanPredicate.of(pred));
        }
    }

    @Nested
    public class BooleanSupplierTest {

        @Test
        public void testGetAsBoolean() {
            final boolean value = true;
            BooleanSupplier supplier = () -> value;
            assertTrue(supplier.getAsBoolean());
        }
    }

    @Nested
    public class BooleanTernaryOperatorTest {
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
        public void testApplyAsChar() {
            BooleanToCharFunction customFunc = value -> value ? 'T' : 'F';
            assertEquals('T', customFunc.applyAsChar(true));
            assertEquals('F', customFunc.applyAsChar(false));
        }
    }

    @Nested
    public class BooleanToIntFunctionTest {

        @Test
        public void testApplyAsInt() {
            BooleanToIntFunction customFunc = value -> value ? 100 : -100;
            assertEquals(100, customFunc.applyAsInt(true));
            assertEquals(-100, customFunc.applyAsInt(false));
        }
    }

    @Nested
    public class BooleanTriConsumerTest {
    }

    @Nested
    public class BooleanTriFunctionTest {
    }

    @Nested
    public class BooleanTriPredicateTest {
        final BooleanTriPredicate atLeastTwoTrue = (a, b, c) -> (a && b) || (b && c) || (a && c);
        final BooleanTriPredicate allSame = (a, b, c) -> (a == b) && (b == c);
    }

    @Nested
    public class BooleanUnaryOperatorTest {
        final BooleanUnaryOperator not = operand -> !operand;

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
    }

    @Nested
    public class ByteBiConsumerTest {
    }

    @Nested
    public class ByteBiFunctionTest {
    }

    @Nested
    public class ByteBinaryOperatorTest {
    }

    @Nested
    public class ByteBiPredicateTest {
    }

    @Nested
    public class ByteConsumerTest {
    }

    @Nested
    public class ByteFunctionTest {
    }

    @Nested
    public class ByteNConsumerTest {
    }

    @Nested
    public class ByteNFunctionTest {
    }

    @Nested
    public class BytePredicateTest {

        @Test
        public void testStaticFactories() {
            assertTrue(BytePredicate.equal((byte) 10).test((byte) 10));
            assertFalse(BytePredicate.equal((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.notEqual((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.greaterThan((byte) 10).test((byte) 11));
            assertTrue(BytePredicate.greaterThanOrEqual((byte) 10).test((byte) 10));
            assertTrue(BytePredicate.lessThan((byte) 10).test((byte) 9));
            assertTrue(BytePredicate.lessThanOrEqual((byte) 10).test((byte) 10));
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
        public void testGetAsByte() {
            ByteSupplier supplier = () -> (byte) 127;
            assertEquals((byte) 127, supplier.getAsByte());
        }
    }

    @Nested
    public class ByteTernaryOperatorTest {
    }

    @Nested
    public class ByteToBooleanFunctionTest {
    }

    @Nested
    public class ByteToIntFunctionTest {
    }

    @Nested
    public class ByteTriConsumerTest {
    }

    @Nested
    public class ByteTriFunctionTest {
    }

    @Nested
    public class ByteTriPredicateTest {
    }

    @Nested
    public class ByteUnaryOperatorTest {
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
    }

    @Nested
    public class CharBiConsumerTest {
    }

    @Nested
    public class CharBiFunctionTest {
    }

    @Nested
    public class CharBinaryOperatorTest {
    }

    @Nested
    public class CharBiPredicateTest {
    }

    @Nested
    public class CharConsumerTest {
    }

    @Nested
    public class CharFunctionTest {
        @Test
        public void testStaticBoxAndIdentity() {
            assertEquals(Character.valueOf('c'), CharFunction.BOX.apply('c'));
            assertEquals('k', CharFunction.identity().apply('k'));
        }
    }

    @Nested
    public class CharNConsumerTest {
    }

    @Nested
    public class CharNFunctionTest {
    }

    @Nested
    public class CharPredicateTest {
    }

    @Nested
    public class CharSupplierTest {

        @Test
        public void testGetAsChar() {
            CharSupplier supplier = () -> 'x';
            assertEquals('x', supplier.getAsChar());
        }
    }

    @Nested
    public class CharTernaryOperatorTest {
    }

    @Nested
    public class CharToBooleanFunctionTest {
    }

    @Nested
    public class CharToIntFunctionTest {
    }

    @Nested
    public class CharTriConsumerTest {
    }

    @Nested
    public class CharTriFunctionTest {
    }

    @Nested
    public class CharTriPredicateTest {
    }

    @Nested
    public class CharUnaryOperatorTest {
    }

    @Nested
    public class ConsumerTest {
    }

    private static final double DELTA = 1e-9;

    @Nested
    public class DoubleBiConsumerTest {
    }

    @Nested
    public class DoubleBiFunctionTest {
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
    }

    @Nested
    public class DoubleConsumerTest {
    }

    @Nested
    public class DoubleFunctionTest {
    }

    @Nested
    public class DoubleMapMultiConsumerTest {
    }

    @Nested
    public class DoubleNConsumerTest {
    }

    @Nested
    public class DoubleNFunctionTest {
    }

    @Nested
    public class DoubleObjConsumerTest {
    }

    @Nested
    public class DoubleObjFunctionTest {
    }

    @Nested
    public class DoubleObjPredicateTest {
    }

    @Nested
    public class DoublePredicateTest {
    }

    @Nested
    public class DoubleSupplierTest {

        @Test
        public void testGetAsDouble() {
            DoubleSupplier supplier = () -> Math.PI;
            assertEquals(Math.PI, supplier.getAsDouble(), DELTA);
        }
    }

    @Nested
    public class DoubleTernaryOperatorTest {
    }

    @Nested
    public class DoubleToFloatFunctionTest {

        @Test
        public void testApplyAsFloat() {
            DoubleToFloatFunction custom = val -> (float) (val * 2.0);
            assertEquals(5.0f, custom.applyAsFloat(2.5d), (float) DELTA);
        }
    }

    @Nested
    public class DoubleToIntFunctionTest {
    }

    @Nested
    public class DoubleToLongFunctionTest {

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
    }

    @Nested
    public class DoubleTriFunctionTest {
    }

    @Nested
    public class DoubleTriPredicateTest {
    }

    @Nested
    public class DoubleUnaryOperatorTest {

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
    }

    @Nested
    public class FloatBiFunctionTest {
    }

    @Nested
    public class FloatBinaryOperatorTest {
    }

    @Nested
    public class FloatBiPredicateTest {
    }

    @Nested
    public class FloatConsumerTest {
    }

    @Nested
    public class FloatFunctionTest {
    }

    @Nested
    public class FloatNConsumerTest {
    }

    @Nested
    public class FloatNFunctionTest {
    }

    @Nested
    public class FloatPredicateTest {
    }

    @Nested
    public class FloatSupplierTest {

        @Test
        public void testGetAsFloat() {
            FloatSupplier supplier = () -> 3.14f;
            assertEquals(3.14f, supplier.getAsFloat(), FLOAT_DELTA);
        }
    }

    @Nested
    public class FloatTernaryOperatorTest {
    }

    @Nested
    public class FloatToDoubleFunctionTest {
    }

    @Nested
    public class FloatToIntFunctionTest {
    }

    @Nested
    public class FloatToLongFunctionTest {
    }

    @Nested
    public class FloatTriConsumerTest {
    }

    @Nested
    public class FloatTriFunctionTest {
    }

    @Nested
    public class FloatTriPredicateTest {
    }

    @Nested
    public class FloatUnaryOperatorTest {
    }

    @Nested
    public class FunctionTest {
    }

    @Nested
    public class IntBiConsumerTest {
    }

    @Nested
    public class IntBiFunctionTest {
    }

    @Nested
    public class IntBinaryOperatorTest {
    }

    @Nested
    public class IntBiObjConsumerTest {
    }

    @Nested
    public class IntBiObjFunctionTest {
    }

    @Nested
    public class IntBiObjPredicateTest {
    }

    @Nested
    public class IntBiPredicateTest {
    }

    @Nested
    public class IntConsumerTest {
    }

    @Nested
    public class IntFunctionTest {
    }

    @Nested
    public class IntMapMultiConsumerTest {
    }

    @Nested
    public class IntNConsumerTest {
    }

    @Nested
    public class IntNFunctionTest {
    }

    @Nested
    public class IntObjConsumerTest {
    }

    @Nested
    public class IntObjFunctionTest {
    }

    @Nested
    public class IntObjOperatorTest {
    }

    @Nested
    public class IntObjPredicateTest {
    }

    @Nested
    public class IntPredicateTest {
    }

    @Nested
    public class IntSupplierTest {

        @Test
        public void testGetAsInt() {
            IntSupplier supplier = () -> 42;
            assertEquals(42, supplier.getAsInt());
        }
    }

    @Nested
    public class IntTernaryOperatorTest {
    }

    @Nested
    public class IntToBooleanFunctionTest {
    }

    @Nested
    public class IntToByteFunctionTest {
    }

    @Nested
    public class IntToCharFunctionTest {
    }

    @Nested
    public class IntToDoubleFunctionTest {
    }

    @Nested
    public class IntToFloatFunctionTest {
    }

    @Nested
    public class IntToLongFunctionTest {
    }

    @Nested
    public class IntToShortFunctionTest {

        @Test
        public void testApplyAsShort() {
            IntToShortFunction custom = val -> (short) (val / 2);
            assertEquals((short) 5, custom.applyAsShort(10));
        }
    }

    @Nested
    public class IntTriConsumerTest {
    }

    @Nested
    public class IntTriFunctionTest {
    }

    @Nested
    public class IntTriPredicateTest {
    }

    @Nested
    public class IntUnaryOperatorTest {
    }

    @Nested
    public class LongBiConsumerTest {
    }

    @Nested
    public class LongBiFunctionTest {
    }

    @Nested
    public class LongBinaryOperatorTest {
    }

    @Nested
    public class LongBiPredicateTest {
    }

    @Nested
    public class LongConsumerTest {
    }

    @Nested
    public class LongFunctionTest {
    }

    @Nested
    public class LongMapMultiConsumerTest {
    }

    @Nested
    public class LongNConsumerTest {
    }

    @Nested
    public class LongNFunctionTest {
    }

    @Nested
    public class LongObjConsumerTest {
    }

    @Nested
    public class LongObjFunctionTest {
    }

    @Nested
    public class LongObjPredicateTest {
    }

    @Nested
    public class LongPredicateTest {
    }

    @Nested
    public class LongSupplierTest {

        @Test
        public void testGetAsLong() {
            LongSupplier supplier = () -> 42L;
            assertEquals(42L, supplier.getAsLong());
        }
    }

    @Nested
    public class LongTernaryOperatorTest {
    }

    @Nested
    public class LongToDoubleFunctionTest {
    }

    @Nested
    public class LongToFloatFunctionTest {
    }

    @Nested
    public class LongToIntFunctionTest {
    }

    @Nested
    public class LongTriConsumerTest {
    }

    @Nested
    public class LongTriFunctionTest {
    }

    @Nested
    public class LongTriPredicateTest {
    }

    @Nested
    public class LongUnaryOperatorTest {
    }

    @Nested
    public class NConsumerTest {
    }

    @Nested
    public class NFunctionTest {
    }

    @Nested
    public class NPredicateTest {
    }

    @Nested
    public class ObjBiIntConsumerTest {
    }

    @Nested
    public class ObjBiIntFunctionTest {
    }

    @Nested
    public class ObjBiIntPredicateTest {
    }

    @Nested
    public class ObjBooleanConsumerTest {
    }

    @Nested
    public class ObjByteConsumerTest {
    }

    @Nested
    public class ObjCharConsumerTest {
    }

    @Nested
    public class ObjDoubleConsumerTest {
    }

    @Nested
    public class ObjDoubleFunctionTest {
    }

    @Nested
    public class ObjDoublePredicateTest {
    }

    @Nested
    public class ObjFloatConsumerTest {
    }

    @Nested
    public class ObjIntConsumerTest {
    }

    @Nested
    public class ObjIntFunctionTest {
    }

    @Nested
    public class ObjIntPredicateTest {
    }

    @Nested
    public class ObjLongConsumerTest {
    }

    @Nested
    public class ObjLongFunctionTest {
    }

    @Nested
    public class ObjLongPredicateTest {
    }

    @Nested
    public class ObjShortConsumerTest {
    }

    @Nested
    public class PredicateTest {
    }

    @Nested
    public class QuadConsumerTest {
    }

    @Nested
    public class QuadFunctionTest {
    }

    @Nested
    public class QuadPredicateTest {
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
    }

    @Nested
    public class ShortBiConsumerTest {
    }

    @Nested
    public class ShortBiFunctionTest {
    }

    @Nested
    public class ShortBinaryOperatorTest {
    }

    @Nested
    public class ShortBiPredicateTest {
    }

    @Nested
    public class ShortConsumerTest {
    }

    @Nested
    public class ShortFunctionTest {
    }

    @Nested
    public class ShortNConsumerTest {
    }

    @Nested
    public class ShortNFunctionTest {
    }

    @Nested
    public class ShortPredicateTest {
    }

    @Nested
    public class ShortSupplierTest {

        @Test
        public void testGetAsShort() {
            ShortSupplier supplier = () -> (short) 42;
            assertEquals((short) 42, supplier.getAsShort());
        }
    }

    @Nested
    public class ShortTernaryOperatorTest {
    }

    @Nested
    public class ShortToIntFunctionTest {
    }

    @Nested
    public class ShortTriConsumerTest {
    }

    @Nested
    public class ShortTriFunctionTest {
    }

    @Nested
    public class ShortTriPredicateTest {
    }

    @Nested
    public class ShortUnaryOperatorTest {
    }

    @Nested
    public class SupplierTest {
        @Test
        public void testGet() {
            Supplier<String> supplier = () -> "hello";
            assertEquals("hello", supplier.get());
        }
    }

    @Nested
    public class ToBooleanBiFunctionTest {
    }

    @Nested
    public class ToBooleanFunctionTest {
        @Test
        public void testStaticUnbox() {
            assertTrue(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.TRUE));
            assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.FALSE));
            assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(null));
        }
    }

    @Nested
    public class ToByteBiFunctionTest {
    }

    @Nested
    public class ToByteFunctionTest {

        @Test
        public void testStaticFromNum() {
            assertEquals((byte) 123, ToByteFunction.FROM_NUM.applyAsByte(123));
            assertEquals((byte) 45, ToByteFunction.FROM_NUM.applyAsByte(45.67d));
            assertEquals((byte) 0, ToByteFunction.FROM_NUM.applyAsByte(null));
        }
    }

    @Nested
    public class ToCharBiFunctionTest {
    }

    @Nested
    public class ToCharFunctionTest {
    }

    @Nested
    public class ToDoubleBiFunctionTest {
    }

    @Nested
    public class ToDoubleFunctionTest {
    }

    @Nested
    public class ToDoubleTriFunctionTest {
    }

    @Nested
    public class ToFloatBiFunctionTest {
    }

    @Nested
    public class ToFloatFunctionTest {
    }

    @Nested
    public class ToIntBiFunctionTest {
    }

    @Nested
    public class ToIntFunctionTest {
    }

    @Nested
    public class ToIntTriFunctionTest {
    }

    @Nested
    public class ToLongBiFunctionTest {
    }

    @Nested
    public class ToLongFunctionTest {
    }

    @Nested
    public class ToLongTriFunctionTest {
    }

    @Nested
    public class ToShortBiFunctionTest {
    }

    @Nested
    public class ToShortFunctionTest {
    }

    @Nested
    public class TriConsumerTest {
    }

    @Nested
    public class TriFunctionTest {
    }

    @Nested
    public class TriPredicateTest {
    }

    @Nested
    public class UnaryOperatorTest {
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
