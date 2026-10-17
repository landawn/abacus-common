package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

/** All function adapters promise an identity view, preserving arguments, results, and exceptions. */
class ThrowableFunctionViewContractTest extends TestBase {
    static java.util.stream.Stream<Class<?>> interfaces() {
        return java.util.stream.Stream.of(BiConsumer.class, BiFunction.class, BiIntObjConsumer.class, BiIntObjFunction.class, BiIntObjPredicate.class,
                BiObjIntConsumer.class, BiObjIntFunction.class, BiObjIntPredicate.class, BiPredicate.class, BinaryOperator.class, BooleanBiConsumer.class,
                BooleanBiFunction.class, BooleanBiPredicate.class, BooleanBinaryOperator.class, BooleanConsumer.class, BooleanFunction.class,
                BooleanNFunction.class, BooleanPredicate.class, BooleanSupplier.class, BooleanTernaryOperator.class, BooleanTriConsumer.class,
                BooleanTriFunction.class, BooleanTriPredicate.class, BooleanUnaryOperator.class, ByteBiConsumer.class, ByteBiFunction.class,
                ByteBiPredicate.class, ByteBinaryOperator.class, ByteConsumer.class, ByteFunction.class, ByteNFunction.class, BytePredicate.class,
                ByteSupplier.class, ByteTernaryOperator.class, ByteTriConsumer.class, ByteTriFunction.class, ByteTriPredicate.class, ByteUnaryOperator.class,
                Callable.class, CharBiConsumer.class, CharBiFunction.class, CharBiPredicate.class, CharBinaryOperator.class, CharConsumer.class,
                CharFunction.class, CharNFunction.class, CharPredicate.class, CharSupplier.class, CharTernaryOperator.class, CharTriConsumer.class,
                CharTriFunction.class, CharTriPredicate.class, CharUnaryOperator.class, Consumer.class, DoubleBiConsumer.class, DoubleBiFunction.class,
                DoubleBiPredicate.class, DoubleBinaryOperator.class, DoubleConsumer.class, DoubleFunction.class, DoubleNFunction.class, DoubleObjConsumer.class,
                DoubleObjFunction.class, DoubleObjPredicate.class, DoublePredicate.class, DoubleSupplier.class, DoubleTernaryOperator.class,
                DoubleToIntFunction.class, DoubleToLongFunction.class, DoubleTriConsumer.class, DoubleTriFunction.class, DoubleTriPredicate.class,
                DoubleUnaryOperator.class, FloatBiConsumer.class, FloatBiFunction.class, FloatBiPredicate.class, FloatBinaryOperator.class, FloatConsumer.class,
                FloatFunction.class, FloatNFunction.class, FloatPredicate.class, FloatSupplier.class, FloatTernaryOperator.class, FloatToDoubleFunction.class,
                FloatToIntFunction.class, FloatToLongFunction.class, FloatTriConsumer.class, FloatTriFunction.class, FloatTriPredicate.class,
                FloatUnaryOperator.class, Function.class, IntBiConsumer.class, IntBiFunction.class, IntBiObjConsumer.class, IntBiObjFunction.class,
                IntBiObjPredicate.class, IntBiPredicate.class, IntBinaryOperator.class, IntConsumer.class, IntFunction.class, IntNFunction.class,
                IntObjConsumer.class, IntObjFunction.class, IntObjOperator.class, IntObjPredicate.class, IntPredicate.class, IntSupplier.class,
                IntTernaryOperator.class, IntToDoubleFunction.class, IntToLongFunction.class, IntTriConsumer.class, IntTriFunction.class, IntTriPredicate.class,
                IntUnaryOperator.class, LongBiConsumer.class, LongBiFunction.class, LongBiPredicate.class, LongBinaryOperator.class, LongConsumer.class,
                LongFunction.class, LongNFunction.class, LongObjConsumer.class, LongObjFunction.class, LongObjPredicate.class, LongPredicate.class,
                LongSupplier.class, LongTernaryOperator.class, LongToDoubleFunction.class, LongToIntFunction.class, LongTriConsumer.class,
                LongTriFunction.class, LongTriPredicate.class, LongUnaryOperator.class, NFunction.class, ObjBiIntConsumer.class, ObjBiIntFunction.class,
                ObjBiIntPredicate.class, ObjBooleanConsumer.class, ObjByteConsumer.class, ObjCharConsumer.class, ObjDoubleConsumer.class,
                ObjDoubleFunction.class, ObjDoublePredicate.class, ObjFloatConsumer.class, ObjIntConsumer.class, ObjIntFunction.class, ObjIntPredicate.class,
                ObjLongConsumer.class, ObjLongFunction.class, ObjLongPredicate.class, ObjShortConsumer.class, Predicate.class, QuadConsumer.class,
                QuadFunction.class, QuadPredicate.class, Runnable.class, ShortBiConsumer.class, ShortBiFunction.class, ShortBiPredicate.class,
                ShortBinaryOperator.class, ShortConsumer.class, ShortFunction.class, ShortNFunction.class, ShortPredicate.class, ShortSupplier.class,
                ShortTernaryOperator.class, ShortTriConsumer.class, ShortTriFunction.class, ShortTriPredicate.class, ShortUnaryOperator.class, Supplier.class,
                ToBooleanBiFunction.class, ToBooleanFunction.class, ToByteBiFunction.class, ToByteFunction.class, ToCharBiFunction.class, ToCharFunction.class,
                ToDoubleBiFunction.class, ToDoubleFunction.class, ToDoubleTriFunction.class, ToFloatBiFunction.class, ToFloatFunction.class,
                ToIntBiFunction.class, ToIntFunction.class, ToIntTriFunction.class, ToLongBiFunction.class, ToLongFunction.class, ToLongTriFunction.class,
                ToShortBiFunction.class, ToShortFunction.class, TriConsumer.class, TriFunction.class, TriPredicate.class, UnaryOperator.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("interfaces")
    void throwableViewPreservesIdentityAndInvocation(Class<?> type) throws Throwable {
        Method adapter = type.getMethod("toThrowable");
        Method operation = Arrays.stream(adapter.getReturnType().getMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()) && !method.isBridge())
                .findFirst()
                .orElseThrow();
        Object[] arguments = Arrays.stream(operation.getParameterTypes()).map(ThrowableFunctionViewContractTest::sample).toArray();
        Object expected = sample(operation.getReturnType());
        AtomicReference<Object[]> received = new AtomicReference<>();
        AtomicBoolean fail = new AtomicBoolean();
        IllegalStateException failure = new IllegalStateException("original callback failure");
        Object function = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            received.set(args == null ? new Object[0] : args.clone());
            if (fail.get()) {
                throw failure;
            }
            return expected;
        });

        Object view = adapter.invoke(function);
        assertSame(function, view);
        assertTrue(adapter.getReturnType().isInstance(view));
        assertEquals(expected, operation.invoke(view, arguments));
        assertArrayEquals(arguments, received.get());

        fail.set(true);
        InvocationTargetException actual = assertThrows(InvocationTargetException.class, () -> operation.invoke(view, arguments));
        assertSame(failure, actual.getCause());
        assertArrayEquals(arguments, received.get());
    }

    private static Object sample(Class<?> type) {
        if (type == void.class)
            return null;
        if (type == boolean.class)
            return true;
        if (type == byte.class)
            return (byte) 7;
        if (type == char.class)
            return 'x';
        if (type == short.class)
            return (short) 11;
        if (type == int.class)
            return 13;
        if (type == long.class)
            return 17L;
        if (type == float.class)
            return 19.5f;
        if (type == double.class)
            return 23.5;
        if (type.isArray()) {
            Object values = Array.newInstance(type.getComponentType(), 2);
            Array.set(values, 0, sample(type.getComponentType()));
            Array.set(values, 1, sample(type.getComponentType()));
            return values;
        }
        return "value";
    }
}
