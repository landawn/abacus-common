/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.util;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * Interface for handling paginated data structures, providing methods to navigate through pages
 * of data and retrieve page information. This interface extends {@link Iterable} to allow
 * iteration over the pages.
 * 
 * <p>The Paginated interface is designed to work with any data structure that can be logically
 * divided into pages, such as database query results, API responses, or large collections that
 * need to be processed in chunks.</p>
 * 
 * <p><b>Key concepts:</b></p>
 * <ul>
 *   <li>Pages are typically numbered starting from 0 or 1 (implementation-specific)</li>
 *   <li>Each page contains a fixed number of items (except possibly the last page)</li>
 *   <li>Navigation between pages is supported through various methods</li>
 *   <li>The interface is generic to support any type of paginated content</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * // Assuming a Paginated implementation for database results
 * Paginated<List<User>> userPages = userRepository.findAllPaginated(pageSize: 20);
 * 
 * // Get specific page
 * List<User> firstPage = userPages.getPage(0);
 * 
 * // Navigate through pages
 * Optional<List<User>> lastPageOpt = userPages.lastPage();
 * if (lastPageOpt.isPresent()) {
 *     List<User> lastUsers = lastPageOpt.get();
 * }
 * 
 * // Stream all pages
 * userPages.stream()
 *     .flatMap(List::stream)
 *     .forEach(user -> process(user));
 * 
 * // Iterate through pages
 * for (List<User> page : userPages) {
 *     processPage(page);
 * }
 * }</pre>
 * 
 * @param <T> the type of data contained in each page (e.g., List&lt;Entity&gt;, Page&lt;Data&gt;)
 * @see Optional
 * @see Stream
 * @see Iterable
 * @since 1.0
 */
public interface Paginated<T> extends Iterable<T> {

    /**
     * Returns the first page of the paginated data wrapped in an Optional.
     * If the paginated data is empty (contains no pages), an empty Optional is returned.
     * 
     * <p>This method provides a safe way to access the first page without throwing
     * exceptions when the data set is empty.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<Product>> products = productService.getPaginatedProducts();
     * 
     * products.firstPage().ifPresent(firstPageProducts -> {
     *     System.out.println("First page has " + firstPageProducts.size() + " products");
     *     // Process first page products
     * });
     * 
     * // Or with explicit handling
     * Optional<List<Product>> firstPage = products.firstPage();
     * if (firstPage.isPresent()) {
     *     displayProducts(firstPage.get());
     * } else {
     *     System.out.println("No products found");
     * }
     * }</pre>
     *
     * @return an Optional containing the first page if it exists, or Optional.empty() if there are no pages
     */
    Optional<T> firstPage();

    /**
     * Returns the last page of the paginated data wrapped in an Optional.
     * If the paginated data is empty (contains no pages), an empty Optional is returned.
     * 
     * <p>This method is useful for scenarios where you need to access the most recent
     * data or check the final page of results. The implementation should efficiently
     * retrieve the last page without iterating through all pages if possible.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<LogEntry>> logs = logService.getPaginatedLogs();
     * 
     * // Get the most recent log entries (assuming newest last)
     * logs.lastPage().ifPresent(recentLogs -> {
     *     System.out.println("Latest " + recentLogs.size() + " log entries");
     *     recentLogs.forEach(this::processLogEntry);
     * });
     * 
     * // Check if there's data before processing
     * if (logs.lastPage().isPresent()) {
     *     int totalPages = logs.totalPages();
     *     System.out.println("Processing " + totalPages + " pages of logs");
     * }
     * }</pre>
     *
     * @return an Optional containing the last page if it exists, or Optional.empty() if there are no pages
     */
    Optional<T> lastPage();

    /**
     * Retrieves the page at the specified page number (0-based index).
     * 
     * <p>This method throws an exception if the requested page number is out of bounds.
     * Use {@link #totalPages()} to check the valid range before calling this method, or
     * use {@link #firstPage()} and {@link #lastPage()} for safe boundary access.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<Order>> orders = orderService.getPaginatedOrders();
     * int totalPages = orders.totalPages();
     * 
     * // Safely get a specific page
     * int pageToRetrieve = 5;
     * if (pageToRetrieve < totalPages) {
     *     List<Order> page = orders.getPage(pageToRetrieve);
     *     processOrders(page);
     * }
     * 
     * // Process multiple pages
     * for (int i = 0; i < Math.min(10, totalPages); i++) {
     *     List<Order> page = orders.getPage(i);
     *     System.out.println("Page " + i + " has " + page.size() + " orders");
     * }
     * }</pre>
     *
     * @param pageNum the page number to retrieve (0-based index)
     * @return the data contained in the specified page
     * @throws IllegalArgumentException if pageNum is negative or exceeds the total number of pages
     * @throws IndexOutOfBoundsException if the page number is out of valid range (alternative to IllegalArgumentException)
     */
    T getPage(int pageNum);

    /**
     * Returns the size of each page in the paginated data.
     * This represents the maximum number of items that can be contained in a single page.
     * 
     * <p>Note that the last page may contain fewer items than the page size if the total
     * number of items is not evenly divisible by the page size.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<Customer>> customers = customerService.getPaginatedCustomers();
     * int pageSize = customers.pageSize();
     * 
     * System.out.println("Each page contains up to " + pageSize + " customers");
     * 
     * // Calculate total items (if all pages are full except possibly the last)
     * int totalPages = customers.totalPages();
     * customers.lastPage().ifPresent(lastPage -> {
     *     int totalItems = (totalPages - 1) * pageSize + lastPage.size();
     *     System.out.println("Total customers: " + totalItems);
     * });
     * }</pre>
     *
     * @return the maximum number of items per page
     */
    int pageSize();

    /**
     * Returns the total number of pages in the paginated data.
     * This method is deprecated and replaced by {@link #totalPages()}.
     * 
     * <p><b>Migration note:</b> Use {@code totalPages()} instead of this method.
     * This method will be removed in a future version.</p>
     * 
     * <p><b>Example migration:</b></p>
     * <pre>{@code
     * // Old code
     * int count = paginated.pageCount();
     * 
     * // New code
     * int count = paginated.totalPages();
     * }</pre>
     *
     * @return the total number of pages
     * @see #totalPages()
     * @deprecated replaced by {@code totalPages()} for better naming consistency
     */
    @Deprecated
    int pageCount();

    /**
     * Returns the total number of pages in the paginated data.
     * If the data is empty, this method returns 0.
     * 
     * <p>This method is essential for pagination controls, determining valid page ranges,
     * and calculating progress through large data sets.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<Document>> documents = searchService.searchDocuments(query);
     * int totalPages = documents.totalPages();
     * 
     * // Display pagination info
     * System.out.println("Found " + totalPages + " pages of results");
     * 
     * // Build pagination controls
     * for (int i = 0; i < Math.min(totalPages, 10); i++) {
     *     // Show first 10 page links
     *     createPageLink(i);
     * }
     * 
     * // Check if pagination is needed
     * if (totalPages > 1) {
     *     showPaginationControls();
     * }
     * }</pre>
     *
     * @return the total number of pages, or 0 if the paginated data is empty
     */
    int totalPages();

    /**
     * Creates a Stream of all pages in the paginated data.
     * This allows for functional-style processing of pages using the Stream API.
     * 
     * <p>The returned stream is sequential by default and provides lazy evaluation of pages.
     * Pages are retrieved as needed when stream operations are performed. This is particularly
     * useful for large data sets where you want to process pages without loading all of them
     * into memory at once.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * Paginated<List<Transaction>> transactions = transactionService.getPaginatedTransactions();
     * 
     * // Process all transactions across all pages
     * long totalAmount = transactions.stream()
     *     .flatMap(List::stream)
     *     .mapToLong(Transaction::getAmount)
     *     .sum();
     * 
     * // Find first page containing a specific transaction
     * Optional<List<Transaction>> pageWithLargeTransaction = transactions.stream()
     *     .filter(page -> page.stream().anyMatch(t -> t.getAmount() > 10000))
     *     .findFirst();
     * 
     * // Process only first 5 pages
     * transactions.stream()
     *     .limit(5)
     *     .forEach(page -> processBatch(page));
     * 
     * // Count total items across all pages
     * long totalItems = transactions.stream()
     *     .mapToLong(List::size)
     *     .sum();
     * }</pre>
     *
     * @return a Stream of pages that can be processed using Stream operations
     * @see Stream
     */
    Stream<T> stream();
}
