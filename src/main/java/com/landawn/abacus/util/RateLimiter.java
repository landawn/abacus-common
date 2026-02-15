/*
 * Copyright (C) 2012 The Guava Authors
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

import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.util.SmoothRateLimiter.SmoothBursty;
import com.landawn.abacus.util.SmoothRateLimiter.SmoothWarmingUp;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p> 
 *
 * A rate limiter. Conceptually, a rate limiter distributes permits at a configurable rate. Each
 * {@link #acquire()} blocks if necessary until a permit is available, and then takes it. Once
 * acquired, permits need not be released.
 *
 * <p>Rate limiters are often used to restrict the rate at which some physical or logical resource
 * is accessed. This is in contrast to {@link java.util.concurrent.Semaphore} which restricts the
 * number of concurrent accesses instead of the rate (note though that concurrency and rate are
 * closely related, e.g., see <a href="http://en.wikipedia.org/wiki/Little%27s_law">Little's
 * Law</a>).
 *
 * <p>A {@code RateLimiter} is defined primarily by the rate at which permits are issued. Absent
 * additional configuration, permits will be distributed at a fixed rate, defined in terms of
 * permits per second. Permits will be distributed smoothly, with the delay between individual
 * permits being adjusted to ensure that the configured rate is maintained.
 *
 * <p>It is possible to configure a {@code RateLimiter} to have a warmup period during which time
 * the permits issued each second steadily increase until it hits the stable rate.
 *
 * <p>As an example, imagine that we have a list of tasks to execute, but we don't want to submit
 * more than 2 per second: 
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *  final RateLimiter rateLimiter = RateLimiter.create(2.0);   // rate is "2 permits per second"
 *  void submitTasks(List<Runnable> tasks, Executor executor) {
 *    for (Runnable task : tasks) {
 *      rateLimiter.acquire();   // may wait
 *      executor.execute(task);
 *    }
 *  }}</pre>
 *
 * <p>As another example, imagine that we produce a stream of data, and we want to cap it at 5kb per
 * second. This could be accomplished by requiring a permit per byte, and specifying a rate of 5000
 * permits per second: <pre>   {@code
 *  final RateLimiter rateLimiter = RateLimiter.create(5000.0);   // rate = 5000 permits per second
 *  void submitPacket(byte[] packet) {
 *    rateLimiter.acquire(packet.length);
 *    networkService.send(packet);
 *  }
 * }</pre>
 *
 * <p>It is important to note that the number of permits requested <i>never</i> affects the
 * throttling of the request itself (an invocation to {@code acquire(1)} and an invocation to
 * {@code acquire(1000)} will result in exactly the same throttling, if any), but it affects the
 * throttling of the <i>next</i> request. I.e., if an expensive task arrives at an idle RateLimiter,
 * it will be granted immediately, but it is the <i>next</i> request that will experience extra
 * throttling, thus paying for the cost of the expensive task.
 *
 * <p>Note: {@code RateLimiter} does not provide fairness guarantees.
 *
 * @author Dimitris Andreou
 */
// TODO(user): switch to nano precision. A natural unit of cost is "bytes", and a micro precision
// would mean a maximum rate of "1MB/s", which might be small in some cases.
public abstract class RateLimiter {

    // TODO(user): "This is equivalent to
    // {@code createWithCapacity(permitsPerSecond, 1, TimeUnit.SECONDS)}".
    /**
     * Creates a {@code RateLimiter} with the specified stable throughput, given as
     * "permits per second" (commonly referred to as <i>QPS</i>, queries per second).
     * This factory method creates a rate limiter with smooth bursty behavior, allowing
     * unused permits to accumulate up to one second's worth.
     *
     * <p>The returned {@code RateLimiter} ensures that on average no more than {@code
     * permitsPerSecond} are issued during any given second, with sustained requests being smoothly
     * spread over each second. When the incoming request rate exceeds {@code permitsPerSecond} the
     * rate limiter will release one permit every {@code
     * (1.0 / permitsPerSecond)} seconds. When the rate limiter is unused, bursts of up to
     * {@code permitsPerSecond} permits will be allowed, with subsequent requests being smoothly
     * limited at the stable rate of {@code permitsPerSecond}.
     *
     * <p><b>Thread Safety:</b> This method is thread-safe. The returned {@code RateLimiter} instance
     * is also thread-safe and can be safely shared across multiple threads. All permit acquisition
     * methods ({@link #acquire}, {@link #tryAcquire}, etc.) are synchronized internally to ensure
     * consistent behavior when accessed concurrently.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(5.0);   // 5 permits per second
     * limiter.acquire();                               // Acquires one permit, may wait if necessary
     * }</pre>
     *
     * @param permitsPerSecond the rate of the returned {@code RateLimiter}, measured in how many
     *     permits become available per second, must be positive and not NaN
     * @return a newly created {@code RateLimiter} with the specified rate
     * @throws IllegalArgumentException if {@code permitsPerSecond} is negative, zero, or NaN
     */
    public static RateLimiter create(final double permitsPerSecond) {
        /*
         * The default RateLimiter configuration can save the unused permits of up to one second. This
         * is to avoid unnecessary stalls in situations like this: A RateLimiter of 1qps, and 4 threads,
         * all calling acquire() at these moments:
         *
         * T0 at 0 seconds
         * T1 at 1.05 seconds
         * T2 at 2 seconds
         * T3 at 3 seconds
         *
         * Due to the slight delay of T1, T2 would have to sleep till 2.05 seconds, and T3 would also
         * have to sleep till 3.05 seconds.
         */
        N.checkArgument(permitsPerSecond > 0.0 && !Double.isNaN(permitsPerSecond), "rate must be positive");
        return create(permitsPerSecond, SleepingStopwatch.createFromSystemTimer());
    }

    /**
     * Creates a {@code RateLimiter} with the specified stable throughput and custom stopwatch.
     *
     * @param permitsPerSecond the rate of permits per second
     * @param stopwatch the stopwatch to use for timing
     * @return a newly created {@code RateLimiter} with the specified rate
     */
    static RateLimiter create(final double permitsPerSecond, final SleepingStopwatch stopwatch) {
        final RateLimiter rateLimiter = new SmoothBursty(stopwatch, 1.0 /* maxBurstSeconds */);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    /**
     * Creates a {@code RateLimiter} with the specified stable throughput, given as
     * "permits per second" (commonly referred to as <i>QPS</i>, queries per second), and a <i>warmup
     * period</i>, during which the {@code RateLimiter} smoothly ramps up its rate, until it reaches
     * its maximum rate at the end of the period (as long as there are enough requests to saturate
     * it). Similarly, if the {@code RateLimiter} is left <i>unused</i> for a duration of
     * {@code warmupPeriod}, it will gradually return to its "cold" state, i.e., it will go through the
     * same warming-up process as when it was first created.
     *
     * <p>The returned {@code RateLimiter} is intended for cases where the resource that actually
     * fulfills the requests (e.g., a remote server) needs "warmup" time, rather than being
     * immediately accessed at the stable (maximum) rate.
     *
     * <p>The returned {@code RateLimiter} starts in a "cold" state (i.e., the warmup period will
     * follow), and if it is left unused for long enough, it will return to that state.
     *
     * <p><b>Thread Safety:</b> This method is thread-safe. The returned {@code RateLimiter} instance
     * is also thread-safe and can be safely shared across multiple threads. All permit acquisition
     * methods ({@link #acquire}, {@link #tryAcquire}, etc.) are synchronized internally to ensure
     * consistent behavior when accessed concurrently.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a rate limiter with 10 permits/sec and 3 second warmup
     * RateLimiter limiter = RateLimiter.create(10.0, 3, TimeUnit.SECONDS);
     * limiter.acquire();   // Initial requests will be slower during warmup
     * }</pre>
     *
     * @param permitsPerSecond the rate of the returned {@code RateLimiter}, measured in how many
     *     permits become available per second, must be positive
     * @param warmupPeriod the duration of the period where the {@code RateLimiter} ramps up its rate,
     *     before reaching its stable (maximum) rate, must be non-negative
     * @param unit the time unit of the warmupPeriod argument, must not be null
     * @return a newly created {@code RateLimiter} with the specified rate and warmup period
     * @throws IllegalArgumentException if {@code permitsPerSecond} is negative or zero, or
     *     {@code warmupPeriod} is negative
     */
    public static RateLimiter create(final double permitsPerSecond, final long warmupPeriod, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgument(warmupPeriod >= 0, "warmupPeriod must not be negative: %s", warmupPeriod);
        return create(permitsPerSecond, warmupPeriod, unit, 3.0, SleepingStopwatch.createFromSystemTimer());
    }

    /**
     * Creates a {@code RateLimiter} with warmup period, cold factor, and custom stopwatch.
     *
     * @param permitsPerSecond the rate of permits per second
     * @param warmupPeriod the duration of the warmup period
     * @param unit the time unit for the warmup period
     * @param coldFactor the cold factor for warmup behavior
     * @param stopwatch the stopwatch to use for timing
     * @return a newly created {@code RateLimiter} with the specified rate and warmup behavior
     */
    static RateLimiter create(final double permitsPerSecond, final long warmupPeriod, final TimeUnit unit, final double coldFactor,
            final SleepingStopwatch stopwatch) {
        final RateLimiter rateLimiter = new SmoothWarmingUp(stopwatch, warmupPeriod, unit, coldFactor);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    /**
     * The underlying timer; used both to measure elapsed time and sleep as necessary. A separate
     * object to facilitate testing.
     */
    private final SleepingStopwatch stopwatch;

    // Can't be initialized in the constructor because mocks don't call the constructor.
    private volatile Object mutexDoNotUseDirectly; //NOSONAR

    private Object mutex() {
        Object result = mutexDoNotUseDirectly;
        if (result == null) {
            synchronized (this) {
                result = mutexDoNotUseDirectly;
                if (result == null) {
                    result = new Object();
                    mutexDoNotUseDirectly = result;
                }
            }
        }
        return result;
    }

    RateLimiter(final SleepingStopwatch stopwatch) {
        this.stopwatch = N.checkArgNotNull(stopwatch);
    }

    /**
     * Updates the stable rate of this {@code RateLimiter}, that is, the {@code permitsPerSecond}
     * argument provided in the factory method that constructed the {@code RateLimiter}. This method
     * allows dynamic adjustment of the rate limiter's throughput without creating a new instance.
     *
     * <p>Currently throttled threads will <b>not</b> be awakened as a result of this invocation,
     * thus they do not observe the new rate; only subsequent requests will.
     *
     * <p>Note though that, since each request repays (by waiting, if necessary) the cost of the
     * <i>previous</i> request, this means that the very next request after an invocation to
     * {@code setRate} will not be affected by the new rate; it will pay the cost of the previous
     * request, which is in terms of the previous rate.
     *
     * <p>The behavior of the {@code RateLimiter} is not modified in any other way, e.g., if the
     * {@code RateLimiter} was configured with a warmup period of 20 seconds, it still has a warmup
     * period of 20 seconds after this method invocation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(5.0);
     * limiter.setRate(10.0);   // Increase rate to 10 permits/second
     * }</pre>
     *
     * @param permitsPerSecond the new stable rate of this {@code RateLimiter}, must be positive and not NaN
     * @throws IllegalArgumentException if {@code permitsPerSecond} is negative, zero, or NaN
     * @see #getRate()
     * @see #create(double)
     */
    public final void setRate(final double permitsPerSecond) throws IllegalArgumentException {
        //noinspection ConstantValue
        N.checkArgument(permitsPerSecond > 0.0 && !Double.isNaN(permitsPerSecond), "rate must be positive");
        synchronized (mutex()) {
            doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }

    /**
     * Internal method to set the rate. Subclasses must implement this to update internal state
     * when the rate is changed via {@link #setRate(double)}.
     *
     * <p><b>Implementation Requirements:</b>
     * <ul>
     * <li>This method is called while holding the internal mutex lock, so implementations do not
     *     need to provide additional synchronization.</li>
     * <li>Implementations must update their internal state to reflect the new {@code permitsPerSecond}
     *     rate, including recalculating any derived values such as stable interval or stored permits.</li>
     * <li>Implementations should use {@code nowMicros} to properly adjust any time-based state to
     *     ensure correct permit calculations after the rate change.</li>
     * <li>The implementation should not block or perform I/O operations as this would hold the lock
     *     for an extended period.</li>
     * </ul>
     *
     * @param permitsPerSecond the new rate of permits per second, guaranteed to be positive and not NaN
     * @param nowMicros the current time in microseconds, used for state synchronization
     */
    abstract void doSetRate(double permitsPerSecond, long nowMicros);

    /**
     * Returns the stable rate (as {@code permits per seconds}) with which this {@code RateLimiter} is
     * configured. The initial value of this is the same as the {@code permitsPerSecond} argument
     * passed in the factory method that produced this {@code RateLimiter}, and it is only updated
     * after invocations to {@linkplain #setRate}.
     *
     * <p>This method is thread-safe and can be called concurrently with other rate limiter operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(5.0);
     * double currentRate = limiter.getRate();   // Returns 5.0
     * }</pre>
     *
     * @return the current stable rate in permits per second
     * @see #setRate(double)
     */
    public final double getRate() {
        synchronized (mutex()) {
            return doGetRate();
        }
    }

    /**
     * Internal method to get the rate. Subclasses must implement this to return the current
     * rate configuration.
     *
     * <p><b>Implementation Requirements:</b>
     * <ul>
     * <li>This method is called while holding the internal mutex lock, ensuring thread-safe
     *     access to the rate value.</li>
     * <li>Implementations must return the current stable rate in permits per second, which
     *     should be the same value that was most recently set via {@link #doSetRate(double, long)}.</li>
     * <li>The returned value must be positive and not NaN.</li>
     * <li>This method should be lightweight and non-blocking, as it is called while holding a lock.</li>
     * </ul>
     *
     * @return the current stable rate in permits per second, guaranteed to be positive
     */
    abstract double doGetRate();

    /**
     * Acquires a single permit from this {@code RateLimiter}, blocking until the request can be
     * granted. This method blocks indefinitely until a permit is available and returns the amount
     * of time spent waiting.
     *
     * <p>This method is equivalent to {@code acquire(1)}.
     *
     * <p>If the rate limiter has unused permits available, this method will return immediately
     * with a return value of 0.0. Otherwise, it will sleep until a permit becomes available.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(2.0);   // 2 permits per second
     * double waitTime = limiter.acquire();             // Acquires 1 permit
     * System.out.println("Waited " + waitTime + " seconds");
     * }</pre>
     *
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @see #acquire(int)
     * @see #tryAcquire()
     * @see #tryAcquire(int)
     * @see #tryAcquire(long, TimeUnit)
     * @see #tryAcquire(int, long, TimeUnit)
     */
    public double acquire() {
        return acquire(1);
    }

    /**
     * Acquires the given number of permits from this {@code RateLimiter}, blocking until the request
     * can be granted. This method blocks indefinitely until the requested permits are available and
     * returns the amount of time spent waiting.
     *
     * <p>Note that the number of permits requested affects the throttling of the <i>next</i> request,
     * not the current one. If this method is called on an idle rate limiter, it will return immediately
     * (even for a large number of permits), but subsequent requests will be throttled to compensate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(5.0);   // 5 permits per second
     * double waitTime = limiter.acquire(3);            // Acquires 3 permits
     * System.out.println("Waited " + waitTime + " seconds for 3 permits");
     * }</pre>
     *
     * @param permits the number of permits to acquire, must be positive
     * @return time spent sleeping to enforce rate, in seconds; 0.0 if not rate-limited
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @see #acquire()
     * @see #tryAcquire()
     * @see #tryAcquire(int)
     * @see #tryAcquire(long, TimeUnit)
     * @see #tryAcquire(int, long, TimeUnit)
     */
    public double acquire(final int permits) {
        final long microsToWait = reserve(permits);
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    /**
     * Reserves the given number of permits from this {@code RateLimiter} for future use, returning
     * the number of microseconds until the reservation can be consumed.
     *
     * @param permits the number of permits to acquire
     * @return time in microseconds to wait until the resource can be acquired, never negative
     */
    final long reserve(final int permits) {
        checkPermits(permits);
        synchronized (mutex()) {
            return reserveAndGetWaitLength(permits, stopwatch.readMicros());
        }
    }

    /**
     * Acquires a permit from this {@code RateLimiter} if it can be obtained without exceeding the
     * specified {@code timeout}, or returns {@code false} immediately (without waiting) if the permit
     * had not been granted before the timeout expired.
     *
     * <p>This method is equivalent to {@code tryAcquire(1, timeout, unit)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(2.0);
     * if (limiter.tryAcquire(100, TimeUnit.MILLISECONDS)) {
     *     // Permit acquired within 100ms
     * } else {
     *     // Timeout expired
     * }
     * }</pre>
     *
     * @param timeout the maximum time to wait for the permit. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument, must not be null
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @see #tryAcquire()
     * @see #tryAcquire(int)
     * @see #tryAcquire(int, long, TimeUnit)
     * @see #acquire()
     * @see #acquire(int)
     */
    public boolean tryAcquire(final long timeout, final TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    /**
     * Acquires the specified number of permits from this {@link RateLimiter} if they can be acquired
     * immediately without any delay. This is a non-blocking operation that returns immediately.
     *
     * <p>This method is equivalent to {@code tryAcquire(permits, 0, anyUnit)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(5.0);
     * if (limiter.tryAcquire(3)) {
     *     // Successfully acquired 3 permits immediately
     * } else {
     *     // Permits not available, no waiting performed
     * }
     * }</pre>
     *
     * @param permits the number of permits to acquire, must be positive
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @see #tryAcquire()
     * @see #tryAcquire(long, TimeUnit)
     * @see #tryAcquire(int, long, TimeUnit)
     * @see #acquire()
     * @see #acquire(int)
     */
    public boolean tryAcquire(final int permits) {
        return tryAcquire(permits, 0, MICROSECONDS);
    }

    /**
     * Acquires a single permit from this {@link RateLimiter} if it can be acquired immediately without
     * any delay. This is a non-blocking operation that returns immediately.
     *
     * <p>This method is equivalent to {@code tryAcquire(1)}.
     *
     * <p>This is useful for operations that should only proceed if resources are immediately available,
     * without waiting or queueing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(10.0);
     * if (limiter.tryAcquire()) {
     *     // Permit acquired immediately, proceed with operation
     *     processRequest();
     * } else {
     *     // No permit available, skip or defer operation
     * }
     * }</pre>
     *
     * @return {@code true} if the permit was acquired, {@code false} otherwise
     * @see #tryAcquire(int)
     * @see #tryAcquire(long, TimeUnit)
     * @see #tryAcquire(int, long, TimeUnit)
     * @see #acquire()
     * @see #acquire(int)
     */
    public boolean tryAcquire() {
        return tryAcquire(1, 0, MICROSECONDS);
    }

    /**
     * Acquires the given number of permits from this {@code RateLimiter} if they can be obtained
     * without exceeding the specified {@code timeout}, or returns {@code false} immediately (without
     * waiting) if the permits had not been granted before the timeout expired.
     *
     * <p>This method will block for up to the specified timeout duration waiting for permits to become
     * available. If permits are available within the timeout, they are acquired and the method returns
     * {@code true}. Otherwise, it returns {@code false} without acquiring any permits.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RateLimiter limiter = RateLimiter.create(2.0);
     * if (limiter.tryAcquire(5, 2, TimeUnit.SECONDS)) {
     *     // Successfully acquired 5 permits within 2 seconds
     *     processBatch();
     * } else {
     *     // Could not acquire permits within timeout
     *     handleTimeout();
     * }
     * }</pre>
     *
     * @param permits the number of permits to acquire, must be positive
     * @param timeout the maximum time to wait for the permits. Negative values are treated as zero.
     * @param unit the time unit of the timeout argument, must not be null
     * @return {@code true} if the permits were acquired, {@code false} otherwise
     * @throws IllegalArgumentException if the requested number of permits is negative or zero
     * @see #tryAcquire()
     * @see #tryAcquire(int)
     * @see #tryAcquire(long, TimeUnit)
     * @see #acquire()
     * @see #acquire(int)
     */
    public boolean tryAcquire(final int permits, final long timeout, final TimeUnit unit) {
        final long timeoutMicros = max(unit.toMicros(timeout), 0);
        checkPermits(permits);
        long microsToWait;
        synchronized (mutex()) {
            final long nowMicros = stopwatch.readMicros();
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false;
            } else {
                microsToWait = reserveAndGetWaitLength(permits, nowMicros);
            }
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    /**
     * Checks if permits can be acquired within the given timeout.
     *
     * @param nowMicros the current time in microseconds
     * @param timeoutMicros the maximum time to wait in microseconds
     * @return {@code true} if permits can be acquired within the timeout, {@code false} otherwise
     */
    private boolean canAcquire(final long nowMicros, final long timeoutMicros) {
        return queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros;
    }

    /**
     * Reserves the next ticket and returns the wait time that the caller must wait for.
     *
     * @param permits the number of permits to acquire
     * @param nowMicros the current time in microseconds
     * @return the time in microseconds that the caller must wait
     */
    final long reserveAndGetWaitLength(final int permits, final long nowMicros) {
        final long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return max(momentAvailable - nowMicros, 0);
    }

    /**
     * Returns the earliest time that permits are available (with one caveat).
     *
     * <p><b>Implementation Requirements:</b>
     * <ul>
     * <li>This method is called while holding the internal mutex lock, so implementations do not
     *     need to provide additional synchronization.</li>
     * <li>Implementations must return the timestamp (in microseconds) representing the earliest
     *     moment at which permits will be available for acquisition.</li>
     * <li>If permits are currently available, this may return a time in the past or equal to
     *     {@code nowMicros}.</li>
     * <li>If no permits are available, this should return a future timestamp indicating when
     *     the next permit will become available.</li>
     * <li>This method should not modify any internal state; it is purely a query operation.</li>
     * <li>The implementation must account for stored permits, stable interval, and any other
     *     rate limiter-specific state (e.g., warmup period).</li>
     * </ul>
     *
     * @param nowMicros the current time in microseconds, used as a reference point
     * @return the earliest time (in microseconds) when permits are available, which may be an
     *         arbitrary past, present, or future time
     */
    abstract long queryEarliestAvailable(long nowMicros);

    /**
     * Reserves the requested number of permits and returns the time that those permits can be used
     * (with one caveat).
     *
     * <p><b>Implementation Requirements:</b>
     * <ul>
     * <li>This method is called while holding the internal mutex lock, so implementations do not
     *     need to provide additional synchronization.</li>
     * <li>Implementations must reserve the specified number of {@code permits} and update internal
     *     state accordingly (e.g., decrement stored permits, advance next free ticket time).</li>
     * <li>The returned timestamp represents when these permits can actually be used. For an idle
     *     rate limiter, this is typically {@code nowMicros} or earlier, meaning the permits can
     *     be used immediately.</li>
     * <li>For a saturated rate limiter, this returns a future timestamp. The current request does
     *     not wait; instead, <i>future</i> requests will be delayed to account for these permits.</li>
     * <li>Implementations must update state such that subsequent calls to {@link #queryEarliestAvailable(long)}
     *     and {@link #reserveEarliestAvailable(int, long)} reflect the consumption of these permits.</li>
     * <li>The number of permits requested affects throttling of <i>subsequent</i> requests, not the
     *     current one. This implements the rate limiter's "pay for the past" behavior.</li>
     * </ul>
     *
     * @param permits the number of permits to reserve and acquire, guaranteed to be positive
     * @param nowMicros the current time in microseconds, used for state updates
     * @return the time (in microseconds) when the reserved permits can be used, which may be an
     *         arbitrary past, present, or future time
     */
    abstract long reserveEarliestAvailable(int permits, long nowMicros);

    /**
     * Returns a string representation of this {@code RateLimiter}, showing its current stable rate.
     * The format is "RateLimiter[stableRate=X.Xqps]" where X.X is the current rate in queries per second.
     *
     * <p>Example output: {@code RateLimiter[stableRate=5.0qps]}
     *
     * @return a string representation of this rate limiter
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "RateLimiter[stableRate=%3.1fqps]", getRate());
    }

    /**
     * Abstract base class for a stopwatch that provides both time measurement and sleeping capabilities.
     * This class is used internally by {@code RateLimiter} to handle timing operations in a testable way.
     *
     * <p>The primary purpose of this abstraction is to separate time measurement and thread sleeping
     * from the core rate limiting logic, making it possible to test {@code RateLimiter} with simulated
     * or accelerated time without actual thread sleep delays.
     *
     * <p><b>Thread Safety:</b> Implementations of this class do not need to be thread-safe themselves,
     * as the {@code RateLimiter} ensures synchronized access to stopwatch methods through its internal
     * mutex locking mechanism.
     *
     * <p>Concrete implementations must provide:
     * <ul>
     * <li>{@link #readMicros()} - Returns the elapsed time in microseconds since the stopwatch started</li>
     * <li>{@link #sleepMicrosUninterruptibly(long)} - Sleeps for a specified duration without being interrupted</li>
     * </ul>
     *
     * @see #createFromSystemTimer()
     * @see RateLimiter
     */
    abstract static class SleepingStopwatch {
        /**
         * Protected constructor to prevent external instantiation.
         * Only subclasses within this package can extend this class.
         */
        protected SleepingStopwatch() {
        }

        /**
         * Returns the elapsed time in microseconds since this stopwatch was created or started.
         * This method is used by {@code RateLimiter} to track time for permit distribution and throttling.
         *
         * <p><b>Implementation Requirements:</b>
         * <ul>
         * <li>Implementations must return a monotonically increasing value representing elapsed time
         *     in microseconds.</li>
         * <li>The returned value should be consistent with the time base used for sleeping operations
         *     in {@link #sleepMicrosUninterruptibly(long)}.</li>
         * <li>This method is typically called while the {@code RateLimiter} holds its internal mutex,
         *     but implementations should not rely on external synchronization.</li>
         * <li>The implementation should be efficient and non-blocking, as it is called frequently
         *     during rate limiter operations.</li>
         * </ul>
         *
         * <p><i>Note:</i> We always hold the mutex when calling this. TODO(cpovirk): Is that important?
         * Perhaps we need to guarantee that each call to reserveEarliestAvailable, etc. sees a value
         * &gt;= the previous? Also, is it OK that we don't hold the mutex when sleeping?
         *
         * @return the elapsed time in microseconds, must be monotonically increasing
         * @see #sleepMicrosUninterruptibly(long)
         * @see RateLimiter
         */
        protected abstract long readMicros();

        /**
         * Sleeps for the specified duration in microseconds without being interrupted.
         *
         * @param micros the duration to sleep in microseconds
         */
        protected abstract void sleepMicrosUninterruptibly(long micros);

        /**
         * Creates a {@code SleepingStopwatch} instance that uses the system timer for time measurement
         * and {@link N#sleepUninterruptibly} for sleeping. This is the standard implementation used by
         * {@code RateLimiter} for production use.
         *
         * <p>The returned stopwatch starts measuring elapsed time immediately from the moment of creation.
         * Time is measured in microseconds using a {@link Stopwatch} instance.
         *
         * @return a new {@code SleepingStopwatch} instance based on the system timer
         */
        public static SleepingStopwatch createFromSystemTimer() {
            return new SleepingStopwatch() {
                final Stopwatch stopwatch = Stopwatch.createStarted();

                @Override
                protected long readMicros() {
                    return stopwatch.elapsed(MICROSECONDS);
                }

                @Override
                protected void sleepMicrosUninterruptibly(final long micros) {
                    if (micros > 0) {
                        N.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                    }
                }
            };
        }
    }

    private static void checkPermits(final int permits) {
        N.checkArgument(permits > 0, "Requested permits (%s) must be positive", permits);
    }
}
