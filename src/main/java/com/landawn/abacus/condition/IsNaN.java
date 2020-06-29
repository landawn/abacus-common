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

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class IsNaN extends Is {

    private static final long serialVersionUID = 7088820715466964254L;

    /**
     * Field NAN.
     */
    static final Expression NAN = CF.expr("NAN");

    // For Kryo
    IsNaN() {
    }

    public IsNaN(String propName) {
        super(propName, NAN);
    }
}
