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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.LockMode;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class LocalXLock.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public final class LocalXLock<T> extends AbstractXLock<T> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(LocalXLock.class);

    /** The blocked lock pool. */
    private final Map<T, ModeLock> blockedLockPool = new ConcurrentHashMap<T, ModeLock>();

    /** The timeout. */
    private final long timeout;

    /**
     * Instantiates a new local X lock.
     */
    public LocalXLock() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * Instantiates a new local X lock.
     *
     * @param timeout the timeout
     */
    public LocalXLock(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Lock.
     *
     * @param target the target
     * @param lockMode the lock mode
     * @param refLockCode the ref lock code
     * @return the string
     */
    @Override
    public String lock(T target, LockMode lockMode, String refLockCode) {
        return lock(target, lockMode, refLockCode, timeout);
    }

    /**
     * Lock.
     *
     * @param target the target
     * @param lockMode the lock mode
     * @param refLockCode the ref lock code
     * @param timeout the timeout
     * @return the string
     */
    @Override
    public String lock(T target, LockMode lockMode, String refLockCode, long timeout) {
        checkTargetObject(target);
        checkLockMode(lockMode);

        if (refLockCode == null) {
            refLockCode = N.uuid();
        }

        final long endTime = System.currentTimeMillis() + timeout;

        final ModeLock modeLock = getOrCreateLock(target);

        boolean isOk = false;

        try {
            if (modeLock.tryLock(endTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)) {
                isOk = true;
            } else {
                return null;
            }
        } catch (InterruptedException e) {

            if (logger.isWarnEnabled()) {
                logger.warn(ExceptionUtil.getMessage(e));
            }

            return null;
        } finally {
            if (!isOk) {
                closeLock(target, modeLock);
            }
        }

        isOk = false;

        try {
            if (modeLock.lockCode != null) {
                do {
                    if (modeLock.lockCode == null) {

                        break;
                    } else {
                        N.sleep(1);
                    }
                } while (endTime - System.currentTimeMillis() > 0);
            }

            if (modeLock.lockCode == null) {
                modeLock.lockCode = refLockCode;
                modeLock.lockMode = lockMode;

                isOk = true;

                return refLockCode;
            }
        } finally {
            modeLock.unlock();

            if (!isOk) {
                closeLock(target, modeLock);
            }
        }

        return null;
    }

    /**
     * Checks if is locked.
     *
     * @param target the target
     * @param requiredLockMode the required lock mode
     * @param refLockCode the ref lock code
     * @return true, if is locked
     */
    @Override
    public boolean isLocked(T target, LockMode requiredLockMode, String refLockCode) {
        checkTargetObject(target);
        checkLockMode(requiredLockMode);

        ModeLock modeLock = getTargetLock(target);

        if (modeLock == null || modeLock.lockCode == null || N.equals(modeLock.lockCode, refLockCode)) {
            return false;
        }

        final LockMode lockMode = modeLock == null ? null : modeLock.lockMode;

        return lockMode != null && lockMode.isXLockOf(requiredLockMode);
    }

    /**
     * Unlock.
     *
     * @param target the target
     * @param refLockCode the ref lock code
     * @return true, if successful
     */
    @Override
    public boolean unlock(T target, String refLockCode) {
        checkTargetObject(target);

        ModeLock modeLock = getTargetLock(target);

        if (modeLock == null) {
            return true;
        }

        boolean isOk = false;

        try {
            if (N.equals(modeLock.lockCode, refLockCode)) {
                modeLock.lockMode = null;
                modeLock.lockCode = null;

                isOk = true;
            } else {
                isOk = false;
            }
        } finally {
            if (isOk) {
                closeLock(target, modeLock);
            }
        }

        return isOk;

    }

    /**
     * Gets the target lock.
     *
     * @param target the target
     * @return the target lock
     */
    private ModeLock getTargetLock(T target) {
        synchronized (blockedLockPool) {
            return blockedLockPool.get(target);
        }
    }

    /**
     * Gets the or create lock.
     *
     * @param target the target
     * @return the or create lock
     */
    private ModeLock getOrCreateLock(T target) {
        ModeLock modeLock = null;

        synchronized (blockedLockPool) {
            modeLock = blockedLockPool.get(target);

            if (modeLock == null) {
                modeLock = new ModeLock();

                blockedLockPool.put(target, modeLock);
            }

            modeLock.incrementRefCount();
        }

        return modeLock;
    }

    /**
     * Close lock.
     *
     * @param target the target
     * @param modeLock the mode lock
     */
    private void closeLock(T target, ModeLock modeLock) {
        synchronized (blockedLockPool) {
            modeLock.decrementRefCount();

            if (modeLock.getRefCount() <= 0) {
                blockedLockPool.remove(target);

                if (modeLock.getRefCount() < 0) {

                    if (logger.isWarnEnabled()) {
                        logger.warn("The reference count on the lock is less than 0 for object: " + N.toString(target));
                    }
                }
            }
        }
    }

    /**
     * The Class ModeLock.
     *
     * @author Haiyang Li
     * @version $Revision: 0.8 $ 07/09/08
     */
    private static class ModeLock extends RefReentrantLock {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 7138974744714225809L;

        /** The lock code. */
        private volatile String lockCode;

        /** The lock mode. */
        private volatile LockMode lockMode;
    }
}
