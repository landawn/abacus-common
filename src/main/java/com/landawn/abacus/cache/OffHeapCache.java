/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.cache;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.Parser;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.pool.AbstractPoolable;
import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.type.ByteBufferType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MoreExecutors;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Try;

import sun.misc.Unsafe;

/**
 * It's not designed for tiny objects(length of bytes < 128 after serialization).
 * Since it's off heap cache, modifying the objects from cache won't impact the objects in cache.
 * 
 * @param <K>
 * @param <V>
 * 
 * @since 0.8
 * 
 * @author haiyang li
 */
public class OffHeapCache<K, V> extends AbstractCache<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(OffHeapCache.class);
    //    /**
    //     * Sets all bytes in a given block of memory to a copy of another
    //     * block.
    //     *
    //     * <p>This method determines each block's base address by means of two parameters,
    //     * and so it provides (in effect) a <em>double-register</em> addressing mode,
    //     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
    //     * the offset supplies an absolute base address.
    //     *
    //     * <p>The transfers are in coherent (atomic) units of a size determined
    //     * by the address and length parameters.  If the effective addresses and
    //     * length are all even modulo 8, the transfer takes place in 'long' units.
    //     * If the effective addresses and length are (resp.) even modulo 4 or 2,
    //     * the transfer takes place in units of 'int' or 'short'.
    //     *
    //     * @since 1.7
    //     */
    //    public native void copyMemory(Object srcBase, long srcOffset,
    //                                  Object destBase, long destOffset,
    //                                  long bytes);

    private static final Parser<?, ?> parser = ParserFactory.isKryoAvailable() ? ParserFactory.createKryoParser() : ParserFactory.createJSONParser();
    private static final ByteBufferType bbType = (ByteBufferType) ((Type<?>) TypeFactory.getType(ByteBufferType.BYTE_BUFFER));

    private static final int SEGMENT_SIZE = 1024 * 1024; // (int) N.ONE_MB;
    private static final int MIN_BLOCK_SIZE = 256;
    private static final int MAX_BLOCK_SIZE = 8192; // 8K

    private static final Unsafe UNSAFE;
    static {
        try {
            final Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Unsafe", e);
        }
    }

    private static final int BYTE_ARRAY_BASE = UNSAFE.arrayBaseOffset(byte[].class);

    private static final ScheduledExecutorService scheduledExecutor;
    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(IOUtil.CPU_CORES);
        executor.setRemoveOnCancelPolicy(true);
        scheduledExecutor = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private ScheduledFuture<?> scheduleFuture;

    private final long _capacityB;
    private final long _startPtr;
    private final Segment[] _segments;
    private final BitSet _segmentBitSet = new BitSet();
    private final Map<Integer, Deque<Segment>> _segmentQueueMap = new ConcurrentHashMap<>();

    private final Deque<Segment> _queue256 = new LinkedList<>();
    private final Deque<Segment> _queue384 = new LinkedList<>();
    private final Deque<Segment> _queue512 = new LinkedList<>();
    private final Deque<Segment> _queue640 = new LinkedList<>();
    private final Deque<Segment> _queue768 = new LinkedList<>();
    private final Deque<Segment> _queue896 = new LinkedList<>();
    private final Deque<Segment> _queue1024 = new LinkedList<>();
    private final Deque<Segment> _queue1280 = new LinkedList<>();
    private final Deque<Segment> _queue1536 = new LinkedList<>();
    private final Deque<Segment> _queue1792 = new LinkedList<>();
    private final Deque<Segment> _queue2048 = new LinkedList<>();
    private final Deque<Segment> _queue2560 = new LinkedList<>();
    private final Deque<Segment> _queue3072 = new LinkedList<>();
    private final Deque<Segment> _queue3584 = new LinkedList<>();
    private final Deque<Segment> _queue4096 = new LinkedList<>();
    private final Deque<Segment> _queue5120 = new LinkedList<>();
    private final Deque<Segment> _queue6144 = new LinkedList<>();
    private final Deque<Segment> _queue7168 = new LinkedList<>();
    private final Deque<Segment> _queue8192 = new LinkedList<>();

    private final AsyncExecutor _asyncExecutor = new AsyncExecutor();
    private final AtomicInteger _activeVacationTaskCount = new AtomicInteger();
    private final KeyedObjectPool<K, Wrapper<V>> _pool;

    /**
     * The memory with the specified size of MB will be allocated at application start up.
     * 
     * @param sizeMB
     */
    public OffHeapCache(int sizeMB) {
        this(sizeMB, 3000);
    }

    /**
     * The memory with the specified size of MB will be allocated at application start up.
     * 
     * @param sizeMB
     * @param evictDelay unit is milliseconds
     */
    public OffHeapCache(int sizeMB, long evictDelay) {
        this(sizeMB, evictDelay, DEFAULT_LIVE_TIME, DEFAULT_MAX_IDLE_TIME);
    }

    /**
     * The memory with the specified size of MB will be allocated at application start up.
     * 
     * @param sizeMB
     * @param evictDelay unit is milliseconds
     * @param defaultLiveTime unit is milliseconds
     * @param defaultMaxIdleTime unit is milliseconds
     */
    public OffHeapCache(int sizeMB, long evictDelay, long defaultLiveTime, long defaultMaxIdleTime) {
        super(defaultLiveTime, defaultMaxIdleTime);

        this._capacityB = sizeMB * (1024L * 1024L); // N.ONE_MB;
        // ByteBuffer.allocateDirect((int) capacity);
        _startPtr = UNSAFE.allocateMemory(_capacityB);
        _segments = new Segment[(int) (_capacityB / SEGMENT_SIZE)];

        for (int i = 0, len = _segments.length; i < len; i++) {
            _segments[i] = new Segment(_startPtr + i * SEGMENT_SIZE);
        }

        _pool = PoolFactory.createKeyedObjectPool((int) (_capacityB / MIN_BLOCK_SIZE), evictDelay);

        if (evictDelay > 0) {
            final Runnable evictTask = new Runnable() {
                @Override
                public void run() {
                    // Evict from the pool
                    try {
                        evict();
                    } catch (Exception e) {
                        // ignore

                        if (logger.isWarnEnabled()) {
                            logger.warn(AbacusException.getErrorMsg(e));
                        }
                    }
                }
            };

            scheduleFuture = scheduledExecutor.scheduleWithFixedDelay(evictTask, evictDelay, evictDelay, TimeUnit.MILLISECONDS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.warn("Starting to shutdown task in OffHeapCache");

                try {
                    close();
                } finally {
                    logger.warn("Completed to shutdown task in OffHeapCache");
                }
            }
        });
    }

    @Override
    public V gett(K k) {
        final Wrapper<V> w = _pool.get(k);

        return w == null ? null : w.read();
    }

    private static void copyFromMemory(final long startPtr, final byte[] bytes, int destOffset, int len) {
        UNSAFE.copyMemory(null, startPtr, bytes, destOffset, len);
    }

    @Override
    public boolean put(K k, V v, long liveTime, long maxIdleTime) {
        final Type<V> type = N.typeOf(v.getClass());
        Wrapper<V> w = null;

        // final byte[] bytes = parser.serialize(v).getBytes();
        ByteArrayOutputStream os = null;
        byte[] bytes = null;
        int size = 0;

        if (type.isPrimitiveByteArray()) {
            bytes = (byte[]) v;
            size = bytes.length;
        } else if (type.isByteBuffer()) {
            bytes = bbType.byteArrayOf((ByteBuffer) v);
            size = bytes.length;
        } else {
            os = Objectory.createByteArrayOutputStream();
            parser.serialize(os, v);
            bytes = os.array();
            size = os.size();
        }

        if (size <= MAX_BLOCK_SIZE) {
            final AvaialbeSegment availableSegment = getAvailableSegment(size);

            if (availableSegment == null) {
                Objectory.recycle(os);

                vacate();
                return false;
            }

            final long startPtr = availableSegment.segment.startPtr + availableSegment.availableBlockIndex * availableSegment.segment.sizeOfBlock;
            boolean isOK = false;

            try {
                copyToMemory(bytes, BYTE_ARRAY_BASE, startPtr, size);

                isOK = true;
            } finally {
                Objectory.recycle(os);

                if (isOK == false) {
                    availableSegment.release();

                    return false;
                }
            }

            w = new SWrapper<>(type, liveTime, maxIdleTime, size, availableSegment.segment, startPtr);
        } else {
            final List<Map.Entry<Long, Segment>> segmentResult = new ArrayList<>(size / MAX_BLOCK_SIZE + 1);
            int copiedSize = 0;
            int srcOffset = BYTE_ARRAY_BASE;

            try {
                while (copiedSize < size) {
                    final int sizeToCopy = size - copiedSize > MAX_BLOCK_SIZE ? MAX_BLOCK_SIZE : size - copiedSize;
                    final AvaialbeSegment availableSegment = getAvailableSegment(sizeToCopy);

                    if (availableSegment == null) {

                        vacate();
                        return false;
                    }

                    final long startPtr = availableSegment.segment.startPtr + availableSegment.availableBlockIndex * availableSegment.segment.sizeOfBlock;
                    boolean isOK = false;

                    try {
                        copyToMemory(bytes, srcOffset, startPtr, sizeToCopy);

                        srcOffset += sizeToCopy;
                        copiedSize += sizeToCopy;

                        isOK = true;
                    } finally {
                        if (isOK == false) {
                            availableSegment.release();

                            return false;
                        }
                    }

                    segmentResult.add(N.newImmutableEntry(startPtr, availableSegment.segment));
                }

                w = new MWrapper<>(type, liveTime, maxIdleTime, size, segmentResult);
            } finally {
                Objectory.recycle(os);

                if (w == null) {
                    for (Map.Entry<Long, Segment> entry : segmentResult) {
                        Segment segment = entry.getValue();
                        segment.release((int) ((entry.getKey() - segment.startPtr) / segment.sizeOfBlock));
                    }
                }
            }
        }

        boolean result = false;

        try {
            result = _pool.put(k, w);
        } finally {
            if (!result) {
                w.destroy();
            }
        }

        return result;
    }

    // TODO: performance tuning for concurrent put.
    private AvaialbeSegment getAvailableSegment(int size) {
        Deque<Segment> queue = null;
        int blockSize = 0;

        if (size <= 256) {
            queue = _queue256;
            blockSize = 256;
        } else if (size <= 384) {
            queue = _queue384;
            blockSize = 384;
        } else if (size <= 512) {
            queue = _queue512;
            blockSize = 512;
        } else if (size <= 640) {
            queue = _queue640;
            blockSize = 640;
        } else if (size <= 768) {
            queue = _queue768;
            blockSize = 768;
        } else if (size <= 896) {
            queue = _queue896;
            blockSize = 896;
        } else if (size <= 1024) {
            queue = _queue1024;
            blockSize = 1024;
        } else if (size <= 1280) {
            queue = _queue1280;
            blockSize = 1280;
        } else if (size <= 1536) {
            queue = _queue1536;
            blockSize = 1536;
        } else if (size <= 1792) {
            queue = _queue1792;
            blockSize = 1792;
        } else if (size <= 2048) {
            queue = _queue2048;
            blockSize = 2048;
        } else if (size <= 2560) {
            queue = _queue2560;
            blockSize = 2560;
        } else if (size <= 3072) {
            queue = _queue3072;
            blockSize = 3072;
        } else if (size <= 3584) {
            queue = _queue3584;
            blockSize = 3584;
        } else if (size <= 4096) {
            queue = _queue4096;
            blockSize = 4096;
        } else if (size <= 5120) {
            queue = _queue5120;
            blockSize = 5120;
        } else if (size <= 6144) {
            queue = _queue6144;
            blockSize = 6144;
        } else if (size <= 7168) {
            queue = _queue7168;
            blockSize = 7168;
        } else if (size <= 8192) {
            queue = _queue8192;
            blockSize = 8192;
        } else {
            throw new RuntimeException("Unsupported object size: " + size);
        }

        Segment segment = null;
        int availableBlockIndex = -1;

        synchronized (queue) {
            Iterator<Segment> iterator = queue.iterator();
            Iterator<Segment> descendingIterator = queue.descendingIterator();
            int half = queue.size() / 2 + 1;
            int cnt = 0;
            while (iterator.hasNext() && half-- > 0) {
                cnt++;
                segment = iterator.next();

                if ((availableBlockIndex = segment.allocate()) >= 0) {
                    if (cnt > 3) {
                        iterator.remove();
                        queue.addFirst(segment);
                    }

                    break;
                }

                segment = descendingIterator.next();

                if ((availableBlockIndex = segment.allocate()) >= 0) {
                    if (cnt > 3) {
                        descendingIterator.remove();
                        queue.addFirst(segment);
                    }

                    break;
                }
            }

            if (availableBlockIndex < 0) {
                synchronized (_segmentBitSet) {
                    int nextSegmentIndex = _segmentBitSet.nextClearBit(0);

                    if (nextSegmentIndex >= _segments.length) {
                        return null; // No space available;
                    }

                    segment = _segments[nextSegmentIndex];
                    _segmentBitSet.set(nextSegmentIndex);
                    _segmentQueueMap.put(nextSegmentIndex, queue);

                    segment.sizeOfBlock = blockSize;
                    queue.addFirst(segment);

                    availableBlockIndex = segment.allocate();
                }
            }
        }

        return new AvaialbeSegment(segment, availableBlockIndex);
    }

    private static void copyToMemory(final byte[] srcBytes, int srcOffset, final long startPtr, final int len) {
        UNSAFE.copyMemory(srcBytes, srcOffset, null, startPtr, len);
    }

    private void vacate() {
        if (_activeVacationTaskCount.get() > 0) {
            return;
        }

        synchronized (_activeVacationTaskCount) {
            if (_activeVacationTaskCount.get() > 0) {
                return;
            }

            _activeVacationTaskCount.incrementAndGet();

            _asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    try {
                        _pool.vacate();

                        evict();

                        // wait for couple of seconds to avoid the second vacation which just arrives before the this vacation is done.
                        N.sleep(3000);
                    } finally {
                        _activeVacationTaskCount.decrementAndGet();
                    }
                }
            });
        }
    }

    @Override
    public void remove(K k) {
        final Wrapper<V> w = _pool.remove(k);

        if (w != null) {
            w.destroy();
        }
    }

    @Override
    public boolean containsKey(K k) {
        return _pool.containsKey(k);
    }

    @Override
    public Set<K> keySet() {
        return _pool.keySet();
    }

    @Override
    public int size() {
        return _pool.size();
    }

    @Override
    public void clear() {
        _pool.clear();
    }

    @Override
    public void close() {
        if (_pool.isClosed()) {
            return;
        }

        try {
            if (scheduleFuture != null) {
                scheduleFuture.cancel(true);
            }
        } finally {
            try {
                _pool.close();
            } finally {
                UNSAFE.freeMemory(_startPtr);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return _pool.isClosed();
    }

    /**
     * recycle the empty Segment.
     * 
     */
    protected void evict() {
        for (int i = 0, len = _segments.length; i < len; i++) {
            if (_segments[i].blockBitSet.isEmpty()) {
                final Deque<Segment> queue = _segmentQueueMap.get(i);

                if (queue != null) {
                    synchronized (queue) {
                        if (_segments[i].blockBitSet.isEmpty()) {
                            synchronized (_segmentBitSet) {
                                queue.remove(_segments[i]);

                                _segmentQueueMap.remove(i);
                                _segmentBitSet.clear(i);
                            }
                        }
                    }
                }
            }
        }
    }

    private static abstract class Wrapper<T> extends AbstractPoolable {
        final Type<T> type;
        final int size;

        Wrapper(final Type<T> type, long liveTime, long maxIdleTime, int size) {
            super(liveTime, maxIdleTime);

            this.type = type;
            this.size = size;
        }

        abstract T read();
    }

    private static final class SWrapper<T> extends Wrapper<T> {
        private Segment segment;
        private final long startPtr;

        SWrapper(final Type<T> type, long liveTime, long maxIdleTime, int size, Segment segment, long startPtr) {
            super(type, liveTime, maxIdleTime, size);

            this.segment = segment;
            this.startPtr = startPtr;
        }

        @Override
        T read() {
            synchronized (this) {
                if (segment == null) {
                    return null;
                }

                final byte[] bytes = new byte[size];

                copyFromMemory(startPtr, bytes, BYTE_ARRAY_BASE, size);

                // it's destroyed after read from memory. dirty data may be read.
                if (type.isPrimitiveByteArray()) {
                    return this.segment == null ? null : (T) bytes;
                } else if (type.isByteBuffer()) {
                    return this.segment == null ? null : (T) bbType.valueOf(bytes);
                } else {
                    return this.segment == null ? null : parser.deserialize(type.clazz(), new ByteArrayInputStream(bytes));
                }
            }
        }

        @Override
        public void destroy() {
            synchronized (this) {
                if (segment != null) {
                    segment.release((int) ((startPtr - segment.startPtr) / segment.sizeOfBlock));
                    segment = null;
                }
            }
        }
    }

    private static final class MWrapper<T> extends Wrapper<T> {
        private List<Map.Entry<Long, Segment>> segments;

        MWrapper(final Type<T> type, long liveTime, long maxIdleTime, int size, List<Map.Entry<Long, Segment>> segments) {
            super(type, liveTime, maxIdleTime, size);

            this.segments = segments;
        }

        @Override
        T read() {
            synchronized (this) {
                if (N.isNullOrEmpty(segments)) {
                    return null;
                }

                final List<Map.Entry<Long, Segment>> segments = this.segments;
                final byte[] bytes = new byte[size];
                int size = this.size;
                int destOffset = BYTE_ARRAY_BASE;

                for (Map.Entry<Long, Segment> entry : segments) {
                    final long startPtr = entry.getKey();
                    final Segment segment = entry.getValue();
                    final int sizeToCopy = size > segment.sizeOfBlock ? segment.sizeOfBlock : size;

                    copyFromMemory(startPtr, bytes, destOffset, sizeToCopy);

                    destOffset += sizeToCopy;
                    size -= sizeToCopy;
                }

                // should never happen.
                if (size != 0) {
                    throw new RuntimeException(
                            "Unknown error happening when retrieve value. The remaining size is " + size + " after finishing fetch data from all segments");
                }

                // it's destroyed after read from memory. dirty data may be read.
                if (type.isPrimitiveByteArray()) {
                    return this.segments == null ? null : (T) bytes;
                } else if (type.isByteBuffer()) {
                    return this.segments == null ? null : (T) (T) bbType.valueOf(bytes);
                } else {
                    return this.segments == null ? null : parser.deserialize(type.clazz(), new ByteArrayInputStream(bytes));
                }
            }
        }

        @Override
        public void destroy() {
            synchronized (this) {
                if (segments != null) {
                    for (Map.Entry<Long, Segment> entry : segments) {
                        final Segment segment = entry.getValue();
                        segment.release((int) ((entry.getKey() - segment.startPtr) / segment.sizeOfBlock));
                    }

                    segments = null;
                }
            }
        }
    }

    private static final class Segment {
        private final BitSet blockBitSet = new BitSet();
        private final long startPtr;
        private int sizeOfBlock;

        public Segment(long segmentStartPtr) {
            this.startPtr = segmentStartPtr;
        }

        public int allocate() {
            synchronized (blockBitSet) {
                int result = blockBitSet.nextClearBit(0);

                if (result >= SEGMENT_SIZE / sizeOfBlock) {
                    return -1;
                }

                blockBitSet.set(result);

                return result;
            }
        }

        public void release(int blockIndex) {
            synchronized (blockBitSet) {
                blockBitSet.clear(blockIndex);
            }
        }
        //
        //        public void clear() {
        //            synchronized (blockBitSet) {
        //                blockBitSet.clear();
        //                sizeOfBlock = 0;
        //            }
        //        }
    }

    private static final class AvaialbeSegment {
        final Segment segment;
        final int availableBlockIndex;

        AvaialbeSegment(final Segment segment, final int availableBlockIndex) {
            this.segment = segment;
            this.availableBlockIndex = availableBlockIndex;
        }

        void release() {
            this.segment.release(availableBlockIndex);
        }
    }
}
