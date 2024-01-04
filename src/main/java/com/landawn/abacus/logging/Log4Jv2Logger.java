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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
class Log4Jv2Logger extends AbstractLogger {

    public static final String FQCN = Log4Jv2Logger.class.getName();

    // private static final String LOG4J_XML = "log4j.xml";
    // private static final boolean existsLog4JFile;
    //
    // static {
    // boolean temp = false;
    //
    // try {
    // URL url = Loader.getResource(LOG4J_XML);
    // temp = (url != null) && (url.getFile() != null);
    // } catch (Exception e) {
    // // ignore
    // }
    //
    // existsLog4JFile = temp;
    // }
    //
    private final ExtendedLogger loggerImpl;

    /**
     *
     *
     * @param name
     */
    public Log4Jv2Logger(String name) {
        super(name);
        // if (!existsLog4JFile) {
        // throw new RuntimeException("Failed to initilze Log4j Logger Factory");
        // }
        //
        loggerImpl = (ExtendedLogger) org.apache.logging.log4j.LogManager.getLogger(name);
    }

    /**
     * Checks if is trace enabled.
     *
     * @return true, if is trace enabled
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isEnabled(Level.TRACE);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void trace(String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.TRACE, null, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void trace(String msg, Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return true, if is debug enabled
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isEnabled(Level.DEBUG);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void debug(String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.DEBUG, null, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void debug(String msg, Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return true, if is info enabled
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isEnabled(Level.INFO);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void info(String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.INFO, null, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void info(String msg, Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return true, if is warn enabled
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isEnabled(Level.WARN);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void warn(String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.WARN, null, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void warn(String msg, Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    /**
     * Checks if is error enabled.
     *
     * @return true, if is error enabled
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isEnabled(Level.ERROR);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void error(String msg) {
        loggerImpl.logIfEnabled(FQCN, Level.ERROR, null, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void error(String msg, Throwable t) {
        loggerImpl.logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }
}
