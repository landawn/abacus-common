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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public abstract class AbstractLogger implements Logger {
    protected final String name;

    protected AbstractLogger(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void trace(String template, Object arg) {
        if (isTraceEnabled()) {
            trace(format(template, arg));
        }
    }

    @Override
    public void trace(String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2));
        }
    }

    @Override
    public void trace(String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void trace(String template, Object... args) {
        if (isTraceEnabled()) {
            trace(format(template, args));
        }
    }

    @Override
    public void trace(Throwable t, String msg) {
        trace(msg, t);
    }

    @Override
    public void trace(Throwable t, String template, Object arg) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg));
        }
    }

    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2));
        }
    }

    @Override
    public void trace(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isTraceEnabled()) {
            trace(t, format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void trace(Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(supplier.get());
        }
    }

    @Override
    public void trace(Supplier<String> supplier, Throwable t) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    @Override
    public void trace(Throwable t, Supplier<String> supplier) {
        if (isTraceEnabled()) {
            trace(t, supplier.get());
        }
    }

    @Override
    public void debug(String template, Object arg) {
        if (isDebugEnabled()) {
            debug(format(template, arg));
        }
    }

    @Override
    public void debug(String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2));
        }
    }

    @Override
    public void debug(String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void debug(String template, Object... args) {
        if (isDebugEnabled()) {
            debug(format(template, args));
        }
    }

    @Override
    public void debug(Throwable t, String msg) {
        debug(msg, t);
    }

    @Override
    public void debug(Throwable t, String template, Object arg) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg));
        }
    }

    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2));
        }
    }

    @Override
    public void debug(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isDebugEnabled()) {
            debug(t, format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void debug(Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(supplier.get());
        }
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable t) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    @Override
    public void debug(Throwable t, Supplier<String> supplier) {
        if (isDebugEnabled()) {
            debug(t, supplier.get());
        }
    }

    @Override
    public void info(String template, Object arg) {
        if (isInfoEnabled()) {
            info(format(template, arg));
        }
    }

    @Override
    public void info(String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2));
        }
    }

    @Override
    public void info(String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void info(String template, Object... args) {
        if (isInfoEnabled()) {
            info(format(template, args));
        }
    }

    @Override
    public void info(Throwable t, String msg) {
        info(msg, t);
    }

    @Override
    public void info(Throwable t, String template, Object arg) {
        if (isInfoEnabled()) {
            info(t, format(template, arg));
        }
    }

    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2));
        }
    }

    @Override
    public void info(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isInfoEnabled()) {
            info(t, format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void info(Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(supplier.get());
        }
    }

    @Override
    public void info(Supplier<String> supplier, Throwable t) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    @Override
    public void info(Throwable t, Supplier<String> supplier) {
        if (isInfoEnabled()) {
            info(t, supplier.get());
        }
    }

    @Override
    public void warn(String template, Object arg) {
        if (isWarnEnabled()) {
            warn(format(template, arg));
        }
    }

    @Override
    public void warn(String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2));
        }
    }

    @Override
    public void warn(String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void warn(String template, Object... args) {
        if (isWarnEnabled()) {
            warn(format(template, args));
        }
    }

    @Override
    public void warn(Throwable t, String msg) {
        warn(msg, t);
    }

    @Override
    public void warn(Throwable t, String template, Object arg) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg));
        }
    }

    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2));
        }
    }

    @Override
    public void warn(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isWarnEnabled()) {
            warn(t, format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void warn(Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(supplier.get());
        }
    }

    @Override
    public void warn(Supplier<String> supplier, Throwable t) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    @Override
    public void warn(Throwable t, Supplier<String> supplier) {
        if (isWarnEnabled()) {
            warn(t, supplier.get());
        }
    }

    @Override
    public void error(String template, Object arg) {
        if (isErrorEnabled()) {
            error(format(template, arg));
        }
    }

    @Override
    public void error(String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2));
        }
    }

    @Override
    public void error(String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void error(String template, Object... args) {
        if (isErrorEnabled()) {
            error(format(template, args));
        }
    }

    @Override
    public void error(Throwable t, String msg) {
        error(msg, t);
    }

    @Override
    public void error(Throwable t, String template, Object arg) {
        if (isErrorEnabled()) {
            error(t, format(template, arg));
        }
    }

    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2));
        }
    }

    @Override
    public void error(Throwable t, String template, Object arg1, Object arg2, Object arg3) {
        if (isErrorEnabled()) {
            error(t, format(template, arg1, arg2, arg3));
        }
    }

    @Override
    public void error(Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(supplier.get());
        }
    }

    @Override
    public void error(Supplier<String> supplier, Throwable t) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    @Override
    public void error(Throwable t, Supplier<String> supplier) {
        if (isErrorEnabled()) {
            error(t, supplier.get());
        }
    }

    static String format(String template, Object arg) {
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

    static String format(String template, Object arg1, Object arg2) {
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

    static String format(String template, Object arg1, Object arg2, Object arg3) {
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
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position: the first {@code %s} gets {@code args[0]}, etc. If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted message in
     * square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s} placeholders.
     * @param args the arguments to be substituted into the message template. Arguments are converted
     *     to strings using {@link String#valueOf(Object)}. Arguments can be null.
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (N.isNullOrEmpty(args)) {
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
