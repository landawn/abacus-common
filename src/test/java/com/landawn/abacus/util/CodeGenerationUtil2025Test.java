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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class CodeGenerationUtil2025Test extends TestBase {

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
    public void test_generatePropNameTableClass_singleParam_nullClass() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(null);
        });
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
    public void test_generatePropNameTableClass_withLowerCaseClassName() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "props");

        assertNotNull(code);
        assertTrue(code.contains("public interface props"));
        assertTrue(code.contains("// NOSONAR"));
    }

    @Test
    public void test_generatePropNameTableClass_withEmptyClassName() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(User.class, "");
        });
    }

    @Test
    public void test_generatePropNameTableClass_withNullClassName() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(User.class, null);
        });
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_noSrcDir() {
        final String code = CodeGenerationUtil.generatePropNameTableClass(User.class, "x", null);

        assertNotNull(code);
        assertTrue(code.contains("public interface x"));
        assertTrue(code.contains("String id = \"id\";"));
    }

    @Test
    public void test_generatePropNameTableClass_threeParams_nullClass() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClass(null, "x", null);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_singleParam() {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final String code = CodeGenerationUtil.generatePropNameTableClasses(classes);

        assertNotNull(code);
        assertTrue(code.contains("public interface s"));
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
        assertTrue(code.contains("public interface s"));
        assertTrue(code.contains("String id = \"id\";"));
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
    public void test_generatePropNameTableClasses_withNullClassName() {
        assertThrows(Exception.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(Arrays.asList(User.class), null);
        });
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
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateSnakeCase(true)
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface sl"));
        assertTrue(code.contains("user_id") || code.contains("Property(field) name in lower case"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withUpperCaseUnderscore() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateScreamingSnakeCase(true)
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface su"));
        assertTrue(code.contains("Property(field) name in upper case"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withFunctionPropName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateFunctionPropName(true)
                .propFunctions(CommonUtil.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface sf"));
        assertTrue(code.contains("min_") || code.contains("max_"));
        assertTrue(code.contains("Function property(field) name"));
    }

    @Test
    public void test_generatePropNameTableClasses_config_withCustomFunctionClassName() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateFunctionPropName(true)
                .functionClassName("funcs")
                .propFunctions(CommonUtil.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("public interface funcs"));
    }

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
                .packageName("com.custom.package")
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("package com.custom.package;"));
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
    public void test_generatePropNameTableClasses_config_duplicateSimpleClassNames() {
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        class User {
            private String id;
        }

        final List<Class<?>> classes = Arrays.asList(CodeGenerationUtil2025Test.User.class, User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("s").generateClassPropNameList(true).build();

        assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    @Test
    public void test_generatePropNameTableClasses_config_withAllFeatures() throws IOException {
        final List<Class<?>> classes = Arrays.asList(User.class, Order.class);
        final File tempDir = Files.createTempDirectory("test-codegen").toFile();
        tempDir.deleteOnExit();

        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .packageName("com.test.all")
                .srcDir(tempDir.getAbsolutePath())
                .generateClassPropNameList(true)
                .generateSnakeCase(true)
                .generateScreamingSnakeCase(true)
                .generateFunctionPropName(true)
                .propFunctions(CommonUtil.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("package com.test.all;"));
        assertTrue(code.contains("public interface s"));
        assertTrue(code.contains("public interface sl"));
        assertTrue(code.contains("public interface su"));
        assertTrue(code.contains("public interface sf"));
        assertTrue(code.contains("userPropNameList"));
        assertTrue(code.contains("orderPropNameList"));

        final File packageDir = new File(tempDir, "com/test/all");
        tempGeneratedFile = new File(packageDir, "s.java");
        assertTrue(tempGeneratedFile.exists());

        IOUtil.deleteRecursivelyIfExists(tempDir);
    }

    @Test
    public void test_generatePropNameTableClasses_config_customPropNameConverterForLowerCase() {
        final List<Class<?>> classes = Arrays.asList(User.class);
        final PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("s")
                .generateSnakeCase(true)
                .propNameConverterForSnakeCase(
                        (cls, propName) -> propName.equals("id") ? "user_id_custom" : Strings.toSnakeCase(propName))
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
                .propNameConverterForScreamingSnakeCase(
                        (cls, propName) -> propName.equals("id") ? "USER_ID_CUSTOM" : Strings.toScreamingSnakeCase(propName))
                .build();

        final String code = CodeGenerationUtil.generatePropNameTableClasses(config);

        assertNotNull(code);
        assertTrue(code.contains("USER_ID_CUSTOM"));
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
    public void test_MIN_FUNC_withComparable() {
        final String result = CodeGenerationUtil.MIN_FUNC.apply(User.class, Integer.class, "age");
        assertEquals("min(age)", result);
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
    public void test_MAX_FUNC_withNonComparable() {
        final String result = CodeGenerationUtil.MAX_FUNC.apply(User.class, Object.class, "obj");
        assertEquals(null, result);
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
}
