/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.common.graph.SuccessorsFunction;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Stream;

/**
 * Provides methods for traversing a graph.
 * @param <T> Node parameter type
 *
 * @see <a href="http://google.github.io/guava/releases/23.3-android/api/docs/">http://google.github.io/guava/releases/23.3-android/api/docs/</a>
 */
public final class Traverser<T> {

    public static final Traverser<File> FILES = forTree(t -> {
        File[] subFiles = t.listFiles();
        return N.isNullOrEmpty(subFiles) ? N.<File> emptyList() : Arrays.asList(subFiles);
    });

    private final com.google.common.graph.Traverser<T> gTraverser;

    private Traverser(com.google.common.graph.Traverser<T> gTraverser) {
        this.gTraverser = gTraverser;
    }

    /**
     * Creates a new traverser for a directed acyclic graph that has at most one path from the start
     * node to any node reachable from the start node, such as a tree.
     *
     * <p>Providing graphs that don't conform to the above description may lead to:
     *
     * <ul>
     *   <li>Traversal not terminating (if the graph has cycles)
     *   <li>Nodes being visited multiple times (if multiple paths exist from the start node to any
     *       node reachable from it)
     * </ul>
     *
     * In these cases, use {@link #forGraph(SuccessorsFunction)} instead.
     *
     * <p><b>Performance notes</b>
     *
     * <ul>
     *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
     *       the start node).
     *   <li>While traversing, the traverser will use <i>O(H)</i> space (where <i>H</i> is the number
     *       of nodes that have been seen but not yet visited, that is, the "horizon").
     * </ul>
     *
     * <p><b>Examples</b>
     *
     * <p>This is a valid input graph (all edges are directed facing downwards):
     *
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
     *
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
     * b->e->f}).
     *
     * <p><b>Note on binary trees</b>
     *
     * <p>This method can be used to traverse over a binary tree. Given methods {@code
     * leftChild(node)} and {@code rightChild(node)}, this method can be called as
     *
     * <pre>{@code
     * Traverser.forTree(node -> ImmutableList.of(leftChild(node), rightChild(node)));
     * }</pre>
     *
     * @param <T>
     * @param tree {@link SuccessorsFunction} representing a directed acyclic graph that has at most
     *     one path between any two nodes
     * @return
     */
    public static <T> Traverser<T> forTree(final Function<? super T, Iterable<? extends T>> tree) {
        return new Traverser<>(com.google.common.graph.Traverser.forGraph(node -> tree.apply(node)));
    }

    /**
     * Creates a new traverser for the given general {@code graph}.
     *
     * <p>If {@code graph} is known to be tree-shaped, consider using {@link
     * #forTree(SuccessorsFunction)} instead.
     *
     * <p><b>Performance notes</b>
     *
     * <ul>
     *   <li>Traversals require <i>O(n)</i> time (where <i>n</i> is the number of nodes reachable from
     *       the start node), assuming that the node objects have <i>O(1)</i> {@code equals()} and
     *       {@code hashCode()} implementations.
     *   <li>While traversing, the traverser will use <i>O(n)</i> space (where <i>n</i> is the number
     *       of nodes that have thus far been visited), plus <i>O(H)</i> space (where <i>H</i> is the
     *       number of nodes that have been seen but not yet visited, that is, the "horizon").
     * </ul>
     *
     * @param <T>
     * @param graph {@link SuccessorsFunction} representing a general graph that may have cycles.
     * @return
     */
    public static <T> Traverser<T> forGraph(final Function<? super T, Iterable<? extends T>> graph) {
        return new Traverser<>(com.google.common.graph.Traverser.forGraph(node -> graph.apply(node)));
    }

    /**
     * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
     * the order of a breadth-first traversal. That is, all the nodes of depth 0 are returned, then
     * depth 1, then 2, and so on.
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
     * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>{@code
     * Iterables.limit(Traverser.forGraph(graph).breadthFirst(node), maxNumberOfNodes);
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia</a> for more
     * info.
     *
     * @param startNode
     * @return
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     */
    public Stream<T> breadthFirst(T startNode) {
        return Stream.of(gTraverser.breadthFirst(startNode).iterator());
    }

    /**
     * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
     * the order of a depth-first pre-order traversal. "Pre-order" implies that nodes appear in the
     * {@code Iterable} in the order in which they are first visited.
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
     * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>{@code
     * Iterables.limit(
     *     Traverser.forGraph(graph).depthFirstPreOrder(node), maxNumberOfNodes);
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
     *
     * @param startNode
     * @return
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     */
    public Stream<T> depthFirstPreOrder(T startNode) {
        return Stream.of(gTraverser.depthFirstPreOrder(startNode).iterator());
    }

    /**
     * Returns an unmodifiable {@code Iterable} over the nodes reachable from {@code startNode}, in
     * the order of a depth-first post-order traversal. "Post-order" implies that nodes appear in the
     * {@code Iterable} in the order in which they are visited for the last time.
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
     * <p>The returned {@code Iterable} can be iterated over multiple times. Every iterator will
     * compute its next element on the fly. It is thus possible to limit the traversal to a certain
     * number of nodes as follows:
     *
     * <pre>{@code
     * Iterables.limit(
     *     Traverser.forGraph(graph).depthFirstPostOrder(node), maxNumberOfNodes);
     * }</pre>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.
     *
     * @param startNode
     * @return
     * @throws IllegalArgumentException if {@code startNode} is not an element of the graph
     */
    public Stream<T> depthFirstPostOrder(T startNode) {
        return Stream.of(gTraverser.depthFirstPostOrder(startNode).iterator());
    }
}
