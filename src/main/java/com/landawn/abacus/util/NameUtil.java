/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.Map;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * A utility class for efficient string name management and conversion between
 * simple names and canonical names.
 * 
 * <p>This class provides caching mechanisms to optimize memory usage when working
 * with frequently used string names, particularly useful for property names,
 * field names, and other identifiers in object mapping and serialization contexts.</p>
 * 
 * <p>The utility maintains several internal caches:</p>
 * <ul>
 *   <li>Cached name pool - for frequently used string names</li>
 *   <li>Simple name pool - for extracted simple names from canonical names</li>
 *   <li>Parent name pool - for parent portions of canonical names</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> This class is marked as {@code @Internal} and is not
 * intended for direct use by client code. It's designed for internal framework use.</p>
 * 
 * <p>Example of name structures:</p>
 * <pre>{@code
 * Canonical name: "com.example.Person.firstName"
 * Parent name: "com.example.Person"
 * Simple name: "firstName"
 * }</pre>
 * 
 * @see ObjectPool
 */
@Internal
@SuppressFBWarnings("JLM_JSR166_UTILCONCURRENT_MONITORENTER")
public final class NameUtil {

    @SuppressWarnings("deprecation")
    static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<String, String> cachedNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<String, String> simpleNamePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<String, String> parentNamePool = new ObjectPool<>(POOL_SIZE);

    // private static final Map<String, Map<String, String>> parentCanonicalNamePool = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private NameUtil() {
        // singleton
    }

    /**
     * Checks if the specified string is present in the cached name pool.
     * 
     * <p>This method is useful for determining whether a string has already been
     * cached and thus can be retrieved efficiently without additional processing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (NameUtil.isCachedName("firstName")) {
     *     // Name is already cached
     * }
     * }</pre>
     * 
     * @param str the string to check for presence in the cache
     * @return {@code true} if the string is in the cached name pool, {@code false} otherwise
     */
    public static boolean isCachedName(final String str) {
        return cachedNamePool.containsKey(str);
    }

    /**
     * Retrieves a cached version of the specified string, caching it if necessary.
     * 
     * <p>If the string is not already in the cache, it will be added (if space permits)
     * and then returned. This ensures that frequently used strings share the same
     * reference, reducing memory usage.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String cached = NameUtil.getCachedName("firstName");
     * // All subsequent calls with "firstName" will return the same reference
     * }</pre>
     * 
     * @param str the string to retrieve from cache or add to cache
     * @return the cached version of the string
     */
    public static String getCachedName(final String str) {
        String cachedString = cachedNamePool.get(str);

        if (cachedString == null) {
            cachedString = cacheName(str, false);
        }

        return cachedString;
    }

    /**
     * Caches the specified name string in the internal pool.
     * 
     * <p>This method interns the string and adds it to the cache pool if there's
     * available space. The force parameter determines whether to bypass the
     * duplicate check.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String cached = NameUtil.cacheName("com.example.Person", false);
     * // The string is now interned and cached
     * }</pre>
     * 
     * @param name the name string to cache
     * @param force if {@code true}, ignores whether the name is already cached;
     *              if {@code false}, skips caching if already present
     * @return the interned version of the name string
     */
    public static String cacheName(String name, final boolean force) {
        synchronized (cachedNamePool) {
            if (cachedNamePool.size() < POOL_SIZE && (force || !cachedNamePool.containsKey(name))) {
                name = name.intern();

                cachedNamePool.put(name, name);
            }

            return name;
        }
    }

    /**
     * Checks if the specified name is a canonical name relative to the given parent name.
     * 
     * <p>A name is considered canonical relative to a parent if it starts with the
     * parent name followed by a period. This is useful for validating hierarchical
     * naming structures.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isCanonical = NameUtil.isCanonicalName("com.example.Person",
     *                                                "com.example.Person.firstName");
     * // Returns true
     *
     * isCanonical = NameUtil.isCanonicalName("com.example", "com.other.Class");
     * // Returns false
     * }</pre>
     * 
     * @param parentName the parent name to check against
     * @param name the name to verify as canonical
     * @return {@code true} if the name starts with parentName + ".", {@code false} otherwise
     */
    public static boolean isCanonicalName(final String parentName, final String name) {
        return name.length() > parentName.length() && name.charAt(parentName.length()) == '.' && parentName.equals(getParentName(name));
    }

    /**
     * Extracts the simple name from a canonical name.
     * 
     * <p>The simple name is the last component of a dot-separated canonical name.
     * For example, from "com.example.Person.firstName", this method returns "firstName".
     * If the name contains no dots, it returns the name as-is.</p>
     * 
     * <p>The result is cached for efficient repeated access.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String simple = NameUtil.getSimpleName("com.example.Person.firstName");
     * // Returns "firstName"
     *
     * String simple2 = NameUtil.getSimpleName("name");
     * // Returns "name"
     * }</pre>
     * 
     * @param name the canonical name from which to extract the simple name
     * @return the simple name (last component after the final dot)
     */
    public static String getSimpleName(final String name) {
        String simplePropName = simpleNamePool.get(name);

        if (simplePropName == null) {
            final int idx = name.lastIndexOf(WD._PERIOD);

            if (idx < 0) {
                simplePropName = getCachedName(name);
            } else {
                simplePropName = getCachedName(name.substring(idx + 1));
            }

            if (simpleNamePool.size() < POOL_SIZE) {
                simpleNamePool.put(name, simplePropName);
            }
        }

        return simplePropName;
    }

    /**
     * Extracts the parent name from a canonical name.
     * 
     * <p>The parent name is everything before the last dot in a canonical name.
     * For example, from "com.example.Person.firstName", this method returns
     * "com.example.Person". If the name contains no dots or only a leading dot,
     * it returns an empty string.</p>
     * 
     * <p>The result is cached for efficient repeated access.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String parent = NameUtil.getParentName("com.example.Person.firstName");
     * // Returns "com.example.Person"
     *
     * String parent2 = NameUtil.getParentName("simpleName");
     * // Returns ""
     *
     * String parent3 = NameUtil.getParentName(".hidden");
     * // Returns ""
     * }</pre>
     * 
     * @param name the canonical name from which to extract the parent name
     * @return the parent name (everything before the last dot), or an empty string
     *         if the name is not a canonical property name
     */
    public static String getParentName(final String name) {
        String parentName = parentNamePool.get(name);

        if (parentName == null) {
            final int index = name.lastIndexOf(WD._PERIOD);

            if (index < 1) {
                parentName = Strings.EMPTY;
            } else {
                parentName = NameUtil.getCachedName(name.substring(0, index));
            }

            if (parentNamePool.size() < POOL_SIZE) {
                parentNamePool.put(name, parentName);
            }
        }

        return parentName;
    }
}