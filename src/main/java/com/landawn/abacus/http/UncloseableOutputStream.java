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
import java.io.OutputStream;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class UncloseableOutputStream extends OutputStream {

    private final OutputStream out;

    public UncloseableOutputStream(OutputStream os) {
        this.out = os;
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        // just flush without closing the target the OutputStream.

        out.flush();
    }

    @Override
    public int hashCode() {
        return out.hashCode();
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

        if (obj instanceof UncloseableOutputStream) {
            UncloseableOutputStream other = (UncloseableOutputStream) obj;

            return N.equals(this.out, other.out);
        }

        return false;
    }

    @Override
    public String toString() {
        return out.toString();
    }
}
