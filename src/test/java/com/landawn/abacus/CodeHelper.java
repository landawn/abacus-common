package com.landawn.abacus;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.stream.Stream.StreamEx;

public class CodeHelper {

    @Test
    public void remove_useless_comments() {
        final File parentPath = new File("./../../Abacus/src/main/java/");

        final Function<String, String> returnMapper = line -> line.trim().startsWith("* @return the ") ? line.substring(0, line.indexOf("* @return the ") + 9)
                : line;

        final Function<String, String> paramMapper = line -> {
            if (line.trim().startsWith("* @param <") && line.indexOf("> the generic type") > 0) {
                return line.replaceAll("> the generic type", ">");
            } else if (line.trim().startsWith("* @param ")) {
                int index = line.indexOf(" the ");
                int index2 = line.indexOf("* @param ");

                if (index > 0) {
                    String paramName = line.substring(index2 + 8, index).trim();
                    String doc = line.substring(index + 5).trim().replaceAll(" ", "");
                    if (paramName.equalsIgnoreCase(doc)) {
                        return line.substring(0, index);
                    }
                }

                line = StringUtil.replaceAll(line, index2 + 8, "       ", " ");
                line = StringUtil.replaceAll(line, index2 + 8, "      ", " ");
                line = StringUtil.replaceAll(line, index2 + 8, "     ", " ");
                line = StringUtil.replaceAll(line, index2 + 8, "    ", " ");
                line = StringUtil.replaceAll(line, index2 + 8, "   ", " ");
                line = StringUtil.replaceAll(line, index2 + 8, "  ", " ");

                return line;
            } else {
                return line;
            }
        };

        Stream.listFiles(parentPath, true) //
                .filter(f -> f.isFile())
                .filter(f -> f.getName().endsWith(".java"))
                .peek(Fn.println())
                .forEach(f -> {
                    final List<String> lines = Stream.lines(f) //
                            .map(returnMapper) //
                            .map(paramMapper)
                            .toList();
                    IOUtil.writeLines(f, lines);
                });

        final Set<String> exclude = N.asSet("* Lazy evaluation.");

        Stream.listFiles(parentPath, true) //
                .filter(f -> f.isFile())
                .filter(f -> f.getName().endsWith(".java"))
                // .peek(Fn.println())
                .forEach(f -> {
                    final List<String> lines = Stream.lines(f) //
                            .collapse((p, n) -> n.trim().startsWith("/**") == false, Suppliers.ofList())
                            .map(list -> {
                                if (list.size() >= 3 && list.get(0).trim().equals("/**") && list.get(2).trim().equals("*")) {
                                    String str = list.get(1).trim();

                                    if (str.startsWith("*") && str.endsWith(".") && str.indexOf('<') < 0 && str.indexOf('{') < 0
                                            && exclude.contains(str) == false) {
                                        String[] strs = str.split(" ");
                                        if (strs.length <= 3) {
                                            N.println(list.remove(1));
                                        }
                                    }
                                }

                                return list;
                            })
                            .flattMap(Fn.identity())
                            .toList();

                    IOUtil.writeLines(f, lines);
                });
    }

    @Test
    public void remove_comments() {
        File file = new File("./src/com/landawn/abacus/util/function/");

        for (File javaFile : file.listFiles()) {
            List<String> lines = IOUtil.readLines(javaFile);
            lines = StreamEx.of(lines).filter(new Predicate<String>() {
                @Override
                public boolean test(String line) {
                    return !(line.startsWith("// ") || line.startsWith("    //"));
                }
            }).toList();

            if (lines.get(lines.size() - 1).equals("}") && lines.get(lines.size() - 2).trim().equals("")) {
                lines.remove(lines.size() - 2);
            }
            IOUtil.writeLines(javaFile, lines);
        }
    }

}
