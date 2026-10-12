/*
 * Copyright (C) 2026 HaiYang Li
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
package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.DoubleIterator;

/** Bounded primitive handoff using the established parallel-worker executor and failure lifecycle. */
final class ParallelPrimitiveBuffer {
    private ParallelPrimitiveBuffer() {
    }

    static ByteStream concatByte(final List<? extends ByteIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final ByteIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return iterator.nextByte();
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return ByteStream.of(new ByteIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public byte nextByte() {
                return (byte) buffer.nextLong();
            }
        }).onClose(buffer::closeResource);
    }

    static CharStream concatChar(final List<? extends CharIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final CharIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return iterator.nextChar();
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return CharStream.of(new CharIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public char nextChar() {
                return (char) buffer.nextLong();
            }
        }).onClose(buffer::closeResource);
    }

    static ShortStream concatShort(final List<? extends ShortIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final ShortIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return iterator.nextShort();
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return ShortStream.of(new ShortIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public short nextShort() {
                return (short) buffer.nextLong();
            }
        }).onClose(buffer::closeResource);
    }

    static IntStream concatInt(final List<? extends IntIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final IntIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return iterator.nextInt();
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return IntStream.of(new IntIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public int nextInt() {
                return (int) buffer.nextLong();
            }
        }).onClose(buffer::closeResource);
    }

    static LongStream concatLong(final List<? extends LongIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final LongIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return iterator.nextLong();
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return LongStream.of(new LongIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public long nextLong() {
                return buffer.nextLong();
            }
        }).onClose(buffer::closeResource);
    }

    static FloatStream concatFloat(final List<? extends FloatIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final FloatIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return Float.floatToRawIntBits(iterator.nextFloat());
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return FloatStream.of(new FloatIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public float nextFloat() {
                return Float.intBitsToFloat((int) buffer.nextLong());
            }
        }).onClose(buffer::closeResource);
    }

    static DoubleStream concatDouble(final List<? extends DoubleIterator> iterators, final boolean cancelUncompletedThreads,
            final AsyncExecutor asyncExecutor) {
        final List<LongIterator> workers = new ArrayList<>(iterators.size());

        for (final DoubleIterator iterator : iterators) {
            workers.add(new LongIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public long nextLong() {
                    return Double.doubleToRawLongBits(iterator.nextDouble());
                }
            });
        }

        final PrimitiveRing buffer = new PrimitiveRing(workers, cancelUncompletedThreads, asyncExecutor);

        return DoubleStream.of(new DoubleIterator() {
            @Override
            public boolean hasNext() {
                return buffer.hasNext();
            }

            @Override
            public double nextDouble() {
                return Double.longBitsToDouble(buffer.nextLong());
            }
        }).onClose(buffer::closeResource);
    }

    /**
     * Integral values and floating-point raw bits share storage without wrapper objects. The worker futures and any
     * temporary executor are released when the ring runs out of values, when it reports a worker failure, when starting
     * the workers fails, or when its stream is closed, whichever comes first.
     */
    private static final class PrimitiveRing extends LongIteratorEx {
        private final List<LongIterator> iterators;
        private final boolean cancelUncompletedThreads;
        private final AsyncExecutor requestedExecutor;
        private final MutableBoolean onGoing = MutableBoolean.of(true);
        private final Holder<Throwable> failure = new Holder<>();
        private final AtomicInteger remaining;
        private final List<ContinuableFuture<Void>> futures;
        private final ReentrantLock queueLock = new ReentrantLock();
        private final Condition notEmpty = queueLock.newCondition();
        private final Condition notFull = queueLock.newCondition();
        private long[] values;
        private int head;
        private int size;
        private boolean started;
        // Written last by a successful start(), which leaves values and executor safely published. Until then
        // hasNext() enters the start() monitor; afterwards it skips that monitor on every element.
        private volatile boolean initialized;
        private boolean closed;
        private AsyncExecutor executor;
        private boolean ready;
        private long next;

        private PrimitiveRing(final List<LongIterator> iterators, final boolean cancelUncompletedThreads, final AsyncExecutor requestedExecutor) {
            this.iterators = iterators;
            this.cancelUncompletedThreads = cancelUncompletedThreads;
            this.requestedExecutor = requestedExecutor;
            remaining = new AtomicInteger(iterators.size());
            futures = cancelUncompletedThreads ? new ArrayList<>(iterators.size()) : null;
        }

        private synchronized void start() {
            if (started || closed) {
                return;
            }

            started = true;
            final int threadNum = iterators.size();
            values = new long[StreamBase.calculateBufferedSize(threadNum, threadNum)];
            executor = StreamBase.checkAsyncExecutor(requestedExecutor, threadNum);

            try {
                for (int i = 0; i < threadNum && !closed; i++) {
                    final LongIterator iterator = iterators.get(i);
                    executor = StreamBase.execute(executor, threadNum, i, futures, () -> {
                        try {
                            while (onGoing.value() && iterator.hasNext()) {
                                final long value = iterator.nextLong();

                                queueLock.lockInterruptibly();
                                try {
                                    while (size == values.length && onGoing.value()) {
                                        notFull.await();
                                    }

                                    if (!onGoing.value()) {
                                        break;
                                    }

                                    int tail = head + size;
                                    if (tail >= values.length) {
                                        tail -= values.length;
                                    }
                                    values[tail] = value;
                                    size++;
                                    // Publish every value immediately: a following callback may wait for its consumer.
                                    notEmpty.signal();
                                } finally {
                                    queueLock.unlock();
                                }
                            }
                        } catch (final Throwable e) {
                            StreamBase.setError(failure, e, onGoing);
                        } finally {
                            remaining.decrementAndGet();
                            queueLock.lock();
                            try {
                                notEmpty.signalAll();
                                notFull.signalAll();
                            } finally {
                                queueLock.unlock();
                            }
                        }
                    });
                }

                // Submitted workers own their source references. Keeping this startup list would retain every
                // source (including captured arrays and mapped collections) after the workers finish.
                iterators.clear();
                if (closed && futures != null) {
                    // An inline executor can reenter closeResource() before execute() appends its future.
                    futures.clear();
                }
                initialized = true;
            } catch (final Throwable e) {
                StreamBase.setError(failure, e, onGoing);
                closeResource();
                throw StreamBase.toRuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            if (ready) {
                return true;
            }

            if (!initialized) {
                start();
            }

            queueLock.lock();
            try {
                try {
                    while (size == 0 && onGoing.value() && remaining.get() > 0) {
                        notEmpty.await();
                    }
                } catch (final InterruptedException e) {
                    StreamBase.setError(failure, e, onGoing);
                    notFull.signalAll();
                }

                // Drain successful publications before surfacing a later worker failure.
                if (size > 0) {
                    next = values[head];
                    head = head + 1 == values.length ? 0 : head + 1;
                    size--;
                    ready = true;
                    notFull.signal();
                }
            } finally {
                queueLock.unlock();
            }

            if (!ready) {
                // Nothing more will be produced: every worker has finished, a worker failed, or the ring is already
                // closed. Release the futures and any temporary executor now, as the replaced flatMap-based
                // concatenation did when its inner stream ran out, and as start() does when submitting fails: a caller
                // that drains iterator(), or abandons it on a failure, without closing the stream must not keep the
                // pool threads alive. A pending worker failure is still the exception that is thrown.
                try {
                    if (failure.value() != null) {
                        StreamBase.setStopFlagAndThrowException(failure, onGoing);
                    }
                } finally {
                    closeResource();
                }
            }

            return ready;
        }

        @Override
        public long nextLong() {
            if (!ready && !hasNext()) {
                throw new NoSuchElementException(StreamBase.ERROR_MSG_FOR_NO_SUCH_EX);
            }

            ready = false;
            return next;
        }

        @Override
        public synchronized void closeResource() {
            if (closed) {
                return;
            }

            closed = true;
            onGoing.setFalse();
            queueLock.lock();
            try {
                notEmpty.signalAll();
                notFull.signalAll();
            } finally {
                queueLock.unlock();
            }

            try {
                if (cancelUncompletedThreads && futures != null) {
                    for (final ContinuableFuture<Void> future : futures) {
                        if (future != null && !future.isDone()) {
                            try {
                                future.cancelAll(true);
                            } catch (final RuntimeException ignored) {
                                // Match parallel concatenation's best-effort cancellation policy.
                            }
                        }
                    }
                }
            } finally {
                iterators.clear();
                if (futures != null) {
                    // Cancellation has finished; the handoff no longer needs to keep completed task graphs alive.
                    futures.clear();
                }
                if (executor != null) {
                    StreamBase.shutdownTempExecutor(executor, requestedExecutor);
                }
            }
        }
    }
}
