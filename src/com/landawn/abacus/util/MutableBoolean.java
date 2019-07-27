/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.Serializable;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at The Apache Software Foundation (http://www.apache.org/), or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 * 
 * A mutable <code>boolean</code> wrapper.
 * <p>
 * Note that as MutableBoolean does not extend Boolean, it is not treated by String.format as a Boolean parameter. 
 * 
 * @see Boolean
 * @since 2.2
 * @version $Id: MutableBoolean.java 1669791 2015-03-28 15:22:59Z britter $
 */
public final class MutableBoolean implements Mutable, Serializable, Comparable<MutableBoolean> {

    /**
     * Required for serialization support.
     * 
     * @see java.io.Serializable
     */
    private static final long serialVersionUID = -4830728138360036487L;

    /** The mutable value. */
    private boolean value;

    /**
     * Constructs a new MutableBoolean with the default value of false.
     */
    MutableBoolean() {
        super();
    }

    /**
     * Constructs a new MutableBoolean with the specified value.
     * 
     * @param value  the initial value to store
     */
    MutableBoolean(final boolean value) {
        super();
        this.value = value;
    }

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
     * @return the value as a Boolean, never null
     * @deprecated replace by {@link #value()}.
     */
    @Deprecated
    public boolean getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value  the value to set
     */
    public void setValue(final boolean value) {
        this.value = value;
    }

    public boolean getAndSet(final boolean value) {
        final boolean result = this.value;
        this.value = value;
        return result;
    }

    public boolean setAndGet(final boolean value) {
        this.value = value;
        return this.value;
    }

    public boolean getAndInvert() {
        final boolean result = value;
        value = !value;
        return result;
    }

    public boolean invertAndGet() {
        value = !value;
        return this.value;
    }

    /**
     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
     * Otherwise just return <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate - test the current value.
     * @return
     */
    public <E extends Exception> boolean setIf(boolean newValue, Try.BooleanPredicate<E> predicate) throws E {
        if (predicate.test(this.value)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Set with the specified new value and returns <code>true</code> if <code>predicate</code> returns true.
     * Otherwise just return <code>false</code> without setting the value to new value.
     * 
     * @param newValue
     * @param predicate the first parameter is the current value, the second parameter is the new value.
     * @return
     */
    public <E extends Exception> boolean setIf(boolean newValue, Try.BooleanBiPredicate<E> predicate) throws E {
        if (predicate.test(this.value, newValue)) {
            this.value = newValue;
            return true;
        }

        return false;
    }

    /**
     * Sets the value to true.
     * 
     * @since 3.3
     */
    public void setFalse() {
        this.value = false;
    }

    /**
     * Sets the value to false.
     * 
     * @since 3.3
     */
    public void setTrue() {
        this.value = true;
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the current value is <code>true</code>.
     * 
     * @return <code>true</code> if the current value is <code>true</code>
     * @since 2.5
     */
    public boolean isTrue() {
        return value == true;
    }

    /**
     * Checks if the current value is <code>false</code>.
     * 
     * @return <code>true</code> if the current value is <code>false</code>
     * @since 2.5
     */
    public boolean isFalse() {
        return value == false;
    }

    public void invert() {
        this.value = !this.value;
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this mutable to another in ascending order.
     * 
     * @param other  the other mutable to compare to, not null
     * @return negative if this is less, zero if equal, positive if greater
     *  where false is less than true
     */
    @Override
    public int compareTo(final MutableBoolean other) {
        return (this.value == other.value) ? 0 : (this.value ? 1 : -1);
    }

    //-----------------------------------------------------------------------
    /**
     * Compares this object to the specified object. The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is an <code>MutableBoolean</code> object that contains the same
     * <code>boolean</code> value as this object.
     * 
     * @param obj  the object to compare with, null returns false
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
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
     * @return the hash code returned by <code>Boolean.TRUE</code> or <code>Boolean.FALSE</code>
     */
    @Override
    public int hashCode() {
        return value ? Boolean.TRUE.hashCode() : Boolean.FALSE.hashCode();
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the String value of this mutable.
     * 
     * @return the mutable value as a string
     */
    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
