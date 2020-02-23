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

import android.util.Log;

/**
 * The Class AndroidLogger.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class AndroidLogger extends AbstractLogger {

    /** The Constant MAX_TAG_SIZE. */
    private static final int MAX_TAG_SIZE = 23;

    /**
     * Instantiates a new android logger.
     *
     * @param name
     */
    public AndroidLogger(String name) {
        super(name.startsWith("com.landawn.abacus") ? "Abacus"
                : (name.length() > MAX_TAG_SIZE ? name.substring(N.max(name.length() - MAX_TAG_SIZE, name.lastIndexOf('.') + 1)) : name));

        try {
            Class.forName("android.util.Log");
        } catch (Throwable e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Checks if is trace enabled.
     *
     * @return true, if is trace enabled
     */
    @Override
    public boolean isTraceEnabled() {
        return Log.isLoggable(name, Log.VERBOSE);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void trace(String msg) {
        Log.v(name, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void trace(String msg, Throwable t) {
        Log.v(name, msg, t);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return true, if is debug enabled
     */
    @Override
    public boolean isDebugEnabled() {
        return Log.isLoggable(name, Log.DEBUG);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void debug(String msg) {
        Log.d(name, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void debug(String msg, Throwable t) {
        Log.d(name, msg, t);
    }

    /**
     * Checks if is info enabled.
     *
     * @return true, if is info enabled
     */
    @Override
    public boolean isInfoEnabled() {
        return Log.isLoggable(name, Log.INFO);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void info(String msg) {
        Log.i(name, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void info(String msg, Throwable t) {
        Log.i(name, msg, t);
    }

    /**
     * Checks if is warn enabled.
     *
     * @return true, if is warn enabled
     */
    @Override
    public boolean isWarnEnabled() {
        return Log.isLoggable(name, Log.WARN);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void warn(String msg) {
        Log.w(name, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void warn(String msg, Throwable t) {
        Log.w(name, msg, t);
    }

    /**
     * Checks if is error enabled.
     *
     * @return true, if is error enabled
     */
    @Override
    public boolean isErrorEnabled() {
        return Log.isLoggable(name, Log.ERROR);
    }

    /**
     *
     * @param msg
     */
    @Override
    public void error(String msg) {
        Log.e(name, msg);
    }

    /**
     *
     * @param msg
     * @param t
     */
    @Override
    public void error(String msg, Throwable t) {
        Log.e(name, msg, t);
    }
}
