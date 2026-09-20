package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ParentExceptionReview20260913Test extends TestBase {
    @Test
    void compoundKeyElementsAreValidatedBeforeValue() {
        final PrefixSearchTable.Builder<String, String> builder = PrefixSearchTable.builder();
        assertThrows(NullPointerException.class, () -> builder.add(Arrays.asList("parent", null), null));
        builder.add(List.of("parent", "child"), "value");
        assertEquals("value", builder.build().get(List.of("parent", "child")).orElseThrow());
    }

    @Test
    void compoundKeyValidationReadsEachInputElementOnce() {
        final int[] reads = new int[2];
        final List<String> key = new AbstractList<>() {
            @Override
            public String get(final int index) {
                assertEquals(0, reads[index]++);
                return index == 0 ? "parent" : "child";
            }

            @Override
            public int size() {
                return 2;
            }
        };
        final PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder().add(key, "value").build();
        assertEquals("value", table.get(List.of("parent", "child")).orElseThrow());
    }

    @Test
    void eagerRowConversionPropagatesUnsupportedInsertion() {
        final Dataset dataset = new RowDataset(List.of("value"), List.of(List.of(1)));
        assertThrows(UnsupportedOperationException.class, () -> dataset.toList(size -> Collections.emptyList()));
        assertThrows(UnsupportedOperationException.class, () -> dataset.getRow(0, size -> Collections.emptyMap()));
    }

    @Test
    void nestedRowConversionRejectsNonBeanParent() {
        final Dataset dataset = new RowDataset(List.of("parent.child"), List.of(List.of("value")));
        assertThrows(UnsupportedOperationException.class, () -> dataset.toList(NonBeanParent.class));
    }

    @Test
    void materializingClosedSequenceRejectsState() throws Exception {
        final Seq<Integer, Exception> sequence = Seq.of(1);
        sequence.close();
        assertThrows(IllegalStateException.class, sequence::toArrayAndClose);
    }

    public static class NonBeanParent {
        private String parent;

        public String getParent() {
            return parent;
        }

        public void setParent(final String parent) {
            this.parent = parent;
        }
    }
}
