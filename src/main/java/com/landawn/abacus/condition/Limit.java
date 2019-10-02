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

package com.landawn.abacus.condition;

import static com.landawn.abacus.util.WD._SPACE;

import java.util.List;

import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

// TODO: Auto-generated Javadoc
/**
 * The Class Limit.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Limit extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -8582266239631072254L;

    /** The count. */
    private int count;

    /** The offset. */
    private int offset;

    /**
     * Instantiates a new limit.
     */
    // For Kryo
    Limit() {
    }

    /**
     * Constructor for LIMIT.
     *
     * @param count
     */
    public Limit(int count) {
        this(0, count);
    }

    /**
     * Instantiates a new limit.
     *
     * @param offset
     * @param count
     */
    public Limit(int offset, int count) {
        super(Operator.LIMIT);
        this.count = count;
        this.offset = offset;
    }

    /**
     * Gets the count.
     *
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets the offset.
     *
     * @return
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getParameters() {
        return N.emptyList();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        // do nothing.
    }

    /**
     *
     * @param condition
     * @return
     */
    @Override
    public And and(Condition condition) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param condition
     * @return
     */
    @Override
    public Or or(Condition condition) {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @return
     */
    @Override
    public Not not() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        return offset > 0 ? WD.LIMIT + _SPACE + count + _SPACE + WD.OFFSET + _SPACE + offset : WD.LIMIT + _SPACE + count;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + count;
        h = (h * 31) + offset;

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Limit) {
            Limit other = (Limit) obj;

            return (count == other.count) && (offset == other.offset);
        }

        return false;
    }
}
