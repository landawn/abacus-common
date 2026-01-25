/*****************************************************************************
 * ------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");           *
 * you may not use this file except in compliance with the License.          *
 * You may obtain a copy of the License at                                   *
 *                                                                           *
 * http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                           *
 * Unless required by applicable law or agreed to in writing, software       *
 * distributed under the License is distributed on an "AS IS" BASIS,         *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 * See the License for the specific language governing permissions and       *
 * limitations under the License.                                            *
 *****************************************************************************/
package com.landawn.abacus.util;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.EntryStream;

/**
 * A lookup table that stores prefix (a list of keys of type {@code K}) -&gt; value mappings.
 *
 * <p>For example, if the table maps {@code [a, b]} prefix to a value "foo", when you search by
 * {@code [a, b, c]}, it will find the {@code [a, b] -> foo} mapping.
 *
 * <p>Conceptually it's a "Trie" except it searches by a list of key prefixes instead of string
 * prefixes.
 * 
 * <br />
 * Note: coped from <a href="https://github.com/google/mug">google mug under</a> under Apache License, Version 2.0 and modified.
 *
 * @param <K> the type of key elements in the compound prefix
 * @param <V> the type of values stored in the table
 * @since 7.1
 */
public final class PrefixSearchTable<K, V> {
    private final Map<K, Node<K, V>> nodes;

    private PrefixSearchTable(Map<K, Node<K, V>> nodes) {
        this.nodes = nodes;
    }

    /**
     * Searches the table for the longest prefix match of {@code compoundKey}.
     *
     * @param compoundKey the compound key to search for
     * @return the value mapped to the longest (non-empty) prefix of {@code compoundKey} if present
     * @throws IllegalArgumentException if {@code compoundKey} is empty
     * @throws NullPointerException if {@code compoundKey} is null or any key element is null
     */
    public Optional<V> get(List<? extends K> compoundKey) {
        return getAll(compoundKey).values().reduce((shorter, longer) -> longer);
    }

    /**
     * Searches the table for prefixes of {@code compoundKey} and returns a <em>lazy</em> EntryStream of
     * all mappings in ascending order of prefix length.
     *
     * <p>If no non-empty prefix exists in the table, an empty EntryStream is returned.
     *
     * <p>To get the longest matched prefix, use {@link #get} instead.
     *
     * @param compoundKey the compound key to search for
     * @return EntryStream of the matched prefixes of {@code compoundKey} and the mapped values
     * @throws IllegalArgumentException if {@code compoundKey} is empty
     * @throws NullPointerException if {@code compoundKey} is null or any key element is null
     */
    public EntryStream<List<K>, V> getAll(List<? extends K> compoundKey) {
        N.checkArgument(compoundKey.size() > 0, "cannot search by empty key");

        return EntryStream.of(new Iterator<Map.Entry<List<K>, V>>() {
            private Map<K, Node<K, V>> remaining = nodes;
            private int cusor = 0;
            private Map.Entry<List<K>, V> next;

            @Override
            public boolean hasNext() {
                if (next == null && remaining != null && cusor < compoundKey.size()) {
                    while (remaining != null && cusor < compoundKey.size()) {
                        Node<K, V> node = remaining.get(requireNonNull(compoundKey.get(cusor)));

                        if (node == null) {
                            remaining = null;
                            break;
                        }

                        cusor++;
                        remaining = node.children;

                        if (node.value != null) {
                            next = new AbstractMap.SimpleImmutableEntry<>(unmodifiableList(compoundKey.subList(0, cusor)), node.value);
                            break;
                        }
                    }
                }

                return next != null;
            }

            @Override
            public Map.Entry<List<K>, V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final Map.Entry<List<K>, V> result = next;
                next = null;
                return result;
            }
        });
    }

    /**
     * Returns a new builder initialized with the same prefix mappings in this table.
     *
     * @return a builder initialized with the current mappings
     * @since 7.2
     */
    public Builder<K, V> toBuilder() {
        Builder<K, V> builder = builder();
        EntryStream.of(nodes).mapValue(Node::toBuilder).forEach(builder.nodes::put);
        return builder;
    }

    @Override
    public String toString() {
        return nodes.toString();
    }

    /**
     * Returns a new builder.
     *
     * @param <K> the type of key elements in the compound prefix
     * @param <V> the type of values stored in the table
     * @return a new builder instance
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder of {@link PrefixSearchTable}.
     *
     * @param <K> the type of key elements in the compound prefix
     * @param <V> the type of values stored in the table
     */
    public static final class Builder<K, V> {
        private final Map<K, Node.Builder<K, V>> nodes = new HashMap<>();

        /**
         * Adds the mapping from {@code compoundKey} to {@code value}.
         *
         * @param compoundKey the compound key to add
         * @param value the value to associate with the compound key
         * @return this builder
         * @throws IllegalArgumentException if {@code compoundKey} is empty, or it has already been mapped to a
         *     value that's not equal to {@code value}.
         * @throws NullPointerException if {@code compoundKey} is null, any of the key element is null
         *     or {@code value} is null
         */
        public Builder<K, V> add(List<? extends K> compoundKey, V value) {
            int size = compoundKey.size();
            N.checkArgument(size > 0, "empty key not allowed");
            N.requireNonNull(value);

            Node.Builder<K, V> node = nodes.computeIfAbsent(compoundKey.get(0), k -> new Node.Builder<>());

            for (int i = 1; i < size; i++) {
                node = node.child(compoundKey.get(i));
            }

            N.checkArgument(node.set(value), "conflicting key: %s", compoundKey);

            return this;
        }

        /**
         * Adds all of {@code mappings} into this builder.
         *
         * @param mappings the mappings to add
         * @return this builder
         * @since 8.0
         */
        public Builder<K, V> addAll(Map<? extends List<? extends K>, ? extends V> mappings) {
            EntryStream.of(mappings).forEach(this::add);
            return this;
        }

        /**
         * Builds a PrefixSearchTable from the accumulated mappings.
         *
         * @return a new PrefixSearchTable containing all added mappings
         */
        public PrefixSearchTable<K, V> build() {
            return new PrefixSearchTable<>(EntryStream.of(nodes).mapValue(Node.Builder::build).toMap());
        }

        Builder() {
        }
    }

    private static record Node<K, V>(V value, Map<K, Node<K, V>> children) {

        private Node(V value, Map<K, Node<K, V>> children) {
            this.value = value;
            this.children = children;
        }

        Builder<K, V> toBuilder() {
            Builder<K, V> builder = new Builder<>();
            builder.value = value;
            EntryStream.of(children).mapValue(Node::toBuilder).forEach(builder.children::put);
            return builder;
        }

        static class Builder<K, V> {
            private V value;
            private final Map<K, Builder<K, V>> children = new HashMap<>();

            Builder<K, V> child(K key) {
                requireNonNull(key);
                return children.computeIfAbsent(key, k -> new Builder<K, V>());
            }

            /**
             * Sets the value carried by this node to {@code value} if it's not already set. Return false
             * if the value has already been set to a different value.
             */
            boolean set(V value) {
                if (this.value == null) {
                    this.value = value;
                    return true;
                } else {
                    return value.equals(this.value);
                }
            }

            Node<K, V> build() {
                return new Node<>(value, EntryStream.of(children).mapValue(Builder::build).toMap());
            }
        }
    }
}
