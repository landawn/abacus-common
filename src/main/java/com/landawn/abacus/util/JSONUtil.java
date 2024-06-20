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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class JSONUtil {

    private JSONUtil() {
        // singleton.
    }

    /**
     *
     * @param map
     * @return
     */
    public static JSONObject wrap(final Map<String, ?> map) {
        return new JSONObject(map);
    }

    /**
     * wrap(bean) -> wrap(Maps.deepBean2Map(bean, true))
     *
     * @param bean
     * @return
     * @see Maps#deepBean2Map(Object)
     */
    @SuppressWarnings("unchecked")
    public static JSONObject wrap(final Object bean) {
        return new JSONObject(bean instanceof Map ? (Map<String, Object>) bean : Maps.deepBean2Map(bean, true));
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final boolean[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final char[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final byte[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final short[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final int[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final long[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final float[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final double[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param array
     * @return
     * @throws JSONException the JSON exception
     */
    public static JSONArray wrap(final Object[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     *
     * @param coll
     * @return
     */
    public static JSONArray wrap(final Collection<?> coll) {
        return new JSONArray(coll);
    }

    /**
     *
     * @param jsonObject
     * @return
     * @throws JSONException the JSON exception
     */
    public static Map<String, Object> unwrap(final JSONObject jsonObject) throws JSONException {
        return unwrap(jsonObject, Map.class);
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonObject 
     * @param targetType 
     * @return 
     * @throws JSONException the JSON exception
     */
    public static <T> T unwrap(final JSONObject jsonObject, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonObject, N.<T> typeOf(targetType));
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonObject 
     * @param targetType 
     * @return 
     * @throws JSONException the JSON exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType) throws JSONException {
        targetType = targetType.isObjectType() ? N.<T> typeOf("Map<String, Object>") : targetType;
        final Class<?> cls = targetType.clazz();

        if (targetType.clazz().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        } else if (targetType.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> map = N.newMap((Class<Map>) cls, jsonObject.keySet().size());
            final Iterator<String> iter = jsonObject.keys();
            final Type<?> valueType = targetType.getParameterTypes()[1];
            String key = null;
            Object value = null;

            while (iter.hasNext()) {
                key = iter.next();
                value = jsonObject.get(key);

                if (value == JSONObject.NULL) {
                    value = null;
                } else if (value != null) {
                    if (value instanceof JSONObject) {
                        value = unwrap((JSONObject) value, valueType);
                    } else if (value instanceof JSONArray) {
                        value = unwrap((JSONArray) value, valueType);
                    }
                }

                map.put(key, value);
            }

            return (T) map;
        } else if (targetType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
            final Object result = beanInfo.createBeanResult();
            final Iterator<String> iter = jsonObject.keys();
            String key = null;
            Object value = null;
            PropInfo propInfo = null;

            while (iter.hasNext()) {
                key = iter.next();
                value = jsonObject.get(key);

                propInfo = beanInfo.getPropInfo(key);

                if (value == JSONObject.NULL) {
                    value = null;
                } else if (value != null) {
                    if (value instanceof JSONObject) {
                        value = unwrap((JSONObject) value, propInfo.jsonXmlType);
                    } else if (value instanceof JSONArray) {
                        value = unwrap((JSONArray) value, propInfo.jsonXmlType);
                    }
                }

                propInfo.setPropValue(result, value);
            }

            return (T) beanInfo.finishBeanResult(result);
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a map or bean type");
        }
    }

    /**
     *
     * @param <T>
     * @param jsonArray
     * @return
     * @throws JSONException the JSON exception
     */
    public static <T> List<T> unwrap(final JSONArray jsonArray) throws JSONException {
        return (List<T>) toList(jsonArray, Object.class);
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonArray 
     * @param targetType array or collection class
     * @return 
     * @throws JSONException the JSON exception
     */
    public static <T> T unwrap(final JSONArray jsonArray, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonArray, N.<T> typeOf(targetType));
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonArray 
     * @param targetType 
     * @return 
     * @throws JSONException the JSON exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType) throws JSONException {
        targetType = targetType.isObjectType() ? N.<T> typeOf("List<Object>") : targetType;
        final int len = jsonArray.length();

        if (targetType.clazz().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        } else if (targetType.isCollection()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> coll = N.newCollection((Class<Collection>) targetType.clazz(), len);
            final Type<?> elementType = targetType.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap((JSONObject) element, elementType);
                    } else if (element instanceof JSONArray) {
                        element = unwrap((JSONArray) element, elementType);
                    }
                }

                coll.add(element);
            }

            return (T) coll;
        } else if (targetType.isPrimitiveArray()) {
            final Object array = N.newArray(targetType.getElementType().clazz(), jsonArray.length());
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                }

                if (element == null) {
                    element = targetType.getElementType().defaultValue();
                }

                Array.set(array, i, element);
            }

            return (T) array;
        } else if (targetType.isArray()) {
            final Object[] array = N.newArray(targetType.getElementType().clazz(), jsonArray.length());
            final Type<?> elementType = targetType.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap((JSONObject) element, elementType);
                    } else if (element instanceof JSONArray) {
                        element = unwrap((JSONArray) element, elementType);
                    }
                }

                array[i] = element;
            }

            return (T) array;
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a array or collection type");
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonArray 
     * @param elementClass 
     * @return 
     * @throws JSONException the JSON exception
     */
    public static <T> List<T> toList(final JSONArray jsonArray, Class<? extends T> elementClass) throws JSONException {
        return toList(jsonArray, Type.of(elementClass));
    }

    /**
     * 
     *
     * @param <T> 
     * @param jsonArray 
     * @param elementType 
     * @return 
     * @throws JSONException the JSON exception
     */
    public static <T> List<T> toList(final JSONArray jsonArray, Type<T> elementType) throws JSONException {
        final int len = jsonArray.length();
        final List<Object> coll = new ArrayList<>(len);

        Object element = null;

        for (int i = 0; i < len; i++) {
            element = jsonArray.get(i);

            if (element == JSONObject.NULL) {
                element = null;
            } else if (element != null) {
                if (element instanceof JSONObject) {
                    element = unwrap((JSONObject) element, elementType);
                } else if (element instanceof JSONArray) {
                    element = unwrap((JSONArray) element, elementType);
                }
            }

            coll.add(element);
        }

        return (List<T>) coll;
    }
}
