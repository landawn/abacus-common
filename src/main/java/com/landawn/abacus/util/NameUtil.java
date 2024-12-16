/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * It's tool for the conversion by between the simple name and canonical name.
 *
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

    private NameUtil() {
        // singleton
    }

    /**
     * Checks if is cached name.
     *
     * @param str
     * @return {@code true}, if is cached name
     */
    public static boolean isCachedName(final String str) {
        return cachedNamePool.containsKey(str);
    }

    /**
     * Gets the cached name.
     *
     * @param str
     * @return
     */
    public static String getCachedName(final String str) {
        String cachedString = cachedNamePool.get(str);

        if (cachedString == null) {
            cachedString = cacheName(str, false);
        }

        return cachedString;
    }

    /**
     *
     * @param name
     * @param force ignore the request if already cached.
     * @return
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
     * Checks if is canonical name.
     *
     * @param parentName
     * @param name
     * @return {@code true} if the specified name starts with parentName + "."
     */
    public static boolean isCanonicalName(final String parentName, final String name) {
        return name.length() > parentName.length() && name.charAt(parentName.length()) == '.' && parentName.equals(getParentName(name));
    }

    //    /**
    //     * Gets the canonical name.
    //     *
    //     * @param parentName
    //     * @param name
    //     * @return
    //     *         or the original string if the specified <code>name</code> starts with <code>parentName + "."</code>, otherwise, an empty string "".
    //     */
    //    public static String getCanonicalName(String parentName, String name) {
    //        String canonicalName = null;
    //        Map<String, String> canonicalNameMap = parentCanonicalNamePool.get(parentName);
    //
    //        if (canonicalNameMap == null) {
    //            synchronized (parentCanonicalNamePool) {
    //                canonicalNameMap = parentCanonicalNamePool.get(parentName);
    //
    //                if (canonicalNameMap == null) {
    //                    canonicalNameMap = new ConcurrentHashMap<>();
    //                    parentCanonicalNamePool.put(parentName, canonicalNameMap);
    //                }
    //            }
    //        }
    //
    //        canonicalName = canonicalNameMap.get(name);
    //
    //        if (canonicalName == null) {
    //            int idx = name.indexOf(WD._PERIOD);
    //
    //            if (idx < 0) {
    //                canonicalName = getCachedName(parentName + "." + name);
    //            } else if (parentName.equals(getParentName(name))) {
    //                canonicalName = getCachedName(canonicalName);
    //            } else {
    //                canonicalName = Strings.EMPTY_STRING;
    //            }
    //
    //            canonicalNameMap.put(name, canonicalName);
    //        }
    //
    //        return canonicalName;
    //    }

    /**
     * Gets the simple name.
     *
     * @param name
     * @return
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
     * Gets the parent name.
     *
     * @param name
     * @return an empty String "" if the specified {@code name} is not a canonical property name.
     */
    public static String getParentName(final String name) {
        String parentName = parentNamePool.get(name);

        if (parentName == null) {
            final int index = name.lastIndexOf(WD._PERIOD);

            if (index < 1) {
                parentName = Strings.EMPTY_STRING;
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
