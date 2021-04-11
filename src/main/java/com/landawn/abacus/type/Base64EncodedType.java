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

package com.landawn.abacus.type;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Base64EncodedType extends AbstractType<byte[]> {

    public static final String BASE64_ENCODED = "Base64Encoded";

    Base64EncodedType() {
        super(BASE64_ENCODED);
    }

    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    @Override
    public String stringOf(byte[] x) {
        return N.base64Encode(x);
    }

    @Override
    public byte[] valueOf(String base64String) {
        return N.base64Decode(base64String);
    }
}
