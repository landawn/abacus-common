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

import javax.jws.WebService;

// TODO: Auto-generated Javadoc
/**
 * The Interface RWLock.
 *
 * @param <T> the generic type
 */
@WebService
/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface RWLock<T> {

    /** The Constant DEFAULT_TIMEOUT. */
    public static final long DEFAULT_TIMEOUT = 3600 * 1000L;

    /**
     * Method lockWriteOn.
     *
     * @param target the target
     */
    public void lockWriteOn(T target);

    /**
     * Lock write on.
     *
     * @param target the target
     * @param timeout the timeout
     */
    public void lockWriteOn(T target, long timeout);

    /**
     * Method unlockWriteOn.
     *
     * @param target the target
     */
    public void unlockWriteOn(T target);

    /**
     * Method lockReadOn.
     *
     * @param target the target
     */
    public void lockReadOn(T target);

    /**
     * Lock read on.
     *
     * @param target the target
     * @param timeout the timeout
     */
    public void lockReadOn(T target, long timeout);

    /**
     * Method unlockReadOn.
     *
     * @param target the target
     */
    public void unlockReadOn(T target);
}
