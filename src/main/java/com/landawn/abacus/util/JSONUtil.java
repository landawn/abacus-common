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
 * The JSONUtil class provides utility methods for handling JSON data.
 * It includes methods for wrapping and unwrapping JSON objects and arrays, converting between JSON and Java data types,
 * and handling exceptions related to JSON processing.
 *
 */
public final class JSONUtil {

    private JSONUtil() {
        // singleton.
    }

    /**
     * Wraps the provided map into a JSONObject.
     *
     * @param map The map to be wrapped into a JSONObject.
     * @return A new JSONObject that represents the provided map.
     */
    public static JSONObject wrap(final Map<String, ?> map) {
        return new JSONObject(map);
    }

    /**
     * Wraps the provided bean into a JSONObject.
     *
     * @param bean The bean to be wrapped into a JSONObject. This can be a Map or any other object that can be converted into a Map.
     * @return A new JSONObject that represents the provided bean.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject wrap(final Object bean) {
        return new JSONObject(bean instanceof Map ? (Map<String, Object>) bean : Maps.deepBean2Map(bean, true));
    }

    /**
     * Wraps the provided boolean array into a JSONArray.
     *
     * @param array The boolean array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided boolean array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final boolean[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided character array into a JSONArray.
     *
     * @param array The character array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided character array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final char[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided byte array into a JSONArray.
     *
     * @param array The byte array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided byte array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final byte[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided short array into a JSONArray.
     *
     * @param array The short array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided short array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final short[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided integer array into a JSONArray.
     *
     * @param array The integer array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided integer array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final int[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided long array into a JSONArray.
     *
     * @param array The long array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided long array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final long[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided float array into a JSONArray.
     *
     * @param array The float array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided float array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final float[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided double array into a JSONArray.
     *
     * @param array The double array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided double array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final double[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided Object array into a JSONArray.
     *
     * @param array The Object array to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided Object array.
     * @throws JSONException if there is an error during the wrapping process.
     */
    public static JSONArray wrap(final Object[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Wraps the provided Collection into a JSONArray.
     *
     * @param coll The Collection to be wrapped into a JSONArray.
     * @return A new JSONArray that represents the provided Collection.
     */
    public static JSONArray wrap(final Collection<?> coll) {
        return new JSONArray(coll);
    }

    /**
     * Unwraps the provided JSONObject into a Map.
     *
     * @param jsonObject The JSONObject to be unwrapped into a Map.
     * @return A new Map that represents the provided JSONObject.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    public static Map<String, Object> unwrap(final JSONObject jsonObject) throws JSONException {
        return unwrap(jsonObject, Map.class);
    }

    /**
     * Unwraps the provided JSONObject into an instance of the specified target type.
     *
     * @param <T> The type of the object to return.
     * @param jsonObject The JSONObject to be unwrapped into an instance of the target type.
     * @param targetType The class of the object to return.
     * @return An instance of the specified target type that represents the provided JSONObject.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    public static <T> T unwrap(final JSONObject jsonObject, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonObject, N.typeOf(targetType));
    }

    /**
     * Unwraps the provided JSONObject into an instance of the specified target type.
     *
     * @param <T> The type of the object to return.
     * @param jsonObject The JSONObject to be unwrapped into an instance of the target type.
     * @param targetType The type of the object to return. This is used to determine how to unwrap the JSONObject.
     * @return An instance of the specified target type that represents the provided JSONObject.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType) throws JSONException {
        targetType = targetType.isObjectType() ? N.typeOf("Map<String, Object>") : targetType;
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

            return beanInfo.finishBeanResult(result);
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a map or bean type");
        }
    }

    /**
     * Unwraps the provided JSONArray into a List.
     *
     * @param <T> The type of the objects in the list.
     * @param jsonArray The JSONArray to be unwrapped into a List.
     * @return A new List that represents the provided JSONArray.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    public static <T> List<T> unwrap(final JSONArray jsonArray) throws JSONException {
        return (List<T>) toList(jsonArray, Object.class);
    }

    /**
     * Unwraps the provided JSONArray into an instance of the specified target type.
     *
     * @param <T> The type of the object to return.
     * @param jsonArray The JSONArray to be unwrapped into an instance of the target type.
     * @param targetType The class of the object to return.
     * @return An instance of the specified target type that represents the provided JSONArray.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    public static <T> T unwrap(final JSONArray jsonArray, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonArray, N.typeOf(targetType));
    }

    /**
     * Unwraps the provided JSONArray into an instance of the specified target type.
     *
     * @param <T> The type of the object to return.
     * @param jsonArray The JSONArray to be unwrapped into an instance of the target type.
     * @param targetType The type of the object to return. This is used to determine how to unwrap the JSONArray.
     * @return An instance of the specified target type that represents the provided JSONArray.
     * @throws JSONException if there is an error during the unwrapping process.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType) throws JSONException {
        targetType = targetType.isObjectType() ? N.typeOf("List<Object>") : targetType;
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
     * Converts the provided JSONArray into a List of the specified element type.
     *
     * @param <T> The type of the elements in the list.
     * @param jsonArray The JSONArray to be converted into a List.
     * @param elementClass The class of the elements in the list.
     * @return A new List that represents the provided JSONArray.
     * @throws JSONException if there is an error during the conversion process.
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Class<? extends T> elementClass) throws JSONException {
        return toList(jsonArray, Type.of(elementClass));
    }

    /**
     * Converts the provided JSONArray into a List of the specified element type.
     *
     * @param <T> The type of the elements in the list.
     * @param jsonArray The JSONArray to be converted into a List.
     * @param elementType The type of the elements in the list. This is used to determine how to convert the JSONArray.
     * @return A new List that represents the provided JSONArray.
     * @throws JSONException if there is an error during the conversion process.
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Type<T> elementType) throws JSONException {
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
