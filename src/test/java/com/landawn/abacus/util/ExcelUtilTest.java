package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.poi.ExcelUtil;
import com.landawn.abacus.poi.ExcelUtil.FreezePane;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;

@Tag("old-test")
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
        CsvUtil.load(csvOutputFile).println();

        IOUtil.deleteQuietly(outputExcelFile);
        IOUtil.deleteQuietly(csvOutputFile);
    }

    @Test
    public void test_loadSheet() {
        final Dataset dataset = CommonUtil.newDataset(CommonUtil.asList("column1", "column2"),
                CommonUtil.asList(CommonUtil.asList("ab", "cd"), CommonUtil.asList("ef", "gh")));
        dataset.println();

        File outputExcelFile = new File("./src/test/resources/test_excel_02.xlsx");
        ExcelUtil.writeSheet("sheet_001", dataset, outputExcelFile);

        Dataset dataset2 = ExcelUtil.loadSheet(outputExcelFile);

        dataset2.println();

        assertEquals(dataset, dataset2);

        IOUtil.deleteQuietly(outputExcelFile);
    }
}
