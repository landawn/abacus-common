package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class ZipWithNullArgumentsTest extends TestBase {

    private record Family(Class<?> streamType, Class<?> elementType) {
        BaseStream<?, ?, ?, ?, ?, ?, ?, ?> create(final boolean iteratorSource, final int workers, final int size) throws ReflectiveOperationException {
            final Object values = Array.newInstance(elementType, size);
            Object source = values;
            Class<?> sourceType = values.getClass();

            if (iteratorSource) {
                if (streamType == Stream.class) {
                    source = Arrays.asList((Object[]) values).iterator();
                    sourceType = java.util.Iterator.class;
                } else {
                    sourceType = Class.forName("com.landawn.abacus.util." + streamType.getSimpleName().replace("Stream", "Iterator"));
                    source = sourceType.getMethod("of", values.getClass()).invoke(null, values);
                }
            }

            final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = (BaseStream<?, ?, ?, ?, ?, ?, ?, ?>) streamType.getMethod("of", sourceType).invoke(null, source);
            return workers == 0 ? stream : stream.parallel(workers);
        }

        @Override
        public String toString() {
            return streamType.getSimpleName();
        }
    }

    static java.util.stream.Stream<Arguments> streamConfigurations() {
        final List<Arguments> cases = new ArrayList<>();
        for (final Family family : List.of(new Family(ByteStream.class, byte.class), new Family(CharStream.class, char.class),
                new Family(ShortStream.class, short.class), new Family(IntStream.class, int.class), new Family(LongStream.class, long.class),
                new Family(FloatStream.class, float.class), new Family(DoubleStream.class, double.class), new Family(Stream.class, Object.class))) {
            for (final boolean iteratorSource : new boolean[] { false, true }) {
                for (final int workers : new int[] { 0, 1, 2 }) {
                    for (final int size : new int[] { 0, 1, 3 }) {
                        cases.add(Arguments.of(family, iteratorSource, workers, size));
                    }
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}, iterator={1}, workers={2}, size={3}")
    @MethodSource("streamConfigurations")
    public void rejectsEveryNullStreamAndOperatorEagerly(final Family family, final boolean iteratorSource, final int workers, final int size)
            throws Throwable {
        // Reflection applies the same contract checks to all four overloads in every stream family.
        final List<Method> overloads = Arrays.stream(family.streamType.getMethods())
                .filter(method -> method.getName().equals("zipWith") && method.getParameterTypes()[0] == family.streamType)
                .toList();
        assertEquals(4, overloads.size());

        for (final Method method : overloads) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (int nullIndex = 0; nullIndex < parameterTypes.length; nullIndex++) {
                if (parameterTypes[nullIndex] != family.streamType && nullIndex != parameterTypes.length - 1) {
                    continue; // Padding values are allowed to be null for object streams.
                }

                final java.util.concurrent.atomic.AtomicInteger closes = new java.util.concurrent.atomic.AtomicInteger();
                try (var receiver = family.create(iteratorSource, workers, size).onClose(closes::incrementAndGet); var b = family.create(false, 0, 3);
                        var c = family.create(false, 0, 3)) {
                    final Object[] args = arguments(method, family, b, c);
                    args[nullIndex] = null;
                    assertThrows(IllegalArgumentException.class, () -> {
                        try (var unexpectedResult = (AutoCloseable) invoke(method, receiver, args)) {
                            // Validation must fail when zipWith is called, without a terminal operation.
                        }
                    }, method + ", null parameter " + nullIndex);
                    // checkArgNotNull also releases the receiver's resources when validation fails.
                    assertEquals(1, closes.get());
                }
                assertEquals(1, closes.get(), "Closing an already rejected receiver must be idempotent");
            }

            try (var receiver = family.create(iteratorSource, workers, size)) {
                receiver.close();
                final Object[] args = arguments(method, family, null, null);
                args[args.length - 1] = null;
                assertThrows(IllegalStateException.class, () -> invoke(method, receiver, args));
            }
        }
    }

    private static Object[] arguments(final Method method, final Family family, final Object b, final Object c) {
        final Class<?>[] types = method.getParameterTypes();
        final Object[] args = new Object[types.length];
        for (int i = 0; i < types.length - 1; i++) {
            args[i] = types[i] == family.streamType ? (i == 0 ? b : c) : types[i].isPrimitive() ? Array.get(Array.newInstance(types[i], 1), 0) : null;
        }
        final Class<?> operatorType = types[types.length - 1];
        args[args.length - 1] = Proxy.newProxyInstance(operatorType.getClassLoader(), new Class<?>[] { operatorType }, (proxy, invoked, values) -> {
            throw new AssertionError("The zip function must not run during argument validation");
        });
        return args;
    }

    private static Object invoke(final Method method, final Object receiver, final Object[] args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static java.util.stream.Stream<Arguments> executionModes() {
        return java.util.stream.Stream.of(Arguments.of(false, 0), Arguments.of(true, 0), Arguments.of(false, 1), Arguments.of(true, 1),
                Arguments.of(false, 2), Arguments.of(true, 2));
    }

    @ParameterizedTest
    @MethodSource("executionModes")
    public void preservesValidZipResults(final boolean iteratorSource, final int workers) {
        // Parallel iterator streams may emit results out of encounter order.
        try (DoubleStream a = doubles(iteratorSource, workers); DoubleStream b = DoubleStream.of(10, 20)) {
            assertArrayEquals(new double[] { 11, 22 }, a.zipWith(b, (x, y) -> x + y).sorted().toArray());
        }
        try (DoubleStream a = doubles(iteratorSource, workers); DoubleStream b = DoubleStream.of(10, 20); DoubleStream c = DoubleStream.of(100)) {
            assertArrayEquals(new double[] { 111 }, a.zipWith(b, c, (x, y, z) -> x + y + z).sorted().toArray());
        }
        try (DoubleStream a = doubles(iteratorSource, workers); DoubleStream b = DoubleStream.of(10, 20)) {
            assertArrayEquals(new double[] { 3, 11, 22 }, a.zipWith(b, 0, 0, (x, y) -> x + y).sorted().toArray());
        }
        try (DoubleStream a = doubles(iteratorSource, workers); DoubleStream b = DoubleStream.of(10, 20); DoubleStream c = DoubleStream.of(100)) {
            assertArrayEquals(new double[] { 3, 22, 111 }, a.zipWith(b, c, 0, 0, 0, (x, y, z) -> x + y + z).sorted().toArray());
        }
    }

    private static DoubleStream doubles(final boolean iteratorSource, final int workers) {
        final DoubleStream stream = iteratorSource ? DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(1, 2, 3)) : DoubleStream.of(1, 2, 3);
        return workers == 0 ? stream : stream.parallel(workers);
    }

    @ParameterizedTest
    @MethodSource("executionModes")
    public void preservesNullableObjectPaddingAndCollectionInputs(final boolean iteratorSource, final int workers) {
        try (Stream<Integer> a = objects(iteratorSource, workers); Stream<Integer> b = Stream.of(10)) {
            assertEquals(Arrays.asList("1:10", "2:null"), a.zipWith(b, null, null, (x, y) -> x + ":" + y).sorted().toList());
        }
        try (Stream<Integer> a = objects(iteratorSource, workers); Stream<Integer> b = Stream.of(10); Stream<Integer> c = Stream.of(100)) {
            assertEquals(Arrays.asList("1:10:100", "2:null:null"), a.zipWith(b, c, null, null, null, (x, y, z) -> x + ":" + y + ":" + z).sorted().toList());
        }
        try (Stream<Integer> a = objects(iteratorSource, workers)) {
            assertEquals(Arrays.asList("1:null", "2:null"), a.zipWith((Collection<Integer>) null, null, null, (x, y) -> x + ":" + y).sorted().toList());
        }
    }

    private static Stream<Integer> objects(final boolean iteratorSource, final int workers) {
        final Stream<Integer> stream = iteratorSource ? Stream.of(Arrays.asList(1, 2).iterator()) : Stream.of(1, 2);
        return workers == 0 ? stream : stream.parallel(workers);
    }
}
