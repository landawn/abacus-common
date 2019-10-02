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
 * The Class GreaterEqual.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class GreaterEqual extends Binary {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 5771377223349523638L;

    /**
     * Instantiates a new greater equal.
     */
    // For Kryo
    GreaterEqual() {
    }

    /**
     * Instantiates a new greater equal.
     *
     * @param propName
     * @param propValue
     */
    public GreaterEqual(String propName, Object propValue) {
        super(propName, Operator.GREATER_EQUAL, propValue);
    }
}
