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
 * The Type interface represents a type in the Abacus type system.
 * It provides comprehensive type information and operations for converting between
 * different representations (String, Object, database types, etc.) and performing
 * type-specific operations like serialization, comparison, and equality checking.
 * 
 * <p>This interface is the core abstraction for type handling in Abacus, supporting:</p>
 * <ul>
 *   <li>Type identification and metadata (name, class, generic parameters)</li>
 *   <li>Type conversion operations (valueOf, stringOf)</li>
 *   <li>Database operations (get/set with ResultSet/PreparedStatement)</li>
 *   <li>Serialization support (JSON/XML writing)</li>
 *   <li>Collection/Array conversions</li>
 *   <li>Type-specific operations (comparison, equality, hash codes)</li>
 * </ul>
 *
 * @param <T> the Java type that this Type represents
 * @see com.landawn.abacus.util.TypeReference
 * @see com.landawn.abacus.util.TypeReference.TypeToken
 */
public interface Type<T> {

    /**
     * Returns the Type instance for the given Java reflection Type.
     * This method handles all Java type representations including Class,
     * ParameterizedType, GenericArrayType, etc.
     *
     * @param <T> the type parameter
     * @param type the Java reflection type
     * @return the corresponding Type instance
     */
    static <T> Type<T> of(final java.lang.reflect.Type type) {
        return TypeFactory.getType(type);
    }

    /**
     * Returns the Type instance for the given TypeReference.
     * TypeReference provides a way to capture generic type information at runtime.
     *
     * @param <T> the type parameter
     * @param typeRef the type reference
     * @return the corresponding Type instance
     */
    static <T> Type<T> of(final TypeReference<T> typeRef) {
        return typeRef.type();
    }

    /**
     * Returns the Type instance for the given Class.
     * This is the most common way to obtain a Type for non-generic classes.
     *
     * @param <T> the type parameter
     * @param cls the class
     * @return the corresponding Type instance
     */
    static <T> Type<T> of(final Class<? extends T> cls) {
        return TypeFactory.getType(cls);
    }

    /**
     * Returns the Type instance by parsing the given type name string.
     * Supports both simple and generic type names (e.g., "String", "List<String>").
     *
     * @param <T> the type parameter
     * @param typeName the type name string
     * @return the corresponding Type instance
     */
    static <T> Type<T> of(final String typeName) {
        return TypeFactory.getType(typeName);
    }

    /**
     * Returns a list of Type instances for the given array of classes.
     * Convenient for obtaining multiple types at once.
     *
     * @param <T> the type parameter
     * @param classes the array of classes
     * @return list of corresponding Type instances
     */
    @SafeVarargs
    static <T> List<Type<T>> ofAll(final Class<? extends T>... classes) {
        return ofAll(Array.asList(classes));
    }

    /**
     * Returns a list of Type instances for the given collection of classes.
     *
     * @param <T> the type parameter
     * @param classes the collection of classes
     * @return list of corresponding Type instances
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
     * Returns a List type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for List of the specified element type
     */
    static <T> Type<List<T>> ofList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("List<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a LinkedList type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for LinkedList of the specified element type
     */
    static <T> Type<LinkedList<T>> ofLinkedList(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedList<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a List of Map type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for List of Map with specified key/value types
     */
    static <K, V> Type<List<Map<K, V>>> ofListOfMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("List<Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Returns a List of LinkedHashMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for List of LinkedHashMap with specified key/value types
     */
    static <K, V> Type<List<Map<K, V>>> ofListOfLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("List<LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Returns a Set type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for Set of the specified element type
     */
    static <T> Type<Set<T>> ofSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Set<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a Set of Map type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for Set of Map with specified key/value types
     */
    static <K, V> Type<Set<Map<K, V>>> ofSetOfMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Set<Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Returns a Set of LinkedHashMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for Set of LinkedHashMap with specified key/value types
     */
    static <K, V> Type<Set<Map<K, V>>> ofSetOfLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Set<LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">>");
    }

    /**
     * Returns a LinkedHashSet type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for LinkedHashSet of the specified element type
     */
    static <T> Type<LinkedHashSet<T>> ofLinkedHashSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedHashSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a SortedSet type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for SortedSet of the specified element type
     */
    static <T> Type<SortedSet<T>> ofSortedSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("SortedSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a NavigableSet type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for NavigableSet of the specified element type
     */
    static <T> Type<NavigableSet<T>> ofNavigableSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("NavigableSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a TreeSet type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for TreeSet of the specified element type
     */
    static <T> Type<TreeSet<T>> ofTreeSet(final Class<? extends T> eleClass) {
        return TypeFactory.getType("TreeSet<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a Queue type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for Queue of the specified element type
     */
    static <T> Type<Queue<T>> ofQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Queue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a Deque type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for Deque of the specified element type
     */
    static <T> Type<Deque<T>> ofDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Deque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns an ArrayDeque type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for ArrayDeque of the specified element type
     */
    static <T> Type<ArrayDeque<T>> ofArrayDeque(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ArrayDeque<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a LinkedBlockingQueue type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for LinkedBlockingQueue of the specified element type
     */
    static <T> Type<LinkedBlockingQueue<T>> ofLinkedBlockingQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("LinkedBlockingQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a ConcurrentLinkedQueue type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for ConcurrentLinkedQueue of the specified element type
     */
    static <T> Type<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("ConcurrentLinkedQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a PriorityQueue type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for PriorityQueue of the specified element type
     */
    static <T> Type<PriorityQueue<T>> ofPriorityQueue(final Class<? extends T> eleClass) {
        return TypeFactory.getType("PriorityQueue<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a Map type for properties (LinkedHashMap<String, Object>).
     * Commonly used for property maps in configuration and data transfer.
     *
     * @return Type instance for LinkedHashMap<String, Object>
     */
    static Type<Map<String, Object>> ofPropsMap() {
        return TypeFactory.getType("LinkedHashMap<String, Object>");
    }

    /**
     * Returns a Map type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for Map with specified key/value types
     */
    static <K, V> Type<Map<K, V>> ofMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("Map<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a LinkedHashMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for LinkedHashMap with specified key/value types
     */
    static <K, V> Type<LinkedHashMap<K, V>> ofLinkedHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("LinkedHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a SortedMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for SortedMap with specified key/value types
     */
    static <K, V> Type<SortedMap<K, V>> ofSortedMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("SortedMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a NavigableMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for NavigableMap with specified key/value types
     */
    static <K, V> Type<NavigableMap<K, V>> ofNavigableMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("NavigableMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a TreeMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for TreeMap with specified key/value types
     */
    static <K, V> Type<TreeMap<K, V>> ofTreeMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("TreeMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a ConcurrentMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for ConcurrentMap with specified key/value types
     */
    static <K, V> Type<ConcurrentMap<K, V>> ofConcurrentMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("ConcurrentMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a ConcurrentHashMap type with the specified key and value types.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyClass the key class
     * @param valClass the value class
     * @return Type instance for ConcurrentHashMap with specified key/value types
     */
    static <K, V> Type<ConcurrentHashMap<K, V>> ofConcurrentHashMap(final Class<? extends K> keyClass, final Class<? extends V> valClass) {
        return TypeFactory.getType("ConcurrentHashMap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(valClass) + ">");
    }

    /**
     * Returns a Multiset type with the specified element type.
     *
     * @param <T> the element type
     * @param eleClass the element class
     * @return Type instance for Multiset of the specified element type
     */
    static <T> Type<Multiset<T>> ofMultiset(final Class<? extends T> eleClass) {
        return TypeFactory.getType("Multiset<" + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a ListMultimap type with the specified key and element types.
     *
     * @param <K> the key type
     * @param <E> the element type
     * @param keyClass the key class
     * @param eleClass the element class
     * @return Type instance for ListMultimap with specified key/element types
     */
    static <K, E> Type<ListMultimap<K, E>> ofListMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("ListMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns a SetMultimap type with the specified key and element types.
     * 
     * @param <K> the key type
     * @param <E> the element type
     * @param keyClass the key class
     * @param eleClass the element class
     * @return Type instance for SetMultimap with specified key/element types
     */
    static <K, E> Type<SetMultimap<K, E>> ofSetMultimap(final Class<? extends K> keyClass, final Class<? extends E> eleClass) {
        return TypeFactory.getType("SetMultimap<" + ClassUtil.getCanonicalClassName(keyClass) + ", " + ClassUtil.getCanonicalClassName(eleClass) + ">");
    }

    /**
     * Returns the name of this type.
     * For generic types, includes type parameters (e.g., "List<String>").
     *
     * @return the type name
     */
    String name();

    /**
     * Returns the declaring name of this type.
     * This may differ from name() for nested or inner types.
     *
     * @return the declaring name
     */
    String declaringName();

    /**
     * Returns the XML-safe name of this type.
     * Angle brackets are escaped for use in XML documents.
     *
     * @return the XML-safe type name
     */
    String xmlName();

    /**
     * Returns the Class object representing this type.
     * For generic types, returns the raw type class.
     *
     * @return the class for this type
     */
    Class<T> clazz();

    /**
     * Checks if this is a primitive type (int, long, double, etc.).
     *
     * @return {@code true} if this is a primitive type
     */
    default boolean isPrimitiveType() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a primitive wrapper type (Integer, Long, Double, etc.).
     *
     * @return {@code true} if this is a primitive wrapper type
     */
    default boolean isPrimitiveWrapper() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a primitive list type (IntList, LongList, etc.).
     *
     * @return {@code true} if this is a primitive list type
     */
    default boolean isPrimitiveList() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a boolean type (boolean or Boolean).
     *
     * @return {@code true} if this is a boolean type
     */
    default boolean isBoolean() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a char type (char or Character).
     *
     * @return {@code true} if this is a char type
     */
    default boolean isCharacter() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a number type (numeric primitive or wrapper).
     *
     * @return {@code true} if this is a number type
     */
    default boolean isNumber() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a byte type (byte or Byte).
     *
     * @return {@code true} if this is a byte type
     */
    default boolean isByte() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a short type (short or Short).
     *
     * @return {@code true} if this is a short type
     */
    default boolean isShort() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is an int type (int or Integer).
     *
     * @return {@code true} if this is an int type
     */
    default boolean isInteger() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a long type (long or Long).
     *
     * @return {@code true} if this is a long type
     */
    default boolean isLong() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a float type (float or Float).
     *
     * @return {@code true} if this is a float type
     */
    default boolean isFloat() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a double type (double or Double).
     *
     * @return {@code true} if this is a double type
     */
    default boolean isDouble() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a String type.
     *
     * @return {@code true} if this is a String type
     */
    default boolean isString() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a CharSequence type.
     *
     * @return {@code true} if this is a CharSequence type
     */
    default boolean isCharSequence() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Date.
     *
     * @return {@code true} if this is a Date type
     */
    default boolean isDate() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Calendar.
     *
     * @return {@code true} if this is a Calendar type
     */
    default boolean isCalendar() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Joda DateTime.
     *
     * @return {@code true} if this is a Joda DateTime type
     */
    default boolean isJodaDateTime() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a primitive array.
     *
     * @return {@code true} if this is a primitive array type
     */
    default boolean isPrimitiveArray() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a byte array (byte[]).
     *
     * @return {@code true} if this is a byte array type
     */
    default boolean isPrimitiveByteArray() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents an object array.
     *
     * @return {@code true} if this is an object array type
     */
    default boolean isObjectArray() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents any kind of array.
     *
     * @return {@code true} if this is an array type
     */
    default boolean isArray() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a List.
     *
     * @return {@code true} if this is a List type
     */
    default boolean isList() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Set.
     *
     * @return {@code true} if this is a Set type
     */
    default boolean isSet() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Collection.
     *
     * @return {@code true} if this is a Collection type
     */
    default boolean isCollection() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Map.
     *
     * @return {@code true} if this is a Map type
     */
    default boolean isMap() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Bean (POJO with properties).
     *
     * @return {@code true} if this is a Bean type
     */
    default boolean isBean() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a MapEntity.
     *
     * @return {@code true} if this is a MapEntity type
     */
    default boolean isMapEntity() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents an EntityId.
     *
     * @return {@code true} if this is an EntityId type
     */
    default boolean isEntityId() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Dataset.
     *
     * @return {@code true} if this is a Dataset type
     */
    default boolean isDataset() {
        return isPrimitiveArray() || isObjectArray(); // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents an InputStream.
     *
     * @return {@code true} if this is an InputStream type
     */
    default boolean isInputStream() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a Reader.
     *
     * @return {@code true} if this is a Reader type
     */
    default boolean isReader() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents a ByteBuffer.
     *
     * @return {@code true} if this is a ByteBuffer type
     */
    default boolean isByteBuffer() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is a generic type with type parameters.
     *
     * @return {@code true} if this is a generic type
     */
    default boolean isGenericType() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if values of this type are immutable.
     *
     * @return {@code true} if this type is immutable
     */
    default boolean isImmutable() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if values of this type can be compared.
     *
     * @return {@code true} if this type is comparable
     */
    default boolean isComparable() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if values of this type can be serialized directly to JSON/XML string.
     * Primitive types, arrays, wrappers, dates, etc. are serializable.
     * Object Arrays, Collections, Maps, and Beans typically are not.
     *
     * @return {@code true} if this type is serializable
     */
    default boolean isSerializable() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this type represents optional or nullable values.
     *
     * @return {@code true} if this is an optional or nullable type
     */
    default boolean isOptionalOrNullable() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if values of this type should not be quoted in CSV format.
     * Typically applies to numbers, booleans, dates, and calendars.
     *
     * @return {@code true} if values should not be quoted in CSV
     */
    default boolean isNonQuotableCsvType() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Checks if this is the Object type.
     *
     * @return {@code true} if this is the Object type
     */
    default boolean isObjectType() {
        return false; // Default implementation, can be overridden by specific types
    }

    /**
     * Gets the serialization type classification for this type.
     *
     * @return the serialization type
     */
    SerializationType getSerializationType();

    /**
     * Gets the element type for collection/array types.
     *
     * @return the element type, or {@code null} if not applicable
     */
    Type<?> getElementType(); //NOSONAR

    /**
     * Gets the parameter types for generic types.
     * For example, Map<K,V> returns types for K and V.
     *
     * @return array of parameter types, empty if none
     */
    Type<?>[] getParameterTypes(); //NOSONAR

    /**
     * Returns the default value for this type.
     * Typically {@code null} for reference types, 0/false for primitives.
     *
     * @return the default value
     */
    T defaultValue();

    /**
     * Checks if the given value is the default value for this type.
     *
     * @param value the value to check
     * @return {@code true} if value equals the default value
     */
    boolean isDefaultValue(T value);

    /**
     * Compares two values of this type.
     * Only supported for comparable types.
     *
     * @param x the first value
     * @param y the second value
     * @return negative if x < y, zero if x equals y, positive if x > y
     * @throws UnsupportedOperationException if this type is not comparable
     */
    int compare(T x, T y);

    /**
     * Converts a value of this type to its string representation.
     * This is the standard way to serialize values as strings.
     *
     * @param x the value to convert
     * @return the string representation
     */
    String stringOf(T x);

    /**
     * Parses a string to create a value of this type.
     * This is the standard way to deserialize values from strings.
     *
     * @param str the string to parse
     * @return the parsed value
     */
    T valueOf(String str);

    /**
     * Converts an object to a value of this type.
     * Handles various input types with appropriate conversions.
     *
     * @param obj the object to convert
     * @return the converted value
     */
    T valueOf(Object obj);

    /**
     * Parses a character array to create a value of this type.
     * Useful for efficient parsing without creating intermediate strings.
     *
     * @param cbuf the character array
     * @param offset the starting position
     * @param len the number of characters to parse
     * @return the parsed value
     */
    T valueOf(char[] cbuf, int offset, int len);

    /**
     * Retrieves a value of this type from a ResultSet at the specified column.
     *
     * @param rs the ResultSet
     * @param columnIndex the column index (1-based)
     * @return the retrieved value
     * @throws SQLException if a database access error occurs
     */
    T get(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * Retrieves a value of this type from a ResultSet by column name.
     *
     * @param rs the ResultSet
     * @param columnLabel the column label
     * @return the retrieved value
     * @throws SQLException if a database access error occurs
     */
    T get(ResultSet rs, String columnLabel) throws SQLException;

    /**
     * Sets a parameter value in a PreparedStatement.
     *
     * @param stmt the PreparedStatement
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set
     * @throws SQLException if a database access error occurs
     */
    void set(PreparedStatement stmt, int columnIndex, T x) throws SQLException;

    /**
     * Sets a parameter value in a CallableStatement by name.
     *
     * @param stmt the CallableStatement
     * @param parameterName the parameter name
     * @param x the value to set
     * @throws SQLException if a database access error occurs
     */
    void set(CallableStatement stmt, String parameterName, T x) throws SQLException;

    /**
     * Sets a parameter value in a PreparedStatement with SQL type or length hint.
     *
     * @param stmt the PreparedStatement
     * @param columnIndex the parameter index (1-based)
     * @param x the value to set
     * @param sqlTypeOrLength the SQL type constant or length hint
     * @throws SQLException if a database access error occurs
     */
    void set(PreparedStatement stmt, int columnIndex, T x, int sqlTypeOrLength) throws SQLException;

    /**
     * Sets a parameter value in a CallableStatement with SQL type or length hint.
     *
     * @param stmt the CallableStatement
     * @param parameterName the parameter name
     * @param x the value to set
     * @param sqlTypeOrLength the SQL type constant or length hint
     * @throws SQLException if a database access error occurs
     */
    void set(CallableStatement stmt, String parameterName, T x, int sqlTypeOrLength) throws SQLException;

    /**
     * Appends the string representation of a value to an Appendable.
     *
     * @param appendable the target to append to
     * @param x the value to append
     * @throws IOException if an I/O error occurs
     */
    void appendTo(Appendable appendable, T x) throws IOException;

    /**
     * Writes a value to a CharacterWriter with serialization configuration.
     * Used for JSON/XML serialization with format control.
     *
     * @param writer the CharacterWriter to write to
     * @param x the value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    void writeCharacter(CharacterWriter writer, T x, JSONXMLSerializationConfig<?> config) throws IOException;

    /**
     * Converts a collection to an array of this type.
     * Only supported for array types.
     *
     * @param c the collection to convert
     * @return the array representation
     * @throws UnsupportedOperationException if not an array type
     */
    T collection2Array(Collection<?> c);

    /**
     * Converts an array to a collection of the specified type.
     * Only supported for array types.
     *
     * @param <E> the element type
     * @param x the array to convert
     * @param collClass the collection class to create
     * @return the created collection containing array elements
     * @throws UnsupportedOperationException if not an array type
     */
    <E> Collection<E> array2Collection(T x, Class<?> collClass);

    /**
     * Converts an array to a collection by adding elements to the output collection.
     * Only supported for array types.
     *
     * @param <E> the element type
     * @param x the array to convert
     * @param output the collection to add elements to
     * @throws UnsupportedOperationException if not an array type
     */
    <E> void array2Collection(T x, Collection<E> output);

    /**
     * Calculates the hash code for a value of this type.
     *
     * @param x the value
     * @return the hash code
     */
    int hashCode(T x);

    /**
     * Calculates the deep hash code for a value of this type.
     * For arrays and collections, includes nested element hash codes.
     *
     * @param x the value
     * @return the deep hash code
     */
    int deepHashCode(T x);

    /**
     * Checks equality between two values of this type.
     *
     * @param x the first value
     * @param y the second value
     * @return {@code true} if the values are equal
     */
    boolean equals(T x, T y);

    /**
     * Checks deep equality between two values of this type.
     * For arrays and collections, compares nested elements.
     *
     * @param x the first value
     * @param y the second value
     * @return {@code true} if the values are deeply equal
     */
    boolean deepEquals(T x, T y);

    /**
     * Converts a value to its string representation.
     * May include type information or formatting.
     *
     * @param x the value
     * @return the string representation
     */
    String toString(T x);

    /**
     * Converts a value to its deep string representation.
     * For arrays and collections, includes nested element strings.
     *
     * @param x the value
     * @return the deep string representation
     */
    String deepToString(T x);

    /**
     * Enumeration of serialization type categories.
     * Used to classify how different types should be serialized.
     */
    enum SerializationType {
        /** Directly serializable types (primitives, strings, dates, etc.) */
        SERIALIZABLE,
        /** Entity/bean types with properties */
        ENTITY,
        /** Map types */
        MAP,
        /** Array types */
        ARRAY,
        /** Collection types */
        COLLECTION,
        /** MapEntity types */
        MAP_ENTITY,
        /** EntityId types */
        ENTITY_ID,
        /** Dataset types */
        DATA_SET,
        /** Sheet types */
        SHEET,
        /** Unknown or unsupported serialization */
        UNKNOWN
    }
}