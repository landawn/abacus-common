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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
@Beta
public final class Objectory {

    private static final Logger logger = LoggerFactory.getLogger(Objectory.class);

    private static AtomicInteger created = new AtomicInteger();

    static final int KB = 1024;

    static final int POOL_SIZE = 1024;

    static final int POOL_SIZE_FOR_BIG_BUFFER = 64;

    static final int POOLABLE_SIZE = 8192;

    static final int BUFFER_SIZE = Math.min(Math.max(IOUtil.MAX_MEMORY_IN_MB, KB * 8), 64 * KB);

    static final int BIG_BUFFER_SIZE = BUFFER_SIZE * 8;

    private static final int POOLABLE_ARRAY_LENGTH = 128;

    private static final int MAX_ARRAY_POOL_SIZE = IOUtil.IS_PLATFORM_ANDROID ? 8 : 64;

    private static final int MAX_ARRAY_LENGTH = IOUtil.IS_PLATFORM_ANDROID ? 128 : 1024;

    private static final Queue<List<?>> listPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<Set<?>> setPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<Set<?>> linkedHashSetPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<Map<?, ?>> mapPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<Map<?, ?>> linkedHashMapPool = new ArrayBlockingQueue<>(POOL_SIZE);

    @SuppressWarnings("unchecked")
    private static final Queue<Object[]>[] objectArrayPool = new Queue[POOLABLE_SIZE + 1];

    private static final Queue<StringBuilder> stringBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<StringBuilder> bigStringBuilderPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BIG_BUFFER);

    private static final Queue<char[]> charArrayBufferPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BIG_BUFFER);

    private static final Queue<byte[]> byteArrayBufferPool = new ArrayBlockingQueue<>(POOL_SIZE_FOR_BIG_BUFFER);

    private static final Queue<ByteArrayOutputStream> byteArrayOutputStreamPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<BufferedWriter> bufferedWriterPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<BufferedXMLWriter> bufferedXMLWriterPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<BufferedJSONWriter> bufferedJSONWriterPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private static final Queue<BufferedReader> bufferedReaderPool = new ArrayBlockingQueue<>(POOL_SIZE);

    private Objectory() {
        // Utility class
    }

    /**
     * Creates the list.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> createList() {
        List<T> list = (List<T>) listPool.poll();

        return (list == null) ? new ArrayList<>() : list;
    }

    /**
     * Creates the set.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createSet() {
        Set<T> set = (Set<T>) setPool.poll();

        return (set == null) ? N.newHashSet() : set;
    }

    /**
     * Creates the linked hash set.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> createLinkedHashSet() {
        Set<T> set = (Set<T>) linkedHashSetPool.poll();

        return (set == null) ? N.newLinkedHashSet() : set;
    }

    /**
     * Creates the map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> createMap() {
        Map<K, V> map = (Map<K, V>) mapPool.poll();

        return (map == null) ? N.newHashMap() : map;
    }

    /**
     * Creates the linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> createLinkedHashMap() {
        Map<K, V> linkedHashMap = (Map<K, V>) linkedHashMapPool.poll();

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
    public static Object[] createObjectArray(int size) {
        if (size > POOLABLE_ARRAY_LENGTH) {
            return new Object[size];
        }

        Object[] objArray = null;

        synchronized (objectArrayPool) {
            Queue<Object[]> arrayQueue = objectArrayPool[size];

            if (N.notNullOrEmpty(arrayQueue)) {
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
    public static StringBuilder createStringBuilder(int initCapacity) {
        if (initCapacity > BIG_BUFFER_SIZE) {
            // logCreated("createStringBuilder");

            return new StringBuilder(initCapacity);
        }

        if (initCapacity <= BUFFER_SIZE) {
            StringBuilder sb = stringBuilderPool.poll();

            if (sb == null) {
                logCreated("createStringBuilder");

                sb = new StringBuilder(BUFFER_SIZE);
            }

            return sb;
        } else {
            return createBigStringBuilder();
        }
    }

    public static StringBuilder createBigStringBuilder() {
        StringBuilder sb = bigStringBuilderPool.poll();

        if (sb == null) {
            logCreated("bigStringBuilderPool");

            sb = new StringBuilder(BIG_BUFFER_SIZE);
        }

        return sb;
    }

    /**
     * Creates the char array buffer.
     *
     * @return
     */
    public static char[] createCharArrayBuffer() {
        return createCharArrayBuffer(BIG_BUFFER_SIZE);
    }

    /**
     * Creates the char array buffer.
     *
     * @param capacity
     * @return
     */
    public static char[] createCharArrayBuffer(int capacity) {
        if (capacity > BIG_BUFFER_SIZE) {
            // logCreated("createCharArrayBuffer");

            return new char[capacity];
        }

        char[] cbuf = charArrayBufferPool.poll();

        if (cbuf == null) {
            logCreated("createCharArrayBuffer");

            cbuf = new char[BIG_BUFFER_SIZE];
        }

        return cbuf;
    }

    /**
     * Creates the byte array buffer.
     *
     * @return
     */
    public static byte[] createByteArrayBuffer() {
        return createByteArrayBuffer(BIG_BUFFER_SIZE);
    }

    /**
     * Creates the byte array buffer.
     *
     * @param capacity
     * @return
     */
    public static byte[] createByteArrayBuffer(int capacity) {
        if (capacity > BIG_BUFFER_SIZE) {
            // logCreated("createByteArrayBuffer");

            return new byte[capacity];
        }

        byte[] bbuf = byteArrayBufferPool.poll();

        if (bbuf == null) {
            logCreated("createByteArrayBuffer");

            bbuf = new byte[BIG_BUFFER_SIZE];
        }

        return bbuf;
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
    public static ByteArrayOutputStream createByteArrayOutputStream(int initCapacity) {
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
    public static BufferedWriter createBufferedWriter(OutputStream os) {
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
    public static BufferedWriter createBufferedWriter(Writer writer) {
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
    public static BufferedXMLWriter createBufferedXMLWriter(OutputStream os) {
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
    public static BufferedXMLWriter createBufferedXMLWriter(Writer writer) {
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
    public static BufferedJSONWriter createBufferedJSONWriter(OutputStream os) {
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
    public static BufferedJSONWriter createBufferedJSONWriter(Writer writer) {
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
    public static BufferedReader createBufferedReader(String st) {
        BufferedReader br = bufferedReaderPool.poll();

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
    public static BufferedReader createBufferedReader(InputStream is) {
        BufferedReader br = bufferedReaderPool.poll();

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
    public static BufferedReader createBufferedReader(Reader reader) {
        BufferedReader br = bufferedReaderPool.poll();

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
    private static void logCreated(String methodName) {
        if (logger.isWarnEnabled() && (created.incrementAndGet() % 1000) == 0) {
            logger.warn("The " + created.get() + "th cachable object is created by " + methodName,
                    new RuntimeException("No error. It's only for debug to print stack trace:"));
        }
    }

    /**
     *
     * @param list
     */
    public static void recycle(List<?> list) {
        if ((list == null) || (list.size() > POOLABLE_SIZE)) {
            return;
        }

        if (listPool.size() < POOL_SIZE) {
            list.clear();
            listPool.offer(list);
        }
    }

    /**
     *
     * @param set
     */
    public static void recycle(Set<?> set) {
        if ((set == null) || (set.size() > POOLABLE_SIZE)) {
            return;
        }

        if (set instanceof LinkedHashSet) {
            if (linkedHashSetPool.size() < POOL_SIZE) {
                set.clear();
                linkedHashSetPool.offer(set);
            }
        } else {
            if (setPool.size() < POOL_SIZE) {
                set.clear();
                setPool.offer(set);
            }
        }
    }

    /**
     *
     * @param map
     */
    public static void recycle(Map<?, ?> map) {
        if ((map == null) || (map.size() > POOLABLE_SIZE)) {
            return;
        }

        if (map instanceof LinkedHashMap) {
            if (linkedHashMapPool.size() < POOL_SIZE) {
                map.clear();
                linkedHashMapPool.offer(map);
            }
        } else {
            if (mapPool.size() < POOL_SIZE) {
                map.clear();
                mapPool.offer(map);
            }
        }
    }

    /**
     *
     * @param objArray
     */
    public static void recycle(Object[] objArray) {
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
     * @param sb
     */
    public static void recycle(StringBuilder sb) {
        if ((sb == null) || (sb.capacity() > BIG_BUFFER_SIZE)) {
            return;
        }

        if (sb.capacity() <= BUFFER_SIZE) {
            if (stringBuilderPool.size() < POOL_SIZE) {
                sb.setLength(0);
                stringBuilderPool.offer(sb);
            }
        } else {
            if (bigStringBuilderPool.size() < POOL_SIZE_FOR_BIG_BUFFER) {
                sb.setLength(0);
                bigStringBuilderPool.offer(sb);
            }
        }
    }

    /**
     *
     * @param cbuf
     */
    public static void recycle(char[] cbuf) {
        if ((cbuf == null) || (cbuf.length > BIG_BUFFER_SIZE)) {
            return;
        }

        charArrayBufferPool.offer(cbuf);
    }

    /**
     *
     * @param bbuf
     */
    public static void recycle(byte[] bbuf) {
        if ((bbuf == null) || (bbuf.length > BIG_BUFFER_SIZE)) {
            return;
        }

        byteArrayBufferPool.offer(bbuf);
    }

    /**
     *
     * @param os
     */
    public static void recycle(ByteArrayOutputStream os) {
        if ((os == null) || (os.capacity() > BUFFER_SIZE)) {
            return;
        }

        if (byteArrayOutputStreamPool.size() < POOL_SIZE) {
            os.reset();
            byteArrayOutputStreamPool.offer(os);
        }
    }

    /**
     *
     * @param bw
     */
    public static void recycle(BufferedWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedWriterPool.offer(bw);
    }

    /**
     *
     * @param bw
     */
    public static void recycle(BufferedXMLWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedXMLWriterPool.offer(bw);
    }

    /**
     *
     * @param bw
     */
    public static void recycle(BufferedJSONWriter bw) {
        if (bw == null) {
            return;
        }

        try {
            bw.flushBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        bw._reset();
        bufferedJSONWriterPool.offer(bw);
    }

    /**
     *
     * @param br
     */
    public static void recycle(BufferedReader br) {
        if (br == null) {
            return;
        }

        br._reset();
        bufferedReaderPool.offer(br);
    }
}
