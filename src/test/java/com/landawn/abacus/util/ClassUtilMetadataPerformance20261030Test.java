package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@Tag("unit")
class ClassUtilMetadataPerformance20261030Test {
    private static final String ALIAS_NAME = "abcdefghijklmnopqrstuvwx";

    @TempDir
    Path temp;

    @Test
    void boundsSuccessfulAliasesWhileAlwaysCachingExactDeclaredNames() throws Exception {
        final Method noArgs = AliasOwner.class.getDeclaredMethod(ALIAS_NAME);
        final Method withArgs = AliasOwner.class.getDeclaredMethod(ALIAS_NAME, int.class);

        for (int bits = 1; bits <= 4096; bits++) {
            assertEquals(noArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, alias(bits)));
            assertEquals(withArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, alias(bits), int.class));
        }

        assertEquals(256, cache("classNoArgDeclaredMethodPool", AliasOwner.class).size());
        assertEquals(256, cache("classDeclaredMethodPool", AliasOwner.class).size());
        final Method cachedNoArgs = ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME);
        final Method cachedWithArgs = ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, int.class);
        assertSame(cachedNoArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, (Class<?>[]) null));
        assertSame(cachedWithArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, int.class));
        assertEquals(257, cache("classNoArgDeclaredMethodPool", AliasOwner.class).size());
        assertEquals(257, cache("classDeclaredMethodPool", AliasOwner.class).size());
        assertFalse(cache("classNoArgDeclaredMethodPool", AliasOwner.class).containsKey(alias(4096)));
        assertFalse(cache("classDeclaredMethodPool", AliasOwner.class).containsKey(alias(4096)));
        assertFalse(cachedNoArgs.isAccessible());

        final Class<?>[] signature = { int.class };
        assertSame(cachedWithArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, signature));
        signature[0] = String.class;
        assertNull(ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, signature));
        assertSame(cachedWithArgs, ClassUtil.getDeclaredMethod(AliasOwner.class, ALIAS_NAME, int.class));
        assertNull(ClassUtil.getDeclaredMethod(AliasChild.class, ALIAS_NAME));
    }

    @Test
    void concurrentAliasAdmissionStaysBoundedAndExactLookupsKeepIdentity() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            final List<Callable<Void>> tasks = new ArrayList<>();

            for (int worker = 0; worker < 8; worker++) {
                final int offset = worker;
                tasks.add(() -> {
                    for (int bits = offset + 1; bits <= 4096; bits += 8) {
                        assertNotNull(ClassUtil.getDeclaredMethod(ParallelAliasOwner.class, alias(bits)));
                        assertNotNull(ClassUtil.getDeclaredMethod(ParallelAliasOwner.class, alias(bits), int.class));
                    }
                    return null;
                });
            }

            for (var result : executor.invokeAll(tasks)) {
                result.get();
            }

            assertEquals(256, cache("classNoArgDeclaredMethodPool", ParallelAliasOwner.class).size());
            assertEquals(256, cache("classDeclaredMethodPool", ParallelAliasOwner.class).size());
            final Method expected = ClassUtil.getDeclaredMethod(ParallelAliasOwner.class, ALIAS_NAME, int.class);
            final List<Callable<Method>> exactTasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                exactTasks.add(() -> ClassUtil.getDeclaredMethod(ParallelAliasOwner.class, ALIAS_NAME, int.class));
            }
            for (var result : executor.invokeAll(exactTasks)) {
                assertSame(expected, result.get());
            }
        }
    }

    @Test
    void distinctDeclaredCaseNamesAndFallbackTieOrderArePreserved() throws Exception {
        for (final Class<?>[] signature : new Class<?>[][] { {}, { int.class } }) {
            final Method lower = ClassUtil.getDeclaredMethod(CaseOwner.class, "alpha", signature);
            final Method upper = ClassUtil.getDeclaredMethod(CaseOwner.class, "Alpha", signature);
            assertEquals("alpha", lower.getName());
            assertEquals("Alpha", upper.getName());
            assertSame(lower, ClassUtil.getDeclaredMethod(CaseOwner.class, "alpha", signature));
            assertSame(upper, ClassUtil.getDeclaredMethod(CaseOwner.class, "Alpha", signature));

            Method expectedFallback = null;
            for (final Method method : CaseOwner.class.getDeclaredMethods()) {
                if (method.getName().equalsIgnoreCase("ALPHA") && java.util.Arrays.equals(signature, method.getParameterTypes())) {
                    expectedFallback = method;
                    break;
                }
            }
            assertEquals(expectedFallback, ClassUtil.getDeclaredMethod(CaseOwner.class, "ALPHA", signature));
        }
    }

    @Test
    void longCallerAliasesAreNotRetainedButExactLongNamesAreCached() throws Exception {
        final String name = "a".repeat(257);
        final String alternate = "A" + name.substring(1);
        final byte[] bytes = classBytes("longalias.Owner", name);
        final Class<?> owner = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass("longalias.Owner", bytes, 0, bytes.length);
            }
        }.define();

        for (final Class<?>[] signature : new Class<?>[][] { {}, { int.class } }) {
            assertEquals(owner.getDeclaredMethod(name, signature), ClassUtil.getDeclaredMethod(owner, alternate, signature));
        }
        assertTrue(cache("classNoArgDeclaredMethodPool", owner).isEmpty());
        assertTrue(cache("classDeclaredMethodPool", owner).isEmpty());
        for (final Class<?>[] signature : new Class<?>[][] { {}, { int.class } }) {
            assertSame(ClassUtil.getDeclaredMethod(owner, name, signature), ClassUtil.getDeclaredMethod(owner, name, signature));
        }
        assertEquals(1, cache("classNoArgDeclaredMethodPool", owner).size());
        assertEquals(1, cache("classDeclaredMethodPool", owner).size());
    }

    @Test
    void overlappingDirectoryRootsVisitEachPhysicalClassOnceAtDepthEight() throws Exception {
        final Path first = temp.resolve("first");
        final Path second = temp.resolve("second");
        final String pkg = "metadatascan.deep";
        final String leaf = pkg + ".sub".repeat(8);
        writeClass(first, leaf + ".LeafA");
        writeClass(second, leaf + ".LeafB");

        try (URLClassLoader loader = scannerLoader(first, second)) {
            final List<String> visited = new ArrayList<>();
            final List<String> expected = List.of(leaf + ".LeafA", leaf + ".LeafB");
            assertEquals(expected, names(scan(loader, pkg, true, false, cls -> {
                visited.add(cls.getName());
                return true;
            })));
            assertEquals(expected, visited);
            assertTrue(scan(loader, pkg, false, false, cls -> true).isEmpty());

            // Two separate files for the same binary name remain two discovery hits.
            writeClass(second, leaf + ".LeafA");
            final List<String> duplicateNames = names(scan(loader, pkg, true, false, cls -> true));
            assertEquals(3, duplicateNames.size());
            assertEquals(2, duplicateNames.stream().filter(name -> name.equals(leaf + ".LeafA")).count());
        }
    }

    @Test
    void mixedJarAndDirectoryDiscoveryPreservesPhysicalOrderAndNonrecursiveFiltering() throws Exception {
        final String pkg = "metadatascan.mixed";
        final Path directory = temp.resolve("directory");
        writeClass(directory, pkg + ".Direct");
        final Path archive = temp.resolve("fixtures.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            for (final String name : List.of("metadatascan/", "metadatascan/mixed/", "metadatascan/mixed/sub/")) {
                jar.putNextEntry(new JarEntry(name));
                jar.closeEntry();
            }
            for (final String name : List.of(pkg + ".JarFirst", pkg + ".sub.JarChild", pkg + ".JarLast")) {
                jar.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
                jar.write(classBytes(name, null));
                jar.closeEntry();
            }
        }

        try (URLClassLoader loader = scannerLoader(directory, archive)) {
            final List<String> visited = new ArrayList<>();
            assertEquals(List.of(pkg + ".Direct", pkg + ".JarFirst", pkg + ".sub.JarChild", pkg + ".JarLast"), names(scan(loader, pkg, true, false, cls -> {
                visited.add(cls.getName());
                return true;
            })));
            assertEquals(List.of(pkg + ".Direct", pkg + ".JarFirst", pkg + ".sub.JarChild", pkg + ".JarLast"), visited);
            assertEquals(List.of(pkg + ".Direct", pkg + ".JarFirst", pkg + ".JarLast"), names(scan(loader, pkg, false, false, cls -> true)));

            final AssertionError marker = new AssertionError("predicate failure");
            assertSame(marker, assertThrows(InvocationTargetException.class, () -> scan(loader, pkg, true, true, cls -> {
                if (cls.getSimpleName().equals("JarFirst")) {
                    throw marker;
                }
                return true;
            })).getCause());
        }
        // On Windows a leaked JarFile would prevent removal after the class loader is closed.
        Files.delete(archive);
    }

    @Test
    void directoryDiscoveryKeepsLoadingFailuresAndPredicateFailuresSeparate() throws Exception {
        final String pkg = "metadatascan.errors";
        final Path directory = temp.resolve("errors");
        writeClass(directory, pkg + ".Good");
        Files.write(directory.resolve(pkg.replace('.', '/')).resolve("Broken.class"), new byte[] { 1, 2, 3 });
        Files.createDirectories(directory.resolve(pkg.replace('.', '/')).resolve("v1.2"));

        try (URLClassLoader loader = scannerLoader(directory)) {
            assertEquals(List.of(pkg + ".Good"), names(scan(loader, pkg, true, true, cls -> true)));
            assertTrue(assertThrows(InvocationTargetException.class, () -> scan(loader, pkg, true, false, cls -> true))
                    .getCause() instanceof IllegalStateException);
            final IllegalArgumentException marker = new IllegalArgumentException("predicate failure");
            assertSame(marker, assertThrows(InvocationTargetException.class, () -> scan(loader, pkg, true, true, cls -> {
                throw marker;
            })).getCause());
        }
    }

    @Test
    @Timeout(45)
    void handlesKeepPublicPrivateDefaultStaticCallerSensitiveAndVirtualSemanticsWithoutModuleOpens() throws Exception {
        final Path output = temp.resolve("handles.log");
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx128m", "-cp", classpath,
                HandleProbe.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "handle probe timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            process.destroyForcibly();
        }
    }

    private static Map<?, ?> cache(final String fieldName, final Class<?> owner) throws Exception {
        final Field field = ClassUtil.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Map<?, ?>) ((ClassValue<?>) field.get(null)).get(owner);
    }

    private static String alias(final int bits) {
        final char[] chars = ALIAS_NAME.toCharArray();
        for (int bit = 0; bit < 13; bit++) {
            if ((bits & (1 << bit)) != 0) {
                chars[bit] = Character.toUpperCase(chars[bit]);
            }
        }
        return new String(chars);
    }

    private static byte[] classBytes(final String binaryName, final String methodName) {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, binaryName.replace('.', '/'), null, "java/lang/Object", null);
        if (methodName != null) {
            for (final String descriptor : List.of("()V", "(I)V")) {
                final MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, methodName, descriptor, null, null);
                method.visitCode();
                method.visitInsn(Opcodes.RETURN);
                method.visitMaxs(0, descriptor.equals("()V") ? 1 : 2);
                method.visitEnd();
            }
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void writeClass(final Path root, final String name) throws Exception {
        final Path file = root.resolve(name.replace('.', '/') + ".class");
        Files.createDirectories(file.getParent());
        Files.write(file, classBytes(name, null));
    }

    private static URLClassLoader scannerLoader(final Path... roots) throws Exception {
        final List<URL> urls = new ArrayList<>();
        for (final Path root : roots) {
            urls.add(root.toUri().toURL());
        }
        urls.add(ClassUtil.class.getProtectionDomain().getCodeSource().getLocation());
        for (final String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
    }

    private static List<?> scan(final ClassLoader loader, final String pkg, final boolean recursive, final boolean skipFailures,
            final Predicate<Class<?>> predicate) throws Exception {
        final Class<?> util = Class.forName(ClassUtil.class.getName(), true, loader);
        return (List<?>) util.getMethod("findClassesInPackage", String.class, boolean.class, boolean.class, Predicate.class)
                .invoke(null, pkg, recursive, skipFailures, predicate);
    }

    private static List<String> names(final List<?> classes) {
        return classes.stream().map(cls -> ((Class<?>) cls).getName()).toList();
    }

    public static class AliasOwner {
        public void abcdefghijklmnopqrstuvwx() {
        }

        public void abcdefghijklmnopqrstuvwx(final int value) {
        }
    }

    public static class AliasChild extends AliasOwner {
    }

    public static class ParallelAliasOwner {
        public void abcdefghijklmnopqrstuvwx() {
        }

        public void abcdefghijklmnopqrstuvwx(final int value) {
        }
    }

    public static class CaseOwner {
        public void alpha() {
        }

        public void Alpha() {
        }

        public void alpha(final int value) {
        }

        public void Alpha(final int value) {
        }
    }

    public interface WithDefault {
        default String value() {
            return "default";
        }
    }

    public static class Base implements WithDefault {
        public String virtual() {
            return "base";
        }

        private String hidden() {
            return "private";
        }
    }

    public static class Derived extends Base {
        @Override
        public String virtual() {
            return "derived";
        }
    }

    public static class HandleProbe {
        public static void main(final String[] args) throws Throwable {
            final MethodHandle length = ClassUtil.createMethodHandle(String.class.getMethod("length"));
            assertEquals(5, (int) length.invokeExact("value"));
            final MethodHandle parse = ClassUtil.createMethodHandle(Integer.class.getMethod("parseInt", String.class));
            assertEquals(42, (int) parse.invokeExact("42"));
            final MethodHandle virtual = ClassUtil.createMethodHandle(Base.class.getDeclaredMethod("virtual"));
            assertEquals("derived", (String) virtual.invokeExact((Base) new Derived()));
            final MethodHandle defaults = ClassUtil.createMethodHandle(WithDefault.class.getMethod("value"));
            assertEquals("default", (String) defaults.invokeExact((WithDefault) new Base()));
            final MethodHandle hidden = ClassUtil.createMethodHandle(Base.class.getDeclaredMethod("hidden"));
            assertEquals("private", (String) hidden.invokeExact(new Base()));
            final MethodHandle callerSensitive = ClassUtil.createMethodHandle(Class.class.getMethod("forName", String.class));
            assertSame(Base.class, (Class<?>) callerSensitive.invokeExact(Base.class.getName()));
            assertThrows(UnsupportedOperationException.class, () -> ClassUtil.createMethodHandle(String.class.getDeclaredMethod("coder")));
            final Class<?> unexported = Class.forName("jdk.internal.misc.VM");
            assertThrows(UnsupportedOperationException.class, () -> ClassUtil.createMethodHandle(unexported.getMethod("getSavedProperty", String.class)));
            System.out.println("public/private/default/static/caller-sensitive/virtual and module failure contracts passed");
        }
    }
}
