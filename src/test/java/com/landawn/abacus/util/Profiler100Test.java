package com.landawn.abacus.util;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Profiler100Test extends TestBase {

    private void doWork() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
    }

    private boolean isProfilerSuspended;

    @BeforeEach
    public void setUp() {
        Profiler.suspend(false);
        isProfilerSuspended = Profiler.isSuspended();
    }

    @AfterEach
    public void tearDown() {
        Profiler.suspend(isProfilerSuspended);
    }

    @Test
    public void testRunWithRunnable() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, () -> doWork());

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1, stats.getThreadNum());
        Assertions.assertTrue(stats.getElapsedTimeInMillis() > 0);
        Assertions.assertEquals(1, stats.getMethodNameList().size());
        Assertions.assertEquals("run", stats.getMethodNameList().get(0));
    }

    @Test
    public void testRunWithRunnableAndLabel() {
        String label = "testMethod";
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, label, () -> doWork());

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1, stats.getThreadNum());
        Assertions.assertTrue(stats.getElapsedTimeInMillis() > 0);
        Assertions.assertEquals(1, stats.getMethodNameList().size());
        Assertions.assertEquals(label, stats.getMethodNameList().get(0));
    }

    @Test
    public void testRunWithAllParameters() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 10, 5, 5, 1, "fullTest", () -> doWork());

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(2, stats.getThreadNum());
        Assertions.assertTrue(stats.getElapsedTimeInMillis() > 0);
        Assertions.assertEquals("fullTest", stats.getMethodNameList().get(0));
    }

    @Test
    public void testRunMultiThreaded() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(3, 10, 1, "multiThread", () -> doWork());

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(3, stats.getThreadNum());
        Assertions.assertTrue(stats.getLoopStatisticsList().size() > 0);

        int totalMethodCalls = 0;
        for (Profiler.LoopStatistics loopStats : stats.getLoopStatisticsList()) {
            totalMethodCalls += loopStats.getMethodSize("multiThread");
        }
        Assertions.assertEquals(30, totalMethodCalls);
    }

    @Test
    public void testRunMultipleRounds() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 3, "multiRound", () -> doWork());

        Assertions.assertNotNull(stats);
        Assertions.assertTrue(stats.getElapsedTimeInMillis() > 0);
    }

    @Test
    public void testRunInvalidThreadNum() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(0, 10, 1, () -> {
        }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(-1, 10, 1, () -> {
        }));
    }

    @Test
    public void testRunInvalidLoopNum() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, 0, 1, () -> {
        }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, -1, 1, () -> {
        }));
    }

    @Test
    public void testRunInvalidDelays() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, -1, 10, 0, 1, "test", () -> {
        }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, 0, 10, -1, 1, "test", () -> {
        }));
    }

    @Test
    public void testSuspend() {
        boolean originalSuspended = false;

        try {
            Profiler.suspend(true);

            Profiler.MultiLoopsStatistics stats = Profiler.run(5, 100, 3, "suspended", () -> doWork());
            Assertions.assertNotNull(stats);

            Profiler.suspend(false);

            stats = Profiler.run(2, 5, 1, "normal", () -> doWork());
            Assertions.assertNotNull(stats);
        } finally {
            Profiler.suspend(originalSuspended);
        }
    }

    @Test
    public void testPrintResult() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;

        try {
            System.setOut(new java.io.PrintStream(baos));

            Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "printTest", () -> doWork());
            stats.printResult();

            String output = baos.toString();
            Assertions.assertTrue(output.contains("printTest"));
            Assertions.assertTrue(output.contains("threadNum=1"));
            Assertions.assertTrue(output.contains("loops=5"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testWriteResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "writeTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("writeTest"));
        Assertions.assertTrue(output.contains("avg time"));

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("writeTest"));
        Assertions.assertTrue(output.contains("avg time"));
    }

    @Test
    public void testWriteHtmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeHtmlResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("<table"));
        Assertions.assertTrue(output.contains("htmlTest"));
        Assertions.assertTrue(output.contains("<th>avg time</th>"));

        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("<table"));
        Assertions.assertTrue(output.contains("htmlTest"));
    }

    @Test
    public void testWriteXmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeXmlResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("<result>"));
        Assertions.assertTrue(output.contains("<method name=\"xmlTest\">"));
        Assertions.assertTrue(output.contains("<avgTime>"));

        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("<result>"));
        Assertions.assertTrue(output.contains("xmlTest"));
    }

    @Test
    public void testStatisticsCalculations() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 10, 1, "statsTest", () -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        });

        Assertions.assertTrue(stats.getMethodTotalElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getMethodAverageElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") >= 0);
        Assertions.assertTrue(stats.getMethodMaxElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getTotalElapsedTimeInMillis() > 0);

        Assertions.assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        double avg = stats.getMethodAverageElapsedTimeInMillis("statsTest");
        Assertions.assertTrue(avg >= stats.getMethodMinElapsedTimeInMillis("statsTest"));
        Assertions.assertTrue(avg <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        Assertions.assertEquals(20, stats.getMethodSize("statsTest"));

        Assertions.assertEquals(20, stats.getMethodStatisticsList("statsTest").size());

        Profiler.MethodStatistics minMethod = stats.getMinElapsedTimeMethod();
        Profiler.MethodStatistics maxMethod = stats.getMaxElapsedTimeMethod();
        Assertions.assertNotNull(minMethod);
        Assertions.assertNotNull(maxMethod);
        Assertions.assertEquals("statsTest", minMethod.getMethodName());
        Assertions.assertEquals("statsTest", maxMethod.getMethodName());
    }

    @Test
    public void testFailedMethodStatistics() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 10, 1, "errorTest", () -> {
            counter[0]++;
            if (counter[0] % 3 == 0) {
                throw new RuntimeException("Test exception");
            }
        });

        Assertions.assertTrue(stats.getFailedMethodStatisticsList("errorTest").size() > 0);
        Assertions.assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();
        Assertions.assertTrue(output.contains("Errors:"));
    }

    @Test
    public void testMethodStatistics() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("testMethod");
        Assertions.assertEquals("testMethod", stats.getMethodName());

        long startMillis = System.currentTimeMillis();
        long startNano = System.nanoTime();
        long endNano = System.nanoTime() + 1000000;
        long endMillis = System.currentTimeMillis() + 1;

        Profiler.MethodStatistics timedStats = new Profiler.MethodStatistics("timedMethod", startMillis, endMillis, startNano, endNano);

        Assertions.assertEquals("timedMethod", timedStats.getMethodName());
        Assertions.assertTrue(timedStats.getElapsedTimeInMillis() > 0);

        Object result = "Success";
        Profiler.MethodStatistics withResult = new Profiler.MethodStatistics("withResult", startMillis, endMillis, startNano, endNano, result);

        Assertions.assertEquals(result, withResult.getResult());
        Assertions.assertFalse(withResult.isFailed());

        Exception error = new RuntimeException("Test error");
        Profiler.MethodStatistics withError = new Profiler.MethodStatistics("withError", startMillis, endMillis, startNano, endNano, error);

        Assertions.assertEquals(error, withError.getResult());
        Assertions.assertTrue(withError.isFailed());

        String str = withResult.toString();
        Assertions.assertTrue(str.contains("withResult"));
        Assertions.assertTrue(str.contains("Success"));

        String errorStr = withError.toString();
        Assertions.assertTrue(errorStr.contains("RuntimeException"));
        Assertions.assertTrue(errorStr.contains("Test error"));
    }

    @Test
    public void testMethodStatisticsSettersGetters() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");

        long startMillis = 1000L;
        long endMillis = 2000L;
        long startNano = 1000000000L;
        long endNano = 2000000000L;
        Object result = "Result";

        stats.setStartTimeInMillis(startMillis);
        stats.setEndTimeInMillis(endMillis);
        stats.setStartTimeInNano(startNano);
        stats.setEndTimeInNano(endNano);
        stats.setResult(result);

        Assertions.assertEquals(startMillis, stats.getStartTimeInMillis());
        Assertions.assertEquals(endMillis, stats.getEndTimeInMillis());
        Assertions.assertEquals(startNano, stats.getStartTimeInNano());
        Assertions.assertEquals(endNano, stats.getEndTimeInNano());
        Assertions.assertEquals(result, stats.getResult());
        Assertions.assertEquals(1000.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testSingleThreadSingleLoop() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "single", () -> {
        });

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1, stats.getThreadNum());
        Assertions.assertEquals(1, stats.getMethodSize("single"));
        Assertions.assertEquals(1, stats.getLoopStatisticsList().size());
    }

    @Test
    public void testEmptyMethodName() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "", () -> {
        });

        Assertions.assertNotNull(stats);
        Assertions.assertEquals(1, stats.getMethodNameList().size());
        Assertions.assertEquals("", stats.getMethodNameList().get(0));
    }

    @Test
    public void testPercentileCalculations() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 100, 1, "percentileTest", () -> {
            try {
                Thread.sleep((long) (Math.random() * 5));
            } catch (InterruptedException e) {
            }
        });

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();

        Assertions.assertTrue(output.contains("0.01% >="));
        Assertions.assertTrue(output.contains("0.1% >="));
        Assertions.assertTrue(output.contains("1% >="));
        Assertions.assertTrue(output.contains("10% >="));
        Assertions.assertTrue(output.contains("50% >="));
        Assertions.assertTrue(output.contains("90% >="));
        Assertions.assertTrue(output.contains("99% >="));
        Assertions.assertTrue(output.contains("99.9% >="));
        Assertions.assertTrue(output.contains("99.99% >="));
    }
}
