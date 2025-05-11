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

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;

/**
 *
 * @param <C>
 */
public abstract class DeserializationConfig<C extends DeserializationConfig<C>> extends ParserConfig<C> {

    boolean ignoreUnmatchedProperty = true;

    Type<?> elementType;

    Type<?> mapKeyType;

    Type<?> mapValueType;

    Map<String, Type<?>> valueTypeMap;

    BeanInfo beanInfoForValueTypes;

    /**
     * Checks if unknown property can be ignored.
     *
     * @return {@code true}, if unknown property can be ignored
     */
    public boolean ignoreUnmatchedProperty() {
        return ignoreUnmatchedProperty;
    }

    /**
     * Sets the ignored unknown property.
     *
     * @param ignoreUnmatchedProperty
     * @return
     */
    public C ignoreUnmatchedProperty(final boolean ignoreUnmatchedProperty) {
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
    public C setElementType(final Class<?> cls) {
        return setElementType(N.typeOf(cls));
    }

    /**
     * Sets the element type.
     *
     * @param type
     * @return
     */
    public C setElementType(final Type<?> type) {
        elementType = type;

        return (C) this;
    }

    /**
     * Sets the element type.
     *
     * @param type
     * @return
     */
    public C setElementType(final String type) {
        return setElementType(N.typeOf(type));
    }

    /**
     * Gets the map key type.
     *
     * @param <T>
     * @return
     */
    public <T> Type<T> getMapKeyType() {
        return (Type<T>) mapKeyType;
    }

    /**
     * Sets the map key type.
     *
     * @param cls
     * @return
     */
    public C setMapKeyType(final Class<?> cls) {
        return this.setMapKeyType(N.typeOf(cls));
    }

    /**
     * Sets the map key type.
     *
     * @param keyType
     * @return
     */
    public C setMapKeyType(final Type<?> keyType) {
        mapKeyType = keyType;

        return (C) this;
    }

    /**
     * Sets the map key type.
     *
     * @param keyType
     * @return
     */
    public C setMapKeyType(final String keyType) {
        return this.setMapKeyType(N.typeOf(keyType));
    }

    /**
     * Gets the map value type.
     *
     * @param <T>
     * @return
     */
    public <T> Type<T> getMapValueType() {
        return (Type<T>) mapValueType;
    }

    /**
     * Sets the map value type.
     *
     * @param cls
     * @return
     */
    public C setMapValueType(final Class<?> cls) {
        return this.setMapValueType(N.typeOf(cls));
    }

    /**
     * Sets the map value type.
     *
     * @param valueType
     * @return
     */
    public C setMapValueType(final Type<?> valueType) {
        mapValueType = valueType;

        return (C) this;
    }

    /**
     * Sets the map value type.
     *
     * @param valueType
     * @return
     */
    public C setMapValueType(final String valueType) {
        return this.setMapValueType(N.typeOf(valueType));
    }

    public boolean hasValueTypes() {
        return beanInfoForValueTypes != null || N.notEmpty(valueTypeMap);
    }

    /**
     * Gets the value type by key name.
     *
     * @param <T>
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @return
     */
    public <T> Type<T> getValueType(final String keyName) {
        return getValueType(keyName, null);
    }

    /**
     * Gets the value type by key name.
     *
     * @param <T>
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param defaultType
     * @return
     */
    public <T> Type<T> getValueType(final String keyName, final Type<T> defaultType) {
        Type<T> ret = null;

        if (valueTypeMap != null) {
            ret = (Type<T>) valueTypeMap.get(keyName);
        }

        if (ret == null && beanInfoForValueTypes != null) {
            final PropInfo propInfo = beanInfoForValueTypes.getPropInfo(keyName);

            if (propInfo != null) {
                ret = (Type<T>) propInfo.type;
            }
        }

        return ret == null ? defaultType : ret;
    }

    /**
     * Gets the value type class by key name.
     *
     * @param <T>
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @return
     */
    public <T> Class<T> getValueTypeClass(final String keyName) {
        return getValueTypeClass(keyName, null);
    }

    /**
     * Gets the value type class by key name.
     *
     * @param <T>
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param defaultTypeClass
     * @return
     */
    public <T> Class<T> getValueTypeClass(final String keyName, final Class<T> defaultTypeClass) {
        final Type<T> ret = getValueType(keyName);

        return ret == null ? defaultTypeClass : ret.clazz();
    }

    /**
     * Sets value type with key name.
     *
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param typeClass
     * @return
     */
    public C setValueType(final String keyName, final Class<?> typeClass) {
        return setValueType(keyName, N.typeOf(typeClass));
    }

    /**
     * Sets value type with key name.
     *
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param type
     * @return
     */
    public C setValueType(final String keyName, final Type<?> type) {
        if (valueTypeMap == null) {
            valueTypeMap = new HashMap<>();
        }

        valueTypeMap.put(keyName, type);

        return (C) this;
    }

    /**
     * Sets value type with key name.
     *
     * @param keyName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param typeName
     * @return
     */
    public C setValueType(final String keyName, final String typeName) {
        return setValueType(keyName, N.typeOf(typeName));
    }

    /**
     * Sets value types with key names.
     *
     * @param valueTypes
     * @return
     */
    public C setValueTypes(final Map<String, Type<?>> valueTypes) {
        valueTypeMap = valueTypes;

        return (C) this;
    }

    /**
     * Sets value types by bean class.
     *
     * @param beanClass
     * @return
     * @throws IllegalArgumentException
     */
    public C setValueTypesByBeanClass(final Class<?> beanClass) throws IllegalArgumentException {
        if (beanClass == null) {
            beanInfoForValueTypes = null;
        } else {
            N.checkArgument(ClassUtil.isBeanClass(beanClass), "{} is not a valid bean class", beanClass);

            beanInfoForValueTypes = ParserUtil.getBeanInfo(beanClass);
        }

        return (C) this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(mapKeyType);
        h = 31 * h + N.hashCode(mapValueType);
        h = 31 * h + N.hashCode(valueTypeMap);
        return 31 * h + N.hashCode(beanInfoForValueTypes);
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DeserializationConfig) {
            final DeserializationConfig<C> other = (DeserializationConfig<C>) obj;

            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty)
                    && N.equals(elementType, other.elementType) && N.equals(mapKeyType, other.mapKeyType) && N.equals(mapValueType, other.mapValueType)
                    && N.equals(valueTypeMap, other.valueTypeMap) && N.equals(beanInfoForValueTypes, other.beanInfoForValueTypes);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty) + ", elementType="
                + N.toString(elementType) + ", mapKeyType=" + N.toString(mapKeyType) + ", mapValueType=" + N.toString(mapValueType) + ", valueTypeMap="
                + N.toString(valueTypeMap) + ", beanInfoForValueTypes=" + N.toString(beanInfoForValueTypes) + "}";
    }
}
