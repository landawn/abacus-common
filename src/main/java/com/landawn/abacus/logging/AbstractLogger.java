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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractLogger implements Logger {

    protected final String name;

    protected AbstractLogger(final String name) {
        this.name = name;
    }

    /**
     * Gets the name.
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     *
     * @param template
     * @param arg
     */
    @Override
    public void trace(final String template, final Object arg) {
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
    public void trace(final String template, final Object arg1, final Object arg2) {
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
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

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
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

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
    @Override
    public void trace(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    public void trace(final String template, final Object... args) {
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
    public void trace(final Throwable t, final String msg) {
        trace(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void trace(final Throwable t, final String template, final Object arg) {
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
    public void trace(final Throwable t, final String template, final Object arg1, final Object arg2) {
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
    public void trace(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void trace(final Supplier<String> supplier) {
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
    public void trace(final Supplier<String> supplier, final Throwable t) {
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
    public void trace(final Throwable t, final Supplier<String> supplier) {
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
    public void debug(final String template, final Object arg) {
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
    public void debug(final String template, final Object arg1, final Object arg2) {
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
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

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
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

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
    @Override
    public void debug(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    public void debug(final String template, final Object... args) {
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
    public void debug(final Throwable t, final String msg) {
        debug(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void debug(final Throwable t, final String template, final Object arg) {
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
    public void debug(final Throwable t, final String template, final Object arg1, final Object arg2) {
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
    public void debug(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void debug(final Supplier<String> supplier) {
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
    public void debug(final Supplier<String> supplier, final Throwable t) {
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
    public void debug(final Throwable t, final Supplier<String> supplier) {
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
    public void info(final String template, final Object arg) {
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
    public void info(final String template, final Object arg1, final Object arg2) {
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
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

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
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

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
    @Override
    public void info(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    public void info(final String template, final Object... args) {
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
    public void info(final Throwable t, final String msg) {
        info(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void info(final Throwable t, final String template, final Object arg) {
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
    public void info(final Throwable t, final String template, final Object arg1, final Object arg2) {
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
    public void info(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void info(final Supplier<String> supplier) {
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
    public void info(final Supplier<String> supplier, final Throwable t) {
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
    public void info(final Throwable t, final Supplier<String> supplier) {
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
    public void warn(final String template, final Object arg) {
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
    public void warn(final String template, final Object arg1, final Object arg2) {
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
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

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
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

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
    @Override
    public void warn(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    public void warn(final String template, final Object... args) {
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
    public void warn(final Throwable t, final String msg) {
        warn(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void warn(final Throwable t, final String template, final Object arg) {
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
    public void warn(final Throwable t, final String template, final Object arg1, final Object arg2) {
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
    public void warn(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void warn(final Supplier<String> supplier) {
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
    public void warn(final Supplier<String> supplier, final Throwable t) {
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
    public void warn(final Throwable t, final Supplier<String> supplier) {
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
    public void error(final String template, final Object arg) {
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
    public void error(final String template, final Object arg1, final Object arg2) {
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
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4));
        }
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     */
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5));
        }
    }

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
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5, arg6));
        }
    }

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
    @Override
    public void error(final String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3, arg4, arg5, arg6, arg7));
        }
    }

    /**
     *
     * @param template
     * @param args
     */
    @Override
    public void error(final String template, final Object... args) {
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
    public void error(final Throwable t, final String msg) {
        error(msg, t);
    }

    /**
     *
     * @param t
     * @param template
     * @param arg
     */
    @Override
    public void error(final Throwable t, final String template, final Object arg) {
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
    public void error(final Throwable t, final String template, final Object arg1, final Object arg2) {
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
    public void error(final Throwable t, final String template, final Object arg1, final Object arg2, final Object arg3) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2, arg3));
        }
    }

    /**
     *
     * @param supplier
     */
    @Override
    public void error(final Supplier<String> supplier) {
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
    public void error(final Supplier<String> supplier, final Throwable t) {
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
    public void error(final Throwable t, final Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     * @return
     */
    static String format(String template, final Object arg) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        if (placeholderStart >= 0) {
            sb.append(template, 0, placeholderStart);
            sb.append(N.toString(arg));
            sb.append(template, placeholderStart + 2, template.length());
        } else {
            sb.append(" [");
            sb.append(N.toString(arg));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 32);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 48);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 64);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 64);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @param arg4
     * @param arg5
     * @param arg6
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 64);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                            placeholderStart = template.indexOf(placeholder, templateStart);

                            if (placeholderStart >= 0) {
                                cnt++;
                                sb.append(template, templateStart, placeholderStart);
                                sb.append(N.toString(arg6));
                                templateStart = placeholderStart + 2;
                            }
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(']');
        } else if (cnt == 5) {
            sb.append(" [");
            sb.append(N.toString(arg6));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

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
     * @return
     */
    static String format(String template, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
            final Object arg7) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 64);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                    placeholderStart = template.indexOf(placeholder, templateStart);

                    if (placeholderStart >= 0) {
                        cnt++;
                        sb.append(template, templateStart, placeholderStart);
                        sb.append(N.toString(arg4));
                        templateStart = placeholderStart + 2;
                        placeholderStart = template.indexOf(placeholder, templateStart);

                        if (placeholderStart >= 0) {
                            cnt++;
                            sb.append(template, templateStart, placeholderStart);
                            sb.append(N.toString(arg5));
                            templateStart = placeholderStart + 2;
                            placeholderStart = template.indexOf(placeholder, templateStart);

                            if (placeholderStart >= 0) {
                                cnt++;
                                sb.append(template, templateStart, placeholderStart);
                                sb.append(N.toString(arg6));
                                templateStart = placeholderStart + 2;
                                placeholderStart = template.indexOf(placeholder, templateStart);

                                if (placeholderStart >= 0) {
                                    cnt++;
                                    sb.append(template, templateStart, placeholderStart);
                                    sb.append(N.toString(arg7));
                                    templateStart = placeholderStart + 2;
                                }
                            }
                        }
                    }
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
            sb.append(", ");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 3) {
            sb.append(" [");
            sb.append(N.toString(arg4));
            sb.append(", ");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 4) {
            sb.append(" [");
            sb.append(N.toString(arg5));
            sb.append(", ");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 5) {
            sb.append(" [");
            sb.append(N.toString(arg6));
            sb.append(", ");
            sb.append(N.toString(arg7));
            sb.append(']');
        } else if (cnt == 6) {
            sb.append(" [");
            sb.append(N.toString(arg7));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position: the first {@code %s} gets {@code args[0]}, etc. If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted message in
     * square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s} placeholders.
     * @param args the arguments to be substituted into the message template. Arguments are converted
     *     to strings using {@link String#valueOf(Object)}. Arguments can be null.
     * @return
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, final Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (N.isEmpty(args)) {
            return template;
        }

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        while (placeholderStart >= 0 && i < args.length) {
            sb.append(template, templateStart, placeholderStart);
            sb.append(N.toString(args[i++]));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);
        }

        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(" [");
            sb.append(N.toString(args[i++]));
            while (i < args.length) {
                sb.append(", ");
                sb.append(N.toString(args[i++]));
            }
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }
}
