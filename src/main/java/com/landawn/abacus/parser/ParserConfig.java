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

package com.landawn.abacus.parser;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.JsonXmlField;

/**
 * Abstract base class for parser configuration that provides common settings
 * shared by both serialization and deserialization configurations.
 *
 * <p>This class provides functionality for managing ignored properties during
 * parsing operations. Properties can be ignored globally or on a per-class basis.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JsonSerConfig config = new JsonSerConfig()
 *     .setIgnoredPropNames(Set.of("password", "internalId"))
 *     .setIgnoredPropNames(User.class, Set.of("temporaryToken"));
 * }</pre>
 *
 * @param <C> the concrete configuration type for method chaining
 * @see JsonXmlField
 * @see SerializationConfig
 * @see DeserializationConfig
 */
public abstract class ParserConfig<C extends ParserConfig<C>> implements Cloneable {

    /**
     * Protected constructor for subclasses.
     */
    protected ParserConfig() {
    }

    /** Map of ignored property names organized by class. */
    Map<Class<?>, Set<String>> ignoredBeanPropNameMap = null;

    /**
     * Gets the complete map of ignored property names organized by class.
     *
     * <p>The returned map contains entries where:</p>
     * <ul>
     *   <li>Keys are class types</li>
     *   <li>Values are sets of property names to ignore for that class</li>
     *   <li>The special key {@code Object.class} contains globally ignored properties</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * KryoSerConfig config = new KryoSerConfig();
     * Map<Class<?>, Set<String>> none = config.getIgnoredPropNames();  // returns null (none configured)
     *
     * config.setIgnoredPropNames(Set.of("password", "version"));
     * Map<Class<?>, Set<String>> map = config.getIgnoredPropNames();
     * Set<String> global = map.get(Object.class);  // returns ["password", "version"] (global ignores)
     * }</pre>
     *
     * @return the map of ignored properties by class, or {@code null} if none are configured
     */
    public Map<Class<?>, Set<String>> getIgnoredPropNames() {
        return ignoredBeanPropNameMap;
    }

    /**
     * Gets the ignored property names for a specific class.
     *
     * <p>This method first looks for class-specific ignored properties. If none
     * are found (no entry for the class, or an entry set to {@code null}), it returns the globally
     * ignored properties (those registered for {@code Object.class}). An empty class-specific set is
     * found and returned as-is, so it overrides the global set.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> ignoredProps = config.getIgnoredPropNames(User.class);
     * if (ignoredProps != null) {
     *     // Process ignored properties
     * }
     * }</pre>
     *
     * @param targetClass the class to get ignored properties for
     * @return collection of ignored property names, or {@code null} if none are configured
     */
    @MayReturnNull
    public Collection<String> getIgnoredPropNames(final Class<?> targetClass) {
        if (ignoredBeanPropNameMap == null) {
            return null; // NOSONAR
        }

        Collection<String> result = ignoredBeanPropNameMap.get(targetClass);

        if (result == null) {
            result = ignoredBeanPropNameMap.get(Object.class);
        }

        return result;
    }

    /**
     * Sets globally ignored property names that apply to all classes.
     *
     * <p>These properties will be ignored during parsing for any class type
     * unless overridden by class-specific settings.</p>
     *
     * <p>This is a shortcut for {@code setIgnoredPropNames(Object.class, ignoredPropNames)}. Passing
     * {@code null} therefore does not remove the map: it stores a {@code null} entry under
     * {@code Object.class}, after which {@link #getIgnoredPropNames()} returns a (non-null) map containing
     * {@code {Object.class=null}} and no property is ignored globally. To clear everything (so that
     * {@link #getIgnoredPropNames()} returns {@code null} again) use {@link #setIgnoredPropNames(Map)} with
     * {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setIgnoredPropNames(Set.of("internalId", "version", "deleted"));
     *
     * config.setIgnoredPropNames((Set<String>) null);                 // no global ignores, but getIgnoredPropNames() is {Object.class=null}
     * config.setIgnoredPropNames((Map<Class<?>, Set<String>>) null);  // getIgnoredPropNames() is null again
     * }</pre>
     *
     * @param ignoredPropNames set of property names to ignore globally; {@code null} keeps an (empty) entry for {@code Object.class}
     * @return this configuration instance for method chaining
     * @throws UnsupportedOperationException if the previously supplied ignored-property map does not support adding or replacing the entry.
     */
    public C setIgnoredPropNames(final Set<String> ignoredPropNames) throws UnsupportedOperationException {
        return setIgnoredPropNames(Object.class, ignoredPropNames);
    }

    /**
     * Sets ignored property names for a specific class.
     *
     * <p>These properties will be ignored during parsing only for the specified
     * class type. A non-null set overrides the global settings for this class - including an
     * <i>empty</i> set, which makes the class ignore nothing even when a global set exists.
     * Passing {@code null} does not override anything: it removes the effect of any previous
     * class-specific entry and lets the global set apply to this class again
     * (see {@link #getIgnoredPropNames(Class)}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setIgnoredPropNames(User.class, Set.of("password", "salt"));
     * config.setIgnoredPropNames(Order.class, Set.of("internalNotes"));
     *
     * config.setIgnoredPropNames(Set.of("version"));      // global
     * config.setIgnoredPropNames(Audit.class, Set.of());  // Audit ignores nothing (overrides the global set)
     * config.setIgnoredPropNames(User.class, null);       // User falls back to the global set: ["version"]
     * }</pre>
     *
     * @param targetClass the class to set ignored properties for
     * @param ignoredPropNames set of property names to ignore for this class; an empty set overrides the global set with "nothing", {@code null} removes the class-specific override so the global set applies again
     * @return this configuration instance for method chaining
     * @throws UnsupportedOperationException if the previously supplied ignored-property map does not support adding or replacing the entry.
     */
    public C setIgnoredPropNames(final Class<?> targetClass, final Set<String> ignoredPropNames) throws UnsupportedOperationException {
        if (ignoredBeanPropNameMap == null) {
            ignoredBeanPropNameMap = new HashMap<>();
        }

        ignoredBeanPropNameMap.put(targetClass, ignoredPropNames);

        return (C) this;
    }

    /**
     * Sets the complete map of ignored property names by class.
     *
     * <p>This replaces any existing ignored property configuration with the
     * provided map. Use {@code Object.class} as a key for globally ignored
     * properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Class<?>, Set<String>> ignoredMap = new HashMap<>();
     * ignoredMap.put(Object.class, Set.of("version"));
     * ignoredMap.put(User.class, Set.of("password"));
     * config.setIgnoredPropNames(ignoredMap);
     * }</pre>
     *
     * @param ignoredPropNames complete map of ignored properties by class
     * @return this configuration instance for method chaining
     */
    public C setIgnoredPropNames(final Map<Class<?>, Set<String>> ignoredPropNames) {
        ignoredBeanPropNameMap = ignoredPropNames;

        return (C) this;
    }

    /**
     * Creates a copy of this configuration.
     *
     * <p>The ignored-property map is copied, so {@link #setIgnoredPropNames(Class, Set)} /
     * {@link #setIgnoredPropNames(Set)} / {@link #setIgnoredPropNames(Map)} on the copy never affects the
     * original (and vice versa), regardless of whether the original already had ignored properties when it
     * was copied. The sets it holds are shared, but the setters replace an entry rather than mutate a set,
     * so a shared set only changes if the caller mutates it directly. Subclasses copy further maps:
     * {@link DeserializationConfig#copy()} copies the value-type map and {@link JsonDeserConfig#copy()}
     * additionally copies the property-handler map.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig original = new JsonSerConfig().setPrettyFormat(true);
     * JsonSerConfig copy = original.copy();
     * // copy has the same settings as original
     *
     * copy.setIgnoredPropNames(User.class, Set.of("password"));
     * original.getIgnoredPropNames(User.class);   // returns null - the original is untouched
     * }</pre>
     *
     * @return a copy of this configuration with its own ignored-property map
     */
    @SuppressWarnings("unchecked")
    public C copy() {
        try {
            final C copy = (C) super.clone();

            if (ignoredBeanPropNameMap != null) {
                copy.ignoredBeanPropNameMap = new HashMap<>(ignoredBeanPropNameMap);
            }

            return copy;
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e); // should never happen.
        }
    }

    // These maps are used only for options without a public live-map getter. A parser can
    // snapshot them for each nested value without copying every unrelated option entry.
    static <K, V> Map<K, V> newConfigMap() {
        return new ConfigMap<>(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> copyConfigMap(final Map<K, V> source) {
        if (source instanceof ConfigMap<?, ?> owned) {
            return ((ConfigMap<K, V>) owned).snapshot();
        }

        // setValueTypes(Map) keeps an external alias: mutations cannot be intercepted,
        // so each snapshot must still copy that map before adopting its private storage.
        return new ConfigMap<>(new HashMap<>(source));
    }

    private static final class ConfigMap<K, V> extends AbstractMap<K, V> {
        private Map<K, V> values;
        private volatile boolean shared;

        ConfigMap(final Map<K, V> values) {
            this.values = values;
        }

        ConfigMap<K, V> snapshot() {
            // The flag turns true only here (the writers reset it once they own a private copy of the map), so
            // the volatile store is skipped while it is already set: a configuration shared by many parsing
            // threads is snapshotted for every nested value, and an unconditional store would keep bouncing
            // this cache line (which also holds the lookup map reference) between their cores.
            if (!shared) {
                shared = true;
            }

            final ConfigMap<K, V> copy = new ConfigMap<>(values);
            copy.shared = true;
            return copy;
        }

        private void prepareWrite() {
            if (shared) {
                values = new HashMap<>(values);
                shared = false;
            }
        }

        @Override
        public V get(final Object key) {
            return values.get(key);
        }

        @Override
        public boolean containsKey(final Object key) {
            return values.containsKey(key);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public V put(final K key, final V value) {
            prepareWrite();
            return values.put(key, value);
        }

        @Override
        public void putAll(final Map<? extends K, ? extends V> map) {
            prepareWrite();
            values.putAll(map);
        }

        @Override
        public V remove(final Object key) {
            prepareWrite();
            return values.remove(key);
        }

        @Override
        public void clear() {
            values = new HashMap<>();
            shared = false;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            // No mutable view may bypass prepareWrite(). The containing configuration
            // exposes individual option setters, not this internal map or its entries.
            return Collections.unmodifiableMap(values).entrySet();
        }
    }
}
