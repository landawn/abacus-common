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

import com.landawn.abacus.condition.ConditionFactory.CF;

// TODO: Auto-generated Javadoc
/**
 * The Class IsInfinite.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class IsInfinite extends Is {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -5615587022187148300L;

    /**
     * Field INFINITE.
     */
    static final Expression INFINITE = CF.expr("INFINITE");

    /**
     * Instantiates a new checks if is infinite.
     *
     * @param propName
     */
    public IsInfinite(String propName) {
        super(propName, INFINITE);
    }
}
