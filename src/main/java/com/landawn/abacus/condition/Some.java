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
 * The Class Some.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Some extends Cell {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 4763591336208948744L;

    /**
     * Instantiates a new some.
     */
    // For Kryo
    Some() {
    }

    /**
     * Instantiates a new some.
     *
     * @param condition
     */
    public Some(SubQuery condition) {
        super(Operator.SOME, condition);
    }
}
