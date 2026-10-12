package com.landawn.abacus.util.stream;

import java.nio.IntBuffer;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntIterator;

/** Shared fixtures and lifecycle for the IntStream contract tests. */
public abstract class IntStreamTestSupport extends TestBase {

    protected IntStream stream;

    protected IntStream createIntStream(int... a) {
        return IntStream.of(a);
    }

    protected IntStream createIntStream(int[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex);
    }

    protected IntStream createIntStream(Integer[] a) {
        return IntStream.of(a);
    }

    protected IntStream createIntStream(Integer[] a, int fromIndex, int toIndex) {
        return IntStream.of(a, fromIndex, toIndex);
    }

    protected IntStream createIntStream(Collection<Integer> coll) {
        return IntStream.of(coll.toArray(new Integer[coll.size()]));
    }

    protected IntStream createIntStream(IntIterator iter) {
        return iter == null ? IntStream.empty() : IntStream.of(iter.toArray());
    }

    protected IntStream createIntStream(IntBuffer buff) {
        return IntStream.of(buff);
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        if (stream != null) {
            stream.close();
        }
    }
}
