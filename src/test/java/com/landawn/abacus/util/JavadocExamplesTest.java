package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jdk.jshell.Diag;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.SourceCodeAnalysis;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;
import jdk.jshell.VarSnippet;

public class JavadocExamplesTest {

    private static final Pattern CODE_BLOCK = Pattern.compile("<pre>\\{@code(.*?)}</pre>", Pattern.DOTALL);
    private static final Pattern INLINE_THROWS_PATTERN = Pattern.compile("^\\s*throw[s]?\\s+([\\w$.]+)");
    private static final Pattern SCOPED_THROWS_PATTERN = Pattern.compile("\\bthrow[s]?\\s+([\\w$.]+)");

    private static final List<String> COMMON_IMPORTS = Arrays.asList("import java.util.*;", "import java.util.concurrent.*;",
            "import java.util.concurrent.locks.*;", "import java.util.stream.*;", "import java.time.*;", "import java.math.*;", "import java.io.*;",
            "import java.net.*;", "import java.nio.*;", "import java.nio.file.*;", "import com.landawn.abacus.util.*;",
            "import com.landawn.abacus.util.function.*;", "import com.landawn.abacus.util.stream.*;");

    private static final List<String> HELPER_SNIPPETS = Arrays.asList("void doBackgroundTask() {}", "void processData() {}",
            "String fetchData() { return \"data\"; }", "String fetchData(Object url) { return \"data\"; }", "String fetchData1() { return \"data1\"; }",
            "String fetchData2() { return \"data2\"; }", "String fetchData3() { return \"data3\"; }", "String processFile1() { return \"file1\"; }",
            "String processFile2() { return \"file2\"; }", "String processFile3() { return \"file3\"; }", "String processFile4() { return \"file4\"; }",
            "String processFile5() { return \"file5\"; }", "String performOperation() { return \"ok\"; }",
            "java.util.Map<String, String> loadConfig() { return new java.util.HashMap<>(); }",
            "java.util.List<String> loadDataFromDatabase() { return java.util.Arrays.asList(\"row1\"); }", "double computeAverage() { return 0.0; }",
            "int count = 1;", "double val = 1.0;", "Object obj = new Object();", "Object obj1 = new Object();", "Object obj2 = new Object();", "char c = 'a';",
            "char c1 = 'a';", "char c2 = 'b';", "String str = \"value\";", "String s = \"value\";", "String s1 = \"a\";", "String s2 = \"b\";",
            "java.util.List items = java.util.Arrays.asList(\"item1\", \"item2\");", "java.util.List urls = java.util.Arrays.asList(\"http://example.com\");",
            "java.util.Iterator iter = java.util.Arrays.asList(\"item\").iterator();",
            "java.util.concurrent.Future<String> future = java.util.concurrent.CompletableFuture.completedFuture(\"value\");", "int maxRetries = 1;",
            "long retryDelayMs = 1L;", "void processNumber(Integer number) {}");

    @Test
    public void testCommonUtilJavadocExamples() throws Exception {
        assertDoesNotThrow(() -> {
            runExamples(Path.of("src/main/java/com/landawn/abacus/util/CommonUtil.java"), "com.landawn.abacus.util.N");
        });
    }

    @Test
    public void testNJavadocExamples() throws Exception {
        assertDoesNotThrow(() -> {
            runExamples(Path.of("src/main/java/com/landawn/abacus/util/N.java"), "com.landawn.abacus.util.N");
        });
    }

    @Test
    public void testUtilFunctionJavadocExamples() throws Exception {
        assertDoesNotThrow(() -> {
            runExamplesInDirectory(Path.of("src/main/java/com/landawn/abacus/util/function"), "com.landawn.abacus.util.N");
        });
    }

    private void runExamples(Path path, String staticImportClass) throws IOException {
        String text = Files.readString(path);
        Matcher matcher = CODE_BLOCK.matcher(text);
        int blockIndex = 0;

        while (matcher.find()) {
            blockIndex++;
            String block = matcher.group(1);
            executeBlock(block, path.getFileName() + "#" + blockIndex, staticImportClass);
        }
    }

    private void runExamplesInDirectory(Path directory, String staticImportClass) throws IOException {
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.filter(Files::isRegularFile).filter(it -> it.toString().endsWith(".java")).sorted().toList()) {
                runExamples(path, staticImportClass);
            }
        }
    }

    private void executeBlock(String block, String blockId, String staticImportClass) {
        try (JShell jshell = JShell.builder().build()) {
            jshell.addToClasspath(System.getProperty("java.class.path"));
            SourceCodeAnalysis sca = jshell.sourceCodeAnalysis();
            initImports(jshell, staticImportClass, blockId);
            initHelpers(jshell, blockId);
            runStatements(jshell, sca, block, blockId);
            cleanupExecutors(jshell);
        }
    }

    private void initImports(JShell jshell, String staticImportClass, String blockId) {
        for (String imp : COMMON_IMPORTS) {
            evalOrFail(jshell, imp, blockId);
        }

        evalOrFail(jshell, "import static " + staticImportClass + ".*;", blockId);
    }

    private void initHelpers(JShell jshell, String blockId) {
        for (String snippet : HELPER_SNIPPETS) {
            evalOrFail(jshell, snippet, blockId);
        }
    }

    private void runStatements(JShell jshell, SourceCodeAnalysis sca, String block, String blockId) {
        StringBuilder buffer = new StringBuilder();
        String expectedException = null;
        String scopedExpectedException = null;
        int statementIndex = 0;

        List<String> rawLines = Arrays.asList(block.split("\\R"));

        for (int i = 0; i < rawLines.size(); i++) {
            String line = rawLines.get(i).replaceFirst("^\\s*\\*\\s?", "").trim();
            if (line.isEmpty()) {
                scopedExpectedException = null;
                continue;
            }

            CodeAndComment codeAndComment = splitInlineComment(line);
            String code = codeAndComment.code.trim();
            String comment = codeAndComment.comment;

            if (code.isEmpty()) {
                if (comment != null) {
                    String lower = comment.toLowerCase();
                    if (lower.contains("no exception") || lower.contains("valid")) {
                        scopedExpectedException = null;
                    }

                    Matcher scopedMatcher = SCOPED_THROWS_PATTERN.matcher(comment);
                    if (scopedMatcher.find()) {
                        scopedExpectedException = scopedMatcher.group(1);
                    }
                }
                continue;
            }

            if (comment != null) {
                Matcher matcher = INLINE_THROWS_PATTERN.matcher(comment);
                if (matcher.find()) {
                    expectedException = matcher.group(1);
                }
            }

            if (expectedException == null && scopedExpectedException != null) {
                expectedException = scopedExpectedException;
            }

            buffer.append(code).append('\n');
            CompletionInfo info = sca.analyzeCompletion(buffer.toString());

            if (info.completeness().isComplete() && !isContinuationLine(rawLines, i)) {
                statementIndex++;
                evalStatement(jshell, buffer.toString(), expectedException, blockId, statementIndex);
                buffer.setLength(0);
                expectedException = null;
            }
        }

        if (buffer.length() > 0) {
            statementIndex++;
            evalStatement(jshell, buffer.toString(), expectedException, blockId, statementIndex);
        }
    }

    private boolean isContinuationLine(List<String> rawLines, int index) {
        for (int i = index + 1; i < rawLines.size(); i++) {
            String next = rawLines.get(i).replaceFirst("^\\s*\\*\\s?", "").trim();
            if (next.isEmpty()) {
                continue;
            }

            return next.startsWith(".");
        }

        return false;
    }

    private void evalStatement(JShell jshell, String code, String expectedExceptionName, String blockId, int statementIndex) {
        EvalResult result = evalWithRetry(jshell, code, blockId, statementIndex);
        Throwable actualException = result.exception;

        if (actualException instanceof jdk.jshell.EvalException) {
            jdk.jshell.EvalException evalException = (jdk.jshell.EvalException) actualException;
            String thrownClassName = evalException.getExceptionClassName();

            if (expectedExceptionName == null) {
                Assertions.fail("Unexpected exception in " + blockId + " statement " + statementIndex + ": " + thrownClassName + ". Code: " + code.trim());
            }

            if (isAnyExceptionMarker(expectedExceptionName)) {
                return;
            }

            Class<?> expectedClass = resolveException(expectedExceptionName);
            if (expectedClass != null) {
                Assertions.assertEquals(expectedClass.getName(), thrownClassName, "Expected exception " + expectedClass.getName() + " in " + blockId
                        + " statement " + statementIndex + " but got " + thrownClassName + ". Code: " + code.trim());
            } else {
                Assertions.assertTrue(thrownClassName.endsWith(expectedExceptionName), "Expected exception name " + expectedExceptionName + " in " + blockId
                        + " statement " + statementIndex + " but got " + thrownClassName + ". Code: " + code.trim());
            }

            return;
        }

        if (expectedExceptionName != null) {
            Assertions.assertNotNull(actualException, "Expected exception " + expectedExceptionName + " in " + blockId + " statement " + statementIndex
                    + " but none was thrown. Code: " + code.trim());

            if (isAnyExceptionMarker(expectedExceptionName)) {
                return;
            }

            Class<?> expectedClass = resolveException(expectedExceptionName);
            if (expectedClass != null) {
                Assertions.assertTrue(expectedClass.isInstance(actualException), "Expected exception " + expectedClass.getName() + " in " + blockId
                        + " statement " + statementIndex + " but got " + actualException.getClass().getName() + ". Code: " + code.trim());
            } else {
                Assertions.assertEquals(expectedExceptionName, actualException.getClass().getSimpleName(), "Expected exception name " + expectedExceptionName
                        + " in " + blockId + " statement " + statementIndex + " but got " + actualException.getClass().getName() + ". Code: " + code.trim());
            }
        } else {
            Assertions.assertNull(actualException,
                    "Unexpected exception in " + blockId + " statement " + statementIndex + ": " + actualException + ". Code: " + code.trim());
        }
    }

    private EvalResult evalWithRetry(JShell jshell, String code, String blockId, int statementIndex) {
        EvalResult result = evalSnippet(jshell, code, blockId, statementIndex);

        if (result.redeclarationName != null) {
            dropVariable(jshell, result.redeclarationName);
            result = evalSnippet(jshell, code, blockId, statementIndex);
        }

        if (result.rejected) {
            Assertions.fail("Rejected snippet in " + blockId + " statement " + statementIndex + ": " + result.diagnostics + ". Code: " + code.trim());
        }

        return result;
    }

    private EvalResult evalSnippet(JShell jshell, String code, String blockId, int statementIndex) {
        List<SnippetEvent> events = jshell.eval(code);
        boolean rejected = false;
        Throwable exception = null;
        List<String> diagnostics = new ArrayList<>();
        String redeclarationName = null;

        for (SnippetEvent event : events) {
            if (event.exception() != null) {
                exception = event.exception();
            }

            if (event.status() == Snippet.Status.REJECTED) {
                rejected = true;
                for (Diag diag : jshell.diagnostics(event.snippet()).toList()) {
                    diagnostics.add(diag.getMessage(null));
                }

                if (isRedeclaration(diagnostics)) {
                    redeclarationName = findDeclaredVariable(code);
                }
            }
        }

        return new EvalResult(rejected, exception, diagnostics, redeclarationName);
    }

    private void dropVariable(JShell jshell, String name) {
        for (VarSnippet var : jshell.variables().toList()) {
            if (var.name().equals(name)) {
                jshell.drop(var);
            }
        }
    }

    private boolean isRedeclaration(List<String> diagnostics) {
        for (String diag : diagnostics) {
            String lower = diag.toLowerCase();
            if (lower.contains("already defined") || lower.contains("already declared")) {
                return true;
            }
        }
        return false;
    }

    private String findDeclaredVariable(String code) {
        Pattern decl = Pattern.compile("\\b(?:final\\s+)?(?:var|[A-Za-z_][A-Za-z0-9_<>\\[\\]]*)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:=|;)");
        Matcher matcher = decl.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void cleanupExecutors(JShell jshell) {
        for (VarSnippet var : jshell.variables().toList()) {
            String typeName = var.typeName();
            if (typeName.contains("Executor")) {
                String name = var.name();
                evalIgnoringErrors(jshell, "if (" + name + " instanceof java.util.concurrent.ExecutorService) { ((java.util.concurrent.ExecutorService) " + name
                        + ").shutdownNow(); }");
            }
        }
    }

    private void evalOrFail(JShell jshell, String code, String blockId) {
        List<SnippetEvent> events = jshell.eval(code);
        for (SnippetEvent event : events) {
            if (event.status() == Snippet.Status.REJECTED) {
                List<String> diagnostics = new ArrayList<>();
                for (Diag diag : jshell.diagnostics(event.snippet()).toList()) {
                    diagnostics.add(diag.getMessage(null));
                }

                Assertions.fail("Rejected snippet while initializing " + blockId + ": " + diagnostics + ". Code: " + code);
            }
        }
    }

    private void evalIgnoringErrors(JShell jshell, String code) {
        jshell.eval(code);
    }

    private CodeAndComment splitInlineComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;

        for (int i = 0; i < line.length() - 1; i++) {
            char ch = line.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if ((inSingle || inDouble) && ch == '\\') {
                escaped = true;
                continue;
            }

            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }

            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }

            if (!inSingle && !inDouble && ch == '/' && line.charAt(i + 1) == '/') {
                return new CodeAndComment(line.substring(0, i), line.substring(i + 2));
            }
        }

        return new CodeAndComment(line, null);
    }

    private Class<?> resolveException(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        try {
            if (name.contains(".")) {
                return Class.forName(name);
            }
        } catch (ClassNotFoundException ignored) {
            return null;
        }

        for (String prefix : Arrays.asList("java.lang.", "java.util.", "java.io.", "com.landawn.abacus.exception.")) {
            try {
                return Class.forName(prefix + name);
            } catch (ClassNotFoundException ignored) {
                // continue
            }
        }

        return null;
    }

    private boolean isAnyExceptionMarker(String name) {
        if (name == null) {
            return false;
        }

        String normalized = name.trim().toLowerCase();
        return "exception".equals(normalized) || "exceptions".equals(normalized) || "error".equals(normalized);
    }

    private static final class CodeAndComment {
        private final String code;
        private final String comment;

        private CodeAndComment(String code, String comment) {
            this.code = code;
            this.comment = comment;
        }
    }

    private static final class EvalResult {
        private final boolean rejected;
        private final Throwable exception;
        private final List<String> diagnostics;
        private final String redeclarationName;

        private EvalResult(boolean rejected, Throwable exception, List<String> diagnostics, String redeclarationName) {
            this.rejected = rejected;
            this.exception = exception;
            this.diagnostics = diagnostics;
            this.redeclarationName = redeclarationName;
        }
    }
}
