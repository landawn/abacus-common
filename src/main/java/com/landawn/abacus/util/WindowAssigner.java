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
 * A minimal extension point for iterator transformations used by stream-windowing integrations.
 *
 * <p>The base class defines only the protected {@link #process(ObjIterator)} hook. It does not
 * itself create window objects, impose window boundaries, validate input, or prescribe ownership
 * and closing behavior. Concrete strategies, including implementations outside this package,
 * define those semantics.</p>
 */
public abstract class WindowAssigner {

    /**
     * Sole constructor for subclasses to invoke.
     * Concrete implementations of {@code WindowAssigner} call this constructor
     * to initialize the base class when defining specific windowing strategies.
     */
    protected WindowAssigner() {
    }

    /**
     * Transforms the input iterator according to this assigner's strategy. Both the input and
     * returned iterators carry the same element type; any grouping, filtering, ordering, null
     * handling, or resource-ownership behavior is defined by the implementation.
     *
     * @param <T> the type of elements in the stream
     * @param iter the input iterator; this base type performs no null validation
     * @return the transformed iterator; the implementation defines whether {@code null} is permitted
     */
    protected abstract <T> ObjIterator<T> process(ObjIterator<T> iter);
}
