package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Stream;

public class StreamPerformanceTest {
    static final String[] a = new String[1000_000];
    static final long sum;
    static {
        long tmp = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = Strings.uuid();

            for (char ch : a[i].toUpperCase().toCharArray()) {
                tmp += ch;
            }
        }

        sum = tmp;
    }

    @Test
    public void test_01() {

        Profiler.run(1, 10, 3, "abacus parallel",
                () -> assertEquals(sum, Stream.of(a).parallel(10).map(t -> t.toUpperCase()).flatMapToInt(t -> CharStream.of(t).asIntStream()).sum()));
    }

    @Test
    public void test_02() {
        assertEquals(sum, Stream.of(a).parallel(10).map(t -> t.toUpperCase()).flatMapToInt(t -> CharStream.of(t).asIntStream()).sum());

    }
}
