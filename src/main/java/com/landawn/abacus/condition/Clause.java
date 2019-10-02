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

// TODO: Auto-generated Javadoc
/**
 * The Class Clause.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class Clause extends Cell {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -4851192970714119122L;

    /**
     * Instantiates a new clause.
     */
    // For Kryo
    Clause() {
    }

    /**
     * Instantiates a new clause.
     *
     * @param operator
     * @param condition
     */
    protected Clause(Operator operator, Condition condition) {
        super(operator, condition);
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
}
