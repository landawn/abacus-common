/*
 * Copyright (C) 2026 HaiYang Li
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

package com.landawn.abacus.parser;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import com.landawn.abacus.util.ByteArrayOutputStream;

/** Internal Base64 adapters that do not copy a large, completed binary payload before encoding it. */
final class BinaryParserSupport {
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    /**
     * Complete 3-byte groups encoded per Writer chunk: 2031 groups become 8124 characters, the chunk size of the
     * JDK's streaming encoder, so a Writer receives the same bounded writes as it did from that encoder.
     */
    private static final int GROUPS_PER_CHUNK = 2031;

    private BinaryParserSupport() {
    }

    static String encodeBase64(final ByteArrayOutputStream source) {
        // For small payloads the byte-array copy is cheaper than two ByteBuffer wrappers.
        if (source.size() < 1024) {
            return BASE64_ENCODER.encodeToString(source.toByteArray());
        }

        final ByteBuffer encoded = BASE64_ENCODER.encode(ByteBuffer.wrap(source.array(), 0, source.size()));
        return new String(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining(), StandardCharsets.ISO_8859_1);
    }

    static void writeBase64(final ByteArrayOutputStream source, final Writer output) throws IOException {
        final int size = source.size();

        if (size < 8192) {
            output.write(encodeBase64(source));
            return;
        }

        // Binary serialization has already succeeded. Encode bounded slices of complete 3-byte groups with the
        // array encoder and widen the ASCII bytes with the ISO-8859-1 decoder - both run as JIT intrinsics, whereas
        // the JDK's streaming encoder is a scalar loop that would still need a scalar byte-to-char pass - and emit
        // each slice as one chunk of characters, without closing or flushing the caller's Writer; the parser retains
        // ownership of flushing. A write failure propagates from the failing call and nothing more is written: a
        // Writer that has thrown receives no stale padded group and no suppressed close failure (the streaming
        // encoder finished its pending group in close(), after the Writer had already failed).
        final byte[] bytes = source.array();
        final int tailLength = size % 3;
        final int groupsLength = size - tailLength;
        final byte[] encoded = new byte[GROUPS_PER_CHUNK * 4];
        final char[] chars = new char[GROUPS_PER_CHUNK * 4];
        final CharsetDecoder widener = StandardCharsets.ISO_8859_1.newDecoder();
        byte[] slice = null;

        for (int offset = 0; offset < groupsLength;) {
            final int sliceLength = Math.min(GROUPS_PER_CHUNK * 3, groupsLength - offset);

            if (slice == null || slice.length != sliceLength) {
                // The array encoder encodes a whole array; a slice holds complete groups only, so it is never padded.
                slice = new byte[sliceLength];
            }

            System.arraycopy(bytes, offset, slice, 0, sliceLength);

            final int encodedLength = BASE64_ENCODER.encode(slice, encoded);

            widener.reset();
            widener.decode(ByteBuffer.wrap(encoded, 0, encodedLength), CharBuffer.wrap(chars), true);

            output.write(chars, 0, encodedLength);
            offset += sliceLength;
        }

        if (tailLength != 0) {
            // The last, padded group is written character by character, as the streaming encoder finished it.
            for (final byte b : BASE64_ENCODER.encode(Arrays.copyOfRange(bytes, groupsLength, size))) {
                output.write(b);
            }
        }
    }
}
