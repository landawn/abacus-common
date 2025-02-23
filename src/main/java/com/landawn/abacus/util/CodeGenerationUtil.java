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
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

public final class CodeGenerationUtil {
    /**
     * Default name of class for field/prop names.
     */
    public static final String S = "s";

    /**
     * Default name of class for lower case field/prop names concatenated by underscore "_".
     */
    public static final String SL = "sl";

    /**
     * Default name of class for upper case field/prop names concatenated by underscore "_".
     */
    public static final String SU = "su";

    /**
     * Default name of class for function field/prop names.
     */
    public static final String SF = "sf";

    /**
     * Default name of inner class for field names inside an entity class.
     */
    public static final String X = "x";

    public static final TriFunction<Class<?>, Class<?>, String, String> MIN_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "min(" + propName + ")";
        }

        return null;
    };

    public static final TriFunction<Class<?>, Class<?>, String, String> MAX_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(propClass)) {
            return "max(" + propName + ")";
        }

        return null;
    };

    private static final String INDENTATION = "    ";

    private static final String BUILDER = "Builder";

    private static final String LINE_SEPARATOR = IOUtil.LINE_SEPARATOR;

    private static final BiFunction<Class<?>, String, String> identityPropNameConverter = (cls, propName) -> propName;
    public static final String NOSONAR_COMMENTS = " // NOSONAR";

    private CodeGenerationUtil() {
        // singleton for utility class.
    }

    /**
     *
     * @param entityClass
     * @return
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass) {
        return generatePropNameTableClass(entityClass, X);
    }

    /**
     *
     * @param entityClass
     * @param propNameTableClassName
     * @return
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName) {
        return generatePropNameTableClass(entityClass, propNameTableClassName, null);
    }

    /**
     *
     * @param entityClass
     * @param propNameTableClassName
     * @param srcDir
     * @return
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
                .append(Character.isLowerCase(interfaceName.charAt(0)) ? NOSONAR_COMMENTS : "")
                .append(LINE_SEPARATOR)
                .append(LINE_SEPARATOR); //

        for (final String propName : ClassUtil.getPropNameList(entityClass)) {

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
     *
     * @param entityClasses
     * @return
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses) {
        return generatePropNameTableClasses(entityClasses, S);
    }

    /**
     *
     * @param entityClasses
     * @param propNameTableClassName
     * @return
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName) {
        return generatePropNameTableClasses(entityClasses, propNameTableClassName, null, null);
    }

    /**
     *
     * @param entityClasses
     * @param propNameTableClassName
     * @param propNameTableClassPackageName
     * @param srcDir
     * @return
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
     *
     * @param codeConfig
     * @return
     */
    public static String generatePropNameTableClasses(final PropNameTableCodeConfig codeConfig) {
        N.checkArgNotNull(codeConfig, cs.codeConfig);

        final Collection<Class<?>> entityClasses = N.checkArgNotEmpty(codeConfig.getEntityClasses(), "entityClasses");
        final String propNameTableClassName = N.checkArgNotEmpty(codeConfig.getClassName(), "className");
        final BiFunction<Class<?>, String, String> propNameConverter = N.defaultIfNull(codeConfig.getPropNameConverter(), identityPropNameConverter);

        final String interfaceName = "public interface " + propNameTableClassName;

        @SuppressWarnings("resource")
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

        @SuppressWarnings("resource")
        final String allClassName = StreamEx.of(entityClassesToUse).map(ClassUtil::getSimpleClassName).join(", ", "[", "]");

        //noinspection resource
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

                for (final String propName : ClassUtil.getPropNameList(cls)) {
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

            //noinspection DuplicateExpressions
            sb.append(LINE_SEPARATOR)
                    .append(interfaceName)
                    .append(" {")
                    .append(Character.isLowerCase(propNameTableClassName.charAt(0)) ? NOSONAR_COMMENTS : "")
                    .append(LINE_SEPARATOR); //

            final List<String> propNames = new ArrayList<>(propNameMap.keySet());
            N.sort(propNames);

            for (final String propName : propNames) {
                @SuppressWarnings("resource")
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

                    //noinspection resource
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

                    for (final String propName : ClassUtil.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInLowerCaseWithUnderscore = propNameConverterForLowerCaseWithUnderscore.apply(cls, propName);

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
                    @SuppressWarnings("resource")
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

                        //noinspection resource
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

                    for (final String propName : ClassUtil.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        propNameInUpperCaseWithUnderscore = propNameConverterForUpperCaseWithUnderscore.apply(cls, propName);

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
                    @SuppressWarnings("resource")
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

                        //noinspection resource
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

                        for (final String propName : ClassUtil.getPropNameList(cls)) {
                            newPropName = propNameConverter.apply(cls, propName);

                            if (ClassUtil.getPropGetMethod(cls, propName) == null) {
                                continue;
                            }

                            //noinspection DataFlowIssue
                            funcPropName = propFunc.apply(cls, ClassUtil.getPropGetMethod(cls, propName).getReturnType(), newPropName);

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
                        @SuppressWarnings("resource")
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
            IOUtil.createIfNotExists(file);

            try {
                IOUtil.write(ret, file);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return ret;
    }

    /**
     * A sample, just a sample, not a general configuration required.
     * <pre>
     * final PropNameTableCodeConfig codeConfig = PropNameTableCodeConfig.builder()
     *          .entityClasses(classes)
     *          .className(CodeGenerationUtil.S)
     *          .packageName("com.landawn.abacus.samples.util")
     *          .srcDir("./samples")
     *          .propNameConverter((cls, propName) -> propName.equals("create_time") ? "createdTime" : propName) // default is: (cls, propName) -> propName
     *          .generateClassPropNameList(true)
     *          .generateLowerCaseWithUnderscore(true)
     *          .generateUpperCaseWithUnderscore(true)
     *          .classNameForUpperCaseWithUnderscore("sau")
     *          .generateFunctionPropName(true) // default is false
     *          .functionClassName("f") // default is "sf" if not set.
     *          .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC)) // Returns null to skip the field.
     *          .build();
     * </pre>
     *
     */
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static final class PropNameTableCodeConfig {
        private Collection<Class<?>> entityClasses;
        private String className;
        private String packageName;
        private String srcDir;
        private BiFunction<Class<?>, String, String> propNameConverter;
        private boolean generateClassPropNameList;
        private boolean generateLowerCaseWithUnderscore;
        private String classNameForLowerCaseWithUnderscore;
        private BiFunction<Class<?>, String, String> propNameConverterForLowerCaseWithUnderscore;
        private boolean generateUpperCaseWithUnderscore;
        private String classNameForUpperCaseWithUnderscore;
        private BiFunction<Class<?>, String, String> propNameConverterForUpperCaseWithUnderscore;
        private boolean generateFunctionPropName;
        private String functionClassName;
        private Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions;
    }
}
