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

package com.landawn.abacus.logging;

import static org.slf4j.spi.LocationAwareLogger.ERROR_INT;
import static org.slf4j.spi.LocationAwareLogger.WARN_INT;

import org.slf4j.helpers.NOPLoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

/**
 *
 */
class SLF4JLogger extends AbstractLogger {

    private static final String FQCN = SLF4JLogger.class.getName();

    private final org.slf4j.Logger loggerImpl;

    private final LocationAwareLogger locationAwareLogger;

    /**
     *
     *
     * @param name
     */
    public SLF4JLogger(final String name) {
        super(name);
        if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
            throw new RuntimeException("Failed to initilze SLF4J Logger Factory");
        }

        loggerImpl = org.slf4j.LoggerFactory.getLogger(name);
        locationAwareLogger = loggerImpl instanceof LocationAwareLogger ? ((LocationAwareLogger) loggerImpl) : null;
    }

    /**
     * Checks if is trace enabled.
     *
     * @return {@code true}, if is trace enabled
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isTraceEnabled();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void trace(final String msg) {
        loggerImpl.trace(msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        loggerImpl.trace(msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return {@code true}, if is debug enabled
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isDebugEnabled();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void debug(final String msg) {
        loggerImpl.debug(msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        loggerImpl.debug(msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return {@code true}, if is info enabled
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isInfoEnabled();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void info(final String msg) {
        loggerImpl.info(msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void info(final String msg, final Throwable t) {
        loggerImpl.info(msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return {@code true}, if is warn enabled
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isWarnEnabled();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void warn(final String msg) {
        if (locationAwareLogger == null) {
            loggerImpl.warn(msg);
        } else {
            locationAwareLogger.log(null, FQCN, WARN_INT, msg, null, null);
        }
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        if (locationAwareLogger == null) {
            loggerImpl.warn(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, WARN_INT, msg, null, t);
        }
    }

    /**
     * Checks if is error enabled.
     *
     * @return {@code true}, if is error enabled
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isErrorEnabled();
    }

    /**
     *
     * @param msg
     */
    @Override
    public void error(final String msg) {
        if (locationAwareLogger == null) {
            loggerImpl.error(msg);
        } else {
            locationAwareLogger.log(null, FQCN, ERROR_INT, msg, null, null);
        }
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void error(final String msg, final Throwable t) {
        if (locationAwareLogger == null) {
            loggerImpl.error(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, ERROR_INT, msg, null, t);
        }
    }
}
