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

/**
 * Guava-compatible hashing abstractions and algorithm factories.
 *
 * <p>{@link Hashing} is the entry point. It supplies:</p>
 * <ul>
 *   <li>Non-cryptographic hashes: {@code goodFastHash}, Murmur3 (32/128), SipHash-2-4, FarmHash Fingerprint64</li>
 *   <li>Cryptographic hashes: MD5, SHA-1, SHA-256, SHA-384, SHA-512, and HMAC variants</li>
 *   <li>Checksums: CRC32, CRC32C, Adler32</li>
 *   <li>Composition: concatenating hash functions and combining {@code HashCode} values</li>
 * </ul>
 *
 * <p>{@link HashFunction} is a stateless algorithm. {@link Hasher} is a stateful, incremental sink
 * obtained from {@link HashFunction#newHasher()}. Both one-shot {@code hash(...)} methods and
 * incremental {@code put(...)} / {@code hash()} flows return Guava
 * {@link com.google.common.hash.HashCode}.</p>
 *
 * <p>Obtain instances from {@link Hashing}; do not construct hash-function implementations directly.
 * Portions are derived from Google Guava under the Apache License 2.0.</p>
 *
 * @see Hashing
 * @see HashFunction
 * @see Hasher
 */
package com.landawn.abacus.guava.hash;
