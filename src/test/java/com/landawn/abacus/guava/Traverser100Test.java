package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Traverser100Test extends TestBase {

    @TempDir
    Path tempDir;

    public static class TreeNode {
        String name;
        List<TreeNode> children = new ArrayList<>();

        TreeNode(String name) {
            this.name = name;
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }

        List<TreeNode> getChildren() {
            return children;
        }
    }

    public static class GraphNode {
        String name;
        Set<GraphNode> neighbors = new HashSet<>();

        GraphNode(String name) {
            this.name = name;
        }

        public void addNeighbor(GraphNode neighbor) {
            neighbors.add(neighbor);
        }

        Set<GraphNode> getNeighbors() {
            return neighbors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            GraphNode graphNode = (GraphNode) o;
            return Objects.equals(name, graphNode.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Test
    public void testForTree_SimpleTree() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        assertNotNull(traverser);
    }

    @Test
    public void testForTree_NullFunction() {
        assertThrows(NullPointerException.class, () -> {
            Traverser.forTree(null);
        });
    }

    @Test
    public void testForGraph_SimpleGraph() {
        GraphNode node = new GraphNode("A");
        Traverser<GraphNode> traverser = Traverser.forGraph(GraphNode::getNeighbors);
        assertNotNull(traverser);
    }

    @Test
    public void testForGraph_NullFunction() {
        assertThrows(NullPointerException.class, () -> {
            Traverser.forGraph(null);
        });
    }

    @Test
    public void testBreadthFirst_Tree() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        Stream<TreeNode> stream = traverser.breadthFirst(root);
        stream.forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F"), result);
    }

    @Test
    public void testBreadthFirst_SingleNode() {
        TreeNode single = new TreeNode("Single");
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.breadthFirst(single).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("Single"), result);
    }

    @Test
    public void testBreadthFirst_Graph() {
        GraphNode nodeA = new GraphNode("A");
        GraphNode nodeB = new GraphNode("B");
        GraphNode nodeC = new GraphNode("C");
        GraphNode nodeD = new GraphNode("D");

        nodeA.addNeighbor(nodeB);
        nodeA.addNeighbor(nodeC);
        nodeB.addNeighbor(nodeD);
        nodeC.addNeighbor(nodeD);

        Traverser<GraphNode> traverser = Traverser.forGraph(GraphNode::getNeighbors);

        Set<String> result = new HashSet<>();
        traverser.breadthFirst(nodeA).forEach(node -> result.add(node.name));

        assertEquals(new HashSet<>(Arrays.asList("A", "B", "C", "D")), result);
    }

    @Test
    public void testDepthFirstPreOrder_Tree() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.depthFirstPreOrder(root).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("A", "B", "D", "E", "C", "F"), result);
    }

    @Test
    public void testDepthFirstPreOrder_SingleNode() {
        TreeNode single = new TreeNode("Single");
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.depthFirstPreOrder(single).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("Single"), result);
    }

    @Test
    public void testDepthFirstPostOrder_Tree() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.depthFirstPostOrder(root).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("D", "E", "B", "F", "C", "A"), result);
    }

    @Test
    public void testDepthFirstPostOrder_SingleNode() {
        TreeNode single = new TreeNode("Single");
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.depthFirstPostOrder(single).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("Single"), result);
    }

    @Test
    public void testFILES_BreadthFirst() throws IOException {
        File rootDir = new File(tempDir.toFile(), "test");
        rootDir.mkdir();
        File subDir1 = new File(rootDir, "sub1");
        subDir1.mkdir();
        File subDir2 = new File(rootDir, "sub2");
        subDir2.mkdir();
        new File(rootDir, "file1.txt").createNewFile();
        new File(subDir1, "file2.txt").createNewFile();
        new File(subDir2, "file3.txt").createNewFile();

        List<String> fileNames = new ArrayList<>();
        Traverser.FILES.breadthFirst(rootDir).forEach(file -> fileNames.add(file.getName()));

        assertTrue(fileNames.contains("test"));
        assertTrue(fileNames.contains("sub1"));
        assertTrue(fileNames.contains("sub2"));
        assertTrue(fileNames.contains("file1.txt"));
        assertTrue(fileNames.contains("file2.txt"));
        assertTrue(fileNames.contains("file3.txt"));
    }

    @Test
    public void testFILES_DepthFirstPreOrder() throws IOException {
        File rootDir = new File(tempDir.toFile(), "test");
        rootDir.mkdir();
        new File(rootDir, "file.txt").createNewFile();

        List<String> fileNames = new ArrayList<>();
        Traverser.FILES.depthFirstPreOrder(rootDir).forEach(file -> fileNames.add(file.getName()));

        assertTrue(fileNames.contains("test"));
        assertTrue(fileNames.contains("file.txt"));
    }

    @Test
    public void testFILES_DepthFirstPostOrder() throws IOException {
        File rootDir = new File(tempDir.toFile(), "test");
        rootDir.mkdir();
        File subDir = new File(rootDir, "sub");
        subDir.mkdir();

        List<String> fileNames = new ArrayList<>();
        Traverser.FILES.depthFirstPostOrder(rootDir).forEach(file -> fileNames.add(file.getName()));

        int subIndex = fileNames.indexOf("sub");
        int testIndex = fileNames.indexOf("test");
        assertTrue(subIndex < testIndex);
    }

    @Test
    public void testFILES_NonExistentFile() {
        File nonExistent = new File("non_existent_file.txt");

        List<File> files = new ArrayList<>();
        Traverser.FILES.breadthFirst(nonExistent).forEach(files::add);

        assertEquals(1, files.size());
        assertEquals(nonExistent, files.get(0));
    }

    @Test
    public void testFILES_EmptyDirectory() throws IOException {
        File emptyDir = new File(tempDir.toFile(), "empty");
        emptyDir.mkdir();

        List<File> files = new ArrayList<>();
        Traverser.FILES.breadthFirst(emptyDir).forEach(files::add);

        assertEquals(1, files.size());
        assertEquals(emptyDir, files.get(0));
    }

    @Test
    public void testStreamOperations_Limit() {
        TreeNode root = createLargeTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        long count = traverser.breadthFirst(root).limit(3).count();
        assertEquals(3, count);
    }

    @Test
    public void testStreamOperations_Filter() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.breadthFirst(root).filter(node -> node.name.compareTo("C") > 0).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("D", "E", "F"), result);
    }

    @Test
    public void testStreamOperations_Map() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<Integer> lengths = new ArrayList<>();
        traverser.breadthFirst(root).map(node -> node.name.length()).forEach(lengths::add);

        assertEquals(6, lengths.size());
        assertTrue(lengths.stream().allMatch(len -> len == 1));
    }

    @Test
    public void testGraphWithCycle() {
        GraphNode nodeA = new GraphNode("A");
        GraphNode nodeB = new GraphNode("B");
        GraphNode nodeC = new GraphNode("C");

        nodeA.addNeighbor(nodeB);
        nodeB.addNeighbor(nodeC);
        nodeC.addNeighbor(nodeA);

        Traverser<GraphNode> traverser = Traverser.forGraph(GraphNode::getNeighbors);

        Set<String> visited = new HashSet<>();
        traverser.breadthFirst(nodeA).forEach(node -> visited.add(node.name));

        assertEquals(3, visited.size());
        assertTrue(visited.contains("A"));
        assertTrue(visited.contains("B"));
        assertTrue(visited.contains("C"));
    }

    private TreeNode createSimpleTree() {
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

        return root;
    }

    private TreeNode createLargeTree() {
        TreeNode root = new TreeNode("Root");
        for (int i = 0; i < 10; i++) {
            TreeNode child = new TreeNode("Child" + i);
            root.addChild(child);
            for (int j = 0; j < 10; j++) {
                child.addChild(new TreeNode("GrandChild" + i + "_" + j));
            }
        }
        return root;
    }
}
