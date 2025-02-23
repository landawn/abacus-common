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

package com.landawn.abacus.type;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;

@SuppressWarnings({ "rawtypes", "java:S2160" })
public class ClazzType extends AbstractType<Class> {

    public static final String CLAZZ = "Clazz"; //NOSONAR

    private final Class clazz; //NOSONAR

    protected ClazzType(final String typeName) {
        super("Clazz<" + typeName + ">");

        clazz = ClassUtil.forClass(typeName);
    }

    @Override
    public Class<Class> clazz() {
        return clazz;
    }

    /**
     * Checks if is immutable.
     *
     * @return {@code true}, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Class x) {
        return x == null ? null : ClassUtil.getCanonicalClassName(x);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Class valueOf(final String str) {
        return Strings.isEmpty(str) ? null : ClassUtil.forClass(str);
    }
}
