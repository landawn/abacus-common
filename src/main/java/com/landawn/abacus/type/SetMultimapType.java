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
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for SetMultimap, which maps keys to sets of values.
 * This class handles serialization and deserialization of SetMultimap instances.
 *
 * @param <K> the key type
 * @param <E> the element type in the value sets
 */
@SuppressWarnings("java:S2160")
public class SetMultimapType<K, E> extends MultimapType<K, E, Set<E>, SetMultimap<K, E>> {

    SetMultimapType(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName) {
        super(typeClass, keyTypeName, valueElementTypeName, null);
    }

    /**
     * Converts a SetMultimap to its JSON string representation.
     * The multimap is first converted to a Map&lt;K, Collection&lt;E&gt;&gt; format where each key
     * maps to a collection of its associated values, then serialized to JSON.
     *
     * @param x the SetMultimap to convert to string
     * @return the JSON string representation of the multimap, or {@code null} if the input is null
     */
    @Override
    @MayReturnNull
    public String stringOf(final SetMultimap<K, E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string representation and returns the corresponding SetMultimap.
     * The string should represent a map where each key maps to a collection of values.
     * The resulting SetMultimap will use LinkedHashSet for value collections to maintain
     * insertion order while ensuring uniqueness.
     *
     * @param str the JSON string to parse
     * @return the parsed SetMultimap, or {@code null} if the input string is {@code null} or empty
     * @throws IllegalArgumentException if the string cannot be parsed as a valid map structure
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public SetMultimap<K, E> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);
        final SetMultimap<K, E> multiMap = N.newLinkedSetMultimap(map.size());

        for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
            multiMap.putMany(entry.getKey(), entry.getValue());
        }

        return multiMap;
    }
}
