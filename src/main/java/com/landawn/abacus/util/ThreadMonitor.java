/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * Monitors a thread, interrupting it if it reaches the specified timeout.
 * <p>
 * This works by sleeping until the specified timeout amount and then
 * interrupting the thread being monitored. If the thread being monitored
 * completes its work before being interrupted, it should {@code interrupt()}
 * the <i>monitor</i> thread.
 * </p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *       long timeoutInMillis = 1000;
 *       try {
 *           Thread monitor = ThreadMonitor.start(timeoutInMillis);
 *           // do some work here
 *           ThreadMonitor.stop(monitor);
 *       } catch (InterruptedException e) {
 *           // timed amount was reached
 *       }
 * }</pre>
 *
 * @version $Id: ThreadMonitor.java 1563227 2014-01-31 19:45:30Z ggregory $
 */
final class ThreadMonitor implements Runnable {

    private final Thread thread;

    private final long timeout;

    /**
     * Start monitoring the current thread with the specified timeout.
     *
     * <p>If the current thread does not complete its work within the specified timeout,
     * it will be interrupted by the monitor thread. The monitored thread should handle
     * the interruption appropriately and call {@link #stop(Thread)} to terminate the
     * monitor thread when the work completes successfully.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long timeoutInMillis = 5000;
     * try {
     *     Thread monitor = ThreadMonitor.start(timeoutInMillis);
     *     // do some work here
     *     performLongRunningOperation();
     *     ThreadMonitor.stop(monitor);
     * } catch (InterruptedException e) {
     *     // timeout was reached
     *     System.err.println("Operation timed out");
     * }
     * }</pre>
     *
     * @param timeout The timeout amount in milliseconds
     * or no timeout if the value is zero or less
     * @return The monitor thread or {@code null}
     * if the timeout amount is not greater than zero
     */
    public static Thread start(final long timeout) {
        return start(Thread.currentThread(), timeout);
    }

    /**
     * Start monitoring the specified thread with the given timeout.
     *
     * <p>Creates a daemon monitor thread that will sleep for the specified timeout duration
     * and then interrupt the target thread if it's still running. If the target thread completes
     * its work before the timeout, it should call {@link #stop(Thread)} to terminate the monitor.</p>
     *
     * <p>The monitor thread is set as a daemon thread, so it won't prevent JVM shutdown.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Thread workerThread = new Thread(() -> {
     *     Thread monitor = ThreadMonitor.start(Thread.currentThread(), 10000);
     *     try {
     *         performWork();
     *         ThreadMonitor.stop(monitor);
     *     } catch (InterruptedException e) {
     *         System.err.println("Work interrupted due to timeout");
     *     }
     * });
     * workerThread.start();
     * }</pre>
     *
     * @param thread The thread to monitor, must not be {@code null}
     * @param timeout The timeout amount in milliseconds
     * or no timeout if the value is zero or less
     * @return The monitor thread or {@code null}
     * if the timeout amount is not greater than zero
     */
    public static Thread start(final Thread thread, final long timeout) {
        Thread monitor = null;
        if (timeout > 0) {
            final ThreadMonitor threadMonitor = new ThreadMonitor(thread, timeout);
            monitor = new Thread(threadMonitor, ThreadMonitor.class.getSimpleName());
            monitor.setDaemon(true);
            monitor.start();
        }
        return monitor;
    }

    /**
     * Stop monitoring the specified thread by interrupting the monitor thread.
     *
     * <p>This method should be called when the monitored operation completes successfully
     * before the timeout. It interrupts the monitor thread, causing it to exit its sleep
     * and terminate, preventing the timeout from occurring.</p>
     *
     * <p>It is safe to call this method with a {@code null} parameter; in such cases,
     * the method does nothing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Thread monitor = ThreadMonitor.start(5000);
     * try {
     *     // Perform work
     *     processData();
     *     // Work completed successfully, stop the monitor
     *     ThreadMonitor.stop(monitor);
     * } catch (InterruptedException e) {
     *     // Timeout occurred
     *     handleTimeout();
     * }
     * }</pre>
     *
     * @param thread The monitor thread to stop, may be {@code null}
     */
    public static void stop(final Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    private ThreadMonitor(final Thread thread, final long timeout) {
        this.thread = thread;
        this.timeout = timeout;
    }

    /**
     * Sleep until the specified timeout amount and then
     * interrupt the thread being monitored.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        try {
            Thread.sleep(timeout);
            thread.interrupt();
        } catch (final InterruptedException e) {
            // timeout isn't reached
        }
    }
}
