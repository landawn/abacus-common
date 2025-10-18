/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class FilenameUtilsTestCase extends FileBasedTestCase {

    private static final String SEP = "" + File.separatorChar;
    private static final boolean WINDOWS = File.separatorChar == '\\';

    private final File testFile1;
    private final File testFile2;

    private final int testFile1Size;
    private final int testFile2Size;

    public FilenameUtilsTestCase() {
        testFile1 = new File(getTestDirectory(), "file1-test.txt");
        testFile2 = new File(getTestDirectory(), "file1a-test.txt");

        testFile1Size = (int) testFile1.length();
        testFile2Size = (int) testFile2.length();
    }

    /** @see junit.framework.TestCase#setUp() */
    @Override
    protected void setUp() throws Exception {
        getTestDirectory().mkdirs();
        createFile(testFile1, testFile1Size);
        createFile(testFile2, testFile2Size);
        FileUtils.deleteDirectory(getTestDirectory());
        getTestDirectory().mkdirs();
        createFile(testFile1, testFile1Size);
        createFile(testFile2, testFile2Size);
    }

    /** @see junit.framework.TestCase#tearDown() */
    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(getTestDirectory());
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNormalize() throws Exception {
        assertEquals(null, FilenameUtil.normalize(null));
        assertEquals(null, FilenameUtil.normalize(":"));
        assertEquals(null, FilenameUtil.normalize("1:\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.normalize("1:"));
        assertEquals(null, FilenameUtil.normalize("1:a"));
        assertEquals(null, FilenameUtil.normalize("\\\\\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.normalize("\\\\a"));

        assertEquals("a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("a\\b/c.txt"));
        assertEquals("" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("\\a\\b/c.txt"));
        assertEquals("C:" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("C:\\a\\b/c.txt"));
        assertEquals("" + SEP + "" + SEP + "server" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("\\\\server\\a\\b/c.txt"));
        assertEquals("~" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("~\\a\\b/c.txt"));
        assertEquals("~user" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalize("~user\\a\\b/c.txt"));

        assertEquals("a" + SEP + "c", FilenameUtil.normalize("a/b/../c"));
        assertEquals("c", FilenameUtil.normalize("a/b/../../c"));
        assertEquals("c" + SEP, FilenameUtil.normalize("a/b/../../c/"));
        assertEquals(null, FilenameUtil.normalize("a/b/../../../c"));
        assertEquals("a" + SEP, FilenameUtil.normalize("a/b/.."));
        assertEquals("a" + SEP, FilenameUtil.normalize("a/b/../"));
        assertEquals("", FilenameUtil.normalize("a/b/../.."));
        assertEquals("", FilenameUtil.normalize("a/b/../../"));
        assertEquals(null, FilenameUtil.normalize("a/b/../../.."));
        assertEquals("a" + SEP + "d", FilenameUtil.normalize("a/b/../c/../d"));
        assertEquals("a" + SEP + "d" + SEP, FilenameUtil.normalize("a/b/../c/../d/"));
        assertEquals("a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("a/b//d"));
        assertEquals("a" + SEP + "b" + SEP, FilenameUtil.normalize("a/b/././."));
        assertEquals("a" + SEP + "b" + SEP, FilenameUtil.normalize("a/b/./././"));
        assertEquals("a" + SEP, FilenameUtil.normalize("./a/"));
        assertEquals("a", FilenameUtil.normalize("./a"));
        assertEquals("", FilenameUtil.normalize("./"));
        assertEquals("", FilenameUtil.normalize("."));
        assertEquals(null, FilenameUtil.normalize("../a"));
        assertEquals(null, FilenameUtil.normalize(".."));
        assertEquals("", FilenameUtil.normalize(""));

        assertEquals(SEP + "a", FilenameUtil.normalize("/a"));
        assertEquals(SEP + "a" + SEP, FilenameUtil.normalize("/a/"));
        assertEquals(SEP + "a" + SEP + "c", FilenameUtil.normalize("/a/b/../c"));
        assertEquals(SEP + "c", FilenameUtil.normalize("/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("/a/b/../../../c"));
        assertEquals(SEP + "a" + SEP, FilenameUtil.normalize("/a/b/.."));
        assertEquals(SEP + "", FilenameUtil.normalize("/a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("/a/b/../../.."));
        assertEquals(SEP + "a" + SEP + "d", FilenameUtil.normalize("/a/b/../c/../d"));
        assertEquals(SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("/a/b//d"));
        assertEquals(SEP + "a" + SEP + "b" + SEP, FilenameUtil.normalize("/a/b/././."));
        assertEquals(SEP + "a", FilenameUtil.normalize("/./a"));
        assertEquals(SEP + "", FilenameUtil.normalize("/./"));
        assertEquals(SEP + "", FilenameUtil.normalize("/."));
        assertEquals(null, FilenameUtil.normalize("/../a"));
        assertEquals(null, FilenameUtil.normalize("/.."));
        assertEquals(SEP + "", FilenameUtil.normalize("/"));

        assertEquals("~" + SEP + "a", FilenameUtil.normalize("~/a"));
        assertEquals("~" + SEP + "a" + SEP, FilenameUtil.normalize("~/a/"));
        assertEquals("~" + SEP + "a" + SEP + "c", FilenameUtil.normalize("~/a/b/../c"));
        assertEquals("~" + SEP + "c", FilenameUtil.normalize("~/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("~/a/b/../../../c"));
        assertEquals("~" + SEP + "a" + SEP, FilenameUtil.normalize("~/a/b/.."));
        assertEquals("~" + SEP + "", FilenameUtil.normalize("~/a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("~/a/b/../../.."));
        assertEquals("~" + SEP + "a" + SEP + "d", FilenameUtil.normalize("~/a/b/../c/../d"));
        assertEquals("~" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("~/a/b//d"));
        assertEquals("~" + SEP + "a" + SEP + "b" + SEP, FilenameUtil.normalize("~/a/b/././."));
        assertEquals("~" + SEP + "a", FilenameUtil.normalize("~/./a"));
        assertEquals("~" + SEP, FilenameUtil.normalize("~/./"));
        assertEquals("~" + SEP, FilenameUtil.normalize("~/."));
        assertEquals(null, FilenameUtil.normalize("~/../a"));
        assertEquals(null, FilenameUtil.normalize("~/.."));
        assertEquals("~" + SEP, FilenameUtil.normalize("~/"));
        assertEquals("~" + SEP, FilenameUtil.normalize("~"));

        assertEquals("~user" + SEP + "a", FilenameUtil.normalize("~user/a"));
        assertEquals("~user" + SEP + "a" + SEP, FilenameUtil.normalize("~user/a/"));
        assertEquals("~user" + SEP + "a" + SEP + "c", FilenameUtil.normalize("~user/a/b/../c"));
        assertEquals("~user" + SEP + "c", FilenameUtil.normalize("~user/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("~user/a/b/../../../c"));
        assertEquals("~user" + SEP + "a" + SEP, FilenameUtil.normalize("~user/a/b/.."));
        assertEquals("~user" + SEP + "", FilenameUtil.normalize("~user/a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("~user/a/b/../../.."));
        assertEquals("~user" + SEP + "a" + SEP + "d", FilenameUtil.normalize("~user/a/b/../c/../d"));
        assertEquals("~user" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("~user/a/b//d"));
        assertEquals("~user" + SEP + "a" + SEP + "b" + SEP, FilenameUtil.normalize("~user/a/b/././."));
        assertEquals("~user" + SEP + "a", FilenameUtil.normalize("~user/./a"));
        assertEquals("~user" + SEP + "", FilenameUtil.normalize("~user/./"));
        assertEquals("~user" + SEP + "", FilenameUtil.normalize("~user/."));
        assertEquals(null, FilenameUtil.normalize("~user/../a"));
        assertEquals(null, FilenameUtil.normalize("~user/.."));
        assertEquals("~user" + SEP, FilenameUtil.normalize("~user/"));
        assertEquals("~user" + SEP, FilenameUtil.normalize("~user"));

        assertEquals("C:" + SEP + "a", FilenameUtil.normalize("C:/a"));
        assertEquals("C:" + SEP + "a" + SEP, FilenameUtil.normalize("C:/a/"));
        assertEquals("C:" + SEP + "a" + SEP + "c", FilenameUtil.normalize("C:/a/b/../c"));
        assertEquals("C:" + SEP + "c", FilenameUtil.normalize("C:/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("C:/a/b/../../../c"));
        assertEquals("C:" + SEP + "a" + SEP, FilenameUtil.normalize("C:/a/b/.."));
        assertEquals("C:" + SEP + "", FilenameUtil.normalize("C:/a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("C:/a/b/../../.."));
        assertEquals("C:" + SEP + "a" + SEP + "d", FilenameUtil.normalize("C:/a/b/../c/../d"));
        assertEquals("C:" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("C:/a/b//d"));
        assertEquals("C:" + SEP + "a" + SEP + "b" + SEP, FilenameUtil.normalize("C:/a/b/././."));
        assertEquals("C:" + SEP + "a", FilenameUtil.normalize("C:/./a"));
        assertEquals("C:" + SEP + "", FilenameUtil.normalize("C:/./"));
        assertEquals("C:" + SEP + "", FilenameUtil.normalize("C:/."));
        assertEquals(null, FilenameUtil.normalize("C:/../a"));
        assertEquals(null, FilenameUtil.normalize("C:/.."));
        assertEquals("C:" + SEP + "", FilenameUtil.normalize("C:/"));

        assertEquals("C:" + "a", FilenameUtil.normalize("C:a"));
        assertEquals("C:" + "a" + SEP, FilenameUtil.normalize("C:a/"));
        assertEquals("C:" + "a" + SEP + "c", FilenameUtil.normalize("C:a/b/../c"));
        assertEquals("C:" + "c", FilenameUtil.normalize("C:a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("C:a/b/../../../c"));
        assertEquals("C:" + "a" + SEP, FilenameUtil.normalize("C:a/b/.."));
        assertEquals("C:" + "", FilenameUtil.normalize("C:a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("C:a/b/../../.."));
        assertEquals("C:" + "a" + SEP + "d", FilenameUtil.normalize("C:a/b/../c/../d"));
        assertEquals("C:" + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("C:a/b//d"));
        assertEquals("C:" + "a" + SEP + "b" + SEP, FilenameUtil.normalize("C:a/b/././."));
        assertEquals("C:" + "a", FilenameUtil.normalize("C:./a"));
        assertEquals("C:" + "", FilenameUtil.normalize("C:./"));
        assertEquals("C:" + "", FilenameUtil.normalize("C:."));
        assertEquals(null, FilenameUtil.normalize("C:../a"));
        assertEquals(null, FilenameUtil.normalize("C:.."));
        assertEquals("C:" + "", FilenameUtil.normalize("C:"));

        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalize("//server/a"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP, FilenameUtil.normalize("//server/a/"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "c", FilenameUtil.normalize("//server/a/b/../c"));
        assertEquals(SEP + SEP + "server" + SEP + "c", FilenameUtil.normalize("//server/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalize("//server/a/b/../../../c"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP, FilenameUtil.normalize("//server/a/b/.."));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalize("//server/a/b/../.."));
        assertEquals(null, FilenameUtil.normalize("//server/a/b/../../.."));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "d", FilenameUtil.normalize("//server/a/b/../c/../d"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalize("//server/a/b//d"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "b" + SEP, FilenameUtil.normalize("//server/a/b/././."));
        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalize("//server/./a"));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalize("//server/./"));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalize("//server/."));
        assertEquals(null, FilenameUtil.normalize("//server/../a"));
        assertEquals(null, FilenameUtil.normalize("//server/.."));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalize("//server/"));
    }

    @Test
    public void testNormalizeUnixWin() throws Exception {

        // Normalize (Unix Separator)
        assertEquals("/a/c/", FilenameUtil.normalize("/a/b/../c/", true));
        assertEquals("/a/c/", FilenameUtil.normalize("\\a\\b\\..\\c\\", true));

        // Normalize (Windows Separator)
        assertEquals("\\a\\c\\", FilenameUtil.normalize("/a/b/../c/", false));
        assertEquals("\\a\\c\\", FilenameUtil.normalize("\\a\\b\\..\\c\\", false));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testNormalizeNoEndSeparator() throws Exception {
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator(null));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator(":"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("1:\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("1:"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("1:a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("\\\\\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("\\\\a"));

        assertEquals("a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("a\\b/c.txt"));
        assertEquals("" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("\\a\\b/c.txt"));
        assertEquals("C:" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("C:\\a\\b/c.txt"));
        assertEquals("" + SEP + "" + SEP + "server" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("\\\\server\\a\\b/c.txt"));
        assertEquals("~" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("~\\a\\b/c.txt"));
        assertEquals("~user" + SEP + "a" + SEP + "b" + SEP + "c.txt", FilenameUtil.normalizeNoEndSeparator("~user\\a\\b/c.txt"));

        assertEquals("a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("a/b/../c"));
        assertEquals("c", FilenameUtil.normalizeNoEndSeparator("a/b/../../c"));
        assertEquals("c", FilenameUtil.normalizeNoEndSeparator("a/b/../../c/"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("a/b/../../../c"));
        assertEquals("a", FilenameUtil.normalizeNoEndSeparator("a/b/.."));
        assertEquals("a", FilenameUtil.normalizeNoEndSeparator("a/b/../"));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator("a/b/../.."));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator("a/b/../../"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("a/b/../../.."));
        assertEquals("a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("a/b/../c/../d"));
        assertEquals("a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("a/b/../c/../d/"));
        assertEquals("a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("a/b//d"));
        assertEquals("a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("a/b/././."));
        assertEquals("a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("a/b/./././"));
        assertEquals("a", FilenameUtil.normalizeNoEndSeparator("./a/"));
        assertEquals("a", FilenameUtil.normalizeNoEndSeparator("./a"));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator("./"));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator("."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator(".."));
        assertEquals("", FilenameUtil.normalizeNoEndSeparator(""));

        assertEquals(SEP + "a", FilenameUtil.normalizeNoEndSeparator("/a"));
        assertEquals(SEP + "a", FilenameUtil.normalizeNoEndSeparator("/a/"));
        assertEquals(SEP + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("/a/b/../c"));
        assertEquals(SEP + "c", FilenameUtil.normalizeNoEndSeparator("/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("/a/b/../../../c"));
        assertEquals(SEP + "a", FilenameUtil.normalizeNoEndSeparator("/a/b/.."));
        assertEquals(SEP + "", FilenameUtil.normalizeNoEndSeparator("/a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("/a/b/../../.."));
        assertEquals(SEP + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("/a/b/../c/../d"));
        assertEquals(SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("/a/b//d"));
        assertEquals(SEP + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("/a/b/././."));
        assertEquals(SEP + "a", FilenameUtil.normalizeNoEndSeparator("/./a"));
        assertEquals(SEP + "", FilenameUtil.normalizeNoEndSeparator("/./"));
        assertEquals(SEP + "", FilenameUtil.normalizeNoEndSeparator("/."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("/../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("/.."));
        assertEquals(SEP + "", FilenameUtil.normalizeNoEndSeparator("/"));

        assertEquals("~" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~/a"));
        assertEquals("~" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~/a/"));
        assertEquals("~" + SEP + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("~/a/b/../c"));
        assertEquals("~" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("~/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~/a/b/../../../c"));
        assertEquals("~" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~/a/b/.."));
        assertEquals("~" + SEP + "", FilenameUtil.normalizeNoEndSeparator("~/a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~/a/b/../../.."));
        assertEquals("~" + SEP + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("~/a/b/../c/../d"));
        assertEquals("~" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("~/a/b//d"));
        assertEquals("~" + SEP + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("~/a/b/././."));
        assertEquals("~" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~/./a"));
        assertEquals("~" + SEP, FilenameUtil.normalizeNoEndSeparator("~/./"));
        assertEquals("~" + SEP, FilenameUtil.normalizeNoEndSeparator("~/."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~/../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~/.."));
        assertEquals("~" + SEP, FilenameUtil.normalizeNoEndSeparator("~/"));
        assertEquals("~" + SEP, FilenameUtil.normalizeNoEndSeparator("~"));

        assertEquals("~user" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~user/a"));
        assertEquals("~user" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~user/a/"));
        assertEquals("~user" + SEP + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("~user/a/b/../c"));
        assertEquals("~user" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("~user/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~user/a/b/../../../c"));
        assertEquals("~user" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~user/a/b/.."));
        assertEquals("~user" + SEP + "", FilenameUtil.normalizeNoEndSeparator("~user/a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~user/a/b/../../.."));
        assertEquals("~user" + SEP + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("~user/a/b/../c/../d"));
        assertEquals("~user" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("~user/a/b//d"));
        assertEquals("~user" + SEP + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("~user/a/b/././."));
        assertEquals("~user" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("~user/./a"));
        assertEquals("~user" + SEP + "", FilenameUtil.normalizeNoEndSeparator("~user/./"));
        assertEquals("~user" + SEP + "", FilenameUtil.normalizeNoEndSeparator("~user/."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~user/../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("~user/.."));
        assertEquals("~user" + SEP, FilenameUtil.normalizeNoEndSeparator("~user/"));
        assertEquals("~user" + SEP, FilenameUtil.normalizeNoEndSeparator("~user"));

        assertEquals("C:" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("C:/a"));
        assertEquals("C:" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("C:/a/"));
        assertEquals("C:" + SEP + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("C:/a/b/../c"));
        assertEquals("C:" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("C:/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:/a/b/../../../c"));
        assertEquals("C:" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("C:/a/b/.."));
        assertEquals("C:" + SEP + "", FilenameUtil.normalizeNoEndSeparator("C:/a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:/a/b/../../.."));
        assertEquals("C:" + SEP + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("C:/a/b/../c/../d"));
        assertEquals("C:" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("C:/a/b//d"));
        assertEquals("C:" + SEP + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("C:/a/b/././."));
        assertEquals("C:" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("C:/./a"));
        assertEquals("C:" + SEP + "", FilenameUtil.normalizeNoEndSeparator("C:/./"));
        assertEquals("C:" + SEP + "", FilenameUtil.normalizeNoEndSeparator("C:/."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:/../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:/.."));
        assertEquals("C:" + SEP + "", FilenameUtil.normalizeNoEndSeparator("C:/"));

        assertEquals("C:" + "a", FilenameUtil.normalizeNoEndSeparator("C:a"));
        assertEquals("C:" + "a", FilenameUtil.normalizeNoEndSeparator("C:a/"));
        assertEquals("C:" + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("C:a/b/../c"));
        assertEquals("C:" + "c", FilenameUtil.normalizeNoEndSeparator("C:a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:a/b/../../../c"));
        assertEquals("C:" + "a", FilenameUtil.normalizeNoEndSeparator("C:a/b/.."));
        assertEquals("C:" + "", FilenameUtil.normalizeNoEndSeparator("C:a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:a/b/../../.."));
        assertEquals("C:" + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("C:a/b/../c/../d"));
        assertEquals("C:" + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("C:a/b//d"));
        assertEquals("C:" + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("C:a/b/././."));
        assertEquals("C:" + "a", FilenameUtil.normalizeNoEndSeparator("C:./a"));
        assertEquals("C:" + "", FilenameUtil.normalizeNoEndSeparator("C:./"));
        assertEquals("C:" + "", FilenameUtil.normalizeNoEndSeparator("C:."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("C:.."));
        assertEquals("C:" + "", FilenameUtil.normalizeNoEndSeparator("C:"));

        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("//server/a"));
        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("//server/a/"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("//server/a/b/../c"));
        assertEquals(SEP + SEP + "server" + SEP + "c", FilenameUtil.normalizeNoEndSeparator("//server/a/b/../../c"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("//server/a/b/../../../c"));
        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("//server/a/b/.."));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalizeNoEndSeparator("//server/a/b/../.."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("//server/a/b/../../.."));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("//server/a/b/../c/../d"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "b" + SEP + "d", FilenameUtil.normalizeNoEndSeparator("//server/a/b//d"));
        assertEquals(SEP + SEP + "server" + SEP + "a" + SEP + "b", FilenameUtil.normalizeNoEndSeparator("//server/a/b/././."));
        assertEquals(SEP + SEP + "server" + SEP + "a", FilenameUtil.normalizeNoEndSeparator("//server/./a"));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalizeNoEndSeparator("//server/./"));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalizeNoEndSeparator("//server/."));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("//server/../a"));
        assertEquals(null, FilenameUtil.normalizeNoEndSeparator("//server/.."));
        assertEquals(SEP + SEP + "server" + SEP + "", FilenameUtil.normalizeNoEndSeparator("//server/"));
    }

    @Test
    public void testNormalizeNoEndSeparatorUnixWin() throws Exception {

        // Normalize (Unix Separator)
        assertEquals("/a/c", FilenameUtil.normalizeNoEndSeparator("/a/b/../c/", true));
        assertEquals("/a/c", FilenameUtil.normalizeNoEndSeparator("\\a\\b\\..\\c\\", true));

        // Normalize (Windows Separator)
        assertEquals("\\a\\c", FilenameUtil.normalizeNoEndSeparator("/a/b/../c/", false));
        assertEquals("\\a\\c", FilenameUtil.normalizeNoEndSeparator("\\a\\b\\..\\c\\", false));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testConcat() {
        assertEquals(null, FilenameUtil.concat("", null));
        assertEquals(null, FilenameUtil.concat(null, null));
        assertEquals(null, FilenameUtil.concat(null, ""));
        assertEquals(null, FilenameUtil.concat(null, "a"));
        assertEquals(SEP + "a", FilenameUtil.concat(null, "/a"));

        assertEquals(null, FilenameUtil.concat("", ":")); // invalid prefix
        assertEquals(null, FilenameUtil.concat(":", "")); // invalid prefix

        assertEquals("f" + SEP, FilenameUtil.concat("", "f/"));
        assertEquals("f", FilenameUtil.concat("", "f"));
        assertEquals("a" + SEP + "f" + SEP, FilenameUtil.concat("a/", "f/"));
        assertEquals("a" + SEP + "f", FilenameUtil.concat("a", "f"));
        assertEquals("a" + SEP + "b" + SEP + "f" + SEP, FilenameUtil.concat("a/b/", "f/"));
        assertEquals("a" + SEP + "b" + SEP + "f", FilenameUtil.concat("a/b", "f"));

        assertEquals("a" + SEP + "f" + SEP, FilenameUtil.concat("a/b/", "../f/"));
        assertEquals("a" + SEP + "f", FilenameUtil.concat("a/b", "../f"));
        assertEquals("a" + SEP + "c" + SEP + "g" + SEP, FilenameUtil.concat("a/b/../c/", "f/../g/"));
        assertEquals("a" + SEP + "c" + SEP + "g", FilenameUtil.concat("a/b/../c", "f/../g"));

        assertEquals("a" + SEP + "c.txt" + SEP + "f", FilenameUtil.concat("a/c.txt", "f"));

        assertEquals(SEP + "f" + SEP, FilenameUtil.concat("", "/f/"));
        assertEquals(SEP + "f", FilenameUtil.concat("", "/f"));
        assertEquals(SEP + "f" + SEP, FilenameUtil.concat("a/", "/f/"));
        assertEquals(SEP + "f", FilenameUtil.concat("a", "/f"));

        assertEquals(SEP + "c" + SEP + "d", FilenameUtil.concat("a/b/", "/c/d"));
        assertEquals("C:c" + SEP + "d", FilenameUtil.concat("a/b/", "C:c/d"));
        assertEquals("C:" + SEP + "c" + SEP + "d", FilenameUtil.concat("a/b/", "C:/c/d"));
        assertEquals("~" + SEP + "c" + SEP + "d", FilenameUtil.concat("a/b/", "~/c/d"));
        assertEquals("~user" + SEP + "c" + SEP + "d", FilenameUtil.concat("a/b/", "~user/c/d"));
        assertEquals("~" + SEP, FilenameUtil.concat("a/b/", "~"));
        assertEquals("~user" + SEP, FilenameUtil.concat("a/b/", "~user"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testSeparatorsToUnix() {
        assertEquals(null, FilenameUtil.separatorsToUnix(null));
        assertEquals("/a/b/c", FilenameUtil.separatorsToUnix("/a/b/c"));
        assertEquals("/a/b/c.txt", FilenameUtil.separatorsToUnix("/a/b/c.txt"));
        assertEquals("/a/b/c", FilenameUtil.separatorsToUnix("/a/b\\c"));
        assertEquals("/a/b/c", FilenameUtil.separatorsToUnix("\\a\\b\\c"));
        assertEquals("D:/a/b/c", FilenameUtil.separatorsToUnix("D:\\a\\b\\c"));
    }

    @Test
    public void testSeparatorsToWindows() {
        assertEquals(null, FilenameUtil.separatorsToWindows(null));
        assertEquals("\\a\\b\\c", FilenameUtil.separatorsToWindows("\\a\\b\\c"));
        assertEquals("\\a\\b\\c.txt", FilenameUtil.separatorsToWindows("\\a\\b\\c.txt"));
        assertEquals("\\a\\b\\c", FilenameUtil.separatorsToWindows("\\a\\b/c"));
        assertEquals("\\a\\b\\c", FilenameUtil.separatorsToWindows("/a/b/c"));
        assertEquals("D:\\a\\b\\c", FilenameUtil.separatorsToWindows("D:/a/b/c"));
    }

    @Test
    public void testSeparatorsToSystem() {
        if (WINDOWS) {
            assertEquals(null, FilenameUtil.separatorsToSystem(null));
            assertEquals("\\a\\b\\c", FilenameUtil.separatorsToSystem("\\a\\b\\c"));
            assertEquals("\\a\\b\\c.txt", FilenameUtil.separatorsToSystem("\\a\\b\\c.txt"));
            assertEquals("\\a\\b\\c", FilenameUtil.separatorsToSystem("\\a\\b/c"));
            assertEquals("\\a\\b\\c", FilenameUtil.separatorsToSystem("/a/b/c"));
            assertEquals("D:\\a\\b\\c", FilenameUtil.separatorsToSystem("D:/a/b/c"));
        } else {
            assertEquals(null, FilenameUtil.separatorsToSystem(null));
            assertEquals("/a/b/c", FilenameUtil.separatorsToSystem("/a/b/c"));
            assertEquals("/a/b/c.txt", FilenameUtil.separatorsToSystem("/a/b/c.txt"));
            assertEquals("/a/b/c", FilenameUtil.separatorsToSystem("/a/b\\c"));
            assertEquals("/a/b/c", FilenameUtil.separatorsToSystem("\\a\\b\\c"));
            assertEquals("D:/a/b/c", FilenameUtil.separatorsToSystem("D:\\a\\b\\c"));
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void testGetPrefixLength() {
        assertEquals(-1, FilenameUtil.getPrefixLength(null));
        assertEquals(-1, FilenameUtil.getPrefixLength(":"));
        assertEquals(-1, FilenameUtil.getPrefixLength("1:\\a\\b\\c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("1:"));
        assertEquals(-1, FilenameUtil.getPrefixLength("1:a"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\\\a\\b\\c.txt"));
        assertEquals(-1, FilenameUtil.getPrefixLength("\\\\a"));

        assertEquals(0, FilenameUtil.getPrefixLength(""));
        assertEquals(1, FilenameUtil.getPrefixLength("\\"));
        assertEquals(2, FilenameUtil.getPrefixLength("C:"));
        assertEquals(3, FilenameUtil.getPrefixLength("C:\\"));
        assertEquals(9, FilenameUtil.getPrefixLength("//server/"));
        assertEquals(2, FilenameUtil.getPrefixLength("~"));
        assertEquals(2, FilenameUtil.getPrefixLength("~/"));
        assertEquals(6, FilenameUtil.getPrefixLength("~user"));
        assertEquals(6, FilenameUtil.getPrefixLength("~user/"));

        assertEquals(0, FilenameUtil.getPrefixLength("a\\b\\c.txt"));
        assertEquals(1, FilenameUtil.getPrefixLength("\\a\\b\\c.txt"));
        assertEquals(2, FilenameUtil.getPrefixLength("C:a\\b\\c.txt"));
        assertEquals(3, FilenameUtil.getPrefixLength("C:\\a\\b\\c.txt"));
        assertEquals(9, FilenameUtil.getPrefixLength("\\\\server\\a\\b\\c.txt"));

        assertEquals(0, FilenameUtil.getPrefixLength("a/b/c.txt"));
        assertEquals(1, FilenameUtil.getPrefixLength("/a/b/c.txt"));
        assertEquals(3, FilenameUtil.getPrefixLength("C:/a/b/c.txt"));
        assertEquals(9, FilenameUtil.getPrefixLength("//server/a/b/c.txt"));
        assertEquals(2, FilenameUtil.getPrefixLength("~/a/b/c.txt"));
        assertEquals(6, FilenameUtil.getPrefixLength("~user/a/b/c.txt"));

        assertEquals(0, FilenameUtil.getPrefixLength("a\\b\\c.txt"));
        assertEquals(1, FilenameUtil.getPrefixLength("\\a\\b\\c.txt"));
        assertEquals(2, FilenameUtil.getPrefixLength("~\\a\\b\\c.txt"));
        assertEquals(6, FilenameUtil.getPrefixLength("~user\\a\\b\\c.txt"));
    }

    @Test
    public void testIndexOfLastSeparator() {
        assertEquals(-1, FilenameUtil.indexOfLastSeparator(null));
        assertEquals(-1, FilenameUtil.indexOfLastSeparator("noseparator.inthispath"));
        assertEquals(3, FilenameUtil.indexOfLastSeparator("a/b/c"));
        assertEquals(3, FilenameUtil.indexOfLastSeparator("a\\b\\c"));
    }

    @Test
    public void testIndexOfExtension() {
        assertEquals(-1, FilenameUtil.indexOfExtension(null));
        assertEquals(-1, FilenameUtil.indexOfExtension("file"));
        assertEquals(4, FilenameUtil.indexOfExtension("file.txt"));
        assertEquals(13, FilenameUtil.indexOfExtension("a.txt/b.txt/c.txt"));
        assertEquals(-1, FilenameUtil.indexOfExtension("a/b/c"));
        assertEquals(-1, FilenameUtil.indexOfExtension("a\\b\\c"));
        assertEquals(-1, FilenameUtil.indexOfExtension("a/b.notextension/c"));
        assertEquals(-1, FilenameUtil.indexOfExtension("a\\b.notextension\\c"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testGetPrefix() {
        assertEquals(null, FilenameUtil.getPrefix(null));
        assertEquals(null, FilenameUtil.getPrefix(":"));
        assertEquals(null, FilenameUtil.getPrefix("1:\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.getPrefix("1:"));
        assertEquals(null, FilenameUtil.getPrefix("1:a"));
        assertEquals(null, FilenameUtil.getPrefix("\\\\\\a\\b\\c.txt"));
        assertEquals(null, FilenameUtil.getPrefix("\\\\a"));

        assertEquals("", FilenameUtil.getPrefix(""));
        assertEquals("\\", FilenameUtil.getPrefix("\\"));
        assertEquals("C:", FilenameUtil.getPrefix("C:"));
        assertEquals("C:\\", FilenameUtil.getPrefix("C:\\"));
        assertEquals("//server/", FilenameUtil.getPrefix("//server/"));
        assertEquals("~/", FilenameUtil.getPrefix("~"));
        assertEquals("~/", FilenameUtil.getPrefix("~/"));
        assertEquals("~user/", FilenameUtil.getPrefix("~user"));
        assertEquals("~user/", FilenameUtil.getPrefix("~user/"));

        assertEquals("", FilenameUtil.getPrefix("a\\b\\c.txt"));
        assertEquals("\\", FilenameUtil.getPrefix("\\a\\b\\c.txt"));
        assertEquals("C:\\", FilenameUtil.getPrefix("C:\\a\\b\\c.txt"));
        assertEquals("\\\\server\\", FilenameUtil.getPrefix("\\\\server\\a\\b\\c.txt"));

        assertEquals("", FilenameUtil.getPrefix("a/b/c.txt"));
        assertEquals("/", FilenameUtil.getPrefix("/a/b/c.txt"));
        assertEquals("C:/", FilenameUtil.getPrefix("C:/a/b/c.txt"));
        assertEquals("//server/", FilenameUtil.getPrefix("//server/a/b/c.txt"));
        assertEquals("~/", FilenameUtil.getPrefix("~/a/b/c.txt"));
        assertEquals("~user/", FilenameUtil.getPrefix("~user/a/b/c.txt"));

        assertEquals("", FilenameUtil.getPrefix("a\\b\\c.txt"));
        assertEquals("\\", FilenameUtil.getPrefix("\\a\\b\\c.txt"));
        assertEquals("~\\", FilenameUtil.getPrefix("~\\a\\b\\c.txt"));
        assertEquals("~user\\", FilenameUtil.getPrefix("~user\\a\\b\\c.txt"));
    }

    @Test
    public void testGetPath() {
        assertEquals(null, FilenameUtil.getPath(null));
        assertEquals("", FilenameUtil.getPath("noseparator.inthispath"));
        assertEquals("", FilenameUtil.getPath("/noseparator.inthispath"));
        assertEquals("", FilenameUtil.getPath("\\noseparator.inthispath"));
        assertEquals("a/b/", FilenameUtil.getPath("a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("a/b/c"));
        assertEquals("a/b/c/", FilenameUtil.getPath("a/b/c/"));
        assertEquals("a\\b\\", FilenameUtil.getPath("a\\b\\c"));

        assertEquals(null, FilenameUtil.getPath(":"));
        assertEquals(null, FilenameUtil.getPath("1:/a/b/c.txt"));
        assertEquals(null, FilenameUtil.getPath("1:"));
        assertEquals(null, FilenameUtil.getPath("1:a"));
        assertEquals(null, FilenameUtil.getPath("///a/b/c.txt"));
        assertEquals(null, FilenameUtil.getPath("//a"));

        assertEquals("", FilenameUtil.getPath(""));
        assertEquals("", FilenameUtil.getPath("C:"));
        assertEquals("", FilenameUtil.getPath("C:/"));
        assertEquals("", FilenameUtil.getPath("//server/"));
        assertEquals("", FilenameUtil.getPath("~"));
        assertEquals("", FilenameUtil.getPath("~/"));
        assertEquals("", FilenameUtil.getPath("~user"));
        assertEquals("", FilenameUtil.getPath("~user/"));

        assertEquals("a/b/", FilenameUtil.getPath("a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("/a/b/c.txt"));
        assertEquals("", FilenameUtil.getPath("C:a"));
        assertEquals("a/b/", FilenameUtil.getPath("C:a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("C:/a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("//server/a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("~/a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getPath("~user/a/b/c.txt"));
    }

    @Test
    public void testGetPathNoEndSeparator() {
        assertEquals(null, FilenameUtil.getPath(null));
        assertEquals("", FilenameUtil.getPath("noseparator.inthispath"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("/noseparator.inthispath"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("\\noseparator.inthispath"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("a/b/c"));
        assertEquals("a/b/c", FilenameUtil.getPathNoEndSeparator("a/b/c/"));
        assertEquals("a\\b", FilenameUtil.getPathNoEndSeparator("a\\b\\c"));

        assertEquals(null, FilenameUtil.getPathNoEndSeparator(":"));
        assertEquals(null, FilenameUtil.getPathNoEndSeparator("1:/a/b/c.txt"));
        assertEquals(null, FilenameUtil.getPathNoEndSeparator("1:"));
        assertEquals(null, FilenameUtil.getPathNoEndSeparator("1:a"));
        assertEquals(null, FilenameUtil.getPathNoEndSeparator("///a/b/c.txt"));
        assertEquals(null, FilenameUtil.getPathNoEndSeparator("//a"));

        assertEquals("", FilenameUtil.getPathNoEndSeparator(""));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("C:"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("C:/"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("//server/"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("~"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("~/"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("~user"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("~user/"));

        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("/a/b/c.txt"));
        assertEquals("", FilenameUtil.getPathNoEndSeparator("C:a"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("C:a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("C:/a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("//server/a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("~/a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getPathNoEndSeparator("~user/a/b/c.txt"));
    }

    @Test
    public void testGetFullPath() {
        assertEquals(null, FilenameUtil.getFullPath(null));
        assertEquals("", FilenameUtil.getFullPath("noseparator.inthispath"));
        assertEquals("a/b/", FilenameUtil.getFullPath("a/b/c.txt"));
        assertEquals("a/b/", FilenameUtil.getFullPath("a/b/c"));
        assertEquals("a/b/c/", FilenameUtil.getFullPath("a/b/c/"));
        assertEquals("a\\b\\", FilenameUtil.getFullPath("a\\b\\c"));

        assertEquals(null, FilenameUtil.getFullPath(":"));
        assertEquals(null, FilenameUtil.getFullPath("1:/a/b/c.txt"));
        assertEquals(null, FilenameUtil.getFullPath("1:"));
        assertEquals(null, FilenameUtil.getFullPath("1:a"));
        assertEquals(null, FilenameUtil.getFullPath("///a/b/c.txt"));
        assertEquals(null, FilenameUtil.getFullPath("//a"));

        assertEquals("", FilenameUtil.getFullPath(""));
        assertEquals("C:", FilenameUtil.getFullPath("C:"));
        assertEquals("C:/", FilenameUtil.getFullPath("C:/"));
        assertEquals("//server/", FilenameUtil.getFullPath("//server/"));
        assertEquals("~/", FilenameUtil.getFullPath("~"));
        assertEquals("~/", FilenameUtil.getFullPath("~/"));
        assertEquals("~user/", FilenameUtil.getFullPath("~user"));
        assertEquals("~user/", FilenameUtil.getFullPath("~user/"));

        assertEquals("a/b/", FilenameUtil.getFullPath("a/b/c.txt"));
        assertEquals("/a/b/", FilenameUtil.getFullPath("/a/b/c.txt"));
        assertEquals("C:", FilenameUtil.getFullPath("C:a"));
        assertEquals("C:a/b/", FilenameUtil.getFullPath("C:a/b/c.txt"));
        assertEquals("C:/a/b/", FilenameUtil.getFullPath("C:/a/b/c.txt"));
        assertEquals("//server/a/b/", FilenameUtil.getFullPath("//server/a/b/c.txt"));
        assertEquals("~/a/b/", FilenameUtil.getFullPath("~/a/b/c.txt"));
        assertEquals("~user/a/b/", FilenameUtil.getFullPath("~user/a/b/c.txt"));
    }

    @Test
    public void testGetFullPathNoEndSeparator() {
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator(null));
        assertEquals("", FilenameUtil.getFullPathNoEndSeparator("noseparator.inthispath"));
        assertEquals("a/b", FilenameUtil.getFullPathNoEndSeparator("a/b/c.txt"));
        assertEquals("a/b", FilenameUtil.getFullPathNoEndSeparator("a/b/c"));
        assertEquals("a/b/c", FilenameUtil.getFullPathNoEndSeparator("a/b/c/"));
        assertEquals("a\\b", FilenameUtil.getFullPathNoEndSeparator("a\\b\\c"));

        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator(":"));
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator("1:/a/b/c.txt"));
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator("1:"));
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator("1:a"));
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator("///a/b/c.txt"));
        assertEquals(null, FilenameUtil.getFullPathNoEndSeparator("//a"));

        assertEquals("", FilenameUtil.getFullPathNoEndSeparator(""));
        assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:"));
        assertEquals("C:/", FilenameUtil.getFullPathNoEndSeparator("C:/"));
        assertEquals("//server/", FilenameUtil.getFullPathNoEndSeparator("//server/"));
        assertEquals("~", FilenameUtil.getFullPathNoEndSeparator("~"));
        assertEquals("~/", FilenameUtil.getFullPathNoEndSeparator("~/"));
        assertEquals("~user", FilenameUtil.getFullPathNoEndSeparator("~user"));
        assertEquals("~user/", FilenameUtil.getFullPathNoEndSeparator("~user/"));

        assertEquals("a/b", FilenameUtil.getFullPathNoEndSeparator("a/b/c.txt"));
        assertEquals("/a/b", FilenameUtil.getFullPathNoEndSeparator("/a/b/c.txt"));
        assertEquals("C:", FilenameUtil.getFullPathNoEndSeparator("C:a"));
        assertEquals("C:a/b", FilenameUtil.getFullPathNoEndSeparator("C:a/b/c.txt"));
        assertEquals("C:/a/b", FilenameUtil.getFullPathNoEndSeparator("C:/a/b/c.txt"));
        assertEquals("//server/a/b", FilenameUtil.getFullPathNoEndSeparator("//server/a/b/c.txt"));
        assertEquals("~/a/b", FilenameUtil.getFullPathNoEndSeparator("~/a/b/c.txt"));
        assertEquals("~user/a/b", FilenameUtil.getFullPathNoEndSeparator("~user/a/b/c.txt"));
    }

    @Test
    public void testGetFullPathNoEndSeparator_IO_248() {

        // Test single separator
        assertEquals("/", FilenameUtil.getFullPathNoEndSeparator("/"));
        assertEquals("\\", FilenameUtil.getFullPathNoEndSeparator("\\"));

        // Test one level directory
        assertEquals("/", FilenameUtil.getFullPathNoEndSeparator("/abc"));
        assertEquals("\\", FilenameUtil.getFullPathNoEndSeparator("\\abc"));

        // Test one level directory
        assertEquals("/abc", FilenameUtil.getFullPathNoEndSeparator("/abc/xyz"));
        assertEquals("\\abc", FilenameUtil.getFullPathNoEndSeparator("\\abc\\xyz"));
    }

    @Test
    public void testGetName() {
        assertEquals(null, FilenameUtil.getName(null));
        assertEquals("noseparator.inthispath", FilenameUtil.getName("noseparator.inthispath"));
        assertEquals("c.txt", FilenameUtil.getName("a/b/c.txt"));
        assertEquals("c", FilenameUtil.getName("a/b/c"));
        assertEquals("", FilenameUtil.getName("a/b/c/"));
        assertEquals("c", FilenameUtil.getName("a\\b\\c"));
    }

    @Test
    public void testGetBaseName() {
        assertEquals(null, FilenameUtil.getBaseName(null));
        assertEquals("noseparator", FilenameUtil.getBaseName("noseparator.inthispath"));
        assertEquals("c", FilenameUtil.getBaseName("a/b/c.txt"));
        assertEquals("c", FilenameUtil.getBaseName("a/b/c"));
        assertEquals("", FilenameUtil.getBaseName("a/b/c/"));
        assertEquals("c", FilenameUtil.getBaseName("a\\b\\c"));
        assertEquals("file.txt", FilenameUtil.getBaseName("file.txt.bak"));
    }

    //    public void testGetExtension() {
    //        assertEquals(null, FilenameUtil.getExtension(null));
    //        assertEquals("ext", FilenameUtil.getExtension("file.ext"));
    //        assertEquals("", FilenameUtil.getExtension("README"));
    //        assertEquals("com", FilenameUtil.getExtension("domain.dot.com"));
    //        assertEquals("jpeg", FilenameUtil.getExtension("image.jpeg"));
    //        assertEquals("", FilenameUtil.getExtension("a.b/c"));
    //        assertEquals("txt", FilenameUtil.getExtension("a.b/c.txt"));
    //        assertEquals("", FilenameUtil.getExtension("a/b/c"));
    //        assertEquals("", FilenameUtil.getExtension("a.b\\c"));
    //        assertEquals("txt", FilenameUtil.getExtension("a.b\\c.txt"));
    //        assertEquals("", FilenameUtil.getExtension("a\\b\\c"));
    //        assertEquals("", FilenameUtil.getExtension("C:\\temp\\foo.bar\\README"));
    //        assertEquals("ext", FilenameUtil.getExtension("../filename.ext"));
    //    }

    @Test
    public void testRemoveExtension() {
        assertEquals(null, FilenameUtil.removeExtension(null));
        assertEquals("file", FilenameUtil.removeExtension("file.ext"));
        assertEquals("README", FilenameUtil.removeExtension("README"));
        assertEquals("domain.dot", FilenameUtil.removeExtension("domain.dot.com"));
        assertEquals("image", FilenameUtil.removeExtension("image.jpeg"));
        assertEquals("a.b/c", FilenameUtil.removeExtension("a.b/c"));
        assertEquals("a.b/c", FilenameUtil.removeExtension("a.b/c.txt"));
        assertEquals("a/b/c", FilenameUtil.removeExtension("a/b/c"));
        assertEquals("a.b\\c", FilenameUtil.removeExtension("a.b\\c"));
        assertEquals("a.b\\c", FilenameUtil.removeExtension("a.b\\c.txt"));
        assertEquals("a\\b\\c", FilenameUtil.removeExtension("a\\b\\c"));
        assertEquals("C:\\temp\\foo.bar\\README", FilenameUtil.removeExtension("C:\\temp\\foo.bar\\README"));
        assertEquals("../filename", FilenameUtil.removeExtension("../filename.ext"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testEquals() {
        assertTrue(FilenameUtil.equals(null, null));
        assertFalse(FilenameUtil.equals(null, ""));
        assertFalse(FilenameUtil.equals("", null));
        assertTrue(FilenameUtil.equals("", ""));
        assertTrue(FilenameUtil.equals("file.txt", "file.txt"));
        assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT"));
        assertFalse(FilenameUtil.equals("a\\b\\file.txt", "a/b/file.txt"));
    }

    @Test
    public void testEqualsOnSystem() {
        assertTrue(FilenameUtil.equalsOnSystem(null, null));
        assertFalse(FilenameUtil.equalsOnSystem(null, ""));
        assertFalse(FilenameUtil.equalsOnSystem("", null));
        assertTrue(FilenameUtil.equalsOnSystem("", ""));
        assertTrue(FilenameUtil.equalsOnSystem("file.txt", "file.txt"));
        assertEquals(WINDOWS, FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT"));
        assertFalse(FilenameUtil.equalsOnSystem("a\\b\\file.txt", "a/b/file.txt"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testEqualsNormalized() {
        assertTrue(FilenameUtil.equalsNormalized(null, null));
        assertFalse(FilenameUtil.equalsNormalized(null, ""));
        assertFalse(FilenameUtil.equalsNormalized("", null));
        assertTrue(FilenameUtil.equalsNormalized("", ""));
        assertTrue(FilenameUtil.equalsNormalized("file.txt", "file.txt"));
        assertFalse(FilenameUtil.equalsNormalized("file.txt", "FILE.TXT"));
        assertTrue(FilenameUtil.equalsNormalized("a\\b\\file.txt", "a/b/file.txt"));
        assertFalse(FilenameUtil.equalsNormalized("a/b/", "a/b"));
    }

    @Test
    public void testEqualsNormalizedOnSystem() {
        assertTrue(FilenameUtil.equalsNormalizedOnSystem(null, null));
        assertFalse(FilenameUtil.equalsNormalizedOnSystem(null, ""));
        assertFalse(FilenameUtil.equalsNormalizedOnSystem("", null));
        assertTrue(FilenameUtil.equalsNormalizedOnSystem("", ""));
        assertTrue(FilenameUtil.equalsNormalizedOnSystem("file.txt", "file.txt"));
        assertEquals(WINDOWS, FilenameUtil.equalsNormalizedOnSystem("file.txt", "FILE.TXT"));
        assertTrue(FilenameUtil.equalsNormalizedOnSystem("a\\b\\file.txt", "a/b/file.txt"));
        assertFalse(FilenameUtil.equalsNormalizedOnSystem("a/b/", "a/b"));
    }

    @Test
    public void testEqualsNormalizedError_IO_128() {
        try {
            FilenameUtil.equalsNormalizedOnSystem("//file.txt", "file.txt");
            fail("Invalid normalized first file");
        } catch (NullPointerException e) {
            // expected result
        }
        try {
            FilenameUtil.equalsNormalizedOnSystem("file.txt", "//file.txt");
            fail("Invalid normalized second file");
        } catch (NullPointerException e) {
            // expected result
        }
        try {
            FilenameUtil.equalsNormalizedOnSystem("//file.txt", "//file.txt");
            fail("Invalid normalized both filse");
        } catch (NullPointerException e) {
            // expected result
        }
    }

    @Test
    public void testEquals_fullControl() {
        assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", true, IOCase.SENSITIVE));
        assertTrue(FilenameUtil.equals("file.txt", "FILE.TXT", true, IOCase.INSENSITIVE));
        assertEquals(WINDOWS, FilenameUtil.equals("file.txt", "FILE.TXT", true, IOCase.SYSTEM));
        assertFalse(FilenameUtil.equals("file.txt", "FILE.TXT", true, null));
    }

    //-----------------------------------------------------------------------
    @Test
    public void testIsExtension() {
        assertFalse(FilenameUtil.isExtension(null, (String) null));
        assertFalse(FilenameUtil.isExtension("file.txt", (String) null));
        assertTrue(FilenameUtil.isExtension("file", (String) null));
        assertFalse(FilenameUtil.isExtension("file.txt", ""));
        assertTrue(FilenameUtil.isExtension("file", ""));
        assertTrue(FilenameUtil.isExtension("file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("file.txt", "rtf"));

        assertFalse(FilenameUtil.isExtension("a/b/file.txt", (String) null));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", ""));
        assertTrue(FilenameUtil.isExtension("a/b/file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", "rtf"));

        assertFalse(FilenameUtil.isExtension("a.b/file.txt", (String) null));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", ""));
        assertTrue(FilenameUtil.isExtension("a.b/file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", "rtf"));

        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", (String) null));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", ""));
        assertTrue(FilenameUtil.isExtension("a\\b\\file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", "rtf"));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", (String) null));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", ""));
        assertTrue(FilenameUtil.isExtension("a.b\\file.txt", "txt"));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", "rtf"));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", "TXT"));
    }

    @Test
    public void testIsExtensionArray() {
        assertFalse(FilenameUtil.isExtension(null, (String[]) null));
        assertFalse(FilenameUtil.isExtension("file.txt", (String[]) null));
        assertTrue(FilenameUtil.isExtension("file", (String[]) null));
        assertFalse(FilenameUtil.isExtension("file.txt", new String[0]));
        assertTrue(FilenameUtil.isExtension("file.txt", new String[] { "txt" }));
        assertFalse(FilenameUtil.isExtension("file.txt", new String[] { "rtf" }));
        assertTrue(FilenameUtil.isExtension("file", new String[] { "rtf", "" }));
        assertTrue(FilenameUtil.isExtension("file.txt", new String[] { "rtf", "txt" }));

        assertFalse(FilenameUtil.isExtension("a/b/file.txt", (String[]) null));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", new String[0]));
        assertTrue(FilenameUtil.isExtension("a/b/file.txt", new String[] { "txt" }));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", new String[] { "rtf" }));
        assertTrue(FilenameUtil.isExtension("a/b/file.txt", new String[] { "rtf", "txt" }));

        assertFalse(FilenameUtil.isExtension("a.b/file.txt", (String[]) null));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", new String[0]));
        assertTrue(FilenameUtil.isExtension("a.b/file.txt", new String[] { "txt" }));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", new String[] { "rtf" }));
        assertTrue(FilenameUtil.isExtension("a.b/file.txt", new String[] { "rtf", "txt" }));

        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", (String[]) null));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", new String[0]));
        assertTrue(FilenameUtil.isExtension("a\\b\\file.txt", new String[] { "txt" }));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", new String[] { "rtf" }));
        assertTrue(FilenameUtil.isExtension("a\\b\\file.txt", new String[] { "rtf", "txt" }));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", (String[]) null));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new String[0]));
        assertTrue(FilenameUtil.isExtension("a.b\\file.txt", new String[] { "txt" }));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new String[] { "rtf" }));
        assertTrue(FilenameUtil.isExtension("a.b\\file.txt", new String[] { "rtf", "txt" }));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new String[] { "TXT" }));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new String[] { "TXT", "RTF" }));
    }

    @Test
    public void testIsExtensionCollection() {
        assertFalse(FilenameUtil.isExtension(null, (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("file.txt", (Collection<String>) null));
        assertTrue(FilenameUtil.isExtension("file", (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("file.txt", new ArrayList<>()));
        assertTrue(FilenameUtil.isExtension("file.txt", new ArrayList<>(Arrays.asList("txt"))));
        assertFalse(FilenameUtil.isExtension("file.txt", new ArrayList<>(Arrays.asList("rtf"))));
        assertTrue(FilenameUtil.isExtension("file", new ArrayList<>(Arrays.asList("rtf", ""))));
        assertTrue(FilenameUtil.isExtension("file.txt", new ArrayList<>(Arrays.asList("rtf", "txt"))));

        assertFalse(FilenameUtil.isExtension("a/b/file.txt", (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", new ArrayList<>()));
        assertTrue(FilenameUtil.isExtension("a/b/file.txt", new ArrayList<>(Arrays.asList("txt"))));
        assertFalse(FilenameUtil.isExtension("a/b/file.txt", new ArrayList<>(Arrays.asList("rtf"))));
        assertTrue(FilenameUtil.isExtension("a/b/file.txt", new ArrayList<>(Arrays.asList("rtf", "txt"))));

        assertFalse(FilenameUtil.isExtension("a.b/file.txt", (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", new ArrayList<>()));
        assertTrue(FilenameUtil.isExtension("a.b/file.txt", new ArrayList<>(Arrays.asList("txt"))));
        assertFalse(FilenameUtil.isExtension("a.b/file.txt", new ArrayList<>(Arrays.asList("rtf"))));
        assertTrue(FilenameUtil.isExtension("a.b/file.txt", new ArrayList<>(Arrays.asList("rtf", "txt"))));

        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", new ArrayList<>()));
        assertTrue(FilenameUtil.isExtension("a\\b\\file.txt", new ArrayList<>(Arrays.asList("txt"))));
        assertFalse(FilenameUtil.isExtension("a\\b\\file.txt", new ArrayList<>(Arrays.asList("rtf"))));
        assertTrue(FilenameUtil.isExtension("a\\b\\file.txt", new ArrayList<>(Arrays.asList("rtf", "txt"))));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", (Collection<String>) null));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>()));
        assertTrue(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>(Arrays.asList("txt"))));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>(Arrays.asList("rtf"))));
        assertTrue(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>(Arrays.asList("rtf", "txt"))));

        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>(Arrays.asList("TXT"))));
        assertFalse(FilenameUtil.isExtension("a.b\\file.txt", new ArrayList<>(Arrays.asList("TXT", "RTF"))));
    }

}
