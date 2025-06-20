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

public interface Paginated<T> extends Iterable<T> {
    //    /**
    //     * Checks for next.
    //     *
    //     * @return {@code true}, if successful
    //     */
    //    boolean hasNext(); // confusing with Iterator.hasNext()
    //
    //    T nextPage();
    //
    //    T currentPage();
    //
    //    T previousPage();
    //
    //    /**
    //     *
    //     * @param pageNum
    //     * @return
    //     */
    //    Paginated<T> absolute(int pageNum);
    //    
    //   /**
    //    *
    //    * @return int
    //    */
    //   int currentPageNum();

    /**
     * Returns the first page.
     *
     * @return
     */
    Optional<T> firstPage();

    /**
     * Returns the last page.
     *
     * @return
     */
    Optional<T> lastPage();

    /**
     *
     * @param pageNum
     * @return
     * @throws IllegalArgumentException the illegal argument exception
     */
    T getPage(int pageNum);

    /**
     *
     * @return int
     */
    int pageSize();

    /**
     *
     * @return int
     * @see #totalPages()
     * @deprecated replaced by {@code totalPages}
     */
    @Deprecated
    int pageCount();

    /**
     *
     * @return int
     */
    int totalPages();

    Stream<T> stream();
}
