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
import java.io.InputStream;

public final class LZ4BlockInputStream extends InputStream {

    private final net.jpountz.lz4.LZ4BlockInputStream in;

    /**
     *
     * @param is
     */
    public LZ4BlockInputStream(final InputStream is) {
        in = new net.jpountz.lz4.LZ4BlockInputStream(is);
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
     *
     * @param b
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return in.read(b);
    }

    /**
     *
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     *
     * @param n
     * @return
     * @throws IllegalArgumentException
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public long skip(final long n) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(n, cs.n);

        return in.skip(n);
    }

    /**
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public int available() throws IOException {
        return in.available();
    }

    /**
     *
     * @param readLimit
     */
    @Override
    public synchronized void mark(final int readLimit) {
        in.mark(readLimit);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void close() throws IOException {
        in.close();
    }
}
