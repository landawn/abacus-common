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
 * The Class CrossJoin.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class CrossJoin extends Join {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3676235178912744582L;

    /**
     * Instantiates a new cross join.
     */
    // For Kryo
    CrossJoin() {
    }

    /**
     * Instantiates a new cross join.
     *
     * @param joinEntity
     */
    public CrossJoin(String joinEntity) {
        super(Operator.CROSS_JOIN, joinEntity);
    }

    /**
     * Instantiates a new cross join.
     *
     * @param joinEntity
     * @param condition
     */
    public CrossJoin(String joinEntity, Condition condition) {
        super(Operator.CROSS_JOIN, joinEntity, condition);
    }

    /**
     * Instantiates a new cross join.
     *
     * @param joinEntities
     * @param condition
     */
    public CrossJoin(Collection<String> joinEntities, Condition condition) {
        super(Operator.CROSS_JOIN, joinEntities, condition);
    }
}
