package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Clob;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UtilsTest extends TestBase {

    @Test
    public void testOpenBinaryStreamPreservesUncheckedFreeFailure() throws Exception {
        final Blob blob = mock(Blob.class);
        final RuntimeException freeFailure = new IllegalStateException("free");
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doThrow(freeFailure).when(blob).free();

        final InputStream stream = Utils.openBinaryStream(blob);

        assertSame(freeFailure, assertThrows(IllegalStateException.class, stream::close));
    }

    @Test
    public void testOpenCharacterStreamPreservesErrorFromFree() throws Exception {
        final Clob clob = mock(Clob.class);
        final AssertionError freeFailure = new AssertionError("free");
        when(clob.getCharacterStream()).thenReturn(new StringReader("value"));
        doThrow(freeFailure).when(clob).free();

        final Reader reader = Utils.openCharacterStream(clob);

        assertSame(freeFailure, assertThrows(AssertionError.class, reader::close));
    }
}
