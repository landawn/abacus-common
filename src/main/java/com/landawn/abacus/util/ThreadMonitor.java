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
 * Monitors a thread, interrupting it when the specified timeout elapses.
 * <p>
 * This works by sleeping until the specified timeout amount and then
 * interrupting the thread being monitored. If the thread being monitored
 * completes its work before being interrupted, it should call {@link Thread#interrupt() interrupt()}
 * on the <i>monitor</i> thread (or use {@link #stop(Thread)}).
 * </p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 *       long timeoutInMillis = 1000;
 *       Thread monitor = ThreadMonitor.start(timeoutInMillis);
 *       try {
 *           Thread.sleep(2_000); // representative interruptible work
 *       } catch (InterruptedException e) {
 *           // timeout was reached
 *       } finally {
 *           ThreadMonitor.stop(monitor);
 *       }
 * }</pre>
 *
 * @version $Id: ThreadMonitor.java 1563227 2014-01-31 19:45:30Z ggregory $
 */
final class ThreadMonitor implements Runnable {

    private final Thread thread;

    private final long timeout;

    private boolean cancelled;

    /**
     * Starts monitoring the current thread with the specified timeout.
     *
     * <p>If the current thread does not complete its work within the specified timeout,
     * it will be interrupted by the monitor thread. Interruption is cooperative: this utility
     * does not forcibly terminate the monitored thread. The operation should react to interruption
     * and call {@link #stop(Thread)} to cancel the monitor when it completes before the timeout.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long timeoutInMillis = 5000;
     * Thread monitor = ThreadMonitor.start(timeoutInMillis);
     * try {
     *     Thread.sleep(10_000); // representative interruptible work
     * } catch (InterruptedException e) {
     *     // timeout was reached
     *     System.err.println("Operation timed out");
     * } finally {
     *     ThreadMonitor.stop(monitor);
     * }
     * }</pre>
     *
     * @param timeout the timeout amount in milliseconds; no monitor thread is started if the value is zero or less
     * @return the monitor thread, or {@code null} if {@code timeout} is not greater than zero
     */
    public static Thread start(final long timeout) {
        return start(Thread.currentThread(), timeout);
    }

    /**
     * Starts monitoring the specified thread with the given timeout.
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
     *         Thread.sleep(20_000); // representative interruptible work
     *     } catch (InterruptedException e) {
     *         System.err.println("Work interrupted due to timeout");
     *     } finally {
     *         ThreadMonitor.stop(monitor);
     *     }
     * });
     * workerThread.start();
     * }</pre>
     *
     * @param thread the thread to monitor; must not be {@code null}
     * @param timeout the timeout amount in milliseconds; no monitor thread is started if the value is zero or less
     * @return the monitor thread, or {@code null} if {@code timeout} is not greater than zero
     * @throws IllegalArgumentException if {@code thread} is {@code null}.
     */
    public static Thread start(final Thread thread, final long timeout) throws IllegalArgumentException {
        N.checkArgNotNull(thread, cs.thread);

        Thread monitor = null;
        if (timeout > 0) {
            final ThreadMonitor threadMonitor = new ThreadMonitor(thread, timeout);
            monitor = new MonitorThread(threadMonitor);
            monitor.setDaemon(true);
            monitor.start();
        }
        return monitor;
    }

    /**
     * Stops monitoring the specified thread and interrupts the monitor thread so that it wakes promptly.
     *
     * <p>This method should be called when the monitored operation completes successfully
     * before the timeout. Cancellation and timeout delivery are synchronized: if cancellation
     * wins, the target will not subsequently be interrupted; if timeout delivery has already won,
     * the target may already have been interrupted.</p>
     *
     * <p>It is safe to call this method with a {@code null} parameter; in such cases,
     * the method does nothing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Thread monitor = ThreadMonitor.start(5000);
     * try {
     *     Thread.sleep(10_000); // representative interruptible work
     * } catch (InterruptedException e) {
     *     // Timeout occurred
     *     System.err.println("Operation timed out");
     * } finally {
     *     // Always cancel a monitor that has not fired yet.
     *     ThreadMonitor.stop(monitor);
     * }
     * }</pre>
     *
     * @param thread the monitor thread to stop; may be {@code null}, in which case this method does nothing
     */
    public static void stop(final Thread thread) {
        if (thread instanceof MonitorThread monitorThread) {
            monitorThread.cancel();
        } else if (thread != null) {
            thread.interrupt();
        }
    }

    private void cancel() {
        synchronized (this) {
            cancelled = true;
        }
    }

    /**
     * Constructs a new {@code ThreadMonitor} that will interrupt {@code thread}
     * after {@code timeout} milliseconds.
     *
     * @param thread the thread to monitor
     * @param timeout the timeout in milliseconds
     */
    private ThreadMonitor(final Thread thread, final long timeout) {
        this.thread = thread;
        this.timeout = timeout;
    }

    /**
     * Sleeps for the configured timeout duration and then interrupts the monitored thread.
     * If this monitor thread is itself interrupted before the timeout elapses (i.e., the
     * monitored operation completed in time via a call to {@link #stop(Thread)}), the
     * {@link InterruptedException} is caught and the monitored thread is left undisturbed.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        try {
            Thread.sleep(timeout);

            synchronized (this) {
                if (!cancelled) {
                    thread.interrupt();
                }
            }
        } catch (final InterruptedException e) {
            // timeout isn't reached
        }
    }

    private static final class MonitorThread extends Thread {

        private final ThreadMonitor task;

        MonitorThread(final ThreadMonitor task) {
            super(task, ThreadMonitor.class.getSimpleName());
            this.task = task;
        }

        void cancel() {
            interrupt();
        }

        @Override
        public void interrupt() {
            task.cancel();
            super.interrupt();
        }
    }
}
