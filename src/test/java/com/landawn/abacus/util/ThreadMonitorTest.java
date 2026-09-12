package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ThreadMonitorTest extends TestBase {

    @Test
    public void testStartRejectsNullAndSkipsNonPositiveTimeouts() {
        assertThrows(IllegalArgumentException.class, () -> ThreadMonitor.start(null, 1));
        assertNull(ThreadMonitor.start(Thread.currentThread(), 0));
        assertNull(ThreadMonitor.start(Thread.currentThread(), -1));
    }

    @Test
    public void testStopBeforeTimeoutDoesNotInterruptTarget() throws Exception {
        CountDownLatch targetStarted = new CountDownLatch(1);
        CountDownLatch releaseTarget = new CountDownLatch(1);
        AtomicBoolean targetInterrupted = new AtomicBoolean();
        Thread target = new Thread(() -> {
            targetStarted.countDown();

            try {
                releaseTarget.await();
            } catch (InterruptedException e) {
                targetInterrupted.set(true);
            }
        });
        target.start();
        Thread monitor = null;

        try {
            assertTrue(targetStarted.await(1, TimeUnit.SECONDS));
            monitor = ThreadMonitor.start(target, TimeUnit.SECONDS.toMillis(10));
            ThreadMonitor.stop(monitor);
            monitor.join(TimeUnit.SECONDS.toMillis(1));

            assertFalse(monitor.isAlive());
            assertFalse(targetInterrupted.get());
        } finally {
            ThreadMonitor.stop(monitor);
            releaseTarget.countDown();
            target.interrupt();
            target.join(TimeUnit.SECONDS.toMillis(1));
        }

        assertFalse(target.isAlive());
    }

    @Test
    public void testDirectMonitorInterruptAlsoCancelsTimeout() throws Exception {
        Thread.interrupted();
        Thread monitor = ThreadMonitor.start(Thread.currentThread(), TimeUnit.SECONDS.toMillis(10));

        try {
            monitor.interrupt();
            monitor.join(TimeUnit.SECONDS.toMillis(1));

            assertFalse(monitor.isAlive());
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            ThreadMonitor.stop(monitor);
            Thread.interrupted();
        }
    }

    @Test
    public void testTimeoutInterruptsTarget() throws Exception {
        CountDownLatch targetStarted = new CountDownLatch(1);
        AtomicBoolean targetInterrupted = new AtomicBoolean();
        Thread target = new Thread(() -> {
            targetStarted.countDown();

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                targetInterrupted.set(true);
            }
        });
        target.start();
        Thread monitor = null;

        try {
            assertTrue(targetStarted.await(1, TimeUnit.SECONDS));
            monitor = ThreadMonitor.start(target, 20);
            monitor.join(TimeUnit.SECONDS.toMillis(2));
            target.join(TimeUnit.SECONDS.toMillis(2));

            assertFalse(monitor.isAlive());
            assertFalse(target.isAlive());
            assertTrue(targetInterrupted.get());
        } finally {
            ThreadMonitor.stop(monitor);
            target.interrupt();
            target.join(TimeUnit.SECONDS.toMillis(1));
        }
    }

    @Test
    public void testUncancelledMonitorInterruptsTargetAfterTheMonitoredWorkFinished() throws Exception {
        // Pins what start(Thread, long) documents: the monitor's only guard is `cancelled`, never the
        // target's liveness or what the target is currently doing, so a forgotten stop(monitor) lands the
        // interrupt on whatever the target moved on to.
        CountDownLatch monitoredWorkDone = new CountDownLatch(1);
        CountDownLatch unrelatedWorkFinished = new CountDownLatch(1);
        AtomicBoolean interruptedDuringUnrelatedWork = new AtomicBoolean();
        Thread target = new Thread(() -> {
            ThreadMonitor.start(Thread.currentThread(), 100);
            // The monitored operation succeeds immediately; ThreadMonitor.stop(monitor) is deliberately omitted.
            monitoredWorkDone.countDown();

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10)); // unrelated work, started after the monitored one
            } catch (InterruptedException e) {
                interruptedDuringUnrelatedWork.set(true);
            }

            unrelatedWorkFinished.countDown();
        });
        target.setDaemon(true);
        target.start();

        try {
            assertTrue(monitoredWorkDone.await(5, TimeUnit.SECONDS));
            assertTrue(unrelatedWorkFinished.await(5, TimeUnit.SECONDS));
            assertTrue(interruptedDuringUnrelatedWork.get());
        } finally {
            target.interrupt();
            target.join(TimeUnit.SECONDS.toMillis(1));
        }

        assertFalse(target.isAlive());
    }
}
