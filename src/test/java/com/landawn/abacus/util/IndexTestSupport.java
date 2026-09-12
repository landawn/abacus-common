package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.landawn.abacus.TestBase;

public abstract class IndexTestSupport extends TestBase {

    // === Review-driven additions ===

    // ------------------------------------------------------------------------------------------------
    // The two backward-search conventions now documented on the class:
    //   - element searches treat startIndexFromBack as an element index, useful range [0, length - 1];
    //   - pattern searches treat it as the highest index a match may start at, useful range [0, length].
    // In both, a negative startIndexFromBack finds nothing, while a negative forward fromIndex means 0.
    // ------------------------------------------------------------------------------------------------

    //
    // ============================ review fixes 2026-09-06 ============================
    //

    /** An AbstractCollection that counts toArray(); public so the reflective descendingIterator() lookup can reach it. */
    public static class ReviewFixes20260906CountingCollection<T> extends java.util.AbstractCollection<T> {
        final List<T> backing;

        public int toArrayCalls = 0;

        public ReviewFixes20260906CountingCollection(final List<T> backing) {
            this.backing = backing;
        }

        @Override
        public Iterator<T> iterator() {
            return backing.iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public Object[] toArray() {
            toArrayCalls++;
            return backing.toArray();
        }
    }

    /** The same, plus the public no-arg descendingIterator() that N.lastIndexOf looks up reflectively. */
    public static final class ReviewFixes20260906DescendingCollection<T> extends ReviewFixes20260906CountingCollection<T> {
        public int descendingIteratorCalls = 0;

        public ReviewFixes20260906DescendingCollection(final List<T> backing) {
            super(backing);
        }

        public Iterator<T> descendingIterator() {
            descendingIteratorCalls++;
            final List<T> copy = new ArrayList<>(backing);
            Collections.reverse(copy);
            return copy.iterator();
        }
    }

    /** An iterator of {@code n} fillers followed by "target"; cheap enough to be walked past Integer.MAX_VALUE. */
    protected static Iterator<Object> reviewFixes20260906TargetAfter(final long n) {
        return new Iterator<>() {
            protected long i = 0;

            @Override
            public boolean hasNext() {
                return i <= n;
            }

            @Override
            public Object next() {
                return i++ < n ? "filler" : "target";
            }
        };
    }
}
