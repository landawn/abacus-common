/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import com.landawn.abacus.util.N;

/**
 *
 * @param <T>
 */
public abstract class AbstractPrimaryType<T> extends AbstractType<T> {

    protected AbstractPrimaryType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if is immutable.
     *
     * @return {@code true}, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Checks if is comparable.
     *
     * @return {@code true}, if is comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public T valueOf(final Object obj) {
        if (obj == null) {
            return defaultValue();
        }

        return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
    }
}
