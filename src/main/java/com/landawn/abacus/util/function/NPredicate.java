/*
 * Copyright (C) 2021 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.N;

/**
 *
 * @since 2.3.17
 *
 * @author Haiyang Li
 */
public interface NPredicate<T> {

    /**
     *
     *
     * @param args
     * @return
     */
    boolean test(T... args);

    /**
     *
     *
     * @return
     */
    default NPredicate<T> negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default NPredicate<T> and(final NPredicate<? super T> other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default NPredicate<T> or(final NPredicate<? super T> other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }
}
