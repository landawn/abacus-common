/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.util;

import com.landawn.abacus.util.u.Optional;

abstract class PrimitivePair<PP extends PrimitivePair<PP>> {

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void accept(Throwables.Consumer<? super PP, E> action) throws E {
        action.accept((PP) this);
    }

    /**
     *
     * @param <U>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <U, E extends Exception> U map(Throwables.Function<? super PP, U, E> mapper) throws E {
        return mapper.apply((PP) this);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> Optional<PP> filter(final Throwables.Predicate<? super PP, E> predicate) throws E {
        return predicate.test((PP) this) ? Optional.of((PP) this) : Optional.<PP> empty();
    }

    /**
     * 
     * @return
     */
    public Optional<PP> toOptional() {
        return Optional.of((PP) this);
    }
}
