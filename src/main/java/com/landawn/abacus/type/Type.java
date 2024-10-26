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

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.TypeReference;

/**
 *
 * @param <T>
 * @see com.landawn.abacus.util.TypeReference
 * @see com.landawn.abacus.util.TypeReference.TypeToken
 */
public interface Type<T> {

    /**
     *
     * @param <T>
     * @param type
     * @return
     */
    static <T> Type<T> of(final java.lang.reflect.Type type) {
        return TypeFactory.getType(type);
    }

    /**
     *
     *
     * @param <T>
     * @param typeRef
     * @return
     */
    static <T> Type<T> of(final TypeReference<T> typeRef) {
        return typeRef.type();
    }

    /**
     *
     * @param <T>
     * @param cls
     * @return
     */
    static <T> Type<T> of(final Class<? extends T> cls) {
        return TypeFactory.getType(cls);
    }

    /**
     *
     * @param <T>
     * @param typeName
     * @return
     */
    static <T> Type<T> of(final String typeName) {
        return TypeFactory.getType(typeName);
    }

    /**
     *
     * @param <T>
     * @param classes
     * @return
     */
    @SuppressWarnings("unchecked")
    @SafeVarargs
    static <T> List<Type<T>> ofAll(final Class<? extends T>... classes) {
        return ofAll(Array.asList(classes));
    }

    /**
     *
     * @param <T>
     * @param classes
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T> List<Type<T>> ofAll(final Collection<Class<? extends T>> classes) {
        final List<Type<T>> types = new ArrayList<>(N.size(classes));

        if (N.notEmpty(classes)) {
            for (final Class<?> cls : classes) {
                types.add((Type<T>) of(cls));
            }
        }

        return types;
    }

    /**
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<List<T>> ofList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("List<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of linked list.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedList<T>> ofLinkedList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedList<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of list of map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<List<Map<K, V>>> ofListOfMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("List<Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Of list of linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<List<Map<K, V>>> ofListOfLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("List<LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Set<T>> ofSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Set<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of set of map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<Set<Map<K, V>>> ofSetOfMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Set<Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Of set of linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<Set<Map<K, V>>> ofSetOfLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Set<LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Of linked hash set.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedHashSet<T>> ofLinkedHashSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedHashSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of sorted set.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<SortedSet<T>> ofSortedSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("SortedSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of navigable set.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<NavigableSet<T>> ofNavigableSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("NavigableSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of tree set.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<TreeSet<T>> ofTreeSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("TreeSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Queue<T>> ofQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Queue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Deque<T>> ofDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Deque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of array deque.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<ArrayDeque<T>> ofArrayDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ArrayDeque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of linked blocking queue.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedBlockingQueue<T>> ofLinkedBlockingQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedBlockingQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of concurrent linked queue.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ConcurrentLinkedQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Of priority queue.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<PriorityQueue<T>> ofPriorityQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("PriorityQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    static Type<Map<String, Object>> ofPropsMap() {
        return TypeFactory.getType("LinkedHashMap<String, Object>");
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<Map<K, V>> ofMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<LinkedHashMap<K, V>> ofLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of sorted map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<SortedMap<K, V>> ofSortedMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("SortedMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of navigable map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<NavigableMap<K, V>> ofNavigableMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("NavigableMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of tree map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<TreeMap<K, V>> ofTreeMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("TreeMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of concurrent map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<ConcurrentMap<K, V>> ofConcurrentMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("ConcurrentMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Of concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass
     * @param valClass
     * @return
     */
    static <K, V> Type<ConcurrentHashMap<K, V>> ofConcurrentHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("ConcurrentHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Set<T>> ofMultiset(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Multiset<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    static <K, E> Type<ListMultimap<K, E>> ofListMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("ListMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    static <K, E> Type<SetMultimap<K, E>> ofSetMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("SetMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    String name();

    String declaringName();

    String xmlName();

    Class<T> clazz();

    /**
     * Checks if is primitive type.
     *
     * @return {@code true}, if is primitive type
     */
    boolean isPrimitiveType();

    /**
     * Checks if is primitive wrapper.
     *
     * @return {@code true}, if is primitive wrapper
     */
    boolean isPrimitiveWrapper();

    /**
     * Checks if is primitive list.
     *
     * @return {@code true}, if is primitive list
     */
    boolean isPrimitiveList();

    /**
     * Checks if is boolean.
     *
     * @return {@code true}, if is boolean
     */
    boolean isBoolean();

    /**
     * Checks if is number.
     *
     * @return {@code true}, if is number
     */
    boolean isNumber();

    /**
     * Checks if is string.
     *
     * @return {@code true}, if is string
     */
    boolean isString();

    boolean isCharSequence();

    /**
     * Checks if is date.
     *
     * @return {@code true}, if is date
     */
    boolean isDate();

    /**
     * Checks if is calendar.
     *
     * @return {@code true}, if is calendar
     */
    boolean isCalendar();

    /**
     * Checks if is joda date time.
     *
     * @return {@code true}, if is joda date time
     */
    boolean isJodaDateTime();

    /**
     * Checks if is primitive array.
     *
     * @return {@code true}, if is primitive array
     */
    boolean isPrimitiveArray();

    /**
     * Checks if is primitive byte array.
     *
     * @return {@code true}, if is primitive byte array
     */
    boolean isPrimitiveByteArray();

    /**
     * Checks if is object array.
     *
     * @return {@code true}, if is object array
     */
    boolean isObjectArray();

    /**
     * Checks if is array.
     *
     * @return {@code true}, if is array
     */
    boolean isArray();

    /**
     * Checks if is list.
     *
     * @return {@code true}, if is list
     */
    boolean isList();

    /**
     * Checks if is sets the.
     *
     * @return {@code true}, if is sets the
     */
    boolean isSet();

    /**
     * Checks if is collection.
     *
     * @return {@code true}, if is collection
     */
    boolean isCollection();

    /**
     * Checks if is map.
     *
     * @return {@code true}, if is map
     */
    boolean isMap();

    /**
     * Checks if is bean.
     *
     * @return {@code true}, if is bean
     */
    boolean isBean();

    /**
     * Checks if is map bean.
     *
     * @return {@code true}, if is map bean
     */
    boolean isMapEntity();

    /**
     * Checks if is bean id.
     *
     * @return {@code true}, if is bean id
     */
    boolean isEntityId();

    /**
     * Checks if is data set.
     *
     * @return {@code true}, if is data set
     */
    boolean isDataSet();

    /**
     * Checks if is input stream.
     *
     * @return {@code true}, if is input stream
     */
    boolean isInputStream();

    /**
     * Checks if is reader.
     *
     * @return {@code true}, if is reader
     */
    boolean isReader();

    /**
     * Checks if is byte buffer.
     *
     * @return {@code true}, if is byte buffer
     */
    boolean isByteBuffer();

    /**
     * Checks if is generic type.
     *
     * @return {@code true}, if is generic type
     */
    boolean isGenericType();

    boolean isObjectType();

    /**
     * Checks if is immutable.
     *
     * @return {@code true}, if is immutable
     */
    boolean isImmutable();

    /**
     * Checks if is comparable.
     *
     * @return {@code true}, if is comparable
     */
    boolean isComparable();

    /**
     * Returns {@code true} if the value of this type can be serialized to json/xml/... String directly. The primitive
     * type/array/wrapper, date, calendar ... belong to this category. Object Array/Collection/Map/Bean are not.
     *
     * @return {@code true}, if is serializable
     */
    boolean isSerializable();

    /**
     * Checks if is optional or nullable.
     *
     * @return {@code true}, if is optional or nullable
     */
    boolean isOptionalOrNullable();

    /**
     * Gets the serialization type.
     *
     * @return
     */
    SerializationType getSerializationType();

    /**
     * Gets the element type.
     *
     * @return
     */
    Type<?> getElementType(); //NOSONAR

    /**
     * Gets the parameter types.
     *
     * @return
     */
    Type<?>[] getParameterTypes(); //NOSONAR

    T defaultValue();

    /**
     *
     * @param value
     * @return
     */
    boolean isDefaultValue(T value);

    /**
     *
     * @param x
     * @param y
     * @return
     */
    int compare(T x, T y);

    /**
     *
     * @param x
     * @return
     */
    String stringOf(T x);

    /**
     *
     * @param str
     * @return
     */
    T valueOf(String str);

    /**
     *
     * @param obj
     * @return
     */
    T valueOf(Object obj);

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    T valueOf(char[] cbuf, int offset, int len);

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    T get(ResultSet rs, int columnIndex) throws SQLException;

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    T get(ResultSet rs, String columnLabel) throws SQLException;

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    void set(PreparedStatement stmt, int columnIndex, T x) throws SQLException;

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    void set(CallableStatement stmt, String parameterName, T x) throws SQLException;

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    void set(PreparedStatement stmt, int columnIndex, T x, int sqlTypeOrLength) throws SQLException;

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    void set(CallableStatement stmt, String parameterName, T x, int sqlTypeOrLength) throws SQLException;

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void appendTo(Appendable appendable, T x) throws IOException;

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    void writeCharacter(CharacterWriter writer, T x, JSONXMLSerializationConfig<?> config) throws IOException;

    /**
     * Collection 2 array.
     *
     * @param c
     * @return
     */
    T collection2Array(Collection<?> c);

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param x
     * @param collClass
     * @return
     */
    <E> Collection<E> array2Collection(T x, Class<?> collClass);

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param x
     * @param output
     */
    <E> void array2Collection(T x, Collection<E> output);

    /**
     *
     * @param x
     * @return
     */
    int hashCode(T x);

    /**
     * Deep hash code.
     *
     * @param x
     * @return
     */
    int deepHashCode(T x);

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    boolean equals(T x, T y);

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    boolean deepEquals(T x, T y);

    /**
     *
     * @param x
     * @return
     */
    String toString(T x);

    /**
     * Deep to string.
     *
     * @param x
     * @return
     */
    String deepToString(T x);

    enum SerializationType {
        SERIALIZABLE, ENTITY, MAP, ARRAY, COLLECTION, MAP_ENTITY, ENTITY_ID, DATA_SET, SHEET, UNKNOWN;
    }
}
