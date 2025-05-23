/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * A mutable {@code boolean} wrapper.
 * <p>
 * Note that as MutableBoolean does not extend Boolean, it is not treated by {@code }String.format} as a Boolean parameter.
 *
 * <p>
 * {@code MutableBoolean} is NOT thread-safe.
 *
 * @version $Id: MutableBoolean.java 1669791 2015-03-28 15:22:59Z britter $
 * @see Boolean
 */
public final class MutableBoolean implements Mutable, Serializable, Comparable<MutableBoolean> {

    @Serial
    private static final long serialVersionUID = -4830728138360036487L;

    private boolean value;

    /**
     * Constructs a new MutableBoolean with the default value of {@code false}.
     */
    MutableBoolean() {
    }

    /**
     * Constructs a new MutableBoolean with the specified value.
     *
     * @param value the initial value to store
     */
    MutableBoolean(final boolean value) {
        this.value = value;
    }

    /**
     *
     * @param value
     * @return
     */
    public static MutableBoolean of(final boolean value) {
        return new MutableBoolean(value);
    }

    public boolean value() {
        return value;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the value as a Boolean instance.
     *
     * @return
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public boolean getValue() { // NOSONAR
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(final boolean value) {
        this.value = value;
    }

    /**
     * Returns the current value and then set new value
     *
     * @param value
     * @return
     */
    public boolean getAndSet(final boolean value) { // NOSONAR
        final boolean result = this.value;
        this.value = value;
        return result;
    }

    /**
     * Sets with the specified value and then return it.
     *
     * @param value
     * @return
     */
    public boolean setAndGet(final boolean value) {
        this.value = value;
        return this.value;
    }

    /**
     * Returns the current value and then invert.
     *
     * @return
     */
    public boolean getAndNegate() { // NOSONAR
        final boolean result = value;
        value = !value;
        return result;
    }

    /**
     * Invert and get.
     *
     * @return
     */
    public boolean negateAndGet() {
        value = !value;
        return value;
    }

    /**
     * Set with the specified new value and returns {@code true} if {@code predicate} returns {@code true}.
     * Otherwise, just return {@code false} without setting the value to new value.
     *
     * @param <E>
     * @param newValue
     * @param predicate - test the current value.
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean setIf(final boolean newValue, final Throwables.BooleanPredicate<E> predicate) throws E {
        if (predicate.test(value)) {
            value = newValue;
            return true;
        }

        return false;
    }

    //    /**
    //     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
    //     * Otherwise, just return <code>false</code> without setting the value to new value.
    //     *
    //     * @param <E>
    //     * @param newValue
    //     * @param predicate the first parameter is the current value, the second parameter is the new value.
    //     * @return
    //     * @throws E the e
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public <E extends Exception> boolean setIf(boolean newValue, Throwables.BooleanBiPredicate<E> predicate) throws E {
    //        if (predicate.test(this.value, newValue)) {
    //            this.value = newValue;
    //            return true;
    //        }
    //
    //        return false;
    //    }

    /**
     * Sets the value to {@code true}.
     *
     */
    public void setFalse() {
        value = false;
    }

    /**
     * Sets the value to {@code false}.
     *
     */
    public void setTrue() {
        value = true;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks if the current value is {@code true}.
     *
     * @return {@code true} if the current value is {@code true}
     */
    public boolean isTrue() { //NOSONAR
        return value;
    }

    /**
     * Checks if the current value is {@code false}.
     *
     * @return {@code true} if the current value is {@code false}
     */
    public boolean isFalse() {
        return !value;
    }

    /**
     * Invert.
     */
    public void invert() {
        value = !value;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this mutable to another in ascending order.
     *
     * @param other the other mutable to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     *  where {@code false} is less than true
     */
    @Override
    public int compareTo(final MutableBoolean other) {
        return (value == other.value) ? 0 : (value ? 1 : -1);
    }

    //-----------------------------------------------------------------------

    /**
     * Compares this object to the specified object. The result is {@code true} if and only if the argument is
     * not {@code null} and is an {@code MutableBoolean} object that contains the same
     * {@code boolean} value as this object.
     *
     * @param obj the object to compare with, {@code null} returns false
     * @return {@code true} if the objects are the same; {@code false} otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableBoolean) {
            return value == ((MutableBoolean) obj).value;
        }
        return false;
    }

    /**
     * Returns a suitable hash code for this mutable.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return value ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode();
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the String value of this mutable.
     *
     * @return
     */
    @Override
    public String toString() {
        return N.stringOf(value);
    }

}
