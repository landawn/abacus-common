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

package com.landawn.abacus.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.common.base.Objects;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

import lombok.Data;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
final class ASMUtil {

    private static final Logger logger = LoggerFactory.getLogger(ASMUtil.class);

    private static final boolean isASMAvailable;

    static {
        boolean tmp = false;

        try {
            Class.forName("com.esotericsoftware.reflectasm.MethodAccess");
            Class.forName("org.objectweb.asm.ClassWriter");

            final Method getMethod = TestBeanA.class.getMethod("getName");
            final Method setMethod = TestBeanA.class.getMethod("setName", String.class);
            final Field field = TestBeanA.class.getDeclaredField("name");

            final com.esotericsoftware.reflectasm.MethodAccess getMethodAccess = com.esotericsoftware.reflectasm.MethodAccess
                    .get(getMethod.getDeclaringClass());
            final com.esotericsoftware.reflectasm.MethodAccess setMethodAccess = com.esotericsoftware.reflectasm.MethodAccess
                    .get(setMethod.getDeclaringClass());
            final com.esotericsoftware.reflectasm.FieldAccess fieldAccess = com.esotericsoftware.reflectasm.FieldAccess.get(field.getDeclaringClass());

            final int getMethodAccessIndex = getMethodAccess.getIndex(getMethod.getName(), 0);
            final int setMethodAccessIndex = setMethodAccess.getIndex(setMethod.getName(), setMethod.getParameterTypes());
            final int fieldAccessIndex = (Modifier.isPrivate(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) ? -1
                    : fieldAccess.getIndex(field.getName());

            TestBeanA bean = new TestBeanA();

            setMethodAccess.invoke(bean, setMethodAccessIndex, "Tom");
            tmp = Objects.equal(fieldAccess.get(bean, fieldAccessIndex), getMethodAccess.invoke(bean, getMethodAccessIndex));

        } catch (Throwable e) {
            // ignore.

            logger.warn("ASM is not avaiable by com.esotericsoftware.reflectasm due to exception: ", e.getClass().getName());
        }

        isASMAvailable = tmp;

        if (isASMAvailable) {
            logger.info("ASM is avaiable by com.esotericsoftware.reflectasm");
        } else {
            logger.info("ASM is not avaiable by com.esotericsoftware.reflectasm");
        }
    }

    /**
     * Checks if is ASM available.
     *
     * @return true, if is ASM available
     */
    public static boolean isASMAvailable() {
        return isASMAvailable;
    }

    private ASMUtil() {
        // Singleton.
    }

    @Data
    public static final class TestBeanA {
        private int id;
        public String name;
    }
}
