package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class FnAdapterContractTest extends TestBase {
    static java.util.stream.Stream<Method> checkedAdapters() {
        return java.util.stream.Stream.of(Fn.class, Fn.Entries.class)
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> Set.of("ss", "pp", "cc", "ff", "rr").contains(m.getName()))
                .filter(m -> m.getParameterCount() > 0
                        && m.getParameterTypes()[m.getParameterCount() - 1].getName().startsWith(Throwables.class.getName() + "$"))
                .sorted(java.util.Comparator.comparing(Method::toGenericString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("checkedAdapters")
    void checkedAdaptersForwardBoundArgumentsAndPreserveFailures(Method factory) throws Throwable {
        Class<?> callbackType = factory.getParameterTypes()[factory.getParameterCount() - 1];
        Method callbackMethod = sam(callbackType);
        Method targetMethod = sam(factory.getReturnType());
        Object[] factoryArgs = new Object[factory.getParameterCount()];
        List<Object> expectedArguments = new ArrayList<>();
        for (int i = 0; i < factoryArgs.length - 1; i++) {
            factoryArgs[i] = "bound-" + i;
            expectedArguments.add(factoryArgs[i]);
        }
        Object[] targetArgs = new Object[targetMethod.getParameterCount()];
        for (int i = 0; i < targetArgs.length; i++) {
            targetArgs[i] = factory.getDeclaringClass() == Fn.Entries.class ? Map.entry("key", "value") : "argument-" + i;
            if (targetArgs[i] instanceof Map.Entry<?, ?> entry) {
                expectedArguments.add(entry.getKey());
                expectedArguments.add(entry.getValue());
            } else
                expectedArguments.add(targetArgs[i]);
        }
        assertThrows(IllegalArgumentException.class, () -> invoke(factory, null, factoryArgs));
        List<Object> seen = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        Object expectedResult = callbackMethod.getReturnType() == boolean.class ? true : callbackMethod.getReturnType() == void.class ? null : "result";
        factoryArgs[factoryArgs.length - 1] = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType }, (proxy, method, args) -> {
            calls.incrementAndGet();
            if (args != null)
                seen.addAll(Arrays.asList(args));
            return expectedResult;
        });
        Object adapter = invoke(factory, null, factoryArgs);
        assertEquals(expectedResult, invoke(targetMethod, adapter, targetArgs));
        assertEquals(1, calls.get());
        assertEquals(expectedArguments, seen);

        for (Exception failure : List.of(new IOException("checked failure"), new IllegalStateException("unchecked failure"))) {
            factoryArgs[factoryArgs.length - 1] = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[] { callbackType },
                    (proxy, method, args) -> {
                        throw failure;
                    });
            Object throwing = invoke(factory, null, factoryArgs);
            RuntimeException actual = assertThrows(RuntimeException.class, () -> invoke(targetMethod, throwing, targetArgs));
            if (failure instanceof RuntimeException)
                assertSame(failure, actual);
            else
                assertSame(failure, actual.getCause());
        }
    }

    static java.util.stream.Stream<Method> primitiveFunctions() {
        return Arrays.stream(Fn.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().matches("F[BCSILFD]"))
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()))
                .filter(m -> (m.getParameterCount() == 1 && Set.of("p", "c", "f").contains(m.getName())) || (m.getParameterCount() == 0
                        && Set.of("equal", "notEqual", "greaterThan", "greaterThanOrEqual", "lessThan", "lessThanOrEqual", "alternate").contains(m.getName())))
                .sorted(java.util.Comparator.comparing(Method::toGenericString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("primitiveFunctions")
    void primitiveAliasesComparisonsAndAlternationHonorTheirContracts(Method factory) throws Throwable {
        if (factory.getParameterCount() == 1) {
            Class<?> type = factory.getParameterTypes()[0];
            Object callback = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (p, m, a) -> null);
            assertSame(callback, invoke(factory, null, callback));
            assertThrows(IllegalArgumentException.class, () -> invoke(factory, null, new Object[] { null }));
            return;
        }
        Object function = invoke(factory, null);
        Method method = sam(factory.getReturnType());
        Class<?> argumentType = method.getParameterTypes()[0];
        if (factory.getName().equals("alternate")) {
            assertEquals(MergeResult.TAKE_FIRST, invoke(method, function, number(argumentType, 1), number(argumentType, 2)));
            assertEquals(MergeResult.TAKE_SECOND, invoke(method, function, number(argumentType, 1), number(argumentType, 2)));
            assertEquals(MergeResult.TAKE_FIRST, invoke(method, function, number(argumentType, 1), number(argumentType, 2)));
            return;
        }
        for (int[] values : new int[][] { { 1, 2 }, { 2, 1 }, { 1, 1 } }) {
            int comparison = Integer.compare(values[0], values[1]);
            boolean expected = switch (factory.getName()) {
                case "equal" -> comparison == 0;
                case "notEqual" -> comparison != 0;
                case "greaterThan" -> comparison > 0;
                case "greaterThanOrEqual" -> comparison >= 0;
                case "lessThan" -> comparison < 0;
                case "lessThanOrEqual" -> comparison <= 0;
                default -> throw new AssertionError(factory);
            };
            assertEquals(expected, invoke(method, function, number(argumentType, values[0]), number(argumentType, values[1])));
        }
    }

    private static Object number(Class<?> type, int value) {
        if (type == byte.class)
            return (byte) value;
        if (type == char.class)
            return (char) value;
        if (type == short.class)
            return (short) value;
        if (type == int.class)
            return value;
        if (type == long.class)
            return (long) value;
        if (type == float.class)
            return (float) value;
        if (type == double.class)
            return (double) value;
        throw new AssertionError(type);
    }

    private static Method sam(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()) && m.getDeclaringClass() != Object.class)
                .findFirst()
                .orElseThrow();
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
