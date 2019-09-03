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

import com.landawn.abacus.LockMode;

// TODO: Auto-generated Javadoc
/**
 * The Interface XLock.
 *
 * @param <T>
 */
@WebService
/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface XLock<T> {

    /** The Constant DEFAULT_TIMEOUT. */
    public static final long DEFAULT_TIMEOUT = 3 * 1000L;

    /**
     *
     * @param target
     * @param lockMode
     * @param refLockCode
     * @return
     */
    public String lock(T target, LockMode lockMode, String refLockCode);

    /**
     *
     * @param target
     * @param lockMode
     * @param refLockCode
     * @param timeout
     * @return String
     */
    public String lock(T target, LockMode lockMode, String refLockCode, long timeout);

    /**
     *
     * @param target
     * @param requiredLockMode
     * @param refLockCode
     * @return boolean
     */
    public boolean isLocked(T target, LockMode requiredLockMode, String refLockCode);

    /**
     *
     * @param target
     * @param refLockCode
     * @return boolean
     */
    public boolean unlock(T target, String refLockCode);
}
