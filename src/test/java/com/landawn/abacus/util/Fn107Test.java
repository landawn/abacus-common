package com.landawn.abacus.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;

@Tag("new-test")
public class Fn107Test extends TestBase {

    @Nested
    public class FnFCTest {

        @Test
        public void testIsZero() {
            CharPredicate predicate = Fn.FC.isZero();
            Assertions.assertTrue(predicate.test('\0'));
            Assertions.assertFalse(predicate.test('a'));
            Assertions.assertFalse(predicate.test(' '));
            Assertions.assertFalse(predicate.test('1'));
        }

        @Test
        public void testIsWhitespace() {
            CharPredicate predicate = Fn.FC.isWhitespace();
            Assertions.assertTrue(predicate.test(' '));
            Assertions.assertTrue(predicate.test('\t'));
            Assertions.assertTrue(predicate.test('\n'));
            Assertions.assertTrue(predicate.test('\r'));
            Assertions.assertFalse(predicate.test('a'));
            Assertions.assertFalse(predicate.test('1'));
            Assertions.assertFalse(predicate.test('\0'));
        }

        @Test
        public void testEqual() {
            CharBiPredicate predicate = Fn.FC.equal();
            Assertions.assertTrue(predicate.test('a', 'a'));
            Assertions.assertTrue(predicate.test('\0', '\0'));
            Assertions.assertFalse(predicate.test('a', 'b'));
            Assertions.assertFalse(predicate.test('A', 'a'));
        }

        @Test
        public void testNotEqual() {
            CharBiPredicate predicate = Fn.FC.notEqual();
            Assertions.assertTrue(predicate.test('a', 'b'));
            Assertions.assertTrue(predicate.test('A', 'a'));
            Assertions.assertFalse(predicate.test('a', 'a'));
            Assertions.assertFalse(predicate.test('\0', '\0'));
        }

        @Test
        public void testGreaterThan() {
            CharBiPredicate predicate = Fn.FC.greaterThan();
            Assertions.assertTrue(predicate.test('b', 'a'));
            Assertions.assertTrue(predicate.test('z', 'a'));
            Assertions.assertFalse(predicate.test('a', 'a'));
            Assertions.assertFalse(predicate.test('a', 'b'));
        }

        @Test
        public void testGreaterEqual() {
            CharBiPredicate predicate = Fn.FC.greaterEqual();
            Assertions.assertTrue(predicate.test('b', 'a'));
            Assertions.assertTrue(predicate.test('a', 'a'));
            Assertions.assertFalse(predicate.test('a', 'b'));
        }

        @Test
        public void testLessThan() {
            CharBiPredicate predicate = Fn.FC.lessThan();
            Assertions.assertTrue(predicate.test('a', 'b'));
            Assertions.assertTrue(predicate.test('a', 'z'));
            Assertions.assertFalse(predicate.test('a', 'a'));
            Assertions.assertFalse(predicate.test('b', 'a'));
        }

        @Test
        public void testLessEqual() {
            CharBiPredicate predicate = Fn.FC.lessEqual();
            Assertions.assertTrue(predicate.test('a', 'b'));
            Assertions.assertTrue(predicate.test('a', 'a'));
            Assertions.assertFalse(predicate.test('b', 'a'));
        }

        @Test
        public void testUnbox() {
            ToCharFunction<Character> func = Fn.FC.unbox();
            Assertions.assertEquals('a', func.applyAsChar(Character.valueOf('a')));
            Assertions.assertEquals('Z', func.applyAsChar(Character.valueOf('Z')));
            Assertions.assertEquals('\0', func.applyAsChar(Character.valueOf('\0')));
        }

        @Test
        public void testP() {
            CharPredicate original = ch -> ch == 'a';
            CharPredicate result = Fn.FC.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test('a'));
            Assertions.assertFalse(result.test('b'));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FC.p(null));
        }

        @Test
        public void testF() {
            CharFunction<String> original = ch -> String.valueOf(ch);
            CharFunction<String> result = Fn.FC.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("a", result.apply('a'));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FC.f(null));
        }

        @Test
        public void testC() {
            final StringBuilder sb = new StringBuilder();
            CharConsumer original = ch -> sb.append(ch);
            CharConsumer result = Fn.FC.c(original);
            Assertions.assertSame(original, result);

            result.accept('x');
            Assertions.assertEquals("x", sb.toString());

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FC.c(null));
        }

        @Test
        public void testLen() {
            Function<char[], Integer> lenFunc = Fn.FC.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new char[0]));
            Assertions.assertEquals(3, lenFunc.apply(new char[] { 'a', 'b', 'c' }));
            Assertions.assertEquals(5, lenFunc.apply(new char[] { '1', '2', '3', '4', '5' }));
        }

        @Test
        public void testAlternate() {
            CharBiFunction<MergeResult> func = Fn.FC.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply('a', 'b'));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply('c', 'd'));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply('e', 'f'));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply('g', 'h'));
        }

        @Test
        public void testCharBinaryOperatorsMIN() {
            CharBinaryOperator minOp = Fn.FC.CharBinaryOperators.MIN;
            Assertions.assertEquals('a', minOp.applyAsChar('a', 'b'));
            Assertions.assertEquals('a', minOp.applyAsChar('b', 'a'));
            Assertions.assertEquals('a', minOp.applyAsChar('a', 'a'));
            Assertions.assertEquals('\0', minOp.applyAsChar('\0', 'a'));
            Assertions.assertEquals('A', minOp.applyAsChar('A', 'a'));
        }

        @Test
        public void testCharBinaryOperatorsMAX() {
            CharBinaryOperator maxOp = Fn.FC.CharBinaryOperators.MAX;
            Assertions.assertEquals('b', maxOp.applyAsChar('a', 'b'));
            Assertions.assertEquals('b', maxOp.applyAsChar('b', 'a'));
            Assertions.assertEquals('a', maxOp.applyAsChar('a', 'a'));
            Assertions.assertEquals('a', maxOp.applyAsChar('\0', 'a'));
            Assertions.assertEquals('a', maxOp.applyAsChar('A', 'a'));
        }
    }

    @Nested
    public class FnFBTest {

        @Test
        public void testPositive() {
            BytePredicate predicate = Fn.FB.positive();
            Assertions.assertTrue(predicate.test((byte) 1));
            Assertions.assertTrue(predicate.test((byte) 127));
            Assertions.assertFalse(predicate.test((byte) 0));
            Assertions.assertFalse(predicate.test((byte) -1));
            Assertions.assertFalse(predicate.test((byte) -128));
        }

        @Test
        public void testNotNegative() {
            BytePredicate predicate = Fn.FB.notNegative();
            Assertions.assertTrue(predicate.test((byte) 0));
            Assertions.assertTrue(predicate.test((byte) 1));
            Assertions.assertTrue(predicate.test((byte) 127));
            Assertions.assertFalse(predicate.test((byte) -1));
            Assertions.assertFalse(predicate.test((byte) -128));
        }

        @Test
        public void testEqual() {
            ByteBiPredicate predicate = Fn.FB.equal();
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 5));
            Assertions.assertTrue(predicate.test((byte) -10, (byte) -10));
            Assertions.assertTrue(predicate.test((byte) 0, (byte) 0));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 6));
            Assertions.assertFalse(predicate.test((byte) -5, (byte) 5));
        }

        @Test
        public void testNotEqual() {
            ByteBiPredicate predicate = Fn.FB.notEqual();
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 6));
            Assertions.assertTrue(predicate.test((byte) -5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 0, (byte) 0));
        }

        @Test
        public void testGreaterThan() {
            ByteBiPredicate predicate = Fn.FB.greaterThan();
            Assertions.assertTrue(predicate.test((byte) 10, (byte) 5));
            Assertions.assertTrue(predicate.test((byte) 0, (byte) -1));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 10));
        }

        @Test
        public void testGreaterEqual() {
            ByteBiPredicate predicate = Fn.FB.greaterEqual();
            Assertions.assertTrue(predicate.test((byte) 10, (byte) 5));
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 10));
        }

        @Test
        public void testLessThan() {
            ByteBiPredicate predicate = Fn.FB.lessThan();
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 10));
            Assertions.assertTrue(predicate.test((byte) -1, (byte) 0));
            Assertions.assertFalse(predicate.test((byte) 5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 10, (byte) 5));
        }

        @Test
        public void testLessEqual() {
            ByteBiPredicate predicate = Fn.FB.lessEqual();
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 10));
            Assertions.assertTrue(predicate.test((byte) 5, (byte) 5));
            Assertions.assertFalse(predicate.test((byte) 10, (byte) 5));
        }

        @Test
        public void testUnbox() {
            ToByteFunction<Byte> func = Fn.FB.unbox();
            Assertions.assertEquals((byte) 42, func.applyAsByte(Byte.valueOf((byte) 42)));
            Assertions.assertEquals((byte) -128, func.applyAsByte(Byte.valueOf((byte) -128)));
            Assertions.assertEquals((byte) 127, func.applyAsByte(Byte.valueOf((byte) 127)));
        }

        @Test
        public void testP() {
            BytePredicate original = b -> b > 0;
            BytePredicate result = Fn.FB.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test((byte) 5));
            Assertions.assertFalse(result.test((byte) -5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FB.p(null));
        }

        @Test
        public void testF() {
            ByteFunction<String> original = b -> String.valueOf(b);
            ByteFunction<String> result = Fn.FB.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("10", result.apply((byte) 10));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FB.f(null));
        }

        @Test
        public void testC() {
            final int[] sum = { 0 };
            ByteConsumer original = b -> sum[0] += b;
            ByteConsumer result = Fn.FB.c(original);
            Assertions.assertSame(original, result);

            result.accept((byte) 5);
            Assertions.assertEquals(5, sum[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FB.c(null));
        }

        @Test
        public void testLen() {
            Function<byte[], Integer> lenFunc = Fn.FB.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new byte[0]));
            Assertions.assertEquals(3, lenFunc.apply(new byte[] { 1, 2, 3 }));
            Assertions.assertEquals(5, lenFunc.apply(new byte[] { -1, -2, -3, -4, -5 }));
        }

        @Test
        public void testSum() {
            Function<byte[], Integer> sumFunc = Fn.FB.sum();
            Assertions.assertEquals(0, sumFunc.apply(new byte[0]));
            Assertions.assertEquals(6, sumFunc.apply(new byte[] { 1, 2, 3 }));
            Assertions.assertEquals(0, sumFunc.apply(new byte[] { -5, 5 }));
            Assertions.assertEquals(15, sumFunc.apply(new byte[] { 1, 2, 3, 4, 5 }));
        }

        @Test
        public void testAverage() {
            Function<byte[], Double> avgFunc = Fn.FB.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new byte[0]));
            Assertions.assertEquals(2.0, avgFunc.apply(new byte[] { 1, 2, 3 }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new byte[] { -5, 5 }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new byte[] { 1, 2, 3, 4, 5 }), 0.0001);
        }

        @Test
        public void testAlternate() {
            ByteBiFunction<MergeResult> func = Fn.FB.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply((byte) 1, (byte) 2));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply((byte) 3, (byte) 4));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply((byte) 5, (byte) 6));
        }

        @Test
        public void testByteBinaryOperatorsMIN() {
            ByteBinaryOperator minOp = Fn.FB.ByteBinaryOperators.MIN;
            Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 5, (byte) 10));
            Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 10, (byte) 5));
            Assertions.assertEquals((byte) 5, minOp.applyAsByte((byte) 5, (byte) 5));
            Assertions.assertEquals((byte) -10, minOp.applyAsByte((byte) -10, (byte) 5));
        }

        @Test
        public void testByteBinaryOperatorsMAX() {
            ByteBinaryOperator maxOp = Fn.FB.ByteBinaryOperators.MAX;
            Assertions.assertEquals((byte) 10, maxOp.applyAsByte((byte) 5, (byte) 10));
            Assertions.assertEquals((byte) 10, maxOp.applyAsByte((byte) 10, (byte) 5));
            Assertions.assertEquals((byte) 5, maxOp.applyAsByte((byte) 5, (byte) 5));
            Assertions.assertEquals((byte) 5, maxOp.applyAsByte((byte) -10, (byte) 5));
        }
    }

    @Nested
    public class FnFSTest {

        @Test
        public void testPositive() {
            ShortPredicate predicate = Fn.FS.positive();
            Assertions.assertTrue(predicate.test((short) 1));
            Assertions.assertTrue(predicate.test((short) 32767));
            Assertions.assertFalse(predicate.test((short) 0));
            Assertions.assertFalse(predicate.test((short) -1));
            Assertions.assertFalse(predicate.test((short) -32768));
        }

        @Test
        public void testNotNegative() {
            ShortPredicate predicate = Fn.FS.notNegative();
            Assertions.assertTrue(predicate.test((short) 0));
            Assertions.assertTrue(predicate.test((short) 1));
            Assertions.assertTrue(predicate.test((short) 32767));
            Assertions.assertFalse(predicate.test((short) -1));
            Assertions.assertFalse(predicate.test((short) -32768));
        }

        @Test
        public void testEqual() {
            ShortBiPredicate predicate = Fn.FS.equal();
            Assertions.assertTrue(predicate.test((short) 100, (short) 100));
            Assertions.assertTrue(predicate.test((short) -1000, (short) -1000));
            Assertions.assertTrue(predicate.test((short) 0, (short) 0));
            Assertions.assertFalse(predicate.test((short) 100, (short) 101));
            Assertions.assertFalse(predicate.test((short) -100, (short) 100));
        }

        @Test
        public void testNotEqual() {
            ShortBiPredicate predicate = Fn.FS.notEqual();
            Assertions.assertTrue(predicate.test((short) 100, (short) 101));
            Assertions.assertTrue(predicate.test((short) -100, (short) 100));
            Assertions.assertFalse(predicate.test((short) 100, (short) 100));
            Assertions.assertFalse(predicate.test((short) 0, (short) 0));
        }

        @Test
        public void testGreaterThan() {
            ShortBiPredicate predicate = Fn.FS.greaterThan();
            Assertions.assertTrue(predicate.test((short) 100, (short) 50));
            Assertions.assertTrue(predicate.test((short) 0, (short) -1));
            Assertions.assertFalse(predicate.test((short) 50, (short) 50));
            Assertions.assertFalse(predicate.test((short) 50, (short) 100));
        }

        @Test
        public void testGreaterEqual() {
            ShortBiPredicate predicate = Fn.FS.greaterEqual();
            Assertions.assertTrue(predicate.test((short) 100, (short) 50));
            Assertions.assertTrue(predicate.test((short) 50, (short) 50));
            Assertions.assertFalse(predicate.test((short) 50, (short) 100));
        }

        @Test
        public void testLessThan() {
            ShortBiPredicate predicate = Fn.FS.lessThan();
            Assertions.assertTrue(predicate.test((short) 50, (short) 100));
            Assertions.assertTrue(predicate.test((short) -1, (short) 0));
            Assertions.assertFalse(predicate.test((short) 50, (short) 50));
            Assertions.assertFalse(predicate.test((short) 100, (short) 50));
        }

        @Test
        public void testLessEqual() {
            ShortBiPredicate predicate = Fn.FS.lessEqual();
            Assertions.assertTrue(predicate.test((short) 50, (short) 100));
            Assertions.assertTrue(predicate.test((short) 50, (short) 50));
            Assertions.assertFalse(predicate.test((short) 100, (short) 50));
        }

        @Test
        public void testUnbox() {
            ToShortFunction<Short> func = Fn.FS.unbox();
            Assertions.assertEquals((short) 1234, func.applyAsShort(Short.valueOf((short) 1234)));
            Assertions.assertEquals((short) -32768, func.applyAsShort(Short.valueOf((short) -32768)));
            Assertions.assertEquals((short) 32767, func.applyAsShort(Short.valueOf((short) 32767)));
        }

        @Test
        public void testP() {
            ShortPredicate original = s -> s > 0;
            ShortPredicate result = Fn.FS.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test((short) 5));
            Assertions.assertFalse(result.test((short) -5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FS.p(null));
        }

        @Test
        public void testF() {
            ShortFunction<String> original = s -> String.valueOf(s);
            ShortFunction<String> result = Fn.FS.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("100", result.apply((short) 100));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FS.f(null));
        }

        @Test
        public void testC() {
            final int[] sum = { 0 };
            ShortConsumer original = s -> sum[0] += s;
            ShortConsumer result = Fn.FS.c(original);
            Assertions.assertSame(original, result);

            result.accept((short) 50);
            Assertions.assertEquals(50, sum[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FS.c(null));
        }

        @Test
        public void testLen() {
            Function<short[], Integer> lenFunc = Fn.FS.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new short[0]));
            Assertions.assertEquals(3, lenFunc.apply(new short[] { 1, 2, 3 }));
            Assertions.assertEquals(5, lenFunc.apply(new short[] { -1, -2, -3, -4, -5 }));
        }

        @Test
        public void testSum() {
            Function<short[], Integer> sumFunc = Fn.FS.sum();
            Assertions.assertEquals(0, sumFunc.apply(new short[0]));
            Assertions.assertEquals(600, sumFunc.apply(new short[] { 100, 200, 300 }));
            Assertions.assertEquals(0, sumFunc.apply(new short[] { -500, 500 }));
            Assertions.assertEquals(15, sumFunc.apply(new short[] { 1, 2, 3, 4, 5 }));
        }

        @Test
        public void testAverage() {
            Function<short[], Double> avgFunc = Fn.FS.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new short[0]));
            Assertions.assertEquals(200.0, avgFunc.apply(new short[] { 100, 200, 300 }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new short[] { -500, 500 }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new short[] { 1, 2, 3, 4, 5 }), 0.0001);
        }

        @Test
        public void testAlternate() {
            ShortBiFunction<MergeResult> func = Fn.FS.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply((short) 10, (short) 20));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply((short) 30, (short) 40));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply((short) 50, (short) 60));
        }

        @Test
        public void testShortBinaryOperatorsMIN() {
            ShortBinaryOperator minOp = Fn.FS.ShortBinaryOperators.MIN;
            Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 50, (short) 100));
            Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 100, (short) 50));
            Assertions.assertEquals((short) 50, minOp.applyAsShort((short) 50, (short) 50));
            Assertions.assertEquals((short) -100, minOp.applyAsShort((short) -100, (short) 50));
        }

        @Test
        public void testShortBinaryOperatorsMAX() {
            ShortBinaryOperator maxOp = Fn.FS.ShortBinaryOperators.MAX;
            Assertions.assertEquals((short) 100, maxOp.applyAsShort((short) 50, (short) 100));
            Assertions.assertEquals((short) 100, maxOp.applyAsShort((short) 100, (short) 50));
            Assertions.assertEquals((short) 50, maxOp.applyAsShort((short) 50, (short) 50));
            Assertions.assertEquals((short) 50, maxOp.applyAsShort((short) -100, (short) 50));
        }
    }

    @Nested
    public class FnFITest {

        @Test
        public void testPositive() {
            IntPredicate predicate = Fn.FI.positive();
            Assertions.assertTrue(predicate.test(1));
            Assertions.assertTrue(predicate.test(Integer.MAX_VALUE));
            Assertions.assertFalse(predicate.test(0));
            Assertions.assertFalse(predicate.test(-1));
            Assertions.assertFalse(predicate.test(Integer.MIN_VALUE));
        }

        @Test
        public void testNotNegative() {
            IntPredicate predicate = Fn.FI.notNegative();
            Assertions.assertTrue(predicate.test(0));
            Assertions.assertTrue(predicate.test(1));
            Assertions.assertTrue(predicate.test(Integer.MAX_VALUE));
            Assertions.assertFalse(predicate.test(-1));
            Assertions.assertFalse(predicate.test(Integer.MIN_VALUE));
        }

        @Test
        public void testEqual() {
            IntBiPredicate predicate = Fn.FI.equal();
            Assertions.assertTrue(predicate.test(1000, 1000));
            Assertions.assertTrue(predicate.test(-10000, -10000));
            Assertions.assertTrue(predicate.test(0, 0));
            Assertions.assertFalse(predicate.test(1000, 1001));
            Assertions.assertFalse(predicate.test(-1000, 1000));
        }

        @Test
        public void testNotEqual() {
            IntBiPredicate predicate = Fn.FI.notEqual();
            Assertions.assertTrue(predicate.test(1000, 1001));
            Assertions.assertTrue(predicate.test(-1000, 1000));
            Assertions.assertFalse(predicate.test(1000, 1000));
            Assertions.assertFalse(predicate.test(0, 0));
        }

        @Test
        public void testGreaterThan() {
            IntBiPredicate predicate = Fn.FI.greaterThan();
            Assertions.assertTrue(predicate.test(1000, 500));
            Assertions.assertTrue(predicate.test(0, -1));
            Assertions.assertFalse(predicate.test(500, 500));
            Assertions.assertFalse(predicate.test(500, 1000));
        }

        @Test
        public void testGreaterEqual() {
            IntBiPredicate predicate = Fn.FI.greaterEqual();
            Assertions.assertTrue(predicate.test(1000, 500));
            Assertions.assertTrue(predicate.test(500, 500));
            Assertions.assertFalse(predicate.test(500, 1000));
        }

        @Test
        public void testLessThan() {
            IntBiPredicate predicate = Fn.FI.lessThan();
            Assertions.assertTrue(predicate.test(500, 1000));
            Assertions.assertTrue(predicate.test(-1, 0));
            Assertions.assertFalse(predicate.test(500, 500));
            Assertions.assertFalse(predicate.test(1000, 500));
        }

        @Test
        public void testLessEqual() {
            IntBiPredicate predicate = Fn.FI.lessEqual();
            Assertions.assertTrue(predicate.test(500, 1000));
            Assertions.assertTrue(predicate.test(500, 500));
            Assertions.assertFalse(predicate.test(1000, 500));
        }

        @Test
        public void testUnbox() {
            ToIntFunction<Integer> func = Fn.FI.unbox();
            Assertions.assertEquals(12345, func.applyAsInt(Integer.valueOf(12345)));
            Assertions.assertEquals(Integer.MIN_VALUE, func.applyAsInt(Integer.valueOf(Integer.MIN_VALUE)));
            Assertions.assertEquals(Integer.MAX_VALUE, func.applyAsInt(Integer.valueOf(Integer.MAX_VALUE)));
        }

        @Test
        public void testP() {
            IntPredicate original = i -> i > 0;
            IntPredicate result = Fn.FI.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test(5));
            Assertions.assertFalse(result.test(-5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FI.p(null));
        }

        @Test
        public void testF() {
            IntFunction<String> original = i -> String.valueOf(i);
            IntFunction<String> result = Fn.FI.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("1000", result.apply(1000));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FI.f(null));
        }

        @Test
        public void testC() {
            final int[] sum = { 0 };
            IntConsumer original = i -> sum[0] += i;
            IntConsumer result = Fn.FI.c(original);
            Assertions.assertSame(original, result);

            result.accept(500);
            Assertions.assertEquals(500, sum[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FI.c(null));
        }

        @Test
        public void testLen() {
            Function<int[], Integer> lenFunc = Fn.FI.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new int[0]));
            Assertions.assertEquals(3, lenFunc.apply(new int[] { 1, 2, 3 }));
            Assertions.assertEquals(5, lenFunc.apply(new int[] { -1, -2, -3, -4, -5 }));
        }

        @Test
        public void testSum() {
            Function<int[], Integer> sumFunc = Fn.FI.sum();
            Assertions.assertEquals(0, sumFunc.apply(new int[0]));
            Assertions.assertEquals(6000, sumFunc.apply(new int[] { 1000, 2000, 3000 }));
            Assertions.assertEquals(0, sumFunc.apply(new int[] { -5000, 5000 }));
            Assertions.assertEquals(15, sumFunc.apply(new int[] { 1, 2, 3, 4, 5 }));
        }

        @Test
        public void testAverage() {
            Function<int[], Double> avgFunc = Fn.FI.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new int[0]));
            Assertions.assertEquals(2000.0, avgFunc.apply(new int[] { 1000, 2000, 3000 }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new int[] { -5000, 5000 }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new int[] { 1, 2, 3, 4, 5 }), 0.0001);
        }

        @Test
        public void testAlternate() {
            IntBiFunction<MergeResult> func = Fn.FI.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(100, 200));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply(300, 400));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(500, 600));
        }

        @Test
        public void testIntBinaryOperatorsMIN() {
            IntBinaryOperator minOp = Fn.FI.IntBinaryOperators.MIN;
            Assertions.assertEquals(500, minOp.applyAsInt(500, 1000));
            Assertions.assertEquals(500, minOp.applyAsInt(1000, 500));
            Assertions.assertEquals(500, minOp.applyAsInt(500, 500));
            Assertions.assertEquals(-1000, minOp.applyAsInt(-1000, 500));

            Assertions.assertEquals(Integer.MIN_VALUE, minOp.applyAsInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        }

        @Test
        public void testIntBinaryOperatorsMAX() {
            IntBinaryOperator maxOp = Fn.FI.IntBinaryOperators.MAX;
            Assertions.assertEquals(1000, maxOp.applyAsInt(500, 1000));
            Assertions.assertEquals(1000, maxOp.applyAsInt(1000, 500));
            Assertions.assertEquals(500, maxOp.applyAsInt(500, 500));
            Assertions.assertEquals(500, maxOp.applyAsInt(-1000, 500));

            Assertions.assertEquals(Integer.MAX_VALUE, maxOp.applyAsInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
    }

    @Nested
    public class FnFLTest {

        @Test
        public void testPositive() {
            LongPredicate predicate = Fn.FL.positive();
            Assertions.assertTrue(predicate.test(1L));
            Assertions.assertTrue(predicate.test(Long.MAX_VALUE));
            Assertions.assertFalse(predicate.test(0L));
            Assertions.assertFalse(predicate.test(-1L));
            Assertions.assertFalse(predicate.test(Long.MIN_VALUE));
        }

        @Test
        public void testNotNegative() {
            LongPredicate predicate = Fn.FL.notNegative();
            Assertions.assertTrue(predicate.test(0L));
            Assertions.assertTrue(predicate.test(1L));
            Assertions.assertTrue(predicate.test(Long.MAX_VALUE));
            Assertions.assertFalse(predicate.test(-1L));
            Assertions.assertFalse(predicate.test(Long.MIN_VALUE));
        }

        @Test
        public void testEqual() {
            LongBiPredicate predicate = Fn.FL.equal();
            Assertions.assertTrue(predicate.test(1000000L, 1000000L));
            Assertions.assertTrue(predicate.test(-10000000L, -10000000L));
            Assertions.assertTrue(predicate.test(0L, 0L));
            Assertions.assertFalse(predicate.test(1000000L, 1000001L));
            Assertions.assertFalse(predicate.test(-1000000L, 1000000L));
        }

        @Test
        public void testNotEqual() {
            LongBiPredicate predicate = Fn.FL.notEqual();
            Assertions.assertTrue(predicate.test(1000000L, 1000001L));
            Assertions.assertTrue(predicate.test(-1000000L, 1000000L));
            Assertions.assertFalse(predicate.test(1000000L, 1000000L));
            Assertions.assertFalse(predicate.test(0L, 0L));
        }

        @Test
        public void testGreaterThan() {
            LongBiPredicate predicate = Fn.FL.greaterThan();
            Assertions.assertTrue(predicate.test(1000000L, 500000L));
            Assertions.assertTrue(predicate.test(0L, -1L));
            Assertions.assertFalse(predicate.test(500000L, 500000L));
            Assertions.assertFalse(predicate.test(500000L, 1000000L));
        }

        @Test
        public void testGreaterEqual() {
            LongBiPredicate predicate = Fn.FL.greaterEqual();
            Assertions.assertTrue(predicate.test(1000000L, 500000L));
            Assertions.assertTrue(predicate.test(500000L, 500000L));
            Assertions.assertFalse(predicate.test(500000L, 1000000L));
        }

        @Test
        public void testLessThan() {
            LongBiPredicate predicate = Fn.FL.lessThan();
            Assertions.assertTrue(predicate.test(500000L, 1000000L));
            Assertions.assertTrue(predicate.test(-1L, 0L));
            Assertions.assertFalse(predicate.test(500000L, 500000L));
            Assertions.assertFalse(predicate.test(1000000L, 500000L));
        }

        @Test
        public void testLessEqual() {
            LongBiPredicate predicate = Fn.FL.lessEqual();
            Assertions.assertTrue(predicate.test(500000L, 1000000L));
            Assertions.assertTrue(predicate.test(500000L, 500000L));
            Assertions.assertFalse(predicate.test(1000000L, 500000L));
        }

        @Test
        public void testUnbox() {
            ToLongFunction<Long> func = Fn.FL.unbox();
            Assertions.assertEquals(1234567890L, func.applyAsLong(Long.valueOf(1234567890L)));
            Assertions.assertEquals(Long.MIN_VALUE, func.applyAsLong(Long.valueOf(Long.MIN_VALUE)));
            Assertions.assertEquals(Long.MAX_VALUE, func.applyAsLong(Long.valueOf(Long.MAX_VALUE)));
        }

        @Test
        public void testP() {
            LongPredicate original = l -> l > 0;
            LongPredicate result = Fn.FL.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test(5L));
            Assertions.assertFalse(result.test(-5L));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FL.p(null));
        }

        @Test
        public void testF() {
            LongFunction<String> original = l -> String.valueOf(l);
            LongFunction<String> result = Fn.FL.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("1000000", result.apply(1000000L));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FL.f(null));
        }

        @Test
        public void testC() {
            final long[] sum = { 0 };
            LongConsumer original = l -> sum[0] += l;
            LongConsumer result = Fn.FL.c(original);
            Assertions.assertSame(original, result);

            result.accept(500000L);
            Assertions.assertEquals(500000L, sum[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FL.c(null));
        }

        @Test
        public void testLen() {
            Function<long[], Integer> lenFunc = Fn.FL.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new long[0]));
            Assertions.assertEquals(3, lenFunc.apply(new long[] { 1L, 2L, 3L }));
            Assertions.assertEquals(5, lenFunc.apply(new long[] { -1L, -2L, -3L, -4L, -5L }));
        }

        @Test
        public void testSum() {
            Function<long[], Long> sumFunc = Fn.FL.sum();
            Assertions.assertEquals(0L, sumFunc.apply(new long[0]));
            Assertions.assertEquals(6000000L, sumFunc.apply(new long[] { 1000000L, 2000000L, 3000000L }));
            Assertions.assertEquals(0L, sumFunc.apply(new long[] { -5000000L, 5000000L }));
            Assertions.assertEquals(15L, sumFunc.apply(new long[] { 1L, 2L, 3L, 4L, 5L }));
        }

        @Test
        public void testAverage() {
            Function<long[], Double> avgFunc = Fn.FL.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new long[0]));
            Assertions.assertEquals(2000000.0, avgFunc.apply(new long[] { 1000000L, 2000000L, 3000000L }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new long[] { -5000000L, 5000000L }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new long[] { 1L, 2L, 3L, 4L, 5L }), 0.0001);
        }

        @Test
        public void testAlternate() {
            LongBiFunction<MergeResult> func = Fn.FL.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(1000L, 2000L));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply(3000L, 4000L));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(5000L, 6000L));
        }

        @Test
        public void testLongBinaryOperatorsMIN() {
            LongBinaryOperator minOp = Fn.FL.LongBinaryOperators.MIN;
            Assertions.assertEquals(500000L, minOp.applyAsLong(500000L, 1000000L));
            Assertions.assertEquals(500000L, minOp.applyAsLong(1000000L, 500000L));
            Assertions.assertEquals(500000L, minOp.applyAsLong(500000L, 500000L));
            Assertions.assertEquals(-1000000L, minOp.applyAsLong(-1000000L, 500000L));

            Assertions.assertEquals(Long.MIN_VALUE, minOp.applyAsLong(Long.MIN_VALUE, Long.MAX_VALUE));
        }

        @Test
        public void testLongBinaryOperatorsMAX() {
            LongBinaryOperator maxOp = Fn.FL.LongBinaryOperators.MAX;
            Assertions.assertEquals(1000000L, maxOp.applyAsLong(500000L, 1000000L));
            Assertions.assertEquals(1000000L, maxOp.applyAsLong(1000000L, 500000L));
            Assertions.assertEquals(500000L, maxOp.applyAsLong(500000L, 500000L));
            Assertions.assertEquals(500000L, maxOp.applyAsLong(-1000000L, 500000L));

            Assertions.assertEquals(Long.MAX_VALUE, maxOp.applyAsLong(Long.MIN_VALUE, Long.MAX_VALUE));
        }
    }

    @Nested
    public class FnFFTest {

        @Test
        public void testPositive() {
            FloatPredicate predicate = Fn.FF.positive();
            Assertions.assertTrue(predicate.test(1.0f));
            Assertions.assertTrue(predicate.test(Float.MAX_VALUE));
            Assertions.assertTrue(predicate.test(0.1f));
            Assertions.assertFalse(predicate.test(0.0f));
            Assertions.assertFalse(predicate.test(-1.0f));
            Assertions.assertFalse(predicate.test(-Float.MAX_VALUE));
            Assertions.assertFalse(predicate.test(Float.NaN));
        }

        @Test
        public void testNotNegative() {
            FloatPredicate predicate = Fn.FF.notNegative();
            Assertions.assertTrue(predicate.test(0.0f));
            Assertions.assertTrue(predicate.test(1.0f));
            Assertions.assertTrue(predicate.test(Float.MAX_VALUE));
            Assertions.assertFalse(predicate.test(-1.0f));
            Assertions.assertFalse(predicate.test(-Float.MAX_VALUE));
            Assertions.assertFalse(predicate.test(Float.NaN));
        }

        @Test
        public void testEqual() {
            FloatBiPredicate predicate = Fn.FF.equal();
            Assertions.assertTrue(predicate.test(1.5f, 1.5f));
            Assertions.assertTrue(predicate.test(-10.25f, -10.25f));
            Assertions.assertTrue(predicate.test(0.0f, 0.0f));
            Assertions.assertTrue(predicate.test(Float.NaN, Float.NaN));
            Assertions.assertFalse(predicate.test(1.5f, 1.50001f));
            Assertions.assertFalse(predicate.test(-1.0f, 1.0f));
        }

        @Test
        public void testNotEqual() {
            FloatBiPredicate predicate = Fn.FF.notEqual();
            Assertions.assertTrue(predicate.test(1.5f, 1.50001f));
            Assertions.assertTrue(predicate.test(-1.0f, 1.0f));
            Assertions.assertFalse(predicate.test(1.5f, 1.5f));
            Assertions.assertFalse(predicate.test(0.0f, 0.0f));
            Assertions.assertFalse(predicate.test(Float.NaN, Float.NaN));
        }

        @Test
        public void testGreaterThan() {
            FloatBiPredicate predicate = Fn.FF.greaterThan();
            Assertions.assertTrue(predicate.test(10.5f, 5.5f));
            Assertions.assertTrue(predicate.test(0.0f, -1.0f));
            Assertions.assertFalse(predicate.test(5.5f, 5.5f));
            Assertions.assertFalse(predicate.test(5.5f, 10.5f));
            Assertions.assertTrue(predicate.test(Float.NaN, 1.0f));
            Assertions.assertFalse(predicate.test(1.0f, Float.NaN));
        }

        @Test
        public void testGreaterEqual() {
            FloatBiPredicate predicate = Fn.FF.greaterEqual();
            Assertions.assertTrue(predicate.test(10.5f, 5.5f));
            Assertions.assertTrue(predicate.test(5.5f, 5.5f));
            Assertions.assertFalse(predicate.test(5.5f, 10.5f));
        }

        @Test
        public void testLessThan() {
            FloatBiPredicate predicate = Fn.FF.lessThan();
            Assertions.assertTrue(predicate.test(5.5f, 10.5f));
            Assertions.assertTrue(predicate.test(-1.0f, 0.0f));
            Assertions.assertFalse(predicate.test(5.5f, 5.5f));
            Assertions.assertFalse(predicate.test(10.5f, 5.5f));
        }

        @Test
        public void testLessEqual() {
            FloatBiPredicate predicate = Fn.FF.lessEqual();
            Assertions.assertTrue(predicate.test(5.5f, 10.5f));
            Assertions.assertTrue(predicate.test(5.5f, 5.5f));
            Assertions.assertFalse(predicate.test(10.5f, 5.5f));
        }

        @Test
        public void testUnbox() {
            ToFloatFunction<Float> func = Fn.FF.unbox();
            Assertions.assertEquals(3.14f, func.applyAsFloat(Float.valueOf(3.14f)));
            Assertions.assertEquals(Float.MIN_VALUE, func.applyAsFloat(Float.valueOf(Float.MIN_VALUE)));
            Assertions.assertEquals(Float.MAX_VALUE, func.applyAsFloat(Float.valueOf(Float.MAX_VALUE)));
            Assertions.assertTrue(Float.isNaN(func.applyAsFloat(Float.valueOf(Float.NaN))));
        }

        @Test
        public void testP() {
            FloatPredicate original = f -> f > 0;
            FloatPredicate result = Fn.FF.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test(5.5f));
            Assertions.assertFalse(result.test(-5.5f));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FF.p(null));
        }

        @Test
        public void testF() {
            FloatFunction<String> original = f -> String.valueOf(f);
            FloatFunction<String> result = Fn.FF.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("10.5", result.apply(10.5f));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FF.f(null));
        }

        @Test
        public void testC() {
            final float[] sum = { 0.0f };
            FloatConsumer original = f -> sum[0] += f;
            FloatConsumer result = Fn.FF.c(original);
            Assertions.assertSame(original, result);

            result.accept(5.5f);
            Assertions.assertEquals(5.5f, sum[0], 0.0001f);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FF.c(null));
        }

        @Test
        public void testLen() {
            Function<float[], Integer> lenFunc = Fn.FF.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new float[0]));
            Assertions.assertEquals(3, lenFunc.apply(new float[] { 1.0f, 2.0f, 3.0f }));
            Assertions.assertEquals(5, lenFunc.apply(new float[] { -1.0f, -2.0f, -3.0f, -4.0f, -5.0f }));
        }

        @Test
        public void testSum() {
            Function<float[], Float> sumFunc = Fn.FF.sum();
            Assertions.assertEquals(0.0f, sumFunc.apply(new float[0]), 0.0001f);
            Assertions.assertEquals(6.0f, sumFunc.apply(new float[] { 1.0f, 2.0f, 3.0f }), 0.0001f);
            Assertions.assertEquals(0.0f, sumFunc.apply(new float[] { -5.5f, 5.5f }), 0.0001f);
            Assertions.assertEquals(15.0f, sumFunc.apply(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }), 0.0001f);
        }

        @Test
        public void testAverage() {
            Function<float[], Double> avgFunc = Fn.FF.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new float[0]));
            Assertions.assertEquals(2.0, avgFunc.apply(new float[] { 1.0f, 2.0f, 3.0f }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new float[] { -5.5f, 5.5f }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f }), 0.0001);
        }

        @Test
        public void testAlternate() {
            FloatBiFunction<MergeResult> func = Fn.FF.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(1.0f, 2.0f));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply(3.0f, 4.0f));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(5.0f, 6.0f));
        }

        @Test
        public void testFloatBinaryOperatorsMIN() {
            FloatBinaryOperator minOp = Fn.FF.FloatBinaryOperators.MIN;
            Assertions.assertEquals(5.5f, minOp.applyAsFloat(5.5f, 10.5f), 0.0001f);
            Assertions.assertEquals(5.5f, minOp.applyAsFloat(10.5f, 5.5f), 0.0001f);
            Assertions.assertEquals(5.5f, minOp.applyAsFloat(5.5f, 5.5f), 0.0001f);
            Assertions.assertEquals(-10.5f, minOp.applyAsFloat(-10.5f, 5.5f), 0.0001f);

            Assertions.assertEquals(-Float.MAX_VALUE, minOp.applyAsFloat(-Float.MAX_VALUE, Float.MAX_VALUE), 0.0001f);
            Assertions.assertTrue(Float.isNaN(minOp.applyAsFloat(Float.NaN, 1.0f)));
        }

        @Test
        public void testFloatBinaryOperatorsMAX() {
            FloatBinaryOperator maxOp = Fn.FF.FloatBinaryOperators.MAX;
            Assertions.assertEquals(10.5f, maxOp.applyAsFloat(5.5f, 10.5f), 0.0001f);
            Assertions.assertEquals(10.5f, maxOp.applyAsFloat(10.5f, 5.5f), 0.0001f);
            Assertions.assertEquals(5.5f, maxOp.applyAsFloat(5.5f, 5.5f), 0.0001f);
            Assertions.assertEquals(5.5f, maxOp.applyAsFloat(-10.5f, 5.5f), 0.0001f);

            Assertions.assertEquals(Float.MAX_VALUE, maxOp.applyAsFloat(-Float.MAX_VALUE, Float.MAX_VALUE), 0.0001f);
            Assertions.assertTrue(Float.isNaN(maxOp.applyAsFloat(Float.NaN, 1.0f)));
        }
    }

    @Nested
    public class FnFDTest {

        @Test
        public void testPositive() {
            DoublePredicate predicate = Fn.FD.positive();
            Assertions.assertTrue(predicate.test(1.0));
            Assertions.assertTrue(predicate.test(Double.MAX_VALUE));
            Assertions.assertTrue(predicate.test(0.1));
            Assertions.assertFalse(predicate.test(0.0));
            Assertions.assertFalse(predicate.test(-1.0));
            Assertions.assertFalse(predicate.test(-Double.MAX_VALUE));
            Assertions.assertFalse(predicate.test(Double.NaN));
        }

        @Test
        public void testNotNegative() {
            DoublePredicate predicate = Fn.FD.notNegative();
            Assertions.assertTrue(predicate.test(0.0));
            Assertions.assertTrue(predicate.test(1.0));
            Assertions.assertTrue(predicate.test(Double.MAX_VALUE));
            Assertions.assertFalse(predicate.test(-1.0));
            Assertions.assertFalse(predicate.test(-Double.MAX_VALUE));
            Assertions.assertFalse(predicate.test(Double.NaN));
        }

        @Test
        public void testEqual() {
            DoubleBiPredicate predicate = Fn.FD.equal();
            Assertions.assertTrue(predicate.test(1.5, 1.5));
            Assertions.assertTrue(predicate.test(-10.25, -10.25));
            Assertions.assertTrue(predicate.test(0.0, 0.0));
            Assertions.assertTrue(predicate.test(Double.NaN, Double.NaN));
            Assertions.assertFalse(predicate.test(1.5, 1.50001));
            Assertions.assertFalse(predicate.test(-1.0, 1.0));
        }

        @Test
        public void testNotEqual() {
            DoubleBiPredicate predicate = Fn.FD.notEqual();
            Assertions.assertTrue(predicate.test(1.5, 1.50001));
            Assertions.assertTrue(predicate.test(-1.0, 1.0));
            Assertions.assertFalse(predicate.test(1.5, 1.5));
            Assertions.assertFalse(predicate.test(0.0, 0.0));
            Assertions.assertFalse(predicate.test(Double.NaN, Double.NaN));
        }

        @Test
        public void testGreaterThan() {
            DoubleBiPredicate predicate = Fn.FD.greaterThan();
            Assertions.assertTrue(predicate.test(10.5, 5.5));
            Assertions.assertTrue(predicate.test(0.0, -1.0));
            Assertions.assertFalse(predicate.test(5.5, 5.5));
            Assertions.assertFalse(predicate.test(5.5, 10.5));
            Assertions.assertTrue(predicate.test(Double.NaN, 1.0));
            Assertions.assertFalse(predicate.test(1.0, Double.NaN));
        }

        @Test
        public void testGreaterEqual() {
            DoubleBiPredicate predicate = Fn.FD.greaterEqual();
            Assertions.assertTrue(predicate.test(10.5, 5.5));
            Assertions.assertTrue(predicate.test(5.5, 5.5));
            Assertions.assertFalse(predicate.test(5.5, 10.5));
        }

        @Test
        public void testLessThan() {
            DoubleBiPredicate predicate = Fn.FD.lessThan();
            Assertions.assertTrue(predicate.test(5.5, 10.5));
            Assertions.assertTrue(predicate.test(-1.0, 0.0));
            Assertions.assertFalse(predicate.test(5.5, 5.5));
            Assertions.assertFalse(predicate.test(10.5, 5.5));
        }

        @Test
        public void testLessEqual() {
            DoubleBiPredicate predicate = Fn.FD.lessEqual();
            Assertions.assertTrue(predicate.test(5.5, 10.5));
            Assertions.assertTrue(predicate.test(5.5, 5.5));
            Assertions.assertFalse(predicate.test(10.5, 5.5));
        }

        @Test
        public void testUnbox() {
            ToDoubleFunction<Double> func = Fn.FD.unbox();
            Assertions.assertEquals(3.14159, func.applyAsDouble(Double.valueOf(3.14159)));
            Assertions.assertEquals(Double.MIN_VALUE, func.applyAsDouble(Double.valueOf(Double.MIN_VALUE)));
            Assertions.assertEquals(Double.MAX_VALUE, func.applyAsDouble(Double.valueOf(Double.MAX_VALUE)));
            Assertions.assertTrue(Double.isNaN(func.applyAsDouble(Double.valueOf(Double.NaN))));
        }

        @Test
        public void testP() {
            DoublePredicate original = d -> d > 0;
            DoublePredicate result = Fn.FD.p(original);
            Assertions.assertSame(original, result);
            Assertions.assertTrue(result.test(5.5));
            Assertions.assertFalse(result.test(-5.5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FD.p(null));
        }

        @Test
        public void testF() {
            DoubleFunction<String> original = d -> String.valueOf(d);
            DoubleFunction<String> result = Fn.FD.f(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("10.5", result.apply(10.5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FD.f(null));
        }

        @Test
        public void testC() {
            final double[] sum = { 0.0 };
            DoubleConsumer original = d -> sum[0] += d;
            DoubleConsumer result = Fn.FD.c(original);
            Assertions.assertSame(original, result);

            result.accept(5.5);
            Assertions.assertEquals(5.5, sum[0], 0.0001);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fn.FD.c(null));
        }

        @Test
        public void testLen() {
            Function<double[], Integer> lenFunc = Fn.FD.len();
            Assertions.assertEquals(0, lenFunc.apply(null));
            Assertions.assertEquals(0, lenFunc.apply(new double[0]));
            Assertions.assertEquals(3, lenFunc.apply(new double[] { 1.0, 2.0, 3.0 }));
            Assertions.assertEquals(5, lenFunc.apply(new double[] { -1.0, -2.0, -3.0, -4.0, -5.0 }));
        }

        @Test
        public void testSum() {
            Function<double[], Double> sumFunc = Fn.FD.sum();
            Assertions.assertEquals(0.0, sumFunc.apply(new double[0]), 0.0001);
            Assertions.assertEquals(6.0, sumFunc.apply(new double[] { 1.0, 2.0, 3.0 }), 0.0001);
            Assertions.assertEquals(0.0, sumFunc.apply(new double[] { -5.5, 5.5 }), 0.0001);
            Assertions.assertEquals(15.0, sumFunc.apply(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }), 0.0001);
        }

        @Test
        public void testAverage() {
            Function<double[], Double> avgFunc = Fn.FD.average();
            Assertions.assertEquals(0.0, avgFunc.apply(new double[0]));
            Assertions.assertEquals(2.0, avgFunc.apply(new double[] { 1.0, 2.0, 3.0 }), 0.0001);
            Assertions.assertEquals(0.0, avgFunc.apply(new double[] { -5.5, 5.5 }), 0.0001);
            Assertions.assertEquals(3.0, avgFunc.apply(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }), 0.0001);
        }

        @Test
        public void testAlternate() {
            DoubleBiFunction<MergeResult> func = Fn.FD.alternate();

            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(1.0, 2.0));
            Assertions.assertEquals(MergeResult.TAKE_SECOND, func.apply(3.0, 4.0));
            Assertions.assertEquals(MergeResult.TAKE_FIRST, func.apply(5.0, 6.0));
        }

        @Test
        public void testDoubleBinaryOperatorsMIN() {
            DoubleBinaryOperator minOp = Fn.FD.DoubleBinaryOperators.MIN;
            Assertions.assertEquals(5.5, minOp.applyAsDouble(5.5, 10.5), 0.0001);
            Assertions.assertEquals(5.5, minOp.applyAsDouble(10.5, 5.5), 0.0001);
            Assertions.assertEquals(5.5, minOp.applyAsDouble(5.5, 5.5), 0.0001);
            Assertions.assertEquals(-10.5, minOp.applyAsDouble(-10.5, 5.5), 0.0001);

            Assertions.assertEquals(-Double.MAX_VALUE, minOp.applyAsDouble(-Double.MAX_VALUE, Double.MAX_VALUE), 0.0001);
            Assertions.assertTrue(Double.isNaN(minOp.applyAsDouble(Double.NaN, 1.0)));
        }

        @Test
        public void testDoubleBinaryOperatorsMAX() {
            DoubleBinaryOperator maxOp = Fn.FD.DoubleBinaryOperators.MAX;
            Assertions.assertEquals(10.5, maxOp.applyAsDouble(5.5, 10.5), 0.0001);
            Assertions.assertEquals(10.5, maxOp.applyAsDouble(10.5, 5.5), 0.0001);
            Assertions.assertEquals(5.5, maxOp.applyAsDouble(5.5, 5.5), 0.0001);
            Assertions.assertEquals(5.5, maxOp.applyAsDouble(-10.5, 5.5), 0.0001);

            Assertions.assertEquals(Double.MAX_VALUE, maxOp.applyAsDouble(-Double.MAX_VALUE, Double.MAX_VALUE), 0.0001);
            Assertions.assertTrue(Double.isNaN(maxOp.applyAsDouble(Double.NaN, 1.0)));
        }
    }

    @Nested
    public class FnFnnTest {

        @Test
        public void testMemoize() {
            final AtomicInteger counter = new AtomicInteger(0);
            Throwables.Supplier<String, RuntimeException> supplier = () -> {
                counter.incrementAndGet();
                return "result";
            };

            Throwables.Supplier<String, RuntimeException> memoized = Fnn.memoize(supplier);

            Assertions.assertEquals("result", memoized.get());
            Assertions.assertEquals(1, counter.get());

            Assertions.assertEquals("result", memoized.get());
            Assertions.assertEquals("result", memoized.get());
            Assertions.assertEquals(1, counter.get());
        }

        @Test
        public void testMemoizeWithExpiration() throws Exception {
            final AtomicInteger counter = new AtomicInteger(0);
            Throwables.Supplier<String, RuntimeException> supplier = () -> {
                counter.incrementAndGet();
                return "result" + counter.get();
            };

            Throwables.Supplier<String, RuntimeException> memoized = Fnn.memoizeWithExpiration(supplier, 100, TimeUnit.MILLISECONDS);

            Assertions.assertEquals("result1", memoized.get());
            Assertions.assertEquals(1, counter.get());

            Assertions.assertEquals("result1", memoized.get());
            Assertions.assertEquals(1, counter.get());

            Thread.sleep(150);

            Assertions.assertEquals("result2", memoized.get());
            Assertions.assertEquals(2, counter.get());

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration(supplier, -1, TimeUnit.SECONDS));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.memoizeWithExpiration(null, 1, TimeUnit.SECONDS));
        }

        @Test
        public void testMemoizeFunction() {
            final AtomicInteger counter = new AtomicInteger(0);
            Throwables.Function<Integer, String, RuntimeException> func = i -> {
                counter.incrementAndGet();
                return "result" + i;
            };

            Throwables.Function<Integer, String, RuntimeException> memoized = Fnn.memoize(func);

            Assertions.assertEquals("result1", memoized.apply(1));
            Assertions.assertEquals("result2", memoized.apply(2));
            Assertions.assertEquals(2, counter.get());

            Assertions.assertEquals("result1", memoized.apply(1));
            Assertions.assertEquals("result2", memoized.apply(2));
            Assertions.assertEquals(2, counter.get());

            Assertions.assertEquals("resultnull", memoized.apply(null));
            Assertions.assertEquals(3, counter.get());
            Assertions.assertEquals("resultnull", memoized.apply(null));
            Assertions.assertEquals(3, counter.get());
        }

        @Test
        public void testIdentity() {
            Throwables.Function<Object, Object, RuntimeException> identity = Fnn.identity();
            Assertions.assertEquals("test", identity.apply("test"));
            Assertions.assertNull(identity.apply(null));
            Assertions.assertEquals(123, identity.apply(123));
        }

        @Test
        public void testAlwaysTrue() {
            Throwables.Predicate<Object, RuntimeException> predicate = Fnn.alwaysTrue();
            Assertions.assertTrue(predicate.test("anything"));
            Assertions.assertTrue(predicate.test(null));
            Assertions.assertTrue(predicate.test(123));
        }

        @Test
        public void testAlwaysFalse() {
            Throwables.Predicate<Object, RuntimeException> predicate = Fnn.alwaysFalse();
            Assertions.assertFalse(predicate.test("anything"));
            Assertions.assertFalse(predicate.test(null));
            Assertions.assertFalse(predicate.test(123));
        }

        @Test
        public void testToStr() {
            Throwables.Function<Object, String, RuntimeException> toStr = Fnn.toStr();
            Assertions.assertEquals("test", toStr.apply("test"));
            Assertions.assertEquals("123", toStr.apply(123));
            Assertions.assertEquals("null", toStr.apply(null));
        }

        @Test
        public void testKey() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
            Throwables.Function<Map.Entry<String, Integer>, String, RuntimeException> keyFunc = Fnn.key();
            Assertions.assertEquals("key", keyFunc.apply(entry));
        }

        @Test
        public void testValue() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
            Throwables.Function<Map.Entry<String, Integer>, Integer, RuntimeException> valueFunc = Fnn.value();
            Assertions.assertEquals(100, valueFunc.apply(entry));
        }

        @Test
        public void testInverse() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
            Throwables.Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>, RuntimeException> inverseFunc = Fnn.invert();
            Map.Entry<Integer, String> inverted = inverseFunc.apply(entry);
            Assertions.assertEquals(100, inverted.getKey());
            Assertions.assertEquals("key", inverted.getValue());
        }

        @Test
        public void testEntry() {
            Throwables.BiFunction<String, Integer, Map.Entry<String, Integer>, RuntimeException> entryFunc = Fnn.entry();
            Map.Entry<String, Integer> entry = entryFunc.apply("key", 100);
            Assertions.assertEquals("key", entry.getKey());
            Assertions.assertEquals(100, entry.getValue());
        }

        @Test
        public void testPair() {
            Throwables.BiFunction<String, Integer, Pair<String, Integer>, RuntimeException> pairFunc = Fnn.pair();
            Pair<String, Integer> pair = pairFunc.apply("left", 100);
            Assertions.assertEquals("left", pair.left());
            Assertions.assertEquals(100, pair.right());
        }

        @Test
        public void testTriple() {
            Throwables.TriFunction<String, Integer, Double, Triple<String, Integer, Double>, RuntimeException> tripleFunc = Fnn.triple();
            Triple<String, Integer, Double> triple = tripleFunc.apply("left", 100, 3.14);
            Assertions.assertEquals("left", triple.left());
            Assertions.assertEquals(100, triple.middle());
            Assertions.assertEquals(3.14, triple.right());
        }

        @Test
        public void testTuple1() {
            Throwables.Function<String, Tuple1<String>, RuntimeException> tuple1Func = Fnn.tuple1();
            Tuple1<String> tuple = tuple1Func.apply("value");
            Assertions.assertEquals("value", tuple._1);
        }

        @Test
        public void testTuple2() {
            Throwables.BiFunction<String, Integer, Tuple2<String, Integer>, RuntimeException> tuple2Func = Fnn.tuple2();
            Tuple2<String, Integer> tuple = tuple2Func.apply("first", 100);
            Assertions.assertEquals("first", tuple._1);
            Assertions.assertEquals(100, tuple._2);
        }

        @Test
        public void testTuple3() {
            Throwables.TriFunction<String, Integer, Double, Tuple3<String, Integer, Double>, RuntimeException> tuple3Func = Fnn.tuple3();
            Tuple3<String, Integer, Double> tuple = tuple3Func.apply("first", 100, 3.14);
            Assertions.assertEquals("first", tuple._1);
            Assertions.assertEquals(100, tuple._2);
            Assertions.assertEquals(3.14, tuple._3);
        }

        @Test
        public void testEmptyAction() {
            Throwables.Runnable<Exception> emptyAction = Fnn.emptyAction();
            Assertions.assertDoesNotThrow(() -> emptyAction.run());
        }

        @Test
        public void testDoNothing() {
            Throwables.Consumer<String, RuntimeException> doNothing = Fnn.doNothing();
            Assertions.assertDoesNotThrow(() -> doNothing.accept("test"));
            Assertions.assertDoesNotThrow(() -> doNothing.accept(null));
        }

        @Test
        public void testThrowRuntimeException() {
            Throwables.Consumer<String, RuntimeException> thrower = Fnn.throwRuntimeException("Error message");
            RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> thrower.accept("test"));
            Assertions.assertEquals("Error message", ex.getMessage());
        }

        @Test
        public void testThrowIOException() {
            Throwables.Consumer<String, IOException> thrower = Fnn.throwIOException("IO Error");
            IOException ex = Assertions.assertThrows(IOException.class, () -> thrower.accept("test"));
            Assertions.assertEquals("IO Error", ex.getMessage());
        }

        @Test
        public void testThrowException() {
            Throwables.Consumer<String, Exception> thrower = Fnn.throwException("General Error");
            Exception ex = Assertions.assertThrows(Exception.class, () -> thrower.accept("test"));
            Assertions.assertEquals("General Error", ex.getMessage());
        }

        @Test
        public void testThrowExceptionWithSupplier() {
            Throwables.Consumer<String, IllegalStateException> thrower = Fnn.throwException(() -> new IllegalStateException("Supplied error"));
            IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> thrower.accept("test"));
            Assertions.assertEquals("Supplied error", ex.getMessage());
        }

        @Test
        public void testSleep() throws Exception {
            Throwables.Consumer<String, RuntimeException> sleeper = Fnn.sleep(50);
            long start = System.currentTimeMillis();
            sleeper.accept("test");
            long duration = System.currentTimeMillis() - start;
            Assertions.assertTrue(duration >= 40);
        }

        @Test
        public void testSleepUninterruptibly() throws Exception {
            Throwables.Consumer<String, RuntimeException> sleeper = Fnn.sleepUninterruptibly(50);
            long start = System.currentTimeMillis();
            sleeper.accept("test");
            long duration = System.currentTimeMillis() - start;
            Assertions.assertTrue(duration >= 40);
        }

        @Test
        public void testRateLimiterWithPermitsPerSecond() throws Exception {
            Throwables.Consumer<String, RuntimeException> rateLimited = Fnn.rateLimiter(10.0);
            long start = System.currentTimeMillis();

            for (int i = 0; i < 3; i++) {
                rateLimited.accept("test");
            }

            long duration = System.currentTimeMillis() - start;
            Assertions.assertTrue(duration >= 180);
        }

        @Test
        public void testClose() throws Exception {
            final boolean[] closed = { false };
            AutoCloseable closeable = () -> closed[0] = true;

            Throwables.Consumer<AutoCloseable, Exception> closer = Fnn.close();
            closer.accept(closeable);
            Assertions.assertTrue(closed[0]);

            Assertions.assertDoesNotThrow(() -> closer.accept(null));
        }

        @Test
        public void testCloseQuietly() {
            final boolean[] closed = { false };
            AutoCloseable closeable = () -> closed[0] = true;

            Throwables.Consumer<AutoCloseable, RuntimeException> closer = Fnn.closeQuietly();
            closer.accept(closeable);
            Assertions.assertTrue(closed[0]);

            AutoCloseable throwingCloseable = () -> {
                throw new IOException("Close failed");
            };
            Assertions.assertDoesNotThrow(() -> closer.accept(throwingCloseable));
        }

        @Test
        public void testPrintln() {
            Throwables.Consumer<String, RuntimeException> printer = Fnn.println();
            Assertions.assertDoesNotThrow(() -> printer.accept("test"));
        }

        @Test
        public void testPrintlnWithSeparator() {
            Throwables.BiConsumer<String, Integer, RuntimeException> printer = Fnn.println(" - ");
            Assertions.assertDoesNotThrow(() -> printer.accept("test", 123));
        }

        @Test
        public void testIsNull() {
            Throwables.Predicate<Object, RuntimeException> isNull = Fnn.isNull();
            Assertions.assertTrue(isNull.test(null));
            Assertions.assertFalse(isNull.test("not null"));
            Assertions.assertFalse(isNull.test(123));
        }

        @Test
        public void testIsEmpty() {
            Throwables.Predicate<CharSequence, RuntimeException> isEmpty = Fnn.isEmpty();
            Assertions.assertTrue(isEmpty.test(""));
            Assertions.assertFalse(isEmpty.test("not empty"));
            Assertions.assertFalse(isEmpty.test(" "));
        }

        @Test
        public void testIsBlank() {
            Throwables.Predicate<CharSequence, RuntimeException> isBlank = Fnn.isBlank();
            Assertions.assertTrue(isBlank.test(""));
            Assertions.assertTrue(isBlank.test(" "));
            Assertions.assertTrue(isBlank.test("\t\n\r"));
            Assertions.assertFalse(isBlank.test("not blank"));
            Assertions.assertFalse(isBlank.test(" a "));
        }

        @Test
        public void testIsEmptyArray() {
            Throwables.Predicate<String[], RuntimeException> isEmptyArray = Fnn.isEmptyArray();
            Assertions.assertTrue(isEmptyArray.test(null));
            Assertions.assertTrue(isEmptyArray.test(new String[0]));
            Assertions.assertFalse(isEmptyArray.test(new String[] { "a" }));
        }

        @Test
        public void testIsEmptyCollection() {
            Throwables.Predicate<java.util.List<String>, RuntimeException> isEmptyCollection = Fnn.isEmptyCollection();
            Assertions.assertTrue(isEmptyCollection.test(null));
            Assertions.assertTrue(isEmptyCollection.test(new java.util.ArrayList<>()));
            Assertions.assertFalse(isEmptyCollection.test(java.util.Arrays.asList("a")));
        }

        @Test
        public void testIsEmptyMap() {
            Throwables.Predicate<Map<String, Integer>, RuntimeException> isEmptyMap = Fnn.isEmptyMap();
            Assertions.assertTrue(isEmptyMap.test(null));
            Assertions.assertTrue(isEmptyMap.test(new HashMap<>()));
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            Assertions.assertFalse(isEmptyMap.test(map));
        }

        @Test
        public void testNotNull() {
            Throwables.Predicate<Object, RuntimeException> notNull = Fnn.notNull();
            Assertions.assertFalse(notNull.test(null));
            Assertions.assertTrue(notNull.test("not null"));
            Assertions.assertTrue(notNull.test(123));
        }

        @Test
        public void testNotEmpty() {
            Throwables.Predicate<CharSequence, RuntimeException> notEmpty = Fnn.notEmpty();
            Assertions.assertFalse(notEmpty.test(""));
            Assertions.assertTrue(notEmpty.test("not empty"));
            Assertions.assertTrue(notEmpty.test(" "));
        }

        @Test
        public void testNotBlank() {
            Throwables.Predicate<CharSequence, RuntimeException> notBlank = Fnn.notBlank();
            Assertions.assertFalse(notBlank.test(""));
            Assertions.assertFalse(notBlank.test(" "));
            Assertions.assertFalse(notBlank.test("\t\n\r"));
            Assertions.assertTrue(notBlank.test("not blank"));
            Assertions.assertTrue(notBlank.test(" a "));
        }

        @Test
        public void testNotEmptyA() {
            Throwables.Predicate<String[], RuntimeException> notEmptyA = Fnn.notEmptyA();
            Assertions.assertFalse(notEmptyA.test(null));
            Assertions.assertFalse(notEmptyA.test(new String[0]));
            Assertions.assertTrue(notEmptyA.test(new String[] { "a" }));
        }

        @Test
        public void testNotEmptyC() {
            Throwables.Predicate<java.util.List<String>, RuntimeException> notEmptyC = Fnn.notEmptyC();
            Assertions.assertFalse(notEmptyC.test(null));
            Assertions.assertFalse(notEmptyC.test(new java.util.ArrayList<>()));
            Assertions.assertTrue(notEmptyC.test(java.util.Arrays.asList("a")));
        }

        @Test
        public void testNotEmptyM() {
            Throwables.Predicate<Map<String, Integer>, RuntimeException> notEmptyM = Fnn.notEmptyM();
            Assertions.assertFalse(notEmptyM.test(null));
            Assertions.assertFalse(notEmptyM.test(new HashMap<>()));
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            Assertions.assertTrue(notEmptyM.test(map));
        }

        @Test
        public void testThrowingMerger() {
            Throwables.BinaryOperator<String, RuntimeException> merger = Fnn.throwingMerger();
            Assertions.assertThrows(IllegalStateException.class, () -> merger.apply("a", "b"));
        }

        @Test
        public void testIgnoringMerger() {
            Throwables.BinaryOperator<String, RuntimeException> merger = Fnn.ignoringMerger();
            Assertions.assertEquals("first", merger.apply("first", "second"));
        }

        @Test
        public void testReplacingMerger() {
            Throwables.BinaryOperator<String, RuntimeException> merger = Fnn.replacingMerger();
            Assertions.assertEquals("second", merger.apply("first", "second"));
        }

        @Test
        public void testTestByKey() {
            Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("key1", 100);
            Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("key2", 200);

            Throwables.Predicate<Map.Entry<String, Integer>, RuntimeException> predicate = Fnn.testByKey(k -> k.equals("key1"));

            Assertions.assertTrue(predicate.test(entry1));
            Assertions.assertFalse(predicate.test(entry2));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.testByKey(null));
        }

        @Test
        public void testTestByValue() {
            Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("key1", 100);
            Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("key2", 200);

            Throwables.Predicate<Map.Entry<String, Integer>, RuntimeException> predicate = Fnn.testByValue(v -> v > 150);

            Assertions.assertFalse(predicate.test(entry1));
            Assertions.assertTrue(predicate.test(entry2));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.testByValue(null));
        }

        @Test
        public void testAcceptByKey() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
            final String[] result = { "" };

            Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException> consumer = Fnn.acceptByKey(k -> result[0] = k);

            consumer.accept(entry);
            Assertions.assertEquals("key", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByKey(null));
        }

        @Test
        public void testAcceptByValue() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);
            final int[] result = { 0 };

            Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException> consumer = Fnn.acceptByValue(v -> result[0] = v);

            consumer.accept(entry);
            Assertions.assertEquals(100, result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.acceptByValue(null));
        }

        @Test
        public void testApplyByKey() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);

            Throwables.Function<Map.Entry<String, Integer>, String, RuntimeException> func = Fnn.applyByKey(k -> k.toUpperCase());

            Assertions.assertEquals("KEY", func.apply(entry));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.applyByKey(null));
        }

        @Test
        public void testApplyByValue() {
            Map.Entry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 100);

            Throwables.Function<Map.Entry<String, Integer>, Integer, RuntimeException> func = Fnn.applyByValue(v -> v * 2);

            Assertions.assertEquals(200, func.apply(entry));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.applyByValue(null));
        }

        @Test
        public void testSelectFirst() {
            Throwables.BinaryOperator<String, RuntimeException> selector = Fnn.selectFirst();
            Assertions.assertEquals("first", selector.apply("first", "second"));
            Assertions.assertEquals(null, selector.apply(null, "second"));
        }

        @Test
        public void testSelectSecond() {
            Throwables.BinaryOperator<String, RuntimeException> selector = Fnn.selectSecond();
            Assertions.assertEquals("second", selector.apply("first", "second"));
            Assertions.assertEquals(null, selector.apply("first", null));
        }

        @Test
        public void testMin() {
            Throwables.BinaryOperator<Integer, RuntimeException> minOp = Fnn.min();
            Assertions.assertEquals(5, minOp.apply(5, 10));
            Assertions.assertEquals(5, minOp.apply(10, 5));
            Assertions.assertEquals(5, minOp.apply(5, 5));
        }

        @Test
        public void testMinWithComparator() {
            Throwables.BinaryOperator<String, RuntimeException> minOp = Fnn.min(String.CASE_INSENSITIVE_ORDER);
            Assertions.assertEquals("apple", minOp.apply("apple", "Banana"));
            Assertions.assertEquals("apple", minOp.apply("Banana", "apple"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.min(null));
        }

        @Test
        public void testMinBy() {
            Throwables.BinaryOperator<String, RuntimeException> minOp = Fnn.minBy(String::length);
            Assertions.assertEquals("cat", minOp.apply("cat", "elephant"));
            Assertions.assertEquals("cat", minOp.apply("elephant", "cat"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.minBy(null));
        }

        @Test
        public void testMinByKey() {
            Map.Entry<Integer, String> entry1 = CommonUtil.newImmutableEntry(10, "ten");
            Map.Entry<Integer, String> entry2 = CommonUtil.newImmutableEntry(5, "five");

            Throwables.BinaryOperator<Map.Entry<Integer, String>, RuntimeException> minOp = Fnn.minByKey();
            Assertions.assertEquals(entry2, minOp.apply(entry1, entry2));
            Assertions.assertEquals(entry2, minOp.apply(entry2, entry1));
        }

        @Test
        public void testMinByValue() {
            Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("ten", 10);
            Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("five", 5);

            Throwables.BinaryOperator<Map.Entry<String, Integer>, RuntimeException> minOp = Fnn.minByValue();
            Assertions.assertEquals(entry2, minOp.apply(entry1, entry2));
            Assertions.assertEquals(entry2, minOp.apply(entry2, entry1));
        }

        @Test
        public void testMax() {
            Throwables.BinaryOperator<Integer, RuntimeException> maxOp = Fnn.max();
            Assertions.assertEquals(10, maxOp.apply(5, 10));
            Assertions.assertEquals(10, maxOp.apply(10, 5));
            Assertions.assertEquals(5, maxOp.apply(5, 5));
        }

        @Test
        public void testMaxWithComparator() {
            Throwables.BinaryOperator<String, RuntimeException> maxOp = Fnn.max(String.CASE_INSENSITIVE_ORDER);
            Assertions.assertEquals("Banana", maxOp.apply("apple", "Banana"));
            Assertions.assertEquals("Banana", maxOp.apply("Banana", "apple"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.max(null));
        }

        @Test
        public void testMaxBy() {
            Throwables.BinaryOperator<String, RuntimeException> maxOp = Fnn.maxBy(String::length);
            Assertions.assertEquals("elephant", maxOp.apply("cat", "elephant"));
            Assertions.assertEquals("elephant", maxOp.apply("elephant", "cat"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.maxBy(null));
        }

        @Test
        public void testMaxByKey() {
            Map.Entry<Integer, String> entry1 = CommonUtil.newImmutableEntry(10, "ten");
            Map.Entry<Integer, String> entry2 = CommonUtil.newImmutableEntry(5, "five");

            Throwables.BinaryOperator<Map.Entry<Integer, String>, RuntimeException> maxOp = Fnn.maxByKey();
            Assertions.assertEquals(entry1, maxOp.apply(entry1, entry2));
            Assertions.assertEquals(entry1, maxOp.apply(entry2, entry1));
        }

        @Test
        public void testMaxByValue() {
            Map.Entry<String, Integer> entry1 = CommonUtil.newImmutableEntry("ten", 10);
            Map.Entry<String, Integer> entry2 = CommonUtil.newImmutableEntry("five", 5);

            Throwables.BinaryOperator<Map.Entry<String, Integer>, RuntimeException> maxOp = Fnn.maxByValue();
            Assertions.assertEquals(entry1, maxOp.apply(entry1, entry2));
            Assertions.assertEquals(entry1, maxOp.apply(entry2, entry1));
        }

        @Test
        public void testNot() {
            Throwables.Predicate<Integer, RuntimeException> isPositive = i -> i > 0;
            Throwables.Predicate<Integer, RuntimeException> notPositive = Fnn.not(isPositive);

            Assertions.assertTrue(notPositive.test(-5));
            Assertions.assertTrue(notPositive.test(0));
            Assertions.assertFalse(notPositive.test(5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.Predicate<Integer, RuntimeException>) null));
        }

        @Test
        public void testNotBiPredicate() {
            Throwables.BiPredicate<Integer, Integer, RuntimeException> isGreater = (a, b) -> a > b;
            Throwables.BiPredicate<Integer, Integer, RuntimeException> notGreater = Fnn.not(isGreater);

            Assertions.assertTrue(notGreater.test(5, 10));
            Assertions.assertTrue(notGreater.test(5, 5));
            Assertions.assertFalse(notGreater.test(10, 5));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.BiPredicate<Integer, Integer, RuntimeException>) null));
        }

        @Test
        public void testNotTriPredicate() {
            Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException> allPositive = (a, b, c) -> a > 0 && b > 0 && c > 0;
            Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException> notAllPositive = Fnn.not(allPositive);

            Assertions.assertTrue(notAllPositive.test(-1, 2, 3));
            Assertions.assertTrue(notAllPositive.test(1, -2, 3));
            Assertions.assertTrue(notAllPositive.test(1, 2, -3));
            Assertions.assertFalse(notAllPositive.test(1, 2, 3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.not((Throwables.TriPredicate<Integer, Integer, Integer, RuntimeException>) null));
        }

        @Test
        public void testAtMost() {
            Throwables.Predicate<String, RuntimeException> atMost3 = Fnn.atMost(3);

            Assertions.assertTrue(atMost3.test("first"));
            Assertions.assertTrue(atMost3.test("second"));
            Assertions.assertTrue(atMost3.test("third"));
            Assertions.assertFalse(atMost3.test("fourth"));
            Assertions.assertFalse(atMost3.test("fifth"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.atMost(-1));
        }

        @Test
        public void testFromSupplier() {
            java.util.function.Supplier<String> javaSupplier = () -> "result";
            Throwables.Supplier<String, RuntimeException> throwableSupplier = Fnn.from(javaSupplier);
            Assertions.assertEquals("result", throwableSupplier.get());

        }

        @Test
        public void testFromIntFunction() {
            java.util.function.IntFunction<String> javaFunc = i -> "value" + i;
            Throwables.IntFunction<String, RuntimeException> throwableFunc = Fnn.from(javaFunc);
            Assertions.assertEquals("value5", throwableFunc.apply(5));
        }

        @Test
        public void testFromPredicate() {
            java.util.function.Predicate<String> javaPred = s -> s.length() > 3;
            Throwables.Predicate<String, RuntimeException> throwablePred = Fnn.from(javaPred);
            Assertions.assertTrue(throwablePred.test("hello"));
            Assertions.assertFalse(throwablePred.test("hi"));
        }

        @Test
        public void testFromBiPredicate() {
            java.util.function.BiPredicate<String, Integer> javaPred = (s, i) -> s.length() == i;
            Throwables.BiPredicate<String, Integer, RuntimeException> throwablePred = Fnn.from(javaPred);
            Assertions.assertTrue(throwablePred.test("hello", 5));
            Assertions.assertFalse(throwablePred.test("hello", 3));
        }

        @Test
        public void testFromConsumer() {
            final String[] result = { "" };
            java.util.function.Consumer<String> javaConsumer = s -> result[0] = s;
            Throwables.Consumer<String, RuntimeException> throwableConsumer = Fnn.from(javaConsumer);
            throwableConsumer.accept("test");
            Assertions.assertEquals("test", result[0]);
        }

        @Test
        public void testFromBiConsumer() {
            final String[] result = { "" };
            java.util.function.BiConsumer<String, Integer> javaBiConsumer = (s, i) -> result[0] = s + i;
            Throwables.BiConsumer<String, Integer, RuntimeException> throwableBiConsumer = Fnn.from(javaBiConsumer);
            throwableBiConsumer.accept("test", 123);
            Assertions.assertEquals("test123", result[0]);
        }

        @Test
        public void testFromFunction() {
            java.util.function.Function<String, Integer> javaFunc = String::length;
            Throwables.Function<String, Integer, RuntimeException> throwableFunc = Fnn.from(javaFunc);
            Assertions.assertEquals(5, throwableFunc.apply("hello"));
        }

        @Test
        public void testFromBiFunction() {
            java.util.function.BiFunction<String, Integer, String> javaBiFunc = (s, i) -> s + i;
            Throwables.BiFunction<String, Integer, String, RuntimeException> throwableBiFunc = Fnn.from(javaBiFunc);
            Assertions.assertEquals("test123", throwableBiFunc.apply("test", 123));
        }

        @Test
        public void testFromUnaryOperator() {
            java.util.function.UnaryOperator<String> javaOp = s -> s.toUpperCase();
            Throwables.UnaryOperator<String, RuntimeException> throwableOp = Fnn.from(javaOp);
            Assertions.assertEquals("HELLO", throwableOp.apply("hello"));
        }

        @Test
        public void testFromBinaryOperator() {
            java.util.function.BinaryOperator<String> javaOp = (a, b) -> a + b;
            Throwables.BinaryOperator<String, RuntimeException> throwableOp = Fnn.from(javaOp);
            Assertions.assertEquals("helloworld", throwableOp.apply("hello", "world"));
        }

        @Test
        public void testSSupplier() {
            Throwables.Supplier<String, RuntimeException> supplier = () -> "test";
            Assertions.assertSame(supplier, Fnn.s(supplier));
        }

        @Test
        public void testSWithPartialApplication() {
            Throwables.Function<String, String, RuntimeException> func = s -> s.toUpperCase();
            Throwables.Supplier<String, RuntimeException> supplier = Fnn.s("hello", func);
            Assertions.assertEquals("HELLO", supplier.get());
        }

        @Test
        public void testPPredicate() {
            Throwables.Predicate<String, RuntimeException> pred = s -> s.length() > 3;
            Assertions.assertSame(pred, Fnn.p(pred));
        }

        @Test
        public void testPWithBiPredicate() {
            Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
            Throwables.Predicate<Integer, RuntimeException> pred = Fnn.p("hello", biPred);
            Assertions.assertTrue(pred.test(5));
            Assertions.assertFalse(pred.test(3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.p("hello", (Throwables.BiPredicate) null));
        }

        @Test
        public void testPWithTriPredicate() {
            Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
            Throwables.Predicate<Boolean, RuntimeException> pred = Fnn.p("hello", 5, triPred);
            Assertions.assertTrue(pred.test(true));
            Assertions.assertFalse(pred.test(false));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.p("hello", 5, null));
        }

        @Test
        public void testPBiPredicate() {
            Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
            Assertions.assertSame(biPred, Fnn.p(biPred));
        }

        @Test
        public void testPBiPredicateWithPartial() {
            Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
            Throwables.BiPredicate<Integer, Boolean, RuntimeException> biPred = Fnn.p("hello", triPred);
            Assertions.assertTrue(biPred.test(5, true));
            Assertions.assertFalse(biPred.test(5, false));
            Assertions.assertFalse(biPred.test(3, true));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.p("hello", (Throwables.TriPredicate<String, Integer, Boolean, RuntimeException>) null));
        }

        @Test
        public void testPTriPredicate() {
            Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> triPred = (s, i, b) -> s.length() == i && b;
            Assertions.assertSame(triPred, Fnn.p(triPred));
        }

        @Test
        public void testCConsumer() {
            Throwables.Consumer<String, RuntimeException> consumer = s -> {
            };
            Assertions.assertSame(consumer, Fnn.c(consumer));
        }

        @Test
        public void testCWithBiConsumer() {
            final String[] result = { "" };
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.Consumer<Integer, RuntimeException> consumer = Fnn.c("test", biConsumer);
            consumer.accept(123);
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c("test", (Throwables.BiConsumer<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testCWithTriConsumer() {
            final String[] result = { "" };
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.Consumer<Boolean, RuntimeException> consumer = Fnn.c("test", 123, triConsumer);
            consumer.accept(true);
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c("test", 123, null));
        }

        @Test
        public void testCBiConsumer() {
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> {
            };
            Assertions.assertSame(biConsumer, Fnn.c(biConsumer));
        }

        @Test
        public void testCBiConsumerWithPartial() {
            final String[] result = { "" };
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.BiConsumer<Integer, Boolean, RuntimeException> biConsumer = Fnn.c("test", triConsumer);
            biConsumer.accept(123, true);
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.c("test", (Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null));
        }

        @Test
        public void testCTriConsumer() {
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> {
            };
            Assertions.assertSame(triConsumer, Fnn.c(triConsumer));
        }

        @Test
        public void testFFunction() {
            Throwables.Function<String, Integer, RuntimeException> func = String::length;
            Assertions.assertSame(func, Fnn.f(func));
        }

        @Test
        public void testFWithBiFunction() {
            Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
            Throwables.Function<Integer, String, RuntimeException> func = Fnn.f("test", biFunc);
            Assertions.assertEquals("test123", func.apply(123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f("test", (Throwables.BiFunction) null));
        }

        @Test
        public void testFWithTriFunction() {
            Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
            Throwables.Function<Boolean, String, RuntimeException> func = Fnn.f("test", 123, triFunc);
            Assertions.assertEquals("test123true", func.apply(true));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f("test", 123, null));
        }

        @Test
        public void testFBiFunction() {
            Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
            Assertions.assertSame(biFunc, Fnn.f(biFunc));
        }

        @Test
        public void testFBiFunctionWithPartial() {
            Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
            Throwables.BiFunction<Integer, Boolean, String, RuntimeException> biFunc = Fnn.f("test", triFunc);
            Assertions.assertEquals("test123true", biFunc.apply(123, true));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.f("test", (Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException>) null));
        }

        @Test
        public void testFTriFunction() {
            Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
            Assertions.assertSame(triFunc, Fnn.f(triFunc));
        }

        @Test
        public void testOUnaryOperator() {
            Throwables.UnaryOperator<String, RuntimeException> op = s -> s.toUpperCase();
            Assertions.assertSame(op, Fnn.o(op));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.UnaryOperator<String, RuntimeException>) null));
        }

        @Test
        public void testOBinaryOperator() {
            Throwables.BinaryOperator<String, RuntimeException> op = (a, b) -> a + b;
            Assertions.assertSame(op, Fnn.o(op));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.o((Throwables.BinaryOperator<String, RuntimeException>) null));
        }

        @Test
        public void testMc() {
            Throwables.BiConsumer<String, java.util.function.Consumer<Character>, RuntimeException> mapper = (s, consumer) -> {
                for (char c : s.toCharArray()) {
                    consumer.accept(c);
                }
            };

            Throwables.BiConsumer<String, java.util.function.Consumer<Character>, RuntimeException> result = Fnn.mc(mapper);
            Assertions.assertSame(mapper, result);

            java.util.List<Character> chars = new java.util.ArrayList<>();
            result.accept("test", chars::add);
            Assertions.assertEquals(java.util.Arrays.asList('t', 'e', 's', 't'), chars);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.mc(null));
        }

        @Test
        public void testPpPredicate() {
            Predicate<String> pred = s -> s.length() > 3;
            Throwables.Predicate<String, RuntimeException> result = Fnn.pp(pred);
            Assertions.assertSame(pred, result);
            Assertions.assertTrue(result.test("hello"));
            Assertions.assertFalse(result.test("hi"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((Predicate<String>) null));
        }

        @Test
        public void testPpWithBiPredicate() {
            java.util.function.BiPredicate<String, Integer> biPred = (s, i) -> s.length() == i;
            Throwables.Predicate<Integer, RuntimeException> pred = Fnn.pp("hello", biPred);
            Assertions.assertTrue(pred.test(5));
            Assertions.assertFalse(pred.test(3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", (java.util.function.BiPredicate<String, Integer>) null));
        }

        @Test
        public void testPpWithTriPredicate() {
            TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
            Throwables.Predicate<Boolean, RuntimeException> pred = Fnn.pp("hello", 5, triPred);
            Assertions.assertTrue(pred.test(true));
            Assertions.assertFalse(pred.test(false));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", 5, (TriPredicate<String, Integer, Boolean>) null));
        }

        @Test
        public void testPpBiPredicate() {
            BiPredicate<String, Integer> biPred = (s, i) -> s.length() == i;
            Throwables.BiPredicate<String, Integer, RuntimeException> result = Fnn.pp(biPred);
            Assertions.assertSame(biPred, result);
            Assertions.assertTrue(result.test("hello", 5));
            Assertions.assertFalse(result.test("hello", 3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((BiPredicate<String, Integer>) null));
        }

        @Test
        public void testPpBiPredicateWithPartial() {
            TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
            Throwables.BiPredicate<Integer, Boolean, RuntimeException> biPred = Fnn.pp("hello", triPred);
            Assertions.assertTrue(biPred.test(5, true));
            Assertions.assertFalse(biPred.test(5, false));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp("hello", (TriPredicate<String, Integer, Boolean>) null));
        }

        @Test
        public void testPpTriPredicate() {
            TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> s.length() == i && b;
            Throwables.TriPredicate<String, Integer, Boolean, RuntimeException> result = Fnn.pp(triPred);
            Assertions.assertSame(triPred, result);
            Assertions.assertTrue(result.test("hello", 5, true));
            Assertions.assertFalse(result.test("hello", 5, false));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.pp((TriPredicate<String, Integer, Boolean>) null));
        }

        @Test
        public void testCcConsumer() {
            Consumer<String> consumer = s -> {
            };
            Throwables.Consumer<String, RuntimeException> result = Fnn.cc(consumer);
            Assertions.assertSame(consumer, result);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((Consumer<String>) null));
        }

        @Test
        public void testCcWithBiConsumer() {
            final String[] result = { "" };
            java.util.function.BiConsumer<String, Integer> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.Consumer<Integer, RuntimeException> consumer = Fnn.cc("test", biConsumer);
            consumer.accept(123);
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", (java.util.function.BiConsumer<String, Integer>) null));
        }

        @Test
        public void testCcWithTriConsumer() {
            final String[] result = { "" };
            TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.Consumer<Boolean, RuntimeException> consumer = Fnn.cc("test", 123, triConsumer);
            consumer.accept(true);
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", 123, (TriConsumer<String, Integer, Boolean>) null));
        }

        @Test
        public void testCcBiConsumer() {
            BiConsumer<String, Integer> biConsumer = (s, i) -> {
            };
            Throwables.BiConsumer<String, Integer, RuntimeException> result = Fnn.cc(biConsumer);
            Assertions.assertSame(biConsumer, result);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((BiConsumer<String, Integer>) null));
        }

        @Test
        public void testCcBiConsumerWithPartial() {
            final String[] result = { "" };
            TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.BiConsumer<Integer, Boolean, RuntimeException> biConsumer = Fnn.cc("test", triConsumer);
            biConsumer.accept(123, true);
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc("test", (TriConsumer<String, Integer, Boolean>) null));
        }

        @Test
        public void testCcTriConsumer() {
            TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
            };
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> result = Fnn.cc(triConsumer);
            Assertions.assertSame(triConsumer, result);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.cc((TriConsumer<String, Integer, Boolean>) null));
        }

        @Test
        public void testFfFunction() {
            Function<String, Integer> func = String::length;
            Throwables.Function<String, Integer, RuntimeException> result = Fnn.ff(func);
            Assertions.assertSame(func, result);
            Assertions.assertEquals(5, result.apply("hello"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((Function<String, Integer>) null));
        }

        @Test
        public void testFfWithBiFunction() {
            java.util.function.BiFunction<String, Integer, String> biFunc = (s, i) -> s + i;
            Throwables.Function<Integer, String, RuntimeException> func = Fnn.ff("test", biFunc);
            Assertions.assertEquals("test123", func.apply(123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", (java.util.function.BiFunction<String, Integer, String>) null));
        }

        @Test
        public void testFfWithTriFunction() {
            TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
            Throwables.Function<Boolean, String, RuntimeException> func = Fnn.ff("test", 123, triFunc);
            Assertions.assertEquals("test123true", func.apply(true));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", 123, (TriFunction<String, Integer, Boolean, String>) null));
        }

        @Test
        public void testFfBiFunction() {
            BiFunction<String, Integer, String> biFunc = (s, i) -> s + i;
            Throwables.BiFunction<String, Integer, String, RuntimeException> result = Fnn.ff(biFunc);
            Assertions.assertSame(biFunc, result);
            Assertions.assertEquals("test123", result.apply("test", 123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((BiFunction<String, Integer, String>) null));
        }

        @Test
        public void testFfBiFunctionWithPartial() {
            TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
            Throwables.BiFunction<Integer, Boolean, String, RuntimeException> biFunc = Fnn.ff("test", triFunc);
            Assertions.assertEquals("test123true", biFunc.apply(123, true));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff("test", (TriFunction<String, Integer, Boolean, String>) null));
        }

        @Test
        public void testFfTriFunction() {
            TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> s + i + b;
            Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> result = Fnn.ff(triFunc);
            Assertions.assertSame(triFunc, result);
            Assertions.assertEquals("test123true", result.apply("test", 123, true));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.ff((TriFunction<String, Integer, Boolean, String>) null));
        }

        @Test
        public void testSpPredicate() {
            final Object mutex = new Object();
            final int[] callCount = { 0 };
            Throwables.Predicate<String, RuntimeException> pred = s -> {
                callCount[0]++;
                return s.length() > 3;
            };

            Throwables.Predicate<String, RuntimeException> syncPred = Fnn.sp(mutex, pred);
            Assertions.assertTrue(syncPred.test("hello"));
            Assertions.assertEquals(1, callCount[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, pred));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, (Throwables.Predicate<String, RuntimeException>) null));
        }

        @Test
        public void testSpWithBiPredicate() {
            final Object mutex = new Object();
            Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
            Throwables.Predicate<Integer, RuntimeException> syncPred = Fnn.sp(mutex, "hello", biPred);

            Assertions.assertTrue(syncPred.test(5));
            Assertions.assertFalse(syncPred.test(3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, "hello", biPred));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, "hello", null));
        }

        @Test
        public void testSpBiPredicate() {
            final Object mutex = new Object();
            Throwables.BiPredicate<String, Integer, RuntimeException> biPred = (s, i) -> s.length() == i;
            Throwables.BiPredicate<String, Integer, RuntimeException> syncBiPred = Fnn.sp(mutex, biPred);

            Assertions.assertTrue(syncBiPred.test("hello", 5));
            Assertions.assertFalse(syncBiPred.test("hello", 3));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(null, biPred));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sp(mutex, (Throwables.BiPredicate<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testScConsumer() {
            final Object mutex = new Object();
            final String[] result = { "" };
            Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
            Throwables.Consumer<String, RuntimeException> syncConsumer = Fnn.sc(mutex, consumer);

            syncConsumer.accept("test");
            Assertions.assertEquals("test", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, consumer));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, (Throwables.Consumer<String, RuntimeException>) null));
        }

        @Test
        public void testScWithBiConsumer() {
            final Object mutex = new Object();
            final String[] result = { "" };
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.Consumer<Integer, RuntimeException> syncConsumer = Fnn.sc(mutex, "test", biConsumer);

            syncConsumer.accept(123);
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, "test", biConsumer));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, "test", null));
        }

        @Test
        public void testScBiConsumer() {
            final Object mutex = new Object();
            final String[] result = { "" };
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.BiConsumer<String, Integer, RuntimeException> syncBiConsumer = Fnn.sc(mutex, biConsumer);

            syncBiConsumer.accept("test", 123);
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(null, biConsumer));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sc(mutex, (Throwables.BiConsumer<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testSfFunction() {
            final Object mutex = new Object();
            Throwables.Function<String, Integer, RuntimeException> func = String::length;
            Throwables.Function<String, Integer, RuntimeException> syncFunc = Fnn.sf(mutex, func);

            Assertions.assertEquals(5, syncFunc.apply("hello"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, func));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(mutex, (Throwables.Function<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testSfWithBiFunction() {
            final Object mutex = new Object();
            Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
            Throwables.Function<Integer, String, RuntimeException> syncFunc = Fnn.sf(mutex, "test", biFunc);

            Assertions.assertEquals("test123", syncFunc.apply(123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, "test", biFunc));
            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(mutex, "test", null));
        }

        @Test
        public void testSfBiFunction() {
            final Object mutex = new Object();
            Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
            Throwables.BiFunction<String, Integer, String, RuntimeException> syncBiFunc = Fnn.sf(mutex, biFunc);

            Assertions.assertEquals("test123", syncBiFunc.apply("test", 123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.sf(null, biFunc));
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.sf(mutex, (Throwables.BiFunction<String, Integer, String, RuntimeException>) null));
        }

        @Test
        public void testC2fConsumerToFunction() {
            final String[] result = { "" };
            Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
            Throwables.Function<String, Void, RuntimeException> func = Fnn.c2f(consumer);

            Assertions.assertNull(func.apply("test"));
            Assertions.assertEquals("test", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, RuntimeException>) null));
        }

        @Test
        public void testC2fConsumerToFunctionWithValue() {
            final String[] result = { "" };
            Throwables.Consumer<String, RuntimeException> consumer = s -> result[0] = s;
            Throwables.Function<String, Integer, RuntimeException> func = Fnn.c2f(consumer, 42);

            Assertions.assertEquals(42, func.apply("test"));
            Assertions.assertEquals("test", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.Consumer<String, RuntimeException>) null, 42));
        }

        @Test
        public void testC2fBiConsumerToBiFunction() {
            final String[] result = { "" };
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.BiFunction<String, Integer, Void, RuntimeException> biFunc = Fnn.c2f(biConsumer);

            Assertions.assertNull(biFunc.apply("test", 123));
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testC2fBiConsumerToBiFunctionWithValue() {
            final String[] result = { "" };
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = (s, i) -> result[0] = s + i;
            Throwables.BiFunction<String, Integer, Double, RuntimeException> biFunc = Fnn.c2f(biConsumer, 3.14);

            Assertions.assertEquals(3.14, biFunc.apply("test", 123));
            Assertions.assertEquals("test123", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.BiConsumer<String, Integer, RuntimeException>) null, 3.14));
        }

        @Test
        public void testC2fTriConsumerToTriFunction() {
            final String[] result = { "" };
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.TriFunction<String, Integer, Boolean, Void, RuntimeException> triFunc = Fnn.c2f(triConsumer);

            Assertions.assertNull(triFunc.apply("test", 123, true));
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null));
        }

        @Test
        public void testC2fTriConsumerToTriFunctionWithValue() {
            final String[] result = { "" };
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = (s, i, b) -> result[0] = s + i + b;
            Throwables.TriFunction<String, Integer, Boolean, Long, RuntimeException> triFunc = Fnn.c2f(triConsumer, 999L);

            Assertions.assertEquals(999L, triFunc.apply("test", 123, true));
            Assertions.assertEquals("test123true", result[0]);

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.c2f((Throwables.TriConsumer<String, Integer, Boolean, RuntimeException>) null, 999L));
        }

        @Test
        public void testF2cFunctionToConsumer() {
            Throwables.Function<String, Integer, RuntimeException> func = String::length;
            Throwables.Consumer<String, RuntimeException> consumer = Fnn.f2c(func);

            Assertions.assertDoesNotThrow(() -> consumer.accept("test"));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.Function<String, Integer, RuntimeException>) null));
        }

        @Test
        public void testF2cBiFunctionToBiConsumer() {
            Throwables.BiFunction<String, Integer, String, RuntimeException> biFunc = (s, i) -> s + i;
            Throwables.BiConsumer<String, Integer, RuntimeException> biConsumer = Fnn.f2c(biFunc);

            Assertions.assertDoesNotThrow(() -> biConsumer.accept("test", 123));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.f2c((Throwables.BiFunction<String, Integer, String, RuntimeException>) null));
        }

        @Test
        public void testF2cTriFunctionToTriConsumer() {
            Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException> triFunc = (s, i, b) -> s + i + b;
            Throwables.TriConsumer<String, Integer, Boolean, RuntimeException> triConsumer = Fnn.f2c(triFunc);

            Assertions.assertDoesNotThrow(() -> triConsumer.accept("test", 123, true));

            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> Fnn.f2c((Throwables.TriFunction<String, Integer, Boolean, String, RuntimeException>) null));
        }

        @Test
        public void testR() {
            Throwables.Runnable<Exception> runnable = () -> {
            };
            Assertions.assertSame(runnable, Fnn.r(runnable));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r(null));
        }

        @Test
        public void testC() {
            Throwables.Callable<String, RuntimeException> callable = () -> "result";
            Assertions.assertSame(callable, Fnn.c(callable));

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c((Throwables.Callable) null));
        }

        @Test
        public void testR2cRunnableToCallable() {
            final boolean[] executed = { false };
            Throwables.Runnable<RuntimeException> runnable = () -> executed[0] = true;
            Throwables.Callable<Void, RuntimeException> callable = Fnn.r2c(runnable);

            Assertions.assertNull(callable.call());
            Assertions.assertTrue(executed[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null));
        }

        @Test
        public void testR2cRunnableToCallableWithValue() {
            final boolean[] executed = { false };
            Throwables.Runnable<RuntimeException> runnable = () -> executed[0] = true;
            Throwables.Callable<String, RuntimeException> callable = Fnn.r2c(runnable, "result");

            Assertions.assertEquals("result", callable.call());
            Assertions.assertTrue(executed[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2c(null, "result"));
        }

        @Test
        public void testC2rCallableToRunnable() {
            Throwables.Callable<String, RuntimeException> callable = () -> "result";
            Throwables.Runnable<RuntimeException> runnable = Fnn.c2r(callable);

            Assertions.assertDoesNotThrow(() -> runnable.run());

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2r(null));
        }

        @Test
        public void testRr() {
            com.landawn.abacus.util.function.Runnable runnable = () -> {
            };
            Throwables.Runnable<RuntimeException> result = Fnn.rr(runnable);
            Assertions.assertSame(runnable, result);
        }

        @Test
        public void testCc() {
            Callable<String> callable = () -> "result";
            Throwables.Callable<String, RuntimeException> result = Fnn.cc(callable);
            Assertions.assertSame(callable, result);
        }

        @Test
        public void testJr2r() {
            final boolean[] executed = { false };
            java.lang.Runnable javaRunnable = () -> executed[0] = true;
            Throwables.Runnable<RuntimeException> throwableRunnable = Fnn.jr2r(javaRunnable);

            throwableRunnable.run();
            Assertions.assertTrue(executed[0]);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.jr2r(null));
        }

        @Test
        public void testR2jr() {
            final boolean[] executed = { false };
            Throwables.Runnable<Exception> throwableRunnable = () -> {
                executed[0] = true;
            };
            java.lang.Runnable javaRunnable = Fnn.r2jr(throwableRunnable);

            javaRunnable.run();
            Assertions.assertTrue(executed[0]);

            Throwables.Runnable<Exception> throwingRunnable = () -> {
                throw new IOException("Test exception");
            };
            java.lang.Runnable wrappedRunnable = Fnn.r2jr(throwingRunnable);
            Assertions.assertThrows(RuntimeException.class, wrappedRunnable::run);

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.r2jr(null));
        }

        @Test
        public void testJc2c() throws Exception {
            java.util.concurrent.Callable<String> javaCallable = () -> "result";
            Throwables.Callable<String, Exception> throwableCallable = Fnn.jc2c(javaCallable);

            Assertions.assertEquals("result", throwableCallable.call());

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.jc2c(null));
        }

        @Test
        public void testC2jc() throws Exception {
            Throwables.Callable<String, RuntimeException> throwableCallable = () -> "result";
            java.util.concurrent.Callable<String> javaCallable = Fnn.c2jc(throwableCallable);

            Assertions.assertEquals("result", javaCallable.call());

            Assertions.assertThrows(IllegalArgumentException.class, () -> Fnn.c2jc(null));
        }
    }
}
