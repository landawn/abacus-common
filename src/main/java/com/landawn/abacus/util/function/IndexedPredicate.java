/*
 * Copyright (C) 2016 HaiYang Li
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
import com.landawn.abacus.util.Throwables;

/**
 *
 * @since 0.8
 *
 * @author Haiyang Li
 */
public interface IndexedPredicate<T> extends Throwables.IndexedPredicate<T, RuntimeException> { //NOSONAR

    /**
    * 
    *
    * @param idx 
    * @param e 
    * @return 
    */
    @Override
    boolean test(int idx, T e);

    /**
     * 
     *
     * @return 
     */
    default IndexedPredicate<T> negate() {
        return (idx, e) -> !test(idx, e);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    default IndexedPredicate<T> and(IndexedPredicate<? super T> other) {
        N.checkArgNotNull(other);

        return (idx, e) -> test(idx, e) && other.test(idx, e);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    default IndexedPredicate<T> or(IndexedPredicate<? super T> other) {
        N.checkArgNotNull(other);

        return (idx, e) -> test(idx, e) || other.test(idx, e);
    }
}
