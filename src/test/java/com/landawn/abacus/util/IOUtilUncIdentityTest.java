package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class IOUtilUncIdentityTest extends TestBase {
    // Supply the real-path spellings returned by Windows/JDK 21 without requiring an administrative SMB share.
    private static File fileResolvingTo(final String realPath) throws Exception {
        final Path resolved = Path.of(realPath);
        // Only toRealPath is exercised on this input. The result is a real Windows Path, so all identity and
        // component comparisons still use the filesystem provider. A JDK proxy needs no agent/self-attachment.
        final Path input = (Path) Proxy.newProxyInstance(Path.class.getClassLoader(), new Class<?>[] { Path.class }, (proxy, method, args) -> {
            if (method.getName().equals("toRealPath")) {
                return resolved;
            }
            throw new AssertionError("Unexpected input-path operation: " + method);
        });
        return new File("unused") {
            private static final long serialVersionUID = 1L;

            @Override
            public Path toPath() {
                return input;
            }
        };
    }

    private static Method ioUtilMethod(final String name, final Class<?>... parameterTypes) throws Exception {
        final Method method = IOUtil.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static void assertRejected(final Method check, final Object... args) {
        final InvocationTargetException e = assertThrows(InvocationTargetException.class, () -> check.invoke(null, args));
        assertInstanceOf(IllegalArgumentException.class, e.getCause(), () -> java.util.Arrays.toString(args));
    }

    private static Object newGuard(final Path sourceRoot) throws Exception {
        final Class<?> guardClass = Class.forName(IOUtil.class.getName() + "$CopyCycleGuard");
        final var constructor = guardClass.getDeclaredConstructor(Path.class);
        constructor.setAccessible(true);
        return constructor.newInstance(sourceRoot);
    }

    @Test
    public void sameDirectoryIgnoresRootCaseButPreservesComponentCase() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS);
        final Method same = ioUtilMethod("isSameDirectory", File.class, File.class);
        assertEquals(true, same.invoke(null, fileResolvingTo("\\\\LOCALHOST\\c$\\dir\\child"), fileResolvingTo("\\\\localhost\\C$\\dir\\child")));
        assertEquals(true, same.invoke(null, fileResolvingTo("C:\\dir\\child"), fileResolvingTo("c:\\dir\\child")));
        assertEquals(false, same.invoke(null, fileResolvingTo("\\\\LOCALHOST\\c$\\dir\\child"), fileResolvingTo("\\\\localhost\\C$\\DIR\\child")));
    }

    @Test
    public void copyCycleGuardRecognizesRootAliasesWithoutFoldingDirectoryNames() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS);
        final Object guard = newGuard(Path.of("\\\\LOCALHOST\\c$\\dir\\src"));
        final Method leadsBack = guard.getClass().getDeclaredMethod("leadsBack", Path.class);
        leadsBack.setAccessible(true);
        final var outputRoot = guard.getClass().getDeclaredField("outputRoot");
        outputRoot.setAccessible(true);
        outputRoot.set(guard, Path.of("\\\\LOCALHOST\\c$\\dir\\out"));

        for (final String target : new String[] { "src", "out", "out\\child" }) {
            assertEquals(true, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\dir\\" + target)), target);
        }
        assertEquals(true, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\dir")));
        for (final String target : new String[] { "SRC", "OUT", "output", "srcSibling", "src\\child" }) {
            assertEquals(false, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\dir\\" + target)), target);
        }
    }

    // A learned alias prefix is matched like every other comparison: the root by the filesystem's rules, names as spelled.
    @Test
    public void learnedAliasesMatchRootCaseInsensitivelyButNamesExactly() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS);
        final Object guard = newGuard(Path.of("\\\\LOCALHOST\\c$\\real\\src"));
        final Method leadsBack = guard.getClass().getDeclaredMethod("leadsBack", Path.class);
        leadsBack.setAccessible(true);
        final var aliasesField = guard.getClass().getDeclaredField("aliases");
        aliasesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<Path, Path> aliases = (Map<Path, Path>) aliasesField.get(guard);
        aliases.put(Path.of("\\\\LOCALHOST\\c$\\alias"), Path.of("\\\\LOCALHOST\\c$\\real"));

        assertEquals(true, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\alias\\src")));
        assertEquals(true, leadsBack.invoke(guard, Path.of("\\\\LOCALHOST\\c$\\alias")));
        // Same root, so no identity lookup is attempted; a name differing in case is another directory.
        assertEquals(false, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\ALIAS\\src")));
        assertEquals(false, leadsBack.invoke(guard, Path.of("\\\\localhost\\C$\\alias\\SRC")));
    }

    // Bug fix: the up-front "destination inside the source" checks of copyToDirectory, copyDirectory and moveToDirectory
    // compared whole strings, so a destination reached through a junction - spelled with the redirector's
    // \\localhost\C$ for the caller's \\LOCALHOST\c$ - passed as outside the source, and the walk then copied the
    // source into itself. Roots are now compared by the filesystem's rules, names below them still exactly.
    @Test
    public void containmentChecksIgnoreRootCaseButNotDirectoryNameCase() throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS);
        final Method requireDest = ioUtilMethod("requireDestDirectoryOutsideSourceDirectory", Path.class, Path.class, String.class);
        final Method requireTarget = ioUtilMethod("requireCopyTargetOutsideSourceDirectory", Path.class, Path.class);
        final Method inSpellingOf = ioUtilMethod("inSpellingOf", Path.class, Path.class);

        for (final String source : new String[] { "\\\\LOCALHOST\\c$\\dir\\src", "C:\\dir\\src" }) {
            final Path src = Path.of(source);
            final String otherRoot = source.startsWith("\\\\") ? "\\\\localhost\\C$\\" : "c:\\";

            // The destination as the copy family compares it: translated into the source's spelling first.
            for (final String dest : new String[] { "dir\\src", "dir\\src\\sub", "dir\\src\\sub\\deeper" }) {
                final Path destInSourceSpelling = (Path) inSpellingOf.invoke(null, Path.of(otherRoot + dest), src);
                assertRejected(requireDest, src, destInSourceSpelling, "copy");
                assertRejected(requireDest, src, destInSourceSpelling, "move");
            }

            for (final String dest : new String[] { "dir\\SRC\\sub", "dir\\Src", "dir\\src2", "dir\\sr", "dir", "other\\src" }) {
                requireDest.invoke(null, src, inSpellingOf.invoke(null, Path.of(otherRoot + dest), src), "copy");
            }

            // The copy target - destinationDirectory/<name> - must not be the source or one of its ancestors.
            for (final String target : new String[] { "", "dir", "dir\\src" }) {
                assertRejected(requireTarget, src, inSpellingOf.invoke(null, Path.of(otherRoot + target), src));
            }

            for (final String target : new String[] { "DIR", "dir\\SRC", "dir\\src\\child", "di", "dir\\src2" }) {
                requireTarget.invoke(null, src, inSpellingOf.invoke(null, Path.of(otherRoot + target), src));
            }
        }
    }

    // On every platform the checks compare whole names, so a prefix neighbor is outside and the root itself contains all.
    @Test
    public void containmentChecksCompareWholeNames(@TempDir final Path directory) throws Exception {
        final Method requireDest = ioUtilMethod("requireDestDirectoryOutsideSourceDirectory", Path.class, Path.class, String.class);
        final Method requireTarget = ioUtilMethod("requireCopyTargetOutsideSourceDirectory", Path.class, Path.class);
        final Path src = directory.resolve("src");

        assertRejected(requireDest, src, src, "copy");
        assertRejected(requireDest, src, src.resolve("a").resolve("b"), "move");
        assertRejected(requireDest, directory.getRoot(), src, "copy");
        requireDest.invoke(null, src, directory.resolve("src2"), "copy");
        requireDest.invoke(null, src, directory.resolve("sr"), "copy");
        requireDest.invoke(null, src, directory, "copy");

        assertRejected(requireTarget, src, src);
        assertRejected(requireTarget, src, directory);
        assertRejected(requireTarget, src, directory.getRoot());
        requireTarget.invoke(null, src, src.resolve("child"));
        requireTarget.invoke(null, src, directory.resolve("src2"));
    }
}
