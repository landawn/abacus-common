package com.landawn.abacus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.stream.Stream;

public class TestHelper {

    @Test
    public void test_changeTestMethodClassToPublic() throws IOException {
        final File parentPath = new File("./src/generated-test/");

        Stream.listFiles(parentPath, true) //
                .filter(file -> file.isFile() && file.getName().endsWith(".java")
                        && Strings.containsAnyIgnoreCase(file.getAbsolutePath(), "claude", "gemini", "chatgpt"))
                .peek(Fn.println())
                .forEach(file -> {
                    List<String> lines = IOUtil.readAllLines(file);

                    boolean changed = false;
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);

                        //    if (line.startsWith("public class ") && line.contains("TestBase")) {
                        //        if (IntStream.range(0, i).mapToObj(lines::get).noneMatch(s -> s.startsWith("import com.landawn.abacus.TestBase;"))) {
                        //            for (int j = i - 1; j > 0; j--) {
                        //                if (lines.get(j).startsWith("import ")) {
                        //                    lines.set(j, lines.get(j) + IOUtil.LINE_SEPARATOR + IOUtil.LINE_SEPARATOR + "import com.landawn.abacus.TestBase;"
                        //                            + IOUtil.LINE_SEPARATOR);
                        //                    break;
                        //                }
                        //            }
                        //        }
                        //        changed = true;
                        //    }  

                        if (line.startsWith("public class ") && !line.contains("TestBase")) {
                            for (int j = i - 1; j > 0; j--) {
                                if (lines.get(j).startsWith("import ")) {
                                    lines.set(j, lines.get(j) + IOUtil.LINE_SEPARATOR + IOUtil.LINE_SEPARATOR + "import com.landawn.abacus.TestBase;"
                                            + IOUtil.LINE_SEPARATOR);
                                    break;
                                }
                            }

                            lines.set(i, Strings.replaceFirst(line, " {", " extends TestBase {"));
                            changed = true;
                        } else if (Strings.startsWithIgnoreCase(line, "    void ")) {
                            lines.set(i, line.replaceFirst("    void ", "    public void "));
                            changed = true;
                        } else if (Strings.startsWithIgnoreCase(line, "        void ")) {
                            lines.set(i, line.replaceFirst("        void ", "        public void "));
                            changed = true;
                        } else if (Strings.startsWithIgnoreCase(line, "    class ")) {
                            lines.set(i, line.replaceFirst("    class ", "    public static class "));
                            changed = true;
                        } else if (Strings.startsWithIgnoreCase(line, "class ")) {
                            lines.set(i, line.replaceFirst("class ", "public class "));
                            changed = true;
                        } else if ((Strings.startsWithIgnoreCase(line, "    public class ")) && !(Strings.containsAnyIgnoreCase(line, "TestBean")) && !Strings
                                .containsAny(lines.get(i - 1), "@Nested", "@DisplayName", "@Builder", "@Data", "@NoArgsConstructor", "@AllArgsConstructor")) {
                            lines.set(i, "    @Nested" + IOUtil.LINE_SEPARATOR + line);
                            changed = true;
                        } else if (Strings.startsWithIgnoreCase(line, "    static class ")) {
                            lines.set(i, line.replaceFirst("    static class ", "    public static class "));
                            changed = true;
                        }
                    }

                    if (changed) {
                        IOUtil.writeLines(lines, file);
                    }
                });
    }

    @Test
    public void test_renameClass() throws IOException {
        renameClass("claude", 100);
        renameClass("gemini", 200);
        renameClass("chatgpt", 300);

    }

    void renameClass(String packageKeyWord, int startIndex) throws IOException {
        final File parentPath = new File("./src/generated-test/");

        Map<Object, List<File>> fileMap = Stream.listFiles(parentPath, true) // 
                .filter(file -> file.isFile() && file.getName().endsWith(".java") && Strings.containsAnyIgnoreCase(file.getAbsolutePath(), packageKeyWord))
                .peek(Fn.println())
                // .limit(10)
                .sortedBy(it -> it.getName())
                .groupTo(it -> it.getName().replaceAll("\\d+", ""));

        fileMap.entrySet().forEach(Fn.println());

        Stream.of(fileMap) //
                .forEach(entry -> {
                    final MutableInt idx = MutableInt.of(startIndex);
                    Stream.of(entry.getValue()).forEach(file -> {
                        String oldName = IOUtil.getNameWithoutExtension(file);
                        String newName = oldName.replaceAll("\\d+", "").replace("Test", "") + idx.getAndIncrement() + "Test";
                        String str = IOUtil.readAllToString(file);

                        IOUtil.write(Strings.replaceAll(str, oldName, newName), file);

                        file.renameTo(new File(file.getParent(), newName + ".java"));
                    });
                });

    }

    //    @Test
    //    public void test_fix() throws IOException {
    //        final File parentPath = new File("./src/generated-test/claude/com/landawn/abacus/type");
    //
    //        Stream.listFiles(parentPath, true) //
    //                .filter(file -> file.isFile() && file.getName().endsWith(".java"))
    //                .peek(Fn.println())
    //                .forEach(file -> {
    //                    List<String> lines = IOUtil.readAllLines(file);
    //
    //                    boolean changed = false;
    //                    for (int i = 0; i < lines.size(); i++) {
    //                        String line = lines.get(i);
    //
    //                        if (line.contains("// To be implemented") || line.contains("// Implementation will be provided later")
    //                                || line.contains("// Empty implementation - to be provided later")) {
    //                            N.deleteRange(lines, i - 1, i + 3);
    //                            i -= 2; // Adjust index after deletion
    //                            changed = true;
    //                        }
    //
    //                        if (changed) {
    //                            IOUtil.writeLines(lines, file);
    //                        }
    //                    }
    //                });
    //    }

    //    @Test
    //    public void test_junit5_migration() throws IOException {
    //        final File parentPath = new File("./src/generated-test/");
    //
    //        Stream.listFiles(parentPath, true) //
    //                .filter(file -> file.isFile() && file.getName().endsWith(".java"))
    //                .peek(Fn.println())
    //                .forEach(file -> {
    //                    List<String> lines = IOUtil.readAllLines(file);
    //
    //                    boolean changed = false;
    //                    for (int i = 0; i < lines.size(); i++) {
    //                        String line = lines.get(i);
    //
    //                        if (line.contains("@Test(expected = ")) {
    //                            String exceptionClass = Strings.substringBetween(line, "@Test(expected = ", ")");
    //                            String iden = Strings.substringBefore(line, "@Test(expected");
    //                            String endLine = iden + "}";
    //
    //                            lines.set(i, Strings.substringBefore(line, "(expected"));
    //
    //                            for (int j = i + 1; j < lines.size(); j++) {
    //                                if (lines.get(j).startsWith(endLine)) {
    //                                    for (int k = j - 1; k >= i; k--) {
    //                                        if (lines.get(k).endsWith(";")) {
    //                                            String lineK = lines.get(k);
    //                                            lineK = lineK.substring(0, lineK.length() - 1) + ");"; // Remove the semicolon
    //                                            lines.set(k, lineK);
    //
    //                                            for (int l = k; l >= i; l--) {
    //                                                String lineL = lines.get(l);
    //
    //                                                if (lineL.length() - lineL.trim().length() == iden.length() + 4) {
    //                                                    String newLine = Strings.repeat(" ", iden.length() + 4) + "assertThrows(" + exceptionClass + ", () -> "
    //                                                            + lineL.trim();
    //                                                    lines.set(l, newLine);
    //                                                    break;
    //                                                }
    //                                            }
    //
    //                                            break;
    //                                        }
    //                                    }
    //                                    break;
    //                                }
    //                            }
    //
    //                            changed = true;
    //                        }
    //
    //                        if (changed) {
    //                            IOUtil.writeLines(lines, file);
    //                        }
    //                    }
    //                });
    //    }


    void test_split_bigClass() throws IOException {
        final File parentPath = new File("./src/main/java");
        final String targetDir = "./src/generated-test/resources/splitedClasses/";
        final int maxLines = 3000;
        final List<String> filesToSplit = List.of("CommonUtil.java", "RowDataSet.java", "Seq.java", "Strings.java", "Numbers.java", "Array.java",
                "Iterators.java");

        Stream.listFiles(parentPath, true) //
                .filter(file -> filesToSplit.contains(file.getName()))
                .peek(Fn.println())
                .forEach(file -> {
                    final MutableInt fileIndex = MutableInt.of(10);
                    List<String> lines = IOUtil.readAllLines(file);

                    for (int i = 0, len = lines.size(); i < len; i++) {
                        String line = lines.get(i);

                        if (line.startsWith("    public ") && !line.contains(" class ")) {
                            for (int j = i + 1; j < len; j++) {
                                String nextLine = lines.get(j);

                                if (((nextLine.startsWith("    public ") && !nextLine.contains(" class ")) && j - i >= maxLines) || nextLine.startsWith("}")) {
                                    if (nextLine.startsWith("    public ") && !nextLine.contains(" class ")) {
                                        while (!lines.get(j).startsWith("    }")) {
                                            j--;
                                        }
                                        j++; // Include the closing brace of the method
                                    }
                                    List<String> linesForNewFile = new ArrayList<>(lines.subList(i, j));
                                    linesForNewFile.add(0, "public class " + file.getName().replace(".java", "") + fileIndex.value() + " {");
                                    linesForNewFile.add(0, "import static com.landawn.abacus.util.N.*;"); // Import the original class if needed 
                                    linesForNewFile.add(0, "import com.landawn.abacus.util.*;"); // Import the original class if needed 
                                    linesForNewFile.add(0, "import java.io.*;"); // Import the original class if needed 
                                    linesForNewFile.add(0, "import java.util.*;"); // Import the original class if needed 

                                    linesForNewFile.add("}");
                                    IOUtil.writeLines(linesForNewFile, new File(targetDir, file.getName().replace(".java", "") + fileIndex.value() + ".java"));

                                    fileIndex.incrementAndGet();
                                    i = j - 1;
                                    break;
                                }
                            }
                        }
                    }
                });

    }

}
