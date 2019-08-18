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

package com.landawn.abacus.lock;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractRWLock.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public abstract class AbstractRWLock<T> implements RWLock<T> {

    /**
     * Check target object.
     *
     * @param target
     */
    protected void checkTargetObject(T target) {
        if (target == null) {
            throw new NullPointerException("The target object can't be null");
        }
    }
}
