package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class BufferedReaderTest extends AbstractTest {

    @Test
    public void test_read() throws Exception {
        String str = "abc123456789";
        BufferedReader reader = new BufferedReader(str);

        assertEquals('a', reader.read());
        reader.skip(1);
        assertEquals('c', reader.read());
        assertTrue(reader.ready());
        assertEquals("123456789", reader.readLine());
    }

    @Test
    public void test_read_2() throws Exception {
        File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        String str = "abc123456789\n\rabc";
        IOUtil.write(str, file);

        InputStream is = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(is);

        assertEquals('a', reader.read());
        reader.skip(1);
        assertEquals('c', reader.read());

        assertTrue(reader.ready());
        assertEquals("123456789", reader.readLine());
        reader.readLine();
        assertEquals("abc", reader.readLine());

        IOUtil.close(is);
    }
}
