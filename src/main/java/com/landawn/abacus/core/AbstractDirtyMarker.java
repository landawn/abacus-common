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

package com.landawn.abacus.core;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@JsonIgnoreProperties({ "dirty" })
@AccessFieldByMethod
public abstract class AbstractDirtyMarker implements DirtyMarker {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDirtyMarker.class);

    /**
     * The field name of <code>DirtyMarker</code> implementation in a generated entity class which implements the <code>DirtyMarker</code> interface. (value is ""dirtyMarkerImpl"")
     */
    private static final String DIRTY_MARKER_IMPL_FIELD_NAME = "dirtyMarkerImpl";

    protected final String entityName;

    Set<String> signedPropNames = N.emptySet();

    Set<String> dirtyPropNames = N.emptySet();

    long version = -1;

    boolean isFrozen = false;

    // for kryo
    AbstractDirtyMarker() {
        this(N.EMPTY_STRING);
    }

    protected AbstractDirtyMarker(String entityName) {
        this.entityName = entityName;

        init();
    }

    /**
     * Inits the.
     */
    protected void init() {
        signedPropNames = N.newHashSet();
        dirtyPropNames = N.newHashSet();
    }

    @Override
    public String entityName() {
        return entityName;
    }

    /**
     * Checks if is dirty.
     *
     * @return true, if is dirty
     */
    @Override
    @XmlTransient
    public boolean isDirty() {
        return dirtyPropNames.size() > 0;
    }

    /**
     * Checks if is dirty.
     *
     * @param propName
     * @return true, if is dirty
     */
    @Override
    @XmlTransient
    public boolean isDirty(String propName) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            return dirtyPropNames.contains(NameUtil.getSimpleName(propName));
        } else {
            return dirtyPropNames.contains(propName);
        }
    }

    /**
     *
     * @param isDirty
     */
    @Override
    public void markDirty(boolean isDirty) {
        checkForzen();

        if (isDirty) {
            dirtyPropNames.addAll(signedPropNames());
        } else {
            dirtyPropNames.clear();
        }
    }

    /**
     *
     * @param propName
     * @param isDirty
     */
    @Override
    public void markDirty(String propName, boolean isDirty) {
        checkForzen();

        if (isDirty) {
            if (NameUtil.isCanonicalName(entityName, propName)) {
                dirtyPropNames.add(NameUtil.getSimpleName(propName));
            } else {
                dirtyPropNames.add(propName);
            }
        } else {
            if (NameUtil.isCanonicalName(entityName, propName)) {
                dirtyPropNames.remove(NameUtil.getSimpleName(propName));
            } else {
                dirtyPropNames.remove(propName);
            }
        }
    }

    /**
     *
     * @param propNames
     * @param isDirty
     */
    @Override
    public void markDirty(Collection<String> propNames, boolean isDirty) {
        checkForzen();

        if (isDirty) {
            for (String propName : propNames) {
                if (NameUtil.isCanonicalName(entityName, propName)) {
                    dirtyPropNames.add(NameUtil.getSimpleName(propName));
                } else {
                    dirtyPropNames.add(propName);
                }
            }
        } else {
            for (String propName : propNames) {
                if (NameUtil.isCanonicalName(entityName, propName)) {
                    dirtyPropNames.remove(NameUtil.getSimpleName(propName));
                } else {
                    dirtyPropNames.remove(propName);
                }
            }
        }
    }

    /**
     * Signed prop names.
     *
     * @return
     */
    @Override
    @XmlTransient
    public Set<String> signedPropNames() {
        return signedPropNames;
    }

    /**
     * Dirty prop names.
     *
     * @return
     */
    @Override
    @XmlTransient
    public Set<String> dirtyPropNames() {
        return dirtyPropNames;
    }

    @Override
    @XmlTransient
    public long version() {
        return version;
    }

    /**
     * Freeze.
     */
    @Override
    public void freeze() {
        isFrozen = true;
    }

    /**
     *
     * @return true, if successful
     */
    @Override
    @XmlTransient
    public boolean frozen() {
        return isFrozen;
    }

    /**
     * Sets the updated prop name.
     *
     * @param propName the new updated prop name
     */
    protected void setUpdatedPropName(String propName) {
        checkForzen();

        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        signedPropNames.add(propName);
        dirtyPropNames.add(propName);
    }

    /**
     * Sets the updated prop names.
     *
     * @param propNames the new updated prop names
     */
    protected void setUpdatedPropNames(Collection<String> propNames) {
        checkForzen();

        for (String propName : propNames) {
            if (NameUtil.isCanonicalName(entityName, propName)) {
                propName = NameUtil.getSimpleName(propName);
            }

            signedPropNames.add(propName);
            dirtyPropNames.add(propName);
        }
    }

    /**
     * Check forzen.
     */
    protected void checkForzen() {
        if (isFrozen) {
            throw new IllegalStateException("This Entity is frozen, can't modify it.");
        }
    }

    /**
     * Checks if is entity dirty.
     *
     * @param entities
     * @return true, if is entity dirty
     */
    @SuppressWarnings("deprecation")
    protected boolean isEntityDirty(Collection<? extends DirtyMarker> entities) {
        if (entities == null) {
            return false;
        } else {
            for (DirtyMarker entity : entities) {
                if (entity.isDirty()) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Mark entity dirty.
     *
     * @param entities
     * @param isDirty
     */
    @SuppressWarnings("deprecation")
    protected void markEntityDirty(Collection<? extends DirtyMarker> entities, boolean isDirty) {
        if (entities != null) {
            for (DirtyMarker entity : entities) {
                entity.markDirty(isDirty);
            }
        }
    }

    /**
     * Gets the dirty marker impl.
     *
     * @param entity
     * @return
     */
    static AbstractDirtyMarker getDirtyMarkerImpl(DirtyMarker entity) {
        Object dirtyMarkerImpl = null;

        try {
            final Field field = entity.getClass().getDeclaredField(DIRTY_MARKER_IMPL_FIELD_NAME);
            ClassUtil.setAccessibleQuietly(field, true);
            dirtyMarkerImpl = field.get(entity);
        } catch (NoSuchFieldException e) {
            // ignore;

            if (logger.isWarnEnabled()) {
                logger.warn(ExceptionUtil.getMessage(e));
            }
        } catch (IllegalAccessException e) {
            // ignore;

            if (logger.isWarnEnabled()) {
                logger.warn(ExceptionUtil.getMessage(e));
            }
        }

        if (dirtyMarkerImpl != null && dirtyMarkerImpl instanceof AbstractDirtyMarker) {
            return (AbstractDirtyMarker) dirtyMarkerImpl;
        } else {
            return null;
        }
    }
}
