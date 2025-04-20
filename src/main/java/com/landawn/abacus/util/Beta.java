/*
 * Copyright (C) 2024 HaiYang Li
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

import java.util.Iterator;
import java.util.Map;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;

/**
 *
 * Not certainly
 *
 */
@com.landawn.abacus.annotation.Beta
public final class Beta {

    private Beta() {
        // utility class
    }

    /**
     * Recursively hashcode each element in Array/Iterable/Iterator or each key/value in Map or each property/field in a bean object.
     *
     * @param obj
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static long hashCodeEverything(final Object obj) {
        if (obj == null) {
            return 0;
        }

        long ret = 1;

        if (obj instanceof Iterable iter) {
            for (final Object e : iter) {
                ret = 31 * ret + hashCodeEverything(e);
            }
        } else if (obj instanceof Map) {
            final Map<Object, Object> map = (Map) obj;

            for (final Map.Entry<Object, Object> entry : map.entrySet()) {
                ret = 31 * ret + hashCodeEverything(entry.getKey());
                ret = 31 * ret + hashCodeEverything(entry.getValue());
            }
        } else if (obj.getClass().isArray()) {
            if (obj instanceof Object[] a) {
                for (final Object e : a) {
                    ret = 31 * ret + hashCodeEverything(e);
                }
            } else {
                ret = 31 * ret + N.deepHashCode(obj);
            }
        } else if (obj instanceof Iterator iter) {
            while (iter.hasNext()) {
                ret = 31 * ret + hashCodeEverything(iter.next());
            }
        } else if (ClassUtil.isBeanClass(obj.getClass())) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(obj.getClass());

            for (final PropInfo propInfo : beanInfo.propInfoList) {
                ret = 31 * ret + N.hashCode(propInfo.name);
                ret = 31 * ret + hashCodeEverything(propInfo.getPropValue(obj));
            }
        } else {
            ret = 31 * ret + N.hashCode(obj);
        }

        return ret;
    }

}
