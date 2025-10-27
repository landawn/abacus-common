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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 * Abstract base class for parser configuration that provides common settings
 * shared by both serialization and deserialization configurations.
 * 
 * <p>This class provides functionality for managing ignored properties during
 * parsing operations. Properties can be ignored globally or on a per-class basis.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ParserConfig config = new MyParserConfig()
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
     * @return the map of ignored properties by class, or {@code null} if none are configured
     */
    @MayReturnNull
    public Map<Class<?>, Set<String>> getIgnoredPropNames() {
        return ignoredBeanPropNameMap;
    }

    /**
     * Gets the ignored property names for a specific class.
     * 
     * <p>This method first looks for class-specific ignored properties. If none
     * are found, it returns the globally ignored properties (those registered
     * for {@code Object.class}).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> ignoredProps = config.getIgnoredPropNames(User.class);
     * if (ignoredProps != null) {
     *     // Process ignored properties
     * }
     * }</pre>
     *
     * @param cls the class to get ignored properties for
     * @return collection of ignored property names, or {@code null} if none are configured
     */
    @MayReturnNull
    public Collection<String> getIgnoredPropNames(final Class<?> cls) {
        if (ignoredBeanPropNameMap == null) {
            return null; // NOSONAR
        }

        Collection<String> result = ignoredBeanPropNameMap.get(cls);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setIgnoredPropNames(Set.of("internalId", "version", "deleted"));
     * }</pre>
     *
     * @param ignoredPropNames set of property names to ignore globally
     * @return this configuration instance for method chaining
     */
    public C setIgnoredPropNames(final Set<String> ignoredPropNames) {
        return setIgnoredPropNames(Object.class, ignoredPropNames);
    }

    /**
     * Sets ignored property names for a specific class.
     * 
     * <p>These properties will be ignored during parsing only for the specified
     * class type. This overrides any global settings for this class.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setIgnoredPropNames(User.class, Set.of("password", "salt"));
     * config.setIgnoredPropNames(Order.class, Set.of("internalNotes"));
     * }</pre>
     *
     * @param cls the class to set ignored properties for
     * @param ignoredPropNames set of property names to ignore for this class
     * @return this configuration instance for method chaining
     */
    public C setIgnoredPropNames(final Class<?> cls, final Set<String> ignoredPropNames) {
        if (ignoredBeanPropNameMap == null) {
            ignoredBeanPropNameMap = new HashMap<>();
        }

        ignoredBeanPropNameMap.put(cls, ignoredPropNames);

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
     * <p>The copy is a shallow clone - the ignored property map and sets are
     * shared with the original. Modifications to the configuration itself are
     * independent, but modifications to the property sets affect both instances.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParserConfig copy = originalConfig.copy();
     * // copy has the same settings as originalConfig
     * }</pre>
     *
     * @return a copy of this configuration
     */
    public C copy() {
        try {
            return (C) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e); // should never happen.
        }
    }
}
