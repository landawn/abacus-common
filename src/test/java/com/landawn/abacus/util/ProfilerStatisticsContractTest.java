package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Profiler.MethodStatistics;
import com.landawn.abacus.util.Profiler.MultiLoopsStatistics;
import com.landawn.abacus.util.Profiler.SingleLoopStatistics;

/** Statistics arithmetic and reports need no timed workload or sleeps. */
class ProfilerStatisticsContractTest extends TestBase {
    private static MethodStatistics sample(String name, long millis, Object result) {
        return new MethodStatistics(name, 1_000, 1_000 + millis, 0, millis * 1_000_000, result);
    }

    @Test
    void singleLoopAggregatesRepeatedNamesAndFiltersFailures() {
        MethodStatistics fast = sample("work", 2, null);
        MethodStatistics failed = sample("work", 4, new IllegalStateException("failure"));
        MethodStatistics other = sample("other", 1, null);
        SingleLoopStatistics loop = new SingleLoopStatistics(1_000, 1_007, 0, 7_000_000);
        loop.setMethodStatisticsList(new ArrayList<>(List.of(fast, failed, other)));

        assertEquals(List.of("work", "other"), loop.getMethodNameList());
        assertSame(other, loop.getMinElapsedTimeMethod());
        assertSame(failed, loop.getMaxElapsedTimeMethod());
        assertEquals(6.0, loop.getMethodTotalElapsedTimeInMillis("work"));
        assertEquals(3.0, loop.getMethodAverageElapsedTimeInMillis("work"));
        assertEquals(2.0, loop.getMethodMinElapsedTimeInMillis("work"));
        assertEquals(4.0, loop.getMethodMaxElapsedTimeInMillis("work"));
        assertEquals(7.0, loop.getTotalElapsedTimeInMillis());
        assertEquals(2, loop.getMethodInvocationCount("work"));
        assertEquals(List.of(fast, failed), loop.getMethodStatisticsList("work"));
        assertEquals(List.of(failed), loop.getFailedMethodStatisticsList("work"));
        assertEquals(List.of(failed), loop.getAllFailedMethodStatisticsList());
        assertEquals(List.of(), loop.getFailedMethodStatisticsList("other"));
        assertEquals(0.0, loop.getMethodAverageElapsedTimeInMillis("missing"));
        assertEquals(0, loop.getMethodInvocationCount("missing"));

        loop.setMethodStatisticsList(null);
        assertNull(loop.getMinElapsedTimeMethod());
        assertEquals(0.0, loop.getMethodAverageElapsedTimeInMillis("work"));
        assertEquals(0.0, loop.getTotalElapsedTimeInMillis());
        assertEquals(List.of(), loop.getFailedMethodStatisticsList("work"));
    }

    @Test
    void multipleLoopsWeightAveragesByInvocationCount() {
        MethodStatistics first = sample("work", 2, null);
        MethodStatistics failed = sample("work", 4, new IllegalArgumentException("failure"));
        MethodStatistics last = sample("work", 6, null);
        SingleLoopStatistics left = new SingleLoopStatistics(0, 6, 0, 6_000_000, List.of(first, failed));
        SingleLoopStatistics right = new SingleLoopStatistics(0, 6, 0, 6_000_000, List.of(last));
        MultiLoopsStatistics aggregate = new MultiLoopsStatistics(0, 12, 0, 12_000_000, 2);
        aggregate.addLoopStatistics(new SingleLoopStatistics());
        aggregate.addLoopStatistics(left);
        aggregate.addLoopStatistics(right);

        assertSame(first, aggregate.getMinElapsedTimeMethod());
        assertSame(last, aggregate.getMaxElapsedTimeMethod());
        assertEquals(12.0, aggregate.getMethodTotalElapsedTimeInMillis("work"));
        assertEquals(12.0, aggregate.getTotalElapsedTimeInMillis());
        assertEquals(4.0, aggregate.getMethodAverageElapsedTimeInMillis("work"));
        assertEquals(3, aggregate.getMethodInvocationCount("work"));
        assertEquals(List.of(first, failed, last), aggregate.getMethodStatisticsList("work"));
        assertEquals(List.of(failed), aggregate.getFailedMethodStatisticsList("work"));
        assertEquals(List.of(), aggregate.getFailedMethodStatisticsList("missing"));
        assertThrows(IllegalArgumentException.class, () -> aggregate.addLoopStatistics(null));
        aggregate.setLoopStatisticsList(null);
        assertNull(aggregate.getMinElapsedTimeMethod());
        assertEquals(0.0, aggregate.getTotalElapsedTimeInMillis());
        assertEquals(0.0, aggregate.getMethodTotalElapsedTimeInMillis("work"));
        assertEquals(List.of(), aggregate.getFailedMethodStatisticsList("work"));
    }

    @Test
    void methodStatisticsKeepClockUnitsSeparateAndTrackFailureState() {
        MethodStatistics sample = new MethodStatistics(null);
        sample.setStartTimeInMillis(100);
        sample.setEndTimeInMillis(104);
        sample.setStartTimeInNano(1_000_000);
        sample.setEndTimeInNano(3_500_000);
        assertEquals("null", sample.getMethodName());
        assertEquals(100, sample.getStartTimeInMillis());
        assertEquals(104, sample.getEndTimeInMillis());
        assertEquals(1_000_000, sample.getStartTimeInNano());
        assertEquals(3_500_000, sample.getEndTimeInNano());
        assertEquals(2.5, sample.getElapsedTimeInMillis());
        sample.setResult("success");
        assertFalse(sample.isFailed());
        assertTrue(sample.toString().contains("success"));
        IllegalStateException failure = new IllegalStateException("failed");
        sample.setResult(failure);
        assertSame(failure, sample.getResult());
        assertTrue(sample.isFailed());
        assertTrue(sample.toString().contains("failed"));
    }

    @Test
    void byteStreamReportsIncludeFailuresAndEscapeMarkup() throws Exception {
        MethodStatistics failure = sample("work", 2, new IllegalStateException("test<failure>&"));
        MultiLoopsStatistics statistics = new MultiLoopsStatistics(0, 2, 0, 2_000_000, 1,
                List.of(new SingleLoopStatistics(0, 2, 0, 2_000_000, List.of(failure))));
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        statistics.writeResult(text);
        assertTrue(text.toString(StandardCharsets.UTF_8).contains("test<failure>&"));
        ByteArrayOutputStream html = new ByteArrayOutputStream();
        statistics.writeHtmlResult(html);
        assertTrue(html.toString(StandardCharsets.UTF_8).contains("test&lt;failure&gt;&amp;"));
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        statistics.writeXmlResult(xml);
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml.toByteArray()));
        assertEquals(1, document.getElementsByTagName("error").getLength());
        assertTrue(document.getElementsByTagName("error").item(0).getTextContent().contains("test<failure>&"));
    }
}
