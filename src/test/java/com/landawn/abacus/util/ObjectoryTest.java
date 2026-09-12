package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjectoryTest extends TestBase {

    @Test
    public void testCreateAndRecycleCollections() {
        List<String> list = Objectory.createList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
        Objectory.recycle(list);
        assertTrue(list.isEmpty());
        assertTrue(Objectory.createList().isEmpty());
        assertDoesNotThrow(() -> Objectory.recycle((List<?>) null));

        Set<String> set = Objectory.createSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertTrue(set.contains("test"));
        Objectory.recycle(set);
        assertTrue(set.isEmpty());
        assertTrue(Objectory.createSet().isEmpty());
        assertDoesNotThrow(() -> Objectory.recycle((Set<?>) null));

        Set<String> linkedSet = Objectory.createLinkedHashSet();
        assertNotNull(linkedSet);
        assertTrue(linkedSet.isEmpty());
        linkedSet.add("first");
        linkedSet.add("second");
        assertEquals(2, linkedSet.size());
        Objectory.recycle(linkedSet);
        assertTrue(linkedSet.isEmpty());
        assertTrue(Objectory.createLinkedHashSet().isEmpty());

        Map<String, Integer> map = Objectory.createMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("key", 100);
        assertEquals(100, map.get("key"));
        Objectory.recycle(map);
        assertTrue(map.isEmpty());
        assertTrue(Objectory.createMap().isEmpty());
        assertDoesNotThrow(() -> Objectory.recycle((Map<?, ?>) null));

        Map<String, Integer> linkedMap = Objectory.createLinkedHashMap();
        assertNotNull(linkedMap);
        assertTrue(linkedMap.isEmpty());
        linkedMap.put("key1", 1);
        linkedMap.put("key2", 2);
        assertEquals(2, linkedMap.size());
        Objectory.recycle(linkedMap);
        assertTrue(linkedMap.isEmpty());
        assertTrue(Objectory.createLinkedHashMap().isEmpty());

        for (int i = 0; i < 500; i++) {
            List<Integer> pooled = Objectory.createList();
            assertTrue(pooled.isEmpty());
            pooled.add(i);
            Objectory.recycle(pooled);
        }
    }

    @Test
    public void testCreateAndRecycleObjectArray() {
        Object[] defaultArray = Objectory.createObjectArray();
        assertNotNull(defaultArray);
        assertEquals(128, defaultArray.length);

        Object[] array = Objectory.createObjectArray(10);
        assertEquals(10, array.length);
        array[0] = "Hello";
        array[1] = 42;
        assertEquals("Hello", array[0]);
        assertEquals(42, array[1]);
        Objectory.recycle(array);
        assertNull(array[0]);
        assertNull(array[1]);
        assertNotNull(Objectory.createObjectArray(10));

        Object[] zero = Objectory.createObjectArray(0);
        assertEquals(0, zero.length);
        assertThrows(IllegalArgumentException.class, () -> Objectory.createObjectArray(-1));
        assertDoesNotThrow(() -> Objectory.recycle((Object[]) null));

        Object[] large = Objectory.createObjectArray(128 + 100);
        assertEquals(228, large.length);
        large[0] = "test";
        Objectory.recycle(large);
        assertEquals("test", large[0]);

        Object[] filled = Objectory.createObjectArray(8);
        for (int i = 0; i < filled.length; i++) {
            filled[i] = "data-" + i;
        }
        Objectory.recycle(filled);
        for (Object element : filled) {
            assertNull(element);
        }

        String[] strings = { "retained" };
        Objectory.recycle(strings);
        assertEquals("retained", strings[0]);
        Object[] pooled = Objectory.createObjectArray(1);
        assertDoesNotThrow(() -> pooled[0] = new Object());
        Objectory.recycle(pooled);
    }

    @Test
    public void testCreateAndRecycleBuffers() {
        char[] chars = Objectory.createCharArrayBuffer();
        assertEquals(Objectory.BUFFER_SIZE, chars.length);
        assertEquals(Objectory.BUFFER_SIZE, Objectory.createCharArrayBuffer(1024).length);
        int largeCapacity = Objectory.BUFFER_SIZE + 1000;
        assertEquals(largeCapacity, Objectory.createCharArrayBuffer(largeCapacity).length);
        chars[0] = 'A';
        Objectory.recycle(chars);
        assertEquals('\0', chars[0]);
        char[] largeChars = new char[Objectory.BUFFER_SIZE + 100];
        largeChars[0] = 'Z';
        assertDoesNotThrow(() -> Objectory.recycle(largeChars));
        assertEquals('Z', largeChars[0]);
        assertDoesNotThrow(() -> Objectory.recycle((char[]) null));

        byte[] bytes = Objectory.createByteArrayBuffer();
        assertEquals(Objectory.BUFFER_SIZE, bytes.length);
        assertEquals(Objectory.BUFFER_SIZE, Objectory.createByteArrayBuffer(4096).length);
        assertEquals(largeCapacity, Objectory.createByteArrayBuffer(largeCapacity).length);
        bytes[0] = 65;
        Objectory.recycle(bytes);
        assertEquals(0, bytes[0]);
        byte[] largeBytes = new byte[Objectory.BUFFER_SIZE + 100];
        largeBytes[0] = 42;
        assertDoesNotThrow(() -> Objectory.recycle(largeBytes));
        assertEquals(42, largeBytes[0]);
        assertDoesNotThrow(() -> Objectory.recycle((byte[]) null));

        StringBuilder sb = Objectory.createStringBuilder();
        assertTrue(sb.capacity() >= Objectory.BUFFER_SIZE);
        sb.append("Hello").append(" ").append("World");
        assertEquals("Hello World", sb.toString());
        Objectory.recycle(sb);
        assertEquals(0, sb.length());
        assertEquals(0, Objectory.createStringBuilder().length());
        StringBuilder sized = Objectory.createStringBuilder(100);
        assertNotNull(sized);
        assertTrue(Objectory.createStringBuilder(largeCapacity).capacity() >= largeCapacity);
        StringBuilder largeSb = new StringBuilder(Objectory.BUFFER_SIZE + 100);
        largeSb.append("test");
        assertDoesNotThrow(() -> Objectory.recycle(largeSb));
        assertEquals(4, largeSb.length());
        assertDoesNotThrow(() -> Objectory.recycle((StringBuilder) null));

        com.landawn.abacus.util.ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
        assertNotNull(baos);
        baos.write("Hello".getBytes(), 0, 5);
        assertEquals("Hello", new String(baos.toByteArray()));
        Objectory.recycle(baos);
        assertEquals(0, baos.size());
        assertEquals(0, baos.array()[0]);
        assertEquals(0, Objectory.createByteArrayOutputStream().size());
        assertNotNull(Objectory.createByteArrayOutputStream(512));
        assertNotNull(Objectory.createByteArrayOutputStream(largeCapacity));
        assertDoesNotThrow(() -> Objectory.recycle((com.landawn.abacus.util.ByteArrayOutputStream) null));

        assertThrows(IllegalArgumentException.class, () -> Objectory.createCharArrayBuffer(-1));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createByteArrayBuffer(-1));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createStringBuilder(-1));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createByteArrayOutputStream(-1));

        for (int i = 0; i < 200; i++) {
            StringBuilder extra = new StringBuilder(Objectory.BUFFER_SIZE / 2);
            extra.append("x");
            assertDoesNotThrow(() -> Objectory.recycle(extra));
        }
        assertEquals(0, Objectory.createStringBuilder().length());
    }

    @Test
    public void testCreateAndRecycleWriters() throws Exception {
        java.io.BufferedWriter pooled = Objectory.createBufferedWriter();
        assertNotNull(pooled);
        pooled.write("first");
        Objectory.recycle(pooled);
        java.io.BufferedWriter reused = Objectory.createBufferedWriter();
        reused.write("second");
        assertEquals("second", reused.toString());
        Objectory.recycle(reused);

        java.io.BufferedWriter existing = new java.io.BufferedWriter(new StringWriter());
        assertSame(existing, Objectory.createBufferedWriter(existing));

        StringWriter writer = new StringWriter();
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(writer);
        bw.write("Test");
        bw.flush();
        assertEquals("Test", writer.toString());
        Objectory.recycle(bw);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        java.io.BufferedWriter osWriter = Objectory.createBufferedWriter(os);
        osWriter.write("hello world");
        osWriter.flush();
        Objectory.recycle(osWriter);
        assertEquals("hello world", os.toString());

        ByteArrayOutputStream recycleFlush = new ByteArrayOutputStream();
        java.io.BufferedWriter recycleWriter = Objectory.createBufferedWriter(recycleFlush);
        recycleWriter.write("recycle-flush");
        Objectory.recycle(recycleWriter);
        assertEquals("recycle-flush", recycleFlush.toString(StandardCharsets.UTF_8));

        AtomicInteger flushCount = new AtomicInteger();
        StringWriter counting = new StringWriter() {
            @Override
            public void flush() {
                flushCount.incrementAndGet();
            }
        };
        java.io.BufferedWriter countingWriter = Objectory.createBufferedWriter(counting);
        countingWriter.write("already-flushed");
        countingWriter.flush();
        Objectory.recycle(countingWriter);
        assertEquals("already-flushed", counting.toString());
        assertEquals(1, flushCount.get());

        StringWriter firstTarget = new StringWriter();
        java.io.BufferedWriter first = Objectory.createBufferedWriter(firstTarget);
        first.write("first");
        Objectory.recycle(first);
        StringWriter secondTarget = new StringWriter();
        java.io.BufferedWriter second = Objectory.createBufferedWriter(secondTarget);
        second.write("second");
        second.flush();
        assertEquals("first", firstTarget.toString());
        assertEquals("second", secondTarget.toString());

        BufferedXmlWriter xml = Objectory.createBufferedXmlWriter();
        assertNotNull(xml);
        assertNotNull(Objectory.createBufferedXmlWriter(new ByteArrayOutputStream()));
        assertNotNull(Objectory.createBufferedXmlWriter(new StringWriter()));
        Objectory.recycle(xml);
        Objectory.recycle((java.io.BufferedWriter) Objectory.createBufferedXmlWriter());
        assertDoesNotThrow(() -> Objectory.recycle((BufferedXmlWriter) null));

        BufferedJsonWriter json = Objectory.createBufferedJsonWriter();
        assertNotNull(json);
        assertNotNull(Objectory.createBufferedJsonWriter(new ByteArrayOutputStream()));
        assertNotNull(Objectory.createBufferedJsonWriter(new StringWriter()));
        Objectory.recycle(json);
        Objectory.recycle((java.io.BufferedWriter) Objectory.createBufferedJsonWriter());
        assertDoesNotThrow(() -> Objectory.recycle((BufferedJsonWriter) null));

        BufferedCsvWriter csv = Objectory.createBufferedCsvWriter();
        assertNotNull(csv);
        assertNotNull(Objectory.createBufferedCsvWriter(new ByteArrayOutputStream()));
        assertNotNull(Objectory.createBufferedCsvWriter(new StringWriter()));
        Objectory.recycle(csv);
        Objectory.recycle((java.io.BufferedWriter) Objectory.createBufferedCsvWriter());
        assertDoesNotThrow(() -> Objectory.recycle((BufferedCsvWriter) null));
        assertDoesNotThrow(() -> Objectory.recycle((java.io.BufferedWriter) null));
    }

    @Test
    public void testPassThroughCoversThisClassesOwnWritersAndReaders() throws Exception {
        // Pins the createBufferedWriter(Writer)/createBufferedReader(Reader) contract: the pass-through also
        // covers this class's own pooled instances, so the result is the argument itself and NOT a second
        // handle - it must be recycled exactly once in total, or the same instance lands in the pool twice.
        // recycle(..) dispatches on the runtime type alone and cannot tell which factory produced an instance,
        // so all twelve writer factories are covered - the OutputStream/Writer overloads, and this method
        // itself, included - and all three reader factories.
        final List<Supplier<java.io.BufferedWriter>> writerFactories = List.of(Objectory::createBufferedWriter,
                () -> Objectory.createBufferedWriter(new ByteArrayOutputStream()), () -> Objectory.createBufferedWriter(new StringWriter()),
                Objectory::createBufferedJsonWriter, () -> Objectory.createBufferedJsonWriter(new ByteArrayOutputStream()),
                () -> Objectory.createBufferedJsonWriter(new StringWriter()), Objectory::createBufferedXmlWriter,
                () -> Objectory.createBufferedXmlWriter(new ByteArrayOutputStream()), () -> Objectory.createBufferedXmlWriter(new StringWriter()),
                Objectory::createBufferedCsvWriter, () -> Objectory.createBufferedCsvWriter(new ByteArrayOutputStream()),
                () -> Objectory.createBufferedCsvWriter(new StringWriter()));

        for (final Supplier<java.io.BufferedWriter> factory : writerFactories) {
            final java.io.BufferedWriter owned = factory.get();

            try {
                assertSame(owned, Objectory.createBufferedWriter((Writer) owned), owned.getClass().getName());
            } finally {
                Objectory.recycle(owned);   // exactly once in total, through the owning handle
            }
        }

        final List<Supplier<java.io.BufferedReader>> readerFactories = List.of(() -> Objectory.createBufferedReader("line"),
                () -> Objectory.createBufferedReader(new ByteArrayInputStream("line".getBytes(StandardCharsets.UTF_8))),
                () -> Objectory.createBufferedReader(new StringReader("line")));

        for (final Supplier<java.io.BufferedReader> factory : readerFactories) {
            final java.io.BufferedReader owned = factory.get();

            try {
                assertSame(owned, Objectory.createBufferedReader((Reader) owned), owned.getClass().getName());
            } finally {
                Objectory.recycle(owned);
            }
        }
    }

    @Test
    public void testRecyclingBothHandlesOfAPassThroughPoolsOneInstanceTwice() throws Exception {
        // The hazard the pass-through paragraph exists to warn about: recycling BOTH handles puts one instance
        // in the pool twice, and two later borrowers are then handed the same writer. Reproduced deliberately
        // on a drained pool - restored in the finally - so the two borrowers are known to be ours.
        final Queue<Object> pool = privatePool("bufferedWriterPool");
        final List<Object> saved = drain(pool);

        try {
            final StringWriter inner = new StringWriter();
            final java.io.BufferedWriter owner = Objectory.createBufferedWriter(inner);
            final java.io.BufferedWriter passThrough = Objectory.createBufferedWriter((Writer) owner);
            assertSame(owner, passThrough);

            owner.write("hello");
            assertEquals("", inner.toString(), "wrapping hides the content of the inner writer until a flush");

            Objectory.recycle(passThrough);
            assertEquals("hello", inner.toString(), "recycling flushes the buffer to the destination");

            Objectory.recycle(owner);   // the documented mistake: a second recycle of the same instance

            assertSame(Objectory.createBufferedWriter(), Objectory.createBufferedWriter(), "one instance was pooled twice");
        } finally {
            pool.clear();
            pool.addAll(saved);
        }
    }

    @Test
    public void testBufferSizeScalesWithTheRawHeapMegabyteCount() throws Exception {
        // Pins the documented derivation of BUFFER_SIZE: IOUtil.MAX_MEMORY_IN_MB is a megabyte count used as a
        // raw scaling number rather than as a byte count, clamped to [16 KB, 128 KB]. Asserted as the three
        // documented regimes; restating the implementation clamp expression here would only compare that one
        // line against a copy of itself.
        assertTrue(Objectory.BUFFER_SIZE >= 16 * 1024 && Objectory.BUFFER_SIZE <= 128 * 1024, "BUFFER_SIZE=" + Objectory.BUFFER_SIZE);

        if (IOUtil.MAX_MEMORY_IN_MB <= 16 * 1024) {
            // The floor binds for every heap up to and including 16 GB. Multiplying the heap figure by KB to
            // "repair" the units would pin BUFFER_SIZE at the 128 KB ceiling here and raise the retained pool
            // footprint eightfold, so this is the assertion that catches that change.
            assertEquals(16 * 1024, Objectory.BUFFER_SIZE);
        } else if (IOUtil.MAX_MEMORY_IN_MB >= 128 * 1024) {
            assertEquals(128 * 1024, Objectory.BUFFER_SIZE);   // saturated, from a 128 GB heap up
        } else {
            assertEquals(IOUtil.MAX_MEMORY_IN_MB, Objectory.BUFFER_SIZE);   // one byte of buffer per megabyte of heap
        }

        // "Only buffers of exactly this length are eligible for pooling" - behaviour, not arithmetic. Checked on
        // a drained pool, which is restored in the finally.
        final Queue<Object> charBuffers = privatePool("charArrayBufferPool");
        final List<Object> saved = drain(charBuffers);

        try {
            Objectory.recycle(new char[Objectory.BUFFER_SIZE - 1]);
            Objectory.recycle(new char[Objectory.BUFFER_SIZE + 1]);
            assertEquals(0, charBuffers.size(), "a buffer of any other length must not be pooled");

            final char[] exact = new char[Objectory.BUFFER_SIZE];
            Objectory.recycle(exact);
            assertEquals(1, charBuffers.size());
            assertSame(exact, Objectory.createCharArrayBuffer());
        } finally {
            charBuffers.clear();
            charBuffers.addAll(saved);
        }
    }

    /** Returns one of the private pools of {@code Objectory}, for a test that must control its exact contents. */
    @SuppressWarnings("unchecked")
    private static Queue<Object> privatePool(final String fieldName) throws Exception {
        final Field field = Objectory.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        return (Queue<Object>) field.get(null);
    }

    private static List<Object> drain(final Queue<Object> pool) {
        final List<Object> drained = new ArrayList<>();

        for (Object pooled = pool.poll(); pooled != null; pooled = pool.poll()) {
            drained.add(pooled);
        }

        return drained;
    }

    @Test
    public void testCreateAndRecycleReaders() throws Exception {
        java.io.BufferedReader existing = new java.io.BufferedReader(new StringReader("test"));
        assertSame(existing, Objectory.createBufferedReader(existing));

        java.io.BufferedReader fromString = Objectory.createBufferedReader("Line 1\nLine 2\nLine 3");
        assertEquals("Line 1", fromString.readLine());
        Objectory.recycle(fromString);

        java.io.BufferedReader fromStream = Objectory.createBufferedReader(new ByteArrayInputStream("Test input".getBytes()));
        assertEquals("Test input", fromStream.readLine());
        Objectory.recycle(fromStream);

        java.io.BufferedReader fromReader = Objectory.createBufferedReader(new StringReader("Test content"));
        assertEquals("Test content", fromReader.readLine());
        Objectory.recycle(fromReader);

        java.io.BufferedReader first = Objectory.createBufferedReader(new StringReader("first"));
        assertEquals("first", first.readLine());
        Objectory.recycle(first);
        java.io.BufferedReader second = Objectory.createBufferedReader(new StringReader("second"));
        assertEquals("second", second.readLine());
        assertNull(second.readLine());
        Objectory.recycle(second);

        java.io.BufferedReader regular = new java.io.BufferedReader(new StringReader("test"));
        Objectory.recycle(regular);
        assertDoesNotThrow(() -> Objectory.recycle((java.io.BufferedReader) null));
    }

    @Test
    public void testRecycleIgnoresForeignImplementations() {
        List<String> list = Collections.singletonList("value");
        Set<String> set = Collections.singleton("value");
        Map<String, String> map = Collections.singletonMap("key", "value");
        assertDoesNotThrow(() -> Objectory.recycle(list));
        assertDoesNotThrow(() -> Objectory.recycle(set));
        assertDoesNotThrow(() -> Objectory.recycle(map));
        assertEquals(Collections.singletonList("value"), list);
        assertEquals(Collections.singleton("value"), set);
        assertEquals(Collections.singletonMap("key", "value"), map);

        Set<Object> setWithUnsupportedSize = new HashSet<>() {
            @Override
            public int size() {
                throw new UnsupportedOperationException("size must not be queried");
            }
        };
        Map<Object, Object> mapWithUnsupportedSize = new HashMap<>() {
            @Override
            public int size() {
                throw new UnsupportedOperationException("size must not be queried");
            }
        };
        assertDoesNotThrow(() -> Objectory.recycle(setWithUnsupportedSize));
        assertDoesNotThrow(() -> Objectory.recycle(mapWithUnsupportedSize));
    }

    @Test
    public void testRecycleByteArrayOutputStreamWipesWholeBackingArray() {
        // Drain the pool so the recycle below is guaranteed to be accepted (and therefore wiped).
        for (int i = 0; i < 64; i++) {
            Objectory.createByteArrayOutputStream();
        }

        com.landawn.abacus.util.ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
        baos.write("secret".getBytes(StandardCharsets.UTF_8), 0, 6);

        byte[] backing = baos.array();
        // reset() only zeroes the count; the written bytes are still in the backing array.
        baos.reset();
        assertEquals(0, baos.size());
        assertEquals((byte) 's', backing[0]);

        Objectory.recycle(baos);

        int firstNonZero = -1;

        for (int i = 0; i < backing.length; i++) {
            if (backing[i] != 0) {
                firstNonZero = i;
                break;
            }
        }

        assertEquals(-1, firstNonZero, "recycled backing array still holds data");
    }

    @Test
    public void testWriterAndReaderFactoriesRejectNullBeforePolling() {
        Objectory.recycle(Objectory.createBufferedWriter());
        Objectory.recycle(Objectory.createBufferedXmlWriter());
        Objectory.recycle(Objectory.createBufferedJsonWriter());
        Objectory.recycle(Objectory.createBufferedCsvWriter());
        Objectory.recycle(Objectory.createBufferedReader("pooled"));

        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedWriter((OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedWriter((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedXmlWriter((OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedXmlWriter((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedJsonWriter((OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedJsonWriter((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedCsvWriter((OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedCsvWriter((java.io.Writer) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedReader((String) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedReader((java.io.InputStream) null));
        assertThrows(IllegalArgumentException.class, () -> Objectory.createBufferedReader((java.io.Reader) null));
    }
}
