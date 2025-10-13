/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * Logger implementation that delegates to SLF4J (Simple Logging Facade for Java).
 * 
 * <p>This implementation provides a bridge to SLF4J, allowing the use of any SLF4J-compatible
 * logging backend (Logback, Log4j, etc.). It supports location-aware logging when the underlying
 * SLF4J implementation provides it, ensuring accurate caller location information in log output.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Direct delegation to SLF4J logger methods</li>
 *   <li>Support for LocationAwareLogger for accurate caller information</li>
 *   <li>Throws RuntimeException if SLF4J is not properly initialized</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * // Requires SLF4J API and an implementation (e.g., Logback) on classpath
 * Logger logger = new SLF4JLogger("com.example.MyClass");
 * logger.info("Using SLF4J with {}", backendName);
 * logger.error("Operation failed", exception);
 * }</pre>
 * 
 * @since 1.0
 */
class SLF4JLogger extends AbstractLogger {

    private static final String FQCN = SLF4JLogger.class.getName();

    private final org.slf4j.Logger loggerImpl;

    private final LocationAwareLogger locationAwareLogger;

    /**
     * Constructs a SLF4JLogger with the specified name.
     * 
     * <p>This constructor obtains a SLF4J logger instance from the SLF4J LoggerFactory.
     * If SLF4J is not properly initialized (i.e., using NOPLoggerFactory), a RuntimeException
     * is thrown.</p>
     * 
     * <p>If the obtained logger implements LocationAwareLogger, it will be used for
     * WARN and ERROR level logging to provide accurate caller location information.</p>
     *
     * @param name the name of the logger
     * @throws RuntimeException if SLF4J is not properly initialized
     */
    public SLF4JLogger(final String name) {
        super(name);
        if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
            throw new RuntimeException("Failed to initialize SLF4J Logger Factory");
        }

        loggerImpl = org.slf4j.LoggerFactory.getLogger(name);
        locationAwareLogger = loggerImpl instanceof LocationAwareLogger ? ((LocationAwareLogger) loggerImpl) : null;
    }

    /**
     * Checks if TRACE level logging is enabled.
     *
     * @return {@code true} if TRACE level logging is enabled, {@code false} otherwise
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isTraceEnabled();
    }

    /**
     * Logs a message at TRACE level.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void trace(final String msg) {
        loggerImpl.trace(msg);
    }

    /**
     * Logs a message at TRACE level with an exception.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        loggerImpl.trace(msg, t);
    }

    /**
     * Checks if DEBUG level logging is enabled.
     *
     * @return {@code true} if DEBUG level logging is enabled, {@code false} otherwise
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isDebugEnabled();
    }

    /**
     * Logs a message at DEBUG level.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void debug(final String msg) {
        loggerImpl.debug(msg);
    }

    /**
     * Logs a message at DEBUG level with an exception.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        loggerImpl.debug(msg, t);
    }

    /**
     * Checks if INFO level logging is enabled.
     *
     * @return {@code true} if INFO level logging is enabled, {@code false} otherwise
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isInfoEnabled();
    }

    /**
     * Logs a message at INFO level.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     */
    @Override
    public void info(final String msg) {
        loggerImpl.info(msg);
    }

    /**
     * Logs a message at INFO level with an exception.
     * 
     * <p>Delegates directly to the SLF4J logger.</p>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        loggerImpl.info(msg, t);
    }

    /**
     * Checks if WARN level logging is enabled.
     *
     * @return {@code true} if WARN level logging is enabled, {@code false} otherwise
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isWarnEnabled();
    }

    /**
     * Logs a message at WARN level.
     * 
     * <p>If the underlying logger supports location awareness (LocationAwareLogger),
     * it is used to provide accurate caller location information. Otherwise, delegates
     * directly to the standard SLF4J logger.</p>
     *
     * @param msg the message to log
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
     * Logs a message at WARN level with an exception.
     * 
     * <p>If the underlying logger supports location awareness (LocationAwareLogger),
     * it is used to provide accurate caller location information. Otherwise, delegates
     * directly to the standard SLF4J logger.</p>
     *
     * @param msg the message to log
     * @param t the exception to log
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
     * Checks if ERROR level logging is enabled.
     *
     * @return {@code true} if ERROR level logging is enabled, {@code false} otherwise
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isErrorEnabled();
    }

    /**
     * Logs a message at ERROR level.
     * 
     * <p>If the underlying logger supports location awareness (LocationAwareLogger),
     * it is used to provide accurate caller location information. Otherwise, delegates
     * directly to the standard SLF4J logger.</p>
     *
     * @param msg the message to log
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
     * Logs a message at ERROR level with an exception.
     * 
     * <p>If the underlying logger supports location awareness (LocationAwareLogger),
     * it is used to provide accurate caller location information. Otherwise, delegates
     * directly to the standard SLF4J logger.</p>
     *
     * @param msg the message to log
     * @param t the exception to log
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