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
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class LocalXLock<T> extends AbstractXLock<T> {
    private static final Logger logger = LoggerFactory.getLogger(LocalXLock.class);

    private final Map<T, ModeLock> blockedLockPool = new ConcurrentHashMap<T, ModeLock>();

    private final long timeout;

    public LocalXLock() {
        this(DEFAULT_TIMEOUT);
    }

    public LocalXLock(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String lock(T target, LockMode lockMode, String refLockCode) {
        return lock(target, lockMode, refLockCode, timeout);
    }

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
                logger.warn(AbacusException.getErrorMsg(e));
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

    private ModeLock getTargetLock(T target) {
        synchronized (blockedLockPool) {
            return blockedLockPool.get(target);
        }
    }

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
     * @author Haiyang Li
     * 
     * @version $Revision: 0.8 $ 07/09/08
     */
    private static class ModeLock extends RefReentrantLock {
        private static final long serialVersionUID = 7138974744714225809L;

        private volatile String lockCode;
        private volatile LockMode lockMode;
    }
}
