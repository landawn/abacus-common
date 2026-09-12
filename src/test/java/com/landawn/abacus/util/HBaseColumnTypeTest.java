package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class HBaseColumnTypeTest {
    @TempDir
    Path directory;

    @Test
    void primitiveTokensProduceTheMatchingBoxedDefaults() {
        assertEquals(0, HBaseColumn.emptyOf(int.class).value());
        assertEquals(0L, HBaseColumn.emptyOf(long.class).value());
        assertEquals((short) 0, HBaseColumn.emptyOf(short.class).value());
        assertEquals((byte) 0, HBaseColumn.emptyOf(byte.class).value());
        assertEquals('\0', HBaseColumn.emptyOf(char.class).value());
        assertEquals(false, HBaseColumn.emptyOf(boolean.class).value());
        assertEquals(0.0f, HBaseColumn.emptyOf(float.class).value());
        assertEquals(0.0, HBaseColumn.emptyOf(double.class).value());
        assertNull(HBaseColumn.emptyOf(String.class).value());
        assertNull(HBaseColumn.emptyOf(Integer.class).value());
        assertSame(HBaseColumn.emptyOf(int.class), HBaseColumn.emptyOf(int.class));
    }

    @Test
    void aClassTokenCannotPromiseAnIncompatibleColumnValue() throws Exception {
        UtilCycle2CompilationSupport.compile(directory, "ValidColumn",
                "HBaseColumn<Integer> value = HBaseColumn.emptyOf(int.class); HBaseColumn<? extends Number> producer = HBaseColumn.emptyOf(int.class);", true);
        UtilCycle2CompilationSupport.compile(directory, "InvalidColumn", "HBaseColumn<String> value = HBaseColumn.emptyOf(int.class);", false);
    }
}
