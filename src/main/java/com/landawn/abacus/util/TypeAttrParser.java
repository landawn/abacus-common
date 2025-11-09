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

package com.landawn.abacus.util;

import static com.landawn.abacus.util.WD.COMMA;
import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.landawn.abacus.annotation.Internal;

/**
 * A parser for type attribute strings that extracts class names, generic type parameters,
 * and constructor parameters from complex type declarations. This utility class supports
 * parsing of nested generic types and constructor arguments in a format similar to Java
 * type declarations.
 *
 * <p>The parser handles three main components:
 * <ul>
 *   <li><b>Class name</b>: The base type name (e.g., "HashMap")</li>
 *   <li><b>Type parameters</b>: Generic type arguments in angle brackets (e.g., "&lt;String, Integer&gt;")</li>
 *   <li><b>Constructor parameters</b>: Arguments in parentheses (e.g., "(16, 0.75f)")</li>
 * </ul>
 *
 * <p>Example type attribute strings:
 * <ul>
 *   <li>{@code "String"} - Simple class name</li>
 *   <li>{@code "List<String>"} - Generic type with one parameter</li>
 *   <li>{@code "Map<String, List<Integer>>"} - Nested generic types</li>
 *   <li>{@code "HashMap<String, Object>(16, 0.75f)"} - Generic type with constructor args</li>
 * </ul>
 */
public final class TypeAttrParser {

    private final String className;

    private final String[] typeParameters;

    private final String[] parameters;

    /**
     * Private constructor used internally to create parser instances.
     * Use {@link #parse(String)} to create instances.
     *
     * @param className the parsed class name
     * @param typeParameters the parsed generic type parameters
     * @param parameters the parsed constructor parameters
     */
    private TypeAttrParser(final String className, final String[] typeParameters, final String[] parameters) {
        this.className = className;

        if (typeParameters == null) {
            this.typeParameters = new String[0];
        } else {
            this.typeParameters = typeParameters;
        }

        if (parameters == null) {
            this.parameters = new String[0];
        } else {
            this.parameters = parameters;
        }
    }

    /**
     * Returns the parsed class name without generic type parameters or constructor arguments.
     * For example, parsing {@code "HashMap<String, Integer>(16)"} returns {@code "HashMap"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("ArrayList<String>(10)");
     * String name = parser.getClassName(); // Returns "ArrayList"
     * }</pre>
     *
     * @return the class name portion of the parsed type attribute
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the parsed generic type parameters as an array of strings.
     * Each parameter is trimmed of whitespace. Returns an empty array if no
     * type parameters were present.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("Map<String, List<Integer>>");
     * String[] types = parser.getTypeParameters(); // Returns ["String", "List<Integer>"]
     * }</pre>
     *
     * @return an array of generic type parameter strings, never null
     */
    public String[] getTypeParameters() {
        return typeParameters;
    }

    /**
     * Returns the parsed constructor parameters as an array of strings.
     * Each parameter is trimmed of whitespace. Returns an empty array if no
     * constructor parameters were present.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("StringBuilder(100)");
     * String[] params = parser.getParameters(); // Returns ["100"]
     *
     * parser = TypeAttrParser.parse("HashMap(16, 0.75f)");
     * params = parser.getParameters(); // Returns ["16", "0.75f"]
     * }</pre>
     *
     * @return an array of constructor parameter strings, never null
     */
    public String[] getParameters() {
        return parameters;
    }

    /**
     * Parses a type attribute string into its component parts: class name,
     * generic type parameters, and constructor parameters. This method handles
     * nested generic types and properly balances angle brackets.
     *
     * <p>The parser recognizes:
     * <ul>
     *   <li>Generic type parameters enclosed in angle brackets: {@code <...>}</li>
     *   <li>Constructor parameters enclosed in parentheses: {@code (...)}</li>
     *   <li>Nested generics with proper bracket matching</li>
     *   <li>Comma-separated lists in both contexts</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple class
     * TypeAttrParser p1 = TypeAttrParser.parse("String");
     *
     * // Generic type
     * TypeAttrParser p2 = TypeAttrParser.parse("List<String>");
     *
     * // Nested generics
     * TypeAttrParser p3 = TypeAttrParser.parse("Map<String, List<Integer>>");
     *
     * // With constructor parameters
     * TypeAttrParser p4 = TypeAttrParser.parse("HashMap<K, V>(16, 0.75f)");
     * }</pre>
     *
     * @param attr the type attribute string to parse
     * @return a TypeAttrParser instance containing the parsed components
     * @throws StringIndexOutOfBoundsException if the string format is invalid
     */
    public static TypeAttrParser parse(final String attr) {
        String className = null;
        String[] typeParameters = null;
        String[] parameters = null;

        int beginIndex = attr.indexOf('<');
        if (beginIndex >= 0) {
            final int endIndex = attr.lastIndexOf('>');

            className = attr.substring(0, beginIndex).trim();
            final List<String> typeParameterList = new ArrayList<>();

            int bracketNum = 0;

            for (int idx = beginIndex + 1, previousIndex = idx; idx < endIndex; idx++) {
                final char ch = attr.charAt(idx);

                if (ch == '<') {
                    bracketNum++;

                    continue;
                }

                if ((bracketNum > 0) && (ch == '>')) {
                    bracketNum--;
                }

                if (bracketNum == 0 && (ch == ',' || idx == endIndex - 1)) {
                    typeParameterList.add(Strings.trim(ch == ',' ? attr.substring(previousIndex, idx) : attr.substring(previousIndex, idx + 1)));

                    previousIndex = idx + 1;
                }
            }

            typeParameters = typeParameterList.toArray(new String[0]);

            beginIndex = endIndex;
        }

        beginIndex = attr.indexOf(_PARENTHESES_L, N.max(0, beginIndex));

        if (beginIndex >= 0) {
            if (className == null) {
                className = attr.substring(0, beginIndex);
            }

            final String str = attr.substring(beginIndex + 1, attr.lastIndexOf(_PARENTHESES_R)).trim();
            parameters = str.isEmpty() ? N.EMPTY_STRING_ARRAY : (COMMA.equals(str) ? new String[] { COMMA } : CSVUtil.CSV_HEADER_PARSER.apply(str));
        }

        if (className == null) {
            className = attr;
        }

        return new TypeAttrParser(className, typeParameters, parameters);
    }

    /**
     * Creates a new instance of a class based on the parsed type attribute string.
     * This internal method uses reflection to instantiate the specified class with
     * the appropriate constructor parameters.
     *
     * <p>The method attempts to find a constructor that matches the combined type
     * parameters and constructor parameters. If no exact match is found, it tries
     * alternative constructor signatures, including one that accepts type parameters
     * as individual strings and constructor parameters as a string array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Creates a HashMap with initial capacity 16 and load factor 0.75
     * HashMap<String, Integer> map = TypeAttrParser.newInstance(null, "HashMap<String, Integer>(16, 0.75f)");
     * }</pre>
     *
     * @param <T> the type of object to create
     * @param cls the class to instantiate, or {@code null} to derive from the attribute string
     * @param attr the type attribute string containing class name and constructor parameters
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if no suitable constructor is found
     * @throws RuntimeException if instantiation fails
     */
    @SuppressWarnings("unchecked")
    @Internal
    static <T> T newInstance(Class<?> cls, final String attr) {
        final TypeAttrParser attrResult = TypeAttrParser.parse(attr);
        final String className = attrResult.getClassName();
        final String[] attrTypeParameters = attrResult.getTypeParameters();
        final String[] attrParameters = attrResult.getParameters();

        if (cls == null) {
            cls = ClassUtil.forClass(className);
        }

        int parameterLength = attrTypeParameters.length + attrParameters.length;

        if (parameterLength > 0) {
            Class<?>[] parameterTypes = new Class[parameterLength];
            Object[] parameters = new Object[parameterLength];

            for (int i = 0; i < attrTypeParameters.length; i++) {
                parameterTypes[i] = String.class;
                parameters[i] = attrTypeParameters[i];
            }

            for (int i = 0; i < attrParameters.length; i++) {
                parameterTypes[i + attrTypeParameters.length] = String.class;
                parameters[i + attrTypeParameters.length] = attrParameters[i];
            }

            Constructor<?> constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);

            if (constructor == null) {
                parameterLength = attrTypeParameters.length + ((attrParameters.length > 1) ? 1 : 0);

                if (parameterLength > 0) {
                    parameterTypes = new Class[parameterLength];
                    parameters = new Object[parameterLength];

                    for (int i = 0; i < attrTypeParameters.length; i++) {
                        parameterTypes[i] = String.class;
                        parameters[i] = attrTypeParameters[i];
                    }

                    if (attrParameters.length > 1) {
                        parameterTypes[parameterTypes.length - 1] = String[].class;
                        parameters[parameters.length - 1] = attrParameters;
                    }
                }

                constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);
            }

            if (constructor == null) {
                throw new IllegalArgumentException(
                        "No constructor found with parameters: " + N.toString(parameterTypes) + ". in class: " + cls.getCanonicalName());
            }

            ClassUtil.setAccessibleQuietly(constructor, true);

            return (T) ClassUtil.invokeConstructor(constructor, parameters);
        } else {
            return (T) N.newInstance(cls);
        }
    }

    /**
     * Reflectively creates a new instance described by the type attribute plus explicit constructor arguments.
     * The attr string may include generic type parameters. Explicit args are passed as pairs of Class and Object.
     * These pairs are prepended to parsed type parameters and constructor parameters (all treated as String)
     * to build a candidate constructor signature. If no exact match exists and there are multiple constructor
     * parameters from attr, a fallback tries replacing those parameters with a single String[] parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create HashMap with type parameters and constructor args
     * TypeAttrParser.newInstance(null, "HashMap<String, Integer>(16, 0.75f)");
     *
     * // Create custom class with additional explicit arguments
     * TypeAttrParser.newInstance(MyClass.class, "MyClass<A, B>(x, y)", String.class, "extra");
     * }</pre>
     *
     * @param <T> the type of object to create
     * @param cls the target class to instantiate, or {@code null} to derive from attr
     * @param attr the type attribute string with optional generics and constructor params
     * @param args alternating pairs of Class and value (must be even length)
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if no matching constructor is found
     * @throws RuntimeException if instantiation fails
     */
    public static <T> T newInstance(Class<?> cls, final String attr, final Object... args) {
        final TypeAttrParser attrResult = TypeAttrParser.parse(attr);
        final String className = attrResult.getClassName();
        final String[] attrTypeParameters = attrResult.getTypeParameters();
        final String[] attrParameters = attrResult.getParameters();

        if (cls == null) {
            cls = ClassUtil.forClass(className);
        }

        int parameterLength = attrTypeParameters.length + attrParameters.length + (args.length / 2);

        if (parameterLength > 0) {
            Class<?>[] parameterTypes = new Class[parameterLength];
            Object[] parameters = new Object[parameterLength];

            for (int i = 0; i < args.length; i++) {
                parameterTypes[i / 2] = (Class<?>) args[i];
                parameters[i / 2] = args[++i];
            }

            for (int i = 0; i < attrTypeParameters.length; i++) {
                parameterTypes[i + (args.length / 2)] = String.class;
                parameters[i + (args.length / 2)] = attrTypeParameters[i];
            }

            for (int i = 0; i < attrParameters.length; i++) {
                parameterTypes[i + (args.length / 2) + attrTypeParameters.length] = String.class;
                parameters[i + (args.length / 2) + attrTypeParameters.length] = attrParameters[i];
            }

            Constructor<?> constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);

            if (constructor == null) {
                parameterLength = attrTypeParameters.length + ((attrParameters.length > 1) ? 1 : 0) + (args.length / 2);

                if (parameterLength > 0) {
                    parameterTypes = new Class[parameterLength];
                    parameters = new Object[parameterLength];

                    for (int i = 0; i < args.length; i++) {
                        parameterTypes[i / 2] = (Class<?>) args[i];
                        parameters[i / 2] = args[++i];
                    }

                    for (int i = 0; i < attrTypeParameters.length; i++) {
                        parameterTypes[i + (args.length / 2)] = String.class;
                        parameters[i + (args.length / 2)] = attrTypeParameters[i];
                    }

                    if (attrParameters.length > 1) {
                        parameterTypes[parameterTypes.length - 1] = String[].class;
                        parameters[parameters.length - 1] = attrParameters;
                    }
                }

                constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);
            }

            if (constructor == null) {
                throw new IllegalArgumentException(
                        "No constructor found with parameters: " + N.toString(parameterTypes) + ". in class: " + cls.getCanonicalName());
            }

            ClassUtil.setAccessibleQuietly(constructor, true);

            return (T) ClassUtil.invokeConstructor(constructor, parameters);
        } else {
            return (T) N.newInstance(cls);
        }
    }

    /**
     * Returns a string representation of this parser instance showing all parsed components.
     * The format is: {@code {className=X, typeParameters=[...], parameters=[...]}}.
     *
     * <p>Example output:
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("HashMap<K, V>(16, 0.75f)");
     * System.out.println(parser);
     * // Output: {className=HashMap, typeParameters=[K, V], parameters=[16, 0.75f]}
     * }</pre>
     *
     * @return a string representation of the parsed components
     */
    @Override
    public String toString() {
        return "{className=" + className + ", typeParameters=" + Arrays.toString(typeParameters) + ", parameters=" + Arrays.toString(parameters) + "}";
    }
}
