/*
 * Copyright (c) 2019, Haiyang Li.
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

package com.landawn.abacus.core;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.ObjectPool;

/**
 *
 * @author Haiyang Li
 * @since 1.8.7
 */
@Internal
public final class DirtyMarkerUtil {

    static final int POOL_SIZE;

    static {
        int multi = (int) (Runtime.getRuntime().maxMemory() / ((1024 * 1024) * 256));

        POOL_SIZE = Math.max(1000, Math.min(1000 * multi, 8192));
    }

    private DirtyMarkerUtil() {
        // no instance.
    }

    private static final Map<Class<?>, Boolean> dirtyMarkerClassPool = new ObjectPool<>(POOL_SIZE);

    /**
     * Checks if is dirty marker.
     *
     * @param cls
     * @return true, if is dirty marker
     */
    public static boolean isDirtyMarker(final Class<?> cls) {
        Boolean b = dirtyMarkerClassPool.get(cls);

        if (b == null) {
            b = DirtyMarker.class.isAssignableFrom(cls);
            dirtyMarkerClassPool.put(cls, b);
        }

        return b;
    }

    /**
     * Checks if is dirty.
     *
     * @param entity
     * @return true, if is dirty
     */
    @SuppressWarnings("deprecation")
    public static boolean isDirty(final DirtyMarker entity) {
        return entity.isDirty();
    }

    /**
     * Checks if is dirty.
     *
     * @param entity
     * @param propName
     * @return true, if is dirty
     */
    @SuppressWarnings("deprecation")
    public static boolean isDirty(final DirtyMarker entity, final String propName) {
        return entity.isDirty(propName);
    }

    /**
     *
     * @param entity
     * @param isDirty
     */
    @SuppressWarnings("deprecation")
    public static void markDirty(final DirtyMarker entity, final boolean isDirty) {
        entity.markDirty(isDirty);
    }

    /**
     *
     * @param entity
     * @param propName
     * @param isDirty
     */
    @SuppressWarnings("deprecation")
    public static void markDirty(final DirtyMarker entity, final String propName, final boolean isDirty) {
        entity.markDirty(propName, isDirty);
    }

    /**
     *
     * @param entity
     * @param propNames
     * @param isDirty
     */
    @SuppressWarnings("deprecation")
    public static void markDirty(final DirtyMarker entity, final Collection<String> propNames, final boolean isDirty) {
        entity.markDirty(propNames, isDirty);
    }

    /**
     * Signed prop names.
     *
     * @param entity
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Set<String> signedPropNames(final DirtyMarker entity) {
        return entity.signedPropNames();
    }

    /**
     * Dirty prop names.
     *
     * @param entity
     * @return
     */
    @SuppressWarnings("deprecation")
    public static Set<String> dirtyPropNames(final DirtyMarker entity) {
        return entity.dirtyPropNames();
    }

    /**
     * Sets the dirty marker.
     *
     * @param source
     * @param target
     */
    @SuppressWarnings("deprecation")
    public static void setDirtyMarker(final Object source, final Object target) {
        if (source instanceof DirtyMarker && target instanceof DirtyMarker) {
            DirtyMarker dirtyMarkerSource = (DirtyMarker) source;
            DirtyMarker dirtyMarkerTarget = (DirtyMarker) target;

            dirtyMarkerTarget.signedPropNames().clear();
            dirtyMarkerTarget.signedPropNames().addAll(dirtyMarkerSource.signedPropNames());

            dirtyMarkerTarget.dirtyPropNames().clear();
            dirtyMarkerTarget.dirtyPropNames().addAll(dirtyMarkerSource.dirtyPropNames());

            setVersion(dirtyMarkerTarget, dirtyMarkerSource.version());
        }
    }

    /**
     * Sets the version.
     *
     * @param targetEntity
     * @param version
     */
    public static void setVersion(final DirtyMarker targetEntity, final long version) {
        AbstractDirtyMarker dirtyMarker = null;

        if (targetEntity instanceof AbstractDirtyMarker) {
            dirtyMarker = (AbstractDirtyMarker) targetEntity;
        } else {
            dirtyMarker = AbstractDirtyMarker.getDirtyMarkerImpl(targetEntity);
        }

        if (dirtyMarker != null) {
            (dirtyMarker).version = version;
        }
    }
}
