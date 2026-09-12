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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.Traverser;

public class TraverserTest extends TestBase {

    public static class TreeNode {
        String value;
        List<TreeNode> children;

        TreeNode(String value) {
            this.value = value;
            this.children = new ArrayList<>();
        }

        public void addChild(TreeNode child) {
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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TreeNode treeNode = (TreeNode) o;
            return value.equals(treeNode.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    private static TreeNode binaryTree() {
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        TreeNode d = new TreeNode("D");
        TreeNode e = new TreeNode("E");
        TreeNode f = new TreeNode("F");
        a.addChild(b);
        a.addChild(c);
        b.addChild(d);
        b.addChild(e);
        c.addChild(f);
        return a;
    }

    @Test
    public void testForTree() {
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        TreeNode root = new TreeNode("root");
        root.addChild(new TreeNode("child1"));
        root.addChild(new TreeNode("child2"));
        List<String> result = traverser.breadthFirst(root).map(TreeNode::toString).toList();
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList("root", "child1", "child2")));

        assertEquals(List.of("leaf"), traverser.breadthFirst(new TreeNode("leaf")).map(TreeNode::toString).toList());

        TreeNode deep = new TreeNode("root");
        TreeNode child = new TreeNode("child");
        deep.addChild(child);
        child.addChild(new TreeNode("grandchild"));
        assertEquals(Arrays.asList("root", "child", "grandchild"), traverser.depthFirstPreOrder(deep).map(TreeNode::toString).toList());

        TreeNode wide = new TreeNode("root");
        TreeNode c1 = new TreeNode("c1");
        TreeNode c2 = new TreeNode("c2");
        TreeNode c3 = new TreeNode("c3");
        wide.addChild(c1);
        wide.addChild(c2);
        wide.addChild(c3);
        c1.addChild(new TreeNode("gc1"));
        c2.addChild(new TreeNode("gc2"));
        List<String> bfs = traverser.breadthFirst(wide).map(TreeNode::toString).toList();
        assertEquals(6, bfs.size());
        assertEquals("root", bfs.get(0));
        assertTrue(bfs.subList(1, 4).containsAll(Arrays.asList("c1", "c2", "c3")));

        Traverser<TreeNode> emptyChildren = Traverser.forTree(node -> node.getChildren().isEmpty() ? Collections.emptyList() : node.getChildren());
        assertEquals(1, emptyChildren.breadthFirst(new TreeNode("root")).toList().size());
    }

    @Test
    public void testForGraph() {
        Map<String, List<String>> linear = new HashMap<>();
        linear.put("A", Arrays.asList("B"));
        linear.put("B", Arrays.asList("C"));
        linear.put("C", Collections.emptyList());
        assertEquals(Arrays.asList("A", "B", "C"), Traverser.forGraph(linear::get).breadthFirst("A").toList());

        Map<String, List<String>> cycle = new HashMap<>();
        cycle.put("A", Arrays.asList("B"));
        cycle.put("B", Arrays.asList("C"));
        cycle.put("C", Arrays.asList("A"));
        List<String> cycled = Traverser.forGraph(cycle::get).breadthFirst("A").toList();
        assertEquals(3, cycled.size());
        assertTrue(cycled.containsAll(Arrays.asList("A", "B", "C")));

        Map<String, List<String>> diamond = new HashMap<>();
        diamond.put("A", Arrays.asList("B", "C"));
        diamond.put("B", Arrays.asList("D"));
        diamond.put("C", Arrays.asList("D"));
        diamond.put("D", Collections.emptyList());
        List<String> diamondVisit = Traverser.forGraph(diamond::get).breadthFirst("A").toList();
        assertEquals(4, diamondVisit.size());
        assertEquals(1, diamondVisit.stream().filter("D"::equals).count());

        Map<String, List<String>> disconnected = new HashMap<>();
        disconnected.put("A", Arrays.asList("B"));
        disconnected.put("B", Collections.emptyList());
        disconnected.put("C", Arrays.asList("D"));
        disconnected.put("D", Collections.emptyList());
        List<String> fromA = Traverser.forGraph(disconnected::get).breadthFirst("A").toList();
        assertEquals(Arrays.asList("A", "B"), fromA);
        assertFalse(fromA.contains("C"));

        Map<String, List<String>> selfLoop = new HashMap<>();
        selfLoop.put("A", Arrays.asList("A", "B"));
        selfLoop.put("B", Collections.emptyList());
        List<String> looped = Traverser.forGraph(selfLoop::get).breadthFirst("A").toList();
        assertEquals(2, looped.size());
        assertEquals(1, looped.stream().filter("A"::equals).count());

        Map<String, List<String>> complex = new HashMap<>();
        complex.put("A", Arrays.asList("B", "C"));
        complex.put("B", Arrays.asList("D", "E"));
        complex.put("C", Arrays.asList("F"));
        complex.put("D", Collections.emptyList());
        complex.put("E", Arrays.asList("F"));
        complex.put("F", Collections.emptyList());
        assertEquals(6, Traverser.forGraph(complex::get).breadthFirst("A").toList().size());

        Map<String, List<String>> empty = new HashMap<>();
        empty.put("A", Collections.emptyList());
        assertEquals(List.of("A"), Traverser.forGraph(empty::get).breadthFirst("A").toList());

        Map<String, List<String>> nullSuccessors = new HashMap<>();
        nullSuccessors.put("A", null);
        assertEquals(1, Traverser.<String> forGraph(node -> {
            List<String> successors = nullSuccessors.get(node);
            return successors != null ? successors : Collections.emptyList();
        }).breadthFirst("A").toList().size());
    }

    @Test
    public void testBreadthFirst() {
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        List<String> bfs = traverser.breadthFirst(binaryTree()).map(TreeNode::toString).toList();
        assertEquals("A", bfs.get(0));
        assertTrue(bfs.subList(1, 3).containsAll(Arrays.asList("B", "C")));
        assertTrue(bfs.subList(3, 6).containsAll(Arrays.asList("D", "E", "F")));
        assertEquals(List.of("single"), traverser.breadthFirst(new TreeNode("single")).map(TreeNode::toString).toList());

        TreeNode chain = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        chain.addChild(b);
        b.addChild(c);
        c.addChild(new TreeNode("D"));
        assertEquals(Arrays.asList("A", "B", "C", "D"), traverser.breadthFirst(chain).map(TreeNode::toString).toList());

        TreeNode root = new TreeNode("root");
        root.addChild(new TreeNode("child1"));
        root.addChild(new TreeNode("child2"));
        assertEquals(2, traverser.breadthFirst(root).limit(2).toList().size());
        assertEquals(2, traverser.breadthFirst(root).filter(node -> node.value.startsWith("child")).toList().size());

        TreeNode large = new TreeNode("root");
        for (int i = 0; i < 10; i++) {
            TreeNode child = new TreeNode("level1_" + i);
            large.addChild(child);
            for (int j = 0; j < 5; j++) {
                child.addChild(new TreeNode("level2_" + i + "_" + j));
            }
        }
        assertEquals(61, traverser.breadthFirst(large).toList().size());
        assertEquals(5, traverser.breadthFirst(large).limit(5).toList().size());
    }

    @Test
    public void testDepthFirstPreOrder() {
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        a.addChild(b);
        a.addChild(c);
        b.addChild(new TreeNode("D"));
        b.addChild(new TreeNode("E"));
        List<String> pre = traverser.depthFirstPreOrder(a).map(TreeNode::toString).toList();
        assertEquals("A", pre.get(0));
        assertEquals("B", pre.get(1));
        assertTrue(pre.indexOf("D") < pre.indexOf("C"));
        assertTrue(pre.indexOf("E") < pre.indexOf("C"));
        assertEquals(List.of("single"), traverser.depthFirstPreOrder(new TreeNode("single")).map(TreeNode::toString).toList());

        TreeNode root = new TreeNode("1");
        TreeNode left = new TreeNode("2");
        root.addChild(left);
        root.addChild(new TreeNode("3"));
        left.addChild(new TreeNode("4"));
        left.addChild(new TreeNode("5"));
        List<String> binary = traverser.depthFirstPreOrder(root).map(TreeNode::toString).toList();
        assertEquals("1", binary.get(0));
        assertEquals("2", binary.get(1));
        assertTrue(binary.indexOf("4") < binary.indexOf("3"));

        Map<String, List<String>> cycle = new HashMap<>();
        cycle.put("A", Arrays.asList("B"));
        cycle.put("B", Arrays.asList("C"));
        cycle.put("C", Arrays.asList("A"));
        List<String> cycled = Traverser.forGraph(cycle::get).depthFirstPreOrder("A").toList();
        assertEquals(3, cycled.size());
        assertEquals("A", cycled.get(0));

        TreeNode filterRoot = new TreeNode("root");
        filterRoot.addChild(new TreeNode("child1"));
        filterRoot.addChild(new TreeNode("child2"));
        String first = traverser.depthFirstPreOrder(filterRoot).filter(node -> node.value.contains("child")).map(TreeNode::toString).findFirst().orElse(null);
        assertNotNull(first);
        assertTrue(first.startsWith("child"));
    }

    @Test
    public void testDepthFirstPostOrder() {
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        TreeNode a = new TreeNode("A");
        TreeNode b = new TreeNode("B");
        TreeNode c = new TreeNode("C");
        a.addChild(b);
        a.addChild(c);
        b.addChild(new TreeNode("D"));
        b.addChild(new TreeNode("E"));
        List<String> post = traverser.depthFirstPostOrder(a).map(TreeNode::toString).toList();
        assertEquals("A", post.get(post.size() - 1));
        assertTrue(post.indexOf("D") < post.indexOf("B"));
        assertTrue(post.indexOf("E") < post.indexOf("B"));
        assertEquals(List.of("single"), traverser.depthFirstPostOrder(new TreeNode("single")).map(TreeNode::toString).toList());

        TreeNode chain = new TreeNode("A");
        TreeNode mid = new TreeNode("B");
        chain.addChild(mid);
        mid.addChild(new TreeNode("C"));
        assertEquals(Arrays.asList("C", "B", "A"), traverser.depthFirstPostOrder(chain).map(TreeNode::toString).toList());

        TreeNode root = new TreeNode("1");
        TreeNode left = new TreeNode("2");
        root.addChild(left);
        root.addChild(new TreeNode("3"));
        left.addChild(new TreeNode("4"));
        left.addChild(new TreeNode("5"));
        List<String> binary = traverser.depthFirstPostOrder(root).map(TreeNode::toString).toList();
        assertEquals("1", binary.get(binary.size() - 1));
        assertTrue(binary.indexOf("4") < binary.indexOf("2"));
        assertEquals(5, traverser.depthFirstPostOrder(root).count());
    }

    @Test
    public void testFiles(@TempDir File tempDir) throws IOException {
        assertNotNull(Traverser.FILES);
        assertEquals(List.of(tempDir), Traverser.FILES.breadthFirst(tempDir).toList());

        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        Files.write(new File(tempDir, "file1.txt").toPath(), "content1".getBytes());
        Files.write(new File(subDir, "file2.txt").toPath(), "content2".getBytes());
        Files.write(new File(tempDir, "Test.java").toPath(), "".getBytes());
        Files.write(new File(tempDir, "readme.txt").toPath(), "".getBytes());

        List<File> bfs = Traverser.FILES.breadthFirst(tempDir).toList();
        assertTrue(bfs.size() >= 3);
        assertTrue(bfs.stream().anyMatch(f -> f.getName().equals("file1.txt")));
        List<File> javaFiles = Traverser.FILES.breadthFirst(tempDir).filter(f -> f.getName().endsWith(".java")).toList();
        assertEquals(1, javaFiles.size());
        assertEquals("Test.java", javaFiles.get(0).getName());

        List<File> pre = Traverser.FILES.depthFirstPreOrder(tempDir).toList();
        assertTrue(pre.size() >= 2);
        assertTrue(pre.contains(tempDir));
        List<File> post = Traverser.FILES.depthFirstPostOrder(tempDir).toList();
        assertTrue(post.indexOf(subDir) < post.indexOf(tempDir));
    }

    @Test
    public void testTraversalComparison() {
        TreeNode root = new TreeNode("A");
        root.addChild(new TreeNode("B"));
        root.addChild(new TreeNode("C"));
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        Set<String> bfs = traverser.breadthFirst(root).map(TreeNode::toString).collect(Collectors.toSet());
        Set<String> pre = traverser.depthFirstPreOrder(root).map(TreeNode::toString).collect(Collectors.toSet());
        Set<String> post = traverser.depthFirstPostOrder(root).map(TreeNode::toString).collect(Collectors.toSet());
        assertEquals(bfs, pre);
        assertEquals(bfs, post);
        assertEquals("A", traverser.breadthFirst(root).map(TreeNode::toString).toList().get(0));
        List<String> postOrder = traverser.depthFirstPostOrder(root).map(TreeNode::toString).toList();
        assertEquals("A", postOrder.get(postOrder.size() - 1));

        TreeNode child = new TreeNode("child");
        TreeNode treeRoot = new TreeNode("root");
        treeRoot.addChild(child);
        assertEquals(Traverser.forTree(TreeNode::getChildren).breadthFirst(treeRoot).map(TreeNode::toString).toList(),
                Traverser.forGraph(TreeNode::getChildren).breadthFirst(treeRoot).map(TreeNode::toString).toList());
    }
}
