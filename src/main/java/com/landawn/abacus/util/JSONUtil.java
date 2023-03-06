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
        return unwrap(Map.class, jsonObject);
    }

    /**
     *
     * @param <T>
     * @param cls
     * @param jsonObject
     * @return
     * @throws JSONException the JSON exception
     */
    public static <T> T unwrap(final Class<? extends T> cls, final JSONObject jsonObject) throws JSONException {
        return unwrap(N.<T> typeOf(cls), jsonObject);
    }

    /**
     *
     * @param <T>
     * @param type
     * @param jsonObject
     * @return
     * @throws JSONException the JSON exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Type<? extends T> type, final JSONObject jsonObject) throws JSONException {
        type = type.isObjectType() ? N.<T> typeOf("Map<String, Object>") : type;
        final Class<?> cls = type.clazz();

        if (type.clazz().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        } else if (type.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> map = N.newMap((Class<Map>) cls, jsonObject.keySet().size());
            final Iterator<String> iter = jsonObject.keys();
            final Type<?> valueType = type.getParameterTypes()[1];
            String key = null;
            Object value = null;

            while (iter.hasNext()) {
                key = iter.next();
                value = jsonObject.get(key);

                if (value == JSONObject.NULL) {
                    value = null;
                } else if (value != null) {
                    if (value instanceof JSONObject) {
                        value = unwrap(valueType, (JSONObject) value);
                    } else if (value instanceof JSONArray) {
                        value = unwrap(valueType, (JSONArray) value);
                    }
                }

                map.put(key, value);
            }

            return (T) map;
        } else if (type.isBean()) {
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
                        value = unwrap(propInfo.jsonXmlType, (JSONObject) value);
                    } else if (value instanceof JSONArray) {
                        value = unwrap(propInfo.jsonXmlType, (JSONArray) value);
                    }
                }

                propInfo.setPropValue(result, value);
            }

            return (T) beanInfo.finishBeanResult(result);
        } else {
            throw new IllegalArgumentException(type.name() + " is not a map or bean type");
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
        return (List<T>) unwrap(jsonArray, Object.class);
    }

    /**
     *
     * @param <T>
     * @param jsonArray
     * @param elementClass
     * @return
     * @throws JSONException the JSON exception
     */
    public static <T> List<T> unwrap(final JSONArray jsonArray, Class<? extends T> elementClass) throws JSONException {
        return unwrap(jsonArray, Type.of(elementClass));
    }

    /**
     *
     * @param <T>
     * @param jsonArray
     * @param elementType
     * @return
     * @throws JSONException the JSON exception
     */
    public static <T> List<T> unwrap(final JSONArray jsonArray, Type<T> elementType) throws JSONException {
        final int len = jsonArray.length();
        final List<Object> coll = new ArrayList<>(len);

        Object element = null;

        for (int i = 0; i < len; i++) {
            element = jsonArray.get(i);

            if (element == JSONObject.NULL) {
                element = null;
            } else if (element != null) {
                if (element instanceof JSONObject) {
                    element = unwrap(elementType, (JSONObject) element);
                } else if (element instanceof JSONArray) {
                    element = unwrap(elementType, (JSONArray) element);
                }
            }

            coll.add(element);
        }

        return (List<T>) coll;
    }

    /**
     *
     * @param <T>
     * @param cls array or collection class
     * @param jsonArray
     * @return
     * @throws JSONException the JSON exception
     */
    public static <T> T unwrap(final Class<? extends T> cls, final JSONArray jsonArray) throws JSONException {
        return unwrap(N.<T> typeOf(cls), jsonArray);
    }

    /**
     *
     * @param <T>
     * @param type
     * @param jsonArray
     * @return
     * @throws JSONException the JSON exception
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Type<? extends T> type, final JSONArray jsonArray) throws JSONException {
        type = type.isObjectType() ? N.<T> typeOf("List<Object>") : type;
        final int len = jsonArray.length();

        if (type.clazz().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        } else if (type.isCollection()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> coll = N.newCollection((Class<Collection>) type.clazz(), len);
            final Type<?> elementType = type.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap(elementType, (JSONObject) element);
                    } else if (element instanceof JSONArray) {
                        element = unwrap(elementType, (JSONArray) element);
                    }
                }

                coll.add(element);
            }

            return (T) coll;
        } else if (type.isPrimitiveArray()) {
            final Object array = N.newArray(type.getElementType().clazz(), jsonArray.length());
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                }

                if (element == null) {
                    element = type.getElementType().defaultValue();
                }

                Array.set(array, i, element);
            }

            return (T) array;
        } else if (type.isArray()) {
            final Object[] array = N.newArray(type.getElementType().clazz(), jsonArray.length());
            final Type<?> elementType = type.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap(elementType, (JSONObject) element);
                    } else if (element instanceof JSONArray) {
                        element = unwrap(elementType, (JSONArray) element);
                    }
                }

                array[i] = element;
            }

            return (T) array;
        } else {
            throw new IllegalArgumentException(type.name() + " is not a array or collection type");
        }
    }
}
