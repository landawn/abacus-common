/*
 * Copyright (c) 2021, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.stream.Stream.StreamEx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Utilities for generating property-name table interfaces from entity classes.
 *
 * <p>The generated source defines constants for bean properties so callers can avoid hard-coded
 * string literals when building queries, projections, or sort clauses.
 *
 * <p>This utility supports:
 * <ul>
 *   <li>Generating an inner interface for a single entity (typically {@value #X})</li>
 *   <li>Generating a standalone interface for multiple entities</li>
 *   <li>Optional variants for snake case, screaming snake case, and function-based names</li>
 *   <li>Optional write-back to source files</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Generate an inner interface for one entity
 * String innerCode = CodeGenerationUtil.generatePropNameTableClass(User.class);
 *
 * // Generate a standalone interface for multiple entities
 * List<Class<?>> entities = Arrays.asList(User.class, Order.class, Product.class);
 * String standaloneCode = CodeGenerationUtil.generatePropNameTableClasses(entities, "Props");
 * }</pre>
 *
 * <p>To include extra fields that are not declared directly on the target entities, provide a
 * synthetic helper type (e.g. {@code DummyEntity}) in {@code entityClasses}, or add shared members through
 * {@link PropNameTableCodeConfig#extendedInterfaces}.
 */
public final class CodeGenerationUtil {

    //    If by “english directory” you mean the English dictionary (i.e., all English words), then there is no single exact number—it depends heavily on which dictionary and what you count as a word (inflections, archaic words, technical terms, proper nouns, etc.).
    //
    //    That said, here are widely cited approximate ranges based on large modern dictionaries (Oxford / Merriam-Webster–scale):
    //
    //    Approximate counts (modern English)
    //    Starting letter Approx. number of words
    //    S   ≈ 70,000 – 80,000
    //    X   ≈ 300 – 500
    //    Why the difference is so extreme
    //
    //    S
    //
    //    Extremely productive letter in English
    //
    //    Used for:
    //
    //    plurals (s, es)
    //
    //    verbs (see, say, stand, …)
    //
    //    prefixes (sub-, super-, semi-, self-, syn-)
    //
    //    Massive Latin + Germanic overlap
    //
    //    X
    //
    //    Rare initial letter in native English
    //
    //    Mostly comes from:
    //
    //    Greek (xeno-, xyl-)
    //
    //    Modern borrowings (x-ray, xenophobia)
    //
    //    Almost no inflectional families
    //
    //    Order-of-magnitude takeaway
    //
    //    Words starting with “s” outnumber “x” by ~200×
    //
    //    Roughly:
    //
    //    1 out of every 10 English words starts with “s”
    //    1 out of every 2,000 starts with “x”

    /** Default inner interface name for single-entity property constants. */
    public static final String X = "x";

    /** Conventional class name for standalone property-name tables. */
    public static final String S = "s";

    /** Default nested interface name for snake_case property constants. */
    public static final String SL = "sl";

    /** Default nested interface name for SCREAMING_SNAKE_CASE property constants. */
    public static final String SU = "su";

    /** Default nested interface name for function-based property constants. */
    public static final String SF = "sf";

    /**
     * Built-in formatter for {@code min(property)} expressions.
     *
     * <p>Returns {@code null} for non-{@link Comparable} property types so those properties are
     * skipped during function-name generation.
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MIN_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "min(" + propName + ")";
        }

        return null;
    };

    /**
     * Built-in formatter for {@code max(property)} expressions.
     *
     * <p>Returns {@code null} for non-{@link Comparable} property types so those properties are
     * skipped during function-name generation.
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MAX_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "max(" + propName + ")";
        }

        return null;
    };

    private static final String INDENTATION = "    ";

    private static final String BUILDER = "Builder";

    private static final String LINE_SEPARATOR = IOUtil.LINE_SEPARATOR_UNIX;

    /** Identity property name converter that returns property names unchanged. */
    private static final BiFunction<Class<?>, String, String> identityPropNameConverter = (cls, propName) -> propName;

    /** Comment string used to suppress SonarQube warnings. */
    public static final String NOSONAR_COMMENTS = " // NOSONAR";

    private CodeGenerationUtil() {
        // singleton for utility class.
    }

    /**
     * Generates source for an inner property-name interface in the target entity.
     *
     * <p>The generated interface name defaults to {@value #X}.
     *
     * @param entityClass the entity class that contributes bean property names
     * @return generated Java source that declares the inner interface and constants
     * @see #generatePropNameTableClass(Class, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass) {
        return generatePropNameTableClass(entityClass, X);
    }

    /**
     * Generates source for an inner property-name interface with a custom interface name.
     *
     * @param entityClass the entity class that contributes bean property names
     * @param propNameTableClassName interface name for generated constants
     * @return generated Java source that declares the inner interface and constants
     * @see #generatePropNameTableClass(Class, String, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName) {
        return generatePropNameTableClass(entityClass, propNameTableClassName, null);
    }

    /**
     * Generates source for an inner property-name interface and optionally writes it back to disk.
     *
     * <p>When {@code srcDir} is not empty, this method loads the entity source file, replaces a
     * previously generated interface with the same name (if present), and inserts the new interface
     * before the entity's closing brace.
     *
     * @param entityClass the entity class that contributes bean property names
     * @param propNameTableClassName interface name for generated constants
     * @param srcDir source root directory; if {@code null} or empty, source is not written
     * @return generated Java source that declares the inner interface and constants
     * @throws RuntimeException if writing the modified source file fails
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName, final String srcDir) {
        final StringBuilder sb = new StringBuilder();

        final String interfaceName = "public interface " + propNameTableClassName;

        sb.append(LINE_SEPARATOR)
                .append("    /**")
                .append(LINE_SEPARATOR)
                .append("     * Auto-generated class for property(field) name table.")
                .append(LINE_SEPARATOR)
                .append("     */");

        //    if (Character.isLowerCase(propNameTableClassName.charAt(0))) {
        //        sb.append(LINE_SEPARATOR).append("    @SuppressWarnings(\"java:S1192\")");
        //    }

        sb.append(LINE_SEPARATOR)
                .append("    ")
                .append(interfaceName)
                .append(" {")
                .append(Character.isLowerCase(propNameTableClassName.charAt(0)) ? NOSONAR_COMMENTS : "")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR); //

        for (final String propName : Beans.getPropNameList(entityClass)) {

            sb.append("        /** Property(field) name {@code \"")
                    .append(propName)
                    .append("\"} */")
                    .append(LINE_SEPARATOR)
                    .append("        String ")
                    .append(propName)
                    .append(" = \"")
                    .append(propName)
                    .append("\";")
                    .append(LINE_SEPARATOR)
                    .append(LINE_SEPARATOR);
        }

        sb.append("    }").append(LINE_SEPARATOR);

        final String ret = sb.toString();

        if (Strings.isNotEmpty(srcDir)) {

            String packageDir = srcDir;
            final String packageName = ClassUtil.getPackageName(entityClass);

            if (Strings.isNotEmpty(packageName)) {
                if (!(packageDir.endsWith("/") || packageDir.endsWith("\\"))) {
                    packageDir += "/";
                }

                packageDir += Strings.replaceAll(packageName, ".", "/");
            }

            final File file = new File(packageDir + IOUtil.DIR_SEPARATOR + ClassUtil.getSimpleClassName(entityClass) + ".java");
            final List<String> lines = IOUtil.readAllLines(file);

            for (int i = 0, size = lines.size(); i < size; i++) {
                if (Strings.startsWithAny(lines.get(i).trim(), interfaceName, "* Auto-generated class for property(field) name table")) {
                    if (Strings.startsWith(lines.get(i).trim(), "* Auto-generated class for property(field) name table")) {
                        i--;
                    }

                    for (int j = i; j < size; j++) {
                        if ("}".equals(Strings.strip(lines.get(j)))) {
                            N.deleteRange(lines, Strings.isBlank(lines.get(i - 1)) ? i - 1 : i, Strings.isBlank(lines.get(j + 1)) ? j + 2 : j + 1);
                            break;
                        }
                    }

                    break;
                }
            }

            for (int i = lines.size() - 1; i > 0; i--) {
                if ("}".equals(Strings.strip(lines.get(i)))) {
                    lines.add(i, ret);
                    break;
                }
            }

            try {
                IOUtil.writeLines(lines, file);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return ret;

    }

    /**
     * Generates a standalone property-name table for multiple entity classes.
     *
     * <p>The generated top-level interface name defaults to {@value #X} for compatibility.
     *
     * @param entityClasses entity classes that contribute bean property names
     * @return generated Java source for the standalone property-name table
     * @see #generatePropNameTableClasses(Collection, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses) {
        return generatePropNameTableClasses(entityClasses, X);
    }

    /**
     * Generates a standalone property-name table for multiple entity classes with a custom name.
     *
     * @param entityClasses entity classes that contribute bean property names
     * @param propNameTableClassName top-level interface name to generate
     * @return generated Java source for the standalone property-name table
     * @see #generatePropNameTableClasses(Collection, String, String, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName) {
        return generatePropNameTableClasses(entityClasses, propNameTableClassName, null, null);
    }

    /**
     * Generates a standalone property-name table for multiple entities with package/file options.
     *
     * @param entityClasses entity classes that contribute bean property names
     * @param propNameTableClassName top-level interface name to generate
     * @param propNameTableClassPackageName package for generated source; if empty, uses the first
     *        entity package
     * @param srcDir source root directory; if {@code null} or empty, source is not written
     * @return generated Java source for the standalone property-name table
     * @throws RuntimeException if writing the generated file fails
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName,
            final String propNameTableClassPackageName, final String srcDir) {

        final PropNameTableCodeConfig codeConfig = PropNameTableCodeConfig.builder()
                .entityClasses(entityClasses)
                .className(propNameTableClassName)
                .packageName(propNameTableClassPackageName)
                .srcDir(srcDir)
                .propNameConverter(identityPropNameConverter)
                .build();

        return generatePropNameTableClasses(codeConfig);
    }

    /**
     * Generates property-name table source using a full configuration object.
     *
     * <p>This overload supports property-name remapping, optional case-specific nested interfaces,
     * function-based property constants, class-level property lists, inherited interfaces, and
     * optional write-to-file behavior.
     *
     * @param codeConfig full generation configuration
     * @return generated Java source for the property-name table class
     * @throws IllegalArgumentException if {@code codeConfig} is invalid
     * @throws RuntimeException if writing to file fails when {@code srcDir} is configured
     */
    public static String generatePropNameTableClasses(final PropNameTableCodeConfig codeConfig) {
        N.checkArgNotNull(codeConfig, cs.codeConfig);

        final Collection<Class<?>> entityClasses = N.checkArgNotEmpty(codeConfig.getEntityClasses(), "entityClasses");

        final List<Class<?>> entityClassesToUse = StreamEx.of(entityClasses).filter(cls -> {
            if (cls.isInterface()) {
                return false;
            }

            final String simpleClassName = ClassUtil.getSimpleClassName(cls);

            // NOSONAR
            return !cls.isMemberClass() || !simpleClassName.endsWith(BUILDER) || cls.getDeclaringClass() == null // NOSONAR
                    || !simpleClassName.equals(ClassUtil.getSimpleClassName(cls.getDeclaringClass()) + BUILDER);
        }).toList();

        final Class<?> entityClass = N.firstElement(entityClassesToUse).orElseThrow();
        final boolean generateClassPropNameList = codeConfig.isGenerateClassPropNameList();
        final String propNameTableClassPackageName = codeConfig.getPackageName();
        final String packageName = N.defaultIfEmpty(propNameTableClassPackageName, ClassUtil.getPackageName(entityClass));
        final String propNameTableClassName = N.checkArgNotEmpty(codeConfig.getClassName(), "className");
        final Collection<Class<?>> extendedInterfaces = codeConfig.getExtendedInterfaces();
        final BiFunction<Class<?>, String, String> propNameConverter = N.defaultIfNull(codeConfig.getPropNameConverter(), identityPropNameConverter);

        final String interfaceName = Stream.of(extendedInterfaces)
                .map(ClassUtil::getCanonicalClassName)
                .map(it -> Strings.isEmpty(packageName) ? it : Strings.replaceAll(it, packageName + ".", ""))
                .mapFirst(it -> " extends " + it)
                .join(", ", "public interface " + propNameTableClassName, "");

        final StringBuilder sb = new StringBuilder();

        if (Strings.isNotEmpty(packageName)) {
            sb.append("package ").append(packageName).append(";").append(LINE_SEPARATOR);
        }

        if (generateClassPropNameList) {
            sb.append(LINE_SEPARATOR).append("import java.util.List;").append(LINE_SEPARATOR);
        }

        final String allClassName = StreamEx.of(entityClassesToUse).map(ClassUtil::getSimpleClassName).join(", ", "[", "]");

        if (generateClassPropNameList && StreamEx.of(entityClassesToUse).map(ClassUtil::getSimpleClassName).hasDuplicates()) {
            throw new IllegalArgumentException(
                    "Duplicate simple class names found: " + allClassName + ". It's not supported when generateClassPropNameList is true");
        }

        {
            final ListMultimap<String, String> propNameMap = N.newListMultimap();
            final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();

            for (final Class<?> cls : entityClassesToUse) {
                final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                String newPropName = null;

                for (final String propName : Beans.getPropNameList(cls)) {
                    newPropName = propNameConverter.apply(cls, propName);

                    if (Strings.isEmpty(newPropName)) {
                        continue;
                    }

                    if (newPropName.equals(propName)) {
                        propNameMap.put(newPropName, simpleClassName);
                    } else {
                        propNameMap.put(newPropName, simpleClassName + "." + propName);
                    }

                    if (generateClassPropNameList) {
                        classPropNameListMap.put(simpleClassName, newPropName);
                    }
                }
            }

            sb.append(LINE_SEPARATOR)
                    .append("/**")
                    .append(LINE_SEPARATOR)
                    .append(" * Auto-generated class for property(field) name table for classes: {@code ")
                    .append(allClassName)
                    .append("}")
                    .append(LINE_SEPARATOR)
                    .append(" */");

            //    if (Character.isLowerCase(propNameTableClassName.charAt(0))) {
            //        sb.append(LINE_SEPARATOR).append("@SuppressWarnings(\"java:S1192\")");
            //    }

            sb.append(LINE_SEPARATOR)
                    .append(interfaceName)
                    .append(" {")
                    .append(Character.isLowerCase(propNameTableClassName.charAt(0)) ? NOSONAR_COMMENTS : "")
                    .append(LINE_SEPARATOR); //

            final List<String> propNames = new ArrayList<>(propNameMap.keySet());
            N.sort(propNames);

            for (final String propName : propNames) {
                final String clsNameList = Stream.of(propNameMap.get(propName)).sorted().join(", ", "{@code [", "]}");

                sb.append(LINE_SEPARATOR)
                        .append("    /** Property(field) name {@code \"")
                        .append(propName)
                        .append("\"} for classes: ")
                        .append(clsNameList)
                        .append(" */")
                        .append(LINE_SEPARATOR)
                        .append("    String ")
                        .append(Strings.isKeyword(propName) ? "_" : "")
                        .append(propName)
                        .append(" = \"")
                        .append(propName)
                        .append("\";")
                        .append(LINE_SEPARATOR);
            }

            if (generateClassPropNameList) {
                for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                    final String fieldNameForPropNameList = Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList";

                    sb.append(LINE_SEPARATOR)
                            .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                            .append(classPropNameListEntry.getKey())
                            .append("\"}.")
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append("    List<String> ")
                            .append(propNameMap.containsKey(fieldNameForPropNameList) ? "_" : "")
                            .append(fieldNameForPropNameList)
                            .append(" = List.of(")
                            .append(StreamEx.of(classPropNameListEntry.getValue()).sorted().join(", "))
                            .append(");")
                            .append(LINE_SEPARATOR);
                }
            }
        }

        {
            if (codeConfig.isGenerateSnakeCase()) {
                final ListMultimap<Tuple2<String, String>, String> propNameMap = N.newListMultimap();
                final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();
                final BiFunction<Class<?>, String, String> propNameConverterForSnakeCase = N.defaultIfNull(codeConfig.getPropNameConverterForSnakeCase(),
                        (cls, propName) -> Strings.toSnakeCase(propName));

                for (final Class<?> cls : entityClassesToUse) {
                    final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                    String newPropName = null;
                    String propNameInSnakeCase = null;

                    for (final String propName : Beans.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInSnakeCase = propNameConverterForSnakeCase.apply(cls, propName);

                        if (Strings.isEmpty(propNameInSnakeCase)) {
                            continue;
                        }

                        if (newPropName.equals(propName)) {
                            propNameMap.put(Tuple.of(newPropName, propNameInSnakeCase), simpleClassName);
                        } else {
                            propNameMap.put(Tuple.of(newPropName, propNameInSnakeCase), simpleClassName + "." + propName);
                        }

                        if (generateClassPropNameList) {
                            classPropNameListMap.put(simpleClassName, newPropName);
                        }
                    }
                }

                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("/**")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" * Auto-generated class for lower case property(field) name table for classes: {@code ")
                        .append(allClassName)
                        .append("}")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" */");

                //noinspection DuplicateExpressions
                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("public interface ")
                        .append(N.defaultIfEmpty(codeConfig.getClassNameForSnakeCase(), SL))
                        .append(" {")
                        .append(Character.isLowerCase(propNameTableClassName.charAt(0)) ? " // NOSONAR" : "")
                        .append(LINE_SEPARATOR); //

                final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(propNameMap.keySet());
                final List<String> propNames = N.map(propNameTPs, it -> it._1);
                N.sortBy(propNameTPs, it -> it._1);

                for (final Tuple2<String, String> propNameTP : propNameTPs) {
                    final String clsNameList = Stream.of(propNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");

                    sb.append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    /** Property(field) name in lower case concatenated with underscore: {@code \"")
                            .append(propNameTP._2)
                            .append("\"} for classes: ")
                            .append(clsNameList)
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    String ")
                            .append(Strings.isKeyword(propNameTP._1) ? "_" : "")
                            .append(propNameTP._1)
                            .append(" = \"")
                            .append(propNameTP._2)
                            .append("\";")
                            .append(LINE_SEPARATOR);
                }

                if (generateClassPropNameList) {
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                        final String fieldNameForPropNameList = Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList";

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                                .append(classPropNameListEntry.getKey())
                                .append("\"}.")
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    List<String> ")
                                .append(propNames.contains(fieldNameForPropNameList) ? "_" : "")
                                .append(fieldNameForPropNameList)
                                .append(" = List.of(")
                                .append(StreamEx.of(classPropNameListEntry.getValue()).sorted().join(", "))
                                .append(");")
                                .append(LINE_SEPARATOR);
                    }
                }

                sb.append(LINE_SEPARATOR).append(INDENTATION).append("}").append(LINE_SEPARATOR);
            }

        }

        {
            if (codeConfig.isGenerateScreamingSnakeCase()) {
                final ListMultimap<Tuple2<String, String>, String> propNameMap = N.newListMultimap();
                final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();
                final BiFunction<Class<?>, String, String> propNameConverterForScreamingSnakeCase = N
                        .defaultIfNull(codeConfig.getPropNameConverterForScreamingSnakeCase(), (cls, propName) -> Strings.toScreamingSnakeCase(propName));

                for (final Class<?> cls : entityClassesToUse) {
                    final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                    String newPropName = null;
                    String propNameInScreamingSnakeCase = null;

                    for (final String propName : Beans.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInScreamingSnakeCase = propNameConverterForScreamingSnakeCase.apply(cls, propName);

                        if (Strings.isEmpty(propNameInScreamingSnakeCase)) {
                            continue;
                        }

                        if (newPropName.equals(propName)) {
                            propNameMap.put(Tuple.of(newPropName, propNameInScreamingSnakeCase), simpleClassName);
                        } else {
                            propNameMap.put(Tuple.of(newPropName, propNameInScreamingSnakeCase), simpleClassName + "." + propName);
                        }

                        if (generateClassPropNameList) {
                            classPropNameListMap.put(simpleClassName, newPropName);
                        }
                    }
                }

                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("/**")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" * Auto-generated class for upper case property(field) name table for classes: {@code ")
                        .append(allClassName)
                        .append("}")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" */");

                //noinspection DuplicateExpressions
                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("public interface ")
                        .append(N.defaultIfEmpty(codeConfig.getClassNameForScreamingSnakeCase(), SU))
                        .append(" {")
                        .append(Character.isLowerCase(propNameTableClassName.charAt(0)) ? " // NOSONAR" : "")
                        .append(LINE_SEPARATOR); //

                final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(propNameMap.keySet());
                final List<String> propNames = N.map(propNameTPs, it -> it._1);
                N.sortBy(propNameTPs, it -> it._1);

                for (final Tuple2<String, String> propNameTP : propNameTPs) {
                    final String clsNameList = Stream.of(propNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");

                    sb.append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    /** Property(field) name in upper case concatenated with underscore: {@code \"")
                            .append(propNameTP._2)
                            .append("\"} for classes: ")
                            .append(clsNameList)
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    String ")
                            .append(Strings.isKeyword(propNameTP._1) ? "_" : "")
                            .append(propNameTP._1)
                            .append(" = \"")
                            .append(propNameTP._2)
                            .append("\";")
                            .append(LINE_SEPARATOR);
                }

                if (generateClassPropNameList) {
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                        final String fieldNameForPropNameList = Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList";

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                                .append(classPropNameListEntry.getKey())
                                .append("\"}.")
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    List<String> ")
                                .append(propNames.contains(fieldNameForPropNameList) ? "_" : "")
                                .append(fieldNameForPropNameList)
                                .append(" = List.of(")
                                .append(StreamEx.of(classPropNameListEntry.getValue()).sorted().join(", "))
                                .append(");")
                                .append(LINE_SEPARATOR);
                    }
                }

                sb.append(LINE_SEPARATOR).append(INDENTATION).append("}").append(LINE_SEPARATOR);
            }
        }

        {
            if (codeConfig.isGenerateFunctionPropName()) {
                final String functionClassName = N.defaultIfEmpty(codeConfig.getFunctionClassName(), SF);
                final Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFuncMap = N.nullToEmpty(codeConfig.getPropFunctions());

                final List<ListMultimap<Tuple2<String, String>, String>> funcPropNameMapList = new ArrayList<>();

                for (final Map.Entry<String, TriFunction<Class<?>, Class<?>, String, String>> propFuncEntry : propFuncMap.entrySet()) {
                    final String funcName = propFuncEntry.getKey();
                    final TriFunction<Class<?>, Class<?>, String, String> propFunc = propFuncEntry.getValue();
                    final ListMultimap<Tuple2<String, String>, String> funcPropNameMap = N.newListMultimap();

                    for (final Class<?> cls : entityClassesToUse) {
                        final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                        String newPropName = null;
                        String funcPropName = null;

                        for (final String propName : Beans.getPropNameList(cls)) {
                            newPropName = propNameConverter.apply(cls, propName);

                            if (Strings.isEmpty(newPropName)) {
                                continue;
                            }

                            final Method propGetMethod = Beans.getPropGetter(cls, propName);

                            if (propGetMethod == null) {
                                continue;
                            }

                            funcPropName = propFunc.apply(cls, propGetMethod.getReturnType(), newPropName);

                            if (Strings.isEmpty(funcPropName)) {
                                continue;
                            }

                            funcPropNameMap.put(Tuple.of(funcName + "_" + newPropName, funcPropName), simpleClassName);
                        }
                    }

                    funcPropNameMapList.add(funcPropNameMap);
                }

                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("/**")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" * Auto-generated class for function property(field) name table for classes: {@code ")
                        .append(allClassName)
                        .append("}")
                        .append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append(" */");

                //    if (Character.isLowerCase(propNameTableClassName.charAt(0))) {
                //        sb.append(LINE_SEPARATOR).append("@SuppressWarnings(\"java:S1192\")");
                //    }

                sb.append(LINE_SEPARATOR)
                        .append(INDENTATION)
                        .append("public interface ")
                        .append(functionClassName)
                        .append(" {")
                        .append(Character.isLowerCase(functionClassName.charAt(0)) ? " // NOSONAR" : "")
                        .append(LINE_SEPARATOR); //

                for (final ListMultimap<Tuple2<String, String>, String> funcPropNameMap : funcPropNameMapList) {
                    final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(funcPropNameMap.keySet());
                    N.sortBy(propNameTPs, it -> it._1);

                    for (final Tuple2<String, String> propNameTP : propNameTPs) {
                        final String clsNameList = Stream.of(funcPropNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Function property(field) name {@code \"")
                                .append(propNameTP._2)
                                .append("\"} for classes: ")
                                .append(clsNameList)
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    String ")
                                .append(propNameTP._1)
                                .append(" = \"")
                                .append(propNameTP._2)
                                .append("\";")
                                .append(LINE_SEPARATOR);
                    }
                }

                sb.append(LINE_SEPARATOR).append(INDENTATION).append("}").append(LINE_SEPARATOR);
            }
        }

        sb.append(LINE_SEPARATOR).append("}").append(LINE_SEPARATOR);

        final String ret = sb.toString();

        final String srcDir = codeConfig.getSrcDir();

        if (Strings.isNotEmpty(srcDir)) {
            String packageDir = srcDir;

            if (Strings.isNotEmpty(packageName)) {
                if (!(packageDir.endsWith("/") || packageDir.endsWith("\\"))) {
                    packageDir += "/";
                }

                packageDir += Strings.replaceAll(packageName, ".", "/");
            }

            IOUtil.mkdirsIfNotExists(new File(packageDir));
            final File file = new File(packageDir + IOUtil.DIR_SEPARATOR + propNameTableClassName + ".java");
            IOUtil.createFileIfNotExists(file);

            try {
                IOUtil.write(ret, file);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return ret;
    }

    /**
     * Configuration for {@link #generatePropNameTableClasses(PropNameTableCodeConfig)}.
     *
     * <p>Use the Lombok-generated builder to enable only the features you need.
     * Typical options include class/package output, property-name conversion, case-specific nested
     * interfaces, function-based constants, and per-class property-name lists.
     *
     * <p>Example:</p>
     * <pre>{@code
     * PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
     *         .entityClasses(classes)
     *         .className(CodeGenerationUtil.S)
     *         .packageName("com.landawn.abacus.samples.util")
     *         .srcDir("./samples")
     *         .propNameConverter((cls, propName) -> propName.equals("create_time") ? "createdTime" : propName)
     *         .generateClassPropNameList(true)
     *         .generateSnakeCase(true)
     *         .generateScreamingSnakeCase(true)
     *         .classNameForScreamingSnakeCase("sau")
     *         .generateFunctionPropName(true)
     *         .functionClassName("f")
     *         .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
     *         .build();
     * }</pre>
     */
    @Builder
    @Data
    @AllArgsConstructor
    @Accessors(chain = true)
    public static final class PropNameTableCodeConfig {
        /** Entity classes to scan for bean properties. Required. */
        private Collection<Class<?>> entityClasses;

        /** Top-level interface name for generated property constants. Required. */
        private String className;

        /** Package for generated source; if {@code null}, uses the first entity package. */
        private String packageName;

        /** Source root to write files into; if {@code null}, only returns generated source text. */
        private String srcDir;

        /** Optional interfaces that the generated top-level interface should extend. */
        private Collection<Class<?>> extendedInterfaces;

        /**
         * Converts property names before constants are emitted.
         *
         * <p>The function receives {@code (entityClass, propName)}. Return {@code null} or empty
         * to skip a property. If {@code null}, identity mapping is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverter;

        /** Whether to generate a {@code List<String>} constant per entity class. Default is {@code false}. */
        private boolean generateClassPropNameList;

        /** Whether to generate a snake_case nested interface. Default is {@code false}. */
        private boolean generateSnakeCase;

        /** Nested interface name for snake_case constants. Defaults to {@link CodeGenerationUtil#SL}. */
        private String classNameForSnakeCase;

        /**
         * Converts property names to snake_case constant values.
         *
         * <p>If {@code null}, {@link Strings#toSnakeCase(String)} is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverterForSnakeCase;

        /** Whether to generate a SCREAMING_SNAKE_CASE nested interface. Default is {@code false}. */
        private boolean generateScreamingSnakeCase;

        /** Nested interface name for SCREAMING_SNAKE_CASE constants. Defaults to {@link CodeGenerationUtil#SU}. */
        private String classNameForScreamingSnakeCase;

        /**
         * Converts property names to SCREAMING_SNAKE_CASE constant values.
         *
         * <p>If {@code null}, {@link Strings#toScreamingSnakeCase(String)} is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverterForScreamingSnakeCase;

        /** Whether to generate a nested interface for function-based names (for example, {@code min(age)}). */
        private boolean generateFunctionPropName;

        /** Nested interface name for function-based constants. Defaults to {@link CodeGenerationUtil#SF}. */
        private String functionClassName;

        /**
         * Function definitions used when {@link #generateFunctionPropName} is enabled.
         *
         * <p>Map key is a constant prefix (for example {@code min}); function input is
         * {@code (entityClass, propertyType, convertedPropertyName)}; returning {@code null} or
         * empty skips the property.
         */
        private Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions;

        /** Default constructor for framework/tooling compatibility. */
        public PropNameTableCodeConfig() {
        }
    }
}
