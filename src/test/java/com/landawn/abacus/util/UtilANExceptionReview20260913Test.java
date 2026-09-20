package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil;

class UtilANExceptionReview20260913Test extends TestBase {
    public static class CountingBean {
        static final AtomicInteger constructions = new AtomicInteger();
        private String value;

        public CountingBean() {
            constructions.incrementAndGet();
        }

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    @Test
    void closedWriterRejectsAppendBeforeReadingTheSequenceOrRange() throws IOException {
        final BufferedWriter writer = new BufferedWriter(new StringWriter());
        writer.close();
        final CharSequence sequence = new CharSequence() {
            @Override
            public int length() {
                throw new AssertionError("Sequence was inspected");
            }

            @Override
            public char charAt(final int index) {
                throw new AssertionError("Sequence was inspected");
            }

            @Override
            public CharSequence subSequence(final int start, final int end) {
                throw new AssertionError("Sequence was inspected");
            }

            @Override
            public String toString() {
                throw new AssertionError("Sequence was inspected");
            }
        };
        assertThrows(IOException.class, () -> writer.append(sequence));
        assertThrows(IOException.class, () -> writer.append(sequence, -1, -2));
        assertThrows(IOException.class, () -> writer.append("abc", -1, 5));
    }

    @Test
    void randomBeanSelectionIsValidatedBeforeConstructionOrBatchCount() {
        // BeanInfo constructs a sample to inspect mutability; initialize that prerequisite before counting result construction.
        ParserUtil.getBeanInfo(CountingBean.class);
        CountingBean.constructions.set(0);
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(CountingBean.class, List.of("missing")));
        assertEquals(0, CountingBean.constructions.get());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(CountingBean.class, List.of("missing"), -1)).getMessage()
                .contains("missing"));
        assertEquals(0, CountingBean.constructions.get());
        assertEquals(0, Beans.newRandomBeanList(CountingBean.class, List.of("value"), 0).size());
    }

    @Test
    void logarithmsValidateValueBeforeRoundingMode() {
        assertEquals("x must be positive and finite", assertThrows(IllegalArgumentException.class, () -> Numbers.log2(0.0, null)).getMessage());
        assertEquals("x must be positive and finite", assertThrows(IllegalArgumentException.class, () -> Numbers.log10(Double.NaN, null)).getMessage());
    }

    @Test
    void conversionsValidateSourceBeforeDestination() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson((File) null, null, null, null)).getMessage().contains("csvFile"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CsvUtil.jsonToCsv((File) null, null, null)).getMessage().contains("jsonFile"));
    }

    @Test
    void unzipPropagatesSupplierResultAndInsertionFailures() {
        final IntFunction<Collection<?>> nullSupplier = size -> null;
        assertThrows(IllegalArgumentException.class,
                () -> N.unzip(Collections.<Integer> emptyList(), (Integer value, Pair<Integer, Integer> pair) -> pair.set(value, value), nullSupplier));
        final Collection<Integer> shared = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> N.unzip3(Collections.<Integer> emptyList(),
                (Integer value, Triple<Integer, Integer, Integer> triple) -> triple.set(value, value, value), size -> shared));
        assertThrows(UnsupportedOperationException.class, () -> N.unzip(List.of(1), (Integer value, Pair<Integer, Integer> pair) -> pair.set(value, value),
                size -> Collections.unmodifiableList(new ArrayList<>())));
    }
}
