package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

public abstract class IterablesTestSupport extends AbstractTest {

    protected static final Comparator<Integer> REVERSE_ORDER_NULLS_FIRST = Comparator.nullsFirst(Comparator.reverseOrder());
    protected static final Comparator<Integer> REVERSE_ORDER_NULLS_LAST = Comparator.nullsLast(Comparator.reverseOrder());

    protected List<Integer> intList;
    protected List<String> stringList;
    protected List<Double> doubleList;
    protected List<BigInteger> bigIntList;
    protected List<BigDecimal> bigDecimalList;

    @BeforeEach
    public void setUp() {
        intList = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        stringList = Arrays.asList("apple", "banana", "cherry", "date");
        doubleList = Arrays.asList(3.14, 2.71, 1.41, 1.73);
        bigIntList = Arrays.asList(BigInteger.valueOf(100), BigInteger.valueOf(200), BigInteger.valueOf(300));
        bigDecimalList = Arrays.asList(BigDecimal.valueOf(10.5), BigDecimal.valueOf(20.5), BigDecimal.valueOf(30.5));
    }

    @SafeVarargs
    protected static <T> List<T> list(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    protected static class TestObject implements Comparable<TestObject> {
        final int id;
        final String value;

        TestObject(int id, String value) {
            this.id = id;
            this.value = value;
        }

        public int getId() {
            return id;
        }

        public String getValue() {
            return value;
        }

        @Override
        public int compareTo(TestObject o) {
            return Integer.compare(this.id, o.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestObject that = (TestObject) o;
            return id == that.id && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "TestObject{id=" + id + ", value='" + value + "'}";
        }
    }

    /**
     * Single-use {@code Iterable} backed by an iterator. Calling {@code iterator()} more than once
     * returns the already-consumed iterator, which would cause a double-iteration bug to drop data.
     */
    protected static <T> Iterable<T> singleUseIterable(final List<T> source) {
        final Iterator<T> it = source.iterator();
        return () -> it;
    }
}
