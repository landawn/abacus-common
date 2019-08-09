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

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class SLF4JLogger.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class SLF4JLogger extends AbstractLogger {

    /** The Constant FQCN. */
    private static final String FQCN = SLF4JLogger.class.getName();

    /** The logger impl. */
    private final org.slf4j.Logger loggerImpl;

    /** The location aware logger. */
    private final LocationAwareLogger locationAwareLogger;

    /**
     * Instantiates a new SLF 4 J logger.
     *
     * @param name the name
     */
    public SLF4JLogger(String name) {
        super(name);
        if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
            throw new AbacusException("Failed to initilze SLF4J Logger Factory");
        }

        loggerImpl = org.slf4j.LoggerFactory.getLogger(name);
        locationAwareLogger = loggerImpl instanceof LocationAwareLogger ? ((LocationAwareLogger) loggerImpl) : null;
    }

    /**
     * Checks if is trace enabled.
     *
     * @return true, if is trace enabled
     */
    @Override
    public boolean isTraceEnabled() {
        return loggerImpl.isTraceEnabled();
    }

    /**
     * Trace.
     *
     * @param msg the msg
     */
    @Override
    public void trace(String msg) {
        loggerImpl.trace(msg);
    }

    /**
     * Trace.
     *
     * @param msg the msg
     * @param t the t
     */
    @Override
    public void trace(String msg, Throwable t) {
        loggerImpl.trace(msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return true, if is debug enabled
     */
    @Override
    public boolean isDebugEnabled() {
        return loggerImpl.isDebugEnabled();
    }

    /**
     * Debug.
     *
     * @param msg the msg
     */
    @Override
    public void debug(String msg) {
        loggerImpl.debug(msg);
    }

    /**
     * Debug.
     *
     * @param msg the msg
     * @param t the t
     */
    @Override
    public void debug(String msg, Throwable t) {
        loggerImpl.debug(msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return true, if is info enabled
     */
    @Override
    public boolean isInfoEnabled() {
        return loggerImpl.isInfoEnabled();
    }

    /**
     * Info.
     *
     * @param msg the msg
     */
    @Override
    public void info(String msg) {
        loggerImpl.info(msg);
    }

    /**
     * Info.
     *
     * @param msg the msg
     * @param t the t
     */
    @Override
    public void info(String msg, Throwable t) {
        loggerImpl.info(msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return true, if is warn enabled
     */
    @Override
    public boolean isWarnEnabled() {
        return loggerImpl.isWarnEnabled();
    }

    /**
     * Warn.
     *
     * @param msg the msg
     */
    @Override
    public void warn(String msg) {
        if (locationAwareLogger == null) {
            loggerImpl.warn(msg);
        } else {
            locationAwareLogger.log(null, FQCN, WARN_INT, msg, null, null);
        }
    }

    /**
     * Warn.
     *
     * @param msg the msg
     * @param t the t
     */
    @Override
    public void warn(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            loggerImpl.warn(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, WARN_INT, msg, null, t);
        }
    }

    /**
     * Checks if is error enabled.
     *
     * @return true, if is error enabled
     */
    @Override
    public boolean isErrorEnabled() {
        return loggerImpl.isErrorEnabled();
    }

    /**
     * Error.
     *
     * @param msg the msg
     */
    @Override
    public void error(String msg) {
        if (locationAwareLogger == null) {
            loggerImpl.error(msg);
        } else {
            locationAwareLogger.log(null, FQCN, ERROR_INT, msg, null, null);
        }
    }

    /**
     * Error.
     *
     * @param msg the msg
     * @param t the t
     */
    @Override
    public void error(String msg, Throwable t) {
        if (locationAwareLogger == null) {
            loggerImpl.error(msg, t);
        } else {
            locationAwareLogger.log(null, FQCN, ERROR_INT, msg, null, t);
        }
    }

    /**
     * Trace.
     *
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void trace(String template, Object arg) {
        if (isTraceEnabled()) {
            trace(format(template, arg));
        }
    }

    /**
     * Trace.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void trace(String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2));
        }
    }

    /**
     * Trace.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void trace(String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Trace.
     *
     * @param template the template
     * @param args the args
     */
    @Override
    @SafeVarargs
    public final void trace(String template, Object... args) {
        if (isTraceEnabled()) {
            trace(format(template, args));
        }
    }

    /**
     * Trace.
     *
     * @param t the t
     * @param msg the msg
     */
    @Override
    public void trace(Throwable t, String msg) {
        trace(msg, t);
    }

    /**
     * Trace.
     *
     * @param t the t
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void trace(Throwable t, String template, Object arg) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg));
        }
    }

    /**
     * Trace.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2));
        }
    }

    /**
     * Trace.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Trace.
     *
     * @param supplier the supplier
     */
    @Override
    public void trace(Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(supplier.get());
        }
    }

    /**
     * Trace.
     *
     * @param supplier the supplier
     * @param t the t
     */
    @Override
    public void trace(Supplier<String> supplier, Throwable t) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     * Trace.
     *
     * @param t the t
     * @param supplier the supplier
     */
    @Override
    public void trace(Throwable t, Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    /**
     * Debug.
     *
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void debug(String template, Object arg) {
        if (isDebugEnabled()) {
            debug(format(template, arg));
        }
    }

    /**
     * Debug.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void debug(String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2));
        }
    }

    /**
     * Debug.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void debug(String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Debug.
     *
     * @param template the template
     * @param args the args
     */
    @Override
    @SafeVarargs
    public final void debug(String template, Object... args) {
        if (isDebugEnabled()) {
            debug(format(template, args));
        }
    }

    /**
     * Debug.
     *
     * @param t the t
     * @param msg the msg
     */
    @Override
    public void debug(Throwable t, String msg) {
        debug(msg, t);
    }

    /**
     * Debug.
     *
     * @param t the t
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void debug(Throwable t, String template, Object arg) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg));
        }
    }

    /**
     * Debug.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2));
        }
    }

    /**
     * Debug.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Debug.
     *
     * @param supplier the supplier
     */
    @Override
    public void debug(Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(supplier.get());
        }
    }

    /**
     * Debug.
     *
     * @param supplier the supplier
     * @param t the t
     */
    @Override
    public void debug(Supplier<String> supplier, Throwable t) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     * Debug.
     *
     * @param t the t
     * @param supplier the supplier
     */
    @Override
    public void debug(Throwable t, Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    /**
     * Info.
     *
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void info(String template, Object arg) {
        if (isInfoEnabled()) {
            info(format(template, arg));
        }
    }

    /**
     * Info.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void info(String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2));
        }
    }

    /**
     * Info.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void info(String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Info.
     *
     * @param template the template
     * @param args the args
     */
    @Override
    @SafeVarargs
    public final void info(String template, Object... args) {
        if (isInfoEnabled()) {
            info(format(template, args));
        }
    }

    /**
     * Info.
     *
     * @param t the t
     * @param msg the msg
     */
    @Override
    public void info(Throwable t, String msg) {
        info(msg, t);
    }

    /**
     * Info.
     *
     * @param t the t
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void info(Throwable t, String template, Object arg) {
        if (isInfoEnabled()) {
            info(t, format(template, arg));
        }
    }

    /**
     * Info.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2));
        }
    }

    /**
     * Info.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Info.
     *
     * @param supplier the supplier
     */
    @Override
    public void info(Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(supplier.get());
        }
    }

    /**
     * Info.
     *
     * @param supplier the supplier
     * @param t the t
     */
    @Override
    public void info(Supplier<String> supplier, Throwable t) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     * Info.
     *
     * @param t the t
     * @param supplier the supplier
     */
    @Override
    public void info(Throwable t, Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    /**
     * Warn.
     *
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void warn(String template, Object arg) {
        if (isWarnEnabled()) {
            warn(format(template, arg));
        }
    }

    /**
     * Warn.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void warn(String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2));
        }
    }

    /**
     * Warn.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void warn(String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Warn.
     *
     * @param template the template
     * @param args the args
     */
    @Override
    @SafeVarargs
    public final void warn(String template, Object... args) {
        if (isWarnEnabled()) {
            warn(format(template, args));
        }
    }

    /**
     * Warn.
     *
     * @param t the t
     * @param msg the msg
     */
    @Override
    public void warn(Throwable t, String msg) {
        warn(msg, t);
    }

    /**
     * Warn.
     *
     * @param t the t
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void warn(Throwable t, String template, Object arg) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg));
        }
    }

    /**
     * Warn.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2));
        }
    }

    /**
     * Warn.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Warn.
     *
     * @param supplier the supplier
     */
    @Override
    public void warn(Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(supplier.get());
        }
    }

    /**
     * Warn.
     *
     * @param supplier the supplier
     * @param t the t
     */
    @Override
    public void warn(Supplier<String> supplier, Throwable t) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     * Warn.
     *
     * @param t the t
     * @param supplier the supplier
     */
    @Override
    public void warn(Throwable t, Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    /**
     * Error.
     *
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void error(String template, Object arg) {
        if (isErrorEnabled()) {
            error(format(template, arg));
        }
    }

    /**
     * Error.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void error(String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2));
        }
    }

    /**
     * Error.
     *
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void error(String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Error.
     *
     * @param template the template
     * @param args the args
     */
    @Override
    @SafeVarargs
    public final void error(String template, Object... args) {
        if (isErrorEnabled()) {
            error(format(template, args));
        }
    }

    /**
     * Error.
     *
     * @param t the t
     * @param msg the msg
     */
    @Override
    public void error(Throwable t, String msg) {
        error(msg, t);
    }

    /**
     * Error.
     *
     * @param t the t
     * @param template the template
     * @param arg the arg
     */
    @Override
    public void error(Throwable t, String template, Object arg) {
        if (isErrorEnabled()) {
            error(t, format(template, arg));
        }
    }

    /**
     * Error.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     */
    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2));
        }
    }

    /**
     * Error.
     *
     * @param t the t
     * @param template the template
     * @param arg1 the arg 1
     * @param arg2 the arg 2
     * @param arg3 the arg 3
     */
    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     * Error.
     *
     * @param supplier the supplier
     */
    @Override
    public void error(Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(supplier.get());
        }
    }

    /**
     * Error.
     *
     * @param supplier the supplier
     * @param t the t
     */
    @Override
    public void error(Supplier<String> supplier, Throwable t) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    /**
     * Error.
     *
     * @param t the t
     * @param supplier the supplier
     */
    @Override
    public void error(Throwable t, Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }
}
