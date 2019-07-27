package com.landawn.samples.abacus;

import org.junit.Test;

import com.landawn.abacus.util.Profiler;

public class PerformanceTestByProfiler extends Jdbc {

    @Test
    public void test_01() {
        Profiler.run(1, 10, 3, "crud_by_Jdbc", () -> crud_by_Jdbc()).printResult();

        Profiler.run(1, 10, 3, "crud_by_SQLExecutor", () -> crud_by_SQLExecutor()).printResult();
    }

}
