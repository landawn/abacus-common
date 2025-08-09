package com.landawn.abacus.util;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Profiler100Test extends TestBase {

    // Test helper method to simulate work
    private void doWork() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean isProfilerSuspended;

    @BeforeEach
    public void setUp() {
        // Reset the profiler before each test
        Profiler.suspend(false);
        isProfilerSuspended = Profiler.isSuspended();
    }

    @AfterEach
    public void tearDown() {
        // Ensure profiler is not suspended after tests
        Profiler.suspend(isProfilerSuspended);
    }

    // Tests for run with Runnable
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

        // Should have executed 30 times total (3 threads * 10 loops)
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
        // Only the last round's statistics are returned
        Assertions.assertTrue(stats.getElapsedTimeInMillis() > 0);
    }

    // Tests for invalid arguments
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

    // Tests for suspend
    @Test
    public void testSuspend() {
        // Store original state
        boolean originalSuspended = false;

        try {
            // Enable suspension
            Profiler.suspend(true);

            // When suspended, profiler should execute only 1 loop with 1 thread
            Profiler.MultiLoopsStatistics stats = Profiler.run(5, 100, 3, "suspended", () -> doWork());
            Assertions.assertNotNull(stats);

            // Re-enable normal operation
            Profiler.suspend(false);

            // Should work normally now
            stats = Profiler.run(2, 5, 1, "normal", () -> doWork());
            Assertions.assertNotNull(stats);
        } finally {
            // Restore original state
            Profiler.suspend(originalSuspended);
        }
    }

    // Tests for result output methods
    @Test
    public void testPrintResult() {
        // Redirect System.out to capture output
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

        // Test writing to OutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("writeTest"));
        Assertions.assertTrue(output.contains("avg time"));

        // Test writing to Writer
        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("writeTest"));
        Assertions.assertTrue(output.contains("avg time"));
    }

    @Test
    public void testWriteHtmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlTest", () -> doWork());

        // Test writing HTML to OutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeHtmlResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("<table"));
        Assertions.assertTrue(output.contains("htmlTest"));
        Assertions.assertTrue(output.contains("<th>avg time</th>"));

        // Test writing HTML to Writer
        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("<table"));
        Assertions.assertTrue(output.contains("htmlTest"));
    }

    @Test
    public void testWriteXmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlTest", () -> doWork());

        // Test writing XML to OutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeXmlResult(baos);
        String output = baos.toString();
        Assertions.assertTrue(output.contains("<result>"));
        Assertions.assertTrue(output.contains("<method name=\"xmlTest\">"));
        Assertions.assertTrue(output.contains("<avgTime>"));

        // Test writing XML to Writer
        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        output = sw.toString();
        Assertions.assertTrue(output.contains("<result>"));
        Assertions.assertTrue(output.contains("xmlTest"));
    }

    // Test statistics calculation methods
    @Test
    public void testStatisticsCalculations() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 10, 1, "statsTest", () -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                // ignore
            }
        });

        // Test various statistics methods
        Assertions.assertTrue(stats.getMethodTotalElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getMethodAverageElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") >= 0);
        Assertions.assertTrue(stats.getMethodMaxElapsedTimeInMillis("statsTest") > 0);
        Assertions.assertTrue(stats.getTotalElapsedTimeInMillis() > 0);

        // Min should be less than or equal to max
        Assertions.assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        // Average should be between min and max
        double avg = stats.getMethodAverageElapsedTimeInMillis("statsTest");
        Assertions.assertTrue(avg >= stats.getMethodMinElapsedTimeInMillis("statsTest"));
        Assertions.assertTrue(avg <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        // Test method size
        Assertions.assertEquals(20, stats.getMethodSize("statsTest")); // 2 threads * 10 loops

        // Test getMethodStatisticsList
        Assertions.assertEquals(20, stats.getMethodStatisticsList("statsTest").size());

        // Test min/max elapsed time methods
        Profiler.MethodStatistics minMethod = stats.getMinElapsedTimeMethod();
        Profiler.MethodStatistics maxMethod = stats.getMaxElapsedTimeMethod();
        Assertions.assertNotNull(minMethod);
        Assertions.assertNotNull(maxMethod);
        Assertions.assertEquals("statsTest", minMethod.getMethodName());
        Assertions.assertEquals("statsTest", maxMethod.getMethodName());
    }

    @Test
    public void testFailedMethodStatistics() {
        // Create a test that throws exceptions sometimes
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 10, 1, "errorTest", () -> {
            counter[0]++;
            if (counter[0] % 3 == 0) {
                throw new RuntimeException("Test exception");
            }
        });

        // Should have some failed executions
        Assertions.assertTrue(stats.getFailedMethodStatisticsList("errorTest").size() > 0);
        Assertions.assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);

        // Check that failed methods are reported in output
        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();
        Assertions.assertTrue(output.contains("Errors:"));
    }

    // Test MethodStatistics class
    @Test
    public void testMethodStatistics() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("testMethod");
        Assertions.assertEquals("testMethod", stats.getMethodName());

        // Test with timing
        long startMillis = System.currentTimeMillis();
        long startNano = System.nanoTime();
        long endNano = System.nanoTime() + 1000000; // 1ms later
        long endMillis = System.currentTimeMillis() + 1;

        Profiler.MethodStatistics timedStats = new Profiler.MethodStatistics("timedMethod", startMillis, endMillis, startNano, endNano);

        Assertions.assertEquals("timedMethod", timedStats.getMethodName());
        Assertions.assertTrue(timedStats.getElapsedTimeInMillis() > 0);

        // Test with result
        Object result = "Success";
        Profiler.MethodStatistics withResult = new Profiler.MethodStatistics("withResult", startMillis, endMillis, startNano, endNano, result);

        Assertions.assertEquals(result, withResult.getResult());
        Assertions.assertFalse(withResult.isFailed());

        // Test with exception
        Exception error = new RuntimeException("Test error");
        Profiler.MethodStatistics withError = new Profiler.MethodStatistics("withError", startMillis, endMillis, startNano, endNano, error);

        Assertions.assertEquals(error, withError.getResult());
        Assertions.assertTrue(withError.isFailed());

        // Test toString
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

        // Test setters and getters
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

    // Test edge cases
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
        // Run enough iterations to get meaningful percentile data
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 100, 1, "percentileTest", () -> {
            try {
                // Add some variability in execution time
                Thread.sleep((long) (Math.random() * 5));
            } catch (InterruptedException e) {
                // ignore
            }
        });

        // Check that the result output includes percentile data
        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();

        // Should contain percentile headers
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