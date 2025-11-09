/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

/**
 * A marker interface that provides mutable access to a value.
 * 
 * <p>This interface serves as a generic marker for the mutable wrapper implementations
 * in this package. It identifies classes that wrap primitive or object values and allow
 * those values to be modified after construction.</p>
 * 
 * <p>The primary use cases for mutable wrappers include:</p>
 * <ul>
 *   <li>Passing primitive values by reference to methods that need to modify them</li>
 *   <li>Storing frequently changing values in collections without creating new wrapper objects</li>
 *   <li>Accumulating values within lambda expressions or anonymous inner classes</li>
 *   <li>Avoiding the overhead of autoboxing when working with primitive values that change frequently</li>
 * </ul>
 * 
 * <p>Common implementations include:</p>
 * <ul>
 *   <li>{@link MutableBoolean} - for boolean values</li>
 *   <li>{@link MutableByte} - for byte values</li>
 *   <li>{@link MutableShort} - for short values</li>
 *   <li>{@link MutableInt} - for int values</li>
 *   <li>{@link MutableLong} - for long values</li>
 *   <li>{@link MutableFloat} - for float values</li>
 *   <li>{@link MutableDouble} - for double values</li>
 *   <li>{@link MutableChar} - for char values</li>
 * </ul>
 * 
 * <p>Example usage with a mutable counter in a lambda:</p>
 * <pre>{@code
 * MutableInt counter = MutableInt.of(0);
 * list.forEach(item -> {
 *     if (item.isValid()) {
 *         counter.increment();
 *     }
 * });
 * System.out.println("Valid items: " + counter.value());
 * }</pre>
 * 
 * <p>Example usage for passing by reference:</p>
 * <pre>{@code
 * public void processData(List<Integer> data, MutableLong sum, MutableInt count) {
 *     for (Integer value : data) {
 *         sum.add(value);
 *         count.increment();
 *     }
 * }
 * }</pre>
 * 
 * <p><strong>Thread Safety:</strong> Implementations of this interface are generally
 * NOT thread-safe. If multiple threads access a mutable instance concurrently,
 * and at least one thread modifies it, external synchronization is required.</p>
 * 
 * <p>Note: This interface is adapted from Apache Commons Lang.</p>
 * 
 * @version $Id: Mutable.java 1478488 2013-05-02 19:05:44Z ggregory $
 */
public interface Mutable {

}
