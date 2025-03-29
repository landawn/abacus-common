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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @param <K> the key type
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class ListMultimapType<K, E> extends MultimapType<K, E, List<E>, ListMultimap<K, E>> {

    ListMultimapType(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName) {
        super(typeClass, keyTypeName, valueElementTypeName, null);
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final ListMultimap<K, E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public ListMultimap<K, E> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);
        final ListMultimap<K, E> multiMap = N.newLinkedListMultimap(map.size());

        for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
            multiMap.putMany(entry.getKey(), entry.getValue());
        }

        return multiMap;
    }

}
