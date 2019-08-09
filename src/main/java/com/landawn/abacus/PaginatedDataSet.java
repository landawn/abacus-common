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

package com.landawn.abacus;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Interface PaginatedDataSet.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface PaginatedDataSet extends Iterable<DataSet> {
    /**
     * Method currentPage.
     * 
     * @return DataSet
     */
    DataSet currentPage();

    /**
     * Method previousPage.
     * 
     * @return DataSet
     */
    DataSet previousPage();

    /**
     * Checks for next.
     *
     * @return true, if successful
     */
    boolean hasNext();

    /**
     * Method nextPage.
     * 
     * @return DataSet
     */
    DataSet nextPage();

    /**
     * Returns the first page.
     * 
     * @return DataSet
     */
    Optional<DataSet> firstPage();

    /**
     * Returns the last page.
     * 
     * @return DataSet
     */
    Optional<DataSet> lastPage();

    /**
     * Method getPage.
     *
     * @param pageNum the page num
     * @return the page
     * @throws IllegalArgumentException the illegal argument exception
     */
    DataSet getPage(int pageNum);

    /**
     * Method absolute.
     *
     * @param pageNum the page num
     * @return the paginated data set
     */
    PaginatedDataSet absolute(int pageNum);

    /**
     * Method currentPageNum.
     * 
     * @return int
     */
    int currentPageNum();

    /**
     * Method pageLength.
     * 
     * @return int
     */
    int pageSize();

    /**
     * Method pageCount.
     * 
     * @return int
     */
    int pageCount();

    /**
     * Stream.
     *
     * @return the stream
     */
    Stream<DataSet> stream();
}
