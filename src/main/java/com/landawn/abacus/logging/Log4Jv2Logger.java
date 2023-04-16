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

import java.util.function.Supplier;

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

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void trace(String template, Object arg) {
        if (isTraceEnabled()) {
            trace(format(template, arg));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void trace(String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void trace(String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    @SafeVarargs
    public final void trace(String template, Object... args) {
        if (isTraceEnabled()) {
            trace(format(template, args));
        }
    }

    /**
     *
     * @param t
     * @param msg
     */
    @Override
    public void trace(Throwable t, String msg) {
        trace(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void trace(Throwable t, String template, Object arg) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void trace(Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(supplier.get());
        }
    }

    /**
     *
     * @param supplier
     * @param t
     */
    @Override
    public void trace(Supplier<String> supplier, Throwable t) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     *
     * @param t
     * @param supplier
     */
    @Override
    public void trace(Throwable t, Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void debug(String template, Object arg) {
        if (isDebugEnabled()) {
            debug(format(template, arg));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void debug(String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void debug(String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    @SafeVarargs
    public final void debug(String template, Object... args) {
        if (isDebugEnabled()) {
            debug(format(template, args));
        }
    }

    /**
     *
     * @param t
     * @param msg
     */
    @Override
    public void debug(Throwable t, String msg) {
        debug(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void debug(Throwable t, String template, Object arg) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void debug(Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(supplier.get());
        }
    }

    /**
     *
     * @param supplier
     * @param t
     */
    @Override
    public void debug(Supplier<String> supplier, Throwable t) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     *
     * @param t
     * @param supplier
     */
    @Override
    public void debug(Throwable t, Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void info(String template, Object arg) {
        if (isInfoEnabled()) {
            info(format(template, arg));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void info(String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void info(String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    @SafeVarargs
    public final void info(String template, Object... args) {
        if (isInfoEnabled()) {
            info(format(template, args));
        }
    }

    /**
     *
     * @param t
     * @param msg
     */
    @Override
    public void info(Throwable t, String msg) {
        info(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void info(Throwable t, String template, Object arg) {
        if (isInfoEnabled()) {
            info(t, format(template, arg));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void info(Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(supplier.get());
        }
    }

    /**
     *
     * @param supplier
     * @param t
     */
    @Override
    public void info(Supplier<String> supplier, Throwable t) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     *
     * @param t
     * @param supplier
     */
    @Override
    public void info(Throwable t, Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void warn(String template, Object arg) {
        if (isWarnEnabled()) {
            warn(format(template, arg));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void warn(String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void warn(String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    @SafeVarargs
    public final void warn(String template, Object... args) {
        if (isWarnEnabled()) {
            warn(format(template, args));
        }
    }

    /**
     *
     * @param t
     * @param msg
     */
    @Override
    public void warn(Throwable t, String msg) {
        warn(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void warn(Throwable t, String template, Object arg) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void warn(Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(supplier.get());
        }
    }

    /**
     *
     * @param supplier
     * @param t
     */
    @Override
    public void warn(Supplier<String> supplier, Throwable t) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     *
     * @param t
     * @param supplier
     */
    @Override
    public void warn(Throwable t, Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void error(String template, Object arg) {
        if (isErrorEnabled()) {
            error(format(template, arg));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void error(String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void error(String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    @SafeVarargs
    public final void error(String template, Object... args) {
        if (isErrorEnabled()) {
            error(format(template, args));
        }
    }

    /**
     *
     * @param t
     * @param msg
     */
    @Override
    public void error(Throwable t, String msg) {
        error(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void error(Throwable t, String template, Object arg) {
        if (isErrorEnabled()) {
            error(t, format(template, arg));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2));
        }
    }

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void error(Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(supplier.get());
        }
    }

    /**
     *
     * @param supplier
     * @param t
     */
    @Override
    public void error(Supplier<String> supplier, Throwable t) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    /**
     *
     * @param t
     * @param supplier
     */
    @Override
    public void error(Throwable t, Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }
}
