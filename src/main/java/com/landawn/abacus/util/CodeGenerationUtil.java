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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

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
 * <p><b>Usage Examples:</b></p>
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
 *
 * <p>Every generated class or interface name must be a valid Java <i>type</i> name, which is narrower than a
 * valid Java identifier: the JLS restricted identifiers {@code permits}, {@code record}, {@code sealed},
 * {@code var} and {@code yield} cannot name a type and are rejected. Generated <i>field</i> names are ordinary
 * identifiers, so those five words remain legal there and only true keywords are prefixed with an underscore.
 */
public final class CodeGenerationUtil {

    /** Default inner interface name ({@code "x"}) for single-entity property constants. */
    public static final String X = "x";

    /**
     * Suggested top-level interface name ({@code "s"}) for standalone property-name tables.
     *
     * <p>Note: the default value used by {@link #generatePropNameTableClasses(Collection)} is
     * {@link #X}, not {@code S}. Pass {@code S} explicitly when a distinct name is desired.
     */
    public static final String S = "s";

    /** Default nested interface name ({@code "sl"}) for snake_case property constants. */
    public static final String SL = "sl";

    /** Default nested interface name ({@code "su"}) for SCREAMING_SNAKE_CASE property constants. */
    public static final String SU = "su";

    /** Default nested interface name ({@code "sf"}) for function-based property constants. */
    public static final String SF = "sf";

    /**
     * Built-in formatter for {@code min(property)} expressions.
     *
     * <p>Primitive property types are evaluated through their wrapper types. Returns {@code null}
     * for non-{@link Comparable} property types so those properties are skipped during
     * function-name generation.
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MIN_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(ClassUtil.wrap(propClass))) {
            return "min(" + propName + ")";
        }

        return null;
    };

    /**
     * Built-in formatter for {@code max(property)} expressions.
     *
     * <p>Primitive property types are evaluated through their wrapper types. Returns {@code null}
     * for non-{@link Comparable} property types so those properties are skipped during
     * function-name generation.
     */
    public static final TriFunction<Class<?>, Class<?>, String, String> MAX_FUNC = (entityClass, propClass, propName) -> {
        if (Comparable.class.isAssignableFrom(ClassUtil.wrap(propClass))) {
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

    /**
     * Opening marker written around a generated inner property-name interface, followed by the
     * interface name and {@code '>'}. Write-back deletes only what lies between a matching
     * marker pair, so hand-written code outside them can never be removed.
     */
    private static final String GENERATED_BEGIN_MARKER_PREFIX = "// <auto-generated-prop-name-table:";

    /** Closing counterpart of {@link #GENERATED_BEGIN_MARKER_PREFIX}. */
    private static final String GENERATED_END_MARKER_PREFIX = "// </auto-generated-prop-name-table:";

    /** Trailing character of both generated markers. */
    private static final String GENERATED_MARKER_SUFFIX = ">";

    /** First line of the Javadoc emitted above a generated inner property-name interface. */
    private static final String GENERATED_DOC_LINE = "* Auto-generated class for property(field) name table.";

    private CodeGenerationUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a name that will declare a generated class or interface. Such a name must be a JLS
     * {@code TypeIdentifier}, so the restricted identifiers {@code permits}, {@code record}, {@code sealed},
     * {@code var} and {@code yield} are rejected even though they are legal identifiers elsewhere.
     * @throws IllegalArgumentException if {@code identifier} is null, empty, or is not a valid Java type identifier.
     */
    private static String checkJavaTypeIdentifier(final String identifier, final String argumentName) throws IllegalArgumentException {
        N.checkArgNotEmpty(identifier, argumentName);
        N.checkArgument(Strings.isValidJavaTypeIdentifier(identifier), "%s must be a valid Java type name: %s", argumentName, identifier);

        return identifier;
    }

    /**
     * @throws IllegalArgumentException if {@code fieldName}, after Java-keyword escaping, is not a valid Java identifier.
     */
    private static void checkGeneratedFieldName(final String fieldName, final String sourceDescription) throws IllegalArgumentException {
        final String emittedName = toGeneratedFieldName(fieldName);
        N.checkArgument(Strings.isValidJavaIdentifier(emittedName), "%s produced an invalid Java field name: %s", sourceDescription, fieldName);
    }

    private static String toGeneratedFieldName(final String fieldName) {
        return Strings.isJavaKeyword(fieldName) ? "_" + fieldName : fieldName;
    }

    /**
     * @throws IllegalArgumentException if {@code fieldName} is already present in {@code generatedFieldNames}.
     */
    private static void addGeneratedFieldName(final Set<String> generatedFieldNames, final String fieldName, final String sourceDescription)
            throws IllegalArgumentException {
        N.checkArgument(generatedFieldNames.add(fieldName), "%s produced duplicate Java field name: %s", sourceDescription, fieldName);
    }

    private static String addUniqueGeneratedFieldName(final Set<String> generatedFieldNames, final String requestedFieldName) {
        String fieldName = toGeneratedFieldName(requestedFieldName);

        while (!generatedFieldNames.add(fieldName)) {
            fieldName = "_" + fieldName;
        }

        return fieldName;
    }

    private static String escapeJavaStringLiteral(final String str) {
        final StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            switch (ch) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (ch < 32 || ch == 127) {
                        sb.append('\\').append((char) ('0' + ((ch >> 6) & 7))).append((char) ('0' + ((ch >> 3) & 7))).append((char) ('0' + (ch & 7)));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }

        return sb.toString();
    }

    private static String escapeJavadocText(final String str) {
        return str.replace("&", "&amp;")
                // Java translates Unicode escapes before recognizing comments. Rendering a
                // backslash as an entity prevents text such as "\\u002a\\u002f" from becoming
                // a comment terminator in the generated source while preserving its Javadoc text.
                .replace("\\", "&#92;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("*/", "*&#47;")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    /**
     * @throws IllegalArgumentException if a component of the nonempty {@code packageName} is not a valid Java identifier.
     */
    private static void checkPackageName(final String packageName) throws IllegalArgumentException {
        if (Strings.isEmpty(packageName)) {
            return;
        }

        for (final String identifier : packageName.split("\\.", -1)) {
            N.checkArgument(Strings.isValidJavaIdentifier(identifier), "packageName must be a valid Java package name: %s", packageName);
        }
    }

    /**
     * Generates source for an inner property-name interface in the target entity.
     *
     * <p>The generated interface name defaults to {@value #X}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClass(User.class);
     * System.out.println(source);
     * }</pre>
     *
     * @param entityClass the entity class that contributes bean property names; must not be {@code null}
     * @return generated Java source that declares the inner interface and constants
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, or property names produce invalid or duplicate Java field names after Java
     *         keywords are prefixed with an underscore, or {@value #X} equals the simple name of {@code entityClass} or of any of its enclosing classes -
     *         a member type may not repeat the simple name of a class enclosing it.
     * @see #generatePropNameTableClass(Class, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass) throws IllegalArgumentException {
        return generatePropNameTableClass(entityClass, X);
    }

    /**
     * Generates source for an inner property-name interface with a custom interface name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClass(User.class, "Props");
     * System.out.println(source);
     * }</pre>
     *
     * @param entityClass the entity class that contributes bean property names; must not be {@code null}
     * @param propNameTableClassName interface name for generated constants; must be a valid Java type name
     * @return generated Java source that declares the inner interface and constants
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, or {@code propNameTableClassName} is not a valid Java type name, or
     *         property names produce invalid or duplicate Java field names after Java keywords are prefixed with an underscore. {@code "String"} is
     *         rejected because the generated interface is a member type of the entity, so that name would shadow {@code java.lang.String} throughout the
     *         entity - both in the generated constants, which are declared {@code String}, and in hand-written members this generator cannot rewrite. A
     *         name equal to the simple name of {@code entityClass} or of any of its enclosing classes is rejected for the same structural reason: a member
     *         type may not repeat the simple name of a class enclosing it. No other implicitly imported name is rejected - whether {@code Integer} or
     *         {@code Object}, say, clashes depends on the entity's own body, which this method does not read.
     * @see #generatePropNameTableClass(Class, String, String)
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName) throws IllegalArgumentException {
        return generatePropNameTableClass(entityClass, propNameTableClassName, null);
    }

    /**
     * Generates source for an inner property-name interface and optionally writes it back to disk.
     *
     * <p>The generated source is bracketed by
     * <code>// &lt;auto-generated-prop-name-table:<i>name</i>&gt;</code> and
     * <code>// &lt;/auto-generated-prop-name-table:<i>name</i>&gt;</code> marker comments.
     *
     * <p>When {@code srcDir} is not empty, this method loads the entity source file, removes the
     * previously generated interface of the same name, and inserts the new one before the entity's
     * closing brace. Removal only ever deletes:
     * <ul>
     *   <li>everything between a matching marker pair for <i>this</i> interface name, when markers
     *       are present; or</li>
     *   <li>for sources generated before markers existed, a line whose trimmed content is
     *       <b>exactly</b> {@code "public interface <name> {"} (optionally followed by
     *       {@code " // NOSONAR"}) through the next line consisting solely of {@code '}'}, plus the
     *       generated Javadoc immediately above it.</li>
     * </ul>
     * Hand-written declarations whose names merely <i>start with</i> {@code propNameTableClassName}
     * are never matched, and no other content is touched. The file is replaced through a temporary
     * sibling file, so a failure part-way through cannot truncate it.
     *
     * <p>If the entity source file does not exist, the source is returned without any file being
     * written.
     *
     * <p>Source write-back requires a JDK compiler. The source is parsed without annotation processing
     * to locate the named top-level entity and its direct members, and the proposed result is parsed
     * before replacing the file. Other declarations, comments, BOM and existing line endings are preserved.
     * Ambiguous or invalid source and raw Unicode escape spellings are rejected without writing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClass(User.class, "Props", "src/main/java");
     * System.out.println(source);
     * }</pre>
     *
     * @param entityClass the entity class that contributes bean property names; must not be {@code null}
     * @param propNameTableClassName interface name for generated constants; must be a valid Java type name
     * @param srcDir source root directory; if {@code null} or empty, source is not written
     * @return generated Java source that declares the inner interface and constants, wrapped in the
     *         marker comments described above
     * @throws IllegalArgumentException if {@code entityClass} is {@code null}, or {@code propNameTableClassName} is not a valid Java type name, or is
     *         {@code "String"}, or equals the simple name of {@code entityClass} or of any of its enclosing classes. The generated interface is a member
     *         type of the entity, so {@code "String"} would shadow {@code java.lang.String} throughout the entity - both in the generated constants, which
     *         are declared {@code String}, and in hand-written members this method cannot rewrite - and a member type may not repeat the simple name of a
     *         class enclosing it. A repeated enclosing name never compiles, and {@code "String"} is a type error in every generated constant; the refusal
     *         is deliberately conservative, covering even a property-less entity, whose generated interface is empty and would in fact compile. Either
     *         name is refused before any file is touched. No other implicitly imported name is rejected - whether {@code Integer} or {@code Object}, say,
     *         clashes depends on the entity's own body, which is read only to be rewritten, not to be type-checked. Also if generated property fields have
     *         invalid or duplicate Java identifiers after keyword escaping.
     * @throws UncheckedIOException if reading the existing source, writing the temporary replacement, or replacing the source file throws an {@link
     *         IOException}.
     * @throws IllegalStateException if write-back of an existing entity source requires an unavailable JDK compiler, parsing or source-location checks
     *         fail, a generated marker pair is incomplete, duplicated, misplaced, or encloses unrelated code, an existing interface is not generated code,
     *         or the source changes before replacement.
     */
    @Beta
    public static String generatePropNameTableClass(final Class<?> entityClass, final String propNameTableClassName, final String srcDir)
            throws IllegalArgumentException, UncheckedIOException, IllegalStateException {
        N.checkArgNotNull(entityClass, cs.entityClass);
        checkJavaTypeIdentifier(propNameTableClassName, cs.propNameTableClassName);
        N.checkArgument(!"String".equals(propNameTableClassName),
                "propNameTableClassName must not be String: the generated interface is a member type of the entity and would shadow java.lang.String");

        for (Class<?> enclosing = entityClass; enclosing != null; enclosing = enclosing.getEnclosingClass()) {
            N.checkArgument(!propNameTableClassName.equals(enclosing.getSimpleName()),
                    "propNameTableClassName must differ from the simple name of the entity and of each of its enclosing classes: %s", propNameTableClassName);
        }

        final StringBuilder sb = new StringBuilder();

        final String interfaceName = "public interface " + propNameTableClassName;
        final String beginMarker = GENERATED_BEGIN_MARKER_PREFIX + propNameTableClassName + GENERATED_MARKER_SUFFIX;
        final String endMarker = GENERATED_END_MARKER_PREFIX + propNameTableClassName + GENERATED_MARKER_SUFFIX;

        sb.append(LINE_SEPARATOR)
                .append(INDENTATION)
                .append(beginMarker)
                .append(LINE_SEPARATOR)
                .append("    /**")
                .append(LINE_SEPARATOR)
                .append("     ")
                .append(GENERATED_DOC_LINE)
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

        final Set<String> generatedFieldNames = N.newHashSet();

        for (final String propName : Beans.getPropNameList(entityClass)) {
            checkGeneratedFieldName(propName, "bean property name");
            final String generatedFieldName = toGeneratedFieldName(propName);
            addGeneratedFieldName(generatedFieldNames, generatedFieldName, "bean property name");

            sb.append("        /** Property(field) name {@code \"")
                    .append(propName)
                    .append("\"} */")
                    .append(LINE_SEPARATOR)
                    .append("        String ")
                    .append(generatedFieldName)
                    .append(" = \"")
                    .append(escapeJavaStringLiteral(propName))
                    .append("\";")
                    .append(LINE_SEPARATOR)
                    .append(LINE_SEPARATOR);
        }

        sb.append("    }").append(LINE_SEPARATOR).append(INDENTATION).append(endMarker).append(LINE_SEPARATOR);

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

            if (!file.exists()) {
                return ret;
            }

            try {
                final String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                final String updated = replacePropertyTable(source, entityClass.getSimpleName(), propNameTableClassName, ret, file);
                if (!Files.readString(file.toPath(), StandardCharsets.UTF_8).equals(source)) {
                    throw new IllegalStateException("Source changed while generating: " + file);
                }
                writeSourceAtomically(updated, file);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return ret;

    }

    /**
     * @throws IllegalStateException if source parsing or location fails, a generated marker pair is incomplete, duplicated, misplaced, or encloses other
     *         members, or the existing interface is not a generated property table.
     * @throws IOException if parsing the source or closing the compiler file manager throws an I/O exception.
     */
    private static String replacePropertyTable(final String source, final String entityName, final String tableName, final String generated, final File file)
            throws IllegalStateException, IOException {
        final SourceLayout layout = parseSourceLayout(source, entityName, tableName, file);
        final String begin = GENERATED_BEGIN_MARKER_PREFIX + tableName + GENERATED_MARKER_SUFFIX;
        final String end = GENERATED_END_MARKER_PREFIX + tableName + GENERATED_MARKER_SUFFIX;
        int markerStart = -1;
        int markerEnd = -1;
        for (final int[] comment : lineComments(source)) {
            if (comment[0] <= layout.start || comment[0] >= layout.end - 1 || insideMember(comment[0], layout.members)) {
                continue;
            }
            final String text = source.substring(comment[0], comment[1]).strip();
            if (text.equals(begin)) {
                if (markerStart >= 0) {
                    throw new IllegalStateException("Duplicate generated marker in " + file);
                }
                markerStart = comment[0];
            } else if (text.equals(end)) {
                if (markerEnd >= 0) {
                    throw new IllegalStateException("Duplicate generated marker in " + file);
                }
                markerEnd = comment[1];
            }
        }

        int from = layout.end - 1;
        int to = from;
        if (markerStart >= 0 || markerEnd >= 0) {
            if (markerStart < 0 || markerEnd < markerStart || layout.table == null || layout.table[0] < markerStart || layout.table[1] > markerEnd) {
                throw new IllegalStateException("Generated marker pair does not enclose the requested interface in " + file);
            }
            for (final int[] member : layout.members) {
                if (member != layout.table && member[0] < markerEnd && member[1] > markerStart) {
                    throw new IllegalStateException("Generated markers include another member in " + file);
                }
            }
            from = lineStart(source, markerStart);
            if (!source.substring(from, markerStart).isBlank()) {
                throw new IllegalStateException("Generated marker shares a line with another member in " + file);
            }
            to = lineEnd(source, markerEnd);
            // The generated fragment supplies its own first newline. Replace the existing one as well,
            // whether the original owner was a single-line declaration or a conventional multi-line class.
            if (from > 0 && source.charAt(from - 1) == '\n')
                from--;
            if (from > 0 && source.charAt(from - 1) == '\r')
                from--;
        } else if (layout.table != null) {
            // Validate only the direct member selected by the Java parser, never a similarly named nested declaration.
            final String declaration = source.substring(layout.table[0], layout.table[1]);
            final List<String> lines = declaration.lines().toList();
            final String expected = "public interface " + tableName + " {";
            if (lines.size() < 2 || !(lines.get(0).strip().equals(expected) || lines.get(0).strip().equals(expected + NOSONAR_COMMENTS))
                    || !isGeneratedBlockBody(lines, 0, lines.size() - 1)) {
                throw new IllegalStateException("Interface is not a generated property-name table in " + file);
            }
            from = lineStart(source, layout.table[0]);
            if (!source.substring(from, layout.table[0]).isBlank()) {
                throw new IllegalStateException("Generated interface shares a line with another member in " + file);
            }
            to = lineEnd(source, layout.table[1]);
            final int docStart = source.lastIndexOf("/**", from);
            final int docEnd = docStart < 0 ? -1 : source.indexOf("*/", docStart);
            if (docEnd >= 0 && docEnd < from && source.substring(docEnd + 2, from).isBlank() && source.substring(docStart, docEnd).contains(GENERATED_DOC_LINE)
                    && !insideMember(docStart, layout.members)) {
                from = lineStart(source, docStart);
                if (!source.substring(from, docStart).isBlank()) {
                    throw new IllegalStateException("Generated documentation shares a line with another member in " + file);
                }
            }
        }
        final String newline = source.contains("\r\n") ? "\r\n" : source.indexOf('\n') >= 0 ? "\n" : source.indexOf('\r') >= 0 ? "\r" : LINE_SEPARATOR;
        final String replacement = generated.replace(LINE_SEPARATOR, newline);
        final String updated = source.substring(0, from) + replacement + source.substring(to);
        parseSourceLayout(updated, entityName, tableName, file); // Parse before staging; no attribution, processors or class loading.
        return updated;
    }

    private record SourceLayout(int start, int end, List<int[]> members, int[] table) {
    }

    /**
     * @throws IllegalStateException if no JDK compiler is available, raw Unicode escapes occur, source parsing fails, the entity declaration is missing or
     *         ambiguous, source positions cannot be resolved, or a conflicting table declaration exists.
     * @throws IOException if parsing the source or closing the compiler file manager throws an I/O exception.
     */
    private static SourceLayout parseSourceLayout(final String source, final String entityName, final String tableName, final File file)
            throws IllegalStateException, IOException {
        final var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("A JDK compiler is required for source write-back");
        }
        // Raw Unicode escapes are translated before tokenization and can disguise comments or braces.
        // Refuse that unsupported spelling rather than apply positions from a different lexical input.
        if (java.util.regex.Pattern.compile("\\\\u+[0-9a-fA-F]{4}").matcher(source).find()) {
            throw new IllegalStateException("Raw Unicode escapes are unsupported for source write-back: " + file);
        }
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final JavaFileObject input = new SimpleJavaFileObject(URI.create("string:///" + file.getName()), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                return source.startsWith("\uFEFF") ? " " + source.substring(1) : source;
            }
        };
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            final JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, List.of(input));
            final CompilationUnitTree unit = task.parse().iterator().next();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalStateException("Cannot parse entity source: " + file);
            }
            ClassTree owner = null;
            for (final Tree declaration : unit.getTypeDecls()) {
                if (declaration instanceof ClassTree type && type.getSimpleName().contentEquals(entityName)) {
                    if (owner != null) {
                        throw new IllegalStateException("Ambiguous entity declaration: " + file);
                    }
                    owner = type;
                }
            }
            if (owner == null) {
                throw new IllegalStateException("Entity declaration not found: " + entityName + " in " + file);
            }
            final SourcePositions positions = Trees.instance(task).getSourcePositions();
            final int start = (int) positions.getStartPosition(unit, owner);
            final int end = (int) positions.getEndPosition(unit, owner);
            if (start < 0 || end <= start || source.charAt(end - 1) != '}') {
                throw new IllegalStateException("Cannot locate entity body: " + file);
            }
            final List<int[]> members = new ArrayList<>();
            int[] table = null;
            for (final Tree member : owner.getMembers()) {
                final int[] range = { (int) positions.getStartPosition(unit, member), (int) positions.getEndPosition(unit, member) };
                if (range[0] < 0 || range[1] < range[0]) {
                    throw new IllegalStateException("Cannot locate entity member: " + file);
                }
                members.add(range);
                if (member instanceof ClassTree type && type.getSimpleName().contentEquals(tableName)) {
                    if (table != null || type.getKind() != Tree.Kind.INTERFACE) {
                        throw new IllegalStateException("Conflicting property table declaration: " + file);
                    }
                    table = range;
                }
            }
            return new SourceLayout(start, end, members, table);
        }
    }

    private static boolean insideMember(final int offset, final List<int[]> members) {
        return members.stream().anyMatch(range -> offset >= range[0] && offset < range[1]);
    }

    private static int lineStart(final String source, final int offset) {
        int start = offset;
        while (start > 0 && source.charAt(start - 1) != '\n' && source.charAt(start - 1) != '\r') {
            start--;
        }
        return start;
    }

    /**
     * @throws IllegalStateException if non-whitespace content occurs between {@code offset} and the end of that source line.
     */
    private static int lineEnd(final String source, final int offset) throws IllegalStateException {
        int end = offset;
        while (end < source.length() && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
            if (!Character.isWhitespace(source.charAt(end))) {
                throw new IllegalStateException("Generated block shares a line with unrelated content");
            }
            end++;
        }
        if (end < source.length() && source.charAt(end) == '\r') {
            end++;
        }
        if (end < source.length() && source.charAt(end) == '\n') {
            end++;
        }
        return end;
    }

    private static List<int[]> lineComments(final String source) {
        final List<int[]> comments = new ArrayList<>();
        for (int i = 0; i < source.length();) {
            if (source.startsWith("//", i)) {
                final int start = i;
                while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                    i++;
                }
                comments.add(new int[] { start, i });
            } else if (source.startsWith("/*", i)) {
                final int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                final char quote = source.charAt(i);
                final boolean textBlock = source.startsWith("\"\"\"", i);
                i += textBlock ? 3 : 1;
                while (i < source.length()) {
                    if (source.charAt(i) == '\\') {
                        i = Math.min(source.length(), i + 2);
                    } else if (textBlock ? source.startsWith("\"\"\"", i) : source.charAt(i) == quote) {
                        i += textBlock ? 3 : 1;
                        break;
                    } else {
                        i++;
                    }
                }
            } else {
                i++;
            }
        }
        return comments;
    }

    /**
     * Returns whether every line strictly between {@code start} and {@code end} has a shape this
     * generator emits: blank, a single-line Javadoc comment, or a {@code String} constant declaration.
     *
     * <p>Used to tell a stale generated block (safe to replace) from a hand-written interface that
     * merely shares the generated name (never safe to delete by brace-scanning).</p>
     *
     * @param lines the source file's lines
     * @param start index of the interface declaration line, exclusive
     * @param end index of the closing brace line, exclusive
     * @return {@code true} if the body looks generated
     */
    private static boolean isGeneratedBlockBody(final List<String> lines, final int start, final int end) {
        for (int i = start + 1; i < end; i++) {
            final String trimmed = lines.get(i).trim();

            if (trimmed.isEmpty() || (trimmed.startsWith("/**") && trimmed.endsWith("*/")) || (trimmed.startsWith("String ") && trimmed.endsWith(";"))) {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Writes {@code source} to {@code file} through a sibling temporary file that is then moved into
     * place, so a failure part-way through never leaves a caller's source file truncated.
     * Each invocation reserves and cleans up its own temporary sibling. Concurrent read-modify-write
     * operations are not serialized or merged.
     *
     * @param source the complete new contents, including the original surrounding formatting
     * @param file the source file to replace
     * @throws UncheckedIOException if creating or writing the temporary source file, or moving it onto {@code file}, throws an {@link IOException}.
     */
    private static void writeSourceAtomically(final String source, final File file) throws UncheckedIOException {
        File tmp = null;

        try {
            // Reserve an invocation-owned sibling; a predictable name could overwrite unrelated data.
            tmp = Files.createTempFile(file.toPath().toAbsolutePath().getParent(), ".codegen-", ".tmp").toFile();
            Files.writeString(tmp.toPath(), source, StandardCharsets.UTF_8);

            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                // Some filesystems cannot move atomically; a plain replace is still better than
                // writing the destination in place.
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            if (tmp != null) {
                IOUtil.deleteIfExists(tmp);
            }
        }
    }

    /**
     * Generates a standalone property-name table for multiple entity classes.
     *
     * <p>The generated top-level interface name defaults to {@value #X} for compatibility.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class, Order.class));
     * System.out.println(source);
     * }</pre>
     *
     * @param entityClasses entity classes that contribute bean property names; must not be {@code null} or empty
     * @return generated Java source for the standalone property-name table
     * @throws IllegalArgumentException if {@code entityClasses} is null, empty, contains null, or has no usable class after filtering interfaces and
     *         Lombok builder classes, or generated property fields have invalid or duplicate Java identifiers.
     * @see #generatePropNameTableClasses(Collection, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses) throws IllegalArgumentException {
        return generatePropNameTableClasses(entityClasses, X);
    }

    /**
     * Generates a standalone property-name table for multiple entity classes with a custom name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class, Order.class), "Props");
     * System.out.println(source);
     * }</pre>
     *
     * @param entityClasses entity classes that contribute bean property names; must not be {@code null} or empty
     * @param propNameTableClassName top-level interface name to generate; must be a valid Java type name
     * @return generated Java source for the standalone property-name table
     * @throws IllegalArgumentException if {@code entityClasses} is null, empty, contains null, or has no usable class after filtering interfaces and
     *         Lombok builder classes, or generated property fields have invalid or duplicate Java identifiers, or {@code propNameTableClassName} is not a
     *         valid Java type name.
     * @see #generatePropNameTableClasses(Collection, String, String, String)
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName)
            throws IllegalArgumentException {
        return generatePropNameTableClasses(entityClasses, propNameTableClassName, null, null);
    }

    /**
     * Generates a standalone property-name table for multiple entities with package/file options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String source = CodeGenerationUtil.generatePropNameTableClasses(
     *         Arrays.asList(User.class, Order.class), "Props", "com.example.props", "src/main/java");
     * }</pre>
     *
     * @param entityClasses entity classes that contribute bean property names; must not be {@code null} or empty
     * @param propNameTableClassName top-level interface name to generate; must be a valid Java type name
     * @param propNameTableClassPackageName package for generated source; if {@code null} or empty,
     *        uses the first entity's package; otherwise it must be a valid dot-separated Java package name
     * @param srcDir source root directory; if {@code null} or empty, source is not written
     * @return generated Java source for the standalone property-name table
     * @throws IllegalArgumentException if {@code entityClasses} is null, empty, contains null, or has no usable class after filtering interfaces and
     *         Lombok builder classes, or generated property fields have invalid or duplicate Java identifiers, or {@code propNameTableClassName} is not a
     *         valid Java type name, or the nonempty package name contains an invalid Java identifier.
     * @throws UncheckedIOException if creating or writing the generated source file throws an {@link IOException} when {@code srcDir} is nonempty.
     */
    public static String generatePropNameTableClasses(final Collection<Class<?>> entityClasses, final String propNameTableClassName,
            final String propNameTableClassPackageName, final String srcDir) throws IllegalArgumentException, UncheckedIOException {

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
     *         .entityClasses(Arrays.asList(User.class, Order.class))
     *         .className("Props")
     *         .build();
     * String source = CodeGenerationUtil.generatePropNameTableClasses(config);
     * }</pre>
     *
     * @param codeConfig full generation configuration; must not be {@code null} and must supply a
     *        non-empty {@code entityClasses} collection and a non-empty {@code className}
     * @return generated Java source for the property-name table class
     * @throws IllegalArgumentException if {@code codeConfig} is {@code null}, its {@code entityClasses} is {@code null} or empty, its
     *         class/package/converted property names are not valid Java names, or generated interface/field names collide, {@code extendedInterfaces}
     *         contains a null, non-interface, duplicate, or self-referential type, {@code generateClassPropNameList} is enabled while the entities have
     *         duplicate simple class names, or no usable entity class remains after filtering out interfaces and Lombok builder classes; a generated or
     *         inherited type named {@code java} prevents the qualified {@code String} reference required by a generated or inherited type named
     *         {@code String}, or the qualified {@code List} reference required by a generated or inherited type named {@code List} when
     *         {@code generateClassPropNameList} is enabled; or an inherited instance method named {@code of} conflicts with the unqualified list factory
     *         required by a generated or inherited type or field named {@code List}. Also
     *         if an entity class is null, or an enabled property function has a null/empty key, a null callback, or produces an invalid field identifier.
     * @throws RuntimeException if a configured property-name converter or property function throws an unchecked exception.
     * @throws UncheckedIOException if creating or writing the generated source file throws an {@link IOException} when {@code srcDir} is configured.
     */
    public static String generatePropNameTableClasses(final PropNameTableCodeConfig codeConfig)
            throws IllegalArgumentException, RuntimeException, UncheckedIOException {
        N.checkArgNotNull(codeConfig, cs.codeConfig);

        final Collection<Class<?>> entityClasses = N.checkArgNotEmpty(codeConfig.getEntityClasses(), "codeConfig.getEntityClasses()");

        for (final Class<?> cls : entityClasses) {
            N.checkArgNotNull(cls, "entity class");
        }

        final List<Class<?>> entityClassesToUse = Stream.of(entityClasses).filter(cls -> {
            if (cls.isInterface()) {
                return false;
            }

            final String simpleClassName = ClassUtil.getSimpleClassName(cls);

            // NOSONAR
            return !cls.isMemberClass() || !simpleClassName.endsWith(BUILDER) || cls.getDeclaringClass() == null // NOSONAR
                    || !simpleClassName.equals(ClassUtil.getSimpleClassName(cls.getDeclaringClass()) + BUILDER);
        }).toList();

        N.checkArgNotEmpty(entityClassesToUse, "entity classes after filtering interfaces and Lombok builder classes");

        final Class<?> entityClass = N.firstElement(entityClassesToUse).orElseThrow();
        final boolean generateClassPropNameList = codeConfig.isGenerateClassPropNameList();
        final String propNameTableClassPackageName = codeConfig.getPackageName();
        // An entity in the unnamed package has a valid empty fallback package name.
        final String packageName = Strings.isEmpty(propNameTableClassPackageName) ? ClassUtil.getPackageName(entityClass) : propNameTableClassPackageName;
        final String propNameTableClassName = checkJavaTypeIdentifier(codeConfig.getClassName(), "codeConfig.getClassName()");
        checkPackageName(packageName);
        final Collection<Class<?>> extendedInterfaces = codeConfig.getExtendedInterfaces();
        final BiFunction<Class<?>, String, String> propNameConverter = N.defaultIfNull(codeConfig.getPropNameConverter(), identityPropNameConverter);

        final List<String> extendedInterfaceNames = new ArrayList<>();
        final Set<String> uniqueExtendedInterfaceNames = N.newHashSet();
        final String generatedCanonicalName = Strings.isEmpty(packageName) ? propNameTableClassName : packageName + "." + propNameTableClassName;

        if (extendedInterfaces != null) {
            for (final Class<?> extendedInterface : extendedInterfaces) {
                N.checkArgNotNull(extendedInterface, "extended interface");
                N.checkArgument(extendedInterface.isInterface(), "extendedInterfaces must contain only interfaces: %s", extendedInterface);

                final String extendedInterfaceName = ClassUtil.getCanonicalClassName(extendedInterface);
                N.checkArgument(!generatedCanonicalName.equals(extendedInterfaceName), "Generated interface cannot extend itself: %s", generatedCanonicalName);
                N.checkArgument(uniqueExtendedInterfaceNames.add(extendedInterfaceName), "Duplicate extended interface: %s", extendedInterfaceName);
                extendedInterfaceNames.add(Strings.isNotEmpty(packageName) && packageName.equals(ClassUtil.getPackageName(extendedInterface))
                        ? extendedInterfaceName.substring(packageName.length() + 1)
                        : extendedInterfaceName);
            }
        }

        final String interfaceName = Stream.of(extendedInterfaceNames)
                .mapFirst(it -> " extends " + it)
                .join(", ", "public interface " + propNameTableClassName, "");
        final Set<String> generatedNestedTypeNames = N.newHashSet();
        generatedNestedTypeNames.add(propNameTableClassName);

        final String snakeCaseClassName = N.defaultIfEmpty(codeConfig.getClassNameForSnakeCase(), SL);
        final String screamingSnakeCaseClassName = N.defaultIfEmpty(codeConfig.getClassNameForScreamingSnakeCase(), SU);
        final String functionClassName = N.defaultIfEmpty(codeConfig.getFunctionClassName(), SF);
        if (codeConfig.isGenerateSnakeCase()) {
            checkJavaTypeIdentifier(snakeCaseClassName, "codeConfig.getClassNameForSnakeCase()");
            N.checkArgument(generatedNestedTypeNames.add(snakeCaseClassName), "Generated interface names must be unique: %s", snakeCaseClassName);
        }
        if (codeConfig.isGenerateScreamingSnakeCase()) {
            checkJavaTypeIdentifier(screamingSnakeCaseClassName, "codeConfig.getClassNameForScreamingSnakeCase()");
            N.checkArgument(generatedNestedTypeNames.add(screamingSnakeCaseClassName), "Generated interface names must be unique: %s",
                    screamingSnakeCaseClassName);
        }
        if (codeConfig.isGenerateFunctionPropName()) {
            checkJavaTypeIdentifier(functionClassName, "codeConfig.getFunctionClassName()");
            N.checkArgument(generatedNestedTypeNames.add(functionClassName), "Generated interface names must be unique: %s", functionClassName);
        }

        // Member types, including inherited ones, shadow imported types throughout the enclosing interface.
        final Set<String> visibleTypeNames = N.newHashSet(generatedNestedTypeNames);
        if (extendedInterfaces != null) {
            final Set<Class<?>> interfacesWithMembers = N.newHashSet(extendedInterfaces);
            for (final Class<?> extendedInterface : extendedInterfaces) {
                interfacesWithMembers.addAll(ClassUtil.getAllInterfaces(extendedInterface));
            }
            for (final Class<?> extendedInterface : interfacesWithMembers) {
                for (final Class<?> memberType : extendedInterface.getClasses()) {
                    visibleTypeNames.add(memberType.getSimpleName());
                }
            }
        }
        final boolean qualifyString = visibleTypeNames.contains("String");
        final boolean qualifyList = generateClassPropNameList && visibleTypeNames.contains("List");
        N.checkArgument(!visibleTypeNames.contains("java") || !(qualifyString || qualifyList),
                "Generated or inherited type java conflicts with the qualified String or List type required by another visible type");
        final String stringType = qualifyString ? "java.lang.String" : "String";
        final String listType = qualifyList ? "java.util.List" : "List";
        boolean outerListShadowed = qualifyList;
        boolean inheritedInstanceOf = false;
        if (extendedInterfaces != null) {
            for (final Class<?> extendedInterface : extendedInterfaces) {
                for (final java.lang.reflect.Field field : extendedInterface.getFields()) {
                    outerListShadowed |= field.getName().equals("List");
                }
                for (final Method method : extendedInterface.getMethods()) {
                    inheritedInstanceOf |= method.getName().equals("of") && !java.lang.reflect.Modifier.isStatic(method.getModifiers());
                }
            }
        }
        boolean importListFactory = false;

        final StringBuilder sb = new StringBuilder();

        final String allClassName = Stream.of(entityClassesToUse).map(ClassUtil::getSimpleClassName).join(", ", "[", "]");

        if (generateClassPropNameList && Stream.of(entityClassesToUse).map(ClassUtil::getSimpleClassName).containsDuplicates()) {
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

                    checkGeneratedFieldName(newPropName, cs.propNameConverter);

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
            final Set<String> generatedFieldNames = N.newHashSet();

            for (final String propName : propNames) {
                final String clsNameList = Stream.of(propNameMap.get(propName)).sorted().join(", ", "{@code [", "]}");
                final String generatedFieldName = toGeneratedFieldName(propName);
                addGeneratedFieldName(generatedFieldNames, generatedFieldName, "propNameConverter");

                sb.append(LINE_SEPARATOR)
                        .append("    /** Property(field) name {@code \"")
                        .append(propName)
                        .append("\"} for classes: ")
                        .append(clsNameList)
                        .append(" */")
                        .append(LINE_SEPARATOR)
                        .append("    ")
                        .append(stringType)
                        .append(" ")
                        .append(generatedFieldName)
                        .append(" = \"")
                        .append(escapeJavaStringLiteral(propName))
                        .append("\";")
                        .append(LINE_SEPARATOR);
            }

            if (generateClassPropNameList) {
                // Expression names also resolve fields: a field named List hides List.of, while a static import
                // keeps fields named java or of usable without changing their public generated names.
                outerListShadowed |= generatedFieldNames.contains("List");
                final String listFactory = outerListShadowed ? "of(" : "List.of(";
                importListFactory |= outerListShadowed;
                for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                    final String fieldNameForPropNameList = addUniqueGeneratedFieldName(generatedFieldNames,
                            Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList");

                    sb.append(LINE_SEPARATOR)
                            .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                            .append(classPropNameListEntry.getKey())
                            .append("\"}.")
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append("    ")
                            .append(listType)
                            .append("<")
                            .append(stringType)
                            .append("> ")
                            .append(fieldNameForPropNameList)
                            .append(" = ")
                            .append(listFactory)
                            .append(Stream.of(classPropNameListEntry.getValue()).sorted().map(CodeGenerationUtil::toGeneratedFieldName).join(", "))
                            .append(");")
                            .append(LINE_SEPARATOR);
                }
            }
        }

        {
            if (codeConfig.isGenerateSnakeCase()) {
                final ListMultimap<Tuple2<String, String>, String> propNameMap = N.newListMultimap();
                final ListMultimap<String, String> classPropNameListMap = N.newListMultimap();
                final BiFunction<Class<?>, String, String> propNameConverterForSnakeCase = CommonUtil
                        .defaultIfNull(codeConfig.getPropNameConverterForSnakeCase(), (cls, propName) -> Strings.toSnakeCase(propName));

                for (final Class<?> cls : entityClassesToUse) {
                    final String simpleClassName = ClassUtil.getSimpleClassName(cls);
                    String newPropName = null;
                    String propNameInSnakeCase = null;

                    for (final String propName : Beans.getPropNameList(cls)) {
                        newPropName = propNameConverter.apply(cls, propName);

                        if (Strings.isEmpty(newPropName)) {
                            continue;
                        }

                        checkGeneratedFieldName(newPropName, cs.propNameConverter);

                        propNameInSnakeCase = propNameConverterForSnakeCase.apply(cls, newPropName);

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
                        .append(snakeCaseClassName)
                        .append(" {")
                        .append(Character.isLowerCase(snakeCaseClassName.charAt(0)) ? NOSONAR_COMMENTS : "")
                        .append(LINE_SEPARATOR); //

                final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(propNameMap.keySet());
                N.sortBy(propNameTPs, it -> it._1);
                final Set<String> generatedFieldNames = N.newHashSet();

                for (final Tuple2<String, String> propNameTP : propNameTPs) {
                    final String clsNameList = Stream.of(propNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");
                    final String generatedFieldName = toGeneratedFieldName(propNameTP._1);
                    addGeneratedFieldName(generatedFieldNames, generatedFieldName, "snake-case property names");

                    sb.append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    /** Property(field) name in lower case concatenated with underscore: <code>&quot;")
                            .append(escapeJavadocText(propNameTP._2))
                            .append("&quot;</code> for classes: ")
                            .append(clsNameList)
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    ")
                            .append(stringType)
                            .append(" ")
                            .append(generatedFieldName)
                            .append(" = \"")
                            .append(escapeJavaStringLiteral(propNameTP._2))
                            .append("\";")
                            .append(LINE_SEPARATOR);
                }

                if (generateClassPropNameList) {
                    final boolean listShadowed = outerListShadowed || generatedFieldNames.contains("List");
                    final String listFactory = listShadowed ? "of(" : "List.of(";
                    importListFactory |= listShadowed;
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                        final String fieldNameForPropNameList = addUniqueGeneratedFieldName(generatedFieldNames,
                                Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList");

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                                .append(classPropNameListEntry.getKey())
                                .append("\"}.")
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    ")
                                .append(listType)
                                .append("<")
                                .append(stringType)
                                .append("> ")
                                .append(fieldNameForPropNameList)
                                .append(" = ")
                                .append(listFactory)
                                .append(Stream.of(classPropNameListEntry.getValue()).sorted().map(CodeGenerationUtil::toGeneratedFieldName).join(", "))
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
                final BiFunction<Class<?>, String, String> propNameConverterForScreamingSnakeCase = CommonUtil
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

                        checkGeneratedFieldName(newPropName, cs.propNameConverter);

                        propNameInScreamingSnakeCase = propNameConverterForScreamingSnakeCase.apply(cls, newPropName);

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
                        .append(screamingSnakeCaseClassName)
                        .append(" {")
                        .append(Character.isLowerCase(screamingSnakeCaseClassName.charAt(0)) ? NOSONAR_COMMENTS : "")
                        .append(LINE_SEPARATOR); //

                final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(propNameMap.keySet());
                N.sortBy(propNameTPs, it -> it._1);
                final Set<String> generatedFieldNames = N.newHashSet();

                for (final Tuple2<String, String> propNameTP : propNameTPs) {
                    final String clsNameList = Stream.of(propNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");
                    final String generatedFieldName = toGeneratedFieldName(propNameTP._1);
                    addGeneratedFieldName(generatedFieldNames, generatedFieldName, "screaming-snake-case property names");

                    sb.append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    /** Property(field) name in upper case concatenated with underscore: <code>&quot;")
                            .append(escapeJavadocText(propNameTP._2))
                            .append("&quot;</code> for classes: ")
                            .append(clsNameList)
                            .append(" */")
                            .append(LINE_SEPARATOR)
                            .append(INDENTATION)
                            .append("    ")
                            .append(stringType)
                            .append(" ")
                            .append(generatedFieldName)
                            .append(" = \"")
                            .append(escapeJavaStringLiteral(propNameTP._2))
                            .append("\";")
                            .append(LINE_SEPARATOR);
                }

                if (generateClassPropNameList) {
                    final boolean listShadowed = outerListShadowed || generatedFieldNames.contains("List");
                    final String listFactory = listShadowed ? "of(" : "List.of(";
                    importListFactory |= listShadowed;
                    for (final Map.Entry<String, List<String>> classPropNameListEntry : classPropNameListMap) {
                        final String fieldNameForPropNameList = addUniqueGeneratedFieldName(generatedFieldNames,
                                Strings.toCamelCase(classPropNameListEntry.getKey()) + "PropNameList");

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Unmodifiable property(field) name list for class: {@code \"")
                                .append(classPropNameListEntry.getKey())
                                .append("\"}.")
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    ")
                                .append(listType)
                                .append("<")
                                .append(stringType)
                                .append("> ")
                                .append(fieldNameForPropNameList)
                                .append(" = ")
                                .append(listFactory)
                                .append(Stream.of(classPropNameListEntry.getValue()).sorted().map(CodeGenerationUtil::toGeneratedFieldName).join(", "))
                                .append(");")
                                .append(LINE_SEPARATOR);
                    }
                }

                sb.append(LINE_SEPARATOR).append(INDENTATION).append("}").append(LINE_SEPARATOR);
            }
        }

        {
            if (codeConfig.isGenerateFunctionPropName()) {
                final Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFuncMap = N.nullToEmpty(codeConfig.getPropFunctions());

                final List<ListMultimap<Tuple2<String, String>, String>> funcPropNameMapList = new ArrayList<>();

                for (final Map.Entry<String, TriFunction<Class<?>, Class<?>, String, String>> propFuncEntry : propFuncMap.entrySet()) {
                    final String funcName = N.checkArgNotEmpty(propFuncEntry.getKey(), "propFunctions key");
                    final TriFunction<Class<?>, Class<?>, String, String> propFunc = N.checkArgNotNull(propFuncEntry.getValue(), "propFunctions value");
                    checkGeneratedFieldName(funcName + "_property", "propFunctions key");
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

                            checkGeneratedFieldName(newPropName, cs.propNameConverter);

                            final Method propGetMethod = Beans.getPropGetter(cls, propName);

                            if (propGetMethod == null) {
                                continue;
                            }

                            funcPropName = propFunc.apply(cls, propGetMethod.getReturnType(), newPropName);

                            if (Strings.isEmpty(funcPropName)) {
                                continue;
                            }

                            final String generatedFieldName = funcName + "_" + newPropName;
                            checkGeneratedFieldName(generatedFieldName, "propFunctions key");
                            funcPropNameMap.put(Tuple.of(toGeneratedFieldName(generatedFieldName), funcPropName), simpleClassName);
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

                final Set<String> generatedFieldNames = N.newHashSet();

                for (final ListMultimap<Tuple2<String, String>, String> funcPropNameMap : funcPropNameMapList) {
                    final List<Tuple2<String, String>> propNameTPs = new ArrayList<>(funcPropNameMap.keySet());
                    N.sortBy(propNameTPs, it -> it._1);

                    for (final Tuple2<String, String> propNameTP : propNameTPs) {
                        final String clsNameList = Stream.of(funcPropNameMap.get(propNameTP)).sorted().join(", ", "{@code [", "]}");
                        addGeneratedFieldName(generatedFieldNames, propNameTP._1, "propFunctions");

                        sb.append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    /** Function property(field) name <code>&quot;")
                                .append(escapeJavadocText(propNameTP._2))
                                .append("&quot;</code> for classes: ")
                                .append(clsNameList)
                                .append(" */")
                                .append(LINE_SEPARATOR)
                                .append(INDENTATION)
                                .append("    ")
                                .append(stringType)
                                .append(" ")
                                .append(propNameTP._1)
                                .append(" = \"")
                                .append(escapeJavaStringLiteral(propNameTP._2))
                                .append("\";")
                                .append(LINE_SEPARATOR);
                    }
                }

                sb.append(LINE_SEPARATOR).append(INDENTATION).append("}").append(LINE_SEPARATOR);
            }
        }

        sb.append(LINE_SEPARATOR).append("}").append(LINE_SEPARATOR);

        N.checkArgument(!importListFactory || !inheritedInstanceOf,
                "An inherited instance method named of conflicts with the list factory required by a generated List name");
        final StringBuilder header = new StringBuilder();
        if (Strings.isNotEmpty(packageName)) {
            header.append("package ").append(packageName).append(";").append(LINE_SEPARATOR);
        }
        if (generateClassPropNameList && !qualifyList) {
            header.append(LINE_SEPARATOR).append("import java.util.List;").append(LINE_SEPARATOR);
        }
        if (importListFactory) {
            header.append(LINE_SEPARATOR).append("import static java.util.List.of;").append(LINE_SEPARATOR);
        }
        final String ret = header.append(sb).toString();

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<Class<?>> classes = Arrays.asList(User.class, Order.class);
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
     *         .propFunctions(N.asMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
     *         .build();
     * }</pre>
     *
     */
    @Builder
    @Data
    @AllArgsConstructor
    @Accessors(chain = true)
    public static final class PropNameTableCodeConfig {
        /** Entity classes to scan for bean properties. Required. */
        private Collection<Class<?>> entityClasses;

        /** Top-level interface name for generated property constants. Required and must be a valid Java type name. */
        private String className;

        /** Package for generated source; if non-empty, it must be a valid dot-separated Java package name. */
        private String packageName;

        /** Source root to write files into; if {@code null} or empty, only the generated source text is returned. */
        private String srcDir;

        /**
         * Optional, distinct interface types that the generated top-level interface should extend.
         * Classes, duplicate interfaces, {@code null} elements, and the generated interface itself are rejected.
         * Interfaces in the generated package are emitted relative to that package; other interfaces retain
         * their fully qualified canonical names.
         * Inherited member types named {@code String} or {@code List} cause references to the standard types
         * to be fully qualified. A visible member type named {@code java} is rejected when it prevents that qualification.
         */
        private Collection<Class<?>> extendedInterfaces;

        /**
         * Converts property names before constants are emitted.
         *
         * <p>The function receives {@code (entityClass, propName)}. Return {@code null} or empty
         * to skip a property. A non-empty result must be a Java identifier (Java keywords are
         * accepted and emitted with an underscore prefix). If {@code null}, identity mapping is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverter;

        /**
         * Whether to generate a {@code List<String>} constant per entity class. Default is {@code false}.
         *
         * <p><b>Note:</b> The property names in each generated {@code xxxPropNameList} constant are emitted in
         * alphabetically sorted order, not bean-declaration order.
         */
        private boolean generateClassPropNameList;

        /** Whether to generate a snake_case nested interface. Default is {@code false}. */
        private boolean generateSnakeCase;

        /** Nested interface name for snake_case constants. Must be a valid Java type name; defaults to {@link CodeGenerationUtil#SL}. */
        private String classNameForSnakeCase;

        /**
         * Converts property names to snake_case constant values.
         *
         * <p>If {@code null}, {@link Strings#toSnakeCase(String)} is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverterForSnakeCase;

        /** Whether to generate a SCREAMING_SNAKE_CASE nested interface. Default is {@code false}. */
        private boolean generateScreamingSnakeCase;

        /** Nested interface name for SCREAMING_SNAKE_CASE constants. Must be a valid Java type name; defaults to {@link CodeGenerationUtil#SU}. */
        private String classNameForScreamingSnakeCase;

        /**
         * Converts property names to SCREAMING_SNAKE_CASE constant values.
         *
         * <p>If {@code null}, {@link Strings#toScreamingSnakeCase(String)} is used.
         */
        private BiFunction<Class<?>, String, String> propNameConverterForScreamingSnakeCase;

        /** Whether to generate a nested interface for function-based names (for example, {@code min(age)}). */
        private boolean generateFunctionPropName;

        /** Nested interface name for function-based constants. Must be a valid Java type name; defaults to {@link CodeGenerationUtil#SF}. */
        private String functionClassName;

        /**
         * Function definitions used when {@link #generateFunctionPropName} is enabled.
         *
         * <p>Map key is a non-empty constant prefix (for example {@code min}); function input is
         * {@code (entityClass, propertyType, convertedPropertyName)}; returning {@code null} or
         * empty skips the property. Each key, when combined with an underscore and a converted
         * property name, must form a valid Java identifier.
         */
        private Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions;

        /**
         * Default constructor for framework/tooling compatibility.
         *
         * <p>Creates a config with all fields at their defaults (object fields are {@code null};
         * boolean flags are {@code false}). Prefer the Lombok-generated {@code builder()} method;
         * this no-arg constructor exists mainly to be populated through the
         * chainable setters (note {@code @Accessors(chain = true)}).</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * PropNameTableCodeConfig config = new PropNameTableCodeConfig();
         * config.getEntityClasses();                             // returns null (object fields default to null)
         * config.isGenerateSnakeCase();                          // returns false (boolean flags default to false)
         *
         * config.setClassName("S").setGenerateSnakeCase(true);   // returns this (chainable setters)
         * config.getClassName();                                 // returns "S"
         * }</pre>
         *
         */
        public PropNameTableCodeConfig() {
        }
    }
}
