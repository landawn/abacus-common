package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;
import com.landawn.abacus.util.function.TriFunction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CodeGenerationUtilTest extends TestBase {

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

    @Test
    public void test_generatePropNameTableClass_singleParam() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String name = \"name\";"));
        assertTrue(code.contains("String email = \"email\";"));
        assertTrue(code.contains("String age = \"age\";"));
        assertTrue(code.contains("String address = \"address\";"));
        assertTrue(code.contains("Auto-generated class for property(field) name table"));
    }

    @Test
    public void test_generatePropNameTableClass_singleParam_withOrder() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(Order.class);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String orderId = \"orderId\";"));
        assertTrue(code.contains("String userId = \"userId\";"));
        assertTrue(code.contains("String productName = \"productName\";"));
        assertTrue(code.contains("String price = \"price\";"));
        assertTrue(code.contains("String quantity = \"quantity\";"));
    }

    @Test
    public void test_generatePropNameTableClass_withCustomClassName() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "Props");

        assertNotNull(code);
        assertTrue(code.contains("public interface Props"));
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String name = \"name\";"));
        assertFalse(code.contains("public interface x"));
    }

    @Test
    public void test_generatePropNameTableClass_prefixesKeywordProperty() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(KeywordProperty.class, "Props");

        assertTrue(code.contains("String _default = \"default\";"), code);
        assertFalse(code.contains("String default = \"default\";"));
    }

    @Test
    public void test_generatePropNameTableClass_withLowerCaseClassName() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "props");

        assertNotNull(code);
        assertTrue(code.contains("public interface props"));
        assertTrue(code.contains("// NOSONAR"));
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_noSrcDir() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "x", null);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_withEmptySrcDir() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "x", "");

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
    }

    @Test
    public void test_generatePropNameTableClass_singleParam_nullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(null);
        });
    }

    @Test
    public void test_generatePropNameTableClass_withEmptyClassName() {
        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(User.class, "");
        });
    }

    @Test
    public void test_generatePropNameTableClass_withNullClassName() {
        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(User.class, null);
        });
    }

    @Test
    public void test_generatePropNameTableClass_rejectsInvalidJavaIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "../Injected"));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClass(User.class, "class"));
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_nullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(null, "x", null);
        });
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_withSrcDir() throws IOException {
        final File tempDir = Files.createTempDirectory("test-codegen-single").toFile();
        tempDir.deleteOnExit();

        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "x", tempDir.getAbsolutePath());

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));

        IOUtil.deleteRecursivelyIfExists(tempDir);
    }

    @Test
    public void test_generatePropNameTableClasses_singleParam() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String name = \"name\";"));
        assertTrue(code.contains("String orderId = \"orderId\";"));
        assertTrue(code.contains("String productName = \"productName\";"));
        assertTrue(code.contains("Auto-generated class for property(field) name table"));
    }

    @Test
    public void test_generatePropNameTableClasses_singleParam_singleClass() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
    }

    @Test
    public void test_generatePropNameTableClasses_withClassName() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "PropNames");

        assertNotNull(code);
        assertTrue(code.contains("public interface PropNames"));
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String orderId = \"orderId\";"));
        assertFalse(code.contains("public interface s"));
    }

    @Test
    public void test_generatePropNameTableClasses_withLowerCaseClassName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "propnames");

        assertNotNull(code);
        assertTrue(code.contains("public interface propnames"));
        assertTrue(code.contains("// NOSONAR"));
    }

    @Test
    public void test_generatePropNameTableClasses_fourParams_basic() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "s", null, null);

        assertNotNull(code);
        assertTrue(code.contains("public interface s"));
        assertTrue(code.contains("String id = \"id\";"));
    }

    @Test
    public void test_generatePropNameTableClasses_fourParams_withPackageName() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "s", "com.test.generated", null);

        assertNotNull(code);
        assertTrue(code.contains("package com.test.generated;"));
        assertTrue(code.contains("public interface s"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_basic() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface s"));
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String orderId = \"orderId\";"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withPropNameConverter() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("email") ? null : propName)
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("String id = \"id\";"));
        assertTrue(code.contains("String name = \"name\";"));
        assertFalse(code.contains("String email"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withClassPropNameList() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateClassPropNameList(true).build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("import java.util.List;"));
        assertTrue(code.contains("userPropNameList"));
        assertTrue(code.contains("orderPropNameList"));
        assertTrue(code.contains("List<String>"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withLowerCaseUnderscore() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateSnakeCase(true).build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface sl"));
        assertTrue(code.contains("user_id") || code.contains("Property(field) name in lower case"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withUpperCaseUnderscore() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateScreamingSnakeCase(true).build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface su"));
        assertTrue(code.contains("Property(field) name in upper case"));
    }
    //     @Test
    //     public void test_generatePropNameTableClasses_config_withCustomFunctionClassName() {
    //         final List<Class<?>> classes = Arrays.asList(User.class);
    //         final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
    //                 .entityClasses(classes)
    //                 .className("s")
    //                 .generateFunctionPropName(true)
    //                 .functionClassName("funcs")
    //                 .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC))
    //                 .build();
    //
    //         final String code = CodeGenerationUtil.generatePropNameTableClasses(config);
    //
    //         assertNotNull(code);
    //         assertTrue(code.contains("public interface funcs"));
    //     }

    @Test
    public void test_generatePropNameTableClasses_config_withCustomLowerCaseClassName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateSnakeCase(true)
                .classNameForSnakeCase("lower")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface lower"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withCustomUpperCaseClassName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateScreamingSnakeCase(true)
                .classNameForScreamingSnakeCase("upper")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface upper"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withPackageName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .packageName("com.custom.generated")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("package com.custom.generated;"));
    }

    @Test
    public void test_generatePropNameTableClasses_externalInterfacePackageIsNotCorrupted() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .packageName("util")
                .extendedInterfaces(Arrays.asList(java.util.function.Supplier.class))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertTrue(code.contains("public interface s extends java.util.function.Supplier"), code);
        assertFalse(code.contains("extends java.function.Supplier"), code);
    }

    @Test
    public void test_generatePropNameTableClasses_config_filterInterface() {
        @Data
        class ConcreteClass {
            private String name;
        }

        final List<Class<?>> classes = Arrays.asList(User.class, Comparable.class, ConcreteClass.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("String id"));
        assertTrue(code.contains("String name"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_rejectsWhenFilteringRemovesEveryClass() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(Comparable.class)).className("s").build();

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(config));

        assertTrue(exception.getMessage().contains("after filtering"), exception.getMessage());
    }

    @Test
    public void test_generatePropNameTableClasses_nestedLowerCaseNamesReceiveNosonarIndependently() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("S")
                .generateSnakeCase(true)
                .classNameForSnakeCase("sl")
                .generateScreamingSnakeCase(true)
                .classNameForScreamingSnakeCase("su")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertTrue(code.contains("public interface sl { // NOSONAR"), code);
        assertTrue(code.contains("public interface su { // NOSONAR"), code);
    }

    @Test
    public void test_generatePropNameTableClasses_config_customPropNameConverterForLowerCase() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> propName.equals("id") ? "user_id_custom" : Strings.toSnakeCase(propName))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("user_id_custom"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_customPropNameConverterForUpperCase() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateScreamingSnakeCase(true)
                .propNameConverterForScreamingSnakeCase((cls, propName) -> propName.equals("id") ? "USER_ID_CUSTOM" : Strings.toScreamingSnakeCase(propName))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("USER_ID_CUSTOM"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_customPropNameConverterAppliedBeforeLowerCaseConverter() {
        final List<Class<?>> classes = Arrays.asList(Order.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("userId") ? "ownerId" : propName)
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> propName.equals("ownerId") ? "owner_id_custom" : Strings.toSnakeCase(propName))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("String ownerId = \"owner_id_custom\";"));
        assertFalse(code.contains("String ownerId = \"user_id\";"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_customPropNameConverterAppliedBeforeUpperCaseConverter() {
        final List<Class<?>> classes = Arrays.asList(Order.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("userId") ? "ownerId" : propName)
                .generateScreamingSnakeCase(true)
                .propNameConverterForScreamingSnakeCase(
                        (cls, propName) -> propName.equals("ownerId") ? "OWNER_ID_CUSTOM" : Strings.toScreamingSnakeCase(propName))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("String ownerId = \"OWNER_ID_CUSTOM\";"));
        assertFalse(code.contains("String ownerId = \"USER_ID\";"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_propNameConverterReturnsEmpty() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase((cls, propName) -> "")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface sl"));
    }

    @Test
    public void test_javaKeywordHandling() {
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        class KeywordClass {
            private String className;
            private String name;
        }

        final String code = CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(KeywordClass.class), "s");

        assertNotNull(code);
        assertTrue(code.contains("String className"));
    }

    @Test
    public void test_multipleClasses_propertyConflictHandling() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class, Product.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "s");

        assertNotNull(code);
        assertTrue(code.contains("String id"));
        assertTrue(code.contains("String orderId"));
        assertTrue(code.contains("String productId"));
        assertTrue(code.contains("String productName"));
    }

    @Test
    public void test_generatePropNameTableClasses_singleParam_emptyCollection() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList());
        });
    }

    @Test
    public void test_generatePropNameTableClasses_singleParam_nullCollection() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses((Collection<Class<?>>) null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_withNullClassName() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_fourParams_withPackageAndSrcDir() throws IOException {
        final List<Class<?>> classes = Arrays.asList(User.class);

        final File tempDir = Files.createTempDirectory("test-codegen").toFile();
        tempDir.deleteOnExit();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes, "s", "com.test", tempDir.getAbsolutePath());

        assertNotNull(code);
        assertTrue(code.contains("package com.test;"));

        final File packageDir = new File(tempDir, "com/test");
        tempGeneratedFile = new File(packageDir, "s.java");
        assertTrue(tempGeneratedFile.exists());

        IOUtil.deleteRecursivelyIfExists(tempDir);
    }

    @Test
    public void test_generatePropNameTableClasses_fourParams_nullCollection() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(null, "s", null, null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_fourParams_nullClassName() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), null, null, null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_nullConfig() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses((PropNameTableCodeConfig) null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_nullEntityClasses() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().className("s").build();

        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_emptyEntityClasses() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(Arrays.asList()).className("s").build();

        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_nullClassName() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(User.class)).build();

        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_rejectsInvalidGeneratedNames() {
        final PropNameTableCodeConfig invalidPackage = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("Props")
                .packageName("com.example/../../outside")
                .build();
        final PropNameTableCodeConfig invalidNestedClass = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("Props")
                .generateSnakeCase(true)
                .classNameForSnakeCase("snake-case")
                .build();

        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(invalidPackage));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(invalidNestedClass));
    }

    @Test
    public void test_generatePropNameTableClasses_config_rejectsInvalidConvertedPropertyName() {
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("Props")
                .propNameConverter((cls, propName) -> "invalid-name")
                .build();
        final PropNameTableCodeConfig invalidFunctionPrefix = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("Props")
                .generateFunctionPropName(true)
                .propFunctions(Map.of("invalid-prefix", CodeGenerationUtil.MIN_FUNC))
                .build();

        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(config));
        assertThrows(IllegalArgumentException.class, () -> CodeGenerationUtil.generatePropNameTableClasses(invalidFunctionPrefix));
    }

    @Test
    public void test_generatePropNameTableClasses_config_duplicateSimpleClassNames() {
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        class User {
            private String id;
        }

        final List<Class<?>> classes = Arrays.asList(CodeGenerationUtilTest.User.class, User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateClassPropNameList(true).build();

        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_keywordPropName_inClassPropNameList() {
        // Regression: a converted property name that is a Java keyword is declared with a leading
        // underscore (e.g. "String _default = \"default\";"). The generated per-class
        // List.of(...) must reference the same "_"-prefixed identifier, otherwise the generated
        // source would not compile.
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .propNameConverter((cls, propName) -> propName.equals("name") ? "default" : propName)
                .generateClassPropNameList(true)
                .generateSnakeCase(true)
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("String _default = \"default\";"));

        int fromIndex = 0;
        int listCount = 0;

        while (true) {
            final int listIdx = code.indexOf("userPropNameList = List.of(", fromIndex);

            if (listIdx < 0) {
                break;
            }

            final String listSegment = code.substring(listIdx, code.indexOf(");", listIdx));
            assertTrue(listSegment.contains("_default"), "List.of(...) should reference _default: " + listSegment);
            assertFalse(listSegment.contains("(default") || listSegment.contains(" default"),
                    "List.of(...) must not reference the bare keyword identifier: " + listSegment);

            listCount++;
            fromIndex = listIdx + 1;
        }

        assertEquals(2, listCount); // one list in the top-level interface, one in the snake_case interface
    }

    @Test
    public void test_MIN_FUNC_withComparable() {
        final String result = CodeGenerationUtil.MIN_FUNC.apply(User.class, Integer.class, "age");
        assertEquals("min(age)", result);
    }

    @Test
    public void test_MIN_FUNC_withPrimitiveComparable() {
        assertEquals("min(age)", CodeGenerationUtil.MIN_FUNC.apply(User.class, int.class, "age"));
    }

    @Test
    public void test_MIN_FUNC_withNonComparable() {
        final String result = CodeGenerationUtil.MIN_FUNC.apply(User.class, Object.class, "obj");
        assertEquals(null, result);
    }

    @Test
    public void test_MAX_FUNC_withComparable() {
        final String result = CodeGenerationUtil.MAX_FUNC.apply(User.class, String.class, "name");
        assertEquals("max(name)", result);
    }

    @Test
    public void test_MAX_FUNC_withPrimitiveComparable() {
        assertEquals("max(price)", CodeGenerationUtil.MAX_FUNC.apply(Order.class, double.class, "price"));
    }

    @Test
    public void test_MAX_FUNC_withNonComparable() {
        final String result = CodeGenerationUtil.MAX_FUNC.apply(User.class, Object.class, "obj");
        assertEquals(null, result);
    }

    @Test
    public void test_generatePropNameTableClasses_config_withFunctionProps() {
        final Map<String, TriFunction<Class<?>, Class<?>, String, String>> propFunctions = new LinkedHashMap<>();
        propFunctions.put("min", CodeGenerationUtil.MIN_FUNC);
        propFunctions.put("max", CodeGenerationUtil.MAX_FUNC);

        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(User.class))
                .className("s")
                .generateFunctionPropName(true)
                .functionClassName("funcs")
                .propFunctions(propFunctions)
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface funcs"));
        assertTrue(code.contains("String min_age = \"min(age)\";"));
        assertTrue(code.contains("String max_name = \"max(name)\";"));
    }

    // TODO: Remaining CodeGenerationUtil gaps are generator filtering/file-emission branches that depend on broader source-tree layouts than this isolated test fixture currently provides.
}
