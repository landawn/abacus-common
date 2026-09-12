package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ObserverTypedStageTest {
    private static <T> List<T> collect(Observer<T> observer) throws Exception {
        List<T> values = new CopyOnWriteArrayList<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        observer.observe(values::add, failure -> {
            error.set(failure);
            done.countDown();
        }, done::countDown);
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        return values;
    }

    private static void rejectAllOperators(Observer<?> stale) throws Exception {
        for (Method method : Observer.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Object[] arguments = new Object[method.getParameterCount()];
            Class<?>[] types = method.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                arguments[i] = types[i] == long.class ? 0L
                        : types[i] == int.class ? 1
                                : types[i] == TimeUnit.class ? TimeUnit.DAYS
                                        : types[i] == Function.class ? (Function<Object, Object>) value -> value
                                                : types[i] == Predicate.class ? (Predicate<Object>) value -> true
                                                        : types[i] == Consumer.class ? (Consumer<Object>) value -> {
                                                        } : (Runnable) () -> {
                                                        };
            }
            InvocationTargetException failure = assertThrows(InvocationTargetException.class, () -> method.invoke(stale, arguments), method.toString());
            assertInstanceOf(IllegalStateException.class, failure.getCause(), method.toString());
        }
    }

    @Test
    void everyTypeChangingOperatorInvalidatesOldHandlesBeforeConsumption() throws Exception {
        for (int mode = 0; mode < 8; mode++) {
            AtomicInteger touched = new AtomicInteger();
            Observer<Integer> original = Observer.of(new Iterator<Integer>() {
                boolean more = true;

                @Override
                public boolean hasNext() {
                    touched.incrementAndGet();
                    return more;
                }

                @Override
                public Integer next() {
                    more = false;
                    return 7;
                }
            });
            Observer<?> current = switch (mode) {
                case 0 -> original.map(Object::toString);
                case 1 -> original.flatMap(value -> List.of(value.toString()));
                case 2 -> original.timestamp();
                case 3 -> original.timeInterval();
                case 4 -> original.buffer(1, TimeUnit.DAYS);
                case 5 -> original.buffer(1, TimeUnit.DAYS, 1);
                case 6 -> original.buffer(1, 1, TimeUnit.DAYS);
                default -> original.buffer(1, 1, TimeUnit.DAYS, 1);
            };
            assertNotSame(original, current);
            rejectAllOperators(original);
            assertEquals(0, touched.get());
            assertEquals(1, collect(current).size());
            rejectAllOperators(current);
        }
    }

    @Test
    void typedChainsNoopsInvalidArgumentsAndScheduledSources() throws Exception {
        Observer<Integer> original = Observer.of(List.of(1, 2, 3));
        Observer<String> strings = original.map(Object::toString);
        Observer<Integer> numbers = strings.map(Integer::valueOf);
        rejectAllOperators(strings);
        assertSame(numbers, numbers.skip(0));
        assertSame(numbers, numbers.delay(0));
        assertSame(numbers, numbers.limit(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> numbers.map(null));
        assertThrows(IllegalArgumentException.class, () -> numbers.flatMap(null));
        assertThrows(IllegalArgumentException.class, () -> numbers.buffer(0, TimeUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> numbers.buffer(1, null));
        assertThrows(IllegalArgumentException.class, () -> numbers.buffer(1, TimeUnit.DAYS, 0));
        assertThrows(IllegalArgumentException.class, () -> numbers.buffer(1, 0, TimeUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> numbers.buffer(1, 1, TimeUnit.DAYS, 0));
        assertEquals(List.of(List.of(2, 2, 3), List.of(3)),
                collect(numbers.filter(value -> value > 1).flatMap(value -> List.of(value, value)).buffer(1, TimeUnit.DAYS, 3)));
        assertTrue(collect(Observer.<Integer> of(List.of()).map(Object::toString)).isEmpty());
        assertEquals(List.of("0"), collect(Observer.interval(0, 1, TimeUnit.DAYS).map(Object::toString).limit(1)));
        assertEquals(1, collect(Observer.timer(0).timestamp().map(value -> value.value().toString())).size());
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        assertEquals(List.of(List.of()), collect(Observer.of(queue).map(Object::toString).buffer(20, TimeUnit.MILLISECONDS).limit(1)));
        assertTrue(queue.isEmpty());
    }

    private static class Custom<T> extends Observer<T> {
        final T value;
        int initialized;

        Custom(T value) {
            this.value = value;
        }

        @Override
        public void observe(Consumer<? super T> action, Consumer<? super Exception> error, Runnable complete) {
            addSubscriptionAction(() -> initialized++);
            beginSubscription(action, error, complete);
            dispatcher.append(new Dispatcher<>() {
                @Override
                public void onNext(Object value) {
                    action.accept((T) value);
                }

                @Override
                public void onError(Exception failure) {
                    error.accept(failure);
                }

                @Override
                public void onComplete() {
                    complete.run();
                }
            });
            startSubscriptionActions();
            dispatcher.onNext(value);
            dispatcher.onComplete();
        }

        void initializeDirectly() {
            addSubscriptionAction(() -> initialized++);
        }
    }

    @Test
    void forwardedCustomSourceCanInitializeButAuthorizationIsCleared() throws Exception {
        Custom<Integer> source = new Custom<>(7);
        Observer<String> mapped = source.map(Object::toString);
        assertThrows(IllegalStateException.class, source::initializeDirectly);
        assertEquals(List.of("7"), collect(mapped));
        assertEquals(1, source.initialized);
        assertThrows(IllegalStateException.class, source::initializeDirectly);
        var field = Observer.class.getDeclaredField("subscriptionDelegate");
        field.setAccessible(true);
        assertNull(((ThreadLocal<?>) field.get(source)).get());
    }
}
