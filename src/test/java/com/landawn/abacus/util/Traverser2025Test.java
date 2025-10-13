package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.Traverser;

@Tag("2025")
public class Traverser2025Test extends TestBase {

    static class TreeNode {
        String value;
        List<TreeNode> children;

        TreeNode(String value) {
            this.value = value;
            this.children = new ArrayList<>();
        }

        void addChild(TreeNode child) {
            children.add(child);
        }

        List<TreeNode> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TreeNode treeNode = (TreeNode) o;
            return value.equals(treeNode.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    @Test
    public void test_forTree_simpleTree() {
        TreeNode root = new TreeNode("root");
        TreeNode child1 = new TreeNode("child1");
        TreeNode child2 = new TreeNode("child2");
        root.addChild(child1);
        root.addChild(child2);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        assertNotNull(traverser);

        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals(3, result.size());
        assertTrue(result.contains("root"));
        assertTrue(result.contains("child1"));
        assertTrue(result.contains("child2"));
    }

    @Test
    public void test_forTree_emptyChildren() {
        TreeNode leaf = new TreeNode("leaf");

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(leaf).map(TreeNode::toString).toList();

        assertEquals(1, result.size());
        assertEquals("leaf", result.get(0));
    }

    @Test
    public void test_forTree_deepTree() {
        TreeNode root = new TreeNode("root");
        TreeNode child = new TreeNode("child");
        TreeNode grandchild = new TreeNode("grandchild");
        root.addChild(child);
        child.addChild(grandchild);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPreOrder(root).map(TreeNode::toString).toList();

        assertEquals(Arrays.asList("root", "child", "grandchild"), result);
    }

    @Test
    public void test_forTree_multipleChildren() {
        TreeNode root = new TreeNode("root");
        TreeNode c1 = new TreeNode("c1");
        TreeNode c2 = new TreeNode("c2");
        TreeNode c3 = new TreeNode("c3");
        TreeNode gc1 = new TreeNode("gc1");
        TreeNode gc2 = new TreeNode("gc2");

        root.addChild(c1);
        root.addChild(c2);
        root.addChild(c3);
        c1.addChild(gc1);
        c2.addChild(gc2);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals(6, result.size());
        assertEquals("root", result.get(0));
        assertTrue(result.subList(1, 4).containsAll(Arrays.asList("c1", "c2", "c3")));
    }

    @Test
    public void test_forTree_withNullChildrenFunction() {
        TreeNode root = new TreeNode("root");

        Traverser<TreeNode> traverser = Traverser.forTree(node -> {
            if (node.getChildren().isEmpty()) {
                return Collections.emptyList();
            }
            return node.getChildren();
        });

        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals(1, result.size());
    }

    @Test
    public void test_forGraph_simpleGraph() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B"));
        graph.put("B", Arrays.asList("C"));
        graph.put("C", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void test_forGraph_withCycles() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B"));
        graph.put("B", Arrays.asList("C"));
        graph.put("C", Arrays.asList("A"));

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void test_forGraph_multiplePathsToSameNode() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B", "C"));
        graph.put("B", Arrays.asList("D"));
        graph.put("C", Arrays.asList("D"));
        graph.put("D", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(4, result.size());
        assertEquals(1, result.stream().filter(s -> s.equals("D")).count());
    }

    @Test
    public void test_forGraph_disconnectedNodes() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B"));
        graph.put("B", Collections.emptyList());
        graph.put("C", Arrays.asList("D"));
        graph.put("D", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("A", "B")));
        assertFalse(result.contains("C"));
        assertFalse(result.contains("D"));
    }

    @Test
    public void test_forGraph_selfLoop() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("A", "B"));
        graph.put("B", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(2, result.size());
        assertEquals(1, result.stream().filter(s -> s.equals("A")).count());
    }

    @Test
    public void test_forGraph_complexGraph() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B", "C"));
        graph.put("B", Arrays.asList("D", "E"));
        graph.put("C", Arrays.asList("F"));
        graph.put("D", Collections.emptyList());
        graph.put("E", Arrays.asList("F"));
        graph.put("F", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList("A", "B", "C", "D", "E", "F")));
    }

    @Test
    public void test_breadthFirst_correctOrder() {
        TreeNode root = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");
        TreeNode e = new TreeNode("E");
        TreeNode f = new TreeNode("F");

        root.addChild(b);
        root.addChild(c);
        b.addChild(d);
        b.addChild(e);
        c.addChild(f);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals("A", result.get(0));
        assertTrue(result.subList(1, 3).containsAll(Arrays.asList("B", "C")));
        assertTrue(result.subList(3, 6).containsAll(Arrays.asList("D", "E", "F")));
    }

    @Test
    public void test_breadthFirst_singleNode() {
        TreeNode single = new TreeNode("single");

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(single).map(TreeNode::toString).toList();

        assertEquals(1, result.size());
        assertEquals("single", result.get(0));
    }

    @Test
    public void test_breadthFirst_linearChain() {
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");

        a.addChild(b);
        b.addChild(c);
        c.addChild(d);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(a).map(TreeNode::toString).toList();

        assertEquals(Arrays.asList("A", "B", "C", "D"), result);
    }

    @Test
    public void test_breadthFirst_withStreamOperations() {
        TreeNode root = new TreeNode("root");
        TreeNode child1 = new TreeNode("child1");
        TreeNode child2 = new TreeNode("child2");
        root.addChild(child1);
        root.addChild(child2);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> limited = traverser.breadthFirst(root).limit(2).map(TreeNode::toString).toList();

        assertEquals(2, limited.size());

        List<String> filtered = traverser.breadthFirst(root).filter(node -> node.value.startsWith("child")).map(TreeNode::toString).toList();

        assertEquals(2, filtered.size());
    }

    @Test
    public void test_breadthFirst_largeTree() {
        TreeNode root = new TreeNode("root");
        for (int i = 0; i < 10; i++) {
            TreeNode child = new TreeNode("level1_" + i);
            root.addChild(child);
            for (int j = 0; j < 5; j++) {
                child.addChild(new TreeNode("level2_" + i + "_" + j));
            }
        }

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals(61, result.size());
    }

    @Test
    public void test_depthFirstPreOrder_correctOrder() {
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");
        TreeNode e = new TreeNode("E");

        a.addChild(b);
        a.addChild(c);
        b.addChild(d);
        b.addChild(e);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPreOrder(a).map(TreeNode::toString).toList();

        assertEquals("A", result.get(0));
        assertEquals("B", result.get(1));
        int cIndex = result.indexOf("C");
        int dIndex = result.indexOf("D");
        int eIndex = result.indexOf("E");
        assertTrue(dIndex < cIndex);
        assertTrue(eIndex < cIndex);
    }

    @Test
    public void test_depthFirstPreOrder_singleNode() {
        TreeNode single = new TreeNode("single");

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPreOrder(single).map(TreeNode::toString).toList();

        assertEquals(1, result.size());
        assertEquals("single", result.get(0));
    }

    @Test
    public void test_depthFirstPreOrder_binaryTree() {
        TreeNode root = new TreeNode("1");
        TreeNode left = new TreeNode("2");
        TreeNode right = new TreeNode("3");
        TreeNode ll = new TreeNode("4");
        TreeNode lr = new TreeNode("5");

        root.addChild(left);
        root.addChild(right);
        left.addChild(ll);
        left.addChild(lr);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPreOrder(root).map(TreeNode::toString).toList();

        assertEquals("1", result.get(0));
        assertEquals("2", result.get(1));
        assertTrue(result.indexOf("4") < result.indexOf("3"));
        assertTrue(result.indexOf("5") < result.indexOf("3"));
    }

    @Test
    public void test_depthFirstPreOrder_withGraphCycles() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Arrays.asList("B"));
        graph.put("B", Arrays.asList("C"));
        graph.put("C", Arrays.asList("A"));

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.depthFirstPreOrder("A").toList();

        assertEquals(3, result.size());
        assertEquals("A", result.get(0));
    }

    @Test
    public void test_depthFirstPreOrder_withStreamOperations() {
        TreeNode root = new TreeNode("root");
        TreeNode child1 = new TreeNode("child1");
        TreeNode child2 = new TreeNode("child2");
        root.addChild(child1);
        root.addChild(child2);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        String first = traverser.depthFirstPreOrder(root).filter(node -> node.value.contains("child")).map(TreeNode::toString).findFirst().orElse(null);

        assertNotNull(first);
        assertTrue(first.startsWith("child"));
    }

    @Test
    public void test_depthFirstPostOrder_correctOrder() {
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");
        TreeNode e = new TreeNode("E");

        a.addChild(b);
        a.addChild(c);
        b.addChild(d);
        b.addChild(e);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPostOrder(a).map(TreeNode::toString).toList();

        assertEquals("A", result.get(result.size() - 1));

        int bIndex = result.indexOf("B");
        int dIndex = result.indexOf("D");
        int eIndex = result.indexOf("E");
        assertTrue(dIndex < bIndex);
        assertTrue(eIndex < bIndex);
    }

    @Test
    public void test_depthFirstPostOrder_singleNode() {
        TreeNode single = new TreeNode("single");

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPostOrder(single).map(TreeNode::toString).toList();

        assertEquals(1, result.size());
        assertEquals("single", result.get(0));
    }

    @Test
    public void test_depthFirstPostOrder_linearChain() {
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");

        a.addChild(b);
        b.addChild(c);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPostOrder(a).map(TreeNode::toString).toList();

        assertEquals(Arrays.asList("C", "B", "A"), result);
    }

    @Test
    public void test_depthFirstPostOrder_binaryTree() {
        TreeNode root = new TreeNode("1");
        TreeNode left = new TreeNode("2");
        TreeNode right = new TreeNode("3");
        TreeNode ll = new TreeNode("4");
        TreeNode lr = new TreeNode("5");

        root.addChild(left);
        root.addChild(right);
        left.addChild(ll);
        left.addChild(lr);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.depthFirstPostOrder(root).map(TreeNode::toString).toList();

        assertEquals("1", result.get(result.size() - 1));

        assertTrue(result.indexOf("4") < result.indexOf("2"));
        assertTrue(result.indexOf("5") < result.indexOf("2"));
    }

    @Test
    public void test_depthFirstPostOrder_useCase_treeSize() {
        TreeNode root = new TreeNode("root");
        TreeNode c1 = new TreeNode("c1");
        TreeNode c2 = new TreeNode("c2");
        TreeNode gc1 = new TreeNode("gc1");

        root.addChild(c1);
        root.addChild(c2);
        c1.addChild(gc1);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        long count = traverser.depthFirstPostOrder(root).count();
        assertEquals(4, count);
    }

    @Test
    public void test_FILES_constant_exists() {
        assertNotNull(Traverser.FILES);
    }

    @Test
    public void test_FILES_traverseDirectory(@TempDir File tempDir) throws IOException {
        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();

        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");

        Files.write(file1.toPath(), "content1".getBytes());
        Files.write(file2.toPath(), "content2".getBytes());

        List<File> result = Traverser.FILES.breadthFirst(tempDir).toList();

        assertTrue(result.size() >= 3);
        assertTrue(result.stream().anyMatch(f -> f.getName().equals("file1.txt")));
    }

    @Test
    public void test_FILES_filterFiles(@TempDir File tempDir) throws IOException {
        File javaFile = new File(tempDir, "Test.java");
        File txtFile = new File(tempDir, "readme.txt");

        Files.write(javaFile.toPath(), "".getBytes());
        Files.write(txtFile.toPath(), "".getBytes());

        List<File> javaFiles = Traverser.FILES.breadthFirst(tempDir).filter(f -> f.getName().endsWith(".java")).toList();

        assertEquals(1, javaFiles.size());
        assertEquals("Test.java", javaFiles.get(0).getName());
    }

    @Test
    public void test_FILES_depthFirstTraversal(@TempDir File tempDir) throws IOException {
        File subDir = new File(tempDir, "sub");
        subDir.mkdir();
        File file = new File(subDir, "file.txt");
        Files.write(file.toPath(), "".getBytes());

        List<File> result = Traverser.FILES.depthFirstPreOrder(tempDir).toList();

        assertTrue(result.size() >= 2);
        assertTrue(result.contains(tempDir));
    }

    @Test
    public void test_FILES_emptyDirectory(@TempDir File tempDir) {
        List<File> result = Traverser.FILES.breadthFirst(tempDir).toList();

        assertEquals(1, result.size());
        assertEquals(tempDir, result.get(0));
    }

    @Test
    public void test_FILES_postOrderTraversal(@TempDir File tempDir) throws IOException {
        File subDir = new File(tempDir, "sub");
        subDir.mkdir();
        File file = new File(subDir, "file.txt");
        Files.write(file.toPath(), "".getBytes());

        List<File> result = Traverser.FILES.depthFirstPostOrder(tempDir).toList();

        assertTrue(result.size() >= 2);
        int tempDirIndex = result.indexOf(tempDir);
        int subDirIndex = result.indexOf(subDir);
        assertTrue(subDirIndex < tempDirIndex);
    }

    @Test
    public void test_traversalComparison_sameNodes() {
        TreeNode root = new TreeNode("root");
        TreeNode c1 = new TreeNode("c1");
        TreeNode c2 = new TreeNode("c2");
        root.addChild(c1);
        root.addChild(c2);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        Set<String> bfs = traverser.breadthFirst(root).map(TreeNode::toString).collect(Collectors.toSet());

        Set<String> dfsPreOrder = traverser.depthFirstPreOrder(root).map(TreeNode::toString).collect(Collectors.toSet());

        Set<String> dfsPostOrder = traverser.depthFirstPostOrder(root).map(TreeNode::toString).collect(Collectors.toSet());

        assertEquals(bfs, dfsPreOrder);
        assertEquals(bfs, dfsPostOrder);
    }

    @Test
    public void test_traversalOrdering_differences() {
        TreeNode root = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        root.addChild(b);
        root.addChild(c);

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> bfs = traverser.breadthFirst(root).map(TreeNode::toString).toList();

        List<String> dfsPost = traverser.depthFirstPostOrder(root).map(TreeNode::toString).toList();

        assertEquals("A", bfs.get(0));
        assertEquals("A", dfsPost.get(dfsPost.size() - 1));
    }

    @Test
    public void test_forTree_vs_forGraph_withTree() {
        TreeNode root = new TreeNode("root");
        TreeNode child = new TreeNode("child");
        root.addChild(child);

        Traverser<TreeNode> treeTraverser = Traverser.forTree(TreeNode::getChildren);
        Traverser<TreeNode> graphTraverser = Traverser.forGraph(TreeNode::getChildren);

        List<String> treeResult = treeTraverser.breadthFirst(root).map(TreeNode::toString).toList();

        List<String> graphResult = graphTraverser.breadthFirst(root).map(TreeNode::toString).toList();

        assertEquals(treeResult, graphResult);
    }

    @Test
    public void test_streamLaziness() {
        TreeNode root = new TreeNode("root");
        for (int i = 0; i < 1000; i++) {
            root.addChild(new TreeNode("child" + i));
        }

        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = traverser.breadthFirst(root).limit(5).map(TreeNode::toString).toList();

        assertEquals(5, result.size());
    }

    @Test
    public void test_emptyGraph() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", Collections.emptyList());

        Traverser<String> traverser = Traverser.forGraph(graph::get);

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(1, result.size());
        assertEquals("A", result.get(0));
    }

    @Test
    public void test_graphWithNullSuccessors() {
        Map<String, List<String>> graph = new HashMap<>();
        graph.put("A", null);

        Traverser<String> traverser = Traverser.forGraph(node -> {
            List<String> successors = graph.get(node);
            return successors != null ? successors : Collections.emptyList();
        });

        List<String> result = traverser.breadthFirst("A").toList();

        assertEquals(1, result.size());
    }
}
