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
public final class LocalRWLock<T> extends AbstractRWLock<T> {
    private static final Logger logger = LoggerFactory.getLogger(LocalRWLock.class);

    private final Map<T, RefReentrantReadWriteLock> blockedLockPool = new ConcurrentHashMap<T, RefReentrantReadWriteLock>();

    private final long timeout;

    public LocalRWLock() {
        this(DEFAULT_TIMEOUT);
    }

    public LocalRWLock(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void lockWriteOn(T target) {
        lockWriteOn(target, timeout);
    }

    @Override
    public void lockWriteOn(T target, long timeout) {
        checkTargetObject(target);

        RefReentrantReadWriteLock refRWLock = getOrCreateLock(target);

        boolean isOk = false;

        try {
            isOk = refRWLock.writeLock().tryLock(timeout, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            throw new AbacusException("Failed to lock write on target: " + N.stringOf(target), e);
        } finally {
            if (!isOk) {
                closeLock(target, refRWLock);
            }
        }
    }

    @Override
    public void unlockWriteOn(T target) {
        checkTargetObject(target);

        RefReentrantReadWriteLock refRWLock = getTargetLock(target);

        if (refRWLock == null) {
            return;
        }

        try {
            refRWLock.writeLock().unlock();
        } finally {
            closeLock(target, refRWLock);
        }
    }

    @Override
    public void lockReadOn(T target) {
        lockReadOn(target, timeout);
    }

    @Override
    public void lockReadOn(T target, long timeout) {
        checkTargetObject(target);

        RefReentrantReadWriteLock refRWLock = getOrCreateLock(target);

        boolean isOk = false;

        try {
            isOk = refRWLock.readLock().tryLock(timeout, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            throw new AbacusException("Failed to lock read on target: " + N.stringOf(target), e);
        } finally {
            if (!isOk) {
                closeLock(target, refRWLock);
            }
        }
    }

    @Override
    public void unlockReadOn(T target) {
        checkTargetObject(target);

        RefReentrantReadWriteLock refRWLock = getTargetLock(target);

        if (refRWLock == null) {
            return;
        }

        try {
            refRWLock.readLock().unlock();
        } finally {
            closeLock(target, refRWLock);
        }
    }

    private RefReentrantReadWriteLock getTargetLock(T target) {
        synchronized (blockedLockPool) {
            return blockedLockPool.get(target);
        }
    }

    private RefReentrantReadWriteLock getOrCreateLock(T target) {
        synchronized (blockedLockPool) {
            RefReentrantReadWriteLock refRWLock = blockedLockPool.get(target);

            if (refRWLock == null) {
                refRWLock = new RefReentrantReadWriteLock();

                blockedLockPool.put(target, refRWLock);
            }

            refRWLock.incrementRefCount();

            return refRWLock;
        }
    }

    private void closeLock(T target, RefReentrantReadWriteLock refRWLock) {
        synchronized (blockedLockPool) {
            refRWLock.decrementRefCount();

            if (refRWLock.getRefCount() <= 0) {
                blockedLockPool.remove(target);

                if (refRWLock.getRefCount() < 0) {

                    if (logger.isWarnEnabled()) {
                        logger.warn("The reference count on the lock is less than 0 for object: " + N.toString(target));
                    }
                }
            }
        }
    }
}
