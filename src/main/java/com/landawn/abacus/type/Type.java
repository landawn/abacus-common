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
import com.landawn.abacus.util.Multiset;
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
     * Returns the type of the given {@code java.lang.reflect.Type} object.
     *
     * @param <T>
     * @param type
     * @return
     */
    static <T> Type<T> of(final java.lang.reflect.Type type) {
        return TypeFactory.getType(type);
    }

    /**
     * Returns the type of the given {@code TypeReference} object.
     *
     * @param <T>
     * @param typeRef
     * @return
     */
    static <T> Type<T> of(final TypeReference<T> typeRef) {
        return typeRef.type();
    }

    /**
     * Returns the type of the given {@code Class} object.
     *
     * @param <T>
     * @param cls
     * @return
     */
    static <T> Type<T> of(final Class<? extends T> cls) {
        return TypeFactory.getType(cls);
    }

    /**
     * Returns the type by the given type name.
     *
     * @param <T>
     * @param typeName
     * @return
     */
    static <T> Type<T> of(final String typeName) {
        return TypeFactory.getType(typeName);
    }

    /**
     * Returns a list of types of the given classes.
     *
     * @param <T>
     * @param classes
     * @return
     */
    @SafeVarargs
    static <T> List<Type<T>> ofAll(final Class<? extends T>... classes) {
        return ofAll(Array.asList(classes));
    }

    /**
     * Returns a list of types of the given classes.
     *
     * @param <T>
     * @param classes
     * @return
     */
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
     * Returns a {@code List} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<List<T>> ofList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("List<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code LinkedList} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedList<T>> ofLinkedList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedList<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code List} type whose key and value types are the given classes.
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
     * Returns a {@code List} type whose key and value types are the given classes.
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
     * Returns a {@code Set} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Set<T>> ofSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Set<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code Set} whose element type is a map with the given key and value classes.
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
     * Returns a {@code Set} whose element type is a linked hash map with the given key and value classes.
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
     * Returns a {@code LinkedHashSet} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedHashSet<T>> ofLinkedHashSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedHashSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code SortedSet} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<SortedSet<T>> ofSortedSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("SortedSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code NavigableSet} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<NavigableSet<T>> ofNavigableSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("NavigableSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code TreeSet} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<TreeSet<T>> ofTreeSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("TreeSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code Queue} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Queue<T>> ofQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Queue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code Deque} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Deque<T>> ofDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Deque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code ArrayDeque} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<ArrayDeque<T>> ofArrayDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ArrayDeque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code LinkedBlockingQueue} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<LinkedBlockingQueue<T>> ofLinkedBlockingQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedBlockingQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code ConcurrentLinkedQueue} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ConcurrentLinkedQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code PriorityQueue} type whose element type is the given class.
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
     * Returns a {@code Map} type whose key and value types are the given classes.
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
     * Returns a {@code LinkedHashMap} type whose key and value types are the given classes.
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
     * Returns a {@code LinkedHashMap} type whose key and value types are the given classes.
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
     * Returns a {@code NavigableMap} type whose key and value types are the given classes.
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
     * Returns a {@code TreeMap} type whose key and value types are the given classes.
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
     * Returns a {@code TreeMap} type whose key and value types are the given classes.
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
     * Returns a {@code ConcurrentHashMap} type whose key and value types are the given classes.
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
     * Returns a {@code Multiset} type whose element type is the given class.
     *
     * @param <T>
     * @param eleClass
     * @return
     */
    static <T> Type<Multiset<T>> ofMultiset(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Multiset<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code ListMultimap} type whose key and value types are the given classes.
     *
     * @param keyClass
     * @param eleClass
     * @return
     * @param <K>
     * @param <E>
     */
    static <K, E> Type<ListMultimap<K, E>> ofListMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("ListMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a {@code SetMultimap} type whose key and value types are the given classes.
     * 
     * @param keyClass
     * @param eleClass
     * @return
     * @param <K>
     * @param <E>
     */
    static <K, E> Type<SetMultimap<K, E>> ofSetMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("SetMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    String name();

    String declaringName();

    String xmlName();

    Class<T> clazz();

    /**
     * Checks if this is a primitive type.
     *
     * @return {@code true}, if is primitive type
     */
    boolean isPrimitiveType();

    /**
     * Checks if this is a primitive wrapper.
     *
     * @return {@code true}, if is primitive wrapper
     */
    boolean isPrimitiveWrapper();

    /**
     * Checks if this is a primitive list.
     *
     * @return {@code true}, if is primitive list
     */
    boolean isPrimitiveList();

    /**
     * Checks if this is a boolean.
     *
     * @return {@code true}, if is boolean
     */
    boolean isBoolean();

    /**
     * Checks if this is a number.
     *
     * @return {@code true}, if is number
     */
    boolean isNumber();

    /**
     * Checks if this is a string.
     *
     * @return {@code true}, if is string
     */
    boolean isString();

    boolean isCharSequence();

    /**
     * Checks if this is a type of {@code Date}.
     *
     * @return {@code true}, if it is {@code Date} type
     */
    boolean isDate();

    /**
     * Checks if this is a type of {@code Calendar}.
     *
     * @return {@code true}, if it is {@code Calendar} type
     */
    boolean isCalendar();

    /**
     * Checks if this is a type of Joda {@code DateTime}.
     *
     * @return {@code true}, if it is Joda {@code DateTime} type
     */
    boolean isJodaDateTime();

    /**
     * Checks if this is a type of primitive {@code array}.
     *
     * @return {@code true}, if it is primitive {@code array} type
     */
    boolean isPrimitiveArray();

    /**
     * Checks if this is a type of {@code byte[]}.
     *
     * @return {@code true}, if it is {@code byte[]} type
     */
    boolean isPrimitiveByteArray();

    /**
     * Checks if this is a type of object {@code array}.
     *
     * @return {@code true}, if it is object {@code array} type
     */
    boolean isObjectArray();

    /**
     * Checks if this is a type of {@code array}.
     *
     * @return {@code true}, if it is {@code array} type
     */
    boolean isArray();

    /**
     * Checks if this is a type of {@code List}.
     *
     * @return {@code true}, if it is {@code List} type
     */
    boolean isList();

    /**
     * Checks if this is a type of {@code Set}.
     *
     * @return {@code true}, if it is {@code Set} type
     */
    boolean isSet();

    /**
     * Checks if this is a type of {@code Collection}.
     *
     * @return {@code true}, if it is {@code Collection} type
     */
    boolean isCollection();

    /**
     * Checks if this is a type of {@code Map}.
     *
     * @return {@code true}, if it is {@code Map} type
     */
    boolean isMap();

    /**
     * Checks if this is a type of {@code Bean}.
     *
     * @return {@code true}, if it is {@code Bean} type
     */
    boolean isBean();

    /**
     * Checks if this is a type of {@code MapEntity}.
     *
     * @return {@code true}, if it is {@code MapEntity} type
     */
    boolean isMapEntity();

    /**
     * Checks if this is a type of {@code EntityId}.
     *
     * @return {@code true}, if it is {@code EntityId} type
     */
    boolean isEntityId();

    /**
     * Checks if this is a type of {@code DataSet}.
     *
     * @return {@code true}, if it is {@code DataSet} type
     */
    boolean isDataSet();

    /**
     * Checks if this is a type of {@code InputStream}.
     *
     * @return {@code true}, if it is {@code InputStream} type
     */
    boolean isInputStream();

    /**
     * Checks if this is a type of {@code Reader}.
     *
     * @return {@code true}, if it is {@code Reader} type
     */
    boolean isReader();

    /**
     * Checks if this is a type of {@code ByteBuffer}.
     *
     * @return {@code true}, if it is {@code ByteBuffer} type
     */
    boolean isByteBuffer();

    /**
     * Checks if this is a generic type.
     *
     * @return {@code true}, if it is a generic type
     */
    boolean isGenericType();

    /**
     * Checks if this is an immutable type.
     *
     * @return {@code true}, if it is immutable
     */
    boolean isImmutable();

    /**
     * Checks if this is a comparable type.
     *
     * @return {@code true}, if it is comparable
     */
    boolean isComparable();

    /**
     * Returns {@code true} if the value of this type can be serialized to json/xml/... String directly. The primitive
     * type/array/wrapper, date, calendar ... belong to this category. Object Array/Collection/Map/Bean are not.
     *
     * @return {@code true}, if is serializable
     */
    boolean isSerializable();

    boolean isObjectType();

    /**
     * Checks if this is an optional or {@code nullable}.
     *
     * @return {@code true}, if is optional or nullable
     */
    boolean isOptionalOrNullable();

    /**
     * Returns {@code true} if this type is a non quoted csv type: Number, Boolean, Date, Calendar,
     *
     * @return {@code true}, if is non quoted csv type
     */
    default boolean isNonQuotableCsvType() {
        return false;
    }

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
     * Converts the given collection to an array of this type.
     *
     * @param c
     * @return
     */
    T collection2Array(Collection<?> c);

    /**
     * Converts the given array of this type to a collection of the specified type.
     *
     * @param <E>
     * @param x
     * @param collClass
     * @return
     */
    <E> Collection<E> array2Collection(T x, Class<?> collClass);

    /**
     * Converts the given array of this type to a collection by adding the array elements to the specified output collection.
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
        SERIALIZABLE, ENTITY, MAP, ARRAY, COLLECTION, MAP_ENTITY, ENTITY_ID, DATA_SET, SHEET, UNKNOWN
    }
}
