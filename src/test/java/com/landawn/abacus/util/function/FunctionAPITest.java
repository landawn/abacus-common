package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables;

public class FunctionAPITest extends TestBase {

    private static final double DELTA = 1e-9;
    private static final float FLOAT_DELTA = 1e-6f;

    @Test
    public void testBiConsumer_toThrowable() {
        BiConsumer<String, Integer> consumer = (s, i) -> {
        };
        Throwables.BiConsumer<String, Integer, ?> throwableConsumer = consumer.toThrowable();
        assertNotNull(throwableConsumer);
        assertNotNull(throwableConsumer);
    }

    @Test
    public void testBiIntObjPredicate() {
        BiIntObjPredicate<String> isSumEven = (i, j, s) -> (i + j) % 2 == 0;
        BiIntObjPredicate<String> isStringLong = (i, j, s) -> s.length() > 5;

        assertTrue(isSumEven.test(2, 4, "any"));
        assertFalse(isSumEven.test(2, 3, "any"));

        BiIntObjPredicate<String> isSumOdd = isSumEven.negate();
        assertFalse(isSumOdd.test(2, 4, "any"));
        assertTrue(isSumOdd.test(2, 3, "any"));

        BiIntObjPredicate<String> and = isSumEven.and(isStringLong);
        assertTrue(and.test(2, 2, "long string"));
        assertFalse(and.test(2, 3, "long string"));
        assertFalse(and.test(2, 2, "short"));
        assertFalse(and.test(2, 3, "short"));

        BiIntObjPredicate<String> or = isSumEven.or(isStringLong);
        assertTrue(or.test(2, 2, "long string"));
        assertTrue(or.test(2, 3, "long string"));
        assertTrue(or.test(2, 2, "short"));
        assertFalse(or.test(2, 3, "short"));
    }

    @Test
    public void testBooleanBinaryOperator() {
        BooleanBinaryOperator logicalAnd = (left, right) -> left && right;
        assertTrue(logicalAnd.applyAsBoolean(true, true));
        assertFalse(logicalAnd.applyAsBoolean(true, false));
        assertFalse(logicalAnd.applyAsBoolean(false, true));
        assertFalse(logicalAnd.applyAsBoolean(false, false));
    }

    @Test
    public void testBooleanBiPredicate() {
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
    public void testBooleanFunction() {
        assertEquals(Boolean.TRUE, BooleanFunction.BOX.apply(true));
        assertEquals(Boolean.FALSE, BooleanFunction.BOX.apply(false));
        BooleanFunction<Boolean> identity = BooleanFunction.identity();
        assertTrue(identity.apply(true));
        assertFalse(identity.apply(false));
    }

    @Test
    public void testBooleanPredicateAndSupplier() {
        BooleanPredicate pred = val -> val;
        assertSame(pred, BooleanPredicate.of(pred));
        assertTrue(((BooleanSupplier) () -> true).getAsBoolean());
    }

    @Test
    public void testBooleanToPrimitiveFunctions() {
        assertEquals((byte) 1, BooleanToByteFunction.DEFAULT.applyAsByte(true));
        assertEquals((byte) 0, BooleanToByteFunction.DEFAULT.applyAsByte(false));
        BooleanToByteFunction customByte = value -> value ? (byte) -1 : (byte) -2;
        assertEquals((byte) -1, customByte.applyAsByte(true));
        assertEquals((byte) -2, customByte.applyAsByte(false));

        BooleanToCharFunction customChar = value -> value ? 'T' : 'F';
        assertEquals('T', customChar.applyAsChar(true));
        assertEquals('F', customChar.applyAsChar(false));

        BooleanToIntFunction customInt = value -> value ? 100 : -100;
        assertEquals(100, customInt.applyAsInt(true));
        assertEquals(-100, customInt.applyAsInt(false));
    }

    @Test
    public void testBooleanUnaryOperator_compose() {
        BooleanUnaryOperator not = operand -> !operand;
        BooleanUnaryOperator identity = BooleanUnaryOperator.identity();
        assertTrue(identity.compose(not).applyAsBoolean(false));
        assertFalse(identity.compose(not).applyAsBoolean(true));
        assertTrue(not.compose(not).applyAsBoolean(true));
        assertFalse(not.compose(not).applyAsBoolean(false));
    }

    @Test
    public void testBytePredicate() {
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

        BytePredicate isNegative = BytePredicate.IS_POSITIVE.negate().and(BytePredicate.IS_ZERO.negate());
        assertTrue(isNegative.test((byte) -5));
        assertFalse(isNegative.test((byte) 0));
        assertFalse(isNegative.test((byte) 5));
        BytePredicate isZeroOrPositive = BytePredicate.IS_ZERO.or(BytePredicate.IS_POSITIVE);
        assertTrue(isZeroOrPositive.test((byte) 0));
        assertTrue(isZeroOrPositive.test((byte) 5));
        assertFalse(isZeroOrPositive.test((byte) -5));
    }

    @Test
    public void testByteSupplier() {
        assertEquals((byte) 127, ((ByteSupplier) () -> (byte) 127).getAsByte());
    }

    @Test
    public void testCallable() {
        assertEquals("Success", ((Callable<String>) () -> "Success").call());
        Callable<String> failing = () -> {
            throw new IllegalStateException("error");
        };
        assertThrows(IllegalStateException.class, failing::call);

        AtomicBoolean called = new AtomicBoolean(false);
        Callable<String> callable = () -> {
            called.set(true);
            return "done";
        };
        callable.toRunnable().run();
        assertTrue(called.get());
    }

    @Test
    public void testCharFunctionAndSupplier() {
        assertEquals(Character.valueOf('c'), CharFunction.BOX.apply('c'));
        assertEquals('k', CharFunction.identity().apply('k'));
        assertEquals('x', ((CharSupplier) () -> 'x').getAsChar());
    }

    @Test
    public void testDoubleOperators() {
        assertEquals(10.0, ((DoubleBinaryOperator) (left, right) -> left * right).applyAsDouble(2.5, 4.0), DELTA);
        assertEquals(Math.PI, ((DoubleSupplier) () -> Math.PI).getAsDouble(), DELTA);
        assertEquals(5.0f, ((DoubleToFloatFunction) val -> (float) (val * 2.0)).applyAsFloat(2.5d), (float) DELTA);

        DoubleToLongFunction floor = val -> (long) Math.floor(val);
        assertEquals(123L, floor.applyAsLong(123.99d));
        assertEquals(-124L, floor.applyAsLong(-123.01d));

        DoubleUnaryOperator addOne = d -> d + 1;
        java.util.function.DoubleUnaryOperator timesTwo = d -> d * 2;
        assertEquals(21.0, addOne.compose(timesTwo).applyAsDouble(10.0), DELTA);
        assertEquals(22.0, addOne.andThen(timesTwo).applyAsDouble(10.0), DELTA);
    }

    @Test
    public void testFloatAndIntSuppliers() {
        assertEquals(3.14f, ((FloatSupplier) () -> 3.14f).getAsFloat(), FLOAT_DELTA);
        assertEquals(42, ((IntSupplier) () -> 42).getAsInt());
        assertEquals((short) 5, ((IntToShortFunction) val -> (short) (val / 2)).applyAsShort(10));
        assertEquals(42L, ((LongSupplier) () -> 42L).getAsLong());
        assertEquals((short) 42, ((ShortSupplier) () -> (short) 42).getAsShort());
        assertEquals("hello", ((Supplier<String>) () -> "hello").get());
    }

    @Test
    public void testRunnable() {
        AtomicBoolean hasRun = new AtomicBoolean(false);
        Runnable runnable = () -> hasRun.set(true);
        runnable.run();
        assertTrue(hasRun.get());

        AtomicBoolean called = new AtomicBoolean(false);
        com.landawn.abacus.util.function.Runnable fnRunnable = () -> called.set(true);
        try {
            assertNull(fnRunnable.toCallable().call());
        } catch (Exception e) {
            fail("Should not have thrown an exception");
        }
        assertTrue(called.get());
    }

    @Test
    public void testToPrimitiveBiFunctions_toThrowable() throws Exception {
        ToBooleanBiFunction<String, String> booleanFn = String::equals;
        Throwables.ToBooleanBiFunction<String, String, Exception> booleanThrowable = booleanFn.toThrowable();
        assertSame(booleanFn, booleanThrowable);
        assertTrue(booleanThrowable.applyAsBoolean("a", "a"));

        ToByteBiFunction<Integer, Integer> byteFn = (a, b) -> (byte) (a + b);
        Throwables.ToByteBiFunction<Integer, Integer, Exception> byteThrowable = byteFn.toThrowable();
        assertSame(byteFn, byteThrowable);
        assertEquals((byte) 3, byteThrowable.applyAsByte(1, 2));

        ToCharBiFunction<String, Integer> charFn = String::charAt;
        Throwables.ToCharBiFunction<String, Integer, Exception> charThrowable = charFn.toThrowable();
        assertSame(charFn, charThrowable);
        assertEquals('b', charThrowable.applyAsChar("abc", 1));

        ToFloatBiFunction<Integer, Integer> floatFn = (a, b) -> (float) a / b;
        Throwables.ToFloatBiFunction<Integer, Integer, Exception> floatThrowable = floatFn.toThrowable();
        assertSame(floatFn, floatThrowable);
        assertEquals(2.5f, floatThrowable.applyAsFloat(5, 2));

        ToShortBiFunction<Integer, Integer> shortFn = (a, b) -> (short) (a + b);
        Throwables.ToShortBiFunction<Integer, Integer, Exception> shortThrowable = shortFn.toThrowable();
        assertSame(shortFn, shortThrowable);
        assertEquals((short) 3, shortThrowable.applyAsShort(1, 2));
    }

    @Test
    public void testToPrimitiveFunctions() {
        assertTrue(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.TRUE));
        assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(Boolean.FALSE));
        assertFalse(ToBooleanFunction.UNBOX.applyAsBoolean(null));

        assertEquals((byte) 123, ToByteFunction.FROM_NUM.applyAsByte(123));
        assertEquals((byte) 45, ToByteFunction.FROM_NUM.applyAsByte(45.67d));
        assertEquals((byte) 0, ToByteFunction.FROM_NUM.applyAsByte(null));
    }

    @Test
    public void testRandomInstancesNotNull() {
        assertNotNull(BooleanSupplier.RANDOM);
    }
}
