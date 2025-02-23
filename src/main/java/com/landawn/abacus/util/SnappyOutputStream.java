/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.OutputStream;

public final class SnappyOutputStream extends OutputStream {

    private final org.xerial.snappy.SnappyOutputStream out;

    /**
     *
     * @param os
     */
    public SnappyOutputStream(final OutputStream os) {
        out = new org.xerial.snappy.SnappyOutputStream(os);
    }

    /**
     *
     * @param os
     * @param bufferSizes
     */
    public SnappyOutputStream(final OutputStream os, final int bufferSizes) {
        out = new org.xerial.snappy.SnappyOutputStream(os, bufferSizes);
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(final int b) throws IOException {
        out.write(b);
    }

    /**
     *
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(final byte[] b) throws IOException {
        out.write(b);
    }

    /**
     *
     * @param b
     * @param off
     * @param len
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void flush() throws IOException {
        out.flush();
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        out.close();
    }
}
