package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class StringWriterTest extends AbstractTest {

    @Test
    public void test_0() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write('c');
        writer.write(new char[] { '1', 'a' });
        writer.write("abc");

        writer.write("abc", 2, 1);

        writer.append('c');
        writer.append("abc");
        N.println(writer.stringBuilder());

        writer.append("abc", 1, 2);

        N.println(writer.stringBuilder());

        assertEquals("c1aabcccabcb", writer.toString());

        StringWriter writer2 = new StringWriter(2);

        writer2.write("c1aabcccabcb");

        assertEquals(writer.toString(), writer2.toString());
    }
}
