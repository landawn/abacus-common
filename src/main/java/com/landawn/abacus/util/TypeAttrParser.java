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

import static com.landawn.abacus.util.SK.COMMA;
import static com.landawn.abacus.util.SK._PARENTHESIS_L;
import static com.landawn.abacus.util.SK._PARENTHESIS_R;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.exception.ParsingException;

/**
 * A parser for type attribute strings that extracts class names, generic type parameters,
 * and constructor parameters from complex type declarations. This utility class supports
 * parsing of nested generic types and constructor arguments in a format similar to Java
 * type declarations. Parsing is syntactic: names are not resolved and constructor arguments
 * remain strings.
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
 *   <li>{@code "Owner<String>.Member<Integer>"} - Member type with a parameterized owner</li>
 *   <li>{@code "HashMap<String, Object>(16, 0.75f)"} - Generic type with constructor args</li>
 * </ul>
 */
public final class TypeAttrParser {
    // Type-attribute syntax has its own backslash escape grammar, independent of the CSV default dialect.
    private static final CsvParser ARGUMENT_PARSER = new CsvParser(',', '"', '\\');

    // parse() re-enters itself once per generic nesting level, once per array suffix, and twice per
    // parameterized qualified-member segment, so hostile input would otherwise raise a StackOverflowError
    // (around 6000 levels on a default stack, around 400 on a 256k one) instead of the documented
    // IllegalArgumentException. The cap therefore counts re-entries, not textual levels: a repeated
    // A<...>[] or Owner<...>.Member costs two per level and trips at 32. No real declaration comes close
    // to either figure - the deepest generic nesting in this project is 2.
    private static final int MAX_NESTING_DEPTH = 64;

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
     * String name = parser.getClassName();   // returns "ArrayList"
     * }</pre>
     *
     * @return the class name portion of the parsed type attribute
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns a copy of the parsed generic type parameters as an array of strings.
     * Each parameter is trimmed of whitespace. Returns an empty array if no
     * type parameters were present.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("Map<String, List<Integer>>");
     * String[] types = parser.getTypeParameters();   // returns ["String", "List<Integer>"]
     * }</pre>
     *
     * @return an array of generic type parameter strings, never null
     */
    public String[] getTypeParameters() {
        return typeParameters.clone();
    }

    /**
     * Returns a copy of the parsed constructor parameters as an array of strings.
     * The parenthesized argument list is parsed as CSV, so surrounding whitespace is stripped from
     * each unquoted argument while whitespace inside a double-quoted argument is preserved.
     * Returns an empty array if no constructor parameters were present.
     *
     * <p><b>Special case:</b> an argument list that is a single comma once the surrounding whitespace is
     * stripped ({@code Foo(,)}, {@code Foo( , )}) yields the single argument {@code ","} rather than the two
     * empty arguments plain CSV would give, so a comma delimiter can be written without quoting;
     * {@code Foo(",")} is the explicit equivalent. No other all-empty list is special-cased:
     * {@code Foo(,,)} yields three empty arguments.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TypeAttrParser parser = TypeAttrParser.parse("StringBuilder(100)");
     * String[] params = parser.getParameters();   // returns ["100"]
     *
     * parser = TypeAttrParser.parse("HashMap(16, 0.75f)");
     * params = parser.getParameters();   // returns ["16", "0.75f"]
     * }</pre>
     *
     * @return an array of constructor parameter strings, never null
     */
    public String[] getParameters() {
        return parameters.clone();
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
     *   <li>Array dimensions after a generic declaration, including nested generic arrays</li>
     *   <li>Qualified member types whose owner segments are parameterized</li>
     *   <li>Comma-separated lists in both contexts</li>
     *   <li>Double-quoted CSV constructor arguments whose commas, parentheses, or angle
     *       brackets are data rather than outer type delimiters. Backslash-escaped and
     *       doubled double quotes are recognized consistently with {@link CsvParser}.</li>
     * </ul>
     *
     * <p>An unescaped double quote inside an otherwise <i>unquoted</i> argument is plain data to
     * {@link CsvParser}, but this parser always reads it as opening a quoted region, so such an argument may
     * be rejected as unbalanced: {@code Foo(bc"d"ef)} parses while {@code Foo(a"b)} does not.</p>
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
     * @return a {@code TypeAttrParser} instance containing the parsed components; the returned
     *         instance never has {@code null} type-parameter or constructor-parameter arrays
     * @throws IllegalArgumentException if {@code attr} is {@code null}, or if the class name (ignoring any trailing array brackets) or a generic
     *         parameter at any nesting level is empty, if generic angle brackets, constructor parentheses, or
     *         quoted constructor arguments are missing, unbalanced, out of order, or otherwise malformed, or if
     *         the declaration needs more than 64 levels of recursive parsing. That limit counts parsing levels
     *         rather than textual nesting levels: a generic nesting level costs one level and so does an array
     *         suffix, while a parameterized qualified-member segment costs two (its owner arguments are
     *         validated, then the normalized name is re-parsed). A declaration that repeats one of the
     *         two-level shapes, {@code A<...>[]} or {@code Owner<...>.Member}, is therefore rejected beyond 32
     *         textual levels. Array dimensions on their own, such as {@code int[][][]}, do not recurse per
     *         dimension and are not limited at all.
     * @see #getClassName()
     * @see #getTypeParameters()
     * @see #getParameters()
     */
    public static TypeAttrParser parse(final String attr) throws IllegalArgumentException {
        N.checkArgNotNull(attr, cs.attr);

        return parse(attr, 0, attr);
    }

    /**
     * Parses {@code attr} at the given nesting depth. Every re-entry passes {@code depth + 1}, so a
     * pathologically nested declaration is rejected with the documented {@code IllegalArgumentException}
     * rather than overflowing the stack. {@code root} is the declaration the caller handed to
     * {@link #parse(String)}; it is what the depth-guard message names, because {@code attr} at the depth the
     * guard trips is an inner fragment that is not itself deeply nested.
     *
     * @throws IllegalArgumentException if {@code depth} exceeds the parsing limit or {@code attr} has malformed type syntax
     */
    private static TypeAttrParser parse(final String attr, final int depth, final String root) throws IllegalArgumentException {
        if (depth > MAX_NESTING_DEPTH) {
            throw new IllegalArgumentException("Malformed type attribute: nesting deeper than " + MAX_NESTING_DEPTH + " levels in: " + root);
        }

        int componentEnd = attr.length();
        while (componentEnd > 0 && Character.isWhitespace(attr.charAt(componentEnd - 1))) {
            componentEnd--;
        }

        final int arrayEnd = componentEnd;
        while (componentEnd >= 2 && attr.charAt(componentEnd - 2) == '[' && attr.charAt(componentEnd - 1) == ']') {
            componentEnd -= 2;
        }

        if (componentEnd < arrayEnd) {
            final String componentName = attr.substring(0, componentEnd).trim();
            if (componentName.endsWith(")")) {
                throw new IllegalArgumentException("Malformed type attribute: array dimensions after constructor arguments in: " + attr);
            }

            final TypeAttrParser component = parse(componentName, depth + 1, root);
            return new TypeAttrParser(component.className + attr.substring(componentEnd, arrayEnd), component.typeParameters, component.parameters);
        }

        final String normalizedMemberType = normalizeQualifiedMemberType(attr, depth, root);

        if (normalizedMemberType != null) {
            return parse(normalizedMemberType, depth + 1, root);
        }

        String className = null;
        String[] typeParameters = null;
        String[] parameters = null;

        final int firstParenthesisIndex = attr.indexOf(_PARENTHESIS_L);
        final int classSyntaxEndIndex = firstParenthesisIndex < 0 ? attr.length() : firstParenthesisIndex;
        int beginIndex = attr.substring(0, classSyntaxEndIndex).indexOf('<');
        final int firstClosingGenericIndex = attr.substring(0, classSyntaxEndIndex).indexOf('>');

        if (firstClosingGenericIndex >= 0 && (beginIndex < 0 || firstClosingGenericIndex < beginIndex)) {
            throw new IllegalArgumentException("Malformed type attribute: unexpected closing '>' in: " + attr);
        }

        final int firstClosingParenthesisIndex = attr.indexOf(_PARENTHESIS_R);

        if (firstClosingParenthesisIndex >= 0 && (firstParenthesisIndex < 0 || firstClosingParenthesisIndex < firstParenthesisIndex)) {
            throw new IllegalArgumentException("Malformed type attribute: unexpected closing ')' in: " + attr);
        }

        if (beginIndex >= 0) {
            final int endIndex = findClosingGeneric(attr, beginIndex);

            className = attr.substring(0, beginIndex).trim();
            final List<String> typeParameterList = new ArrayList<>();

            int bracketNum = 0;
            int parenthesisDepth = 0;
            boolean inQuotes = false;
            int previousIndex = beginIndex + 1;

            for (int idx = previousIndex; idx < endIndex; idx++) {
                final char ch = attr.charAt(idx);

                if (inQuotes) {
                    if (ch == SK._BACKSLASH && idx + 1 < endIndex && (attr.charAt(idx + 1) == SK._DOUBLE_QUOTE || attr.charAt(idx + 1) == SK._BACKSLASH)) {
                        idx++;
                    } else if (ch == SK._DOUBLE_QUOTE) {
                        if (idx + 1 < endIndex && attr.charAt(idx + 1) == SK._DOUBLE_QUOTE) {
                            idx++;
                        } else {
                            inQuotes = false;
                        }
                    }

                    continue;
                }

                if (ch == SK._DOUBLE_QUOTE) {
                    inQuotes = true;
                } else if (ch == _PARENTHESIS_L) {
                    parenthesisDepth++;
                } else if (ch == _PARENTHESIS_R) {
                    if (parenthesisDepth == 0) {
                        throw new IllegalArgumentException("Malformed type attribute: unexpected closing ')' in: " + attr);
                    }

                    parenthesisDepth--;
                } else if (parenthesisDepth > 0) {
                    // Constructor arguments belonging to a nested type are opaque here. In
                    // particular, their commas must not split the outer generic parameter list.
                    continue;
                } else if (ch == '<') {
                    bracketNum++;
                } else if (ch == '>') {
                    if (bracketNum > 0) {
                        bracketNum--;
                    } else {
                        throw new IllegalArgumentException("Malformed type attribute: unexpected closing '>' in: " + attr);
                    }
                } else if (bracketNum == 0 && ch == ',') {
                    typeParameterList.add(Strings.trim(attr.substring(previousIndex, idx)));
                    previousIndex = idx + 1;
                }
            }

            if (bracketNum != 0 || parenthesisDepth != 0 || inQuotes) {
                throw new IllegalArgumentException("Malformed type attribute: unbalanced nested parameter syntax in: " + attr);
            }

            typeParameterList.add(Strings.trim(attr.substring(previousIndex, endIndex)));

            if (Strings.isEmpty(className)) {
                throw new IllegalArgumentException("Malformed type attribute: missing class name in: " + attr);
            }

            for (final String typeParameter : typeParameterList) {
                if (Strings.isEmpty(typeParameter)) {
                    throw new IllegalArgumentException("Malformed type attribute: empty generic parameter in: " + attr);
                }

                // Validate every nested declaration as well. Merely balancing the outer text is
                // insufficient for inputs such as List<Map<String,>> or Map<String, List<>>.
                parse(typeParameter, depth + 1, root);
            }

            typeParameters = typeParameterList.toArray(new String[0]);

            beginIndex = endIndex;
        }

        int endIndex = beginIndex;
        beginIndex = attr.indexOf(_PARENTHESIS_L, N.max(0, beginIndex));

        if (beginIndex >= 0) {
            if (className != null && endIndex >= 0 && N.notEmpty(attr.substring(endIndex + 1, beginIndex).trim())) {
                throw new IllegalArgumentException("Malformed type attribute: unexpected trailing text in: " + attr);
            }

            if (className == null) {
                className = attr.substring(0, beginIndex).trim();
            }

            endIndex = findClosingParenthesis(attr, beginIndex);

            final String str = attr.substring(beginIndex + 1, endIndex).trim();

            try {
                parameters = str.isEmpty() ? N.EMPTY_STRING_ARRAY : (COMMA.equals(str) ? new String[] { COMMA } : ARGUMENT_PARSER.parseLineToArray(str));
            } catch (final ParsingException e) {
                // The delimiter scanner above opens a quoted region on any '"', while the argument parser treats
                // a '"' inside an unquoted field as data, so a substring the scanner accepted can still be
                // malformed CSV. Report it as the malformed type attribute this method documents.
                throw new IllegalArgumentException("Malformed type attribute: malformed quoted constructor argument in: " + attr, e);
            }
        } else if (attr.indexOf(_PARENTHESIS_R, N.max(0, endIndex)) >= 0) {
            throw new IllegalArgumentException("Malformed type attribute: unexpected closing ')' in: " + attr);
        }

        if (endIndex >= 0 && N.notEmpty(attr.substring(endIndex + 1).trim())) {
            throw new IllegalArgumentException("Malformed type attribute: unexpected trailing text in: " + attr);
        }

        if (className == null) {
            className = attr.trim(); // the generics/constructor-paren paths above both trim; a bare name must too
        }

        if (Strings.isEmpty(withoutArrayBrackets(className))) {
            throw new IllegalArgumentException("Malformed type attribute: missing class name in: " + attr);
        }

        return new TypeAttrParser(className, typeParameters, parameters);
    }

    /**
     * Strips a trailing run of {@code []} pairs from a class name. Checking emptiness through this keeps the
     * component-less array rejection reachable from every path: the bare spelling {@code "[]"} is rejected by
     * the array branch, which peels the brackets and re-enters with an empty name, but {@code "[]()"} and
     * {@code "[]<A>"} never take that branch and would otherwise parse to the class name {@code "[]"}.
     */
    private static String withoutArrayBrackets(final String className) {
        int end = className.length();

        while (end >= 2 && className.charAt(end - 2) == '[' && className.charAt(end - 1) == ']') {
            end -= 2;
        }

        return end == className.length() ? className : className.substring(0, end).trim();
    }

    /**
     * Removes generic clauses from owner segments while retaining the final member's own generic
     * clause. For example, {@code Owner<String>.Member<Integer>} becomes
     * {@code Owner.Member<Integer>}. The original owner arguments are still validated before they
     * are removed; callers that need them retain them in the original reflection type or type name.
     */
    private static String normalizeQualifiedMemberType(final String attr, final int depth, final String root) {
        final int firstParenthesisIndex = attr.indexOf(_PARENTHESIS_L);
        final int classSyntaxEndIndex = firstParenthesisIndex < 0 ? attr.length() : firstParenthesisIndex;
        final int firstGenericStart = attr.substring(0, classSyntaxEndIndex).indexOf('<');

        if (firstGenericStart < 0) {
            return null;
        }

        final int firstGenericEnd = findClosingGeneric(attr, firstGenericStart);
        int cursor = skipWhitespace(attr, firstGenericEnd + 1);

        if (cursor >= attr.length() || (attr.charAt(cursor) != '.' && attr.charAt(cursor) != '$')) {
            return null;
        }

        final String ownerName = attr.substring(0, firstGenericStart).trim();

        if (Strings.isEmpty(ownerName)) {
            throw new IllegalArgumentException("Malformed type attribute: missing class name in: " + attr);
        }

        // Validate the owner's generic clause before removing it from the parser-facing name.
        parse(ownerName + attr.substring(firstGenericStart, firstGenericEnd + 1), depth + 1, root);

        final StringBuilder normalized = new StringBuilder(ownerName);

        while (cursor < attr.length() && (attr.charAt(cursor) == '.' || attr.charAt(cursor) == '$')) {
            cursor = skipWhitespace(attr, cursor + 1);
            final int memberNameStart = cursor;

            while (cursor < attr.length()) {
                final char ch = attr.charAt(cursor);

                if (ch == '<' || ch == '.' || ch == '$' || ch == _PARENTHESIS_L || ch == _PARENTHESIS_R || ch == '>' || Character.isWhitespace(ch)) {
                    break;
                }

                cursor++;
            }

            if (memberNameStart == cursor) {
                throw new IllegalArgumentException("Malformed type attribute: missing member class name in: " + attr);
            }

            final String memberName = attr.substring(memberNameStart, cursor);
            normalized.append('.').append(memberName);
            cursor = skipWhitespace(attr, cursor);

            if (cursor < attr.length() && attr.charAt(cursor) == '<') {
                final int genericEnd = findClosingGeneric(attr, cursor);
                final String genericClause = attr.substring(cursor, genericEnd + 1);
                cursor = skipWhitespace(attr, genericEnd + 1);

                if (cursor < attr.length() && (attr.charAt(cursor) == '.' || attr.charAt(cursor) == '$')) {
                    // This member is itself an owner. Validate its generic arguments, then continue
                    // with the next member segment without exposing those arguments as the final
                    // member's own type parameters.
                    parse(memberName + genericClause, depth + 1, root);
                    continue;
                }

                normalized.append(genericClause);
            }

            if (cursor == attr.length()) {
                return normalized.toString();
            }

            if (attr.charAt(cursor) == _PARENTHESIS_L) {
                return normalized.append(attr.substring(cursor)).toString();
            }

            if (attr.charAt(cursor) != '.' && attr.charAt(cursor) != '$') {
                throw new IllegalArgumentException("Malformed type attribute: unexpected trailing text in: " + attr);
            }
        }

        throw new IllegalArgumentException("Malformed type attribute: missing member class name in: " + attr);
    }

    private static int skipWhitespace(final String str, int index) {
        while (index < str.length() && Character.isWhitespace(str.charAt(index))) {
            index++;
        }

        return index;
    }

    /**
     * Finds the closing angle bracket paired with {@code beginIndex}. Angle brackets inside a
     * nested type's parenthesized, double-quoted CSV constructor arguments are treated as data.
     * Backslash-escaped and doubled double quotes follow the explicitly configured argument parser's rules.
     */
    private static int findClosingGeneric(final String attr, final int beginIndex) {
        int depth = 0;
        int parenthesisDepth = 0;
        boolean inQuotes = false;

        for (int i = beginIndex, len = attr.length(); i < len; i++) {
            final char ch = attr.charAt(i);

            if (inQuotes) {
                if (ch == SK._BACKSLASH && i + 1 < len && (attr.charAt(i + 1) == SK._DOUBLE_QUOTE || attr.charAt(i + 1) == SK._BACKSLASH)) {
                    i++;
                } else if (ch == SK._DOUBLE_QUOTE) {
                    if (i + 1 < len && attr.charAt(i + 1) == SK._DOUBLE_QUOTE) {
                        i++;
                    } else {
                        inQuotes = false;
                    }
                }

                continue;
            }

            if (ch == SK._DOUBLE_QUOTE) {
                inQuotes = true;
            } else if (ch == _PARENTHESIS_L) {
                parenthesisDepth++;
            } else if (ch == _PARENTHESIS_R) {
                if (parenthesisDepth == 0) {
                    throw new IllegalArgumentException("Malformed type attribute: unexpected closing ')' in: " + attr);
                }

                parenthesisDepth--;
            } else if (parenthesisDepth > 0) {
                continue;
            } else if (ch == '<') {
                depth++;
            } else if (ch == '>') {
                if (--depth == 0) {
                    return i;
                }

                if (depth < 0) {
                    break;
                }
            }
        }

        throw new IllegalArgumentException("Malformed type attribute: missing closing '>' in: " + attr);
    }

    /**
     * Finds the closing parenthesis paired with {@code beginIndex}. Parentheses inside double-quoted
     * CSV constructor arguments are treated as data, while balanced nested parentheses are allowed.
     * Backslash-escaped and doubled double quotes follow the explicitly configured argument parser's rules.
     */
    private static int findClosingParenthesis(final String attr, final int beginIndex) {
        int depth = 0;
        boolean inQuotes = false;

        for (int i = beginIndex, len = attr.length(); i < len; i++) {
            final char ch = attr.charAt(i);

            if (inQuotes) {
                if (ch == SK._BACKSLASH && i + 1 < len && (attr.charAt(i + 1) == SK._DOUBLE_QUOTE || attr.charAt(i + 1) == SK._BACKSLASH)) {
                    i++;
                } else if (ch == SK._DOUBLE_QUOTE) {
                    if (i + 1 < len && attr.charAt(i + 1) == SK._DOUBLE_QUOTE) {
                        i++;
                    } else {
                        inQuotes = false;
                    }
                }

                continue;
            }

            if (ch == SK._DOUBLE_QUOTE) {
                inQuotes = true;
            } else if (ch == _PARENTHESIS_L) {
                depth++;
            } else if (ch == _PARENTHESIS_R) {
                if (--depth == 0) {
                    return i;
                }

                if (depth < 0) {
                    break;
                }
            }
        }

        throw new IllegalArgumentException("Malformed type attribute: missing closing ')' in: " + attr);
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
     * // Resolve java.lang.StringBuilder and invoke its String constructor.
     * StringBuilder builder = TypeAttrParser.newInstance(
     *     null, "java.lang.StringBuilder(initial text)");
     * }</pre>
     *
     * <p>All parsed type parameters and constructor parameters are passed to the constructor
     * as {@code String} values, so a matching constructor must accept {@code String} (or, in the
     * fallback signature, a trailing {@code String[]}) arguments. If the attribute string declares
     * no type or constructor parameters, the no-argument constructor is used.
     *
     * <p>A non-null class token determines the result type. With a null token, the class name is resolved dynamically;
     * the caller is responsible for choosing a compatible result type.</p>
     *
     * @param <T> the type of object to create
     * @param cls the class to instantiate, or {@code null} to derive it from the class name
     *            in the attribute string
     * @param attr the type attribute string containing the class name and constructor parameters
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if {@code attr} is {@code null}, or if {@code attr} has malformed type syntax or no suitable constructor is found
     * @throws RuntimeException if the class cannot be resolved or instantiation fails
     * @see #parse(String)
     */
    @SuppressWarnings("unchecked")
    @Internal
    static <T> T newInstance(Class<T> cls, final String attr) throws IllegalArgumentException, RuntimeException {
        final TypeAttrParser attrResult = TypeAttrParser.parse(attr);
        final String className = attrResult.getClassName();
        final String[] attrTypeParameters = attrResult.getTypeParameters();
        final String[] attrParameters = attrResult.getParameters();

        if (cls == null) {
            cls = ClassUtil.forName(className);
        }

        int parameterLength = attrTypeParameters.length + attrParameters.length;

        if (parameterLength > 0) {
            Class<?>[] parameterTypes = new Class<?>[parameterLength];
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
            // Keep the signature that was tried FIRST: the fallback below overwrites parameterTypes, and a
            // failure message naming only the String[] fallback hides the arity the caller actually wrote.
            final Class<?>[] primaryParameterTypes = parameterTypes;

            if (constructor == null && attrParameters.length > 0) {
                parameterLength = attrTypeParameters.length + 1;

                if (parameterLength > 0) {
                    parameterTypes = new Class<?>[parameterLength];
                    parameters = new Object[parameterLength];

                    for (int i = 0; i < attrTypeParameters.length; i++) {
                        parameterTypes[i] = String.class;
                        parameters[i] = attrTypeParameters[i];
                    }

                    if (attrParameters.length > 0) {
                        parameterTypes[parameterTypes.length - 1] = String[].class;
                        parameters[parameters.length - 1] = attrParameters;
                    }
                }

                constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);
            }

            if (constructor == null) {
                throw new IllegalArgumentException("No constructor found with parameters: " + N.toString(primaryParameterTypes)
                        + (parameterTypes == primaryParameterTypes ? "" : " or " + N.toString(parameterTypes)) + ". in class: " + cls.getCanonicalName());
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
     * to build a candidate constructor signature. If no exact match exists and {@code attr} contains any
     * constructor parameters, a fallback tries replacing those parameters with a single {@code String[]} parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select StringBuilder(CharSequence) with one explicit (Class, value) pair.
     * StringBuilder builder = TypeAttrParser.newInstance(
     *     StringBuilder.class, "StringBuilder", CharSequence.class, "initial text");
     * }</pre>
     *
     * <p>A non-null class token determines the result type. With a null token, the class name is resolved dynamically;
     * the caller is responsible for choosing a compatible result type.</p>
     *
     * @param <T> the type of object to create
     * @param cls the target class to instantiate, or {@code null} to derive it from the
     *            class name in {@code attr}
     * @param attr the type attribute string with optional generics and constructor params
     * @param args alternating {@code (Class, value)} pairs prepended to the parsed parameters;
     *             must have an even length, with every even-indexed element being a {@code Class}
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if {@code args} or {@code attr} is {@code null}, {@code attr} has malformed type syntax,
     *         {@code args} has an odd length or an even-indexed element that is not a {@code Class}, or no matching constructor is found
     * @throws RuntimeException if the class cannot be resolved or instantiation fails
     * @see #parse(String)
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> cls, final String attr, final Object... args) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(args, cs.args);

        final TypeAttrParser attrResult = TypeAttrParser.parse(attr);
        final String className = attrResult.getClassName();
        final String[] attrTypeParameters = attrResult.getTypeParameters();
        final String[] attrParameters = attrResult.getParameters();

        if (cls == null) {
            cls = ClassUtil.forName(className);
        }

        if ((args.length & 1) != 0) {
            throw new IllegalArgumentException("The specified args must be [Class, value] pairs, but length is: " + args.length);
        }

        for (int i = 0; i < args.length; i += 2) {
            if (!(args[i] instanceof Class<?>)) {
                throw new IllegalArgumentException("The arg at index " + i + " must be a Class, but was: " + N.toString(args[i]));
            }
        }

        int parameterLength = attrTypeParameters.length + attrParameters.length + (args.length / 2);

        if (parameterLength > 0) {
            Class<?>[] parameterTypes = new Class<?>[parameterLength];
            Object[] parameters = new Object[parameterLength];

            for (int i = 0; i < args.length; i += 2) {
                parameterTypes[i / 2] = (Class<?>) args[i];
                parameters[i / 2] = args[i + 1];
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
            // Keep the signature that was tried FIRST: the fallback below overwrites parameterTypes, and a
            // failure message naming only the String[] fallback hides the arity the caller actually wrote.
            final Class<?>[] primaryParameterTypes = parameterTypes;

            if (constructor == null && attrParameters.length > 0) {
                parameterLength = attrTypeParameters.length + 1 + (args.length / 2);

                if (parameterLength > 0) {
                    parameterTypes = new Class<?>[parameterLength];
                    parameters = new Object[parameterLength];

                    for (int i = 0; i < args.length; i += 2) {
                        parameterTypes[i / 2] = (Class<?>) args[i];
                        parameters[i / 2] = args[i + 1];
                    }

                    for (int i = 0; i < attrTypeParameters.length; i++) {
                        parameterTypes[i + (args.length / 2)] = String.class;
                        parameters[i + (args.length / 2)] = attrTypeParameters[i];
                    }

                    if (attrParameters.length > 0) {
                        parameterTypes[parameterTypes.length - 1] = String[].class;
                        parameters[parameters.length - 1] = attrParameters;
                    }
                }

                constructor = ClassUtil.getDeclaredConstructor(cls, parameterTypes);
            }

            if (constructor == null) {
                throw new IllegalArgumentException("No constructor found with parameters: " + N.toString(primaryParameterTypes)
                        + (parameterTypes == primaryParameterTypes ? "" : " or " + N.toString(parameterTypes)) + ". in class: " + cls.getCanonicalName());
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
