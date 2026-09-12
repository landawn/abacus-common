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

/**
 * Spreadsheet utilities built on Apache POI.
 *
 * <p>{@link ExcelUtil} reads and writes XLS (Excel 97–2003) and XLSX (Excel 2007+) workbooks and
 * converts them to and from Abacus {@link com.landawn.abacus.util.Dataset} values and CSV.</p>
 *
 * <p>Primary operations:</p>
 * <ul>
 *   <li>{@code readDatasetFromSheet} &mdash; first physical row becomes column names</li>
 *   <li>{@code readRowsFromSheet} / {@code streamRowsFromSheet} &mdash; raw rows, optional skip of the first row;
 *       streams must be closed so the workbook is released</li>
 *   <li>{@code writeDatasetToSheet} / {@code writeRowsToSheet} &mdash; write a Dataset or header-plus-rows;
 *       {@code SheetCreateOptions} can freeze panes, auto-size columns, and apply an auto-filter</li>
 *   <li>{@code exportSheetToCsv} &mdash; render a sheet as CSV (numeric cells use POI
 *       {@code DataFormatter} so dates and integers look as they do in Excel)</li>
 * </ul>
 *
 * <p>{@code File}/{@code Path} writers infer XLS vs XLSX from the extension and replace the destination
 * via a sibling temporary file. {@code OutputStream} writers require an explicit format and do not close
 * the stream. Apache POI must be on the classpath.</p>
 *
 * @see ExcelUtil
 * @see com.landawn.abacus.util.Dataset
 */
package com.landawn.abacus.poi;
