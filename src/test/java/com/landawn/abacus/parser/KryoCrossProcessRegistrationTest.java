package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;

public class KryoCrossProcessRegistrationTest extends TestBase {
    @TempDir
    Path directory;

    // A round trip within one JVM cannot detect identity-hash-dependent registration order. Use two independent JVMs
    // with different CDS and class-identity-hash initialization order, and exchange actual wire data. The second JVM
    // decodes the first one's payloads and writes its own; identical bytes and IDs then show that the first JVM would
    // decode the second one's payloads as well, so two child JVMs cover both directions (this used to start four).
    @Test
    @Timeout(90)
    public void independentJvmsAssignTheSameIdsAndExchangePayloads() throws Exception {
        final Path first = Files.createDirectory(directory.resolve("first"));
        final Path second = Files.createDirectory(directory.resolve("second"));
        run("write", first, null, false);
        run("read-write", first, second, true);
        assertEquals(Files.readString(first.resolve("ids.txt")), Files.readString(second.resolve("ids.txt")));
        for (int i = 0; i < Probe.TYPES.size(); i++) {
            assertArrayEquals(Files.readAllBytes(first.resolve(i + ".bin")), Files.readAllBytes(second.resolve(i + ".bin")));
        }
    }

    private void run(final String action, final Path input, final Path output, final boolean reverse) throws Exception {
        final Path log = directory.resolve(action + "-" + input.getFileName() + ".log");
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), reverse ? "-Xshare:off" : "-Xshare:auto",
                "-Xmx128m", "-XX:ActiveProcessorCount=2", "--add-opens=java.base/java.lang=ALL-UNNAMED", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED", "--add-opens=java.base/java.util=ALL-UNNAMED", "-cp", classpath, Probe.class.getName(), action,
                input.toString(), Boolean.toString(reverse), output == null ? "" : output.toString()).redirectErrorStream(true)
                        .redirectOutput(log.toFile())
                        .start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), () -> "Child JVM timed out: " + action + " " + input);
            assertEquals(0, process.exitValue(), () -> {
                try {
                    return Files.readString(log);
                } catch (final Exception e) {
                    return e.toString();
                }
            });
        } finally {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    public static class KryoWireValue {
        public String text;
    }

    public static class KryoWireA extends KryoWireValue {
    }

    public static class KryoWireB extends KryoWireValue {
    }

    public static class KryoWireC extends KryoWireValue {
    }

    public static class KryoWireD extends KryoWireValue {
    }

    public static class KryoWireE extends KryoWireValue {
    }

    public static class KryoWireF extends KryoWireValue {
    }

    public static class KryoWireG extends KryoWireValue {
    }

    public static class KryoWireH extends KryoWireValue {
    }

    public static class KryoWireI extends KryoWireValue {
    }

    public static class KryoWireJ extends KryoWireValue {
    }

    public static class KryoWireK extends KryoWireValue {
    }

    public static class KryoWireL extends KryoWireValue {
    }

    public static class KryoWireM extends KryoWireValue {
    }

    public static class KryoWireN extends KryoWireValue {
    }

    private static final class ValueSerializer<T extends KryoWireValue> extends Serializer<T> {
        @Override
        public void write(final Kryo kryo, final Output output, final T value) {
            output.writeString(value.text);
        }

        @Override
        public T read(final Kryo kryo, final Input input, final Class<? extends T> type) {
            try {
                final T value = type.getConstructor().newInstance();
                value.text = input.readString();
                return value;
            } catch (final ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }

    public static class Probe {
        private static final List<Class<? extends KryoWireValue>> TYPES = List.of(KryoWireA.class, KryoWireB.class, KryoWireC.class, KryoWireD.class,
                KryoWireE.class, KryoWireF.class, KryoWireG.class, KryoWireH.class, KryoWireI.class, KryoWireJ.class, KryoWireK.class, KryoWireL.class,
                KryoWireM.class, KryoWireN.class);

        public static void main(final String[] args) throws Exception {
            // Initialize before Kryo's built-in serializer setup; no parent-JVM registrations are inherited.
            new StringBuffer().append("ready");
            final boolean reverse = Boolean.parseBoolean(args[2]);
            for (int i = 0; i < TYPES.size(); i++) {
                System.identityHashCode(TYPES.get(reverse ? TYPES.size() - 1 - i : i));
            }
            System.identityHashCode(reverse ? Integer.class : int.class);
            System.identityHashCode(reverse ? int.class : Integer.class);
            final KryoParser parser = registeredParser();
            final Path data = Path.of(args[1]);
            final String ids = registrationIds(parser);
            if (args[0].equals("read-write")) {
                // Decode the other JVM's payloads, then write this JVM's own for the byte comparison in the test.
                verify(parser, ids, data);
                write(parser, ids, Path.of(args[3]));
            } else {
                write(parser, ids, data);
            }
        }

        private static void write(final KryoParser parser, final String ids, final Path data) throws Exception {
            Files.writeString(data.resolve("ids.txt"), ids);
            for (int i = 0; i < TYPES.size(); i++) {
                final KryoWireValue value = TYPES.get(i).getConstructor().newInstance();
                value.text = "payload-" + i + "-世界";
                Files.write(data.resolve(i + ".bin"), parser.encode(value));
            }
        }

        private static void verify(final KryoParser parser, final String ids, final Path data) throws Exception {
            if (!ids.equals(Files.readString(data.resolve("ids.txt")))) {
                throw new AssertionError("Registration IDs differ between independent JVMs");
            }
            for (int i = 0; i < TYPES.size(); i++) {
                final Object decoded = parser.decode(Files.readAllBytes(data.resolve(i + ".bin")));
                if (decoded.getClass() != TYPES.get(i) || !("payload-" + i + "-世界").equals(((KryoWireValue) decoded).text)) {
                    throw new AssertionError("Wrong decoded class or payload at index " + i);
                }
            }
        }

        private static KryoParser registeredParser() {
            // Deliberately interleave the four overload kinds. Their documented bucket order is distinct
            // from overall call order; both global and local scopes must replay identically in every JVM.
            ParserFactory.registerKryo(KryoWireE.class, new ValueSerializer<KryoWireE>());
            ParserFactory.registerKryo(KryoWireA.class);
            ParserFactory.registerKryo(KryoWireD.class, 20_000);
            ParserFactory.registerKryo(KryoWireB.class);
            ParserFactory.registerKryo(KryoWireG.class, new ValueSerializer<KryoWireG>(), 21_000);
            ParserFactory.registerKryo(KryoWireF.class, new ValueSerializer<KryoWireF>());
            ParserFactory.registerKryo(KryoWireC.class);
            ParserFactory.registerKryo(KryoWireA.class); // Move to the latest position of this kind.
            ParserFactory.registerKryo(int.class, 24_000);
            ParserFactory.registerKryo(Integer.class, 24_001); // Shared primitive/wrapper registration: latest call wins.

            final KryoParser parser = new KryoParser();
            parser.register(KryoWireL.class, new ValueSerializer<KryoWireL>());
            parser.register(KryoWireH.class);
            parser.register(KryoWireK.class, 25_000);
            parser.register(KryoWireI.class);
            parser.register(KryoWireN.class, new ValueSerializer<KryoWireN>(), 26_000);
            parser.register(KryoWireM.class, new ValueSerializer<KryoWireM>());
            parser.register(KryoWireJ.class);
            parser.register(KryoWireH.class);
            return parser;
        }

        private static String registrationIds(final KryoParser parser) {
            final Kryo kryo = parser.createKryo();
            try {
                final StringBuilder ids = new StringBuilder();
                for (final Class<?> type : TYPES) {
                    ids.append(type.getName()).append('=').append(kryo.getRegistration(type).getId()).append('\n');
                }
                if (kryo.getRegistration(int.class).getId() != 24_001 || kryo.getRegistration(Integer.class).getId() != 24_001) {
                    throw new AssertionError("Primitive/wrapper latest-call registration was not preserved");
                }
                return ids.append("int/Integer=24001\n").toString();
            } finally {
                parser.recycle(kryo);
            }
        }
    }
}
