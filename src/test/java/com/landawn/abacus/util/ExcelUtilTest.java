/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.poi.ExcelUtil;
import com.landawn.abacus.poi.ExcelUtil.FreezePane;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;

public class ExcelUtilTest extends AbstractTest {
    @Test
    public void test_read_sheet() throws Exception {
        File file = new File("./src/test/resources/test_excel_01.xlsx");

        List<List<Object>> rowList = ExcelUtil.readSheet(file);

        rowList.forEach(Fn.println());

        List<List<Object>> rows = ExcelUtil.readSheet(file, 0, false, RowMappers.DEFAULT);

        File outputExcelFile = new File("./src/test/resources/test_excel_02.xlsx");
        ExcelUtil.writeSheet("sheet_001", rows.get(0), rows.subList(1, rows.size()),
                SheetCreateOptions.builder().autoSizeColumn(true).freezePane(new FreezePane(0, 1)).autoFilterByFirstRow(true).build(), outputExcelFile);

        rowList = ExcelUtil.readSheet(outputExcelFile);

        N.println(Strings.repeat("=", 80));
        rowList.forEach(Fn.println());

        File csvOutputFile = new File("./src/test/resources/test_03.csv");
        ExcelUtil.saveSheetAsCsv(outputExcelFile, 0, null, csvOutputFile, Charsets.UTF_8);

        N.println(Strings.repeat("=", 80));
        N.println(IOUtil.readAllToString(csvOutputFile));

        N.println(Strings.repeat("=", 80));
        CSVUtil.loadCSV(csvOutputFile).println();

        IOUtil.deleteQuietly(outputExcelFile);
        IOUtil.deleteQuietly(csvOutputFile);
    }

    @Test
    public void test_loadSheet() {
        final DataSet dataSet = N.newDataSet(N.asList("column1", "column2"), N.asList(N.asList("ab", "cd"), N.asList("ef", "gh")));
        dataSet.println();

        File outputExcelFile = new File("./src/test/resources/test_excel_02.xlsx");
        ExcelUtil.writeSheet("sheet_001", dataSet, outputExcelFile);

        DataSet dataSet2 = ExcelUtil.loadSheet(outputExcelFile);

        dataSet2.println();

        assertEquals(dataSet, dataSet2);

        IOUtil.deleteQuietly(outputExcelFile);
    }
}
