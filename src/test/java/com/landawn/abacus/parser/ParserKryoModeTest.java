package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Strings;

public class ParserKryoModeTest extends TestBase {
    private final KryoParser parser = ParserFactory.createKryoParser();

    @Test
    public void defaultTypedScalarRoundTripsPreserveZerosAndBoundaries() {
        for (Object value : List.of(0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0L, Long.MIN_VALUE, Long.MAX_VALUE, (byte) 0, Byte.MIN_VALUE,
                Byte.MAX_VALUE, (short) 0, Short.MIN_VALUE, false, true, 0.0f, -0.0f, Float.NaN, Float.POSITIVE_INFINITY, 0.0d, -0.0d, Double.MIN_VALUE,
                Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.NaN)) {
            assertEquals(value, parser.deserialize(parser.serialize(value), value.getClass()), value.getClass().getName());
        }
    }

    @Test
    public void primitiveTargetsAndTypeDescriptorsSelectTypedData() {
        for (Class<?> primitive : List.of(int.class, long.class, byte.class, boolean.class)) {
            Object value = primitive == boolean.class ? false : primitive == long.class ? (Object) 0L : primitive == byte.class ? (Object) (byte) 0 : 0;
            String encoded = parser.serialize(value);
            assertEquals(value, parser.deserialize(encoded, primitive));
            assertEquals(value, parser.deserialize(encoded, Type.of(primitive)));
        }
    }

    @Test
    public void allSourceOverloadsPreserveTypedZeroAndText() throws Exception {
        Path file = Files.createTempFile("parser-kryo-mode-", ".bin");
        try {
            for (Object value : List.of(0, 0L, (byte) 0, false, "", "\u540d\ud83d\ude00", new ArrayList<>(), new HashMap<>())) {
                String encoded = parser.serialize(value);
                assertEquals(value, parser.deserialize(new StringReader(encoded), value.getClass()));
                assertEquals(value, parser.deserialize(new ByteArrayInputStream(Strings.base64Decode(encoded)), value.getClass()));
                parser.serialize(value, null, file.toFile());
                assertEquals(value, parser.deserialize(file.toFile(), value.getClass()));
                assertEquals(value, parser.deserialize(file.toFile(), Type.of(value.getClass())));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void nullTargetSelectsClassBearingValuesAndNulls() throws Exception {
        Path file = Files.createTempFile("parser-kryo-class-mode-", ".bin");
        try {
            for (Object value : List.of(0, 0L, false, "", "\u540d\ud83d\ude00", new ArrayList<>())) {
                KryoSerConfig config = KryoSerConfig.create().setWriteClass(true);
                String encoded = parser.serialize(value, config);
                assertEquals(value, parser.deserialize(encoded, (Class<Object>) null));
                assertEquals(value, parser.deserialize(new StringReader(encoded), (Class<Object>) null));
                assertEquals(value, parser.deserialize(new ByteArrayInputStream(Strings.base64Decode(encoded)), (Class<Object>) null));
                parser.serialize(value, config, file.toFile());
                assertEquals(value, parser.deserialize(file.toFile(), (Class<Object>) null));
            }
            for (KryoSerConfig config : List.of(KryoSerConfig.create(), KryoSerConfig.create().setWriteClass(true))) {
                assertNull(parser.deserialize(parser.serialize(null, config), (Class<Object>) null));
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    public static class ZeroPayload {
    }

    @Test
    public void customTypedPayloadIsNeverSpeculativelyReadAsNull() {
        parser.register(ZeroPayload.class, new Serializer<ZeroPayload>() {
            @Override
            public void write(Kryo kryo, Output output, ZeroPayload value) {
                output.writeByte(0);
            }

            @Override
            public ZeroPayload read(Kryo kryo, Input input, Class<? extends ZeroPayload> type) {
                assertEquals(0, input.readByte());
                return new ZeroPayload();
            }
        });
        Object result = parser.deserialize(parser.serialize(new ZeroPayload()), ZeroPayload.class);
        assertEquals(ZeroPayload.class, result.getClass());
    }

    @Test
    public void failedReadsDoNotPoisonThePoolOrCloseCallerInput() {
        assertThrows(RuntimeException.class, () -> parser.deserialize(new ByteArrayInputStream(new byte[] { (byte) 0x80 }), Integer.class));
        class TrackedInput extends ByteArrayInputStream {
            boolean closed;

            TrackedInput() {
                super(Strings.base64Decode(parser.serialize(0)));
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackedInput input = new TrackedInput();
        assertEquals(Integer.valueOf(0), parser.deserialize(input, Integer.class));
        assertFalse(input.closed);
    }
}
