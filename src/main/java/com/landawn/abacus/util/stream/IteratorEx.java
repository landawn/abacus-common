/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.util.Iterator;

/**
 * An extended iterator interface that adds bulk-skip, count, and resource-management capabilities
 * on top of the standard {@link Iterator}. It adds three default methods:
 * <ul>
 *   <li>{@link #advance(long)} — skip a number of elements in bulk;</li>
 *   <li>{@link #count()} — consume and count the remaining elements;</li>
 *   <li>{@link #closeResource()} — release any resources attached to the iterator.</li>
 * </ul>
 *
 * <p>The {@link #closeResource()} method has a no-op default; implementations that hold resources
 * (file handles, database connections, network streams, etc.) should override it. It is purely a
 * resource-release handler associated with this iterator: calling it does not necessarily exhaust or
 * invalidate the iterator, so {@link #hasNext()}, {@link #next()}, and the other methods may continue to behave
 * as before.  However, the fact that those methods may still work does not mean it is correct to keep using
 * the iterator: once {@code closeResource()} has been called, this iterator should be considered
 * spent and discarded.
 *
 * <p>Note that this interface does <i>not</i> extend {@link AutoCloseable}, so
 * {@link #closeResource()} is not invoked automatically by a try-with-resources statement; the
 * caller is responsible for invoking it once the iterator is no longer needed.
 *
 * @param <T> the type of elements returned by this iterator
 * @see Iterator
 */
public interface IteratorEx<T> extends Iterator<T> {

    /**
     * Advances the iterator by skipping the specified number of elements without returning them.
     * If the iterator has fewer elements than the specified number, all remaining elements will be skipped.
     * If {@code n} is 0 or negative, no action is performed.
     *
     * <p>The default implementation repeatedly calls {@link #next()}; implementations backed by an
     * indexable source may override this to skip in constant time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example: Skip first 3 elements
     * IteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c", "d", "e");
     * iter.advance(3);
     * String fourthElement = iter.hasNext() ? iter.next() : null;   // "d"
     * }</pre>
     *
     * @param n the number of elements to skip (negative or zero values are ignored)
     * @see #count()
     */
    default void advance(long n) {
        if (n <= 0) {
            return;
        }

        while (n > 0 && hasNext()) {
            next();
            n--;
        }
    }

    /**
     * Counts the remaining elements in this iterator by consuming all of them.
     * After this method returns, the iterator will be exhausted ({@link #hasNext()} returns {@code false}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example: Count remaining elements
     * IteratorEx<String> iter = ObjIteratorEx.of("a", "b", "c", "d", "e");
     * long total = iter.count();                            // 5
     * System.out.println("Found " + total + " elements");   // prints: Found 5 elements
     * }</pre>
     *
     * @return the number of elements that remained in this iterator before this call
     * @see #advance(long)
     */
    default long count() {
        long result = 0;

        while (hasNext()) {
            next();

            result++;
        }

        return result;
    }

    /**
     * Releases any resources attached to this iterator.
     * The default implementation does nothing. Implementing classes should override this method
     * if they hold resources that need to be released (e.g., file handles, database connections,
     * or network streams).
     *
     * <p>This method is purely a resource-release handler associated with this iterator; it is not a
     * terminal operation on the iteration itself. Invoking it does not necessarily exhaust or
     * invalidate the iterator, so {@link #hasNext()}, {@link #next()}, {@link #advance(long)}, {@link #count()}, and
     * the other methods may continue to behave as before after this method has been called.
     * However, the fact that those methods may still work does not mean it is correct to keep using
     * the iterator: once {@code closeResource()} has been called, this iterator should be considered
     * spent and discarded.
     *
     * <p>Because {@code IteratorEx} does not extend {@link AutoCloseable}, this method is <i>not</i>
     * invoked automatically by a try-with-resources statement; the caller must invoke it explicitly
     * (typically from a {@code finally} block) once the iterator is no longer needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example: Explicit cleanup in a finally block
     * IteratorEx<String> iter = createFileIterator("data.txt");
     * try {
     *     while (iter.hasNext()) {
     *         process(iter.next());
     *     }
     * } finally {
     *     iter.closeResource();   // releases the underlying file handle
     * }
     * }</pre>
     *
     */
    default void closeResource() {
        // do nothing.
    }
}
