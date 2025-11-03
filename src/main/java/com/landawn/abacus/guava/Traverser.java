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
import java.util.Arrays;
import java.util.function.Function;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Stream;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 * 
 * Provides methods for traversing a graph or tree structure in various orders.
 * This class wraps Google Guava's Traverser functionality and adapts it to work with
 * abacus-common Stream API for more convenient iteration.
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
 *   <li><b>Breadth-First</b>: Visits all nodes at depth n before visiting nodes at depth n+1</li>
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
 *     List<TreeNode> getChildren() { return null; }
 * }
 * TreeNode root = new TreeNode();
 * Traverser<TreeNode> treeTraverser = Traverser.forTree(node -> node.getChildren());
 * Stream<TreeNode> allNodes = treeTraverser.depthFirstPreOrder(root);
 * }</pre>
 * 
 * @param <T> the type of nodes in the graph
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
     * infinite loops if there are circular symbolic links in the file system.
     */
    public static final Traverser<File> FILES = forTree(t -> {
        final File[] subFiles = t.listFiles();
        return N.isEmpty(subFiles) ? N.emptyList() : Arrays.asList(subFiles);
    });

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
     * <p>because there are two paths from {@code b} to {@code f} ({@code b->d->f} and {@code
     * b-&gt;e-&gt;f}).
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
     * @param <T> the type of nodes in the tree
     * @param tree {@link Function} representing a directed acyclic graph that has at most
     *     one path between any two nodes. The function takes a node and returns its children.
     * @return a new Traverser for the specified tree structure
     * @see #forGraph(Function)
     */
    public static <T> Traverser<T> forTree(final Function<? super T, ? extends Iterable<T>> tree) {
        return new Traverser<>(com.google.common.graph.Traverser.forGraph(tree::apply));
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
     *     .limit(1000)  // Limit traversal
     *     .toSet();
     * }</pre>
     *
     * @param <T> the type of nodes in the graph
     * @param graph {@link Function} representing a general graph that may have cycles.
     *     The function takes a node and returns its adjacent nodes (successors).
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
     * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
     * the order {@code abcdef} (assuming successors are returned in alphabetical order).
     *
     * <pre>{@code
     * b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
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
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     * @see #depthFirstPreOrder(Object)
     * @see #depthFirstPostOrder(Object)
     */
    public Stream<T> breadthFirst(final T startNode) {
        return Stream.of(gTraverser.breadthFirst(startNode).iterator());
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
     * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
     * the order {@code abecfd} (assuming successors are returned in alphabetical order).
     *
     * <pre>{@code
     * b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
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
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     * @see #breadthFirst(Object)
     * @see #depthFirstPostOrder(Object)
     */
    public Stream<T> depthFirstPreOrder(final T startNode) {
        return Stream.of(gTraverser.depthFirstPreOrder(startNode).iterator());
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
     * <p><b>Example:</b> The following graph with {@code startNode} {@code a} would return nodes in
     * the order {@code fcebda} (assuming successors are returned in alphabetical order).
     *
     * <pre>{@code
     * b ---- a ---- d
     * |      |
     * |      |
     * e ---- c ---- f
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
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     * @see #breadthFirst(Object)
     * @see #depthFirstPreOrder(Object)
     */
    public Stream<T> depthFirstPostOrder(final T startNode) {
        return Stream.of(gTraverser.depthFirstPostOrder(startNode).iterator());
    }
}
