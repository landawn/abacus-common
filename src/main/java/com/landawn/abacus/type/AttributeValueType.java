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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AttributeValueType extends AbstractType<AttributeValue> {

    public static final String ATTRIBUTE_VALUE = "AttributeValue";

    private static final Type<ByteBuffer> byteBufferType = TypeFactory.getType("ByteBuffer");

    private static final Type<List<String>> strListType = TypeFactory.getType("List<String>");

    private static final Type<List<ByteBuffer>> byteBufferListType = TypeFactory.getType("List<ByteBuffer>");

    private static final Type<List<AttributeValue>> attrValueListType = TypeFactory.getType("List<AttributeValue>");

    private static final Type<Map<String, AttributeValue>> attrMapType = TypeFactory.getType("Map<String, AttributeValue>");

    AttributeValueType() {
        super(ATTRIBUTE_VALUE);
    }

    @Override
    public Class<AttributeValue> clazz() {
        return AttributeValue.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(AttributeValue x) {
        if (x == null || (x.getNULL() != null && x.isNULL())) {
            return null;
        }

        if (N.notNullOrEmpty(x.getS())) {
            return x.getS();
        } else if (N.notNullOrEmpty(x.getN())) {
            return x.getN();
        } else if (x.getBOOL() != null) {
            return x.getBOOL().toString();
        } else if (x.getB() != null) {
            return byteBufferType.stringOf(x.getB());
        } else if (N.notNullOrEmpty(x.getSS())) {
            return strListType.stringOf(x.getSS());
        } else if (N.notNullOrEmpty(x.getNS())) {
            return strListType.stringOf(x.getNS());
        } else if (N.notNullOrEmpty(x.getBS())) {
            return byteBufferListType.stringOf(x.getBS());
        } else if (N.notNullOrEmpty(x.getL())) {
            return attrValueListType.stringOf(x.getL());
        } else if (N.notNullOrEmpty(x.getM())) {
            return attrMapType.stringOf(x.getM());
        } else if (x.getS() != null) {
            return x.getS();
        } else if (x.getN() != null) {
            return x.getN();
        } else if (x.getSS() != null) {
            return strListType.stringOf(x.getSS());
        } else if (x.getNS() != null) {
            return strListType.stringOf(x.getNS());
        } else if (x.getBS() != null) {
            return byteBufferListType.stringOf(x.getBS());
        } else if (x.getL() != null) {
            return attrValueListType.stringOf(x.getL());
        } else if (x.getM() != null) {
            return attrMapType.stringOf(x.getM());
        } else if (x.getNULL() != null) {
            return x.getNULL().toString();
        } else {
            throw new IllegalArgumentException("Unsupported Attribute type: " + x.toString());
        }
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public AttributeValue valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : new AttributeValue(str);
    }
}
