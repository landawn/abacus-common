package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;
import com.landawn.abacus.util.function.TriFunction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CodeGenerationUtilTest extends TestBase {

    public interface HasOf {
        List<String> of(String... values);
    }

    public interface HasList {
        String List = "inherited";
    }

    public interface InheritedHasList extends HasList {
    }

    public static class CollidingKeywordProperty extends KeywordProperty {
        private String _default;

        public String get_default() {
            return _default;
        }

        public void set_default(final String value) {
            _default = value;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private Long id;
        private String name;
        private String email;
        private Integer age;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private Long orderId;
        private Long userId;
        private String productName;
        private Double price;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private Long productId;
        private String productName;
        private Double price;
        private String category;
    }

    public static class KeywordProperty {
        private String value;

        public String getDefault() {
            return value;
        }

        public void setDefault(final String value) {
            this.value = value;
        }
    }

    public static class Foo_Bar {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

    public static class FooBar {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    /** Entity whose own simple name equals {@link CodeGenerationUtil#X}, the default generated table name. */
    public static class x {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        /** Entity nested in a class named {@code x}. */
        public static class Nested {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(final String value) {
                this.value = value;
            }
        }
    }

    /** Entity with no bean properties at all: its generated table is an empty interface. */
    public static class NoProps {
        public String describe() {
            return "no props";
        }
    }

    /** Entity whose property name is not a valid Java identifier, so no field can be generated for it. */
    public static class DigitLeadingProperty {
        private String value;

        public String get2Foo() {
            return value;
        }

        public void set2Foo(final String value) {
            this.value = value;
        }
    }

    private File tempSourceFile;
    private File tempGeneratedFile;

    @AfterEach
    public void cleanup() throws IOException {
        if (tempSourceFile != null && tempSourceFile.exists()) {
            Files.deleteIfExists(tempSourceFile.toPath());
        }
        if (tempGeneratedFile != null && tempGeneratedFile.exists()) {
            Files.deleteIfExists(tempGeneratedFile.toPath());
        }
    }

    private static void assertCompiles(final String className, final String code) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK so generated source can be compiled");
        final Path compilationDir = Files.createTempDirectory("codegen-" + className);
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                    return code;
                }
            };
            final StringWriter compilerOutput = new StringWriter();
            final Boolean compiled = compiler
                    .getTask(compilerOutput, fileManager, null, Arrays.asList("-proc:none", "-d", compilationDir.toString()), null, Arrays.asList(source))
                    .call();
            assertTrue(Boolean.TRUE.equals(compiled), compilerOutput + System.lineSeparator() + code);
        } finally {
            IOUtil.deleteRecursivelyIfExists(compilationDir.toFile());
        }
    }

    @Test
    public void testGeneratePropNamesForUnnamedPackageEntity() throws Exception {
        final Path compilationDir = Files.createTempDirectory("codegen-unnamed-entity");
        final Path source = compilationDir.resolve("UnnamedEntity.java");
        try {
            Files.writeString(source, "public class UnnamedEntity { public String getValue() { return null; } public void setValue(String value) {} }");
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull(compiler);
            assertEquals(0, compiler.run(null, null, null, "-proc:none", "-d", compilationDir.toString(), source.toString()));
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[] { compilationDir.toUri().toURL() })) {
                final Class<?> entity = loader.loadClass("UnnamedEntity");
                for (final String outputPackage : new String[] { "generated", null, "" }) {
                    final String code = CodeGenerationUtil.generatePropNameTableClasses(new PropNameTableCodeConfig().setEntityClasses(List.of(entity))
                            .setClassName("Props").setPackageName(outputPackage));
                    assertTrue(code.contains("String value = \"value\";"));
                    assertEquals("generated".equals(outputPackage), code.contains("package generated;"));
                    assertCompiles("Props", code);
                }
            }
        } finally {
            IOUtil.deleteRecursivelyIfExists(compilationDir.toFile());
        }
    }

    @Test
    public void testGeneratePropNameTableClass() throws IOException {
        String user = CodeGenerationUtil.generatePropNameTableClass(User.class);
        assertTrue(user.contains("public interface x"));
        assertTrue(user.contains("String id = \"id\";"));
        assertTrue(user.contains("String name = \"name\";"));
        assertTrue(user.contains("String email = \"email\";"));
        assertTrue(user.contains("String age = \"age\";"));
        assertTrue(user.contains("String address = \"address\";"));
        assertTrue(user.contains("Auto-generated class for property(field) name table"));

        String order = CodeGenerationUtil.generatePropNameTableClass(Order.class);
        assertTrue(order.contains("String orderId = \"orderId\";"));
        assertTrue(order.contains("String userId = \"userId\";"));
        assertTrue(order.contains("String productName = \"productName\";"));
        assertTrue(order.contains("String price = \"price\";"));
        assertTrue(order.contains("String quantity = \"quantity\";"));

        String props = CodeGenerationUtil.generatePropNameTableClass(User.class, "Props");
        assertTrue(props.contains("public interface Props"));
        assertFalse(props.contains("public interface x"));

        String keyword = CodeGenerationUtil.generatePropNameTableClass(KeywordProperty.class, "Props");
        assertTrue(keyword.contains("String _default = \"default\";"), keyword);
        assertFalse(keyword.contains("String default = \"default\";"));

        String lower = CodeGenerationUtil.generatePropNameTableClass(User.class, "props");
        assertTrue(lower.contains("public interface props"));
        assertTrue(lower.contains("// NOSONAR"));

        assertTrue(CodeGenerationUtil.generatePropNameTableClass(User.class, "x", null).contains("public interface x"));
        assertTrue(CodeGenerationUtil.generatePropNameTableClass(User.class, "x", "").contains("String id = \"id\";"));

        File tempDir = Files.createTempDirectory("test-codegen-single").toFile();
        try {
            assertTrue(CodeGenerationUtil.generatePropNameTableClass(User.class, "x", tempDir.getAbsolutePath()).contains("public interface x"));
        } finally {
            IOUtil.deleteRecursivelyIfExists(tempDir);
        }
    }

    @Test
    public void testGeneratePropNameTableClass_EdgeCase() {
        assertTrue(Beans.getPropNameList(CollidingKeywordProperty.class).containsAll(Arrays.asList("default", "_default")));
        IllegalArgumentException collision = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(CollidingKeywordProperty.class, "Props"));
        assertTrue(collision.getMessage().contains("duplicate Java field name"), collision.getMessage());

        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(null));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, ""));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, null));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "../Injected"));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "class"));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(null, "x", null));
    }

    /**
     * The 1-arg overload delegates with {@code X = "x"}, so it throws everything the 2-arg overload throws: the
     * duplicate-field-name {@code IllegalArgumentException}, and - because {@code "x"} is a name like any other -
     * the name-collision one, whenever the entity or one of its enclosing classes is itself named {@code x}.
     */
    @Test
    public void testGeneratePropNameTableClass_oneArgThrowsEverythingTheTwoArgThrows() {
        assertEquals("x", CodeGenerationUtil.X);

        final IllegalArgumentException duplicateField = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(CollidingKeywordProperty.class));
        assertEquals("bean property name produced duplicate Java field name: _default", duplicateField.getMessage());

        assertEquals(List.of("2Foo"), Beans.getPropNameList(DigitLeadingProperty.class));
        final IllegalArgumentException invalidField = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(DigitLeadingProperty.class));
        assertEquals("bean property name produced an invalid Java field name: 2Foo", invalidField.getMessage());

        final IllegalArgumentException ownName = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(x.class));
        assertEquals("propNameTableClassName must differ from the simple name of the entity and of each of its enclosing classes: x",
                ownName.getMessage());

        final IllegalArgumentException enclosingName = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(x.Nested.class));
        assertEquals("propNameTableClassName must differ from the simple name of the entity and of each of its enclosing classes: x",
                enclosingName.getMessage());

        // only the default name collides: an explicit, non-colliding name generates both entities fine
        assertTrue(CodeGenerationUtil.generatePropNameTableClass(x.class, "Props").contains("String name = \"name\";"));
        assertTrue(CodeGenerationUtil.generatePropNameTableClass(x.Nested.class, "Props").contains("String value = \"value\";"));
    }

    /**
     * The {@code "String"} rejection is conservative, not a compile-failure prediction: a property-less entity
     * generates an EMPTY interface, and that one does compile when named {@code String}. A name repeated from an
     * enclosing class, by contrast, never compiles.
     */
    @Test
    public void testGeneratePropNameTableClass_stringNameIsRefusedConservatively() throws IOException {
        assertTrue(Beans.getPropNameList(NoProps.class).isEmpty());
        final String emptyTable = CodeGenerationUtil.generatePropNameTableClass(NoProps.class, "Zz");
        assertTrue(emptyTable.contains("public interface Zz {"), emptyTable);
        assertFalse(emptyTable.contains("String "), emptyTable);

        // the rewritten entity source this generator WOULD have produced for the name "String" compiles
        assertCompiles("NoProps", "public class NoProps {\n" + asTableNamed(emptyTable, "String") + "}\n");

        // ... and is refused all the same, as is a name repeated from the entity itself
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(NoProps.class, "String"));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(NoProps.class, "NoProps"));

        // with even one property the constants are declared with the BARE type name, which the member type shadows
        final String oneProp = CodeGenerationUtil.generatePropNameTableClass(FooBar.class, "Zz");
        assertTrue(oneProp.contains("        String name = \"name\";"), oneProp);
        assertDoesNotCompile("FooBarEntity", nestTableInEntity("FooBarEntity", asTableNamed(oneProp, "String")));

        // a repeated enclosing simple name never compiles, whatever the entity looks like
        assertDoesNotCompile("NoProps", "public class NoProps {\n" + asTableNamed(emptyTable, "NoProps") + "}\n");
        assertDoesNotCompile("Outer", "public class Outer { public static class Nested { public interface Outer {} } }\n");
    }

    /** Renames the generated interface, and its marker comments, to {@code newName}. */
    private static String asTableNamed(final String table, final String newName) {
        return table.replace("interface Zz", "interface " + newName).replace(":Zz>", ":" + newName + ">");
    }

    private static void assertDoesNotCompile(final String className, final String code) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK so generated source can be compiled");
        final Path compilationDir = Files.createTempDirectory("codegen-bad-" + className);
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            final JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
                    return code;
                }
            };
            final StringWriter compilerOutput = new StringWriter();
            final Boolean compiled = compiler
                    .getTask(compilerOutput, fileManager, null, Arrays.asList("-proc:none", "-d", compilationDir.toString()), null, Arrays.asList(source))
                    .call();
            assertFalse(Boolean.TRUE.equals(compiled), "expected a compile error but the source compiled: " + code);
        } finally {
            IOUtil.deleteRecursivelyIfExists(compilationDir.toFile());
        }
    }

    @Test
    public void testGeneratePropNameTableClasses() {
        List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        String multi = CodeGenerationUtil.generatePropNameTableClasses(classes);
        assertTrue(multi.contains("public interface x"));
        assertTrue(multi.contains("String id = \"id\";"));
        assertTrue(multi.contains("String name = \"name\";"));
        assertTrue(multi.contains("String orderId = \"orderId\";"));
        assertTrue(multi.contains("String productName = \"productName\";"));
        assertTrue(multi.contains("Auto-generated class for property(field) name table"));

        assertTrue(CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class)).contains("String id = \"id\";"));

        String named = CodeGenerationUtil.generatePropNameTableClasses(classes, "PropNames");
        assertTrue(named.contains("public interface PropNames"));
        assertFalse(named.contains("public interface s"));

        String lower = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), "propnames");
        assertTrue(lower.contains("public interface propnames"));
        assertTrue(lower.contains("// NOSONAR"));

        assertTrue(CodeGenerationUtil.generatePropNameTableClasses(classes, "s", null, null).contains("public interface s"));
        assertTrue(CodeGenerationUtil.generatePropNameTableClasses(classes, "s", "com.test.generated", null).contains("package com.test.generated;"));

        String conflict = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class, Order.class, Product.class), "s");
        assertTrue(conflict.contains("String id"));
        assertTrue(conflict.contains("String orderId"));
        assertTrue(conflict.contains("String productId"));
        assertTrue(conflict.contains("String productName"));
    }

    @Test
    public void testGeneratePropNameTableClasses_config() {
        List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        String basic = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder().entityClasses(classes).className("s").build());
        assertTrue(basic.contains("public interface s"));
        assertTrue(basic.contains("String id = \"id\";"));
        assertTrue(basic.contains("String orderId = \"orderId\";"));

        String converted = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("email") ? null : propName)
                .build());
        assertTrue(converted.contains("String id = \"id\";"));
        assertFalse(converted.contains("String email"));

        String lists = CodeGenerationUtil
                .generatePropNameTableClasses(PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateClassPropNameList(true).build());
        assertTrue(lists.contains("import java.util.List;"));
        assertTrue(lists.contains("userPropNameList"));
        assertTrue(lists.contains("orderPropNameList"));

        String snake = CodeGenerationUtil.generatePropNameTableClasses(
                PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).className("s").generateSnakeCase(true).build());
        assertTrue(snake.contains("public interface sl"));
        assertTrue(snake.contains("user_id") || snake.contains("Property(field) name in lower case"));

        String screaming = CodeGenerationUtil.generatePropNameTableClasses(
                PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).className("s").generateScreamingSnakeCase(true).build());
        assertTrue(screaming.contains("public interface su"));
        assertTrue(screaming.contains("Property(field) name in upper case"));

        String customLower = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateSnakeCase(true)
                .classNameForSnakeCase("lower")
                .build());
        assertTrue(customLower.contains("public interface lower"));

        String customUpper = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateScreamingSnakeCase(true)
                .classNameForScreamingSnakeCase("upper")
                .build());
        assertTrue(customUpper.contains("public interface upper"));

        String pkg = CodeGenerationUtil.generatePropNameTableClasses(
                PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).className("s").packageName("com.custom.generated").build());
        assertTrue(pkg.contains("package com.custom.generated;"));

        String external = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .packageName("util")
                .extendedInterfaces(Arrays.asList(java.util.function.Supplier.class))
                .build());
        assertTrue(external.contains("public interface s extends java.util.function.Supplier"), external);
        assertFalse(external.contains("extends java.function.Supplier"), external);
    }

    @Test
    public void testGeneratePropNameTableClasses_configConverters() {
        String lowerCustom = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> propName.equals("id") ? "user_id_custom" : Strings.toSnakeCase(propName))
                .build());
        assertTrue(lowerCustom.contains("user_id_custom"));

        String upperCustom = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateScreamingSnakeCase(true)
                .propNameConverterForScreamingSnakeCase((cls, propName) -> propName.equals("id") ? "USER_ID_CUSTOM" : Strings.toScreamingSnakeCase(propName))
                .build());
        assertTrue(upperCustom.contains("USER_ID_CUSTOM"));

        String beforeLower = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(Order.class))
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("userId") ? "ownerId" : propName)
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> propName.equals("ownerId") ? "owner_id_custom" : Strings.toSnakeCase(propName))
                .build());
        assertTrue(beforeLower.contains("String ownerId = \"owner_id_custom\";"));
        assertFalse(beforeLower.contains("String ownerId = \"user_id\";"));

        String beforeUpper = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(Order.class))
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("userId") ? "ownerId" : propName)
                .generateScreamingSnakeCase(true)
                .propNameConverterForScreamingSnakeCase(
                        (cls, propName) -> propName.equals("ownerId") ? "OWNER_ID_CUSTOM" : Strings.toScreamingSnakeCase(propName))
                .build());
        assertTrue(beforeUpper.contains("String ownerId = \"OWNER_ID_CUSTOM\";"));
        assertFalse(beforeUpper.contains("String ownerId = \"USER_ID\";"));

        String empty = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> "")
                .build());
        assertTrue(empty.contains("public interface sl"));

        String nosonar = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("S")
                .generateSnakeCase(true)
                .classNameForSnakeCase("sl")
                .generateScreamingSnakeCase(true)
                .classNameForScreamingSnakeCase("su")
                .build());
        assertTrue(nosonar.contains("public interface sl { // NOSONAR"), nosonar);
        assertTrue(nosonar.contains("public interface su { // NOSONAR"), nosonar);
    }

    @Test
    public void testGeneratePropNameTableClasses_filterAndKeywords() {
        @Data
        class ConcreteClass {
            private String name;
        }
        String filtered = CodeGenerationUtil.generatePropNameTableClasses(
                PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class, Comparable.class, ConcreteClass.class)).className("s").build());
        assertTrue(filtered.contains("String id"));
        assertTrue(filtered.contains("String name"));

        IllegalArgumentException allFiltered = assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil
                .generatePropNameTableClasses(PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(Comparable.class)).className("s").build()));
        assertTrue(allFiltered.getMessage().contains("after filtering"), allFiltered.getMessage());

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        class KeywordClass {
            private String className;
            private String name;
        }
        assertTrue(CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(KeywordClass.class), "s").contains("String className"));
    }

    @Test
    public void testGeneratePropNameTableClasses_escapesAndCompiles() throws IOException {
        Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions = new LinkedHashMap<>();
        propFunctions.put("expr", (cls, propClass, propName) -> "fn(\"" + propName + "\\path\nnext*/)");
        String escaped = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> "quoted\"value\\path\nnext*/")
                .generateFunctionPropName(true)
                .propFunctions(propFunctions)
                .build());
        assertTrue(escaped.contains("quoted\\\"value\\\\path\\nnext*/"), escaped);
        assertTrue(escaped.contains("fn(\\\"id\\\\path\\nnext*/)"), escaped);
        assertTrue(escaped.contains("*&#47;"), escaped);

        String unicode = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("GeneratedProps")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> "value\\u002a\\u002f")
                .build());
        assertTrue(unicode.contains("value&#92;u002a&#92;u002f"), unicode);
        assertCompiles("GeneratedProps", unicode);

        String subpackage = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("GeneratedProps")
                .packageName("com.landawn")
                .extendedInterfaces(Arrays.asList(TriFunction.class))
                .build());
        assertCompiles("GeneratedProps", subpackage);
    }

    @Test
    public void testGeneratePropNameTableClasses_typeAndFieldNameCollisions() throws IOException {
        for (final String outerName : new String[] { "List", "String", "java" }) {
            final PropNameTableCodeConfig config = new PropNameTableCodeConfig().setEntityClasses(List.of(User.class)).setClassName(outerName)
                    .setGenerateClassPropNameList(true).setPropNameConverter((cls, property) -> switch (property) {
                        case "id" -> "List";
                        case "name" -> "java";
                        case "email" -> "of";
                        default -> property;
                    });
            final String code = CodeGenerationUtil.generatePropNameTableClasses(config);
            assertTrue(code.contains("import static java.util.List.of;"), code);
            assertCompiles(outerName, code);
        }

        for (final String nestedName : new String[] { "List", "String" }) {
            for (int variant = 0; variant < 3; variant++) {
                final PropNameTableCodeConfig config = new PropNameTableCodeConfig().setEntityClasses(List.of(User.class)).setClassName("Props")
                        .setGenerateClassPropNameList(true);
                if (variant == 0) {
                    config.setGenerateSnakeCase(true).setClassNameForSnakeCase(nestedName);
                } else if (variant == 1) {
                    config.setGenerateScreamingSnakeCase(true).setClassNameForScreamingSnakeCase(nestedName);
                } else {
                    config.setGenerateFunctionPropName(true).setFunctionClassName(nestedName).setPropFunctions(Map.of("min", CodeGenerationUtil.MIN_FUNC));
                }
                assertCompiles("Props", CodeGenerationUtil.generatePropNameTableClasses(config));
            }
        }

        final PropNameTableCodeConfig inherited = new PropNameTableCodeConfig().setEntityClasses(List.of(User.class)).setClassName("Props")
                .setGenerateClassPropNameList(true).setExtendedInterfaces(List.of(HasOf.class));
        final String ordinary = CodeGenerationUtil.generatePropNameTableClasses(inherited);
        assertTrue(ordinary.contains(" = List.of("));
        assertFalse(ordinary.contains("import static"));
        assertCompiles("Props", ordinary);
        inherited.setExtendedInterfaces(List.of(InheritedHasList.class));
        final String inheritedField = CodeGenerationUtil.generatePropNameTableClasses(inherited);
        assertTrue(inheritedField.contains("import static java.util.List.of;"));
        assertCompiles("Props", inheritedField);
        inherited.setExtendedInterfaces(List.of(HasOf.class)).setClassName("List");
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(inherited));

        for (final String conflictingName : new String[] { "List", "String" }) {
            final PropNameTableCodeConfig impossible = new PropNameTableCodeConfig().setEntityClasses(List.of(User.class)).setClassName("java")
                    .setGenerateClassPropNameList(true).setGenerateSnakeCase(true).setClassNameForSnakeCase(conflictingName);
            assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(impossible));
        }
    }

    @Test
    public void testGeneratePropNameTableClasses_EdgeCase() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("s")
                        .extendedInterfaces(Arrays.asList(String.class))
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("s")
                        .extendedInterfaces(Arrays.asList(java.io.Serializable.class, java.io.Serializable.class))
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("s")
                        .generateSnakeCase(true)
                        .classNameForSnakeCase("caseNames")
                        .generateScreamingSnakeCase(true)
                        .classNameForScreamingSnakeCase("caseNames")
                        .build()));

        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList()));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses((Collection<Class<?>>) null));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), null));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses(null, "s", null, null));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), null, null, null));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses((PropNameTableCodeConfig) null));
        assertThrows(Exception.class, () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder().className("s").build()));
        assertThrows(Exception.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder().entityClasses(Arrays.asList()).className("s").build()));
        assertThrows(Exception.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).build()));

        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("Props")
                        .packageName("com.example/../../outside")
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("Props")
                        .generateSnakeCase(true)
                        .classNameForSnakeCase("snake-case")
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("Props")
                        .propNameConverter((cls, propName) -> "invalid-name")
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("Props")
                        .generateFunctionPropName(true)
                        .propFunctions(Map.of("invalid-prefix", CodeGenerationUtil.MIN_FUNC))
                        .build()));
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(User.class))
                        .className("Props")
                        .propNameConverter((cls, propName) -> propName.equals("id") ? "default" : propName.equals("name") ? "_default" : null)
                        .build()));

        File tempDir = Files.createTempDirectory("test-codegen").toFile();
        try {
            String code = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), "s", "com.test", tempDir.getAbsolutePath());
            assertTrue(code.contains("package com.test;"));
            tempGeneratedFile = new File(new File(tempDir, "com/test"), "s.java");
            assertTrue(tempGeneratedFile.exists());
        } finally {
            IOUtil.deleteRecursivelyIfExists(tempDir);
        }
    }

    @Test
    public void testGeneratePropNameTableClasses_classListAndKeywords() {
        String disambiguated = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(Foo_Bar.class, FooBar.class))
                .className("Props")
                .generateClassPropNameList(true)
                .build());
        assertTrue(disambiguated.contains("List<String> fooBarPropNameList ="), disambiguated);
        assertTrue(disambiguated.contains("List<String> _fooBarPropNameList ="), disambiguated);

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        class User {
            private String id;
        }
        assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                        .entityClasses(Arrays.asList(CodeGenerationUtilTest.User.class, User.class))
                        .className("s")
                        .generateClassPropNameList(true)
                        .build()));

        String code = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(CodeGenerationUtilTest.User.class))
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("name") ? "default" : propName)
                .generateClassPropNameList(true)
                .generateSnakeCase(true)
                .build());
        assertTrue(code.contains("String _default = \"default\";"));
        int fromIndex = 0;
        int listCount = 0;
        while (true) {
            int listIdx = code.indexOf("userPropNameList = List.of(", fromIndex);
            if (listIdx < 0) {
                break;
            }
            String listSegment = code.substring(listIdx, code.indexOf(");", listIdx));
            assertTrue(listSegment.contains("_default"), "List.of(...) should reference _default: " + listSegment);
            assertFalse(listSegment.contains("(default") || listSegment.contains(" default"),
                    "List.of(...) must not reference the bare keyword identifier: " + listSegment);
            listCount++;
            fromIndex = listIdx + 1;
        }
        assertEquals(2, listCount);
    }

    @Test
    public void testMinMaxFuncAndFunctionProps() {
        assertEquals("min(age)", CodeGenerationUtil.MIN_FUNC.apply(User.class, Integer.class, "age"));
        assertEquals("min(age)", CodeGenerationUtil.MIN_FUNC.apply(User.class, int.class, "age"));
        assertEquals(null, CodeGenerationUtil.MIN_FUNC.apply(User.class, Object.class, "obj"));
        assertEquals("max(name)", CodeGenerationUtil.MAX_FUNC.apply(User.class, String.class, "name"));
        assertEquals("max(price)", CodeGenerationUtil.MAX_FUNC.apply(Order.class, double.class, "price"));
        assertEquals(null, CodeGenerationUtil.MAX_FUNC.apply(User.class, Object.class, "obj"));

        Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions = new LinkedHashMap<>();
        propFunctions.put("min", CodeGenerationUtil.MIN_FUNC);
        propFunctions.put("max", CodeGenerationUtil.MAX_FUNC);
        String code = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateFunctionPropName(true)
                .functionClassName("funcs")
                .propFunctions(propFunctions)
                .build());
        assertTrue(code.contains("public interface funcs"));
        assertTrue(code.contains("String min_age = \"min(age)\";"));
        assertTrue(code.contains("String max_name = \"max(name)\";"));
    }

    /**
     * The five JLS restricted identifiers ({@code permits}, {@code record}, {@code sealed}, {@code var},
     * {@code yield}) are legal identifiers but cannot name a type, so every generated class/interface name
     * must reject them while a generated field of the same name stays legal.
     */
    @Test
    public void testRestrictedIdentifiersRejectedAsGeneratedClassNames() {
        for (final String restricted : Arrays.asList("record", "var", "sealed", "permits", "yield")) {
            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClasses(
                            PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).className(restricted).build()),
                    "className must reject the restricted identifier: " + restricted);

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClass(User.class, restricted, null),
                    "propNameTableClassName must reject the restricted identifier: " + restricted);

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                            .entityClasses(Arrays.asList(User.class))
                            .className("Props")
                            .generateSnakeCase(true)
                            .classNameForSnakeCase(restricted)
                            .build()),
                    "classNameForSnakeCase must reject the restricted identifier: " + restricted);

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                            .entityClasses(Arrays.asList(User.class))
                            .className("Props")
                            .generateScreamingSnakeCase(true)
                            .classNameForScreamingSnakeCase(restricted)
                            .build()),
                    "classNameForScreamingSnakeCase must reject the restricted identifier: " + restricted);

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                            .entityClasses(Arrays.asList(User.class))
                            .className("Props")
                            .generateFunctionPropName(true)
                            .functionClassName(restricted)
                            .propFunctions(Map.of("min", CodeGenerationUtil.MIN_FUNC))
                            .build()),
                    "functionClassName must reject the restricted identifier: " + restricted);
        }

        // ... but a generated FIELD may still be named after a restricted identifier: they are excluded from
        // TypeIdentifier only, so `String record = "record";` compiles.
        final String code = CodeGenerationUtil.generatePropNameTableClasses(PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("Props")
                .propNameConverter((cls, propName) -> propName.equals("name") ? "record" : propName)
                .build());
        assertTrue(code.contains("String record = \"record\";"), code);
    }

    @Test
    public void testGeneratePropNameTableClass_typeAndFieldNameCollisions() throws IOException {
        for (final String tableName : new String[] { "List", "java" }) {
            final String table = CodeGenerationUtil.generatePropNameTableClass(FooBar.class, tableName);
            assertTrue(table.contains("public interface " + tableName + " {"), table);
            assertCompiles("FooBarEntity", nestTableInEntity("FooBarEntity", table));
        }

        final IllegalArgumentException shadowing = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(FooBar.class, "String"));
        assertTrue(shadowing.getMessage().contains("must not be String"), shadowing.getMessage());
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(FooBar.class, "String", null));

        final IllegalArgumentException ownName = assertThrows(IllegalArgumentException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "User"));
        assertTrue(ownName.getMessage().contains("must differ from the simple name"), ownName.getMessage());
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "CodeGenerationUtilTest"));

        assertTrue(CodeGenerationUtil.generatePropNameTableClass(User.class, "x", "").contains("String id = \"id\";"));
        assertCompiles("UserEntity", nestTableInEntity("UserEntity", CodeGenerationUtil.generatePropNameTableClass(User.class, "x")));
    }

    @Test
    public void testGeneratePropNameTableClass_rejectedNameLeavesEntitySourceUntouched() throws IOException {
        final File tempDir = Files.createTempDirectory("test-codegen-reject").toFile();
        try {
            final Path packageDir = tempDir.toPath().resolve(ClassUtil.getPackageName(User.class).replace('.', '/'));
            Files.createDirectories(packageDir);
            final Path entitySource = packageDir.resolve(ClassUtil.getSimpleClassName(User.class) + ".java");
            final String original = "package " + ClassUtil.getPackageName(User.class) + ";\n" //
                    + "public class User {\n" //
                    + "    private String name;\n" //
                    + "    public String describe() { return \"user:\" + name; }\n" //
                    + "}\n";
            Files.writeString(entitySource, original);

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "String", tempDir.getAbsolutePath()));
            assertEquals(original, Files.readString(entitySource));

            assertThrows(IllegalArgumentException.class,
                    () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "User", tempDir.getAbsolutePath()));
            assertEquals(original, Files.readString(entitySource));

            assertTrue(CodeGenerationUtil.generatePropNameTableClass(User.class, "Props", tempDir.getAbsolutePath()).contains("public interface Props"));
            assertTrue(Files.readString(entitySource).contains("public interface Props"));
        } finally {
            IOUtil.deleteRecursivelyIfExists(tempDir);
        }
    }

    private static String nestTableInEntity(final String entityName, final String table) {
        return "public class " + entityName + " {\n" //
                + "    private String name;\n" //
                + "    public String getName() { return name; }\n" //
                + "    public void setName(final String value) { name = value; }\n" //
                + "    public String describe() { return \"entity:\" + name; }\n" //
                + table //
                + "}\n";
    }
}
