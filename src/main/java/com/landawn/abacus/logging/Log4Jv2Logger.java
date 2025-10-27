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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * Logger implementation that delegates to Apache Log4j 2.
 * 
 * <p>This implementation provides a bridge to Log4j 2, leveraging its ExtendedLogger
 * interface for optimal performance and accurate caller location information. The logger
 * uses the logIfEnabled methods which provide efficient level checking.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Direct integration with Log4j 2's ExtendedLogger</li>
 *   <li>Accurate caller location information using FQCN</li>
 *   <li>Efficient level checking with logIfEnabled</li>
 *   <li>Full support for all logging levels</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Log4Jv2Logger is automatically used by LoggerFactory when Log4j 2 is on classpath
 * Logger logger = LoggerFactory.getLogger(MyClass.class);
 * int recordCount = 100;
 * logger.debug("Processing {} records", recordCount);
 * Exception sqlException = new Exception("Connection timeout");
 * logger.error("Database connection failed", sqlException);
 * }</pre>
 * 
 * @since 1.0
 */
class Log4Jv2Logger extends AbstractLogger {

    /**
     * Fully qualified class name used for accurate caller location detection.
     */
    public static final String FQCN = Log4Jv2Logger.class.getName();

    private final ExtendedLogger loggerImpl;

    /**
     * Constructs a Log4Jv2Logger with the specified name.
     *
     * <p>This constructor obtains a Log4j 2 logger instance from the LogManager
     * and casts it to ExtendedLogger for access to advanced features.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Typically instantiated by LoggerFactory, not directly
     * Log4Jv2Logger logger = new Log4Jv2Logger("com.example.MyClass");
     * logger.info("Log4j 2 logger initialized");
     * }</pre>
     *
     * @param name the name of the logger
     */
    public Log4Jv2Logger(final String name) {
        super(name);
        // if (!existsLog4JFile) {
        // throw new RuntimeException("Failed to initialize Log4j Logger Factory");
        // }
        //
        loggerImpl = (ExtendedLogger) org.apache.logging.log4j.LogManager.getLogger(name);
    }

    /**
     * Checks if TRACE level logging is enabled.
     *
     * <p>Delegates to Log4j 2's level checking mechanism.</p>
     *
     * @return {@code true} if TRACE level is enabled, {@code false} otherwise
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isEnabled(Level.TRACE);
    }

    /**
     * Logs a message at TRACE level.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.trace("Entering method processData");
     * }</pre>
     *
     * @param msg the message to log
     */
    @Override
    public void trace(final String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.TRACE, null, msg);
    }

    /**
     * Logs a message at TRACE level with an exception.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Exception e = new Exception("Trace error");
     * logger.trace("Detailed trace information", e);
     * }</pre>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    /**
     * Checks if DEBUG level logging is enabled.
     *
     * <p>Delegates to Log4j 2's level checking mechanism.</p>
     *
     * @return {@code true} if DEBUG level is enabled, {@code false} otherwise
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isEnabled(Level.DEBUG);
    }

    /**
     * Logs a message at DEBUG level.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.debug("Cache miss for key: userProfile");
     * }</pre>
     *
     * @param msg the message to log
     */
    @Override
    public void debug(final String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.DEBUG, null, msg);
    }

    /**
     * Logs a message at DEBUG level with an exception.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Exception e = new SQLException("Connection failed");
     * logger.debug("Database query attempt failed", e);
     * }</pre>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    /**
     * Checks if INFO level logging is enabled.
     *
     * <p>Delegates to Log4j 2's level checking mechanism.</p>
     *
     * @return {@code true} if INFO level is enabled, {@code false} otherwise
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isEnabled(Level.INFO);
    }

    /**
     * Logs a message at INFO level.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.info("Application started successfully");
     * }</pre>
     *
     * @param msg the message to log
     */
    @Override
    public void info(final String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.INFO, null, msg);
    }

    /**
     * Logs a message at INFO level with an exception.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Exception e = new ConfigurationException("Config file missing");
     * logger.info("Using default configuration", e);
     * }</pre>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    /**
     * Checks if WARN level logging is enabled.
     *
     * <p>Delegates to Log4j 2's level checking mechanism.</p>
     *
     * @return {@code true} if WARN level is enabled, {@code false} otherwise
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isEnabled(Level.WARN);
    }

    /**
     * Logs a message at WARN level.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.warn("Connection pool running low on connections");
     * }</pre>
     *
     * @param msg the message to log
     */
    @Override
    public void warn(final String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.WARN, null, msg);
    }

    /**
     * Logs a message at WARN level with an exception.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Exception e = new TimeoutException("Request timeout");
     * logger.warn("Request took longer than expected", e);
     * }</pre>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    /**
     * Checks if ERROR level logging is enabled.
     *
     * <p>Delegates to Log4j 2's level checking mechanism.</p>
     *
     * @return {@code true} if ERROR level is enabled, {@code false} otherwise
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isEnabled(Level.ERROR);
    }

    /**
     * Logs a message at ERROR level.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * logger.error("Failed to process transaction");
     * }</pre>
     *
     * @param msg the message to log
     */
    @Override
    public void error(final String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.ERROR, null, msg);
    }

    /**
     * Logs a message at ERROR level with an exception.
     *
     * <p>Uses logIfEnabled for efficient logging with proper caller location.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     processData();
     * } catch (Exception e) {
     *     logger.error("Critical error during data processing", e);
     * }
     * }</pre>
     *
     * @param msg the message to log
     * @param t the exception to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }
}
