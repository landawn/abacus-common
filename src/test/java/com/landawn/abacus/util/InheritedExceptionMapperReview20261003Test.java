package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;

import testfixtures.ExceptionMapperReview20261003Fixtures.ForceBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.ForceChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.InheritedBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.InheritedChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.InheritedGrandchild;
import testfixtures.ExceptionMapperReview20261003Fixtures.LateBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.LateChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.MappedFailure;
import testfixtures.ExceptionMapperReview20261003Fixtures.NullBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.NullChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.OverrideBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.OverrideChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.OverrideGrandchild;
import testfixtures.ExceptionMapperReview20261003Fixtures.SequenceBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.SequenceChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.SpecificFailure;
import testfixtures.ExceptionMapperReview20261003Fixtures.ThrowingBase;
import testfixtures.ExceptionMapperReview20261003Fixtures.ThrowingChild;
import testfixtures.ExceptionMapperReview20261003Fixtures.Unmapped;

@Tag("unit")
public class InheritedExceptionMapperReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void registeredMapperRecognizesEveryDescendant(final int depth) {
        ExceptionUtil.registerRuntimeExceptionMapper(InheritedBase.class, MappedFailure::new, true);
        final InheritedBase cause = depth == 0 ? new InheritedBase() : depth == 1 ? new InheritedChild() : new InheritedGrandchild();
        final RuntimeException wrapper = ExceptionUtil.toRuntimeException(cause);
        assertInstanceOf(MappedFailure.class, wrapper);
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void inheritedMapperWorksInsideReflectionAndConcurrentWrappers(final boolean concurrent) {
        ExceptionUtil.registerRuntimeExceptionMapper(InheritedBase.class, MappedFailure::new, true);
        final InheritedChild cause = new InheritedChild();
        final RuntimeException mapped = ExceptionUtil.toRuntimeException(cause);
        final Exception wrapper = concurrent ? new ExecutionException(mapped) : new InvocationTargetException(mapped);
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
    }

    @Test
    void nearestRegisteredAncestorWinsInBothDirections() {
        ExceptionUtil.registerRuntimeExceptionMapper(OverrideBase.class, MappedFailure::new);
        ExceptionUtil.registerRuntimeExceptionMapper(OverrideChild.class, SpecificFailure::new);
        final OverrideGrandchild cause = new OverrideGrandchild();
        final RuntimeException mapped = ExceptionUtil.toRuntimeException(cause);
        assertInstanceOf(SpecificFailure.class, mapped);
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(mapped));
        final MappedFailure unrelated = new MappedFailure(cause);
        assertSame(unrelated, ExceptionUtil.tryToGetOriginalCheckedException(unrelated));
    }

    @Test
    void lateAncestorRegistrationInvalidatesPreviouslyResolvedDefault() {
        final LateChild cause = new LateChild();
        assertInstanceOf(UncheckedException.class, ExceptionUtil.toRuntimeException(cause));
        ExceptionUtil.registerRuntimeExceptionMapper(LateBase.class, MappedFailure::new);
        final RuntimeException mapped = ExceptionUtil.toRuntimeException(cause);
        assertInstanceOf(MappedFailure.class, mapped);
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(mapped));
    }

    @Test
    void forcedAncestorReplacementInvalidatesPreviouslyResolvedMapper() {
        final ForceChild cause = new ForceChild();
        ExceptionUtil.registerRuntimeExceptionMapper(ForceBase.class, MappedFailure::new);
        assertInstanceOf(MappedFailure.class, ExceptionUtil.toRuntimeException(cause));
        ExceptionUtil.registerRuntimeExceptionMapper(ForceBase.class, SpecificFailure::new, true);
        final RuntimeException mapped = ExceptionUtil.toRuntimeException(cause);
        assertInstanceOf(SpecificFailure.class, mapped);
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(mapped));
    }

    @Test
    void streamBridgeRecoversTheInheritedCheckedFailureRaisedByItsStage() {
        ExceptionUtil.registerRuntimeExceptionMapper(SequenceBase.class, MappedFailure::new);
        final SequenceChild cause = new SequenceChild();
        final Exception actual = assertThrows(Exception.class, () -> {
            try (var sequence = Seq.<Integer, SequenceBase> of(1).transformViaStream(stream -> stream.map(value -> {
                throw ExceptionUtil.toRuntimeException(cause);
            }))) {
                sequence.toList();
            }
        });
        assertSame(cause, actual);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullReturningMapperKeepsItsFailureContract(final boolean inherited) {
        ExceptionUtil.registerRuntimeExceptionMapper(NullBase.class, cause -> null, true);
        final NullBase cause = inherited ? new NullChild() : new NullBase();
        assertThrows(NullPointerException.class, () -> ExceptionUtil.tryToGetOriginalCheckedException(new MappedFailure(cause)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void throwingMapperKeepsItsFailureIdentity(final boolean inherited) {
        final IllegalStateException failure = new IllegalStateException("mapper failure");
        ExceptionUtil.registerRuntimeExceptionMapper(ThrowingBase.class, cause -> {
            throw failure;
        }, true);
        final ThrowingBase cause = inherited ? new ThrowingChild() : new ThrowingBase();
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ExceptionUtil.tryToGetOriginalCheckedException(new MappedFailure(cause))));
    }

    @Test
    void unrelatedRuntimeWrappersRemainUnchanged() {
        final Exception[] wrappers = { new MappedFailure(new Unmapped()), new SpecificFailure(new IOException()), new RuntimeException(new IOException()),
                new MappedFailure(null) };
        for (final Exception wrapper : wrappers) {
            assertSame(wrapper, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
        }
    }

    @Test
    void unregisteredCauseDoesNotConstructADefaultWrapper() {
        final int[] stringCalls = { 0 };
        final Exception cause = new Exception() {
            @Override
            public String toString() {
                stringCalls[0]++;
                return "unregistered";
            }
        };
        // An explicit message avoids stringifying the cause when this wrapper is created.
        final RuntimeException wrapper = new RuntimeException("original", cause);
        for (int i = 0; i < 3; i++) {
            assertSame(wrapper, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
        }
        assertEquals(0, stringCalls[0], "looking up an absent mapper must not construct a throwaway exception");
    }

    @Test
    void runtimeCausesAndUndeclaredThrowableWrappersRemainUnchanged() {
        final RuntimeException cause = new IllegalArgumentException();
        final MappedFailure wrapper = new MappedFailure(cause);
        assertSame(wrapper, ExceptionUtil.tryToGetOriginalCheckedException(wrapper));
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(new ExecutionException(cause)));
        final UndeclaredThrowableException undeclared = new UndeclaredThrowableException(new IOException());
        assertSame(undeclared, ExceptionUtil.tryToGetOriginalCheckedException(undeclared));
    }

    @Test
    void builtInCheckedConversionsAndCheckedInputsKeepTheirIdentity() {
        final IOException cause = new java.io.FileNotFoundException();
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(ExceptionUtil.toRuntimeException(cause)));
        assertSame(cause, ExceptionUtil.tryToGetOriginalCheckedException(cause));
    }

    @Test
    @Timeout(30)
    void inheritedCauseDoesNotOutliveItsDisposableLoader(@TempDir final Path directory) throws Exception {
        final Path source = directory.resolve("Descendant.java");
        Files.writeString(source, """
                package disposable;
                public class Descendant extends testfixtures.ExceptionMapperReview20261003Fixtures.InheritedBase { }
                """);
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-cp", classpath, "-d", directory.toString(), source.toString()));
        final Path log = directory.resolve("loader.log");
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx96m", "-XX:ActiveProcessorCount=2",
                "-XX:+ClassUnloading", "-cp", classpath, LoaderProbe.class.getName(), directory.toString()).redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "loader probe completed");
            assertEquals(0, process.exitValue(), Files.readString(log));
        } finally {
            process.destroyForcibly();
        }
    }

    public static final class LoaderProbe {
        public static void main(final String[] args) throws Exception {
            ExceptionUtil.registerRuntimeExceptionMapper(InheritedBase.class, MappedFailure::new);
            final WeakReference<?>[] references = exercise(Path.of(args[0]));
            for (int round = 0; round < 80; round++) {
                System.gc();
                if (references[0].get() == null && references[1].get() == null) {
                    return;
                }
                final byte[][] pressure = new byte[8][512 * 1024];
                Thread.sleep(10);
            }
            throw new AssertionError("Inherited conversion retained the disposable cause class or loader");
        }

        private static WeakReference<?>[] exercise(final Path directory) throws Exception {
            try (final URLClassLoader loader = new URLClassLoader(new URL[] { directory.toUri().toURL() }, LoaderProbe.class.getClassLoader())) {
                final Class<?> type = loader.loadClass("disposable.Descendant");
                final Exception cause = (Exception) type.getConstructor().newInstance();
                if (ExceptionUtil.tryToGetOriginalCheckedException(ExceptionUtil.toRuntimeException(cause)) != cause) {
                    throw new AssertionError("Inherited conversion did not recover the original checked cause");
                }
                return new WeakReference<?>[] { new WeakReference<>(type), new WeakReference<>(loader) };
            }
        }
    }
}
