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

import java.util.List;

import com.landawn.abacus.util.NamingPolicy;

// TODO: Auto-generated Javadoc
/**
 * The Interface Condition.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface Condition {
    /**
     * 
     * @return Operator
     */
    Operator getOperator();

    /**
     *
     * @param condition
     * @return And
     */
    And and(Condition condition);

    /**
     *
     * @param condition
     * @return Or
     */
    Or or(Condition condition);

    /**
     * 
     * @return Not
     */
    Not not();

    /**
     *
     * @param <T>
     * @return T
     */
    <T extends Condition> T copy();

    /**
     * 
     * @return List<Object>
     */
    List<Object> getParameters();

    /**
     * Method clearParameters.
     */
    void clearParameters();

    /**
     *
     * @param namingPolicy
     * @return
     */
    String toString(NamingPolicy namingPolicy);
}
