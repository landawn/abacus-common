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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
 * cancellation (via {@link #limit(long)}). Event delivery by the built-in timed operators is
 * serialized where required, but pipeline construction is not thread-safe. An instance is a
 * single-use pipeline and accepts exactly one subscription.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Observer.of(Arrays.asList(1, 2, 3, 4, 5))
 *     .filter(n -> n % 2 == 0)
 *     .map(n -> n * 2)
 *     .observe(System.out::println);
 * }</pre>
 *
 * <p>The pipeline is mutable and construction is not thread-safe. Same-type operators return the
 * current stage; type-changing operators return a new typed stage and invalidate previous handles.
 * Continue and subscribe through the returned stage. Stale handles and all operators after subscription
 * throw {@link IllegalStateException}, including no-op operators. Invalid arguments preserve the current
 * stage; an unexpected failure while appending a type-changing operator seals the pipeline.</p>
 *
 * <p>Custom sources must use the protected subscription and dispatcher hooks and deliver terminal values
 * generically. Typed stages forward subscription to the source owner with authorization limited to the
 * calling thread and cleared on return. Intermediate operators on a typed stage use private owner
 * implementations; custom public operator overrides on the original source are not reinvoked.</p>
 *
 * <p>Source and timed operator callbacks are serialized for each subscription. Reaching a limit or
 * encountering an exception stops source scheduling, wakes a pending queue poll, releases timed
 * operator state, and delivers at most one terminal callback. Normal completion flushes downstream
 * buffers within their limits; errors discard pending values. Caller-owned queues are not closed.
 * Already-running caller code, including iterator methods and callbacks, must return cooperatively.</p>
 * <p>A fatal {@link Error} also stops the subscription and releases timed state, then propagates
 * unchanged from the failing task. It does not invoke an additional terminal callback. Failures
 * while submitting or starting source work likewise release resources before propagating.</p>
 *
 * <p>Every thread an observer runs on is a daemon thread, and the shared pools are stopped without waiting
 * when the JVM exits: a subscription that is still running is interrupted, receives no terminal callback,
 * and does not delay the exit. Complete a queue source with {@link #complete(BlockingQueue)}, or bound the
 * subscription with {@link #limit(long)}, when a subscriber has to finish before the JVM goes away.</p>
 *
 * @param <T> the type of elements emitted by this Observer
 * @see Timed
 */
public abstract class Observer<T> {

    private static final AtomicInteger WORKER_ID = new AtomicInteger();

    private static final Object NONE = ClassUtil.newNullSentinel();

    private static final Object COMPLETE_FLAG = ClassUtil.newNullSentinel();

    /**
     * Legacy interval multiplier retained for source and binary compatibility. Current timed
     * operators schedule against their exact configured interval and do not use this value.
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

    /**
     * Set by the shutdown hook installed by {@link #stopOnJvmExit(ExecutorService)} immediately before the pool
     * threads are interrupted, so that the interrupt is recognised as a library-initiated stop rather than a
     * failure of the source being observed.
     */
    private static volatile boolean jvmShuttingDown = false;

    static {
        if (IOUtil.IS_PLATFORM_ANDROID) {
            asyncExecutor = AndroidUtil.getThreadPoolExecutor();
        } else {
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
                    N.max(64, IOUtil.CPU_CORES * 8), // coreThreadPoolSize
                    N.max(128, IOUtil.CPU_CORES * 16), // // maxThreadPoolSize
                    180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), daemonThreadFactory("abacus-observer-async-"));

            asyncExecutor = threadPoolExecutor;

            stopOnJvmExit(threadPoolExecutor);
        }
    }

    /**
     * Scheduled executor used by intermediate operators (debounce, throttleFirst/throttleLast, buffer)
     * to schedule deferred or periodic tasks within the processing pipeline.
     */
    protected static final ScheduledThreadPoolExecutor schedulerForIntermediateOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : N.max(64, IOUtil.CPU_CORES * 8), daemonThreadFactory("abacus-observer-intermediate-"));

    /**
     * Scheduled executor used by terminal observe operations ({@link TimerObserver},
     * {@link IntervalObserver}) to emit items on a scheduled basis.
     */
    protected static final ScheduledThreadPoolExecutor schedulerForObserveOp = new ScheduledThreadPoolExecutor(
            IOUtil.IS_PLATFORM_ANDROID ? Math.max(8, IOUtil.CPU_CORES) : N.max(64, IOUtil.CPU_CORES * 8), daemonThreadFactory("abacus-observer-terminal-"));

    static {
        // Timed pipelines frequently cancel tasks with long delays. Remove them immediately so
        // cancelled futures do not retain their observer/dispatcher graphs until the delay expires.
        schedulerForIntermediateOp.setRemoveOnCancelPolicy(true);
        schedulerForObserveOp.setRemoveOnCancelPolicy(true);

        stopOnJvmExit(schedulerForIntermediateOp);
        stopOnJvmExit(schedulerForObserveOp);
    }

    /**
     * Registers a JVM shutdown hook that stops the specified observer pool without waiting for its tasks.
     *
     * @param pool the observer pool to stop when the JVM exits
     */
    private static void stopOnJvmExit(final ExecutorService pool) {
        // A delayed hook (shutdown() then awaitTermination) cannot end these tasks, so it burned its whole
        // timeout on every exit: shutdown() interrupts only IDLE workers, so an emission parked in
        // pollSource(BlockingQueue) keeps running, and it keeps pending one-shot tasks, so a timer() with a
        // long delay keeps its scheduler alive too. Every thread these pools create is a daemon, so no
        // graceful window is needed here: interrupt whatever is still running, wait for nothing, and let the
        // JVM exit at once.
        MoreExecutors.addShutdownHook(MoreExecutors.newThread("abacus-observer-shutdown-hook", () -> {
            jvmShuttingDown = true;

            pool.shutdownNow();
        }));
    }

    private static ThreadFactory daemonThreadFactory(final String namePrefix) {
        return task -> {
            final Thread thread = MoreExecutors.newThread(namePrefix + WORKER_ID.incrementAndGet(), task);
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Periodic buffer tasks tracked for cancellation when a subscription terminates. The mapped
     * delay is retained for binary/source compatibility with the existing internal representation.
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

    /** Guards the single-use subscription contract. Access is synchronized by {@link #beginSubscription}. */
    private boolean subscribed;

    // Always acquire this gate before an operator's local lock, including in scheduled callbacks.
    private final Object eventGate = new Object();
    private boolean lifecycleFinished;
    private boolean terminalDelivered;
    private Exception terminalFailure;
    private final List<Runnable> terminationActions = new ArrayList<>();
    private final Object waitGate = new Object();
    private Thread pollingThread;
    private boolean internalWakeup;
    private ScheduledFuture<?> sourceFuture;

    private void runEvent(final Runnable action) {
        synchronized (eventGate) {
            if (lifecycleFinished) {
                return;
            }
            try {
                action.run();
            } catch (final Error fatal) {
                abortSubscription(fatal);
                throw fatal;
            } catch (final Exception e) {
                if (terminalDelivered) {
                    throw e;
                }
                finishSubscription(e);
                return;
            } finally {
                if (!hasMore && !lifecycleFinished) {
                    finishSubscription(terminalFailure);
                }
            }
        }
    }

    private void emitSource(final Object value) {
        runEvent(() -> {
            if (hasMore) {
                dispatcher.onNext(value);
            }
        });
    }

    private void finishSubscription(final Exception error) {
        synchronized (eventGate) {
            if (lifecycleFinished) {
                return;
            }
            // Close the lifecycle before flushing: a downstream limit can reenter the terminal path.
            lifecycleFinished = true;
            hasMore = false;
            try {
                stopSource();
                if (!terminalDelivered) {
                    if (error == null) {
                        dispatcher.onComplete();
                    } else {
                        dispatcher.onError(error);
                    }
                }
            } catch (final RuntimeException | Error failure) {
                abortSubscription(failure);
                throw failure;
            } finally {
                try {
                    for (final Runnable cleanup : terminationActions) {
                        cleanup.run();
                    }
                    terminationActions.clear();
                    cancelScheduledFutures();
                } catch (final RuntimeException | Error failure) {
                    abortSubscription(failure);
                    throw failure;
                }
            }
        }
    }

    // Abort does not emit an Exception-valued terminal signal. Continue cleanup even if one
    // action fails, preserving the original failure rather than replacing it with cleanup errors.
    private void abortSubscription(final Throwable failure) {
        synchronized (eventGate) {
            lifecycleFinished = true;
            hasMore = false;
            try {
                stopSource();
            } catch (final Throwable cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            // A terminal callback may fail after lifecycleFinished was set, so do not return early.
            for (final Runnable cleanup : terminationActions) {
                try {
                    cleanup.run();
                } catch (final Throwable cleanupFailure) {
                    if (cleanupFailure != failure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
            }
            terminationActions.clear();
            try {
                cancelScheduledFutures();
            } catch (final Throwable cleanupFailure) {
                if (cleanupFailure != failure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    private void startSource(final Runnable submission) {
        try {
            submission.run();
        } catch (final RuntimeException | Error failure) {
            abortSubscription(failure);
            throw failure;
        }
    }

    private void stopSource() {
        if (sourceFuture != null) {
            sourceFuture.cancel(false);
        }
        // Register and interrupt only the queue-polling region, never the user's onNext callback.
        synchronized (waitGate) {
            if (pollingThread != null && pollingThread != Thread.currentThread()) {
                internalWakeup = true;
                pollingThread.interrupt();
            }
        }
    }

    private void reportSourceFailure(final Exception error) {
        // The interrupt sent by the shutdown hook is a library-initiated stop, not a failure of the source:
        // delivering it would run a terminal callback while the JVM is exiting and, with no onError supplied,
        // ON_ERROR_MISSING would rethrow it out of a pool thread and print an uncaught stack trace at exit.
        if (jvmShuttingDown && error instanceof InterruptedException) {
            return;
        }

        synchronized (eventGate) {
            if (terminalDelivered) {
                throw ExceptionUtil.toRuntimeException(error, true);
            }
            finishSubscription(error);
        }
    }

    private void publishSourceFuture(final ScheduledFuture<?> future) {
        synchronized (eventGate) {
            sourceFuture = future;
            // A zero-delay invocation can finish before schedule() returns its future.
            if (lifecycleFinished) {
                future.cancel(false);
            }
        }
    }

    private <E> E pollSource(final BlockingQueue<E> queue) throws InterruptedException {
        synchronized (waitGate) {
            if (!hasMore) {
                return null;
            }
            pollingThread = Thread.currentThread();
        }
        try {
            return queue.poll(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            synchronized (waitGate) {
                if (!internalWakeup) {
                    throw e;
                }
            }
            return null;
        } finally {
            synchronized (waitGate) {
                pollingThread = null;
                if (internalWakeup) {
                    Thread.interrupted();
                    internalWakeup = false;
                }
            }
        }
    }

    private void deliverError(final Consumer<? super Exception> callback, final Exception error) {
        if (terminalDelivered) {
            return;
        }
        terminalDelivered = true;
        terminalFailure = error;
        hasMore = false;
        stopSource();
        callback.accept(error);
    }

    private void deliverComplete(final Runnable callback) {
        if (terminalDelivered) {
            return;
        }
        terminalDelivered = true;
        callback.run();
    }

    /** Deferred initializers for timed intermediate operators. They run only after a terminal dispatcher is installed. */
    private final List<Runnable> subscriptionActions = new ArrayList<>();

    // One owner holds all source/lifecycle state. Facades change only the element type advertised to
    // callers; the current-stage guard prevents a retained earlier type from reaching the new tail.
    private final Observer<?> pipelineOwner;
    private Observer<?> currentStage = this;
    private final ThreadLocal<Observer<?>> subscriptionDelegate = new ThreadLocal<>();

    private Observer(final Observer<?> owner) {
        dispatcher = owner.dispatcher;
        pipelineOwner = owner;
    }

    @SuppressWarnings("unchecked")
    private Observer<T> ownerForStage() {
        return (Observer<T>) pipelineOwner;
    }

    /**
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     */
    private void checkCurrentStage() throws IllegalStateException {
        N.checkState(pipelineOwner.currentStage == this, "This Observer stage was replaced by a type-changing operator");
        N.checkState(!pipelineOwner.subscribed, "This Observer has already been subscribed");
    }

    private <R> Observer<R> nextStage(final Runnable mutation) {
        final Observer<R> next = new TypedStage<>(pipelineOwner);
        // Allocate first, then seal during mutation. A partial append must never resurrect the old type.
        pipelineOwner.currentStage = null;
        mutation.run();
        pipelineOwner.currentStage = next;
        return next;
    }

    private static final class TypedStage<R> extends Observer<R> {
        private TypedStage(final Observer<?> owner) {
            super(owner);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void observe(final Consumer<? super R> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalStateException, IllegalArgumentException {
            final Observer<?> self = this;
            self.checkCurrentStage();
            N.checkArgNotNull(action, cs.action);
            N.checkArgNotNull(onError, cs.onError);
            N.checkArgNotNull(onComplete, cs.onComplete);
            final Observer<?> owner = self.pipelineOwner;
            owner.subscriptionDelegate.set(this);
            // Do not hold the owner monitor across user source startup or callbacks.
            try {
                ((Observer) owner).observe(action, onError, onComplete);
            } finally {
                owner.subscriptionDelegate.remove();
            }
        }
    }

    /** Creates a new Observer with a fresh, empty {@link Dispatcher} chain. */
    protected Observer() {
        this(new Dispatcher<>());
    }

    /**
     * Creates a new Observer that uses the given dispatcher as the head of its chain.
     *
     * @param dispatcher the head dispatcher for this Observer's pipeline; must not be {@code null}
     * @throws IllegalArgumentException if {@code dispatcher} is {@code null}.
     */
    protected Observer(final Dispatcher<Object> dispatcher) throws IllegalArgumentException {
        this.dispatcher = N.checkArgNotNull(dispatcher, cs.dispatcher);
        pipelineOwner = this;
    }

    /**
     * Validates and records the single subscription permitted for this observer.
     *
     * @param action the action to invoke for each emitted item
     * @param onError the action to invoke when observation fails
     * @param onComplete the action to invoke when observation completes
     * @throws IllegalStateException if this observer has already been subscribed or this source is
     *         a stale stage being subscribed without current-stage forwarding authorization
     * @throws IllegalArgumentException if {@code action}, {@code onError}, or {@code onComplete} is {@code null}
     */
    @SuppressWarnings("unused")
    protected final synchronized void beginSubscription(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
            throws IllegalStateException, IllegalArgumentException {
        N.checkState(currentStage == this || (currentStage != null && subscriptionDelegate.get() == currentStage), "This Observer stage was replaced");
        N.checkState(!subscribed, "This Observer has already been subscribed");
        N.checkArgNotNull(action, cs.action);
        N.checkArgNotNull(onError, cs.onError);
        N.checkArgNotNull(onComplete, cs.onComplete);
        subscribed = true;
    }

    /**
     * Registers work that must not start until this pipeline is subscribed. Custom sources may call
     * this during authorized forwarded startup, before {@link #beginSubscription}; stale direct calls
     * and calls after subscription are rejected.
     *
     * @param action work to run when the subscription starts; must not be {@code null}
     * @throws IllegalStateException if this stage is stale or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    protected final synchronized void addSubscriptionAction(final Runnable action) throws IllegalStateException, IllegalArgumentException {
        N.checkState(
                pipelineOwner.currentStage == this
                        || (pipelineOwner.currentStage != null && pipelineOwner.subscriptionDelegate.get() == pipelineOwner.currentStage),
                "This Observer stage was replaced");
        addSubscriptionActionInternal(action);
    }

    /**
     * @throws IllegalStateException if the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code action} is {@code null}
     */
    private synchronized void addSubscriptionActionInternal(final Runnable action) throws IllegalStateException, IllegalArgumentException {
        N.checkState(!subscribed, "This Observer has already been subscribed");
        N.checkArgNotNull(action, cs.action);

        subscriptionActions.add(action);
    }

    /** Starts deferred intermediate work after the terminal dispatcher has been appended. */
    protected final void startSubscriptionActions() {
        final List<Runnable> actions;

        synchronized (this) {
            actions = new ArrayList<>(subscriptionActions);
            subscriptionActions.clear();
        }

        try {
            for (final Runnable action : actions) {
                synchronized (eventGate) {
                    if (!lifecycleFinished && hasMore) {
                        action.run();
                    }
                }
            }
        } catch (final RuntimeException | Error e) {
            abortSubscription(e);
            throw e;
        }
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
     * queue.add("alpha");
     * queue.add("beta");
     * Observer.complete(queue);   // signals completion
     * }</pre>
     *
     * @param queue the {@code BlockingQueue} to complete
     * @throws IllegalArgumentException if {@code queue} is {@code null}.
     * @throws ClassCastException if the queue orders or otherwise restricts its elements and
     *         cannot accept the private completion marker
     * @throws RuntimeException if the current thread is interrupted while waiting to insert
     *         the completion flag into a full queue
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void complete(final BlockingQueue<?> queue) throws IllegalArgumentException, ClassCastException, RuntimeException {
        N.checkArgNotNull(queue, cs.queue);

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
     * @throws IllegalArgumentException if {@code queue} is {@code null}.
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
     * @throws IllegalArgumentException if {@code iter} is {@code null}.
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
     * Observer.timer(1000)
     *     .observe(value -> System.out.println("Timer fired!"));
     * }</pre>
     *
     * @param delayInMillis the delay in milliseconds before emitting
     * @return a new Observer that emits a single {@code 0L} after the delay and then completes
     * @throws IllegalArgumentException if {@code delayInMillis} is negative.
     * @see #timer(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#timer(long,%20java.util.concurrent.TimeUnit)">RxJava#timer</a>
     */
    public static Observer<Long> timer(final long delayInMillis) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if {@code delay} is negative or {@code unit} is {@code null}.
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
     * Observer.interval(1000)
     *     .limit(5)
     *     .observe(System.out::println);   // Prints 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param periodInMillis the period between emissions in milliseconds; the first value
     *        ({@code 0L}) is emitted immediately with no initial delay
     * @return a new Observer that emits sequential {@code Long} values periodically
     * @throws IllegalArgumentException if {@code periodInMillis} is zero or negative.
     * @see #interval(long, TimeUnit)
     * @see #interval(long, long)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long periodInMillis) throws IllegalArgumentException {
        return interval(0, periodInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates an Observer that emits sequential numbers periodically with an initial delay
     * and specified interval in milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(2000, 1000) // wait 2s, then emit every 1s
     *     .limit(3)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param initialDelayInMillis the initial delay before the first emission in milliseconds
     * @param periodInMillis the period between subsequent emissions in milliseconds
     * @return a new Observer that emits sequential {@code Long} values periodically
     * @throws IllegalArgumentException if {@code initialDelayInMillis} is negative or {@code periodInMillis} is zero
     *         or negative.
     * @see #interval(long, long, TimeUnit)
     * @see #interval(long)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long initialDelayInMillis, final long periodInMillis) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if {@code period} is zero or negative or {@code unit} is {@code null}.
     * @see #interval(long, long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#interval(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#interval</a>
     */
    public static Observer<Long> interval(final long period, final TimeUnit unit) throws IllegalArgumentException {
        return interval(0, period, unit);
    }

    /**
     * Creates an Observer that emits sequential numbers periodically with an initial delay,
     * period, and time unit.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(5, 2, TimeUnit.SECONDS) // wait 5s, then emit every 2s
     *     .map(n -> "Event " + n)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param initialDelay the initial delay before the first emission
     * @param period the period between subsequent emissions
     * @param unit the time unit for both delay and period
     * @return a new Observer that emits periodically
     * @throws IllegalArgumentException if initialDelay is negative, period is non-positive, or unit is null.
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
     * without emitting another item. On normal completion, the most recent pending item is
     * emitted immediately; an error discards any pending item. This is useful for handling
     * rapid events like user input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * userInputObserver
     *     .debounce(300)
     *     .observe(text -> performSearch(text));
     * }</pre>
     *
     * @param intervalDurationInMillis the debounce interval in milliseconds; if zero, this
     *        Observer is returned unchanged with no debounce applied
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative.
     * @see #debounce(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDurationInMillis) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().debounceInternal(intervalDurationInMillis);
        return this;
    }

    private Observer<T> debounceInternal(final long intervalDurationInMillis) {
        return debounceInternal(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies debounce operator that only emits an item if a particular timespan has passed
     * without emitting another item. On normal completion, the most recent pending item is
     * emitted immediately; an error discards any pending item. The time interval can be
     * specified with custom units.
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
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit} is {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#debounce(long,%20java.util.concurrent.TimeUnit,%20io.reactivex.Scheduler)">RxJava#debounce</a>
     */
    public Observer<T> debounce(final long intervalDuration, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().debounceInternal(intervalDuration, unit);
        return this;
    }

    private Observer<T> debounceInternal(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Interval cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInNanos = unit.toNanos(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            private final Object terminalSignal = new Object();
            { //NOSONAR
                terminationActions.add(() -> terminate(false));
            }
            private ScheduledFuture<?> future;
            private long generation = 0;
            private boolean terminated = false;

            @Override
            public void onNext(final Object param) {
                Exception schedulingFailure = null;

                synchronized (holder) {
                    if (terminated) {
                        return;
                    }

                    holder.setValue(param);

                    if (future != null) {
                        future.cancel(false);
                    }

                    final long scheduledGeneration = ++generation;

                    try {
                        future = schedulerForIntermediateOp.schedule(() -> runEvent(() -> emitPending(scheduledGeneration)), intervalDurationInNanos,
                                TimeUnit.NANOSECONDS);
                    } catch (final Exception e) {
                        terminated = true;
                        holder.setValue(NONE);
                        hasMore = false;
                        schedulingFailure = e;
                    }
                }

                if (schedulingFailure != null) {
                    super.onError(schedulingFailure);
                }
            }

            @Override
            public void onError(final Exception error) {
                if (terminate(false)) {
                    super.onError(error);
                }
            }

            @Override
            public void onComplete() {
                final Object pending = takePendingOnTermination(true);

                if (pending == terminalSignal) {
                    return;
                }

                if (pending != NONE && downDispatcher != null && !isLimitReached()) {
                    try {
                        downDispatcher.onNext(pending);
                    } catch (final Exception e) {
                        super.onError(e);
                        return;
                    }
                }

                super.onComplete();
            }

            private void emitPending(final long scheduledGeneration) {
                Exception failure = null;

                synchronized (holder) {
                    if (terminated || scheduledGeneration != generation) {
                        return;
                    }

                    final Object pending = holder.value();
                    holder.setValue(NONE);
                    future = null;

                    if (pending != NONE && downDispatcher != null && !isLimitReached()) {
                        try {
                            // Keep the state lock while delivering so a terminal signal cannot
                            // overtake this scheduled onNext.
                            downDispatcher.onNext(pending);
                        } catch (final Exception e) {
                            terminated = true;
                            generation++;
                            hasMore = false;
                            failure = e;
                        }
                    }
                }

                if (failure != null) {
                    super.onError(failure);
                }
            }

            private boolean terminate(final boolean keepPending) {
                synchronized (holder) {
                    if (terminated) {
                        return false;
                    }

                    terminated = true;
                    generation++;

                    if (future != null) {
                        future.cancel(false);
                        future = null;
                    }

                    if (!keepPending) {
                        holder.setValue(NONE);
                    }

                    return true;
                }
            }

            private Object takePendingOnTermination(final boolean keepPending) {
                synchronized (holder) {
                    if (!terminate(keepPending)) {
                        return terminalSignal;
                    }

                    final Object pending = holder.value();
                    holder.setValue(NONE);
                    return pending;
                }
            }
        });

        return this;
    }

    /**
     * Applies throttleFirst operator that immediately emits the first item received, then ignores
     * subsequent items until the specified duration has elapsed. Each accepted item starts the
     * next suppression window.
     *
     * <p>Accepted items and terminal signals are delivered in reservation order by one drain, outside the
     * throttle state monitor. Reentrant or concurrent signals are queued behind the current callback; under
     * contention, a queued item runs on the active drain's thread rather than necessarily its producer's
     * thread, and its producer may return before delivery. A {@code RuntimeException} from a nonterminal
     * downstream callback discards queued signals and is sent to {@code onError}. An {@code Error}, or an
     * exception from a terminal callback, propagates on the drain thread. These rules apply when throttling
     * is enabled; a zero interval leaves the pipeline unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * buttonClickObserver
     *     .throttleFirst(1000)
     *     .observe(click -> handleButtonClick());
     * }</pre>
     *
     * @param intervalDurationInMillis the throttle window duration in milliseconds; if zero,
     *        this Observer is returned unchanged with no throttling applied
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative.
     * @see #throttleFirst(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDurationInMillis) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().throttleFirstInternal(intervalDurationInMillis);
        return this;
    }

    private Observer<T> throttleFirstInternal(final long intervalDurationInMillis) {
        return throttleFirstInternal(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies throttleFirst operator that immediately emits the first item received, then ignores
     * subsequent items until the specified duration has elapsed. Each accepted item starts the
     * next suppression window. The duration can be specified with custom time units.
     *
     * <p>Delivery follows the same ordered-drain, callback-thread and failure rules as
     * {@link #throttleFirst(long)}: callbacks run outside the throttle state monitor, queued signals may
     * run on another producer's active drain thread, and nonterminal callback {@code RuntimeException}s
     * are routed to {@code onError}. Terminal callback failures and {@code Error}s propagate on that thread.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * apiCallObserver
     *     .throttleFirst(5, TimeUnit.SECONDS) // rate limit API calls
     *     .observe(request -> makeApiCall(request));
     * }</pre>
     *
     * @param intervalDuration the throttle window duration; if zero, this Observer is returned
     *        unchanged with no throttling applied
     * @param unit the time unit of the interval
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit} is {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleFirst(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleFirst</a>
     */
    public Observer<T> throttleFirst(final long intervalDuration, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().throttleFirstInternal(intervalDuration, unit);
        return this;
    }

    private Observer<T> throttleFirstInternal(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Interval cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInNanos = unit.toNanos(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            private long lastEmissionTime;
            private boolean hasEmitted;
            private boolean terminated;
            private boolean draining;
            private final ArrayDeque<Runnable> signals = new ArrayDeque<>();
            private Runnable terminalSignal;

            @Override
            public void onNext(final Object param) {
                synchronized (holder) {
                    if (terminated) {
                        return;
                    }
                    final long now = System.nanoTime();
                    if (hasEmitted && now - lastEmissionTime < intervalDurationInNanos) {
                        return;
                    }
                    hasEmitted = true;
                    lastEmissionTime = now;
                    signals.add(() -> super.onNext(param));
                    if (draining) {
                        return;
                    }
                    draining = true;
                }
                drain();
            }

            @Override
            public void onError(final Exception error) {
                terminate(() -> super.onError(error));
            }

            @Override
            public void onComplete() {
                terminate(() -> super.onComplete());
            }

            private void terminate(final Runnable terminal) {
                synchronized (holder) {
                    if (terminated) {
                        return;
                    }
                    terminated = true;
                    terminalSignal = terminal;
                    signals.add(terminal);
                    if (draining) {
                        return;
                    }
                    draining = true;
                }
                drain();
            }

            // A single drain preserves reservation order, including reentrant and concurrent terminal signals.
            // No downstream callback holds the state monitor, and a producer never waits for a running callback.
            private void drain() {
                while (true) {
                    final Runnable signal;
                    synchronized (holder) {
                        signal = signals.poll();
                        if (signal == null) {
                            draining = false;
                            return;
                        }
                    }
                    try {
                        signal.run();
                    } catch (final RuntimeException | Error failure) {
                        synchronized (holder) {
                            // Fail the current drain permanently before notifying downstream. Accepted items
                            // and a queued terminal signal must not follow a failed onNext callback.
                            signals.clear();
                            draining = false;
                            terminated = true;
                        }
                        // A terminal callback's own failure must propagate, never trigger a second onError.
                        if (failure instanceof RuntimeException exception && signal != terminalSignal) {
                            super.onError(exception);
                            return;
                        }
                        throw failure;
                    }
                }
            }
        });

        return this;
    }

    /**
     * Applies throttleLast operator that emits the most recent item after each full sampling
     * window. A pending item from an incomplete window is discarded on completion or error.
     * Also known as "sample" in some reactive libraries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * mousePositionObserver
     *     .throttleLast(100)
     *     .observe(pos -> updateCursorPosition(pos));
     * }</pre>
     *
     * @param intervalDurationInMillis the sampling window duration in milliseconds; if zero,
     *        this Observer is returned unchanged with no throttling applied
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDurationInMillis} is negative.
     * @see #throttleLast(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDurationInMillis) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().throttleLastInternal(intervalDurationInMillis);
        return this;
    }

    private Observer<T> throttleLastInternal(final long intervalDurationInMillis) {
        return throttleLastInternal(intervalDurationInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Applies throttleLast operator that emits the most recent item after each full sampling
     * window. A pending item from an incomplete window is discarded on completion or error.
     * The duration can be specified with custom time units.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * temperatureSensor
     *     .throttleLast(1, TimeUnit.MINUTES) // sample every minute
     *     .observe(temp -> logTemperature(temp));
     * }</pre>
     *
     * @param intervalDuration the sampling window duration; if zero, this Observer is returned
     *        unchanged with no throttling applied
     * @param unit the time unit of the interval
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code intervalDuration} is negative or {@code unit} is {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#throttleLast(long,%20java.util.concurrent.TimeUnit)">RxJava#throttleLast</a>
     */
    public Observer<T> throttleLast(final long intervalDuration, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().throttleLastInternal(intervalDuration, unit);
        return this;
    }

    private Observer<T> throttleLastInternal(final long intervalDuration, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(intervalDuration >= 0, "Interval cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (intervalDuration == 0) {
            return this;
        }

        final long intervalDurationInNanos = unit.toNanos(intervalDuration);

        dispatcher.append(new Dispatcher<>() {
            { //NOSONAR
                terminationActions.add(this::terminate);
            }
            private ScheduledFuture<?> future;
            private boolean terminated = false;

            @Override
            public void onNext(final Object param) {
                Exception schedulingFailure = null;

                synchronized (holder) {
                    if (terminated) {
                        return;
                    }

                    if (holder.value() == NONE) {
                        try {
                            future = schedulerForIntermediateOp.schedule(() -> runEvent(this::emitPending), intervalDurationInNanos, TimeUnit.NANOSECONDS);
                        } catch (final Exception e) {
                            terminated = true;
                            hasMore = false;
                            schedulingFailure = e;
                        }
                    }

                    if (!terminated) {
                        holder.setValue(param);
                    }
                }

                if (schedulingFailure != null) {
                    super.onError(schedulingFailure);
                }
            }

            @Override
            public void onError(final Exception error) {
                if (terminate()) {
                    super.onError(error);
                }
            }

            @Override
            public void onComplete() {
                if (terminate()) {
                    super.onComplete();
                }
            }

            private void emitPending() {
                Exception failure = null;

                synchronized (holder) {
                    if (terminated) {
                        return;
                    }

                    final Object pending = holder.value();
                    holder.setValue(NONE);
                    future = null;

                    if (pending != NONE && downDispatcher != null && !isLimitReached()) {
                        try {
                            // Serialize delivery with terminal signals.
                            downDispatcher.onNext(pending);
                        } catch (final Exception e) {
                            terminated = true;
                            hasMore = false;
                            failure = e;
                        }
                    }
                }

                if (failure != null) {
                    super.onError(failure);
                }
            }

            private boolean terminate() {
                synchronized (holder) {
                    if (terminated) {
                        return false;
                    }

                    terminated = true;
                    holder.setValue(NONE);

                    if (future != null) {
                        future.cancel(false);
                        future = null;
                    }

                    return true;
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
     *     .delay(1000)
     *     .observe(System.out::println);
     * }</pre>
     *
     * @param delayInMillis the delay duration in milliseconds; if zero, this Observer is
     *        returned unchanged with no delay applied
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code delayInMillis} is negative.
     * @see #delay(long, TimeUnit)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delayInMillis) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().delayInternal(delayInMillis);
        return this;
    }

    private Observer<T> delayInternal(final long delayInMillis) {
        return delayInternal(delayInMillis, TimeUnit.MILLISECONDS);
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
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code delay} is negative or {@code unit} is {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#delay(long,%20java.util.concurrent.TimeUnit)">RxJava#delay</a>
     */
    public Observer<T> delay(final long delay, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().delayInternal(delay, unit);
        return this;
    }

    private Observer<T> delayInternal(final long delay, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(delay >= 0, "Delay cannot be negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");

        if (delay == 0) {
            return this;
        }

        final long delayInNanos = unit.toNanos(delay);

        dispatcher.append(new Dispatcher<>() {
            private final long startTimeInNanos = System.nanoTime();
            private volatile boolean isDelayed = false;

            @Override
            public void onNext(final Object param) {
                if (!isDelayed) {
                    final long elapsedTimeInNanos = System.nanoTime() - startTimeInNanos;
                    final long remainingDelayInNanos = delayInNanos - elapsedTimeInNanos;

                    if (remainingDelayInNanos > 0) {
                        N.sleepUninterruptibly(remainingDelayInNanos, TimeUnit.NANOSECONDS);
                    }

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
     * consecutive emissions in milliseconds. For the first item the interval is measured from
     * the moment this operator was applied, not from the start of the subscription.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.interval(1000)
     *     .timeInterval()
     *     .observe(timed -> System.out.println("Value: " + timed.value() +
     *                                          ", Interval: " + timed.timestamp() + "ms"));
     * }</pre>
     *
     * @return a new current typed stage, invalidating prior handles, emitting {@code Timed<T>} objects whose
     *         timestamp field holds the elapsed interval in milliseconds since the previous
     *         emission
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @see Timed
     * @see #timestamp()
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html#timeInterval()">RxJava#timeInterval</a>
     */
    public Observer<Timed<T>> timeInterval() throws IllegalStateException {
        checkCurrentStage();
        return nextStage(() -> ownerForStage().timeIntervalInternal());
    }

    private Observer<Timed<T>> timeIntervalInternal() {
        dispatcher.append(new Dispatcher<>() {
            private long startTimeInNanos = System.nanoTime();

            @Override
            public synchronized void onNext(final Object param) {
                if (downDispatcher != null) {
                    final long now = System.nanoTime();
                    final long intervalInMillis = TimeUnit.NANOSECONDS.toMillis(now - startTimeInNanos);
                    startTimeInNanos = now;

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
     * @return a new current typed stage, invalidating prior handles, emitting {@code Timed<T>} objects whose
     *         timestamp field holds the emission time in milliseconds since the epoch
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @see Timed
     * @see #timeInterval()
     * @see <a href="http://reactivex.io/RxJava/javadoc/io/reactivex/Observable.html#timestamp()">RxJava#timestamp</a>
     */
    public Observer<Timed<T>> timestamp() throws IllegalStateException {
        checkCurrentStage();
        return nextStage(() -> ownerForStage().timestampInternal());
    }

    private Observer<Timed<T>> timestampInternal() {
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
     *     .observe(System.out::println);   // prints 3, 4, 5
     * }</pre>
     *
     * <p>After the requested number of items has been skipped, all subsequent items
     * are forwarded, including when {@code n} is {@link Long#MAX_VALUE}.</p>
     *
     * @param n the number of items to skip; if zero, no items are skipped (a negative
     *        value is rejected as described below)
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code n} is negative.
     */
    public Observer<T> skip(final long n) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().skipInternal(n);
        return this;
    }

    /**
     * @throws IllegalArgumentException if {@code n} is negative
     */
    private Observer<T> skipInternal(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n > 0) {
            dispatcher.append(new Dispatcher<>() {
                private final AtomicLong counter = new AtomicLong();

                @Override
                public void onNext(final Object param) {
                    if (downDispatcher != null && counter.getAndUpdate(count -> count < n ? count + 1 : count) >= n) {
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
     *     .observe(System.out::println);   // prints 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param maxSize the maximum number of items to emit; once this count is reached the
     *        upstream source is signaled to stop producing further items
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code maxSize} is negative.
     */
    public Observer<T> limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().limitInternal(maxSize);
        return this;
    }

    /**
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    private Observer<T> limitInternal(final long maxSize) throws IllegalArgumentException {
        N.checkArgNotNegative(maxSize, cs.maxSize);

        if (maxSize == 0) {
            hasMore = false;
            return this;
        }

        dispatcher.append(new Dispatcher<>() {
            private final AtomicLong counter = new AtomicLong();

            @Override
            boolean isLimitReached() {
                return counter.get() >= maxSize || super.isLimitReached();
            }

            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null) {
                    final long current = counter.incrementAndGet();

                    if (current <= maxSize) {
                        downDispatcher.onNext(param);

                        if (current == maxSize) {
                            hasMore = false;
                        }
                    } else {
                        hasMore = false;
                    }
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
     *     .observe(System.out::println);   // prints 1, 2, 3
     * }</pre>
     *
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @see #distinctBy(Function)
     */
    public Observer<T> distinct() throws IllegalStateException {
        checkCurrentStage();
        ownerForStage().distinctInternal();
        return this;
    }

    private Observer<T> distinctInternal() {
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
     *     .distinctBy(s -> s.charAt(0))  // distinct by first letter
     *     .observe(System.out::println); // prints apple, banana
     * }</pre>
     *
     * @param keyExtractor function to extract the key used to determine uniqueness; key
     *        equality is based on {@code hashCode()} and {@code equals()}
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}.
     * @see #distinct()
     */
    public Observer<T> distinctBy(final Function<? super T, ?> keyExtractor) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().distinctByInternal(keyExtractor);
        return this;
    }

    /**
     * @throws IllegalArgumentException if {@code keyExtractor} is {@code null}
     */
    private Observer<T> distinctByInternal(final Function<? super T, ?> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);

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
     *     .observe(System.out::println);   // prints 2, 4
     * }</pre>
     *
     * @param filter the predicate used to test each item; items for which the predicate returns
     *               {@code true} are forwarded downstream
     * @return this Observer instance for method chaining
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     */
    public Observer<T> filter(final Predicate<? super T> filter) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        ownerForStage().filterInternal(filter);
        return this;
    }

    /**
     * @throws IllegalArgumentException if {@code filter} is {@code null}
     */
    private Observer<T> filterInternal(final Predicate<? super T> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

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
     *     .observe(System.out::println);   // prints 1, 4, 9
     * }</pre>
     *
     * @param <R> the type of items emitted after transformation
     * @param mapper the function to transform each item
     * @return a new current {@code Observer<R>} stage, invalidating prior handles, emitting the
     *         transformed items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     * @see #flatMap(Function)
     */
    public <R> Observer<R> map(final Function<? super T, R> mapper) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgNotNull(mapper, cs.mapper);
        return nextStage(() -> ownerForStage().mapInternal(mapper));
    }

    /**
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     */
    private <R> Observer<R> mapInternal(final Function<? super T, R> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

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
     * Traversal of a mapped collection stops when a downstream {@link #limit(long)} is reached,
     * and subsequent inputs are not passed to the mapper.
     * An upstream limit still allows all mapped items from each accepted input to be emitted,
     * including inputs emitted by a buffer during normal completion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Observer.of(Arrays.asList("Hello World", "Foo Bar"))
     *     .flatMap(s -> Arrays.asList(s.split(" ")))
     *     .observe(System.out::println);   // prints Hello, World, Foo, Bar
     * }</pre>
     *
     * @param <R> the type of items in the flattened sequence
     * @param mapper function that transforms each item into a collection; if it returns
     *        {@code null} or an empty collection, no items are emitted for that input
     * @return a new current {@code Observer<R>} stage, invalidating prior handles, emitting the
     *         flattened items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     * @see #map(Function)
     */
    public <R> Observer<R> flatMap(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgNotNull(mapper, cs.mapper);
        return nextStage(() -> ownerForStage().flatMapInternal(mapper));
    }

    /**
     * @throws IllegalArgumentException if {@code mapper} is {@code null}
     */
    private <R> Observer<R> flatMapInternal(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper, cs.mapper);

        dispatcher.append(new Dispatcher<>() {
            @Override
            public void onNext(final Object param) {
                if (downDispatcher != null && !downDispatcher.isLimitReached()) {
                    final Collection<? extends R> c = mapper.apply((T) param);

                    if (N.notEmpty(c)) {
                        final Iterator<? extends R> iter = c.iterator();

                        while (!downDispatcher.isLimitReached() && iter.hasNext()) {
                            downDispatcher.onNext(iter.next());
                        }
                    }
                }
            }
        });

        return (Observer<R>) this;
    }

    /**
     * Buffers items into lists based on a time window. Emits a list of items
     * every timespan period. Normal completion emits a final non-empty partial
     * buffer; an error discards it.
     *
     * <p>A window that expires with no items buffered still emits an empty list; the periodic emission is
     * never suppressed. Only the final partial buffer delivered on normal completion is suppressed when it
     * is empty.</p>
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
     * @return a new current typed stage, invalidating prior handles, emitting {@code List<T>} buffers of items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code timespan} is zero or negative or {@code unit} is {@code null}.
     * @see #buffer(long, TimeUnit, int)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        return nextStage(() -> ownerForStage().bufferInternal(timespan, unit));
    }

    private Observer<List<T>> bufferInternal(final long timespan, final TimeUnit unit) {
        return bufferInternal(timespan, unit, Integer.MAX_VALUE);
    }

    /**
     * Buffers items into lists based on time windows or item count, whichever occurs first.
     * Emits when either the time window expires or the buffer reaches the specified count.
     * Normal completion emits a final non-empty partial buffer; an error discards it.
     *
     * <p>A window that expires with no items buffered still emits an empty list; the periodic emission is
     * never suppressed. Only the final partial buffer delivered on normal completion is suppressed when it
     * is empty.</p>
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
     * @return a new current typed stage, invalidating prior handles, emitting {@code List<T>} buffers of items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code timespan} or {@code count} is zero or negative, or {@code unit} is
     *         {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20java.util.concurrent.TimeUnit,%20int)">RxJava#buffer(long, java.util.concurrent.TimeUnit, int)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final TimeUnit unit, final int count) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");
        return nextStage(() -> ownerForStage().bufferInternal(timespan, unit, count));
    }

    private Observer<List<T>> bufferInternal(final long timespan, final TimeUnit unit, final int count) throws IllegalArgumentException {
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final List<T> queue = new ArrayList<>();
            private volatile ScheduledFuture<?> scheduledFuture;
            private boolean terminated = false;

            { //NOSONAR
                addSubscriptionActionInternal(this::startScheduling);
                terminationActions.add(() -> terminate(false));
            }

            private void startScheduling() {
                final ScheduledFuture<?> future = schedulerForIntermediateOp.scheduleAtFixedRate(() -> runEvent(this::emitPeriodically), timespan, timespan,
                        unit);
                scheduledFuture = future;
                scheduledFutures.put(future, Math.max(1L, unit.toMillis(timespan)));
            }

            @Override
            public void onNext(final Object param) {
                synchronized (queue) {
                    if (terminated) {
                        return;
                    }

                    queue.add((T) param);

                    if (queue.size() == count) {
                        final List<T> list = new ArrayList<>(queue);
                        queue.clear();

                        if (downDispatcher != null) {
                            // Serialize count-triggered delivery with terminal signals.
                            downDispatcher.onNext(list);
                        }
                    }
                }
            }

            @Override
            public void onError(final Exception error) {
                if (terminate(false) != TERMINATED_BUFFER) {
                    super.onError(error);
                }
            }

            @Override
            public void onComplete() {
                final List<T> pending = terminate(true);

                if (pending == TERMINATED_BUFFER) {
                    return;
                }

                if (N.notEmpty(pending) && downDispatcher != null && !isLimitReached()) {
                    try {
                        downDispatcher.onNext(pending);
                    } catch (final Exception e) {
                        super.onError(e);
                        return;
                    }
                }

                super.onComplete();
            }

            private final List<T> TERMINATED_BUFFER = new ArrayList<>(0);

            private void emitPeriodically() {
                Exception failure = null;

                synchronized (queue) {
                    if (terminated) {
                        return;
                    }

                    final List<T> list = new ArrayList<>(queue);
                    queue.clear();

                    if (downDispatcher != null) {
                        try {
                            // Serialize scheduled delivery with terminal signals.
                            downDispatcher.onNext(list);
                        } catch (final Exception e) {
                            terminated = true;
                            hasMore = false;
                            queue.clear();
                            failure = e;
                        }
                    }
                }

                if (failure != null) {
                    cancelScheduledFuture();
                    super.onError(failure);
                }
            }

            private List<T> terminate(final boolean takePending) {
                cancelScheduledFuture();

                synchronized (queue) {
                    if (terminated) {
                        return TERMINATED_BUFFER;
                    }

                    terminated = true;
                    final List<T> pending = takePending && N.notEmpty(queue) ? new ArrayList<>(queue) : null;
                    queue.clear();
                    return pending;
                }
            }

            private void cancelScheduledFuture() {
                final ScheduledFuture<?> future = scheduledFuture;

                if (future != null) {
                    future.cancel(false);
                }
            }
        });

        return (Observer<List<T>>) this;
    }

    /**
     * Buffers items into lists based on sliding time windows. Creates overlapping or
     * gapped windows based on the timespan and timeskip parameters. Normal completion
     * emits each active non-empty partial window; an error discards active windows.
     *
     * <p>A window that expires with no items buffered still emits an empty list; the periodic emission is
     * never suppressed. Only the active partial windows delivered on normal completion are suppressed when
     * they are empty.</p>
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
     * @return a new current typed stage, invalidating prior handles, emitting {@code List<T>} buffers of items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code timespan} or {@code timeskip} is zero or negative, or {@code unit}
     *         is {@code null}.
     * @see #buffer(long, long, TimeUnit, int)
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgument(timeskip > 0, "timeskip cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        return nextStage(() -> ownerForStage().bufferInternal(timespan, timeskip, unit));
    }

    private Observer<List<T>> bufferInternal(final long timespan, final long timeskip, final TimeUnit unit) {
        return bufferInternal(timespan, timeskip, unit, Integer.MAX_VALUE);
    }

    /**
     * Buffers items into lists based on sliding time windows with a maximum count per buffer.
     * Creates overlapping or gapped windows that emit when either the window duration
     * expires or the count is reached. Normal completion emits each active non-empty partial
     * window; an error discards active windows.
     *
     * <p>A window that expires with no items buffered still emits an empty list; the periodic emission is
     * never suppressed. Only the active partial windows delivered on normal completion are suppressed when
     * they are empty.</p>
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
     * @return a new current typed stage, invalidating prior handles, emitting {@code List<T>} buffers of items
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code timespan}, {@code timeskip}, or {@code count} is zero or negative,
     *         or {@code unit} is {@code null}.
     * @see <a href="http://reactivex.io/RxJava/2.x/javadoc/io/reactivex/Observable.html#buffer(long,%20long,%20java.util.concurrent.TimeUnit)">RxJava#buffer(long, long, java.util.concurrent.TimeUnit)</a>
     */
    public Observer<List<T>> buffer(final long timespan, final long timeskip, final TimeUnit unit, final int count)
            throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgument(timeskip > 0, "timeskip cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");
        return nextStage(() -> ownerForStage().bufferInternal(timespan, timeskip, unit, count));
    }

    private Observer<List<T>> bufferInternal(final long timespan, final long timeskip, final TimeUnit unit, final int count) throws IllegalArgumentException {
        N.checkArgument(timespan > 0, "timespan cannot be 0 or negative");
        N.checkArgument(timeskip > 0, "timeskip cannot be 0 or negative");
        N.checkArgNotNull(unit, "Time unit cannot be null");
        N.checkArgument(count > 0, "count cannot be 0 or negative");

        dispatcher.append(new Dispatcher<>() {
            private final List<List<T>> TERMINATED_WINDOWS = new ArrayList<>(0);
            private final List<List<T>> windows = new ArrayList<>();
            private final Map<List<T>, ScheduledFuture<?>> windowFutures = new IdentityHashMap<>();
            private volatile ScheduledFuture<?> windowStarter;
            private boolean terminated = false;

            { //NOSONAR
                addSubscriptionActionInternal(this::startScheduling);
                terminationActions.add(() -> terminate(false));
            }

            private void startScheduling() {
                synchronized (windows) {
                    final List<T> initialWindow = new ArrayList<>();
                    windows.add(initialWindow);

                    try {
                        windowFutures.put(initialWindow, schedulerForIntermediateOp.schedule(() -> runEvent(() -> emitWindow(initialWindow)), timespan, unit));

                        final ScheduledFuture<?> future = schedulerForIntermediateOp.scheduleAtFixedRate(() -> runEvent(this::startWindow), timeskip, timeskip,
                                unit);
                        windowStarter = future;
                        scheduledFutures.put(future, Math.max(1L, unit.toMillis(timeskip)));
                    } catch (final RuntimeException | Error e) {
                        terminateLocked();
                        throw e;
                    }
                }
            }

            @Override
            public void onNext(final Object param) {
                synchronized (windows) {
                    if (terminated) {
                        return;
                    }

                    List<List<T>> fullWindows = null;

                    for (final Iterator<List<T>> iter = windows.iterator(); iter.hasNext();) {
                        final List<T> window = iter.next();
                        window.add((T) param);

                        if (window.size() == count) {
                            iter.remove();

                            final ScheduledFuture<?> future = windowFutures.remove(window);

                            if (future != null) {
                                future.cancel(false);
                            }

                            if (fullWindows == null) {
                                fullWindows = new ArrayList<>();
                            }

                            fullWindows.add(new ArrayList<>(window));
                        }
                    }

                    if (fullWindows != null && downDispatcher != null) {
                        // Serialize count-triggered delivery with scheduled and terminal signals.
                        for (final List<T> window : fullWindows) {
                            downDispatcher.onNext(window);
                        }
                    }
                }
            }

            @Override
            public void onError(final Exception error) {
                if (terminate(false) != TERMINATED_WINDOWS) {
                    super.onError(error);
                }
            }

            @Override
            public void onComplete() {
                final List<List<T>> pending = terminate(true);

                if (pending == TERMINATED_WINDOWS) {
                    return;
                }

                if (downDispatcher != null) {
                    try {
                        for (final List<T> window : pending) {
                            if (isLimitReached()) {
                                break;
                            }
                            downDispatcher.onNext(window);
                        }
                    } catch (final Exception e) {
                        super.onError(e);
                        return;
                    }
                }

                super.onComplete();
            }

            private void startWindow() {
                Exception failure = null;

                synchronized (windows) {
                    if (terminated) {
                        return;
                    }

                    final List<T> window = new ArrayList<>();
                    windows.add(window);

                    try {
                        windowFutures.put(window, schedulerForIntermediateOp.schedule(() -> runEvent(() -> emitWindow(window)), timespan, unit));
                    } catch (final Exception e) {
                        windows.remove(window);
                        hasMore = false;
                        terminateLocked();
                        failure = e;
                    }
                }

                if (failure != null) {
                    super.onError(failure);
                }
            }

            private void emitWindow(final List<T> window) {
                Exception failure = null;

                synchronized (windows) {
                    // Equal contents do not identify the same overlapping window or scheduled callback.
                    if (terminated || !windows.removeIf(activeWindow -> activeWindow == window)) {
                        return;
                    }

                    windowFutures.remove(window);

                    if (downDispatcher != null) {
                        try {
                            // Serialize scheduled delivery with source and terminal signals.
                            downDispatcher.onNext(new ArrayList<>(window));
                        } catch (final Exception e) {
                            hasMore = false;
                            terminateLocked();
                            failure = e;
                        }
                    }
                }

                if (failure != null) {
                    super.onError(failure);
                }
            }

            private List<List<T>> terminate(final boolean takePending) {
                synchronized (windows) {
                    if (terminated) {
                        return TERMINATED_WINDOWS;
                    }

                    final List<List<T>> pending = new ArrayList<>();

                    if (takePending) {
                        for (final List<T> window : windows) {
                            if (N.notEmpty(window)) {
                                pending.add(new ArrayList<>(window));
                            }
                        }
                    }

                    terminateLocked();
                    return pending;
                }
            }

            private void terminateLocked() {
                terminated = true;

                final ScheduledFuture<?> starter = windowStarter;

                if (starter != null) {
                    starter.cancel(false);
                }

                for (final ScheduledFuture<?> future : windowFutures.values()) {
                    future.cancel(false);
                }

                windows.clear();
                windowFutures.clear();
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
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if {@code action} is {@code null}.
    
     * @see #observe(Consumer, Consumer)
     * @see #observe(Consumer, Consumer, Runnable)
     */
    public void observe(final Consumer<? super T> action) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgNotNull(action, cs.action);

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
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if any of {@code action}, {@code onError} is {@code null}.
    
     * @see #observe(Consumer, Consumer, Runnable)
     */
    public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError) throws IllegalStateException, IllegalArgumentException {
        checkCurrentStage();
        N.checkArgNotNull(action, cs.action);
        N.checkArgNotNull(onError, cs.onError);

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
     * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
     * @throws IllegalArgumentException if any callback is {@code null}.
     */
    public abstract void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
            throws IllegalStateException, IllegalArgumentException;

    /** Cancels and forgets all periodic intermediate-operation tasks associated with this Observer. */
    void cancelScheduledFutures() {
        if (N.notEmpty(scheduledFutures)) {
            for (final ScheduledFuture<?> future : scheduledFutures.keySet()) {
                future.cancel(false);
            }

            scheduledFutures.clear();
        }
    }

    /**
     * Internal dispatcher class that handles the chaining of operators and event propagation
     * through the Observer pipeline. Each operator appends a new Dispatcher to the chain.
     *
     * @param <T> the type of items handled by this dispatcher
     */
    protected static class Dispatcher<T> {

        /** Creates an empty dispatcher with no downstream dispatcher. */
        protected Dispatcher() {
            // Empty by design.
        }

        /** Holds the most-recently-received item for debounce/throttle operators; initialised to the sentinel {@code NONE}. */
        protected final Holder<Object> holder = Holder.of(NONE);

        /** The next dispatcher in the chain, or {@code null} if this is the last dispatcher. */
        protected Dispatcher<T> downDispatcher;

        // Only limits later in the pipeline constrain this dispatcher's output. The observer's
        // hasMore flag also represents upstream completion and cannot distinguish that case.
        boolean isLimitReached() {
            return downDispatcher != null && downDispatcher.isLimitReached();
        }

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
         * @throws IllegalArgumentException if {@code downDispatcher} is {@code null}.
         */
        public void append(final Dispatcher<T> downDispatcher) throws IllegalArgumentException {
            N.checkArgNotNull(downDispatcher, cs.downDispatcher);

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
         * @param onError the consumer to invoke when an error is signalled
         * @param onComplete the runnable to invoke when the stream completes
         * @throws IllegalArgumentException if {@code onError} or {@code onComplete} is {@code null}
         */
        protected DispatcherBase(final Consumer<? super Exception> onError, final Runnable onComplete) throws IllegalArgumentException {
            N.checkArgNotNull(onError, cs.onError);
            N.checkArgNotNull(onComplete, cs.onComplete);

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
     * or emission is stopped by a downstream operator such as {@link #limit(long)}; an empty
     * queue does not stop emission.
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
         * @param action the action to perform on each item
         * @param onError the consumer invoked if an exception occurs during emission
         * @param onComplete the runnable invoked when the queue signals completion
         * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
         * @throws IllegalArgumentException if any of {@code action}, {@code onError}, or {@code onComplete} is
         *         {@code null}.
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalStateException, IllegalArgumentException {
            beginSubscription(action, onError, onComplete);

            dispatcher.append(new DispatcherBase<>(e -> ((Observer<?>) this).deliverError(onError, e), () -> ((Observer<?>) this).deliverComplete(onComplete)) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            startSubscriptionActions();

            ((Observer<?>) this).startSource(() -> asyncExecutor.execute(() -> {
                try {
                    while (hasMore) {
                        final T next = ((Observer<?>) this).pollSource(queue);
                        if (!hasMore || next == COMPLETE_FLAG) {
                            break;
                        }
                        if (next != null) {
                            ((Observer<?>) this).emitSource(next);
                        }
                    }
                    ((Observer<?>) this).finishSubscription(null);
                } catch (final Error fatal) {
                    ((Observer<?>) this).abortSubscription(fatal);
                    throw fatal;
                } catch (final Exception e) {
                    ((Observer<?>) this).reportSourceFailure(e);
                }
            }));
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
         * @param action the action to perform on each item
         * @param onError the consumer invoked if an exception occurs during emission
         * @param onComplete the runnable invoked when the iterator is exhausted
         * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
         * @throws IllegalArgumentException if any of {@code action}, {@code onError}, or {@code onComplete} is
         *         {@code null}.
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalStateException, IllegalArgumentException {
            beginSubscription(action, onError, onComplete);

            dispatcher.append(new DispatcherBase<>(e -> ((Observer<?>) this).deliverError(onError, e), () -> ((Observer<?>) this).deliverComplete(onComplete)) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            startSubscriptionActions();

            ((Observer<?>) this).startSource(() -> asyncExecutor.execute(() -> {
                try {
                    while (hasMore && iter.hasNext()) {
                        if (!hasMore) {
                            break;
                        }
                        ((Observer<?>) this).emitSource(iter.next());
                    }
                    ((Observer<?>) this).finishSubscription(null);
                } catch (final Error fatal) {
                    ((Observer<?>) this).abortSubscription(fatal);
                    throw fatal;
                } catch (final Exception e) {
                    ((Observer<?>) this).reportSourceFailure(e);
                }
            }));
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
         * {@link Observer#schedulerForObserveOp}, then signals completion. If an upstream
         * stop condition (for example {@code limit(0)}) is already in effect, no value is
         * emitted and completion is signalled asynchronously without waiting for the timer.
         *
         * @param action the action to perform when the timer fires
         * @param onError the consumer invoked if an exception occurs during the scheduled emission
         * @param onComplete the runnable invoked immediately after the single emission
         * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
         * @throws IllegalArgumentException if any of {@code action}, {@code onError}, or {@code onComplete} is
         *         {@code null}.
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalStateException, IllegalArgumentException {
            beginSubscription(action, onError, onComplete);

            dispatcher.append(new DispatcherBase<>(e -> ((Observer<?>) this).deliverError(onError, e), () -> ((Observer<?>) this).deliverComplete(onComplete)) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            startSubscriptionActions();

            final Runnable task = () -> {
                ((Observer<?>) this).emitSource(0L);
                ((Observer<?>) this).finishSubscription(null);
            };
            ((Observer<?>) this).startSource(() -> {
                if (hasMore) {
                    ((Observer<?>) this).publishSourceFuture(schedulerForObserveOp.schedule(task, delay, unit));
                } else {
                    asyncExecutor.execute(task);
                }
            });
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
         * If production was stopped before subscription (for example by {@code limit(0)}),
         * completion is signalled asynchronously without waiting for {@code initialDelay}.
         *
         * @param action the action to perform on each emission
         * @param onError the consumer invoked if an exception occurs during an emission
         * @param onComplete the runnable invoked when the interval is cancelled
         * @throws IllegalStateException if this stage has been replaced or the pipeline has already been subscribed
         * @throws IllegalArgumentException if any of {@code action}, {@code onError}, or {@code onComplete} is
         *         {@code null}.
         */
        @Override
        public void observe(final Consumer<? super T> action, final Consumer<? super Exception> onError, final Runnable onComplete)
                throws IllegalStateException, IllegalArgumentException {
            beginSubscription(action, onError, onComplete);

            dispatcher.append(new DispatcherBase<>(e -> ((Observer<?>) this).deliverError(onError, e), () -> ((Observer<?>) this).deliverComplete(onComplete)) {
                @Override
                public void onNext(final Object param) {
                    action.accept((T) param);
                }
            });

            startSubscriptionActions();

            ((Observer<?>) this).startSource(() -> {
                if (!hasMore) {
                    asyncExecutor.execute(() -> ((Observer<?>) this).finishSubscription(null));
                    return;
                }
                final AtomicLong value = new AtomicLong();
                final ScheduledFuture<?> task = schedulerForObserveOp.scheduleAtFixedRate(() -> ((Observer<?>) this).emitSource(value.getAndIncrement()),
                        initialDelay, period, unit);
                ((Observer<?>) this).publishSourceFuture(task);
            });
        }

    }
}
