package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Writer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class JoinerTest extends AbstractTest {

    @Test
    public void test_001() throws IOException {
        String str = Joiner.defauLt().useForNull("NULL").append(false).append(0).append(3L).append(5d).append((Boolean) null).append((Double) null).toString();
        N.println(str);

        assertEquals("false, 0, 3, 5.0, NULL, NULL", str);

        Writer writer = IOUtil.newStringWriter();

        IOUtil.write(false, writer);
        IOUtil.write(0, writer);
        IOUtil.write(3L, writer);
        IOUtil.write(5d, writer);
        IOUtil.write((Boolean) null, writer);
        IOUtil.write((Double) null, writer);

        str = writer.toString();
        N.println(str);
        assertEquals("false035.0nullnull", str);

    }

}
