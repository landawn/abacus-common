
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

public final class ApiDocGenerator {

    private ApiDocGenerator() {
    }

    private static final class LibraryInfo {
        String name = "unknown";
        String version = "unknown";
        String javaTarget = "unknown";
        String gitSha = "unknown";
        String generatedAt = LocalDate.now().toString();
    }

    private static final class DeprecatedInfo {
        String message;
        String since;
        boolean forRemoval;
    }

    private static final class TypeParamInfo {
        String name;
        List<String> bounds = new ArrayList<>();
    }

    private static final class ParamInfo {
        String name;
        String type;
        String javadoc;
        String nullability = "unspecified";
    }

    private static final class ThrowInfo {
        String type;
        String condition;
    }

    private static final class ConstructorInfo {
        String signature;
        String javadocSummary;
        String since;
        DeprecatedInfo deprecated;
        boolean recordCanonical;
        List<String> annotations = new ArrayList<>();
        List<ParamInfo> params = new ArrayList<>();
        List<ThrowInfo> throwsList = new ArrayList<>();
        List<String> apiNotes = new ArrayList<>();
        List<String> implementationNotes = new ArrayList<>();
        List<String> examples = new ArrayList<>();
    }

    private static final class MethodInfo {
        String name;
        String kind;
        List<String> modifiers = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        String signature;
        String returnType;
        String returnNullability = "unspecified";
        List<TypeParamInfo> typeParams = new ArrayList<>();
        List<ParamInfo> params = new ArrayList<>();
        List<ThrowInfo> throwsList = new ArrayList<>();
        String since;
        DeprecatedInfo deprecated;
        String javadocSummary;
        String returns;
        List<String> contract = new ArrayList<>();
        String performance;
        boolean inheritsDocumentation;
        List<String> apiNotes = new ArrayList<>();
        List<String> implementationNotes = new ArrayList<>();
        List<String> examples = new ArrayList<>();
        List<String> seeAlso = new ArrayList<>();
        int order;
    }

    private static final class RecordComponentInfo {
        String name;
        String type;
        boolean varArgs;
        String javadoc;
        String nullability = "unspecified";
        List<String> annotations = new ArrayList<>();
    }

    private static final class FieldInfo {
        String name;
        List<String> modifiers = new ArrayList<>();
        String type;
        String value;
        String javadocSummary;
    }

    private static final class TypeInfo {
        String fqn;
        String name;
        String kind;
        List<String> modifiers = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        String since;
        DeprecatedInfo deprecated;
        String javadocSummary;
        String threadSafety = "unspecified";
        String nullability = "unspecified";
        List<String> permittedSubtypes = new ArrayList<>();
        /** Fully-qualified names of direct supertypes (extends + implements), without type arguments. */
        List<String> superTypes = new ArrayList<>();
        List<TypeParamInfo> typeParams = new ArrayList<>();
        List<RecordComponentInfo> recordComponents = new ArrayList<>();
        boolean declaresConstructor;
        boolean suppressDefaultConstructor;
        List<ConstructorInfo> constructors = new ArrayList<>();
        List<MethodInfo> methods = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();
        List<String> apiNotes = new ArrayList<>();
        List<String> implementationNotes = new ArrayList<>();
        int order;
    }

    private static final class PackageInfoData {
        String name;
        String summary;
        List<TypeInfo> types = new ArrayList<>();
    }

    private static final class UnitData {
        CompilationUnitTree unit;
        Path path;
        String source;
        String packageName;
        Map<String, String> explicitImports = new LinkedHashMap<>();
        List<String> wildcardImports = new ArrayList<>();
    }

    private static final class DocInfo {
        String summary;
        String body;
        String since;
        String deprecatedMessage;
        String returns;
        Map<String, String> paramDocs = new LinkedHashMap<>();
        Map<String, String> throwsDocs = new LinkedHashMap<>();
        List<ThrowInfo> documentedThrows = new ArrayList<>();
        List<String> seeAlso = new ArrayList<>();
        List<String> contract = new ArrayList<>();
        String performance;
        boolean inheritsDocumentation;
        List<String> apiNotes = new ArrayList<>();
        List<String> implementationNotes = new ArrayList<>();
        List<String> examples = new ArrayList<>();
    }

    /**
     * Documented marker annotations used across the code base (com.landawn.abacus.annotation)
     * that carry API contract information worth surfacing in the generated docs.
     */
    private static final Set<String> MARKER_ANNOTATIONS = Set.of("Beta", "Internal", "Immutable", "Mutable", "Stateful", "MayReturnNull", "NotNull",
            "NullSafe", "SequentialOnly", "ParallelSupported", "IntermediateOp", "TerminalOp", "TerminalOpTriggered", "LazyEvaluation",
            "UnsupportedOperation");

    private static final Set<String> CONSTRUCTOR_GENERATING_ANNOTATIONS = Set.of("lombok.AllArgsConstructor", "lombok.Builder", "lombok.Data",
            "lombok.NoArgsConstructor", "lombok.RequiredArgsConstructor", "lombok.Value", "lombok.experimental.StandardException",
            "lombok.experimental.SuperBuilder", "lombok.experimental.UtilityClass");

    private static final Pattern THREAD_SAFE = Pattern.compile("\\bthread(?:-|\\s+)safe\\b");
    private static final Pattern NOT_THREAD_SAFE = Pattern.compile(
            "\\b(?:not|never)\\s+(?:generally\\s+|necessarily\\s+)?thread(?:-|\\s+)safe\\b|\\bnon(?:-|\\s+)thread(?:-|\\s+)safe\\b|\\bnot safe for concurrent\\b");
    private static final Pattern CONDITIONAL_THREAD_SAFETY = Pattern.compile(
            "\\bconditionally thread(?:-|\\s+)safe\\b|\\bthread(?:-|\\s+)safe[^.!?]{0,40}\\b(?:if|when)\\b|"
                    + "\\bthread(?:-|\\s+)safety[^.!?]{0,80}\\b(?:depends|characteristics|responsibility|backing|contained)\\b|"
                    + "\\bsame safety requirements as (?:the )?backing\\b|\\bwrapping does not add synchronization\\b|"
                    + "\\binvalidates?[^.!?]{0,60}thread(?:-|\\s+)safety guarantees\\b");
    private static final Pattern THREAD_SAFETY_DISCLAIMER = Pattern.compile(
            "\\bdoes not[^.!?]{0,100}(?:thread(?:-|\\s+)safe|thread(?:-|\\s+)safety)\\b|\\bnot a general thread(?:-|\\s+)safety annotation\\b");

    /** Inline HTML tags that must not inject spaces around surrounding text/punctuation. */
    private static final Set<String> INLINE_HTML_TAGS = Set.of("a", "b", "i", "u", "s", "em", "strong", "code", "span", "tt", "sub", "sup", "small",
            "big", "font", "strike", "cite", "dfn", "var", "samp", "kbd", "abbr", "acronym", "label");

    /** Block-ish HTML tags that separate prose; emit a single space when they open or close. */
    private static final Set<String> BLOCK_HTML_TAGS = Set.of("p", "div", "li", "ul", "ol", "tr", "td", "th", "table", "thead", "tbody", "tfoot", "h1", "h2",
            "h3", "h4", "h5", "h6", "blockquote", "pre", "dl", "dt", "dd", "section", "article", "header", "footer", "nav", "br", "hr");

    private static int methodOrder;

    public static void main(final String[] args) throws Exception {
        final Path sourceRoot = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/java");
        final Path markdownOut = args.length > 1 ? Path.of(args[1]) : Path.of("./docs/ai/API.md");
        final Path jsonOut = args.length > 2 ? Path.of(args[2]) : Path.of("./docs/ai/api-index.json");
        final Path pomPath = args.length > 3 ? Path.of(args[3]) : Path.of("pom.xml");

        final LibraryInfo library = readLibraryInfo(pomPath);
        final List<Path> javaFiles = listJavaFiles(sourceRoot);
        if (javaFiles.isEmpty()) {
            throw new IllegalStateException("No Java source files under " + sourceRoot);
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler");
        }

        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);
        final Iterable<? extends JavaFileObject> files = fileManager.getJavaFileObjectsFromPaths(javaFiles);
        final JavacTask task = (JavacTask) compiler.getTask(new StringWriter(), fileManager, diagnostics, List.of("-proc:none", "-Xlint:none"), null, files);

        // Parse only — everything extracted here is syntactic. Running analyze() would inject
        // synthesized members (record canonical constructors/accessors, enum values/valueOf)
        // into the trees with positions pointing at the type declaration, corrupting signatures.
        final List<CompilationUnitTree> parsedUnits = new ArrayList<>();
        for (final CompilationUnitTree unit : task.parse()) {
            parsedUnits.add(unit);
        }
        fileManager.close();

        final DocTrees docTrees = DocTrees.instance(task);
        final Trees trees = Trees.instance(task);
        final SourcePositions sourcePositions = trees.getSourcePositions();

        final Map<String, PackageInfoData> packageMap = new LinkedHashMap<>();
        final Map<String, Map<String, String>> typesByPackage = new LinkedHashMap<>();
        final Map<String, String> allTypes = new LinkedHashMap<>();
        final List<UnitData> units = new ArrayList<>();

        for (final CompilationUnitTree unit : parsedUnits) {
            final UnitData ud = new UnitData();
            ud.unit = unit;
            ud.path = Path.of(unit.getSourceFile().toUri());
            ud.source = readFile(ud.path);
            ud.packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
            packageMap.computeIfAbsent(ud.packageName, k -> {
                final PackageInfoData p = new PackageInfoData();
                p.name = k;
                return p;
            });

            for (final ImportTree it : unit.getImports()) {
                if (it.isStatic()) {
                    continue;
                }
                final String q = it.getQualifiedIdentifier().toString();
                if (q.endsWith(".*")) {
                    ud.wildcardImports.add(q.substring(0, q.length() - 2));
                } else {
                    final int idx = q.lastIndexOf('.');
                    if (idx > 0) {
                        ud.explicitImports.put(q.substring(idx + 1), q);
                    }
                }
            }

            if ("package-info.java".equals(ud.path.getFileName().toString()) && unit.getPackage() != null) {
                final DocInfo d = readDoc(docTrees, TreePath.getPath(unit, unit.getPackage()));
                if (d != null && !isBlank(d.summary)) {
                    packageMap.get(ud.packageName).summary = d.summary;
                }
            }

            for (final Tree t : unit.getTypeDecls()) {
                if (t instanceof ClassTree ct) {
                    final String simple = ct.getSimpleName().toString();
                    if (!simple.isEmpty()) {
                        final String fqn = ud.packageName.isEmpty() ? simple : ud.packageName + "." + simple;
                        typesByPackage.computeIfAbsent(ud.packageName, k -> new LinkedHashMap<>()).put(simple, fqn);
                        allTypes.put(fqn, fqn);
                    }
                }
            }

            units.add(ud);
        }

        int typeOrder = 0;
        methodOrder = 0;
        for (final UnitData ud : units) {
            for (final Tree t : ud.unit.getTypeDecls()) {
                if (t instanceof ClassTree ct) {
                    typeOrder = collectType(TreePath.getPath(ud.unit, ct), ud, null, null, packageMap, typesByPackage, allTypes, docTrees, sourcePositions,
                            typeOrder);
                }
            }
        }

        final List<PackageInfoData> packages = new ArrayList<>(packageMap.values());
        packages.sort(Comparator.comparing(p -> p.name));
        for (final PackageInfoData p : packages) {
            p.types.sort(Comparator.comparingInt(t -> t.order));
        }

        // Resolve summaries (and other empty doc fields) for overrides / {@inheritDoc} from supertypes.
        resolveInheritedDocumentation(packages);

        createParentDirectories(markdownOut);
        createParentDirectories(jsonOut);
        Files.writeString(markdownOut, sanitizeForUtf8(toMarkdown(library, packages), false), StandardCharsets.UTF_8);
        Files.writeString(jsonOut, sanitizeForUtf8(toJson(library, packages), true), StandardCharsets.UTF_8);

        final long errors = diagnostics.getDiagnostics().stream().filter(d -> d.getKind() == Diagnostic.Kind.ERROR).count();
        System.out.println("Generated " + markdownOut + " and " + jsonOut + " from " + javaFiles.size() + " files.");
        System.out.println("Diagnostics: " + diagnostics.getDiagnostics().size() + " (" + errors + " errors)");
    }

    private static int collectType(final TreePath typePath, final UnitData unitData, final TypeInfo parentType, final String enclosingFqn,
            final Map<String, PackageInfoData> packageMap, final Map<String, Map<String, String>> typesByPackage, final Map<String, String> allTypes,
            final DocTrees docTrees, final SourcePositions sourcePositions, final int startOrder) {
        int nextOrder = startOrder;
        final ClassTree classTree = (ClassTree) typePath.getLeaf();
        if (!isPublicType(classTree, parentType)) {
            return nextOrder;
        }

        final String simpleName = classTree.getSimpleName().toString();
        final String fqn;
        if (enclosingFqn == null) {
            fqn = unitData.packageName.isEmpty() ? simpleName : unitData.packageName + "." + simpleName;
        } else {
            fqn = enclosingFqn + "." + simpleName;
        }
        allTypes.putIfAbsent(fqn, fqn);

        final TypeInfo type = new TypeInfo();
        type.order = nextOrder++;
        type.fqn = fqn;
        type.name = simpleName;
        type.kind = mapTypeKind(classTree.getKind());
        type.modifiers = typeModifiers(classTree, parentType);
        type.annotations = readMarkerAnnotations(classTree.getModifiers());
        type.permittedSubtypes = classTree.getPermitsClause().stream().map(Tree::toString).map(ApiDocGenerator::normalize).collect(Collectors.toList());
        type.typeParams = readTypeParams(classTree.getTypeParameters());
        type.nullability = inferNullability(classTree.getModifiers().getAnnotations(), "");
        type.suppressDefaultConstructor = hasConstructorGeneratingAnnotation(classTree.getModifiers(), unitData);

        final DocInfo typeDoc = readDoc(docTrees, typePath);
        if (typeDoc != null) {
            type.javadocSummary = typeDoc.summary;
            type.since = typeDoc.since;
            type.apiNotes = typeDoc.apiNotes;
            type.implementationNotes = typeDoc.implementationNotes;
        }
        type.superTypes = readSuperTypes(classTree, unitData, typesByPackage, allTypes);
        type.recordComponents = readRecordComponents(classTree, unitData, sourcePositions, typeDoc);
        type.threadSafety = inferThreadSafety(typeDoc);
        type.deprecated = readDeprecated(classTree.getModifiers(), typeDoc);

        for (final Tree member : classTree.getMembers()) {
            if (member instanceof VariableTree varTree) {
                if (!isPublicField(varTree, classTree)) {
                    continue;
                }
                final FieldInfo field = new FieldInfo();
                field.name = varTree.getName().toString();
                field.type = normalize(varTree.getType() == null ? "" : varTree.getType().toString());
                field.modifiers = fieldModifiers(varTree.getModifiers(), type);
                if (varTree.getInitializer() != null && !isEnumConstant(varTree, classTree)) {
                    field.value = normalize(varTree.getInitializer().toString());
                }
                final DocInfo fieldDoc = readDoc(docTrees, new TreePath(typePath, varTree));
                if (fieldDoc != null) {
                    field.javadocSummary = expandFieldValue(fieldDoc.summary, field.value);
                }
                type.fields.add(field);
            } else if (member instanceof MethodTree methodTree) {
                if (isConstructor(methodTree, classTree)) {
                    type.declaresConstructor = true;
                    if (!isPublicConstructor(methodTree, classTree)) {
                        continue;
                    }
                    final DocInfo ctorDoc = readDoc(docTrees, new TreePath(typePath, methodTree));
                    final ConstructorInfo ctor = new ConstructorInfo();
                    ctor.signature = readMethodSignature(unitData, sourcePositions, methodTree);
                    ctor.annotations = readMarkerAnnotations(methodTree.getModifiers());
                    ctor.javadocSummary = ctorDoc == null ? null : ctorDoc.summary;
                    ctor.since = ctorDoc == null ? null : ctorDoc.since;
                    ctor.deprecated = readDeprecated(methodTree.getModifiers(), ctorDoc);
                    ctor.recordCanonical = isRecordCanonicalConstructor(methodTree, type.recordComponents);
                    ctor.params = readParams(methodTree, ctorDoc);
                    ctor.throwsList = readThrows(methodTree, ctorDoc, unitData, typesByPackage, allTypes, type);
                    if (ctorDoc != null) {
                        ctor.apiNotes = ctorDoc.apiNotes;
                        ctor.implementationNotes = ctorDoc.implementationNotes;
                        ctor.examples = ctorDoc.examples;
                    }
                    if (classTree.getKind() == Tree.Kind.RECORD && (ctor.signature == null || !ctor.signature.contains("("))) {
                        final List<ParamInfo> canonicalParams = type.recordComponents.stream().map(ApiDocGenerator::recordComponentParam)
                                .collect(Collectors.toList());
                        ctor.signature = recordConstructorSignature(type.name, canonicalParams);
                    }
                    type.constructors.add(ctor);
                } else {
                    if (!isPublicMethod(methodTree, classTree)) {
                        continue;
                    }
                    final DocInfo methodDoc = readDoc(docTrees, new TreePath(typePath, methodTree));
                    final MethodInfo method = new MethodInfo();
                    method.order = methodOrder++;
                    method.name = methodTree.getName().toString();
                    method.kind = methodTree.getModifiers().getFlags().contains(Modifier.STATIC) ? "static" : "instance";
                    method.modifiers = methodModifiers(methodTree, type);
                    method.annotations = readMarkerAnnotations(methodTree.getModifiers());
                    method.signature = readMethodSignature(unitData, sourcePositions, methodTree);
                    method.returnType = normalize(methodTree.getReturnType() == null ? "void" : methodTree.getReturnType().toString());
                    if (method.annotations.contains("MayReturnNull")) {
                        method.returnNullability = "nullable";
                    } else if (method.annotations.contains("NotNull")) {
                        method.returnNullability = "non-null";
                    }
                    method.typeParams = readTypeParams(methodTree.getTypeParameters());
                    method.params = readParams(methodTree, methodDoc);
                    method.throwsList = readThrows(methodTree, methodDoc, unitData, typesByPackage, allTypes, type);
                    if (methodDoc != null) {
                        method.javadocSummary = methodDoc.summary;
                        method.returns = methodDoc.returns;
                        method.since = methodDoc.since;
                        method.contract = methodDoc.contract;
                        method.performance = methodDoc.performance;
                        method.inheritsDocumentation = methodDoc.inheritsDocumentation;
                        method.apiNotes = methodDoc.apiNotes;
                        method.implementationNotes = methodDoc.implementationNotes;
                        method.examples = methodDoc.examples;
                        method.seeAlso = methodDoc.seeAlso;
                    }
                    method.deprecated = readDeprecated(methodTree.getModifiers(), methodDoc);
                    type.methods.add(method);
                }
            } else if (member instanceof ClassTree nested) {
                nextOrder = collectType(new TreePath(typePath, nested), unitData, type, fqn, packageMap, typesByPackage, allTypes, docTrees, sourcePositions,
                        nextOrder);
            }
        }

        completeDefaultConstructor(type);
        completeRecordApi(type);

        packageMap.get(unitData.packageName).types.add(type);
        return nextOrder;
    }

    private static boolean isPublicType(final ClassTree classTree, final TypeInfo parentType) {
        final Set<Modifier> flags = classTree.getModifiers().getFlags();
        if (flags.contains(Modifier.PUBLIC)) {
            return true;
        }
        if (parentType == null) {
            return false;
        }
        if ("interface".equals(parentType.kind) || "annotation".equals(parentType.kind)) {
            return !flags.contains(Modifier.PRIVATE);
        }
        return false;
    }

    private static boolean isConstructor(final MethodTree methodTree, final ClassTree owner) {
        // javac's parse tree stores constructors under the name "<init>", not the class name.
        return methodTree.getReturnType() == null
                && (methodTree.getName().contentEquals("<init>") || methodTree.getName().contentEquals(owner.getSimpleName()));
    }

    private static boolean isPublicConstructor(final MethodTree methodTree, final ClassTree owner) {
        final Set<Modifier> flags = methodTree.getModifiers().getFlags();
        if (flags.contains(Modifier.PUBLIC)) {
            return true;
        }
        return owner.getKind() == Tree.Kind.RECORD && !flags.contains(Modifier.PRIVATE) && !flags.contains(Modifier.PROTECTED);
    }

    private static boolean isPublicMethod(final MethodTree methodTree, final ClassTree owner) {
        final Set<Modifier> flags = methodTree.getModifiers().getFlags();
        if (flags.contains(Modifier.PUBLIC)) {
            return true;
        }
        if (flags.contains(Modifier.PRIVATE)) {
            return false;
        }
        return owner.getKind() == Tree.Kind.INTERFACE || owner.getKind() == Tree.Kind.ANNOTATION_TYPE;
    }

    private static boolean isPublicField(final VariableTree variableTree, final ClassTree owner) {
        final Set<Modifier> flags = variableTree.getModifiers().getFlags();
        if (flags.contains(Modifier.PUBLIC)) {
            return true;
        }
        if (flags.contains(Modifier.PRIVATE)) {
            return false;
        }
        return owner.getKind() == Tree.Kind.INTERFACE || owner.getKind() == Tree.Kind.ANNOTATION_TYPE;
    }

    private static boolean isEnumConstant(final VariableTree variableTree, final ClassTree owner) {
        if (owner.getKind() != Tree.Kind.ENUM || variableTree.getType() == null || variableTree.getInitializer() == null) {
            return false;
        }
        final Set<Modifier> flags = variableTree.getModifiers().getFlags();
        return flags.contains(Modifier.PUBLIC) && flags.contains(Modifier.STATIC) && flags.contains(Modifier.FINAL)
                && owner.getSimpleName().contentEquals(simpleName(normalize(variableTree.getType().toString())))
                && variableTree.getInitializer().getKind() == Tree.Kind.NEW_CLASS;
    }

    private static List<String> typeModifiers(final ClassTree classTree, final TypeInfo ownerType) {
        final Set<Modifier> modifiers = new LinkedHashSet<>(classTree.getModifiers().getFlags());
        final Tree.Kind kind = classTree.getKind();
        if (kind == Tree.Kind.INTERFACE || kind == Tree.Kind.ANNOTATION_TYPE) {
            modifiers.add(Modifier.ABSTRACT);
        } else if (kind == Tree.Kind.RECORD) {
            modifiers.add(Modifier.FINAL);
        }
        if (ownerType != null) {
            if ("interface".equals(ownerType.kind) || "annotation".equals(ownerType.kind)) {
                modifiers.add(Modifier.PUBLIC);
                modifiers.add(Modifier.STATIC);
            } else if (kind == Tree.Kind.INTERFACE || kind == Tree.Kind.ANNOTATION_TYPE || kind == Tree.Kind.ENUM || kind == Tree.Kind.RECORD) {
                modifiers.add(Modifier.STATIC);
            }
        }
        return sortedModifiers(modifiers);
    }

    private static List<String> fieldModifiers(final ModifiersTree modifiersTree, final TypeInfo ownerType) {
        final Set<Modifier> modifiers = new LinkedHashSet<>(modifiersTree.getFlags());
        if ("interface".equals(ownerType.kind) || "annotation".equals(ownerType.kind)) {
            modifiers.add(Modifier.PUBLIC);
            modifiers.add(Modifier.STATIC);
            modifiers.add(Modifier.FINAL);
        }
        return sortedModifiers(modifiers);
    }

    private static List<String> methodModifiers(final MethodTree methodTree, final TypeInfo ownerType) {
        final Set<Modifier> modifiers = new LinkedHashSet<>(methodTree.getModifiers().getFlags());
        if ("interface".equals(ownerType.kind) || "annotation".equals(ownerType.kind)) {
            modifiers.add(Modifier.PUBLIC);
            if (methodTree.getBody() == null && !modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.DEFAULT)) {
                modifiers.add(Modifier.ABSTRACT);
            }
        }
        return sortedModifiers(modifiers);
    }

    private static List<String> sortedModifiers(final Set<Modifier> modifiers) {
        final List<String> out = new ArrayList<>();
        final List<Modifier> order = List.of(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.ABSTRACT, Modifier.STATIC, Modifier.FINAL,
                Modifier.SEALED, Modifier.NON_SEALED, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.SYNCHRONIZED, Modifier.NATIVE, Modifier.STRICTFP,
                Modifier.DEFAULT);
        for (final Modifier m : order) {
            if (modifiers.contains(m)) {
                out.add(m.toString().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static List<RecordComponentInfo> readRecordComponents(final ClassTree classTree, final UnitData unitData,
            final SourcePositions sourcePositions, final DocInfo typeDoc) {
        final List<RecordComponentInfo> out = new ArrayList<>();
        if (classTree.getKind() != Tree.Kind.RECORD) {
            return out;
        }

        final long typeStart = sourcePositions.getStartPosition(unitData.unit, classTree);
        if (typeStart < 0 || typeStart >= unitData.source.length()) {
            return out;
        }

        final int[] componentBounds = findRecordComponentBounds(unitData.source, (int) typeStart, classTree.getSimpleName().toString());
        if (componentBounds == null) {
            return out;
        }

        for (final Tree member : classTree.getMembers()) {
            if (!(member instanceof VariableTree component)) {
                continue;
            }
            final long componentStart = sourcePositions.getStartPosition(unitData.unit, component);
            if (componentStart <= componentBounds[0] || componentStart >= componentBounds[1]) {
                continue;
            }
            final long componentEnd = sourcePositions.getEndPosition(unitData.unit, component);

            final RecordComponentInfo info = new RecordComponentInfo();
            info.name = component.getName().toString();
            info.type = normalize(component.getType() == null ? "" : component.getType().toString());
            info.varArgs = componentEnd > componentStart && componentEnd <= unitData.source.length()
                    && unitData.source.substring((int) componentStart, (int) componentEnd).contains("...");
            info.annotations = readMarkerAnnotations(component.getModifiers());
            info.javadoc = typeDoc == null ? null : typeDoc.paramDocs.get(info.name);
            info.nullability = inferNullability(component.getModifiers().getAnnotations(), info.type);
            out.add(info);
        }
        return out;
    }

    private static int[] findRecordComponentBounds(final String source, final int typeStart, final String simpleName) {
        final int recordIndex = indexOfWord(source, "record", typeStart);
        if (recordIndex < 0) {
            return null;
        }
        final int nameIndex = source.indexOf(simpleName, recordIndex + "record".length());
        if (nameIndex < 0) {
            return null;
        }
        final int open = source.indexOf('(', nameIndex + simpleName.length());
        if (open < 0) {
            return null;
        }
        final int close = findMatchingParenthesis(source, open);
        return close < 0 ? null : new int[] { open, close };
    }

    private static int indexOfWord(final String source, final String word, final int fromIndex) {
        int index = source.indexOf(word, fromIndex);
        while (index >= 0) {
            final int end = index + word.length();
            final boolean startsAtWord = index == 0 || !Character.isJavaIdentifierPart(source.charAt(index - 1));
            final boolean endsAtWord = end >= source.length() || !Character.isJavaIdentifierPart(source.charAt(end));
            if (startsAtWord && endsAtWord) {
                return index;
            }
            index = source.indexOf(word, end);
        }
        return -1;
    }

    private static int findMatchingParenthesis(final String source, final int openIndex) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = openIndex; i < source.length(); i++) {
            final char c = source.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static void completeRecordApi(final TypeInfo type) {
        if (!"record".equals(type.kind)) {
            return;
        }

        if (type.constructors.stream().noneMatch(c -> c.recordCanonical)) {
            final ConstructorInfo ctor = new ConstructorInfo();
            ctor.recordCanonical = true;
            ctor.params = type.recordComponents.stream().map(ApiDocGenerator::recordComponentParam).collect(Collectors.toList());
            ctor.signature = recordConstructorSignature(type.name, ctor.params);
            type.constructors.add(0, ctor);
        }

        for (final RecordComponentInfo component : type.recordComponents) {
            final boolean accessorDeclared = type.methods.stream().anyMatch(m -> component.name.equals(m.name) && m.params.isEmpty());
            if (accessorDeclared) {
                continue;
            }

            final MethodInfo accessor = new MethodInfo();
            accessor.order = methodOrder++;
            accessor.name = component.name;
            accessor.kind = "instance";
            accessor.modifiers = List.of("public");
            accessor.annotations = new ArrayList<>(component.annotations);
            accessor.signature = "public " + component.type + " " + component.name + "()";
            accessor.returnType = component.type;
            accessor.returnNullability = component.nullability;
            accessor.javadocSummary = component.javadoc;
            accessor.returns = component.javadoc;
            type.methods.add(accessor);
        }

        addImplicitRecordMethod(type, "equals", "boolean", "public final boolean equals(Object o)", "o", "Object");
        addImplicitRecordMethod(type, "hashCode", "int", "public final int hashCode()", null, null);
        addImplicitRecordMethod(type, "toString", "String", "public final String toString()", null, null);
    }

    private static void completeDefaultConstructor(final TypeInfo type) {
        if (!"class".equals(type.kind) || type.declaresConstructor || type.suppressDefaultConstructor) {
            return;
        }
        final ConstructorInfo ctor = new ConstructorInfo();
        ctor.signature = "public " + type.name + "()";
        type.constructors.add(ctor);
    }

    private static void addImplicitRecordMethod(final TypeInfo type, final String name, final String returnType, final String signature,
            final String paramName, final String paramType) {
        final int paramCount = paramName == null ? 0 : 1;
        if (type.methods.stream().anyMatch(m -> name.equals(m.name) && m.params.size() == paramCount)) {
            return;
        }
        final MethodInfo method = new MethodInfo();
        method.order = methodOrder++;
        method.name = name;
        method.kind = "instance";
        method.modifiers = List.of("public", "final");
        method.signature = signature;
        method.returnType = returnType;
        if (paramName != null) {
            final ParamInfo param = new ParamInfo();
            param.name = paramName;
            param.type = paramType;
            method.params.add(param);
        }
        type.methods.add(method);
    }

    private static ParamInfo recordComponentParam(final RecordComponentInfo component) {
        final ParamInfo param = new ParamInfo();
        param.name = component.name;
        param.type = component.varArgs && component.type.endsWith("[]") ? component.type.substring(0, component.type.length() - 2) + "..." : component.type;
        param.javadoc = component.javadoc;
        param.nullability = component.nullability;
        return param;
    }

    private static String recordConstructorSignature(final String typeName, final List<ParamInfo> params) {
        return "public " + typeName + "(" + params.stream().map(p -> p.type + " " + p.name).collect(Collectors.joining(", ")) + ")";
    }

    private static boolean isRecordCanonicalConstructor(final MethodTree methodTree, final List<RecordComponentInfo> components) {
        if (methodTree.getParameters().size() != components.size()) {
            return false;
        }
        for (int i = 0; i < components.size(); i++) {
            final VariableTree param = methodTree.getParameters().get(i);
            final RecordComponentInfo component = components.get(i);
            if (!param.getName().contentEquals(component.name)
                    || !component.type.equals(normalize(param.getType() == null ? "" : param.getType().toString()))) {
                return false;
            }
        }
        return true;
    }

    private static List<TypeParamInfo> readTypeParams(final List<? extends TypeParameterTree> typeParameters) {
        final List<TypeParamInfo> out = new ArrayList<>();
        for (final TypeParameterTree tp : typeParameters) {
            final TypeParamInfo info = new TypeParamInfo();
            info.name = tp.getName().toString();
            if (tp.getBounds() == null || tp.getBounds().isEmpty()) {
                info.bounds.add("java.lang.Object");
            } else {
                for (final Tree bound : tp.getBounds()) {
                    info.bounds.add(normalize(bound.toString()));
                }
            }
            out.add(info);
        }
        return out;
    }

    private static String readMethodSignature(final UnitData unitData, final SourcePositions sourcePositions, final MethodTree methodTree) {
        final long start = sourcePositions.getStartPosition(unitData.unit, methodTree);
        if (start < 0 || start >= unitData.source.length()) {
            return normalize(methodTree.toString());
        }
        long end = sourcePositions.getEndPosition(unitData.unit, methodTree);
        final BlockTree body = methodTree.getBody();
        if (body != null) {
            final long bodyStart = sourcePositions.getStartPosition(unitData.unit, body);
            if (bodyStart > start) {
                end = bodyStart;
            }
        }
        if (end < 0 || end > unitData.source.length()) {
            end = unitData.source.length();
        }
        String value = unitData.source.substring((int) start, (int) end).trim();
        if (value.endsWith("{")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return normalize(value);
    }

    private static DocInfo readDoc(final DocTrees docTrees, final TreePath path) {
        if (path == null) {
            return null;
        }
        final DocCommentTree comment = docTrees.getDocCommentTree(path);
        if (comment == null) {
            return null;
        }

        final DocInfo doc = new DocInfo();
        doc.inheritsDocumentation = comment.toString().contains("{@inheritDoc}");
        doc.summary = renderDocText(comment.getFirstSentence());
        doc.body = renderDocText(comment.getBody());
        // {@inheritDoc} is rendered as empty text. When it is the whole first sentence (optionally
        // with a trailing period), prefer the first sentence of the remaining body if any.
        if (isBlankSummary(doc.summary) && !isBlank(doc.body)) {
            final int sentenceEnd = firstSentenceEnd(doc.body);
            doc.summary = tidyRenderedText(sentenceEnd < 0 ? doc.body : doc.body.substring(0, sentenceEnd + 1));
        }
        doc.summary = completeAbbreviatedSummary(doc.summary, doc.body);
        if (isBlankSummary(doc.summary)) {
            doc.summary = null;
        }
        doc.examples = readExamples(comment);
        if (!isBlank(doc.body)) {
            for (final String sentence : doc.body.split("(?<=[.!?])\\s+")) {
                final String s = sentence.trim();
                if (s.isEmpty()) {
                    continue;
                }
                final String lower = s.toLowerCase(Locale.ROOT);
                if (lower.contains(" o(") || lower.startsWith("o(") || lower.contains("complexity")) {
                    doc.performance = s;
                } else if (lower.contains("must") || lower.contains("if ") || lower.contains("when ") || lower.contains("should")) {
                    doc.contract.add(s);
                }
            }
        }

        for (final DocTree tag : comment.getBlockTags()) {
            switch (tag.getKind()) {
                case SINCE -> doc.since = normalize(tag.toString().replaceFirst("@since", ""));
                case PARAM -> {
                    final ParamTree p = (ParamTree) tag;
                    doc.paramDocs.put(p.getName().toString(), renderDocText(p.getDescription()));
                }
                case RETURN -> {
                    final ReturnTree r = (ReturnTree) tag;
                    doc.returns = renderDocText(r.getDescription());
                }
                case THROWS, EXCEPTION -> {
                    final ThrowsTree t = (ThrowsTree) tag;
                    final String key = normalize(t.getExceptionName().toString());
                    final String value = renderDocText(t.getDescription());
                    doc.throwsDocs.put(key, value);
                    doc.throwsDocs.putIfAbsent(simpleName(key), value);
                    final ThrowInfo documentedThrow = new ThrowInfo();
                    documentedThrow.type = key;
                    documentedThrow.condition = value;
                    doc.documentedThrows.add(documentedThrow);
                }
                case SEE -> {
                    final SeeTree s = (SeeTree) tag;
                    final String ref = renderSeeReference(s.getReference());
                    if (!isBlank(ref)) {
                        doc.seeAlso.add(ref);
                    }
                }
                case DEPRECATED -> {
                    final DeprecatedTree d = (DeprecatedTree) tag;
                    doc.deprecatedMessage = renderDocText(d.getBody());
                }
                case UNKNOWN_BLOCK_TAG -> {
                    final UnknownBlockTagTree unknown = (UnknownBlockTagTree) tag;
                    final String value = renderDocText(unknown.getContent());
                    if (!isBlank(value)) {
                        if ("apiNote".equals(unknown.getTagName())) {
                            doc.apiNotes.add(value);
                        } else if ("implNote".equals(unknown.getTagName())) {
                            doc.implementationNotes.add(value);
                        }
                    }
                }
                default -> {
                }
            }
        }
        return doc;
    }

    /**
     * Renders javadoc description trees to plain text: inline {@code ...} becomes `backticked`,
     * {@literal ...} is unwrapped, {@link ...} keeps its label (or reference), HTML tags are
     * dropped, and common HTML entities are decoded. Preformatted blocks, including multi-line
     * {@code ...} usage examples and plain-text tables, are excluded here; code examples are
     * extracted separately by readExamples.
     */
    private static String renderDocText(final List<? extends DocTree> trees) {
        if (trees == null || trees.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        appendDocText(trees, sb);
        return tidyRenderedText(sb.toString());
    }

    private static void appendDocText(final List<? extends DocTree> trees, final StringBuilder sb) {
        int preformattedDepth = 0;
        for (final DocTree tree : trees) {
            if (tree.getKind() == DocTree.Kind.START_ELEMENT && "pre".contentEquals(((StartElementTree) tree).getName())) {
                preformattedDepth++;
                continue;
            }
            if (tree.getKind() == DocTree.Kind.END_ELEMENT && "pre".contentEquals(((EndElementTree) tree).getName())) {
                preformattedDepth = Math.max(0, preformattedDepth - 1);
                continue;
            }
            if (preformattedDepth > 0) {
                continue;
            }
            switch (tree.getKind()) {
                case TEXT -> sb.append(((TextTree) tree).getBody());
                case CODE -> {
                    final String body = ((LiteralTree) tree).getBody().getBody();
                    final String code = body == null ? "" : body.strip();
                    if (!code.isEmpty() && !code.contains("\n")) {
                        sb.append('`').append(code).append('`');
                    }
                }
                case LITERAL -> sb.append(((LiteralTree) tree).getBody().getBody());
                case LINK, LINK_PLAIN -> {
                    final LinkTree link = (LinkTree) tree;
                    if (link.getLabel() == null || link.getLabel().isEmpty()) {
                        sb.append(link.getReference() == null ? "" : link.getReference().getSignature());
                    } else {
                        appendDocText(link.getLabel(), sb);
                    }
                }
                case ENTITY -> sb.append(decodeEntity(((EntityTree) tree).getName().toString()));
                case VALUE -> {
                    final ValueTree value = (ValueTree) tree;
                    if (value.getReference() == null) {
                        sb.append("{@value}");
                    } else {
                        sb.append('`').append(value.getReference().getSignature()).append('`');
                    }
                }
                // Resolved later from supertypes; do not emit a synthetic placeholder that would
                // pollute method summaries (e.g. "inherited documentation Applies the consumer...").
                case INHERIT_DOC -> {
                }
                case START_ELEMENT -> appendHtmlBoundary(((StartElementTree) tree).getName().toString(), sb);
                case END_ELEMENT -> appendHtmlBoundary(((EndElementTree) tree).getName().toString(), sb);
                default -> sb.append(' ').append(tree).append(' ');
            }
        }
    }

    /**
     * Inline tags ({@code <b>}, {@code <a>}, {@code <i>}, …) are dropped without adding spaces so
     * punctuation stays attached (e.g. {@code <b>Supplier</b>.} → {@code Supplier.}). Block tags
     * insert a single separating space.
     */
    private static void appendHtmlBoundary(final String tagName, final StringBuilder sb) {
        if (tagName == null) {
            return;
        }
        final String name = tagName.toLowerCase(Locale.ROOT);
        if (INLINE_HTML_TAGS.contains(name)) {
            return;
        }
        if (BLOCK_HTML_TAGS.contains(name) || name.length() > 0) {
            appendSpaceIfNeeded(sb);
        }
    }

    private static void appendSpaceIfNeeded(final StringBuilder sb) {
        if (sb.length() == 0) {
            return;
        }
        final char last = sb.charAt(sb.length() - 1);
        if (!Character.isWhitespace(last)) {
            sb.append(' ');
        }
    }

    /**
     * Collapses whitespace and removes spaces that HTML/doc-tree rendering can leave before
     * punctuation (e.g. {@code prime number .} → {@code prime number.}).
     */
    private static String tidyRenderedText(final String value) {
        if (value == null) {
            return null;
        }
        String v = value.replaceAll("\\s+([\\.,;:!?])", "$1");
        v = v.replaceAll("\\(\\s+", "(").replaceAll("\\s+\\)", ")");
        return normalize(v);
    }

    private static boolean isBlankSummary(final String summary) {
        if (isBlank(summary)) {
            return true;
        }
        final String trimmed = summary.trim();
        // Bare punctuation left after stripping {@inheritDoc} (e.g. "{@inheritDoc}.")
        if (trimmed.matches("[.!?…]+")) {
            return true;
        }
        final String lower = trimmed.toLowerCase(Locale.ROOT);
        return "inherited documentation".equals(lower) || "inherited documentation.".equals(lower);
    }

    private static String renderSeeReference(final List<? extends DocTree> trees) {
        if (trees == null || trees.isEmpty()) {
            return null;
        }

        String href = null;
        final List<DocTree> labelTrees = new ArrayList<>();
        boolean inAnchor = false;
        for (final DocTree tree : trees) {
            if (tree.getKind() == DocTree.Kind.START_ELEMENT) {
                final StartElementTree start = (StartElementTree) tree;
                if ("a".contentEquals(start.getName())) {
                    inAnchor = true;
                    for (final DocTree attribute : start.getAttributes()) {
                        if (attribute instanceof AttributeTree attr && "href".contentEquals(attr.getName())) {
                            href = renderDocText(attr.getValue());
                            break;
                        }
                    }
                    continue;
                }
            } else if (tree.getKind() == DocTree.Kind.END_ELEMENT) {
                final EndElementTree end = (EndElementTree) tree;
                if ("a".contentEquals(end.getName())) {
                    inAnchor = false;
                    continue;
                }
            }
            if (inAnchor) {
                labelTrees.add(tree);
            }
        }

        if (href != null) {
            final String label = renderDocText(labelTrees);
            return isBlank(label) || href.equals(label) ? href : label + " (" + href + ")";
        }
        return renderDocText(trees);
    }

    private static String completeAbbreviatedSummary(final String summary, final String body) {
        if (isBlank(summary) || isBlank(body)) {
            return summary;
        }
        final String lower = summary.toLowerCase(Locale.ROOT);
        if (!lower.endsWith("i.e.") && !lower.endsWith("e.g.")) {
            return summary;
        }

        final int sentenceEnd = firstSentenceEnd(body);
        final String continuation = sentenceEnd < 0 ? body : body.substring(0, sentenceEnd + 1);
        return normalize(summary + " " + continuation);
    }

    private static int firstSentenceEnd(final String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && (i + 1 == value.length() || Character.isWhitespace(value.charAt(i + 1)))) {
                return i;
            }
        }
        return -1;
    }

    private static String decodeEntity(final String name) {
        if (name != null && name.startsWith("#")) {
            try {
                final boolean hexadecimal = name.length() > 2 && (name.charAt(1) == 'x' || name.charAt(1) == 'X');
                final int codePoint = Integer.parseInt(name.substring(hexadecimal ? 2 : 1), hexadecimal ? 16 : 10);
                if (Character.isValidCodePoint(codePoint) && !(codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE)) {
                    return new String(Character.toChars(codePoint));
                }
            } catch (final NumberFormatException ignored) {
                // Preserve malformed or out-of-range references verbatim below.
            }
        }
        return switch (name) {
            case "amp" -> "&";
            case "lt" -> "<";
            case "gt" -> ">";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "nbsp", "ensp", "emsp", "thinsp" -> " ";
            case "mdash" -> "\u2014";
            case "ndash" -> "\u2013";
            case "hellip" -> "\u2026";
            case "larr" -> "\u2190";
            case "rarr" -> "\u2192";
            case "uarr" -> "\u2191";
            case "darr" -> "\u2193";
            case "harr" -> "\u2194";
            case "middot" -> "\u00B7";
            case "bull" -> "\u2022";
            case "times" -> "\u00D7";
            case "plusmn" -> "\u00B1";
            case "le" -> "\u2264";
            case "ge" -> "\u2265";
            case "ne" -> "\u2260";
            case "copy" -> "\u00A9";
            case "reg" -> "\u00AE";
            case "trade" -> "\u2122";
            default -> "&" + name + ";";
        };
    }

    private static String expandFieldValue(final String summary, final String value) {
        if (isBlank(summary) || isBlank(value) || !summary.contains("{@value}")) {
            return summary;
        }
        return summary.replace("{@value}", "`" + value + "`");
    }

    /**
     * Extracts usage examples from a doc comment: {@code ...} blocks nested in {@code <pre>}
     * elements, plus multi-line code blocks retained for compatibility with older Javadocs.
     */
    private static List<String> readExamples(final DocCommentTree comment) {
        final List<String> out = new ArrayList<>();
        int preformattedDepth = 0;
        for (final DocTree tree : comment.getFullBody()) {
            if (tree.getKind() == DocTree.Kind.START_ELEMENT && "pre".contentEquals(((StartElementTree) tree).getName())) {
                preformattedDepth++;
                continue;
            }
            if (tree.getKind() == DocTree.Kind.END_ELEMENT && "pre".contentEquals(((EndElementTree) tree).getName())) {
                preformattedDepth = Math.max(0, preformattedDepth - 1);
                continue;
            }
            if (tree.getKind() != DocTree.Kind.CODE) {
                continue;
            }

            final String body = ((LiteralTree) tree).getBody().getBody();
            final String code = body == null ? "" : body.strip();
            if (!code.isEmpty() && (preformattedDepth > 0 || code.contains("\n") || code.contains("\r"))) {
                out.add(code);
            }
        }
        return out;
    }

    private static List<String> readMarkerAnnotations(final ModifiersTree modifiers) {
        final List<String> out = new ArrayList<>();
        for (final AnnotationTree ann : modifiers.getAnnotations()) {
            final String name = simpleName(normalize(ann.getAnnotationType().toString()));
            if (name != null && MARKER_ANNOTATIONS.contains(name)) {
                out.add(name);
            }
        }
        return out;
    }

    private static boolean hasConstructorGeneratingAnnotation(final ModifiersTree modifiers, final UnitData unitData) {
        for (final AnnotationTree annotation : modifiers.getAnnotations()) {
            final String declaredName = normalize(annotation.getAnnotationType().toString());
            if (CONSTRUCTOR_GENERATING_ANNOTATIONS.contains(declaredName)) {
                return true;
            }
            final String explicitImport = unitData.explicitImports.get(declaredName);
            if (explicitImport != null && CONSTRUCTOR_GENERATING_ANNOTATIONS.contains(explicitImport)) {
                return true;
            }
            for (final String wildcardImport : unitData.wildcardImports) {
                if (CONSTRUCTOR_GENERATING_ANNOTATIONS.contains(wildcardImport + "." + declaredName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String inferThreadSafety(final DocInfo doc) {
        final String summary = doc == null || doc.summary == null ? "" : doc.summary;
        final String body = doc == null || doc.body == null ? "" : doc.body;
        final String text = (summary + " " + body).toLowerCase(Locale.ROOT);

        if (NOT_THREAD_SAFE.matcher(text).find() || text.contains("external synchronization is required")
                || text.contains("requires external synchronization")) {
            return "not thread-safe";
        }
        if (CONDITIONAL_THREAD_SAFETY.matcher(text).find()) {
            return "conditional";
        }
        if (THREAD_SAFETY_DISCLAIMER.matcher(text).find()) {
            return "unspecified";
        }
        if (THREAD_SAFE.matcher(text).find() || text.contains("safe for concurrent access") || text.contains("safe for concurrent use")) {
            return "thread-safe";
        }
        return "unspecified";
    }

    private static List<ParamInfo> readParams(final MethodTree methodTree, final DocInfo doc) {
        final List<ParamInfo> out = new ArrayList<>();
        for (final VariableTree param : methodTree.getParameters()) {
            final ParamInfo p = new ParamInfo();
            p.name = param.getName().toString();
            final String annotations = param.getModifiers().getAnnotations().stream().map(a -> normalize(a.toString())).collect(Collectors.joining(" "));
            final String type = normalize(param.getType() == null ? "" : param.getType().toString());
            p.type = annotations.isEmpty() ? type : annotations + " " + type;
            p.javadoc = doc == null ? null : doc.paramDocs.get(p.name);
            p.nullability = inferNullability(param.getModifiers().getAnnotations(), p.type);
            out.add(p);
        }
        return out;
    }

    private static List<ThrowInfo> readThrows(final MethodTree methodTree, final DocInfo doc, final UnitData unitData,
            final Map<String, Map<String, String>> typesByPackage, final Map<String, String> allTypes, final TypeInfo ownerType) {
        final List<ThrowInfo> out = new ArrayList<>();
        final Set<String> includedTypes = new LinkedHashSet<>();
        final Set<String> ownerTypeParams = ownerType.typeParams.stream().map(tp -> tp.name).collect(Collectors.toSet());
        final Set<String> methodTypeParams = methodTree.getTypeParameters().stream().map(tp -> tp.getName().toString()).collect(Collectors.toSet());
        for (final ExpressionTree thrownType : methodTree.getThrows()) {
            final String declared = normalize(thrownType.toString());
            final ThrowInfo t = new ThrowInfo();
            t.type = resolveExceptionType(declared, unitData, typesByPackage, allTypes, ownerTypeParams, methodTypeParams);
            includedTypes.add(t.type);
            includedTypes.add(simpleName(t.type));
            if (doc != null) {
                String condition = doc.throwsDocs.get(declared);
                if (isBlank(condition)) {
                    condition = doc.throwsDocs.get(simpleName(declared));
                }
                t.condition = condition;
            }
            out.add(t);
        }
        if (doc != null) {
            for (final ThrowInfo documented : doc.documentedThrows) {
                final String resolved = resolveExceptionType(documented.type, unitData, typesByPackage, allTypes, ownerTypeParams, methodTypeParams);
                if (includedTypes.contains(resolved) || includedTypes.contains(simpleName(resolved))) {
                    continue;
                }
                final ThrowInfo t = new ThrowInfo();
                t.type = resolved;
                t.condition = documented.condition;
                out.add(t);
                includedTypes.add(resolved);
                includedTypes.add(simpleName(resolved));
            }
        }
        return out;
    }

    private static String resolveExceptionType(final String declared, final UnitData unitData, final Map<String, Map<String, String>> typesByPackage,
            final Map<String, String> allTypes, final Set<String> ownerTypeParams, final Set<String> methodTypeParams) {
        if (isBlank(declared) || declared.contains(".") || ownerTypeParams.contains(declared) || methodTypeParams.contains(declared)) {
            return declared;
        }
        final String explicit = unitData.explicitImports.get(declared);
        if (explicit != null) {
            return explicit;
        }
        final Map<String, String> inPackage = typesByPackage.get(unitData.packageName);
        if (inPackage != null && inPackage.containsKey(declared)) {
            return inPackage.get(declared);
        }
        for (final String wildcardImport : unitData.wildcardImports) {
            final String candidate = wildcardImport + "." + declared;
            if (allTypes.containsKey(candidate) || "java.lang".equals(wildcardImport)) {
                return candidate;
            }
        }
        if (declared.endsWith("Exception") || declared.endsWith("Error") || "Throwable".equals(declared)) {
            return "java.lang." + declared;
        }
        if (!unitData.packageName.isEmpty()) {
            final String candidate = unitData.packageName + "." + declared;
            if (allTypes.containsKey(candidate)) {
                return candidate;
            }
        }
        return declared;
    }

    private static List<String> readSuperTypes(final ClassTree classTree, final UnitData unitData, final Map<String, Map<String, String>> typesByPackage,
            final Map<String, String> allTypes) {
        final List<String> out = new ArrayList<>();
        final Set<String> seen = new LinkedHashSet<>();
        final Tree extendsClause = classTree.getExtendsClause();
        if (extendsClause != null) {
            addResolvedSuperType(out, seen, extendsClause.toString(), unitData, typesByPackage, allTypes);
        }
        for (final Tree implemented : classTree.getImplementsClause()) {
            addResolvedSuperType(out, seen, implemented.toString(), unitData, typesByPackage, allTypes);
        }
        return out;
    }

    private static void addResolvedSuperType(final List<String> out, final Set<String> seen, final String declared, final UnitData unitData,
            final Map<String, Map<String, String>> typesByPackage, final Map<String, String> allTypes) {
        final String resolved = resolveDeclaredTypeName(declared, unitData, typesByPackage, allTypes);
        if (!isBlank(resolved) && seen.add(resolved)) {
            out.add(resolved);
        }
    }

    /**
     * Resolves a declared supertype (possibly with type arguments or annotations) to an FQN without
     * type arguments, using the same import/package rules as exception resolution.
     */
    private static String resolveDeclaredTypeName(final String declared, final UnitData unitData, final Map<String, Map<String, String>> typesByPackage,
            final Map<String, String> allTypes) {
        if (isBlank(declared)) {
            return declared;
        }
        String raw = declared.trim();
        // Drop leading annotations: @Foo Bar / @Foo(Bar) Baz
        while (raw.startsWith("@")) {
            int i = 1;
            while (i < raw.length() && (Character.isJavaIdentifierPart(raw.charAt(i)) || raw.charAt(i) == '.')) {
                i++;
            }
            if (i < raw.length() && raw.charAt(i) == '(') {
                int depth = 0;
                for (; i < raw.length(); i++) {
                    final char c = raw.charAt(i);
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            i++;
                            break;
                        }
                    }
                }
            }
            raw = raw.substring(i).trim();
        }
        final int genericStart = raw.indexOf('<');
        if (genericStart >= 0) {
            raw = raw.substring(0, genericStart).trim();
        }
        final int space = raw.lastIndexOf(' ');
        if (space >= 0) {
            raw = raw.substring(space + 1).trim();
        }
        if (raw.contains(".")) {
            // Nested simple form Outer.Inner may still need package prefixing.
            if (allTypes.containsKey(raw)) {
                return raw;
            }
            final String first = raw.substring(0, raw.indexOf('.'));
            final String rest = raw.substring(raw.indexOf('.'));
            final String resolvedOuter = resolveExceptionType(first, unitData, typesByPackage, allTypes, Set.of(), Set.of());
            if (!first.equals(resolvedOuter) && resolvedOuter.contains(".")) {
                final String candidate = resolvedOuter + rest;
                if (allTypes.containsKey(candidate)) {
                    return candidate;
                }
                return candidate;
            }
            return raw;
        }
        return resolveExceptionType(raw, unitData, typesByPackage, allTypes, Set.of(), Set.of());
    }

    /**
     * Fills blank method summaries (and other empty doc fields) from matching methods on supertypes.
     * Covers pure {@code {@inheritDoc}} and undocumented overrides such as interface implementations.
     */
    private static void resolveInheritedDocumentation(final List<PackageInfoData> packages) {
        final Map<String, TypeInfo> byFqn = new LinkedHashMap<>();
        for (final PackageInfoData p : packages) {
            for (final TypeInfo t : p.types) {
                byFqn.put(t.fqn, t);
            }
        }

        final Set<String> resolved = new LinkedHashSet<>();
        for (final TypeInfo type : byFqn.values()) {
            resolveInheritedDocumentation(type, byFqn, resolved, new LinkedHashSet<>());
        }
    }

    /** Resolves supertypes first so inherited documentation never depends on source-file order. */
    private static void resolveInheritedDocumentation(final TypeInfo type, final Map<String, TypeInfo> byFqn, final Set<String> resolved,
            final Set<String> resolving) {
        if (resolved.contains(type.fqn) || !resolving.add(type.fqn)) {
            return;
        }

        for (final String superFqn : type.superTypes) {
            final TypeInfo superType = byFqn.get(superFqn);
            if (superType != null) {
                resolveInheritedDocumentation(superType, byFqn, resolved, resolving);
            }
        }

        for (final MethodInfo method : type.methods) {
            if (needsInheritedDocumentation(method)) {
                inheritMethodDocumentation(type, method, byFqn, new LinkedHashSet<>());
            }
        }

        resolving.remove(type.fqn);
        resolved.add(type.fqn);
    }

    private static boolean needsInheritedDocumentation(final MethodInfo method) {
        if (isBlank(method.javadocSummary) || !hasCompleteParamDocs(method)) {
            return true;
        }
        if (!"void".equals(method.returnType) && isBlank(method.returns)) {
            return true;
        }
        for (final ThrowInfo thrown : method.throwsList) {
            if (isBlank(thrown.condition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCompleteParamDocs(final MethodInfo method) {
        if (method.params.isEmpty()) {
            return true;
        }
        for (final ParamInfo p : method.params) {
            if (isBlank(p.javadoc)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Merges matching declarations from nearest to farthest supertypes. A declaration with only
     * {@code @param}, {@code @return}, or {@code @throws} text is still useful; requiring a summary
     * here would discard valid partial Javadocs.
     */
    private static void inheritMethodDocumentation(final TypeInfo type, final MethodInfo method, final Map<String, TypeInfo> byFqn,
            final Set<String> visiting) {
        if (!visiting.add(type.fqn)) {
            return;
        }
        for (final String superFqn : type.superTypes) {
            final TypeInfo superType = byFqn.get(superFqn);
            if (superType == null) {
                continue;
            }
            for (final MethodInfo candidate : superType.methods) {
                if (methodsMatchForDocInheritance(method, candidate)) {
                    applyInheritedMethodDoc(method, candidate);
                    break;
                }
            }
            if (!needsInheritedDocumentation(method)) {
                visiting.remove(type.fqn);
                return;
            }
            inheritMethodDocumentation(superType, method, byFqn, visiting);
            if (!needsInheritedDocumentation(method)) {
                visiting.remove(type.fqn);
                return;
            }
        }
        visiting.remove(type.fqn);
    }

    private static boolean methodsMatchForDocInheritance(final MethodInfo a, final MethodInfo b) {
        if (!a.name.equals(b.name) || a.params.size() != b.params.size()) {
            return false;
        }
        // Instance vs static should not inherit across each other.
        if (a.kind == null ? b.kind != null : !a.kind.equals(b.kind)) {
            return false;
        }
        for (int i = 0; i < a.params.size(); i++) {
            if (!paramTypesMatchForDocInheritance(a.params.get(i).type, b.params.get(i).type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean paramTypesMatchForDocInheritance(final String left, final String right) {
        final String a = normalizeParamTypeForMatch(left);
        final String b = normalizeParamTypeForMatch(right);
        if (a.equals(b)) {
            return true;
        }
        // Compare by simple name when one side is package-qualified.
        final String sa = simpleTypeName(a);
        final String sb = simpleTypeName(b);
        if (!sa.equals(sb)) {
            return false;
        }
        // If both have generics, compare generic tails loosely by simple names too.
        return stripPackageFromType(a).equals(stripPackageFromType(b));
    }

    private static String normalizeParamTypeForMatch(final String type) {
        if (type == null) {
            return "";
        }
        String t = type.trim();
        // Drop leading annotations: @NotNull List<String>
        while (t.startsWith("@")) {
            int i = 1;
            while (i < t.length() && (Character.isJavaIdentifierPart(t.charAt(i)) || t.charAt(i) == '.')) {
                i++;
            }
            if (i < t.length() && t.charAt(i) == '(') {
                int depth = 0;
                for (; i < t.length(); i++) {
                    final char c = t.charAt(i);
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        depth--;
                        if (depth == 0) {
                            i++;
                            break;
                        }
                    }
                }
            }
            t = t.substring(i).trim();
        }
        // final is not usually in tree types, but be defensive
        if (t.startsWith("final ")) {
            t = t.substring(6).trim();
        }
        return normalize(t);
    }

    private static String simpleTypeName(final String type) {
        if (type == null) {
            return "";
        }
        String t = type;
        final int generic = t.indexOf('<');
        if (generic >= 0) {
            t = t.substring(0, generic);
        }
        if (t.endsWith("...")) {
            t = t.substring(0, t.length() - 3);
        }
        while (t.endsWith("[]")) {
            t = t.substring(0, t.length() - 2);
        }
        return simpleName(t);
    }

    private static String stripPackageFromType(final String type) {
        if (type == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(type.length());
        int i = 0;
        while (i < type.length()) {
            final char c = type.charAt(i);
            if (Character.isJavaIdentifierStart(c)) {
                final int start = i;
                i++;
                while (i < type.length() && (Character.isJavaIdentifierPart(type.charAt(i)) || type.charAt(i) == '.')) {
                    i++;
                }
                final String ident = type.substring(start, i);
                sb.append(simpleName(ident));
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static void applyInheritedMethodDoc(final MethodInfo target, final MethodInfo source) {
        boolean filled = false;
        if (isBlank(target.javadocSummary) && !isBlank(source.javadocSummary)) {
            target.javadocSummary = source.javadocSummary;
            filled = true;
        }
        if (isBlank(target.returns) && !isBlank(source.returns)) {
            target.returns = source.returns;
            filled = true;
        }
        for (int i = 0; i < target.params.size() && i < source.params.size(); i++) {
            if (isBlank(target.params.get(i).javadoc) && !isBlank(source.params.get(i).javadoc)) {
                target.params.get(i).javadoc = source.params.get(i).javadoc;
                filled = true;
            }
        }
        if (target.throwsList != null && source.throwsList != null) {
            for (final ThrowInfo srcThrow : source.throwsList) {
                if (isBlank(srcThrow.condition)) {
                    continue;
                }
                boolean matched = false;
                for (final ThrowInfo dstThrow : target.throwsList) {
                    if (throwTypesMatch(dstThrow.type, srcThrow.type)) {
                        if (isBlank(dstThrow.condition)) {
                            dstThrow.condition = srcThrow.condition;
                            filled = true;
                        }
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    final ThrowInfo copy = new ThrowInfo();
                    copy.type = srcThrow.type;
                    copy.condition = srcThrow.condition;
                    target.throwsList.add(copy);
                    filled = true;
                }
            }
        }
        if ((target.examples == null || target.examples.isEmpty()) && source.examples != null && !source.examples.isEmpty()) {
            target.examples = new ArrayList<>(source.examples);
            filled = true;
        }
        if ((target.seeAlso == null || target.seeAlso.isEmpty()) && source.seeAlso != null && !source.seeAlso.isEmpty()) {
            target.seeAlso = new ArrayList<>(source.seeAlso);
            filled = true;
        }
        if ((target.contract == null || target.contract.isEmpty()) && source.contract != null && !source.contract.isEmpty()) {
            target.contract = new ArrayList<>(source.contract);
            filled = true;
        }
        if (isBlank(target.performance) && !isBlank(source.performance)) {
            target.performance = source.performance;
            filled = true;
        }
        if (filled) {
            target.inheritsDocumentation = true;
        }
    }

    private static boolean throwTypesMatch(final String a, final String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b) || simpleName(a).equals(simpleName(b));
    }

    private static DeprecatedInfo readDeprecated(final ModifiersTree modifiers, final DocInfo doc) {
        AnnotationTree deprecatedAnn = null;
        for (final AnnotationTree ann : modifiers.getAnnotations()) {
            final String annName = ann.getAnnotationType().toString();
            if ("Deprecated".equals(annName) || "java.lang.Deprecated".equals(annName)) {
                deprecatedAnn = ann;
                break;
            }
        }

        final boolean deprecatedTag = doc != null && !isBlank(doc.deprecatedMessage);
        if (deprecatedAnn == null && !deprecatedTag) {
            return null;
        }

        final DeprecatedInfo d = new DeprecatedInfo();
        if (deprecatedTag) {
            d.message = doc.deprecatedMessage;
        }
        if (deprecatedAnn != null) {
            for (final ExpressionTree arg : deprecatedAnn.getArguments()) {
                if (arg instanceof AssignmentTree assignmentTree) {
                    final String key = assignmentTree.getVariable().toString();
                    final String value = stripQuotes(normalize(assignmentTree.getExpression().toString()));
                    if ("since".equals(key)) {
                        d.since = value;
                    } else if ("forRemoval".equals(key)) {
                        d.forRemoval = Boolean.parseBoolean(value);
                    }
                }
            }
        }
        return d;
    }

    private static String inferNullability(final List<? extends AnnotationTree> annotations, final String typeText) {
        final String anns = annotations.stream().map(a -> a.getAnnotationType().toString().toLowerCase(Locale.ROOT)).collect(Collectors.joining(" "));
        final String txt = (anns + " " + String.valueOf(typeText).toLowerCase(Locale.ROOT)).trim();
        if (txt.contains("nullable") || txt.contains("checkfornull") || txt.contains("nullsafe")) {
            return "nullable";
        }
        if (txt.contains("nonnull") || txt.contains("notnull") || txt.contains("non_null")) {
            return "non-null";
        }
        return "unspecified";
    }

    private static LibraryInfo readLibraryInfo(final Path pomPath) {
        final LibraryInfo out = new LibraryInfo();
        if (!Files.exists(pomPath)) {
            return out;
        }
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            final Document doc;
            try (InputStream input = Files.newInputStream(pomPath)) {
                doc = factory.newDocumentBuilder().parse(input);
            }
            doc.getDocumentElement().normalize();

            final String name = readProjectElement(doc, "name");
            final String artifactId = readProjectElement(doc, "artifactId");
            final String version = readProjectElement(doc, "version");
            final String javaTarget = readProjectElement(doc, "maven.compiler.target");
            final String javaRelease = readProjectElement(doc, "maven.compiler.release");
            if (!isBlank(name)) {
                out.name = name.trim();
            } else if (!isBlank(artifactId)) {
                out.name = artifactId.trim();
            }
            if (!isBlank(version)) {
                out.version = version.trim();
            }
            if (!isBlank(javaTarget)) {
                out.javaTarget = javaTarget.trim();
            } else if (!isBlank(javaRelease)) {
                out.javaTarget = javaRelease.trim();
            }
        } catch (final Exception ignored) {
        }
        return out;
    }

    private static String readProjectElement(final Document doc, final String key) {
        final NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (key.equals(child.getNodeName())) {
                return child.getTextContent();
            }
            if ("properties".equals(child.getNodeName())) {
                final NodeList props = child.getChildNodes();
                for (int j = 0; j < props.getLength(); j++) {
                    final Node prop = props.item(j);
                    if (prop.getNodeType() == Node.ELEMENT_NODE && key.equals(prop.getNodeName())) {
                        return prop.getTextContent();
                    }
                }
            }
        }
        return null;
    }

    private static List<Path> listJavaFiles(final Path root) throws IOException {
        final List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(Path::toString));
        return files;
    }

    private static void createParentDirectories(final Path output) throws IOException {
        final Path parent = output.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String toMarkdown(final LibraryInfo library, final List<PackageInfoData> packages) {
        final StringBuilder sb = new StringBuilder(1 << 20);
        sb.append("# ").append(md(library.name)).append(" API Index (v").append(md(library.version)).append(")\n");
        sb.append("- Build: ").append(md(library.gitSha)).append('\n');
        sb.append("- Java: ").append(md(library.javaTarget)).append('\n');
        sb.append("- Generated: ").append(md(library.generatedAt)).append("\n\n");
        sb.append("## Packages\n");
        for (final PackageInfoData p : packages) {
            if (isBlank(p.summary)) {
                sb.append("- ").append(md(p.name)).append('\n');
            } else {
                sb.append("- ").append(md(p.name)).append(" — ").append(md(p.summary)).append('\n');
            }
        }
        sb.append('\n');

        for (final PackageInfoData p : packages) {
            sb.append("## ").append(md(p.name)).append('\n');
            for (final TypeInfo t : p.types) {
                sb.append("### ").append(capitalize(md(t.kind))).append(' ').append(md(t.name)).append(" (").append(md(t.fqn)).append(")\n");
                sb.append(isBlank(t.javadocSummary) ? "unspecified" : md(t.javadocSummary)).append("\n\n");
                if (!isBlank(t.since)) {
                    sb.append("**Since:** ").append(md(t.since)).append('\n');
                }
                sb.append("**Thread-safety:** ").append(md(t.threadSafety)).append('\n');
                sb.append("**Nullability:** ").append(md(t.nullability)).append('\n');
                if (!t.annotations.isEmpty()) {
                    sb.append("**Annotations:** ").append(md(annotationList(t.annotations))).append('\n');
                }
                if (!t.permittedSubtypes.isEmpty()) {
                    sb.append("**Permitted subtypes:** ").append(md(String.join(", ", t.permittedSubtypes))).append('\n');
                }
                appendNotesMarkdown(sb, "API note", t.apiNotes, "**", ":**");
                appendNotesMarkdown(sb, "Implementation note", t.implementationNotes, "**", ":**");
                sb.append('\n');

                if (!t.recordComponents.isEmpty()) {
                    sb.append("#### Record Components\n");
                    for (final RecordComponentInfo component : t.recordComponents) {
                        sb.append("- `").append(component.name).append("` (`").append(component.type).append("`)");
                        if (!isBlank(component.javadoc)) {
                            sb.append(" - ").append(md(component.javadoc));
                        }
                        if (!"unspecified".equals(component.nullability)) {
                            sb.append(" [").append(md(component.nullability)).append(']');
                        }
                        sb.append('\n');
                    }
                    sb.append('\n');
                }

                sb.append("#### Public Constructors\n");
                if (t.constructors.isEmpty()) {
                    sb.append("- (none)\n\n");
                } else {
                    for (final ConstructorInfo c : t.constructors) {
                        sb.append("- `").append(c.signature).append('`');
                        if (!c.annotations.isEmpty()) {
                            sb.append(" (").append(md(annotationList(c.annotations))).append(')');
                        }
                        if (!isBlank(c.javadocSummary)) {
                            sb.append(" — ").append(md(c.javadocSummary));
                        }
                        sb.append('\n');
                        appendNotesMarkdown(sb, "API note", c.apiNotes, "  - **", ":**");
                        appendNotesMarkdown(sb, "Implementation note", c.implementationNotes, "  - **", ":**");
                        if (!c.throwsList.isEmpty()) {
                            sb.append("  - **Throws:**\n");
                            for (final ThrowInfo thrown : c.throwsList) {
                                sb.append("    - `").append(thrown.type).append('`');
                                if (!isBlank(thrown.condition)) {
                                    sb.append(" — ").append(md(thrown.condition));
                                }
                                sb.append('\n');
                            }
                        }
                        for (final String example : c.examples) {
                            sb.append("  ```java\n");
                            for (final String line : example.split("\r?\n", -1)) {
                                sb.append("  ").append(line).append('\n');
                            }
                            sb.append("  ```\n");
                        }
                    }
                    sb.append('\n');
                }

                sb.append("#### Public Static Methods\n");
                appendMethodGroupsMarkdown(sb, t.methods, "static");
                sb.append('\n');

                sb.append("#### Public Instance Methods\n");
                appendMethodGroupsMarkdown(sb, t.methods, "instance");
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendMethodGroupsMarkdown(final StringBuilder sb, final List<MethodInfo> methods, final String kind) {
        final Map<String, List<MethodInfo>> groups = new LinkedHashMap<>();
        methods.stream()
                .filter(m -> kind.equals(m.kind))
                .sorted(Comparator.comparingInt(m -> m.order))
                .forEach(m -> groups.computeIfAbsent(m.name, k -> new ArrayList<>()).add(m));
        if (groups.isEmpty()) {
            sb.append("- (none)\n");
            return;
        }
        for (final Map.Entry<String, List<MethodInfo>> e : groups.entrySet()) {
            final List<String> returnTypes = e.getValue().stream().map(m -> m.returnType).distinct().toList();
            final String returnType = returnTypes.size() == 1 ? returnTypes.get(0) : "varies by overload";
            sb.append("##### ").append(md(e.getKey())).append("(...) -> ").append(md(returnType)).append('\n');
            for (final MethodInfo m : e.getValue()) {
                sb.append("- **Signature:** `").append(m.signature).append("`\n");
                if (!m.annotations.isEmpty()) {
                    sb.append("- **Annotations:** ").append(md(annotationList(m.annotations))).append('\n');
                }
                if (!isBlank(m.javadocSummary)) {
                    sb.append("- **Summary:** ").append(md(m.javadocSummary)).append('\n');
                }
                if (m.inheritsDocumentation) {
                    sb.append("- **Documentation:** inherits documentation from the overridden member\n");
                }
                appendNotesMarkdown(sb, "API note", m.apiNotes, "- **", ":**");
                appendNotesMarkdown(sb, "Implementation note", m.implementationNotes, "- **", ":**");
                if (!m.contract.isEmpty()) {
                    sb.append("- **Contract:**\n");
                    for (final String c : m.contract) {
                        sb.append("  - ").append(md(c)).append('\n');
                    }
                }
                sb.append("- **Parameters:**\n");
                if (m.params.isEmpty()) {
                    sb.append("  - (none)\n");
                } else {
                    for (final ParamInfo p : m.params) {
                        sb.append("  - `").append(p.name).append("` (`").append(p.type).append("`)");
                        if (!isBlank(p.javadoc)) {
                            sb.append(" — ").append(md(p.javadoc));
                        }
                        sb.append('\n');
                    }
                }
                if (!"void".equals(m.returnType)) {
                    sb.append("- **Returns:** ").append(isBlank(m.returns) ? "unspecified" : md(m.returns));
                    if ("nullable".equals(m.returnNullability)) {
                        sb.append(" (may return null)");
                    } else if ("non-null".equals(m.returnNullability)) {
                        sb.append(" (never returns null)");
                    }
                    sb.append('\n');
                }
                if (!m.throwsList.isEmpty()) {
                    sb.append("- **Throws:**\n");
                    for (final ThrowInfo t : m.throwsList) {
                        sb.append("  - `").append(t.type).append('`');
                        if (!isBlank(t.condition)) {
                            sb.append(" — ").append(md(t.condition));
                        }
                        sb.append('\n');
                    }
                }
                if (!isBlank(m.performance)) {
                    sb.append("- **Performance:** ").append(md(m.performance)).append('\n');
                }
                if (!m.examples.isEmpty()) {
                    sb.append("- **Examples:**\n");
                    for (final String example : m.examples) {
                        sb.append("  ```java\n");
                        for (final String line : example.split("\r?\n", -1)) {
                            sb.append("  ").append(line).append('\n');
                        }
                        sb.append("  ```\n");
                    }
                }
                if (!m.seeAlso.isEmpty()) {
                    sb.append("- **See also:** ").append(md(String.join(", ", m.seeAlso))).append('\n');
                }
            }
        }
    }

    private static String annotationList(final List<String> annotations) {
        return annotations.stream().map(a -> "@" + a).collect(Collectors.joining(", "));
    }

    private static void appendNotesMarkdown(final StringBuilder sb, final String label, final List<String> notes, final String prefix,
            final String labelSuffix) {
        for (final String note : notes) {
            sb.append(prefix).append(label).append(labelSuffix).append(' ').append(md(note)).append('\n');
        }
    }

    private static String toJson(final LibraryInfo library, final List<PackageInfoData> packages) {
        final Map<String, Object> root = new LinkedHashMap<>();
        final Map<String, Object> lib = new LinkedHashMap<>();
        lib.put("name", library.name);
        lib.put("version", library.version);
        lib.put("generated_at", library.generatedAt);
        lib.put("java_target", library.javaTarget);
        final Map<String, Object> source = new LinkedHashMap<>();
        source.put("git_sha", library.gitSha);
        lib.put("source", source);
        root.put("library", lib);

        final List<Object> packageArr = new ArrayList<>();
        for (final PackageInfoData p : packages) {
            final Map<String, Object> pkg = new LinkedHashMap<>();
            pkg.put("name", p.name);
            if (!isBlank(p.summary)) {
                pkg.put("summary", p.summary);
            }
            final List<Object> typeArr = new ArrayList<>();
            for (final TypeInfo t : p.types) {
                final Map<String, Object> type = new LinkedHashMap<>();
                type.put("fqn", t.fqn);
                type.put("name", t.name);
                type.put("kind", t.kind);
                type.put("modifiers", t.modifiers);
                type.put("annotations", t.annotations);
                if (!isBlank(t.since)) {
                    type.put("since", t.since);
                }
                if (t.deprecated != null) {
                    type.put("deprecated", deprecatedJson(t.deprecated));
                }
                if (!isBlank(t.javadocSummary)) {
                    type.put("javadoc_summary", t.javadocSummary);
                }
                type.put("thread_safety", t.threadSafety);
                type.put("nullability", t.nullability);
                type.put("permitted_subtypes", t.permittedSubtypes);
                type.put("type_params", t.typeParams.stream().map(ApiDocGenerator::typeParamJson).collect(Collectors.toList()));
                type.put("record_components", t.recordComponents.stream().map(ApiDocGenerator::recordComponentJson).collect(Collectors.toList()));
                type.put("constructors", t.constructors.stream().map(ApiDocGenerator::constructorJson).collect(Collectors.toList()));
                type.put("methods", t.methods.stream().map(ApiDocGenerator::methodJson).collect(Collectors.toList()));
                type.put("fields", t.fields.stream().map(ApiDocGenerator::fieldJson).collect(Collectors.toList()));
                type.put("api_notes", t.apiNotes);
                type.put("implementation_notes", t.implementationNotes);
                typeArr.add(type);
            }
            pkg.put("types", typeArr);
            packageArr.add(pkg);
        }
        root.put("packages", packageArr);
        return JsonWriter.write(root);
    }

    private static Map<String, Object> typeParamJson(final TypeParamInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", value.name);
        out.put("bounds", value.bounds);
        return out;
    }

    private static Map<String, Object> recordComponentJson(final RecordComponentInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", value.name);
        out.put("type", value.type);
        out.put("annotations", value.annotations);
        if (!isBlank(value.javadoc)) {
            out.put("javadoc", value.javadoc);
        }
        out.put("nullability", value.nullability);
        return out;
    }

    private static Map<String, Object> constructorJson(final ConstructorInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("signature", value.signature);
        out.put("annotations", value.annotations);
        if (!isBlank(value.javadocSummary)) {
            out.put("javadoc_summary", value.javadocSummary);
        }
        if (!isBlank(value.since)) {
            out.put("since", value.since);
        }
        if (value.deprecated != null) {
            out.put("deprecated", deprecatedJson(value.deprecated));
        }
        out.put("params", value.params.stream().map(ApiDocGenerator::paramJson).collect(Collectors.toList()));
        out.put("throws", value.throwsList.stream().map(ApiDocGenerator::throwJson).collect(Collectors.toList()));
        out.put("api_notes", value.apiNotes);
        out.put("implementation_notes", value.implementationNotes);
        out.put("examples", value.examples == null ? List.of() : value.examples);
        return out;
    }

    private static Map<String, Object> methodJson(final MethodInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", value.name);
        out.put("kind", value.kind);
        out.put("modifiers", value.modifiers);
        out.put("annotations", value.annotations);
        out.put("signature", value.signature);
        out.put("return_type", value.returnType);
        if (!"unspecified".equals(value.returnNullability)) {
            out.put("return_nullability", value.returnNullability);
        }
        out.put("type_params", value.typeParams.stream().map(ApiDocGenerator::typeParamJson).collect(Collectors.toList()));
        out.put("params", value.params.stream().map(ApiDocGenerator::paramJson).collect(Collectors.toList()));
        out.put("throws", value.throwsList.stream().map(ApiDocGenerator::throwJson).collect(Collectors.toList()));
        if (!isBlank(value.since)) {
            out.put("since", value.since);
        }
        if (value.deprecated != null) {
            out.put("deprecated", deprecatedJson(value.deprecated));
        }
        if (!isBlank(value.javadocSummary)) {
            out.put("javadoc_summary", value.javadocSummary);
        }
        if (!isBlank(value.returns)) {
            out.put("returns", value.returns);
        }
        if (value.inheritsDocumentation) {
            out.put("inherits_documentation", true);
        }
        out.put("contract", value.contract == null ? List.of() : value.contract);
        if (!isBlank(value.performance)) {
            out.put("performance", value.performance);
        }
        out.put("api_notes", value.apiNotes);
        out.put("implementation_notes", value.implementationNotes);
        out.put("examples", value.examples == null ? List.of() : value.examples);
        out.put("see_also", value.seeAlso == null ? List.of() : value.seeAlso);
        return out;
    }

    private static Map<String, Object> paramJson(final ParamInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", value.name);
        out.put("type", value.type);
        if (!isBlank(value.javadoc)) {
            out.put("javadoc", value.javadoc);
        }
        out.put("nullability", value.nullability);
        return out;
    }

    private static Map<String, Object> throwJson(final ThrowInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", value.type);
        if (!isBlank(value.condition)) {
            out.put("condition", value.condition);
        }
        return out;
    }

    private static Map<String, Object> fieldJson(final FieldInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", value.name);
        out.put("modifiers", value.modifiers);
        out.put("type", value.type);
        if (!isBlank(value.value)) {
            out.put("value", value.value);
        }
        if (!isBlank(value.javadocSummary)) {
            out.put("javadoc_summary", value.javadocSummary);
        }
        return out;
    }

    private static Map<String, Object> deprecatedJson(final DeprecatedInfo value) {
        final Map<String, Object> out = new LinkedHashMap<>();
        out.put("is_deprecated", true);
        if (!isBlank(value.message)) {
            out.put("message", value.message);
        }
        if (!isBlank(value.since)) {
            out.put("since", value.since);
        }
        out.put("for_removal", value.forRemoval);
        return out;
    }

    private static String mapTypeKind(final Tree.Kind kind) {
        return switch (kind) {
            case CLASS -> "class";
            case INTERFACE -> "interface";
            case ENUM -> "enum";
            case RECORD -> "record";
            case ANNOTATION_TYPE -> "annotation";
            default -> "class";
        };
    }

    private static String capitalize(final String value) {
        if (isBlank(value)) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String simpleName(final String value) {
        if (value == null) {
            return null;
        }
        final int idx = value.lastIndexOf('.');
        return idx < 0 ? value : value.substring(idx + 1);
    }

    private static String stripQuotes(final String value) {
        if (value == null) {
            return null;
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Re-escapes characters that would corrupt the output files back to their textual \\uXXXX
     * form: unpaired UTF-16 surrogates (not encodable as UTF-8) and raw control characters.
     * Javadoc in this code base intentionally documents such characters via unicode escapes
     * (e.g. Strings.indexOfAny examples with lone surrogates, '\\u0000' default values), and
     * javac translates those escapes to raw code units even inside comments. For the JSON
     * output the backslash is doubled because sanitization runs after JSON string escaping.
     */
    private static String sanitizeForUtf8(final String text, final boolean escapeBackslashForJson) {
        StringBuilder out = null;
        int replaced = 0;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            final boolean lone = (Character.isHighSurrogate(c) && (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1))))
                    || (Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(text.charAt(i - 1))));
            final boolean control = (c < 0x20 && c != '\n' && c != '\r' && c != '\t') || c == 0x7F;
            if ((lone || control) && out == null) {
                out = new StringBuilder(text.length() + 64);
                out.append(text, 0, i);
            }
            if (out != null) {
                if (lone || control) {
                    out.append(escapeBackslashForJson ? "\\\\" : "\\").append(String.format("u%04X", (int) c));
                    replaced++;
                } else {
                    out.append(c);
                }
            }
        }
        if (replaced > 0) {
            System.out.println("Re-escaped " + replaced + " unencodable character(s) as \\uXXXX text.");
        }
        return out == null ? text : out.toString();
    }

    private static String readFile(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            return "";
        }
    }

    private static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        final String v = value.replaceAll("\\s+", " ").trim();
        return v.isEmpty() ? null : v;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String md(final String text) {
        if (text == null) {
            return "";
        }
        final String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        final StringBuilder out = new StringBuilder(normalized.length() + 16);
        for (int i = 0; i < normalized.length(); i++) {
            final char c = normalized.charAt(i);
            if (c == '[' || c == ']' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static final class JsonWriter {
        private JsonWriter() {
        }

        static String write(final Object value) {
            final StringBuilder sb = new StringBuilder(1 << 20);
            writeValue(value, sb, 0);
            sb.append('\n');
            return sb.toString();
        }

        @SuppressWarnings("unchecked")
        private static void writeValue(final Object value, final StringBuilder sb, final int indent) {
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String s) {
                sb.append('"').append(escape(s)).append('"');
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map<?, ?> map) {
                final List<Map.Entry<String, Object>> entries = ((Map<String, Object>) map).entrySet().stream().toList();
                sb.append("{\n");
                for (int i = 0; i < entries.size(); i++) {
                    final Map.Entry<String, Object> entry = entries.get(i);
                    indent(sb, indent + 2);
                    sb.append('"').append(escape(entry.getKey())).append("\": ");
                    writeValue(entry.getValue(), sb, indent + 2);
                    if (i < entries.size() - 1) {
                        sb.append(',');
                    }
                    sb.append('\n');
                }
                indent(sb, indent);
                sb.append('}');
            } else if (value instanceof List<?> list) {
                sb.append("[");
                if (!list.isEmpty()) {
                    sb.append('\n');
                    for (int i = 0; i < list.size(); i++) {
                        indent(sb, indent + 2);
                        writeValue(list.get(i), sb, indent + 2);
                        if (i < list.size() - 1) {
                            sb.append(',');
                        }
                        sb.append('\n');
                    }
                    indent(sb, indent);
                }
                sb.append(']');
            } else {
                sb.append('"').append(escape(value.toString())).append('"');
            }
        }

        private static void indent(final StringBuilder sb, final int spaces) {
            sb.append(" ".repeat(Math.max(0, spaces)));
        }

        private static String escape(final String s) {
            final StringBuilder out = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                final char c = s.charAt(i);
                switch (c) {
                    case '\\' -> out.append("\\\\");
                    case '"' -> out.append("\\\"");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.toString();
        }
    }
}
