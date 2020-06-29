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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Limit extends AbstractCondition {

    private static final long serialVersionUID = -8582266239631072254L;

    private int count;

    private int offset;

    private String expr;

    // For Kryo
    Limit() {
    }

    /**
     * Constructor for LIMIT.
     *
     * @param count
     */
    public Limit(final int count) {
        this(0, count);
    }

    public Limit(final int offset, final int count) {
        super(Operator.LIMIT);
        this.count = count;
        this.offset = offset;
    }

    public Limit(final String expr) {
        this(0, Integer.MAX_VALUE);

        this.expr = expr;
    }

    public String getExpr() {
        return expr;
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
    public And and(Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param condition
     * @return
     */
    @Override
    public Or or(Condition condition) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Not not() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        if (N.notNullOrEmpty(expr)) {
            return expr;
        } else {
            return offset > 0 ? WD.LIMIT + _SPACE + count + _SPACE + WD.OFFSET + _SPACE + offset : WD.LIMIT + _SPACE + count;
        }
    }

    @Override
    public int hashCode() {
        if (N.notNullOrEmpty(expr)) {
            return expr.hashCode();
        } else {
            int h = 17;
            h = (h * 31) + count;
            h = (h * 31) + offset;

            return h;
        }
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
            final Limit other = (Limit) obj;

            if (N.notNullOrEmpty(expr)) {
                return this.expr.equals(other.expr);
            } else {
                return (count == other.count) && (offset == other.offset);
            }
        }

        return false;
    }
}
