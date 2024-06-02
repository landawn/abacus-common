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

import org.bson.types.ObjectId;

import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class BSONObjectIdType extends AbstractType<ObjectId> {

    public static final String BSON_OBJECT_ID = "BSONObjectId";

    BSONObjectIdType() {
        super(BSON_OBJECT_ID);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<ObjectId> clazz() {
        return ObjectId.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(ObjectId x) {
        return x == null ? null : x.toHexString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public ObjectId valueOf(String str) {
        return Strings.isEmpty(str) ? null : new ObjectId(str);
    }
}
