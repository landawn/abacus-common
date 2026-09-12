package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NullValidationRestartAMTest extends TestBase {
    public static String staticValue() {
        return "ok";
    }

    @Test
    public void adaptersValidateTheirSourceBeforeReturningWork() {
        assertThrows(IllegalArgumentException.class, () -> Enumerations.create((Iterator<?>) null));
        assertFalse(Enumerations.create((Collection<?>) null).hasMoreElements());
        final Enumeration<String> enumeration = Enumerations.create(Arrays.asList("a", null).iterator());
        assertEquals("a", enumeration.nextElement());
        assertNull(enumeration.nextElement());
        assertThrows(IllegalArgumentException.class, () -> Futures.allOf((Future<?>[]) null));
        assertThrows(IllegalArgumentException.class, () -> Futures.anyOf((Future<?>[]) null));
        assertThrows(IllegalArgumentException.class, () -> Futures.iterate((Future<?>[]) null));
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.ESCAPE_JAVA.with((EscapeUtil.CharSequenceTranslator[]) null));
        assertNull(EscapeUtil.escapeJava(null));
    }

    @Test
    public void shuffleAndCopyFollowTheirValidationOrder() throws Exception {
        final java.util.Random unusedRandom = new java.util.Random() {
            @Override
            public int nextInt(final int bound) {
                throw new AssertionError("No-op shuffles must not consume randomness");
            }
        };
        final Class<?>[] components = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class, Object.class };
        for (Class<?> component : components) {
            final Object pair = java.lang.reflect.Array.newInstance(component, 2);
            final Object empty = java.lang.reflect.Array.newInstance(component, 0);
            final Method shuffle = N.class.getMethod("shuffle", pair.getClass(), int.class, int.class, java.util.Random.class);
            final Method shuffleAll = N.class.getMethod("shuffle", pair.getClass(), java.util.Random.class);
            // Arrays validate the range and then rnd, before considering an empty or singleton range.
            for (int toIndex : new int[] { 0, 1, 2 }) {
                assertEquals(IllegalArgumentException.class,
                        assertThrows(InvocationTargetException.class, () -> shuffle.invoke(null, pair, 0, toIndex, null)).getCause().getClass());
            }
            assertEquals(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> shuffle.invoke(null, null, 0, 0, null)).getCause().getClass());
            assertEquals(IndexOutOfBoundsException.class,
                    assertThrows(InvocationTargetException.class, () -> shuffle.invoke(null, pair, -1, 0, null)).getCause().getClass());
            assertEquals(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> shuffleAll.invoke(null, empty, null)).getCause().getClass());
            assertEquals(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> shuffleAll.invoke(null, null, null)).getCause().getClass());
            assertDoesNotThrow(() -> shuffle.invoke(null, pair, 0, 1, unusedRandom));
            assertDoesNotThrow(() -> shuffle.invoke(null, null, 0, 0, unusedRandom));
            assertDoesNotThrow(() -> shuffleAll.invoke(null, empty, unusedRandom));
        }
        final List<Integer> list = Arrays.asList(1, 2);
        assertThrowsExactly(IllegalArgumentException.class, () -> N.shuffle(list, null));
        assertEquals(Arrays.asList(1, 2), list);
        final Collection<Integer> collection = new LinkedHashSet<>(list);
        assertThrowsExactly(IllegalArgumentException.class, () -> N.shuffle(collection, null));
        assertEquals(new LinkedHashSet<>(list), collection);
        // List checks size before rnd; the Collection overload checks rnd before its size no-op.
        assertDoesNotThrow(() -> N.shuffle((List<?>) null, null));
        assertDoesNotThrow(() -> N.shuffle(Collections.emptyList(), null));
        assertDoesNotThrow(() -> N.shuffle(Collections.singletonList(1), null));
        assertThrowsExactly(IllegalArgumentException.class, () -> N.shuffle((Collection<?>) null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> N.shuffle((Collection<?>) Collections.emptyList(), null));
        assertThrowsExactly(IllegalArgumentException.class, () -> N.shuffle((Collection<?>) Collections.singletonList(1), null));
        assertDoesNotThrow(() -> N.shuffle((Collection<?>) Collections.singletonList(1), unusedRandom));
        final Object[] primitiveLists = { new BooleanList(), new ByteList(), new CharList(), new DoubleList(), new FloatList(), new IntList(), new LongList() };
        for (Object primitiveList : primitiveLists) {
            final Method shuffle = primitiveList.getClass().getMethod("shuffle", java.util.Random.class);
            assertEquals(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> shuffle.invoke(primitiveList, (Object) null)).getCause().getClass());
            assertDoesNotThrow(() -> shuffle.invoke(primitiveList, unusedRandom));
        }
        assertThrowsExactly(IllegalArgumentException.class, () -> Iterables.copyInto(list, null));
        assertDoesNotThrow(() -> Iterables.copyInto(Collections.emptyList(), null));
    }

    @Test
    public void bufferValidationPrecedesReadingOrOpeningFiles() {
        final AtomicInteger reads = new AtomicInteger();
        final InputStream input = new ByteArrayInputStream(new byte[] { 1 }) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                reads.incrementAndGet();
                return super.read(b, off, len);
            }
        };
        final Reader reader = new StringReader("a");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(input, (byte[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(input, null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(reader, (char[]) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(reader, null, 0, 0));
        final File missing = new File("target/null-validation-restart/nonexistent-buffer-input");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(missing, (byte[]) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.read(missing, (char[]) null, 0, 0));
        assertEquals(0, reads.get());
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.writeField(null, null, null));
    }

    @Test
    public void beanAndReflectionArgumentsPreserveStaticReceivers() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(null, (com.landawn.abacus.util.function.Predicate<String>) p -> true));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValueIfPresent(null, "name"));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(null, "name", "value"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(new Object(), (Method) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(new Object(), (Method) null, "value"));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.invokeMethod((Method) null));
        final Method staticGetter = getClass().getMethod("staticValue");
        assertEquals("ok", ClassUtil.invokeMethod(staticGetter));
        assertEquals("ok", Beans.getPropValue(null, staticGetter));
        final Method instanceMethod = String.class.getMethod("length");
        assertThrows(NullPointerException.class, () -> ClassUtil.invokeMethod(instanceMethod));
        assertThrows(IllegalArgumentException.class, () -> Array.newInstance(String.class, (int[]) null));
        assertThrows(IllegalArgumentException.class, () -> N.newArray(String.class, (int[]) null));
        assertThrows(NullPointerException.class, () -> Array.get(null, 0));
    }

    @Test
    public void protectedExecutorBoundaryRejectsNullBeforeSubmitting() {
        final AtomicInteger submitted = new AtomicInteger();
        final AsyncExecutor executor = new AsyncExecutor(command -> submitted.incrementAndGet());
        assertThrows(IllegalArgumentException.class, () -> executor.execute((FutureTask<?>) null));
        assertEquals(0, submitted.get());
        executor.shutdown();
    }
}
