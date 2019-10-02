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

import java.util.Collection;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * It's recommended to use {@code On}, instead of {@code Using}. Because {@code Using} is associated with the table
 * column, not the object property.
 *
 * @author Haiyang Li
 * @since 0.8
 * @deprecated Using.
 */
@Deprecated
public class Using extends Cell {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7269824120390077165L;

    /**
     * Instantiates a new using.
     */
    // For Kryo
    Using() {
    }

    /**
     * Instantiates a new using.
     *
     * @param columnNames
     */
    @SafeVarargs
    public Using(String... columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Instantiates a new using.
     *
     * @param columnNames
     */
    public Using(Collection<String> columnNames) {
        super(Operator.USING, createUsingCondition(columnNames));
    }

    /**
     * Creates the using condition.
     *
     * @param columnNames
     * @return
     */
    static Condition createUsingCondition(String... columnNames) {
        if (N.isNullOrEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames can't be null or empty");
        }

        return CF.expr(concatPropNames(columnNames));
    }

    /**
     * Creates the using condition.
     *
     * @param columnNames
     * @return
     */
    static Condition createUsingCondition(Collection<String> columnNames) {
        if (N.isNullOrEmpty(columnNames)) {
            throw new IllegalArgumentException("To create the using condition, columnNames " + columnNames + " must has one or more than one column name. ");
        }

        return CF.expr(concatPropNames(columnNames));
    }
}
