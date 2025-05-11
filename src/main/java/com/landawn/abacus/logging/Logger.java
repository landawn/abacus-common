/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.landawn.abacus.logging;

import java.util.function.Supplier;

/**
 * The org.slf4j.Logger interface is the main user entry point of SLF4J API. It is expected that logging takes place
 * through concrete implementations of this interface.
 *
 * <h3>Typical usage pattern:</h3>
 *
 * <pre>
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 *
 * public class Wombat {
 *
 *   <span style="color:green">static final Logger logger = LoggerFactory.getLogger(Wombat.class);</span>
 *   Integer t;
 *   Integer oldT;
 *
 *   public void setTemperature(Integer temperature) {
 *     oldT = t;
 *     t = temperature;
 *     <span style="color:green">logger.debug("Temperature set to {}. Old temperature was {}.", t, oldT);</span>
 *     if(temperature.intValue() > 50) {
 *       <span style="color:green">logger.info("Temperature has risen above 50 degrees.");</span>
 *     }
 *   }
 * }
 * </pre>
 *
 *
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public interface Logger {

    /**
     * Case-insensitive String constant used to retrieve the name of the root logger.
     *
     */
    String ROOT_LOGGER_NAME = "ROOT";

    /**
     * Gets the name.
     *
     * @return
     */
    String getName();

    /**
     * Is the logger instance enabled for the TRACE level?.
     *
     * @return True if this Logger is enabled for the TRACE level, {@code false} otherwise.
     */
    boolean isTraceEnabled();

    /**
     * Log a message at the TRACE level.
     *
     * @param msg
     *            the message string to be logged
     */
    void trace(String msg);

    /**
     *
     * @param template
     * @param arg
     */
    void trace(String template, Object arg);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    void trace(String template, Object arg1, Object arg2);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void trace(String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @param arg7
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Log a message at the TRACE level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the TRACE level.
     * </p>
     *
     * @param template
     *            the template string
     * @param args
     *            an array of arguments
     *
     * @deprecated {@link #trace(Supplier)} is recommended
     */
    @Deprecated
    void trace(String template, Object... args);

    /**
     * Log an exception (throwable) at the TRACE level with an accompanying message.
     *
     * @param msg
     *            the message accompanying the exception
     * @param t
     *            the exception (throwable) to log
     *
     */
    void trace(String msg, Throwable t);

    /**
     *
     * @param t
     * @param msg
     */
    void trace(Throwable t, String msg);

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    void trace(Throwable t, String template, Object arg);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    void trace(Throwable t, String template, Object arg1, Object arg2);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param supplier
     */
    void trace(Supplier<String> supplier);

    /**
     *
     * @param supplier
     * @param t
     * @deprecated replaced by {@link #trace(Throwable, Supplier)}
     */
    @Deprecated
    void trace(Supplier<String> supplier, Throwable t);

    /**
     *
     * @param t
     * @param supplier
     */
    void trace(Throwable t, Supplier<String> supplier);

    /**
     * Is the logger instance enabled for the DEBUG level?.
     *
     * @return True if this Logger is enabled for the DEBUG level, {@code false} otherwise.
     */
    boolean isDebugEnabled();

    /**
     * Log a message at the DEBUG level.
     *
     * @param msg
     *            the message string to be logged
     */
    void debug(String msg);

    /**
     *
     * @param template
     * @param arg
     */
    void debug(String template, Object arg);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    void debug(String template, Object arg1, Object arg2);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void debug(String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @param arg7
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Log a message at the DEBUG level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the level.
     * </p>
     *
     * @param template
     *            the template string
     * @param args
     *            an array of arguments
     *
     * @deprecated {@link #debug(Supplier)} is recommended
     */
    @Deprecated
    void debug(String template, Object... args);

    /**
     * Log an exception (throwable) at the DEBUG level with an accompanying message.
     *
     * @param msg
     *            the message accompanying the exception
     * @param t
     *            the exception (throwable) to log
     *
     */
    void debug(String msg, Throwable t);

    /**
     *
     * @param t
     * @param msg
     */
    void debug(Throwable t, String msg);

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    void debug(Throwable t, String template, Object arg);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    void debug(Throwable t, String template, Object arg1, Object arg2);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param supplier
     */
    void debug(Supplier<String> supplier);

    /**
     *
     * @param supplier
     * @param t
     * @deprecated replaced by {@link #debug(Throwable, Supplier)}
     */
    @Deprecated
    void debug(Supplier<String> supplier, Throwable t);

    /**
     *
     * @param t
     * @param supplier
     */
    void debug(Throwable t, Supplier<String> supplier);

    /**
     * Is the logger instance enabled for the INFO level?.
     *
     * @return True if this Logger is enabled for the INFO level, {@code false} otherwise.
     */
    boolean isInfoEnabled();

    /**
     * Log a message at the INFO level.
     *
     * @param msg
     *            the message string to be logged
     */
    void info(String msg);

    /**
     *
     * @param template
     * @param arg
     */
    void info(String template, Object arg);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    void info(String template, Object arg1, Object arg2);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void info(String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @param arg7
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Log a message at the INFO level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the INFO level.
     * </p>
     *
     * @param template
     *            the template string
     * @param args
     *            an array of arguments
     *
     * @deprecated {@link #info(Supplier)} is recommended
     */
    @Deprecated
    void info(String template, Object... args);

    /**
     * Log an exception (throwable) at the INFO level with an accompanying message.
     *
     * @param msg
     *            the message accompanying the exception
     * @param t
     *            the exception (throwable) to log
     *
     */
    void info(String msg, Throwable t);

    /**
     *
     * @param t
     * @param msg
     */
    void info(Throwable t, String msg);

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    void info(Throwable t, String template, Object arg);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    void info(Throwable t, String template, Object arg1, Object arg2);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void info(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param supplier
     */
    void info(Supplier<String> supplier);

    /**
     *
     * @param supplier
     * @param t
     * @deprecated replaced by {@link #info(Throwable, Supplier)}
     */
    @Deprecated
    void info(Supplier<String> supplier, Throwable t);

    /**
     *
     * @param t
     * @param supplier
     */
    void info(Throwable t, Supplier<String> supplier);

    /**
     * Is the logger instance enabled for the WARN level?.
     *
     * @return True if this Logger is enabled for the WARN level, {@code false} otherwise.
     */
    boolean isWarnEnabled();

    /**
     * Log a message at the WARNING level.
     *
     * @param msg
     *            the message string to be logged
     */
    void warn(String msg);

    /**
     *
     * @param template
     * @param arg
     */
    void warn(String template, Object arg);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    void warn(String template, Object arg1, Object arg2);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void warn(String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @param arg7
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Log a message at the WARNING level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the WARNING level.
     * </p>
     *
     * @param template
     *            the template string
     * @param args
     *            an array of arguments
     *
     * @deprecated {@link #warn(Supplier)} is recommended
     */
    @Deprecated
    void warn(String template, Object... args);

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying message.
     *
     * @param msg
     *            the message accompanying the exception
     * @param t
     *            the exception (throwable) to log
     *
     */
    void warn(String msg, Throwable t);

    /**
     *
     * @param t
     * @param msg
     */
    void warn(Throwable t, String msg);

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    void warn(Throwable t, String template, Object arg);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    void warn(Throwable t, String template, Object arg1, Object arg2);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param supplier
     */
    void warn(Supplier<String> supplier);

    /**
     *
     * @param supplier
     * @param t
     * @deprecated replaced by {@link #warn(Throwable, Supplier)}
     */
    @Deprecated
    void warn(Supplier<String> supplier, Throwable t);

    /**
     *
     * @param t
     * @param supplier
     */
    void warn(Throwable t, Supplier<String> supplier);

    /**
     * Is the logger instance enabled for the ERROR level?.
     *
     * @return True if this Logger is enabled for the ERROR level, {@code false} otherwise.
     */
    boolean isErrorEnabled();

    /**
     * Log a message at the ERROR level.
     *
     * @param msg
     *            the message string to be logged
     */
    void error(String msg);

    /**
     *
     * @param template
     * @param arg
     */
    void error(String template, Object arg);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     */
    void error(String template, Object arg1, Object arg2);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void error(String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @param arg7
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Log a message at the ERROR level according to the specified format and arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
     * </p>
     *
     * @param template
     *            the template string
     * @param args
     *            an array of arguments
     *
     * @deprecated {@link #error(Supplier)} is recommended
     */
    @Deprecated
    void error(String template, Object... args);

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg
     *            the message accompanying the exception
     * @param t
     *            the exception (throwable) to log
     *
     */
    void error(String msg, Throwable t);

    /**
     *
     * @param t
     * @param msg
     */
    void error(Throwable t, String msg);

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    void error(Throwable t, String template, Object arg);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     */
    void error(Throwable t, String template, Object arg1, Object arg2);

    /**
     *
     * @param t
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     */
    void error(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     *
     * @param supplier
     */
    void error(Supplier<String> supplier);

    /**
     *
     * @param supplier
     * @param t
     * @deprecated replaced by {@link #error(Throwable, Supplier)}
     */
    @Deprecated
    void error(Supplier<String> supplier, Throwable t);

    /**
     *
     * @param t
     * @param supplier
     */
    void error(Throwable t, Supplier<String> supplier);

}
