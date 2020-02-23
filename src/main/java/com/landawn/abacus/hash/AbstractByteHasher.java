/*
 * Copyright (C) 2012 The Guava Authors
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.landawn.abacus.hash.Util.Chars;
import com.landawn.abacus.hash.Util.Ints;
import com.landawn.abacus.hash.Util.Longs;
import com.landawn.abacus.hash.Util.Shorts;
import com.landawn.abacus.util.N;

/**
 * Note: It's copied from Google Guava under Apache License 2.0
 * 
 * Abstract {@link Hasher} that handles converting primitives to bytes using a scratch {@code
 * ByteBuffer} and streams all bytes to a sink to compute the hash.
 *
 * @author Colin Decker
 */
abstract class AbstractByteHasher extends AbstractHasher {

    /** The scratch. */
    private final ByteBuffer scratch = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    /**
     * Updates this hasher with the given byte.
     *
     * @param b
     */
    protected abstract void update(byte b);

    /**
     * Updates this hasher with the given bytes.
     *
     * @param b
     */
    protected void update(byte[] b) {
        update(b, 0, b.length);
    }

    /**
     * Updates this hasher with {@code len} bytes starting at {@code off} in the given buffer.
     *
     * @param b
     * @param off
     * @param len
     */
    protected void update(byte[] b, int off, int len) {
        for (int i = off; i < off + len; i++) {
            update(b[i]);
        }
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public Hasher put(byte b) {
        update(b);
        return this;
    }

    /**
     *
     * @param bytes
     * @return
     */
    @Override
    public Hasher put(byte[] bytes) {
        N.checkArgNotNull(bytes);
        update(bytes);
        return this;
    }

    /**
     *
     * @param bytes
     * @param off
     * @param len
     * @return
     */
    @Override
    public Hasher put(byte[] bytes, int off, int len) {
        Util.checkPositionIndexes(off, off + len, bytes.length);

        update(bytes, off, len);
        return this;
    }

    /**
     * Updates the sink with the given number of bytes from the buffer.
     *
     * @param bytes
     * @return
     */
    private Hasher update(int bytes) {
        try {
            update(scratch.array(), 0, bytes);
        } finally {
            scratch.clear();
        }
        return this;
    }

    /**
     *
     * @param s
     * @return
     */
    @Override
    public Hasher put(short s) {
        scratch.putShort(s);
        return update(Shorts.BYTES);
    }

    /**
     *
     * @param i
     * @return
     */
    @Override
    public Hasher put(int i) {
        scratch.putInt(i);
        return update(Ints.BYTES);
    }

    /**
     *
     * @param l
     * @return
     */
    @Override
    public Hasher put(long l) {
        scratch.putLong(l);
        return update(Longs.BYTES);
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public Hasher put(char c) {
        scratch.putChar(c);
        return update(Chars.BYTES);
    }
}
