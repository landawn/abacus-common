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
 * Utility class for generating property name table classes from entity classes.
 * This class provides methods to generate inner classes or standalone classes containing
 * constants for property names, which can be used to avoid hard-coding string literals
 * when referencing entity properties.
 * 
 * <p>The generated classes help with:
 * <ul>
 *   <li>Type-safe property name references</li>
 *   <li>IDE auto-completion support</li>
 *   <li>Compile-time checking of property names</li>
 *   <li>Easier refactoring when property names change</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Generate inner class 'x' for a single entity
 * String code = CodeGenerationUtil.generatePropNameTableClass(User.class);
 * 
 * // Generate standalone class for multiple entities
 * List<Class<?>> entities = Arrays.asList(User.class, Order.class, Product.class);
 * String code = CodeGenerationUtil.generatePropNameTableClasses(entities, "s");
 * }</pre>
 * 
 */
public final class CodeGenerationUtil {
    /**
     * Default name of class for field/prop names.
     * Used as the default class name when generating property name table classes.
     */
    public static final String S = "s";

    /**
     * Default name of class for lower case field/prop names concatenated by underscore "_".
     * Used for generating property names in lower_case_format.
     */
    public static final String SL = "sl";

    /**
     * Default name of class for upper case field/prop names concatenated by underscore "_".
     * Used for generating property names in UPPER_CASE_FORMAT.
     */
    public static final String SU = "su";

    /**
     * Default name of class for function field/prop names.
     * Used for generating function-based property names like min(), max(), etc.
     */
    public static final String SF = "sf";

    /**
     * Default name of inner class for field names inside an entity class.
     * Typically used when generating property name tables as inner classes.
     */
    public static final String X = "x";

    /**
     * Predefined function for generating min() property names.
     * Only applicable to properties that implement Comparable interface.
     * 
     * <p><b>Usage Examples:</b></p> For a property "age", it generates "min(age)"
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MIN_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "min(" + propName + ")";
        }

        return null;
    };

    /**
     * Predefined function for generating max() property names.
     * Only applicable to properties that implement Comparable interface.
     * 
     * <p><b>Usage Examples:</b></p> For a property "age", it generates "max(age)"
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MAX_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "max(" + propName + ")";
        }

        return null;
    };

    private static final String INDENTATION = "    ";

    private static final String BUILDER = "Builder";

    private static final String LINE_SEPARATOR = IOUtil.LINE_SEPARATOR;

    /** Identity property name converter that returns property names unchanged. */
    private static final BiFunction<Class<?>, String, String> identityPropNameConverter = (cls, propName) -> propName;

    /** Comment string used to suppress SonarQube warnings. */
    public static final String NOSONAR_COMMENTS = " // NOSONAR";

    private CodeGenerationUtil() {
        // singleton for utility class.
    }

    /**
     * Generates a property name table class as an inner interface for the specified entity class.
     * The generated interface will contain string constants for each property in the entity.
     * 
     * <p>The default interface name is "x".
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String code = CodeGenerationUtil.generatePropNameTableClass(User.class);
     * // Generates:
     * // public interface x {
     * //     String id = "id";
     * //     String name = "name";
     * //     String email = "email";
     * // }
     * }</pre>
     *
     * @param entityClass the entity class to generate property names for
     * @return the generated Java code as a string
     * @see #generatePropNameTableClass(Class, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass) {
        return generatePropNameTableClass(entityClass, X);
    }

    /**
     * Generates a property name table class as an inner interface for the specified entity class
     * with a custom interface name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "Props");
     * // Generates:
     * // public interface Props {
     * //     String id = "id";
     * //     String name = "name";
     * //     String email = "email";
     * // }
     * }</pre>
     *
     * @param entityClass the entity class to generate property names for
     * @param propNameTableClassName the name of the generated interface
     * @return the generated Java code as a string
     * @see #generatePropNameTableClass(Class, String, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName) {
        return generatePropNameTableClass(entityClass, propNameTableClassName, null);
    }

    /**
     * Generates a property name table class as an inner interface for the specified entity class
     * and optionally writes it to the source file.
     * 
     * <p>If srcDir is provided, the generated code will be inserted into the existing
     * entity class file. If the interface already exists, it will be replaced.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate and write to file
     * String code = CodeGenerationUtil.generatePropNameTableClass(
     *     User.class, "Props", "./src/main/java"
     * );
     * }</pre>
     *
     * @param entityClass the entity class to generate property names for
     * @param propNameTableClassName the name of the generated interface
     * @param srcDir the source directory to write the file to, or null to only return the code
     * @return the generated Java code as a string
     * @throws RuntimeException if writing to file fails
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
     * Generates property name table classes for multiple entity classes using default settings.
     * The generated class will contain property name constants for all provided entities.
     * 
     * <p>Uses "s" as the default class name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Class<?>> entities = Arrays.asList(User.class, Order.class);
     * String code = CodeGenerationUtil.generatePropNameTableClasses(entities);
     * }</pre>
     *
     * @param entityClasses collection of entity classes to process
     * @return the generated Java code as a string
     * @see #generatePropNameTableClasses(Collection, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses) {
        return generatePropNameTableClasses(entityClasses, S);
    }

    /**
     * Generates property name table classes for multiple entity classes with a custom class name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Class<?>> entities = Arrays.asList(User.class, Order.class);
     * String code = CodeGenerationUtil.generatePropNameTableClasses(entities, "Props");
     * }</pre>
     *
     * @param entityClasses collection of entity classes to process
     * @param propNameTableClassName the name of the generated class
     * @return the generated Java code as a string
     * @see #generatePropNameTableClasses(Collection, String, String, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName) {
        return generatePropNameTableClasses(entityClasses, propNameTableClassName, null, null);
    }

    /**
     * Generates property name table classes for multiple entity classes with full customization.
     * Optionally writes the generated class to a file in the specified source directory.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Class<?>> entities = Arrays.asList(User.class, Order.class);
     * String code = CodeGenerationUtil.generatePropNameTableClasses(
     *     entities, 
     *     "Props", 
     *     "com.example.generated", 
     *     "./src/main/java"
     * );
     * }</pre>
     *
     * @param entityClasses collection of entity classes to process
     * @param propNameTableClassName the name of the generated class
     * @param propNameTableClassPackageName the package name for the generated class
     * @param srcDir the source directory to write the file to, or null to only return the code
     * @return the generated Java code as a string
     * @throws RuntimeException if writing to file fails
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
     * Generates property name table classes using a comprehensive configuration object.
     * This is the most flexible method, allowing full customization of the generation process.
     * 
     * <p>The configuration object supports:
     * <ul>
     *   <li>Custom property name converters</li>
     *   <li>Lower/upper case property name generation</li>
     *   <li>Function-based property names (min, max, etc.)</li>
     *   <li>Per-class property name lists</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
     *     .entityClasses(entities)
     *     .className("Props")
     *     .packageName("com.example.generated")
     *     .srcDir("./src/main/java")
     *     .generateLowerCaseWithUnderscore(true)
     *     .generateUpperCaseWithUnderscore(true)
     *     .generateFunctionPropName(true)
     *     .propFunctions(N.asLinkedHashMap("min", MIN_FUNC, "max", MAX_FUNC))
     *     .build();
     * 
     * String code = CodeGenerationUtil.generatePropNameTableClasses(config);
     * }</pre>
     *
     * @param codeConfig the configuration object containing all generation parameters
     * @return the generated Java code as a string
     * @throws IllegalArgumentException if codeConfig is null or invalid
     * @throws RuntimeException if writing to file fails (when srcDir is specified)
     */
    public static String generatePropNameTableClasses(final PropNameTableCodeConfig codeConfig) {
        N.checkArgNotNull(codeConfig, cs.codeConfig);

        final Collection<Class<?>> entityClasses = N.checkArgNotEmpty(codeConfig.getEntityClasses(), "entityClasses");
        final String propNameTableClassName = N.checkArgNotEmpty(codeConfig.getClassName(), "className");
        final BiFunction<Class<?>, String, String> propNameConverter = N.defaultIfNull(codeConfig.getPropNameConverter(), identityPropNameConverter);

        final String interfaceName = "public interface " + propNameTableClassName;

        final List<Class<?>> entityClassesToUse = StreamEx.of(entityClasses).filter(cls -> {
            if (cls.isInterface()) {
                return false;
            }

            final String simpleClassName = ClassUtil.getSimpleClassName(cls);

            // NOSONAR
            return !cls.isMemberClass() || !simpleClassName.endsWith(BUILDER) || cls.getDeclaringClass() == null // NOSONAR
                    || !simpleClassName.equals(ClassUtil.getSimpleClassName(cls.getDeclaringClass()) + BUILDER);
        }).toList();

        final StringBuilder sb = new StringBuilder();

        final Class<?> entityClass = N.firstElement(entityClassesToUse).orElseThrow();
        final String propNameTableClassPackageName = codeConfig.getPackageName();
        final String packageName = N.defaultIfEmpty(propNameTableClassPackageName, ClassUtil.getPackageName(entityClass));
        final boolean generateClassPropNameList = codeConfig.isGenerateClassPropNameList();

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
                for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap.entrySet()) {
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
            if (codeConfig.isGenerateLowerCaseWithUnderscore()) {
                final ListMultimap<Tuple2<String, String>, String> propNameMap = N.newListMultimap();
                final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();
                final BiFunction<Class<?>, String, String> propNameConverterForLowerCaseWithUnderscore = N.defaultIfNull(
                        codeConfig.getPropNameConverterForLowerCaseWithUnderscore(), (cls, propName) -> Strings.toLowerCaseWithUnderscore(propName));

                for (final Class<?> cls : entityClassesToUse) {
                    final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                    String newPropName = null;
                    String propNameInLowerCaseWithUnderscore = null;

                    for (final String propName : Beans.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInLowerCaseWithUnderscore = propNameConverterForLowerCaseWithUnderscore.apply(cls, propName);

                        if (Strings.isEmpty(propNameInLowerCaseWithUnderscore)) {
                            continue;
                        }

                        if (newPropName.equals(propName)) {
                            propNameMap.put(Tuple.of(newPropName, propNameInLowerCaseWithUnderscore), simpleClassName);
                        } else {
                            propNameMap.put(Tuple.of(newPropName, propNameInLowerCaseWithUnderscore), simpleClassName + "." + propName);
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
                        .append(N.defaultIfEmpty(codeConfig.getClassNameForLowerCaseWithUnderscore(), SL))
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
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap.entrySet()) {
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
            if (codeConfig.isGenerateUpperCaseWithUnderscore()) {
                final ListMultimap<Tuple2<String, String>, String> propNameMap = N.newListMultimap();
                final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();
                final BiFunction<Class<?>, String, String> propNameConverterForUpperCaseWithUnderscore = N.defaultIfNull(
                        codeConfig.getPropNameConverterForUpperCaseWithUnderscore(), (cls, propName) -> Strings.toUpperCaseWithUnderscore(propName));

                for (final Class<?> cls : entityClassesToUse) {
                    final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                    String newPropName = null;
                    String propNameInUpperCaseWithUnderscore = null;

                    for (final String propName : Beans.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInUpperCaseWithUnderscore = propNameConverterForUpperCaseWithUnderscore.apply(cls, propName);

                        if (Strings.isEmpty(propNameInUpperCaseWithUnderscore)) {
                            continue;
                        }

                        if (newPropName.equals(propName)) {
                            propNameMap.put(Tuple.of(newPropName, propNameInUpperCaseWithUnderscore), simpleClassName);
                        } else {
                            propNameMap.put(Tuple.of(newPropName, propNameInUpperCaseWithUnderscore), simpleClassName + "." + propName);
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
                        .append(N.defaultIfEmpty(codeConfig.getClassNameForUpperCaseWithUnderscore(), SU))
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
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap.entrySet()) {
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

                            final Method propGetMethod = Beans.getPropGetMethod(cls, propName);

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
     * Configuration class for property name table code generation.
     * This class provides a builder pattern for configuring all aspects of the code generation process.
     * 
     * <p>Example configuration:</p>
     * <pre>{@code
     * PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
     *     .entityClasses(classes)
     *     .className(CodeGenerationUtil.S)
     *     .packageName("com.landawn.abacus.samples.util")
     *     .srcDir("./samples")
     *     .propNameConverter((cls, propName) -> propName.equals("create_time") ? "createdTime" : propName)
     *     .generateClassPropNameList(true)
     *     .generateLowerCaseWithUnderscore(true)
     *     .generateUpperCaseWithUnderscore(true)
     *     .classNameForUpperCaseWithUnderscore("sau")
     *     .generateFunctionPropName(true)
     *     .functionClassName("f")
     *     .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
     *     .build();
     * }</pre>
     */
    @Builder
    @Data
    @AllArgsConstructor
    @Accessors(chain = true)
    public static final class PropNameTableCodeConfig {
        /** Collection of entity classes to generate property name tables for. Required. */
        private Collection<Class<?>> entityClasses;

        /** Name of the generated property name table class. Required. */
        private String className;

        /** Package name for the generated class. If null, uses the package of the first entity class. */
        private String packageName;

        /** Source directory to write the generated file to. If null, only returns the generated code as a string. */
        private String srcDir;

        /** Function to convert property names. Receives the entity class and property name, returns the converted property name.
         * Return null or empty string to skip a property. If null, uses identity function (no conversion). */
        private BiFunction<Class<?>, String, String> propNameConverter;

        /** Whether to generate a List of property names for each class. Default is false. */
        private boolean generateClassPropNameList;

        /** Whether to generate an inner interface with lower case property names concatenated with underscore. Default is false. */
        private boolean generateLowerCaseWithUnderscore;

        /** Name for the lower case with underscore inner interface. If null, uses {@link #SL}. */
        private String classNameForLowerCaseWithUnderscore;

        /** Function to convert property names to lower case with underscore format.
         * If null, uses {@link Strings#toLowerCaseWithUnderscore(String)}. */
        private BiFunction<Class<?>, String, String> propNameConverterForLowerCaseWithUnderscore;

        /** Whether to generate an inner interface with upper case property names concatenated with underscore. Default is false. */
        private boolean generateUpperCaseWithUnderscore;

        /** Name for the upper case with underscore inner interface. If null, uses {@link #SU}. */
        private String classNameForUpperCaseWithUnderscore;

        /** Function to convert property names to upper case with underscore format.
         * If null, uses {@link Strings#toUpperCaseWithUnderscore(String)}. */
        private BiFunction<Class<?>, String, String> propNameConverterForUpperCaseWithUnderscore;

        /** Whether to generate an inner interface with function-based property names (e.g., min(age), max(salary)). Default is false. */
        private boolean generateFunctionPropName;

        /** Name for the function property name inner interface. If null, uses {@link #SF}. */
        private String functionClassName;

        /** Map of function name to function implementation. The function receives entity class, property class, and property name,
         * and returns the function property name string. Return null to skip a property for that function. */
        private Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions;

        /**
         * Default constructor.
         * Creates a new instance with default values for all fields.
         */
        public PropNameTableCodeConfig() {
        }
    }
}