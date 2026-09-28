package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Objectory;

@Tag("unit")
class PerformancePrimitiveListSerializationRegressionTest {
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void everyPrimitiveListMatchesItsLogicalArrayAcrossFormats() throws Exception {
        Object[] arrays = { new boolean[] { true, false, true, false }, new byte[] { -1, 0, 127, 99 },
                new char[] { '\'', '\n', '&', 'X' }, new short[] { -1, 0, 32767, 99 }, new int[] { -1, 0, Integer.MAX_VALUE, 99 },
                new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE, 99 }, new float[] { -0.0f, 1.25f, Float.NaN, 99 },
                new double[] { -0.0d, 1.25d, Double.POSITIVE_INFINITY, 99 } };
        String[] names = { "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double" };
        JsonXmlSerConfig<?>[] configs = { null, JsonSerConfig.create(), JsonSerConfig.create().setCharQuotation('\'').setStringQuotation('\''),
                JsonSerConfig.create().setWriteLongAsString(true), JsonSerConfig.create().setCharQuotation((char) 0), XmlSerConfig.create() };
        for (int i = 0; i < arrays.length; i++) {
            Object backing = arrays[i];
            Class<?> listClass = Class.forName("com.landawn.abacus.util." + names[i] + "List");
            Type listType = Type.of(listClass);
            Type arrayType = Type.of(backing.getClass());
            for (int size : new int[] { 0, 3 }) {
                Object list = listClass.getMethod("of", backing.getClass(), int.class).invoke(null, backing, size);
                Object logical = Array.newInstance(backing.getClass().getComponentType(), size);
                System.arraycopy(backing, 0, logical, 0, size);
                assertEquals(arrayType.stringOf(logical), listType.stringOf(list), names[i]);
                StringBuilder expected = new StringBuilder(), actual = new StringBuilder();
                arrayType.appendTo(expected, logical);
                listType.appendTo(actual, list);
                assertEquals(expected.toString(), actual.toString(), names[i]);
                for (JsonXmlSerConfig<?> config : configs) {
                    for (boolean xml : new boolean[] { false, true }) {
                        assertEquals(render(arrayType, logical, config, xml), render(listType, list, config, xml), names[i]);
                        assertEquals(render(arrayType, null, config, xml), render(listType, null, config, xml), names[i]);
                    }
                }
            }
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void customArrayTypesReceiveLogicalSnapshotsForEveryPrimitiveList() throws Exception {
        Object[] arrays = { new boolean[] { true, false, true, false }, new byte[] { 1, 2, 3, 4 },
                new char[] { 'a', 'b', 'c', 'd' }, new short[] { 1, 2, 3, 4 }, new int[] { 1, 2, 3, 4 },
                new long[] { 1, 2, 3, 4 }, new float[] { 1, 2, 3, 4 }, new double[] { 1, 2, 3, 4 } };
        String[] names = { "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double" };
        for (int i = 0; i < arrays.length; i++) {
            Object backing = arrays[i];
            Object logical = Array.newInstance(backing.getClass().getComponentType(), 3);
            System.arraycopy(backing, 0, logical, 0, 3);
            Type arrayType = Type.of(backing.getClass());
            Class<?> listClass = Class.forName("com.landawn.abacus.util." + names[i] + "List");
            Object list = listClass.getMethod("of", backing.getClass(), int.class).invoke(null, backing, 3);
            Class<?> typeClass = Class.forName("com.landawn.abacus.type.Primitive" + names[i] + "ListType");
            Type listType = (Type) typeClass.getDeclaredConstructor().newInstance();
            AtomicInteger callbacks = new AtomicInteger();
            Type custom = (Type) Proxy.newProxyInstance(Type.class.getClassLoader(), new Class<?>[] { Type.class }, (proxy, method, args) -> {
                Object supplied = args[method.getName().equals("stringOf") ? 0 : 1];
                assertNotSame(backing, supplied, "Custom type must not receive the mutable list backing array");
                assertEquals(3, Array.getLength(supplied), "Custom type must receive only logical elements");
                callbacks.incrementAndGet();
                Array.set(backing, 2, Array.get(backing, 3)); // Reentrant mutation must not change the supplied snapshot.
                try { return method.invoke(arrayType, args); } catch (InvocationTargetException failure) { throw failure.getCause(); }
            });
            // Inject the constructor-cached dependency into a fresh handler without changing the global type registry.
            Field arrayTypeField = typeClass.getDeclaredField("arrayType");
            arrayTypeField.setAccessible(true);
            arrayTypeField.set(listType, custom);
            assertEquals(arrayType.stringOf(logical), listType.stringOf(list), names[i]);
            System.arraycopy(logical, 0, backing, 0, 3);
            StringBuilder expected = new StringBuilder(), actual = new StringBuilder();
            arrayType.appendTo(expected, logical);
            listType.appendTo(actual, list);
            assertEquals(expected.toString(), actual.toString(), names[i]);
            System.arraycopy(logical, 0, backing, 0, 3);
            assertEquals(render(arrayType, logical, null, false), render(listType, list, null, false), names[i]);
            assertEquals(3, callbacks.get(), names[i]);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String render(Type type, Object value, JsonXmlSerConfig<?> config, boolean xml) throws Exception {
        CharacterWriter writer = xml ? Objectory.createBufferedXmlWriter() : Objectory.createBufferedJsonWriter();
        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            if (xml) Objectory.recycle((com.landawn.abacus.util.BufferedXmlWriter) writer);
            else Objectory.recycle((BufferedJsonWriter) writer);
        }
    }

    @Test
    void largeIntListSerializationDoesNotAllocateACopy() throws Exception {
        allocationBean(); // Skip consistently if this VM cannot report thread allocation.
        // Other type tests mock BufferedJsonWriter. Mockito's inline instrumentation remains
        // installed and adds dispatch allocations to real writers, so measure in a fresh VM.
        String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path output = Files.createTempFile("primitive-list-allocation-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(executable, "-cp", classpath, IntListAllocationProcess.class.getName())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "allocation probe did not terminate");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            Files.deleteIfExists(output);
        }
    }

    public static final class IntListAllocationProcess {
        public static void main(String[] args) throws Exception {
            verifyLargeIntListSerializationAllocation();
        }
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        var bean = ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        var mx = (com.sun.management.ThreadMXBean) bean;
        org.junit.jupiter.api.Assumptions.assumeTrue(mx.isThreadAllocatedMemorySupported());
        mx.setThreadAllocatedMemoryEnabled(true);
        return mx;
    }

    private static void verifyLargeIntListSerializationAllocation() throws Exception {
        var mx = allocationBean();
        IntList values = IntList.of(new int[100_000]);
        Type<IntList> type = Type.of(IntList.class);
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(Writer.nullWriter());
        try {
            for (int i = 0; i < 8; i++) { type.serializeTo(writer, values, null); writer.flush(); }
            long start = mx.getThreadAllocatedBytes(Thread.currentThread().threadId());
            type.serializeTo(writer, values, null);
            writer.flush();
            long allocated = mx.getThreadAllocatedBytes(Thread.currentThread().threadId()) - start;
            assertTrue(allocated < 40_000, "List serialization allocated " + allocated + " bytes");
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void applicationDestinationsKeepThePreviousSnapshotWhenTheyMutateTheList() throws Exception {
        IntList small = IntList.of(1, 2, 3);
        StringBuilder appended = new StringBuilder();
        Appendable destination = new Appendable() {
            private void mutate() { small.set(2, 99); }
            public Appendable append(char value) { mutate(); appended.append(value); return this; }
            public Appendable append(CharSequence value) { mutate(); appended.append(value); return this; }
            public Appendable append(CharSequence value, int start, int end) { mutate(); appended.append(value, start, end); return this; }
        };
        Type.<IntList>of(IntList.class).appendTo(destination, small);
        assertEquals("[1, 2, 3]", appended.toString());
        assertEquals(99, small.get(2));

        int[] values = new int[100_000];
        Arrays.fill(values, 1);
        IntList large = IntList.of(values);
        String expected = Type.<int[]>of(int[].class).stringOf(values);
        StringBuilder output = new StringBuilder();
        Writer callback = new Writer() {
            public void write(char[] buffer, int offset, int length) {
                large.set(large.size() - 1, 99);
                output.append(buffer, offset, length);
            }
            public void flush() { }
            public void close() { }
        };
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(callback);
        try {
            Type.<IntList>of(IntList.class).serializeTo(writer, large, null);
            assertEquals(99, large.get(large.size() - 1), "Destination must be invoked before serialization finishes");
            writer.flush();
            assertEquals(expected, output.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void customConfigurationCallbacksKeepThePreviousSnapshot() throws Exception {
        LongList values = LongList.of(1, 2, 3);
        JsonSerConfig config = new JsonSerConfig() {
            @Override public boolean isWriteLongAsString() {
                values.set(2, 99);
                return false;
            }
        };
        assertEquals("[1, 2, 3]", render(Type.of(LongList.class), values, config, false));
        assertEquals(99, values.get(2));
    }
}
