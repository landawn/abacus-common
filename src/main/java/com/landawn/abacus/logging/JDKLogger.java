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

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 */
class JDKLogger extends AbstractLogger {

    static final String SELF = JDKLogger.class.getName();

    static final String SUPER = AbstractLogger.class.getName();

    private final java.util.logging.Logger loggerImpl;

    /**
     *
     *
     * @param name
     */
    public JDKLogger(final String name) {
        super(name);
        loggerImpl = java.util.logging.Logger.getLogger(name);
    }

    /**
     * Checks if is trace enabled.
     *
     * @return {@code true}, if is trace enabled
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isLoggable(Level.FINEST);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void trace(final String msg) {
        log(Level.FINEST, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        log(Level.FINEST, msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return {@code true}, if is debug enabled
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isLoggable(Level.FINE);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void debug(final String msg) {
        log(Level.FINE, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        log(Level.FINE, msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return {@code true}, if is info enabled
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isLoggable(Level.INFO);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void info(final String msg) {
        log(Level.INFO, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void info(final String msg, final Throwable t) {
        log(Level.INFO, msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return {@code true}, if is warn enabled
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isLoggable(Level.WARNING);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void warn(final String msg) {
        log(Level.WARNING, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        log(Level.WARNING, msg, t);
    }

    /**
     * Checks if is error enabled.
     *
     * @return {@code true}, if is error enabled
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isLoggable(Level.SEVERE);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void error(final String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void error(final String msg, final Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    /**
     *
     * @param level
     * @param msg
     */
    private void log(final Level level, final String msg) {
        log(SELF, level, msg, null);
    }

    /**
     *
     * @param level
     * @param msg
     * @param t
     */
    private void log(final Level level, final String msg, final Throwable t) {
        log(SELF, level, msg, t);
    }

    /**
     * Log the message at the specified level with the specified throwable if any.
     * This method creates a LogRecord and fills in caller date before calling
     * this instance's JDK14 logger.
     *
     * See bug report #13 for more details.
     *
     * @param callerFQCN
     * @param level
     * @param msg
     * @param t
     */
    private void log(final String callerFQCN, final Level level, final String msg, final Throwable t) {
        // millis and thread are filled by the constructor
        final LogRecord logRecord = new LogRecord(level, msg);
        logRecord.setLoggerName(getName());
        logRecord.setThrown(t);
        // Note: parameters in record are not set because SLF4J only
        // supports a single formatting style
        fillCallerData(callerFQCN, logRecord);
        loggerImpl.log(logRecord);
    }

    /**
     * Fill in caller data if possible.
     *
     * @param callerFQCN
     * @param logRecord The record to update
     */
    private static void fillCallerData(final String callerFQCN, final LogRecord logRecord) {
        final StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (className.equals(callerFQCN) || className.equals(SUPER)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                found = i;
                break;
            }
        }

        if (found != -1) {
            final StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            logRecord.setSourceClassName(ste.getClassName());
            logRecord.setSourceMethodName(ste.getMethodName());
        }
    }
}
