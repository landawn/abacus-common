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
import java.io.OutputStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class LZ4BlockOutputStream extends OutputStream {

    private final net.jpountz.lz4.LZ4BlockOutputStream out;

    /**
     * 
     *
     * @param os 
     */
    public LZ4BlockOutputStream(OutputStream os) {
        this.out = new net.jpountz.lz4.LZ4BlockOutputStream(os);
    }

    /**
     * 
     *
     * @param os 
     * @param blockSize 
     */
    public LZ4BlockOutputStream(OutputStream os, int blockSize) {
        this.out = new net.jpountz.lz4.LZ4BlockOutputStream(os, blockSize);
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
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(byte[] b) throws IOException {
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
    public void write(byte[] b, int off, int len) throws IOException {
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
    public void finish() throws IOException {
        out.finish();
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
