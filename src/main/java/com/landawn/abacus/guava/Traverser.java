/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.guava;

import java.io.File;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Path;
import java.util.function.Function;

import com.landawn.abacus.util.stream.Stream;

/**
 * Provides methods for traversing a graph or tree structure in various orders.
 * This class wraps Google Guava's Traverser functionality and adapts it to work with
 * the abacus-common Stream API for more convenient iteration.
 *
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 *
 * <p>This class supports two main types of graph structures:
 * <ul>
 *   <li><b>Trees</b>: Directed acyclic graphs where there is at most one path between any two nodes.
 *       Use {@link #forTree(Function)} for better performance with tree structures.</li>
 *   <li><b>General Graphs</b>: May contain cycles and multiple paths between nodes.
 *       Use {@link #forGraph(Function)} for these structures.</li>
 * </ul>
 *
 * <p>Three traversal orders are supported:
 * <ul>
 *   <li><b>Breadth-First</b>: Visits all nodes at depth {@code n} before visiting nodes at depth {@code n+1}</li>
 *   <li><b>Depth-First Pre-Order</b>: Visits nodes as they are first encountered</li>
 *   <li><b>Depth-First Post-Order</b>: Visits nodes after all their descendants have been visited</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Traverse a file system
 * Traverser<File> fileTraverser = Traverser.FILES;
 * fileTraverser.breadthFirst(new File("/home"))
 *     .filter(File::isFile)
 *     .forEach(System.out::println);
 *
 * // Create a custom tree traverser
 * class TreeNode {
 *     List<TreeNode> children = new ArrayList<>();
 *     List<TreeNode> getChildren() { return children; } // return an empty list (not null) for leaf nodes
 * }
 * TreeNode root = new TreeNode();
 * Traverser<TreeNode> treeTraverser = Traverser.forTree(node -> node.getChildren());
 * Stream<TreeNode> allNodes = treeTraverser.depthFirstPreOrder(root);
 * }</pre>
 *
 * @param <T> the type of nodes in the graph
 * @see com.google.common.graph.Traverser
 * @see <a href="http://google.github.io/guava/releases/23.3-android/api/docs/">Guava Documentation</a>
 */
public final class Traverser<T> {

    /**
     * A pre-configured traverser for traversing file system directories and files.
     * This traverser treats the file system as a tree where directories are internal nodes
     * and files are leaf nodes.
     *
     * <p>The traverser uses {@link File#listFiles()} to get children of directories.
     * Files (non-directories) are treated as leaf nodes with no children.
     * If a directory cannot be read, it is treated as having no children.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all .java files in a directory tree
     * Traverser.FILES.breadthFirst(new File("src"))
     *     .filter(f -> f.getName().endsWith(".java"))
     *     .forEach(System.out::println);
     *
     * // Count total size of all files
     * long totalSize = Traverser.FILES.depthFirstPreOrder(new File("/home/user"))
     *     .filter(File::isFile)
     *     .mapToLong(File::length)
     *     .sum();
     * }</pre>
     *
     * <p><b>Note:</b> This traverser may follow symbolic links, which could lead to
     * infinite loops if there are circular symbolic links in the file system. For a
     * symbolic-link-aware, {@link Path}-based alternative, see {@link #PATHS}.
     *
     * @see #PATHS
     */
    public static final Traverser<File> FILES = wrap(com.google.common.io.Files.fileTraverser());

    /**
     * A pre-configured traverser for traversing file system directories and files using the NIO
     * {@link Path} API. This traverser treats the file system as a tree where directories are
     * internal nodes and files are leaf nodes.
     *
     * <p>This is the {@link Path}-based counterpart of {@link #FILES}. Unlike {@link #FILES}, this
     * traverser attempts to avoid following symbolic links to directories. However, it cannot
     * guarantee that it will not follow symbolic links to directories, as it is possible for a
     * directory to be replaced with a symbolic link between checking if the file is a directory and
     * actually reading the contents of that directory.
     *
     * <p>If a {@link Path} passed to one of the traversal methods does not exist or is not a
     * directory, no exception is thrown and the traversal yields a single element: that path.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find all .java files in a directory tree
     * Traverser.PATHS.breadthFirst(Paths.get("src"))
     *     .filter(p -> p.toString().endsWith(".java"))
     *     .forEach(System.out::println);
     * }</pre>
     *
     * <p><b>Note:</b> {@link DirectoryIteratorException} may be thrown while consuming the returned
     * stream if an {@link java.io.IOException} occurs while reading a directory's contents.
     *
     * @see #FILES
     */
    public static final Traverser<Path> PATHS = wrap(com.google.common.io.MoreFiles.fileTraverser());

    private final com.google.common.graph.Traverser<T> gTraverser;

    /**
     * Creates a new Traverser wrapping a Guava traverser.
     * This constructor is private to ensure traversers are created through the factory methods.
     *
     * @param gTraverser the Guava traverser to wrap
     */
    private Traverser(final com.google.common.graph.Traverser<T> gTraverser) {
        this.gTraverser = gTraverser;
    }

    /**
     * Wraps an existing Guava {@link com.google.common.graph.Traverser} in a library
     * {@code Traverser}. This package-private factory lets the {@code guava} package expose a single
     * {@code Traverser} type while reusing Guava's pre-built traversers (for example
     * {@code com.google.common.io.Files.fileTraverser()} and
     * {@code com.google.common.io.MoreFiles.fileTraverser()}), which are wrapped by {@link #FILES}
     * and {@link #PATHS} respectively.
     *
     * @param <T> the type of nodes in the graph
     * @param gTraverser the Guava traverser to wrap
     * @return a new library Traverser delegating to the given Guava traverser
     */
    static <T> Traverser<T> wrap(final com.google.common.graph.Traverser<T> gTraverser) {
        return new Traverser<>(gTraverser);
    }

    /**
     * Creates a new traverser for a directed acyclic graph that has at most one path from the start
     * node to any node reachable from the start node, such as a tree.
     *
     * <p>This method is optimized for tree-like structures and will perform better than
     * {@link #forGraph(Function)} when the graph is known to be a tree. The optimization
     * comes from not needing to track visited nodes.
     *
     * <p>Providing graphs that don't conform to the tree structure may lead to:
     * <ul>
     *   <li>Traversal not terminating (if the graph has cycles)</li>
     *   <li>Nodes being visited multiple times (if multiple paths exist from the start node to any
     *       node reachable from it)</li>
     * </ul>
     *
     * In these cases, use {@link #forGraph(Function)} instead.
     *
     * <p><b>Performance notes</b>
     * <ul>
     *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
     *       the start node).</li>
     *   <li>While traversing, the traverser will use <i>O(H)</i> space (where <i>H</i> is the number
     *       of nodes that have been seen but not yet visited, that is, the "horizon").</li>
     * </ul>
     *
     * <p><b>Examples of valid and invalid tree structures:</b>
     *
     * <p>This is a valid input graph (all edges are directed facing downwards):
     * <pre>{@code
     *    a     b      c
     *   / \   / \     |
     *  /   \ /   \    |
     * d     e     f   g
     *       |
     *       |
     *       h
     * }</pre>
     *
     * <p>This is <b>not</b> a valid input graph (all edges are directed facing downwards):
     * <pre>{@code
     *    a     b
     *   / \   / \
     *  /   \ /   \
     * c     d     e
     *        \   /
     *         \ /
     *          f
     * }</pre>
     *
     * <p>because there are two paths from {@code b} to {@code f} ({@code b->d->f} and {@code b->e->f}).
     *
     * <p><b>Note on binary trees:</b>
     *
     * <p>This method can be used to traverse over a binary tree. Given methods {@code
     * leftChild(node)} and {@code rightChild(node)}, this method can be called as:
     *
     * <pre>{@code
     * Traverser.forTree(node -> Arrays.asList(leftChild(node), rightChild(node)));
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Traverse an organizational hierarchy
     * class Employee {
     *     String name;
     *     List<Employee> directReports;
     *     List<Employee> getDirectReports() { return directReports; }
     *     String getName() { return name; }
     * }
     *
     * Employee ceo = new Employee();
     * Traverser<Employee> orgTraverser = Traverser.forTree(Employee::getDirectReports);
     * orgTraverser.breadthFirst(ceo)
     *     .forEach(employee -> System.out.println(employee.getName()));
     * }</pre>
     *
     * <p><b>Note:</b> the successor function must not return {@code null}; return an empty
     * {@link Iterable} for leaf nodes. A {@code null} return is not guarded here and causes a
     * {@link NullPointerException} during stream consumption, not at factory-creation time.
     *
     * @param <T> the type of nodes in the tree
     * @param tree {@link Function} representing a directed acyclic graph that has at most
     *     one path between any two nodes. The function takes a node and returns its children
     *     (an empty {@link Iterable} for leaf nodes; must not return {@code null}).
     * @return a new Traverser for the specified tree structure
     * @see #forGraph(Function)
     */
    public static <T> Traverser<T> forTree(final Function<? super T, ? extends Iterable<T>> tree) {
        return new Traverser<>(com.google.common.graph.Traverser.forTree(tree::apply));
    }

    /**
     * Creates a new traverser for the given general graph that may contain cycles.
     *
     * <p>This method should be used when the graph may have cycles or multiple paths
     * between nodes. It uses additional memory to track visited nodes and prevent
     * infinite loops.
     *
     * <p>If {@code graph} is known to be tree-shaped, consider using {@link #forTree(Function)} instead
     * for better performance.
     *
     * <p><b>Performance notes</b>
     * <ul>
     *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
     *       the start node), assuming that the node objects have <i>O(1)</i> {@code equals()} and
     *       {@code hashCode()} implementations.</li>
     *   <li>While traversing, the traverser will use <i>O(n)</i> space (where <i>n</i> is the number
     *       of nodes that have thus far been visited), plus <i>O(H)</i> space (where <i>H</i> is the
     *       number of nodes that have been seen but not yet visited, that is, the "horizon").</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Traverse a social network graph (which may have cycles)
     * class Person {
     *     List<Person> friends;
     *     List<Person> getFriends() { return friends; }
     * }
     *
     * Person person = new Person();
     * Traverser<Person> socialTraverser = Traverser.forGraph(Person::getFriends);
     *
     * // Find all people within 3 degrees of separation
     * Set<Person> network = socialTraverser.breadthFirst(person)
     *     .limit(1000)  // stops after 1000 nodes
     *     .toSet();
     * }</pre>
     *
     * <p><b>Note:</b> the successor function must not return {@code null}; return an empty
     * {@link Iterable} for nodes with no successors. A {@code null} return is not guarded here
     * and causes a {@link NullPointerException} during stream consumption, not at
     * factory-creation time.
     *
     * @param <T> the type of nodes in the graph
     * @param graph {@link Function} representing a general graph that may have cycles.
     *     The function takes a node and returns its adjacent nodes (successors); return an empty
     *     {@link Iterable} for nodes with no successors (must not return {@code null}).
     * @return a new Traverser for the specified graph structure
     * @see #forTree(Function)
     */
    public static <T> Traverser<T> forGraph(final Function<? super T, ? extends Iterable<T>> graph) {
        return new Traverser<>(com.google.common.graph.Traverser.forGraph(graph::apply));
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from {@code startNode}, in
     * the order of a breadth-first traversal. That is, all the nodes of depth 0 are returned, then
     * depth 1, then 2, and so on.
     *
     * <p>Breadth-first traversal is useful when you want to explore nodes level by level,
     * such as finding the shortest path or exploring nodes in increasing distance from the start.
     *
     * <p><b>Example:</b> For the following directed tree (edges point downward) with
     * {@code startNode} {@code a}, and assuming each node's successors are returned in alphabetical
     * order, breadth-first traversal returns the nodes in the order {@code a, b, c, d, e, f}
     * ({@code a} at depth 0; then {@code b, c} at depth 1; then {@code d, e, f} at depth 2):
     *
     * <pre>{@code
     *       a
     *      / \
     *     b   c
     *    / \   \
     *   d   e   f
     * }</pre>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated, meaning traversal happens as elements
     * are consumed from the stream. This allows for efficient operations like limiting the
     * number of nodes traversed:
     *
     * <pre>{@code
     * // Only traverse the first 100 nodes
     * // Assuming: TreeNode startNode; void processNode(TreeNode node);
     * Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
     * traverser.breadthFirst(startNode)
     *     .limit(100)
     *     .forEach(node -> processNode(node));
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia</a> for more
     * info.
     *
     * @param startNode the node to start traversal from
     * @return a Stream of nodes in breadth-first order
     * @see #breadthFirst(Iterable)
     * @see #depthFirstPreOrder(Object)
     * @see #depthFirstPostOrder(Object)
     */
    public Stream<T> breadthFirst(final T startNode) {
        return Stream.of(gTraverser.breadthFirst(startNode).iterator());
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from any of the given {@code startNodes}, in
     * the order of a breadth-first traversal. This is the multi-root counterpart of
     * {@link #breadthFirst(Object)}: it behaves as if a virtual root with edges to each of the
     * {@code startNodes} were traversed, except that the virtual root itself is not returned. The
     * {@code startNodes} themselves are visited in the order they are provided (all at depth 0),
     * before their successors.
     *
     * <p>This is useful for traversing a forest of trees, or a graph from a set of seed nodes.</p>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated, meaning traversal happens as elements
     * are consumed from the stream.
     *
     * @param startNodes the nodes to start traversal from
     * @return a Stream of nodes in breadth-first order
     * @see #breadthFirst(Object)
     * @see #depthFirstPreOrder(Iterable)
     * @see #depthFirstPostOrder(Iterable)
     */
    public Stream<T> breadthFirst(final Iterable<? extends T> startNodes) {
        return Stream.of(gTraverser.breadthFirst(startNodes).iterator());
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from {@code startNode}, in
     * the order of a depth-first pre-order traversal. "Pre-order" implies that nodes appear in the
     * {@code Stream} in the order in which they are first visited.
     *
     * <p>Depth-first pre-order traversal is useful for operations where you want to process
     * a node before processing its descendants, such as copying a tree structure or
     * printing a hierarchical representation.
     *
     * <p><b>Example:</b> For the following directed tree (edges point downward) with
     * {@code startNode} {@code a}, and assuming each node's successors are returned in alphabetical
     * order, depth-first pre-order traversal returns the nodes in the order {@code a, b, d, e, c, f}
     * (each node is emitted before its descendants; the {@code b} subtree is fully visited before
     * {@code c}):
     *
     * <pre>{@code
     *       a
     *      / \
     *     b   c
     *    / \   \
     *   d   e   f
     * }</pre>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated, allowing for efficient operations:
     *
     * <pre>{@code
     * // Find the first node matching a condition
     * // Assuming: TreeNode root; boolean meetsCondition(TreeNode node);
     * Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
     * Optional<TreeNode> found = traverser.depthFirstPreOrder(root)
     *     .filter(node -> meetsCondition(node))
     *     .findFirst();
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
     *
     * @param startNode the node to start traversal from
     * @return a Stream of nodes in depth-first pre-order
     * @see #depthFirstPreOrder(Iterable)
     * @see #breadthFirst(Object)
     * @see #depthFirstPostOrder(Object)
     */
    public Stream<T> depthFirstPreOrder(final T startNode) {
        return Stream.of(gTraverser.depthFirstPreOrder(startNode).iterator());
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from any of the given {@code startNodes}, in
     * the order of a depth-first pre-order traversal. This is the multi-root counterpart of
     * {@link #depthFirstPreOrder(Object)}: it behaves as if a virtual root with edges to each of the
     * {@code startNodes} were traversed, except that the virtual root itself is not returned. Each
     * {@code startNode}'s subtree is fully visited (pre-order) before moving on to the next
     * {@code startNode}.
     *
     * <p>This is useful for traversing a forest of trees, or a graph from a set of seed nodes.</p>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated.
     *
     * @param startNodes the nodes to start traversal from
     * @return a Stream of nodes in depth-first pre-order
     * @see #depthFirstPreOrder(Object)
     * @see #breadthFirst(Iterable)
     * @see #depthFirstPostOrder(Iterable)
     */
    public Stream<T> depthFirstPreOrder(final Iterable<? extends T> startNodes) {
        return Stream.of(gTraverser.depthFirstPreOrder(startNodes).iterator());
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from {@code startNode}, in
     * the order of a depth-first post-order traversal. "Post-order" implies that nodes appear in the
     * {@code Stream} in the order in which they are visited for the last time.
     *
     * <p>Depth-first post-order traversal is useful for operations where you want to process
     * a node after processing all its descendants, such as computing sizes of subtrees,
     * deleting a tree structure, or performing bottom-up computations.
     *
     * <p><b>Example:</b> For the following directed tree (edges point downward) with
     * {@code startNode} {@code a}, and assuming each node's successors are returned in alphabetical
     * order, depth-first post-order traversal returns the nodes in the order {@code d, e, b, f, c, a}
     * (each node is emitted only after all of its descendants):
     *
     * <pre>{@code
     *       a
     *      / \
     *     b   c
     *    / \   \
     *   d   e   f
     * }</pre>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated. Common use cases include:
     *
     * <pre>{@code
     * // Calculate sizes of all subtrees
     * // Assuming: TreeNode root; List<TreeNode> getChildren(TreeNode node);
     * Traverser<TreeNode> traverser = Traverser.forTree(TreeNode::getChildren);
     * Map<TreeNode, Integer> sizes = new HashMap<>();
     * traverser.depthFirstPostOrder(root)
     *     .forEach(node -> {
     *         int size = 1 + getChildren(node).stream()
     *             .mapToInt(sizes::get)
     *             .sum();
     *         sizes.put(node, size);
     *     });
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
     *
     * @param startNode the node to start traversal from
     * @return a Stream of nodes in depth-first post-order
     * @see #depthFirstPostOrder(Iterable)
     * @see #breadthFirst(Object)
     * @see #depthFirstPreOrder(Object)
     */
    public Stream<T> depthFirstPostOrder(final T startNode) {
        return Stream.of(gTraverser.depthFirstPostOrder(startNode).iterator());
    }

    /**
     * Returns a {@link Stream} over the nodes reachable from any of the given {@code startNodes}, in
     * the order of a depth-first post-order traversal. This is the multi-root counterpart of
     * {@link #depthFirstPostOrder(Object)}: it behaves as if a virtual root with edges to each of the
     * {@code startNodes} were traversed, except that the virtual root itself is not returned. Each
     * {@code startNode}'s subtree is fully visited (post-order, so descendants precede the
     * {@code startNode}) before moving on to the next {@code startNode}.
     *
     * <p>This is useful for traversing a forest of trees, or a graph from a set of seed nodes.</p>
     *
     * <p>The behavior of this method is undefined if the nodes, or the topology of the graph, change
     * while iteration is in progress.
     *
     * <p>The returned {@code Stream} is lazily evaluated.
     *
     * @param startNodes the nodes to start traversal from
     * @return a Stream of nodes in depth-first post-order
     * @see #depthFirstPostOrder(Object)
     * @see #breadthFirst(Iterable)
     * @see #depthFirstPreOrder(Iterable)
     */
    public Stream<T> depthFirstPostOrder(final Iterable<? extends T> startNodes) {
        return Stream.of(gTraverser.depthFirstPostOrder(startNodes).iterator());
    }
}
