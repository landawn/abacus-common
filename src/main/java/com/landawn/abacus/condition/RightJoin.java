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
 * The Class RightJoin.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class RightJoin extends Join {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8066983231406226947L;

    /**
     * Instantiates a new right join.
     */
    // For Kryo
    RightJoin() {
    }

    /**
     * Instantiates a new right join.
     *
     * @param joinEntity
     */
    public RightJoin(String joinEntity) {
        super(Operator.RIGHT_JOIN, joinEntity);
    }

    /**
     * Instantiates a new right join.
     *
     * @param joinEntity
     * @param condition
     */
    public RightJoin(String joinEntity, Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntity, condition);
    }

    /**
     * Instantiates a new right join.
     *
     * @param joinEntities
     * @param condition
     */
    public RightJoin(Collection<String> joinEntities, Condition condition) {
        super(Operator.RIGHT_JOIN, joinEntities, condition);
    }
}
