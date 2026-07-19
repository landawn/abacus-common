/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.Beta;

/**
 * {@code ImmutableCollection} is a base class for read-only collection implementations.
 * This class extends {@link AbstractCollection} and implements the {@link Immutable} interface,
 * representing a collection that cannot be modified through its own API.
 *
 * <p>All mutating operations ({@code add}, {@code remove}, {@code clear}, etc.) throw
 * {@link UnsupportedOperationException}. The collection provides read-only access to its
 * elements through standard collection methods like {@link #contains(Object)}, {@link #size()},
 * and {@link #iterator()}.</p>
 *
 * <p>{@link #wrap(Collection)} retains the supplied collection as its backing storage. Changes made
 * directly to that collection are therefore visible through the returned view; its thread-safety is
 * the thread-safety of the backing collection.</p>
 *
 * <p>This class serves as the base for other immutable collection types in the framework,
 * such as {@link ImmutableList} and {@link ImmutableSet}. It should generally not be
 * instantiated directly; use the static factory method {@link #wrap(Collection)} or the
 * specific immutable collection types instead.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Collection<String> mutable = Arrays.asList("a", "b", "c");
 * ImmutableCollection<String> immutable = ImmutableCollection.wrap(mutable);
 * System.out.println(immutable.contains("b"));   // returns true
 * // immutable.add("d");   // throws UnsupportedOperationException
 * }</pre>
 *
 * @param <E> the type of elements in this collection
 * @see Immutable
 * @see ImmutableList
 * @see ImmutableSet
 */
@com.landawn.abacus.annotation.Immutable
public class ImmutableCollection<E> extends AbstractCollection<E> implements Immutable {

    final Collection<E> coll;

    /**
     * Constructs an {@code ImmutableCollection} backed by the provided collection.
     * The collection reference is stored directly; subclasses that need modification
     * protection should wrap the collection (e.g. via {@link java.util.Collections#unmodifiableCollection})
     * before passing it to this constructor.
     *
     * @param c the backing collection; must not be {@code null}
     */
    protected ImmutableCollection(final Collection<? extends E> c) {
        coll = (Collection<E>) c;
    }

    /**
     * Wraps the given collection into an ImmutableCollection. If the given collection is {@code null},
     * an empty ImmutableList is returned. If the given collection is already an instance of
     * ImmutableCollection, it is directly returned. Otherwise, returns a new ImmutableCollection
     * backed by the provided Collection.
     *
     * <p><b>Warning:</b> Changes to the specified Collection will be reflected in the ImmutableCollection.
     * This method does not create a defensive copy. For a {@code true} immutable copy, use the specific
     * immutable collection type's copyOf method (e.g., {@link ImmutableList#copyOf}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> mutableList = new ArrayList<>();
     * mutableList.add("hello");
     * ImmutableCollection<String> wrapped = ImmutableCollection.wrap(mutableList);
     * mutableList.add("world");             // this change is visible in wrapped!
     * System.out.println(wrapped.size());   // prints 2
     * }</pre>
     *
     * @param <E> the type of elements in the collection
     * @param c the collection to be wrapped into an ImmutableCollection
     * @return an ImmutableCollection that contains the elements of the given collection; an empty
     *         ImmutableList if {@code c} is {@code null}, or {@code c} itself if it is already an ImmutableCollection
     */
    @Beta
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) {
        if (c == null) {
            return ImmutableList.empty();
        } else if (c instanceof ImmutableCollection) {
            return (ImmutableCollection<E>) c;
        }

        return new ImmutableCollection<>(c);
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param e the element to add (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean add(final E e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param newElements the collection of elements to add (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean addAll(final Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param object the element to remove (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean remove(final Object object) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param filter the predicate to use for filtering (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean removeIf(final Predicate<? super E> filter) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param oldElements the collection of elements to remove (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean removeAll(final Collection<?> oldElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param elementsToKeep the collection of elements to retain (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final boolean retainAll(final Collection<?> elementsToKeep) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableCollection.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableCollection does not support modification operations
     */
    @Deprecated
    @Override
    public final void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns {@code true} if this collection contains the specified element.
     * More formally, returns {@code true} if and only if this collection contains
     * at least one element {@code e} such that
     * {@code (valueToFind==null ? e==null : valueToFind.equals(e))}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<String> collection = ImmutableList.of("a", "b", "c");
     * System.out.println(collection.contains("b"));   // returns true
     * System.out.println(collection.contains("d"));   // returns false
     * }</pre>
     *
     * @param valueToFind element whose presence in this collection is to be tested
     * @return {@code true} if this collection contains the specified element
     * @throws ClassCastException if the type of the specified element is incompatible with this collection (optional)
     * @throws NullPointerException if the specified element is {@code null} and this collection
     *         does not permit {@code null} elements (optional)
     */
    @Override
    public boolean contains(final Object valueToFind) {
        return coll.contains(valueToFind);
    }

    /**
     * Returns an iterator over the elements in this collection.
     * The returned iterator's {@code remove()} method throws {@link UnsupportedOperationException}.
     *
     * <p>The order of elements returned by the iterator depends on the underlying
     * collection type. For ordered collections (like List), the iteration order
     * is predictable. For unordered collections (like Set), the order may vary.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<Integer> numbers = ImmutableList.of(1, 2, 3);
     * ObjIterator<Integer> iter = numbers.iterator();
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     * }
     * }</pre>
     *
     * @return an {@link ObjIterator} over the elements in this collection
     */
    @Override
    public ObjIterator<E> iterator() {
        final java.util.Iterator<E> iter = coll.iterator();

        // ObjIterator.of may return a mutable ObjIterator subclass unchanged.
        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public E next() {
                return iter.next();
            }
        };
    }

    /**
     * Returns the number of elements in this collection. If this collection
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<String> collection = ImmutableList.of("x", "y", "z");
     * System.out.println(collection.size());   // returns 3
     * }</pre>
     *
     * @return the number of elements in this collection
     */
    @Override
    public int size() {
        return coll.size();
    }

    /**
     * Returns an array containing all of the elements in this collection.
     * The returned array will be "safe" in that no references to it are maintained
     * by this collection. The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based APIs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<String> collection = ImmutableList.of("a", "b", "c");
     * Object[] array = collection.toArray();
     * System.out.println(Arrays.toString(array));   // returns [a, b, c]
     * }</pre>
     *
     * @return an array containing all of the elements in this collection
     */
    @Override
    public Object[] toArray() {
        return coll.toArray();
    }

    /**
     * Returns an array containing all of the elements in this collection;
     * the runtime type of the returned array is that of the specified array.
     * If the collection fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the specified
     * array and the size of this collection.
     *
     * <p>If this collection fits in the specified array with room to spare
     * (i.e., the array has more elements than this collection), the element in
     * the array immediately following the end of the collection is set to {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<String> collection = ImmutableList.of("x", "y", "z");
     * String[] array = collection.toArray(new String[0]);
     * System.out.println(Arrays.toString(array));   // returns [x, y, z]
     * }</pre>
     *
     * @param <T> the runtime type of the array to contain the collection
     * @param a the array into which the elements of this collection are to be
     *        stored, if it is big enough; otherwise, a new array of the same
     *        runtime type is allocated for this purpose
     * @return an array containing all of the elements in this collection
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in this collection
     * @throws NullPointerException if the specified array is {@code null}
     */
    @Override
    public <T> T[] toArray(final T[] a) {
        return coll.toArray(a);
    }

    /**
     * Compares the specified object with this collection for equality. The base
     * {@code ImmutableCollection} uses identity equality, matching the convention of
     * {@link AbstractCollection}. Concrete {@link List} and {@link Set} implementations use
     * their interface-defined value equality.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<Integer> col1 = ImmutableList.of(1, 2, 3);
     * ImmutableCollection<Integer> col2 = ImmutableList.of(1, 2, 3);
     * ImmutableCollection<Integer> col3 = ImmutableList.of(4, 5, 6);
     * System.out.println(col1.equals(col2));   // returns true
     * System.out.println(col1.equals(col3));   // returns false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this collection
     * @return {@code true} if the specified object is equal to this collection
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        // Collection itself has no value-equality contract. Delegating a list-backed base
        // wrapper to List.equals would make wrapper.equals(list) true while
        // list.equals(wrapper) is false because the wrapper is not a List.
        if (!(this instanceof List<?>) && !(this instanceof Set<?>)) {
            return false;
        }

        // The backing-collection shortcut is only valid when the other side presents its backing
        // collection in its natural order; a reordered view (e.g. ImmutableList.reversed()) shares
        // the FORWARD collection as its backing, so unwrapping would compare the wrong order.
        if (obj instanceof ImmutableCollection ic && !ic.isReorderedView()) {
            return coll.equals(ic.coll);
        }

        return coll.equals(obj);
    }

    /**
     * Whether this collection presents its backing collection in a different iteration order
     * (e.g. a reversed view). Such instances must not be compared via the backing-collection
     * shortcut in {@link #equals(Object)}.
     *
     * @return {@code true} if this is a reordered view of its backing collection
     */
    boolean isReorderedView() {
        return false;
    }

    /**
     * Returns the hash code value for this collection. The base
     * {@code ImmutableCollection} uses an identity hash code; concrete {@link List} and
     * {@link Set} implementations use their interface-defined value hash code.
     *
     * @return the hash code value for this collection
     */
    @Override
    public int hashCode() {
        return this instanceof List<?> || this instanceof Set<?> ? coll.hashCode() : System.identityHashCode(this);
    }

    /**
     * Returns a string representation of this collection. The string
     * representation consists of the string representation of the underlying
     * collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableCollection<Integer> collection = ImmutableList.of(1, 2, 3);
     * System.out.println(collection.toString());   // returns [1, 2, 3]
     * }</pre>
     *
     * @return a string representation of this collection
     */
    @Override
    public String toString() {
        return coll.toString();
    }
}
