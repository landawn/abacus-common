package net.java8.lambda;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.landawn.abacus.util.Profiler;
import com.landawn.abacus.util.stream.Stream;

public class StreamPerformanceTest {
    static final SecureRandom rand = new SecureRandom();
    static final int length = 1000;
    static final int[] intArray = new int[length];

    {
        for (int i = 0, len = intArray.length; i < len; i++) {
            intArray[i] = rand.nextInt();
        }
    }

    static final List<Integer> intList = new ArrayList<>(length);

    {
        for (int num : intArray) {
            intList.add(num);
        }
    }

    @Test
    public void testStreamPerformance() {
        final int threadNum = 8;
        final int loopNum = 300000;
        Profiler.run(this, "test_array_max_by_for_loop", threadNum, loopNum, 3).printResult();

        Profiler.run(this, "test_array_max_by_jdk8_stream", threadNum, loopNum, 3).printResult();

        Profiler.run(this, "test_array_max_by_abacus_stream", threadNum, loopNum, 3).printResult();

        Profiler.run(this, "test_list_max_by_for_loop", threadNum, loopNum, 3).printResult();

        Profiler.run(this, "test_list_max_by_jdk8_stream", threadNum, loopNum, 3).printResult();

        Profiler.run(this, "test_list_max_by_abacus_stream", threadNum, loopNum, 3).printResult();
    }

    @Test
    public void test_array_max_by_for_loop() {
        int m = Integer.MIN_VALUE;
        for (int i = 0, len = intArray.length; i < len; i++) {
            if (intArray[i] > m) {
                m = intArray[i];
            }
        }
    }

    @Test
    public void test_array_max_by_jdk8_stream() {
        Arrays.stream(intArray).max();
    }

    @Test
    public void test_array_max_by_abacus_stream() {
        Stream.from(intArray).max();
    }

    @Test
    public void test_list_max_by_for_loop() {
        int m = Integer.MIN_VALUE;
        for (int e : intList) {
            if (e > m) {
                m = e;
            }
        }
    }

    @Test
    public void test_list_max_by_jdk8_stream() {
        intList.stream().max((a, b) -> (a.intValue() > b.intValue()) ? 1 : ((a.intValue() == b.intValue()) ? 0 : -1));
    }

    @Test
    public void test_list_max_by_abacus_stream() {
        Stream.of(intList).max((a, b) -> (a.intValue() > b.intValue()) ? 1 : ((a.intValue() == b.intValue()) ? 0 : -1));
    }
}
