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

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.MemcachedLock;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class MemcachedRWLock.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public class MemcachedRWLock<T> extends AbstractRWLock<T> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(MemcachedRWLock.class);

    /** The Constant DEFAULT_LIVE_TIME. */
    static final long DEFAULT_LIVE_TIME = 3600 * 1000L;

    /**
     * MUST NOT CHANGE THE VALUE.
     * 
     */
    private final String keyPrefix;

    /**
     * MUST NOT CHANGE THE VALUE.
     * 
     */
    private final String readKeyPrefix;

    /**
     * MUST NOT CHANGE THE VALUE.
     * 
     */
    private final String writeKeyPrefix;

    /** Filed memcachedLock. */
    private final MemcachedLock<String, Number> mLock;

    /**
     * Field liveTime. unit is milliseconds
     */
    private final long liveTime;

    /**
     * Field timeout. unit is milliseconds
     */
    private final long timeout;

    /**
     * Instantiates a new memcached RW lock.
     *
     * @param servers
     */
    public MemcachedRWLock(String servers) {
        this(servers, N.EMPTY_STRING, DEFAULT_LIVE_TIME, DEFAULT_TIMEOUT);
    }

    /**
     * Instantiates a new memcached RW lock.
     *
     * @param servers
     * @param keyPrefix
     * @param liveTime
     */
    public MemcachedRWLock(String servers, String keyPrefix, String liveTime) {
        this(servers, keyPrefix, Long.valueOf(liveTime), DEFAULT_TIMEOUT);
    }

    /**
     * Instantiates a new memcached RW lock.
     *
     * @param servers
     * @param keyPrefix
     * @param liveTime
     * @param timeout
     */
    public MemcachedRWLock(String servers, String keyPrefix, long liveTime, long timeout) {
        this.mLock = new MemcachedLock<String, Number>(servers);

        this.liveTime = liveTime;
        this.timeout = timeout;

        this.keyPrefix = keyPrefix;
        readKeyPrefix = keyPrefix + "_READ_";
        writeKeyPrefix = keyPrefix + "_WRITE_";
    }

    /**
     * Lock write on.
     *
     * @param target
     */
    @Override
    public void lockWriteOn(T target) {
        lockWriteOn(target, timeout);
    }

    /**
     * Lock write on.
     *
     * @param target
     * @param timeout
     */
    @Override
    public void lockWriteOn(T target, long timeout) {
        checkTargetObject(target);

        final long endTime = System.currentTimeMillis() + timeout;

        final String key = generateKey(keyPrefix, target);

        do {
            if (mLock.lock(key, liveTime)) {
                try {
                    String readKey = generateKey(readKeyPrefix, target);

                    boolean isOK = false;
                    do {
                        try {
                            Object count = mLock.client().get(readKey);

                            if (count == null) {
                                isOK = true;

                                break;
                            } else if (N.parseInt(count.toString()) <= 0) {
                                mLock.client().delete(readKey);
                                isOK = true;

                                break;
                            }
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Failed to retrive with key(" + readKey + ")", e);
                            }
                        }
                    } while (endTime - System.currentTimeMillis() > 0);

                    if (isOK) {
                        isOK = false;
                    } else {
                        throw new AbacusException(
                                "Failed to lock 'write' on the target object: " + N.toString(target) + " because 'read' is locked on it with key: " + readKey);
                    }

                    String writeKey = generateKey(writeKeyPrefix, target);

                    do {
                        try {
                            Object count = mLock.client().get(writeKey);

                            if (count == null) {
                                isOK = true;

                                break;
                            } else if (N.parseInt(count.toString()) <= 0) {
                                mLock.client().delete(writeKey);
                                isOK = true;

                                break;
                            }
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Failed to retrive with key(" + writeKey + ")", e);
                            }
                        }
                    } while (endTime - System.currentTimeMillis() > 0);

                    if (isOK) {
                        isOK = false;
                    } else {
                        throw new AbacusException("Failed to lock 'write' on the target object: " + N.toString(target)
                                + " because 'write' is locked on it with key: " + writeKey);
                    }

                    try {
                        if (mLock.lock(writeKey, 1, liveTime)) {
                            throw new AbacusException("Failed to lock 'write' on the target object: " + N.toString(target) + " with key: " + writeKey);
                        }
                    } catch (Exception e) {
                        if (mLock.client().get(writeKey) == null) {
                            throw new AbacusException("Failed to lock 'write' on the target object: " + N.toString(target) + " with key: " + writeKey);
                        }
                    }
                } finally {
                    mLock.unlock(key);
                }

                return;
            }
        } while (endTime - System.currentTimeMillis() > 0);

        throw new AbacusException("Failed to lock the target object: " + N.toString(target) + " with key: " + key);
    }

    /**
     * Unlock write on.
     *
     * @param target
     */
    @Override
    public void unlockWriteOn(T target) {
        checkTargetObject(target);

        String writeKey = generateKey(writeKeyPrefix, target);

        if (!mLock.unlock(writeKey)) {
            throw new AbacusException("Failed to unlock 'write' on the target object: " + N.toString(target) + " with key: " + writeKey);
        }
    }

    /**
     * Lock read on.
     *
     * @param target
     */
    @Override
    public void lockReadOn(T target) {
        lockReadOn(target, timeout);
    }

    /**
     * Lock read on.
     *
     * @param target
     * @param timeout
     */
    @Override
    public void lockReadOn(T target, long timeout) {
        checkTargetObject(target);

        final long endTime = System.currentTimeMillis() + timeout;

        final String key = generateKey(keyPrefix, target);

        do {
            if (mLock.lock(key, liveTime)) {
                try {
                    String writeKey = generateKey(writeKeyPrefix, target);
                    boolean isOK = false;

                    do {
                        try {
                            Object count = mLock.client().get(writeKey);

                            if (count == null) {
                                isOK = true;

                                break;
                            } else if (N.parseInt(count.toString()) <= 0) {
                                mLock.client().delete(writeKey);
                                isOK = true;

                                break;
                            }
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn("Failed to retrive with key(" + writeKey + ")", e);
                            }
                        }
                    } while (endTime - System.currentTimeMillis() > 0);

                    if (isOK) {
                        isOK = false;
                    } else {
                        throw new AbacusException("Failed to lock 'write' on the target object: " + N.toString(target)
                                + " because 'write' is locked on it with key: " + writeKey);
                    }

                    String readKey = generateKey(readKeyPrefix, target);

                    if (mLock.client().incr(readKey, 1, 1, liveTime) <= 0) {
                        throw new AbacusException("Failed to lock 'read' the target object: " + N.toString(target) + " with key: " + readKey);
                    }
                } finally {
                    mLock.unlock(key);
                }

                return;
            }
        } while (endTime - System.currentTimeMillis() > 0);

        throw new AbacusException("Failed to lock the target object: " + N.toString(target) + " with key: " + key);

    }

    /**
     * Unlock read on.
     *
     * @param target
     */
    @Override
    public void unlockReadOn(T target) {
        checkTargetObject(target);

        String readKey = generateKey(readKeyPrefix, target);

        if (mLock.client().decr(readKey, 1, 0, liveTime) < 0) {
            throw new AbacusException("Failed to unlock 'read' on the target object: " + N.toString(target) + " with key: " + readKey);
        }
    }

    /**
     *
     * @param keyPrefix
     * @param obj
     * @return
     */
    protected String generateKey(String keyPrefix, T obj) {
        return N.isNullOrEmpty(keyPrefix) ? N.base64Encode(N.stringOf(obj).getBytes()) : (keyPrefix + N.base64Encode(N.stringOf(obj).getBytes()));
    }
}
