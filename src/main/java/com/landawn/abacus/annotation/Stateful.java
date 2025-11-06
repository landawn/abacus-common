/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class, method, or field as stateful, indicating that it maintains mutable internal state
 * that can change between invocations or over time. This annotation serves as a warning that the
 * annotated element is not thread-safe and requires careful handling in concurrent environments.
 * 
 * <p>Elements marked with {@code @Stateful} have the following characteristics:</p>
 * <ul>
 *   <li>They maintain internal state that affects their behavior</li>
 *   <li>They are generally not thread-safe without external synchronization</li>
 *   <li>They should not be cached or shared across multiple threads</li>
 *   <li>They may produce different results when called multiple times with the same inputs</li>
 * </ul>
 * 
 * <p>Important considerations:</p>
 * <ul>
 *   <li>Do not cache or store stateful objects in shared contexts</li>
 *   <li>Do not access or update stateful objects from multiple threads without synchronization</li>
 *   <li>Consider using thread-local storage or instance-per-thread patterns</li>
 *   <li>Be aware that stateful objects may not be suitable for functional programming patterns. In particular, avoid using stateful objects or interfaces within parallel streams.</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * import java.io.BufferedReader;
 * import java.util.Iterator;
 *
 * @Stateful
 * public class Counter {
 *     private int count = 0;
 *
 *     public int increment() {
 *         return ++count;  // Not thread-safe!
 *     }
 *
 *     public int getCount() {
 *         return count;
 *     }
 * }
 *
 * public class DataReader {
 *     @Stateful
 *     public Iterator<String> getIterator(BufferedReader reader) {
 *         // Returns a stateful iterator that maintains position
 *         return reader.lines().iterator();
 *     }
 * }
 *
 * @Stateful
 * public class StreamBuilder<T> {
 *     private List<T> elements = new ArrayList<>();
 *
 *     public StreamBuilder<T> add(T element) {
 *         elements.add(element);
 *         return this;  // Modifies internal state
 *     }
 * }
 * }</pre>
 *
 * @see Immutable
 * @see SequentialOnly
 * @see Mutable
 * @see ParallelSupported
 */
@Documented
@Retention(value = RetentionPolicy.CLASS)
@Target(value = { ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
public @interface Stateful {

}
