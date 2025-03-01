/*
 * Copyright (C) 2016 HaiYang Li
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

package com.landawn.abacus.util;

public final class IndexedInt extends AbstractIndexed {

    private final int value;

    IndexedInt(final long index, final int value) {
        super(index);
        this.value = value;
    }

    /**
     *
     * @param value
     * @param index
     * @return
     * @throws IllegalArgumentException
     */
    public static IndexedInt of(final int value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedInt(index, value);
    }

    /**
     *
     * @param value
     * @param index
     * @return
     * @throws IllegalArgumentException
     */
    public static IndexedInt of(final int value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedInt(index, value);
    }

    public int value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedInt && ((IndexedInt) obj).index == index && N.equals(((IndexedInt) obj).value, value);
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
