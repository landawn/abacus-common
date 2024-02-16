/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.util;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface PaginatedDataSet extends Iterable<DataSet> {

    /**
     * Returns a frozen {@code DataSet}.
     *
     * @return a frozen {@code DataSet}.
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    DataSet currentPage();

    /**
     *
     * @return a frozen {@code DataSet}.
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    DataSet previousPage();

    /**
     * Checks for next.
     *
     * @return true, if successful
     */
    boolean hasNext();

    /**
     *
     * @return a frozen {@code DataSet}.
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    DataSet nextPage();

    /**
     * Returns the first page.
     *
     * @return a frozen {@code DataSet}.
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    Optional<DataSet> firstPage();

    /**
     * Returns the last page.
     *
     * @return a frozen {@code DataSet}.
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    Optional<DataSet> lastPage();

    /**
     *
     * @param pageNum
     * @return a frozen {@code DataSet}.
     * @throws IllegalArgumentException the illegal argument exception
     * @see DataSet#slice(int, int, java.util.Collection)
     */
    DataSet getPage(int pageNum);

    /**
     *
     * @param pageNum
     * @return
     */
    PaginatedDataSet absolute(int pageNum);

    /**
     *
     * @return int
     */
    int currentPageNum();

    /**
     *
     * @return int
     */
    int pageSize();

    /**
     * 
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

    /**
     * 
     *
     * @return 
     */
    Stream<DataSet> stream();
}
