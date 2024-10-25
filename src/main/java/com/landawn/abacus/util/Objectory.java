/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

@Internal
@Beta
public final class Objectory {

    private static final Logger logger = LoggerFactory.getLogger(Objectory.class);

    private static AtomicInteger created = new AtomicInteger();

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

    private static final Queue<BufferedReader> bufferedReaderPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BUFFER);

    private Objectory() {
        // Utility class
    }

    /**
     * Creates the list.
     *
     * @param <T>
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> List<T> createList() {
        final List<T> list = (List<T>) listPool.poll();

        return (list == null) ? new ArrayList<>() : list;
    }

    /**
     * Creates the set.
     *
     * @param <T>
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createSet() {
        final Set<T> set = (Set<T>) setPool.poll();

        return (set == null) ? N.newHashSet() : set;
    }

    /**
     * Creates the linked hash set.
     *
     * @param <T>
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createLinkedHashSet() {
        final Set<T> set = (Set<T>) linkedHashSetPool.poll();

        return (set == null) ? N.newLinkedHashSet() : set;
    }

    /**
     * Creates the map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> createMap() {
        final Map<K, V> map = (Map<K, V>) mapPool.poll();

        return (map == null) ? N.newHashMap() : map;
    }

    /**
     * Creates the linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @deprecated for internal use only
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> createLinkedHashMap() {
        final Map<K, V> linkedHashMap = (Map<K, V>) linkedHashMapPool.poll();

        return (linkedHashMap == null) ? N.newLinkedHashMap() : linkedHashMap;
    }

    /**
     * Creates the object array.
     *
     * @return
     */
    public static Object[] createObjectArray() {
        return createObjectArray(POOLABLE_ARRAY_LENGTH);
    }

    /**
     * Creates the object array.
     *
     * @param size
     * @return
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
     * Creates the char array buffer.
     *
     * @return
     */
    public static char[] createCharArrayBuffer() {
        return createCharArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates the char array buffer.
     *
     * @param capacity
     * @return
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
     * Creates the byte array buffer.
     *
     * @return
     */
    public static byte[] createByteArrayBuffer() {
        return createByteArrayBuffer(BUFFER_SIZE);
    }

    /**
     * Creates the byte array buffer.
     *
     * @param capacity
     * @return
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
     * Creates the string builder.
     *
     * @return
     */
    public static StringBuilder createStringBuilder() {
        return createStringBuilder(BUFFER_SIZE);
    }

    /**
     * Creates the string builder.
     *
     * @param initCapacity
     * @return
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
     * Creates the byte array output stream.
     *
     * @return
     */
    public static ByteArrayOutputStream createByteArrayOutputStream() {
        return createByteArrayOutputStream(BUFFER_SIZE);
    }

    /**
     * Creates the byte array output stream.
     *
     * @param initCapacity
     * @return
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
     * Creates the buffered writer.
     *
     * @return
     */
    public static BufferedWriter createBufferedWriter() {
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
     * Creates the buffered writer.
     *
     * @param os
     * @return
     */
    public static BufferedWriter createBufferedWriter(final OutputStream os) {
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
     * Creates the buffered writer.
     *
     * @param writer
     * @return
     */
    public static BufferedWriter createBufferedWriter(final Writer writer) {
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
     * Creates the buffered XML writer.
     *
     * @return
     */
    public static BufferedXMLWriter createBufferedXMLWriter() {
        BufferedXMLWriter bw = bufferedXMLWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedXMLWriter"); //NOSONAR

            bw = new BufferedXMLWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates the buffered XML writer.
     *
     * @param os
     * @return
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
     * Creates the buffered XML writer.
     *
     * @param writer
     * @return
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
     * Creates the buffered JSON writer.
     *
     * @return
     */
    public static BufferedJSONWriter createBufferedJSONWriter() {
        BufferedJSONWriter bw = bufferedJSONWriterPool.poll();

        if (bw == null) {
            logCreated("createBufferedJSONWriter"); //NOSONAR

            bw = new BufferedJSONWriter();
        } else {
            bw.reinit();
        }

        return bw;
    }

    /**
     * Creates the buffered JSON writer.
     *
     * @param os
     * @return
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
     * Creates the buffered JSON writer.
     *
     * @param writer
     * @return
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
     * Creates the buffered reader.
     *
     * @param st
     * @return
     */
    public static BufferedReader createBufferedReader(final String st) {
        final BufferedReader br = bufferedReaderPool.poll();

        if (br == null) {
            logCreated("createBufferedReader"); //NOSONAR

            return new BufferedReader(st);
        } else {
            br.reinit(st);

            return br;
        }
    }

    /**
     * Creates the buffered reader.
     *
     * @param is
     * @return
     */
    public static BufferedReader createBufferedReader(final InputStream is) {
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
     * Creates the buffered reader.
     *
     * @param reader
     * @return
     */
    public static BufferedReader createBufferedReader(final Reader reader) {
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
     *
     * @param methodName
     */
    private static void logCreated(final String methodName) {
        if (logger.isWarnEnabled() && (created.incrementAndGet() % 1000) == 0) {
            logger.warn("The " + created.get() + "th cachable object is created by " + methodName,
                    new RuntimeException("No error. It's only for debug to print stack trace:"));
        }
    }

    /**
     *
     * @param list
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
     *
     * @param set
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
     *
     * @param map
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
     *
     * @param objArray
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
     *
     * @param cbuf
     */
    public static void recycle(final char[] cbuf) {
        if ((cbuf == null) || (cbuf.length > BUFFER_SIZE)) {
            return;
        }

        charArrayBufferPool.offer(cbuf);
    }

    /**
     *
     * @param bbuf
     */
    public static void recycle(final byte[] bbuf) {
        if ((bbuf == null) || (bbuf.length > BUFFER_SIZE)) {
            return;
        }

        byteArrayBufferPool.offer(bbuf);
    }

    /**
     *
     * @param sb
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
     *
     * @param os
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
     *
     * @param bw
     */
    public static void recycle(final BufferedWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedWriterPool.offer(bw);
    }

    /**
     *
     * @param bw
     */
    public static void recycle(final BufferedXMLWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedXMLWriterPool.offer(bw);
    }

    /**
     *
     * @param bw
     */
    public static void recycle(final BufferedJSONWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedJSONWriterPool.offer(bw);
    }

    /**
     *
     * @param br
     */
    public static void recycle(final BufferedReader br) {
        if (br == null) {
            return;
        }

        br._reset();
        bufferedReaderPool.offer(br);
    }
}
