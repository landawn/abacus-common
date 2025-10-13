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

package com.landawn.abacus.parser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

import lombok.Data;

/**
 * Utility class for checking ASM (reflectasm) availability in the runtime environment.
 * This class provides functionality to determine if the reflectasm library is available
 * and can be used for optimized reflection operations.
 * 
 * <p>The class performs a runtime check to verify if reflectasm classes are present
 * and functional by attempting to use MethodAccess and FieldAccess on a test bean.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * if (ASMUtil.isASMAvailable()) {
 *     // Use ASM-optimized reflection
 * } else {
 *     // Fall back to standard reflection
 * }
 * }</pre>
 * 
 * @since 0.8
 */
final class ASMUtil {
    private static final Logger logger = LoggerFactory.getLogger(ASMUtil.class);

    private static final boolean isASMAvailable; // NOSONAR

    static {
        boolean tmp = false;

        try {
            Class.forName("com.esotericsoftware.reflectasm.MethodAccess");
            Class.forName("org.objectweb.asm.ClassWriter");

            final Method getMethod = TestBeanA.class.getMethod("getName");
            final Method setMethod = TestBeanA.class.getMethod("setName", String.class);
            final Field field = TestBeanA.class.getDeclaredField("name");

            final var getMethodAccess = com.esotericsoftware.reflectasm.MethodAccess.get(getMethod.getDeclaringClass());
            final var setMethodAccess = com.esotericsoftware.reflectasm.MethodAccess.get(setMethod.getDeclaringClass());
            final var fieldAccess = com.esotericsoftware.reflectasm.FieldAccess.get(field.getDeclaringClass());

            final int getMethodAccessIndex = getMethodAccess.getIndex(getMethod.getName(), 0);
            final int setMethodAccessIndex = setMethodAccess.getIndex(setMethod.getName(), setMethod.getParameterTypes());
            final int fieldAccessIndex = (Modifier.isPrivate(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) ? -1
                    : fieldAccess.getIndex(field.getName());

            final TestBeanA bean = new TestBeanA();

            setMethodAccess.invoke(bean, setMethodAccessIndex, "Tom");
            tmp = Objects.equals(fieldAccess.get(bean, fieldAccessIndex), getMethodAccess.invoke(bean, getMethodAccessIndex));

        } catch (final Throwable e) { // NOSONAR
            // ignore.

            logger.warn("ASM is not available by com.esotericsoftware.reflectasm due to exception: ", e.getClass().getName());
        }

        isASMAvailable = tmp;

        if (isASMAvailable) {
            logger.info("ASM is available by com.esotericsoftware.reflectasm");
        } else {
            logger.info("ASM is not available by com.esotericsoftware.reflectasm");
        }
    }

    /**
     * Checks if ASM (reflectasm) is available in the runtime environment.
     * 
     * <p>This method returns the result of the static initialization check that determines
     * whether the reflectasm library is present and functional. The check is performed
     * once during class loading.</p>
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * if (ASMUtil.isASMAvailable()) {
     *     // Use optimized reflection with ASM
     *     MethodAccess access = MethodAccess.get(MyClass.class);
     * } else {
     *     // Use standard Java reflection
     *     Method method = MyClass.class.getMethod("myMethod");
     * }
     * }</pre>
     *
     * @return {@code true} if ASM (reflectasm) is available and functional, {@code false} otherwise
     */
    public static boolean isASMAvailable() {
        return isASMAvailable;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     * This class is designed as a singleton utility and should not be instantiated.
     */
    private ASMUtil() {
        // Singleton.
    }

    /**
     * Test bean class used internally for verifying ASM functionality.
     * This class is used during the static initialization to test whether
     * reflectasm can successfully access fields and methods.
     * 
     * <p>The class contains a private id field and a public name field
     * to test different access levels with ASM.</p>
     */
    @Data
    public static final class TestBeanA {
        private int id;
        public String name; //NOSONAR
    }
}