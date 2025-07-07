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
 * The main Logger interface providing a unified API for logging across different implementations.
 * 
 * <p>This interface is inspired by SLF4J but provides additional features such as:</p>
 * <ul>
 *   <li>Support for multiple parameter substitution (up to 7 parameters)</li>
 *   <li>Supplier-based lazy evaluation for expensive message construction</li>
 *   <li>Consistent method signatures across all log levels</li>
 *   <li>Both (Throwable, String) and (String, Throwable) parameter orders</li>
 * </ul>
 * 
 * <p>The interface supports five log levels in increasing order of severity:</p>
 * <ol>
 *   <li>TRACE - Most detailed information</li>
 *   <li>DEBUG - Detailed information for debugging</li>
 *   <li>INFO - General informational messages</li>
 *   <li>WARN - Warning messages for potentially harmful situations</li>
 *   <li>ERROR - Error messages for error events</li>
 * </ol>
 * 
 * <p>Typical usage pattern:</p>
 * <pre>{@code
 * import com.landawn.abacus.logging.Logger;
 * import com.landawn.abacus.logging.LoggerFactory;
 * 
 * public class Wombat {
 *   static final Logger logger = LoggerFactory.getLogger(Wombat.class);
 *   Integer t;
 *   Integer oldT;
 * 
 *   public void setTemperature(Integer temperature) {
 *     oldT = t;
 *     t = temperature;
 *     logger.debug("Temperature set to {}. Old temperature was {}.", t, oldT);
 *     if(temperature.intValue() > 50) {
 *       logger.info("Temperature has risen above 50 degrees.");
 *     }
 *   }
 * }
 * }</pre>
 * 
 * @author Ceki G&uuml;lc&uuml; (original SLF4J author)
 * @author HaiYang Li (adaptations for Abacus)
 * @since 1.0
 */
public interface Logger {

    /**
     * Case-insensitive String constant used to retrieve the name of the root logger.
     * 
     * <p>This constant can be used with LoggerFactory.getLogger(String) to obtain
     * the root logger instance.</p>
     */
    String ROOT_LOGGER_NAME = "ROOT";

    /**
     * Gets the name of this logger instance.
     * 
     * <p>Logger names typically follow Java package naming conventions.</p>
     *
     * @return the name of this logger
     */
    String getName();

    /**
     * Checks if the logger instance is enabled for the TRACE level.
     * 
     * <p>This method should be used to guard expensive trace message construction:</p>
     * <pre>{@code
     * if (logger.isTraceEnabled()) {
     *     logger.trace("Entry count: " + expensiveCalculation());
     * }
     * }</pre>
     *
     * @return {@code true} if this Logger is enabled for the TRACE level, {@code false} otherwise
     */
    boolean isTraceEnabled();

    /**
     * Logs a message at the TRACE level.
     * 
     * <p>TRACE level is typically used for very detailed information, finer than DEBUG.</p>
     *
     * @param msg the message string to be logged
     */
    void trace(String msg);

    /**
     * Logs a message at the TRACE level with one parameter.
     * 
     * <p>The message should contain a placeholder {} or %s for the argument:</p>
     * <pre>{@code
     * logger.trace("Entry number: {}", i);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void trace(String template, Object arg);

    /**
     * Logs a message at the TRACE level with two parameters.
     *
     * @param template the message template containing two placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void trace(String template, Object arg1, Object arg2);

    /**
     * Logs a message at the TRACE level with three parameters.
     *
     * @param template the message template containing three placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void trace(String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the TRACE level with four parameters.
     *
     * @param template the message template containing four placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     * Logs a message at the TRACE level with five parameters.
     *
     * @param template the message template containing five placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     * Logs a message at the TRACE level with six parameters.
     *
     * @param template the message template containing six placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     * Logs a message at the TRACE level with seven parameters.
     *
     * @param template the message template containing seven placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    void trace(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Logs a message at the TRACE level with variable number of arguments.
     * 
     * <p>This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level. However, the Supplier-based methods are preferred for
     * expensive message construction.</p>
     *
     * @param template the template string
     * @param args an array of arguments
     * @deprecated {@link #trace(Supplier)} is recommended for lazy evaluation
     */
    @Deprecated
    void trace(String template, Object... args);

    /**
     * Logs an exception at the TRACE level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    void trace(String msg, Throwable t);

    /**
     * Logs an exception at the TRACE level with an accompanying message.
     * 
     * <p>This method provides an alternative parameter order for consistency
     * with other APIs.</p>
     *
     * @param t the exception (throwable) to log
     * @param msg the message accompanying the exception
     */
    void trace(Throwable t, String msg);

    /**
     * Logs an exception at the TRACE level with a formatted message and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void trace(Throwable t, String template, Object arg);

    /**
     * Logs an exception at the TRACE level with a formatted message and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void trace(Throwable t, String template, Object arg1, Object arg2);

    /**
     * Logs an exception at the TRACE level with a formatted message and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the TRACE level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only invoked if TRACE level is enabled, making this
     * efficient for expensive message construction:</p>
     * <pre>{@code
     * logger.trace(() -> "Result: " + computeExpensiveResult());
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    void trace(Supplier<String> supplier);

    /**
     * Logs a message at the TRACE level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #trace(Throwable, Supplier)} for consistent parameter order
     */
    @Deprecated
    void trace(Supplier<String> supplier, Throwable t);

    /**
     * Logs a message at the TRACE level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    void trace(Throwable t, Supplier<String> supplier);

    /**
     * Checks if the logger instance is enabled for the DEBUG level.
     * 
     * <p>This method should be used to guard expensive debug message construction.</p>
     *
     * @return {@code true} if this Logger is enabled for the DEBUG level, {@code false} otherwise
     */
    boolean isDebugEnabled();

    /**
     * Logs a message at the DEBUG level.
     * 
     * <p>DEBUG level is typically used for information useful during development.</p>
     *
     * @param msg the message string to be logged
     */
    void debug(String msg);

    /**
     * Logs a message at the DEBUG level with one parameter.
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void debug(String template, Object arg);

    /**
     * Logs a message at the DEBUG level with two parameters.
     *
     * @param template the message template containing two placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void debug(String template, Object arg1, Object arg2);

    /**
     * Logs a message at the DEBUG level with three parameters.
     *
     * @param template the message template containing three placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void debug(String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the DEBUG level with four parameters.
     *
     * @param template the message template containing four placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     * Logs a message at the DEBUG level with five parameters.
     *
     * @param template the message template containing five placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     * Logs a message at the DEBUG level with six parameters.
     *
     * @param template the message template containing six placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     * Logs a message at the DEBUG level with seven parameters.
     *
     * @param template the message template containing seven placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    void debug(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Logs a message at the DEBUG level with variable number of arguments.
     * 
     * <p>This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.</p>
     *
     * @param template the template string
     * @param args an array of arguments
     * @deprecated {@link #debug(Supplier)} is recommended for lazy evaluation
     */
    @Deprecated
    void debug(String template, Object... args);

    /**
     * Logs an exception at the DEBUG level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    void debug(String msg, Throwable t);

    /**
     * Logs an exception at the DEBUG level with an accompanying message.
     * 
     * <p>This method provides an alternative parameter order for consistency
     * with other APIs.</p>
     *
     * @param t the exception (throwable) to log
     * @param msg the message accompanying the exception
     */
    void debug(Throwable t, String msg);

    /**
     * Logs an exception at the DEBUG level with a formatted message and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void debug(Throwable t, String template, Object arg);

    /**
     * Logs an exception at the DEBUG level with a formatted message and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void debug(Throwable t, String template, Object arg1, Object arg2);

    /**
     * Logs an exception at the DEBUG level with a formatted message and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the DEBUG level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only invoked if DEBUG level is enabled:</p>
     * <pre>{@code
     * logger.debug(() -> "Complex object state: " + complexObject.debugString());
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    void debug(Supplier<String> supplier);

    /**
     * Logs a message at the DEBUG level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #debug(Throwable, Supplier)} for consistent parameter order
     */
    @Deprecated
    void debug(Supplier<String> supplier, Throwable t);

    /**
     * Logs a message at the DEBUG level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    void debug(Throwable t, Supplier<String> supplier);

    /**
     * Checks if the logger instance is enabled for the INFO level.
     * 
     * <p>This method should be used to guard expensive info message construction.</p>
     *
     * @return {@code true} if this Logger is enabled for the INFO level, {@code false} otherwise
     */
    boolean isInfoEnabled();

    /**
     * Logs a message at the INFO level.
     * 
     * <p>INFO level is typically used for informational messages that highlight
     * the progress of the application.</p>
     *
     * @param msg the message string to be logged
     */
    void info(String msg);

    /**
     * Logs a message at the INFO level with one parameter.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * logger.info("Server started on port {}", port);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void info(String template, Object arg);

    /**
     * Logs a message at the INFO level with two parameters.
     *
     * @param template the message template containing two placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void info(String template, Object arg1, Object arg2);

    /**
     * Logs a message at the INFO level with three parameters.
     *
     * @param template the message template containing three placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void info(String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the INFO level with four parameters.
     *
     * @param template the message template containing four placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     * Logs a message at the INFO level with five parameters.
     *
     * @param template the message template containing five placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     * Logs a message at the INFO level with six parameters.
     *
     * @param template the message template containing six placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     * Logs a message at the INFO level with seven parameters.
     *
     * @param template the message template containing seven placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    void info(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Logs a message at the INFO level with variable number of arguments.
     * 
     * <p>This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.</p>
     *
     * @param template the template string
     * @param args an array of arguments
     * @deprecated {@link #info(Supplier)} is recommended for lazy evaluation
     */
    @Deprecated
    void info(String template, Object... args);

    /**
     * Logs an exception at the INFO level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    void info(String msg, Throwable t);

    /**
     * Logs an exception at the INFO level with an accompanying message.
     * 
     * <p>This method provides an alternative parameter order for consistency
     * with other APIs.</p>
     *
     * @param t the exception (throwable) to log
     * @param msg the message accompanying the exception
     */
    void info(Throwable t, String msg);

    /**
     * Logs an exception at the INFO level with a formatted message and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void info(Throwable t, String template, Object arg);

    /**
     * Logs an exception at the INFO level with a formatted message and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void info(Throwable t, String template, Object arg1, Object arg2);

    /**
     * Logs an exception at the INFO level with a formatted message and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void info(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the INFO level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only invoked if INFO level is enabled:</p>
     * <pre>{@code
     * logger.info(() -> "Processed " + getProcessedCount() + " records");
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    void info(Supplier<String> supplier);

    /**
     * Logs a message at the INFO level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #info(Throwable, Supplier)} for consistent parameter order
     */
    @Deprecated
    void info(Supplier<String> supplier, Throwable t);

    /**
     * Logs a message at the INFO level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    void info(Throwable t, Supplier<String> supplier);

    /**
     * Checks if the logger instance is enabled for the WARN level.
     * 
     * <p>This method should be used to guard expensive warn message construction.</p>
     *
     * @return {@code true} if this Logger is enabled for the WARN level, {@code false} otherwise
     */
    boolean isWarnEnabled();

    /**
     * Logs a message at the WARN level.
     * 
     * <p>WARN level is typically used for potentially harmful situations that
     * the application can recover from.</p>
     *
     * @param msg the message string to be logged
     */
    void warn(String msg);

    /**
     * Logs a message at the WARN level with one parameter.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * logger.warn("Cache size {} exceeds recommended maximum", cacheSize);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void warn(String template, Object arg);

    /**
     * Logs a message at the WARN level with two parameters.
     *
     * @param template the message template containing two placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void warn(String template, Object arg1, Object arg2);

    /**
     * Logs a message at the WARN level with three parameters.
     *
     * @param template the message template containing three placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void warn(String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the WARN level with four parameters.
     *
     * @param template the message template containing four placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     * Logs a message at the WARN level with five parameters.
     *
     * @param template the message template containing five placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     * Logs a message at the WARN level with six parameters.
     *
     * @param template the message template containing six placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     * Logs a message at the WARN level with seven parameters.
     *
     * @param template the message template containing seven placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    void warn(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Logs a message at the WARN level with variable number of arguments.
     * 
     * <p>This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.</p>
     *
     * @param template the template string
     * @param args an array of arguments
     * @deprecated {@link #warn(Supplier)} is recommended for lazy evaluation
     */
    @Deprecated
    void warn(String template, Object... args);

    /**
     * Logs an exception at the WARN level with an accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    void warn(String msg, Throwable t);

    /**
     * Logs an exception at the WARN level with an accompanying message.
     * 
     * <p>This method provides an alternative parameter order for consistency
     * with other APIs.</p>
     *
     * @param t the exception (throwable) to log
     * @param msg the message accompanying the exception
     */
    void warn(Throwable t, String msg);

    /**
     * Logs an exception at the WARN level with a formatted message and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void warn(Throwable t, String template, Object arg);

    /**
     * Logs an exception at the WARN level with a formatted message and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void warn(Throwable t, String template, Object arg1, Object arg2);

    /**
     * Logs an exception at the WARN level with a formatted message and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the WARN level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only invoked if WARN level is enabled:</p>
     * <pre>{@code
     * logger.warn(() -> "Memory usage " + getMemoryUsage() + " exceeds threshold");
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    void warn(Supplier<String> supplier);

    /**
     * Logs a message at the WARN level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #warn(Throwable, Supplier)} for consistent parameter order
     */
    @Deprecated
    void warn(Supplier<String> supplier, Throwable t);

    /**
     * Logs a message at the WARN level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    void warn(Throwable t, Supplier<String> supplier);

    /**
     * Checks if the logger instance is enabled for the ERROR level.
     * 
     * <p>This method should be used to guard expensive error message construction.</p>
     *
     * @return {@code true} if this Logger is enabled for the ERROR level, {@code false} otherwise
     */
    boolean isErrorEnabled();

    /**
     * Logs a message at the ERROR level.
     * 
     * <p>ERROR level is typically used for error events that might still allow
     * the application to continue running.</p>
     *
     * @param msg the message string to be logged
     */
    void error(String msg);

    /**
     * Logs a message at the ERROR level with one parameter.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * logger.error("Failed to process file: {}", filename);
     * }</pre>
     *
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void error(String template, Object arg);

    /**
     * Logs a message at the ERROR level with two parameters.
     *
     * @param template the message template containing two placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void error(String template, Object arg1, Object arg2);

    /**
     * Logs a message at the ERROR level with three parameters.
     *
     * @param template the message template containing three placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void error(String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the ERROR level with four parameters.
     *
     * @param template the message template containing four placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4);

    /**
     * Logs a message at the ERROR level with five parameters.
     *
     * @param template the message template containing five placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

    /**
     * Logs a message at the ERROR level with six parameters.
     *
     * @param template the message template containing six placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

    /**
     * Logs a message at the ERROR level with seven parameters.
     *
     * @param template the message template containing seven placeholders
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     * @param arg4 the fourth argument
     * @param arg5 the fifth argument
     * @param arg6 the sixth argument
     * @param arg7 the seventh argument
     */
    void error(String template, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

    /**
     * Logs a message at the ERROR level with variable number of arguments.
     * 
     * <p>This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.</p>
     *
     * @param template the template string
     * @param args an array of arguments
     * @deprecated {@link #error(Supplier)} is recommended for lazy evaluation
     */
    @Deprecated
    void error(String template, Object... args);

    /**
     * Logs an exception at the ERROR level with an accompanying message.
     * 
     * <p>This is commonly used for logging exceptions:</p>
     * <pre>{@code
     * try {
     *     // some operation
     * } catch (Exception e) {
     *     logger.error("Operation failed", e);
     * }
     * }</pre>
     *
     * @param msg the message accompanying the exception
     * @param t the exception (throwable) to log
     */
    void error(String msg, Throwable t);

    /**
     * Logs an exception at the ERROR level with an accompanying message.
     * 
     * <p>This method provides an alternative parameter order for consistency
     * with other APIs.</p>
     *
     * @param t the exception (throwable) to log
     * @param msg the message accompanying the exception
     */
    void error(Throwable t, String msg);

    /**
     * Logs an exception at the ERROR level with a formatted message and one parameter.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg the argument to be substituted in the template
     */
    void error(Throwable t, String template, Object arg);

    /**
     * Logs an exception at the ERROR level with a formatted message and two parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    void error(Throwable t, String template, Object arg1, Object arg2);

    /**
     * Logs an exception at the ERROR level with a formatted message and three parameters.
     *
     * @param t the exception to log
     * @param template the message template
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @param arg3 the third argument
     */
    void error(Throwable t, String template, Object arg1, Object arg2, Object arg3);

    /**
     * Logs a message at the ERROR level using a supplier for lazy evaluation.
     * 
     * <p>The supplier is only invoked if ERROR level is enabled:</p>
     * <pre>{@code
     * logger.error(() -> "Critical error in module: " + module.getDetailedStatus());
     * }</pre>
     *
     * @param supplier the supplier that provides the message
     */
    void error(Supplier<String> supplier);

    /**
     * Logs a message at the ERROR level with an exception using a supplier.
     * 
     * @param supplier the supplier that provides the message
     * @param t the exception to log
     * @deprecated Use {@link #error(Throwable, Supplier)} for consistent parameter order
     */
    @Deprecated
    void error(Supplier<String> supplier, Throwable t);

    /**
     * Logs a message at the ERROR level with an exception using a supplier for lazy evaluation.
     *
     * @param t the exception to log
     * @param supplier the supplier that provides the message
     */
    void error(Throwable t, Supplier<String> supplier);

}