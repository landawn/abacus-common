/*
 * Copyright (C) 2017 HaiYang Li
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
 * A reactive stream implementation that provides asynchronous event processing with operators
 * similar to RxJava's Observable. This class supports various stream operations including
 * filtering, mapping, throttling, and time-based operations.
 *
 * <p>The Observer pattern implementation allows for asynchronous data streams with cooperative
 * cancellation (via {@link #limit(long)}) and thread-safe operations. It provides a fluent API
 * for composing complex data processing pipelines.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Observer.of(Arrays.asList(1, 2, 3, 4, 5))
 *     .filter(n -> n % 2 == 0)
 *     .map(n -> n * 2)
 *     .observe(System.out::println);
 * }</pre>
 *
 * <p>This class implements the {@link Immutable} marker interface. Note that the intermediate
 * operator methods append to an internal dispatcher chain and return {@code this} for fluent
 * chaining rather than producing new instances.</p>
 *
 * @param <T> the type of elements emitted by this Observer
 * @see Timed
 */
@com.landawn.abacus.annotation.Immutable
public abstract class Observer<T> implements Immutable {

    private static final Object NONE = ClassUtil.newNullSentinel();

    private static final Object COMPLETE_FLAG = ClassUtil.newNullSentinel();

    /**
     * Multiplier applied to the configured interval duration when deciding whether to schedule
     * a new debounce/throttle task. A new task is scheduled only if no task has been scheduled
     * within the last {@code interval * INTERVAL_FACTOR} milliseconds, preventing runaway
     * task accumulation under high event rates.
     */
    protected static final double INTERVAL_FACTOR = 3;

    /** A no-op {@code Runnable} used as the default completion handler when none is specified. */
    protected static final Runnable EMPTY_ACTION = () -> {
        // Do nothing;
    };

    /**
     * Default error handler used when no {@code onError} consumer is supplied by the caller.
     * Rethrows the received {@code Exception} wrapped in a {@code RuntimeException}.
     */
    protected static final Consumer<Exception> ON_ERROR_MISSING = t -> {
        throw new RuntimeException(t);
    };

    /**
     * Shared thread pool used to execute the asynchronous emission loop for
     * {@link BlockingQueueObserver} and {@link IteratorObserver} subscriptions.
     * On Android, delegates to {@link AndroidUtil#getThreadPoolExecutor()}.
     */
    protected static final Executor asyncExecutor;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            asyncExecutor = AndroidUtil.getThreadPoolExecutor();
        } else {
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
                    N.max(64, IOUtil.CPU_CORES * 8), // coreThreadPoolSize
                    N.max(128, IOUtil.CPU_CORES * 16), // // maxThreadPoolSize
                    180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

            asyncExecutor = threadPoolExecutor;

            MoreExecutors.addDelayedShutdownHook(threadPoolExecutor, 120, TimeUnit.SECONDS);
        }
    }

    /**
     * Scheduled executor used by intermediate operators (debounce, throttle, delay, buffer)
     * to schedule deferred or periodic tasks within the processing pipeline.
     */
    protected static final ScheduledThreadPoolExecutor schedulerForIntermediateOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : N.max(64, IOUtil.CPU_CORES * 8));

    /**
     * Scheduled executor used by terminal observe operations ({@link TimerObserver},
     * {@link IntervalObserver}) to emit items on a scheduled basis.
     */
    protected static final ScheduledThreadPoolExecutor schedulerForObserveOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : N.max(64, IOUtil.CPU_CORES * 8));

    static {
        //    schedulerForIntermediateOp.setRemoveOnCancelPolicy(true);
        //    schedulerForObserveOp.setRemoveOnCancelPolicy(true);

        MoreExecutors.addDelayedShutdownHook(schedulerForIntermediateOp, 120, TimeUnit.SECONDS);
        MoreExecutors.addDelayedShutdownHook(schedulerForObserveOp, 120, TimeUnit.SECONDS);
    }

    /**
     * Map of scheduled futures to their associated delay (in milliseconds), maintained so that
     * {@link #cancelScheduledFutures()} can wait for each scheduled task to complete before
     * cancelling it, ensuring clean shutdown of buffer and sliding-window operators.
     */
    protected final Map<ScheduledFuture<?>, Long> scheduledFutures = new LinkedHashMap<>();

    /** The head of the dispatcher chain that receives items from the upstream source. */
    protected final Dispatcher<Object> dispatcher;

    /**
     * Flag that indicates whether the upstream source should continue producing items.
     * Set to {@code false} by the {@link #limit(long)} operator once the configured maximum
     * number of items has been emitted, causing the emission loop to exit.
     */
    protected volatile boolean hasMore = true;

    /** Creates a new Observer with a fresh, empty {@link Dispatcher} chain. */
    protected Observer() {
        this(new Dispatcher<>());
    }

    /**
     * Creates a new Observer that uses the given dispatcher as the head of its chain.
     *
     * @param dispatcher the head dispatcher for this Observer's pipeline; must not be {@code null}
     */
    protected Observer(final Dispatcher<Object> dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Signals completion to a {@code BlockingQueue} by adding a special internal completion
     * flag. This method is used to indicate that no more elements will be added to the queue,
     * causing an Observer created via {@link #of(BlockingQueue)} to stop polling and invoke
     * its completion handler. The flag is first offered to the queue and, if the queue is
     * full, is inserted with a blocking {@code put}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BlockingQueue<String> queue = new LinkedBlockingQueue<>();
     * // Add elements to queue...
     * Observer.complete(queue);   // Signal completion
     * }</pre>
     *
     * @param queue the {@code BlockingQueue} to complete
     * @throws RuntimeException if the current thread is interrupted while waiting to insert
     *         the completion flag into a full queue
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void complete(final BlockingQueue<?> queue) {
        if (!((Queue) queue).offer(COMPLETE_FLAG)) {
            try {
                ((BlockingQueue) queue).put(COMPLETE_FLAG);
            } catch (final InterruptedException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Creates an Observer from a BlockingQueue. When subscribed, the Observer blocks waiting
     * for elements and emits each one until it encounters the completion flag (added via
     * {@link #complete(BlockingQueue)}) or until emission is stopped by a downstream operator
     * such as {@link #limit(long)}. An empty queue does not stop emission; the Observer waits
     * indefinitely for the next element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
     * queue.offer(1);
     * queue.offer(2);
     * Observer.complete(queue);
     *
     * Observer.of(queue)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param <T> the type of elements in the queue
     * @param queue the {@code BlockingQueue} to create an Observer from
     * @return a new Observer that emits elements from the queue
     * @throws IllegalArgumentException if {@code queue} is {@code null}
     * @see #complete(BlockingQueue)
     */
    public static <T> Observer<T> of(final BlockingQueue<T> queue) throws IllegalArgumentException {
        N.checkArgNotNull(queue, cs.queue);

        return new BlockingQueueObserver<>(queue);
    }

    /**
     * Creates an Observer from a Collection. The Observer will emit all elements
     * from the collection in iteration order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * Observer.of(list)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param <T> the type of elements in the collection
     * @param c the Collection to create an Observer from; may be {@code null} or empty, in
     *        which case the resulting Observer emits no elements
     * @return a new Observer that emits elements from the collection
     */
    public static <T> Observer<T> of(final Collection<? extends T> c) {
        return of(N.isEmpty(c) ? ObjIterator.empty() : c.iterator());
    }

    /**
     * Creates an Observer from an Iterator. The Observer will emit all elements
     * from the iterator until hasNext() returns {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<Integer> iter = Arrays.asList(1, 2, 3).iterator();
     * Observer.of(iter)
     *     .map(n -> n * 2)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param iter the Iterator to create an Observer from
     * @return a new Observer that emits elements from the iterator
     * @throws IllegalArgumentException if {@code iter} is {@code null}
     */
    public static <T> Observer<T> of(final Iterator<? extends T> iter) throws IllegalArgumentException {
        N.checkArgNotNull(iter, cs.iterator);

        return new IteratorObserver<>(iter);
    }

    /**
     * Creates an Observer that emits a single value (0L) after the specified delay in milliseconds.
     * This is useful for creating delayed actions or timeouts.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.timer(1000) // Wait 1 second
     *     .observe(value -> System.out.println("Timer fired!"));
     * }</pre>
     *
     * @param delayInMillis the delay in milliseconds before emitting
     * @return a new Observer that emits a single {@code 0L} after the delay and then completes
     * @throws IllegalArgumentException if {@code delayInMillis} is negative
     * @see #timer(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#timer(long,%20java.util.concurrent.TimeUnit)">RxJava#timer</a>
     */
    public static Observer<Long> timer(final long delayInMillis) {
        return timer(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates an Observer that emits a single value (0L) after the specified delay.
     * This is useful for creating delayed actions or timeouts with custom time units.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.timer(5, TimeUnit.SECONDS)
     *     .observe(value -> System.out.println("5 seconds elapsed!"));
     * }</pre>
     *
     * @param delay the delay before emitting
     * @param unit the time unit of the delay
     * @return a new Observer that emits a single {@code 0L} after the delay and then completes
     * @throws IllegalArgumentException if {@code delay} is negative or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#timer(long,%20java.util.concurrent.TimeUnit)">RxJava#timer</a>
     */
    public static Observer<Long> timer(final long delay, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(delay >= 0, "delay cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null"); //NOSONAR

        return new TimerObserver<>(delay, unit);
    }

    /**
     * Creates an Observer that emits sequential numbers (0, 1, 2, ...) periodically
     * with the specified interval in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(1000) // Emit every second
     *     .limit(5)
     *     .observe(System.out::println);   // Prints 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param periodInMillis the period between emissions in milliseconds; the first value
     *        ({@code 0L}) is emitted immediately with no initial delay
     * @return a new Observer that emits sequential {@code Long} values periodically
     * @throws IllegalArgumentException if {@code periodInMillis} is zero or negative
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long periodInMillis) {
        return interval(0, periodInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates an Observer that emits sequential numbers periodically with an initial delay
     * and specified interval in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(2000, 1000) // Wait 2s, then emit every 1s
     *     .limit(3)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param initialDelayInMillis the initial delay before the first emission in milliseconds
     * @param periodInMillis the period between subsequent emissions in milliseconds
     * @return a new Observer that emits sequential {@code Long} values periodically
     * @throws IllegalArgumentException if {@code initialDelayInMillis} is negative or
     *         {@code periodInMillis} is zero or negative
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long initialDelayInMillis, final long periodInMillis) {
        return interval(initialDelayInMillis, periodInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates an Observer that emits sequential numbers periodically with the specified
     * period and time unit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(2, TimeUnit.SECONDS)
     *     .observe(n -> System.out.println("Tick: " + n));
     * }</pre>
     *
     * @param period the period between emissions; the first value ({@code 0L}) is emitted
     *        immediately with no initial delay
     * @param unit the time unit of the period
     * @return a new Observer that emits sequential {@code Long} values periodically
     * @throws IllegalArgumentException if {@code period} is zero or negative or {@code unit}
     *         is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long period, final TimeUnit unit) {
        return interval(0, period, unit);
    }

    /**
     * Creates an Observer that emits sequential numbers periodically with an initial delay,
     * period, and time unit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(5, 2, TimeUnit.SECONDS) // Wait 5s, then emit every 2s
     *     .map(n -> "Event " + n)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param initialDelay the initial delay before the first emission
     * @param period the period between subsequent emissions
     * @param unit the time unit for both delay and period
     * @return a new Observer that emits periodically
     * @throws IllegalArgumentException if initialDelay is negative, period is non-positive, or unit is null
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long initialDelay, final long period, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(initialDelay >= 0, "initialDelay cannot be negative");
        N.checkArgument(period > 0, "period cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        return new IntervalObserver<>(initialDelay, period, unit);
    }

    /**
     * Applies debounce operator that only emits an item if a particular timespan has passed
     * without emitting another item. This is useful for handling rapid events like user input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * userInputObserver
     *     .debounce(300) // Wait 300ms after last input
     *     .observe(text -> performSearch(text));
     * }</pre>
     *
     * @param intervalDurationInMillis the debounce interval in milliseconds; if zero, this
     *        Observer is returned unchanged with no debounce applied
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative
     * @see #debounce(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDurationInMillis) {
        return debounce(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies debounce operator that only emits an item if a particular timespan has passed
     * without emitting another item. The time interval can be specified with custom units.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * sensorDataObserver
     *     .debounce(1, TimeUnit.SECONDS)
     *     .observe(data -> processSensorData(data));
     * }</pre>
     *
     * @param intervalDuration the debounce interval; if zero, this Observer is returned
     *        unchanged with no debounce applied
     * @param unit the time unit of the interval
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit}
     *         is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Interval cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInMillis = unit.toMillis(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            private long prevTimestamp = 0;
            private long lastScheduledTime = 0;

            @Override
            public void onNext(final Object param) {
                synchronized (holder) {
                    final long now = System.currentTimeMillis();

                    if (holder.value() == NONE || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
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
                                holder.setValue(NONE);
                            }

                            if (lastParam != NONE && downDispatcher != null) {
                                downDispatcher.onNext(lastParam);
                            }
                        } else {
                            schedule(intervalDurationInMillis - pastIntervalInMills, TimeUnit.MILLISECONDS);
                        }
                    }, delay, unit);

                    lastScheduledTime = System.currentTimeMillis();
                } catch (final Exception e) {
                    holder.setValue(NONE);

                    if (downDispatcher != null) {
                        downDispatcher.onError(e);
                    }
                }
            }
        });

        return this;
    }

    /**
     * Applies throttleFirst operator that only emits the first item emitted during sequential
     * time windows of a specified duration. Subsequent items are ignored until the window expires.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * buttonClickObserver
     *     .throttleFirst(1000) // Prevent rapid clicks
     *     .observe(click -> handleButtonClick());
     * }</pre>
     *
     * @param intervalDurationInMillis the throttle window duration in milliseconds; if zero,
     *        this Observer is returned unchanged with no throttling applied
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative
     * @see #throttleFirst(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDurationInMillis) {
        return throttleFirst(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies throttleFirst operator that only emits the first item emitted during sequential
     * time windows of a specified duration with custom time units.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * apiCallObserver
     *     .throttleFirst(5, TimeUnit.SECONDS) // Rate limit API calls
     *     .observe(request -> makeApiCall(request));
     * }</pre>
     *
     * @param intervalDuration the throttle window duration; if zero, this Observer is returned
     *        unchanged with no throttling applied
     * @param unit the time unit of the interval
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit}
     *         is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Interval cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

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

                    if (holder.value() == NONE || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
                        holder.setValue(param);

                        try {
                            schedulerForIntermediateOp.schedule(() -> {
                                Object firstParam = null;

                                synchronized (holder) {
                                    firstParam = holder.value();
                                    holder.setValue(NONE);
                                }

                                if (firstParam != NONE && downDispatcher != null) {
                                    downDispatcher.onNext(firstParam);
                                }
                            }, intervalDuration, unit);

                            lastScheduledTime = now;
                        } catch (final Exception e) {
                            holder.setValue(NONE);

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
     * Applies throttleLast operator that only emits the last item emitted during sequential
     * time windows of a specified duration. Also known as "sample" in some reactive libraries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * mousePositionObserver
     *     .throttleLast(100) // Sample mouse position every 100ms
     *     .observe(pos -> updateCursorPosition(pos));
     * }</pre>
     *
     * @param intervalDurationInMillis the sampling window duration in milliseconds; if zero,
     *        this Observer is returned unchanged with no throttling applied
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative
     * @see #throttleLast(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDurationInMillis) {
        return throttleLast(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies throttleLast operator that only emits the last item emitted during sequential
     * time windows of a specified duration with custom time units.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * temperatureSensor
     *     .throttleLast(1, TimeUnit.MINUTES) // Sample every minute
     *     .observe(temp -> logTemperature(temp));
     * }</pre>
     *
     * @param intervalDuration the sampling window duration; if zero, this Observer is returned
     *        unchanged with no throttling applied
     * @param unit the time unit of the interval
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit}
     *         is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Delay cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

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

                    if (holder.value() == NONE || now - lastScheduledTime > intervalDurationInMillis * INTERVAL_FACTOR) {
                        holder.setValue(param);

                        try {
                            schedulerForIntermediateOp.schedule(() -> {
                                Object lastParam = null;

                                synchronized (holder) {
                                    lastParam = holder.value();
                                    holder.setValue(NONE);
                                }

                                if (lastParam != NONE && downDispatcher != null) {
                                    downDispatcher.onNext(lastParam);
                                }
                            }, intervalDuration, unit);

                            lastScheduledTime = now;
                        } catch (final Exception e) {
                            holder.setValue(NONE);

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
     * Delays the start of emissions by the specified duration in milliseconds. The delay is
     * measured from the time this operator is applied; once the initial delay has elapsed,
     * the first and all subsequent items are emitted without further per-item delay.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList("A", "B", "C"))
     *     .delay(1000) // Delay the first emission by 1 second
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param delayInMillis the delay duration in milliseconds; if zero, this Observer is
     *        returned unchanged with no delay applied
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code delayInMillis} is negative
     * @see #delay(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delayInMillis) {
        return delay(delayInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Delays the emission of items by the specified duration with custom time units.
     * The delay is measured from the time this operator is applied; subsequent items
     * are emitted immediately after the initial delay has elapsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.timer(0)
     *     .delay(3, TimeUnit.SECONDS)
     *     .observe(n -> System.out.println("Delayed action"));
     * }</pre>
     *
     * @param delay the delay duration; if zero, this Observer is returned unchanged with no
     *        delay applied
     * @param unit the time unit of the delay
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code delay} is negative or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delay, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(delay >= 0, "Delay cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (delay == 0) {
            return this;
        }

        dispatcher.append(new Dispatcher<>() {
            private final long startTime = System.currentTimeMillis();
            private volatile boolean isDelayed = false;

            @Override
            public void onNext(final Object param) {
                if (!isDelayed) {
                    N.sleepUninterruptibly(Math.max(0, unit.toMillis(delay) - (System.currentTimeMillis() - startTime)));
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
     * Transforms items into Timed objects that contain the time interval between
     * consecutive emissions in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(1000)
     *     .timeInterval()
     *     .observe(timed -> System.out.println("Value: " + timed.value() +
     *                                          ", Interval: " + timed.timestamp() + "ms"));
     * }</pre>
     *
     * @return this Observer instance (re-typed) emitting {@code Timed<T>} objects whose
     *         timestamp field holds the elapsed interval in milliseconds since the previous
     *         emission
     * @see Timed
     * @see #timestamp()
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html#timeInterval()">RxJava#timeInterval</a>
     */
    public Observer<Timed<T>> timeInterval() {
        dispatcher.append(new Dispatcher<>() {
            private long startTime = System.currentTimeMillis();

            @Override
            public synchronized void onNext(final Object param) {
                if (downDispatcher != null) {
                    final long now = System.currentTimeMillis();
                    final long intervalInMillis = now - startTime;
                    startTime = now;

                    downDispatcher.onNext(Timed.of(param, intervalInMillis));
                }
            }
        });

        return (Observer<Timed<T>>) this;
    }

    /**
     * Transforms items into Timed objects that contain the timestamp of emission
     * in milliseconds since epoch.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventObserver
     *     .timestamp()
     *     .observe(timed -> System.out.println("Event: " + timed.value() +
     *                                          " at " + new Date(timed.timestamp())));
     * }</pre>
     *
     * @return this Observer instance (re-typed) emitting {@code Timed<T>} objects whose
     *         timestamp field holds the emission time in milliseconds since the epoch
     * @see Timed
     * @see #timeInterval()
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
     * Skips the first n items emitted by this Observer and emits the remaining items.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList(1, 2, 3, 4, 5))
     *     .skip(2)
     *     .observe(System.out::println);   // Prints: 3, 4, 5
     * }</pre>
     *
     * @param n the number of items to skip; if zero, no items are skipped (a negative
     *        value is rejected as described below)
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public Observer<T> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

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
     * Limits the number of items emitted by this Observer to at most maxSize items.
     * After emitting maxSize items, the Observer completes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(100)
     *     .limit(5)
     *     .observe(System.out::println);   // Prints: 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param maxSize the maximum number of items to emit; once this count is reached the
     *        upstream source is signaled to stop producing further items
     * @return this Observer instance for method chaining
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    public Observer<T> limit(final long maxSize) throws IllegalArgumentException {
        N.checkArgNotNegative(maxSize, cs.maxSize);

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

    /**
     * Filters out duplicate items, ensuring each unique item is emitted only once.
     * Uniqueness is determined by the item's {@code hashCode()} and {@code equals()}
     * methods (a {@code HashSet} of seen items is maintained).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList(1, 2, 2, 3, 1, 3))
     *     .distinct()
     *     .observe(System.out::println);   // Prints: 1, 2, 3
     * }</pre>
     *
     * @return this Observer instance for method chaining
     * @see #distinctBy(Function)
     */
    public Observer<T> distinct() {
        dispatcher.append(new Dispatcher<>() {
            private final Set<T> set = N.newHashSet();

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
     * Filters out items with duplicate keys as determined by the keyExtractor function.
     * Only the first item with each unique key is emitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList("apple", "banana", "apricot", "blueberry"))
     *     .distinctBy(s -> s.charAt(0)) // Distinct by first letter
     *     .observe(System.out::println);   // Prints: apple, banana
     * }</pre>
     *
     * @param keyExtractor function to extract the key used to determine uniqueness; key
     *        equality is based on {@code hashCode()} and {@code equals()}; must not be {@code null}
     * @return this Observer instance for method chaining
     * @see #distinct()
     */
    public Observer<T> distinctBy(final Function<? super T, ?> keyExtractor) {
        dispatcher.append(new Dispatcher<>() {
            private final Set<Object> set = N.newHashSet();

            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && set.add(keyExtractor.apply((T) param))) { // onError if keyExtractor.apply throws exception?
                    downDispatcher.onNext(param);
                }
            }
        });

        return this;
    }

    /**
     * Filters items emitted by this Observer, only emitting those that satisfy the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList(1, 2, 3, 4, 5))
     *     .filter(n -> n % 2 == 0)
     *     .observe(System.out::println);   // Prints: 2, 4
     * }</pre>
     *
     * @param filter the predicate used to test each item; items for which the predicate returns
     *               {@code true} are forwarded downstream; must not be {@code null}
     * @return this Observer instance for method chaining
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
     * Transforms each item emitted by this Observer by applying a mapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList(1, 2, 3))
     *     .map(n -> n * n)
     *     .observe(System.out::println);   // Prints: 1, 4, 9
     * }</pre>
     *
     * @param <R> the type of items emitted after transformation
     * @param mapper the function to transform each item; must not be {@code null}
     * @return this Observer instance, re-typed as {@code Observer<R>}, emitting the
     *         transformed items
     * @see #flatMap(Function)
     */
    public <R> Observer<R> map(final Function<? super T, R> mapper) {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    downDispatcher.onNext(mapper.apply((T) param)); // onError if map.apply throws exception?
                }
            }
        });

        return (Observer<R>) this;
    }

    /**
     * Transforms each item into a collection and flattens the results into a single sequence.
     * This is useful for one-to-many transformations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList("Hello World", "Foo Bar"))
     *     .flatMap(s -> Arrays.asList(s.split(" ")))
     *     .observe(System.out::println);   // Prints: Hello, World, Foo, Bar
     * }</pre>
     *
     * @param <R> the type of items in the flattened sequence
     * @param mapper function that transforms each item into a collection; if it returns
     *        {@code null} or an empty collection, no items are emitted for that input;
     *        must not be {@code null}
     * @return this Observer instance, re-typed as {@code Observer<R>}, emitting the
     *         flattened items
     * @see #map(Function)
     */
    public <R> Observer<R> flatMap(final Function<? super T, ? extends Collection<? extends R>> mapper) {
        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    final Collection<? extends R> c = mapper.apply((T) param); // onError if map.apply throws exception?

                    if (N.notEmpty(c)) {
                        for (final R u : c) {
                            downDispatcher.onNext(u);
                        }
                    }
                }
            }
        });

        return (Observer<R>) this;
    }

    /**
     * Buffers items into lists based on a time window. Emits a list of items
     * every timespan period.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * eventObserver
     *     .buffer(1000, TimeUnit.MILLISECONDS)
     *     .observe(list -> System.out.println("Events in last second: " + list.size()));
     * }</pre>
     *
     * @param timespan the time window duration
     * @param unit the time unit of the timespan
     * @return this Observer instance (re-typed) emitting {@code List<T>} buffers of items
     * @throws IllegalArgumentException if {@code timespan} is zero or negative or {@code unit}
     *         is {@code null}
     * @see #buffer(long, TimeUnit, int)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit) {
        return buffer(timespan, unit, Integer.MAX_VALUE);
    }

    /**
     * Buffers items into lists based on time windows or item count, whichever occurs first.
     * Emits when either the time window expires or the buffer reaches the specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * dataObserver
     *     .buffer(5000, TimeUnit.MILLISECONDS, 10)
     *     .observe(batch -> processBatch(batch));
     * }</pre>
     *
     * @param timespan the time window duration
     * @param unit the time unit of the timespan
     * @param count the maximum number of items per buffer
     * @return this Observer instance (re-typed) emitting {@code List<T>} buffers of items
     * @throws IllegalArgumentException if {@code timespan} or {@code count} is zero or
     *         negative, or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit,%20int)">RxJava#buffer(long, java.util.concurrent.TimeUnit, int)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit, final int count) throws IllegalArgumentException {
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final List<T> queue = new ArrayList<>();

            { //NOSONAR
                scheduledFutures.put(schedulerForIntermediateOp.scheduleAtFixedRate(() -> {
                    List<T> list = null;
                    synchronized (queue) {
                        list = new ArrayList<>(queue);
                        queue.clear();
                    }

                    if (downDispatcher != null) {
                        downDispatcher.onNext(list);
                    }
                }, timespan, timespan, unit), Math.max(1L, unit.toMillis(timespan)));
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
     * Buffers items into lists based on sliding time windows. Creates overlapping or
     * gapped windows based on the timespan and timeskip parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Overlapping windows: every 3 seconds, emit items from last 5 seconds
     * observer.buffer(5, 3, TimeUnit.SECONDS)
     *     .observe(window -> System.out.println("Window: " + window));
     * }</pre>
     *
     * @param timespan the duration of each buffer window
     * @param timeskip the interval between starting new buffers
     * @param unit the time unit for both timespan and timeskip
     * @return this Observer instance (re-typed) emitting {@code List<T>} buffers of items
     * @throws IllegalArgumentException if {@code timespan} or {@code timeskip} is zero or
     *         negative, or {@code unit} is {@code null}
     * @see #buffer(long, long, TimeUnit, int)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit) {
        return buffer(timespan, timeskip, unit, Integer.MAX_VALUE);
    }

    /**
     * Buffers items into lists based on sliding time windows with a maximum count per buffer.
     * Creates overlapping or gapped windows that emit when either the window duration
     * expires or the count is reached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * sensorData
     *     .buffer(10, 5, TimeUnit.SECONDS, 100) // 10s windows every 5s, max 100 items
     *     .observe(window -> analyzeWindow(window));
     * }</pre>
     *
     * @param timespan the duration of each buffer window
     * @param timeskip the interval between starting new buffers
     * @param unit the time unit for both timespan and timeskip
     * @param count the maximum number of items per buffer
     * @return this Observer instance (re-typed) emitting {@code List<T>} buffers of items
     * @throws IllegalArgumentException if {@code timespan}, {@code timeskip}, or {@code count}
     *         is zero or negative, or {@code unit} is {@code null}
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit, final int count) throws IllegalArgumentException {
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgument(timeskip > 0, "timeskip cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final long startNanos = System.nanoTime();
            private final long intervalNanos = unit.toNanos(timeskip);
            private final long timespanNanos = unit.toNanos(timespan);
            private final List<T> queue = new ArrayList<>();

            { //NOSONAR
                scheduledFutures.put(schedulerForIntermediateOp.scheduleAtFixedRate(() -> {
                    List<T> list = null;
                    synchronized (queue) {
                        list = new ArrayList<>(queue);
                        queue.clear();
                    }

                    if (downDispatcher != null) {
                        downDispatcher.onNext(list);
                    }
                }, timespan, timeskip, unit), Math.max(1L, unit.toMillis(timeskip)));
            }

            @Override
            public void onNext(final Object param) {
                if ((System.nanoTime() - startNanos) % intervalNanos <= timespanNanos) {
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
     * Subscribes to this Observer with an action to be performed on each emitted item.
     * If an error occurs, it is rethrown as a {@code RuntimeException} (the default missing
     * error handler). Subscription is asynchronous; this method returns immediately and
     * items are delivered on a background thread.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList(1, 2, 3))
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param action the action to perform on each item
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @see #observe(Consumer, Consumer)
     * @see #observe(Consumer, Consumer, Runnable)
     */
    public void observe(final Consumer<? super T> action) {
        observe(action, ON_ERROR_MISSING);
    }

    /**
     * Subscribes to this Observer with actions for items and errors.
     * The completion action is an empty no-op action. Subscription is asynchronous; this
     * method returns immediately and items are delivered on a background thread.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * dataObserver.observe(
     *     data -> processData(data),
     *     error -> logger.error("Error processing data", error)
     * );
     * }</pre>
     *
     * @param action the action to perform on each item
     * @param onError the action to perform on error
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @see #observe(Consumer, Consumer, Runnable)
     */
    public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError) {
        observe(action, onError, EMPTY_ACTION);
    }

    /**
     * Subscribes to this Observer with actions for items, errors, and completion.
     * This is the most complete form of subscription. Subscription is asynchronous; this
     * method returns immediately and items are delivered on a background thread. Concrete
     * implementations validate their arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * fileProcessor.observe(
     *     line -> processLine(line),
     *     error -> logger.error("Error reading file", error),
     *     () -> logger.info("File processing completed")
     * );
     * }</pre>
     *
     * @param action the action to perform on each item
     * @param onError the action to perform on error
     * @param onComplete the action to perform on completion
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    public abstract void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete);

    /**
     * Cancels all scheduled futures associated with this Observer.
     * This method is called internally during cleanup to ensure proper resource management.
     */
    void cancelScheduledFutures() {
        final long startTime = System.currentTimeMillis();

        if (N.notEmpty(scheduledFutures)) {
            for (final Map.Entry<ScheduledFuture<?>, Long> entry : scheduledFutures.entrySet()) {
                final long delay = entry.getValue();

                N.sleepUninterruptibly(Math.max(0, delay - (System.currentTimeMillis() - startTime)
                        + delay /* Extending another delay just wants to make sure the last schedule can be completed before the schedule task is canceled*/));

                entry.getKey().cancel(false);
            }
        }
    }

    /**
     * Internal dispatcher class that handles the chaining of operators and event propagation
     * through the Observer pipeline. Each operator appends a new Dispatcher to the chain.
     *
     * @param <T> the type of items handled by this dispatcher
     */
    protected static class Dispatcher<T> {

        /** Holds the most-recently-received item for debounce/throttle operators; initialised to the sentinel {@code NONE}. */
        protected final Holder<Object> holder = Holder.of(NONE);

        /** The next dispatcher in the chain, or {@code null} if this is the last dispatcher. */
        protected Dispatcher<T> downDispatcher;

        /**
         * Propagates an item to the next dispatcher in the chain.
         *
         * @param value the item to propagate
         */
        public void onNext(final T value) {
            if (downDispatcher != null) {
                downDispatcher.onNext(value);
            }
        }

        /**
         * Signals an exception to the next dispatcher in the chain.
         *
         * @param error the exception to signal; must not be {@code null}
         */
        public void onError(final Exception error) {
            if (downDispatcher != null) {
                downDispatcher.onError(error);
            }
        }

        /**
         * Signals completion to the next dispatcher in the chain.
         */
        public void onComplete() {
            if (downDispatcher != null) {
                downDispatcher.onComplete();
            }
        }

        /**
         * Appends the given dispatcher to the tail of this dispatcher chain,
         * making it the new last element.
         *
         * @param downDispatcher the dispatcher to append; must not be {@code null}
         */
        public void append(final Dispatcher<T> downDispatcher) {
            Dispatcher<T> tmp = this;

            while (tmp.downDispatcher != null) {
                tmp = tmp.downDispatcher;
            }

            tmp.downDispatcher = downDispatcher;
        }
    }

    /**
     * Base class for terminal dispatchers that handle the final subscription callbacks
     * including onError and onComplete handlers.
     *
     * @param <T> the type of items handled by this dispatcher
     */
    protected abstract static class DispatcherBase<T> extends Dispatcher<T> {

        /** The consumer invoked when an error is signalled. */
        private final Consumer<? super Exception> onError;

        /** The runnable invoked when the stream completes normally. */
        private final Runnable onComplete;

        /**
         * Constructs a terminal dispatcher with the given error and completion handlers.
         *
         * @param onError the consumer to invoke when an error is signalled; must not be {@code null}
         * @param onComplete the runnable to invoke when the stream completes; must not be {@code null}
         */
        protected DispatcherBase(final Consumer<? super Exception> onError, final Runnable onComplete) {
            this.onError = onError;
            this.onComplete = onComplete;
        }

        @Override
        public void onError(final Exception error) {
            onError.accept(error);
        }

        @Override
        public void onComplete() {
            onComplete.run();
        }
    }

    /**
     * Base class for concrete Observer implementations. Provides a foundation
     * for specific Observer types like BlockingQueueObserver and IteratorObserver.
     *
     * @param <T> the type of items emitted by this Observer
     */
    protected abstract static class ObserverBase<T> extends Observer<T> {

        /** Creates a new {@code ObserverBase} instance with a default empty dispatcher chain. */
        protected ObserverBase() {

        }
    }

    /**
     * Observer implementation that emits items from a BlockingQueue.
     * Items are pulled from the queue asynchronously until a completion flag is encountered
     * or the queue is empty.
     *
     * @param <T> the type of items in the queue
     */
    static final class BlockingQueueObserver<T> extends ObserverBase<T> {

        /** The blocking queue from which items are pulled during subscription. */
        private final BlockingQueue<T> queue;

        BlockingQueueObserver(final BlockingQueue<T> queue) {
            this.queue = queue;
        }

        /**
         * Subscribes to the BlockingQueue with the specified handlers.
         * Items are polled from the queue on a background thread until {@link Observer#complete(BlockingQueue)}
         * has been called or {@link #hasMore} is set to {@code false}.
         *
         * @param action the action to perform on each item; must not be {@code null}
         * @param onError the consumer invoked if an exception occurs during emission
         * @param onComplete the runnable invoked when the queue signals completion
         * @throws IllegalArgumentException if {@code action} is {@code null}
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalArgumentException {
            N.checkArgNotNull(action, "action"); //NOSONAR

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            asyncExecutor.execute(() -> {
                T next = null;
                boolean isOnError = true;

                try {
                    while (hasMore && (next = queue.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) != COMPLETE_FLAG) {
                        isOnError = true;

                        dispatcher.onNext(next);

                        isOnError = false;
                    }

                    isOnError = false;

                    onComplete.run();
                } catch (final Exception e) {
                    if (isOnError) {
                        onError.accept(e);
                    } else {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                } finally {
                    cancelScheduledFutures();
                }
            });
        }
    }

    /**
     * Observer implementation that emits items from an Iterator.
     * Items are pulled from the iterator asynchronously until hasNext() returns {@code false}.
     *
     * @param <T> the type of items in the iterator
     */
    static final class IteratorObserver<T> extends ObserverBase<T> {

        /** The iterator from which items are pulled during subscription. */
        private final Iterator<? extends T> iter;

        IteratorObserver(final Iterator<? extends T> iter) {
            this.iter = iter;
        }

        /**
         * Subscribes to the Iterator with the specified handlers.
         * Items are pulled from the iterator on a background thread until
         * {@code iter.hasNext()} returns {@code false} or {@link #hasMore} is set to {@code false}.
         *
         * @param action the action to perform on each item; must not be {@code null}
         * @param onError the consumer invoked if an exception occurs during emission
         * @param onComplete the runnable invoked when the iterator is exhausted
         * @throws IllegalArgumentException if {@code action} is {@code null}
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalArgumentException {
            N.checkArgNotNull(action, cs.action);

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            asyncExecutor.execute(() -> {
                boolean isOnError = true;

                try {
                    while (hasMore && iter.hasNext()) {
                        isOnError = true;

                        dispatcher.onNext(iter.next());

                        isOnError = false;
                    }

                    isOnError = false;

                    onComplete.run();
                } catch (final Exception e) {
                    if (isOnError) {
                        onError.accept(e);
                    } else {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                } finally {
                    cancelScheduledFutures();
                }
            });
        }

    }

    /**
     * Observer implementation that emits a single value after a specified delay.
     * Used by the timer() factory method to create delayed emissions.
     *
     * @param <T> the type of items emitted (always Long for timers)
     */
    static final class TimerObserver<T> extends ObserverBase<T> {

        /** The delay before the single emission, expressed in {@link #unit}. */
        private final long delay;

        /** The time unit for {@link #delay}. */
        private final TimeUnit unit;

        TimerObserver(final long delay, final TimeUnit unit) {
            this.delay = delay;
            this.unit = unit;
        }

        /**
         * Subscribes to the timer with the specified handlers.
         * Schedules a single emission of {@code 0L} after the configured delay on
         * {@link Observer#schedulerForObserveOp}, then signals completion.
         *
         * @param action the action to perform when the timer fires; must not be {@code null}
         * @param onError the consumer invoked if an exception occurs during the scheduled emission
         * @param onComplete the runnable invoked immediately after the single emission
         * @throws IllegalArgumentException if {@code action} is {@code null}
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalArgumentException {
            N.checkArgNotNull(action, cs.action);

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            schedulerForObserveOp.schedule(() -> {
                try {
                    dispatcher.onNext(0L);
                    dispatcher.onComplete();
                } catch (final Exception e) {
                    dispatcher.onError(e);
                } finally {
                    cancelScheduledFutures();
                }
            }, delay, unit);
        }
    }

    /**
     * Observer implementation that emits sequential numbers at fixed intervals.
     * Used by the interval() factory methods to create periodic emissions.
     *
     * @param <T> the type of items emitted (always Long for intervals)
     */
    static final class IntervalObserver<T> extends ObserverBase<T> {

        /** The delay before the first emission, expressed in {@link #unit}. */
        private final long initialDelay;

        /** The period between successive emissions, expressed in {@link #unit}. */
        private final long period;

        /** The time unit for {@link #initialDelay} and {@link #period}. */
        private final TimeUnit unit;

        /** The scheduled future for the fixed-rate task; cancelled when {@link #hasMore} becomes {@code false}. */
        private volatile ScheduledFuture<?> future = null;

        IntervalObserver(final long initialDelay, final long period, final TimeUnit unit) {
            this.initialDelay = initialDelay;
            this.period = period;
            this.unit = unit;
        }

        /**
         * Subscribes to the interval with the specified handlers.
         * Schedules a fixed-rate task on {@link Observer#schedulerForObserveOp} that emits
         * sequential {@code Long} values starting from {@code 0} after {@code initialDelay},
         * then every {@code period}. Cancels the task when {@link #hasMore} becomes {@code false}.
         *
         * @param action the action to perform on each emission; must not be {@code null}
         * @param onError the consumer invoked if an exception occurs during an emission
         * @param onComplete the runnable invoked when the interval is cancelled
         * @throws IllegalArgumentException if {@code action} is {@code null}
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalArgumentException {
            N.checkArgNotNull(action, cs.action);

            dispatcher.append(new DispatcherBase<>(onError, onComplete) {
                @Override
                public void onNext(final Object param) {
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
                        } catch (final Exception e) {
                            dispatcher.onError(e);
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
                        } catch (final Exception e) {
                            try {
                                dispatcher.onError(e);
                            } finally {
                                future.cancel(true);
                                cancelScheduledFutures();
                            }
                        }
                    }
                }
            }, initialDelay, period, unit);
        }
    }
}
