package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("slow-test")
public class ProfilerTest extends AbstractTest {

    static {
        Profiler.suspend(true);
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

    private void doWork() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    String normal() throws Exception {
        Thread.sleep(10);

        return null;
    }

    Object error(Object obj) {
        throw new RuntimeException(CommonUtil.toString(obj));
    }

    // ============================================================
    // SingleLoopStatistics tests
    // ============================================================

    private Profiler.MethodStatistics makeMethodStats(String name, long elapsedMs, boolean failed) {
        long now = System.currentTimeMillis();
        long startNano = System.nanoTime();
        Object result = failed ? new RuntimeException("failed") : null;
        return new Profiler.MethodStatistics(name, now - elapsedMs, now, startNano - elapsedMs * 1_000_000L, startNano, result);
    }

    @Test
    public void testRunWithRunnable_DefaultLabel() {
        // Verifies default label is "run" when no label is specified
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, () -> {
        });
        assertEquals("run", stats.getMethodNameList().get(0));
        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        assertTrue(sw.toString().contains("run"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getStartTimeInMillis/EndTime/Nano setters/getters
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetStartEndTime() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "timeTest", () -> {
        });

        assertTrue(stats.getStartTimeInMillis() > 0);
        assertTrue(stats.getEndTimeInMillis() >= stats.getStartTimeInMillis());
        assertTrue(stats.getStartTimeInNano() > 0);
        assertTrue(stats.getEndTimeInNano() >= stats.getStartTimeInNano());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.printResult() — source line ~1924
    // ============================================================

    @Test
    public void testPrintResult() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;

        try {
            System.setOut(new java.io.PrintStream(baos));

            Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "printTest", () -> doWork());
            stats.printResult();

            String output = baos.toString();
            assertTrue(output.contains("printTest"));
            assertTrue(output.contains("threadNum=1"));
            assertTrue(output.contains("loops=5"));
        } finally {
            System.setOut(originalOut);
        }
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeResult(OutputStream) — source line ~1961
    // ============================================================

    @Test
    public void testWriteResultOutputStream() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "osTest", () -> {
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("osTest"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeResult(Writer) — source line ~2044
    // ============================================================

    @Test
    public void testWriteResultWriter() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "writerTest", () -> {
        });

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("writerTest"));
    }

    @Test
    public void testWriteResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "writeTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("writeTest"));
        assertTrue(output.contains("avg time"));

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        output = sw.toString();
        assertTrue(output.contains("writeTest"));
        assertTrue(output.contains("avg time"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeHtmlResult(OutputStream) — source line ~2160
    // ============================================================

    @Test
    public void testWriteHtmlResultOutputStream() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlOsTest", () -> {
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeHtmlResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("<table"));
        assertTrue(output.contains("htmlOsTest"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeHtmlResult(Writer) — source line ~2254
    // ============================================================

    @Test
    public void testWriteHtmlResultWriter() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlWriterTest", () -> {
        });

        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("<table"));
        assertTrue(output.contains("htmlWriterTest"));
    }

    @Test
    public void testWriteHtmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeHtmlResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("<table"));
        assertTrue(output.contains("htmlTest"));
        assertTrue(output.contains("<th>avg time</th>"));

        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        output = sw.toString();
        assertTrue(output.contains("<table"));
        assertTrue(output.contains("htmlTest"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeXmlResult(OutputStream) — source line ~2374
    // ============================================================

    @Test
    public void testWriteXmlResultOutputStream() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlOsTest", () -> {
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeXmlResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("<result>"));
        assertTrue(output.contains("xmlOsTest"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.writeXmlResult(Writer) — source line ~2504
    // ============================================================

    @Test
    public void testWriteXmlResultWriter() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlWriterTest", () -> {
        });

        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("<result>"));
        assertTrue(output.contains("xmlWriterTest"));
    }

    @Test
    public void testWriteXmlResult() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlTest", () -> doWork());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stats.writeXmlResult(baos);
        String output = baos.toString();
        assertTrue(output.contains("<result>"));
        assertTrue(output.contains("<method name=\"xmlTest\">"));
        assertTrue(output.contains("<avgTime>"));

        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        output = sw.toString();
        assertTrue(output.contains("<result>"));
        assertTrue(output.contains("xmlTest"));
    }

    @Test
    public void testLoopStatistics_GetMethodTotalElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopTotal", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double total = loopStats.getMethodTotalElapsedTimeInMillis("loopTotal");
        assertTrue(total >= 0);
    }

    @Test
    public void testLoopStatistics_GetMethodAverageElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopAvg", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double avg = loopStats.getMethodAverageElapsedTimeInMillis("loopAvg");
        assertTrue(avg >= 0);
    }

    @Test
    public void testLoopStatistics_GetTotalElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopTotalElapsed", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double total = loopStats.getTotalElapsedTimeInMillis();
        assertTrue(total >= 0);
    }

    @Test
    public void testLoopStatistics_GetMethodSize() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopSize", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        assertEquals(1, loopStats.getMethodSize("loopSize"));
    }

    @Test
    public void testLoopStatistics_GetElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopElapsed", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        assertTrue(loopStats.getElapsedTimeInMillis() >= 0);
    }

    @Test
    public void testLoopStatistics_GetStartEndTime() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopTime", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        assertTrue(loopStats.getStartTimeInMillis() > 0);
        assertTrue(loopStats.getEndTimeInMillis() >= loopStats.getStartTimeInMillis());
        assertTrue(loopStats.getStartTimeInNano() > 0);
        assertTrue(loopStats.getEndTimeInNano() >= loopStats.getStartTimeInNano());
    }

    @Test
    public void testPercentileCalculations_XmlFormat() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 100, 1, "percentileXml", () -> {
        });

        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        String output = sw.toString();

        assertTrue(output.contains("<_0.0001>"));
        assertTrue(output.contains("<_0.001>"));
        assertTrue(output.contains("<_0.01>"));
        assertTrue(output.contains("<_0.5>"));
        assertTrue(output.contains("<_0.9>"));
        assertTrue(output.contains("<_0.99>"));
        assertTrue(output.contains("<_0.999>"));
        assertTrue(output.contains("<_0.9999>"));
    }

    // ============================================================
    // Tests for Profiler.run(int, int, int, Runnable) — source line ~300
    // ============================================================

    @Test
    public void testRunWithRunnable() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, () -> doWork());

        assertNotNull(stats);
        assertEquals(1, stats.getThreadNum());
        assertTrue(stats.getElapsedTimeInMillis() > 0);
        assertEquals(1, stats.getMethodNameList().size());
        assertEquals("run", stats.getMethodNameList().get(0));
    }

    // ============================================================
    // Tests for Profiler.run(int, int, int, String, Runnable) — source line ~375
    // ============================================================

    @Test
    public void testRunWithRunnableAndLabel() {
        String label = "testMethod";
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, label, () -> doWork());

        assertNotNull(stats);
        assertEquals(1, stats.getThreadNum());
        assertTrue(stats.getElapsedTimeInMillis() > 0);
        assertEquals(1, stats.getMethodNameList().size());
        assertEquals(label, stats.getMethodNameList().get(0));
    }

    @Test
    public void testRunWithRunnableAndLabel_NullLabel() {
        // label can be null; method should still execute
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 3, 1, (String) null, () -> {
        });
        assertNotNull(stats);
    }

    // ============================================================
    // Tests for Profiler.run(int, long, int, long, int, String, Runnable) — source line ~480
    // ============================================================

    @Test
    public void testRunWithAllParameters() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 10, 5, 5, 1, "fullTest", () -> doWork());

        assertNotNull(stats);
        assertEquals(2, stats.getThreadNum());
        assertTrue(stats.getElapsedTimeInMillis() > 0);
        assertEquals("fullTest", stats.getMethodNameList().get(0));
    }

    @Test
    public void testRunWithAllParameters_ZeroDelays() {
        // threadDelay=0, loopDelay=0 should work fine
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 0, 3, 0, 1, "noDelay", () -> {
        });
        assertNotNull(stats);
        assertEquals(1, stats.getThreadNum());
    }

    // ============================================================
    // Tests for Profiler.run(Object, String, int, int, int) — source line ~495
    // ============================================================

    @Test
    public void testRunWithMethodName() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, "normal", 1, 2, 1);
        assertNotNull(stats);
        assertTrue(stats.getElapsedTimeInMillis() > 0);
    }

    // ============================================================
    // Tests for Profiler.run(Object, Method, int, int, int) — source line ~509
    // ============================================================

    @Test
    public void testRunWithMethodObject() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, 1, 2, 1);
        assertNotNull(stats);
    }

    @Test
    public void testRunWithMethodObject_MultipleRounds() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, 1, 2, 2);
        assertNotNull(stats);
    }

    @Test
    public void testRunWithMethodAndSingleArg_NullArg() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, (Object) null, 1, 2, 1);
        assertNotNull(stats);
    }

    // ============================================================
    // Tests for Profiler.run(Object, Method, Object, int, long, int, long, int) — source line ~541
    // ============================================================

    @Test
    public void testRunWithMethodAndDelays() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, (Object) null, 1, 0, 2, 0, 1);
        assertNotNull(stats);
    }

    // ============================================================
    // Tests for Profiler.run(Object, Method, List, int, int, int) — source line ~557
    // ============================================================

    @Test
    public void test_normal() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).writeHtmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeXmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeResult(System.out);
        assertEquals(String.class, method.getReturnType());

        Profiler.run(this, "normal", 2, 2, 1).printResult();
        Profiler.run(this, "normal", 2, 2, 1).printResult();
    }

    @Test
    public void testRunWithMethodReflection() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, null, 1, 2, 1);
        assertNotNull(stats);
        assertTrue(stats.getElapsedTimeInMillis() > 0);
    }

    @Test
    public void testRunWithMethodReflection_EmptyArgsList() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, new ArrayList<>(), 1, 2, 1);
        assertNotNull(stats);
    }

    // ============================================================
    // Tests for Profiler.run(Object, Method, List, Method...setup/teardown, int, long, int, long, int) — source line ~580
    // ============================================================

    @Test
    public void testRunWithSetupAndTeardown() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        // Using null for setup/teardown methods
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, null, null, null, null, null, 1, 0, 2, 0, 1);
        assertNotNull(stats);
    }

    @Test
    public void testRunWithSetupAndTeardown_MultipleRounds() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, null, null, null, null, null, 1, 0, 2, 0, 2);
        assertNotNull(stats);
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodNameList_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "nameListTest", () -> {
        });
        List<String> names = stats.getMethodNameList();
        assertEquals(1, names.size());
        assertEquals("nameListTest", names.get(0));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getLoopStatisticsList() / setLoopStatisticsList() — source line ~1637
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsSetLoopStatisticsList() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "setListTest", () -> doWork());
        List<Profiler.LoopStatistics> originalList = stats.getLoopStatisticsList();
        assertNotNull(originalList);

        List<Profiler.LoopStatistics> newList = new ArrayList<>();
        stats.setLoopStatisticsList(newList);
        assertSame(newList, stats.getLoopStatisticsList());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.addMethodStatisticsList(LoopStatistics) — source line ~1658
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsAddMethodStatisticsList() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 1, 0, 1000000, 1);
        assertTrue(stats.getLoopStatisticsList().isEmpty());

        Profiler.MultiLoopsStatistics fromRun = Profiler.run(1, 5, 1, "addTest", () -> {
        });
        Profiler.LoopStatistics loopStats = fromRun.getLoopStatisticsList().get(0);
        stats.addMethodStatisticsList(loopStats);

        assertEquals(1, stats.getLoopStatisticsList().size());
    }

    @Test
    public void testMultiLoopsStatisticsAddMethodStatisticsList_Multiple() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 1, 0, 1000000, 1);
        Profiler.MultiLoopsStatistics fromRun = Profiler.run(1, 5, 1, "addMultiTest", () -> {
        });

        for (Profiler.LoopStatistics ls : fromRun.getLoopStatisticsList()) {
            stats.addMethodStatisticsList(ls);
        }
        assertEquals(fromRun.getLoopStatisticsList().size(), stats.getLoopStatisticsList().size());
    }

    @Test
    public void testMultiLoopsStatisticsGetMaxElapsedTimeMethod_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "maxTimeTest", () -> doWork());
        Profiler.MethodStatistics maxMethod = stats.getMaxElapsedTimeMethod();
        assertNotNull(maxMethod);
        assertEquals("maxTimeTest", maxMethod.getMethodName());
        assertTrue(maxMethod.getElapsedTimeInMillis() > 0);
    }

    @Test
    public void testMultiLoopsStatisticsGetMinElapsedTimeMethod_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "minTimeTest", () -> doWork());
        Profiler.MethodStatistics minMethod = stats.getMinElapsedTimeMethod();
        assertNotNull(minMethod);
        assertEquals("minTimeTest", minMethod.getMethodName());
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodTotalElapsedTime_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "totalTimeTest", () -> doWork());
        double total = stats.getMethodTotalElapsedTimeInMillis("totalTimeTest");
        assertTrue(total > 0);
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodMaxElapsedTime_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "maxElapsed", () -> doWork());
        double maxTime = stats.getMethodMaxElapsedTimeInMillis("maxElapsed");
        assertTrue(maxTime > 0);
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodMinElapsedTime_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "minElapsed", () -> doWork());
        double minTime = stats.getMethodMinElapsedTimeInMillis("minElapsed");
        assertTrue(minTime >= 0);
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodAverageElapsedTime_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "avgElapsed", () -> doWork());
        double avg = stats.getMethodAverageElapsedTimeInMillis("avgElapsed");
        assertTrue(avg > 0);
        // Average should be between min and max
        double min = stats.getMethodMinElapsedTimeInMillis("avgElapsed");
        double max = stats.getMethodMaxElapsedTimeInMillis("avgElapsed");
        assertTrue(avg >= min);
        assertTrue(avg <= max);
    }

    @Test
    public void testMultiLoopsStatisticsGetTotalElapsedTime_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "totalTest", () -> doWork());
        assertTrue(stats.getTotalElapsedTimeInMillis() > 0);
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodSize_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 5, 1, "sizeTest", () -> {
        });
        assertEquals(10, stats.getMethodSize("sizeTest")); // 2 threads * 5 loops
    }

    @Test
    public void testMultiLoopsStatisticsGetMethodStatisticsList_NonEmpty() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "statsList", () -> {
        });
        List<Profiler.MethodStatistics> list = stats.getMethodStatisticsList("statsList");
        assertEquals(5, list.size());
        for (Profiler.MethodStatistics ms : list) {
            assertEquals("statsList", ms.getMethodName());
        }
    }

    @Test
    public void testWriteResult_ContainsTimingInfo() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "timingInfo", () -> {
        });
        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("(unit: milliseconds)"));
        assertTrue(output.contains("startTime:"));
        assertTrue(output.contains("endTime:"));
        assertTrue(output.contains("totalElapsedTime:"));
    }

    @Test
    public void testWriteHtmlResult_ContainsAllPercentileHeaders() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "htmlPercentile", () -> {
        });
        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("<th>method name</th>"));
        assertTrue(output.contains("<th>avg time</th>"));
        assertTrue(output.contains("<th>min time</th>"));
        assertTrue(output.contains("<th>max time</th>"));
        assertTrue(output.contains("<th>0.01%"));
        assertTrue(output.contains("<th>99.99%"));
    }

    @Test
    public void testWriteXmlResult_ContainsAllElements() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "xmlElements", () -> {
        });
        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("<unit>milliseconds</unit>"));
        assertTrue(output.contains("<threadNum>"));
        assertTrue(output.contains("<loops>"));
        assertTrue(output.contains("<startTime>"));
        assertTrue(output.contains("<endTime>"));
        assertTrue(output.contains("<totalElapsedTime>"));
        assertTrue(output.contains("<avgTime>"));
        assertTrue(output.contains("<minTime>"));
        assertTrue(output.contains("<maxTime>"));
        assertTrue(output.contains("</result>"));
    }

    // ============================================================
    // Multi-threaded and multi-round integration tests
    // ============================================================

    @Test
    public void testRunMultiThreaded() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(3, 10, 1, "multiThread", () -> doWork());

        assertNotNull(stats);
        assertEquals(3, stats.getThreadNum());
        assertTrue(stats.getLoopStatisticsList().size() > 0);

        int totalMethodCalls = 0;
        for (Profiler.LoopStatistics loopStats : stats.getLoopStatisticsList()) {
            totalMethodCalls += loopStats.getMethodSize("multiThread");
        }
        assertEquals(30, totalMethodCalls);
    }

    @Test
    public void testRunMultipleRounds() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 3, "multiRound", () -> doWork());

        assertNotNull(stats);
        assertTrue(stats.getElapsedTimeInMillis() > 0);
    }

    @Test
    public void testRunWithMultipleRounds() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 2, "multiRound2", () -> {
        });
        assertNotNull(stats);
        assertEquals(1, stats.getMethodNameList().size());
    }

    @Test
    public void testSingleThreadSingleLoop() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "single", () -> {
        });

        assertNotNull(stats);
        assertEquals(1, stats.getThreadNum());
        assertEquals(1, stats.getMethodSize("single"));
        assertEquals(1, stats.getLoopStatisticsList().size());
    }

    @Test
    public void testEmptyMethodName() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 1, 1, "", () -> {
        });

        assertNotNull(stats);
        assertEquals(1, stats.getMethodNameList().size());
        assertEquals("", stats.getMethodNameList().get(0));
    }

    @Test
    public void testGetLoopStatisticsList() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 5, 1, "loopStatsTest", () -> {
        });
        List<Profiler.LoopStatistics> loopStatsList = stats.getLoopStatisticsList();
        assertNotNull(loopStatsList);
        assertEquals(10, loopStatsList.size()); // 2 threads * 5 loops
    }

    // ============================================================
    // Tests for LoopStatistics interface methods accessed through getLoopStatisticsList()
    // ============================================================

    @Test
    public void testLoopStatistics_GetMethodNameList() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMethodNames", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        List<String> names = loopStats.getMethodNameList();
        assertNotNull(names);
        assertEquals(1, names.size());
        assertEquals("loopMethodNames", names.get(0));
    }

    @Test
    public void testLoopStatistics_GetMinElapsedTimeMethod() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMin", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        Profiler.MethodStatistics minMethod = loopStats.getMinElapsedTimeMethod();
        assertNotNull(minMethod);
        assertEquals("loopMin", minMethod.getMethodName());
    }

    @Test
    public void testLoopStatistics_GetMaxElapsedTimeMethod() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMax", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        Profiler.MethodStatistics maxMethod = loopStats.getMaxElapsedTimeMethod();
        assertNotNull(maxMethod);
        assertEquals("loopMax", maxMethod.getMethodName());
    }

    @Test
    public void testLoopStatistics_GetMethodMaxElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMaxTime", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double maxTime = loopStats.getMethodMaxElapsedTimeInMillis("loopMaxTime");
        assertTrue(maxTime >= 0);
    }

    @Test
    public void testLoopStatistics_GetMethodMinElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMinTime", () -> doWork());
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double minTime = loopStats.getMethodMinElapsedTimeInMillis("loopMinTime");
        assertTrue(minTime >= 0);
    }

    @Test
    public void testLoopStatistics_GetMethodMinElapsedTimeInMillis_NotFound() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopMinNotFound", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double minTime = loopStats.getMethodMinElapsedTimeInMillis("nonExistent");
        assertEquals(0.0, minTime, 0.001);
    }

    @Test
    public void testLoopStatistics_GetMethodAverageElapsedTimeInMillis_NotFound() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopAvgNotFound", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        double avg = loopStats.getMethodAverageElapsedTimeInMillis("nonExistent");
        assertEquals(0.0, avg, 0.001);
    }

    @Test
    public void testLoopStatistics_GetMethodSize_NotFound() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopSizeNotFound", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        assertEquals(0, loopStats.getMethodSize("nonExistent"));
    }

    @Test
    public void testLoopStatistics_GetMethodStatisticsList() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopStatsList", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        List<Profiler.MethodStatistics> list = loopStats.getMethodStatisticsList("loopStatsList");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("loopStatsList", list.get(0).getMethodName());
    }

    @Test
    public void testLoopStatistics_GetMethodStatisticsList_NotFound() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopStatsNotFound", () -> {
        });
        Profiler.LoopStatistics loopStats = stats.getLoopStatisticsList().get(0);
        List<Profiler.MethodStatistics> list = loopStats.getMethodStatisticsList("nonExistent");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ============================================================
    // Reflection-based run method tests (with Method object)
    // ============================================================

    @Test
    public void testRunWithMethodReflection_MultipleThreads() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, null, 2, 2, 1);
        assertNotNull(stats);
        assertEquals(2, stats.getThreadNum());
    }

    @Test
    public void testRunWithMethodReflection_WithDelaysAndNullArg() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "normal");
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, (Object) null, 1, 0, 3, 0, 1);
        assertNotNull(stats);
        assertEquals(3, stats.getMethodSize("normal"));
    }

    @Test
    public void testRunWithMethodName_MultipleRounds() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, "normal", 1, 2, 2);
        assertNotNull(stats);
    }

    @Test
    public void testRunInvalidThreadNum() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(0, 10, 1, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(-1, 10, 1, () -> {
        }));
    }

    @Test
    public void testRunInvalidLoopNum() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, 0, 1, () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, -1, 1, () -> {
        }));
    }

    @Test
    public void testRunInvalidDelays() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, -1, 10, 0, 1, "test", () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, 0, 10, -1, 1, "test", () -> {
        }));
    }

    @Test
    public void testRunInvalidDelays_BothNegative() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(1, -1, 5, -1, 1, "test", () -> {
        }));
    }

    @Test
    public void testRunWithMethodName_NonExistentMethod() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(this, "nonExistentMethod", 1, 2, 1));
    }

    // ============================================================
    // Tests for Profiler.run(Object, Method, Object, int, int, int) — source line ~524
    // ============================================================

    @Test
    public void testRunWithMethodAndSingleArg() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, "arg", 1, 2, 1);
        assertNotNull(stats);
    }

    @Test
    public void testRunWithMethodAndDelays_WithArg() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, "testVal", 1, 0, 2, 0, 1);
        assertNotNull(stats);
        assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);
    }

    @Test
    public void test_error() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.run(this, method, CommonUtil.toList("a", "b"), 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).printResult();
        Profiler.run(this, method, null, 2, 2, 1).writeHtmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeXmlResult(System.out);
        Profiler.run(this, method, new ArrayList<>(), 2, 2, 1).writeResult(System.out);
        assertNotNull(method);
    }

    @Test
    public void testRunWithMethodReflectionAndArgs() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, CommonUtil.toList("testArg"), 1, 2, 1);
        assertNotNull(stats);
        // Method throws, so there should be failed stats
        assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);
    }

    @Test
    public void testRunWithMethodReflection_ArgsListSizeMismatch() {
        // args.size() > 1 and args.size() != threadNum should throw
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        List<Object> args = new ArrayList<>();
        args.add("a");
        args.add("b");
        args.add("c");
        // 3 args but threadNum=2 should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(this, method, args, 2, 2, 1));
    }

    @Test
    public void testRunWithMethodReflection_ArgsSizeMatchesThreadNum() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        List<Object> args = new ArrayList<>();
        args.add("arg1");
        args.add("arg2");
        // 2 args, threadNum=2 should work
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, args, 2, 2, 1);
        assertNotNull(stats);
    }

    @Test
    public void testMultiLoopsStatisticsGetFailedMethodStatisticsList_NoFailures() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "noFail", () -> {
        });
        List<Profiler.MethodStatistics> list = stats.getFailedMethodStatisticsList("noFail");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMultiLoopsStatisticsGetAllFailedMethodStatisticsList_NoFailures() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "allNoFail", () -> {
        });
        assertTrue(stats.getAllFailedMethodStatisticsList().isEmpty());
    }

    @Test
    public void testPrintResult_WithErrors() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;

        try {
            System.setOut(new java.io.PrintStream(baos));

            final int[] counter = { 0 };
            Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "printErr", () -> {
                counter[0]++;
                if (counter[0] % 2 == 0) {
                    throw new RuntimeException("fail");
                }
            });
            stats.printResult();

            String output = baos.toString();
            assertTrue(output.contains("printErr"));
            assertTrue(output.contains("Errors:"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testWriteHtmlResultWithErrors() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 10, 1, "htmlErrorTest", () -> {
            counter[0]++;
            if (counter[0] % 3 == 0) {
                throw new RuntimeException("HTML error test");
            }
        });

        StringWriter sw = new StringWriter();
        stats.writeHtmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("Errors:"));
    }

    @Test
    public void testWriteXmlResultWithErrors() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 10, 1, "xmlErrorTest", () -> {
            counter[0]++;
            if (counter[0] % 3 == 0) {
                throw new RuntimeException("XML error test");
            }
        });

        StringWriter sw = new StringWriter();
        stats.writeXmlResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("<errors>"));
    }

    @Test
    public void testRunInvalidRoundNum() {
        // roundNum can be 0 or negative but the profiler still runs at least once
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 0, "zeroRound", () -> {
        });
        // Depending on implementation, it should either return null or valid stats
        // Just verify no exception is thrown
        assertNotNull(stats);
    }

    @Test
    public void testLoopStatistics_GetFailedMethodStatisticsList() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopFailed", () -> {
            counter[0]++;
            if (counter[0] % 2 == 0) {
                throw new RuntimeException("fail");
            }
        });
        boolean foundFailed = false;
        for (Profiler.LoopStatistics ls : stats.getLoopStatisticsList()) {
            if (!ls.getFailedMethodStatisticsList("loopFailed").isEmpty()) {
                foundFailed = true;
                break;
            }
        }
        assertTrue(foundFailed);
    }

    @Test
    public void testLoopStatistics_GetAllFailedMethodStatisticsList() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "loopAllFailed", () -> {
            counter[0]++;
            if (counter[0] % 2 == 0) {
                throw new RuntimeException("fail");
            }
        });
        boolean foundFailed = false;
        for (Profiler.LoopStatistics ls : stats.getLoopStatisticsList()) {
            if (!ls.getAllFailedMethodStatisticsList().isEmpty()) {
                foundFailed = true;
                break;
            }
        }
        assertTrue(foundFailed);
    }

    // ============================================================
    // Failed method statistics integration tests
    // ============================================================

    @Test
    public void testFailedMethodStatistics() {
        final int[] counter = { 0 };
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 10, 1, "errorTest", () -> {
            counter[0]++;
            if (counter[0] % 3 == 0) {
                throw new RuntimeException("Test exception");
            }
        });

        assertTrue(stats.getFailedMethodStatisticsList("errorTest").size() > 0);
        assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);

        StringWriter sw = new StringWriter();
        stats.writeResult(sw);
        String output = sw.toString();
        assertTrue(output.contains("Errors:"));
    }

    @Test
    public void testFailedMethodStatistics_AllFail() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(1, 5, 1, "allFail", () -> {
            throw new RuntimeException("always fails");
        });
        assertEquals(5, stats.getAllFailedMethodStatisticsList().size());
        for (Profiler.MethodStatistics ms : stats.getAllFailedMethodStatisticsList()) {
            assertTrue(ms.isFailed());
            assertEquals("allFail", ms.getMethodName());
        }
    }

    // ============================================================
    // Statistics calculations comprehensive tests
    // ============================================================

    @Test
    public void testStatisticsCalculations() {
        Profiler.MultiLoopsStatistics stats = Profiler.run(2, 10, 1, "statsTest", () -> {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
            }
        });

        assertTrue(stats.getMethodTotalElapsedTimeInMillis("statsTest") > 0);
        assertTrue(stats.getMethodAverageElapsedTimeInMillis("statsTest") > 0);
        assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") >= 0);
        assertTrue(stats.getMethodMaxElapsedTimeInMillis("statsTest") > 0);
        assertTrue(stats.getTotalElapsedTimeInMillis() > 0);

        assertTrue(stats.getMethodMinElapsedTimeInMillis("statsTest") <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        double avg = stats.getMethodAverageElapsedTimeInMillis("statsTest");
        assertTrue(avg >= stats.getMethodMinElapsedTimeInMillis("statsTest"));
        assertTrue(avg <= stats.getMethodMaxElapsedTimeInMillis("statsTest"));

        assertEquals(20, stats.getMethodSize("statsTest"));

        assertEquals(20, stats.getMethodStatisticsList("statsTest").size());

        Profiler.MethodStatistics minMethod = stats.getMinElapsedTimeMethod();
        Profiler.MethodStatistics maxMethod = stats.getMaxElapsedTimeMethod();
        assertNotNull(minMethod);
        assertNotNull(maxMethod);
        assertEquals("statsTest", minMethod.getMethodName());
        assertEquals("statsTest", maxMethod.getMethodName());
    }

    // ============================================================
    // Percentile calculation tests
    // ============================================================

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

        assertTrue(output.contains("0.01% >="));
        assertTrue(output.contains("0.1% >="));
        assertTrue(output.contains("1% >="));
        assertTrue(output.contains("10% >="));
        assertTrue(output.contains("50% >="));
        assertTrue(output.contains("90% >="));
        assertTrue(output.contains("99% >="));
        assertTrue(output.contains("99.9% >="));
        assertTrue(output.contains("99.99% >="));
    }

    @Test
    public void testRunWithMethodReflection_ErrorMethod() {
        Method method = ClassUtil.getDeclaredMethod(ProfilerTest.class, "error", Object.class);
        Profiler.MultiLoopsStatistics stats = Profiler.run(this, method, (Object) null, 1, 2, 1);
        assertNotNull(stats);
        // error method should produce failed statistics
        assertTrue(stats.getAllFailedMethodStatisticsList().size() > 0);
    }

    // ============================================================
    // Tests for Profiler.suspend(boolean) — source line ~888
    // ============================================================

    @Test
    public void testSuspend() {
        boolean originalSuspended = false;

        try {
            Profiler.suspend(true);

            Profiler.MultiLoopsStatistics stats = Profiler.run(5, 100, 3, "suspended", () -> doWork());
            assertNotNull(stats);

            Profiler.suspend(false);

            stats = Profiler.run(2, 5, 1, "normal", () -> doWork());
            assertNotNull(stats);
        } finally {
            Profiler.suspend(originalSuspended);
        }
    }

    @Test
    public void testSuspend_ToggleMultipleTimes() {
        boolean original = Profiler.isSuspended();
        try {
            Profiler.suspend(true);
            assertTrue(Profiler.isSuspended());
            Profiler.suspend(false);
            assertFalse(Profiler.isSuspended());
            Profiler.suspend(true);
            assertTrue(Profiler.isSuspended());
            Profiler.suspend(false);
            assertFalse(Profiler.isSuspended());
        } finally {
            Profiler.suspend(original);
        }
    }

    @Test
    public void testSuspend_RunExecutesSingleIteration() {
        boolean original = Profiler.isSuspended();
        try {
            Profiler.suspend(true);
            // When suspended, should run with 1 thread, 1 loop regardless of params
            Profiler.MultiLoopsStatistics stats = Profiler.run(10, 100, 5, "suspendedRun", () -> {
            });
            assertNotNull(stats);
            // The loopStatisticsList should be minimal (1 iteration)
            assertEquals(1, stats.getLoopStatisticsList().size());
        } finally {
            Profiler.suspend(original);
        }
    }

    // ============================================================
    // Tests for Profiler.isSuspended() — source line ~899
    // ============================================================

    @Test
    public void testIsSuspended() {
        boolean original = Profiler.isSuspended();
        try {
            Profiler.suspend(true);
            assertTrue(Profiler.isSuspended());

            Profiler.suspend(false);
            assertFalse(Profiler.isSuspended());
        } finally {
            Profiler.suspend(original);
        }
    }

    // ============================================================
    // Tests for Profiler.sleep(long) — source line ~903 (package-private)
    // ============================================================

    @Test
    public void testSleep_WhenNotSuspended() {
        boolean original = Profiler.isSuspended();
        try {
            Profiler.suspend(false);
            long start = System.currentTimeMillis();
            Profiler.sleep(50);
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed >= 40); // Allow some tolerance
        } finally {
            Profiler.suspend(original);
        }
    }

    @Test
    public void testSleep_WhenSuspended() {
        boolean original = Profiler.isSuspended();
        try {
            Profiler.suspend(true);
            long start = System.currentTimeMillis();
            Profiler.sleep(1000);
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed < 500); // Should return immediately when suspended
        } finally {
            Profiler.suspend(original);
        }
    }

    // ============================================================
    // Tests for MethodStatistics constructors — source line ~1227
    // ============================================================

    @Test
    public void testMethodStatisticsDefaultConstructor() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("myMethod");
        assertEquals("myMethod", stats.getMethodName());
        assertNull(stats.getResult());
        assertFalse(stats.isFailed());
        assertEquals(0, stats.getStartTimeInMillis());
        assertEquals(0, stats.getEndTimeInMillis());
        assertEquals(0, stats.getStartTimeInNano());
        assertEquals(0, stats.getEndTimeInNano());
        assertEquals(0.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testMethodStatisticsTimingConstructor() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("method", 100, 200, 100000000L, 200000000L);
        assertEquals("method", stats.getMethodName());
        assertNull(stats.getResult());
        assertFalse(stats.isFailed());
        assertEquals(100, stats.getStartTimeInMillis());
        assertEquals(200, stats.getEndTimeInMillis());
    }

    @Test
    public void testMethodStatisticsFullConstructor() {
        Object result = "success";
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("method", 100, 200, 100000000L, 200000000L, result);
        assertEquals("method", stats.getMethodName());
        assertEquals(result, stats.getResult());
        assertFalse(stats.isFailed());
    }

    @Test
    public void testMethodStatisticsFullConstructorWithException() {
        Exception error = new RuntimeException("fail");
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("method", 100, 200, 100000000L, 200000000L, error);
        assertTrue(stats.isFailed());
        assertEquals(error, stats.getResult());
    }

    @Test
    public void testMethodStatisticsFullConstructor_NullResult() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("method", 100, 200, 100000000L, 200000000L, null);
        assertNull(stats.getResult());
        assertFalse(stats.isFailed());
    }

    // ============================================================
    // Tests for MethodStatistics.getMethodName() — source line ~1267
    // ============================================================

    @Test
    public void testMethodStatistics() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("testMethod");
        assertEquals("testMethod", stats.getMethodName());

        long startMillis = System.currentTimeMillis();
        long startNano = System.nanoTime();
        long endNano = System.nanoTime() + 1000000;
        long endMillis = System.currentTimeMillis() + 1;

        Profiler.MethodStatistics timedStats = new Profiler.MethodStatistics("timedMethod", startMillis, endMillis, startNano, endNano);

        assertEquals("timedMethod", timedStats.getMethodName());
        assertTrue(timedStats.getElapsedTimeInMillis() > 0);

        Object result = "Success";
        Profiler.MethodStatistics withResult = new Profiler.MethodStatistics("withResult", startMillis, endMillis, startNano, endNano, result);

        assertEquals(result, withResult.getResult());
        assertFalse(withResult.isFailed());

        Exception error = new RuntimeException("Test error");
        Profiler.MethodStatistics withError = new Profiler.MethodStatistics("withError", startMillis, endMillis, startNano, endNano, error);

        assertEquals(error, withError.getResult());
        assertTrue(withError.isFailed());

        String str = withResult.toString();
        assertTrue(str.contains("withResult"));
        assertTrue(str.contains("Success"));

        String errorStr = withError.toString();
        assertTrue(errorStr.contains("RuntimeException"));
        assertTrue(errorStr.contains("Test error"));
    }

    @Test
    public void testMethodStatisticsGetMethodName_EmptyName() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("");
        assertEquals("", stats.getMethodName());
    }

    // ============================================================
    // Tests for MethodStatistics.getResult() / setResult(Object) — source line ~1271
    // ============================================================

    @Test
    public void testMethodStatisticsGetSetResult() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        assertNull(stats.getResult());
        stats.setResult("value");
        assertEquals("value", stats.getResult());
        assertFalse(stats.isFailed());

        stats.setResult(new RuntimeException("err"));
        assertTrue(stats.isFailed());
    }

    @Test
    public void testMethodStatisticsSetResult_NullAfterValue() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setResult("value");
        assertEquals("value", stats.getResult());
        stats.setResult(null);
        assertNull(stats.getResult());
        assertFalse(stats.isFailed());
    }

    @Test
    public void testMethodStatisticsSetResult_ReplaceExceptionWithValue() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setResult(new RuntimeException("err"));
        assertTrue(stats.isFailed());
        stats.setResult("recovered");
        assertFalse(stats.isFailed());
        assertEquals("recovered", stats.getResult());
    }

    // ============================================================
    // Tests for MethodStatistics.isFailed() — source line ~1287
    // ============================================================

    @Test
    public void testMethodStatisticsIsFailed_NonExceptionObject() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setResult(42);
        assertFalse(stats.isFailed());
    }

    @Test
    public void testMethodStatisticsIsFailed_CheckedException() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setResult(new Exception("checked"));
        assertTrue(stats.isFailed());
    }

    @Test
    public void testMethodStatisticsIsFailed_Error() {
        // Error is not an Exception, so isFailed should return false
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setResult(new Error("oom"));
        assertFalse(stats.isFailed());
    }

    // ============================================================
    // Tests for MethodStatistics.toString() — source line ~1298
    // ============================================================

    @Test
    public void testMethodStatisticsToStringSuccess() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("myMethod", 1000, 2000, 1000000000L, 2000000000L, "OK");
        String str = stats.toString();
        assertTrue(str.contains("myMethod"));
        assertTrue(str.contains("OK"));
    }

    @Test
    public void testMethodStatisticsToStringFailed() {
        RuntimeException e = new RuntimeException("Something broke");
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("myMethod", 1000, 2000, 1000000000L, 2000000000L, e);
        String str = stats.toString();
        assertTrue(str.contains("myMethod"));
        assertTrue(str.contains("RuntimeException"));
        assertTrue(str.contains("Something broke"));
    }

    @Test
    public void testMethodStatisticsToString_NullResult() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("myMethod", 1000, 2000, 1000000000L, 2000000000L, null);
        String str = stats.toString();
        assertTrue(str.contains("myMethod"));
        assertTrue(str.contains("null"));
    }

    @Test
    public void testMethodStatisticsToString_NullExceptionMessage() {
        RuntimeException e = new RuntimeException((String) null);
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("myMethod", 1000, 2000, 1000000000L, 2000000000L, e);
        String str = stats.toString();
        assertTrue(str.contains("myMethod"));
        assertTrue(str.contains("RuntimeException"));
        assertTrue(str.contains("null"));
    }

    // ============================================================
    // Tests for MethodStatistics time getters/setters (inherited from AbstractStatistics)
    // ============================================================

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

        assertEquals(startMillis, stats.getStartTimeInMillis());
        assertEquals(endMillis, stats.getEndTimeInMillis());
        assertEquals(startNano, stats.getStartTimeInNano());
        assertEquals(endNano, stats.getEndTimeInNano());
        assertEquals(result, stats.getResult());
        assertEquals(1000.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testMethodStatisticsGetElapsedTimeInMillis() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test", 0, 0, 0, 5000000L);
        assertEquals(5.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testMethodStatisticsGetElapsedTimeInMillis_Zero() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test", 0, 0, 0, 0);
        assertEquals(0.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testMethodStatisticsSetStartTimeInMillis() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setStartTimeInMillis(500L);
        assertEquals(500L, stats.getStartTimeInMillis());
    }

    @Test
    public void testMethodStatisticsSetEndTimeInMillis() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setEndTimeInMillis(1500L);
        assertEquals(1500L, stats.getEndTimeInMillis());
    }

    @Test
    public void testMethodStatisticsSetStartTimeInNano() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setStartTimeInNano(999L);
        assertEquals(999L, stats.getStartTimeInNano());
    }

    @Test
    public void testMethodStatisticsSetEndTimeInNano() {
        Profiler.MethodStatistics stats = new Profiler.MethodStatistics("test");
        stats.setEndTimeInNano(8888L);
        assertEquals(8888L, stats.getEndTimeInNano());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics constructors — source line ~1590
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsConstructorWithoutList() {
        long startMillis = System.currentTimeMillis();
        long startNano = System.nanoTime();
        long endNano = System.nanoTime() + 1000000;
        long endMillis = System.currentTimeMillis() + 1;

        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(startMillis, endMillis, startNano, endNano, 4);
        assertEquals(4, stats.getThreadNum());
        assertNotNull(stats.getLoopStatisticsList());
        assertTrue(stats.getLoopStatisticsList().isEmpty());
        assertNotNull(stats.getMethodNameList());
        assertTrue(stats.getMethodNameList().isEmpty());
    }

    @Test
    public void testMultiLoopsStatisticsNullLoopStatisticsList() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1, null);
        assertNotNull(stats.getLoopStatisticsList());
        assertTrue(stats.getLoopStatisticsList().isEmpty());
    }

    @Test
    public void testMultiLoopsStatisticsConstructor_WithList() {
        List<Profiler.LoopStatistics> list = new ArrayList<>();
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(100, 200, 100000L, 200000L, 2, list);
        assertEquals(2, stats.getThreadNum());
        assertSame(list, stats.getLoopStatisticsList());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getThreadNum() — source line ~1617
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetThreadNum() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 7);
        assertEquals(7, stats.getThreadNum());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodNameList() — source line ~1622
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodNameListEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        stats.setLoopStatisticsList(null);
        // When loopStatisticsList is null, should return empty list
        List<String> names = stats.getMethodNameList();
        assertNotNull(names);
        assertTrue(names.isEmpty());
    }

    @Test
    public void testMultiLoopsStatisticsSetLoopStatisticsList_Null() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        stats.setLoopStatisticsList(null);
        // getLoopStatisticsList should return a new empty list when null
        assertNotNull(stats.getLoopStatisticsList());
        assertTrue(stats.getLoopStatisticsList().isEmpty());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMaxElapsedTimeMethod() — source line ~1663
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMaxElapsedTimeMethodEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertNull(stats.getMaxElapsedTimeMethod());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMinElapsedTimeMethod() — source line ~1679
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMinElapsedTimeMethodEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertNull(stats.getMinElapsedTimeMethod());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodTotalElapsedTimeInMillis(String) — source line ~1699
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodTotalElapsedTimeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getMethodTotalElapsedTimeInMillis("nonexistent"), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodMaxElapsedTimeInMillis(String) — source line ~1716
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodMaxElapsedTimeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getMethodMaxElapsedTimeInMillis("nonexistent"), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodMinElapsedTimeInMillis(String) — source line ~1736
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodMinElapsedTimeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getMethodMinElapsedTimeInMillis("nonexistent"), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodAverageElapsedTimeInMillis(String) — source line ~1758
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodAverageElapsedTimeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getMethodAverageElapsedTimeInMillis("nonexistent"), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getTotalElapsedTimeInMillis() — source line ~1772
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetTotalElapsedTimeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getTotalElapsedTimeInMillis(), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodSize(String) — source line ~1784
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodSizeEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0, stats.getMethodSize("nonexistent"));
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getMethodStatisticsList(String) — source line ~1795
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetMethodStatisticsListEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        List<Profiler.MethodStatistics> list = stats.getMethodStatisticsList("nonexistent");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getFailedMethodStatisticsList(String) — source line ~1806
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetFailedMethodStatisticsListEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        List<Profiler.MethodStatistics> list = stats.getFailedMethodStatisticsList("nonexistent");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getAllFailedMethodStatisticsList() — source line ~1817
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetAllFailedMethodStatisticsListEmpty() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        List<Profiler.MethodStatistics> list = stats.getAllFailedMethodStatisticsList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMultiLoopsStatisticsSetStartEndTime() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);

        stats.setStartTimeInMillis(1000L);
        stats.setEndTimeInMillis(2000L);
        stats.setStartTimeInNano(1000000000L);
        stats.setEndTimeInNano(2000000000L);

        assertEquals(1000L, stats.getStartTimeInMillis());
        assertEquals(2000L, stats.getEndTimeInMillis());
        assertEquals(1000000000L, stats.getStartTimeInNano());
        assertEquals(2000000000L, stats.getEndTimeInNano());
        assertEquals(1000.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getResult() / setResult(Object)
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetSetResult() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);

        assertNull(stats.getResult());
        Object result = "testResult";
        stats.setResult(result);
        assertEquals(result, stats.getResult());
    }

    @Test
    public void testMultiLoopsStatisticsGetSetResult_Null() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        stats.setResult("something");
        stats.setResult(null);
        assertNull(stats.getResult());
    }

    // ============================================================
    // Tests for MultiLoopsStatistics.getElapsedTimeInMillis()
    // ============================================================

    @Test
    public void testMultiLoopsStatisticsGetElapsedTimeInMillis() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 5000000L, 1);
        assertEquals(5.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testMultiLoopsStatisticsGetElapsedTimeInMillis_Zero() {
        Profiler.MultiLoopsStatistics stats = new Profiler.MultiLoopsStatistics(0, 0, 0, 0, 1);
        assertEquals(0.0, stats.getElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testSingleLoopStatistics_DefaultConstructor() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        assertNotNull(stats);
        assertTrue(stats.getMethodNameList().isEmpty());
        assertTrue(stats.getMethodStatisticsList().isEmpty());
    }

    @Test
    public void testSingleLoopStatistics_ConstructorWithTimes() {
        long start = System.currentTimeMillis();
        long end = start + 100;
        long startNano = System.nanoTime();
        long endNano = startNano + 100_000_000L;
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics(start, end, startNano, endNano);
        assertNotNull(stats);
        assertEquals(start, stats.getStartTimeInMillis());
        assertEquals(end, stats.getEndTimeInMillis());
    }

    @Test
    public void testSingleLoopStatistics_SetAndGetMethodList() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        List<Profiler.MethodStatistics> list = new ArrayList<>();
        list.add(makeMethodStats("method1", 10, false));
        list.add(makeMethodStats("method2", 20, false));
        stats.setMethodStatisticsList(list);
        assertEquals(2, stats.getMethodStatisticsList().size());
    }

    @Test
    public void testSingleLoopStatistics_AddAndGetMethodNameList() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("alpha", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("beta", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("alpha", 15, false)); // duplicate name
        List<String> names = stats.getMethodNameList();
        assertEquals(2, names.size());
        assertTrue(names.contains("alpha"));
        assertTrue(names.contains("beta"));
    }

    @Test
    public void testSingleLoopStatistics_GetMaxElapsedTimeMethod() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("fast", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("slow", 50, false));
        stats.addMethodStatisticsList(makeMethodStats("medium", 20, false));
        Profiler.MethodStatistics max = stats.getMaxElapsedTimeMethod();
        assertNotNull(max);
        assertEquals("slow", max.getMethodName());
    }

    @Test
    public void testSingleLoopStatistics_GetMinElapsedTimeMethod() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("fast", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("slow", 50, false));
        Profiler.MethodStatistics min = stats.getMinElapsedTimeMethod();
        assertNotNull(min);
        assertEquals("fast", min.getMethodName());
    }

    @Test
    public void testSingleLoopStatistics_GetMaxMinElapsedTime_Empty() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        assertNull(stats.getMaxElapsedTimeMethod());
        assertNull(stats.getMinElapsedTimeMethod());
    }

    @Test
    public void testSingleLoopStatistics_GetMethodTotalElapsedTime() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 20, false));
        stats.addMethodStatisticsList(makeMethodStats("other", 5, false));
        double total = stats.getMethodTotalElapsedTimeInMillis("m");
        assertTrue(total > 0);
    }

    @Test
    public void testSingleLoopStatistics_GetMethodMaxElapsedTime() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 30, false));
        double max = stats.getMethodMaxElapsedTimeInMillis("m");
        assertTrue(max > 0);
    }

    @Test
    public void testSingleLoopStatistics_GetMethodMinElapsedTime() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 30, false));
        double min = stats.getMethodMinElapsedTimeInMillis("m");
        assertTrue(min > 0);
        assertTrue(min <= stats.getMethodMaxElapsedTimeInMillis("m"));
    }

    @Test
    public void testSingleLoopStatistics_GetMethodMinElapsedTime_NoMatch() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("other", 10, false));
        double min = stats.getMethodMinElapsedTimeInMillis("nonexistent");
        assertEquals(0.0, min, 0.001);
    }

    @Test
    public void testSingleLoopStatistics_GetMethodAverageElapsedTime() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 20, false));
        double avg = stats.getMethodAverageElapsedTimeInMillis("m");
        assertTrue(avg > 0);
    }

    @Test
    public void testSingleLoopStatistics_GetMethodAverageElapsedTime_NoMethod() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        double avg = stats.getMethodAverageElapsedTimeInMillis("nonexistent");
        assertEquals(0.0, avg, 0.001);
    }

    @Test
    public void testSingleLoopStatistics_GetTotalElapsedTime() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("a", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("b", 20, false));
        double total = stats.getTotalElapsedTimeInMillis();
        assertTrue(total > 0);
    }

    @Test
    public void testSingleLoopStatistics_GetTotalElapsedTime_Empty() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        assertEquals(0.0, stats.getTotalElapsedTimeInMillis(), 0.001);
    }

    @Test
    public void testSingleLoopStatistics_GetMethodSize() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("other", 5, false));
        assertEquals(2, stats.getMethodSize("m"));
        assertEquals(1, stats.getMethodSize("other"));
        assertEquals(0, stats.getMethodSize("absent"));
    }

    @Test
    public void testSingleLoopStatistics_GetMethodStatisticsListByName() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 10, false));
        stats.addMethodStatisticsList(makeMethodStats("other", 5, false));
        List<Profiler.MethodStatistics> list = stats.getMethodStatisticsList("m");
        assertEquals(2, list.size());
        for (Profiler.MethodStatistics ms : list) {
            assertEquals("m", ms.getMethodName());
        }
    }

    @Test
    public void testSingleLoopStatistics_GetFailedMethodStatisticsListByName() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("m", 10, true));
        stats.addMethodStatisticsList(makeMethodStats("m", 15, true));
        List<Profiler.MethodStatistics> failed = stats.getFailedMethodStatisticsList("m");
        assertEquals(2, failed.size());
    }

    @Test
    public void testSingleLoopStatistics_GetFailedMethodStatisticsListByName_NoFailed() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("m", 5, false));
        List<Profiler.MethodStatistics> failed = stats.getFailedMethodStatisticsList("m");
        assertEquals(0, failed.size());
    }

    @Test
    public void testSingleLoopStatistics_GetAllFailedMethodStatisticsList() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        stats.addMethodStatisticsList(makeMethodStats("a", 5, false));
        stats.addMethodStatisticsList(makeMethodStats("b", 10, true));
        stats.addMethodStatisticsList(makeMethodStats("c", 15, true));
        List<Profiler.MethodStatistics> failed = stats.getAllFailedMethodStatisticsList();
        assertEquals(2, failed.size());
    }

    @Test
    public void testSingleLoopStatistics_GetAllFailedMethodStatisticsList_Empty() {
        Profiler.SingleLoopStatistics stats = new Profiler.SingleLoopStatistics();
        List<Profiler.MethodStatistics> failed = stats.getAllFailedMethodStatisticsList();
        assertEquals(0, failed.size());
    }

}
