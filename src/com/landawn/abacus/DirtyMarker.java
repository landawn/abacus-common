/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus;

import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landawn.abacus.annotation.Internal;

/**
 * Memo of the signed and modified properties in this entity instance.
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
@JsonIgnoreProperties({ "dirty" })
public interface DirtyMarker {
    /**
     * Returns the entity name of the simple class name.
     * 
     * @return
     */
    String entityName();

    /**
     * Returns the mapping record version when it's loaded.
     * 
     * @return long
     */
    @XmlTransient
    long version();

    /**
     * Check if there is any property modified in this entity.
     * 
     * @return boolean
     */
    @XmlTransient
    @Internal
    @Deprecated
    boolean isDirty();

    /**
     * Check if the specified property is modified.
     * 
     * @param propName
     * @return boolean
     */
    @XmlTransient
    @Internal
    @Deprecated
    boolean isDirty(String propName);

    /**
     * Set the dirty status for all signed properties.
     * 
     * @param isDirty
     */
    @Internal
    @Deprecated
    void markDirty(boolean isDirty);

    /**
     * Set the dirty status for the specified property
     * 
     * @param propName
     * @param isDirty
     */
    @Internal
    @Deprecated
    void markDirty(String propName, boolean isDirty);

    /**
     * Set the dirty status for the specified properties.
     * 
     * @param propNames
     * @param isDirty
     */
    @Internal
    @Deprecated
    void markDirty(Collection<String> propNames, boolean isDirty);

    /**
     * Returns the names of the signed properties. It's for the internal design and implementation in Abacus, should not be used externally.
     * 
     * 
     * @return Collection<String> Must not modify the returned collection
     */
    @XmlTransient
    @Internal
    @Deprecated
    Set<String> signedPropNames();

    /**
     * Returns the names of updated properties. It's for the internal design and implementation in Abacus, should not be used externally.
     * 
     * @return Collection<String>  Must not modify the returned collection
     */
    @XmlTransient
    @Internal
    @Deprecated
    Set<String> dirtyPropNames();

    /**
     * Method freeze.
     */
    @XmlTransient
    void freeze();

    /**
     * Method frozen.
     * 
     * @return
     */
    @XmlTransient
    boolean frozen();
}
