package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ReflectASMModuleAccessReview20261010Test extends TestBase {
    @TempDir
    static Path temporaryDirectory;

    private static Path modules;

    @BeforeAll
    static void compileNamedModule() throws Exception {
        final Path sources = temporaryDirectory.resolve("src/fixture.access");
        modules = temporaryDirectory.resolve("modules");
        writeSource(sources, "module-info.java", """
                module fixture.access {
                    exports fixture.api;
                    opens fixture.api;
                    exports fixture.qualified to java.logging;
                    opens fixture.opened;
                }
                """);
        writeSource(sources, "fixture/hidden/HiddenTypes.java", """
                package fixture.hidden;
                public final class HiddenTypes {
                    public enum Color { RED, GREEN }
                    public static final class Scalar {
                        public final String value = "scalar";
                    }
                    public static class Parent {
                        public String inheritedField = "parent";
                        public String inheritedMethod() { return inheritedField; }
                    }
                    public static final class Owner {
                        public String value = "owner";
                        public String value() { return value; }
                    }
                }
                """);
        writeSource(sources, "fixture/qualified/QualifiedTypes.java", """
                package fixture.qualified;
                public final class QualifiedTypes {
                    public enum Color { RED, GREEN }
                }
                """);
        writeSource(sources, "fixture/opened/OpenedTypes.java", """
                package fixture.opened;
                public final class OpenedTypes {
                    public enum Color { RED, GREEN }
                }
                """);
        writeSource(sources, "fixture/api/Fixtures.java", """
                package fixture.api;
                import com.landawn.abacus.TestBase;
                import com.landawn.abacus.annotation.AccessFieldByMethod;
                import com.landawn.abacus.annotation.JsonXmlField;
                import fixture.hidden.HiddenTypes;
                import fixture.qualified.QualifiedTypes;
                import fixture.opened.OpenedTypes;
                public final class Fixtures {
                    public static Object target() { return new Target(); }
                    public static Object owner() { return new HiddenTypes.Owner(); }
                    public static Object child() { return new Child(); }
                    public static Object color() { return HiddenTypes.Color.GREEN; }
                    public static Object scalar() { return new HiddenTypes.Scalar(); }
                    public static Object colors() { return new HiddenTypes.Color[][] {{ HiddenTypes.Color.GREEN }}; }
                    public static Object scalars() { return new HiddenTypes.Scalar[][] {{ new HiddenTypes.Scalar() }}; }
                    public static Object qualifiedColor() { return QualifiedTypes.Color.GREEN; }
                    public static Object qualifiedColors() { return new QualifiedTypes.Color[][] {{ QualifiedTypes.Color.GREEN }}; }
                    public static Object openedColor() { return OpenedTypes.Color.GREEN; }
                    public static void openHiddenTo(Module other) {
                        Fixtures.class.getModule().addOpens("fixture.hidden", other);
                    }
                    public static final class Child extends HiddenTypes.Parent { }
                    public static final class Target {
                        public HiddenTypes.Color color;
                        public HiddenTypes.Scalar scalar;
                        public HiddenTypes.Color[][] colors;
                        public HiddenTypes.Scalar[][] scalars;
                        public QualifiedTypes.Color qualifiedColor;
                        public QualifiedTypes.Color[][] qualifiedColors;
                        public int calls;
                        public final IllegalAccessError failure = new IllegalAccessError("from target");
                        public Object color(HiddenTypes.Color value) { return value; }
                        public Object scalar(HiddenTypes.Scalar value) { return value; }
                        public Object colors(HiddenTypes.Color[][] value) { return value; }
                        public Object scalars(HiddenTypes.Scalar[][] value) { return value; }
                        public Object qualifiedColor(QualifiedTypes.Color value) { return value; }
                        public Object qualifiedColors(QualifiedTypes.Color[][] value) { return value; }
                        public Object openedColor(OpenedTypes.Color value) { return value; }
                        public int primitiveArrays(int[][] values) { return values.length; }
                        public void fail() { calls++; throw failure; }
                        public void failWithHiddenType(HiddenTypes.Color value) { calls++; throw failure; }
                    }
                    @AccessFieldByMethod
                    public static final class ColorBean {
                        @JsonXmlField(name = "shade")
                        private HiddenTypes.Color color;
                        @JsonXmlField(ignore = true)
                        private int sets;
                        @JsonXmlField(ignore = true)
                        private int gets;
                        public HiddenTypes.Color getColor() { gets++; return color; }
                        public void setColor(HiddenTypes.Color value) {
                            sets++;
                            color = value == HiddenTypes.Color.RED ? HiddenTypes.Color.GREEN : value;
                        }
                        public int setterCalls() { return sets; }
                        public int getterCalls() { return gets; }
                    }
                    @AccessFieldByMethod
                    public static final class ArrayBean {
                        @JsonXmlField(name = "shades")
                        private HiddenTypes.Color[][] colors;
                        @JsonXmlField(ignore = true)
                        private int sets;
                        @JsonXmlField(ignore = true)
                        private int gets;
                        public HiddenTypes.Color[][] getColors() { gets++; return colors; }
                        public void setColors(HiddenTypes.Color[][] value) { sets++; colors = value; }
                        public int setterCalls() { return sets; }
                        public int getterCalls() { return gets; }
                    }
                    public static final class FieldBean {
                        public HiddenTypes.Color color;
                        public HiddenTypes.Color[][] colors;
                    }
                }
                """);

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "The named-module regression fixture requires a JDK");
        final List<String> arguments = new ArrayList<>(List.of("--release", "21", "--module-source-path", sources.getParent().toString(), "--add-reads",
                "fixture.access=ALL-UNNAMED", "-classpath", System.getProperty("java.class.path"), "-d", modules.toString()));
        try (var files = Files.walk(sources)) {
            files.filter(path -> path.toString().endsWith(".java")).sorted().forEach(path -> arguments.add(path.toString()));
        }
        final ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        assertEquals(0, compiler.run(null, diagnostics, diagnostics, arguments.toArray(String[]::new)), diagnostics.toString(StandardCharsets.UTF_8));
    }

    @Test
    void methodsCheckOwnersDeclaringClassesAndScalarOrArrayParameterTypes() throws Exception {
        runProbe("methods", false);
    }

    @Test
    void fieldsUseReflectionForModuleRestrictionsWithoutLinkageWarnings() throws Exception {
        runProbe("fields", false);
    }

    @Test
    void jsonFallbackPreservesAnnotatedGettersAndSetters() throws Exception {
        runProbe("json", false);
    }

    @Test
    void unconditionalOpensRemainAsmAccessibleAtRuntime() throws Exception {
        runProbe("opened", false);
    }

    @Test
    void allUnnamedExportsKeepWorkingWithConservativeFallback() throws Exception {
        runProbe("methods", true);
    }

    private static void writeSource(final Path root, final String relativePath, final String source) throws Exception {
        final Path file = root.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, source, StandardCharsets.UTF_8);
    }

    private static void runProbe(final String mode, final boolean exportToAllUnnamed) throws Exception {
        final List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "--module-path",
                modules.toString(), "--add-modules", "fixture.access,java.logging", "--add-reads", "fixture.access=ALL-UNNAMED"));
        if (exportToAllUnnamed) {
            command.addAll(List.of("--add-exports", "fixture.access/fixture.qualified=ALL-UNNAMED"));
        }
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), ModuleProbe.class.getName(), mode));

        // A file avoids filling a pipe if a regression emits many linkage diagnostics before the fork exits.
        final Path outputFile = Files.createTempFile(temporaryDirectory, "module-access-", ".log");
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(outputFile.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Module access probe timed out: " + mode);
            final String output = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("module-access-ok:" + mode), output);
            assertTrue(!output.contains("failed to link"), output);
        } finally {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    public static final class ModuleProbe {
        public static void main(final String[] arguments) throws Exception {
            final Class<?> fixtures = Class.forName("fixture.api.Fixtures");
            check(fixtures.getModule().isNamed(), "fixture must be a named module");
            // The application can reflect on these members, but this qualified opening does not grant the
            // generated accessor's separate unnamed module permission to access their declaring classes.
            fixtures.getMethod("openHiddenTo", Module.class).invoke(null, Reflection.class.getModule());

            switch (arguments[0]) {
                case "methods" -> checkMethods();
                case "fields" -> checkFields();
                case "json" -> checkJson();
                case "opened" -> checkUnconditionalOpens();
                default -> throw new AssertionError("Unknown probe: " + arguments[0]);
            }
            System.out.println("module-access-ok:" + arguments[0]);
        }

        private static Object fixture(final String name) throws Exception {
            return Class.forName("fixture.api.Fixtures").getMethod(name).invoke(null);
        }

        private static void checkMethods() throws Exception {
            final Object target = fixture("target");
            for (final String name : List.of("color", "scalar", "colors", "scalars", "qualifiedColor", "qualifiedColors")) {
                final Object value = fixture(name);
                check(!ReflectASM.on(target).canInvoke(name, value), "inaccessible parameter accepted by ASM: " + name);
                same(value, Reflection.on(target).invoke(name, value));
                Reflection.on(target).call(name, value);
            }

            final Object owner = fixture("owner");
            check(!owner.getClass().getModule().isExported(owner.getClass().getPackageName()), "qualified opening must not become unconditional");
            check(!ReflectASM.on(owner).canInvoke("value"), "inaccessible owner accepted by ASM");
            equal("owner", Reflection.on(owner).invoke("value"));
            final Object child = fixture("child");
            check(!ReflectASM.on(child).canInvoke("inheritedMethod"), "inaccessible declaring class accepted by ASM");
            equal("parent", Reflection.on(child).invoke("inheritedMethod"));

            final int[][] primitiveArrays = new int[2][1];
            check(ReflectASM.on(target).canInvoke("primitiveArrays", (Object) primitiveArrays), "primitive array rejected by ASM");
            equal(2, Reflection.on(target).invoke("primitiveArrays", (Object) primitiveArrays));
            checkTargetErrorIsNotRetried(target, "fail");
            checkTargetErrorIsNotRetried(target, "failWithHiddenType", fixture("color"));
        }

        private static void checkTargetErrorIsNotRetried(final Object target, final String name, final Object... arguments) {
            final int before = (Integer) Reflection.on(target).get("calls");
            final IllegalAccessError expected = Reflection.on(target).get("failure");
            try {
                Reflection.on(target).invoke(name, arguments);
                throw new AssertionError("Expected target failure from " + name);
            } catch (final IllegalAccessError actual) {
                same(expected, actual);
            } catch (final RuntimeException actual) {
                // The reflective path wraps a target Error; the ASM path above propagates it directly.
                // Both must preserve the original failure and execute the target exactly once.
                same(expected, actual.getCause());
            }
            equal(before + 1, Reflection.on(target).get("calls"));
        }

        private static void checkFields() throws Exception {
            final Object target = fixture("target");
            for (final String name : List.of("color", "scalar", "colors", "scalars", "qualifiedColor", "qualifiedColors")) {
                final Object value = fixture(name);
                Reflection.on(target).set(name, value);
                same(value, Reflection.on(target).get(name));
                check(!Reflection.asmUnreachableFields.get(target.getClass()).contains(name),
                        "expected module restriction recorded as linkage failure: " + name);
            }
            for (final String name : List.of("owner", "child")) {
                final Object value = fixture(name);
                final String field = name.equals("owner") ? "value" : "inheritedField";
                Reflection.on(value).set(field, "updated");
                equal("updated", Reflection.on(value).get(field));
                check(!Reflection.asmUnreachableFields.get(value.getClass()).contains(field), "inaccessible field owner/declaring class reached ASM");
            }
        }

        private static void checkJson() throws Exception {
            final Object colorBean = N.fromJson("{\"shade\":\"RED\"}", Class.forName("fixture.api.Fixtures$ColorBean"));
            equal(1, Reflection.on(colorBean).invoke("setterCalls"));
            equal(Map.of("shade", "GREEN"), N.fromJson(N.toJson(colorBean), Map.class));
            equal(1, Reflection.on(colorBean).invoke("getterCalls"));

            final Object arrayBean = N.fromJson("{\"shades\":[[\"RED\"],[\"GREEN\"]]}", Class.forName("fixture.api.Fixtures$ArrayBean"));
            equal(1, Reflection.on(arrayBean).invoke("setterCalls"));
            equal(Map.of("shades", List.of(List.of("RED"), List.of("GREEN"))), N.fromJson(N.toJson(arrayBean), Map.class));
            equal(1, Reflection.on(arrayBean).invoke("getterCalls"));

            final Object fieldBean = N.fromJson("{\"color\":\"GREEN\",\"colors\":[[\"RED\"]]}", Class.forName("fixture.api.Fixtures$FieldBean"));
            equal(Map.of("color", "GREEN", "colors", List.of(List.of("RED"))), N.fromJson(N.toJson(fieldBean), Map.class));
        }

        private static void checkUnconditionalOpens() throws Exception {
            final Object target = fixture("target");
            final Object value = fixture("openedColor");
            final Module module = value.getClass().getModule();
            final String packageName = value.getClass().getPackageName();
            check(module.isOpen(packageName) && module.isExported(packageName), "unconditional opens must be exported at runtime");
            check(ReflectASM.on(target).canInvoke("openedColor", value), "runtime-accessible opened package rejected by ASM");
            same(value, Reflection.on(target).invoke("openedColor", value));
        }

        private static void check(final boolean condition, final String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        private static void same(final Object expected, final Object actual) {
            check(expected == actual, "Expected the same instance: " + expected + ", actual: " + actual);
        }

        private static void equal(final Object expected, final Object actual) {
            check(Objects.equals(expected, actual), "Expected: " + expected + ", actual: " + actual);
        }
    }
}
