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
 * @since 0.8
 * 
 * @author Haiyang Li
 */
class JDKLogger extends AbstractLogger {
    static final String SELF = JDKLogger.class.getName();
    static final String SUPER = AbstractLogger.class.getName();

    private final java.util.logging.Logger loggerImpl;

    public JDKLogger(String name) {
        super(name);
        loggerImpl = java.util.logging.Logger.getLogger(name);
    }

    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isLoggable(Level.FINEST);
    }

    @Override
    public void trace(String msg) {
        log(Level.FINEST, msg);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(Level.FINEST, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isLoggable(Level.FINE);
    }

    @Override
    public void debug(String msg) {
        log(Level.FINE, msg);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(Level.FINE, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isLoggable(Level.INFO);
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(Level.INFO, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(Level.WARNING, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isLoggable(Level.SEVERE);
    }

    @Override
    public void error(String msg) {
        log(Level.SEVERE, msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(Level.SEVERE, msg, t);
    }

    private void log(Level level, String msg) {
        log(SELF, level, msg, null);
    }

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
     * @param record
     *          The record to update
     */
    final private void fillCallerData(String callerFQCN, LogRecord record) {
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
