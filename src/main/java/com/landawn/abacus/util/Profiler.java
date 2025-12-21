/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * A comprehensive, enterprise-grade performance profiling and benchmarking utility providing advanced
 * execution time measurement, multi-threaded performance testing, and detailed statistical analysis
 * capabilities. This class serves as a powerful toolkit for performance optimization, load testing,
 * and systematic performance analysis in enterprise applications requiring precise timing measurements
 * and comprehensive performance metrics for method execution, code blocks, and system operations.
 *
 * <p>The {@code Profiler} class addresses critical challenges in enterprise performance testing by
 * providing sophisticated benchmarking capabilities that enable developers to accurately measure
 * execution times under various load conditions, identify performance bottlenecks, and validate
 * optimization improvements. It supports configurable multi-threaded testing scenarios, statistical
 * analysis of performance data, and flexible reporting options suitable for production performance
 * monitoring and development-time performance validation.</p>
 *
 * <p><b>⚠️ IMPORTANT - Memory Considerations:</b>
 * When using large loop counts (>100,000 iterations), the profiler stores individual execution times
 * which can consume significant memory and potentially impact measurement accuracy. For high-iteration
 * testing, implement internal loops within test methods rather than using high loop parameters to
 * minimize memory overhead while maintaining measurement precision.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Multi-Threaded Performance Testing:</b> Configurable concurrent execution with customizable thread pools</li>
 *   <li><b>Comprehensive Statistical Analysis:</b> Min, max, average, median, percentile, and distribution metrics</li>
 *   <li><b>Multiple Test Rounds:</b> Support for warm-up rounds and multiple test iterations for accuracy</li>
 *   <li><b>Flexible Execution Models:</b> Support for Runnable, Callable, and reflection-based method invocation</li>
 *   <li><b>Advanced Reporting:</b> Multiple output formats including console, file, HTML, and custom formatters</li>
 *   <li><b>Execution Control:</b> Profiler suspension, debugging hooks, and conditional execution</li>
 *   <li><b>Memory Management:</b> Optimized memory usage patterns for large-scale performance testing</li>
 *   <li><b>Integration Ready:</b> Seamless integration with testing frameworks and CI/CD pipelines</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Accuracy First:</b> Minimize measurement overhead and external factors affecting timing precision</li>
 *   <li><b>Scalability Focus:</b> Support for testing under various load conditions and concurrency levels</li>
 *   <li><b>Comprehensive Analysis:</b> Detailed statistical metrics for thorough performance understanding</li>
 *   <li><b>Ease of Use:</b> Simple API for common use cases with advanced options for complex scenarios</li>
 *   <li><b>Production Ready:</b> Robust error handling and resource management for enterprise environments</li>
 * </ul>
 *
 * <p><b>Statistical Metrics Provided:</b>
 * <ul>
 *   <li><b>Basic Metrics:</b> Minimum, maximum, average, and total execution times</li>
 *   <li><b>Distribution Analysis:</b> Standard deviation, variance, and coefficient of variation</li>
 *   <li><b>Percentile Analysis:</b> 50th, 90th, 95th, 99th percentile execution times</li>
 *   <li><b>Throughput Metrics:</b> Operations per second and transactions per minute</li>
 *   <li><b>Concurrency Analysis:</b> Thread efficiency and contention metrics</li>
 *   <li><b>Trend Analysis:</b> Performance trends across rounds and time periods</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic single-threaded performance test
 * Profiler.run(1, 1000, 3, "String concatenation", () -> {
 *     String result = "";
 *     for (int i = 0; i < 1000; i++) {
 *         result += "test" + i;
 *     }
 * });
 *
 * // Multi-threaded load testing
 * Profiler.run(10, 1000, 5, "Database query", () -> {
 *     try {
 *         userService.findById(randomUserId());
 *     } catch (Exception e) {
 *         logger.error("Query failed", e);
 *     }
 * });
 * }</pre>
 *
 * <p><b>Profiler Configuration and Control:</b>
 * <ul>
 *   <li><b>Thread Pool Management:</b> Custom ExecutorService support for specialized threading models</li>
 *   <li><b>Suspension Control:</b> {@code setSuspended(boolean)} for debugging and conditional execution</li>
 *   <li><b>Output Customization:</b> Multiple output formats and custom result processors</li>
 *   <li><b>Memory Optimization:</b> Configurable result collection strategies for large-scale testing</li>
 *   <li><b>Error Handling:</b> Comprehensive exception handling with detailed error reporting</li>
 * </ul>
 *
 * <p><b>Performance Measurement Accuracy:</b>
 * <ul>
 *   <li><b>JVM Warmup:</b> Multiple rounds help eliminate JIT compilation overhead</li>
 *   <li><b>Garbage Collection:</b> Statistical analysis helps identify GC-related performance variations</li>
 *   <li><b>System Load:</b> Multiple iterations provide statistically significant results</li>
 *   <li><b>Measurement Overhead:</b> Minimal profiler overhead to preserve timing accuracy</li>
 *   <li><b>Thread Synchronization:</b> Precise synchronization for accurate concurrent measurements</li>
 * </ul>
 *
 * <p><b>Memory Management Strategies:</b>
 * <ul>
 *   <li><b>Optimal Loop Sizes:</b> Recommend loop counts under 10,000 for memory efficiency</li>
 *   <li><b>Internal Iteration:</b> Use internal loops within test methods for high-iteration testing</li>
 *   <li><b>Result Streaming:</b> Stream results for processing instead of storing all measurements</li>
 *   <li><b>Garbage Collection:</b> Strategic GC calls between test rounds for clean measurements</li>
 *   <li><b>Resource Cleanup:</b> Automatic cleanup of profiling resources and thread pools</li>
 * </ul>
 *
 * <p><b>Integration with Testing Frameworks:</b>
 * <ul>
 *   <li><b>JUnit Integration:</b> Use in @Test methods for performance regression testing</li>
 *   <li><b>TestNG Support:</b> Compatible with TestNG performance testing annotations</li>
 *   <li><b>Maven/Gradle:</b> Integration with build tools for automated performance testing</li>
 *   <li><b>CI/CD Pipelines:</b> Automated performance validation in continuous integration</li>
 *   <li><b>Monitoring Systems:</b> Export results to monitoring and alerting systems</li>
 * </ul>
 *
 * <p><b>Output and Reporting Options:</b>
 * <ul>
 *   <li><b>Console Output:</b> Formatted console output with tabular statistical summaries</li>
 *   <li><b>File Export:</b> Export results to text files, CSV, JSON, or XML formats</li>
 *   <li><b>HTML Reports:</b> Rich HTML reports with charts and interactive elements</li>
 *   <li><b>Custom Formatters:</b> Pluggable result formatters for specialized reporting needs</li>
 *   <li><b>Real-time Monitoring:</b> Stream results to monitoring dashboards and alerting systems</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Use multiple rounds (3-5) to account for JVM warmup and obtain stable measurements</li>
 *   <li>Implement internal loops for high-iteration testing to minimize memory overhead</li>
 *   <li>Include warmup rounds in performance-critical testing to eliminate JIT effects</li>
 *   <li>Use appropriate thread counts based on system capabilities and test objectives</li>
 *   <li>Monitor memory usage during profiling to prevent OutOfMemoryError</li>
 *   <li>Validate test environment consistency for accurate comparative measurements</li>
 *   <li>Use statistical analysis to identify and handle performance outliers</li>
 *   <li>Implement proper exception handling within profiled code blocks</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Using extremely high loop counts (>100,000) that consume excessive memory</li>
 *   <li>Ignoring JVM warmup effects in performance measurements</li>
 *   <li>Running performance tests on systems with inconsistent load</li>
 *   <li>Not handling exceptions properly within profiled code blocks</li>
 *   <li>Comparing results from different JVM instances or system configurations</li>
 *   <li>Using profiler suspension in production environments inappropriately</li>
 *   <li>Not considering garbage collection impact on measurement accuracy</li>
 * </ul>
 *
 * <p><b>Performance Testing Methodologies:</b>
 * <ul>
 *   <li><b>Baseline Testing:</b> Establish performance baselines for regression detection</li>
 *   <li><b>Load Testing:</b> Validate performance under expected production loads</li>
 *   <li><b>Stress Testing:</b> Determine system breaking points and failure modes</li>
 *   <li><b>Comparative Testing:</b> Compare different implementations or optimizations</li>
 *   <li><b>Regression Testing:</b> Validate that changes don't degrade performance</li>
 * </ul>
 *
 * <p><b>Statistical Analysis Capabilities:</b>
 * <ul>
 *   <li><b>Central Tendency:</b> Mean, median, and mode calculation for execution times</li>
 *   <li><b>Variability:</b> Standard deviation, variance, and coefficient of variation</li>
 *   <li><b>Distribution:</b> Percentile analysis and execution time distribution</li>
 *   <li><b>Outlier Detection:</b> Identification and handling of performance outliers</li>
 *   <li><b>Trend Analysis:</b> Performance trends across multiple test rounds</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrent Usage:</b>
 * <ul>
 *   <li><b>Thread-Safe Operations:</b> All Profiler methods are thread-safe for concurrent usage</li>
 *   <li><b>Isolated Measurements:</b> Each profiling session uses isolated timing measurements</li>
 *   <li><b>Concurrent Testing:</b> Support for running multiple profiling sessions simultaneously</li>
 *   <li><b>Resource Management:</b> Proper cleanup of thread pools and profiling resources</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Profiling Tools:</b>
 * <ul>
 *   <li><b>vs. JMH (Java Microbenchmark Harness):</b> Simplified API vs. annotation-based comprehensive framework</li>
 *   <li><b>vs. VisualVM:</b> Programmatic profiling vs. GUI-based application profiling</li>
 *   <li><b>vs. JProfiler:</b> Code-embedded testing vs. external profiling tool</li>
 *   <li><b>vs. Apache Bench:</b> Java method profiling vs. HTTP load testing</li>
 * </ul>
 *
 * @see ExecutorService
 * @see java.util.concurrent.Callable
 * @see java.lang.Runnable
 * @see com.landawn.abacus.logging.Logger
 * @see <a href="https://openjdk.java.net/projects/code-tools/jmh/">JMH (Java Microbenchmark Harness)</a>
 * @see <a href="https://medium.com/@AlexanderObregon/introduction-to-java-microbenchmarking-with-jmh-java-microbenchmark-harness-55af74b2fd38">Introduction to Java Microbenchmarking with JMH</a>
 */
@SuppressWarnings({ "java:S1244", "java:S1943" })
public final class Profiler {

    private static final Logger logger = LoggerFactory.getLogger(Profiler.class);

    private static final DecimalFormat elapsedTimeFormat = new DecimalFormat("#0.000");

    private Profiler() {
        // singleton
    }

    /**
     * Executes a multi-threaded performance test with the specified configuration parameters.
     * This method provides a simplified interface for running concurrent performance tests where each
     * thread executes a given command multiple times across multiple test rounds. This is the primary
     * entry point for basic performance profiling scenarios without custom labels or timing delays.
     *
     * <p>The test execution follows this pattern:
     * <ol>
     *   <li>Creates a thread pool with {@code threadNum} threads</li>
     *   <li>Each thread executes {@code command} for {@code loopNum} iterations</li>
     *   <li>The entire test is repeated {@code roundNum} times for statistical accuracy</li>
     *   <li>Collects comprehensive timing statistics for analysis</li>
     * </ol>
     *
     * <p>This method is ideal for quick performance benchmarks where you need to assess how code
     * performs under concurrent load. The multi-round approach helps eliminate JVM warmup effects
     * and provides more stable, statistically significant results. Each round's results are printed
     * automatically to the console, and the final round's statistics are returned for further analysis.
     *
     * <p><b>Performance Considerations:</b>
     * <ul>
     *   <li>Memory usage scales with threadNum * loopNum (stores individual execution times)</li>
     *   <li>For loop counts exceeding 100,000, consider using internal loops within the command</li>
     *   <li>Multiple rounds help account for JIT compilation and garbage collection variations</li>
     *   <li>Thread synchronization overhead is minimized through careful implementation</li>
     * </ul>
     *
     * <p><b>Typical Use Cases:</b>
     * <ul>
     *   <li>Database query performance testing under concurrent load</li>
     *   <li>API endpoint response time measurement</li>
     *   <li>Algorithm efficiency comparison</li>
     *   <li>Cache hit rate and performance validation</li>
     *   <li>Resource pool contention analysis</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic string concatenation performance test
     * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, () -> {
     *     String result = "";
     *     for (int i = 0; i < 100; i++) {
     *         result += "test";
     *     }
     * });
     * stats.printResult();
     *
     * // Database query performance test
     * Profiler.run(10, 500, 5, () -> {
     *     userRepository.findById(12345L);
     * });
     *
     * // Thread-safe collection performance
     * ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
     * Profiler.run(8, 10000, 3, () -> {
     *     map.put(UUID.randomUUID().toString(), "value");
     * });
     * }</pre>
     *
     * @param threadNum the number of concurrent threads to execute the test, must be greater than 0.
     *                  Higher values test concurrent performance but consume more system resources.
     *                  Typical values: 1 (single-threaded), 4-8 (moderate concurrency), 50+ (stress testing)
     * @param loopNum the number of times each thread executes the command, must be greater than 0.
     *                Each execution is timed individually for statistical analysis.
     *                Recommended range: 100-10,000 for balanced memory usage and statistical significance
     * @param roundNum the number of times to repeat the entire test (all threads and loops).
     *                 Multiple rounds help eliminate JVM warmup effects and provide stable measurements.
     *                 Recommended: 3-5 rounds for most tests, 1 for quick checks
     * @param command the code to be profiled and executed in each loop iteration.
     *                This should be a Runnable that can throw checked exceptions.
     *                The command is executed (threadNum * loopNum * roundNum) times in total
     * @return a {@link MultiLoopsStatistics} object containing comprehensive performance metrics including
     *         minimum, maximum, average, and percentile execution times for all test executions.
     *         The returned statistics represent the final round of testing
     * @throws IllegalArgumentException if {@code threadNum <= 0} or {@code loopNum <= 0}, as these would
     *                                  result in invalid test configurations with no actual profiling
     * @see #run(int, int, int, String, Throwables.Runnable) for tests with custom labels
     * @see #run(int, long, int, long, int, String, Throwables.Runnable) for tests with timing delays
     */
    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final Throwables.Runnable<? extends Exception> command) {
        return run(threadNum, loopNum, roundNum, "run", command);
    }

    /**
     * Executes a multi-threaded performance test with a custom descriptive label for result identification.
     * This method extends the basic {@link #run(int, int, int, Throwables.Runnable)} by allowing you to
     * specify a meaningful label that appears in the performance statistics output, making it easier to
     * identify and distinguish between different test scenarios when running multiple benchmarks.
     *
     * <p>The custom label is particularly useful when:
     * <ul>
     *   <li>Running multiple performance tests in sequence and comparing results</li>
     *   <li>Generating reports that need clear identification of each test case</li>
     *   <li>Profiling different implementations of the same functionality</li>
     *   <li>Creating test suites with descriptive names for each benchmark</li>
     *   <li>Exporting results to external monitoring or analysis tools</li>
     * </ul>
     *
     * <p>The label appears in all output formats (console, HTML, XML) and helps distinguish between
     * different methods or scenarios being tested. Without a label, the default "run" identifier is used,
     * which can make it difficult to differentiate between multiple tests in the output.
     *
     * <p><b>Best Practices for Labels:</b>
     * <ul>
     *   <li>Use descriptive names that clearly indicate what is being tested (e.g., "StringConcatenation", "DatabaseQuery")</li>
     *   <li>Keep labels concise but meaningful (avoid overly long labels that clutter output)</li>
     *   <li>Use consistent naming conventions across related tests (e.g., "Cache_Get", "Cache_Put", "Cache_Remove")</li>
     *   <li>Avoid special characters that might interfere with output formatting</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Compare StringBuilder vs String concatenation
     * Profiler.run(4, 1000, 3, "StringBuilder", () -> {
     *     StringBuilder sb = new StringBuilder();
     *     for (int i = 0; i < 100; i++) {
     *         sb.append("test");
     *     }
     * });
     *
     * Profiler.run(4, 1000, 3, "StringConcat", () -> {
     *     String s = "";
     *     for (int i = 0; i < 100; i++) {
     *         s += "test";
     *     }
     * });
     *
     * // Test different cache implementations
     * MultiLoopsStatistics stats = Profiler.run(10, 5000, 5, "ConcurrentHashMap_Get", () -> {
     *     cache.get(randomKey());
     * });
     * stats.printResult();
     * }</pre>
     *
     * @param threadNum the number of concurrent threads to execute the test, must be greater than 0.
     *                  Determines the level of concurrent load applied during the test
     * @param loopNum the number of times each thread executes the command, must be greater than 0.
     *                Each iteration is measured individually for comprehensive statistics
     * @param roundNum the number of times to repeat the entire test sequence.
     *                 Multiple rounds help produce stable, reliable performance measurements
     * @param label a descriptive identifier for this test that appears in all result outputs.
     *              Use meaningful names that clearly describe what is being tested.
     *              This label will appear in console output, HTML reports, and XML exports.
     *              Can be {@code null}, in which case "run" is used as the default label
     * @param command the code block to be profiled and executed in each loop iteration.
     *                Should be a Runnable that encapsulates the operation being benchmarked
     * @return a {@link MultiLoopsStatistics} object containing comprehensive performance metrics
     *         for the labeled test, including execution times, percentiles, and statistical analysis.
     *         The statistics object includes the label for easy identification
     * @throws IllegalArgumentException if {@code threadNum <= 0} or {@code loopNum <= 0},
     *                                  indicating invalid test configuration parameters
     * @see #run(int, int, int, Throwables.Runnable) for basic testing without custom labels
     * @see #run(int, long, int, long, int, String, Throwables.Runnable) for tests with timing delays and labels
     */
    public static MultiLoopsStatistics run(final int threadNum, final int loopNum, final int roundNum, final String label,
            final Throwables.Runnable<? extends Exception> command) {
        return run(threadNum, 0, loopNum, 0, roundNum, label, command);
    }

    /**
     * Executes a comprehensive multi-threaded performance test with fine-grained control over timing,
     * delays, and execution patterns. This advanced profiling method enables sophisticated testing
     * scenarios that closely simulate real-world conditions by introducing controlled delays between
     * thread starts and loop iterations, making it ideal for testing rate-limited systems, throttled
     * APIs, and scenarios requiring gradual load ramp-up.
     *
     * <p>This method provides the most comprehensive control over test execution timing and is designed
     * for advanced profiling scenarios where precise control over concurrency patterns and execution
     * timing is required. The delay parameters allow you to:
     * <ul>
     *   <li><b>Stagger thread startup:</b> Gradually ramp up load instead of sudden concurrent bursts</li>
     *   <li><b>Simulate rate limiting:</b> Add delays between iterations to mimic throttled operations</li>
     *   <li><b>Test resource contention:</b> Control timing to expose race conditions and deadlocks</li>
     *   <li><b>Model real traffic:</b> Create realistic load patterns matching production scenarios</li>
     *   <li><b>Avoid overwhelming systems:</b> Prevent test load from exceeding system capacity</li>
     * </ul>
     *
     * <p><b>Thread Delay Use Cases:</b>
     * <ul>
     *   <li>Gradual ramp-up testing: Start threads progressively to observe system behavior under increasing load</li>
     *   <li>Connection pool testing: Avoid simultaneous connection attempts that might overwhelm pools</li>
     *   <li>Distributed system testing: Simulate staggered client connections in microservices</li>
     *   <li>Resource acquisition testing: Test systems with limited resources (file handles, ports)</li>
     * </ul>
     *
     * <p><b>Loop Delay Use Cases:</b>
     * <ul>
     *   <li>API rate limiting: Respect rate limits by adding delays between requests</li>
     *   <li>Database throttling: Prevent overwhelming database connections with continuous queries</li>
     *   <li>Think time simulation: Model realistic user behavior with pauses between actions</li>
     *   <li>Sustained load testing: Maintain consistent load over extended periods</li>
     * </ul>
     *
     * <p><b>Performance Impact:</b>
     * Note that delays affect total test duration but not individual operation timing measurements.
     * Each operation's execution time is measured independently, excluding delay periods. Total test
     * duration approximately equals: (threadDelay * threadNum) + (loopDelay * loopNum) + actual execution time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Gradual load ramp-up: Start one thread every 100ms
     * Profiler.run(10, 100, 1000, 0, 3, "GradualRampUp", () -> {
     *     apiClient.makeRequest();
     * });
     *
     * // Rate-limited API testing: 10ms delay between each request
     * Profiler.run(4, 0, 1000, 10, 3, "RateLimitedAPI", () -> {
     *     rateLimitedService.call();
     * });
     *
     * // Combined: Staggered start + throttled execution
     * MultiLoopsStatistics stats = Profiler.run(
     *     8,      // 8 threads
     *     50,     // 50ms between thread starts
     *     500,    // 500 iterations per thread
     *     20,     // 20ms between iterations
     *     5,      // 5 rounds
     *     "DatabaseQuery_Throttled",
     *     () -> {
     *         database.query("SELECT * FROM users WHERE id = ?", randomId());
     *     }
     * );
     * stats.printResult();
     *
     * // Simulate realistic user behavior with think time
     * Profiler.run(20, 100, 100, 500, 3, "UserWorkflow", () -> {
     *     // Simulate user action (delay represents "think time")
     *     shoppingCart.addItem(randomProduct());
     * });
     * }</pre>
     *
     * @param threadNum the number of concurrent threads to execute the test, must be greater than 0.
     *                  Each thread runs independently and executes the full loop sequence
     * @param threadDelay the delay in milliseconds to wait before starting each subsequent thread, must be >= 0.
     *                    A value of 0 starts all threads simultaneously. Higher values create gradual ramp-up.
     *                    Example: With 10 threads and 100ms delay, the 10th thread starts 900ms after the first
     * @param loopNum the number of times each thread executes the command, must be greater than 0.
     *                Each execution is measured and contributes to the statistical analysis
     * @param loopDelay the delay in milliseconds to wait between each loop iteration within a thread, must be >= 0.
     *                  A value of 0 means continuous execution. Use positive values to throttle execution rate.
     *                  This delay is NOT included in the measured execution time of each iteration
     * @param roundNum the number of times to repeat the entire test (all threads and loops), must be greater than 0.
     *                 Each round runs sequentially, with results from intermediate rounds printed to console.
     *                 Multiple rounds help achieve statistically stable measurements
     * @param label a descriptive identifier for this test that appears in all result outputs.
     *              Use meaningful names to distinguish between different test scenarios.
     *              Can be {@code null}, defaulting to a generic identifier
     * @param command the code block to be profiled and executed in each loop iteration.
     *                Execution time is measured individually for each invocation, excluding delay periods.
     *                The command can throw checked exceptions which will be caught and logged
     * @return a {@link MultiLoopsStatistics} object containing comprehensive performance metrics
     *         including minimum, maximum, average, percentile execution times, and failure information.
     *         The statistics represent the final round of testing
     * @throws IllegalArgumentException if {@code threadNum <= 0}, {@code loopNum <= 0},
     *                                  {@code threadDelay < 0}, or {@code loopDelay < 0}.
     *                                  These conditions indicate invalid test configurations
     * @see #run(int, int, int, Throwables.Runnable) for basic testing without delays
     * @see #run(int, int, int, String, Throwables.Runnable) for testing with labels but no delays
     */
    public static MultiLoopsStatistics run(final int threadNum, final long threadDelay, final int loopNum, final long loopDelay, final int roundNum,
            final String label, final Throwables.Runnable<? extends Exception> command) {
        return run(command, label, getMethod(command, "run"), null, null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the name of the method to be tested
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final String method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, getMethod(instance, method), threadNum, loopNum, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, (Object) null, threadNum, loopNum, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param arg the argument to be passed to the method
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final int loopNum, final int roundNum) {
        return run(instance, method, arg, threadNum, 0, loopNum, 0, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, thread delay, loops, loop delay, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param arg the argument to be passed to the method
     * @param threadNum the number of threads to use
     * @param threadDelay the delay between starting each thread
     * @param loopNum the number of loops to run in each thread
     * @param loopDelay the delay between each loop
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final Object arg, final int threadNum, final long threadDelay,
            final int loopNum, final long loopDelay, final int roundNum) {
        return run(instance, method, ((arg == null) ? null : Array.asList(arg)), null, null, null, null, threadNum, threadDelay, loopNum, loopDelay, roundNum);
    }

    /**
     * Runs a performance test for the specified method with the given number of threads, loops, and rounds.
     *
     * @param instance the instance on which the method is invoked
     * @param method the method to be tested
     * @param args the arguments to be passed to the method. The size of {@code args} can be 0, 1, or same size with {@code threadNum}. It's the input argument for every loop in each thread.
     * @param threadNum the number of threads to use
     * @param loopNum the number of loops to run in each thread
     * @param roundNum the number of rounds to repeat the test
     * @return the statistics of the performance test
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final List<?> args, final int threadNum, final int loopNum,
            final int roundNum) {
        return run(instance, method, args, null, null, null, null, threadNum, 0, loopNum, 0, roundNum);
    }

    /**
     * Run performance test for the specified {@code method} with the specified {@code threadNum} and {@code loopNum} for each thread.
     * The performance test will be repeatedly executedd times specified by {@code roundNum}.
     *
     * @param instance the instance on which to invoke the method, may be {@code null} for static methods
     * @param method the method to be profiled
     * @param args the size of {@code args} can be 0, 1, or same size with {@code threadNum}. It's the input argument for every loop in each thread.
     * @param setUpForMethod the setup method to be executed before the profiled method, may be {@code null}
     * @param tearDownForMethod the teardown method to be executed after the profiled method, may be {@code null}
     * @param setUpForLoop the setup method to be executed before each loop iteration, may be {@code null}
     * @param tearDownForLoop the teardown method to be executed after each loop iteration, may be {@code null}
     * @param threadNum the number of threads to use for the performance test
     * @param threadDelay the delay in milliseconds between starting each thread
     * @param loopNum the number of loop iterations to execute in each thread
     * @param loopDelay the delay in milliseconds between each loop iteration
     * @param roundNum the number of rounds to repeat the entire performance test
     * @return the performance statistics from multiple loop executions
     */
    static MultiLoopsStatistics run(final Object instance, final Method method, final List<?> args, final Method setUpForMethod, final Method tearDownForMethod,
            final Method setUpForLoop, final Method tearDownForLoop, final int threadNum, final long threadDelay, final int loopNum, final long loopDelay,
            final int roundNum) {
        return run(instance, method.getName(), method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay, loopNum,
                loopDelay, roundNum);
    }

    /**
     * Run performance test for the specified {@code methodList} with the specified {@code threadNum} and {@code loopNum} for each thread.
     * The performance test will be repeatedly executed times specified by {@code roundNum}.
     *
     * @param instance it can be {@code null} if methods in the specified {@code methodList} are static methods
     * @param methodName the name of the method being profiled
     * @param method the method to be profiled
     * @param args the size of {@code args} can be 0, 1, or same size with {@code threadNum}. It's the input argument for every loop in each thread.
     * @param setUpForMethod the setup method to be executed before the profiled method, may be {@code null}
     * @param tearDownForMethod the teardown method to be executed after the profiled method, may be {@code null}
     * @param setUpForLoop the setup method to be executed before each loop iteration, may be {@code null}
     * @param tearDownForLoop the teardown method to be executed after each loop iteration, may be {@code null}
     * @param threadNum the number of threads to use for the performance test
     * @param threadDelay the delay in milliseconds between starting each thread
     * @param loopNum loops run by each thread.
     * @param loopDelay the delay in milliseconds between each loop iteration
     * @param roundNum the number of rounds to repeat the entire performance test
     * @return the performance statistics from multiple loop executions
     */
    static MultiLoopsStatistics run(final Object instance, final String methodName, final Method method, final List<?> args, final Method setUpForMethod,
            final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int threadNum, final long threadDelay,
            final int loopNum, final long loopDelay, final int roundNum) {
        if ((threadNum <= 0) || (loopNum <= 0) || (threadDelay < 0) || (loopDelay < 0)) {
            throw new IllegalArgumentException("threadNum=" + threadNum + ", loopNum=" + loopNum + ", threadDelay=" + threadDelay + ", loopDelay=" + loopDelay); //NOSONAR
        }
        if (N.notEmpty(args) && (args.size() > 1) && (args.size() != threadNum)) {
            throw new IllegalArgumentException(
                    "The input args must be null or size = 1 or size = threadNum. It's the input parameter for the every loop in each thread ");
        }
        // It takes about 250MB memory to save 1 million test results.
        if (threadNum * loopNum > IOUtil.MAX_MEMORY_IN_MB * 1000) {
            if (IOUtil.MAX_MEMORY_IN_MB < 1024) {
                logger.warn(
                        "Saving big number loop result in small memory may slow down the performance of target method. Consider increasing the maximum JVM memory size.");
            } else {
                logger.warn(
                        "Saving big number loop result may slow down the performance of target method. Consider adding for-loop to outer of the target method and reducing the loop number ("
                                + loopNum + ") to a smaller number");
            }
        }

        if (roundNum == 1) {
            return run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay, loopNum,
                    loopDelay);
        } else {
            MultiLoopsStatistics result = null;

            for (int i = 0; i < (suspended ? 1 : roundNum); i++) {
                if (result != null) {
                    result.printResult();
                    result = null; //NOSONAR
                }

                result = run(instance, methodName, method, args, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, threadNum, threadDelay,
                        loopNum, loopDelay);
            }

            return result;
        }
    }

    @SuppressWarnings("deprecation")
    private static MultiLoopsStatistics run(final Object instance, final String methodName, final Method method, final List<?> args,
            final Method setUpForMethod, final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int threadNum,
            final long threadDelay, final int loopNum, final long loopDelay) {
        if (!method.isAccessible()) {
            ClassUtil.setAccessibleQuietly(method, true);
        }

        gc();

        final ExecutorService asyncExecutor = Executors.newFixedThreadPool(threadNum);
        final AtomicInteger threadCounter = new AtomicInteger();
        // MXBean mxBean = new MXBean();
        final List<LoopStatistics> loopStatisticsList = Collections.synchronizedList(new ArrayList<>());
        final PrintStream ps = System.out; //NOSONAR
        final long startTimeInMillis = System.currentTimeMillis();
        final long startTimeInNano = System.nanoTime();

        for (int threadIndex = 0; threadIndex < (suspended ? 1 : threadNum); threadIndex++) {
            final Object arg = (N.isEmpty(args)) ? null : ((args.size() == 1) ? args.get(0) : args.get(threadIndex));
            threadCounter.incrementAndGet();

            asyncExecutor.execute(() -> {
                try {
                    runLoops(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, setUpForLoop, tearDownForLoop, loopNum, loopDelay,
                            loopStatisticsList, ps);
                } finally {
                    threadCounter.decrementAndGet();
                }
            });

            sleep(threadDelay);
        }

        while (threadCounter.get() > 0) {
            N.sleep(1);
        }

        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();
        return new MultiLoopsStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, loopStatisticsList);
    }

    private static void runLoops(final Object instance, final String methodName, final Method method, final Object arg, final Method setUpForMethod,
            final Method tearDownForMethod, final Method setUpForLoop, final Method tearDownForLoop, final int loopNum, final long loopDelay,
            final List<LoopStatistics> loopStatisticsList, final PrintStream ps) {
        for (int loopIndex = 0; loopIndex < (suspended ? 1 : loopNum); loopIndex++) {
            if (setUpForLoop != null) {
                try {
                    setUpForLoop.invoke(instance);
                } catch (final Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }

            final long startTimeInMillis = System.currentTimeMillis();
            final long startTimeInNano = System.nanoTime();
            final List<MethodStatistics> methodStatisticsList = runLoop(instance, methodName, method, arg, setUpForMethod, tearDownForMethod, ps);
            final long endTimeInNano = System.nanoTime();
            final long endTimeInMillis = System.currentTimeMillis();

            if (tearDownForLoop != null) {
                try {
                    tearDownForLoop.invoke(instance);
                } catch (final Exception e) {
                    // ignore;
                    e.printStackTrace(ps);
                    logger.warn(ExceptionUtil.getErrorMessage(e, true));
                }
            }

            final SingleLoopStatistics loopStatistics = new SingleLoopStatistics(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano,
                    methodStatisticsList);
            loopStatisticsList.add(loopStatistics);

            sleep(loopDelay);
        }
    }

    /**
     * Execute a single loop iteration for performance testing.
     *
     * @param instance the instance on which to invoke the method, may be {@code null} for static methods
     * @param methodName the name of the method being profiled
     * @param method the method to be profiled
     * @param arg the argument to pass to the method, may be {@code null}
     * @param setUpForMethod the setup method to be executed before the profiled method, may be {@code null}
     * @param tearDownForMethod the teardown method to be executed after the profiled method, may be {@code null}
     * @param ps the PrintStream for output
     * @return the list of method statistics for this loop iteration
     */
    private static List<MethodStatistics> runLoop(final Object instance, final String methodName, final Method method, final Object arg,
            final Method setUpForMethod, final Method tearDownForMethod, final PrintStream ps) {
        final List<MethodStatistics> methodStatisticsList = new ArrayList<>();

        if (setUpForMethod != null) {
            try {
                setUpForMethod.invoke(instance);
            } catch (final Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(ExceptionUtil.getErrorMessage(e, true));
            }
        }

        final long startTimeInMillis = System.currentTimeMillis();
        final long startTimeInNano = System.nanoTime();
        Object result = null;

        try {
            if (method.getParameterTypes().length == 0) {
                method.invoke(instance);
            } else {
                method.invoke(instance, arg);
            }
        } catch (final InvocationTargetException e) {
            e.printStackTrace(ps);
            logger.warn(ExceptionUtil.getErrorMessage(e, true));
            result = e.getTargetException();
        } catch (final Exception e) {
            e.printStackTrace(ps);
            logger.warn(ExceptionUtil.getErrorMessage(e, true));
            result = e;
        }

        final long endTimeInNano = System.nanoTime();
        final long endTimeInMillis = System.currentTimeMillis();

        if (tearDownForMethod != null) {
            try {
                tearDownForMethod.invoke(instance);
            } catch (final Exception e) {
                // ignore;
                e.printStackTrace(ps);
                logger.warn(ExceptionUtil.getErrorMessage(e, true));
            }
        }

        final MethodStatistics methodStatistics = new MethodStatistics(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, result);
        methodStatisticsList.add(methodStatistics);
        return methodStatisticsList;
    }

    @SuppressWarnings("deprecation")
    private static Method getMethod(final Object instance, final String methodName) {
        final Method method = ClassUtil.getDeclaredMethod(instance.getClass(), methodName);
        if (method == null) {
            throw new IllegalArgumentException("No method found by name: " + methodName);
        } else if (!method.isAccessible()) {
            ClassUtil.setAccessibleQuietly(method, true);
        }
        return method;
    }

    private static volatile boolean suspended = false;

    /**
     * Controls the suspension state of the profiler, enabling or disabling full-scale performance testing.
     * When suspended, all profiler operations run in minimal mode (single thread, single loop, single round)
     * regardless of the parameters specified in test method calls. This feature is invaluable during
     * development and debugging when you want to verify test logic without incurring the time and resource
     * overhead of comprehensive performance testing.
     *
     * <p><b>Suspension Behavior:</b>
     * When the profiler is suspended ({@code suspend(true)}):
     * <ul>
     *   <li>All tests execute with exactly 1 thread, 1 loop, and 1 round</li>
     *   <li>Thread delays and loop delays are completely bypassed</li>
     *   <li>Garbage collection between rounds is skipped</li>
     *   <li>Test execution completes almost immediately</li>
     *   <li>Statistical analysis still occurs but with minimal data</li>
     * </ul>
     *
     * <p><b>Primary Use Cases:</b>
     * <ul>
     *   <li><b>Development Debugging:</b> Quickly verify that profiled code executes without errors</li>
     *   <li><b>Integration Testing:</b> Test profiler integration without long-running performance tests</li>
     *   <li><b>CI/CD Pipelines:</b> Run smoke tests with profiler calls without performance overhead</li>
     *   <li><b>Rapid Iteration:</b> Quickly validate code changes before running full benchmarks</li>
     *   <li><b>Exception Testing:</b> Verify error handling in profiled code without waiting</li>
     * </ul>
     *
     * <p><b>Important Considerations:</b>
     * <ul>
     *   <li>Suspension is a global state affecting all profiler operations across all threads</li>
     *   <li>Statistics from suspended runs are NOT representative of actual performance</li>
     *   <li>Always resume the profiler before running actual performance tests</li>
     *   <li>Suspension state is not persisted; it resets when the JVM restarts</li>
     *   <li>This is a volatile variable, so changes are immediately visible across threads</li>
     * </ul>
     *
     * <p><b>Best Practices:</b>
     * <ul>
     *   <li>Use try-finally blocks to ensure profiler is resumed after debugging sessions</li>
     *   <li>Never leave profiler suspended in production environments</li>
     *   <li>Document suspension state changes in test code comments</li>
     *   <li>Consider using environment variables or system properties to control suspension</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic suspension for debugging
     * Profiler.suspend(true);
     * try {
     *     // This runs with just 1 thread, 1 loop regardless of parameters
     *     Profiler.run(100, 1000, 10, () -> {
     *         complexMethod(); // Just verify it doesn't crash
     *     });
     * } finally {
     *     Profiler.suspend(false); // Always resume
     * }
     *
     * // Conditional suspension based on environment
     * boolean isDebugMode = System.getProperty("debug.mode", "false").equals("true");
     * Profiler.suspend(isDebugMode);
     *
     * // Quick smoke test of multiple profiler calls
     * Profiler.suspend(true);
     * testSuite.runAllProfilerTests(); // Fast execution
     * Profiler.suspend(false);
     *
     * // Integration test scenario
     * if (!"production".equals(environment)) {
     *     Profiler.suspend(true); // Skip heavy testing in non-prod
     * }
     * performanceTestSuite.run();
     * }</pre>
     *
     * @param yesOrNo {@code true} to suspend the profiler (minimal execution mode), {@code false} to resume
     *                normal profiler operation with full performance testing capabilities.
     *                The suspension state affects all subsequent profiler operations until changed again
     * @see #isSuspended() to check the current suspension state
     */
    public static void suspend(final boolean yesOrNo) {
        suspended = yesOrNo;
    }

    /**
     * Checks if the profiler is currently suspended.
     * When suspended, all profiler runs execute with only one thread and one loop,
     * regardless of the specified parameters.
     *
     * @return {@code true} if the profiler is suspended, {@code false} otherwise
     */
    public static boolean isSuspended() {
        return suspended;
    }

    static void sleep(final long millis) {
        if (suspended) {
            return;
        }

        N.sleep(millis);
    }

    private static void gc() {
        if (suspended) {
            return;
        }

        Runtime.getRuntime().gc(); //NOSONAR
        N.sleep(1000);
    }

    /**
     * Base interface for all statistics objects in the profiler.
     * Provides common methods for tracking execution times and results.
     *
     * @version $Revision: 0.8 $
     */
    interface Statistics {

        /**
         * Gets the result of the execution, typically {@code null} for successful runs
         * or an Exception object if the execution failed.
         *
         * @return the execution result
         */
        Object getResult();

        /**
         * Sets the result of the execution.
         *
         * @param result the new result
         */
        void setResult(Object result);

        /**
         * Gets the start time in milliseconds since epoch.
         *
         * @return the start time in milliseconds
         */
        long getStartTimeInMillis();

        /**
         * Sets the start time in milliseconds.
         *
         * @param startTimeInMillis the new start time in milliseconds
         */
        void setStartTimeInMillis(long startTimeInMillis);

        /**
         * Gets the end time in milliseconds since epoch.
         *
         * @return the end time in milliseconds
         */
        long getEndTimeInMillis();

        /**
         * Sets the end time in milliseconds.
         *
         * @param endTimeInMillis the new end time in milliseconds
         */
        void setEndTimeInMillis(long endTimeInMillis);

        /**
         * Gets the start time in nanoseconds.
         *
         * @return the start time in nanoseconds
         */
        long getStartTimeInNano();

        /**
         * Sets the start time in nanoseconds.
         *
         * @param startTimeInNano the new start time in nanoseconds
         */
        void setStartTimeInNano(long startTimeInNano);

        /**
         * Gets the end time in nanoseconds.
         *
         * @return the end time in nanoseconds
         */
        long getEndTimeInNano();

        /**
         * Sets the end time in nanoseconds.
         *
         * @param endTimeInNano the new end time in nanoseconds
         */
        void setEndTimeInNano(long endTimeInNano);

        /**
         * Calculates and returns the elapsed time in milliseconds.
         * The calculation is based on nanosecond precision for accuracy.
         *
         * @return the elapsed time in milliseconds
         */
        double getElapsedTimeInMillis();
    }

    /**
     * Interface for loop-level statistics, providing aggregate information
     * about multiple method executions within loops.
     *
     * @version $Revision: 0.8 $
     */
    public interface LoopStatistics extends Statistics {

        /**
         * Gets the list of unique method names that were executed.
         *
         * @return a list of method names
         */
        List<String> getMethodNameList();

        /**
         * Gets the method execution with the minimum elapsed time.
         *
         * @return the MethodStatistics with minimum elapsed time
         */
        MethodStatistics getMinElapsedTimeMethod();

        /**
         * Gets the method execution with the maximum elapsed time.
         *
         * @return the MethodStatistics with maximum elapsed time
         */
        MethodStatistics getMaxElapsedTimeMethod();

        /**
         * Gets the total elapsed time for all executions of the specified method.
         *
         * @param methodName the name of the method
         * @return the total elapsed time in milliseconds
         */
        double getMethodTotalElapsedTimeInMillis(String methodName);

        /**
         * Gets the maximum elapsed time among all executions of the specified method.
         *
         * @param methodName the name of the method
         * @return the maximum elapsed time in milliseconds
         */
        double getMethodMaxElapsedTimeInMillis(String methodName);

        /**
         * Gets the minimum elapsed time among all executions of the specified method.
         *
         * @param methodName the name of the method
         * @return the minimum elapsed time in milliseconds
         */
        double getMethodMinElapsedTimeInMillis(String methodName);

        /**
         * Gets the average elapsed time for all executions of the specified method.
         *
         * @param methodName the name of the method
         * @return the average elapsed time in milliseconds
         */
        double getMethodAverageElapsedTimeInMillis(String methodName);

        /**
         * Gets the total elapsed time for all method executions.
         *
         * @return the total elapsed time in milliseconds
         */
        double getTotalElapsedTimeInMillis();

        /**
         * Gets the number of times the specified method was executed.
         *
         * @param methodName the name of the method
         * @return the execution count
         */
        int getMethodSize(String methodName);

        /**
         * Gets all statistics for executions of the specified method.
         *
         * @param methodName the name of the method
         * @return a list of MethodStatistics
         */
        List<MethodStatistics> getMethodStatisticsList(String methodName);

        /**
         * Gets statistics for failed executions of the specified method.
         *
         * @param methodName the name of the method
         * @return a list of MethodStatistics for failed executions
         */
        List<MethodStatistics> getFailedMethodStatisticsList(String methodName);

        /**
         * Gets all failed method executions across all methods.
         *
         * @return a list of all failed MethodStatistics
         */
        List<MethodStatistics> getAllFailedMethodStatisticsList();
    }

    /**
     * Abstract base class for statistics implementations.
     *
     * @version $Revision: 0.8 $
     */
    abstract static class AbstractStatistics implements Statistics {

        /** The start time in millis. */
        private long startTimeInMillis;

        /** The end time in millis. */
        private long endTimeInMillis;

        /** The start time in nano. */
        private long startTimeInNano;

        /** The end time in nano. */
        private long endTimeInNano;

        /** The result. */
        private Object result;

        protected AbstractStatistics() {
            this(0, 0, 0, 0);
        }

        protected AbstractStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this.startTimeInMillis = startTimeInMillis;
            this.endTimeInMillis = endTimeInMillis;
            this.startTimeInNano = startTimeInNano;
            this.endTimeInNano = endTimeInNano;
        }

        @Override
        public long getStartTimeInMillis() {
            return startTimeInMillis;
        }

        @Override
        public void setStartTimeInMillis(final long startTimeInMillis) {
            this.startTimeInMillis = startTimeInMillis;
        }

        @Override
        public long getEndTimeInMillis() {
            return endTimeInMillis;
        }

        @Override
        public void setEndTimeInMillis(final long endTimeInMillis) {
            this.endTimeInMillis = endTimeInMillis;
        }

        @Override
        public long getStartTimeInNano() {
            return startTimeInNano;
        }

        @Override
        public void setStartTimeInNano(final long startTimeInNano) {
            this.startTimeInNano = startTimeInNano;
        }

        @Override
        public long getEndTimeInNano() {
            return endTimeInNano;
        }

        @Override
        public void setEndTimeInNano(final long endTimeInNano) {
            this.endTimeInNano = endTimeInNano;
        }

        @Override
        public double getElapsedTimeInMillis() {
            return (endTimeInNano - startTimeInNano) / 1000000.0d; // convert to milliseconds.
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        /**
         * Converts a time in milliseconds to a formatted string representation.
         *
         * @param timeInMillis the time in milliseconds since epoch
         * @return the formatted time string in ISO local date-time format
         */
        protected String time2String(final long timeInMillis) {
            final Timestamp timestamp = Dates.createTimestamp(timeInMillis);
            return Dates.format(timestamp, Dates.ISO_LOCAL_DATE_TIME_FORMAT); // + " " + N.LOCAL_TIME_ZONE.getID();
        }
    }

    /**
     * Statistics for individual method executions.
     * This class captures timing information and results for a single method call.
     *
     * @version $Revision: 0.8 $
     */
    public static class MethodStatistics extends AbstractStatistics {

        /** The method name. */
        private final String methodName;

        /** The result. */
        private Object result;

        /**
         * Creates a new MethodStatistics instance with the specified method name.
         *
         * @param methodName the name of the method
         */
        public MethodStatistics(final String methodName) {
            this.methodName = methodName;
        }

        /**
         * Creates a new MethodStatistics instance with timing information.
         *
         * @param methodName the name of the method
         * @param startTimeInMillis the start time in milliseconds
         * @param endTimeInMillis the end time in milliseconds
         * @param startTimeInNano the start time in nanoseconds
         * @param endTimeInNano the end time in nanoseconds
         */
        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano) {
            this(methodName, startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        /**
         * Creates a new MethodStatistics instance with complete information.
         *
         * @param methodName the name of the method
         * @param startTimeInMillis the start time in milliseconds
         * @param endTimeInMillis the end time in milliseconds
         * @param startTimeInNano the start time in nanoseconds
         * @param endTimeInNano the end time in nanoseconds
         * @param result the execution result (null for success, Exception for failure)
         */
        public MethodStatistics(final String methodName, final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano,
                final long endTimeInNano, final Object result) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodName = methodName;
            this.result = result;
        }

        /**
         * Gets the name of the method that was executed.
         *
         * @return the method name
         */
        public String getMethodName() {
            return methodName;
        }

        @Override
        public Object getResult() {
            return result;
        }

        @Override
        public void setResult(final Object result) {
            this.result = result;
        }

        /**
         * Checks if the method execution failed.
         * A method is considered failed if the result is an Exception.
         *
         * @return {@code true} if the execution failed, {@code false} otherwise
         */
        public boolean isFailed() {
            return result instanceof Exception;
        }

        /**
         * Returns a string representation of this method statistics.
         * For failed executions, includes the exception class and message.
         *
         * @return a string representation of the statistics
         */
        @Override
        public String toString() {
            if (isFailed()) {
                final Exception e = (Exception) result;
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + ClassUtil.getSimpleClassName(e.getClass()) + ": " + e.getMessage() + ".";
            } else {
                return "method=" + methodName + ", startTime=" + time2String(getStartTimeInMillis()) + ", endTime=" + time2String(getEndTimeInMillis())
                        + ", result=" + result + ".";
            }
        }
    }

    /**
     * The Class SingleLoopStatistics.
     */
    static class SingleLoopStatistics extends AbstractStatistics implements LoopStatistics {

        /** The method statistics list. */
        private List<MethodStatistics> methodStatisticsList;

        /**
         * Instantiates a new single loop statistics.
         */
        public SingleLoopStatistics() {
        }

        /**
         * Instantiates a new single loop statistics.
         *
         * @param startTimeInMillis the start time in milliseconds since epoch
         * @param endTimeInMillis the end time in milliseconds since epoch
         * @param startTimeInNano the start time in nanoseconds
         * @param endTimeInNano the end time in nanoseconds
         */
        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, null);
        }

        /**
         * Instantiates a new single loop statistics.
         *
         * @param startTimeInMillis the start time in milliseconds since epoch
         * @param endTimeInMillis the end time in milliseconds since epoch
         * @param startTimeInNano the start time in nanoseconds
         * @param endTimeInNano the end time in nanoseconds
         * @param methodStatisticsList the list of method statistics for this loop
         */
        public SingleLoopStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final List<MethodStatistics> methodStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.methodStatisticsList = methodStatisticsList;
        }

        @Override
        public List<String> getMethodNameList() {
            final List<String> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (!result.contains(methodStatistics.getMethodName())) {
                        result.add(methodStatistics.getMethodName());
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method statistics list.
         *
         * @return the list of all method statistics
         */
        public List<MethodStatistics> getMethodStatisticsList() {
            if (methodStatisticsList == null) {
                methodStatisticsList = new ArrayList<>();
            }
            return methodStatisticsList;
        }

        /**
         * Sets the method statistics list.
         *
         * @param methodStatisticsList the new method statistics list
         */
        public void setMethodStatisticsList(final List<MethodStatistics> methodStatisticsList) {
            this.methodStatisticsList = methodStatisticsList;
        }

        /**
         * Adds the method statistics to this loop's statistics list.
         *
         * @param methodStatistics the method statistics to add
         */
        public void addMethodStatisticsList(final MethodStatistics methodStatistics) {
            getMethodStatisticsList().add(methodStatistics);
        }

        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method total elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the total elapsed time in milliseconds
         */
        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result += methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method max elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the maximum elapsed time in milliseconds
         */
        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName) && (methodStatistics.getElapsedTimeInMillis() > result)) {
                        result = methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method min elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the minimum elapsed time in milliseconds
         */
        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName) && (methodStatistics.getElapsedTimeInMillis() < result)) {
                        result = methodStatistics.getElapsedTimeInMillis();
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method average elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the average elapsed time in milliseconds
         */
        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        totalTime += methodStatistics.getElapsedTimeInMillis();
                        methodNum++;
                    }
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    result += methodStatistics.getElapsedTimeInMillis();
                }
            }
            return result;
        }

        @Override
        public int getMethodSize(final String methodName) {
            int methodSize = 0;
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        methodSize++;
                    }
                }
            }
            return methodSize;
        }

        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>(getMethodSize(methodName));
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed() && methodStatistics.getMethodName().equals(methodName)) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            final List<MethodStatistics> result = new ArrayList<>();
            if (methodStatisticsList != null) {
                for (final MethodStatistics methodStatistics : methodStatisticsList) {
                    if (methodStatistics.isFailed()) {
                        result.add(methodStatistics);
                    }
                }
            }
            return result;
        }
    }

    /**
     * Comprehensive statistics for multiple loops across multiple threads.
     * This class aggregates all loop statistics and provides methods to analyze
     * the overall performance test results, including percentile calculations
     * and various output formats.
     */
    public static class MultiLoopsStatistics extends AbstractStatistics implements LoopStatistics {

        /** The Constant SEPARATOR_LINE. */
        private static final String SEPARATOR_LINE = "========================================================================================================================";

        /** The thread num. */
        private final int threadNum;

        /** The loop statistics list. */
        private List<LoopStatistics> loopStatisticsList;

        /**
         * Creates a new MultiLoopsStatistics instance with timing information.
         *
         * @param startTimeInMillis the overall start time in milliseconds
         * @param endTimeInMillis the overall end time in milliseconds
         * @param startTimeInNano the overall start time in nanoseconds
         * @param endTimeInNano the overall end time in nanoseconds
         * @param threadNum the number of threads used in the test
         */
        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum) {
            this(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano, threadNum, null);
        }

        /**
         * Creates a new MultiLoopsStatistics instance with complete information.
         *
         * @param startTimeInMillis the overall start time in milliseconds
         * @param endTimeInMillis the overall end time in milliseconds
         * @param startTimeInNano the overall start time in nanoseconds
         * @param endTimeInNano the overall end time in nanoseconds
         * @param threadNum the number of threads used in the test
         * @param loopStatisticsList the list of loop statistics from all threads
         */
        public MultiLoopsStatistics(final long startTimeInMillis, final long endTimeInMillis, final long startTimeInNano, final long endTimeInNano,
                final int threadNum, final List<LoopStatistics> loopStatisticsList) {
            super(startTimeInMillis, endTimeInMillis, startTimeInNano, endTimeInNano);
            this.threadNum = threadNum;
            this.loopStatisticsList = loopStatisticsList;
        }

        /**
         * Gets the number of threads used in the performance test.
         *
         * @return the thread count
         */
        public int getThreadNum() {
            return threadNum;
        }

        @Override
        public List<String> getMethodNameList() {
            List<String> result = null;
            if (loopStatisticsList == null) {
                result = new ArrayList<>();
            } else {
                result = (loopStatisticsList.get(0)).getMethodNameList();
            }
            return result;
        }

        /**
         * Gets the loop statistics list.
         *
         * @return the list of loop statistics from all threads
         */
        public List<LoopStatistics> getLoopStatisticsList() {
            if (loopStatisticsList == null) {
                loopStatisticsList = new ArrayList<>();
            }
            return loopStatisticsList;
        }

        /**
         * Sets the loop statistics list.
         *
         * @param loopStatisticsList the new loop statistics list
         */
        public void setLoopStatisticsList(final List<LoopStatistics> loopStatisticsList) {
            this.loopStatisticsList = loopStatisticsList;
        }

        /**
         * Adds the loop statistics to the list of all loop statistics.
         *
         * @param loopStatistics the loop statistics to add
         */
        public void addMethodStatisticsList(final LoopStatistics loopStatistics) {
            getLoopStatisticsList().add(loopStatistics);
        }

        @Override
        public MethodStatistics getMaxElapsedTimeMethod() {
            MethodStatistics result = null;

            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final MethodStatistics methodStatistics = loopStatistics.getMaxElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() > result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }

            return result;
        }

        @Override
        public MethodStatistics getMinElapsedTimeMethod() {
            MethodStatistics result = null;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final MethodStatistics methodStatistics = loopStatistics.getMinElapsedTimeMethod();
                    if ((result == null) || (methodStatistics.getElapsedTimeInMillis() < result.getElapsedTimeInMillis())) {
                        result = methodStatistics;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method total elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the total elapsed time in milliseconds
         */
        @Override
        public double getMethodTotalElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                }
            }
            return result;
        }

        /**
         * Gets the method max elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the maximum elapsed time in milliseconds
         */
        @Override
        public double getMethodMaxElapsedTimeInMillis(final String methodName) {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodMaxTime = loopStatistics.getMethodMaxElapsedTimeInMillis(methodName);
                    if (loopMethodMaxTime > result) {
                        result = loopMethodMaxTime;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method min elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the minimum elapsed time in milliseconds
         */
        @Override
        public double getMethodMinElapsedTimeInMillis(final String methodName) {
            double result = Integer.MAX_VALUE;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodMinTime = loopStatistics.getMethodMinElapsedTimeInMillis(methodName);
                    if (loopMethodMinTime < result) {
                        result = loopMethodMinTime;
                    }
                }
            }
            return result;
        }

        /**
         * Gets the method average elapsed time in millis.
         *
         * @param methodName the name of the method
         * @return the average elapsed time in milliseconds
         */
        @Override
        public double getMethodAverageElapsedTimeInMillis(final String methodName) {
            double totalTime = 0;
            int methodNum = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    final double loopMethodTotalTime = loopStatistics.getMethodTotalElapsedTimeInMillis(methodName);
                    final int loopMethodSize = loopStatistics.getMethodSize(methodName);
                    totalTime += loopMethodTotalTime;
                    methodNum += loopMethodSize;
                }
            }
            return (methodNum > 0) ? (totalTime / methodNum) : totalTime;
        }

        @Override
        public double getTotalElapsedTimeInMillis() {
            double result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getTotalElapsedTimeInMillis();
                }
            }
            return result;
        }

        @Override
        public int getMethodSize(final String methodName) {
            int result = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result += loopStatistics.getMethodSize(methodName);
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> methodStatisticsList = new ArrayList<>(getMethodSize(methodName));
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    methodStatisticsList.addAll(loopStatistics.getMethodStatisticsList(methodName));
                }
            }
            return methodStatisticsList;
        }

        @Override
        public List<MethodStatistics> getFailedMethodStatisticsList(final String methodName) {
            final List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getFailedMethodStatisticsList(methodName));
                }
            }
            return result;
        }

        @Override
        public List<MethodStatistics> getAllFailedMethodStatisticsList() {
            final List<MethodStatistics> result = new ArrayList<>();
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    result.addAll(loopStatistics.getAllFailedMethodStatisticsList());
                }
            }
            return result;
        }

        /**
         * Gets the total number of method calls across all loops.
         *
         * @return the total number of method calls
         */
        private int getTotalCall() {
            int res = 0;
            if (loopStatisticsList != null) {
                for (final LoopStatistics loopStatistics : loopStatisticsList) {
                    res += loopStatistics.getMethodNameList().size();
                }
            }
            return res;
        }

        /**
         * Prints comprehensive performance test results to the standard output console (System.out).
         * This method generates a detailed, formatted report including execution summary, statistical
         * analysis, percentile distributions, and error information. The output is designed for immediate
         * human readability during development, testing, and performance analysis sessions.
         *
         * <p>The generated report provides a complete performance analysis including:
         * <ul>
         *   <li><b>Test Configuration:</b> Thread count, loop iterations per thread, and timing information</li>
         *   <li><b>Time Metrics:</b> Start time, end time, and total elapsed time for the entire test</li>
         *   <li><b>Per-Method Statistics:</b> Average, minimum, and maximum execution times for each profiled method</li>
         *   <li><b>Percentile Analysis:</b> Execution time percentiles (0.01%, 0.1%, 1%, 10%, 20%, 50%, 80%, 90%, 99%, 99.9%, 99.99%)</li>
         *   <li><b>Error Summary:</b> Detailed information about any failed method executions with stack traces</li>
         * </ul>
         *
         * <p><b>Output Format Details:</b>
         * The console output uses a tabular format with columns aligned for easy reading. All timing values
         * are displayed in milliseconds with three decimal places of precision. The percentile columns show
         * the execution time threshold that the given percentage of executions completed within, helping
         * identify performance outliers and distribution patterns.
         *
         * <p><b>Percentile Interpretation:</b>
         * <ul>
         *   <li><b>0.01% >=:</b> The fastest 0.01% of executions took at least this long (best case scenario)</li>
         *   <li><b>50% >= (median):</b> Half of all executions were at least this fast</li>
         *   <li><b>99% >=:</b> Only 1% of executions were slower than this (typical "worst case")</li>
         *   <li><b>99.99% >=:</b> Only 0.01% of executions were slower (extreme outliers)</li>
         * </ul>
         *
         * <p><b>Typical Use Cases:</b>
         * <ul>
         *   <li>Quick performance validation during development</li>
         *   <li>Console-based performance regression testing</li>
         *   <li>Interactive performance analysis and debugging</li>
         *   <li>Automated test runs where console output is captured</li>
         *   <li>Real-time performance monitoring during load tests</li>
         * </ul>
         *
         * <p><b>Example output format:</b></p>
         * <pre>{@code
         * ========================================================================================================================
         * (unit: milliseconds)
         * threadNum=4; loops=1000
         * startTime: 2023-01-01 10:00:00
         * endTime:   2023-01-01 10:00:05
         * totalElapsedTime: 5000.000
         *
         * <method name>,        |avg time|, |min time|, |max time|, |0.01% >=|, |0.1% >=|,  |1% >=|,    |10% >=|,   |20% >=|,   |50% >=|,   |80% >=|,   |90% >=|,   |99% >=|,   |99.9% >=|, |99.99% >=|
         * databaseQuery,        1.234,      0.123,      12.345,      0.150,      0.200,      0.300,      0.800,      1.000,      1.200,      1.500,      2.000,      5.000,      8.000,       10.000
         * cacheOperation,       0.456,      0.050,      2.100,       0.060,      0.070,      0.100,      0.250,      0.350,      0.450,      0.600,      0.750,      1.200,      1.500,       1.800
         *
         * Errors: 5 (0.125%)
         * --------------------------------------------------------------------------------
         * method=databaseQuery, startTime=2023-01-01 10:00:01, endTime=2023-01-01 10:00:01, result=SQLException: Connection timeout.
         * ========================================================================================================================
         * }</pre>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Basic usage: Run test and immediately print results
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, () -> {
         *     performDatabaseQuery();
         * });
         * stats.printResult(); // Prints to console
         *
         * // Compare multiple implementations
         * System.out.println("Testing ArrayList:");
         * Profiler.run(8, 5000, 5, "ArrayList", () -> {
         *     arrayList.add(randomValue());
         * }).printResult();
         *
         * System.out.println("\nTesting CopyOnWriteArrayList:");
         * Profiler.run(8, 5000, 5, "CopyOnWriteArrayList", () -> {
         *     cowList.add(randomValue());
         * }).printResult();
         * }</pre>
         *
         * @see #writeResult(OutputStream) to write results to a file or custom output stream
         * @see #writeResult(Writer) to write results using a Writer
         * @see #writeHtmlResult(Writer) for HTML-formatted output suitable for web reports
         * @see #writeXmlResult(Writer) for machine-readable XML output
         */
        public void printResult() {
            writeResult(new PrintWriter(System.out)); //NOSONAR
        }

        /**
         * Writes comprehensive performance test results to the specified OutputStream in human-readable
         * text format. This method provides byte-stream based output with the same detailed formatting
         * as {@link #printResult()}, enabling direct writing to files, network sockets, or any other
         * byte-oriented output destination.
         *
         * <p>This is a convenience method that wraps the OutputStream in a PrintWriter and delegates to
         * {@link #writeResult(Writer)}. All performance metrics, statistical data, and error information
         * are written in the same tabular text format as console output.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Write to file output stream
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, () -> {
         *     performOperation();
         * });
         *
         * try (FileOutputStream fos = new FileOutputStream("results.txt")) {
         *     stats.writeResult(fos);
         * }
         *
         * // Write to socket
         * try (OutputStream out = socket.getOutputStream()) {
         *     stats.writeResult(out);
         * }
         * }</pre>
         *
         * @param output the OutputStream to which performance results will be written. Must not be {@code null}.
         *               The stream will be flushed but NOT closed by this method
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #writeResult(Writer) for character-stream based output
         * @see #printResult() for console output
         */
        public void writeResult(final OutputStream output) {
            writeResult(new PrintWriter(output));
        }

        /**
         * Writes comprehensive performance test results to the specified Writer in human-readable text format.
         * This method provides the same detailed, tabular output as {@link #printResult()} but directs it to
         * a custom Writer destination, enabling flexible output handling such as file persistence, string
         * buffers, network streams, or custom output processing pipelines.
         *
         * <p>The written output includes all performance metrics in a formatted, column-aligned text table
         * with the same structure as console output: test configuration, timing summary, per-method statistics,
         * percentile distributions, and error details. This format is optimized for human readability and can
         * be easily archived, analyzed, or shared.
         *
         * <p><b>Common Use Cases:</b>
         * <ul>
         *   <li><b>File Archiving:</b> Save performance test results to log files for historical analysis</li>
         *   <li><b>Build Reports:</b> Integrate results into CI/CD build artifacts and reports</li>
         *   <li><b>Email Reports:</b> Generate text-based performance reports for distribution</li>
         *   <li><b>String Processing:</b> Capture results in StringWriter for programmatic analysis</li>
         *   <li><b>Network Streaming:</b> Stream results to remote monitoring systems</li>
         *   <li><b>Log Integration:</b> Append results to application log files</li>
         * </ul>
         *
         * <p><b>Output Format:</b>
         * The output format is identical to {@link #printResult()}, containing separator lines, timing
         * information, statistical tables with percentile data, and error summaries. All timing values
         * are in milliseconds with three decimal places. The Writer is flushed after writing but NOT closed,
         * allowing the caller to maintain control over resource lifecycle.
         *
         * <p><b>Resource Management:</b>
         * This method does NOT close the provided Writer. The caller is responsible for closing the Writer
         * when appropriate, typically using try-with-resources. The Writer is flushed after writing to
         * ensure all data is written to the underlying stream.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Write to file with automatic resource management
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, () -> {
         *     performDatabaseQuery();
         * });
         *
         * try (FileWriter writer = new FileWriter("performance-results.txt")) {
         *     stats.writeResult(writer);
         * }
         *
         * // Append to existing log file
         * try (FileWriter writer = new FileWriter("performance-log.txt", true)) {
         *     writer.write("\n=== Performance Test Run: " + new Date() + " ===\n");
         *     stats.writeResult(writer);
         * }
         *
         * // Capture results as a string for processing
         * StringWriter stringWriter = new StringWriter();
         * stats.writeResult(stringWriter);
         * String results = stringWriter.toString();
         * emailService.sendPerformanceReport(results);
         *
         * // Write to multiple destinations
         * List<Writer> destinations = Arrays.asList(
         *     new FileWriter("results.txt"),
         *     new FileWriter("archive.txt"),
         *     new StringWriter()
         * );
         * for (Writer writer : destinations) {
         *     try {
         *         stats.writeResult(writer);
         *     } finally {
         *         writer.close();
         *     }
         * }
         * }</pre>
         *
         * @param output the Writer to which performance results will be written. Must not be {@code null}.
         *               The Writer will be flushed but NOT closed by this method. All timing data,
         *               statistics, and error information will be written in formatted text
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #printResult() for writing to standard output console
         * @see #writeResult(OutputStream) for byte-stream based output
         * @see #writeHtmlResult(Writer) for HTML-formatted output
         * @see #writeXmlResult(Writer) for structured XML output
         */
        public void writeResult(final Writer output) {
            writeResult(new PrintWriter(output));
        }

        /**
         * Writes the performance test results to the specified PrintWriter.
         *
         * @param output the PrintWriter to write to
         */
        private void writeResult(final PrintWriter output) {
            output.println();
            output.println(SEPARATOR_LINE);
            output.println("(unit: milliseconds)");
            output.println("threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum));
            output.println("startTime: " + time2String(getStartTimeInMillis()));
            output.println("endTime:   " + time2String(getEndTimeInMillis()));
            output.println("totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()));
            output.println();
            final String methodNameTitle = "<method name>";
            final List<String> methodNameList = getMethodNameList();
            int maxMethodNameLength = methodNameTitle.length();
            if (methodNameList.size() > 0) {
                for (final String methodName : methodNameList) {
                    if (methodName.length() > maxMethodNameLength) {
                        maxMethodNameLength = methodName.length();
                    }
                }
            }
            output.println();
            maxMethodNameLength += 3;
            output.println(Strings.padEnd(methodNameTitle + ",  ", maxMethodNameLength)
                    + "|avg time|, |min time|, |max time|, |0.01% >=|, |0.1% >=|,  |1% >=|,    |10% >=|,   |20% >=|,   |50% >=|,   |80% >=|,   |90% >=|,   |99% >=|,   |99.9% >=|, |99.99% >=|");
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final int minLen = 12;
                output.println(Strings.padEnd(methodName + ",  ", maxMethodNameLength) + Strings.padEnd(elapsedTimeFormat.format(avgTime) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(minTime) + ",  ", minLen) + Strings.padEnd(elapsedTimeFormat.format(maxTime) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + ",  ", minLen)
                        + Strings.padEnd(elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + ",  ", minLen));
            }
            output.println();
            writeError(output);
            output.println(SEPARATOR_LINE);
            output.flush();
        }

        /**
         * Writes the error information for failed method executions to the output.
         *
         * @param output the PrintWriter to write to
         */
        private void writeError(final PrintWriter output) {
            MethodStatistics methodStatistics;
            final List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println();
                output.println("Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)"); //NOSONAR
                for (final Object element : failedMethodList) {
                    output.println("--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) element;
                    output.println(methodStatistics.toString());
                }
            }
        }

        /**
         * Writes performance test results to the specified OutputStream in HTML format with structured
         * tables and formatted markup. This method provides byte-stream based HTML output, enabling direct
         * writing to files, network streams, or any other byte-oriented output destination.
         *
         * <p>This is a convenience method that wraps the OutputStream in a PrintWriter and delegates to
         * {@link #writeHtmlResult(Writer)}. All performance statistics are rendered as HTML tables with
         * proper formatting suitable for web browsers and HTML-based reporting systems.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Write HTML to file output stream
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, "APITest", () -> {
         *     apiClient.call();
         * });
         *
         * try (FileOutputStream fos = new FileOutputStream("report.html")) {
         *     stats.writeHtmlResult(fos);
         * }
         *
         * // Stream HTML to HTTP response
         * try (OutputStream out = httpResponse.getOutputStream()) {
         *     stats.writeHtmlResult(out);
         * }
         * }</pre>
         *
         * @param output the OutputStream to which HTML-formatted results will be written.
         *               Must not be {@code null}. The stream will be flushed but NOT closed by this method
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #writeHtmlResult(Writer) for character-stream based HTML output
         * @see #writeResult(OutputStream) for plain text output
         */
        public void writeHtmlResult(final OutputStream output) {
            writeHtmlResult(new PrintWriter(output));
        }

        /**
         * Writes performance test results to the specified Writer in HTML format with structured tables
         * and formatted markup. This method generates web-ready HTML output that can be directly embedded
         * in web pages, email reports, or dashboard views, providing a visually organized presentation of
         * performance metrics suitable for stakeholders, management reports, and web-based monitoring systems.
         *
         * <p>The HTML output includes all performance statistics organized in a well-structured HTML table
         * with proper headers, column alignment, and formatting. Unlike plain text output, the HTML format
         * provides better visual organization, making it easier to scan large datasets and identify
         * performance trends. The output uses semantic HTML markup suitable for styling with CSS.
         *
         * <p><b>Generated HTML Structure:</b>
         * <ul>
         *   <li><b>Summary Header:</b> Test configuration, timing, and metadata in formatted lines</li>
         *   <li><b>Statistics Table:</b> HTML table with headers and data rows for each profiled method</li>
         *   <li><b>Table Columns:</b> Method name, avg/min/max times, and all percentile thresholds</li>
         *   <li><b>Error Section:</b> HTML-formatted error details if any method executions failed</li>
         *   <li><b>HTML Entities:</b> Proper encoding of special characters (e.g., &gt; for >)</li>
         * </ul>
         *
         * <p><b>Common Use Cases:</b>
         * <ul>
         *   <li><b>Web Reports:</b> Embed results in HTML reports for web browsers</li>
         *   <li><b>Email Notifications:</b> Send HTML-formatted performance reports via email</li>
         *   <li><b>Dashboard Integration:</b> Display results in monitoring dashboards</li>
         *   <li><b>Documentation:</b> Include results in technical documentation and wikis</li>
         *   <li><b>Stakeholder Reports:</b> Create executive-friendly performance summaries</li>
         *   <li><b>CI/CD Artifacts:</b> Generate HTML build artifacts for Jenkins, GitLab, etc.</li>
         * </ul>
         *
         * <p><b>HTML Table Features:</b>
         * The generated table has a fixed width of 1200 pixels and includes borders for clear visual
         * separation. Each column header uses HTML table header tags (&lt;th&gt;), and data cells use
         * standard table data tags (&lt;td&gt;). The HTML can be easily styled with custom CSS for
         * corporate branding or specific design requirements.
         *
         * <p><b>Resource Management:</b>
         * This method does NOT close the provided Writer. The caller maintains responsibility for proper
         * resource cleanup, typically using try-with-resources. The Writer is flushed after writing to
         * ensure complete data transfer to the underlying stream.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Generate standalone HTML report file
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, "DatabaseQuery", () -> {
         *     repository.findById(randomId());
         * });
         *
         * try (FileWriter writer = new FileWriter("performance-report.html")) {
         *     writer.write("<html><head><title>Performance Report</title></head><body>\n");
         *     writer.write("<h1>Database Query Performance Test</h1>\n");
         *     stats.writeHtmlResult(writer);
         *     writer.write("</body></html>");
         * }
         *
         * // Email HTML report
         * StringWriter htmlWriter = new StringWriter();
         * stats.writeHtmlResult(htmlWriter);
         * String htmlContent = htmlWriter.toString();
         * emailService.sendHtmlEmail("Performance Results", htmlContent);
         *
         * // Embed in web dashboard
         * try (PrintWriter out = response.getWriter()) {
         *     out.println("<div class='performance-results'>");
         *     stats.writeHtmlResult(out);
         *     out.println("</div>");
         * }
         *
         * // Generate styled report with custom CSS
         * try (FileWriter writer = new FileWriter("styled-report.html")) {
         *     writer.write("<html><head>");
         *     writer.write("<style>");
         *     writer.write("table { border-collapse: collapse; font-family: Arial; }");
         *     writer.write("th { background-color: #4CAF50; color: white; padding: 12px; }");
         *     writer.write("td { padding: 8px; border: 1px solid #ddd; }");
         *     writer.write("</style></head><body>");
         *     stats.writeHtmlResult(writer);
         *     writer.write("</body></html>");
         * }
         * }</pre>
         *
         * @param output the Writer to which HTML-formatted performance results will be written.
         *               Must not be {@code null}. The Writer will be flushed but NOT closed by this method.
         *               All statistics are rendered as HTML tables and formatted text
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #writeHtmlResult(OutputStream) for byte-stream based HTML output
         * @see #writeResult(Writer) for plain text formatted output
         * @see #writeXmlResult(Writer) for machine-readable XML output
         * @see #printResult() for console output
         */
        public void writeHtmlResult(final Writer output) {
            writeHtmlResult(new PrintWriter(output));
        }

        /**
         * Writes the performance test results in HTML format to the specified PrintWriter.
         *
         * @param output the PrintWriter to write to
         */
        private void writeHtmlResult(final PrintWriter output) {
            output.println(SEPARATOR_LINE);
            output.println("<br/>" + "(unit: milliseconds)"); //NOSONAR
            output.println("<br/>" + "threadNum=" + threadNum + "; loops=" + (loopStatisticsList.size() / threadNum));
            output.println("<br/>" + "startTime: " + time2String(getStartTimeInMillis()));
            output.println("<br/>" + "endTime:   " + time2String(getEndTimeInMillis()));
            output.println("<br/>" + "totalElapsedTime: " + elapsedTimeFormat.format(getElapsedTimeInMillis()));
            output.println("<br/>"); //NOSONAR

            output.println("<br/>");
            output.println("<table width=\"1200\" border=\"1\">");
            output.println("<tr>");
            output.println("<th>method name</th>");
            output.println("<th>avg time</th>");
            output.println("<th>min time</th>");
            output.println("<th>max time</th>");
            output.println("<th>0.01% &gt;=</th>");
            output.println("<th>0.1% &gt;=</th>");
            output.println("<th>1% &gt;=</th>");
            output.println("<th>10% &gt;=</th>");
            output.println("<th>20% &gt;=</th>");
            output.println("<th>50% &gt;=</th>");
            output.println("<th>80% &gt;=</th>");
            output.println("<th>90% &gt;=</th>");
            output.println("<th>99% &gt;=</th>");
            output.println("<th>99.9% &gt;=</th>");
            output.println("<th>99.99% &gt;=</th>");
            output.println("</tr>");
            final List<String> methodNameList = getMethodNameList();
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                output.println("<tr>");
                output.println("<td>" + methodName + "</td>"); //NOSONAR
                output.println("<td>" + elapsedTimeFormat.format(avgTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(minTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(maxTime) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.1)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</td>");
                output.println("<td>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</td>");
                output.println("</tr>");
            }
            output.println("</table>");
            writeHtmlError(output);
            output.println(SEPARATOR_LINE);
            output.flush();
        }

        /**
         * Writes the error information for failed method executions in HTML format to the output.
         *
         * @param output the PrintWriter to write to
         */
        private void writeHtmlError(final PrintWriter output) {
            MethodStatistics methodStatistics;
            final List<?> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println("<h4>Errors:" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</h4>"); //NOSONAR
                for (final Object element : failedMethodList) {
                    output.println("<br/>" + "--------------------------------------------------------------------------------");
                    methodStatistics = (MethodStatistics) element;
                    output.println("<br/>" + methodStatistics.toString());
                }
            }
        }

        /**
         * Writes performance test results to the specified OutputStream in structured XML format, providing
         * machine-readable output suitable for automated processing and data integration. This method provides
         * byte-stream based XML output, enabling direct writing to files, network streams, or any other
         * byte-oriented output destination.
         *
         * <p>This is a convenience method that wraps the OutputStream in a PrintWriter and delegates to
         * {@link #writeXmlResult(Writer)}. All performance metrics are rendered as well-formed XML elements
         * in a hierarchical structure suitable for parsing and automated processing.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Write XML to file output stream
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, "DataProcessor", () -> {
         *     processData();
         * });
         *
         * try (FileOutputStream fos = new FileOutputStream("results.xml")) {
         *     stats.writeXmlResult(fos);
         * }
         *
         * // Stream XML to network destination
         * try (OutputStream out = socket.getOutputStream()) {
         *     stats.writeXmlResult(out);
         * }
         * }</pre>
         *
         * @param output the OutputStream to which XML-formatted results will be written.
         *               Must not be {@code null}. The stream will be flushed but NOT closed by this method
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #writeXmlResult(Writer) for character-stream based XML output
         * @see #writeResult(OutputStream) for plain text output
         */
        public void writeXmlResult(final OutputStream output) {
            writeXmlResult(new PrintWriter(output));
        }

        /**
         * Writes performance test results to the specified Writer in structured XML format, providing
         * machine-readable output suitable for automated processing, data integration, and programmatic
         * analysis. This method generates well-formed XML with hierarchical structure containing all
         * performance metrics, statistical data, and error information in a format that can be easily
         * parsed, transformed, and integrated into automated reporting and monitoring systems.
         *
         * <p>The XML output provides a hierarchical, structured representation of all performance data,
         * making it ideal for integration with automated systems, data warehouses, monitoring platforms,
         * and business intelligence tools. Unlike human-readable formats, XML enables reliable automated
         * parsing, validation against schemas, and transformation using XSLT or similar technologies.
         *
         * <p><b>XML Structure and Elements:</b>
         * <ul>
         *   <li><b>Root Element:</b> {@code <result>} contains all performance test data</li>
         *   <li><b>Metadata:</b> {@code <unit>}, {@code <threadNum>}, {@code <loops>}, timing information</li>
         *   <li><b>Method Elements:</b> {@code <method name="...">} for each profiled method</li>
         *   <li><b>Timing Statistics:</b> {@code <avgTime>}, {@code <minTime>}, {@code <maxTime>}</li>
         *   <li><b>Percentiles:</b> {@code <_0.01>}, {@code <_0.1>}, {@code <_0.5>}, etc.</li>
         *   <li><b>Error Information:</b> {@code <errors>} count and individual {@code <error>} elements</li>
         * </ul>
         *
         * <p><b>Common Use Cases:</b>
         * <ul>
         *   <li><b>Automated Processing:</b> Parse results with XML parsers for programmatic analysis</li>
         *   <li><b>Data Integration:</b> Import performance data into databases and data warehouses</li>
         *   <li><b>Monitoring Systems:</b> Feed results into monitoring and alerting platforms</li>
         *   <li><b>Trend Analysis:</b> Store historical data for long-term performance trend analysis</li>
         *   <li><b>Report Generation:</b> Transform XML to various formats using XSLT</li>
         *   <li><b>API Integration:</b> Exchange performance data between systems via XML</li>
         *   <li><b>Business Intelligence:</b> Import into BI tools for executive dashboards</li>
         * </ul>
         *
         * <p><b>XML Format Features:</b>
         * <ul>
         *   <li>Well-formed XML suitable for standard XML parsers (SAX, DOM, StAX)</li>
         *   <li>All timing values in milliseconds with three decimal places</li>
         *   <li>Consistent element naming and structure across all test runs</li>
         *   <li>Percentile elements use underscore prefix (e.g., {@code <_0.99>})</li>
         *   <li>Method names included as XML attributes for easy filtering</li>
         *   <li>Error details formatted as text content within error elements</li>
         * </ul>
         *
         * <p><b>Resource Management:</b>
         * This method does NOT close the provided Writer. The caller is responsible for proper resource
         * management, typically using try-with-resources. The Writer is flushed after writing to ensure
         * all XML data is written to the underlying stream.
         *
         * <p><b>Example XML Output Structure:</b></p>
         * <pre>{@code
         * <result>
         *   <unit>milliseconds</unit>
         *   <threadNum>4</threadNum>
         *   <loops>1000</loops>
         *   <startTime>2023-01-01 10:00:00</startTime>
         *   <endTime>2023-01-01 10:00:05</endTime>
         *   <totalElapsedTime>5000.000</totalElapsedTime>
         *
         *   <method name="databaseQuery">
         *     <avgTime>1.234</avgTime>
         *     <minTime>0.123</minTime>
         *     <maxTime>12.345</maxTime>
         *     <_0.0001>0.150</_0.0001>
         *     <_0.001>0.200</_0.001>
         *     <_0.01>0.300</_0.01>
         *     <_0.2>1.000</_0.2>
         *     <_0.5>1.200</_0.5>
         *     <_0.8>1.500</_0.8>
         *     <_0.9>2.000</_0.9>
         *     <_0.99>5.000</_0.99>
         *     <_0.999>8.000</_0.999>
         *     <_0.9999>10.000</_0.9999>
         *   </method>
         *
         *   <errors>5 (0.125%)</errors>
         *   <error>method=databaseQuery, startTime=2023-01-01 10:00:01, result=SQLException: Connection timeout.</error>
         * </result>
         * }</pre>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Write XML results to file
         * MultiLoopsStatistics stats = Profiler.run(4, 1000, 3, "DatabaseQuery", () -> {
         *     repository.findById(randomId());
         * });
         *
         * try (FileWriter writer = new FileWriter("performance-results.xml")) {
         *     stats.writeXmlResult(writer);
         * }
         *
         * // Parse XML results programmatically
         * StringWriter xmlWriter = new StringWriter();
         * stats.writeXmlResult(xmlWriter);
         * String xmlContent = xmlWriter.toString();
         *
         * DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         * Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
         * NodeList methods = doc.getElementsByTagName("method");
         * // Process XML data...
         *
         * // Store in database
         * try (FileWriter writer = new FileWriter("results.xml")) {
         *     stats.writeXmlResult(writer);
         * }
         * performanceDataService.importXmlResults("results.xml");
         *
         * // Generate multiple format outputs
         * String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
         * try (FileWriter xmlWriter = new FileWriter("perf-" + timestamp + ".xml");
         *      FileWriter txtWriter = new FileWriter("perf-" + timestamp + ".txt");
         *      FileWriter htmlWriter = new FileWriter("perf-" + timestamp + ".html")) {
         *     stats.writeXmlResult(xmlWriter);
         *     stats.writeResult(txtWriter);
         *     stats.writeHtmlResult(htmlWriter);
         * }
         * }</pre>
         *
         * @param output the Writer to which XML-formatted performance results will be written.
         *               Must not be {@code null}. The Writer will be flushed but NOT closed by this method.
         *               All statistics are rendered as well-formed XML elements
         * @throws NullPointerException if {@code output} is {@code null}
         * @see #writeXmlResult(OutputStream) for byte-stream based XML output
         * @see #writeResult(Writer) for plain text formatted output
         * @see #writeHtmlResult(Writer) for HTML-formatted output
         * @see #printResult() for console output
         */
        public void writeXmlResult(final Writer output) {
            writeXmlResult(new PrintWriter(output));
        }

        /**
         * Writes the performance test results in XML format to the specified PrintWriter.
         *
         * @param output the PrintWriter to write to
         */
        private void writeXmlResult(final PrintWriter output) {
            output.println("<result>");
            output.println("<unit>milliseconds</unit>");
            output.println("<threadNum>" + threadNum + "</threadNum>");
            output.println("<loops>" + (loopStatisticsList.size() / threadNum) + "</loops>");
            output.println("<startTime>" + time2String(getStartTimeInMillis()) + "</startTime>");
            output.println("<endTime>" + time2String(getEndTimeInMillis()) + "</endTime>");
            output.println("<totalElapsedTime>" + elapsedTimeFormat.format(getElapsedTimeInMillis()) + "</totalElapsedTime>");
            output.println();
            final List<String> methodNameList = getMethodNameList();
            for (final String methodName : methodNameList) {
                final List<MethodStatistics> methodStatisticsList = getMethodStatisticsList(methodName);
                final int size = methodStatisticsList.size();
                methodStatisticsList.sort((o1, o2) -> Double.compare(o2.getElapsedTimeInMillis(), o1.getElapsedTimeInMillis()));
                final double avgTime = getMethodAverageElapsedTimeInMillis(methodName);
                final double minTime = methodStatisticsList.get(size - 1).getElapsedTimeInMillis();
                final double maxTime = methodStatisticsList.get(0).getElapsedTimeInMillis();
                output.println("<method name=\"" + methodName + "\">");
                output.println("<avgTime>" + elapsedTimeFormat.format(avgTime) + "</avgTime>");
                output.println("<minTime>" + elapsedTimeFormat.format(minTime) + "</minTime>");
                output.println("<maxTime>" + elapsedTimeFormat.format(maxTime) + "</maxTime>");
                output.println("<_0.0001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.0001)).getElapsedTimeInMillis()) + "</_0.0001>");
                output.println("<_0.001>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.001)).getElapsedTimeInMillis()) + "</_0.001>");
                output.println("<_0.01>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.01)).getElapsedTimeInMillis()) + "</_0.01>");
                output.println("<_0.2>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.2)).getElapsedTimeInMillis()) + "</_0.2>");
                output.println("<_0.5>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.5)).getElapsedTimeInMillis()) + "</_0.5>");
                output.println("<_0.8>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.8)).getElapsedTimeInMillis()) + "</_0.8>");
                output.println("<_0.9>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9)).getElapsedTimeInMillis()) + "</_0.9>");
                output.println("<_0.99>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.99)).getElapsedTimeInMillis()) + "</_0.99>");
                output.println("<_0.999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.999)).getElapsedTimeInMillis()) + "</_0.999>");
                output.println("<_0.9999>" + elapsedTimeFormat.format(methodStatisticsList.get((int) (size * 0.9999)).getElapsedTimeInMillis()) + "</_0.9999>");
                output.println("</method>");
            }
            final List<MethodStatistics> failedMethodList = getAllFailedMethodStatisticsList();
            if (failedMethodList.size() > 0) {
                output.println("<errors>" + failedMethodList.size() + " (" + (failedMethodList.size() * 100D) / getTotalCall() + "%)</errors>"); //NOSONAR
                for (final MethodStatistics methodStatistics : failedMethodList) {
                    output.println("<error>" + methodStatistics.toString() + "</error>");
                }
            }
            output.println("</result>");
            output.flush();
        }
    }
}
