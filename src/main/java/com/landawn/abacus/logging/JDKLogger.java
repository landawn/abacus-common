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
 * @author Haiyang Li
 * @since 0.8
 */
class JDKLogger extends AbstractLogger {

    static final String SELF = JDKLogger.class.getName();

    static final String SUPER = AbstractLogger.class.getName();

    private final java.util.logging.Logger loggerImpl;

    public JDKLogger(String name) {
        super(name);
        loggerImpl = java.util.logging.Logger.getLogger(name);
    }

    /**
     * Checks if is trace enabled.
     *
     * @return true, if is trace enabled
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
    public void trace(String msg) {
        log(Level.FINEST, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void trace(String msg, Throwable t) {
        log(Level.FINEST, msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return true, if is debug enabled
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
    public void debug(String msg) {
        log(Level.FINE, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void debug(String msg, Throwable t) {
        log(Level.FINE, msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return true, if is info enabled
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
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return true, if is warn enabled
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
    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARNING, msg, t);
    }

    /**
     * Checks if is error enabled.
     *
     * @return true, if is error enabled
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
    public void error(String msg) {
        log(Level.SEVERE, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void error(String msg, Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    /**
     *
     * @param level
     * @param msg
     */
    private void log(Level level, String msg) {
        log(SELF, level, msg, null);
    }

    /**
     *
     * @param level
     * @param msg
     * @param t
     */
    private void log(Level level, String msg, Throwable t) {
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
    private void log(String callerFQCN, Level level, String msg, Throwable t) {
        // millis and thread are filled by the constructor
        LogRecord record = new LogRecord(level, msg);
        record.setLoggerName(getName());
        record.setThrown(t);
        // Note: parameters in record are not set because SLF4J only
        // supports a single formatting style
        fillCallerData(callerFQCN, record);
        loggerImpl.log(record);
    }

    /**
     * Fill in caller data if possible.
     *
     * @param callerFQCN
     * @param record The record to update
     */
    private static void fillCallerData(String callerFQCN, LogRecord record) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

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
            StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }
}
