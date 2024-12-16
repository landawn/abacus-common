/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A simple way to run load/performance tests.
 *
 * <br />
 * <br />
 * Caution: if the loop number is too big, it may take a lot of memory to save the test result and impact the test result.
 * <br />
 * So instead of running the performance with big loop number:
 * <pre>
 * <code>
 * final int bigLoopNum = 1000_000;
 * Profiler.run(threadNum, bigLoopNum, roundNum, "yourMethod", () -> yourMethod());
 * </code>
 *
 * <b>// reduce the <i>bigLoopNum</i> by for-loop:</b>
 *
 * <code>
 * Profiler.run(threadNum, bigLoopNum / 1000, roundNum, "yourMethod", () ->
 *  {
 *      for (int i = 0; i &lt; 1000; i++) {
 *          yourMethod();
 *      }
 *  });
 * </code>
 * </pre>
 *
 * @see <a href="https://medium.com/@AlexanderObregon/introduction-to-java-microbenchmarking-with-jmh-java-microbenchmark-harness-55af74b2fd38">Introduction to Java Microbenchmarking with JMH</a>
 *
 */
@SuppressWarnings({ "java:S1244", "java:S1943" })
public final class Profiler {

    private static final Logger logger = LoggerFactory.getLogger(Profiler.class);

    private static final DecimalFormat elapsedTimeFormat = new DecimalFormat("#0.000");

    private Profiler() {
        // singleton
    }

    /**
     * Runs a performance test with the specified number of threads, loops, and rounds.
     *
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @param command the command to be executed in each loop
     * @return the statistics of the performance test
     */
    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final Throwables.Runnable<? extends Exception> command) {
        return run(threadNum, loopNum, roundNum, "run", command);
    }

    /**
     * Runs a performance test with the specified number of threads, loops, and rounds, and a custom label.
     *
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @param label a custom label for the test
     * @param command the command to be executed in each loop
     * @return the statistics of the performance test
     */
    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final String label,
            final Throwables.Runnable<? extends Exception> command) {
        return run(threadNum, 0, loopNum, 0, roundNum, label, command);
    }

    /**
     * Runs a performance test with the specified number of threads, thread delay, loops, loop delay, rounds, and a custom label.
     *
     * @param threadNum the number of threads to use
     * @param threadDelay the delay between starting each thread
     * @param loopNum the number of loops to run in each thread
     * @param loopDelay the delay between each loop
     * @param roundNum the number of rounds to repeat the test
     * @param label a custom label for the test
     * @param command the command to be executed in each loop
     * @return the statistics of the performance test
     */
    public static MultiLoopsStatistics run(final int threadNum, final long threadDelay, final int loopNum, final long loopDelay, final int roundNum,
            final String label, final Throwables.Runnable<? extends Exception> command) {
        return run(command, label, getMethod(command, "run"), null, null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the name of the method to be tested
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final String method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, getMethod(instance, method), threadNum, loopNum, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, (Object) null, threadNum, loopNum, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param arg the argument to be passed to the method
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, arg, threadNum, 0, loopNum, 0, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, thread delay, loops, loop delay, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param arg the argument to be passed to the method
     * @param threadNum the number of threads to use
     * @param threadDelay the delay between starting each thread
     * @param loopNum the number of loops to run in each thread
     * @param loopDelay the delay between each loop
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final long threadDelay,
            final int loopNum, final long loopDelay, final int roundNum) {
        return run(instance, method, ((arg == null) ? null : Array.asList(arg)), null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param args the arguments to be passed to the method. The size of {@code args} can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final List<?> args, final int threadNum, final int loopNum,
            final int roundNum) {
        return run(instance, method, args, null, null, null, null, threadNum, 0, loopNum, 0, roundNum);
    }

    /**
     * Run performance test for the specified {@code method} with the specified {@code threadNum} and {@code loopNum} for each thread.
     * The performance test will be repeatedly execute times specified by {@code roundNum}.
     *
     * @param instance
     * @param method
     * @param args the size of {@code args} can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
     * @param setUpForMethod
     * @param tearDownForMethod
     * @param setUpForLoop
     * @param tearDownForLoop
     * @param threadNum
     * @param threadDelay
     * @param loopNum
     * @param loopDelay
     * @param roundNum
     * @return
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final List<?> args, final Method setUpForMethod, final Method tearDownForMethod,
            final Method setUpForLoop, final Method tearDownForLoop, final int threadNum, final long threadDelay, final int loopNum, final long loopDelay,
            final int roundNum) {
        return run(instance, method.getName(), method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay, loopNum,
                loopDelay, roundNum);
    }

    /**
     * Run performance test for the specified {@code methodList} with the specified {@code threadNum} and {@code loopNum} for each thread.
     * The performance test will be repeatedly execute times specified by {@code roundNum}.
     *
     * @param instance it can be {@code null} if methods in the specified {@code methodList} are static methods
     * @param methodName
     * @param method
     * @param args the size of {@code args} can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
     * @param setUpForMethod
     * @param tearDownForMethod
     * @param setUpForLoop
     * @param tearDownForLoop
     * @param threadNum
     * @param threadDelay
     * @param loopNum loops run by each thread.
     * @param loopDelay
     * @param roundNum
     * @return
     */
    static MultiLoopsStatistics run(final Object instance, final String methodName, final Method method, final List<?> args, final Method setUpForMethod,
            final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int threadNum, final long threadDelay,
            final int loopNum, final long loopDelay, final int roundNum) {
        if ((threadNum <= 0) || (loopNum <= 0) || (threadDelay < 0) || (loopDelay < 0)) {
            throw new IllegalArgumentException("threadNum=" + threadNum + ", loopNum=" + loopNum + ", threadDelay=" + threadDelay + ", loopDelay=" + loopDelay); //NOSONAR
        }
        if (N.notEmpty(args) && (args.size() > 1) && (args.size() != threadNum)) {
            throw new IllegalArgumentException(
                    "The input args must be null or size = 1 or size = threadNum. It's the input parameter for the every loop in each thread ");
        }
        // It takes about 250MB memory to save 1 million test result.
        if (threadNum * loopNum > IOUtil.MAX_MEMORY_IN_MB * 1000) {
            if (IOUtil.MAX_MEMORY_IN_MB < 1024) {
                logger.warn(
                        "Saving big number loop result in small memory may slow down the performance of target method. Consider increasing the maximum JVM memory size.");
            } else {
                logger.warn(
                        "Saving big number loop result may slow down the performance of target method. Consider adding for-loop to outer of the target method and reducing the loop number ("
                                + loopNum + ") to a smaller number");
            }
        }

        if (roundNum == 1) {
            return run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay, loopNum,
                    loopDelay);
        } else {
            MultiLoopsStatistics result = null;

            for (int i = 0; i < (suspended ? 1 : roundNum); i++) {
                if (result != null) {
                    result.printResult();
                    result = null; //NOSONAR
                }

                result = run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay,
                        loopNum, loopDelay);
            }

            return result;
        }
    }

    /**
     *
     * @param instance
     * @param methodName
     * @param method
     * @param args
     * @param setUpForMethod
     * @param tearDownForMethod
     * @param setUpForLoop
     * @param tearDownForLoop
     * @param threadNum
     * @param threadDelay
     * @param loopNum
     * @param loopDelay
     * @return
     */
    @SuppressWarnings("deprecation")
    private static MultiLoopsStatistics run(final Object instance, final String methodName, final Method method, final List<?> args,
            final Method setUpForMethod, final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int threadNum,
            final long threadDelay, final int loopNum, final long loopDelay) {
        if (!method.isAccessible()) {
            ClassUtil.setAccessibleQuietly(method, true);
        }

        gc();

        @SuppressWarnings("resource")
        final ExecutorService asyncExecutor = Executors.newFixedThreadPool(threadNum);
        final AtomicInteger threadCounter = new AtomicInteger();
        // MXBean mxBean = new MXBean();
        final List<LoopStatistics> loopStatisticsList = Collections.synchronizedList(new ArrayList<>());
        final PrintStream ps = System.out; //NOSONAR
        final long startTimeInMillis = System.currentTimeMillis();
        final long startTimeInNano = System.nanoTime();

        for (int threadIndex = 0; threadIndex < (suspended ? 1 : threadNum); threadIndex++) {
            final Object arg = (N.isEmpty(args)) ? null : ((args.size() == 1) ? args.get(0) : args.get(threadIndex));
            threadCounter.incrementAndGet();

            asyncExecutor.execute(() -> {
                try {
                    runLoops(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, loopNum, loopDelay,
                            loopStatisticsList, ps);
                } finally {
                    threadCounter.decrementAndGet();
                }
            });

            sleep(threadDelay);
        }

        while (threadCounter.get() > 0) {
            N.sleep(1);
        }

        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();
        return new MultiLoopsStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, loopStatisticsList);
    }

    /**
     *
     * @param instance
     * @param methodName
     * @param method
     * @param arg
     * @param setUpForMethod
     * @param tearDownForMethod
     * @param setUpForLoop
     * @param tearDownForLoop
     * @param loopNum
     * @param loopDelay
     * @param loopStatisticsList
     * @param ps
     */
    private static void runLoops(final Object instance, final String methodName, final Method method, final Object arg, final Method setUpForMethod,
            final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int loopNum, final long loopDelay,
            final List<LoopStatistics> loopStatisticsList, final PrintStream ps) {
        for (int loopIndex = 0; loopIndex < (suspended ? 1 : loopNum); loopIndex++) {
            if (setUpForLoop != null) {
                try {
                    setUpForLoop.invoke(instance);
                } catch (final Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }

            final long startTimeInMillis = System.currentTimeMillis();
            final long startTimeInNano = System.nanoTime();
            final List<MethodStatistics> methodStatisticsList = runLoop(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, ps);
            final long endTimeInNano = System.nanoTime();
            final long endTimeInMillis = System.currentTimeMillis();

            if (tearDownForLoop != null) {
                try {
                    tearDownForLoop.invoke(instance);
                } catch (final Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }

            final SingleLoopStatistics loopStatistics = new SingleLoopStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano,
                    methodStatisticsList);
            loopStatisticsList.add(loopStatistics);

            sleep(loopDelay);
        }
    }

    /**
     *
     * @param instance
     * @param methodName
     * @param method
     * @param arg
     * @param setUpForMethod
     * @param tearDownForMethod
     * @param ps
     * @return
     */
    private static List<MethodStatistics> runLoop(final Object instance, final String methodName, final Method method, final Object arg,
            final Method setUpForMethod, final Method tearDownForMethod, final PrintStream ps) {
        final List<MethodStatistics> methodStatisticsList = new ArrayList<>();

        if (setUpForMethod != null) {
            try {
                setUpForMethod.invoke(instance);
            } catch (final Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(ExceptionUtil.getErrorMessage(e, true));
            }
        }

        final long startTimeInMillis = System.currentTimeMillis();
        final long startTimeInNano = System.nanoTime();
        Object result = null;

        try {
            if (method.getParameterTypes().length == 0) {
                method.invoke(instance);
            } else {
                method.invoke(instance, arg);
            }
        } catch (final InvocationTargetException e) {
            e.printStackTrace(ps);
            logger.warn(ExceptionUtil.getErrorMessage(e, true));
            result = e.getTargetException();
        } catch (final Exception e) {
            e.printStackTrace(ps);
            logger.warn(ExceptionUtil.getErrorMessage(e, true));
            result = e;
        }

        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();

        if (tearDownForMethod != null) {
            try {
                tearDownForMethod.invoke(instance);
            } catch (final Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(ExceptionUtil.getErrorMessage(e, true));
            }
        }

        final MethodStatistics methodStatistics = new MethodStatistics(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, result);
        methodStatisticsList.add(methodStatistics);
        return methodStatisticsList;
    }

    /**
     * Gets the method.
     *
     * @param instance
     * @param methodName
     * @return
     */
    @SuppressWarnings("deprecation")
    private static Method getMethod(final Object instance, final String methodName) {
        final Method method = ClassUtil.getDeclaredMethod(instance.getClass(), methodName);
        if (method == null) {
            throw new IllegalArgumentException("No method found by name: " + methodName);
        } else if (!method.isAccessible()) {
            ClassUtil.setAccessibleQuietly(method, true);
        }
        return method;
    }

    private static volatile boolean suspended = false;

    /**
     * Suspends or resumes performance tests running on {@code Profiler}.
     * If the performance tests are suspended, the {@code Profiler} will only execute one loop with one thread for each test.
     *
     * @param yesOrNo if {@code true}, the profiler is suspended; if {@code false}, the profiler is resumed
     */
    public static void suspend(final boolean yesOrNo) {
        suspended = yesOrNo;
    }

    static void sleep(final long millis) {
        if (suspended) {
            return;
        }

        N.sleep(millis);
    }

    private static void gc() {
        if (suspended) {
            return;
        }

        Runtime.getRuntime().gc(); //NOSONAR
        N.sleep(1000);
    }

    /**
     * The Interface Statistics.
     *
     * @version $Revision: 0.8 $
     */
    interface Statistics {

        /**
         * Gets the result.
         *
         * @return
         */
        Object getResult();

        /**
         * Sets the result.
         *
         * @param result the new result
         */
        void setResult(Object result);

        /**
         * Gets the start time in millis.
         *
         * @return
         */
        long getStartTimeInMillis();

        /**
         * Sets the start time in millis.
         *
         * @param startTimeInMillis the new start time in millis
         */
        void setStartTimeInMillis(long startTimeInMillis);

        /**
         * Gets the end time in millis.
         *
         * @return
         */
        long getEndTimeInMillis();

        /**
         * Sets the end time in millis.
         *
         * @param endTimeInMillis the new end time in millis
         */
        void setEndTimeInMillis(long endTimeInMillis);

        /**
         * Gets the start time in nano.
         *
         * @return
         */
        long getStartTimeInNano();

        /**
         * Sets the start time in nano.
         *
         * @param startTimeInNano the new start time in nano
         */
        void setStartTimeInNano(long startTimeInNano);

        /**
         * Gets the end time in nano.
         *
         * @return
         */
        long getEndTimeInNano();

        /**
         * Sets the end time in nano.
         *
         * @param endTimeInNano the new end time in nano
         */
        void setEndTimeInNano(long endTimeInNano);

        /**
         * Gets the elapsed time in millis.
         *
         * @return
         */
        double getElapsedTimeInMillis();
    }

    /**
     * The Interface LoopStatistics.
     *
     * @version $Revision: 0.8 $
     */
    public interface LoopStatistics extends Statistics {

        /**
         * Gets the method name list.
         *
         * @return
         */
        List<String> getMethodNameList();

        /**
         * Gets the min elapsed time method.
         *
         * @return
         */
        MethodStatistics getMinElapsedTimeMethod();

        /**
         * Gets the max elapsed time method.
         *
         * @return
         */
        MethodStatistics getMaxElapsedTimeMethod();

        /**
         * Gets the method total elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        double getMethodTotalElapsedTimeInMillis(String methodName);

        /**
         * Gets the method max elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        double getMethodMaxElapsedTimeInMillis(String methodName);

        /**
         * Gets the method min elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        double getMethodMinElapsedTimeInMillis(String methodName);

        /**
         * Gets the method average elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        double getMethodAverageElapsedTimeInMillis(String methodName);

        /**
         * Gets the total elapsed time in millis.
         *
         * @return
         */
        double getTotalElapsedTimeInMillis();

        /**
         * Gets the method size.
         *
         * @param methodName
         * @return
         */
        int getMethodSize(String methodName);

        /**
         * Gets the method statistics list.
         *
         * @param methodName
         * @return
         */
        List<MethodStatistics> getMethodStatisticsList(String methodName);

        /**
         * Gets the failed method statistics list.
         *
         * @param methodName
         * @return
         */
        List<MethodStatistics> getFailedMethodStatisticsList(String methodName);

        /**
         * Gets the all failed method statistics list.
         *
         * @return
         */
        List<MethodStatistics> getAllFailedMethodStatisticsList();
    }

    /**
     * The Class AbstractStatistics.
     *
     * @version $Revision: 0.8 $
     */
    abstract static class AbstractStatistics implements Statistics {

        /** The start time in millis. */
        private long startTimeInMillis;

        /** The end time in millis. */
        private long endTimeInMillis;

        /** The start time in nano. */
        private long startTimeInNano;

        /** The end time in nano. */
        private long endTimeInNano;

        /** The result. */
        private Object result;

        /**
         * Instantiates a new abstract statistics.
         */
        protected AbstractStatistics() {
            this(0, 0, 0, 0);
        }

        /**
         * Instantiates a new abstract statistics.
         *
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         */
        protected AbstractStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this.startTimeInMillis = startTimeInMillis;
            this.endTimeInMillis = endTimeInMillis;
            this.startTimeInNano = startTimeInNano;
            this.endTimeInNano = endTimeInNano;
        }

        /**
         * Gets the start time in millis.
         *
         * @return
         */
        @Override
        public long getStartTimeInMillis() {
            return startTimeInMillis;
        }

        /**
         * Sets the start time in millis.
         *
         * @param startTimeInMillis the new start time in millis
         */
        @Override
        public void setStartTimeInMillis(final long startTimeInMillis) {
            this.startTimeInMillis = startTimeInMillis;
        }

        /**
         * Gets the end time in millis.
         *
         * @return
         */
        @Override
        public long getEndTimeInMillis() {
            return endTimeInMillis;
        }

        /**
         * Sets the end time in millis.
         *
         * @param endTimeInMillis the new end time in millis
         */
        @Override
        public void setEndTimeInMillis(final long endTimeInMillis) {
            this.endTimeInMillis = endTimeInMillis;
        }

        /**
         * Gets the start time in nano.
         *
         * @return
         */
        @Override
        public long getStartTimeInNano() {
            return startTimeInNano;
        }

        /**
         * Sets the start time in nano.
         *
         * @param startTimeInNano the new start time in nano
         */
        @Override
        public void setStartTimeInNano(final long startTimeInNano) {
            this.startTimeInNano = startTimeInNano;
        }

        /**
         * Gets the end time in nano.
         *
         * @return
         */
        @Override
        public long getEndTimeInNano() {
            return endTimeInNano;
        }

        /**
         * Sets the end time in nano.
         *
         * @param endTimeInNano the new end time in nano
         */
        @Override
        public void setEndTimeInNano(final long endTimeInNano) {
            this.endTimeInNano = endTimeInNano;
        }

        /**
         * Gets the elapsed time in millis.
         *
         * @return
         */
        @Override
        public double getElapsedTimeInMillis() {
            return (endTimeInNano - startTimeInNano) / 1000000.0d; // convert to milliseconds.
        }

        /**
         * Gets the result.
         *
         * @return
         */
        @Override
        public Object getResult() {
            return result;
        }

        /**
         * Sets the result.
         *
         * @param result the new result
         */
        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        /**
         * Time 2 string.
         *
         * @param timeInMillis
         * @return
         */
        protected String time2String(final long timeInMillis) {
            final Timestamp timestamp = DateUtil.createdTimestamp(timeInMillis);
            return DateUtil.format(timestamp, DateUtil.ISO_LOCAL_DATE_TIME_FORMAT); // + " " + N.LOCAL_TIME_ZONE.getID();
        }
    }

    /**
     * The Class MethodStatistics.
     *
     * @version $Revision: 0.8 $
     */
    public static class MethodStatistics extends AbstractStatistics {

        /** The method name. */
        private final String methodName;

        /** The result. */
        private Object result;

        /**
         * Instantiates a new method statistics.
         *
         * @param methodName
         */
        public MethodStatistics(final String methodName) {
            this.methodName = methodName;
        }

        /**
         * Instantiates a new method statistics.
         *
         * @param methodName
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         */
        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano) {
            this(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        /**
         * Instantiates a new method statistics.
         *
         * @param methodName
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         * @param result
         */
        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano, final Object result) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodName = methodName;
            this.result = result;
        }

        /**
         * Gets the method name.
         *
         * @return
         */
        public String getMethodName() {
            return methodName;
        }

        /**
         * Gets the result.
         *
         * @return
         */
        @Override
        public Object getResult() {
            return result;
        }

        /**
         * Sets the result.
         *
         * @param result the new result
         */
        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        /**
         * Checks if is failed.
         *
         * @return {@code true}, if is failed
         */
        public boolean isFailed() {
            return result instanceof Exception;
        }

        @Override
        public String toString() {
            if (isFailed()) {
                final Exception e = (Exception) result;
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + ClassUtil.getSimpleClassName(e.getClass()) + ": " + e.getMessage() + ".";
            } else {
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + result + ".";
            }
        }
    }

    /**
     * The Class SingleLoopStatistics.
     */
    static class SingleLoopStatistics extends AbstractStatistics implements LoopStatistics {

        /** The method statistics list. */
        private List<MethodStatistics> methodStatisticsList;

        /**
         * Instantiates a new single loop statistics.
         */
        public SingleLoopStatistics() {
        }

        /**
         * Instantiates a new single loop statistics.
         *
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         */
        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        /**
         * Instantiates a new single loop statistics.
         *
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         * @param methodStatisticsList
         */
        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final List<MethodStatistics> methodStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodStatisticsList = methodStatisticsList;
        }

        /**
         * Gets the method name list.
         *
         * @return
         */
        @Override
        public List<String> getMethodNameList() {
            final List<String> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (!result.contains(methodStatistics.getMethodName())) {
                        result.add(methodStatistics.getMethodName());
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method statistics list.
         *
         * @return
         */
        public List<MethodStatistics> getMethodStatisticsList() {
            if (methodStatisticsList == null) {
                methodStatisticsList = new ArrayList<>();
            }
            return methodStatisticsList;
        }

        /**
         * Sets the method statistics list.
         *
         * @param methodStatisticsList the new method statistics list
         */
        public void setMethodStatisticsList(final List<MethodStatistics> methodStatisticsList) {
            this.methodStatisticsList = methodStatisticsList;
        }

        /**
         * Adds the method statistics list.
         *
         * @param methodStatistics
         */
        public void addMethodStatisticsList(final MethodStatistics methodStatistics) {
            getMethodStatisticsList().add(methodStatistics);
        }

        /**
         * Gets the max elapsed time method.
         *
         * @return
         */
        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the min elapsed time method.
         *
         * @return
         */
        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method total elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result += methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method max elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName) && (methodStatistics.getElapsedTimeInMillis() > result)) {
                        result = methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method min elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName) && (methodStatistics.getElapsedTimeInMillis() < result)) {
                        result = methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method average elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        totalTime += methodStatistics.getElapsedTimeInMillis();
                        methodNum++;
                    }
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        /**
         * Gets the total elapsed time in millis.
         *
         * @return
         */
        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    result += methodStatistics.getElapsedTimeInMillis();
                }
            }
            return result;
        }

        /**
         * Gets the method size.
         *
         * @param methodName
         * @return
         */
        @Override
        public int getMethodSize(final String methodName) {
            int methodSize = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        methodSize++;
                    }
                }
            }
            return methodSize;
        }

        /**
         * Gets the method statistics list.
         *
         * @param methodName
         * @return
         */
        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>(getMethodSize(methodName));
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        /**
         * Gets the failed method statistics list.
         *
         * @param methodName
         * @return
         */
        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed() && methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        /**
         * Gets the all failed method statistics list.
         *
         * @return
         */
        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            final List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed()) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }
    }

    /**
     * The Class MultiLoopsStatistics.
     */
    public static class MultiLoopsStatistics extends AbstractStatistics implements LoopStatistics {

        /** The Constant SEPARATOR_LINE. */
        private static final String SEPARATOR_LINE = "========================================================================================================================";

        /** The thread num. */
        private final int threadNum;

        /** The loop statistics list. */
        private List<LoopStatistics> loopStatisticsList;

        /**
         * Instantiates a new multi loops statistics.
         *
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         * @param threadNum
         */
        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, null);
        }

        /**
         * Instantiates a new multi loops statistics.
         *
         * @param startTimeInMillis
         * @param endTimeInMillis
         * @param startTimeInNano
         * @param endTimeInNano
         * @param threadNum
         * @param loopStatisticsList
         */
        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum, final List<LoopStatistics> loopStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.threadNum = threadNum;
            this.loopStatisticsList = loopStatisticsList;
        }

        /**
         * Gets the thread num.
         *
         * @return
         */
        public int getThreadNum() {
            return threadNum;
        }

        /**
         * Gets the method name list.
         *
         * @return
         */
        @Override
        public List<String> getMethodNameList() {
            List<String> result = null;
            if (loopStatisticsList == null) {
                result = new ArrayList<>();
            } else {
                result = (loopStatisticsList.get(0)).getMethodNameList();
            }
            return result;
        }

        /**
         * Gets the loop statistics list.
         *
         * @return
         */
        public List<LoopStatistics> getLoopStatisticsList() {
            if (loopStatisticsList == null) {
                loopStatisticsList = new ArrayList<>();
            }
            return loopStatisticsList;
        }

        /**
         * Sets the loop statistics list.
         *
         * @param loopStatisticsList the new loop statistics list
         */
        public void setLoopStatisticsList(final List<LoopStatistics> loopStatisticsList) {
            this.loopStatisticsList = loopStatisticsList;
        }

        /**
         * Adds the method statistics list.
         *
         * @param loopStatistics
         */
        public void addMethodStatisticsList(final LoopStatistics loopStatistics) {
            getLoopStatisticsList().add(loopStatistics);
        }

        /**
         * Gets the max elapsed time method.
         *
         * @return
         */
        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;

            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final MethodStatistics methodStatistics = loopStatistics.getMaxElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }

            return result;
        }

        /**
         * Gets the min elapsed time method.
         *
         * @return
         */
        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final MethodStatistics methodStatistics = loopStatistics.getMinElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method total elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                }
            }
            return result;
        }

        /**
         * Gets the method max elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodMaxTime = loopStatistics.getMethodMaxElapsedTimeInMillis(methodName);
                    if (loopMethodMaxTime > result) {
                        result = loopMethodMaxTime;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method min elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodMinTime = loopStatistics.getMethodMinElapsedTimeInMillis(methodName);
                    if (loopMethodMinTime < result) {
                        result = loopMethodMinTime;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method average elapsed time in millis.
         *
         * @param methodName
         * @return
         */
        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodTotalTime = loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                    final int loopMethodSize = loopStatistics.getMethodSize(methodName);
                    totalTime += loopMethodTotalTime;
                    methodNum += loopMethodSize;
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        /**
         * Gets the total elapsed time in millis.
         *
         * @return
         */
        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getTotalElapsedTimeInMillis();
                }
            }
            return result;
        }

        /**
         * Gets the method size.
         *
         * @param methodName
         * @return
         */
        @Override
        public int getMethodSize(final String methodName) {
            int result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodSize(methodName);
                }
            }
            return result;
        }

        /**
         * Gets the method statistics list.
         *
         * @param methodName
         * @return
         */
        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> methodStatisticsList = new ArrayList<>(getMethodSize(methodName));
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    methodStatisticsList.addAll(loopStatistics.getMethodStatisticsList(methodName));
                }
            }
            return methodStatisticsList;
        }

        /**
         * Gets the failed method statistics list.
         *
         * @param methodName
         * @return
         */
        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getFailedMethodStatisticsList(methodName));
                }
            }
            return result;
        }

        /**
         * Gets the all failed method statistics list.
         *
         * @return
         */
        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            final List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getAllFailedMethodStatisticsList());
                }
            }
            return result;
        }

        /**
         * Gets the total call.
         *
         * @return
         */
        private int getTotalCall() {
            int res = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    res += loopStatistics.getMethodNameList().size();
                }
            }
            return res;
        }

        /**
         * Prints the result.
         */
        public void printResult() {
            writeResult(new PrintWriter(System.out)); //NOSONAR
        }

        /**
         *
         * @param output
         */
        public void writeResult(final OutputStream output) {
            writeResult(new PrintWriter(output));
        }

        /**
         *
         * @param output
         */
        public void writeResult(final Writer output) {
            writeResult(new PrintWriter(output));
        }

        /**
         *
         * @param output
         */
        private void writeResult(final PrintWriter output) {
            output.println();
            output.println(SEPARATOR_LINE);
            output.println("(unit: milliseconds)");
            output.println("threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum));
            output.println("startTime: " + time2String(getStartTimeInMillis()));
            output.println("endTime:   " + time2String(getEndTimeInMillis()));
            output.println("totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()));
            output.println();
            //            MethodStatistics methodStatistics = getMaxElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println("maxMethodTime(" + methodStatistics.getMethodName() + "): " + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()));
            //            }
            //
            //            methodStatistics = getMinElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println("minMethodTime(" + methodStatistics.getMethodName() + "): " + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()));
            //            }
            final String methodNameTitle = "<method name>";
            final List<String> methodNameList = getMethodNameList();
            int maxMethodNameLength = methodNameTitle.length();
            if (methodNameList.size() > 0) {
                for (final String methodName : methodNameList) {
                    if (methodName.length() > maxMethodNameLength) {
                        maxMethodNameLength = methodName.length();
                    }
                }
            }
            output.println();
            maxMethodNameLength += 3;
            output.println(Strings.padEnd(methodNameTitle + ",  ", maxMethodNameLength)
                    + "|avg time|, |min time|, |max time|, |0.01% >=|, |0.1% >=|,  |1% >=|,    |10% >=|,   |20% >=|,   |50% >=|,   |80% >=|,   |90% >=|,   |99% >=|,   |99.9% >=|, |99.99% >=|");
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final int minLen = 12;
                output.println(Strings.padEnd(methodName + ",  ", maxMethodNameLength) + Strings.padEnd(elapsedTimeFormat.format(avgTime) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(minTime) + ",  ", minLen) + Strings.padEnd(elapsedTimeFormat.format(maxTime) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + ",  ", minLen));
            }
            output.println();
            writeError(output);
            output.println(SEPARATOR_LINE);
            output.flush();
        }

        /**
         *
         * @param output
         */
        private void writeError(final PrintWriter output) {
            MethodStatistics methodStatistics;
            final List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println();
                output.println("Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)"); //NOSONAR
                for (final Object element : failedMethodList) {
                    output.println("--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) element;
                    output.println(methodStatistics.toString());
                }
            }
        }

        /**
         * Write html result.
         *
         * @param output
         */
        public void writeHtmlResult(final OutputStream output) {
            writeHtmlResult(new PrintWriter(output));
        }

        /**
         * Write html result.
         *
         * @param output
         */
        public void writeHtmlResult(final Writer output) {
            writeHtmlResult(new PrintWriter(output));
        }

        /**
         * Write html result.
         *
         * @param output
         */
        private void writeHtmlResult(final PrintWriter output) {
            output.println(SEPARATOR_LINE);
            output.println("<br/>" + "(unit: milliseconds)"); //NOSONAR
            output.println("<br/>" + "threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum));
            output.println("<br/>" + "startTime: " + time2String(getStartTimeInMillis()));
            output.println("<br/>" + "endTime:   " + time2String(getEndTimeInMillis()));
            output.println("<br/>" + "totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()));
            output.println("<br/>"); //NOSONAR

            //            MethodStatistics methodStatistics = getMaxElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println(
            //                        "<br/>" + "maxMethodTime: " + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()));
            //            }
            //
            //            methodStatistics = getMinElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println(
            //                        "<br/>" + "minMethodTime(" + methodStatistics.getMethodName() + "): " + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()));
            //            }
            output.println("<br/>");
            output.println("<table width=\"1200\" border=\"1\">");
            output.println("<tr>");
            output.println("<th>method name</th>");
            output.println("<th>avg time</th>");
            output.println("<th>min time</th>");
            output.println("<th>max time</th>");
            output.println("<th>0.01% &gt;=</th>");
            output.println("<th>0.1% &gt;=</th>");
            output.println("<th>1% &gt;=</th>");
            output.println("<th>10% &gt;=</th>");
            output.println("<th>20% &gt;=</th>");
            output.println("<th>50% &gt;=</th>");
            output.println("<th>80% &gt;=</th>");
            output.println("<th>90% &gt;=</th>");
            output.println("<th>99% &gt;=</th>");
            output.println("<th>99.9% &gt;=</th>");
            output.println("<th>99.99% &gt;=</th>");
            output.println("</tr>");
            final List<String> methodNameList = getMethodNameList();
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                output.println("<tr>");
                output.println("<td>" + methodName + "</td>"); //NOSONAR
                output.println("<td>" + elapsedTimeFormat.format(avgTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(minTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(maxTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</td>");
                output.println("</tr>");
            }
            output.println("</table>");
            writeHtmlError(output);
            output.println(SEPARATOR_LINE);
            output.flush();
        }

        /**
         * Write html error.
         *
         * @param output
         */
        private void writeHtmlError(final PrintWriter output) {
            MethodStatistics methodStatistics;
            final List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println("<h4>Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</h4>"); //NOSONAR
                for (final Object element : failedMethodList) {
                    output.println("<br/>" + "--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) element;
                    output.println("<br/>" + methodStatistics.toString());
                }
            }
        }

        /**
         * Write xml result.
         *
         * @param output
         */
        public void writeXmlResult(final OutputStream output) {
            writeXmlResult(new PrintWriter(output));
        }

        /**
         * Write xml result.
         *
         * @param output
         */
        public void writeXmlResult(final Writer output) {
            writeXmlResult(new PrintWriter(output));
        }

        /**
         * Write xml result.
         *
         * @param output
         */
        private void writeXmlResult(final PrintWriter output) {
            output.println("<result>");
            output.println("<unit>milliseconds</unit>");
            output.println("<threadNum>" + threadNum + "</threadNum>");
            output.println("<loops>" + (loopStatisticsList.size() / threadNum) + "</loops>");
            output.println("<startTime>" + time2String(getStartTimeInMillis()) + "</startTime>");
            output.println("<endTime>" + time2String(getEndTimeInMillis()) + "</endTime>");
            output.println("<totalElapsedTime>" + elapsedTimeFormat.format(getElapsedTimeInMillis()) + "</totalElapsedTime>");
            output.println();
            //            MethodStatistics methodStatistics = getMinElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println("<minMethodTime>" + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()) + "</minMethodTime>");
            //            }
            //
            //            methodStatistics = getMaxElapsedTimeInMillisMethod();
            //
            //            if (methodStatistics != null) {
            //                writer.println("<maxMethodTime>" + elapsedTimeInMillisFormat.format(methodStatistics.getElapsedTimeInMillis()) + "</maxMethodTime>");
            //            }
            final List<String> methodNameList = getMethodNameList();
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                output.println("<method name=\"" + methodName + "\">");
                output.println("<avgTime>" + elapsedTimeFormat.format(avgTime) + "</avgTime>");
                output.println("<minTime>" + elapsedTimeFormat.format(minTime) + "</minTime>");
                output.println("<maxTime>" + elapsedTimeFormat.format(maxTime) + "</maxTime>");
                output.println("<_0.0001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</_0.0001>");
                output.println("<_0.001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</_0.001>");
                output.println("<_0.01>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</_0.01>");
                output.println("<_0.2>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</_0.2>");
                output.println("<_0.5>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</_0.5>");
                output.println("<_0.8>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</_0.8>");
                output.println("<_0.9>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</_0.9>");
                output.println("<_0.99>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</_0.99>");
                output.println("<_0.999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</_0.999>");
                output.println("<_0.9999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</_0.9999>");
                output.println("</method>");
            }
            final List<MethodStatistics> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println("<errors>" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</errors>"); //NOSONAR
                for (final MethodStatistics methodStatistics : failedMethodList) {
                    output.println("<error>" + methodStatistics.toString() + "</error>");
                }
            }
            output.println("</result>");
            output.flush();
        }
    }
}
