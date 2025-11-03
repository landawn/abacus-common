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

import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for ListMultimap serialization and deserialization.
 * This type handles conversion between ListMultimap instances and their JSON string representations.
 *
 * @param <K> the key type of the multimap
 * @param <E> the element type stored in the lists of the multimap
 */
@SuppressWarnings("java:S2160")
class ListMultimapType<K, E> extends MultimapType<K, E, List<E>, ListMultimap<K, E>> {

    ListMultimapType(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName) {
        super(typeClass, keyTypeName, valueElementTypeName, null);
    }

    /**
     * Converts a ListMultimap to its JSON string representation.
     *
     * The multimap is first converted to a Map&lt;K, List&lt;E&gt;&gt; structure,
     * then serialized to JSON using the default JSON parser.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimapType<String, Integer> type = new ListMultimapType<>(ListMultimap.class, "String", "Integer");
     * ListMultimap<String, Integer> multimap = N.newLinkedListMultimap();
     * multimap.put("scores", 85);
     * multimap.put("scores", 90);
     * multimap.put("scores", 78);
     * multimap.put("grades", 95);
     *
     * String json = type.stringOf(multimap);
     * // Returns: {"scores":[85,90,78],"grades":[95]}
     *
     * json = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the ListMultimap to convert to JSON string
     * @return the JSON string representation of the multimap, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final ListMultimap<K, E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string into a ListMultimap instance.
     *
     * This method deserializes the JSON string as a Map&lt;K, Collection&lt;E&gt;&gt;,
     * then populates a new LinkedListMultimap with the entries.
     * The resulting multimap preserves insertion order for both keys and values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListMultimapType<String, Integer> type = new ListMultimapType<>(ListMultimap.class, "String", "Integer");
     *
     * ListMultimap<String, Integer> multimap = type.valueOf("{\"scores\":[85,90,78],\"grades\":[95]}");
     * // Returns: ListMultimap with "scores" -> [85, 90, 78] and "grades" -> [95]
     * // multimap.get("scores") returns [85, 90, 78]
     *
     * multimap = type.valueOf(null);
     * // Returns: null
     *
     * multimap = type.valueOf("{}");
     * // Returns: empty ListMultimap
     * }</pre>
     *
     * @param str the JSON string to parse
     * @return a ListMultimap instance populated with the parsed data, or {@code null} if the string is empty or null
     * @throws IllegalArgumentException if the JSON string is malformed
     */
    @SuppressWarnings("unchecked")
    @Override
    public ListMultimap<K, E> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
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
