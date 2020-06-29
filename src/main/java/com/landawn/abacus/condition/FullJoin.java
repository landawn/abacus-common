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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class FullJoin extends Join {

    private static final long serialVersionUID = 685295398958634480L;

    // For Kryo
    FullJoin() {
    }

    public FullJoin(String joinEntity) {
        super(Operator.FULL_JOIN, joinEntity);
    }

    public FullJoin(String joinEntity, Condition condition) {
        super(Operator.FULL_JOIN, joinEntity, condition);
    }

    public FullJoin(Collection<String> joinEntities, Condition condition) {
        super(Operator.FULL_JOIN, joinEntities, condition);
    }
}
