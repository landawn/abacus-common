/*
 * Copyright (C) 2023 HaiYang Li
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
package com.landawn.abacus.guava;

import java.util.function.Function;

import com.google.common.collect.Range;

public class GuavaUtil {
    private GuavaUtil() {
        // Utility class.
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param range
     * @param mapper
     * @return
     */
    public static <T extends Comparable<? super T>, U extends Comparable<? super U>> Range<U> transform(final Range<T> range, final Function<T, U> mapper) {
        if (range.hasLowerBound() && range.hasUpperBound()) {
            return Range.range(mapper.apply(range.lowerEndpoint()), range.lowerBoundType(), mapper.apply(range.upperEndpoint()), range.upperBoundType());
        } else if (range.hasLowerBound()) {
            return Range.downTo(mapper.apply(range.lowerEndpoint()), range.lowerBoundType());
        } else if (range.hasUpperBound()) {
            return Range.upTo(mapper.apply(range.upperEndpoint()), range.upperBoundType());
        } else {
            return Range.all();
        }
    }

}
