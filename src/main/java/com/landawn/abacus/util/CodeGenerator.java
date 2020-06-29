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

package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.landawn.abacus.core.AbstractDirtyMarker;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeType;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class CodeGenerator {
    /**
     * To generate the entity classes which not depend on <code>N.class</code> in the codes of <code>hashcode/equals/toString</code> methods, specify the parameter <code>utilClassForHashEqualsToString</code> in method <code>entityDefinitionXml2Class</code> with this value <code>_N</code>.
     */
    public static final Class<?> _N = _N.class;

    private static final String POSTFIX_OF_JAVA_FILE = ".java";

    /**
     * Field USUAL_TYPES.
     */
    private static Set<String> USUAL_TYPES = N.newLinkedHashSet();

    static {
        USUAL_TYPES.add("java.lang");
        USUAL_TYPES.add("java.util");
        USUAL_TYPES.add("java.util.concurrent");
        USUAL_TYPES.add("java.time");
        USUAL_TYPES.add("java.io");
        USUAL_TYPES.add("java.nio");
        USUAL_TYPES.add("java.sql");
        USUAL_TYPES.add("java.net");
        USUAL_TYPES.add("java.math");
        USUAL_TYPES.add("javax.xml");
        USUAL_TYPES.add(HBaseColumn.class.getCanonicalName());
    }

    private static Map<String, String> JAVA_TYPE_PROP_NAME = new HashMap<>();

    static {
        JAVA_TYPE_PROP_NAME.put(boolean.class.getName(), "boolean_");
        JAVA_TYPE_PROP_NAME.put(char.class.getName(), "char_");
        JAVA_TYPE_PROP_NAME.put(byte.class.getName(), "byte_");
        JAVA_TYPE_PROP_NAME.put(short.class.getName(), "short_");
        JAVA_TYPE_PROP_NAME.put(int.class.getName(), "int_");
        JAVA_TYPE_PROP_NAME.put(long.class.getName(), "long_");
        JAVA_TYPE_PROP_NAME.put(float.class.getName(), "float_");
        JAVA_TYPE_PROP_NAME.put(double.class.getName(), "double_");
        JAVA_TYPE_PROP_NAME.put("class", "class_");
    }

    /**
     * Constructor for CodeGenerator.
     */
    private CodeGenerator() {
        // No instance.
    }

    /**
     * Gets the simple prop name table class name.
     *
     * @param className
     * @return
     */
    protected static String getSimplePropNameTableClassName(final String className) {
        String simpleClassName = className;
        int index = className.lastIndexOf(WD._PERIOD);

        if (index > -1) {
            simpleClassName = className.substring(index + 1);
        }

        return simpleClassName;
    }

    /**
     * Write the generated methods by the fields defined the in specified class to the source file.
     *
     * <br />
     * Add below comments to specified the section where the generated methods should be written to
     * <pre>
     * =====>
     *
     * <=====
     * </pre>
     *
     * @param srcDir
     * @param cls
     */
    public static void writeClassMethod(final File srcDir, final Class<?> cls) {
        writeClassMethod(srcDir, cls, false, false, false, null, null, Objects.class);
    }

    /**
     * Write the generated methods by the fields defined the in specified class to the source file.
     *
     * <br />
     * Add below comments to specified the section where the generated methods should be written to
     * <pre>
     * =====>
     *
     * <=====
     * </pre>
     *
     * @param srcDir
     * @param cls
     * @param constructor generate constructor
     * @param copyMethod generate the copy method.
     * @param fluentSetter
     * @param ignoreFieldNames
     * @param fieldName2MethodName
     * @param utilClassForHashEqualsToString is <code>Objects.class</code> by default. It can also be <code>N.class</code> or any classes else which provide the {@code hashCode/equals/toString} method.
     *      Or specify <code>CodeGenerator._N</code> or your own utility class to generate entity classes which not dependent on abacus-util.jar for Methods {@code hashCode/equals/toString}.
     */
    public static void writeClassMethod(final File srcDir, final Class<?> cls, final boolean constructor, final boolean copyMethod, final boolean fluentSetter,
            Set<String> ignoreFieldNames, final Map<String, String> fieldName2MethodName, final Class<?> utilClassForHashEqualsToString) {
        writeClassMethod(srcDir, cls, constructor, copyMethod, fluentSetter, ignoreFieldNames, fieldName2MethodName, ParentPropertyMode.FIRST,
                ParentPropertyMode.FIRST, utilClassForHashEqualsToString);
    }

    /**
     * Write the generated methods by the fields defined the in specified class to the source file.
     *
     * <br />
     * Add below comments to specified the section where the generated methods should be written to
     * <pre>
     * =====>
     *
     * <=====
     * </pre>
     *
     *
     * @param srcDir
     * @param cls
     * @param constructor
     * @param copyMethod
     * @param fluentSetter
     * @param ignoreFieldNames
     * @param fieldName2MethodName
     * @param parentPropertyModeForHashEquals
     * @param parentPropertyModeForToString
     * @param utilClassForHashEqualsToString is <code>Objects.class</code> by default. It can also be <code>N.class</code> or any classes else which provide the {@code hashCode/equals/toString} method.
     *      Or specify <code>CodeGenerator._N</code> or your own utility class to generate entity classes which not dependent on abacus-util.jar for Methods {@code hashCode/equals/toString}.
     */
    public static void writeClassMethod(final File srcDir, final Class<?> cls, final boolean constructor, final boolean copyMethod, final boolean fluentSetter,
            Set<String> ignoreFieldNames, final Map<String, String> fieldName2MethodName, final ParentPropertyMode parentPropertyModeForHashEquals,
            final ParentPropertyMode parentPropertyModeForToString, final Class<?> utilClassForHashEqualsToString) {

        final Package pkg = cls.getPackage();
        final String clsSourcePath = srcDir.getAbsolutePath()
                + (pkg == null ? "" : IOUtil.FILE_SEPARATOR + StringUtil.replaceAll(pkg.getName(), ".", IOUtil.FILE_SEPARATOR)) + IOUtil.FILE_SEPARATOR
                + cls.getSimpleName() + ".java";
        final File clsSourceFile = new File(clsSourcePath);

        if (clsSourceFile.exists() == false) {
            throw new RuntimeException("No source file found by path: " + clsSourcePath + " for class: " + cls.getCanonicalName());
        }

        if (ignoreFieldNames == null) {
            ignoreFieldNames = N.newLinkedHashSet();
        }

        final String simpleClassName = cls.getSimpleName();
        final List<String> lines = ImmutableList.of(IOUtil.readAllLines(clsSourceFile));
        final Map<String, Type<?>> fieldTypes = new LinkedHashMap<>();

        for (Field field : cls.getDeclaredFields()) {
            final String fieldName = field.getName();
            if (Modifier.isStatic(field.getModifiers()) || ignoreFieldNames.contains(fieldName)) {
                continue;
            } else {
                fieldTypes.put(fieldName, N.typeOf(field.getType()));
            }
        }

        if (N.isNullOrEmpty(fieldTypes) && fluentSetter == false && constructor == false && copyMethod == false) {
            return;
        }

        final BiMap<String, Class<?>> importedClasses = new BiMap<>();
        boolean hasGenericTypeField = false;
        for (int i = 0, size = lines.size(); i < size; i++) {
            final String tmp = "class " + simpleClassName;

            while (i < size && lines.get(i).indexOf(tmp) < 0) {
                String line = lines.get(i);
                if (line.startsWith("import ") && line.endsWith(";") && line.indexOf(" static ") < 0) {
                    String clsName = StringUtil.substringBetween(line, ' ', line.lastIndexOf(';')).get();
                    importedClasses.put(clsName, ClassUtil.forClass(clsName));
                }
                i++;
            }

            int start = i + 1;

            while (i < size && (lines.get(i).indexOf(") {") < 0 /*&& lines.get(i).indexOf("}") < 0*/)) {
                i++;
            }

            int end = i;
            if (start < size && end <= size && start < end) {
                for (int j = start; j < end; j++) {
                    String line = lines.get(j).trim().replaceAll("  ", " ");
                    int lastIndex = line.lastIndexOf(' ');
                    String fieldName = null;

                    if (lastIndex > 0 && line.endsWith(";") && fieldTypes.containsKey((fieldName = line.substring(lastIndex + 1, line.length() - 1)))) {
                        int firstIdx = line.indexOf('<');

                        if (firstIdx > 0) {
                            hasGenericTypeField = true;

                            int fromIndex = line.lastIndexOf(' ', firstIdx);

                            if (fromIndex >= 0) {
                                String typeName = line.substring(fromIndex + 1, lastIndex);

                                if ((typeName = typeName.trim()).length() > 0) {

                                    // for java.util.Date
                                    if (importedClasses.containsKey("java.util.Date")) {
                                        typeName = StringUtil.replaceAll(typeName, "<Date>", "<java.util.Date>");
                                        typeName = StringUtil.replaceAll(typeName, "<Date,", "<java.util.Date,");
                                        typeName = StringUtil.replaceAll(typeName, " Date>", " java.util.Date>");
                                        typeName = StringUtil.replaceAll(typeName, ",Date>", ",java.util.Date>");
                                        typeName = StringUtil.replaceAll(typeName, " Date,", " java.util.Date,");
                                        typeName = StringUtil.replaceAll(typeName, ",Date,", ",java.util.Date,");

                                        typeName = StringUtil.replaceAll(typeName, "<Date[", "<java.util.Date[");
                                        typeName = StringUtil.replaceAll(typeName, " Date[", " java.util.Date[");
                                        typeName = StringUtil.replaceAll(typeName, ",Date[", ",java.util.Date[");
                                    }

                                    fieldTypes.put(fieldName, N.typeOf(typeName));
                                }
                            }
                        }
                    }
                }
            }
        }

        final String packageName = ClassUtil.getPackageName(cls);

        final File dirFile = new File(srcDir.getAbsolutePath()
                + (N.isNullOrEmpty(packageName) ? "" : IOUtil.FILE_SEPARATOR + StringUtil.replaceAll(packageName, ".", IOUtil.FILE_SEPARATOR)));

        if (dirFile.exists() == false) {
            dirFile.mkdirs();
        }

        final Class<?> utilClass = utilClassForHashEqualsToString == null ? Objects.class : utilClassForHashEqualsToString;

        if (_N.equals(utilClass)) {
            File utilClassFile = new File(dirFile.getAbsolutePath() + IOUtil.FILE_SEPARATOR + ClassUtil.getSimpleClassName(_N) + POSTFIX_OF_JAVA_FILE);
            if (!utilClassFile.exists()) {
                String sourceCode = _N_STRING.replaceFirst("package com.landawn.abacus.util;",
                        N.isNullOrEmpty(packageName) ? "" : "package " + packageName + ";");
                IOUtil.write(utilClassFile, sourceCode);
            }
        }

        try (StringWriter writer = new StringWriter()) {
            writeClassMethod(cls, ClassUtil.getSimpleClassName(cls), cls.getSuperclass(), packageName, fieldTypes, constructor, copyMethod, fluentSetter,
                    parentPropertyModeForHashEquals, parentPropertyModeForToString, fieldName2MethodName, importedClasses, utilClass, writer);

            int start = -1, end = -1;

            for (int i = 0, size = lines.size(); i < size; i++) {
                if (lines.get(i).indexOf("=====>") > 0) {
                    start = i;
                } else if (lines.get(i).indexOf("<=====") > 0) {
                    end = i;
                }
            }

            final List<String> newLines = new ArrayList<>();

            if (start >= 0 && end >= 0) {
                newLines.addAll(lines.subList(0, start + 1));
                newLines.add(writer.toString());
                newLines.addAll(lines.subList(end, lines.size()));
            } else {
                for (int i = 0, size = lines.size(); i < size; i++) {
                    if (lines.get(i).indexOf(") {") > 0) {
                        String tmp = lines.get(i).trim();
                        if (tmp.startsWith("public ") && (tmp.indexOf(cls.getSimpleName()) > 0 || tmp.indexOf(" get") > 0 || tmp.indexOf(" set") > 0)) {
                            start = i;
                            while (start-- > 0 && lines.get(start).trim().startsWith("@")) {

                            }

                            while (lines.get(start).trim().length() == 0 && start-- > 0) {

                            }

                            break;
                        }
                    }
                }

                if (start < 0) {
                    for (int i = lines.size() - 1; i >= 0; i--) {
                        if (lines.get(i).trim().startsWith("}")) {
                            start = i - 1;

                            break;
                        }
                    }
                }

                newLines.addAll(lines.subList(0, start + 1));
                newLines.add(writer.toString());
                newLines.add("}");
            }

            if (!ClassUtil.getPackageName(utilClass).equals(packageName) && !_N.equals(utilClass) && fieldTypes.size() > 0) {
                String importUtilClass = "import " + utilClass.getCanonicalName() + ";";

                for (int i = 0, size = newLines.size(); i < size; i++) {
                    if (newLines.get(i).indexOf(importUtilClass) >= 0) {
                        break;
                    } else if (newLines.get(i).indexOf("public ") >= 0) {

                        int j = i;
                        while (j-- >= 0) {
                            if (newLines.get(j).startsWith("import") || newLines.get(j).startsWith("package")) {
                                break;
                            }
                        }

                        newLines.add(j + 1, N.EMPTY_STRING);
                        newLines.add(j + 2, importUtilClass);

                        if (newLines.get(j + 3).trim().length() > 0) {
                            newLines.add(j + 3, N.EMPTY_STRING);
                        }

                        break;
                    }
                }
            }

            if (hasGenericTypeField) {
                //                String importTypeClass = "import com.landawn.abacus.annotation.Type;";
                //
                //                for (int i = 0, size = newLines.size(); i < size; i++) {
                //                    if (newLines.get(i).indexOf(importTypeClass) >= 0) {
                //                        break;
                //                    } else if (newLines.get(i).indexOf("public ") >= 0) {
                //                        int ins = 0;
                //                        if (newLines.get(i - 1).trim().length() > 0) {
                //                            newLines.add(i + ins++, N.EMPTY_STRING);
                //                        }
                //                        newLines.add(i + ins++, importTypeClass);
                //                        newLines.add(i + ins++, N.EMPTY_STRING);
                //                        break;
                //                    }
                //                }
            }

            if (newLines.get(newLines.size() - 1).startsWith("}") && newLines.get(newLines.size() - 2).endsWith(IOUtil.LINE_SEPARATOR)) {
                newLines.set(newLines.size() - 2, StringUtil.chop(newLines.get(newLines.size() - 2)));
            }

            IOUtil.writeLines(clsSourceFile, newLines);

            // Add annotation back.
            Map<String, Set<String>> annoMap = new LinkedHashMap<>();
            for (int i = 0, len = lines.size(); i < len; i++) {
                String line = lines.get(i);
                if (line.trim().startsWith("@")) {
                    Set<String> tmp = N.newLinkedHashSet();
                    tmp.add(line);
                    while (++i < len && lines.get(i).trim().startsWith("@")) {
                        tmp.add(lines.get(i));
                    }

                    while (i < len && lines.get(i).trim().equals("")) {
                        i++;
                    }

                    annoMap.put(lines.get(i), tmp);
                }
            }

            if (N.notNullOrEmpty(annoMap)) {
                final List<String> finalLines = new ArrayList<>();
                final List<String> srcLines = IOUtil.readAllLines(clsSourceFile);

                for (int i = 0, len = srcLines.size(); i < len; i++) {
                    final String line = srcLines.get(i);
                    final Set<String> annons = annoMap.get(line);

                    if (N.notNullOrEmpty(annons)) {
                        int j = finalLines.size() - 1;

                        while (j >= 0 && finalLines.get(j).trim().startsWith("@")) {
                            annons.add(finalLines.remove(j--));
                        }

                        finalLines.addAll(annoMap.get(line));
                    }

                    finalLines.add(line);
                }

                IOUtil.writeLines(clsSourceFile, finalLines);
            }

        } catch (IOException | NoSuchFieldException | SecurityException e) {
            throw N.toRuntimeException(e);
        }
    }

    //    public static void generateEntity(File srcDir, String packageName, Map<String, LinkedHashMap<String, ?>> classNameFields) {
    //        generateEntity(srcDir, packageName, classNameFields, false, false, false);
    //    }
    //
    //    /**
    //     *
    //     * @param srcDir
    //     * @param packageName
    //     * @param classNameFields
    //     * @param constructor
    //     * @param copyMethod
    //     * @param fluentSetter
    //     */
    //    public static void generateEntity(File srcDir, String packageName, Map<String, LinkedHashMap<String, ?>> classNameFields, final boolean constructor,
    //            final boolean copyMethod, final boolean fluentSetter) {
    //        generateEntity(srcDir, packageName, classNameFields, constructor, copyMethod, fluentSetter, null, ParentPropertyMode.NONE, ParentPropertyMode.NONE);
    //    }
    //
    //    /**
    //     *
    //     * @param srcDir
    //     * @param packageName
    //     * @param classNameFields
    //     * @param constructor
    //     * @param copyMethod
    //     * @param fluentSetter
    //     * @param parentClass
    //     * @param parentPropertyModeForHashEquals
    //     * @param parentPropertyModeForToString
    //     */
    //    public static void generateEntity(File srcDir, String packageName, Map<String, LinkedHashMap<String, ?>> classNameFields, final boolean constructor,
    //            final boolean copyMethod, final boolean fluentSetter, Class<?> parentClass, final ParentPropertyMode parentPropertyModeForHashEquals,
    //            final ParentPropertyMode parentPropertyModeForToString) {
    //
    //        for (String className : classNameFields.keySet()) {
    //            generateEntity(srcDir, packageName, className, classNameFields.get(className), constructor, copyMethod, fluentSetter, parentClass,
    //                    parentPropertyModeForHashEquals, parentPropertyModeForToString);
    //        }
    //    }

    //    /**
    //     * Generate and Print out the methods according to fields defined the in specified class.
    //     *
    //     * @param cls
    //     */
    //    public static void printClassMethod(final Class<?> cls) {
    //        printClassMethod(cls, false, false, false, null, null);
    //    }
    //
    //    /**
    //     * Generate and Print out the methods according to fields defined the in specified class.
    //     *
    //     * @param cls
    //     * @param constructor generate constructor
    //     * @param copyMethod generate the copy method.
    //     * @param fluentSetter
    //     * @param ignoreFieldNames
    //     * @param fieldName2MethodName
    //     */
    //    public static void printClassMethod(final Class<?> cls, final boolean constructor, final boolean copyMethod, final boolean fluentSetter,
    //            Set<String> ignoreFieldNames, final Map<String, String> fieldName2MethodName) {
    //        printClassMethod(cls, constructor, copyMethod, fluentSetter, ignoreFieldNames, fieldName2MethodName, ParentPropertyMode.FIRST,
    //                ParentPropertyMode.FIRST);
    //    }
    //
    //    /**
    //     * Generate and Print out the methods according to fields defined the in specified class.
    //     *
    //     * @param cls
    //     * @param constructor
    //     * @param copyMethod
    //     * @param fluentSetter
    //     * @param ignoreFieldNames
    //     * @param fieldName2MethodName
    //     * @param parentPropertyModeForHashEquals
    //     * @param parentPropertyModeForToString
    //     */
    //    public static void printClassMethod(final Class<?> cls, final boolean constructor, final boolean copyMethod, final boolean fluentSetter,
    //            Set<String> ignoreFieldNames, final Map<String, String> fieldName2MethodName, final ParentPropertyMode parentPropertyModeForHashEquals,
    //            final ParentPropertyMode parentPropertyModeForToString) {
    //        if (ignoreFieldNames == null) {
    //            ignoreFieldNames = N.newLinkedHashSet();
    //        }
    //
    //        final Map<String, Type<?>> fieldTypes = new LinkedHashMap<>();
    //
    //        for (Field field : cls.getDeclaredFields()) {
    //            final String fieldName = field.getName();
    //            if (Modifier.isStatic(field.getModifiers()) || ignoreFieldNames.contains(fieldName)) {
    //                continue;
    //            } else {
    //                fieldTypes.put(fieldName, N.typeOf(field.getType()));
    //            }
    //        }
    //
    //        try (Writer writer = new OutputStreamWriter(System.out)) {
    //            printClassMethod(cls, ClassUtil.getSimpleClassName(cls), cls.getSuperclass(), cls.getPackage() == null ? null : ClassUtil.getPackageName(cls),
    //                    fieldTypes, constructor, copyMethod, fluentSetter, parentPropertyModeForHashEquals, parentPropertyModeForToString, fieldName2MethodName,
    //                    new LinkedHashMap<String, Class<?>>(), writer);
    //        } catch (IOException | NoSuchFieldException | SecurityException e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }

    /**
     * Write class method.
     *
     * @param cls
     * @param className
     * @param parentClass
     * @param pkgName
     * @param fieldTypes
     * @param constructor
     * @param copyMethod
     * @param fluentSetter
     * @param parentPropertyModeForHashEquals
     * @param parentPropertyModeForToString
     * @param fieldName2MethodName
     * @param importedClasses
     * @param utilClass
     * @param writer
     * @throws NoSuchFieldException the no such field exception
     * @throws SecurityException the security exception
     */
    private static void writeClassMethod(Class<?> cls, final String className, final Class<?> parentClass, final String pkgName,
            final Map<String, Type<?>> fieldTypes, final boolean constructor, final boolean copyMethod, final boolean fluentSetter,
            ParentPropertyMode parentPropertyModeForHashEquals, ParentPropertyMode parentPropertyModeForToString, Map<String, String> fieldName2MethodName,
            final Map<String, Class<?>> importedClasses, final Class<?> utilClass, Writer writer) throws NoSuchFieldException, SecurityException {

        if (N.isNullOrEmpty(fieldTypes) && fluentSetter == false && constructor == false && copyMethod == false) {
            return;
        }

        if (parentPropertyModeForHashEquals == null) {
            parentPropertyModeForHashEquals = ParentPropertyMode.NONE;
        }

        if (parentPropertyModeForToString == null) {
            parentPropertyModeForToString = ParentPropertyMode.NONE;
        }

        if (fieldName2MethodName == null) {
            fieldName2MethodName = new LinkedHashMap<>();
        }

        final String utilClassName = utilClass.getSimpleName();
        final List<Method> parentGetterMethods = new ArrayList<>();
        final Map<String, Method> parentSettterMethods = new LinkedHashMap<>();
        final List<Class<?>> allClasses = new ArrayList<>();

        if (parentClass != null) {
            allClasses.add(parentClass);

            while (allClasses.get(allClasses.size() - 1).getSuperclass() != null) {
                allClasses.add(allClasses.get(allClasses.size() - 1).getSuperclass());
            }

            for (Class<?> superClass : allClasses) {
                parentGetterMethods.addAll(ClassUtil.getPropGetMethods(superClass).values());
                parentSettterMethods.putAll(ClassUtil.getPropSetMethods(superClass));
            }
        }

        final String iden = "    ";

        if (constructor) {
            IOUtil.writeLine(writer, N.EMPTY_STRING);
            IOUtil.writeLine(writer, iden + "public " + className + "() {");

            if (parentClass != null && AbstractDirtyMarker.class.isAssignableFrom(parentClass)) {
                IOUtil.writeLine(writer, iden + iden + "super(" + className + ".class.getSimpleName());");
            }

            IOUtil.writeLine(writer, iden + "}");

            IOUtil.writeLine(writer, N.EMPTY_STRING);
            String parameterStr = "";
            String signValues = "";

            for (Map.Entry<String, Method> entry : parentSettterMethods.entrySet()) {
                if (parameterStr.length() > 0) {
                    parameterStr += ", ";
                }

                parameterStr += (getParameterTypeName(pkgName, entry));

                if (signValues.length() > 0) {
                    signValues += IOUtil.LINE_SEPARATOR;
                }

                signValues += (iden + iden + "this." + entry.getValue().getName() + "(" + entry.getKey() + ");");
            }

            for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
                if (parameterStr.length() > 0) {
                    parameterStr += ", ";
                }

                parameterStr += (getSimpleType(entry.getValue(), pkgName, importedClasses) + " " + entry.getKey());

                if (signValues.length() > 0) {
                    signValues += IOUtil.LINE_SEPARATOR;
                }

                signValues += (iden + iden + "this." + entry.getKey() + " = " + entry.getKey() + ";");
            }

            if (parameterStr.length() > 0) {
                IOUtil.writeLine(writer, iden + "public " + className + "(" + parameterStr + ") {");

                if (parentClass != null && AbstractDirtyMarker.class.isAssignableFrom(parentClass)) {
                    IOUtil.writeLine(writer, iden + iden + "super(" + className + ".class.getSimpleName());");
                    IOUtil.writeLine(writer, N.EMPTY_STRING);
                }

                IOUtil.writeLine(writer, signValues);
                IOUtil.writeLine(writer, iden + "}");
            }
        }

        if (fluentSetter && N.notNullOrEmpty(parentSettterMethods)) {
            for (Map.Entry<String, Method> entry : parentSettterMethods.entrySet()) {
                if (parentClass.isAssignableFrom(entry.getValue().getReturnType()) == false) {
                    continue;
                }

                final String methodName = entry.getValue().getName();

                IOUtil.writeLine(writer, N.EMPTY_STRING);
                IOUtil.writeLine(writer, iden + "public " + className + " " + methodName + "(" + getParameterTypeName(pkgName, entry) + ") {");
                IOUtil.writeLine(writer, iden + iden + "super." + methodName + "(" + entry.getKey() + ");");
                IOUtil.writeLine(writer, N.EMPTY_STRING);
                IOUtil.writeLine(writer, iden + iden + "return this;");
                IOUtil.writeLine(writer, iden + "}");
            }
        }

        for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
            final String fieldName = entry.getKey();
            final String simpleTypeName = getSimpleType(entry.getValue(), pkgName, importedClasses);
            //            final String getPrefix = boolean.class.equals(entry.getValue().getTypeClass()) || Boolean.class.equals(entry.getValue().getTypeClass()) ? "is"
            //                    : "get";
            final String postfix = fieldName2MethodName.containsKey(fieldName) ? fieldName2MethodName.get(fieldName)
                    : (StringUtil.isAllUpperCase(fieldName) ? fieldName : StringUtil.capitalize(fieldName));

            IOUtil.writeLine(writer, N.EMPTY_STRING);

            // final String annoTypeName = getAnnoType(entry.getValue(), pkgName, importedClasses);

            // IOUtil.writeLine(writer, iden + "@Type(\"" + entry.getValue().getName() + "\")");
            //            if (!entry.getValue().getName().equals(annoTypeName) || N.notNullOrEmpty(entry.getValue().getParameterTypes())) {
            //                IOUtil.writeLine(writer, iden + "@Type(\"" + annoTypeName + "\")");
            //            }

            IOUtil.writeLine(writer, iden + "public " + simpleTypeName + " get" + postfix + "() {");
            IOUtil.writeLine(writer, iden + iden + "return " + fieldName + ";");
            IOUtil.writeLine(writer, iden + "}");

            if (cls == null || Modifier.isFinal(cls.getDeclaredField(fieldName).getModifiers()) == false) {
                IOUtil.writeLine(writer, N.EMPTY_STRING);
                if (fluentSetter) {
                    IOUtil.writeLine(writer, iden + "public " + className + " set" + postfix + "(" + simpleTypeName + " " + fieldName + ") {");
                } else {
                    IOUtil.writeLine(writer, iden + "public void set" + postfix + "(" + simpleTypeName + " " + fieldName + ") {");
                }

                if (parentClass != null && AbstractDirtyMarker.class.isAssignableFrom(parentClass)) {
                    IOUtil.writeLine(writer, iden + iden + "super.setUpdatedPropName(\"" + fieldName + "\");");
                }

                IOUtil.writeLine(writer, iden + iden + "this." + fieldName + " = " + fieldName + ";");

                if (fluentSetter) {
                    IOUtil.writeLine(writer, N.EMPTY_STRING);
                    IOUtil.writeLine(writer, iden + iden + "return this;");
                }

                IOUtil.writeLine(writer, iden + "}");
            }
        }

        if (copyMethod) {
            IOUtil.writeLine(writer, N.EMPTY_STRING);
            IOUtil.writeLine(writer, iden + "public " + className + " copy() {");
            IOUtil.writeLine(writer, iden + iden + "final " + className + " copy = new " + className + "();");
            IOUtil.writeLine(writer, N.EMPTY_STRING);

            for (Method method : parentGetterMethods) {
                IOUtil.writeLine(writer,
                        iden + iden + "copy." + ClassUtil.getPropSetMethod(method.getDeclaringClass(), ClassUtil.getPropNameByMethod(method)).getName()
                                + "(this." + method.getName() + "());");
            }

            for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
                IOUtil.writeLine(writer, iden + iden + "copy." + entry.getKey() + " = this." + entry.getKey() + ";");
            }

            IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + "return copy;");
            IOUtil.writeLine(writer, iden + "}");
        }

        if (N.notNullOrEmpty(fieldTypes)) {
            {
                IOUtil.writeLine(writer, N.EMPTY_STRING);
                IOUtil.writeLine(writer, iden + "@Override");
                IOUtil.writeLine(writer, iden + "public int hashCode() {");
                IOUtil.writeLine(writer, iden + iden + "int h = 17;");

                if (parentPropertyModeForHashEquals == ParentPropertyMode.FIRST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        IOUtil.writeLine(writer, iden + iden + "h = 31 * h + " + utilClassName + ".hashCode(" + method.getName() + "());");
                    }
                }

                for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
                    IOUtil.writeLine(writer, iden + iden + "h = 31 * h + " + utilClassName + ".hashCode(" + entry.getKey() + ");");
                }

                if (parentPropertyModeForHashEquals == ParentPropertyMode.LAST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        IOUtil.writeLine(writer, iden + iden + "h = 31 * h + " + utilClassName + "hashCode(" + method.getName() + "());");
                    }
                }

                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + "return h;");
                IOUtil.writeLine(writer, iden + "}");
            }

            {
                IOUtil.writeLine(writer, N.EMPTY_STRING);
                IOUtil.writeLine(writer, iden + "@Override");
                IOUtil.writeLine(writer, iden + "public boolean equals(Object obj) {");
                IOUtil.writeLine(writer, iden + iden + "if (this == obj) {");
                IOUtil.writeLine(writer, iden + iden + iden + "return true;");
                IOUtil.writeLine(writer, iden + iden + "}");

                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + "if (obj instanceof " + className + ") {");
                IOUtil.writeLine(writer, iden + iden + iden + "final " + className + " other = (" + className + ") obj;");

                int i = 0;

                if (parentPropertyModeForHashEquals == ParentPropertyMode.FIRST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        if (i++ == 0) {
                            if (i == parentGetterMethods.size() + fieldTypes.size()) {
                                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + method.getName()
                                        + "(), other." + method.getName() + "());");
                            } else {
                                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + method.getName()
                                        + "(), other." + method.getName() + "())");
                            }
                        } else {
                            if (i == parentGetterMethods.size() + fieldTypes.size()) {
                                IOUtil.writeLine(writer, iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + method.getName() + "(), other."
                                        + method.getName() + "());");
                            } else {
                                IOUtil.writeLine(writer, iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + method.getName() + "(), other."
                                        + method.getName() + "())");
                            }
                        }
                    }
                }

                for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
                    final String fieldName = entry.getKey();

                    if (i++ == 0) {
                        if (i == fieldTypes.size() && (parentGetterMethods.size() == 0 || parentPropertyModeForHashEquals != ParentPropertyMode.LAST)) {
                            IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + fieldName
                                    + ", other." + fieldName + ");");
                        } else {
                            IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + fieldName
                                    + ", other." + fieldName + ")");
                        }
                    } else {
                        if (((parentPropertyModeForHashEquals != ParentPropertyMode.FIRST && i == fieldTypes.size())
                                || (parentPropertyModeForHashEquals == ParentPropertyMode.FIRST && i == fieldTypes.size() + parentGetterMethods.size()))
                                && (parentGetterMethods.size() == 0 || parentPropertyModeForHashEquals != ParentPropertyMode.LAST)) {
                            IOUtil.writeLine(writer,
                                    iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + fieldName + ", other." + fieldName + ");");
                        } else {
                            IOUtil.writeLine(writer, iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + fieldName + ", other." + fieldName + ")");
                        }
                    }
                }

                if (parentPropertyModeForHashEquals == ParentPropertyMode.LAST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        if (i++ == 0) {
                            if (i == parentGetterMethods.size() + fieldTypes.size()) {
                                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + method.getName()
                                        + "(), other." + method.getName() + "());");
                            } else {
                                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + iden + "return " + utilClassName + ".equals(" + method.getName()
                                        + "(), other." + method.getName() + "())");
                            }
                        } else {
                            if (i == parentGetterMethods.size() + fieldTypes.size()) {
                                IOUtil.writeLine(writer, iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + method.getName() + "(), other."
                                        + method.getName() + "());");
                            } else {
                                IOUtil.writeLine(writer, iden + iden + iden + iden + "&& " + utilClassName + ".equals(" + method.getName() + "(), other."
                                        + method.getName() + "())");
                            }
                        }
                    }
                }

                IOUtil.writeLine(writer, iden + iden + "}");
                IOUtil.writeLine(writer, IOUtil.LINE_SEPARATOR + iden + iden + "return false;");
                IOUtil.writeLine(writer, iden + "}");
            }

            {
                final StringBuilder sb = new StringBuilder();
                IOUtil.writeLine(writer, N.EMPTY_STRING);
                sb.append(iden + "@Override" + IOUtil.LINE_SEPARATOR);
                sb.append(iden + "public String toString() {" + IOUtil.LINE_SEPARATOR);

                int i = 0;

                if (parentPropertyModeForToString == ParentPropertyMode.FIRST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        if (i++ == 0) {
                            sb.append(iden + iden + "return \"{" + ClassUtil.getPropNameByMethod(method) + "=\" + " + utilClassName + ".toString("
                                    + method.getName() + "())");
                        } else {
                            sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \", " + ClassUtil.getPropNameByMethod(method) + "=\" + " + utilClassName
                                    + ".toString(" + method.getName() + "())");
                        }

                        if (i == parentGetterMethods.size() + fieldTypes.size()) {
                            if (i > 1) {
                                sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \"}\";" + IOUtil.LINE_SEPARATOR);
                            } else {
                                sb.append(" + \"}\";" + IOUtil.LINE_SEPARATOR);
                            }
                        }
                    }
                }

                for (Map.Entry<String, Type<?>> entry : fieldTypes.entrySet()) {
                    final String fieldName = entry.getKey();

                    if (i++ == 0) {
                        sb.append(iden + iden + "return \"{" + fieldName + "=\" + " + utilClassName + ".toString(" + fieldName + ")");
                    } else {
                        sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \", " + fieldName + "=\" + " + utilClassName + ".toString(" + fieldName
                                + ")");
                    }

                    if ((((parentPropertyModeForToString == null || parentPropertyModeForToString == ParentPropertyMode.NONE)
                            || (parentPropertyModeForToString == ParentPropertyMode.LAST && parentGetterMethods.size() == 0)) && i == fieldTypes.size())
                            || (parentPropertyModeForToString == ParentPropertyMode.FIRST && i == parentGetterMethods.size() + fieldTypes.size())) {
                        if (i > 1) {
                            sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \"}\";" + IOUtil.LINE_SEPARATOR);
                        } else {
                            sb.append(" + \"}\";" + IOUtil.LINE_SEPARATOR);
                        }
                    }
                }

                if (parentPropertyModeForToString == ParentPropertyMode.LAST && parentGetterMethods.size() > 0) {
                    for (Method method : parentGetterMethods) {
                        if (i++ == 0) {
                            sb.append(iden + iden + "return \"{" + ClassUtil.getPropNameByMethod(method) + "=\" + " + utilClassName + ".toString("
                                    + method.getName() + "())");
                        } else {
                            sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \", " + ClassUtil.getPropNameByMethod(method) + "=\" + " + utilClassName
                                    + ".toString(" + method.getName() + "())");
                        }

                        if (i == parentGetterMethods.size() + fieldTypes.size()) {
                            if (i > 1) {
                                sb.append(IOUtil.LINE_SEPARATOR + iden + iden + "         + \"}\";" + IOUtil.LINE_SEPARATOR);
                            } else {
                                sb.append(" + \"}\";" + IOUtil.LINE_SEPARATOR);
                            }
                        }
                    }
                }

                sb.append(iden + "}");

                IOUtil.writeLine(writer, sb.toString());
            }
        }
    }

    /**
     * Gets the parameter type name.
     *
     * @param pkgName
     * @param entry
     * @return
     */
    private static String getParameterTypeName(final String pkgName, Map.Entry<String, Method> entry) {
        String paraTypeName = ClassUtil.getParameterizedTypeNameByMethod(entry.getValue());

        if (N.notNullOrEmpty(pkgName)) {
            String tmp = pkgName + ".";
            int idx = 0;
            char ch = 0;

            while ((idx = paraTypeName.indexOf(tmp, idx)) >= 0) {
                for (int i = idx + tmp.length(), len = paraTypeName.length(); i < len; i++) {
                    ch = paraTypeName.charAt(i);

                    if ((Character.isLetterOrDigit(ch) || ch == '$' || ch == '_') && i != len - 1) {
                        continue;
                    } else if (ch == '.') {
                        idx = i;
                        break;
                    } else {
                        paraTypeName = paraTypeName.replace(paraTypeName.substring(idx, i), paraTypeName.substring(idx + tmp.length(), i));
                        idx += (i - idx - tmp.length());
                        break;
                    }
                }
            }
        }

        return paraTypeName + " " + entry.getKey();
    }

    /**
     * Write util class for hash equals to string.
     *
     * @param srcDir
     * @param pkgName
     */
    public static void writeUtilClassForHashEqualsToString(final File srcDir, final String pkgName) {
        writeUtilClassForHashEqualsToString(srcDir, pkgName, "_N");
    }

    /**
     * Write util class for hash equals to string.
     *
     * @param srcDir
     * @param pkgName
     * @param utilClassName
     */
    public static void writeUtilClassForHashEqualsToString(final File srcDir, final String pkgName, final String utilClassName) {
        final String utilClassFilePath = srcDir.getAbsolutePath()
                + (N.isNullOrEmpty(pkgName) ? "" : IOUtil.FILE_SEPARATOR + StringUtil.replaceAll(pkgName, ".", IOUtil.FILE_SEPARATOR)) + IOUtil.FILE_SEPARATOR
                + utilClassName + ".java";
        final File utilClassFile = new File(utilClassFilePath);

        if (utilClassFile.exists() == false && IOUtil.createIfNotExists(utilClassFile) == false) {
            throw new RuntimeException("Failed to create new File by path: " + utilClassFilePath);
        }

        if (N.isNullOrEmpty(pkgName)) {
            IOUtil.write(utilClassFile, _N_STRING.replaceFirst("package com.landawn.abacus.util;", "").replaceAll("_N", utilClassName));
        } else {
            IOUtil.write(utilClassFile, _N_STRING.replaceFirst("com.landawn.abacus.util", pkgName).replaceAll("_N", utilClassName));
        }
    }

    /**
     * Prints the transfer method.
     *
     * @param sourceClass
     * @param targetClass
     */
    public static void printTransferMethod(final Class<?> sourceClass, final Class<?> targetClass) {
        printTransferMethod(sourceClass, targetClass, null);
    }

    /**
     * Prints the transfer method.
     *
     * @param sourceClass
     * @param targetClass
     * @param propNameMapping
     */
    public static void printTransferMethod(final Class<?> sourceClass, final Class<?> targetClass, final Map<String, String> propNameMapping) {
        final String iden = "    ";
        final String srcClassName = sourceClass.getSimpleName();
        final String targetClassName = targetClass.getSimpleName();

        StringBuilder sb = new StringBuilder();
        sb.append("public static ")
                .append(targetClassName)
                .append(" ")
                .append(ClassUtil.formalizePropName(srcClassName))
                .append("2")
                .append(targetClassName)
                .append(" (")
                .append(srcClassName)
                .append(" source) {")
                .append(IOUtil.LINE_SEPARATOR);
        sb.append(iden).append("final ").append(targetClassName).append(" result = new ").append(targetClassName).append("();").append(IOUtil.LINE_SEPARATOR);

        for (Map.Entry<String, Method> entry : ClassUtil.getPropGetMethods(sourceClass).entrySet()) {
            final Method getMethod = entry.getValue();
            String propName = entry.getKey();

            if (propNameMapping != null && propNameMapping.containsKey(propName)) {
                propName = propNameMapping.get(propName);
            }

            final Method setMethod = ClassUtil.getPropSetMethod(targetClass, propName);

            if (setMethod == null) {
                sb.append(iden).append("// No set method found for: source.").append(getMethod.getName()).append("()").append(IOUtil.LINE_SEPARATOR);
            } else if (!setMethod.getParameterTypes()[0].isAssignableFrom(getMethod.getReturnType())) {
                sb.append(iden).append("// Incompatible parameter type for: source.").append(getMethod.getName()).append("()").append(IOUtil.LINE_SEPARATOR);
            } else {
                sb.append(iden)
                        .append("result.")
                        .append(setMethod.getName())
                        .append("(source.")
                        .append(getMethod.getName())
                        .append("());")
                        .append(IOUtil.LINE_SEPARATOR);
            }
        }

        sb.append(iden).append("return result; ").append(IOUtil.LINE_SEPARATOR);
        sb.append("}").append(IOUtil.LINE_SEPARATOR);

        System.out.println(IOUtil.LINE_SEPARATOR);
        System.out.println(sb.toString());
        System.out.println(IOUtil.LINE_SEPARATOR);
    }

    /**
     * Gets the simple type.
     *
     * @param type
     * @param pkgName
     * @param importedClasses
     * @return
     */
    private static String getSimpleType(Type<?> type, final String pkgName, final Map<String, Class<?>> importedClasses) {
        final Class<?> typeClass = type.clazz();

        String typeName = null;

        if (type.isGenericType()) {
            typeName = type.name();
        } else {
            Class<?> clazz = type.clazz();
            typeName = Object.class.equals(clazz) && !type.name().equals(ObjectType.OBJECT) ? type.name() : clazz.getCanonicalName();
        }

        if (typeClass.isArray()) {
            String componentClassName = StringUtil.substring(typeName, 0, typeName.indexOf('[')).get();
            if (importedClasses.containsKey(componentClassName)) {
                typeName = typeName.replaceAll(componentClassName.substring(0, componentClassName.lastIndexOf('.') + 1), "");
            }
        } else if (typeName.startsWith("java.lang.") || (importedClasses.containsValue(typeClass) && N.notNullOrEmpty(ClassUtil.getPackageName(typeClass)))) {
            typeName = typeName.replace(ClassUtil.getPackageName(typeClass) + ".", "");
        }

        if (type.isGenericType()) {
            Type<?>[] paramTypes = type.getParameterTypes();

            if (N.notNullOrEmpty(paramTypes)) {
                String tmp = typeName.substring(0, typeName.indexOf('<')) + "<";

                for (int i = 0, len = paramTypes.length; i < len; i++) {
                    if (i > 0) {
                        tmp += ", ";
                    }

                    tmp += getSimpleType(paramTypes[i], pkgName, importedClasses);
                }

                tmp += ">";
                typeName = tmp;

                //                for (Type<?> paraType : parameterTypes) {
                //                    try {
                //                        if (isUsualType(paraType.getTypeClass().getCanonicalName())) {
                //                            typeName = typeName.replace(ClassUtil.getPackageName(paraType.getTypeClass()) + ".", "");
                //                        }
                //                    } catch (Exception e) {
                //                        // ignore;
                //                    }
                //                }
            }
        }

        if ((ClassUtil.getPackageName(TypeType.class) + "." + TypeType.TYPE).equals(typeName)) {
            typeName = typeName + "<Object>";
        }

        if (N.notNullOrEmpty(pkgName)) {
            if ((typeName.indexOf(pkgName) == 0) && (typeName.lastIndexOf(WD._PERIOD) == pkgName.length())) {
                typeName = typeName.replace(pkgName + WD._PERIOD, "");
            }
        }
        return typeName;
    }

    /**
     * The Enum ParentPropertyMode.
     */
    public enum ParentPropertyMode {

        /** The none. */
        NONE,
        /** The first. */
        FIRST,
        /** The last. */
        LAST
    }

    static final String _N_STRING = "/*\r\n" + " * Licensed to the Apache Software Foundation (ASF) under one or more\r\n"
            + " * contributor license agreements.  See the NOTICE file distributed with\r\n"
            + " * this work for additional information regarding copyright ownership.\r\n"
            + " * The ASF licenses this file to You under the Apache License, Version 2.0\r\n"
            + " * (the \"License\"); you may not use this file except in compliance with\r\n" + " * the License.  You may obtain a copy of the License at\r\n"
            + " *\r\n" + " *      http://www.apache.org/licenses/LICENSE-2.0\r\n" + " *\r\n"
            + " * Unless required by applicable law or agreed to in writing, software\r\n"
            + " * distributed under the License is distributed on an \"AS IS\" BASIS,\r\n"
            + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\r\n"
            + " * See the License for the specific language governing permissions and\r\n" + " * limitations under the License.\r\n" + " */\r\n"
            + "package com.landawn.abacus.util;\r\n" + "\r\n" + "import java.util.ArrayList;\r\n" + "import java.util.HashMap;\r\n"
            + "import java.util.HashSet;\r\n" + "import java.util.LinkedHashMap;\r\n" + "import java.util.LinkedHashSet;\r\n"
            + "import java.util.LinkedList;\r\n" + "import java.util.List;\r\n" + "import java.util.Map;\r\n" + "import java.util.Set;\r\n" + "\r\n"
            + "// TODO: Auto-generated Javadoc\r\n" + "/**\r\n" + " * The Class _N.\r\n" + " *\r\n" + " * @author Haiyang Li\r\n" + " * @since 0.8\r\n"
            + " */\r\n" + "public final class _N {\r\n" + "\r\n" + "    /**\r\n" + "     * Instantiates a new  n.\r\n" + "     */\r\n"
            + "    private _N() {\r\n" + "        //singleton\r\n" + "    }\r\n" + "\r\n" + "    /** The Constant MAX_ARRAY_SIZE. */\r\n"
            + "    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;\r\n" + "\r\n" + "    /** The Constant MAX_HASH_LENGTH. */\r\n"
            + "    private static final int MAX_HASH_LENGTH = (int) (MAX_ARRAY_SIZE / 1.25) - 1;\r\n" + "\r\n" + "    /** The Constant NULL_STRING. */\r\n"
            + "    private static final String NULL_STRING = \"null\";\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final boolean a, final boolean b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final char a, final char b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final byte a, final byte b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final short a, final short b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final int a, final int b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final long a, final long b) {\r\n" + "        return a == b;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final float a, final float b) {\r\n" + "        return Float.compare(a, b) == 0;\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n"
            + "     */\r\n" + "    public static boolean equals(final double a, final double b) {\r\n" + "        return Double.compare(a, b) == 0;\r\n"
            + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n"
            + "     * @return true, if successful\r\n" + "     */\r\n" + "    public static boolean equals(final String a, final String b) {\r\n"
            + "        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n"
            + "     */\r\n" + "    public static boolean equals(final Object a, final Object b) {\r\n"
            + "        return (a == null) ? b == null : (b == null ? false : a.equals(b));\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final boolean[] a, final boolean[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final boolean[] a, final int fromIndex, final int toIndex, final boolean[] b) {\r\n"
            + "        if (a == b) {\r\n" + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final char[] a, final char[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final char[] a, final int fromIndex, final int toIndex, final char[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final byte[] a, final byte[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final byte[] a, final int fromIndex, final int toIndex, final byte[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final short[] a, final short[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final short[] a, final int fromIndex, final int toIndex, final short[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final int[] a, final int[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final int[] a, final int fromIndex, final int toIndex, final int[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final long[] a, final long[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final long[] a, final int fromIndex, final int toIndex, final long[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (a[i] != b[i]) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final float[] a, final float[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final float[] a, final int fromIndex, final int toIndex, final float[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (Float.compare(a[i], b[i]) != 0) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final double[] a, final double[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final double[] a, final int fromIndex, final int toIndex, final double[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (Double.compare(a[i], b[i]) != 0) {\r\n" + "                return false;\r\n" + "            }\r\n" + "        }\r\n" + "\r\n"
            + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final Object[] a, final Object[] b) {\r\n"
            + "        return (a == null || b == null) ? a == b : (a.length == b.length && equals(a, 0, a.length, b));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Equals.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @param b\r\n" + "     * @return true, if successful\r\n" + "     */\r\n"
            + "    public static boolean equals(final Object[] a, final int fromIndex, final int toIndex, final Object[] b) {\r\n" + "        if (a == b) {\r\n"
            + "            return true;\r\n" + "        }\r\n" + "\r\n"
            + "        if ((a == null && b != null) || (a != null && b == null) || a.length < toIndex || b.length < toIndex) {\r\n"
            + "            return false;\r\n" + "        }\r\n" + "\r\n" + "        for (int i = fromIndex; i < toIndex; i++) {\r\n"
            + "            if (!(a[i] == null ? b[i] == null : a[i].equals(b[i]))) {\r\n" + "                return false;\r\n" + "            }\r\n"
            + "        }\r\n" + "\r\n" + "        return true;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final boolean value) {\r\n"
            + "        return value ? 1231 : 1237;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final char value) {\r\n"
            + "        return value;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final byte value) {\r\n" + "        return value;\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final short value) {\r\n" + "        return value;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final int value) {\r\n" + "        return value;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final long value) {\r\n" + "        return (int) (value ^ (value >>> 32));\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final float value) {\r\n" + "        return Float.floatToIntBits(value);\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final double value) {\r\n" + "        long bits = Double.doubleToLongBits(value);\r\n" + "\r\n"
            + "        return (int) (bits ^ (bits >>> 32));\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param obj\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final Object obj) {\r\n"
            + "        if (obj == null) {\r\n" + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        return obj.hashCode();\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final boolean[] a) {\r\n" + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final boolean[] a, final int fromIndex, final int toIndex) {\r\n"
            + "        if (a == null) {\r\n" + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + (a[i] ? 1231 : 1237);\r\n" + "        }\r\n"
            + "\r\n" + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final char[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final char[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + a[i];\r\n" + "        }\r\n" + "\r\n"
            + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final byte[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final byte[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + a[i];\r\n" + "        }\r\n" + "\r\n"
            + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final short[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final short[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + a[i];\r\n" + "        }\r\n" + "\r\n"
            + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final int[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final int[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + a[i];\r\n" + "        }\r\n" + "\r\n"
            + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final long[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final long[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + (int) (a[i] ^ (a[i] >>> 32));\r\n"
            + "        }\r\n" + "\r\n" + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final float[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final float[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + Float.floatToIntBits(a[i]);\r\n" + "        }\r\n"
            + "\r\n" + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final double[] a) {\r\n"
            + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final double[] a, final int fromIndex, final int toIndex) {\r\n" + "        if (a == null) {\r\n"
            + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            long bits = Double.doubleToLongBits(a[i]);\r\n"
            + "            result = 31 * result + (int) (bits ^ (bits >>> 32));\r\n" + "        }\r\n" + "\r\n" + "        return result;\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static int hashCode(final Object[] a) {\r\n" + "        return a == null ? 0 : hashCode(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * Hash code.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @param fromIndex\r\n" + "     * @param toIndex\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static int hashCode(final Object[] a, final int fromIndex, final int toIndex) {\r\n"
            + "        if (a == null) {\r\n" + "            return 0;\r\n" + "        }\r\n" + "\r\n" + "        int result = 1;\r\n" + "\r\n"
            + "        for (int i = fromIndex; i < toIndex; i++) {\r\n" + "            result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());\r\n"
            + "        }\r\n" + "\r\n" + "        return result;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final boolean value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final char value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final byte value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final short value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final int value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final long value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final float value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param value\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final double value) {\r\n"
            + "        return String.valueOf(value);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param obj\r\n" + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final Object obj) {\r\n"
            + "        if (obj == null) {\r\n" + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        return obj.toString();\r\n"
            + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final boolean[] a) {\r\n" + "        if (a == null) {\r\n" + "            return NULL_STRING;\r\n"
            + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n" + "        }\r\n" + "\r\n"
            + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n"
            + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final boolean[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final boolean[] a) {\r\n"
            + "        if (a == null) {\r\n" + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n"
            + "            sb.append(\"[]\");\r\n" + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n"
            + "     * @param to\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final boolean[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final char[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final char[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final char[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final char[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final byte[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final byte[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final byte[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final byte[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final short[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final short[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final short[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final short[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final int[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final int[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final int[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final int[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final long[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final long[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final long[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final long[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final float[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final float[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final float[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n" + "            sb.append(\"[]\");\r\n"
            + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n"
            + "     */\r\n" + "    static void toString(final StringBuilder sb, final float[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final double[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final double[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final double[] a) {\r\n"
            + "        if (a == null) {\r\n" + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n"
            + "            sb.append(\"[]\");\r\n" + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n"
            + "     * @param to\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final double[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(a[i]);\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param a\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    public static String toString(final Object[] a) {\r\n" + "        if (a == null) {\r\n"
            + "            return NULL_STRING;\r\n" + "        }\r\n" + "\r\n" + "        if (a.length == 0) {\r\n" + "            return \"[]\";\r\n"
            + "        }\r\n" + "\r\n" + "        return toString(a, 0, a.length);\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n"
            + "     *\r\n" + "     * @param a\r\n" + "     * @param from\r\n" + "     * @param to\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    public static String toString(final Object[] a, final int from, final int to) {\r\n"
            + "        final StringBuilder sb = new StringBuilder();\r\n" + "\r\n" + "        toString(sb, a, from, to);\r\n" + "\r\n"
            + "        return sb.toString();\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n"
            + "     * @param a\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final Object[] a) {\r\n"
            + "        if (a == null) {\r\n" + "            sb.append(NULL_STRING);\r\n" + "        } else if (a.length == 0) {\r\n"
            + "            sb.append(\"[]\");\r\n" + "        } else {\r\n" + "            toString(sb, a, 0, a.length);\r\n" + "        }\r\n" + "    }\r\n"
            + "\r\n" + "    /**\r\n" + "     * To string.\r\n" + "     *\r\n" + "     * @param sb\r\n" + "     * @param a\r\n" + "     * @param from\r\n"
            + "     * @param to\r\n" + "     */\r\n" + "    static void toString(final StringBuilder sb, final Object[] a, final int from, final int to) {\r\n"
            + "        sb.append('[');\r\n" + "\r\n" + "        for (int i = from; i < to; i++) {\r\n" + "            if (i > from) {\r\n"
            + "                sb.append(\", \");\r\n" + "            }\r\n" + "\r\n" + "            sb.append(toString(a[i]));\r\n" + "        }\r\n" + "\r\n"
            + "        sb.append(']');\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * As list.\r\n" + "     *\r\n" + "     * @param <T>\r\n"
            + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n" + "    @SafeVarargs\r\n" + "    public static <T> List<T> asList(final T... a) {\r\n"
            + "        if (a == null) {\r\n" + "            return new ArrayList<>();\r\n" + "        }\r\n" + "\r\n"
            + "        final List<T> list = new ArrayList<T>(a.length);\r\n" + "\r\n" + "        for (T e : a) {\r\n" + "            list.add(e);\r\n"
            + "        }\r\n" + "\r\n" + "        return list;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * As linked list.\r\n" + "     *\r\n"
            + "     * @param <T>\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n" + "    @SafeVarargs\r\n"
            + "    public static <T> LinkedList<T> asLinkedList(final T... a) {\r\n" + "        if (a == null) {\r\n"
            + "            return new LinkedList<>();\r\n" + "        }\r\n" + "\r\n" + "        LinkedList<T> list = new LinkedList<T>();\r\n" + "\r\n"
            + "        for (T e : a) {\r\n" + "            list.add(e);\r\n" + "        }\r\n" + "\r\n" + "        return list;\r\n" + "    }\r\n" + "\r\n"
            + "    /**\r\n" + "     * As set.\r\n" + "     *\r\n" + "     * @param <T>\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    @SafeVarargs\r\n" + "    public static <T> Set<T> asSet(final T... a) {\r\n" + "        if (a == null) {\r\n"
            + "            return new HashSet<>();\r\n" + "        }\r\n" + "\r\n"
            + "        final Set<T> set = new HashSet<T>(initHashCapacity(a.length));\r\n" + "\r\n" + "        for (T e : a) {\r\n"
            + "            set.add(e);\r\n" + "        }\r\n" + "\r\n" + "        return set;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n"
            + "     * As linked hash set.\r\n" + "     *\r\n" + "     * @param <T>\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    @SafeVarargs\r\n" + "    public static <T> LinkedHashSet<T> asLinkedHashSet(final T... a) {\r\n" + "        if (a == null) {\r\n"
            + "            return new LinkedHashSet<>();\r\n" + "        }\r\n" + "\r\n"
            + "        final LinkedHashSet<T> set = new LinkedHashSet<T>(initHashCapacity(a.length));\r\n" + "\r\n" + "        for (T e : a) {\r\n"
            + "            set.add(e);\r\n" + "        }\r\n" + "\r\n" + "        return set;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * As map.\r\n"
            + "     *\r\n" + "     * @param <K> the key type\r\n" + "     * @param <V> the value type\r\n" + "     * @param a\r\n" + "     * @return\r\n"
            + "     */\r\n" + "    @SuppressWarnings(\"unchecked\")\r\n" + "    @SafeVarargs\r\n"
            + "    public static <K, V> Map<K, V> asMap(final Object... a) {\r\n" + "        if (a == null) {\r\n" + "            return new HashMap<>();\r\n"
            + "        }\r\n" + "\r\n" + "        final Map<K, V> m = new HashMap<K, V>(initHashCapacity(a.length / 2));\r\n" + "\r\n"
            + "        for (int i = 0; i < a.length; i++) {\r\n" + "            m.put((K) a[i], (V) a[++i]);\r\n" + "        }\r\n" + "\r\n"
            + "        return m;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * As linked hash map.\r\n" + "     *\r\n"
            + "     * @param <K> the key type\r\n" + "     * @param <V> the value type\r\n" + "     * @param a\r\n" + "     * @return\r\n" + "     */\r\n"
            + "    @SuppressWarnings(\"unchecked\")\r\n" + "    @SafeVarargs\r\n"
            + "    public static <K, V> LinkedHashMap<K, V> asLinkedHashMap(final Object... a) {\r\n" + "        if (a == null) {\r\n"
            + "            return new LinkedHashMap<>();\r\n" + "        }\r\n" + "\r\n"
            + "        final LinkedHashMap<K, V> m = new LinkedHashMap<K, V>(initHashCapacity(a.length / 2));\r\n" + "\r\n"
            + "        for (int i = 0; i < a.length; i++) {\r\n" + "            m.put((K) a[i], (V) a[++i]);\r\n" + "        }\r\n" + "\r\n"
            + "        return m;\r\n" + "    }\r\n" + "\r\n" + "    /**\r\n" + "     * Inits the hash capacity.\r\n" + "     *\r\n" + "     * @param size\r\n"
            + "     * @return\r\n" + "     */\r\n" + "    private static int initHashCapacity(final int size) {\r\n"
            + "        return size < MAX_HASH_LENGTH ? (int) (size * 1.25) + 1 : MAX_ARRAY_SIZE;\r\n" + "    }\r\n" + "}\r\n" + "";
}
