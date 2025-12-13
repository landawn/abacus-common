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

package com.landawn.abacus.exception;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;

/**
 * A runtime exception that wraps {@link ReflectiveOperationException} and its subclasses,
 * allowing reflection exceptions to be thrown without being declared in method signatures.
 * 
 * <p>This exception wraps all reflection-related checked exceptions including:</p>
 * <ul>
 *   <li>{@link ClassNotFoundException} - When a class cannot be found</li>
 *   <li>{@link InstantiationException} - When a class cannot be instantiated</li>
 *   <li>{@link IllegalAccessException} - When access to a class, method, or field is denied</li>
 *   <li>{@link NoSuchMethodException} - When a method cannot be found</li>
 *   <li>{@link NoSuchFieldException} - When a field cannot be found</li>
 *   <li>{@link InvocationTargetException} - When an invoked method throws an exception</li>
 * </ul>
 * 
 * <p>This exception is particularly useful in contexts where reflection exceptions cannot be declared:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces</li>
 *   <li>Stream operations involving reflection</li>
 *   <li>Framework code that uses reflection internally</li>
 *   <li>Dynamic proxy implementations</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Creating instances dynamically
 * public <T> T createInstance(Class<T> clazz) {
 *     try {
 *         return clazz.getDeclaredConstructor().newInstance();
 *     } catch (ReflectiveOperationException e) {
 *         throw new UncheckedReflectiveOperationException(
 *             "Failed to create instance of " + clazz.getName(), e);
 *     }
 * }
 * 
 * // In a stream operation
 * List<Object> instances = classNames.stream()
 *     .map(className -> {
 *         try {
 *             return Class.forName(className).getDeclaredConstructor().newInstance();
 *         } catch (ReflectiveOperationException e) {
 *             throw new UncheckedReflectiveOperationException(e);
 *         }
 *     })
 *     .collect(Collectors.toList());
 * 
 * // Accessing private fields
 * public Object getFieldValue(Object obj, String fieldName) {
 *     try {
 *         Field field = obj.getClass().getDeclaredField(fieldName);
 *         field.setAccessible(true);
 *         return field.get(obj);
 *     } catch (ReflectiveOperationException e) {
 *         throw new UncheckedReflectiveOperationException(
 *             "Cannot access field: " + fieldName, e);
 *     }
 * }
 * }</pre>
 * 
 * @see UncheckedException
 * @see ReflectiveOperationException
 * @see ClassNotFoundException
 * @see NoSuchMethodException
 * @see IllegalAccessException
 */
public class UncheckedReflectiveOperationException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = 8731226260560735852L;

    /**
     * Constructs a new {@code UncheckedReflectiveOperationException} by wrapping the specified
     * {@link ReflectiveOperationException} or any of its subclasses.
     *
     * <p>This constructor preserves all information from the original exception including
     * its message, stack trace, and any suppressed exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Method method = clazz.getMethod("someMethod", String.class);
     *     return method.invoke(instance, "parameter");
     * } catch (ReflectiveOperationException e) {
     *     throw new UncheckedReflectiveOperationException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link ReflectiveOperationException} to wrap. Must not be {@code null}.
     *              This can be any subclass of ReflectiveOperationException including
     *              ClassNotFoundException, NoSuchMethodException, IllegalAccessException, etc.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedReflectiveOperationException(final ReflectiveOperationException cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UncheckedReflectiveOperationException} with the specified detail message
     * and {@link ReflectiveOperationException}.
     *
     * <p>This constructor allows you to provide additional context about the reflection operation
     * that failed, while preserving all information from the original exception.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Class<?> clazz = Class.forName(className);
     *     Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
     *     constructor.setAccessible(true);
     *     return constructor.newInstance(args);
     * } catch (ReflectiveOperationException e) {
     *     throw new UncheckedReflectiveOperationException(
     *         "Failed to instantiate class: " + className + " with args: " + Arrays.toString(args), e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the {@link ReflectiveOperationException} to wrap. Must not be {@code null}.
     *              This can be any subclass of ReflectiveOperationException.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedReflectiveOperationException(final String message, final ReflectiveOperationException cause) {
        super(message, cause);
    }
}
