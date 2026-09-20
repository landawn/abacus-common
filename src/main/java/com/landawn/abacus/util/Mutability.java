/*
 * Copyright (c) 2026, Haiyang Li.
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

import com.landawn.abacus.annotation.Beta;

/**
 * A conservative classification of a collection or map based on its implementation, without trial mutations.
 * This classification does not describe deep immutability, thread safety, or ownership of backing storage.
 *
 * @see N#mutabilityOf(java.util.Collection)
 * @see N#mutabilityOf(java.util.Map)
 */
@Beta
public enum Mutability {
    /**
     * A recognized unmodifiable implementation or an implementation of {@link Immutable}.
     * Changes through the container's API are unsupported, although elements or external backing storage
     * may still change. Mutating operations that have no effect may complete normally.
     */
    KNOWN_UNMODIFIABLE,

    /**
     * A recognized generally mutable implementation, including views that share a recognized runtime class.
     * Individual operations remain subject to their preconditions, including restrictions on elements,
     * keys, values, comparators and range bounds.
     */
    KNOWN_MUTABLE,

    /**
     * No classification is established. The container may be mutable, unmodifiable, or partially mutable;
     * a {@code null} container also has this result. Neither mutability nor unmodifiability may be assumed.
     */
    UNKNOWN
}
