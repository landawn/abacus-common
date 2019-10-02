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

// TODO: Auto-generated Javadoc
/**
 * The Class Or.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Or extends Junction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -2842422533648510863L;

    /**
     * Instantiates a new or.
     */
    // For Kryo
    Or() {
    }

    /**
     * Instantiates a new or.
     *
     * @param condition
     */
    @SafeVarargs
    public Or(Condition... condition) {
        super(Operator.OR, condition);
    }

    /**
     * Instantiates a new or.
     *
     * @param conditions
     */
    public Or(Collection<? extends Condition> conditions) {
        super(Operator.OR, conditions);
    }
}
