/*
 * Copyright (C) 2019 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * A Set implementation that uses reference-equality (==) instead of object-equality (equals()) 
 * when comparing elements. This set is backed by an {@link IdentityHashMap}, which means that
 * two elements e1 and e2 are considered equal if and only if (e1==e2).
 * 
 * <p>This class is useful when you need to track object instances rather than logical equality.
 * For example, it can be used to detect circular references or to maintain a collection of
 * unique object instances regardless of their equals() implementation.</p>
 * 
 * <p>This implementation is not synchronized. If multiple threads access an identity hash set
 * concurrently, and at least one of the threads modifies the set, it must be synchronized
 * externally.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IdentityHashSet<String> set = new IdentityHashSet<>();
 * String s1 = new String("hello");
 * String s2 = new String("hello");
 * set.add(s1);
 * set.add(s2);
 * // set.size() == 2, because s1 != s2 (different instances)
 * }</pre>
 *
 * @param <T> the type of elements maintained by this set
 * @see IdentityHashMap
 * @see AbstractSet
 */
public final class IdentityHashSet<T> extends AbstractSet<T> {

    private static final Object VAL = Boolean.TRUE;

    private final IdentityHashMap<T, Object> map;

    /**
     * Constructs a new, empty identity hash set with the default initial capacity (21).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<Object> set = new IdentityHashSet<>();
     * }</pre>
     */
    public IdentityHashSet() {
        map = new IdentityHashMap<>();
    }

    /**
     * Constructs a new, empty identity hash set with the specified initial capacity.
     * The actual initial capacity is the smallest power of two greater than or equal
     * to the specified value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>(100);
     * }</pre>
     *
     * @param initialCapacity the initial capacity of the identity hash set
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public IdentityHashSet(final int initialCapacity) {
        map = N.newIdentityHashMap(initialCapacity);
    }

    /**
     * Constructs a new identity hash set containing the elements in the specified collection.
     * The identity hash set is created with a capacity sufficient to hold the elements
     * in the specified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * IdentityHashSet<String> set = new IdentityHashSet<>(list);
     * }</pre>
     *
     * @param c the collection whose elements are to be placed into this set
     */
    public IdentityHashSet(final Collection<? extends T> c) {
        map = N.newIdentityHashMap(N.size(c));

        addAll(c); // NOSONAR
    }

    /**
     * Adds the specified element to this set if it is not already present using
     * reference-equality comparison. More formally, adds the specified element e to
     * this set if the set contains no element e2 such that (e==e2).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s = "hello";
     * boolean added = set.add(s);        // returns true
     * boolean addedAgain = set.add(s);   // returns false
     * }</pre>
     *
     * @param e element to be added to this set
     * @return {@code true} if this set did not already contain the specified element
     */
    @Override
    public boolean add(final T e) {
        return map.put(e, VAL) == null;
    }

    /**
     * Removes the specified element from this set if it is present using reference-equality
     * comparison. More formally, removes an element e such that (o==e), if this set contains
     * such an element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s = "hello";
     * set.add(s);
     * boolean removed = set.remove(s);   // returns true
     * }</pre>
     *
     * @param o object to be removed from this set, if present
     * @return {@code true} if the set contained the specified element
     */
    @Override
    public boolean remove(final Object o) {
        return map.remove(o) != null;
    }

    /**
     * Returns {@code true} if this set contains all of the elements in the specified collection.
     * The comparison uses reference-equality (==) rather than the equals() method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s1 = "a", s2 = "b";
     * set.add(s1);
     * set.add(s2);
     * boolean result = set.containsAll(Arrays.asList(s1, s2));   // returns true
     * }</pre>
     *
     * @param c collection to be checked for containment in this set
     * @return {@code true} if this set contains all of the elements in the specified collection
     */
    @Override
    public boolean containsAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            return true;
        }

        return map.keySet().containsAll(c);
    }

    /**
     * Adds all of the elements in the specified collection to this set using reference-equality
     * comparison. For each element e in the collection, it is added if the set contains no
     * element e2 such that (e==e2).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * List<String> list = Arrays.asList("a", "b", "c");
     * boolean changed = set.addAll(list);   // returns true
     * }</pre>
     *
     * @param c collection containing elements to be added to this set
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean addAll(final Collection<? extends T> c) {
        boolean modified = false;

        if (N.notEmpty(c)) {
            for (final T e : c) {
                if (add(e)) {
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Removes from this set all of its elements that are contained in the specified collection
     * using reference-equality comparison. For each element e in the collection, removes from
     * this set any element e2 such that (e==e2).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s1 = "a", s2 = "b";
     * set.add(s1);
     * set.add(s2);
     * boolean changed = set.removeAll(Arrays.asList(s1));   // returns true, removes s1
     * }</pre>
     *
     * @param c collection containing elements to be removed from this set
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean modified = false;

        if (N.notEmpty(c)) {
            for (final Object e : c) {
                if (remove(e)) {
                    modified = true;
                }
            }
        }

        return modified;
    }

    /**
     * Retains only the elements in this set that are contained in the specified collection
     * using reference-equality comparison. In other words, removes from this set all of its
     * elements that are not contained in the specified collection based on reference equality.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s1 = "a", s2 = "b", s3 = "c";
     * set.add(s1);
     * set.add(s2);
     * set.add(s3);
     * boolean changed = set.retainAll(Arrays.asList(s1, s2));   // returns true, keeps only s1 and s2
     * }</pre>
     *
     * @param c collection containing elements to be retained in this set
     * @return {@code true} if this set changed as a result of the call
     */
    @Override
    public boolean retainAll(final Collection<?> c) {
        if (N.isEmpty(c)) {
            if (!map.isEmpty()) {
                map.clear();
                return true;
            }
        } else {
            final IdentityHashSet<T> kept = new IdentityHashSet<>(N.min(c.size(), size()));

            for (final Object e : c) {
                if (this.contains(e)) {
                    kept.add((T) e);
                }
            }

            if (kept.size() < this.size()) {
                clear();
                addAll(kept);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if this set contains the specified element using reference-equality
     * comparison. More formally, returns {@code true} if and only if this set contains an
     * element e such that (valueToFind==e).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * String s = "hello";
     * set.add(s);
     * boolean contains = set.contains(s);                        // returns true
     * boolean containsNew = set.contains(new String("hello"));   // returns false
     * }</pre>
     *
     * @param valueToFind element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element based on reference equality
     */
    @Override
    public boolean contains(final Object valueToFind) {
        //noinspection SuspiciousMethodCalls
        return map.containsKey(valueToFind);
    }

    /**
     * Returns an iterator over the elements in this set. The elements are returned in no
     * particular order. The iterator is fail-fast: if the set is modified after the iterator
     * is created, the iterator will throw a {@link java.util.ConcurrentModificationException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * Iterator<String> it = set.iterator();
     * while (it.hasNext()) {
     *     String element = it.next();
     *     // process element
     * }
     * }</pre>
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns an array containing all of the elements in this set. The returned array
     * will be "safe" in that no references to it are maintained by this set. The caller
     * is thus free to modify the returned array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * Object[] array = set.toArray();   // returns array with "a" and "b"
     * }</pre>
     *
     * @return an array containing all of the elements in this set
     */
    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    /**
     * Returns an array containing all of the elements in this set; the runtime type of
     * the returned array is that of the specified array. If the set fits in the specified
     * array, it is returned therein. Otherwise, a new array is allocated with the runtime
     * type of the specified array and the size of this set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * String[] array = set.toArray(new String[0]);   // returns String array with "a" and "b"
     * }</pre>
     *
     * @param <A> the runtime type of the array to contain the collection
     * @param a the array into which the elements of this set are to be stored, if it is
     *          big enough; otherwise, a new array of the same runtime type is allocated
     * @return an array containing all of the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array is not a
     *         supertype of the runtime type of every element in this set
     */
    @Override
    public <A> A[] toArray(final A[] a) {
        return map.keySet().toArray(a);
    }

    /**
     * Returns the number of elements in this set (its cardinality).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * int count = set.size();   // returns 2
     * }</pre>
     *
     * @return the number of elements in this set
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns {@code true} if this set contains no elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * boolean empty = set.isEmpty();   // returns true
     * set.add("a");
     * empty = set.isEmpty();   // returns false
     * }</pre>
     *
     * @return {@code true} if this set contains no elements
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Removes all of the elements from this set. The set will be empty after this call returns.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * set.clear();
     * // set.isEmpty() == true
     * }</pre>
     */
    @Override
    public void clear() {
        map.clear();
    }

    /**
     * Compares the specified object with this set for equality. Returns {@code true} if the
     * specified object is also an IdentityHashSet and the two sets have identical mappings.
     * Note that this compares the internal maps for equality, which means it checks if both
     * sets contain the same object references.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set1 = new IdentityHashSet<>();
     * IdentityHashSet<String> set2 = new IdentityHashSet<>();
     * String s = "hello";
     * set1.add(s);
     * set2.add(s);
     * boolean equal = set1.equals(set2);   // returns true
     * }</pre>
     *
     * @param o object to be compared for equality with this set
     * @return {@code true} if the specified object is equal to this set
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof IdentityHashSet) {
            return ((IdentityHashSet) o).map.equals(map);
        }

        return false;
    }

    /**
     * Returns the hash code value for this set. The hash code of a set is defined to be
     * the sum of the hash codes of the elements in the set. This ensures that
     * s1.equals(s2) implies that s1.hashCode()==s2.hashCode() for any two sets s1 and s2,
     * as required by the general contract of {@link Object#hashCode}.
     *
     * @return the hash code value for this set
     */
    @Override
    public int hashCode() {
        return map.keySet().hashCode();
    }

    /**
     * Returns a string representation of this set. The string representation consists of
     * a list of the set's elements in the order they are returned by its iterator, enclosed
     * in square brackets ("[]"). Adjacent elements are separated by the characters ", "
     * (comma and space).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IdentityHashSet<String> set = new IdentityHashSet<>();
     * set.add("a");
     * set.add("b");
     * String str = set.toString();   // returns something like "[a, b]"
     * }</pre>
     *
     * @return a string representation of this set
     */
    @Override
    public String toString() {
        return map.keySet().toString();
    }
}
