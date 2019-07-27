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
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A simple way to run load/performance test.
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
 * <b>// reduce the 'bigLoopNum' by for-loop:</b>
 * 
 * <code>
 * Profiler.run(threadNum, bigLoopNum / 1000, roundNum, "yourMethod", () -> 
 *  {
 *      for (int i = 0; i < 1000; i++) {
 *          yourMethod();
 *      }
 *  });
 * </code>
 * </pre>
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class Profiler {
    private static final Logger logger = LoggerFactory.getLogger(Profiler.class);
    private static final DecimalFormat elapsedTimeFormat = new DecimalFormat("#0.000");

    private Profiler() {
        // singleton
    }

    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final Try.Runnable<? extends Exception> command) {
        return run(threadNum, loopNum, roundNum, "run", command);
    }

    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final String label,
            final Try.Runnable<? extends Exception> command) {
        return run(threadNum, 0, loopNum, 0, roundNum, label, command);
    }

    public static MultiLoopsStatistics run(final int threadNum, final long threadDelay, final int loopNum, final long loopDelay, final int roundNum,
            final String label, final Try.Runnable<? extends Exception> command) {
        return run(command, label, getMethod(command, "run"), null, null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    static MultiLoopsStatistics run(final Object instance, final String method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, getMethod(instance, method), threadNum, loopNum, roundNum);
    }

    static MultiLoopsStatistics run(final Object instance, final Method method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, (Object) null, threadNum, loopNum, roundNum);
    }

    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, arg, threadNum, 0, loopNum, 0, roundNum);
    }

    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final long threadDelay,
            final int loopNum, final long loopDelay, final int roundNum) {
        return run(instance, method, ((arg == null) ? null : N.asList(arg)), null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    /**
     * 
     * @param instance
     * @param method
     * @param args the size of <code>args</code> can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
     * @param threadNum
     * @param loopNum
     * @param roundNum
     * @return
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final List<?> args, final int threadNum, final int loopNum,
            final int roundNum) {
        return run(instance, method, args, null, null, null, null, threadNum, 0, loopNum, 0, roundNum);
    }

    /**
     * Run performance test for the specified <code>method</code> with the specified <code>threadNum</code> and <code>loopNum</code> for each thread.
     * The performance test will be repeatedly execute times specified by <code>roundNum</code>. 
     * 
     * @param instance
     * @param method
     * @param args the size of <code>args</code> can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
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
     * Run performance test for the specified <code>methodList</code> with the specified <code>threadNum</code> and <code>loopNum</code> for each thread.
     * The performance test will be repeatly execute times specified by <code>roundNum</code>. 
     * 
     * @param instance it can be null if methods in the specified <code>methodList</code> are static methods
     * @param methodName
     * @param method
     * @param args the size of <code>args</code> can be 0, 1, or same size with <code>threadNum. It's the input argument for every loop in each thread.
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
            throw new IllegalArgumentException("threadNum=" + threadNum + ", loopNum=" + loopNum + ", threadDelay=" + threadDelay + ", loopDelay=" + loopDelay);
        }
        if (N.notNullOrEmpty(args) && (args.size() > 1) && (args.size() != threadNum)) {
            throw new IllegalArgumentException(
                    "The input args must be null or size = 1 or size = threadNum. It's the input parameter for the every loop in each thread ");
        }
        // It takes about 250MB memory to save 1 million test result.
        if (threadNum * loopNum > IOUtil.MAX_MEMORY_IN_MB * 1000) {
            if (IOUtil.MAX_MEMORY_IN_MB < 1024) {
                logger.warn(
                        "Saving big number loop result in small memory may slow down the performance of target method. Conside increasing the maximium JVM memory size.");
            } else {
                logger.warn(
                        "Saving big number loop result may slow down the performance of target method. Conside adding for-loop to outter of the target method and reducing the loop number ("
                                + loopNum + ") to a smaller number");
            }
        }
        if (roundNum == 1) {
            return run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay, loopNum,
                    loopDelay);
        } else {
            MultiLoopsStatistics result = null;
            for (int i = 0; i < roundNum; i++) {
                if (result != null) {
                    result.printResult();
                    result = null;
                }
                result = run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay,
                        loopNum, loopDelay);
            }
            return result;
        }
    }

    private static MultiLoopsStatistics run(final Object instance, final String methodName, final Method method, final List<?> args,
            final Method setUpForMethod, final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int threadNum,
            final long threadDelay, final int loopNum, final long loopDelay) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        gc();
        N.sleep(1000);
        final ExecutorService asyncExecutor = Executors.newFixedThreadPool(threadNum);
        final AtomicInteger threadCounter = new AtomicInteger();
        // MXBean mxBean = new MXBean();
        final List<LoopStatistics> loopStatisticsList = Collections.synchronizedList(new ArrayList<LoopStatistics>());
        final PrintStream ps = System.out;
        final long startTimeInMillis = System.currentTimeMillis();
        final long startTimeInNano = System.nanoTime();
        for (int threadIndex = 0; threadIndex < (suspended ? 1 : threadNum); threadIndex++) {
            final Object arg = (N.isNullOrEmpty(args)) ? null : ((args.size() == 1) ? args.get(0) : args.get(threadIndex));
            threadCounter.incrementAndGet();
            asyncExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        runLoops(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, loopNum, loopDelay,
                                loopStatisticsList, ps);
                    } finally {
                        threadCounter.decrementAndGet();
                    }
                }
            });
            N.sleep(threadDelay);
        }
        while (threadCounter.get() > 0) {
            N.sleep(1);
        }
        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();
        return new MultiLoopsStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, loopStatisticsList);
    }

    private static void runLoops(final Object instance, final String methodName, final Method method, final Object arg, final Method setUpForMethod,
            final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int loopNum, final long loopDelay,
            final List<LoopStatistics> loopStatisticsList, final PrintStream ps) {
        for (int loopIndex = 0; loopIndex < (suspended ? 1 : loopNum); loopIndex++) {
            if (setUpForLoop != null) {
                try {
                    setUpForLoop.invoke(instance);
                } catch (Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            }
            final long startTimeInMillis = System.currentTimeMillis();
            final long startTimeInNano = System.nanoTime();
            List<MethodStatistics> methodStatisticsList = runLoop(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, ps);
            final long endTimeInNano = System.nanoTime();
            final long endTimeInMillis = System.currentTimeMillis();
            if (tearDownForLoop != null) {
                try {
                    tearDownForLoop.invoke(instance);
                } catch (Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            }
            final SingleLoopStatistics loopStatistics = new SingleLoopStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano,
                    methodStatisticsList);
            loopStatisticsList.add(loopStatistics);
            N.sleep(loopDelay);
        }
    }

    private static List<MethodStatistics> runLoop(final Object instance, final String methodName, final Method method, final Object arg,
            final Method setUpForMethod, final Method tearDownForMethod, final PrintStream ps) {
        final List<MethodStatistics> methodStatisticsList = new ArrayList<>();
        if (setUpForMethod != null) {
            try {
                setUpForMethod.invoke(instance);
            } catch (Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(AbacusException.getErrorMsg(e));
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
        } catch (InvocationTargetException e) {
            e.printStackTrace(ps);
            logger.warn(AbacusException.getErrorMsg(e));
            result = e.getTargetException();
        } catch (Exception e) {
            e.printStackTrace(ps);
            logger.warn(AbacusException.getErrorMsg(e));
            result = e;
        }
        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();
        if (tearDownForMethod != null) {
            try {
                tearDownForMethod.invoke(instance);
            } catch (Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(AbacusException.getErrorMsg(e));
            }
        }
        MethodStatistics methodStatistics = new MethodStatistics(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, result);
        methodStatisticsList.add(methodStatistics);
        return methodStatisticsList;
    }

    private static Method getMethod(final Object instance, final String methodName) {
        Method method = ClassUtil.getDeclaredMethod(instance.getClass(), methodName);
        if (method == null) {
            throw new IllegalArgumentException("No method found by name: " + methodName);
        } else if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    private static void gc() {
        Runtime.getRuntime().gc();
        N.sleep(3000);
    }

    private static boolean suspended = false;

    static void suspend(boolean yesOrNo) {
        suspended = yesOrNo;
    }

    /**
     * @author Haiyang Li
     * @version $Revision: 0.8 $
     */
    static interface Statistics {
        Object getResult();

        void setResult(Object result);

        long getStartTimeInMillis();

        void setStartTimeInMillis(long startTimeInMillis);

        long getEndTimeInMillis();

        void setEndTimeInMillis(long endTimeInMillis);

        long getStartTimeInNano();

        void setStartTimeInNano(long startTimeInNano);

        long getEndTimeInNano();

        void setEndTimeInNano(long endTimeInNano);

        double getElapsedTimeInMillis();
    }

    /**
     * @author Haiyang Li
     * @version $Revision: 0.8 $
     */
    static interface LoopStatistics extends Statistics {
        List<String> getMethodNameList();

        MethodStatistics getMinElapsedTimeMethod();

        MethodStatistics getMaxElapsedTimeMethod();

        public double getMethodTotalElapsedTimeInMillis(String methodName);

        public double getMethodMaxElapsedTimeInMillis(String methodName);

        public double getMethodMinElapsedTimeInMillis(String methodName);

        public double getMethodAverageElapsedTimeInMillis(String methodName);

        public double getTotalElapsedTimeInMillis();

        public int getMethodSize(String methodName);

        public List<MethodStatistics> getMethodStatisticsList(String methodName);

        public List<MethodStatistics> getFailedMethodStatisticsList(String methodName);

        List<MethodStatistics> getAllFailedMethodStatisticsList();
    }

    /**
     * @author Haiyang Li
     * @version $Revision: 0.8 $
     */
    static abstract class AbstractStatistics implements Statistics {
        private long startTimeInMillis;
        private long endTimeInMillis;
        private long startTimeInNano;
        private long endTimeInNano;
        private Object result;

        public AbstractStatistics() {
            this(0, 0, 0, 0);
        }

        public AbstractStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this.startTimeInMillis = startTimeInMillis;
            this.endTimeInMillis = endTimeInMillis;
            this.startTimeInNano = startTimeInNano;
            this.endTimeInNano = endTimeInNano;
        }

        @Override
        public long getStartTimeInMillis() {
            return startTimeInMillis;
        }

        @Override
        public void setStartTimeInMillis(long startTimeInMillis) {
            this.startTimeInMillis = startTimeInMillis;
        }

        @Override
        public long getEndTimeInMillis() {
            return endTimeInMillis;
        }

        @Override
        public void setEndTimeInMillis(long endTimeInMillis) {
            this.endTimeInMillis = endTimeInMillis;
        }

        @Override
        public long getStartTimeInNano() {
            return startTimeInNano;
        }

        @Override
        public void setStartTimeInNano(final long startTimeInNano) {
            this.startTimeInNano = startTimeInNano;
        }

        @Override
        public long getEndTimeInNano() {
            return endTimeInNano;
        }

        @Override
        public void setEndTimeInNano(final long endTimeInNano) {
            this.endTimeInNano = endTimeInNano;
        }

        @Override
        public double getElapsedTimeInMillis() {
            return (endTimeInNano - startTimeInNano) / 1000000.0d; // convert to milliseconds.
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        protected String time2String(final long timeInMillis) {
            final Timestamp timestamp = DateUtil.createTimestamp(timeInMillis);
            return DateUtil.format(timestamp, DateUtil.LOCAL_TIMESTAMP_FORMAT); // + " " + N.LOCAL_TIME_ZONE.getID();
        }
    }

    /**
     * @author Haiyang Li
     * @version $Revision: 0.8 $
     */
    static class MethodStatistics extends AbstractStatistics {
        private final String methodName;
        private Object result;

        public MethodStatistics(final String methodName) {
            super();
            this.methodName = methodName;
        }

        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano) {
            this(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano, final Object result) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodName = methodName;
            this.result = result;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        public boolean isFailed() {
            return (result != null) && result instanceof Exception;
        }

        @Override
        public String toString() {
            if (isFailed()) {
                Exception e = (Exception) result;
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + ClassUtil.getSimpleClassName(e.getClass()) + ": " + e.getMessage() + ".";
            } else {
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + result + ".";
            }
        }
    }

    static class SingleLoopStatistics extends AbstractStatistics implements LoopStatistics {
        private List<MethodStatistics> methodStatisticsList;

        public SingleLoopStatistics() {
            super();
        }

        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final List<MethodStatistics> methodStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodStatisticsList = methodStatisticsList;
        }

        @Override
        public List<String> getMethodNameList() {
            List<String> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (!result.contains(methodStatistics.getMethodName())) {
                        result.add(methodStatistics.getMethodName());
                    }
                }
            }
            return result;
        }

        public List<MethodStatistics> getMethodStatisticsList() {
            if (methodStatisticsList == null) {
                methodStatisticsList = new ArrayList<>();
            }
            return methodStatisticsList;
        }

        public void setMethodStatisticsList(final List<MethodStatistics> methodStatisticsList) {
            this.methodStatisticsList = methodStatisticsList;
        }

        public void addMethodStatisticsList(final MethodStatistics methodStatistics) {
            getMethodStatisticsList().add(methodStatistics);
        }

        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result += methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        if (methodStatistics.getElapsedTimeInMillis() > result) {
                            result = methodStatistics.getElapsedTimeInMillis();
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        if (methodStatistics.getElapsedTimeInMillis() < result) {
                            result = methodStatistics.getElapsedTimeInMillis();
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        totalTime += methodStatistics.getElapsedTimeInMillis();
                        methodNum++;
                    }
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    result += methodStatistics.getElapsedTimeInMillis();
                }
            }
            return result;
        }

        @Override
        public int getMethodSize(final String methodName) {
            int methodSize = 0;
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        methodSize++;
                    }
                }
            }
            return methodSize;
        }

        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            List<MethodStatistics> result = new ArrayList<>(getMethodSize(methodName));
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed() && methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed()) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }
    }

    public static class MultiLoopsStatistics extends AbstractStatistics implements LoopStatistics {
        private static final String SEPARATOR_LINE = "========================================================================================================================";
        private final int threadNum;
        private List<LoopStatistics> loopStatisticsList;

        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, null);
        }

        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum, final List<LoopStatistics> loopStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.threadNum = threadNum;
            this.loopStatisticsList = loopStatisticsList;
        }

        public int getThreadNum() {
            return threadNum;
        }

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

        public List<LoopStatistics> getLoopStatisticsList() {
            if (loopStatisticsList == null) {
                loopStatisticsList = new ArrayList<>();
            }
            return loopStatisticsList;
        }

        public void setLoopStatisticsList(final List<LoopStatistics> loopStatisticsList) {
            this.loopStatisticsList = loopStatisticsList;
        }

        public void addMethodStatisticsList(final LoopStatistics loopStatistics) {
            getLoopStatisticsList().add(loopStatistics);
        }

        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    MethodStatistics methodStatistics = loopStatistics.getMaxElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    MethodStatistics methodStatistics = loopStatistics.getMinElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                }
            }
            return result;
        }

        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    double loopMethodMaxTime = loopStatistics.getMethodMaxElapsedTimeInMillis(methodName);
                    if (loopMethodMaxTime > result) {
                        result = loopMethodMaxTime;
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    double loopMethodMinTime = loopStatistics.getMethodMinElapsedTimeInMillis(methodName);
                    if (loopMethodMinTime < result) {
                        result = loopMethodMinTime;
                    }
                }
            }
            return result;
        }

        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    double loopMethodTotalTime = loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                    int loopMethodSize = loopStatistics.getMethodSize(methodName);
                    totalTime += loopMethodTotalTime;
                    methodNum += loopMethodSize;
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (loopStatisticsList != null) {
                for (int index = 0; index < loopStatisticsList.size(); index++) {
                    LoopStatistics loopStatistics = loopStatisticsList.get(index);
                    result += loopStatistics.getTotalElapsedTimeInMillis();
                }
            }
            return result;
        }

        @Override
        public int getMethodSize(final String methodName) {
            int result = 0;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodSize(methodName);
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            List<MethodStatistics> methodStatisticsList = new ArrayList<>(getMethodSize(methodName));
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    methodStatisticsList.addAll(loopStatistics.getMethodStatisticsList(methodName));
                }
            }
            return methodStatisticsList;
        }

        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getFailedMethodStatisticsList(methodName));
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getAllFailedMethodStatisticsList());
                }
            }
            return result;
        }

        private int getTotalCall() {
            int res = 0;
            if (loopStatisticsList != null) {
                for (LoopStatistics loopStatistics : loopStatisticsList) {
                    res += loopStatistics.getMethodNameList().size();
                }
            }
            return res;
        }

        public void printResult() {
            writeResult(new PrintWriter(System.out));
        }

        public void writeResult(final OutputStream os) {
            writeResult(new PrintWriter(os));
        }

        public void writeResult(final Writer writer) {
            writeResult(new PrintWriter(writer));
        }

        private void writeResult(final PrintWriter writer) {
            writer.println();
            writer.println(SEPARATOR_LINE);
            writer.println("(unit: milliseconds)");
            writer.println("threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum));
            writer.println("startTime: " + time2String(getStartTimeInMillis()));
            writer.println("endTime:   " + time2String(getEndTimeInMillis()));
            writer.println("totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()));
            writer.println();
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
            String methodNameTitil = "<method name>";
            List<String> methodNameList = getMethodNameList();
            int maxMethodNameLength = methodNameTitil.length();
            if (methodNameList.size() > 0) {
                for (String methodName : methodNameList) {
                    if (methodName.length() > maxMethodNameLength) {
                        maxMethodNameLength = methodName.length();
                    }
                }
            }
            writer.println();
            maxMethodNameLength += 3;
            writer.println(StringUtil.padEnd(methodNameTitil + ",  ", maxMethodNameLength)
                    + "|avg time|, |min time|, |max time|, |0.01% >=|, |0.1% >=|,  |1% >=|,    |10% >=|,   |20% >=|,   |50% >=|,   |80% >=|,   |90% >=|,   |99% >=|,   |99.9% >=|, |99.99% >=|");
            for (String methodName : methodNameList) {
                List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                int size = methodStatisticsList.size();
                Collections.sort(methodStatisticsList, new Comparator<MethodStatistics>() {
                    @Override
                    public int compare(final MethodStatistics o1, final MethodStatistics o2) {
                        return (o1.getElapsedTimeInMillis() == o2.getElapsedTimeInMillis()) ? 0
                                : ((o1.getElapsedTimeInMillis() > o2.getElapsedTimeInMillis()) ? (-1) : 1);
                    }
                });
                double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final int minLen = 12;
                writer.println(StringUtil.padEnd(methodName + ",  ", maxMethodNameLength) + StringUtil.padEnd(elapsedTimeFormat.format(avgTime) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(minTime) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(maxTime) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + StringUtil.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + ",  ",
                                minLen));
            }
            writer.println();
            writeError(writer);
            writer.println(SEPARATOR_LINE);
            writer.flush();
        }

        private void writeError(final PrintWriter writer) {
            MethodStatistics methodStatistics;
            List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                writer.println();
                writer.println("Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)");
                for (int index = 0; index < failedMethodList.size(); index++) {
                    writer.println("--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) failedMethodList.get(index);
                    writer.println(methodStatistics.toString());
                }
            }
        }

        public void writeHtmlResult(final OutputStream os) {
            writeHtmlResult(new PrintWriter(os));
        }

        public void writeHtmlResult(final Writer writer) {
            writeHtmlResult(new PrintWriter(writer));
        }

        private void writeHtmlResult(final PrintWriter writer) {
            writer.println(SEPARATOR_LINE);
            writer.println("<br/>" + "(unit: milliseconds)");
            writer.println("<br/>" + "threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum) + "");
            writer.println("<br/>" + "startTime: " + time2String(getStartTimeInMillis()) + "");
            writer.println("<br/>" + "endTime:   " + time2String(getEndTimeInMillis()) + "");
            writer.println("<br/>" + "totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()) + "");
            writer.println("<br/>");
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
            writer.println("<br/>");
            writer.println("<table width=\"1200\" border=\"1\">");
            writer.println("<tr>");
            writer.println("<th>method name</th>");
            writer.println("<th>avg time</th>");
            writer.println("<th>min time</th>");
            writer.println("<th>max time</th>");
            writer.println("<th>0.01% &gt;=</th>");
            writer.println("<th>0.1% &gt;=</th>");
            writer.println("<th>1% &gt;=</th>");
            writer.println("<th>10% &gt;=</th>");
            writer.println("<th>20% &gt;=</th>");
            writer.println("<th>50% &gt;=</th>");
            writer.println("<th>80% &gt;=</th>");
            writer.println("<th>90% &gt;=</th>");
            writer.println("<th>99% &gt;=</th>");
            writer.println("<th>99.9% &gt;=</th>");
            writer.println("<th>99.99% &gt;=</th>");
            writer.println("</tr>");
            List<String> methodNameList = getMethodNameList();
            for (String methodName : methodNameList) {
                List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                int size = methodStatisticsList.size();
                Collections.sort(methodStatisticsList, new Comparator<MethodStatistics>() {
                    @Override
                    public int compare(final MethodStatistics o1, final MethodStatistics o2) {
                        return (o1.getElapsedTimeInMillis() == o2.getElapsedTimeInMillis()) ? 0
                                : ((o1.getElapsedTimeInMillis() > o2.getElapsedTimeInMillis()) ? (-1) : 1);
                    }
                });
                double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                writer.println("<tr>");
                writer.println("<td>" + methodName + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(avgTime) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(minTime) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(maxTime) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</td>");
                writer.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            writeHtmlError(writer);
            writer.println(SEPARATOR_LINE);
            writer.flush();
        }

        private void writeHtmlError(final PrintWriter writer) {
            MethodStatistics methodStatistics;
            List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                writer.println("<h4>Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</h4>");
                for (int index = 0; index < failedMethodList.size(); index++) {
                    writer.println("<br/>" + "--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) failedMethodList.get(index);
                    writer.println("<br/>" + methodStatistics.toString());
                }
            }
        }

        public void writeXmlResult(final OutputStream os) {
            writeXmlResult(new PrintWriter(os));
        }

        public void writeXmlResult(final Writer writer) {
            writeXmlResult(new PrintWriter(writer));
        }

        private void writeXmlResult(final PrintWriter writer) {
            writer.println("<result>");
            writer.println("<unit>milliseconds</unit>");
            writer.println("<threadNum>" + threadNum + "</threadNum>");
            writer.println("<loops>" + (loopStatisticsList.size() / threadNum) + "</loops>");
            writer.println("<startTime>" + time2String(getStartTimeInMillis()) + "</startTime>");
            writer.println("<endTime>" + time2String(getEndTimeInMillis()) + "</endTime>");
            writer.println("<totalElapsedTime>" + elapsedTimeFormat.format(getElapsedTimeInMillis()) + "</totalElapsedTime>");
            writer.println();
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
            List<String> methodNameList = getMethodNameList();
            for (String methodName : methodNameList) {
                List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                int size = methodStatisticsList.size();
                Collections.sort(methodStatisticsList, new Comparator<MethodStatistics>() {
                    @Override
                    public int compare(final MethodStatistics o1, final MethodStatistics o2) {
                        return (o1.getElapsedTimeInMillis() == o2.getElapsedTimeInMillis()) ? 0
                                : ((o1.getElapsedTimeInMillis() > o2.getElapsedTimeInMillis()) ? (-1) : 1);
                    }
                });
                double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                writer.println("<method name=\"" + methodName + "\">");
                writer.println("<avgTime>" + elapsedTimeFormat.format(avgTime) + "</avgTime>");
                writer.println("<minTime>" + elapsedTimeFormat.format(minTime) + "</minTime>");
                writer.println("<maxTime>" + elapsedTimeFormat.format(maxTime) + "</maxTime>");
                writer.println("<_0.0001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</_0.0001>");
                writer.println("<_0.001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</_0.001>");
                writer.println("<_0.01>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</_0.01>");
                writer.println("<_0.2>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</_0.2>");
                writer.println("<_0.5>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</_0.5>");
                writer.println("<_0.8>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</_0.8>");
                writer.println("<_0.9>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</_0.9>");
                writer.println("<_0.99>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</_0.99>");
                writer.println("<_0.999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</_0.999>");
                writer.println("<_0.9999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</_0.9999>");
                writer.println("</method>");
            }
            List<MethodStatistics> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                writer.println("<errors>" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</errors>");
                for (MethodStatistics methodStatistics : failedMethodList) {
                    writer.println("<error>" + methodStatistics.toString() + "</error>");
                }
            }
            writer.println("</result>");
            writer.flush();
        }
    }
}
