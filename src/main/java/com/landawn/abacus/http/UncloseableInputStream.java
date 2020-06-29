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

package com.landawn.abacus.http;

import java.io.IOException;
import java.io.InputStream;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class UncloseableInputStream extends InputStream {

    private final InputStream in;

    public UncloseableInputStream(InputStream is) {
        this.in = is;
    }

    /**
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int read() throws IOException {
        return in.read();
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        // ignore. do nothing.
    }

    @Override
    public int hashCode() {
        return in.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof UncloseableInputStream) {
            UncloseableInputStream other = (UncloseableInputStream) obj;

            return N.equals(this.in, other.in);
        }

        return false;
    }

    @Override
    public String toString() {
        return in.toString();
    }
}
