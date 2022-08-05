/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.9
 */
@com.landawn.abacus.annotation.Immutable
public abstract class Observer<T> implements Immutable {

    private static final Object COMPLETE_FLAG = new Object();

    protected static final double INTERVAL_FACTOR = 3;

    protected static final Runnable EMPTY_ACTION = () -> {
        // Do nothing;
    };

    protected static final Consumer<Exception> ON_ERROR_MISSING = t -> {
        throw new RuntimeException(t);
    };

    protected static final Executor asyncExecutor;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            asyncExecutor = AndroidUtil.getThreadPoolExecutor();
        } else {
            asyncExecutor = new ThreadPoolExecutor(Math.max(64, Math.min(IOUtil.CPU_CORES * 8, IOUtil.MAX_MEMORY_IN_MB / 1024) * 32),
                    Math.max(256, (IOUtil.MAX_MEMORY_IN_MB / 1024) * 64), 180L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }

    protected static final ScheduledThreadPoolExecutor schedulerForIntermediateOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : Math.max(64, Math.min(IOUtil.CPU_CORES * 8, IOUtil.MAX_MEMORY_IN_MB / 1024) * 32));

    protected static final ScheduledThreadPoolExecutor schedulerForObserveOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : Math.max(64, Math.min(IOUtil.CPU_CORES * 8, IOUtil.MAX_MEMORY_IN_MB / 1024) * 32));

    static {
        schedulerForIntermediateOp.setRemoveOnCancelPolicy(true);
        schedulerForObserveOp.setRemoveOnCancelPolicy(true);

        MoreExecutors.addDelayedShutdownHook(schedulerForIntermediateOp, 120, TimeUnit.SECONDS);
        MoreExecutors.addDelayedShutdownHook(schedulerForObserveOp, 120, TimeUnit.SECONDS);
    }

    protected final Map<ScheduledFuture<?>, Long> scheduledFutures = new LinkedHashMap<>();

    protected final Dispatcher<Object> dispatcher;

    protected boolean hasMore = true;

    protected Observer() {
        this(new Dispatcher<>());
    }

    protected Observer(Dispatcher<Object> dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     *
     * @param queue
     */
    @SuppressWarnings("rawtypes")
    public static void complete(BlockingQueue<?> queue) {
        ((Queue) queue).offer(COMPLETE_FLAG);
    }

    /**
     *
     * @param <T>
     * @param queue
     * @return
     */
    public static <T> Observer<T> of(final BlockingQueue<T> queue) {
        N.checkArgNotNull(queue, "queue");

        return new BlockingQueueObserver<>(queue);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Observer<T> of(final Collection<T> c) {
        return of(N.isNullOrEmpty(c) ? ObjIterator.<T> empty() : c.iterator());
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Observer<T> of(final Iterator<T> iter) {
        N.checkArgNotNull(iter, "iterator");

        return new IteratorObserver<>(iter);
    }

    /**
     *
     *
     * @param delayInMillis
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#timer(long,%20java.util.concurrent.TimeUnit)">RxJava#timer</a>
     */
    public static Observer<Long> timer(long delayInMillis) {
        return timer(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     *
     * @param delay
     * @param unit
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#timer(long,%20java.util.concurrent.TimeUnit)">RxJava#timer</a>
     */
    public static Observer<Long> timer(long delay, TimeUnit unit) {
        N.checkArgument(delay >= 0, "delay can't be negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        return new TimerObserver<>(delay, unit);
    }

    /**
     *
     *
     * @param periodInMillis
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(long periodInMillis) {
        return interval(0, periodInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param initialDelayInMillis
     * @param periodInMillis
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(long initialDelayInMillis, long periodInMillis) {
        return interval(initialDelayInMillis, periodInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     *
     * @param period
     * @param unit
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(long period, TimeUnit unit) {
        return interval(0, period, unit);
    }

    /**
     *
     *
     * @param initialDelay
     * @param period
     * @param unit
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(long initialDelay, long period, TimeUnit unit) {
        N.checkArgument(initialDelay >= 0, "initialDelay can't be negative");
        N.checkArgument(period > 0, "period can't be 0 or negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        return new IntervalObserver<>(initialDelay, period, unit);
    }

    /**
     *
     * @param intervalDurationInMillis
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDurationInMillis) {
        return debounce(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param intervalDuration
     * @param unit
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDuration, final TimeUnit unit) {
        N.checkArgument(intervalDuration >= 0, "Interval can't be negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInMillis = unit.toMillis(intervalDuration);

        dispatcher.append(new Dispatcher<Object>() {
            private long prevTimestamp = 0;
            private long lastScheduledTime = 0;

            @Override
            public void onNext(final Object param) {
                synchronized (holder) {
                    final long now = System.currentTimeMillis();

                    if (holder.value() == N.NULL_MASK || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
                        holder.setValue(param);
                        prevTimestamp = now;

                        schedule(intervalDuration, unit);
                    } else {
                        holder.setValue(param);
                        prevTimestamp = now;
                    }
                }
            }

            private void schedule(final long delay, final TimeUnit unit) {
                try {
                    schedulerForIntermediateOp.schedule(() -> {
                        final long pastIntervalInMills = System.currentTimeMillis() - prevTimestamp;

                        if (pastIntervalInMills >= intervalDurationInMillis) {
                            Object lastParam = null;

                            synchronized (holder) {
                                lastParam = holder.value();
                                holder.setValue(N.NULL_MASK);
                            }

                            if (lastParam != N.NULL_MASK && downDispatcher != null) {
                                downDispatcher.onNext(lastParam);
                            }
                        } else {
                            schedule(intervalDurationInMillis - pastIntervalInMills, TimeUnit.MILLISECONDS);
                        }
                    }, delay, unit);

                    lastScheduledTime = System.currentTimeMillis();
                } catch (Exception e) {
                    holder.setValue(N.NULL_MASK);

                    if (downDispatcher != null) {
                        downDispatcher.onError(e);
                    }
                }
            }
        });

        return this;
    }

    /**
     *
     * @param intervalDurationInMillis
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDurationInMillis) {
        return throttleFirst(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param intervalDuration
     * @param unit
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDuration, final TimeUnit unit) {
        N.checkArgument(intervalDuration >= 0, "Interval can't be negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInMillis = unit.toMillis(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            private long lastScheduledTime = 0;

            @Override
            public void onNext(final Object param) {
                synchronized (holder) {
                    final long now = System.currentTimeMillis();

                    if (holder.value() == N.NULL_MASK || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
                        holder.setValue(param);

                        try {
                            schedulerForIntermediateOp.schedule(() -> {
                                Object firstParam = null;

                                synchronized (holder) {
                                    firstParam = holder.value();
                                    holder.setValue(N.NULL_MASK);
                                }

                                if (firstParam != N.NULL_MASK && downDispatcher != null) {
                                    downDispatcher.onNext(firstParam);
                                }
                            }, intervalDuration, unit);

                            lastScheduledTime = now;
                        } catch (Exception e) {
                            holder.setValue(N.NULL_MASK);

                            if (downDispatcher != null) {
                                downDispatcher.onError(e);
                            }
                        }
                    }
                }
            }
        });

        return this;
    }

    /**
     *
     * @param intervalDurationInMillis
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDurationInMillis) {
        return throttleLast(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param intervalDuration
     * @param unit
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDuration, final TimeUnit unit) {
        N.checkArgument(intervalDuration >= 0, "Delay can't be negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInMillis = unit.toMillis(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            private long lastScheduledTime = 0;

            @Override
            public void onNext(final Object param) {
                synchronized (holder) {
                    final long now = System.currentTimeMillis();

                    if (holder.value() == N.NULL_MASK || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
                        holder.setValue(param);

                        try {
                            schedulerForIntermediateOp.schedule(() -> {
                                Object lastParam = null;

                                synchronized (holder) {
                                    lastParam = holder.value();
                                    holder.setValue(N.NULL_MASK);
                                }

                                if (lastParam != N.NULL_MASK && downDispatcher != null) {
                                    downDispatcher.onNext(lastParam);
                                }
                            }, intervalDuration, unit);

                            lastScheduledTime = now;
                        } catch (Exception e) {
                            holder.setValue(N.NULL_MASK);

                            if (downDispatcher != null) {
                                downDispatcher.onError(e);
                            }
                        }
                    } else {
                        holder.setValue(param);
                    }
                }
            }
        });

        return this;
    }

    /**
     *
     * @param delayInMillis
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delayInMillis) {
        return delay(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param delay
     * @param unit
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delay, final TimeUnit unit) {
        N.checkArgument(delay >= 0, "Delay can't be negative");
        N.checkArgNotNull(unit, "Time unit can't be null");

        if (delay == 0) {
            return this;
        }

        dispatcher.append(new Dispatcher<>() {
            private final long startTime = System.currentTimeMillis();
            private boolean isDelayed = false;

            @Override
            public void onNext(final Object param) {
                if (!isDelayed) {
                    N.sleep(unit.toMillis(delay) - (System.currentTimeMillis() - startTime));
                    isDelayed = true;
                }

                if (downDispatcher != null) {
                    downDispatcher.onNext(param);
                }
            }
        });

        return this;
    }

    /**
     *
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html#timeInterval()">RxJava#timeInterval</a>
     */
    public Observer<Timed<T>> timeInterval() {
        dispatcher.append(new Dispatcher<>() {
            private long startTime = System.currentTimeMillis();

            @Override
            public synchronized void onNext(final Object param) {
                if (downDispatcher != null) {
                    long now = System.currentTimeMillis();
                    long interval = now - startTime;
                    startTime = now;

                    downDispatcher.onNext(Timed.of(param, interval));
                }
            }
        });

        return (Observer<Timed<T>>) this;
    }

    /**
     *
     * @return this instance.
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html#timestamp()">RxJava#timestamp</a>
     */
    public Observer<Timed<T>> timestamp() {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    downDispatcher.onNext(Timed.of(param, System.currentTimeMillis()));
                }
            }
        });

        return (Observer<Timed<T>>) this;
    }

    /**
     *
     * @param n
     * @return
     */
    public Observer<T> skip(final long n) {
        N.checkArgNotNegative(n, "n");

        if (n > 0) {
            dispatcher.append(new Dispatcher<>() {
                private final AtomicLong counter = new AtomicLong();

                @Override
                public void onNext(final Object param) {
                    if (downDispatcher != null && counter.incrementAndGet() > n) {
                        downDispatcher.onNext(param);
                    }
                }
            });
        }

        return this;
    }

    /**
     *
     * @param maxSize
     * @return
     */
    public Observer<T> limit(final long maxSize) {
        N.checkArgNotNegative(maxSize, "maxSize");

        dispatcher.append(new Dispatcher<>() {
            private final AtomicLong counter = new AtomicLong();

            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && counter.incrementAndGet() <= maxSize) {
                    downDispatcher.onNext(param);
                } else {
                    hasMore = false;
                }
            }
        });

        return this;
    }

    public Observer<T> distinct() {
        dispatcher.append(new Dispatcher<>() {
            private Set<T> set = N.newHashSet();

            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && set.add((T) param)) {
                    downDispatcher.onNext(param);
                }
            }
        });

        return this;
    }

    /**
     *
     * @param keyMapper
     * @return
     */
    public Observer<T> distinctBy(final Function<? super T, ?> keyMapper) {
        dispatcher.append(new Dispatcher<>() {
            private Set<Object> set = N.newHashSet();

            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && set.add(keyMapper.apply((T) param))) { // onError if keyMapper.apply throws exception?
                    downDispatcher.onNext(param);
                }
            }
        });

        return this;
    }

    /**
     *
     * @param filter
     * @return
     */
    public Observer<T> filter(final Predicate<? super T> filter) {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && filter.test((T) param)) { // onError if filter.test throws exception?
                    downDispatcher.onNext(param);
                }
            }
        });

        return this;
    }

    /**
     *
     * @param <U>
     * @param map
     * @return
     */
    public <U> Observer<U> map(final Function<? super T, U> map) {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    downDispatcher.onNext(map.apply((T) param)); // onError if map.apply throws exception?
                }
            }
        });

        return (Observer<U>) this;
    }

    /**
     *
     * @param <U>
     * @param map
     * @return
     */
    public <U> Observer<U> flatMap(final Function<? super T, Collection<U>> map) {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    final Collection<U> c = map.apply((T) param); // onError if map.apply throws exception?

                    if (N.notNullOrEmpty(c)) {
                        for (U u : c) {
                            downDispatcher.onNext(u);
                        }
                    }
                }
            }
        });

        return (Observer<U>) this;
    }

    /**
     *
     * @param timespan
     * @param unit
     * @return this instance
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit)">RxJava#window(long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit) {
        return buffer(timespan, unit, Integer.MAX_VALUE);
    }

    /**
     *
     * @param timespan
     * @param unit
     * @param count
     * @return this instance
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit,%20int)">RxJava#window(long, java.util.concurrent.TimeUnit, int)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit, final int count) {
        N.checkArgument(timespan > 0, "timespan can't be 0 or negative");
        N.checkArgNotNull(unit, "Time unit can't be null");
        N.checkArgument(count > 0, "count can't be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final List<T> queue = new ArrayList<>();

            {
                scheduledFutures.put(schedulerForIntermediateOp.scheduleAtFixedRate(() -> {
                    List<T> list = null;
                    synchronized (queue) {
                        list = new ArrayList<>(queue);
                        queue.clear();
                    }

                    if (downDispatcher != null) {
                        downDispatcher.onNext(list);
                    }
                }, timespan, timespan, unit), timespan);
            }

            @Override
            public void onNext(final Object param) {
                List<T> list = null;

                synchronized (queue) {
                    queue.add((T) param);

                    if (queue.size() == count) {
                        list = new ArrayList<>(queue);
                        queue.clear();
                    }
                }

                if (list != null && downDispatcher != null) {
                    downDispatcher.onNext(list);
                }
            }
        });

        return (Observer<List<T>>) this;
    }

    /**
     *
     * @param timespan
     * @param timeskip
     * @param unit
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#window(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit) {
        return buffer(timespan, timeskip, unit, Integer.MAX_VALUE);
    }

    /**
     *
     * @param timespan
     * @param timeskip
     * @param unit
     * @param count
     * @return
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#window(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit, final int count) {
        N.checkArgument(timespan > 0, "timespan can't be 0 or negative");
        N.checkArgument(timeskip > 0, "timeskip can't be 0 or negative");
        N.checkArgNotNull(unit, "Time unit can't be null");
        N.checkArgument(count > 0, "count can't be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final long startTime = System.currentTimeMillis();
            private final long interval = timespan + timeskip;
            private final List<T> queue = new ArrayList<>();

            {
                scheduledFutures.put(schedulerForIntermediateOp.scheduleAtFixedRate(() -> {
                    List<T> list = null;
                    synchronized (queue) {
                        list = new ArrayList<>(queue);
                        queue.clear();
                    }

                    if (downDispatcher != null) {
                        downDispatcher.onNext(list);
                    }
                }, timespan, interval, unit), interval);
            }

            @Override
            public void onNext(final Object param) {
                if ((System.currentTimeMillis() - startTime) % interval <= timespan) {
                    List<T> list = null;

                    synchronized (queue) {
                        queue.add((T) param);

                        if (queue.size() == count) {
                            list = new ArrayList<>(queue);
                            queue.clear();
                        }
                    }

                    if (list != null && downDispatcher != null) {
                        downDispatcher.onNext(list);
                    }
                }
            }
        });

        return (Observer<List<T>>) this;
    }

    /**
     *
     * @param action
     */
    public void observe(final Consumer<? super T> action) {
        observe(action, ON_ERROR_MISSING);
    }

    /**
     *
     * @param action
     * @param onError
     */
    public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError) {
        observe(action, onError, EMPTY_ACTION);
    }

    /**
     *
     * @param action
     * @param onError
     * @param onComplete
     */
    public abstract void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete);

    /**
     * Cancel scheduled futures.
     */
    void cancelScheduledFutures() {
        final long startTime = System.currentTimeMillis();

        if (N.notNullOrEmpty(scheduledFutures)) {
            for (Map.Entry<ScheduledFuture<?>, Long> entry : scheduledFutures.entrySet()) {
                final long delay = entry.getValue();

                N.sleep(delay - (System.currentTimeMillis() - startTime)
                        + delay /* Extending another delay just want to make sure last schedule can be completed before the schedule task is cancelled*/);

                entry.getKey().cancel(false);
            }
        }
    }

    /**
     * The Class Node.
     *
     * @param <T>
     */
    protected static class Node<T> {

        /** The value. */
        public final T value;

        /** The next. */
        public Node<T> next;

        /**
         * Instantiates a new node.
         *
         * @param value
         */
        public Node(final T value) {
            this(value, null);
        }

        /**
         * Instantiates a new node.
         *
         * @param value
         * @param next
         */
        public Node(final T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    /**
     * The Class Dispatcher.
     *
     * @param <T>
     */
    protected static class Dispatcher<T> {

        /** The holder. */
        protected final Holder<Object> holder = Holder.of(N.NULL_MASK);

        /** The down dispatcher. */
        protected Dispatcher<T> downDispatcher;

        /**
         *
         * @param value
         */
        public void onNext(final T value) {
            if (downDispatcher != null) {
                downDispatcher.onNext(value);
            }
        }

        /**
         * Signal a Exception exception.
         * @param error the Exception to signal, not null
         */
        public void onError(final Exception error) {
            if (downDispatcher != null) {
                downDispatcher.onError(error);
            }
        }

        /**
         * Signal a completion.
         */
        public void onComplete() {
            if (downDispatcher != null) {
                downDispatcher.onComplete();
            }
        }

        /**
         *
         * @param downDispatcher
         */
        public void append(Dispatcher<T> downDispatcher) {
            Dispatcher<T> tmp = this;

            while (tmp.downDispatcher != null) {
                tmp = tmp.downDispatcher;
            }

            tmp.downDispatcher = downDispatcher;
        }
    }

    /**
     * The Class DispatcherBase.
     *
     * @param <T>
     */
    protected static abstract class DispatcherBase<T> extends Dispatcher<T> {

        /** The on error. */
        private final Consumer<? super Exception> onError;

        /** The on complete. */
        private final Runnable onComplete;

        /**
         * Instantiates a new dispatcher base.
         *
         * @param onError
         * @param onComplete
         */
        protected DispatcherBase(final Consumer<? super Exception> onError, final Runnable onComplete) {
            this.onError = onError;
            this.onComplete = onComplete;
        }

        /**
         *
         * @param error
         */
        @Override
        public void onError(final Exception error) {
            onError.accept(error);
        }

        /**
         * On complete.
         */
        @Override
        public void onComplete() {
            onComplete.run();
        }
    }

    /**
     * The Class ObserverBase.
     *
     * @param <T>
     */
    protected static abstract class ObserverBase<T> extends Observer<T> {

        /**
         * Instantiates a new observer base.
         */
        protected ObserverBase() {

        }
    }

    /**
     * An asynchronous update interface for receiving notifications
     * about BlockingQueue information as the BlockingQueue is constructed.
     *
     * @param <T>
     */
    static final class BlockingQueueObserver<T> extends ObserverBase<T> {

        /** The queue. */
        private final BlockingQueue<T> queue;

        /**
         * This method is called when information about an BlockingQueue
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param queue
         */
        BlockingQueueObserver(final BlockingQueue<T> queue) {
            this.queue = queue;
        }

        /**
         * This method is called when information about an BlockingQueue
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param action
         * @param onError
         * @param onComplete
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete) {
            N.checkArgNotNull(action, "action");

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(Object param) {
                    action.accept((T) param);
                }
            });

            asyncExecutor.execute(() -> {
                T next = null;
                boolean isOnError = true;

                try {
                    while (hasMore && (next = queue.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) != COMPLETE_FLAG) {
                        isOnError = false;

                        dispatcher.onNext(next);

                        isOnError = true;
                    }

                    isOnError = false;

                    onComplete.run();
                } catch (Exception e) {
                    if (isOnError) {
                        onError.accept(e);
                    } else {
                        throw ExceptionUtil.toRuntimeException(e);
                    }
                } finally {
                    cancelScheduledFutures();
                }
            });
        }
    }

    /**
     * An asynchronous update interface for receiving notifications
     * about Iterator information as the Iterator is constructed.
     *
     * @param <T>
     */
    static final class IteratorObserver<T> extends ObserverBase<T> {

        /** The iter. */
        private final Iterator<T> iter;

        /**
         * This method is called when information about an Iterator
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param iter
         */
        IteratorObserver(final Iterator<T> iter) {
            this.iter = iter;
        }

        /**
         * This method is called when information about an Iterator
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param action
         * @param onError
         * @param onComplete
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete) {
            N.checkArgNotNull(action, "action");

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(Object param) {
                    action.accept((T) param);
                }
            });

            asyncExecutor.execute(() -> {
                boolean isOnError = true;

                try {
                    while (hasMore && iter.hasNext()) {
                        isOnError = false;

                        dispatcher.onNext(iter.next());

                        isOnError = true;
                    }

                    isOnError = false;

                    onComplete.run();
                } catch (Exception e) {
                    if (isOnError) {
                        onError.accept(e);
                    } else {
                        throw ExceptionUtil.toRuntimeException(e);
                    }
                } finally {
                    cancelScheduledFutures();
                }
            });
        }

    }

    /**
     * An asynchronous update interface for receiving notifications
     * about Timer information as the Timer is constructed.
     *
     * @param <T>
     */
    static final class TimerObserver<T> extends ObserverBase<T> {

        /** The delay. */
        private final long delay;

        /** The unit. */
        private final TimeUnit unit;

        /**
         * This method is called when information about an Timer
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param delay
         * @param unit
         */
        TimerObserver(long delay, TimeUnit unit) {
            this.delay = delay;
            this.unit = unit;
        }

        /**
         * This method is called when information about an Timer
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param action
         * @param onError
         * @param onComplete
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete) {
            N.checkArgNotNull(action, "action");

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(Object param) {
                    action.accept((T) param);
                }
            });

            schedulerForObserveOp.schedule(() -> {
                try {
                    dispatcher.onNext(0L);

                    onComplete.run();
                } finally {
                    cancelScheduledFutures();
                }
            }, delay, unit);
        }
    }

    /**
     * An asynchronous update interface for receiving notifications
     * about Interval information as the Interval is constructed.
     *
     * @param <T>
     */
    static final class IntervalObserver<T> extends ObserverBase<T> {

        /** The initial delay. */
        private final long initialDelay;

        /** The period. */
        private final long period;

        /** The unit. */
        private final TimeUnit unit;

        /** The future. */
        private ScheduledFuture<?> future = null;

        /**
         * This method is called when information about an Interval
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param initialDelay
         * @param period
         * @param unit
         */
        IntervalObserver(long initialDelay, long period, TimeUnit unit) {
            this.initialDelay = initialDelay;
            this.period = period;
            this.unit = unit;
        }

        /**
         * This method is called when information about an Interval
         * which was previously requested using an asynchronous
         * interface becomes available.
         *
         * @param action
         * @param onError
         * @param onComplete
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete) {
            N.checkArgNotNull(action, "action");

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(Object param) {
                    action.accept((T) param);
                }
            });

            future = schedulerForObserveOp.scheduleAtFixedRate(new Runnable() {
                private long val = 0;

                @Override
                public void run() {
                    if (!hasMore) {
                        try {
                            dispatcher.onComplete();
                        } finally {
                            try {
                                future.cancel(true);
                            } finally {
                                cancelScheduledFutures();
                            }
                        }
                    } else {
                        try {
                            dispatcher.onNext(val++);
                        } catch (Exception e) {
                            try {
                                future.cancel(true);
                            } finally {
                                cancelScheduledFutures();
                            }
                        }
                    }
                }
            }, initialDelay, period, unit);
        }
    }
}
