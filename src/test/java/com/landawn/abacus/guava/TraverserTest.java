package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

public class TraverserTest extends TestBase {

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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GraphNode graphNode = (GraphNode) o;
            return Objects.equals(name, graphNode.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
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
    public void testForTree_SimpleTree() {
        TreeNode root = createSimpleTree();
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
        assertNotNull(traverser);
    }

    @Test
    public void testForTree_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Traverser.forTree(null);
        });
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

    @Test
    public void testForGraph_SimpleGraph() {
        GraphNode node = new GraphNode("A");
        Traverser<GraphNode> traverser = Traverser.forGraph(GraphNode::getNeighbors);
        assertNotNull(traverser);
    }

    @Test
    public void testForGraph_NullFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
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
    public void testBreadthFirst_SingleNode() {
        TreeNode single = new TreeNode("Single");
        Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);

        List<String> result = new ArrayList<>();
        traverser.breadthFirst(single).forEach(node -> result.add(node.name));

        assertEquals(Arrays.asList("Single"), result);
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
    public void testFILES_EmptyDirectory() throws IOException {
        File emptyDir = new File(tempDir.toFile(), "empty");
        emptyDir.mkdir();

        List<File> files = new ArrayList<>();
        Traverser.FILES.breadthFirst(emptyDir).forEach(files::add);

        assertEquals(1, files.size());
        assertEquals(emptyDir, files.get(0));
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

    // ---------------------------------------------------------------------------------------------
    // a01 F-2: Guava validates start nodes eagerly - the successor function runs once per start node
    // inside the traversal method (before any consumption), and again during traversal.
    // ---------------------------------------------------------------------------------------------

    private static List<String> names(Stream<TreeNode> s) {
        return s.map(n -> n.name).toList();
    }

    @Test
    public void testSuccessorFunction_IsCalledOncePerStartNode_BeforeConsumption() {
        TreeNode root = createSimpleTree(); // A -> B, C ; B -> D, E ; C -> F
        AtomicInteger calls = new AtomicInteger();
        Traverser<TreeNode> traverser = Traverser.forTree(n -> {
            calls.incrementAndGet();
            return n.getChildren();
        });

        Stream<TreeNode> bfs = traverser.breadthFirst(root);
        assertEquals(1, calls.get(), "one validation call for the single start node, before consumption");
        assertEquals(6, bfs.count());
        assertEquals(7, calls.get(), "validation call + one call per traversed node");

        calls.set(0);
        Stream<TreeNode> pre = traverser.depthFirstPreOrder(root);
        assertEquals(1, calls.get());
        pre.count();

        calls.set(0);
        Stream<TreeNode> post = traverser.depthFirstPostOrder(root);
        assertEquals(1, calls.get());
        post.count();

        // two start nodes -> two validation calls before anything is consumed
        calls.set(0);
        TreeNode other = new TreeNode("X");
        Stream<TreeNode> multi = traverser.breadthFirst(Arrays.asList(root, other));
        assertEquals(2, calls.get());
        assertEquals(7, multi.count());
    }

    @Test
    public void testSuccessorFunction_ThrowingForStartNode_PropagatesFromTraversalMethod_NotFromStream() {
        TreeNode root = new TreeNode("root");
        Traverser<TreeNode> thrower = Traverser.forTree(n -> {
            throw new IllegalStateException("succ(" + n.name + ")");
        });

        // no consumption at all - the exception escapes from the wrapper call itself
        IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> thrower.breadthFirst(root));
        assertEquals("succ(root)", e1.getMessage());
        assertThrows(IllegalStateException.class, () -> thrower.depthFirstPreOrder(root));
        assertThrows(IllegalStateException.class, () -> thrower.depthFirstPostOrder(root));
        assertThrows(IllegalStateException.class, () -> thrower.breadthFirst(Arrays.asList(root)));
        assertThrows(IllegalStateException.class, () -> thrower.depthFirstPreOrder(Arrays.asList(root)));
        assertThrows(IllegalStateException.class, () -> thrower.depthFirstPostOrder(Arrays.asList(root)));
    }

    @Test
    public void testSuccessorFunction_ThrowingForChildNode_IsDeferredToConsumption() {
        TreeNode root = new TreeNode("root");
        TreeNode child = new TreeNode("child");
        root.addChild(child);
        Traverser<TreeNode> childThrower = Traverser.forTree(n -> {
            if (n == child) {
                throw new IllegalStateException("succ(child)");
            }
            return n.getChildren();
        });

        // the start node validates fine, so the stream is created ...
        Stream<TreeNode> s = childThrower.breadthFirst(root);
        // ... and the failure surfaces only while consuming
        assertThrows(IllegalStateException.class, () -> s.toList());
    }

    @Test
    public void testSuccessorFunction_ReturningNullForStartNode_FailsOnlyOnConsumption() {
        TreeNode root = new TreeNode("root");
        Traverser<TreeNode> nullReturner = Traverser.forTree(n -> null);

        // validate() ignores the returned Iterable, so the factory-level doc statement holds
        Stream<TreeNode> s = nullReturner.breadthFirst(root);
        assertThrows(NullPointerException.class, () -> s.toList());
    }

    @Test
    public void testPATHS_StartDirectoryIsListedAtCallTimeAndAgainOnConsumption() throws IOException {
        Path dir = tempDir.resolve("double-listed");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(dir.resolve("one.txt"));

        Stream<Path> s = Traverser.PATHS.breadthFirst(dir);
        // created AFTER the call but BEFORE consumption: the second listing picks it up
        java.nio.file.Files.createFile(dir.resolve("two.txt"));

        List<String> seen = s.map(p -> p.getFileName().toString()).toList();
        assertEquals(3, seen.size());
        assertTrue(seen.contains("one.txt"));
        assertTrue(seen.contains("two.txt"));
    }

    // Windows-only: an unlistable START directory makes PATHS throw from the traversal method itself;
    // an unlistable CHILD directory throws only on consumption; FILES yields the single start element.

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static boolean runIcacls(String... args) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("icacls");
            cmd.addAll(Arrays.asList(args));
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Denies READ_DATA (directory listing) on {@code dir} for the current user; returns {@code false} (and
     * leaves the ACL unchanged) if that is not possible here, in which case the caller skips.
     */
    private static boolean denyListing(Path dir) {
        if (!isWindows()) {
            return false;
        }
        String user = System.getProperty("user.name");
        if (user == null || user.isEmpty() || !runIcacls(dir.toString(), "/deny", user + ":(RD)")) {
            return false;
        }
        try (java.nio.file.DirectoryStream<Path> ds = java.nio.file.Files.newDirectoryStream(dir)) {
            ds.iterator().hasNext(); // still listable (e.g. privileged account): undo and skip
            runIcacls(dir.toString(), "/remove:d", user);
            return false;
        } catch (IOException expected) {
            return true;
        }
    }

    private static void allowListing(Path dir) {
        runIcacls(dir.toString(), "/remove:d", System.getProperty("user.name"));
    }

    @Test
    public void testPATHS_UnlistableStartDirectory_ThrowsFromTraversalMethod() throws IOException {
        Path locked = tempDir.resolve("locked-start");
        java.nio.file.Files.createDirectory(locked);
        java.nio.file.Files.createFile(locked.resolve("inside.txt"));
        Assumptions.assumeTrue(denyListing(locked), "could not deny directory listing on this platform/account");
        try {
            // no consumption: the DirectoryIteratorException escapes from the wrapper call
            DirectoryIteratorException e = assertThrows(DirectoryIteratorException.class, () -> Traverser.PATHS.breadthFirst(locked));
            assertTrue(e.getCause() instanceof AccessDeniedException, String.valueOf(e.getCause()));
            assertThrows(DirectoryIteratorException.class, () -> Traverser.PATHS.depthFirstPreOrder(locked));
            assertThrows(DirectoryIteratorException.class, () -> Traverser.PATHS.depthFirstPostOrder(Arrays.asList(locked)));

            // FILES treats an unreadable directory as a leaf (File.listFiles() returns null)
            assertEquals(Arrays.asList(locked.toFile()), Traverser.FILES.breadthFirst(locked.toFile()).toList());
        } finally {
            allowListing(locked);
        }
    }

    @Test
    public void testPATHS_UnlistableChildDirectory_ThrowsOnlyOnConsumption() throws IOException {
        Path parent = tempDir.resolve("parent-of-locked");
        Path locked = parent.resolve("locked-child");
        java.nio.file.Files.createDirectories(locked);
        Assumptions.assumeTrue(denyListing(locked), "could not deny directory listing on this platform/account");
        try {
            // the start node (parent) is listable, so the stream is created ...
            Stream<Path> s = Traverser.PATHS.breadthFirst(parent);
            // ... and the child's failure surfaces during consumption
            assertThrows(DirectoryIteratorException.class, () -> s.toList());
        } finally {
            allowListing(locked);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // a01 F-4: multi-root overloads - equal start nodes are collapsed; a forTree start node reachable
    // from an earlier start node is visited again; forGraph visits once.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testMultiRoot_DuplicateStartNodes_AreCollapsed() {
        TreeNode a = new TreeNode("a");
        TreeNode b = new TreeNode("b");
        a.addChild(b);
        Traverser<TreeNode> tree = Traverser.forTree(TreeNode::getChildren);

        assertEquals(Arrays.asList("a", "b"), names(tree.breadthFirst(Arrays.asList(a, a))));
        assertEquals(Arrays.asList("a", "b"), names(tree.depthFirstPreOrder(Arrays.asList(a, a))));
        assertEquals(Arrays.asList("b", "a"), names(tree.depthFirstPostOrder(Arrays.asList(a, a))));
    }

    @Test
    public void testMultiRoot_ForTree_StartNodeReachableFromEarlierStartNode_IsVisitedTwice() {
        TreeNode a = new TreeNode("a");
        TreeNode b = new TreeNode("b");
        a.addChild(b);
        Traverser<TreeNode> tree = Traverser.forTree(TreeNode::getChildren);

        assertEquals(Arrays.asList("a", "b", "b"), names(tree.breadthFirst(Arrays.asList(a, b))));
        assertEquals(Arrays.asList("b", "a", "b"), names(tree.breadthFirst(Arrays.asList(b, a))));
        assertEquals(Arrays.asList("a", "b", "b"), names(tree.depthFirstPreOrder(Arrays.asList(a, b))));
        assertEquals(Arrays.asList("b", "a", "b"), names(tree.depthFirstPostOrder(Arrays.asList(a, b))));
    }

    @Test
    public void testMultiRoot_ForGraph_VisitsEachNodeOnce() {
        GraphNode a = new GraphNode("a");
        GraphNode b = new GraphNode("b");
        a.addNeighbor(b);
        Traverser<GraphNode> graph = Traverser.forGraph(GraphNode::getNeighbors);

        List<String> bfs = graph.breadthFirst(Arrays.asList(a, b)).map(n -> n.name).toList();
        assertEquals(Arrays.asList("a", "b"), bfs);
        assertEquals(Arrays.asList("a", "b"), graph.depthFirstPreOrder(Arrays.asList(a, b)).map(n -> n.name).toList());
        // an equal (by equals) duplicate start node is collapsed; the surviving 'a' still reaches 'b'
        assertEquals(Arrays.asList("a", "b"), graph.breadthFirst(Arrays.asList(a, new GraphNode("a"))).map(n -> n.name).toList());
    }

    @Test
    public void testMultiRoot_EmptyIterable_YieldsEmptyStream() {
        Traverser<TreeNode> tree = Traverser.forTree(TreeNode::getChildren);
        List<TreeNode> none = new ArrayList<>();

        assertEquals(0, tree.breadthFirst(none).count());
        assertEquals(0, tree.depthFirstPreOrder(none).count());
        assertEquals(0, tree.depthFirstPostOrder(none).count());
    }

    // ---------------------------------------------------------------------------------------------
    // a01 F-6: traversal methods reject null with NullPointerException (factories throw IAE, locked above).
    // ---------------------------------------------------------------------------------------------

    @Test
    public void testTraversalMethods_NullStartNode_ThrowNullPointerException() {
        Traverser<TreeNode> tree = Traverser.forTree(TreeNode::getChildren);

        assertThrows(NullPointerException.class, () -> tree.breadthFirst((TreeNode) null));
        assertThrows(NullPointerException.class, () -> tree.depthFirstPreOrder((TreeNode) null));
        assertThrows(NullPointerException.class, () -> tree.depthFirstPostOrder((TreeNode) null));
        assertThrows(NullPointerException.class, () -> tree.breadthFirst((Iterable<TreeNode>) null));
        assertThrows(NullPointerException.class, () -> tree.depthFirstPreOrder((Iterable<TreeNode>) null));
        assertThrows(NullPointerException.class, () -> tree.depthFirstPostOrder((Iterable<TreeNode>) null));
        assertThrows(NullPointerException.class, () -> tree.breadthFirst(Arrays.asList(new TreeNode("a"), null)));
        assertThrows(NullPointerException.class, () -> Traverser.FILES.breadthFirst((File) null));
        assertThrows(NullPointerException.class, () -> Traverser.PATHS.depthFirstPreOrder((Path) null));
    }
}
