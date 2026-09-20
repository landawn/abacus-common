package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.alibaba.fastjson2.JSONReader;

@org.junit.jupiter.api.Tag("unit")
public class FastJsonReaderOwnershipTest extends TestBase {
    private static Object parse(final int mode, final Reader reader) {
        return switch (mode) {
            case 0 -> FastJson.fromJson(reader, Object.class);
            case 1 -> FastJson.fromJson(reader, Object.class, new JSONReader.Feature[0]);
            case 2 -> FastJson.fromJson(reader, Object.class, new JSONReader.Context());
            case 3 -> FastJson.fromJson(reader, (Type) Object.class);
            case 4 -> FastJson.fromJson(reader, (Type) Object.class, new JSONReader.Feature[0]);
            default -> FastJson.fromJson(reader, (Type) Object.class, new JSONReader.Context());
        };
    }

    @Test
    void allReaderOverloadsLeaveSourcesOpenOnSuccessEmptyAndMalformedInput() throws Exception {
        for (int mode = 0; mode < 6; mode++) {
            for (final String json : new String[] { "{\"value\":\"\uD83D\uDE00\"}", "", "{broken" }) {
                final int selected = mode;
                final int[] closes = { 0 };
                final var reader = new StringReader(json) {
                    @Override
                    public void close() {
                        closes[0]++;
                        super.close();
                    }
                };
                if (json.equals("{broken")) {
                    assertThrows(RuntimeException.class, () -> parse(selected, reader));
                } else if (json.isEmpty()) {
                    assertNull(parse(selected, reader));
                } else {
                    assertEquals("\uD83D\uDE00", ((Map<?, ?>) parse(selected, reader)).get("value"));
                }
                assertEquals(0, closes[0]);
                reader.read();
                reader.close();
                assertEquals(1, closes[0]);
            }
        }
    }

    @Test
    void ioFailuresAndNullValidationDoNotTransferOwnership() {
        for (int mode = 0; mode < 6; mode++) {
            final int selected = mode;
            final int[] closes = { 0 };
            final Reader failure = new Reader() {
                @Override
                public int read(char[] buffer, int offset, int length) throws IOException {
                    throw new IOException("read failure");
                }

                @Override
                public void close() {
                    closes[0]++;
                }
            };
            assertThrows(RuntimeException.class, () -> parse(selected, failure));
            assertEquals(0, closes[0]);
            assertThrows(IllegalArgumentException.class, () -> parse(selected, null));
        }
    }
}
