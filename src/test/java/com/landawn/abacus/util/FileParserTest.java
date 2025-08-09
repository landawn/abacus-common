/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.stream.Stream;

public class FileParserTest extends AbstractTest {

    @Test
    public void test_parser() throws Exception {
        Consumer<String> lineParser = line -> {
            if (line != null) {
                N.println(line);
            }
        };

        File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        while (!file.exists()) {
            N.sleep(1000);
        }

        List<String> lines = Stream.range(0, 1000).map(it -> Strings.uuid()).toList();
        IOUtil.writeLines(lines, file);

        IOUtil.forLines(file, lineParser);

        IOUtil.deleteIfExists(file);
    }
}
