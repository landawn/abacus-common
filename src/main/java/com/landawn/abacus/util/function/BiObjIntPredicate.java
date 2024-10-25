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

import com.landawn.abacus.util.Throwables;

public interface BiObjIntPredicate<T, U> extends Throwables.BiObjIntPredicate<T, U, RuntimeException> { //NOSONAR

    /**
     *
     *
     * @param t
     * @param u
     * @param i
     * @return
     */
    @Override
    boolean test(T t, U u, int i);

    default BiObjIntPredicate<T, U> negate() {
        return (t, u, i) -> !test(t, u, i);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BiObjIntPredicate<T, U> and(final BiObjIntPredicate<? super T, ? super U> other) {
        return (t, u, i) -> test(t, u, i) && other.test(t, u, i);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BiObjIntPredicate<T, U> or(final BiObjIntPredicate<? super T, ? super U> other) {
        return (t, u, i) -> test(t, u, i) || other.test(t, u, i);
    }
}
