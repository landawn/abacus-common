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
 *     Objectory.recycle(sb);   // Return to pool for reuse
 * }
 * }</pre>
 * 
 * @see BufferedWriter
 * @see BufferedReader
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

    private static final Queue<BufferedXMLWriter> bufferedXMLWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedJSONWriter> bufferedJSONWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private static final Queue<BufferedCSVWriter> bufferedCSVWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);
    private static final Queue<BufferedCSVWriter> backSlashBufferedCSVWriterPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

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
     * @return an empty ArrayList from the pool or a new instance if the pool is empty
     * @deprecated for internal use only
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
     * @return an empty HashSet from the pool or a new instance if the pool is empty
     * @deprecated for internal use only
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
     * @return an empty LinkedHashSet from the pool or a new instance if the pool is empty
     * @deprecated for internal use only
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
     * @return an empty HashMap from the pool or a new instance if the pool is empty
     * @deprecated for internal use only
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
     * @return an empty LinkedHashMap from the pool or a new instance if the pool is empty
     * @deprecated for internal use only
     */
    @Deprecated
    public static <K, V> Map<K, V> createLinkedHashMap() {
        final Map<K, V> linkedHashMap = (Map<K, V>) linkedHashMapPool.poll();

        return (linkedHashMap == null) ? N.newLinkedHashMap() : linkedHashMap;
    }

    /**
     * Creates or retrieves an Object array with the default poolable size.
     * The array length will be {@link #POOLABLE_ARRAY_LENGTH}.
     * 
     * <p>After use, the array should be recycled using {@link #recycle(Object[])}.</p>
     *
     * @return an Object array from the pool or a new instance if the pool is empty
     */
    public static Object[] createObjectArray() {
        return createObjectArray(POOLABLE_ARRAY_LENGTH);
    }

    /**
     * Creates or retrieves an Object array of the specified size.
     * Arrays larger than {@link #POOLABLE_ARRAY_LENGTH} are always newly allocated.
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
     * @return an Object array of the specified size
     */
    public static Object[] createObjectArray(final int size) {
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
     * Creates or retrieves a char array buffer with the default buffer size.
     * The buffer size will be {@link #BUFFER_SIZE}.
     * 
     * <p>After use, the buffer should be recycled using {@link #recycle(char[])}.</p>
     *
     * @return a char array buffer from the pool or a new instance if the pool is empty
     */
    public static char[] createCharArrayBuffer() {
        return createCharArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a char array buffer of the specified capacity.
     * Buffers larger than {@link #BUFFER_SIZE} are always newly allocated.
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
     * @param capacity the desired capacity of the buffer
     * @return a char array buffer of the specified capacity
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
     * Creates or retrieves a byte array buffer with the default buffer size.
     * The buffer size will be {@link #BUFFER_SIZE}.
     * 
     * <p>After use, the buffer should be recycled using {@link #recycle(byte[])}.</p>
     *
     * @return a byte array buffer from the pool or a new instance if the pool is empty
     */
    public static byte[] createByteArrayBuffer() {
        return createByteArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a byte array buffer of the specified capacity.
     * Buffers larger than {@link #BUFFER_SIZE} are always newly allocated.
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
     * @param capacity the desired capacity of the buffer
     * @return a byte array buffer of the specified capacity
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
     * Creates or retrieves a StringBuilder with the default buffer size.
     * The initial capacity will be {@link #BUFFER_SIZE}.
     * 
     * <p>After use, the StringBuilder should be recycled using {@link #recycle(StringBuilder)}.</p>
     *
     * @return a StringBuilder from the pool or a new instance if the pool is empty
     */
    public static StringBuilder createStringBuilder() {
        return createStringBuilder(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a StringBuilder with the specified initial capacity.
     * StringBuilders with capacity larger than {@link #BUFFER_SIZE} are always newly allocated.
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
     * @return a StringBuilder with the specified initial capacity
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
     * Creates or retrieves a ByteArrayOutputStream with the default buffer size.
     * The initial capacity will be {@link #BUFFER_SIZE}.
     * 
     * <p>After use, the stream should be recycled using {@link #recycle(ByteArrayOutputStream)}.</p>
     *
     * @return a ByteArrayOutputStream from the pool or a new instance if the pool is empty
     */
    public static ByteArrayOutputStream createByteArrayOutputStream() {
        return createByteArrayOutputStream(BUFFER_SIZE);
    }

    /**
     * Creates or retrieves a ByteArrayOutputStream with the specified initial capacity.
     * Streams with capacity larger than {@link #BUFFER_SIZE} are always newly allocated.
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
     * @param initCapacity the desired initial capacity
     * @return a ByteArrayOutputStream with the specified initial capacity
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
     * Creates or retrieves a BufferedWriter with no underlying writer.
     * The writer must be initialized with {@link BufferedWriter#reinit(Writer)} before use.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * @return a BufferedWriter from the pool or a new instance if the pool is empty
     */
    public static java.io.BufferedWriter createBufferedWriter() {
        BufferedWriter bw = bufferedWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedWriter");   //NOSONAR

            bw = new BufferedWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedWriter wrapping the specified OutputStream.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * @param os the OutputStream to wrap
     * @return a BufferedWriter wrapping the specified stream
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
     * Creates or retrieves a BufferedWriter wrapping the specified Writer.
     * If the writer is already a BufferedWriter, it is returned as-is.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(java.io.BufferedWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer fileWriter = new FileWriter("output.txt")) {
     *     BufferedWriter bw = Objectory.createBufferedWriter(fileWriter);
     *     try {
     *         bw.write("Hello World");
     *         bw.newLine();
     *     } finally {
     *         Objectory.recycle(bw);
     *     }
     * }
     * }</pre>
     *
     * @param writer the Writer to wrap
     * @return a BufferedWriter wrapping the specified writer
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
     * Creates or retrieves a BufferedXMLWriter with no underlying writer.
     * The writer must be initialized before use.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXMLWriter)}.</p>
     *
     * @return a BufferedXMLWriter from the pool or a new instance if the pool is empty
     */
    public static BufferedXMLWriter createBufferedXMLWriter() {
        BufferedXMLWriter bw = bufferedXMLWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXMLWriter");   //NOSONAR

            bw = new BufferedXMLWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedXMLWriter wrapping the specified OutputStream.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXMLWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedXMLWriter writer = Objectory.createBufferedXMLWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the OutputStream to wrap
     * @return a BufferedXMLWriter wrapping the specified stream
     */
    public static BufferedXMLWriter createBufferedXMLWriter(final OutputStream os) {
        BufferedXMLWriter bw = bufferedXMLWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXMLWriter");

            bw = new BufferedXMLWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedXMLWriter wrapping the specified Writer.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedXMLWriter)}.</p>
     *
     * @param writer the Writer to wrap
     * @return a BufferedXMLWriter wrapping the specified writer
     */
    public static BufferedXMLWriter createBufferedXMLWriter(final Writer writer) {
        BufferedXMLWriter bw = bufferedXMLWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXMLWriter");

            bw = new BufferedXMLWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedJSONWriter with no underlying writer.
     * The writer must be initialized before use.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJSONWriter)}.</p>
     *
     * @return a BufferedJSONWriter from the pool or a new instance if the pool is empty
     */
    public static BufferedJSONWriter createBufferedJSONWriter() {
        BufferedJSONWriter bw = bufferedJSONWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJSONWriter");   //NOSONAR

            bw = new BufferedJSONWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedJSONWriter wrapping the specified OutputStream.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJSONWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJSONWriter writer = Objectory.createBufferedJSONWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the OutputStream to wrap
     * @return a BufferedJSONWriter wrapping the specified stream
     */
    public static BufferedJSONWriter createBufferedJSONWriter(final OutputStream os) {
        BufferedJSONWriter bw = bufferedJSONWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJSONWriter");

            bw = new BufferedJSONWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedJSONWriter wrapping the specified Writer.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedJSONWriter)}.</p>
     *
     * @param writer the Writer to wrap
     * @return a BufferedJSONWriter wrapping the specified writer
     */
    public static BufferedJSONWriter createBufferedJSONWriter(final Writer writer) {
        BufferedJSONWriter bw = bufferedJSONWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJSONWriter");

            bw = new BufferedJSONWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedCSVWriter with no underlying writer.
     * The writer must be initialized before use.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCSVWriter)}.</p>
     *
     * @return a BufferedCSVWriter from the pool or a new instance if the pool is empty
     */
    public static BufferedCSVWriter createBufferedCSVWriter() {
        BufferedCSVWriter bw = CSVUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCSVWriterPool.poll() : bufferedCSVWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCSVWriter");   //NOSONAR

            bw = new BufferedCSVWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedCSVWriter wrapping the specified OutputStream.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCSVWriter)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedCSVWriter writer = Objectory.createBufferedCSVWriter(outputStream);
     * try {
     *     writer.write("Hello");
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param os the OutputStream to wrap
     * @return a BufferedCSVWriter wrapping the specified stream
     */
    public static BufferedCSVWriter createBufferedCSVWriter(final OutputStream os) {
        BufferedCSVWriter bw = CSVUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCSVWriterPool.poll() : bufferedCSVWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCSVWriter");

            bw = new BufferedCSVWriter(os);
        } else {
            bw.reinit(os);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedCSVWriter wrapping the specified Writer.
     * 
     * <p>After use, the writer should be recycled using {@link #recycle(BufferedCSVWriter)}.</p>
     *
     * @param writer the Writer to wrap
     * @return a BufferedCSVWriter wrapping the specified writer
     */
    public static BufferedCSVWriter createBufferedCSVWriter(final Writer writer) {
        BufferedCSVWriter bw = CSVUtil.isBackSlashEscapeCharForWrite() ? backSlashBufferedCSVWriterPool.poll() : bufferedCSVWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedCSVWriter");

            bw = new BufferedCSVWriter(writer);
        } else {
            bw.reinit(writer);
        }

        return bw;
    }

    /**
     * Creates or retrieves a BufferedReader wrapping the specified String.
     *
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String text = "Line 1\nLine 2\nLine 3";
     * BufferedReader reader = Objectory.createBufferedReader(text);
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
     * @param str the String to read from
     * @return a BufferedReader reading from the specified string
     */
    public static java.io.BufferedReader createBufferedReader(final String str) {
        return createBufferedReader(new java.io.StringReader(str));
    }

    /**
     * Creates or retrieves a BufferedReader wrapping the specified InputStream.
     * 
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * @param is the InputStream to wrap
     * @return a BufferedReader wrapping the specified stream
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
     * Creates or retrieves a BufferedReader wrapping the specified Reader.
     * If the reader is already a BufferedReader, it is returned as-is.
     *
     * <p>After use, the reader should be recycled using {@link #recycle(java.io.BufferedReader)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader fileReader = new FileReader("input.txt")) {
     *     BufferedReader br = Objectory.createBufferedReader(fileReader);
     *     try {
     *         String line = br.readLine();
     *         // Process line
     *     } finally {
     *         Objectory.recycle(br);
     *     }
     * }
     * }</pre>
     *
     * @param reader the Reader to wrap
     * @return a BufferedReader wrapping the specified reader
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
     * Returns a List to the object pool for reuse.
     * The list is cleared before being added to the pool.
     * Lists larger than {@link #POOLABLE_SIZE} are not pooled.
     * 
     * <p>This method should be called in a finally block to ensure proper recycling:</p>
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
     * @param list the list to recycle
     * @deprecated for internal use only
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
     * Returns a Set to the object pool for reuse.
     * The set is cleared before being added to the pool.
     * Sets larger than {@link #POOLABLE_SIZE} are not pooled.
     * LinkedHashSets are pooled separately from regular HashSets.
     *
     * @param set the set to recycle
     * @deprecated for internal use only
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
     * Returns a Map to the object pool for reuse.
     * The map is cleared before being added to the pool.
     * Maps larger than {@link #POOLABLE_SIZE} are not pooled.
     * LinkedHashMaps are pooled separately from regular HashMaps.
     *
     * @param map the map to recycle
     * @deprecated for internal use only
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
     * Returns an Object array to the object pool for reuse.
     * The array is cleared (all elements set to null) before being added to the pool.
     * Arrays larger than {@link #POOLABLE_ARRAY_LENGTH} are not pooled.
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
     * @param objArray the array to recycle
     */
    public static void recycle(final Object[] objArray) {
        if ((objArray == null) || (objArray.length > POOLABLE_ARRAY_LENGTH)) {
            return;
        }

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

    /**
     * Returns a char array buffer to the object pool for reuse.
     * Arrays larger than {@link #BUFFER_SIZE} are not pooled.
     *
     * @param cbuf the char array to recycle
     */
    public static void recycle(final char[] cbuf) {
        if ((cbuf == null) || (cbuf.length > BUFFER_SIZE)) {
            return;
        }

        charArrayBufferPool.offer(cbuf);
    }

    /**
     * Returns a byte array buffer to the object pool for reuse.
     * Arrays larger than {@link #BUFFER_SIZE} are not pooled.
     *
     * @param bbuf the byte array to recycle
     */
    public static void recycle(final byte[] bbuf) {
        if ((bbuf == null) || (bbuf.length > BUFFER_SIZE)) {
            return;
        }

        byteArrayBufferPool.offer(bbuf);
    }

    /**
     * Returns a StringBuilder to the object pool for reuse.
     * The StringBuilder is cleared before being added to the pool.
     * StringBuilders with capacity larger than {@link #BUFFER_SIZE} are not pooled.
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
     * @param sb the StringBuilder to recycle
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
     * Returns a ByteArrayOutputStream to the object pool for reuse.
     * The stream is reset before being added to the pool.
     * Streams with capacity larger than {@link #BUFFER_SIZE} are not pooled.
     *
     * @param os the ByteArrayOutputStream to recycle
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
     * Returns a BufferedXMLWriter to the object pool for reuse.
     * The writer's buffer is flushed and the writer is reset before being added to the pool.
     *
     * @param bw the BufferedXMLWriter to recycle
     * @throws UncheckedIOException if an I/O error occurs while flushing
     */
    public static void recycle(final BufferedXMLWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBufferToWriter();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedXMLWriterPool.offer(bw);
    }

    /**
     * Returns a BufferedJSONWriter to the object pool for reuse.
     * The writer's buffer is flushed and the writer is reset before being added to the pool.
     *
     * @param bw the BufferedJSONWriter to recycle
     * @throws UncheckedIOException if an I/O error occurs while flushing
     */
    public static void recycle(final BufferedJSONWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBufferToWriter();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedJSONWriterPool.offer(bw);
    }

    /**
     * Returns a BufferedCSVWriter to the object pool for reuse.
     * The writer's buffer is flushed and the writer is reset before being added to the pool.
     * Writers are pooled separately based on their escape character settings.
     *
     * @param bw the BufferedCSVWriter to recycle
     * @throws UncheckedIOException if an I/O error occurs while flushing
     */
    public static void recycle(final BufferedCSVWriter bw) {
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
            backSlashBufferedCSVWriterPool.offer(bw);
        } else {
            bufferedCSVWriterPool.offer(bw);
        }
    }

    /**
     * Returns a BufferedWriter to the object pool for reuse.
     * The writer's buffer is flushed and the writer is reset before being added to the pool.
     * This method handles all types of buffered writers (JSON, XML, CSV, and plain).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedWriter writer = Objectory.createBufferedWriter(outputStream);
     * try {
     *     writer.write("Some text");
     *     writer.flush();
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * @param writer the BufferedWriter to recycle
     * @throws UncheckedIOException if an I/O error occurs while flushing
     */
    public static void recycle(final java.io.BufferedWriter writer) {
        if (writer instanceof BufferedJSONWriter) {
            recycle((BufferedJSONWriter) writer);
        } else if (writer instanceof BufferedXMLWriter) {
            recycle((BufferedXMLWriter) writer);
        } else if (writer instanceof BufferedCSVWriter) {
            recycle((BufferedCSVWriter) writer);
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
     * Returns a BufferedReader to the object pool for reuse.
     * The reader is reset before being added to the pool.
     * Only instances of the custom BufferedReader class are pooled.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedReader reader = Objectory.createBufferedReader(inputStream);
     * try {
     *     String line = reader.readLine();
     *     // Process line
     * } finally {
     *     Objectory.recycle(reader);
     * }
     * }</pre>
     *
     * @param reader the BufferedReader to recycle
     */
    public static void recycle(final java.io.BufferedReader reader) {
        if (reader instanceof BufferedReader br) {
            br._reset();
            bufferedReaderPool.offer(br);
        }
    }
}
