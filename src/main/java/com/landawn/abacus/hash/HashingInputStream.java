/*
 * Copyright (C) 2013 The Guava Authors
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

package com.landawn.abacus.hash;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.landawn.abacus.util.N;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * An {@link InputStream} that maintains a hash of the data read from it.
 *
 * @author Qian Huang
 * @since 16.0
 */
public final class HashingInputStream extends FilterInputStream {

    private final Hasher hasher;

    /**
     * Creates an input stream that hashes using the given {@link HashFunction} and delegates all data
     * read from it to the underlying {@link InputStream}.
     * 
     * <p>The {@link InputStream} should not be read from before or after the hand-off.
     *
     * @param hashFunction
     * @param in
     */
    public HashingInputStream(HashFunction hashFunction, InputStream in) {
        super(N.checkArgNotNull(in));
        this.hasher = N.checkArgNotNull(hashFunction.newHasher());
    }

    /**
     * Reads the next byte of data from the underlying input stream and updates the hasher with the
     * byte read.
     *
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override

    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            hasher.put((byte) b);
        }
        return b;
    }

    /**
     * Reads the specified bytes of data from the underlying input stream and updates the hasher with
     * the bytes read.
     *
     * @param bytes
     * @param off
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override

    public int read(byte[] bytes, int off, int len) throws IOException {
        int numOfBytesRead = in.read(bytes, off, len);
        if (numOfBytesRead != -1) {
            hasher.put(bytes, off, numOfBytesRead);
        }
        return numOfBytesRead;
    }

    /**
     * mark() is not supported for HashingInputStream.
     *
     * @return {@code false} always
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * mark() is not supported for HashingInputStream.
     *
     * @param readlimit
     */
    @Override
    public void mark(int readlimit) {
    }

    /**
     * reset() is not supported for HashingInputStream.
     *
     * @throws IOException this operation is not supported
     */
    @Override
    public void reset() throws IOException {
        throw new IOException("reset not supported");
    }

    /**
     * Returns the {@link HashCode} based on the data read from this stream. The result is unspecified
     * if this method is called more than once on the same instance.
     *
     * @return
     */
    public HashCode hash() {
        return hasher.hash();
    }
}
