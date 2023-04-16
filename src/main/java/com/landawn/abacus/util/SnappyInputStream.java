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

package com.landawn.abacus.util;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class SnappyInputStream extends InputStream {

    private final org.xerial.snappy.SnappyInputStream in;

    /**
     * 
     *
     * @param is 
     * @throws IOException 
     */
    public SnappyInputStream(InputStream is) throws IOException {
        in = new org.xerial.snappy.SnappyInputStream(is);
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
    public int read(byte[] b) throws IOException {
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
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    /**
     *
     * @param n
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public long skip(long n) throws IOException {
        N.checkArgNotNegative(n, "n");

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
     * @param readlimit
     */
    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    /**
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }

    /**
     *
     * @return
     */
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
