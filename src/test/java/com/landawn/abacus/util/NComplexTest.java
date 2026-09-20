package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

import jakarta.xml.bind.JAXBException;
import testfixtures.entity.PersonType;
import testfixtures.entity.extendDirty.basic.Account;
import testfixtures.entity.extendDirty.basic.AccountContact;
import testfixtures.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import testfixtures.types.WeekDay;

public class NComplexTest extends NTestSupport {
    @Test
    public void testDeepToStringGuardsArrayCyclesAndDelegatesOtherObjects() {
        Object[] cycle = new Object[1];
        cycle[0] = cycle;
        assertEquals("[[...]]", N.deepToString(cycle));

        RuntimeException failure = new RuntimeException("non-array rendering");
        Object element = new Object() {
            @Override
            public String toString() {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(RuntimeException.class, () -> N.deepToString(new Object[] { element })));
    }

    @Test
    public void testHashCodeEverythingConsumesDirectAndNestedIterators() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        Iterator<Integer> direct = values.iterator();
        assertEquals(N.hashCodeEverything(values), N.hashCodeEverything(direct));
        assertFalse(direct.hasNext());

        Iterator<Integer> nested = values.iterator();
        assertEquals(N.hashCodeEverything(new Object[] { values }), N.hashCodeEverything(new Object[] { nested }));
        assertFalse(nested.hasNext());
    }

    @Test
    public void testMultimapEmptinessCountsRetainedKeys() {
        ListMultimap<String, Integer> multimap = ListMultimap.of("key", 1);
        multimap.get("key").clear();

        assertEquals(0, multimap.totalValueCount());
        assertFalse(N.isEmpty(multimap));
        assertSame(multimap, N.checkArgNotEmpty(multimap, "multimap"));

        multimap.removeAll("key");
        assertTrue(N.isEmpty(multimap));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty(multimap, "multimap"));
        assertTrue(N.isEmpty((Multimap<?, ?, ?>) null));
        assertThrows(IllegalArgumentException.class, () -> N.checkArgNotEmpty((Multimap<?, ?, ?>) null, "multimap"));
    }

    @Test
    public void testComplexFlatMapping() {
        List<String> departments = Arrays.asList("Engineering", "Sales");
        Map<String, List<String>> employees = new HashMap<>();
        employees.put("Engineering", Arrays.asList("Alice", "Bob"));
        employees.put("Sales", Arrays.asList("Charlie", "David", "Eve"));

        List<String> result = new ArrayList<>();
        N.forEach(departments, dept -> employees.get(dept), emp -> Arrays.asList(emp.toLowerCase(), emp.toUpperCase()),
                (dept, emp, formatted) -> result.add(dept + ":" + emp + ":" + formatted));

        assertEquals(10, result.size());
        assertTrue(result.contains("Engineering:Alice:alice"));
        assertTrue(result.contains("Sales:Eve:EVE"));
    }

    @Test
    public void testConditionalBatchProcessing() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger evenBatches = new AtomicInteger(0);
        AtomicInteger oddBatches = new AtomicInteger(0);

        N.forEach(numbers, n -> {
            N.ifOrEmpty(n % 2 == 0, () -> "even").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> evenBatches.incrementAndGet());
            });

            N.ifOrEmpty(n % 2 != 0, () -> "odd").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> oddBatches.incrementAndGet());
            });
        });

        assertEquals(5, evenBatches.get());
        assertEquals(5, oddBatches.get());
    }

    @Test
    public void test_cartesianProduct() throws Exception {
        assertDoesNotThrow(() -> {
            Sets.cartesianProduct(CommonUtil.toList(CommonUtil.toSet("a", "b"), CommonUtil.toSet("a", "b", "c"))).forEach(Fn.println());

            N.println(Strings.repeat('=', 80));

            Iterables.cartesianProduct(CommonUtil.toList(CommonUtil.toSet("a", "b"), CommonUtil.toSet("a", "b", "c"))).forEach(Fn.println());
        });
    }

    @Test
    public void parallelForEach_waitsForAcceptedWorkerWhenLaterSubmissionIsRejected() {
        for (final boolean indexed : Arrays.asList(false, true)) {
            final CountDownLatch actionStarted = new CountDownLatch(1);
            final AtomicBoolean actionFinished = new AtomicBoolean();
            final AtomicInteger submissionCount = new AtomicInteger();
            final Executor partiallyRejectingExecutor = command -> {
                if (submissionCount.getAndIncrement() == 0) {
                    final Thread worker = new Thread(command, "NTest-partially-accepted-worker");
                    worker.setDaemon(true);
                    worker.start();

                    try {
                        assertTrue(actionStarted.await(5, TimeUnit.SECONDS));
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                } else {
                    throw new RejectedExecutionException("reject later worker");
                }
            };

            assertThrows(RejectedExecutionException.class, () -> {
                if (indexed) {
                    N.forEachIndexedInParallel(Arrays.asList(1, 2).iterator(), (idx, value) -> {
                        actionStarted.countDown();
                        N.sleepUninterruptibly(25);
                        actionFinished.set(true);
                    }, 2, partiallyRejectingExecutor);
                } else {
                    N.forEachInParallel(Arrays.asList(1, 2).iterator(), value -> {
                        actionStarted.countDown();
                        N.sleepUninterruptibly(25);
                        actionFinished.set(true);
                    }, 2, partiallyRejectingExecutor);
                }
            });

            assertTrue(actionFinished.get(), "The accepted worker must finish before the submission failure is rethrown");
        }
    }

    @Test
    public void parallelForEach_coordinatorInterruptionStopsAcceptedWorkers() throws Exception {
        assertCoordinatorInterruptionStopsAcceptedWorkers(false);
    }

    @Test
    public void parallelForEachIndexed_coordinatorInterruptionStopsAcceptedWorkers() throws Exception {
        assertCoordinatorInterruptionStopsAcceptedWorkers(true);
    }

    @Test
    public void parallelForEach_coordinatorInterruptionKeepsPriorWorkerFailureReachable() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(3);
        final CountDownLatch blockedWorkerStarted = new CountDownLatch(1);
        final CountDownLatch workersStarted = new CountDownLatch(3);
        final CountDownLatch releaseBlockedWorker = new CountDownLatch(1);
        final AtomicReference<Throwable> coordinatorFailure = new AtomicReference<>();
        final RuntimeException firstWorkerFailure = new RuntimeException("first prior worker failure");
        final RuntimeException secondWorkerFailure = new RuntimeException("second prior worker failure");
        final Thread coordinator = new Thread(() -> {
            try {
                N.forEachInParallel(Arrays.asList(1, 2, 3).iterator(), value -> {
                    workersStarted.countDown();
                    assertTrue(workersStarted.await(5, TimeUnit.SECONDS));

                    if (value <= 2) {
                        throw value == 1 ? firstWorkerFailure : secondWorkerFailure;
                    }

                    blockedWorkerStarted.countDown();
                    releaseBlockedWorker.await();
                }, 3, executor);
            } catch (final Throwable e) {
                coordinatorFailure.set(e);
            }
        }, "NTest-prior-failure-interrupted-coordinator");

        try {
            coordinator.start();
            assertTrue(workersStarted.await(5, TimeUnit.SECONDS));
            assertTrue(blockedWorkerStarted.await(5, TimeUnit.SECONDS));
            final Throwable priorWorkerFailure = awaitAggregatedWorkerFailure(firstWorkerFailure, secondWorkerFailure, 5, TimeUnit.SECONDS);
            assertNotNull(priorWorkerFailure, "Both worker failures must be recorded before interruption");

            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));

            assertFalse(coordinator.isAlive());
            final Throwable interruption = findCause(coordinatorFailure.get(), InterruptedException.class);
            assertNotNull(interruption);
            assertTrue(Arrays.asList(interruption.getSuppressed()).contains(priorWorkerFailure),
                    "The interruption returned to the caller must retain the earlier worker failure");
        } finally {
            releaseBlockedWorker.countDown();
            coordinator.interrupt();
            coordinator.join(TimeUnit.SECONDS.toMillis(5));
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void parallelForEach_waitingWorkerDoesNotPullAfterCoordinatorInterruption() throws Exception {
        assertWaitingWorkerDoesNotPullAfterCoordinatorInterruption(false);
    }

    @Test
    public void parallelForEachIndexed_waitingWorkerDoesNotPullAfterCoordinatorInterruption() throws Exception {
        assertWaitingWorkerDoesNotPullAfterCoordinatorInterruption(true);
    }

    @Test
    public void parallelForEach_sameExceptionInstanceDoesNotEscapeWorkerAggregation() throws Exception {
        assertSameExceptionInstanceDoesNotEscapeWorkerAggregation(false);
    }

    @Test
    public void parallelForEachIndexed_sameExceptionInstanceDoesNotEscapeWorkerAggregation() throws Exception {
        assertSameExceptionInstanceDoesNotEscapeWorkerAggregation(true);
    }

    @Test
    public void testHighConcurrencyForEachInParallel() throws Exception {
        int threadCount = 50;
        int itemsPerThread = 100;
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < itemsPerThread; i++) {
                        items.add(threadId * itemsPerThread + i);
                    }
                    N.forEachInParallel(items, results::add, 5);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(threadCount * itemsPerThread, results.size());
        Set<Integer> uniqueResults = new HashSet<>(results);
        assertEquals(threadCount * itemsPerThread, uniqueResults.size());
    }

    @Test
    public void testPairAndTripleIncrementMustBePositive() {
        final Integer[] array = { 1, 2, 3 };
        final List<Integer> list = Arrays.asList(array);

        assertThrows(IllegalArgumentException.class, () -> N.forEachPair(array, 0, (a, b) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> N.forEachPair(list, -1, (a, b) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> N.forEachPair(list.iterator(), 0, (a, b) -> {
        }));

        assertThrows(IllegalArgumentException.class, () -> N.forEachTriple(array, 0, (a, b, c) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> N.forEachTriple(list, -1, (a, b, c) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> N.forEachTriple(list.iterator(), 0, (a, b, c) -> {
        }));
    }

    @Test
    public void testMixedSynchronousAsynchronousExecution() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> syncResults = new ArrayList<>();

        ContinuableFuture<Void> async1 = N.asyncExecute(() -> {
            Thread.sleep(50);
            counter.addAndGet(10);
        });

        N.forEach(Arrays.asList(1, 2, 3), syncResults::add);

        ContinuableFuture<Integer> async2 = N.asyncExecute(() -> {
            Thread.sleep(25);
            return counter.addAndGet(20);
        });

        N.forEach(Arrays.asList(4, 5, 6), syncResults::add);

        async1.get();
        int async2Result = async2.get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), syncResults);
        assertEquals(30, counter.get());
        assertEquals(20, async2Result);
    }

    @Test
    public void testAsynRun() {
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> {
        }, () -> {
        }, () -> {
        });
        ObjIterator<Void> iter = N.runAsync(commands);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testAsynRunWithExceptionInOneTask() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(counter::incrementAndGet, () -> {
            throw new RuntimeException("Test exception");
        }, counter::incrementAndGet);

        ObjIterator<Void> iter = N.runAsync(commands);

        // Every probe of this iterator must sit inside assertThrows. runAsync yields results in
        // COMPLETION order, so the throwing command can be the very first one out, and the failure
        // surfaces from hasNext() (not just next()) -- see the throw in N.runAsync's iterator. An
        // assertTrue(iter.hasNext()) or an iter.next() before this block is a race that fails ~0.4%
        // of the time. counter is not asserted either: the javadoc says the other commands keep
        // running after the failure is raised, so its value here is not deterministic.
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            while (iter.hasNext()) {
                iter.next();
            }
        });

        assertEquals("Test exception", thrown.getMessage());
    }

    @Test
    public void testAsynCall() {
        List<Callable<Integer>> commands = Arrays.asList(() -> 1, () -> 2, () -> 3);
        ObjIterator<Integer> iter = N.callAsync(commands);
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }
        assertEquals(3, results.size());
        assertTrue(results.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testExecuteWithNoRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        N.runWithRetry(() -> attempts.incrementAndGet(), 0, 0, e -> false);
        assertEquals(1, attempts.get());
    }

    @Test
    public void execute_runnable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Throwables.Runnable<IOException> flakyRunnable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 3) {
                throw new IOException("Temporary failure");
            }
        };

        N.runWithRetry(flakyRunnable, 3, 10, e -> e instanceof IOException);
        assertEquals(3, attempts.get());

        attempts.set(0);
        Throwables.Runnable<IOException> failingRunnable = () -> {
            attempts.incrementAndGet();
            throw new IOException("Persistent failure");
        };
        assertThrows(RuntimeException.class, () -> N.runWithRetry(failingRunnable, 2, 10, e -> e instanceof IOException));
        assertEquals(3, attempts.get());
    }

    @Test
    public void testExecute() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runWithRetry(counter::incrementAndGet, 2, 10, e -> e instanceof RuntimeException);
        assertEquals(1, counter.get());
    }

    @Test
    public void testExecuteWithConditionalRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        assertThrows(RuntimeException.class, () -> N.runWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            RuntimeException e = new RuntimeException("Attempt " + attempt);
            exceptions.add(e);
            throw e;
        }, 3, 10, e -> e.getMessage().contains("Attempt 1") || e.getMessage().contains("Attempt 2")));

        assertEquals(3, attempts.get());
        assertEquals(3, exceptions.size());
    }

    @Test
    public void testExecuteCallableWithBiPredicate() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = N.callWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                return "wrong";
            } else {
                return "correct";
            }
        }, 3, 10, (r, e) -> "wrong".equals(r));

        assertEquals("correct", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testExecuteCallable() {
        Callable<String> callable = () -> "success";
        String result = N.callWithRetry(callable, 2, 10, (r, e) -> e != null);
        assertEquals("success", result);
    }

    @Test
    public void execute_callable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> flakyCallable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 2) {
                throw new ExecutionException("Temp fail", new IOException());
            }
            return "Success";
        };

        String result = N.callWithRetry(flakyCallable, 3, 10, (res, e) -> e != null && e.getCause() instanceof IOException);
        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void testExecuteWithRetryRecovery() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = N.callWithRetry(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Fail " + attempt);
            }
            return "Success on attempt " + attempt;
        }, 5, 10, (r, e) -> e != null);

        assertEquals("Success on attempt 3", result);
        assertEquals(3, attempts.get());
    }

    @Test
    public void testIteratorBatchesAreIndependentSnapshots() {
        List<Integer> values = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<List<Integer>> observed = new ArrayList<>();

        N.runByBatch(values.iterator(), 2, observed::add);
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)), observed);

        List<List<Integer>> returned = N.callByBatch(values.iterator(), 2, batch -> batch);
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)), returned);
    }

    @Test
    public void testIteratorBatchingDoesNotPreallocateTheRequestedBatchSize() {
        final List<List<Integer>> observed = new ArrayList<>();
        N.runByBatch(Arrays.asList(1, 2, 3).iterator(), Integer.MAX_VALUE, observed::add);

        assertEquals(Collections.singletonList(Arrays.asList(1, 2, 3)), observed);
        assertThrows(UnsupportedOperationException.class, () -> observed.get(0).add(4));
        assertEquals(Collections.singletonList(3), N.callByBatch(Arrays.asList(1, 2, 3).iterator(), Integer.MAX_VALUE, List::size));
    }

    @Test
    public void testListBatchingAvoidsIndexOverflow() {
        final List<Integer> virtualList = new java.util.AbstractList<>() {
            @Override
            public Integer get(final int index) {
                if (index < 0 || index >= size()) {
                    throw new IndexOutOfBoundsException();
                }

                return index;
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };

        final List<Integer> observedSizes = new ArrayList<>();
        N.runByBatch(virtualList, 1_500_000_000, batch -> observedSizes.add(batch.size()));
        assertEquals(Arrays.asList(1_500_000_000, 647_483_647), observedSizes);

        assertEquals(Arrays.asList(1_500_000_000, 647_483_647), N.callByBatch(virtualList, 1_500_000_000, List::size));
    }

    @Test
    public void testBatchProcessingWithStateManagement() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Map<Integer, List<String>> batchMap = new HashMap<>();
        AtomicInteger batchNumber = new AtomicInteger(0);

        N.runByBatch(items, 2, (idx, item) -> {
            int currentBatch = batchNumber.get();
            batchMap.computeIfAbsent(currentBatch, k -> new ArrayList<>()).add(item);
        }, () -> batchNumber.incrementAndGet());

        assertEquals(3, batchMap.size());
        assertEquals(Arrays.asList("a", "b"), batchMap.get(0));
        assertEquals(Arrays.asList("c", "d"), batchMap.get(1));
        assertEquals(Arrays.asList("e", "f"), batchMap.get(2));
    }

    @Test
    public void testBatchProcessingWithEmptyBatches() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger batchCount = new AtomicInteger(0);

        N.runByBatch(items.iterator(), 10, batch -> {
            assertFalse(batch.isEmpty());
            batchCount.incrementAndGet();
        });

        assertEquals(1, batchCount.get());
    }

    @Test
    public void testBatchMethodsValidateBatchSizeOnEmptyInput() {
        // Per contract, all run/callByBatch overloads should validate batchSize, even when input is empty/null.
        // Previously, several Iterable/array variants returned silently for empty input, swallowing IAE.

        final Integer[] emptyArray = new Integer[0];
        final List<Integer> emptyList = new ArrayList<>();
        final Throwables.Consumer<List<Integer>, RuntimeException> batchConsumer = batch -> {
        };
        final Throwables.IntObjConsumer<Integer, RuntimeException> elemConsumer = (i, v) -> {
        };
        final Throwables.Runnable<RuntimeException> noopRunnable = () -> {
        };
        final Throwables.Function<List<Integer>, Integer, RuntimeException> batchFn = batch -> 0;
        final Throwables.Callable<Integer, RuntimeException> noopCallable = () -> 0;

        // runByBatch(T[], int, Consumer)
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyArray, 0, batchConsumer));
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyArray, -3, batchConsumer));

        // runByBatch(T[], int, IntObjConsumer, Runnable)
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyArray, 0, elemConsumer, noopRunnable));
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyArray, -1, elemConsumer, noopRunnable));

        // runByBatch(Iterable, int, IntObjConsumer, Runnable)
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyList, 0, elemConsumer, noopRunnable));
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(emptyList, -1, elemConsumer, noopRunnable));

        // callByBatch(T[], int, Function)
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyArray, 0, batchFn));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyArray, -1, batchFn));

        // callByBatch(T[], int, IntObjConsumer, Callable)
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyArray, 0, elemConsumer, noopCallable));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyArray, -1, elemConsumer, noopCallable));

        // callByBatch(Iterable, int, IntObjConsumer, Callable)
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyList, 0, elemConsumer, noopCallable));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(emptyList, -1, elemConsumer, noopCallable));
    }

    @Test
    public void testMemoryEfficientBatchProcessing() {
        int totalItems = 1000;
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger maxBatchesInMemory = new AtomicInteger(0);
        List<WeakReference<List<Integer>>> batchRefs = new ArrayList<>();

        Iterator<Integer> iter = new Iterator<>() {
            int current = 0;

            @Override
            public boolean hasNext() {
                return current < totalItems;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current++;
            }
        };

        N.runByBatch(iter, 50, batch -> {
            processedCount.addAndGet(batch.size());
            batchRefs.add(new WeakReference<>(batch));

            if (batchRefs.size() % 5 == 0) {
                System.gc();

                long activeRefs = batchRefs.stream().filter(ref -> ref.get() != null).count();
                maxBatchesInMemory.set(Math.max(maxBatchesInMemory.get(), (int) activeRefs));
            }
        });

        assertEquals(totalItems, processedCount.get());
        assertTrue(maxBatchesInMemory.get() < 10);
    }

    @Test
    public void testComplexBatchProcessing() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<String> results = N.callByBatch(data, 10, batch -> {
            int sum = batch.stream().mapToInt(Integer::intValue).sum();
            int min = batch.stream().min(Integer::compareTo).orElse(0);
            int max = batch.stream().max(Integer::compareTo).orElse(0);
            return String.format("Batch[%d-%d]: sum=%d", min, max, sum);
        });

        assertEquals(10, results.size());
        assertTrue(results.get(0).contains("Batch[1-10]: sum=55"));
        assertTrue(results.get(9).contains("Batch[91-100]: sum=955"));
    }

    @Test
    public void testCombiningBatchAndParallelProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<ContinuableFuture<Integer>> futures = N.callByBatch(data, 10, batch -> N.asyncExecute(() -> batch.stream().mapToInt(Integer::intValue).sum()));

        int totalSum = 0;
        for (ContinuableFuture<Integer> future : futures) {
            totalSum += future.get();
        }

        assertEquals(5050, totalSum);
    }

    @Test
    public void testComplexParallelBatchProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            data.add(i);
        }

        ConcurrentHashMap<Integer, Integer> results = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = N.callByBatch(data, 5, batch -> {
            return (Callable<Void>) () -> {
                int sum = batch.stream().mapToInt(Integer::intValue).sum();
                results.put(batch.get(0), sum);
                return null;
            };
        });

        N.callInParallel(tasks);

        assertEquals(10, results.size());
        int totalSum = results.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(1225, totalSum);
    }

    @Test
    public void testUninterruptibleNegativeNanosecondBudgetDoesNotBecomePositive() {
        for (final TimeUnit suppliedUnit : Arrays.asList(TimeUnit.NANOSECONDS, TimeUnit.DAYS)) {
            final List<Long> runBudgets = new ArrayList<>();
            final List<Long> callBudgets = new ArrayList<>();
            try {
                N.runUninterruptibly((remaining, unit) -> {
                    assertEquals(TimeUnit.NANOSECONDS, unit);
                    runBudgets.add(remaining);
                    if (runBudgets.size() == 1) {
                        throw new InterruptedException();
                    }
                }, Long.MIN_VALUE, suppliedUnit);
                assertTrue(Thread.interrupted());

                assertEquals("done", N.callUninterruptibly((remaining, unit) -> {
                    assertEquals(TimeUnit.NANOSECONDS, unit);
                    callBudgets.add(remaining);
                    if (callBudgets.size() == 1) {
                        throw new InterruptedException();
                    }
                    return "done";
                }, Long.MIN_VALUE, suppliedUnit));
                assertTrue(Thread.interrupted());
            } finally {
                Thread.interrupted();
            }

            assertEquals(Arrays.asList(Long.MIN_VALUE, Long.MIN_VALUE), runBudgets);
            assertEquals(Arrays.asList(Long.MIN_VALUE, Long.MIN_VALUE), callBudgets);
        }
    }

    @Test
    public void testUninterruptibleMillisTimeoutUsesElapsedTimeNotWallClockDeadline() {
        List<Long> runBudgets = new ArrayList<>();
        try {
            N.runUninterruptibly(remainingMillis -> {
                runBudgets.add(remainingMillis);
                if (runBudgets.size() == 1) {
                    throw new InterruptedException();
                }
            }, Long.MAX_VALUE);
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }

        assertEquals(Long.MAX_VALUE, runBudgets.get(0));
        assertTrue(runBudgets.get(1) > Long.MAX_VALUE - 60_000);

        List<Long> callBudgets = new ArrayList<>();
        try {
            assertEquals("done", N.callUninterruptibly(remainingMillis -> {
                callBudgets.add(remainingMillis);
                if (callBudgets.size() == 1) {
                    throw new InterruptedException();
                }
                return "done";
            }, Long.MAX_VALUE));
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }

        assertEquals(Long.MAX_VALUE, callBudgets.get(0));
        assertTrue(callBudgets.get(1) > Long.MAX_VALUE - 60_000);
    }

    @Test
    public void testUninterruptibleBatchProcessing() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> results = new ArrayList<>();

        N.runUninterruptibly(() -> {
            N.callByBatch(data, 3, batch -> {
                Thread.sleep(10);
                return batch.stream().mapToInt(Integer::intValue).sum();
            }).forEach(results::add);
        });

        assertEquals(4, results.size());
        assertEquals(55, results.stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    public void tryOrEmptyIfExceptionOccurred_callable() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result.isPresent());
        assertEquals("success", result.get());

        result = N.tryOrEmptyIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyIfExceptionOccurred() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result1.isPresent());
        assertEquals("success", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrEmptyWithInitAndFunction() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred("input", s -> s.toUpperCase());
        assertTrue(result1.isPresent());
        assertEquals("INPUT", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrEmptyWithSuccessfulExecution() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "Success");
        assertTrue(result.isPresent());
        assertEquals("Success", result.get());
    }

    @Test
    public void testTryOrEmptyWithCheckedException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IOException("IO Error");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithRuntimeException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IllegalArgumentException("Invalid argument");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithNullResult() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testTryOrEmptyWithFunction() {
        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred("123", Integer::parseInt);
        assertTrue(result1.isPresent());
        assertEquals(123, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred("abc", Integer::parseInt);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrEmptyWithComplexOperation() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);

        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred(map, m -> m.get("key"));
        assertTrue(result1.isPresent());
        assertEquals(42, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred(map, m -> {
            throw new UnsupportedOperationException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testNestedTryOperations() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            String intermediate = N.tryOrDefaultIfExceptionOccurred(() -> {
                if (Math.random() < 0.5) {
                    throw new RuntimeException("Random failure");
                }
                return "Success";
            }, "Fallback");

            return N.tryOrDefaultIfExceptionOccurred(intermediate, s -> s.toUpperCase(), "ERROR");
        });

        assertTrue(result.isPresent());
        assertTrue("SUCCESS".equals(result.get()) || "FALLBACK".equals(result.get()));
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultValue() {
        String result = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertNull(N.tryOrDefaultIfExceptionOccurred(() -> null, "default"));
        assertNull(N.tryOrDefaultIfExceptionOccurred("input", value -> null, "default"));
        assertNull(N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new IOException("failed");
        }, (String) null));
        assertNull(N.tryOrDefaultIfExceptionOccurred("input", value -> {
            throw new IOException("failed");
        }, (String) null));
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, "default");
        assertEquals("default", result);
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultSupplier() {
        Supplier<String> defaultSupplier = () -> "defaultSupplier";
        String result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> "success", defaultSupplier);
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, (java.util.function.Supplier<String>) defaultSupplier);
        assertEquals("defaultSupplier", result);
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred() {
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertEquals("success", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testTryOrDefaultWithSupplier() {
        {
            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", Fn.s(() -> "default"));
            assertEquals("success", result1);

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, Fn.s(() -> "default"));
            assertEquals("default", result2);
        }
        {

            AtomicInteger supplierCalls = new AtomicInteger(0);

            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", Fn.s(() -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            }));
            assertEquals("Success", result1);
            assertEquals(0, supplierCalls.get());

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, Fn.s(() -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            }));
            assertEquals("Supplied Default", result2);
            assertEquals(1, supplierCalls.get());

        }
    }

    @Test
    public void testTryOrDefaultWithInitAndFunction() {
        String result1 = N.tryOrDefaultIfExceptionOccurred("input", s -> s.toUpperCase(), "default");
        assertEquals("INPUT", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testTryOrDefaultWithVariousScenarios() {
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", "Default");
        assertEquals("Success", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "Default");
        assertEquals("Default", result2);

        String result3 = N.tryOrDefaultIfExceptionOccurred(() -> null, "Default");
        assertNull(result3);
    }

    @Test
    public void testTryOrDefaultWithFunctionAndInit() {
        List<String> list = Arrays.asList("a", "b", "c");

        Integer result1 = N.tryOrDefaultIfExceptionOccurred(list, List::size, -1);
        assertEquals(3, result1);

        Integer result2 = N.tryOrDefaultIfExceptionOccurred(list, l -> {
            throw new IndexOutOfBoundsException();
        }, -1);
        assertEquals(-1, result2);
    }

    @Test
    public void testTryOrDefaultWithExceptionInDefault() {
        assertThrows(RuntimeException.class, () -> {
            N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new IOException("Primary failed");
            }, Fn.s(() -> {
                throw new RuntimeException("Default also failed");
            }));
        });
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred_Function_Supplier() {
        java.util.function.Supplier<String> defaultSupplier = () -> "default";
        String result = N.tryOrDefaultIfExceptionOccurred("hello", s -> s.toUpperCase(), defaultSupplier);
        assertEquals("HELLO", result);

        String fallback = N.tryOrDefaultIfExceptionOccurred("hello", (com.landawn.abacus.util.Throwables.Function<String, String, Exception>) s -> {
            throw new RuntimeException("oops");
        }, defaultSupplier);
        assertEquals("default", fallback);
    }

    @Test
    public void sleep_millis() {
        long start = System.nanoTime();
        N.sleep(10);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(duration >= 10, "Sleep duration was less than expected.");
    }

    @Test
    public void testSleep() {
        long start = System.currentTimeMillis();
        N.sleep(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepWithTimeUnit() {
        long start = System.currentTimeMillis();
        N.sleep(50, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testAsynCallWithDifferentExecutionTimes() {
        List<Callable<String>> commands = Arrays.asList(() -> {
            Thread.sleep(100);
            return "slow";
        }, () -> {
            Thread.sleep(10);
            return "fast";
        }, () -> {
            Thread.sleep(50);
            return "medium";
        });

        ObjIterator<String> iter = N.callAsync(commands);
        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(3, results.size());
        assertTrue(results.contains("slow"));
        assertTrue(results.contains("fast"));
        assertTrue(results.contains("medium"));
    }

    @Test
    public void testAsynCallOrderPreservation() {
        int size = 100;
        List<Callable<Integer>> commands = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final int value = i;
            commands.add(() -> {
                Thread.sleep(ThreadLocalRandom.current().nextInt(10));
                return value;
            });
        }

        ObjIterator<Integer> iter = N.callAsync(commands);
        Set<Integer> results = new HashSet<>();

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(size, results.size());
        for (int i = 0; i < size; i++) {
            assertTrue(results.contains(i));
        }
    }

    @Test
    public void testSleepWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleep(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void testSleepWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.sleep(100, null);
        });
    }

    @Test
    public void testSleepInterruption() {
        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 50, TimeUnit.MILLISECONDS);

        try {
            assertThrows(RuntimeException.class, () -> N.sleep(200));
            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void sleepUninterruptibly_millis() {
        long start = System.nanoTime();
        N.sleepUninterruptibly(10);
        long end = System.nanoTime();
        assertTrue((end - start) >= TimeUnit.MILLISECONDS.toNanos(10) - TimeUnit.MILLISECONDS.toNanos(5));
    }

    @Test
    public void testSleepUninterruptibly() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepUninterruptiblyWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void lazyInit_abacusSupplier() {
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Supplier<String> lazySupplier = N.lazyInit(() -> {
            supplierCallCount.incrementAndGet();
            return "lazyValue";
        });

        assertEquals(0, supplierCallCount.get(), "Supplier should not be called before get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should be called once on first get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should not be called again on subsequent gets()");
    }

    @Test
    public void testLazyInit() {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertEquals(0, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testLazyInitializationThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "initialized";
        });

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    results.add(lazy.get());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        assertEquals(1, counter.get());
        assertEquals(threadCount, results.size());
        for (String result : results) {
            assertEquals("initialized", result);
        }
    }

    @Test
    public void testLazyInitWithException() {
        AtomicInteger attempts = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Initialization failed");
        });

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(1, attempts.get());

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(2, attempts.get());
    }

    @Test
    public void test_as() {
        assertDoesNotThrow(() -> {
            List<String> list = CommonUtil.toList("a");
            N.println(list);

            list = CommonUtil.toList("a", "b");
            N.println(list);

            list = CommonUtil.toList(Array.of("a", "b", "c"));
            N.println(list);
        });
    }

    @Test
    public void test_001() {
        assertDoesNotThrow(() -> {
            final Account account = Beans.newRandomBean(Account.class);
            N.println(account);
        });
    }

    @Test
    public void test_pair() {
        assertDoesNotThrow(() -> {
            final Pair<Integer, String> pair = Pair.of(1, "abc");
            N.println(pair);
            final Triple<Integer, Character, String> triple = Triple.of(1, 'c', "abc");
            N.println(triple);
        });
    }

    @Test
    public void test_asImmutableMap() {
        assertDoesNotThrow(() -> {
            final Map<String, String> m = ImmutableMap.of("123", "abc", "234", "ijk");
            N.println(m);

            for (final Map.Entry<String, String> entry : m.entrySet()) {
                N.println(entry.getKey() + ": " + entry.getValue());
            }
        });
    }

    @Test
    public void test_asResultSet() {
        List<?> list = createAccountList(Account.class, 99);
        Dataset rs = CommonUtil.newDataset(list);
        rs.println();

        list = createAccountList(testfixtures.entity.pjo.basic.Account.class, 99);
        rs = CommonUtil.newDataset(list);
        rs.println();

        final List<?> list2 = createAccountPropsList(79);

        rs = CommonUtil.newDataset(list2);
        rs.println();

        list.addAll((List) list2);
        rs = CommonUtil.newDataset(list);
        rs.println();

        assertEquals(178, rs.size());
    }

    @Test
    public void test_asResultSet_2() {
        final List<Account> beanList = createAccountList(Account.class, 13);
        final Dataset rs1 = CommonUtil.newDataset(beanList);
        rs1.println();

        final List<Map<String, Object>> mapLsit = new ArrayList<>(beanList.size());
        for (final Account account : beanList) {
            mapLsit.add(Beans.beanToMap(account));
        }

        Dataset rs2 = CommonUtil.newDataset(rs1.columnNames(), mapLsit);
        rs2.println();
        assertEquals(rs1, rs2);

        rs2 = CommonUtil.newDataset(rs1.toList(Map.class));
        rs2.println();

        final Dataset rs3 = CommonUtil.newDataset(rs1.columnNames(), rs1.toList(Object[].class));
        rs3.println();

        final Dataset rs4 = CommonUtil.newDataset(rs1.columnNames(), rs1.toList(List.class));
        rs4.println();

    }

    @Test
    public void test_lastIndexOf() {
        assertDoesNotThrow(() -> {
            final String str = "aaa";
            N.println(str.lastIndexOf("a"));
            N.println(str.lastIndexOf("a", str.length()));
            N.println(str.lastIndexOf("a", str.length() - 1));
            N.println(str.lastIndexOf("a", str.length() - 2));
        });
    }

    @Test
    public void test_wrap() {
        assertDoesNotThrow(() -> {
            N.println(Array.box(1, 2, 3));
        });
    }

    @Test
    public void test_getEnumMap() {
        assertDoesNotThrow(() -> {
            final List<UnifiedStatus> statusList = CommonUtil.enumListOf(UnifiedStatus.class);
            N.println(statusList);

            final Set<UnifiedStatus> statusSet = CommonUtil.enumSetOf(UnifiedStatus.class);
            N.println(statusSet);

            final Map<UnifiedStatus, String> statusMap = CommonUtil.enumNameMap(UnifiedStatus.class);
            N.println(statusMap);
        });
    }

    @Test
    public void test_getPackage() {
        assertDoesNotThrow(() -> {
            N.println(ClassUtil.getClassName(int.class));
            N.println(ClassUtil.getSimpleClassName(int.class));
            N.println(ClassUtil.getCanonicalClassName(int.class));

            N.println(ClassUtil.getPackage(int.class));
            N.println(ClassUtil.getPackage(Integer.class));

            N.println(ClassUtil.getPackageName(int.class));
            N.println(ClassUtil.getPackageName(Integer.class));
        });
    }

    @Test
    public void test_uuidPerformance() {
        assertDoesNotThrow(() -> {
            final long startTime = System.currentTimeMillis();
            int k = 0;
            for (int i = 0; i < 1000000; i++) {
                Strings.uuid();
                k++;
            }

            N.println(k + " took: " + (System.currentTimeMillis() - startTime));
        });
    }

    @Test
    public void test_propNameMethod() {
        assertDoesNotThrow(() -> {
            final Account account = createAccount(Account.class);

            N.println(Beans.getPropValue(account, "firstName"));
            N.println(Beans.getPropValue(account, "firstname"));
            N.println(Beans.getPropValue(account, "FirstName"));
            N.println(Beans.getPropValue(account, "FIRSTNAME"));

            Beans.setPropValue(account, "lastName", "lastName1");
            N.println(Beans.getPropValue(account, "LASTNAME"));

            Beans.setPropValue(account, "lastname", "lastName2");
            N.println(Beans.getPropValue(account, "lastname"));

            Beans.setPropValue(account, "LastName", "lastName3");
            N.println(Beans.getPropValue(account, "LastName"));

            Beans.setPropValue(account, "LASTNAME", "lastName4");
            N.println(Beans.getPropValue(account, "LASTNAME"));
            N.println(Beans.getPropValue(account, "lastName"));
        });
    }

    @Test
    public void test_arrayOf() {
        assertDoesNotThrow(() -> {
            N.println(Array.of(false, true));
            N.println(Array.of('a', 'b'));
            N.println(Array.of((byte) 1, (byte) 2));
            N.println(Array.of((short) 1, (short) 2));
            N.println(Array.of(1, 2));
            N.println(Array.of(1L, 2L));
            N.println(Array.of(1f, 2f));
            N.println(Array.of(1d, 2d));
            N.println(CommonUtil.asArray(Dates.currentJUDate(), Dates.currentDate()));
            N.println(CommonUtil.asArray(Dates.currentCalendar(), Dates.currentCalendar()));

            final String a1 = "a";
            final String b1 = "b";
            final List<String> list = CommonUtil.toList(a1, b1);
            N.println(list);

            final List<Integer> list2 = CommonUtil.toList(1, 2, 3);
            N.println(list2);

            final int[] a = Array.of(1, 2, 3);
            N.println(a);

            final Class<?>[] classes = CommonUtil.asArray(String.class, Integer.class);
            N.println(classes);

            final Type<?>[] types = CommonUtil.asArray(CommonUtil.typeOf(int.class), CommonUtil.typeOf(long.class));
            N.println(types);

            final Date[] dates = CommonUtil.asArray(Dates.currentDate(), Dates.currentDate());
            N.println(dates);

            final java.util.Date[] dateTimes = CommonUtil.asArray(Dates.currentDate(), Dates.currentTime());
            N.println(dateTimes);

            final UnifiedStatus[] status = CommonUtil.asArray(UnifiedStatus.ACTIVE, UnifiedStatus.CANCELED);
            N.println(status);

            N.println(ClassUtil.getCanonicalClassName(int.class));
        });
    }

    @Test
    public void test_encode_decode_2() {
        final String str = "ůůůůů";
        N.println(Strings.base64UrlEncode(str.getBytes()));

        N.println(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(str.getBytes()));

        final String string = "This string encoded will be longer that 76 characters and cause MIME base64 line folding";

        System.out.println("commons-codec JDK8Base64.encodeBase64\n" + Strings.base64Encode(string.getBytes()));

        assertEquals(string, Strings.base64DecodeToString(Strings.base64Encode(string.getBytes())));
    }

    @Test
    public void test_getPropField() {
        assertDoesNotThrow(() -> {
            Field field = Beans.getPropField(Account.class, "firstName");
            N.println(field);

            field = Beans.getPropField(Account.class, "first_Name");
            N.println(field);

            field = Beans.getPropField(Account.class, "getfirstName");
            N.println(field);
        });
    }

    @Test
    public void test_registerPropertyAccessor() {
        class AccessorProbe {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(final String value) {
                this.value = value;
            }
        }

        Beans.registerPropertyAccessor("a", ClassUtil.getDeclaredMethod(AccessorProbe.class, "getValue"));

        Method method = Beans.getPropGetter(AccessorProbe.class, "a");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(AccessorProbe.class, "getValue"), method);

        Beans.registerPropertyAccessor("b", ClassUtil.getDeclaredMethod(AccessorProbe.class, "setValue", String.class));
        method = Beans.getPropSetter(AccessorProbe.class, "b");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(AccessorProbe.class, "setValue", String.class), method);
    }

    @Test
    public void test_invokeConstructor() {
        assertDoesNotThrow(() -> {
            final Account account = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(Account.class));
            N.println(account);
        });
    }

    @Test
    public void test_getClassName() {
        assertDoesNotThrow(() -> {
            N.println(ClassUtil.getSimpleClassName(long.class));
            N.println(ClassUtil.getClassName(long.class));
            N.println(ClassUtil.getCanonicalClassName(long.class));

            N.println(ClassUtil.getSimpleClassName(long[].class));
            N.println(ClassUtil.getClassName(long[].class));
            N.println(ClassUtil.getCanonicalClassName(long[].class));

            N.println(ClassUtil.getSimpleClassName(Long.class));
            N.println(ClassUtil.getClassName(Long.class));
            N.println(ClassUtil.getCanonicalClassName(Long.class));

            N.println(ClassUtil.getSimpleClassName(Long[].class));
            N.println(ClassUtil.getClassName(Long[].class));
            N.println(ClassUtil.getCanonicalClassName(Long[].class));
        });
    }

    @Test
    public void test_asArray() {
        assertDoesNotThrow(() -> {
            final String[] a = CommonUtil.asArray("a", "b");
            N.println(a);

            final Object[] b = CommonUtil.asArray("a", 'c');
            N.println(b);

            final char[] c = Array.of('a', 'c');
            N.println(c);
        });
    }

    @Test
    public void test_Bean2Map() {
        assertDoesNotThrow(() -> {
            final Account account = createAccountWithContact(Account.class);

            Map<String, Object> m = Beans.beanToFlatMap(account);
            N.println(CommonUtil.stringOf(m));

            final XBean xBean = createBigXBean(1);
            m = Beans.beanToMap(xBean);
            N.println(CommonUtil.stringOf(m));

            m = Beans.beanToFlatMap(xBean);
            N.println(CommonUtil.stringOf(m));
        });
    }

    @Test
    public void test_Bean2Map_2() {
        assertDoesNotThrow(() -> {
            final testfixtures.entity.pjo.basic.Account account = createAccountWithContact(testfixtures.entity.pjo.basic.Account.class);

            Map<String, Object> m = Beans.beanToMap(account);
            N.println(CommonUtil.stringOf(m));

            m = Beans.deepBeanToMap(account);
            N.println(CommonUtil.stringOf(m));

            m = Beans.beanToFlatMap(account);
            N.println(CommonUtil.stringOf(m));
        });
    }

    @Test
    public void testFormat_1() {
        assertDoesNotThrow(() -> {
            N.println(Dates.parseToDate("2014-01-01"));

            String st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT);
            N.println(st + " : " + Dates.format(Dates.parseToDate(st)));

            st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
            N.println(st + " : " + Dates.format(Dates.parseToDate(st), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("PST")));
        });
    }

    @Test
    public void testFormat_2() {
        assertDoesNotThrow(() -> {
            final String st = Dates.format(Dates.currentDate());
            N.println(st + " : " + Dates.format(Dates.parseToDate(st)));

            for (int i = 0; i < 1000000; i++) {
                Dates.parseToDate(st);
            }
        });
    }

    @Test
    public void testFormat_3() {
        assertDoesNotThrow(() -> {
            final String st = Dates.format(Dates.currentDate(), Dates.ISO_8601_TIMESTAMP_FORMAT);
            N.println(st + " : " + Dates.format(Dates.parseToDate(st), Dates.ISO_8601_TIMESTAMP_FORMAT));

            for (int i = 0; i < 1000000; i++) {
                Dates.parseToDate(st);
            }
        });
    }

    @Test
    public void test_string2Array() {
        assertDoesNotThrow(() -> {
            final byte[] array = Splitter.with(",").trim(true).splitToArray("1,2,3, 4", byte[].class);

            N.println(array);
        });
    }

    @Test
    public void testArrayName() {
        N.println(byte[].class.getSimpleName());
        assertTrue("byte[]".equals(byte[].class.getSimpleName()));

        N.println(int[].class.getSimpleName());
        assertTrue("int[]".equals(int[].class.getSimpleName()));
    }

    @Test
    public void testString2Array() {
        assertDoesNotThrow(() -> {
            N.println(Splitter.withDefault().splitToArray("a, b, c", char[].class));
            N.println(Splitter.withDefault().splitToArray("1, 2, 3", byte[].class));
            N.println(Splitter.withDefault().splitToArray("1, 2, 3", int[].class));
        });
    }

    @Test
    public void testStringOf() {
        assertDoesNotThrow(() -> {
            N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray(CommonUtil.stringOf(Dates.currentDate()), Date[].class)));
            N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("a, b, c", char[].class)));
            N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("1, 2, 3", byte[].class)));
            N.println(CommonUtil.stringOf(Splitter.withDefault().splitToArray("1, 2, 3", int[].class)));
        });
    }

    @Test
    public void testValueOf() {
        assertDoesNotThrow(() -> {
            N.println(abacusXmlParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
            N.println(abacusXmlParser.deserialize("<array>a, b, c</array>", char[].class));
            N.println(abacusXmlParser.deserialize("<array>1, 2, 3</array>", byte[].class));
            N.println(abacusXmlParser.deserialize("<array>1, 2, 3</array>", int[].class));
        });
    }

    @Test
    public void testValueOf_1() {
        assertDoesNotThrow(() -> {
            N.println(abacusXMLDOMParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
            N.println(abacusXMLDOMParser.deserialize("<array>a, b, c</array>", char[].class));
            N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", byte[].class));
            N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", int[].class));
        });
    }

    @Test
    public void testPropGetSetMethod() {
        assertDoesNotThrow(() -> {
            final Account account = new Account();
            account.setFirstName("fn");
            account.setMiddleName("mn");
            account.setLastName("ln");

            println(Beans.getPropGetter(Account.class, "firstName"));
            println(Beans.getPropSetter(Account.class, "id"));
        });
    }

    @Test
    public void testCloneBean() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(1000);

        final Account copy = Beans.deepCopy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCombine() {
        final String[] a = { "a", "b", "c" };
        final String[] b = { "d", "e", "f" };
        final String[] c = { "a", "b", "c", "d", "e", "f" };

        println(CommonUtil.toString(N.concat(b, a)));
        assertTrue(CommonUtil.equals(c, N.concat(a, b)));

        final int[] i1 = { 1, 2, 3 };
        final int[] i2 = { 4, 5, 6 };

        N.concat(i2, i1);
    }

    @Test
    public void testBean2Map() {
        assertDoesNotThrow(() -> {
            final Account account = createAccount(Account.class);
            println(Beans.beanToMap(account));
        });
    }

    @Test
    public void testAsDate() {
        final Calendar c = Calendar.getInstance();
        println(Dates.roll(Dates.createDate(c), 10, CalendarField.MONTH));

        println(Dates.roll(Dates.createDate(c), 10, TimeUnit.DAYS));
        println(Dates.roll(Dates.createDate(c), -10, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), -10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR));

        println(Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTime(c), -1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createTime(c), -7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTimestamp(c), -1000, CalendarField.WEEK_OF_YEAR)),
                Dates.roll(Dates.createTimestamp(c), -7000, CalendarField.DAY_OF_MONTH));

        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, TimeUnit.DAYS)));
        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, CalendarField.DAY_OF_MONTH)));
    }

    @Test
    public void testAsDateWithString() {
        assertDoesNotThrow(() -> {
            final Type<Date> type = TypeFactory.getType(Date.class.getCanonicalName());
            final String st = type.stringOf(Dates.currentDate());
            N.println(st);
        });
    }

    @Test
    public void testUUID() {
        assertDoesNotThrow(() -> {
            N.println(Strings.uuidWithoutHyphens());
            N.println(Strings.uuidWithoutHyphens().length());
            N.println(Strings.uuid());
            N.println(Strings.uuid().length());

            println(UUID.randomUUID().toString());

            final String uuid = Strings.uuid();
            println(uuid);
            println(Strings.base64Encode(uuid.getBytes()));
        });
    }

    @Test
    public void testAsString() {
        assertDoesNotThrow(() -> {
            final Account account = createAccount(Account.class);
            final AccountContact contact = createAccountContact(AccountContact.class);
            account.setContact(contact);

            println(CommonUtil.stringOf(account));

            final Bean bean = new Bean();
            println(CommonUtil.stringOf(bean));

            bean.setStrings(CommonUtil.asArray("a", "b"));
            println(CommonUtil.stringOf(bean));

            bean.setBytes(new byte[] { 1, 2 });
            println(CommonUtil.stringOf(bean));
        });
    }

    @Test
    public void testAsXml() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXmlParser.serialize(account));
        println(abacusXmlParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.toSet("lastUpdateTime"));
        final XmlSerConfig config = new XmlSerConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXmlParser.serialize(account, config));

        println(abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXmlParser.deserialize(abacusXmlParser.serialize(account), Account.class)));
    }

    @Test
    public void testAsXml_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXMLDOMParser.serialize(account));
        println(abacusXMLDOMParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.toSet("lastUpdateTime"));
        final XmlSerConfig config = new XmlSerConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXMLDOMParser.serialize(account, config));

        println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class)));
    }

    @Test
    public void testSerialize() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXmlParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(account));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXmlParser.serialize(account), abacusXmlParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXmlParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XmlSerConfig config = new XmlSerConfig();
        config.setTagByPropertyName(false);
        xml = abacusXmlParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.setWriteTypeInfo(true);
        xml = abacusXmlParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXmlParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(account));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.serialize(account), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XmlSerConfig config = new XmlSerConfig();
        config.setTagByPropertyName(false);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.setWriteTypeInfo(true);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize1() {
        assertDoesNotThrow(() -> {
            final Bean bean = new Bean();
            bean.setTypeList(CommonUtil.toList(
                    "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★',
                    '\n', '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                    new String[] {
                            "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                            "\r", "\t", "\"", "\'" }));

            bean.setBytes(new byte[] { 1, 2 });
            bean.setStrings(new String[] { "aa", "bb", "<>>" });
            bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

            final String xml = abacusXmlParser.serialize(bean);
            println(xml);

            final Bean xmlBean = abacusXmlParser.deserialize(xml, Bean.class);
            N.println(abacusXmlParser.serialize(bean));
            N.println(abacusXmlParser.serialize(xmlBean));

            N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean.class));
            N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), Bean.class));
        });
    }

    @Test
    public void testSerialize1_1() {
        final Bean bean = new Bean();
        bean.setTypeList(CommonUtil.toList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final Bean xmlBean = abacusXMLDOMParser.deserialize(xml, Bean.class);
        assertEquals(abacusXMLDOMParser.serialize(bean), abacusXMLDOMParser.serialize(xmlBean));
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize3() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXmlParser.serialize(account);
        println(st);

        println(abacusXmlParser.deserialize(st, Account.class));

        st = abacusXmlParser.serialize(account);
        println(st);

        println(abacusXmlParser.deserialize(st, Account.class));
        assertEquals(account, abacusXmlParser.deserialize(st, Account.class));
    }

    @Test
    public void testSerialize3_1() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));

        st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));
        assertEquals(account, abacusXMLDOMParser.deserialize(st, Account.class));
    }

    @Test
    public void testGenericType() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.toSet(1L, 2L));

        final XmlSerConfig config = new XmlSerConfig();
        config.setWriteTypeInfo(true);

        final String xml = abacusXmlParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXmlParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testGenericType_1() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.toList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.toSet(1L, 2L));

        final XmlSerConfig config = new XmlSerConfig();
        config.setWriteTypeInfo(true);

        final String xml = abacusXMLDOMParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXMLDOMParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testSerialize6() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXmlParser.serialize(account);
        println(xml);

        final testfixtures.entity.extendDirty.basic.Account xmlBean = abacusXmlParser.deserialize(xml, testfixtures.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize6_1() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXMLDOMParser.serialize(account);
        println(xml);

        final testfixtures.entity.extendDirty.basic.Account xmlBean = abacusXMLDOMParser.deserialize(xml, testfixtures.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testPrintlnWithSpecialCharacters() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.println("Line1\nLine2\tTab\r\nLine3");
            String output = baos.toString();
            assertTrue(output.contains("Line1"));
            assertTrue(output.contains("Line2"));
            assertTrue(output.contains("Tab"));
            assertTrue(output.contains("Line3"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void test_misMatch() {

        assertEquals(Double.valueOf(2), CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));
        assertEquals(2D, CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));

        double d = CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3));
        N.println(d);

        final int len = 1;
        int[] a = Array.range(0, len);
        int[] b = Array.range(0, len);
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a[len - 1] = 0;
        b[len - 1] = 1;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        a = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        b = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
        b = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
    }

    @Test
    public void test_PermutationIterator() {
        assertDoesNotThrow(() -> {
            PermutationIterator.of(CommonUtil.toList(1, 2, null, 3)).forEachRemaining(Fn.println());

            Iterables.powerSet(CommonUtil.toSet(1, 2, null, 3)).forEach(Fn.println());
        });
    }

    @Test
    public void test_firstNonEmpty() {
        assertDoesNotThrow(() -> {
            final Optional<List<String>> result = CommonUtil.firstNonEmpty(CommonUtil.toList(), CommonUtil.toList("a"), CommonUtil.toList());
            N.println(result);
        });
    }

    @Test
    public void test_clone() {
        final int[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] b = a.clone();
        CommonUtil.reverse(b[0]);
        assertTrue(CommonUtil.equals(a[0], b[0]));

        b = CommonUtil.clone(a);
        CommonUtil.reverse(b[0]);
        assertFalse(CommonUtil.equals(a[0], b[0]));

        final String[] c = null;
        N.println(CommonUtil.clone(c));

        final String[][] d = null;
        N.println(CommonUtil.clone(d));

        final String[][][] e = null;
        N.println(CommonUtil.clone(e));

    }

    @Test
    public void test_lambda() {
        assertDoesNotThrow(() -> {
            int[] a = Array.repeat(3, 10);
            N.println(a);

            CommonUtil.fill(a, 0);
            N.println(a);

            a = Array.of(1, 2, 3, 4, 5, 6);
            CommonUtil.reverse(a);
            N.println(a);

            a = Array.of(1, 2, 3, 4, 5, 6);
            CommonUtil.rotate(a, 2);
            N.println(a);

            a = Array.of(1, 2, 3, 4, 5, 6);
            CommonUtil.shuffle(a);
            N.println(a);

            a = Array.of(1, 2, 3, 4, 5, 6);
            N.println(N.sum(a));
            N.println(N.average(a));
            N.println(N.min(a));
            N.println(N.max(a));
            N.println(N.lowerMedian(a));
        });
    }

    @Test
    public void test_stringOf() {

        assertEquals(Strings.EMPTY, "abc".substring(1, 1));

        {
            final Multiset<String> multiSet = CommonUtil.toMultiset("1", "2", "3", "2", "3", "3");
            N.println(multiSet);

            final String str = CommonUtil.stringOf(multiSet);

            final Multiset<String> multiSet2 = CommonUtil.valueOf(str, Multiset.class);

            N.println(multiSet2);

            final Multiset<Integer> multiSet3 = (Multiset<Integer>) CommonUtil.typeOf("Multiset<Integer>").valueOf(str);
            N.println(multiSet3);

            final Multiset<Object> multiSet4 = (Multiset<Object>) CommonUtil.typeOf("Multiset<Object>").valueOf(str);
            N.println(multiSet4);
        }

    }

    @Test
    public void test_checkNullOrEmpty() {
        assertDoesNotThrow(() -> {
            List<String> list = CommonUtil.toList("a");
            list = CommonUtil.checkArgNotEmpty(list, "list");
            N.println(list);

            Set<String> set = CommonUtil.toSet("a");
            set = CommonUtil.checkArgNotEmpty(set, "set");
            N.println(set);

            Queue<String> queue = CommonUtil.toQueue("a");
            queue = CommonUtil.checkArgNotEmpty(queue, "queue");
            N.println(queue);
        });
    }

    @Test
    public void test_Collections() {
        assertDoesNotThrow(() -> {
            N.println(CommonUtil.asSingletonSet("abc"));
            N.println(CommonUtil.asSingletonList("abc"));
            N.println(CommonUtil.asSingletonMap("key", "value"));

            final List<String> list = CommonUtil.toList("a", "b", "c", "d");
            N.println(list);
            CommonUtil.reverse(list);
            N.println(list);
            N.replaceAll(list, "a", "newValue");
            N.println(list);
        });
    }

    @Test
    public void test_format() {
        assertDoesNotThrow(() -> {
            N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_TIME_FORMAT));
            N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT));
            N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_TIME_FORMAT));
            N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_TIME_FORMAT));

            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentDate())));
            com.landawn.abacus.util.BufferedWriter writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentDate())));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")), Dates.LOCAL_DATE_FORMAT,
                    TimeZone.getTimeZone("UTC")));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")), Dates.LOCAL_DATE_FORMAT,
                    TimeZone.getTimeZone("UTC")));
            Dates.formatTo(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), writer);
            N.println(Dates.parseToTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            Objectory.recycle(writer);

            writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentCalendar())));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")),
                    Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")),
                    Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));

            Dates.formatTo(Dates.currentCalendar(), null, null, writer);
            N.println(Dates.parseToTimestamp(writer.toString()));
            Objectory.recycle(writer);

            writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
            Dates.formatTo(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), writer);
            N.println(Dates.parseToTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            Objectory.recycle(writer);

            writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentXMLGregorianCalendar())));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")),
                    Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            N.println(Dates.parseToTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")),
                    Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            Dates.formatTo(Dates.currentXMLGregorianCalendar(), null, null, writer);
            N.println(Dates.parseToTimestamp(writer.toString()));
            Objectory.recycle(writer);

            writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
            Dates.formatTo(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), writer);
            N.println(Dates.parseToTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            Objectory.recycle(writer);
        });
    }

    @Test
    public void test_hashCode() {
        N.println(CommonUtil.hashCode(false));
        N.println(CommonUtil.hashCode(true));
        N.println(CommonUtil.hashCode('a'));
        N.println(CommonUtil.hashCode((byte) 1));
        N.println(CommonUtil.hashCode((short) 1));
        N.println(CommonUtil.hashCode(1));
        N.println(CommonUtil.hashCode(1L));
        N.println(CommonUtil.hashCode(1f));
        N.println(CommonUtil.hashCode(1d));

        N.println(CommonUtil.hashCode(new boolean[] { true, false }));

        N.println(CommonUtil.hashCode(new char[] { 'a', 'b' }));

        N.println(CommonUtil.hashCode(new byte[] { (byte) 1, (byte) 1 }));

        N.println(CommonUtil.hashCode(new short[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new int[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new long[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new float[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new double[] { 1, 1 }));

        final String[][] a = { { "a", "b", "c" }, { "1", "2", "3" } };
        N.println(CommonUtil.hashCode(a));
        N.println(CommonUtil.deepHashCode(a));

        final Object b = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };

        N.println(CommonUtil.hashCode(b));
        N.println(CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode(a), CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode("abc"), CommonUtil.hashCode("abc"));
        assertEquals(0, CommonUtil.hashCode((Object) null));
        assertEquals(0, CommonUtil.deepHashCode((Object) null));
    }

    @Test
    public void test_erase() {
        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");

        N.println(account);
        Beans.clearAllProps(account);
        N.println(account);

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.clearProps(account, "firstName");
        N.println(account);
        assertNull(account.getFirstName());

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.clearProps(account, CommonUtil.toList("firstName", "lastName"));
        assertNull(account.getFirstName());
        assertNull(account.getLastName());

        final PersonType personType = new PersonType();
        personType.setBirthday(Dates.currentDate());
        Beans.clearAllProps(personType);
        assertNull(personType.getBirthday());
    }

    @Test
    public void testInvokeMethod() {
        final Account account = createAccount(Account.class);
        assertEquals(account.getFirstName(), ClassUtil.invokeMethod(account, ClassUtil.getDeclaredMethod(Account.class, "getFirstName")));
    }

    @Test
    public void testPropGetSetValue() {
        final Account account = new Account();

        Method getMethod = Beans.getPropGetter(Account.class, "firstName");
        Method setMethod = Beans.getPropSetter(Account.class, "firstName");
        Beans.setPropValue(account, setMethod, "fn");
        assertEquals("fn", Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetter(Account.class, AccountPNL.ID);
        setMethod = Beans.getPropSetter(Account.class, AccountPNL.ID);
        Beans.setPropValue(account, setMethod, -1);
        Beans.setPropValue(account, setMethod, Integer.valueOf(-2));
        assertEquals(Long.valueOf(-2), Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetter(Account.class, AccountPNL.BIRTH_DATE);
        setMethod = Beans.getPropSetter(Account.class, AccountPNL.BIRTH_DATE);

        final Date date = Dates.currentDate();
        Beans.setPropValue(account, setMethod, date);
        println(account);

        Beans.setPropValue(account, "firstName", "newfn");
        assertEquals("newfn", Beans.getPropValue(account, "firstName"));
        println(account);

        Beans.setPropValue(account, "id", null);

    }

    @Test
    public void testPropGetSetValue_2() {
        final Account account = new Account();
        String firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        Beans.setPropValue(account, "firstName", firstName);

        assertEquals(firstName, Beans.getPropValue(account, "firstName"));
        firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = Beans.getPropValue(account, "contact.id");
        N.println(contactId);

        String email = Beans.getPropValue(account, "contact.email");
        N.println(email);

        assertNull(Beans.getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = Beans.getPropValue(account, "contact.email");
        N.println(email);
    }

    @Test
    public void testPropGetSetValue_by_ParserUtil() {
        final Account account = new Account();
        String firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        ParserUtil.getBeanInfo(Account.class).setPropValue(account, "firstName", firstName);

        assertEquals(firstName, ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName"));
        firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.id");
        N.println(contactId);

        String email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);

        assertNull(ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);
    }

    private static XmlDeserConfig xBeanXmlDeserConfig() {
        return XmlDeserConfig.create().setValueType("account", Account.class).setValueType("accountContact", AccountContact.class);
    }

    @Test
    public void testSerialize2() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        final List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        final String xml = abacusXmlParser.serialize(xBean, XmlSerConfig.create().setWriteTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XmlDeserConfig xdc = xBeanXmlDeserConfig();
        final XBean xmlBean = abacusXmlParser.deserialize(xml, xdc, XBean.class);
        N.println(xmlBean.getTypeList().get(1).getClass());
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(xBean));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean), xdc, XBean.class),
                abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), xdc, XBean.class));

        assertEquals(abacusXmlParser.serialize(xBean), abacusXmlParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXmlParser.serialize(xBean));
        N.println(abacusXmlParser.serialize(xmlBean));

        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xBean), xdc, XBean.class));
        N.println(abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), xdc, XBean.class));
    }

    @Test
    public void testSerialize2_1() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        final List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        final String xml = abacusXMLDOMParser.serialize(xBean, XmlSerConfig.create().setWriteTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XmlDeserConfig xdc = xBeanXmlDeserConfig();
        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, xdc, XBean.class);
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), xdc, XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), xdc, XBean.class));

        assertEquals(abacusXMLDOMParser.serialize(xBean), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), xdc, XBean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), xdc, XBean.class));
    }

    @Test
    public void testSerialize4() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXmlParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXmlParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize4_1() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXMLDOMParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testXBean() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXmlParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXmlParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXmlParser.serialize(bean));
        N.println(abacusXmlParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), XBean.class),
                abacusXmlParser.deserialize(abacusXmlParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void testXBean_1() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void println_object() {
        assertDoesNotThrow(() -> {
            N.println("Test string");
            N.println(123);
            N.println(createSampleBean());
            N.println(Arrays.asList("a", "b"));
            N.println(new String[] { "c", "d" });
            Map<String, String> map = new HashMap<>();
            map.put("key", "value");
            N.println(map);
            N.println((Object) null);
        });
    }

    @Test
    public void testPrintln() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            String result = N.println("test");
            assertEquals("test", result);
            assertTrue(baos.toString().contains("test"));

            baos.reset();
            List<String> list = Arrays.asList("a", "b", "c");
            List<String> listResult = N.println(list);
            assertSame(list, listResult);
            assertTrue(baos.toString().contains("[a, b, c]"));

            baos.reset();
            String[] array = { "x", "y", "z" };
            String[] arrayResult = N.println(array);
            assertSame(array, arrayResult);
            assertTrue(baos.toString().contains("[x, y, z]"));

            baos.reset();
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            Map<String, Integer> mapResult = N.println(map);
            assertSame(map, mapResult);
            String mapOutput = baos.toString();
            assertTrue(mapOutput.contains("{") && mapOutput.contains("}"));
            assertTrue(mapOutput.contains("a=1") || mapOutput.contains("b=2"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithNullValue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Object result = N.println(null);
            assertNull(result);
            assertTrue(baos.toString().contains("null"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithPrimitiveArrays() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            int[] intArray = { 1, 2, 3 };
            int[] result = N.println(intArray);
            assertSame(intArray, result);

            baos.reset();
            Object[] objArray = { 1, "two", 3.0 };
            Object[] objResult = N.println(objArray);
            assertSame(objArray, objResult);
            assertTrue(baos.toString().contains("[1, two, 3.0]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithNestedCollections() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
            List<List<String>> result = N.println(nested);
            assertSame(nested, result);
            assertTrue(baos.toString().contains("[[a, b], [c, d]]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void test_stream_persist_json() throws IOException {
        assertDoesNotThrow(() -> {
            final List<Map<String, Object>> list = new ArrayList<>();
            list.add(CommonUtil.asMap("a", 1));

            final File file = new File("./a.json");

            Stream.of(list).persistToJson(new File("./a.json"));

            final String json = IOUtil.readAllToString(file);

            N.println(json);

            file.delete();
        });
    }

    @Test
    public void test_range() throws Exception {
        assertDoesNotThrow(() -> {
            N.println(Array.range(0, 10));
            N.println(Array.rangeClosed(0, 10));
            N.println(Array.range(0, 10, -1));
            N.println(Array.range(10, 0, -1));
            N.println(Array.rangeClosed(10, 0, -1));
            N.println(Array.range(10, 0, 1));
        });
    }

    @Test
    public void test_subSet() throws Exception {
        final Set<?> a = CommonUtil.toSet("a", "c");
        final Set<?> b = CommonUtil.toSet("b", "a", "c");

        N.println(N.difference(a, b));
        N.println(SetUtils.difference(a, b));

        N.println(N.difference(b, a));
        N.println(SetUtils.difference(b, a));

        assertTrue(Range.just("a").contains("a"));

        final NavigableSet<String> c = CommonUtil.toNavigableSet("a", "c", "d", "b");

        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.just("a")));

        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.open("a", "a")));
        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.closed("a", "a")));
        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.openClosed("a", "a")));
        assertEquals(CommonUtil.toNavigableSet(), Iterables.subSet(c, Range.closedOpen("a", "a")));
        assertEquals(CommonUtil.toNavigableSet("a"), Iterables.subSet(c, Range.closedOpen("a", "b")));
        assertEquals(CommonUtil.toNavigableSet("a", "b"), Iterables.subSet(c, Range.closed("a", "b")));
        assertEquals(CommonUtil.toNavigableSet("a", "b"), Iterables.subSet(c, Range.closedOpen("a", "c")));

        N.println(Strings.repeat("=", 80));

        N.println(N.commonSet(a, b));
        N.println(Strings.repeat("=", 80));

    }

    @Test
    public void test_jaxb_1() throws Exception {
        assertDoesNotThrow(() -> {
            Beans.registerXmlBindingClass(JaxbBean.class);

            final JaxbBean jb = new JaxbBean();
            jb.setString("string1");
            jb.getList().add("list_e_1");
            jb.getMap().put("map_key_1", "map_value_1");
            N.println(abacusXmlParser.serialize(jb));
            N.println(CommonUtil.stringOf(abacusXmlParser.deserialize(abacusXmlParser.serialize(jb), JaxbBean.class)));
        });
    }

    @Test
    public void test_jaxb_2() throws Exception {
        assertDoesNotThrow(() -> {
            Beans.registerXmlBindingClass(JaxbBean.class);

            final JaxbBean jb = new JaxbBean();
            jb.setString("string1");
            jb.getList().add("list_e_1");
            jb.getMap().put("map_key_1", "map_value_1");
            N.println(abacusXMLDOMParser.serialize(jb));
            N.println(CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(jb), JaxbBean.class)));
        });
    }

    @Test
    public void test_valueOf_1() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.toList("abc", "123"));
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.toList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.toList(1, 2, 3));
        assertFalse(bean.equals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXMLGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXmlParser.serialize(bean));

        assertFalse(bean.equals(abacusXmlParser.deserialize(abacusXmlParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void test_valueOf_2() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.toList("abc", "123"));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.toList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.toList(1, 2, 3));
        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXMLGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXMLDOMParser.serialize(bean));

        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void testSerialize7() throws Exception {
        assertDoesNotThrow(() -> {
            final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
            final XBean xmlBean = abacusXmlParser.deserialize(is, xBeanXmlDeserConfig(), XBean.class);
            is.close();

            N.println(CommonUtil.stringOf(xmlBean));
        });
    }

    @Test
    public void testSerialize7_() throws Exception {
        assertDoesNotThrow(() -> {
            final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
            final XBean xmlBean = abacusXMLDOMParser.deserialize(is, xBeanXmlDeserConfig(), XBean.class);
            is.close();

            N.println(CommonUtil.stringOf(xmlBean));
        });
    }

    @Test
    public void testMarshaller() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + "kd ");
        customer.setAge(29);
        customer.setChar('c');

        final String xml = XmlUtil.marshal(customer);
        println(xml);

        final Customer newCustomer = XmlUtil.unmarshal(Customer.class, xml);

        assertEquals(customer, newCustomer);
    }

    @Test
    public void testXMLEncoder() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + (char) 0 + "kd ");
        customer.setAge(29);
        customer.setChar((char) 1);

        //        final String xml = XmlUtil.xmlEncode(customer);
        //        println(xml);
        //
        //        final Customer newCustomer = XmlUtil.xmlDecode(xml);
        //
        //        assertEquals(customer, newCustomer);

        assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlEncode(customer));
    }

    @Test
    public void fprintln_format() {
        assertDoesNotThrow(() -> {
            N.fprintln("Hello, %s! You are %d.", "World", 30);
        });
    }

    @Test
    public void testfprintln() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("Hello %s, you are %d years old", "John", 30);
            String output = baos.toString();
            assertTrue(output.contains("Hello John, you are 30 years old"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testfprintlnWithVariousFormats() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("String: %s, Integer: %d, Float: %.2f", "test", 42, 3.14159);
            String output = baos.toString();
            assertTrue(output.contains("String: test, Integer: 42, Float: 3.14"));

            baos.reset();
            N.fprintln("No arguments");
            assertEquals("No arguments" + IOUtil.LINE_SEPARATOR, baos.toString());

            baos.reset();
            N.fprintln("%s", "used", "ignored");
            assertEquals("used" + IOUtil.LINE_SEPARATOR, baos.toString());
            assertThrows(java.util.MissingFormatArgumentException.class, () -> N.fprintln("%s %s", "only one"));
            assertThrows(java.util.IllegalFormatConversionException.class, () -> N.fprintln("%d", "text"));

            baos.reset();
            N.fprintln("%d%% complete", 75);
            assertTrue(baos.toString().contains("75% complete"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void test_fprintln() {
        assertDoesNotThrow(() -> {
            Object[] array = null;
            N.println(array);

            array = new Object[0];
            N.println(array);

            array = CommonUtil.asArray("123", "abc", "234", "ijk");
            N.println(array);

            List<?> list = null;
            N.println(list);

            list = new ArrayList<>();
            N.println(list);

            list = CommonUtil.toList("123", "abc", "234", "ijk");
            N.println(list);

            Map<?, ?> map = null;
            N.println(map);

            map = new HashMap<>();
            N.println(map);

            map = CommonUtil.asMap("123", "abc", "234", "ijk");
            N.println(map);
        });
    }

    @Test
    public void testCollectorIntegration() {
        List<String> words = Arrays.asList("apple", "banana", "apricot", "blueberry", "cherry");

        Map<Character, String> joined = N.groupBy(words, s -> s.charAt(0), Collectors.joining(", "));

        assertEquals("apple, apricot", joined.get('a'));
        assertEquals("banana, blueberry", joined.get('b'));
        assertEquals("cherry", joined.get('c'));

        Map<Integer, Long> countByLength = N.groupBy(words, String::length, Collectors.counting());

        assertEquals(1L, (long) countByLength.get(5));
        assertEquals(2L, (long) countByLength.get(6));
    }

    @Test
    public void testComplexIfNotEmptyChaining() {
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("numbers", Arrays.asList(1, 2, 3, 4, 5));

        AtomicInteger result = new AtomicInteger(0);

        N.ifNotEmpty(data, map -> {
            N.ifNotEmpty(map.get("numbers"), list -> {
                N.ifNotEmpty(list.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()), evens -> {
                    result.set(evens.stream().mapToInt(Integer::intValue).sum());
                });
            });
        });

        assertEquals(6, result.get());
    }

    @Test
    public void test_compare_perf() {
        final int len = 1000;
        final int[] a = Array.range(0, len);
        final int[] b = Array.range(0, len);
        a[len - 1] = 0;
        b[len - 1] = 1;

        assertEquals(-1, CommonUtil.compare(a, b));
        assertEquals(-1, Arrays.compare(a, b));

        Profiler.run(1, 1000, 3, "N.compare(...)", () -> assertEquals(-1, CommonUtil.compare(a, b))).printResult();
        Profiler.run(1, 1000, 3, "Arrays.compare(...)", () -> assertEquals(-1, Arrays.compare(a, b))).printResult();
    }

    @Test
    public void test_clone_01() {
        assertDoesNotThrow(() -> {
            Beans.deepCopy(u.Optional.of("a"));
            Beans.deepCopy(u.Nullable.of("a"));
        });
    }

    @Test
    public void test_repeat() {
        assertDoesNotThrow(() -> {
            Array.repeat((String) null, 10);
        });
    }

    @Test
    public void test_0003() {
        String str = "-a-";

        assertEquals("a", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("a", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));

        str = "--";

        assertEquals("", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));
    }

    @Test
    public void test_002() {
        assertDoesNotThrow(() -> {
            try {
                Try.call((Callable<Object>) () -> {
                    throw new Exception();
                });
            } catch (final RuntimeException e) {

            }

            try {
                Try.run((Throwables.Runnable) () -> {
                    throw new Exception();
                });
            } catch (final RuntimeException e) {

            }
        });
    }

    @Test
    public void testFprintln_nullFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> N.fprintln((String) null));
    }
}
