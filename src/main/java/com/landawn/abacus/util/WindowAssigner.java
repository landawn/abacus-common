/*
 * Copyright (c) 2024, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * An abstract class that defines how elements in a stream should be assigned to windows.
 * This class is designed to work with the Stream.window() operations for grouping stream
 * elements into logical windows based on various criteria such as time, count, or custom logic.
 * 
 * <p>WindowAssigner implementations determine the boundaries of windows and control how
 * elements are grouped together for processing. Common windowing strategies include:</p>
 * <ul>
 *   <li>Fixed-size windows (e.g., every N elements)</li>
 *   <li>Time-based windows (e.g., every 5 seconds)</li>
 *   <li>Sliding windows (e.g., overlapping windows)</li>
 *   <li>Session windows (e.g., grouped by activity with gaps)</li>
 * </ul>
 * 
 * <p>This is an internal class used by the Stream API's windowing operations.</p>
 *
 * @author Haiyang Li
 * @since 5.3.0
 * @see Stream#window(WindowAssigner)
 */
public abstract class WindowAssigner {

    /**
     * Processes the input iterator and returns a new iterator that groups elements into windows.
     * This method is called internally by the Stream API to apply windowing logic.
     * 
     * <p>Implementations should define how elements from the input iterator are grouped
     * into windows and returned as batches through the output iterator.</p>
     *
     * @param <T> the type of elements in the stream
     * @param iter the input iterator containing the stream elements
     * @return an iterator that produces windows (groups) of elements
     */
    abstract <T> ObjIterator<T> process(ObjIterator<T> iter);
}