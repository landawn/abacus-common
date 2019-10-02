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
 * The Class Where.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Where extends Clause {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3408850761610296780L;

    /**
     * Instantiates a new where.
     */
    // For Kryo
    Where() {
    }

    /**
     * Instantiates a new where.
     *
     * @param condition
     */
    public Where(Condition condition) {
        super(Operator.WHERE, condition);
    }
}
