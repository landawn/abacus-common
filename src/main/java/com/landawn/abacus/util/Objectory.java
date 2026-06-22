/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A factory class for creating and recycling commonly used objects to reduce memory allocation overhead.
 * This class maintains object pools for various data structures and buffers, allowing for efficient
 * reuse of objects in performance-critical applications.
 *
 * <p>The Objectory pattern helps to:</p>
 * <ul>
 *   <li>Reduce garbage collection pressure by reusing objects</li>
 *   <li>Improve performance in scenarios with frequent object creation/destruction</li>
 *   <li>Provide pre-sized buffers for common I/O operations</li>
 * </ul>
 *
 * <p><b>Important:</b> Objects obtained from Objectory should be properly recycled after use
 * to maintain pool efficiency. Failing to recycle objects will result in new allocations.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create and use a StringBuilder
 * StringBuilder sb = Objectory.createStringBuilder();
 * try {
 *     sb.append("Hello").append(" ").append("World");
 *     return sb.toString();
 * } finally {
 *     Objectory.recycle(sb);   // returns sb to the pool for reuse
 * }
 * }</pre>
 *
 * <p><b>Note:</b> This is an internal utility class ({@link Internal @Internal},
 * {@link Beta @Beta}) and is not intended for use outside this library. Several
 * {@code create*}/{@code recycle} methods are marked {@code @deprecated} solely
 * to discourage external use.</p>
 *
 * @see java.io.BufferedWriter
 * @see java.io.BufferedReader
 */
@Internal
@Beta
public final class Objectory {

    private static final Logger logger = LoggerFactory.getLogger(Objectory.class);

    private static final AtomicInteger created = new AtomicInteger();

    static final int KB = 1024;

    static final int BUFFER_SIZE = Math.min(Math.max(IOUtil.MAX_MEMORY_IN_MB, KB * 16), 128 * KB);

    private static final int POOL_SIZE_FOR_COLLECTION = 256;

    private static final int POOL_SIZE_FOR_BUFFER = 64;

    private static final int POOLABLE_SIZE = 8192;

    private static final int POOLABLE_ARRAY_LENGTH = 128;

    private static final int MAX_ARRAY_POOL_SIZE = IOUtil.IS_PLATFORM_ANDROID ? 8 : 64;

    private static final int MAX_ARRAY_LENGTH = IOUtil.IS_PLATFORM_ANDROID ? 128 : 1024;

    @SuppressWarnings("unchecked")
    private static final Queue<Object[]>[] objectArrayPool = new Queue[POOLABLE_SIZE + 1];

    private static final Queue<List<?>> listPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_COLLECTION);

    private static final Queue<Set<?>> setPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_COLLECTION);

    private static final Queue<Set<?>> linkedHashSetPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_COLLECTION);

    private static final Queue<Map<?, ?>> mapPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_COLLECTION);

    private static final Queue<Map<?, ?>> linkedHashMapPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_COLLECTION);

    private static final Queue<StringBuilder> stringBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<char[]> charArrayBufferPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<byte[]> byteArrayBufferPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<ByteArrayOutputStream> byteArrayOutputStreamPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedWriter> bufferedWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedXmlWriter> bufferedXmlWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedJsonWriter> bufferedJsonWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedCsvWriter> bufferedCsvWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);
    private static final Queue<BufferedCsvWriter> backSlashBufferedCsvWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedReader> bufferedReaderPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private Objectory() {
        // Utility class - private constructor to prevent instantiation
    }

    /**
     * Creates or retrieves a List from the object pool.
     * The returned list is empty and ready for use.
     *
     * <p>After use, the list should be recycled using {@link #recycle(List)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Objectory.createList();
     * try {
     *     list.add("a");
     * } finally {
     *     Objectory.recycle(list);
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the list
     * @return an empty {@link ArrayList} obtained from the pool, or a new
     *         instance if the pool is empty
     * @deprecated for internal use only
     * @see #recycle(List)
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> List<T> createList() {
        final List<T> list = (List<T>) listPool.poll();

        return (list == null) ? new ArrayList<>() : list;
    }

    /**
     * Creates or retrieves a Set from the object pool.
     * The returned set is empty and ready for use.
     *
     * <p>After use, the set should be recycled using {@link #recycle(Set)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> set = Objectory.createSet();
     * try {
     *     set.add("a");
     * } finally {
     *     Objectory.recycle(set);
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the set
     * @return an empty {@link java.util.HashSet} obtained from the pool, or a
     *         new instance if the pool is empty
     * @deprecated for internal use only
     * @see #recycle(Set)
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createSet() {
        final Set<T> set = (Set<T>) setPool.poll();

        return (set == null) ? N.newHashSet() : set;
    }

    /**
     * Creates or retrieves a LinkedHashSet from the object pool.
     * The returned set is empty and ready for use, maintaining insertion order.
     *
     * <p>After use, the set should be recycled using {@link #recycle(Set)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> set = Objectory.createLinkedHashSet();
     * try {
     *     set.add("a");
     * } finally {
     *     Objectory.recycle(set);
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the set
     * @return an empty {@link LinkedHashSet} obtained from the pool, or a new
     *         instance if the pool is empty
     * @deprecated for internal use only
     * @see #recycle(Set)
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createLinkedHashSet() {
        final Set<T> set = (Set<T>) linkedHashSetPool.poll();

        return (set == null) ? N.newLinkedHashSet() : set;
    }

    /**
     * Creates or retrieves a Map from the object pool.
     * The returned map is empty and ready for use.
     *
     * <p>After use, the map should be recycled using {@link #recycle(Map)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = Objectory.createMap();
     * try {
     *     map.put("k", "v");
     * } finally {
     *     Objectory.recycle(map);
     * }
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return an empty {@link java.util.HashMap} obtained from the pool, or a
     *         new instance if the pool is empty
     * @deprecated for internal use only
     * @see #recycle(Map)
     */
    @Deprecated
    public static <K, V> Map<K, V> createMap() {
        final Map<K, V> map = (Map<K, V>) mapPool.poll();

        return (map == null) ? N.newHashMap() : map;
    }

    /**
     * Creates or retrieves a LinkedHashMap from the object pool.
     * The returned map is empty and ready for use, maintaining insertion order.
     *
     * <p>After use, the map should be recycled using {@link #recycle(Map)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = Objectory.createLinkedHashMap();
     * try {
     *     map.put("k", "v");
     * } finally {
     *     Objectory.recycle(map);
     * }
     * }</pre>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return an empty {@link LinkedHashMap} obtained from the pool, or a new
     *         instance if the pool is empty
     * @deprecated for internal use only
     * @see #recycle(Map)
     */
    @Deprecated
    public static <K, V> Map<K, V> createLinkedHashMap() {
        final Map<K, V> linkedHashMap = (Map<K, V>) linkedHashMapPool.poll();

        return (linkedHashMap == null) ? N.newLinkedHashMap() : linkedHashMap;
    }

    /**
     * Creates or retrieves an {@code Object[]} of the default poolable length
     * ({@code 128}).
     *
     * <p>After use, the array should be recycled using {@link #recycle(Object[])}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] array = Objectory.createObjectArray();
     * try {
     *     array[0] = "Hello";
     *     array[1] = 42;
     * } finally {
     *     Objectory.recycle(array);
     * }
     * }</pre>
     *
     * @return an {@code Object[]} of length {@code 128} obtained from the pool,
     *         or a new instance if the pool is empty
     * @see #createObjectArray(int)
     */
    public static Object[] createObjectArray() {
        return createObjectArray(POOLABLE_ARRAY_LENGTH);
    }

    /**
     * Creates or retrieves an {@code Object[]} of the specified size.
     * Arrays with size greater than {@code 128} are always newly allocated and
     * are not pooled.
     *
     * <p>After use, the array should be recycled using {@link #recycle(Object[])}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] array = Objectory.createObjectArray(10);
     * try {
     *     // Use the array
     *     array[0] = "Hello";
     *     array[1] = 42;
     * } finally {
     *     Objectory.recycle(array);
     * }
     * }</pre>
     *
     * @param size the desired size of the array
     * @return an {@code Object[]} of the specified size
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public static Object[] createObjectArray(final int size) {
        if (size < 0) {
            throw new IllegalArgumentException("The specified array size cannot be negative: " + size);
        }

        if (size > POOLABLE_ARRAY_LENGTH) {
            return new Object[size];
        }

        Object[] objArray = null;

        synchronized (objectArrayPool) {
            final Queue<Object[]> arrayQueue = objectArrayPool[size];

            if (N.notEmpty(arrayQueue)) {
                objArray = arrayQueue.poll();
            }

            if (objArray == null) {
                // logCreated("createObjectArray");
                objArray = new Object[size];
            }
        }

        return objArray;
    }

    /**
     * Creates or retrieves a {@code char[]} buffer of the default buffer size.
     * The returned buffer's length is the internal default buffer size.
     *
     * <p>After use, the buffer should be recycled using {@link #recycle(char[])}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] buffer = Objectory.createCharArrayBuffer();
     * try {
     *     Reader reader = new FileReader("file.txt");
     *     int charsRead = reader.read(buffer);
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @return a {@code char[]} buffer obtained from the pool, or a new instance
     *         if the pool is empty
     * @see #createCharArrayBuffer(int)
     */
    public static char[] createCharArrayBuffer() {
        return createCharArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a {@code char[]} buffer of at least the specified
     * capacity. If {@code capacity} exceeds the internal default buffer size, a
     * new array of exactly {@code capacity} is allocated (and is not pooled).
     * Otherwise a pooled buffer is returned whose length is the default buffer
     * size, which may be <i>larger</i> than the requested {@code capacity}.
     *
     * <p>This is useful for character-based I/O operations where a temporary buffer is needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] buffer = Objectory.createCharArrayBuffer(1024);
     * try {
     *     Reader reader = new FileReader("file.txt");
     *     int charsRead = reader.read(buffer);
     *     // Process the characters
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @param capacity the minimum desired capacity of the buffer
     * @return a {@code char[]} buffer at least {@code capacity} long
     */
    public static char[] createCharArrayBuffer(final int capacity) {
        if (capacity > BUFFER_SIZE) {
            // logCreated("createCharArrayBuffer");

            return new char[capacity];
        }

        char[] cbuf = charArrayBufferPool.poll();

        if (cbuf == null) {
            logCreated("createCharArrayBuffer");

            cbuf = new char[BUFFER_SIZE];
        }

        return cbuf;
    }

    /**
     * Creates or retrieves a {@code byte[]} buffer of the default buffer size.
     * The returned buffer's length is the internal default buffer size.
     *
     * <p>After use, the buffer should be recycled using {@link #recycle(byte[])}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = Objectory.createByteArrayBuffer();
     * try {
     *     InputStream input = new FileInputStream("file.bin");
     *     int bytesRead = input.read(buffer);
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @return a {@code byte[]} buffer obtained from the pool, or a new instance
     *         if the pool is empty
     * @see #createByteArrayBuffer(int)
     */
    public static byte[] createByteArrayBuffer() {
        return createByteArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a {@code byte[]} buffer of at least the specified
     * capacity. If {@code capacity} exceeds the internal default buffer size, a
     * new array of exactly {@code capacity} is allocated (and is not pooled).
     * Otherwise a pooled buffer is returned whose length is the default buffer
     * size, which may be <i>larger</i> than the requested {@code capacity}.
     *
     * <p>This is useful for binary I/O operations where a temporary buffer is needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = Objectory.createByteArrayBuffer(4096);
     * try {
     *     InputStream input = new FileInputStream("file.bin");
     *     int bytesRead = input.read(buffer);
     *     // Process the bytes
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @param capacity the minimum desired capacity of the buffer
     * @return a {@code byte[]} buffer at least {@code capacity} long
     */
    public static byte[] createByteArrayBuffer(final int capacity) {
        if (capacity > BUFFER_SIZE) {
            // logCreated("createByteArrayBuffer");

            return new byte[capacity];
        }

        byte[] bbuf = byteArrayBufferPool.poll();

        if (bbuf == null) {
            logCreated("createByteArrayBuffer");

            bbuf = new byte[BUFFER_SIZE];
        }

        return bbuf;
    }

    /**
     * Creates or retrieves a {@link StringBuilder} with the default initial
     * capacity (the internal default buffer size).
     *
     * <p>After use, the {@code StringBuilder} should be recycled using
     * {@link #recycle(StringBuilder)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = Objectory.createStringBuilder();
     * try {
     *     sb.append("Hello").append(" ").append("World");
     *     return sb.toString();
     * } finally {
     *     Objectory.recycle(sb);
     * }
     * }</pre>
     *
     * @return a {@code StringBuilder} obtained from the pool, or a new instance
     *         if the pool is empty
     * @see #createStringBuilder(int)
     */
    public static StringBuilder createStringBuilder() {
        return createStringBuilder(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a {@link StringBuilder} suitable for the specified
     * initial capacity. If {@code initCapacity} exceeds the internal default
     * buffer size, a new {@code StringBuilder} of that capacity is allocated
     * (and is not pooled). Otherwise a pooled {@code StringBuilder} is returned,
     * whose capacity is typically the default buffer size but may be smaller if
     * a smaller builder was recycled into the pool (it grows automatically as
     * needed).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = Objectory.createStringBuilder(100);
     * try {
     *     sb.append("Hello");
     *     sb.append(" ");
     *     sb.append("World");
     *     return sb.toString();
     * } finally {
     *     Objectory.recycle(sb);
     * }
     * }</pre>
     *
     * @param initCapacity the desired initial capacity
     * @return an empty {@code StringBuilder}; freshly allocated with capacity
     *         {@code initCapacity} when that exceeds the default buffer size,
     *         otherwise obtained from the pool
     */
    public static StringBuilder createStringBuilder(final int initCapacity) {
        if (initCapacity > BUFFER_SIZE) {
            return new StringBuilder(initCapacity);
        }

        StringBuilder sb = stringBuilderPool.poll();

        if (sb == null) {
            logCreated("createStringBuilder");

            sb = new StringBuilder(BUFFER_SIZE);
        }

        return sb;
    }

    /**
     * Creates or retrieves a {@link ByteArrayOutputStream} with the default
     * initial capacity (the internal default buffer size).
     *
     * <p>After use, the stream should be recycled using {@link #recycle(ByteArrayOutputStream)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
     * try {
     *     baos.write("Hello".getBytes());
     *     byte[] result = baos.toByteArray();
     * } finally {
     *     Objectory.recycle(baos);
     * }
     * }</pre>
     *
     * @return a {@code ByteArrayOutputStream} obtained from the pool, or a new
     *         instance if the pool is empty
     * @see #createByteArrayOutputStream(int)
     */
    public static ByteArrayOutputStream createByteArrayOutputStream() {
        return createByteArrayOutputStream(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a {@link ByteArrayOutputStream} suitable for the
     * specified initial capacity. If {@code initCapacity} exceeds the internal
     * default buffer size, a new stream of that capacity is allocated (and is
     * not pooled). Otherwise a pooled stream is returned, whose capacity is
     * typically the default buffer size (it grows automatically as needed).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream(4096);
     * try {
     *     baos.write("Hello".getBytes());
     *     byte[] result = baos.toByteArray();
     * } finally {
     *     Objectory.recycle(baos);
     * }
     * }</pre>
     *
     * @param initCapacity the desired initial capacity
     * @return an empty {@code ByteArrayOutputStream}; freshly allocated with capacity
     *         {@code initCapacity} when that exceeds the default buffer size,
     *         otherwise obtained from the pool
     */
    public static ByteArrayOutputStream createByteArrayOutputStream(final int initCapacity) {
        if (initCapacity > BUFFER_SIZE) {
            // logCreated("createByteArrayOutputStream");

            return new ByteArrayOutputStream(initCapacity);
        }

        ByteArrayOutputStream os = byteArrayOutputStreamPool.poll();

        if (os == null) {
            logCreated("createByteArrayOutputStream");

            os = new ByteArrayOutputStream(BUFFER_SIZE);
        }

        return os;
    }

    /**
     * Creates or retrieves a {@link BufferedWriter} in internal buffer mode
     * (no underlying target writer). Written content is accumulated in memory
     * and can be retrieved via {@code toString()}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.io.BufferedWriter bw = Objectory.createBufferedWriter();
     * try {
     *     bw.write("Hello World");
     *     String result = bw.toString();
     * } finally {
     *     Objectory.recycle(bw);
     * }
     * }</pre>
     *
     * @return a {@code BufferedWriter} obtained from the pool, or a new instance
     *         if the pool is empty
     */
    public static java.io.BufferedWriter createBufferedWriter() {
        BufferedWriter bw = bufferedWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedWriter"); //NOSONAR

            bw = new BufferedWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedWriter} that writes to the specified
     * {@link OutputStream}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     java.io.BufferedWriter bw = Objectory.createBufferedWriter(os);
     *     try {
     *         bw.write("Hello World");
     *         bw.newLine();
     *     } finally {
     *         Objectory.recycle(bw);
     *     }
     * }
     * }</pre>
     *
     * @param os the {@code OutputStream} to write to
     * @return a {@code BufferedWriter} writing to the specified stream
     */
    public static java.io.BufferedWriter createBufferedWriter(final OutputStream os) {
        BufferedWriter bw = bufferedWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedWriter");

            bw = new BufferedWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedWriter} that writes to the specified
     * {@link Writer}. If {@code writer} is already a
     * {@link java.io.BufferedWriter}, it is returned as-is (and will not be
     * pooled by {@link #recycle(java.io.BufferedWriter)}).
     *
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer fileWriter = new FileWriter("output.txt")) {
     *     java.io.BufferedWriter bw = Objectory.createBufferedWriter(fileWriter);
     *     try {
     *         bw.write("Hello World");
     *         bw.newLine();
     *     } finally {
     *         Objectory.recycle(bw);
     *     }
     * }
     * }</pre>
     *
     * @param writer the {@code Writer} to write to
     * @return a {@code BufferedWriter} writing to the specified writer
     */
    public static java.io.BufferedWriter createBufferedWriter(final Writer writer) {
        if (writer instanceof java.io.BufferedWriter) {
            return (java.io.BufferedWriter) writer;
        }

        BufferedWriter bw = bufferedWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedWriter");

            bw = new BufferedWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedXmlWriter} in internal buffer mode
     * (no underlying target writer). Written content is accumulated in memory
     * and can be retrieved via {@code toString()}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXmlWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
     * try {
     *     writer.write("<root>");
     *     writer.writeCharacter("Hello");
     *     writer.write("</root>");
     *     String xml = writer.toString();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @return a {@code BufferedXmlWriter} obtained from the pool, or a new
     *         instance if the pool is empty
     */
    public static BufferedXmlWriter createBufferedXmlWriter() {
        BufferedXmlWriter bw = bufferedXmlWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXmlWriter"); //NOSONAR

            bw = new BufferedXmlWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedXmlWriter} that writes to the
     * specified {@link OutputStream}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXmlWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the {@code OutputStream} to write to
     * @return a {@code BufferedXmlWriter} writing to the specified stream
     */
    public static BufferedXmlWriter createBufferedXmlWriter(final OutputStream os) {
        BufferedXmlWriter bw = bufferedXmlWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXmlWriter");

            bw = new BufferedXmlWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedXmlWriter} that writes to the
     * specified {@link Writer}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXmlWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer fileWriter = new FileWriter("output.xml")) {
     *     BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(fileWriter);
     *     try {
     *         writer.write("Hello");
     *     } finally {
     *         Objectory.recycle(writer);
     *     }
     * }
     * }</pre>
     *
     * @param writer the {@code Writer} to write to
     * @return a {@code BufferedXmlWriter} writing to the specified writer
     */
    public static BufferedXmlWriter createBufferedXmlWriter(final Writer writer) {
        BufferedXmlWriter bw = bufferedXmlWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXmlWriter");

            bw = new BufferedXmlWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedJsonWriter} in internal buffer mode
     * (no underlying target writer). Written content is accumulated in memory
     * and can be retrieved via {@code toString()}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJsonWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * try {
     *     writer.write("{\"name\": \"Hello\"}");
     *     String json = writer.toString();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @return a {@code BufferedJsonWriter} obtained from the pool, or a new
     *         instance if the pool is empty
     */
    public static BufferedJsonWriter createBufferedJsonWriter() {
        BufferedJsonWriter bw = bufferedJsonWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJsonWriter"); //NOSONAR

            bw = new BufferedJsonWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedJsonWriter} that writes to the
     * specified {@link OutputStream}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJsonWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the {@code OutputStream} to write to
     * @return a {@code BufferedJsonWriter} writing to the specified stream
     */
    public static BufferedJsonWriter createBufferedJsonWriter(final OutputStream os) {
        BufferedJsonWriter bw = bufferedJsonWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJsonWriter");

            bw = new BufferedJsonWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedJsonWriter} that writes to the
     * specified {@link Writer}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJsonWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer fileWriter = new FileWriter("output.json")) {
     *     BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(fileWriter);
     *     try {
     *         writer.write("Hello");
     *     } finally {
     *         Objectory.recycle(writer);
     *     }
     * }
     * }</pre>
     *
     * @param writer the {@code Writer} to write to
     * @return a {@code BufferedJsonWriter} writing to the specified writer
     */
    public static BufferedJsonWriter createBufferedJsonWriter(final Writer writer) {
        BufferedJsonWriter bw = bufferedJsonWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJsonWriter");

            bw = new BufferedJsonWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedCsvWriter} in internal buffer mode
     * (no underlying target writer). Written content is accumulated in memory
     * and can be retrieved via {@code toString()}. The pooled instance is drawn
     * from either the backslash-escape pool or the default pool depending on
     * the current {@code CsvUtil} backslash-escape setting.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCsvWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = Objectory.createBufferedCsvWriter();
     * try {
     *     writer.write("Hello");
     *     String csv = writer.toString();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @return a {@code BufferedCsvWriter} obtained from the pool, or a new
     *         instance if the pool is empty
     */
    public static BufferedCsvWriter createBufferedCsvWriter() {
        BufferedCsvWriter bw = CsvUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCsvWriterPool.poll() : bufferedCsvWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCsvWriter"); //NOSONAR

            bw = new BufferedCsvWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedCsvWriter} that writes to the
     * specified {@link OutputStream}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCsvWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = Objectory.createBufferedCsvWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the {@code OutputStream} to write to
     * @return a {@code BufferedCsvWriter} writing to the specified stream
     */
    public static BufferedCsvWriter createBufferedCsvWriter(final OutputStream os) {
        BufferedCsvWriter bw = CsvUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCsvWriterPool.poll() : bufferedCsvWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCsvWriter");

            bw = new BufferedCsvWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedCsvWriter} that writes to the
     * specified {@link Writer}.
     *
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCsvWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer fileWriter = new FileWriter("output.csv")) {
     *     BufferedCsvWriter writer = Objectory.createBufferedCsvWriter(fileWriter);
     *     try {
     *         writer.write("Hello");
     *     } finally {
     *         Objectory.recycle(writer);
     *     }
     * }
     * }</pre>
     *
     * @param writer the {@code Writer} to write to
     * @return a {@code BufferedCsvWriter} writing to the specified writer
     */
    public static BufferedCsvWriter createBufferedCsvWriter(final Writer writer) {
        BufferedCsvWriter bw = CsvUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCsvWriterPool.poll() : bufferedCsvWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCsvWriter");

            bw = new BufferedCsvWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a {@link BufferedReader} that reads from the
     * specified {@code String} (via an internal {@link java.io.StringReader}).
     *
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\nLine 3";
     * java.io.BufferedReader reader = Objectory.createBufferedReader(text);
     * try {
     *     String line;
     *     while ((line = reader.readLine()) != null) {
     *         System.out.println(line);
     *     }
     * } finally {
     *     Objectory.recycle(reader);
     * }
     * }</pre>
     *
     * @param str the {@code String} to read from
     * @return a {@code BufferedReader} reading from the specified string
     */
    public static java.io.BufferedReader createBufferedReader(final String str) {
        return createBufferedReader(new java.io.StringReader(str));
    }

    /**
     * Creates or retrieves a {@link BufferedReader} that reads from the
     * specified {@link InputStream}.
     *
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("input.txt")) {
     *     java.io.BufferedReader reader = Objectory.createBufferedReader(is);
     *     try {
     *         String line;
     *         while ((line = reader.readLine()) != null) {
     *             System.out.println(line);
     *         }
     *     } finally {
     *         Objectory.recycle(reader);
     *     }
     * }
     * }</pre>
     *
     * @param is the {@code InputStream} to read from
     * @return a {@code BufferedReader} reading from the specified stream
     */
    public static java.io.BufferedReader createBufferedReader(final InputStream is) {
        final BufferedReader br = bufferedReaderPool.poll();

        if (br == null) {
            logCreated("createBufferedReader");

            return new BufferedReader(is);
        } else {
            br.reinit(is);

            return br;
        }
    }

    /**
     * Creates or retrieves a {@link BufferedReader} that reads from the
     * specified {@link Reader}. If {@code reader} is already a
     * {@link java.io.BufferedReader}, it is returned as-is (and will not be
     * pooled by {@link #recycle(java.io.BufferedReader)}).
     *
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader fileReader = new FileReader("input.txt")) {
     *     java.io.BufferedReader br = Objectory.createBufferedReader(fileReader);
     *     try {
     *         String line = br.readLine();
     *         // Process line
     *     } finally {
     *         Objectory.recycle(br);
     *     }
     * }
     * }</pre>
     *
     * @param reader the {@code Reader} to read from
     * @return a {@code BufferedReader} reading from the specified reader
     */
    public static java.io.BufferedReader createBufferedReader(final Reader reader) {
        if (reader instanceof java.io.BufferedReader) {
            return (java.io.BufferedReader) reader;
        }

        final BufferedReader br = bufferedReaderPool.poll();

        if (br == null) {
            logCreated("createBufferedReader");

            return new BufferedReader(reader);
        } else {
            br.reinit(reader);

            return br;
        }
    }

    /**
     * Logs creation of a new object when the pool is exhausted.
     * This helps monitor pool efficiency and detect potential memory issues.
     *
     * @param methodName the name of the method creating the object
     */
    private static void logCreated(final String methodName) {
        if (logger.isWarnEnabled() && (created.incrementAndGet() % 1000) == 0) {
            logger.warn("The " + created.get() + "th cacheable object is created by " + methodName,
                    new RuntimeException("No error. It's only for debug to print stack trace:"));
        }
    }

    /**
     * Returns a {@code List} to the object pool for reuse.
     * The list is cleared before being added to the pool. A {@code null} list,
     * a list whose size exceeds the internal poolable-size limit, or a list
     * offered when the pool is already full is silently ignored (not pooled).
     *
     * <p>This method should be called in a {@code finally} block to ensure
     * proper recycling:</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Objectory.createList();
     * try {
     *     // Use the list
     * } finally {
     *     Objectory.recycle(list);
     * }
     * }</pre>
     *
     * @param list the list to recycle; may be {@code null}
     * @deprecated for internal use only
     * @see #createList()
     */
    @Deprecated
    public static void recycle(final List<?> list) {
        if ((list == null) || (list.size() > POOLABLE_SIZE)) {
            return;
        }

        if (listPool.size() < POOL_SIZE_FOR_COLLECTION) {
            list.clear();
            listPool.offer(list);
        }
    }

    /**
     * Returns a {@code Set} to the object pool for reuse.
     * The set is cleared before being added to the pool. A {@code null} set, a
     * set whose size exceeds the internal poolable-size limit, or a set offered
     * when the relevant pool is already full is silently ignored (not pooled).
     * {@link LinkedHashSet} instances are pooled separately from other
     * {@code Set} implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> set = Objectory.createSet();
     * try {
     *     set.add("hello");
     * } finally {
     *     Objectory.recycle(set);
     * }
     * }</pre>
     *
     * @param set the set to recycle; may be {@code null}
     * @deprecated for internal use only
     * @see #createSet()
     * @see #createLinkedHashSet()
     */
    @Deprecated
    public static void recycle(final Set<?> set) {
        if ((set == null) || (set.size() > POOLABLE_SIZE)) {
            return;
        }

        if (set instanceof LinkedHashSet) {
            if (linkedHashSetPool.size() < POOL_SIZE_FOR_COLLECTION) {
                set.clear();
                linkedHashSetPool.offer(set);
            }
        } else {
            if (setPool.size() < POOL_SIZE_FOR_COLLECTION) {
                set.clear();
                setPool.offer(set);
            }
        }
    }

    /**
     * Returns a {@code Map} to the object pool for reuse.
     * The map is cleared before being added to the pool. A {@code null} map, a
     * map whose size exceeds the internal poolable-size limit, or a map offered
     * when the relevant pool is already full is silently ignored (not pooled).
     * {@link LinkedHashMap} instances are pooled separately from other
     * {@code Map} implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, String> map = Objectory.createMap();
     * try {
     *     map.put("k1", "v1");
     *     map.put("k2", "v2");
     * } finally {
     *     Objectory.recycle(map);
     * }
     * }</pre>
     *
     * @param map the map to recycle; may be {@code null}
     * @deprecated for internal use only
     * @see #createMap()
     * @see #createLinkedHashMap()
     */
    @Deprecated
    public static void recycle(final Map<?, ?> map) {
        if ((map == null) || (map.size() > POOLABLE_SIZE)) {
            return;
        }

        if (map instanceof LinkedHashMap) {
            if (linkedHashMapPool.size() < POOL_SIZE_FOR_COLLECTION) {
                map.clear();
                linkedHashMapPool.offer(map);
            }
        } else {
            if (mapPool.size() < POOL_SIZE_FOR_COLLECTION) {
                map.clear();
                mapPool.offer(map);
            }
        }
    }

    /**
     * Returns an {@code Object[]} to the object pool for reuse.
     * The array is cleared (all elements set to {@code null}) before being
     * added to the pool. A {@code null} array, an empty array, an array longer
     * than the maximum poolable length ({@code 128}), or an array offered when
     * the per-length pool is already full is silently ignored (not pooled).
     * Arrays are pooled in separate queues keyed by their exact length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] array = Objectory.createObjectArray(10);
     * try {
     *     // Use the array
     * } finally {
     *     Objectory.recycle(array);
     * }
     * }</pre>
     *
     * @param objArray the array to recycle; may be {@code null}
     * @see #createObjectArray(int)
     */
    public static void recycle(final Object[] objArray) {
        if ((objArray == null) || (objArray.length == 0) || (objArray.length > POOLABLE_ARRAY_LENGTH)) {
            return;
        }

        synchronized (objectArrayPool) {
            final int poolSize = N.min(MAX_ARRAY_POOL_SIZE, MAX_ARRAY_LENGTH / objArray.length);
            Queue<Object[]> arrayQueue = objectArrayPool[objArray.length];

            if (arrayQueue == null) {
                arrayQueue = new ArrayBlockingQueue<>(poolSize);
                objectArrayPool[objArray.length] = arrayQueue;
            }

            if (arrayQueue.size() < poolSize) {
                Arrays.fill(objArray, null);
                arrayQueue.offer(objArray);
            }
        }
    }

    /**
     * Returns a {@code char[]} buffer to the object pool for reuse.
     * Only arrays whose length is exactly the internal default buffer size are
     * pooled; a {@code null} array or any other length is silently ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] buffer = Objectory.createCharArrayBuffer();
     * try {
     *     // Use the buffer
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @param cbuf the char array to recycle; may be {@code null}
     * @see #createCharArrayBuffer(int)
     */
    public static void recycle(final char[] cbuf) {
        // Only pool arrays of exactly BUFFER_SIZE — createCharArrayBuffer(int) promises a buffer
        // at least as large as the requested capacity, so a smaller pooled array would silently
        // violate that contract on a subsequent allocation. Larger-than-BUFFER_SIZE arrays from
        // createCharArrayBuffer(capacity>BUFFER_SIZE) are always freshly allocated and were never
        // pooled to begin with.
        if (cbuf == null || cbuf.length != BUFFER_SIZE) {
            return;
        }

        charArrayBufferPool.offer(cbuf);
    }

    /**
     * Returns a {@code byte[]} buffer to the object pool for reuse.
     * Only arrays whose length is exactly the internal default buffer size are
     * pooled; a {@code null} array or any other length is silently ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = Objectory.createByteArrayBuffer();
     * try {
     *     // Use the buffer
     * } finally {
     *     Objectory.recycle(buffer);
     * }
     * }</pre>
     *
     * @param bbuf the byte array to recycle; may be {@code null}
     * @see #createByteArrayBuffer(int)
     */
    public static void recycle(final byte[] bbuf) {
        // See recycle(char[]) for the rationale on the exact-length check.
        if (bbuf == null || bbuf.length != BUFFER_SIZE) {
            return;
        }

        byteArrayBufferPool.offer(bbuf);
    }

    /**
     * Returns a {@link StringBuilder} to the object pool for reuse.
     * The {@code StringBuilder} is reset to zero length before being added to
     * the pool. A {@code null} builder, a builder whose capacity exceeds the
     * internal default buffer size, or a builder offered when the pool is
     * already full is silently ignored (not pooled).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = Objectory.createStringBuilder();
     * try {
     *     sb.append("Hello World");
     *     return sb.toString();
     * } finally {
     *     Objectory.recycle(sb);
     * }
     * }</pre>
     *
     * @param sb the StringBuilder to recycle; may be {@code null}
     * @see #createStringBuilder(int)
     */
    public static void recycle(final StringBuilder sb) {
        if ((sb == null) || (sb.capacity() > BUFFER_SIZE)) {
            return;
        }

        if (stringBuilderPool.size() < POOL_SIZE_FOR_BUFFER) {
            sb.setLength(0);
            stringBuilderPool.offer(sb);
        }
    }

    /**
     * Returns a {@link ByteArrayOutputStream} to the object pool for reuse.
     * The stream is reset before being added to the pool. A {@code null}
     * stream, a stream whose capacity exceeds the internal default buffer size,
     * or a stream offered when the pool is already full is silently ignored
     * (not pooled).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
     * try {
     *     baos.write("data".getBytes());
     *     byte[] result = baos.toByteArray();
     * } finally {
     *     Objectory.recycle(baos);
     * }
     * }</pre>
     *
     * @param os the ByteArrayOutputStream to recycle; may be {@code null}
     * @see #createByteArrayOutputStream(int)
     */
    public static void recycle(final ByteArrayOutputStream os) {
        if ((os == null) || (os.capacity() > BUFFER_SIZE)) {
            return;
        }

        if (byteArrayOutputStreamPool.size() < POOL_SIZE_FOR_BUFFER) {
            os.reset();
            byteArrayOutputStreamPool.offer(os);
        }
    }

    /**
     * Returns a {@link BufferedXmlWriter} to the object pool for reuse.
     * The writer's buffer is flushed to its underlying target and the writer is
     * reset before being added to the pool. A {@code null} writer is silently
     * ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(outputStream);
     * try {
     *     writer.write("<root>Hello</root>");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param bw the BufferedXmlWriter to recycle; may be {@code null}
     * @throws UncheckedIOException if an I/O error occurs while flushing the buffer
     * @see #createBufferedXmlWriter()
     */
    public static void recycle(final BufferedXmlWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBufferToWriter();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedXmlWriterPool.offer(bw);
    }

    /**
     * Returns a {@link BufferedJsonWriter} to the object pool for reuse.
     * The writer's buffer is flushed to its underlying target and the writer is
     * reset before being added to the pool. A {@code null} writer is silently
     * ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(outputStream);
     * try {
     *     writer.write("{\"name\": \"value\"}");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param bw the BufferedJsonWriter to recycle; may be {@code null}
     * @throws UncheckedIOException if an I/O error occurs while flushing the buffer
     * @see #createBufferedJsonWriter()
     */
    public static void recycle(final BufferedJsonWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBufferToWriter();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedJsonWriterPool.offer(bw);
    }

    /**
     * Returns a {@link BufferedCsvWriter} to the object pool for reuse.
     * The writer's buffer is flushed to its underlying target and the writer is
     * reset before being added to the pool. Writers using a backslash escape
     * character are pooled separately from those that do not. A {@code null}
     * writer is silently ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCsvWriter writer = Objectory.createBufferedCsvWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param bw the BufferedCsvWriter to recycle; may be {@code null}
     * @throws UncheckedIOException if an I/O error occurs while flushing the buffer
     * @see #createBufferedCsvWriter()
     */
    public static void recycle(final BufferedCsvWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBufferToWriter();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();

        if (bw.isBackSlash()) {
            backSlashBufferedCsvWriterPool.offer(bw);
        } else {
            bufferedCsvWriterPool.offer(bw);
        }
    }

    /**
     * Returns a {@link java.io.BufferedWriter} to the object pool for reuse.
     * The writer's buffer is flushed to its underlying target and the writer is
     * reset before being added to the pool. This method dispatches to the
     * appropriate pool based on the concrete writer type (the library's JSON,
     * XML, CSV, or plain buffered writer). A {@code null} writer, or any writer
     * that is not one of these library types (for example a plain
     * {@link java.io.BufferedWriter} instance not created by this class), is
     * silently ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.io.BufferedWriter writer = Objectory.createBufferedWriter(outputStream);
     * try {
     *     writer.write("Some text");
     *     writer.flush();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param writer the {@code BufferedWriter} to recycle; may be {@code null}
     * @throws UncheckedIOException if an I/O error occurs while flushing the buffer
     * @see #createBufferedWriter()
     */
    public static void recycle(final java.io.BufferedWriter writer) {
        if (writer instanceof BufferedJsonWriter) {
            recycle((BufferedJsonWriter) writer);
        } else if (writer instanceof BufferedXmlWriter) {
            recycle((BufferedXmlWriter) writer);
        } else if (writer instanceof BufferedCsvWriter) {
            recycle((BufferedCsvWriter) writer);
        } else if (writer instanceof BufferedWriter bw) {
            try {
                bw.flushBufferToWriter();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            bw._reset();
            bufferedWriterPool.offer(bw);
        }
    }

    /**
     * Returns a {@link java.io.BufferedReader} to the object pool for reuse.
     * The reader is reset before being added to the pool. Only instances of the
     * library's internal {@code BufferedReader} are pooled; a {@code null}
     * reader or any other {@link java.io.BufferedReader} is silently ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * java.io.BufferedReader reader = Objectory.createBufferedReader(inputStream);
     * try {
     *     String line = reader.readLine();
     *     // Process line
     * } finally {
     *     Objectory.recycle(reader);
     * }
     * }</pre>
     *
     * @param reader the {@code BufferedReader} to recycle; may be {@code null}
     * @see #createBufferedReader(InputStream)
     * @see #createBufferedReader(Reader)
     */
    public static void recycle(final java.io.BufferedReader reader) {
        if (reader instanceof BufferedReader br) {
            br._reset();
            bufferedReaderPool.offer(br);
        }
    }
}
