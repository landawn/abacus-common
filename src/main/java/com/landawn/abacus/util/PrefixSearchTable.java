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
 *  *****************************************************************************/
package com.landawn.abacus.util;

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
 * <p>Instances are immutable and are created through a {@link Builder} obtained from
 * {@link #builder()}.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
 *         .add(Arrays.asList("a", "b"), "foo")
 *         .build();
 *
 * table.get(Arrays.asList("a", "b", "c"));   // returns Optional["foo"]
 * table.get(Arrays.asList("a"));             // returns Optional.empty()
 * }</pre>
 *
 * <br />
 * Note: copied from <a href="https://github.com/google/mug">google mug</a> under Apache License, Version 2.0 and modified.
 *
 * @param <K> the type of key elements in the compound prefix
 * @param <V> the type of values stored in the table
 * @see Builder
 */
public final class PrefixSearchTable<K, V> {
    private final Map<K, Node<K, V>> nodes;

    private PrefixSearchTable(Map<K, Node<K, V>> nodes) {
        this.nodes = nodes;
    }

    /**
     * Searches the table for the longest prefix match of {@code compoundKey}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
     *         .add(Arrays.asList("a", "b"), "foo")
     *         .add(Arrays.asList("a"), "bar")
     *         .build();
     *
     * table.get(Arrays.asList("a", "b", "c"));   // returns Optional["foo"] (longest match)
     * table.get(Arrays.asList("a"));             // returns Optional["bar"]
     * table.get(Arrays.asList("x"));             // returns Optional.empty()
     * }</pre>
     *
     * @param compoundKey the non-empty compound key to search for; elements must not be {@code null}
     * @return an {@code Optional} holding the value mapped to the longest (non-empty) prefix of
     *         {@code compoundKey}; an empty {@code Optional} if no non-empty prefix is present
     * @throws IllegalArgumentException if {@code compoundKey} is empty
     * @throws NullPointerException if {@code compoundKey} is {@code null} or any key element is
     *         {@code null}
     * @see #getAll(List)
     */
    public Optional<V> get(List<? extends K> compoundKey) {
        return getAll(compoundKey).values().reduce((shorter, longer) -> longer);
    }

    /**
     * Searches the table for prefixes of {@code compoundKey} and returns a <em>lazy</em>
     * {@link EntryStream} of all matching mappings in ascending order of prefix length.
     *
     * <p>The returned stream is backed by a single-use iterator and should be consumed at most once.
     * If no non-empty prefix exists in the table, an empty {@code EntryStream} is returned.
     *
     * <p>To get only the longest matched prefix, use {@link #get(List)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
     *         .add(Arrays.asList("a"), "bar")
     *         .add(Arrays.asList("a", "b"), "foo")
     *         .build();
     *
     * EntryStream<List<String>, String> results = table.getAll(Arrays.asList("a", "b", "c"));
     * // Streams: [a] -> "bar", then [a, b] -> "foo"
     * }</pre>
     *
     * @param compoundKey the non-empty compound key to search for; elements must not be {@code null}
     * @return a lazy {@code EntryStream} pairing each matched prefix of {@code compoundKey}
     *         (as an immutable snapshot) with its mapped value, in ascending order of
     *         prefix length; empty if no non-empty prefix is present
     * @throws IllegalArgumentException if {@code compoundKey} is empty (thrown eagerly by this method)
     * @throws NullPointerException if {@code compoundKey} is {@code null} or any key element is
     *         {@code null} (thrown eagerly by this method)
     * @see #get(List)
     */
    public EntryStream<List<K>, V> getAll(List<? extends K> compoundKey) throws IllegalArgumentException {
        N.checkArgument(compoundKey.size() > 0, "cannot search by empty key");
        final List<K> keySnapshot = List.copyOf(compoundKey);

        return EntryStream.of(new Iterator<Map.Entry<List<K>, V>>() {
            private Map<K, Node<K, V>> remaining = nodes;
            private int cursor = 0;
            private Map.Entry<List<K>, V> next;

            @Override
            public boolean hasNext() {
                if (next == null && remaining != null && cursor < keySnapshot.size()) {
                    while (remaining != null && cursor < keySnapshot.size()) {
                        Node<K, V> node = remaining.get(keySnapshot.get(cursor));

                        if (node == null) {
                            remaining = null;
                            break;
                        }

                        cursor++;
                        remaining = node.children;

                        if (node.value != null) {
                            next = new AbstractMap.SimpleImmutableEntry<>(List.copyOf(keySnapshot.subList(0, cursor)), node.value);
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
     * Returns a new {@link Builder} pre-populated with all prefix-to-value mappings in this table.
     * The returned builder can be used to add further mappings and produce a new table
     * without modifying this instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
     *         .add(Arrays.asList("a"), "foo")
     *         .build();
     *
     * PrefixSearchTable<String, String> extended = table.toBuilder()
     *         .add(Arrays.asList("a", "b"), "bar")
     *         .build();
     * }</pre>
     *
     * @return a new builder containing all current mappings of this table
     */
    public Builder<K, V> toBuilder() {
        Builder<K, V> builder = builder();
        EntryStream.of(nodes).mapValue(Node::toBuilder).forEach(builder.nodes::put);
        return builder;
    }

    /**
     * Returns a string representation of this table derived from its internal node map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
     *         .add(Arrays.asList("a", "b"), "foo")
     *         .build();
     *
     * System.out.println(table);
     * // Prints the internal node-map structure of the table.
     * }</pre>
     *
     * @return a string representation of this table
     */
    @Override
    public String toString() {
        return nodes.toString();
    }

    /**
     * Returns a new builder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PrefixSearchTable<String, Integer> table = PrefixSearchTable.<String, Integer>builder()
     *         .add(Arrays.asList("a", "b"), 1)
     *         .build();
     * }</pre>
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
         *         .add(Arrays.asList("a", "b"), "foo")
         *         .add(Arrays.asList("a", "b", "c"), "bar")
         *         .build();
         * }</pre>
         *
         * @param compoundKey the non-empty compound key to add
         * @param value the non-{@code null} value to associate with the compound key
         * @return this builder
         * @throws IllegalArgumentException if {@code compoundKey} is empty, or it has already been mapped to a
         *     value that is not equal to {@code value}
         * @throws NullPointerException if {@code compoundKey} is {@code null}, any key element is {@code null},
         *     or {@code value} is {@code null}
         */
        public Builder<K, V> add(List<? extends K> compoundKey, V value) throws IllegalArgumentException {
            int size = compoundKey.size();
            N.checkArgument(size > 0, "empty key not allowed");
            N.requireNonNull(value);

            Node.Builder<K, V> node = nodes.computeIfAbsent(requireNonNull(compoundKey.get(0)), k -> new Node.Builder<>());

            for (int i = 1; i < size; i++) {
                node = node.child(compoundKey.get(i));
            }

            N.checkArgument(node.set(value), "conflicting key: %s", compoundKey);

            return this;
        }

        /**
         * Adds all mappings from {@code mappings} into this builder.
         * Each entry's key becomes the compound key and its value becomes the associated value.
         *
         * <p>A {@code null} or empty {@code mappings} map is treated as a no-op; no entry is added.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<List<String>, String> mappings = new HashMap<>();
         * mappings.put(Arrays.asList("a", "b"), "foo");
         * mappings.put(Arrays.asList("x", "y"), "bar");
         *
         * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
         *         .addAll(mappings)
         *         .build();
         * }</pre>
         *
         * @param mappings the mappings to add; an empty compound key, or a {@code null} compound key
         *     element or value within an entry, is not permitted
         * @return this builder
         * @throws IllegalArgumentException if any compound key is empty or conflicts with an existing mapping
         * @throws NullPointerException if any compound key element or value within an entry is {@code null}
         */
        public Builder<K, V> addAll(Map<? extends List<? extends K>, ? extends V> mappings) {
            EntryStream.of(mappings).forEach(this::add);
            return this;
        }

        /**
         * Builds an immutable {@link PrefixSearchTable} from the accumulated mappings.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String>builder()
         *         .add(Arrays.asList("a", "b"), "foo")
         *         .add(Arrays.asList("c"), "bar")
         *         .build();
         * }</pre>
         *
         * @return a new {@code PrefixSearchTable} containing all mappings added to this builder
         */
        public PrefixSearchTable<K, V> build() {
            return new PrefixSearchTable<>(EntryStream.of(nodes).mapValue(Node.Builder::build).toMap());
        }

        /**
         * Package-private constructor; use {@link PrefixSearchTable#builder()} to obtain an instance.
         */
        Builder() {
        }
    }

    private record Node<K, V>(V value, Map<K, Node<K, V>> children) {

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
                return children.computeIfAbsent(key, k -> new Builder<>());
            }

            /**
             * Sets the value carried by this node to {@code value} if it is not already set.
             * Returns {@code false} if the value has already been set to a value that is not equal
             * to {@code value}; otherwise returns {@code true}.
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
