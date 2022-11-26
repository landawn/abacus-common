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

package com.landawn.abacus.parser;

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @param <C>
 * @since 0.8
 */
public abstract class DeserializationConfig<C extends DeserializationConfig<C>> extends ParserConfig<C> {

    boolean ignoreUnmatchedProperty = true;

    Type<?> elementType;

    Type<?> keyType;

    Type<?> valueType;

    Map<String, Type<?>> propTypes;

    /**
     * Checks if is ignore unknown property.
     *
     * @return true, if is ignore unknown property
     */
    public boolean ignoreUnmatchedProperty() {
        return ignoreUnmatchedProperty;
    }

    /**
     * Sets the ignore unknown property.
     *
     * @param ignoreUnmatchedProperty
     * @return
     */
    public C ignoreUnmatchedProperty(boolean ignoreUnmatchedProperty) {
        this.ignoreUnmatchedProperty = ignoreUnmatchedProperty;

        return (C) this;
    }

    /**
     * Gets the element type.
     *
     * @param <T>
     * @return
     */
    public <T> Type<T> getElementType() {
        return (Type<T>) elementType;
    }

    /**
     * Sets the element type.
     *
     * @param cls
     * @return
     */
    public C setElementType(Class<?> cls) {
        return setElementType(N.typeOf(cls));
    }

    /**
     * Sets the element type.
     *
     * @param type
     * @return
     */
    public C setElementType(Type<?> type) {
        this.elementType = type;

        return (C) this;
    }

    /**
     * Sets the element type.
     *
     * @param type
     * @return
     */
    public C setElementType(String type) {
        return setElementType(N.typeOf(type));
    }

    /**
     * Gets the map key type.
     *
     * @param <T>
     * @return
     */
    public <T> Type<T> getMapKeyType() {
        return (Type<T>) keyType;
    }

    /**
     * Sets the map key type.
     *
     * @param cls
     * @return
     */
    public C setMapKeyType(Class<?> cls) {
        return this.setMapKeyType(N.typeOf(cls));
    }

    /**
     * Sets the map key type.
     *
     * @param keyType
     * @return
     */
    public C setMapKeyType(Type<?> keyType) {
        this.keyType = keyType;

        return (C) this;
    }

    /**
     * Sets the map key type.
     *
     * @param keyType
     * @return
     */
    public C setMapKeyType(String keyType) {
        return this.setMapKeyType(N.typeOf(keyType));
    }

    /**
     * Gets the map value type.
     *
     * @param <T>
     * @return
     */
    public <T> Type<T> getMapValueType() {
        return (Type<T>) valueType;
    }

    /**
     * Sets the map value type.
     *
     * @param cls
     * @return
     */
    public C setMapValueType(Class<?> cls) {
        return this.setMapValueType(N.typeOf(cls));
    }

    /**
     * Sets the map value type.
     *
     * @param valueType
     * @return
     */
    public C setMapValueType(Type<?> valueType) {
        this.valueType = valueType;

        return (C) this;
    }

    /**
     * Sets the map value type.
     *
     * @param valueType
     * @return
     */
    public C setMapValueType(String valueType) {
        return this.setMapValueType(N.typeOf(valueType));
    }

    /**
     * Gets the prop types.
     *
     * @return
     */
    public Map<String, Type<?>> getPropTypes() {
        return propTypes;
    }

    /**
     * Sets the prop types.
     *
     * @param propTypes
     * @return
     */
    public C setPropTypes(Map<String, Type<?>> propTypes) {
        this.propTypes = propTypes;

        return (C) this;
    }

    /**
     * Gets the prop type.
     *
     * @param <T>
     * @param propName
     * @return
     */
    public <T> Type<T> getPropType(String propName) {
        return propTypes == null ? null : (Type<T>) propTypes.get(propName);
    }

    /**
     * Sets the prop type.
     *
     * @param propName
     * @param cls
     * @return
     */
    public C setPropType(String propName, Class<?> cls) {
        return setPropType(propName, N.typeOf(cls));
    }

    /**
     * Sets the prop type.
     *
     * @param propName
     * @param type
     * @return
     */
    public C setPropType(String propName, Type<?> type) {
        if (propTypes == null) {
            propTypes = new HashMap<>();
        }

        this.propTypes.put(propName, type);

        return (C) this;
    }

    /**
     * Sets the prop type.
     *
     * @param propName
     * @param type
     * @return
     */
    public C setPropType(String propName, String type) {
        return setPropType(propName, N.typeOf(type));
    }

    /**
     * Checks for prop type.
     *
     * @param propName
     * @return true, if successful
     */
    public boolean hasPropType(String propName) {
        return propTypes != null && propTypes.containsKey(propName);
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(keyType);
        h = 31 * h + N.hashCode(valueType);
        return 31 * h + N.hashCode(propTypes);
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DeserializationConfig) {
            final DeserializationConfig<C> other = (DeserializationConfig<C>) obj;

            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty)
                    && N.equals(elementType, other.elementType) && N.equals(keyType, other.keyType) && N.equals(valueType, other.valueType)
                    && N.equals(propTypes, other.propTypes)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty) + ", elementType="
                + N.toString(elementType) + ", keyType=" + N.toString(keyType) + ", valueType=" + N.toString(valueType) + ", propTypes=" + N.toString(propTypes)
                + "}";
    }
}
